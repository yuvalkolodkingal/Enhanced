package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Reading the grimoires (3.17): the Darkhold (its forbidden spells: dreamwalking, the power siphon, soul binding,
 * necromancy; they corrupt), the Book of the Vishanti (its spells of light) and Cagliostro's grimoire (its spells of
 * time). The first time a caster (a witch, a sorcerer, Billy, a hero sorcerer) uses one, he reads it and its spells are
 * his ({@link MagicData}'s flags); after that the book does what it always did. Both sides answer alike, the client from
 * the synced magic, so a use is never half taken.
 */
public final class Grimoires {
    private Grimoires() {
    }

    /** Whether this use of a grimoire is the reading of it (and, on the server, reads it). */
    public static boolean read(Player player, int flag, Component book) {
        if (!Magic.isCaster(player) || Magic.data(player).has(flag)) {
            return false;
        }
        if (player instanceof ServerPlayer server) {
            Magic.set(server, Magic.data(server).with(flag));
            ServerLevel level = server.serverLevel();
            boolean dark = flag == MagicData.DARKHOLD;
            int color = dark ? 0x8A2FD6 : flag == MagicData.CAGLIOSTRO ? 0x5FE07A : 0xFFF6C8;
            MagicFx.sendOn(level, MagicFx.AWAKEN, server, List.of(), color, 0xFFFFFF, 1.0F, 30);
            MagicFx.spiral(level, server.position(), 0.8, server.getBbHeight(), color, 1.0F);
            level.playSound(null, server.getX(), server.getY(), server.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.PLAYERS, 1.0F, dark ? 0.6F : 1.2F);
            level.playSound(null, server.getX(), server.getY(), server.getZ(), dark ? SoundEvents.SCULK_SHRIEKER_SHRIEK
                    : SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, dark ? 0.8F : 1.4F);
            server.displayClientMessage(Component.translatable("message.enhanced.grimoire.read", book)
                    .withStyle(s -> s.withColor(color).withBold(true)), false);
            server.displayClientMessage(Component.translatable(dark ? "message.enhanced.grimoire.read.darkhold"
                    : "message.enhanced.grimoire.read.spells").withStyle(dark ? ChatFormatting.DARK_PURPLE : ChatFormatting.GRAY), false);
            Powers.syncIfDirty(server);
        }
        return true;
    }
}
