package games.cubi.raycastedEntityOcclusion.PDC;

import games.cubi.raycastedEntityOcclusion.RaycastedEntityOcclusion;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerHidingManager {
    private RaycastedEntityOcclusion plugin;
    public PlayerHidingManager(RaycastedEntityOcclusion plugin) {
        this.plugin = plugin;
    }

    public void markPlayerAsHidden(Player player, Player target) {
        if (player == null || target == null) return;
        NamespacedKey key = getKey(target);
        long time = System.currentTimeMillis();
        player.getPersistentDataContainer().set(key, PersistentDataType.LONG, time);
        target.getPersistentDataContainer().set(key, PersistentDataType.LONG, time);

    }
    public boolean wasPlayerHidden(Player player, Player target) {
        if (player == null || target == null) return false;

        PersistentDataContainer container = player.getPersistentDataContainer();
        long time = -1;
        NamespacedKey key = getKey(target);
        if (container.has(key, PersistentDataType.LONG)) {
            time = container.get(key, PersistentDataType.LONG);
        }
        player.getPersistentDataContainer().remove(getKey(target));
        if (time == -1) return false;
        if (System.currentTimeMillis() - time < 1000) {
            return true;
        }
        return false;
    }

    private NamespacedKey getKey(Player p) {
        return new NamespacedKey(plugin, p.getName());
    }

}
