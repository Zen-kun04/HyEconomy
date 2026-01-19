package com.dunystudios.hytale.plugins;

import com.dunystudios.hytale.plugins.commands.BalanceCommand;
import com.dunystudios.hytale.plugins.commands.BaltopCommand;
import com.dunystudios.hytale.plugins.commands.PayCommand;
import com.dunystudios.hytale.plugins.commands.admin.EcoCommand;
import com.dunystudios.hytale.plugins.config.ConfigManager;
import com.dunystudios.hytale.plugins.events.PlayerJoin;
import com.dunystudios.hytale.plugins.events.PlayerLeave;
import com.dunystudios.hytale.plugins.json.PlayerManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class HyEconomy extends JavaPlugin {
    private ConfigManager configManager;
    private static HyEconomy instance = null;
    private final PlayerManager playerManager;
    private final Economy economy;

    public HyEconomy(@Nonnull JavaPluginInit init) {
        super(init);
        this.instance = this;
        this.configManager = new ConfigManager();
        this.configManager.setup();
        this.playerManager = new PlayerManager(configManager.getData());
        this.economy = new Economy(playerManager, configManager);
    }

    @Override
    protected void setup() {
        IEcoAPI.Service.setInstance(economy);
        new PlayerJoin();
        new PlayerLeave();
        getCommandRegistry().registerCommand(new BalanceCommand("balance", "Check your balance"));
        getCommandRegistry().registerCommand(new PayCommand("pay", "Pay another player"));
        getCommandRegistry().registerCommand(new BaltopCommand("baltop", "View top player balances"));
        getCommandRegistry().registerCommand(new EcoCommand("eco", "Admin economy commands"));

        getLogger().atInfo().log("HyEconomy plugin has been initialized.");
    }

    @Override
    protected void shutdown() {
        getLogger().atInfo().log("HyEconomy plugin is shutting down.");
        playerManager.unloadAll();
    }

    public static HyEconomy getInstance() {
        return instance;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Economy getEconomy() {
        return economy;
    }
}
