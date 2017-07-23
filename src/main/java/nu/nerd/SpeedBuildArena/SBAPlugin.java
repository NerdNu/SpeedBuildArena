package nu.nerd.SpeedBuildArena;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.primesoft.blockshub.IBlocksHubApi;
import org.primesoft.blockshub.api.BlockData;
import org.primesoft.blockshub.api.Vector;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.factory.BlockFactory;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

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

    private WorldGuardPlugin _wg = null;
    private IBlocksHubApi _blocksHub = null;
    private WorldEdit _we = null;
    private WorldEditPlugin _wep = null;
    private SBAConfig _config;
    private SBA _speedBuild = null;

    /**
     * Called when the plugin is first enabled
     */
    @Override
    public void onEnable() {
        getLogger().info("Loading SpeedBuildArena");

        // Load plugins
        Plugin p;

        p = getServer().getPluginManager().getPlugin("WorldGuard");
        if (p == null || !(p instanceof WorldGuardPlugin)) {
            getLogger().severe("Failed to load WorldGuard plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        _wg = (WorldGuardPlugin) p;
        getLogger().info("Loaded WorldGuard plugin");

        // Load blocks hub
        p = getServer().getPluginManager().getPlugin("BlocksHub");
        if (p != null
                && (p instanceof org.primesoft.blockshub.BlocksHubBukkit)) {
            org.primesoft.blockshub.BlocksHubBukkit bh = (org.primesoft.blockshub.BlocksHubBukkit) p;
            _blocksHub = (IBlocksHubApi) bh.getApi();
            getLogger().info("Loaded BlocksHub plugin");
        }

        p = getServer().getPluginManager().getPlugin("WorldEdit");
        if (p != null && (p instanceof WorldEditPlugin)) {
            _wep = (WorldEditPlugin) p;
            _we = _wep.getWorldEdit();
            getLogger().info("Loaded WorldEdit plugin");
        }

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
        _blocksHub = null;
        _we = null;
        _wep = null;
    }

    /**
     * Process user commands.
     * 
     * @param sender
     *            Command origin
     * @param command
     *            The command to be performed. Includes all yml meta data.
     * @param label
     *            The exact name of the command the user typed in
     * @param args
     *            Command arguments
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
                sender.sendMessage(ChatColor.RED
                        + "An unexpected error occurd while running this command.");
            }
            break;
        case "reload":
            reloadSBAConfig(sender);
            break;
        default:
            sender.sendMessage(
                    ChatColor.RED + "Invalid command \"" + subcmd + "\".");
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
            sender.sendMessage(ChatColor.RED
                    + "You do not have permission to run this command");
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
     * @param sender
     *            Command Sender
     * @param args
     *            Command arguments
     */
    private void cmdStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("speedbuildarena.admin")) {
            sender.sendMessage(ChatColor.RED
                    + "You do not have permission to run this command");
            return;
        }

        if (_speedBuild != null) {
            sender.sendMessage(
                    ChatColor.RED + "ERROR: Speed build is already running.");
            sender.sendMessage(ChatColor.RED
                    + "Did you mean to use /SpeedBuildArena abort");
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
     * @param sender
     *            Command Sender
     * @param args
     *            Command arguments
     */
    private void cmdAbort(CommandSender sender, String[] args) {
        if (!sender.hasPermission("speedbuildarena.admin")) {
            sender.sendMessage(ChatColor.RED
                    + "You do not have permission to run this command");
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
     * @param sender
     *            The player
     * @param args
     *            The block type to set the floor too
     * @throws WorldEditException
     * @throws MaxChangedBlocksException
     */
    @SuppressWarnings("deprecation")
    private void cmdSetFloor(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Silly Console. Trix are for kids");
            return;
        }

        Player player = (Player) sender;

        // Ensure that an event is in progress
        if (_speedBuild == null) {
            sender.sendMessage(ChatColor.RED
                    + "A Speed Build event is not in progress. Sorry :(");
            return;
        }

        if (args.length != 1) {
            printSetFloorUsage(sender);
            return;
        }
        String blockName = args[0];
        int blockId = 0;
        int blockData = 0;

        if (_wep != null && _we != null) {
            // If WE is loaded, use WE to lookup the block name and use it's
            // black list
            com.sk89q.worldedit.entity.Player wePlayer = _wep
                    .wrapPlayer(player);
            com.sk89q.worldedit.world.World weWorld = wePlayer.getWorld();
            BlockFactory bf = _we.getBlockFactory();
            ParserContext context = new ParserContext();
            context.setActor(wePlayer);
            context.setWorld(weWorld);
            context.setSession(_we.getSessionManager().get(wePlayer));
            context.setRestricted(true);
            context.setPreferringWildcard(false);
            BaseBlock block = null;
            try {
                block = bf.parseFromInput(blockName, context);
            } catch (Exception ex) {
                sender.sendMessage(ChatColor.RED + ex.getMessage());
                return;
            }
            blockId = block.getId();
            blockData = (byte) block.getData();
        } else {
            // Fall back and use direct lookup. No black list.
            Material material;
            material = Material.matchMaterial(blockName);
            if (material == null) {
                sender.sendMessage(
                        ChatColor.RED + "Cannot find item: " + blockName);
                return;
            }
            blockId = material.getId();
            blockData = 0;
        }

        // Make sure the player is a participant
        List<SBAPlot> plots = _speedBuild.getPlots();
        int x, y, z;
        x = (int) player.getLocation().getX();
        y = (int) player.getLocation().getY();
        z = (int) player.getLocation().getZ();
        boolean foundPlot = false;
        for (SBAPlot plot : plots) {
            if (plot.getPlot().contains(x, y, z)) {
                // Make sure the player is registered with this plot
                if (!(plot.getPlot().getOwners().contains(player.getUniqueId())
                        || plot.getPlot().getMembers()
                                .contains(player.getUniqueId()))) {
                    sender.sendMessage(ChatColor.RED
                            + "Get off my lawn, you whippersnapper! Find your own plot!");
                    return;
                }

                // for each block
                int minx = plot.getFloor().getMinimumPoint().getBlockX();
                int miny = plot.getFloor().getMinimumPoint().getBlockY();
                int minz = plot.getFloor().getMinimumPoint().getBlockZ();
                int maxx = plot.getFloor().getMaximumPoint().getBlockX();
                int maxy = plot.getFloor().getMaximumPoint().getBlockY();
                int maxz = plot.getFloor().getMaximumPoint().getBlockZ();
                World w = player.getWorld();

                // getLogger().info(String.format("%d, %d, %d, %d, %d, %d",
                // minx, miny, minz, maxx, maxy, maxz));

                for (x = minx; x <= maxx; x++) {
                    for (y = miny; y <= maxy; y++) {
                        for (z = minz; z <= maxz; z++) {
                            BlockData orgData = null;
                            // getLogger().info(String.format("processing %d,
                            // %d, %d", minx, miny, minz));
                            Block b = w.getBlockAt(x, y, z);
                            if (_blocksHub != null) {
                                orgData = new BlockData(b.getTypeId(),
                                        b.getData());
                            }
                            b.setTypeId(blockId);
                            b.setData((byte) blockData);
                            if (_blocksHub != null) {
                                Vector pos = new Vector(x, y, z);
                                String owner = this.getName();
                                BlockData newData = new BlockData(b.getTypeId(),
                                        b.getData());
                                _blocksHub.logBlock(pos, owner, w.getName(),
                                        orgData, newData);
                            }
                        }
                    }
                }
                foundPlot = true;
                break;
            }
        }
        if (!foundPlot) {
            sender.sendMessage(
                    ChatColor.RED + "You must be standing in your plot");
            return;
        }

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
     * Return the world guard plugin.
     * 
     * @return World Guard
     */
    public WorldGuardPlugin getWorldGuard() {
        return _wg;
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
     * @param e
     *            Exception
     */
    public void printStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        getLogger().severe(sw.toString());
    }

}
