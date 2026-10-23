package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PlaceBook;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.build.BillyRanks;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.config.VishantiConfig;
import com.kolodkingal.enhanced.gametest.BossTestSupport.TestFight;
import com.kolodkingal.enhanced.network.WardSyncPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
import com.kolodkingal.enhanced.vishanti.Ward;
import com.kolodkingal.enhanced.vishanti.WardEvents;
import com.kolodkingal.enhanced.vishanti.WardManager;
import com.kolodkingal.enhanced.vishanti.WardStore;
import com.kolodkingal.enhanced.vishanti.WardStrength;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Book of the Vishanti (3.2): who can read it, the places it saves and teleports to (any dimension, the cooldown, the
 * rules of every teleport), and the ward round a base that no monster can enter (its strength by sorcerer, persistence,
 * monsters pushed out, spawns refused, bosses let through). The tests of the book run in the batch {@code items_vishanti},
 * those of the ward in {@code items_ward} (whose radii are set small, so that one ward never reaches a neighbouring test);
 * a test that changes a setting the others rely on has a batch of its own.
 */
public final class ItemsTests {
    private static final String BOOK = "items_vishanti";
    private static final String WARD = "items_ward";
    private static final ResourceKey<Level> ROAD = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("enhanced:witches_road"));

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** A player with a power, holding the book. */
    private static ServerPlayer holder(TestKit.Fixture f, double x, double z, Power power) {
        ServerPlayer player = f.player(x, FLOOR, z, power);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOOK_OF_THE_VISHANTI.get()));
        // Wards stand in the world's data until someone takes them away: whoever a test made takes theirs with them.
        f.onClose(() -> WardManager.removeAll(f.level().getServer(), player.getUUID()));
        return player;
    }

    private static BifrostData book(ServerPlayer player) {
        return PlaceBook.Kind.VISHANTI.data(player);
    }

    private static void add(ServerPlayer player, BifrostData.Place place) {
        PlaceBook.Kind.VISHANTI.set(player, book(player).with(place));
    }

    private static boolean says(Component text, String key) {
        return text.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key);
    }

    private static boolean sorcerer(Power power) {
        // (3.3: Agatha Harkness is a sorcerer too; 3.6: and Doctor Strange.)
        return power == Power.LOKI || power == Power.SCARLET_WITCH || power == Power.BILLY || power == Power.AGATHA
                || power == Power.DOCTOR_STRANGE;
    }

    private static boolean tooltipHas(ItemStack stack, String key) {
        for (Component line : stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL)) {
            if (says(line, key)) {
                return true;
            }
        }
        return false;
    }

    private static <T> T roundTrip(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T value,
                                   RegistryAccess access) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        codec.encode(buf, value);
        T decoded = codec.decode(buf);
        check(buf.readableBytes() == 0, "the codec read everything it wrote (" + buf.readableBytes() + " bytes left)");
        return decoded;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The item

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void theBookIsCraftedFromAmethystAnEnderEyeGoldABookAndLapis(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("book_of_the_vishanti"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:book_of_the_vishanti exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack a = new ItemStack(Items.AMETHYST_SHARD);
        ItemStack e = new ItemStack(Items.ENDER_EYE);
        ItemStack g = new ItemStack(Items.GOLD_INGOT);
        ItemStack b = new ItemStack(Items.BOOK);
        ItemStack l = new ItemStack(Items.LAPIS_LAZULI);
        check(recipe.matches(CraftingInput.of(3, 3, List.of(a, e, a, g, b, g, a, l, a)), level), "A E A / G B G / A L A");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(a, e, a, g, new ItemStack(Items.PAPER), g, a, l, a)), level),
                "a book, not paper");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(a, new ItemStack(Items.ENDER_PEARL), a, g, b, g, a, l, a)), level),
                "an eye, not a pearl");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.BOOK_OF_THE_VISHANTI.get()), "it makes the book");
        check(level.getServer().getAdvancements().get(Enhanced.id("recipes/equipment/book_of_the_vishanti")) != null,
                "and its recipe is unlocked by an advancement");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void theBookIsOneOfAKindAndSaysWhoCanReadIt(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.BOOK_OF_THE_VISHANTI.get());
        check(stack.getMaxStackSize() == 1, "one book to a stack");
        check(tooltipHas(stack, "item.enhanced.book_of_the_vishanti.sorcerers"), "everyone is told that only a sorcerer can read it");
        check(tooltipHas(stack, "item.enhanced.book_of_the_vishanti.use") && tooltipHas(stack, "item.enhanced.book_of_the_vishanti.ward"),
                "and what it does");
        check("item.enhanced.book_of_the_vishanti".equals(stack.getItem().getDescriptionId()), "its name is its own key");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void onlyLokiTheScarletWitchAndBillyCanReadIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Power power : Power.values()) {
            ServerPlayer player = holder(f, 10.5, 10.5, power);
            check(VishantiBook.readable(player, false) == sorcerer(power), power + ": sorcerer = " + sorcerer(power));
            check(WardStrength.isSorcerer(power) == sorcerer(power) && WardStrength.isSorcerer(player) == sorcerer(power),
                    power + ": the ward agrees who a sorcerer is");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void everyoneElseSeesTheRunesSwim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Power power : new Power[]{Power.NONE, Power.TOMMY, Power.THOR, Power.MAGNETO, Power.VISION}) {
            ServerPlayer player = holder(f, 10.5, 10.5, power);
            TestKit.sentPackets(player);
            VishantiBook.savePlace(player, "Home", false);
            check("message.enhanced.vishanti.not_sorcerer".equals(lastActionBarKey(player)), power + ": 'only a sorcerer can read it'");
            check(book(player).places().isEmpty(), power + ": nothing is saved");
            BifrostData sneaked = book(player).with(new BifrostData.Place("Else", helper.getLevel().dimension(), Vec3.ZERO, 0.0F, true));
            PlaceBook.Kind.VISHANTI.set(player, sneaked);
            player.setShiftKeyDown(true);
            player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            check(!PlaceBook.travelling(player), power + ": the book takes nobody who cannot read it");
            check("message.enhanced.vishanti.not_sorcerer".equals(lastActionBarKey(player)), power + ": using it says so too");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void theBookMustBeHeld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = holder(f, 10.5, 10.5, Power.LOKI);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        TestKit.sentPackets(player);
        VishantiBook.savePlace(player, "Home", false);
        check(book(player).places().isEmpty() && "message.enhanced.vishanti.needs_book".equals(lastActionBarKey(player)),
                "no book in hand, no place");
        add(player, new BifrostData.Place("Home", helper.getLevel().dimension(), player.position(), 0.0F));
        VishantiBook.travelToPlace(player, 0, "Home");
        check(!PlaceBook.travelling(player), "and no way either");
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.BOOK_OF_THE_VISHANTI.get()));
        VishantiBook.travelToPlace(player, 0, "Home");
        check(PlaceBook.travelling(player), "the off hand will do");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Places and teleporting

    @EnhancedTest(template = ARENA, timeoutTicks = 200, batch = BOOK)
    public static void everySorcererSavesAPlaceAndIsTakenBackToIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Power[] sorcerers = {Power.LOKI, Power.SCARLET_WITCH, Power.BILLY};
        ServerPlayer[] players = new ServerPlayer[3];
        Vec3[] camps = new Vec3[3];
        for (int i = 0; i < 3; i++) {
            players[i] = holder(f, 4.5 + 6 * i, 4.5, sorcerers[i]);
            VishantiBook.savePlace(players[i], "§cCamp\n", false);
            BifrostData data = book(players[i]);
            check(data.places().size() == 1 && data.places().get(0).name().equals("Camp") && !data.places().get(0).base(),
                    sorcerers[i] + ": saved as Camp, no formatting, not a base: " + data);
            check(data.places().get(0).dimension() == helper.getLevel().dimension(), sorcerers[i] + ": in this world");
            check(ModAttachments.BIFROST.get(players[i]).places().isEmpty(), sorcerers[i] + ": the Bifrost's list is not the book's");
            camps[i] = players[i].position();
            Vec3 away = helper.absoluteVec(new Vec3(4.5 + 6 * i, FLOOR, 15.5));
            players[i].moveTo(away.x, away.y, away.z);
            VishantiBook.travelToPlace(players[i], 0, "Somewhere else");
            check(!PlaceBook.travelling(players[i]), sorcerers[i] + ": a place the list no longer has is ignored");
            VishantiBook.travelToPlace(players[i], 0, "Camp");
            check(PlaceBook.travelling(players[i]), sorcerers[i] + ": the book gathers its light");
            check(PlaceBook.cooldown(players[i], PlaceBook.Kind.VISHANTI) > 0, sorcerers[i] + ": its cooldown runs");
            check(PlaceBook.cooldown(players[i], PlaceBook.Kind.BIFROST) == 0, sorcerers[i] + ": the Bifrost's does not");
        }
        f.everyTick(60, tick -> {
            boolean done = true;
            for (ServerPlayer player : players) {
                PowerTicker.tick(player);
                done &= !PlaceBook.travelling(player);
            }
            if (!done) {
                return false;
            }
            for (int i = 0; i < 3; i++) {
                check(players[i].position().distanceTo(camps[i]) < 0.6, sorcerers[i] + ": back at the camp: " + players[i].position());
                VishantiBook.travelToPlace(players[i], 0, "Camp");
                check(!PlaceBook.travelling(players[i]), sorcerers[i] + ": not again until the book has gathered its strength");
                check("message.enhanced.vishanti.not_ready".equals(lastActionBarKey(players[i])), sorcerers[i] + ": and says so");
            }
            return true;
        });
    }

    @EnhancedTest(template = ARENA, timeoutTicks = 200, batch = BOOK)
    public static void theBookTakesItsHolderToAnotherWorldAndBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = holder(f, 4.5, 4.5, Power.SCARLET_WITCH);
        VishantiBook.savePlace(player, "Camp", false);
        add(player, new BifrostData.Place("Nether roof", Level.NETHER, new Vec3(0.5, 128.0, 0.5), 90.0F));
        Vec3 camp = player.position();
        VishantiBook.travelToPlace(player, 1, "Nether roof");
        check(PlaceBook.travelling(player), "the book gathers its light");
        boolean[] there = {false};
        f.everyTick(120, tick -> {
            PowerTicker.tick(player);
            if (!there[0]) {
                if (player.level().dimension() != Level.NETHER) {
                    return false;
                }
                there[0] = true;
                assertNear(player.getX(), 0.5, 1.0E-6, "x in the Nether");
                assertNear(player.getZ(), 0.5, 1.0E-6, "z in the Nether");
                check(player.getY() >= 128.0 - 1.0E-6, "standing on the roof of the Nether");
                // The cooldown, made short, so the way back can be tried too.
                PlaceBook.readyAt(player, PlaceBook.Kind.VISHANTI, player.level().getGameTime());
                VishantiBook.travelToPlace(player, 0, "Camp");
                check(PlaceBook.travelling(player), "and the way back opens in the Nether too");
                return false;
            }
            if (PlaceBook.travelling(player) || player.level().dimension() != Level.OVERWORLD) {
                return false;
            }
            check(player.position().distanceTo(camp) < 0.6, "back at the camp: " + player.position());
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void sneakAndUseGoesToTheFirstBase(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = holder(f, 4.5, 4.5, Power.LOKI);
        player.setShiftKeyDown(true);
        TestKit.sentPackets(player);
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        check(!PlaceBook.travelling(player) && "message.enhanced.vishanti.no_base".equals(lastActionBarKey(player)),
                "without a base the book says so");
        Vec3 spot = helper.absoluteVec(new Vec3(14.5, FLOOR, 14.5));
        add(player, new BifrostData.Place("Plain", helper.getLevel().dimension(), Vec3.ZERO, 0.0F));
        add(player, new BifrostData.Place("Base", helper.getLevel().dimension(), spot, 45.0F, true));
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        PlaceBook.Journey journey = PlaceBook.journey(player, PlaceBook.Kind.VISHANTI);
        check(journey != null && journey.to().equals(spot), "Sneak + use opens the way to the first base, not to a plain place");
        check(PlaceBook.journey(player, PlaceBook.Kind.BIFROST) == null, "and it is not the Bifrost's");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void theBookIsShutInTheWitchesRoad(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(VishantiBook.shutIn(ROAD), "the Witches' Road is shut to the book, whatever the config says");
        check(!VishantiBook.shutIn(Level.OVERWORLD) && !VishantiBook.shutIn(Level.NETHER), "the other worlds are not");
        check(VishantiConfig.DENIED_DIMENSIONS.get().contains("enhanced:witches_road"), "and the config lists it by default");
        ServerPlayer player = holder(f, 4.5, 4.5, Power.BILLY);
        add(player, new BifrostData.Place("Road", ROAD, new Vec3(0.5, 64.0, 0.5), 0.0F));
        VishantiBook.travelToPlace(player, 0, "Road");
        check(!PlaceBook.travelling(player), "no way leads into the Witches' Road");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "items_vishanti_shut")
    public static void aShutWorldNeitherSavesNorTeleports(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(VishantiConfig.DENIED_DIMENSIONS, List.of("minecraft:overworld"));
        ServerPlayer player = holder(f, 4.5, 4.5, Power.LOKI);
        TestKit.sentPackets(player);
        VishantiBook.savePlace(player, "Home", false);
        check(book(player).places().isEmpty() && "message.enhanced.vishanti.shut".equals(lastActionBarKey(player)),
                "nothing is saved where the book is shut");
        add(player, new BifrostData.Place("Roof", Level.NETHER, new Vec3(0.5, 128.0, 0.5), 0.0F));
        VishantiBook.travelToPlace(player, 0, "Roof");
        check(!PlaceBook.travelling(player) && "message.enhanced.vishanti.shut".equals(lastActionBarKey(player)),
                "and nobody is taken out of it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "items_vishanti_cost")
    public static void theCooldownAndTheCostOfATeleportAreConfigured(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(VishantiConfig.TELEPORT_COOLDOWN, 123);
        f.config(VishantiConfig.TELEPORT_EXHAUSTION, 3.0);
        f.config(VishantiConfig.TELEPORT_CHARGE_TICKS, 7);
        ServerPlayer player = holder(f, 4.5, 4.5, Power.LOKI);
        add(player, new BifrostData.Place("Far", helper.getLevel().dimension(), helper.absoluteVec(new Vec3(15.5, FLOOR, 15.5)), 0.0F));
        float before = player.getFoodData().getExhaustionLevel();
        long now = helper.getLevel().getGameTime();
        VishantiBook.travelToPlace(player, 0, "Far");
        check(PlaceBook.cooldown(player, PlaceBook.Kind.VISHANTI) == 123, "the cooldown is the configured 123 ticks");
        PlaceBook.Journey journey = PlaceBook.journey(player, PlaceBook.Kind.VISHANTI);
        check(journey != null && journey.arrivesAt() == now + 7, "it gathers for the configured 7 ticks");
        assertNear(player.getFoodData().getExhaustionLevel() - before, 3.0, 0.01, "it costs 3.0 exhaustion");
        ServerPlayer creative = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BOOK_OF_THE_VISHANTI.get()));
        Powers.setPower(creative, Power.BILLY);
        add(creative, new BifrostData.Place("Far", helper.getLevel().dimension(), helper.absoluteVec(new Vec3(15.5, FLOOR, 5.5)), 0.0F));
        float creativeBefore = creative.getFoodData().getExhaustionLevel();
        VishantiBook.travelToPlace(creative, 0, "Far");
        check(PlaceBook.travelling(creative) && creative.getFoodData().getExhaustionLevel() == creativeBefore, "a Creative player pays nothing");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void placeNamesAreCleanedAndTheListIsBounded(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = holder(f, 4.5, 4.5, Power.LOKI);
        for (int i = 0; i < BifrostData.MAX_PLACES; i++) {
            VishantiBook.savePlace(player, "P" + i, false);
        }
        check(book(player).isFull(), "24 places");
        VishantiBook.savePlace(player, "One more", false);
        check(book(player).places().size() == BifrostData.MAX_PLACES && "message.enhanced.vishanti.full".equals(lastActionBarKey(player)),
                "no 25th, and the book says so");
        VishantiBook.savePlace(player, "p3", false);
        check(book(player).places().get(3).name().equals("p3") && book(player).places().size() == BifrostData.MAX_PLACES,
                "but an existing name can still be moved");
        VishantiBook.forgetPlace(player, 0, "wrong");
        check(book(player).places().size() == BifrostData.MAX_PLACES, "forgetting checks the name");
        VishantiBook.forgetPlace(player, 0, "P0");
        check(book(player).places().size() == BifrostData.MAX_PLACES - 1 && "message.enhanced.vishanti.forgotten".equals(lastActionBarKey(player)),
                "and forgets the one asked for");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void thePlacesAndTheMessagesTravelAndSurviveASave(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BifrostData data = BifrostData.EMPTY
                .with(new BifrostData.Place("Home", Level.OVERWORLD, new Vec3(1.5, 64.0, -2.5), 90.0F))
                .with(new BifrostData.Place("Base", Level.NETHER, new Vec3(8.5, 70.0, 8.5), 0.0F, true))
                .readyAt(1234L);
        check(data.bases() == 1, "one base");
        BifrostData back = BifrostData.CODEC.parse(NbtOps.INSTANCE,
                BifrostData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
        check(back.equals(data), "the saved form keeps the base flag: " + back);
        check(roundTrip(BifrostData.STREAM_CODEC, data, level.registryAccess()).equals(data), "so does the wire form");
        // A list saved before 3.2 has no base flag: no place of it is a base.
        CompoundTag old = new CompoundTag();
        CompoundTag place = new CompoundTag();
        place.putString("name", "Old");
        place.putString("dimension", "minecraft:overworld");
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(1.0));
        pos.add(DoubleTag.valueOf(2.0));
        pos.add(DoubleTag.valueOf(3.0));
        place.put("pos", pos);
        ListTag list = new ListTag();
        list.add(place);
        old.put("places", list);
        BifrostData loaded = BifrostData.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
        check(loaded.places().size() == 1 && !loaded.places().get(0).base(), "an old list loads, with no base");
        check(data.withoutBases().bases() == 0 && data.withoutBases().places().size() == 2 && data.withoutBases().readyAt() == 1234L,
                "withoutBases keeps the places and the cooldown");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, timeoutTicks = 100, batch = "items_vishanti_hex")
    public static void aHexWallStopsTheBookLikeEveryOtherTeleport(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer wanda = HexTests.raiseHex(f);
        ServerPlayer loki = holder(f, 1.5, 1.5, Power.LOKI);
        Vec3 inside = helper.absoluteVec(new Vec3(10.5, FLOOR, 12.5));
        add(loki, new BifrostData.Place("In", level.dimension(), inside, 0.0F));
        Vec3 start = loki.position();
        VishantiBook.travelToPlace(loki, 0, "In");
        check(PlaceBook.travelling(loki), "the book gathers its light: the wall is asked only when the way opens");
        f.everyTick(60, tick -> {
            PowerTicker.tick(loki);
            if (PlaceBook.travelling(loki)) {
                return false;
            }
            check(loki.position().distanceTo(start) < 1.0E-6, "the Hex does not let a stranger through: " + loki.position());
            check("message.enhanced.vishanti.closed".equals(lastActionBarKey(loki)), "the book says the way closed");
            check(PlaceBook.cooldown(loki, PlaceBook.Kind.VISHANTI) == 0, "and the cooldown is given back");
            check(wanda.isAlive(), "(Wanda is still there)");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = "items_vishanti_boss")
    public static void aFighterInAnArenaCannotLeaveItWithTheBookNorRaiseAWardIntoIt(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer fighter = holder(f, 10.5, 10.5, Power.LOKI);
        TestFight fight = BossFrameworkTests.start(f, BossKind.ULTRON, fighter, BossFrameworkTests.fight(f, BossKind.ULTRON, Containment.FIELD, false));
        check(fight != null, "a fight is on");
        Vec3 outside = helper.absoluteVec(new Vec3(19.5, FLOOR, 19.5));
        add(fighter, new BifrostData.Place("Out", level.dimension(), outside, 0.0F));
        TestKit.sentPackets(fighter);
        VishantiBook.travelToPlace(fighter, 0, "Out");
        check(!PlaceBook.travelling(fighter) && "message.enhanced.boss.held".equals(lastActionBarKey(fighter)),
                "the arena does not let the book take a fighter out");
        TestKit.sentPackets(fighter);
        VishantiBook.savePlace(fighter, "Fort", true);
        check(book(fighter).bases() == 0 && "message.enhanced.vishanti.ward_arena".equals(lastActionBarKey(fighter)),
                "and a ward cannot reach into the arena");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The strength of a ward

    @EnhancedTest(template = ARENA, batch = BOOK)
    public static void theStrongerTheSorcererTheLargerTheWard(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        int wanda = WardStrength.radius(Power.SCARLET_WITCH, 0.0);
        int loki = WardStrength.radius(Power.LOKI, 0.0);
        int billyLow = WardStrength.radius(Power.BILLY, 0.0);
        int billyHigh = WardStrength.radius(Power.BILLY, 1.0);
        check(wanda == VishantiConfig.WARD_RADIUS_WANDA.get() && loki == VishantiConfig.WARD_RADIUS_LOKI.get(), "Wanda's and Loki's are the config's");
        check(billyLow == VishantiConfig.WARD_RADIUS_BILLY_MIN.get() && billyHigh == VishantiConfig.WARD_RADIUS_BILLY_MAX.get(),
                "Billy's runs from the first rank's to the highest's");
        check(wanda >= billyHigh && billyHigh > billyLow && wanda > loki, "Wanda is the strongest: " + wanda + " " + billyHigh + " " + loki);
        int last = 0;
        for (double fraction = 0.0; fraction <= 1.0; fraction += 0.125) {
            int radius = WardStrength.radius(Power.BILLY, fraction);
            check(radius >= last, "Billy's ward never shrinks as he grows");
            last = radius;
        }
        for (Power power : Power.values()) {
            check(sorcerer(power) == (WardStrength.radius(power, 1.0) > 0), power + ": only a sorcerer has a ward");
        }
        // Billy's rank, read through the one accessor: his first rank is 0, his highest 1.
        ServerPlayer billy = f.player(10.5, FLOOR, 10.5, Power.BILLY);
        int max = BillyRanks.maxRank();
        ModAttachments.BUILD_DATA.set(billy, BuildData.EMPTY.withRank(BillyRanks.MIN_RANK));
        assertNear(WardStrength.billyFraction(billy), 0.0, 1.0E-9, "an Apprentice");
        check(WardStrength.radiusOf(billy) == billyLow, "so his ward is the smallest");
        ModAttachments.BUILD_DATA.set(billy, BuildData.EMPTY.withRank(max));
        assertNear(WardStrength.billyFraction(billy), 1.0, 1.0E-9, "at his highest rank");
        check(WardStrength.radiusOf(billy) == billyHigh, "the largest");
        ModAttachments.BUILD_DATA.set(billy, BuildData.EMPTY.withRank((BillyRanks.MIN_RANK + max) / 2));
        double middle = WardStrength.billyFraction(billy);
        check(middle > 0.0 && middle < 1.0 && WardStrength.radiusOf(billy) > billyLow && WardStrength.radiusOf(billy) < billyHigh,
                "in between in between (" + middle + ")");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The ward

    /** Small wards that can be raised again and again: a ward never reaches the test next to the one that made it. */
    private static TestKit.Fixture wardFixture(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(VishantiConfig.WARD_RADIUS_LOKI, 6);
        f.config(VishantiConfig.WARD_RADIUS_WANDA, 7);
        f.config(VishantiConfig.WARD_RADIUS_BILLY_MIN, 5);
        f.config(VishantiConfig.WARD_RADIUS_BILLY_MAX, 8);
        f.config(VishantiConfig.WARD_COOLDOWN, 0);
        return f;
    }

    private static Ward wardOf(ServerLevel level, ServerPlayer owner, String name) {
        WardStore store = WardStore.peek(level);
        if (store != null) {
            for (Ward ward : store.wards()) {
                if (ward.is(owner.getUUID(), name)) {
                    return ward;
                }
            }
        }
        return null;
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void aBaseRaisesAWardRoundItAsLargeAsTheSorcererIsStrong(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        TestKit.sentPackets(loki);
        VishantiBook.savePlace(loki, "Fort", true);
        List<BifrostData.Place> places = book(loki).places();
        check(places.size() == 1 && places.get(0).base() && places.get(0).name().equals("Fort"), "the base is saved as a base: " + places);
        Ward ward = wardOf(level, loki, "Fort");
        check(ward != null && ward.radius() == 6 && ward.center().distanceTo(loki.position()) < 1.0E-6 && ward.awake(),
                "a ward of Loki's size stands where he stood: " + ward);
        check(ward.color() == Power.LOKI.color, "in his colour");
        check("message.enhanced.vishanti.ward_raised".equals(lastActionBarKey(loki)), "and the book says so");
        check(WardManager.protects(level, loki.position().add(5.0, 0, 0)) && !WardManager.protects(level, loki.position().add(6.5, 0, 0)),
                "it reaches its radius");
        check(WardManager.protects(level, loki.position().add(0, 5.0, 0)), "up as well: it is a sphere");

        ServerPlayer wanda = holder(f, 10.5, 10.5, Power.SCARLET_WITCH);
        VishantiBook.savePlace(wanda, "Fort", true);
        check(wardOf(level, wanda, "Fort").radius() == 7, "the Scarlet Witch's is larger");
        ServerPlayer billy = holder(f, 10.5, 10.5, Power.BILLY);
        ModAttachments.BUILD_DATA.set(billy, BuildData.EMPTY.withRank(BillyRanks.MIN_RANK));
        VishantiBook.savePlace(billy, "Fort", true);
        check(wardOf(level, billy, "Fort").radius() == 5, "Billy's, an Apprentice's, is the smallest");
        ModAttachments.BUILD_DATA.set(billy, BuildData.EMPTY.withRank(BillyRanks.maxRank()));
        WardManager.refresh(billy);
        check(wardOf(level, billy, "Fort").radius() == 8, "and grows with his Build Magic rank");

        // What the clients are told.
        WardSyncPayload payload = WardManager.payload(level);
        check(payload.dimension() == level.dimension() && payload.domes().stream().filter(d -> d.radius() == 8).count() == 1,
                "the clients are sent the awake wards of the dimension");
        check(roundTrip(WardSyncPayload.STREAM_CODEC, payload, level.registryAccess()).equals(payload), "and the message travels whole");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void aPlayerKeepsAtMostAFewBasesAndMovesOneByNameAgain(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "One", true);
        VishantiBook.savePlace(loki, "Two", true);
        check(book(loki).bases() == VishantiConfig.MAX_WARDS.get() && VishantiConfig.MAX_WARDS.get() == 2, "two bases by default");
        TestKit.sentPackets(loki);
        VishantiBook.savePlace(loki, "Three", true);
        check(book(loki).bases() == 2 && "message.enhanced.vishanti.ward_max".equals(lastActionBarKey(loki)), "no third, and the book says so");
        check(wardOf(level, loki, "Three") == null, "no ward either");
        VishantiBook.savePlace(loki, "Three", false);
        check(book(loki).places().size() == 3 && wardOf(level, loki, "Three") == null, "but a plain place is always fine");
        Vec3 moved = helper.absoluteVec(new Vec3(8.5, FLOOR, 8.5));
        loki.moveTo(moved.x, moved.y, moved.z);
        VishantiBook.savePlace(loki, "one", true);
        check(book(loki).bases() == 2 && wardOf(level, loki, "One").center().distanceTo(moved) < 1.0E-6, "saving a base's name again moves its ward");
        VishantiBook.savePlace(loki, "Two", false);
        check(book(loki).bases() == 2 && wardOf(level, loki, "Two") != null && wardOf(level, loki, "Two").center().distanceTo(moved) < 1.0E-6,
                "saving a base's name as a plain place moves the base (and its ward) instead of dropping the ward unseen");
        check("message.enhanced.vishanti.ward_raised".equals(lastActionBarKey(loki)), "and says so");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void forgettingTheBaseTakesTheWardAway(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "Fort", true);
        check(wardOf(level, loki, "Fort") != null, "a ward stands");
        VishantiBook.forgetPlace(loki, 0, "Fort");
        check(wardOf(level, loki, "Fort") == null && !WardManager.protects(level, loki.position()), "forgetting the base takes it away");
        check("message.enhanced.vishanti.ward_removed".equals(lastActionBarKey(loki)), "and the book says so");
        VishantiBook.savePlace(loki, "Fort", true);
        check(WardManager.count(level.getServer(), loki.getUUID()) == 1, "one ward is counted");
        int removed = WardManager.removeAll(level.getServer(), loki.getUUID());
        check(removed == 1 && wardOf(level, loki, "Fort") == null, "an operator can take every ward of a player away");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, timeoutTicks = 120, batch = WARD)
    public static void aMonsterInsideTheWardIsPushedOutAndNothingElseIs(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "Fort", true);
        Vec3 center = loki.position();
        Zombie near = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        Zombie deep = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 11.5));
        Zombie outside = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(19.5, FLOOR, 10.5));
        // The arena's ceiling lets the sun through: a zombie may burn by day, and "nobody was hurt" is about the ward alone.
        for (Zombie zombie : List.of(near, deep, outside)) {
            zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
        }
        Pig pig = TestKit.spawnWithNoFreeWill(helper, EntityType.PIG, new Vec3(9.5, FLOOR, 9.5));
        Vec3 pigAt = pig.position();
        Vec3 outsideAt = outside.position();
        float health = near.getHealth();
        f.everyTick(100, tick -> {
            PowerTicker.tick(loki);
            boolean out = near.position().distanceTo(center) > 6.0 && deep.position().distanceTo(center) > 6.0;
            if (!out) {
                return false;
            }
            check(near.isAlive() && deep.isAlive() && near.getHealth() == health && deep.getHealth() == health,
                    "nobody was hurt: " + near.getHealth() + " " + deep.getHealth() + " of " + health + " at tick " + tick + ", by "
                            + (deep.getLastDamageSource() == null ? near.getLastDamageSource() : deep.getLastDamageSource()) + " at "
                            + helper.relativeVec(deep.position()) + ", " + helper.relativeVec(near.position()));
            check(outside.position().distanceTo(outsideAt) < 0.5, "the one outside stays outside, untouched");
            check(pig.position().distanceTo(pigAt) < 0.5 && pig.isAlive(), "a pig inside the ward is not touched");
            check(loki.position().distanceTo(center) < 0.5, "nor is the sorcerer");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, timeoutTicks = 60, batch = WARD)
    public static void bossesAndAFightsCreaturesAreKeptOutToo(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "Fort", true);
        List<EntityType<?>> bosses = List.of(EntityType.WITHER, EntityType.ENDER_DRAGON, EntityType.WARDEN, EntityType.ELDER_GUARDIAN,
                ModEntities.THANOS.get(), ModEntities.ULTRON.get(), ModEntities.APOCALYPSE.get(), ModEntities.ULTRON_SENTRY.get());
        for (EntityType<?> boss : bosses) {
            check(WardManager.keepsOut(boss), boss + " is kept out of a ward too (3.3)");
        }
        check(WardManager.keepsOut(EntityType.ZOMBIE) && WardManager.keepsOut(EntityType.CREEPER) && WardManager.keepsOut(EntityType.SKELETON)
                        && WardManager.keepsOut(EntityType.PHANTOM) && WardManager.keepsOut(EntityType.WITCH),
                "the monsters are kept out");
        check(!WardManager.keepsOut(EntityType.PIG) && !WardManager.keepsOut(EntityType.VILLAGER) && !WardManager.keepsOut(EntityType.IRON_GOLEM)
                        && !WardManager.keepsOut(EntityType.PLAYER),
                "animals, villagers, golems and players are not monsters");
        Mob wither = TestKit.spawnWithNoFreeWill(helper, EntityType.WITHER, new Vec3(12.5, FLOOR + 1.0, 10.5));
        wither.setCustomName(net.minecraft.network.chat.Component.literal("Named"));
        check(!WardManager.exempt(wither), "a boss is never let through, whatever it is called (it is pushed out like any monster)");
        wither.discard();
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void noMonsterSpawnsInsideAWard(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "Fort", true);
        BlockPos inside = BlockPos.containing(loki.position().add(2, 0, 0));
        BlockPos outside = BlockPos.containing(loki.position().add(9, 0, 0));
        check(WardManager.forbidsSpawn(level, EntityType.ZOMBIE, Vec3.atCenterOf(inside)), "a zombie may not spawn inside");
        check(!WardManager.forbidsSpawn(level, EntityType.ZOMBIE, Vec3.atCenterOf(outside)), "but may outside");
        check(!WardManager.forbidsSpawn(level, EntityType.PIG, Vec3.atCenterOf(inside)), "a pig may spawn inside");
        check(WardManager.forbidsSpawn(level, EntityType.WITHER, Vec3.atCenterOf(inside)), "nor a boss (3.3)");
        // The spawn placement check every natural spawn and every spawner goes through.
        MobSpawnEvent.SpawnPlacementCheck placement = new MobSpawnEvent.SpawnPlacementCheck(EntityType.ZOMBIE, level, MobSpawnType.NATURAL,
                inside, level.getRandom(), true);
        NeoForge.EVENT_BUS.post(placement);
        check(placement.getResult() == MobSpawnEvent.SpawnPlacementCheck.Result.FAIL, "the spawn placement of a zombie fails");
        MobSpawnEvent.SpawnPlacementCheck free = new MobSpawnEvent.SpawnPlacementCheck(EntityType.ZOMBIE, level, MobSpawnType.NATURAL,
                outside, level.getRandom(), true);
        NeoForge.EVENT_BUS.post(free);
        check(free.getResult() == MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT, "outside it is left alone");
        // And the finishing of a spawn that got past the placement (a spawner's, a structure's).
        Zombie zombie = EntityType.ZOMBIE.create(level);
        Vec3 at = Vec3.atBottomCenterOf(inside);
        zombie.moveTo(at.x, at.y, at.z);
        EventHooks.finalizeMobSpawn(zombie, level, level.getCurrentDifficultyAt(inside), MobSpawnType.SPAWNER, null);
        check(zombie.isSpawnCancelled(), "a spawner's zombie is turned away");
        Zombie eggs = EntityType.ZOMBIE.create(level);
        eggs.moveTo(at.x, at.y, at.z);
        EventHooks.finalizeMobSpawn(eggs, level, level.getCurrentDifficultyAt(inside), MobSpawnType.COMMAND, null);
        check(!eggs.isSpawnCancelled(), "(a command's is let through; the look will see to it)");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "items_ward_spawns")
    public static void aWardThatIsToldNotToStopSpawnsDoesNot(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        f.config(VishantiConfig.WARD_BLOCKS_SPAWNS, false);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "Fort", true);
        check(!WardManager.forbidsSpawn(level, EntityType.ZOMBIE, loki.position()), "wardBlocksSpawns off: spawns are left to the look");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void wardsAreSavedWithTheWorldAndLoadAgain(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "Fort", true);
        WardStore store = WardStore.get(level);
        check(store.isDirty(), "the store is marked to be saved");
        CompoundTag tag = store.save(new CompoundTag(), level.registryAccess());
        WardStore loaded = WardStore.factory().deserializer().apply(tag, level.registryAccess());
        check(loaded.wards().containsAll(store.wards()) && loaded.wards().size() == store.wards().size(), "what is saved loads again, whole");
        check(WardStore.get(level) == store, "the level keeps one store");
        check(loaded.wards().stream().anyMatch(w -> w.is(loki.getUUID(), "Fort") && w.radius() == 6), "Loki's ward among them");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, timeoutTicks = 100, batch = WARD)
    public static void aWardSleepsWhileItsOwnerIsNoSorcererAndWakesWhenTheyAre(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "Fort", true);
        ServerPlayer owner = loki;
        Powers.setPower(owner, Power.TOMMY);
        WardManager.refresh(owner);
        Ward asleep = wardOf(level, loki, "Fort");
        check(asleep != null && !asleep.awake() && asleep.radius() == 0, "the ward is kept, asleep");
        check(!WardManager.protects(level, loki.position()) && WardManager.payload(level).domes().stream()
                .noneMatch(d -> Math.abs(d.x() - asleep.center().x) < 1.0E-6 && Math.abs(d.z() - asleep.center().z) < 1.0E-6),
                "it protects nothing and the clients are not shown it");
        check(book(loki).bases() == 1, "and the book keeps the base");
        Powers.setPower(owner, Power.SCARLET_WITCH);
        WardManager.refresh(owner);
        check(wardOf(level, loki, "Fort").radius() == 7 && WardManager.protects(level, loki.position()), "a Scarlet Witch's ward wakes, her size");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void operatorsCanListAndRemoveWards(GameTestHelper helper) throws CommandSyntaxException {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "Fort", true);
        List<Component> said = new ArrayList<>();
        CommandSource sink = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                said.add(message);
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack source = new CommandSourceStack(sink, Vec3.ZERO, Vec2.ZERO, level, 4, "test", Component.literal("test"), server, null);
        int listed = server.getCommands().getDispatcher().execute("enhanced ward list", source);
        check(listed >= 1 && said.stream().anyMatch(line -> says(line, "commands.enhanced.ward.line")), "the list names the ward: " + said);
        check(!server.getCommands().getDispatcher().getRoot().getChild("enhanced").getRequirement().test(
                server.createCommandSourceStack().withPermission(0)), "and only operators can use it");
        said.clear();
        int removed = server.getCommands().getDispatcher().execute("enhanced ward remove @a[name=" + loki.getGameProfile().getName() + "]", source);
        check(removed == 1 && wardOf(level, loki, "Fort") == null, "remove takes the ward away");
        check(book(loki).places().size() == 1 && book(loki).bases() == 0, "and the base becomes a plain place, so the ward does not come back");
        WardManager.refresh(loki);
        check(wardOf(level, loki, "Fort") == null, "even after the next refresh");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void everyWorldHasItsOwnWards(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        check(nether != null, "the test server has a Nether");
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        Vec3 spot = new Vec3(5000.5, 100.0, 5000.5);
        add(loki, new BifrostData.Place("Hell", Level.NETHER, spot, 0.0F, true));
        WardManager.refresh(loki);
        check(wardOf(nether, loki, "Hell") != null && wardOf(level, loki, "Hell") == null, "a base in the Nether has its ward in the Nether");
        check(WardManager.protects(nether, spot) && !WardManager.protects(level, spot), "and not in this world");
        WardManager.removeAll(server, loki.getUUID());
        check(wardOf(nether, loki, "Hell") == null, "operators' removal reaches every world");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void aWardWhereNothingIsLoadedDoesNotLoadAnythingAndLooksAtNothing(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        Vec3 far = new Vec3(300000.5, 70.0, 300000.5);
        add(loki, new BifrostData.Place("Far", level.dimension(), far, 0.0F, true));
        WardManager.refresh(loki);
        Ward ward = wardOf(level, loki, "Far");
        check(ward != null, "the ward is kept for when its chunks load");
        for (int i = 0; i < 20; i++) {
            WardManager.tickLevel(level);
        }
        check(!level.hasChunkAt(BlockPos.containing(far)), "looking at it loaded nothing");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Review fixes

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void aWardWhoseBaseIsGoneIsTakenAwayAtLoginAndByIdAlone(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        WardStore.get(level).put(new Ward(loki.getUUID(), "Lost", loki.position(), 6, Power.LOKI.color));
        check(book(loki).bases() == 0 && wardOf(level, loki, "Lost") != null, "a ward stands that the book knows nothing of");
        WardEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(loki));
        check(wardOf(level, loki, "Lost") == null, "logging in takes it away");
        UUID gone = UUID.randomUUID();
        WardStore.get(level).put(new Ward(gone, "Old", loki.position(), 6, Power.LOKI.color));
        check(WardManager.count(level.getServer(), gone) == 1 && WardManager.removeAll(level.getServer(), gone) == 1, "a player who never comes back: removed by id alone");
        check(WardManager.count(level.getServer(), gone) == 0, "and gone");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, timeoutTicks = 60, batch = "items_ward_off")
    public static void turningWardsOffSilencesEveryWardEvenOfOfflineOwners(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        f.config(VishantiConfig.WARDS_ENABLED, false);
        ServerLevel level = helper.getLevel();
        UUID offline = UUID.randomUUID();
        Vec3 center = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        WardStore store = WardStore.get(level);
        store.put(new Ward(offline, "Away", center, 6, Power.LOKI.color));
        f.onClose(() -> WardManager.removeAll(level.getServer(), offline));
        ServerPlayer watcher = f.player(3.5, FLOOR, 3.5);
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
        Vec3 at = zombie.position();
        check(!WardManager.protects(level, center) && !WardManager.forbidsSpawn(level, EntityType.ZOMBIE, center), "the ward protects nothing and refuses no spawn");
        f.everyTick(30, tick -> {
            if (tick < 30) {
                return false;
            }
            check(zombie.position().distanceTo(at) < 0.5 && watcher.isAlive(), "and pushes nobody");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, timeoutTicks = 100, batch = WARD)
    public static void aWardKeepsWhatThePlayerKeepsAndWhatIsBeingCured(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerPlayer loki = holder(f, 10.5, 10.5, Power.LOKI);
        VishantiBook.savePlace(loki, "Fort", true);
        Zombie plain = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(13.5, FLOOR, 10.5));
        Zombie named = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(13.5, FLOOR, 9.0));
        named.setCustomName(Component.literal("Zed"));
        Zombie leashed = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(13.5, FLOOR, 12.0));
        leashed.setLeashedTo(loki, true);
        ZombieVillager curing = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE_VILLAGER,
                new Vec3(10.5, FLOOR, 13.5));
        // (The cure: weakness, then a golden apple.)
        curing.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1000, 0));
        loki.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE));
        curing.mobInteract(loki, InteractionHand.MAIN_HAND);
        check(curing.isConverting(), "the villager is being cured");
        for (Zombie zombie : List.of(plain, named, leashed)) {
            zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
        }
        curing.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000));
        check(!WardManager.exempt(plain) && WardManager.exempt(named) && WardManager.exempt(leashed) && WardManager.exempt(curing),
                "a named, a leashed and a curing monster are let be; a plain one is not");
        Vec3 namedAt = named.position();
        Vec3 curingAt = curing.position();
        f.everyTick(50, tick -> {
            PowerTicker.tick(loki);
            if (tick < 50) {
                return false;
            }
            check(plain.position().distanceTo(loki.position()) > 6.0, "the plain zombie was pushed out");
            check(named.position().distanceTo(namedAt) < 0.5 && curing.position().distanceTo(curingAt) < 0.5, "the others were not moved");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void aMonsterIsNeverSetDownInLavaFireOrOnAnEdge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spot = helper.absolutePos(new BlockPos(5, 2, 5));
        Vec3 c = Vec3.atBottomCenterOf(spot);
        net.minecraft.world.phys.AABB body = new net.minecraft.world.phys.AABB(c.x - 0.3, c.y, c.z - 0.3, c.x + 0.3, c.y + 1.9, c.z + 0.3);
        check(!WardManager.hazardous(level, body), "open air over the floor is fine");
        level.setBlockAndUpdate(spot, Blocks.LAVA.defaultBlockState());
        check(WardManager.hazardous(level, body), "lava in the body is a hazard");
        level.setBlockAndUpdate(spot, Blocks.FIRE.defaultBlockState());
        check(WardManager.hazardous(level, body), "so is fire");
        level.setBlockAndUpdate(spot, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(spot.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
        check(WardManager.hazardous(level, body), "and magma underfoot");
        level.setBlockAndUpdate(spot.below(), Blocks.SMOOTH_STONE.defaultBlockState());
        check(!WardManager.hazardous(level, body), "the floor is back");
        Zombie zombie = TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 8.5));
        Mob phantom = TestKit.spawnWithNoFreeWill(helper, EntityType.PHANTOM, new Vec3(8.5, FLOOR + 3.0, 6.5));
        Mob guardian = TestKit.spawnWithNoFreeWill(helper, EntityType.GUARDIAN, new Vec3(8.5, FLOOR + 3.0, 4.5));
        check(WardManager.needsGround(zombie) && !WardManager.needsGround(phantom) && !WardManager.needsGround(guardian),
                "a walker needs a floor, a flier and a swimmer do not (whatever they are doing at the instant of the look)");
        phantom.discard();
        guardian.discard();
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WARD)
    public static void everyoneIsToldOnlyOfTheWardsNearThemAndTheirOwn(GameTestHelper helper) {
        TestKit.Fixture f = wardFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer viewer = f.player(10.5, FLOOR, 10.5);
        UUID stranger = UUID.randomUUID();
        UUID someone = UUID.randomUUID();
        WardStore store = WardStore.get(level);
        Vec3 near = viewer.position().add(100, 0, 0);
        Vec3 far = viewer.position().add(5000, 0, 0);
        store.put(new Ward(stranger, "Near", near, 20, Power.LOKI.color));
        store.put(new Ward(someone, "Far", far, 20, Power.LOKI.color));
        store.put(new Ward(viewer.getUUID(), "Mine", far.add(0, 0, 5000), 20, Power.BILLY.color));
        f.onClose(() -> {
            WardManager.removeAll(level.getServer(), stranger);
            WardManager.removeAll(level.getServer(), someone);
            WardManager.removeAll(level.getServer(), viewer.getUUID());
        });
        var told = WardManager.payload(level, viewer).domes();
        check(told.stream().anyMatch(d -> Math.abs(d.x() - near.x) < 1.0E-6), "a ward a hundred blocks away is told");
        check(told.stream().noneMatch(d -> Math.abs(d.x() - far.x) < 1.0E-6 && Math.abs(d.z() - far.z) < 1.0E-6), "a stranger's ward five thousand blocks away is not");
        check(told.stream().anyMatch(d -> Math.abs(d.z() - (far.z + 5000)) < 1.0E-6), "the viewer's own is, wherever it is");
        check(WardManager.payload(level).domes().size() >= 3, "the operators' view has them all");
        helper.succeed();
    }
}
