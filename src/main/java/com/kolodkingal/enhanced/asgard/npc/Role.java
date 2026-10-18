package com.kolodkingal.enhanced.asgard.npc;

import java.util.Locale;

/**
 * The roles of the Asgardians (3.5), the one entity {@code enhanced:asgardian}: the eight named gods and warriors (one
 * each in the city, the "fixtures"), the Einherjar guards and the citizens. Saved by name: append only.
 *
 * <p>Each role carries what makes it itself on the server: its health, armour and blow, its size (Volstagg is big), the
 * colour of its name on the talk screen, the pitch of its murmur, whether it fights beside the players, whether it trades,
 * and how many skins it has (Einherjar 4, citizens 8, every named one 1).
 */
public enum Role {
    HEIMDALL(60, 8, 9, 1.0F, 0xFFD36B, Voice.LOW, true, false, 1, false),
    ODIN(80, 10, 9, 1.0F, 0xFFE9B0, Voice.LOW, false, false, 1, false),
    FRIGGA(40, 4, 6, 1.0F, 0xF3E3B6, Voice.HIGH, false, false, 1, true),
    SIF(40, 8, 8, 1.0F, 0xC9D3E0, Voice.HIGH, true, true, 1, true),
    VALKYRIE(40, 8, 8, 1.0F, 0x8FC8FF, Voice.HIGH, true, true, 1, true),
    VOLSTAGG(60, 7, 8, 1.15F, 0xE0784A, Voice.LOW, true, true, 1, false),
    FANDRAL(40, 6, 7, 1.0F, 0x9AD86E, Voice.MID, true, false, 1, false),
    HOGUN(40, 7, 8, 1.0F, 0x9FB3D9, Voice.LOW, true, false, 1, false),
    EINHERJAR(30, 6, 6, 1.0F, 0xE8C26A, Voice.MID, true, false, 4, false),
    CITIZEN(30, 0, 2, 1.0F, 0xE6D8B8, Voice.MID, false, false, 8, false);

    /** The pitch of a role's murmur ({@code npc_talk_low|mid|high}). */
    public enum Voice { LOW, MID, HIGH }

    private final double health;
    private final double armor;
    private final double damage;
    private final float scale;
    private final int color;
    private final Voice voice;
    private final boolean warrior;
    private final boolean trader;
    private final int skins;
    private final boolean slim;

    Role(double health, double armor, double damage, float scale, int color, Voice voice, boolean warrior, boolean trader,
         int skins, boolean slim) {
        this.health = health;
        this.armor = armor;
        this.damage = damage;
        this.scale = scale;
        this.color = color;
        this.voice = voice;
        this.warrior = warrior;
        this.trader = trader;
        this.skins = skins;
        this.slim = slim;
    }

    /** Whether there is exactly one of this role in the city (the eight named ones). */
    public boolean named() {
        return this != EINHERJAR && this != CITIZEN;
    }

    /** The id in lang keys and slots: {@code heimdall}, {@code einherjar}... */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The lang key of the role's name: {@code entity.enhanced.asgardian.<role>}. */
    public String nameKey() {
        return "entity.enhanced.asgardian." + id();
    }

    public double health() {
        return health;
    }

    public double armor() {
        return armor;
    }

    public double damage() {
        return damage;
    }

    /** The size of the role's body (1 = a player's). */
    public float scale() {
        return scale;
    }

    /** The colour of the role's name. */
    public int color() {
        return color;
    }

    public Voice voice() {
        return voice;
    }

    /**
     * Whether the role fights Hela's army beside the players ({@code DefendAsgardGoal}): Heimdall, Sif, Valkyrie, the
     * Warriors Three and the Einherjar. Odin, Frigga and the citizens do not (the citizens flee).
     */
    public boolean warrior() {
        return warrior;
    }

    /** Whether the role trades through the vanilla merchant screen (Sif, Valkyrie, Volstagg). */
    public boolean trader() {
        return trader;
    }

    /** How many skins the role has ({@code variant} is below this). */
    public int skins() {
        return skins;
    }

    /** Whether this role's skin {@code variant} has slim (3-pixel) arms: Frigga, Sif, Valkyrie and the odd citizens. */
    public boolean slim(int variant) {
        return this == CITIZEN ? variant % 2 == 1 : slim;
    }

    /** The Warriors Three. */
    public boolean warriorsThree() {
        return this == VOLSTAGG || this == FANDRAL || this == HOGUN;
    }

    /** The role saved under {@code name}, or CITIZEN for an unknown name. */
    public static Role byName(String name) {
        for (Role role : values()) {
            if (role.name().equals(name)) {
                return role;
            }
        }
        return CITIZEN;
    }
}
