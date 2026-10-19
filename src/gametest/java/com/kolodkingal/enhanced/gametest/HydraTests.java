package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.MindLink;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Thoughts;
import com.kolodkingal.enhanced.config.HydraConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.hydra.HydraAlarm;
import com.kolodkingal.enhanced.hydra.HydraBlasterItem;
import com.kolodkingal.enhanced.hydra.HydraBoltEntity;
import com.kolodkingal.enhanced.hydra.HydraDrops;
import com.kolodkingal.enhanced.hydra.HydraGarrison;
import com.kolodkingal.enhanced.hydra.HydraIntel;
import com.kolodkingal.enhanced.hydra.HydraLoyalty;
import com.kolodkingal.enhanced.hydra.HydraOfficerEntity;
import com.kolodkingal.enhanced.hydra.HydraOutposts;
import com.kolodkingal.enhanced.hydra.HydraSenses;
import com.kolodkingal.enhanced.hydra.HydraSoldierEntity;
import com.kolodkingal.enhanced.hydra.Post;
import com.kolodkingal.enhanced.hydra.Rank;
import com.kolodkingal.enhanced.network.ThoughtsPayload;
import com.kolodkingal.enhanced.network.XavierInputPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.stones.MindStonePowers;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.StonePowers;
import com.kolodkingal.enhanced.world.HydraOutpostPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Hydra soldiers (3.6, lane L8; the plan's 15 and 17.3): their numbers, sight, weapons (the burst of three, the
 * reload, the bolt that passes through Hydra, the riot shield, the Reality Stone, the sniper's warning, the baton), the
 * garrison of an outpost (nine posts, not twice, the respawn after three days only while nobody is near, never and
 * always, Peaceful, the caps, the posts turned every way, the pillagers gone), the alarm (the officer at once, a
 * soldier after 60 ticks of fighting, four reinforcements after 200 ticks, two waves a day, the radio console, the
 * officer killed early, a frozen soldier), the drops and the intel map, and the minds (the Scepter, Professor X, the
 * Mind Stone's seam, the Phoenix). The outposts of these tests are shrunken round the arena's middle ({@link
 * #outpost}); no 33-block template is placed.
 *
 * <p>Batches: {@code hydra}, {@code hydra_garrison}, {@code hydra_alarm}, {@code hydra_minds}, and the ones that
 * change a config or the difficulty ({@code hydra_cfg}, {@code hydra_respawn_never}, {@code hydra_respawn_always},
 * {@code hydra_cap}, {@code hydra_peaceful}).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class HydraTests {
    static final String BATCH = "hydra";
    static final String GARRISON = "hydra_garrison";
    static final String ALARM = "hydra_alarm";
    static final String MINDS = "hydra_minds";
    /** How much a test's outpost is drawn in round the arena's middle (its perimeter fits the arena). */
    static final float SHRINK = 0.4F;

    private static final AtomicLong KEYS = new AtomicLong(-7_000_000_000L);

    private HydraTests() {
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A shrunken outpost centred on the arena's middle (or {@code centre}, relative), forgotten when the test ends. */
    static HydraOutposts.Outpost outpost(TestKit.Fixture f, BlockPos centre, Rotation rotation, float scale) {
        ServerLevel level = f.level();
        BlockPos middle = f.helper().absolutePos(centre);
        BlockPos turned = StructureTemplate.transform(HydraOutpostPlan.CENTRE, Mirror.NONE, rotation,
                HydraOutpostPlan.PIVOT);
        HydraOutposts.Outpost outpost = new HydraOutposts.Outpost(KEYS.decrementAndGet(), middle.subtract(turned),
                rotation, scale);
        HydraOutposts store = HydraOutposts.get(level);
        store.add(outpost);
        f.onClose(() -> store.remove(outpost.key()));
        return outpost;
    }

    static HydraOutposts.Outpost outpost(TestKit.Fixture f) {
        return outpost(f, new BlockPos(10, 2, 10), Rotation.NONE, SHRINK);
    }

    /** A soldier of {@code rank} at (x, z) of the arena, looking along {@code yaw}. */
    static HydraSoldierEntity soldier(TestKit.Fixture f, Rank rank, double x, double z, float yaw) {
        HydraSoldierEntity soldier = ModEntities.HYDRA_SOLDIER.get().create(f.level());
        soldier.setRank(rank, 0);
        place(f, soldier, x, z, yaw);
        return soldier;
    }

    static HydraOfficerEntity officer(TestKit.Fixture f, double x, double z, float yaw) {
        HydraOfficerEntity officer = ModEntities.HYDRA_OFFICER.get().create(f.level());
        place(f, officer, x, z, yaw);
        return officer;
    }

    private static void place(TestKit.Fixture f, HydraSoldierEntity soldier, double x, double z, float yaw) {
        Vec3 at = f.helper().absoluteVec(new Vec3(x, FLOOR, z));
        soldier.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        soldier.setYHeadRot(yaw);
        soldier.yBodyRot = yaw;
        soldier.yBodyRotO = yaw;
        soldier.yHeadRotO = yaw;
        soldier.setPersistenceRequired();
        f.level().addFreshEntity(soldier);
    }

    /** Makes a soldier the one of post {@code post} of the outpost. */
    static <T extends HydraSoldierEntity> T posted(HydraOutposts.Outpost outpost, int post, T soldier) {
        soldier.belongTo(outpost, post, false);
        setPost(outpost, post, soldier.getUUID());
        return soldier;
    }

    /** Records a post's soldier (as the garrison does when it posts one). */
    private static void setPost(HydraOutposts.Outpost outpost, int post, UUID id) {
        try {
            var method = HydraOutposts.Outpost.class.getDeclaredMethod("setSoldier", int.class, UUID.class);
            method.setAccessible(true);
            method.invoke(outpost, post, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A Survival player who takes no harm (the soldiers' bolts would kill him before the test ends). */
    static ServerPlayer intruder(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = f.player(x, FLOOR, z);
        player.setInvulnerable(true);
        return player;
    }

    /** The bolts of {@code owner} near the arena. */
    static List<HydraBoltEntity> bolts(GameTestHelper helper, Entity owner) {
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(40.0);
        return helper.getLevel().getEntitiesOfClass(HydraBoltEntity.class, box, b -> b.getOwner() == owner);
    }

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The soldiers

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRanksHaveThePlansNumbers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraSoldierEntity rifleman = soldier(f, Rank.RIFLEMAN, 3.5, 3.5, 0.0F);
        HydraSoldierEntity enforcer = soldier(f, Rank.ENFORCER, 5.5, 3.5, 0.0F);
        HydraSoldierEntity sniper = soldier(f, Rank.SNIPER, 7.5, 3.5, 0.0F);
        HydraOfficerEntity officer = officer(f, 9.5, 3.5, 0.0F);
        check(rifleman.getMaxHealth() == 24.0F && rifleman.getArmorValue() == 6, "rifleman 24 / 6");
        check(enforcer.getMaxHealth() == 30.0F && enforcer.getArmorValue() == 10, "enforcer 30 / 10");
        check(sniper.getMaxHealth() == 20.0F && sniper.getArmorValue() == 4, "sniper 20 / 4");
        check(officer.getMaxHealth() == 36.0F && officer.getArmorValue() == 8, "officer 36 / 8");
        check(rifleman.arms() == HydraSoldierEntity.Arms.RIFLE && enforcer.arms() == HydraSoldierEntity.Arms.BATON
                && sniper.arms() == HydraSoldierEntity.Arms.LONG_RIFLE
                && officer.arms() == HydraSoldierEntity.Arms.PISTOL,
                "their weapons");
        check(Rank.RIFLEMAN.xp() == 5 && Rank.ENFORCER.xp() == 5 && Rank.SNIPER.xp() == 8, "experience 5 / 5 / 8");
        check(rifleman.getType().is(ModTags.HYDRA) && officer.getType().is(ModTags.HYDRA), "#enhanced:hydra");
        check(rifleman.getType().getCategory() == MobCategory.MONSTER
                && officer.getType().getCategory() == MobCategory.MONSTER, "monsters (Peaceful, the Snap)");
        check(rifleman.getDisplayName().getContents() instanceof TranslatableContents t
                && t.getKey().equals("entity.enhanced.hydra_soldier.rifleman"), "a rifleman is called one");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRankAndSkinAreSavedByName(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraSoldierEntity sniper = soldier(f, Rank.SNIPER, 3.5, 3.5, 0.0F);
        CompoundTag tag = sniper.saveWithoutId(new CompoundTag());
        check(tag.getString("Rank").equals("sniper"), "saved by name: " + tag.getString("Rank"));
        HydraSoldierEntity copy = ModEntities.HYDRA_SOLDIER.get().create(helper.getLevel());
        copy.load(tag);
        check(copy.rank() == Rank.SNIPER && copy.getMaxHealth() == 20.0F, "loaded back a sniper");
        check(Rank.byName("general") == Rank.RIFLEMAN, "an unknown rank is a rifleman");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aSoldierSeesInFrontNotBehindNorASneakerFarOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // the rule's numbers: 24 in the light, half for a sneaker, three quarters in the dark
        TestKit.assertNear(HydraSenses.reach(24, false, 0.5, false), 24.0, 1.0E-9, "a player at 20 is in reach");
        check(HydraSenses.reach(24, true, 0.5, false) < 14.0, "a sneaker at 14 is out of reach");
        TestKit.assertNear(HydraSenses.reach(24, false, 0.5, true), 18.0, 1.0E-9, "the dark: 18");
        check(HydraSenses.inCone(0.0, Vec3.ZERO, new Vec3(0, 0, 20)), "south is in front of yaw 0");
        check(!HydraSenses.inCone(0.0, Vec3.ZERO, new Vec3(0, 0, -20)), "north is behind it");
        check(!HydraSenses.inCone(0.0, Vec3.ZERO, new Vec3(20, 0, 1)), "east is out of a 120-degree cone");
        // the soldiers themselves (yaw 0 looks south, +z)
        HydraSoldierEntity facing = soldier(f, Rank.RIFLEMAN, 4.5, 2.5, 0.0F);
        HydraSoldierEntity away = soldier(f, Rank.RIFLEMAN, 6.5, 2.5, 180.0F);
        facing.setNoAi(true);
        away.setNoAi(true);
        ServerPlayer near = f.player(5.5, FLOOR, 15.5);
        check(HydraSenses.sees(facing, near), "the one facing him sees him at 13");
        check(!HydraSenses.sees(away, near), "the one facing away does not");
        near.setShiftKeyDown(true);
        check(!HydraSenses.sees(facing, near), "a sneaking player at 13 is not seen");
        near.setShiftKeyDown(false);
        near.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        check(!HydraSenses.sees(facing, near), "nor a Creative one");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 40)
    public static void everySoldierSpotsAnIntruderInFrontOfHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // several in a row: their entity ids are odd and even (the sight check must reach both)
        List<HydraSoldierEntity> row = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            row.add(soldier(f, Rank.RIFLEMAN, 4.5 + i * 4, 3.5, 0.0F));
        }
        ServerPlayer player = intruder(f, 10.5, 15.5);
        f.everyTick(30, tick -> {
            for (HydraSoldierEntity s : row) {
                if (s.getTarget() != player) {
                    return false;
                }
            }
            check(row.stream().map(Entity::getId).anyMatch(id -> id % 2 == 0)
                    && row.stream().map(Entity::getId).anyMatch(id -> id % 2 != 0), "odd and even ids");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 140)
    public static void aRiflemanFiresBurstsOfThreeAndReloadsEverySix(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraSoldierEntity rifleman = soldier(f, Rank.RIFLEMAN, 10.5, 2.5, 0.0F);
        Villager target = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(10.5, FLOOR, 13.5));
        target.setInvulnerable(true);
        rifleman.setTarget(target);
        Map<UUID, Long> seen = new HashMap<>();
        Set<Float> damages = new HashSet<>();
        f.everyTick(130, tick -> {
            rifleman.setTarget(target);
            for (HydraBoltEntity bolt : bolts(helper, rifleman)) {
                if (seen.putIfAbsent(bolt.getUUID(), (long) tick) == null) {
                    damages.add(bolt.damage());
                }
            }
            if (tick < 125) {
                return false;
            }
            List<Long> times = new ArrayList<>(seen.values());
            times.sort(Long::compare);
            check(times.size() >= 6, "two bursts by now: " + times);
            check(times.get(1) - times.get(0) == 5 && times.get(2) - times.get(1) == 5,
                    "three bolts 5 apart: " + times);
            check(times.get(3) - times.get(0) >= 50, "the next burst 50 later: " + times);
            if (times.size() > 6) {
                check(times.get(6) - times.get(5) >= 30, "a reload after six bolts: " + times);
            }
            check(damages.equals(Set.of(4.0F)), "each bolt 4: " + damages);
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aBoltPassesThroughHydraAndHitsWhatIsBehind(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraSoldierEntity shooter = soldier(f, Rank.RIFLEMAN, 3.5, 10.5, -90.0F);
        HydraSoldierEntity friend = soldier(f, Rank.RIFLEMAN, 7.5, 10.5, -90.0F);
        shooter.setNoAi(true);
        friend.setNoAi(true);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 10.5));
        Vec3 from = helper.absoluteVec(new Vec3(5.0, FLOOR + 1.2, 10.5));
        HydraBoltEntity bolt = HydraBoltEntity.create(helper.getLevel(), shooter, from, new Vec3(1, 0, 0), 2.2, 4.0F,
                20.0, HydraBoltEntity.RIFLE);
        helper.getLevel().addFreshEntity(bolt);
        f.everyTick(20, tick -> {
            if (zombie.getHealth() >= zombie.getMaxHealth()) {
                return false;
            }
            check(friend.getHealth() == friend.getMaxHealth(), "the soldier in the way is not hurt");
            check(helper.getBlockState(new BlockPos(11, 2, 10)).isAir(), "and no block broke");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRiotShieldStopsABoltFromTheFrontNotFromBehind(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // two enforcers facing west (yaw 90); a bolt comes from the west at one, from the east at the other
        HydraSoldierEntity front = soldier(f, Rank.ENFORCER, 8.5, 5.5, 90.0F);
        HydraSoldierEntity back = soldier(f, Rank.ENFORCER, 8.5, 15.5, 90.0F);
        front.setNoAi(true);
        back.setNoAi(true);
        HydraBoltEntity a = HydraBoltEntity.create(helper.getLevel(), null, helper.absoluteVec(new Vec3(4.5, 3.2, 5.5)),
                new Vec3(1, 0, 0), 2.2, 4.0F, 20.0, HydraBoltEntity.RIFLE);
        HydraBoltEntity b = HydraBoltEntity.create(helper.getLevel(), null, helper.absoluteVec(new Vec3(12.5, 3.2,
                15.5)), new Vec3(-1, 0, 0), 2.2, 4.0F, 20.0, HydraBoltEntity.RIFLE);
        helper.getLevel().addFreshEntity(a);
        helper.getLevel().addFreshEntity(b);
        check(HydraSoldierEntity.holdsRanged(f.player(1.5, FLOOR, 1.5)) == false, "an empty hand is no bow");
        f.everyTick(20, tick -> {
            if (back.getHealth() >= back.getMaxHealth()) {
                return false;
            }
            check(front.getHealth() == front.getMaxHealth(), "the shield took the bolt from the front");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRealityStoneBubblesABolt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer holder = f.player(5.5, FLOOR, 5.5, Power.THANOS);
        HydraSoldierEntity shooter = soldier(f, Rank.RIFLEMAN, 10.5, 5.5, 90.0F);
        shooter.setNoAi(true);
        HydraBoltEntity bolt = HydraBoltEntity.create(helper.getLevel(), shooter, helper.absoluteVec(
                new Vec3(8.5, 3.0, 5.5)), new Vec3(-1, 0, 0), 2.2, 4.0F, 20.0, HydraBoltEntity.RIFLE);
        helper.getLevel().addFreshEntity(bolt);
        check(!bolt.getType().is(ModTags.REALITY_PROOF), "a bolt is no Gungnir");
        check(StonePowers.realityCatches(holder, bolt) && bolt.isRemoved(), "the Reality Stone turns it to bubbles");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 80)
    public static void theSnipersLaserWarnsThirtyTicksBeforeHisShot(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraSoldierEntity sniper = soldier(f, Rank.SNIPER, 10.5, 1.5, 0.0F);
        Villager target = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(10.5, FLOOR, 18.5));
        target.setInvulnerable(true);
        AtomicLong laserAt = new AtomicLong(-1);
        f.everyTick(75, tick -> {
            sniper.setTarget(target);
            if (laserAt.get() < 0 && sniper.laserTarget() == target.getId()) {
                laserAt.set(tick);
            }
            List<HydraBoltEntity> shots = bolts(helper, sniper);
            if (shots.isEmpty()) {
                return false;
            }
            check(laserAt.get() >= 0, "the laser showed first");
            long warning = tick - laserAt.get();
            check(warning >= 29 && warning <= 32, "30 ticks of warning: " + warning);
            check(shots.get(0).damage() == 8.0F && shots.get(0).kind() == HydraBoltEntity.SNIPER, "one bolt of 8");
            check(sniper.laserTarget() == -1, "the laser is off after the shot");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 80)
    public static void theEnforcersBatonHurtsAndSlows(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraSoldierEntity enforcer = soldier(f, Rank.ENFORCER, 8.5, 10.5, -90.0F);
        Villager target = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(10.0, FLOOR, 10.5));
        f.everyTick(75, tick -> {
            enforcer.setTarget(target);
            if (target.getHealth() >= target.getMaxHealth()) {
                return false;
            }
            TestKit.assertNear(target.getMaxHealth() - target.getHealth(), 6.0, 0.01, "the baton: 6");
            MobEffectInstance slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            check(slow != null && slow.getAmplifier() == 1 && slow.getDuration() <= 20, "Slowness II for 1 s");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hydraSpareHydraButNotATraitor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraSoldierEntity a = soldier(f, Rank.RIFLEMAN, 3.5, 3.5, 0.0F);
        HydraSoldierEntity b = soldier(f, Rank.ENFORCER, 5.5, 3.5, 0.0F);
        HydraOfficerEntity c = officer(f, 7.5, 3.5, 0.0F);
        ServerPlayer loki = f.player(9.5, FLOOR, 9.5, Power.LOKI);
        check(!AreaTargets.mayHurt(a, b, false) && !AreaTargets.mayHurt(c, a, false), "the ally rule");
        check(HydraLoyalty.sameSide(a, c) && a.isAlliedTo(b), "the same side");
        check(HydraLoyalty.turn(b, loki, 200), "the Mind Stone's seam turns him");
        check(AreaTargets.mayHurt(a, b, false) && !HydraLoyalty.sameSide(a, b), "a turned one is fair game to Hydra");
        check(!AreaTargets.mayHurt(loki, b, false), "and spared by his leader's area attacks");
        check(HydraLoyalty.isTraitor(b) && b.turnedBy(loki), "he is a traitor, turned by Loki");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBlasterFiresOneBoltOfFiveForAnyone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(3.5, FLOOR, 3.5);
        ItemStack blaster = new ItemStack(ModItems.HYDRA_BLASTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, blaster);
        TestKit.look(player, 0.0F, 0.0F);
        check(blaster.getMaxDamage() == 96, "96 shots");
        blaster.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        List<HydraBoltEntity> shot = bolts(helper, player);
        check(shot.size() == 1 && shot.get(0).damage() == 5.0F, "one bolt of 5: " + shot.size());
        check(player.getCooldowns().isOnCooldown(blaster.getItem()), "then a rest of 20 ticks");
        check(blaster.getDamageValue() == 1, "a shot spent");
        check(blaster.getItem().isValidRepairItem(blaster, new ItemStack(ModItems.HYDRA_ENERGY_CELL.get()))
                && !blaster.getItem().isValidRepairItem(blaster, new ItemStack(Items.IRON_INGOT)),
                "an energy cell repairs it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theIntelMapLeadsToItsTargetAndTheTagToTheTower(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos tower = helper.absolutePos(new BlockPos(10, 2, 10)).offset(600, 0, -300);
        ItemStack map = HydraIntel.mapTo(level, tower);
        check(map.is(Items.FILLED_MAP), "a filled map");
        check(map.get(DataComponents.ITEM_NAME).getContents() instanceof TranslatableContents t
                && t.getKey().equals("item.enhanced.hydra_intel"), "called Hydra intel");
        MapItemSavedData data = net.minecraft.world.item.MapItem.getSavedData(map, level);
        check(data != null && Math.abs(data.centerX - tower.getX()) <= 256
                && Math.abs(data.centerZ - tower.getZ()) <= 256, "centred near the tower");
        check(map.has(DataComponents.MAP_DECORATIONS) && !map.get(DataComponents.MAP_DECORATIONS).decorations()
                .isEmpty(), "marked with an X");
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceKey<Structure> towerKey = ResourceKey.create(Registries.STRUCTURE, Enhanced.id("avengers_tower"));
        check(structures.getHolder(towerKey).map(h -> h.is(ModTags.ON_AVENGERS_TOWER_MAPS)).orElse(false),
                "the intel's tag holds Avengers Tower");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theOutpostsChestsHoldCellsAndThePillagersStopped(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        for (String chest : List.of("supply", "barracks", "lab")) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                    Enhanced.id("chests/hydra_outpost/" + chest));
            LootTable table = server.reloadableRegistries().getLootTable(key);
            boolean cell = false;
            for (long seed = 0; seed < 200 && !cell; seed++) {
                LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,
                        Vec3.ZERO).create(LootContextParamSets.CHEST);
                cell = table.getRandomItems(params, seed).stream()
                        .anyMatch(s -> s.is(ModItems.HYDRA_ENERGY_CELL.get()));
            }
            check(cell, "the " + chest + " chest can hold an energy cell");
        }
        Structure outpost = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(HydraGarrison.OUTPOST);
        check(outpost != null && outpost.spawnOverrides().containsKey(MobCategory.MONSTER)
                && outpost.spawnOverrides().get(MobCategory.MONSTER).spawns().isEmpty(),
                "no pillagers (no monster at all) spawn in the compound");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theyHaveThoughtsOfTheirOwn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = f.player(3.5, FLOOR, 3.5, Power.PROFESSOR_X);
        HydraSoldierEntity soldier = soldier(f, Rank.RIFLEMAN, 6.5, 3.5, 0.0F);
        HydraOfficerEntity officer = officer(f, 8.5, 3.5, 0.0F);
        soldier.setNoAi(true);
        officer.setNoAi(true);
        long time = helper.getLevel().getGameTime();
        for (LivingEntity e : List.of(soldier, officer)) {
            ThoughtsPayload.Thought thought = Thoughts.thought(charles, e, time);
            String prefix = "thought.enhanced." + (e == officer ? "hydra_officer." : "hydra_soldier.");
            check(thought.text().getContents() instanceof TranslatableContents t && t.getKey().startsWith(prefix),
                    "an open mind with its own thoughts: " + thought.text());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void theOfficerSteadiesHisMenWithResistance(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraOfficerEntity officer = officer(f, 10.5, 10.5, 0.0F);
        officer.setNoAi(true);
        HydraSoldierEntity near = soldier(f, Rank.RIFLEMAN, 14.5, 10.5, 0.0F);
        near.setNoAi(true);
        f.everyTick(50, tick -> {
            MobEffectInstance resistance = near.getEffect(MobEffects.DAMAGE_RESISTANCE);
            return resistance != null && resistance.getAmplifier() == 0;
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void aSoldierHearsABlockBrokenNearHimAndGoesToLook(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraSoldierEntity soldier = soldier(f, Rank.RIFLEMAN, 5.5, 5.5, 0.0F);
        HydraSoldierEntity far = soldier(f, Rank.RIFLEMAN, 18.5, 18.5, 0.0F);
        far.setNoAi(true);
        Vec3 noise = helper.absoluteVec(new Vec3(9.5, FLOOR, 5.5));
        HydraSenses.heard(helper.getLevel(), noise);
        double before = soldier.distanceToSqr(noise);
        f.everyTick(50, tick -> {
            check(far.act() != HydraSoldierEntity.Act.ALERT, "one 18 blocks off heard nothing");
            return soldier.distanceToSqr(noise) < before - 4.0;
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void aTurnedSoldierIsHydraAgainWhenTheHoldFades(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(3.5, FLOOR, 3.5, Power.LOKI);
        HydraSoldierEntity soldier = soldier(f, Rank.RIFLEMAN, 6.5, 3.5, 0.0F);
        soldier.setNoAi(true);
        check(HydraLoyalty.turn(soldier, loki, 20) && soldier.isTurned(), "turned for 20 ticks");
        f.everyTick(50, tick -> {
            if (tick < 25) {
                return false;
            }
            check(!soldier.isTurned() && !HydraLoyalty.isTraitor(soldier) && soldier.mayRaiseAlarm(),
                    "Hydra again");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void aRiflemanButtStrikesWhoComesTooClose(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraSoldierEntity rifleman = soldier(f, Rank.RIFLEMAN, 10.5, 10.5, -90.0F);
        Villager target = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(12.0, FLOOR, 10.5));
        f.everyTick(50, tick -> {
            rifleman.setTarget(target);
            if (target.getHealth() >= target.getMaxHealth()) {
                return false;
            }
            float lost = target.getMaxHealth() - target.getHealth();
            check(Math.abs(lost - 4.0F) < 0.01F || bolts(helper, rifleman).size() > 0, "the butt: 4 (" + lost + ")");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theOfficersPostIsTheCommandRoomByDayTheRadioRoomByNight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraOutposts.Outpost outpost = outpost(f);
        HydraOfficerEntity officer = posted(outpost, Post.OFFICER.ordinal(), officer(f, 10.5, 10.5, 0.0F));
        officer.setNoAi(true);
        BlockPos expected = outpost.where(helper.getLevel().isDay() ? HydraOutpostPlan.COMMAND_POST
                : HydraOutpostPlan.RADIO_POST);
        check(officer.postPos().equals(expected), "his post now: " + officer.postPos() + " not " + expected);
        check(HydraGarrison.spawnPos(helper.getLevel(), outpost, Post.OFFICER).equals(expected), "where he appears");
        check(Post.OFFICER.isOfficer() && Post.values().length == 14 && Post.CONSOLES.size() == 4, "the table");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The garrison

    @GameTest(template = ARENA, batch = GARRISON)
    public static void theGarrisonFillsNinePostsAndNotTwice(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        List<HydraSoldierEntity> came = HydraGarrison.fill(level, outpost, level.getGameTime(), level.getDayTime());
        f.onClose(() -> came.forEach(Entity::discard));
        check(came.size() == 9, "nine posts: " + came.size());
        long officers = came.stream().filter(HydraSoldierEntity::isOfficer).count();
        long snipers = came.stream().filter(s -> !s.isOfficer() && s.rank() == Rank.SNIPER).count();
        long enforcers = came.stream().filter(s -> !s.isOfficer() && s.rank() == Rank.ENFORCER).count();
        check(officers == 1 && snipers == 1 && enforcers == 2, "one officer, one sniper, two enforcers");
        check(came.stream().allMatch(s -> s.outpost() == outpost && s.isPersistenceRequired()), "theirs, kept");
        check(came.stream().filter(HydraSoldierEntity::isPatrolling).count() == 4, "two patrols of two");
        check(HydraGarrison.fill(level, outpost, level.getGameTime(), level.getDayTime()).isEmpty(), "not twice");
        HydraSoldierEntity gate = came.get(Post.GATE_WEST.ordinal());
        check(gate.postPos().equals(outpost.where(HydraOutpostPlan.GATE_POSTS.get(0))), "the gate guard's post");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = GARRISON)
    public static void aDeadPostComesBackAfterThreeDaysOnlyWhileNobodyIsNear(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        long game = level.getGameTime();
        long day = level.getDayTime();
        List<HydraSoldierEntity> came = HydraGarrison.fill(level, outpost, game, day);
        f.onClose(() -> came.forEach(Entity::discard));
        HydraSoldierEntity gate = came.get(0);
        gate.kill();
        check(!gate.isAlive() && outpost.diedGame(0) != HydraOutposts.NEVER, "his death is noted");
        check(!HydraGarrison.due(level, outpost, 0, game + 72000 - 1, day + 72000 - 1), "not before three days");
        ServerPlayer near = f.player(1.5, FLOOR, 1.5);
        check(HydraGarrison.playerNear(level, outpost, 64.0), "a player is near");
        check(!HydraGarrison.due(level, outpost, 0, game + 72000, day + 72000), "not while a player is near");
        level.getServer().getPlayerList().remove(near);
        check(HydraGarrison.due(level, outpost, 0, game + 72000, day), "three days of game time");
        check(HydraGarrison.due(level, outpost, 0, game, day + 72000), "or three days of /time add");
        List<HydraSoldierEntity> back = HydraGarrison.fill(level, outpost, game + 72000, day + 72000);
        f.onClose(() -> back.forEach(Entity::discard));
        check(back.size() == 1 && outpost.soldier(0).equals(back.get(0).getUUID()), "the gate guard is back");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = GARRISON)
    public static void thePostsTurnWithTheOutpostEveryWay(GameTestHelper helper) throws IOException {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos origin = new BlockPos(5000, 70, -3000);
        for (Rotation rotation : Rotation.values()) {
            HydraOutposts.Outpost real = new HydraOutposts.Outpost(1L, origin, rotation, 1.0F);
            for (Post post : Post.values()) {
                BlockPos local = post.spawnPoint();
                check(real.where(local).equals(HydraOutpostPlan.toWorld(local, rotation, origin)),
                        post + " turned " + rotation);
            }
            check(real.box().contains(Vec3.atCenterOf(real.where(HydraOutpostPlan.CENTRE))), "the box holds it");
        }
        // every post and console spot of the garrison is free in the template; every console is an observer
        MinecraftServer server = helper.getLevel().getServer();
        CompoundTag root;
        try (InputStream in = server.getResourceManager().getResourceOrThrow(
                Enhanced.id("structure/hydra_outpost/base.nbt")).open()) {
            root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
        Map<BlockPos, String> blocks = new HashMap<>();
        for (Tag t : root.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            blocks.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    palette.getCompound(b.getInt("state")).getString("Name"));
        }
        for (Post post : Post.values()) {
            if (post.route() == null || !post.route().onSurface()) {
                BlockPos p = post.spawnPoint();
                check(blocks.getOrDefault(p, "minecraft:air").equals("minecraft:air")
                        && blocks.getOrDefault(p.above(), "minecraft:air").equals("minecraft:air"),
                        post + " has room at " + p + ": " + blocks.get(p));
            }
        }
        for (BlockPos spot : Post.CONSOLE_SPOTS) {
            check(blocks.getOrDefault(spot, "minecraft:air").equals("minecraft:air"), "a console's spot " + spot);
        }
        for (BlockPos console : Post.CONSOLES) {
            check("minecraft:observer".equals(blocks.get(console)), "a console at " + console);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = GARRISON)
    public static void theStandInPillagersAreSentAway(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraOutposts.Outpost outpost = outpost(f);
        Pillager pillager = helper.spawn(EntityType.PILLAGER, new Vec3(10.5, FLOOR, 12.5));
        check(HydraGarrison.sendPillagersAway(helper.getLevel(), outpost) >= 1 && pillager.isRemoved(),
                "the pillager is gone");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = GARRISON)
    public static void aSoldierFarFromHisOutpostLetsGoAndGoesBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // his outpost lies 60 blocks off: beyond the leash of 48
        HydraOutposts.Outpost outpost = outpost(f, new BlockPos(70, 2, 10), Rotation.NONE, SHRINK);
        HydraSoldierEntity soldier = posted(outpost, Post.GATE_WEST.ordinal(),
                soldier(f, Rank.RIFLEMAN, 5.5, 5.5, 0.0F));
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(5.5, FLOOR, 9.5));
        soldier.setTarget(villager);
        f.everyTick(10, tick -> tick == 10 && soldier.getTarget() == null);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The alarm

    /** Redstone lamps at the outpost's alarm lamps (shrunken into the arena). */
    static void lamps(GameTestHelper helper, HydraOutposts.Outpost outpost) {
        for (BlockPos lamp : lampsOf(outpost)) {
            helper.getLevel().setBlock(lamp, Blocks.REDSTONE_LAMP.defaultBlockState(), 2);
        }
    }

    /** The lamps' places, but for one that the shrinking put on a radio console's block. */
    static List<BlockPos> lampsOf(HydraOutposts.Outpost outpost) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos lamp : HydraOutpostPlan.ALARM_LAMPS) {
            BlockPos at = outpost.where(lamp);
            if (Post.RADIO_CONSOLES.stream().noneMatch(c -> outpost.where(c).equals(at)) && !out.contains(at)) {
                out.add(at);
            }
        }
        return out;
    }

    static boolean lampsLit(GameTestHelper helper, HydraOutposts.Outpost outpost) {
        for (BlockPos lamp : lampsOf(outpost)) {
            var state = helper.getLevel().getBlockState(lamp);
            if (!state.is(Blocks.REDSTONE_LAMP) || !state.getValue(RedstoneLampBlock.LIT)) {
                return false;
            }
        }
        return true;
    }

    @GameTest(template = ARENA, batch = ALARM, timeoutTicks = 60)
    public static void theOfficerRaisesTheAlarmTheMomentHeSeesAnIntruder(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HydraOutposts.Outpost outpost = outpost(f);
        lamps(helper, outpost);
        HydraOfficerEntity officer = posted(outpost, Post.OFFICER.ordinal(), officer(f, 10.5, 3.5, 0.0F));
        ServerPlayer player = intruder(f, 10.5, 12.5);
        TestKit.look(player, 180.0F, 0.0F);
        f.everyTick(50, tick -> {
            if (!outpost.alarmOn(helper.getLevel().getGameTime())) {
                return false;
            }
            check(officer.getTarget() == player, "he fights the intruder");
            check(tick <= 12, "at once: tick " + tick);
            check(lampsLit(helper, outpost), "the lamps are lit");
            check(outpost.waveAt() > 0, "a wave is called");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = ALARM, timeoutTicks = 260)
    public static void aSoldierRaisesItAfterThreeSecondsOfFightingAtAConsole(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        // the officer lives (in the far corner, without a will: he raises nothing himself)
        HydraOfficerEntity officer = posted(outpost, Post.OFFICER.ordinal(), officer(f, 1.5, 19.5, 0.0F));
        officer.setNoAi(true);
        HydraSoldierEntity rifleman = posted(outpost, Post.GATE_WEST.ordinal(),
                soldier(f, Rank.RIFLEMAN, 10.5, 15.5, 180.0F));
        ServerPlayer player = intruder(f, 10.5, 4.5);
        AtomicLong fightFrom = new AtomicLong(-1);
        f.everyTick(250, tick -> {
            if (fightFrom.get() < 0 && rifleman.getTarget() == player) {
                fightFrom.set(tick);
            }
            boolean on = outpost.alarmOn(level.getGameTime());
            if (fightFrom.get() >= 0 && tick - fightFrom.get() < 60) {
                check(!on, "no alarm in the first 3 s of the fight (tick " + (tick - fightFrom.get()) + ")");
            }
            if (!on) {
                return false;
            }
            check(fightFrom.get() >= 0 && tick - fightFrom.get() >= 60, "after 60 ticks of fighting");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = ALARM, timeoutTicks = 260)
    public static void aSoldierWhoCannotReachAConsoleCallsOnHisOwnRadio(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        HydraOfficerEntity officer = posted(outpost, Post.OFFICER.ordinal(), officer(f, 1.5, 19.5, 0.0F));
        officer.setNoAi(true);
        // a glass cage round him: no way to any console
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                for (int y = 2; y <= 4; y++) {
                    if (x == 1 || x == 5 || z == 1 || z == 5 || y == 4) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.GLASS);
                    }
                }
            }
        }
        HydraSoldierEntity rifleman = posted(outpost, Post.GATE_WEST.ordinal(),
                soldier(f, Rank.RIFLEMAN, 3.5, 3.5, 0.0F));
        ServerPlayer player = intruder(f, 15.5, 15.5);
        AtomicLong radioFrom = new AtomicLong(-1);
        f.everyTick(250, tick -> {
            rifleman.setTarget(player);
            if (radioFrom.get() < 0 && rifleman.act() == HydraSoldierEntity.Act.RADIO) {
                radioFrom.set(tick);
            }
            if (!outpost.alarmOn(level.getGameTime())) {
                return false;
            }
            check(radioFrom.get() >= 60, "he called after 3 s of fighting: " + radioFrom.get());
            check(tick - radioFrom.get() >= HydraAlarm.CALL_TICKS - 4,
                    "and the call took 3 s: " + (tick - radioFrom.get()));
            return true;
        });
    }

    @GameTest(template = ARENA, batch = ALARM, timeoutTicks = 260)
    public static void fourReinforcementsComeAfterTwoHundredTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        check(HydraAlarm.raise(level, outpost, null, null), "raised");
        long raised = level.getGameTime();
        f.onClose(() -> outpost.reinforcements().forEach(id -> {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }));
        f.everyTick(250, tick -> {
            long since = level.getGameTime() - raised;
            if (since < HydraAlarm.WAVE_DELAY) {
                check(outpost.reinforcements().isEmpty(), "none before 200 ticks (" + since + ")");
                return false;
            }
            if (outpost.reinforcements().isEmpty()) {
                return false;
            }
            check(outpost.reinforcements().size() == HydraAlarm.waveSize(level.getDifficulty()) && outpost
                    .reinforcements().size() == 4, "four of them: " + outpost.reinforcements().size());
            int enforcers = 0;
            for (UUID id : outpost.reinforcements()) {
                HydraSoldierEntity s = (HydraSoldierEntity) level.getEntity(id);
                check(s != null && s.isReinforcement() && s.post() < 0, "reinforcements, not posts");
                enforcers += s.rank() == Rank.ENFORCER ? 1 : 0;
            }
            check(enforcers == 1, "riflemen and an enforcer");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = ALARM)
    public static void atMostTwoWavesADay(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        long day = HydraAlarm.day(level);
        check(HydraAlarm.raise(level, outpost, null, null) && outpost.waveAt() > 0, "the first wave");
        HydraAlarm.end(level, outpost);
        check(HydraAlarm.raise(level, outpost, null, null) && outpost.waveAt() > 0, "the second wave");
        HydraAlarm.end(level, outpost);
        check(HydraAlarm.raise(level, outpost, null, null), "a third alarm sounds");
        check(outpost.waveAt() < 0 && outpost.waves(day) == 2, "but brings no third wave");
        HydraAlarm.end(level, outpost);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = ALARM, timeoutTicks = 100)
    public static void holdingUseOnTheRadioConsoleSwitchesItOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        lamps(helper, outpost);
        BlockPos console = outpost.where(Post.RADIO_CONSOLES.get(0));
        level.setBlock(console, Blocks.OBSERVER.defaultBlockState(), 2);
        ServerPlayer player = f.player(1.5, FLOOR, 1.5);
        check(HydraAlarm.raise(level, outpost, null, null) && lampsLit(helper, outpost), "the alarm, the lamps");
        check(!HydraAlarm.useConsole(player, console.above(3)), "elsewhere is no console");
        long start = level.getGameTime();
        f.everyTick(90, tick -> {
            if (tick % 4 == 1) {
                check(HydraAlarm.useConsole(player, console), "the console takes the use");
            }
            long held = level.getGameTime() - start;
            boolean on = outpost.alarmOn(level.getGameTime());
            if (held < HydraAlarm.SILENCE_TICKS - 4) {
                check(on, "still on after " + held + " ticks");
                return false;
            }
            if (on) {
                return false;
            }
            check(!lampsLit(helper, outpost), "the lamps are dark");
            check(outpost.waveAt() < 0, "no wave comes");
            check(!HydraAlarm.raise(level, outpost, null, null), "and it stays off for the day");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = ALARM, timeoutTicks = 260)
    public static void killingTheOfficerEarlyStopsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        // one outpost: the officer raised it and falls before the reinforcements come
        HydraOutposts.Outpost first = outpost(f);
        HydraOfficerEntity officer = posted(first, Post.OFFICER.ordinal(), officer(f, 2.5, 2.5, 0.0F));
        officer.setNoAi(true);
        check(HydraAlarm.raise(level, first, officer, null), "raised");
        officer.kill();
        check(!first.alarmOn(level.getGameTime()) && first.waveAt() < 0, "the alarm stops, no wave");
        // another: no officer, so a soldier's fight raises nothing
        HydraOutposts.Outpost second = outpost(f, new BlockPos(10, 2, 10), Rotation.CLOCKWISE_90, SHRINK);
        HydraSoldierEntity rifleman = posted(second, Post.GATE_EAST.ordinal(),
                soldier(f, Rank.RIFLEMAN, 10.5, 15.5, 180.0F));
        ServerPlayer player = intruder(f, 10.5, 4.5);
        f.everyTick(150, tick -> {
            rifleman.setTarget(player);
            check(!second.alarmOn(level.getGameTime()) && second.runner() == null, "nobody answers the radio");
            return tick == 150;
        });
    }

    @GameTest(template = ARENA, batch = ALARM, timeoutTicks = 60)
    public static void aFrozenOfficerRaisesNoAlarm(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        HydraOfficerEntity officer = posted(outpost, Post.OFFICER.ordinal(), officer(f, 10.5, 3.5, 0.0F));
        ServerPlayer charles = f.player(10.5, FLOOR, 10.5, Power.PROFESSOR_X);
        charles.setInvulnerable(true);
        press(charles, 4); // Freeze Minds, before the officer has looked
        check(officer.isFrozen() && !officer.mayRaiseAlarm(), "Freeze Minds holds him");
        f.everyTick(50, tick -> {
            check(!outpost.alarmOn(level.getGameTime()), "a frozen officer raises nothing");
            check(!HydraAlarm.raise(level, outpost, officer, charles), "not even by hand");
            return tick == 50;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The minds

    @GameTest(template = ARENA, batch = MINDS, timeoutTicks = 100)
    public static void lokisScepterTurnsASoldierWhoThenFightsHydra(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(10.5, FLOOR, 4.5, Power.LOKI);
        loki.setInvulnerable(true);
        HydraSoldierEntity soldier = soldier(f, Rank.RIFLEMAN, 10.5, 6.5, 180.0F);
        HydraOfficerEntity officer = officer(f, 16.5, 6.5, 90.0F);
        officer.setNoAi(true);
        HydraSoldierEntity loyal = soldier(f, Rank.ENFORCER, 10.5, 16.5, 180.0F);
        loyal.setNoAi(true);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.5, FLOOR, 3.5));
        ItemStack scepter = new ItemStack(ModItems.LOKIS_SCEPTER.get());
        scepter.getItem().hurtEnemy(scepter, soldier, loki);
        check(soldier.isTurned() && soldier.turnedBy(loki), "the Scepter turned him");
        check(soldier.turnedTicksLeft() == 1200, "for 1200 ticks: " + soldier.turnedTicksLeft());
        check(!soldier.hasEffect(ModEffects.CONFUSED), "turned, not confused");
        check(HydraLoyalty.onScepterHit(officer, loki) && officer.turnedTicksLeft() == 600, "an officer for half");
        scepter.getItem().hurtEnemy(scepter, zombie, loki);
        check(zombie.hasEffect(ModEffects.CONFUSED), "anything else: the old confusion");
        check(!soldier.mayRaiseAlarm(), "a turned soldier raises no alarm");
        f.everyTick(90, tick -> {
            LivingEntity target = soldier.getTarget();
            if (target == null) {
                return false;
            }
            check(target == loyal || target == officer, "he goes for Hydra: " + target);
            return true;
        });
    }

    @GameTest(template = ARENA, batch = MINDS, timeoutTicks = 60)
    public static void professorXTakesASoldierAndFiresHisRifle(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = f.player(10.5, FLOOR, 3.5, Power.PROFESSOR_X);
        charles.setInvulnerable(true);
        f.onClose(() -> MindLink.end(charles.server, charles.getUUID(), MindLink.End.SERVER));
        HydraSoldierEntity soldier = soldier(f, Rank.RIFLEMAN, 10.5, 7.5, 0.0F);
        HydraSoldierEntity other = soldier(f, Rank.RIFLEMAN, 4.5, 14.5, 90.0F);
        other.setInvulnerable(true);
        TestKit.lookAt(charles, soldier);
        press(charles, 3);
        check(MindLink.isHost(soldier) && HydraLoyalty.isTraitor(soldier), "he is in the soldier");
        MindLink.input(charles, new XavierInputPayload(0.0F, 0.0F, false, false, 0.0F, 0.0F, true));
        PowerTicker.tick(charles);
        f.everyTick(50, tick -> {
            PowerTicker.tick(charles);
            if (bolts(helper, soldier).isEmpty() || other.getTarget() != soldier) {
                return false;
            }
            check(bolts(helper, soldier).get(0).damage() == 4.0F, "his own rifle's bolt");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = MINDS)
    public static void aPossessedOrCommandedSoldierIsATraitor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer phoenix = f.player(3.5, FLOOR, 3.5, Power.PHOENIX);
        HydraSoldierEntity a = soldier(f, Rank.RIFLEMAN, 6.5, 3.5, 0.0F);
        HydraSoldierEntity b = soldier(f, Rank.RIFLEMAN, 8.5, 3.5, 0.0F);
        a.setNoAi(true);
        b.setNoAi(true);
        a.addEffect(new MobEffectInstance(ModEffects.POSSESSED, 200, 0), phoenix);
        check(HydraLoyalty.isTraitor(a) && !HydraLoyalty.sameSide(a, b), "a possessed soldier is not their side");
        check(!HydraLoyalty.turn(helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 3.5)), phoenix,
                100), "the seam turns only Hydra");
        check(HydraLoyalty.turn(b, phoenix, 100) && b.turnedTicksLeft() == 100, "the Mind Stone's command turns him");
        helper.succeed();
    }

    /** The Mind Stone's own Command (lane L2's) turns a soldier for the command's time, as the seam above. */
    @GameTest(template = ARENA, batch = MINDS)
    public static void theMindStonesCommandTurnsASoldier(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = StonePowerTests.holder(f, 3.5, 10.5, Power.THOR, StoneKind.MIND);
        HydraSoldierEntity soldier = soldier(f, Rank.RIFLEMAN, 8.5, 10.5, 90.0F);
        HydraSoldierEntity other = soldier(f, Rank.RIFLEMAN, 14.5, 10.5, 90.0F);
        soldier.setNoAi(true);
        other.setNoAi(true);
        TestKit.lookAt(thor, soldier);
        StonePowerTests.use(thor, StoneKind.MIND, true);
        check(thor.getUUID().equals(MindStonePowers.commanderOf(soldier)), "the soldier is commanded");
        check(HydraLoyalty.isTraitor(soldier) && soldier.turnedBy(thor), "and turned traitor for Thor");
        check(soldier.turnedTicksLeft() == StonesConfig.MIND_COMMAND_TICKS.get(), "for the command's time");
        check(!HydraLoyalty.sameSide(soldier, other) && AreaTargets.mayHurt(other, soldier, false),
                "Hydra's own now count him an enemy");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Config and difficulty (each its own batch)

    @GameTest(template = ARENA, batch = "hydra_cfg")
    public static void anOfficerKilledByAPlayerLeavesTheRareDropsAndOnlyThen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(HydraConfig.BLASTER_DROP, 1.0);
        f.config(HydraConfig.ENERGY_CELL_DROP, 1.0);
        ServerPlayer player = f.player(2.5, FLOOR, 2.5);
        HydraOfficerEntity byPlayer = officer(f, 6.5, 6.5, 0.0F);
        byPlayer.setNoAi(true);
        byPlayer.hurt(player.damageSources().playerAttack(player), 1000.0F);
        HydraSoldierEntity byZombie = soldier(f, Rank.RIFLEMAN, 14.5, 14.5, 0.0F);
        byZombie.setNoAi(true);
        byZombie.hurt(helper.getLevel().damageSources().generic(), 1000.0F);
        check(HydraDrops.cellChance(byZombie) == 1.0, "the cell's chance is the option's");
        f.everyTick(10, tick -> {
            List<ItemEntity> nearPlayer = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    byPlayer.getBoundingBox().inflate(3.0));
            List<ItemEntity> nearZombie = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    byZombie.getBoundingBox().inflate(3.0));
            if (nearPlayer.isEmpty()) {
                return false;
            }
            check(nearPlayer.stream().anyMatch(i -> i.getItem().is(ModItems.HYDRA_ENERGY_CELL.get())), "a cell");
            check(nearPlayer.stream().anyMatch(i -> i.getItem().is(ModItems.HYDRA_BLASTER.get())), "a blaster");
            check(nearZombie.stream().noneMatch(i -> i.getItem().is(ModItems.HYDRA_ENERGY_CELL.get())),
                    "no rare drop without a player's kill");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "hydra_cfg")
    public static void aSwitchedOffBlasterIsDeadMetal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(HydraConfig.BLASTER_ENABLED, false);
        ServerPlayer player = f.player(12.5, FLOOR, 12.5);
        ItemStack blaster = new ItemStack(ModItems.HYDRA_BLASTER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, blaster);
        check(!blaster.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult()
                .consumesAction(), "refused");
        check(bolts(helper, player).isEmpty() && !HydraBlasterItem.enabled(), "no bolt");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "hydra_respawn_never")
    public static void withRespawnNeverAClearedPostStaysEmpty(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(HydraConfig.RESPAWN, HydraConfig.Respawn.NEVER);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        List<HydraSoldierEntity> came = HydraGarrison.fill(level, outpost, level.getGameTime(), level.getDayTime());
        f.onClose(() -> came.forEach(Entity::discard));
        came.get(1).kill();
        long later = level.getGameTime() + 24000L * 30;
        check(!HydraGarrison.due(level, outpost, 1, later, later), "never again");
        check(HydraGarrison.fill(level, outpost, later, later).isEmpty(), "nobody comes back");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "hydra_respawn_always")
    public static void withRespawnAlwaysEveryCheckRefills(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(HydraConfig.RESPAWN, HydraConfig.Respawn.ALWAYS);
        ServerLevel level = helper.getLevel();
        HydraOutposts.Outpost outpost = outpost(f);
        List<HydraSoldierEntity> came = HydraGarrison.fill(level, outpost, level.getGameTime(), level.getDayTime());
        f.onClose(() -> came.forEach(Entity::discard));
        came.get(2).kill();
        List<HydraSoldierEntity> back = HydraGarrison.fill(level, outpost, level.getGameTime(), level.getDayTime());
        f.onClose(() -> back.forEach(Entity::discard));
        check(back.size() == 1, "back at once: " + back.size());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "hydra_cap")
    public static void theDimensionsCapStopsTheGarrison(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        f.config(HydraConfig.MAX_LOADED, HydraGarrison.loaded(level) + 3);
        HydraOutposts.Outpost outpost = outpost(f);
        List<HydraSoldierEntity> came = HydraGarrison.fill(level, outpost, level.getGameTime(), level.getDayTime());
        f.onClose(() -> came.forEach(Entity::discard));
        check(came.size() == 3, "only up to the cap: " + came.size());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "hydra_peaceful", timeoutTicks = 220)
    public static void peacefulHasNoGarrison(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        Difficulty before = level.getDifficulty();
        f.onClose(() -> server.setDifficulty(before, true));
        server.setDifficulty(Difficulty.PEACEFUL, true);
        HydraOutposts.Outpost outpost = outpost(f);
        check(HydraGarrison.fill(level, outpost, level.getGameTime(), level.getDayTime()).isEmpty(), "none");
        check(HydraAlarm.raise(level, outpost, null, null), "an alarm may sound");
        f.everyTick(205, tick -> {
            if (tick < 205) {
                return false;
            }
            check(outpost.reinforcements().isEmpty(), "but no reinforcements come");
            HydraAlarm.end(level, outpost);
            return true;
        });
    }
}
