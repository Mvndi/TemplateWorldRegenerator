package net.mvndicraft.templateworldregenerator.handlers;

import net.mvndicraft.townyroads.Road;
import net.mvndicraft.townyroads.TownyRoadsPlugin;
import org.bukkit.Location;

public class TownyRoadsHandler {
    private TownyRoadsHandler() {}
    public static boolean isValidRoad(Location location) {
        Road road = TownyRoadsPlugin.getInstance().getRoadManager().getRoadAt(location);
        return road != null && road.isValid();
    }
    public static boolean isRoad(Location location) {
        Road road = TownyRoadsPlugin.getInstance().getRoadManager().getRoadAt(location);
        return road != null;
    }
}
