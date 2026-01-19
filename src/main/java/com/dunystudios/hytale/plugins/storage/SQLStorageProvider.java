package com.dunystudios.hytale.plugins.storage;

import com.dunystudios.hytale.plugins.HyEconomy;
import com.dunystudios.hytale.plugins.json.UserProfile;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SQLStorageProvider implements StorageProvider {
    private HikariDataSource dataSource;
    private final Map<UUID, UserProfile> cache;
    private final String storageType;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public SQLStorageProvider(String storageType, String host, int port, String database, String username, String password) {
        this.cache = new ConcurrentHashMap<>();
        this.storageType = storageType.toLowerCase();
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();

        switch (storageType) {
            case "sqlite":
                config.setJdbcUrl("jdbc:sqlite:HyEconomy/economy.db");
                config.setDriverClassName("org.sqlite.JDBC");
                config.setMaximumPoolSize(1);
                break;
            case "mysql":
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setUsername(username);
                config.setPassword(password);
                config.setMaximumPoolSize(10);
                break;
            default:
                throw new IllegalArgumentException("Unsupported storage type: " + storageType);
        }

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setConnectionTimeout(10000);

        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String createTableSQL;

        if (storageType.equals("sqlite")) {
            createTableSQL = "CREATE TABLE IF NOT EXISTS economy_users (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "balance REAL NOT NULL" +
                    ")";
        } else {
            createTableSQL = "CREATE TABLE IF NOT EXISTS economy_users (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "balance FLOAT NOT NULL" +
                    ")";
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            HyEconomy.getInstance().getLogger().atInfo().log("Economy table created or already exists");
        } catch (SQLException e) {
            HyEconomy.getInstance().getLogger().atSevere().log("Failed to create economy table", e);
        }
    }

    @Override
    public void shutdown() {
        unloadAll();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public void loadPlayerData(UUID uuid) {
        String selectSQL = "SELECT uuid, balance FROM economy_users WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSQL)) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UserProfile profile = new UserProfile();
                profile.setUuid(rs.getString("uuid"));
                profile.setBalance(rs.getFloat("balance"));
                cache.put(uuid, profile);
            } else {
                UserProfile profile = new UserProfile();
                profile.setUuid(uuid.toString());
                profile.setBalance(HyEconomy.getInstance().getConfigManager().getData().balance.starting);
                cache.put(uuid, profile);

                String insertSQL = "INSERT INTO economy_users (uuid, balance) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                    insertStmt.setString(1, uuid.toString());
                    insertStmt.setFloat(2, profile.balance);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            HyEconomy.getInstance().getLogger().atSevere().log("Failed to load player data for " + uuid, e);
        }
    }

    @Override
    public UserProfile getProfile(UUID uuid) {
        return cache.get(uuid);
    }

    @Override
    public void updateProfile(UUID uuid, UserProfile profile) {
        cache.put(uuid, profile);
    }

    @Override
    public void savePlayerData(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        if (profile == null) return;

        String updateSQL = "UPDATE economy_users SET balance = ? WHERE uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSQL)) {

            stmt.setFloat(1, profile.balance);
            stmt.setString(2, uuid.toString());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                String insertSQL = "INSERT INTO economy_users (uuid, balance) VALUES (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                    insertStmt.setString(1, uuid.toString());
                    insertStmt.setFloat(2, profile.balance);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            HyEconomy.getInstance().getLogger().atSevere().log("Failed to save player data for " + uuid, e);
        }
    }

    @Override
    public void unloadPlayer(UUID uuid) {
        savePlayerData(uuid);
        cache.remove(uuid);
    }

    @Override
    public void unloadAll() {
        for (UUID uuid : cache.keySet()) {
            savePlayerData(uuid);
        }
        cache.clear();
    }

    @Override
    public List<UserProfile> getTopBalances(int limit, int offset) {
        List<UserProfile> profiles = new ArrayList<>();
        String selectSQL = "SELECT uuid, balance FROM economy_users ORDER BY balance DESC LIMIT ? OFFSET ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(selectSQL)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UserProfile profile = new UserProfile();
                profile.setUuid(rs.getString("uuid"));
                profile.setBalance(rs.getFloat("balance"));
                profiles.add(profile);
            }
        } catch (SQLException e) {
            HyEconomy.getInstance().getLogger().atSevere().log("Failed to fetch top balances", e);
        }

        return profiles;
    }
}