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
    HELA(0x2E7D4F, 0x9CF2B5);

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
        // (3.3: "Agatha Harkness" is one character too wide: she wakes as "Agatha".)
        return this == PHOENIX || this == AGATHA ? Component.translatable("power.enhanced." + getSerializedName() + ".title")
                : displayName();
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
     * on his optic blast and Professor X walks or rolls in his chair; the gods of Asgard (3.5) walk, and the Bifrost carries
     * them; everyone else can fly (Thor only with Mjolnir, see ThorAbilities).
     */
    public boolean canFly() {
        return switch (this) {
            // (A custom hero flies only if Flight is one of its powers: PowerTicker asks its definition.)
            case MAGNETO, SCARLET_WITCH, VISION, BILLY, THOR, PHOENIX, ULTRON, APOCALYPSE, AGATHA, CUSTOM -> true;
            case NONE, QUICKSILVER, TOMMY, LOKI, SPIDER_MAN, THANOS, CYCLOPS, PROFESSOR_X, HEIMDALL, ODIN, HELA -> false;
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
            case NONE, QUICKSILVER, TOMMY, LOKI, SPIDER_MAN, THANOS, CYCLOPS, PROFESSOR_X, HEIMDALL, ODIN, HELA -> 0.05F;
        };
    }

    /** The ability (1-5) that toggles flight for this hero, matching the original concepts' numbering. */
    public int flightAbility() {
        return switch (this) {
            case MAGNETO -> 5;
            case SCARLET_WITCH, VISION, BILLY, THOR, PHOENIX, ULTRON, APOCALYPSE, AGATHA -> 2;
            // (A custom hero's Flight is on whichever key its player put it.)
            case NONE, QUICKSILVER, TOMMY, LOKI, SPIDER_MAN, THANOS, CYCLOPS, PROFESSOR_X, CUSTOM, HEIMDALL, ODIN, HELA -> -1;
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
