package net.mvndicraft.templateworldregenerator.regeneration;

import java.util.Arrays;
import java.util.Map;
import net.mvndicraft.templateworldregenerator.TemplateWorldRegeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;

public record ChunkRegenerator(int chunkX, int chunkZ, World from, World to) {

    public void run() {
        Bukkit.getRegionScheduler().run(TemplateWorldRegeneratorPlugin.getInstance(), from(), chunkX(), chunkZ(), t -> {
            TemplateWorldRegeneratorPlugin.debug("Inside from world");
            Chunk chunkFrom = from().getChunkAt(chunkX(), chunkZ());
            TWRChunkSnapshot twrChunkSnapshot = new TWRChunkSnapshot(chunkFrom);
            applySnapshot(twrChunkSnapshot);
        });

    }

    private void applySnapshot(TWRChunkSnapshot twrChunkSnapshot) {

        Bukkit.getRegionScheduler().run(TemplateWorldRegeneratorPlugin.getInstance(), to(), chunkX(), chunkZ(), t -> {
            Chunk chunkTo = to().getChunkAt(chunkX(), chunkZ());
            killEntities(chunkTo);
            replaceBlocks(twrChunkSnapshot, chunkTo);
            placeEntities(twrChunkSnapshot);
            replacePdcData(twrChunkSnapshot, chunkTo);
            saveLastRegenerationDate(chunkTo);
            TemplateWorldRegeneratorPlugin.debug(() -> "Regenerated chunk " + chunkX() + " " + chunkZ());
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
    private void replaceBlocks(TWRChunkSnapshot twrChunkSnapshot, Chunk chunkTo) {
        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                for (int y = to().getMinHeight(); y < to().getMaxHeight(); y++) {

                    BlockData data = twrChunkSnapshot.chunkSnapshot().getBlockData(bx, y, bz);

                    chunkTo.getBlock(bx, y, bz).setBlockData(data, false);
                }
            }
        }
    }

    private void placeEntities(TWRChunkSnapshot twrChunkSnapshot) {
        for (Map.Entry<EntitySnapshot, Location> entry : twrChunkSnapshot.entitySnapshots().entrySet()) {
            Location newLocation = new Location(to(), entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getZ());
            entry.getKey().createEntity(newLocation);
        }
    }

    public void replacePdcData(TWRChunkSnapshot twrChunkSnapshot, Chunk chunkTo) {
        byte[] pdcData = twrChunkSnapshot.pdcData();
        if (pdcData.length != 0) {
            try {
                chunkTo.getPersistentDataContainer().readFromBytes(twrChunkSnapshot.pdcData(), true);
            } catch (Exception e) {
                TemplateWorldRegeneratorPlugin.warning("Failed to replace PDC data", e);
            }
        }
    }

    public void saveLastRegenerationDate(Chunk chunkTo) {
        chunkTo.getPersistentDataContainer().set(TemplateWorldRegeneratorPlugin.getInstance().getLastRegenerationDateKey(),
                PersistentDataType.LONG, System.currentTimeMillis());
    }
}
