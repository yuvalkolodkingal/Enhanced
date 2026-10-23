package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.MjolnirShatter;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.HelaBossConfig;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.hela.BerserkerEntity;
import com.kolodkingal.enhanced.hela.BladeSpikeEntity;
import com.kolodkingal.enhanced.hela.FenrisEntity;
import com.kolodkingal.enhanced.hela.HelaArmy;
import com.kolodkingal.enhanced.hela.HelaEntity;
import com.kolodkingal.enhanced.hela.HelaFight;
import com.kolodkingal.enhanced.hela.HelaLane;
import com.kolodkingal.enhanced.hela.HelaRules;
import com.kolodkingal.enhanced.hela.HelaState;
import com.kolodkingal.enhanced.hela.NecroswordEntity;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Hela, the boss (3.5, PLAN 6 and 12): the lane's refusals and the seal, her health scaled by the fighters, her phases,
 * every attack of hers hitting a fighter and sparing her own, the army's cap and Fenris's bar, the channel broken or healing,
 * Mjolnir caught and crushed (and Stormbreaker not), the rewards (the headdress and the right to Gungnir) and what her first
 * defeat does to Asgard, and the ends that reward nobody. Every fight test has a batch of its own (a fight is server-wide
 * state) and takes {@link TestKit#bossFixture}; the fight is placed in the test's arena ({@link HelaFight#spawnAt}, radius
 * 9) and her arrival skipped, and she chooses nothing by herself: each test starts the attack it checks.
 */
public final class HelaBossTests {
    static final Vec3 CENTRE = new Vec3(10.5, FLOOR, 10.5);

    static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        keepAsgardState(f);
        return f;
    }

    /** The state of Asgard (Odin, the seal, her defeats, the murals) comes back as it was when the test ends. */
    static void keepAsgardState(TestKit.Fixture f) {
        AsgardState state = AsgardState.get(f.level().getServer());
        boolean odin = state.odinAwake();
        int defeats = state.helaDefeats();
        long seal = state.sealReadyAt();
        boolean murals = state.muralsConquest();
        f.onClose(() -> {
            AsgardState s = AsgardState.get(f.level().getServer());
            s.setOdinAwake(odin);
            s.setHelaDefeats(defeats);
            s.setSealReadyAt(seal);
            s.setMuralsConquest(murals);
        });
    }

    /** A fight in the arena with {@code starter}: she stands in the middle, ready, choosing nothing. */
    static HelaFight fight(TestKit.Fixture f, ServerPlayer starter) {
        HelaFight fight = HelaFight.spawnAt(f.level(), f.helper().absoluteVec(CENTRE), 9.0, starter);
        fight.skipArrival();
        check(fight.boss() != null, "the fight made her");
        return fight;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The call

    /** The lane refuses before anything happens: switched off, not in Asgard, a Creative starter. The seal asks first. */
    @EnhancedTest(template = ARENA, batch = "hela_lane")
    public static void theLaneRefusesAndTheSealAsksFirst(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        HelaLane lane = new HelaLane();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        check(key(lane.refusal(player, false)).equals("message.enhanced.hela.not_asgard"), "only in Asgard");
        check(lane.refusal(player, true) == null || key(lane.refusal(player, true)).equals("message.enhanced.hela.no_asgard"),
                "an operator's forced start needs only an Asgard");
        f.config(HelaBossConfig.SUMMON_ENABLED, false);
        check(key(lane.refusal(player, false)).equals("message.enhanced.hela.disabled"), "switched off");
        f.config(HelaBossConfig.SUMMON_ENABLED, true);
        HelaLane.testSite = f.helper().absoluteVec(CENTRE);
        f.onClose(() -> HelaLane.testSite = null);
        ServerPlayer creative = f.player(6.5, FLOOR, 6.5, GameType.CREATIVE);
        check(key(lane.refusal(creative, false)).equals("message.enhanced.hela.creative"), "no Creative starter");
        check(lane.refusal(player, false) == null, "at the test site, a Survival player may call her");
        HelaLane.testSite = null;
        check(!HelaLane.onSeal(player, BlockPos.ZERO), "the first click on the seal only asks");
        check(!HelaLane.onSeal(player, BlockPos.ZERO) && BossFights.all(f.level().getServer()).isEmpty(),
                "the second breaks it, and the lane's refusal (not in Asgard) stops the fight");
        check(BossFights.lane(BossKind.HELA) instanceof HelaLane, "her lane is registered");
        helper.succeed();
    }

    private static String key(Component c) {
        return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(c);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Her body

    /** 600 for one fighter, 35 percent more for each other; past the attribute's ceiling the blows are scaled instead. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_scale")
    public static void herHealthScalesWithTheFighters(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(5.5, FLOOR, 5.5);
        HelaFight fight = fight(f, a);
        HelaEntity hela = fight.boss();
        check(Math.abs(hela.designMax() - 600.0F) < 0.01F && Math.abs(hela.getMaxHealth() - 600.0F) < 0.01F, "600 alone: " + hela.designMax());
        f.player(15.5, FLOOR, 15.5);
        helper.runAfterDelay(3, () -> {
            check(fight.fighterCount() == 2, "the second fighter joined: " + fight.fighterCount());
            check(Math.abs(hela.designMax() - 810.0F) < 0.01F, "810 for two: " + hela.designMax());
            hela.applyBody(4);
            check(Math.abs(hela.designMax() - 1230.0F) < 0.01F && hela.getMaxHealth() <= HelaEntity.REAL_CEILING + 0.01F
                    && hela.perHealth() > 1.0F, "four: 1230 points on at most 1000 of health");
            check(hela.getType().is(ModTags.BOSSES), "she is a boss");
            helper.succeed();
        });
    }

    /** Below 70 percent the army rises (Fenris with his bar); below 35 she channels. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_phase", timeoutTicks = 200)
    public static void herPhasesFollowHerHealth(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(5.5, FLOOR, 5.5);
        HelaFight fight = fight(f, a);
        HelaEntity hela = fight.boss();
        hela.setHealth(hela.getMaxHealth() * 0.69F);
        helper.runAfterDelay(2, () -> {
            check(fight.phase() == 2 && hela.state() == HelaState.RISE, "phase 2 begins with her rise: " + hela.state());
            check(hela.hurt(a.damageSources().playerAttack(a), 10.0F) == false, "nothing hurts her while she rises");
        });
        helper.runAfterDelay(30, () -> {
            List<BerserkerEntity> wave = fight.berserkers();
            check(wave.size() == fight.waveSize(), "a wave of " + fight.waveSize() + ": " + wave.size());
            FenrisEntity wolf = fight.fenris();
            check(wolf != null && wolf.isAlive() && Math.abs(wolf.getMaxHealth() - HelaBossConfig.FENRIS_HEALTH.get()) < 0.01, "Fenris came");
            check(fight.bars().second().isVisible(), "with his bar");
            check(wolf.getType().is(ModTags.HELA_ARMY) && wave.get(0).getType().is(ModTags.HELA_ARMY), "they are her army");
            fight.enterPhase(3, f.level().getGameTime());
        });
        helper.runAfterDelay(33, () -> {
            check(fight.phase() == 3 && hela.state() == HelaState.CHANNEL, "phase 3 begins with a channel: " + hela.state());
            helper.succeed();
        });
    }

    /** A wave never makes more than berserkerMax stand. */
    @EnhancedTest(template = ARENA, batch = "hela_army_cap")
    public static void theArmyIsCapped(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        HelaFight fight = fight(f, f.player(5.5, FLOOR, 5.5));
        int made = fight.raiseWave(fight.boss(), 40);
        check(made == HelaBossConfig.BERSERKER_MAX.get() && fight.berserkers().size() == made, "capped at " + made);
        check(fight.raiseWave(fight.boss(), 4) == 0, "none more while they stand");
        for (BerserkerEntity b : fight.berserkers()) {
            check(!HelaRules.canHurt(b, fight.boss()) && !HelaRules.canHurt(fight.boss(), b), "her dead and she spare each other");
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Her attacks

    /** What she may hurt: a fighter yes, her own no; and her blade's damage reaches him. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_rules")
    public static void herBladesHurtAFighter(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(10.5, FLOOR, 4.5);
        HelaFight fight = fight(f, a);
        HelaEntity hela = fight.boss();
        check(HelaRules.canHurt(hela, a), "a fighter: yes");
        check(!HelaRules.canHurt(hela, hela), "not herself");
        boolean hit = HelaRules.hurt(a, com.kolodkingal.enhanced.registry.ModDamageTypes.NECROSWORD, hela, hela, 4.0F);
        check(hit && a.getHealth() < a.getMaxHealth(), "the blade's damage reaches him: " + hit + " " + a.getHealth() + " " + f.level().getDifficulty());
        helper.succeed();
    }

    /** The volley's blades fly at the target and hurt him. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_volley", timeoutTicks = 100)
    public static void theVolleyHitsItsTarget(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(10.5, FLOOR, 4.5);
        HelaFight fight = fight(f, a);
        fight.boss().brain().start(HelaState.VOLLEY, a);
        helper.runAfterDelay(13, () -> check(!f.level().getEntitiesOfClass(NecroswordEntity.class, new AABB(f.helper().absoluteVec(CENTRE), f.helper()
                .absoluteVec(CENTRE)).inflate(12)).isEmpty() || a.getHealth() < a.getMaxHealth(), "the blades fly"));
        f.everyTick(80, t -> t > 13 && a.getHealth() < a.getMaxHealth());
    }

    /** The wall's spikes rise toward the target, hurt him, and stand solid. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_wall", timeoutTicks = 100)
    public static void theWallRisesAndBlocks(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(10.5, FLOOR, 4.5);
        HelaFight fight = fight(f, a);
        fight.boss().brain().start(HelaState.WALL, a);
        f.everyTick(80, t -> {
            if (t < 30) {
                return false;
            }
            List<BladeSpikeEntity> spikes = f.level().getEntitiesOfClass(BladeSpikeEntity.class, new AABB(f.helper().absoluteVec(CENTRE),
                    f.helper().absoluteVec(CENTRE)).inflate(14));
            check(!spikes.isEmpty(), "a wall of spikes");
            check(spikes.stream().anyMatch(BladeSpikeEntity::canBeCollidedWith), "standing, solid");
            check(spikes.stream().allMatch(fight::owns), "the fight's");
            return a.getHealth() < a.getMaxHealth();
        });
    }

    /** The rain falls on the circle at the target. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_rain", timeoutTicks = 120)
    public static void theRainFallsOnTheTarget(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(10.5, FLOOR, 4.5);
        HelaFight fight = fight(f, a);
        fight.boss().brain().start(HelaState.RAIN, a);
        f.everyTick(100, t -> t > 8 && a.getHealth() < a.getMaxHealth());
    }

    /** The storm cuts what stands on the ground as it passes, and not one who jumps over it. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_storm", timeoutTicks = 100)
    public static void theStormCutsTheGroundedOnly(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer grounded = f.player(10.5, FLOOR, 5.5);
        ServerPlayer flying = f.player(15.5, FLOOR + 3.0, 10.5);
        flying.getAbilities().mayfly = true;
        flying.getAbilities().flying = true;
        flying.setNoGravity(true);
        HelaFight fight = fight(f, grounded);
        HelaEntity hela = fight.boss();
        hela.brain().start(HelaState.STORM, grounded);
        helper.runAfterDelay(5, () -> check(hela.state() == HelaState.STORM && hela.stateTicks() >= 3,
                "the storm runs: " + hela.state() + " " + hela.stateTicks() + " foes " + fight.foes().size() + " over " + fight.isOver()
                        + " at " + hela.position().subtract(grounded.position())));
        helper.runAfterDelay(45, () -> {
            check(hela.brain().stormHit(grounded) && grounded.getHealth() < grounded.getMaxHealth(), "the grounded one is cut: hit "
                    + hela.brain().stormHit(grounded) + " health " + grounded.getHealth() + " canHurt " + HelaRules.canHurt(hela, grounded)
                    + " d " + Math.hypot(grounded.getX() - hela.getX(), grounded.getZ() - hela.getZ()) + " dy "
                    + (grounded.getY() - HelaRules.groundAt(f.level(), grounded.getX(), grounded.getY() + 0.5, grounded.getZ()))
                    + " alive " + grounded.isAlive() + " over " + fight.isOver() + " state " + hela.state());
            check(!hela.brain().stormHit(flying), "the one in the air is not");
            helper.succeed();
        });
    }

    /** Within reach, the combo's blows hurt; her blows spare her own dead. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_melee", timeoutTicks = 80)
    public static void theMeleeHurtsWithinReach(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(10.5, FLOOR, 8.5);
        HelaFight fight = fight(f, a);
        fight.boss().brain().start(HelaState.MELEE, a);
        f.everyTick(40, t -> t > 8 && a.getHealth() < a.getMaxHealth());
    }

    // ------------------------------------------------------------------------------------------------------------
    // The channel

    /** Six percent of her health dealt in the channel breaks it: she is stunned and takes more. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_break", timeoutTicks = 60)
    public static void theChannelBreaksUnderBlows(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(9.5, FLOOR, 8.5);
        HelaFight fight = fight(f, a);
        HelaEntity hela = fight.boss();
        hela.setHealth(hela.getMaxHealth() * 0.3F);
        fight.enterPhase(2, 0);
        fight.enterPhase(3, f.level().getGameTime());
        helper.runAfterDelay(2, () -> {
            check(hela.state() == HelaState.CHANNEL, "she channels: " + hela.state());
            hela.hurt(a.damageSources().playerAttack(a), 100.0F);
        });
        helper.runAfterDelay(4, () -> {
            check(hela.state() == HelaState.STUNNED, "broken: stunned: " + hela.state());
            float before = hela.getHealth();
            hela.invulnerableTime = 0;
            hela.hurt(a.damageSources().playerAttack(a), 20.0F);
            check(before - hela.getHealth() > 0.0F, "and she takes damage");
            helper.succeed();
        });
    }

    /** An unbroken channel heals her by channelHeal. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_heal", timeoutTicks = 140)
    public static void anUnbrokenChannelHeals(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(9.5, FLOOR, 8.5);
        HelaFight fight = fight(f, a);
        HelaEntity hela = fight.boss();
        hela.setHealth(hela.getMaxHealth() * 0.3F);
        fight.enterPhase(2, 0);
        fight.enterPhase(3, f.level().getGameTime());
        float start = hela.getMaxHealth() * 0.3F;
        int ticks = HelaBossConfig.CHANNEL_SECONDS.get() * 20 + 4;
        helper.runAfterDelay(ticks, () -> {
            float healed = hela.getHealth() - start;
            check(Math.abs(healed - hela.getMaxHealth() * HelaBossConfig.CHANNEL_HEAL.get()) < 1.0F, "she healed 10 percent: " + healed);
            check(hela.state() != HelaState.CHANNEL, "and stands again: " + hela.state());
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Mjolnir

    /** From the second phase a thrown Mjolnir is caught and shattered, once a minute; Stormbreaker she lets by. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_mjolnir", timeoutTicks = 60)
    public static void sheCatchesMjolnirNotStormbreaker(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer thor = f.player(10.5, FLOOR, 4.5);
        HelaFight fight = fight(f, thor);
        HelaEntity hela = fight.boss();
        ServerLevel level = f.level();
        Vec3 hand = hela.position().add(0.0, hela.getBbHeight() * 0.7, -0.5);
        ThrownWeaponEntity axe = new ThrownWeaponEntity(level, thor, new ItemStack(ModItems.STORMBREAKER.get()));
        axe.setPos(hand);
        axe.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(axe);
        ThrownWeaponEntity early = new ThrownWeaponEntity(level, thor, new ItemStack(ModItems.MJOLNIR.get()));
        early.setPos(hand);
        early.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(early);
        helper.runAfterDelay(2, () -> {
            check(early.isAlive(), "not in the first phase");
            check(axe.isAlive(), "Stormbreaker is not hers");
            early.discard();
            hela.setHealth(hela.getMaxHealth() * 0.6F);
            fight.enterPhase(2, level.getGameTime());
            hela.setState(HelaState.IDLE);
            fight.readyCatch();
            ThrownWeaponEntity hammer = new ThrownWeaponEntity(level, thor, new ItemStack(ModItems.MJOLNIR.get()));
            hammer.setPos(hand);
            hammer.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(hammer);
            helper.runAfterDelay(3, () -> {
                check(!hammer.isAlive(), "caught");
                check(MjolnirShatter.isShattered(thor), "and shattered (it comes whole to Thor again)");
                check(hela.state() == HelaState.CATCH, "she crushes it: " + hela.state());
                check(axe.isAlive(), "and Stormbreaker still flies");
                ThrownWeaponEntity again = new ThrownWeaponEntity(level, thor, new ItemStack(ModItems.MJOLNIR.get()));
                again.setPos(hand);
                again.setDeltaMovement(Vec3.ZERO);
                level.addFreshEntity(again);
                helper.runAfterDelay(3, () -> {
                    check(again.isAlive(), "once a minute");
                    again.discard();
                    axe.discard();
                    MjolnirShatter.onGone(thor);
                    helper.succeed();
                });
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The end

    /**
     * Beaten: the headdress (marked for its winner) and the right to Gungnir to the eligible only; Odin wakes, her defeats
     * are counted, the seal rests, the murals turn back, her army crumbles.
     */
    @EnhancedTest(template = ARENA, batch = "hela_fight_victory", timeoutTicks = 120)
    public static void victoryRewardsTheEligibleAndWakesOdin(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        AsgardTestSupport.ensureAsgard(f.level().getServer());
        f.config(BossConfig.REWARD_MIN_SECONDS, 0);
        AsgardState state = AsgardState.get(f.level().getServer());
        state.setOdinAwake(false);
        state.setHelaDefeats(0);
        ServerPlayer winner = f.player(5.5, FLOOR, 5.5);
        ServerPlayer idle = f.player(15.5, FLOOR, 15.5);
        HelaFight fight = fight(f, winner);
        HelaEntity hela = fight.boss();
        fight.raiseWave(hela, 3);
        helper.runAfterDelay(3, () -> {
            check(fight.isParticipant(idle.getUUID()), "both joined");
            fight.credit(winner, "damage", hela.getMaxHealth() * 0.05F);
            hela.invulnerableTime = 0;
            hela.hurt(winner.damageSources().playerAttack(winner), 100000.0F);
            check(fight.isOver() && fight.result() == BossEnd.VICTORY, "a blow that would kill her wins");
            check(hela.isAlive() && hela.state() == HelaState.DYING, "she dissolves, she does not die");
            check(hasHeaddress(winner), "the winner has the headdress");
            check(ModAttachments.ASGARD.get(winner).has(AsgardProgress.HELA_VICTOR), "and the right to Gungnir");
            check(!hasHeaddress(idle) && !ModAttachments.ASGARD.get(idle).has(AsgardProgress.HELA_VICTOR), "nothing for one who did nothing");
            AsgardState s = AsgardState.get(f.level().getServer());
            check(s.odinAwake() && s.helaDefeats() == 1, "Odin wakes, her defeat is counted");
            check(s.sealReadyAt() > f.level().getGameTime(), "the seal rests");
            check(!s.muralsConquest(), "the murals turn back");
            check(fight.berserkers().isEmpty(), "her dead crumble");
            check(BossFights.restLeft(f.level().getServer(), BossKind.HELA) > 0, "and the framework rests her");
        });
        helper.runAfterDelay(HelaState.DYING.ticks + 10, () -> {
            check(hela.isRemoved() && fight.isFinished(), "gone, and the fight with her");
            helper.succeed();
        });
    }

    private static boolean hasHeaddress(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.HELA_HEADDRESS.get())) {
                var data = stack.get(DataComponents.CUSTOM_DATA);
                return data != null && data.copyTag().getUUID(HelaFight.VICTOR_TAG).equals(player.getUUID());
            }
        }
        return false;
    }

    /** A wipe or an abandonment rewards nobody: she sinks into the seal, the murals turn back, half a rest. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_wipe", timeoutTicks = 80)
    public static void aWipeRewardsNobody(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(5.5, FLOOR, 5.5);
        HelaFight fight = fight(f, a);
        HelaEntity hela = fight.boss();
        fight.end(BossEnd.WIPE);
        check(hela.state() == HelaState.LEAVING, "she leaves");
        check(!hasHeaddress(a) && !ModAttachments.ASGARD.get(a).has(AsgardProgress.HELA_VICTOR), "no reward");
        long rest = BossFights.restLeft(f.level().getServer(), BossKind.HELA);
        check(rest > 0 && rest <= HelaBossConfig.REST_TICKS.get() / 2, "half a rest: " + rest);
        helper.runAfterDelay(HelaState.LEAVING.ticks + 5, () -> {
            check(hela.isRemoved(), "gone into the seal");
            helper.succeed();
        });
    }

    /** /kill ends the fight with no winner. */
    @EnhancedTest(template = ARENA, batch = "hela_fight_kill", timeoutTicks = 40)
    public static void aKillIsNoVictory(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = f.player(5.5, FLOOR, 5.5);
        HelaFight fight = fight(f, a);
        fight.boss().kill();
        helper.runAfterDelay(2, () -> {
            check(fight.isOver() && fight.result() == BossEnd.ABORT, "aborted: " + fight.result());
            check(!hasHeaddress(a), "no reward");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Her army outside a fight

    /** A berserker of the boss's that belongs to no fight crumbles; a loose one (an operator's) stays. */
    @EnhancedTest(template = ARENA, batch = "hela_army")
    public static void strayDeadCrumble(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        BerserkerEntity stray = new BerserkerEntity(ModEntities.BERSERKER.get(), level);
        Vec3 at = f.helper().absoluteVec(new Vec3(5.5, FLOOR, 5.5));
        stray.moveTo(at.x, at.y, at.z);
        level.addFreshEntity(stray);
        BerserkerEntity loose = new BerserkerEntity(ModEntities.BERSERKER.get(), level);
        ModAttachments.PERSISTENT_DATA.get(loose).putBoolean(HelaArmy.LOOSE_TAG, true);
        loose.moveTo(at.x + 4, at.y, at.z);
        level.addFreshEntity(loose);
        helper.runAfterDelay(3, () -> {
            check(stray.isRemoved(), "the stray crumbled");
            check(loose.isAlive(), "the loose one stands");
            loose.discard();
            helper.succeed();
        });
    }
}
