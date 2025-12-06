package dev.proplayer919.construkt.instance.match;

import dev.proplayer919.construkt.generators.InstanceCreator;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public class DeathmatchMatchType extends MatchType {
    public DeathmatchMatchType() {
        super("deathmatch", "Deathmatch", 16, 2, createInstance());
    }

    private static Instance createInstance() {
        return InstanceCreator.createSimpleInstanceContainer(Block.GRASS_BLOCK, Block.AMETHYST_BLOCK, true);
    }
}
