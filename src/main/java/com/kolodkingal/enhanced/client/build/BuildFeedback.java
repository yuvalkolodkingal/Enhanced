package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.build.BuildGrammar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * What the builder alone hears while choosing and shaping (D-B 6.8): vanilla events played straight on this client,
 * never sent, never heard by anyone else. The spell's own sounds (the cast, the hum, the fizzle, the undo) are the
 * server's ({@code BuildSounds}).
 */
final class BuildFeedback {
    /** A corner, a planted shape: a chime. */
    private static final float CHIME = 0.6F;
    /** A face pushed, a size or a style changed: a soft click, higher as the thing grows. */
    private static final float CLICK = 0.15F;
    /** The biggest number the click's pitch still rises for. */
    private static final float CLICK_TOP = 32.0F;

    /** The sound of one input's result. */
    static void heard(BuildGrammar.Result result, BuildGrammar grammar) {
        switch (result) {
            case CORNER_A, PLANTED -> play(SoundEvents.AMETHYST_BLOCK_CHIME, CHIME, 1.0F);
            case CORNER_B, DRAGGED, FIXED -> play(SoundEvents.AMETHYST_BLOCK_CHIME, CHIME, 1.4F);
            case SELECTED, ADDED -> play(SoundEvents.AMETHYST_BLOCK_CHIME, CHIME, 1.2F);
            case LET_GO -> unwound();
            case PUSHED, NUMBER, STYLE -> play(SoundEvents.UI_BUTTON_CLICK.value(), CLICK, 0.8F + 0.8F * Math.min(1.0F, size(grammar) / CLICK_TOP));
            // Billy's longest side (or the edge of the world) holds it: a soft "tock".
            case HELD_BACK -> play(SoundEvents.NOTE_BLOCK_HAT.value(), 0.3F, 0.6F);
            default -> {
            }
        }
    }

    /** The hold ring fills: the beacon's hum, rising as it fills, every 5 ticks. */
    static void holding(int held, int of) {
        if (held % 5 == 0) {
            play(SoundEvents.BEACON_AMBIENT, 0.5F, 0.8F + 0.8F * held / Math.max(1.0F, of));
        }
    }

    /** Let go: the ring before it was full, or the box on the sky. */
    static void unwound() {
        play(SoundEvents.BEACON_DEACTIVATE, 0.4F, 1.0F);
    }

    /** Copied into the pocket, or picked up to move. */
    static void copied() {
        play(SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.6F, 1.3F);
    }

    /** What the click's pitch follows: the box's longest side, or the number the wheel turns. */
    private static int size(BuildGrammar grammar) {
        BoundingBox box = grammar.box();
        if (box != null) {
            return Math.max(box.getXSpan(), Math.max(box.getYSpan(), box.getZSpan()));
        }
        return switch (grammar.spell()) {
            case SHAPE -> grammar.radius() * 2;
            case REPEAT -> grammar.copies();
            case PULL -> Math.abs(grammar.layers());
            case PAINT, LAND, GROW -> grammar.brush() * 4;
            case COPY, MOVE -> Math.abs(grammar.lift()) + grammar.turn() * 8;
            default -> grammar.style() * 6;
        };
    }

    private static void play(SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private BuildFeedback() {
    }
}
