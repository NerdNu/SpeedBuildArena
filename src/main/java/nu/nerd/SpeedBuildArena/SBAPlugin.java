package nu.nerd.SpeedBuildArena;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import net.md_5.bungee.api.ChatColor;


/**
 * 
 * Speed Build Arena management plugin.
 * 
 * Automatically start, time and run a speed build event.
 * 
 * Features:
 *   Start timed event.
 *   Show progress bar for all players.
 *   Automatic permission management.
 *
 */
public class SBAPlugin extends JavaPlugin {
	
	private WorldGuardPlugin _wg = null;
	private SBAConfig _config;
	private SBA _speedBuild = null;

	
	/**
	 * Called when the plugin is first enabled
	 */
	@Override
	public void onEnable() {
	    getLogger().info("Loading SpeedBuildArena");
		
		// Load plugins
		Plugin p = getServer().getPluginManager().getPlugin("WorldGuard");
		if (p == null || ! (p instanceof WorldGuardPlugin)) {
			getLogger().severe("Failed to load WorldGuard plugin");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		_wg = (WorldGuardPlugin)p;

		saveDefaultConfig();
		
		_config = new SBAConfig();
		try {
		    _config.load(this);
		} catch (Exception ex) {
		    printStackTrace(ex);
		}
		
	}
	
	
	/**
	 * Called when the plugin is disabled.
	 */
	@Override
	public void onDisable() {
	    getLogger().info("Unloading SpeedBuildArena");
	    if (_speedBuild != null) {
	        _speedBuild.close();
	        _speedBuild = null;
	    }
	    _config = null;
		_wg = null;
	}
	
	
	/**
	 * Process user commands.
	 * 
	 * @param sender Command origin
	 * @param command The command to be performed. Includes all yml meta data.
	 * @param label The exact name of the command the user typed in
	 * @param args Command arguments
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		// Check Command
		if (!command.getName().equalsIgnoreCase("speedbuildarena")) {
			return false;
		}
		
		if (args.length == 0) {
			printUsage(sender);
			return true;
		}
		
		// Execute sub-command
		String subcmd = args[0].toLowerCase();
		args = Arrays.copyOfRange(args, 1, args.length);
		switch (subcmd) {
		case "start":
		case "begin":
			cmdStart(sender, args);
			break;
		case "abort":
		case "cancel":
		case "stop":
			cmdAbort(sender, args);
			break;
		case "reload":
		    reloadSBAConfig(sender);
		    break;
		default:
			sender.sendMessage(ChatColor.RED + "Invalid command \"" + subcmd + "\".");
			printUsage(sender);
			return false;
		}

		return true;
	}
	
	
	/**
	 * Relaod configuration file
	 */
	public void reloadSBAConfig(CommandSender sender) {
	    try {
	        _config.load(this);
	    } catch (Exception ex) {
	        printStackTrace(ex);
	        if (sender != null) {
	            sender.sendMessage(ChatColor.RED + "ERROR:" + ex.getMessage());
	        }
	    }
	    sender.sendMessage(ChatColor.GREEN + "Configuration reloaded");
	}
	
	
	/**
	 * Start a speed build event.
	 * @param sender Command Sender
	 * @param args Command arguments
	 */
	private void cmdStart(CommandSender sender, String[] args) {
	    if (_speedBuild != null) {
	        sender.sendMessage(ChatColor.RED + "ERROR: Speed build is already running.");
	        sender.sendMessage(ChatColor.RED + "Did you mean to use /SpeedBuildArena abort");
	        return;
	    }

	    try {
	        _speedBuild = new SBA(this);
	    } catch (Exception ex) {
	        printStackTrace(ex);
	        sender.sendMessage(ChatColor.RED + "ERROR: " + ex.getMessage());
	    }
	}
	
	
	/**
	 * Abort a speed build event.
	 * @param sender Command Sender
	 * @param args Command arguments
	 */
	private void cmdAbort(CommandSender sender, String[] args) {
		if (_speedBuild == null) {
		    sender.sendMessage(ChatColor.RED + "Speed Build is not running.");
		} else {
		    _speedBuild.close();
		    _speedBuild = null;
		    sender.sendMessage(ChatColor.GREEN + "Speed Build aborted.");
		}
	}
	
	
	/**
	 * Print full usage to a sender.
	 * @param sender
	 */
	public void printUsage(CommandSender sender) {
		printStartUsage(sender);
		printAbortUsage(sender);
		printReloadUsage(sender);
	}
	
	
	/**
	 * Print usage for the start command.
	 * @param sender
	 */
	public void printStartUsage(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "/SpeedBuildArena start");
	}
	
	
	/**
	 * Print usage for the abort command.
	 * @param sender
	 */
	public void printAbortUsage(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "/SpeedBuildArena abort");
	}
	
	
	   /**
     * Print usage for the reload command.
     * @param sender
     */
    public void printReloadUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "/SpeedBuildArena reload");
    }
	
	
	/**
	 * Return the world guard plugin.
	 * @return World Guard
	 */
	public WorldGuardPlugin getWorldGuard() {
		return _wg;
	}
	
	
	/**
	 * Return configuration object
	 * @return Configuration
	 */
	public SBAConfig getSBAConfig() {
	    return _config;
	}
	
	
	/**
	 * Handle speed build finished event (Close enough to an event anyway)
	 * This should only be called from a running SpeedBuild object.
	 */
	public void onSpeedBuildFinished() {
	    _speedBuild = null;
	}
	
	
	/**
	 * Print a stack trace
	 * 
	 * @param e Exception
	 */
	public void printStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        getLogger().severe(sw.toString());
	}

}
