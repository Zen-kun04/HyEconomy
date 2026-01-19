package com.dunystudios.hytale.plugins.json;

import com.dunystudios.hytale.plugins.HyEconomy;
import com.dunystudios.hytale.plugins.config.ConfigData;
import com.dunystudios.hytale.plugins.storage.JSONStorageProvider;
import com.dunystudios.hytale.plugins.storage.SQLStorageProvider;
import com.dunystudios.hytale.plugins.storage.StorageProvider;

import java.util.List;
import java.util.UUID;

public class PlayerManager {
    private final StorageProvider storageProvider;

    public PlayerManager(ConfigData configData) {
        String storageType = configData.storage.type.toLowerCase();

        switch (storageType) {
            case "json":
                this.storageProvider = new JSONStorageProvider();
                break;
            case "sqlite":
                this.storageProvider = new SQLStorageProvider("sqlite", null, 0, null, null, null);
                break;
            case "mysql":
                ConfigData.Storage.MySQL mysql = configData.storage.mysql;
                this.storageProvider = new SQLStorageProvider("mysql", mysql.host, mysql.port, mysql.database, mysql.username, mysql.password);
                break;
            default:
                HyEconomy.getInstance().getLogger().atWarning().log("Unknown storage type: " + storageType + ", defaulting to JSON");
                this.storageProvider = new JSONStorageProvider();
                break;
        }

        storageProvider.init();
    }

    public void loadPlayerData(UUID uuid) {
        storageProvider.loadPlayerData(uuid);
    }

    public UserProfile getProfile(UUID uuid) {
        return storageProvider.getProfile(uuid);
    }

    public void updateProfile(UUID uuid, UserProfile profile) {
        storageProvider.updateProfile(uuid, profile);
    }

    public void savePlayerData(UUID uuid) {
        storageProvider.savePlayerData(uuid);
    }

    public void unloadPlayer(UUID uuid) {
        storageProvider.unloadPlayer(uuid);
    }

    public void unloadAll() {
        storageProvider.unloadAll();
        storageProvider.shutdown();
    }

    public List<UserProfile> getTopBalances(int limit, int offset) {
        return storageProvider.getTopBalances(limit, offset);
    }
}
