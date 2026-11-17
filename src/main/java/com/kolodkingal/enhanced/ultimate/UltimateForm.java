package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.power.Power;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Every hero's ultimate form (3.17, Ultimate Mode): what O awakens, as the films and comics have each one at their
 * strongest. A form has its colours, the aura every client draws round the hero while it lasts ({@link Aura}, drawn by
 * {@code client.magic.MagicFx}), the signature move O fires while it lasts ({@link Signature}, {@link UltimateForms})
 * and the strengths it gives (status effects, kept up while it lasts).
 *
 * <p>Two forms change the hero's powers themselves: Vision's is the Solar Jewel (his beam, Overload and eye beams are
 * the jewel's, {@code VisionQuest.solar}), and Billy's is the Demiurge's star form (his spells need no incantation,
 * {@code magic.Casting}). A witch (no hero power, the witchcraft of 3.17) awakens the Witch Supreme.
 */
public enum UltimateForm {
    MAGNETO(Power.MAGNETO, 0xA8B0BA, 0xF0F3F6, Aura.SHARDS, Signature.VORTEX,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.DAMAGE_BOOST, 0)),
    SCARLET_WITCH(Power.SCARLET_WITCH, 0xE0193F, 0xFF8FA2, Aura.RUNES, Signature.NOVA,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.REGENERATION, 0)),
    QUICKSILVER(Power.QUICKSILVER, 0xB4BDC8, 0xEEF2F6, Aura.SPEED, Signature.BARRAGE,
            fx(MobEffects.MOVEMENT_SPEED, 2), fx(MobEffects.DIG_SPEED, 1)),
    VISION(Power.VISION, 0xFFC93A, 0xFFF3C2, Aura.SOLAR, Signature.BEAM_RING,
            fx(MobEffects.DAMAGE_RESISTANCE, 0), fx(MobEffects.FIRE_RESISTANCE, 0)),
    BILLY(Power.BILLY, 0x7FD0FF, 0xFFFFFF, Aura.STARS, Signature.STORM,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.REGENERATION, 1)),
    TOMMY(Power.TOMMY, 0x2E9C7E, 0xA8F0D4, Aura.SPEED, Signature.VORTEX,
            fx(MobEffects.MOVEMENT_SPEED, 2), fx(MobEffects.DIG_SPEED, 1)),
    LOKI(Power.LOKI, 0x76B154, 0xE4FFB8, Aura.TIMELINES, Signature.BIND,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.REGENERATION, 0)),
    THOR(Power.THOR, 0x4DB8FF, 0xE6F6FF, Aura.LIGHTNING, Signature.STRIKES,
            fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.DAMAGE_RESISTANCE, 0)),
    PHOENIX(Power.PHOENIX, 0xFF6A12, 0xFFE07A, Aura.FLAME, Signature.FIRE,
            fx(MobEffects.FIRE_RESISTANCE, 0), fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.REGENERATION, 0)),
    SPIDER_MAN(Power.SPIDER_MAN, 0xDC2B2F, 0xDCE6F2, Aura.MIND, Signature.BIND,
            fx(MobEffects.MOVEMENT_SPEED, 1), fx(MobEffects.JUMP, 2), fx(MobEffects.DAMAGE_RESISTANCE, 0)),
    THANOS(Power.THANOS, 0x9B4DEB, 0xFFD24A, Aura.STONES, Signature.SNAP,
            fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.DAMAGE_RESISTANCE, 1)),
    ULTRON(Power.ULTRON, 0xFF6A1A, 0xFFE0A0, Aura.NANO, Signature.STRIKES,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.DAMAGE_BOOST, 0)),
    APOCALYPSE(Power.APOCALYPSE, 0xF2B84B, 0xFFF0B8, Aura.SAND, Signature.VORTEX,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.DAMAGE_BOOST, 1)),
    AGATHA(Power.AGATHA, 0x9A3FE0, 0xE2C2FF, Aura.SHADOW, Signature.DRAIN,
            fx(MobEffects.DAMAGE_RESISTANCE, 0), fx(MobEffects.REGENERATION, 1)),
    CYCLOPS(Power.CYCLOPS, 0xE8323A, 0xFFB3A8, Aura.MIND, Signature.BEAM_RING,
            fx(MobEffects.DAMAGE_RESISTANCE, 0), fx(MobEffects.DAMAGE_BOOST, 0)),
    PROFESSOR_X(Power.PROFESSOR_X, 0x6F8CFF, 0xDCE4FF, Aura.MIND, Signature.BIND,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.REGENERATION, 0)),
    CUSTOM(Power.CUSTOM, 0xF5A623, 0xFFE7A8, Aura.HALO, Signature.NOVA,
            fx(MobEffects.DAMAGE_RESISTANCE, 0), fx(MobEffects.DAMAGE_BOOST, 0)),
    HEIMDALL(Power.HEIMDALL, 0xE8B54A, 0xFFE7A3, Aura.HALO, Signature.REVEAL,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.NIGHT_VISION, 0)),
    ODIN(Power.ODIN, 0xD9A63A, 0xFFF6D8, Aura.HALO, Signature.NOVA,
            fx(MobEffects.DAMAGE_RESISTANCE, 2), fx(MobEffects.REGENERATION, 0)),
    HELA(Power.HELA, 0x2E7D4F, 0x9CF2B5, Aura.SHADOW, Signature.BLADES,
            fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.DAMAGE_RESISTANCE, 0)),
    DOCTOR_STRANGE(Power.DOCTOR_STRANGE, 0xF29A2E, 0xFFE0A8, Aura.RUNES, Signature.BIND,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.REGENERATION, 0)),
    HULK(Power.HULK, 0x5BA33A, 0xC8FF8A, Aura.GAMMA, Signature.NOVA,
            fx(MobEffects.DAMAGE_BOOST, 2), fx(MobEffects.DAMAGE_RESISTANCE, 1)),
    WOLVERINE(Power.WOLVERINE, 0xF2C230, 0xE6EBF0, Aura.FERAL, Signature.BARRAGE,
            fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.MOVEMENT_SPEED, 1), fx(MobEffects.REGENERATION, 1)),
    DEADPOOL(Power.DEADPOOL, 0xC8102E, 0xFFB0B0, Aura.FERAL, Signature.BLAST,
            fx(MobEffects.MOVEMENT_SPEED, 1), fx(MobEffects.REGENERATION, 2)),
    CAPTAIN_AMERICA(Power.CAPTAIN_AMERICA, 0x4DB8FF, 0xE8EEF5, Aura.LIGHTNING, Signature.STRIKES,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.DAMAGE_BOOST, 0)),
    RED_HULK(Power.RED_HULK, 0xE0352A, 0xFFB060, Aura.FLAME, Signature.FIRE,
            fx(MobEffects.FIRE_RESISTANCE, 0), fx(MobEffects.DAMAGE_BOOST, 2), fx(MobEffects.DAMAGE_RESISTANCE, 0)),
    IRON_MAN(Power.IRON_MAN, 0xD62A2A, 0xFFD24A, Aura.NANO, Signature.BEAM_RING,
            fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.DAMAGE_BOOST, 0)),
    FALCON(Power.FALCON, 0xB8C2D0, 0xE0313A, Aura.WINGS, Signature.BLADES,
            fx(MobEffects.MOVEMENT_SPEED, 1), fx(MobEffects.DAMAGE_RESISTANCE, 0)),
    LOVE(Power.LOVE, 0xB57BFF, 0xFFE59A, Aura.LIGHTNING, Signature.STRIKES,
            fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.DAMAGE_RESISTANCE, 0)),
    /** A witch of 3.17 (no hero power): the Witch Supreme, in the colour of her path. */
    WITCH(Power.NONE, 0x7CE0B0, 0xF0FFF6, Aura.RUNES, Signature.NOVA,
            fx(MobEffects.DAMAGE_RESISTANCE, 0), fx(MobEffects.REGENERATION, 1));

    /** How the aura of a form is drawn round the hero while it lasts (and while it awakens). */
    public enum Aura {
        /** Shards of metal circling in tilted orbits. */
        SHARDS,
        /** Rings of runes turning round the body, a crown of light over the head. */
        RUNES,
        /** Streaks of light whirling round the body. */
        SPEED,
        /** A sun at the brow, rays turning, motes drawn in. */
        SOLAR,
        /** A white star: stars orbit the hero, a halo and rays of white-blue light (the Demiurge). */
        STARS,
        /** Branching threads of time growing out round the hero (the God of Stories). */
        TIMELINES,
        /** Lightning crackling over the body and up into the sky. */
        LIGHTNING,
        /** Flames rising round the body, wings of fire. */
        FLAME,
        /** Rings round the head, pulsing outward. */
        MIND,
        /** Six coloured stones in orbit. */
        STONES,
        /** Rings of hexagonal plates turning, data motes. */
        NANO,
        /** A spiral of sand rising round the body. */
        SAND,
        /** Dark wisps rising, a ring of shadow at the feet. */
        SHADOW,
        /** A golden halo and rays behind the head. */
        HALO,
        /** Green veins of light and a cracking ring on the ground. */
        GAMMA,
        /** Claw-marks of light slashing round the body. */
        FERAL,
        /** Two great wings of light at the back. */
        WINGS
    }

    /** What O does while the form lasts: the hero's signature move. */
    public enum Signature {
        /** A shockwave runs out along the ground, hurting and throwing back everything it passes. */
        NOVA,
        /** Bolts from the sky on everything near. */
        STRIKES,
        /** Everything near is pulled in to the hero, then thrown out. */
        VORTEX,
        /** Beams from everything near to the hero: what they take heals him. */
        DRAIN,
        /** Everything near is bound where it stands (rings of light hold it). */
        BIND,
        /** The hero strikes everything near, one after another, too fast to see. */
        BARRAGE,
        /** Half of the hostile creatures near turn to dust; the rest are hurt. */
        SNAP,
        /** A rain of blades (or feathers of vibranium) on everything near. */
        BLADES,
        /** Bolts of heaven's own lightning, again and again for three seconds. */
        STORM,
        /** A ring of beams sweeps round the hero. */
        BEAM_RING,
        /** A nova of fire: everything near burns. */
        FIRE,
        /** Everything near is revealed (it glows through walls) and struck by a golden wave. */
        REVEAL,
        /** Blasts go off on everything near (no block is broken). */
        BLAST
    }

    /** A status effect a form keeps up while it lasts. */
    public record Effect(Holder<MobEffect> effect, int amplifier) {
    }

    private static Effect fx(Holder<MobEffect> effect, int amplifier) {
        return new Effect(effect, amplifier);
    }

    public final Power power;
    public final int color;
    public final int glow;
    public final Aura aura;
    public final Signature signature;
    public final List<Effect> effects;

    UltimateForm(Power power, int color, int glow, Aura aura, Signature signature, Effect... effects) {
        this.power = power;
        this.color = color;
        this.glow = glow;
        this.aura = aura;
        this.signature = signature;
        this.effects = List.of(effects);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The form's name ("The Demiurge", "Dark Phoenix"...). */
    public Component displayName() {
        return Component.translatable("ultimate.enhanced." + id());
    }

    /** One line about it. */
    public Component description() {
        return Component.translatable("ultimate.enhanced." + id() + ".desc");
    }

    /** The name of its signature move. */
    public Component signatureName() {
        return Component.translatable("ultimate.enhanced." + id() + ".signature");
    }

    /** The form of a player: his hero's, a witch's {@link #WITCH}, or null (no power and no witchcraft). */
    @Nullable
    public static UltimateForm of(net.minecraft.world.entity.player.Player player) {
        Power power = com.kolodkingal.enhanced.power.Powers.power(player);
        if (power == Power.NONE) {
            return com.kolodkingal.enhanced.magic.Magic.isWitch(player) ? WITCH : null;
        }
        return of(power);
    }

    /** The form's colour on this player (a witch's is her path's). */
    public int color(net.minecraft.world.entity.player.Player player) {
        com.kolodkingal.enhanced.magic.WitchPath path = this == WITCH ? com.kolodkingal.enhanced.magic.Magic.path(player) : null;
        return path != null ? path.color : color;
    }

    /** The form's bright colour on this player. */
    public int glow(net.minecraft.world.entity.player.Player player) {
        com.kolodkingal.enhanced.magic.WitchPath path = this == WITCH ? com.kolodkingal.enhanced.magic.Magic.path(player) : null;
        return path != null ? path.glow : glow;
    }

    /** The form of a hero, or null for none ({@link Power#NONE}: the witch's is {@link #WITCH}, asked for by name). */
    @Nullable
    public static UltimateForm of(Power power) {
        if (power == Power.NONE) {
            return null;
        }
        for (UltimateForm form : values()) {
            if (form.power == power) {
                return form;
            }
        }
        return null;
    }
}
