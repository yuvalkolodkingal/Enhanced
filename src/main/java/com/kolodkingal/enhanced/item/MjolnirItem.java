package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Mjolnir, forged from Uru in the heart of a dying star. Only the worthy can lift it (see {@link Worthiness}): in
 * anyone else's hands it slips to the ground, and wherever it comes to rest it stays, as a block nobody else can move.
 * Whoever lifts it first, if they have no other power, becomes Thor. The worthy who hold it can call the Bifrost (use
 * it; see {@link ThorAbilities#useWeapon}). Never breaks; fire and lava can't hurt it.
 */
public class MjolnirItem extends Item {
    public MjolnirItem(Properties properties) {
        super(properties);
    }

    /** 10 attack damage at axe-like speed. */
    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 9.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** Use: the Bifrost screen (Sneak: the Bifrost to where you look). */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ThorAbilities.useWeapon(level, player, hand);
    }

    /** Sneak + use on a block: set Mjolnir down on top of it. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Level level = context.getLevel();
        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            ItemStack hammer = context.getItemInHand().copyWithCount(1);
            if (!MjolnirBlock.setDown(server, pos, hammer)) {
                return InteractionResult.FAIL;
            }
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** A dropped Mjolnir comes to rest as a block where it lands; until then it never despawns. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            if (entity.tickCount == 1) {
                entity.setUnlimitedLifetime();
            }
            if (entity.onGround() && entity.tickCount > 5 && !entity.isRemoved()) {
                BlockPos pos = entity.blockPosition();
                if (MjolnirBlock.setDown(level, pos, stack.copyWithCount(1))) {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        entity.discard();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.mjolnir.desc").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.mjolnir.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enhanced.thor_weapon.bifrost").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.enhanced.unbreakable").withStyle(ChatFormatting.BLUE));
    }
}
