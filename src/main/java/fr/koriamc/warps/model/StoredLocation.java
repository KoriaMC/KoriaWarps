package fr.koriamc.warps.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Position enregistrée dans un fichier : monde (par son nom), coordonnées et orientation.
 * Le monde peut être déchargé : {@link #toLocation()} renvoie alors null.
 */
public final class StoredLocation {

    public final String world;
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;

    public StoredLocation(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static StoredLocation of(Location location) {
        return new StoredLocation(location.getWorld().getName(), round(location.getX()), round(location.getY()),
                round(location.getZ()), (float) round(location.getYaw()), (float) round(location.getPitch()));
    }

    /** @return la position, ou null si la section est incomplète */
    public static StoredLocation read(ConfigurationSection s) {
        if (s == null || !s.isString("monde") || !s.contains("x") || !s.contains("y") || !s.contains("z")) {
            return null;
        }
        return new StoredLocation(s.getString("monde"), s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    /** Monde chargé (nom exact, sinon sans tenir compte de la casse), ou null. */
    public World loadedWorld() {
        World w = Bukkit.getWorld(world);
        if (w != null) {
            return w;
        }
        for (World candidate : Bukkit.getWorlds()) {
            if (candidate.getName().equalsIgnoreCase(world)) {
                return candidate;
            }
        }
        return null;
    }

    /** @return la position, ou null si le monde n'est pas chargé */
    public Location toLocation() {
        World w = loadedWorld();
        return w == null ? null : new Location(w, x, y, z, yaw, pitch);
    }

    /** Champs à écrire dans le fichier (monde, x, y, z, yaw, pitch). */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        writeTo(map);
        return map;
    }

    public void writeTo(Map<String, Object> map) {
        map.put("monde", world);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", (double) yaw);
        map.put("pitch", (double) pitch);
    }

    /** "monde (x, y, z)" pour les messages. */
    public String describe() {
        return String.format(Locale.ROOT, "%s (%.1f, %.1f, %.1f)", world, x, y, z);
    }

    private static double round(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
