package dev.proplayer919.construkt.instance.gameplayer;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class GamePlayerData {
    private final UUID uuid;

    @Setter
    private GamePlayerStatus status = GamePlayerStatus.ALIVE;

    public GamePlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isAlive() {
        return status == GamePlayerStatus.ALIVE;
    }

    public boolean isDead() {
        return status == GamePlayerStatus.DEAD;
    }

    public boolean isSpectating() {
        return status == GamePlayerStatus.SPECTATOR;
    }

}
