package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.asgard.FeastHall;
import com.kolodkingal.enhanced.block.asgard.AsgardBenchBlock;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.AsgardPopulation;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.asgard.npc.AsgardTrades;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Asgardians;
import com.kolodkingal.enhanced.asgard.npc.DefendAsgardGoal;
import com.kolodkingal.enhanced.asgard.npc.NpcState;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.config.AsgardConfig;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Asgardians (3.5, lane L5): never killed by a player, knocked down by others and rising; the keeper (each slot once,
 * no duplicates, a lost one made again only where its home is loaded); the dialogue (the node for each stage of a
 * player's story, the reactions to his power, answers checked: not offered or too far is refused); the trades and their
 * restock; the warriors' choice of foe; the summoned Einherjar; Odin's command and the bow; every lang key of the table in
 * EN and HE. Batches {@code asgard_npc}, {@code asgard_npc_fight}, {@code asgard_npc_closed} (config) and the two
 * command batches (Odin's command reaches 64 blocks).
 */
public final class AsgardianTests {
    static final String BATCH = "asgard_npc";
    static final String FIGHT = "asgard_npc_fight";

    private AsgardianTests() {
    }

    /** An Asgardian of {@code role} standing in the arena at (x, z). */
    static AsgardianEntity npc(TestKit.Fixture fx, Role role, double x, double z) {
        ServerLevel level = fx.level();
        AsgardianEntity npc = ModEntities.ASGARDIAN.get().create(level);
        npc.setRole(role, 0);
        Vec3 at = fx.helper().absoluteVec(new Vec3(x, FLOOR, z));
        npc.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        check(level.addFreshEntity(npc), "the Asgardian joins");
        fx.onClose(npc::discard);
        return npc;
    }

    static <T extends Entity> T mob(TestKit.Fixture fx, EntityType<T> type, double x, double z) {
        T e = type.create(fx.level());
        Vec3 at = fx.helper().absoluteVec(new Vec3(x, FLOOR, z));
        e.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        fx.level().addFreshEntity(e);
        fx.onClose(e::discard);
        return e;
    }

    // ---- never killed by a player; down and rising ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aPlayerNeverHurtsAnAsgardian(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer player = fx.player(5.5, FLOOR, 5.5);
        AsgardianEntity sif = npc(fx, Role.SIF, 7.5, 5.5);
        float full = sif.getHealth();
        check(!sif.hurt(fx.level().damageSources().playerAttack(player), 30.0F), "a blow of a player is refused");
        Arrow arrow = new Arrow(fx.level(), player, new ItemStack(Items.ARROW), null);
        check(!sif.hurt(fx.level().damageSources().arrow(arrow, player), 30.0F), "and his arrow");
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        check(!sif.hurt(fx.level().damageSources().playerAttack(player), 1000.0F), "even in Creative");
        check(sif.getHealth() == full && sif.isAlive() && !sif.isDown(), "she is whole: " + sif.getHealth());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = FIGHT)
    public static void othersKnockThemDownAndTheyRise(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        AsgardianEntity einherjar = npc(fx, Role.EINHERJAR, 5.5, 5.5);
        Zombie zombie = mob(fx, EntityType.ZOMBIE, 9.5, 9.5);
        check(einherjar.hurt(fx.level().damageSources().mobAttack(zombie), 6.0F), "a monster's blow lands");
        check(einherjar.getHealth() < einherjar.getMaxHealth(), "and hurts");
        einherjar.hurt(fx.level().damageSources().mobAttack(zombie), 500.0F);
        check(einherjar.isAlive() && einherjar.isDown() && einherjar.getHealth() == 1.0F, "a deadly blow knocks him down, alive");
        check(einherjar.downUntil() - fx.level().getGameTime() >= AsgardianEntity.DOWN_TICKS - 2, "for 20 seconds");
        check(!einherjar.hurt(fx.level().damageSources().mobAttack(zombie), 6.0F), "down, nothing touches him");
        einherjar.setDownUntil(fx.level().getGameTime());
        helper.runAfterDelay(2, () -> {
            check(!einherjar.isDown() && einherjar.state() == NpcState.IDLE, "he rises");
            check(Math.abs(einherjar.getHealth() - einherjar.getMaxHealth() * 0.5F) < 0.01F, "at half his health");
            // Only the void (or /kill) truly ends an Asgardian: the keeper makes him anew.
            einherjar.hurt(fx.level().damageSources().fellOutOfWorld(), 1000.0F);
            check(!einherjar.isAlive(), "the void kills");
            helper.succeed();
        });
    }

    // ---- the keeper ----

    /** Each running test's own records (the tests of a batch run side by side: each keeps its book to itself). */
    private static final Map<TestKit.Fixture, AsgardPopulation.Residents> BOOKS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** Two homes in the arena and a test's own records. */
    static Map<String, UUID> records(TestKit.Fixture fx) {
        Map<String, UUID> table = new HashMap<>();
        AsgardPopulation.Residents book = new AsgardPopulation.Residents() {
            @Override
            public UUID resident(String slot) {
                return table.get(slot);
            }

            @Override
            public void setResident(String slot, UUID id) {
                if (id == null) {
                    table.remove(slot);
                } else {
                    table.put(slot, id);
                }
            }
        };
        BOOKS.put(fx, book);
        AsgardPopulation.useResidents(fx.level(), book);
        fx.onClose(() -> {
            BOOKS.remove(fx);
            if (AsgardPopulation.residents(fx.level()) == book) {
                AsgardPopulation.useResidents(fx.level(), null);
            }
            for (UUID id : table.values()) {
                Entity e = fx.level().getEntity(id);
                if (e != null) {
                    e.discard();
                }
            }
        });
        return table;
    }

    /** The test's own records (not the level's hook, which another test of the batch may have set since). */
    static AsgardPopulation.Residents residents(TestKit.Fixture fx) {
        return BOOKS.get(fx);
    }

    /** Whether the entities round a home are loaded, as the keeper asks (its {@code AREA} chunks each way). */
    /**
     * How many ticks a test waits for the keeper's area to load (the test server runs its ticks as fast as it can, and
     * in a big run the chunks of 5 x 5 areas are made and their entities read behind many others: 80 ticks were not
     * always enough). A test ends as soon as the area is there.
     */
    static final int AREA_WAIT = 600;

    /** Keeps the keeper's area round a home loaded for the test, as a player standing there would. */
    static void loadArea(TestKit.Fixture fx, BlockPos home) {
        fx.loadChunksAround(home, AsgardPopulation.AREA);
    }

    static boolean areaLoaded(ServerLevel level, BlockPos home) {
        net.minecraft.world.level.ChunkPos center = new net.minecraft.world.level.ChunkPos(home);
        for (int dx = -AsgardPopulation.AREA; dx <= AsgardPopulation.AREA; dx++) {
            for (int dz = -AsgardPopulation.AREA; dz <= AsgardPopulation.AREA; dz++) {
                if (!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(center.x + dx, center.z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = AREA_WAIT + 20)
    public static void theKeeperMakesEachSlotOnceAndNeverTwice(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        Map<String, UUID> table = records(fx);
        BlockPos floor = helper.absolutePos(new BlockPos(5, 2, 5));
        List<CityPlan.Home> homes = List.of(new CityPlan.Home(Role.FRIGGA, 0, floor, 4),
                new CityPlan.Home(Role.CITIZEN, 3, floor.east(6), 4));
        // The keeper makes nobody where the entities round a home are not loaded yet (a fresh arena loads them in a
        // tick or two): wait for them, as the keeper would.
        loadArea(fx, floor);
        loadArea(fx, floor.east(6));
        fx.everyTick(AREA_WAIT, tick -> {
            if (!areaLoaded(fx.level(), floor) || !areaLoaded(fx.level(), floor.east(6))) {
                return false;
            }
            AsgardPopulation.useResidents(fx.level(), residents(fx)); // (the joins ask the level's hook: this test's)
            List<AsgardianEntity> made = AsgardPopulation.keep(fx.level(), homes, residents(fx));
            check(made.size() == 2 && table.size() == 2, "both slots made: " + made.size());
            check(made.get(0).role() == Role.FRIGGA && "FRIGGA".equals(made.get(0).slot()), "Frigga in her slot");
            check(made.get(1).role() == Role.CITIZEN && made.get(1).variant() == 3 && "CITIZEN#3".equals(made.get(1).slot()),
                    "the citizen of slot 3 wears skin 3");
            check(AsgardPopulation.keep(fx.level(), homes, residents(fx)).isEmpty(), "a second beat makes nobody");
            // A copy of Frigga (her slot, another UUID) is refused as it joins.
            AsgardianEntity copy = ModEntities.ASGARDIAN.get().create(fx.level());
            copy.setRole(Role.FRIGGA, 0);
            copy.setHome("FRIGGA", floor, 4);
            copy.moveTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
            check(!fx.level().addFreshEntity(copy), "a duplicate of a slot is refused");
            // Gone: made again, once.
            made.get(1).discard();
            List<AsgardianEntity> again = AsgardPopulation.keep(fx.level(), homes, residents(fx));
            check(again.size() == 1 && again.get(0).role() == Role.CITIZEN && table.get("CITIZEN#3").equals(again.get(0).getUUID()),
                    "the missing citizen is made again and recorded");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theKeeperWaitsUntilAHomeIsLoaded(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        Map<String, UUID> table = records(fx);
        BlockPos far = new BlockPos(2_000_000, 70, 2_000_000);
        check(!fx.level().hasChunkAt(far), "(the far home is not loaded)");
        List<CityPlan.Home> homes = List.of(new CityPlan.Home(Role.SIF, 0, far, 10));
        check(AsgardPopulation.keep(fx.level(), homes, residents(fx)).isEmpty() && table.isEmpty(),
                "nobody is made where the home's area is not loaded");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "asgard_npc_closed", timeoutTicks = AREA_WAIT + 20)
    public static void withoutNpcsTheKeeperRemovesItsOwn(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        Map<String, UUID> table = records(fx);
        BlockPos floor = helper.absolutePos(new BlockPos(5, 2, 5));
        List<CityPlan.Home> homes = List.of(new CityPlan.Home(Role.HOGUN, 0, floor, 4));
        loadArea(fx, floor);
        fx.everyTick(AREA_WAIT, tick -> {
            if (!areaLoaded(fx.level(), floor)) {
                return false;
            }
            List<AsgardianEntity> made = AsgardPopulation.keep(fx.level(), homes, residents(fx));
            check(made.size() == 1, "Hogun made: " + made.size());
            AsgardianEntity hogun = made.get(0);
            fx.config(AsgardConfig.NPCS, false);
            check(AsgardPopulation.keep(fx.level(), homes, residents(fx)).isEmpty(), "none made");
            check(hogun.isRemoved() && table.isEmpty(), "and the keeper's own is gone");
            return true;
        });
    }

    // ---- the dialogue ----

    static AsgardDialogue.Ctx ctx(ServerPlayer player, AsgardianEntity npc) {
        return AsgardTalk.context(player, npc, npc.role());
    }

    static String picked(ServerPlayer player, AsgardianEntity npc) {
        AsgardDialogue.Node node = AsgardDialogue.pick(ctx(player, npc));
        return node == null ? null : node.key();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theNodeFollowsThePlayersStory(GameTestHelper helper) {
        TestKit.Fixture fx = AsgardTestSupport.fixture(helper);
        ServerPlayer player = fx.player(5.5, FLOOR, 5.5);
        AsgardianEntity sif = npc(fx, Role.SIF, 7.5, 5.5);
        AsgardianEntity frigga = npc(fx, Role.FRIGGA, 5.5, 7.5);
        AsgardianEntity odin = npc(fx, Role.ODIN, 3.5, 5.5);
        AsgardState state = AsgardState.get(fx.level().getServer());
        boolean awake = state.odinAwake();
        int defeats = state.helaDefeats();
        fx.onClose(() -> {
            state.setOdinAwake(awake);
            state.setHelaDefeats(defeats);
        });
        state.setOdinAwake(false);
        state.setHelaDefeats(0);
        // First visit, then the usual greeting.
        check("greeting_first".equals(picked(player, sif)), "Sif greets a newcomer: " + picked(player, sif));
        check(AsgardTalk.begin(player, sif), "the talk opens");
        check("greeting_first".equals(AsgardTalk.currentNode(player)), "with the first greeting");
        check(ModAttachments.ASGARD.get(player).has(AsgardProgress.TALKED_SIF), "and he has talked to Sif");
        check("greeting".equals(picked(player, sif)), "then the usual greeting");
        check(AsgardTalk.offered(player).containsAll(List.of("train", "trade", "ask_advice")), "training, trades, advice");
        AsgardTalk.end(player);
        // Odin asleep: Frigga's words; awake: his own (Odin's table is lane L3's: "asleep" says nothing, "greet").
        check("asleep".equals(picked(player, odin)), "asleep, Odin's own node is the silent one: " + picked(player, odin));
        odin.setState(NpcState.SLEEPING, 0);
        check(AsgardTalk.begin(player, odin) && "odin_asleep".equals(AsgardTalk.currentNode(player)), "Frigga answers for him");
        AsgardTalk.end(player);
        odin.setState(NpcState.IDLE, 0);
        state.setOdinAwake(true);
        check("greet".equals(picked(player, odin)), "awake, he greets: " + picked(player, odin));
        // Frigga: the first talk, then after Hela the hint to Gungnir.
        check("greeting_first".equals(picked(player, frigga)), "Frigga's first greeting");
        ModAttachments.ASGARD.set(player, ModAttachments.ASGARD.get(player).with(AsgardProgress.TALKED_FRIGGA));
        check("greeting".equals(picked(player, frigga)), "her usual greeting");
        state.setHelaDefeats(1);
        check("after_hela".equals(picked(player, frigga)), "after Hela falls, the way to Gungnir");
        ModAttachments.ASGARD.set(player, ModAttachments.ASGARD.get(player).with(AsgardProgress.GUNGNIR_TAKEN));
        check("greeting".equals(picked(player, frigga)), "not once Gungnir is his");
        // Hurt, Frigga offers healing.
        player.setHealth(10.0F);
        AsgardDialogue.Node greeting = AsgardDialogue.find(Role.FRIGGA, "greeting");
        check(AsgardDialogue.offered(ctx(player, frigga), greeting).stream().anyMatch(o -> o.key().equals("heal_me")), "she offers to heal");
        helper.succeed();
    }

    /** A Bifrost opened beside them never carries the city's own Asgardians off; a summoned Einherjar goes with his master. */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBifrostLeavesTheCitysAsgardiansAtTheirPosts(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer traveller = fx.player(5.5, FLOOR, 5.5);
        AsgardianEntity heimdall = npc(fx, Role.HEIMDALL, 6.5, 5.5);
        AsgardianEntity guard = npc(fx, Role.EINHERJAR, 5.5, 6.5);
        AsgardianEntity summoned = npc(fx, Role.EINHERJAR, 4.5, 5.5);
        summoned.summonFor(traveller, 200);
        check(!com.kolodkingal.enhanced.bifrost.Bifrost.goesAlong(traveller, heimdall), "Heimdall keeps his post");
        check(!com.kolodkingal.enhanced.bifrost.Bifrost.goesAlong(traveller, guard), "a guard of the city stays");
        check(com.kolodkingal.enhanced.bifrost.Bifrost.goesAlong(traveller, summoned), "a summoned Einherjar goes along");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theyAnswerTheHeroThePlayerIs(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer player = fx.player(5.5, FLOOR, 5.5, Power.THOR);
        AsgardianEntity frigga = npc(fx, Role.FRIGGA, 7.5, 5.5);
        AsgardianEntity guard = npc(fx, Role.EINHERJAR, 5.5, 7.5);
        check("react_thor".equals(picked(player, frigga)), "Frigga greets her son: " + picked(player, frigga));
        check("common.react_thor".equals(picked(player, guard)), "a guard his prince");
        Powers.setPower(player, Power.LOKI);
        check("common.react_loki".equals(picked(player, guard)), "and watches the trickster");
        Powers.setPower(player, Power.HELA);
        check("react_hela".equals(picked(player, frigga)), "Frigga stands up to Hela");
        Powers.setPower(player, Power.ODIN);
        check(AsgardTalk.begin(player, guard) && guard.state() == NpcState.BOWING, "they bow to the Allfather");
        AsgardTalk.end(player);
        Powers.setPower(player, Power.NONE);
        check("greeting".equals(picked(player, guard)), "anyone else: the greeting");
        check(AsgardDialogue.lineKey(Role.EINHERJAR, "common.react_odin").equals("npc.enhanced.common.react_odin"), "shared lines");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onlyAnOfferedAnswerFromNearIsTaken(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer player = fx.player(5.5, FLOOR, 5.5);
        AsgardianEntity hogun = npc(fx, Role.HOGUN, 7.5, 5.5);
        ModAttachments.ASGARD.set(player, ModAttachments.ASGARD.get(player).with(AsgardProgress.TALKED_WARRIORS));
        check(!AsgardTalk.tryChoose(player, hogun.getId(), "about_vault"), "no answer without a talk");
        check(AsgardTalk.begin(player, hogun), "the talk opens");
        check(hogun.state() == NpcState.TALKING && player.getUUID().equals(hogun.talkingWith()), "Hogun stops to talk");
        check(AsgardTalk.offered(player).equals(List.of("about_vault", "about_hela")), "his answers: " + AsgardTalk.offered(player));
        check(!AsgardTalk.tryChoose(player, hogun.getId(), "trade"), "an answer not offered is refused");
        check(!AsgardTalk.tryChoose(player, hogun.getId() + 1000, "about_vault"), "and one for someone else");
        check(AsgardTalk.tryChoose(player, hogun.getId(), "about_vault"), "an offered one is taken");
        check("vault".equals(AsgardTalk.currentNode(player)), "and leads on: " + AsgardTalk.currentNode(player));
        Vec3 away = helper.absoluteVec(new Vec3(17.5, FLOOR, 17.5));
        player.teleportTo(away.x, away.y, away.z);
        check(!AsgardTalk.tryChoose(player, hogun.getId(), "why_sealed"), "from too far it is refused");
        check(AsgardTalk.tryChoose(player, hogun.getId(), ""), "closing the screen ends the talk");
        check(AsgardTalk.currentNode(player) == null && hogun.talkingWith() == null && hogun.state() == NpcState.IDLE,
                "and Hogun goes back to his day");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everyLineAndAnswerIsInEnglishAndHebrew(GameTestHelper helper) throws IOException {
        List<String> missing = new ArrayList<>();
        for (String name : List.of("en_us", "he_il")) {
            Path file = Platform.get().modResource("assets/" + Enhanced.MODID + "/lang/" + name + ".json").orElseThrow();
            JsonObject lang = JsonParser.parseString(Files.readString(file,
                    StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : AsgardDialogue.langKeys()) {
                if (!lang.has(key)) {
                    missing.add(name + ":" + key);
                } else if (lang.get(key).getAsString().length() > 140) {
                    missing.add(name + ":" + key + " (longer than 140)");
                }
            }
        }
        check(AsgardDialogue.langKeys().size() > 150, "the table is full: " + AsgardDialogue.langKeys().size());
        check(missing.isEmpty(), "missing: " + missing);
        helper.succeed();
    }

    // ---- trades ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theTradersTradeAndRestock(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        for (Role role : Role.values()) {
            check(AsgardTrades.offers(role).isEmpty() != role.trader(), role + " trades if and only if a trader");
        }
        AsgardianEntity volstagg = npc(fx, Role.VOLSTAGG, 5.5, 5.5);
        MerchantOffers offers = volstagg.getOffers();
        check(offers.size() >= 4, "Volstagg sells food");
        for (MerchantOffer offer : offers) {
            check(offer.getBaseCostA().is(Items.GOLD_INGOT) || offer.getBaseCostA().is(Items.EMERALD), "paid in gold or emeralds");
        }
        MerchantOffer bread = offers.get(0);
        while (!bread.isOutOfStock()) {
            bread.increaseUses();
        }
        volstagg.restock();
        check(!bread.isOutOfStock(), "restocked");
        MerchantOffer horse = AsgardTrades.offers(Role.VALKYRIE).get(4);
        check(horse.getResult().is(Items.LEAD) && horse.getResult().get(DataComponents.ITEM_NAME) != null, "Valkyrie sells a horse");
        check(AsgardTrades.token().is(Items.GOLD_NUGGET), "Sif's Token");
        helper.succeed();
    }

    // ---- fighting beside the players ----

    @EnhancedTest(template = ARENA, batch = FIGHT)
    public static void theWarriorsChooseTheArmyFirstAndHelaLast(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer player = fx.player(1.5, FLOOR, 1.5);
        Vec3 from = helper.absoluteVec(new Vec3(5.5, FLOOR, 5.5));
        Zombie zombie = mob(fx, EntityType.ZOMBIE, 7.5, 5.5);
        zombie.setTarget(player);
        Husk berserker = mob(fx, EntityType.HUSK, 12.5, 12.5); // stands in for a berserker (lane L4's tag)
        WitherSkeleton hela = mob(fx, EntityType.WITHER_SKELETON, 9.5, 9.5); // stands in for Hela
        java.util.function.Predicate<Entity> army = e -> e == berserker;
        java.util.function.Predicate<Entity> isHela = e -> e == hela;
        List<LivingEntity> all = List.of(zombie, berserker, hela);
        check(DefendAsgardGoal.pick(from, all, 32, army, isHela, DefendAsgardGoal::hostileMonster) == berserker,
                "the army before a nearer monster");
        check(DefendAsgardGoal.pick(from, List.of(zombie, hela), 32, army, isHela, DefendAsgardGoal::hostileMonster) == zombie,
                "a monster after a player before Hela");
        check(DefendAsgardGoal.pick(from, List.of(hela, berserker), 32, army, isHela, e -> false) == berserker,
                "never Hela while her army is near");
        check(DefendAsgardGoal.pick(from, List.of(hela), 32, army, isHela, e -> false) == hela, "Hela when she stands alone");
        zombie.setTarget(null);
        check(DefendAsgardGoal.pick(from, List.of(zombie), 32, army, isHela, DefendAsgardGoal::hostileMonster) == null,
                "a monster after nobody is left alone");
        AsgardianEntity guard = npc(fx, Role.EINHERJAR, 5.5, 5.5);
        check(!guard.canAttack(player), "a player is never their foe");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = FIGHT, timeoutTicks = 120)
    public static void summonedEinherjarFollowGuardAndLeave(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer owner = fx.player(5.5, FLOOR, 5.5);
        ServerPlayer friend = fx.player(7.5, FLOOR, 7.5);
        List<Entity> summoned = Asgardians.summonEinherjar(owner, 3, 60);
        check(summoned.size() == 3, "three come: " + summoned.size());
        for (Entity e : summoned) {
            AsgardianEntity einherjar = (AsgardianEntity) e;
            fx.onClose(e::discard);
            check(einherjar.role() == Role.EINHERJAR && owner.getUUID().equals(einherjar.owner()), "Einherjar of their master");
            check(!einherjar.canAttack(owner) && !einherjar.canAttack(friend), "never against his team (PvP off)");
            check(!einherjar.doHurtTarget(owner), "a blow at the master does nothing");
        }
        Zombie zombie = mob(fx, EntityType.ZOMBIE, 15.5, 15.5);
        owner.setLastHurtMob(zombie);
        fx.everyTick(110, tick -> {
            if (tick == 20) {
                for (Entity e : summoned) {
                    check(((AsgardianEntity) e).getTarget() == zombie, "they fight what their master fights");
                }
            }
            if (tick == 80) {
                for (Entity e : summoned) {
                    check(e.isRemoved(), "their time is up: they vanish");
                }
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "asgard_npc_command")
    public static void theAllfathersCommandAndTheBow(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer odin = fx.player(5.5, FLOOR, 5.5);
        AsgardianEntity guard = npc(fx, Role.EINHERJAR, 8.5, 5.5);
        AsgardianEntity citizen = npc(fx, Role.CITIZEN, 5.5, 9.5);
        Zombie zombie = mob(fx, EntityType.ZOMBIE, 12.5, 12.5);
        check(Asgardians.command(odin, zombie, 200) == 1, "the warrior answers, the citizen does not");
        check(guard.getTarget() == zombie, "and attacks the foe");
        Asgardians.bowTo(odin);
        check(citizen.state() == NpcState.BOWING, "those who see him bow");
        helper.succeed();
    }

    /** Odin's command reaches 64 blocks: each command test has a batch of its own (nothing else near). */
    @EnhancedTest(template = ARENA, batch = "asgard_npc_command_strike", timeoutTicks = 220)
    public static void theAllfathersCommandMakesTheEinherjarStrike(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer odin = fx.player(2.5, FLOOR, 2.5);
        AsgardianEntity guard = npc(fx, Role.EINHERJAR, 8.5, 8.5);
        Zombie zombie = mob(fx, EntityType.ZOMBIE, 12.5, 12.5);
        zombie.setNoAi(true);
        check(Asgardians.command(odin, zombie, 400) == 1, "the guard answers the command");
        fx.everyTick(200, tick -> {
            check(guard.isAlive() && !guard.isDown(), "the guard stands");
            return zombie.getLastHurtByMob() == guard || !zombie.isAlive();
        });
    }

    @EnhancedTest(template = ARENA, batch = FIGHT)
    public static void sifsTrainingCountsTouchesAndGivesHerToken(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerPlayer player = fx.player(5.5, FLOOR, 5.5);
        AsgardianEntity sif = npc(fx, Role.SIF, 6.5, 5.5);
        ModAttachments.ASGARD.set(player, ModAttachments.ASGARD.get(player).with(AsgardProgress.TALKED_SIF));
        check(AsgardTalk.begin(player, sif) && AsgardTalk.tryChoose(player, sif.getId(), "train"), "she offers training");
        check(AsgardTalk.tryChoose(player, sif.getId(), "ready"), "and it begins");
        check(sif.state() == NpcState.SPARRING, "she spars");
        float health = player.getHealth();
        for (int i = 0; i < 5; i++) {
            sif.invulnerableTime = 0;
            check(!sif.hurt(fx.level().damageSources().playerAttack(player), 5.0F), "a touch, no harm");
        }
        check(sif.state() == NpcState.IDLE && sif.getHealth() == sif.getMaxHealth(), "five touches: the duel is over, Sif unhurt");
        boolean token = false;
        for (ItemStack stack : player.getInventory().items) {
            token |= stack.is(Items.GOLD_NUGGET) && stack.get(DataComponents.ITEM_NAME) != null;
        }
        check(token, "Sif's Token is his");
        check(player.getHealth() == health, "nobody was hurt");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theCitysPeopleNeverLeaveTheirRealm(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        AsgardianEntity frigga = npc(fx, Role.FRIGGA, 5.5, 5.5);
        ServerLevel nether = fx.level().getServer().getLevel(Level.NETHER);
        Entity moved = frigga.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(nether, frigga.position(),
                Vec3.ZERO, 0.0F, 0.0F, net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        check(moved == null && frigga.level() == fx.level() && !frigga.isRemoved(), "Frigga stays");
        helper.succeed();
    }

    // ---- dining (3.7.2: Asgard's dining hall) ----

    /**
     * A table in the arena as the Hall of Feasts has them: a board of top spruce slabs along x (6 to 14) at z 10, legs
     * of stripped spruce at its ends, three benches on each side facing it (x 8, 10, 12; z 9 facing south, z 11 north).
     */
    private static void table(GameTestHelper helper) {
        for (int x = 6; x <= 14; x++) {
            helper.setBlock(new BlockPos(x, 2, 10), x == 6 || x == 14 ? Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState()
                    : Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        }
        helper.setBlock(new BlockPos(8, 3, 10), Blocks.CAKE.defaultBlockState());
        for (int x = 8; x <= 12; x += 2) {
            helper.setBlock(new BlockPos(x, 2, 9),
                    ModBlocks.ASGARD_BENCH.get().defaultBlockState().setValue(AsgardBenchBlock.FACING,
                            Direction.SOUTH));
            helper.setBlock(new BlockPos(x, 2, 11),
                    ModBlocks.ASGARD_BENCH.get().defaultBlockState().setValue(AsgardBenchBlock.FACING,
                            Direction.NORTH));
        }
    }

    /** Someone sitting on the bench at (x, z) of the arena (an armour stand: the seat is taken). */
    private static void taken(TestKit.Fixture fx, int x, int z) {
        ArmorStand stand = mob(fx, EntityType.ARMOR_STAND, x + 0.5, z + 0.5);
        Vec3 at = fx.helper().absoluteVec(new Vec3(x + 0.5, FLOOR + 0.5, z + 0.5));
        stand.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        stand.setNoGravity(true);
    }

    /**
     * Volstagg goes to a free seat at the table (the others are taken), never over the table on his way; he sits on the
     * bench facing the table, a chicken leg in hand, eats, and keeps facing the table while a player walks round him.
     */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void volstaggSitsAtTheTableFacingItAndEats(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        table(helper);
        for (int[] t : new int[][]{{8, 9}, {10, 9}, {12, 9}, {8, 11}, {10, 11}}) {
            taken(fx, t[0], t[1]);
        }
        BlockPos free = helper.absolutePos(new BlockPos(12, 2, 11));
        AsgardianEntity volstagg = npc(fx, Role.VOLSTAGG, 3.5, 3.5);
        volstagg.setHome("", helper.absolutePos(new BlockPos(10, 2, 10)), 10);
        FeastHall.invite(volstagg);
        ServerPlayer player = fx.player(17.5, FLOOR, 17.5);
        long[] seated = {-1};
        helper.onEachTick(() -> {
            if (seated[0] >= 0 && helper.getTick() == seated[0] + 10) {
                volstagg.push(0.6, 0.0, 0.6); // a shove: he stays in his seat
            }
            BlockPos at = volstagg.blockPosition();
            check(!FeastHall.overTable(helper.getLevel(), at.getX(), at.getY(), at.getZ()), "never over the table: "
                    + volstagg.position());
            if (seated[0] >= 0) {
                // a player walks round him: he still faces the table
                double a = (helper.getTick() - seated[0]) * 0.15;
                Vec3 round = Vec3.atBottomCenterOf(free).add(Math.cos(a) * 2.5, 0.0, Math.sin(a) * 2.5);
                player.teleportTo(round.x, round.y, round.z);
            }
        });
        helper.succeedWhen(() -> {
            check(volstagg.state() == NpcState.DINING, "he sits down to eat: " + volstagg.state() + " at "
                    + volstagg.position());
            if (seated[0] < 0) {
                seated[0] = helper.getTick();
            }
            check(volstagg.blockPosition().equals(free) && Math.abs(volstagg.getY() - (free.getY() + 0.5)) < 0.01,
                    "on the free bench, on its seat: " + volstagg.position());
            float north = Direction.NORTH.toYRot();
            check(Math.abs(net.minecraft.util.Mth.wrapDegrees(volstagg.getYRot() - north)) < 1.0F
                    && Math.abs(net.minecraft.util.Mth.wrapDegrees(volstagg.yBodyRot - north)) < 1.0F,
                    "facing the table (north): " + volstagg.getYRot() + " " + volstagg.yBodyRot);
            check(volstagg.getMainHandItem().is(Items.COOKED_CHICKEN), "a chicken leg in hand");
            check(helper.getTick() - seated[0] >= 40, "and he stays");
        });
    }

    /**
     * The feast at dusk: who comes, when, to which seat; where one from far off arrives; the guests' seats face the
     * hall.
     */
    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theWarriorsThreeAndSifComeToTheHighTableAtDusk(GameTestHelper helper) {
        check(!FeastHall.feastAt(10999) && FeastHall.feastAt(11000) && FeastHall.feastAt(13499)
                && !FeastHall.feastAt(13500)
                && FeastHall.feastAt(24000L * 3 + 12000), "the feast is at dusk, every day");
        check(FeastHall.feastLeft(12000) == 1500 && FeastHall.feastLeft(6000) == 0, "and ends at the first stars");
        BlockPos ring = CityPlan.TRAINING_RING;
        List<BlockPos> seats = new ArrayList<>();
        for (Role role : List.of(Role.VOLSTAGG, Role.SIF, Role.FANDRAL, Role.HOGUN)) {
            check(FeastHall.guest(role) && FeastHall.homeAt(role, ring, 12000).equals(CityPlan.FEAST_HALL)
                    && FeastHall.homeAt(role, ring, 6000).equals(ring), role + " goes to the hall at dusk, home after");
            CityPlan.Bench seat = FeastHall.seatOf(role);
            check(seat != null && FeastHall.highTable().contains(seat) && !seats.contains(seat.pos()), role
                    + " has his own place at the high table");
            seats.add(seat.pos());
            check(seat.facing() == Direction.WEST, "facing the hall (west, its doors): " + seat);
        }
        for (Role role : List.of(Role.FRIGGA, Role.CITIZEN, Role.EINHERJAR, Role.ODIN, Role.HEIMDALL)) {
            check(!FeastHall.guest(role) && FeastHall.homeAt(role, ring, 12000).equals(ring), role + " keeps his home");
        }
        check(FeastHall.arrival(CityPlan.FEAST_HALL).equals(FeastHall.DOOR) && FeastHall.arrival(ring).equals(ring),
                "one from far off arrives at the hall's front door");
        helper.succeed();
    }

    /**
     * At dusk Sif and Fandral sit at the table with bread and an apple (no sparring at the feast), and get up when the
     * night comes.
     */
    @EnhancedTest(template = ARENA, batch = "asgard_npc_feast", timeoutTicks = 400)
    public static void atDuskSifDinesUntilTheNight(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        ServerLevel level = fx.level();
        long before = level.getDayTime();
        long day = 24000L * (before / 24000L);
        level.setDayTime(day + 11500L);
        fx.onClose(() -> level.setDayTime(before));
        table(helper);
        AsgardianEntity sif = npc(fx, Role.SIF, 4.5, 15.5);
        sif.setHome("", helper.absolutePos(new BlockPos(10, 2, 10)), 10);
        AsgardianEntity fandral = npc(fx, Role.FANDRAL, 16.5, 4.5);
        fandral.setHome("", helper.absolutePos(new BlockPos(10, 2, 10)), 10);
        long[] night = {-1};
        helper.onEachTick(() -> {
            if (night[0] < 0 && sif.state() == NpcState.DINING && fandral.state() == NpcState.DINING) {
                check(FeastHall.diningSeat(level, sif.blockPosition()) && FeastHall.diningSeat(level, fandral.blockPosition()),
                        "both on seats at the table");
                check(sif.getMainHandItem().is(Items.BREAD) && fandral.getMainHandItem().is(Items.APPLE),
                        "bread and an apple, swords put by");
                level.setDayTime(day + 13600L); // the night comes
                night[0] = helper.getTick();
            }
        });
        helper.succeedWhen(() -> {
            check(night[0] >= 0, "both dine at dusk: " + sif.state() + " " + fandral.state());
            check(helper.getTick() - night[0] >= 10 && sif.state() != NpcState.DINING && fandral.state() != NpcState.DINING,
                    "and get up when the night comes: " + sif.state() + " " + fandral.state());
            check(!sif.getMainHandItem().is(Items.BREAD), "the bread put away");
        });
    }
}
