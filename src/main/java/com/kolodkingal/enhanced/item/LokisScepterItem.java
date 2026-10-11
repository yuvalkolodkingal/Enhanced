package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.effect.ConfusedEffect;
import com.kolodkingal.enhanced.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Loki's Scepter, with the Mind Stone in its blue gem. Using it means touching the stone: with the Scarlet Witch
 * Gene in your other hand you awaken as the Scarlet Witch, with a Synthezoid Core you become Vision; with neither,
 * the stone's energy tears you apart. It is also a melee weapon. In the films it takes over the mind of whoever it
 * touches; that is too much, so a hit only confuses: a player gets Nausea and Slowness, a creature the
 * {@link ConfusedEffect confused} effect (the bosses shrug it off). Whenever the stone is touched or lands a blow it
 * sings with its own voice (see {@link InfinityStone}), and it hums softly for as long as someone holds the scepter.
 * <p>
 * What touching the stone does with another item in the other hand (the genes, the Synthezoid Core, Ultron's Core and
 * Program, the Tesseract) is one table, {@link ScepterRituals}; this class only hands the use, and the five-second hold
 * that calls the Mad Titan, over to it.
 */
public class LokisScepterItem extends Item {
    public LokisScepterItem(Properties properties) {
        super(properties);
    }

    /** 8 attack damage, 1.6 attack speed (like a sword). */
    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 7.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack scepter = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (ScepterRituals.useScepter(serverPlayer, hand, false)) {
                // The Mad Titan is being called: five seconds of holding it.
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(scepter);
            }
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(scepter, level.isClientSide());
    }

    /** Sneak + use on a block with Ultron's Program in the other hand wakes Ultron; every other use is {@link #use}. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer player && ScepterRituals.useScepterOnBlock(player, context.getHand())) {
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        return InteractionResult.PASS;
    }

    /** The player touches the Mind Stone with their {@code scepterHand}; the other hand may hold the gene. */
    public static void touchMindStone(ServerPlayer player, InteractionHand scepterHand) {
        ScepterRituals.useScepter(player, scepterHand, false);
    }

    // The five seconds that call the Mad Titan (with the Tesseract in the other hand): see ScepterRituals.

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ScepterRituals.HOLD_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        ScepterRituals.holdTick(entity, stack, remainingUseDuration);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        ScepterRituals.holdReleased(entity, stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ScepterRituals.holdFinished(entity, stack);
        return stack;
    }

    /** A melee hit that landed: whoever it hit is left confused. */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        confuse(target, attacker);
        return true;
    }

    /**
     * The Mind Stone muddles whoever the Scepter strikes. A player gets Nausea and Slowness I; any other creature the
     * confused effect, unless it is too powerful (see {@code enhanced:confused_immune}). The durations are in the
     * config.
     */
    public static void confuse(LivingEntity target, LivingEntity attacker) {
        if (target.level().isClientSide() || target instanceof ArmorStand || !target.isAlive()) {
            return; // (a creature the blow just killed is left alone)
        }
        boolean muddled;
        if (target instanceof Player) {
            int nausea = EnhancedConfig.SCEPTER_NAUSEA_TICKS.get();
            int slowness = EnhancedConfig.SCEPTER_SLOWNESS_TICKS.get();
            // Magneto's helmet turns the Nausea away; the Slowness still lands.
            muddled = nausea > 0 && target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nausea, 0), attacker);
            muddled |= slowness > 0 && target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowness, 0), attacker);
        } else {
            int ticks = EnhancedConfig.SCEPTER_CONFUSION_TICKS.get();
            muddled = ticks > 0 && target.addEffect(new MobEffectInstance(ModEffects.CONFUSED, ticks, 0, false, false, true), attacker);
        }
        if (muddled && target.level() instanceof ServerLevel level) {
            ConfusedEffect.burst(level, target);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.lokis_scepter.desc").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.enhanced.lokis_scepter.desc2").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.enhanced.lokis_scepter.desc3").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
