package net.mvndicraft.templateworldregenerator;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.adventure.text.Component;
import net.mvndicraft.templateworldregenerator.regeneration.ChunkRegenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
            int chunkX = player.getLocation().getBlockX() / 16;
            int chunkZ = player.getLocation().getBlockZ() / 16;
            TemplateWorldRegeneratorPlugin.debug("regenerateChunk runned by a player in " + chunkX + " " + chunkZ);
            World from = Bukkit.getWorld("world_template");
            World to = Bukkit.getWorld("world");
            new ChunkRegenerator(chunkX, chunkZ, from, to).run();
        }
    }

}
