package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.custom.CustomHeroServer;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hulk.HulkForms;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * N: Red Hulk and Thaddeus Ross (3.8, PLAN 4.4). The two forms are one toggle ({@link Toggle#ROSS_FORM}): Ross is the
 * player's own skin in General Ross's suit, with no body and no keys but N; Red Hulk is the 1.5-times giant. Ross grows
 * into Red Hulk over {@code transformTicks} (his look at once, so the red giant grows out of him) and Red Hulk shrinks
 * back over as many. A growth needs the grown body's room free of blocks before and all the way through ({@code
 * HulkForms.roomFor}, the Hulk's rule): "No room for Red Hulk here." and he stays (or becomes again) Ross. Ross's anger
 * at 100 turns him into Red Hulk ({@link Anger}); a new Red Hulk grows into his size the same way ({@link #awaken}).
 *
 * <p>A custom hero with {@code red_hulk.transform} has the Human Form instead, as with {@code hulk.transform}: every
 * power off until N again (both toggles, so his rows say so and his own skin shows).
 */
public final class RossForm {
    /**
     * A change under way: into Red Hulk ({@code toRed}) or back into Ross, from game time {@code start}, over {@code
     * ticks}.
     */
    record Change(boolean toRed, long start, int ticks) {
        /** How far it is (0 to 1) at game time {@code now}. */
        float progress(long now) {
            return ticks <= 0 ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float) (now - start) / ticks));
        }

        /** The body's scale bonus at {@code now}: a smooth step between Ross's (0) and Red Hulk's ({@code full}). */
        double scale(long now, double full) {
            float p = progress(now);
            double eased = p * p * (3.0 - 2.0 * p);
            return toRed ? full * eased : full * (1.0 - eased);
        }
    }

    /** Whether the player is changing form right now (server). */
    public static boolean changing(ServerPlayer player) {
        RedHulkAbilities.State st = RedHulkAbilities.peek(player.getUUID());
        return st != null && st.change != null;
    }

    /** N (the hero himself): Ross grows into Red Hulk, Red Hulk shrinks into Ross. */
    public static boolean transform(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        RedHulkAbilities.State st = RedHulkAbilities.state(player.getUUID());
        if (st.change != null || !data.isReady(HeroAbility.RED_HULK_TRANSFORM, now)) {
            return false;
        }
        boolean began = RedHulkAbilities.isRoss(data) ? grow(player, data, st, true) : shrink(player, data, st);
        if (began) {
            data.startCooldown(HeroAbility.RED_HULK_TRANSFORM, now, RedHulkAbilities.TRANSFORM_COOLDOWN);
        }
        return began;
    }

    /** He has Red Hulk's body but not his size yet (he has just become him): he grows into it, or stays Ross. */
    static void awaken(ServerPlayer player, PowerData data, RedHulkAbilities.State st) {
        if (!grow(player, data, st, false)) {
            if (data.power() == Power.CUSTOM) {
                data.setToggle(Toggle.HUMAN_FORM, true);
            }
            data.setToggle(Toggle.ROSS_FORM, true);
            player.displayClientMessage(Component.translatable("message.enhanced.red_hulk.no_room")
                    .withStyle(ChatFormatting.GRAY), true);
            Powers.syncIfDirty(player);
        }
    }

    /**
     * Ross grows into Red Hulk, if the grown body fits where he stands; {@code tell}: say so when it does not.
     *
     * @return whether the growth began
     */
    static boolean grow(ServerPlayer player, PowerData data, RedHulkAbilities.State st, boolean tell) {
        if (!HulkForms.roomFor(player, RedHulkAbilities.scaleBonus(data))) {
            if (tell) {
                player.displayClientMessage(Component.translatable("message.enhanced.red_hulk.no_room")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        ServerLevel level = player.serverLevel();
        int ticks = RedHulkAbilities.cfg(RedHulkConfig.TRANSFORM_TICKS, 30);
        st.change = new Change(true, level.getGameTime(), ticks);
        data.setToggle(Toggle.ROSS_FORM, false);
        data.set(Live.RED_HULK_TRANSFORMING, true);
        Vec3 at = player.position();
        RedHulkAbilities.send(level, player, RedHulkFxPayload.TRANSFORM_OUT, List.of(at), ticks);
        RedHulkAbilities.sound(level, at, ModSounds.RED_HULK_TRANSFORM.get(), 1.6F, 1.0F);
        Powers.syncIfDirty(player);
        return true;
    }

    /** Red Hulk shrinks back into Ross (always fits: he gets smaller). */
    static boolean shrink(ServerPlayer player, PowerData data, RedHulkAbilities.State st) {
        ServerLevel level = player.serverLevel();
        int ticks = RedHulkAbilities.cfg(RedHulkConfig.TRANSFORM_TICKS, 30);
        st.change = new Change(false, level.getGameTime(), ticks);
        data.set(Live.RED_HULK_TRANSFORMING, true);
        Vec3 at = player.position();
        RedHulkAbilities.send(level, player, RedHulkFxPayload.TRANSFORM_IN, List.of(at), ticks);
        RedHulkAbilities.sound(level, at, ModSounds.RED_HULK_TRANSFORM.get(), 1.2F, 0.85F);
        Powers.syncIfDirty(player);
        return true;
    }

    /**
     * Every tick: a growth under way stops if a block is in the way of the next tick's body (he is Ross again), and
     * the change ends when its time is up.
     */
    static void tick(ServerPlayer player, PowerData data, RedHulkAbilities.State st, long now) {
        Change change = st.change;
        if (change == null) {
            return;
        }
        if (!data.has(HeroAbility.RED_HULK_BODY) && data.power() != Power.RED_HULK) {
            st.change = null;
            return;
        }
        if (change.toRed() && now < change.start() + change.ticks()
                && !HulkForms.roomFor(player, change.scale(now + 1, RedHulkAbilities.scaleBonus(data)))) {
            st.change = null;
            data.setToggle(Toggle.ROSS_FORM, true);
            if (data.power() == Power.CUSTOM) {
                data.setToggle(Toggle.HUMAN_FORM, true);
            }
            data.set(Live.RED_HULK_TRANSFORMING, false);
            RedHulkAbilities.applyBody(player, data);
            player.displayClientMessage(Component.translatable("message.enhanced.red_hulk.no_room")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (now >= change.start() + change.ticks()) {
            finish(player, data, st);
        }
    }

    /** The change under way, if any, ends at once (a test's, a command's): he is his new form. */
    public static void settle(ServerPlayer player) {
        RedHulkAbilities.State st = RedHulkAbilities.peek(player.getUUID());
        if (st != null && st.change != null) {
            finish(player, Powers.get(player), st);
        }
    }

    /** The change under way ends at once: he is his new form. */
    static void finish(ServerPlayer player, PowerData data, RedHulkAbilities.State st) {
        Change change = st.change;
        st.change = null;
        data.set(Live.RED_HULK_TRANSFORMING, false);
        if (change == null) {
            return;
        }
        if (!change.toRed()) {
            data.setToggle(Toggle.ROSS_FORM, true);
        }
        st.sized = change.toRed();
        RedHulkAbilities.applyBody(player, data); // (his new body at once, not a tick later)
        if (change.toRed() && player.isAlive()) {
            RedHulkAbilities.sound(player.serverLevel(), player.position(), ModSounds.RED_HULK_ROAR.get(), 1.5F,
                    1.0F);
        }
        Powers.syncIfDirty(player);
    }

    /**
     * A custom hero's N: his Human Form (Ross for a custom Red Hulk). Every power of his ends and stays off (his rows
     * answer no, his modules give nothing, his body is gone, his own skin shows) until N again.
     */
    public static void humanForm(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.RED_HULK_TRANSFORM, now)) {
            return;
        }
        boolean human = !data.toggle(Toggle.HUMAN_FORM);
        ServerLevel level = player.serverLevel();
        if (human) {
            CustomHeroServer.endPowers(player, data);
            data.setToggle(Toggle.HUMAN_FORM, true);
            data.setToggle(Toggle.ROSS_FORM, true);
            RedHulkAbilities.send(level, player, RedHulkFxPayload.TRANSFORM_IN, List.of(player.position()), 0);
        } else {
            data.setToggle(Toggle.HUMAN_FORM, false);
            data.setToggle(Toggle.ROSS_FORM, false);
            RedHulkAbilities.send(level, player, RedHulkFxPayload.TRANSFORM_OUT, List.of(player.position()), 0);
        }
        RedHulkAbilities.sound(level, player.position(), ModSounds.RED_HULK_TRANSFORM.get(), 1.0F, 1.0F);
        data.startCooldown(HeroAbility.RED_HULK_TRANSFORM, now, RedHulkAbilities.TRANSFORM_COOLDOWN);
        CustomHeroes.syncState(player);
    }

    private RossForm() {
    }
}
