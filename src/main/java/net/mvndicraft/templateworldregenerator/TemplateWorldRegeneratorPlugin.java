package net.mvndicraft.templateworldregenerator;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class TemplateWorldRegeneratorPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        new Metrics(this, 31503);
    }
}
