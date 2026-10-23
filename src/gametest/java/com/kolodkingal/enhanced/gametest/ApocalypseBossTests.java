package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.apocalypse.ApocalypseAttack;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.apocalypse.SandHazards;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.entity.SandBoulderEntity;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.entity.SandLanceEntity;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Log;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.duel;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.every;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.put;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.rel;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The boss's attacks (spec 3.6 and 8, tests 20 to 29): each has a tell of its length, locks its aim at its moment, and
 * does exactly what the table says; the sand is stopped by what the spec says stops it; the controller chooses by its
 * rules. The attacks are forced one at a time ({@code brain().force}) with the boss otherwise calm, except for the one test
 * that lets him fight on his own.
 */
public final class ApocalypseBossTests {

    /** The ticks the fight needs to look round before an attack can be forced (it learns who its foes are). */
    private static final int WARM = 2;

    private static float lost(ServerPlayer player, float before) {
        return before - player.getHealth();
    }

    /** The distance from a point to the line through {@code from} along {@code direction}. */
    private static double distanceToLine(Vec3 point, Vec3 from, Vec3 direction) {
        Vec3 unit = direction.normalize();
        Vec3 offset = point.subtract(from);
        return offset.subtract(unit.scale(offset.dot(unit))).length();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Lances

    @EnhancedTest(template = ARENA, batch = "apocalypse_lances", timeoutTicks = 160)
    public static void lancesFanFiveWithATellAndTheAimLocks(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        Log log = new Log(hero);
        float health = hero.getHealth();
        int[] tellTicks = {0};
        Vec3[] locked = {null};
        boolean[] moved = {false};
        boolean[] checked = {false};
        every(helper, 100, tick -> {
            if (tick == WARM) {
                check(boss.brain().force(boss, d.fight(), ApocalypseAttack.LANCES), "he begins the Lances");
                check(boss.state() == ApocalypseState.TELL_LANCE, "with the pose of the tell");
                return false;
            }
            if (tick < WARM) {
                return false;
            }
            if (boss.brain().attack() == ApocalypseAttack.LANCES) {
                tellTicks[0]++;
                int n = boss.brain().attackTicks();
                if (n == 3) {
                    check(!log.drain().effects(ApocalypseFxPayload.STREAK).isEmpty(), "a glint line runs to the target during the tell");
                }
                if (n >= 9 && !moved[0]) {
                    // The aim locked at tick 8: whoever steps aside now is not followed.
                    locked[0] = hero.position().add(0, hero.getBbHeight() * 0.65, 0);
                    put(d.f(), hero, 3.5, FLOOR, 5.5);
                    moved[0] = true;
                }
                return false;
            }
            if (!moved[0] || checked[0]) {
                return false;
            }
            checked[0] = true;
            check(tellTicks[0] >= 13 && tellTicks[0] <= 15, "the tell lasts 14 ticks: " + tellTicks[0]);
            List<SandLanceEntity> lances = level.getEntitiesOfClass(SandLanceEntity.class, boss.getBoundingBox().inflate(60.0));
            check(lances.size() == ApocalypseBossConfig.LANCE_COUNT.get(), "five lances fly: " + lances.size());
            // A flat fan of 14 degrees: the middle one passes through where the target was when the aim locked.
            List<Double> angles = new ArrayList<>();
            double best = Double.MAX_VALUE;
            for (SandLanceEntity lance : lances) {
                Vec3 direction = lance.getDeltaMovement();
                angles.add(net.minecraft.util.Mth.wrapDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)) - Math.toDegrees(Math.atan2(lances.get(0).getDeltaMovement().z, lances.get(0).getDeltaMovement().x))));
                best = Math.min(best, distanceToLine(locked[0], lance.position(), direction));
                check(Math.abs(lance.getDeltaMovement().length() - 1.9) < 0.05, "at 1.9 blocks a tick: " + lance.getDeltaMovement().length());
                check(lance.getOwner() == boss && lance.damage() == 6.0F, "six damage each, his");
            }
            angles.sort(Comparator.naturalOrder());
            assertNear(angles.get(4) - angles.get(0), 14.0, 0.6, "a fan of 14 degrees");
            assertNear(angles.get(2) - angles.get(0), 7.0, 0.6, "with the middle one in the middle");
            check(best < 0.6, "and the middle one points where the target stood at the lock (" + best + ")");
            helper.runAfterDelay(8, () -> {
                check(hero.getHealth() == health, "the hero who stepped aside was not hit");
                helper.succeed();
            });
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_sand_damage", timeoutTicks = 120)
    public static void aLanceDoesSixAndABoulderElevenAndShattersOnABlockWithoutTouchingIt(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        put(d.f(), hero, 6.5, FLOOR, 10.5);
        float health = hero.getHealth();
        Vec3 chest = hero.position().add(0, 1.2, 0);

        SandLanceEntity lance = new SandLanceEntity(ModEntities.SAND_LANCE.get(), level);
        lance.launch(boss, boss.position().add(0, 1.5, 0), chest.subtract(boss.position().add(0, 1.5, 0)), 1.9F, 6.0F);
        d.fight().spawn(lance);
        helper.runAfterDelay(6, () -> {
            check(Math.abs(lost(hero, health) - 6.0) < 1.0E-3, "a lance does 6: " + lost(hero, health) + " (lance " + (lance.isRemoved() ? "spent" : "at " + rel(d.f(), lance)) + ", hero at " + rel(d.f(), hero) + ", boss at " + rel(d.f(), boss) + ")");
            check(lance.isRemoved(), "and is spent");
            float before = hero.getHealth();
            hero.invulnerableTime = 0;
            SandBoulderEntity boulder = new SandBoulderEntity(ModEntities.SAND_BOULDER.get(), level);
            Vec3 from = boss.position().add(0, 1.5, 0);
            boulder.launch(boss, from, chest.subtract(from), 1.3F, 11.0F);
            d.fight().spawn(boulder);
            helper.runAfterDelay(8, () -> {
                assertNear(lost(hero, before), 11.0, 1.0E-3, "a boulder does 11");
                check(boulder.isRemoved(), "and shatters");
                // Against a block it shatters too, and the block is as it was.
                BlockPos stone = new BlockPos(14, 2, 10);
                helper.setBlock(stone, Blocks.STONE);
                SandBoulderEntity second = new SandBoulderEntity(ModEntities.SAND_BOULDER.get(), level);
                Vec3 start = helper.absoluteVec(new Vec3(11.5, FLOOR + 0.5, 10.5));
                second.launch(boss, start, new Vec3(1.0, 0.0, 0.0), 1.3F, 11.0F);
                d.fight().spawn(second);
                SandLanceEntity other = new SandLanceEntity(ModEntities.SAND_LANCE.get(), level);
                other.launch(boss, start.add(0.0, 0.0, 0.5), new Vec3(1.0, 0.0, 0.0), 1.9F, 6.0F);
                d.fight().spawn(other);
                helper.runAfterDelay(10, () -> {
                    check(second.isRemoved() && other.isRemoved(), "a boulder and a lance that meet a block are spent");
                    helper.assertBlockPresent(Blocks.STONE, stone);
                    check(level.getBlockState(helper.absolutePos(stone.west())).isAir(), "and nothing was dug out before it");
                    helper.succeed();
                });
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The wave and the spikes

    @EnhancedTest(template = ARENA, batch = "apocalypse_wave", timeoutTicks = 200)
    public static void waveHitsOnceAndCanBeJumpedAndBreaksOnAStep(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        float health = hero.getHealth();
        put(d.f(), hero, 4.5, FLOOR, 10.5);
        ServerPlayer jumper = d.f().player(5.5, FLOOR + 1.6, 10.5);
        ApocalypseTestSupport.tank(jumper);
        ServerPlayer flier = d.f().player(6.5, FLOOR + 3.0, 10.5);
        ApocalypseTestSupport.tank(flier);
        flier.getAbilities().mayfly = true;
        flier.getAbilities().flying = true;
        IronGolem zombie = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(7.5, FLOOR, 10.5));
        // A step of three blocks across the way, and a zombie behind it.
        for (int z = 4; z <= 17; z++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(2, y, z), Blocks.STONE);
            }
        }
        IronGolem behind = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(0.9, FLOOR, 10.5));
        float jumperHealth = jumper.getHealth();
        float flierHealth = flier.getHealth();

        helper.runAfterDelay(WARM, () -> {
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.WAVE), "he begins the Wave");
            check(boss.state() == ApocalypseState.TELL_WAVE, "with its tell");
            every(helper, 80, tick -> {
                if (!SandHazards.all().isEmpty() || boss.brain().isBusy()) {
                    return false;
                }
                check(tick > ApocalypseAttack.WAVE.tellTicks(), "the wave rolled after its tell");
                assertNear(lost(hero, health), 10.0, 1.0E-3, "a player in its path takes 10, once");
                assertNear(zombie.getMaxHealth() - zombie.getHealth(), 10.0, 1.0E-3, "so does an Iron Golem, once");
                check(jumper.getHealth() == jumperHealth, "a player who jumps over it takes nothing");
                check(flier.getHealth() == flierHealth, "nor one who flies");
                check(behind.getHealth() == behind.getMaxHealth(), "a three-block step broke it up before the golem behind it (it has " + behind.getHealth() + ", wall block "
                        + helper.getBlockState(new BlockPos(1, 3, 10)).getBlock() + ", hazards " + SandHazards.count() + ")");
                return true;
            });
        });
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_spikes", timeoutTicks = 120)
    public static void spikesHitInsideTheRingOnly(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        put(d.f(), hero, 5.5, FLOOR, 10.5);
        ServerPlayer beside = d.f().player(5.5 + 2.4, FLOOR, 10.5);
        ApocalypseTestSupport.tank(beside);
        ServerPlayer above = d.f().player(5.5, FLOOR + 4.0, 10.5);
        ApocalypseTestSupport.tank(above);
        float health = hero.getHealth();
        float besideHealth = beside.getHealth();
        float aboveHealth = above.getHealth();
        Vec3 centre = hero.position();
        SandHazards.spikes(level, boss, d.fight().id(), centre, 1.8, 0, 9.0F);
        helper.runAfterDelay(3, () -> {
            assertNear(health - hero.getHealth(), 9.0, 1.0E-3, "inside the ring: 9");
            check(hero.getDeltaMovement().y >= 0.89, "and is launched 0.9 up: " + hero.getDeltaMovement().y);
            check(beside.getHealth() == besideHealth, "2.4 blocks from the centre is outside it");
            check(above.getHealth() == aboveHealth, "four blocks up is out of the way");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_spikes_n", timeoutTicks = 160)
    public static void spikesSitUnderUpToOnePlusNPlayersAtMostFour(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ApocalypseEntity boss = d.boss();
        List<ServerPlayer> players = new ArrayList<>();
        players.add(d.hero());
        double[][] spots = {{3.5, 3.5}, {3.5, 17.5}, {17.5, 3.5}, {17.5, 17.5}, {10.5, 3.5}};
        for (double[] spot : spots) {
            ServerPlayer p = d.f().player(spot[0], FLOOR, spot[1]);
            ApocalypseTestSupport.tank(p);
            players.add(p);
        }
        put(d.f(), d.hero(), 10.5, FLOOR, 17.5);
        Map<ServerPlayer, Float> before = new java.util.HashMap<>();
        for (ServerPlayer p : players) {
            before.put(p, p.getHealth());
        }
        helper.runAfterDelay(WARM + 2, () -> {
            check(d.fight().fighterCount() == 6, "six fight: " + d.fight().fighterCount());
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.SPIKES), "he begins the Spikes");
            helper.runAfterDelay(ApocalypseAttack.SPIKES.tellTicks() + 6, () -> {
                check(!boss.brain().isBusy(), "the attack is over");
                int hit = 0;
                for (ServerPlayer p : players) {
                    if (p.getHealth() < before.get(p)) {
                        hit++;
                        assertNear(before.get(p) - p.getHealth(), 9.0, 1.0E-3, "a spike does 9");
                    }
                }
                check(hit == 4, "rings sit under min(1 + 6, 4) = 4 of six players: " + hit);
                helper.succeed();
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Boulders

    @EnhancedTest(template = ARENA, batch = "apocalypse_boulders", timeoutTicks = 160)
    public static void boulderBarrageAimsAtDifferentPlayers(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ApocalypseEntity boss = d.boss();
        ServerPlayer a = d.hero();
        put(d.f(), a, 3.5, FLOOR, 10.5);
        ServerPlayer b = d.f().player(10.5, FLOOR, 3.5);
        ServerPlayer c = d.f().player(17.5, FLOOR, 17.5);
        ApocalypseTestSupport.tank(b);
        ApocalypseTestSupport.tank(c);
        List<ServerPlayer> players = List.of(a, b, c);
        float[] before = {a.getHealth(), b.getHealth(), c.getHealth()};
        helper.runAfterDelay(WARM + 2, () -> {
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.BOULDERS), "he begins the Boulders");
            check(boss.state() == ApocalypseState.TELL_BOULDERS, "with the tell");
            helper.runAfterDelay(ApocalypseAttack.BOULDERS.tellTicks() + 2, () -> {
                // They are in the air: three of them, each on the line to a different player.
                List<SandBoulderEntity> boulders = d.level().getEntitiesOfClass(SandBoulderEntity.class, boss.getBoundingBox().inflate(60.0));
                check(boulders.size() == 3, "three boulders: " + boulders.size());
                Set<ServerPlayer> aimedAt = new java.util.HashSet<>();
                for (SandBoulderEntity boulder : boulders) {
                    ServerPlayer best = null;
                    double nearest = Double.MAX_VALUE;
                    for (ServerPlayer p : players) {
                        double miss = distanceToLine(p.position().add(0, 1.0, 0), boulder.position(), boulder.getDeltaMovement());
                        if (miss < nearest) {
                            nearest = miss;
                            best = p;
                        }
                    }
                    check(nearest < 1.2, "each boulder is aimed at someone (missing by " + nearest + ")");
                    aimedAt.add(best);
                }
                check(aimedAt.size() == 3, "each at a different player");
                helper.runAfterDelay(16, () -> {
                    for (int i = 0; i < 3; i++) {
                        assertNear(before[i] - players.get(i).getHealth(), 9.0, 1.0E-3, "each player took one boulder: 9 (3.2: was 11)");
                    }
                    helper.succeed();
                });
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Entomb

    @EnhancedTest(template = ARENA, batch = "apocalypse_entomb", timeoutTicks = 420)
    public static void entombLocksThenTheCocoonHoldsAndNeverKills(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        put(d.f(), hero, 3.5, FLOOR, 10.5);
        hero.setHealth(6.0F);
        int[] phase = {0};
        float[] lowest = {Float.MAX_VALUE};
        every(helper, 400, tick -> {
            if (tick == WARM) {
                check(boss.brain().force(boss, d.fight(), ApocalypseAttack.ENTOMB), "he reaches for the hero");
                check(boss.state() == ApocalypseState.TELL_ENTOMB, "with the tell");
                return false;
            }
            if (tick < WARM) {
                return false;
            }
            if (phase[0] == 0) {
                if (boss.brain().isBusy()) {
                    if (boss.brain().attackTicks() == 21) {
                        // The stripe locked at tick 20: three blocks away is out of its reach.
                        put(d.f(), hero, 3.5, FLOOR, 7.5);
                    }
                    return false;
                }
                check(SandCocoonEntity.cocoonOf(hero) == null && !hero.hasEffect(ModEffects.ENTOMBED), "a player who moved three blocks escaped");
                check(hero.getHealth() == 6.0F, "untouched");
                phase[0] = 1;
                put(d.f(), hero, 3.5, FLOOR, 10.5);
                check(boss.brain().force(boss, d.fight(), ApocalypseAttack.ENTOMB), "he tries again");
                return false;
            }
            if (phase[0] == 1) {
                if (boss.brain().isBusy()) {
                    return false;
                }
                SandCocoonEntity cocoon = SandCocoonEntity.cocoonOf(hero);
                check(cocoon != null && hero.hasEffect(ModEffects.ENTOMBED), "one who did not is entombed: the cocoon and the effect");
                check(cocoon.health() == 20.0F && cocoon.ticksLeft() > 70, "a cocoon of 20 health for 80 ticks (3.2: was 100): " + cocoon.ticksLeft());
                check(d.fight().owns(cocoon), "the cocoon belongs to the fight");
                phase[0] = 2;
                return false;
            }
            if (phase[0] == 2) {
                lowest[0] = Math.min(lowest[0], hero.getHealth());
                check(hero.getHealth() >= 4.0F - 1.0E-3F, "the sand never takes them below two hearts: " + hero.getHealth());
                if (SandCocoonEntity.cocoonOf(hero) == null) {
                    check(!hero.hasEffect(ModEffects.ENTOMBED), "when it lets go the effect goes");
                    check(lowest[0] <= 4.5F, "but it did hurt, down to two hearts: " + lowest[0]);
                    return true;
                }
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_cocoon", timeoutTicks = 120)
    public static void threeSwordHitsBreakACocoonAndAFriendCanFreeItWithTelekinesis(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        ServerPlayer victim = d.f().player(6.5, FLOOR, 4.5);
        ApocalypseTestSupport.tank(victim);
        SandCocoonEntity cocoon = SandControl.entomb(level, boss, victim, 100, 1.5F, 20.0F, d.fight());
        check(cocoon != null && victim.hasEffect(ModEffects.ENTOMBED), "swallowed");
        ServerPlayer friend = d.f().player(6.5, FLOOR, 7.5);
        ItemStack sword = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        friend.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        d.f().pvp(true);
        for (int i = 1; i <= 3; i++) {
            check(!cocoon.isRemoved(), "the cocoon holds before hit " + i);
            TestKit.chargeAttack(friend);
            cocoon.hurt(level.damageSources().playerAttack(friend), 7.0F);
        }
        check(cocoon.isRemoved() && !victim.hasEffect(ModEffects.ENTOMBED), "three hits of 7 break a cocoon of 20, and the effect goes with it");

        // A creature of the fight's side cannot be freed by hitting the cocoon of someone else... but Wanda lifting a friend can.
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, FLOOR, 10.5));
        SandCocoonEntity pigCocoon = SandControl.entomb(level, boss, pig, 100, 0.0F, 20.0F, d.fight());
        check(pigCocoon != null && SandCocoonEntity.cocoonOf(pig) == pigCocoon, "a pig in a cocoon");
        put(d.f(), d.hero(), 3.5, FLOOR, 10.5);
        lookAt(d.hero(), pig);
        AbilityHandler.onKey(d.hero(), 3, true);
        check(Telekinesis.isHolding(d.hero()), "Wanda takes hold of it");
        check(pigCocoon.isRemoved() && !pig.hasEffect(ModEffects.ENTOMBED), "and that breaks the cocoon");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_entombed_hero", timeoutTicks = 120)
    public static void anEntombedHeroCannotUseAbilitiesAndAHeldBeamStops(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ServerPlayer wanda = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        AbilityHandler.onKey(wanda, 1, true);
        check(Powers.get(wanda).beamActive(), "the beam is on");
        SandCocoonEntity cocoon = SandControl.entomb(level, boss, wanda, 100, 0.0F, 20.0F, d.fight());
        check(cocoon != null, "she is swallowed");
        helper.runAfterDelay(3, () -> {
            check(!Powers.get(wanda).beamActive(), "the beam she held stopped");
            AbilityHandler.onKey(wanda, 1, true);
            check(!Powers.get(wanda).beamActive(), "and a press does nothing");
            AbilityHandler.onKey(wanda, 2, true);
            check(!wanda.getAbilities().flying, "nor does the key of flight");
            AbilityHandler.onKey(wanda, 1, false);
            // She cannot swing a sword or use an item either.
            check(NeoForge.EVENT_BUS.post(new AttackEntityEvent(wanda, boss)).isCanceled(), "her attacks are cancelled");
            cocoon.release();
            check(!wanda.hasEffect(ModEffects.ENTOMBED), "free");
            AbilityHandler.onKey(wanda, 1, true);
            check(Powers.get(wanda).beamActive(), "and the beam answers again");
            AbilityHandler.onKey(wanda, 1, false);
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_entomb_skips", timeoutTicks = 120)
    public static void theGraspSkipsWhomTheSandCannotHold(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.SCARLET_WITCH);
        ServerPlayer wanda = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        ServerPlayer creative = d.f().player(5.5, FLOOR, 4.5, GameType.CREATIVE);
        ServerPlayer spectator = d.f().player(5.5, FLOOR, 16.5, GameType.SPECTATOR);
        PowerData data = Powers.get(wanda);
        data.raiseChaosShield(level.getGameTime(), 0, 100.0F);
        check(SandControl.hasCompleteShield(wanda), "Wanda's shield is complete");
        check("message.enhanced.apocalypse.shield_protects".equals(SandControl.entombRefusal(wanda)), "so the sand cannot hold her: " + SandControl.entombRefusal(wanda));
        check("message.enhanced.entomb.slips".equals(SandControl.entombRefusal(creative)), "a Creative player slips out");
        check("message.enhanced.entomb.slips".equals(SandControl.entombRefusal(spectator)), "and a Spectator");
        check("message.enhanced.entomb.slips".equals(SandControl.entombRefusal(boss)), "and he himself, whose type is entomb_immune");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 14.5));
        check(SandControl.entombRefusal(zombie) == null, "a Zombie can be held");
        SandCocoonEntity cocoon = SandControl.entomb(level, boss, zombie, 100, 0.0F, 20.0F, d.fight());
        check(cocoon != null && SandControl.entombRefusal(zombie) != null, "and only once");
        check(SandControl.entomb(level, boss, zombie, 100, 0.0F, 20.0F, d.fight()) == null, "a second cocoon on the same creature is refused");
        cocoon.release();

        // The grasp itself reaches for the shielded Wanda (and fails, with a word), and has no one to reach for among the others.
        Log log = new Log(wanda);
        helper.runAfterDelay(WARM, () -> {
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.ENTOMB), "he reaches for the one who is there");
            every(helper, 60, tick -> {
                if (boss.brain().isBusy()) {
                    return false;
                }
                check(SandCocoonEntity.cocoonOf(wanda) == null && !wanda.hasEffect(ModEffects.ENTOMBED), "and the shield keeps her out of the sand");
                check(log.drain().toldOnActionBar("message.enhanced.apocalypse.shield_protects"), "she is told why");
                return true;
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Mind Crush

    @EnhancedTest(template = ARENA, batch = "apocalypse_crush", timeoutTicks = 160)
    public static void mindCrushHitsEveryoneButMagnetosHelmet(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.SCARLET_WITCH);
        ApocalypseEntity boss = d.boss();
        ServerPlayer wanda = d.hero();
        ServerPlayer plain = d.f().player(6.5, FLOOR, 4.5);
        ServerPlayer magneto = d.f().player(14.5, FLOOR, 6.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        ServerPlayer faraway = d.f().player(19.5, FLOOR, 19.5);
        ServerPlayer creative = d.f().player(14.5, FLOOR, 14.5, GameType.CREATIVE);
        PowerData data = Powers.get(wanda);
        data.raiseChaosShield(d.level().getGameTime(), 0, 100.0F);
        d.f().config(ApocalypseBossConfig.MIND_CRUSH_RANGE, 12);
        helper.runAfterDelay(WARM, () -> {
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.MIND_CRUSH), "he begins the Mind Crush");
            check(boss.state() == ApocalypseState.TELL_MIND_CRUSH, "with the tell");
            helper.runAfterDelay(ApocalypseAttack.MIND_CRUSH.tellTicks() + 3, () -> {
                for (ServerPlayer p : List.of(wanda, plain)) {
                    check(p.hasEffect(MobEffects.CONFUSION) && p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && p.hasEffect(MobEffects.BLINDNESS),
                            "Nausea, Slowness and Blindness for " + p.getName().getString());
                }
                check(WandaAbilities.hasShieldUp(wanda), "and a Chaos Shield did not stop it");
                check(!magneto.hasEffect(MobEffects.CONFUSION) && !magneto.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !magneto.hasEffect(MobEffects.BLINDNESS),
                        "Magneto's helmet turns all of it away");
                check(!creative.hasEffect(MobEffects.CONFUSION), "a Creative player is spared");
                check(!faraway.hasEffect(MobEffects.CONFUSION), "and one beyond the range is not touched");
                helper.succeed();
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Portal Step

    @EnhancedTest(template = ARENA, batch = "apocalypse_portal", timeoutTicks = 200)
    public static void portalStepAppearsBehindAndRespectsTheHex(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        put(d.f(), hero, 3.5, FLOOR, 10.5);
        float health = hero.getHealth();
        Vec3 start = boss.position();
        helper.runAfterDelay(WARM, () -> {
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.PORTAL_STEP), "he steps into a portal (foes " + d.fight().foes().size()
                    + ", behind " + com.kolodkingal.enhanced.ability.Teleports.behind(boss, hero) + ", fits "
                    + com.kolodkingal.enhanced.ability.Teleports.fits(boss, hero.position().add(3.5, 0.0, 0.0)) + ", boss " + boss.getBoundingBox().getXsize()
                    + " x " + boss.getBoundingBox().getYsize() + ")");
            check(boss.state() == ApocalypseState.TELL_PORTAL, "with its tell");
            every(helper, 60, tick -> {
                if (boss.brain().isBusy()) {
                    return false;
                }
                check(boss.position().distanceTo(start) > 3.0, "he is somewhere else: " + boss.position().distanceTo(start));
                check(boss.position().distanceTo(hero.position()) < 5.5, "and next to the hero: " + boss.position().distanceTo(hero.position()));
                assertNear(health - hero.getHealth(), ApocalypseBossConfig.PORTAL_SHOCK_DAMAGE.get(), 1.0E-3, "the arrival shockwave did 6 to the hero, who stood by the exit");
                check(hero.getDeltaMovement().horizontalDistance() > 0.5, "and threw them back: " + hero.getDeltaMovement());
                // With a Hex wall between, the step is refused: nothing to step to.
                ServerPlayer witch = d.f().player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
                d.f().config(com.kolodkingal.enhanced.EnhancedConfig.HEX_DEFAULT_RADIUS, 5);
                AbilityHandler.onKey(witch, 5, true);
                check(HexManager.ownedBy(d.level(), witch.getUUID()).isPresent(), "a Hex stands round the hero");
                boss.moveTo(helper.absoluteVec(new Vec3(14.5, FLOOR, 10.5)));
                check(!boss.brain().force(boss, d.fight(), ApocalypseAttack.PORTAL_STEP), "he cannot step into it: the wall refuses");
                check(boss.state() != ApocalypseState.TELL_PORTAL, "and so he walks");
                return true;
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Backhand

    @EnhancedTest(template = ARENA, batch = "apocalypse_backhand", timeoutTicks = 100)
    public static void backhandIsHisFistAndOnlyWithinReach(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        put(d.f(), hero, 10.5, FLOOR, 8.5);
        TestKit.lookAt(hero, boss);
        float health = hero.getHealth();
        helper.runAfterDelay(WARM, () -> {
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.BACKHAND), "he swings");
            helper.runAfterDelay(ApocalypseAttack.BACKHAND.tellTicks() + 3, () -> {
                assertNear(health - hero.getHealth(), 12.0, 1.0E-3, "the fist lands on a player in front of him: 12 (3.2: was 16)");
                check(hero.getLastDamageSource() != null && hero.getLastDamageSource().is(ModDamageTypes.APOCALYPSE_BLOW), "it is a blow of his");
                // Out of reach it finds no one.
                float after = hero.getHealth();
                put(d.f(), hero, 10.5, FLOOR, 4.5);
                hero.invulnerableTime = 0;
                check(boss.brain().force(boss, d.fight(), ApocalypseAttack.BACKHAND), "and again");
                helper.runAfterDelay(ApocalypseAttack.BACKHAND.tellTicks() + 3, () -> {
                    check(hero.getHealth() == after, "six blocks away it finds no one");
                    helper.succeed();
                });
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Tornadoes

    @EnhancedTest(template = ARENA, batch = "apocalypse_tornado", timeoutTicks = 160)
    public static void tornadoesDriftLiftAndHurt(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        put(d.f(), hero, 4.5, FLOOR, 10.5);
        float health = hero.getHealth();
        Vec3 spot = helper.absoluteVec(new Vec3(8.5, FLOOR, 10.5));
        SandHazards.Tornado tornado = SandHazards.tornado(level, boss, d.fight().id(), spot, 3.2, 160, 2.0F);
        Vec3[] seen = {null};
        every(helper, 70, tick -> {
            if (tick == 1) {
                seen[0] = tornado.position();
            }
            if (tick == 11) {
                // Ten ticks of drifting toward the player at 0.12.
                assertNear(tornado.position().distanceTo(seen[0]), 1.2, 0.05, "it drifts 0.12 a tick toward the nearest player");
                check(tornado.position().x < seen[0].x, "toward the player, who is to the west");
            }
            if (tick == 70) {
                float taken = health - hero.getHealth();
                check(taken >= 4.0F && taken % 2.0F < 1.0E-3F, "2 at a time, every 10 ticks, once he is inside its radius: " + taken);
                check(hero.getDeltaMovement().y > 0.2 && hero.getDeltaMovement().y <= 0.6001, "it lifted him, but 0.6 at most in all: " + hero.getDeltaMovement().y);
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Chaos Shield and the sand

    @EnhancedTest(template = ARENA, batch = "apocalypse_shield_sand", timeoutTicks = 240)
    public static void chaosShieldStopsSandAndChargesItsDamage(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ServerPlayer wanda = d.hero();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        put(d.f(), wanda, 6.5, FLOOR, 10.5);
        PowerData data = Powers.get(wanda);
        data.raiseChaosShield(level.getGameTime(), 0, 500.0F);
        float health = wanda.getHealth();
        float[] strength = {data.shieldStrength()};
        Vec3 feet = wanda.position();

        // A lance and a boulder meet the sphere: neutralised, and the shield pays what they would have done.
        SandLanceEntity lance = new SandLanceEntity(ModEntities.SAND_LANCE.get(), level);
        lance.launch(boss, feet.add(-3.0, 1.0, 0.0), new Vec3(1.0, 0.0, 0.0), 1.9F, 6.0F);
        d.fight().spawn(lance);
        PowerTicker.tick(wanda);
        check(lance.isRemoved(), "the shield turned the lance away (shield up " + WandaAbilities.hasShieldUp(wanda) + ", active " + data.chaosShieldActive() + ", strength " + data.shieldStrength() + ", lance at " + rel(d.f(), lance) + ", Wanda at " + rel(d.f(), wanda) + ")");
        assertNear(strength[0] - data.shieldStrength(), 6.0, 1.0E-3, "and paid 6");
        strength[0] = data.shieldStrength();
        SandBoulderEntity boulder = new SandBoulderEntity(ModEntities.SAND_BOULDER.get(), level);
        boulder.launch(boss, feet.add(-3.0, 1.0, 0.0), new Vec3(1.0, 0.0, 0.0), 1.3F, 11.0F);
        d.fight().spawn(boulder);
        PowerTicker.tick(wanda);
        check(boulder.isRemoved(), "and the boulder");
        assertNear(strength[0] - data.shieldStrength(), 11.0, 1.0E-3, "for 11");
        strength[0] = data.shieldStrength();

        // The hazards hurt through the shield's own hook: it pays and she is untouched.
        SandHazards.spikes(level, boss, d.fight().id(), feet, 1.8, 0, 9.0F);
        helper.runAfterDelay(3, () -> {
            assertNear(strength[0] - data.shieldStrength(), 9.0, 1.0E-3, "a spike costs the shield 9");
            check(wanda.getHealth() == health, "and does nothing to her");
            strength[0] = data.shieldStrength();
            SandHazards.WaveSpec spec = new SandHazards.WaveSpec(0.8, 30, 4, 1.1, 10.0F, 40, 1.0);
            SandHazards.wave(level, boss, d.fight().id(), boss.position(), new Vec3(-1.0, 0.0, 0.0), spec, d.fight().arena().center(), d.fight().arena().radius());
            helper.runAfterDelay(14, () -> {
                assertNear(strength[0] - data.shieldStrength(), 10.0, 1.0E-3, "a wave costs 10");
                check(wanda.getHealth() == health && !wanda.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "and neither hurts nor slows her");
                strength[0] = data.shieldStrength();
                SandHazards.ring(level, boss, d.fight().id(), feet, 0.9, 1.5, 1.1, 6.0, 12.0F);
                helper.runAfterDelay(4, () -> {
                    assertNear(strength[0] - data.shieldStrength(), 12.0, 1.0E-3, "a ring of the Cairo Collapse costs 12");
                    check(wanda.getHealth() == health, "and does nothing to her");
                    strength[0] = data.shieldStrength();
                    SandHazards.tornado(level, boss, d.fight().id(), feet, 3.2, 40, 2.0F);
                    helper.runAfterDelay(25, () -> {
                        float paid = strength[0] - data.shieldStrength();
                        check(paid >= 4.0F && paid % 2.0F < 1.0E-3F, "a tornado costs the shield what it would have done, 2 at a time: " + paid);
                        check(wanda.getHealth() == health, "and does nothing to her");
                        // The Backhand is a blow of his hand: stopped like any melee blow.
                        strength[0] = data.shieldStrength();
                        put(d.f(), wanda, 10.5, FLOOR, 8.5);
                        check(boss.brain().force(boss, d.fight(), ApocalypseAttack.BACKHAND), "he swings at her");
                        helper.runAfterDelay(ApocalypseAttack.BACKHAND.tellTicks() + 3, () -> {
                            assertNear(strength[0] - data.shieldStrength(), ApocalypseBossConfig.BACKHAND_DAMAGE.get(), 1.0E-3, "the Backhand costs the shield 16");
                            check(wanda.getHealth() == health, "and does not touch her");
                            helper.succeed();
                        });
                    });
                });
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the sand is

    @EnhancedTest(template = ARENA, batch = "apocalypse_pure")
    public static void sandProjectilesCanBeCaughtButTheCocoonCannot(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer billy = f.player(4.5, FLOOR, 4.5, Power.BILLY);
        SandLanceEntity lance = new SandLanceEntity(ModEntities.SAND_LANCE.get(), level);
        lance.setPos(helper.absoluteVec(new Vec3(8.5, FLOOR + 1.0, 4.5)));
        level.addFreshEntity(lance);
        SandBoulderEntity boulder = new SandBoulderEntity(ModEntities.SAND_BOULDER.get(), level);
        boulder.setPos(helper.absoluteVec(new Vec3(8.5, FLOOR + 1.0, 6.5)));
        level.addFreshEntity(boulder);
        SandCocoonEntity cocoon = new SandCocoonEntity(ModEntities.SAND_COCOON.get(), level);
        cocoon.setPos(helper.absoluteVec(new Vec3(8.5, FLOOR, 8.5)));
        level.addFreshEntity(cocoon);
        check(Telekinesis.canLift(billy, lance, Telekinesis.Mode.BILLY), "Billy can catch a lance in flight");
        check(Telekinesis.canLift(billy, boulder, Telekinesis.Mode.BILLY), "and a boulder");
        check(!Telekinesis.canLift(billy, cocoon, Telekinesis.Mode.BILLY), "but not a cocoon");
        ServerPlayer wanda = f.player(4.5, FLOOR, 8.5, Power.SCARLET_WITCH);
        check(!Telekinesis.canLift(wanda, cocoon, Telekinesis.Mode.WANDA), "nor can Wanda lift one");
        for (var type : List.of(ModEntities.SAND_LANCE.get(), ModEntities.SAND_BOULDER.get(), ModEntities.SAND_COCOON.get())) {
            check(type.is(ModTags.SHAPESHIFT_IMMUNE), type + " is shapeshift_immune");
        }
        check(ModEntities.SAND_COCOON.get().is(ModTags.TELEKINESIS_IMMUNE), "the cocoon is telekinesis_immune");
        lance.discard();
        boulder.discard();
        cocoon.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Choosing

    @EnhancedTest(template = ARENA, batch = "apocalypse_pure")
    public static void attackSelectionRules(GameTestHelper helper) {
        long now = 10_000;
        RandomSource random = RandomSource.create(7L);
        // Phase 1, a player in the middle distance in sight, nothing special: not Entomb, Mind Crush, Tornado or Portal.
        for (int i = 0; i < 400; i++) {
            ApocalypseAttack picked = ApocalypseAttack.choose(choice(1, 12.0, false, false, null, now, -1000, -1000, true, false, a -> true), random);
            check(picked != null && picked != ApocalypseAttack.ENTOMB && picked != ApocalypseAttack.MIND_CRUSH && picked != ApocalypseAttack.TORNADO
                    && picked != ApocalypseAttack.PORTAL_STEP && picked != ApocalypseAttack.BACKHAND, "phase 1 at 12 blocks picks among the five: " + picked);
        }
        // The same attack is never chosen twice in a row.
        for (ApocalypseAttack last : ApocalypseAttack.values()) {
            for (int i = 0; i < 200; i++) {
                ApocalypseAttack picked = ApocalypseAttack.choose(choice(3, 2.0, false, false, last, now, -1000, -1000, true, true, a -> true), random);
                check(picked != last, "never " + last + " twice in a row");
            }
        }
        // Entomb and Mind Crush are never within 100 ticks of each other.
        for (int i = 0; i < 300; i++) {
            ApocalypseAttack a = ApocalypseAttack.choose(choice(2, 12.0, false, false, null, now, now - 50, -1000, true, false, x -> true), random);
            check(a != ApocalypseAttack.MIND_CRUSH, "no Mind Crush within 100 ticks of an Entomb");
            ApocalypseAttack b = ApocalypseAttack.choose(choice(2, 12.0, false, false, null, now, -1000, now - 50, true, false, x -> true), random);
            check(b != ApocalypseAttack.ENTOMB, "no Entomb within 100 ticks of a Mind Crush");
        }
        boolean crush = false;
        boolean entomb = false;
        for (int i = 0; i < 600; i++) {
            ApocalypseAttack a = ApocalypseAttack.choose(choice(2, 12.0, false, false, null, now, now - 100, -1000, true, false, x -> true), random);
            crush |= a == ApocalypseAttack.MIND_CRUSH;
            ApocalypseAttack b = ApocalypseAttack.choose(choice(2, 12.0, false, false, null, now, -1000, now - 100, true, false, x -> true), random);
            entomb |= b == ApocalypseAttack.ENTOMB;
        }
        check(crush && entomb, "after 100 ticks they are allowed again");
        // The first attack after a stagger is the Lances.
        for (int i = 0; i < 100; i++) {
            check(ApocalypseAttack.choose(choice(1, 12.0, true, true, null, now, -1000, -1000, true, false, x -> true), random) == ApocalypseAttack.LANCES,
                    "the Lances come first after a stagger");
        }
        // The Backhand only within 3.2 blocks.
        check(ApocalypseAttack.BACKHAND.allowed(choice(1, 3.1, false, false, null, now, -1000, -1000, true, false, x -> true)), "the Backhand at 3.1");
        check(!ApocalypseAttack.BACKHAND.allowed(choice(1, 3.3, false, false, null, now, -1000, -1000, true, false, x -> true)), "but not at 3.3");
        // In a window only the Lances, the Backhand and the Spikes.
        Set<ApocalypseAttack> seen = EnumSet.noneOf(ApocalypseAttack.class);
        for (int i = 0; i < 600; i++) {
            ApocalypseAttack a = ApocalypseAttack.choose(choice(3, 8.0, true, false, null, now, -1000, -1000, true, true, x -> true), random);
            if (a != null) {
                seen.add(a);
            }
        }
        check(seen.equals(EnumSet.of(ApocalypseAttack.LANCES, ApocalypseAttack.SPIKES)), "in a window at 8 blocks: " + seen);
        // The Portal Step only when wanted; the Mind Crush needs a sight; an attack on cooldown is not chosen.
        check(!ApocalypseAttack.PORTAL_STEP.allowed(choice(1, 12.0, false, false, null, now, -1000, -1000, true, false, x -> true)), "no portal unless it is wanted");
        check(ApocalypseAttack.PORTAL_STEP.allowed(choice(1, 12.0, false, false, null, now, -1000, -1000, true, true, x -> true)), "and one when it is");
        check(!ApocalypseAttack.MIND_CRUSH.allowed(choice(2, 12.0, false, false, null, now, -1000, -1000, false, false, x -> true)), "no Mind Crush with nobody in sight");
        check(!ApocalypseAttack.WAVE.allowed(choice(1, 12.0, false, false, null, now, -1000, -1000, true, false, x -> x != ApocalypseAttack.WAVE)), "none on cooldown");
        check(ApocalypseAttack.choose(choice(1, 12.0, false, false, null, now, -1000, -1000, true, false, x -> false), random) == null, "nothing ready: nothing");
        // Each by its weight: the Backhand (60) next to the five of phase 1 in reach.
        Map<ApocalypseAttack, Integer> counts = new EnumMap<>(ApocalypseAttack.class);
        int total = 20_000;
        for (int i = 0; i < total; i++) {
            ApocalypseAttack a = ApocalypseAttack.choose(choice(1, 3.0, false, false, null, now, -1000, -1000, true, false, x -> true), random);
            counts.merge(a, 1, Integer::sum);
        }
        // At 3 blocks: Backhand 60, Lances need 6+ so no, Wave 30, Spikes needs 4+ so no, Boulders need 8+ so no.
        double backhand = counts.getOrDefault(ApocalypseAttack.BACKHAND, 0) / (double) total;
        check(Math.abs(backhand - 60.0 / 90.0) < 0.02, "the Backhand is 60 of 90 at 3 blocks: " + backhand);
        helper.succeed();
    }

    private static ApocalypseAttack.Choice choice(int phase, double nearest, boolean inWindow, boolean afterStagger, ApocalypseAttack last, long now,
                                                  long lastEntomb, long lastMindCrush, boolean sight, boolean portal,
                                                  java.util.function.Predicate<ApocalypseAttack> ready) {
        return new ApocalypseAttack.Choice(phase, nearest, inWindow, afterStagger, last, now, lastEntomb, lastMindCrush, sight, portal, ready);
    }

    @EnhancedTest(template = ARENA, batch = "apocalypse_brain", timeoutTicks = 700)
    public static void theBrainFightsByTheRulesWithAllHisAttacks(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.NONE);
        ServerPlayer hero = d.hero();
        ApocalypseEntity boss = d.boss();
        boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(ApocalypseEntity.SPEED);
        boss.brain().setChoosing(true);
        boss.brain().setRandom(RandomSource.create(42L));
        ServerPlayer second = d.f().player(16.5, FLOOR, 16.5);
        ApocalypseTestSupport.tank(second);
        put(d.f(), hero, 6.5, FLOOR, 6.5);
        List<ApocalypseAttack> started = new ArrayList<>();
        List<Long> startedAt = new ArrayList<>();
        ApocalypseAttack[] current = {null};
        every(helper, 600, tick -> {
            // Nothing he does may end the fight, and the heroes live on.
            check(!d.fight().isOver(), "the fight is still on at tick " + tick);
            hero.setHealth(Math.max(hero.getHealth(), 500.0F));
            second.setHealth(Math.max(second.getHealth(), 500.0F));
            ApocalypseAttack now = boss.brain().attack();
            if (now != null && now != current[0]) {
                started.add(now);
                startedAt.add(d.level().getGameTime());
            }
            current[0] = now;
            if (tick < 600) {
                return false;
            }
            check(started.size() >= 6, "he attacked at least six times in 600 ticks: " + started);
            for (int i = 1; i < started.size(); i++) {
                check(started.get(i) != started.get(i - 1), "never the same attack twice in a row: " + started);
                ApocalypseAttack a = started.get(i);
                if (a == ApocalypseAttack.ENTOMB || a == ApocalypseAttack.MIND_CRUSH) {
                    for (int j = 0; j < i; j++) {
                        ApocalypseAttack b = started.get(j);
                        if ((b == ApocalypseAttack.ENTOMB || b == ApocalypseAttack.MIND_CRUSH) && b != a) {
                            check(startedAt.get(i) - startedAt.get(j) >= 100, "Entomb and Mind Crush are 100 ticks apart: " + started + " " + startedAt);
                        }
                    }
                }
            }
            check(EnumSet.copyOf(started).size() >= 4, "and he uses a variety: " + started);
            return true;
        });
    }

    private ApocalypseBossTests() {
    }
}
