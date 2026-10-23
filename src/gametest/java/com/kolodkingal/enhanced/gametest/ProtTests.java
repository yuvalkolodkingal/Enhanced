package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.BillyBarrier;
import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.config.HexRunesConfig;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.hex.HexRuneLayout;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.DarkMagic;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.protect.Sorcery;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.ultron.SokoviaShape;
import com.kolodkingal.enhanced.ultron.SokoviaValidator;
import com.kolodkingal.enhanced.vishanti.Ward;
import com.kolodkingal.enhanced.vishanti.WardManager;
import com.kolodkingal.enhanced.vishanti.WardStore;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Lane PROT of 3.3: the walls that keep powers out. Wanda's Hex (only those she lets in come in; no power crosses its wall
 * from outside, tested by category: projectiles, beams and rays, blows and harm, explosions, effects, lightning, grips and
 * pulls, area attacks, block changes, teleports and arrivals, the Sokovia lift), the ward of the Book of the Vishanti
 * (monsters, bosses and what they launch, dark magic; the powers of players pass), Billy's Hex Barrier against beams, grips,
 * effects and chaos magic, and the Hex Runes. Every test has its own batch: a Hex or a ward affects everything round it.
 *
 * <p>The Hex is raised by Wanda in the middle of the arena (10.5, 2, 10.5) with radius 8 ({@link HexTests#raiseHex}): z 1.5
 * is outside it, z 5.5 inside.
 */
public final class ProtTests {
    private static Vec3 abs(GameTestHelper helper, double x, double y, double z) {
        return helper.absoluteVec(new Vec3(x, y, z));
    }

    private static Hex hexOf(TestKit.Fixture f, ServerPlayer wanda) {
        return HexManager.ownedBy(f.level(), wanda.getUUID()).orElseThrow();
    }

    // ============================================================================================================
    // The Hex: who comes in

    /** Only those she lets in: a survivor of the wall is still thrown out; letting creatures in lets no boss in. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_entry", timeoutTicks = 40)
    public static void onlyThoseSheAllowsComeIn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 0.0);
        ServerPlayer wanda = HexTests.raiseHex(f);
        ServerPlayer nobody = f.player(10.5, FLOOR, 2.0);
        Hex hex = hexOf(f, wanda);
        HexManager.handleAction(wanda, HexAction.SET_CREATURES, Optional.empty(), 1);
        hex = hexOf(f, wanda);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(3.0, FLOOR, 3.0));
        check(HexManager.judge(hex, zombie) == HexManager.Verdict.PASS, "creatures allowed: a zombie may come in");
        net.minecraft.world.entity.LivingEntity boss = ModEntities.THANOS.get().create(helper.getLevel());
        check(boss != null && HexManager.judge(hex, boss) != HexManager.Verdict.PASS, "but never a boss");
        f.everyTick(12, tick -> {
            if (tick == 2) {
                HexTests.walkTo(nobody, abs(helper, 10.5, FLOOR, 2.7));
                check(HexTests.inside(f, wanda, nobody), "he stepped through the wall");
            }
            if (tick >= 5) {
                check(nobody.isAlive(), "he survived the wall (death chance 0)");
                check(!HexTests.inside(f, wanda, nobody), "and is thrown back out all the same");
                return true;
            }
            return false;
        });
    }

    /** However fast a player comes (a jump from far outside the box round the Hex to its middle), the wall sees it. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_fast", timeoutTicks = 30)
    public static void aFastFlierIsStillJudged(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 6);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        AbilityHandler.onKey(wanda, 5, true);
        ServerPlayer thor = f.player(10.5, FLOOR + 30.0, 10.5, Power.THOR);
        f.everyTick(10, tick -> {
            if (tick == 3) {
                HexTests.walkTo(thor, abs(helper, 10.5, FLOOR, 12.0));
            }
            if (tick >= 6) {
                check(!HexTests.inside(f, wanda, thor), "Thor is thrown back out though he came from 30 blocks up in one tick");
                return true;
            }
            return false;
        });
    }

    /** A portal or a respawn into the Hex: whoever she did not let in is set down outside its wall. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_arrive")
    public static void arrivingInsideFromElsewhereIsJudged(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        ServerPlayer loki = f.player(10.5, FLOOR, 7.5, Power.LOKI);
        ServerPlayer friend = f.player(12.5, FLOOR, 10.5, Power.THOR);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(friend.getUUID()), 1);
        HexManager.arrive(helper.getLevel(), loki);
        HexManager.arrive(helper.getLevel(), friend);
        check(!HexTests.inside(f, wanda, loki), "Loki, not let in, is set down outside: " + loki.position());
        check(HexTests.inside(f, wanda, friend), "her friend stays");
        helper.succeed();
    }

    /** Ultron's island carries no one through a Hex: the lift is refused where a wall cuts its column. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_sokovia")
    public static void sokoviaDoesNotRiseThroughAHex(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        SokoviaShape.Spec spec = new SokoviaShape.Spec(6, 40, 3, 3, 4, 1L);
        BlockPos near = BlockPos.containing(abs(helper, 18.5, FLOOR, 10.5));
        BlockPos far = near.offset(200, 0, 0);
        check(SokoviaValidator.cutByHex(helper.getLevel(), spec, near), "the wall cuts a column beside the Hex");
        check(!SokoviaValidator.cutByHex(helper.getLevel(), spec, far), "and none far away");
        check(wanda.isAlive(), "(Wanda stands)");
        helper.succeed();
    }

    // ============================================================================================================
    // The Hex: what powers do from outside

    /** Category projectiles: a skeleton's arrow from outside stops at the wall; Wanda's own arrow goes on. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_projectile", timeoutTicks = 60)
    public static void projectilesStopAtTheWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        Skeleton skeleton = TestKit.spawnWithNoFreeWill(helper, EntityType.SKELETON, new Vec3(10.5, FLOOR, 1.0));
        skeleton.setNoAi(true);
        Pig pig = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(10.5, FLOOR, 6.0));
        Arrow arrow = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), null);
        Vec3 from = abs(helper, 10.5, FLOOR + 0.6, 1.5);
        arrow.setPos(from);
        arrow.setDeltaMovement(0, 0, 1.2);
        arrow.setNoGravity(true);
        helper.getLevel().addFreshEntity(arrow);
        Arrow hers = new Arrow(helper.getLevel(), wanda, new ItemStack(Items.ARROW), null);
        hers.setPos(abs(helper, 3.0, FLOOR + 0.6, 1.5));
        hers.setDeltaMovement(0, 0, 0.5);
        hers.setNoGravity(true);
        helper.getLevel().addFreshEntity(hers);
        f.everyTick(40, tick -> {
            if (tick < 30) {
                return false;
            }
            check(arrow.isRemoved(), "the skeleton's arrow is gone at the wall");
            assertNear(pig.getHealth(), pig.getMaxHealth(), 1.0E-3, "and the pig inside is not hurt");
            check(!hers.isRemoved() && hers.getZ() > abs(helper, 0, 0, 5.0).z, "Wanda's own arrow flies into her Hex: " + hers.position());
            return true;
        });
    }

    /** Category beams and rays: a beam's line ends at the wall; a player's aim does not reach in. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_beam")
    public static void beamsAndAimsStopAtTheWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        ServerPlayer thor = f.player(10.5, FLOOR, 1.0, Power.THOR);
        Pig pig = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(10.5, FLOOR, 6.0));
        TestKit.lookAt(thor, pig);
        WandaAbilities.ShieldStop stop = WandaAbilities.shieldAcross(helper.getLevel(), thor, thor.getEyePosition(), Shelters.middle(pig), 1.0F);
        check(stop != null && stop.wall() == Shelters.Wall.HEX, "Thor's beam ends at the wall: " + stop);
        check(Raycasts.entity(thor, 24.0, 0.3, e -> e == pig, 1.0F) == null, "his aim does not find the pig inside");
        Telekinesis.press(thor, Telekinesis.Mode.MAGNETO);
        check(!Telekinesis.isHeld(pig), "no grip takes it");
        check(WandaAbilities.shieldAcross(helper.getLevel(), wanda, wanda.getEyePosition().add(0, 0, -9.5), Shelters.middle(pig), 1.0F) == null,
                "her own beam from outside goes in");
        helper.succeed();
    }

    /** Category harm: a blow across the wall, and Thor's lightning beam's damage, do nothing inside. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_harm")
    public static void harmFromOutsideDoesNotReachIn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        ServerPlayer friend = f.player(10.5, FLOOR, 3.5);
        HexManager.handleAction(wanda, HexAction.SET_PLAYER, Optional.of(friend.getUUID()), 1);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 1.5));
        ServerPlayer thor = f.player(1.5, FLOOR, 1.5, Power.THOR);
        f.pvp(true);
        check(!friend.hurt(helper.getLevel().damageSources().mobAttack(zombie), 4.0F), "the zombie's blow across the wall does nothing");
        check(!friend.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.LIGHTNING_BEAM, thor, thor), 4.0F), "nor Thor's lightning");
        friend.invulnerableTime = 0;
        check(friend.hurt(helper.getLevel().damageSources().fall(), 1.0F), "harm from nowhere is not the wall's business");
        helper.succeed();
    }

    /** Category explosions: one outside breaks no block of the Hex and throws no one in it. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_explosion")
    public static void explosionsOutsideDoNothingInside(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        BlockPos inside = BlockPos.containing(10.5, FLOOR, 4.5);
        helper.setBlock(inside, Blocks.STONE);
        Pig pig = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(10.5, FLOOR, 4.0));
        Vec3 at = abs(helper, 10.5, FLOOR + 0.5, 1.5);
        helper.getLevel().explode(null, at.x, at.y, at.z, 4.0F, Level.ExplosionInteraction.TNT);
        helper.assertBlockPresent(Blocks.STONE, inside);
        assertNear(pig.getHealth(), pig.getMaxHealth(), 1.0E-3, "the pig inside is not hurt");
        check(pig.getDeltaMovement().lengthSqr() < 0.01, "nor thrown: " + pig.getDeltaMovement());
        check(wanda.isAlive(), "(Wanda stands)");
        helper.succeed();
    }

    /** Category effects: Billy's Freeze from outside does not take on a creature inside; Wanda's would. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_effect")
    public static void effectsFromOutsideDoNotTake(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        ServerPlayer billy = f.player(10.5, FLOOR, 1.0, Power.BILLY);
        Pig pig = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(10.5, FLOOR, 5.0));
        check(!pig.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100), billy), "Billy's spell does not cross the wall");
        check(pig.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100), wanda), "hers does");
        helper.succeed();
    }

    /** Category lightning: a bolt someone outside calls down inside the Hex does not come. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_lightning")
    public static void lightningCalledFromOutsideDoesNotStrikeInside(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HexTests.raiseHex(f);
        ServerPlayer thor = f.player(10.5, FLOOR, 1.0, Power.THOR);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        Vec3 in = abs(helper, 10.5, FLOOR, 6.0);
        bolt.moveTo(in);
        bolt.setCause(thor);
        check(!helper.getLevel().addFreshEntity(bolt), "the bolt is not let in");
        helper.succeed();
    }

    /** Category area attacks (bosses' slams, waves, the Phoenix Burst): nothing behind the wall may be hurt. */
    @EnhancedTest(template = ARENA, batch = "prot_hex_area")
    public static void areaAttacksSpareWhatIsInside(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HexTests.raiseHex(f);
        Zombie inside = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 5.0));
        Zombie outside = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(3.0, FLOOR, 1.0));
        ServerPlayer phoenix = f.player(10.5, FLOOR, 1.0, Power.PHOENIX);
        check(!AreaTargets.mayHurt(phoenix, inside, false), "the zombie inside is out of her reach");
        check(AreaTargets.mayHurt(phoenix, outside, false), "the one outside is not");
        helper.succeed();
    }

    /** Category blocks: no power from outside changes a block of the Hex (Build Magic, lifting, pulling...). */
    @EnhancedTest(template = ARENA, batch = "prot_hex_blocks")
    public static void blocksInsideAreNotChangedFromOutside(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        ServerPlayer billy = f.player(10.5, FLOOR, 1.0, Power.BILLY);
        ServerLevel level = helper.getLevel();
        BlockPos inside = BlockPos.containing(abs(helper, 10.5, FLOOR, 6.5));
        BlockPos outside = BlockPos.containing(abs(helper, 2.5, FLOOR, 1.5));
        check(BlockRights.massEditRefuses(billy, level, inside) && !BlockRights.mayBreak(billy, level, inside, level.getBlockState(inside)),
                "Billy's magic changes nothing inside from outside");
        check(!BlockRights.behindTheWall(billy, level, outside), "outside, the wall is no matter");
        check(!BlockRights.behindTheWall(wanda, level, inside), "and Wanda builds in her own Hex");
        helper.succeed();
    }

    // ============================================================================================================
    // The ward of the Book of the Vishanti

    private static UUID ward(TestKit.Fixture f, double radius) {
        UUID owner = UUID.randomUUID();
        WardStore.get(f.level()).put(new Ward(owner, "Base", abs(f.helper(), 10.5, FLOOR, 10.5), (int) radius, Power.LOKI.color));
        f.onClose(() -> WardManager.removeAll(f.level().getServer(), owner));
        return owner;
    }

    /** Monsters and bosses are kept out; a creature someone keeps (named) and a player's pet are not. */
    @EnhancedTest(template = ARENA, batch = "prot_ward_kinds")
    public static void theWardKeepsOutMonstersAndBosses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(WardManager.keepsOut(EntityType.ZOMBIE) && WardManager.keepsOut(ModEntities.THANOS.get()) && WardManager.keepsOut(EntityType.WITHER)
                && WardManager.keepsOut(ModEntities.ULTRON_SENTRY.get()), "monsters, bosses and minions");
        check(!WardManager.keepsOut(EntityType.PIG) && !WardManager.keepsOut(EntityType.VILLAGER), "nothing else");
        check(f.level() != null, "(fixture)");
        helper.succeed();
    }

    /** What a monster shoots stops at the edge; a player's arrow passes; harm from a monster outside does not reach in. */
    @EnhancedTest(template = ARENA, batch = "prot_ward_shots", timeoutTicks = 60)
    public static void theWardStopsWhatMonstersShoot(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ward(f, 6);
        ServerPlayer player = f.player(10.5, FLOOR, 1.0);
        ServerPlayer inside = f.player(10.5, FLOOR, 10.5);
        Skeleton skeleton = TestKit.spawnWithNoFreeWill(helper, EntityType.SKELETON, new Vec3(3.0, FLOOR, 1.0));
        skeleton.setNoAi(true);
        check(!inside.hurt(helper.getLevel().damageSources().mobAttack(skeleton), 3.0F), "a monster's harm from outside does not reach in");
        Arrow arrow = new Arrow(helper.getLevel(), skeleton, new ItemStack(Items.ARROW), null);
        arrow.setPos(abs(helper, 10.5, FLOOR + 0.6, 2.0));
        arrow.setDeltaMovement(0, 0, 1.0);
        arrow.setNoGravity(true);
        helper.getLevel().addFreshEntity(arrow);
        Arrow mine = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), null);
        mine.setPos(abs(helper, 8.5, FLOOR + 3.0, 2.0));
        mine.setDeltaMovement(0, 0, 0.5);
        mine.setNoGravity(true);
        helper.getLevel().addFreshEntity(mine);
        f.everyTick(30, tick -> {
            if (tick < 25) {
                return false;
            }
            check(arrow.isRemoved(), "the skeleton's arrow stopped at the edge");
            check(!mine.isRemoved() && mine.getZ() > abs(helper, 0, 0, 7.0).z, "the player's arrow went in: " + mine.position());
            return true;
        });
    }

    /** Dark magic (the Darkhold's purple beam) stops at the edge; chaos magic and Thor's lightning pass. */
    @EnhancedTest(template = ARENA, batch = "prot_ward_dark")
    public static void theWardStopsDarkMagicOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ward(f, 6);
        ServerPlayer caster = f.player(10.5, FLOOR, 1.0, Power.LOKI);
        ServerPlayer inside = f.player(10.5, FLOOR, 10.5);
        f.pvp(true);
        WandaAbilities.ShieldStop dark = WandaAbilities.shieldAcross(helper.getLevel(), caster, caster.getEyePosition(), Shelters.middle(inside), 1.0F, true);
        check(dark != null && dark.wall() == Shelters.Wall.WARD, "the purple beam ends at the ward: " + dark);
        check(WandaAbilities.shieldAcross(helper.getLevel(), caster, caster.getEyePosition(), Shelters.middle(inside), 1.0F, false) == null,
                "a player's other beams pass");
        check(DarkMagic.is(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM, caster, caster)), "its harm is dark magic");
        check(!inside.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM, caster, caster), 2.0F), "and stops at the edge");
        inside.invulnerableTime = 0;
        check(inside.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM, caster, caster), 1.0F), "chaos magic passes");
        helper.succeed();
    }

    // ============================================================================================================
    // Billy's Hex Barrier against powers

    private static ServerPlayer barrier(TestKit.Fixture f, Runnable then) {
        ServerPlayer billy = f.player(10.5, FLOOR, 8.5, Power.BILLY);
        TestKit.look(billy, 0.0F, 0.0F); // south, +z
        billy.setShiftKeyDown(true);
        AbilityHandler.onKey(billy, 4, true);
        billy.setShiftKeyDown(false);
        f.helper().runAfterDelay(EnhancedConfig.BILLY_BARRIER_WIND_UP.get() + 1, () -> {
            check(BillyBarrier.isUp(billy), "the barrier is up");
            then.run();
        });
        return billy;
    }

    /** A beam from the front ends at the plate (3.3.1: Thor's does not wear it); one from behind reaches him. */
    @EnhancedTest(template = ARENA, batch = "prot_barrier_beam", timeoutTicks = 40)
    public static void theBarrierStopsBeamsFromTheFront(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(10.5, FLOOR, 16.5, Power.THOR);
        ServerPlayer behind = f.player(10.5, FLOOR, 2.5, Power.THOR);
        f.pvp(true);
        ServerPlayer[] billy = new ServerPlayer[1];
        billy[0] = barrier(f, () -> {
            WandaAbilities.ShieldStop stop = WandaAbilities.shieldAcross(helper.getLevel(), thor, thor.getEyePosition(), Shelters.middle(billy[0]), 1.0F);
            check(stop != null && stop.wall() == Shelters.Wall.HEX_BARRIER, "Thor's beam ends at the plate: " + stop);
            WandaAbilities.pay(stop, thor, 5.0F);
            assertNear(Powers.get(billy[0]).shieldStrength(), 60.0, 1.0E-3, "and it is not worn by it (3.3.1)");
            check(WandaAbilities.shieldAcross(helper.getLevel(), behind, behind.getEyePosition(), Shelters.middle(billy[0]), 1.0F) == null,
                    "from behind nothing stops it");
            helper.succeed();
        });
    }

    /** Grips and curses from the front are stopped; a power of the Scarlet Witch wears it down (3.3.1), until it shatters. */
    @EnhancedTest(template = ARENA, batch = "prot_barrier_chaos", timeoutTicks = 40)
    public static void chaosMagicWearsTheBarrierDown(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(10.5, FLOOR, 14.5, Power.MAGNETO);
        ServerPlayer wanda = f.player(12.5, FLOOR, 14.5, Power.SCARLET_WITCH);
        ServerPlayer[] billy = new ServerPlayer[1];
        billy[0] = barrier(f, () -> {
            ServerPlayer b = billy[0];
            check(!b.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100), magneto), "a curse from the front does not take");
            check(Shelters.stops(magneto, magneto.getEyePosition(), b, 0.0F), "Magneto's pull is stopped");
            check(BillyBarrier.isUp(b), "and the barrier stands");
            check(Shelters.stops(magneto, magneto.getEyePosition(), b, 9.0F), "a pull that would cost 9 is stopped");
            assertNear(Powers.get(b).shieldStrength(), 60.0, 1.0E-3, "and does not wear it");
            check(Shelters.stops(wanda, wanda.getEyePosition(), b, 9.0F), "Wanda's grip is stopped too");
            check(BillyBarrier.isUp(b), "her chaos magic no longer shatters it at once");
            assertNear(Powers.get(b).shieldStrength(), 51.0, 1.0E-3, "it wears it down by its cost");
            check(Shelters.stops(wanda, wanda.getEyePosition(), b, 9.0F), "again at once");
            assertNear(Powers.get(b).shieldStrength(), 51.0, 1.0E-3, "a power costs at most twice a second");
            BillyBarrier.wear(b, Powers.get(b), 60.0F, wanda);
            check(!BillyBarrier.isUp(b), "enough chaos magic shatters it");
            long left = Powers.get(b).cooldownUntil(4) - helper.getLevel().getGameTime();
            check(left > 150, "and it cannot be raised for a while: " + left);
            helper.succeed();
        });
    }

    // ============================================================================================================
    // The Hex Runes

    /** Who is a sorcerer: Loki, Billy, a Scarlet Witch, and anyone holding a book of magic. */
    @EnhancedTest(template = ARENA, batch = "prot_runes_who")
    public static void whoIsASorcerer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer loki = f.player(2.5, FLOOR, 2.5, Power.LOKI);
        ServerPlayer thor = f.player(4.5, FLOOR, 2.5, Power.THOR);
        ServerPlayer nobody = f.player(6.5, FLOOR, 2.5);
        check(Sorcery.isSorcerer(loki) && !Sorcery.isSorcerer(thor) && !Sorcery.isSorcerer(nobody), "Loki is; Thor and a player are not");
        thor.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.DARKHOLD.get()));
        nobody.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOOK_OF_THE_VISHANTI.get()));
        check(Sorcery.isSorcerer(thor) && Sorcery.isSorcerer(nobody), "holding the Darkhold or the Book of the Vishanti, they are");
        helper.succeed();
    }

    /** The runes silence every other sorcerer inside: no key works, flight ends; Wanda keeps hers; wiping them frees them. */
    @EnhancedTest(template = ARENA, batch = "prot_runes", timeoutTicks = 40)
    public static void theRunesSilenceSorcerersInside(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        wanda.getFoodData().setFoodLevel(20);
        ServerPlayer billy = f.player(10.5, FLOOR, 7.5, Power.BILLY);
        ServerPlayer thor = f.player(12.5, FLOOR, 10.5, Power.THOR);
        ServerPlayer outside = f.player(10.5, FLOOR, 1.0, Power.LOKI);
        HexManager.handleAction(wanda, HexAction.SET_RUNES, Optional.empty(), 1);
        check(hexOf(f, wanda).runes(), "N's screen inscribes the runes");
        check(HexRunes.silences(billy), "Billy inside is held");
        check(!HexRunes.silences(wanda), "Wanda keeps her powers");
        check(!HexRunes.silences(thor), "Thor is no sorcerer");
        check(!HexRunes.silences(outside), "Loki outside is not held");
        PowerData data = Powers.get(billy);
        HexRunes.tick(billy);
        AbilityHandler.onKey(billy, 5, true);
        check(!data.toggle(Toggle.THE_NOISE), "his keys do nothing");
        PowerTicker.applyPassives(billy, data);
        check(!billy.mayFly(), "and he cannot fly");
        AbilityHandler.onKey(wanda, 4, true);
        check(Powers.get(wanda).chaosShieldActive(), "her own keys work");
        AbilityHandler.onKey(wanda, 4, false);
        HexManager.handleAction(wanda, HexAction.SET_RUNES, Optional.empty(), 0);
        check(!HexRunes.silences(billy), "wiped: he is free");
        PowerTicker.applyPassives(billy, data);
        check(billy.mayFly(), "and flies again");
        HexManager.handleAction(wanda, HexAction.SET_RUNES, Optional.empty(), 1);
        check(!hexOf(f, wanda).runes(), "inscribing again waits for the recharge (" + HexRunesConfig.COOLDOWN.get() + " ticks)");
        helper.succeed();
    }

    /** The books are shut too: a Darkhold holder inside cannot use it. */
    @EnhancedTest(template = ARENA, batch = "prot_runes_books")
    public static void theRunesShutTheBooks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        wanda.getFoodData().setFoodLevel(20);
        ServerPlayer loki = f.player(10.5, FLOOR, 7.5, Power.LOKI);
        loki.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DARKHOLD.get()));
        check(com.kolodkingal.enhanced.darkhold.DarkholdBook.mayUse(loki), "before the runes, he reads it");
        HexManager.handleAction(wanda, HexAction.SET_RUNES, Optional.empty(), 1);
        check(!com.kolodkingal.enhanced.darkhold.DarkholdBook.mayUse(loki), "with the runes up, the book is shut");
        helper.succeed();
    }

    /**
     * 3.7.1: the runes are spread over all four walls and grow with the Hex. Every wall has several, the film's two among
     * them, each on its own quarter of the dome between the ground and the roof, none crowding another (on its wall or the
     * next); a bigger Hex has bigger runes, up to a cap, and past it more of them. While the wall eases to a new size the runes
     * are already the new Hex's, at the size of the wall drawn now.
     */
    @EnhancedTest(template = ARENA, batch = "prot_runes_layout")
    public static void theRunesCoverTheWallsAndGrowWithTheHex(GameTestHelper helper) {
        float[] out = new float[HexRuneLayout.MAX_PER_WALL * HexRuneLayout.FIELDS];
        Vec3[] spots = new Vec3[4 * HexRuneLayout.MAX_PER_WALL];
        int lastCount = 0;
        double lastSize = 0.0;
        check(HexRuneLayout.count(4) >= 1, "the smallest Hex has a rune on each wall");
        for (int radius : new int[]{8, 16, 24, 32, 48, 64, 96, 128, 192, 256}) {
            int count = HexRuneLayout.count(radius);
            double size = HexRuneLayout.size(radius);
            check(count >= 2 && count <= HexRuneLayout.MAX_PER_WALL, "radius " + radius + ": runes on a wall " + count);
            check(count >= lastCount, "radius " + radius + ": a bigger Hex never has fewer runes (" + count + " < " + lastCount + ")");
            check(size >= lastSize, "radius " + radius + ": a bigger Hex never has smaller runes");
            lastCount = count;
            lastSize = size;
            int spotCount = 0;
            for (int side = 0; side < 4; side++) {
                int n = HexRuneLayout.place(radius, radius, side, out, 0);
                check(n == count, "radius " + radius + " wall " + side + ": " + n + " runes, not " + count);
                boolean branch = false;
                boolean gate = false;
                for (int k = 0; k < n; k++) {
                    int at = k * HexRuneLayout.FIELDS;
                    double azimuth = out[at + HexRuneLayout.AZIMUTH];
                    double elevation = out[at + HexRuneLayout.ELEVATION];
                    int glyph = (int) out[at + HexRuneLayout.GLYPH];
                    check(out[at + HexRuneLayout.SIZE] == (float) size, "every rune of the wall is the Hex's size");
                    check(Math.abs(azimuth) < HexRuneLayout.QUARTER * 0.5, "radius " + radius + ": a rune off its wall");
                    check(radius * Math.sin(elevation) - size * 0.5 >= HexRuneLayout.GROUND - 1.0E-3,
                            "radius " + radius + ": a rune's foot in the ground");
                    check(elevation <= HexRuneLayout.TOP, "radius " + radius + ": a rune up in the roof");
                    check(glyph >= 0 && glyph < HexRuneLayout.GLYPHS, "a rune that does not exist: " + glyph);
                    branch |= glyph == 0;
                    gate |= glyph == 1;
                    spots[spotCount++] = HexRuneLayout.direction(side, azimuth, elevation).scale(radius);
                }
                check(branch && gate, "radius " + radius + " wall " + side + ": the film's two runes are on it");
            }
            for (int a = 0; a < spotCount; a++) {
                for (int b = a + 1; b < spotCount; b++) {
                    check(spots[a].distanceTo(spots[b]) >= size * 1.2,
                            "radius " + radius + ": two runes crowd each other (" + spots[a].distanceTo(spots[b]) + ")");
                }
            }
        }
        check(HexRuneLayout.size(8) < HexRuneLayout.size(16) && HexRuneLayout.size(16) < HexRuneLayout.size(32)
                && HexRuneLayout.size(32) < HexRuneLayout.size(48), "the runes grow with the Hex");
        check(HexRuneLayout.size(96) == HexRuneLayout.MAX_SIZE && HexRuneLayout.size(256) == HexRuneLayout.MAX_SIZE,
                "but never into giants");
        check(HexRuneLayout.count(256) > HexRuneLayout.count(64) && HexRuneLayout.count(96) > HexRuneLayout.count(32),
                "a huge Hex has more runes instead");
        // Easing from 32 to 64: the 64's runes, at the size and height of the wall drawn now.
        int n = HexRuneLayout.place(48, 64, 2, out, 0);
        check(n == HexRuneLayout.count(64), "the runes of the Hex it is set to");
        assertNear(out[HexRuneLayout.SIZE], HexRuneLayout.size(48), 1.0E-4, "at the size of the wall drawn now");
        double low = 48 * Math.sin(out[HexRuneLayout.ELEVATION]);
        HexRuneLayout.place(64, 64, 2, out, 0);
        check(64 * Math.sin(out[HexRuneLayout.ELEVATION]) > low, "and they rise as the wall grows");
        helper.succeed();
    }

    /** Resizing the Hex keeps its runes: grown over a sorcerer standing outside, they hold him; shrunk off him, he is free. */
    @EnhancedTest(template = ARENA, batch = "prot_runes_grow", timeoutTicks = 40)
    public static void theRunesGrowWithTheHex(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        wanda.getFoodData().setFoodLevel(20);
        ServerPlayer loki = f.player(10.5, FLOOR, 1.0, Power.LOKI);
        HexManager.handleAction(wanda, HexAction.SET_RUNES, Optional.empty(), 1);
        check(hexOf(f, wanda).runes() && !HexRunes.silences(loki), "runes up; Loki outside is free");
        HexManager.handleAction(wanda, HexAction.SET_RADIUS, Optional.empty(), 12);
        check(hexOf(f, wanda).radius() == 12 && hexOf(f, wanda).runes(), "she grows the Hex: the runes stay inscribed");
        check(HexRunes.silences(loki), "and they hold Loki, inside the bigger Hex now");
        HexManager.handleAction(wanda, HexAction.SET_RADIUS, Optional.empty(), 8);
        check(hexOf(f, wanda).runes() && !HexRunes.silences(loki), "shrunk again: he is outside and free");
        helper.succeed();
    }

    private ProtTests() {
    }
}
