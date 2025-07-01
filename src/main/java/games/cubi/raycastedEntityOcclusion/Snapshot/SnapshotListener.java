package games.cubi.raycastedEntityOcclusion.Snapshot;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class SnapshotListener implements Listener {
    private final ChunkSnapshotManager manager;

    public SnapshotListener(ChunkSnapshotManager mgr) {
        this.manager = mgr;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        manager.onChunkLoad(e.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        manager.onChunkUnload(e.getChunk());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        manager.onBlockChange(e.getBlock().getLocation(), e.getBlock().getType());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        manager.onBlockChange(e.getBlock().getLocation(), Material.AIR);
    }

    @EventHandler
    public void onBurn(BlockBurnEvent e) {
        manager.onBlockChange(e.getBlock().getLocation(), Material.AIR);
    }
    // These events do not cover all cases, but I can't be bothered to figure out a better solution rn. Frequent snapshot refreshes is the solution. If anyone has a solutioon please let me know.
}