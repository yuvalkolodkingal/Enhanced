package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.wolverine.Claws;
import com.kolodkingal.enhanced.wolverine.WolverineAbilities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The Adamantium Injection (3.7): a thick steel syringe of liquid adamantium from the lab's Weapon X rack. Hold
 * right-click for {@code [adamantium_injection] holdTicks} (three seconds, the arm held out as for the Mutant Cure):
 * silver motes are drawn into the arm, and at the end the injection is used up (kept in Creative) and a powerless
 * player is Wolverine: the screen flashes silver, a deep "snikt", the title, and he wakes with the claws out for two
 * seconds. Letting go early, swapping slots or dying changes nothing.
 *
 * <p>A hero is refused at the first click, with nothing used up ({@code heroesRefused}, the default: "The
 * adamantium needs a body that can heal. Take the Mutant Cure first."; Deadpool hears a word of his own); with {@code
 * heroesRefused} off the injection replaces his power, as {@code /enhanced power set} would. Only on yourself: never
 * on another player.
 */
public class AdamantiumInjectionItem extends Item {
    /** The silver of the motes, and the liquid's highlight. */
    private static final int SILVER = WolverineAbilities.SILVER;
    private static final int BRIGHT = 0xF4F7FA;

    public AdamantiumInjectionItem(Properties properties) {
        super(properties);
    }

    /** {@code [adamantium_injection] holdTicks}, three seconds unless a server says otherwise. */
    public static int holdTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? WolverineConfig.INJECTION_HOLD_TICKS.get() : 60;
    }

    /** Whether a hero is turned away ({@code [adamantium_injection] heroesRefused}, yes unless a server says no). */
    public static boolean heroesRefused() {
        return !EnhancedConfig.SPEC.isLoaded() || WolverineConfig.INJECTION_HEROES_REFUSED.get();
    }

    /** Whether the player would be turned away: a hero, while heroes are refused. */
    public static boolean refuses(Player player) {
        return heroesRefused() && Powers.power(player).hasPowers();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (refuses(player)) {
            if (!level.isClientSide) {
                String key = Powers.power(player) == Power.DEADPOOL ? "message.enhanced.wolverine.injection_deadpool"
                        : "message.enhanced.wolverine.injection_hero";
                player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
            }
            // (The click is spent on the refusal: the game must not go on to use what the other hand holds.)
            return InteractionResultHolder.consume(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ADAMANTIUM_CLANK.get(),
                    SoundSource.PLAYERS, 0.4F, 1.6F);
        }
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

    /** Every tick of the hold, silver motes are drawn into the arm, more of them as it goes on. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer user)) {
            return;
        }
        int total = holdTicks();
        float progress = 1.0F - Math.max(0, remaining) / (float) Math.max(1, total);
        float side = user.getMainArm() == HumanoidArm.RIGHT ^ user.getUsedItemHand() == InteractionHand.OFF_HAND
                ? 1.0F : -1.0F;
        double yaw = Math.toRadians(user.yBodyRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw)).scale(side * 0.35);
        Vec3 arm = user.position().add(0, user.getBbHeight() * 0.62, 0).add(right);
        double angle = (total - remaining) * 0.7;
        Vec3 out = new Vec3(Math.cos(angle), 0.5, Math.sin(angle)).normalize().scale(0.45 + 0.4 * (1.0F - progress));
        Vec3 at = arm.add(out);
        Vec3 in = arm.subtract(at);
        server.sendParticles(new EnergyParticleOptions((total - remaining) % 3 == 0 ? BRIGHT : SILVER,
                0.5F + 0.6F * progress), at.x, at.y, at.z, 0, in.x, in.y, in.z, 0.12 + 0.1 * progress);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || refuses(player)) {
            return stack;
        }
        inject(player);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return stack;
    }

    /** The injection goes in: he is Wolverine, with the injection's own moment. */
    public static void inject(ServerPlayer player) {
        Powers.awaken(player, Power.WOLVERINE);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ADAMANTIUM_INJECTION.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WOLVERINE_AWAKEN.get(),
                SoundSource.PLAYERS, 1.2F, 1.0F);
        Powers.burst(level, player.getX(), player.getY() + 1.0, player.getZ(), SILVER, 80, 1.0);
        // (The silver flash on his own screen.)
        PacketDistributor.sendToPlayer(player, new WolverineFxPayload(WolverineFxPayload.SNIKT, player.getId(),
                List.of(player.position()), 1));
        Claws.awakened(player);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.adamantium_injection.desc")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.adamantium_injection.desc2").withStyle(ChatFormatting.AQUA));
    }
}
