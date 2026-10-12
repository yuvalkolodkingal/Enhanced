package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.effect.RadioactiveBiteEffect;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Radioactive Spider: a small spider sealed in a jar, glowing sickly green (DESIGN section 14). Used by a player with
 * no power, it bites: they take one point of damage (never the last), are sick for {@link SpiderConfig#BITE_DELAY_TICKS}
 * ticks (the effect Radioactive Bite, with Nausea, Weakness and Slowness, and Poison for the last three seconds) and,
 * when the sickness ends, become Spider-Man (see {@code SpiderEvents}). Milk, death or another power in between cancel
 * it. Anyone else is turned away without using the spider up; Creative players are bitten and keep it.
 */
public class RadioactiveSpiderItem extends Item {
    public RadioactiveSpiderItem(Properties properties) {
        super(properties);
    }

    /**
     * Why the player cannot be bitten, or null if they can: a power of their own (it does not mix, whether it is
     * Spider-Man's or another's) or a bite still making them sick.
     */
    @Nullable
    public static Component refusal(Player player) {
        if (Powers.power(player).hasPowers()) {
            return Component.translatable("message.enhanced.bite.not_now");
        }
        if (player.hasEffect(ModEffects.RADIOACTIVE_BITE)) {
            return Component.translatable("message.enhanced.bite.sick");
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Component refusal = refusal(player);
        if (refusal != null) {
            if (!level.isClientSide) {
                player.displayClientMessage(refusal, true);
            }
            // The click is spent on the refusal: the game must not go on to use what the other hand holds (Loki's
            // Scepter there would tear a hero apart).
            return InteractionResultHolder.consume(stack);
        }
        if (level.isClientSide || !(player instanceof ServerPlayer bitten)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        bite(bitten);
        stack.consume(1, bitten);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /**
     * The bite: a sound and green motes, one point of damage (the bite never kills), and the sickness that ends in
     * Spider-Man.
     */
    public static void bite(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SPIDER_BITE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(new EnergyParticleOptions(RadioactiveBiteEffect.GREEN, 0.9F), player.getX(), player.getY() + 1.1, player.getZ(),
                14, 0.25, 0.25, 0.25, 0.06);
        player.setHealth(Math.max(1.0F, player.getHealth() - 1.0F));
        int delay = SpiderConfig.BITE_DELAY_TICKS.get();
        player.addEffect(new MobEffectInstance(ModEffects.RADIOACTIVE_BITE, delay, 0));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, delay, 0));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, delay, 0));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, delay, 0));
        if (delay <= RadioactiveBiteEffect.POISON_TICKS) {
            // The sickness is short: the poison is all of it.
            player.addEffect(new MobEffectInstance(MobEffects.POISON, delay, 0));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.radioactive_spider.desc").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.enhanced.radioactive_spider.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.radioactive_spider.desc3").withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
