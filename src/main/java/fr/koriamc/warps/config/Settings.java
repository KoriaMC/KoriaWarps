package fr.koriamc.warps.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Réglages typés de config.json, relus à chaque rechargement.
 */
public final class Settings {

    public boolean debug;

    // Warps
    /** Chaque warp demande koriawarps.warp.&lt;nom&gt; quand il n'a pas de permission propre. */
    public boolean permissionPerWarp;
    /** Les warps inaccessibles apparaissent verrouillés dans le menu (sinon ils sont masqués). */
    public boolean showLocked;
    public String defaultIcon;

    // Téléportation
    public int delaySeconds;
    public boolean cancelOnMove;
    public boolean cancelOnDamage;
    public int cooldownSeconds;
    public boolean countdown;

    // Spawn
    /** Chaque monde peut avoir son propre spawn (sinon un seul spawn pour tout le serveur). */
    public boolean spawnPerWorld;
    public boolean setWorldSpawn;
    public boolean spawnFirstJoin;
    public boolean spawnEveryJoin;
    public boolean spawnOnRespawn;
    public boolean respawnIgnoreBed;
    public boolean voidEnabled;
    public double voidHeight;
    public List<String> voidWorlds = new ArrayList<String>();
    public boolean spawnUsesDelay;

    // Menu
    public String layout;
    public boolean installLayout;

    // Titres
    public int titleFadeIn;
    public int titleStay;
    public int titleFadeOut;

    private final Map<String, String> sounds = new HashMap<String, String>();

    public void load(ConfigurationSection c, Logger logger) {
        debug = c.getBoolean("debug");
        permissionPerWarp = c.getBoolean("warps.permission-par-warp", false);
        showLocked = c.getBoolean("warps.afficher-verrouilles", true);
        defaultIcon = str(c, "warps.icone-par-defaut", "minecraft:ender_pearl").trim();

        delaySeconds = Math.max(0, c.getInt("teleportation.delai-secondes", 3));
        cancelOnMove = c.getBoolean("teleportation.annuler-si-mouvement", true);
        cancelOnDamage = c.getBoolean("teleportation.annuler-si-degats", true);
        cooldownSeconds = Math.max(0, c.getInt("teleportation.cooldown-secondes", 0));
        countdown = c.getBoolean("teleportation.compte-a-rebours", true);

        spawnPerWorld = c.getBoolean("spawn.par-monde", false);
        setWorldSpawn = c.getBoolean("spawn.definir-spawn-du-monde", true);
        spawnFirstJoin = c.getBoolean("spawn.connexion.premiere-connexion", true);
        spawnEveryJoin = c.getBoolean("spawn.connexion.chaque-connexion", false);
        spawnOnRespawn = c.getBoolean("spawn.reapparition.actif", true);
        respawnIgnoreBed = c.getBoolean("spawn.reapparition.ignorer-lit", false);
        voidEnabled = c.getBoolean("spawn.vide.actif", false);
        voidHeight = c.getDouble("spawn.vide.hauteur", 0);
        voidWorlds = new ArrayList<String>();
        for (String world : c.getStringList("spawn.vide.mondes")) {
            voidWorlds.add(world.toLowerCase(Locale.ROOT));
        }
        spawnUsesDelay = c.getBoolean("spawn.delai-sur-spawn", true);

        layout = str(c, "menu.layout", "warps").trim().toLowerCase(Locale.ROOT);
        installLayout = c.getBoolean("menu.installer-layout", true);

        titleFadeIn = c.getInt("titres.apparition", 5);
        titleStay = c.getInt("titres.duree", 40);
        titleFadeOut = c.getInt("titres.disparition", 10);
        sounds.clear();
        ConfigurationSection soundSection = c.getConfigurationSection("sons");
        if (soundSection != null) {
            for (String key : soundSection.getKeys(false)) {
                sounds.put(key, soundSection.getString(key, ""));
            }
        }
        if (layout.isEmpty()) {
            logger.warning("config.json : menu.layout est vide, \"warps\" utilisé.");
            layout = "warps";
        }
    }

    /** Le monde est-il surveillé pour la chute dans le vide ? (liste vide = tous) */
    public boolean watchesVoid(String world) {
        return voidEnabled && (voidWorlds.isEmpty() || voidWorlds.contains(world.toLowerCase(Locale.ROOT)));
    }

    /** Son de la section sons (chaîne vide si absent). */
    public String sound(String key) {
        String value = sounds.get(key);
        return value == null ? "" : value;
    }

    private static String str(ConfigurationSection c, String path, String def) {
        String value = c.getString(path);
        return value == null ? def : value;
    }
}
