package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.apocalypse.Horsemen;
import com.kolodkingal.enhanced.apocalypse.SandControl;
import com.kolodkingal.enhanced.apocalypse.SandHazards;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGifts;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import com.kolodkingal.enhanced.entity.SandLanceEntity;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Log;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.breakShield;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.calm;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.duel;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.every;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.noCrits;
import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.put;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The fight as a whole (spec 3.7 and 8, tests 30 to 42): the transitions with their Horsemen and their Cairo Collapse, the
 * third break and the true form, the Escape and what interrupts it, the kneel and the two deaths, who is rewarded, how a
 * fight can end and what each ending gives back, the arena, the bars, and the three things the heroes do to him (Quicksilver
 * Time, Magneto's strip). The fight is jumped to a stage with {@code forceState}, so each test is only about its stage.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ApocalypseFightTests {

    // ------------------------------------------------------------------------------------------------------------
    // The first transition: the Horsemen

    @GameTest(template = ARENA, batch = "apocalypse_t1", timeoutTicks = 260)
    public static void transitionOneCallsTheHorsemen(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.NONE);
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        ServerPlayer second = d.f().player(14.5, FLOOR, 4.5);
        ApocalypseTestSupport.tank(second);
        ServerPlayer magneto = d.f().player(4.5, FLOOR, 16.5, Power.MAGNETO);
        ApocalypseTestSupport.tank(magneto);
        Log log = new Log(d.hero());
        helper.runAfterDelay(4, () -> {
            check(fight.fighterCount() == 3, "three fight: " + fight.fighterCount());
            check(Horsemen.countFor(1) == 2 && Horsemen.countFor(2) == 3 && Horsemen.countFor(3) == 4 && Horsemen.countFor(6) == 4, "min(4, 1 + n) Horsemen");
            fight.forceState(ApocalypseFight.Stage.TRANSITION_1, 1);
            check(fight.stage() == ApocalypseFight.Stage.TRANSITION_1 && boss.state() == ApocalypseState.HOVER, "he rises and hangs");
            check(boss.shield().state() == ApocalypseShield.State.GONE && fight.bossInvulnerable(), "the first shield is gone, and nothing hurts him");
            boolean[] finished = {false};
            every(helper, 160, tick -> {
                if (fight.stage() == ApocalypseFight.Stage.TRANSITION_1 && fight.stageTicks() < 60) {
                    check(fight.horsemenAlive().isEmpty(), "they come out of the portals at tick 60, not before (" + fight.stageTicks() + ")");
                    return false;
                }
                if (fight.stage() == ApocalypseFight.Stage.TRANSITION_1 && fight.stageTicks() < 104) {
                    List<Husk> horsemen = fight.horsemenAlive();
                    check(horsemen.size() == 4, "min(4, 1 + 3) = 4 Horsemen: " + horsemen.size());
                    Set<Horsemen.Kind> kinds = new HashSet<>();
                    for (Husk husk : horsemen) {
                        Horsemen.Kind kind = Horsemen.kindOf(husk);
                        check(kind != null && kinds.add(kind), "each of a different kind");
                        assertNear(husk.getMaxHealth(), 40.0, 1.0E-3, "twice the health of a Husk");
                        if (kind == Horsemen.Kind.FAMINE || kind == Horsemen.Kind.DEATH) {
                            assertNear(husk.getAttributeValue(Attributes.ATTACK_DAMAGE), 7.0, 1.0E-3, "four more damage than a Husk (3)");
                        } else {
                            check(husk.getAttributeValue(Attributes.ATTACK_DAMAGE) >= 7.0, "four more damage, and its weapon on top");
                        }
                        check(husk.hasCustomName() && husk.isCustomNameVisible() && husk.isCurrentlyGlowing(), "a name above its head, glowing");
                        check(fight.owns(husk) && BossFights.fightOf(husk) == fight, "the fight's own");
                    }
                    check(Telekinesis.canLift(magneto, horsemen.stream().filter(h -> Horsemen.kindOf(h) == Horsemen.Kind.WAR).findFirst().orElseThrow(), Telekinesis.Mode.MAGNETO),
                            "War is magnetic: Magneto can lift it");
                    check(!Telekinesis.canLift(magneto, horsemen.stream().filter(h -> Horsemen.kindOf(h) == Horsemen.Kind.FAMINE).findFirst().orElseThrow(),
                            Telekinesis.Mode.MAGNETO), "Famine is not");
                    return false;
                }
                if (fight.stage() == ApocalypseFight.Stage.TRANSITION_1 && fight.stageTicks() >= 104) {
                    check(boss.shield().state() == ApocalypseShield.State.FORMING, "his second shield knits from tick 100");
                    assertNear(boss.shield().max(), ApocalypseShield.strengthFor(2, 3), 1.0E-3, "at the strength of shield 2 for three fighters");
                    return false;
                }
                if (fight.stage() == ApocalypseFight.Stage.FIGHT && !finished[0]) {
                    finished[0] = true;
                    check(fight.phase() == 2 && boss.phase() == 2, "phase 2 begins after 120 ticks");
                    check(!boss.isNoGravity() && boss.state() == ApocalypseState.IDLE, "he is on the ground");
                    // Whoever falls leaves nothing behind: no drops, no experience.
                    Husk husk = fight.horsemenAlive().get(0);
                    BlockPos at = husk.blockPosition();
                    husk.hurt(level.damageSources().generic(), 1000.0F);
                    check(!husk.isAlive(), "a Horseman can be killed");
                    helper.runAfterDelay(2, () -> {
                        AABB around = new AABB(at).inflate(5.0);
                        check(level.getEntitiesOfClass(ItemEntity.class, around).isEmpty(), "and drops nothing");
                        check(level.getEntitiesOfClass(ExperienceOrb.class, around).isEmpty(), "and no experience");
                        check(log.drain().said("speech.enhanced.apocalypse.horsemen"), "he said his line");
                        helper.succeed();
                    });
                    return false;
                }
                return false;
            });
        });
    }

    /** One Horseman, still, next to a hero: the trait it has fires within its period, and it does what the table says. */
    private static void traitTest(GameTestHelper helper, Horsemen.Kind kind, double heroX, java.util.function.Consumer<TraitRun> step) {
        Duel d = duel(helper, 1, Power.NONE);
        ServerPlayer hero = d.hero();
        ServerLevel level = d.level();
        put(d.f(), hero, heroX, FLOOR, 10.5);
        d.f().player(0.5, FLOOR, 0.5); // a second pair of eyes is not needed; keeps the Horseman's nearest-target logic honest
        Husk husk = d.fight().horsemen().spawn(level, kind, helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)));
        husk.setNoAi(true);
        d.boss().moveTo(helper.absoluteVec(new Vec3(18.5, FLOOR, 18.5)));
        TraitRun run = new TraitRun(d, husk, hero.getHealth());
        every(helper, 110, tick -> {
            step.accept(run);
            return run.done;
        });
    }

    private static final class TraitRun {
        final Duel d;
        final Husk husk;
        final float heroHealth;
        final Set<UUID> seen = new HashSet<>();
        boolean done;

        TraitRun(Duel d, Husk husk, float heroHealth) {
            this.d = d;
            this.husk = husk;
            this.heroHealth = heroHealth;
        }
    }

    @GameTest(template = ARENA, batch = "apocalypse_horseman_war", timeoutTicks = 160)
    public static void warFiresAVolleyOfThreeIronShards(GameTestHelper helper) {
        traitTest(helper, Horsemen.Kind.WAR, 3.5, run -> {
            for (IronShrapnelEntity shard : run.d.level().getEntitiesOfClass(IronShrapnelEntity.class, new AABB(run.husk.blockPosition()).inflate(25.0))) {
                if (shard.getOwner() == run.husk) {
                    run.seen.add(shard.getUUID());
                }
            }
            if (run.seen.size() >= 3) {
                check(run.seen.size() == 3, "three iron shards in a fan: " + run.seen.size());
                run.done = true;
            }
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_horseman_famine", timeoutTicks = 160)
    public static void famineCallsAVisualBoltAndAGust(GameTestHelper helper) {
        traitTest(helper, Horsemen.Kind.FAMINE, 5.5, run -> {
            List<LightningBolt> bolts = run.d.level().getEntitiesOfClass(LightningBolt.class, new AABB(run.husk.blockPosition()).inflate(30.0));
            if (!bolts.isEmpty()) {
                check(run.d.level().getBlockState(run.husk.blockPosition().offset(-3, 0, 0)).isAir(), "the bolt sets nothing alight");
                check(run.d.hero().getHealth() < run.heroHealth && run.heroHealth - run.d.hero().getHealth() <= 4.6F,
                        "the gust hurts for about 3: " + (run.heroHealth - run.d.hero().getHealth()));
                run.done = true;
            }
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_horseman_pestilence", timeoutTicks = 160)
    public static void pestilenceLeapsWithASlowingBlade(GameTestHelper helper) {
        traitTest(helper, Horsemen.Kind.PESTILENCE, 9.5, run -> {
            if (run.d.hero().hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                check(run.d.hero().getHealth() < run.heroHealth, "the blade cut as well: " + (run.heroHealth - run.d.hero().getHealth()));
                run.done = true;
            }
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_horseman_death", timeoutTicks = 160)
    public static void deathDivesAndSlamsInARadius(GameTestHelper helper) {
        traitTest(helper, Horsemen.Kind.DEATH, 8.5, run -> {
            if (run.d.hero().getHealth() < run.heroHealth) {
                check(run.heroHealth - run.d.hero().getHealth() >= 4.0F, "the slam hurt: " + (run.heroHealth - run.d.hero().getHealth()));
                run.done = true;
            }
        });
    }

    /**
     * The Horsemen are one side (found in the real game: War's volley cut Pestilence down, Famine was hurt and War slew
     * him, all within half a minute of their coming). Neither his shards nor his axe hurt another Horseman, nor do they
     * hurt the boss, and no Horseman takes another for a target. One that a Phoenix holds is hers: it fights them, and
     * they may fight it back.
     */
    @GameTest(template = ARENA, batch = "apocalypse_horsemen_one_side", timeoutTicks = 60)
    public static void horsemenDoNotHurtOneAnother(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.NONE);
        ServerLevel level = d.level();
        Husk war = d.fight().horsemen().spawn(level, Horsemen.Kind.WAR, helper.absoluteVec(new Vec3(6.5, FLOOR, 16.5)));
        Husk pestilence = d.fight().horsemen().spawn(level, Horsemen.Kind.PESTILENCE, helper.absoluteVec(new Vec3(8.5, FLOOR, 16.5)));
        war.setNoAi(true);
        pestilence.setNoAi(true);
        float full = pestilence.getHealth();
        IronShrapnelEntity shard = new IronShrapnelEntity(level, war);
        check(!pestilence.hurt(ModDamageTypes.source(level, ModDamageTypes.SHRAPNEL, war, shard), 4.0F), "War's shard does not hurt Pestilence");
        check(!pestilence.hurt(level.damageSources().mobAttack(war), 7.0F), "nor does his axe");
        check(!war.hurt(level.damageSources().mobAttack(d.boss()), 7.0F), "nor does the boss hurt his own");
        check(pestilence.getHealth() == full && pestilence.getLastHurtByMob() == null, "Pestilence is whole and holds nothing against War");
        float shield = d.boss().shield().strength();
        check(!d.boss().hurt(ModDamageTypes.source(level, ModDamageTypes.SHRAPNEL, war, shard), 4.0F) && d.boss().shield().strength() == shield,
                "a shard of War's that flies into the boss is nothing to him");
        pestilence.setTarget(war);
        check(pestilence.getTarget() == null, "Pestilence does not take War for a target");
        pestilence.setTarget(d.hero());
        check(pestilence.getTarget() == d.hero(), "the fighters are still its prey");
        // Taken over by a Phoenix, War fights for her, and Pestilence may answer.
        war.addEffect(new MobEffectInstance(ModEffects.POSSESSED, 200));
        check(pestilence.hurt(level.damageSources().mobAttack(war), 3.0F), "a possessed War strikes Pestilence");
        pestilence.setTarget(war);
        check(pestilence.getTarget() == war, "and Pestilence may fight him back");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The second transition: the Cairo Collapse

    @GameTest(template = ARENA, batch = "apocalypse_t2", timeoutTicks = 260)
    public static void transitionTwoIsTheCairoCollapse(GameTestHelper helper) {
        Duel d = duel(helper, 2, Power.NONE);
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        ServerPlayer low = d.hero();
        put(d.f(), low, 5.5, FLOOR, 10.5);
        ServerPlayer flier = d.f().player(5.5, FLOOR + 2.0, 12.5);
        ApocalypseTestSupport.tank(flier);
        flier.getAbilities().mayfly = true;
        flier.getAbilities().flying = true;
        // He had called Horsemen: they crumble to sand when it begins.
        Husk husk = fight.horsemen().spawn(level, Horsemen.Kind.WAR, helper.absoluteVec(new Vec3(16.5, FLOOR, 16.5)));
        husk.setNoAi(true);
        float lowHealth = low.getHealth();
        float flierHealth = flier.getHealth();
        helper.runAfterDelay(3, () -> {
            fight.forceState(ApocalypseFight.Stage.TRANSITION_2, 2);
            check(fight.stage() == ApocalypseFight.Stage.TRANSITION_2 && boss.shield().state() == ApocalypseShield.State.GONE, "the second transition begins");
            check(husk.isRemoved() && level.getEntitiesOfClass(ItemEntity.class, new AABB(husk.blockPosition()).inflate(4.0)).isEmpty(),
                    "the Horsemen crumbled without a drop");
            every(helper, 200, tick -> {
                if (fight.stage() == ApocalypseFight.Stage.TRANSITION_2) {
                    if (fight.stageTicks() == 110) {
                        assertNear(lowHealth - low.getHealth(), 36.0, 1.0E-3, "three rings, 12 each, to whoever stands on the ground");
                        check(flier.getHealth() == flierHealth, "and none to the one who flies two blocks up");
                    }
                    if (fight.stageTicks() == 125) {
                        check(boss.shield().state() == ApocalypseShield.State.FORMING, "shield 3 forms from tick 120");
                    }
                    return false;
                }
                check(fight.stage() == ApocalypseFight.Stage.FIGHT && fight.phase() == 3 && boss.phase() == 3, "phase 3 begins after 160 ticks");
                check(!boss.isNoGravity(), "he lands");
                check(SandHazards.all().stream().noneMatch(h -> h.ownerId().equals(boss.getUUID())), "the rings are over");
                return true;
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The third break and the true form

    @GameTest(template = ARENA, batch = "apocalypse_armour", timeoutTicks = 260)
    public static void theThirdBreakDismantlesTheArmour(GameTestHelper helper) {
        Duel d = duel(helper, 3, Power.SCARLET_WITCH);
        noCrits(d.f()); // the armour and the floors, the blows counted as in 3.0 (8a.2)
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(18.5, FLOOR, 18.5));
        DamageSource blow = level.damageSources().mobAttack(zombie);
        Log log = new Log(d.hero());
        double heightBefore = boss.getBbHeight();
        assertNear(boss.getAttributeValue(Attributes.ARMOR), 10.0, 1.0E-3, "his armour is 10 in the armoured form");
        breakShield(d);
        check(fight.stage() == ApocalypseFight.Stage.ARMOUR_FALL && boss.state() == ApocalypseState.ARMOUR_FALL, "the third break is the armour falling");
        check(boss.shield().state() == ApocalypseShield.State.GONE, "the shield is gone for good");
        check(boss.isTrueForm() && boss.getAttributeValue(Attributes.ARMOR) == 0.0 && boss.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0.0, "no armour");
        check(boss.getBbHeight() < heightBefore && boss.getBbWidth() <= 0.81F, "a smaller body: " + boss.getBbWidth() + " x " + boss.getBbHeight());
        assertNear(boss.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.36, 1.0E-3, "quicker");
        check(fight.bossInvulnerable() && !boss.hurt(blow, 20.0F), "nothing hurts him while the plates fall");
        check(!fight.bars().second().isVisible(), "the shield bar is removed");
        check(!log.drain().effects(ApocalypseFxPayload.ARMOUR_FALL).isEmpty(), "the clients are told");
        every(helper, 100, tick -> {
            if (fight.stage() == ApocalypseFight.Stage.ARMOUR_FALL) {
                return false;
            }
            check(fight.stage() == ApocalypseFight.Stage.TRUE_FORM && boss.state() == ApocalypseState.IDLE, "then the true form");
            check(tick >= ApocalypseFight.ARMOUR_FALL_TICKS - 2, "after sixty ticks");
            float before = boss.getHealth();
            boss.invulnerableTime = 0;
            check(boss.hurt(blow, 20.0F), "now everything hurts him");
            assertNear(before - boss.getHealth(), 20.0, 1.0E-3, "fully: no armour, no window to wait for");
            // Floors: 15 percent until the Escape, never lower than the kneel's 5.
            for (int i = 0; i < 12; i++) {
                boss.invulnerableTime = 0;
                boss.hurt(blow, 500.0F);
            }
            check(boss.healthFraction() >= ApocalypseFight.ESCAPE_AT - 0.002 && boss.isAlive(), "nobody brings him below 15 percent before he tries to escape: " + boss.healthFraction());
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_true_regen", timeoutTicks = 300)
    public static void theTrueFormRegeneratesWhenLeftAlone(GameTestHelper helper) {
        Duel d = duel(helper, 3, Power.NONE);
        d.f().config(ApocalypseBossConfig.TRUE_FORM_REGEN_PER_SECOND, 3.0); // 3.0's healing: since 3.1 his health never heals
        noCrits(d.f()); // and 3.0's blows (8a.2)
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        fight.forceState(ApocalypseFight.Stage.TRUE_FORM, 3);
        calm(fight);
        boss.setFraction(0.30);
        float start = boss.getHealth();
        float[] afterBlow = {0.0F};
        every(helper, 260, tick -> {
            if (tick == 100) {
                float gained = boss.getHealth() - start;
                check(gained > 11.0F && gained <= 15.001F, "left alone he mends 3 a second: " + gained);
                boss.invulnerableTime = 0;
                boss.hurt(d.level().damageSources().playerAttack(d.hero()), 5.0F);
                afterBlow[0] = boss.getHealth();
                assertNear(start + gained - afterBlow[0], 5.0, 1.0E-3, "the blow took 5");
            }
            if (tick > 100 && tick < 175) {
                check(boss.getHealth() == afterBlow[0], "a blow stops the mending for four seconds (tick " + tick + ")");
            }
            if (tick == 230) {
                check(boss.getHealth() > afterBlow[0] + 2.9F, "and then he mends again: " + (boss.getHealth() - afterBlow[0]));
                check(boss.healthFraction() <= 1.0 / 3.0 + 0.002, "never above the third he was at when the last shield broke");
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Escape

    @GameTest(template = ARENA, batch = "apocalypse_escape", timeoutTicks = 500)
    public static void theEscapeIsInterruptedByDamageAndStaggersHimOnce(GameTestHelper helper) {
        Duel d = duel(helper, 3, Power.NONE);
        noCrits(d.f()); // the Escape, the blows counted as in 3.0 (8a.2)
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(18.5, FLOOR, 18.5));
        DamageSource blow = level.damageSources().mobAttack(zombie);
        Log log = new Log(d.hero());
        // At 15 percent the channel starts by itself.
        fight.forceState(ApocalypseFight.Stage.TRUE_FORM, 3);
        calm(fight);
        boss.setFraction(0.16);
        boss.invulnerableTime = 0;
        boss.hurt(blow, 20.0F);
        every(helper, 400, tick -> {
            if (tick == 3) {
                check(fight.stage() == ApocalypseFight.Stage.ESCAPE && boss.state() == ApocalypseState.ESCAPING && fight.escapeUsed(),
                        "at 15 percent he channels his escape: " + fight.stage());
                check(!log.drain().effects(ApocalypseFxPayload.ESCAPE).isEmpty(), "the clients see the portal open");
                // Four percent of his health in damage interrupts it.
                for (int i = 0; i < 2; i++) {
                    boss.invulnerableTime = 0;
                    boss.hurt(blow, 20.0F);
                }
            }
            if (tick == 6) {
                check(fight.stage() == ApocalypseFight.Stage.TRUE_FORM, "the blows interrupted it: " + fight.stage());
                check(boss.state() == ApocalypseState.STAGGERED && fight.staggerLeft() > 100, "and he is staggered for six seconds: " + fight.staggerLeft());
                check(fight.escapeUsed() && boss.getHealth() >= boss.healthAt(ApocalypseFight.KNEEL_AT), "he cannot try again");
            }
            if (tick == 8) {
                // Staggered, he is open to every blow a quarter harder, and attacks nothing.
                float before = boss.getHealth();
                boss.invulnerableTime = 0;
                boss.hurt(blow, 8.0F);
                assertNear(before - boss.getHealth(), 10.0, 1.0E-3, "a quarter harder while staggered");
                check(!boss.brain().isBusy(), "and he attacks nothing");
            }
            if (tick == 140) {
                check(fight.staggerLeft() == 0 && boss.state() != ApocalypseState.STAGGERED, "the stagger is over");
                float before = boss.getHealth();
                boss.invulnerableTime = 0;
                boss.hurt(blow, 8.0F);
                assertNear(before - boss.getHealth(), 8.0, 1.0E-3, "a blow is a blow again");
                check(fight.stage() == ApocalypseFight.Stage.TRUE_FORM && fight.escapeUsed(), "no second escape: he is under 15 percent and in the same stage");
                return true;
            }
            return false;
        });
    }

    /**
     * The blow that takes him to 15 percent earns him the Escape whatever second of the game it lands in: his healing (once
     * a second, after four quiet seconds) must not mend him past the line in the same tick first. A blow of something that
     * is not a player does not count as a hit for the rest, so the healing is due at once; the scenario is run in a row of
     * ticks, which meets every moment of that second.
     */
    @GameTest(template = ARENA, batch = "apocalypse_escape_heal", timeoutTicks = 400)
    public static void theBlowThatEarnsTheEscapeStartsItEvenWhenHealingIsDue(GameTestHelper helper) {
        Duel d = duel(helper, 3, Power.NONE);
        noCrits(d.f()); // the Escape, the blows counted as in 3.0 (8a.2)
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(18.5, FLOOR, 18.5));
        DamageSource blow = level.damageSources().mobAttack(zombie);
        int[] rounds = {0};
        every(helper, 380, tick -> {
            if (level.getGameTime() < 100) {
                return false; // the quiet that the healing waits for is over in any case: this is only for a short-lived server
            }
            if (rounds[0] > 0) {
                check(fight.stage() == ApocalypseFight.Stage.ESCAPE && boss.state() == ApocalypseState.ESCAPING,
                        "round " + rounds[0] + ", game second " + (level.getGameTime() % 20) + ": he channels his escape: " + fight.stage() + " at " + boss.healthFraction());
            }
            if (rounds[0] == 45) {
                return true;
            }
            rounds[0]++;
            fight.setEscapeUsed(false);
            fight.forceState(ApocalypseFight.Stage.TRUE_FORM, 3);
            calm(fight);
            boss.setFraction(0.16);
            boss.invulnerableTime = 0;
            check(boss.hurt(blow, 20.0F), "the blow lands");
            return false;
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_escape_bolt", timeoutTicks = 200)
    public static void lightningAndAHexWallInterruptTheEscape(GameTestHelper helper) {
        Duel d = duel(helper, 3, Power.NONE);
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        d.f().config(ApocalypseBossConfig.ESCAPE_CHANNEL_TICKS, 20);
        fight.forceState(ApocalypseFight.Stage.ESCAPE, 3);
        calm(fight);
        check(fight.stage() == ApocalypseFight.Stage.ESCAPE, "the channel is on");
        // Any lightning damage that gets through interrupts it.
        boss.hurt(level.damageSources().lightningBolt(), 2.0F);
        helper.runAfterDelay(3, () -> {
            check(fight.stage() == ApocalypseFight.Stage.TRUE_FORM && boss.state() == ApocalypseState.STAGGERED, "a bolt of lightning interrupted it: " + fight.stage());
            // A Hex wall refuses the teleport at the end of the channel.
            fight.setEscapeUsed(false);
            fight.forceState(ApocalypseFight.Stage.ESCAPE, 3);
            calm(fight);
            ServerPlayer witch = d.f().player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
            d.f().config(com.kolodkingal.enhanced.EnhancedConfig.HEX_DEFAULT_RADIUS, 6);
            AbilityHandler.onKey(witch, 5, true);
            check(HexManager.ownedBy(level, witch.getUUID()).isPresent(), "a Hex stands round the middle");
            helper.runAfterDelay(30, () -> {
                check(fight.stage() == ApocalypseFight.Stage.TRUE_FORM && boss.state() == ApocalypseState.STAGGERED,
                        "the Hex refused his portal: " + fight.stage());
                check(!boss.isInvisible(), "he is still here");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_vanish", timeoutTicks = 420)
    public static void anUninterruptedEscapeReturnsHimHealedWithANewThirdShield(GameTestHelper helper) {
        Duel d = duel(helper, 3, Power.NONE);
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        d.f().config(ApocalypseBossConfig.ESCAPE_CHANNEL_TICKS, 20);
        fight.forceState(ApocalypseFight.Stage.ESCAPE, 3);
        calm(fight);
        every(helper, 300, tick -> {
            if (tick == 30) {
                check(fight.stage() == ApocalypseFight.Stage.VANISHED, "the channel ran out and he is gone: " + fight.stage());
                check(boss.isInvisible() && boss.isSilent(), "invisible and silent");
                check(!fight.bars().second().isVisible(), "no shield bar");
            }
            if (tick == 150) {
                check(fight.stage() == ApocalypseFight.Stage.VANISHED, "ten seconds away");
            }
            if (tick >= 40 && fight.stage() == ApocalypseFight.Stage.FIGHT) {
                check(!boss.isInvisible() && !boss.isSilent(), "he is back");
                check(boss.healthFraction() >= ApocalypseBossConfig.ESCAPE_HEAL_TO.get() - 0.002, "at 40 percent of his health: " + boss.healthFraction());
                check(boss.shield().state() == ApocalypseShield.State.FORMING && boss.shield().max() == ApocalypseShield.strengthFor(3, 1), "behind a new third shield");
                check(fight.escapeUsed(), "and the Escape is spent");
                check(tick > 200, "after ten seconds: " + tick);
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The end

    @GameTest(template = ARENA, batch = "apocalypse_end_sand", timeoutTicks = 480)
    public static void theEndKneelsThenDiesInSand(GameTestHelper helper) {
        endTest(helper, false);
    }

    @GameTest(template = ARENA, batch = "apocalypse_end_fire", timeoutTicks = 480)
    public static void theEndKneelsThenDiesInThePhoenixFire(GameTestHelper helper) {
        endTest(helper, true);
    }

    private static void endTest(GameTestHelper helper, boolean phoenix) {
        Duel d = duel(helper, 3, phoenix ? Power.PHOENIX : Power.NONE);
        noCrits(d.f()); // the end, the blows counted as in 3.0 (8a.2)
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        ServerLevel level = d.level();
        fight.forceState(ApocalypseFight.Stage.TRUE_FORM, 3);
        fight.setEscapeUsed(true);
        boss.setFloor(ApocalypseFight.KNEEL_AT);
        calm(fight);
        boss.setFraction(0.08);
        Log log = new Log(d.hero());
        helper.runAfterDelay(3, () -> {
            DamageSource finishing = phoenix ? d.by(ModDamageTypes.PHOENIX_BURST) : level.damageSources().playerAttack(d.hero());
            boss.invulnerableTime = 0;
            boss.hurt(finishing, 60.0F);
            check(boss.healthFraction() >= ApocalypseFight.KNEEL_AT - 0.002, "nobody takes him under 5 percent: " + boss.healthFraction());
            helper.runAfterDelay(3, () -> {
                check(fight.stage() == ApocalypseFight.Stage.KNEEL && boss.state() == ApocalypseState.KNEELING, "at 5 percent he kneels: " + fight.stage());
                check(fight.bossInvulnerable() && !boss.hurt(level.damageSources().generic(), 50.0F), "and nothing hurts him");
                check(log.drain().said("speech.enhanced.apocalypse.revealed"), "\"All is revealed.\"");
                check(boss.diesInFire() == phoenix, "he will die in " + (phoenix ? "the Phoenix's fire" : "sand"));
                check(fight.participant(d.hero().getUUID()).struckLastBlow(), "whoever took him down struck the last blow");
                helper.runAfterDelay(ApocalypseFight.KNEEL_TICKS + 4, () -> {
                    check(fight.isOver() && fight.result() == BossEnd.VICTORY && fight.stage() == ApocalypseFight.Stage.DEATH, "then he dies: " + fight.result());
                    check(boss.state() == ApocalypseState.DYING && boss.isAlive(), "dissolving");
                    Log after = log.drain();
                    check(after.effects(phoenix ? ApocalypseFxPayload.DEATH_PHOENIX : ApocalypseFxPayload.DEATH_SAND).size() == 1,
                            "the clients are sent the right death");
                    check(after.effects(phoenix ? ApocalypseFxPayload.DEATH_SAND : ApocalypseFxPayload.DEATH_PHOENIX).isEmpty(), "and only that one");
                    helper.runAfterDelay(ApocalypseFight.DEATH_TICKS + 4, () -> {
                        check(boss.isRemoved(), "he is gone");
                        check(level.getEntitiesOfClass(ItemEntity.class, new AABB(boss.blockPosition()).inflate(12.0)).stream()
                                .noneMatch(i -> !i.getItem().is(ModItems.ESSENCE.get())), "he dropped nothing");
                        check(level.getEntitiesOfClass(ExperienceOrb.class, new AABB(boss.blockPosition()).inflate(12.0)).isEmpty(), "no experience");
                        helper.succeed();
                    });
                });
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The reward

    @GameTest(template = ARENA, batch = "apocalypse_rewards", timeoutTicks = 400)
    public static void everyQualifyingFighterGetsTheEssence(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        f.config(BossConfig.REWARD_MIN_SECONDS, 3);
        f.config(BossConfig.XP_REWARD_PER_PLAYER, 1500);
        f.config(ApocalypseBossConfig.DROPS_PHOENIX_EMBER, true);
        MinecraftServer server = f.level().getServer();
        ServerPlayer shieldOnly = f.player(10.5, FLOOR, 10.5);
        ApocalypseFight fight = ApocalypseTestSupport.inPhase(f, shieldOnly, 3);
        put(f, shieldOnly, 4.5, FLOOR, 4.5);
        ApocalypseTestSupport.tank(shieldOnly);
        ServerPlayer lastBlow = f.player(16.5, FLOOR, 4.5, Power.SCARLET_WITCH);
        ServerPlayer creative = f.player(16.5, FLOOR, 16.5, GameType.CREATIVE);
        ServerPlayer dead = f.player(4.5, FLOOR, 16.5);
        ServerPlayer full = f.player(10.5, FLOOR, 16.5);
        for (int slot = 0; slot < full.getInventory().items.size(); slot++) {
            full.getInventory().items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        helper.runAfterDelay(70, () -> {
            // Late on the scene, with some damage but too little time.
            ServerPlayer late = f.player(8.5, FLOOR, 8.5);
            helper.runAfterDelay(30, () -> {
                fight.credit(shieldOnly, "shield", 40.0F);
                fight.credit(late, BossFight.DAMAGE, 50.0F);
                fight.credit(creative, BossFight.DAMAGE, 50.0F);
                fight.credit(dead, BossFight.DAMAGE, 50.0F);
                fight.credit(full, BossFight.DAMAGE, 50.0F);
                fight.lastBlow(lastBlow);
                check(fight.participant(shieldOnly.getUUID()).secondsPresent() >= 3 && fight.participant(late.getUUID()).secondsPresent() < 3,
                        "the first has been there for three seconds and the second has not");
                int xpBefore = shieldOnly.totalExperience;
                dead.kill();
                fight.end(BossEnd.VICTORY);
                check(essences(shieldOnly) == 1, "a fighter with only shield damage and the time is rewarded");
                check(shieldOnly.totalExperience - xpBefore >= 1500, "with the experience");
                check(shieldOnly.getInventory().contains(new ItemStack(ModItems.PHOENIX_EMBER.get())), "and an Ember, as the server asked, having no power");
                check(essences(late) == 0, "one who came late is not");
                check(essences(lastBlow) == 1, "the one who struck the last blow is, whatever their time");
                check(!lastBlow.getInventory().contains(new ItemStack(ModItems.PHOENIX_EMBER.get())), "(no Ember for a hero)");
                check(essences(creative) == 0, "a Creative player never is");
                check(BossGifts.pending(server, dead.getUUID()).stream().anyMatch(s -> s.is(ModItems.ESSENCE.get())), "the dead one's waits as a gift");
                check(f.level().getEntitiesOfClass(ItemEntity.class, full.getBoundingBox().inflate(3.0)).stream().anyMatch(i -> i.getItem().is(ModItems.ESSENCE.get())),
                        "a full inventory drops it at their feet");
                helper.succeed();
            });
        });
    }

    private static int essences(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.ESSENCE.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // ------------------------------------------------------------------------------------------------------------
    // How it can end, and what is given back

    @GameTest(template = ARENA, batch = "apocalypse_withdraw", timeoutTicks = 400)
    public static void noBreakerWithdrawsAndRefundsTheCapstone(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ApocalypseRitualTests.readyGround(f);
        f.config(ApocalypseBossConfig.NO_BREAKER_SECONDS, 2);
        ServerLevel level = f.level();
        ServerPlayer wanda = f.player(10.5, FLOOR, 15.5, Power.SCARLET_WITCH);
        ApocalypseTestSupport.tank(wanda);
        ServerPlayer starter = ApocalypseRitualTests.starter(f, 10.5, 5.5);
        helper.runAfterDelay(3, () -> {
            ApocalypseRitualTests.plant(starter, helper.absolutePos(ApocalypseRitualTests.CLICK));
            ApocalypseFight fight = (ApocalypseFight) BossFights.active(level);
            check(fight != null && starter.getMainHandItem().isEmpty(), "the ritual began and the Capstone is gone");
            fight.forceState(ApocalypseFight.Stage.FIGHT, 1);
            calm(fight);
            wanda.kill();
            helper.runAfterDelay(30, () -> {
                check(!fight.isOver(), "thirty ticks without a breaker are not a minute");
                // A breaker returning resets the clock.
                ServerPlayer second = f.player(10.5, FLOOR, 15.5, Power.PHOENIX);
                helper.runAfterDelay(30, () -> {
                    check(!fight.isOver(), "a Phoenix has come: the clock starts again");
                    second.kill();
                    helper.runAfterDelay(30, () -> {
                        check(!fight.isOver(), "thirty more");
                        helper.runAfterDelay(20, () -> {
                            check(fight.isOver() && fight.result() == BossEnd.WITHDRAW, "no one can break his shield: he withdraws: " + fight.result());
                            check(starter.getInventory().contains(new ItemStack(ModItems.CAPSTONE.get())), "and the Capstone is given back to whoever planted it");
                            helper.succeed();
                        });
                    });
                });
            });
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_stop", timeoutTicks = 200)
    public static void anOperatorStopRefundsAndAWipeDoesNot(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ApocalypseRitualTests.readyGround(f);
        f.config(BossConfig.WIPE_SECONDS, 1);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer wanda = f.player(10.5, FLOOR, 15.5, Power.SCARLET_WITCH);
        ApocalypseTestSupport.tank(wanda);
        ServerPlayer starter = ApocalypseRitualTests.starter(f, 10.5, 5.5);
        helper.runAfterDelay(3, () -> {
            ApocalypseRitualTests.plant(starter, helper.absolutePos(ApocalypseRitualTests.CLICK));
            ApocalypseFight fight = (ApocalypseFight) BossFights.active(level);
            check(fight != null, "the ritual began");
            fight.forceState(ApocalypseFight.Stage.FIGHT, 1);
            calm(fight);
            BossFights.stopAll(server, StopReason.COMMAND);
            check(fight.isOver() && fight.result() == BossEnd.COMMAND, "an operator stopped it");
            check(starter.getInventory().contains(new ItemStack(ModItems.CAPSTONE.get())), "the Capstone is back");
            BossFights.clear();
            // A second fight, lost: the Capstone is gone for good.
            starter.getInventory().clearContent();
            starter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAPSTONE.get()));
            helper.runAfterDelay(25, () -> {
                ApocalypseRitualTests.plant(starter, helper.absolutePos(ApocalypseRitualTests.CLICK));
                ApocalypseFight second = (ApocalypseFight) BossFights.active(level);
                check(second != null, "a second ritual");
                second.forceState(ApocalypseFight.Stage.FIGHT, 1);
                calm(second);
                wanda.kill();
                starter.kill();
                helper.runAfterDelay(60, () -> {
                    check(second.isOver() && second.result() == BossEnd.WIPE, "everyone fell: a wipe: " + second.result());
                    check(!starter.getInventory().contains(new ItemStack(ModItems.CAPSTONE.get())) && BossGifts.pending(server, starter.getUUID()).isEmpty(),
                            "and the Capstone is not given back");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_server_stop", timeoutTicks = 200)
    public static void aServerStopRefundsLeavesNothingBehindAndReleasesTheChunks(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ApocalypseRitualTests.readyGround(f);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer wanda = f.player(10.5, FLOOR, 15.5, Power.SCARLET_WITCH);
        ApocalypseTestSupport.tank(wanda);
        ServerPlayer starter = ApocalypseRitualTests.starter(f, 10.5, 5.5);
        helper.runAfterDelay(3, () -> {
            ApocalypseRitualTests.plant(starter, helper.absolutePos(ApocalypseRitualTests.CLICK));
            ApocalypseFight fight = (ApocalypseFight) BossFights.active(level);
            check(fight != null, "the ritual began");
            ChunkPos middle = new ChunkPos(BlockPos.containing(fight.arena().center()));
            check(level.getChunkSource().chunkMap.getDistanceManager().shouldForceTicks(middle.toLong()), "the arena's chunks are held and ticking");
            fight.forceState(ApocalypseFight.Stage.TRANSITION_1, 1);
            ApocalypseEntity boss = fight.boss();
            Vec3 at = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));
            helper.runAfterDelay(65, () -> {
                check(!fight.horsemenAlive().isEmpty(), "the Horsemen are out");
                SandLanceEntity lance = new SandLanceEntity(com.kolodkingal.enhanced.registry.ModEntities.SAND_LANCE.get(), level);
                lance.setPos(at.add(3.0, 1.0, 0.0));
                fight.spawn(lance);
                SandCocoonEntity cocoon = SandControl.entomb(level, boss, wanda, 100, 1.5F, 20.0F, fight);
                check(cocoon != null, "someone is in a cocoon");
                SandHazards.tornado(level, boss, fight.id(), at.add(4.0, 0.0, 0.0), 3.2, 160, 2.0F);
                BossFights.stopAll(server, StopReason.SERVER_STOPPING);
                check(fight.isFinished(), "the fight is finished at once");
                check(level.getEntitiesOfClass(ApocalypseEntity.class, new AABB(BlockPos.containing(at)).inflate(60.0)).isEmpty(), "he is gone");
                check(level.getEntitiesOfClass(Husk.class, new AABB(BlockPos.containing(at)).inflate(60.0)).stream().noneMatch(h -> Horsemen.kindOf(h) != null),
                        "so are the Horsemen");
                check(level.getEntitiesOfClass(SandCocoonEntity.class, new AABB(BlockPos.containing(at)).inflate(60.0)).isEmpty(), "the cocoon");
                check(lance.isRemoved() && SandHazards.count() == 0, "the lance and the tornado");
                check(!wanda.hasEffect(com.kolodkingal.enhanced.registry.ModEffects.ENTOMBED), "and whoever was held is free");
                check(!level.getChunkSource().chunkMap.getDistanceManager().shouldForceTicks(middle.toLong()), "the chunks are let go");
                check(BossGifts.pending(server, starter.getUUID()).stream().anyMatch(s -> s.is(ModItems.CAPSTONE.get())), "the Capstone waits as a gift");
                helper.succeed();
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The arena

    @GameTest(template = ARENA, batch = "apocalypse_arena", timeoutTicks = 200)
    public static void theArenaIsProtectedAndClosed(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        f.config(ApocalypseBossConfig.ARENA_RADIUS, 5);
        f.config(ApocalypseBossConfig.ARENA_HEIGHT, 16);
        ServerLevel level = f.level();
        ServerPlayer hero = f.player(10.5, FLOOR, 10.5);
        ApocalypseTestSupport.tank(hero);
        ApocalypseFight fight = ApocalypseTestSupport.inPhase(f, hero, 1);
        BossArena arena = fight.arena();
        check(arena.containment() == BossArena.Containment.WALL, "a wall");
        assertNear(arena.containRadius(), 9.0, 1.0E-6, "at the radius plus four");
        assertNear(arena.radius(), 13.0, 1.0E-6, "inside a protected cylinder of the radius plus eight");
        assertNear(arena.top() - arena.center().y, 16.0, 1.0E-6, "as tall as arenaHeight");
        check(arena.protects(helper.absolutePos(new BlockPos(10, 1, 10))) && !arena.protects(helper.absolutePos(new BlockPos(10, 1, 10)).offset(20, 0, 0)),
                "its blocks are protected and what is outside it is not");
        // No block placed or broken by a player, no explosion that breaks anything.
        BlockPos stone = new BlockPos(14, 2, 10);
        helper.setBlock(stone, Blocks.STONE);
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, helper.absolutePos(stone), level.getBlockState(helper.absolutePos(stone)), hero);
        NeoForge.EVENT_BUS.post(event);
        check(event.isCanceled(), "a block cannot be broken");
        Vec3 blast = helper.absoluteVec(new Vec3(14.5, FLOOR + 0.5, 10.5));
        level.explode(null, blast.x, blast.y, blast.z, 4.0F, Level.ExplosionInteraction.TNT);
        helper.assertBlockPresent(Blocks.STONE, stone);
        // No teleport takes a fighter out of the cylinder.
        check(!BossFights.allowsTeleport(level, hero, helper.absoluteVec(new Vec3(40.5, FLOOR, 10.5))), "a pearl, a Bifrost or a portal cannot take a fighter out");
        check(BossFights.allowsTeleport(level, hero, helper.absoluteVec(new Vec3(12.5, FLOOR, 10.5))), "but within the arena they work");
        // The wall pushes back, once in a while, for a damage that cannot kill.
        put(f, hero, 19.9, FLOOR, 10.5);
        hero.setHealth(1.0F);
        helper.runAfterDelay(8, () -> {
            check(arena.distance(hero.position()) < 9.0, "the wall pushed the fighter back in: " + arena.distance(hero.position()));
            check(hero.getHealth() >= 1.0F && hero.isAlive(), "without killing them");
            // Nobody flies over it.
            put(f, hero, 10.5, FLOOR + 30.0, 10.5);
            helper.runAfterDelay(30, () -> {
                check(hero.getY() < helper.absoluteVec(new Vec3(0, FLOOR + 16.5, 0)).y, "nor over it: " + hero.getY());
                // Nor under: a tractor lifts whoever is nine blocks down.
                put(f, hero, 10.5, FLOOR - 10.0, 10.5);
                helper.runAfterDelay(30, () -> {
                    check(hero.getY() > helper.absoluteVec(new Vec3(0, FLOOR - 1.0, 0)).y, "a fighter nine blocks down is lifted back: " + hero.getY());
                    helper.succeed();
                });
            });
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The bars

    @GameTest(template = ARENA, batch = "apocalypse_bars", timeoutTicks = 320)
    public static void twoBossBarsShowHealthAndShield(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        noCrits(d.f()); // the bars, the blows counted as in 3.0 (8a.2)
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        ServerPlayer bystander = d.f().player(0.5, FLOOR, 0.5, GameType.SPECTATOR);
        d.f().config(ApocalypseBossConfig.WINDOW_TICKS, 40);
        d.f().config(ApocalypseBossConfig.STAGGER_TICKS, 5);
        helper.runAfterDelay(25, () -> {
            var health = fight.bars().health();
            var shield = fight.bars().second();
            check(key(health.getName()).equals("boss.enhanced.apocalypse.phase1") && health.getColor() == BossEvent.BossBarColor.BLUE, "the first bar: his name, blue");
            check(health.getProgress() == 1.0F && health.isVisible(), "full");
            check(key(shield.getName()).equals("boss.enhanced.apocalypse.shield") && shield.getColor() == BossEvent.BossBarColor.YELLOW, "the second: his shield, yellow");
            check(shield.getProgress() == 1.0F, "full");
            check(health.getPlayers().contains(d.hero()) && shield.getPlayers().contains(d.hero()), "both shown to the fighter");
            check(health.getPlayers().contains(bystander), "and to whoever is within 64 blocks");
            // Wear it to half.
            d.hit(ModDamageTypes.CHAOS_BEAM, boss.shield().strength() / 2.0F);
            helper.runAfterDelay(2, () -> {
                assertNear(shield.getProgress(), 0.5, 0.02, "the shield bar follows the strength: " + shield.getProgress());
                boss.setFraction(0.9);
                helper.runAfterDelay(12, () -> {
                    assertNear(health.getProgress(), 0.9, 0.01, "the health bar follows his health");
                    breakShield(d);
                    helper.runAfterDelay(2, () -> {
                        check(key(shield.getName()).equals("boss.enhanced.apocalypse.shield.broken"), "a broken shield shows the seconds of the window: " + key(shield.getName()) + " (state " + boss.shield().state() + ", over " + fight.isOver() + ", stage " + fight.stage() + ")");
                        check(shield.getProgress() > 0.9F, "which runs down from the full bar: " + shield.getProgress());
                        helper.runAfterDelay(45, () -> {
                            check(key(shield.getName()).equals("boss.enhanced.apocalypse.shield.forming"), "unused, it knits again: " + key(shield.getName()));
                            helper.succeed();
                        });
                    });
                });
            });
        });
    }

    private static String key(net.minecraft.network.chat.Component name) {
        return name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : name.getString();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The heroes against him

    @GameTest(template = ARENA, batch = "apocalypse_adapt", timeoutTicks = 400)
    public static void heAdaptsToQuicksilverTime(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.QUICKSILVER);
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        d.f().config(ApocalypseBossConfig.ADAPTED_TICKS, 120);
        int adapt = ApocalypseBossConfig.ADAPT_TICKS.get();
        helper.runAfterDelay(3, () -> {
            int[] adaptedAt = {-1};
            every(helper, adapt + 200, tick -> {
                int factor = boss.slowFactor(50);
                if (adaptedAt[0] < 0) {
                    if (factor == 50) {
                        check(tick < adapt + 2, "he is slowed in full for " + adapt + " ticks (tick " + tick + ")");
                        return false;
                    }
                    adaptedAt[0] = tick;
                    check(factor == 0 && tick >= adapt && tick <= adapt + 2, "then he adapts: " + tick);
                    check(boss.hasAdapted(), "and knows it");
                    SandCocoonEntity cocoon = SandCocoonEntity.cocoonOf(d.hero());
                    check(cocoon != null && cocoon.ticksLeft() <= 40, "the answer is an Entomb on the speedster's feet: " + cocoon);
                    return false;
                }
                if (tick < adaptedAt[0] + 118) {
                    check(factor == 0, "and ignores it for 120 ticks (tick " + tick + ")");
                    return false;
                }
                if (tick > adaptedAt[0] + 124) {
                    check(factor == 50 && !boss.hasAdapted(), "and then it slows him again");
                    return true;
                }
                return false;
            });
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_strip", timeoutTicks = 260)
    public static void magnetosStripCutsHisArmourInAWindow(GameTestHelper helper) {
        Duel d = duel(helper, 1, Power.SCARLET_WITCH);
        ApocalypseFight fight = d.fight();
        ApocalypseEntity boss = d.boss();
        d.f().config(ApocalypseBossConfig.WINDOW_TICKS, 80);
        d.f().config(ApocalypseBossConfig.STAGGER_TICKS, 10);
        ServerPlayer magneto = d.f().player(5.5, FLOOR, 5.5, Power.MAGNETO);
        // Outside a window the strip takes nothing from him.
        check(boss.onMagnetoStrip(magneto), "the strip is answered");
        assertNear(boss.getAttributeValue(Attributes.ARMOR), 10.0, 1.0E-6, "while his shield is up it cuts nothing");
        breakShield(d);
        int left = boss.shield().windowLeft();
        check(boss.onMagnetoStrip(magneto), "in a window");
        assertNear(boss.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-6, "it cuts his armour to nothing");
        assertNear(boss.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 0.0, 1.0E-6, "and his toughness");
        check(boss.shield().windowLeft() == left + ApocalypseBossConfig.MAGNETO_STRIP_EXTEND_TICKS.get(), "and lengthens the window by 100 ticks");
        boss.onMagnetoStrip(magneto);
        check(boss.shield().windowLeft() == left + ApocalypseBossConfig.MAGNETO_STRIP_EXTEND_TICKS.get(), "once only");
        every(helper, 220, tick -> {
            if (tick == 100) {
                check(boss.shield().state() == ApocalypseShield.State.BROKEN, "the window is still open, a hundred ticks longer");
                assertNear(boss.getAttributeValue(Attributes.ARMOR), 0.0, 1.0E-6, "and his armour is still gone");
            }
            if (tick == 190) {
                check(boss.shield().state() != ApocalypseShield.State.BROKEN && !boss.isArmourCut(), "when it closes his armour is back");
                assertNear(boss.getAttributeValue(Attributes.ARMOR), 10.0, 1.0E-6, "all of it");
                return true;
            }
            return false;
        });
    }

    private ApocalypseFightTests() {
    }
}
