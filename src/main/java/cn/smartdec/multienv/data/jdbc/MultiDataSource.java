package cn.smartdec.multienv.data.jdbc;

import cn.smartdec.multienv.support.jdbc.MultiJdbcContext;
import cn.smartdec.multienv.support.jdbc.MultiJdbcApi;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class MultiDataSource extends AbstractDataSource implements InitializingBean, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(MultiDataSource.class);

    private final ApplicationContext applicationContext;

    /**
     * admin连接池属性配置
     */
    private final DataSourceProperties adminProperties;

    /**
     * 各种类型的连接池属性配置
     * <p>
     * key: className; value: Properties
     */
    private final Map<String, Properties> otherDataSourceProperties;
    private final DataSource adminDataSource;

    private final Class<? extends DataSource> defaultDataSourceType;

    /**
     * key: envKey; value: DataSource
     */
    private Cache<String, DataSource> dataSourcesCache;

    private final Interner<Object> interner = Interners.newWeakInterner();

    private MultiJdbcApi multiJdbcApi;

    public MultiDataSource(ApplicationContext applicationContext, DataSourceProperties adminProperties,
                           Map<String, Properties> otherDataSourceProperties) {
        this.applicationContext = applicationContext;
        this.adminProperties = adminProperties;
        this.otherDataSourceProperties = otherDataSourceProperties;

        this.defaultDataSourceType = adminProperties.getType(); // 默认连接池类型和主配置一致
        this.adminDataSource = createDataSource(this.adminProperties);
    }

    @Override
    public void afterPropertiesSet() {
        RemovalListener<String, DataSource> listener = notification -> {
            String envKey = notification.getKey();
            LOG.info("Remove DataSource, envKey: {}, cause: {}", envKey, notification.getCause());

            DataSource dataSource = notification.getValue();
            if (dataSource instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) dataSource).close();
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        };
        dataSourcesCache = CacheBuilder.newBuilder()
                .maximumSize(128)
                .expireAfterAccess(24, TimeUnit.HOURS)
                .removalListener(listener)
                .build();

        // 防止循环依赖，不从构造方法进行注入
        multiJdbcApi = applicationContext.getBean(MultiJdbcApi.class);
    }

    @Override
    public void destroy() throws Exception {
        if (null != dataSourcesCache) {
            dataSourcesCache.invalidateAll();
        }

        if (adminDataSource instanceof AutoCloseable) {
            ((AutoCloseable) adminDataSource).close();
        }
    }

    public void invalidateDataSource(String envKey) {
        dataSourcesCache.invalidate(envKey);
    }

    protected DataSource createDataSource(DataSourceProperties properties) {
        DataSourceBuilder<?> dataSourceBuilder = properties.initializeDataSourceBuilder();

        if (null == properties.getType()) {
            dataSourceBuilder.type(defaultDataSourceType);
        }

        DataSource dataSource = dataSourceBuilder.build();

        Properties dataSourceConfiguration = otherDataSourceProperties.get(dataSource.getClass().getName());
        if (null != dataSourceConfiguration && !dataSourceConfiguration.isEmpty()) {
            ConfigurationPropertySource source = new MapConfigurationPropertySource(dataSourceConfiguration);
            Binder binder = new Binder(source);
            binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(dataSource));
        }

        return dataSource;
    }

    protected DataSource determineTargetDataSource() {
        Assert.notNull(this.dataSourcesCache, "DataSource router not initialized");
        String envKey = determineCurrentEvnKey();
        if (null == envKey) {
            throw new IllegalStateException("EnvKey is missing");
        }

        if (MultiJdbcContext.ENV_KEY_ADMIN.equals(envKey)) {
            return adminDataSource;
        }

        DataSource dataSource = this.dataSourcesCache.getIfPresent(envKey);
        if (null == dataSource) {
            synchronized (interner.intern(envKey)) {
                dataSource = this.dataSourcesCache.getIfPresent(envKey);
                if (null == dataSource) {
                    dataSource = buildDataSource(envKey);
                    if (null != dataSource) {
                        this.dataSourcesCache.put(envKey, dataSource);
                    }
                }
            }
        }
        if (null == dataSource) {
            throw new IllegalStateException("Cannot determine target dataSource for envKey [" + envKey + "]");
        }
        return dataSource;
    }

    @Nullable
    protected String determineCurrentEvnKey() {
        return MultiJdbcContext.getEnvKey();
    }

    private DataSource buildDataSource(String envKey) {
        SubDataSourceProperties subDataSourceProperties = multiJdbcApi.subDataSourceProperties(envKey);
        if (null == subDataSourceProperties) {
            return null;
        }
        DataSourceProperties properties = subDataSourceProperties.convert();
        return createDataSource(properties);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return determineTargetDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return determineTargetDataSource().getConnection(username, password);
    }

}
