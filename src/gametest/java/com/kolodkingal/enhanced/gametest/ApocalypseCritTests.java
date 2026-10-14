package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.MindPower;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;
import com.kolodkingal.enhanced.entity.TelekineticBlockEntity;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.breakShield;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.calm;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.duel;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.every;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.noCrits;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.tank;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Apocalypse heals no more, and the mind power of Wanda and of the Phoenix is critical to him (3.1, Eilon's items 8 and
 * 9; spec 8a, tests T63 to T67): on his shield twice the wear (their strains too), on his body twice the harm while
 * every other blow counts 0.4, a critical hit seen, heard and named on the attacker's action bar; Billy, named a
 * breaker by the server, and the Apocalypse hero are not changed; and his health never mends, though his shield still
 * does. Every test has a batch of its own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ApocalypseCritTests {
    /** Ticks after which one attacker's critical hit is seen and heard again. */
    private static final int CRIT_GAP = ApocalypseEntity.CRIT_EFFECT_TICKS + 1;

    /**
     * T63 (8a.2): on shield 1 of a lone fighter (150), with the default factors, one beam hit of 5 by Wanda removes 10;
     * a {@code telekinetic_slam} of 8 caused by her removes 16; the Phoenix's Burst of 30 removes the rest (300 against
     * what is left); on a fresh shield her Mind Control removes 50 and her Psionic Field 120; a borrowed block of 10.4
     * removes {@code 10.4 x 0.5 x 2}.
     */
    @GameTest(template = ARENA, batch = "apocalypse_crit_shield", timeoutTicks = 80)
    public static void mindPowerIsCriticalToHisShield(GameTestHelper helper) {
        shieldWear(helper, true);
    }

    /** T63 with the three factors 1.0: the 3.0 numbers (5, 8, 150, 25, 60, 5.2) and no crit effect. */
    @GameTest(template = ARENA, batch = "apocalypse_crit_off", timeoutTicks = 80)
    public static void mindPowerIsNotCriticalWithTheFactorsAtOne(GameTestHelper helper) {
        shieldWear(helper, false);
    }

    private static void shieldWear(GameTestHelper helper, boolean critical) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        if (!critical) {
            noCrits(d.f());
        }
        double k = critical ? 2.0 : 1.0;
        ServerLevel level = d.level();
        ApocalypseEntity boss = d.boss();
        ServerPlayer wanda = d.hero();
        ServerPlayer phoenix = d.f().player(3.5, FLOOR, 14.5, Power.PHOENIX);
        tank(phoenix);
        List<Object> wandaSaw = new ArrayList<>();
        List<Object> phoenixSaw = new ArrayList<>();
        float max = boss.shield().max();
        assertNear(max, 150.0, 1.0E-3, "shield 1 of a lone fighter");

        // Her beam, and the Phoenix's strains, each on a fresh shield.
        float before = boss.shield().strength();
        check(d.hit(ModDamageTypes.CHAOS_BEAM, 5.0F), "her beam reaches the shield");
        assertNear(before - boss.shield().strength(), 5.0 * k, 1.0E-3, "a beam hit of 5 removes " + 5.0 * k);
        boss.shield().raiseNow(max);
        PhoenixPower.touched(phoenix, boss, PhoenixPower.Kind.MIND_CONTROL, 0.0F);
        assertNear(max - boss.shield().strength(), 25.0 * k, 1.0E-3, "her Mind Control removes " + 25.0 * k);
        boss.shield().raiseNow(max);
        PhoenixPower.touched(phoenix, boss, PhoenixPower.Kind.PSIONIC_FIELD, 0.0F);
        assertNear(max - boss.shield().strength(), 60.0 * k, 1.0E-3, "her Psionic Field removes " + 60.0 * k);
        boss.shield().raiseNow(max);
        check(boss.getHealth() == boss.getMaxHealth(), "none of it touched his health");
        drain(wanda, wandaSaw);
        drain(phoenix, phoenixSaw);

        helper.runAfterDelay(ApocalypseShieldConfig.MIND_HIT_COOLDOWN_TICKS.get() + 1, () -> {
            // A block borrowed from his floor and thrown by her: half a slam, then critical.
            TelekineticBlockEntity block = new TelekineticBlockEntity(level, BlockPos.containing(boss.position().add(-3.0, 1.0, 0.0)),
                    Blocks.SAND.defaultBlockState(), wanda);
            d.fight().adopt(block);
            check(block.isBorrowed(), "the block is borrowed (it carries the fight's mark)");
            boss.invulnerableTime = 0;
            check(boss.hurt(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, wanda, block), 10.4F), "the borrowed block reaches the shield");
            assertNear(max - boss.shield().strength(), 10.4 * 0.5 * k, 1.0E-3, "a borrowed block of 10.4 removes 10.4 x 0.5 x " + k);
            boss.shield().raiseNow(max);
            drain(wanda, wandaSaw);

            helper.runAfterDelay(ApocalypseShieldConfig.MIND_HIT_COOLDOWN_TICKS.get() + 1, () -> {
                // Her slam, then the Phoenix's Burst on what is left.
                check(d.hit(ModDamageTypes.TELEKINETIC_SLAM, 8.0F), "her slam reaches the shield");
                float left = boss.shield().strength();
                assertNear(max - left, 8.0 * k, 1.0E-3, "a slam of 8 caused by her removes " + 8.0 * k);
                boss.invulnerableTime = 0;
                check(boss.hurt(ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, phoenix, phoenix), 30.0F), "the Burst reaches it");
                check(boss.shield().state() == ApocalypseShield.State.BROKEN, "a Burst of 30 (" + 150.0 * k + ") removes the rest and breaks it");
                check(boss.getHealth() == boss.getMaxHealth(), "and his health is whole");
                drain(wanda, wandaSaw);
                drain(phoenix, phoenixSaw);
                for (List<Object> saw : List.of(wandaSaw, phoenixSaw)) {
                    if (critical) {
                        check(!critBars(saw).isEmpty() && critSounds(saw) > 0 && critSparks(saw, null) >= 20,
                                "each of them saw and heard a critical hit: " + critBars(saw) + ", " + critSounds(saw) + " cracks");
                    } else {
                        check(critBars(saw).isEmpty() && critSounds(saw) == 0 && critSparks(saw, null) == 0,
                                "with the factors at 1 nobody sees or hears a critical hit: " + critBars(saw) + ", " + critSounds(saw) + " cracks");
                    }
                }
                helper.succeed();
            });
        });
    }

    /**
     * T64 (8a.2): after Wanda's critical hit on his shield, a watcher 10 blocks away was sent at least 20 {@code CRIT}
     * sparks at the hit, Wanda heard the stand-in (the player's crit, pitch 0.7) once and her action bar says "Critical
     * hit! x2"; a second critical hit within 10 ticks (counted: the shield's own cooldown is 1 tick in this batch)
     * shows nothing more, a third 10 ticks after the first shows it again; Magneto's slam, on the shield or on his
     * body, shows none.
     */
    @GameTest(template = ARENA, batch = "apocalypse_crit_fx", timeoutTicks = 80)
    public static void aCritIsSeenAndHeard(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        d.f().config(ApocalypseShieldConfig.MIND_HIT_COOLDOWN_TICKS, 1);
        ServerLevel level = d.level();
        ApocalypseEntity boss = d.boss();
        ServerPlayer wanda = d.hero();
        ServerPlayer watcher = d.f().player(18.0, FLOOR, 10.5);
        ServerPlayer magneto = d.f().player(10.5, FLOOR, 17.5, Power.MAGNETO);
        tank(watcher);
        tank(magneto);
        DamageSource magnetoSlam = ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, magneto, magneto);
        List<Object> wandaSaw = new ArrayList<>();
        List<Object> watcherSaw = new ArrayList<>();
        List<Object> magnetoSaw = new ArrayList<>();

        // Magneto's slam first, while nothing else is shown: it is turned away, and it is no crit.
        float max = boss.shield().strength();
        boss.invulnerableTime = 0;
        check(!boss.hurt(magnetoSlam, 8.0F) && boss.shield().strength() == max, "Magneto's slam is turned away by the shield");
        drain(magneto, magnetoSaw);
        check(critBars(magnetoSaw).isEmpty() && critSounds(magnetoSaw) == 0 && critSparks(magnetoSaw, null) == 0, "and shows no crit");
        drain(wanda, wandaSaw);
        drain(watcher, watcherSaw);
        wandaSaw.clear();
        watcherSaw.clear();

        // Her beam: one critical hit.
        Vec3 at = hitPoint(boss, wanda);
        check(at.distanceTo(watcher.position()) >= 9.5, "the watcher is 10 blocks from the hit: " + at.distanceTo(watcher.position()));
        check(d.hit(ModDamageTypes.CHAOS_BEAM, 5.0F), "her beam reaches the shield");
        assertNear(max - boss.shield().strength(), 10.0, 1.0E-3, "and wears it by 10");
        drain(wanda, wandaSaw);
        drain(watcher, watcherSaw);
        check(critSparks(watcherSaw, at) >= 20, "the watcher was sent 20 crit sparks at the hit: " + critSparks(watcherSaw, at));
        check(enchantedSparks(watcherSaw, at) >= 10, "and 10 enchanted ones: " + enchantedSparks(watcherSaw, at));
        check(critSounds(watcherSaw) == 1, "and heard the crack: " + critSounds(watcherSaw));
        check(critSounds(wandaSaw) == 1, "Wanda heard the crack once: " + critSounds(wandaSaw));
        check(critBars(wandaSaw).equals(List.of("2")), "her action bar says Critical hit! x2: " + critBars(wandaSaw));
        check(critBars(watcherSaw).isEmpty(), "the watcher's does not");

        helper.runAfterDelay(5, () -> {
            float strength = boss.shield().strength();
            check(d.hit(ModDamageTypes.CHAOS_BEAM, 5.0F), "a second hit 5 ticks later counts");
            assertNear(strength - boss.shield().strength(), 10.0, 1.0E-3, "and wears it by 10 again");
            drain(wanda, wandaSaw);
            drain(watcher, watcherSaw);
            check(critSounds(wandaSaw) == 1 && critBars(wandaSaw).size() == 1 && critSparks(watcherSaw, null) == 20,
                    "but shows nothing more within 10 ticks: " + critSounds(wandaSaw) + " cracks, " + critBars(wandaSaw));
            helper.runAfterDelay(5, () -> {
                check(d.hit(ModDamageTypes.CHAOS_BEAM, 5.0F), "a third hit 10 ticks after the first");
                drain(wanda, wandaSaw);
                check(critSounds(wandaSaw) == 2 && critBars(wandaSaw).size() == 2, "is seen and heard again: " + critSounds(wandaSaw));

                // In a window (broken by her beam a tick later: the shield counts one hit of hers a tick): her beam is
                // critical on his body, Magneto's slam is not.
                helper.runAfterDelay(2, () -> {
                    breakShield(d);
                    drain(wanda, wandaSaw);
                    drain(magneto, magnetoSaw);
                    magnetoSaw.clear();
                    helper.runAfterDelay(CRIT_GAP, () -> {
                        float health = boss.getHealth();
                        boss.invulnerableTime = 0;
                        check(boss.hurt(magnetoSlam, 8.0F) && boss.getHealth() < health, "Magneto's slam hurts him in a window");
                        drain(magneto, magnetoSaw);
                        check(critBars(magnetoSaw).isEmpty() && critSounds(magnetoSaw) == 0 && critSparks(magnetoSaw, null) == 0,
                                "and shows no crit: " + critBars(magnetoSaw));
                        drain(wanda, wandaSaw);
                        wandaSaw.clear();
                        check(d.hit(ModDamageTypes.CHAOS_BEAM, 5.0F), "her beam hurts his body");
                        drain(wanda, wandaSaw);
                        Vec3 chest = boss.position().add(0, boss.getBbHeight() * 0.7, 0);
                        check(critSparks(wandaSaw, chest) >= 20 && critSounds(wandaSaw) == 1 && critBars(wandaSaw).equals(List.of("2")),
                                "a critical hit on his body bursts at his chest: " + critSparks(wandaSaw, chest) + " sparks, " + critBars(wandaSaw));
                        helper.succeed();
                    });
                });
            });
        });
    }

    /**
     * T65 (8a.2): in a window (his armour cut, so the same armour for both), a {@code telekinetic_slam} of 8 caused by
     * Wanda takes exactly 5 times what the same slam caused by Magneto takes (2.0 / 0.4, the window's 1.25 for both);
     * in the true form the same ratio, without the 1.25; the floor of the phase still stops both; the 12 % cap still
     * caps a Phoenix's Burst.
     */
    @GameTest(template = ARENA, batch = "apocalypse_crit_body", timeoutTicks = 60)
    public static void theirHitsAreCriticalOnHisBodyAndOthersWeak(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        ServerLevel level = d.level();
        ApocalypseEntity boss = d.boss();
        ApocalypseFight fight = d.fight();
        ServerPlayer magneto = d.f().player(10.5, FLOOR, 17.5, Power.MAGNETO);
        ServerPlayer phoenix = d.f().player(3.5, FLOOR, 14.5, Power.PHOENIX);
        tank(magneto);
        tank(phoenix);
        DamageSource wandaSlam = d.by(ModDamageTypes.TELEKINETIC_SLAM);
        DamageSource magnetoSlam = ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, magneto, magneto);

        // A window, his armour cut.
        breakShield(d);
        boss.setArmourCut(true);
        double hers = designLoss(boss, wandaSlam, 8.0F);
        double his = designLoss(boss, magnetoSlam, 8.0F);
        assertNear(hers, 8.0 * 2.0 * 1.25, 1.0E-3, "in a window her slam of 8 takes 8 x 2 x 1.25");
        assertNear(his, 8.0 * 0.4 * 1.25, 1.0E-3, "Magneto's 8 x 0.4 x 1.25");
        assertNear(hers / his, 5.0, 1.0E-3, "five times as much");
        double cap = 0.12 * boss.designMax();
        double burst = designLoss(boss, ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, phoenix, phoenix), 100.0F);
        assertNear(burst, cap, 1.0E-2, "a critical Burst of 100 (250) is capped at 12 percent of his health");
        // The floor of the window: two thirds, for both.
        float floor = boss.floorHealth();
        boss.setFraction(boss.floorFraction() + 2.0 / boss.designMax());
        hurt(boss, wandaSlam, 8.0F);
        assertNear(boss.getHealth(), floor, 1.0E-3, "her critical slam stops at the floor of the window");
        boss.setFraction(boss.floorFraction() + 2.0 / boss.designMax());
        hurt(boss, magnetoSlam, 8.0F);
        assertNear(boss.getHealth(), floor, 1.0E-3, "so does his");

        // The true form: the same ratio, without the window's quarter.
        fight.forceState(ApocalypseFight.Stage.TRUE_FORM, 3);
        calm(fight);
        check(boss.isTrueForm() && boss.shield().state() == ApocalypseShield.State.GONE, "the true form");
        double hersTrue = designLoss(boss, wandaSlam, 8.0F);
        double hisTrue = designLoss(boss, magnetoSlam, 8.0F);
        assertNear(hersTrue, 8.0 * 2.0, 1.0E-3, "in the true form her slam of 8 takes 16");
        assertNear(hisTrue, 8.0 * 0.4, 1.0E-3, "Magneto's 3.2");
        assertNear(hersTrue / hisTrue, 5.0, 1.0E-3, "five times as much");
        float escape = boss.floorHealth();
        boss.setFraction(boss.floorFraction() + 2.0 / boss.designMax());
        hurt(boss, wandaSlam, 8.0F);
        assertNear(boss.getHealth(), escape, 1.0E-3, "her slam stops at the floor of the true form (where he tries to escape)");
        boss.setFraction(boss.floorFraction() + 2.0 / boss.designMax());
        hurt(boss, magnetoSlam, 8.0F);
        assertNear(boss.getHealth(), escape, 1.0E-3, "so does his");
        helper.succeed();
    }

    /**
     * T66 (8a.2): with {@code billy} added to {@code mindPowerHeroes} (not to {@code critHeroes}), a {@code
     * telekinetic_slam} of 8 caused by Billy wears the boss's shield by 8 x 1 and shows no crit; the Apocalypse hero (a
     * player, his shield up) hit by Wanda's beam loses exactly {@link MindPower#shieldDamage} (as in 3.0), and she sees
     * no crit.
     */
    @GameTest(template = ARENA, batch = "apocalypse_crit_billy", timeoutTicks = 60)
    public static void billyAndTheHeroGetNoCrit(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.BILLY);
        d.f().onClose(Telekinesis::clear);
        d.f().config(ApocalypseShieldConfig.MIND_POWER_HEROES, List.of("scarlet_witch", "phoenix", "billy"));
        ServerLevel level = d.level();
        ApocalypseEntity boss = d.boss();
        ServerPlayer billy = d.hero();
        check(MindPower.isBreaker(billy) && !MindPower.isCritHero(billy), "Billy breaks the shield, but is no crit hero");
        List<Object> billySaw = new ArrayList<>();
        drain(billy, billySaw);
        billySaw.clear();
        float max = boss.shield().strength();
        check(d.hit(ModDamageTypes.TELEKINETIC_SLAM, 8.0F), "Billy's slam reaches the shield");
        assertNear(max - boss.shield().strength(), 8.0, 1.0E-3, "and wears it by 8 x 1");
        drain(billy, billySaw);
        check(critBars(billySaw).isEmpty() && critSounds(billySaw) == 0 && critSparks(billySaw, null) == 0, "with no crit: " + critBars(billySaw));

        // The Apocalypse hero's shield reads the same rule, never multiplied.
        ServerPlayer hero = d.f().player(16.5, FLOOR, 16.5, Power.APOCALYPSE);
        ServerPlayer wanda = d.f().player(16.5, FLOOR, 12.5, Power.SCARLET_WITCH);
        PowerData data = Powers.get(hero);
        data.raiseChaosShield(level.getGameTime(), 0, 60.0F);
        check(ApocalypseGuard.isUp(hero), "the hero's shield is up with 60 points");
        List<Object> wandaSaw = new ArrayList<>();
        drain(wanda, wandaSaw);
        wandaSaw.clear();
        DamageSource beam = ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, wanda, wanda);
        float wear = MindPower.shieldDamage(beam, 5.0F);
        assertNear(wear, 5.0, 1.0E-6, "a beam of 5 is 5 of mind power");
        LivingIncomingDamageEvent event = new LivingIncomingDamageEvent(hero, new DamageContainer(beam, 5.0F));
        NeoForge.EVENT_BUS.post(event);
        check(event.isCanceled(), "her beam is held by his shield");
        assertNear(data.shieldStrength(), 60.0 - wear, 1.0E-4, "which loses exactly MindPower.shieldDamage, as in 3.0");
        drain(wanda, wandaSaw);
        check(critBars(wandaSaw).isEmpty() && critSounds(wandaSaw) == 0, "and she sees no crit: " + critBars(wandaSaw));
        helper.succeed();
    }

    /**
     * T67 (8a.1): by default his health never heals: his shield up for 200 ticks at half his health, it does not rise
     * (nor by the game's own healing, a potion's); in the true form at 30 %, 200 ticks after a blow, it does not
     * either; and his shield, worn to 30, still mends once it has had 160 ticks without mind power.
     */
    @GameTest(template = ARENA, batch = "apocalypse_no_regen", timeoutTicks = 440)
    public static void heNeverHealsByDefault(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        d.f().onClose(Telekinesis::clear);
        ApocalypseEntity boss = d.boss();
        ApocalypseFight fight = d.fight();
        boss.setFraction(0.5);
        float half = boss.getHealth();
        boss.heal(50.0F);
        check(boss.getHealth() == half, "the game's own healing does nothing to him: " + boss.getHealth());
        // Wear the shield to 30: (150 - 30) / 2, a critical beam.
        float max = boss.shield().strength();
        check(d.hit(ModDamageTypes.CHAOS_BEAM, (max - 30.0F) / 2.0F), "her beam wears the shield");
        assertNear(boss.shield().strength(), 30.0, 1.0E-3, "to 30");
        int delay = ApocalypseShieldConfig.SHIELD_REGEN_DELAY_TICKS.get();
        float[] afterBlow = {-1.0F};
        every(helper, 420, tick -> {
            if (tick <= 200) {
                check(boss.shield().isUp(), "the shield stays up (tick " + tick + ")");
                check(boss.getHealth() == half, "his health does not rise behind his shield (tick " + tick + "): " + boss.getHealth());
                if (tick == delay - 2) {
                    assertNear(boss.shield().strength(), 30.0, 1.0E-3, "the shield waits " + delay + " ticks without mind power");
                }
                if (tick == 200) {
                    check(boss.shield().strength() > 30.5F, "then it mends: " + boss.shield().strength());
                    fight.forceState(ApocalypseFight.Stage.TRUE_FORM, 3);
                    calm(fight);
                    boss.setFraction(0.30);
                    boss.invulnerableTime = 0;
                    check(boss.hurt(d.f().level().damageSources().playerAttack(d.hero()), 5.0F), "a blow in the true form");
                    afterBlow[0] = boss.getHealth();
                    check(afterBlow[0] < boss.healthAt(0.30), "took something");
                }
                return false;
            }
            check(fight.stage() == ApocalypseFight.Stage.TRUE_FORM, "still the true form (tick " + tick + "): " + fight.stage());
            check(boss.getHealth() == afterBlow[0], "his health does not rise in the true form (tick " + tick + "): " + boss.getHealth());
            return tick >= 400;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** One blow with no invulnerability frames in the way. */
    private static void hurt(ApocalypseEntity boss, DamageSource source, float amount) {
        boss.invulnerableTime = 0;
        boss.hurt(source, amount);
    }

    /** What one blow took from his health, in the design's points (a fight of many has more than the attribute). */
    private static double designLoss(ApocalypseEntity boss, DamageSource source, float amount) {
        float before = boss.getHealth();
        hurt(boss, source, amount);
        return (before - boss.getHealth()) * (double) boss.designPerHealth();
    }

    /** Where a blow of {@code attacker}'s strikes the sphere of his shield (as the boss works it out). */
    private static Vec3 hitPoint(ApocalypseEntity boss, ServerPlayer attacker) {
        Vec3 center = boss.position().add(0, boss.getBbHeight() * 0.5, 0);
        Vec3 from = attacker.position().add(0, attacker.getBbHeight() * 0.5, 0);
        return center.add(from.subtract(center).normalize().scale(ApocalypseShield.RADIUS));
    }

    /** Adds what the player was sent since the last look to {@code into}. */
    private static void drain(ServerPlayer player, List<Object> into) {
        into.addAll(TestKit.sentPackets(player));
    }

    /** How many {@code CRIT} sparks were sent, at {@code at} (to a hundredth of a block), or anywhere if it is null. */
    private static int critSparks(List<Object> packets, Vec3 at) {
        return sparks(packets, at, ParticleTypes.CRIT.getType());
    }

    private static int enchantedSparks(List<Object> packets, Vec3 at) {
        return sparks(packets, at, ParticleTypes.ENCHANTED_HIT.getType());
    }

    private static int sparks(List<Object> packets, Vec3 at, Object type) {
        int count = 0;
        for (ClientboundLevelParticlesPacket packet : TestKit.particlesIn(packets)) {
            if (packet.getParticle().getType() == type
                    && (at == null || new Vec3(packet.getX(), packet.getY(), packet.getZ()).distanceTo(at) < 0.01)) {
                count += packet.getCount();
            }
        }
        return count;
    }

    /** How many times the crack of a critical hit (until its own sound: the player's crit at pitch 0.7) was heard. */
    private static int critSounds(List<Object> packets) {
        int count = 0;
        for (Object sent : packets) {
            if (sent instanceof ClientboundSoundPacket sound && sound.getSound().value() == SoundEvents.PLAYER_ATTACK_CRIT
                    && Math.abs(sound.getPitch() - 0.7F) < 1.0E-3F) {
                count++;
            }
        }
        return count;
    }

    /** The factors of the "Critical hit! x%s" lines on the action bar, oldest first. */
    private static List<String> critBars(List<Object> packets) {
        List<String> factors = new ArrayList<>();
        for (Object sent : packets) {
            if (sent instanceof ClientboundSystemChatPacket chat && chat.overlay() && chat.content().getContents() instanceof TranslatableContents text
                    && text.getKey().equals(ApocalypseEntity.CRIT_MESSAGE)) {
                Object factor = text.getArgs().length > 0 ? text.getArgs()[0] : "";
                factors.add(factor instanceof Component component ? component.getString() : String.valueOf(factor));
            }
        }
        return factors;
    }

    private ApocalypseCritTests() {
    }
}
