package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.entity.LokiDaggerEntity;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.entity.MjolnirPinEntity;
import com.kolodkingal.enhanced.entity.SandBoulderEntity;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.entity.SandLanceEntity;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosMeteorEntity;
import com.kolodkingal.enhanced.thanos.ThanosOrbEntity;
import com.kolodkingal.enhanced.thanos.ThanosPhantomEntity;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.VibraniumKeyEntity;
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

    /** A fragment of a shattered Mjolnir. Never saved: the hammer is kept in Thor's own data while it is in pieces. */
    public static final DeferredHolder<EntityType<?>, EntityType<MjolnirFragmentEntity>> MJOLNIR_FRAGMENT = ENTITIES.register("mjolnir_fragment",
            () -> EntityType.Builder.<MjolnirFragmentEntity>of(MjolnirFragmentEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .build("mjolnir_fragment"));

    /** Mjolnir lying on someone it pins down. */
    public static final DeferredHolder<EntityType<?>, EntityType<MjolnirPinEntity>> MJOLNIR_PIN = ENTITIES.register("mjolnir_pin",
            () -> EntityType.Builder.<MjolnirPinEntity>of(MjolnirPinEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("mjolnir_pin"));

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

    /** The Vision of Wanda's Hex. Never saved: the Hex that raised him makes him again when the world is loaded. */
    public static final DeferredHolder<EntityType<?>, EntityType<HexVisionEntity>> HEX_VISION = ENTITIES.register("hex_vision",
            () -> EntityType.Builder.<HexVisionEntity>of(HexVisionEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .build("hex_vision"));

    // The entities of 3.0, in one region per hero (the // ===== lines). None can be summoned (the bosses are started by
    // a ritual or by /enhanced boss start), and none but a Sentry is saved with the world: a fight does not survive a
    // restart.

    // ===== thanos =====
    public static final DeferredHolder<EntityType<?>, EntityType<ThanosEntity>> THANOS = ENTITIES.register("thanos",
            () -> EntityType.Builder.<ThanosEntity>of(ThanosEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.3F)
                    .eyeHeight(2.95F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .noSave()
                    .noSummon()
                    .build("thanos"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThanosSwordEntity>> THANOS_SWORD = ENTITIES.register("thanos_sword",
            () -> EntityType.Builder.<ThanosSwordEntity>of(ThanosSwordEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("thanos_sword"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThanosMeteorEntity>> THANOS_METEOR = ENTITIES.register("thanos_meteor",
            () -> EntityType.Builder.<ThanosMeteorEntity>of(ThanosMeteorEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("thanos_meteor"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThanosOrbEntity>> THANOS_ORB = ENTITIES.register("thanos_orb",
            () -> EntityType.Builder.<ThanosOrbEntity>of(ThanosOrbEntity::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("thanos_orb"));
    public static final DeferredHolder<EntityType<?>, EntityType<ThanosPhantomEntity>> THANOS_PHANTOM = ENTITIES.register("thanos_phantom",
            () -> EntityType.Builder.<ThanosPhantomEntity>of(ThanosPhantomEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.3F)
                    .eyeHeight(2.95F)
                    .clientTrackingRange(16)
                    .noSave()
                    .noSummon()
                    .build("thanos_phantom"));

    // ===== ultron =====
    public static final DeferredHolder<EntityType<?>, EntityType<UltronEntity>> ULTRON = ENTITIES.register("ultron",
            () -> EntityType.Builder.<UltronEntity>of(UltronEntity::new, MobCategory.MONSTER)
                    .sized(1.3F, 3.4F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("ultron"));
    public static final DeferredHolder<EntityType<?>, EntityType<UltronSentryEntity>> ULTRON_SENTRY = ENTITIES.register("ultron_sentry",
            () -> EntityType.Builder.<UltronSentryEntity>of(UltronSentryEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.7F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .noSummon()
                    .build("ultron_sentry"));
    public static final DeferredHolder<EntityType<?>, EntityType<VibraniumKeyEntity>> VIBRANIUM_KEY = ENTITIES.register("vibranium_key",
            () -> EntityType.Builder.<VibraniumKeyEntity>of(VibraniumKeyEntity::new, MobCategory.MISC)
                    .sized(3.2F, 3.4F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .noSave()
                    .noSummon()
                    .build("vibranium_key"));
    public static final DeferredHolder<EntityType<?>, EntityType<UltronBoltEntity>> ULTRON_BOLT = ENTITIES.register("ultron_bolt",
            () -> EntityType.Builder.<UltronBoltEntity>of(UltronBoltEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("ultron_bolt"));

    // ===== apocalypse =====
    public static final DeferredHolder<EntityType<?>, EntityType<ApocalypseEntity>> APOCALYPSE = ENTITIES.register("apocalypse",
            () -> EntityType.Builder.<ApocalypseEntity>of(ApocalypseEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.9F)
                    .eyeHeight(2.6F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("apocalypse"));
    public static final DeferredHolder<EntityType<?>, EntityType<SandLanceEntity>> SAND_LANCE = ENTITIES.register("sand_lance",
            () -> EntityType.Builder.<SandLanceEntity>of(SandLanceEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("sand_lance"));
    public static final DeferredHolder<EntityType<?>, EntityType<SandBoulderEntity>> SAND_BOULDER = ENTITIES.register("sand_boulder",
            () -> EntityType.Builder.<SandBoulderEntity>of(SandBoulderEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("sand_boulder"));
    public static final DeferredHolder<EntityType<?>, EntityType<SandCocoonEntity>> SAND_COCOON = ENTITIES.register("sand_cocoon",
            () -> EntityType.Builder.<SandCocoonEntity>of(SandCocoonEntity::new, MobCategory.MISC)
                    .sized(0.9F, 2.0F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .noSave()
                    .noSummon()
                    .build("sand_cocoon"));

    /** Mod bus: attributes of the mod's living entities. */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LOKI_ILLUSION.get(), LokiIllusionEntity.createAttributes().build());
        event.put(HEX_VISION.get(), HexVisionEntity.createAttributes().build());
        event.put(THANOS.get(), ThanosEntity.createAttributes().build());
        event.put(THANOS_PHANTOM.get(), ThanosPhantomEntity.createAttributes().build());
        event.put(ULTRON.get(), UltronEntity.createAttributes().build());
        event.put(ULTRON_SENTRY.get(), UltronSentryEntity.createAttributes().build());
        event.put(APOCALYPSE.get(), ApocalypseEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
