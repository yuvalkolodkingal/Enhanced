package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.power.Power;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The roles of the Asgardians (3.5), the one entity {@code enhanced:asgardian}: the eight named gods and warriors (one
 * each in the city, the "fixtures"), the Einherjar guards and the citizens. Saved by name: append only. 3.7: the three
 * of Agatha's Cottage ({@link #cottage()}: Agatha, her son Nicky and Rio Vidal), of the second entity type {@code
 * enhanced:cottage_resident}.
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
    CITIZEN(30, 0, 2, 1.0F, 0xE6D8B8, Voice.MID, false, false, 8, false),
    // 3.7: the family of Agatha's Cottage (the entity enhanced:cottage_resident, the same class; lane L6). Appended:
    // the roles above keep their ordinals.
    AGATHA(30, 2, 2, 1.0F, 0x9A3FE0, Voice.HIGH, false, true, 1, true),
    NICKY(20, 0, 1, 0.75F, 0x7FD8B8, Voice.HIGH, false, false, 1, false),
    RIO(30, 2, 2, 1.0F, 0x4FA35A, Voice.MID, false, true, 1, true);

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

    /** The voice of a skin variant: the citizens who are women (odd variants) hum high. */
    public Voice voice(int variant) {
        return this == CITIZEN && variant % 2 == 1 ? Voice.HIGH : voice;
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

    /**
     * The playable hero this role is (3.5.1): Heimdall and Odin are both, else null. There is one of each character: while
     * a player is the role's hero, the role's Asgardian {@link #yieldsToHero() yields} to him.
     */
    @Nullable
    public Power hero() {
        return switch (this) {
            case HEIMDALL -> Power.HEIMDALL;
            case ODIN -> Power.ODIN;
            // (3.7: the cottage's Agatha is away while a player is Agatha.)
            case AGATHA -> Power.AGATHA;
            default -> null;
        };
    }

    /**
     * Whether the role's Asgardian leaves the city while any player online is its {@link #hero()} (the keeper sends him
     * away in a gold shimmer and brings him back when nobody is): every role with a hero but Heimdall, who stays and hands
     * his post to the player for a while instead.
     */
    public boolean yieldsToHero() {
        return hero() != null && this != HEIMDALL;
    }

    /** Whether the role lives in Agatha's Cottage (3.7: Agatha, Nicky, Rio), not in Asgard. */
    public boolean cottage() {
        return this == AGATHA || this == NICKY || this == RIO;
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
