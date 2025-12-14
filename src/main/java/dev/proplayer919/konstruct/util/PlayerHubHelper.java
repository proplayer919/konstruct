package dev.proplayer919.konstruct.util;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.hubs.HubData;
import dev.proplayer919.konstruct.hubs.HubRegistry;
import dev.proplayer919.konstruct.sidebar.SidebarData;
import dev.proplayer919.konstruct.sidebar.SidebarRegistry;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.attribute.Attribute;

public class PlayerHubHelper {
    public static void returnPlayerToHub(CustomPlayer player) {
        // Find the hub with the least players and send the player there
        HubData hubData = HubRegistry.getInstanceWithLowestPlayers();
        if (hubData != null) {
            movePlayerToHub(player, hubData);
        } else {
            player.kick("No hub instance available. Please try reconnecting later.");
        }
    }

    public static void movePlayerToHub(CustomPlayer player, HubData hubData) {
        // Find the player's current hub, if any, and remove them from it
        HubData currentHub = HubRegistry.getInstanceWithPlayer(player.getUuid());
        if (currentHub != null) {
            currentHub.getPlayers().remove(player);
        }

        // Update the player's sidebar
        SidebarData sidebarData = SidebarRegistry.getSidebarByPlayerId(player.getUuid());
        if (sidebarData != null) {
            sidebarData.setInstanceId(hubData.getId());
        }

        hubData.getPlayers().add(player);
        player.setInstance(hubData.getInstance());
        player.teleport(new Pos(0.5, 40, 0.5)); // Teleport to hub spawn point
        player.setGameMode(GameMode.SURVIVAL);
        resetPlayerAttributes(player);
    }

    public static void resetPlayerAttributes(CustomPlayer player) {
        Attribute.values().forEach(attribute -> {
            player.getAttribute(attribute).modifiers().forEach(modifier -> {
                player.getAttribute(attribute).modifiers().remove(modifier);
            });
        });

        player.setHealth((float) player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setAdditionalHearts(0);

        player.setFood(20);
        player.setFoodSaturation(7);

        player.setFireTicks(0);
        player.setGlowing(false);
        player.setAllowFlying(false);

        player.setLevel(0);
        player.setExp(0);
        player.clearEffects();

        player.getInventory().clear();
        player.resetTitle();
        player.tagHandler().updateContent(CompoundBinaryTag.empty());
    }
}
