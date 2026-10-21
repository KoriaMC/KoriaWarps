package fr.koriamc.warps.gui;

import fr.koriamc.warps.KoriaWarps;
import fr.koriamc.warps.hook.GuiHook;
import fr.koriamc.warps.model.Warp;
import fr.koriamc.warps.util.Text;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu custom des warps (layout KoriaMC, koriamc/guis/warps.json).
 *
 * Le plugin n'envoie que des données : variables %var_warps_...% et la liste list:warps_liste
 * (id, nom, icone, statut, infobulle, resume, monde, prix). Un clic sur un warp lance
 * "command: warp {item.id}". Sans le client KoriaMC, la liste est envoyée dans le chat
 * (clic sur un warp pour s'y téléporter).
 */
public final class WarpMenu {

    private final KoriaWarps plugin;

    public WarpMenu(KoriaWarps plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiHook gui = plugin.gui();
        if (!gui.canDisplay(player)) {
            chat(player);
            return;
        }
        push(player);
        String layout = plugin.settings().layout;
        if (!gui.open(player, layout)) {
            plugin.getLogger().warning("Le menu " + layout + " n'a pas pu s'ouvrir : le fichier koriamc/guis/" + layout
                    + ".json existe-t-il ?");
            chat(player);
        }
    }

    private void push(Player player) {
        GuiHook gui = plugin.gui();
        List<Warp> warps = plugin.warps().listed(player, plugin.settings().showLocked);
        int usable = 0;
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        for (Warp warp : warps) {
            boolean open = plugin.warps().canUse(player, warp);
            if (open) {
                usable++;
            }
            Map<String, String> item = new HashMap<String, String>();
            item.put("id", warp.getId());
            item.put("nom", Text.color(warp.getName()));
            item.put("icone", warp.getIcon());
            item.put("statut", open ? "libre" : "verrouille");
            item.put("monde", warp.getLocation().world);
            item.put("prix", price(warp));
            item.put("resume", warp.getDescription().isEmpty() ? "" : Text.color(warp.getDescription().get(0)));
            item.put("infobulle", tooltip(warp, open));
            items.add(item);
        }
        gui.setList(player, "warps_liste", items);
        gui.setVariable(player, "warps_total", String.valueOf(warps.size()));
        gui.setVariable(player, "warps_accessibles", String.valueOf(usable));
        gui.setVariable(player, "warps_commande", plugin.commands().label("warp"));
        gui.setVariable(player, "warps_spawn", String.valueOf(plugin.spawns().spawnFor(player.getWorld()) != null));
        gui.setVariable(player, "warps_commande_spawn", plugin.commands().label("spawn"));
    }

    private String price(Warp warp) {
        return warp.getPrice() > 0 ? plugin.msg().text("menu.price", "prix", Text.number(warp.getPrice(), 2))
                : plugin.msg().text("values.free");
    }

    /** Infobulle d'un warp : nom, description, prix et état. */
    private String tooltip(Warp warp, boolean open) {
        StringBuilder sb = new StringBuilder(Text.color(warp.getName()));
        for (String line : warp.getDescription()) {
            sb.append('\n').append(Text.color("&7" + line));
        }
        sb.append("\n\n").append(plugin.msg().text("menu.tooltip-world", "monde", warp.getLocation().world));
        sb.append('\n').append(plugin.msg().text("menu.tooltip-price", "prix", price(warp)));
        sb.append("\n\n").append(plugin.msg().text(open ? "menu.tooltip-open" : "menu.tooltip-locked"));
        return sb.toString();
    }

    /** Liste des warps dans le chat : cliquable pour un joueur, texte simple pour la console. */
    public void chat(CommandSender to) {
        Player player = to instanceof Player ? (Player) to : null;
        List<Warp> warps = plugin.warps().listed(player, player != null && plugin.settings().showLocked);
        if (warps.isEmpty()) {
            plugin.msg().send(to, "menu.chat-empty");
            return;
        }
        plugin.msg().send(to, "menu.chat-header", "count", warps.size());
        String command = plugin.commands().label("warp");
        for (Warp warp : warps) {
            boolean open = player == null || plugin.warps().canUse(player, warp);
            String line = plugin.msg().get(open ? "menu.chat-entry" : "menu.chat-entry-locked",
                    "warp", warp.getId(), "destination", Text.color(warp.getName()), "prix", price(warp),
                    "monde", warp.getLocation().world);
            if (player == null) {
                to.sendMessage(line);
                continue;
            }
            BaseComponent[] parts = TextComponent.fromLegacyText(line);
            HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(tooltip(warp, open)));
            ClickEvent click = open ? new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + command + " " + warp.getId()) : null;
            for (BaseComponent part : parts) {
                part.setHoverEvent(hover);
                if (click != null) {
                    part.setClickEvent(click);
                }
            }
            player.spigot().sendMessage(parts);
        }
        plugin.msg().send(to, "menu.chat-footer");
    }
}
