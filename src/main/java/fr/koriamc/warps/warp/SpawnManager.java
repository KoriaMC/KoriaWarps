package fr.koriamc.warps.warp;

import fr.koriamc.warps.KoriaWarps;
import fr.koriamc.warps.config.JsonFile;
import fr.koriamc.warps.model.StoredLocation;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Spawn du serveur (spawns.json). Le spawn global peut se trouver dans n'importe quel monde :
 * /spawn y ramène depuis tous les mondes. Avec spawn.par-monde, chaque monde peut avoir
 * son propre spawn, le spawn global servant pour les mondes qui n'en ont pas.
 */
public final class SpawnManager {

    private final KoriaWarps plugin;
    private StoredLocation global;
    private final Map<String, StoredLocation> worlds = new LinkedHashMap<String, StoredLocation>();

    public SpawnManager(KoriaWarps plugin) {
        this.plugin = plugin;
    }

    /** Relit spawns.json. */
    public void load(ConfigurationSection file) {
        global = StoredLocation.read(file.getConfigurationSection("global"));
        worlds.clear();
        ConfigurationSection section = file.getConfigurationSection("mondes");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                StoredLocation location = StoredLocation.read(section.getConfigurationSection(name));
                if (location != null) {
                    worlds.put(name.toLowerCase(Locale.ROOT), location);
                } else {
                    plugin.getLogger().warning("spawns.json : mondes." + name + " est incomplet (monde, x, y, z).");
                }
            }
        }
    }

    public StoredLocation getGlobal() {
        return global;
    }

    /** Spawn propre à ce monde, ou null. */
    public StoredLocation getWorld(String world) {
        return world == null ? null : worlds.get(world.toLowerCase(Locale.ROOT));
    }

    public Map<String, StoredLocation> getWorlds() {
        return Collections.unmodifiableMap(worlds);
    }

    /**
     * Spawn pour un joueur qui se trouve dans ce monde : celui du monde (si spawn.par-monde),
     * sinon le spawn global. Null si aucun n'est défini.
     */
    public StoredLocation spawnFor(World current) {
        if (plugin.settings().spawnPerWorld && current != null) {
            StoredLocation own = getWorld(current.getName());
            if (own != null) {
                return own;
            }
        }
        return global;
    }

    /** Position du spawn, ou null si aucun spawn n'est défini ou si son monde n'est pas chargé. */
    public Location locationFor(World current) {
        StoredLocation spawn = spawnFor(current);
        return spawn == null ? null : spawn.toLocation();
    }

    // ======================================================== Modifications

    public void setGlobal(Location location) throws IOException {
        plugin.spawnsFile().set(Collections.singletonList("global"), StoredLocation.of(location).toMap());
        applyWorldSpawn(location);
        save();
    }

    public void setWorld(Location location) throws IOException {
        String world = location.getWorld().getName();
        removeKey(world);
        plugin.spawnsFile().set(Arrays.asList("mondes", world), StoredLocation.of(location).toMap());
        applyWorldSpawn(location);
        save();
    }

    /** @return false s'il n'y avait pas de spawn global */
    public boolean removeGlobal() throws IOException {
        if (!plugin.spawnsFile().remove(Collections.singletonList("global"))) {
            return false;
        }
        save();
        return true;
    }

    /** @return false si ce monde n'avait pas de spawn */
    public boolean removeWorld(String world) throws IOException {
        if (!removeKey(world)) {
            return false;
        }
        save();
        return true;
    }

    /** Retire mondes.&lt;monde&gt; quelle que soit sa casse. */
    private boolean removeKey(String world) {
        Object section = plugin.spawnsFile().getTree().get("mondes");
        if (!(section instanceof Map)) {
            return false;
        }
        Map<String, Object> map = JsonFile.castMap(section);
        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase(world)) {
                map.remove(key);
                return true;
            }
        }
        return false;
    }

    /** Point d'apparition vanilla du monde (boussole, joueurs sans plugin) aligné sur le spawn. */
    private void applyWorldSpawn(Location location) {
        if (plugin.settings().setWorldSpawn) {
            location.getWorld().setSpawnLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    private void save() throws IOException {
        plugin.spawnsFile().save();
        plugin.reloadSpawns();
    }
}
