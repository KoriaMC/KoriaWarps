package fr.koriamc.warps.listener;

import fr.koriamc.warps.KoriaWarps;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Spawn automatique : première connexion (ou chaque connexion), réapparition après la mort
 * et chute dans le vide.
 */
public final class SpawnListener implements Listener {

    private final KoriaWarps plugin;

    public SpawnListener(KoriaWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        boolean first = !player.hasPlayedBefore();
        if (!(plugin.settings().spawnEveryJoin || (first && plugin.settings().spawnFirstJoin))) {
            return;
        }
        // 2 ticks plus tard : après KoriaMultiverse (arrivee.premier-monde) et les autres plugins
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                Location spawn = plugin.spawns().locationFor(player.getWorld());
                if (spawn != null) {
                    player.setFallDistance(0);
                    player.teleport(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            }
        }, 2L);
    }

    /** Priorité la plus haute : passe après la réapparition par monde de KoriaMultiverse. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.settings().spawnOnRespawn || (event.isBedSpawn() && !plugin.settings().respawnIgnoreBed)) {
            return;
        }
        Location spawn = plugin.spawns().locationFor(event.getPlayer().getWorld());
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVoid(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || to.getY() >= plugin.settings().voidHeight || event.getFrom().getY() < to.getY()
                || !plugin.settings().watchesVoid(to.getWorld().getName())) {
            return;
        }
        Player player = event.getPlayer();
        Location spawn = plugin.spawns().locationFor(player.getWorld());
        if (spawn == null) {
            return;
        }
        plugin.teleporter().cancel(player, null);
        player.setFallDistance(0);
        event.setTo(spawn);
        plugin.msg().send(player, "spawn.void");
    }
}
