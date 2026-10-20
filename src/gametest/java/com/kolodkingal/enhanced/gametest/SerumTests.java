package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.network.SerumScreenPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.serum.SerumServer;
import com.kolodkingal.enhanced.wolverine.Claws;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Super Soldier Serum (3.8, lane L1, PLAN 2): using it sends the choice screen (Falcon locked without the
 * Exo-Wings, open with them in the inventory) and uses nothing up; the server checks the answer again: a choice with no
 * screen open is refused, a forged Falcon choice without the wings is refused (the serum kept), Captain America comes
 * through {@code Powers.awaken} (a Wolverine with his claws out loses them: the old hero cleared), the serum used up
 * (not in Creative) and a shield given; the same hero is refused; the open screen runs out at 600 ticks (not 599); the
 * serum moved to the other hand is refused; Escape (no answer) keeps it; the operators' command opens the screen
 * without a serum. Batch {@code serum}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SerumTests {
    private static final String BATCH = "serum";

    /** A Survival player holding a serum in {@code hand}. */
    static ServerPlayer holder(TestKit.Fixture f, double x, double z, Power power, InteractionHand hand) {
        ServerPlayer player = f.player(x, FLOOR, z, power);
        f.onClose(() -> HeroLifecycle.onLogout(player));
        f.onClose(() -> SerumServer.forget(player));
        player.setItemInHand(hand, new ItemStack(ModItems.SPECIAL_SOLDIER_SERUM.get()));
        return player;
    }

    /** He uses the serum in {@code hand} (the item's own use, server side). */
    static void use(ServerPlayer player, InteractionHand hand) {
        player.getItemInHand(hand).use(player.level(), player, hand);
    }

    /** The serum's screen the server sent him since the last look, or null. */
    @Nullable
    static SerumScreenPayload screen(ServerPlayer player) {
        SerumScreenPayload found = null;
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom
                    && custom.payload() instanceof SerumScreenPayload screen) {
                found = screen;
            }
        }
        return found;
    }

    static int serums(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.SPECIAL_SOLDIER_SERUM.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** The use sends the screen, Falcon locked; with the wings in the inventory, open. Nothing is used up. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theUseSendsTheScreen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = holder(f, 3.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        TestKit.sentPackets(plain);
        use(plain, InteractionHand.MAIN_HAND);
        SerumScreenPayload first = screen(plain);
        check(first != null && !first.falconUnlocked() && first.hand() == InteractionHand.MAIN_HAND,
                "the screen, Falcon locked: " + first);
        check(SerumServer.pending(plain) != null && serums(plain) == 1, "a choice open, the serum kept");
        ServerPlayer winged = holder(f, 8.5, 3.5, Power.NONE, InteractionHand.OFF_HAND);
        winged.getInventory().add(new ItemStack(ModItems.FALCON_EXO_WINGS.get()));
        TestKit.sentPackets(winged);
        use(winged, InteractionHand.OFF_HAND);
        SerumScreenPayload second = screen(winged);
        check(second != null && second.falconUnlocked() && second.hand() == InteractionHand.OFF_HAND,
                "with the wings in the inventory, Falcon open: " + second);
        helper.succeed();
    }

    /** No screen open: the answer is refused and nothing changes. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aChoiceWithNoScreenIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = holder(f, 3.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        check(!SerumServer.choose(player, Power.CAPTAIN_AMERICA), "refused");
        check(Powers.power(player) == Power.NONE && serums(player) == 1, "nobody changed, the serum kept");
        helper.succeed();
    }

    /** A Falcon answer without the wings (a forged one: the card was locked) is refused; the serum is kept. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aForgedFalconChoiceIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = holder(f, 3.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        use(player, InteractionHand.MAIN_HAND);
        check(!SerumServer.choose(player, Power.FALCON), "refused");
        check(Powers.power(player) == Power.NONE && serums(player) == 1, "not Falcon, the serum kept");
        // Not one of the serum's heroes either.
        use(player, InteractionHand.MAIN_HAND);
        check(!SerumServer.choose(player, Power.THOR) && Powers.power(player) == Power.NONE, "Thor is no choice");
        // With the wings it is his.
        ServerPlayer winged = holder(f, 8.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        winged.getInventory().add(new ItemStack(ModItems.FALCON_EXO_WINGS.get()));
        use(winged, InteractionHand.MAIN_HAND);
        check(SerumServer.choose(winged, Power.FALCON) && Powers.power(winged) == Power.FALCON, "Falcon");
        check(serums(winged) == 0, "the serum used up");
        helper.succeed();
    }

    /**
     * Captain America through {@code Powers.awaken}: a Wolverine with his claws out loses them (the old hero cleared),
     * the serum is used up, a Vibranium Shield is put in the hand it left; in Creative the serum stays.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void captainAmericaThroughTheAwakening(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = holder(f, 3.5, 3.5, Power.WOLVERINE, InteractionHand.MAIN_HAND);
        logan.doTick();
        Claws.toggle(logan, Powers.get(logan));
        check(Claws.out(Powers.get(logan)), "his claws are out");
        use(logan, InteractionHand.MAIN_HAND);
        check(SerumServer.choose(logan, Power.CAPTAIN_AMERICA), "the choice is taken");
        check(Powers.power(logan) == Power.CAPTAIN_AMERICA, "he is Captain America");
        check(!Powers.get(logan).toggle(Toggle.CLAWS_OUT), "the claws are gone with Wolverine");
        check(serums(logan) == 0 && logan.getMainHandItem().is(ModItems.VIBRANIUM_SHIELD.get()),
                "the serum used up, the shield in his hand");
        check(logan.getMaxHealth() == 26.0F, "a super-soldier's health: " + logan.getMaxHealth());
        ServerPlayer builder = holder(f, 8.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        builder.setGameMode(GameType.CREATIVE);
        use(builder, InteractionHand.MAIN_HAND);
        check(SerumServer.choose(builder, Power.RED_HULK) && Powers.power(builder) == Power.RED_HULK, "Red Hulk");
        check(serums(builder) == 1, "in Creative the serum stays");
        helper.succeed();
    }

    /** He is Captain America already: refused, the serum kept. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theSameHeroIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer cap = holder(f, 3.5, 3.5, Power.CAPTAIN_AMERICA, InteractionHand.MAIN_HAND);
        use(cap, InteractionHand.MAIN_HAND);
        check(!SerumServer.choose(cap, Power.CAPTAIN_AMERICA), "refused");
        check(Powers.power(cap) == Power.CAPTAIN_AMERICA && serums(cap) == 1, "the serum kept");
        helper.succeed();
    }

    /** The open screen is good for 599 ticks: an answer at 600 is refused. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 700)
    public static void theScreenRunsOutAt600Ticks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer early = holder(f, 3.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        ServerPlayer late = holder(f, 8.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        use(early, InteractionHand.MAIN_HAND);
        use(late, InteractionHand.MAIN_HAND);
        long opened = helper.getLevel().getGameTime();
        check(SerumServer.pendingTicks() == 600, "the default: " + SerumServer.pendingTicks());
        helper.runAfterDelay(599, () -> {
            check(helper.getLevel().getGameTime() - opened == 599, "599 ticks");
            check(SerumServer.choose(early, Power.CAPTAIN_AMERICA), "at 599 the choice is taken");
        });
        helper.runAfterDelay(600, () -> {
            check(helper.getLevel().getGameTime() - opened == 600, "600 ticks");
            check(!SerumServer.choose(late, Power.CAPTAIN_AMERICA), "at 600 it is refused");
            check(Powers.power(late) == Power.NONE && serums(late) == 1, "the serum kept");
            helper.succeed();
        });
    }

    /** The serum moved to the other hand (or another slot chosen) before the answer: refused, kept. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theSerumMovedIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = holder(f, 3.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        use(player, InteractionHand.MAIN_HAND);
        ItemStack serum = player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, player.getOffhandItem());
        player.setItemInHand(InteractionHand.OFF_HAND, serum);
        check(!SerumServer.choose(player, Power.CAPTAIN_AMERICA), "the hands swapped: refused");
        check(Powers.power(player) == Power.NONE && player.getOffhandItem().is(ModItems.SPECIAL_SOLDIER_SERUM.get()),
                "nobody changed, the serum in the other hand");
        ServerPlayer other = holder(f, 8.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        use(other, InteractionHand.MAIN_HAND);
        other.getInventory().selected = (other.getInventory().selected + 1) % 9;
        other.getInventory().setItem(other.getInventory().selected, new ItemStack(ModItems.SPECIAL_SOLDIER_SERUM
                .get()));
        check(!SerumServer.choose(other, Power.CAPTAIN_AMERICA), "another slot (another serum): refused");
        check(Powers.power(other) == Power.NONE && serums(other) == 2, "both serums kept");
        helper.succeed();
    }

    /** Escape sends nothing: the serum is kept and nothing changes; the screen opens again on the next use. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void escapeKeepsTheSerum(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = holder(f, 3.5, 3.5, Power.NONE, InteractionHand.MAIN_HAND);
        use(player, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(20, () -> {
            check(Powers.power(player) == Power.NONE && serums(player) == 1, "kept");
            TestKit.sentPackets(player);
            use(player, InteractionHand.MAIN_HAND);
            check(screen(player) != null, "the screen again");
            check(SerumServer.choose(player, Power.CAPTAIN_AMERICA), "and a choice now");
            helper.succeed();
        });
    }

    /** The operators' command: the screen without a serum, the choice checked as any other, nothing used up. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theCommandOpensTheScreen(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(3.5, FLOOR, 3.5);
        f.onClose(() -> HeroLifecycle.onLogout(player));
        TestKit.sentPackets(player);
        SerumServer.openFree(player);
        check(screen(player) != null, "the screen");
        check(!SerumServer.choose(player, Power.FALCON), "Falcon still needs the wings");
        SerumServer.openFree(player);
        check(SerumServer.choose(player, Power.CAPTAIN_AMERICA) && Powers.power(player) == Power.CAPTAIN_AMERICA,
                "Captain America");
        check(player.getMainHandItem().is(ModItems.VIBRANIUM_SHIELD.get()), "with his shield");
        helper.succeed();
    }
}
