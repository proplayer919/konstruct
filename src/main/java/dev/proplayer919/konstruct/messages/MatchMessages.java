package dev.proplayer919.konstruct.messages;

import dev.proplayer919.konstruct.Constants;
import dev.proplayer919.konstruct.util.DateStringUtility;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Date;
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
}
