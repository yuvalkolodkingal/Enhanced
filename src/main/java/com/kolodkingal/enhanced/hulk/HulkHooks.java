package com.kolodkingal.enhanced.hulk;

import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Where other lanes' code calls the Hulk's (3.6). Written empty by the 3.6 skeleton; lane L7 fills the bodies, keeping
 * the signatures.
 */
public final class HulkHooks {
    /** Weakness II for this long after his Snap (ticks). */
    public static final int WEAKNESS_TICKS = 1200;

    /**
     * The Hulk has just snapped with a full Infinity Gauntlet (lane L1 calls it after the Snap): the cost is his own,
     * his right arm burned ({@code HulkState.armBurnedUntil}, Weakness II), instead of the 2-health cost of a Bearer
     * (Endgame).
     */
    public static void onSnap(ServerPlayer hulk) {
        long until = hulk.level().getGameTime() + 20L * HulkAbilities.cfg(HulkConfig.ARM_BURNED_SECONDS, 600);
        hulk.setData(ModAttachments.HULK, HulkRage.get(hulk).withArmBurnedUntil(until));
        hulk.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 1));
        hulk.displayClientMessage(Component.translatable("message.enhanced.hulk.arm_burned")
                .withStyle(ChatFormatting.GOLD), true);
    }

    /** Whether the Hulk's right arm is still burned from his Snap (both sides: the state is synced to him). */
    public static boolean armBurned(net.minecraft.world.entity.player.Player player) {
        return HulkRage.get(player).armBurnedUntil() > player.level().getGameTime();
    }

    private HulkHooks() {
    }
}
