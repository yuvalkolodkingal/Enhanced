package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.mixin.client.MinecraftAccessor;
import com.kolodkingal.enhanced.mixin.client.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Quicksilver's hands are quick: while one of his modes is on, Minecraft's own client-side pauses between one block
 * and the next (after a right click, and after breaking a block) are cut by the same factor he does everything else
 * faster by, so he can place and break blocks one after another without waiting. (How long a block takes to break
 * is his block break speed.)
 */
final class QuickHands {
    /** Minecraft's pause (ticks) after a right click: holding the button places a block every 4 ticks. */
    private static final int RIGHT_CLICK_DELAY = 4;
    /** Minecraft's pause (ticks) after a block is broken, before the next one starts breaking. */
    private static final int DESTROY_DELAY = 5;

    static void tick(Minecraft mc, LocalPlayer player) {
        if (mc.gameMode == null || !QuicksilverAbilities.isQuick(player)) {
            return;
        }
        double speed = QuicksilverAbilities.actionSpeed();
        MinecraftAccessor minecraft = (MinecraftAccessor) mc;
        int rightClick = (int) (RIGHT_CLICK_DELAY / speed);
        minecraft.enhanced$setRightClickDelay(Math.min(minecraft.enhanced$getRightClickDelay(), rightClick));
        MultiPlayerGameModeAccessor gameMode = (MultiPlayerGameModeAccessor) mc.gameMode;
        int destroy = (int) (DESTROY_DELAY / speed);
        gameMode.enhanced$setDestroyDelay(Math.min(gameMode.enhanced$getDestroyDelay(), destroy));
    }

    private QuickHands() {
    }
}
