package com.kolodkingal.enhanced.item;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.CureConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Mutant Cure (3.3.1), the injector of X-Men: The Last Stand: it takes a hero's power away for good, exactly as an
 * operator's {@code /enhanced power set <player> none} does ({@link Powers#setPower}: everything of the power ends, its
 * flight, shields, beams and holds; items such as Mjolnir stay items in the inventory).
 *
 * <p>Hold right-click for {@code [mutant_cure].holdTicks} (two seconds): pale wisps are drawn out of the one being
 * cured; at the end the cure is used up (not in Creative), a hiss and a falling tone are heard, a grey burst marks the
 * spot and both are told. Letting go early, swapping slots or dying changes nothing. Someone with no power is turned
 * away at the first click with nothing used up.
 *
 * <p>On another player: right-click on him and hold, while he stays within reach. Allowed to an operator always, to
 * anyone else only while PvP is on and he may be hurt ({@code [mutant_cure].onOthers} false: only on yourself).
 * Creatures carry no powers: right-clicking one is an ordinary use (on yourself).
 */
public class MutantCureItem extends Item {
    /** How far (blocks) another player may be at the end of the hold. */
    public static final double REACH = 4.5;
    private static final int DRAINED = 0xDDE8F0;
    private static final int GREY = 0x9AA3AD;

    /** Whom each player is injecting, while he holds the cure on another player (server side). */
    private static final Map<UUID, UUID> TARGETS = new HashMap<>();
    /** When each player was last cured (server side): right-click still held after the cure goes in says nothing. */
    private static final Map<UUID, Long> CURED_AT = new HashMap<>();

    public MutantCureItem(Properties properties) {
        super(properties);
    }

    /** Whether the cure works on this server ({@code [mutant_cure].enabled}). */
    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || CureConfig.ENABLED.get();
    }

    /** {@code [mutant_cure].holdTicks}, two seconds unless a server says otherwise. */
    public static int holdTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? CureConfig.HOLD_TICKS.get() : 40;
    }

    /**
     * Whether {@code user} may inject {@code patient}: the option allows it, and he is an operator, or PvP is on and the
     * patient may be hurt by him (not in Creative, not on his team when friendly fire is off).
     */
    public static boolean mayCure(Player user, Player patient) {
        if (user == patient) {
            return true;
        }
        if (EnhancedConfig.SPEC.isLoaded() && !CureConfig.ON_OTHERS.get()) {
            return false;
        }
        if (user.hasPermissions(2)) {
            return true;
        }
        MinecraftServer server = patient.getServer();
        return server != null && server.isPvpAllowed() && user.canHarmPlayer(patient) && !patient.isCreative() && !patient.isSpectator();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            TARGETS.remove(player.getUUID());
        }
        if (!enabled()) {
            tell(level, player, Component.translatable("message.enhanced.mutant_cure.disabled"));
            return InteractionResultHolder.consume(stack);
        }
        if (!Powers.power(player).hasPowers()) {
            // (The click is spent on the refusal: the game must not go on to use what the other hand holds. Right-click
            // still held from the cure that just went in is not told again: the cure's own message stays.)
            if (!justCured(level, player)) {
                tell(level, player, Component.translatable("message.enhanced.mutant_cure.no_power"));
            }
            return InteractionResultHolder.consume(stack);
        }
        begin(level, player, hand);
        return InteractionResultHolder.consume(stack);
    }

    /** Right-click on another player: the hold begins on him (a creature is an ordinary use, on yourself). */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player patient)) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (!enabled()) {
            tell(level, player, Component.translatable("message.enhanced.mutant_cure.disabled"));
            return InteractionResult.CONSUME;
        }
        if (!Powers.power(patient).hasPowers()) {
            if (!justCured(level, player)) {
                tell(level, player, Component.translatable("message.enhanced.mutant_cure.no_power_other", patient.getDisplayName()));
            }
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide() && !mayCure(player, patient)) {
            tell(level, player, Component.translatable("message.enhanced.mutant_cure.not_allowed"));
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide()) {
            TARGETS.put(player.getUUID(), patient.getUUID());
        }
        begin(level, player, hand);
        return InteractionResult.CONSUME;
    }

    private static void begin(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
    }

    /** Whether {@code player} cured someone (or was cured) in the last second. */
    private static boolean justCured(Level level, Player player) {
        Long cured = CURED_AT.get(player.getUUID());
        return cured != null && level.getGameTime() >= cured && level.getGameTime() - cured <= 20;
    }

    private static void tell(Level level, Player player, Component message) {
        if (!level.isClientSide()) {
            player.displayClientMessage(message, true);
        }
    }

    /** The one being cured: the player he holds it on, or himself. Null when that player is gone or out of reach. */
    @Nullable
    private static ServerPlayer patient(ServerPlayer user) {
        UUID id = TARGETS.get(user.getUUID());
        if (id == null) {
            return user;
        }
        ServerPlayer patient = user.server.getPlayerList().getPlayer(id);
        if (patient == null || !patient.isAlive() || patient.level() != user.level() || patient.distanceTo(user) > REACH) {
            return null;
        }
        return patient;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return holdTicks();
    }

    /** Every tick of the hold, pale wisps are drawn out of the one being cured, more of them as it goes on. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer user)) {
            return;
        }
        ServerPlayer patient = patient(user);
        if (patient == null) {
            return;
        }
        int total = holdTicks();
        float progress = 1.0F - Math.max(0, remaining) / (float) Math.max(1, total);
        Vec3 centre = patient.position().add(0.0, patient.getBbHeight() * 0.55, 0.0);
        Power power = Powers.power(patient);
        double angle = (total - remaining) * 0.6;
        Vec3 out = new Vec3(Math.cos(angle), 0.35, Math.sin(angle)).normalize();
        Vec3 at = centre.add(out.scale(0.3));
        server.sendParticles(new EnergyParticleOptions(power.hasPowers() ? power.color : DRAINED, 0.6F + progress), at.x, at.y, at.z, 0,
                out.x, out.y, out.z, 0.08 + 0.1 * progress);
    }

    /** Let go early: nothing happens, nothing is used up. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide()) {
            TARGETS.remove(entity.getUUID());
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer user)) {
            return stack;
        }
        boolean onAnother = TARGETS.containsKey(user.getUUID());
        ServerPlayer patient = patient(user);
        TARGETS.remove(user.getUUID());
        if (!enabled()) {
            user.displayClientMessage(Component.translatable("message.enhanced.mutant_cure.disabled"), true);
            return stack;
        }
        if (patient == null) {
            user.displayClientMessage(Component.translatable("message.enhanced.mutant_cure.too_far"), true);
            return stack;
        }
        if (!Powers.power(patient).hasPowers() || (onAnother && !mayCure(user, patient))) {
            return stack;
        }
        cure(patient, patient == user ? null : user);
        if (!user.isCreative()) {
            stack.shrink(1);
        }
        return stack;
    }

    /**
     * Takes the power of {@code patient} away, as an operator's {@code power set none} does, with the cure's moment: the
     * hiss and the falling tone, a grey burst, and both told.
     *
     * @param by who injected him, or null when he cured himself
     */
    public static void cure(ServerPlayer patient, @Nullable ServerPlayer by) {
        Power old = Powers.power(patient);
        if (!old.hasPowers()) {
            return;
        }
        Powers.setPower(patient, Power.NONE);
        ServerLevel level = patient.serverLevel();
        CURED_AT.put((by != null ? by : patient).getUUID(), level.getGameTime());
        level.playSound(null, patient.getX(), patient.getY(), patient.getZ(), ModSounds.MUTANT_CURE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        Powers.burst(level, patient.getX(), patient.getY() + 1.0, patient.getZ(), GREY, 50, 0.9);
        Powers.burst(level, patient.getX(), patient.getY() + 1.0, patient.getZ(), old.color, 30, 0.5);
        patient.displayClientMessage(Component.translatable("message.enhanced.mutant_cure.cured", old.displayName())
                .withStyle(ChatFormatting.GRAY), true);
        if (by != null) {
            by.displayClientMessage(Component.translatable("message.enhanced.mutant_cure.cured_other", patient.getDisplayName(), old.displayName())
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.enhanced.mutant_cure.desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.enhanced.mutant_cure.desc2").withStyle(ChatFormatting.AQUA));
    }
}
