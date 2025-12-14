package dev.proplayer919.konstruct.matches;

import dev.proplayer919.konstruct.CustomPlayer;
import dev.proplayer919.konstruct.loot.ChestLootRegistry;
import dev.proplayer919.konstruct.util.PlayerInventoryBlockRegistry;
import lombok.Getter;
import lombok.Setter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
public class MatchData {
    private final UUID matchUUID = UUID.randomUUID();

    private final UUID hostUUID;
    private final String hostUsername;

    private final Collection<CustomPlayer> players;
    private final Instance lobbyInstance;
    private final Instance matchInstance;

    @Setter
    private MatchStatus status;

    private final ChestLootRegistry chestLootRegistry;
    private final PlayerInventoryBlockRegistry inventoryBlockRegistry;
    private final Date startTime = new Date(System.currentTimeMillis() + 300000); // Default to 5 minutes from now

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Pos lobbySpawn = new Pos(0.5, 40, 0.5);
    private final Pos spectatorSpawn = new Pos(0.5, 60, 0.5);

    private final int minPlayers = 2;
    private final int maxPlayers = 2;

    private final Pos buildingBounds1 = new Pos(-150, 35, -150);
    private final Pos buildingBounds2 = new Pos(150, 60, 150);

    public MatchData(CustomPlayer host, Instance lobbyInstance, Instance matchInstance) {
        this.hostUUID = host.getUuid();
        this.hostUsername = host.getUsername();

        this.players = new ArrayList<>();
        this.lobbyInstance = lobbyInstance;
        this.matchInstance = matchInstance;

        this.status = MatchStatus.WAITING;

        this.chestLootRegistry = new ChestLootRegistry();
        this.inventoryBlockRegistry = new PlayerInventoryBlockRegistry();
    }

    public boolean isPlayerAlive(CustomPlayer player) {
        return players.contains(player) && player.isAlive();
    }

    public Collection<CustomPlayer> getAlivePlayers() {
        Collection<CustomPlayer> alivePlayers = new ArrayList<>();
        for (CustomPlayer player : players) {
            if (player.isAlive()) {
                alivePlayers.add(player);
            }
        }
        return alivePlayers;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getAlivePlayerCount() {
        return getAlivePlayers().size();
    }

    public Collection<CustomPlayer> getSpectators() {
        Collection<CustomPlayer> spectators = new ArrayList<>();
        for (CustomPlayer player : players) {
            if (!player.isAlive()) {
                spectators.add(player);
            }
        }
        return spectators;
    }

    public int getSpectatorCount() {
        return getSpectators().size();
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public boolean hasEnoughPlayers() {
        return players.size() >= minPlayers;
    }

    public void addPlayer(CustomPlayer player) {
        this.players.add(player);
    }

    public void removePlayer(CustomPlayer player) {
        this.players.remove(player);
    }


}
