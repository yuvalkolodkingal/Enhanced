package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * The Hulk's forms (3.6): one hero, two states. The looks {@code banner} (Bruce Banner) and {@code own} (the player's
 * own skin as Banner) are human (no powers at all); {@code ragnarok} and {@code brand_new_day} are the two Hulks. A
 * custom hero with {@code hulk.transform} is human while his {@link Toggle#HUMAN_FORM} is on.
 *
 * <p>Every change of look goes through {@link #change}, so every path plays the same transformation: N, Sneak + N, the
 * Change Look key (a step across the human/Hulk line is a transformation too: {@link #tick} sees it), anger, the Gamma
 * Emitter's accident. Banner Hulks out over {@code transformTicks} (he hunches, clenches, grows, roars) and the Hulk
 * calms down over as many (he kneels and shrinks); the two Hulks trade places in a green shimmer of 20 ticks. A change
 * to a bigger look needs the grown body's room free of blocks, before and all the way through: he never suffocates in a
 * wall.
 */
public final class HulkForms {
    /** The looks of the Hulk that are human. */
    public static final Set<String> HUMAN_LOOKS = Set.of("banner", "own");
    /** The ticks Ragnarok and Brand New Day take to trade places. */
    public static final int SWITCH_TICKS = 20;

    /** A change under way: from one look to another, from game time {@code start}, over {@code ticks}. */
    record Change(String from, String to, long start, int ticks) {
        /** How far it is (0 to 1) at game time {@code now}. */
        float progress(long now) {
            return ticks <= 0 ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float) (now - start) / ticks));
        }

        /** The body's scale bonus at game time {@code now} (a smooth step from the old look's to the new one's). */
        double scale(long now) {
            float p = progress(now);
            double eased = p * p * (3.0 - 2.0 * p);
            return HulkAbilities.scaleOf(from) + (HulkAbilities.scaleOf(to) - HulkAbilities.scaleOf(from)) * eased;
        }

        boolean grows() {
            return HulkAbilities.scaleOf(to) > HulkAbilities.scaleOf(from);
        }
    }

    /** Whether the player is the Hulk (or a custom hero with his Transform) in a human form right now. Both sides. */
    public static boolean isHuman(Player player) {
        return isHuman(Powers.get(player));
    }

    /** Whether this power data is a human form of the Hulk's. */
    public static boolean isHuman(PowerData data) {
        if (data.power() == Power.HULK) {
            return HUMAN_LOOKS.contains(data.variant());
        }
        return data.power() == Power.CUSTOM && data.toggle(Toggle.HUMAN_FORM);
    }

    /** Whether the Hulk is changing look right now (server). */
    public static boolean changing(Player player) {
        HulkAbilities.State state = HulkAbilities.peek(player.getUUID());
        return state != null && state.change != null;
    }

    /**
     * Changes the Hulk's look to {@code to}, with the transformation: a human into a Hulk grows (his new look at once,
     * so the green giant grows out of him), a Hulk into a human shrinks (his look changes when he has shrunk), a Hulk
     * into the other trades places. Refused, with "No room to Hulk out here.", when the bigger body would not fit.
     *
     * @return whether the change began
     */
    public static boolean change(ServerPlayer player, String to) {
        return change(player, to, true);
    }

    /** {@link #change(ServerPlayer, String)}, telling the player when there is no room only if {@code tell}. */
    static boolean change(ServerPlayer player, String to, boolean tell) {
        PowerData data = Powers.get(player);
        if (data.power() != Power.HULK || !Variants.get(Power.HULK, to).id().equals(to)) {
            return false;
        }
        HulkAbilities.State state = HulkAbilities.state(player.getUUID());
        String from = HulkAbilities.look(data);
        if (state.change != null || from.equals(to)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        boolean human = HUMAN_LOOKS.contains(to);
        boolean wasHuman = HUMAN_LOOKS.contains(from);
        if (human && wasHuman) {
            Powers.setVariant(player, Variants.get(Power.HULK, to));
            state.settled = to;
            return true;
        }
        int ticks = human != wasHuman ? HulkAbilities.cfg(HulkConfig.TRANSFORM_TICKS, 40) : SWITCH_TICKS;
        Change change = new Change(from, to, now, ticks);
        if (change.grows() && !roomFor(player, HulkAbilities.scaleOf(to))) {
            if (tell) {
                player.displayClientMessage(Component.translatable("message.enhanced.hulk.no_room")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return false;
        }
        if (state.move != null) {
            HulkAbilities.Move move = state.move;
            state.move = null;
            move.end(player, data);
        }
        state.settled = from;
        state.change = change;
        data.set(Live.HULK_TRANSFORMING, true);
        if (!human) {
            Powers.setVariant(player, Variants.get(Power.HULK, to));
            player.setData(ModAttachments.HULK, HulkRage.get(player).withLastHulk(to));
        } else {
            HulkRage.calm(player);
        }
        Vec3 at = player.position();
        int kind = wasHuman ? HulkFxPayload.TRANSFORM_OUT : human ? HulkFxPayload.TRANSFORM_IN : HulkFxPayload.SWITCH;
        HulkAbilities.send(level, player, kind, List.of(at), ticks);
        if (kind == HulkFxPayload.TRANSFORM_OUT) {
            HulkAbilities.sound(level, at, ModSounds.HULK_TRANSFORM_OUT.get(), 1.6F, 1.0F);
        } else if (kind == HulkFxPayload.TRANSFORM_IN) {
            HulkAbilities.sound(level, at, ModSounds.HULK_TRANSFORM_IN.get(), 1.2F, 1.0F);
        } else {
            HulkAbilities.sound(level, at, ModSounds.HULK_SWITCH.get(), 1.4F, 1.0F);
        }
        Powers.syncIfDirty(player);
        return true;
    }

    /** Whether a body of this scale bonus fits where the player stands (no block, no solid entity in it). */
    public static boolean roomFor(Player player, double scaleBonus) {
        double scale = 1.0 + scaleBonus;
        double half = 0.3 * scale;
        Vec3 at = player.position();
        AABB box = new AABB(at.x - half, at.y, at.z - half, at.x + half, at.y + 1.8 * scale, at.z + half)
                .deflate(1.0E-3);
        return player.level().noCollision(player, box);
    }

    /**
     * Every tick of the Hulk: a change under way grows or shrinks him (and stops if a block is in the way: back to what
     * he was), ends when its time is up; a look that changed by itself (the Change Look key, a command) across the
     * human/Hulk line, or between the two Hulks, is taken through the transformation.
     */
    static void tick(ServerPlayer player, PowerData data, HulkAbilities.State state, long now) {
        String current = HulkAbilities.look(data);
        if (state.settled == null) {
            state.settled = current;
        }
        Change change = state.change;
        if (change != null) {
            if (change.grows() && now < change.start() + change.ticks() && !roomFor(player, change.scale(now + 1))) {
                // A block in the way: the change stops, he is what he was.
                state.change = null;
                data.set(Live.HULK_TRANSFORMING, false);
                Powers.setVariant(player, Variants.get(Power.HULK, change.from()));
                state.settled = change.from();
                player.displayClientMessage(Component.translatable("message.enhanced.hulk.no_room")
                        .withStyle(ChatFormatting.GRAY), true);
                return;
            }
            if (now >= change.start() + change.ticks()) {
                finish(player, data, state);
            }
            return;
        }
        data.set(Live.HULK_TRANSFORMING, false);
        if (current.equals(state.settled)) {
            return;
        }
        // The look changed without us (the Change Look key, a command, the creator): act it out.
        String from = state.settled;
        if (HUMAN_LOOKS.contains(from) && HUMAN_LOOKS.contains(current)) {
            state.settled = current;
            return;
        }
        Powers.setVariant(player, Variants.get(Power.HULK, from));
        if (!change(player, current)) {
            state.settled = HulkAbilities.look(data);
        }
    }

    /** The change under way ends at once: he is his new look. */
    static void finish(ServerPlayer player, PowerData data, HulkAbilities.State state) {
        Change change = state.change;
        state.change = null;
        data.set(Live.HULK_TRANSFORMING, false);
        if (change == null || data.power() != Power.HULK) {
            return;
        }
        Powers.setVariant(player, Variants.get(Power.HULK, change.to()));
        state.settled = change.to();
        HulkAbilities.applyBody(player, data); // (his new body at once, not a tick later)
        if (!HUMAN_LOOKS.contains(change.to()) && HUMAN_LOOKS.contains(change.from())) {
            HulkAbilities.sound(player.serverLevel(), player.position(), ModSounds.HULK_ROAR.get(), 1.5F, 1.0F);
        }
    }

    /**
     * The Gamma Emitter's accident: a powerless player has just become the Hulk as Banner, and the first change comes
     * at once (into Ragnarok).
     */
    public static boolean accident(ServerPlayer player) {
        HulkAbilities.State state = HulkAbilities.state(player.getUUID());
        state.settled = HulkAbilities.look(Powers.get(player));
        return change(player, HulkState.FIRST_HULK);
    }

    private HulkForms() {
    }
}
