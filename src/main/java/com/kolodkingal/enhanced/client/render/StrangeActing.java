package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKeys;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.strange.AstralBodyEntity;
import com.kolodkingal.enhanced.strange.AstralProjection;
import com.kolodkingal.enhanced.strange.EldritchWhip;
import com.kolodkingal.enhanced.strange.EyeTime;
import com.kolodkingal.enhanced.strange.MirrorDimension;
import com.kolodkingal.enhanced.strange.SeraphimShield;
import com.kolodkingal.enhanced.strange.StrangeAbilities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;

/**
 * How Doctor Strange acts what he does, besides the keys of {@link StrangeGestures}: which of his stances are on and
 * their poses (casting, the Shield held, the Cloak's hover), his hands in first person, the sparks that go with his
 * gestures, and his HUD lines (the lash of the combo, the Shield's and the Dome's time, the spirit's time and cord, the
 * portals' and the Mirror's time, the Eye). Client only: {@link StrangeClient} hands its arguments on.
 */
final class StrangeActing {
    /** What this client saw of a player's switches and his Eye, on its own clock. */
    private static final class Watch {
        long astralSince = -1;
        long mirrorSince = -1;
        long slowSince = -1;
        long eyeSince = -1;
    }

    private static final Map<Integer, Watch> WATCHES = new HashMap<>();

    private static Watch watch(AbstractClientPlayer p) {
        return WATCHES.computeIfAbsent(p.getId(), id -> new Watch());
    }

    private StrangeActing() {
    }

    // ---- the HUD ----

    @Nullable
    static Component status(LocalPlayer player, PowerData data, int ability, long now) {
        Watch w = watch(player);
        StrangeSpellFx.Seen seen = StrangeSpellFx.seen(player.getId());
        return switch (ability) {
            case 1 -> {
                int step = now - seen.stepAt <= EldritchWhip.COMBO_RESET ? seen.step : 0;
                Component cooldown = PowerHud.cooldown(data, 1, now);
                if (step <= 0 || step >= 3) {
                    yield cooldown;
                }
                Component lash = Component.translatable("hud.enhanced.strange.lash", step + 1)
                        .withStyle(s -> s.withColor(StrangeAbilities.ORANGE));
                yield cooldown == null ? lash : lash.copy().append("  ").append(cooldown);
            }
            case 2 -> Component.translatable(data.flying() ? "hud.enhanced.on" : "hud.enhanced.off")
                    .withStyle(data.flying() ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY);
            case 3 -> {
                if (data.is(Live.STRANGE_SHIELD)) {
                    yield SeraphimShield.shieldLine(seen.shieldSince < 0 ? 0 : now - seen.shieldSince);
                }
                if (seen.domeUntil > now) {
                    yield seconds("hud.enhanced.strange.dome", seen.domeUntil - now, StrangeAbilities.ORANGE);
                }
                yield PowerHud.cooldown(data, 3, now);
            }
            case 4 -> {
                if (data.toggle(Toggle.STRANGE_ASTRAL)) {
                    long left = Math.max(0, AstralProjection.ticks() - (w.astralSince < 0 ? 0 : now - w.astralSince));
                    AstralBodyEntity body = body(player);
                    int far = body == null ? 0 : (int) Math.round(player.position().distanceTo(body.position()));
                    yield Component.translatable("hud.enhanced.strange.astral", format(left), far,
                            AstralProjection.range()).withStyle(s -> s.withColor(StrangeAbilities.ASTRAL_BLUE));
                }
                yield PowerHud.cooldown(data, 4, now);
            }
            case 5 -> {
                if (data.toggle(Toggle.STRANGE_MIRROR)) {
                    long left = Math.max(0, MirrorDimension.ticks() - (w.mirrorSince < 0 ? 0 : now - w.mirrorSince));
                    yield seconds("hud.enhanced.strange.mirror", left, 0xB58CFF);
                }
                if (seen.portalsUntil > now) {
                    yield seconds("hud.enhanced.strange.portals", seen.portalsUntil - now, StrangeAbilities.ORANGE);
                }
                yield PowerHud.cooldown(data, 5, now);
            }
            case HeroKeys.Y -> eye(player, data, w, now);
            default -> PowerHud.cooldown(data, ability, now);
        };
    }

    /** The Eye's line: missing, its Slow Time, its recharge, or ready with the Rewind's history. */
    private static Component eye(LocalPlayer player, PowerData data, Watch w, long now) {
        if (!EyeTime.hasEye(player)) {
            return Component.translatable("hud.enhanced.strange.no_eye").withStyle(ChatFormatting.RED);
        }
        if (data.toggle(Toggle.TIME_SLOW)) {
            long left = Math.max(0, EyeTime.slowMaxTicks() - (w.slowSince < 0 ? 0 : now - w.slowSince));
            return seconds("hud.enhanced.strange.slowing", left, StrangeAbilities.TIME_GREEN);
        }
        float charge = player.getCooldowns().getCooldownPercent(EyeTime.eye(), 0.0F);
        if (charge > 0.0F) {
            return Component.translatable("hud.enhanced.strange.eye_charging", Math.round((1.0F - charge) * 100))
                    .withStyle(ChatFormatting.GRAY);
        }
        long held = w.eyeSince < 0 ? 0 : now - w.eyeSince;
        int full = EyeTime.rewindSeconds() * 20;
        int bars = (int) Math.min(5, held * 5 / Math.max(1, full));
        MutableComponent line = Component.translatable("hud.enhanced.strange.eye_ready")
                .withStyle(s -> s.withColor(StrangeAbilities.TIME_GREEN));
        return line.append(Component.literal("  " + "▮".repeat(bars) + "▯".repeat(5 - bars))
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    private static Component seconds(String key, long ticks, int color) {
        return Component.translatable(key, format(ticks)).withStyle(s -> s.withColor(color));
    }

    private static String format(long ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0);
    }

    /** The body of this player's spirit, if this client has it. */
    @Nullable
    static AstralBodyEntity body(AbstractClientPlayer player) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        for (AstralBodyEntity body : level.getEntitiesOfClass(AstralBodyEntity.class,
                player.getBoundingBox().inflate(AstralProjection.range() + 8))) {
            if (player.getUUID().equals(body.owner())) {
                return body;
            }
        }
        return null;
    }

    // ---- the edges: what the HUD counts ----

    static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
        Watch w = watch(p);
        w.astralSince = data.toggle(Toggle.STRANGE_ASTRAL) ? (w.astralSince < 0 ? now : w.astralSince) : -1;
        w.mirrorSince = data.toggle(Toggle.STRANGE_MIRROR) ? (w.mirrorSince < 0 ? now : w.mirrorSince) : -1;
        boolean slow = data.toggle(Toggle.TIME_SLOW);
        if (!slow && w.slowSince >= 0) {
            w.eyeSince = now; // (the Eye's history begins again after its time)
        }
        w.slowSince = slow ? (w.slowSince < 0 ? now : w.slowSince) : -1;
        boolean eye = EyeTime.hasEye(p) && p.isAlive();
        w.eyeSince = eye ? (w.eyeSince < 0 ? now : w.eyeSince) : -1;
    }

    /** A Rewind: the history is spent (StrangeSpellFx tells it). */
    static void rewound(AbstractClientPlayer p, long now) {
        watch(p).eyeSince = now;
    }

    // ---- the stances ----

    static void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
        boolean shield = data.has(HeroAbility.STRANGE_SHIELDS) && data.is(Live.STRANGE_SHIELD);
        on[Stance.STRANGE_SHIELD.ordinal()] = shield;
        on[Stance.STRANGE_CASTING.ordinal()] = !shield && data.is(Live.STRANGE_CASTING) && data.acts(Power.DOCTOR_STRANGE);
        on[Stance.STRANGE_LEVITATE.ordinal()] = s.flyingEff && Math.abs(s.s) < 0.35F && !shield
                && data.power() == Power.DOCTOR_STRANGE;
    }

    /** The Cloak's hover: the legs together, the knees a little bent, the hands relaxed. */
    static void ambient(PoseContext c) {
        float w = c.st.stance(Stance.STRANGE_LEVITATE);
        if (w > 0.001F) {
            float drift = EnhancedClientConfig.reducedMotion() ? 0.0F : 2.0F * (float) Math.sin(c.age * 0.05F);
            c.arm(true, StrangeGestures.LEVITATE_M[0] + drift, StrangeGestures.LEVITATE_M[1],
                    StrangeGestures.LEVITATE_M[2], w);
            c.arm(false, StrangeGestures.LEVITATE_O[0] - drift, StrangeGestures.LEVITATE_O[1],
                    StrangeGestures.LEVITATE_O[2], w);
            c.leg(true, StrangeGestures.LEVITATE_LEG, 1, w);
            c.leg(false, StrangeGestures.LEVITATE_LEG * 0.6F, -1, w);
        }
    }

    /** The Shield held and the casting mudra. */
    static void aimed(PoseContext c) {
        float w = c.st.stance(Stance.STRANGE_SHIELD);
        if (w > 0.001F) {
            float t = EnhancedClientConfig.reducedMotion() ? 0.0F : c.age * 0.35F;
            c.aim(false, LOOK, 0, 4, 0, w);
            c.arm(true, StrangeGestures.SHIELD_M[0] + 9.0F * (float) Math.sin(t), StrangeGestures.SHIELD_M[1]
                    + 9.0F * (float) Math.cos(t), StrangeGestures.SHIELD_M[2], w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
        w = c.st.stance(Stance.STRANGE_CASTING);
        if (w > 0.001F) {
            c.arm(true, StrangeGestures.CAST_M[0], StrangeGestures.CAST_M[1], StrangeGestures.CAST_M[2], w);
            c.arm(false, StrangeGestures.CAST_O[0], StrangeGestures.CAST_O[1], StrangeGestures.CAST_O[2], w);
            c.tremble(true, 1.2F, 0.5F, w);
            c.tremble(false, 1.2F, 0.5F, w);
            c.occupy(true, w);
            c.occupy(false, w);
        }
    }

    static float lean(HeroAnimState st) {
        return 2.0F * st.stance(Stance.STRANGE_SHIELD) + 1.0F * st.stance(Stance.STRANGE_CASTING);
    }

    static float drop(HeroAnimState st) {
        return 0.03F * st.stance(Stance.STRANGE_LEVITATE);
    }

    // ---- first person ----

    /** His hands at the edges of the screen: the Shield's palm, the mudra, and every spell's gesture. */
    static void firstPerson(HeroAnimState s, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float w = s.stance(Stance.STRANGE_SHIELD);
        boolean calm = EnhancedClientConfig.reducedMotion();
        float time = (float) s.time;
        if (w > 0.001F) {
            o.add(w, 0.06F, 0.14F, -0.16F, -50, 12, 0);
            float t = calm ? 0.0F : time * 0.35F;
            m.add(w, 0.04F * (float) Math.sin(t), 0.12F + 0.04F * (float) Math.cos(t), -0.06F, -40, -10, 0);
        }
        w = s.stance(Stance.STRANGE_CASTING);
        if (w > 0.001F) {
            m.add(w, -0.06F, 0.14F, -0.08F, -40, -14, 0);
            o.add(w, 0.06F, 0.14F, -0.08F, -40, 14, 0);
            FirstPersonArms.tremble(m, o, w * 0.4F, 0.002F, 0.5F, time, calm);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float gw = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case STRANGE_WHIP_1, STRANGE_WHIP_2 -> {
                    float u = ss(1.0F, 5.0F, t);
                    float side = slot.id == GestureId.STRANGE_WHIP_1 ? 1.0F : -1.0F;
                    m.add(gw, lerp(u, 0.12F * side, -0.2F * side), lerp(u, -0.1F, 0.12F), lerp(u, 0.06F, -0.16F),
                            lerp(u, 10, -40), lerp(u, 20 * side, -30 * side), 0);
                }
                case STRANGE_WHIP_3 -> {
                    float up = ss(0.0F, 4.0F, t);
                    float down = ss(4.0F, 7.0F, t);
                    m.add(gw, 0.0F, lerp(down, 0.36F * up, -0.12F), -0.06F, lerp(down, -60 * up, 30), 0, 0);
                }
                case STRANGE_GRAPPLE -> {
                    float out = ss(0.0F, 3.0F, t) * (1.0F - ss(8.0F, 14.0F, t));
                    m.add(gw * out, 0.0F, 0.1F, -0.22F, -60, 0, 0);
                    o.add(gw * ss(5.0F, 8.0F, t) * (1.0F - ss(12.0F, 16.0F, t)), 0.0F, 0.08F, -0.18F, -50, 0, 0);
                }
                case STRANGE_BANDS -> {
                    float out = ss(3.0F, 6.0F, t) * (1.0F - ss(10.0F, 14.0F, t));
                    m.add(gw * out, -0.08F, 0.12F, -0.24F, -60, -10, 0);
                    o.add(gw * out, 0.08F, 0.12F, -0.24F, -60, 10, 0);
                }
                case STRANGE_SLING -> {
                    float a = (float) Math.PI * 2.0F * ss(3.0F, 14.0F, t);
                    float in = ss(0.0F, 3.0F, t) * (1.0F - ss(14.0F, 18.0F, t));
                    m.add(gw * in, -0.06F + 0.14F * (float) Math.sin(a), 0.16F + 0.14F * (float) Math.cos(a), -0.18F,
                            -50, 0, 0);
                    o.add(gw * in, 0.12F, 0.06F, -0.06F, -30, 10, 0);
                }
                case STRANGE_MIRROR -> {
                    float in = ss(0.0F, 5.0F, t) * (1.0F - ss(16.0F, 22.0F, t));
                    float apart = ss(9.0F, 14.0F, t);
                    m.add(gw * in, lerp(apart, -0.14F, 0.1F), 0.16F, -0.14F, -50, lerp(apart, -30, 20), 0);
                    o.add(gw * in, lerp(apart, 0.14F, -0.1F), 0.16F, -0.14F, -50, lerp(apart, 30, -20), 0);
                }
                case STRANGE_DOME, STRANGE_ASTRAL -> {
                    float up = ss(2.0F, 9.0F, t) * (1.0F - ss(15.0F, 21.0F, t));
                    m.add(gw * up, 0.12F, 0.3F, -0.06F, -60, -10, 0);
                    o.add(gw * up, -0.12F, 0.3F, -0.06F, -60, 10, 0);
                }
                case STRANGE_EYE_OPEN, STRANGE_REWIND -> {
                    float in = ss(0.0F, 5.0F, t) * (1.0F - ss(slot.id.dur - 5.0F, slot.id.dur, t));
                    float turn = calm ? 0.0F : 12.0F * (float) Math.sin(t * 0.8F);
                    m.add(gw * in, -0.1F, 0.08F, -0.1F, -40, -20, turn);
                    o.add(gw * in, 0.1F, 0.08F, -0.1F, -40, 20, -turn);
                }
                case STRANGE_SHIELD_UP -> o.add(gw * ss(0.0F, 4.0F, t), 0.06F, 0.14F, -0.16F, -50, 12, 0);
                case STRANGE_ASTRAL_IN -> m.add(gw * (1.0F - ss(4.0F, 12.0F, t)), -0.08F, 0.08F, 0.0F, -30, -20, 0);
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

    /** A gesture of his just began: the sparks that go with it, from his hands. */
    static void started(AbstractClientPlayer player, GestureId id) {
        switch (id) {
            case STRANGE_SLING -> motes(player, HumanoidArm.RIGHT, 14, 0.6F, 0.05, StrangeAbilities.ORANGE);
            case STRANGE_BANDS -> {
                motes(player, HumanoidArm.RIGHT, 8, 0.6F, 0.05, StrangeAbilities.CRIMSON);
                motes(player, HumanoidArm.LEFT, 8, 0.6F, 0.05, StrangeAbilities.CRIMSON);
            }
            case STRANGE_EYE_OPEN, STRANGE_REWIND -> {
                motes(player, HumanoidArm.RIGHT, 8, 0.5F, 0.03, StrangeAbilities.TIME_GREEN);
                if (id == GestureId.STRANGE_REWIND) {
                    rewound(player, player.level().getGameTime());
                }
            }
            case STRANGE_ASTRAL, STRANGE_ASTRAL_IN -> motes(player, HumanoidArm.LEFT, 10, 0.5F, 0.04,
                    StrangeAbilities.ASTRAL_BLUE);
            case STRANGE_MIRROR, STRANGE_DOME, STRANGE_SHIELD_UP -> {
                motes(player, HumanoidArm.RIGHT, 8, 0.5F, 0.04, StrangeAbilities.ORANGE);
                motes(player, HumanoidArm.LEFT, 8, 0.5F, 0.04, StrangeAbilities.GOLD);
            }
            default -> {
            }
        }
    }

    private static void motes(AbstractClientPlayer player, HumanoidArm side, int count, float size, double speed,
                              int colour) {
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
            int c = i % 3 == 0 ? StrangeAbilities.GOLD : colour;
            level.addParticle(new EnergyParticleOptions(c, scale), hand.x + (random.nextDouble() - 0.5) * 0.2,
                    hand.y + (random.nextDouble() - 0.5) * 0.2, hand.z + (random.nextDouble() - 0.5) * 0.2,
                    (random.nextDouble() - 0.5) * speed, 0.01 + random.nextDouble() * speed,
                    (random.nextDouble() - 0.5) * speed);
        }
    }

    static void clear() {
        WATCHES.clear();
    }
}
