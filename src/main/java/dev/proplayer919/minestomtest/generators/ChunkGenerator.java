package dev.proplayer919.minestomtest.generators;

import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.generator.GenerationUnit;

public class ChunkGenerator {
    public static void generateUnit(GenerationUnit unit, Block material, int maxY) {
        final Point start = unit.absoluteStart();
        final Point size = unit.size();
        for (int x = 0; x < size.blockX(); x++) {
            for (int z = 0; z < size.blockZ(); z++) {
                for (int y = 0; y < Math.min((maxY + 1) - start.blockY(), size.blockY()); y++) {
                    unit.modifier().setBlock(start.add(x, y, z), material);
                }
            }
        }
    }
}
