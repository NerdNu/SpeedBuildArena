package nu.nerd.SpeedBuildArena.SBAScript;

import nu.nerd.SpeedBuildArena.SBA;

public class SBASleep implements SBACommand {
    private long _delay;

    public SBASleep(String args) {
        _delay = Long.parseLong(args);
    }

    /**
     * Broadcast a message to everyone
     * 
     * @param context
     *            SpeedBuild object
     */
    @Override
    public void execute(SBA context) {
        context.addTime(_delay);
    }
}
