package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.PhoenixAbilities;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Phoenix Ember, a spark of the cosmic fire: hold right-click for three seconds with no power to awaken as Jean
 * Grey. Found in chests (the loot modifiers {@code ember_from_*}), never crafted. While it is held, golden-orange
 * flames spiral up the player and the Phoenix gathers (the sound of the Burst's charge); let go early and the flames
 * sputter out and nothing is used up; at the end it burns into them, is used up (not in Creative) and hurts nobody: from
 * now on they are fire-proof. A hero is turned away at the first click with nothing used up (see {@link
 * AwakeningItem}). (DESIGN section 13.)
 */
public class PhoenixEmberItem extends AwakeningItem {
    public PhoenixEmberItem(Properties properties) {
        super(properties);
    }

    @Override
    public Power power() {
        return Power.PHOENIX;
    }

    /** Three seconds unless the server says otherwise (read by the client too, which has the server's config). */
    @Override
    public int holdTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? PhoenixConfig.EMBER_RITUAL_TICKS.get() : DEFAULT_HOLD_TICKS;
    }

    @Override
    protected Component alreadyPowered() {
        return Component.translatable("message.enhanced.phoenix_ember.already_powered");
    }

    /** The flames spiral up the player; the Phoenix's gathering roar starts with the hold. */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int held = getUseDuration(stack, user) - remaining;
        if (held == 0) {
            server.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.PHOENIX_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        double turn = held * 0.4;
        for (int i = 0; i < 3; i++) {
            double angle = turn + i * Mth.TWO_PI / 3.0;
            double y = user.getY() + ((held + i * 6) % 30) / 30.0 * user.getBbHeight();
            int colour = i == 0 ? PhoenixAbilities.FIRE_GOLD : (i == 1 ? PhoenixAbilities.FIRE_MAIN : PhoenixAbilities.FIRE_CORE);
            // For the others to see: a spark a block from the user's own camera is a blob over their screen.
            PhoenixAbilities.sparksForOthers(server, user, new EnergyParticleOptions(colour, 1.0F), user.getX() + Math.cos(angle) * 0.7, y,
                    user.getZ() + Math.sin(angle) * 0.7, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (held % 4 == 0) {
            PhoenixAbilities.sparksForOthers(server, user, ParticleTypes.FLAME, user.getX(), user.getY() + user.getBbHeight() * 0.6, user.getZ(), 1,
                    0.15, 0.1, 0.15, 0.02);
        }
    }

    /** Let go too soon: the flames sputter out, the gathering roar is cut, nothing is used up. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level instanceof ServerLevel server && user instanceof ServerPlayer player) {
            server.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
            server.sendParticles(ParticleTypes.SMOKE, user.getX(), user.getY() + user.getBbHeight() * 0.6, user.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            ClientboundStopSoundPacket stop = new ClientboundStopSoundPacket(ModSounds.PHOENIX_CHARGE.getId(), SoundSource.PLAYERS);
            for (ServerPlayer other : server.players()) {
                if (other.distanceToSqr(player) <= 64.0 * 64.0) {
                    other.connection.send(stop);
                }
            }
        }
    }

    /** The ember burns into them: the title and the broadcast of the awakening, then a burst of fire that hurts nobody. */
    @Override
    protected void awaken(ServerPlayer player) {
        super.awaken(player);
        ServerLevel level = player.serverLevel();
        level.sendParticles(new EnergyParticleOptions(PhoenixAbilities.FIRE_MAIN, 1.6F), player.getX(), player.getY() + 1.0, player.getZ(),
                80, 0.5, 0.8, 0.5, 0.2);
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.7, 0.4, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PHOENIX_BURST.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.phoenix_ember.desc").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.enhanced.phoenix_ember.desc2").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
