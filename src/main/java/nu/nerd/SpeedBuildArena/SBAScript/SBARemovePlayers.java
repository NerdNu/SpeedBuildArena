package nu.nerd.SpeedBuildArena.SBAScript;

import java.util.List;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import nu.nerd.SpeedBuildArena.SBA;

public class SBARemovePlayers implements SBACommand {

    public SBARemovePlayers(String args) {
        // Ignore the args
    }
    
    /**
     * Remove all player permisions from the plots
     * 
     * @param context SpeedBuild object
     */
    @Override
    public void execute(SBA context) {
        List<ProtectedRegion> plots = context.getPlots();
        
        for(ProtectedRegion plot : plots) {
            plot.getOwners().clear();
            plot.getMembers().clear();
        }
    }
}
