package com.kolodkingal.enhanced.platform.ext;

/**
 * The loader's entity hooks the mod's entities answer (port P0, PLAN2 3.3), with the exact signatures of NeoForge's
 * {@code IEntityExtension} methods of the same names (see {@link ItemExt}). Fabric (P1): mixins on the level's entity
 * callbacks; note that on NeoForge {@code Entity} itself has these methods, and an override that calls
 * {@code super.onAddedToLevel()} keeps NeoForge's bookkeeping.
 */
public final class EntityExt {
    private EntityExt() {
    }

    /** The entity joins and leaves its level's ticking list. */
    public interface LevelPresence {
        void onAddedToLevel();

        void onRemovedFromLevel();
    }
}
