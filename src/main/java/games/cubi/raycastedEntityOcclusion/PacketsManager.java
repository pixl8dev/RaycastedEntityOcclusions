package games.cubi.raycastedEntityOcclusion;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import games.cubi.raycastedEntityOcclusion.PDC.PlayerHidingManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class PacketsManager implements PacketListener {
    private RaycastedEntityOcclusion plugin;
    private PlayerHidingManager playerHidingManager;
    public PacketsManager(RaycastedEntityOcclusion plugin) {
        // This is run on load, not on enable.
        this.plugin = plugin;

    }
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.PLAYER_INFO_REMOVE) {
            return;
        }
        WrapperPlayServerPlayerInfoRemove removePacket = new WrapperPlayServerPlayerInfoRemove(event);
        Player player = event.getPlayer();
        List<UUID> playersBeingRemovedPreProcessing = removePacket.getProfileIds();
        if (playersBeingRemovedPreProcessing.size() != 1) {
            return;
        }
        Player playerBeingRemoved = Bukkit.getPlayer(playersBeingRemovedPreProcessing.getFirst());
        if (playerBeingRemoved == null || player == null) {
            return;
        }

        if (playerHidingManager == null) {
            // Initialize the PlayerHidingManager if it hasn't been initialized yet since the initialization of the PacketsManager is on load, not on enable.
            playerHidingManager = plugin.getPlayerHidingManager();
        }

        if (playerHidingManager.wasPlayerHidden(player, playerBeingRemoved)) {
            // If the player was hidden, we cancel the packet.
            Bukkit.getLogger().info("Player " + playerBeingRemoved.getName() + " was hidden from " + player.getName() + ", cancelling packet.");
            event.setCancelled(true);
            return;
        }

        Bukkit.getLogger().info("ergsergsergwtbsegseg ");
    }


}
