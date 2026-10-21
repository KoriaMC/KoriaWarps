package fr.koriamc.warps.listener;

import fr.koriamc.warps.KoriaWarps;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Annule la téléportation en attente si le joueur bouge ou prend des dégâts.
 */
public final class TeleportListener implements Listener {

    private final KoriaWarps plugin;

    public TeleportListener(KoriaWarps plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.settings().cancelOnMove) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        // tourner la tête ne compte pas
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        Player player = event.getPlayer();
        Location start = plugin.teleporter().pendingStart(player);
        if (start == null) {
            return;
        }
        if (!start.getWorld().equals(to.getWorld()) || start.getBlockX() != to.getBlockX()
                || start.getBlockY() != to.getBlockY() || start.getBlockZ() != to.getBlockZ()) {
            plugin.teleporter().cancel(player, "teleport.cancelled-move");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (plugin.settings().cancelOnDamage && event.getEntity() instanceof Player
                && plugin.teleporter().isPending((Player) event.getEntity())) {
            plugin.teleporter().cancel((Player) event.getEntity(), "teleport.cancelled-damage");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.teleporter().forget(event.getPlayer());
    }
}
