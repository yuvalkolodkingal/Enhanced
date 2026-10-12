package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.entity.SandBoulderEntity;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.entity.SandLanceEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What each of the boss's nine attacks does, in four moments (spec 3.6): it <em>begins</em> (the tell: a pose, a glow, a
 * sound), it <em>tracks</em> its target until the aim moment, it <em>locks</em> its aim (after which it does not follow,
 * so it can be dodged), and it <em>fires</em> at the end of the tell. The brain calls these in order; every number is in
 * {@code [apocalypse_boss]}. Everything that hurts is a projectile or a hazard that Wanda's Chaos Shield already
 * understands (the damage type {@code sand} is a projectile; his fist is a melee blow).
 */
final class ApocalypseAttacks {
    private static final double FAN_DEGREES = 7.0;
    private static final float LANCE_SPEED = 1.9F;
    private static final float BOULDER_SPEED = 1.3F;
    private static final double WAVE_SPEED = 0.8;
    private static final double WAVE_LENGTH = 30.0;
    private static final double WAVE_WIDTH = 4.0;
    private static final double WAVE_HEIGHT = 1.1;
    private static final double SPIKE_RADIUS = 1.8;
    private static final double TORNADO_RADIUS = 3.2;
    private static final double BACKHAND_REACH = 3.6;

    // ---- begin ----

    /** The tell begins. @return false if the attack has nothing to aim at (then it does not start) */
    static boolean begin(ApocalypseBrain brain, ApocalypseEntity boss, ApocalypseFight fight, ApocalypseAttack attack, LivingEntity target) {
        ServerLevel level = (ServerLevel) boss.level();
        brain.aim = chest(target);
        boss.setAim(brain.aim);
        face(boss, target.position());
        switch (attack) {
            case LANCES -> {
                streak(level, boss, target, attack.tellTicks());
                sound(level, boss, ModSounds.SAND_WAVE.get(), 0.8F, 1.8F);
            }
            case WAVE -> {
                brain.aim = target.position();
                sound(level, boss, ModSounds.SAND_WAVE.get(), 0.7F, 0.6F);
            }
            case SPIKES -> {
                brain.targets.addAll(pick(fight.foes(), Math.min(1 + fight.fighterCount(), 4), boss));
                for (LivingEntity foe : brain.targets) {
                    brain.points.add(foe.position());
                }
                sound(level, boss, ModSounds.SAND_SPIKE.get(), 0.6F, 0.6F);
            }
            case BOULDERS -> {
                List<ServerPlayer> picked = pick(fight.foes(), 3, boss);
                for (int i = 0; i < 3; i++) {
                    LivingEntity foe = picked.get(i % picked.size());
                    brain.targets.add(foe);
                    brain.points.add(chest(foe));
                }
                sound(level, boss, ModSounds.SAND_SPIKE.get(), 0.7F, 0.5F);
            }
            case ENTOMB -> {
                LivingEntity victim = entombVictim(fight, fight.foes(), boss);
                if (victim == null) {
                    return false;
                }
                brain.victim = victim;
                brain.aim = victim.position();
                streak(level, boss, victim, attack.tellTicks());
                sound(level, boss, ModSounds.ENTOMB.get(), 0.6F, 0.7F);
            }
            case MIND_CRUSH -> {
                brain.interrupted = false;
                Vec3 head = boss.position().add(0, boss.getBbHeight() * 0.85, 0);
                ApocalypseFx.send(level, boss.position(), ApocalypseFxPayload.RING_TELL, boss.getId(), head, Vec3.ZERO, attack.tellTicks(),
                        ApocalypseBossConfig.MIND_CRUSH_RANGE.get());
                sound(level, boss, ModSounds.APOCALYPSE_MIND_CRUSH.get(), 1.2F, 1.0F);
            }
            case PORTAL_STEP -> {
                if (brain.exit == null) {
                    return false;
                }
                ApocalypseFx.send(level, boss.position(), ApocalypseFxPayload.PORTAL, boss.getId(), boss.position(), brain.exit,
                        attack.tellTicks() + 12, 25);
                sound(level, boss, ModSounds.APOCALYPSE_PORTAL.get(), 1.0F, 1.0F);
            }
            case BACKHAND -> boss.swing(InteractionHand.MAIN_HAND);
            case TORNADO -> {
                int count = fight.attackPhase() >= 3 ? 3 : 1;
                List<ServerPlayer> picked = pick(fight.foes(), count, boss);
                for (ServerPlayer foe : picked) {
                    Vec3 spot = tornadoSpot(fight, boss, foe);
                    brain.points.add(spot);
                    ApocalypseFx.send(level, spot, ApocalypseFxPayload.TORNADO_TELL, boss.getId(), spot, Vec3.ZERO, attack.tellTicks(),
                            (int) Math.round(TORNADO_RADIUS * 10.0));
                }
                sound(level, boss, ModSounds.SAND_STORM_LOOP.get(), 0.8F, 0.8F);
            }
        }
        return true;
    }

    // ---- track and lock ----

    /** Until the aim moment the attack follows its target. */
    static void track(ApocalypseBrain brain, ApocalypseEntity boss, ApocalypseFight fight, ApocalypseAttack attack, @Nullable LivingEntity target) {
        switch (attack) {
            case LANCES -> {
                if (target != null) {
                    brain.aim = chest(target);
                }
            }
            case WAVE -> {
                if (target != null) {
                    brain.aim = target.position();
                }
            }
            case SPIKES -> follow(brain, false);
            case BOULDERS -> follow(brain, true);
            case ENTOMB -> {
                if (brain.victim != null && brain.victim.isAlive()) {
                    brain.aim = brain.victim.position();
                }
            }
            default -> {
            }
        }
        boss.setAim(brain.aim);
        face(boss, brain.aim);
    }

    private static void follow(ApocalypseBrain brain, boolean chest) {
        for (int i = 0; i < brain.targets.size() && i < brain.points.size(); i++) {
            LivingEntity foe = brain.targets.get(i);
            if (foe.isAlive()) {
                brain.points.set(i, chest ? chest(foe) : foe.position());
            }
        }
    }

    /** The aim moment: from here the attack does not follow, and what it will hit is shown. */
    static void lock(ApocalypseBrain brain, ApocalypseEntity boss, ApocalypseFight fight, ApocalypseAttack attack) {
        ServerLevel level = (ServerLevel) boss.level();
        switch (attack) {
            case WAVE -> {
                Vec3 dir = SandControl.flat(boss.position(), brain.aim);
                if (dir.equals(Vec3.ZERO)) {
                    dir = Vec3.directionFromRotation(0.0F, boss.getYRot());
                }
                brain.aim = boss.position().add(dir);
                ApocalypseFx.send(level, boss.position(), ApocalypseFxPayload.WAVE_TELL, boss.getId(), boss.position(), dir,
                        attack.tellTicks() - attack.lockTick(), (int) WAVE_LENGTH);
            }
            case SPIKES -> {
                for (Vec3 point : brain.points) {
                    Vec3 ground = new Vec3(point.x, SandControl.groundAt(level, point.x, point.y + 1.0, point.z), point.z);
                    ApocalypseFx.send(level, ground, ApocalypseFxPayload.SPIKE_TELL, boss.getId(), ground, Vec3.ZERO,
                            attack.tellTicks() - attack.lockTick(), (int) Math.round(SPIKE_RADIUS * 10.0));
                }
            }
            case BOULDERS -> {
                for (LivingEntity foe : brain.targets) {
                    streak(level, boss, foe, attack.tellTicks() - attack.lockTick());
                }
            }
            default -> {
            }
        }
        boss.setAim(brain.aim);
    }

    // ---- fire ----

    /** The tell is over: the blow lands. */
    static void fire(ApocalypseBrain brain, ApocalypseEntity boss, ApocalypseFight fight, ApocalypseAttack attack) {
        ServerLevel level = (ServerLevel) boss.level();
        switch (attack) {
            case LANCES -> lances(brain, boss, fight, level);
            case WAVE -> wave(brain, boss, fight, level);
            case SPIKES -> {
                for (Vec3 point : brain.points) {
                    Vec3 ground = new Vec3(point.x, SandControl.groundAt(level, point.x, point.y + 1.0, point.z), point.z);
                    SandHazards.spikes(level, boss, fight.id(), ground, SPIKE_RADIUS, 0, ApocalypseBossConfig.SPIKE_DAMAGE.get().floatValue());
                }
                sound(level, boss, ModSounds.SAND_SPIKE.get(), 1.2F, 0.9F);
            }
            case BOULDERS -> boulders(brain, boss, fight, level);
            case ENTOMB -> entomb(brain, boss, fight, level);
            case MIND_CRUSH -> mindCrush(boss, level);
            case PORTAL_STEP -> portalStep(brain, boss, level);
            case BACKHAND -> backhand(boss, level);
            case TORNADO -> {
                for (Vec3 spot : brain.points) {
                    SandHazards.tornado(level, boss, fight.id(), spot, TORNADO_RADIUS, ApocalypseBossConfig.TORNADO_TICKS.get(),
                            ApocalypseBossConfig.STORM_DAMAGE.get().floatValue(), fight.arena().center(), fight.arena().containRadius());
                }
                sound(level, boss, ModSounds.SAND_SPIKE.get(), 1.0F, 0.6F);
            }
        }
    }

    private static void lances(ApocalypseBrain brain, ApocalypseEntity boss, ApocalypseFight fight, ServerLevel level) {
        Vec3 from = palm(boss);
        Vec3 toward = brain.aim.subtract(from);
        if (toward.lengthSqr() < 1.0E-6) {
            toward = Vec3.directionFromRotation(0.0F, boss.getYRot());
        }
        int count = ApocalypseBossConfig.LANCE_COUNT.get();
        float damage = ApocalypseBossConfig.LANCE_DAMAGE.get().floatValue();
        for (int i = 0; i < count; i++) {
            double angle = count == 1 ? 0.0 : -FAN_DEGREES + 2.0 * FAN_DEGREES * i / (count - 1);
            SandLanceEntity lance = new SandLanceEntity(ModEntities.SAND_LANCE.get(), level);
            lance.launch(boss, from, rotateY(toward.normalize(), Math.toRadians(angle)), LANCE_SPEED, damage);
            fight.spawn(lance);
        }
        sound(level, boss, ModSounds.SAND_WAVE.get(), 1.0F, 1.6F);
    }

    private static void wave(ApocalypseBrain brain, ApocalypseEntity boss, ApocalypseFight fight, ServerLevel level) {
        Vec3 dir = SandControl.flat(boss.position(), brain.aim);
        if (dir.equals(Vec3.ZERO)) {
            dir = Vec3.directionFromRotation(0.0F, boss.getYRot());
        }
        SandHazards.WaveSpec spec = new SandHazards.WaveSpec(WAVE_SPEED, WAVE_LENGTH, WAVE_WIDTH, WAVE_HEIGHT,
                ApocalypseBossConfig.WAVE_DAMAGE.get().floatValue(), 40, 1.0);
        SandHazards.wave(level, boss, fight.id(), boss.position(), dir, spec, fight.arena().center(), fight.arena().radius());
        sound(level, boss, ModSounds.SAND_WAVE.get(), 1.3F, 0.8F);
    }

    private static void boulders(ApocalypseBrain brain, ApocalypseEntity boss, ApocalypseFight fight, ServerLevel level) {
        float damage = ApocalypseBossConfig.BOULDER_DAMAGE.get().floatValue();
        for (int i = 0; i < brain.points.size(); i++) {
            // They tear out of the ground and orbit over his head: they start there, spread round it.
            double angle = Math.PI * 2.0 * i / Math.max(1, brain.points.size());
            Vec3 from = boss.position().add(Math.cos(angle) * 1.2, boss.getBbHeight() + 0.8, Math.sin(angle) * 1.2);
            SandBoulderEntity boulder = new SandBoulderEntity(ModEntities.SAND_BOULDER.get(), level);
            Vec3 toward = brain.points.get(i).subtract(from);
            boulder.launch(boss, from, toward.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0.0F, boss.getYRot()) : toward, BOULDER_SPEED, damage);
            fight.spawn(boulder);
        }
        sound(level, boss, ModSounds.SAND_SPIKE.get(), 1.1F, 1.3F);
    }

    private static void entomb(ApocalypseBrain brain, ApocalypseEntity boss, ApocalypseFight fight, ServerLevel level) {
        LivingEntity victim = brain.victim;
        if (victim == null || !victim.isAlive() || Math.hypot(victim.getX() - brain.aim.x, victim.getZ() - brain.aim.z) > 1.5) {
            // They kept moving after the lock: the hand closes on the sand.
            SandControl.puff(level, brain.aim.add(0, 0.5, 0), 20, 0.5);
            return;
        }
        String refusal = SandControl.entombRefusal(victim);
        if (refusal != null) {
            if (victim instanceof ServerPlayer player && refusal.endsWith("shield_protects")) {
                player.displayClientMessage(Component.translatable(refusal), true);
            }
            return;
        }
        SandControl.entomb(level, boss, victim, ApocalypseBossConfig.ENTOMB_TICKS.get(), ApocalypseBossConfig.ENTOMB_DAMAGE_PER_SECOND.get().floatValue(),
                ApocalypseBossConfig.COCOON_HEALTH.get().floatValue(), fight);
        sound(level, boss, ModSounds.ENTOMB.get(), 1.2F, 1.0F);
    }

    private static void mindCrush(ApocalypseEntity boss, ServerLevel level) {
        double range = ApocalypseBossConfig.MIND_CRUSH_RANGE.get();
        int nausea = ApocalypseBossConfig.MIND_CRUSH_NAUSEA_TICKS.get();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || player.distanceTo(boss) > range || MagnetoAbilities.wearsHelmet(player)) {
                continue; // Magneto's helmet turns all of it away
            }
            if (nausea > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nausea, 0), boss);
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), boss);
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), boss);
        }
        sound(level, boss, ModSounds.APOCALYPSE_MIND_CRUSH.get(), 1.4F, 0.8F);
    }

    /** The Mind Crush was interrupted in its tell (a Phoenix's mind touched his): the ring breaks. */
    static void interrupted(ApocalypseEntity boss, ApocalypseFight fight) {
        ServerLevel level = (ServerLevel) boss.level();
        sound(level, boss, ModSounds.APOCALYPSE_SHIELD_HIT.get(), 1.0F, 0.5F);
        SandControl.puff(level, boss.position().add(0, boss.getBbHeight() * 0.8, 0), 12, 0.4);
    }

    private static void portalStep(ApocalypseBrain brain, ApocalypseEntity boss, ServerLevel level) {
        Vec3 exit = brain.exit;
        if (exit == null || !HexManager.onTeleport(level, boss, exit)) {
            return; // the wall refuses it: he walks instead
        }
        boss.moveTo(exit.x, exit.y, exit.z, boss.getYRot(), boss.getXRot());
        boss.getNavigation().stop();
        boss.setDeltaMovement(Vec3.ZERO);
        float damage = ApocalypseBossConfig.PORTAL_SHOCK_DAMAGE.get().floatValue();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(3.0, 1.5, 3.0), e -> e != boss && SandControl.canHurt(boss, e))) {
            if (victim.distanceTo(boss) > 3.0F + victim.getBbWidth()) {
                continue;
            }
            if (victim.hurt(ModDamageTypes.source(level, ModDamageTypes.SAND, boss, boss), damage)) {
                Vec3 out = SandControl.flat(boss.position(), victim.position());
                victim.knockback(1.2, -out.x, -out.z);
                victim.hurtMarked = true;
            }
        }
        SandControl.puff(level, exit.add(0, 0.5, 0), 30, 0.8);
        sound(level, boss, ModSounds.APOCALYPSE_PORTAL.get(), 1.2F, 0.8F);
    }

    private static void backhand(ApocalypseEntity boss, ServerLevel level) {
        float damage = ApocalypseBossConfig.BACKHAND_DAMAGE.get().floatValue();
        Vec3 forward = Vec3.directionFromRotation(0.0F, boss.yBodyRot);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(BACKHAND_REACH, 1.0, BACKHAND_REACH),
                e -> e != boss && SandControl.canHurt(boss, e))) {
            Vec3 toward = SandControl.flat(boss.position(), victim.position());
            double reach = BACKHAND_REACH + victim.getBbWidth() * 0.5;
            if (ApocalypseBrain.flatDistance(boss, victim) > reach || (!toward.equals(Vec3.ZERO) && toward.dot(forward) < 0.25)) {
                continue;
            }
            if (victim.hurt(ModDamageTypes.source(level, ModDamageTypes.APOCALYPSE_BLOW, boss, boss), damage)) {
                victim.knockback(1.5, -toward.x, -toward.z);
                victim.hurtMarked = true;
            }
        }
        sound(level, boss, ModSounds.SAND_SPIKE.get(), 1.0F, 0.7F);
    }

    // ---- helpers ----

    /** He turns to face a point (the body, the head and the look). */
    static void face(ApocalypseEntity boss, Vec3 point) {
        double dx = point.x - boss.getX();
        double dz = point.z - boss.getZ();
        if (dx * dx + dz * dz < 1.0E-6) {
            return;
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        boss.setYRot(yaw);
        boss.yBodyRot = yaw;
        boss.setYHeadRot(yaw);
    }

    /** The chest of a creature: where a lance or a boulder is aimed. */
    static Vec3 chest(LivingEntity target) {
        return target.position().add(0, target.getBbHeight() * 0.65, 0);
    }

    /** Where his raised hand is: the lances leave from it. */
    static Vec3 palm(ApocalypseEntity boss) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, boss.yBodyRot);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x).scale(-1.0);
        return boss.position().add(0, boss.getBbHeight() * 0.7, 0).add(forward.scale(0.7)).add(right.scale(0.6));
    }

    private static void streak(ServerLevel level, ApocalypseEntity boss, LivingEntity target, int ticks) {
        ApocalypseFx.send(level, boss.position(), ApocalypseFxPayload.STREAK, boss.getId(), palm(boss), chest(target), ticks, target.getId());
    }

    private static void sound(ServerLevel level, ApocalypseEntity boss, net.minecraft.sounds.SoundEvent event, float volume, float pitch) {
        level.playSound(null, boss.getX(), boss.getY(), boss.getZ(), event, SoundSource.HOSTILE, volume, pitch);
    }

    static Vec3 rotateY(Vec3 v, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /** Up to {@code count} different fighters, in a random order. */
    private static List<ServerPlayer> pick(List<ServerPlayer> foes, int count, ApocalypseEntity boss) {
        List<ServerPlayer> shuffled = new ArrayList<>(foes);
        Collections.shuffle(shuffled, new java.util.Random(boss.getRandom().nextLong()));
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /**
     * Whom the grasp reaches for: the fighter who has dealt the most mind-power damage in the last ten seconds, else a
     * random one. Creative and Spectator players are never chosen, nor anyone the sand cannot swallow.
     */
    @Nullable
    private static LivingEntity entombVictim(ApocalypseFight fight, List<ServerPlayer> foes, ApocalypseEntity boss) {
        List<ServerPlayer> eligible = new ArrayList<>();
        for (ServerPlayer foe : foes) {
            if (SandControl.entombRefusal(foe) == null || (SandControl.hasCompleteShield(foe) && !foe.isCreative())) {
                eligible.add(foe);
            }
        }
        if (eligible.isEmpty()) {
            return null;
        }
        ServerPlayer best = null;
        float most = 0.0F;
        for (ServerPlayer foe : eligible) {
            float recent = fight.recentMindDamage(foe.getUUID());
            if (recent > most) {
                most = recent;
                best = foe;
            }
        }
        return best != null ? best : eligible.get(boss.getRandom().nextInt(eligible.size()));
    }

    /** A spot for a tornado to start, six to nine blocks from the fighter, inside the arena. */
    private static Vec3 tornadoSpot(ApocalypseFight fight, ApocalypseEntity boss, LivingEntity foe) {
        ServerLevel level = (ServerLevel) boss.level();
        Vec3 centre = fight.arena().center();
        double angle = boss.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = 6.0 + boss.getRandom().nextDouble() * 3.0;
        double x = foe.getX() + Math.cos(angle) * distance;
        double z = foe.getZ() + Math.sin(angle) * distance;
        double out = Math.hypot(x - centre.x, z - centre.z);
        double reach = Math.max(2.0, fight.arena().containRadius() - 3.0);
        if (out > reach) {
            x = centre.x + (x - centre.x) / out * reach;
            z = centre.z + (z - centre.z) / out * reach;
        }
        return new Vec3(x, SandControl.groundAt(level, x, foe.getY() + 2.0, z), z);
    }

    /**
     * Where a Portal Step lands: right behind the fighter farthest from him, six or more blocks from all the others,
     * inside the arena, with room for his body and without crossing a Hex wall. Null if there is no such place.
     */
    @Nullable
    static Vec3 findExit(ApocalypseEntity boss, ApocalypseFight fight, List<ServerPlayer> foes) {
        if (foes.isEmpty()) {
            return null;
        }
        ServerLevel level = (ServerLevel) boss.level();
        ServerPlayer farthest = foes.get(0);
        for (ServerPlayer foe : foes) {
            if (ApocalypseBrain.flatDistance(boss, foe) > ApocalypseBrain.flatDistance(boss, farthest)) {
                farthest = foe;
            }
        }
        List<Vec3> candidates = new ArrayList<>();
        Vec3 behind = Teleports.behind(boss, farthest);
        if (behind != null) {
            candidates.add(behind);
        }
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            candidates.add(farthest.position().add(Math.cos(angle) * 3.5, 0.0, Math.sin(angle) * 3.5));
        }
        Vec3 centre = fight.arena().center();
        double reach = Math.max(2.0, fight.arena().containRadius() - 2.0);
        for (Vec3 candidate : candidates) {
            if (Math.hypot(candidate.x - centre.x, candidate.z - centre.z) > reach) {
                continue;
            }
            Vec3 spot = new Vec3(candidate.x, SandControl.groundAt(level, candidate.x, farthest.getY() + 1.5, candidate.z), candidate.z);
            if (!Teleports.fits(boss, spot) || Teleports.crossesHex(level, boss, spot)) {
                continue;
            }
            boolean crowded = false;
            for (ServerPlayer other : foes) {
                crowded |= other != farthest && other.position().distanceTo(spot) < 6.0;
            }
            if (!crowded && farthest.position().distanceTo(spot) >= 2.0) {
                return spot;
            }
        }
        return null;
    }

    /** The cocoon that holds a creature, for the callers that need one (the boss's grasp, a test). */
    @Nullable
    static SandCocoonEntity cocoonOf(LivingEntity victim) {
        return SandCocoonEntity.cocoonOf(victim);
    }

    /** The AABB that holds the whole arena: a helper for the fights' scans. */
    static AABB around(Vec3 centre, double radius, double height) {
        return new AABB(centre.x - radius, centre.y - 4.0, centre.z - radius, centre.x + radius, centre.y + height, centre.z + radius);
    }
}
