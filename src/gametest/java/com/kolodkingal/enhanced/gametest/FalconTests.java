package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.client.custom.HeroAbilityAnims;
import com.kolodkingal.enhanced.client.render.FalconGestures;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.Gestures;
import com.kolodkingal.enhanced.client.render.RunCharacter;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.falcon.Dive;
import com.kolodkingal.enhanced.falcon.FalconDartEntity;
import com.kolodkingal.enhanced.falcon.FalconState;
import com.kolodkingal.enhanced.falcon.FalconWings;
import com.kolodkingal.enhanced.falcon.Redwing;
import com.kolodkingal.enhanced.falcon.RedwingEntity;
import com.kolodkingal.enhanced.ironman.IronMissileEntity;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Falcon (3.8, lane L5): flight only with the Exo-Wings on the chest (not broken); the wings an elytra only for Falcon;
 * his body; no fall damage with the wings; the Wrist Blasters' darts (4 each, a pair) and the Wrist Rocket (8, a boom
 * that breaks no block); the Dive (refused on the ground; from 10 up it hits a husk for 10, the splash, no fall
 * damage); the Boost Climb (24 ± 1 up); the Wing Shield (an arrow from the front bounces back his, a blow from the
 * front cut by 70 %, from behind not; a hover); the Wing Slash (7 to every husk within 3, not to one farther); Redwing
 * (it marks the monsters within 32 and zaps the nearest marked one for 2 every 20 ticks; N again takes it home; shot
 * down it needs 600 ticks of repairs); Redwing Strike (8 and Slowness II); without the wings every wing move and
 * Redwing refused, the wrist weapons not; and the pure tables of his animation. Batches: {@code falcon}, {@code
 * falcon_flight} (the sky opened over the arena: the dive and the climb).
 */
public final class FalconTests {
    private static final String BATCH = "falcon";
    private static final String BATCH_FLIGHT = "falcon_flight";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Falcon at full health, the Exo-Wings on his chest if {@code wings}, his body on (one tick). */
    static ServerPlayer falcon(TestKit.Fixture f, double x, double y, double z, boolean wings) {
        ServerPlayer sam = f.player(x, y, z, Power.FALCON);
        f.onClose(() -> HeroLifecycle.onLogout(sam));
        if (wings) {
            sam.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.FALCON_EXO_WINGS.get()));
        }
        sam.doTick();
        sam.setHealth(sam.getMaxHealth());
        return sam;
    }

    /** Ticks the players once a server tick until {@code done} (at most {@code max} ticks), then runs {@code then}. */
    static void until(GameTestHelper helper, int max, BooleanSupplier done, Runnable then, ServerPlayer... players) {
        untilStep(helper, 1, max, done, then, players);
    }

    private static void untilStep(GameTestHelper helper, int tick, int max, BooleanSupplier done, Runnable then,
                                  ServerPlayer... players) {
        helper.runAfterDelay(1, () -> {
            for (ServerPlayer p : players) {
                p.doTick();
            }
            if (done.getAsBoolean()) {
                then.run();
            } else if (tick >= max) {
                throw new GameTestAssertException("not done within " + max + " ticks");
            } else {
                untilStep(helper, tick + 1, max, done, then, players);
            }
        });
    }

    /** Ticks the players once a server tick for {@code n} ticks, then runs {@code then}. */
    static void step(GameTestHelper helper, int n, Runnable then, ServerPlayer... players) {
        int[] count = {0};
        until(helper, n + 1, () -> ++count[0] > n, then, players);
    }

    static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /** A husk with no armour and 200 health that stands where it is put (no blow moves it). */
    static Husk husk(GameTestHelper helper, double x, double y, double z) {
        Husk husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, y, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    static float lost(LivingEntity e) {
        return e.getMaxHealth() - e.getHealth();
    }

    static PowerData data(ServerPlayer player) {
        return Powers.get(player);
    }

    /** Whether a message with this key reached the player since the last look (the action bar or the chat). */
    static boolean told(ServerPlayer player, String key) {
        for (Object sent : TestKit.sentPackets(player)) {
            Component text = sent instanceof ClientboundSetActionBarTextPacket bar ? bar.text()
                    : sent instanceof ClientboundSystemChatPacket chat ? chat.content() : null;
            if (text != null && text.getContents() instanceof TranslatableContents t && key.equals(t.getKey())) {
                return true;
            }
        }
        return false;
    }

    /** The entities of a class inside this test's own arena (the arenas of a batch lie side by side). */
    static <T extends net.minecraft.world.entity.Entity> List<T> inArena(GameTestHelper helper, Class<T> type) {
        AABB box = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO)),
                Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(21, 12, 21))));
        return helper.getLevel().getEntitiesOfClass(type, box);
    }

    /** Takes the barrier ceiling off the arena (the sky is open), and puts it back when the test ends. */
    static void openTheSky(GameTestHelper helper, TestKit.Fixture f) {
        fill(helper, -1, 11, -1, 21, 11, 21, Blocks.AIR);
        f.onClose(() -> fill(helper, -1, 11, -1, 21, 11, 21, Blocks.BARRIER));
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    /** No armour at all on him (the wings' plates too), so a blow's number is the blow's. */
    static void bare(ServerPlayer player) {
        player.getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(Enhanced.MODID, "test_bare"), -100.0,
                AttributeModifier.Operation.ADD_VALUE));
        player.getAttribute(Attributes.ARMOR_TOUGHNESS).addPermanentModifier(new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(Enhanced.MODID, "test_bare"), -100.0,
                AttributeModifier.Operation.ADD_VALUE));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The wings, the flight and the body

    /** He flies only with the wings on his chest, and not with broken ones; G says so without them. */
    @EnhancedTest(template = ARENA, batch = BATCH_FLIGHT)
    public static void heFliesOnlyWithTheWingsOnHisChest(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 5.5, FLOOR, 5.5, false);
        check(!sam.mayFly(), "no wings: no flight");
        TestKit.sentPackets(sam);
        press(sam, 2, false);
        check(!sam.getAbilities().flying && told(sam, "message.enhanced.falcon.needs_wings"), "G: put on your wings");
        ItemStack wings = new ItemStack(ModItems.FALCON_EXO_WINGS.get());
        sam.getInventory().add(wings);
        sam.doTick();
        check(!sam.mayFly(), "wings in the bag do not fly");
        check(FalconWings.carries(sam), "but he carries them (the serum's rule)");
        sam.getInventory().clearContent();
        sam.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.FALCON_EXO_WINGS.get()));
        sam.doTick();
        check(sam.mayFly(), "the wings on his chest fly");
        press(sam, 2, false);
        check(sam.getAbilities().flying, "G: he flies");
        assertNear(sam.getAbilities().getFlyingSpeed(), 0.10, 1.0E-4, "at the wings' speed");
        ItemStack worn = sam.getItemBySlot(EquipmentSlot.CHEST);
        worn.setDamageValue(worn.getMaxDamage() - 1);
        sam.doTick();
        check(!sam.mayFly() && !sam.getAbilities().flying, "broken wings: down he comes");
        helper.succeed();
    }

    /** The wings are an elytra for Falcon, folded wings for a powerless wearer; the glide is FALCON_GLIDING. */
    @EnhancedTest(template = ARENA, batch = BATCH_FLIGHT)
    public static void theWingsAreAnElytraOnlyForFalcon(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 5.5, FLOOR + 6.0, 5.5, true);
        ServerPlayer other = f.player(12.5, FLOOR + 6.0, 12.5);
        f.onClose(() -> HeroLifecycle.onLogout(other));
        other.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.FALCON_EXO_WINGS.get()));
        sam.setOnGround(false);
        other.setOnGround(false);
        check(!other.tryToStartFallFlying(), "a powerless wearer cannot glide");
        check(sam.tryToStartFallFlying(), "Falcon glides");
        step(helper, 1, () -> {
            check(sam.isFallFlying() && data(sam).is(Live.FALCON_GLIDING), "gliding, and the state says so");
            sam.stopFallFlying();
            step(helper, 1, () -> {
                check(!data(sam).is(Live.FALCON_GLIDING), "and stops");
                helper.succeed();
            }, sam);
        }, sam);
    }

    /** His body: +4 health and +10 % speed; with the wings a fall and a wall do nothing; without them a fall hurts. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisBodyAndTheWingsSaveHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 5.5, FLOOR, 5.5, true);
        assertNear(sam.getMaxHealth(), 24.0, 1.0E-4, "24 health");
        assertNear(sam.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.1 * 1.1, 1.0E-4, "+10 % speed");
        sam.hurt(sam.damageSources().fall(), 8.0F);
        sam.invulnerableTime = 0;
        sam.hurt(sam.damageSources().flyIntoWall(), 6.0F);
        assertNear(sam.getHealth(), 24.0, 1.0E-4, "the wings take the fall and the wall");
        ServerPlayer bare = falcon(f, 12.5, FLOOR, 12.5, false);
        bare.hurt(bare.damageSources().fall(), 8.0F);
        check(bare.getHealth() < 24.0F, "without the wings a fall hurts");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: the Wrist Blasters and the Wrist Rocket

    /** A press fires a pair of darts, the right 3 ticks after the left, 4 each; a new pair waits its gap. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theDartsHitForFourEach(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 3.5, FLOOR, 10.5, true);
        Husk husk = husk(helper, 10.5, FLOOR, 10.5);
        lookAt(sam, husk);
        press(sam, 1, false);
        press(sam, 1, false);
        check(inArena(helper, FalconDartEntity.class).size() == 1,
                "one dart now, the second press within the gap is nothing");
        step(helper, 12, () -> {
            assertNear(lost(husk), 8.0, 0.01, "two darts of 4");
            check(inArena(helper, FalconDartEntity.class).isEmpty(), "the darts are gone");
            helper.succeed();
        }, sam);
    }

    /** Sneak + R: one rocket, 8 and a small boom that breaks no block and spares him; R waits 80. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRocketHitsForEightAndBreaksNoBlock(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 3.5, FLOOR, 10.5, true);
        Husk husk = husk(helper, 9.5, FLOOR, 10.5);
        helper.setBlock(new BlockPos(11, 2, 10), Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(10, 2, 11), Blocks.GLASS);
        lookAt(sam, husk);
        press(sam, 1, true);
        long now = helper.getLevel().getGameTime();
        check(!data(sam).isReady(1, now + 79) && data(sam).isReady(1, now + 80), "R waits 80");
        until(helper, 30, () -> lost(husk) > 0.0F && inArena(helper, IronMissileEntity.class).isEmpty(), () -> {
            check(lost(husk) >= 8.0F, "8 and the boom: " + lost(husk));
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(11, 2, 10));
            helper.assertBlockPresent(Blocks.GLASS, new BlockPos(10, 2, 11));
            assertNear(sam.getHealth(), sam.getMaxHealth(), 1.0E-4, "the boom spares him");
            helper.succeed();
        }, sam);
    }

    // ------------------------------------------------------------------------------------------------------------
    // V: the Dive and the Boost Climb

    /** On the ground the Dive is refused (and waits nothing); from 10 up it hits a husk for 10, the splash 3. */
    @EnhancedTest(template = ARENA, batch = BATCH_FLIGHT, timeoutTicks = 200)
    public static void theDiveGoesOnlyFromTheAirAndHitsForTen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        ServerPlayer sam = falcon(f, 3.5, FLOOR, 3.5, true);
        Husk prey = husk(helper, 12.5, FLOOR, 12.5);
        Husk near = husk(helper, 13.5, FLOOR, 13.8);
        Husk far = husk(helper, 17.5, FLOOR, 17.5);
        lookAt(sam, prey);
        TestKit.sentPackets(sam);
        press(sam, 3, false);
        check(!Dive.flying(sam) && !data(sam).is(Live.FALCON_DIVING), "no dive from the ground");
        check(told(sam, "message.enhanced.falcon.dive_from_air"), "dive from the air");
        check(data(sam).isReady(3, helper.getLevel().getGameTime()), "and no wait");
        Vec3 up = helper.absoluteVec(new Vec3(3.5, FLOOR + 10.0, 3.5));
        sam.teleportTo(up.x, up.y, up.z);
        sam.setOnGround(false);
        lookAt(sam, prey);
        press(sam, 3, false);
        check(Dive.flying(sam) && data(sam).is(Live.FALCON_DIVING), "the dive");
        until(helper, 60, () -> !Dive.flying(sam), () -> {
            assertNear(lost(prey), 10.0, 0.01, "10 to the prey");
            assertNear(lost(near), 3.0, 0.01, "3 to the one beside it");
            assertNear(lost(far), 0.0, 0.01, "nothing to the far one");
            check(!data(sam).is(Live.FALCON_DIVING), "the dive is over");
            step(helper, 40, () -> {
                assertNear(sam.getHealth(), sam.getMaxHealth(), 1.0E-4, "no harm to him, no fall damage");
                helper.succeed();
            }, sam);
        }, sam);
    }

    /** Sneak + V: the Boost Climb takes him 24 ± 1 up from the ground, and he hovers there. */
    @EnhancedTest(template = ARENA, batch = BATCH_FLIGHT, timeoutTicks = 200)
    public static void theBoostClimbRisesTwentyFour(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        openTheSky(helper, f);
        ServerPlayer sam = falcon(f, 10.5, FLOOR, 10.5, true);
        f.loadChunksAround(sam.blockPosition(), 1);
        double start = sam.getY();
        press(sam, 3, true);
        check(Dive.flying(sam), "the climb");
        until(helper, 40, () -> !Dive.flying(sam), () -> {
            assertNear(sam.getY() - start, 24.0, 1.0, "24 up");
            check(sam.getAbilities().flying, "and he hovers");
            helper.succeed();
        }, sam);
    }

    // ------------------------------------------------------------------------------------------------------------
    // B: the Wing Shield and the Wing Slash

    /** An arrow at the front of the Wing Shield bounces back, his now; he is not hurt. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theWingShieldBouncesAnArrow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 4.5, FLOOR, 10.5, true);
        look(sam, EAST, 0.0F);
        sam.setShiftKeyDown(false);
        AbilityHandler.onKey(sam, 4, true);
        check(data(sam).is(Live.FALCON_WING_SHIELD), "the shield is up");
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        Vec3 from = helper.absoluteVec(new Vec3(12.5, FLOOR + 1.3, 10.5));
        arrow.setPos(from);
        arrow.setDeltaMovement(-2.0, 0.0, 0.0);
        helper.getLevel().addFreshEntity(arrow);
        until(helper, 20, () -> arrow.getDeltaMovement().x > 0.0 || arrow.isRemoved(), () -> {
            check(!arrow.isRemoved() && arrow.getDeltaMovement().x > 1.0, "it flies back east");
            check(arrow.getOwner() == sam, "his now");
            assertNear(sam.getHealth(), sam.getMaxHealth(), 1.0E-4, "not hurt");
            AbilityHandler.onKey(sam, 4, false);
            check(!data(sam).is(Live.FALCON_WING_SHIELD), "let go: down");
            check(!data(sam).isReady(4, helper.getLevel().getGameTime() + 59), "and B waits 60");
            helper.succeed();
        }, sam);
    }

    /** Behind the Wing Shield a blow from the front loses 70 %; one from behind does not; it ends at 100 ticks. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theWingShieldCutsABlowFromTheFront(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 10.5, FLOOR, 10.5, true);
        bare(sam);
        look(sam, EAST, 0.0F);
        Husk front = husk(helper, 12.5, FLOOR, 10.5);
        Husk behind = husk(helper, 8.5, FLOOR, 10.5);
        AbilityHandler.onKey(sam, 4, true);
        sam.hurt(sam.damageSources().mobAttack(front), 10.0F);
        assertNear(24.0F - sam.getHealth(), 3.0, 0.01, "70 % off from the front");
        sam.setHealth(24.0F);
        sam.invulnerableTime = 0;
        sam.hurt(sam.damageSources().mobAttack(behind), 10.0F);
        assertNear(24.0F - sam.getHealth(), 10.0, 0.01, "all of it from behind");
        until(helper, 120, () -> !data(sam).is(Live.FALCON_WING_SHIELD), () -> {
            check(true, "it came down by itself");
            helper.succeed();
        }, sam);
    }

    /** Sneak + B: the Wing Slash, 7 to every husk within 3, nothing to one at 5. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theWingSlashHitsEveryoneRoundHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 10.5, FLOOR, 10.5, true);
        List<Husk> near = List.of(husk(helper, 12.5, FLOOR, 10.5), husk(helper, 8.5, FLOOR, 10.5),
                husk(helper, 10.5, FLOOR, 12.5));
        Husk far = husk(helper, 10.5, FLOOR, 15.5);
        press(sam, 4, true);
        for (Husk h : near) {
            assertNear(lost(h), 7.0, 0.01, "7 to each within 3");
        }
        assertNear(lost(far), 0.0, 0.01, "nothing at 5");
        check(!data(sam).isReady(4, helper.getLevel().getGameTime() + 79), "B waits 80");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // N: Redwing and Redwing Strike

    /** Redwing marks the monsters within 32 and zaps the nearest marked one for 2 every 20 ticks; N takes it home. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void redwingMarksAndZaps(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 3.5, FLOOR, 3.5, true);
        Husk nearer = husk(helper, 9.5, FLOOR, 3.5);
        Husk farther = husk(helper, 17.5, FLOOR, 17.5);
        press(sam, 5, false);
        check(data(sam).toggle(Toggle.REDWING_OUT), "Redwing is out");
        RedwingEntity drone = Redwing.drone(sam);
        check(drone != null && sam.getUUID().equals(drone.ownerId()), "his drone");
        step(helper, 45, () -> {
            Set<Integer> marks = Redwing.marks(sam);
            check(marks.contains(nearer.getId()) && marks.contains(farther.getId()), "both marked: " + marks);
            float zapped = lost(nearer) + lost(farther);
            assertNear(zapped, 4.0, 0.01, "two zaps of 2 in 45 ticks");
            check(lost(nearer) >= 2.0F, "the nearer one is zapped first");
            press(sam, 5, false);
            check(!data(sam).toggle(Toggle.REDWING_OUT) && drone.isRemoved(), "N: home into the pack");
            check(Redwing.marks(sam).isEmpty(), "the marks go with it");
            helper.succeed();
        }, sam);
    }

    /** Shot down, Redwing drops into the pack and needs 600 ticks of repairs; N is refused till then. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void redwingShotDownNeedsRepairs(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 3.5, FLOOR, 3.5, true);
        press(sam, 5, false);
        RedwingEntity drone = Redwing.drone(sam);
        check(drone != null, "out");
        assertNear(drone.getMaxHealth(), 12.0, 1.0E-4, "12 health");
        long now = helper.getLevel().getGameTime();
        drone.hurt(helper.getLevel().damageSources().generic(), 50.0F);
        check(drone.isRemoved(), "down, and gone into the pack");
        FalconState state = ModAttachments.FALCON.get(sam);
        check(!data(sam).toggle(Toggle.REDWING_OUT), "no longer out");
        check(state.redwingDownUntil() == now + 600, "repairs till " + (now + 600) + ": " + state.redwingDownUntil());
        TestKit.sentPackets(sam);
        press(sam, 5, false);
        check(Redwing.drone(sam) == null && !data(sam).toggle(Toggle.REDWING_OUT), "refused while repaired");
        check(told(sam, "message.enhanced.falcon.redwing_repairs"), "Redwing needs repairs");
        helper.succeed();
    }

    /** Sneak + N: Redwing (launched first) rams the husk he looks at, 8 and Slowness II; N waits 200. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void redwingStrikeRams(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 3.5, FLOOR, 10.5, true);
        Husk husk = husk(helper, 14.5, FLOOR, 10.5);
        lookAt(sam, husk);
        press(sam, 5, true);
        check(Redwing.drone(sam) != null && Redwing.drone(sam).striking(), "launched, and ramming");
        check(!data(sam).isReady(5, helper.getLevel().getGameTime() + 199), "N waits 200");
        until(helper, 80, () -> lost(husk) > 0.0F, () -> {
            assertNear(lost(husk), 8.0, 0.01, "8");
            MobEffectInstance slow = husk.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            check(slow != null && slow.getAmplifier() == 1, "Slowness II");
            helper.succeed();
        }, sam);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Without the wings

    /** Without the wings every wing move and Redwing say "Put on your Exo-Wings!"; the wrist weapons still fire. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void withoutTheWingsOnlyTheWristsWork(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer sam = falcon(f, 3.5, FLOOR, 10.5, false);
        Husk husk = husk(helper, 6.5, FLOOR, 10.5);
        lookAt(sam, husk);
        long now = helper.getLevel().getGameTime();
        for (int key = 2; key <= 5; key++) {
            for (boolean sneak : new boolean[]{false, true}) {
                TestKit.sentPackets(sam);
                press(sam, key, sneak);
                check(told(sam, "message.enhanced.falcon.needs_wings"), "key " + key + (sneak ? " with Sneak" : "")
                        + ": put on your wings");
            }
        }
        AbilityHandler.onKey(sam, 4, false);
        PowerData data = data(sam);
        check(!data.is(Live.FALCON_WING_SHIELD) && !data.is(Live.FALCON_DIVING) && !data.toggle(Toggle.REDWING_OUT)
                && !sam.getAbilities().flying && !Dive.flying(sam), "nothing happened");
        for (int key = 2; key <= 5; key++) {
            check(data.isReady(key, now), "key " + key + " waits nothing");
        }
        press(sam, 1, false);
        press(sam, 1, true);
        check(inArena(helper, FalconDartEntity.class).size() == 1, "a dart flies");
        check(inArena(helper, IronMissileEntity.class).size() == 1, "and a rocket");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // A custom hero and the preset

    /** A custom hero with his Wing Shield on R and the blasters on G needs no item (the rows bring the wings). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aCustomHeroHasHisWingsWithoutTheItem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        com.kolodkingal.enhanced.custom.CustomHero def = CustomKitSupport.def()
                .pick("falcon.wing_shield", 3).key(1, "falcon.wing_shield")
                .pick("falcon.blasters", 3).key(2, "falcon.blasters").pick("falcon.body", 3).build();
        ServerPlayer hero = CustomKitSupport.hero(f, 5.5, 10.5, def);
        f.onClose(() -> HeroLifecycle.onLogout(hero));
        CustomKitSupport.tickN(hero, 1);
        check(FalconWings.hasWings(hero, data(hero)) && !FalconWings.worn(hero), "wings without the item");
        Husk husk = husk(helper, 12.5, FLOOR, 10.5);
        lookAt(hero, husk);
        CustomKitSupport.press(hero, 1, false, true);
        check(data(hero).is(Live.FALCON_WING_SHIELD), "R held: the Wing Shield");
        CustomKitSupport.press(hero, 1, false, false);
        check(!data(hero).is(Live.FALCON_WING_SHIELD), "let go: down");
        CustomKitSupport.tap(hero, 2, false);
        check(inArena(helper, FalconDartEntity.class).size() == 1, "G: a dart");
        check(hero.getMaxHealth() > 20.0F, "his body");
        helper.succeed();
    }

    /** His preset reads, is his hero's, and fits 60 points with no note (the integrator lists it in HEROES). */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisPresetFitsSixtyPoints(GameTestHelper helper) throws java.io.IOException {
        java.nio.file.Path file = net.neoforged.fml.ModList.get().getModFileById(Enhanced.MODID).getFile()
                .findResource("assets", Enhanced.MODID, "hero_presets", "falcon.json");
        String json = java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
        com.kolodkingal.enhanced.custom.HeroPresets.Preset preset =
                com.kolodkingal.enhanced.custom.HeroPresets.read("falcon", json);
        check(preset != null && preset.like() == Power.FALCON, "the preset reads, like Falcon");
        com.kolodkingal.enhanced.custom.CustomHeroRules.Result r = com.kolodkingal.enhanced.custom.CustomHeroRules
                .sanitize(preset.hero(), com.kolodkingal.enhanced.custom.CustomHeroRules.Limits.DEFAULT.withBudget(
                        com.kolodkingal.enhanced.custom.HeroPresets.MAX_POINTS));
        check(r.notes().isEmpty(), "no note: " + r.notes());
        check(com.kolodkingal.enhanced.custom.CustomHeroRules.cost(r.hero())
                <= com.kolodkingal.enhanced.custom.HeroPresets.MAX_POINTS, "60 points at most");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // His animation (pure tables)

    /** His gestures have keys, his moves' effects start them, the stances hand over, his rows act his moves. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void hisAnimationTablesAreWhole(GameTestHelper helper) {
        for (GestureId id : GestureId.values()) {
            if (id.name().startsWith("FALCON_")) {
                check(Gestures.get(id) != null && Gestures.get(id).problems().isEmpty(), id + " has good keys");
                check(GestureRoutes.startedElsewhere(id), id + " is started by his effects");
            }
        }
        check(FalconGestures.gestureOf(FalconFxPayload.BLAST, 0) == GestureId.FALCON_BLAST_L
                && FalconGestures.gestureOf(FalconFxPayload.BLAST, 1) == GestureId.FALCON_BLAST_R, "the two wrists");
        check(FalconGestures.gestureOf(FalconFxPayload.DIVE_HIT, 0) == null, "a dive that hit nothing has no blow");
        check(GestureRoutes.toggle(Power.FALCON, Toggle.REDWING_OUT, true) == GestureId.FALCON_REDWING_LAUNCH,
                "Redwing's launch");
        Gestures.Def shield = Gestures.get(GestureId.FALCON_SHIELD);
        Gestures.ArmKey m = shield.m.get(shield.m.size() - 1);
        assertNear(m.x, FalconGestures.SHIELD_M[0], 0.5, "the shield's crossing ends on its stance");
        check(RunCharacter.of(Power.FALCON, false) == RunCharacter.FALCON, "his run");
        check(HeroAbilityAnims.covers(HeroAbility.FALCON_BLASTERS) && HeroAbilityAnims.covers(HeroAbility.FALCON_BODY),
                "his rows");
        for (Live live : new Live[]{Live.FALCON_DIVING, Live.FALCON_WING_SHIELD, Live.FALCON_GLIDING}) {
            String reason = GestureRoutes.reason(live);
            check(reason != null && !reason.startsWith("pending"), live + " is acted: " + reason);
        }
        helper.succeed();
    }
}
