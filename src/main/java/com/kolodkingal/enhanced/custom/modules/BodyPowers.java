package com.kolodkingal.enhanced.custom.modules;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.custom.ModuleLogic;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.LokiForm;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The custom powers of the body (3.3): Super Strength, Durability, the Healing Factor, Shapeshifting, Invisibility and
 * Phasing. Their numbers are {@link ModuleStats}' at the power's strength.
 */
public final class BodyPowers {
    private static final ResourceLocation STRENGTH = Enhanced.id("custom_strength");
    private static final ResourceLocation ARMOR = Enhanced.id("custom_armor");
    private static final ResourceLocation TOUGHNESS = Enhanced.id("custom_toughness");
    private static final ResourceLocation KNOCKBACK = Enhanced.id("custom_knockback");
    private static final ResourceLocation HEALTH = Enhanced.id("custom_health");
    private static final ResourceLocation SCALE = Enhanced.id("custom_scale");
    private static final ResourceLocation SCALE_DAMAGE = Enhanced.id("custom_scale_damage");
    private static final ResourceLocation SCALE_REACH = Enhanced.id("custom_scale_reach");
    private static final ResourceLocation SCALE_SPEED = Enhanced.id("custom_scale_speed");

    public static void register(Map<PowerModule, ModuleLogic> logic) {
        logic.put(PowerModule.SUPER_STRENGTH, new Strength());
        logic.put(PowerModule.DURABILITY, new Durability());
        logic.put(PowerModule.HEALING, new Healing());
        logic.put(PowerModule.SHAPESHIFT, new Shapeshift());
        logic.put(PowerModule.INVISIBILITY, new Invisibility());
        logic.put(PowerModule.PHASING, new Phasing());
    }

    // ---- Super Strength: harder blows always; the key a Mighty Punch, a Ground Slam or a Super Leap ----

    static final class Strength implements ModuleLogic {
        @Override
        public void body(ServerPlayer player, @Nullable CustomHero.Pick pick) {
            ModuleLogic.modifier(player, Attributes.ATTACK_DAMAGE, STRENGTH,
                    pick == null ? 0 : ModuleStats.STRENGTH_MELEE.value(pick.strength()), pick != null);
        }

        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.option("slam")) {
                double radius = c.v(ModuleStats.SLAM_RADIUS);
                for (LivingEntity e : Shots.area(p, p.position(), radius)) {
                    Shots.hurt(p, e, c.v(ModuleStats.SLAM_DAMAGE));
                    Shots.push(e, p.position(), 0.8, 0.5);
                }
                BlockState ground = c.level().getBlockState(p.blockPosition().below());
                if (!ground.isAir()) {
                    c.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), p.getX(), p.getY() + 0.1, p.getZ(),
                            (int) (radius * 12), radius * 0.5, 0.1, radius * 0.5, 0.15);
                }
                Shots.ring(c.level(), p.position(), radius, c.color());
                c.sound(SoundEvents.GENERIC_EXPLODE, 0.6F, 1.5F);
                c.sound(ModSounds.CUSTOM_IMPACT.get(), 1.0F, 0.8F);
                c.cooldown(ModuleStats.SLAM_COOLDOWN);
            } else if (c.option("leap")) {
                double power = c.v(ModuleStats.LEAP_POWER);
                Vec3 look = p.getLookAngle();
                p.setDeltaMovement(look.x * power, Math.max(0.5, look.y * power + 0.45 * power), look.z * power);
                p.hurtMarked = true;
                CustomHeroes.guardFall(p, 100);
                c.sparks(p.position(), 16, 0.4, 1.2F);
                c.sound(SoundEvents.WIND_CHARGE_BURST.value(), 0.8F, 0.8F);
                c.cooldown(ModuleStats.LEAP_COOLDOWN);
            } else {
                Shots.Aim aim = Shots.aim(p, 4.5);
                if (aim.target() == null) {
                    c.refuse("message.enhanced.custom.no_target");
                    return;
                }
                LivingEntity target = aim.target();
                Shots.hurt(p, target, c.v(ModuleStats.PUNCH_DAMAGE));
                Shots.push(target, p.position(), c.v(ModuleStats.PUNCH_KNOCKBACK), 0.35);
                c.sparks(aim.end(), 14, 0.2, 1.4F);
                c.level().sendParticles(ParticleTypes.EXPLOSION, aim.end().x, aim.end().y, aim.end().z, 1, 0, 0, 0, 0);
                c.sound(ModSounds.CUSTOM_IMPACT.get(), 1.0F, 1.1F);
                p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                c.cooldown(ModuleStats.PUNCH_COOLDOWN);
            }
        }
    }

    // ---- Durability: armour and toughness, or more health; harder to knock back ----

    static final class Durability implements ModuleLogic {
        @Override
        public void body(ServerPlayer player, @Nullable CustomHero.Pick pick) {
            boolean armor = pick != null && !"health".equals(pick.option());
            boolean health = pick != null && "health".equals(pick.option());
            int s = pick == null ? 1 : pick.strength();
            ModuleLogic.modifier(player, Attributes.ARMOR, ARMOR, ModuleStats.ARMOR.value(s), armor);
            ModuleLogic.modifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS, ModuleStats.TOUGHNESS.value(s), armor);
            ModuleLogic.modifier(player, Attributes.MAX_HEALTH, HEALTH, ModuleStats.EXTRA_HEALTH.value(s), health);
            ModuleLogic.modifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK,
                    (health ? ModuleStats.HEALTH_KNOCKBACK_RESIST : ModuleStats.KNOCKBACK_RESIST).value(s), pick != null);
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    // ---- the Healing Factor: health comes back by itself; "burst" heals at once on its key ----

    static final class Healing implements ModuleLogic {
        @Override
        public void tick(Ctx c) {
            ServerPlayer p = c.player;
            if (p.tickCount % 20 != 0 || p.getHealth() >= p.getMaxHealth() || p.isDeadOrDying()) {
                return;
            }
            p.heal(c.v(c.option("burst") ? ModuleStats.BURST_REGEN : ModuleStats.REGEN));
            if (p.tickCount % 60 == 0) {
                c.level().sendParticles(new EnergyParticleOptions(c.color(), 0.8F), p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.3, 0.5, 0.3, 0.01);
            }
        }

        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.option("burst") || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            p.heal(c.v(ModuleStats.BURST_HEAL));
            p.removeEffect(MobEffects.POISON);
            p.removeEffect(MobEffects.WITHER);
            c.level().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.6, p.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
            c.sparks(p.position().add(0, 1, 0), 24, 0.5, 1.2F);
            c.sound(ModSounds.CUSTOM_HEAL.get(), 1.0F, 1.0F);
            c.cooldown(ModuleStats.BURST_COOLDOWN);
        }
    }

    // ---- Shapeshifting: grow or shrink (a switch), or take the shape of a creature (Loki's way of being drawn) ----

    static final class Shapeshift implements ModuleLogic {
        @Override
        public void body(ServerPlayer player, @Nullable CustomHero.Pick pick) {
            CustomState state = CustomHeroes.state(player);
            boolean on = pick != null && state.isOn(PowerModule.SHAPESHIFT) && !"creature".equals(pick.option());
            boolean grow = on && "grow".equals(pick.option());
            double scale = !on ? 1.0 : (grow ? ModuleStats.GROW_SCALE : ModuleStats.SHRINK_SCALE).value(pick.strength());
            ModuleLogic.modifier(player, Attributes.SCALE, SCALE, scale - 1.0, on, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            ModuleLogic.modifier(player, Attributes.ATTACK_DAMAGE, SCALE_DAMAGE, (scale - 1.0) * 3.0, grow);
            ModuleLogic.modifier(player, Attributes.ENTITY_INTERACTION_RANGE, SCALE_REACH, scale - 1.0, grow);
            ModuleLogic.modifier(player, Attributes.MOVEMENT_SPEED, SCALE_SPEED, 0.2, on && !grow, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override
        public void key(Ctx c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.option("creature")) {
                if (!c.data.form().isEmpty()) {
                    c.data.setForm("");
                    c.say("message.enhanced.custom.shapeshift.self");
                    poof(c);
                    return;
                }
                if (!c.ready()) {
                    return;
                }
                Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.CREATURE_RANGE));
                LivingEntity target = aim.target();
                if (target == null || target instanceof Player || target.getType().is(ModTags.BOSSES) || !(target instanceof Mob)) {
                    c.refuse("message.enhanced.custom.shapeshift.no_creature");
                    return;
                }
                c.data.setForm(LokiForm.creature(target.getType()).serialize());
                c.say("message.enhanced.custom.shapeshift.into", target.getType().getDescription());
                poof(c);
                c.cooldown(ModuleStats.CREATURE_COOLDOWN);
                return;
            }
            boolean on = !c.state.isOn(PowerModule.SHAPESHIFT);
            if (on && c.option("grow")) {
                double scale = c.v(ModuleStats.GROW_SCALE);
                AABB box = p.getBoundingBox();
                AABB grown = new AABB(box.minX - box.getXsize() * (scale - 1) / 2, box.minY, box.minZ - box.getZsize() * (scale - 1) / 2,
                        box.maxX + box.getXsize() * (scale - 1) / 2, box.minY + box.getYsize() * scale, box.maxZ + box.getZsize() * (scale - 1) / 2);
                if (!c.level().noCollision(p, grown)) {
                    c.refuse("message.enhanced.custom.shapeshift.no_room");
                    return;
                }
            }
            c.state.set(PowerModule.SHAPESHIFT, on);
            body(p, c.pick);
            poof(c);
            c.say(on ? (c.option("grow") ? "message.enhanced.custom.shapeshift.grow" : "message.enhanced.custom.shapeshift.shrink")
                    : "message.enhanced.custom.shapeshift.self");
        }

        @Override
        public void end(Ctx c) {
            c.state.set(PowerModule.SHAPESHIFT, false);
            if (!c.data.form().isEmpty()) {
                c.data.setForm("");
            }
            body(c.player, null);
        }

        private static void poof(Ctx c) {
            ServerPlayer p = c.player;
            c.level().sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
            c.sparks(p.position().add(0, 1, 0), 20, 0.5, 1.2F);
            c.sound(ModSounds.CUSTOM_SHIFT.get(), 1.0F, 1.0F);
        }
    }

    // ---- Invisibility: a switch; "full" also hides him from monsters, "shimmer" leaves a faint outline of light ----

    static final class Invisibility implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down) {
                return;
            }
            boolean on = !c.state.isOn(PowerModule.INVISIBILITY);
            if (on && !c.fed()) {
                c.refuse("message.enhanced.custom.hungry");
                return;
            }
            set(c, on);
        }

        private static void set(Ctx c, boolean on) {
            ServerPlayer p = c.player;
            c.state.set(PowerModule.INVISIBILITY, on);
            if (on) {
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, true, false, false));
                if (c.option("full")) {
                    loseMonsters(p);
                }
            } else {
                MobEffectInstance effect = p.getEffect(MobEffects.INVISIBILITY);
                if (effect != null && effect.isAmbient()) {
                    p.removeEffect(MobEffects.INVISIBILITY);
                }
            }
            c.sparks(p.position().add(0, 1, 0), 24, 0.4, 1.0F);
            c.sound(ModSounds.CUSTOM_SHIFT.get(), 0.8F, on ? 1.4F : 0.9F);
            c.say(on ? "message.enhanced.custom.invisible.on" : "message.enhanced.custom.invisible.off");
        }

        @Override
        public void tick(Ctx c) {
            if (!c.state.isOn(PowerModule.INVISIBILITY)) {
                return;
            }
            ServerPlayer p = c.player;
            if (p.tickCount % 20 == 0) {
                c.exhaust(c.v(ModuleStats.INVISIBLE_HUNGER));
                if (!c.fed()) {
                    set(c, false);
                    return;
                }
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, true, false, false));
                if (c.option("full")) {
                    loseMonsters(p);
                }
            }
            if (c.option("shimmer") && p.tickCount % 6 == 0) {
                c.level().sendParticles(new EnergyParticleOptions(c.color(), 0.6F), p.getX(), p.getY() + 1.0, p.getZ(), 2, 0.25, 0.6, 0.25, 0.0);
            }
        }

        /** Monsters that were after him lose him (unless he hit them himself in the last three seconds). */
        private static void loseMonsters(ServerPlayer p) {
            for (Mob mob : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p)) {
                if (mob.getLastHurtByMob() != p || mob.tickCount - mob.getLastHurtByMobTimestamp() > 60) {
                    mob.setTarget(null);
                }
            }
        }

        @Override
        public void end(Ctx c) {
            if (c.state.isOn(PowerModule.INVISIBILITY)) {
                set(c, false);
            }
        }
    }

    // ---- Phasing: a switch; "phase" passes through walls (Vision's phasing), "ghost" lets every blow pass through him ----

    static final class Phasing implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            boolean on = !c.state.isOn(PowerModule.PHASING);
            if (on && !c.fed()) {
                c.refuse("message.enhanced.custom.hungry");
                return;
            }
            if (!on && c.option("phase")) {
                Double lift = freeSpaceAbove(p);
                if (lift == null) {
                    c.refuse("message.enhanced.phase.inside_wall");
                    return;
                }
                if (lift > 0) {
                    p.teleportTo(p.getX(), p.getY() + lift, p.getZ());
                }
            }
            c.state.set(PowerModule.PHASING, on);
            if (on && c.option("phase")) {
                PowerTicker.applyPassives(p, c.data);
                if (p.mayFly() && !p.getAbilities().flying) {
                    p.getAbilities().flying = true;
                    p.onUpdateAbilities();
                }
            }
            c.sound(ModSounds.VISION_PHASE.get(), 1.0F, on ? 1.15F : 0.85F);
            c.say(on ? "message.enhanced.custom.phase.on" : "message.enhanced.custom.phase.off");
        }

        @Override
        public void tick(Ctx c) {
            if (!c.state.isOn(PowerModule.PHASING)) {
                return;
            }
            ServerPlayer p = c.player;
            if (p.tickCount % 20 == 0) {
                c.exhaust(c.v(ModuleStats.PHASE_HUNGER));
            }
            if (p.tickCount % 4 == 0) {
                c.level().sendParticles(new EnergyParticleOptions(c.color(), 0.9F), p.getX(), p.getY() + 1.0, p.getZ(), 1, 0.3, 0.7, 0.3, 0.0);
            }
            if (!c.option("phase")) {
                return;
            }
            Abilities abilities = p.getAbilities();
            if (p.mayFly() && !abilities.flying) {
                abilities.flying = true;
                p.onUpdateAbilities();
            }
            p.resetFallDistance();
            p.clearFire();
            WorldBorder border = p.level().getWorldBorder();
            if (!border.isWithinBounds(p.getBoundingBox())) {
                double half = p.getBbWidth() / 2 + 1.0E-3;
                p.teleportTo(net.minecraft.util.Mth.clamp(p.getX(), border.getMinX() + half, border.getMaxX() - half), p.getY(),
                        net.minecraft.util.Mth.clamp(p.getZ(), border.getMinZ() + half, border.getMaxZ() - half));
            }
            if (!c.fed()) {
                // Starving, he must come out: where there is room, at once; inside a wall, as soon as there is.
                Double lift = freeSpaceAbove(p);
                if (lift != null) {
                    if (lift > 0) {
                        p.teleportTo(p.getX(), p.getY() + lift, p.getZ());
                    }
                    c.state.set(PowerModule.PHASING, false);
                    c.say("message.enhanced.custom.phase.off");
                }
            }
        }

        @Override
        public void end(Ctx c) {
            if (!c.state.isOn(PowerModule.PHASING)) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.option("phase")) {
                // Never left inside a wall: up to the first room above, as a Vision who stops phasing.
                for (int dy = 0; dy < 64; dy++) {
                    BlockPos at = p.blockPosition().above(dy);
                    if (p.level().noCollision(p, p.getBoundingBox().move(0, at.getY() - p.getY(), 0))) {
                        if (dy > 0) {
                            p.teleportTo(p.getX(), at.getY(), p.getZ());
                        }
                        break;
                    }
                }
            }
            c.state.set(PowerModule.PHASING, false);
        }

        /** How far up (0 to 1 block) he must rise to stand free of blocks, or null if he is deeper in a wall. */
        @Nullable
        private static Double freeSpaceAbove(ServerPlayer player) {
            for (int step = 0; step <= 10; step++) {
                double dy = step * 0.1;
                if (player.level().noCollision(player, player.getBoundingBox().move(0, dy, 0))) {
                    return dy;
                }
            }
            return null;
        }
    }

    private BodyPowers() {
    }
}
