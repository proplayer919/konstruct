package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.editor.EditorSession;
import dev.proplayer919.konstruct.editor.EditorSessionRegistry;
import dev.proplayer919.konstruct.instance.InstanceLoader;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public class EditorCommand extends Command {

    public EditorCommand() {
        super("editor", "editormode", "buildermode");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Usage: /editor <map-path>"));

        var mapPathArg = ArgumentType.String("map-path").setSuggestionCallback((sender, context, suggestion) -> {
            // For each folder in data/maps, add a suggestion
            Path mapsPath = Path.of("data", "maps");
            File mapsDir = mapsPath.toFile();
            if (mapsDir.exists() && mapsDir.isDirectory()) {
                // List all directories in data/maps & data/maps/arenas, but not the data/maps/arenas folder itself
                File[] mapFolders = mapsDir.listFiles(File::isDirectory);
                if (mapFolders != null) {
                    for (File folder : mapFolders) {
                        if (Objects.equals(folder.getName(), "arenas")) continue;
                        suggestion.addEntry(new SuggestionEntry(folder.getName()));
                    }
                }

                File arenasPath = mapsPath.resolve("arenas").toFile();
                if (arenasPath.exists() && arenasPath.isDirectory()) {
                    File[] arenaFolders = arenasPath.listFiles(File::isDirectory);
                    if (arenaFolders != null) {
                        for (File folder : arenaFolders) {
                            suggestion.addEntry(new SuggestionEntry("arenas/" + folder.getName()));
                        }
                    }
                }
            }
        });

        addSyntax((sender, context) -> {
            final String mapPath = context.get("map-path");
            if (sender instanceof CustomPlayer player) {
                if (!PlayerPermissionRegistry.hasPermission(player, "command.editor")) {
                    MessagingHelper.sendMessage(sender, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }

                // Load the map
                Instance mapInstance = InstanceLoader.loadAnvilInstance("data/maps/" + mapPath, false);

                EditorSession editorSession = new EditorSession(mapInstance, player);
                EditorSessionRegistry.addSession(editorSession);

                player.setInstance(mapInstance);
                player.teleport(new Pos(0.5, 50, 0.5));
                player.setGameMode(GameMode.CREATIVE);
                player.setFlying(true);
            } else {
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Only players can use this command.");
            }
        }, mapPathArg);
    }
}