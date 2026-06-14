package com.pewez.fantasticessentials.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-player persisted data: homes and the "back" location.
 */
public class PlayerData {

    /** name -> location */
    public Map<String, Location> homes = new LinkedHashMap<>();

    /** Last location for the /back command (set on teleport and death). */
    public Location lastLocation = null;

    public Map<String, Location> homes() {
        if (homes == null) {
            homes = new LinkedHashMap<>();
        }
        return homes;
    }
}
