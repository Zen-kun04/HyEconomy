package com.dunystudios.hytale.plugins.commands;

import com.dunystudios.hytale.plugins.HyEconomy;
import com.dunystudios.hytale.plugins.config.ConfigManager;
import com.dunystudios.hytale.plugins.json.UserProfile;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public class BaltopCommand extends CommandBase {
    private final ConfigManager configManager;
    private static final int PAGE_SIZE = 10;

    public BaltopCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
        this.addAliases("baltop", "moneytop");
        this.setPermissionGroup(GameMode.Adventure);

        this.setAllowsExtraArguments(true);
        this.configManager = HyEconomy.getInstance().getConfigManager();
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        try {
            int page = 1;

            String[] args = commandContext.getInputString().split("\\s+");
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                    if (page < 1) page = 1;
                } catch (NumberFormatException e) {
                    commandContext.sender().sendMessage(
                            Message.raw("Invalid page number. Usage: /baltop [page]")
                    );
                    return;
                }
            }

            int offset = (page - 1) * PAGE_SIZE;

            List<UserProfile> topBalances = HyEconomy.getInstance()
                    .getPlayerManager()
                    .getTopBalances(PAGE_SIZE, offset);

            if (topBalances.isEmpty()) {
                commandContext.sender().sendMessage(
                        Message.raw("No players found on page " + page + ".")
                );
                return;
            }

            StringBuilder message = new StringBuilder();
            message.append("=== Top Balances (Page ").append(page).append(") ===\n");

            for (int i = 0; i < topBalances.size(); i++) {
                UserProfile profile = topBalances.get(i);
                int rank = offset + i + 1;

                String formattedBalance = new BigDecimal(profile.balance)
                        .setScale(configManager.getData().chat.decimal_places, RoundingMode.DOWN)
                        .toString();

                UUID uuid = UUID.fromString(profile.uuid);
                String playerName = getPlayerName(uuid);

                message.append(rank).append(". ")
                        .append(playerName)
                        .append(": ")
                        .append(configManager.getData().chat.currency)
                        .append(formattedBalance)
                        .append("\n");
            }

            message.append("\nUse /baltop ").append(page + 1).append(" to see the next page.");

            commandContext.sender().sendMessage(Message.raw(message.toString()));

        } catch (Exception e) {
            commandContext.sender().sendMessage(
                    Message.raw(configManager.getData().messages.error_occurred)
            );
            HyEconomy.getInstance().getLogger().atSevere().log("Error executing baltop command", e);
        }
    }

    private String getPlayerName(UUID uuid) {
        try {
            List<PlayerRef> players = Universe.get().getPlayers();
            for (PlayerRef player : players) {
                if (player.getUuid().equals(uuid)) {
                    return player.getUsername();
                }
            }
        } catch (Exception e) {
            HyEconomy.getInstance().getLogger().atWarning().log("Could not get player name for UUID: " + uuid);
        }
        return uuid.toString().substring(0, 8);
    }
}
