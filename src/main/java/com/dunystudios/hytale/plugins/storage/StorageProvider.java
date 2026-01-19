package com.dunystudios.hytale.plugins.storage;

import com.dunystudios.hytale.plugins.json.UserProfile;

import java.util.List;
import java.util.UUID;

public interface StorageProvider {
    void init();

    void shutdown();

    void loadPlayerData(UUID uuid);

    UserProfile getProfile(UUID uuid);

    void updateProfile(UUID uuid, UserProfile profile);

    void savePlayerData(UUID uuid);

    void unloadPlayer(UUID uuid);

    void unloadAll();

    List<UserProfile> getTopBalances(int limit, int offset);
}