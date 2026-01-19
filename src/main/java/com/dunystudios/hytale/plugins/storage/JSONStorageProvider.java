package com.dunystudios.hytale.plugins.storage;

import com.dunystudios.hytale.plugins.HyEconomy;
import com.dunystudios.hytale.plugins.json.UserProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JSONStorageProvider implements StorageProvider {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, UserProfile> cache;
    private final Path dataFolder;

    public JSONStorageProvider() {
        this.cache = new ConcurrentHashMap<>();
        this.dataFolder = Path.of("HyEconomy", "profiles");
    }

    @Override
    public void init() {
        File folder = dataFolder.toFile();
        if (!folder.exists()) folder.mkdirs();
    }

    @Override
    public void shutdown() {
        unloadAll();
    }

    @Override
    public void loadPlayerData(UUID uuid) {
        File file = dataFolder.resolve(uuid.toString() + ".json").toFile();

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                UserProfile profile = gson.fromJson(reader, UserProfile.class);
                cache.put(uuid, profile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            UserProfile profile = new UserProfile();
            profile.setUuid(uuid.toString());
            profile.setBalance(HyEconomy.getInstance().getConfigManager().getData().balance.starting);
            cache.put(uuid, profile);
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

        File file = dataFolder.resolve(uuid.toString() + ".json").toFile();
        HyEconomy.getInstance().getLogger().atInfo().log("Saving data for player " + uuid + " in file " + file.getAbsolutePath());
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(profile, writer);
        } catch (IOException e) {
            e.printStackTrace();
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
        List<UserProfile> allProfiles = new ArrayList<>();

        File folder = dataFolder.toFile();
        if (!folder.exists()) return allProfiles;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return allProfiles;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                UserProfile profile = gson.fromJson(reader, UserProfile.class);
                allProfiles.add(profile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        allProfiles.sort((a, b) -> Float.compare(b.balance, a.balance));

        int fromIndex = Math.min(offset, allProfiles.size());
        int toIndex = Math.min(offset + limit, allProfiles.size());

        return allProfiles.subList(fromIndex, toIndex);
    }
}