package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.effect.ConfusedEffect;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
            touchMindStone(serverPlayer, hand);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(scepter, level.isClientSide());
    }

    /** The player touches the Mind Stone with their {@code scepterHand}; the other hand may hold the gene. */
    public static void touchMindStone(ServerPlayer player, InteractionHand scepterHand) {
        ServerLevel level = player.serverLevel();
        Power power = Powers.power(player);
        if (power == Power.SCARLET_WITCH || power == Power.VISION) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.recognizes").withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
            InfinityStone.MIND.use(level, player.position(), InfinityStone.UNDER, 1.0F);
            return;
        }

        InteractionHand otherHand = scepterHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack gene = player.getItemInHand(otherHand);
        if (gene.is(ModItems.SCARLET_WITCH_GENE.get())) {
            if (power.hasPowers()) {
                player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.already_powered").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            InfinityStone.MIND.use(level, player.position(), InfinityStone.UNDER, 1.0F);
            ScarletWitchGeneItem.awaken(player, gene);
            return;
        }

        ItemStack core = player.getItemInHand(otherHand);
        if (core.is(ModItems.SYNTHEZOID_CORE.get())) {
            if (power.hasPowers()) {
                player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.already_powered").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            core.consume(1, player);
            InfinityStone.MIND.use(level, player.position(), InfinityStone.UNDER, 1.0F);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(player.getX(), player.getY(), player.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            Powers.awaken(player, Power.VISION);
            return;
        }

        // No gene or synthezoid body: the Mind Stone's energy is too much for an ordinary body. The stone sings lower
        // and louder than it does for a friend.
        InfinityStone.MIND.use(level, player.position(), 1.0F, 0.75F);
        level.explode(player, player.getX(), player.getY() + 1.0, player.getZ(), 3.0F, Level.ExplosionInteraction.NONE);
        if (player.isCreative() || player.isSpectator()) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.creative").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        player.hurt(ModDamageTypes.source(level, ModDamageTypes.MIND_STONE), 1.0E6F);
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
