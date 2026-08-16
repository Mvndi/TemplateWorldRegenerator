package net.mvndicraft.templateworldregenerator.regeneration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.mvndicraft.templateworldregenerator.TemplateWorldRegeneratorPlugin;
import net.mvndicraft.templateworldregenerator.util.ChunkCoordinate;
import org.bukkit.Chunk;
import org.bukkit.World;

public class WorldRegenerator {
    private final Set<ChunkCoordinate> regeneratedChunks = ConcurrentHashMap.newKeySet();
    private volatile RegenerationJob regenerationJob;

    public void addAreaToChunksToRegenerate(int x1, int z1, int x2, int z2) {
        int chunkX1 = Math.floorDiv(x1, 16);
        int chunkZ1 = Math.floorDiv(z1, 16);
        int chunkX2 = Math.floorDiv(x2, 16);
        int chunkZ2 = Math.floorDiv(z2, 16);

        int maxChunkX = Math.max(chunkX1, chunkX2);
        int maxChunkZ = Math.max(chunkZ1, chunkZ2);
        int minChunkX = Math.min(chunkX1, chunkX2);
        int minChunkZ = Math.min(chunkZ1, chunkZ2);

        regeneratedChunks.clear();
        regenerationJob = new RegenerationJob(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
    }
    public void stopRegeneration() {
        regenerationJob = null;
        regeneratedChunks.clear();
    }

    public int getChunksToRegenerateCount() {
        RegenerationJob currentJob = regenerationJob;
        if (currentJob == null) {
            return 0;
        }
        return Math.max(0, currentJob.totalChunks() - regeneratedChunks.size());
    }

    public void regenerateIfNeeded(Chunk chunk) {
        TemplateWorldRegeneratorPlugin.debug("regenerateIfNeeded runned for chunk " + chunk.getX() + " " + chunk.getZ());
        ChunkCoordinate chunkCoordinate = new ChunkCoordinate(chunk.getX(), chunk.getZ());
        World toWorld = chunk.getWorld();
        RegenerationJob currentJob = regenerationJob;
        if (TemplateWorldRegeneratorPlugin.getInstance().getFromWorld() != null // source exist
                // it's the to world where to replace
                && toWorld.getUID().equals(TemplateWorldRegeneratorPlugin.getInstance().getToWorld().getUID())
                // it a chunk to regenerate
                && currentJob != null
                && currentJob.contains(chunkCoordinate)
                && !regeneratedChunks.contains(chunkCoordinate)
                // it's not a town or a road
                && !TemplateWorldRegeneratorPlugin.getInstance().isTownOrRoad(chunk)) {
            TemplateWorldRegeneratorPlugin.debug("Regenerating chunk " + chunk.getX() + " " + chunk.getZ());
            if (regeneratedChunks.add(chunkCoordinate)) {
                new ChunkRegenerator(chunk.getX(), chunk.getZ(), TemplateWorldRegeneratorPlugin.getInstance().getFromWorld(),
                        TemplateWorldRegeneratorPlugin.getInstance().getToWorld()).run();
            }
        }
    }

    private record RegenerationJob(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        private boolean contains(ChunkCoordinate chunkCoordinate) {
            return chunkCoordinate.x() >= minChunkX
                    && chunkCoordinate.x() <= maxChunkX
                    && chunkCoordinate.z() >= minChunkZ
                    && chunkCoordinate.z() <= maxChunkZ;
        }

        private int totalChunks() {
            long chunkCount = ((long) maxChunkX - minChunkX + 1) * ((long) maxChunkZ - minChunkZ + 1);
            return chunkCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) chunkCount;
        }
    }
}
