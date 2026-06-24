package me.drex.essentials.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-wide persisted data: warps.
 */
public class ServerData {

    /** name -> location */
    public Map<String, Location> warps = new LinkedHashMap<>();

    public Map<String, Location> warps() {
        if (warps == null) {
            warps = new LinkedHashMap<>();
        }
        return warps;
    }
}
