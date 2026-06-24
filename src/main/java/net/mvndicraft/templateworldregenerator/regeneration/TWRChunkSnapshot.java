package net.mvndicraft.templateworldregenerator.regeneration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.mvndicraft.templateworldregenerator.TemplateWorldRegeneratorPlugin;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;

public record TWRChunkSnapshot(ChunkSnapshot chunkSnapshot, Map<EntitySnapshot, Location> entitySnapshots, byte[] pdcData) {

    public TWRChunkSnapshot(Chunk chunkFrom) {
        this(chunkFrom.getChunkSnapshot(),
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
}
