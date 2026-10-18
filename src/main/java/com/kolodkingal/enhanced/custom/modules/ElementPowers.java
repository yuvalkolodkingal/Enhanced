package com.kolodkingal.enhanced.custom.modules;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.CustomHeroConfig;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.ModuleLogic;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The elemental custom powers (3.3): Fire, Ice, Lightning, Water, Earth, Wind and Weather Control. Each element also
 * guards its master from itself (fire, cold, lightning, drowning, falls: {@code CustomEvents}).
 */
public final class ElementPowers {
    public static void register(Map<PowerModule, ModuleLogic> logic) {
        logic.put(PowerModule.FIRE, new Fire());
        logic.put(PowerModule.ICE, new Ice());
        logic.put(PowerModule.LIGHTNING, new Lightning());
        logic.put(PowerModule.WATER, new Water());
        logic.put(PowerModule.EARTH, new Earth());
        logic.put(PowerModule.WIND, new Wind());
        logic.put(PowerModule.WEATHER, new Weather());
    }

    /** A bolt of light that strikes {@code at} and does nothing by itself (no fire, no damage: the power deals its own). */
    static void visualBolt(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    // ---- Fire ----

    static final class Fire implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            ServerPlayer p = c.player;
            if (c.option("flamethrower")) {
                c.state.set(PowerModule.FIRE, down && c.fed() && !p.isSpectator());
                if (down) {
                    c.sound(SoundEvents.FIRECHARGE_USE, 0.8F, 0.8F);
                }
                return;
            }
            if (!down) {
                return;
            }
            if (c.option("aura")) {
                boolean on = !c.state.isOn(PowerModule.FIRE);
                if (on && !c.fed()) {
                    c.refuse("message.enhanced.custom.hungry");
                    return;
                }
                c.state.set(PowerModule.FIRE, on);
                c.sound(SoundEvents.FIRECHARGE_USE, 0.8F, on ? 1.2F : 0.7F);
                c.say(on ? "message.enhanced.custom.fire.aura_on" : "message.enhanced.custom.fire.aura_off");
                return;
            }
            if (!c.ready()) {
                return;
            }
            Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.FIREBALL_RANGE));
            Shots.line(c.level(), Shots.hand(p), aim.end(), ParticleTypes.FLAME, 2.0);
            Shots.line(c.level(), Shots.hand(p), aim.end(), c.color(), c.glow());
            if (aim.target() != null) {
                Shots.hurt(p, aim.target(), c.v(ModuleStats.FIREBALL_DAMAGE));
                aim.target().igniteForTicks(c.ticks(ModuleStats.FIREBALL_BURN));
            }
            c.level().sendParticles(ParticleTypes.LAVA, aim.end().x, aim.end().y, aim.end().z, 6, 0.2, 0.2, 0.2, 0.0);
            c.sound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
            c.cooldown(ModuleStats.FIREBALL_COOLDOWN);
        }

        @Override
        public void tick(Ctx c) {
            ServerPlayer p = c.player;
            p.clearFire();
            if (!c.state.isOn(PowerModule.FIRE)) {
                return;
            }
            if (!c.fed() || p.isSpectator()) {
                c.state.set(PowerModule.FIRE, false);
                return;
            }
            if (c.option("flamethrower")) {
                if (p.tickCount % 4 == 0) {
                    double range = c.v(ModuleStats.FLAME_RANGE);
                    for (LivingEntity e : Shots.cone(p, range, 20)) {
                        Shots.hurt(p, e, c.v(ModuleStats.FLAME_DAMAGE), ModDamageTypes.HERO_BEAM);
                        e.igniteForSeconds(3);
                    }
                    c.exhaust(0.1F);
                }
                Vec3 from = Shots.hand(p);
                for (int i = 0; i < 4; i++) {
                    Vec3 v = p.getLookAngle().scale(0.7 + p.getRandom().nextDouble() * 0.3)
                            .add(p.getRandom().nextGaussian() * 0.08, p.getRandom().nextGaussian() * 0.08, p.getRandom().nextGaussian() * 0.08);
                    c.level().sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 0, v.x, v.y, v.z, 0.6);
                }
                if (p.tickCount % 8 == 0) {
                    c.level().sendParticles(new EnergyParticleOptions(c.color(), 1.2F), from.x, from.y, from.z, 0, p.getLookAngle().x,
                            p.getLookAngle().y, p.getLookAngle().z, 0.5);
                    c.sound(SoundEvents.FIRE_AMBIENT, 0.8F, 1.3F);
                }
            } else if (c.option("aura") && p.tickCount % 10 == 0) {
                double radius = c.v(ModuleStats.AURA_RADIUS);
                for (LivingEntity e : Shots.area(p, p.position(), radius)) {
                    Shots.hurt(p, e, c.v(ModuleStats.AURA_DAMAGE));
                    e.igniteForSeconds(2);
                }
                c.level().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.8, p.getZ(), 12, radius * 0.4, 0.5, radius * 0.4, 0.01);
                c.exhaust(0.15F);
            }
        }

        @Override
        public void end(Ctx c) {
            c.state.set(PowerModule.FIRE, false);
        }
    }

    // ---- Ice ----

    static final class Ice implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.option("ice_path")) {
                boolean on = !c.state.isOn(PowerModule.ICE);
                c.state.set(PowerModule.ICE, on);
                c.sound(SoundEvents.GLASS_PLACE, 0.8F, on ? 1.5F : 0.8F);
                c.say(on ? "message.enhanced.custom.ice.path_on" : "message.enhanced.custom.ice.path_off");
                return;
            }
            if (!c.ready()) {
                return;
            }
            int slow = c.ticks(ModuleStats.FREEZE_TIME);
            if (c.option("blizzard")) {
                double radius = c.v(ModuleStats.BLIZZARD_RADIUS);
                for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), radius)) {
                    Shots.hurt(p, e, c.v(ModuleStats.BLIZZARD_DAMAGE));
                    freeze(e, 100, 1);
                }
                c.level().sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.5, p.getZ(), (int) (radius * 25), radius * 0.6, 1.0,
                        radius * 0.6, 0.05);
                Shots.ring(c.level(), p.position(), radius, c.color());
                c.sound(SoundEvents.POWDER_SNOW_BREAK, 1.2F, 0.6F);
                c.cooldown(ModuleStats.BLIZZARD_COOLDOWN);
                return;
            }
            Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.FREEZE_RANGE));
            Shots.line(c.level(), Shots.hand(p), aim.end(), ParticleTypes.SNOWFLAKE, 2.0);
            Shots.line(c.level(), Shots.hand(p), aim.end(), c.color(), c.glow());
            if (aim.target() != null) {
                Shots.hurt(p, aim.target(), c.v(ModuleStats.FREEZE_DAMAGE));
                freeze(aim.target(), slow, 2);
            }
            c.sound(SoundEvents.GLASS_BREAK, 0.6F, 1.8F);
            c.cooldown(ModuleStats.FREEZE_COOLDOWN);
        }

        static void freeze(LivingEntity e, int ticks, int amplifier) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier));
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + ticks));
        }

        @Override
        public void tick(Ctx c) {
            ServerPlayer p = c.player;
            if (p.getTicksFrozen() > 0) {
                p.setTicksFrozen(0);
            }
            if (!c.option("ice_path") || !c.state.isOn(PowerModule.ICE) || !p.onGround() && !p.isInWater() && p.tickCount % 2 != 0) {
                return;
            }
            int radius = Math.max(1, Math.round(c.v(ModuleStats.ICE_PATH_RADIUS)));
            ServerLevel level = c.level();
            BlockPos feet = p.blockPosition().below();
            BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
            for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-radius, 0, -radius), feet.offset(radius, 0, radius))) {
                if (pos.distToCenterSqr(p.getX(), pos.getY() + 0.5, p.getZ()) > (radius + 0.5) * (radius + 0.5)) {
                    continue;
                }
                above.set(pos.getX(), pos.getY() + 1, pos.getZ());
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof LiquidBlock && level.getFluidState(pos).is(Fluids.WATER) && level.getFluidState(pos).isSource()
                        && level.getBlockState(above).isAir() && !BossFights.protects(level, pos) && level.mayInteract(p, pos)) {
                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                    level.scheduleTick(pos.immutable(), Blocks.FROSTED_ICE, Mth.nextInt(p.getRandom(), 60, 120));
                }
            }
        }

        @Override
        public void end(Ctx c) {
            c.state.set(PowerModule.ICE, false);
        }
    }

    // ---- Lightning ----

    static final class Lightning implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.option("chain")) {
                Shots.Aim aim = Shots.aim(p, 20);
                if (aim.target() == null) {
                    c.refuse("message.enhanced.custom.no_target");
                    return;
                }
                float damage = c.v(ModuleStats.CHAIN_DAMAGE);
                Set<LivingEntity> hit = new HashSet<>();
                LivingEntity at = aim.target();
                Vec3 from = Shots.hand(p);
                int jumps = Math.round(c.v(ModuleStats.CHAIN_JUMPS));
                for (int i = 0; at != null && i <= jumps; i++) {
                    Vec3 to = at.position().add(0, at.getBbHeight() * 0.5, 0);
                    Shots.line(c.level(), from, to, c.color(), c.glow());
                    Shots.line(c.level(), from, to, ParticleTypes.ELECTRIC_SPARK, 1.5);
                    Shots.hurt(p, at, damage);
                    hit.add(at);
                    damage *= 0.75F;
                    from = to;
                    LivingEntity next = null;
                    double best = 36.0;
                    for (LivingEntity e : Shots.area(p, to, 6.0)) {
                        if (!hit.contains(e) && e.distanceToSqr(at) < best) {
                            best = e.distanceToSqr(at);
                            next = e;
                        }
                    }
                    at = next;
                }
                c.sound(ModSounds.CUSTOM_ZAP.get(), 1.0F, 0.7F);
                c.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.5F);
                c.cooldown(ModuleStats.CHAIN_COOLDOWN);
                return;
            }
            Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.STRIKE_RANGE));
            Vec3 at = aim.target() != null ? aim.target().position() : aim.end();
            visualBolt(c.level(), at);
            for (LivingEntity e : Shots.area(p, at.add(0, 1, 0), 2.5)) {
                Shots.hurt(p, e, c.v(ModuleStats.STRIKE_DAMAGE));
            }
            c.sparks(at.add(0, 0.5, 0), 20, 0.5, 1.4F);
            c.cooldown(ModuleStats.STRIKE_COOLDOWN);
        }
    }

    // ---- Water ----

    static final class Water implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            ServerPlayer p = c.player;
            if (c.option("jet")) {
                c.state.set(PowerModule.WATER, down && c.fed() && !p.isSpectator());
                if (down) {
                    c.sound(SoundEvents.BUCKET_EMPTY, 1.0F, 0.8F);
                }
                return;
            }
            if (!down || !c.ready()) {
                return;
            }
            double radius = c.v(ModuleStats.WAVE_RADIUS);
            for (LivingEntity e : Shots.area(p, p.position().add(0, 1, 0), radius)) {
                Shots.hurt(p, e, c.v(ModuleStats.WAVE_DAMAGE));
                Shots.push(e, p.position(), c.v(ModuleStats.WAVE_PUSH), 0.3);
                e.clearFire();
            }
            p.clearFire();
            c.level().sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.5, p.getZ(), (int) (radius * 30), radius * 0.6, 0.3, radius * 0.6, 0.2);
            Shots.ring(c.level(), p.position(), radius, c.color());
            c.sound(SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.2F, 0.8F);
            c.cooldown(ModuleStats.WAVE_COOLDOWN);
        }

        @Override
        public void tick(Ctx c) {
            ServerPlayer p = c.player;
            if (p.isInWater()) {
                p.setAirSupply(p.getMaxAirSupply());
                if (p.tickCount % 40 == 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, false));
                }
            }
            if (!c.option("jet") || !c.state.isOn(PowerModule.WATER)) {
                return;
            }
            if (!c.fed() || p.isSpectator()) {
                c.state.set(PowerModule.WATER, false);
                return;
            }
            Vec3 look = p.getLookAngle();
            if (p.tickCount % 2 == 0) {
                for (LivingEntity e : Shots.cone(p, c.v(ModuleStats.JET_RANGE), 15)) {
                    double push = c.v(ModuleStats.JET_PUSH) * 0.35;
                    e.setDeltaMovement(e.getDeltaMovement().add(look.x * push, 0.08, look.z * push));
                    e.hurtMarked = true;
                    e.clearFire();
                    if (e.isSensitiveToWater() && p.tickCount % 10 == 0) {
                        Shots.hurt(p, e, 2.0F, ModDamageTypes.HERO_BEAM);
                    }
                }
                c.exhaust(0.05F);
            }
            Vec3 from = Shots.hand(p);
            for (int i = 0; i < 4; i++) {
                Vec3 v = look.add(p.getRandom().nextGaussian() * 0.05, p.getRandom().nextGaussian() * 0.05, p.getRandom().nextGaussian() * 0.05);
                c.level().sendParticles(ParticleTypes.SPLASH, from.x + v.x * i, from.y + v.y * i, from.z + v.z * i, 1, 0.05, 0.05, 0.05, 0.0);
            }
            c.level().sendParticles(new EnergyParticleOptions(c.color(), 1.0F), from.x, from.y, from.z, 0, look.x, look.y, look.z, 0.8);
            if (p.tickCount % 10 == 0) {
                c.sound(SoundEvents.WATER_AMBIENT, 0.6F, 1.5F);
            }
        }

        @Override
        public void end(Ctx c) {
            c.state.set(PowerModule.WATER, false);
        }
    }

    // ---- Earth: no block is changed; the ground shakes and spikes of it throw what stands there ----

    static final class Earth implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            if (c.option("spikes")) {
                Vec3 dir = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z);
                if (dir.lengthSqr() < 1.0E-4) {
                    dir = new Vec3(0, 0, 1);
                }
                dir = dir.normalize();
                Set<LivingEntity> hit = new HashSet<>();
                double length = c.v(ModuleStats.SPIKES_LENGTH);
                for (double d = 1; d <= length; d += 1) {
                    Vec3 at = p.position().add(dir.scale(d));
                    if (Shots.hexCut(level, p.position().add(0, 0.5, 0), at.add(0, 0.5, 0)) != null) {
                        break;
                    }
                    BlockState ground = level.getBlockState(BlockPos.containing(at).below());
                    if (!ground.isAir()) {
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.3, at.z, 8, 0.2, 0.4, 0.2, 0.2);
                        level.sendParticles(new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground), at.x, at.y, at.z, 4, 0.2, 0.0, 0.2, 0.3);
                    }
                    for (LivingEntity e : Shots.area(p, at.add(0, 0.5, 0), 1.3)) {
                        if (hit.add(e)) {
                            Shots.hurt(p, e, c.v(ModuleStats.SPIKES_DAMAGE));
                            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.9, 0));
                            e.hurtMarked = true;
                        }
                    }
                }
                c.sound(ModSounds.CUSTOM_IMPACT.get(), 1.2F, 0.6F);
                c.cooldown(ModuleStats.SPIKES_COOLDOWN);
                return;
            }
            double radius = c.v(ModuleStats.TREMOR_RADIUS);
            for (LivingEntity e : Shots.area(p, p.position(), radius)) {
                if (e.onGround() || e.getY() - p.getY() < 1.0) {
                    Shots.hurt(p, e, c.v(ModuleStats.TREMOR_DAMAGE));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.4, 0));
                    e.hurtMarked = true;
                }
            }
            BlockState ground = level.getBlockState(p.blockPosition().below());
            if (!ground.isAir()) {
                for (int i = 0; i < 40; i++) {
                    double a = p.getRandom().nextDouble() * Math.PI * 2;
                    double r = p.getRandom().nextDouble() * radius;
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), p.getX() + Math.cos(a) * r, p.getY() + 0.1,
                            p.getZ() + Math.sin(a) * r, 2, 0.1, 0.1, 0.1, 0.1);
                }
            }
            Shots.ring(level, p.position(), radius, c.color());
            c.sound(SoundEvents.GENERIC_EXPLODE, 0.5F, 0.5F);
            c.sound(ModSounds.CUSTOM_IMPACT.get(), 1.0F, 0.5F);
            c.cooldown(ModuleStats.TREMOR_COOLDOWN);
        }
    }

    // ---- Wind: a gust that throws what is in front of him, or an updraft that lifts him; he falls softly ----

    static final class Wind implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            if (c.option("updraft")) {
                double lift = c.v(ModuleStats.UPDRAFT_LIFT);
                p.setDeltaMovement(p.getDeltaMovement().x, lift, p.getDeltaMovement().z);
                p.hurtMarked = true;
                CustomHeroes.guardFall(p, 100);
                for (LivingEntity e : Shots.area(p, p.position(), 3.0)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(0, lift * 0.8, 0));
                    e.hurtMarked = true;
                }
                level.sendParticles(ParticleTypes.GUST, p.getX(), p.getY() + 0.2, p.getZ(), 6, 0.6, 0.2, 0.6, 0.0);
                c.sparks(p.position(), 16, 0.6, 1.0F);
                c.sound(SoundEvents.WIND_CHARGE_BURST.value(), 1.0F, 0.9F);
                c.cooldown(ModuleStats.UPDRAFT_COOLDOWN);
                return;
            }
            double range = c.v(ModuleStats.GUST_RANGE);
            Vec3 look = p.getLookAngle();
            double push = c.v(ModuleStats.GUST_PUSH);
            for (LivingEntity e : Shots.cone(p, range, 30)) {
                Shots.hurt(p, e, c.v(ModuleStats.GUST_DAMAGE));
                e.setDeltaMovement(e.getDeltaMovement().add(look.x * push, 0.3 + Math.max(0, look.y) * push, look.z * push));
                e.hurtMarked = true;
            }
            // Loose things go with it too (items, arrows in flight), but never through a Hex wall.
            AABB box = p.getBoundingBox().inflate(range);
            for (Entity e : level.getEntities(p, box, e -> !(e instanceof LivingEntity) && e.isAlive() && !e.isSpectator()
                    && (e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof net.minecraft.world.entity.projectile.Projectile))) {
                Vec3 to = e.position().subtract(p.getEyePosition());
                if (to.length() <= range && to.normalize().dot(look) > Math.cos(Math.toRadians(30)) && Shots.reaches(p, e)) {
                    e.setDeltaMovement(e.getDeltaMovement().add(look.scale(push * 0.6)));
                    e.hurtMarked = true;
                }
            }
            for (double d = 1; d <= range; d += 1.5) {
                Vec3 at = p.getEyePosition().add(look.scale(d));
                level.sendParticles(ParticleTypes.GUST, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
            c.sound(SoundEvents.WIND_CHARGE_BURST.value(), 1.0F, 1.2F);
            c.cooldown(ModuleStats.GUST_COOLDOWN);
        }
    }

    // ---- Weather Control: "storm" strikes the monsters round him for a while; "cycle" turns the world's weather ----

    static final class Weather implements ModuleLogic {
        private static final Map<UUID, Long> STORMS = new HashMap<>();

        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerLevel level = c.level();
            if (c.option("cycle")) {
                boolean allowed = EnhancedConfig.SPEC.isLoaded() ? CustomHeroConfig.WEATHER_CHANGES_WORLD.get() : true;
                if (!allowed || !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) {
                    c.refuse("message.enhanced.custom.weather.cannot");
                    return;
                }
                String next;
                if (level.isThundering()) {
                    level.setWeatherParameters(12000, 0, false, false);
                    next = "message.enhanced.custom.weather.clear";
                } else if (level.isRaining()) {
                    level.setWeatherParameters(0, 6000, true, true);
                    next = "message.enhanced.custom.weather.thunder";
                } else {
                    level.setWeatherParameters(0, 6000, true, false);
                    next = "message.enhanced.custom.weather.rain";
                }
                c.say(next);
                c.sound(SoundEvents.TRIDENT_THUNDER.value(), 0.6F, 1.2F);
                c.cooldown(ModuleStats.CYCLE_COOLDOWN);
                return;
            }
            STORMS.put(c.player.getUUID(), c.now() + c.ticks(ModuleStats.STORM_TIME));
            c.say("message.enhanced.custom.weather.storm");
            c.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F, 1.4F);
            c.cooldown(ModuleStats.STORM_COOLDOWN);
        }

        @Override
        public void tick(Ctx c) {
            Long until = STORMS.get(c.player.getUUID());
            if (until == null) {
                return;
            }
            if (c.now() > until) {
                STORMS.remove(c.player.getUUID());
                return;
            }
            ServerPlayer p = c.player;
            ServerLevel level = c.level();
            if (p.tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 6, p.getZ(), 6, 3, 0.3, 3, 0.0);
                level.sendParticles(ParticleTypes.FALLING_WATER, p.getX(), p.getY() + 5, p.getZ(), 10, 3, 0.2, 3, 0.0);
            }
            if (p.tickCount % 20 != 0) {
                return;
            }
            List<LivingEntity> monsters = new ArrayList<>();
            for (LivingEntity e : Shots.area(p, p.position(), c.v(ModuleStats.STORM_RADIUS))) {
                if (e instanceof Enemy) {
                    monsters.add(e);
                }
            }
            if (monsters.isEmpty()) {
                return;
            }
            LivingEntity struck = monsters.get(p.getRandom().nextInt(monsters.size()));
            visualBolt(level, struck.position());
            Shots.hurt(p, struck, c.v(ModuleStats.STORM_DAMAGE));
            level.playSound(null, struck.getX(), struck.getY(), struck.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 0.8F, 1.0F);
        }

        @Override
        public void end(Ctx c) {
            STORMS.remove(c.player.getUUID());
        }

        @Override
        public void forget(ServerPlayer player) {
            STORMS.remove(player.getUUID());
        }

        @Override
        public void clear() {
            STORMS.clear();
        }
    }

    private ElementPowers() {
    }
}
