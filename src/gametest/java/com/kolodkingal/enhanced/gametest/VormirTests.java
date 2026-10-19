package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.config.SoulStoneConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneProgress;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.StonePowers;
import com.kolodkingal.enhanced.vormir.Sacrifice;
import com.kolodkingal.enhanced.vormir.Stonekeeper;
import com.kolodkingal.enhanced.vormir.StonekeeperEntity;
import com.kolodkingal.enhanced.vormir.VormirKeeper;
import com.kolodkingal.enhanced.vormir.VormirSites;
import com.kolodkingal.enhanced.vormir.VormirTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The Soul Stone's sacrifice at the Vormir Cliff (3.6, lane L5; the plan's 6 and 10). A small cliff in each arena
 * ({@link VormirTestSite}): the victim stands on its edge, is pushed (a hit, a power's shove, a telekinetic drop), is
 * over the chasm and lands from its height. The full sacrifice (a Thor seeker, a friend with PvP on: his items gone
 * even under {@code keepInventory}, his ender chest kept, his power gone; the seeker in the pool with the stone) and
 * the seeker's own wolf; every accident of 6.5 is an ordinary death: a solo fall, a creature's push, another cliff, PvP
 * off, a totem, a fall survived, a Creative seeker, a non-Bearer, someone else's dog, one who has his stone already,
 * the world's stone taken, the push too old, a logout in the air; the configs ({@code destroyItems}, {@code
 * playerSacrifice}, {@code friendRule}, {@code onePer}) each in a batch of its own; the Stonekeeper's lines and that
 * nothing moves him.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class VormirTests {
    /** Tests with PvP off (the server's default) and the defaults. */
    private static final String BATCH = "vormir";
    /** Tests with PvP on. */
    private static final String PVP = "vormir_pvp";

    // ---- the steps ----

    /**
     * The victim stands on the edge now; at tick 2 the push (null: none) and he is in the air over the chasm; at tick 4
     * he lands from the chasm's height, then {@code after} checks and the test ends.
     */
    private static void fall(TestKit.Fixture f, VormirTestSite cliff, LivingEntity victim, Runnable push,
            Runnable after) {
        cliff.standOnEdge(victim, 3);
        f.everyTick(12, tick -> {
            if (tick == 2) {
                check(VormirTracker.onEdge(victim), "the tracker saw him standing on the edge");
                if (push != null) {
                    push.run();
                }
                cliff.overThePit(victim, 2.0);
            }
            if (tick == 4) {
                check(cliff.departed(victim), "the tracker saw him leave the edge");
                VormirTestSite.fallDie(victim);
                after.run();
                return true;
            }
            return false;
        });
    }

    /** The seeker's hit (melee). */
    private static boolean hit(ServerPlayer seeker, LivingEntity victim) {
        return victim.hurt(seeker.damageSources().playerAttack(seeker), 1.0F);
    }

    private static boolean hasStone(ServerPlayer player) {
        return player.getInventory().contains(new ItemStack(ModItems.SOUL_STONE.get()));
    }

    private static ServerPlayer friend(TestKit.Fixture f, Power power) {
        ServerPlayer friend = f.player(14.5, TestKit.FLOOR, 14.5, power);
        friend.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        friend.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.APPLE, 5));
        friend.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        return friend;
    }

    /** What an ordinary death leaves: his things lie in the chasm, no stone for the seeker, nothing said. */
    private static void ordinary(VormirTestSite cliff, ServerPlayer seeker, ServerPlayer victim, Power power,
            String why) {
        check(victim.isDeadOrDying(), why + ": he died");
        check(!cliff.items().isEmpty(), why + ": his things fell with him (an ordinary death)");
        check(Powers.power(victim) == power, why + ": he keeps his power");
        check(!hasStone(seeker) && !seeker.getData(ModAttachments.STONES).has(StoneProgress.SACRIFICED_FOR),
                why + ": the seeker gets no stone");
        check(!Sacrifice.awaitsRespawn(victim.getUUID()), why + ": he was not given");
    }

    private static ServerPlayer thor(TestKit.Fixture f) {
        return f.player(5.5, 6.0, 12.5, Power.THOR);
    }

    // ---- the sacrifice ----

    /**
     * The full sacrifice (6.1, 6.2): with PvP on, Thor pushes a friend (Doctor Strange, armed, with experience and an
     * emerald in his ender chest) off the edge; he dies in the chasm: nothing drops (no items, no experience), his
     * power is gone; Thor is in the pool holding the Soul Stone, has his progress, hears "A soul for a soul."; the
     * friend respawns with nothing and his ender chest, and is told.
     */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void thorPushesAFriendOffTheEdgeAndWakesWithTheSoulStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.DOCTOR_STRANGE);
        friend.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        friend.getEnderChestInventory().addItem(new ItemStack(Items.EMERALD, 2));
        friend.giveExperienceLevels(7);
        MinecraftServer server = helper.getLevel().getServer();
        fall(f, cliff, friend, () -> check(hit(thor, friend), "Thor's blow lands (PvP on)"), () -> {
            check(friend.isDeadOrDying(), "the friend died in the chasm");
            check(friend.getInventory().isEmpty(), "every item of his is gone");
            check(cliff.items().isEmpty(), "nothing fell with him");
            check(helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, AABB.of(cliff.site().bounds())).isEmpty()
                    && friend.experienceLevel == 0, "and his experience is lost, not dropped");
            check(Powers.power(friend) == Power.NONE, "his power is gone");
            check(friend.getData(ModAttachments.STONES).has(StoneProgress.WAS_SACRIFICED), "he was given");
            check(thor.getMainHandItem().is(ModItems.SOUL_STONE.get()), "Thor holds the Soul Stone");
            check(thor.position().distanceTo(cliff.pool()) < 0.6, "Thor wakes in the pool");
            StoneProgress progress = thor.getData(ModAttachments.STONES);
            check(progress.has(StoneProgress.SACRIFICED_FOR) && progress.hasTaken(StoneKind.SOUL),
                    "his progress: a soul given, the Soul Stone taken");
            check(VormirTestSite.keeperLines(thor).contains("message.enhanced.vormir.soul_for_soul"),
                    "the Stonekeeper says \"A soul for a soul.\"");
            ServerPlayer reborn = server.getPlayerList().respawn(friend, false, Entity.RemovalReason.KILLED);
            f.onClose(() -> server.getPlayerList().remove(reborn));
            check(reborn.getInventory().isEmpty() && Powers.power(reborn) == Power.NONE,
                    "he respawns with nothing and no power");
            check(reborn.getEnderChestInventory().countItem(Items.EMERALD) == 2, "his ender chest is untouched");
            check(messages(reborn).contains("message.enhanced.vormir.given") && !Sacrifice.awaitsRespawn(
                    reborn.getUUID()), "and he is told what happened");
        });
    }

    /** The translation keys of the chat lines (not the action bar) the player was sent since the last look. */
    private static List<String> messages(ServerPlayer player) {
        return TestKit.sentPackets(player).stream()
                .filter(p -> p instanceof ClientboundSystemChatPacket chat && !chat.overlay()
                        && chat.content().getContents() instanceof TranslatableContents)
                .map(p -> ((TranslatableContents) ((ClientboundSystemChatPacket) p).content().getContents()).getKey())
                .toList();
    }

    /**
     * Under {@code keepInventory} the friend still loses every item and his experience (the user's decision), and the
     * ender chest is still untouched.
     */
    @GameTest(template = ARENA, batch = "vormir_keep", timeoutTicks = 60)
    public static void keepInventoryStillLosesEverything(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        MinecraftServer server = helper.getLevel().getServer();
        GameRules.BooleanValue keep = server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        boolean was = keep.get();
        keep.set(true, server);
        f.onClose(() -> keep.set(was, server));
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        friend.getEnderChestInventory().addItem(new ItemStack(Items.EMERALD));
        friend.giveExperienceLevels(4);
        fall(f, cliff, friend, () -> hit(thor, friend), () -> {
            check(hasStone(thor), "Thor gets the stone");
            ServerPlayer reborn = server.getPlayerList().respawn(friend, false, Entity.RemovalReason.KILLED);
            f.onClose(() -> server.getPlayerList().remove(reborn));
            check(reborn.getInventory().isEmpty(), "keepInventory keeps nothing of a sacrifice");
            check(reborn.experienceLevel == 0, "nor his experience");
            check(reborn.getEnderChestInventory().countItem(Items.EMERALD) == 1, "the ender chest is untouched");
            check(Powers.power(reborn) == Power.NONE, "his power is gone");
        });
    }

    /** His own tamed wolf pushed off the edge: the stone; the wolf drops nothing and its soul rises to him. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void hisOwnWolfIsASacrifice(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(16, 2, 16));
        wolf.tame(thor);
        wolf.setOrderedToSit(true);
        fall(f, cliff, wolf, () -> check(hit(thor, wolf), "Thor's blow reaches his wolf"), () -> {
            check(!wolf.isAlive(), "the wolf died in the chasm");
            check(hasStone(thor) && thor.position().distanceTo(cliff.pool()) < 0.6,
                    "Thor wakes in the pool with the stone");
            check(cliff.items().isEmpty() && helper.getLevel().getEntitiesOfClass(ExperienceOrb.class,
                    AABB.of(cliff.site().bounds())).isEmpty(), "the wolf leaves nothing");
        });
    }

    /** Someone else's dog pushed off: not his to give: an ordinary death, and the keeper says so. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void someoneElsesDogIsNotHisToGive(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer owner = f.player(16.5, TestKit.FLOOR, 4.5);
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(16, 2, 16));
        wolf.tame(owner);
        wolf.setOrderedToSit(true);
        fall(f, cliff, wolf, () -> hit(thor, wolf), () -> {
            check(!wolf.isAlive(), "the dog died");
            check(!hasStone(thor), "Thor gets nothing");
            check(VormirTestSite.keeperLines(thor).contains("npc.enhanced.stonekeeper.not_loved"),
                    "the keeper: that soul was never his to give");
        });
    }

    // ---- the accidents (6.5): each an ordinary death ----

    /** A friend jumps off the edge by himself: nobody pushed him. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aSoloFallFromTheEdgeGivesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        fall(f, cliff, friend, null, () -> {
            ordinary(cliff, thor, friend, Power.SPIDER_MAN, "a solo fall");
            check(VormirTestSite.keeperLines(thor).isEmpty(), "the keeper says nothing");
        });
    }

    /** Thor hits his friend, then a zombie's blow throws him off: the last push is the zombie's. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aMobsPushGivesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(16, 2, 16));
        fall(f, cliff, friend, () -> {
            check(hit(thor, friend), "Thor hits him first");
            friend.invulnerableTime = 0;
            check(friend.hurt(helper.getLevel().damageSources().mobAttack(zombie), 1.0F), "then the zombie's blow");
        }, () -> ordinary(cliff, thor, friend, Power.SPIDER_MAN, "a zombie's push"));
    }

    /**
     * Another cliff: pushed off a ledge that is not the edge he dies in the chasm (no departure from the edge); pushed
     * off the edge he dies outside the chasm. Neither counts.
     */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aFallFromAnotherCliffGivesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer first = friend(f, Power.SPIDER_MAN);
        ServerPlayer second = friend(f, Power.SPIDER_MAN);
        // The first stands on a pillar elsewhere (another cliff); the second on the edge.
        helper.setBlock(new BlockPos(15, 5, 15), net.minecraft.world.level.block.Blocks.STONE);
        Vec3 pillar = helper.absoluteVec(new Vec3(15.5, 6.0, 15.5));
        first.teleportTo(pillar.x, pillar.y, pillar.z);
        first.setOnGround(true);
        cliff.standOnEdge(second, 1);
        f.everyTick(12, tick -> {
            if (tick == 2) {
                hit(thor, first);
                hit(thor, second);
                cliff.overThePit(first, 2.0);
                Vec3 away = helper.absoluteVec(new Vec3(15.5, 2.0, 4.5));
                second.teleportTo(away.x, away.y, away.z);
                second.setOnGround(false);
            }
            if (tick == 4) {
                check(VormirTracker.departure(first) == null, "the pillar is no edge");
                check(cliff.departed(second), "the second left the edge");
                VormirTestSite.fallDie(first);
                VormirTestSite.fallDie(second);
                check(first.isDeadOrDying() && second.isDeadOrDying(), "both died of the fall");
                check(Powers.power(first) == Power.SPIDER_MAN && Powers.power(second) == Power.SPIDER_MAN,
                        "both keep their power");
                check(!hasStone(thor), "Thor gets nothing");
                return true;
            }
            return false;
        });
    }

    /** With PvP off a friend cannot be pushed: the blow does not land; his fall is an ordinary death. */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 60)
    public static void withPvpOffAFriendCannotBePushed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        fall(f, cliff, friend, () -> check(!hit(thor, friend), "PvP off: Thor's blow does not land"),
                () -> ordinary(cliff, thor, friend, Power.SPIDER_MAN, "PvP off"));
    }

    /** The friend holds a totem of undying: he lives; no stone, nothing lost. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aTotemSavesTheFriendAndGivesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        friend.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        fall(f, cliff, friend, () -> hit(thor, friend), () -> {
            check(friend.isAlive() && !friend.isDeadOrDying(), "the totem saves him");
            check(!hasStone(thor), "no death, no stone");
            check(Powers.power(friend) == Power.SPIDER_MAN && friend.getInventory().countItem(Items.DIAMOND) == 3,
                    "he keeps his power and his things");
        });
    }

    /** The friend survives the fall (slow falling, Feather Falling: a fall that does not kill): nothing happens. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aFriendWhoSurvivesTheFallGivesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        cliff.standOnEdge(friend, 3);
        f.everyTick(12, tick -> {
            if (tick == 2) {
                hit(thor, friend);
                cliff.overThePit(friend, 2.0);
            }
            if (tick == 4) {
                friend.fallDistance = 40.0F;
                friend.invulnerableTime = 0;
                friend.hurt(friend.damageSources().fall(), 2.0F);
                check(friend.isAlive(), "a gentle landing: he lives");
                check(!hasStone(thor) && Powers.power(friend) == Power.SPIDER_MAN, "no stone, nothing lost");
                return true;
            }
            return false;
        });
    }

    /** A Creative seeker never counts. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aCreativeSeekerGetsNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer seeker = f.player(5.5, 6.0, 12.5, GameType.CREATIVE);
        Powers.setPower(seeker, Power.THOR);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        fall(f, cliff, friend, () -> hit(seeker, friend), () -> {
            ordinary(cliff, seeker, friend, Power.SPIDER_MAN, "a Creative seeker");
            check(VormirTestSite.keeperLines(seeker).contains("npc.enhanced.stonekeeper.creative"),
                    "the keeper tells him why");
        });
    }

    /** A seeker who is no Bearer pushes his friend: an ordinary death, and the keeper had said so. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aNonBearersPushIsAnOrdinaryDeath(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer seeker = f.player(5.5, 6.0, 12.5, Power.SPIDER_MAN);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        fall(f, cliff, friend, () -> hit(seeker, friend), () -> {
            ordinary(cliff, seeker, friend, Power.SPIDER_MAN, "a non-Bearer's push");
            check(VormirTestSite.keeperLines(seeker).contains("npc.enhanced.stonekeeper.not_bearer"),
                    "the keeper: go home");
        });
    }

    /** {@code onePer = player}: one who has had his Soul Stone gets no second one. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void oneWhoHasHisStoneGetsNoOther(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        thor.setData(ModAttachments.STONES, thor.getData(ModAttachments.STONES).withTaken(StoneKind.SOUL, true));
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        fall(f, cliff, friend, () -> hit(thor, friend), () -> {
            ordinary(cliff, thor, friend, Power.SPIDER_MAN, "he has one");
            check(VormirTestSite.keeperLines(thor).contains("npc.enhanced.stonekeeper.has_one"),
                    "the keeper: you already carry a soul");
        });
    }

    /** The push must come within 100 ticks before he leaves the edge: 101 ticks is too old. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 160)
    public static void aPushOlderThanTheWindowGivesNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        cliff.standOnEdge(friend, 3);
        long[] pushed = new long[1];
        f.everyTick(140, tick -> {
            long now = helper.getLevel().getGameTime();
            if (tick == 2) {
                hit(thor, friend);
                pushed[0] = now;
            }
            if (tick > 2 && now == pushed[0] + 100) {
                // (He leaves the edge this tick: the tracker sees it at its end, 101 ticks after the push.)
                cliff.overThePit(friend, 2.0);
            }
            if (tick > 2 && now == pushed[0] + 103) {
                VormirTracker.Departure gone = VormirTracker.departure(friend);
                check(gone != null && gone.credit() == null, "he left the edge with no push to his name");
                VormirTestSite.fallDie(friend);
                ordinary(cliff, thor, friend, Power.SPIDER_MAN, "a push 101 ticks old");
                return true;
            }
            return false;
        });
    }

    /** One who logs out in the air is forgotten: nothing is kept of him. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aLogoutInTheAirForgetsHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        cliff.standOnEdge(friend, 3);
        f.everyTick(12, tick -> {
            if (tick == 2) {
                hit(thor, friend);
                cliff.overThePit(friend, 2.0);
            }
            if (tick == 4) {
                check(cliff.departed(friend), "he left the edge, pushed");
                helper.getLevel().getServer().getPlayerList().remove(friend);
                check(VormirTracker.departure(friend) == null && VormirTracker.lastCredit(friend) == null,
                        "his logout forgets him");
                check(!hasStone(thor), "and gives nothing");
                return true;
            }
            return false;
        });
    }

    // ---- the pushes ----

    /** Wanda (a Bearer) lifts her friend off the edge with telekinesis and lets go over the chasm: her push. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 60)
    public static void aTelekineticDropIsAPush(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.onClose(Telekinesis::clear);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer wanda = f.player(5.5, 6.0, 12.5, Power.SCARLET_WITCH);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        cliff.standOnEdge(friend, 3);
        f.everyTick(14, tick -> {
            if (tick == 2) {
                lookAt(wanda, friend);
                AbilityHandler.onKey(wanda, 3, true);
                AbilityHandler.onKey(wanda, 3, false);
                check(Telekinesis.holds(wanda, friend), "Wanda's grip takes him");
                cliff.overThePit(friend, 2.0);
            }
            if (tick == 4) {
                check(cliff.departed(friend), "carried off the edge");
                VormirTracker.Departure carried = VormirTracker.departure(friend);
                check(carried.credit() == null, "no push yet: he is only carried");
                wanda.setShiftKeyDown(true);
                AbilityHandler.onKey(wanda, 3, true);
                AbilityHandler.onKey(wanda, 3, false);
                wanda.setShiftKeyDown(false);
                check(!Telekinesis.isHolding(wanda), "she lets go");
                VormirTracker.Departure dropped = VormirTracker.departure(friend);
                check(dropped != null && dropped.credit() != null && wanda.getUUID().equals(dropped.credit().pusher()),
                        "letting go over the chasm is her push");
                cliff.overThePit(friend, 1.0); // (where she held him: over the chasm)
                VormirTestSite.fallDie(friend);
                check(hasStone(wanda) && Powers.power(friend) == Power.NONE, "she is given the stone for him");
                return true;
            }
            return false;
        });
    }

    /**
     * The shared push helper ({@code StonePowers.shove} with its caster) credits a player; a creature's shove takes the
     * credit away; none changes nothing; his pet's bite and his arrow are his.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 20)
    public static void theSharedPushesAreCredited(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(4.5, TestKit.FLOOR, 4.5, Power.THOR);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(10, 2, 10));
        Zombie other = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(14, 2, 14));
        StonePowers.shove(target, thor.position(), 1.0, 0.2, thor);
        check(VormirTracker.lastCredit(target) != null
                && thor.getUUID().equals(VormirTracker.lastCredit(target).pusher()),
                "Thor's shove is his push");
        StonePowers.shove(target, other.position(), 1.0, 0.2, null);
        check(thor.getUUID().equals(VormirTracker.lastCredit(target).pusher()), "a shove of no one changes nothing");
        StonePowers.shove(target, other.position(), 1.0, 0.2, other);
        check(VormirTracker.lastCredit(target).pusher() == null, "a zombie's shove takes the credit away");
        Wolf wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(6, 2, 6));
        wolf.tame(thor);
        VormirTracker.credit(target, wolf);
        check(thor.getUUID().equals(VormirTracker.lastCredit(target).pusher()), "his wolf's push is his");
        net.minecraft.world.entity.projectile.Arrow arrow = EntityType.ARROW.create(helper.getLevel());
        arrow.setOwner(thor);
        VormirTracker.credit(target, other);
        VormirTracker.credit(target, arrow);
        check(thor.getUUID().equals(VormirTracker.lastCredit(target).pusher()), "his arrow's push is his");
        VormirTracker.forget(target.getUUID());
        helper.succeed();
    }

    // ---- the configs (each its own batch) ----

    /** {@code destroyItems = false}: his things drop as in any death (his power still goes). */
    @GameTest(template = ARENA, batch = "vormir_cfg_items", timeoutTicks = 60)
    public static void withDestroyItemsOffHisThingsDrop(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(SoulStoneConfig.DESTROY_ITEMS, false);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        fall(f, cliff, friend, () -> hit(thor, friend), () -> {
            check(hasStone(thor), "Thor gets the stone");
            check(!cliff.items().isEmpty(), "the friend's things drop");
            check(Powers.power(friend) == Power.NONE, "his power still goes");
        });
    }

    /** {@code playerSacrifice = false}: a friend is no sacrifice (only a pet); the keeper says so. */
    @GameTest(template = ARENA, batch = "vormir_cfg_players", timeoutTicks = 60)
    public static void withPlayerSacrificeOffAFriendIsNoSacrifice(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(SoulStoneConfig.PLAYER_SACRIFICE, false);
        VormirTestSite cliff = VormirTestSite.build(f);
        ServerPlayer thor = thor(f);
        ServerPlayer friend = friend(f, Power.SPIDER_MAN);
        check(Stonekeeper.lines(thor).get(2).equals("npc.enhanced.stonekeeper.pets_only"),
                "the keeper's third line: only a creature you love");
        fall(f, cliff, friend, () -> hit(thor, friend), () -> {
            ordinary(cliff, thor, friend, Power.SPIDER_MAN, "playerSacrifice off");
            check(VormirTestSite.keeperLines(thor).contains("npc.enhanced.stonekeeper.pets_only"),
                    "and he says it again");
        });
    }

    /** {@code friendRule = team}: a player of no team is not his friend; a team mate is. */
    @GameTest(template = ARENA, batch = "vormir_cfg_team", timeoutTicks = 60)
    public static void withTheTeamRuleOnlyATeamMateCounts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(SoulStoneConfig.FRIEND_RULE, SoulStoneConfig.FriendRule.TEAM);
        VormirTestSite cliff = VormirTestSite.build(f);
        VormirTestSite second = VormirTestSite.build(f, 11);
        ServerPlayer thor = thor(f);
        ServerPlayer stranger = friend(f, Power.SPIDER_MAN);
        ServerPlayer mate = friend(f, Power.SPIDER_MAN);
        Scoreboard board = helper.getLevel().getScoreboard();
        PlayerTeam team = board.addPlayerTeam("vormir_test_team");
        f.onClose(() -> board.removePlayerTeam(team));
        board.addPlayerToTeam(thor.getScoreboardName(), team);
        board.addPlayerToTeam(mate.getScoreboardName(), team);
        cliff.standOnEdge(stranger, 3);
        second.standOnEdge(mate, 3);
        f.everyTick(12, tick -> {
            if (tick == 2) {
                check(hit(thor, stranger) && hit(thor, mate), "both blows land (friendly fire is on)");
                cliff.overThePit(stranger, 2.0);
                second.overThePit(mate, 2.0);
            }
            if (tick == 4) {
                VormirTestSite.fallDie(stranger);
                check(!hasStone(thor) && Powers.power(stranger) == Power.SPIDER_MAN, "a stranger is no friend");
                check(VormirTestSite.keeperLines(thor).contains("npc.enhanced.stonekeeper.not_loved"),
                        "the keeper: never yours to give");
                VormirTestSite.fallDie(mate);
                check(hasStone(thor) && Powers.power(mate) == Power.NONE, "a team mate is");
                return true;
            }
            return false;
        });
    }

    /**
     * {@code onePer = world}: the first sacrifice takes the world's stone (the stones' ledger names its holder); at a
     * second cliff another gets nothing and the keeper says it has been taken. The cliff follows the ledger: with a
     * ledger that holds nothing (the 3.6 skeleton's, before lane L1's is merged) the second is given his own.
     */
    @GameTest(template = ARENA, batch = "vormir_cfg_world", timeoutTicks = 60)
    public static void inWorldModeTheSecondCliffHasNoStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(SoulStoneConfig.ONE_PER, SoulStoneConfig.OnePer.WORLD);
        VormirTestSite cliff = VormirTestSite.build(f);
        VormirTestSite second = VormirTestSite.build(f, 11);
        ServerPlayer thor = thor(f);
        ServerPlayer loki = f.player(16.5, 6.0, 12.5, Power.LOKI);
        ServerPlayer first = friend(f, Power.SPIDER_MAN);
        ServerPlayer other = friend(f, Power.SPIDER_MAN);
        check(Stonekeeper.lines(loki).get(2).equals(Stonekeeper.CHOOSE), "before: the keeper asks Loki to choose");
        cliff.standOnEdge(first, 3);
        second.standOnEdge(other, 3);
        f.everyTick(12, tick -> {
            if (tick == 2) {
                hit(thor, first);
                hit(loki, other);
                cliff.overThePit(first, 2.0);
                second.overThePit(other, 2.0);
            }
            if (tick == 4) {
                VormirTestSite.fallDie(first);
                check(hasStone(thor), "Thor takes the world's stone");
                if (StoneLedger.mayTake(loki, StoneKind.SOUL)) {
                    // (A ledger that holds nothing: the second cliff gives too.)
                    VormirTestSite.fallDie(other);
                    check(hasStone(loki), "with no holder in the ledger, the second cliff gives its stone");
                    return true;
                }
                VormirTestSite.fallDie(other);
                check(!hasStone(loki) && Powers.power(other) == Power.SPIDER_MAN, "at the second cliff: nothing");
                check(VormirTestSite.keeperLines(loki).contains("npc.enhanced.stonekeeper.taken"),
                        "the keeper: the stone has been taken");
                check(Stonekeeper.lines(loki).get(2).equals("npc.enhanced.stonekeeper.taken"),
                        "and that is his third line now");
                return true;
            }
            return false;
        });
    }

    // ---- the Stonekeeper ----

    /** His three lines, the third for who asks; a right-click starts them (one every 40 ticks) and he is met. */
    @GameTest(template = ARENA, batch = PVP, timeoutTicks = 140)
    public static void theStonekeeperSaysHisLines(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        VormirTestSite cliff = VormirTestSite.build(f);
        StonekeeperEntity keeper = VormirKeeper.ensure(helper.getLevel(), cliff.site());
        ServerPlayer thor = f.player(5.5, TestKit.FLOOR, 16.5, Power.THOR);
        ServerPlayer plain = f.player(4.5, TestKit.FLOOR, 16.5);
        ServerPlayer creative = f.player(6.5, TestKit.FLOOR, 16.5, GameType.CREATIVE);
        ServerPlayer had = f.player(3.5, TestKit.FLOOR, 16.5, Power.ODIN);
        had.setData(ModAttachments.STONES, had.getData(ModAttachments.STONES).withTaken(StoneKind.SOUL, true));
        check(Stonekeeper.lines(thor).equals(List.of(Stonekeeper.WELCOME, Stonekeeper.PRICE, Stonekeeper.CHOOSE)),
                "to Thor: welcome, the price, choose well");
        check(Stonekeeper.lines(plain).get(2).equals("npc.enhanced.stonekeeper.not_bearer"), "no Bearer: go home");
        check(Stonekeeper.lines(creative).get(2).equals("npc.enhanced.stonekeeper.creative"), "Creative");
        check(Stonekeeper.lines(had).get(2).equals("npc.enhanced.stonekeeper.has_one"), "one who has his stone");
        VormirTestSite.keeperLines(thor);
        keeper.interact(thor, InteractionHand.MAIN_HAND);
        check(keeper.speaksTo(thor), "a right-click: he speaks");
        check(thor.getData(ModAttachments.STONES).has(StoneProgress.MET_STONEKEEPER), "Thor has met him");
        keeper.interact(thor, InteractionHand.MAIN_HAND);
        f.everyTick(130, tick -> {
            if (tick < 125) {
                return false;
            }
            check(!keeper.speaksTo(thor), "the speech is over");
            check(VormirTestSite.keeperLines(thor).equals(List.of(Stonekeeper.WELCOME, Stonekeeper.PRICE,
                    Stonekeeper.CHOOSE)), "Thor heard the three lines once, in order");
            return true;
        });
    }

    /**
     * Nothing moves or hurts him: a blow, a push, a knockback, a shove, gravity; he floats over his place; {@code
     * /kill} reaches him, and a missing keeper is put back, once. His tags keep the powers off him.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 40)
    public static void theStonekeeperCannotBeHurtOrPushed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        VormirTestSite cliff = VormirTestSite.build(f);
        StonekeeperEntity keeper = VormirKeeper.ensure(helper.getLevel(), cliff.site());
        check(keeper != null && VormirKeeper.ensure(helper.getLevel(), cliff.site()) == keeper, "one keeper, not two");
        ServerPlayer thor = f.player(5.5, TestKit.FLOOR, 16.5, Power.THOR);
        Vec3 place = Vec3.atBottomCenterOf(cliff.site().keeperHome()).add(0.0, StonekeeperEntity.HOVER, 0.0);
        check(keeper.position().distanceTo(place) < 1.0E-3, "he floats over his place");
        check(!keeper.hurt(thor.damageSources().playerAttack(thor), 100.0F)
                && keeper.getHealth() == keeper.getMaxHealth(), "a blow does nothing");
        keeper.push(2.0, 1.0, 2.0);
        keeper.knockback(3.0, 1.0, 0.0);
        StonePowers.shove(keeper, thor.position(), 3.0, 1.0, thor);
        check(!keeper.isPushable(), "he cannot be pushed");
        for (var tag : List.of(ModTags.PULL_IMMUNE, ModTags.TELEKINESIS_IMMUNE, ModTags.CONFUSED_IMMUNE,
                ModTags.WARD_IMMUNE)) {
            check(keeper.getType().is(tag), "his type is in " + tag.location());
        }
        f.everyTick(30, tick -> {
            if (tick < 10) {
                return false;
            }
            check(keeper.position().distanceTo(place) < 1.0E-3, "ten ticks later he is still at his place");
            keeper.kill();
            check(!keeper.isAlive(), "/kill reaches him");
            StonekeeperEntity next = VormirKeeper.ensure(helper.getLevel(), cliff.site());
            check(next != null && next != keeper && next.isAlive() && next.getType() == ModEntities.STONEKEEPER.get(),
                    "a missing keeper is put back");
            check(VormirSites.at(helper.getLevel(), BlockPos.containing(place)) == cliff.site(),
                    "his place is in the cliff's site");
            return true;
        });
    }
}
