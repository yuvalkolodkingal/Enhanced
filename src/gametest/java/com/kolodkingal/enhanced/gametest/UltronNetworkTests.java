package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronDrones;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.Log;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.MIDDLE;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.calm;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.fixture;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.onTheGround;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.put;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.sentryAt;

/**
 * Ultron's network and his army (DESIGN section 16): Upload and where it will not take him, Second Life, the Hack, the
 * Sentry Army, what a drone does and when it falls apart, Command, and how the other heroes treat him and his drones.
 * Each test has a batch of its own: drones, Sentries and a fight are server-wide state that lives from one tick to the
 * next.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class UltronNetworkTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    private static ServerPlayer hero(TestKit.Fixture f, double x, double z) {
        ServerPlayer hero = f.player(x, FLOOR, z, Power.ULTRON);
        look(hero, EAST, 0.0F);
        return hero;
    }

    private static void key(ServerPlayer player, int ability, boolean down) {
        AbilityHandler.onKey(player, ability, down);
    }

    private static void tick(ServerPlayer player) {
        PowerTicker.tick(player);
    }

    /** A drone of the hero, assembled; held in place unless the test wants it to act. */
    private static UltronSentryEntity drone(TestKit.Fixture f, ServerPlayer hero, double x, double y, double z, boolean held) {
        UltronSentryEntity drone = UltronSentryEntity.drone(f.level(), f.helper().absoluteVec(new Vec3(x, y, z)), hero.getUUID(), false, false);
        drone.setHeld(held);
        f.level().addFreshEntity(drone);
        return drone;
    }

    private static Zombie dummy(GameTestHelper helper, double x, double z) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(400.0);
        zombie.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        zombie.setHealth(400.0F);
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100000, 0, false, false));
        return zombie;
    }

    private static String describe(UltronSentryEntity drone) {
        LivingEntity target = drone.getTarget();
        return " [a drone going for " + (target == null ? "nothing" : target.getType().toShortString() + " " + target.getId()) + "]";
    }

    private static void kill(ServerLevel level, ServerPlayer player, DamageSource source) {
        player.invulnerableTime = 0;
        player.hurt(source, 1000.0F);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 4: Upload

    @GameTest(template = ARENA, batch = "ultron_upload", timeoutTicks = 100)
    public static void upload(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 4.5, 10.5);
        PowerData data = Powers.get(hero);
        UltronSentryEntity looked = drone(f, hero, 12.5, FLOOR, 10.5, true);
        UltronSentryEntity nearer = drone(f, hero, 4.5, FLOOR, 14.5, true);
        Log log = new Log(hero);
        tick(hero);

        // Without a drone to jump into, he is told.
        ServerPlayer lonely = hero(f, 4.5, 4.5);
        Log lonelyLog = new Log(lonely);
        key(lonely, 4, true);
        check(lonelyLog.drain().toldOnActionBar("message.enhanced.ultron.no_drone") && Powers.get(lonely).isReady(4, level.getGameTime()), "no Sentry, no jump");

        hero.setHealth(10.0F);
        for (var effect : List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS, MobEffects.CONFUSION, MobEffects.BLINDNESS, MobEffects.DARKNESS,
                MobEffects.DIG_SLOWDOWN)) {
            hero.addEffect(new MobEffectInstance(effect, 600, 0));
        }
        hero.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
        hero.setRemainingFireTicks(100);
        Vec3 where = looked.position();
        key(hero, 4, true);
        check(looked.isRemoved() && !nearer.isRemoved(), "the drone he looked at is the one he jumps into (and is gone)");
        check(hero.position().distanceTo(where) < 0.01, "he stands where it was: " + hero.position() + " vs " + where);
        assertNear(hero.getHealth(), 18.0, 0.01, "healed 8");
        check(!hero.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !hero.hasEffect(MobEffects.WEAKNESS) && !hero.hasEffect(MobEffects.CONFUSION)
                && !hero.hasEffect(MobEffects.BLINDNESS) && !hero.hasEffect(MobEffects.DARKNESS) && !hero.hasEffect(MobEffects.DIG_SLOWDOWN),
                "every harmful effect is cleared");
        check(hero.hasEffect(MobEffects.MOVEMENT_SPEED) && !hero.isOnFire(), "a good one is kept, and the fire is out");
        long now = level.getGameTime();
        assertNear(data.cooldownUntil(4) - now, 500.0, 0.5, "the cooldown is 25 seconds");

        // On cooldown nothing happens; ready again, and looking at nothing, it is the nearest drone.
        key(hero, 4, true);
        check(!nearer.isRemoved(), "not while it recharges");
        data.startCooldown(4, now, 0);
        look(hero, -EAST, 0.0F);
        key(hero, 4, true);
        check(nearer.isRemoved(), "looking at none, he jumps into the nearest");
        check(log.drain().actionBarCount("message.enhanced.ultron.no_drone") == 0, "and is told nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "ultron_cut", timeoutTicks = 100)
    public static void aVisionNearCutsTheNetwork(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = hero(f, 4.5, 10.5);
        PowerData data = Powers.get(hero);
        UltronSentryEntity drone = drone(f, hero, 12.5, FLOOR, 10.5, true);
        ServerPlayer vision = f.player(16.5, FLOOR, 16.5, Power.VISION);
        Log log = new Log(hero);
        tick(hero);
        hero.setHealth(10.0F);
        key(hero, 4, true);
        check(!drone.isRemoved() && hero.getHealth() == 10.0F && data.isReady(4, f.level().getGameTime()), "Upload does nothing with a Vision near");
        check(log.drain().toldOnActionBar("message.enhanced.ultron.cut_off"), "Vision has cut your connection");
        check(UltronDrones.isCut(hero), "and the HUD can say so");
        f.config(UltronConfig.VISION_CUT_RANGE, 5);
        check(!UltronDrones.isCut(hero), "a Vision further than the range does not");
        f.config(UltronConfig.VISION_CUT_RANGE, 48);
        Powers.setPower(vision, Power.NONE);
        check(!UltronDrones.isCut(hero), "and only a Vision does it");
        Powers.setPower(vision, Power.VISION);
        f.config(UltronConfig.VISION_CUT_RANGE, 0);
        key(hero, 4, true);
        check(drone.isRemoved(), "a range of 0 turns the cut off");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "ultron_gate", timeoutTicks = 100)
    public static void uploadWillNotCrossAHexOrLeaveTheArena(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(UltronConfig.VISION_CUT_RANGE, 0);
        ServerLevel level = f.level();

        // A Hex that does not know him: a hero is bounced off the wall, and so is his jump.
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        key(wanda, 5, true);
        check(HexManager.ownedBy(level, wanda.getUUID()).isPresent(), "Wanda raises her Hex");
        ServerPlayer outside = hero(f, 1.5, 10.5);
        UltronSentryEntity inside = drone(f, outside, 10.5, FLOOR, 10.5, true);
        look(outside, EAST, 0.0F);
        PowerData data = Powers.get(outside);
        key(outside, 4, true);
        check(!inside.isRemoved() && outside.getX() < f.helper().absoluteVec(new Vec3(3.0, 0, 0)).x && data.isReady(4, level.getGameTime()),
                "the Hex wall stops the jump: he is where he was, the drone is whole and nothing recharges");
        HexManager.dissolveAllOwnedBy(level.getServer(), wanda.getUUID());
        Powers.setPower(wanda, Power.NONE);
        key(outside, 4, true);
        check(inside.isRemoved(), "with no Hex between them it works");

        // An arena: the fighters of the fight cannot leave it, by a jump either.
        f.config(UltronBossConfig.ISLAND_RADIUS, 1);
        ServerPlayer fighter = hero(f, 10.5, 10.5);
        UltronFight fight = onTheGround(f, fighter);
        calm(fight);
        Vec3 corner = f.helper().absoluteVec(new Vec3(1.0, FLOOR, 1.0));
        check(!fight.arena().contains(corner), "the corner of the test floor is outside the field");
        UltronSentryEntity beyond = drone(f, fighter, 1.0, FLOOR, 1.0, true);
        look(fighter, 0.0F, 0.0F);
        lookAt(fighter, beyond);
        Vec3 stand = fighter.position();
        key(fighter, 4, true);
        check(!beyond.isRemoved() && fighter.position().distanceTo(stand) < 0.01, "the field does not let him jump out of the arena");
        UltronSentryEntity within = drone(f, fighter, 12.5, FLOOR, 10.5, true);
        lookAt(fighter, within);
        key(fighter, 4, true);
        check(within.isRemoved(), "but a jump inside it is his");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The passive: Second Life

    @GameTest(template = ARENA, batch = "ultron_second_life", timeoutTicks = 100)
    public static void secondLife(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(UltronConfig.VISION_CUT_RANGE, 0);
        ServerLevel level = f.level();
        DamageSource blow = level.damageSources().generic();

        // A Totem of Undying works first, and leaves Second Life as it was.
        ServerPlayer hero = hero(f, 4.5, 10.5);
        PowerData data = Powers.get(hero);
        UltronSentryEntity drone = drone(f, hero, 14.5, FLOOR, 10.5, true);
        tick(hero);
        hero.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        kill(level, hero, blow);
        check(hero.isAlive() && hero.getOffhandItem().isEmpty() && !drone.isRemoved() && data.isReady(4, level.getGameTime()),
                "the Totem saved him, and Second Life is still there");

        // Then Second Life itself.
        Vec3 where = drone.position();
        kill(level, hero, blow);
        check(hero.isAlive() && hero.getHealth() > 0.0F, "a lethal blow does not kill him");
        assertNear(hero.getHealth(), hero.getMaxHealth() * 0.25, 0.01, "he comes back with a quarter of his health");
        check(hero.position().distanceTo(where) < 0.01 && drone.isRemoved(), "in the drone's place, and it is gone");
        MobEffectInstance resistance = hero.getEffect(MobEffects.DAMAGE_RESISTANCE);
        check(resistance != null && resistance.getAmplifier() == 3 && resistance.getDuration() > 50 && resistance.getDuration() <= 60, "Resistance IV for three seconds");
        assertNear(data.cooldownUntil(4) - level.getGameTime(), 6000.0, 0.5, "and five minutes to recharge");
        hero.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        UltronSentryEntity another = drone(f, hero, 4.5, FLOOR, 14.5, true);
        kill(level, hero, blow);
        check(!hero.isAlive() && another.isRemoved(), "recharging, the next blow kills him, and his army with him");

        // No drone, no save.
        ServerPlayer alone = hero(f, 10.5, 4.5);
        kill(level, alone, blow);
        check(!alone.isAlive(), "without a drone within 64 blocks he dies");

        // The void and /kill are not saved.
        for (DamageSource unstoppable : List.of(level.damageSources().fellOutOfWorld(), level.damageSources().genericKill())) {
            ServerPlayer doomed = hero(f, 14.5, 14.5);
            UltronSentryEntity near = drone(f, doomed, 16.5, FLOOR, 14.5, true);
            kill(level, doomed, unstoppable);
            check(!doomed.isAlive() && near.isRemoved(), unstoppable.type().msgId() + " kills him, and his drones fall apart with him");
        }

        // A Vision near cuts it.
        ServerPlayer cut = hero(f, 8.5, 16.5);
        UltronSentryEntity companion = drone(f, cut, 10.5, FLOOR, 16.5, true);
        ServerPlayer vision = f.player(18.5, FLOOR, 18.5, Power.VISION);
        f.config(UltronConfig.VISION_CUT_RANGE, 48);
        kill(level, cut, blow);
        check(!cut.isAlive() && companion.isRemoved(), "with a Vision near there is no second life");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sneak + B: the Hack

    @GameTest(template = ARENA, batch = "ultron_hack", timeoutTicks = 200)
    public static void hack(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(UltronConfig.VISION_CUT_RANGE, 0);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 4.5, 10.5);
        PowerData data = Powers.get(hero);
        UltronFight fight = onTheGround(f, hero);
        calm(fight);
        UltronSentryEntity first = sentryAt(f, fight, 10.5, 10.5);
        UltronSentryEntity second = sentryAt(f, fight, 10.5, 12.5);
        UltronSentryEntity third = sentryAt(f, fight, 10.5, 8.5);
        UltronSentryEntity victim = sentryAt(f, fight, 14.5, 10.5);
        for (UltronSentryEntity sentry : List.of(first, second, third, victim)) {
            sentry.setHeld(true);
        }
        int budget = fight.budgetLeft();
        int alive = fight.sentries().size();
        Log log = new Log(hero);
        hero.setShiftKeyDown(true);
        look(hero, EAST, 0.0F);
        lookAt(hero, first);
        key(hero, 4, true);
        check(data.is(Live.CASTING) && UltronDrones.isHacking(hero) && !first.isRemoved(), "Sneak + B: the hand reaches out for twenty ticks");
        UltronSentryEntity[] taken = new UltronSentryEntity[1];
        f.everyTick(150, t -> {
            tick(hero);
            if (t == 18) {
                check(!first.isRemoved() && data.is(Live.CASTING), "not yet");
            }
            if (t == 22) {
                check(first.isRemoved() && !data.is(Live.CASTING), "and then it is his");
                List<UltronSentryEntity> mine = UltronDrones.drones(hero);
                check(mine.size() == 1, "one drone of his");
                taken[0] = mine.get(0);
                check(taken[0].isHacked() && hero.getUUID().equals(taken[0].getOwnerUUID()) && taken[0].eyes() == UltronSentryEntity.Eyes.RED,
                        "owned, hacked, red-eyed");
                check(taken[0].lifetime() > 1700 && taken[0].lifetime() <= 1800, "with a life of its own: " + taken[0].lifetime());
                check(BossFights.fightOf(taken[0]) == null && !fight.sentries().contains(taken[0]), "and it is no longer one of the fight's robots");
                check(fight.sentries().size() == alive - 1 && fight.budgetLeft() == budget, "the count falls by one, the budget does not change");
                // A second one, and a third that he cannot have (two hacked at a time).
                lookAt(hero, second);
                key(hero, 4, true);
            }
            if (t == 45) {
                check(second.isRemoved() && UltronDrones.hackedCount(hero) == 2, "the second is his");
                lookAt(hero, third);
                key(hero, 4, true);
                check(!UltronDrones.isHacking(hero) && !third.isRemoved(), "not a third: two hacked at a time");
                check(log.drain().toldOnActionBar("message.enhanced.ultron.drones.max"), "he is told");
                taken[0].setHeld(false);
                for (UltronSentryEntity sentry : List.of(third, victim)) {
                    sentry.setHealth(sentry.getMaxHealth());
                }
            }
            if (t == 140) {
                // The hacked Sentries fight for him: the fight's Sentries take their plasma.
                var body = fight.boss();
                check(third.getHealth() < third.getMaxHealth() || victim.getHealth() < victim.getMaxHealth() || third.isRemoved() || victim.isRemoved()
                                || (body != null && body.getHealth() < body.getMaxHealth()),
                        "his hacked Sentries hurt the fight's: " + third.getHealth() + " " + victim.getHealth());
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "ultron_hack_limits", timeoutTicks = 100)
    public static void theHackNeverTakesABodyOrNothing(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(UltronConfig.VISION_CUT_RANGE, 0);
        ServerPlayer hero = hero(f, 4.5, 10.5);
        PowerData data = Powers.get(hero);
        UltronFight fight = onTheGround(f, hero);
        calm(fight);
        var body = UltronTestSupport.boss(fight);
        put(f, body, 10.5, FLOOR + 1.0, 10.5);
        Log log = new Log(hero);
        hero.setShiftKeyDown(true);
        lookAt(hero, body);
        key(hero, 4, true);
        check(!UltronDrones.isHacking(hero) && !data.is(Live.CASTING) && log.drain().toldOnActionBar("message.enhanced.ultron.hack.none"),
                "there is no hacking a body: nothing to hack");
        UltronSentryEntity close = sentryAt(f, fight, 14.5, 10.5);
        close.setHeld(true);
        lookAt(hero, close);
        key(hero, 4, true);
        check(UltronDrones.isHacking(hero), "a Sentry, yes");
        // It dies (or is lost) in the cast: nothing is his.
        close.discard();
        tick(hero);
        check(!UltronDrones.isHacking(hero) && !data.is(Live.CASTING) && UltronDrones.drones(hero).isEmpty(), "the cast is lost with it");
        // An owner's drone is not an enemy.
        UltronSentryEntity mine = drone(f, hero, 12.5, FLOOR, 10.5, true);
        lookAt(hero, mine);
        key(hero, 4, true);
        check(!UltronDrones.isHacking(hero), "nor is a drone of his own");
        // And with a Vision near, no Sentry at all: it is part of the network.
        UltronSentryEntity enemy = sentryAt(f, fight, 12.5, 14.5);
        enemy.setHeld(true);
        f.player(16.5, FLOOR, 16.5, Power.VISION);
        f.config(UltronConfig.VISION_CUT_RANGE, 48);
        lookAt(hero, enemy);
        log.drain();
        key(hero, 4, true);
        check(!UltronDrones.isHacking(hero) && log.drain().toldOnActionBar("message.enhanced.ultron.cut_off"), "Vision has cut the Hack too");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 5: the Sentry Army

    @GameTest(template = ARENA, batch = "ultron_army", timeoutTicks = 200)
    public static void sentryArmy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 10.5, 10.5);
        PowerData data = Powers.get(hero);
        Log log = new Log(hero);
        tick(hero);
        key(hero, 5, true);
        List<UltronSentryEntity> army = UltronDrones.drones(hero);
        check(army.size() == 3, "N calls three Sentries, not " + army.size());
        assertNear(data.cooldownUntil(5) - level.getGameTime(), 400.0, 0.5, "the cooldown is 20 seconds");
        for (UltronSentryEntity drone : army) {
            check(drone.isDrone() && hero.getUUID().equals(drone.getOwnerUUID()) && !drone.isHacked(), "owned by him");
            check(drone.isAssembling() && !drone.hurt(level.damageSources().generic(), 5.0F), "assembling, and untouchable while it does");
            assertNear(drone.getMaxHealth(), 24.0, 1.0E-4, "24 health");
            check(drone.lifetime() == 1800, "and ninety seconds to live");
            check(drone.eyes() == UltronSentryEntity.Eyes.RED, "red eyes");
            check(BossFights.fightOf(drone) == null && drone.getOwnerUUID() != null, "no fight's, so the sweep leaves it alone");
        }
        key(hero, 5, true);
        check(UltronDrones.drones(hero).size() == 3, "not again in the cooldown");

        f.everyTick(60, t -> {
            tick(hero);
            if (t == 22) {
                check(army.stream().noneMatch(UltronSentryEntity::isAssembling), "twenty ticks of assembling, and they are ready");
                // Up to six at a time: three more, and then no more.
                data.startCooldown(5, level.getGameTime(), 0);
                key(hero, 5, true);
                check(UltronDrones.drones(hero).size() == 6, "six at most");
                data.startCooldown(5, level.getGameTime(), 0);
                log.drain();
                key(hero, 5, true);
                check(UltronDrones.drones(hero).size() == 6 && log.drain().toldOnActionBar("message.enhanced.ultron.drones.max"), "and he is told so");
                // Two fall: there is room for two.
                army.get(0).discard();
                army.get(1).discard();
                key(hero, 5, true);
                check(UltronDrones.drones(hero).size() == 6, "room for the two that were lost, and no more: " + UltronDrones.drones(hero).size());
                // A drone lives for its time.
                army.get(2).setLifetime(2);
            }
            if (t == 26) {
                check(army.get(2).isRemoved(), "at the end of its life it falls apart");
                return true;
            }
            return false;
        });
    }

    /**
     * The hero stands three and a half blocks in from two walls, not in the corner: the drone that catches up appears
     * three blocks from him at a random angle, and from the corner that spot can be past the arena's wall, in a chunk
     * that is not the test's. The GameTest server does not tick a creature there (it forces only the test's own chunks,
     * and its simulation distance is 0), so that drone would keep the target it had, and the test failed on about one
     * placement of the arena in ten.
     */
    @GameTest(template = ARENA, batch = "ultron_drones", timeoutTicks = 300)
    public static void dronesFollowAndFight(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer hero = hero(f, 3.5, 3.5);
        UltronSentryEntity a = drone(f, hero, 5.5, FLOOR + 1.0, 5.5, false);
        UltronSentryEntity b = drone(f, hero, 5.5, FLOOR + 1.0, 3.5, false);
        // One of them is far from him (the far corner, high up: more than 24 blocks): it catches up by teleporting.
        UltronSentryEntity far = drone(f, hero, 20.5, FLOOR + 5.0, 20.5, false);
        check(far.distanceTo(hero) > UltronSentryEntity.DRONE_LEASH, "the far drone starts beyond the leash: " + far.distanceTo(hero));
        Zombie hostile = dummy(helper, 11.5, 3.5);
        Zombie distant = dummy(helper, 16.5, 16.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.5, FLOOR, 10.5));
        ServerPlayer bystander = f.player(10.5, FLOOR, 8.5);
        tick(hero);
        f.everyTick(240, t -> {
            tick(hero);
            if (t == 3) {
                check(far.distanceTo(hero) < 8.0, "more than 24 blocks away, a drone catches up at once: " + far.distanceTo(hero));
            }
            if (t == 100) {
                check(hostile.getHealth() < 400.0F, "a hostile creature within 12 blocks of him is shot: " + hostile.getHealth());
                double lost = 400.0 - hostile.getHealth();
                check(Math.abs(lost / 3.0 - Math.round(lost / 3.0)) < 0.01, "in bolts of 3 (or a punch of 6): " + lost);
                check(distant.getHealth() == 400.0F && a.getTarget() != distant && b.getTarget() != distant, "one 18 blocks from him is not his business yet");
                check(pig.getHealth() == pig.getMaxHealth(), "a pig is not an enemy");
                check(bystander.getHealth() == 20.0F && a.getTarget() != bystander && b.getTarget() != bystander, "nor is a player, with PvP off");
                // What he strikes, they strike: the far zombie, hurt by him.
                distant.hurt(level.damageSources().playerAttack(hero), 1.0F);
                check(hero.getLastHurtMob() == distant, "it is what he last hit");
            }
            if (t > 100 && a.getTarget() == distant && b.getTarget() == distant && far.getTarget() == distant) {
                check(List.of(a, b, far).stream().allMatch(d -> d.isAlive() && d.distanceTo(hero) <= 49.0), "they stay with him");
                return true;
            }
            check(t < 235, "his drones go for what he struck: " + describe(a) + describe(b) + describe(far));
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "ultron_drone_players", timeoutTicks = 200)
    public static void dronesNeverHuntAPlayerUnlessPvpAndProvoked(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = hero(f, 3.5, 3.5);
        UltronSentryEntity a = drone(f, hero, 5.5, FLOOR + 1.0, 5.5, false);
        UltronSentryEntity b = drone(f, hero, 5.5, FLOOR + 1.0, 3.5, false);
        ServerPlayer attacker = f.player(9.5, FLOOR, 3.5);
        ServerPlayer bystander = f.player(9.5, FLOOR, 7.5);
        tick(hero);
        f.pvp(true);
        f.everyTick(160, t -> {
            tick(hero);
            if (t == 30) {
                check(a.getTarget() != attacker && a.getTarget() != bystander && b.getTarget() != attacker && b.getTarget() != bystander,
                        "with PvP on, players who did nothing are left alone");
                // A player hits one of his drones.
                a.invulnerableTime = 0;
                check(a.hurt(helper.getLevel().damageSources().playerAttack(attacker), 1.0F), "a player strikes a drone");
                check(UltronDrones.provoked(hero, attacker) && !UltronDrones.provoked(hero, bystander), "that player, and no other, is fair game");
            }
            if (t == 60) {
                check(a.getTarget() == attacker || b.getTarget() == attacker, "the drones go for the one who hit one of them");
                check(a.getTarget() != bystander && b.getTarget() != bystander, "and not for anyone else");
                f.pvp(false);
            }
            if (t == 100) {
                check(a.getTarget() != attacker && b.getTarget() != attacker, "with PvP off they let go");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "ultron_drone_end", timeoutTicks = 100)
    public static void dronesFallApartWithTheirMaster(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        f.config(UltronConfig.VISION_CUT_RANGE, 0);

        ServerPlayer loses = hero(f, 4.5, 4.5);
        UltronSentryEntity one = drone(f, loses, 6.5, FLOOR, 4.5, true);
        Powers.setPower(loses, Power.NONE);
        check(one.isRemoved(), "he loses the power: his drones fall apart");

        ServerPlayer leaves = hero(f, 4.5, 8.5);
        UltronSentryEntity two = drone(f, leaves, 6.5, FLOOR, 8.5, true);
        HeroLifecycle.onLogout(leaves);
        check(two.isRemoved(), "he logs out");

        ServerPlayer travels = hero(f, 4.5, 12.5);
        UltronSentryEntity three = drone(f, travels, 6.5, FLOOR, 12.5, true);
        HeroLifecycle.onChangeDimension(travels);
        check(three.isRemoved(), "he changes dimension");

        ServerPlayer dies = hero(f, 4.5, 16.5);
        UltronSentryEntity four = drone(f, dies, 6.5, FLOOR, 16.5, true);
        kill(level, dies, level.damageSources().fellOutOfWorld());
        check(!dies.isAlive() && four.isRemoved(), "he dies");

        // A drone whose master is not there at all (a restart that kept the drone) goes within a tick.
        UltronSentryEntity five = UltronSentryEntity.drone(level, helper.absoluteVec(new Vec3(14.5, FLOOR, 4.5)), UUID.randomUUID(), false, false);
        level.addFreshEntity(five);
        f.everyTick(5, t -> {
            if (t == 3) {
                check(five.isRemoved(), "a drone without its master falls apart by itself");
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sneak + N: Command

    @GameTest(template = ARENA, batch = "ultron_command", timeoutTicks = 200)
    public static void command(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = hero(f, 1.5, 1.5);
        UltronSentryEntity a = drone(f, hero, 3.5, FLOOR + 1.0, 3.5, false);
        UltronSentryEntity b = drone(f, hero, 3.5, FLOOR + 1.0, 1.5, false);
        UltronSentryEntity far = drone(f, hero, 12.5, FLOOR + 1.0, 12.5, false);
        // Well away from him, so that the order is the only reason for what they do.
        Zombie fixed = dummy(helper, 18.5, 8.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.5, FLOOR, 1.5));
        tick(hero);
        hero.setShiftKeyDown(true);
        lookAt(hero, pig);
        key(hero, 5, true);
        helper.runAfterDelay(3, () -> {
            check(a.getTarget() != pig && b.getTarget() != pig, "a pig is not an order they take");
            lookAt(hero, fixed);
            key(hero, 5, true);
        });
        helper.runAfterDelay(8, () -> {
            check(a.getTarget() == fixed && b.getTarget() == fixed && far.getTarget() == fixed, "every drone attacks what he looks at");
            // Looking at nothing: they gather, and the one more than 24 blocks away comes to him.
            put(f, far, 20.5, FLOOR + 1.0, 20.5);
            look(hero, -EAST, -80.0F);
            key(hero, 5, true);
            check(far.distanceTo(hero) < 6.0, "the one beyond 24 blocks is at his side: " + far.distanceTo(hero));
        });
        helper.runAfterDelay(14, () -> {
            check(a.getTarget() != fixed && b.getTarget() != fixed, "and the order is lifted");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The other heroes

    @GameTest(template = ARENA, batch = "ultron_others", timeoutTicks = 300)
    public static void otherHeroesAndUltron(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = f.level();
        f.pvp(true);
        f.config(UltronConfig.VISION_CUT_RANGE, 0);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        UltronSentryEntity drone = drone(f, hero, 3.5, FLOOR + 1.0, 14.5, true);
        ServerPlayer magneto = f.player(8.5, FLOOR, 4.5, Power.MAGNETO);
        ServerPlayer billy = f.player(8.5, FLOOR, 6.5, Power.BILLY);
        tick(hero);

        // Magneto: vibranium is metal (3.1, D-M 2.3). He lifts the hero, in every look, and his drones too.
        check(Telekinesis.canLift(magneto, hero, Telekinesis.Mode.MAGNETO), "Magneto lifts Ultron: vibranium is metal now");
        check(Telekinesis.canLift(magneto, drone, Telekinesis.Mode.MAGNETO), "and he lifts a drone");
        UltronBoltEntity bolt = UltronBoltEntity.create(level, hero, f.helper().absoluteVec(new Vec3(6.5, FLOOR + 1.0, 10.5)), new Vec3(0.2, 0, 0), 4.0F,
                UltronBoltEntity.Palette.PRIME);
        level.addFreshEntity(bolt);
        check(!MagnetoAbilities.isMetalProjectile(bolt), "plasma is not metal: the Magnetic Shield does not repel it");
        // Billy catches his plasma in flight.
        check(Telekinesis.canLift(billy, bolt, Telekinesis.Mode.BILLY), "Billy catches a bolt in flight");
        bolt.discard();

        // Wanda's shield stops the plasma and pays for it, and the Slam is no projectile.
        ServerPlayer wanda = f.player(9.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        PowerData shield = Powers.get(wanda);
        shield.raiseChaosShield(level.getGameTime(), 0, 30.0F);
        tick(wanda);
        look(hero, EAST, 0.0F);
        key(hero, 1, true);
        float health = wanda.getHealth();
        f.everyTick(40, t -> {
            tick(hero);
            tick(wanda);
            if (t == 4) {
                key(hero, 1, false);
            }
            if (t == 20) {
                check(wanda.getHealth() == health, "the shield took the plasma: not a point of her");
                check(shield.shieldStrength() < 30.0F && ((30.0F - shield.shieldStrength()) % 4.0F) < 1.0E-3F, "and paid its damage, 4 a bolt: " + shield.shieldStrength());
                float strength = shield.shieldStrength();
                look(hero, EAST, 0.0F);
                put(f, wanda, 6.5, FLOOR, 10.5);
                hero.setShiftKeyDown(true);
                key(hero, 3, true);
                hero.setShiftKeyDown(false);
                check(wanda.getHealth() < health, "the Gravity Slam is not a thing a shield stops: " + wanda.getHealth());
                check(shield.shieldStrength() == strength, "and costs it nothing");
            }
            if (t == 22) {
                // Quicksilver Time hangs his plasma like an arrow.
                ServerPlayer quicksilver = f.player(15.5, FLOOR, 15.5, Power.QUICKSILVER);
                key(quicksilver, 1, true);
                key(quicksilver, 1, false);
                tick(quicksilver);
                SlowMotion.clear();
                UltronBoltEntity slowed = UltronBoltEntity.create(level, hero, f.helper().absoluteVec(new Vec3(14.5, FLOOR + 1.0, 15.5)), new Vec3(0.5, 0, 0), 4.0F,
                        UltronBoltEntity.Palette.PRIME);
                level.addFreshEntity(slowed);
                SlowMotion.serverTick(level);
                check(SlowMotion.timeScale(slowed) < 0.5, "Quicksilver Time slows a bolt like an arrow: " + SlowMotion.timeScale(slowed));
                slowed.discard();
                SlowMotion.clear();
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "ultron_hex_pass", timeoutTicks = 100)
    public static void aHexThatLetsHimInLetsHisDronesThrough(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer hero = hero(f, 10.5, 10.5);
        UltronSentryEntity drone = drone(f, hero, 12.5, FLOOR, 10.5, true);
        Vec3 centre = f.helper().absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        Hex strangers = new Hex(UUID.randomUUID(), UUID.randomUUID(), "Wanda", centre, 5, Set.of(), false, 0L, false);
        Hex friends = new Hex(UUID.randomUUID(), UUID.randomUUID(), "Wanda", centre, 5, Set.of(hero.getUUID()), false, 0L, false);
        check(HexManager.judge(strangers, hero, false) == HexManager.Verdict.BLOCK, "a Hex that does not know him bounces him");
        check(HexManager.judge(strangers, drone, false) == HexManager.Verdict.GAMBLE, "and is as deadly to his drones as to any creature");
        check(HexManager.judge(friends, hero, false) == HexManager.Verdict.PASS && HexManager.judge(friends, drone, false) == HexManager.Verdict.PASS,
                "one that let him in lets his drones through, like pets");
        helper.succeed();
    }
}
