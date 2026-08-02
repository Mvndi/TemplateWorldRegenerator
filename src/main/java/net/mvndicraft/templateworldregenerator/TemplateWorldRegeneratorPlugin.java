package net.mvndicraft.templateworldregenerator;

import co.aikar.commands.PaperCommandManager;
import java.util.function.Supplier;
import java.util.logging.Level;
import net.mvndicraft.templateworldregenerator.handlers.TownyHandler;
import net.mvndicraft.templateworldregenerator.handlers.TownyRoadsHandler;
import net.mvndicraft.templateworldregenerator.regeneration.ChunkLoadListener;
import net.mvndicraft.templateworldregenerator.regeneration.WorldRegenerator;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

public final class TemplateWorldRegeneratorPlugin extends JavaPlugin {
    private NamespacedKey lastRegenerationDateKey;
    @SuppressWarnings("java:S3077")
    private volatile World from;
    @SuppressWarnings("java:S3077")
    private volatile World to;
    private WorldRegenerator worldRegenerator;
    private boolean townyEnabled;
    private boolean townyRoadsEnabled;

    public TemplateWorldRegeneratorPlugin() { lastRegenerationDateKey = new NamespacedKey(this, "last_regeneration_date"); }

    @Override
    public void onEnable() {
        new Metrics(this, 31503);

        // Save config in our plugin data folder if it does not exist.
        saveDefaultConfig();
        initPluginBoolean();

        worldRegenerator = new WorldRegenerator();

        PaperCommandManager manager = new PaperCommandManager(this);
        manager.registerCommand(new TemplateWorldRegeneratorCommand());


        getServer().getPluginManager().registerEvents(new ChunkLoadListener(), this);
    }

    public static TemplateWorldRegeneratorPlugin getInstance() { return getPlugin(TemplateWorldRegeneratorPlugin.class); }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        initPluginBoolean();
    }

    private void initPluginBoolean() {
        Plugin towny = getServer().getPluginManager().getPlugin("Towny");
        townyEnabled = (towny != null && towny.isEnabled());
        debug("townyEnabled: " + townyEnabled);

        if (townyEnabled) {
            Plugin townyRoads = getServer().getPluginManager().getPlugin("TownyRoads");
            townyRoadsEnabled = (townyRoads != null && townyRoads.isEnabled());
        }
        debug("townyRoadsEnabled: " + townyRoadsEnabled);
    }

    public WorldRegenerator getWorldRegenerator() { return worldRegenerator; }

    public NamespacedKey getLastRegenerationDateKey() { return lastRegenerationDateKey; }


    public World getFromWorld() {
        if (from != null) {
            return from;
        }

        synchronized (this) {
            if (from == null) {
                from = getWorld("from_world");
                info("from loaded from bukkit: " + from + " " + from.getUID());
            }
            return from;
        }
    }

    public World getToWorld() {
        if (to != null) {
            return to;
        }

        synchronized (this) {
            if (to == null) {
                to = getWorld("to_world");
                info("to loaded from bukkit: " + to + " " + to.getUID());
            }
            return to;
        }
    }

    /**
     * Test if the to world have a town or a road at that location.
     * Towny & TownyRoads are optinal dependencies and it will always return false if they are not enabled.
     */
    public boolean isTownOrRoad(Chunk chunk) {
        Location toTestLocation = new Location(getToWorld(), chunk.getX() * 16D, 0D, chunk.getZ() * 16D);
        return (townyEnabled && TownyHandler.isTown(toTestLocation)) || (townyRoadsEnabled && TownyRoadsHandler.isRoad(toTestLocation));
    }

    public @Nullable World getWorld(String configName) {
        String worldName = getConfig().getString(configName);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            NamespacedKey key = NamespacedKey.fromString(configName);
            if (key != null) {
                world = Bukkit.getWorld(key);
            }
        }
        return world;
    }

    // Usual log with debug level
    public static void log(Level level, String message) { getInstance().getLogger().log(level, message); }
    public static void log(Level level, Supplier<String> messageProvider) { getInstance().getLogger().log(level, messageProvider); }
    public static void log(Level level, String message, Throwable e) { getInstance().getLogger().log(level, message, e); }
    public static void debug(String message) {
        if (getInstance().getConfig().getBoolean("debug", false)) {
            log(Level.INFO, message);
        }
    }
    public static void debug(Supplier<String> messageProvider) {
        if (getInstance().getConfig().getBoolean("debug", false)) {
            log(Level.INFO, messageProvider);
        }
    }
    public static void info(String message) { log(Level.INFO, message); }
    public static void info(String message, Throwable e) { log(Level.INFO, message, e); }
    public static void warning(String message) { log(Level.WARNING, message); }
    public static void warning(String message, Throwable e) { log(Level.WARNING, message, e); }
    public static void error(String message) { log(Level.SEVERE, message); }
    public static void error(String message, Throwable e) { log(Level.SEVERE, message, e); }
}
