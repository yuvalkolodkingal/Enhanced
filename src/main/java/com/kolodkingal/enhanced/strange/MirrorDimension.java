package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.asgard.AsgardRules;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.StrangeFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.road.RoadDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Doctor Strange's Mirror Dimension (Sneak + N, a switch), 3.6, simplified: in the films a kaleidoscope copy of the world
 * where nothing breaks in the real one; here a sphere of radius {@code mirrorRadius} round the spot where he cast it,
 * {@code mirrorSeconds} long, one per Strange. Inside it:
 * <ol>
 * <li>nothing of the real world breaks: explosions break no blocks, fire does not spread, no creature griefs, and nobody
 * but him breaks or places a block ({@code BlockRights} asks {@link #protects}; the vanilla events are {@link
 * StrangeEvents}');</li>
 * <li>a creature he hurt inside cannot leave: the edge sets it back inside (a player only while PvP is on); anyone else
 * walks in and out;</li>
 * <li>his spells do {@code mirrorDamageBonus} more;</li>
 * <li>every client inside sees the world bend (the client's {@code MirrorView}, off with {@code reduceMotion}), and hears
 * the sphere's shimmer ({@code strange_mirror_loop}, a looping sound the client plays while the sphere stands).</li>
 * </ol>
 * Refused in a boss's arena, on the Witches' Road and in Asgard's protected city.
 */
public final class MirrorDimension {
    /** A Mirror Dimension: whose, where, how far, until when, and who may not leave it. */
    public record Mirror(UUID owner, ResourceKey<Level> dimension, Vec3 center, double radius, long until,
                         Set<UUID> held) {
        public boolean contains(Vec3 at) {
            return at.distanceToSqr(center) <= radius * radius;
        }
    }

    private static final Map<UUID, Mirror> MIRRORS = new HashMap<>();

    public static int radius() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.MIRROR_RADIUS.get(), 14);
    }

    public static int ticks() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.MIRROR_SECONDS.get(), 20) * 20;
    }

    public static int cooldown() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.MIRROR_COOLDOWN.get(), 1200);
    }

    public static double damageBonus() {
        return StrangeAbilities.cfg(() -> StrangeConfig.MIRROR_DAMAGE_BONUS.get(), 0.2);
    }

    // ---- queries ----

    /** This Strange's Mirror Dimension, or null. */
    @Nullable
    public static Mirror of(UUID owner) {
        return MIRRORS.get(owner);
    }

    /** The Mirror Dimension that holds {@code at} in {@code level}, or null. */
    @Nullable
    public static Mirror at(Level level, Vec3 at) {
        if (MIRRORS.isEmpty()) {
            return null;
        }
        for (Mirror mirror : MIRRORS.values()) {
            if (mirror.dimension() == level.dimension() && mirror.contains(at)) {
                return mirror;
            }
        }
        return null;
    }

    /**
     * Whether a Mirror Dimension keeps the block at {@code pos} as it is against {@code player} (anyone but the Strange
     * whose sphere it is; null: no player, an explosion or a creature). False while there is none.
     */
    public static boolean protects(@Nullable ServerPlayer player, ServerLevel level, BlockPos pos) {
        Mirror mirror = at(level, Vec3.atCenterOf(pos));
        return mirror != null && (player == null || !mirror.owner().equals(player.getUUID()));
    }

    /** Whether no block may break at {@code pos} (whoever asks): explosions, fire, griefing creatures. */
    public static boolean shields(Level level, BlockPos pos) {
        return at(level, Vec3.atCenterOf(pos)) != null;
    }

    // ---- the switch ----

    static void toggle(ServerPlayer player, PowerData data) {
        if (MIRRORS.containsKey(player.getUUID()) || data.toggle(Toggle.STRANGE_MIRROR)) {
            close(player, data, true);
        } else {
            open(player, data);
        }
    }

    /** Why the Mirror Dimension cannot open here (a lang key), or null. */
    @Nullable
    static String refusal(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos at = player.blockPosition();
        if (BossFights.inAnyArena(level, at)) {
            return "message.enhanced.strange.mirror_arena";
        }
        if (RoadDimension.in(player)) {
            return "message.enhanced.strange.mirror_road";
        }
        if (AsgardRules.protectsCity(null, level, at)) {
            return "message.enhanced.strange.mirror_asgard";
        }
        return null;
    }

    static void open(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.STRANGE_PORTAL, now)) {
            return;
        }
        String refusal = refusal(player);
        if (refusal != null) {
            StrangeAbilities.say(player, refusal);
            return;
        }
        if (StrangeAbilities.refuseHungry(player)) {
            return;
        }
        Vec3 center = player.position().add(0, 1.0, 0);
        int radius = radius();
        MIRRORS.put(player.getUUID(), new Mirror(player.getUUID(), level.dimension(), center, radius, now + ticks(),
                new HashSet<>()));
        data.setToggle(Toggle.STRANGE_MIRROR, true);
        data.set(Live.STRANGE_MIRROR, true);
        StrangeAbilities.spend(player, data, 3.0F);
        StrangeAbilities.sound(player, ModSounds.STRANGE_MIRROR_IN.get(), 1.2F, 1.0F);
        StrangeSignals.near(level, center, StrangeFxPayload.MIRROR_ON, player, List.of(center), radius * 10);
    }

    /** The Mirror Dimension closes (the switch, the time, the power lost, he left); the recharge starts if it was open. */
    static void close(ServerPlayer player, PowerData data, boolean tell) {
        Mirror mirror = MIRRORS.remove(player.getUUID());
        boolean was = data.toggle(Toggle.STRANGE_MIRROR) || data.is(Live.STRANGE_MIRROR) || mirror != null;
        data.setToggle(Toggle.STRANGE_MIRROR, false);
        data.set(Live.STRANGE_MIRROR, false);
        if (!was) {
            return;
        }
        data.startCooldown(HeroAbility.STRANGE_PORTAL, player.level().getGameTime(), cooldown());
        Vec3 center = mirror != null ? mirror.center() : player.position();
        ServerLevel level = mirror == null ? player.serverLevel() : player.server.getLevel(mirror.dimension());
        if (level != null) {
            level.playSound(null, center.x, center.y, center.z, ModSounds.STRANGE_MIRROR_OUT.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.0F);
            StrangeSignals.near(level, center, StrangeFxPayload.MIRROR_OFF, player, List.of(center), 0);
        }
    }

    // ---- the tick ----

    static void tick(ServerPlayer player, PowerData data, long now) {
        Mirror mirror = MIRRORS.get(player.getUUID());
        if (mirror == null) {
            if (data.toggle(Toggle.STRANGE_MIRROR) || data.is(Live.STRANGE_MIRROR)) {
                data.setToggle(Toggle.STRANGE_MIRROR, false);
                data.set(Live.STRANGE_MIRROR, false);
            }
            return;
        }
        if (now >= mirror.until() || !data.has(HeroAbility.STRANGE_PORTAL)
                || player.level().dimension() != mirror.dimension()) {
            close(player, data, true);
            return;
        }
        keepIn(player.serverLevel(), mirror);
    }

    /** Whoever he hurt inside cannot leave: one at the edge is set back inside, facing in. */
    static void keepIn(ServerLevel level, Mirror mirror) {
        double inner = mirror.radius() - 1.0;
        mirror.held().removeIf(id -> {
            Entity e = level.getEntity(id);
            if (!(e instanceof LivingEntity living) || !living.isAlive()) {
                return true;
            }
            if (living instanceof Player p && !p.getServer().isPvpAllowed()) {
                return true;
            }
            Vec3 at = living.position();
            Vec3 off = at.subtract(mirror.center());
            if (off.lengthSqr() > inner * inner) {
                Vec3 in = off.lengthSqr() < 1.0E-4 ? Vec3.ZERO : off.normalize();
                Vec3 back = mirror.center().add(in.scale(inner - 0.5));
                back = new Vec3(back.x, Math.max(back.y, at.y - 1.0), back.z);
                if (living instanceof ServerPlayer p) {
                    p.teleportTo(level, back.x, at.y, back.z, p.getYRot(), p.getXRot());
                } else {
                    living.teleportTo(back.x, at.y, back.z);
                }
                living.setDeltaMovement(in.scale(-0.4).add(0, 0.1, 0));
                living.hurtMarked = true;
            }
            return false;
        });
    }

    // ---- the harm ----

    /**
     * A harm inside a Mirror Dimension: dealt by its Strange, its victim is held inside from now on (a player only with
     * PvP on), and his spells do more there. Returns the amount.
     */
    public static float onHurt(LivingEntity victim, DamageSource source, float amount) {
        if (MIRRORS.isEmpty() || !(source.getEntity() instanceof ServerPlayer caster)) {
            return amount;
        }
        Mirror mirror = MIRRORS.get(caster.getUUID());
        if (mirror == null || victim == caster || mirror.dimension() != victim.level().dimension()
                || !mirror.contains(victim.position())) {
            return amount;
        }
        if (!(victim instanceof Player) || caster.server.isPvpAllowed()) {
            mirror.held().add(victim.getUUID());
        }
        if (isSpell(source)) {
            amount *= (float) (1.0 + damageBonus());
        }
        return amount;
    }

    /** Whether a harm is one of Doctor Strange's spells (the whip, the bands). */
    static boolean isSpell(DamageSource source) {
        return source.is(ModDamageTypes.ELDRITCH_WHIP) || source.is(ModDamageTypes.CRIMSON_BANDS);
    }

    /** Whether {@code e} is held inside a Mirror Dimension (it may not leave). */
    public static boolean holds(Entity e) {
        for (Mirror mirror : MIRRORS.values()) {
            if (mirror.held().contains(e.getUUID())) {
                return true;
            }
        }
        return false;
    }

    /** Every Mirror Dimension open now (the clients' warp is told by the payloads; a test may ask). */
    public static Iterable<Mirror> all() {
        return MIRRORS.values();
    }

    /** The data of a Strange whose Mirror is open, for a server-side caller with only a UUID. */
    static boolean isOpen(Player player) {
        return MIRRORS.containsKey(player.getUUID()) && Powers.get(player).has(HeroAbility.STRANGE_PORTAL);
    }

    static void clear() {
        MIRRORS.clear();
    }

    private MirrorDimension() {
    }
}
