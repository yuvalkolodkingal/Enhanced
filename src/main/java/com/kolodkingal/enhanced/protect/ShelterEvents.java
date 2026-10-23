package com.kolodkingal.enhanced.protect;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.vishanti.Ward;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The game events through which the walls of {@link Shelters} keep out what every power of the mod (and of the game) does
 * from outside: the harm it deals, its explosions, its effects, its projectiles and lightning, and whoever comes in through a
 * portal or by dying. Each ability that grips, pulls or curses asks {@link Shelters#stops} itself; this is everything else.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class ShelterEvents {
    /** Creatures on their way through a portal: where they arrive, the Hex judges them as coming from outside. */
    private static final Set<UUID> TRAVELLING = new HashSet<>();

    // ---- harm ----

    /**
     * Harm from outside a Hex does not reach in (unless its maker may cross the wall), and harm from a monster, a boss or dark
     * magic does not reach into a ward: a blow across the wall, an arrow that got in, an explosion outside, a beam, a slam. Harm
     * that comes from nowhere (falling, hunger, poison, the Hex wall's own) and what bypasses invulnerability pass. First, before
     * any shield pays for it.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        DamageSource source = event.getSource();
        List<Vec3> origins = origins(source);
        if (origins.isEmpty()) {
            return;
        }
        Entity cause = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        Vec3 at = Shelters.middle(target);
        for (Hex hex : HexManager.data(level).hexes()) {
            if (hex.contains(at) && anyOutside(origins, hex.center(), hex.radius(), false) && !Shelters.hexTrusts(hex, cause)) {
                event.setCanceled(true);
                Shelters.flash(level, hex.id(), Shelters.entry(hex.center(), hex.radius(), origins.get(0), at), HexManager.wallColor(hex));
                return;
            }
        }
        if (DarkMagic.is(source) || DarkMagic.is(source.getDirectEntity()) || Shelters.hostile(cause)) {
            for (Ward ward : Shelters.wards(level)) {
                if (ward.contains(at) && anyOutside(origins, ward.center(), ward.radius(), true)) {
                    event.setCanceled(true);
                    Shelters.flash(level, ward.owner(), Shelters.entry(ward.center(), ward.radius(), origins.get(0), at), ward.color());
                    return;
                }
            }
        }
    }

    /** The places a harm comes from: whoever dealt it, what struck (the projectile, the blow), and an explosion's centre. */
    private static List<Vec3> origins(DamageSource source) {
        List<Vec3> origins = new ArrayList<>(3);
        if (source.getDirectEntity() != null) {
            origins.add(Shelters.middle(source.getDirectEntity()));
        }
        if (source.getEntity() != null && source.getEntity() != source.getDirectEntity()) {
            origins.add(Shelters.middle(source.getEntity()));
        }
        if (source.getSourcePosition() != null && source.getDirectEntity() == null) {
            origins.add(source.getSourcePosition());
        }
        return origins;
    }

    private static boolean anyOutside(List<Vec3> points, Vec3 center, double radius, boolean edgeInside) {
        for (Vec3 point : points) {
            double d = point.distanceToSqr(center);
            if (edgeInside ? d > radius * radius : d >= radius * radius) {
                return true;
            }
        }
        return false;
    }

    // ---- explosions ----

    /**
     * An explosion outside a Hex breaks no block of it and throws or hurts no one in it, unless whoever set it off may cross the
     * wall; one a monster or a boss set off outside a ward does nothing inside it. (A lit TNT's or a fireball's maker is whoever
     * lit or shot it; a TNT nobody lit is nobody's, and stays out.)
     */
    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Explosion explosion = event.getExplosion();
        Vec3 center = explosion.center();
        Entity cause = explosion.getIndirectSourceEntity() != null ? explosion.getIndirectSourceEntity() : explosion.getDirectSourceEntity();
        for (Hex hex : HexManager.data(level).hexes()) {
            if (!hex.contains(center) && !Shelters.hexTrusts(hex, cause)) {
                keepOut(event, pos -> hex.contains(pos));
            }
        }
        if (Shelters.hostile(cause) || DarkMagic.is(explosion.getDirectSourceEntity())) {
            for (Ward ward : Shelters.wards(level)) {
                if (!ward.contains(center)) {
                    keepOut(event, ward::contains);
                }
            }
        }
    }

    private static void keepOut(ExplosionEvent.Detonate event, java.util.function.Predicate<Vec3> inside) {
        event.getAffectedBlocks().removeIf(pos -> inside.test(Vec3.atCenterOf(pos)));
        event.getAffectedEntities().removeIf(entity -> inside.test(Shelters.middle(entity)));
    }

    /** Billy's raised barrier stops the push of an explosion in front of him as it stops its harm. */
    @SubscribeEvent
    public static void onExplosionKnockback(ExplosionKnockbackEvent event) {
        if (event.getAffectedEntity() instanceof Player billy && !billy.level().isClientSide()
                && BillyBarrier.isUp(billy) && BillyBarrier.covers(billy, event.getExplosion().center())) {
            event.setKnockbackVelocity(Vec3.ZERO);
        }
    }

    // ---- effects ----

    /**
     * An effect laid by someone outside a Hex on someone inside it (a curse, a freeze, Nausea, a splash potion that got in) does
     * not take, nor one of a monster, a boss or dark magic in a ward, nor one from in front of Billy's raised barrier (it pays a
     * little for it).
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        Entity source = event.getEffectSource();
        LivingEntity target = event.getEntity();
        if (source == null || source == target || target.level().isClientSide()) {
            return;
        }
        if (Shelters.stops(source, Shelters.middle(source), target, 2.0F)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // ---- projectiles, lit TNT, fangs, lightning ----

    /**
     * A projectile (or a lit TNT) that would cross into a Hex that does not trust its maker, or a ward against a monster's or a
     * boss's shot or dark magic, stops at the wall before it moves: it turns to sparks of the wall's colour (Mjolnir, Stormbreaker
     * and the other one-of-a-kind weapons turn back to their owner).
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Projectile || entity instanceof PrimedTnt) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!hasWalls(level) || (entity instanceof Projectile projectile && WandaAbilities.isHeadingHome(projectile))) {
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec3 from = Shelters.middle(entity);
        Shelters.Stop stop = Shelters.wallAcross(level, entity, from, from.add(motion).add(motion.normalize().scale(0.3)));
        if (stop != null) {
            stopAt(level, entity, stop);
            event.setCanceled(true);
        }
    }

    /** Whether the level has any wall that keeps things out (a Hex, a ward): what a projectile asks first, every tick. */
    static boolean hasWalls(Level level) {
        return !HexManager.data(level).hexes().isEmpty() || !Shelters.wards(level).isEmpty();
    }

    /** What reached a wall stops there: a projectile is neutralised in the wall's colour, a lit TNT fizzles out. */
    static void stopAt(ServerLevel level, Entity missile, Shelters.Stop stop) {
        Vec3 at = stop.point();
        Shelters.flash(level, stop);
        if (missile instanceof Projectile projectile) {
            projectile.setPos(at.x, at.y - projectile.getBbHeight() * 0.5, at.z);
            WandaAbilities.neutralize(level, projectile, stop.color());
        } else {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.02);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.2F);
            missile.discard();
        }
    }

    /**
     * What a power sends into a Hex or a ward without flying there (a projectile made right inside it, the fangs an Evoker raises
     * there, lightning called down there) does not come, if its maker stands outside and the wall keeps them out.
     */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || event.loadedFromDisk()) {
            return;
        }
        if (entity instanceof LivingEntity living && TRAVELLING.remove(entity.getUUID())) {
            // A creature that came through a portal: the Hex judges it as coming from outside.
            HexManager.arrive(level, living);
            return;
        }
        boolean lightning = entity instanceof LightningBolt;
        if (!(entity instanceof Projectile || entity instanceof PrimedTnt || entity instanceof EvokerFangs || lightning) || !hasWalls(level)) {
            return;
        }
        Entity maker = lightning ? ((LightningBolt) entity).getCause() : Shelters.maker(entity);
        if (maker == null || maker == entity) {
            return; // nobody's: a storm's lightning, a dispenser's arrow fired where it stands
        }
        if (Shelters.keepsOut(level, maker, Shelters.middle(maker), entity.position(), DarkMagic.is(entity))) {
            event.setCanceled(true);
        }
    }

    /** Lightning someone called down outside a Hex strikes no one inside it (unless they may cross the wall). */
    @SubscribeEvent
    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        LightningBolt bolt = event.getLightning();
        Entity target = event.getEntity();
        if (bolt.getCause() == null || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        if (Shelters.hexAgainst(level, bolt.getCause(), bolt.position(), Shelters.middle(target)) != null) {
            event.setCanceled(true);
        }
    }

    /**
     * A projectile that is already over the wall (it started its flight inside the box of a Hex it crossed this tick) hits
     * nothing inside: the impact is cancelled and it is neutralised where the wall is.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(projectile.level() instanceof ServerLevel level) || !hasWalls(level) || WandaAbilities.isHeadingHome(projectile)) {
            return;
        }
        Vec3 from = new Vec3(projectile.xo, projectile.yo + projectile.getBbHeight() * 0.5, projectile.zo);
        Shelters.Stop stop = Shelters.wallAcross(level, projectile, from, event.getRayTraceResult().getLocation());
        if (stop != null) {
            event.setCanceled(true);
            stopAt(level, projectile, stop);
        }
    }

    // ---- coming in through a portal, by dying, or logging in ----

    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof LivingEntity living && !(living instanceof Player) && !living.level().isClientSide()) {
            TRAVELLING.add(living.getUUID());
        }
    }

    /** A player who comes through a portal into a Hex is judged as one who crosses its wall (after its sides are forgotten). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HexManager.arrive(player.serverLevel(), player);
        }
    }

    /** A player who respawns inside a Hex (a bed, an anchor, the spawn) that does not let them in is set down outside it. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.isEndConquered()) {
            HexManager.arrive(player.serverLevel(), player);
        }
    }

    // ---- the Hex Runes, and forgetting ----

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HexRunes.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HexRunes.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Shelters.clear();
        BillyBarrier.clear();
        HexRunes.clear();
        TRAVELLING.clear();
    }

    private ShelterEvents() {
    }
}
