package com.kolodkingal.enhanced.ultron;

import org.jetbrains.annotations.Nullable;

/**
 * The lift that is installed (see {@link LiftHook}): the Tremor until the Sokovia lane installs its own, once, when the
 * mod loads. A test that installs its own puts the old one back with the result of {@link #install}.
 */
public final class LiftHooks {
    private static volatile LiftHook current = LiftHook.TREMOR;

    /** The lift the fights use. */
    public static LiftHook get() {
        return current;
    }

    /**
     * Installs a lift (null puts the Tremor back).
     *
     * @return the lift that was installed before
     */
    public static LiftHook install(@Nullable LiftHook hook) {
        LiftHook old = current;
        current = hook == null ? LiftHook.TREMOR : hook;
        return old;
    }

    private LiftHooks() {
    }
}
