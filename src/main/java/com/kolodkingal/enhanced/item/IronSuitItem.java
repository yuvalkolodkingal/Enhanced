package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.ironman.IronManAbilities;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitControl;
import com.kolodkingal.enhanced.ironman.SuitItemState;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.platform.ext.ItemExt;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * One of Iron Man's suits (3.8, PLAN 5.3): a classic suit is one chestplate item that is the whole suit (its armour on
 * the chest slot, from {@code [mark_*]}; its armour texture empty: {@code SuitLayer} draws it); the Mark 5 is its
 * suitcase in the hand ({@code enhanced:suit} folded: use it and it unfolds onto him); the Mark 50's and Mark 85's are
 * nanotech housings ({@link #nanotech}) carried in the inventory, never worn as items.
 *
 * <p>Only Iron Man may wear one ({@code [iron_man] onlyIronManWears}; a Creative player may, to build). Using a classic
 * suit (right-click) suits him up in it, its suit-up and all ({@link SuitControl#putOn}); the Hulkbuster's use is
 * Veronica's drop (lane L4's listener on the same phase). A suit never breaks: at its last point it is "offline"
 * (its armour still protects) until it is repaired at an anvil (iron ingots, diamonds for the Mark 50, netherite scrap
 * for the Mark 85, iron blocks for the Hulkbuster). Its durability is the server's ({@code [mark_*] durability}).
 */
public class IronSuitItem extends ArmorItem implements ItemExt.Equip, ItemExt.DamageItem, ItemExt.MaxDamage {
    private final SuitKind kind;

    public IronSuitItem(SuitKind kind, Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
        this.kind = kind;
    }

    /** Which suit it is. */
    public SuitKind kind() {
        return kind;
    }

    /** A nanotech housing (Mark 50, Mark 85), not a suit worn as an item. */
    public boolean nanotech() {
        return kind.nanotech();
    }

    /** Whether the Mark 5 is folded into its suitcase. */
    public static boolean folded(ItemStack stack) {
        SuitItemState state = stack.get(ModDataComponents.SUIT.get());
        return state != null && state.folded();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind.nanotech()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!IronSuits.mayWear(player)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.enhanced.iron_man.only_iron_man")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            SuitControl.putOn(server, kind, stack, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }


    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
        if (slot != EquipmentSlot.CHEST || kind.nanotech()) {
            return false;
        }
        return !(entity instanceof Player player) || IronSuits.mayWear(player);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return switch (kind) {
            case MARK_50 -> repair.is(Items.DIAMOND);
            case MARK_85 -> repair.is(Items.NETHERITE_SCRAP);
            case HULKBUSTER -> repair.is(Items.IRON_BLOCK);
            default -> repair.is(Items.IRON_INGOT);
        };
    }

    /** Wear never breaks a suit: it stops at its last point ("offline"). */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity,
                                                   Consumer<Item> onBroken) {
        int left = stack.getMaxDamage() - 1 - stack.getDamageValue();
        return Math.max(0, Math.min(amount, left));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        ConfigSpec.IntValue value = switch (kind) {
            case MARK_1 -> IronManConfig.MARK1_DURABILITY;
            case MARK_3 -> IronManConfig.MARK3_DURABILITY;
            case MARK_5 -> IronManConfig.MARK5_DURABILITY;
            case MARK_50 -> IronManConfig.MARK50_DURABILITY;
            case MARK_85 -> IronManConfig.MARK85_DURABILITY;
            case HULKBUSTER -> IronManConfig.HULKBUSTER_DURABILITY;
        };
        return IronManAbilities.cfg(value, super.getMaxDamage(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY,
                ChatFormatting.ITALIC));
        if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            tooltip.add(Component.translatable("message.enhanced.iron_man.offline").withStyle(ChatFormatting.RED));
        }
    }
}
