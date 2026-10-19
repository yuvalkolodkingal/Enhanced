package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.hulk.HulkForms;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * How the Hulk acts what he does (3.6): his three stances (hunched at rest, enraged, tucked in the air of a leap), what
 * they and his gestures add to his hands in first person, the gestures his moves' payloads start (each names him: the
 * Smash, the Grab Smash, the Clap, the Roar, the leaps and the landing, the boulder torn up and thrown, the charge, the
 * transformation), and the dust at his feet and hands. Bruce Banner acts as anyone.
 */
public final class HulkActing {
    /** Below this speed (blocks per tick) he stands still. */
    static final float STILL = 0.03F;
    /** The throws to start: the player, and the client tick the boulder leaves his hands. */
    private static final List<long[]> THROWS = new ArrayList<>();

    private HulkActing() {
    }

    /** Whether the state's player is the Hulk himself (a Hulk look; not Banner, not a custom hero). */
    static boolean isHulk(HeroAnimState s, PowerData data) {
        return data.power() == Power.HULK && !HulkForms.isHuman(data);
    }

    // ---- the stances ----

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on) {
        boolean body = HulkAbilities.hasBody(data);
        boolean leaping = data.is(Live.HULK_LEAPING) && (data.power() == Power.HULK || data.has(HeroAbility.HULK_LEAP));
        on[Stance.HULK_AIR.ordinal()] = leaping && !s.onGround;
        on[Stance.HULK_ENRAGED.ordinal()] = body && data.is(Live.HULK_ENRAGED) && !leaping;
        on[Stance.HULK_IDLE.ordinal()] = isHulk(s, data) && !leaping && !data.is(Live.HULK_ENRAGED) && !p.isUsingItem();
    }

    /** The ambient rows: hunched at rest, and enraged. */
    static void ambient(PoseContext c) {
        HeroAnimState st = c.st;
        float w = st.stance(Stance.HULK_IDLE);
        if (w > 0.001F) {
            // A slow heavy breath swells the shoulders.
            float breath = EnhancedBreath.of(c.age);
            c.arm(true, HulkGestures.IDLE_M[0] + breath, HulkGestures.IDLE_M[1], HulkGestures.IDLE_M[2] + breath * 0.6F,
                    w);
            c.arm(false, HulkGestures.IDLE_O[0] + breath, HulkGestures.IDLE_O[1],
                    HulkGestures.IDLE_O[2] - breath * 0.6F, w);
            c.head(HulkGestures.IDLE_HEAD, 0, 0, w);
        }
        if ((w = st.stance(Stance.HULK_ENRAGED)) > 0.001F) {
            c.arm(true, HulkGestures.RAGE_M[0], HulkGestures.RAGE_M[1], HulkGestures.RAGE_M[2], w);
            c.arm(false, HulkGestures.RAGE_O[0], HulkGestures.RAGE_O[1], HulkGestures.RAGE_O[2], w);
            c.tremble(true, 1.6F, 0.9F, w);
            c.tremble(false, 1.6F, 0.9F, w);
            c.head(-4, 0, 0, w);
        }
    }

    /** The aimed row: tucked in the air. */
    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.HULK_AIR);
        if (w > 0.001F) {
            c.arm(true, HulkGestures.AIR_M[0], HulkGestures.AIR_M[1], HulkGestures.AIR_M[2], w);
            c.arm(false, HulkGestures.AIR_O[0], HulkGestures.AIR_O[1], HulkGestures.AIR_O[2], w);
            c.leg(true, HulkGestures.AIR_LEG_R, 4, w);
            c.leg(false, HulkGestures.AIR_LEG_L, -4, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    /** The lean of his stances: hunched forward at rest and enraged. */
    static float lean(HeroAnimState st) {
        return HulkGestures.IDLE_LEAN * st.stance(Stance.HULK_IDLE) + 12.0F * st.stance(Stance.HULK_ENRAGED);
    }

    /** The breath of his rest: a slow swell of a few degrees. */
    static final class EnhancedBreath {
        static float of(float age) {
            return 2.5F * Mth.sin(age * 0.09F);
        }

        private EnhancedBreath() {
        }
    }

    // ---- first person ----

    /** What his stances and gestures add to his hands in first person (the huge forearms move into view). */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w;
        if ((w = s.stance(Stance.HULK_ENRAGED)) > 0.001F) {
            m.add(w, -0.02F, 0.04F, -0.04F, -6, 0, 0);
        }
        if ((w = s.stance(Stance.HULK_AIR)) > 0.001F) {
            m.add(w, 0.0F, 0.30F, 0.05F, -50, 0, 0);
            o.add(w, 0.0F, 0.30F, 0.05F, -50, 0, 0);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float w2 = slot.weight;
            switch (slot.id) {
                case HULK_SMASH -> {
                    // Up over the head, then the fist down into the ground at the bottom of the view.
                    float up = slot.t < 5.0F ? 1.0F : Math.max(0.0F, 1.0F - (slot.t - 5.0F) / 2.0F);
                    m.add(w2, -0.04F, 0.45F * up - 0.25F * (1.0F - up), -0.05F, -70 * up + 40 * (1.0F - up), 0, 0);
                    o.add(w2, 0.04F, 0.45F * up - 0.25F * (1.0F - up), -0.05F, -70 * up + 40 * (1.0F - up), 0, 0);
                }
                case HULK_CLAP -> {
                    // Wide, then the hands slammed together in the middle of the view.
                    float meet = slot.t < 4.0F ? 0.0F : Math.min(1.0F, (slot.t - 4.0F));
                    m.add(w2, 0.25F - 0.55F * meet, 0.10F, -0.10F * meet, 0, 20 * meet, 0);
                    o.add(w2, -0.25F + 0.55F * meet, 0.10F, -0.10F * meet, 0, -20 * meet, 0);
                }
                case HULK_ROAR -> {
                    m.add(w2, 0.10F, -0.25F, 0.10F, 30, 0, 10);
                    o.add(w2, -0.10F, -0.25F, 0.10F, 30, 0, -10);
                }
                case HULK_GRAB_SMASH -> m.add(w2, -0.10F, 0.05F + 0.2F * Mth.sin(slot.t * 0.32F), -0.20F, -30, 0, 0);
                case HULK_RIP -> {
                    m.add(w2, -0.05F, slot.t < 4.0F ? -0.30F : 0.40F, 0.0F, slot.t < 4.0F ? 30 : -60, 0, 0);
                    o.add(w2, 0.05F, slot.t < 4.0F ? -0.30F : 0.40F, 0.0F, slot.t < 4.0F ? 30 : -60, 0, 0);
                }
                case HULK_THROW -> {
                    m.add(w2, -0.05F, 0.20F, -0.25F, -40, 0, 0);
                    o.add(w2, 0.05F, 0.20F, -0.25F, -40, 0, 0);
                }
                case HULK_CHARGE -> o.add(w2, 0.20F, -0.05F, -0.15F, -20, -30, 0);
                case HULK_LAND -> m.add(w2, -0.05F, -0.30F, -0.10F, 40, 0, 0);
                case HULK_OUT, HULK_SWITCH -> {
                    m.add(w2, 0.05F, -0.05F, 0.05F, 10, 0, 15);
                    o.add(w2, -0.05F, -0.05F, 0.05F, 10, 0, -15);
                }
                case HULK_CALM -> m.add(w2, 0.0F, -0.20F, 0.05F, 25, 0, 0);
                default -> {
                }
            }
        }
    }

    // ---- the payloads: the gesture each of his moves plays ----

    /** A payload of his arrived: the gesture it stands for plays on him (and the throw follows the tearing up). */
    public static void onPayload(HulkFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.entityId() < 0) {
            return;
        }
        Entity entity = level.getEntity(payload.entityId());
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        GestureId id = HulkGestures.gestureOf(payload.kind(), payload.arg());
        if (id == null) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(player);
        if (s == null) {
            return;
        }
        HeroAnimator.start(s, id, 0.0F, player.tickCount);
        if (id == GestureId.HULK_RIP) {
            THROWS.add(new long[]{player.getId(), level.getGameTime() + HulkAbilities.RIP_TICKS});
        }
    }

    /** Every client tick: the throws that are due. */
    static void tick(ClientLevel level) {
        Iterator<long[]> it = THROWS.iterator();
        while (it.hasNext()) {
            long[] t = it.next();
            if (level.getGameTime() >= t[1]) {
                it.remove();
                if (level.getEntity((int) t[0]) instanceof AbstractClientPlayer player) {
                    HeroAnimState s = HeroAnimator.peek(player);
                    if (s != null) {
                        HeroAnimator.start(s, GestureId.HULK_THROW, 0.0F, player.tickCount);
                    }
                }
            } else if (t[1] - level.getGameTime() > 100) {
                it.remove();
            }
        }
    }

    // ---- the flourishes ----

    /** A gesture of his began: the dust at his feet for the heavy ones. */
    static void gestureStarted(AbstractClientPlayer player, HeroAnimState state, GestureId id, float param) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player.isInvisible()) {
            return;
        }
        int count = switch (id) {
            case HULK_LEAP_CROUCH, HULK_BOUND, HULK_CHARGE -> 16;
            case HULK_RIP -> 24;
            default -> 0;
        };
        if (count > 0) {
            dust(level, player, count);
        }
    }

    /** Dust of the ground he stands on, kicked up round his feet. */
    static void dust(ClientLevel level, AbstractClientPlayer player, int count) {
        BlockPos below = player.getOnPos();
        BlockState ground = level.getBlockState(below);
        if (ground.isAir()) {
            return;
        }
        RandomSource r = level.random;
        double spread = player.getBbWidth();
        for (int i = 0; i < count; i++) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), player.getX()
                    + (r.nextDouble() - 0.5) * spread * 2.0,
                    player.getY() + 0.1, player.getZ() + (r.nextDouble() - 0.5) * spread * 2.0,
                            (r.nextDouble() - 0.5) * 0.3, 0.2,
                    (r.nextDouble() - 0.5) * 0.3);
        }
    }

    /** Whether the player is the Hulk himself, as his client sees him. */
    static boolean hulkOf(AbstractClientPlayer p) {
        PowerData data = Powers.get(p);
        return data.power() == Power.HULK && !HulkForms.isHuman(data);
    }

    static void clear() {
        THROWS.clear();
    }

    // ---- the eyes ----

    private static final net.minecraft.client.renderer.RenderType EYES = net.minecraft.client.renderer.RenderType.eyes(
            com.kolodkingal.enhanced.Enhanced.id("textures/entity/hulk/eyes.png"));
    private static final net.minecraft.client.renderer.RenderType EYES_FLAT =
            net.minecraft.client.renderer.RenderType.eyes(
            com.kolodkingal.enhanced.Enhanced.id("textures/entity/hulk/eyes_player.png"));

    /**
     * The mask of his glowing eyes: on his own body's skin, or on the player model's (the {@code hulkModel} option
     * off).
     */
    static net.minecraft.client.renderer.RenderType eyes(AbstractClientPlayer player) {
        return com.kolodkingal.enhanced.client.hulk.HulkPlayerRenderer.drawsAsHulk(player) ? EYES : EYES_FLAT;
    }
}
