package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.apocalypse.ApocalypseAttack;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.Horsemen;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Duel;
import com.kolodkingal.enhanced.gametest.BuildTestSupport.Seen;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.put;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.boxSpell;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.count;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The fixes of the review of lane ABW (and the Instant level it shared a finding with): an Instant undo whose chunks are
 * not loaded does not spin; the Instant time of one server tick is shared (instantTickMillis); tapping the Hex Beam's key
 * cannot hit faster than its interval; the unlimited Chaos Shield can be made limited against bosses; the Death Horseman
 * is not lost when the Four are cut to three. Every test has a batch of its own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class AbwReviewTests {
    private static final BoundingBox BIG = new BoundingBox(1, 2, 1, 19, 9, 19);

    /** An Instant undo in chunks that are not loaded goes at normal speed at once (it used to hold the tick until instantMaxMillis ran out). */
    @GameTest(template = ARENA, batch = "abw_review_instant_dark", timeoutTicks = 100)
    public static void instantUndoInUnloadedChunksDoesNotSpin(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.WANDA_INSTANT_MAX_MILLIS, 4000);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        BuildMagic.setInstant(wanda, true);
        cast(wanda, boxSpell(f, Spell.FILL, BIG));
        check(count(f, BIG, Blocks.STONE) == 2888 && working(wanda) == null, "the instant fill is there");
        // The chunks of the spell unload (what the job thinks is loaded is a test's predicate), then she undoes it.
        BuildScheduler.manual(wanda.getUUID(), (x, z) -> false);
        Seen.of(wanda);
        long began = System.nanoTime();
        BuildMagic.undo(wanda);
        long millis = (System.nanoTime() - began) / 1_000_000L;
        check(millis < 1500L, "the undo returned at once, not after the 4000 ms of instantMaxMillis: " + millis + " ms");
        BuildJob job = working(wanda);
        check(job != null && !job.instant() && !job.finished(), "it is queued, at normal speed, to load its chunks over the next ticks");
        check(Seen.of(wanda).said("message.enhanced.build.instant_slow"), "and she is told it was not instant");
        helper.succeed();
    }

    /** The Instant time of one server tick is shared: when the first Instant spell spends it, the second goes at normal speed without writing. */
    @GameTest(template = ARENA, batch = "abw_review_instant_tick", timeoutTicks = 1200)
    public static void instantSpellsShareTheTicksTime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(BuildConfig.WANDA_INSTANT_MAX_MILLIS, 5000);
        f.config(BuildConfig.WANDA_INSTANT_TICK_MILLIS, 100);
        f.config(BuildConfig.WANDA_INSTANT_MAX_BLOCKS, 100_000_000);
        ServerLevel level = f.level();
        ServerPlayer first = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        ServerPlayer second = builder(f, Power.SCARLET_WITCH, 2.5, 0.5, Blocks.STONE);
        BuildMagic.setInstant(first, true);
        BuildMagic.setInstant(second, true);
        BlockPos o = abs(f, 0, 0, 0);
        BoundingBox one = new BoundingBox(o.getX() + 24, 200, o.getZ() + 24, o.getX() + 83, 259, o.getZ() + 83);
        BoundingBox two = new BoundingBox(o.getX() + 124, 200, o.getZ() + 24, o.getX() + 183, 259, o.getZ() + 83);
        for (BoundingBox box : new BoundingBox[]{one, two}) {
            for (int cx = (box.minX() >> 4) - 1; cx <= (box.maxX() >> 4) + 1; cx++) {
                for (int cz = (box.minZ() >> 4) - 1; cz <= (box.maxZ() >> 4) + 1; cz++) {
                    level.getChunk(cx, cz);
                }
            }
        }
        Seen.of(first);
        Seen.of(second);
        // (both casts in this one server tick)
        BuildMagic.cast(first, recipe(Spell.FILL).box(one).at(new BlockPos(one.minX(), one.minY(), one.minZ()), Direction.UP).build(), 1,
                BuildCastPayload.HELD_SHORT | BuildCastPayload.HELD_LONG);
        BuildMagic.cast(second, recipe(Spell.FILL).box(two).at(new BlockPos(two.minX(), two.minY(), two.minZ()), Direction.UP).build(), 2,
                BuildCastPayload.HELD_SHORT | BuildCastPayload.HELD_LONG);
        BuildJob a = working(first);
        BuildJob b = working(second);
        check(a != null && !a.instant() && a.written() > 0, "the first spent the tick's Instant time writing some, and went on at normal speed: " + (a == null ? "-" : a.written()));
        check(b != null && !b.instant() && b.written() == 0, "the second found none left: normal speed, nothing written in this tick: " + (b == null ? "-" : b.written()));
        check(Seen.of(second).said("message.enhanced.build.instant_slow"), "and its Wanda is told");
        finish(first, second);
        BuildMagic.undo(first);
        BuildMagic.undo(second);
        finish(first, second);
        helper.succeed();
    }

    /** Press, release, press within two ticks hits once (and the interval still holds after the second press). */
    @GameTest(template = ARENA, batch = "abw_review_beam_tap", timeoutTicks = 60)
    public static void tappingTheBeamKeyCannotHitFasterThanItsInterval(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.BILLY_BEAM, true);
        ServerPlayer billy = f.player(3.5, FLOOR, 10.5, Power.BILLY);
        Pig target = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(10.5, FLOOR, 10.5));
        target.setNoAi(true);
        lookAt(billy, target);
        float perHit = EnhancedConfig.BILLY_BEAM_DAMAGE_PER_SECOND.get().floatValue() * EnhancedConfig.BILLY_BEAM_INTERVAL.get() / 20.0F;
        AbilityHandler.onKey(billy, 1, true);
        PowerTicker.tick(billy);
        AbilityHandler.onKey(billy, 1, false);
        AbilityHandler.onKey(billy, 1, true);
        PowerTicker.tick(billy);
        assertNear(target.getMaxHealth() - target.getHealth(), perHit, 1.0E-3, "press, release, press: one hit");
        helper.runAfterDelay(2, () -> {
            AbilityHandler.onKey(billy, 1, false);
            AbilityHandler.onKey(billy, 1, true);
            PowerTicker.tick(billy);
            assertNear(target.getMaxHealth() - target.getHealth(), perHit, 1.0E-3, "and another tap two ticks later still gives only that one");
            helper.runAfterDelay(4, () -> {
                PowerTicker.tick(billy);
                assertNear(target.getMaxHealth() - target.getHealth(), 2 * perHit, 1.0E-3, "the next hit comes when the interval is over");
                AbilityHandler.onKey(billy, 1, false);
                helper.succeed();
            });
        });
    }

    private static void raiseShield(Duel d, ServerPlayer wanda, Runnable then) {
        AbilityHandler.onKey(wanda, 4, true);
        d.f().helper().runAfterDelay(EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get() + 2, () -> {
            check(WandaAbilities.hasShieldUp(wanda), "the shield is up");
            then.run();
        });
    }

    /** The default: unlimited against bosses too. His Backhand does not wear the shield. */
    @GameTest(template = ARENA, batch = "abw_review_shield_boss_on", timeoutTicks = 100)
    public static void unlimitedShieldIsUnlimitedAgainstBossesByDefault(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, true);
        bossBlow(helper, d, 0.0);
    }

    /** With shieldUnlimitedVsBosses off, what a boss deals wears the shield by the old rules; an arrow still does not. */
    @GameTest(template = ARENA, batch = "abw_review_shield_boss_off", timeoutTicks = 100)
    public static void bossDamageWearsTheUnlimitedShieldWhenAsked(GameTestHelper helper) {
        Duel d = ApocalypseTestSupport.duel(helper, 1, Power.SCARLET_WITCH);
        d.f().config(EnhancedConfig.WANDA_SHIELD_UNLIMITED, true);
        d.f().config(EnhancedConfig.WANDA_SHIELD_UNLIMITED_VS_BOSSES, false);
        bossBlow(helper, d, 12.0);
    }

    private static void bossBlow(GameTestHelper helper, Duel d, double expectedWear) {
        ServerPlayer wanda = d.hero();
        ApocalypseEntity boss = d.boss();
        put(d.f(), wanda, 8.5, FLOOR, 10.5);
        lookAt(wanda, boss);
        raiseShield(d, wanda, () -> {
            PowerData data = Powers.get(wanda);
            float full = data.shieldStrength();
            float health = wanda.getHealth();
            // an arrow of an ordinary skeleton never wears an unlimited shield
            Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new Vec3(1.5, FLOOR, 1.5));
            skeleton.setNoAi(true);
            Arrow arrow = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), null);
            wanda.invulnerableTime = 0;
            wanda.hurt(helper.getLevel().damageSources().arrow(arrow, skeleton), 5.0F);
            assertNear(full - data.shieldStrength(), 0.0, 1.0E-4, "an arrow costs the unlimited shield nothing");
            check(boss.brain().force(boss, d.fight(), ApocalypseAttack.BACKHAND), "he swings");
            helper.runAfterDelay(ApocalypseAttack.BACKHAND.tellTicks() + 3, () -> {
                assertNear(health - wanda.getHealth(), 0.0, 1.0E-3, "his fist does not touch her");
                assertNear(full - data.shieldStrength(), expectedWear, 1.0E-3, "and it wore the shield " + expectedWear);
                check(data.chaosShieldActive() || expectedWear >= full, "it stands (or the blow broke it)");
                helper.succeed();
            });
        });
    }

    /** Death is not lost when powerFactor cuts the Four to three in a fight big enough for four. */
    @GameTest(template = ARENA, batch = "abw_review_horsemen_kinds")
    public static void deathTakesThePlaceOfTheThirdHorseman(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.config(ApocalypseBossConfig.POWER_FACTOR, 0.65);
        check(Horsemen.countFor(3) == 3, "three Horsemen at 0.65");
        check(Horsemen.kindAt(0, 3, 3) == Horsemen.Kind.WAR && Horsemen.kindAt(1, 3, 3) == Horsemen.Kind.FAMINE && Horsemen.kindAt(2, 3, 3) == Horsemen.Kind.DEATH,
                "War, Famine and Death in a fight of three");
        check(Horsemen.kindAt(1, 2, 1) == Horsemen.Kind.FAMINE, "a small fight is as before: War, Famine");
        f.config(ApocalypseBossConfig.POWER_FACTOR, 1.0);
        check(Horsemen.kindAt(2, 4, 3) == Horsemen.Kind.PESTILENCE && Horsemen.kindAt(3, 4, 3) == Horsemen.Kind.DEATH, "at 1.0 the four come in their old order");
        helper.succeed();
    }

    /** The key's name follows billy.beam: "Hex Beam" with the beam, "Hex Bolt" with the single shot. */
    @GameTest(template = ARENA, batch = "abw_review_billy_key_name")
    public static void theKeyNameFollowsTheBeamOption(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.BILLY_BEAM, true);
        check(Power.BILLY.abilityKey(1).equals("ability.enhanced.billy.1"), "the beam: " + Power.BILLY.abilityKey(1));
        f.config(EnhancedConfig.BILLY_BEAM, false);
        check(Power.BILLY.abilityKey(1).equals("ability.enhanced.billy.1.bolt"), "the bolt: " + Power.BILLY.abilityKey(1));
        check(Power.BILLY.abilityKey(4).equals("ability.enhanced.billy.4"), "the other keys are unchanged");
        helper.succeed();
    }
}
