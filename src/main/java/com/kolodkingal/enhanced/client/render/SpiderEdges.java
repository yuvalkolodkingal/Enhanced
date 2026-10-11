package com.kolodkingal.enhanced.client.render;

/**
 * The rules by which the animator starts the three gestures of Spider-Man that no key and no change in his synced data
 * stands for (DESIGN section 14, "Animation"): the landing after a fast fall, the push off a wall or a ceiling, and the
 * grab of a wall at the end of a zip. They are told from what is seen of his motion tick by tick, so they are pure
 * arithmetic over a few numbers and a small memory, which the game tests can drive without a client.
 */
public final class SpiderEdges {
    /** A cling that begins within this many ticks after a zip fell is the arrival of the zip. */
    public static final int ARRIVAL_TICKS = 6;
    /**
     * The upward speed (blocks per tick) a cling must end with to be a push off the surface and not a slip or the climb
     * over an edge (he crawls at 0.22, up to 0.3 with his legs out; a leap leaves at 0.9). Other players' cling flags
     * come a few ticks after the push, when the leap has slowed to about 0.6.
     */
    public static final float LEAP_RISE = 0.35F;
    /** A landing is made only after at least this many ticks in the air. */
    public static final int LAND_MIN_AIR = 6;
    /**
     * From this fall speed (blocks per tick) he lands softly, bending his knees; from {@link #HARD_FALL} like a hero.
     * His own jump (about two blocks high) comes down at 0.58, so that is no landing at all; a Black Eyes jump (0.66)
     * bends his knees, and the hero's landing starts at a drop of about four blocks.
     */
    public static final float SOFT_FALL = 0.62F;
    public static final float HARD_FALL = 0.78F;
    /**
     * The web, a zip or a cling that ended this recently makes the touch-down part of that, not a landing of its own.
     */
    public static final int WEB_GRACE = 3;

    /** What the motion of this tick starts. */
    public enum Event {
        NONE, LEAP, ARRIVE
    }

    /** How he lands. */
    public enum Landing {
        /** Not at all: the dip of every landing is enough. */
        NONE,
        /** A short bend of the knees ({@link GestureId#LAND_SOFT}). */
        SOFT,
        /** The superhero landing ({@link GestureId#SPIDER_LAND}). */
        HARD
    }

    /** What was seen of a hero the tick before. */
    public static final class Memory {
        private boolean clinging;
        /** Ticks since the last tick on which he was zipping (0: the tick before), saturating. */
        private int sinceZip = 100;
        /** Ticks since the last tick on which he was zipping, swinging or clinging, saturating. */
        private int sinceWeb = 100;

        /** The ticks since he last held on to a web or a surface. */
        public int sinceWeb() {
            return sinceWeb;
        }

        public void clear() {
            clinging = false;
            sinceZip = 100;
            sinceWeb = 100;
        }
    }

    /**
     * One tick of his motion. {@code rise} is how fast he moved up this tick (blocks per tick, negative falling).
     * A cling that begins just after a zip is {@link Event#ARRIVE}; a cling that ends with an upward push, and not
     * because a zip or a swing took him off it, is {@link Event#LEAP}.
     */
    public static Event step(Memory m, boolean clinging, boolean zipping, boolean swinging, float rise) {
        boolean recentZip = zipping || m.sinceZip < ARRIVAL_TICKS;
        Event event = Event.NONE;
        if (clinging && !m.clinging && recentZip) {
            event = Event.ARRIVE;
        } else if (!clinging && m.clinging && rise > LEAP_RISE && !zipping && !swinging) {
            event = Event.LEAP;
        }
        m.sinceZip = zipping ? 0 : Math.min(m.sinceZip + 1, 100);
        m.sinceWeb = zipping || swinging || clinging ? 0 : Math.min(m.sinceWeb + 1, 100);
        m.clinging = clinging;
        return event;
    }

    /**
     * How he lands when his feet touch the ground after {@code airTicks} ticks in the air at {@code fall} blocks per
     * tick downward: a hard fall is the superhero landing, a firm one a bend of the knees, and a short hop or the
     * touch-down that ends a zip or a climb nothing of its own.
     */
    public static Landing landing(int airTicks, float fall, int sinceWeb) {
        if (airTicks < LAND_MIN_AIR || sinceWeb <= WEB_GRACE) {
            return Landing.NONE;
        }
        return fall >= HARD_FALL ? Landing.HARD : fall >= SOFT_FALL ? Landing.SOFT : Landing.NONE;
    }

    private SpiderEdges() {
    }
}
