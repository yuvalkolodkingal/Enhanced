package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Enhanced.MODID);

    /**
     * Magneto's helmet: one piece that protects like a whole Netherite set
     * (20 armor, 12 toughness, 40% knockback resistance).
     */
    public static final Holder<ArmorMaterial> MAGNETO = ARMOR_MATERIALS.register("magneto", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                    map.put(type, 20);
                }
            }),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(Items.NETHERITE_BLOCK),
            List.of(new ArmorMaterial.Layer(Enhanced.id("magneto_helmet"))),
            12.0F,
            0.4F));

    /**
     * Wanda's crown. It has no protection of its own: the Scarlet Witch gets her Diamond-level protection and
     * 40 max health from {@link com.kolodkingal.enhanced.ability.WandaAbilities} while she wears it.
     */
    public static final Holder<ArmorMaterial> WANDA_CROWN = ARMOR_MATERIALS.register("wanda_crown", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                    map.put(type, 0);
                }
            }),
            25,
            SoundEvents.ARMOR_EQUIP_GOLD,
            () -> Ingredient.of(Items.GOLD_INGOT),
            List.of(new ArmorMaterial.Layer(Enhanced.id("wanda_crown"))),
            0.0F,
            0.0F));

    private ModArmorMaterials() {
    }
}
