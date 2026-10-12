package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosDefence;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosFight;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.fighter;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.fixture;
import static com.kolodkingal.enhanced.gametest.ThanosMoveSupport.lost;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The fight as a whole: whom he goes after, what he is immune to, how the other heroes meet him, and how a fight ends (the
 * wipe, the loss of interest, the victory with its Gauntlets and its rest), then the Gauntlet that makes a Thanos.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ThanosFightTests {

    private static ThanosEntity boss(TestKit.Fixture f, ServerPlayer starter) {
        return ThanosBossTests.boss(f, starter);
    }

    // ============================================================================================================
    // Targeting

    /**
     * Never a Creative player or one in the Soul World; the nearest by default, the one who hurt him most when someone has,
     * and a Vision before anyone; he keeps a choice for sixty ticks of the game (the review found that he kept it for sixty
     * of his idle ticks, which are a moment between two moves: the first target was held for minutes).
     */
    @GameTest(template = ARENA, batch = "thanos_fight_target", timeoutTicks = 120)
    public static void targetingGoesToVisionThenTheBiggestHitterThenTheNearest(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer nearest = fighter(f, 0, 3);
        ServerPlayer hitter = fighter(f, 5, 5);
        ServerPlayer creative = f.player(ThanosBossTests.CENTRE.x, FLOOR, ThanosBossTests.CENTRE.z + 1.5, GameType.CREATIVE);
        ServerPlayer soul = fighter(f, 0, 2);
        soul.addEffect(new MobEffectInstance(ModEffects.SOULWORLD, 400, 0, false, false, true));
        ThanosEntity boss = boss(f, nearest);
        ServerPlayer vision = fighter(f, -7, 5, Power.VISION);
        Powers.get(vision).setVisionPhased(false);
        f.everyTick(100, t -> {
            if (t == 1) {
                // (Vision is not yet there for him: he joins the fight next tick.)
                return false;
            }
            if (t == 2) {
                check(boss.chooseTargetNow(true) == vision, "a Vision first");
                return false;
            }
            if (t == 3) {
                // The Vision stands aside; the rest are judged.
                Powers.setPower(vision, Power.NONE);
                check(boss.chooseTargetNow(true) == nearest, "the nearest, and never the Creative player or the one in the Soul World");
                ThanosBossTests.hit(boss, hitter, 100.0F);
                for (int i = 0; i < 60; i++) {
                    check(boss.chooseTargetNow(false) == nearest, "he holds the choice for a while: " + i);
                }
                return false;
            }
            if (t < 3 + ThanosEntity.TARGET_HOLD_TICKS) {
                // (Every tick of the game counts, not every call: he is idle only a moment between two moves.)
                check(boss.chooseTargetNow(false) == nearest, "he holds it for sixty ticks: " + t);
                return false;
            }
            check(boss.chooseTargetNow(false) == hitter, "then he goes for the one who hurt him most");
            return true;
        });
    }

    /** Until he has the Soul Stone he goes for the nearest of Loki's copies, which is the trick they are for. */
    @GameTest(template = ARENA, batch = "thanos_fight_lure", timeoutTicks = 60)
    public static void lokisCopiesLureHimUntilHeHasTheSoulStone(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer loki = fighter(f, 6, 6, Power.LOKI);
        ThanosEntity boss = boss(f, loki);
        com.kolodkingal.enhanced.ability.AbilityHandler.onKey(loki, 2, true);
        com.kolodkingal.enhanced.ability.AbilityHandler.onKey(loki, 2, false);
        f.everyTick(6, t -> {
            if (t < 2) {
                return false;
            }
            check(boss.chooseTargetNow(true) instanceof com.kolodkingal.enhanced.entity.LokiIllusionEntity, "a copy lures him");
            boss.forceStones(4);
            check(boss.chooseTargetNow(true) == loki, "with the Soul Stone he sees the real one");
            return true;
        });
    }

    // ============================================================================================================
    // Immunities and the other heroes

    /** What cannot touch him: the poisons and the weakeners, the stones' own effects, deep slowness, knockback, fire and falls. */
    @GameTest(template = ARENA, batch = "thanos_fight_immune", timeoutTicks = 40)
    public static void immunitiesRefuseTheEffectsAndTheKnocks(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a);
        for (var effect : java.util.List.of(MobEffects.WITHER, MobEffects.POISON, MobEffects.HUNGER, MobEffects.CONFUSION, MobEffects.LEVITATION,
                MobEffects.DIG_SLOWDOWN, MobEffects.WEAKNESS, ModEffects.PETRIFIED, ModEffects.SOULWORLD, ModEffects.TIME_LOCKED)) {
            check(!boss.addEffect(new MobEffectInstance(effect, 200)), "refuses " + effect.getRegisteredName());
            check(!boss.hasEffect(effect), "and has not got " + effect.getRegisteredName());
        }
        double base = boss.getAttributeValue(Attributes.MOVEMENT_SPEED);
        check(boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1)), "Slowness II takes");
        check(!boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2)), "but not Slowness III");
        check(boss.getAttributeValue(Attributes.MOVEMENT_SPEED) >= base * 0.5, "his speed never falls below half");
        check(boss.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0, "knockback is resisted wholly");
        boss.setDeltaMovement(Vec3.ZERO);
        boss.knockback(2.0, 1.0, 0.0);
        check(boss.getDeltaMovement().horizontalDistanceSqr() < 1.0E-6, "he is not moved by a knock");
        for (var source : java.util.List.of(level.damageSources().onFire(), level.damageSources().lava(), level.damageSources().fall(),
                level.damageSources().drown(), level.damageSources().inWall(), level.damageSources().wither())) {
            check(boss.isInvulnerableTo(source), "immune to " + source.type().msgId());
        }
        helper.succeed();
    }

    /** The Hex bounces him at its wall, in and out; Mjolnir will not pin him. */
    @GameTest(template = ARENA, batch = "thanos_fight_hex", timeoutTicks = 40)
    public static void theHexBouncesHimAndMjolnirWillNotPinHim(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        f.pvp(true);
        ServerPlayer wanda = fighter(f, 7, 7, Power.SCARLET_WITCH);
        ThanosEntity boss = boss(f, wanda);
        Hex hex = HexManager.create(wanda);
        check(HexManager.judge(hex, boss) == HexManager.Verdict.BLOCK, "he is turned back going in");
        check(HexManager.judge(hex, boss, true) == HexManager.Verdict.BLOCK, "and going out");
        ServerPlayer thor = fighter(f, -6, 0, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        MjolnirPin.pin(thor, InteractionHand.MAIN_HAND, boss);
        check(!MjolnirPin.isPinned(boss), "Mjolnir will not hold him down");
        check(thor.getMainHandItem().is(ModItems.MJOLNIR.get()), "and stays in Thor's hand");
        helper.succeed();
    }

    /** The hooks of the other heroes: Quicksilver's time, Magneto's strip, Spider-Man's web. */
    @GameTest(template = ARENA, batch = "thanos_fight_hooks", timeoutTicks = 120)
    public static void theHeroesHooksAnswerForHim(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(ThanosBossConfig.WEB_LOCK_TICKS, 20);
        f.config(ThanosBossConfig.WEB_LOCK_COOLDOWN, 100);
        ServerPlayer spider = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, spider);
        check(boss.slowFactor(5) == 2 && boss.slowFactor(1) == 1, "Quicksilver's time slows him to half at most");
        check(boss.onMagnetoStrip(spider), "Magneto's pull takes nothing from him");
        boss.onWebShot(spider);
        check(boss.gauntletLocked() && boss.flag(ThanosEntity.FLAG_GAUNTLET_LOCKED), "a web locks the Gauntlet");
        f.everyTick(60, t -> {
            if (t == 40) {
                check(!boss.gauntletLocked(), "the lock wore off");
                boss.onWebShot(spider);
                check(!boss.gauntletLocked(), "a second web within the cooldown does nothing");
                boss.forceStones(5);
                check(boss.slowFactor(5) == 0, "with the Time Stone nothing slows him");
                return true;
            }
            return false;
        });
    }

    /** A mob's blow does a quarter, and an Iron Golem that dares is swatted away. */
    @GameTest(template = ARENA, batch = "thanos_fight_mobs", timeoutTicks = 40)
    public static void mobsDoAQuarterAndAGolemIsSwatted(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a);
        IronGolem golem = EntityType.IRON_GOLEM.create(level);
        golem.moveTo(ThanosMoveSupport.spot(f, 2, FLOOR, 0));
        level.addFreshEntity(golem);
        float before = boss.getHealth();
        ThanosBossTests.hit(boss, a, 40.0F);
        float byPlayer = before - boss.getHealth();
        before = boss.getHealth();
        ThanosBossTests.hit(boss, level.damageSources().mobAttack(golem), 40.0F);
        float byGolem = before - boss.getHealth();
        check(byPlayer > 0.0F && Math.abs(byGolem - byPlayer * 0.25F) < byPlayer * 0.05F, "a quarter of a player's blow: " + byGolem + " against " + byPlayer);
        check(golem.isDeadOrDying() || golem.isRemoved(), "the golem was swatted away with the back of his hand");
        helper.succeed();
    }

    /** The stones' effects do not take on a boss, but they take on a zombie. */
    @GameTest(template = ARENA, batch = "thanos_fight_spellbound", timeoutTicks = 40)
    public static void spellboundImmuneCreaturesShakeOffTheStones(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a);
        net.minecraft.world.entity.boss.wither.WitherBoss wither = EntityType.WITHER.create(level);
        wither.setNoAi(true);
        wither.moveTo(ThanosMoveSupport.spot(f, -6, FLOOR, -6));
        level.addFreshEntity(wither);
        check(!com.kolodkingal.enhanced.thanos.StonePowers.petrify(level, boss, wither, 60) && !wither.hasEffect(ModEffects.PETRIFIED),
                "a Wither is not turned to stone");
        check(!wither.addEffect(new MobEffectInstance(ModEffects.TIME_LOCKED, 80)) && !wither.addEffect(new MobEffectInstance(ModEffects.SOULWORLD, 80)),
                "nor locked in time, nor taken to the Soul World");
        wither.discard();
        net.minecraft.world.entity.monster.Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.5, FLOOR, 3.5));
        check(com.kolodkingal.enhanced.thanos.StonePowers.petrify(level, boss, zombie, 60) && zombie.hasEffect(ModEffects.PETRIFIED), "a zombie is");
        helper.succeed();
    }

    /** A slam of Wanda's tears a plate away, but only once in six seconds. */
    @GameTest(template = ARENA, batch = "thanos_fight_slam", timeoutTicks = 40)
    public static void aTelekineticSlamTearsAPlateOnceInSixSeconds(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer wanda = fighter(f, 6, 6, Power.SCARLET_WITCH);
        ThanosEntity boss = boss(f, wanda);
        var slam = ModDamageTypes.source(f.level(), ModDamageTypes.TELEKINETIC_SLAM, wanda, wanda);
        ThanosBossTests.hit(boss, slam, 5.0F);
        check(Integer.bitCount(boss.plates()) == 3, "a plate is torn off: " + boss.plates());
        ThanosBossTests.hit(boss, slam, 5.0F);
        check(Integer.bitCount(boss.plates()) == 3, "not a second within six seconds");
        helper.succeed();
    }

    // ============================================================================================================
    // The entity's keeping

    /** He is never saved, never despawns, and one that does not belong to a fight is swept away. */
    @GameTest(template = ARENA, batch = "thanos_fight_keeping", timeoutTicks = 40)
    public static void aThanosIsNeverSavedAndAnOrphanIsSwept(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a);
        check(!boss.shouldBeSaved(), "he is not written to the world");
        check(!boss.removeWhenFarAway(1.0E6) && boss.isPersistenceRequired(), "and does not despawn");
        ThanosEntity stray = new ThanosEntity(ModEntities.THANOS.get(), level);
        stray.moveTo(ThanosMoveSupport.spot(f, -6, FLOOR, -6));
        boolean added = level.addFreshEntity(stray);
        check(!added && level.getEntity(stray.getUUID()) == null, "a Thanos that no fight made is turned away as it joins the level");
        helper.succeed();
    }

    /** The bar is in the colour of his newest stone, and at five stones it says the Mind Stone remains. */
    @GameTest(template = ARENA, batch = "thanos_fight_bar", timeoutTicks = 80)
    public static void theBarShowsTheNewestStoneAndSaysFive(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a);
        ThanosFight fight = boss.fight();
        boss.forceStones(3);
        f.everyTick(40, t -> {
            if (t < 3) {
                return false;
            }
            if (t == 3) {
                check(fight.bars().health().getColor() == StoneKind.nth(3).barColor(), "the colour of the Reality Stone");
                check(nameKey(fight).equals("boss.enhanced.thanos"), "the plain name");
                boss.forceStones(5);
                return false;
            }
            if (t == 6) {
                check(fight.bars().health().getColor() == StoneKind.nth(5).barColor(), "the colour of the Time Stone");
                check(nameKey(fight).equals("boss.enhanced.thanos.five"), "'only the Mind Stone remains': " + nameKey(fight));
                return true;
            }
            return false;
        });
    }

    /** How many orbs of its value an experience orb stands for (orbs that meet merge, and the count is private). */
    private static int orbCount(ExperienceOrb orb) {
        try {
            java.lang.reflect.Field count = ExperienceOrb.class.getDeclaredField("count");
            count.setAccessible(true);
            return count.getInt(orb);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static String nameKey(ThanosFight fight) {
        return fight.bars().health().getName().getContents() instanceof TranslatableContents name ? name.getKey() : "";
    }

    // ============================================================================================================
    // The end

    /** Every fighter dead for the wipe time: he leaves, nobody is rewarded and the rest is half. */
    @GameTest(template = ARENA, batch = "thanos_fight_wipe", timeoutTicks = 160)
    public static void aWipeLeavesAHalfRestAndNoReward(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.WIPE_SECONDS, 1);
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a);
        ThanosFight fight = boss.fight();
        a.hurt(f.level().damageSources().genericKill(), 100000.0F);
        check(!a.isAlive(), "the only fighter is dead");
        f.everyTick(120, t -> {
            if (!fight.isFinished()) {
                return false;
            }
            check(fight.result() == BossEnd.WIPE, "the fight ended as a wipe: " + fight.result());
            long rest = BossFights.restLeft(f.level().getServer(), BossKind.THANOS);
            long half = ThanosBossConfig.SUMMON_COOLDOWN_TICKS.get() / 2;
            check(rest > half - 100 && rest <= half, "half the rest: " + rest + " of " + half);
            check(boss.isRemoved(), "he is gone");
            check(!a.getInventory().contains(new ItemStack(ModItems.INFINITY_GAUNTLET.get())), "nobody was rewarded");
            return true;
        });
    }

    /** Nobody near him for the abandon time: he loses interest and goes, with half the rest and no reward. */
    @GameTest(template = ARENA, batch = "thanos_fight_abandon", timeoutTicks = 400)
    public static void leavingTheArenaMakesHimLoseInterest(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.ABANDON_SECONDS, 10);
        ServerPlayer a = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, a);
        ThanosFight fight = boss.fight();
        Vec3 far = ThanosMoveSupport.spot(f, 90, FLOOR, 0);
        f.level().getChunk(BlockPos.containing(far));
        a.teleportTo(f.level(), far.x, far.y, far.z, 0.0F, 0.0F);
        f.everyTick(360, t -> {
            if (!fight.isFinished()) {
                return false;
            }
            check(fight.result() == BossEnd.ABANDON, "he lost interest: " + fight.result());
            long rest = BossFights.restLeft(f.level().getServer(), BossKind.THANOS);
            long half = ThanosBossConfig.SUMMON_COOLDOWN_TICKS.get() / 2;
            check(rest > half - 400 && rest <= half, "with half the rest: " + rest);
            check(boss.isRemoved(), "he went through a ring of Space");
            return true;
        });
    }

    /** Walled in by blocks with the fighter out of reach, he frees himself to a spot beside him within a few seconds. */
    @GameTest(template = ARENA, batch = "thanos_fight_stuck", timeoutTicks = 900)
    public static void theStuckRuleFreesHim(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer target = fighter(f, 6, 6);
        ThanosEntity boss = boss(f, target);
        boss.setPassive(false);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy <= 6; dy++) {
                    boolean shell = Math.abs(dx) == 3 || Math.abs(dz) == 3 || dy == 6;
                    if (shell) {
                        f.helper().setBlock(new BlockPos(10 + dx, (int) FLOOR + dy, 10 + dz), Blocks.STONE);
                    }
                }
            }
        }
        Vec3 inside = boss.position();
        f.everyTick(850, t -> {
            if (boss.position().distanceTo(inside) < 4.0 || boss.state() == ThanosState.ARRIVING) {
                return false;
            }
            check(boss.position().distanceTo(target.position()) < 9.0, "he came out beside the one he hunts: " + boss.position().distanceTo(target.position()));
            return true;
        });
    }

    /**
     * Beaten, he rewards one Gauntlet to each fighter who did 3 percent or struck the last blow and not to the one who did
     * less, gives experience, kneels and turns to dust, and the rest begins.
     */
    @GameTest(template = ARENA, batch = "thanos_fight_defeat", timeoutTicks = 260)
    public static void defeatDeliversTheGauntletsAndStartsTheRest(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.REWARD_MIN_SECONDS, 0);
        ServerLevel level = f.level();
        ServerPlayer big = fighter(f, 6, 6);
        ServerPlayer killer = fighter(f, -6, 6);
        ServerPlayer small = fighter(f, 6, -6);
        ThanosEntity boss = boss(f, big);
        ThanosFight fight = boss.fight();
        f.everyTick(240, t -> {
            if (t == 1) {
                // (Everyone has joined the fight by now.)
                ThanosBossTests.hit(boss, small, 100.0F);
                ThanosBossTests.hit(boss, big, 100.0F);
                ThanosBossTests.hit(boss, big, 100.0F);
                check(boss.ledger().get(big.getUUID()) > boss.ledger().get(small.getUUID()), "the ledger counts the damage of each");
                boss.setHealth(5.0F);
                ThanosBossTests.hit(boss, killer, 100.0F);
                check(boss.state() == ThanosState.DYING && boss.getHealth() >= 1.0F, "he kneels instead of dying outright: " + boss.state());
                check(fight.isOver() && fight.result() == BossEnd.VICTORY, "the fight is won");
                ItemStack gauntlet = new ItemStack(ModItems.INFINITY_GAUNTLET.get());
                check(big.getInventory().countItem(gauntlet.getItem()) == 1, "the one who did most is rewarded");
                check(killer.getInventory().countItem(gauntlet.getItem()) == 1, "so is the one who struck the last blow");
                check(small.getInventory().countItem(gauntlet.getItem()) == 0, "and the one who did less than three percent is not");
                check(big.totalExperience >= BossConfig.XP_REWARD_PER_PLAYER.get(), "the rewarded are given experience");
                check(small.totalExperience == 0, "and the one who is not is not");
                long rest = BossFights.restLeft(level.getServer(), BossKind.THANOS);
                check(rest > ThanosBossConfig.SUMMON_COOLDOWN_TICKS.get() - 100, "the full rest begins: " + rest);
                float health = boss.getHealth();
                ThanosBossTests.hit(boss, big, 1000.0F);
                check(boss.getHealth() == health, "nothing hurts him while he turns to dust");
                return false;
            }
            if (!boss.isRemoved()) {
                return false;
            }
            int xp = 0;
            for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, new net.minecraft.world.phys.AABB(boss.position(), boss.position()).inflate(12.0))) {
                xp += orb.getValue() * orbCount(orb);
            }
            check(xp >= ThanosBossConfig.XP_REWARD.get(), "the experience was dropped where he stood: " + xp);
            check(t >= ThanosEntity.DYING_TICKS, "after the dying: " + t);
            return true;
        });
    }

    // ============================================================================================================
    // The Gauntlet

    /**
     * Held for the ritual with no power the Gauntlet makes Thanos and is used up; held by a hero it is refused with its
     * message and kept; in Creative it is kept.
     */
    @GameTest(template = ARENA, batch = "thanos_gauntlet", timeoutTicks = 260)
    public static void theGauntletMakesThanosOnlyOfThePowerless(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer plain = f.player(4.5, FLOOR, 4.5);
        ServerPlayer hero = f.player(8.5, FLOOR, 4.5, Power.THOR);
        ServerPlayer creative = f.player(12.5, FLOOR, 4.5, GameType.CREATIVE);
        for (ServerPlayer player : new ServerPlayer[]{plain, hero, creative}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.INFINITY_GAUNTLET.get()));
        }
        TestKit.sentPackets(hero);
        hero.getMainHandItem().getItem().use(level, hero, InteractionHand.MAIN_HAND);
        String shown = BossTestSupport.lastActionBarKey(hero);
        check("message.enhanced.thanos.already_powered".equals(shown) && !hero.isUsingItem(), "a hero is turned away: " + shown);
        check(hero.getMainHandItem().is(ModItems.INFINITY_GAUNTLET.get()), "and keeps it");
        plain.getMainHandItem().getItem().use(level, plain, InteractionHand.MAIN_HAND);
        creative.getMainHandItem().getItem().use(level, creative, InteractionHand.MAIN_HAND);
        check(plain.isUsingItem() && creative.isUsingItem(), "the others hold it up");
        int hold = ((InfinityGauntletItem) ModItems.INFINITY_GAUNTLET.get()).holdTicks();
        f.everyTick(hold + 10, t -> {
            plain.doTick();
            creative.doTick();
            if (t < hold - 2) {
                check(Powers.power(plain) == Power.NONE, "nothing happens before the hold is over: " + t);
            }
            if (t < hold + 2) {
                return false;
            }
            check(Powers.power(plain) == Power.THANOS, "the powerless becomes Thanos: " + Powers.power(plain));
            check(plain.getMainHandItem().isEmpty(), "and the Gauntlet is used up");
            check(Powers.power(creative) == Power.THANOS && creative.getMainHandItem().is(ModItems.INFINITY_GAUNTLET.get()), "in Creative it is kept");
            return true;
        });
    }

    /** No move takes the Mind Stone, and none is a Snap. */
    @GameTest(template = ARENA, batch = "thanos_fight_nosnap", timeoutTicks = 20)
    public static void noSixthStoneNoSnap(GameTestHelper helper) {
        Set<String> names = new HashSet<>();
        for (ThanosMove move : ThanosMove.values()) {
            check(move.stone() != StoneKind.MIND, move + " does not use the Mind Stone");
            check(!move.name().contains("SNAP") && !move.name().contains("DUST"), move + " is not a snap");
            names.add(move.name());
        }
        check(ThanosDefence.ALL_PLATES == 15, "four plates and no more");
        check(StoneKind.granted().size() == 5 && !StoneKind.granted().contains(StoneKind.MIND), "five stones are granted, and the sixth never");
        helper.succeed();
    }

    private ThanosFightTests() {
    }
}
