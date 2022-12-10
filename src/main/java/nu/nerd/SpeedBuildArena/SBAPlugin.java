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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.NotABlockException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;

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
    private void cmdSetFloor(CommandSender sender, String[] args) throws WorldEditException, MaxChangedBlocksException{
        if (!(sender instanceof Player)) {
            sender.sendMessage("Silly Console. Trix are for kids");
            return;
        }
        
        Player player = (Player)sender;

        // Ensure that an event is in progress
        if (_speedBuild == null) {
            sender.sendMessage(ChatColor.RED + "A Speed Build event is not in progress. Sorry :(");
            return;
        }
        
        if (args.length != 0) {
            printSetFloorUsage(sender);
            return;
        }

        World world = getServer().getWorld(_config.WORLD_NAME);
        if (world == null) {
            throw new IllegalStateException("Unknown world \"%s\"".format(_config.WORLD_NAME));
        }

        //// Find what plot the player is in.
        int x, y, z;
        x = (int)player.getLocation().getX();
        y = (int)player.getLocation().getY();
        z = (int)player.getLocation().getZ();
        SBAPlot plot = null;
        for(SBAPlot p : _speedBuild.getPlots()) {
            if(p.getPlot().contains(x, y, z)) {
                // Make sure the player is registered with this plot
                if (p.getPlot().getOwners().contains(player.getUniqueId())
                   || p.getPlot().getMembers().contains(player.getUniqueId())) {
                    plot = p;
                    break;
                } else {
                    sender.sendMessage(ChatColor.RED + "Get off my lawn, you whippersnapper! Find your own plot!");
                    return;
                } 
            }
        }
        if (plot == null) {
            sender.sendMessage(ChatColor.RED + "You must be standing in your plot");
            return;
        }

        //// Create WE region
        CuboidRegion region = new CuboidRegion(
            plot.getFloor().getMaximumPoint(),
            plot.getFloor().getMinimumPoint());

        //// Lookup the block.
        BaseBlock block = null;
        try {
            block = BukkitAdapter.adapt(player).getBlockInHand(HandSide.MAIN_HAND);
        } catch (NotABlockException e) {
            sendInvalidBlockMsg(sender);
            return;
        }

        //// Block blocks that are not solid, such as air and levers.
        if (block == null || !block.getBlockType().getMaterial().isSolid()) {
            // Don't allow org.bukkit.Materiall.AIR
            sendInvalidBlockMsg(sender);
            return;
        }

        //// Set the floor.
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            editSession.setBlocks(region, block);
        }

    }

    /**
    * Send the invalid block message to the player
    * 
    */
    public void sendInvalidBlockMsg(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "You must hold a " + ChatColor.BOLD + "block" + ChatColor.RESET + ChatColor.RED + " in your hand.");
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
        sender.sendMessage(ChatColor.GREEN + "/SpeedBuildArena setfloor");
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
