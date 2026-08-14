package net.mvndicraft.templateworldregenerator.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.mvndicraft.templateworldregenerator.regeneration.BlockEntitySnapshot;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;

public class NBTCompondTagUtil {
    private NBTCompondTagUtil() {}
    public static byte[] serialize(CompoundTag tag) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); DataOutputStream dataOutput = new DataOutputStream(output)) {

            NbtIo.write(tag, dataOutput);

            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    public static CompoundTag deserialize(byte[] data) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(data); DataInputStream dataInput = new DataInputStream(input)) {

            return NbtIo.read(dataInput, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public static byte[] getBlockEntityNbt(Block block) {
        BlockState state = block.getState();

        if ((state instanceof CraftBlockEntityState<? extends BlockEntity> craftState)) {
            CompoundTag tag = craftState.getSnapshotNBT();
            return serialize(tag);
        }
        return new byte[] {};

    }

    public static void restoreBlockEntity(BlockEntitySnapshot snapshot, Chunk chunkTo) {
        Block block = chunkTo.getBlock(snapshot.x() % 16, snapshot.y(), snapshot.z() % 16);

        BlockState state = block.getState();

        if (!(state instanceof CraftBlockEntityState<?> craftState)) {
            return;
        }

        CompoundTag tag = deserialize(snapshot.nbt());

        craftState.loadData(tag);

        craftState.update(true, false);
    }
}
