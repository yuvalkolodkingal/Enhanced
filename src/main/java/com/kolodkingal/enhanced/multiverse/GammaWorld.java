package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * What If... the Gamma Bomb Went Off? (3.19.1): Bruce Banner's gamma bomb went off over the whole world, and every
 * creature in it came out of the green light a Hulk of its kind: bigger (half again to more than twice its size), four
 * times as tough, two and a half times as strong, faster, harder to knock back and thick-skinned, each a little
 * different from the next. They glow with gamma (green sparks round them near a player), and the air is green
 * (the client's fog). Their hide swells green and veined (the client's {@code WhatIfLooks}), and every one of them, a cow
 * or a chicken too, turns on whoever hurts it, and its kind round it join in ({@link WhatIfGoals#temper}). Players and
 * the survivors stay as they are.
 */
public final class GammaWorld {
    static final ResourceLocation SCALE = Enhanced.id("gamma_scale");
    static final ResourceLocation HEALTH = Enhanced.id("gamma_health");
    static final ResourceLocation DAMAGE = Enhanced.id("gamma_damage");
    static final ResourceLocation SPEED = Enhanced.id("gamma_speed");
    static final ResourceLocation KNOCKBACK = Enhanced.id("gamma_knockback");
    static final ResourceLocation ARMOR = Enhanced.id("gamma_armor");
    /** The gamma's green. */
    public static final int GREEN = 0x5BFF3A;
    /** How far round a player the gamma sparks show, and on how many creatures at most. */
    static final double SPARK_RANGE = 24.0;
    static final int SPARK_MAX = 12;

    private GammaWorld() {
    }

    /** Whether the creature is already a Hulk of its kind. */
    public static boolean isHulk(LivingEntity living) {
        AttributeInstance scale = living.getAttribute(Attributes.SCALE);
        return scale != null && scale.getModifier(SCALE) != null;
    }

    /** Makes a creature that comes into the Gamma world a Hulk of its kind (once: the changes are saved with it). */
    public static void hulkify(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity instanceof Player || entity instanceof ArmorStand
                || WhatIfPeople.isPerson(entity)
                || com.kolodkingal.enhanced.Enhanced.MODID.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace())) {
            return; // (3.19.1) the mod's own bodies, as Wanda's Hex Vision, are not the Gamma Bomb's
        }
        // Its temper, every time it comes (goals are not saved): it turns on whoever hurts it.
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            WhatIfGoals.temper(mob);
        }
        if (isHulk(living)) {
            return;
        }
        int h = entity.getUUID().hashCode();
        float size = entity.getBbWidth() > 1.5F || entity.getBbHeight() > 2.6F ? 0.3F : 0.5F + Math.floorMod(h, 8) * 0.1F;
        add(living, Attributes.SCALE, SCALE, size, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(living, Attributes.MAX_HEALTH, HEALTH, 3.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(living, Attributes.ATTACK_DAMAGE, DAMAGE, 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(living, Attributes.MOVEMENT_SPEED, SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(living, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, 0.6, AttributeModifier.Operation.ADD_VALUE);
        add(living, Attributes.ARMOR, ARMOR, 4.0 + Math.floorMod(h >> 4, 5), AttributeModifier.Operation.ADD_VALUE);
        living.setHealth(living.getMaxHealth());
    }

    private static void add(LivingEntity living, Holder<Attribute> attribute, ResourceLocation id, double amount,
                            AttributeModifier.Operation operation) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /** Once a second for each player in the Gamma world: the gamma sparks on the Hulks near him. */
    static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SPARK_RANGE),
                e -> e != player && isHulk(e));
        int shown = 0;
        for (LivingEntity hulk : near) {
            if (shown++ >= SPARK_MAX) {
                break;
            }
            double w = hulk.getBbWidth() * 0.5;
            level.sendParticles(new EnergyParticleOptions(GREEN, 0.9F), hulk.getX(), hulk.getY() + hulk.getBbHeight() * 0.5,
                    hulk.getZ(), 3, w, hulk.getBbHeight() * 0.35, w, 0.02);
        }
    }
}
