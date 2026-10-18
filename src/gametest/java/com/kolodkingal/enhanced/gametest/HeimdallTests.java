package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.bifrost.BifrostKeys;
import com.kolodkingal.enhanced.bifrost.BifrostMenu;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.heimdall.GuardiansWatch;
import com.kolodkingal.enhanced.heimdall.HeimdallAbilities;
import com.kolodkingal.enhanced.heimdall.HeimdallServer;
import com.kolodkingal.enhanced.heimdall.HeimdallSight;
import com.kolodkingal.enhanced.heimdall.HeimdallTalk;
import com.kolodkingal.enhanced.heimdall.HofundBond;
import com.kolodkingal.enhanced.item.HofundItem;
import com.kolodkingal.enhanced.network.BifrostActionPayload;
import com.kolodkingal.enhanced.network.HeimdallSightPayload;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;

/**
 * Heimdall (3.5): becoming him (the Guardian's Watch, his own Hofund), Hofund for anyone and its bond, every key of his (the
 * combo and its reset, the lunge against a wall, the Sight and its payload, Far Sight, the Ward, the Call, the Strike, the
 * Bifrost for a friend), the Watch itself (seven shadows, only the watcher strikes them, the win and the loss), his custom
 * rows (R1 on another key, R3 the variant by the Sneak cell, the body by its row, the sword rule), the Bifrost screen's
 * menu and his dialogue nodes. Quick tests share the batch {@code heimdall}; the Watch (server-wide state, Asgard) has
 * {@code heimdall_watch}; the tests that hurt players {@code heimdall_pvp}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class HeimdallTests {
    private static final String BATCH = "heimdall";
    private static final String WATCH = "heimdall_watch";
    private static final String PVP = "heimdall_pvp";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    private static void press(ServerPlayer player, int ability, boolean sneak) {
        CustomKitSupport.tap(player, ability, sneak);
    }

    /** A husk that stands still with plenty of health (the blows are counted on it). */
    private static Zombie husk(GameTestHelper helper, double x, double z) {
        Zombie husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        husk.setHealth(200.0F);
        return husk;
    }

    private static ServerPlayer heimdall(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = f.player(x, FLOOR, z, Power.HEIMDALL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HOFUND.get()));
        return player;
    }

    private static void ready(ServerPlayer player, int slot) {
        Powers.get(player).startCooldown(slot, player.level().getGameTime(), 0);
    }

    private static List<HeimdallSightPayload> sight(ServerPlayer player) {
        List<HeimdallSightPayload> found = new ArrayList<>();
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof HeimdallSightPayload p) {
                found.add(p);
            }
        }
        return found;
    }

    private static void won(ServerPlayer player) {
        player.setData(ModAttachments.ASGARD, player.getData(ModAttachments.ASGARD).with(AsgardProgress.WATCH_WON));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Becoming Heimdall, Hofund

    @GameTest(template = ARENA, batch = BATCH)
    public static void theWinnerOfTheWatchHoldingHisOwnHofundBecomesHeimdall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer winner = f.player(3.5, FLOOR, 3.5);
        won(winner);
        HofundBond.give(winner);
        check(HofundItem.isOwnHofund(winner, winner.getMainHandItem()), "his own Hofund is in his hand");
        PowerTicker.tick(winner);
        check(Powers.is(winner, Power.HEIMDALL), "he becomes Heimdall");

        ServerPlayer other = f.player(6.5, FLOOR, 3.5);
        won(other);
        other.setItemInHand(InteractionHand.MAIN_HAND, HofundBond.bind(winner).copy());
        PowerTicker.tick(other);
        check(Powers.get(other).power() == Power.NONE, "someone else's Hofund makes no Heimdall");

        ServerPlayer unproven = f.player(9.5, FLOOR, 3.5);
        HofundBond.give(unproven);
        PowerTicker.tick(unproven);
        check(Powers.get(unproven).power() == Power.NONE, "his own Hofund without the Watch makes no Heimdall");

        ServerPlayer thor = f.player(12.5, FLOOR, 3.5, Power.THOR);
        won(thor);
        HofundBond.give(thor);
        PowerTicker.tick(thor);
        check(Powers.is(thor, Power.THOR), "a hero keeps his power");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hofundOpensTheBifrostForAnyoneWhoHoldsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer anyone = f.player(10.5, FLOOR, 10.5);
        check(!BifrostKeys.holdsKey(anyone), "an empty hand is no key");
        anyone.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.HOFUND.get()));
        check(BifrostKeys.holdsKey(anyone), "an unbound Hofund in the off hand is a key");
        anyone.setItemInHand(InteractionHand.MAIN_HAND, anyone.getOffhandItem());
        anyone.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        look(anyone, 0.0F, 60.0F);
        anyone.setShiftKeyDown(true);
        anyone.getMainHandItem().use(anyone.serverLevel(), anyone, InteractionHand.MAIN_HAND);
        anyone.setShiftKeyDown(false);
        check(Powers.get(anyone).is(Live.BIFROST) && Bifrost.inBifrost(anyone), "Sneak + use: the Bifrost gathers to where he looks");
        PlaceBook.cancel(anyone);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aStaleHofundCrumblesAndANewOneComesBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer owner = f.player(10.5, FLOOR, 10.5);
        ItemStack first = HofundBond.give(owner);
        check(!HofundBond.stale(first, owner.getData(ModAttachments.ASGARD).hofundBond()), "the newest copy is not stale");
        ItemStack old = owner.getMainHandItem();
        HofundBond.bind(owner);
        check(HofundBond.stale(old, owner.getData(ModAttachments.ASGARD).hofundBond()), "a newer bond makes the old copy stale");
        owner.tickCount = 10;
        old.inventoryTick(owner.level(), owner, 0, true);
        check(old.isEmpty(), "the stale copy crumbles when it ticks");
        ItemStack free = new ItemStack(ModItems.HOFUND.get());
        free.inventoryTick(owner.level(), owner, 0, true);
        check(!free.isEmpty(), "an unbound Hofund never crumbles");

        // Heimdall with no Hofund of his own anywhere: R calls a new one into his hand.
        ServerPlayer heimdall = f.player(4.5, FLOOR, 4.5, Power.HEIMDALL);
        press(heimdall, 1, false);
        check(HofundItem.isOwnHofund(heimdall, heimdall.getMainHandItem()), "Hofund returns to his hand");
        ItemStack before = heimdall.getMainHandItem();
        heimdall.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        heimdall.getInventory().add(before);
        press(heimdall, 1, false);
        check(heimdall.getMainHandItem().is(Items.BREAD), "with his own in his pack, R only tells him to take it up");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: the combo and the lunge

    @GameTest(template = ARENA, batch = BATCH)
    public static void theThreeBlowsOfTheComboAndItsReset(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = heimdall(f, 10.5, 8.5);
        Zombie husk = husk(helper, 10.5, 10.5);
        float hp = husk.getHealth();
        press(heimdall, 1, false);
        float first = hp - husk.getHealth();
        TestKit.assertNear(first, 9.0 * 1.3, 0.6, "the first blow: 9, and Hofund's 30% more");
        check(Powers.get(heimdall).is(Live.HEIMDALL_SLASHING), "the blow's state is up for the clients");
        check(HeimdallAbilities.comboStep(heimdall, heimdall.level().getGameTime()) == 1, "the combo is on its first blow");
        press(heimdall, 1, false);
        check(husk.getHealth() == hp - first, "a second press within the blow's cooldown does nothing");
        for (int blow = 2; blow <= 3; blow++) {
            ready(heimdall, 1);
            husk.invulnerableTime = 0;
            float before = husk.getHealth();
            press(heimdall, 1, false);
            check(HeimdallAbilities.comboStep(heimdall, heimdall.level().getGameTime()) == blow, "blow " + blow);
            TestKit.assertNear(before - husk.getHealth(), (blow == 2 ? 11.0 : 14.0) * 1.3, 0.6, "blow " + blow + "'s damage");
        }
        check(HeimdallAbilities.nextStep(3, 100, 105) == 1, "after the third, the first again");
        check(HeimdallAbilities.nextStep(1, 100, 100 + HeimdallAbilities.COMBO_RESET + 1) == 1, "the combo lapses after 30 ticks");
        check(HeimdallAbilities.nextStep(1, 100, 120) == 2, "within them it goes on");
        check(HeimdallAbilities.inArc(Vec3.ZERO, new Vec3(0, 0, 1), new Vec3(0, 0, 3), 3.5, 100)
                && !HeimdallAbilities.inArc(Vec3.ZERO, new Vec3(0, 0, 1), new Vec3(0, 0, -3), 3.5, 100)
                && !HeimdallAbilities.inArc(Vec3.ZERO, new Vec3(0, 0, 1), new Vec3(0, 0, 5), 3.5, 100), "the arc: before him, within reach");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void withoutHofundHeimdallStrikesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = f.player(10.5, FLOOR, 8.5, Power.HEIMDALL);
        HofundBond.give(heimdall);
        ItemStack hofund = heimdall.getMainHandItem();
        heimdall.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        heimdall.getInventory().add(hofund);
        Zombie husk = husk(helper, 10.5, 10.5);
        press(heimdall, 1, false);
        check(husk.getHealth() == 200.0F, "an iron sword is not Hofund: no blow");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theLungeStopsAtAWallAndThrustsTheFirstCreature(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = heimdall(f, 10.5, 4.5);
        for (int x = 8; x <= 12; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
            }
        }
        Vec3 end = HeimdallAbilities.lungeEnd(heimdall.serverLevel(), heimdall, heimdall.position(), new Vec3(0, 0, 1));
        double went = end.z - heimdall.getZ();
        check(went > 2.0 && went < 3.3, "the dash stops short of the wall 3.5 blocks ahead: " + went);
        Vec3 open = HeimdallAbilities.lungeEnd(heimdall.serverLevel(), heimdall, heimdall.position(), new Vec3(-1, 0, 0));
        TestKit.assertNear(heimdall.getX() - open.x, HeimdallAbilities.LUNGE_DISTANCE, 0.3, "in the open, six blocks");
        look(heimdall, 90.0F, 0.0F);
        Zombie husk = husk(helper, 7.5, 4.5);
        press(heimdall, 1, true);
        TestKit.assertNear(200.0F - husk.getHealth(), 12.0 * 1.3, 0.6, "the thrust: 12, Hofund's 30% more");
        check(!Powers.get(heimdall).isReady(1, heimdall.level().getGameTime() + 40), "the lunge's long cooldown");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // G: the Sight

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSightTogglesAndIsSentToHimAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = heimdall(f, 4.5, 4.5);
        ServerPlayer other = f.player(16.5, FLOOR, 16.5);
        TestKit.sentPackets(heimdall).clear();
        TestKit.sentPackets(other).clear();
        press(heimdall, 2, false);
        check(Powers.get(heimdall).toggle(Toggle.HEIMDALL_SIGHT), "G opens the Sight");
        check(!sight(heimdall).isEmpty(), "he is sent what the Sight shows");
        check(sight(other).isEmpty(), "nobody else is");
        check(HeimdallSight.open(heimdall) && !HeimdallSight.open(other), "the Sight is his");
        CustomKitSupport.tickN(heimdall, 21);
        check(heimdall.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "he sees in the dark");
        press(heimdall, 2, false);
        check(!Powers.get(heimdall).toggle(Toggle.HEIMDALL_SIGHT), "G again closes it");
        List<HeimdallSightPayload> sent = sight(heimdall);
        check(!sent.isEmpty() && sent.get(sent.size() - 1).players().isEmpty(), "and his client forgets the far players");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void farSightIsHeldAndTheFarPlayersAreListed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = heimdall(f, 4.5, 4.5);
        CustomKitSupport.press(heimdall, 2, true, true);
        check(Powers.get(heimdall).is(Live.HEIMDALL_FARSIGHT), "Sneak + G held: Far Sight");
        check(HeimdallSight.open(heimdall), "the Sight is open while it is held");
        CustomKitSupport.press(heimdall, 2, true, false);
        heimdall.setShiftKeyDown(false);
        check(!Powers.get(heimdall).is(Live.HEIMDALL_FARSIGHT), "let go, it ends");
        // A player far away in the same realm is listed; one near is left to the client's own tracking.
        ServerPlayer near = f.player(8.5, FLOOR, 4.5);
        ServerPlayer far = f.player(8.5, FLOOR, 8.5);
        Vec3 away = heimdall.position().add(300, 0, 0);
        far.teleportTo(away.x, away.y, away.z);
        List<HeimdallSightPayload.Far> seen = HeimdallSight.far(heimdall);
        Set<UUID> ids = new HashSet<>();
        seen.forEach(s -> ids.add(s.id()));
        check(ids.contains(far.getUUID()) && !ids.contains(near.getUUID()), "the far player is listed, the near one is not: " + seen);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // B: the Ward and the Call

    @GameTest(template = ARENA, batch = BATCH)
    public static void theWardStopsAnArrowAndPushesAMonsterOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = heimdall(f, 10.5, 10.5);
        CustomKitSupport.press(heimdall, 4, false, true);
        PowerData data = Powers.get(heimdall);
        check(data.is(Live.HEIMDALL_WARD) && HeimdallAbilities.warded(heimdall), "B held: the Ward stands");
        check(heimdall.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0, "nothing knocks him back");
        Zombie husk = husk(helper, 12.5, 10.5);
        Arrow arrow = new Arrow(EntityType.ARROW, helper.getLevel());
        Vec3 from = heimdall.position().add(-5.5, 1.0, 0);
        arrow.moveTo(from.x, from.y, from.z);
        arrow.setDeltaMovement(1.5, 0, 0);
        helper.getLevel().addFreshEntity(arrow);
        PowerTicker.tick(heimdall);
        check(arrow.isRemoved(), "the arrow stops at the dome");
        check(husk.getDeltaMovement().x > 0.1 || husk.getX() - heimdall.getX() > 2.0, "the husk is pushed out: " + husk.getDeltaMovement());
        float health = heimdall.getHealth();
        heimdall.hurt(heimdall.damageSources().magic(), 10.0F);
        TestKit.assertNear(health - heimdall.getHealth(), 6.0, 0.01, "under the Ward he takes 40% less");
        CustomKitSupport.press(heimdall, 4, false, false);
        check(!data.is(Live.HEIMDALL_WARD) && !data.isReady(4, heimdall.level().getGameTime()), "let go: it falls, and recharges");
        check(heimdall.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1.0, "his footing is his own again");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theCallSummonsThreeEinherjar(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HeimdallAbilities.Summoner real = HeimdallAbilities.summoner;
        int[] asked = {0};
        HeimdallAbilities.summoner = (owner, count, ticks) -> {
            asked[0] = count;
            return List.of();
        };
        f.onClose(() -> HeimdallAbilities.summoner = real);
        ServerPlayer heimdall = heimdall(f, 10.5, 10.5);
        press(heimdall, 4, true);
        check(asked[0] == HeimdallAbilities.CALL_COUNT, "three Einherjar answer outside Asgard: " + asked[0]);
        check(!Powers.get(heimdall).isReady(4, heimdall.level().getGameTime() + 600), "the Call's long cooldown");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // N: the Strike and the Bifrost for a friend

    @GameTest(template = ARENA, batch = BATCH)
    public static void theStrikeHurtsWhatIsInItsRadius(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = heimdall(f, 3.5, 3.5);
        Zombie inside = husk(helper, 12.5, 12.5);
        Zombie outside = husk(helper, 17.5, 12.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(11.5, FLOOR, 12.5));
        HeimdallAbilities.strikeAt(heimdall, helper.absoluteVec(new Vec3(12.5, FLOOR, 12.5)));
        float struck = 200.0F - inside.getHealth();
        check(struck > 12.0F && struck <= 14.0F, "14 to what is in its radius (less its armour): " + struck);
        check(outside.getHealth() == 200.0F, "nothing beyond it");
        check(pig.getHealth() == pig.getMaxHealth(), "a farm animal is spared");
        check(inside.getDeltaMovement().y > 0.3, "thrown up");
        check(!inside.isOnFire(), "and set on fire by nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void heimdallSendsAFriendHome(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = heimdall(f, 4.5, 4.5);
        ServerPlayer friend = f.player(4.5, FLOOR, 8.5);
        Vec3 start = friend.position();
        check(Bifrost.send(heimdall, friend.getId(), "home"), "the Send begins");
        check(Bifrost.beingSent(friend) && Powers.get(friend).is(Live.BIFROST), "the light gathers round the friend");
        check(!Powers.get(heimdall).isReady(HeroAbility.HEIMDALL_STRIKE, heimdall.level().getGameTime()), "Heimdall pays the cooldown");
        check(Powers.get(friend).cooldownUntil(5) == 0 && PlaceBook.cooldown(friend, PlaceBook.Kind.BIFROST) == 0, "the friend pays nothing");
        f.everyTick(150, tick -> {
            PowerTicker.tick(friend);
            if (Bifrost.inBifrost(friend)) {
                return false;
            }
            check(friend.position().distanceToSqr(start) > 1.0, "he went: " + friend.position());
            return true;
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void aSneakingFriendRefusesTheSend(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = heimdall(f, 4.5, 4.5);
        ServerPlayer friend = f.player(4.5, FLOOR, 8.5);
        Vec3 start = friend.position();
        check(Bifrost.send(heimdall, friend.getId(), "home"), "the Send begins");
        friend.setShiftKeyDown(true);
        PowerTicker.tick(friend);
        friend.setShiftKeyDown(false);
        check(!Bifrost.inBifrost(friend) && !Bifrost.beingSent(friend), "Sneak: he stays out of the Bifrost");
        check(friend.position().equals(start), "where he was");
        ServerPlayer far = f.player(4.5, FLOOR, 8.5);
        far.teleportTo(far.getX() + 60, far.getY(), far.getZ());
        ready(heimdall, 5);
        check(!Bifrost.send(heimdall, far.getId(), "home"), "a friend beyond sendRange cannot be sent");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The custom hero's rows (3.4 R1-R7)

    @GameTest(template = ARENA, batch = BATCH)
    public static void aCustomHeroSlashesWithAnySwordOnHisOwnKey(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer custom = CustomKitSupport.hero(f, 10.5, 8.5, CustomKitSupport.def().pick("heimdall.hofund", 5).key(3, "heimdall.hofund").build());
        check(CustomKitSupport.column(custom, HeroAbility.HEIMDALL_HOFUND) == 3, "R1: on V, not his home R");
        Zombie husk = husk(helper, 10.5, 10.5);
        CustomKitSupport.tap(custom, 3, false);
        check(husk.getHealth() == 200.0F, "no sword in hand: no blow");
        custom.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        CustomKitSupport.tap(custom, 3, false);
        check(husk.getHealth() < 200.0F, "any sword serves a custom hero");
        long now = custom.level().getGameTime();
        PowerData data = Powers.get(custom);
        check(!data.isReady(3, now) && data.isReady(1, now), "R1: the cooldown is on his V");
        ready(custom, 3);
        husk.invulnerableTime = 0;
        float before = husk.getHealth();
        CustomKitSupport.tap(custom, 3, true);
        check(husk.getHealth() < before || data.cooldownUntil(3) - now > 20, "R3: Sneak + V is the Lunge (its long cooldown)");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBodyComesOnlyWithItsRow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer with = CustomKitSupport.hero(f, 4.5, 4.5, CustomKitSupport.def().pick("heimdall.body", 5).pick("heimdall.sight", 3)
                .key(2, "heimdall.sight").build());
        ServerPlayer without = CustomKitSupport.hero(f, 8.5, 4.5, CustomKitSupport.def().pick("heimdall.sight", 3).key(2, "heimdall.sight").build());
        PowerTicker.applyPassives(with, Powers.get(with));
        PowerTicker.applyPassives(without, Powers.get(without));
        check(with.getAttributeValue(Attributes.ARMOR) > without.getAttributeValue(Attributes.ARMOR), "armour by the body row");
        check(with.getMaxHealth() > without.getMaxHealth(), "health by the body row");
        ServerPlayer hero = f.player(12.5, FLOOR, 4.5, Power.HEIMDALL);
        PowerTicker.applyPassives(hero, Powers.get(hero));
        TestKit.assertNear(hero.getMaxHealth(), 20.0 + HeimdallAbilities.BODY_HEALTH_BONUS, 0.01, "Heimdall: +8 health");
        CustomKitSupport.tap(without, 2, false);
        check(Powers.get(without).toggle(Toggle.HEIMDALL_SIGHT) && HeimdallSight.open(without), "a custom hero's Sight opens on his G");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Bifrost screen's menu, Heimdall's words

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBifrostMenuOffersTheRealmsThenThePlaces(GameTestHelper helper) {
        List<BifrostData.Place> places = List.of(new BifrostData.Place("Base", Level.OVERWORLD, Vec3.ZERO, 0.0F, false),
                new BifrostData.Place("Hall", AsgardDimension.KEY, Vec3.ZERO, 0.0F, false));
        List<BifrostMenu.Entry> out = BifrostMenu.entries(false, true, places);
        check(out.size() == 4 && out.get(0).kind() == BifrostMenu.Kind.ASGARD && out.get(1).kind() == BifrostMenu.Kind.HOME
                && out.get(1).labelKey().equals(BifrostMenu.HOME_KEY) && out.get(3).name().equals("Hall"), "Asgard, Home, the places: " + out);
        List<BifrostMenu.Entry> in = BifrostMenu.entries(true, true, places);
        check(in.get(0).kind() == BifrostMenu.Kind.HOME && in.get(0).labelKey().equals(BifrostMenu.HOME_MIDGARD_KEY), "in Asgard: no Asgard, Home (Midgard)");
        check(BifrostMenu.entries(false, false, places).get(0).kind() == BifrostMenu.Kind.HOME, "Asgard closed: not offered");
        check(BifrostMenu.action(out.get(0), -1).action() == BifrostActionPayload.ASGARD, "Asgard's button");
        check(BifrostMenu.action(out.get(3), -1).equals(new BifrostActionPayload(BifrostActionPayload.TRAVEL, 1, "Hall")), "a place's");
        check(BifrostMenu.action(out.get(0), 42).equals(new BifrostActionPayload(BifrostActionPayload.SEND, 42, "asgard"))
                && BifrostMenu.action(out.get(1), 42).name().equals("home")
                && BifrostMenu.action(out.get(2), 42).name().equals("Base"), "Send mode sends the friend there");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void heimdallsWordsHaveAtMostThreeAnswersInBothLanguages(GameTestHelper helper) throws IOException {
        HeimdallTalk.register();
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        List<AsgardDialogue.Node> nodes = AsgardDialogue.nodes(Role.HEIMDALL);
        Set<String> keys = new HashSet<>();
        for (AsgardDialogue.Node node : nodes) {
            keys.add(node.key());
            check(node.options().size() <= 3, node.key() + " offers at most three answers");
            String line = "npc.enhanced.heimdall." + node.key();
            check(en.has(line) && he.has(line), "the words of " + line);
            check(en.get(line).getAsString().length() <= 140 && he.get(line).getAsString().length() <= 140, line + " fits");
            for (AsgardDialogue.Option option : node.options()) {
                String key = "npc.enhanced.option." + option.key();
                check(en.has(key) && he.has(key), "the words of " + key);
            }
        }
        check(keys.containsAll(List.of("greeting", "watch_offer", "watch_running", "hold_hofund", "idle")), "his nodes: " + keys);
        check(nodes.size() == keys.size(), "each once, however often the table is asked to register them");
        helper.succeed();
    }

    static JsonObject lang(String name) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(HeimdallTests.class.getResourceAsStream(
                "/assets/enhanced/lang/" + name + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // PvP

    @GameTest(template = ARENA, batch = PVP)
    public static void theBlowsHurtPlayersOnlyWithPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer heimdall = heimdall(f, 10.5, 8.5);
        ServerPlayer victim = f.player(10.5, FLOOR, 10.5);
        f.pvp(false);
        press(heimdall, 1, false);
        check(victim.getHealth() == victim.getMaxHealth(), "PvP off: the blow spares a player");
        f.pvp(true);
        ready(heimdall, 1);
        press(heimdall, 1, false);
        check(victim.getHealth() < victim.getMaxHealth(), "PvP on: it lands");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Guardian's Watch

    /** A player of the test brought to Asgard's Observatory (the Bifrost's catch: at once). */
    private static ServerPlayer inAsgard(TestKit.Fixture f, GameTestHelper helper) {
        AsgardTestSupport.withArrival(helper.getLevel().getServer());
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        check(Bifrost.open(player, Bifrost.Destination.OBSERVATORY, Bifrost.Source.RESCUE), "the Bifrost takes him to the Observatory");
        check(AsgardDimension.in(player), "he is in Asgard");
        f.onClose(() -> GuardiansWatch.lose(player, "message.enhanced.watch.lost"));
        return player;
    }

    @GameTest(template = ARENA, batch = WATCH, timeoutTicks = 200)
    public static void theWatchIsWonByStrikingTheSevenShadows(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        check(GuardiansWatch.pick(UUID.nameUUIDFromBytes(new byte[]{1}), 0, CityPlan.hidingSpots(), 7)
                .equals(GuardiansWatch.pick(UUID.nameUUIDFromBytes(new byte[]{1}), 0, CityPlan.hidingSpots(), 7)), "the spots are the player's");
        check(!GuardiansWatch.pick(UUID.nameUUIDFromBytes(new byte[]{1}), 0, CityPlan.hidingSpots(), 7)
                .equals(GuardiansWatch.pick(UUID.nameUUIDFromBytes(new byte[]{1}), 1, CityPlan.hidingSpots(), 7)), "a new try, new spots");
        ServerPlayer outside = f.player(4.5, FLOOR, 4.5);
        check("message.enhanced.watch.not_here".equals(GuardiansWatch.refusal(outside)), "the Watch is kept in Asgard");
        ServerPlayer watcher = inAsgard(f, helper);
        check(GuardiansWatch.start(watcher), "the Watch begins");
        check(!GuardiansWatch.start(watcher), "one Watch at a time");
        check(HeimdallSight.open(watcher), "the Watch lends him the Sight");
        ServerPlayer stranger = inAsgard(f, helper);
        // The shadows hide as soon as their chunks are held (a tick or two for the tickets).
        f.everyTick(100, tick -> {
            PowerTicker.tick(watcher);
            List<Entity> shadows = GuardiansWatch.shadowsOf(watcher);
            if (shadows.size() < GuardiansWatch.SHADOWS) {
                return false;
            }
            Entity first = shadows.get(0);
            first.hurt(stranger.damageSources().playerAttack(stranger), 20.0F);
            check(first.isAlive() && GuardiansWatch.found(watcher) == 0, "nobody else can strike his shadows");
            for (Entity shadow : shadows) {
                shadow.hurt(watcher.damageSources().playerAttack(watcher), 1.0F);
            }
            AsgardProgress progress = watcher.getData(ModAttachments.ASGARD);
            check(!GuardiansWatch.watching(watcher) && progress.has(AsgardProgress.WATCH_WON), "all seven struck: the Watch is won");
            check(HofundBond.hasOwn(watcher), "Heimdall gives him Hofund");
            check(GuardiansWatch.refusal(watcher) != null, "it is not taken twice");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = WATCH, timeoutTicks = 200)
    public static void aLostWatchCleansUpAndMustRest(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerPlayer watcher = inAsgard(f, helper);
        check(GuardiansWatch.start(watcher), "the Watch begins");
        f.everyTick(100, tick -> {
            PowerTicker.tick(watcher);
            List<Entity> shadows = GuardiansWatch.shadowsOf(watcher);
            if (shadows.size() < GuardiansWatch.SHADOWS) {
                return false;
            }
            new HeimdallServer().onLogout(watcher);
            check(!GuardiansWatch.watching(watcher), "logging out loses the Watch");
            for (Entity shadow : shadows) {
                check(shadow.isRemoved(), "its shadows are gone");
            }
            check(!watcher.getData(ModAttachments.ASGARD).has(AsgardProgress.WATCH_WON), "nothing won");
            check("message.enhanced.watch.rest".equals(GuardiansWatch.refusal(watcher)), "he must rest before he tries again");
            watcher.setData(ModAttachments.ASGARD, watcher.getData(ModAttachments.ASGARD).withWatchReadyAt(0));
            f.config(com.kolodkingal.enhanced.config.HeimdallConfig.WATCH_SECONDS, 30);
            check(GuardiansWatch.start(watcher), "rested, he may try again");
            long left = GuardiansWatch.ticksLeft(watcher, watcher.server.overworld().getGameTime());
            check(left > 0 && left <= 30 * 20, "the sand of watchSeconds: " + left);
            GuardiansWatch.lose(watcher, "message.enhanced.watch.lost");
            check(GuardiansWatch.shadowsOf(watcher).isEmpty(), "and lost again, clean");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = WATCH, timeoutTicks = 200)
    public static void theCallIsFourInAsgard(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerPlayer player = inAsgard(f, helper);
        check(HeimdallAbilities.callCount(player) == HeimdallAbilities.CALL_COUNT_ASGARD, "four Einherjar answer in Asgard");
        helper.succeed();
    }
}
