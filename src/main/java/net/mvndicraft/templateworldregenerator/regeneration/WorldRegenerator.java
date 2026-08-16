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
    private static final String CHUNKS_TO_SCHEDULE_AT_ONCE_CONFIG = "chunks_to_schedule_at_once";
    private static final long DEFAULT_MAX_MILLIS_PER_TICK = 50L;
    private static final int DEFAULT_CHUNKS_TO_SCHEDULE_AT_ONCE = 1000;
    private static final long MIN_REMAINING_MILLIS_PER_TICK = 5L;

    private final ConcurrentHashMap<Thread, TickBudget> tickBudgets = new ConcurrentHashMap<>();
    private final Set<ChunkCoordinate> regeneratedChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkCoordinate> regeneratingChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkCoordinate> runningChunks = ConcurrentHashMap.newKeySet();
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
        runningChunks.clear();
        scheduledRetryChunks.clear();
        tickBudgets.clear();
        regenerationJob = new RegenerationJob(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
        scheduleMoreChunks(regenerationJob);
    }

    public void stopRegeneration() {
        regenerationJob = null;
        regeneratedChunks.clear();
        regeneratingChunks.clear();
        runningChunks.clear();
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

    public int getChunksTotalCount() {
        RegenerationJob currentJob = regenerationJob;
        if (currentJob == null) {
            return 0;
        }
        return currentJob.totalChunks();
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
                && currentJob != null && currentJob.contains(chunkCoordinate) && !regeneratedChunks.contains(chunkCoordinate)) {
            if (TemplateWorldRegeneratorPlugin.getInstance().isTownOrRoad(chunk)) {
                finishRegeneration(currentJob, chunkCoordinate);
                return;
            }
            startRegeneration(currentJob, toWorld, chunkCoordinate);
        }
    }

    private synchronized void scheduleMoreChunks(RegenerationJob job) {
        if (regenerationJob != job) {
            return;
        }

        World toWorld = TemplateWorldRegeneratorPlugin.getInstance().getToWorld();
        if (toWorld == null) {
            return;
        }

        while (regenerationJob == job && getScheduledChunksCount() < getChunksToScheduleAtOnce()) {
            ChunkCoordinate chunkCoordinate = job.nextChunk();
            if (chunkCoordinate == null) {
                return;
            }
            scheduleChunk(job, toWorld, chunkCoordinate);
        }
    }

    private int getScheduledChunksCount() {
        return regeneratingChunks.size() + scheduledRetryChunks.size();
    }

    private int getChunksToScheduleAtOnce() {
        int configuredChunkCount = TemplateWorldRegeneratorPlugin.getInstance().getConfig()
                .getInt(CHUNKS_TO_SCHEDULE_AT_ONCE_CONFIG, DEFAULT_CHUNKS_TO_SCHEDULE_AT_ONCE);
        return Math.max(1, configuredChunkCount);
    }

    private void scheduleChunk(RegenerationJob job, World toWorld, ChunkCoordinate chunkCoordinate) {
        if (regenerationJob != job || regeneratedChunks.contains(chunkCoordinate)
                || scheduledRetryChunks.contains(chunkCoordinate) || !regeneratingChunks.add(chunkCoordinate)) {
            return;
        }

        Bukkit.getRegionScheduler().run(TemplateWorldRegeneratorPlugin.getInstance(), toWorld, chunkCoordinate.x(),
                chunkCoordinate.z(), task -> {
                    if (regenerationJob != job) {
                        return;
                    }
                    Chunk chunk = toWorld.getChunkAt(chunkCoordinate.x(), chunkCoordinate.z());
                    if (TemplateWorldRegeneratorPlugin.getInstance().isTownOrRoad(chunk)) {
                        finishRegeneration(job, chunkCoordinate);
                        return;
                    }
                    startRegeneration(job, toWorld, chunkCoordinate);
                });
    }

    private void startRegeneration(RegenerationJob job, World toWorld, ChunkCoordinate chunkCoordinate) {
        if (regenerationJob != job || regeneratedChunks.contains(chunkCoordinate)) {
            return;
        }
        if (!regeneratingChunks.contains(chunkCoordinate) && !regeneratingChunks.add(chunkCoordinate)) {
            return;
        }
        if (!runningChunks.add(chunkCoordinate)) {
            return;
        }
        scheduledRetryChunks.remove(chunkCoordinate);
        TemplateWorldRegeneratorPlugin.debug("Regenerating chunk " + chunkCoordinate.x() + " " + chunkCoordinate.z());
        new ChunkRegenerator(chunkCoordinate.x(), chunkCoordinate.z(),
                TemplateWorldRegeneratorPlugin.getInstance().getFromWorld(),
                TemplateWorldRegeneratorPlugin.getInstance().getToWorld(),
                this::canStartChunkUpdate,
                chunk -> !TemplateWorldRegeneratorPlugin.getInstance().isTownOrRoad(chunk),
                () -> retryLater(job, toWorld, chunkCoordinate),
                () -> finishRegeneration(job, chunkCoordinate),
                () -> finishRegeneration(job, chunkCoordinate)).run();
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
        runningChunks.remove(chunkCoordinate);
        if (!scheduledRetryChunks.add(chunkCoordinate)) {
            return;
        }
        Bukkit.getRegionScheduler().runDelayed(TemplateWorldRegeneratorPlugin.getInstance(), toWorld, chunkCoordinate.x(),
                chunkCoordinate.z(), task -> {
                    scheduledRetryChunks.remove(chunkCoordinate);
                    if (regenerationJob == job) {
                        scheduleChunk(job, toWorld, chunkCoordinate);
                    }
                }, 1L);
    }

    private void finishRegeneration(RegenerationJob job, ChunkCoordinate chunkCoordinate) {
        if (regenerationJob != job) {
            return;
        }
        regeneratedChunks.add(chunkCoordinate);
        regeneratingChunks.remove(chunkCoordinate);
        runningChunks.remove(chunkCoordinate);
        scheduledRetryChunks.remove(chunkCoordinate);
        scheduleMoreChunks(job);
    }

    private record TickBudget(int tick, long startedAtNanos) {}

    private static class RegenerationJob {
        private final int minChunkX;
        private final int minChunkZ;
        private final int maxChunkX;
        private final int maxChunkZ;
        private int nextChunkX;
        private int nextChunkZ;

        RegenerationJob(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
            this.minChunkX = minChunkX;
            this.minChunkZ = minChunkZ;
            this.maxChunkX = maxChunkX;
            this.maxChunkZ = maxChunkZ;
            this.nextChunkX = minChunkX;
            this.nextChunkZ = minChunkZ;
        }

        private boolean contains(ChunkCoordinate chunkCoordinate) {
            return chunkCoordinate.x() >= minChunkX && chunkCoordinate.x() <= maxChunkX && chunkCoordinate.z() >= minChunkZ
                    && chunkCoordinate.z() <= maxChunkZ;
        }

        private synchronized ChunkCoordinate nextChunk() {
            if (nextChunkX > maxChunkX) {
                return null;
            }

            ChunkCoordinate chunkCoordinate = new ChunkCoordinate(nextChunkX, nextChunkZ);
            if (nextChunkZ < maxChunkZ) {
                nextChunkZ++;
            } else {
                nextChunkZ = minChunkZ;
                nextChunkX++;
            }
            return chunkCoordinate;
        }

        private int totalChunks() {
            long chunkCount = ((long) maxChunkX - minChunkX + 1) * ((long) maxChunkZ - minChunkZ + 1);
            return chunkCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) chunkCount;
        }
    }
}
