package nu.nerd.SpeedBuildArena;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Store plot config data
 *
 */
public class SBAConfigPlot {
    
    private String _regionPlot; /** name of plot's primary region */
    private String _regionFloor; /** name of a region that marks the floor. Works with /sba setfloor */
    
    /**
     * Load config data from yaml configuration section
     * @param conf
     * @throws Exception 
     */
    public SBAConfigPlot(ConfigurationSection conf) throws Exception {
        _regionPlot = conf.getString("region_plot");
        if(_regionPlot == null) {
            throw new Exception("region_plot sections cannot be null");
        }
        _regionFloor = conf.getString("region_floor");
        if(_regionFloor == null) {
            throw new Exception("region_floor sections cannot be null");
        }
    }
    
    /**
     * Get the plot's region name
     * @return Name of the region, or null.
     */
    public String getRegionPlot() {
        return _regionPlot;
    }
    
    /**
     * Get the plot's floor region name
     * @return Name of the region, or null.
     */
    public String getRegionFloor() {
        return _regionFloor;
    }
}
