package games.cubi.raycastedEntityOcclusion.Packets;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import games.cubi.raycastedEntityOcclusion.RaycastedEntityOcclusion;

public class PacketsListener implements PacketListener {
    private RaycastedEntityOcclusion plugin;
    private PacketProcessor packetProcessor;
    public PacketsListener(RaycastedEntityOcclusion plugin) {
        // This is run on load, not on enable.
        this.plugin = plugin;

    }
    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_REMOVE) {
            if (packetProcessor == null) {
                // Initialize the PacketProcessor if it hasn't been initialized yet since the initialization of the PacketsListener is on load, not on enable.
                packetProcessor = plugin.getPacketProcessor();
            }
            packetProcessor.processPlayerInfoRemovePacket(event);
        }

    }


}
