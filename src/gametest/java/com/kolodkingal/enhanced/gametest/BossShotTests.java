package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.Metal;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StonePowers;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosMeteorEntity;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosOrbEntity;
import com.kolodkingal.enhanced.thanos.ThanosPhantomEntity;
import com.kolodkingal.enhanced.thanos.ThanosState;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * What a boss fight throws, in the hands of telekinesis (3.1, D-T 6.1, 6.2.1, 6.3 and 10): which of the bosses and their
 * shots the tags leave to telekinesis and which the villains' own pulls pass by; Thanos's sword, meteor and orb caught,
 * still in the grip, and thrown back at him; Ultron's bolts thrown back at his side, which they no longer spare.
 */
public final class BossShotTests {

    /**
     * T23 (D-T 10, R37): the three villains and Thanos's sword, meteor and orb left {@code telekinesis_immune} for {@code
     * pull_immune}, the tag the villain heroes' pulls read; the Wither, the Warden and the Elder Guardian are held as bosses
     * and the pulls still pass them by; what no telekinesis lifts (the Ender Dragon, a fragment of Mjolnir, the Hex's
     * Vision, Apocalypse's cocoon, the Vibranium Key) is in both. A Thanos stone's grasp still refuses every boss.
     */
    @EnhancedTest(template = ARENA, batch = "boss_shot_tags")
    public static void bossTagsAreSplit(GameTestHelper helper) {
        TestKit.fixture(helper).onClose(Telekinesis::clear);
        for (EntityType<?> type : List.of(ModEntities.ULTRON.get(), ModEntities.THANOS.get(), ModEntities.APOCALYPSE.get(),
                ModEntities.THANOS_SWORD.get(), ModEntities.THANOS_METEOR.get(), ModEntities.THANOS_ORB.get())) {
            check(type.is(ModTags.PULL_IMMUNE), EntityType.getKey(type) + " is in pull_immune: the villains' pulls pass it by");
            check(!type.is(ModTags.TELEKINESIS_IMMUNE), EntityType.getKey(type) + " is not in telekinesis_immune any more");
            check(!type.is(ModTags.HELD_AS_BOSSES), EntityType.getKey(type) + " is not a vanilla boss");
        }
        for (EntityType<?> type : List.of(EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN)) {
            check(type.is(ModTags.HELD_AS_BOSSES), EntityType.getKey(type) + " is held as a boss");
            check(type.is(ModTags.PULL_IMMUNE), EntityType.getKey(type) + " is in pull_immune, as it was in 3.0");
            check(!type.is(ModTags.TELEKINESIS_IMMUNE), EntityType.getKey(type) + " is not in telekinesis_immune any more");
        }
        for (EntityType<?> type : List.of(EntityType.ENDER_DRAGON, ModEntities.MJOLNIR_FRAGMENT.get(), ModEntities.HEX_VISION.get(),
                ModEntities.SAND_COCOON.get(), ModEntities.VIBRANIUM_KEY.get())) {
            check(type.is(ModTags.TELEKINESIS_IMMUNE) && type.is(ModTags.PULL_IMMUNE),
                    EntityType.getKey(type) + " is in telekinesis_immune and in pull_immune");
            check(!type.is(ModTags.HELD_AS_BOSSES), EntityType.getKey(type) + " is not held as a boss");
        }

        // The stones' grasp (the Thanos hero's Space Pull, the boss's Choke and grabs) reads pull_immune: no boss.
        ServerLevel level = helper.getLevel();
        for (EntityType<? extends LivingEntity> type : List.<EntityType<? extends LivingEntity>>of(ModEntities.ULTRON.get(),
                ModEntities.THANOS.get(), ModEntities.APOCALYPSE.get(), EntityType.WITHER, EntityType.WARDEN, EntityType.ELDER_GUARDIAN)) {
            LivingEntity boss = type.create(level);
            check(boss != null && !StonePowers.graspable(boss), EntityType.getKey(type) + " cannot be grasped by a stone");
        }
        LivingEntity zombie = EntityType.ZOMBIE.create(level);
        check(zombie != null && StonePowers.graspable(zombie), "a zombie can");
        helper.succeed();
    }

    /**
     * R8 (D-T 4.3, D-M 3.5): Thanos's Double-Edged Sword is metal to Magneto, and a grip holds it {@code
     * thanos_boss.swordHoldTicks} (40) at most, Billy's and the Phoenix's alike (Billy's factor is for the bosses only),
     * Magneto's 1.5 times as long (60): on metal, his grip is the stronger. A shot is no boss: nothing keeps it from the
     * next grip.
     */
    @EnhancedTest(template = ARENA, batch = "boss_shot_hold")
    public static void magnetoHoldsTheSwordLonger(GameTestHelper helper) {
        TestKit.fixture(helper).onClose(Telekinesis::clear);
        ThanosSwordEntity sword = ModEntities.THANOS_SWORD.get().create(helper.getLevel());
        check(sword != null && Metal.isMagnetic(sword), "the Double-Edged Sword is metal to Magneto");
        check(BossGrip.holdLimit(sword, Telekinesis.Mode.BILLY) == 40 && BossGrip.holdLimit(sword, Telekinesis.Mode.JEAN) == 40,
                "Billy and the Phoenix hold it 40 ticks: " + BossGrip.holdLimit(sword, Telekinesis.Mode.BILLY));
        check(BossGrip.holdLimit(sword, Telekinesis.Mode.MAGNETO) == 60, "Magneto 60: " + BossGrip.holdLimit(sword, Telekinesis.Mode.MAGNETO));
        check(!BossGrip.isBoss(sword) && BossGrip.regripLeft(sword) == 0, "a shot has no regrip of its own");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Presses the key of an ability, with Sneak held or not. */
    private static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /** A zombie that stands still and does not burn in the sun (the tests run by day). */
    private static Zombie stillZombie(GameTestHelper helper, double x, double y, double z) {
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(x, y, z));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        return zombie;
    }

    /** A wall of stone across the floor, {@code x} the column it stands in (test coordinates), 6 blocks high. */
    private static void wallAt(GameTestHelper helper, int x, int fromZ, int toZ) {
        for (int z = fromZ; z <= toZ; z++) {
            for (int y = 2; y < 8; y++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
    }

    /** The same, across the floor the other way: {@code z} the row it stands in. */
    private static void wallAlong(GameTestHelper helper, int z, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) {
            for (int y = 2; y < 8; y++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
            }
        }
    }

    /**
     * A meteor of Thanos's fight that would land at {@code landing} (test coordinates), put at {@code at} on its way down
     * (where a test catches it at once).
     */
    private static ThanosMeteorEntity meteor(TestKit.Fixture f, ThanosEntity thanos, Vec3 landing, Vec3 at) {
        ThanosMeteorEntity meteor = new ThanosMeteorEntity(ModEntities.THANOS_METEOR.get(), f.level());
        meteor.launch(thanos, f.helper().absoluteVec(landing));
        Vec3 there = f.helper().absoluteVec(at);
        meteor.setPos(there.x, there.y, there.z);
        BossFights.fightOf(thanos).spawn(meteor);
        return meteor;
    }

    /** An orb of Thanos's fight at {@code at} (test coordinates), flying along {@code direction}. */
    private static ThanosOrbEntity orb(TestKit.Fixture f, ThanosEntity thanos, Vec3 at, Vec3 direction) {
        ThanosOrbEntity orb = new ThanosOrbEntity(ModEntities.THANOS_ORB.get(), f.level());
        orb.launch(thanos, f.helper().absoluteVec(at), direction);
        BossFights.fightOf(thanos).spawn(orb);
        return orb;
    }

    /** The thrown sword of Thanos, or null if it is in his hand. */
    private static ThanosSwordEntity sword(ThanosEntity thanos) {
        List<ThanosSwordEntity> swords = thanos.level().getEntitiesOfClass(ThanosSwordEntity.class, thanos.getBoundingBox().inflate(40.0),
                sword -> !sword.isRemoved());
        return swords.isEmpty() ? null : swords.get(0);
    }

    /** Every hurt of a telekinetic slam that reaches {@code target} (counted until the test ends). */
    private static List<DamageSource> slamsOn(TestKit.Fixture f, Entity target) {
        List<DamageSource> slams = new ArrayList<>();
        Consumer<LivingIncomingDamageEvent> listener = event -> {
            if (event.getEntity() == target && event.getSource().is(ModDamageTypes.TELEKINETIC_SLAM)) {
                slams.add(event.getSource());
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, LivingIncomingDamageEvent.class, listener);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(listener));
        return slams;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Thanos's shots (D-T 6.2.1)

    /**
     * T20 (D-T 6.2.1, {@code thanos_boss.swordHoldTicks} 20 here): Thanos throws his sword at Billy, who catches it in
     * flight. In the grip it is still; after 20 ticks it tears free ({@code sword_slips}) and turns home. Billy catches it
     * again and throws it at once at Thanos: it strikes him as Billy's telekinetic slam and a plate tears; then it turns at
     * the wall behind him and comes home harming nobody (a zombie put on its way is untouched), into his hand.
     */
    @EnhancedTest(template = ARENA, batch = "boss_shot_sword", timeoutTicks = 200)
    public static void theSwordCaughtTearsFreeOrIsThrownAtHim(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(ThanosBossConfig.SWORD_HOLD_TICKS, 20);
        ServerPlayer billy = ThanosMoveSupport.fighter(f, -7.0, 0.0, Power.BILLY);
        ThanosEntity thanos = ThanosBossTests.boss(f, billy);
        wallAt(helper, 17, 5, 16); // behind him: a sword thrown at him turns there
        int plates = Integer.bitCount(thanos.plates());
        List<DamageSource> slams = slamsOn(f, thanos);
        thanos.forceMove(ThanosMove.SWORD_THROW, billy);
        int[] caughtAt = {-1};
        ThanosSwordEntity[] held = {null};
        boolean[] thrown = {false};
        Zombie[] bystander = {null};
        float[] health = {0.0F};
        boolean[] passed = {false};
        f.everyTick(190, tick -> {
            PowerTicker.tick(billy);
            if (caughtAt[0] < 0) {
                ThanosSwordEntity sword = sword(thanos);
                check(tick < 80, "Thanos throws his sword");
                if (sword == null || sword.position().subtract(thanos.position()).horizontalDistance() < 3.0) {
                    return false;
                }
                lookAt(billy, sword);
                press(billy, 3, false);
                check(Telekinesis.holds(billy, sword) && !sword.isReturning(), "Billy catches the sword in flight");
                held[0] = sword;
                caughtAt[0] = tick;
                return false;
            }
            ThanosSwordEntity sword = held[0];
            int since = tick - caughtAt[0];
            if (since < 20) {
                check(Telekinesis.holds(billy, sword) && !sword.isReturning() && thanos.swordAway(),
                        "in his grip it is still, and away from Thanos (tick " + since + ")");
                return false;
            }
            if (since == 20) {
                check(!Telekinesis.isHolding(billy) && ThanosSwordEntity.SLIPS.equals(lastActionBarKey(billy)),
                        "after swordHoldTicks it tears free, and Billy reads so");
                check(sword.isReturning(), "and flies home");
                lookAt(billy, sword);
                press(billy, 3, false);
                check(Telekinesis.holds(billy, sword) && !sword.isReturning(), "Billy catches it again on its way home");
                lookAt(billy, thanos);
                press(billy, 3, false);
                check(!Telekinesis.isHolding(billy) && sword.isTurned(), "and throws it at once at Thanos");
                health[0] = billy.getHealth();
                thrown[0] = true;
                return false;
            }
            check(billy.getHealth() == health[0], "the sword never hurts its thrower (tick " + since + ")");
            if (bystander[0] == null) {
                if (!sword.isReturning()) {
                    check(since < 40, "the sword turns at the wall");
                    return false;
                }
                check(!slams.isEmpty() && slams.get(0).getEntity() == billy && slams.get(0).getDirectEntity() == sword,
                        "on its way out it struck Thanos, as Billy's telekinetic slam");
                check(Integer.bitCount(thanos.plates()) == plates - 1, "and tore a plate: " + Integer.bitCount(thanos.plates()));
                // Halfway home, in its way: a zombie floating where the sword passes.
                Vec3 middle = sword.position().add(thanos.swordHandPosition()).scale(0.5);
                Zombie zombie = stillZombie(helper, 0.0, FLOOR, 0.0);
                zombie.setNoGravity(true);
                zombie.moveTo(middle.x, middle.y - 1.0, middle.z);
                bystander[0] = zombie;
                return false;
            }
            Zombie zombie = bystander[0];
            check(zombie.getHealth() == zombie.getMaxHealth(), "on its way home it harms nobody (tick " + since + ")");
            if (!sword.isRemoved() && sword.getBoundingBox().intersects(zombie.getBoundingBox().inflate(0.5))) {
                passed[0] = true;
            }
            if (thanos.swordAway()) {
                check(since < 120, "the sword comes home");
                return false;
            }
            check(passed[0], "it went through the zombie on its way");
            check(sword.isRemoved(), "it is in his hand again");
            zombie.discard();
            return true;
        });
    }

    /**
     * T21 (D-T 6.2.1): what the Phoenix and Billy catch of Thanos's is theirs when they throw it. A meteor the Phoenix
     * throws at Thanos bursts on him as her telekinetic slam, she is not hurt, and no block of the floor or around him
     * changes; an orb Billy throws at Thanos staggers him, one at a Phantom of his dissolves it, and one at a zombie
     * petrifies it.
     */
    @EnhancedTest(template = ARENA, batch = "boss_shot_meteor", timeoutTicks = 120)
    public static void aCaughtMeteorOrOrbIsTheThrowers(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerLevel level = helper.getLevel();
        ServerPlayer phoenix = ThanosMoveSupport.fighter(f, -7.0, 0.0, Power.PHOENIX);
        ServerPlayer billy = ThanosMoveSupport.fighter(f, 0.0, -7.0, Power.BILLY);
        ThanosEntity thanos = ThanosBossTests.boss(f, phoenix);
        List<DamageSource> slams = slamsOn(f, thanos);

        // Every block round him, to compare after the burst.
        List<BlockPos> cells = new ArrayList<>();
        List<BlockState> before = new ArrayList<>();
        for (BlockPos cell : BlockPos.betweenClosed(thanos.blockPosition().offset(-6, -1, -6), thanos.blockPosition().offset(6, 4, 6))) {
            cells.add(cell.immutable());
            before.add(level.getBlockState(cell));
        }
        ThanosMeteorEntity meteor = meteor(f, thanos, new Vec3(6.5, FLOOR, 10.5), new Vec3(6.5, FLOOR + 1.0, 10.5));
        lookAt(phoenix, meteor);
        press(phoenix, 3, false);
        check(Telekinesis.holds(phoenix, meteor), "the Phoenix catches a meteor");
        lookAt(phoenix, thanos);
        press(phoenix, 3, false);
        check(!Telekinesis.isHolding(phoenix) && meteor.isTurned() && meteor.getOwner() == phoenix, "and throws it at Thanos: it is hers");
        check(BossFights.fightOf(meteor) == null, "it left his fight");

        int[] stage = {0};
        int[] stageAt = {0};
        ThanosPhantomEntity[] phantom = {null};
        Zombie[] zombie = {null};
        ThanosOrbEntity[] orb = {null};
        f.everyTick(110, tick -> {
            PowerTicker.tick(phoenix);
            PowerTicker.tick(billy);
            check(tick - stageAt[0] < 30, "stage " + stage[0] + " is over in time");
            switch (stage[0]) {
                case 0 -> {
                    if (!meteor.isRemoved()) {
                        return false;
                    }
                    check(!slams.isEmpty() && slams.get(0).getEntity() == phoenix && slams.get(0).getDirectEntity() == meteor,
                            "it burst on Thanos as her telekinetic slam");
                    check(phoenix.getHealth() == ThanosMoveSupport.MAX, "she is not hurt");
                    for (int i = 0; i < cells.size(); i++) {
                        check(level.getBlockState(cells.get(i)) == before.get(i), "no block changed: " + cells.get(i));
                    }
                    // An orb in front of Billy, which he catches and throws at Thanos.
                    orb[0] = orb(f, thanos, new Vec3(10.5, FLOOR + 1.2, 6.0), new Vec3(0.0, 0.0, -1.0));
                    lookAt(billy, orb[0]);
                    press(billy, 3, false);
                    check(Telekinesis.holds(billy, orb[0]), "Billy catches an orb");
                    lookAt(billy, thanos);
                    press(billy, 3, false);
                    check(!Telekinesis.isHolding(billy) && orb[0].isTurned(), "and throws it at Thanos");
                    stage[0] = 1;
                    stageAt[0] = tick;
                }
                case 1 -> {
                    if (thanos.state() != ThanosState.STAGGERED) {
                        return false;
                    }
                    check(orb[0].isRemoved() && !thanos.hasEffect(ModEffects.PETRIFIED), "the orb staggered Thanos (he is never petrified)");
                    // A Phantom of his, to the east of Billy.
                    ThanosPhantomEntity illusion = ModEntities.THANOS_PHANTOM.get().create(level);
                    check(illusion != null, "a Phantom is made");
                    Vec3 at = helper.absoluteVec(new Vec3(17.5, FLOOR, 3.5));
                    illusion.moveTo(at.x, at.y, at.z);
                    illusion.setNoAi(true);
                    BossFights.fightOf(thanos).spawn(illusion);
                    phantom[0] = illusion;
                    orb[0] = orb(f, thanos, new Vec3(13.5, FLOOR + 1.2, 3.5), new Vec3(-1.0, 0.0, 0.0));
                    lookAt(billy, orb[0]);
                    press(billy, 3, false);
                    check(Telekinesis.holds(billy, orb[0]), "Billy catches another orb");
                    lookAt(billy, illusion);
                    press(billy, 3, false);
                    check(!Telekinesis.isHolding(billy), "and throws it at a Phantom");
                    stage[0] = 2;
                    stageAt[0] = tick;
                }
                case 2 -> {
                    if (!phantom[0].isRemoved()) {
                        return false;
                    }
                    check(orb[0].isRemoved(), "the Phantom dissolved at its touch");
                    zombie[0] = stillZombie(helper, 3.5, FLOOR, 3.5);
                    orb[0] = orb(f, thanos, new Vec3(7.5, FLOOR + 1.2, 3.5), new Vec3(1.0, 0.0, 0.0));
                    lookAt(billy, orb[0]);
                    press(billy, 3, false);
                    check(Telekinesis.holds(billy, orb[0]), "Billy catches a third orb");
                    lookAt(billy, zombie[0]);
                    press(billy, 3, false);
                    check(!Telekinesis.isHolding(billy), "and throws it at a zombie");
                    stage[0] = 3;
                    stageAt[0] = tick;
                }
                default -> {
                    if (!zombie[0].hasEffect(ModEffects.PETRIFIED)) {
                        return false;
                    }
                    check(orb[0].isRemoved(), "the zombie is petrified");
                    zombie[0].discard();
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * T51 (D-T 6.2.1, {@code thanos_boss.swordHoldTicks} 100 here): a caught shot is still in the hand. A meteor the Phoenix
     * holds 2 blocks and more below where it would have landed, for 40 ticks, does not burst, hurts nobody near and deals
     * no {@code stone_blast}; let go without a throw, it is gone at once and hurts nobody. The sword Billy catches as it
     * flies home stays where his grip holds it for 20 ticks, still away from Thanos's hand, and a zombie standing in it is
     * not cut. An orb he holds against a zombie for 20 ticks petrifies nothing.
     */
    @EnhancedTest(template = ARENA, batch = "boss_shot_still", timeoutTicks = 260)
    public static void aCaughtShotIsStillInTheHand(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(ThanosBossConfig.SWORD_HOLD_TICKS, 100);
        ServerPlayer phoenix = ThanosMoveSupport.fighter(f, -7.0, 6.0, Power.PHOENIX);
        ServerPlayer billy = ThanosMoveSupport.fighter(f, -6.0, -6.0, Power.BILLY);
        ThanosEntity thanos = ThanosBossTests.boss(f, phoenix);
        int[] blasts = {0};
        Consumer<LivingIncomingDamageEvent> listener = event -> {
            if (event.getSource().is(ModDamageTypes.STONE_BLAST)) {
                blasts[0]++;
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, LivingIncomingDamageEvent.class, listener);
        f.onClose(() -> NeoForge.EVENT_BUS.unregister(listener));

        // The meteor: caught, and held below where it would land.
        double landingY = helper.absoluteVec(new Vec3(0.0, FLOOR + 4.0, 0.0)).y;
        ThanosMeteorEntity meteor = meteor(f, thanos, new Vec3(7.5, FLOOR + 4.0, 16.5), new Vec3(7.5, FLOOR + 5.0, 16.5));
        lookAt(phoenix, meteor);
        press(phoenix, 3, false);
        check(Telekinesis.holds(phoenix, meteor), "the Phoenix catches a meteor");
        look(phoenix, TestKit.EAST, 0.0F); // level: her grip brings it down to her eyes, far below where it would land
        Zombie near = stillZombie(helper, 9.5, FLOOR, 18.5);

        // The sword, once he has thrown it at a decoy north of him and it turned at the wall.
        Zombie decoy = stillZombie(helper, 10.5, FLOOR, 3.5);
        decoy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        decoy.setHealth(1000.0F);
        wallAlong(helper, 1, 7, 14);

        int[] stage = {0};
        int[] stageAt = {0};
        ThanosSwordEntity[] sword = {null};
        Vec3[] grip = {null};
        Zombie[] cut = {null};
        ThanosOrbEntity[] orb = {null};
        f.everyTick(250, tick -> {
            PowerTicker.tick(phoenix);
            PowerTicker.tick(billy);
            int since = tick - stageAt[0];
            switch (stage[0]) {
                case 0 -> {
                    check(Telekinesis.holds(phoenix, meteor) && meteor.isAlive(), "the meteor is in her grip, whole (tick " + since + ")");
                    check(near.getHealth() == near.getMaxHealth() && phoenix.getHealth() == ThanosMoveSupport.MAX && blasts[0] == 0,
                            "it hurts nobody (tick " + since + ")");
                    if (since > 10) {
                        check(meteor.getY() <= landingY - 2.0, "held 2 blocks and more below where it would land: " + (landingY - meteor.getY()));
                    }
                    if (since < 40) {
                        return false;
                    }
                    press(phoenix, 3, true);
                    check(!Telekinesis.isHolding(phoenix), "she lets it go without a throw");
                    stage[0] = 1;
                    stageAt[0] = tick;
                }
                case 1 -> {
                    check(meteor.isRemoved(), "a meteor let go crumbles away at once");
                    check(near.getHealth() == near.getMaxHealth() && phoenix.getHealth() == ThanosMoveSupport.MAX && blasts[0] == 0,
                            "and hurts nobody");
                    near.discard();
                    thanos.forceMove(ThanosMove.SWORD_THROW, decoy);
                    stage[0] = 2;
                    stageAt[0] = tick;
                }
                case 2 -> {
                    ThanosSwordEntity flying = sword(thanos);
                    check(since < 100, "the sword flies out and turns home");
                    if (flying == null || !flying.isReturning() || flying.position().subtract(thanos.position()).horizontalDistance() > 6.5) {
                        return false;
                    }
                    decoy.discard();
                    lookAt(billy, flying);
                    press(billy, 3, false);
                    check(Telekinesis.holds(billy, flying) && !flying.isReturning(), "Billy catches the sword on its way home");
                    sword[0] = flying;
                    grip[0] = flying.position();
                    cut[0] = stillZombie(helper, 0.0, FLOOR, 0.0);
                    cut[0].moveTo(grip[0].x, helper.absoluteVec(new Vec3(0.0, FLOOR, 0.0)).y, grip[0].z);
                    stage[0] = 3;
                    stageAt[0] = tick;
                }
                case 3 -> {
                    check(Telekinesis.holds(billy, sword[0]) && sword[0].position().distanceTo(grip[0]) <= 1.0,
                            "the sword stays where his grip holds it (tick " + since + "): " + sword[0].position().distanceTo(grip[0]));
                    check(thanos.swordAway() && !sword[0].isReturning(), "away from Thanos's hand, not flying home");
                    check(cut[0].getHealth() == cut[0].getMaxHealth(), "the zombie standing in it is not cut");
                    if (since < 20) {
                        return false;
                    }
                    cut[0].discard();
                    Telekinesis.release(billy, false);
                    check(sword[0].isReturning(), "let go, it flies home");
                    // An orb in front of Billy, held against a zombie.
                    orb[0] = orb(f, thanos, new Vec3(8.5, FLOOR + 1.2, 4.5), new Vec3(-1.0, 0.0, 0.0));
                    lookAt(billy, orb[0]);
                    press(billy, 3, false);
                    check(Telekinesis.holds(billy, orb[0]), "Billy catches an orb");
                    cut[0] = stillZombie(helper, 0.0, FLOOR, 0.0);
                    Vec3 at = orb[0].position();
                    cut[0].moveTo(at.x, helper.absoluteVec(new Vec3(0.0, FLOOR, 0.0)).y, at.z);
                    stage[0] = 4;
                    stageAt[0] = tick;
                }
                default -> {
                    check(Telekinesis.holds(billy, orb[0]) && orb[0].isAlive(), "the orb is in his grip (tick " + since + ")");
                    check(!cut[0].hasEffect(ModEffects.PETRIFIED), "and petrifies nothing it touches (tick " + since + ")");
                    if (since < 20) {
                        return false;
                    }
                    press(billy, 3, true);
                    check(orb[0].isRemoved(), "let go without a throw, it fades away");
                    check(!cut[0].hasEffect(ModEffects.PETRIFIED), "petrifying nobody");
                    cut[0].discard();
                    return true;
                }
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ultron's bolts (D-T 6.1)

    /**
     * T22 (D-T 6.1): a bolt of Ultron's that Billy catches is his when he throws it: it leaves the fight (its mark is gone,
     * and the orphan sweep leaves it alone: its owner is Billy), and it hurts the side it was fired for. Thrown at a
     * Sentry, the Sentry is hurt; thrown at Ultron's body, his health drops and the fight's ledger credits Billy.
     */
    @EnhancedTest(template = ARENA, batch = "boss_shot_bolt", timeoutTicks = 80)
    public static void boltsThrownBackHitUltronAndHisSentries(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = helper.getLevel();
        ServerPlayer billy = f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.BILLY);
        UltronTestSupport.tank(billy);
        UltronFight fight = UltronTestSupport.onTheGround(f, billy);
        UltronTestSupport.calm(fight);
        UltronEntity body = UltronTestSupport.boss(fight);
        UltronTestSupport.put(f, billy, 3.5, FLOOR, 10.5);
        UltronTestSupport.put(f, body, 16.5, FLOOR + 1.0, 15.5);
        UltronSentryEntity sentry = UltronTestSupport.sentryAt(f, fight, 16.5, 5.5);
        UltronTestSupport.calm(fight);
        float sentryHealth = sentry.getHealth();
        float bodyHealth = body.getHealth();

        UltronBoltEntity first = bolt(fight, body, billy, sentry);
        lookAt(billy, first);
        press(billy, 3, false);
        check(Telekinesis.holds(billy, first), "Billy catches a bolt of Ultron's");
        lookAt(billy, sentry);
        press(billy, 3, false);
        check(!Telekinesis.isHolding(billy) && first.getOwner() == billy, "and throws it at a Sentry: it is his");
        check(BossFights.fightOf(first) == null, "it left the fight");
        EntityJoinLevelEvent join = new EntityJoinLevelEvent(first, level);
        NeoForge.EVENT_BUS.post(join);
        check(!join.isCanceled(), "and the orphan sweep leaves it alone");

        UltronBoltEntity[] second = {null};
        f.everyTick(70, tick -> {
            PowerTicker.tick(billy);
            check(tick < 60, "the bolts reach their marks");
            if (second[0] == null) {
                if (sentry.getHealth() >= sentryHealth) {
                    return false;
                }
                check(first.isRemoved(), "the bolt hurt the Sentry it was fired for");
                second[0] = bolt(fight, body, billy, body);
                lookAt(billy, second[0]);
                press(billy, 3, false);
                check(Telekinesis.holds(billy, second[0]), "Billy catches another bolt");
                lookAt(billy, body);
                press(billy, 3, false);
                check(!Telekinesis.isHolding(billy) && BossFights.fightOf(second[0]) == null, "and throws it at Ultron");
                return false;
            }
            if (body.getHealth() >= bodyHealth) {
                return false;
            }
            check(second[0].isRemoved(), "the bolt hurt Ultron's body");
            check(fight.participant(billy.getUUID()).damage() > 0.0F, "and the fight's ledger credits Billy");
            return true;
        });
    }

    /**
     * A bolt of Ultron's fight (his body fired it), hanging still 3 blocks in front of Billy on his line to {@code target},
     * so that thrown where he looks it flies at the target.
     */
    private static UltronBoltEntity bolt(UltronFight fight, UltronEntity body, ServerPlayer billy, Entity target) {
        Vec3 eye = billy.getEyePosition();
        Vec3 middle = eye.add(target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(eye).normalize().scale(3.0));
        UltronBoltEntity bolt = UltronBoltEntity.create(billy.level(), body, middle, Vec3.ZERO, 4.0F, UltronBoltEntity.Palette.PRIME);
        bolt.setPos(middle.x, middle.y - bolt.getBbHeight() * 0.5, middle.z);
        fight.adopt(bolt);
        billy.level().addFreshEntity(bolt);
        return bolt;
    }

    private BossShotTests() {
    }
}
