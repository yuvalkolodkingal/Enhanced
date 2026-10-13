package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.power.Power;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Build Magic on the server (D-B 11): the entry points the two build payloads call ({@code BuildCastPayload},
 * {@code BuildActionPayload}), Billy's meter, ranks and Mom's help, and the messages. It validates every cast (D-B 10.2),
 * plans it again from its recipe, answers a preflight, and hands the spell to {@link BuildScheduler}.
 *
 * <p>A stub of the skeleton (3.1, step S): every method does nothing yet, so nothing a client sends changes anything.
 * Lane BS fills it (D-B 11.3, 11.9, 12).
 */
public final class BuildMagic {
    /**
     * A cast arrived: {@code recipe} as the client sent it (to be {@linkplain SpellRecipe#clamped clamped} first),
     * {@code token} to answer with, {@code flags} the cast's ({@code BuildCastPayload.PREFLIGHT}, {@code HELD_SHORT},
     * {@code HELD_LONG}). Lane BS: validate, preflight or queue, answer a {@code BuildVerdictPayload}.
     */
    public static void cast(ServerPlayer player, SpellRecipe recipe, int token, int flags) {
    }

    /**
     * One of the builder's actions arrived ({@code BuildActionPayload}'s {@code MODE}, {@code UNDO}, {@code REDO},
     * {@code PICK}, {@code PICK_BLOCK}, {@code HAND}, {@code STROKE_END}, {@code VIEW}, {@code POCKET}), with its box
     * {@code a}-{@code b} and its number. Lane BS: UNDO and REDO need no build mode (R42).
     */
    public static void action(ServerPlayer player, int action, BlockPos a, BlockPos b, int value) {
    }

    /**
     * The player's power changed from {@code from} to {@code to} ({@code Powers.setPower}): build mode goes off, the spell
     * at work finishes and the waiting ones are dropped; the history, the pocket and the meter are kept (D-B 11.9). Lane BS
     * calls it from {@code Powers.setPower}.
     */
    public static void onPowerChanged(ServerPlayer player, Power from, Power to) {
    }

    /** Forgets every builder's live state (the sessions, the rate buckets): for the server's stop and for tests. */
    public static void clear() {
    }

    private BuildMagic() {
    }
}
