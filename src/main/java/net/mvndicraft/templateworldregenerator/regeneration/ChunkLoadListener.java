package net.mvndicraft.templateworldregenerator.regeneration;

import net.mvndicraft.templateworldregenerator.TemplateWorldRegeneratorPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkLoadListener implements Listener {
    @EventHandler
    public void onChunkLoadEvent(ChunkLoadEvent event) {
        TemplateWorldRegeneratorPlugin.getInstance().getWorldRegenerator().regenerateIfNeeded(event.getChunk());
    }
}
