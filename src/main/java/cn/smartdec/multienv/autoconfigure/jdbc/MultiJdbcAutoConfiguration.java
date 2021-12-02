package cn.smartdec.multienv.autoconfigure.jdbc;

import cn.smartdec.multienv.data.jdbc.MultiDataSource;
import cn.smartdec.multienv.support.jdbc.MultiJdbcApi;
import cn.smartdec.multienv.support.jdbc.MultiJdbcApiDefaultImpl;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@ConditionalOnClass({DataSource.class})
@ConditionalOnMissingBean(DataSource.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class) // 抢先在spring-boot的DataSource配置之前
@EnableConfigurationProperties({DataSourceProperties.class})
@Import({MultiJdbcAutoConfiguration.MultiJdbcApiConfiguration.class})
public class MultiJdbcAutoConfiguration {

    @Configuration
    @ConditionalOnMissingBean(MultiJdbcApi.class) // 使用者可以实现自定义的MultiJdbcApi实现类
    @EnableConfigurationProperties({MultiJdbcApiDefaultImpl.MultiJdbcProperties.class})
    public static class MultiJdbcApiConfiguration {

        @Bean
        public MultiJdbcApi multiJdbcApi(MultiJdbcApiDefaultImpl.MultiJdbcProperties multiJdbcProperties) {
            return new MultiJdbcApiDefaultImpl(multiJdbcProperties);
        }
    }

    static class HikariDataSourceProperties extends Properties {
        static final String CONFIG_DATA_SOURCE_CLASS = "com.zaxxer.hikari.HikariDataSource";
    }

    static class TomcatDataSourceProperties extends Properties {
        static final String CONFIG_DATA_SOURCE_CLASS = "org.apache.tomcat.jdbc.pool.DataSource";
    }

    static class Dbcp2DataSourceProperties extends Properties {
        static final String CONFIG_DATA_SOURCE_CLASS = "org.apache.commons.dbcp2.BasicDataSource";
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariDataSourceProperties hikariDataSourceProperties() {
        return new HikariDataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.tomcat")
    public TomcatDataSourceProperties tomcatDataSourceProperties() {
        return new TomcatDataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.dbcp2")
    public Dbcp2DataSourceProperties dbcp2DataSourceProperties() {
        return new Dbcp2DataSourceProperties();
    }

    private Map<String, Properties> otherDataSourceProperties() {
        Map<String, Properties> otherDataSourceProperties = new HashMap<>();

        otherDataSourceProperties.put(HikariDataSourceProperties.CONFIG_DATA_SOURCE_CLASS, hikariDataSourceProperties());
        otherDataSourceProperties.put(TomcatDataSourceProperties.CONFIG_DATA_SOURCE_CLASS, tomcatDataSourceProperties());
        otherDataSourceProperties.put(Dbcp2DataSourceProperties.CONFIG_DATA_SOURCE_CLASS, dbcp2DataSourceProperties());

        return otherDataSourceProperties;
    }

    @Bean
    public DataSource multiDataSource(ApplicationContext applicationContext, DataSourceProperties properties) {
        return new MultiDataSource(applicationContext, properties, otherDataSourceProperties());
    }

}
