package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.WitchcraftConfig;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * The Darkhold's price (3.17): every forbidden spell darkens the caster's magic ({@link MagicData#corruption}, out of
 * 100). Corrupted magic is darker to look at ({@link Magic#corrupt}), hurts more ({@link Magic#corruptionBonus}) and drains
 * the caster's own life with every spell. The light of the Vishanti, an antidote, the Unbinding and the Coven's Road
 * cleanse it.
 */
public final class Darkness {
    private Darkness() {
    }

    /** After a spell: a forbidden one corrupts; any spell of a corrupted caster costs him some of his life. */
    static void afterSpell(ServerPlayer player, Spell spell) {
        MagicData data = Magic.data(player);
        int corruption = data.corruption();
        if (spell.forbidden()) {
            int add = EnhancedConfig.SPEC.isLoaded() ? WitchcraftConfig.CORRUPTION_PER_SPELL.get() : 6;
            int next = Math.min(100, corruption + add);
            Magic.set(player, data.withCorruption(next));
            if (corruption < 50 && next >= 50) {
                player.displayClientMessage(Component.translatable("message.enhanced.darkhold.corrupting").withStyle(ChatFormatting.DARK_PURPLE), false);
            }
            corruption = next;
        }
        float drain = spell.forbidden() ? 1.0F + corruption / 25.0F : corruption / 50.0F;
        if (drain >= 0.5F && !player.getAbilities().invulnerable) {
            player.hurt(ModDamageTypes.source(player.level(), ModDamageTypes.DARK_SPELL), Math.min(drain, Math.max(0.0F, player.getHealth() - 1.0F)));
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SOUL_ESCAPE.value(),
                    SoundSource.PLAYERS, 0.8F, 0.7F);
        }
    }

    /** Takes {@code amount} of the corruption away; true if there was any. */
    public static boolean cleanse(ServerPlayer player, int amount) {
        MagicData data = Magic.data(player);
        if (data.corruption() <= 0) {
            return false;
        }
        Magic.set(player, data.withCorruption(Math.max(0, data.corruption() - amount)));
        return true;
    }
}
