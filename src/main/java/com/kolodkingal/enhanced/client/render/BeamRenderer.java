package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.BeamPayload;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.DarkMagic;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws every continuous beam there is (3.2, see {@code ContinuousBeam}) in the {@link BeamStyle} the server named: a
 * jagged bolt from the shooter's hand to what his crosshair is on (found here by looking along his view, as the server
 * does, and stopped at a complete Chaos Shield), made of a dark fringe, a body, a pale inner light and a white-hot core
 * that writhe from one shape to the next (never jumping), with loose strands crackling beside it, short forks branching
 * off, and flares where it leaves and where it lands. Billy's is blue; another source brings its palette in its style and
 * uses this class as it is.
 *
 * <p>A beam is shown from the {@link BeamPayload}s (start, every few ticks, stop) and lapses if none comes for a moment.
 */
public final class BeamRenderer {
    /** A beam nobody has mentioned for this long is gone (the server repeats itself every 8 ticks). */
    private static final int LAPSE_TICKS = 24;
    /** How long one shape of the bolt lasts before it has flowed into the next (ticks). */
    static final float SHAPE_TICKS = 1.5F;

    private record Active(BeamStyle style, float range, long seen) {
    }

    private static final Map<Integer, Active> ACTIVE = new HashMap<>();

    /**
     * A beam drawn its own way instead of the jagged bolt (3.3: Cyclops's straight beams, from his eyes): where it leaves,
     * where it ends (given where the crosshair ray ends, already stopped at a Chaos Shield) and how it is drawn between the
     * two (camera-relative points, into the energy batch).
     */
    public interface Drawer {
        Vec3 origin(Minecraft mc, AbstractClientPlayer player, BeamStyle style, float partialTick);

        Vec3 end(AbstractClientPlayer player, BeamStyle style, float range, Vec3 crosshair, float partialTick);

        void draw(Minecraft mc, AbstractClientPlayer player, BeamStyle style, Matrix4f pose, VertexConsumer vc, Vec3 a, Vec3 b, float partialTick,
                  float time);
    }

    private static final Map<String, Drawer> DRAWERS = new HashMap<>();

    /** Draws the beams of this style with {@code drawer} (once, when the client loads). */
    public static void registerDrawer(BeamStyle style, Drawer drawer) {
        DRAWERS.put(style.id(), drawer);
    }

    /** The server says a beam burns (or has stopped). */
    public static void update(BeamPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BeamStyle style = BeamStyles.byId(payload.style());
        if (!payload.on() || style == null) {
            ACTIVE.remove(payload.shooter());
            return;
        }
        ACTIVE.put(payload.shooter(), new Active(style, payload.range(), level.getGameTime()));
    }

    /** The style of the beam this entity is firing, or null. (The pose of the arm asks which hand.) */
    @Nullable
    public static BeamStyle styleOf(int shooterId) {
        Active active = ACTIVE.get(shooterId);
        return active == null ? null : active.style();
    }

    public static void clear() {
        ACTIVE.clear();
    }

    /** Draws the beams into the energy batch (but the camera's own seen from behind him: {@link #renderOwn}). */
    static void render(Minecraft mc, ClientLevel level, Matrix4f pose, VertexConsumer vc, Vec3 camera, float partialTick, float time) {
        ACTIVE.values().removeIf(a -> time - a.seen() > LAPSE_TICKS || time < a.seen() - 1);
        for (Map.Entry<Integer, Active> entry : ACTIVE.entrySet()) {
            if (level.getEntity(entry.getKey()) instanceof AbstractClientPlayer shooter && !OwnBeams.behind(mc, shooter)) {
                beam(mc, shooter, entry.getValue(), pose, vc, camera, partialTick, time);
            }
        }
    }

    /** 3.6: the beam of the camera's own player seen from behind him, into the batch over everything ({@link OwnBeams}). */
    static void renderOwn(Minecraft mc, AbstractClientPlayer self, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                          float partialTick, float time) {
        Active active = ACTIVE.get(self.getId());
        if (active != null) {
            beam(mc, self, active, pose, vc, camera, partialTick, time);
        }
    }

    /**
     * 3.5: a hero's beam in the colour a custom hero's pick of it chose: the Darkhold's Purple Beam of Agatha's row,
     * the Optic Blast and the giant beam of Cyclops's rows (their styles keep their ids: the same drawers, and a ward
     * still knows the Darkhold's beam); any other beam, and a built-in hero's, as it is.
     */
    private static BeamStyle tinted(AbstractClientPlayer player, BeamStyle style) {
        HeroAbility row = switch (style.id()) {
            case "darkhold" -> Powers.get(player).has(HeroAbility.AGATHA_PURPLE_BEAM) ? HeroAbility.AGATHA_PURPLE_BEAM
                    : null;
            case "cyclops_optic" -> HeroAbility.CYCLOPS_OPTIC_BLAST;
            case "cyclops_unleashed" -> HeroAbility.CYCLOPS_OPEN_EYES;
            default -> null;
        };
        int chosen = row == null ? -1 : AbilityColors.chosen(player, row);
        if (chosen < 0) {
            return style;
        }
        return new BeamStyle(style.id(), style.nameKey(), AbilityColors.recolor(style.core(), chosen),
                AbilityColors.recolor(style.inner(), chosen),
                AbilityColors.recolor(style.main(), chosen), AbilityColors.recolor(style.body(), chosen),
                        AbilityColors.recolor(style.fringe(), chosen),
                style.offHand());
    }

    /** Where the beam leaves: the hand of the style, drawn where the body really has it (in first person, at the screen's edge). */
    private static Vec3 origin(Minecraft mc, AbstractClientPlayer player, BeamStyle style, float partialTick) {
        Vec3 main = WorldEffectsRenderer.handPosition(mc, player, partialTick);
        if (!style.offHand()) {
            return main;
        }
        Vec3 tracked = HandTracker.hand(player, player.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT, partialTick);
        if (tracked != null) {
            return tracked;
        }
        Vec3 look = player.getViewVector(partialTick);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : right.normalize();
        double side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;
        return main.subtract(right.scale(side * 0.45));
    }

    private static void beam(Minecraft mc, AbstractClientPlayer player, Active active, Matrix4f pose, VertexConsumer vc, Vec3 camera,
                             float partialTick, float time) {
        BeamStyle style = tinted(player, active.style());
        Drawer drawer = DRAWERS.get(style.id());
        Vec3 from = drawer != null ? drawer.origin(mc, player, style, partialTick) : origin(mc, player, style, partialTick);
        Vec3 target = Raycasts.any(player, active.range(), ContinuousBeam.targets(player), partialTick).getLocation();
        // A wall in the way (a Chaos Shield, a Hex Barrier, a Hex's wall, a ward against dark magic) stops it there, as on the server.
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(player.level(), player, player.getEyePosition(partialTick), target, partialTick,
                DarkMagic.isDark(style));
        if (shield != null) {
            target = shield.point();
        }
        if (drawer != null) {
            target = drawer.end(player, style, active.range(), target, partialTick);
            drawer.draw(mc, player, style, pose, vc, from.subtract(camera), target.subtract(camera), partialTick, time);
            return;
        }
        Vec3 a = from.subtract(camera);
        Vec3 b = target.subtract(camera);
        double length = a.distanceTo(b);
        if (length < 0.05) {
            return;
        }
        float flicker = 0.85F + 0.15F * Mth.sin(time * 2.9F) * Mth.cos(time * 1.3F);
        long seed = player.getId() * 104729L;
        // (3.3) The bolt flows from one shape to the next, and the strands and forks fade across, instead of jumping.
        float phase = time / SHAPE_TICKS;
        double jitter = Math.max(0.1, Math.min(0.9, length * 0.03));

        // A soft glow all along the line.
        EnergyGeometry.ribbon(pose, vc, a, b, 0.14F, 0.18F, EnergyGeometry.argb(style.fringe(), 0.10F * flicker), EnergyGeometry.argb(style.fringe(), 0.14F * flicker));
        List<Vec3> path = EnergyGeometry.flowingPath(a, b, jitter, seed, phase);
        layers(pose, vc, path, style, 1.0F, flicker);
        Vec3 dir = b.subtract(a).scale(1.0 / length);
        EnergyGeometry.crossfade(seed + 31L, phase, (random, weight) -> {
            // Loose strands that crackle round the main bolt.
            for (int i = 0; i < 2; i++) {
                List<Vec3> strand = EnergyGeometry.lightningPath(a, b, jitter * 1.7, random);
                tapered(pose, vc, strand, 0.05F, EnergyGeometry.argb(style.main(), 0.55F * weight));
                tapered(pose, vc, strand, 0.018F, EnergyGeometry.argb(style.inner(), 0.7F * weight));
            }
            // Forks branching off, like his bolt.
            for (int i = 0; i < 3 && path.size() > 3; i++) {
                Vec3 start = path.get(1 + random.nextInt(path.size() - 2));
                double reach = 0.5 + random.nextDouble() * Math.min(2.0, length * 0.15);
                Vec3 side = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5,
                        random.nextDouble() - 0.5).normalize();
                Vec3 end = start.add(dir.scale(reach * 0.6)).add(side.scale(reach * 0.8));
                List<Vec3> fork = EnergyGeometry.lightningPath(start, end, reach * 0.2, random);
                EnergyGeometry.polyline(pose, vc, fork, 0.07F, EnergyGeometry.argb(style.body(), 0.6F * weight));
                EnergyGeometry.polyline(pose, vc, fork, 0.025F, EnergyGeometry.argb(style.core(), 0.85F * weight));
            }
        });
        // Flares where it leaves (smaller the closer it is to the camera) and where it lands.
        float near = (float) Mth.clamp(a.length() / 2.5, 0.12, 1.0);
        flare(pose, vc, a, 0.2F * flicker * near, style);
        flare(pose, vc, b, 0.36F * flicker, style);
    }

    /** The bolt in layers: the fringe outside, then the body, the main light, the inner light and the white-hot core. */
    private static void layers(Matrix4f pose, VertexConsumer vc, List<Vec3> path, BeamStyle style, float scale, float flicker) {
        tapered(pose, vc, path, 0.30F * scale, EnergyGeometry.argb(style.fringe(), 0.28F * flicker));
        tapered(pose, vc, path, 0.17F * scale, EnergyGeometry.argb(style.body(), 0.5F * flicker));
        tapered(pose, vc, path, 0.09F * scale, EnergyGeometry.argb(style.main(), 0.8F));
        tapered(pose, vc, path, 0.05F * scale, EnergyGeometry.argb(style.inner(), 0.92F));
        tapered(pose, vc, path, 0.022F * scale, EnergyGeometry.argb(style.core(), 1.0F));
    }

    /** The path as ribbons, a quarter as wide at the start growing to the full width three blocks along (so it never fills the screen). */
    private static void tapered(Matrix4f pose, VertexConsumer vc, List<Vec3> path, float width, int color) {
        double along = 0.0;
        for (int i = 1; i < path.size(); i++) {
            Vec3 p = path.get(i - 1);
            Vec3 q = path.get(i);
            double next = along + p.distanceTo(q);
            EnergyGeometry.ribbon(pose, vc, p, q, taper(width, along), taper(width, next), color, color);
            along = next;
        }
    }

    private static float taper(float width, double along) {
        return width * (float) Math.min(1.0, 0.25 + along / 3.0);
    }

    /** A glow facing the camera: rings of light thicker and brighter towards the middle, and a white-hot heart. */
    private static void flare(Matrix4f pose, VertexConsumer vc, Vec3 center, float radius, BeamStyle style) {
        Vec3 toCamera = center.scale(-1);
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.8F, radius * 0.45F, 14, EnergyGeometry.argb(style.fringe(), 0.22F));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.5F, radius * 0.4F, 14, EnergyGeometry.argb(style.main(), 0.4F));
        EnergyGeometry.ring(pose, vc, center, toCamera, radius * 0.22F, radius * 0.3F, 12, EnergyGeometry.argb(style.core(), 0.9F));
    }

    private BeamRenderer() {
    }
}
