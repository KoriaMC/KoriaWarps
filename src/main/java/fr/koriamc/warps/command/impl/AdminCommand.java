package fr.koriamc.warps.command.impl;

import fr.koriamc.warps.KoriaWarps;
import fr.koriamc.warps.command.CommandContext;
import fr.koriamc.warps.command.RootCommand;
import fr.koriamc.warps.config.JsonFile;
import fr.koriamc.warps.model.StoredLocation;
import fr.koriamc.warps.model.Warp;
import fr.koriamc.warps.util.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /koriawarps [help | info &lt;warp&gt; | spawns | config [fichier] | reload]
 */
public final class AdminCommand extends RootCommand {

    public static final String PERM_INFO = "koriawarps.admin.info";
    public static final String PERM_CONFIG = "koriawarps.admin.config";
    public static final String PERM_RELOAD = "koriawarps.admin.reload";

    private static final List<String> ACTIONS = Arrays.asList("help", "info", "spawns", "config", "reload");

    public AdminCommand(KoriaWarps plugin) {
        super(plugin, "koriawarps", false, 0, false);
    }

    @Override
    public void execute(CommandContext ctx) {
        String action = ctx.size() == 0 ? "help" : ctx.arg(0).toLowerCase(Locale.ROOT);
        if (action.equals("help") || action.equals("?")) {
            help(ctx);
        } else if (action.equals("info")) {
            if (!allowed(ctx, PERM_INFO)) {
                return;
            }
            if (ctx.size() < 2) {
                ctx.msg("general.usage", "usage", "info <warp>");
                return;
            }
            Warp warp = WarpCommands.warp(ctx, ctx.arg(1));
            if (warp != null) {
                info(ctx, warp);
            }
        } else if (action.equals("spawns")) {
            if (allowed(ctx, PERM_INFO)) {
                spawns(ctx);
            }
        } else if (action.equals("config")) {
            if (!allowed(ctx, PERM_CONFIG)) {
                return;
            }
            if (ctx.player == null) {
                ctx.msg("general.players-only");
                return;
            }
            if (!plugin.menus().openConfig(ctx.player, ctx.arg(1))) {
                ctx.msg("config-editor.unknown-file", "file", ctx.arg(1));
            }
        } else if (action.equals("reload") || action.equals("rl")) {
            if (!allowed(ctx, PERM_RELOAD)) {
                return;
            }
            List<String> problems = plugin.reload();
            ctx.msg("general.reload-done", "warps", plugin.catalog().count());
            if (!problems.isEmpty()) {
                ctx.msg("general.reload-problems", "count", problems.size());
                for (String problem : problems) {
                    ctx.msg("general.reload-problem", "problem", problem);
                }
            }
        } else {
            ctx.msg("general.unknown-command");
        }
    }

    private static boolean allowed(CommandContext ctx, String node) {
        if (ctx.sender.hasPermission(node)) {
            return true;
        }
        ctx.msg("general.no-permission");
        return false;
    }

    private void help(CommandContext ctx) {
        ctx.msg("help.header");
        for (RootCommand root : plugin.commands().visible(ctx.sender)) {
            if (root != this) {
                ctx.msg("help.line", "command", root.getName(), "usage", root.getUsage(), "description", root.getDescription());
            }
        }
        if (ctx.sender.hasPermission(PERM_INFO) || ctx.sender.hasPermission(PERM_CONFIG)
                || ctx.sender.hasPermission(PERM_RELOAD)) {
            ctx.msg("help.admin-line", "command", getName(), "usage", getUsage(), "description", getDescription());
        }
        ctx.msg("help.footer");
    }

    /** Fiche d'un warp (aussi /editwarp &lt;warp&gt; info). */
    static void info(CommandContext ctx, Warp warp) {
        KoriaWarps plugin = ctx.plugin;
        String permission = plugin.warps().requiredPermission(warp);
        String none = plugin.msg().text("values.none");
        String created = warp.getCreatedAt() <= 0 ? "?"
                : new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(new Date(warp.getCreatedAt()));
        ctx.msg("info.warp", "warp", warp.getId(), "destination", Text.color(warp.getName()),
                "position", warp.getLocation().describe(),
                "charge", plugin.msg().yesNo(warp.getLocation().loadedWorld() != null),
                "permission", permission.isEmpty() ? none : permission,
                "prix", warp.getPrice() > 0 ? Text.number(warp.getPrice(), 2) : plugin.msg().text("values.free"),
                "cache", plugin.msg().yesNo(warp.isHidden()), "icone", warp.getIcon(),
                "auteur", warp.getCreatedBy().isEmpty() ? "?" : warp.getCreatedBy(), "date", created);
        for (String line : warp.getDescription()) {
            ctx.msg("info.warp-description", "ligne", Text.color(line));
        }
    }

    private void spawns(CommandContext ctx) {
        StoredLocation global = plugin.spawns().getGlobal();
        String none = plugin.msg().text("values.none");
        ctx.msg("info.spawns", "global", global == null ? none : global.describe(),
                "mode", plugin.msg().text(plugin.settings().spawnPerWorld ? "values.per-world" : "values.global-only"));
        for (Map.Entry<String, StoredLocation> e : plugin.spawns().getWorlds().entrySet()) {
            ctx.msg("info.spawn-world", "monde", e.getKey(), "position", e.getValue().describe());
        }
    }

    @Override
    public List<String> complete(CommandContext ctx) {
        if (ctx.size() == 1) {
            return Text.complete(ctx.arg(0), ACTIONS);
        }
        if (ctx.size() == 2 && ctx.arg(0).equalsIgnoreCase("info")) {
            return WarpCommands.warpNames(ctx, ctx.arg(1), true);
        }
        if (ctx.size() == 2 && ctx.arg(0).equalsIgnoreCase("config")) {
            List<String> names = new ArrayList<String>();
            for (JsonFile file : plugin.configFiles()) {
                names.add(file.getName().replace(".json", ""));
            }
            return Text.complete(ctx.arg(1), names);
        }
        return Collections.emptyList();
    }
}
