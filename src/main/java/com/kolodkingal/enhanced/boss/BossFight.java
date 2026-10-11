package com.kolodkingal.enhanced.boss;

/**
 * One running boss fight. A placeholder until the boss framework is built: it will hold what every boss shares (the
 * fighters, the ledger of who did what, the two boss bars, wipe and abandon, the rewards) and be extended by the three
 * fights. Nothing creates one yet, so {@link BossFights} knows no fight.
 */
public abstract class BossFight {
    /** Which villain this fight is against. */
    protected abstract BossKind kind();
}
