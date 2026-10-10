package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Vision: a synthetic body with the Mind Stone in its forehead. Flies, fires the Mind Stone's beam, and controls his
 * own density: intangible enough to walk through walls, or dense enough to hit like a falling building.
 */
public final class VisionAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("vision_body_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("vision_body_armor");
    private static final ResourceLocation DENSE_ARMOR = Enhanced.id("vision_dense_armor");
    private static final ResourceLocation DENSE_KNOCKBACK = Enhanced.id("vision_dense_knockback");
    private static final ResourceLocation DENSE_DAMAGE = Enhanced.id("vision_dense_damage");
    private static final ResourceLocation DENSE_PUNCH = Enhanced.id("vision_dense_punch");
    private static final ResourceLocation DENSE_SPEED = Enhanced.id("vision_dense_speed");
    private static final ResourceLocation DENSE_GRAVITY = Enhanced.id("vision_dense_gravity");

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        switch (ability) {
            case 1 -> {
                boolean start = down && !player.isSpectator() && !data.visionPhased();
                if (start && !data.beamActive()) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.8F);
                }
                data.setBeamActive(start);
            }
            case 2 -> {
                if (down) {
                    PowerTicker.toggleFlight(player);
                }
            }
            case 3 -> {
                if (down) {
                    togglePhase(player, data);
                }
            }
            case 4 -> {
                if (down) {
                    toggleDensity(player, data);
                }
            }
            case 5 -> {
                if (down) {
                    data.setDisguise(!data.disguise());
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.2F);
                    player.serverLevel().sendParticles(new EnergyParticleOptions(Powers.glow(player), 1.4F), player.getX(), player.getY() + 1, player.getZ(),
                            30, 0.3, 0.8, 0.3, 0.02);
                    player.displayClientMessage(Component.translatable(data.disguise()
                            ? "message.enhanced.disguise.on" : "message.enhanced.disguise.off"), true);
                }
            }
            default -> {
            }
        }
    }

    static void tick(ServerPlayer player, PowerData data) {
        // A synthetic body: never hungry, never out of breath.
        if (player.getFoodData().getFoodLevel() < 20) {
            player.getFoodData().setFoodLevel(20);
        }
        player.setAirSupply(player.getMaxAirSupply());

        if (data.visionPhased()) {
            phaseTick(player, data);
        }
        if (data.beamActive()) {
            if (data.visionPhased()) {
                data.setBeamActive(false);
            } else {
                beamTick(player);
            }
        }
        if (data.dense() && !player.isSpectator() && player.tickCount % 5 == 0) {
            player.serverLevel().sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.1, player.getZ(), 1, 0.3, 0, 0.3, 0);
        }
    }

    /** The synthetic body's stats and the dense mode's, kept in sync with the player's state. */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean vision = data.power() == Power.VISION;
        boolean dense = vision && data.dense();
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, 10.0, vision);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, 8.0, vision);
        PowerTicker.setModifier(player, Attributes.ARMOR, DENSE_ARMOR, 12.0, dense);
        PowerTicker.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, DENSE_KNOCKBACK, 1.0, dense);
        PowerTicker.setModifier(player, Attributes.ATTACK_DAMAGE, DENSE_DAMAGE, 6.0, dense);
        PowerTicker.setModifier(player, Attributes.ATTACK_KNOCKBACK, DENSE_PUNCH, 1.5, dense);
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, DENSE_SPEED, -0.3, dense, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PowerTicker.setModifier(player, Attributes.GRAVITY, DENSE_GRAVITY, 0.04, dense);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static boolean isPhased(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.VISION && data.visionPhased();
    }

    public static boolean isDense(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.VISION && data.dense();
    }

    // ---- Ability 1: Mind Stone beam ----

    private static void beamTick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        HitResult hit = Raycasts.any(player, EnhancedConfig.CHAOS_BEAM_RANGE.get(), WandaAbilities.BEAM_TARGET, 1.0F);
        if (hit instanceof EntityHitResult entityHit) {
            entityHit.getEntity().hurt(ModDamageTypes.source(level, ModDamageTypes.MIND_STONE_BEAM, player, player),
                    EnhancedConfig.MIND_STONE_BEAM_DAMAGE.get().floatValue());
        }
        Vec3 end = hit.getLocation();
        if (player.tickCount % 2 == 0) {
            level.sendParticles(new EnergyParticleOptions(Powers.glow(player), 1.4F), end.x, end.y, end.z, 4, 0.12, 0.12, 0.12, 0.12);
        }
    }

    // ---- Ability 3: Phase (intangibility) ----

    private static void togglePhase(ServerPlayer player, PowerData data) {
        if (data.visionPhased()) {
            // Can't become solid inside a wall; slightly sunk into the floor is fine, he rises out of it.
            Double lift = freeSpaceAbove(player);
            if (lift == null) {
                player.displayClientMessage(Component.translatable("message.enhanced.phase.inside_wall"), true);
                return;
            }
            if (lift > 0) {
                player.teleportTo(player.getX(), player.getY() + lift, player.getZ());
            }
            data.setVisionPhased(false);
            player.displayClientMessage(Component.translatable("message.enhanced.phase.off"), true);
        } else {
            data.setDense(false);
            data.setBeamActive(false);
            data.setVisionPhased(true);
            // Start floating right away, so he doesn't sink into the floor before the next tick.
            if (player.mayFly() && !player.getAbilities().flying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
            player.displayClientMessage(Component.translatable("message.enhanced.phase.on"), true);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.VISION_PHASE.get(), SoundSource.PLAYERS, 1.0F,
                data.visionPhased() ? 1.15F : 0.85F);
    }

    /** How far up (0 to 1 block) Vision must rise to stand free of blocks, or null if he is stuck deeper in a wall. */
    private static Double freeSpaceAbove(ServerPlayer player) {
        for (int step = 0; step <= 10; step++) {
            double dy = step * 0.1;
            if (player.level().noCollision(player, player.getBoundingBox().move(0, dy, 0))) {
                return dy;
            }
        }
        return null;
    }

    private static void phaseTick(ServerPlayer player, PowerData data) {
        // Intangible: he floats instead of sinking through the floor.
        Abilities abilities = player.getAbilities();
        if (player.mayFly() && !abilities.flying) {
            abilities.flying = true;
            player.onUpdateAbilities();
        }
        player.resetFallDistance();
        // Lava and fire pass through him like everything else.
        player.clearFire();
        // Walls can't hold him, but the world border does.
        WorldBorder border = player.level().getWorldBorder();
        if (!border.isWithinBounds(player.getBoundingBox())) {
            double half = player.getBbWidth() / 2 + 1.0E-3;
            player.teleportTo(Mth.clamp(player.getX(), border.getMinX() + half, border.getMaxX() - half), player.getY(),
                    Mth.clamp(player.getZ(), border.getMinZ() + half, border.getMaxZ() - half));
        }
        if (!player.isSpectator() && player.tickCount % 4 == 0) {
            player.serverLevel().sendParticles(new EnergyParticleOptions(Powers.glow(player), 0.9F), player.getX(), player.getY() + 1.0, player.getZ(),
                    1, 0.3, 0.7, 0.3, 0.0);
        }
    }

    // ---- Ability 4: Density ----

    private static void toggleDensity(ServerPlayer player, PowerData data) {
        if (data.visionPhased()) {
            player.displayClientMessage(Component.translatable("message.enhanced.dense.phased"), true);
            return;
        }
        boolean dense = !data.dense();
        data.setDense(dense);
        if (dense && player.getAbilities().flying && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.displayClientMessage(Component.translatable(dense ? "message.enhanced.dense.on" : "message.enhanced.dense.off"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                dense ? SoundEvents.ANVIL_PLACE : SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 0.7F, dense ? 0.6F : 1.4F);
    }

    private VisionAbilities() {
    }
}
