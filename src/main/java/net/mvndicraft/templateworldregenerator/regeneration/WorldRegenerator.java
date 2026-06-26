package net.mvndicraft.templateworldregenerator.regeneration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.mvndicraft.templateworldregenerator.TemplateWorldRegeneratorPlugin;
import net.mvndicraft.templateworldregenerator.util.ChunkCoordinate;
import org.bukkit.Chunk;
import org.bukkit.World;

public class WorldRegenerator {
    private Set<ChunkCoordinate> chunksToRegenerate = ConcurrentHashMap.newKeySet();

    public void addAreaToChunksToRegenerate(int x1, int z1, int x2, int z2) {
        int chunkX1 = x1 / 16;
        int chunkZ1 = z1 / 16;
        int chunkX2 = x2 / 16;
        int chunkZ2 = z2 / 16;

        int maxChunkX = Math.max(chunkX1, chunkX2);
        int maxChunkZ = Math.max(chunkZ1, chunkZ2);
        int minChunkX = Math.min(chunkX1, chunkX2);
        int minChunkZ = Math.min(chunkZ1, chunkZ2);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunksToRegenerate.add(new ChunkCoordinate(chunkX, chunkZ));
            }
        }
    }
    public void stopRegeneration() { chunksToRegenerate.clear(); }

    public int getChunksToRegenerateCount() { return chunksToRegenerate.size(); }

    public void regenerateIfNeeded(Chunk chunk) {
        ChunkCoordinate chunkCoordinate = new ChunkCoordinate(chunk.getX(), chunk.getZ());
        World fromWorld = chunk.getWorld();
        if (fromWorld.getUID() == TemplateWorldRegeneratorPlugin.getInstance().getFromWorld().getUID()
                && chunksToRegenerate.contains(chunkCoordinate) && TemplateWorldRegeneratorPlugin.getInstance().isTownOrRoad(chunk)) {
            chunksToRegenerate.remove(chunkCoordinate);
            new ChunkRegenerator(chunk.getX(), chunk.getZ(), fromWorld, TemplateWorldRegeneratorPlugin.getInstance().getToWorld()).run();
        }
    }
}
