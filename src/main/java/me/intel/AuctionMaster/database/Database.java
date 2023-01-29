package me.intel.AuctionMaster.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {

    private HikariDataSource hikari;
    private final HikariConfig hikariConfig = new HikariConfig();

    public Database setUsername(String username) {
        hikariConfig.addDataSourceProperty("user", username);
        return this;
    }

    public Database setPassword(String password) {
        hikariConfig.addDataSourceProperty("password", password);
        return this;
    }

    public Database setJdbcUrl(String jdbcUrl) {
        hikariConfig.setJdbcUrl(jdbcUrl);
        return this;
    }

    public Database setup() {
        hikariConfig.setMaxLifetime(600000); // zeby uniknac wiekszy lifetime hikari niz mysql
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setMaximumPoolSize(20);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true"); //pozwala lepiej wspolpracowac z prepared statements
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari = new HikariDataSource(hikariConfig);
        return this;
    }

    public HikariDataSource getHikari() {
        return hikari;
    }

    public HikariConfig getHikariConfig() {
        return hikariConfig;
    }
}