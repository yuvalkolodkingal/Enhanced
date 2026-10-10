package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.entity.PhotonBlastEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.entity.ThrownMjolnirEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownMjolnirEntity>> THROWN_MJOLNIR = ENTITIES.register("thrown_mjolnir",
            () -> EntityType.Builder.<ThrownMjolnirEntity>of(ThrownMjolnirEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .build("thrown_mjolnir"));

    private ModEntities() {
    }
}
