package com.kolodkingal.enhanced.wolverine;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.healing.HealingHooks;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wolverine's event hooks (3.7). Priorities as {@code docs/HEROES-AND-BOSSES.md} lists them:
 * <ul> <li>{@link PlayerTickEvent.Pre}, both sides (his own client for a real player): his weight in water ({@link
 * Adamantium}) and the claws' climb ({@link ClawClimb});</li> <li>{@link LivingIncomingDamageEvent}, HIGH: no fall
 * damage from his own leaps; LOW: a blow of his empty main hand with the claws out hits for {@code clawBonus} more (and
 * the Rage's share of it), and a full-strength one sweeps the creatures beside the one struck;</li> <li>{@link
 * AttackEntityEvent}: the strength of the blow, read before the game resets it;</li> <li>{@link
 * LivingEntityUseItemEvent.Start} and {@link PlayerInteractEvent.RightClickItem}: in the Rage he cannot use items ("Too
 * angry to eat!").</li> </ul>
 * And the healing factor's hooks ({@link HealingHooks}): when he rises, a growl and the claws.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class WolverineEvents {
    /** The sweep of a full-strength claw blow: its damage, its reach round the one struck. */
    public static final float SWEEP_DAMAGE = 2.0F;
    public static final double SWEEP_REACH = 1.0;
    /** The strength of each player's last blow (read before the game resets it), and when it was struck. */
    private static final Map<UUID, Float> STRENGTH = new HashMap<>();
    /** The last time "Too angry to eat!" was said to each (once a second at most). */
    private static final Map<UUID, Long> SAID = new HashMap<>();

    /** The healing factor's moments, for Wolverine: he rises with a growl, the claws out. */
    static void listen() {
        HealingHooks.register(new HealingHooks.Listener() {
            @Override
            public void down(ServerPlayer player) {
                if (WolverineAbilities.hasBody(Powers.get(player))) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            ModSounds.WOLVERINE_GROWL.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
                }
            }

            @Override
            public void rise(ServerPlayer player, boolean rescued) {
                PowerData data = Powers.get(player);
                if (!WolverineAbilities.hasBody(data)) {
                    return;
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.WOLVERINE_GROWL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                if (data.has(com.kolodkingal.enhanced.custom.HeroAbility.WOLVERINE_CLAWS)) {
                    Claws.popOut(player, data);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide && !player.isLocalPlayer()) {
            return; // (another player on this client: his own client moves him)
        }
        Adamantium.sink(player);
        ClawClimb.move(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFall(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypeTags.IS_FALL)
                && (Pounce.fallGuarded(player) || ClawClimb.on(player, Powers.get(player))
                && ClawClimb.wall(player) != null)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STRENGTH.put(player.getUUID(), player.getAttackStrengthScale(0.5F));
        }
    }

    /** A blow of his empty main hand with the claws out: the claws' damage, and a full one sweeps. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onClawBlow(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player
                || !source.is(DamageTypes.PLAYER_ATTACK) || event.getEntity() == player
                || !player.getMainHandItem().isEmpty()) {
            return;
        }
        PowerData data = Powers.get(player);
        if (!Claws.out(data)) {
            return;
        }
        float strength = STRENGTH.getOrDefault(player.getUUID(), 1.0F);
        event.setAmount(event.getAmount() + Claws.bonus(player, data) * (0.2F + 0.8F * strength * strength));
        if (strength >= 0.9F && player.onGround() && !player.isSprinting()) {
            sweep(player, event.getEntity());
        }
    }

    /** The creatures beside the one struck take a sweep of the claws. */
    private static void sweep(ServerPlayer player, LivingEntity struck) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox()
                .inflate(SWEEP_REACH, 0.25, SWEEP_REACH), e -> e != struck && WolverineAbilities.mayHurt(player, e)
                        && player.distanceToSqr(e) < 16.0)) {
            WolverineAbilities.hurt(player, e, ModDamageTypes.WOLVERINE_CLAWS, SWEEP_DAMAGE
                    * WolverineAbilities.rageFactor(player));
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player && Berserker.raging(player)) {
            event.setCanceled(true);
            tooAngry(player);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (Berserker.raging(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            tooAngry(event.getEntity());
        }
    }

    private static void tooAngry(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        long now = player.level().getGameTime();
        Long said = SAID.get(player.getUUID());
        if (said == null || now - said >= 20 || said > now) {
            SAID.put(player.getUUID(), now);
            player.displayClientMessage(Component.translatable("message.enhanced.wolverine.too_angry")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    static void forget(ServerPlayer player) {
        STRENGTH.remove(player.getUUID());
        SAID.remove(player.getUUID());
    }

    static void clear() {
        STRENGTH.clear();
        SAID.clear();
    }

    private WolverineEvents() {
    }
}
