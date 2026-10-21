package fr.koriamc.warps.gui;

import fr.koriamc.warps.KoriaWarps;
import fr.koriamc.warps.command.impl.AdminCommand;
import fr.koriamc.warps.config.JsonFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Menu custom des warps (layout KoriaMC) et éditeur de configuration (menus coffre des admins).
 */
public final class MenuManager implements Listener {

    /** Layout par défaut, copié dans koriamc/guis/ s'il n'y est pas. */
    private static final String LAYOUT_RESOURCE = "gui/warps.json";

    private final KoriaWarps plugin;
    private final ChatPrompt prompt;
    private final WarpMenu warpMenu;

    public MenuManager(KoriaWarps plugin) {
        this.plugin = plugin;
        this.prompt = new ChatPrompt(plugin);
        this.warpMenu = new WarpMenu(plugin);
    }

    /** Saisie de valeurs au clavier (éditeur de configuration). */
    public ChatPrompt prompt() {
        return prompt;
    }

    public WarpMenu warpMenu() {
        return warpMenu;
    }

    /** Section de menus.json (jamais nulle). */
    public ConfigurationSection section(String path) {
        ConfigurationSection s = plugin.menusConfig().getConfigurationSection(path);
        return s != null ? s : new MemoryConfiguration().createSection(path);
    }

    public void openWarps(Player player) {
        warpMenu.open(player);
    }

    /** Ouvre un menu d'après son identifiant : warps, config, config:&lt;fichier&gt;. */
    public void open(Player player, String id) {
        String raw = id == null ? "warps" : id.trim();
        int colon = raw.indexOf(':');
        String key = (colon < 0 ? raw : raw.substring(0, colon)).toLowerCase(Locale.ROOT);
        String arg = colon < 0 ? null : raw.substring(colon + 1).trim();
        if (key.equals("warps") || key.equals("principal")) {
            openWarps(player);
        } else if (key.equals("config")) {
            if (!player.hasPermission(AdminCommand.PERM_CONFIG)) {
                plugin.msg().send(player, "general.no-permission");
                return;
            }
            if (!openConfig(player, arg)) {
                plugin.msg().send(player, "config-editor.unknown-file", "file", arg);
            }
        } else {
            plugin.getLogger().warning("Menu inconnu : " + id);
        }
    }

    /**
     * Éditeur de configuration : liste des fichiers, ou directement un fichier.
     *
     * @return false si le fichier demandé n'existe pas
     */
    public boolean openConfig(Player player, String fileName) {
        if (fileName == null) {
            new ConfigFilesMenu(plugin, player).open();
            return true;
        }
        for (JsonFile file : plugin.configFiles()) {
            String name = file.getName();
            if (name.equalsIgnoreCase(fileName) || name.equalsIgnoreCase(fileName + ".json")) {
                new ConfigSectionMenu(plugin, player, file, Collections.<String>emptyList(), 0).open();
                return true;
            }
        }
        return false;
    }

    /**
     * Copie le layout du menu dans koriamc/guis/ s'il n'existe pas encore (il n'est jamais
     * écrasé : les modifications faites dans l'éditeur de GUI sont gardées).
     *
     * @return true si le fichier vient d'être créé
     */
    public boolean installLayout() {
        if (!plugin.settings().installLayout || !plugin.gui().isAvailable()) {
            return false;
        }
        File target = new File(new File("koriamc", "guis"), plugin.settings().layout + ".json");
        if (target.exists()) {
            return false;
        }
        InputStream in = plugin.getResource(LAYOUT_RESOURCE);
        if (in == null) {
            return false;
        }
        try {
            File folder = target.getParentFile();
            if (!folder.isDirectory() && !folder.mkdirs()) {
                throw new IOException("dossier " + folder.getPath() + " impossible à créer");
            }
            Files.copy(in, target.toPath());
            plugin.getLogger().info("Menu custom installé : " + target.getPath());
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible d'installer le menu " + target.getPath(), e);
            return false;
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
                // rien à faire
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof Menu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize() && event.getWhoClicked() instanceof Player) {
            ((Menu) top.getHolder()).handleClick(event);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
