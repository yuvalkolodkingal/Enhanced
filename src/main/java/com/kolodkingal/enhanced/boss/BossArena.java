package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.mixin.ServerGamePacketListenerImplAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Where a boss fight happens: a cylinder round a centre, and the rules that keep the fight inside it. One arena belongs
 * to one {@link BossFight}; a boss whose arena needs more than the defaults extends this class (Ultron's island and
 * Apocalypse's sand wall are subclasses of their own).
 *
 * <ul>
 *     <li><b>Protection.</b> While the fight lasts, the blocks of the cylinder cannot be broken, placed, burned,
 *     flooded, pushed or blown up ({@link BossArenaEvents} cancels the events, and every ability of the mod that
 *     changes blocks asks {@link BossFights#protects}), so the world is the same when the fight ends. Switched off by
 *     {@code [boss].protectArena} and by {@code protectBlocks} (Thanos's arena is never protected).</li>
 *     <li><b>Containment.</b> {@link Containment#FIELD} rescues a fighter who strays past {@link #containRadius}
 *     (teleports them back inside, a small damage that cannot kill, a few seconds of Slowness); {@link
 *     Containment#WALL} pushes them back, once a second, and stops them at the arena's top; {@link Containment#NONE}
 *     lets them go (Thanos's leash is his own).</li>
 *     <li><b>The tractor.</b> A fighter who ends up deep underground (a shaft, a cave under the arena) is lifted to
 *     the surface, without damage.</li>
 *     <li><b>The teleport gate.</b> A fighter inside the cylinder cannot teleport out of it, nor to another dimension
 *     (Ender Pearls, Chorus Fruit, the Bifrost, Vanish, the Tesseract, a portal); see {@link #allowsTeleport}. There is
 *     none without containment.</li>
 * </ul>
 *
 * <p>The numbers are set with the fluent methods before the fight begins (the defaults are those of Apocalypse's wall:
 * a fighter is rescued 4 blocks inside the protected radius, and joins 12 blocks beyond it).
 */
public class BossArena {
    /** How the arena keeps the fighters inside. */
    public enum Containment {
        /** Rescued back inside with a small damage that cannot kill (Ultron). */
        FIELD,
        /** Pushed back inward, once a second, and stopped at the top (Apocalypse). */
        WALL,
        /** Not at all (Thanos). */
        NONE
    }

    /** The containment is checked this often, in ticks. */
    public static final int CHECK_INTERVAL = 5;
    /** A fighter is rescued from the field at most this often, in ticks. */
    public static final int FIELD_COOLDOWN = 100;
    /** A fighter is pushed back by the wall at most this often, in ticks. */
    public static final int WALL_COOLDOWN = 20;
    /** A fighter is lifted out of the ground by the tractor at most this often, in ticks. */
    public static final int TRACTOR_COOLDOWN = 100;

    private final ServerLevel level;
    private final Vec3 center;
    private final double radius;
    private final Containment containment;
    private final boolean protectBlocks;

    private double containRadius;
    private double joinRadius;
    private double abandonRadius = 64.0;
    private double bottom;
    private double top;
    private int ticketRadius;
    private int ticketDistance = 2;
    private double tractorDepth = 8.0;
    private float pushDamage;
    private int slowTicks;
    private boolean keepsDrops;

    private final Map<UUID, Long> lastPushed = new HashMap<>();
    private final Map<UUID, Long> lastLifted = new HashMap<>();

    /**
     * @param center        where the fight is centred (the clicked block's top face, or the operator's feet)
     * @param radius        the radius of the protected cylinder, and of the teleport gate
     * @param protectBlocks whether the blocks of the cylinder are protected
     */
    public BossArena(ServerLevel level, Vec3 center, double radius, Containment containment, boolean protectBlocks) {
        this.level = level;
        this.center = center;
        this.radius = radius;
        this.containment = containment;
        this.protectBlocks = protectBlocks;
        this.containRadius = Math.max(1.0, radius - 4.0);
        this.joinRadius = radius + 12.0;
        this.ticketRadius = (int) Math.ceil(radius + 16.0);
        this.bottom = level.getMinBuildHeight();
        this.top = level.getMaxBuildHeight();
        boolean field = containment == Containment.FIELD;
        this.pushDamage = field ? 2.0F : containment == Containment.WALL ? 1.0F : 0.0F;
        this.slowTicks = field ? 60 : 0;
    }

    // ---- setup (before the fight begins) ----

    /** Where the field or the wall stands: a fighter farther from the centre than this is brought back. */
    public BossArena containAt(double containRadius) {
        this.containRadius = containRadius;
        this.joinRadius = Math.max(joinRadius, containRadius + 4.0);
        return this;
    }

    /** How far from the centre a player joins the fight (and counts as present, and as in the arena). */
    public BossArena joinWithin(double joinRadius) {
        this.joinRadius = joinRadius;
        return this;
    }

    /** How near to the centre a participant must be, for the fight not to be abandoned. */
    public BossArena abandonBeyond(double abandonRadius) {
        this.abandonRadius = abandonRadius;
        return this;
    }

    /** The cylinder's vertical extent, in absolute Y (it is the whole height of the world by default). */
    public BossArena vertical(double bottom, double top) {
        this.bottom = bottom;
        this.top = top;
        return this;
    }

    /** How far from the centre the chunks are kept loaded, and the distance of each ticket. */
    public BossArena tickets(int ticketRadius, int ticketDistance) {
        this.ticketRadius = ticketRadius;
        this.ticketDistance = ticketDistance;
        return this;
    }

    /** How deep below the surface a fighter may go before the tractor lifts them out (0 = never). */
    public BossArena tractorDepth(double tractorDepth) {
        this.tractorDepth = tractorDepth;
        return this;
    }

    /** What being brought back costs: a damage that cannot kill, and ticks of Slowness. */
    public BossArena pushback(float damage, int slowTicks) {
        this.pushDamage = damage;
        this.slowTicks = slowTicks;
        return this;
    }

    /** Whether the drops of a fighter who dies in the arena stay until the fight is over (Ultron, Apocalypse). */
    public BossArena keepDrops(boolean keepsDrops) {
        this.keepsDrops = keepsDrops;
        return this;
    }

    // ---- what it is ----

    public ServerLevel level() {
        return level;
    }

    public Vec3 center() {
        return center;
    }

    public double radius() {
        return radius;
    }

    public Containment containment() {
        return containment;
    }

    public double containRadius() {
        return containRadius;
    }

    public double joinRadius() {
        return joinRadius;
    }

    public double abandonRadius() {
        return abandonRadius;
    }

    public double bottom() {
        return bottom;
    }

    public double top() {
        return top;
    }

    public int ticketRadius() {
        return ticketRadius;
    }

    public int ticketDistance() {
        return ticketDistance;
    }

    public boolean keepsDrops() {
        return keepsDrops;
    }

    /** Whether the arena keeps its fighters in, and so gates their teleports. */
    public boolean gates() {
        return containment != Containment.NONE;
    }

    /** The horizontal distance from the centre. */
    public double distance(Vec3 pos) {
        return Math.hypot(pos.x - center.x, pos.z - center.z);
    }

    /** Whether the point is inside the cylinder. */
    public boolean contains(Vec3 pos) {
        return distance(pos) <= radius && pos.y >= bottom && pos.y <= top + 1.0;
    }

    public boolean contains(BlockPos pos) {
        return contains(Vec3.atCenterOf(pos));
    }

    /** Whether a player at this point is near enough to be in the fight. */
    public boolean inJoinRange(Vec3 pos) {
        return distance(pos) <= joinRadius;
    }

    /**
     * Whether this arena protects the block: it is inside the cylinder and the arena protects blocks at all, here
     * and in the server's config.
     */
    public boolean protects(BlockPos pos) {
        return protectBlocks && BossConfig.PROTECT_ARENA.get() && contains(pos);
    }

    /**
     * Whether {@code entity}, a member of the fight, may teleport to {@code to} in {@code destination}: not out of the
     * cylinder, not to another dimension, if it is inside the cylinder now (a fighter who is already far away is none
     * of the arena's business). Always true without containment.
     */
    public boolean allowsTeleport(Entity entity, ServerLevel destination, Vec3 to) {
        if (!gates() || entity.level() != level || !contains(entity.position())) {
            return true;
        }
        return destination == level && contains(to);
    }

    // ---- ticking ----

    /**
     * Every tick of the fight, also while it winds down (it acts every {@link #CHECK_INTERVAL}): the containment and
     * the tractor.
     *
     * @param clock the ticks since the fight began, counting the ending
     */
    void tick(BossFight fight, long clock) {
        if (clock % CHECK_INTERVAL != 0) {
            return;
        }
        long now = level.getGameTime();
        for (ServerPlayer player : fight.fighters()) {
            if (!inJoinRange(player.position())) {
                continue;
            }
            if (containment != Containment.NONE && strayed(player)) {
                bringBack(player, now);
            } else if (tractorDepth > 0.0 && belowTheSurface(player)) {
                lift(player, now);
            }
        }
    }

    /** Whether the player is outside what the containment allows. */
    private boolean strayed(ServerPlayer player) {
        Vec3 pos = player.position();
        return distance(pos) > containRadius || pos.y > top || (containment == Containment.FIELD && pos.y < bottom);
    }

    private boolean belowTheSurface(ServerPlayer player) {
        Vec3 surface = surfaceSpot(player, player.getX(), player.getZ());
        return surface != null && player.getY() < surface.y - tractorDepth;
    }

    private void bringBack(ServerPlayer player, long now) {
        int cooldown = containment == Containment.FIELD ? FIELD_COOLDOWN : WALL_COOLDOWN;
        Long last = lastPushed.get(player.getUUID());
        if (last != null && now - last < cooldown) {
            return;
        }
        Vec3 spot = containment == Containment.FIELD ? rescueSpot(player) : pushSpot(player);
        lastPushed.put(player.getUUID(), now);
        moveTo(player, spot);
        if (pushDamage > 0.0F && !player.isCreative()) {
            // A damage that cannot kill: it leaves at least half a heart.
            float amount = Math.min(pushDamage, player.getHealth() - 1.0F);
            if (amount > 0.0F) {
                player.hurt(level.damageSources().generic(), amount);
            }
        }
        if (slowTicks > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, 0, true, false));
        }
        player.displayClientMessage(Component.translatable("message.enhanced.boss.pushed"), true);
    }

    private void lift(ServerPlayer player, long now) {
        Long last = lastLifted.get(player.getUUID());
        if (last != null && now - last < TRACTOR_COOLDOWN) {
            return;
        }
        Vec3 spot = surfaceSpot(player, player.getX(), player.getZ());
        if (spot == null) {
            return;
        }
        lastLifted.put(player.getUUID(), now);
        moveTo(player, spot);
        player.displayClientMessage(Component.translatable("message.enhanced.boss.lifted"), true);
    }

    /** Moves the player with a trail of particles from where they were to where they land. */
    private void moveTo(ServerPlayer player, Vec3 spot) {
        Vec3 from = player.position();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1.0, from.z, 12, 0.3, 0.6, 0.3, 0.05);
        player.teleportTo(level, spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        player.resetFallDistance();
        ((ServerGamePacketListenerImplAccessor) player.connection).enhanced$setAboveGroundTickCount(0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, spot.x, spot.y + 1.0, spot.z, 12, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    // ---- where fighters are put (a boss with its own ground overrides these) ----

    /**
     * Where a fighter who strayed from the field is put: three blocks inside the field's edge, on the ground there.
     * (Ultron's island has its own surface.)
     */
    protected Vec3 rescueSpot(ServerPlayer player) {
        Vec3 inward = inwardPoint(player, 3.0);
        Vec3 surface = surfaceSpot(player, inward.x, inward.z);
        return surface != null ? surface : center;
    }

    /** Where the wall puts a fighter back: a block and a half inside it, at their own height if there is room. */
    protected Vec3 pushSpot(ServerPlayer player) {
        Vec3 inward = inwardPoint(player, 1.5);
        double y = Math.min(player.getY(), top - player.getBbHeight());
        Vec3 spot = new Vec3(inward.x, y, inward.z);
        if (Teleports.fits(player, spot)) {
            return spot;
        }
        Vec3 surface = surfaceSpot(player, inward.x, inward.z);
        return surface != null ? surface : center;
    }

    /**
     * The point {@code margin} blocks inside the containment's edge, along the line from the centre to the player; a
     * player who is already inside it (they strayed up or down) keeps their column.
     */
    protected final Vec3 inwardPoint(ServerPlayer player, double margin) {
        double dx = player.getX() - center.x;
        double dz = player.getZ() - center.z;
        double length = Math.hypot(dx, dz);
        double reach = Math.max(0.5, containRadius - margin);
        if (length <= reach) {
            return new Vec3(player.getX(), center.y, player.getZ());
        }
        return new Vec3(center.x + dx / length * reach, center.y, center.z + dz / length * reach);
    }

    /**
     * The surface of the column at {@code (x, z)} as {@code player} could stand on it: the highest safe spot under the
     * sky, or null if the column has none. The tractor lifts to it and the field rescues to it.
     */
    @Nullable
    protected Vec3 surfaceSpot(ServerPlayer player, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!level.hasChunk(bx >> 4, bz >> 4)) {
            return null;
        }
        return Teleports.groundAt(player, level, bx, bz);
    }
}
