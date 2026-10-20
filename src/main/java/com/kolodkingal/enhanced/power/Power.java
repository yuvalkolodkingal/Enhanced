package com.kolodkingal.enhanced.power;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.AgathaConfig;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * The hero a player has become. A player has exactly one power at a time.
 */
public enum Power implements StringRepresentable {
    NONE(0xFFFFFF, 0xFFFFFF),
    // Energy colours follow the films and shows: Magneto's magnetism has no glow in the X-Men films (iron-grey metal
    // dust), the Scarlet Witch's chaos magic is crimson with hot pink highlights, Fox Quicksilver leaves only a grey
    // blur, and the Mind Stone's beam is pale gold.
    MAGNETO(0x8A9096, 0xD2D6DA),
    SCARLET_WITCH(0xE0193F, 0xFF6F86),
    QUICKSILVER(0x9AA3AD, 0xC8D0D8),
    VISION(0xF5D77A, 0xFFF6D5),
    BILLY(0x5CC8FF, 0xCFEFFF),
    TOMMY(0x2E9C7E, 0x7FD8B8),
    LOKI(0x76B154, 0xCAF297),
    THOR(0x4DB8FF, 0xBFE6FF),
    // The heroes of 3.0, in Eilon's order. They are appended after Thor and in this order: the stream codec sends the
    // ordinal, so every hero of one version must be in the same build (saves use the name, so the order never matters
    // on disk). Phoenix fire is gold-orange, Spider-Man's suit red with silver webs, the Infinity Stones purple, the
    // Sentry-bright amber of Ultron's body and the ochre and gold of Apocalypse.
    PHOENIX(0xFF8A1F, 0xFFD65A),
    SPIDER_MAN(0xDC2B2F, 0xDCE6F2),
    THANOS(0x9B4DEB, 0xE6D2FF),
    ULTRON(0xFFA23A, 0xFFE9B8),
    APOCALYPSE(0xF2B84B, 0xFFF0B8),
    // The heroes of 3.3, appended after Apocalypse in this order (Agatha, Cyclops, Professor X; each lane adds its own).
    // Agatha's dark magic is the violet of WandaVision ("It's been Agatha all along"), with a pale lilac glow.
    AGATHA(0x9A3FE0, 0xE2C2FF),
    // Cyclops's optic blast is ruby red with a hot pink-white core: the colour his ruby-quartz glasses give it.
    CYCLOPS(0xE8323A, 0xFFB3A8),
    // Professor X's telepathy has the cold blue-white of Cerebro.
    PROFESSOR_X(0x6F8CFF, 0xDCE4FF),
    // 3.3 (the heroes AGATHA, CYCLOPS and PROFESSOR_X of the other lanes come before it): a hero of the player's own,
    // made with the Hero Creator. Its name, powers, keys, colours and look are the player's saved definition (see
    // custom/CustomHero); these two colours are only the Hero Creator's own gold, for whatever has no definition.
    CUSTOM(0xF5A623, 0xFFE7A8),
    // The heroes of 3.5 (Asgard), appended after CUSTOM in this order (the 3.5 skeleton adds them; each lane fills its own).
    // Heimdall's armour and Hofund are gold, with the warm light of the Bifrost; the Allfather's Odinforce is a paler
    // gold; Hela is the dark emerald of her headdress, with a pale green glow (her name in a brighter green).
    HEIMDALL(0xE8B54A, 0xFFE7A3),
    ODIN(0xD9A63A, 0xFFF0C2),
    HELA(0x2E7D4F, 0x9CF2B5),
    // The heroes of 3.6, appended after HELA in this order (the 3.6 skeleton adds them; each lane fills its own).
    // Doctor Strange's spells are the orange-gold of the mystic arts' sparks, with a warm pale glow (his name in the
    // cloak's red); the Hulk is Hulk green with a gamma-green glow (his name a lighter green).
    DOCTOR_STRANGE(0xF29A2E, 0xFFE0A8),
    HULK(0x5BA33A, 0xB6F27A),
    // The heroes of 3.7, appended after HULK in this order (the 3.7 skeleton adds them; each lane fills its own).
    // Wolverine is the yellow of the classic suit with the silver of adamantium; Deadpool the red of his suit with a
    // pale pink glow (his name in a deeper red).
    WOLVERINE(0xF2C230, 0xD9DEE3),
    DEADPOOL(0xC8102E, 0xFF8A8A),
    // The heroes of 3.8, appended after DEADPOOL in this order (the 3.8 skeleton adds them; each lane fills its own).
    // Captain America is the suit's blue with the star's silver-white (his name a brighter blue); Red Hulk a deep red
    // with a hot orange glow; Iron Man the suit's red with the arc reactor's blue; Falcon the wings' steel with his red
    // accents (his name red); Love the purple of her eyes with Asgard's gold.
    CAPTAIN_AMERICA(0x2A4BA0, 0xE8EEF5),
    RED_HULK(0xC4231B, 0xFF7A3A),
    IRON_MAN(0xB3121B, 0x9FE8FF),
    FALCON(0x8A8F98, 0xE0313A),
    LOVE(0xB57BFF, 0xFFE59A);

    /**
     * By name. A name this version does not know (a hero it no longer has) reads as {@link #NONE} instead of
     * failing, so a world saved with that hero still loads.
     */
    public static final Codec<Power> CODEC = Codec.STRING.xmap(name -> {
        Power power = byName(name);
        return power == null ? NONE : power;
    }, Power::getSerializedName);

    /** Main colour (UI text, particles). */
    public final int color;
    /** Secondary glow colour. */
    public final int glow;

    Power(int color, int glow) {
        this.color = color;
        this.glow = glow;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Colour of the hero's name (titles, HUD): the energy colour, except for three whose energy would not read as the
     * hero: Magneto's power has no colour, Ultron is named in the red of his eyes and Apocalypse in the blue of his.
     */
    public int titleColor() {
        return switch (this) {
            case MAGNETO -> 0xC8323C;
            case ULTRON -> 0xD8262B;
            case APOCALYPSE -> 0x4A9BC4;
            // (3.5: her dark emerald would not read on the dark sky of a title.)
            case HELA -> 0x3FBF6F;
            // (3.6: Doctor Strange is named in the red of his Cloak of Levitation, the Hulk in a lighter green.)
            case DOCTOR_STRANGE -> 0xC2302E;
            case HULK -> 0x6CC04A;
            // (3.7: Wolverine is named in his suit's yellow, Deadpool in a red that reads on the dark sky.)
            case WOLVERINE -> 0xF2C230;
            case DEADPOOL -> 0xD6262E;
            // (3.8: each named so that he reads on the dark sky of a title.)
            case CAPTAIN_AMERICA -> 0x3D6FD6;
            case RED_HULK -> 0xE0352A;
            case IRON_MAN -> 0xD62A2A;
            case FALCON -> 0xD8323C;
            case LOVE -> 0xC08CFF;
            default -> color;
        };
    }

    public boolean hasPowers() {
        return this != NONE;
    }

    public Component displayName() {
        return Component.translatable("power.enhanced." + getSerializedName());
    }

    /**
     * The name the title of the awakening shows: the hero's own, except where that is too wide for the screen. A title is
     * drawn in bold four times the size of the text, and a 1080p window at the default GUI scale is only 480 of those
     * wide (the default 854x480 window 427): "Jean Grey (Phoenix)" was cut at both ends, so the Phoenix wakes as "The Phoenix".
     */
    public Component awakeningTitle() {
        // (3.3: "Agatha Harkness" is one character too wide: she wakes as "Agatha". 3.6: Doctor Strange's Hebrew name
        // is one too wide: he wakes as "ד"ר סטריינג'". 3.8: "Captain America" is one too wide: he wakes as "The
        // Captain".)
        return this == PHOENIX || this == AGATHA || this == DOCTOR_STRANGE || this == CAPTAIN_AMERICA
                ? Component.translatable("power.enhanced." + getSerializedName() + ".title") : displayName();
    }

    /** Translation key for the given ability (1-5). */
    public String abilityKey(int ability) {
        if (this == BILLY && ability == 1 && !com.kolodkingal.enhanced.ability.BillyAbilities.beamEnabled()) {
            return "ability.enhanced.billy.1.bolt"; // (billy.beam false: R is the single Hex Bolt again)
        }
        return "ability.enhanced." + getSerializedName() + "." + ability;
    }

    /**
     * Quicksilver, Tommy, Spider-Man and Thanos run, swing or step through portals, Loki walks, Cyclops runs and leaps
     * on his optic blast and Professor X walks or rolls in his chair; the gods of Asgard (3.5) walk, and the Bifrost
     * carries them; the Hulk (3.6) leaps; everyone else can fly (Thor only with Mjolnir, see ThorAbilities; Doctor
     * Strange on his Cloak of Levitation); Wolverine and Deadpool (3.7) leap, pounce and teleport. 3.8: Iron Man flies
     * in a suit that flies, Falcon with his Exo-Wings on and Love while she holds Stormbreaker ({@code PowerTicker}
     * asks each one's gate); Captain America and Red Hulk run and leap.
     */
    public boolean canFly() {
        return switch (this) {
            // (A custom hero flies only if Flight is one of its powers: PowerTicker asks its definition.)
            case MAGNETO, SCARLET_WITCH, VISION, BILLY, THOR, PHOENIX, ULTRON, APOCALYPSE, AGATHA, CUSTOM,
                    DOCTOR_STRANGE, IRON_MAN, FALCON, LOVE -> true;
            case NONE, QUICKSILVER, TOMMY, LOKI, SPIDER_MAN, THANOS, CYCLOPS, PROFESSOR_X, HEIMDALL, ODIN, HELA,
                    HULK, WOLVERINE, DEADPOOL, CAPTAIN_AMERICA, RED_HULK -> false;
        };
    }

    public float flightSpeed() {
        return switch (this) {
            case MAGNETO -> EnhancedConfig.MAGNETO_FLIGHT_SPEED.get().floatValue();
            case SCARLET_WITCH -> EnhancedConfig.WANDA_FLIGHT_SPEED.get().floatValue();
            case VISION -> EnhancedConfig.VISION_FLIGHT_SPEED.get().floatValue();
            case BILLY -> EnhancedConfig.BILLY_FLIGHT_SPEED.get().floatValue();
            case THOR -> EnhancedConfig.THOR_FLIGHT_SPEED.get().floatValue();
            case PHOENIX -> PhoenixConfig.FLIGHT_SPEED.get().floatValue();
            case ULTRON -> UltronConfig.FLIGHT_SPEED.get().floatValue();
            case APOCALYPSE -> ApocalypseConfig.FLIGHT_SPEED.get().floatValue();
            case AGATHA -> AgathaConfig.FLIGHT_SPEED.get().floatValue();
            // (A custom hero's speed is its Flight's level: PowerTicker asks its definition.)
            case CUSTOM -> com.kolodkingal.enhanced.config.CustomHeroConfig.FLIGHT_SPEED.get().floatValue();
            // (3.6: the Cloak of Levitation is graceful, slower than Thor.)
            case DOCTOR_STRANGE -> com.kolodkingal.enhanced.config.StrangeConfig.FLIGHT_SPEED.get().floatValue();
            // (3.8: Iron Man's speed is his worn suit's, PowerTicker asks IronSuits.flightSpeed; this is the Mark 3's.)
            case IRON_MAN -> com.kolodkingal.enhanced.config.IronManConfig.MARK3_FLIGHT_SPEED.get().floatValue();
            case FALCON -> com.kolodkingal.enhanced.config.FalconConfig.FLIGHT_SPEED.get().floatValue();
            case LOVE -> com.kolodkingal.enhanced.config.EternityConfig.LOVE_FLIGHT_SPEED.get().floatValue();
            case NONE, QUICKSILVER, TOMMY, LOKI, SPIDER_MAN, THANOS, CYCLOPS, PROFESSOR_X, HEIMDALL, ODIN, HELA,
                    HULK, WOLVERINE, DEADPOOL, CAPTAIN_AMERICA, RED_HULK -> 0.05F;
        };
    }

    /** The ability (1-5) that toggles flight for this hero, matching the original concepts' numbering. */
    public int flightAbility() {
        return switch (this) {
            case MAGNETO -> 5;
            case SCARLET_WITCH, VISION, BILLY, THOR, PHOENIX, ULTRON, APOCALYPSE, AGATHA, DOCTOR_STRANGE, IRON_MAN,
                    FALCON, LOVE -> 2;
            // (A custom hero's Flight is on whichever key its player put it; 3.6: all five of the Hulk's keys are
            // rows; 3.7: and Wolverine's and Deadpool's; 3.8: and Captain America's and Red Hulk's.)
            case NONE, QUICKSILVER, TOMMY, LOKI, SPIDER_MAN, THANOS, CYCLOPS, PROFESSOR_X, CUSTOM, HEIMDALL, ODIN, HELA,
                    HULK, WOLVERINE, DEADPOOL, CAPTAIN_AMERICA, RED_HULK -> -1;
        };
    }

    public static Power byName(String name) {
        for (Power p : values()) {
            if (p.getSerializedName().equals(name)) {
                return p;
            }
        }
        return null;
    }
}
