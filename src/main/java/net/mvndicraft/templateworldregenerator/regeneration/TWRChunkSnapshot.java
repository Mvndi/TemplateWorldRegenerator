package net.mvndicraft.templateworldregenerator.regeneration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.mvndicraft.templateworldregenerator.TemplateWorldRegeneratorPlugin;
import net.mvndicraft.templateworldregenerator.util.NBTCompondTagUtil;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;

public record TWRChunkSnapshot(ChunkSnapshot chunkSnapshot, List<BlockEntitySnapshot> blockEntities,
        Map<EntitySnapshot, Location> entitySnapshots, byte[] pdcData) {

    public TWRChunkSnapshot(Chunk chunkFrom) {
        this(chunkFrom.getChunkSnapshot(), toBlockEntitySnapshots(chunkFrom),
                Arrays.stream(chunkFrom.getEntities()).collect(Collectors.toMap(Entity::createSnapshot, Entity::getLocation)),
                serializePersistentDataContainer(chunkFrom));
    }

    private static byte[] serializePersistentDataContainer(Chunk chunk) {
        try {
            return chunk.getPersistentDataContainer().serializeToBytes();
        } catch (Exception e) {
            TemplateWorldRegeneratorPlugin.warning("Failed to save PDC data", e);
            return new byte[0];
        }
    }

    private static List<BlockEntitySnapshot> toBlockEntitySnapshots(Chunk chunk) {
        // return Arrays.stream(chunk.getTileEntities(true)).toList();
        return Arrays.stream(chunk.getTileEntities()).map(state -> {
            byte[] nbt = NBTCompondTagUtil.getBlockEntityNbt(state.getBlock());

            return new BlockEntitySnapshot(state.getX(), state.getY(), state.getZ(), nbt);
        }).toList();
    }


}
