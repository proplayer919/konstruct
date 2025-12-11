package dev.proplayer919.konstruct.util;

import dev.proplayer919.konstruct.instance.HubInstanceData;
import dev.proplayer919.konstruct.instance.HubInstanceRegistry;
import dev.proplayer919.konstruct.messages.MessageType;
import dev.proplayer919.konstruct.messages.MessagingHelper;
import dev.proplayer919.konstruct.sidebar.SidebarData;
import dev.proplayer919.konstruct.sidebar.SidebarRegistry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;

public class PlayerHubHelper {
    public static void returnPlayerToHub(Player player) {
        // Find the hub with the least players and send the player there
        HubInstanceData hubInstanceData = HubInstanceRegistry.getInstanceWithLowestPlayers();
        if (hubInstanceData != null) {
            movePlayerToHub(player, hubInstanceData);
        } else {
            player.kick("No hub instance available. Please try reconnecting later.");
        }
    }

    public static void movePlayerToHub(Player player, HubInstanceData hubInstanceData) {
        // Find the player's current hub, if any, and remove them from it
        HubInstanceData currentHub = HubInstanceRegistry.getInstanceWithPlayer(player.getUuid());
        if (currentHub != null) {
            currentHub.getPlayers().remove(player);
        }

        // Update the player's sidebar
        SidebarData sidebarData = SidebarRegistry.getSidebarByPlayerId(player.getUuid());
        if (sidebarData != null) {
            sidebarData.setInstanceId(hubInstanceData.getId());
        }

        hubInstanceData.getPlayers().add(player);
        player.setInstance(hubInstanceData.getInstance());
        player.teleport(new Pos(0.5, 40, 0.5)); // Teleport to hub spawn point
        player.setGameMode(GameMode.SURVIVAL);
        resetPlayerAttributes(player);
    }

    public static void resetPlayerAttributes(Player player) {
        player.setHealth(20);
        player.setAdditionalHearts(0);
        player.setFoodSaturation(7);
        player.setFood(20);
        player.getInventory().clear();
        player.clearEffects();
    }
}
