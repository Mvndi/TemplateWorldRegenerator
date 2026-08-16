package net.mvndicraft.templateworldregenerator;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.text.Component;
import net.mvndicraft.templateworldregenerator.regeneration.ChunkRegenerator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("templateworldregenerator|twr")
@CommandPermission("templateworldregenerator.admin")
public class TemplateWorldRegeneratorCommand extends BaseCommand {
    @Default
    @Description("Lists the version of the plugin")
    public static void onTwr(CommandSender commandSender) {
        commandSender.sendMessage(Component.text(TemplateWorldRegeneratorPlugin.getInstance().toString()));
    }

    @Subcommand("reload")
    @Description("Reloads the plugin config and data file")
    public static void onReload(CommandSender commandSender) {
        TemplateWorldRegeneratorPlugin.getInstance().reloadConfig();
        commandSender.sendMessage(Component.text("TemplateWorldRegenerator reloaded"));
    }

    @Subcommand("regenerateChunk")
    @Description("Regenerate the chunk the player is standing on")
    public static void onRegenerate(CommandSender commandSender) {
        if (commandSender instanceof Player player) {
            int chunkX = Math.floorDiv(player.getLocation().getBlockX(), 16);
            int chunkZ = Math.floorDiv(player.getLocation().getBlockZ(), 16);
            TemplateWorldRegeneratorPlugin.debug("regenerateChunk runned by a player in " + chunkX + " " + chunkZ);

            new ChunkRegenerator(chunkX, chunkZ, TemplateWorldRegeneratorPlugin.getInstance().getFromWorld(),
                    TemplateWorldRegeneratorPlugin.getInstance().getToWorld()).run();
        }
    }

    @Subcommand("regenerateWorld")
    @Description("Regenerate the whole world except town & roads")
    public static void onRegenerateWorld(CommandSender commandSender, int x1, int z1, int x2, int z2) {
        TemplateWorldRegeneratorPlugin.debug("regenerateWorld runned by " + commandSender.getName());
        commandSender.sendMessage(Component.text("World regeneration started"));
        TemplateWorldRegeneratorPlugin.getInstance().getWorldRegenerator().addAreaToChunksToRegenerate(x1, z1, x2, z2);
    }

    @Subcommand("stopRegeneration")
    @Description("Stops regeneration of the world")
    public static void onStopRegeneration(CommandSender commandSender) {
        TemplateWorldRegeneratorPlugin.debug("stopRegeneration runned by " + commandSender.getName());
        commandSender.sendMessage(Component.text("Regeneration stopped"));
        TemplateWorldRegeneratorPlugin.getInstance().getWorldRegenerator().stopRegeneration();
    }

    @Subcommand("regenerationProgress")
    @Description("Returns the number of chunks to regenerate and the total number of chunks to regenerate in the world")
    public static void onRegenerationProgress(CommandSender commandSender) {
        commandSender.sendMessage(Component.text("Chunks to regenerate: "
                + TemplateWorldRegeneratorPlugin.getInstance().getWorldRegenerator().getChunksToRegenerateCount()));
    }

}
