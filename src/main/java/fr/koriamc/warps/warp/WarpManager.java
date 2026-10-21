package fr.koriamc.warps.warp;

import fr.koriamc.warps.KoriaWarps;
import fr.koriamc.warps.config.JsonFile;
import fr.koriamc.warps.model.StoredLocation;
import fr.koriamc.warps.model.Warp;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Accès aux warps (permissions, liste visible) et modifications de warps.json
 * (/setwarp, /delwarp, /editwarp). Chaque modification est enregistrée puis relue.
 */
public final class WarpManager {

    /** Donne l'accès à tous les warps, quelle que soit leur permission. */
    public static final String ALL_WARPS = "koriawarps.warp.*";

    private final KoriaWarps plugin;

    public WarpManager(KoriaWarps plugin) {
        this.plugin = plugin;
    }

    // ================================================================ Accès

    /** Permission à avoir pour ce warp (vide = aucune en plus de celle de /warp). */
    public String requiredPermission(Warp warp) {
        if (!warp.getPermission().isEmpty()) {
            return warp.getPermission();
        }
        return plugin.settings().permissionPerWarp ? "koriawarps.warp." + warp.getId() : "";
    }

    public boolean canUse(Player player, Warp warp) {
        String node = requiredPermission(warp);
        return node.isEmpty() || player.hasPermission(node) || player.hasPermission(ALL_WARPS);
    }

    /**
     * Warps listés pour ce joueur (menu, autocomplétion, /warps) : les warps cachés n'y sont
     * jamais, les warps inaccessibles seulement si warps.afficher-verrouilles est activé.
     */
    public List<Warp> listed(Player player, boolean includeLocked) {
        List<Warp> list = new ArrayList<Warp>();
        for (Warp warp : plugin.catalog().all()) {
            if (warp.isHidden()) {
                continue;
            }
            if (player == null || canUse(player, warp) || includeLocked) {
                list.add(warp);
            }
        }
        return list;
    }

    // ======================================================== Modifications

    /**
     * Crée le warp, ou le déplace s'il existe (ses autres réglages sont gardés).
     *
     * @param permission null pour ne pas changer la permission
     * @return true si le warp vient d'être créé
     */
    public boolean set(String name, Location location, String permission, String author) throws IOException {
        JsonFile file = plugin.warpsFile();
        String key = findKey(name);
        boolean created = key == null;
        Map<String, Object> entry;
        if (created) {
            key = name;
            entry = new LinkedHashMap<String, Object>();
            entry.put("nom", "&b" + name);
            StoredLocation.of(location).writeTo(entry);
            entry.put("permission", permission == null ? "" : permission);
            entry.put("icone", "");
            entry.put("description", new ArrayList<Object>());
            entry.put("prix", 0);
            entry.put("cache", false);
            entry.put("cree-par", author);
            entry.put("cree-le", System.currentTimeMillis());
        } else {
            entry = entry(key);
            StoredLocation.of(location).writeTo(entry);
            if (permission != null) {
                entry.put("permission", permission);
            }
        }
        file.set(Collections.singletonList(key), entry);
        save();
        return created;
    }

    /** @return false si le warp n'existe pas */
    public boolean delete(String name) throws IOException {
        String key = findKey(name);
        if (key == null) {
            return false;
        }
        plugin.warpsFile().remove(Collections.singletonList(key));
        save();
        return true;
    }

    /**
     * Change une option du warp (nom, permission, icone, description, prix, cache).
     *
     * @param value null pour retirer l'option
     * @return false si le warp n'existe pas
     */
    public boolean edit(String name, String option, Object value) throws IOException {
        String key = findKey(name);
        if (key == null) {
            return false;
        }
        Map<String, Object> entry = entry(key);
        if (value == null) {
            entry.remove(option);
        } else {
            entry.put(option, value);
        }
        plugin.warpsFile().set(Collections.singletonList(key), entry);
        save();
        return true;
    }

    /**
     * Renomme le warp en gardant sa place dans le fichier.
     *
     * @return false si le warp n'existe pas ou si le nouveau nom est déjà pris
     */
    public boolean rename(String name, String newName) throws IOException {
        String key = findKey(name);
        String taken = findKey(newName);
        if (key == null || (taken != null && !taken.equals(key))) {
            return false;
        }
        Map<String, Object> tree = plugin.warpsFile().getTree();
        Map<String, Object> copy = new LinkedHashMap<String, Object>(tree);
        tree.clear();
        for (Map.Entry<String, Object> e : copy.entrySet()) {
            tree.put(e.getKey().equals(key) ? newName : e.getKey(), e.getValue());
        }
        save();
        return true;
    }

    /** Clé exacte du warp dans le fichier (sans tenir compte de la casse), ou null. */
    private String findKey(String name) {
        for (String key : plugin.warpsFile().getTree().keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return key;
            }
        }
        return null;
    }

    private Map<String, Object> entry(String key) {
        Object current = plugin.warpsFile().getTree().get(key);
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        if (current instanceof Map) {
            entry.putAll(JsonFile.castMap(current));
        }
        return entry;
    }

    private void save() throws IOException {
        plugin.warpsFile().save();
        plugin.reloadWarps();
    }

    /** Nom de warp normalisé pour les messages et les permissions. */
    public static String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
