package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import java.util.Optional;

/** 26.3 only: small things 1.21.1 made from an item stack or a name. */
public final class Particles {
    private Particles() {
    }

    /** The item particle of a stack (26.3's takes a template of a stack that is not empty). */
    public static ItemParticleOption item(ItemStack stack) {
        return new ItemParticleOption(ParticleTypes.ITEM, stack.isEmpty() ? new ItemStackTemplate(Items.STONE)
                : ItemStackTemplate.fromNonEmptyStack(stack));
    }

    /** 1.21.1's {@code EntityType.byString}. */
    public static Optional<EntityType<?>> entityType(String id) {
        Identifier key = Identifier.tryParse(id);
        return key == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(key);
    }
}
