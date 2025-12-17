package dev.proplayer919.konstruct.commands.admin;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.permissions.PlayerPermissionRegistry;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.Objects;

public class GiveCommand extends Command {

    public GiveCommand() {
        super("give", "giveme", "getitem", "get", "i", "item", "giveitem");

        // Executed if no other executor can be used
        setDefaultExecutor((sender, context) -> MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Usage: /give <target> <item> [amount]"));

        var targetArg = ArgumentType.String("target").setSuggestionCallback((sender, context, suggestion) -> MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> suggestion.addEntry(new SuggestionEntry(player.getUsername()))));
        var itemIdArg = ArgumentType.String("item-id");
        var amountArg = ArgumentType.Integer("amount").setDefaultValue(1);

        addSyntax((sender, context) -> {
            final String targetName = context.get(targetArg);
            final String itemId = context.get(itemIdArg);
            final int amount = context.get(amountArg);
            if (sender instanceof CustomPlayer player) {
                if (!PlayerPermissionRegistry.hasPermission(player, "command.gamemode")) {
                    MessagingHelper.sendMessage(sender, MessageType.PERMISSION, "You do not have permission to use this command.");
                    return;
                }

                try {
                    ItemStack itemStack = ItemStack.of(Objects.requireNonNull(Material.fromKey(itemId)), amount);
                    Player target = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(targetName);
                    if (target == null) {
                        MessagingHelper.sendMessage(sender, MessageType.ERROR, "Player '" + targetName + "' not found.");
                        return;
                    }
                    target.getInventory().addItemStack(itemStack);
                    MessagingHelper.sendMessage(sender, MessageType.ADMIN, "Gave " + targetName + " " + amount + "x of item " + itemId);
                } catch (Exception e) {
                    MessagingHelper.sendMessage(sender, MessageType.ERROR, "Invalid item ID: " + itemId);
                }
            } else {
                MessagingHelper.sendMessage(sender, MessageType.ERROR, "Only players can use this command.");
            }
        }, targetArg, itemIdArg, amountArg);
    }
}