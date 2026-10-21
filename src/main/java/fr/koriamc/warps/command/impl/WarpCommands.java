package fr.koriamc.warps.command.impl;

import fr.koriamc.warps.KoriaWarps;
import fr.koriamc.warps.command.CommandContext;
import fr.koriamc.warps.command.RootCommand;
import fr.koriamc.warps.config.WarpCatalog;
import fr.koriamc.warps.util.ItemIds;
import fr.koriamc.warps.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/**
 * /warp, /warps, /setwarp, /delwarp, /editwarp.
 */
public final class WarpCommands {

    /** Téléporter quelqu'un d'autre : /warp &lt;warp&gt; &lt;joueur&gt;. */
    public static final String OTHERS = "koriawarps.command.warp.others";

    private WarpCommands() {
    }

    static List<String> onlineNames(String input) {
        List<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return Text.complete(input, names);
    }

    /** Joueur en ligne ou message d'erreur (null). */
    static Player target(CommandContext ctx, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            player = Bukkit.getPlayer(name);
        }
        if (player == null) {
            ctx.msg("general.unknown-player", "player", name);
        }
        return player;
    }

    /** Le warp demandé, ou message d'erreur (null). */
    static fr.koriamc.warps.model.Warp warp(CommandContext ctx, String name) {
        fr.koriamc.warps.model.Warp warp = ctx.plugin.catalog().get(name);
        if (warp == null) {
            ctx.msg("warp.unknown", "warp", name, "warps", ctx.plugin.commands().label("warps"));
        }
        return warp;
    }

    static void saveError(CommandContext ctx, String file, IOException e) {
        ctx.plugin.getLogger().log(Level.SEVERE, "Impossible d'enregistrer " + file, e);
        ctx.msg("general.save-error", "file", file, "error", e.getMessage());
    }

    /** Warps proposés à l'autocomplétion (ceux que l'expéditeur peut utiliser). */
    static List<String> warpNames(CommandContext ctx, String input, boolean all) {
        List<String> names = new ArrayList<String>();
        if (all || ctx.player == null) {
            names.addAll(ctx.plugin.catalog().ids());
        } else {
            for (fr.koriamc.warps.model.Warp w : ctx.plugin.warps().listed(ctx.player, false)) {
                names.add(w.getId());
            }
        }
        return Text.complete(input, names);
    }

    /** "none" / "auto" / nœud de permission. */
    static String permissionValue(String input, String warpId) {
        String v = input.trim();
        if (v.equalsIgnoreCase("none") || v.equalsIgnoreCase("aucune") || v.equals("-")) {
            return "";
        }
        if (v.equalsIgnoreCase("auto")) {
            return "koriawarps.warp." + warpId.toLowerCase(Locale.ROOT);
        }
        return v;
    }

    // ================================================================ /warp

    /** /warp [warp] [joueur] : sans argument, ouvre le menu des warps. */
    public static final class Warp extends RootCommand {
        public Warp(KoriaWarps plugin) {
            super(plugin, "warp", false, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (ctx.size() == 0) {
                if (ctx.player != null) {
                    plugin.menus().openWarps(ctx.player);
                } else {
                    ctx.msg("general.usage", "usage", getUsage());
                }
                return;
            }
            fr.koriamc.warps.model.Warp warp = warp(ctx, ctx.arg(0));
            if (warp == null) {
                return;
            }
            if (ctx.size() > 1) {
                if (!ctx.sender.hasPermission(OTHERS)) {
                    ctx.msg("general.no-permission");
                    return;
                }
                Player target = target(ctx, ctx.arg(1));
                if (target == null) {
                    return;
                }
                if (plugin.teleporter().toWarp(target, warp, true)) {
                    ctx.msg("warp.sent-other", "player", target.getName(), "destination", Text.color(warp.getName()));
                }
                return;
            }
            if (ctx.player == null) {
                ctx.msg("general.usage", "usage", getUsage());
                return;
            }
            if (!plugin.warps().canUse(ctx.player, warp)) {
                ctx.msg("warp.no-permission", "destination", Text.color(warp.getName()));
                return;
            }
            plugin.teleporter().toWarp(ctx.player, warp, false);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return warpNames(ctx, ctx.arg(0), false);
            }
            if (ctx.size() == 2 && ctx.sender.hasPermission(OTHERS)) {
                return onlineNames(ctx.arg(1));
            }
            return Collections.emptyList();
        }
    }

    /** /warps : menu (ou liste dans le chat) */
    public static final class Warps extends RootCommand {
        public Warps(KoriaWarps plugin) {
            super(plugin, "warps", false, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (ctx.player != null) {
                plugin.menus().openWarps(ctx.player);
            } else {
                plugin.menus().warpMenu().chat(ctx.sender);
            }
        }
    }

    // ======================================================== Administration

    /** /setwarp &lt;nom&gt; [permission] : crée le warp ici, ou le déplace ici */
    public static final class SetWarp extends RootCommand {
        public SetWarp(KoriaWarps plugin) {
            super(plugin, "setwarp", true, 1, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            String name = ctx.arg(0);
            if (!WarpCatalog.VALID_NAME.matcher(name).matches()) {
                ctx.msg("warp.invalid-name", "warp", name);
                return;
            }
            String permission = ctx.size() > 1 ? permissionValue(ctx.arg(1), name) : null;
            try {
                boolean created = plugin.warps().set(name, ctx.player.getLocation(), permission, ctx.player.getName());
                fr.koriamc.warps.model.Warp warp = plugin.catalog().get(name);
                String shown = warp == null ? "" : plugin.warps().requiredPermission(warp);
                ctx.msg(created ? "warp.created" : "warp.moved", "warp", name.toLowerCase(Locale.ROOT),
                        "destination", warp == null ? name : Text.color(warp.getName()),
                        "permission", shown.isEmpty() ? plugin.msg().text("values.none") : shown);
            } catch (IOException e) {
                saveError(ctx, "warps.json", e);
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return warpNames(ctx, ctx.arg(0), true);
            }
            if (ctx.size() == 2) {
                return Text.complete(ctx.arg(1), Arrays.asList("none", "auto", "koriawarps.warp."));
            }
            return Collections.emptyList();
        }
    }

    /** /delwarp &lt;warp&gt; */
    public static final class DelWarp extends RootCommand {
        public DelWarp(KoriaWarps plugin) {
            super(plugin, "delwarp", false, 1, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            fr.koriamc.warps.model.Warp warp = warp(ctx, ctx.arg(0));
            if (warp == null) {
                return;
            }
            try {
                plugin.warps().delete(warp.getId());
                ctx.msg("warp.deleted", "warp", warp.getId(), "destination", Text.color(warp.getName()));
            } catch (IOException e) {
                saveError(ctx, "warps.json", e);
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? warpNames(ctx, ctx.arg(0), true) : Collections.<String>emptyList();
        }
    }

    /** /editwarp &lt;warp&gt; &lt;option&gt; [valeur] */
    public static final class EditWarp extends RootCommand {

        private static final List<String> OPTIONS = Arrays.asList("name", "permission", "icon", "description", "price",
                "hidden", "move", "rename", "info");

        public EditWarp(KoriaWarps plugin) {
            super(plugin, "editwarp", false, 1, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            fr.koriamc.warps.model.Warp warp = warp(ctx, ctx.arg(0));
            if (warp == null) {
                return;
            }
            String option = ctx.size() > 1 ? ctx.arg(1).toLowerCase(Locale.ROOT) : "info";
            String value = ctx.size() > 2 ? Text.join(ctx.args, 2, " ") : null;
            try {
                if (!apply(ctx, warp, option, value)) {
                    return;
                }
            } catch (IOException e) {
                saveError(ctx, "warps.json", e);
            }
        }

        /** @return false si un message d'erreur a déjà été envoyé */
        private boolean apply(CommandContext ctx, fr.koriamc.warps.model.Warp warp, String option, String value)
                throws IOException {
            String id = warp.getId();
            if (option.equals("info")) {
                AdminCommand.info(ctx, warp);
                return true;
            }
            if (option.equals("move")) {
                if (ctx.player == null) {
                    ctx.msg("general.players-only");
                    return false;
                }
                plugin.warps().set(id, ctx.player.getLocation(), null, ctx.player.getName());
                edited(ctx, id, "move", ctx.player.getWorld().getName());
                return true;
            }
            if (!OPTIONS.contains(option)) {
                ctx.msg("edit.unknown-option", "option", option, "options", Text.join(OPTIONS, ", "));
                return false;
            }
            if (value == null && !option.equals("icon")) {
                ctx.msg("edit.missing-value", "option", option, "warp", id);
                return false;
            }
            if (option.equals("name")) {
                plugin.warps().edit(id, "nom", value);
                edited(ctx, id, option, Text.color(value));
            } else if (option.equals("permission")) {
                String node = permissionValue(value, id);
                plugin.warps().edit(id, "permission", node);
                edited(ctx, id, option, node.isEmpty() ? plugin.msg().text("values.none") : node);
            } else if (option.equals("icon")) {
                String icon = value;
                if (icon == null || icon.equalsIgnoreCase("hand")) {
                    icon = ctx.player == null ? null : ItemIds.of(ctx.player.getItemInHand());
                    if (icon == null) {
                        ctx.msg("edit.icon-hand");
                        return false;
                    }
                } else if (icon.equalsIgnoreCase("none") || icon.equalsIgnoreCase("default")) {
                    icon = "";
                } else if (!icon.contains(":")) {
                    icon = "minecraft:" + icon.toLowerCase(Locale.ROOT);
                }
                plugin.warps().edit(id, "icone", icon);
                edited(ctx, id, option, icon.isEmpty() ? plugin.settings().defaultIcon : icon);
            } else if (option.equals("description")) {
                List<Object> lines = new ArrayList<Object>();
                if (!value.equalsIgnoreCase("none")) {
                    for (String line : value.split("\\|")) {
                        lines.add(line.trim());
                    }
                }
                plugin.warps().edit(id, "description", lines);
                edited(ctx, id, option, lines.isEmpty() ? plugin.msg().text("values.none") : Text.color(value));
            } else if (option.equals("price")) {
                Double price = Text.parseDouble(value);
                if (price == null || price < 0) {
                    ctx.msg("general.invalid-number", "input", value);
                    return false;
                }
                plugin.warps().edit(id, "prix", price);
                edited(ctx, id, option, Text.number(price, 2));
            } else if (option.equals("hidden")) {
                boolean hidden = Text.parseBoolean(value, !warp.isHidden());
                plugin.warps().edit(id, "cache", hidden);
                edited(ctx, id, option, plugin.msg().yesNo(hidden));
            } else if (option.equals("rename")) {
                if (!WarpCatalog.VALID_NAME.matcher(value).matches()) {
                    ctx.msg("warp.invalid-name", "warp", value);
                    return false;
                }
                if (!plugin.warps().rename(id, value)) {
                    ctx.msg("warp.name-taken", "warp", value);
                    return false;
                }
                ctx.msg("warp.renamed", "warp", id, "nouveau", value.toLowerCase(Locale.ROOT));
            }
            return true;
        }

        private void edited(CommandContext ctx, String id, String option, String value) {
            ctx.msg("edit.done", "warp", id, "option", option, "value", value);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return warpNames(ctx, ctx.arg(0), true);
            }
            if (ctx.size() == 2) {
                return Text.complete(ctx.arg(1), OPTIONS);
            }
            if (ctx.size() == 3) {
                String option = ctx.arg(1).toLowerCase(Locale.ROOT);
                if (option.equals("permission")) {
                    return Text.complete(ctx.arg(2), Arrays.asList("none", "auto", "koriawarps.warp."));
                }
                if (option.equals("icon")) {
                    return Text.complete(ctx.arg(2), Arrays.asList("hand", "default", "minecraft:", "koriamc:"));
                }
                if (option.equals("hidden")) {
                    return Text.complete(ctx.arg(2), Arrays.asList("yes", "no"));
                }
                if (option.equals("description")) {
                    return Text.complete(ctx.arg(2), Collections.singletonList("none"));
                }
            }
            return Collections.emptyList();
        }
    }
}
