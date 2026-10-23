package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.CyclopsAbilities;
import com.kolodkingal.enhanced.ability.CyclopsGlasses;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.CyclopsConfig;
import com.kolodkingal.enhanced.item.RubyQuartzGlassesItem;
import com.kolodkingal.enhanced.network.BeamPayload;
import com.kolodkingal.enhanced.network.CyclopsFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Cyclops (3.3, requirement B2): the ruby-quartz glasses that make him, the Optic Blast on the shared beam, the giant beam
 * that pours out while the glasses are off (and its safety: never him, no hunger, no blocks by default, gone at death and
 * logout, never started by coming back), the Ricochet, the Concussion Blast, the Optic Leap, the Rally, his body and his
 * looks. Every test that keeps server-wide state (a beam, his record) has a batch of its own and {@link TestKit#bossFixture}.
 */
public final class CyclopsTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A Cyclops at full health, wearing his glasses, his record of them taken. */
    private static ServerPlayer hero(TestKit.Fixture f, double x, double z) {
        ServerPlayer hero = f.player(x, FLOOR, z, Power.CYCLOPS);
        hero.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.RUBY_QUARTZ_GLASSES.get()));
        PowerTicker.tick(hero);
        hero.setHealth(hero.getMaxHealth());
        f.onClose(() -> {
            ContinuousBeam.forget(hero);
            HeroLifecycle.onLogout(hero);
        });
        return hero;
    }

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
    }

    private static void release(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, false);
    }

    private static void sneakPress(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        AbilityHandler.onKey(player, ability, true);
        player.setShiftKeyDown(false);
    }

    /** A husk with no armour and 200 health that stands where it is put. */
    private static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    private static boolean hurt(LivingEntity e) {
        return e.getHealth() < e.getMaxHealth();
    }

    private static List<BeamPayload> beams(ServerPlayer player) {
        List<BeamPayload> found = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof BeamPayload beam) {
                found.add(beam);
            }
        }
        return found;
    }

    private static List<CyclopsFxPayload> fx(ServerPlayer player) {
        List<CyclopsFxPayload> found = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof CyclopsFxPayload payload) {
                found.add(payload);
            }
        }
        return found;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The glasses, the power, the body

    /** Putting the glasses on with no power makes Cyclops (30 health, armour 4, no flight); on another hero they are only glasses. */
    @EnhancedTest(template = ARENA, batch = "cyclops_awaken")
    public static void theGlassesMakeCyclops(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer nobody = f.player(4.5, FLOOR, 4.5);
        nobody.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.RUBY_QUARTZ_GLASSES.get()));
        PowerTicker.tick(nobody);
        f.onClose(() -> HeroLifecycle.onLogout(nobody));
        check(Powers.power(nobody) == Power.CYCLOPS, "the glasses make Cyclops, not " + Powers.power(nobody));
        check(Powers.variant(nobody).id().equals("apocalypse"), "in the film's look first");
        assertNear(nobody.getMaxHealth(), 20.0 + CyclopsConfig.BODY_HEALTH_BONUS.get(), 1.0E-4, "his health");
        assertNear(nobody.getAttributeValue(Attributes.ARMOR), CyclopsConfig.ARMOR.get(), 1.0E-4, "his armour");
        check(!nobody.mayFly() && !Power.CYCLOPS.canFly() && Power.CYCLOPS.flightAbility() == -1, "he does not fly");
        PowerTicker.tick(nobody);
        check(!Powers.get(nobody).is(Live.CYCLOPS_UNLEASHED), "wearing them, nothing pours out");

        ServerPlayer magneto = f.player(8.5, FLOOR, 4.5, Power.MAGNETO);
        magneto.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.RUBY_QUARTZ_GLASSES.get()));
        PowerTicker.tick(magneto);
        check(Powers.power(magneto) == Power.MAGNETO, "on another hero they are only glasses");

        Powers.setPower(nobody, Power.NONE);
        nobody.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        PowerTicker.tick(nobody);
        assertNear(nobody.getMaxHealth(), 20.0, 1.0E-4, "the body goes with the power");
        helper.succeed();
    }

    /** The item: a helmet, unbreakable, epic; his two looks and his own skin; his place after Apocalypse. */
    @EnhancedTest(template = ARENA, batch = "cyclops_pure")
    public static void theGlassesAndTheLooks(GameTestHelper helper) {
        ItemStack glasses = new ItemStack(ModItems.RUBY_QUARTZ_GLASSES.get());
        check(glasses.getItem() instanceof RubyQuartzGlassesItem item && item.getEquipmentSlot() == EquipmentSlot.HEAD, "worn on the head");
        check(!glasses.isDamageableItem() && glasses.getMaxStackSize() == 1, "it never breaks and does not stack");
        check(Power.CYCLOPS.ordinal() > Power.APOCALYPSE.ordinal(), "Cyclops comes after Apocalypse");
        check(Power.byName("cyclops") == Power.CYCLOPS, "found by its name");
        List<String> looks = Variants.of(Power.CYCLOPS).stream().map(v -> v.id()).toList();
        check(looks.equals(List.of("apocalypse", "classic", "own")), "his looks: " + looks);
        check(RubyQuartzGlassesItem.OPTIC.id().equals("cyclops_optic") && RubyQuartzGlassesItem.UNLEASHED.id().equals("cyclops_unleashed"),
                "his two beam styles");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Optic Blast

    /** R held with the glasses on: a beam from his eyes hurts and shoves what the crosshair is on; released, it stops. */
    @EnhancedTest(template = ARENA, batch = "cyclops_optic", timeoutTicks = 80)
    public static void theOpticBlastHurtsPushesAndStops(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        Husk target = husk(helper, 9.5, 10.5);
        Husk aside = husk(helper, 9.5, 15.5);
        lookAt(hero, target);
        TestKit.sentPackets(hero);
        press(hero, 1);
        PowerData data = Powers.get(hero);
        check(data.is(Live.BEAMING) && ContinuousBeam.isFiring(hero) && CyclopsAbilities.opticFiring(hero), "R held: the blast burns");
        List<BeamPayload> told = beams(hero);
        check(!told.isEmpty() && told.get(0).style().equals("cyclops_optic") && told.get(0).on(), "the clients are told, in his style: " + told);
        double[] push = {0.0};
        f.everyTick(40, tick -> {
            lookAt(hero, target);
            float before = target.getHealth();
            PowerTicker.tick(hero);
            if (target.getHealth() < before) {
                push[0] = Math.max(push[0], target.getDeltaMovement().x);
            }
            if (tick == 20) {
                check(hurt(target), "the target is hurt");
                check(target.getLastDamageSource() != null && target.getLastDamageSource().is(ModDamageTypes.OPTIC_BLAST)
                        && target.getLastDamageSource().getEntity() == hero, "by his optic blast, with the credit");
                check(push[0] > 0.2, "and shoved away: " + push[0]);
                check(!hurt(aside), "what the crosshair is not on is not hurt");
                release(hero, 1);
                check(!data.is(Live.BEAMING) && !ContinuousBeam.isFiring(hero), "released, it stops");
                check(beams(hero).stream().anyMatch(b -> !b.on()), "and the clients are told");
                target.setHealth(target.getMaxHealth());
            }
            if (tick > 20) {
                check(!hurt(target), "nothing after (tick " + tick + ")");
            }
            return tick == 40;
        });
    }

    /** No blast of his eyes without the glasses on, or on an empty stomach. */
    @EnhancedTest(template = ARENA, batch = "cyclops_optic_refused")
    public static void theOpticBlastNeedsTheGlassesAndFood(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        PowerData data = Powers.get(hero);
        hero.getFoodData().setFoodLevel(0);
        press(hero, 1);
        check(!data.is(Live.BEAMING), "starving: nothing");
        hero.getFoodData().setFoodLevel(20);
        // Without the glasses (his eyes shut: no glasses anywhere, N pressed) none of the aimed blasts works.
        hero.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        PowerTicker.tick(hero);
        press(hero, 5); // nothing to put on: he shuts his eyes
        check(!data.is(Live.CYCLOPS_UNLEASHED) && !ContinuousBeam.isFiring(hero), "with no glasses left, N shuts his eyes");
        press(hero, 1);
        check(!data.is(Live.BEAMING), "no optic blast without the glasses");
        check(CyclopsAbilities.concussion(hero, data) == 0 && data.isReady(CyclopsAbilities.CONCUSSION_SLOT, hero.level().getGameTime()),
                "no concussion blast either, and no cooldown");
        check(!CyclopsAbilities.leap(hero, data), "nor a leap");
        check(CyclopsAbilities.ricochet(hero, data).isEmpty() && data.isReady(CyclopsAbilities.RICOCHET_SLOT, hero.level().getGameTime()),
                "nor a ricochet");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The glasses off: the giant beam

    /**
     * Sneak + N takes the glasses off into his hand: the giant beam pours out, hurts everything along its line (the crosshair's
     * creature and the one behind it) and never him; N puts them back on and it stops. The cooldown of N says off and on.
     */
    @EnhancedTest(template = ARENA, batch = "cyclops_unleashed", timeoutTicks = 80)
    public static void takingTheGlassesOffUnleashesTheBeamUntilTheyAreBackOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        Husk first = husk(helper, 8.5, 10.5);
        Husk behind = husk(helper, 12.5, 10.5);
        lookAt(hero, first);
        PowerData data = Powers.get(hero);
        TestKit.sentPackets(hero);
        sneakPress(hero, 5);
        check(!CyclopsGlasses.wears(hero) && hero.getMainHandItem().is(ModItems.RUBY_QUARTZ_GLASSES.get()), "the glasses go to his hand");
        check(data.is(Live.CYCLOPS_UNLEASHED) && ContinuousBeam.isFiring(hero), "the giant beam pours out");
        check(beams(hero).stream().anyMatch(b -> b.on() && b.style().equals("cyclops_unleashed")), "drawn in its own style");
        long now = hero.level().getGameTime();
        assertNear(data.cooldownUntil(CyclopsAbilities.GLASSES_SLOT) - now, CyclopsGlasses.OFF_TICKS, 0.5, "N's signal: off");
        float health = hero.getHealth();
        float exhaustion = hero.getFoodData().getExhaustionLevel();
        f.everyTick(50, tick -> {
            lookAt(hero, first);
            PowerTicker.tick(hero);
            check(tick > 30 || data.is(Live.CYCLOPS_UNLEASHED), "it does not stop by itself (tick " + tick + ")");
            if (tick == 30) {
                check(hurt(first) && hurt(behind), "everything along the line is hurt: " + first.getHealth() + ", " + behind.getHealth());
                check(hero.getHealth() == health, "never him");
                assertNear(hero.getFoodData().getExhaustionLevel() - exhaustion, 0.0, 1.0E-4, "and it costs him no hunger");
                press(hero, 5);
                check(CyclopsGlasses.wears(hero) && hero.getMainHandItem().isEmpty(), "N puts them back on from his hand");
                check(!data.is(Live.CYCLOPS_UNLEASHED) && !ContinuousBeam.isFiring(hero), "and the beam stops");
                long t = hero.level().getGameTime();
                assertNear(data.cooldownUntil(CyclopsAbilities.GLASSES_SLOT) - t, CyclopsGlasses.ON_TICKS, 0.5, "N's signal: on");
                first.setHealth(first.getMaxHealth());
                behind.setHealth(behind.getMaxHealth());
                return false;
            }
            if (tick > 30) {
                check(!hurt(first) && !hurt(behind) && !data.is(Live.CYCLOPS_UNLEASHED), "nothing after (tick " + tick + ")");
            }
            return tick == 50;
        });
    }

    /** The glasses leaving the head slot any way at all lets it out; putting them back in the slot stops it. */
    @EnhancedTest(template = ARENA, batch = "cyclops_slot")
    public static void theHeadSlotIsTheRule(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        PowerData data = Powers.get(hero);
        hero.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        PowerTicker.tick(hero);
        check(data.is(Live.CYCLOPS_UNLEASHED), "dragged out of the slot: it pours out");
        hero.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.RUBY_QUARTZ_GLASSES.get()));
        PowerTicker.tick(hero);
        check(!data.is(Live.CYCLOPS_UNLEASHED) && !ContinuousBeam.isFiring(hero), "back in the slot: it stops");
        // Sneak + N with a full hand: they go to the inventory; N finds them there and puts back what was on his head.
        hero.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        sneakPress(hero, 5);
        check(hero.getInventory().contains(new ItemStack(ModItems.RUBY_QUARTZ_GLASSES.get())), "into the inventory");
        hero.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        PowerTicker.tick(hero);
        check(data.is(Live.CYCLOPS_UNLEASHED), "a hat is not ruby quartz");
        press(hero, 5);
        check(CyclopsGlasses.wears(hero) && hero.getInventory().contains(new ItemStack(Items.LEATHER_HELMET)), "the hat takes their place");
        check(!data.is(Live.CYCLOPS_UNLEASHED), "and the beam stops");
        helper.succeed();
    }

    /** It ends when he dies and when he leaves, and coming back without the glasses never starts it. */
    @EnhancedTest(template = ARENA, batch = "cyclops_safe")
    public static void itEndsAtDeathAndLogoutAndNeverRestartsByItself(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        PowerData data = Powers.get(hero);
        sneakPress(hero, 5);
        check(data.is(Live.CYCLOPS_UNLEASHED), "off: it pours out");
        HeroLifecycle.onDeath(hero);
        check(!data.is(Live.CYCLOPS_UNLEASHED) && !ContinuousBeam.isFiring(hero), "death ends it");
        HeroLifecycle.onRespawn(hero);
        for (int i = 0; i < 3; i++) {
            PowerTicker.tick(hero);
        }
        check(!data.is(Live.CYCLOPS_UNLEASHED) && !ContinuousBeam.isFiring(hero), "back without the glasses: nothing starts");
        press(hero, 5);
        check(CyclopsGlasses.wears(hero), "he puts them on");
        sneakPress(hero, 5);
        check(data.is(Live.CYCLOPS_UNLEASHED), "and off again: it pours out");
        HeroLifecycle.onLogout(hero);
        check(!data.is(Live.CYCLOPS_UNLEASHED) && !ContinuousBeam.isFiring(hero), "leaving ends it");
        HeroLifecycle.onLogin(hero);
        PowerTicker.tick(hero);
        check(!data.is(Live.CYCLOPS_UNLEASHED), "logging in without them starts nothing");
        // Losing the power ends it too.
        press(hero, 5);
        sneakPress(hero, 5);
        check(data.is(Live.CYCLOPS_UNLEASHED), "off once more");
        Powers.setPower(hero, Power.NONE);
        check(!data.is(Live.CYCLOPS_UNLEASHED) && !ContinuousBeam.isFiring(hero), "losing the power ends it");
        helper.succeed();
    }

    /** With PvP off the giant beam spares players; the crosshair's creature still takes it. */
    @EnhancedTest(template = ARENA, batch = "cyclops_pvp", timeoutTicks = 60)
    public static void theGiantBeamFollowsThePvpRule(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        f.pvp(false);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        ServerPlayer friend = f.player(7.5, FLOOR, 10.5);
        Husk husk = husk(helper, 11.5, 10.5);
        lookAt(hero, husk);
        sneakPress(hero, 5);
        f.everyTick(30, tick -> {
            lookAt(hero, husk);
            PowerTicker.tick(hero);
            if (tick == 30) {
                check(friend.getHealth() == friend.getMaxHealth(), "a player is spared with PvP off");
                check(hurt(husk), "the husk is not");
                return true;
            }
            return false;
        });
    }

    /** No block breaks by default; with the option, the struck wall breaks, but never round his own feet. */
    @EnhancedTest(template = ARENA, batch = "cyclops_blocks", timeoutTicks = 60)
    public static void theGiantBeamBreaksBlocksOnlyWhenAllowed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 3.5, 10.5);
        BlockPos wall = new BlockPos(9, (int) FLOOR + 1, 10);
        helper.setBlock(wall, Blocks.STONE);
        TestKit.look(hero, TestKit.EAST, 0.0F);
        sneakPress(hero, 5);
        f.everyTick(40, tick -> {
            TestKit.look(hero, TestKit.EAST, 0.0F);
            PowerTicker.tick(hero);
            if (tick == 15) {
                check(helper.getBlockState(wall).is(Blocks.STONE), "off by default: the wall stands");
                f.config(CyclopsConfig.UNLEASHED_BREAKS_BLOCKS, true);
            }
            if (tick == 40) {
                check(helper.getBlockState(wall).isAir(), "with the option the struck wall breaks");
                BlockPos feet = helper.absolutePos(new BlockPos(3, (int) FLOOR - 1, 10));
                check(CyclopsGlasses.breakAround(hero, helper.getLevel(), feet) == 0, "never the blocks round his own feet");
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The other abilities

    /** Sneak + R: the bolt strikes the wall ahead and comes back off it to hit the husk behind him; everyone near sees its path. */
    @EnhancedTest(template = ARENA, batch = "cyclops_ricochet")
    public static void theRicochetBouncesOffWalls(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 5.5, 10.5);
        Husk behind = husk(helper, 1.5, 10.5);
        for (int y = (int) FLOOR; y <= (int) FLOOR + 3; y++) {
            helper.setBlock(12, y, 10, Blocks.STONE);
        }
        TestKit.look(hero, TestKit.EAST, 0.0F);
        TestKit.sentPackets(hero);
        PowerData data = Powers.get(hero);
        List<LivingEntity> hit = CyclopsAbilities.ricochet(hero, data);
        check(hit.contains(behind) && hurt(behind), "the bolt came back off the wall and hit the husk behind him: " + hit);
        check(hero.getHealth() == hero.getMaxHealth(), "never him");
        long left = data.cooldownUntil(CyclopsAbilities.RICOCHET_SLOT) - hero.level().getGameTime();
        assertNear(left, CyclopsConfig.RICOCHET_COOLDOWN.get(), 0.5, "R's cooldown");
        List<CyclopsFxPayload> told = fx(hero);
        check(told.size() == 1 && told.get(0).kind() == CyclopsFxPayload.RICOCHET && told.get(0).points().size() >= 3,
                "the path is sent, a bounce at least: " + told);
        check(CyclopsAbilities.ricochet(hero, data).isEmpty(), "and not again before the cooldown");
        helper.succeed();
    }

    /** G: the cone ahead is hit and thrown back; what is behind or beside him is not. */
    @EnhancedTest(template = ARENA, batch = "cyclops_concussion")
    public static void theConcussionBlastHitsTheConeAhead(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 5.5, 10.5);
        Husk ahead = husk(helper, 9.5, 11.0);
        Husk behind = husk(helper, 2.5, 10.5);
        Husk far = husk(helper, 18.5, 10.5);
        TestKit.look(hero, TestKit.EAST, 0.0F);
        PowerData data = Powers.get(hero);
        int count = CyclopsAbilities.concussion(hero, data);
        check(count == 1 && hurt(ahead), "the one ahead is hit: " + count);
        check(ahead.getDeltaMovement().x > 0.3, "and thrown back: " + ahead.getDeltaMovement());
        check(!hurt(behind) && !hurt(far), "nothing behind him or beyond its reach");
        check(!data.isReady(CyclopsAbilities.CONCUSSION_SLOT, hero.level().getGameTime()), "G's cooldown runs");
        check(CyclopsAbilities.inCone(Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(3, 1, 0), 7, Math.cos(Math.toRadians(40))), "the cone's rule: in");
        check(!CyclopsAbilities.inCone(Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(1, 3, 0), 7, Math.cos(Math.toRadians(40))), "out");
        helper.succeed();
    }

    /** V: he is thrown up and forward, the husk at his feet is thrown out, and the fall of the leap does him no harm. */
    @EnhancedTest(template = ARENA, batch = "cyclops_leap")
    public static void theOpticLeapThrowsHimUpWithoutFallDamage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 5.5, 10.5);
        Husk near = husk(helper, 6.5, 10.5);
        TestKit.look(hero, TestKit.EAST, 0.0F);
        PowerData data = Powers.get(hero);
        check(CyclopsAbilities.leap(hero, data), "he leaps");
        check(hero.getDeltaMovement().y > 0.5 && hero.getDeltaMovement().x > 0.3, "up and forward: " + hero.getDeltaMovement());
        check(CyclopsAbilities.leaping(hero), "and is in the air");
        check(hurt(near), "the blast at the ground hurts the husk at his feet");
        float health = hero.getHealth();
        hero.causeFallDamage(12.0F, 1.0F, hero.damageSources().fall());
        check(hero.getHealth() == health, "no fall damage from the leap");
        check(!CyclopsAbilities.leap(hero, data), "not again before the cooldown");
        helper.succeed();
    }

    /** B: he and his allies near him are given Speed, Strength and Resistance; a husk is not. */
    @EnhancedTest(template = ARENA, batch = "cyclops_rally")
    public static void theRallyStrengthensHisSide(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hero = hero(f, 5.5, 10.5);
        ServerPlayer friend = f.player(9.5, FLOOR, 10.5);
        Husk husk = husk(helper, 7.5, 14.5);
        PowerData data = Powers.get(hero);
        int count = CyclopsAbilities.rally(hero, data);
        check(count == 2, "him and his friend: " + count);
        for (LivingEntity ally : List.of(hero, friend)) {
            check(ally.hasEffect(MobEffects.MOVEMENT_SPEED) && ally.hasEffect(MobEffects.DAMAGE_BOOST) && ally.hasEffect(MobEffects.DAMAGE_RESISTANCE),
                    ally.getName().getString() + " is rallied");
        }
        check(!husk.hasEffect(MobEffects.DAMAGE_BOOST), "a husk is not his side");
        check(CyclopsAbilities.rally(hero, data) == 0, "not again before the cooldown");
        // It is his voice, not his eyes: it works with the glasses off.
        data.startCooldown(CyclopsAbilities.RALLY_SLOT, hero.level().getGameTime(), 0);
        sneakPress(hero, 5);
        check(CyclopsAbilities.rally(hero, data) == 2, "with the glasses off too");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Data

    /** The effects payload goes there and back, and the loot that the Mutant School (and a rare chest) can name exists. */
    @EnhancedTest(template = ARENA, batch = "cyclops_pure")
    public static void thePayloadAndTheLoot(GameTestHelper helper) {
        CyclopsFxPayload sent = new CyclopsFxPayload(CyclopsFxPayload.RICOCHET, 42, List.of(new Vec3(1, 2, 3), new Vec3(-4.5, 5, 6.25)), 2);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CyclopsFxPayload.STREAM_CODEC.encode(buf, sent);
        CyclopsFxPayload back = CyclopsFxPayload.STREAM_CODEC.decode(buf);
        check(back.equals(sent), "the payload round-trips: " + back);
        MinecraftServer server = helper.getLevel().getServer();
        for (String path : List.of("loot_table/inject/ruby_quartz_glasses.json", "loot_modifiers/ruby_quartz_glasses_from_woodland_mansion.json",
                "loot_modifiers/ruby_quartz_glasses_from_stronghold_library.json")) {
            check(server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(Enhanced.MODID, path)).isPresent(), path + " exists");
        }
        helper.succeed();
    }
}
