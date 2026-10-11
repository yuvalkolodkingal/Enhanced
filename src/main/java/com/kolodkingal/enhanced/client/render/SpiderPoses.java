package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.SpiderClimb;
import com.kolodkingal.enhanced.client.SpiderMotion;
import com.kolodkingal.enhanced.client.render.Gestures.Target;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The body language of Spider-Man's stances (DESIGN section 14, "Animation"): clinging to a wall (the hands reach up
 * and the knees go out, one cycle of the crawl for every 1.1 blocks he climbs), hanging from a ceiling, the Black Eyes'
 * hunch, Instant Kill's menace, the guard of the spider-sense, and the arm of the web in a zip and a swing. Besides the
 * arms it turns the whole body: against a wall it faces the wall whichever way the camera looks, on a ceiling it is
 * flipped over about its middle, and a zip or a swing faces its anchor ({@link #advance}).
 *
 * <p>The pose rows are written for a right-handed player, as every row of the mod is (see {@link PoseContext}); the
 * weights are the stances' eased ones, so nothing snaps.
 */
final class SpiderPoses {
    /** How far toward the wall his body is moved, so the chest is against it and not in the middle of his box. */
    static final float WALL_OFFSET = 0.22F;
    /** The part of a full turn of the walk swing that the ceiling's upside-down walk keeps (it is scaled by this). */
    private static final float CEILING_SWING = 1.15F;

    // =================================================================================================================
    // Once per tick
    // =================================================================================================================

    /**
     * What the tick sees of the hero: the wall he clings to, and how fast he moves along the surface (the displacement
     * of the tick projected on its plane). The local hero's own reckoning of the surface wins over the one the data
     * gives.
     */
    static void sample(HeroAnimState s, AbstractClientPlayer p, PowerData data) {
        Vec3 d = new Vec3(p.getX() - p.xOld, p.getY() - p.yOld, p.getZ() - p.zOld);
        boolean wall = data.is(Live.CLINGING_WALL);
        boolean ceiling = data.is(Live.CLINGING_CEILING);
        SpiderClimb.Surface surface = SpiderClimb.Surface.NONE;
        if (wall || ceiling) {
            surface = p == Minecraft.getInstance().player ? SpiderMotion.surface() : SpiderClimb.Surface.NONE;
            if (!surface.touches()) {
                surface = SpiderClimb.detect(p.level(), p, Vec3.ZERO, null);
            }
        }
        float speed = 0.0F;
        if (wall) {
            if (surface.kind() == SpiderClimb.Kind.WALL) {
                s.wallDir = surface.normalVector().scale(-1);
            }
            Vec3 n = s.wallDir;
            Vec3 along = d.subtract(n.scale(d.dot(n)));
            speed = (float) along.length();
        } else if (ceiling) {
            speed = (float) Math.hypot(d.x, d.z);
        }
        s.crawlSpeed = speed;
        // The local hero's arm aims at the end of the web his own movement holds (the server's word comes later).
        if (p == Minecraft.getInstance().player && SpiderMotion.anchor() != null) {
            s.anchor = SpiderMotion.anchor();
        }
        if (s.stance(Stance.SPIDER_ZIP) < 0.01F && s.stance(Stance.SPIDER_SWING) < 0.01F && !s.stanceOn[Stance.SPIDER_ZIP.ordinal()]
                && !s.stanceOn[Stance.SPIDER_SWING.ordinal()]) {
            s.anchor = null;
        }
    }

    // =================================================================================================================
    // Once per frame
    // =================================================================================================================

    /**
     * Moves the crawl cycle on (one cycle per 1.1 blocks of the speed along the surface), and works out how the cling
     * turns the body: it faces the wall, or the anchor of a zip or a swing, whichever way the camera looks (the head
     * still looks where the camera looks, see HeroPoses); a ceiling flips it over; a wall moves it a little closer.
     */
    static void advance(HeroAnimState s, AbstractClientPlayer p, float dt, boolean snap) {
        float omega = CrawlMath.omega(s.crawlSpeed);
        float amp = CrawlMath.amp(s.crawlSpeed);
        s.crawlOmega = snap ? omega : AnimMath.ease(s.crawlOmega, omega, dt, 3.0F);
        s.crawlPhase = CrawlMath.advance(s.crawlPhase, s.crawlOmega, dt);
        s.crawlAmp = snap ? amp : AnimMath.ease(s.crawlAmp, amp, dt, 3.0F);

        float wall = s.stance(Stance.SPIDER_CLING_WALL);
        float web = Math.max(s.stance(Stance.SPIDER_ZIP), s.stance(Stance.SPIDER_SWING));
        float weight = Math.max(wall, web);
        float side = p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0F : -1.0F;
        if (weight > 0.001F) {
            // Where the body is to face: the wall (a unit vector toward it) and the anchor, as the stances' weights
            // blend them.
            double x = 0.0;
            double z = 0.0;
            if (wall > 0.001F && s.wallDir.lengthSqr() > 0.5) {
                x += s.wallDir.x * wall;
                z += s.wallDir.z * wall;
            }
            if (web > 0.001F && s.anchor != null) {
                double ax = s.anchor.x - p.getX();
                double az = s.anchor.z - p.getZ();
                double length = Math.hypot(ax, az);
                if (length > 0.6) {
                    x += ax / length * web;
                    z += az / length * web;
                }
            }
            if (x * x + z * z > 1.0E-6) {
                float target = (float) (Math.atan2(-x, z) * Mth.RAD_TO_DEG);
                s.faceYaw = !s.faceSet || snap ? target : s.faceYaw + Mth.wrapDegrees(target - s.faceYaw) * (1.0F - (float) Math.exp(-dt / 3.0F));
                s.faceSet = true;
            }
            s.stanceYaw = s.faceSet ? Mth.wrapDegrees(s.faceYaw - s.bodyYaw) * weight * side : 0.0F;
        } else {
            s.faceSet = false;
            s.stanceYaw = 0.0F;
        }

        s.ceilingRoll = 180.0F * s.stance(Stance.SPIDER_CLING_CEILING);
        // The chest against the wall: moved toward it in the body's own frame (x to his right, z behind him).
        if (wall > 0.001F && s.wallDir.lengthSqr() > 0.5) {
            float yaw = s.bodyYaw * Mth.DEG_TO_RAD;
            float right = (float) (-s.wallDir.x * Mth.cos(yaw) - s.wallDir.z * Mth.sin(yaw));
            float forward = (float) (-s.wallDir.x * Mth.sin(yaw) + s.wallDir.z * Mth.cos(yaw));
            s.clingOffX = right * WALL_OFFSET * wall;
            s.clingOffZ = -forward * WALL_OFFSET * wall;
        } else {
            s.clingOffX = 0.0F;
            s.clingOffZ = 0.0F;
        }
    }

    // =================================================================================================================
    // The stances' rows
    // =================================================================================================================

    /** The ambient stances: clinging, the surge, the kill, the guard. */
    static void ambient(PoseContext c) {
        HeroAnimState st = c.st;
        float w;
        if ((w = st.stance(Stance.SPIDER_CLING_WALL)) > 0.001F) {
            // Arms reach up the wall one after the other and the knees go out and up, like a gecko; at rest they hold,
            // with the small drift of the pipeline.
            float p = st.crawlPhase;
            float amp = st.crawlAmp;
            float[] m = SpiderGestures.CLING_WALL_M;
            float[] o = SpiderGestures.CLING_WALL_O;
            c.arm(true, m[0] + 24 * Mth.sin(p) * amp, m[1], m[2], w);
            c.arm(false, o[0] + 24 * Mth.sin(p + Mth.PI) * amp, o[1], o[2], w);
            c.leg(true, SpiderGestures.CLING_WALL_LEGS + 22 * Mth.sin(p + Mth.PI) * amp, 22, w);
            c.leg(false, SpiderGestures.CLING_WALL_LEGS + 22 * Mth.sin(p) * amp, -22, w);
            c.head(SpiderGestures.CLING_WALL_HEAD, 0, 0, w);
            Stances.drift(c, 1.5F, w * (1.0F - amp), true);
        }
        if ((w = st.stance(Stance.SPIDER_CLING_CEILING)) > 0.001F) {
            // The body is flipped (HeroAnimator): the arms hang a little out and the walk swing is a little more,
            // upside down.
            c.armAdd(true, 0, 0, 10, w);
            c.armAdd(false, 0, 0, -10, w);
            float scale = 1.0F + (CEILING_SWING - 1.0F) * w;
            c.model.rightLeg.xRot *= scale;
            c.model.leftLeg.xRot *= scale;
            c.model.rightArm.xRot *= scale;
            c.model.leftArm.xRot *= scale;
            c.head(12, 0, 0, w);
        }
        if ((w = st.stance(Stance.SPIDER_SURGE)) > 0.001F) {
            // A predatory hunch: the hands hang forward and out, clawed, trembling, the head down.
            pose(c, SpiderGestures.SURGE_M, SpiderGestures.SURGE_O, w);
            c.tremble(true, 1.1F, 1.9F, w);
            c.tremble(false, 1.1F, 1.9F, w);
            c.head(SpiderGestures.SURGE_HEAD, 0, 0, SpiderGestures.kept(st, Stance.SPIDER_SURGE));
            float ground = w * (1.0F - st.wAir);
            c.leg(true, -10, 6, ground);
            c.leg(false, 10, -6, ground);
        }
        if ((w = st.stance(Stance.SPIDER_KILL)) > 0.001F) {
            // Menace: the arms a little out, a tremble, the head down; the legs and the eyes do the rest.
            pose(c, SpiderGestures.KILL_M, SpiderGestures.KILL_O, w);
            c.tremble(true, 0.8F, 2.3F, w);
            c.tremble(false, 0.8F, 2.3F, w);
            c.head(SpiderGestures.KILL_HEAD, 0, 0, SpiderGestures.kept(st, Stance.SPIDER_KILL));
        }
        if ((w = st.stance(Stance.SPIDER_ALERT)) > 0.001F) {
            // The guard: both forearms up in front of him, the head a little up.
            pose(c, SpiderGestures.ALERT_M, SpiderGestures.ALERT_O, w);
            c.head(SpiderGestures.ALERT_HEAD, 0, 0, SpiderGestures.kept(st, Stance.SPIDER_ALERT));
        }
    }

    /** The aimed stances: the arm of the web. */
    static void aimed(PoseContext c) {
        HeroAnimState st = c.st;
        float w;
        if ((w = st.stance(Stance.SPIDER_ZIP)) > 0.001F) {
            // The web arm points at the anchor (a little under it), the other swept back; the legs trail.
            float[] aim = SpiderGestures.ZIP_AIM;
            float[] o = SpiderGestures.ZIP_O;
            c.aim(true, Target.ANCHOR, aim[0], aim[1], aim[2], w);
            c.tremble(true, 1.0F, 2.1F, w);
            c.arm(false, o[0], o[1], o[2], w);
            c.leg(true, SpiderGestures.ZIP_LEGS[0], 2, w);
            c.leg(false, SpiderGestures.ZIP_LEGS[1], -2, w);
            c.head(SpiderGestures.ZIP_HEAD, 0, 0, w);
            c.occupy(true, w);
        }
        if ((w = st.stance(Stance.SPIDER_SWING)) > 0.001F) {
            // The arm runs along the web, up; the other back and out; the knees tucked.
            float[] aim = SpiderGestures.SWING_AIM;
            float[] o = SpiderGestures.SWING_O;
            c.aim(true, Target.ANCHOR, aim[0], aim[1], aim[2], w);
            c.tremble(true, 1.5F, 1.7F, w);
            c.arm(false, o[0], o[1], o[2], w);
            c.leg(true, SpiderGestures.SWING_LEGS[0], 8, w);
            c.leg(false, SpiderGestures.SWING_LEGS[1], -8, w);
            c.head(SpiderGestures.SWING_HEAD, 0, 0, w);
            c.occupy(true, w);
        }
    }

    /** Both arms to a pose (the main arm's row and the off arm's), by weight. */
    private static void pose(PoseContext c, float[] m, float[] o, float w) {
        c.arm(true, m[0], m[1], m[2], w);
        c.arm(false, o[0], o[1], o[2], w);
    }

    private SpiderPoses() {
    }
}
