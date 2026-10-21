package fr.koriamc.warps.api;

import fr.koriamc.warps.KoriaWarps;
import fr.koriamc.warps.model.StoredLocation;
import fr.koriamc.warps.model.Warp;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Accès aux warps et au spawn pour les autres plugins. Évènement : fr.koriamc.warps.api.event.
 */
public final class KoriaWarpsAPI {

    private KoriaWarpsAPI() {
    }

    private static KoriaWarps plugin() {
        KoriaWarps plugin = KoriaWarps.get();
        if (plugin == null) {
            throw new IllegalStateException("KoriaWarps n'est pas activé");
        }
        return plugin;
    }

    /** Warp par son nom, ou null. */
    public static Warp getWarp(String name) {
        return plugin().catalog().get(name);
    }

    public static List<Warp> getWarps() {
        return new ArrayList<Warp>(plugin().catalog().all());
    }

    /** Le joueur a-t-il accès à ce warp (permission propre au warp) ? */
    public static boolean canUse(Player player, Warp warp) {
        return plugin().warps().canUse(player, warp);
    }

    /**
     * Téléporte au warp.
     *
     * @param instant sans délai, attente ni prix (et sans vérifier la permission du warp)
     */
    public static boolean teleport(Player player, Warp warp, boolean instant) {
        return plugin().teleporter().toWarp(player, warp, instant);
    }

    /** Spawn qui s'applique dans ce monde (celui du monde ou le spawn global), ou null. */
    public static Location getSpawn(World world) {
        return plugin().spawns().locationFor(world);
    }

    /** Spawn global du serveur, ou null. */
    public static Location getGlobalSpawn() {
        StoredLocation global = plugin().spawns().getGlobal();
        return global == null ? null : global.toLocation();
    }

    /** Téléporte au spawn (instant : sans délai ni attente). */
    public static boolean teleportToSpawn(Player player, boolean instant) {
        return plugin().teleporter().toSpawn(player, instant);
    }
}
