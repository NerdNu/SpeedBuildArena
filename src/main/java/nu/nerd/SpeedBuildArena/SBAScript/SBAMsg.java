package nu.nerd.SpeedBuildArena.SBAScript;

import org.bukkit.ChatColor;

import nu.nerd.SpeedBuildArena.SBA;

/**
 * 
 * Run the /msg command
 *
 */
public class SBAMsg implements SBACommand {

    private String _msg;

    public SBAMsg(String msg) {
        _msg = ChatColor.translateAlternateColorCodes('&', msg);
    }

    /**
     * Broadcast a message to everyone
     * 
     * @param context
     *            SpeedBuild object
     */
    @Override
    public void execute(SBA context) {
        context.getServer().broadcastMessage(_msg);
    }
}
