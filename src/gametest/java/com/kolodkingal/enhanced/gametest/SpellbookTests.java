package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.google.gson.JsonObject;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.spellbook.SpellMagic;
import com.kolodkingal.enhanced.spellbook.SpellShots;
import com.kolodkingal.enhanced.spellbook.Spellbook;
import com.kolodkingal.enhanced.spellbook.SpellbookData;
import com.kolodkingal.enhanced.spellbook.SpellbookHatPayload;
import com.kolodkingal.enhanced.spellbook.Spellbooks;
import com.kolodkingal.enhanced.world.CottagePlan;
import com.kolodkingal.enhanced.world.StoneSitePlans;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The spellbooks: each makes a player with no power its caster (a custom hero with the book's definition and his own
 * skin), with the book's spells and nothing more (the Ethereal witch has no drain); the Ethereal witch's hat comes off and
 * on and takes a colour; a hero who uses a book keeps his power; giving the book up gives back the player's own custom
 * hero; the Ethereal Orb and the thrown earth fly and hurt, the Earth Wall fills only free air and crumbles by itself,
 * the growth makes crops grow; and the three books lie on their lecterns in their rooms. Server-safe: no client class is
 * loaded.
 */
public final class SpellbookTests {
    private static final String BATCH = "spellbooks";

    /** {@code ticks} server ticks of his powers (nothing ticks a player without a client), then {@code after}. */
    private static void run(GameTestHelper helper, ServerPlayer player, int ticks, Runnable after) {
        for (int i = 1; i <= ticks; i++) {
            helper.runAfterDelay(i, () -> {
                player.tickCount++;
                PowerTicker.tick(player);
            });
        }
        helper.runAfterDelay(ticks + 1, after);
    }

    private static void use(ServerPlayer player, Item book, boolean sneak) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(book));
        player.setShiftKeyDown(sneak);
        book.use(player.level(), player, InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(false);
    }

    private static void press(ServerPlayer player, int key, boolean sneak) {
        AbilityHandler.onKey(player, key, true, sneak);
        AbilityHandler.onKey(player, key, false, sneak);
    }

    /** The context of a message from this player. */
    private static PacketContext ctx(ServerPlayer player) {
        return new PacketContext() {
            @Override
            public net.minecraft.world.entity.player.Player player() {
                return player;
            }

            @Override
            public void enqueueWork(Runnable work) {
                work.run();
            }

            @Override
            public boolean isClientbound() {
                return false;
            }

            @Override
            public void disconnect(net.minecraft.network.chat.Component reason) {
            }
        };
    }

    private static Zombie zombie(GameTestHelper helper, double x, double z) {
        Zombie zombie = TestKit.spawn(helper, EntityType.ZOMBIE, new Vec3(x, FLOOR, z));
        zombie.setNoAi(true);
        zombie.setPersistenceRequired();
        return zombie;
    }

    // ---- the Book of Ethereal Witchcraft ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theEtherealBookMakesAWitchWithAgathasMagicButNoDrain(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        use(player, ModItems.BOOK_OF_ETHEREAL_WITCHCRAFT.get(), false);
        PowerData data = Powers.get(player);
        check(data.power() == Power.CUSTOM && Spellbook.of(player) == Spellbook.ETHEREAL, "she is the Ethereal witch");
        CustomHero hero = ModAttachments.CUSTOM_HERO.get(player);
        check(CustomHero.LOOK_OWN.equals(hero.look()), "she keeps her own skin");
        for (HeroAbility a : List.of(HeroAbility.AGATHA_PURPLE_BEAM, HeroAbility.AGATHA_SHIELD, HeroAbility.AGATHA_RUNES,
                HeroAbility.AGATHA_BODY)) {
            check(data.has(a), "she has Agatha's " + a);
        }
        check(!data.has(HeroAbility.AGATHA_DRAIN), "but not the power drain");
        check(hero.pick(PowerModule.FLIGHT) != null, "she flies");
        HeroKit kit = data.kit();
        check(kit != null && kit.plain(3) != null && kit.plain(3).module() == PowerModule.MAGIC
                && "orb".equals(hero.pick(PowerModule.MAGIC).option()), "V is the Ethereal Orb where the drain was");
        check(kit.plain(1).ability() == HeroAbility.AGATHA_PURPLE_BEAM && kit.plain(4).ability() == HeroAbility.AGATHA_SHIELD
                && kit.plain(5).ability() == HeroAbility.AGATHA_RUNES, "Agatha's beam, shield and runes on her keys");
        check("witch_hat".equals(hero.layers().hat().style()) && hero.layers().hat().color() == SpellbookData.DEFAULT_HAT,
                "the book puts Agatha's hat on her");
        check(CustomHeroes.name(player).getString().equals(Spellbook.ETHEREAL.title().getString()), "her panel names her");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theEtherealWitchsHatComesOffGoesOnAndTakesAColour(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        Item book = ModItems.BOOK_OF_ETHEREAL_WITCHCRAFT.get();
        use(player, book, false);
        check(Spellbooks.wearsHat(player), "the hat is on");
        use(player, book, false);
        check(!Spellbooks.wearsHat(player) && Spellbook.of(player) == Spellbook.ETHEREAL, "using the book again takes it off");
        use(player, book, true);
        check(!Spellbooks.wearsHat(player) && Spellbook.of(player) == Spellbook.ETHEREAL,
                "Sneak + use opens the screen (on the client) and changes nothing here");
        int green = Spellbooks.HAT_COLORS.get(8);
        SpellbookHatPayload.handle(new SpellbookHatPayload(SpellbookHatPayload.COLOR, 8), ctx(player));
        check(Spellbooks.wearsHat(player) && ModAttachments.CUSTOM_HERO.get(player).layers().hat().color() == green,
                "a colour chosen puts the hat back on in that colour");
        SpellbookHatPayload.handle(new SpellbookHatPayload(SpellbookHatPayload.COLOR, 99), ctx(player));
        check(ModAttachments.CUSTOM_HERO.get(player).layers().hat().color() == green, "a colour that is no swatch changes nothing");
        use(player, book, false);
        use(player, book, false);
        check(Spellbooks.wearsHat(player) && ModAttachments.CUSTOM_HERO.get(player).layers().hat().color() == green,
                "off and on again, the hat keeps its colour");
        check(ModAttachments.SPELLBOOK.get(player).hatColor() == green, "the colour is saved with her");
        check(Spellbook.of(player) == Spellbook.ETHEREAL, "the hat changes nothing of her magic");
        SpellbookHatPayload.handle(new SpellbookHatPayload(SpellbookHatPayload.RENOUNCE, 0), ctx(player));
        check(Powers.power(player) == Power.NONE, "the screen's last button gives the book up");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theEtherealOrbFliesAndHurts(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(4.5, FLOOR, 10.5);
        use(player, ModItems.BOOK_OF_ETHEREAL_WITCHCRAFT.get(), false);
        Zombie zombie = zombie(helper, 12.5, 10.5);
        float before = zombie.getHealth();
        TestKit.lookAt(player, zombie);
        press(player, 3, false);
        check(SpellShots.inFlight(player, SpellShots.Kind.ORB) == 1, "V throws an orb");
        run(helper, player, 20, () -> {
            check(SpellShots.inFlight(player, SpellShots.Kind.ORB) == 0, "it struck");
            check(zombie.getHealth() < before, "the orb hurt the zombie: " + zombie.getHealth());
            helper.succeed();
        });
    }

    // ---- the Book of Eldritch Witchcraft ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theEldritchBookGivesStrangesSpells(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        use(player, ModItems.BOOK_OF_ELDRITCH_WITCHCRAFT.get(), false);
        PowerData data = Powers.get(player);
        check(Spellbook.of(player) == Spellbook.ELDRITCH, "he is the Eldritch sorcerer");
        HeroKit kit = data.kit();
        check(kit != null, "his kit");
        check(kit.plain(1).ability() == HeroAbility.STRANGE_WHIP, "R: the Eldritch Whip");
        check(kit.sneak(1) != null && kit.sneak(1).isAlt(), "Sneak + R: the whip's grapple");
        check(kit.plain(2).module() == PowerModule.MAGIC, "G: eldritch bolts");
        check(kit.plain(3).ability() == HeroAbility.STRANGE_SHIELDS, "V: the Shields of the Seraphim");
        check(kit.plain(4) != null && kit.plain(4).picked() == HeroAbility.STRANGE_MIRROR, "B: the Mirror Dimension");
        check(kit.sneak(4) != null && kit.sneak(4).picked() == HeroAbility.STRANGE_ASTRAL, "Sneak + B: the astral form");
        check(kit.plain(5).ability() == HeroAbility.STRANGE_PORTAL, "N: the portals");
        check(data.has(HeroAbility.STRANGE_PORTAL) && !data.has(HeroAbility.STRANGE_EYE), "no Eye of Agamotto");
        check(CustomHero.LOOK_OWN.equals(ModAttachments.CUSTOM_HERO.get(player).look())
                && ModAttachments.CUSTOM_HERO.get(player).layers().hat().isNone(), "his own skin, and no hat");
        use(player, ModItems.BOOK_OF_ELDRITCH_WITCHCRAFT.get(), false);
        check(Spellbook.of(player) == Spellbook.ELDRITCH, "using it again only says what he is");
        use(player, ModItems.BOOK_OF_ELDRITCH_WITCHCRAFT.get(), true);
        check(Powers.power(player) == Power.NONE, "Sneak + use gives it up");
        helper.succeed();
    }

    // ---- the Book of Green Witchcraft ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theGreenBookGivesTheGreenMagic(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        use(player, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        PowerData data = Powers.get(player);
        CustomHero hero = ModAttachments.CUSTOM_HERO.get(player);
        check(Spellbook.of(player) == Spellbook.GREEN, "she is the Green witch");
        HeroKit kit = data.kit();
        check(kit.plain(1).module() == PowerModule.EARTH && "throw".equals(hero.pick(PowerModule.EARTH).option()), "R: thrown earth");
        check(kit.sneak(1) != null && kit.sneak(1).module() == PowerModule.EARTH && "wall".equals(kit.sneak(1).action()),
                "Sneak + R: the Earth Wall");
        check(kit.plain(2).module() == PowerModule.NATURE, "G: growth");
        check(kit.plain(3).ability() == HeroAbility.AGATHA_RUNES && hero.pick(HeroAbility.AGATHA_RUNES).strength() < 5,
                "V: Agatha's runes, weaker");
        check(hero.pick(PowerModule.HEALING) != null && kit.plain(4) == null, "healing needs no key");
        check(!data.has(HeroAbility.AGATHA_DRAIN) && !data.has(HeroAbility.AGATHA_PURPLE_BEAM), "nothing else of Agatha's");
        check((Spellbook.GREEN.color >> 8 & 0xFF) > (Spellbook.GREEN.color >> 16 & 0xFF), "her magic is green");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void theEarthWallFillsOnlyFreeAirAndCrumbles(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 6.5);
        use(player, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        TestKit.look(player, 0.0F, 0.0F); // (south)
        ServerLevel level = helper.getLevel();
        Direction facing = player.getDirection();
        BlockPos middle = player.blockPosition().relative(facing, SpellMagic.WALL_DISTANCE);
        BlockPos built = middle.relative(facing.getClockWise());
        level.setBlock(built, Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
        List<BlockPos> raised = SpellMagic.raise(player, level, facing, 3, 30);
        Block wall = ModBlocks.EARTH_WALL.get();
        check(!raised.isEmpty() && raised.stream().allMatch(p -> level.getBlockState(p).is(wall)), "the wall stands: " + raised);
        check(raised.contains(middle) && raised.contains(middle.above()), "two blocks high in front of her");
        check(level.getBlockState(built).is(Blocks.OAK_PLANKS), "a block someone built is never replaced");
        check(raised.stream().noneMatch(p -> p.equals(built)), "nor counted");
        helper.runAfterDelay(45, () -> {
            check(raised.stream().allMatch(p -> level.getBlockState(p).isAir()), "the wall has crumbled away");
            check(level.getBlockState(built).is(Blocks.OAK_PLANKS), "the built block is still there");
            // and Sneak + R raises it with the power's own numbers
            press(player, 1, true);
            check(level.getBlockState(middle).is(wall), "Sneak + R raises the Earth Wall");
            level.setBlock(middle, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(middle.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void thrownEarthHurtsAndKnocksBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(4.5, FLOOR, 10.5);
        use(player, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        Zombie zombie = zombie(helper, 10.5, 10.5);
        float before = zombie.getHealth();
        double startX = zombie.getX();
        TestKit.lookAt(player, zombie);
        press(player, 1, false);
        check(SpellShots.inFlight(player, SpellShots.Kind.CLOD) == 1, "R throws a clod of earth");
        run(helper, player, 15, () -> {
            check(zombie.getHealth() < before, "the clod hurt the zombie");
            check(zombie.getDeltaMovement().x > 0.1 || zombie.getX() > startX + 0.1,
                    "and knocked it back: " + zombie.getDeltaMovement() + " " + (zombie.getX() - startX));
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void growthMakesTheCropsGrow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        use(player, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        ServerLevel level = helper.getLevel();
        BlockPos crop = player.blockPosition().east(2);
        level.setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(crop, Blocks.WHEAT.defaultBlockState(), Block.UPDATE_ALL);
        press(player, 2, false);
        BlockState after = level.getBlockState(crop);
        check(after.is(Blocks.WHEAT) && after.getValue(CropBlock.AGE) > 0, "G makes the wheat grow: " + after);
        check(level.getBlockState(crop.below()).is(Blocks.FARMLAND), "and changes no other block");
        helper.succeed();
    }

    // ---- who may use a book, and giving it up ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void aHeroWhoUsesABookKeepsHisPower(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Item book : List.of(ModItems.BOOK_OF_ETHEREAL_WITCHCRAFT.get(), ModItems.BOOK_OF_ELDRITCH_WITCHCRAFT.get(),
                ModItems.BOOK_OF_GREEN_WITCHCRAFT.get())) {
            for (Power power : List.of(Power.AGATHA, Power.DOCTOR_STRANGE, Power.MAGNETO)) {
                ServerPlayer player = f.player(8.5, FLOOR, 8.5, power);
                use(player, book, false);
                use(player, book, true);
                check(Powers.power(player) == power, power + " keeps his power with " + book);
            }
        }
        ServerPlayer custom = f.player(8.5, FLOOR, 8.5);
        CustomHero own = CustomHeroRules.sanitize(CustomHero.EMPTY.withName("Mine").withPowers(List.of(
                new CustomHero.Pick("flight", 3, "jet", -1))).withKeys(List.of("flight", "", "", "", "")),
                CustomHeroRules.Limits.DEFAULT).hero();
        CustomHeroes.become(custom, own);
        use(custom, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        check(Powers.power(custom) == Power.CUSTOM && Spellbook.of(custom) == null
                && ModAttachments.CUSTOM_HERO.get(custom).equals(own), "a hero of his own making keeps him too");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void givingTheBookUpGivesBackTheHeroOfHisOwnMaking(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(8.5, FLOOR, 8.5);
        CustomHero own = CustomHeroRules.sanitize(CustomHero.EMPTY.withName("Mine").withPowers(List.of(
                new CustomHero.Pick("flight", 3, "jet", -1))).withKeys(List.of("flight", "", "", "", "")),
                CustomHeroRules.Limits.DEFAULT).hero();
        CustomHeroes.become(player, own);
        Powers.setPower(player, Power.NONE);
        use(player, ModItems.BOOK_OF_ELDRITCH_WITCHCRAFT.get(), false);
        check(Spellbook.of(player) == Spellbook.ELDRITCH, "with no power he becomes the book's caster");
        use(player, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), false);
        check(Spellbook.of(player) == Spellbook.GREEN, "another book's caster may change books");
        check(ModAttachments.SPELLBOOK.get(player).stash().equals(own), "his own hero is kept aside, once");
        use(player, ModItems.BOOK_OF_GREEN_WITCHCRAFT.get(), true);
        check(Powers.power(player) == Power.NONE, "Sneak + use gives the book up");
        check(ModAttachments.CUSTOM_HERO.get(player).equals(own), "and his own hero is his again (for the Hero Creator)");
        check(ModAttachments.SPELLBOOK.get(player).stash().isEmpty(), "nothing is kept aside any more");
        check(!player.mayFly() || player.isCreative(), "none of the book's powers stays");
        helper.succeed();
    }

    // ---- where the books are ----

    private static Map<BlockPos, CompoundTag> lecterns(MinecraftServer server, String template) throws IOException {
        ResourceLocation file = Enhanced.id("structure/" + template + ".nbt");
        CompoundTag root;
        try (InputStream in = server.getResourceManager().getResourceOrThrow(file).open()) {
            root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
        Map<BlockPos, CompoundTag> books = new HashMap<>();
        for (Tag t : root.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            CompoundTag state = palette.getCompound(b.getInt("state"));
            if ("minecraft:lectern".equals(state.getString("Name")) && b.contains("nbt")
                    && "true".equals(state.getCompound("Properties").getString("has_book"))) {
                ListTag pos = b.getList("pos", Tag.TAG_INT);
                books.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), b.getCompound("nbt").getCompound("Book"));
            }
        }
        return books;
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theBooksLieOnLecternsInTheirRooms(GameTestHelper helper) throws IOException {
        MinecraftServer server = helper.getLevel().getServer();
        Map<BlockPos, CompoundTag> cottage = lecterns(server, "agatha_cottage/house");
        BlockPos agatha = new BlockPos(4, CottagePlan.UPSTAIRS, 9);
        BlockPos rio = new BlockPos(17, CottagePlan.UPSTAIRS, 8);
        check(cottage.size() == 2, "two books in the cottage: " + cottage.keySet());
        check("enhanced:book_of_ethereal_witchcraft".equals(cottage.get(agatha).getString("id")), "Ethereal in Agatha's room");
        check(agatha.distManhattan(CottagePlan.BEDS.get(0)) <= 6, "by Agatha's bed");
        check("enhanced:book_of_green_witchcraft".equals(cottage.get(rio).getString("id")), "Green in Rio's room (Death's)");
        check(rio.distManhattan(CottagePlan.BEDS.get(2)) <= 6, "by Rio's bed");
        Map<BlockPos, CompoundTag> sanctum = lecterns(server, "sanctum/house");
        check(sanctum.size() == 1 && sanctum.values().iterator().next().getString("id").equals("enhanced:book_of_eldritch_witchcraft"),
                "Eldritch in the Sanctum's library: " + sanctum.keySet());
        check(sanctum.keySet().iterator().next().getY() == StoneSitePlans.Sanctum.FLOOR + 7, "on the library's floor");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void everySpellbookWordIsInBothLanguages(GameTestHelper helper) throws IOException {
        for (String name : List.of("en_us", "he_il")) {
            JsonObject lang = TestKit.lang(name);
            for (Spellbook book : Spellbook.values()) {
                for (String key : List.of("item.enhanced." + book.itemId(), "item.enhanced." + book.itemId() + ".desc",
                        "item.enhanced." + book.itemId() + ".powers", "item.enhanced." + book.itemId() + ".use",
                        "spellbook.enhanced." + book.id + ".caster", "spellbook.enhanced." + book.id + ".awaken")) {
                    check(lang.has(key), name + ": " + key);
                }
            }
            for (String key : List.of("block.enhanced.earth_wall", "spellbook.enhanced.broadcast", "screen.enhanced.spellbook_hat.title",
                    "screen.enhanced.spellbook_hat.toggle", "screen.enhanced.spellbook_hat.renounce", "message.enhanced.spellbook.hat_on",
                    "message.enhanced.spellbook.hat_off", "message.enhanced.spellbook.renounced", "message.enhanced.spellbook.has_power",
                    "commands.enhanced.spellbook.set")) {
                check(lang.has(key), name + ": " + key);
            }
        }
        helper.succeed();
    }
}
