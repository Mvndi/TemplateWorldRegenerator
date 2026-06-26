// package net.mvndicraft.templateworldregenerator.worldtask;

// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.Semaphore;
// import java.util.function.Consumer;
// import org.bukkit.Chunk;
// import org.bukkit.World;

// public class WorldTask {
// private final ChunkSupplierType type;
// private final World world;
// private final int minChunkX;
// private final int maxChunkX;
// private final int minChunkZ;
// private final int maxChunkZ;
// private int currentChunkX;
// private int currentChunkZ;
// private static final int MAX_WORKING_COUNT = 50;
// private boolean stopped;

// public WorldTask(World world, int chunkX1, int chunkZ1, int chunkX2, int chunkZ2, ChunkSupplierType type) {
// this.type = type;
// this.world = world;
// if (chunkX1 > chunkX2) {
// this.minChunkX = chunkX2;
// this.maxChunkX = chunkX1;
// } else {
// this.minChunkX = chunkX1;
// this.maxChunkX = chunkX2;
// }
// if (chunkZ1 > chunkZ2) {
// this.minChunkZ = chunkZ2;
// this.maxChunkZ = chunkZ1;
// } else {
// this.minChunkZ = chunkZ1;
// this.maxChunkZ = chunkZ2;
// }
// this.currentChunkX = minChunkX;
// this.currentChunkZ = minChunkZ;
// this.stopped = false;
// }

// public boolean hasNext() { return currentChunkX <= maxChunkX || currentChunkZ <= maxChunkZ; }
// public boolean isFinished() { return !hasNext(); }

// public void stop() { stopped = true; }

// public CompletableFuture<Chunk> next() {
// Chunk chunk = world.getChunkAt(currentChunkX, currentChunkZ);
// currentChunkX += 1;
// if (currentChunkX > maxChunkX) {
// currentChunkX = minChunkX;
// currentChunkZ += 1;
// }
// return CompletableFuture.completedFuture(chunk);
// }

// public void runAll(Consumer<Chunk> consumer) {
// final Semaphore working = new Semaphore(MAX_WORKING_COUNT);
// while (!stopped && hasNext()) {
// try {
// working.acquire();
// } catch (InterruptedException e) {
// Thread.currentThread().interrupt();
// stop();
// break;
// }

// Chunk chunk = world.getChunkAt(currentChunkX, currentChunkZ);
// consumer.accept(chunk);
// currentChunkX += 1;
// if (currentChunkX > maxChunkX) {
// currentChunkX = minChunkX;
// currentChunkZ += 1;
// }
// working.release();
// }
// }


// public enum ChunkSupplierType {
// RANDOM, RANDOM_BY_REGION, SEQUENTIAL
// // TODO RANDOM & RANDOM_BY_REGION later.
// }
// }