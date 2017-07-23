package nu.nerd.SpeedBuildArena;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
//import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import com.mewin.WGRegionEvents.events.RegionEnteredEvent;
import com.mewin.WGRegionEvents.events.RegionLeftEvent;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import nu.nerd.SpeedBuildArena.SBAScript.SBACommand;

/**
 * 
 * Control a single round of speed build
 * 
 * Manages the following items:
 * * Grants plot permissions to users standing in plots
 * * Runs the time announcements
 * * Manages the boss bar
 * * Clears permissions at the end of the round
 *
 */
public class SBA implements AutoCloseable, Runnable, Listener {
	private SBAPlugin _sba;
	private SBAConfig _config;
	private RegionManager _wgrm;
	private List<SBAPlot> _plots;
	private ProtectedRegion _arena;

	private long _nextCommandTime; // Time to run the next SBA command
	private List<SBACommand> _script; // Array of SBA commands
	private int _commandIndex = 0; // Instruction Pointer
	
	private BossBar _bossBar;
	private long _bossBarStartTime;
	private long _bossBarNextUpdateTime;
	private long _bossBarStopTime;
	
	/** Try to not leak these in the server */
	private BukkitTask _task = null;
	
	private boolean _running = false;
	
	/**
	 * Make a new speed build round
	 * @param sba
	 */
	public SBA(SBAPlugin sba) throws Exception {
		_sba = sba;
		
		_config = _sba.getSBAConfig();
		if(!_config.VALID) {
		    throw new Exception("Configuration invalid");
		}
		_script = _config.SCRIPT;

		World world = _sba.getServer().getWorld(_config.WORLD_NAME);
		if(world == null) {
		    logNthrow("Unknown world \"%s\"", _config.WORLD_NAME);
		}

		// Get the region manager for the world
		_wgrm = _sba.getWorldGuard().getRegionManager(world);
		if(_wgrm == null) {
		    logNthrow("Failed to get World guard RegionManager for world: \"%s\".", world);
		}
		
		// Get the arena plot
		_arena = _wgrm.getRegion(_config.ARENA_PLOT_NAME);
		if(_arena == null) {
		    logNthrow("Unknown plot: \"%s\"", _config.ARENA_PLOT_NAME);
		}

		// Get all the plots
		_plots = new ArrayList<SBAPlot>();
		for(SBAConfigPlot plot : _config.PLOTS) {
		    _plots.add(new SBAPlot(plot, _wgrm));
		}
		
		// Fire up the task scheduler
		_task = _sba.getServer().getScheduler().runTaskTimer(_sba, this, 0, 1);
		_nextCommandTime = System.currentTimeMillis();
		_running = true;
	}
	
	
	/**
	 * Log and throw an exception
	 * @param format Format String
	 * @param args Arguments for the format string
	 * @throws Exception Always throws an exception
	 */
	private void logNthrow(String format, Object... args) throws Exception {
	    String msg = String.format(format, args);
	    _sba.getLogger().warning(msg);
	    throw new Exception(msg);
	}
	
	/**
	 * Remove all owners and members from all sba plots
	 */
	public void wipePlots() {
	    //Server server = _sba.getServer();
        //ConsoleCommandSender console = server.getConsoleSender();
	    //String worldName = _config.WORLD_NAME;
        try {
            for(SBAPlot plot : _plots) {
                //server.dispatchCommand(console, String.format("rg removeowner %s -a -w %s", plot.getPlot().getId(), worldName));
                //server.dispatchCommand(console, String.format("rg removemember %s -a -w %s", plot.getPlot().getId(), worldName));
                plot.getPlot().getOwners().clear();
                plot.getPlot().getMembers().clear();
            }
        } catch (Throwable ex) {
            _sba.printStackTrace(ex);
        }
	}
	
	/**
	 * Shutdown anything that _really_ needs to be shutdown
	 */
	@Override
	public void close() {
	    _running = false;
	    wipePlots();
	    stopBossBar();
	    if(_task != null) {
	        _task.cancel();
	        _task = null;
	    }
		_sba.onSpeedBuildFinished(); // This deletes self reference so we can be GCed
		_sba = null;
	    _config = null;
	    _wgrm = null;
	    _plots = null;
	    _arena = null;
	}

	
	/**
	 * Run the Speed Build Arena Script
	 */
    @Override
    public void run() { 
        long now = System.currentTimeMillis();

        // Do the boss bar
        if(_bossBar != null) {
            if (now > _bossBarStopTime) {
                stopBossBar();
            } else if (now > _bossBarNextUpdateTime) {
                _bossBarNextUpdateTime += 1000;
                _bossBar.setProgress(
                        1.0 - ((double)now              - (double)_bossBarStartTime)
                            / ((double)_bossBarStopTime - (double)_bossBarStartTime)
                        );
                updateBossTitle(_bossBarStopTime - now);
            }
        }
        
        // Process all the commands.
        // NOTE: The /sleep command will update _nextCommandTime.
        if(_commandIndex < _script.size() && now > _nextCommandTime) {
            SBACommand cmd = _script.get(_commandIndex);
            cmd.execute(this);
            _commandIndex++;
        }
        
        // If _commandIndex is at the end, shut down
        if (_commandIndex >= _script.size()) {
            close();
        }
    }
    
    
    /**
     * Add players to the boss bar if they enter the Speed Build Arena
     * @param event The Event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerEnteredRegion(RegionEnteredEvent event) {
        if(event.getRegion() == _arena && _bossBar != null) {
            _bossBar.addPlayer(event.getPlayer());
        }
    }
    
    
    /**
     * Remove players from the boss bar if they leave the Speed Build Arena
     * @param event The Event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeftRegion(RegionLeftEvent event) {
        if(event.getRegion() == _arena && _bossBar != null) {
            _bossBar.removePlayer(event.getPlayer());
        }
    }


    /**
     * Get the SpeedBuildPlugin
     * @return SpeedBuildPlugin
     */
    public SBAPlugin getPlugin() {
        return _sba;
    }
    
    
    /**
     * Get the Minecraft Server
     * @return Minecraft Server
     */
    public Server getServer() {
        return _sba.getServer();
    }
    
    
    /**
     * Add time to the command timer. This will delay the next command.
     * This is mainly used by the sleep command.
     */
    public void addTime(long offset) {
        _nextCommandTime += offset;
    }
    
    
    /**
     * See if this speed build event is running or not
     * @return running
     */
    public boolean isRunning() {
        return _running;
    }
    
    
    /**
     * Get the World Guard Region Manager
     * @return RegionManager
     */
    public RegionManager getRegionManager() {
        return _wgrm;
    }

    
    /**
     * Get Plot data
     * @return The plots
     */
    public List<SBAPlot> getPlots() {
        return _plots;
    }


    /**
     * Return the master speed build arena plot. Assume all plots in
     * the arena are inside the arena plot.
     * @return Arena Plot
     */
    public ProtectedRegion getArenaPlot() {
        return _arena;
    }
    
    
    
    /**
     * Start running the boss bar
     * @param duration Time to run the boss bar in milliseconds
     */
    public void startBossTimer(long duration) {
        if (_bossBar == null) {
            _bossBar = _sba.getServer().createBossBar("", BarColor.BLUE, BarStyle.SOLID);
            _bossBar.setProgress(1.0);
            _bossBar.setVisible(false);
            updateBossTitle(duration);

            // Register for events
            if (_config.HAVE_WGREGION_EVENTS) {
                _sba.getServer().getPluginManager().registerEvents(this, _sba);
            }
        }
        
        // Show the boss bar to all players in Speed Build Arena
        for(Player player : getServer().getOnlinePlayers()) {
            int x, y, z;
            x = (int)player.getLocation().getX();
            y = (int)player.getLocation().getY();
            z = (int)player.getLocation().getZ();
            if(_arena.contains(x, y, z)) {
                _bossBar.addPlayer(player);
            }
        }
        _bossBar.setVisible(true);
        
        // Setup all the timers
        _bossBarStartTime = System.currentTimeMillis();
        _bossBarStopTime = _bossBarStartTime + duration;
        _bossBarNextUpdateTime = _bossBarStartTime + 1000;
    }
    
    
    
    /**
     * Shutdown and stop the boss bar
     */
    public void stopBossBar() {
        if (_bossBar != null) {
            _bossBar.removeAll();
            _bossBar.setVisible(false);
            _bossBar = null;
        }
        HandlerList.unregisterAll(this);
    }
    
    
    /**
     * Update the boss title with the time remaining
     * @param timeRemaining
     */
    private void updateBossTitle(long timeRemaining) {
        if (_bossBar != null) {
            long acc = timeRemaining;
            // round up to the nearest second. We have to do this to stay in sync with
            // the messages
            acc = (acc + 999)/ 1000; // milliseconds
            long seconds = acc % 60;
            long minutes = acc / 60;
            
            _bossBar.setTitle(String.format("Speed Build %02d:%02d", minutes, seconds));
        }
    }
 
}

