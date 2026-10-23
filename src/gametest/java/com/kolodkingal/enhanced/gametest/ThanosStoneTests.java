package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.thanos.ThanosDefence;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosMeteorEntity;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosOrbEntity;
import com.kolodkingal.enhanced.thanos.ThanosPhantomEntity;
import com.kolodkingal.enhanced.thanos.ThanosState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.blow;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.fighter;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.fixture;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.lost;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.near;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * What the five stones do: each stone's moves, started with {@code forceMove} and read as damage, effects and flags. The
 * test arena has a barrier roof nine blocks up, so the tests of what falls from the sky open a column in it first.
 */
public final class ThanosStoneTests {

    private static ThanosEntity boss(TestKit.Fixture f, ServerPlayer starter, int stones) {
        ThanosEntity boss = ThanosBossTests.boss(f, starter);
        boss.forceStones(stones);
        return boss;
    }

    /** A wall of stone: {@code width} blocks along x from {@code x}, {@code height} tall, one thick at {@code z}. */
    private static void wall(TestKit.Fixture f, int x, int z, int width, int height) {
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                f.helper().setBlock(new BlockPos(x + dx, (int) FLOOR + dy, z), Blocks.STONE);
            }
        }
    }

    /** Opens the barrier roof over a patch of the arena, so something can fall through. */
    private static void openRoof(TestKit.Fixture f, int x, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int y = 3; y < 40; y++) {
                    BlockPos pos = new BlockPos(x + dx, y, z + dz);
                    if (f.helper().getBlockState(pos).is(Blocks.BARRIER)) {
                        f.helper().setBlock(pos, Blocks.AIR);
                    }
                }
            }
        }
    }

    private static void shield(TestKit.Fixture f, ServerPlayer wanda, float strength) {
        Powers.get(wanda).raiseChaosShield(f.level().getGameTime() - 100L, 0, strength);
    }

    // ============================================================================================================
    // The Power Stone

    /**
     * The Power Blast: 20 to the one it meets, 8 to those round where it ends, and the wall it ends on is not changed.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_power_blast", timeoutTicks = 120)
    public static void thePowerBlastHurtsSplashesAndChangesNoBlock(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer target = fighter(f, 0, 8);
        ServerPlayer splashed = fighter(f, 2.5, 8);
        ServerPlayer far = fighter(f, -6, 8);
        ThanosEntity boss = boss(f, target, 1);
        wall(f, 8, 20, 6, 4);
        boss.forceMove(ThanosMove.POWER_BLAST, target);
        f.everyTick(60, t -> {
            if (boss.currentMove() == ThanosMove.POWER_BLAST) {
                return false;
            }
            near(lost(target), blow(boss, ThanosBossConfig.POWER_BLAST_DAMAGE.get()), "the one in the beam takes the blast");
            near(lost(splashed), blow(boss, ThanosBossConfig.POWER_BLAST_SPLASH.get()), "the one by where it ends takes the splash");
            check(lost(far) == 0.0F, "the one off to the side is untouched");
            for (int dx = 0; dx < 6; dx++) {
                for (int dy = 0; dy < 4; dy++) {
                    f.helper().assertBlockPresent(Blocks.STONE, new BlockPos(8 + dx, (int) FLOOR + dy, 20));
                }
            }
            return true;
        });
    }

    /** A complete Chaos Shield stops the blast (and the splash), and loses 20. */
    @EnhancedTest(template = ARENA, batch = "thanos_power_blast_shield", timeoutTicks = 120)
    public static void aChaosShieldStopsThePowerBlastAndLosesTwenty(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = fighter(f, 0, 6, Power.SCARLET_WITCH);
        ServerPlayer behind = fighter(f, 0, 9);
        ThanosEntity boss = boss(f, wanda, 1);
        shield(f, wanda, 100.0F);
        PowerData data = Powers.get(wanda);
        boss.forceMove(ThanosMove.POWER_BLAST, wanda);
        f.everyTick(60, t -> {
            if (boss.currentMove() == ThanosMove.POWER_BLAST) {
                return false;
            }
            check(wanda.getHealth() == MAX_HEALTH, "the blast never reached her");
            TestKit.assertNear(100.0F - data.shieldStrength(), 20.0, 0.01, "the shield paid 20");
            check(lost(behind) == 0.0F, "and nothing came through behind her");
            return true;
        });
    }

    private static final float MAX_HEALTH = ThanosMoveSupport.MAX;

    /** The Overload fixes its sigils at tick 12: the one who stands there takes it, the one who left does not. */
    @EnhancedTest(template = ARENA, batch = "thanos_overload", timeoutTicks = 120)
    public static void theOverloadMarksThenDetonatesWhereTheyWereAndBreaksNoBlock(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer stays = fighter(f, 5, 0);
        ServerPlayer leaves = fighter(f, -5, 0);
        ThanosEntity boss = boss(f, stays, 1);
        BlockPos nearby = new BlockPos(15 + 1, (int) FLOOR, 10);
        f.helper().setBlock(nearby, Blocks.DIRT);
        boss.forceMove(ThanosMove.OVERLOAD, stays);
        Vec3 away = ThanosMoveSupport.spot(f, 0, FLOOR, -8);
        f.everyTick(70, t -> {
            if (t == 14) {
                leaves.teleportTo(f.level(), away.x, away.y, away.z, 0.0F, 0.0F);
            }
            if (boss.currentMove() == ThanosMove.OVERLOAD) {
                check(t >= 28 || lost(stays) == 0.0F, "nothing lands before the sigils fire: " + t);
                return false;
            }
            near(lost(stays), blow(boss, ThanosBossConfig.OVERLOAD_DAMAGE.get()), "the one on the sigil takes the Overload");
            check(lost(leaves) == 0.0F, "the one who left it is untouched");
            f.helper().assertBlockPresent(Blocks.DIRT, nearby);
            return true;
        });
    }

    /** With block damage on (and mob griefing), the Overload digs a small crater: close blocks go, far ones stay. */
    @EnhancedTest(template = ARENA, batch = "thanos_overload_crater", timeoutTicks = 120)
    public static void withBlockDamageOnTheOverloadMakesASmallCrater(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(ThanosBossConfig.BLOCK_DAMAGE, true);
        ServerPlayer stays = fighter(f, 5, 0);
        ThanosEntity boss = boss(f, stays, 1);
        BlockPos close = new BlockPos(15 + 1, (int) FLOOR, 10);
        BlockPos far = new BlockPos(15 + 5, (int) FLOOR, 10);
        f.helper().setBlock(close, Blocks.DIRT);
        f.helper().setBlock(far, Blocks.DIRT);
        boss.forceMove(ThanosMove.OVERLOAD, stays);
        f.everyTick(70, t -> {
            if (boss.currentMove() == ThanosMove.OVERLOAD) {
                return false;
            }
            check(f.helper().getBlockState(close).isAir(), "a block next to the sigil is gone");
            f.helper().assertBlockPresent(Blocks.DIRT, far);
            return true;
        });
    }

    /** The ring of the Shockwave: someone on the ground takes 18, someone in the air when it passes takes nothing. */
    @EnhancedTest(template = ARENA, batch = "thanos_shockwave", timeoutTicks = 100)
    public static void theShockwaveIsJumpable(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer onGround = fighter(f, 5, 0);
        ServerPlayer jumper = fighter(f, -5, 0);
        ThanosEntity boss = boss(f, onGround, 1);
        // (A block and a half up: well above the half a block the ring reaches, and inside the box it looks for creatures in.)
        Vec3 high = ThanosMoveSupport.spot(f, -5, FLOOR + 1.5, 0);
        jumper.teleportTo(f.level(), high.x, high.y, high.z, 0.0F, 0.0F);
        boss.forceMove(ThanosMove.SHOCKWAVE, onGround);
        f.everyTick(60, t -> {
            jumper.setDeltaMovement(Vec3.ZERO);
            jumper.setPos(high);
            if (boss.currentMove() == ThanosMove.SHOCKWAVE) {
                return false;
            }
            near(lost(onGround), blow(boss, ThanosBossConfig.SHOCKWAVE_DAMAGE.get()), "the one on the ground takes the ring");
            check(lost(jumper) == 0.0F, "the one in the air takes nothing");
            return true;
        });
    }

    /**
     * A fighter sealed in a stone room, where he can neither be seen nor walked to, is found by the Power Pulse after
     * the config's time: 12 damage through the walls, and not again before the Pulse's cooldown.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_pulse", timeoutTicks = 300)
    public static void thePowerPulseFindsAFighterSealedInARoom(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(ThanosBossConfig.UNREACHABLE_TICKS, 40);
        ServerPlayer hider = fighter(f, 5, 0);
        ThanosEntity boss = boss(f, hider, 1);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    boolean shell = Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 3;
                    if (shell) {
                        f.helper().setBlock(new BlockPos(15 + dx, (int) FLOOR + dy, 10 + dz), Blocks.STONE);
                    }
                }
            }
        }
        boolean[] pulsed = {false};
        f.everyTick(220, t -> {
            ThanosMove pick = boss.pickMove(hider);
            if (!pulsed[0]) {
                if (t < 25) {
                    check(pick != ThanosMove.POWER_PULSE, "not before the time: " + t);
                    return false;
                }
                if (pick != ThanosMove.POWER_PULSE) {
                    check(t < 90, "he found the hider: " + pick);
                    return false;
                }
                pulsed[0] = true;
                boss.forceMove(ThanosMove.POWER_PULSE, hider);
                return false;
            }
            if (boss.currentMove() == ThanosMove.POWER_PULSE) {
                return false;
            }
            near(lost(hider), blow(boss, ThanosBossConfig.PULSE_DAMAGE.get()), "twelve through the walls");
            check(pick != ThanosMove.POWER_PULSE, "and not again at once: the Pulse rests");
            return true;
        });
    }

    // ============================================================================================================
    // The Space Stone

    /** The Portal Step puts him behind the target, then a Jab or a Sweep follows at once. */
    @EnhancedTest(template = ARENA, batch = "thanos_step", timeoutTicks = 120)
    public static void thePortalStepArrivesBehindAndChainsAStrike(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer target = fighter(f, 0, 7);
        ThanosEntity boss = boss(f, target, 2);
        target.yBodyRot = 0.0F;
        boss.forceMove(ThanosMove.PORTAL_STEP, target);
        boolean[] stepped = {false};
        f.everyTick(80, t -> {
            if (!stepped[0] && boss.currentMove() == ThanosMove.PORTAL_STEP && boss.moveTick() >= 17) {
                stepped[0] = true;
                check(boss.distanceTo(target) < 3.0, "he stands behind him: " + boss.distanceTo(target));
                check(boss.position().z < target.position().z, "on the side he is not facing");
            }
            if (stepped[0] && (boss.currentMove() == ThanosMove.GAUNTLET_JAB || boss.currentMove() == ThanosMove.SWORD_SWEEP)) {
                return false;
            }
            if (stepped[0] && boss.currentMove() == null && lost(target) > 0.0F) {
                return true;
            }
            return false;
        });
    }

    /** A Hex wall between him and where he would land refuses the step: he does not move. */
    @EnhancedTest(template = ARENA, batch = "thanos_step_hex", timeoutTicks = 80)
    public static void aHexWallRefusesThePortalStep(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        ServerPlayer wanda = fighter(f, 7, 7, Power.SCARLET_WITCH);
        ThanosEntity boss = boss(f, wanda, 2);
        HexManager.create(wanda);
        check(boss.pickMove(wanda) != ThanosMove.PORTAL_STEP, "a step into the Hex is not even considered");
        for (int i = 0; i < 200; i++) {
            check(boss.pickMove(wanda) != ThanosMove.PORTAL_STEP, "never chosen against someone inside the Hex");
        }
        Vec3 before = boss.position();
        boss.forceMove(ThanosMove.PORTAL_STEP, wanda);
        f.everyTick(40, t -> {
            if (boss.currentMove() == ThanosMove.PORTAL_STEP) {
                return false;
            }
            check(boss.position().distanceTo(before) < 0.5, "the wall refused the step: he stayed");
            return true;
        });
    }

    /** One meteor for each player, landing on its ring for 16 in radius 2.5, breaking no block. */
    @EnhancedTest(template = ARENA, batch = "thanos_meteor", timeoutTicks = 200)
    public static void meteorHailMarksEveryPlayerAndEachLandsInItsRing(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(ThanosBossConfig.METEOR_EXTRA, 0);
        ServerPlayer one = fighter(f, -5, 0);
        ServerPlayer two = fighter(f, 5, 0);
        ThanosEntity boss = boss(f, one, 2);
        openRoof(f, 5, 10, 3);
        openRoof(f, 15, 10, 3);
        BlockPos block = new BlockPos(15 + 4, (int) FLOOR, 10);
        f.helper().setBlock(block, Blocks.DIRT);
        boss.forceMove(ThanosMove.METEOR_HAIL, one);
        Set<UUID> seen = new HashSet<>();
        f.everyTick(150, t -> {
            for (ThanosMeteorEntity meteor : f.level().getEntitiesOfClass(ThanosMeteorEntity.class, boss.getBoundingBox().inflate(60.0))) {
                seen.add(meteor.getUUID());
            }
            if (boss.currentMove() == ThanosMove.METEOR_HAIL || !f.level().getEntitiesOfClass(ThanosMeteorEntity.class, boss.getBoundingBox().inflate(60.0)).isEmpty()) {
                return false;
            }
            check(seen.size() == 2, "one meteor for each player: " + seen.size());
            float damage = blow(boss, ThanosBossConfig.METEOR_DAMAGE.get());
            near(lost(one), damage, "the first is hit for 16");
            near(lost(two), damage, "and the second");
            f.helper().assertBlockPresent(Blocks.DIRT, block);
            return true;
        });
    }

    /** A crowd is marked with at most ten meteors, however many players there are. */
    @EnhancedTest(template = ARENA, batch = "thanos_meteor_cap", timeoutTicks = 200)
    public static void meteorHailNeverCallsDownMoreThanTen(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        List<ServerPlayer> crowd = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            crowd.add(fighter(f, -9.0 + (i % 6) * 3.6, (i / 6) * 4.0 - 8.0));
        }
        ThanosEntity boss = boss(f, crowd.get(0), 2);
        boss.forceMove(ThanosMove.METEOR_HAIL, crowd.get(0));
        Set<UUID> seen = new HashSet<>();
        f.everyTick(150, t -> {
            for (ThanosMeteorEntity meteor : f.level().getEntitiesOfClass(ThanosMeteorEntity.class, boss.getBoundingBox().inflate(60.0))) {
                seen.add(meteor.getUUID());
            }
            if (boss.currentMove() == ThanosMove.METEOR_HAIL) {
                return false;
            }
            check(seen.size() == 10, "twelve players, ten meteors: " + seen.size());
            return true;
        });
    }

    /** A Chaos Shield stops a meteor and loses 16. */
    @EnhancedTest(template = ARENA, batch = "thanos_meteor_shield", timeoutTicks = 200)
    public static void aChaosShieldStopsAMeteorAndLosesSixteen(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(ThanosBossConfig.METEOR_EXTRA, 0);
        ServerPlayer wanda = fighter(f, 5, 0, Power.SCARLET_WITCH);
        ThanosEntity boss = boss(f, wanda, 2);
        openRoof(f, 15, 10, 3);
        shield(f, wanda, 100.0F);
        PowerData data = Powers.get(wanda);
        boss.forceMove(ThanosMove.METEOR_HAIL, wanda);
        f.everyTick(150, t -> {
            PowerTicker.tick(wanda);
            if (boss.currentMove() == ThanosMove.METEOR_HAIL || !f.level().getEntitiesOfClass(ThanosMeteorEntity.class, boss.getBoundingBox().inflate(60.0)).isEmpty()) {
                return false;
            }
            check(wanda.getHealth() == MAX_HEALTH, "the meteor never touched her");
            TestKit.assertNear(100.0F - data.shieldStrength(), 16.0, 0.01, "the shield paid 16");
            return true;
        });
    }

    /** The Pull draws in those he can see (not the one behind a wall, not a phased Vision), then crushes close to him for 14. */
    @EnhancedTest(template = ARENA, batch = "thanos_pull", timeoutTicks = 160)
    public static void gravityPullBreaksOnCoverAndCrushesWhatItDrawsIn(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer seen = fighter(f, 0, 9);
        ServerPlayer covered = fighter(f, -9, 0);
        ServerPlayer phased = fighter(f, 0, -9, Power.VISION);
        Powers.get(phased).setVisionPhased(true);
        ThanosEntity boss = boss(f, seen, 2);
        wall(f, 5, 8, 1, 4);
        wall(f, 5, 9, 1, 4);
        wall(f, 5, 10, 1, 4);
        wall(f, 5, 11, 1, 4);
        wall(f, 5, 12, 1, 4);
        Vec3 coveredAt = covered.position();
        Vec3 phasedAt = phased.position();
        double start = boss.distanceTo(seen);
        boss.forceMove(ThanosMove.GRAVITY_PULL, seen);
        f.everyTick(100, t -> {
            seen.doTick();
            covered.doTick();
            phased.doTick();
            if (boss.currentMove() == ThanosMove.GRAVITY_PULL) {
                return false;
            }
            check(boss.distanceTo(seen) < start - 4.0, "he was drawn in: " + start + " to " + boss.distanceTo(seen));
            check(covered.position().distanceTo(coveredAt) < 0.5, "the one behind a wall did not move");
            check(phased.position().distanceTo(phasedAt) < 0.5, "a phased Vision did not move");
            near(lost(seen), blow(boss, ThanosBossConfig.PULL_CRUSH_DAMAGE.get()), "and the crush at the end hurt him");
            return true;
        });
    }

    /**
     * The Space Barrier turns ranged blows (an arrow, Thor's beam, a dagger) to nothing, and lets the stone-born through (Wanda's
     * beam, Vision's beam, Stormbreaker); it stands 60 ticks and rests 300.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_barrier", timeoutTicks = 200)
    public static void theBarrierStopsRangedNotStoneBorn(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer shooter = fighter(f, 0, 6);
        ThanosEntity boss = boss(f, shooter, 2);
        boss.forceMove(ThanosMove.SPACE_BARRIER, shooter);
        Arrow arrow = EntityType.ARROW.create(level);
        f.everyTick(100, t -> {
            if (t == 12) {
                check(boss.flag(ThanosEntity.FLAG_BARRIER), "the Barrier is up");
                check(!boss.ready(ThanosMove.SPACE_BARRIER), "and it rests");
                float health = boss.getHealth();
                ThanosBossTests.hit(boss, level.damageSources().arrow(arrow, shooter), 80.0F);
                ThanosBossTests.hit(boss, ModDamageTypes.source(level, ModDamageTypes.LIGHTNING_BEAM, shooter, shooter), 80.0F);
                ThanosBossTests.hit(boss, ModDamageTypes.source(level, ModDamageTypes.LOKI_DAGGER, shooter, arrow), 80.0F);
                check(boss.getHealth() == health, "an arrow, Thor's beam and a dagger do nothing");
                ThanosBossTests.hit(boss, ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, shooter, shooter), 80.0F);
                check(boss.getHealth() < health, "Wanda's beam passes");
                health = boss.getHealth();
                ThanosBossTests.hit(boss, ModDamageTypes.source(level, ModDamageTypes.MIND_STONE_BEAM, shooter, shooter), 80.0F);
                check(boss.getHealth() < health, "Vision's beam passes");
                health = boss.getHealth();
                ThanosBossTests.hit(boss, ModDamageTypes.source(level, ModDamageTypes.STORMBREAKER, shooter, shooter), 80.0F);
                check(boss.getHealth() < health, "Stormbreaker passes");
            }
            if (t == 10 + ThanosBossConfig.BARRIER_TICKS.get() + 4) {
                check(!boss.flag(ThanosEntity.FLAG_BARRIER), "the Barrier fell after its time");
                return true;
            }
            return false;
        });
    }

    // ============================================================================================================
    // The Reality Stone

    /** The Veil turns arrows within twelve blocks to bubbles, leaves Stormbreaker alone, and falls after 120 ticks. */
    @EnhancedTest(template = ARENA, batch = "thanos_veil", timeoutTicks = 260)
    public static void theVeilTurnsProjectilesToBubbles(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer shooter = fighter(f, 0, 6);
        ThanosEntity boss = boss(f, shooter, 3);
        boss.forceMove(ThanosMove.REALITY_VEIL, shooter);
        Arrow[] arrow = {null};
        Arrow[] proof = {null};
        f.everyTick(200, t -> {
            if (t == 14) {
                check(boss.flag(ThanosEntity.FLAG_VEIL), "the Veil is up");
                arrow[0] = EntityType.ARROW.create(level);
                arrow[0].setOwner(shooter);
                arrow[0].setNoGravity(true);
                arrow[0].moveTo(ThanosMoveSupport.spot(f, 8, FLOOR + 1.5, 0));
                arrow[0].setDeltaMovement(-0.4, 0.0, 0.0);
                level.addFreshEntity(arrow[0]);
            }
            if (t == 20) {
                check(arrow[0].isRemoved(), "the arrow became a bubble and popped");
                check(boss.getHealth() == boss.getMaxHealth(), "and did not touch him");
            }
            if (t == 12 + ThanosBossConfig.VEIL_TICKS.get() + 4) {
                check(!boss.flag(ThanosEntity.FLAG_VEIL), "the Veil fell after its time");
                return true;
            }
            return false;
        });
    }

    /** The Phantoms: three illusions of 1 health, the real one hidden for 80 ticks until something touches him. */
    @EnhancedTest(template = ARENA, batch = "thanos_phantoms", timeoutTicks = 220)
    public static void phantomsAndTheHiddenReal(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer a = fighter(f, 0, 2);
        ServerPlayer b = fighter(f, 4, 4);
        ThanosEntity boss = boss(f, a, 3);
        boss.forceMove(ThanosMove.PHANTOMS, a);
        boolean[] done = {false};
        f.everyTick(100, t -> {
            if (t < 26) {
                return false;
            }
            List<ThanosPhantomEntity> phantoms = level.getEntitiesOfClass(ThanosPhantomEntity.class, boss.getBoundingBox().inflate(30.0));
            if (!done[0]) {
                done[0] = true;
                check(phantoms.size() == 3, "three phantoms: " + phantoms.size());
                check(boss.hidden() && boss.isInvisible(), "the real one is hidden");
                for (ThanosPhantomEntity phantom : phantoms) {
                    check(phantom.getMaxHealth() == 1.0F, "one health each");
                }
                float before = a.getHealth();
                phantoms.get(0).doHurtTarget(a);
                near(before - a.getHealth(), blow(boss, ThanosBossConfig.PHANTOM_DAMAGE.get()), "a phantom's jab");
                phantoms.get(1).hurt(level.damageSources().playerAttack(b), 1.0F);
                check(phantoms.get(1).isRemoved(), "one touch dissolves a phantom");
                ThanosBossTests.hit(boss, b, 10.0F);
                check(!boss.hidden() && !boss.isInvisible(), "a hit shows the real one");
                return false;
            }
            if (t == 26 + 45) {
                check(boss.hidden() && boss.isInvisible(), "after forty ticks of being seen he hides among them again");
                return true;
            }
            return false;
        });
    }

    /** The Reality orb turns the first player it meets to stone for 60 ticks (half damage, cannot move); a Chaos Shield and a Creative player are not touched. */
    @EnhancedTest(template = ARENA, batch = "thanos_orb", timeoutTicks = 200)
    public static void thePetrifyOrbFreezesOnePlayerAndMeetsAShield(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer target = fighter(f, -5, 8);
        ServerPlayer wanda = fighter(f, 5, 8, Power.SCARLET_WITCH);
        ServerPlayer creative = f.player(ThanosBossTests.CENTRE.x, FLOOR, ThanosBossTests.CENTRE.z + 8.0, GameType.CREATIVE);
        ThanosEntity boss = boss(f, target, 3);
        shield(f, wanda, 100.0F);
        PowerData data = Powers.get(wanda);
        boss.forceMove(ThanosMove.PETRIFY_ORB, target);
        int[] stage = {0};
        f.everyTick(190, t -> {
            PowerTicker.tick(wanda);
            if (stage[0] == 0 && target.hasEffect(ModEffects.PETRIFIED)) {
                stage[0] = 1;
                check(target.getEffect(ModEffects.PETRIFIED).getDuration() <= 60, "sixty ticks at most");
                check(target.getAttributeValue(Attributes.MOVEMENT_SPEED) < 0.01, "he cannot move");
                float before = target.getHealth();
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().generic(), 10.0F);
                near(before - target.getHealth(), 10.0F * ThanosConfig.PETRIFY_DAMAGE_FACTOR.get().floatValue(), "and takes half damage");
                boss.forceMove(ThanosMove.PETRIFY_ORB, wanda);
            }
            if (stage[0] == 1 && boss.currentMove() == null && !level.getEntitiesOfClass(ThanosOrbEntity.class, boss.getBoundingBox().inflate(60.0)).isEmpty()) {
                return false;
            }
            if (stage[0] == 1 && boss.currentMove() == null) {
                check(!wanda.hasEffect(ModEffects.PETRIFIED), "the shield stopped the orb");
                TestKit.assertNear(100.0F - data.shieldStrength(), 2.0, 0.01, "for 2");
                stage[0] = 2;
                boss.forceMove(ThanosMove.PETRIFY_ORB, creative);
                return false;
            }
            if (stage[0] == 2 && boss.currentMove() == null) {
                check(!creative.hasEffect(ModEffects.PETRIFIED), "a Creative player is untouched");
                return true;
            }
            return false;
        });
    }

    // ============================================================================================================
    // The Soul Stone

    /** The Soul Stone sees through Loki: the duplicates dissolve, the invisibility ends and his borrowed shape is gone. */
    @EnhancedTest(template = ARENA, batch = "thanos_sight", timeoutTicks = 120)
    public static void soulSightDissolvesLokisTricks(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer loki = fighter(f, 4, 4, Power.LOKI);
        ThanosEntity boss = boss(f, loki, 4);
        Zombie model = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(ThanosBossTests.CENTRE.x + 4.0, FLOOR, ThanosBossTests.CENTRE.z + 6.0));
        lookAt(loki, model);
        AbilityHandler.onKey(loki, 3, true);
        AbilityHandler.onKey(loki, 3, false);
        model.discard();
        AbilityHandler.onKey(loki, 2, true);
        AbilityHandler.onKey(loki, 2, false);
        PowerData data = Powers.get(loki);
        check(!data.form().isEmpty(), "Loki wears a borrowed shape");
        check(!LokiIllusionEntity.ownedBy(level, loki.getUUID()).isEmpty(), "and has duplicates");
        check(loki.hasEffect(MobEffects.INVISIBILITY), "and hides");
        check(boss.pickMove(loki) == ThanosMove.SOUL_SIGHT, "the Soul Stone is what he reaches for");
        boss.forceMove(ThanosMove.SOUL_SIGHT, loki);
        f.everyTick(40, t -> {
            if (boss.currentMove() == ThanosMove.SOUL_SIGHT) {
                return false;
            }
            check(LokiIllusionEntity.ownedBy(level, loki.getUUID()).isEmpty(), "the duplicates are gone");
            check(!loki.hasEffect(MobEffects.INVISIBILITY) && !data.is(Live.HIDDEN), "the hiding is over");
            check(data.form().isEmpty(), "and his shape is his own");
            return true;
        });
    }

    /**
     * Soul Drain: 3 every ten ticks that armour and enchantments cannot lessen, 40 percent of it healing him; the chain snaps
     * when a block comes between, and when 60 poise of blows are dealt to him.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_drain", timeoutTicks = 260)
    public static void soulDrainTakesLifeThroughArmourAndHealsHim(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer victim = fighter(f, 6, 0);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (ItemStack piece : List.of(new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.NETHERITE_CHESTPLATE),
                new ItemStack(Items.NETHERITE_LEGGINGS), new ItemStack(Items.NETHERITE_BOOTS))) {
            piece.enchant(enchantments.getOrThrow(Enchantments.PROTECTION), 4);
            victim.setItemSlot(((net.minecraft.world.item.ArmorItem) piece.getItem()).getEquipmentSlot(), piece);
        }
        ThanosEntity boss = boss(f, victim, 4);
        boss.setHealth(boss.getMaxHealth() * 0.5F);
        double pointsBefore = boss.points();
        boss.forceMove(ThanosMove.SOUL_DRAIN, victim);
        f.everyTick(160, t -> {
            if (boss.currentMove() == ThanosMove.SOUL_DRAIN) {
                return false;
            }
            float events = 8.0F;
            float expected = events * blow(boss, ThanosBossConfig.SOUL_DRAIN_TICK.get());
            near(lost(victim), expected, "3 every ten ticks for eight beats, armour or none");
            double healed = boss.points() - pointsBefore;
            TestKit.assertNear(healed, 0.4 * expected, 0.6, "and he heals forty percent of it");
            return true;
        });
    }

    /** A block between him and the chain snaps it before its first beat; the victim loses nothing. */
    @EnhancedTest(template = ARENA, batch = "thanos_drain_snap", timeoutTicks = 160)
    public static void theDrainChainSnapsWithoutASightAndBreaksUnderPoise(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer cut = fighter(f, 6, 0);
        ThanosEntity boss = boss(f, cut, 4);
        boss.forceMove(ThanosMove.SOUL_DRAIN, cut);
        boolean[] walled = {false};
        f.everyTick(60, t -> {
            if (t == 30 && !walled[0]) {
                walled[0] = true;
                for (int dz = -2; dz <= 2; dz++) {
                    wall(f, 13, 10 + dz, 1, 4);
                }
            }
            if (boss.currentMove() == ThanosMove.SOUL_DRAIN) {
                return false;
            }
            check(lost(cut) == 0.0F, "the chain snapped before it bit");
            return true;
        });
    }

    /** Sixty poise of blows during the Drain end it. */
    @EnhancedTest(template = ARENA, batch = "thanos_drain_poise", timeoutTicks = 160)
    public static void poiseBreaksTheDrain(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer victim = fighter(f, 6, 0);
        ServerPlayer rescuer = fighter(f, -6, -6);
        ThanosEntity boss = boss(f, victim, 4);
        boss.forceMove(ThanosMove.SOUL_DRAIN, victim);
        f.everyTick(100, t -> {
            if (t == 40) {
                check(boss.currentMove() == ThanosMove.SOUL_DRAIN, "the drain is running");
                ThanosBossTests.hit(boss, rescuer, 100.0F);
            }
            if (t >= 44) {
                check(boss.currentMove() != ThanosMove.SOUL_DRAIN, "a hard blow broke the chain");
                float lostSoFar = lost(victim);
                check(lostSoFar <= blow(boss, ThanosBossConfig.SOUL_DRAIN_TICK.get()) * 1.01F, "after at most one beat: " + lostSoFar);
                return true;
            }
            return false;
        });
    }

    /** The Soul World takes whoever is in the pillar at the end (nothing hurts them), heals him 3 percent each and spares those who left. */
    @EnhancedTest(template = ARENA, batch = "thanos_soulworld", timeoutTicks = 160)
    public static void soulworldHoldsAndHeals(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer inside = fighter(f, 6, 0);
        ServerPlayer alsoInside = fighter(f, 6, 1.5);
        ServerPlayer leaves = fighter(f, 6, -1.5);
        ServerPlayer creative = f.player(ThanosBossTests.CENTRE.x + 6.0, FLOOR, ThanosBossTests.CENTRE.z + 0.5, GameType.CREATIVE);
        ThanosEntity boss = boss(f, inside, 4);
        boss.setHealth(boss.getMaxHealth() * 0.5F);
        float before = boss.getHealth();
        boss.forceMove(ThanosMove.SOULWORLD, inside);
        Vec3 away = ThanosMoveSupport.spot(f, -6, FLOOR, 0);
        f.everyTick(70, t -> {
            if (t == 14) {
                leaves.teleportTo(level, away.x, away.y, away.z, 0.0F, 0.0F);
            }
            if (boss.currentMove() == ThanosMove.SOULWORLD) {
                return false;
            }
            check(inside.hasEffect(ModEffects.SOULWORLD) && alsoInside.hasEffect(ModEffects.SOULWORLD), "both in the pillar are taken");
            check(!leaves.hasEffect(ModEffects.SOULWORLD), "the one who left is not");
            check(!creative.hasEffect(ModEffects.SOULWORLD), "nor is a Creative player");
            TestKit.assertNear(boss.getHealth() - before, boss.getMaxHealth() * ThanosBossConfig.SOULWORLD_HEAL.get() * 2.0, 1.0, "he healed 3 percent for each");
            float health = inside.getHealth();
            inside.hurt(level.damageSources().generic(), 50.0F);
            check(inside.getHealth() == health, "nothing hurts a soul in the Soul World");
            return true;
        });
    }

    // ============================================================================================================
    // The Time Stone

    /**
     * The Rewind returns up to 8 percent of his health, the broken plates and his place, and does nothing if he is staggered
     * first; it is not chosen until he has taken enough damage.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_rewind", timeoutTicks = 260)
    public static void theRewindRestoresWhatWasLost(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a, 5);
        float max = boss.getMaxHealth();
        Vec3 home = boss.position();
        float[] hurt = {0.0F};
        boolean[] cast = {false};
        f.everyTick(200, t -> {
            if (t == 40) {
                check(boss.pickMove(a) != ThanosMove.REWIND, "no Rewind before he is hurt");
                boss.setHealth(max * 0.76F);
                ThanosBossTests.hit(boss, a, 100.0F);
                boss.setHealth(max * 0.56F);
                ThanosBossTests.hit(boss, a, 100.0F);
                check(Integer.bitCount(boss.plates()) == 2, "two plates broke: " + boss.plates());
                boss.moveTo(home.x + 5.0, home.y, home.z);
                hurt[0] = boss.getHealth();
                boss.forceMove(ThanosMove.REWIND, a);
                cast[0] = true;
                return false;
            }
            if (!cast[0] || boss.currentMove() == ThanosMove.REWIND) {
                return false;
            }
            float healed = boss.getHealth() - hurt[0];
            check(Math.abs(healed - max * ThanosBossConfig.REWIND_MAX_HEAL.get().floatValue()) < max * 0.005F,
                    "he got back the most a Rewind returns: " + (healed / max));
            check(boss.plates() == ThanosDefence.ALL_PLATES, "the plates are whole");
            check(boss.position().distanceTo(home) < 1.5, "and he is where he was: " + boss.position().distanceTo(home));
            return true;
        });
    }

    /** A stagger in the Rewind's wind-up undoes it. */
    @EnhancedTest(template = ARENA, batch = "thanos_rewind_fail", timeoutTicks = 160)
    public static void aStaggeredTitanCannotRewind(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a, 5);
        float max = boss.getMaxHealth();
        boolean[] cast = {false};
        f.everyTick(120, t -> {
            if (t == 40) {
                boss.setHealth(max * 0.5F);
                boss.forceMove(ThanosMove.REWIND, a);
                cast[0] = true;
                return false;
            }
            if (cast[0] && t == 50) {
                boss.lockGauntlet(40, null);
                check(boss.state() == ThanosState.STAGGERED, "the lock in the wind-up staggers him: " + boss.state());
            }
            if (t == 100) {
                check(Math.abs(boss.getHealth() - max * 0.5F) < max * 0.02F, "and nothing came back");
                return true;
            }
            return false;
        });
    }

    /** The Rewind is his answer to a lot of damage taken lately, and not otherwise. */
    @EnhancedTest(template = ARENA, batch = "thanos_rewind_trigger", timeoutTicks = 200)
    public static void theRewindIsChosenOnlyAfterEnoughDamage(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a, 5);
        f.everyTick(120, t -> {
            if (t < 30) {
                return false;
            }
            if (t == 30) {
                for (int i = 0; i < 200; i++) {
                    check(boss.pickMove(a) != ThanosMove.REWIND, "nothing lost, nothing to rewind");
                }
                for (int i = 0; i < 4; i++) {
                    ThanosBossTests.hit(boss, a, 400.0F);
                }
                check(boss.pickMove(a) == ThanosMove.REWIND, "after a lot of damage it is the first thing he reaches for");
                return true;
            }
            return false;
        });
    }

    /** The Snare locks those in the discs at the end of the wind-up, not the ones who stepped out. */
    @EnhancedTest(template = ARENA, batch = "thanos_snare", timeoutTicks = 120)
    public static void timeSnareLocksOnlyThoseInside(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer stays = fighter(f, 5, 0);
        ServerPlayer leaves = fighter(f, -5, 0);
        ThanosEntity boss = boss(f, stays, 5);
        double speed = stays.getAttributeValue(Attributes.MOVEMENT_SPEED);
        Vec3 away = ThanosMoveSupport.spot(f, 0, FLOOR, -8);
        boss.forceMove(ThanosMove.TIME_SNARE, stays);
        f.everyTick(60, t -> {
            if (t == 14) {
                leaves.teleportTo(f.level(), away.x, away.y, away.z, 0.0F, 0.0F);
            }
            if (boss.currentMove() == ThanosMove.TIME_SNARE) {
                return false;
            }
            check(stays.hasEffect(ModEffects.TIME_LOCKED), "the one in the disc is locked");
            check(!leaves.hasEffect(ModEffects.TIME_LOCKED), "the one who left is not");
            TestKit.assertNear(stays.getAttributeValue(Attributes.MOVEMENT_SPEED), speed * 0.4, speed * 0.1, "at forty percent of his speed");
            return true;
        });
    }

    /**
     * The Infinity Beam: 40 to each in the fan once, none to the one behind a wall or outside, the Chaos Shield shatters, and it
     * waits for the fifth stone.
     */
    @EnhancedTest(template = ARENA, batch = "thanos_infinity", timeoutTicks = 200)
    public static void theInfinityBeamNeedsFiveAndAWall(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer straight = fighter(f, 0, 8);
        ServerPlayer leftOfIt = fighter(f, -2.9, 8);
        ServerPlayer outside = fighter(f, 7, 4);
        ServerPlayer covered = fighter(f, 4, 8);
        ThanosEntity four = ThanosBossTests.boss(f, straight);
        four.forceStones(4);
        for (int i = 0; i < 300; i++) {
            check(four.pickMove(straight) != ThanosMove.INFINITY_BEAM, "not with four stones");
        }
        four.forceStones(5);
        wall(f, 12, 15, 5, 4);
        wall(f, 12, 14, 5, 4);
        four.forceMove(ThanosMove.INFINITY_BEAM, straight);
        f.everyTick(150, t -> {
            if (four.currentMove() == ThanosMove.INFINITY_BEAM) {
                return false;
            }
            float damage = blow(four, ThanosBossConfig.INFINITY_BEAM_DAMAGE.get());
            near(lost(straight), damage, "the one ahead takes the beam, once");
            near(lost(leftOfIt), damage, "so does the one in the fan");
            check(lost(outside) == 0.0F, "the one outside the fan is untouched");
            check(lost(covered) == 0.0F, "and the one behind the wall");
            return true;
        });
    }

    /** A Chaos Shield in the beam's way loses 40 (it shatters at its usual strength). */
    @EnhancedTest(template = ARENA, batch = "thanos_infinity_shield", timeoutTicks = 200)
    public static void aChaosShieldShattersUnderTheInfinityBeam(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = fighter(f, 0, 8, Power.SCARLET_WITCH);
        ThanosEntity boss = boss(f, wanda, 5);
        shield(f, wanda, 30.0F);
        PowerData data = Powers.get(wanda);
        boss.forceMove(ThanosMove.INFINITY_BEAM, wanda);
        f.everyTick(150, t -> {
            if (boss.currentMove() == ThanosMove.INFINITY_BEAM) {
                return false;
            }
            check(wanda.getHealth() == MAX_HEALTH, "the beam did not reach her");
            check(!data.chaosShieldActive() && data.shieldStrength() <= 0.0F, "the shield shattered");
            return true;
        });
    }

    private ThanosStoneTests() {
    }
}
