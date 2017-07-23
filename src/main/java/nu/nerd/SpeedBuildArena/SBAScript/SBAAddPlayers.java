package nu.nerd.SpeedBuildArena.SBAScript;

import java.util.List;

import org.bukkit.entity.Player;

import com.sk89q.worldedit.Vector;
//import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import nu.nerd.SpeedBuildArena.SBA;
import nu.nerd.SpeedBuildArena.SBAConfig;
import nu.nerd.SpeedBuildArena.SBAPlot;

public class SBAAddPlayers implements SBACommand {

    public SBAAddPlayers(String args) {
        // Ignore the args
    }

    /**
     * Find all players in plots. Grand any player standing in a plot permission
     * in the plot
     * 
     * @param context
     *            SpeedBuild object
     */
    @Override
    public void execute(SBA context) {
        SBAConfig config = context.getPlugin().getSBAConfig();
        // WorldGuardPlugin wgp = context.getPlugin().getWorldGuard();
        ProtectedRegion arena = context.getArenaPlot();
        List<SBAPlot> plots = context.getPlots();
        String worldName = context.getPlugin().getSBAConfig().WORLD_NAME;

        // Is there a better way than to test every player with every plot?
        for (Player player : context.getServer().getOnlinePlayers()) {
            int x, y, z;
            x = (int) player.getLocation().getX();
            y = (int) player.getLocation().getY();
            z = (int) player.getLocation().getZ();
            Vector pos = new Vector(x, y, z);

            // Don't consider players outside the arena
            // If your not in the arena, you can't be in a plot, right?
            if (arena.contains(pos)) {
                for (SBAPlot plot : plots) {
                    ProtectedRegion rplot = plot.getPlot();
                    if (rplot.contains(pos)) {
                        if (config.ADD_OWNERS) {
                            context.dispatchCommand(
                                    String.format("region addowner %s %s -w %s",
                                            rplot.getId(), player.getName(),
                                            worldName));
                            // rplot.getOwners().addPlayer(wgp.wrapPlayer(player));
                            // // this does not update WG player cache
                        } else {
                            context.dispatchCommand(String.format(
                                    "region addmember %s %s -w %s",
                                    rplot.getId(), player.getName(),
                                    worldName));
                            // rplot.getMembers().addPlayer(wgp.wrapPlayer(player));
                            // // this does not update WG player cache
                        }
                    }
                }
            }
        }
    }
}
