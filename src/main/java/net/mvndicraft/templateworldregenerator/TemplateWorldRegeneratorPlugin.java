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

public final class TemplateWorldRegeneratorPlugin extends JavaPlugin {
    private NamespacedKey lastRegenerationDateKey;
    private World from;
    private World to;
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

        // loadFromWorld();

        getServer().getPluginManager().registerEvents(new ChunkLoadListener(), this);
    }

    public void loadFromWorld() {
        // info("getWorldRegistry:" + WorldsAccess.access().getWorldRegistry().entrySet().map(t -> t.toString()).toList());
        // info("isRegistered:" + WorldsAccess.access().getWorldRegistry().isRegistered(Key.key("worlds:world_template")));
        // info("get:" + WorldsAccess.access().getWorldRegistry().get(Key.key("worlds:world_template")).get());
        // info("get.class:" + WorldsAccess.access().getWorldRegistry().get(Key.key("worlds:world_template")).get().getClass());
        // info("getdimension:" + WorldsAccess.access().getWorldRegistry().get(Key.key("worlds:world_template")).get().dimension());


        // try {
        // info("world get:" + WorldsAccess.access().load(Key.key("worlds:world_template")).get());
        // } catch (Exception e) {
        // warning("world get failed for " + e + " " + e.getMessage());
        // }

        // // WorldsAccess.access().load(Key.key("minecraft:overworld")).whenComplete((world, t) -> {
        // WorldsAccess.access().load(Key.key("worlds:world_template")).whenComplete((world, t) -> {
        // info("----------------------------------------------------------------------------------------");
        // info("t=" + t.getMessage());
        // info("tgetMessage=" + t.getMessage());
        // from = world;
        // info("world loaded: " + world);
        // info("from loaded: " + from);
        // });
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
        if (from == null) {
            from = Bukkit.getWorld(NamespacedKey.fromString("worlds:world_template"));
            info("from loaded from bukkit: " + from);
        }
        return from;
    }

    public World getToWorld() {
        if (to == null) {
            to = Bukkit.getWorld("world");
            info("to loaded: " + to);
        }
        return to;
    }

    /**
     * Test if the to world have a town or a road at that location.
     * Towny & TownyRoads are optinal dependencies and it will always return false if they are not enabled.
     */
    public boolean isTownOrRoad(Chunk chunk) {
        Location toTestLocation = new Location(getToWorld(), chunk.getX() * 16D, 0D, chunk.getZ() * 16D);
        return (townyEnabled && TownyHandler.isTown(toTestLocation)) || (townyRoadsEnabled && TownyRoadsHandler.isRoad(toTestLocation));
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
