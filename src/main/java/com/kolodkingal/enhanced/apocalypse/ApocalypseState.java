package com.kolodkingal.enhanced.apocalypse;

/**
 * What the boss is doing, as the clients animate it: the one state of his controller, with the game time it began (both
 * synced), from which they draw every tell, every pose and the set pieces of the fight. The ids are sent as bytes and
 * never change.
 */
public enum ApocalypseState {
    /** Standing or walking; the body animates the walk from his speed. */
    IDLE(0, 0),
    /** The portal has opened and he has not stepped out: dark eyes, arms at his sides. */
    AWAKENING(1, 0),
    /** The right hand raised, the fingers pinched: five thin glint lines to the target. */
    TELL_LANCE(2, 14),
    /** Both arms sweep low from the sides: a ridge rises along a lane. */
    TELL_WAVE(3, 22),
    /** A fist raised and slammed down: an amber ring under each target. */
    TELL_SPIKES(4, 24),
    /** Both arms rise: three blocks of sand tear out of the ground and orbit over his head. */
    TELL_BOULDERS(5, 30),
    /** The right hand reaches for one player: a stripe of sand crawls to their feet. */
    TELL_ENTOMB(6, 30),
    /** Two fingers to the temple, the eyes flash violet, a ring contracts. */
    TELL_MIND_CRUSH(7, 40),
    /** Violet shards swirl at his feet and a second ring opens at the exit. */
    TELL_PORTAL(8, 16),
    /** The arm drawn across the chest. */
    BACKHAND(9, 8),
    /** Both arms raised, the sky above him darkens, dust spirals up at the spots. */
    TELL_TORNADO(10, 40),
    /** Kneeling: the shield has broken (a window), or the Escape was interrupted. */
    STAGGERED(11, 0),
    /** Floating, arms wide and up: a transition (the Horsemen, the Cairo Collapse). */
    HOVER(12, 0),
    /** The right hand raised to the sky, a portal growing over him. */
    ESCAPING(13, 0),
    /** Kneeling, the head back, the cracks of the cap blazing gold: "All is revealed." */
    KNEELING(14, 0),
    /** The arms crossed, then flung out as the plates fly. */
    ARMOUR_FALL(15, 0),
    /** Slumping while the body dissolves in sand or in the Phoenix's fire. */
    DYING(16, 0),
    /** Stepping into his portal: the fight is lost, and he leaves. */
    WITHDRAWING(17, 0);

    private final byte id;
    private final int tell;

    ApocalypseState(int id, int tell) {
        this.id = (byte) id;
        this.tell = tell;
    }

    public byte id() {
        return id;
    }

    /** The length in ticks of this state when it is a tell (0 for the others). */
    public int tellTicks() {
        return tell;
    }

    public boolean isTell() {
        return tell > 0 && this != BACKHAND;
    }

    public static ApocalypseState byId(byte id) {
        for (ApocalypseState state : values()) {
            if (state.id == id) {
                return state;
            }
        }
        return IDLE;
    }
}
