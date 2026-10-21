package fr.koriamc.warps.model;

import java.util.Collections;
import java.util.List;

/**
 * Un warp de warps.json : position, nom affiché, permission éventuelle, icône et prix.
 */
public final class Warp {

    private final String id;
    private final String name;
    private final StoredLocation location;
    private final String permission;
    private final String icon;
    private final List<String> description;
    private final double price;
    private final boolean hidden;
    private final String createdBy;
    private final long createdAt;

    public Warp(String id, String name, StoredLocation location, String permission, String icon, List<String> description,
                double price, boolean hidden, String createdBy, long createdAt) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.permission = permission == null ? "" : permission.trim();
        this.icon = icon;
        this.description = Collections.unmodifiableList(description);
        this.price = Math.max(0, price);
        this.hidden = hidden;
        this.createdBy = createdBy == null ? "" : createdBy;
        this.createdAt = createdAt;
    }

    /** Identifiant (clé de warps.json, en minuscules) : /warp &lt;id&gt;. */
    public String getId() {
        return id;
    }

    /** Nom affiché (codes couleur &amp;). */
    public String getName() {
        return name;
    }

    public StoredLocation getLocation() {
        return location;
    }

    /** Permission propre au warp (vide = aucune en plus de celle de /warp). */
    public String getPermission() {
        return permission;
    }

    /** Item affiché dans le menu (minecraft:ender_pearl, koriamc:jade...). */
    public String getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    /** Prix de chaque téléportation (Vault), 0 = gratuit. */
    public double getPrice() {
        return price;
    }

    /** Absent du menu et de l'autocomplétion (reste utilisable avec /warp &lt;id&gt;). */
    public boolean isHidden() {
        return hidden;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
