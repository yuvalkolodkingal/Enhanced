package com.kolodkingal.enhanced.platform.client;

/**
 * When a key mapping is read, and which other mappings its key may clash with (port P0, PLAN2 3.3). {@link #IN_GAME}
 * (no screen open), {@link #UNIVERSAL} (always; clashes with every context) and {@link #GUI} (a screen open) are the
 * loader's own contexts: on NeoForge they are {@code KeyConflictContext}'s three, so other mods' keys clash with the
 * mod's as they did. A mod's own context implements this interface (an enum of contexts works well: compare with
 * {@code ==}); the loader hands it every other mapping's context as a {@code KeyContext}, its own three as these
 * constants. Client only.
 */
public interface KeyContext {
    /** No screen open. */
    KeyContext IN_GAME = Vanilla.IN_GAME;
    /** Always active; in conflict with every context. */
    KeyContext UNIVERSAL = Vanilla.UNIVERSAL;
    /** A screen open. */
    KeyContext GUI = Vanilla.GUI;

    /** Whether the mappings of this context are read now. */
    boolean isActive();

    /** Whether a mapping of this context and one of {@code other} on the same key clash. */
    boolean conflicts(KeyContext other);

    /** The loader's three contexts; what they do is the loader's (NeoForge: {@code KeyConflictContext}). */
    enum Vanilla implements KeyContext {
        UNIVERSAL,
        GUI,
        IN_GAME;

        @Override
        public boolean isActive() {
            return ClientServices.get().isActive(this);
        }

        @Override
        public boolean conflicts(KeyContext other) {
            return ClientServices.get().conflicts(this, other);
        }
    }
}
