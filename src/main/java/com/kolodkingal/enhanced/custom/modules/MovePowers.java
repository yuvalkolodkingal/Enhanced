package com.kolodkingal.enhanced.custom.modules;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.ModuleLogic;
import com.kolodkingal.enhanced.custom.ModuleStats;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.mixin.ServerGamePacketListenerImplAccessor;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The custom powers of moving (3.3): Super Speed, Flight, Teleportation and Wall-Crawling. Flight is the mod's own (the
 * creative flight every flying hero has, at the speed of its strength); the climbing of Wall-Crawling is done by the
 * hero's own client, as every movement of a player is ({@code CustomMotion}).
 */
public final class MovePowers {
    private static final ResourceLocation SPEED = Enhanced.id("custom_speed");
    private static final ResourceLocation STEP = Enhanced.id("custom_speed_step");

    public static void register(Map<PowerModule, ModuleLogic> logic) {
        logic.put(PowerModule.SUPER_SPEED, new Speed());
        logic.put(PowerModule.FLIGHT, new Flight());
        logic.put(PowerModule.TELEPORT, new Teleport());
        logic.put(PowerModule.WALL_CRAWL, new WallCrawl());
    }

    // ---- Super Speed: "run" is a switch (and the run of Quicksilver and Tommy: the trail, the stride); "blitz" a dash ----

    static final class Speed implements ModuleLogic {
        @Override
        public void body(ServerPlayer player, @Nullable CustomHero.Pick pick) {
            boolean on = pick != null && CustomHeroes.state(player).isOn(PowerModule.SUPER_SPEED);
            ModuleLogic.modifier(player, Attributes.MOVEMENT_SPEED, SPEED, on ? ModuleStats.RUN_SPEED.value(pick.strength()) : 0, on,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            ModuleLogic.modifier(player, Attributes.STEP_HEIGHT, STEP, 0.5, on);
        }

        @Override
        public void key(Ctx c, boolean down) {
            if (!down) {
                return;
            }
            ServerPlayer p = c.player;
            if (c.option("blitz")) {
                if (c.ready()) {
                    blitz(c);
                }
                return;
            }
            boolean on = !c.state.isOn(PowerModule.SUPER_SPEED);
            if (on && !c.fed()) {
                c.refuse("message.enhanced.custom.hungry");
                return;
            }
            c.state.set(PowerModule.SUPER_SPEED, on);
            if (!on) {
                c.data.set(Live.SPEEDING, false);
            }
            body(p, c.pick);
            c.sound(ModSounds.CUSTOM_WHOOSH.get(), 0.8F, on ? 1.3F : 0.8F);
            c.say(on ? "message.enhanced.custom.speed.on" : "message.enhanced.custom.speed.off");
        }

        @Override
        public void tick(Ctx c) {
            if (!c.option("run") || !c.state.isOn(PowerModule.SUPER_SPEED)) {
                return;
            }
            ServerPlayer p = c.player;
            Vec3 moved = new Vec3(p.getX() - p.xo, 0, p.getZ() - p.zo);
            boolean running = p.isSprinting() && !p.getAbilities().flying && moved.lengthSqr() > 0.01;
            c.data.set(Live.SPEEDING, running);
            if (running && p.tickCount % 20 == 0) {
                c.exhaust(c.v(ModuleStats.RUN_HUNGER));
                if (!c.fed()) {
                    c.state.set(PowerModule.SUPER_SPEED, false);
                    c.data.set(Live.SPEEDING, false);
                    body(p, c.pick);
                    c.refuse("message.enhanced.custom.hungry");
                }
            }
        }

        /** A dash along the ground where he looks: every creature on the way is struck; walls and Hex walls stop it. */
        private static void blitz(Ctx c) {
            ServerPlayer p = c.player;
            Vec3 dir = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z);
            if (dir.lengthSqr() < 1.0E-4) {
                c.refuse("message.enhanced.custom.no_room");
                return;
            }
            dir = dir.normalize();
            double distance = c.v(ModuleStats.BLITZ_DISTANCE);
            Vec3 start = p.position();
            Vec3 end = start;
            for (double d = 0.5; d <= distance; d += 0.5) {
                Vec3 at = start.add(dir.scale(d));
                AABB box = p.getBoundingBox().move(at.subtract(start));
                if (!p.level().noCollision(p, box) || Shots.hexCut(c.level(), start.add(0, 1, 0), at.add(0, 1, 0)) != null) {
                    Vec3 up = at.add(0, 1.0, 0);
                    if (p.level().noCollision(p, p.getBoundingBox().move(up.subtract(start))) && Shots.hexCut(c.level(), start.add(0, 1, 0), up.add(0, 1, 0)) == null) {
                        at = up; // a step up, as a runner takes it
                        start = start.add(0, 1.0, 0);
                    } else {
                        break;
                    }
                }
                end = at;
            }
            if (end.distanceToSqr(p.position()) < 1.0 || !HexManager.onTeleport(c.level(), p, end)) {
                c.refuse("message.enhanced.custom.no_room");
                return;
            }
            Set<LivingEntity> struck = new HashSet<>();
            Vec3 from = p.position();
            for (LivingEntity e : Shots.area(p, from.lerp(end, 0.5), from.distanceTo(end) / 2 + 1.5)) {
                Vec3 mid = e.position();
                Vec3 seg = end.subtract(from);
                double t = Math.max(0, Math.min(1, mid.subtract(from).dot(seg) / Math.max(1.0E-4, seg.lengthSqr())));
                if (mid.distanceTo(from.add(seg.scale(t))) <= 1.6 && struck.add(e)) {
                    Shots.hurt(p, e, c.v(ModuleStats.BLITZ_DAMAGE));
                    Shots.push(e, from, 0.6, 0.3);
                }
            }
            Shots.line(c.level(), from.add(0, 1, 0), end.add(0, 1, 0), c.color(), c.glow());
            p.teleportTo(end.x, end.y, end.z);
            CustomHeroes.guardFall(p, 40);
            c.sound(ModSounds.CUSTOM_WHOOSH.get(), 1.0F, 1.6F);
            c.cooldown(ModuleStats.BLITZ_COOLDOWN);
        }

        @Override
        public void end(Ctx c) {
            c.state.set(PowerModule.SUPER_SPEED, false);
            c.data.set(Live.SPEEDING, false);
            body(c.player, null);
        }
    }

    // ---- Flight: take off and land (the flight is the creative flight of every flying hero); a trail by its style ----

    static final class Flight implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (down) {
                PowerTicker.toggleFlight(c.player);
            }
        }

        @Override
        public void tick(Ctx c) {
            ServerPlayer p = c.player;
            if (!p.getAbilities().flying || p.isSpectator() || p.isInvisible() || p.tickCount % 2 != 0) {
                return;
            }
            Vec3 at = p.position();
            if (c.option("jet")) {
                c.level().sendParticles(new EnergyParticleOptions(c.color(), 1.3F), at.x, at.y + 0.1, at.z, 2, 0.08, 0.05, 0.08, 0.02);
                c.level().sendParticles(new EnergyParticleOptions(c.glow(), 0.8F), at.x, at.y + 0.1, at.z, 1, 0.03, 0.02, 0.03, 0.0);
                if (p.tickCount % 6 == 0) {
                    c.level().sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.05, 0.0, 0.05, 0.01);
                }
            } else if (c.option("wings")) {
                float yaw = p.yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
                Vec3 back = new Vec3(net.minecraft.util.Mth.sin(yaw), 0, -net.minecraft.util.Mth.cos(yaw)).scale(0.35);
                Vec3 side = new Vec3(net.minecraft.util.Mth.cos(yaw), 0, net.minecraft.util.Mth.sin(yaw));
                double flap = Math.sin(p.tickCount * 0.5) * 0.25;
                for (int side2 = -1; side2 <= 1; side2 += 2) {
                    for (int i = 1; i <= 3; i++) {
                        Vec3 tip = at.add(0, 1.3 + flap * i / 3.0, 0).add(back).add(side.scale(side2 * 0.35 * i));
                        c.level().sendParticles(new EnergyParticleOptions(i == 3 ? c.glow() : c.color(), 0.8F), tip.x, tip.y, tip.z, 1, 0, 0, 0, 0);
                    }
                }
            } else if (p.tickCount % 4 == 0) {
                Shots.ring(c.level(), at.add(0, -0.1, 0), 0.5, c.color());
            }
        }
    }

    // ---- Teleportation: blink to where he looks, or behind the creature he looks at ----

    static final class Teleport implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down || !c.ready()) {
                return;
            }
            ServerPlayer p = c.player;
            Vec3 spot;
            float yaw = p.getYRot();
            if (c.option("behind")) {
                Shots.Aim aim = Shots.aim(p, c.v(ModuleStats.BEHIND_RANGE));
                if (aim.target() == null) {
                    c.refuse("message.enhanced.custom.no_target");
                    return;
                }
                spot = Teleports.behind(p, aim.target());
                yaw = aim.target().yBodyRot;
            } else {
                double range = c.v(ModuleStats.BLINK_RANGE);
                BlockHitResult hit = Raycasts.block(p, range, 1.0F);
                if (hit.getType() == HitResult.Type.MISS) {
                    Vec3 air = p.getEyePosition().add(p.getLookAngle().scale(range)).subtract(0, p.getEyeHeight(), 0);
                    spot = Teleports.fits(p, air) ? air : null;
                } else {
                    spot = Teleports.onto(p, hit);
                }
            }
            if (spot == null || !Teleports.fits(p, spot) || Shots.hexCut(c.level(), p.position().add(0, 1, 0), spot.add(0, 1, 0)) != null) {
                c.refuse("message.enhanced.custom.no_room");
                return;
            }
            if (!HexManager.onTeleport(c.level(), p, spot)) {
                return;
            }
            Vec3 from = p.position();
            c.level().sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 30, 0.3, 0.6, 0.3, 0.3);
            c.sparks(from.add(0, 1, 0), 20, 0.4, 1.3F);
            p.teleportTo(c.level(), spot.x, spot.y, spot.z, yaw, p.getXRot());
            p.resetFallDistance();
            CustomHeroes.guardFall(p, 40);
            c.sparks(spot.add(0, 1, 0), 20, 0.4, 1.3F);
            c.level().playSound(null, from.x, from.y, from.z, ModSounds.CUSTOM_TELEPORT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
            c.sound(ModSounds.CUSTOM_TELEPORT.get(), 1.0F, 1.2F);
            c.cooldown(c.option("behind") ? ModuleStats.BEHIND_COOLDOWN : ModuleStats.BLINK_COOLDOWN);
        }
    }

    // ---- Wall-Crawling: a switch (on from the start); his client climbs, the server forgives the fall and the float ----

    static final class WallCrawl implements ModuleLogic {
        @Override
        public void key(Ctx c, boolean down) {
            if (!down) {
                return;
            }
            boolean on = !c.state.isOn(PowerModule.WALL_CRAWL);
            c.state.set(PowerModule.WALL_CRAWL, on);
            c.sound(net.minecraft.sounds.SoundEvents.SPIDER_STEP, 0.8F, on ? 1.3F : 0.8F);
            c.say(on ? "message.enhanced.custom.crawl.on" : "message.enhanced.custom.crawl.off");
        }

        @Override
        public void tick(Ctx c) {
            ServerPlayer p = c.player;
            boolean on = c.state.isOn(PowerModule.WALL_CRAWL);
            boolean wall = on && touchesWall(p);
            if (wall) {
                p.resetFallDistance();
                ((ServerGamePacketListenerImplAccessor) p.connection).enhanced$setAboveGroundTickCount(0);
            }
            // (3.4) The flags everyone's client reads for Spider-Man's cling stances: on a wall off the ground, or under a
            // ceiling with no wall beside him.
            boolean aloft = !p.onGround() && !p.isInWaterOrBubble();
            c.data.set(Live.CLINGING_WALL, wall && aloft);
            c.data.set(Live.CLINGING_CEILING, on && !wall && aloft && touchesCeiling(p));
        }

        @Override
        public void end(Ctx c) {
            c.data.set(Live.CLINGING_WALL, false);
            c.data.set(Live.CLINGING_CEILING, false);
        }
    }

    /** Whether a solid block is right above his head. */
    public static boolean touchesCeiling(ServerPlayer p) {
        AABB box = p.getBoundingBox().move(0, 0.12, 0);
        return !p.level().noCollision(p, box);
    }

    /** Whether a solid block is right beside him (at his feet or his head). */
    public static boolean touchesWall(ServerPlayer p) {
        AABB box = p.getBoundingBox().inflate(0.12, 0, 0.12);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (double y : new double[]{0.1, p.getBbHeight() - 0.1}) {
                BlockPos pos = BlockPos.containing(p.getX() + dir.getStepX() * (p.getBbWidth() / 2 + 0.1), p.getY() + y,
                        p.getZ() + dir.getStepZ() * (p.getBbWidth() / 2 + 0.1));
                if (!p.level().getBlockState(pos).getCollisionShape(p.level(), pos).isEmpty()
                        && p.level().getBlockState(pos).getCollisionShape(p.level(), pos).bounds().move(pos).intersects(box)) {
                    return true;
                }
            }
        }
        return false;
    }

    private MovePowers() {
    }
}
