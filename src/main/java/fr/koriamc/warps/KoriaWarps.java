package fr.koriamc.warps;

import fr.koriamc.warps.command.CommandManager;
import fr.koriamc.warps.config.ConfigDocs;
import fr.koriamc.warps.config.JsonFile;
import fr.koriamc.warps.config.Messages;
import fr.koriamc.warps.config.Settings;
import fr.koriamc.warps.config.WarpCatalog;
import fr.koriamc.warps.gui.Menu;
import fr.koriamc.warps.gui.MenuManager;
import fr.koriamc.warps.hook.GuiHook;
import fr.koriamc.warps.hook.VaultHook;
import fr.koriamc.warps.listener.SpawnListener;
import fr.koriamc.warps.listener.TeleportListener;
import fr.koriamc.warps.teleport.Teleporter;
import fr.koriamc.warps.warp.SpawnManager;
import fr.koriamc.warps.warp.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * KoriaWarps : warps (avec permission par warp, prix, icône, menu custom) et spawn
 * multi-mondes pour KoriaMC (Spigot 1.8.8). Le spawn peut se trouver dans n'importe quel
 * monde et /spawn y ramène depuis tous les mondes ; chaque monde peut aussi avoir le sien.
 */
public final class KoriaWarps extends JavaPlugin {

    private static KoriaWarps instance;

    // Fichiers
    private JsonFile configFile;
    private JsonFile warpsFile;
    private JsonFile spawnsFile;
    private JsonFile messagesFile;
    private JsonFile menusFile;
    private Configuration config = new MemoryConfiguration();
    private Configuration messagesConfig = new MemoryConfiguration();
    private Configuration menusConfig = new MemoryConfiguration();

    // Réglages
    private final Settings settings = new Settings();
    private final WarpCatalog catalog = new WarpCatalog();
    private final Messages messages = new Messages();
    private final ConfigDocs configDocs = new ConfigDocs();

    // Services
    private WarpManager warps;
    private SpawnManager spawns;
    private Teleporter teleporter;
    private MenuManager menus;
    private CommandManager commands;
    private GuiHook gui;
    private VaultHook vault;
    private boolean vaultChecked;

    public static KoriaWarps get() {
        return instance;
    }

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        instance = this;

        configFile = new JsonFile(this, "config.json");
        warpsFile = new JsonFile(this, "warps.json", "*", "*.description");
        spawnsFile = new JsonFile(this, "spawns.json", "global", "mondes");
        messagesFile = new JsonFile(this, "messages.json");
        menusFile = new JsonFile(this, "menus.json", "editeur.fichiers");

        gui = new GuiHook(getLogger());
        warps = new WarpManager(this);
        spawns = new SpawnManager(this);
        teleporter = new Teleporter(this);
        menus = new MenuManager(this);
        commands = new CommandManager(this);

        for (String problem : loadConfigs()) {
            getLogger().warning(problem);
        }
        configDocs.load(this);
        configDocs.writeReadme(this, configFiles());
        commands.load(config, messagesConfig);

        if (menus.installLayout()) {
            gui.reloadLayouts();
        }

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new TeleportListener(this), this);
        pm.registerEvents(new SpawnListener(this), this);
        pm.registerEvents(menus, this);
        pm.registerEvents(menus.prompt(), this);
        commands.registerBukkitCommands();

        if (spawns.getGlobal() == null) {
            getLogger().info("Aucun spawn défini : faites /" + commands.label("setspawn") + " en jeu.");
        }
        getLogger().info("KoriaWarps activé en " + (System.currentTimeMillis() - start) + " ms ("
                + catalog.count() + " warp(s)).");
    }

    @Override
    public void onDisable() {
        if (teleporter != null) {
            teleporter.cancelAll();
        }
        if (commands != null) {
            commands.unregisterBukkitCommands();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                player.closeInventory();
            }
        }
        instance = null;
    }

    /** @return les problèmes à signaler (erreurs JSON, warps invalides) */
    private List<String> loadConfigs() {
        List<String> problems = new ArrayList<String>();
        for (JsonFile file : configFiles()) {
            String error = file.load();
            if (error != null) {
                problems.add(file.getName() + " : " + error);
            }
        }
        config = configFile.toConfiguration();
        messagesConfig = messagesFile.toConfiguration();
        menusConfig = menusFile.toConfiguration();

        settings.load(config, getLogger());
        messages.load(messagesConfig, settings);
        reloadWarps();
        problems.addAll(catalog.getProblems());
        reloadSpawns();
        return problems;
    }

    /** Relit les warps depuis l'arbre de warps.json (sans les valeurs du jar). */
    public void reloadWarps() {
        Configuration warpsConfig = warpsFile.toConfiguration();
        warpsConfig.setDefaults(new MemoryConfiguration());
        catalog.load(warpsConfig, settings.defaultIcon);
    }

    /** Relit les spawns depuis l'arbre de spawns.json. */
    public void reloadSpawns() {
        Configuration spawnsConfig = spawnsFile.toConfiguration();
        spawnsConfig.setDefaults(new MemoryConfiguration());
        spawns.load(spawnsConfig);
    }

    /**
     * Recharge toute la configuration (/koriawarps reload, éditeur) et réenregistre les commandes.
     *
     * @return les problèmes détectés, à afficher à l'auteur de la commande
     */
    public List<String> reload() {
        List<String> problems = loadConfigs();
        if (commands != null) {
            commands.load(config, messagesConfig);
            commands.registerBukkitCommands();
        }
        if (menus != null && menus.installLayout()) {
            gui.reloadLayouts();
        }
        return problems;
    }

    /** Économie Vault pour le prix des warps, ou null si Vault ou une économie manque. */
    public VaultHook economy() {
        if (vault == null && !vaultChecked && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultChecked = true;
            vault = VaultHook.create();
        }
        return vault;
    }

    // ============================================================ Accesseurs

    public Settings settings() {
        return settings;
    }

    public WarpCatalog catalog() {
        return catalog;
    }

    public Messages msg() {
        return messages;
    }

    /** Explication des options (éditeur /koriawarps config et LISEZMOI.txt). */
    public ConfigDocs configDocs() {
        return configDocs;
    }

    public Configuration config() {
        return config;
    }

    public Configuration menusConfig() {
        return menusConfig;
    }

    /** Fichiers de configuration, dans l'ordre de l'éditeur. */
    public List<JsonFile> configFiles() {
        return Arrays.asList(configFile, warpsFile, spawnsFile, messagesFile, menusFile);
    }

    /** Warps, modifié par /setwarp, /delwarp, /editwarp et l'éditeur. */
    public JsonFile warpsFile() {
        return warpsFile;
    }

    /** Spawns, modifié par /setspawn, /delspawn et l'éditeur. */
    public JsonFile spawnsFile() {
        return spawnsFile;
    }

    public WarpManager warps() {
        return warps;
    }

    public SpawnManager spawns() {
        return spawns;
    }

    public Teleporter teleporter() {
        return teleporter;
    }

    public MenuManager menus() {
        return menus;
    }

    public CommandManager commands() {
        return commands;
    }

    /** Pont vers les GUI custom KoriaMC. */
    public GuiHook gui() {
        return gui;
    }
}
