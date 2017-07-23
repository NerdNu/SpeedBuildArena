package nu.nerd.SpeedBuildArena.SBAScript;

import nu.nerd.SpeedBuildArena.SBA;

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
        context.wipePlots();
    }
}
