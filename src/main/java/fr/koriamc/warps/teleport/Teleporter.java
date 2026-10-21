package fr.koriamc.warps.teleport;

import fr.koriamc.warps.KoriaWarps;
import fr.koriamc.warps.api.event.WarpTeleportEvent;
import fr.koriamc.warps.hook.VaultHook;
import fr.koriamc.warps.model.StoredLocation;
import fr.koriamc.warps.model.Warp;
import fr.koriamc.warps.util.Sounds;
import fr.koriamc.warps.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Téléportations vers les warps et le spawn : délai avec compte à rebours (annulé si le joueur
 * bouge ou prend des dégâts), temps d'attente entre deux téléportations et prix des warps.
 */
public final class Teleporter {

    public static final String BYPASS_DELAY = "koriawarps.bypass.delay";
    public static final String BYPASS_COOLDOWN = "koriawarps.bypass.cooldown";
    public static final String BYPASS_PRICE = "koriawarps.bypass.price";

    /** Une téléportation demandée. */
    public static final class Request {
        final WarpTeleportEvent.Destination destination;
        final Warp warp;
        final StoredLocation target;
        /** Nom affiché de la destination (couleurs traduites). */
        final String name;
        /** Sans délai, attente ni prix (admin qui téléporte quelqu'un, connexion, vide). */
        final boolean forced;
        /** Le délai s'applique-t-il (spawn.delai-sur-spawn pour le spawn) ? */
        final boolean delayed;

        public Request(WarpTeleportEvent.Destination destination, Warp warp, StoredLocation target, String name,
                       boolean forced, boolean delayed) {
            this.destination = destination;
            this.warp = warp;
            this.target = target;
            this.name = name;
            this.forced = forced;
            this.delayed = delayed;
        }

        double price() {
            return warp == null ? 0 : warp.getPrice();
        }
    }

    private static final class Pending {
        final Request request;
        final Location start;
        BukkitTask task;

        Pending(Request request, Location start) {
            this.request = request;
            this.start = start;
        }
    }

    private final KoriaWarps plugin;
    private final Map<UUID, Pending> pending = new HashMap<UUID, Pending>();
    private final Map<UUID, Long> lastTeleport = new HashMap<UUID, Long>();

    public Teleporter(KoriaWarps plugin) {
        this.plugin = plugin;
    }

    /**
     * Téléporte au warp (la permission du warp doit déjà avoir été vérifiée).
     *
     * @param forced sans délai, attente ni prix
     */
    public boolean toWarp(Player player, Warp warp, boolean forced) {
        return start(player, new Request(WarpTeleportEvent.Destination.WARP, warp, warp.getLocation(),
                Text.color(warp.getName()), forced, true));
    }

    /**
     * Téléporte au spawn qui correspond au monde du joueur.
     *
     * @return false si aucun spawn n'est défini (message envoyé) ou si c'est refusé
     */
    public boolean toSpawn(Player player, boolean forced) {
        StoredLocation spawn = plugin.spawns().spawnFor(player.getWorld());
        if (spawn == null) {
            plugin.msg().send(player, "spawn.not-set");
            return false;
        }
        return start(player, new Request(WarpTeleportEvent.Destination.SPAWN, null, spawn,
                plugin.msg().text("spawn.name"), forced, plugin.settings().spawnUsesDelay));
    }

    /**
     * Lance la téléportation (vérifie l'attente et le prix, puis démarre le délai).
     *
     * @return false si elle a été refusée tout de suite (message déjà envoyé)
     */
    public boolean start(final Player player, final Request request) {
        if (!request.forced) {
            long wait = cooldownLeft(player);
            if (wait > 0) {
                refuse(player, "teleport.cooldown", "temps", plugin.msg().duration(wait));
                return false;
            }
            if (!canPay(player, request)) {
                return false;
            }
        }
        cancel(player, null);
        int delay = plugin.settings().delaySeconds;
        if (request.forced || !request.delayed || delay <= 0 || player.hasPermission(BYPASS_DELAY)) {
            execute(player, request);
            return true;
        }
        final Pending p = new Pending(request, player.getLocation());
        pending.put(player.getUniqueId(), p);
        plugin.msg().send(player, "teleport.warmup", "destination", request.name, "secondes", delay);
        Sounds.play(player, plugin.settings().sound("debut-delai"));
        p.task = new BukkitRunnable() {
            private int left = plugin.settings().delaySeconds;

            @Override
            public void run() {
                if (!player.isOnline() || pending.get(player.getUniqueId()) != p) {
                    cancel();
                    return;
                }
                left--;
                if (left > 0) {
                    if (plugin.settings().countdown) {
                        plugin.msg().send(player, "teleport.countdown", "destination", request.name, "secondes", left);
                    }
                    return;
                }
                cancel();
                pending.remove(player.getUniqueId());
                execute(player, request);
            }
        }.runTaskTimer(plugin, 20L, 20L);
        return true;
    }

    /** Le joueur a-t-il une téléportation en attente ? */
    public boolean isPending(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    /** Position du joueur au début du délai, ou null. */
    public Location pendingStart(Player player) {
        Pending p = pending.get(player.getUniqueId());
        return p == null ? null : p.start;
    }

    /**
     * Annule la téléportation en attente.
     *
     * @param messageKey message envoyé au joueur (null = aucun)
     */
    public void cancel(Player player, String messageKey) {
        Pending p = pending.remove(player.getUniqueId());
        if (p == null) {
            return;
        }
        if (p.task != null) {
            p.task.cancel();
        }
        if (messageKey != null) {
            refuse(player, messageKey, "destination", p.request.name);
        }
    }

    /** Oublie le joueur (déconnexion). */
    public void forget(Player player) {
        cancel(player, null);
        lastTeleport.remove(player.getUniqueId());
    }

    public void cancelAll() {
        for (Pending p : pending.values()) {
            if (p.task != null) {
                p.task.cancel();
            }
        }
        pending.clear();
    }

    /** Temps restant avant la prochaine téléportation, en millisecondes (0 = possible). */
    public long cooldownLeft(Player player) {
        int seconds = plugin.settings().cooldownSeconds;
        Long last = lastTeleport.get(player.getUniqueId());
        if (seconds <= 0 || last == null || player.hasPermission(BYPASS_COOLDOWN)) {
            return 0;
        }
        return Math.max(0, last + seconds * 1000L - System.currentTimeMillis());
    }

    // ============================================================ Exécution

    private void execute(Player player, Request request) {
        Location to = request.target.toLocation();
        if (to == null) {
            refuse(player, "teleport.world-not-loaded", "destination", request.name, "monde", request.target.world);
            return;
        }
        double price = request.forced ? 0 : request.price();
        if (price > 0 && player.hasPermission(BYPASS_PRICE)) {
            price = 0;
        }
        if (price > 0 && !canPay(player, request)) {
            return;
        }
        WarpTeleportEvent event = new WarpTeleportEvent(player, request.destination, request.warp, to);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        player.setFallDistance(0);
        if (!player.teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            refuse(player, "teleport.failed", "destination", request.name);
            return;
        }
        player.setFallDistance(0);
        if (!request.forced) {
            lastTeleport.put(player.getUniqueId(), System.currentTimeMillis());
        }
        VaultHook economy = price > 0 ? plugin.economy() : null;
        if (economy != null && economy.withdraw(player, price)) {
            plugin.msg().send(player, "teleport.paid", "destination", request.name, "prix", Text.number(price, 2));
        }
        plugin.msg().send(player, request.destination == WarpTeleportEvent.Destination.SPAWN ? "spawn.teleported"
                : "warp.teleported", "destination", request.name);
        Sounds.play(player, plugin.settings().sound("teleportation"));
    }

    /** Le joueur peut-il payer le warp ? (message envoyé si non) */
    private boolean canPay(Player player, Request request) {
        double price = request.price();
        if (price <= 0 || request.forced || player.hasPermission(BYPASS_PRICE)) {
            return true;
        }
        VaultHook economy = plugin.economy();
        if (economy == null) {
            plugin.getLogger().warning("Prix du warp " + request.warp.getId() + " ignoré : aucune économie Vault disponible.");
            return true;
        }
        if (!economy.has(player, price)) {
            refuse(player, "teleport.too-poor", "destination", request.name, "prix", Text.number(price, 2));
            return false;
        }
        return true;
    }

    private void refuse(Player player, String key, Object... kv) {
        plugin.msg().send(player, key, kv);
        Sounds.play(player, plugin.settings().sound("refus"));
    }
}
