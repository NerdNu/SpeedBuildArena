package nu.nerd.SpeedBuildArena.SBAScript;

import java.util.List;

import nu.nerd.SpeedBuildArena.SBA;
import nu.nerd.SpeedBuildArena.SBAPlot;

public class SBARemovePlayers implements SBACommand {

    public SBARemovePlayers(String args) {
        // Ignore the args
    }
    
    /**
     * Remove all player permissions from the plots
     * 
     * @param context SpeedBuild object
     */
    @Override
    public void execute(SBA context) {
        List<SBAPlot> plots = context.getPlots();
        
        for(SBAPlot plot : plots) {
            plot.getPlot().getOwners().clear();
            plot.getPlot().getMembers().clear();
        }
    }
}
