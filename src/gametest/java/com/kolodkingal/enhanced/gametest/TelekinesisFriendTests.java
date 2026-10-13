package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Telekinesis and the players it holds (3.1, D-M 3.8), for every form: a player let go of is not gripped again by the same
 * hero at once.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class TelekinesisFriendTests {

    /** Presses the key of an ability, with Sneak held or not. */
    private static void press(ServerPlayer player, int ability, boolean sneak) {
        player.setShiftKeyDown(sneak);
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
        player.setShiftKeyDown(false);
    }

    /**
     * M55 (D-M 3.8): Magneto sets down a player in an iron chestplate; his R at that player at once grips nothing and says
     * {@code telekinesis.player_regrip}, while Wanda's V takes the player at once (the wait is the holder's own); 60 ticks
     * ({@code magneto.telekinesisPlayerRegripTicks}) after his grip ended, his R holds the player again.
     */
    @GameTest(template = ARENA, batch = "telekinesis_regrip", timeoutTicks = 100)
    public static void aPlayerIsNotGrippedAgainAtOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        ServerPlayer wanda = f.player(10.5, FLOOR, 3.5, Power.SCARLET_WITCH);
        ServerPlayer friend = f.player(10.5, FLOOR, 10.5);
        friend.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));

        lookAt(magneto, friend);
        press(magneto, 1, false);
        check(Telekinesis.holds(magneto, friend), "Magneto's R takes the player in iron");
        press(magneto, 1, true);
        check(!Telekinesis.isHolding(magneto), "Sneak + R sets him down");
        long letGo = helper.getLevel().getGameTime();

        lastActionBarKey(magneto);
        press(magneto, 1, false);
        check(!Telekinesis.isHeld(friend), "R at him at once grips nothing");
        check(Telekinesis.PLAYER_REGRIP.equals(lastActionBarKey(magneto)), "and says to give him a moment");

        lookAt(wanda, friend);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, friend), "Wanda's V takes him at once: the wait is Magneto's, not hers");
        Telekinesis.release(wanda, false);

        f.everyTick(80, tick -> {
            long since = helper.getLevel().getGameTime() - letGo;
            if (since == 59) {
                lastActionBarKey(magneto);
                press(magneto, 1, false);
                check(!Telekinesis.isHeld(friend) && Telekinesis.PLAYER_REGRIP.equals(lastActionBarKey(magneto)),
                        "59 ticks after, Magneto still may not grip him");
            }
            if (since < 60) {
                return false;
            }
            press(magneto, 1, false);
            check(Telekinesis.holds(magneto, friend), "60 ticks after his grip ended, Magneto's R holds him again");
            Telekinesis.release(magneto, false);
            return true;
        });
    }

    private TelekinesisFriendTests() {
    }
}
