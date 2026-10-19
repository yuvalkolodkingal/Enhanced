package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKeys;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Doctor Strange, the Sorcerer Supreme (3.6): a powerless player who held the Eye of Agamotto ({@link EyeOfAgamotto}).
 * He flies on his Cloak of Levitation (G, the shared flight of every flier) and casts:
 * <ul>
 * <li><b>R</b> the Eldritch Whip, a combo of three lashes; Sneak + R the Whip Grapple ({@link EldritchWhip}).</li>
 * <li><b>V</b> held, the Shield of the Seraphim; Sneak + V the Seraphim Dome ({@link SeraphimShield}).</li>
 * <li><b>B</b> the Crimson Bands of Cyttorak ({@link CrimsonBands}); Sneak + B the Astral Projection ({@link
 * AstralProjection}).</li>
 * <li><b>N</b> a pair of Sling-Ring portals ({@link SlingPortals}); Sneak + N the Mirror Dimension ({@link
 * MirrorDimension}).</li>
 * <li><b>Y</b> the Eye of Agamotto, while it is with him: Slow Time; Sneak + Y Rewind ({@link EyeTime}).</li>
 * </ul>
 * Every ability is a {@link HeroAbility} row (3.4): a custom hero who picks one has it on his own key, at his strength
 * (cooldowns by {@link PowerData#startCooldown(HeroAbility, long, int)}, damage by {@code HeroDamage}, the body by {@link
 * PowerData#bodyScale}). The plug-in is {@link StrangeServer}; his events are {@link StrangeEvents}.
 */
public final class StrangeAbilities {
    // ---- his colours: the orange-gold sparks of the mystic arts, the Crimson Bands, the Eye's green ----
    public static final int ORANGE = 0xF29A2E;
    public static final int GOLD = 0xFFE0A8;
    public static final int CRIMSON = 0xD8283A;
    public static final int TIME_GREEN = 0x35D46A;
    public static final int ASTRAL_BLUE = 0xA8DCFF;

    private static final ResourceLocation BODY_HEALTH = Enhanced.id("strange_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("strange_armor");
    /** What his body takes off the hunger of a spell (the mystic arts are learned, not drawn from the body). */
    public static final float BODY_HUNGER = 0.8F;

    // ------------------------------------------------------------------------------------------------------------
    // Config (read when asked; the defaults where it is not loaded, as in a pure test)

    static double cfg(Supplier<Double> read, double fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? read.get() : fallback;
    }

    static int cfgInt(Supplier<Integer> read, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? read.get() : fallback;
    }

    static boolean cfgBool(Supplier<Boolean> read, boolean fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? read.get() : fallback;
    }

    public static double healthBonus() {
        return cfg(() -> StrangeConfig.HEALTH_BONUS.get(), 6.0);
    }

    public static double armorBonus() {
        return cfg(() -> StrangeConfig.ARMOR_BONUS.get(), 2.0);
    }

    /** Falls shorter than this (blocks) do him no harm: the Cloak catches him. */
    public static double safeFall() {
        return cfg(() -> StrangeConfig.SAFE_FALL.get(), 12.0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Queries

    /** Whether this entity is a player with this ability of Doctor Strange's (the hero, or a custom hero who picked it). */
    public static boolean has(@Nullable Entity e, HeroAbility ability) {
        return e instanceof Player player && Powers.get(player).has(ability);
    }

    /** Whether a spell can be cast: he is fed (a spell costs hunger), or cannot starve. */
    static boolean fed(Player player) {
        return player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0;
    }

    /** A spell refused for hunger: told, and nothing spent. */
    static boolean refuseHungry(ServerPlayer player) {
        if (fed(player)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("message.enhanced.strange.hungry").withStyle(ChatFormatting.GRAY),
                true);
        return true;
    }

    /** A spell's hunger: as the Scarlet Witch's scale, a fifth less with his body. Creative casts for nothing. */
    static void spend(ServerPlayer player, PowerData data, float exhaustion) {
        if (player.getAbilities().invulnerable) {
            return;
        }
        player.causeFoodExhaustion(data.has(HeroAbility.STRANGE_BODY) ? exhaustion * BODY_HUNGER : exhaustion);
    }

    static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                volume, pitch);
    }

    static void say(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input

    /**
     * A key of his (3.4: also one of a custom hero's bindings, with the key the ability has for him; 3.6: key {@link
     * HeroKeys#Y} is the Eye). {@code alt}: Sneak's variant.
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down) {
            if (ability == 3) {
                SeraphimShield.release(player, data);
            }
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    EldritchWhip.grapple(player, data);
                } else {
                    EldritchWhip.lash(player, data);
                }
            }
            case 3 -> {
                if (alt) {
                    SeraphimShield.dome(player, data);
                } else {
                    SeraphimShield.raise(player, data);
                }
            }
            case 4 -> {
                if (alt) {
                    AstralProjection.toggle(player, data);
                } else {
                    CrimsonBands.cast(player, data);
                }
            }
            case 5 -> {
                if (alt) {
                    MirrorDimension.toggle(player, data);
                } else {
                    SlingPortals.openByKey(player, data);
                }
            }
            case HeroKeys.Y -> {
                if (alt) {
                    EyeTime.rewind(player, data, HeroAbility.STRANGE_EYE, true);
                } else {
                    EyeTime.toggleSlow(player, data, HeroAbility.STRANGE_EYE, true);
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ticks, the body, the end

    /** His tick, or (3.4) a custom hero's with any of his abilities: each part only for the ability it belongs to. */
    static void tick(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        EldritchWhip.tick(player, data, now);
        SeraphimShield.tick(player, data, now);
        AstralProjection.tick(player, data, now);
        MirrorDimension.tick(player, data, now);
        EyeTime.tick(player, data, now);
    }

    /**
     * His body (every player, whenever the passives are applied): health and armour while he has it, at his strength
     * for a custom hero; none of it otherwise. The Cloak's soft landing is {@link StrangeEvents}'.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean body = data.has(HeroAbility.STRANGE_BODY);
        double scale = body ? data.bodyScale(HeroAbility.STRANGE_BODY) : 0.0;
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, healthBonus() * scale, body);
        setModifier(player, Attributes.ARMOR, BODY_ARMOR, armorBonus() * scale, body);
        if (!body && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** He loses the power (or a custom hero with his abilities redefines, dies or leaves): every spell of his ends. */
    static void endAll(ServerPlayer player, PowerData data) {
        AstralProjection.end(player, data, 0.0F, null);
        SeraphimShield.endAll(player, data);
        MirrorDimension.close(player, data, false);
        SlingPortals.closeOwnedBy(player.server, player.getUUID());
        EldritchWhip.forget(player.getUUID());
        EyeTime.endAll(player, data);
    }

    /** Gone from this world for a moment (death, another dimension, logout): what holds him here ends. */
    static void onGone(ServerPlayer player) {
        PowerData data = Powers.get(player);
        AstralProjection.end(player, data, 0.0F, null);
        SeraphimShield.endAll(player, data);
        MirrorDimension.close(player, data, false);
        SlingPortals.closeOwnedBy(player.server, player.getUUID());
        EldritchWhip.forget(player.getUUID());
        EyeTime.endAll(player, data);
    }

    /** The held keys are let go of (death, the power lost, a screen opened): the Shield of the Seraphim comes down. */
    static void stopHold(PowerData data) {
        data.set(com.kolodkingal.enhanced.power.Live.STRANGE_SHIELD, false);
    }

    static void clear() {
        EldritchWhip.clear();
        SeraphimShield.clear();
        CrimsonBands.clear();
        AstralProjection.clear();
        MirrorDimension.clear();
        SlingPortals.clear();
        EyeTime.clear();
    }

    /** Puts an attribute modifier on (with this amount) or takes it off. */
    static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount,
                            boolean present) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    /** The id of a player, or null. */
    @Nullable
    static UUID id(@Nullable Entity e) {
        return e == null ? null : e.getUUID();
    }

    private StrangeAbilities() {
    }
}
