package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.MagnetPull;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.apocalypse.Horsemen;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Magneto's metal against the bosses and the bodies of metal (3.1, D-M 5, 6; spec 18.3): what his strip and his pull do to
 * a fight and to what is metal through and through. Every test uses {@link TestKit#bossFixture} and has a batch of its own
 * ({@code magneto_boss_*}).
 */
public final class MagnetoBossTests {
    /**
     * How far a Sentry may drift by itself while a pull runs (it floats; a body pushes it): a tug moves it a quarter of a
     * block up and up to a block across in its first tick.
     */
    private static final double STILL = 0.2;

    /**
     * M40a. When the metal is the body itself (a Sentry, an Iron Golem, a Vision player with PvP on), V has nothing to tear
     * off: it says to lift it with R ({@code disarm.metal_body}) and starts no cooldown.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_boss_bodies")
    public static void aMetalBodyHasNothingToStrip(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(true);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        PowerData data = Powers.get(magneto);
        ServerPlayer ultron = f.player(18.5, FLOOR, 18.5, Power.ULTRON);
        UltronSentryEntity sentry = UltronSentryEntity.drone(f.level(), helper.absoluteVec(new Vec3(10.5, FLOOR + 1.0, 4.5)),
                ultron.getUUID(), false, false);
        sentry.setHeld(true);
        f.level().addFreshEntity(sentry);
        IronGolem golem = TestKit.spawnWithNoFreeWill(helper, EntityType.IRON_GOLEM, new Vec3(10.5, FLOOR, 10.5));
        ServerPlayer vision = f.player(10.5, FLOOR, 16.5, Power.VISION);

        for (LivingEntity body : List.<LivingEntity>of(sentry, golem, vision)) {
            float health = body.getHealth();
            lookAt(magneto, body);
            BossTestSupport.lastActionBarKey(magneto);
            AbilityHandler.onKey(magneto, 3, true);
            String said = BossTestSupport.lastActionBarKey(magneto);
            check("message.enhanced.disarm.metal_body".equals(said), name(body) + ": nothing to tear off, the metal is the body (said " + said + ")");
            check(data.isReady(3, helper.getLevel().getGameTime()), name(body) + ": and the strip does not rest");
            check(body.isAlive() && body.getHealth() == health, name(body) + ": nor is it hurt");
        }
        helper.succeed();
    }

    /**
     * M38. Apocalypse's Horsemen are disarmed but never farmed: in his arena, the pull empties War's head, chest and hand
     * and Pestilence's hand, and every piece crumbles (heard breaking): no iron helmet, chestplate, axe or sword is in
     * Magneto's inventory or lies anywhere. An iron ore in the arena's floor stays; Famine is left as he was. V at a fresh
     * War: the same, and nothing falls.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_boss_apocalypse", timeoutTicks = 100)
    public static void theHorsemenAreDisarmedButNeverFarmed(GameTestHelper helper) {
        ApocalypseTestSupport.Duel d = ApocalypseTestSupport.duel(helper, 1, Power.MAGNETO);
        ServerPlayer magneto = d.hero();
        ServerLevel level = d.level();
        Husk war = horseman(d, Horsemen.Kind.WAR, 6.5, 7.5);
        Husk pestilence = horseman(d, Horsemen.Kind.PESTILENCE, 6.5, 13.5);
        Husk famine = horseman(d, Horsemen.Kind.FAMINE, 5.5, 15.5);
        check(war.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && pestilence.getMainHandItem().is(Items.IRON_SWORD),
                "War wears iron, Pestilence holds an iron sword");
        BlockPos ore = new BlockPos(4, 1, 13);
        helper.setBlock(ore, Blocks.IRON_ORE);
        check(BossFights.inAnyArena(level, helper.absolutePos(ore)), "the ore lies in the arena's floor");
        float famineHealth = famine.getHealth();
        List<Object> packets = new ArrayList<>();
        TestKit.sentPackets(magneto);
        MagnetPullTests.sneakR(magneto);

        d.f().everyTick(40, tick -> {
            PowerTicker.tick(magneto);
            packets.addAll(TestKit.sentPackets(magneto));
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(war.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && war.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    && war.getMainHandItem().isEmpty(), "the pull empties War's head, chest and hand");
            check(pestilence.getMainHandItem().isEmpty(), "and Pestilence's hand");
            noHorsemansIron(helper, magneto);
            check(TestKit.soundsIn(packets).stream().anyMatch(sound -> sound.id().equals(SoundEvents.ITEM_BREAK.getLocation())),
                    "he heard their gear crumble");
            check(helper.getBlockState(ore).is(Blocks.IRON_ORE) && magneto.getInventory().countItem(Items.RAW_IRON) == 0,
                    "the arena's ore stays");
            check(famine.isAlive() && famine.getHealth() == famineHealth, "Famine is left as he was");

            Husk fresh = horseman(d, Horsemen.Kind.WAR, 7.5, 10.5);
            Powers.get(magneto).startCooldown(3, level.getGameTime(), 0);
            lookAt(magneto, fresh);
            AbilityHandler.onKey(magneto, 3, true);
            check(fresh.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && fresh.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                    && fresh.getMainHandItem().isEmpty(), "V at a fresh War tears off all he wears");
            noHorsemansIron(helper, magneto);
            return true;
        });
    }

    /**
     * M39. The pull leaves Ultron's arena and the Sokovia island alone: on the ground, an iron ore in the arena's ground
     * stays; on the island, its copy of that ore and the original in the ground below stay; he gets no raw iron, reads
     * {@code pull.protected}, and the fight's Sentry in reach is not tugged.
     */
    @EnhancedTest(template = SokoviaTestSupport.PAD, batch = "magneto_boss_ultron", timeoutTicks = 500)
    public static void thePullLeavesUltronsArenaAndIslandAlone(GameTestHelper helper) {
        islandAndArena(helper, true);
    }

    /** M39 with {@code boss.protectArena} off: the island and its ground stay all the same (every running arena is closed). */
    @EnhancedTest(template = SokoviaTestSupport.PAD, batch = "magneto_boss_ultron_open", timeoutTicks = 500)
    public static void thePullLeavesTheIslandAloneInAnOpenArena(GameTestHelper helper) {
        islandAndArena(helper, false);
    }

    private static void islandAndArena(GameTestHelper helper, boolean protect) {
        TestKit.Fixture f = SokoviaTestSupport.pad(helper);
        if (!protect) {
            f.config(BossConfig.PROTECT_ARENA, false);
        }
        BlockPos ore = new BlockPos(24, SokoviaTestSupport.SURFACE - 1, 21);
        helper.setBlock(ore, Blocks.IRON_ORE);
        ServerPlayer magneto = f.player(SokoviaTestSupport.MIDDLE_X, SokoviaTestSupport.GROUND, SokoviaTestSupport.MIDDLE_Z, Power.MAGNETO);
        UltronTestSupport.tank(magneto);
        UltronFight fight = UltronTestSupport.onTheGround(f, magneto);
        UltronTestSupport.calm(fight);
        UltronSentryEntity sentry = UltronSentryEntity.army(f.level(), magneto.position().add(3.0, 1.0, 2.0), false);
        fight.spawn(sentry);
        fight.refresh();
        sentry.setHeld(true);
        Vec3[] sentryAt = new Vec3[1];
        BlockPos copy = ore.above(SokoviaTestSupport.LIFT);

        SokoviaTestSupport.Script script = new SokoviaTestSupport.Script();
        script.then("a pull on the ground", () -> {
            sentryAt[0] = sentry.position();
            BossTestSupport.lastActionBarKey(magneto);
            MagnetPullTests.sneakR(magneto);
        });
        script.step("it ends", () -> {
            PowerTicker.tick(magneto);
            return !MagnetPull.isPulling(magneto);
        });
        script.then("the arena's ground is left alone", () -> {
            check(helper.getBlockState(ore).is(Blocks.IRON_ORE) && magneto.getInventory().countItem(Items.RAW_IRON) == 0,
                    "the ore in the arena's ground stays, and no raw iron is his");
            check("message.enhanced.pull.protected".equals(BossTestSupport.lastActionBarKey(magneto)), "he reads that it is protected");
            check(MagnetPull.lastTugs(magneto) == 0 && sentry.position().distanceTo(sentryAt[0]) < STILL,
                    "the fight's Sentry is not tugged (tugs " + MagnetPull.lastTugs(magneto) + ", moved " + sentry.position().distanceTo(sentryAt[0]) + ")");
        });
        script.step("he joins the fight", () -> fight.participants().size() == 1);
        script.then("the lift begins", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
        });
        script.step("the island rises", () -> {
            SokoviaTestSupport.client(magneto);
            return fight.phase() == UltronFight.Phase.ISLAND;
        });
        script.then("a pull on the island", () -> {
            check(helper.getBlockState(copy).is(Blocks.IRON_ORE) && helper.getBlockState(ore).is(Blocks.IRON_ORE),
                    "the island carries a copy of the ore, and the ground keeps the original");
            check(magneto.position().distanceTo(Vec3.atCenterOf(helper.absolutePos(copy))) < EnhancedConfig.PULL_RADIUS.get(),
                    "the copy is in the pull's reach");
            sentry.moveTo(magneto.position().add(3.0, 1.0, 2.0));
            sentry.setDeltaMovement(Vec3.ZERO);
            sentryAt[0] = sentry.position();
            Powers.get(magneto).startCooldown(1, f.level().getGameTime(), 0);
            BossTestSupport.lastActionBarKey(magneto);
            MagnetPullTests.sneakR(magneto);
        });
        script.step("it ends", () -> {
            PowerTicker.tick(magneto);
            return !MagnetPull.isPulling(magneto);
        });
        script.then("the island and its ground are left alone", () -> {
            check(helper.getBlockState(copy).is(Blocks.IRON_ORE), "the island's copy of the ore stays");
            check(helper.getBlockState(ore).is(Blocks.IRON_ORE), "and so does the original in the ground below");
            check(magneto.getInventory().countItem(Items.RAW_IRON) == 0, "no raw iron is his");
            check("message.enhanced.pull.protected".equals(BossTestSupport.lastActionBarKey(magneto)), "he reads that it is protected");
            check(MagnetPull.lastTugs(magneto) == 0 && sentry.position().distanceTo(sentryAt[0]) < STILL,
                    "the fight's Sentry is not tugged (tugs " + MagnetPull.lastTugs(magneto) + ", moved " + sentry.position().distanceTo(sentryAt[0]) + ")");
        });
        script.run(f, 480);
    }

    /**
     * M53. A fight keeps a dead fighter's drops from the pull: in Ultron's arena a fighter dies with an iron ingot; Magneto,
     * 4 blocks away, pulls: the ingot still lies where it fell (even with no pickup delay left), the arena's iron block
     * stands, and the Sentry in reach is not tugged.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_boss_drops", timeoutTicks = 100)
    public static void aFightKeepsItsDropsFromThePull(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer fighter = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z);
        UltronFight fight = UltronTestSupport.onTheGround(f, fighter);
        UltronTestSupport.calm(fight);
        ServerPlayer magneto = f.player(UltronTestSupport.MIDDLE.x - 4.0, FLOOR, UltronTestSupport.MIDDLE.z, Power.MAGNETO);
        UltronTestSupport.tank(magneto);
        TestKit.look(magneto, TestKit.EAST, 0.0F);
        BlockPos block = new BlockPos(8, 1, 10);
        helper.setBlock(block, Blocks.IRON_BLOCK);
        UltronSentryEntity sentry = UltronTestSupport.sentryAt(f, fight, UltronTestSupport.MIDDLE.x - 4.0, UltronTestSupport.MIDDLE.z + 4.0);
        sentry.setHeld(true);
        ItemEntity[] kept = new ItemEntity[1];
        Vec3[] keptAt = new Vec3[1];
        Vec3[] sentryAt = new Vec3[1];
        int[] stage = {0};

        f.everyTick(80, tick -> {
            if (tick == 3) {
                fighter.getInventory().add(new ItemStack(Items.IRON_INGOT));
                fighter.kill();
                List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(fighter.blockPosition()).inflate(5),
                        item -> item.getItem().is(Items.IRON_INGOT));
                check(drops.size() == 1 && BossFights.isKeptDrop(drops.get(0)), "the fight keeps the dead fighter's ingot");
                kept[0] = drops.get(0);
                return false;
            }
            if (tick < 3) {
                return false;
            }
            PowerTicker.tick(magneto);
            if (stage[0] == 0) {
                if (!kept[0].onGround() || kept[0].getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                    return false; // it falls and slides, then lies still
                }
                kept[0].setNoPickUpDelay(); // only the fight's keeping holds it now
                keptAt[0] = kept[0].position();
                sentryAt[0] = sentry.position();
                BossTestSupport.lastActionBarKey(magneto);
                MagnetPullTests.sneakR(magneto);
                stage[0] = 1;
                return false;
            }
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(kept[0].isAlive() && kept[0].position().distanceTo(keptAt[0]) < 0.01 && magneto.getInventory().countItem(Items.IRON_INGOT) == 0,
                    "the kept ingot still lies where it fell");
            check(helper.getBlockState(block).is(Blocks.IRON_BLOCK) && magneto.getInventory().countItem(Items.IRON_BLOCK) == 0,
                    "the arena's iron block stands");
            check("message.enhanced.pull.protected".equals(BossTestSupport.lastActionBarKey(magneto)), "he reads that it is protected");
            check(MagnetPull.lastTugs(magneto) == 0 && sentry.position().distanceTo(sentryAt[0]) < STILL, "the Sentry in reach is not tugged");
            return true;
        });
    }

    /**
     * The claims at an arena's edge: someone's claim holds everything round a protected arena whose round edge cuts
     * through a chunk section. A husk in the arena, in iron, loses its helmet to the pull (an arena guards its blocks,
     * not what fights on them), while an iron ingot just outside the arena, in the same section, stays: what the arena
     * answered for the husk is not kept for the section, so the ingot is still asked the claim. He reads that the rest
     * is protected.
     */
    @EnhancedTest(template = ARENA, batch = "magneto_boss_claims", timeoutTicks = 60)
    public static void anArenasEdgeOpensNoClaim(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerLevel level = f.level();
        // The longest row of floor cells (along x) in one section: the husk at its west end, the ingot 6 blocks east.
        int originX = helper.absolutePos(BlockPos.ZERO).getX();
        int start = 1;
        int rowStart = 1;
        int rowLength = 0;
        for (int x = 1; x <= 19; x++) {
            if (SectionPos.blockToSectionCoord(originX + x) != SectionPos.blockToSectionCoord(originX + x - 1)) {
                start = x;
            }
            if (x - start + 1 > rowLength) {
                rowStart = start;
                rowLength = x - start + 1;
            }
        }
        check(rowLength >= 8, "eight floor cells in a row lie in one section");
        double huskX = rowStart + 0.5;

        Vec3 centre = helper.absoluteVec(new Vec3(huskX, FLOOR, 10.5));
        BossTestSupport.TestArena arena = new BossTestSupport.TestArena(level, centre, 4.0, Containment.NONE, true);
        arena.containAt(3.0).joinWithin(5.0).tractorDepth(0.0);
        BossFrameworkTests.start(f, BossKind.ULTRON, f.player(huskX, FLOOR, 8.5),
                new BossTestSupport.TestFight(level, BossKind.ULTRON, arena));
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(huskX, FLOOR, 10.5));
        husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Vec3 at = helper.absoluteVec(new Vec3(huskX + 6.0, FLOOR, 10.5));
        ItemEntity ingot = new ItemEntity(level, at.x, at.y, at.z, new ItemStack(Items.IRON_INGOT), 0.0, 0.0, 0.0);
        level.addFreshEntity(ingot);
        check(BossFights.protects(level, husk.getOnPos()) && !BossFights.protects(level, ingot.getOnPos()),
                "the husk stands in the protected arena, the ingot just outside it");
        check(SectionPos.asLong(husk.blockPosition()) == SectionPos.asLong(ingot.blockPosition()), "both lie in one section");

        // Someone's claim on everything here: the arena's own rule answers first inside it.
        AABB bounds = helper.getBounds();
        int[] asked = {0};
        Consumer<BlockEvent.BreakEvent> claim = event -> {
            if (bounds.contains(Vec3.atCenterOf(event.getPos()))) {
                asked[0]++;
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, claim);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(claim));
        ServerPlayer magneto = f.player(huskX + 5.0, FLOOR, 14.5, Power.MAGNETO);
        check(!arena.contains(magneto.position()), "Magneto stands outside the arena");
        BossTestSupport.lastActionBarKey(magneto);
        MagnetPullTests.sneakR(magneto);

        f.everyTick(50, tick -> {
            PowerTicker.tick(magneto);
            if (MagnetPull.isPulling(magneto)) {
                return false;
            }
            check(husk.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && magneto.getInventory().countItem(Items.IRON_HELMET) == 1,
                    "the husk in the arena loses its iron helmet to him");
            check(ingot.isAlive() && magneto.getInventory().countItem(Items.IRON_INGOT) == 0,
                    "the ingot outside the arena, in the same section, stays on the claim");
            check(asked[0] > 0, "the claim was asked for it");
            check("message.enhanced.pull.done_protected".equals(BossTestSupport.lastActionBarKey(magneto)),
                    "he reads that the rest is protected");
            return true;
        });
    }

    /** A Horseman of the duel's fight, standing still at (x, z) of the floor. */
    private static Husk horseman(ApocalypseTestSupport.Duel d, Horsemen.Kind kind, double x, double z) {
        Husk husk = d.fight().horsemen().spawn(d.level(), kind, d.f().helper().absoluteVec(new Vec3(x, FLOOR, z)));
        husk.setNoAi(true);
        return husk;
    }

    /** No piece of a Horseman's iron is in Magneto's inventory or lies anywhere in the arena. */
    private static void noHorsemansIron(GameTestHelper helper, ServerPlayer magneto) {
        for (Item item : List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_AXE, Items.IRON_SWORD)) {
            check(magneto.getInventory().countItem(item) == 0 && helper.getEntities(EntityType.ITEM).stream().noneMatch(e -> e.getItem().is(item)),
                    "no " + BuiltInRegistries.ITEM.getKey(item) + " came of a Horseman");
        }
    }

    private static String name(Entity entity) {
        return entity.getType().getDescriptionId();
    }

    private MagnetoBossTests() {
    }
}
