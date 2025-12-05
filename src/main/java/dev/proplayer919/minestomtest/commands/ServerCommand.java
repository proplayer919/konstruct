package dev.proplayer919.minestomtest.commands;

import dev.proplayer919.minestomtest.generators.InstanceCreator;
import dev.proplayer919.minestomtest.helpers.MessagingHelper;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.util.Map;

public class ServerCommand extends Command {
    public ServerCommand(Map<String, Instance> instanceMap) {
        super("server", "goto");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player) {
                MessagingHelper.sendServerMessage(sender, "Usage: /server <server-id>");
            }
        });

        var serverIdArg = ArgumentType.String("server-id").setDefaultValue("hub");

        addSyntax((sender, context) -> {
            final String serverId = context.get(serverIdArg);

            if (sender instanceof Player player) {
                if (instanceMap.containsKey(serverId)) {
                    // Send player to existing server
                    Instance instance = instanceMap.get(serverId);

                    if (player.getInstance() == instance) {
                        MessagingHelper.sendServerMessage(player, "You are already on the server with ID '" + serverId + "'.");
                        return;
                    }

                    player.setInstance(instance);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(new Pos(0.5, 40, 0.5));
                    MessagingHelper.sendServerMessage(player, "You have been transferred to the existing server with ID '" + serverId + "'.");
                    return;
                }

                InstanceContainer newInstance = InstanceCreator.createSimpleInstanceContainer(Block.STONE, Block.DIAMOND_BLOCK, true);
                instanceMap.put(serverId, newInstance);
                player.setInstance(newInstance);
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(new Pos(0.5, 40, 0.5));
                MessagingHelper.sendServerMessage(player, "A new server with ID '" + serverId + "' has been created and you have been transferred to it.");
            } else {
                MessagingHelper.sendErrorMessage(sender, "This command can only be used by players.");
            }
        }, serverIdArg);
    }
}
