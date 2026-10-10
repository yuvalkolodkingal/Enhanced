package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.entity.LokiDaggerEntity;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.entity.PhotonBlastEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Enhanced.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<IronShrapnelEntity>> IRON_SHRAPNEL = ENTITIES.register("iron_shrapnel",
            () -> EntityType.Builder.<IronShrapnelEntity>of(IronShrapnelEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("iron_shrapnel"));

    public static final DeferredHolder<EntityType<?>, EntityType<PhotonBlastEntity>> PHOTON_BLAST = ENTITIES.register("photon_blast",
            () -> EntityType.Builder.<PhotonBlastEntity>of(PhotonBlastEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(6)
                    .updateInterval(10)
                    .build("photon_blast"));

    public static final DeferredHolder<EntityType<?>, EntityType<TelekineticBlockEntity>> TELEKINETIC_BLOCK = ENTITIES.register("telekinetic_block",
            () -> EntityType.Builder.<TelekineticBlockEntity>of(TelekineticBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("telekinetic_block"));

    /** Mjolnir or Stormbreaker, thrown by Thor. */
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownWeaponEntity>> THROWN_WEAPON = ENTITIES.register("thrown_weapon",
            () -> EntityType.Builder.<ThrownWeaponEntity>of(ThrownWeaponEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .build("thrown_weapon"));
    public static final DeferredHolder<EntityType<?>, EntityType<LokiDaggerEntity>> LOKI_DAGGER = ENTITIES.register("loki_dagger",
            () -> EntityType.Builder.<LokiDaggerEntity>of(LokiDaggerEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("loki_dagger"));

    /** Never saved: an illusion lasts seconds, and its caster may not be there when the world loads again. */
    public static final DeferredHolder<EntityType<?>, EntityType<LokiIllusionEntity>> LOKI_ILLUSION = ENTITIES.register("loki_illusion",
            () -> EntityType.Builder.<LokiIllusionEntity>of(LokiIllusionEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .noSave()
                    .noSummon()
                    .build("loki_illusion"));

    /** Mod bus: attributes of the mod's living entities. */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LOKI_ILLUSION.get(), LokiIllusionEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
