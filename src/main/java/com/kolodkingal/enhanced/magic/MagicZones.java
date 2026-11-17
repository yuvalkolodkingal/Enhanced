package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What 3.17's spells leave in the world for a while, and the states they put on people: the circles, sigils, domes and
 * fields ({@link Zone}), the portals, the Infinite Hallway, the bound souls, and each caster's timed blessings
 * ({@link #buff}: Foresight, Reversal, the Siphon, Premonition, the broom, the Spirit Walk...). All in memory: a restart
 * ends them, as it ends a spell.
 */
public final class MagicZones {
    public enum Kind {
        PROTECTION_CIRCLE, FLAME_WARD, SEEING_SIGIL, SILENCING_SIGIL, PROTECTION_RUNE, VISHANTI_SHIELD, PROBABILITY_HEX,
        ZERO_GRAVITY, DARK_DIMENSION, COMBAT_PORTAL, PORTAL, HALLWAY, SOUL_BIND, DISKS
    }

    /** A spell's lasting work. */
    public static final class Zone {
        public final Kind kind;
        public final UUID owner;
        public final ResourceKey<Level> dim;
        public Vec3 center;
        public final double radius;
        public final long until;
        public final float strength;
        int fxId;
        /** The one it holds (the Hallway's target, a bound soul). */
        @Nullable
        public UUID target;
        /** Where a portal goes. */
        @Nullable
        public Vec3 dest;
        @Nullable
        public ResourceKey<Level> destDim;
        /** A portal's own action instead of a place (the Coven's Road). */
        @Nullable
        public Runnable onEnter;
        public double startDistance;
        long lastWarn;

        Zone(Kind kind, UUID owner, ResourceKey<Level> dim, Vec3 center, double radius, long until, float strength) {
            this.kind = kind;
            this.owner = owner;
            this.dim = dim;
            this.center = center;
            this.radius = radius;
            this.until = until;
            this.strength = strength;
        }

        boolean contains(Entity e) {
            return e.level().dimension() == dim && e.position().distanceToSqr(center) <= radius * radius;
        }
    }

    private static final List<Zone> ZONES = new ArrayList<>();
    /** Player -> blessing -> game time it ends. */
    private static final Map<UUID, Map<String, Long>> BUFFS = new HashMap<>();
    /** Players whose name tag a silencing sigil hides: the team they were on before (null: none). */
    private static final Map<UUID, String> HIDDEN = new HashMap<>();
    private static final String HIDDEN_TEAM = "enhanced_silenced";

    private MagicZones() {
    }

    public static Zone add(Kind kind, ServerPlayer owner, Vec3 center, double radius, int ticks, float strength) {
        Zone zone = new Zone(kind, owner.getUUID(), owner.level().dimension(), center, radius,
                owner.level().getGameTime() + ticks, strength);
        ZONES.add(zone);
        return zone;
    }

    /** Ties a lasting shape the clients draw to the zone (it is taken down with it). */
    public static void show(Zone zone, int fxId) {
        zone.fxId = fxId;
    }

    public static List<Zone> zones() {
        return ZONES;
    }

    /** Zones of a kind in which {@code e} stands. */
    public static List<Zone> at(Entity e, Kind kind) {
        List<Zone> out = new ArrayList<>();
        for (Zone z : ZONES) {
            if (z.kind == kind && z.contains(e)) {
                out.add(z);
            }
        }
        return out;
    }

    // ---- blessings ----

    public static void buff(Player player, String name, int ticks) {
        BUFFS.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(name, player.level().getGameTime() + ticks);
    }

    public static boolean has(Entity entity, String name) {
        Map<String, Long> map = BUFFS.get(entity.getUUID());
        Long until = map == null ? null : map.get(name);
        return until != null && entity.level().getGameTime() < until;
    }

    public static void unbuff(Entity entity, String name) {
        Map<String, Long> map = BUFFS.get(entity.getUUID());
        if (map != null) {
            map.remove(name);
        }
    }

    /** Any blessing or curse put on someone (mobs too: a curse, a transmutation). */
    public static void mark(Entity entity, String name, int ticks) {
        BUFFS.computeIfAbsent(entity.getUUID(), k -> new HashMap<>()).put(name, entity.level().getGameTime() + ticks);
    }

    // ---- what the zones forbid ----

    /** Whether a caster's magic is held: a silencing sigil or a rune of protection of someone he is no friend of. */
    public static boolean silenced(Player player) {
        for (Zone z : ZONES) {
            if ((z.kind == Kind.SILENCING_SIGIL || z.kind == Kind.PROTECTION_RUNE) && z.contains(player)
                    && !z.owner.equals(player.getUUID())) {
                Player owner = player.level().getPlayerByUUID(z.owner);
                if (owner == null || !Magic.friend(owner, player) || z.kind == Kind.PROTECTION_RUNE && Magic.enemy(owner, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether a hero's powers are held where he stands (a Rune of Protection of an enemy). */
    public static boolean powersStripped(Player player) {
        for (Zone z : ZONES) {
            if (z.kind == Kind.PROTECTION_RUNE && z.contains(player) && !z.owner.equals(player.getUUID())) {
                Player owner = player.level().getPlayerByUUID(z.owner);
                if (owner != null && Magic.enemy(owner, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether magic sent at {@code victim} by {@code attacker} is kept out (a circle of protection, the Vishanti's shield). */
    public static boolean shields(LivingEntity victim, @Nullable Entity attacker) {
        for (Zone z : ZONES) {
            if ((z.kind == Kind.PROTECTION_CIRCLE || z.kind == Kind.VISHANTI_SHIELD) && z.contains(victim)) {
                Player owner = victim.level().getPlayerByUUID(z.owner);
                if (owner == null || !Magic.friend(owner, victim)) {
                    continue;
                }
                if (attacker == null || !z.contains(attacker) || attacker instanceof LivingEntity l && Magic.enemy(owner, l)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether an attacker's blow goes wide (it stands in someone's Probability Hex). */
    public static boolean unlucky(LivingEntity attacker) {
        for (Zone z : ZONES) {
            if (z.kind == Kind.PROBABILITY_HEX && z.contains(attacker)) {
                Player owner = attacker.level().getPlayerByUUID(z.owner);
                if (owner != null && owner != attacker && Magic.enemy(owner, attacker) && attacker.getRandom().nextFloat() < 0.3F) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The zone that binds this soul to someone, or null. */
    @Nullable
    public static Zone soulBindOf(Entity caster) {
        for (Zone z : ZONES) {
            if (z.kind == Kind.SOUL_BIND && z.owner.equals(caster.getUUID())) {
                return z;
            }
        }
        return null;
    }

    // ---- ticking ----

    /** Every server tick. */
    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        Iterator<Zone> it = ZONES.iterator();
        List<Zone> ended = new ArrayList<>();
        while (it.hasNext()) {
            Zone z = it.next();
            ServerLevel level = server.getLevel(z.dim);
            ServerPlayer owner = server.getPlayerList().getPlayer(z.owner);
            if (level == null || now >= z.until || owner == null && z.kind != Kind.PORTAL) {
                it.remove();
                ended.add(z);
                continue;
            }
            if (!tickZone(level, owner, z, now)) {
                it.remove();
                ended.add(z);
            }
        }
        for (Zone z : ended) {
            ServerLevel level = server.getLevel(z.dim);
            if (level != null) {
                MagicFx.cancel(level, z.center, z.fxId);
            }
            if (z.kind == Kind.SILENCING_SIGIL) {
                unhide(server, z.owner);
            }
        }
        if (now % 20 == 0) {
            BUFFS.values().removeIf(map -> {
                map.values().removeIf(until -> until <= now);
                return map.isEmpty();
            });
        }
    }

    private static List<LivingEntity> inside(ServerLevel level, Zone z) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(z.center, z.center).inflate(z.radius),
                e -> e.isAlive() && e.position().distanceToSqr(z.center) <= z.radius * z.radius);
    }

    /** One tick of a zone; false when it is over before its time. */
    private static boolean tickZone(ServerLevel level, @Nullable ServerPlayer owner, Zone z, long now) {
        switch (z.kind) {
            case PROTECTION_CIRCLE, VISHANTI_SHIELD -> {
                if (now % 10 == 0 && owner != null) {
                    for (LivingEntity e : inside(level, z)) {
                        if (Magic.friend(owner, e)) {
                            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, false, true));
                        } else if (Magic.enemy(owner, e) && !(e instanceof Player)) {
                            com.kolodkingal.enhanced.custom.Shots.push(e, z.center, 0.8, 0.2);
                        }
                    }
                }
                if (z.kind == Kind.VISHANTI_SHIELD) {
                    for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(z.center, z.center).inflate(z.radius))) {
                        Entity shooter = p.getOwner();
                        if (owner != null && shooter instanceof LivingEntity l && Magic.enemy(owner, l)
                                && p.position().distanceToSqr(z.center) <= z.radius * z.radius) {
                            MagicFx.burst(level, p.position(), 0xFFF6C8, 6, 0.1, 0.8F);
                            p.discard();
                        }
                    }
                }
            }
            case FLAME_WARD -> {
                if (now % 10 == 0 && owner != null) {
                    for (LivingEntity e : inside(level, z)) {
                        double d = Math.sqrt(e.position().distanceToSqr(z.center));
                        if (d >= z.radius - 1.6 && Magic.enemy(owner, e)) {
                            e.igniteForSeconds(4);
                            e.hurt(ModDamageTypes.source(level, ModDamageTypes.SPELL, owner, owner), 2.0F * z.strength);
                        }
                    }
                }
            }
            case SEEING_SIGIL -> {
                if (now % 20 == 0 && owner != null && now - z.lastWarn > 200) {
                    for (LivingEntity e : inside(level, z)) {
                        if (e != owner && (e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof Player p && !Magic.friend(owner, p))) {
                            z.lastWarn = now;
                            owner.displayClientMessage(Component.translatable("message.enhanced.spell.sigil_warns", e.getDisplayName())
                                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
                            owner.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.8F);
                            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
                            break;
                        }
                    }
                }
            }
            case SILENCING_SIGIL -> {
                if (owner != null && now % 10 == 0) {
                    if (z.contains(owner)) {
                        hide(owner);
                    } else {
                        unhide(level.getServer(), owner.getUUID());
                    }
                }
            }
            case PROTECTION_RUNE -> {
                if (now % 10 == 0 && owner != null) {
                    for (LivingEntity e : inside(level, z)) {
                        if (Magic.friend(owner, e)) {
                            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, false, true));
                        } else if (Magic.enemy(owner, e)) {
                            strip(e);
                        }
                    }
                }
            }
            case PROBABILITY_HEX -> {
                if (now % 10 == 0 && owner != null) {
                    for (LivingEntity e : inside(level, z)) {
                        if (Magic.enemy(owner, e)) {
                            e.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 30, 1, true, false, true));
                            if (e.getRandom().nextFloat() < 0.15F && e.onGround()) {
                                // a stumble
                                e.setDeltaMovement(e.getDeltaMovement().add((e.getRandom().nextDouble() - 0.5) * 0.6, 0.1,
                                        (e.getRandom().nextDouble() - 0.5) * 0.6));
                                e.hurtMarked = true;
                            }
                        }
                    }
                }
            }
            case ZERO_GRAVITY -> {
                if (owner != null) {
                    for (LivingEntity e : inside(level, z)) {
                        if (Magic.enemy(owner, e)) {
                            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 6, 0, true, false, false));
                            e.setDeltaMovement(e.getDeltaMovement().multiply(0.8, 1.0, 0.8));
                        }
                    }
                }
            }
            case DARK_DIMENSION -> {
                if (owner != null) {
                    for (LivingEntity e : inside(level, z)) {
                        if (Magic.enemy(owner, e)) {
                            Vec3 to = z.center.subtract(e.position());
                            if (to.lengthSqr() > 0.5) {
                                e.setDeltaMovement(e.getDeltaMovement().scale(0.7).add(to.normalize().scale(0.18)));
                                e.hurtMarked = true;
                            }
                            if (now % 10 == 0) {
                                e.hurt(ModDamageTypes.source(level, ModDamageTypes.SPELL, owner, owner), 1.0F * z.strength);
                            }
                        }
                    }
                }
            }
            case COMBAT_PORTAL -> {
                for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(z.center, z.center).inflate(z.radius))) {
                    Entity shooter = p.getOwner();
                    if (shooter != null && shooter != owner && !(MagicZones.has(p, "reflected"))
                            && p.position().distanceToSqr(z.center) <= z.radius * z.radius) {
                        reflect(level, p, shooter);
                    }
                }
            }
            case PORTAL -> {
                for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(z.center, z.center).inflate(z.radius, 2.0, z.radius),
                        e -> (e instanceof Player || e instanceof net.minecraft.world.entity.TamableAnimal) && e.isAlive())) {
                    if (has(e, "portal_used")) {
                        continue;
                    }
                    if (e.position().distanceToSqr(z.center) > z.radius * z.radius + 1.0) {
                        continue;
                    }
                    mark(e, "portal_used", 40);
                    if (z.onEnter != null && e instanceof ServerPlayer p) {
                        Portals.through(p, z);
                    } else {
                        Portals.through(e, z);
                    }
                }
            }
            case HALLWAY -> {
                if (owner == null || z.target == null) {
                    return false;
                }
                Entity target = level.getEntity(z.target);
                if (!(target instanceof LivingEntity living) || !living.isAlive() || target.level() != owner.level()) {
                    return false;
                }
                Vec3 from = owner.position();
                Vec3 away = target.position().subtract(from);
                double d = away.length();
                if (d < z.startDistance && d > 1.0E-3) {
                    // however far he walks, the corridor stretches: he is where he was
                    Vec3 back = from.add(away.scale(z.startDistance / d));
                    target.teleportTo(back.x, target.getY(), back.z);
                    target.setDeltaMovement(away.normalize().scale(0.15));
                    target.hurtMarked = true;
                }
            }
            case SOUL_BIND -> {
                if (owner == null || z.target == null) {
                    return false;
                }
                Entity target = level.getEntity(z.target);
                if (!(target instanceof LivingEntity living) || !living.isAlive()) {
                    return false;
                }
                z.center = owner.position();
                if (target.distanceToSqr(owner) > z.radius * z.radius) {
                    Vec3 to = owner.position().subtract(target.position()).normalize();
                    target.setDeltaMovement(to.scale(0.6).add(0, 0.1, 0));
                    target.hurtMarked = true;
                }
                if (now % 10 == 0) {
                    MagicFx.line(level, owner.position().add(0, 1, 0), target.position().add(0, 1, 0), 0x8A2FD6, 0.6F);
                }
            }
            case DISKS -> {
                if (owner == null) {
                    return false;
                }
                z.center = owner.position();
                if (now % 4 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double a = now * 0.3 + i * Math.PI * 2 / 3;
                        Vec3 at = owner.position().add(Math.cos(a) * z.radius, 1.0, Math.sin(a) * z.radius);
                        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(0.9),
                                e -> Magic.enemy(owner, e))) {
                            e.hurt(ModDamageTypes.source(level, ModDamageTypes.SPELL, owner, owner), 3.0F * z.strength);
                        }
                    }
                }
            }
        }
        return true;
    }

    /** A Rune of Protection strips an enemy's magic: his blessings go. */
    private static void strip(LivingEntity e) {
        List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> good = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) {
            if (i.getEffect().value().isBeneficial()) {
                good.add(i.getEffect());
            }
        }
        for (var h : good) {
            e.removeEffect(h);
        }
    }

    /** A projectile turned back at whoever sent it. */
    public static void reflect(ServerLevel level, Projectile p, Entity shooter) {
        Vec3 to = shooter.position().add(0, shooter.getBbHeight() * 0.5, 0).subtract(p.position());
        double speed = Math.max(0.8, p.getDeltaMovement().length());
        p.setDeltaMovement(to.normalize().scale(speed));
        p.hurtMarked = true;
        mark(p, "reflected", 100);
        MagicFx.burst(level, p.position(), 0x5CC8FF, 8, 0.15, 0.9F);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    // ---- the silencing sigil's hidden name ----

    private static void hide(ServerPlayer player) {
        if (HIDDEN.containsKey(player.getUUID())) {
            return;
        }
        Scoreboard board = player.level().getServer().getScoreboard();
        PlayerTeam before = player.getTeam();
        if (before != null && !HIDDEN_TEAM.equals(before.getName())) {
            return; // (a player on a team of his own keeps it: the sigil does not take it from him)
        }
        PlayerTeam team = board.getPlayerTeam(HIDDEN_TEAM);
        if (team == null) {
            team = board.addPlayerTeam(HIDDEN_TEAM);
            team.setNameTagVisibility(Team.Visibility.NEVER);
        }
        board.addPlayerToTeam(player.getScoreboardName(), team);
        HIDDEN.put(player.getUUID(), "");
    }

    private static void unhide(MinecraftServer server, UUID id) {
        if (HIDDEN.remove(id) == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            PlayerTeam team = server.getScoreboard().getPlayerTeam(HIDDEN_TEAM);
            if (team != null && player.getTeam() == team) {
                server.getScoreboard().removePlayerFromTeam(player.getScoreboardName(), team);
            }
        }
    }

    // ---- lifecycle ----

    /** Ends every zone of a player (he left, or lost his magic). */
    public static void endOf(MinecraftServer server, UUID owner) {
        Iterator<Zone> it = ZONES.iterator();
        while (it.hasNext()) {
            Zone z = it.next();
            if (z.owner.equals(owner)) {
                it.remove();
                ServerLevel level = server.getLevel(z.dim);
                if (level != null) {
                    MagicFx.cancel(level, z.center, z.fxId);
                }
            }
        }
        unhide(server, owner);
    }

    public static void clear(@Nullable MinecraftServer server) {
        if (server != null) {
            for (UUID id : new ArrayList<>(HIDDEN.keySet())) {
                unhide(server, id);
            }
        }
        ZONES.clear();
        BUFFS.clear();
        HIDDEN.clear();
    }
}
