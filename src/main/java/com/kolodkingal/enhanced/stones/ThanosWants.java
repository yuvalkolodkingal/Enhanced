package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The Mad Titan and the real stones (3.6): from his second stone on, a fighter who carries a real Infinity Stone (bare
 * or in a Gauntlet) is the one he goes after first (after a Vision), and he says so once per fight to each of them:
 * "You carry something that is mine." His sword, meteors and orbs stay {@code reality_proof}: he holds the Reality
 * Stone himself. Server only.
 */
public final class ThanosWants {
    /** How much a stone carrier weighs in his choice of target (a Vision weighs 1000, a blow 10 a point). */
    public static final double PREFERENCE = 500.0;
    /** From this many stones (his second phase) he wants them back. */
    public static final int FROM_STONES = 2;

    /** The fights, and whom each has already told. */
    private static final Map<BossFight, Set<UUID>> TOLD = new WeakHashMap<>();

    /** What the player adds to his score as Thanos's target ({@code ThanosEntity.chooseTarget}). */
    public static double preference(ThanosEntity thanos, ServerPlayer player) {
        if (thanos.stones() < FROM_STONES || !StoneUse.carriesAnyStone(player)) {
            return 0.0;
        }
        BossFight fight = thanos.fight();
        if (fight != null && TOLD.computeIfAbsent(fight, f -> new HashSet<>()).add(player.getUUID())) {
            fight.speak(Component.translatable("entity.enhanced.thanos"), StoneKind.POWER.color(),
                    Component.translatable("message.enhanced.stones.thanos.mine"), null);
        }
        return PREFERENCE;
    }

    static void clear() {
        TOLD.clear();
    }

    private ThanosWants() {
    }
}
