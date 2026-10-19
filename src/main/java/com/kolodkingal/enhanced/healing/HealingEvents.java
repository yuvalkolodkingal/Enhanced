package com.kolodkingal.enhanced.healing;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.network.HealingFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The healing factor's events (3.7, the plan's 2.10):
 *
 * <ul>
 *     <li><b>{@code LivingIncomingDamageEvent}, HIGHEST</b>: a healer who is DOWN is untouchable: every blow is
 *     cancelled but one that may defeat him ({@link HealingFactor#defeat}), which kills. (A player in a trial of the
 *     Witches' Road is left to the Road's own rule, {@code RoadRules}, same priority.)</li>
 *     <li><b>{@code LivingDamageEvent.Pre}, LOWEST</b>: after armour, enchantments and every other handler, a blow
 *     that would bring a healer to 0 is cut to leave him 1 health (absorption counted) and he goes DOWN ({@link
 *     Downed}), unless it may defeat him. The death never happens, so a Totem of Undying is never used up.</li>
 *     <li><b>{@code LivingDamageEvent.Post}</b>: the time he was last hurt (the faster regeneration waits for it).</li>
 *     <li><b>{@code LivingDeathEvent}, HIGHEST</b>: the net for code that kills without a blow (health set to 0): the
 *     death is refused, he stays at 1 health and goes DOWN, unless the source may defeat him; <b>LOWEST</b>: a healer
 *     defeated after all is told about to everyone near ("…'s healing factor could not stop chaos magic.").</li>
 *     <li><b>{@code LivingChangeTargetEvent}, HIGH</b>: a monster may not take a DOWN healer for its target.</li>
 *     <li><b>{@code MobEffectEvent.Added}</b>: the shrug ({@link Suppression}).</li>
 *     <li><b>{@code PlayerTickEvent.Post}</b>: the DOWN timer and the rise, poison's edges, the regeneration.</li>
 *     <li>A DOWN healer uses no item, strikes no blow, breaks no block (both sides); his keys are refused in
 *     {@code AbilityHandler}.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class HealingEvents {
    /** When each healer last showed a cut blow (the ring is not sent more than once in {@link Downed#CUT_GAP}). */
    private static final Map<UUID, Long> LAST_CUT = new ConcurrentHashMap<>();
    /**
     * What is defeating each healer whose death is under way (found first, before the death's other handlers: the
     * Vormir sacrifice's forgets the push that made it), for the line told at the end.
     */
    private static final Map<UUID, HealingFactor.Defeat> DEFEATING = new ConcurrentHashMap<>();
    /** How far (blocks) the line of a healer's defeat is heard. */
    static final double DEFEAT_RADIUS = 64.0;

    private HealingEvents() {
    }

    /** One tick of a healer's factor (the player tick's; tests call it for a player nothing ticks). */
    public static void tick(ServerPlayer player) {
        if (!player.isAlive()) {
            return;
        }
        Downed.tick(player);
        Suppression.tick(player);
        Regeneration.tick(player);
    }

    // ---- damage and death ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || RoadTrials.isInside(player)
                || Downed.downUntil(player) <= 0L) {
            return;
        }
        if (HealingFactor.defeat(event.getSource(), player) != null) {
            return;
        }
        event.setCanceled(true);
        if (event.getSource().getEntity() != null) {
            cut(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !HealingFactor.has(player)
                || RoadTrials.isInside(player)) {
            return;
        }
        float absorption = player.getAbsorptionAmount();
        float loss = Math.max(0.0F, event.getNewDamage() - absorption);
        if (loss < player.getHealth() || HealingFactor.defeat(event.getSource(), player) != null) {
            return;
        }
        event.setNewDamage(absorption + Math.max(0.0F, player.getHealth() - 1.0F));
        cut(player);
        Downed.down(player);
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0.0F
                && HealingFactor.has(player)) {
            Downed.setState(player, Downed.state(player).withLastHarm(player.level().getGameTime()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || RoadTrials.isInside(player)
                || !HealingFactor.has(player)) {
            return;
        }
        HealingFactor.Defeat defeat = HealingFactor.defeat(event.getSource(), player);
        if (defeat != null) {
            DEFEATING.put(player.getUUID(), defeat);
            return;
        }
        event.setCanceled(true);
        player.setHealth(1.0F);
        Downed.down(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDefeated(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HealingFactor.Defeat defeat = DEFEATING.remove(player.getUUID());
        if (event.isCanceled()) {
            return; // (something saved him after all: a Second Life, the Road)
        }
        Suppression.forget(player.getUUID());
        LAST_CUT.remove(player.getUUID());
        PowerData data = Powers.get(player);
        if (data.is(Live.HEALING_DOWN)) {
            data.set(Live.HEALING_DOWN, false);
            Powers.syncIfDirty(player);
        }
        if (defeat == null) {
            return;
        }
        Component line = Component.translatable(defeat.messageKey(), player.getDisplayName())
                .withStyle(ChatFormatting.GOLD);
        for (ServerPlayer near : player.serverLevel().players()) {
            if (near == player || near.distanceToSqr(player) <= DEFEAT_RADIUS * DEFEAT_RADIUS) {
                near.sendSystemMessage(line);
            }
        }
    }

    /** A blow was cut short (or turned away while he is DOWN): the ring the attacker sees, a shimmer of sound. */
    static void cut(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long last = LAST_CUT.get(player.getUUID());
        if (last != null && now - last < Downed.CUT_GAP && now >= last) {
            return;
        }
        LAST_CUT.put(player.getUUID(), now);
        Downed.fx(player, HealingFxPayload.CUT, 0);
        Downed.sound(player, ModSounds.HEALING_CUT.get(), 0.8F);
    }

    // ---- monsters, effects ----

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob && event.getNewAboutToBeSetTarget() instanceof ServerPlayer player
                && HealingFactor.isDown(player) && Downed.mobsForget()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Suppression.onAdded(player, event.getEffectInstance());
        }
    }

    // ---- ticking, logging in and out ----

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Downed.login(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Suppression.forget(event.getEntity().getUUID());
        LAST_CUT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Suppression.clear();
        LAST_CUT.clear();
        DEFEATING.clear();
    }

    // ---- a DOWN healer does nothing (both sides: his client knows the bit) ----

    private static void refuse(Player player, ICancellableEvent event) {
        if (player != null && HealingFactor.isDown(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        refuse(event.getEntity(), event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onStartUsing(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player) {
            refuse(player, event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (HealingFactor.isDown(event.getEntity())) {
            event.setNewSpeed(0.0F);
        }
    }
}
