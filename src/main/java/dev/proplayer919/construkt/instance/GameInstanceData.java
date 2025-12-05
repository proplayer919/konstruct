package dev.proplayer919.construkt.instance;

import dev.proplayer919.construkt.instance.gameplayer.GamePlayerData;
import lombok.Getter;
import net.minestom.server.instance.Instance;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

@Getter
public class GameInstanceData extends InstanceData {
    private final UUID hostUUID;
    private final int maxPlayers;
    private final int minPlayers = 2;

    private final Collection<GamePlayerData> players = new HashSet<>();

    public GameInstanceData(String id, Instance instance, UUID hostUUID, int maxPlayers) {
        super(InstanceType.GAME, instance, id);
        this.hostUUID = hostUUID;
        this.maxPlayers = maxPlayers;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean hasEnoughPlayers() {
        return players.size() >= minPlayers;
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
