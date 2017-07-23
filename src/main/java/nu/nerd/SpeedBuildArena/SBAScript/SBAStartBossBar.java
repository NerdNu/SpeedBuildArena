package nu.nerd.SpeedBuildArena.SBAScript;

import nu.nerd.SpeedBuildArena.SBA;

/**
 * 
 * Run the /bossbar <duration> command
 *
 */
public class SBAStartBossBar implements SBACommand {

    private long _duration;

    public SBAStartBossBar(String args) {
        _duration = Long.parseLong(args);
    }

    /**
     * Broadcast a message to everyone
     * 
     * @param context
     *            SpeedBuild object
     */
    @Override
    public void execute(SBA context) {
        context.startBossTimer(_duration);
    }
}
