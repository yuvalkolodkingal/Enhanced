package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGifts;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.BossRewards;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.BossFrameworkTests.fixture;
import static com.kolodkingal.enhanced.gametest.BossFrameworkTests.moveTo;
import static com.kolodkingal.enhanced.gametest.BossFrameworkTests.start;
import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Who a boss fight rewards, and how the reward, or a refund, reaches them: into the inventory, at the feet for them
 * alone, or as a gift that waits for the login, the respawn or the next dimension.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BossRewardTests {

    private static boolean has(ServerPlayer player, net.minecraft.world.item.Item item) {
        return player.getInventory().contains(new ItemStack(item));
    }

    @GameTest(template = ARENA, batch = "boss_reward_rule", timeoutTicks = 160)
    public static void aParticipantIsRewardedForTimeAndDamageOrForTheLastBlowAndNeverForBeingCreative(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.REWARD_MIN_SECONDS, 1);
        f.config(BossConfig.XP_REWARD_PER_PLAYER, 100);
        ServerPlayer worker = f.player(10.5, FLOOR, 10.5);
        ServerPlayer idler = f.player(12.5, FLOOR, 10.5);
        ServerPlayer creative = f.player(8.5, FLOOR, 10.5, GameType.CREATIVE);
        ServerPlayer latecomer = f.player(1.5, FLOOR, 1.5);
        ServerPlayer finisher = f.player(1.5, FLOOR, 3.5);
        TestFight fight = start(f, BossKind.THANOS, worker, BossFrameworkTests.fight(f, BossKind.THANOS, Containment.NONE, false));
        f.everyTick(100, tick -> {
            if (tick == 30) {
                moveTo(helper, latecomer, 9.5, 9.5);
                moveTo(helper, finisher, 11.5, 9.5);
            }
            if (tick < 34) {
                return false;
            }
            fight.credit(worker, BossFight.DAMAGE, 5.0F);
            fight.credit(creative, BossFight.DAMAGE, 50.0F);
            fight.credit(latecomer, BossFight.DAMAGE, 50.0F);
            fight.lastBlow(finisher);
            check(fight.participant(worker.getUUID()).secondsPresent() >= 1 && fight.participant(latecomer.getUUID()).secondsPresent() < 1,
                    "the latecomer has not been there a second");
            fight.end(BossEnd.VICTORY);
            check(has(worker, Items.DIAMOND), "who was there and hurt it is rewarded");
            check(!has(idler, Items.DIAMOND), "who only stood by is not");
            check(!has(latecomer, Items.DIAMOND), "who hurt it but came late is not");
            check(has(finisher, Items.DIAMOND), "who struck the last blow is, whatever the time and the damage");
            check(!has(creative, Items.DIAMOND), "a Creative player is not, whatever they did");
            check(worker.totalExperience == 100 && idler.totalExperience == 0, "the experience goes to the rewarded");
            check(fight.log.stream().filter(line -> line.startsWith("reward:")).count() == 2, "the lane gave out two rewards");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "boss_reward_creative", timeoutTicks = 60)
    public static void creativePlayersAreRewardedIfTheServerSaysSo(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.REWARD_MIN_SECONDS, 0);
        f.config(BossConfig.CREATIVE_GETS_REWARD, true);
        ServerPlayer creative = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        TestFight fight = start(f, BossKind.ULTRON, creative, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, false));
        helper.runAfterDelay(3, () -> {
            fight.credit(creative, BossFight.DAMAGE, 1.0F);
            fight.end(BossEnd.VICTORY);
            check(has(creative, Items.DIAMOND), "creativeGetsReward gives it to them");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "boss_reward_lane")
    public static void theLaneSetsTheDamageThatCountsAndTheLeastOfIt(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(BossConfig.REWARD_MIN_SECONDS, 0);
        ServerPlayer a = f.player(10.5, FLOOR, 10.5);
        TestFight fight = start(f, BossKind.THANOS, a, BossFrameworkTests.fight(f, BossKind.THANOS, Containment.NONE, false));
        check(BossRewards.eligible(fight).isEmpty(), "with no damage and no last blow, nobody");
        fight.credit(a, BossFight.DAMAGE, 0.5F);
        check(BossRewards.eligible(fight).size() == 1, "any damage will do for a boss that asks for none in particular");
        fight.minDamage = 90.0F;
        check(BossRewards.eligible(fight).isEmpty(), "Thanos asks for 3 percent of his health: 90 of 3000");
        fight.credit(a, BossFight.DAMAGE, 90.0F);
        check(BossRewards.eligible(fight).size() == 1, "which is enough");
        BossFights.clear();
        TestFight shield = start(f, BossKind.APOCALYPSE, a, BossFrameworkTests.fight(f, BossKind.APOCALYPSE, Containment.NONE, false));
        shield.counters = java.util.Set.of(BossFight.DAMAGE, "shield");
        shield.credit(a, "shield", 3.0F);
        check(BossRewards.eligible(shield).size() == 1, "Apocalypse counts the shield worn down as damage done");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Delivery

    @GameTest(template = ARENA, batch = "boss_reward_delivery", timeoutTicks = 60)
    public static void aRewardGoesToTheInventoryOrToTheFeetOfItsOwnerOrWaitsForThem(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer inventory = f.player(10.5, FLOOR, 10.5);
        ServerPlayer full = f.player(12.5, FLOOR, 10.5);
        ServerPlayer stranger = f.player(12.5, FLOOR, 12.5);
        ServerPlayer gone = f.player(8.5, FLOOR, 10.5);
        ServerPlayer dead = f.player(8.5, FLOOR, 12.5);
        for (int slot = 0; slot < full.getInventory().items.size(); slot++) {
            full.getInventory().items.set(slot, new ItemStack(Items.STONE, 64));
        }
        BossRewards.give(inventory, new ItemStack(Items.DIAMOND));
        check(has(inventory, Items.DIAMOND), "it goes straight into the inventory");

        BossRewards.give(full, new ItemStack(Items.DIAMOND));
        check(!has(full, Items.DIAMOND), "a full inventory has no room");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(full.blockPosition()).inflate(3),
                item -> item.getItem().is(Items.DIAMOND));
        check(drops.size() == 1, "it drops at the feet instead");
        ItemEntity drop = drops.get(0);
        drop.playerTouch(stranger);
        check(!has(stranger, Items.DIAMOND) && !drop.isRemoved(), "nobody else can pick it up");
        check(drop.getAge() < 0 && drop.isInvulnerable(), "and it never despawns or burns");

        // A fighter who is gone or dead at the victory gets a gift.
        TestFight fight = start(f, BossKind.ULTRON, inventory, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.NONE, false));
        f.config(BossConfig.REWARD_MIN_SECONDS, 0);
        helper.runAfterDelay(3, () -> {
            for (ServerPlayer player : List.of(inventory, gone, dead)) {
                check(fight.isParticipant(player.getUUID()), player.getGameProfile().getName() + " fights");
                fight.credit(player, BossFight.DAMAGE, 1.0F);
            }
            server.getPlayerList().remove(gone);
            dead.kill();
            fight.end(BossEnd.VICTORY);
            check(BossGifts.pending(server, gone.getUUID()).size() == 1 && BossGifts.pending(server, dead.getUUID()).size() == 1,
                    "the fighters who are offline and dead have a gift waiting");
            check(BossGifts.pending(server, inventory.getUUID()).isEmpty(), "the one who is there does not");

            // It is delivered when they come back: the same person, a new body, logging in.
            ServerPlayer back = f.player(8.5, FLOOR, 10.5, GameType.SURVIVAL, gone.getUUID());
            check(has(back, Items.DIAMOND) && BossGifts.pending(server, gone.getUUID()).isEmpty(), "at login");
            check(BossGifts.deliver(dead) == 0 && BossGifts.pending(server, dead.getUUID()).size() == 1, "a dead player gets nothing yet");
            ServerPlayer reborn = server.getPlayerList().respawn(dead, false, Entity.RemovalReason.KILLED);
            f.onClose(() -> server.getPlayerList().remove(reborn));
            check(has(reborn, Items.DIAMOND) && BossGifts.pending(server, dead.getUUID()).isEmpty(), "and at the respawn");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "boss_gifts")
    public static void aGiftWaitsForTheLoginTheRespawnOrTheDimensionChange(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        BossGifts.add(server, player.getUUID(), new ItemStack(Items.NETHER_STAR));
        BossGifts.add(server, player.getUUID(), ItemStack.EMPTY);
        check(BossGifts.pending(server, player.getUUID()).size() == 1, "an empty stack is no gift");
        lastActionBarKey(player);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        check(has(player, Items.NETHER_STAR) && BossGifts.pending(server, player.getUUID()).isEmpty(), "login delivers it");
        check("message.enhanced.boss.gift".equals(lastActionBarKey(player)), "and says so");

        BossGifts.add(server, player.getUUID(), new ItemStack(Items.ECHO_SHARD));
        BossGifts.add(server, player.getUUID(), new ItemStack(Items.ECHO_SHARD));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, Level.OVERWORLD, Level.NETHER));
        check(player.getInventory().countItem(Items.ECHO_SHARD) == 2 && BossGifts.pending(server, player.getUUID()).isEmpty(),
                "a change of dimension delivers every one that waits");

        BossGifts.add(server, player.getUUID(), new ItemStack(Items.TOTEM_OF_UNDYING));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));
        check(has(player, Items.TOTEM_OF_UNDYING), "so does a respawn");
        check(BossGifts.deliver(player) == 0, "and there is nothing left to deliver");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "boss_gifts_saved")
    public static void theGiftsAndTheRestAreSavedWithTheWorldNotWithTheFight(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        BossGifts.add(server, player.getUUID(), new ItemStack(Items.NETHER_STAR, 1));
        BossFights.rest(server, BossKind.APOCALYPSE, 5000L);
        var data = server.overworld().getData(com.kolodkingal.enhanced.registry.ModAttachments.BOSS_DATA);
        check(data.giftsOf(player.getUUID()).size() == 1 && data.restUntil(BossKind.APOCALYPSE) == 5000L, "the overworld holds them");
        check(data.withoutGifts(player.getUUID()).giftsOf(player.getUUID()).isEmpty() && data.withoutGifts(player.getUUID()).restUntil(BossKind.APOCALYPSE) == 5000L,
                "taking the gifts leaves the rest");
        check(data.giftsOf(player.getUUID()).size() == 1, "and the data never change under a holder");
        BossFights.stopAll(server, StopReason.SERVER_STOPPING);
        helper.succeed();
    }

    private BossRewardTests() {
    }
}
