package com.kolodkingal.enhanced.event;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.MjolnirShatter;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.command.EnhancedCommand;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.hex.HexVisions;
import com.kolodkingal.enhanced.platform.event.AttackEntityEvent;
import com.kolodkingal.enhanced.platform.event.EntityJoinLevelEvent;
import com.kolodkingal.enhanced.platform.event.EntityMountEvent;
import com.kolodkingal.enhanced.platform.event.EntityStruckByLightningEvent;
import com.kolodkingal.enhanced.platform.event.EntityTeleportEvent;
import com.kolodkingal.enhanced.platform.event.EntityTickEvent;
import com.kolodkingal.enhanced.platform.event.EntityTravelToDimensionEvent;
import com.kolodkingal.enhanced.platform.event.ItemEntityPickupEvent;
import com.kolodkingal.enhanced.platform.event.LevelTickEvent;
import com.kolodkingal.enhanced.platform.event.LivingChangeTargetEvent;
import com.kolodkingal.enhanced.platform.event.LivingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingEntityUseItemEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingKnockBackEvent;
import com.kolodkingal.enhanced.platform.event.MobEffectEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.ProjectileImpactEvent;
import com.kolodkingal.enhanced.platform.event.RegisterCommandsEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.TriState;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.road.WitchesRoad;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/** Game-bus events shared by the server and singleplayer. */
@Subscriber
public final class CommonEvents {

    // ---- ticking ----

    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerTicker.tick(player);
        }
    }

    @Subscribe
    public static void onServerTick(ServerTickEvent.Post event) {
        Telekinesis.tickFlights(event.getServer());
    }

    @Subscribe
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HexManager.tickLevel(level);
            HexVisions.tickLevel(level);
            SlowMotion.serverTick(level);
            MagnetoAbilities.tickHeld(level);
            WitchesRoad.tick(level);
        }
    }

    @Subscribe
    public static void onServerStopped(ServerStoppedEvent event) {
        HeroLifecycle.onServerStopped();
        Telekinesis.clear();
        HexManager.clearMemory();
        SlowMotion.clear();
        MagnetoAbilities.clearHeld();
        TommyAbilities.clear();
        com.kolodkingal.enhanced.ability.SpeedSurfaces.clear();
        com.kolodkingal.enhanced.ability.SpeedStrikes.clear();
        ThorAbilities.clear();
        LokiAbilities.clear();
        BillyAbilities.clear();
        WitchesRoad.clear();
    }

    // ---- Quicksilver does everything faster (runs on both sides, so the client's use animation keeps up) ----

    /** Eating, drinking, drawing a bow, loading a crossbow: most uses of an item go faster while a mode is on. */
    @Subscribe
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof Player player) {
            event.setDuration(QuicksilverAbilities.fasterUse(player, event.getItem(), event.getDuration()));
        }
    }

    // ---- Quicksilver Time slows the world around him (runs on both sides) ----

    @Subscribe
    public static void onEntityTick(EntityTickEvent.Pre event) {
        MjolnirPin.beforeTick(event.getEntity());
        if (SlowMotion.skipTick(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onEntityTickDone(EntityTickEvent.Post event) {
        SlowMotion.afterTick(event.getEntity());
        MjolnirPin.afterTick(event.getEntity());
    }

    @Subscribe
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SlowMotion.onStartTracking(event.getTarget(), player);
        }
    }

    @Subscribe
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            SlowMotion.onJoin(event.getEntity());
            MagnetoAbilities.onJoin(event.getEntity());
            Telekinesis.onJoin(event.getEntity());
        }
    }

    // ---- lifecycle ----

    @Subscribe
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Telekinesis.onLogout(player);
            Powers.get(player).setBeamActive(false);
            Powers.get(player).setChaosShieldActive(false);
            Powers.get(player).setShrapnelActive(false);
            ThorAbilities.onLogout(player);
            ContinuousBeam.forget(player);
            Powers.get(player).set(Live.BEAMING, false);
            MjolnirShatter.onGone(player);
            MjolnirPin.onOwnerGone(player);
            WitchesRoad.forget(player.getUUID());
            // The Vision of her Hex goes with Wanda; the Hex itself stays, and raises him again when she is back.
            HexVisions.ownerGone(player.getUUID());
            HeroLifecycle.onLogout(player);
        }
    }

    @Subscribe
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Telekinesis.release(player, false);
            HexManager.forget(player.getUUID());
            HeroLifecycle.onChangeDimension(player);
        }
    }

    @Subscribe
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        HexManager.forget(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            HeroLifecycle.onRespawn(player);
        }
    }

    @Subscribe
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        HexManager.forget(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            HeroLifecycle.onLogin(player);
        }
    }

    @Subscribe
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Telekinesis.release(player, false);
            if (Powers.get(player).has(HeroAbility.WANDA_HEX)) { // (3.4: or a custom hero with her Hex)
                HexManager.onOwnerDeath(player);
            }
            // A Mjolnir in pieces comes together and falls where he dies; whoever he pinned is free.
            MjolnirShatter.onGone(player);
            MjolnirPin.onOwnerGone(player);
            HeroLifecycle.onDeath(player);
        }
    }

    @Subscribe
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EnhancedCommand.register(event.getDispatcher());
    }

    // ---- Magneto's mental immunity; the few too powerful to be spellbound or confused ----

    @Subscribe
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(MobEffects.CONFUSION) && MagnetoAbilities.wearsHelmet(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
        if (event.getEffectInstance().is(ModEffects.SPELLBOUND) && event.getEntity().getType().is(ModTags.SPELLBOUND_IMMUNE)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
        if (event.getEffectInstance().is(ModEffects.CONFUSED) && event.getEntity().getType().is(ModTags.CONFUSED_IMMUNE)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // ---- defences ----

    @Subscribe
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        DamageSource source = event.getSource();
        boolean unstoppable = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);

        if (VisionAbilities.isPhased(player) && !unstoppable) {
            event.setCanceled(true);
            return;
        }
        if (source.is(DamageTypeTags.IS_FREEZING) && LokiAbilities.isFrostGiant(player)) {
            // A Frost Giant is made of the cold.
            event.setCanceled(true);
            return;
        }
        if (player instanceof ServerPlayer billy && BillyBarrier.isUp(billy) && !unstoppable && BillyBarrier.absorb(billy, event)) {
            return;
        }
        if (player instanceof ServerPlayer wanda && WandaAbilities.hasShieldUp(player) && !unstoppable) {
            // What the shield stops wears it down (and when nothing is left it shatters).
            if (source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() != null && source.getEntity() != player) {
                WandaAbilities.wear(wanda, event.getAmount(), source.getEntity() != null ? source.getEntity() : source.getDirectEntity());
                event.setCanceled(true);
                return;
            }
            if (WandaAbilities.isMeleeHit(source) && source.getDirectEntity() instanceof LivingEntity attacker && attacker != player) {
                // Close in is no way round it: a creature's or a player's blow is stopped like an arrow, and the
                // attacker is thrown back out of the dome. (With PvP off a player's blow never gets here: it costs
                // nothing.)
                WandaAbilities.blockMelee(wanda, attacker, event.getAmount());
                event.setCanceled(true);
                return;
            }
            if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                float absorbable = event.getAmount() * (1.0F - EnhancedConfig.CHAOS_SHIELD_EXPLOSION_FACTOR.get().floatValue());
                event.setAmount(event.getAmount() - WandaAbilities.wear(wanda, absorbable,
                        source.getEntity() != null ? source.getEntity() : source.getDirectEntity()));
            }
            if (source.is(DamageTypeTags.IS_LIGHTNING) && ThorAbilities.isThor(source.getEntity())) {
                // Thor's lightning (his beam, and the arcs that leap on from what it hits) never gets through the
                // shield. (The beam is stopped at the sphere before it gets here, unless Thor stands inside it.)
                WandaAbilities.wear(wanda, event.getAmount());
                event.setCanceled(true);
                return;
            }
            if (source.is(ModTags.CHAOS_SHIELD_STOPS) && source.getEntity() != player) {
                // 3.8.2: nor do Iron Man's repulsors, beams, lasers and bolts (and the other lasers of 3.8), even
                // from inside the sphere, nor the small boom of a repulsor that lands next to her.
                WandaAbilities.wear(wanda, event.getAmount(), source.getEntity() != null ? source.getEntity()
                        : source.getDirectEntity());
                event.setCanceled(true);
                return;
            }
        }
        if (MagnetoAbilities.wearsHelmet(player) && isWandasAttack(source)) {
            // The helmet only blocks Nausea: Magneto takes Wanda's attacks at full strength.
            event.addReductionModifier(LivingIncomingDamageEvent.Reduction.ARMOR, (container, reduction) -> 0F);
            event.addReductionModifier(LivingIncomingDamageEvent.Reduction.ENCHANTMENTS, (container, reduction) -> 0F);
        }
        if (source.is(DamageTypeTags.IS_FALL) && (QuicksilverAbilities.isRunning(player) || TommyAbilities.isRunning(player))) {
            event.setCanceled(true);
            return;
        }
        if (ThorAbilities.isThor(player) && !unstoppable && (source.is(DamageTypeTags.IS_LIGHTNING)
                || (source.is(DamageTypeTags.IS_FALL) && ThorAbilities.holdsWeapon(player)))) {
            // The God of Thunder; and with Mjolnir or Stormbreaker in hand he lands like one.
            event.setCanceled(true);
            return;
        }
        if (MagnetoAbilities.hasShieldUp(player) && MagnetoAbilities.isMetalProjectile(source.getDirectEntity())
                && source.getEntity() != player) {
            event.setCanceled(true);
        }
    }

    /**
     * Thor's lightning beam hits again and again: it leaves no invulnerability behind, and ignores what is left of
     * anyone's.
     */
    @Subscribe
    public static void onBeamHit(LivingIncomingDamageEvent event) {
        if (event.getSource().is(ModDamageTypes.LIGHTNING_BEAM)) {
            event.setInvulnerabilityTicks(0);
        }
    }

    /**
     * Every blow the God of Thunder lands with his own hands (or his weapon in them) arcs lightning. (Last: a blow that
     * a Chaos Shield, or any other defence, has stopped lands nothing.)
     */
    @Subscribe(priority = Priority.LOW)
    public static void onThorMelee(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof ServerPlayer thor && source.getEntity() == thor
                && event.getEntity() != thor) {
            ThorAbilities.onMeleeHit(thor, event.getEntity());
        }
    }

    /** Lightning doesn't hurt or burn Thor. */
    @Subscribe
    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        if (ThorAbilities.isThor(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Only the worthy can pick Mjolnir up, and only its owner once it has one. */
    @Subscribe
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        ItemStack stack = event.getItemEntity().getItem();
        if (ThorAbilities.isMjolnir(stack) && !Worthiness.canLift(event.getPlayer(), stack)) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    // ---- Mjolnir pins someone down ----

    /**
     * Sneak + right click with Mjolnir sets it down on a creature; right click on the pinned one lifts it (its owner
     * only).
     */
    @Subscribe
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        InteractionResult result = MjolnirPin.interact(event.getEntity(), event.getHand(), event.getTarget());
        if (result != null) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    /** Whoever is pinned can't be knocked about, climb onto a mount or leave for another dimension. */
    @Subscribe
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (MjolnirPin.isPinned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && MjolnirPin.isPinned(event.getEntityMounting())) {
            event.setCanceled(true);
        }
    }

    @Subscribe
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (MjolnirPin.isPinned(event.getEntity())) {
            event.setCanceled(true);
        }
        // A portal doesn't take Vision out of a Hex that holds him either.
        if (event.getEntity() instanceof ServerPlayer player && HexManager.holdsInside(player.serverLevel(), player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.hex.holds_vision"), true);
            event.setCanceled(true);
        }
    }

    // ---- the Vision of the Hex fights for Wanda ----

    /** Whoever hurts Wanda (or her Vision), or whom she hurts, he goes for. */
    @Subscribe
    public static void onHurtForHexVision(LivingDamageEvent.Post event) {
        HexVisions.onDamage(event.getEntity(), event.getSource());
    }

    /** The Chaos Beam, or anything a Scarlet Witch hurled with telekinesis. */
    private static boolean isWandasAttack(DamageSource source) {
        return source.is(ModDamageTypes.CHAOS_BEAM)
                || (source.is(ModDamageTypes.TELEKINETIC_SLAM) && source.getEntity() instanceof Player thrower
                && Powers.get(thrower).has(HeroAbility.WANDA_TELEKINESIS));
    }

    /** A slowed projectile gets its full speed back the moment it hits someone, so it hits as hard as it would have. */
    @Subscribe(priority = Priority.HIGHEST)
    public static void onSlowedProjectileImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide() && event.getRayTraceResult() instanceof EntityHitResult) {
            SlowMotion.unslow(event.getProjectile());
        }
    }

    @Subscribe
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide() || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ServerPlayer player) || projectile.getOwner() == player) {
            return;
        }
        if (VisionAbilities.isPhased(player)) {
            event.setCanceled(true); // passes straight through
            return;
        }
        if (WandaAbilities.hasShieldUp(player)) {
            event.setCanceled(true);
            WandaAbilities.block(player, projectile);
            return;
        }
        if (MagnetoAbilities.hasShieldUp(player) && MagnetoAbilities.isMetalProjectile(projectile)) {
            event.setCanceled(true);
            if (!ModAttachments.PERSISTENT_DATA.get(projectile).getBoolean(MagnetoAbilities.DEMAGNETIZED_TAG)) {
                MagnetoAbilities.repel(player, projectile);
            }
        }
    }

    @Subscribe
    public static void onAttack(AttackEntityEvent event) {
        if (VisionAbilities.isPhased(event.getEntity())) {
            event.setCanceled(true);
        }
        // A spellbound player can't strike anyone (not even with an enchanted weapon).
        if (event.getEntity().hasEffect(ModEffects.SPELLBOUND)) {
            event.setCanceled(true);
        }
    }

    /** A blow that landed: a Frost Giant's freezes. */
    @Subscribe
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player) {
            LokiAbilities.onMeleeHit(player, event.getEntity());
        }
    }

    // ---- who creatures go after ----

    /**
     * A spellbound or confused mob can't pick anyone to go after (it may only let go of its target); and a shapeshifted
     * Loki is one of them: creatures of his new kind never go after him.
     */
    @Subscribe
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && (event.getEntity().hasEffect(ModEffects.SPELLBOUND) || event.getEntity().hasEffect(ModEffects.CONFUSED))) {
            event.setCanceled(true);
        } else if (target instanceof Player player && LokiAbilities.passesAs(player, event.getEntity().getType())) {
            event.setCanceled(true);
        }
    }

    // ---- the Hex wall also stops teleports ----

    @Subscribe
    public static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        checkHexTeleport(event);
    }

    @Subscribe
    public static void onChorusFruit(EntityTeleportEvent.ChorusFruit event) {
        if (MjolnirPin.isPinned(event.getEntityLiving())) {
            event.setCanceled(true);
        } else if (!event.getEntityLiving().level().isClientSide()) {
            HexManager.rememberTeleportStart(event.getEntityLiving());
        }
    }

    @Subscribe
    public static void onEnderTeleport(EntityTeleportEvent.EnderEntity event) {
        if (MjolnirPin.isPinned(event.getEntityLiving())) {
            event.setCanceled(true);
        } else if (!event.getEntityLiving().level().isClientSide()) {
            HexManager.rememberTeleportStart(event.getEntityLiving());
        }
    }

    private static void checkHexTeleport(EntityTeleportEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity && entity.level() instanceof ServerLevel level
                && !HexManager.onTeleport(level, entity, event.getTarget())) {
            event.setCanceled(true);
        }
    }

    private CommonEvents() {
    }
}
