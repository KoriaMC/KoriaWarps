package fr.koriamc.warps.command.impl;

import fr.koriamc.warps.KoriaWarps;
import fr.koriamc.warps.command.CommandContext;
import fr.koriamc.warps.command.RootCommand;
import fr.koriamc.warps.model.StoredLocation;
import fr.koriamc.warps.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /spawn, /setspawn, /delspawn.
 */
public final class SpawnCommands {

    /** Téléporter quelqu'un d'autre : /spawn &lt;joueur&gt;. */
    public static final String OTHERS = "koriawarps.command.spawn.others";

    private SpawnCommands() {
    }

    /** /spawn [joueur] */
    public static final class Spawn extends RootCommand {
        public Spawn(KoriaWarps plugin) {
            super(plugin, "spawn", false, 0, false);
        }

        @Override
        public void execute(CommandContext ctx) {
            if (ctx.size() > 0) {
                if (!ctx.sender.hasPermission(OTHERS)) {
                    ctx.msg("general.no-permission");
                    return;
                }
                Player target = WarpCommands.target(ctx, ctx.arg(0));
                if (target == null) {
                    return;
                }
                if (plugin.spawns().spawnFor(target.getWorld()) == null) {
                    ctx.msg("spawn.not-set");
                    return;
                }
                if (plugin.teleporter().toSpawn(target, true)) {
                    ctx.msg("spawn.sent-other", "player", target.getName());
                }
                return;
            }
            if (ctx.player == null) {
                ctx.msg("general.usage", "usage", getUsage());
                return;
            }
            plugin.teleporter().toSpawn(ctx.player, false);
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 && ctx.sender.hasPermission(OTHERS)
                    ? WarpCommands.onlineNames(ctx.arg(0)) : Collections.<String>emptyList();
        }
    }

    /** /setspawn [global|world] : spawn du serveur (par défaut) ou de ce monde */
    public static final class SetSpawn extends RootCommand {
        public SetSpawn(KoriaWarps plugin) {
            super(plugin, "setspawn", true, 0, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            String scope = ctx.size() > 0 ? ctx.arg(0).toLowerCase(Locale.ROOT) : "global";
            try {
                if (scope.equals("global")) {
                    plugin.spawns().setGlobal(ctx.player.getLocation());
                    ctx.msg("spawn.set-global", "position", StoredLocation.of(ctx.player.getLocation()).describe());
                } else if (scope.equals("world")) {
                    plugin.spawns().setWorld(ctx.player.getLocation());
                    ctx.msg("spawn.set-world", "monde", ctx.player.getWorld().getName(),
                            "position", StoredLocation.of(ctx.player.getLocation()).describe());
                    if (!plugin.settings().spawnPerWorld) {
                        ctx.msg("spawn.per-world-disabled");
                    }
                } else {
                    ctx.msg("general.usage", "usage", getUsage());
                }
            } catch (IOException e) {
                WarpCommands.saveError(ctx, "spawns.json", e);
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            return ctx.size() == 1 ? Text.complete(ctx.arg(0), Arrays.asList("global", "world"))
                    : Collections.<String>emptyList();
        }
    }

    /** /delspawn &lt;global|world&gt; [monde] */
    public static final class DelSpawn extends RootCommand {
        public DelSpawn(KoriaWarps plugin) {
            super(plugin, "delspawn", false, 1, true);
        }

        @Override
        public void execute(CommandContext ctx) {
            String scope = ctx.arg(0).toLowerCase(Locale.ROOT);
            try {
                if (scope.equals("global")) {
                    ctx.msg(plugin.spawns().removeGlobal() ? "spawn.removed-global" : "spawn.not-set");
                } else if (scope.equals("world")) {
                    String world = ctx.size() > 1 ? ctx.arg(1) : ctx.player != null ? ctx.player.getWorld().getName() : null;
                    if (world == null) {
                        ctx.msg("general.usage", "usage", getUsage());
                        return;
                    }
                    ctx.msg(plugin.spawns().removeWorld(world) ? "spawn.removed-world" : "spawn.no-world-spawn",
                            "monde", world);
                } else {
                    ctx.msg("general.usage", "usage", getUsage());
                }
            } catch (IOException e) {
                WarpCommands.saveError(ctx, "spawns.json", e);
            }
        }

        @Override
        public List<String> complete(CommandContext ctx) {
            if (ctx.size() == 1) {
                return Text.complete(ctx.arg(0), Arrays.asList("global", "world"));
            }
            if (ctx.size() == 2 && ctx.arg(0).equalsIgnoreCase("world")) {
                List<String> names = new ArrayList<String>(plugin.spawns().getWorlds().keySet());
                for (World world : Bukkit.getWorlds()) {
                    if (!names.contains(world.getName().toLowerCase(Locale.ROOT))) {
                        names.add(world.getName());
                    }
                }
                return Text.complete(ctx.arg(1), names);
            }
            return Collections.emptyList();
        }
    }
}
