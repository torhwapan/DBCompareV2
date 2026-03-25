package com.dbcompare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "database")
public class DatabaseProperties {

    private Oracle oracle = new Oracle();
    private Postgres postgres = new Postgres();

    // Oracle配置类
    public static class Oracle {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "oracle.jdbc.OracleDriver";

        // getters and setters
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

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }

    // Postgres配置类
    public static class Postgres {
        private String url;
        private String username;
        private String password;
        private String driverClassName = "org.postgresql.Driver";

        // getters and setters
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

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }

    // getters and setters
    public Oracle getOracle() {
        return oracle;
    }

    public void setOracle(Oracle oracle) {
        this.oracle = oracle;
    }

    public Postgres getPostgres() {
        return postgres;
    }

    public void setPostgres(Postgres postgres) {
        this.postgres = postgres;
    }
}