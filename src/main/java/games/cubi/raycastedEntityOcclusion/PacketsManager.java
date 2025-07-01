package games.cubi.raycastedEntityOcclusion;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class PacketsManager implements PacketListener {
    public static void showTabName(Player p, Player other) {
        /*Bukkit.getLogger().info("Showing tab name for " + other.displayName() + " to " + p.getName());
        UserProfile profile = new UserProfile(other.getUniqueId(), other.getName());
        MiniMessage mm = MiniMessage.miniMessage();
        Component name = mm.deserialize("Test");
        PlayerInfo data = new PlayerInfo(
                profile,
                true,
                999,
                GameMode.ADVENTURE,
                //other.displayName(),
                name,
                null
        );

        WrapperPlayServerPlayerInfoUpdate addPacket = new WrapperPlayServerPlayerInfoUpdate(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, data);

        PacketEvents.getAPI().getPlayerManager().sendPacket(p, addPacket);*/

    }
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.PLAYER_INFO_REMOVE) {
            return;
        }
        WrapperPlayServerPlayerInfoRemove removePacket = new WrapperPlayServerPlayerInfoRemove(event);
        Player target = event.getPlayer();
        List<UUID> playersBeingRemovedPreProcessing = removePacket.getProfileIds();
        if (playersBeingRemovedPreProcessing.size() != 1) {
            return;
        }
        Player playerBeingRemoved = Bukkit.getPlayer(playersBeingRemovedPreProcessing.getFirst());
        Bukkit.getLogger().info("ergsergsergwtbsegseg ");
        event.setCancelled(true);
    }


}
