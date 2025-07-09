package games.cubi.raycastedEntityOcclusion;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    public int snapshotRefreshInterval;
    public int engineMode;
    public int maxOccludingCount;
    public boolean debugMode;
    public int alwaysShowRadius;
    public int raycastRadius;
    public int searchRadius;
    public boolean cullPlayers;
    public boolean onlyCullSneakingPlayers;
    public int recheckInterval;
    public boolean checkTileEntities;
    public int tileEntityRecheckInterval;
    public FileConfiguration cfg;
    public boolean packetEventsPresent = false;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        cfg = plugin.getConfig();

        snapshotRefreshInterval = cfg.getInt("snapshot-refresh-interval", 60);
        engineMode = cfg.getInt("engine-mode", 1);
        maxOccludingCount = cfg.getInt("max-occluding-count", 3);
        debugMode = cfg.getBoolean("debug-mode", false);

        alwaysShowRadius = cfg.getInt("always-show-radius", 8);
        raycastRadius = cfg.getInt("raycast-radius", 48);
        searchRadius = cfg.getInt("search-radius", 48);
        cullPlayers = cfg.getBoolean("cull-players", false);
        onlyCullSneakingPlayers = cfg.getBoolean("only-cull-sneaking-players", false);
        recheckInterval = cfg.getInt("recheck-interval", 20);

        checkTileEntities = cfg.getBoolean("check-tile-entities", false);
        tileEntityRecheckInterval = cfg.getInt("tile-entity-recheck-interval", 0);

        // Write defaults if missing
        cfg.addDefault("snapshot-refresh-interval", 60);
        cfg.addDefault("engine-mode", 1);
        cfg.addDefault("max-occluding-count", 3);
        cfg.addDefault("debug-mode", false);
        cfg.addDefault("always-show-radius", 8);
        cfg.addDefault("raycast-radius", 48);
        cfg.addDefault("search-radius", 48);
        cfg.addDefault("cull-players", false);
        cfg.addDefault("only-cull-sneaking-players", false);
        cfg.addDefault("recheck-interval", 20);
        cfg.addDefault("check-tile-entities", false);
        cfg.addDefault("tile-entity-recheck-interval", 0);
        cfg.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public int setConfigValue(String path, String rawValue) {
        if (!cfg.contains(path)) return -1;
        Object current = cfg.get(path);
        Object parsed;
        if (current instanceof Boolean) {
            String lower = rawValue.toLowerCase();
            if (!lower.equals("true") && !lower.equals("false")) return -1;
            parsed = Boolean.parseBoolean(lower);
        } else if (current instanceof Number) {
            int intVal;
            try {
                intVal = Integer.parseInt(rawValue);
            } catch (NumberFormatException e) {
                return -1;
            }
            if (intVal < 0 || intVal > 256) return 0;
            parsed = intVal;
        } else {
            return -1;
        }
        cfg.set(path, parsed);
        plugin.saveConfig();
        load();
        return 1;
        /*
        -1 = invalid input
        0 = out of range
        1 = success
         */
    }
    public void setPacketEventsPresent(boolean present) {
        this.packetEventsPresent = present;
    }
}