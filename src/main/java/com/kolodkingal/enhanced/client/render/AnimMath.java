package com.kolodkingal.enhanced.client.render;

/**
 * The maths of the hero animation: frame-rate independent easing, exact springs, smoothstep and the easing curves of
 * the gesture tables. Pure (no Minecraft imports), so the game tests can check it.
 *
 * <p>Everything that eases takes the frame's {@code dt} in ticks and an exponential time constant {@code tau}, so a
 * pose settles in the same time at 30 or at 240 frames per second.
 */
public final class AnimMath {

    /** The clamped smoothstep: 0 before {@code a}, 1 after {@code b}, an S curve between. */
    public static float ss(float a, float b, float x) {
        float u = clamp((x - a) / (b - a), 0.0F, 1.0F);
        return u * u * (3.0F - 2.0F * u);
    }

    public static float clamp(float x, float min, float max) {
        return x < min ? min : Math.min(x, max);
    }

    public static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }

    /** Exponential approach, exact for any dt: reaches 63 percent of the way in {@code tau} ticks. {@code tau <= 0} means at once. */
    public static float ease(float cur, float target, float dt, float tau) {
        return tau <= 0.0F ? target : cur + (target - cur) * (1.0F - (float) Math.exp(-dt / tau));
    }

    /** As {@link #ease(float, float, float, float)} with one time constant for rising (away from 0) and one for falling. */
    public static float ease(float cur, float target, float dt, float tauIn, float tauOut) {
        return ease(cur, target, dt, Math.abs(target) >= Math.abs(cur) ? tauIn : tauOut);
    }

    public static float easeOutCubic(float u) {
        float v = 1.0F - u;
        return 1.0F - v * v * v;
    }

    public static float easeInCubic(float u) {
        return u * u * u;
    }

    public static float easeInOutSine(float u) {
        return (1.0F - (float) Math.cos(Math.PI * u)) * 0.5F;
    }

    /** Ease-out-back: overshoots by about 10 percent and settles. */
    public static float easeOutBack(float u) {
        float v = u - 1.0F;
        return 1.0F + 2.70158F * v * v * v + 1.70158F * v * v;
    }

    /** The easing codes of the gesture tables (the segment arriving at a key uses that key's code). */
    public enum Ease {
        /** Smoothstep. */
        S,
        /** Ease-out cubic: a snap, then a settle. */
        O,
        /** Ease-in cubic: accelerates into a release. */
        I,
        /** Linear. */
        L,
        /** Ease-in-out sine. */
        E,
        /** Ease-out-back. */
        B;

        public float apply(float u) {
            return switch (this) {
                case S -> u * u * (3.0F - 2.0F * u);
                case O -> easeOutCubic(u);
                case I -> easeInCubic(u);
                case L -> u;
                case E -> easeInOutSine(u);
                case B -> easeOutBack(u);
            };
        }
    }

    /** A spring: position {@link #x} and velocity {@link #v}; the target is passed to each step. */
    public static final class Spring {
        public float x;
        public float v;

        /** Critically damped, exact for any dt (no overshoot): {@code omega} in radians per tick, {@code 2 / smoothTime}. */
        public void critical(float target, float omega, float dt) {
            float d = x - target;
            float e = (float) Math.exp(-omega * dt);
            float c = v + omega * d;
            x = target + (d + c * dt) * e;
            v = (v - omega * c * dt) * e;
        }

        /** Under-damped ({@code zeta < 1}), exact for any dt: overshoots by {@code exp(-pi zeta / sqrt(1 - zeta^2))}. */
        public void damped(float target, float omega, float zeta, float dt) {
            float d = x - target;
            float wd = omega * (float) Math.sqrt(1.0F - zeta * zeta);
            float e = (float) Math.exp(-zeta * omega * dt);
            float cs = (float) Math.cos(wd * dt);
            float sn = (float) Math.sin(wd * dt);
            float b = (v + zeta * omega * d) / wd;
            float nx = target + e * (d * cs + b * sn);
            float nv = e * (v * cs - ((zeta * omega * v + omega * omega * d) / wd) * sn);
            x = nx;
            v = nv;
        }

        public void snap(float value) {
            x = value;
            v = 0.0F;
        }
    }

    /** A gesture's weight at time {@code t}: an ease-out attack of {@code a} ticks, a full hold, an ease-in-out release of {@code r} ticks. */
    public static float envelope(float t, float a, float dur, float r) {
        if (t <= 0.0F || t >= dur) {
            return 0.0F;
        }
        if (t < a) {
            return easeOutCubic(t / a);
        }
        float releaseStart = dur - r;
        return t < releaseStart ? 1.0F : 1.0F - easeInOutSine((t - releaseStart) / r);
    }

    private AnimMath() {
    }
}
