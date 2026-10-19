package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Infinity Gauntlet, the reward of the Thanos fight: hold right-click for three seconds with no power to become
 * Thanos. While it is held the five stones light in turn, each with its voice, from Power to Time; when the hold is
 * done a bolt of light (it hurts nobody) strikes where he stands and the item is used up. (DESIGN section 15.)
 */
public class InfinityGauntletItem extends AwakeningItem {
    public InfinityGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public Power power() {
        return Power.THANOS;
    }

    @Override
    protected Component alreadyPowered() {
        return Component.translatable("message.enhanced.thanos.already_powered");
    }

    /**
     * 3.6: only the Titan's Gauntlet (every old one and the boss's reward: the item's default component) awakens
     * Thanos; a crafted one, empty or filled, does not (lane L1 gives it its uses).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        GauntletContents contents = player.getItemInHand(hand).get(ModDataComponents.GAUNTLET.get());
        if (contents != null && !contents.titan()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    /** Three seconds by default; {@code gauntletRitualTicks} in the {@code [thanos_boss]} config. */
    @Override
    public int holdTicks() {
        return ThanosBossConfig.GAUNTLET_RITUAL_TICKS.get();
    }

    /**
     * The ceremony: the five stones light one after the other over the length of the hold, each with its voice and a
     * little of its colour round the left hand.
     */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        int hold = Math.max(1, holdTicks());
        int used = hold - remaining;
        int step = Math.max(1, hold / StoneKind.granted().size());
        if (used % step != 0 || used / step >= StoneKind.granted().size()) {
            return;
        }
        StoneKind stone = StoneKind.nth(used / step + 1);
        Vec3 hand = player.position().add(0.0, player.getBbHeight() * 0.7, 0.0);
        stone.voice().use(level, hand, 0.7F, 1.0F);
        ((ServerLevel) level).sendParticles(new EnergyParticleOptions(stone.color(), 1.3F), hand.x, hand.y, hand.z, 14, 0.3, 0.3, 0.3, 0.06);
    }

    /** The moment of the awakening: a bolt of light, then the power (the same as a hero whom Mjolnir finds worthy). */
    @Override
    protected void awaken(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(player.getX(), player.getY(), player.getZ());
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        super.awaken(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.desc2").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.enhanced.infinity_gauntlet.desc3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
