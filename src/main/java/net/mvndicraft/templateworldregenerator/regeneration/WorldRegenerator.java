package net.mvndicraft.templateworldregenerator.regeneration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.mvndicraft.templateworldregenerator.TemplateWorldRegeneratorPlugin;
import net.mvndicraft.templateworldregenerator.util.ChunkCoordinate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

public class WorldRegenerator {
    private static final String MAX_MILLIS_PER_TICK_CONFIG = "max_millis_per_tick";
    private static final long DEFAULT_MAX_MILLIS_PER_TICK = 50L;
    private static final long MIN_REMAINING_MILLIS_PER_TICK = 5L;

    private final ConcurrentHashMap<Thread, TickBudget> tickBudgets = new ConcurrentHashMap<>();
    private final Set<ChunkCoordinate> regeneratedChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkCoordinate> regeneratingChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkCoordinate> scheduledRetryChunks = ConcurrentHashMap.newKeySet();
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
        regeneratingChunks.clear();
        scheduledRetryChunks.clear();
        tickBudgets.clear();
        regenerationJob = new RegenerationJob(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
    }

    public void stopRegeneration() {
        regenerationJob = null;
        regeneratedChunks.clear();
        regeneratingChunks.clear();
        scheduledRetryChunks.clear();
        tickBudgets.clear();
    }

    public int getChunksToRegenerateCount() {
        RegenerationJob currentJob = regenerationJob;
        if (currentJob == null) {
            return 0;
        }
        return Math.max(0, currentJob.totalChunks() - regeneratedChunks.size());
    }

    public int getChunksRegeneratedCount() {
        RegenerationJob currentJob = regenerationJob;
        if (currentJob == null) {
            return 0;
        }
        return Math.max(0, regeneratedChunks.size());
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
                && currentJob != null && currentJob.contains(chunkCoordinate) && !regeneratedChunks.contains(chunkCoordinate)
                && !regeneratingChunks.contains(chunkCoordinate)
                // it's not a town or a road
                && !TemplateWorldRegeneratorPlugin.getInstance().isTownOrRoad(chunk)) {
            TemplateWorldRegeneratorPlugin.debug("Regenerating chunk " + chunk.getX() + " " + chunk.getZ());
            if (regeneratingChunks.add(chunkCoordinate)) {
                scheduledRetryChunks.remove(chunkCoordinate);
                new ChunkRegenerator(chunk.getX(), chunk.getZ(), TemplateWorldRegeneratorPlugin.getInstance().getFromWorld(),
                        TemplateWorldRegeneratorPlugin.getInstance().getToWorld(), this::canStartChunkUpdate,
                        () -> retryLater(currentJob, toWorld, chunkCoordinate), () -> finishRegeneration(currentJob, chunkCoordinate))
                                .run();
            }
        }
    }

    private boolean canStartChunkUpdate() {
        int currentTick = Bukkit.getCurrentTick();
        long now = System.nanoTime();
        Thread currentThread = Thread.currentThread();
        TickBudget tickBudget = tickBudgets.get(currentThread);
        if (tickBudget == null || tickBudget.tick() != currentTick) {
            tickBudgets.put(currentThread, new TickBudget(currentTick, now));
            return true;
        }

        long configuredMaxMillis = TemplateWorldRegeneratorPlugin.getInstance().getConfig().getLong(MAX_MILLIS_PER_TICK_CONFIG,
                DEFAULT_MAX_MILLIS_PER_TICK);
        long maxMillisPerTick = Math.max(MIN_REMAINING_MILLIS_PER_TICK, configuredMaxMillis);
        long elapsedNanos = now - tickBudget.startedAtNanos();
        long maxElapsedNanos = TimeUnit.MILLISECONDS.toNanos(maxMillisPerTick - MIN_REMAINING_MILLIS_PER_TICK);
        return elapsedNanos < maxElapsedNanos;
    }

    private void retryLater(RegenerationJob job, World toWorld, ChunkCoordinate chunkCoordinate) {
        if (regenerationJob != job) {
            return;
        }
        regeneratingChunks.remove(chunkCoordinate);
        if (!scheduledRetryChunks.add(chunkCoordinate)) {
            return;
        }
        Bukkit.getRegionScheduler().runDelayed(TemplateWorldRegeneratorPlugin.getInstance(), toWorld, chunkCoordinate.x(),
                chunkCoordinate.z(), task -> {
                    scheduledRetryChunks.remove(chunkCoordinate);
                    if (regenerationJob == job) {
                        regenerateIfNeeded(toWorld.getChunkAt(chunkCoordinate.x(), chunkCoordinate.z()));
                    }
                }, 1L);
    }

    private void finishRegeneration(RegenerationJob job, ChunkCoordinate chunkCoordinate) {
        if (regenerationJob != job) {
            return;
        }
        regeneratedChunks.add(chunkCoordinate);
        regeneratingChunks.remove(chunkCoordinate);
        scheduledRetryChunks.remove(chunkCoordinate);
    }

    private record TickBudget(int tick, long startedAtNanos) {}

    private record RegenerationJob(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        private boolean contains(ChunkCoordinate chunkCoordinate) {
            return chunkCoordinate.x() >= minChunkX && chunkCoordinate.x() <= maxChunkX && chunkCoordinate.z() >= minChunkZ
                    && chunkCoordinate.z() <= maxChunkZ;
        }

        private int totalChunks() {
            long chunkCount = ((long) maxChunkX - minChunkX + 1) * ((long) maxChunkZ - minChunkZ + 1);
            return chunkCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) chunkCount;
        }
    }
}
