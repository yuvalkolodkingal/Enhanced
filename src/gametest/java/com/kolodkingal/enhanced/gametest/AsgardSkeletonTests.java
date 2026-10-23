package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.HeroServers;
import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.asgard.AsgardChunkGenerator;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardGate;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.bifrost.BifrostKeys;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.AsgardConfig;
import com.kolodkingal.enhanced.config.BifrostConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.Resource;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.network.AsgardFxPayload;
import com.kolodkingal.enhanced.network.AsgardTalkPayload;
import com.kolodkingal.enhanced.network.BifrostActionPayload;
import com.kolodkingal.enhanced.network.HeimdallSightPayload;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.network.ThorFxPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The skeleton of 3.5, Asgard (S0): the three gods appended after CUSTOM, their rows, bits and payloads (protocol 9), the
 * realm's dimension with its placeholder platform, the gate that lets only the Bifrost in and out, the Book of the Vishanti
 * refused, the keys of the Bifrost and its journeys: to Asgard (the Observatory), and with everyone inside its circle.
 * Batch {@code asgard_skel}; a test that changes the config has a batch of its own.
 */
public final class AsgardSkeletonTests {
    static final String BATCH = "asgard_skel";
    static final List<Power> GODS = List.of(Power.HEIMDALL, Power.ODIN, Power.HELA);

    // ---- the powers, the rows, the bits ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theGodsOfAsgardComeAfterCustomInOrder(GameTestHelper helper) throws IOException {
        Power[] all = Power.values();
        check(all[Power.CUSTOM.ordinal() + 1] == Power.HEIMDALL && all[Power.CUSTOM.ordinal() + 2] == Power.ODIN
                && all[Power.CUSTOM.ordinal() + 3] == Power.HELA && all.length >= Power.CUSTOM.ordinal() + 4,
                "…CUSTOM, HEIMDALL, ODIN, HELA (the stream sends the ordinal; 3.6 appends after them)");
        check(Power.AGATHA.ordinal() == 14 && Power.CUSTOM.ordinal() == 17, "nobody before them moved");
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        for (String name : List.of("en_us", "he_il")) {
            JsonObject lang = JsonParser.parseString(Files.readString(mod.findResource("assets", Enhanced.MODID, "lang", name + ".json"),
                    StandardCharsets.UTF_8)).getAsJsonObject();
            for (Power god : GODS) {
                check(lang.has("power.enhanced." + god.getSerializedName()), god + " has a name in " + name);
                for (int ability = 1; ability <= 5; ability++) {
                    check(lang.has(god.abilityKey(ability)), god + " ability " + ability + " has a name in " + name);
                }
                for (Variant look : Variants.of(god)) {
                    check(lang.has(look.translationKey()), look.translationKey() + " in " + name);
                }
            }
        }
        for (Power god : GODS) {
            check(HeroServers.of(god) != null && HeroServers.of(god).power() == god, god + " has a server plug-in");
            check(Variants.of(god).size() == 3 && Variants.of(god).get(2).id().equals("own"), god + ": two looks and the player's own");
            check(!god.canFly() && god.flightAbility() == -1 && god.flightSpeed() == 0.05F, god + " walks");
            check(Power.byName(god.getSerializedName()) == god, god + " is found by its name");
        }
        check(Power.HEIMDALL.color == 0xE8B54A && Power.HEIMDALL.glow == 0xFFE7A3 && Power.ODIN.color == 0xD9A63A
                && Power.ODIN.glow == 0xFFF0C2 && Power.HELA.color == 0x2E7D4F && Power.HELA.glow == 0x9CF2B5, "their colours");
        check(Power.HELA.titleColor() == 0x3FBF6F && Power.HEIMDALL.titleColor() == Power.HEIMDALL.color, "their names' colours");
        check(BossKind.HELA.ordinal() == 3 && BossKind.byId("hela") == BossKind.HELA, "Hela is the fourth boss");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBitsFitAndTheProtocolMovedOnFromNine(GameTestHelper helper) {
        // (3.6 bumps the protocol to 10 and appends its own states: StoneSkeletonTests counts them exactly.)
        check(Integer.parseInt(ModNetwork.PROTOCOL_VERSION) >= 9, "protocol 9 or later");
        // (3.8: the states are two words, room for 128.)
        check(Live.values().length <= 128 && Live.values().length >= 42, "Live: 42 of 128 bits or more: "
                + Live.values().length);
        check(Toggle.values().length <= 32 && Toggle.values().length >= 15, "Toggle: 15 of 32 bits or more: "
                + Toggle.values().length);
        check(Live.HEIMDALL_WARD.ordinal() == 35 && Live.HELA_SUMMONING.ordinal() == 41, "the states of 3.5 are appended");
        check(Toggle.HEIMDALL_SIGHT.ordinal() == 12 && Toggle.ODINSLEEP.ordinal() == 13 && Toggle.HELA_FORM.ordinal() == 14,
                "and the toggles");
        check(AsgardProgress.FLAG_COUNT <= 31 && AsgardProgress.TALKED_WARRIORS == 1 << 9, "the progress flags fit");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theRowsOfAsgardAreEighteenInTheirRegions(GameTestHelper helper) {
        check(HeroAbility.values().length >= 133, "81 + 18 rows, and the creator lane's 34 (3.5; 3.6 appends): "
                + HeroAbility.values().length);
        Set<String> ids = new HashSet<>();
        for (HeroAbility a : HeroAbility.values()) {
            check(ids.add(a.id()), a.id() + " is unique");
        }
        int first = HeroAbility.HEIMDALL_HOFUND.ordinal();
        check(HeroAbility.values()[first - 1] == HeroAbility.XAVIER_CEREBRO, "appended after Professor X's rows");
        for (Power god : GODS) {
            List<HeroAbility> rows = HeroAbility.of(god);
            check(rows.size() == 6, god + " has six rows");
            for (int home = 0; home <= 5; home++) {
                check(HeroAbility.at(god, home) != null, god + " has a row on key " + home);
            }
            for (HeroAbility a : rows) {
                check(a.heroId().equals(god.getSerializedName()) && a.ordinal() >= first, a + " is in its region");
            }
        }
        check(HeroAbility.byId("heimdall.sight").isOn(dataWith(Toggle.HEIMDALL_SIGHT)), "the Sight is on with its toggle");
        check(HeroAbility.ODIN_GUNGNIR.resources(HeroAbility.BOTH).contains(Resource.GUNGNIR) && HeroAbility.HELA_ARMY.resources(HeroAbility.BOTH).contains(Resource.UNDEAD),
                "the new resources");
        check(HeroAbility.ODIN_ODINFORCE.resources(HeroAbility.BOTH).contains(Resource.SHIELD), "the Odinforce is a shield");
        check(HeroAbility.HEIMDALL_BIFROST.world && HeroAbility.HEIMDALL_STRIKE.world && HeroAbility.ODIN_BANISH.world
                && HeroAbility.ODIN_COMMAND.world && HeroAbility.HELA_ARMY.world, "the world rows");
        helper.succeed();
    }

    private static com.kolodkingal.enhanced.power.PowerData dataWith(Toggle toggle) {
        com.kolodkingal.enhanced.power.PowerData data = CustomKitSupport.data(Power.HEIMDALL);
        data.setToggle(toggle, true);
        return data;
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void thePayloadsOfAsgardRoundTrip(GameTestHelper helper) {
        roundTrip(AsgardTalkPayload.Open.STREAM_CODEC, new AsgardTalkPayload.Open(7, 2, "npc.enhanced.frigga.greeting", List.of("Eilon"),
                List.of("bye", "hela")), "the talk opening");
        roundTrip(AsgardTalkPayload.Choose.STREAM_CODEC, new AsgardTalkPayload.Choose(7, "bye"), "the answer");
        roundTrip(HeimdallSightPayload.STREAM_CODEC, new HeimdallSightPayload(List.of(new HeimdallSightPayload.Far(UUID.randomUUID(),
                "Peter", Level.NETHER.location(), new Vec3(1.5, 64, -2.5)))), "the far players");
        roundTrip(AsgardFxPayload.STREAM_CODEC, new AsgardFxPayload(AsgardFxPayload.HELA + 3, 12, List.of(Vec3.ZERO, new Vec3(1, 2, 3)), 40),
                "an effect of Asgard");
        roundTrip(BifrostActionPayload.STREAM_CODEC, new BifrostActionPayload(BifrostActionPayload.ASGARD, 0, ""), "the Bifrost to Asgard");
        roundTrip(BifrostActionPayload.STREAM_CODEC, new BifrostActionPayload(BifrostActionPayload.SEND, 42, "home"), "Heimdall's Send");
        check(BifrostActionPayload.ASGARD == 4 && BifrostActionPayload.SEND == 5, "actions 4 and 5");
        check(ThorFxPayload.BIFROST_DEPART == 4, "the departure of the Bifrost is kind 4");
        roundTrip(ThorFxPayload.STREAM_CODEC, new ThorFxPayload(ThorFxPayload.BIFROST_DEPART, 3, List.of(Vec3.ZERO)), "the departure");
        AsgardProgress progress = AsgardProgress.EMPTY.with(AsgardProgress.WATCH_WON | AsgardProgress.CROWNED)
                .withCameFrom(GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 2, 3))).withHofundBond(2).withGungnirBond(1).withWatchReadyAt(99L);
        roundTrip(AsgardProgress.STREAM_CODEC, progress, "the progress (sent)");
        check(AsgardProgress.CODEC.parse(NbtOps.INSTANCE, AsgardProgress.CODEC.encodeStart(NbtOps.INSTANCE, progress).getOrThrow())
                .getOrThrow().equals(progress), "the progress (saved)");
        check(progress.has(AsgardProgress.WATCH_WON) && !progress.without(AsgardProgress.WATCH_WON).has(AsgardProgress.WATCH_WON)
                && !progress.has(AsgardProgress.VISITED), "its flags");
        helper.succeed();
    }

    private static <T> void roundTrip(StreamCodec<ByteBuf, T> codec, T value, String what) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        check(codec.decode(buf).equals(value), what + " round-trips");
    }

    // ---- the realm ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void asgardIsADimensionWithItsGeneratorAndTheLandingPlatform(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel asgard = AsgardTestSupport.withArrival(server);
        check(AsgardDimension.level(server) == asgard && AsgardDimension.is(asgard), "enhanced:asgard is a level");
        check(asgard.getChunkSource().getGenerator() instanceof AsgardChunkGenerator, "made by Asgard's own generator");
        DimensionType type = asgard.dimensionType();
        check(type.hasSkyLight() && !type.hasCeiling() && type.bedWorks() && !type.respawnAnchorWorks() && !type.hasRaids(),
                "a sky, beds that work, no anchors, no raids");
        check(type.minY() == -64 && type.height() == 384 && type.effectsLocation().equals(Enhanced.id("asgard")), "-64 to 320, its own sky");
        Biome biome = server.registryAccess().registryOrThrow(Registries.BIOME).get(Enhanced.id("asgard"));
        check(biome != null && biome.getMobSettings().getMobs(MobCategory.MONSTER).isEmpty() && biome.getBackgroundMusic().isPresent(),
                "its biome: no monsters, its music");
        // (3.5 L1: the realm's generator replaced the placeholder platform; the landing is on the Observatory's floor.)
        BlockPos landing = BlockPos.containing(CityPlan.ARRIVAL);
        check(!asgard.getBlockState(landing.below()).isAir() && asgard.getBlockState(landing).isAir(), "solid floor under the landing, room above it");
        BlockPos socket = CityPlan.OBSERVATORY_SOCKET;
        check(asgard.getBlockState(socket.offset(0, -1, AsgardChunkGenerator.PLATFORM_RADIUS + 2)).isAir(), "and nothing beyond it: the void");
        check(AsgardState.get(server) != null && !AsgardState.get(server).odinAwake(), "Odin sleeps in a new Asgard");
        helper.succeed();
    }

    // ---- the gate ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theGateLetsOnlyTheBifrostAndOperatorsThrough(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel asgard = AsgardTestSupport.withArrival(server);
        Vec3 arrival = CityPlan.ARRIVAL;
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        player.teleportTo(asgard, arrival.x, arrival.y, arrival.z, 0, 0);
        check(player.level() == helper.getLevel(), "a teleport into Asgard is refused");
        check("message.enhanced.asgard.only_bifrost".equals(BossTestSupport.lastActionBarKey(player)), "and he is told why");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 10.5));
        check(zombie.changeDimension(new DimensionTransition(asgard, arrival, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING)) == null
                && zombie.level() == helper.getLevel(), "a creature cannot go in either");
        check(AsgardGate.allows(player, Level.OVERWORLD, Level.NETHER), "the gate does not care for other worlds");

        // (The GameTest server gives its operators level 0: this one is given level 2, as /tp needs.)
        server.getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), 2, false));
        f.onClose(() -> server.getPlayerList().getOps().remove(player.getGameProfile()));
        player.teleportTo(asgard, arrival.x, arrival.y, arrival.z, 0, 0);
        check(AsgardDimension.in(player), "an operator's /tp is free");
        server.getPlayerList().getOps().remove(player.getGameProfile());
        player.teleportTo(helper.getLevel(), player.getX(), player.getY(), player.getZ(), 0, 0);
        check(AsgardDimension.in(player), "nor is the way out open but by the Bifrost");
        check(Bifrost.open(player, Bifrost.Destination.HOME, Bifrost.Source.RESCUE), "the Bifrost (Heimdall's catch: at once)");
        check(!AsgardDimension.in(player), "takes him out");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theVishantiCannotReachAsgard(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerPlayer loki = f.player(5.5, FLOOR, 5.5, Power.LOKI);
        loki.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOOK_OF_THE_VISHANTI.get()));
        PlaceBook.Kind.VISHANTI.set(loki, BifrostData.EMPTY.with(new BifrostData.Place("Realm", AsgardDimension.KEY, CityPlan.ARRIVAL, 0.0F)));
        BossTestSupport.lastActionBarKey(loki);
        VishantiBook.travelToPlace(loki, 0, "Realm");
        check(!PlaceBook.travelling(loki) && PlaceBook.cooldown(loki, PlaceBook.Kind.VISHANTI) == 0, "no way opens, nothing is spent");
        check("message.enhanced.vishanti.asgard".equals(BossTestSupport.lastActionBarKey(loki)), "the Vishanti cannot reach the Realm Eternal");
        check(!PlaceBook.refusesAsgard(loki, PlaceBook.Kind.BIFROST, AsgardDimension.KEY), "the Bifrost's book can");
        helper.succeed();
    }

    // ---- the Bifrost ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theKeysOfTheBifrost(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = f.player(2.5, FLOOR, 2.5);
        check(!BifrostKeys.holdsKey(plain), "a powerless player with nothing in hand has no key");
        plain.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        check(BifrostKeys.holdsKey(plain), "Stormbreaker is a key (as before)");
        plain.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        check(BifrostKeys.holdsKey(plain), "Mjolnir too");
        plain.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        plain.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.HOFUND.get()));
        check(BifrostKeys.holdsKey(plain), "anyone's Hofund, in either hand");
        plain.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        ServerPlayer other = f.player(4.5, FLOOR, 2.5);
        ItemStack gungnir = new ItemStack(ModItems.GUNGNIR.get());
        gungnir.set(ModDataComponents.OWNER.get(), MjolnirOwner.of(other));
        plain.setItemInHand(InteractionHand.MAIN_HAND, gungnir.copy());
        check(!BifrostKeys.holdsKey(plain), "someone else's Gungnir is not");
        other.setItemInHand(InteractionHand.MAIN_HAND, gungnir);
        check(BifrostKeys.holdsKey(other), "its owner's is");
        ServerPlayer heimdall = f.player(6.5, FLOOR, 2.5, Power.HEIMDALL);
        check(BifrostKeys.holdsKey(heimdall) && BifrostKeys.opensScreen(heimdall, 3, false) && !BifrostKeys.opensScreen(heimdall, 3, true),
                "Heimdall needs no item: V opens the screen, Sneak + V goes to the server");
        ServerPlayer odin = f.player(8.5, FLOOR, 2.5, Power.ODIN);
        ServerPlayer hela = f.player(10.5, FLOOR, 2.5, Power.HELA);
        check(!BifrostKeys.holdsKey(odin) && !BifrostKeys.holdsKey(hela), "Odin without Gungnir and Hela have none");
        ServerPlayer custom = CustomKitSupport.hero(f, 12.5, 2.5, CustomKitSupport.def().pick("heimdall.bifrost", 5).key(2, "heimdall.bifrost").build());
        check(BifrostKeys.holdsKey(custom) && BifrostKeys.opensScreen(custom, 2, false), "a custom hero with Heimdall's Bifrost");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theBifrostToAsgardLandsAtTheObservatoryWithTheCircle(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerLevel asgard = AsgardTestSupport.withArrival(helper.getLevel().getServer());
        ServerPlayer traveller = f.player(5.5, FLOOR, 5.5);
        traveller.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HOFUND.get()));
        ServerPlayer friend = f.player(5.5, FLOOR, 7.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.5, FLOOR, 5.5));
        UUID pigId = pig.getUUID();
        f.onClose(() -> {
            Entity moved = asgard.getEntity(pigId);
            if (moved != null) {
                moved.discard();
            }
        });
        Bifrost.toAsgard(traveller);
        check(Powers.get(traveller).is(Live.BIFROST), "the Bifrost gathers");
        f.everyTick(150, tick -> {
            PowerTicker.tick(traveller);
            if (!AsgardDimension.in(traveller)) {
                return false;
            }
            Vec3 at = traveller.position();
            check(Math.abs(at.x - CityPlan.ARRIVAL.x) < 1.0E-6 && Math.abs(at.z - CityPlan.ARRIVAL.z) < 1.0E-6 && at.y >= CityPlan.ARRIVAL.y,
                    "he lands at the Observatory: " + at);
            check(AsgardDimension.in(friend) && friend.position().distanceTo(at.add(0, 0, 2)) < 1.1, "his friend in the circle comes, beside him: "
                    + friend.position());
            Entity moved = asgard.getEntity(pigId);
            check(moved instanceof Pig && moved.position().distanceTo(at.add(2, 0, 0)) < 1.1, "and the pig, in its place: " + moved);
            check(!pig.isAlive() || pig.isRemoved(), "the pig left the Overworld");
            check(asgard.noCollision(friend) && asgard.noCollision(moved), "nobody inside a block");
            return true;
        });
    }

    /** A same-world journey to a saved place: who is in the circle goes, who is not stays; a boss never goes. */
    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theBifrostTakesItsCircleAndNobodyElse(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer traveller = f.player(5.5, FLOOR, 5.5);
        traveller.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        Vec3 there = helper.absoluteVec(new Vec3(14.5, FLOOR, 14.5));
        ModAttachments.BIFROST.set(traveller, BifrostData.EMPTY.with(new BifrostData.Place("There", helper.getLevel().dimension(), there, 0.0F)));
        ServerPlayer inside = f.player(5.5, FLOOR, 7.5);
        ServerPlayer sneaking = f.player(3.5, FLOOR, 4.5);
        sneaking.setShiftKeyDown(true);
        ServerPlayer outside = f.player(5.5, FLOOR, 5.5 + Bifrost.CIRCLE_RADIUS + 0.4);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.5, FLOOR, 5.5));
        ElderGuardian boss = helper.spawnWithNoFreeWill(EntityType.ELDER_GUARDIAN, new Vec3(3.5, FLOOR, 7.0));
        // A pillar where the one inside would land: he must land on it, not in it.
        BlockPos pillar = BlockPos.containing(there.add(0, 0, 2));
        helper.getLevel().setBlockAndUpdate(pillar, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pillar.above(), Blocks.STONE.defaultBlockState());
        Vec3 outsideAt = outside.position();
        Bifrost.toPlace(traveller, 0, "There");
        check(Bifrost.inBifrost(traveller), "the Bifrost gathers round him");
        f.everyTick(150, tick -> {
            PowerTicker.tick(traveller);
            if (Bifrost.inBifrost(traveller)) {
                return false;
            }
            check(traveller.position().distanceTo(there) < 0.6, "he lands at the place: " + traveller.position());
            check(pig.position().distanceTo(there.add(2, 0, 0)) < 0.6, "the pig in the circle goes, keeping its place: " + pig.position());
            check(sneaking.position().distanceTo(there.add(-2, 0, -1)) < 0.6, "sneaking does not keep a player out by default: "
                    + sneaking.position());
            check(Math.abs(inside.getX() - there.x) < 0.01 && Math.abs(inside.getZ() - (there.z + 2)) < 0.01
                    && inside.getY() >= pillar.getY() + 2 - 1.0E-6, "a member whose place is built over lands on top: " + inside.position());
            for (ServerPlayer p : List.of(inside, sneaking)) {
                check(helper.getLevel().noCollision(p), "nobody lands inside a block");
            }
            check(outside.position().equals(outsideAt), "one just outside the circle stays");
            // (An Elder Guardian on land flops about by itself: it must only not be at the landing.)
            check(boss.level() == helper.getLevel() && boss.position().distanceTo(there) > 6.0, "a boss never goes: " + boss.position());
            check(PlaceBook.cooldown(inside, PlaceBook.Kind.BIFROST) == 0 && PlaceBook.cooldown(traveller, PlaceBook.Kind.BIFROST) > 0,
                    "only the one who opened it pays the cooldown");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = "asgard_skel_sneak", timeoutTicks = 200)
    public static void sneakToStayKeepsASneakingPlayerBehind(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(BifrostConfig.SNEAK_TO_STAY, true);
        ServerPlayer traveller = f.player(5.5, FLOOR, 5.5);
        traveller.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        Vec3 there = helper.absoluteVec(new Vec3(14.5, FLOOR, 14.5));
        ModAttachments.BIFROST.set(traveller, BifrostData.EMPTY.with(new BifrostData.Place("There", helper.getLevel().dimension(), there, 0.0F)));
        ServerPlayer sneaking = f.player(5.5, FLOOR, 7.5);
        sneaking.setShiftKeyDown(true);
        ServerPlayer standing = f.player(7.5, FLOOR, 5.5);
        Vec3 stays = sneaking.position();
        Bifrost.toPlace(traveller, 0, "There");
        f.everyTick(150, tick -> {
            PowerTicker.tick(traveller);
            if (Bifrost.inBifrost(traveller)) {
                return false;
            }
            check(sneaking.position().equals(stays), "with bifrost.sneakToStay a sneaking player stays");
            check(standing.position().distanceTo(there.add(2, 0, 0)) < 0.6, "one standing goes");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = "asgard_skel_closed")
    public static void asgardSwitchedOffClosesTheWayIn(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        f.config(AsgardConfig.ENABLED, false);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HOFUND.get()));
        BossTestSupport.lastActionBarKey(player);
        Bifrost.toAsgard(player);
        check(!Bifrost.inBifrost(player) && "message.enhanced.bifrost.asgard_closed".equals(BossTestSupport.lastActionBarKey(player)),
                "the Bifrost does not go to a closed Asgard");
        check(!AsgardGate.allows(player, Level.OVERWORLD, AsgardDimension.KEY), "nor does the gate let anyone in");
        check(!AsgardGate.allows(player, AsgardDimension.KEY, Level.OVERWORLD), "(the way out is the Bifrost's still)");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aLandingIsNeverInsideABlock(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(3.5, FLOOR, 3.5));
        BlockPos wall = helper.absolutePos(new BlockPos(10, (int) FLOOR, 10));
        for (int y = 0; y < 3; y++) {
            level.setBlockAndUpdate(wall.above(y), Blocks.STONE.defaultBlockState());
        }
        Vec3 base = helper.absoluteVec(new Vec3(14.5, FLOOR, 14.5));
        Vec3 landed = Bifrost.landing(level, zombie, Vec3.atBottomCenterOf(wall), base);
        check(level.noCollision(zombie, zombie.getType().getDimensions().makeBoundingBox(landed)), "a landing in a wall moves: " + landed);
        check(landed.y >= wall.getY() + 3 || landed.equals(base), "onto it (or to the traveller's own spot): " + landed);
        Vec3 open = Bifrost.landing(level, zombie, helper.absoluteVec(new Vec3(16.5, FLOOR, 14.5)), base);
        check(open.distanceTo(helper.absoluteVec(new Vec3(16.5, FLOOR, 14.5))) < 1.0E-6, "a free spot on the ground is kept");
        check(Bifrost.inCircle(zombie, zombie.position(), Bifrost.CIRCLE_RADIUS) && Bifrost.groupRadius() == Bifrost.CIRCLE_RADIUS,
                "the circle is the runes' (3.2 by default)");
        helper.succeed();
    }

    private AsgardSkeletonTests() {
    }
}
