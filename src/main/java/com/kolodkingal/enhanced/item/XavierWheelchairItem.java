package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.XavierAbilities;
import com.kolodkingal.enhanced.config.XavierConfig;
import com.kolodkingal.enhanced.network.XavierPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
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
 * Professor X's wheelchair (3.3): crafted, or found in the Mutant School. Hold right-click for three seconds with no
 * power and a ring of blue-white motes closes round the player's head, Cerebro's hum rising; let go early and it fades,
 * nothing used up. At the end the chair is his (used up, not in Creative), he awakens as Professor X and a screen asks
 * which Charles he is: young (First Class, Days of Future Past) or old (the professor in his wheelchair). A hero is
 * turned away at the first click with nothing used up (see {@link AwakeningItem}).
 */
public class XavierWheelchairItem extends AwakeningItem {
    public XavierWheelchairItem(Properties properties) {
        super(properties);
    }

    @Override
    public Power power() {
        return Power.PROFESSOR_X;
    }

    /** {@code [professor_x].wheelchairRitualTicks}, three seconds unless a server says otherwise (the client reads it too). */
    @Override
    public int holdTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? XavierConfig.WHEELCHAIR_RITUAL_TICKS.get() : DEFAULT_HOLD_TICKS;
    }

    @Override
    protected Component alreadyPowered() {
        return Component.translatable("message.enhanced.xavier_wheelchair.already_powered");
    }

    /** A ring of motes turns round the head, tighter as the hold goes on; Cerebro's hum starts with it. */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int total = holdTicks();
        int held = getUseDuration(stack, user) - remaining;
        if (held == 0) {
            server.playSound(null, user.getX(), user.getY(), user.getZ(), ModSounds.XAVIER_AWAKEN.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        }
        float progress = Mth.clamp(held / (float) Math.max(1, total), 0.0F, 1.0F);
        double radius = 1.2 - 0.7 * progress;
        for (int i = 0; i < 2; i++) {
            double angle = held * 0.35 + i * Math.PI;
            server.sendParticles(new EnergyParticleOptions(i == 0 ? XavierAbilities.CORE : XavierAbilities.MAIN, 0.8F + progress * 0.6F),
                    user.getX() + Math.cos(angle) * radius, user.getEyeY() + 0.3, user.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** Let go too soon: the ring fades, the hum is cut, nothing is used up. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (level instanceof ServerLevel server && user instanceof ServerPlayer player) {
            server.getServer().getPlayerList().broadcast(null, player.getX(), player.getY(), player.getZ(), 48.0, server.dimension(),
                    new ClientboundStopSoundPacket(ModSounds.XAVIER_AWAKEN.getId(), SoundSource.PLAYERS));
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F, 0.7F);
        }
    }

    /** He awakens, and the screen of his two versions opens on his client. */
    @Override
    protected void awaken(ServerPlayer player) {
        super.awaken(player);
        player.connection.send(XavierPayload.of(XavierPayload.CHOOSE));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.xavier_wheelchair.desc").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.enhanced.xavier_wheelchair.desc2").withStyle(ChatFormatting.GRAY));
    }
}
