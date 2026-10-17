package com.kolodkingal.enhanced.custom.modules;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.custom.CustomBeams;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.ModuleLogic;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The custom powers of energy (3.3): Energy Blast (a beam on the shared beam system, bolts, or a burst round him), the
 * Force Field, Magic (mystic bolts, a healing ward, a curse), Energy and Power Absorption, and the Sonic Scream.
 */
public final class EnergyPowers {
    public static void register(Map<PowerModule, ModuleLogic> logic) {
        logic.put(PowerModule.ENERGY_BLAST, new Blast());
        logic.put(PowerModule.FORCE_FIELD, new Field());
        logic.put(PowerModule.MAGIC, new Magic());
        logic.put(PowerModule.ABSORPTION, new Absorption());
        logic.put(PowerModule.SONIC_SCREAM, new Scream());
    }

    // ---- Energy Blast ----

    static final class Blast implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            ServerPlayer p = c.player;
            if (c.option("beam")) {
                if (down && c.fed() && !p.isSpectator()) {
                    c.state.set(PowerModule.ENERGY_BLAST, true);
                    ContinuousBeam.start(p, CustomBeams.style(c.color(), c.glow()), c.v(ModuleStats.BEAM_RANGE));
                    c.sound(ModSounds.CUSTOM_BLAST.get(), 0.7F, 1.4F);
                } else if (!down) {
                    stop(c);
                }
                return;
            }
            if (!down || !c.ready()) {
                return;
            }
            if (c.option("burst")) {
                double radius = c.v(ModuleStats.BURST_RADIUS);
                for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), radius)) {
                    Shots.hurt(p, e, c.v(ModuleStats.BURST_DAMAGE));
                    Shots.push(e, p.position(), 0.9, 0.4);
                }
                ServerLevel level = c.level();
                for (int i = 0; i < 60; i++) {
                    Vec3 dir = new Vec3(level.random.nextGaussian(), level.random.nextGaussian() * 0.6, level.random.nextGaussian()).normalize();
                    level.sendParticles(new EnergyParticleOptions(i % 3 == 0 ? c.glow() : c.color(), 1.4F), p.getX(), p.getY() + 1, p.getZ(),
                            0, dir.x, dir.y, dir.z, radius * 0.12);
                }
                Shots.ring(level, p.position(), radius, c.color());
                c.sound(ModSounds.CUSTOM_BLAST.get(), 1.2F, 0.7F);
                c.cooldown(ModuleStats.BURST_COOL);
                return;
            }
            Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.BOLT_RANGE));
            Shots.line(c.level(), Shots.hand(p), aim.end(), c.color(), c.glow());
            if (aim.target() != null) {
                Shots.hurt(p, aim.target(), c.v(ModuleStats.BOLT_DAMAGE));
            }
            c.sparks(aim.end(), 8, 0.15, 1.2F);
            c.sound(ModSounds.CUSTOM_ZAP.get(), 0.9F, 1.0F + c.player.getRandom().nextFloat() * 0.2F);
            c.cooldown(ModuleStats.BOLT_COOLDOWN);
        }

        @Override
        public void tick(Ctx c) {
            if (!c.option("beam") || !c.state.isOn(PowerModule.ENERGY_BLAST)) {
                return;
            }
            if (!c.fed() || c.player.isSpectator() || !ContinuousBeam.isFiring(c.player)) {
                stop(c);
                return;
            }
            float hunger = c.v(ModuleStats.BEAM_HUNGER) * (float) ModuleLogic.scale(CustomHeroConfig.EXHAUSTION_SCALE, 1.0);
            ContinuousBeam.tick(c.player, new BeamSpec(CustomBeams.style(c.color(), c.glow()), c.v(ModuleStats.BEAM_RANGE),
                    c.v(ModuleStats.BEAM_DAMAGE), 5, hunger, ModDamageTypes.HERO_BEAM, null));
        }

        private static void stop(Ctx c) {
            c.state.set(PowerModule.ENERGY_BLAST, false);
            if (ContinuousBeam.isFiring(c.player)) {
                ContinuousBeam.stop(c.player);
            }
        }

        @Override
        public void end(Ctx c) {
            stop(c);
        }
    }

    // ---- the Force Field: a sphere while the key is held; "reflect" sends what is shot at it back ----

    static final class Field implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (down) {
                if (c.state.field()) {
                    return;
                }
                if (!c.ready()) {
                    c.refuse("message.enhanced.custom.field.recharging");
                    return;
                }
                c.state.raiseField(c.now(), c.v(ModuleStats.FIELD_STRENGTH));
                c.state.set(PowerModule.FORCE_FIELD, true);
                c.sound(ModSounds.CUSTOM_SHIELD.get(), 1.0F, 1.0F);
            } else if (c.state.field()) {
                drop(c, false);
            }
        }

        @Override
        public void tick(Ctx c) {
            if (!c.state.field()) {
                return;
            }
            if (c.now() - c.state.fieldSince() >= c.ticks(ModuleStats.FIELD_TIME)) {
                drop(c, false);
                return;
            }
            ServerPlayer p = c.player;
            if (p.tickCount % 10 == 0) {
                c.exhaust(0.05F);
            }
        }

        static void drop(Ctx c, boolean shattered) {
            c.state.dropField();
            c.state.set(PowerModule.FORCE_FIELD, false);
            ServerPlayer p = c.player;
            if (shattered) {
                c.level().sendParticles(new EnergyParticleOptions(c.color(), 1.5F), p.getX(), p.getY() + 1, p.getZ(), 40, 1.0, 1.0, 1.0, 0.1);
                c.sound(SoundEvents.GLASS_BREAK, 1.0F, 0.8F);
                c.say("message.enhanced.custom.field.broken");
            } else {
                c.sound(ModSounds.CUSTOM_SHIELD.get(), 0.6F, 0.6F);
            }
            int ticks = c.ticks(ModuleStats.FIELD_COOLDOWN);
            if (c.slot > 0) {
                c.data.startCooldown(c.slot, c.now(), shattered ? ticks * 3 / 2 : ticks);
            }
        }

        @Override
        public void end(Ctx c) {
            c.state.dropField();
            c.state.set(PowerModule.FORCE_FIELD, false);
        }
    }

    /** The radius of a Force Field (a little more than the player, as the Chaos Shield is). */
    public static final double FIELD_RADIUS = 1.6;

    /**
     * A hit on a hero whose Force Field is up: the field takes it (all of it, until its strength runs out; then it breaks).
     *
     * @return whether the field stopped the hit
     */
    public static boolean fieldTakes(ServerPlayer player, float amount) {
        CustomState state = CustomHeroes.state(player);
        if (!state.field()) {
            return false;
        }
        float left = state.fieldStrength() - amount;
        state.setFieldStrength(Math.max(0, left));
        ModuleLogic.Ctx c = com.kolodkingal.enhanced.custom.CustomHeroServer.ctx(player, PowerModule.FORCE_FIELD);
        if (c != null) {
            c.sparks(player.position().add(0, 1, 0), 6, 0.6, 1.0F);
            if (left <= 0) {
                Field.drop(c, true);
            }
        }
        CustomHeroes.syncState(player);
        return true;
    }

    /** A projectile meets a Force Field: it is stopped (and broken up), or with "reflect", sent back the way it came. */
    public static void fieldMeets(ServerPlayer player, Projectile projectile) {
        ModuleLogic.Ctx c = com.kolodkingal.enhanced.custom.CustomHeroServer.ctx(player, PowerModule.FORCE_FIELD);
        if (c != null && c.option("reflect")) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-1.0));
            projectile.setOwner(player);
            projectile.hurtMarked = true;
            c.sparks(projectile.position(), 8, 0.1, 1.0F);
            c.sound(SoundEvents.SHIELD_BLOCK, 0.8F, 1.4F);
            fieldTakes(player, 1.0F);
            return;
        }
        WandaAbilities.neutralize(player.serverLevel(), projectile, c == null ? Powers.color(player) : c.color());
        fieldTakes(player, 2.0F);
    }

    // ---- Magic ----

    static final class Magic implements ModuleLogic {
        private record Ward(ServerLevel level, Vec3 center, double radius, long until, int color) {
        }

        private static final Map<UUID, Ward> WARDS = new HashMap<>();

        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.option("ward")) {
                WARDS.put(p.getUUID(), new Ward(c.level(), p.position(), c.v(ModuleStats.WARD_RADIUS), c.now() + c.ticks(ModuleStats.WARD_TIME), c.color()));
                c.sound(ModSounds.CUSTOM_SPELL.get(), 1.0F, 1.0F);
                c.say("message.enhanced.custom.magic.ward");
                c.cooldown(ModuleStats.WARD_COOLDOWN);
                return;
            }
            if (c.option("curse")) {
                Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.CURSE_RANGE));
                if (aim.target() == null || !AreaTargets.mayHurt(p, aim.target(), true)) {
                    c.refuse("message.enhanced.custom.no_target");
                    return;
                }
                int ticks = c.ticks(ModuleStats.CURSE_TIME);
                aim.target().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1), p);
                aim.target().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1), p);
                aim.target().addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0), p);
                Shots.line(c.level(), Shots.hand(p), aim.end(), c.color(), c.glow());
                c.level().sendParticles(ParticleTypes.WITCH, aim.end().x, aim.end().y, aim.end().z, 20, 0.3, 0.5, 0.3, 0.05);
                c.sound(ModSounds.CUSTOM_SPELL.get(), 1.0F, 0.6F);
                c.cooldown(ModuleStats.CURSE_COOLDOWN);
                return;
            }
            // A mystic bolt finds a creature near where he aims, as a spell does.
            double range = c.v(ModuleStats.MYSTIC_RANGE);
            Shots.Aim aim = Shots.aim(p, range);
            LivingEntity target = aim.target();
            Vec3 end = aim.end();
            if (target == null) {
                double best = Math.cos(Math.toRadians(8));
                for (LivingEntity e : Shots.cone(p, range, 8)) {
                    Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(p.getEyePosition()).normalize();
                    double dot = to.dot(p.getLookAngle());
                    if (dot > best && p.hasLineOfSight(e)) {
                        best = dot;
                        target = e;
                        end = e.position().add(0, e.getBbHeight() * 0.5, 0);
                    }
                }
            }
            Shots.line(c.level(), Shots.hand(p), end, c.color(), c.glow());
            c.level().sendParticles(ParticleTypes.ENCHANT, end.x, end.y, end.z, 16, 0.3, 0.3, 0.3, 0.5);
            if (target != null) {
                Shots.hurt(p, target, c.v(ModuleStats.MYSTIC_DAMAGE));
            }
            c.sound(ModSounds.CUSTOM_SPELL.get(), 0.8F, 1.5F);
            c.cooldown(ModuleStats.MYSTIC_COOLDOWN);
        }

        @Override
        public void tick(Ctx c) {
            Ward ward = WARDS.get(c.player.getUUID());
            if (ward == null) {
                return;
            }
            if (c.now() > ward.until() || ward.level() != c.level()) {
                WARDS.remove(c.player.getUUID());
                return;
            }
            if (c.player.tickCount % 10 != 0) {
                return;
            }
            Shots.ring(c.level(), ward.center(), ward.radius(), ward.color());
            int amplifier = c.strength() >= 6 ? 1 : 0;
            AABB box = new AABB(ward.center(), ward.center()).inflate(ward.radius());
            for (LivingEntity e : c.level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (e.position().distanceTo(ward.center()) > ward.radius()) {
                    continue;
                }
                if (e instanceof Player) {
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, amplifier, true, true));
                    e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, true));
                } else if (e instanceof Enemy && AreaTargets.mayHurt(c.player, e, false)) {
                    Shots.push(e, ward.center(), 0.6, 0.2);
                }
            }
        }

        @Override
        public void end(Ctx c) {
            WARDS.remove(c.player.getUUID());
        }

        @Override
        public void forget(ServerPlayer player) {
            WARDS.remove(player.getUUID());
        }

        @Override
        public void clear() {
            WARDS.clear();
        }
    }

    // ---- Absorption: "energy" soaks up fire, lightning, blasts and magic and lets it out again; "power" drains ----

    /** Whether damage of this kind is energy Absorption can take in. */
    public static boolean isEnergy(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_LIGHTNING) || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(ModTags.MAGIC_DAMAGE) || source.is(ModDamageTypes.HERO_BEAM) || source.is(ModDamageTypes.HERO_POWER);
    }

    /**
     * Energy damage on a hero with "energy" Absorption: as much as he can still store is taken off the hit and stored.
     *
     * @return what is left of the hit
     */
    public static float absorb(ServerPlayer player, float amount) {
        ModuleLogic.Ctx c = com.kolodkingal.enhanced.custom.CustomHeroServer.ctx(player, PowerModule.ABSORPTION);
        if (c == null || !c.option("energy")) {
            return amount;
        }
        float room = c.v(ModuleStats.ABSORB_CAPACITY) - c.state.charge();
        float taken = Math.max(0, Math.min(room, amount));
        if (taken > 0) {
            c.state.setCharge(c.state.charge() + taken);
            c.sparks(player.position().add(0, 1, 0), 10, 0.5, 1.1F);
            CustomHeroes.syncState(player);
        }
        return amount - taken;
    }

    static final class Absorption implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.option("power")) {
                drain(c);
                return;
            }
            float charge = c.state.charge();
            if (charge < 1) {
                c.refuse("message.enhanced.custom.absorb.empty");
                return;
            }
            double radius = c.v(ModuleStats.ABSORB_RADIUS);
            for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), radius)) {
                Shots.hurt(p, e, charge);
                Shots.push(e, p.position(), 1.0, 0.4);
            }
            c.state.setCharge(0);
            Shots.ring(c.level(), p.position(), radius, c.color());
            c.sparks(p.position().add(0, 1, 0), 50, radius * 0.3, 1.5F);
            c.sound(ModSounds.CUSTOM_BLAST.get(), 1.2F, 0.9F);
            c.cooldown(ModuleStats.ABSORB_COOLDOWN);
        }

        private static void drain(Ctx c) {
            ServerPlayer p = c.player;
            Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.DRAIN_RANGE));
            LivingEntity target = aim.target();
            if (target == null || !AreaTargets.mayHurt(p, target, true)) {
                c.refuse("message.enhanced.custom.no_target");
                return;
            }
            float damage = c.v(ModuleStats.DRAIN_DAMAGE);
            if (Shots.hurt(p, target, damage)) {
                p.heal(damage * 0.5F);
            }
            if (target instanceof ServerPlayer hero && Powers.power(hero).hasPowers()) {
                int ticks = c.ticks(ModuleStats.DRAIN_SUPPRESS);
                CustomHeroes.suppress(hero, ticks);
                hero.displayClientMessage(Component.translatable("message.enhanced.custom.absorb.drained", p.getDisplayName(), ticks / 20)
                        .withStyle(s -> s.withColor(c.color())), true);
                c.say("message.enhanced.custom.absorb.took", hero.getDisplayName(), ticks / 20);
            } else {
                List<MobEffectInstance> stolen = new ArrayList<>();
                for (MobEffectInstance effect : target.getActiveEffects()) {
                    if (effect.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
                        stolen.add(new MobEffectInstance(effect.getEffect(), Math.min(effect.getDuration(), 400), effect.getAmplifier()));
                    }
                }
                for (MobEffectInstance effect : stolen) {
                    target.removeEffect(effect.getEffect());
                    p.addEffect(effect);
                }
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
            }
            Shots.line(c.level(), aim.end(), Shots.hand(p), c.color(), c.glow());
            c.sound(ModSounds.CUSTOM_SPELL.get(), 1.0F, 0.5F);
            c.cooldown(ModuleStats.DRAIN_COOLDOWN);
        }

        @Override
        public void end(Ctx c) {
            c.state.setCharge(0);
        }
    }

    // ---- the Sonic Scream: a cone in front of him, or a ring all round ----

    static final class Scream implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            boolean ring = c.option("ring");
            List<LivingEntity> hit = ring ? Shots.area(p, p.position().add(0, 1, 0), c.v(ModuleStats.RING_RADIUS))
                    : Shots.cone(p, c.v(ModuleStats.SCREAM_RANGE), 35);
            for (LivingEntity e : hit) {
                Shots.hurt(p, e, c.v(ring ? ModuleStats.RING_DAMAGE : ModuleStats.SCREAM_DAMAGE));
                Shots.push(e, p.position(), ring ? 1.0 : 1.3, 0.3);
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), p);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), p);
            }
            ServerLevel level = c.level();
            if (ring) {
                double radius = c.v(ModuleStats.RING_RADIUS);
                for (double r = 1; r <= radius; r += 1.5) {
                    Shots.ring(level, p.position().add(0, 1, 0), r, c.color());
                }
            } else {
                double range = c.v(ModuleStats.SCREAM_RANGE);
                for (double d = 1.5; d <= range; d += 2.0) {
                    Vec3 at = p.getEyePosition().add(p.getLookAngle().scale(d));
                    level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
                }
                shatterGlass(c, range);
            }
            c.sound(ModSounds.CUSTOM_SCREAM.get(), 1.5F, 1.0F);
            c.cooldown(ring ? ModuleStats.RING_COOLDOWN : ModuleStats.SCREAM_COOLDOWN);
        }

        /** With {@code custom_hero.blockChanges}, glass along the scream's line shatters (each block asks the claims). */
        private static void shatterGlass(Ctx c, double range) {
            if (!EnhancedConfig.SPEC.isLoaded() || !CustomHeroConfig.BLOCK_CHANGES.get()) {
                return;
            }
            ServerPlayer p = c.player;
            int broken = 0;
            for (double d = 1; d <= range && broken < 16; d += 0.5) {
                BlockPos pos = BlockPos.containing(p.getEyePosition().add(p.getLookAngle().scale(d)));
                BlockState state = c.level().getBlockState(pos);
                if ((state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)) && BlockRights.mayBreak(p, c.level(), pos, state)) {
                    c.level().destroyBlock(pos, false, p);
                    broken++;
                }
            }
        }
    }

    /** Whether {@code e} is inside the Force Field of the hero (a projectile that reaches it is stopped there). */
    public static boolean insideField(ServerPlayer hero, Entity e) {
        return CustomHeroes.state(hero).field() && e.position().distanceTo(hero.position().add(0, hero.getBbHeight() * 0.5, 0)) <= FIELD_RADIUS + 0.5;
    }

    private EnergyPowers() {
    }
}
