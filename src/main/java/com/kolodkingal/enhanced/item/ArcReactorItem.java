package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.ironman.IronManAbilities;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The Arc Reactor (3.8, PLAN 5.2): a round palm-sized reactor, a white-blue core in a steel ring. Hold right-click for
 * {@code [iron_man] reactorHoldTicks} (two seconds, pressed to the chest, blue light pulling in) and the holder becomes
 * Iron Man by the power-set path ({@code Powers.awaken}: the old hero is cleared). The reactor is <em>kept</em>:
 * carried anywhere, it powers the classic suits. Already Iron Man: "Your Arc Reactor glows at full power." With {@code
 * reactorReplacesHero} off, a hero is refused (as the awakening items refuse).
 */
public class ArcReactorItem extends Item {
    public ArcReactorItem(Properties properties) {
        super(properties);
    }

    /** {@code [iron_man] reactorHoldTicks}, two seconds unless a server says otherwise. */
    public static int holdTicks() {
        return IronManAbilities.cfg(IronManConfig.REACTOR_HOLD_TICKS, 40);
    }

    /** Whether a reactor would turn this player away: a hero, while heroes are not replaced. */
    public static boolean refuses(Player player) {
        Power power = Powers.power(player);
        return power != Power.IRON_MAN && power.hasPowers()
                && !IronManAbilities.cfg(IronManConfig.REACTOR_REPLACES_HERO, true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (Powers.power(player) == Power.IRON_MAN) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.enhanced.iron_man.full_power")
                        .withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (refuses(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.enhanced.iron_man.reactor_refused")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.consume(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return holdTicks();
    }

    /** Every tick of the hold, blue light is drawn into his chest, more of it as it goes on. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer user) {
            pull(server, user, holdTicks(), remaining);
        }
    }

    /** The hold's light: motes spiral into the chest. */
    static void pull(ServerLevel server, ServerPlayer user, int total, int remaining) {
        float progress = 1.0F - Math.max(0, remaining) / (float) Math.max(1, total);
        Vec3 chest = user.position().add(0, user.getBbHeight() * 0.68, 0).add(user.getLookAngle().multiply(0.3, 0.0,
                0.3));
        double angle = (total - remaining) * 0.8;
        Vec3 out = new Vec3(Math.cos(angle), 0.3 * Math.sin(angle * 0.5), Math.sin(angle)).scale(0.9 - 0.6 * progress);
        Vec3 at = chest.add(out);
        Vec3 in = chest.subtract(at);
        server.sendParticles(new EnergyParticleOptions((total - remaining) % 3 == 0 ? 0xFFFFFF
                : IronManAbilities.REACTOR_BLUE, 0.5F + 0.6F * progress), at.x, at.y, at.z, 0, in.x, in.y, in.z,
                0.12 + 0.1 * progress);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || refuses(player)
                || Powers.power(player) == Power.IRON_MAN) {
            return stack;
        }
        become(player);
        return stack;
    }

    /** He is Iron Man: the reactor clicks into his chest, the title, the hum. */
    public static void become(ServerPlayer player) {
        Powers.awaken(player, Power.IRON_MAN);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ARC_REACTOR_INSERT.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.IRON_MAN_AWAKEN.get(),
                SoundSource.PLAYERS, 1.2F, 1.0F);
        Powers.burst(level, player.getX(), player.getY() + 1.2, player.getZ(), IronManAbilities.REACTOR_BLUE, 60, 0.8);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.arc_reactor.tooltip").withStyle(ChatFormatting.GRAY,
                ChatFormatting.ITALIC));
    }
}
