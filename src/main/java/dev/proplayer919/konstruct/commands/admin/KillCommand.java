package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.instance.GameInstanceData;
import dev.proplayer919.konstruct.instance.GameInstanceRegistry;
import dev.proplayer919.konstruct.instance.gameplayer.GamePlayerData;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;

import java.util.UUID;

public class KillCommand extends Command {

    public KillCommand() {
        super("kill", "killplayer");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Usage: /kill <username>"));

        var usernameArg = ArgumentType.String("username").setSuggestionCallback((sender, context, suggestion) -> {
            if (sender instanceof Player player) {
                GameInstanceData gameInstanceData = GameInstanceRegistry.getInstanceWithPlayer(player.getUuid());
                if (gameInstanceData != null) {
                    for (GamePlayerData gp : gameInstanceData.getAlivePlayers()) {
                        Player targetPlayer = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(gp.getUuid());
                        if (targetPlayer != null) {
                            suggestion.addEntry(new SuggestionEntry(targetPlayer.getUsername()));
                        }
                    }
                }
            }
        });

        addSyntax((sender, context) -> {
            final String username = context.get(usernameArg);
            if (sender instanceof Player player) {
                if (!PlayerPermissionRegistry.hasPermission(player, "command.kill")) {
                    MessagingHelper.sendMessage(sender, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }

                GameInstanceData gameInstanceData = GameInstanceRegistry.getInstanceWithPlayer(player.getUuid());
                if (gameInstanceData == null) {
                    MessagingHelper.sendMessage(sender, MessageType.ERROR, "You are not currently in a game.");
                    return;
                }

                Player targetPlayer = MinecraftServer.getConnectionManager().findOnlinePlayer(username);
                if (targetPlayer == null) {
                    MessagingHelper.sendMessage(sender, MessageType.ERROR, "Player with username '" + username + "' is not online.");
                    return;
                }

                GamePlayerData targetPlayerData = gameInstanceData.getAlivePlayers().stream()
                        .filter(gp -> gp.getUuid().equals(targetPlayer.getUuid()))
                        .findFirst()
                        .orElse(null);

                if (targetPlayerData == null) {
                    MessagingHelper.sendMessage(sender, MessageType.ERROR, "Player '" + username + "' is not in your game or is already dead.");
                    return;
                }

                gameInstanceData.killPlayer(targetPlayerData);

                if (gameInstanceData.getAlivePlayers().size() == 1) {
                    GamePlayerData gamePlayerData = gameInstanceData.getAlivePlayers().iterator().next();
                    gameInstanceData.winMatch(gamePlayerData);
                }
            } else {
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Only players can use this command.");
            }
        }, usernameArg);
    }
}