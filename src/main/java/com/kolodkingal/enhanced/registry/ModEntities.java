package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.asgard.SeatEntity;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.hela.BerserkerEntity;
import com.kolodkingal.enhanced.hela.BladeSpikeEntity;
import com.kolodkingal.enhanced.hela.FenrisEntity;
import com.kolodkingal.enhanced.hela.HelaEntity;
import com.kolodkingal.enhanced.hela.HelaShadowEntity;
import com.kolodkingal.enhanced.odin.GungnirEntity;
import com.kolodkingal.enhanced.odin.RavenEntity;
import com.kolodkingal.enhanced.hela.NecroswordEntity;
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
import com.kolodkingal.enhanced.platform.reg.RegEntry;
import com.kolodkingal.enhanced.platform.reg.Registrar;
import com.kolodkingal.enhanced.platform.reg.Registrars;
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

public final class ModEntities {
    public static final Registrar<EntityType<?>> ENTITIES = Registrars.create(Registries.ENTITY_TYPE, Enhanced.MODID);

    public static final RegEntry<EntityType<?>, EntityType<IronShrapnelEntity>> IRON_SHRAPNEL = ENTITIES.register("iron_shrapnel",
            () -> EntityType.Builder.<IronShrapnelEntity>of(IronShrapnelEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("iron_shrapnel"));

    public static final RegEntry<EntityType<?>, EntityType<TelekineticBlockEntity>> TELEKINETIC_BLOCK = ENTITIES.register("telekinetic_block",
            () -> EntityType.Builder.<TelekineticBlockEntity>of(TelekineticBlockEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("telekinetic_block"));

    /** Mjolnir or Stormbreaker, thrown by Thor. */
    public static final RegEntry<EntityType<?>, EntityType<ThrownWeaponEntity>> THROWN_WEAPON = ENTITIES.register("thrown_weapon",
            () -> EntityType.Builder.<ThrownWeaponEntity>of(ThrownWeaponEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .build("thrown_weapon"));

    /** A fragment of a shattered Mjolnir. Never saved: the hammer is kept in Thor's own data while it is in pieces. */
    public static final RegEntry<EntityType<?>, EntityType<MjolnirFragmentEntity>> MJOLNIR_FRAGMENT = ENTITIES.register("mjolnir_fragment",
            () -> EntityType.Builder.<MjolnirFragmentEntity>of(MjolnirFragmentEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .build("mjolnir_fragment"));

    /** Mjolnir lying on someone it pins down. */
    public static final RegEntry<EntityType<?>, EntityType<MjolnirPinEntity>> MJOLNIR_PIN = ENTITIES.register("mjolnir_pin",
            () -> EntityType.Builder.<MjolnirPinEntity>of(MjolnirPinEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .fireImmune()
                    .noSummon()
                    .build("mjolnir_pin"));

    public static final RegEntry<EntityType<?>, EntityType<LokiDaggerEntity>> LOKI_DAGGER = ENTITIES.register("loki_dagger",
            () -> EntityType.Builder.<LokiDaggerEntity>of(LokiDaggerEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("loki_dagger"));

    /** Never saved: an illusion lasts seconds, and its caster may not be there when the world loads again. */
    public static final RegEntry<EntityType<?>, EntityType<LokiIllusionEntity>> LOKI_ILLUSION = ENTITIES.register("loki_illusion",
            () -> EntityType.Builder.<LokiIllusionEntity>of(LokiIllusionEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .noSave()
                    .noSummon()
                    .build("loki_illusion"));

    /** The Vision of Wanda's Hex. Never saved: the Hex that raised him makes him again when the world is loaded. */
    public static final RegEntry<EntityType<?>, EntityType<HexVisionEntity>> HEX_VISION = ENTITIES.register("hex_vision",
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
    public static final RegEntry<EntityType<?>, EntityType<ThanosEntity>> THANOS = ENTITIES.register("thanos",
            () -> EntityType.Builder.<ThanosEntity>of(ThanosEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.3F)
                    .eyeHeight(2.95F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .noSave()
                    .noSummon()
                    .build("thanos"));
    public static final RegEntry<EntityType<?>, EntityType<ThanosSwordEntity>> THANOS_SWORD = ENTITIES.register("thanos_sword",
            () -> EntityType.Builder.<ThanosSwordEntity>of(ThanosSwordEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("thanos_sword"));
    public static final RegEntry<EntityType<?>, EntityType<ThanosMeteorEntity>> THANOS_METEOR = ENTITIES.register("thanos_meteor",
            () -> EntityType.Builder.<ThanosMeteorEntity>of(ThanosMeteorEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("thanos_meteor"));
    public static final RegEntry<EntityType<?>, EntityType<ThanosOrbEntity>> THANOS_ORB = ENTITIES.register("thanos_orb",
            () -> EntityType.Builder.<ThanosOrbEntity>of(ThanosOrbEntity::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("thanos_orb"));
    public static final RegEntry<EntityType<?>, EntityType<ThanosPhantomEntity>> THANOS_PHANTOM = ENTITIES.register("thanos_phantom",
            () -> EntityType.Builder.<ThanosPhantomEntity>of(ThanosPhantomEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.3F)
                    .eyeHeight(2.95F)
                    .clientTrackingRange(16)
                    .noSave()
                    .noSummon()
                    .build("thanos_phantom"));

    // ===== ultron =====
    public static final RegEntry<EntityType<?>, EntityType<UltronEntity>> ULTRON = ENTITIES.register("ultron",
            () -> EntityType.Builder.<UltronEntity>of(UltronEntity::new, MobCategory.MONSTER)
                    .sized(1.3F, 3.4F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("ultron"));
    public static final RegEntry<EntityType<?>, EntityType<UltronSentryEntity>> ULTRON_SENTRY = ENTITIES.register("ultron_sentry",
            () -> EntityType.Builder.<UltronSentryEntity>of(UltronSentryEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.7F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .noSummon()
                    .build("ultron_sentry"));
    public static final RegEntry<EntityType<?>, EntityType<VibraniumKeyEntity>> VIBRANIUM_KEY = ENTITIES.register("vibranium_key",
            () -> EntityType.Builder.<VibraniumKeyEntity>of(VibraniumKeyEntity::new, MobCategory.MISC)
                    .sized(3.2F, 3.4F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .noSave()
                    .noSummon()
                    .build("vibranium_key"));
    public static final RegEntry<EntityType<?>, EntityType<UltronBoltEntity>> ULTRON_BOLT = ENTITIES.register("ultron_bolt",
            () -> EntityType.Builder.<UltronBoltEntity>of(UltronBoltEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("ultron_bolt"));

    // ===== apocalypse =====
    public static final RegEntry<EntityType<?>, EntityType<ApocalypseEntity>> APOCALYPSE = ENTITIES.register("apocalypse",
            () -> EntityType.Builder.<ApocalypseEntity>of(ApocalypseEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.9F)
                    .eyeHeight(2.6F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("apocalypse"));
    public static final RegEntry<EntityType<?>, EntityType<SandLanceEntity>> SAND_LANCE = ENTITIES.register("sand_lance",
            () -> EntityType.Builder.<SandLanceEntity>of(SandLanceEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("sand_lance"));
    public static final RegEntry<EntityType<?>, EntityType<SandBoulderEntity>> SAND_BOULDER = ENTITIES.register("sand_boulder",
            () -> EntityType.Builder.<SandBoulderEntity>of(SandBoulderEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("sand_boulder"));
    public static final RegEntry<EntityType<?>, EntityType<SandCocoonEntity>> SAND_COCOON = ENTITIES.register("sand_cocoon",
            () -> EntityType.Builder.<SandCocoonEntity>of(SandCocoonEntity::new, MobCategory.MISC)
                    .sized(0.9F, 2.0F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .noSave()
                    .noSummon()
                    .build("sand_cocoon"));

    // ===== asgard (3.5) =====
    // (The skeleton registers the two entities another lane needs by name; each lane adds its own in its region below.)
    /** The invisible seat of the throne and the benches (lane L1; Asgardians sit on it too). Never saved. */
    public static final RegEntry<EntityType<?>, EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F)
                    .clientTrackingRange(10)
                    .noSave()
                    .noSummon()
                    .build("seat"));
    // ===== heimdall (3.5) =====
    // ===== odin (3.5) =====
    /** Gungnir in flight: Odin's Gungnir Throw (it comes home to his hand). */
    public static final RegEntry<EntityType<?>, EntityType<GungnirEntity>> GUNGNIR = ENTITIES.register("gungnir",
            () -> EntityType.Builder.<GungnirEntity>of(GungnirEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("gungnir"));
    /** Huginn or Muninn, one of Odin's two ravens (never saved). */
    public static final RegEntry<EntityType<?>, EntityType<RavenEntity>> RAVEN = ENTITIES.register("raven",
            () -> EntityType.Builder.<RavenEntity>of(RavenEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.4F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("raven"));
    // ===== hela (3.5) =====
    /** A Shadow of Hela of the Guardian's Watch (lane L2 spawns them; lane L4 owns the entity). Never saved. */
    public static final RegEntry<EntityType<?>, EntityType<HelaShadowEntity>> HELA_SHADOW = ENTITIES.register("hela_shadow",
            () -> EntityType.Builder.<HelaShadowEntity>of(HelaShadowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.6F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .noSave()
                    .noSummon()
                    .build("hela_shadow"));
    /** Hela, the boss of the throne room (lane L4): made only by her fight. */
    public static final RegEntry<EntityType<?>, EntityType<HelaEntity>> HELA = ENTITIES.register("hela",
            () -> EntityType.Builder.<HelaEntity>of(HelaEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.1F)
                    .eyeHeight(1.85F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("hela"));
    /** A berserker of her army (the boss's waves, the hero's Raise the Dead). Never saved. */
    public static final RegEntry<EntityType<?>, EntityType<BerserkerEntity>> BERSERKER = ENTITIES.register("berserker",
            () -> EntityType.Builder.<BerserkerEntity>of(BerserkerEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .noSave()
                    .noSummon()
                    .build("berserker"));
    /** Fenris, her giant wolf. Never saved. */
    public static final RegEntry<EntityType<?>, EntityType<FenrisEntity>> FENRIS = ENTITIES.register("fenris",
            () -> EntityType.Builder.<FenrisEntity>of(FenrisEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 2.4F)
                    .eyeHeight(2.0F)
                    .clientTrackingRange(12)
                    .noSave()
                    .noSummon()
                    .build("fenris"));
    /** A necrosword in flight, or hanging over a Blade Rain. */
    public static final RegEntry<EntityType<?>, EntityType<NecroswordEntity>> NECROSWORD = ENTITIES.register("necrosword",
            () -> EntityType.Builder.<NecroswordEntity>of(NecroswordEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .build("necrosword"));
    /** A spike of a Blade Wall or a Blade Burst: solid while it stands. */
    public static final RegEntry<EntityType<?>, EntityType<BladeSpikeEntity>> BLADE_SPIKE = ENTITIES.register("blade_spike",
            () -> EntityType.Builder.<BladeSpikeEntity>of(BladeSpikeEntity::new, MobCategory.MISC)
                    .sized(0.7F, 1.7F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .noSave()
                    .noSummon()
                    .build("blade_spike"));
    // ===== asgardians (3.5) =====
    /** An Asgardian of any role (lane L5). Saved; the keeper makes and keeps them. */
    public static final RegEntry<EntityType<?>, EntityType<AsgardianEntity>> ASGARDIAN = ENTITIES.register("asgardian",
            () -> EntityType.Builder.<AsgardianEntity>of(AsgardianEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("asgardian"));

    // ===== strange (3.6) =====
    // (The skeleton registers every entity of 3.6, drawn as nothing or as a placeholder; each lane owns its classes.)
    /** One ring of a pair of Sling-Ring portals (lane L3). Never saved. */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.strange.SlingPortalEntity>> SLING_PORTAL =
            ENTITIES.register("sling_portal",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.strange.SlingPortalEntity>of(
                            com.kolodkingal.enhanced.strange.SlingPortalEntity::new, MobCategory.MISC)
                    .sized(2.4F, 2.4F)
                    .clientTrackingRange(10)
                    .noSave()
                    .noSummon()
                    .build("sling_portal"));
    /** Doctor Strange's kneeling body while his spirit is out (lane L3). Never saved. */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.strange.AstralBodyEntity>> ASTRAL_BODY =
            ENTITIES.register("astral_body",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.strange.AstralBodyEntity>of(
                            com.kolodkingal.enhanced.strange.AstralBodyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.3F)
                    .clientTrackingRange(10)
                    .noSave()
                    .noSummon()
                    .build("astral_body"));
    // ===== vormir (3.6) =====
    /** The Stonekeeper of the Vormir Cliff (lane L5). Saved (the cliff's template places him). */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.vormir.StonekeeperEntity>> STONEKEEPER =
            ENTITIES.register("stonekeeper",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.vormir.StonekeeperEntity>of(
                            com.kolodkingal.enhanced.vormir.StonekeeperEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("stonekeeper"));
    // ===== hulk (3.6) =====
    /** The boulder the Hulk throws (lane L7). */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.hulk.HulkBoulderEntity>> HULK_BOULDER =
            ENTITIES.register("hulk_boulder",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.hulk.HulkBoulderEntity>of(
                            com.kolodkingal.enhanced.hulk.HulkBoulderEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("hulk_boulder"));
    // ===== hydra (3.6) =====
    /** A Hydra soldier: a rifleman, an enforcer or a sniper (lane L8). A monster; saved. */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.hydra.HydraSoldierEntity>> HYDRA_SOLDIER =
            ENTITIES.register("hydra_soldier",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.hydra.HydraSoldierEntity>of(
                            com.kolodkingal.enhanced.hydra.HydraSoldierEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("hydra_soldier"));
    /** A Hydra officer (lane L8). A monster; saved. */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.hydra.HydraOfficerEntity>> HYDRA_OFFICER =
            ENTITIES.register("hydra_officer",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.hydra.HydraOfficerEntity>of(
                            com.kolodkingal.enhanced.hydra.HydraOfficerEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("hydra_officer"));
    /** A Hydra energy bolt (lane L8). */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.hydra.HydraBoltEntity>> HYDRA_BOLT =
            ENTITIES.register("hydra_bolt", () -> EntityType.Builder.<com.kolodkingal.enhanced.hydra.HydraBoltEntity>of(
                            com.kolodkingal.enhanced.hydra.HydraBoltEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("hydra_bolt"));

    // ===== healing (3.7) =====
    // ===== wolverine (3.7) =====
    // ===== deadpool (3.7) =====
    // (The skeleton registers every entity of 3.7, drawn as nothing or as a placeholder; each lane owns its classes.)
    /** One of Deadpool's bullets (lane L3). */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.deadpool.DeadpoolBulletEntity>> DEADPOOL_BULLET =
            ENTITIES.register("deadpool_bullet",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.deadpool.DeadpoolBulletEntity>of(
                            com.kolodkingal.enhanced.deadpool.DeadpoolBulletEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("deadpool_bullet"));
    /** Dogpool, Deadpool's dog (lane L3). Not saved for long: he goes home after his time. */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.deadpool.DogpoolEntity>> DOGPOOL =
            ENTITIES.register("dogpool",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.deadpool.DogpoolEntity>of(
                            com.kolodkingal.enhanced.deadpool.DogpoolEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F)
                    .clientTrackingRange(10)
                    .build("dogpool"));
    // ===== school_lab (3.7) =====
    // ===== finders (3.7) =====
    // ===== cottage (3.7) =====
    /**
     * Agatha, Nicky and Rio of Agatha's Cottage (lane L6): a second entity type of the Asgardians' class (its roles
     * AGATHA, NICKY, RIO), so the cottage has their talk, trades, DOWN rule and voices. Saved; the cottage's keeper
     * makes and keeps them.
     */
    public static final RegEntry<EntityType<?>, EntityType<AsgardianEntity>> COTTAGE_RESIDENT =
            ENTITIES.register("cottage_resident",
                    () -> EntityType.Builder.<AsgardianEntity>of(AsgardianEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("cottage_resident"));

    // ===== captain (3.8) =====
    // (The skeleton registers every entity of 3.8, drawn as nothing or as a placeholder box; each lane owns its
    // classes.)
    /** Captain America's thrown shield (lane L1). */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.captain.ThrownShieldEntity>> THROWN_SHIELD =
            ENTITIES.register("thrown_shield",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.captain.ThrownShieldEntity>of(
                            com.kolodkingal.enhanced.captain.ThrownShieldEntity::new, MobCategory.MISC)
                    .sized(0.8F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("thrown_shield"));
    // ===== iron_man (3.8) =====
    /** A rocket or mini-missile of Iron Man's suits (lane L3; the homing lane L4). */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.ironman.IronMissileEntity>> IRON_MISSILE =
            ENTITIES.register("iron_missile",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.ironman.IronMissileEntity>of(
                            com.kolodkingal.enhanced.ironman.IronMissileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("iron_missile"));
    // ===== nanotech (3.8) =====
    /** A nanotech drone of the Mark 85 (lane L4). Not saved for long: it ends with its Iron Man. */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.ironman.nano.NanoDroneEntity>> NANO_DRONE =
            ENTITIES.register("nano_drone",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.ironman.nano.NanoDroneEntity>of(
                            com.kolodkingal.enhanced.ironman.nano.NanoDroneEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.4F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("nano_drone"));
    /** The Hulkbuster's frame that Veronica drops (lane L4). */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.ironman.hulkbuster.HulkbusterFrameEntity>> HULKBUSTER_FRAME =
            ENTITIES.register("hulkbuster_frame",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.ironman.hulkbuster.HulkbusterFrameEntity>of(
                            com.kolodkingal.enhanced.ironman.hulkbuster.HulkbusterFrameEntity::new, MobCategory.MISC)
                    .sized(2.0F, 4.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSummon()
                    .build("hulkbuster_frame"));
    // ===== falcon (3.8) =====
    /** Falcon's Wrist Rocket (lane L5): the class of Iron Man's missiles, a type of its own. */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.ironman.IronMissileEntity>> FALCON_ROCKET =
            ENTITIES.register("falcon_rocket",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.ironman.IronMissileEntity>of(
                            com.kolodkingal.enhanced.ironman.IronMissileEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("falcon_rocket"));
    /** One of Falcon's energy darts (lane L5). */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.falcon.FalconDartEntity>> FALCON_DART =
            ENTITIES.register("falcon_dart",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.falcon.FalconDartEntity>of(
                            com.kolodkingal.enhanced.falcon.FalconDartEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("falcon_dart"));
    /** Redwing, Falcon's drone (lane L5). */
    public static final RegEntry<EntityType<?>,
            EntityType<com.kolodkingal.enhanced.falcon.RedwingEntity>> REDWING =
            ENTITIES.register("redwing",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.falcon.RedwingEntity>of(
                            com.kolodkingal.enhanced.falcon.RedwingEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.35F)
                    .clientTrackingRange(10)
                    .build("redwing"));
    // ===== eternity (3.8) =====
    /** Eternity, in his realm (lane L8): one per realm, kept by {@code EternityRealm}. */
    public static final RegEntry<EntityType<?>, EntityType<com.kolodkingal.enhanced.eternity.EternityEntity>>
            ETERNITY = ENTITIES.register("eternity",
                    () -> EntityType.Builder.<com.kolodkingal.enhanced.eternity.EternityEntity>of(
                            com.kolodkingal.enhanced.eternity.EternityEntity::new, MobCategory.MISC)
                    .sized(16.0F, 44.0F)
                    .clientTrackingRange(32)
                    .fireImmune()
                    .noSummon()
                    .build("eternity"));
    // ===== love (3.8) =====

    /** Mod bus: attributes of the mod's living entities. */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LOKI_ILLUSION.get(), LokiIllusionEntity.createAttributes().build());
        event.put(HEX_VISION.get(), HexVisionEntity.createAttributes().build());
        event.put(THANOS.get(), ThanosEntity.createAttributes().build());
        event.put(THANOS_PHANTOM.get(), ThanosPhantomEntity.createAttributes().build());
        event.put(ULTRON.get(), UltronEntity.createAttributes().build());
        event.put(ULTRON_SENTRY.get(), UltronSentryEntity.createAttributes().build());
        event.put(APOCALYPSE.get(), ApocalypseEntity.createAttributes().build());
        // 3.5
        event.put(HELA_SHADOW.get(), HelaShadowEntity.createAttributes().build());
        // ===== odin (3.5) =====
        event.put(RAVEN.get(), RavenEntity.createAttributes().build());
        event.put(HELA.get(), HelaEntity.createAttributes().build());
        event.put(BERSERKER.get(), BerserkerEntity.createAttributes().build());
        event.put(FENRIS.get(), FenrisEntity.createAttributes().build());
        event.put(ASGARDIAN.get(), AsgardianEntity.createAttributes().build());
        // 3.6
        event.put(ASTRAL_BODY.get(), com.kolodkingal.enhanced.strange.AstralBodyEntity.createAttributes().build());
        event.put(STONEKEEPER.get(), com.kolodkingal.enhanced.vormir.StonekeeperEntity.createAttributes().build());
        event.put(HYDRA_SOLDIER.get(), com.kolodkingal.enhanced.hydra.HydraSoldierEntity.createAttributes().build());
        event.put(HYDRA_OFFICER.get(), com.kolodkingal.enhanced.hydra.HydraOfficerEntity.createAttributes().build());
        // 3.7
        event.put(DOGPOOL.get(), com.kolodkingal.enhanced.deadpool.DogpoolEntity.createAttributes().build());
        event.put(COTTAGE_RESIDENT.get(), AsgardianEntity.createAttributes().build());
        // 3.8
        event.put(REDWING.get(), com.kolodkingal.enhanced.falcon.RedwingEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
