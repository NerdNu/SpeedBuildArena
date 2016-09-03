package nu.nerd.SpeedBuildArena.SBAScript;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

public class SBAFactory {
    /**
     * Load the SBA Script from the config file
     * @param sba SpeedBuildPlugin
     * @return New command list
     */
    public static ArrayList<SBACommand> loadFromConfig(FileConfiguration config)
        throws Exception {
        ArrayList<SBACommand> cmds = new ArrayList<SBACommand>();
        List<String> cmdList = config.getStringList("command_sequence");
        if(cmdList == null)
            throw new Exception("Failed to read arena_sequence from config.yml");

        int i = 0;
        for(String fullCommand : cmdList) {
            
            if(fullCommand == null)
                throw new Exception("Null command found in arena_sequence at step: " + Integer.toBinaryString(i));
            
            String[] parts = fullCommand.split(" ", 2);
            String cmdName = parts[0];
            String cmdArgs = null;
            if (parts.length > 1) {
                cmdArgs = parts[1];
            }
            
            switch (parts[0].toLowerCase()) {
            case "/msg":
                cmds.add(new SBAMsg(cmdArgs));
                break;
            case "/sleep":
                cmds.add(new SBASleep(cmdArgs));
                break;
            case "/startbossbar":
                cmds.add(new SBAStartBossBar(cmdArgs));
                break;
            case "/addplayers":
                cmds.add(new SBAAddPlayers(cmdArgs));
                break;
            case "/removeplayers":
                cmds.add(new SBARemovePlayers(cmdArgs));
                break;
            default:
                throw new Exception(String.format("Unknown command %s found at step %d.", cmdName, i));
            }
            i++;
        }
        return cmds;
    }
    
}
