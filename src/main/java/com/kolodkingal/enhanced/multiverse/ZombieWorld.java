package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Earth-2149, the zombies' universe (3.19.1): every creature is a zombie of its kind. One that the game has a zombie of
 * comes as that zombie (a villager or a wandering trader as a zombie villager, a piglin as a zombified piglin, a hoglin
 * as a zoglin, a horse, a donkey or a mule as a zombie horse); every other kind is zombified as it is: it rots (the
 * client's {@code WhatIfLooks}), it is half again as tough, it groans, it hunts every player on sight, even a cow or a
 * chicken ({@link WhatIfGoals#hunt}), and it drops rotten flesh. The undead stay as they are.
 */
public final class ZombieWorld {
    /** The tag of a creature already zombified (its toughness and its flesh are given once, and saved). */
    static final String ZOMBIFIED = "enhanced_zombified";
    static final ResourceLocation HEALTH = Enhanced.id("zombified_health");
    /** How far round a player the zombified groan, and how often (one chance in this, each second). */
    static final double GROAN_RANGE = 16.0;
    static final int GROAN_CHANCE = 12;

    private ZombieWorld() {
    }

    /** The game's own zombie of a kind, or null for a kind that has none. */
    @Nullable
    public static EntityType<?> zombieOf(EntityType<?> type) {
        if (type == EntityType.VILLAGER || type == EntityType.WANDERING_TRADER) {
            return EntityType.ZOMBIE_VILLAGER;
        }
        if (type == EntityType.PIGLIN || type == EntityType.PIGLIN_BRUTE) {
            return EntityType.ZOMBIFIED_PIGLIN;
        }
        if (type == EntityType.HOGLIN) {
            return EntityType.ZOGLIN;
        }
        if (type == EntityType.HORSE || type == EntityType.DONKEY || type == EntityType.MULE) {
            return EntityType.ZOMBIE_HORSE;
        }
        return null;
    }

    /** Whether a creature is one of those that zombify (not undead, not one of the mod's own: its heroes' foes and the universe's people). */
    static boolean zombifies(Mob mob) {
        return !mob.getType().is(EntityTypeTags.UNDEAD) && !WhatIfPeople.isPerson(mob)
                && !Enhanced.MODID.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace());
    }

    /**
     * A creature comes into Earth-2149: returns the zombie to make in its place (it is not added), or null when it stays,
     * zombified.
     */
    @Nullable
    static EntityType<?> onJoin(Mob mob) {
        if (!zombifies(mob)) {
            return null;
        }
        EntityType<?> zombie = zombieOf(mob.getType());
        if (zombie != null && !mob.isPersistenceRequired() && !mob.isVehicle()) {
            return zombie;
        }
        zombify(mob);
        return null;
    }

    /** Makes a creature a zombie of its kind: once its toughness and its flesh, every time its hunger for the living. */
    public static void zombify(Mob mob) {
        if (!mob.getTags().contains(ZOMBIFIED)) {
            mob.addTag(ZOMBIFIED);
            AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
            if (health != null && health.getModifier(HEALTH) == null) {
                health.addPermanentModifier(new AttributeModifier(HEALTH, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                mob.setHealth(mob.getMaxHealth());
            }
            if (!(mob instanceof Enemy) && mob.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.ROTTEN_FLESH, 1 + mob.getRandom().nextInt(2)));
                mob.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
            }
        }
        WhatIfGoals.hunt(mob);
    }

    /** Once a second for each player in Earth-2149: the zombified round him groan now and then. */
    static void tick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<Mob> near = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(GROAN_RANGE),
                m -> m.getTags().contains(ZOMBIFIED));
        for (Mob mob : near) {
            if (mob.getRandom().nextInt(GROAN_CHANCE) == 0) {
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ZOMBIE_AMBIENT, mob.getSoundSource(), 0.6F,
                        0.6F + mob.getRandom().nextFloat() * 0.3F);
            }
        }
    }

    /** Whether the creature was zombified here (for the tests). */
    public static boolean isZombified(Entity entity) {
        return entity.getTags().contains(ZOMBIFIED);
    }
}
