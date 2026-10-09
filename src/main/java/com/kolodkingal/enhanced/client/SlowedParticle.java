package com.kolodkingal.enhanced.client;

/** Implemented by every particle (through a mixin): where it is in its slowed-down tick cycle, or -1 if it isn't slowed. */
public interface SlowedParticle {
    int enhanced$slowPhase();

    void enhanced$setSlowPhase(int phase);
}
