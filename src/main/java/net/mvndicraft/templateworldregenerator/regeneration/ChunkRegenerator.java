package net.mvndicraft.templateworldregenerator.regeneration;

import net.mvndicraft.templateworldregenerator.TemplateWorldRegeneratorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

public record ChunkRegenerator(int chunkX, int chunkZ, World from, World to) {

    public void run() {
        Bukkit.getRegionScheduler().run(TemplateWorldRegeneratorPlugin.getInstance(), from, chunkX, chunkX, t -> {
            TemplateWorldRegeneratorPlugin.debug("Inside from world");
            Chunk chunkFrom = from.getChunkAt(chunkX(), chunkZ());
            ChunkSnapshot snapshot = chunkFrom.getChunkSnapshot();
            TemplateWorldRegeneratorPlugin.debug("snapshot create");
            applySnapshot(snapshot, to, chunkX, chunkZ);
        });

    }

    private void applySnapshot(ChunkSnapshot snapshot, World to, int chunkX, int zchunkX) {

        Bukkit.getRegionScheduler().run(TemplateWorldRegeneratorPlugin.getInstance(), to, chunkX, zchunkX, t -> {
            Chunk chunkTo = to.getChunkAt(chunkX(), chunkZ());

            for (int bx = 0; bx < 16; bx++) {
                for (int bz = 0; bz < 16; bz++) {
                    for (int y = to.getMinHeight(); y < to.getMaxHeight(); y++) {

                        BlockData data = snapshot.getBlockData(bx, y, bz);

                        chunkTo.getBlock(bx, y, bz).setBlockData(data, false);
                    }
                }
            }
        });
    }
}
