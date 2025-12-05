package dev.proplayer919.construkt.commands;

import dev.proplayer919.construkt.helpers.MessagingHelper;
import dev.proplayer919.construkt.permissions.PermissionRegistry;
import dev.proplayer919.construkt.permissions.PlayerPermissionRegistry;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class GameModeCommand extends Command {

    public GameModeCommand() {
        super("gamemode", "gm", "setgamemode", "setgm");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendAdminMessage(sender, "Usage: /gamemode <gamemode>"));

        var gamemodeArg = ArgumentType.String("gamemode");

        addSyntax((sender, context) -> {
            final String gamemode = context.get(gamemodeArg);
            if (sender instanceof Player player) {
                if (!PlayerPermissionRegistry.hasPermission(player, PermissionRegistry.getPermissionByNode("command.gamemode"))) {
                    MessagingHelper.sendPermissionMessage(sender, "You do not have permission to use this command.");
                    return;
                }

                switch (gamemode.toLowerCase()) {
                    case "survival", "s", "0" -> player.setGameMode(GameMode.SURVIVAL);
                    case "creative", "c", "1" -> player.setGameMode(GameMode.CREATIVE);
                    case "adventure", "a", "2" -> player.setGameMode(GameMode.ADVENTURE);
                    case "spectator", "sp", "3" -> player.setGameMode(GameMode.SPECTATOR);
                    default -> {
                        MessagingHelper.sendErrorMessage(sender, "Invalid gamemode. Valid options are: survival, creative, adventure, spectator.");
                        return;
                    }
                }

                MessagingHelper.sendAdminMessage(sender, "Your gamemode has been set to " + gamemode + ".");
            } else {
                MessagingHelper.sendErrorMessage(sender, "Only players can use this command.");
            }
        }, gamemodeArg);
    }
}