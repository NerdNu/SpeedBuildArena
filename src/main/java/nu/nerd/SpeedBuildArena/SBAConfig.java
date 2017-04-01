package nu.nerd.SpeedBuildArena;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import nu.nerd.SpeedBuildArena.SBAScript.SBACommand;
import nu.nerd.SpeedBuildArena.SBAScript.SBAFactory;

/**
 * Load config.yml
 */    
public class SBAConfig {
    
    /** Set to true if the config is valid */
    public boolean VALID;
    
    /** Add players as owners or members */
    public boolean ADD_OWNERS;
    
    /** MineCraft world name where the plots are */
    public String WORLD_NAME;
    
    /** Name of the minecraft arena region */
    public String ARENA_PLOT_NAME;
    
    /** Name of all the speed build plots */
    //public List<String> PLOT_NAMES;
    
    /** Plot data */
    public List<SBAConfigPlot> PLOTS;
    
    /** List of commands to be executed */
    public List<SBACommand> SCRIPT;
    
    /** Not actually in the yml, but we need to know of WGRegionEvents is installed or not */
    public boolean HAVE_WGREGION_EVENTS;
    
    public void load(SBAPlugin sba) throws Exception {
        VALID = false;
        sba.reloadConfig();
        
        FileConfiguration config = sba.getConfig();
        
        ADD_OWNERS = config.getBoolean("add_owners", false);
        
        WORLD_NAME = config.getString("world_name", "world");
        
        ARENA_PLOT_NAME = config.getString("arena_plot_name");
        if(ARENA_PLOT_NAME == null) {
            throw new Exception("Invalid config.yml. \"arena_plot_name\" is missing or corrupt");
        }

        PLOTS = new ArrayList<SBAConfigPlot>();
        if(PLOTS == null) {
            throw new Exception("Invalid config.yml: \"plots\" missing or corrupt");
        }
        ConfigurationSection section = config.getConfigurationSection("plots");
        for (String key : section.getKeys(false)) {
            PLOTS.add(new SBAConfigPlot(section.getConfigurationSection(key)));
        }
    
        SCRIPT = SBAFactory.loadFromConfig(config);
        
        if(SCRIPT == null || SCRIPT.isEmpty()) {
            throw new Exception("Empty command sequence");
        }
        
        HAVE_WGREGION_EVENTS = sba.getServer().getPluginManager().getPlugin("WGRegionEvents") != null;
        
        VALID = true;
    }
}
