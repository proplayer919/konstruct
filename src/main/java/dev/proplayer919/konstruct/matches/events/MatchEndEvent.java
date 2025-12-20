package dev.proplayer919.konstruct.matches.events;

import dev.proplayer919.konstruct.matches.MatchData;
import lombok.Getter;
import net.minestom.server.event.Event;

@Getter
public class MatchEndEvent implements Event {
    private final MatchData matchData;

    public MatchEndEvent(MatchData matchData) {
        this.matchData = matchData;
    }
}
