package dev.proplayer919.konstruct;

import dev.proplayer919.konstruct.matches.PlayerStatus;
import lombok.Getter;
import lombok.Setter;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;

@Getter
public class CustomPlayer extends Player {
    @Setter
    private PlayerStatus playerStatus;

    @Setter
    private boolean frozen;

    public CustomPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    public boolean isAlive() {
        return this.playerStatus == PlayerStatus.ALIVE;
    }
}
