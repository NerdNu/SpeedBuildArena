package nu.nerd.SpeedBuildArena;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * Store plot data during a SpeedBuildArena event
 *
 */
public class SBAPlot {

    private ProtectedRegion _plot;
    private ProtectedRegion _floor;

    /**
     * Create new plot data object
     * 
     * @param config
     *            Plot config to load
     * @param wgrm
     *            World Guard Region Manager to pull plots from
     * @throws Exception
     */
    public SBAPlot(SBAConfigPlot config, RegionManager wgrm) throws Exception {
        _plot = wgrm.getRegion(config.getRegionPlot());
        if (_plot == null)
            throw new Exception(
                    "Cannot find region: " + config.getRegionPlot());
        _floor = wgrm.getRegion(config.getRegionFloor());
        if (_floor == null)
            throw new Exception(
                    "Cannot find region: " + config.getRegionFloor());
    }

    /**
     * Get the plot region
     * 
     * @return The region
     */
    public ProtectedRegion getPlot() {
        return _plot;
    }

    /**
     * Get the floor region
     * 
     * @return The region
     */
    public ProtectedRegion getFloor() {
        return _floor;
    }

}
