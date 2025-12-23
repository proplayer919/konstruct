package dev.proplayer919.konstruct.messages;

import dev.proplayer919.konstruct.Constants;
import dev.proplayer919.konstruct.matches.modifiers.Modifier;
import dev.proplayer919.konstruct.util.DateStringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

public final class MatchMessages {
    private MatchMessages() {}

    public static Component createMatchAdvertiseMessage(UUID matchUUID, String hostName, Date startTime) {
        String formattedStartTime = DateStringUtility.formatDuration(startTime.getTime() - System.currentTimeMillis(), true);
        ClickEvent clickEvent = ClickEvent.runCommand("/join " + matchUUID.toString());

        return Component.text(hostName, Constants.BRAND_COLOUR_ALT_1)
                .append(Component.text(" is hosting a match, starting in ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(formattedStartTime, Constants.BRAND_COLOUR_ALT_2))
                .append(Component.text("! ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text("JOIN", Constants.BRAND_COLOUR_ALT_1).decorate(TextDecoration.BOLD).clickEvent(clickEvent));
    }

    public static Component createPlayerJoinedMessage(String playerName, int currentPlayers, int maxPlayers) {
        return Component.text(playerName, Constants.BRAND_COLOUR_ALT_1)
                .append(Component.text(" has joined the match! (", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(currentPlayers, Constants.BRAND_COLOUR_ALT_2))
                .append(Component.text("/", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(maxPlayers, Constants.BRAND_COLOUR_ALT_2))
                .append(Component.text(")", Constants.BRAND_COLOUR_PRIMARY_2));
    }

    public static Component createPlayerLeftMessage(String playerName, int currentPlayers, int maxPlayers) {
        return Component.text(playerName, Constants.BRAND_COLOUR_ALT_1)
                .append(Component.text(" has left the match! (", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(currentPlayers, Constants.BRAND_COLOUR_ALT_2))
                .append(Component.text("/", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(maxPlayers, Constants.BRAND_COLOUR_ALT_2))
                .append(Component.text(")", Constants.BRAND_COLOUR_PRIMARY_2));
    }

    public static Component createPreMatchCountdownMessage(int secondsLeft) {
        return Component.text("Match starting in ", Constants.BRAND_COLOUR_PRIMARY_2)
                .append(Component.text(secondsLeft + " seconds!", Constants.BRAND_COLOUR_PRIMARY_1));
    }

    public static Component createMatchTooLittlePlayersMessage(int minPlayers) {
        return Component.text("Match cannot start: at least ", Constants.BRAND_COLOUR_PRIMARY_2)
                .append(Component.text(minPlayers, Constants.BRAND_COLOUR_ALT_2))
                .append(Component.text(" players are required. You will be transferred back to a hub.", Constants.BRAND_COLOUR_PRIMARY_2));
    }

    public static Component createCountdownMessage(Date startTime, int currentPlayers, int minPlayers) {
        long millisecondsLeft = startTime.getTime() - System.currentTimeMillis();
        String formattedCountdown = DateStringUtility.formatDuration(millisecondsLeft, true);
        Component message = Component.text("Match starting in ", Constants.BRAND_COLOUR_PRIMARY_2)
                .append(Component.text(formattedCountdown + "!", Constants.BRAND_COLOUR_PRIMARY_1));
        int needed = Math.max(0, minPlayers - currentPlayers);
        if (needed > 0) {
            String playerWord = needed == 1 ? " player" : " players";
            message = message.append(Component.text(" We need ", Constants.BRAND_COLOUR_PRIMARY_2))
                    .append(Component.text(needed + playerWord, Constants.BRAND_COLOUR_ALT_2))
                    .append(Component.text(" to start the match.", Constants.BRAND_COLOUR_PRIMARY_2));
        }
        return message;
    }

    public static Component createPlayerDisconnectMessage(String playerName, int remainingPlayers) {
        return Component.text(playerName, Constants.BRAND_COLOUR_ALT_1)
                .append(Component.text(" left the match and was eliminated. ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(remainingPlayers + " players remaining.", Constants.BRAND_COLOUR_ALT_2));
    }

    public static Component createPlayerVoidMessage(String playerName, int remainingPlayers) {
        return Component.text(playerName, Constants.BRAND_COLOUR_ALT_1)
                .append(Component.text(" fell into the abyss. ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(remainingPlayers + " players remaining.", Constants.BRAND_COLOUR_ALT_2));
    }

    public static Component createPlayerEliminatedMessage(String playerName, String killerName, int remainingPlayers) {
        return Component.text(playerName, Constants.BRAND_COLOUR_ALT_1)
                .append(Component.text(" was eliminated by ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(killerName, Constants.BRAND_COLOUR_ALT_1))
                .append(Component.text(". ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(remainingPlayers + " players remaining.", Constants.BRAND_COLOUR_ALT_2));
    }

    public static Component createKillerMessage(String playerName) {
        return Component.text("☠ KILL ", Constants.BRAND_COLOUR_ALT_2)
                .append(Component.text("on ", Constants.BRAND_COLOUR_ALT_1))
                .append(Component.text(playerName, Constants.BRAND_COLOUR_PRIMARY_2));
    }

    public static Component createWinnerMessage(String playerName) {
        return Component.text("⭐ WINNER ", Constants.BRAND_COLOUR_ALT_1)
                .append(Component.text("is ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(playerName, Constants.BRAND_COLOUR_ALT_1));
    }

    public static Component createMOTDMessage(String hostUsername, String mapName, int playerCount, int maxPlayers, Collection<Modifier> activeModifiers) {
        Component modifiersComponent = Component.text("None", Constants.BRAND_COLOUR_ALT_2);
        if (activeModifiers != null && !activeModifiers.isEmpty()) {
            Component comp = Component.empty();
            Iterator<Modifier> it = activeModifiers.iterator();
            while (it.hasNext()) {
                Modifier mod = it.next();
                Component name = Component.text(mod.getName(), Constants.BRAND_COLOUR_ALT_1)
                        .hoverEvent(HoverEvent.showText(Component.text(mod.getDescription(), Constants.BRAND_COLOUR_PRIMARY_2)));
                comp = comp.append(name);
                if (it.hasNext()) {
                    comp = comp.append(Component.text(", ", Constants.BRAND_COLOUR_PRIMARY_2));
                }
            }
            modifiersComponent = comp;
        }

        return Component.text("PLAYING DEATHMATCH\n", Constants.BRAND_COLOUR_ALT_1).decorate(TextDecoration.BOLD)
                .append(Component.text("Map: ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(mapName + "\n", Constants.BRAND_COLOUR_ALT_1))
                .append(Component.text("Hosted by: ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(hostUsername + "\n", Constants.BRAND_COLOUR_ALT_1))
                .append(Component.text("Players: ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(Component.text(playerCount + "/" + maxPlayers + "\n", Constants.BRAND_COLOUR_ALT_2))
                .append(Component.text("Modifiers: ", Constants.BRAND_COLOUR_PRIMARY_2))
                .append(modifiersComponent)
                .append(Component.text("\nʀᴜɴ ʙʏ ᴋᴏɴѕᴛʀᴜᴄᴛ", Constants.BRAND_COLOUR_PRIMARY_2));
    }
}
