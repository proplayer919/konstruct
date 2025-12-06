package dev.proplayer919.construkt.instance;

import dev.proplayer919.construkt.instance.gameplayer.GamePlayerData;
import dev.proplayer919.construkt.instance.match.MatchType;
import lombok.Getter;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

@Getter
public class GameInstanceData extends InstanceData {
    private final UUID hostUUID;
    private final MatchType matchType;

    private final Collection<GamePlayerData> players = new HashSet<>();

    public GameInstanceData(String id, UUID hostUUID, MatchType matchType) {
        super(InstanceType.GAME, matchType.getInstance(), id);
        this.hostUUID = hostUUID;
        this.matchType = matchType;
    }

    public boolean isFull() {
        return players.size() >= matchType.getMaxPlayers();
    }

    public boolean hasEnoughPlayers() {
        return players.size() >= matchType.getMinPlayers();
    }

    public Collection<GamePlayerData> getAlivePlayers() {
        Collection<GamePlayerData> alivePlayers = new HashSet<>();
        for (GamePlayerData player : players) {
            if (player.isAlive()) {
                alivePlayers.add(player);
            }
        }
        return alivePlayers;
    }

    public Collection<GamePlayerData> getDeadPlayers() {
        Collection<GamePlayerData> deadPlayers = new HashSet<>();
        for (GamePlayerData player : players) {
            if (player.isDead()) {
                deadPlayers.add(player);
            }
        }
        return deadPlayers;
    }

    public Collection<GamePlayerData> getSpectators() {
        Collection<GamePlayerData> spectators = new HashSet<>();
        for (GamePlayerData player : players) {
            if (player.isSpectating()) {
                spectators.add(player);
            }
        }
        return spectators;
    }

}
