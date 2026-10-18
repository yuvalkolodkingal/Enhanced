package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import net.minecraft.util.Mth;

import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * The flight layer of the pose: three anchor poses per hero (hover, cruise, sprint) blended by the normalised speed, with
 * the hover drift, the leg flutter at speed, the lean of a slow climb or descent and the lean back of a reverse on top,
 * all scaled by the weight of the air. Thor's weapon arm is held by its world elevation, so it leads the flight at every
 * lean: up to the sky while hovering, thrust ahead at cruise.
 */
final class FlightPoses {
    private static final float[] POSE = new float[10];

    static void apply(PoseContext c) {
        HeroAnimState st = c.st;
        float w = st.wAir;
        if (w <= 0.001F) {
            return;
        }
        FlightProfile profile = FlightProfile.of(st);
        if (profile == null) {
            return;
        }
        float s = st.s;
        float age = c.age;
        float calm = EnhancedClientConfig.reducedMotion() ? 0.3F : 1.0F;
        profile.blend(s, POSE);
        float mx = POSE[0];
        float oy = POSE[4];
        float my = POSE[1];
        float mz = POSE[2];
        float ox = POSE[3];
        float oz = POSE[5];
        float rx = POSE[6];
        float rz = POSE[7];
        float lx = POSE[8];
        float lz = POSE[9];

        // Backwards: the hover pose with the arms and legs trailing forward.
        float back = ss(0.1F, 0.6F, -s);
        float armBack = -20.0F * back;
        float legBack = -10.0F * back;

        // Hovering or slow: the arms drift on the air, the legs sway.
        float hover = (1.0F - ss(0.2F, 0.8F, st.sH)) * calm;
        float dx = profile.armDrift * hover;
        float mDriftX = dx * Mth.sin(0.07F * age);
        float oDriftX = dx * Mth.sin(0.07F * age + 1.7F);
        float mDriftZ = profile.mWorld ? 0.0F : dx * Mth.sin(0.09F * age);
        float oDriftZ = dx * Mth.sin(0.09F * age + 1.7F);
        float rSway;
        float lSway;
        if (profile.mWorld) {
            // Thor treads air: his legs alternate.
            float tread = profile.legSway * hover * Mth.sin(0.15F * age);
            rSway = tread;
            lSway = -tread;
        } else {
            float sway = profile.legSway * hover * Mth.sin(0.07F * age);
            rSway = sway;
            lSway = sway;
        }

        // A slow climb or descent: arms in or out, legs up or forward.
        float vertical = ss(0.3F, 0.8F, Math.abs(st.u)) * (1.0F - ss(0.3F, 0.8F, s));
        float armIn = 0.0F;
        float legV = 0.0F;
        if (st.u > 0.0F) {
            armIn = -6.0F * vertical;
            legV = 4.0F * vertical;
        } else if (st.u < 0.0F) {
            armIn = 8.0F * vertical;
            legV = -6.0F * vertical;
        }

        // At speed the legs flutter in the wind.
        float flutter = profile.legFlutter * ss(0.5F, 1.5F, s) * Mth.sin(0.45F * age);

        float mxRot = profile.mWorld ? c.worldX(mx) + mDriftX : mx + mDriftX + armBack;
        c.arm(true, mxRot, my, mz + (profile.mWorld ? 0.0F : mDriftZ + armIn), w);
        c.arm(false, ox + oDriftX + armBack, oy, oz + oDriftZ - armIn, w);
        c.leg(true, rx + rSway + legBack + legV + flutter, rz, w);
        c.leg(false, lx + lSway + legBack + legV - flutter, lz, w);
    }

    private FlightPoses() {
    }
}
