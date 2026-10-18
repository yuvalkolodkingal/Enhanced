package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.config.HeimdallConfig;
import com.kolodkingal.enhanced.heimdall.HofundBond;
import com.kolodkingal.enhanced.item.HofundItem;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.heimdall.HeimdallAbilities;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * How Heimdall acts what he does, besides the keys of {@link HeimdallGestures}: which of his stances are on, their rows,
 * what they and his gestures add to his hands in first person, the blows of his combo (counted from the rises of
 * {@link Live#HEIMDALL_SLASHING}) and the golden motes that go with his gestures. The big pictures (the arcs, the column of
 * the Strike, the Ward's dome) are {@link HeimdallEffects}. Client only: {@link HeimdallClient} hands its arguments on.
 */
final class HeimdallActing {
    /** Standing still this long (ticks) with the Sight open, he settles into his watch. */
    private static final float WATCH_AFTER = 40.0F;

    /** One player's combo as his client sees it: whether the state was up last tick, the last blow and when. */
    private static final class Blows {
        boolean wasSlashing;
        int step;
        long at = Long.MIN_VALUE / 2;
        float stillSince;
    }

    private static final Map<Integer, Blows> BLOWS = new HashMap<>();
    /** Player id -> when his Ward was seen to rise (his client's clock). */
    private static final Map<Integer, Long> WARD_SEEN = new HashMap<>();
    /** The mask of his eyes (the skin's eyes, on the face's fifth row), amber while the Sight is open. */
    private static final RenderType EYES = RenderType.eyes(Enhanced.id("textures/entity/heimdall_eyes.png"));

    static RenderType eyes() {
        return EYES;
    }

    /** The blow his combo is on (1-3) as this client saw it, or 0 when it has lapsed. */
    static int comboStep(AbstractClientPlayer p, long now) {
        Blows b = BLOWS.get(p.getId());
        return b == null || now - b.at > HeimdallAbilities.COMBO_RESET ? 0 : b.step;
    }

    /** The ticks his Ward has left (of {@code full}), as this client saw it rise, or 0. */
    static long wardLeft(AbstractClientPlayer p, long now, int full) {
        Long since = WARD_SEEN.get(p.getId());
        return since == null ? 0 : Math.max(0, full - (now - since));
    }

    private HeimdallActing() {
    }

    private static Blows blows(AbstractClientPlayer p) {
        return BLOWS.computeIfAbsent(p.getId(), id -> new Blows());
    }

    // ---- the HUD ----

    /** His HUD lines: R's blade and combo, G's Sight with its far players, B's Ward time; the rest their recharge. */
    @Nullable
    static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        return switch (ability) {
            case 1 -> blade(player, data, now);
            case 2 -> data.toggle(Toggle.HEIMDALL_SIGHT) || data.is(Live.HEIMDALL_FARSIGHT)
                    ? Component.translatable("hud.enhanced.heimdall.sight", HeimdallSightView.farCount()).withStyle(ChatFormatting.GOLD)
                    : Component.translatable("hud.enhanced.off").withStyle(ChatFormatting.DARK_GRAY);
            case 4 -> {
                long left = wardLeft(player, now, (EnhancedConfig.SPEC.isLoaded() ? HeimdallConfig.WARD_SECONDS.get() : 10) * 20);
                yield left > 0 ? Component.translatable("hud.enhanced.heimdall.ward", String.format(Locale.ROOT, "%.1f", left / 20.0))
                        .withStyle(s -> s.withColor(HeimdallAbilities.GOLD)) : PowerHud.cooldown(data, 4, now);
            }
            default -> PowerHud.cooldown(data, ability, now);
        };
    }

    /** R's line: Hofund missing (and whether R calls it back), the blow the combo is on, or the recharge. */
    @Nullable
    private static Component blade(LocalPlayer player, PowerData data, long now) {
        if (data.power() == Power.HEIMDALL && !HofundItem.isHofund(player.getMainHandItem())) {
            return Component.translatable(HofundBond.hasOwn(player) ? "hud.enhanced.heimdall.no_hofund" : "hud.enhanced.heimdall.call_hofund")
                    .withStyle(ChatFormatting.RED);
        }
        int step = comboStep(player, now);
        Component cooldown = PowerHud.cooldown(data, 1, now);
        if (step <= 0 || step >= 3) {
            return cooldown;
        }
        Component blow = Component.translatable("hud.enhanced.heimdall.combo", step + 1).withStyle(s -> s.withColor(HeimdallAbilities.GOLD));
        return cooldown == null ? blow : blow.copy().append(Component.literal("  ")).append(cooldown);
    }

    // ---- the stances ----

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        boolean ward = data.has(HeroAbility.HEIMDALL_WARD) && data.is(Live.HEIMDALL_WARD);
        boolean far = data.has(HeroAbility.HEIMDALL_SIGHT) && data.is(Live.HEIMDALL_FARSIGHT);
        on[Stance.HEIMDALL_WARD.ordinal()] = ward;
        if (ward) {
            WARD_SEEN.putIfAbsent(p.getId(), now);
        } else {
            WARD_SEEN.remove(p.getId());
        }
        on[Stance.HEIMDALL_FARSIGHT.ordinal()] = far && !ward;
        Blows b = blows(p);
        boolean still = s.speed < 0.02F && s.onGround && !data.is(Live.BIFROST);
        if (!still) {
            b.stillSince = s.time < 0 ? 0 : (float) s.time;
        }
        on[Stance.HEIMDALL_WATCH.ordinal()] = data.has(HeroAbility.HEIMDALL_SIGHT) && data.toggle(Toggle.HEIMDALL_SIGHT) && !ward && !far
                && still && s.time - b.stillSince > WATCH_AFTER;
        if (data.power() == Power.HEIMDALL) {
            // He turns the sword in the light rather than raising it to the sky (HEIMDALL_BIFROST).
            on[Stance.THOR_BIFROST.ordinal()] = false;
        }
    }

    /** The watch: both hands resting on the sword, upright. */
    static void ambient(PoseContext c) {
        float w = c.st.stance(Stance.HEIMDALL_WATCH);
        if (w > 0.001F) {
            arms(c, HeimdallGestures.WATCH_M, HeimdallGestures.WATCH_O, w);
            c.head(-3 + 1.2F * (float) Math.sin(c.age * 0.03F), 0, 0, w);
        }
    }

    /** The Ward and Far Sight. */
    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.HEIMDALL_WARD);
        if (w > 0.001F) {
            arms(c, HeimdallGestures.WARD_M, HeimdallGestures.WARD_O, w);
            c.head(HeimdallGestures.WARD_HEAD, 0, 0, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        w = c.st.stance(Stance.HEIMDALL_FARSIGHT);
        if (w > 0.001F) {
            arms(c, HeimdallGestures.FAR_M, HeimdallGestures.FAR_O, w);
            c.head(HeimdallGestures.FAR_HEAD, 0, 0, w);
            c.tremble(false, 0.4F, 0.3F, w);
            c.occupy(false, w);
        }
    }

    private static void arms(PoseContext c, float[] m, float[] o, float w) {
        c.arm(true, m[0], m[1], m[2], w);
        c.arm(false, o[0], o[1], o[2], w);
    }

    static float lean(HeroAnimState st) {
        return 2.0F * st.stance(Stance.HEIMDALL_WARD) - 1.0F * st.stance(Stance.HEIMDALL_FARSIGHT);
    }

    static float drop(HeroAnimState st) {
        return 0.05F * st.stance(Stance.HEIMDALL_WARD);
    }

    // ---- the edges: the blows of the combo ----

    static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        Blows b = blows(p);
        boolean slashing = data.is(Live.HEIMDALL_SLASHING);
        if (slashing && !b.wasSlashing) {
            int step = HeimdallGestures.step(b.step, b.at, now);
            b.step = step;
            b.at = now;
            GestureId blow = HeimdallGestures.blow(step);
            if (blow != null) {
                HeroAnimator.start(s, blow, 0.0F, p.tickCount);
            }
        }
        b.wasSlashing = slashing;
    }

    // ---- first person ----

    /** His hands at the edges of the screen: on the pommel for the Ward, shading the eyes, the blows and the thrust. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w = s.stance(Stance.HEIMDALL_WARD);
        if (w > 0.001F) {
            m.add(w, 0.10F, -0.10F, 0.02F, -24, 16, 0);
            o.add(w, -0.10F, -0.10F, 0.02F, -24, -16, 0);
        }
        // (Far Sight: the shading hand is left out of his own view, which the zoom fills.)
        w = s.stance(Stance.HEIMDALL_FARSIGHT);
        if (w > 0.001F) {
            m.add(w, 0.0F, -0.06F, 0.0F, 0, 0, 0);
            FirstPersonArms.tremble(m, o, w * 0.3F, 0.002F, 0.4F, (float) s.time, EnhancedClientConfig.reducedMotion());
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float gw = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case HEIMDALL_SLASH_1 -> {
                    // From high on the right, across and down to the left.
                    float u = ss(3.0F, 6.0F, t);
                    m.add(gw, lerp(u, 0.18F, -0.34F), lerp(u, 0.30F, -0.10F), lerp(u, 0.06F, -0.10F), lerp(u, -40, 30), lerp(u, -30, 40), lerp(u, 20, -30));
                }
                case HEIMDALL_SLASH_2 -> {
                    float u = ss(3.0F, 6.0F, t);
                    m.add(gw, lerp(u, -0.30F, 0.22F), lerp(u, 0.12F, -0.06F), lerp(u, -0.06F, -0.10F), lerp(u, -30, 20), lerp(u, 50, -40), lerp(u, -20, 30));
                }
                case HEIMDALL_SLASH_3 -> {
                    float up = ss(0.0F, 5.0F, t);
                    float down = ss(5.0F, 8.0F, t);
                    m.add(gw, -0.10F * up, lerp(down, 0.36F * up, -0.18F), -0.04F, lerp(down, -60 * up, 40), 10, 0);
                    o.add(gw * up, 0.16F, lerp(down, 0.30F, -0.16F), 0.0F, lerp(down, -60, 40), -16, 0);
                }
                case HEIMDALL_LUNGE -> {
                    float back = ss(0.0F, 2.0F, t) * (1.0F - ss(2.0F, 5.0F, t));
                    float out = ss(2.0F, 5.0F, t);
                    m.add(gw, 0.0F, -0.02F * back, 0.14F * back - 0.26F * out, -10 * back - 4 * out, 0, 0);
                }
                case HEIMDALL_BIFROST -> {
                    float turn = (float) Math.sin(Math.max(0.0F, t - 4.0F) / 22.0F * Math.PI * 2.0) * 12.0F;
                    m.add(gw, 0.08F, -0.06F, 0.0F, -20, 12, turn);
                    o.add(gw, -0.08F, -0.06F, 0.0F, -20, -12, turn);
                }
                case HEIMDALL_CALL, HEIMDALL_STRIKE -> m.add(gw * ss(0.0F, 5.0F, t) * (1.0F - ss(7.0F, 10.0F, t)), 0.0F, 0.40F, 0.06F, -60, 0, 0);
                case HEIMDALL_SEND -> o.add(gw * ss(0.0F, 4.0F, t), 0.04F, 0.16F, -0.22F, -56, 8, 0);
                case HEIMDALL_SIGHT_ON -> o.add(gw * ss(0.0F, 4.0F, t), 0.10F, 0.34F, 0.06F, -70, 18, 0);
                case HEIMDALL_RETURN -> m.add(gw * (1.0F - ss(5.0F, 12.0F, t)), -0.12F, 0.10F, 0.06F, -20, -10, 10);
                default -> {
                }
            }
        }
    }

    private static float ss(float a, float b, float t) {
        float x = Math.max(0.0F, Math.min(1.0F, (t - a) / (b - a)));
        return x * x * (3.0F - 2.0F * x);
    }

    private static float lerp(float u, float a, float b) {
        return a + (b - a) * u;
    }

    // ---- the flourishes ----

    /** A gesture of his just began: the golden motes that go with it. */
    static void started(AbstractClientPlayer player, GestureId id) {
        switch (id) {
            case HEIMDALL_SIGHT_ON -> motes(player, HumanoidArm.LEFT, 6, 0.5F, 0.02, HeimdallAbilities.LIGHT);
            case HEIMDALL_CALL, HEIMDALL_RETURN -> motes(player, HumanoidArm.RIGHT, 10, 0.7F, 0.05, HeimdallAbilities.LIGHT);
            case HEIMDALL_STRIKE, HEIMDALL_BIFROST -> motes(player, HumanoidArm.RIGHT, 8, 0.6F, 0.03, HeimdallEffects.rainbow(player.getRandom().nextInt(7)));
            case HEIMDALL_SEND -> motes(player, HumanoidArm.LEFT, 8, 0.6F, 0.04, HeimdallAbilities.LIGHT);
            default -> {
            }
        }
    }

    private static void motes(AbstractClientPlayer player, HumanoidArm side, int count, float size, double speed, int colour) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        HumanoidArm arm = side == HumanoidArm.RIGHT ? player.getMainArm() : player.getMainArm().getOpposite();
        Vec3 hand = HandTracker.hand(player, arm, 1.0F);
        if (hand == null) {
            hand = WorldEffectsRenderer.handPosition(mc, player, 1.0F);
        }
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        float scale = firstPerson ? size * 0.45F : size;
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            int c = i % 3 == 0 ? HeimdallAbilities.LIGHT : colour;
            level.addParticle(new EnergyParticleOptions(c, scale), hand.x + (random.nextDouble() - 0.5) * 0.2, hand.y + (random.nextDouble() - 0.5) * 0.2,
                    hand.z + (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * speed, 0.01 + random.nextDouble() * speed,
                    (random.nextDouble() - 0.5) * speed);
        }
    }

    static void clear() {
        BLOWS.clear();
        WARD_SEEN.clear();
    }
}
