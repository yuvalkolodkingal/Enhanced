package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Telekinesis and the players it holds (3.1, D-M 3.8), for every form: a player let go of is not gripped again by the same
 * hero at once, and a friend let go of is spared his landing, until another grip takes him.
 */
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
    @EnhancedTest(template = ARENA, batch = "telekinesis_regrip", timeoutTicks = 100)
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

    /**
     * M54a (D-M 3.8, the player part with an iron chestplate, R18): with PvP off, a player a grip lets go of is spared his
     * landing. Magneto holds a player in an iron chestplate, carries him 12 blocks up and lets go: fall damage is refused
     * while he is in the air; once he has landed (3 ticks later) it hurts again. Wanda's let-go spares him too (every form).
     * With PvP on, a let-go spares nobody.
     */
    @EnhancedTest(template = ARENA, batch = "telekinesis_friends", timeoutTicks = 40)
    public static void friendsAreSetDownSafely(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.pvp(false);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        ServerPlayer wanda = f.player(10.5, FLOOR, 3.5, Power.SCARLET_WITCH);
        ServerPlayer billy = f.player(17.5, FLOOR, 10.5, Power.BILLY);
        ServerPlayer friend = f.player(10.5, FLOOR, 10.5);
        friend.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        Vec3 ground = friend.position();

        lookAt(magneto, friend);
        press(magneto, 1, false);
        check(Telekinesis.holds(magneto, friend), "Magneto's R takes the player in iron");
        friend.teleportTo(ground.x, ground.y + 12.0, ground.z);
        friend.setOnGround(false);
        press(magneto, 1, true);
        check(!Telekinesis.isHolding(magneto) && Telekinesis.isSpared(friend), "let go 12 blocks up, he is spared his landing");
        float health = friend.getHealth();
        check(!friend.hurt(friend.damageSources().fall(), 6.0F) && friend.getHealth() == health, "a fall does him no harm");
        friend.setOnGround(true);
        f.everyTick(20, tick -> {
            if (tick < 3) {
                return false;
            }
            if (tick == 3) {
                check(!Telekinesis.isSpared(friend), "3 ticks after he landed, the guard is gone");
                check(friend.hurt(friend.damageSources().fall(), 6.0F) && friend.getHealth() < health, "and a fall hurts him again");
                friend.teleportTo(ground.x, ground.y, ground.z);
                lookAt(wanda, friend);
                press(wanda, 3, false);
                check(Telekinesis.holds(wanda, friend), "Wanda holds him");
                press(wanda, 3, true);
                check(Telekinesis.isSpared(friend), "and her let-go spares him too");
                friend.setOnGround(true);
                return false;
            }
            if (tick == 10) {
                check(!Telekinesis.isSpared(friend), "he landed: the guard is gone");
                f.pvp(true);
                lookAt(billy, friend);
                press(billy, 3, false);
                check(Telekinesis.holds(billy, friend), "with PvP on, Billy holds him");
                press(billy, 3, true);
                check(!Telekinesis.isSpared(friend), "and his let-go spares nobody: a friend may hurt him now");
                return true;
            }
            return false;
        });
    }

    /**
     * D-M 3.8 (review fix): the landing guard is the last grip's. With PvP on, Wanda lets go of her teammate (friendly fire
     * off) up in the air: he is spared his landing. Billy, on no team, takes hold of him before he lands: the guard was
     * hers, not his grip's, and goes; let go by Billy, who may hurt him, he is spared nothing, and a fall hurts him.
     */
    @EnhancedTest(template = ARENA, batch = "telekinesis_spared_enemy", timeoutTicks = 20)
    public static void aFriendsGuardIsNotAnEnemysGrip(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(Telekinesis::clear);
        f.pvp(true);
        ServerPlayer wanda = f.player(3.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        ServerPlayer billy = f.player(10.5, FLOOR, 3.5, Power.BILLY);
        ServerPlayer friend = f.player(10.5, FLOOR, 10.5);
        Scoreboard scoreboard = helper.getLevel().getServer().getScoreboard();
        PlayerTeam team = scoreboard.addPlayerTeam("telekinesis_test_" + UUID.randomUUID().toString().substring(0, 8));
        team.setAllowFriendlyFire(false);
        scoreboard.addPlayerToTeam(wanda.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(friend.getScoreboardName(), team);
        f.onClose(() -> scoreboard.removePlayerTeam(team));
        Vec3 ground = friend.position();

        lookAt(wanda, friend);
        press(wanda, 3, false);
        check(Telekinesis.holds(wanda, friend), "Wanda holds her teammate");
        friend.teleportTo(ground.x, ground.y + 6.0, ground.z);
        friend.setOnGround(false);
        press(wanda, 3, true);
        check(!Telekinesis.isHolding(wanda) && Telekinesis.isSpared(friend), "let go up in the air, he is spared his landing");

        lookAt(billy, friend);
        press(billy, 3, false);
        check(Telekinesis.holds(billy, friend), "Billy, on no team, takes hold of him before he lands");
        check(!Telekinesis.isSpared(friend), "the guard was Wanda's, not his grip's: it goes");
        press(billy, 3, true);
        check(!Telekinesis.isHolding(billy) && !Telekinesis.isSpared(friend), "let go by Billy, who may hurt him, he is spared nothing");
        float health = friend.getHealth();
        check(friend.hurt(friend.damageSources().fall(), 4.0F) && friend.getHealth() < health, "a fall hurts him");
        helper.succeed();
    }

    private TelekinesisFriendTests() {
    }
}
