package dev.proplayer919.minestomtest.generators;

import dev.proplayer919.minestomtest.Main;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.ChunkRange;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class InstanceCreator {
    public static @NotNull InstanceContainer createSimpleInstanceContainer(Block block, Block spawnBlock, boolean pvpEnabled) {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

        // Set the ChunkGenerator
        instanceContainer.setGenerator(unit -> ChunkGenerator.generateUnit(unit, block, 39));

        // Setup lighting
        instanceContainer.setChunkSupplier(LightingChunk::new);

        var chunks = new ArrayList<CompletableFuture<Chunk>>();
        ChunkRange.chunksInRange(0, 0, 32, (x, z) -> chunks.add(instanceContainer.loadChunk(x, z)));

        CompletableFuture.runAsync(() -> {
            CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new)).join();
            LightingChunk.relight(instanceContainer, instanceContainer.getChunks());
        });

        instanceContainer.setBlock(0, 39, 0, spawnBlock);

        // Add PvP feature if enabled
        if (pvpEnabled) {
            // instanceContainer.eventNode().addChild(Main.modernVanilla.createNode());
        }

        return instanceContainer;
    }
}
