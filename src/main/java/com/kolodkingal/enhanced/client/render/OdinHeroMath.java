package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.OdinConfig;
import com.kolodkingal.enhanced.odin.OdinAbilities;
import org.jetbrains.annotations.Nullable;

/**
 * The pure numbers of Odin's animation (3.5), with no client class so that the game tests can check them: which use of a
 * key a cooldown stands for (two uses of one key start cooldowns of different lengths: the one nearest to what the client
 * sees is the one that was used, whatever the server's config), and the shapes his effects are drawn with.
 */
public final class OdinHeroMath {

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    private static int blast() {
        return loaded() ? OdinConfig.BLAST_COOLDOWN.get() : 25;
    }

    private static int throwing() {
        return loaded() ? OdinConfig.THROW_COOLDOWN.get() : 60;
    }

    private static int shield() {
        return loaded() ? OdinConfig.SHIELD_COOLDOWN.get() : 200;
    }

    private static int heal() {
        return loaded() ? OdinConfig.HEAL_COOLDOWN.get() : 600;
    }

    private static int banish() {
        return loaded() ? OdinConfig.BANISH_COOLDOWN.get() : 400;
    }

    private static int wave() {
        return loaded() ? OdinConfig.WAVE_COOLDOWN.get() : 220;
    }

    private static int command() {
        return loaded() ? OdinConfig.COMMAND_COOLDOWN.get() : 1200;
    }

    private static int odinsleep() {
        return loaded() ? OdinConfig.ODINSLEEP_COOLDOWN.get() : 1800;
    }

    /** Whether {@code left} is nearer to {@code a} than to {@code b} (a tie goes to {@code a}). */
    static boolean nearer(long left, int a, int b) {
        return Math.abs(left - a) <= Math.abs(left - b);
    }

    /**
     * The gesture of a cooldown of Odin's that starts with {@code left} ticks, or null when another route acts it: R's is the
     * blast's or a new Gungnir's coming to his hand (the throw is acted by its state); G's the Heal's (the shield's own rest is
     * acted by its flag); V's Banish's or the Wave's; N's the Command's (the Odinsleep's rest after he wakes is its toggle's).
     */
    @Nullable
    public static GestureId cooldownGesture(int ability, long left) {
        return switch (ability) {
            case 1 -> {
                int b = blast();
                int t = throwing();
                int r = OdinAbilities.RECALL_COOLDOWN;
                if (nearer(left, b, t) && nearer(left, b, r)) {
                    yield GestureId.ODIN_BLAST;
                }
                yield nearer(left, r, t) ? GestureId.ODIN_CATCH : null;
            }
            case 2 -> {
                int s = shield();
                int broken = (int) Math.round(s * OdinAbilities.SHIELD_BROKEN_FACTOR);
                int h = heal();
                yield nearer(left, h, s) && nearer(left, h, broken) ? GestureId.ODIN_HEAL : null;
            }
            case 3 -> nearer(left, banish(), wave()) ? GestureId.ODIN_BANISH : GestureId.ODIN_WAVE;
            case 5 -> nearer(left, command(), odinsleep()) ? GestureId.ODIN_COMMAND : null;
            default -> null;
        };
    }

    /**
     * The radius (blocks) of the golden cocoon of the Odinsleep at {@code t} ticks after it began: it closes over him in its
     * first second and then breathes, never jumping.
     */
    public static float cocoonRadius(float t) {
        float closing = Math.min(1.0F, Math.max(0.0F, t / 20.0F));
        float eased = closing * closing * (3.0F - 2.0F * closing);
        return 1.6F - 0.6F * eased + 0.05F * eased * (float) Math.sin(t * 0.15F);
    }

    /** The radius (blocks) of the Odinforce Wave's ring {@code t} ticks after it was struck, out to {@code max}. */
    public static float waveRadius(float t, float max) {
        float u = Math.min(1.0F, Math.max(0.0F, t / 8.0F));
        return max * (1.0F - (1.0F - u) * (1.0F - u));
    }

    private OdinHeroMath() {
    }
}
