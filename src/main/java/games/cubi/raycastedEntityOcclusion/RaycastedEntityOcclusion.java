package games.cubi.raycastedEntityOcclusion;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import games.cubi.raycastedEntityOcclusion.PDC.PlayerHidingManager;
import games.cubi.raycastedEntityOcclusion.Raycast.Engine;
import games.cubi.raycastedEntityOcclusion.Raycast.MovementTracker;
import games.cubi.raycastedEntityOcclusion.Snapshot.ChunkSnapshotManager;
import games.cubi.raycastedEntityOcclusion.Snapshot.SnapshotListener;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class RaycastedEntityOcclusion extends JavaPlugin implements CommandExecutor {
    private ConfigManager cfg;
    private ChunkSnapshotManager snapMgr;
    private MovementTracker tracker;
    private CommandsManager commands;
    private boolean packetEventsPresent = false;
    private VersionIndependentMethods vim;
    private PlayerHidingManager playerHidingManager;

    public int tick = 0;

    @Override
    public void onLoad() {
        Plugin packetEvents = Bukkit.getPluginManager().getPlugin("packetevents");
        if (packetEvents != null) {
            packetEventsPresent = true;
            getLogger().info("PacketEvents detected.");
            //PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
            //On Bukkit, calling this here is essential, hence the name "load"
            //PacketEvents.getAPI().load();
            PacketEvents.getAPI().getEventManager().registerListener(
                    new PacketsManager(this), PacketListenerPriority.NORMAL);

        } else {
            getLogger().info("PacketEvents not detected, disabling packet-based tablist modification. Don't worry, the plugin will still work without it.");
        }
    }

    @Override
    public void onEnable() {
        cfg = new ConfigManager(this);
        vim = new VersionIndependentMethods(this, cfg, getServer().getVersion());
        snapMgr = new ChunkSnapshotManager(this);
        tracker = new MovementTracker(this);
        commands = new CommandsManager(this, cfg);
        playerHidingManager = new PlayerHidingManager(this);
        getServer().getPluginManager().registerEvents(new SnapshotListener(snapMgr), this);
        getServer().getPluginManager().registerEvents(new UpdateChecker(this), this);

        //Brigadier API
        LiteralCommandNode<CommandSourceStack> buildCommand = commands.registerCommand();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(buildCommand);
            //alias "reo"
            commands.registrar().register(Commands.literal("reo")
                    .requires(sender -> sender.getSender().hasPermission("raycastedentityocclusions.command"))
                    .executes(context -> {
                        new CommandsManager(this, cfg).helpCommand(context);
                        return Command.SINGLE_SUCCESS;
                    })
                    .redirect(buildCommand).build());
        });

        //bStats
        int pluginId = 24553;
        new Metrics(this, pluginId);

        new BukkitRunnable() {
            @Override
            public void run() {
                tick++;
                Engine.runEngine(cfg, snapMgr, tracker, RaycastedEntityOcclusion.this);
                Engine.runTileEngine(cfg, snapMgr, tracker, RaycastedEntityOcclusion.this);
            }
        }.runTaskTimer(this, 0L, 1L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (packetEventsPresent && Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
                    cfg.setPacketEventsPresent(true);
                    getLogger().info("PacketEvents is enabled, enabling packet-based tablist modification.");
                }
            }
        }.runTaskLater(this, 1L);
    }


    public ConfigManager getConfigManager() {
        return cfg;
    }
    public ChunkSnapshotManager getChunkSnapshotManager() {
        return snapMgr;
    }
    public MovementTracker getMovementTracker() {
        return tracker;
    }
    public CommandsManager getCommandsManager() {
        return commands;
    }
    public PlayerHidingManager getPlayerHidingManager() {
        return playerHidingManager;
    }
}