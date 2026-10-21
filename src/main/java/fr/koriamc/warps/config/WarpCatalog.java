package fr.koriamc.warps.config;

import fr.koriamc.warps.model.StoredLocation;
import fr.koriamc.warps.model.Warp;
import fr.koriamc.warps.util.Items;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Warps de warps.json, dans l'ordre du fichier.
 */
public final class WarpCatalog {

    /** Nom de warp accepté : lettres, chiffres, - et _ (32 caractères au plus). */
    public static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final Map<String, Warp> warps = new LinkedHashMap<String, Warp>();
    private final List<String> problems = new ArrayList<String>();

    public void load(ConfigurationSection file, String defaultIcon) {
        warps.clear();
        problems.clear();
        for (String key : file.getKeys(false)) {
            ConfigurationSection s = file.getConfigurationSection(key);
            String where = "warps.json : " + key;
            if (s == null) {
                problems.add(where + " doit être un bloc { ... }");
                continue;
            }
            if (!VALID_NAME.matcher(key).matches()) {
                problems.add(where + " : nom invalide (lettres, chiffres, - et _ uniquement).");
                continue;
            }
            StoredLocation location = StoredLocation.read(s);
            if (location == null) {
                problems.add(where + " : position incomplète (monde, x, y, z).");
                continue;
            }
            String id = key.toLowerCase(Locale.ROOT);
            if (warps.containsKey(id)) {
                problems.add(where + " existe deux fois (majuscules différentes), ignoré.");
                continue;
            }
            String icon = s.getString("icone", "");
            warps.put(id, new Warp(id, s.getString("nom", key), location, s.getString("permission", ""),
                    icon.trim().isEmpty() ? defaultIcon : icon.trim(), Items.stringList(s, "description"),
                    s.getDouble("prix", 0), s.getBoolean("cache", false), s.getString("cree-par", ""),
                    s.getLong("cree-le", 0)));
        }
    }

    public Warp get(String id) {
        return id == null ? null : warps.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Warp> all() {
        return warps.values();
    }

    public List<String> ids() {
        return new ArrayList<String>(warps.keySet());
    }

    public int count() {
        return warps.size();
    }

    public List<String> getProblems() {
        return problems;
    }
}
