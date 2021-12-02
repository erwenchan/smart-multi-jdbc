package cn.smartdec.multienv.data.jdbc;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

public class SubDataSourceProperties {

    private String envKey;

    /**
     * JDBC URL of the database.
     */
    private String url;

    /**
     * Login username of the database.
     */
    private String username;

    /**
     * Login password of the database.
     */
    private String password;

    public DataSourceProperties convert() {
        DataSourceProperties dataSourceProperties = new DataSourceProperties();
        dataSourceProperties.setUrl(this.url);
        dataSourceProperties.setUsername(this.username);
        dataSourceProperties.setPassword(this.password);
        return dataSourceProperties;
    }

    public String getEnvKey() {
        return envKey;
    }

    public void setEnvKey(String envKey) {
        this.envKey = envKey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
