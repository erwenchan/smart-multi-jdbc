package cn.smartdec.multienv.support.jdbc;

import cn.smartdec.multienv.data.jdbc.SubDataSourceProperties;

public interface MultiJdbcApi {

    SubDataSourceProperties subDataSourceProperties(String envKey);

}
