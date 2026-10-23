package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * The rewards and refunds that could not be handed over when they were earned (the player was dead or offline), kept in
 * the world's {@link BossData} until the player can have them. {@link BossEvents} delivers them at login, respawn and
 * dimension change. Nothing is lost if the server stops or crashes: they are saved with the Overworld.
 */
public final class BossGifts {

    /** Keeps a stack for the player until they can have it. */
    public static void add(MinecraftServer server, UUID who, ItemStack stack) {
        if (!stack.isEmpty()) {
            store(server, data(server).withGift(who, stack));
        }
    }

    /** The stacks waiting for the player. */
    public static List<ItemStack> pending(MinecraftServer server, UUID who) {
        return data(server).giftsOf(who).stream().map(ItemStack::copy).toList();
    }

    /**
     * Hands the player everything that waits for them (into the inventory, or at their feet for them alone).
     *
     * @return how many stacks were handed over
     */
    public static int deliver(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || !player.isAlive()) {
            return 0;
        }
        List<ItemStack> waiting = pending(server, player.getUUID());
        if (waiting.isEmpty()) {
            return 0;
        }
        store(server, data(server).withoutGifts(player.getUUID()));
        for (ItemStack stack : waiting) {
            BossRewards.give(player, stack);
        }
        player.displayClientMessage(Component.translatable("message.enhanced.boss.gift"), true);
        return waiting.size();
    }

    static BossData data(MinecraftServer server) {
        return ModAttachments.BOSS_DATA.get(server.overworld());
    }

    static void store(MinecraftServer server, BossData data) {
        ModAttachments.BOSS_DATA.set(server.overworld(), data);
    }

    private BossGifts() {
    }
}
