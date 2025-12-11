package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionCallback;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

import java.util.function.Supplier;

public class GameModeCommand extends Command {

    public GameModeCommand() {
        super("gamemode", "gm", "setgamemode", "setgm");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Usage: /gamemode <gamemode> [target]"));

        var gamemodeArg = ArgumentType.String("gamemode").setSuggestionCallback((sender, context, suggestion) -> {
            suggestion.addEntry(new SuggestionEntry("survival"));
            suggestion.addEntry(new SuggestionEntry("creative"));
            suggestion.addEntry(new SuggestionEntry("adventure"));
            suggestion.addEntry(new SuggestionEntry("spectator"));
        });
        var targetArg = ArgumentType.String("target").setDefaultValue((CommandSender sender) -> {
            if (sender instanceof Player player) {
                return player.getUsername();
            }
            return null;
        }).setSuggestionCallback((sender, context, suggestion) -> {
            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
                suggestion.addEntry(new SuggestionEntry(player.getUsername()));
            });
        });

        addSyntax((sender, context) -> {
            final String gamemode = context.get(gamemodeArg);
            final String target = context.get(targetArg);

            if (sender instanceof Player player) {
                if (!PlayerPermissionRegistry.hasPermission(player, "command.gamemode")) {
                    MessagingHelper.sendMessage(sender, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }

                GameMode gameMode;
                switch (gamemode.toLowerCase()) {
                    case "survival", "s", "0" -> gameMode = GameMode.SURVIVAL;
                    case "creative", "c", "1" -> gameMode = GameMode.CREATIVE;
                    case "adventure", "a", "2" -> gameMode = GameMode.ADVENTURE;
                    case "spectator", "sp", "3" -> gameMode = GameMode.SPECTATOR;
                    default -> {
                        MessagingHelper.sendMessage(sender, MessageType.ERROR, "Invalid gamemode '" + gamemode + "'. Valid options are: survival, creative, adventure, spectator.");
                        return;
                    }
                }

                Player targetPlayer = MinecraftServer.getConnectionManager().findOnlinePlayer(target);
                if (targetPlayer == null) {
                    MessagingHelper.sendMessage(sender, MessageType.ERROR, "Player with username '" + target + "' is not online.");
                    return;
                }
                targetPlayer.setGameMode(gameMode);
                MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Set gamemode of " + target + " to " + gamemode + ".");
            } else {
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Only players can use this command.");
            }
        }, gamemodeArg, targetArg);
    }
}