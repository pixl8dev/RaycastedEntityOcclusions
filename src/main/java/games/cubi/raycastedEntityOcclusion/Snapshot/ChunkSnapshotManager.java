package games.cubi.raycastedEntityOcclusion.Snapshot;

import games.cubi.raycastedEntityOcclusion.ConfigManager;
import games.cubi.raycastedEntityOcclusion.Logger;
import games.cubi.raycastedEntityOcclusion.Raycast.Engine;
import games.cubi.raycastedEntityOcclusion.RaycastedEntityOcclusion;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkSnapshotManager {
    public static class Data {
        public final ChunkSnapshot snapshot;
        public final ConcurrentHashMap<Location, Material> delta = new ConcurrentHashMap<>();
        public final Set<Location> tileEntities = ConcurrentHashMap.newKeySet();
        public long lastRefresh;
        public int minHeight;
        public int maxHeight;
        public final Set<Player> trackingPlayers = ConcurrentHashMap.newKeySet();

        public Data(ChunkSnapshot snapshot, long time) {
            this.snapshot = snapshot;
            this.lastRefresh = time;
        }
    }

    private static final ConcurrentHashMap<String, Data> dataMap = new ConcurrentHashMap<>();
    private final ConfigManager cfg;
    private final Set<Chunk> activeChunks = ConcurrentHashMap.newKeySet();
    private static final int BLOCK_VIEW_DISTANCE = 5; // 5 block radius around each player

    public ChunkSnapshotManager(RaycastedEntityOcclusion plugin) {
        cfg = plugin.getConfigManager();
        
        // Start player position tracking task
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerChunks();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second

        // Chunk refresh task - only refresh chunks that are actively being used
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                int chunksRefreshed = 0;
                int chunksToRefreshMaximum = Math.max(1, getNumberOfCachedChunks() / 3);
                
                // Only refresh chunks that are being actively tracked
                for (Chunk chunk : activeChunks) {
                    String key = key(chunk);
                    Data data = dataMap.get(key);
                    if (data != null && now - data.lastRefresh >= cfg.snapshotRefreshInterval * 1000L 
                            && chunksRefreshed < chunksToRefreshMaximum) {
                        chunksRefreshed++;
                        snapshotChunk(chunk);
                    }
                }
                
                if (cfg.debugMode) {
                    Logger.info("ChunkSnapshotManager: Refreshed " + chunksRefreshed + " chunks out of " + chunksToRefreshMaximum + " maximum.");
                }
            }
        }.runTaskTimerAsynchronously(plugin, cfg.snapshotRefreshInterval * 2L, cfg.snapshotRefreshInterval * 2L);
    }

    public void onChunkLoad(Chunk c) {
        // Only snapshot if it's within a player's view distance
        if (isChunkNearPlayers(c)) {
            snapshotChunk(c);
        }
    }

    public void onChunkUnload(Chunk c) {
        // Don't immediately remove the snapshot, it might still be needed
        // The cleanup will handle removing unused chunks
    }

    public void snapshotChunk(Chunk c) {
        if (c == null || !c.isLoaded()) return;
        
        String chunkKey = key(c);
        Data data = dataMap.get(chunkKey);
        long now = System.currentTimeMillis();
        
        // Only take a new snapshot if we don't have one or it's expired
        if (data == null || now - data.lastRefresh >= cfg.snapshotRefreshInterval * 1000L) {
            if (cfg.debugMode) {
                Logger.info("ChunkSnapshotManager: Taking snapshot of chunk " + chunkKey);
            }
            dataMap.put(chunkKey, takeSnapshot(c, now));
        }
        
        // Mark as active
        activeChunks.add(c);
    }
    public void snapshotChunk(String key) {
        snapshotChunk(getKeyChunk(key));
    }
    public void removeChunkSnapshot(Chunk c) {
        if (c == null) return;
        
        String chunkKey = key(c);
        Data data = dataMap.get(chunkKey);
        if (data != null) {
            // Only remove if no players are tracking this chunk
            if (data.trackingPlayers.isEmpty()) {
                if (cfg.debugMode) {
                    Logger.info("ChunkSnapshotManager: Removing snapshot of chunk " + chunkKey);
                }
                dataMap.remove(chunkKey);
                activeChunks.remove(c);
            }
        }
    }

    // Used by EventListener to update the delta map when a block is placed or broken
    public void onBlockChange(Location loc, Material m) {
        if (loc == null) return;
        
        Chunk chunk = loc.getChunk();
        if (chunk == null || !chunk.isLoaded()) return;
        
        if (cfg.debugMode) {
            Logger.info("ChunkSnapshotManager: Block change at " + loc + " to " + m);
        }
        
        // Only update if we have a snapshot for this chunk
        String chunkKey = key(chunk);
        Data d = dataMap.get(chunkKey);
        
        // If we don't have a snapshot but the chunk is near players, create one
        if (d == null && isChunkNearPlayers(chunk)) {
            snapshotChunk(chunk);
            d = dataMap.get(chunkKey);
        }
        
        if (d != null) {
            d.delta.put(blockLoc(loc), m);
            if (cfg.checkTileEntities) {
                // Check if the block is a tile entity
                BlockState data = loc.getBlock().getState();
                loc = loc.clone().add(0.5, 0.5, 0.5);
                if (data instanceof TileState) {
                    if (cfg.debugMode){
                        Logger.info("ChunkSnapshotManager: Tile entity at " + loc);
                    }
                    d.tileEntities.add(loc);
                } else {
                    d.tileEntities.remove(loc);
                }
            }
        }
        else {Logger.error("Data map value empty, ignoring block update!");}
    }

    private Data takeSnapshot(Chunk c, long now) {
        World w = c.getWorld();
        Data data = new Data(c.getChunkSnapshot(), now);
        int chunkX = c.getX() * 16;
        int chunkZ = c.getZ() * 16;
        int minHeight = w.getMinHeight();
        int maxHeight = w.getMaxHeight();
        data.maxHeight = maxHeight;
        data.minHeight = minHeight;
        if (cfg.checkTileEntities) {
            for (int x = 0; x < 16; x++) {
                for (int y = minHeight; y < maxHeight; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState bs = data.snapshot.getBlockData(x, y, z).createBlockState();

                        if (bs instanceof TileState) {
                            data.tileEntities.add(new Location(w, x+ chunkX +0.5, y+0.5, z + chunkZ+0.5));
                        }
                    }
                }
            }
        }
        return data;
    }

    public String key(Chunk c) {
        return c.getWorld().getName() + ":" + c.getX() + ":" + c.getZ();
    }
    public String key(World world, int x, int z) {
        return world.getName() + ":" + x + ":" + z;
    }
    public int getKeyX(String key) {
        String[] parts = key.split(":");
        return Integer.parseInt(parts[1]);
    }
    public int getKeyZ(String key) {
        String[] parts = key.split(":");
        return Integer.parseInt(parts[2]);
    }
    public World getKeyWorld(String key) {
        String[] parts = key.split(":");
        return Bukkit.getWorld(parts[0]);
    }
    public Chunk getKeyChunk(String key) {
        return getKeyWorld(key).getChunkAt(getKeyX(key), getKeyZ(key));
    }

    public Material getMaterialAt(Location loc) {
        if (loc == null) return Material.AIR;
        
        Chunk chunk = loc.getChunk();
        if (chunk == null || !chunk.isLoaded()) return Material.AIR;
        
        String chunkKey = key(chunk);
        Data d = dataMap.get(chunkKey);
        
        // If we don't have a snapshot, check if we should create one
        if (d == null) {
            // Only create a snapshot if the chunk is near players
            if (isChunkNearPlayers(chunk)) {
                snapshotChunk(chunk);
                d = dataMap.get(chunkKey);
            }
            
            // If we still don't have a snapshot, return AIR
            if (d == null) {
                return Material.AIR;
            }
        }
        double yLevel = loc.getY();
        if (yLevel < d.minHeight || yLevel > d.maxHeight) {
            return null;
        }
        Material dm = d.delta.get(blockLoc(loc));
        if (dm != null) {
            if (cfg.debugMode) Logger.info("Using delta");
            return dm;
        }
        int x = loc.getBlockX() & 0xF;
        int y = loc.getBlockY();
        int z = loc.getBlockZ() & 0xF;

        return d.snapshot.getBlockType(x, y, z);
    }

    //get TileEntity Locations in chunk
    public Set<Location> getTileEntitiesInChunk(World world, int x, int z) {
        if (world == null) return Collections.emptySet();
        
        Data d = dataMap.get(key(world, x, z));
        if (d != null) {
            return d.tileEntities;
        } else {
            // Try to get the chunk and create a snapshot if needed
            Chunk chunk = world.getChunkAt(x, z);
            if (chunk != null && chunk.isLoaded() && isChunkNearPlayers(chunk)) {
                snapshotChunk(chunk);
                d = dataMap.get(key(world, x, z));
                if (d != null) {
                    return d.tileEntities;
                }
            }
            return Collections.emptySet();
        }
    }

    public void removeTileEntity(Location loc) {
        Chunk c = loc.getChunk();
        Data d = dataMap.get(key(c));
        if (d != null) {
            d.tileEntities.remove(blockLoc(loc));
            Logger.info("ChunkSnapshotManager: Removed tile entity at " + loc);
        } else {
            Logger.error("ChunkSnapshotManager: No snapshot for " + c + " when removing tile entity at " + loc);
        }
    }

    public int getNumberOfCachedChunks() {
        return dataMap.size();
    }
    
    /**
     * Updates the chunks that should be tracked based on player positions
     */
    private void updatePlayerChunks() {
        Set<Chunk> chunksToKeep = new HashSet<>();
        
        // First, clear all tracking data
        for (Data data : dataMap.values()) {
            data.trackingPlayers.clear();
        }
        
        // Track blocks around each player
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) continue;
            
            Location playerLoc = player.getLocation();
            World world = playerLoc.getWorld();
            if (world == null) continue;
            
            // Get player's chunk
            Chunk centerChunk = playerLoc.getChunk();
            
            // Calculate the block coordinates for the 5-block radius
            int centerX = playerLoc.getBlockX();
            int centerY = playerLoc.getBlockY();
            int centerZ = playerLoc.getBlockZ();
            
            // Track all chunks that contain blocks within the 5-block radius
            int minX = centerX - BLOCK_VIEW_DISTANCE;
            int maxX = centerX + BLOCK_VIEW_DISTANCE;
            int minZ = centerZ - BLOCK_VIEW_DISTANCE;
            int maxZ = centerZ + BLOCK_VIEW_DISTANCE;
            
            // Convert block coordinates to chunk coordinates
            int minChunkX = minX >> 4; // Same as dividing by 16
            int maxChunkX = maxX >> 4;
            int minChunkZ = minZ >> 4;
            int maxChunkZ = maxZ >> 4;
            
            // Process each chunk that might contain blocks in the radius
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                    if (chunk != null && chunk.isLoaded()) {
                        chunksToKeep.add(chunk);
                        
                        // Add player to tracking data for this chunk
                        String chunkKey = key(chunk);
                        Data data = dataMap.get(chunkKey);
                        if (data != null) {
                            data.trackingPlayers.add(player);
                        } else {
                            // Take a snapshot of the chunk if it's not already in the cache
                            snapshotChunk(chunk);
                            data = dataMap.get(chunkKey);
                            if (data != null) {
                                data.trackingPlayers.add(player);
                            }
                        }
                    }
                }
            }
        }
        
        // Clean up chunks that are no longer needed
        for (Chunk chunk : activeChunks) {
            if (!chunksToKeep.contains(chunk)) {
                removeChunkSnapshot(chunk);
            }
        }
    }
    
    /**
     * Checks if a chunk contains any blocks within the 5-block radius of any player
     */
    private boolean isChunkNearPlayers(Chunk chunk) {
        if (chunk == null || !chunk.isLoaded()) return false;
        
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        // Calculate the world coordinates of the chunk's corners
        int minX = chunkX << 4; // Same as multiplying by 16
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) continue;
            
            Location loc = player.getLocation();
            if (!loc.getWorld().equals(world)) continue;
            
            int playerX = loc.getBlockX();
            int playerZ = loc.getBlockZ();
            
            // Calculate the closest point in the chunk to the player
            int closestX = Math.max(minX, Math.min(playerX, maxX));
            int closestZ = Math.max(minZ, Math.min(playerZ, maxZ));
            
            // Calculate squared distance to avoid square root
            int dx = playerX - closestX;
            int dz = playerZ - closestZ;
            int distanceSquared = dx * dx + dz * dz;
            
            if (distanceSquared <= BLOCK_VIEW_DISTANCE * BLOCK_VIEW_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    public static Location blockLoc(Location fullLoc) {
        Location blockLoc = fullLoc.toBlockLocation();
        blockLoc.setYaw(0);
        blockLoc.setPitch(0);
        return blockLoc;
    }

}
