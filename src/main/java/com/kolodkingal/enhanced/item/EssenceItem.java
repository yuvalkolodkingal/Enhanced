package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.ApocalypseConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * The Essence of En Sabah Nur, the reward of the Apocalypse fight: hold right-click for three seconds with no power to
 * become Apocalypse (the film's essence transference). Sapphire wisps with gold veins spiral from the item into the
 * player while it is held, to a slow heartbeat; at the end the Essence is used up (not in Creative), a bolt of lightning
 * that burns nothing strikes where they stand and they awaken, with no damage. Letting go early, swapping slots or dying
 * changes nothing and the wisps fade. Someone who already has a power is turned away at the first click with nothing
 * used up; someone who already is Apocalypse is told so. (DESIGN section 17.)
 */
public class EssenceItem extends AwakeningItem {
    private static final int SAPPHIRE = 0x3FA0FF;
    private static final int GOLD_VEIN = 0xF5C84A;

    public EssenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public Power power() {
        return Power.APOCALYPSE;
    }

    /** {@code [apocalypse].essenceRitualTicks}, three seconds unless a server says otherwise. */
    @Override
    public int holdTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? ApocalypseConfig.ESSENCE_RITUAL_TICKS.get() : DEFAULT_HOLD_TICKS;
    }

    @Override
    protected Component alreadyPowered() {
        return Component.translatable("message.enhanced.essence.already_powered");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (Powers.is(player, Power.APOCALYPSE)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.enhanced.essence.already_apocalypse"), true);
            }
            // (The click is spent on the refusal, so the other hand's item is not used instead.)
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (!level.isClientSide && player.isUsingItem()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ESSENCE_TRANSFER.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        return result;
    }

    /** Every tick of the hold, wisps spiral in from the item to the player, closer and quicker as the transference goes on. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int total = holdTicks();
        float progress = 1.0F - Math.max(0, remaining) / (float) Math.max(1, total);
        Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
        for (int i = 0; i < 2; i++) {
            double angle = (total - remaining) * 0.45 + i * Math.PI;
            double radius = 1.5 - 1.1 * progress;
            Vec3 at = center.add(Math.cos(angle) * radius, (i == 0 ? 0.5 : -0.4) * (1.0F - progress), Math.sin(angle) * radius);
            Vec3 in = center.subtract(at).normalize();
            server.sendParticles(new EnergyParticleOptions(i == 0 ? SAPPHIRE : GOLD_VEIN, 1.0F + progress), at.x, at.y, at.z, 0, in.x, in.y, in.z, 0.12);
        }
    }

    /** Let go early: nothing is used up, the wisps fade and the heartbeat stops. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player) {
            server.getServer().getPlayerList().broadcast(null, player.getX(), player.getY(), player.getZ(), 48.0, server.dimension(),
                    new ClientboundStopSoundPacket(ModSounds.ESSENCE_TRANSFER.getId(), SoundSource.PLAYERS));
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3F, 1.6F);
        }
    }

    /** The transference: a bolt of lightning that burns nothing, and he awakens. */
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
        tooltip.add(Component.translatable("item.enhanced.essence.desc").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.enhanced.essence.desc2").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
