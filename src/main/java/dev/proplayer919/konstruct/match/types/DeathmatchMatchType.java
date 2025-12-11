package dev.proplayer919.konstruct.match.types;

import dev.proplayer919.konstruct.generators.InstanceCreator;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import java.nio.file.Path;

public class DeathmatchMatchType extends MatchType {
    public DeathmatchMatchType() {
        int minPlayers = 2;
        int maxPlayers = 2;

        Pos bounds1 = new Pos(-150, 35, -150);
        Pos bounds2 = new Pos(150, 50, 150);

        super("deathmatch", "Deathmatch", maxPlayers, minPlayers, new Pos(0.5, 60, 0.5), new Pos(0.5, 80, 0.5), bounds1, bounds2);
    }

    public Instance getInstance() {
        // Use the correct path to the anvil world folder inside the project
        String anvilPath = Path.of("data", "arenas", "deathmatch1").toString();
        return InstanceCreator.createInstanceFromAnvil(anvilPath, true);
    }
}
