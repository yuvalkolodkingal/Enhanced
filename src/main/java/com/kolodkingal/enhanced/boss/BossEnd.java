package com.kolodkingal.enhanced.boss;

import java.util.Locale;

/**
 * How a boss fight ends, and what each ending means for the players: who is rewarded and whether the ritual item that
 * started the fight comes back. The rule for the refund is the one of the three boss specs: it is given back when the
 * fight ends for a reason that is not the players' doing (an operator, the server, the difficulty, the boss itself
 * withdrawing); a fight lost or abandoned by the players costs them the item.
 */
public enum BossEnd {
    /** The boss fell: the fighters are rewarded. */
    VICTORY(true, false),
    /** Every fighter was dead or gone for {@code wipeSeconds}: the boss leaves, nobody is rewarded. */
    WIPE(false, false),
    /** Nobody was near the boss for {@code abandonSeconds}: it leaves. */
    ABANDON(false, false),
    /** The fight lasted {@code maxFightMinutes}: the boss leaves (a fail-safe against a stuck fight). */
    TIME_OUT(false, false),
    /** The boss was killed or removed by other means (a {@code /kill}, a command): no reward, no refund. */
    ABORT(false, false),
    /** The boss itself sends the fight away for a reason that was not the players' doing (nobody can hurt it). */
    WITHDRAW(false, true),
    /** An operator ended it ({@code /enhanced boss stop}). */
    COMMAND(false, true),
    /** The difficulty was set to Peaceful, which would make the boss vanish. */
    PEACEFUL(false, true),
    /** The server is stopping: no fight survives a restart. */
    SERVER_STOPPING(false, true);

    private final boolean rewards;
    private final boolean refunds;

    BossEnd(boolean rewards, boolean refunds) {
        this.rewards = rewards;
        this.refunds = refunds;
    }

    /** Whether the qualifying fighters are rewarded. */
    public boolean rewards() {
        return rewards;
    }

    /** Whether the ritual item the fight consumed is given back to whoever started it. */
    public boolean refunds() {
        return refunds;
    }

    /** The key of the message the fighters are told when the fight ends this way (victory has its own). */
    public String messageKey() {
        return "message.enhanced.boss.end." + name().toLowerCase(Locale.ROOT);
    }
}
