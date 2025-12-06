package dev.proplayer919.construkt.instance.match;

import lombok.Getter;
import net.minestom.server.instance.Instance;

@Getter
public class MatchType {
    private final String id;
    private final String name;
    private final int maxPlayers;
    private final int minPlayers;
    private final Instance instance;

    public MatchType(String id, String name, int maxPlayers, int minPlayers, Instance instance) {
        this.id = id;
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.minPlayers = minPlayers;
        this.instance = instance;
    }

}
