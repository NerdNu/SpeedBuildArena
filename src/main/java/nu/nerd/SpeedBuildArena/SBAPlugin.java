package nu.nerd.SpeedBuildArena;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

// import com.sk89q.worldedit.MaxChangedBlocksException;
// import com.sk89q.worldedit.WorldEdit;
// import com.sk89q.worldedit.WorldEditException;
// import com.sk89q.worldedit.extension.factory.BlockFactory;
// import com.sk89q.worldedit.extension.input.ParserContext;
// import com.sk89q.worldedit.world.block.BlockStateHolder;

import net.md_5.bungee.api.ChatColor;

/**
 * 
 * Speed Build Arena management plugin.
 * 
 * Automatically start, time and run a speed build event.
 * 
 * Features: Start timed event. Show progress bar for all players. Automatic
 * permission management.
 *
 */
public class SBAPlugin extends JavaPlugin {

    private SBAConfig _config;
    private SBA _speedBuild = null;

    /**
     * Called when the plugin is first enabled
     */
    @Override
    public void onEnable() {
        getLogger().info("Loading SpeedBuildArena");

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
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

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
        case "setfloor":
            try {
                cmdSetFloor(sender, args);
            } catch (Exception ex) {
                printStackTrace(ex);
                sender.sendMessage(ChatColor.RED + "An unexpected error occurd while running this command.");
            }
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
        if (!sender.hasPermission("speedbuildarena.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command");
            return;
        }

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
     * 
     * @param sender Command Sender
     * @param args Command arguments
     */
    private void cmdStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("speedbuildarena.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command");
            return;
        }

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
     * 
     * @param sender Command Sender
     * @param args Command arguments
     */
    private void cmdAbort(CommandSender sender, String[] args) {
        if (!sender.hasPermission("speedbuildarena.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command");
            return;
        }

        if (_speedBuild == null) {
            sender.sendMessage(ChatColor.RED + "Speed Build is not running.");
        } else {
            _speedBuild.close();
            _speedBuild = null;
            sender.sendMessage(ChatColor.GREEN + "Speed Build aborted.");
        }
    }

    /**
     * Set the floor area of a players arena
     * 
     * @param sender The player
     * @param args The block type to set the floor too
     */
    private void cmdSetFloor(CommandSender sender, String[] args) {
        int num = (int)(Math.random()*4.0);
        String msg;
        switch (num) {
            case 0: msg = "This command is down for repairs."; break;
            case 1: msg = "Try again later."; break;
            case 2: msg = "I can't do that Dave."; break;
            default: msg = "This command is down. Please try again."; break;
        }
        sender.sendMessage(ChatColor.RED + msg);
    }

    /**
     * Print full usage to a sender.
     * 
     * @param sender
     */
    public void printUsage(CommandSender sender) {
        printStartUsage(sender);
        printAbortUsage(sender);
        printReloadUsage(sender);
        printSetFloorUsage(sender);
    }

    /**
     * Print usage for the start command.
     * 
     * @param sender
     */
    public void printStartUsage(CommandSender sender) {
        if (sender.hasPermission("speedbuildarena.admin")) {
            sender.sendMessage(ChatColor.GREEN + "/SpeedBuildArena start");
        }
    }

    /**
     * Print usage for the abort command.
     * 
     * @param sender
     */
    public void printAbortUsage(CommandSender sender) {
        if (sender.hasPermission("speedbuildarena.admin")) {
            sender.sendMessage(ChatColor.GREEN + "/SpeedBuildArena abort");
        }
    }

    /**
     * Print usage for the reload command.
     * 
     * @param sender
     */
    public void printReloadUsage(CommandSender sender) {
        if (sender.hasPermission("speedbuildarena.admin")) {
            sender.sendMessage(ChatColor.GREEN + "/SpeedBuildArena reload");
        }
    }

    /**
     * Print usage for the setfloor command
     * 
     * @param sender
     */
    public void printSetFloorUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "/SpeedBuildArena setfloor BLOCK");
    }

    /**
     * Return configuration object
     * 
     * @return Configuration
     */
    public SBAConfig getSBAConfig() {
        return _config;
    }

    /**
     * Handle speed build finished event (Close enough to an event anyway) This
     * should only be called from a running SpeedBuild object.
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
