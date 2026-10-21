package fr.koriamc.warps.api.event;

import fr.koriamc.warps.model.Warp;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Un joueur va être téléporté à un warp ou au spawn (après le délai, avant de payer).
 * L'annuler empêche la téléportation.
 */
public final class WarpTeleportEvent extends PlayerEvent implements Cancellable {

    /** Destination : un warp ou le spawn. */
    public enum Destination {
        WARP,
        SPAWN
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Destination destination;
    private final Warp warp;
    private Location to;
    private boolean cancelled;

    public WarpTeleportEvent(Player player, Destination destination, Warp warp, Location to) {
        super(player);
        this.destination = destination;
        this.warp = warp;
        this.to = to;
    }

    public Destination getDestination() {
        return destination;
    }

    /** Le warp, ou null pour le spawn. */
    public Warp getWarp() {
        return warp;
    }

    public Location getTo() {
        return to;
    }

    public void setTo(Location to) {
        if (to != null && to.getWorld() != null) {
            this.to = to;
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
