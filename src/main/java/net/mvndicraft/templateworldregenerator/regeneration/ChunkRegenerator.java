package net.mvndicraft.templateworldregenerator.regeneration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.mvndicraft.templateworldregenerator.TemplateWorldRegeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;

public record ChunkRegenerator(int chunkX, int chunkZ, World from, World to) {

    public void run() {
        Bukkit.getRegionScheduler().run(TemplateWorldRegeneratorPlugin.getInstance(), from(), chunkX(), chunkZ(), t -> {
            TemplateWorldRegeneratorPlugin.debug("Inside from world");
            Chunk chunkFrom = from().getChunkAt(chunkX(), chunkZ());
            ChunkSnapshot snapshotFrom = chunkFrom.getChunkSnapshot();
            TemplateWorldRegeneratorPlugin.debug("snapshotFrom created");
            Map<EntitySnapshot, Location> entitySnapshotsFrom = Arrays.stream(chunkFrom.getEntities())
                    .collect(Collectors.toMap(Entity::createSnapshot, Entity::getLocation));
            applySnapshot(snapshotFrom, entitySnapshotsFrom);
        });

    }

    private void applySnapshot(ChunkSnapshot snapshotFrom, Map<EntitySnapshot, Location> entitySnapshotsFrom) {

        Bukkit.getRegionScheduler().run(TemplateWorldRegeneratorPlugin.getInstance(), to(), chunkX(), chunkZ(), t -> {
            Chunk chunkTo = to().getChunkAt(chunkX(), chunkZ());
            killEntities(chunkTo);
            replaceBlocks(snapshotFrom, chunkTo);
            placeEntities(entitySnapshotsFrom);
        });
    }

    /**
     * To run on the chunkTo region scheduler
     */
    private void killEntities(Chunk chunkTo) {
        Arrays.stream(chunkTo.getEntities()).filter(entity -> entity.getType() == EntityType.PLAYER).forEach(Entity::remove);
    }

    /**
     * To run on the chunkTo region scheduler
     */
    private void replaceBlocks(ChunkSnapshot snapshotFrom, Chunk chunkTo) {
        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                for (int y = to().getMinHeight(); y < to().getMaxHeight(); y++) {

                    BlockData data = snapshotFrom.getBlockData(bx, y, bz);

                    chunkTo.getBlock(bx, y, bz).setBlockData(data, false);
                }
            }
        }
    }

    private void placeEntities(Map<EntitySnapshot, Location> entitySnapshotsFrom) {
        for (Map.Entry<EntitySnapshot, Location> entry : entitySnapshotsFrom.entrySet()) {
            Location newLocation = new Location(to(), entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getZ());
            entry.getKey().createEntity(newLocation);
        }
    }
}
