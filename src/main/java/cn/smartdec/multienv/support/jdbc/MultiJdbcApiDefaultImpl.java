package cn.smartdec.multienv.support.jdbc;

import cn.smartdec.multienv.data.jdbc.SubDataSourceProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiJdbcApiDefaultImpl implements MultiJdbcApi {

    private final Map<String, SubDataSourceProperties> propertiesMap;

    public MultiJdbcApiDefaultImpl(MultiJdbcProperties multiJdbcProperties) {
        Map<String, SubDataSourceProperties> map = new HashMap<>();
        if (null != multiJdbcProperties.getDatasources()) {
            multiJdbcProperties.getDatasources().forEach(properties ->
                    map.put(properties.getEnvKey(), properties)
            );
        }
        propertiesMap = Collections.unmodifiableMap(map);
    }

    @Override
    public SubDataSourceProperties subDataSourceProperties(String envKey) {
        SubDataSourceProperties source = propertiesMap.get(envKey);
        if (null == source) {
            return null;
        }

        // copy一个对象输出，确保原配置对象不被修改
        SubDataSourceProperties target = new SubDataSourceProperties();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    @ConfigurationProperties(prefix = "smart.multienv.jdbc")
    public static class MultiJdbcProperties {

        private List<SubDataSourceProperties> datasources;

        public List<SubDataSourceProperties> getDatasources() {
            return datasources;
        }

        public void setDatasources(List<SubDataSourceProperties> datasources) {
            this.datasources = datasources;
        }

    }

}
