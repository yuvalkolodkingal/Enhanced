package com.kolodkingal.enhanced.client.ironman;

import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.kolodkingal.enhanced.ironman.SuitKind;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.EnumMap;
import java.util.Map;

/**
 * A suit's choreography (3.8, PLAN 5.8, 7.2): how each part is posed at a moment of a suit-up ({@code up}) or a
 * take-off, from the phase's progress (0-1, interpolated with the partial tick). {@code SuitLayer} (lane L3) resets the
 * poses, asks the suit's assembly, and draws each part with its pose; the first-person arms use the same. Lane L3
 * writes the generic one ("plates slide in"), lane L7 the six suits' ({@link #of}); the 3.8 skeleton's is {@link
 * #PLAIN}.
 */
@FunctionalInterface
public interface SuitAssembly {
    /** Poses the parts for the moment {@code progress} of the phase. */
    void pose(Poses parts, float progress, boolean up, AbstractClientPlayer player, float partialTick);

    /** The skeleton's: every part shown as far as {@link SuitTimelines#reveal} says, in place. */
    SuitAssembly PLAIN = (parts, progress, up, player, partialTick) -> {
        for (SuitParts part : SuitParts.VALUES) {
            parts.get(part).shown = SuitTimelines.reveal(parts.kind, part, progress, up);
        }
    };

    /** The assembly of a suit (lane L7 registers its own with {@link Registry#register}; else {@link #PLAIN}). */
    static SuitAssembly of(SuitKind kind) {
        return Registry.ASSEMBLIES.getOrDefault(kind, PLAIN);
    }

    /** The registered assemblies. */
    final class Registry {
        static final Map<SuitKind, SuitAssembly> ASSEMBLIES = new EnumMap<>(SuitKind.class);

        public static void register(SuitKind kind, SuitAssembly assembly) {
            ASSEMBLIES.put(kind, assembly);
        }

        private Registry() {
        }
    }

    /** One part's pose: how far it is shown (0: absent, 1: whole), its offset (pixels) and turn (radians). */
    final class Pose {
        public float shown = 1.0F;
        public float x;
        public float y;
        public float z;
        public float xRot;
        public float yRot;
        public float zRot;
        public float scale = 1.0F;

        public void reset() {
            shown = 1.0F;
            x = y = z = 0.0F;
            xRot = yRot = zRot = 0.0F;
            scale = 1.0F;
        }
    }

    /** Every part's pose for one suit (reused frame to frame). */
    final class Poses {
        public final SuitKind kind;
        private final Map<SuitParts, Pose> poses = new EnumMap<>(SuitParts.class);

        public Poses(SuitKind kind) {
            this.kind = kind;
            for (SuitParts part : SuitParts.VALUES) {
                poses.put(part, new Pose());
            }
        }

        public Pose get(SuitParts part) {
            return poses.get(part);
        }

        public void reset() {
            poses.values().forEach(Pose::reset);
        }
    }
}
