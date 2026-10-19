package com.kolodkingal.enhanced.hydra;

/**
 * The rank of a {@link HydraSoldierEntity} (3.6, the plan's 15.2): what he carries, how tough he is, where he serves.
 * Saved by name, so new ranks are only ever appended. The officer is his own type ({@link HydraOfficerEntity}).
 */
public enum Rank {
    /** An energy rifle (bursts of three bolts) and its butt up close: the gate, the yard, the perimeter. */
    RIFLEMAN("rifleman", 24.0, 6.0, 4, 5),
    /** A stun baton and a riot shield that stops arrows and bolts from the front: the yard patrol, the cellar. */
    ENFORCER("enforcer", 30.0, 10.0, 2, 5),
    /** A long rifle: a red laser warns for a moment, then one heavy bolt. The watchtower only. */
    SNIPER("sniper", 20.0, 4.0, 1, 8);

    private final String id;
    private final double health;
    private final double armor;
    private final int skins;
    private final int xp;

    Rank(String id, double health, double armor, int skins, int xp) {
        this.id = id;
        this.health = health;
        this.armor = armor;
        this.skins = skins;
        this.xp = xp;
    }

    /** Its name in saves and in lang keys ({@code entity.enhanced.hydra_soldier.<id>}). */
    public String id() {
        return id;
    }

    public double health() {
        return health;
    }

    public double armor() {
        return armor;
    }

    /** How many skins the rank has (the client draws {@code textures/entity/hydra/<id>_<variant>.png}). */
    public int skins() {
        return skins;
    }

    /** The experience he leaves. */
    public int xp() {
        return xp;
    }

    /** The rank of a saved name; an unknown name (a save from a later version) is a rifleman. */
    public static Rank byName(String name) {
        for (Rank rank : values()) {
            if (rank.id.equals(name)) {
                return rank;
            }
        }
        return RIFLEMAN;
    }

    /** The rank of a synced ordinal (anything out of range is a rifleman). */
    public static Rank byOrdinal(int ordinal) {
        Rank[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : RIFLEMAN;
    }
}
