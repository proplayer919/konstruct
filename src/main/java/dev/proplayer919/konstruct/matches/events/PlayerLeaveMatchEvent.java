package dev.proplayer919.konstruct.matches.events;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.matches.MatchData;
import lombok.Getter;
import net.minestom.server.event.trait.PlayerEvent;

@Getter
public class PlayerLeaveMatchEvent implements PlayerEvent {
    private final CustomPlayer player;
    private final MatchData matchData;

    public PlayerLeaveMatchEvent(CustomPlayer player, MatchData matchData) {
        this.player = player;
        this.matchData = matchData;
    }
}
