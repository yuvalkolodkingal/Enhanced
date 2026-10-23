package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestLane;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static com.kolodkingal.enhanced.gametest.BossFrameworkTests.fight;
import static com.kolodkingal.enhanced.gametest.BossFrameworkTests.fixture;
import static com.kolodkingal.enhanced.gametest.BossFrameworkTests.start;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * What a boss fight does to the rest of the mod: who is a member and a minion, the damage it credits, the orphans it
 * sweeps, the hooks in Quicksilver's slow motion, Magneto's strip, Wanda's telekinesis and the Hex, the commands and
 * the Scepter ritual that start a fight.
 */
public final class BossHookTests {

    /** A zombie that answers Quicksilver Time like a boss would. */
    private static final class HookedZombie extends Zombie implements BossHooks {
        int slow = -1;
        boolean strips;
        int stripped;
        final java.util.List<String> lightning = new java.util.ArrayList<>();

        HookedZombie(Level level) {
            super(EntityType.ZOMBIE, level);
            setNoAi(true);
        }

        @Override
        public int slowFactor(int zoneFactor) {
            return slow < 0 ? zoneFactor : slow;
        }

        @Override
        public boolean onMagnetoStrip(ServerPlayer magneto) {
            stripped++;
            return strips;
        }

        @Override
        public void onLightning(ServerPlayer thor, float amount) {
            lightning.add(thor == null ? "weather:" + amount : "thor:" + thor.getGameProfile().getName() + ":" + amount);
        }
    }

    private static HookedZombie hooked(GameTestHelper helper, double x, double z) {
        HookedZombie zombie = new HookedZombie(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(x, FLOOR, z));
        zombie.moveTo(at.x, at.y, at.z);
        helper.getLevel().addFreshEntity(zombie);
        return zombie;
    }

    private static int run(ServerPlayer player, String command) throws CommandSyntaxException {
        CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        return player.getServer().getCommands().getDispatcher().execute(command, source);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Members, minions, credit and orphans

    @EnhancedTest(template = ARENA, batch = "boss_members")
    public static void aFightKnowsItsMembersAndMinionsAndItsBossSparesItsOwn(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        TestFight fight = start(f, BossKind.ULTRON, player, fight(f, BossKind.ULTRON, Containment.NONE, false));
        var boss = ModEntities.ULTRON.get().create(level);
        var sentry = ModEntities.ULTRON_SENTRY.get().create(level);
        boss.moveTo(helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)));
        sentry.moveTo(helper.absoluteVec(new Vec3(12.5, FLOOR, 10.5)));
        fight.spawn(boss);
        fight.spawn(sentry);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 14.5));

        check(BossFights.isMember(boss) && BossFights.isMember(sentry) && BossFights.isMember(player) && !BossFights.isMember(zombie),
                "the boss, its minion and the fighters are members, a bystander is not");
        check(BossFights.isMinion(sentry) && !BossFights.isMinion(boss) && !BossFights.isMinion(zombie) && !BossFights.isMinion(player),
                "only the minion is a minion");
        check(BossFights.fightOf(sentry) == fight && fight.owns(boss) && fight.entities().size() == 2, "the fight owns what it marked");
        check(BossFights.sameFight(boss, sentry) && !BossFights.sameFight(boss, zombie) && !BossFights.sameFight(player, boss),
                "the boss and its minion are on one side");
        check(!AreaTargets.mayHurt(boss, sentry, true), "so the boss's slam spares its minion");
        check(AreaTargets.mayHurt(player, boss, false) && AreaTargets.mayHurt(player, sentry, false), "while a hero's area attack hurts both");
        check(AreaTargets.mayHurt(zombie, player, false), "and the boss side may hurt a fighter");

        // What the fight made is gone when it is.
        BossFights.clear();
        check(boss.isRemoved() && sentry.isRemoved() && !zombie.isRemoved(), "a finished fight discards its entities and only them");
        check(!BossFights.isMember(player) && !BossFights.isMinion(sentry), "and nobody is a member of nothing");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "boss_orphans")
    public static void anEntityOfABossThatNoRunningFightOwnsNeverJoinsALevel(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        Vec3 at = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));

        var stray = ModEntities.ULTRON_SENTRY.get().create(level);
        stray.moveTo(at);
        check(!level.addFreshEntity(stray), "with no fight, a Sentry that nobody owns is turned away");
        var thanos = ModEntities.THANOS_PHANTOM.get().create(level);
        thanos.moveTo(at);
        check(!level.addFreshEntity(thanos), "so is a phantom of Thanos");

        TestFight fight = start(f, BossKind.ULTRON, player, fight(f, BossKind.ULTRON, Containment.NONE, false));
        var unmarked = ModEntities.ULTRON_SENTRY.get().create(level);
        unmarked.moveTo(at);
        check(!level.addFreshEntity(unmarked), "while a fight runs, an unmarked one still is");
        var owned = ModEntities.ULTRON_SENTRY.get().create(level);
        owned.moveTo(at);
        check(level.addFreshEntity(fight.adopt(owned)), "one the fight marked joins");

        Predicate<net.minecraft.world.entity.Entity> drone = e -> e.getTags().contains("test_drone");
        BossFights.exemptFromSweep(drone);
        var droneEntity = ModEntities.ULTRON_SENTRY.get().create(level);
        droneEntity.moveTo(at);
        droneEntity.addTag("test_drone");
        boolean joined = level.addFreshEntity(droneEntity);
        BossFights.stopExempting(drone);
        check(joined, "an exempted entity (a hero's own drone) joins");

        // A projectile a player shot is a hero's own (the Ultron hero's bolt); one a boss shot belongs to its fight.
        var heroBolt = ModEntities.ULTRON_BOLT.get().create(level);
        heroBolt.moveTo(at);
        heroBolt.setOwner(player);
        check(level.addFreshEntity(heroBolt), "a bolt a player shot joins");
        var bossBolt = ModEntities.ULTRON_BOLT.get().create(level);
        bossBolt.moveTo(at);
        bossBolt.setOwner(unmarked);
        check(!level.addFreshEntity(bossBolt), "a bolt a boss shot, not marked by its fight, is turned away");
        var markedBolt = ModEntities.ULTRON_BOLT.get().create(level);
        markedBolt.moveTo(at);
        markedBolt.setOwner(owned);
        check(level.addFreshEntity(fight.adopt(markedBolt)), "and one the fight marked joins");

        fight.end(BossEnd.ABORT);
        BossFights.clear();
        var late = ModEntities.ULTRON_SENTRY.get().create(level);
        late.moveTo(at);
        fight.adopt(late);
        check(!level.addFreshEntity(late), "an entity marked for a fight that is over is an orphan too");
        droneEntity.discard();
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "boss_credit")
    public static void aPlayersDamageToTheBossAndItsMinionsGoesInTheirLedgerAndTheKillerStrikesTheLastBlow(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(8.5, FLOOR, 10.5);
        TestFight fight = start(f, BossKind.ULTRON, player, fight(f, BossKind.ULTRON, Containment.NONE, false));
        var first = ModEntities.ULTRON_SENTRY.get().create(level);
        var second = ModEntities.ULTRON_SENTRY.get().create(level);
        var boss = ModEntities.ULTRON.get().create(level);
        first.moveTo(helper.absoluteVec(new Vec3(12.5, FLOOR, 10.5)));
        second.moveTo(helper.absoluteVec(new Vec3(12.5, FLOOR, 12.5)));
        boss.moveTo(helper.absoluteVec(new Vec3(12.5, FLOOR, 8.5)));
        fight.spawn(first);
        fight.spawn(second);
        fight.spawn(boss);
        // (The real Sentry wears armour, and the ledger counts what got through it: this test is about the ledger.)
        first.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0.0);
        BossFight.Participant ledger = fight.participant(player.getUUID());

        first.hurt(level.damageSources().playerAttack(player), 6.0F);
        assertNear(ledger.damage(), 6.0, 0.01, "the damage the player did is credited");
        Wolf wolf = TestKit.spawnWithNoFreeWill(helper, EntityType.WOLF, new Vec3(14.5, FLOOR, 14.5));
        wolf.tame(player);
        second.hurt(level.damageSources().mobAttack(wolf), 4.0F);
        assertNear(ledger.damage(), 6.0, 0.01, "what a tamed wolf does is credited to nobody");
        fight.creditsEvents = false;
        first.invulnerableTime = 0;
        first.hurt(level.damageSources().playerAttack(player), 6.0F);
        assertNear(ledger.damage(), 6.0, 0.01, "a lane that credits by itself is not credited twice");

        boss.setHealth(1.0F); // (the real Ultron takes at most 15 percent of his health from one blow)
        boss.hurt(level.damageSources().playerAttack(player), 100000.0F);
        check(boss.isDeadOrDying() && ledger.struckLastBlow(), "whoever kills the boss struck the last blow");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Hooks in the heroes

    @EnhancedTest(template = ARENA, batch = "boss_hook_slow")
    public static void quicksilverTimeSlowsABossAsItSaysAndEverythingElseAsBefore(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer quicksilver = f.player(10.5, FLOOR, 10.5, Power.QUICKSILVER);
        AbilityHandler.onKey(quicksilver, 1, true);
        AbilityHandler.onKey(quicksilver, 1, false);
        PowerTicker.tick(quicksilver);
        check(Powers.get(quicksilver).slowMo(), "Quicksilver Time is on");
        SlowMotion.clear(); // (the zones are cached for the tick, and were cached before the time was on)
        int zone = SlowMotion.factor();
        Zombie plain = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        HookedZombie follows = hooked(helper, 14.5, 12.5);
        HookedZombie immune = hooked(helper, 14.5, 8.5);
        HookedZombie halved = hooked(helper, 16.5, 10.5);
        immune.slow = 0;
        halved.slow = 2;
        check(skipped(plain, 20) == 20 - 20 / zone, "an ordinary zombie runs one tick in " + zone);
        check(skipped(follows, 20) == 20 - 20 / zone, "a boss that answers the zone's factor is slowed like it");
        check(skipped(immune, 20) == 0, "a boss that answers 0 is not slowed at all");
        check(skipped(halved, 20) == 10, "one that answers 2 runs every other tick");
        helper.succeed();
    }

    private static int skipped(net.minecraft.world.entity.Entity entity, int ticks) {
        int skipped = 0;
        for (int tick = 0; tick < ticks; tick++) {
            entity.tickCount = tick;
            skipped += SlowMotion.skipTick(entity) ? 1 : 0;
        }
        return skipped;
    }

    @EnhancedTest(template = ARENA, batch = "boss_hook_strip")
    public static void magnetosStripGoesToABossFirstAndSkipsTheNormalStripWhenItIsHandled(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer magneto = f.player(4.5, FLOOR, 10.5, Power.MAGNETO);
        HookedZombie boss = hooked(helper, 12.5, 10.5);
        boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        lookAt(magneto, boss);

        boss.strips = true;
        AbilityHandler.onKey(magneto, 3, true);
        check(boss.stripped == 1 && boss.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "the boss handled it: its armour stays");
        check(!Powers.get(magneto).isReady(3, helper.getLevel().getGameTime()), "and the strip is spent");

        Powers.get(magneto).startCooldown(3, helper.getLevel().getGameTime() - 100, 1);
        boss.strips = false;
        AbilityHandler.onKey(magneto, 3, true);
        check(boss.stripped == 2 && boss.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "a boss that does not handle it is stripped like anyone");
        helper.succeed();
    }

    /** Lightning is told to an entity that wants to know, with Thor if it is his, and only when it got through. */
    @EnhancedTest(template = ARENA, batch = "boss_hook_lightning")
    public static void lightningIsToldToABossWithThorIfItIsHisAndOnlyWhenItGotThrough(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer thor = f.player(4.5, FLOOR, 10.5, Power.THOR);
        ServerPlayer nobody = f.player(4.5, FLOOR, 14.5);
        HookedZombie boss = hooked(helper, 12.5, 10.5);
        boss.setHealth(boss.getMaxHealth());

        boss.hurt(ModDamageTypes.source(level, ModDamageTypes.LIGHTNING_BEAM, thor, thor), 4.0F);
        boss.invulnerableTime = 0;
        boss.hurt(ModDamageTypes.source(level, DamageTypes.LIGHTNING_BOLT, thor, thor), 3.0F);
        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().lightningBolt(), 5.0F);
        boss.invulnerableTime = 0;
        boss.hurt(ModDamageTypes.source(level, ModDamageTypes.LIGHTNING_BEAM, nobody, nobody), 2.0F);
        String thorName = thor.getGameProfile().getName();
        check(boss.lightning.equals(java.util.List.of("thor:" + thorName + ":4.0", "thor:" + thorName + ":3.0", "weather:5.0", "weather:2.0")),
                "the bolts were told, Thor's with Thor: " + boss.lightning);

        boss.lightning.clear();
        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().generic(), 2.0F);
        boss.invulnerableTime = 0;
        boss.hurt(level.damageSources().playerAttack(thor), 2.0F);
        check(boss.lightning.isEmpty(), "nothing else is told as lightning: " + boss.lightning);
        // An entity that does not ask is left alone (and nothing breaks for it).
        Zombie plain = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 14.5));
        check(plain.hurt(level.damageSources().lightningBolt(), 2.0F), "an ordinary creature is struck as ever");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "boss_hook_telekinesis", timeoutTicks = 60)
    public static void wandasTelekinesisGoesToTheFightFirstWhenSheHoldsNothingAndWhenSheThrows(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = f.player(6.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 10.5));
        TestFight fight = start(f, BossKind.ULTRON, wanda, fight(f, BossKind.ULTRON, Containment.NONE, false));
        lookAt(wanda, zombie);

        fight.pressHandled = true;
        AbilityHandler.onKey(wanda, 3, true);
        AbilityHandler.onKey(wanda, 3, false);
        check(fight.pressed == 1 && !Telekinesis.isHolding(wanda), "the fight took the press: nothing is lifted");
        fight.pressHandled = false;
        AbilityHandler.onKey(wanda, 3, true);
        AbilityHandler.onKey(wanda, 3, false);
        check(fight.pressed == 2 && Telekinesis.isHolding(wanda) && Telekinesis.isHeld(zombie), "otherwise she lifts it as always");

        fight.launchHandled = true;
        AbilityHandler.onKey(wanda, 3, true);
        AbilityHandler.onKey(wanda, 3, false);
        check(fight.launched == 1 && fight.pressed == 2 && !Telekinesis.isHolding(wanda), "the fight took what she threw: it was let go");
        check(zombie.getDeltaMovement().lengthSqr() < 0.01, "not thrown");

        AbilityHandler.onKey(wanda, 3, true);
        AbilityHandler.onKey(wanda, 3, false);
        fight.launchHandled = false;
        AbilityHandler.onKey(wanda, 3, true);
        AbilityHandler.onKey(wanda, 3, false);
        check(fight.launched == 2 && zombie.getDeltaMovement().lengthSqr() > 0.25, "otherwise it is thrown as always");
        Telekinesis.clear();
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "boss_hook_hex")
    public static void theHexLetsAFightsMembersThroughOnlyWhenTheLaneSaysSo(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer member = f.player(10.5, FLOOR, 10.5, Power.THOR);
        ServerPlayer bystander = f.player(12.5, FLOOR, 10.5, Power.BILLY);
        Hex hex = new Hex(UUID.randomUUID(), UUID.randomUUID(), "Wanda", helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)), 5, Set.of(), false, 0L, false);
        TestFight fight = start(f, BossKind.ULTRON, member, fight(f, BossKind.ULTRON, Containment.NONE, false));
        check(!BossFights.isHexExempt(member) && HexManager.judge(hex, member, false) == HexManager.Verdict.BLOCK,
                "a hero is bounced by the wall, a fight's member too");
        fight.setHexExempt(true);
        // 3.3: only those Wanda lets in come in: the lane's word no longer opens the wall (the lift is refused where a Hex cuts it)
        check(BossFights.isHexExempt(member) && HexManager.judge(hex, member, false) == HexManager.Verdict.BLOCK,
                "even while the lane says so, its members are bounced like anyone she did not let in");
        check(!BossFights.isHexExempt(bystander) && HexManager.judge(hex, bystander, false) == HexManager.Verdict.BLOCK, "a bystander too");
        fight.setHexExempt(false);
        check(HexManager.judge(hex, member, false) == HexManager.Verdict.BLOCK, "and then the wall is the wall again");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Commands and the ritual

    @EnhancedTest(template = ARENA, batch = "boss_commands")
    public static void theBossCommandsStartListAndStopARealFightThroughItsLane(GameTestHelper helper) throws CommandSyntaxException {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        TestFight fight = fight(f, BossKind.ULTRON, Containment.NONE, true);
        TestLane lane = new TestLane(BossKind.ULTRON, p -> fight);
        BossFights.register(BossKind.ULTRON, lane);
        check(run(player, "enhanced boss list") == 0, "nothing is listed");
        check(run(player, "enhanced boss start ultron force") == 1 && lane.lastForce, "an operator's start goes to the lane, forced");
        check(run(player, "enhanced boss list") == 1, "the fight is listed");
        check(run(player, "enhanced boss start ultron") == 0 && lane.creates == 1, "a second start is refused");
        check(run(player, "enhanced boss stop") == 1 && fight.result() == BossEnd.COMMAND, "stop ends it");
        check(fight.describe().contains("ultron") && fight.describe().contains("fighters"), "and it describes itself");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "boss_ritual")
    public static void theScepterAndTheProgramStartTheUltronFightThroughItsLane(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        TestFight fight = fight(f, BossKind.ULTRON, Containment.FIELD, true);
        TestLane lane = new TestLane(BossKind.ULTRON, p -> fight);
        BossFights.register(BossKind.ULTRON, lane);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.ULTRON_PROGRAM.get()));
        ScepterRituals.useScepter(player, InteractionHand.MAIN_HAND, false);
        check(lane.creates == 0 && !BossFights.anyActive(helper.getLevel().getServer()), "an ordinary use only hints");
        player.setShiftKeyDown(true);
        check(ScepterRituals.useScepterOnBlock(player, InteractionHand.MAIN_HAND), "Sneak + use on a block is the ritual");
        check(lane.creates == 1 && BossFights.active(helper.getLevel()) == fight, "and the Ultron lane makes the fight");
        check(player.isAlive(), "without hurting anyone");
        helper.succeed();
    }

    private BossHookTests() {
    }
}
