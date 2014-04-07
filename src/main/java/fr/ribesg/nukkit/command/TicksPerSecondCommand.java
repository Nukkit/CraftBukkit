package fr.ribesg.nukkit.command;

import net.minecraft.server.MinecraftServer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;

public class TicksPerSecondCommand extends Command {

    private DecimalFormat format;

    public TicksPerSecondCommand() {
        super("tps");
        this.description = "Gets the current ticks per second for the server";
        this.usageMessage = "/tps";
        this.setPermission("nukkit.command.tps");
        this.format = new DecimalFormat("0.0#");
    }

    @Override
    public boolean execute(final CommandSender sender, final String currentAlias, final String[] args) {
        if (!testPermission(sender)) {
            return true;
        }

        final double[] recentTps = MinecraftServer.getServer().recentTps;
        sender.sendMessage(ChatColor.GOLD + "TPS from last 1m / 5m / 15m: " +
                format(recentTps[0]) + ChatColor.GOLD + " / " +
                format(recentTps[1]) + ChatColor.GOLD + " / " +
                format(recentTps[2]));
        return true;
    }

    private String format(double tps) {
        return ((tps > 18.0) ? ChatColor.GREEN : (tps > 16.0) ? ChatColor.YELLOW : ChatColor.RED) +
                this.format.format(Math.min(tps, 20.0)) + ((tps > 20.0) ? "*" : "");
    }
}
