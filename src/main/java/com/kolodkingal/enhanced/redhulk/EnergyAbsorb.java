package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

import java.util.List;

/**
 * Sneak + B, held: the Energy Absorb (3.8, PLAN 4.4). He plants his feet and red light pulls into him for as long as
 * the key is held (at most {@code absorbMaxTicks}): fire, lightning, explosions and beams ({@code #minecraft:is_fire},
 * {@code is_lightning}, {@code is_explosion}, the mod's {@code #enhanced:energy_damage}) do him no harm and feed his
 * anger instead, {@code absorbAngerPerHealth} for each health they would have taken. A blow of a fist or a blade still
 * hurts. The cooldown begins when he lets go. {@code Live.RED_HULK_ABSORBING} while it lasts; {@link
 * RedHulkState#absorbUntil()} says until when (synced: the HUD's bar).
 */
public final class EnergyAbsorb {
    private EnergyAbsorb() {
    }

    /** Whether this damage is the kind he soaks up. */
    public static boolean absorbs(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_LIGHTNING)
                || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(ModTags.ENERGY_DAMAGE);
    }

    /** Whether he is absorbing now (server). */
    public static boolean active(ServerPlayer player) {
        RedHulkAbilities.State st = RedHulkAbilities.peek(player.getUUID());
        return st != null && st.absorbStart >= 0L;
    }

    /** Sneak + B pressed: he begins to absorb. */
    public static boolean start(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.RED_HULK_ABSORB, now)) {
            return false;
        }
        RedHulkAbilities.State st = RedHulkAbilities.state(player.getUUID());
        st.absorbStart = now;
        int max = RedHulkAbilities.cfg(RedHulkConfig.ABSORB_MAX_TICKS, 80);
        Anger.set(player, Anger.get(player).withAbsorbUntil(now + max));
        data.set(Live.RED_HULK_ABSORBING, true);
        RedHulkAbilities.send(level, player, RedHulkFxPayload.ABSORB_ON, List.of(player.position()), max);
        return true;
    }

    /** Every tick while he absorbs: it ends at its time. */
    static void tick(ServerPlayer player, PowerData data, RedHulkAbilities.State st, long now) {
        if (!RedHulkAbilities.hasBody(data)
                || now - st.absorbStart >= RedHulkAbilities.cfg(RedHulkConfig.ABSORB_MAX_TICKS, 80)) {
            stop(player, data, st);
        }
    }

    /** The key let go (or the time is up): it ends and the cooldown begins. */
    static void stop(ServerPlayer player, PowerData data, RedHulkAbilities.State st) {
        if (st.absorbStart < 0L) {
            return;
        }
        st.absorbStart = -1L;
        long now = player.level().getGameTime();
        Anger.set(player, Anger.get(player).withAbsorbUntil(0L));
        data.set(Live.RED_HULK_ABSORBING, false);
        data.startCooldown(HeroAbility.RED_HULK_ABSORB, now, RedHulkAbilities.cfg(RedHulkConfig.ABSORB_COOLDOWN,
                200));
        RedHulkAbilities.send(player.serverLevel(), player, RedHulkFxPayload.ABSORB_OFF, List.of(player.position()),
                0);
    }

    /** It took {@code amount} of what he soaks up: no harm, and anger. */
    static void soak(ServerPlayer player, PowerData data, float amount) {
        Anger.add(player, data, amount * (float) RedHulkAbilities.cfg(RedHulkConfig.ABSORB_ANGER_PER_HEALTH, 2.0),
                true);
        RedHulkAbilities.send(player.serverLevel(), player, RedHulkFxPayload.ABSORB_HIT,
                List.of(player.position().add(0, player.getBbHeight() * 0.6, 0)), Math.round(amount));
    }
}
