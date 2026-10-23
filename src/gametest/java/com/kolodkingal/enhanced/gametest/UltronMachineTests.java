package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.ultron.UltronAttack;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronBrain;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.UltronState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.MIDDLE;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.boss;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.calm;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.fixture;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.onTheGround;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.onTheIsland;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.put;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.sentryAt;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.tank;

/**
 * The machines among the heroes: Quicksilver's time, Wanda's shield, hand and Hex, Magneto's pull, the Mind Stone's
 * beam and Thor's lightning on the bodies and the Sentries, what the Sentry does and in what order, how the waves come
 * and how the budget runs out, and that nothing he does ever breaks a block.
 */
public final class UltronMachineTests {

    // ------------------------------------------------------------------------------------------------------------
    // Quicksilver

    @EnhancedTest(template = ARENA, batch = "ultron_slow")
    public static void quicksilverTimeSlowsSentriesNotUltron(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer quicksilver = f.player(MIDDLE.x, FLOOR, MIDDLE.z, Power.QUICKSILVER);
        UltronFight fight = onTheGround(f, quicksilver);
        calm(fight);
        UltronEntity boss = boss(fight);
        UltronSentryEntity sentry = sentryAt(f, fight, 12.5, 10.5);
        sentry.setHeld(true);
        UltronBoltEntity bolt = UltronBoltEntity.create(level, boss, f.helper().absoluteVec(new Vec3(8.5, FLOOR + 1.5, 12.5)), new Vec3(0.5, 0.0, 0.0), 4.0F,
                UltronBoltEntity.Palette.PRIME);
        fight.spawn(bolt);
        Zombie plain = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 8.5));

        AbilityHandler.onKey(quicksilver, 1, true);
        AbilityHandler.onKey(quicksilver, 1, false);
        PowerTicker.tick(quicksilver);
        check(Powers.get(quicksilver).slowMo(), "Quicksilver Time is on");
        SlowMotion.clear();
        int zone = SlowMotion.factor();
        check(zone > 1, "the zone slows time");
        check(skipped(plain, 20) == 20 - 20 / zone, "an ordinary zombie runs one tick in " + zone);
        check(skipped(sentry, 20) == 20 - 20 / zone, "a Sentry runs one tick in " + zone + " too");
        check(skipped(boss, 20) == 0, "but the machine on the network is not slowed at all");
        check(boss.slowFactor(zone) == 0 && sentry.slowFactor(zone) == zone, "the hook says the same");

        // A bolt is slowed like an arrow: its speed is divided, and it lives by its own time.
        Vec3 before = bolt.getDeltaMovement();
        SlowMotion.serverTick(level);
        assertNear(SlowMotion.timeScale(bolt), 1.0 / zone, 1.0E-6, "the bolt's time runs " + zone + " times slower");
        assertNear(bolt.getDeltaMovement().length(), before.length() / zone, 1.0E-4, "so it crawls");
        helper.succeed();
    }

    private static int skipped(Entity entity, int ticks) {
        int skipped = 0;
        for (int tick = 0; tick < ticks; tick++) {
            entity.tickCount = tick;
            skipped += SlowMotion.skipTick(entity) ? 1 : 0;
        }
        return skipped;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Wanda

    @EnhancedTest(template = ARENA, batch = "ultron_shield", timeoutTicks = 400)
    public static void chaosShieldVersusUltron(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer wanda = f.player(MIDDLE.x, FLOOR, MIDDLE.z, Power.SCARLET_WITCH);
        UltronFight fight = onTheGround(f, wanda);
        calm(fight);
        UltronEntity boss = boss(fight);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        put(f, wanda, 10.5, FLOOR, 14.5);
        put(f, boss, 10.5, FLOOR + 2.0, 5.5);
        PowerData data = Powers.get(wanda);
        Vec3 chest = f.helper().absoluteVec(new Vec3(10.5, FLOOR + 1.0, 14.5));
        Vec3 from = f.helper().absoluteVec(new Vec3(10.5, FLOOR + 1.0, 8.5));

        // Without the shield a bolt hurts.
        fight.spawn(UltronBoltEntity.create(level, boss, from, chest.subtract(from).normalize().scale(1.7), 6.0F, UltronBoltEntity.Palette.PRIME));
        // (it needs a few ticks to arrive: see the steps below)
        int[] step = {0};
        int[] since = {0};
        float[] spared = {0};
        f.everyTick(380, tick -> {
            since[0]++;
            switch (step[0]) {
                case 0 -> {
                    if (since[0] == 12) {
                        check(wanda.getHealth() < 19.5F, "a bolt hurts her when no shield is up: " + wanda.getHealth());
                        check(level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds()).isEmpty(), "and is spent");
                        // Now the shield: three bolts of 6 take 18 of its 30, and not a point of her.
                        wanda.setHealth(20.0F);
                        wanda.invulnerableTime = 0;
                        data.raiseChaosShield(level.getGameTime(), 0, 30.0F);
                        check(shieldUp(wanda), "the shield is up");
                        for (int i = 0; i < 3; i++) {
                            Vec3 start = from.add(0.3 * (i - 1), 0.0, 0.0);
                            fight.spawn(UltronBoltEntity.create(level, boss, start, chest.subtract(start).normalize().scale(1.7), 6.0F, UltronBoltEntity.Palette.PRIME));
                        }
                        step[0] = 1;
                        since[0] = 0;
                    }
                }
                case 1 -> {
                    if (since[0] == 12) {
                        assertNear(wanda.getHealth(), 20.0, 1.0E-4, "the shield took the three bolts: not a point of her");
                        assertNear(data.shieldStrength(), 12.0, 1.0E-3, "and paid their 18 out of 30");
                        check(level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds()).isEmpty(), "the bolts are gone");
                        // The boss's own Volley: he is a Sentry-Ultron now, with one burst of three.
                        boss.applyBody(2, 1);
                        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
                        put(f, boss, 10.5, FLOOR + 2.0, 5.5);
                        data.raiseChaosShield(level.getGameTime(), 0, 30.0F);
                        check(boss.brain().force(boss, fight, UltronAttack.VOLLEY), "the Volley starts");
                        step[0] = 2;
                        since[0] = 0;
                    }
                }
                case 2 -> {
                    if (since[0] > 5 && boss.brain().current() == null && level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds()).isEmpty()) {
                        assertNear(wanda.getHealth(), 20.0, 1.0E-4, "the Volley did not reach her");
                        double paid = 30.0 - data.shieldStrength();
                        check(paid > 5.9 && paid < 18.1 && Math.abs(paid / 6.0 - Math.round(paid / 6.0)) < 1.0E-3, "the shield paid 6 for each bolt it stopped: " + paid);
                        // Beams: ten rays, one price a step, whatever the number that touch the shield.
                        boss.applyBody(0, 1);
                        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
                        put(f, boss, 10.5, FLOOR + 3.0, 3.5);
                        data.raiseChaosShield(level.getGameTime(), 0, 30.0F);
                        check(boss.brain().force(boss, fight, UltronAttack.BEAMS), "the Beams start");
                        step[0] = 3;
                        since[0] = 0;
                    }
                }
                default -> {
                    if (since[0] == 34) {
                        assertNear(data.shieldStrength(), 24.0, 1.0E-3, "the first step of the Beams cost 6");
                        assertNear(wanda.getHealth(), 20.0, 1.0E-4, "and she is untouched");
                    }
                    check(wanda.getHealth() > 19.99F, "the beams are stopped at the shield: " + wanda.getHealth() + " at " + since[0]);
                    if (since[0] > 5 && boss.brain().current() == null) {
                        assertNear(data.shieldStrength(), 0.0, 1.0E-3, "five steps of 6 wore the whole shield out");
                        check(!data.chaosShieldActive(), "and it shattered");
                        return true;
                    }
                }
            }
            check(tick < 370, "step " + step[0] + " did not finish");
            return false;
        });
    }

    private static boolean shieldUp(ServerPlayer wanda) {
        return com.kolodkingal.enhanced.ability.WandaAbilities.hasShieldUp(wanda);
    }

    @EnhancedTest(template = ARENA, batch = "ultron_wanda")
    public static void wandaAndTheMachines(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(BossConfig.REGRIP_TICKS, 0); // (each step takes hold of him again at once)
        ServerPlayer wanda = f.player(MIDDLE.x, FLOOR, MIDDLE.z, Power.SCARLET_WITCH);
        UltronFight fight = onTheGround(f, wanda);
        calm(fight);
        UltronEntity boss = boss(fight);
        put(f, wanda, 10.5, FLOOR, 15.5);
        put(f, boss, 10.5, FLOOR + 1.0, 8.5);
        lookAt(wanda, boss);

        // A Prime in good health: her hand holds him as a boss (3.1), and tears nothing.
        press(wanda);
        check(Telekinesis.isHeld(boss) && BossGrip.GRABBED.equals(lastActionBarKey(wanda)), "she holds a body in good health as a boss");
        check(fight.uploads() == 0 && boss.getHealth() == boss.getMaxHealth(), "nothing was torn");
        Telekinesis.release(wanda, false);
        // Before the island is up he cannot fall, whatever his health: his floor of a tenth is below the threshold.
        put(f, boss, 10.5, FLOOR + 1.0, 8.5);
        lookAt(wanda, boss);
        boss.setHealth(1.0F);
        check(boss.getHealth() >= boss.getMaxHealth() * 0.1F - 0.01F, "Prime keeps a tenth until the island is up: " + boss.getHealth());
        press(wanda);
        check(Telekinesis.isHeld(boss) && !fight.isTransferring() && !fight.isDying(),
                "and below the threshold, on the ground, her hand holds him but cannot tear him apart");
        press(wanda);
        check(!Telekinesis.isHolding(wanda) && !fight.isTransferring() && !fight.isDying(), "nor when she throws him");
        Telekinesis.clear();

        // She can lift a Sentry, and when she throws it, its core is crushed instead.
        fight.forceState(UltronFight.Phase.ISLAND);
        calm(fight);
        boss.setHealth(boss.getMaxHealth());
        UltronSentryEntity sentry = sentryAt(f, fight, 14.5, 12.5); // (not in line with him: her hand goes to the first body it finds)
        sentry.setHeld(true);
        float sentryHealth = sentry.getHealth();
        lookAt(wanda, sentry);
        press(wanda);
        check(Telekinesis.isHolding(wanda) && Telekinesis.isHeld(sentry), "she holds a Sentry");
        press(wanda);
        check(!Telekinesis.isHolding(wanda) && sentry.isDeadOrDying(), "and what she throws, her hand crushes: it is dead");
        assertNear(fight.participant(wanda.getUUID()).damage(), sentryHealth, 0.01, "its health went to her ledger");
        Telekinesis.clear();

        // On the island, at a sixth of his health: she holds him. Set to 14.9 percent while she holds him, her V tears out the
        // core instead of throwing him.
        put(f, boss, 10.5, FLOOR + 1.0, 8.5);
        lookAt(wanda, boss);
        boss.setHealth(boss.getMaxHealth() * 0.16F);
        press(wanda);
        check(Telekinesis.holds(wanda, boss) && !fight.isTransferring(), "at 16 percent she holds him");
        boss.setHealth(boss.getMaxHealth() * 0.149F);
        float torn = boss.getHealth();
        press(wanda);
        check(fight.isTransferring() && fight.uploads() == 1, "below 15 percent, held, her V tears the core out and he is uploaded");
        check(!Telekinesis.isHolding(wanda), "he was not thrown");
        check(boss.isShielded() && boss.state() == UltronState.TRANSFER, "which makes him untouchable for its time");
        check(fight.participant(wanda.getUUID()).damage() >= sentryHealth + torn - 0.1F, "and what was left of the body is hers");
        helper.succeed();
    }

    private static void press(ServerPlayer wanda) {
        AbilityHandler.onKey(wanda, 3, true);
        AbilityHandler.onKey(wanda, 3, false);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Magneto

    @EnhancedTest(template = ARENA, batch = "ultron_magneto", timeoutTicks = 400)
    public static void magnetoAndTheSentries(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(UltronBossConfig.SENTRY_STUN_TICKS, 30);
        ServerLevel level = f.level();
        ServerPlayer magneto = f.player(MIDDLE.x, FLOOR, MIDDLE.z, Power.MAGNETO);
        tank(magneto);
        UltronFight fight = onTheGround(f, magneto);
        calm(fight);
        put(f, magneto, 5.5, FLOOR, 10.5);
        UltronEntity boss = boss(fight);
        put(f, boss, 10.5, FLOOR + 3.0, 3.5);
        UltronSentryEntity sentry = sentryAt(f, fight, 10.5, 10.5);
        sentry.setHeld(false);

        // Iron: a Sentry is metal and he lifts it; so is Prime's vibranium (3.1), and on the ground, unshielded, Magneto, Wanda
        // and Billy can each hold him as a boss; shielded, nobody can.
        check(Telekinesis.isMagnetic(sentry) && Telekinesis.isMagnetic(boss), "a Sentry is magnetic, and so is Prime");
        check(Telekinesis.canLift(magneto, sentry, Telekinesis.Mode.MAGNETO), "Magneto can lift a Sentry");
        check(Telekinesis.canLift(magneto, boss, Telekinesis.Mode.MAGNETO) && Telekinesis.canLift(magneto, boss, Telekinesis.Mode.WANDA)
                && Telekinesis.canLift(magneto, boss, Telekinesis.Mode.BILLY), "on the ground, unshielded, Prime can be held");
        boss.setShielded(true);
        check(!Telekinesis.canLift(magneto, boss, Telekinesis.Mode.MAGNETO) && !Telekinesis.canLift(magneto, boss, Telekinesis.Mode.WANDA)
                && !Telekinesis.canLift(magneto, boss, Telekinesis.Mode.BILLY), "shielded, nobody can hold him");
        boss.setShielded(false);
        check(Telekinesis.canLift(magneto, sentry, Telekinesis.Mode.WANDA) && Telekinesis.canLift(magneto, sentry, Telekinesis.Mode.BILLY),
                "and Wanda or Billy can lift a Sentry too");

        // The plasma is not metal: his field does not repel it.
        UltronBoltEntity bolt = UltronBoltEntity.create(level, boss, magneto.position().add(3, 1, 0), new Vec3(0.5, 0, 0), 4.0F, UltronBoltEntity.Palette.PRIME);
        check(!MagnetoAbilities.isMetalProjectile(bolt), "a plasma bolt is not a metal projectile");

        // A Sentry in his grip does not act; let go, it hunts again.
        lookAt(magneto, sentry);
        Telekinesis.press(magneto, Telekinesis.Mode.MAGNETO);
        check(Telekinesis.isHolding(magneto) && Telekinesis.isHeld(sentry), "Magneto holds the Sentry");
        int[] shots = {0};
        int[] released = {-1};
        f.everyTick(300, tick -> {
            PowerTicker.tick(magneto);
            int bolts = level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds(), b -> b.getOwner() == sentry).size();
            if (released[0] < 0) {
                check(bolts == 0, "a held Sentry does not shoot");
                check(sentry.isAlive() && Telekinesis.isHeld(sentry), "and is held");
                if (tick == 80) {
                    Telekinesis.release(magneto, true);
                    check(!Telekinesis.isHeld(sentry), "let go");
                    released[0] = tick;
                }
                return false;
            }
            shots[0] += bolts;
            if (shots[0] > 0) {
                return true;
            }
            check(tick - released[0] < 150, "a Sentry that was let go never shot again");
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Mind Stone and the lightning

    @EnhancedTest(template = ARENA, batch = "ultron_bonus")
    public static void visionAndLightningBonuses(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer vision = f.player(MIDDLE.x, FLOOR, MIDDLE.z, Power.VISION);
        UltronFight fight = onTheGround(f, vision);
        calm(fight);
        UltronEntity boss = boss(fight);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        boss.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0);
        UltronSentryEntity sentry = sentryAt(f, fight, 14.5, 10.5);
        sentry.setHeld(true);
        sentry.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, FLOOR, 6.5));
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        pig.setHealth(100.0F);
        ServerPlayer plain = f.player(4.5, FLOOR, 14.5);
        ServerPlayer hero = f.player(16.5, FLOOR, 14.5, Power.ULTRON);
        sentry.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        sentry.setHealth(1000.0F);
        DamageSource beam = ModDamageTypes.source(level, ModDamageTypes.MIND_STONE_BEAM);
        DamageSource lightning = level.damageSources().lightningBolt();

        // The Mind Stone beam: 1.5 times to the bodies and the Sentries, nothing to anyone else.
        assertNear(lose(boss, beam, 10.0F), 15.0, 0.01, "the beam does 15 to a body");
        assertNear(lose(sentry, beam, 10.0F), 15.0, 0.01, "and to a Sentry");
        assertNear(lose(pig, beam, 10.0F), 10.0, 0.01, "but 10 to a pig");
        assertNear(lose(hero, beam, 4.0F), afterArmour(hero, 4.0F, beam), 0.01, "and 4 (before his armour) to the Ultron hero: the beam is for machines");

        // Lightning: 1.25 to the bodies, the Sentries and the human Ultron.
        assertNear(lose(boss, lightning, 20.0F), 25.0, 0.01, "lightning does 25 where it would do 20");
        assertNear(lose(sentry, lightning, 8.0F), 10.0, 0.01, "10 to a Sentry");
        check(sentry.isStunned() && !boss.isStaggered(), "and the bolt stuns the Sentry (and not the body)");
        assertNear(lose(pig, lightning, 8.0F), 8.0, 0.01, "8 to a pig");
        assertNear(lose(plain, lightning, 4.0F), 4.0, 0.01, "4 to a player");
        assertNear(lose(hero, lightning, 4.0F), afterArmour(hero, 5.0F, lightning), 0.01, "5 (before his armour) to the Ultron hero");
        f.config(UltronConfig.LIGHTNING_FACTOR, 2.0);
        f.config(UltronBossConfig.VISION_BEAM_BONUS, 3.0);
        assertNear(lose(sentry, lightning, 8.0F), 16.0, 0.01, "the factors are the config's: lightningFactor 2");
        assertNear(lose(sentry, beam, 4.0F), 12.0, 0.01, "visionBeamBonus 3");
        f.config(UltronConfig.LIGHTNING_FACTOR, 1.25);
        f.config(UltronBossConfig.VISION_BEAM_BONUS, 1.5);

        // A Vision near him cuts his uploads: within visionCutRange of the body.
        f.config(UltronConfig.VISION_CUT_RANGE, 6);
        put(f, boss, 10.5, FLOOR + 3.0, 10.5);
        put(f, vision, 10.5, FLOOR, 14.5);
        check(fight.visionNear(boss.position()), "a Vision 4 blocks away cuts him off");
        put(f, vision, 10.5, FLOOR, 18.5);
        check(!fight.visionNear(boss.position()), "one 8 blocks away, with a range of 6, does not");
        put(f, vision, 10.5, FLOOR, 14.5);
        f.config(UltronConfig.VISION_CUT_RANGE, 0);
        check(!fight.visionNear(boss.position()), "a range of 0 turns the cut off");
        f.config(UltronConfig.VISION_CUT_RANGE, 6);
        Powers.setPower(vision, Power.THOR);
        check(!fight.visionNear(boss.position()), "and only a Vision does it");
        Powers.setPower(vision, Power.VISION);
        check(fight.visionNear(boss.position()), "a Vision again");
        vision.setGameMode(GameType.SPECTATOR);
        check(!fight.visionNear(boss.position()), "a Spectator is not there");
        helper.succeed();
    }

    /** What is left of a blow of {@code amount} after the creature's armour (the vibranium body has some). */
    private static float afterArmour(LivingEntity entity, float amount, DamageSource source) {
        return CombatRules.getDamageAfterAbsorb(entity, amount, source, entity.getArmorValue(), (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    /** Hurts the creature with the source (the hit cooldown cleared) and returns the health it lost. */
    private static double lose(LivingEntity entity, DamageSource source, float amount) {
        entity.invulnerableTime = 0;
        float before = entity.getHealth();
        entity.hurt(source, amount);
        return before - entity.getHealth();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Hex

    @EnhancedTest(template = ARENA, batch = "ultron_hex")
    public static void hexAndTheMachines(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer member = f.player(MIDDLE.x, FLOOR, MIDDLE.z, Power.THOR);
        ServerPlayer bystander = f.player(12.5, FLOOR, 12.5, Power.BILLY);
        UltronFight fight = onTheGround(f, member);
        calm(fight);
        UltronEntity boss = boss(fight);
        UltronSentryEntity sentry = sentryAt(f, fight, 14.5, 10.5);
        sentry.setHeld(true);
        Hex hex = new Hex(UUID.randomUUID(), UUID.randomUUID(), "Wanda", f.helper().absoluteVec(new Vec3(10.5, FLOOR, 10.5)), 5, Set.of(), false, 0L, false);

        check(HexManager.judge(hex, boss, false) == HexManager.Verdict.BLOCK && HexManager.judge(hex, boss, true) == HexManager.Verdict.BLOCK,
                "the wall turns the body back, it does not gamble with him");
        check(HexManager.judge(hex, sentry, false) == HexManager.Verdict.GAMBLE, "a Sentry that crosses is at the wall's mercy, like any creature");
        check(!fight.hexExempt() && !exempt(boss), "in an ordinary fight nobody is exempt");

        // While the island rises everyone it carries passes, in both directions.
        fight.setHexExempt(true);
        // 3.3: only those Wanda lets in come in: the lift no longer carries anyone through (it is refused where a Hex cuts its way)
        check(HexManager.judge(hex, boss, false) == HexManager.Verdict.BLOCK && HexManager.judge(hex, sentry, false) == HexManager.Verdict.GAMBLE
                && HexManager.judge(hex, member, false) != HexManager.Verdict.PASS, "the lift carries nobody through");
        check(HexManager.judge(hex, bystander, false) == HexManager.Verdict.BLOCK, "but not a bystander");
        fight.setHexExempt(false);
        check(HexManager.judge(hex, boss, false) == HexManager.Verdict.BLOCK, "and then the wall is the wall again");
        helper.succeed();
    }

    private static boolean exempt(Entity entity) {
        return com.kolodkingal.enhanced.boss.BossFights.isHexExempt(entity);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Nothing breaks

    /**
     * Whatever he does, the world stays as it was: every attack, the Sentries' shots and the falling Sokovia are played
     * with the game rule that lets mobs grief set, and not one block of the arena changes, and nothing is dropped, set
     * alight or dropped as an entity.
     */
    @EnhancedTest(template = ARENA, batch = "ultron_blocks", timeoutTicks = 1400)
    public static void nothingBreaksBlocks(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, level.getServer());
        f.onClose(() -> level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(griefing, level.getServer()));
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheIsland(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        AABB bounds = helper.getBounds();
        Map<BlockPos, BlockState> before = snapshot(level, bounds);
        check(before.size() > 1000, "the arena was looked at: " + before.size() + " blocks");

        UltronSentryEntity sentry = sentryAt(f, fight, 16.5, 4.5);
        check(sentry.isAlive(), "a Sentry shoots meanwhile");
        UltronBrain brain = boss.brain();
        List<UltronAttack> attacks = List.of(UltronAttack.VOLLEY, UltronAttack.BEAMS, UltronAttack.DIVE, UltronAttack.PULL, UltronAttack.PUNCH, UltronAttack.CALL);
        fight.setNextDebrisAt(level.getGameTime());
        int[] index = {0};
        int[] since = {0};
        int[] endAt = {-1};
        f.everyTick(1300, tick -> {
            if (index[0] == attacks.size()) {
                // Let the pieces of Sokovia land and the last Sentries emerge and shoot, then look at the world.
                if (tick < endAt[0]) {
                    return false;
                }
                Map<BlockPos, BlockState> after = snapshot(level, bounds);
                check(after.size() == before.size(), "the same blocks were looked at");
                for (Map.Entry<BlockPos, BlockState> entry : before.entrySet()) {
                    check(entry.getValue().equals(after.get(entry.getKey())),
                            "the block at " + entry.getKey().subtract(helper.absolutePos(BlockPos.ZERO)) + " was " + entry.getValue() + " and is " + after.get(entry.getKey()));
                }
                check(level.getEntitiesOfClass(ItemEntity.class, bounds).isEmpty(), "no item was dropped");
                check(level.getEntitiesOfClass(FallingBlockEntity.class, bounds).isEmpty() && level.getEntitiesOfClass(PrimedTnt.class, bounds).isEmpty(),
                        "no block fell and nothing was primed");
                return true;
            }
            if (since[0] == 0) {
                player.setHealth(1000.0F);
                player.invulnerableTime = 0;
                UltronAttack attack = attacks.get(index[0]);
                put(f, player, 10.5, FLOOR, attack == UltronAttack.PUNCH ? 11.0 : 12.5);
                put(f, boss, 10.5, attack == UltronAttack.PUNCH ? FLOOR + 1.0 : FLOOR + 3.0, attack == UltronAttack.PULL ? 10.5 : attack == UltronAttack.PUNCH ? 8.5 : 3.5);
                check(brain.force(boss, fight, attack), attack + " starts");
            }
            since[0]++;
            if (since[0] >= 4 && brain.current() == null) {
                index[0]++;
                since[0] = 0;
                if (index[0] == attacks.size()) {
                    endAt[0] = tick + 90;
                }
            }
            check(tick < 1250, "the attacks did not finish");
            return false;
        });
    }

    private static Map<BlockPos, BlockState> snapshot(ServerLevel level, AABB bounds) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ), BlockPos.containing(bounds.maxX - 1.0E-6, bounds.maxY - 1.0E-6, bounds.maxZ - 1.0E-6))) {
            states.put(pos.immutable(), level.getBlockState(pos));
        }
        return states;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Sentry

    @EnhancedTest(template = ARENA, batch = "ultron_sentry", timeoutTicks = 600)
    public static void sentryEyesAndStun(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        f.config(UltronBossConfig.SENTRY_STUN_TICKS, 30);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        put(f, player, 10.5, FLOOR, 16.5);
        UltronSentryEntity sentry = UltronSentryEntity.army(level, f.helper().absoluteVec(new Vec3(10.5, FLOOR + 1.5, 8.5)), true);
        fight.spawn(sentry);

        check(sentry.isAssembling() && sentry.act() == UltronSentryEntity.Act.ASSEMBLING && sentry.eyes() == UltronSentryEntity.Eyes.BLUE,
                "a new Sentry is assembling, its eyes dark blue");
        float health = sentry.getHealth();
        check(!sentry.hurt(level.damageSources().generic(), 5.0F) && sentry.getHealth() == health, "and nothing hurts it while it does");

        int[] tellStart = {-1};
        int[] firstBolt = {-1};
        float[] healthAtBolt = {0.0F};
        int[] stunnedAt = {-1};
        int[] shotsWhileStunned = {0};
        int[] wokeAt = {-1};
        f.everyTick(560, tick -> {
            List<UltronBoltEntity> bolts = level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds(), b -> b.getOwner() == sentry);
            if (tick <= 18) {
                check(sentry.isAssembling() && bolts.isEmpty(), "it neither acts nor shoots while it assembles (tick " + tick + ")");
                check(!sentry.hurt(level.damageSources().generic(), 5.0F), "and stays untouchable (tick " + tick + ")");
            }
            if (tick == 24) {
                check(!sentry.isAssembling() && sentry.act() != UltronSentryEntity.Act.ASSEMBLING, "twenty ticks later it is whole");
                check(sentry.eyes() == UltronSentryEntity.Eyes.RED, "and hunting, its eyes red: " + sentry.eyes());
            }
            if (stunnedAt[0] < 0) {
                if (tellStart[0] < 0 && sentry.act() == UltronSentryEntity.Act.SHOT) {
                    tellStart[0] = tick;
                    check(sentry.eyes() == UltronSentryEntity.Eyes.WHITE, "the eyes go white for the shot's tell");
                    check(bolts.isEmpty(), "and no bolt has left yet");
                }
                if (tellStart[0] > 0 && tick == tellStart[0] + 3) {
                    // Out of the line of the shot, after its aim was fixed: six blocks to the side of it, wherever the Sentry circles to.
                    Vec3 aimed = player.position();
                    Vec3 along = aimed.subtract(sentry.position());
                    Vec3 side = new Vec3(-along.z, 0.0, along.x).normalize().scale(6.0);
                    Vec3 one = f.helper().relativeVec(aimed.add(side));
                    Vec3 other = f.helper().relativeVec(aimed.subtract(side));
                    Vec3 chosen = one.x > 1.5 && one.x < 19.5 && one.z > 1.5 && one.z < 19.5 ? one : other;
                    put(f, player, chosen.x, FLOOR, chosen.z);
                }
                if (tellStart[0] > 0 && firstBolt[0] < 0 && !bolts.isEmpty()) {
                    firstBolt[0] = tick;
                    // (It may have punched on its way here, wherever it circled to: only what the bolt does counts.)
                    healthAtBolt[0] = player.getHealth();
                    check(tick - tellStart[0] >= UltronSentryEntity.SHOT_TELL - 1, "the bolt came " + (tick - tellStart[0]) + " ticks after the tell began");
                    check(tick - tellStart[0] <= UltronSentryEntity.SHOT_TELL + 3, "and not much later than it ends");
                    // (It stands still for a while, so that no punch of its own can land while the shot is judged.)
                    sentry.freeze(12);
                }
                if (firstBolt[0] > 0 && tick == firstBolt[0] + 14) {
                    check(player.getHealth() == healthAtBolt[0], "the shot went where it was aimed when the tell began, not after the target: "
                            + healthAtBolt[0] + " then " + player.getHealth());
                    // Lightning stuns it for 30 ticks.
                    sentry.onLightning(null, 5.0F);
                    stunnedAt[0] = tick;
                    check(sentry.isStunned(), "lightning stuns the Sentry");
                    put(f, player, 10.5, FLOOR, 16.5);
                }
                check(tick < 200, "the Sentry never shot");
                return false;
            }
            int sinceStun = tick - stunnedAt[0];
            if (sinceStun > 1 && sinceStun < 27) {
                check(sentry.isStunned() && sentry.act() == UltronSentryEntity.Act.STUNNED && sentry.eyes() == UltronSentryEntity.Eyes.BLUE,
                        "stunned: dark eyes, the flicker (tick " + sinceStun + ")");
                shotsWhileStunned[0] += bolts.size();
                return false;
            }
            if (sinceStun >= 30 && wokeAt[0] < 0) {
                check(shotsWhileStunned[0] == 0, "it shot nothing while it was stunned");
                check(!sentry.isStunned(), "the stun is over after 30 ticks");
                wokeAt[0] = tick;
            }
            if (wokeAt[0] > 0) {
                if (sentry.eyes() == UltronSentryEntity.Eyes.RED || sentry.eyes() == UltronSentryEntity.Eyes.WHITE) {
                    return tick > wokeAt[0] + 3;
                }
                check(tick - wokeAt[0] < 30, "its eyes never lit again");
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "ultron_waves", timeoutTicks = 900)
    public static void sentryWavesAndBudget(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        check(fight.budget() == 20 && fight.budgetLeft() == 20 && fight.maxAlive() == 8, "one fighter: a budget of 20, 8 alive at most");
        check(fight.sentriesLeft() == 20 && fight.sentries().isEmpty() && fight.pendingSentries() == 0, "the second bar counts those alive and those to come");

        // A wave is announced and emerges thirty ticks later, four at a time.
        check(fight.callWave(true) && fight.pendingSentries() == 4, "a wave of four is announced");
        check(fight.sentries().isEmpty() && fight.budgetSpent() == 0, "nothing has emerged yet");
        check(fight.budgetLeft() == 16 && fight.sentriesLeft() == 20,
                "the four on their way are no longer in the budget, so the second bar does not count them twice: " + fight.sentriesLeft());
        check(!fight.mayCall() && !fight.callWave(true), "and a second is refused while it is on its way");
        int[] stage = {0};
        long[] announcedAt = {level.getGameTime()};
        int[] total = {0};
        f.everyTick(850, tick -> {
            for (UltronSentryEntity sentry : fight.sentries()) {
                sentry.setHeld(true);
            }
            int alive = fight.sentries().size();
            check(alive <= fight.maxAlive(), "never more than " + fight.maxAlive() + " alive: " + alive);
            check(fight.budgetSpent() <= fight.budget(), "never more than the budget is spent");
            check(fight.sentriesLeft() == alive + fight.budgetLeft() + fight.pendingSentries(), "the second bar adds up");
            long age = level.getGameTime() - announcedAt[0];
            switch (stage[0]) {
                case 0 -> {
                    if (age < UltronFight.WAVE_TELL - 2) {
                        check(alive == 0, "nobody emerges before the tell is over");
                    }
                    if (fight.pendingSentries() == 0) {
                        check(alive == 4 && fight.budgetSpent() == 4 && age >= UltronFight.WAVE_TELL - 1, "four emerged, at tick " + age);
                        for (UltronSentryEntity sentry : fight.sentries()) {
                            Vec3 at = sentry.position();
                            Vec3 centre = fight.island().center();
                            double ring = Math.hypot(at.x - centre.x, at.z - centre.z);
                            check(sentry.isAssembling() && Math.abs(ring - 6.0) < 0.6 && at.distanceTo(player.position()) >= 5.0, "each emerged assembling, on the ring, five blocks from the player");
                            check(Math.abs(at.y - (centre.y + 0.3)) < 0.8, "on the ground");
                        }
                        check(fight.callWave(true) && fight.pendingSentries() == 4, "a second wave of four");
                        announcedAt[0] = level.getGameTime();
                        stage[0] = 1;
                    }
                }
                case 1 -> {
                    if (fight.pendingSentries() == 0) {
                        check(alive == 8 && fight.budgetSpent() == 8, "eight are alive");
                        check(!fight.mayCall() && !fight.callWave(true) && fight.pendingSentries() == 0, "and the cap refuses a third");
                        // Three fall: room for three.
                        List<UltronSentryEntity> some = new ArrayList<>(fight.sentries());
                        for (int i = 0; i < 3; i++) {
                            some.get(i).kill();
                        }
                        stage[0] = 2;
                    }
                }
                case 2 -> {
                    if (fight.sentries().size() == 5) {
                        check(fight.mayCall() && fight.callWave(true) && fight.pendingSentries() == 3, "with five alive a wave of three fills the cap (not four)");
                        announcedAt[0] = level.getGameTime();
                        stage[0] = 3;
                    }
                }
                case 3 -> {
                    if (fight.pendingSentries() == 0) {
                        check(alive == 8 && fight.budgetSpent() == 11 && fight.budgetLeft() == 9, "eight again; 9 of the budget left");
                        for (UltronSentryEntity sentry : new ArrayList<>(fight.sentries())) {
                            sentry.kill();
                        }
                        stage[0] = 4;
                    }
                }
                case 4 -> {
                    // Waves of four, four and one use up the budget; each time the field is cleared.
                    if (fight.sentries().isEmpty() && fight.pendingSentries() == 0 && fight.budgetLeft() > 0) {
                        int expect = Math.min(4, fight.budgetLeft());
                        check(fight.callWave(true) && fight.pendingSentries() == expect, "a wave of " + expect + " (the budget decides the last)");
                        total[0] += expect;
                        announcedAt[0] = level.getGameTime();
                    } else if (fight.pendingSentries() == 0 && !fight.sentries().isEmpty() && fight.sentries().stream().allMatch(UltronSentryEntity::isAssembling)) {
                        for (UltronSentryEntity sentry : new ArrayList<>(fight.sentries())) {
                            sentry.kill();
                        }
                    }
                    if (fight.budgetLeft() == 0 && fight.pendingSentries() == 0) {
                        check(total[0] == 9 && fight.budgetSpent() == 20, "the whole budget of 20 was used and no more: " + fight.budgetSpent());
                        check(!fight.mayCall() && !fight.callWave(true), "nothing more can be called");
                        stage[0] = 5;
                    }
                }
                default -> {
                    for (UltronSentryEntity sentry : new ArrayList<>(fight.sentries())) {
                        sentry.kill();
                    }
                    if (fight.sentries().isEmpty()) {
                        check(fight.sentriesLeft() == 0, "with the last Sentry down, the second bar reads zero");
                        return true;
                    }
                }
            }
            check(tick < 840, "stage " + stage[0] + " did not finish");
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The shockwaves

    /** The Pull drags everyone it can see, the shockwaves of the Pull and the Dive hurt those on the ground and let the airborne pass. */
    @EnhancedTest(template = ARENA, batch = "ultron_shock", timeoutTicks = 500)
    public static void shockwavesSpareTheAirborne(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerPlayer low = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        ServerPlayer high = f.player(16.5, FLOOR, 16.5);
        ServerPlayer far = f.player(4.5, FLOOR, 16.5);
        tank(low);
        tank(high);
        tank(far);
        UltronFight fight = onTheGround(f, low);
        calm(fight);
        UltronEntity boss = boss(fight);
        UltronBrain brain = boss.brain();
        int[] stage = {0};
        int[] since = {0};
        f.everyTick(480, tick -> {
            if (since[0] == 0) {
                for (ServerPlayer p : List.of(low, high, far)) {
                    p.setHealth(1000.0F);
                    p.invulnerableTime = 0;
                    p.setDeltaMovement(Vec3.ZERO);
                    p.removeAllEffects();
                }
                if (stage[0] == 0) {
                    put(f, boss, 10.5, FLOOR + 3.0, 10.5);
                    put(f, low, 10.5, FLOOR, 14.5);
                    put(f, high, 14.5, FLOOR + 2.5, 10.5);
                    put(f, far, 10.5, FLOOR, 19.5);
                    check(brain.force(boss, fight, UltronAttack.PULL), "the Pull starts");
                } else {
                    put(f, boss, 10.5, FLOOR + 3.0, 3.5);
                    put(f, low, 10.5, FLOOR, 12.5);
                    put(f, high, 13.5, FLOOR + 2.5, 12.5);
                    put(f, far, 17.5, FLOOR, 12.5);
                    check(brain.force(boss, fight, UltronAttack.DIVE), "the Dive starts");
                }
            }
            since[0]++;
            int s = since[0];
            if (stage[0] == 0) {
                if (s == 20) {
                    check(low.getDeltaMovement().lengthSqr() < 1.0E-6 && far.getDeltaMovement().lengthSqr() < 1.0E-6, "nobody is pulled while he tells");
                }
                if (s == 30) {
                    for (ServerPlayer p : List.of(low, high, far)) {
                        Vec3 toBoss = boss.position().add(0, 1.0, 0).subtract(p.position());
                        check(p.getDeltaMovement().lengthSqr() > 0.04 && p.getDeltaMovement().dot(toBoss) > 0.0, "everyone he can see is dragged toward him");
                    }
                }
                if (s > 5 && brain.current() == null) {
                    check(low.getHealth() < 999.0F, "the punch of the Pull hurts the one on the ground beside him: " + low.getHealth());
                    check(high.getHealth() > 999.99F, "and the one two blocks up passes over it");
                    check(far.getHealth() > 999.99F, "and the one beyond six blocks is not reached");
                    stage[0] = 1;
                    since[0] = 0;
                }
            } else if (s > 5 && brain.current() == null) {
                check(low.getHealth() < 999.0F, "the Dive's landing hurts the one in the ring: " + low.getHealth());
                check(low.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && low.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1, "and slows him (Slowness II)");
                check(low.getDeltaMovement().lengthSqr() > 0.01, "and throws him back");
                check(high.getHealth() > 999.99F && far.getHealth() > 999.99F, "the one in the air and the one outside the ring are spared");
                return true;
            }
            check(s < 200, "stage " + stage[0] + " never ended");
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The bolts

    @EnhancedTest(template = ARENA, batch = "ultron_bolts", timeoutTicks = 200)
    public static void boltsHitOnlyTheirEnemies(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer survivor = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(survivor);
        ServerPlayer creative = f.player(10.5, FLOOR, 7.5, GameType.CREATIVE);
        ServerPlayer hero = f.player(6.5, FLOOR, 2.5);
        tank(hero);
        UltronFight fight = onTheGround(f, survivor);
        calm(fight);
        UltronEntity boss = boss(fight);
        put(f, boss, 10.5, FLOOR + 1.0, 3.5);
        put(f, survivor, 10.5, FLOOR, 14.5);
        UltronSentryEntity sentry = sentryAt(f, fight, 10.5, 10.5);
        sentry.setHeld(true);
        float sentryHealth = sentry.getHealth();
        Vec3 from = f.helper().absoluteVec(new Vec3(10.5, FLOOR + 1.0, 4.5));
        float creativeHealth = creative.getHealth();

        // The boss's bolt flies through the Creative player and the Sentry and hurts the one behind them.
        fight.spawn(UltronBoltEntity.create(level, boss, from, new Vec3(0.0, 0.0, 1.7), 6.0F, UltronBoltEntity.Palette.PRIME));
        // A hero's bolts: through his drone, not through a player unless PvP allows it, and every one lands on a creature.
        UltronSentryEntity drone = UltronSentryEntity.army(level, f.helper().absoluteVec(new Vec3(6.5, FLOOR + 1.0, 8.5)), false);
        drone.setOwner(hero.getUUID());
        level.addFreshEntity(drone);
        drone.setHeld(true);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(6.5, FLOOR, 11.5));
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        pig.setHealth(100.0F);
        ServerPlayer other = f.player(6.5, FLOOR, 16.5);
        tank(other);
        Vec3 heroFrom = f.helper().absoluteVec(new Vec3(6.5, FLOOR + 1.0, 4.5));
        for (int i = 0; i < 2; i++) {
            UltronBoltEntity bolt = UltronBoltEntity.create(level, hero, heroFrom, new Vec3(0.0, 0.0, 1.7), 5.0F, UltronBoltEntity.Palette.HUMAN);
            level.addFreshEntity(bolt);
        }
        helper.runAfterDelay(20, () -> {
            double lost = 1000.0 - survivor.getHealth();
            check(lost > 0.0 && lost <= 9.1, "the boss's bolt hurt the one behind: " + lost);
            check(creative.getHealth() == creativeHealth, "the Creative player was passed through");
            check(sentry.getHealth() == sentryHealth, "and so was his own Sentry");
            check(drone.getHealth() == drone.getMaxHealth(), "a hero's bolt passes his drone");
            check(other.getHealth() > 999.99F, "and, with PvP off, another player");
            check(pig.getHealth() < 100.0F - 5.0F * 1.9F && pig.getHealth() > 100.0F - 5.0F * 2.1F, "while two bolts at once both land on a creature: " + (100.0F - pig.getHealth()));
            check(hero.getHealth() > 999.99F, "and never on the hero");
            // PvP on: the next bolt reaches the other player.
            f.pvp(true);
            pig.discard();
            UltronBoltEntity bolt = UltronBoltEntity.create(level, hero, heroFrom, new Vec3(0.0, 0.0, 1.7), 5.0F, UltronBoltEntity.Palette.HUMAN);
            level.addFreshEntity(bolt);
            helper.runAfterDelay(20, () -> {
                check(other.getHealth() < 999.0F, "with PvP on, the bolt reaches the other player: " + other.getHealth());
                check(drone.getHealth() == drone.getMaxHealth(), "still not his drone");
                helper.succeed();
            });
        });
    }

    /**
     * What the arena has in it that is nobody's enemy is not shot: an armour stand, a boat, a minecart and an item frame
     * (a projectile that struck one would knock the item out of the frame or break the stand), whether it is the boss's
     * bolt or a Sentry's, and a Sentry's fist.
     */
    @EnhancedTest(template = ARENA, batch = "ultron_bolts_spare", timeoutTicks = 200)
    public static void theBossBoltsAndFistsSpareWhatHangsOrFloatsOrStands(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        UltronEntity boss = boss(fight);
        put(f, boss, 10.5, FLOOR + 1.0, 1.5);
        put(f, player, 19.5, FLOOR, 19.5);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(3.5, FLOOR, 10.5));
        Boat boat = helper.spawn(EntityType.BOAT, new Vec3(6.5, FLOOR, 10.5));
        Minecart cart = helper.spawn(EntityType.MINECART, new Vec3(9.5, FLOOR, 10.5));
        BlockPos wall = helper.absolutePos(new BlockPos(13, (int) FLOOR, 11));
        level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        f.onClose(() -> level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3));
        ItemFrame frame = new ItemFrame(level, wall.north(), Direction.NORTH);
        frame.setItem(new ItemStack(Items.DIAMOND));
        level.addFreshEntity(frame);
        check(frame.isAlive(), "the item frame holds on to its wall");
        UltronSentryEntity sentry = UltronSentryEntity.army(level, helper.absoluteVec(new Vec3(3.5, FLOOR + 1.0, 8.5)), false);
        fight.spawn(sentry);
        sentry.setHeld(true);
        float standHealth = stand.getHealth();
        for (double x : new double[]{3.5, 6.5, 9.5, 13.5}) {
            Vec3 from = f.helper().absoluteVec(new Vec3(x, FLOOR + 0.8, 4.5));
            fight.spawn(UltronBoltEntity.create(level, boss, from, new Vec3(0.0, 0.0, 1.7), 6.0F, UltronBoltEntity.Palette.PRIME));
        }
        helper.runAfterDelay(25, () -> {
            check(stand.isAlive() && stand.getHealth() == standHealth, "the armour stand stands");
            check(boat.isAlive() && cart.isAlive(), "the boat and the minecart float and roll on");
            check(frame.isAlive() && frame.getItem().is(Items.DIAMOND), "the item frame still holds its diamond");
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Loki's Scepter and Wanda's Spellbound

    @EnhancedTest(template = ARENA, batch = "ultron_sentry2", timeoutTicks = 520)
    public static void confusedAndSpellboundSentriesDoNotHunt(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(UltronBossConfig.SENTRIES_AT_START, 0);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        tank(player);
        UltronFight fight = onTheGround(f, player);
        calm(fight);
        put(f, player, 10.5, FLOOR, 16.5);
        UltronSentryEntity sentry = UltronSentryEntity.army(level, f.helper().absoluteVec(new Vec3(10.5, FLOOR + 1.5, 8.5)), false);
        fight.spawn(sentry);
        sentry.setEyes(UltronSentryEntity.Eyes.RED);

        int[] stage = {0};
        int[] since = {0};
        f.everyTick(500, tick -> {
            since[0]++;
            List<UltronBoltEntity> bolts = level.getEntitiesOfClass(UltronBoltEntity.class, helper.getBounds(), b -> b.getOwner() == sentry);
            switch (stage[0]) {
                case 0 -> {
                    // Hunting: it gets a target and shoots.
                    if (!bolts.isEmpty()) {
                        sentry.addEffect(new MobEffectInstance(ModEffects.CONFUSED, 70));
                        check(sentry.hasEffect(ModEffects.CONFUSED), "the Scepter's confusion takes hold of a Sentry");
                        stage[0] = 1;
                        since[0] = 0;
                    }
                    check(tick < 150, "the Sentry never shot");
                }
                case 1 -> {
                    if (since[0] > 20 && since[0] < 70) {
                        check(sentry.getTarget() == null && bolts.isEmpty(), "a confused Sentry hunts nobody and shoots nothing (tick " + since[0] + ")");
                        check(sentry.eyes() == UltronSentryEntity.Eyes.BLUE, "its eyes are dark");
                    }
                    if (since[0] >= 75) {
                        stage[0] = 2;
                        since[0] = 0;
                    }
                }
                case 2 -> {
                    if (!bolts.isEmpty()) {
                        sentry.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 70));
                        check(sentry.hasEffect(ModEffects.SPELLBOUND), "Wanda's spell takes hold of a Sentry");
                        stage[0] = 3;
                        since[0] = 0;
                    }
                    check(since[0] < 120, "a Sentry that was confused never shot again");
                }
                default -> {
                    if (since[0] > 20 && since[0] < 70) {
                        check(sentry.getTarget() == null && bolts.isEmpty(), "a spellbound Sentry does nothing (tick " + since[0] + ")");
                    }
                    if (since[0] >= 75) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private UltronMachineTests() {
    }
}
