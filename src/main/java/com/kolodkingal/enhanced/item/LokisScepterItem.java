package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Loki's Scepter, with the Mind Stone in its blue gem. Using it means touching the stone: with the Scarlet Witch
 * Gene in your other hand you awaken as the Scarlet Witch; without it, the stone's energy tears you apart.
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
        if (power == Power.SCARLET_WITCH) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.recognizes").withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }

        InteractionHand otherHand = scepterHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack gene = player.getItemInHand(otherHand);
        if (gene.is(ModItems.SCARLET_WITCH_GENE.get())) {
            if (power.hasPowers()) {
                player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.already_powered").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            gene.consume(1, player);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(player.getX(), player.getY(), player.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            Powers.awaken(player, Power.SCARLET_WITCH);
            ItemStack crown = new ItemStack(ModItems.WANDA_CROWN.get());
            if (!player.getInventory().add(crown)) {
                player.drop(crown, false);
            }
            return;
        }

        // No gene: the Mind Stone's energy is too much for an ordinary body.
        level.explode(player, player.getX(), player.getY() + 1.0, player.getZ(), 3.0F, Level.ExplosionInteraction.NONE);
        if (player.isCreative() || player.isSpectator()) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind_stone.creative").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        player.hurt(ModDamageTypes.source(level, ModDamageTypes.MIND_STONE), 1.0E6F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.lokis_scepter.desc").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.enhanced.lokis_scepter.desc2").withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
