package dev.proplayer919.konstruct.match.types;

import lombok.Getter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;

@Getter
public class MatchType {
    private final String id;
    private final String name;
    private final int maxPlayers;
    private final int minPlayers;
    private final Pos spectatorSpawn;
    private final Pos waitingSpawn;

    public MatchType(String id, String name, int maxPlayers, int minPlayers, Pos spectatorSpawn, Pos waitingSpawn) {
        this.id = id;
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.minPlayers = minPlayers;
        this.spectatorSpawn = spectatorSpawn;
        this.waitingSpawn = waitingSpawn;
    }

    public static Pos getPointOnCircle(Pos center, double radius, int numerator, int denominator) {
        double angleRadians = 2 * Math.PI * numerator / denominator;
        double x = center.x() + radius * Math.cos(angleRadians);
        double z = center.z() + radius * Math.sin(angleRadians);
        return center.withX(x).withZ(z);
    }

    public Pos getSpawnPointForPlayer(int playerIndex, int playerAmount) {
        // Spread players out in a circle around the center spawn point
        Pos centerSpawn = new Pos(0.5, 40, 0.5);

        Pos point = getPointOnCircle(centerSpawn, 30, playerIndex, playerAmount);

        // Compute yaw and pitch so the player looks at the center
        double dx = centerSpawn.x() - point.x();
        double dz = centerSpawn.z() - point.z();
        double dy = centerSpawn.y() - point.y();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));

        return point.withYaw(yaw).withPitch(pitch);
    }

    public Instance getInstance() {
        return null;
    }

}
