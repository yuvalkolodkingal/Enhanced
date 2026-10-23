package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.HydraConfig;
import com.kolodkingal.enhanced.network.HydraFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Hydra blaster (3.6, the plan's 15.7; the user's default: anyone may use it): a Tesseract-blue energy pistol
 * that officers sometimes drop and a few supply chests hold. Use fires one blue bolt ({@link HydraBoltEntity}: {@code
 * [hydra] blasterDamage} 5, a rest of {@code blasterCooldown} 20 ticks); it has 96 shots and a Hydra energy cell
 * repairs it in an anvil. With {@code blasterEnabled} false it is dead metal.
 */
public class HydraBlasterItem extends Item {
    /** How far its bolt flies. */
    public static final double RANGE = 32.0;

    public HydraBlasterItem(Properties properties) {
        super(properties);
    }

    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || HydraConfig.BLASTER_ENABLED.get();
    }

    static float damage() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.BLASTER_DAMAGE.get().floatValue() : 5.0F;
    }

    static int cooldown() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.BLASTER_COOLDOWN.get() : 20;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!enabled()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.enhanced.hydra.blaster_off"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel server) {
            fire(server, player);
            int rest = cooldown();
            if (rest > 0) {
                player.getCooldowns().addCooldown(this, rest);
            }
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** One bolt from {@code shooter}'s hand where he looks. */
    public static HydraBoltEntity fire(ServerLevel level, LivingEntity shooter) {
        Vec3 look = shooter.getViewVector(1.0F);
        Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
        Vec3 from = shooter.getEyePosition().add(0.0, -0.15, 0.0).add(right.scale(0.2)).add(look.scale(0.5));
        double speed = EnhancedConfig.SPEC.isLoaded() ? HydraConfig.BOLT_SPEED.get() : 2.2;
        HydraBoltEntity bolt = HydraBoltEntity.create(level, shooter, from, look, speed, damage(), RANGE,
                HydraBoltEntity.RIFLE);
        level.addFreshEntity(bolt);
        level.playSound(null, from.x, from.y, from.z, ModSounds.HYDRA_BLASTER.get(), SoundSource.PLAYERS, 1.0F,
                0.95F + level.random.nextFloat() * 0.1F);
        Net.sendToPlayersTrackingEntityAndSelf(shooter, new HydraFxPayload(HydraFxPayload.MUZZLE,
                shooter.getId(), List.of(from), HydraBoltEntity.RIFLE));
        return bolt;
    }

    /** A Hydra energy cell repairs it in an anvil. */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModItems.HYDRA_ENERGY_CELL.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.enhanced.hydra_blaster.desc").withStyle(ChatFormatting.GRAY));
        if (!enabled()) {
            lines.add(Component.translatable("message.enhanced.hydra.blaster_off").withStyle(ChatFormatting.RED));
        }
    }
}
