package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.block.EternityAltarBlock;
import com.kolodkingal.enhanced.block.EternityAltarBlockEntity;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.EternityConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.eternity.EternityCommand;
import com.kolodkingal.enhanced.eternity.EternityEntity;
import com.kolodkingal.enhanced.eternity.EternityEvents;
import com.kolodkingal.enhanced.eternity.EternityProgress;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.kolodkingal.enhanced.eternity.Wishes;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.network.EternityScreenPayload;
import com.kolodkingal.enhanced.network.EternityWishPayload;
import com.kolodkingal.enhanced.odin.GungnirStand;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.world.ShrinePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Eternity (3.8, lane L8, PLAN 10.2-10.5): the shrine's template holds its points (the altar on the dais, the statues
 * on their plinths with their names, the lectern with the book); the altar with Stormbreaker takes its holder to the
 * realm after 60 ticks, at the arrival, facing Eternity; without a key nothing; walking away cancels; a boss fight's
 * participant is refused; the mirror lies at y 63 everywhere sampled; Eternity is made once. The wish: a stack of
 * diamonds; a command block refused (a forged payload too); a site stone through the ledger (world mode: a second
 * player refused with the ledger's line); Gungnir refused while the world has its bearer, given and recorded when not;
 * Mjolnir refused to the unworthy, claimed by the worthy; a second wish refused, {@code reset} gives it back; the
 * return to the exact spot and facing; a player logged out in the realm sent home at login; Love: the power, 32 health,
 * a Stormbreaker if none; the config's one item and no Love. Batches {@code eternity}, {@code eternity_wish} (Gungnir's
 * bearer put back when it ends), {@code eternity_cfg} (the ledger's world mode, the wish's options) and {@code
 * eternity_boss} (a boss fight's stand-in).
 */
public final class EternityTests {
    private static final String BATCH = "eternity";
    private static final String WISH = "eternity_wish";
    private static final String BOSS = "eternity_boss";
    private static final String CFG = "eternity_cfg";

    // ---- helpers ----

    /** A player in the realm, come from where he stands in the arena (his way home saved). */
    static ServerPlayer traveller(TestKit.Fixture f, double x, double z, Power power) {
        EternityTestSupport.ensureRealm(f.level().getServer());
        ServerPlayer player = f.player(x, FLOOR, z, power);
        check(EternityRealm.enter(player, player.blockPosition()), "taken to the realm");
        return player;
    }

    private static int count(ServerPlayer player, Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    // ---- the shrine ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theShrineHoldsItsPointsStatuesAndBook(GameTestHelper helper) throws IOException {
        MinecraftServer server = helper.getLevel().getServer();
        ResourceLocation file = Enhanced.id("structure/eternity_shrine/shrine.nbt");
        CompoundTag root;
        try (InputStream in = server.getResourceManager().getResourceOrThrow(file).open()) {
            root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
        Map<BlockPos, String> names = new HashMap<>();
        Map<BlockPos, CompoundTag> data = new HashMap<>();
        for (Tag t : root.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            BlockPos at = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            names.put(at, palette.getCompound(b.getInt("state")).getString("Name"));
            if (b.contains("nbt")) {
                data.put(at, b.getCompound("nbt"));
            }
        }
        check("enhanced:eternity_altar".equals(names.get(ShrinePlan.ALTAR)), "the altar on the dais");
        check(ShrinePlan.ALTAR.getY() == ShrinePlan.FLOOR + 4, "three steps up from the plaza");
        check(!"minecraft:air".equals(names.get(ShrinePlan.ALTAR.below())), "the dais under the altar");
        check(names.get(ShrinePlan.STAIRS) != null && names.get(ShrinePlan.STAIRS).endsWith("_stairs"),
                "the stair's foot: " + names.get(ShrinePlan.STAIRS));
        check("minecraft:lectern".equals(names.get(ShrinePlan.LECTERN)), "the lectern by the stair");
        CompoundTag book = data.get(ShrinePlan.LECTERN).getCompound("Book");
        check("minecraft:written_book".equals(book.getString("id")), "the book on the lectern");
        ListTag pages = book.getCompound("components").getCompound("minecraft:written_book_content")
                .getList("pages", Tag.TAG_STRING);
        check(pages.size() == 9 && pages.getString(0).contains("book.enhanced.cosmic_entities.page1"),
                "nine translated pages, one per entity and the way in");
        String statue = names.get(ShrinePlan.ETERNITY_STATUE);
        check(statue != null && !"minecraft:air".equals(statue), "Eternity's statue's foot");
        int glass = 0;
        int horns = 0;
        for (Map.Entry<BlockPos, String> e : names.entrySet()) {
            BlockPos p = e.getKey();
            if (p.getZ() <= ShrinePlan.ETERNITY_STATUE.getZ() && Math.abs(p.getX() - 23) <= 9) {
                glass += e.getValue().endsWith("stained_glass") ? 1 : 0;
                horns += e.getValue().equals("minecraft:chain") || e.getValue().equals("minecraft:iron_bars") ? 1 : 0;
            }
        }
        check(glass >= 20 && horns >= 8, "his starry glass (" + glass + ") and his horns (" + horns + ")");
        Set<String> signs = new HashSet<>();
        for (CompoundTag nbt : data.values()) {
            if (nbt.contains("front_text")) {
                for (Tag line : nbt.getCompound("front_text").getList("messages", Tag.TAG_STRING)) {
                    String text = line.getAsString();
                    if (text.contains("sign.enhanced.shrine.")) {
                        signs.add(text.substring(text.indexOf("sign.enhanced.shrine.") + 21, text.lastIndexOf('"')));
                    }
                }
            }
        }
        for (ShrinePlan.Statue s : ShrinePlan.STATUES) {
            check(!"minecraft:air".equals(names.getOrDefault(s.plinth(), "minecraft:air")), s.id() + "'s plinth");
            check(names.containsKey(s.plinth().above(3)), s.id() + " stands on it");
            check(signs.contains(s.id()), s.id() + "'s name on a sign: " + signs);
        }
        helper.succeed();
    }

    // ---- the way in ----

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void theAltarTakesTheKeyHolderToTheRealm(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel realm = EternityTestSupport.ensureRealm(helper.getLevel().getServer());
        BlockPos altar = helper.absolutePos(new BlockPos(10, 2, 10));
        helper.getLevel().setBlockAndUpdate(altar, ModBlocks.ETERNITY_ALTAR.get().defaultBlockState());
        ServerPlayer thor = f.player(10.5, FLOOR, 12.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        EternityAltarBlock.use(thor, helper.getLevel(), altar);
        EternityAltarBlockEntity be = (EternityAltarBlockEntity) helper.getLevel().getBlockEntity(altar);
        check(be != null && be.charging() && be.key().is(ModItems.STORMBREAKER.get()), "the axe's copy floats");
        check(thor.getMainHandItem().is(ModItems.STORMBREAKER.get()), "the axe stays in his hand");
        helper.runAfterDelay(EternityAltarBlockEntity.chargeTicks() - 2, () ->
                check(!EternityRealm.in(thor), "not before the charge is done"));
        helper.runAfterDelay(EternityAltarBlockEntity.chargeTicks() + 3, () -> {
            check(EternityRealm.in(thor) && thor.level() == realm, "he is in Eternity's realm");
            assertNear(thor.position().distanceTo(EternityRealm.ARRIVAL), 0.0, 1.0E-6, "at the arrival");
            assertNear(thor.getYRot(), EternityRealm.ARRIVAL_YAW, 1.0E-3, "facing north, toward Eternity");
            check(!be.charging(), "the altar is quiet again");
            EternityProgress progress = ModAttachments.ETERNITY.get(thor);
            check(progress.returnTo().isPresent() && progress.returnTo().get().dimension() == Level.OVERWORLD
                    && progress.visits() == 1, "his way home saved, one visit");
            check(realm.getBlockState(EternityRealm.WISHING_STONE).is(ModBlocks.ETERNITY_ALTAR.get()),
                    "the wishing stone stands before him");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void withoutAKeyTheAltarWaits(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        BlockPos altar = helper.absolutePos(new BlockPos(10, 2, 10));
        helper.getLevel().setBlockAndUpdate(altar, ModBlocks.ETERNITY_ALTAR.get().defaultBlockState());
        ServerPlayer plain = f.player(10.5, FLOOR, 12.5);
        plain.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        EternityAltarBlock.use(plain, helper.getLevel(), altar);
        EternityAltarBlockEntity be = (EternityAltarBlockEntity) helper.getLevel().getBlockEntity(altar);
        check(be != null && !be.charging(), "no key, no charge");
        // Someone else's Gungnir is no key of his.
        ServerPlayer odin = f.player(12.5, FLOOR, 12.5, Power.ODIN);
        plain.setItemInHand(InteractionHand.MAIN_HAND, GungnirItem.bound(odin, 1));
        check(!EternityAltarBlock.isKey(plain, plain.getMainHandItem()), "another's Gungnir opens nothing");
        check(EternityAltarBlock.isKey(odin, GungnirItem.bound(odin, 1)), "but his own does");
        for (Item key : List.of(ModItems.MJOLNIR.get(), ModItems.STORMBREAKER.get(), ModItems.HOFUND.get())) {
            check(EternityAltarBlock.isKey(plain, new ItemStack(key)), key + " is a key");
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void walkingAwayCancelsTheJourney(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        EternityTestSupport.ensureRealm(helper.getLevel().getServer());
        BlockPos altar = helper.absolutePos(new BlockPos(10, 2, 10));
        helper.getLevel().setBlockAndUpdate(altar, ModBlocks.ETERNITY_ALTAR.get().defaultBlockState());
        ServerPlayer thor = f.player(10.5, FLOOR, 12.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        EternityAltarBlock.use(thor, helper.getLevel(), altar);
        EternityAltarBlockEntity be = (EternityAltarBlockEntity) helper.getLevel().getBlockEntity(altar);
        check(be != null && be.charging(), "it charges");
        helper.runAfterDelay(10, () -> {
            Vec3 away = helper.absoluteVec(new Vec3(10.5, FLOOR, 17.5));
            thor.teleportTo(away.x, away.y, away.z);
        });
        helper.runAfterDelay(14, () -> check(!be.charging(), "five blocks away: cancelled"));
        helper.runAfterDelay(EternityAltarBlockEntity.chargeTicks() + 5, () -> {
            check(!EternityRealm.in(thor), "and he stays");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = BOSS)
    public static void aBossFightersKeyOpensNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer thor = f.player(4.5, FLOOR, 4.5, Power.THOR);
        ServerLevel level = helper.getLevel();
        BossTestSupport.TestFight fight = new BossTestSupport.TestFight(level, BossKind.ULTRON,
                new BossTestSupport.TestArena(level, helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)), 8.0,
                        BossArena.Containment.NONE, false));
        BossFights.register(BossKind.ULTRON, new BossTestSupport.TestLane(BossKind.ULTRON, p -> fight));
        check(BossFights.start(BossKind.ULTRON, thor, false) && BossFights.isParticipant(thor), "he fights a boss");
        BlockPos altar = helper.absolutePos(new BlockPos(4, 2, 6));
        level.setBlockAndUpdate(altar, ModBlocks.ETERNITY_ALTAR.get().defaultBlockState());
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        EternityAltarBlock.use(thor, level, altar);
        check(!((EternityAltarBlockEntity) level.getBlockEntity(altar)).charging(), "the altar does not answer him");
        helper.succeed();
    }

    // ---- the realm ----

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theMirrorLiesAtSixtyThreeEverywhere(GameTestHelper helper) {
        ServerLevel realm = EternityTestSupport.ensureRealm(helper.getLevel().getServer());
        for (int[] xz : new int[][]{{0, 0}, {0, -48}, {37, 211}, {-500, 90}, {1234, -777}}) {
            BlockPos mirror = new BlockPos(xz[0], EternityRealm.MIRROR_Y, xz[1]);
            realm.getChunk(mirror);
            check(realm.getBlockState(mirror).is(ModBlocks.ETERNITY_MIRROR.get()), "the mirror at " + mirror);
            check(realm.getBlockState(mirror.above()).isAir() && realm.getBlockState(mirror.below()).isAir(),
                    "air above it and nothing below it at " + mirror);
        }
        check(realm.dimensionType().fixedTime().isPresent() && realm.dimensionType().ambientLight() == 1.0F,
                "a still, bright realm");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void eternityIsMadeOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer one = traveller(f, 4.5, 4.5, Power.NONE);
        ServerPlayer two = traveller(f, 6.5, 4.5, Power.NONE);
        ServerLevel realm = one.serverLevel();
        EternityEntity first = EternityRealm.keepEternity(realm);
        EternityEntity again = EternityRealm.keepEternity(realm);
        List<EternityEntity> all = realm.getEntities(ModEntities.ETERNITY.get(),
                new AABB(EternityRealm.ETERNITY).inflate(200.0), e -> e.isAlive());
        check(all.size() == 1 && first == again && two.level() == realm, "one Eternity for two travellers: "
                + all.size());
        check(first.blockPosition().equals(EternityRealm.ETERNITY) && first.isInvulnerable() && !first.isPickable()
                && !first.shouldBeSaved(), "seated at his place, invulnerable, never saved");
        helper.succeed();
    }

    // ---- the wish ----

    @EnhancedTest(template = ARENA, batch = WISH)
    public static void aWishGivesAStackOfDiamonds(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = traveller(f, 4.5, 4.5, Power.NONE);
        check(Wishes.mayWish(player), "a wish to make");
        EternityRealm.wish(player, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(Items.DIAMOND));
        check(count(player, Items.DIAMOND) == 64, "64 diamonds: " + count(player, Items.DIAMOND));
        check(!Wishes.mayWish(player) && ModAttachments.ETERNITY.get(player).lastWish() == EternityProgress.ITEM,
                "his wish is spent");
        check(EternityRealm.goingHome(player), "he goes home in a moment");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WISH)
    public static void aCommandBlockIsNeverGranted(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = traveller(f, 4.5, 4.5, Power.NONE);
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        EternityRealm.wish(player, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(Items.COMMAND_BLOCK));
        answer(new EternityWishPayload(EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(Items.BARRIER)),
                player);
        answer(new EternityWishPayload(EternityWishPayload.ITEM, ResourceLocation.withDefaultNamespace("no_such")),
                player);
        check(count(player, Items.COMMAND_BLOCK) == 0 && count(player, Items.BARRIER) == 0, "nothing of it");
        check(Wishes.mayWish(player), "and the wish is still his");
        check(Wishes.forbidden(Items.STRUCTURE_BLOCK, player.serverLevel().enabledFeatures())
                && Wishes.forbidden(Items.DEBUG_STICK, player.serverLevel().enabledFeatures())
                && !Wishes.forbidden(Items.DIAMOND, player.serverLevel().enabledFeatures()), "the deny rule");
        // A forged wish from outside the realm does nothing.
        ServerPlayer outside = f.player(8.5, FLOOR, 8.5);
        answer(new EternityWishPayload(EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(Items.DIAMOND)),
                outside);
        check(count(outside, Items.DIAMOND) == 0, "no wish outside the realm");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = CFG)
    public static void aSiteStoneComesThroughTheLedger(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        f.config(StonesConfig.UNIQUENESS, StonesConfig.Uniqueness.WORLD);
        f.onClose(() -> StoneLedger.reset(server));
        StoneLedger.reset(server);
        ServerPlayer first = traveller(f, 4.5, 4.5, Power.THOR);
        ServerPlayer second = traveller(f, 6.5, 4.5, Power.THOR);
        List<EternityScreenPayload.Lock> before = Wishes.locks(second);
        check(before.stream().noneMatch(l -> l.item().equals(BuiltInRegistries.ITEM.getKey(ModItems.TIME_STONE.get()))),
                "the Time Stone is free for anyone at first");
        EternityRealm.wish(first, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(ModItems.TIME_STONE.get()));
        check(count(first, ModItems.TIME_STONE.get()) == 1, "he holds the Time Stone (one, not a stack)");
        check(first.getUUID().equals(StoneLedger.holder(server, com.kolodkingal.enhanced.thanos.StoneKind.TIME)),
                "the ledger names him");
        check("message.enhanced.stones.one_per_world".equals(Wishes.refusal(second,
                ModItems.TIME_STONE.get())), "someone else holds it now");
        check(Wishes.locks(second).stream().anyMatch(l -> l.item().equals(BuiltInRegistries.ITEM.getKey(
                ModItems.TIME_STONE.get())) && l.reasonKey().equals("message.enhanced.stones.one_per_world")),
                "his picker shows it locked with the ledger's line");
        EternityRealm.wish(second, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(ModItems.TIME_STONE.get()));
        check(count(second, ModItems.TIME_STONE.get()) == 0 && Wishes.mayWish(second), "refused, the wish kept");
        // The Mind Stone is not counted; a stone that would burn its holder is no wish.
        ServerPlayer plain = traveller(f, 8.5, 4.5, Power.NONE);
        check(Wishes.refusal(second, ModItems.MIND_STONE.get()) == null, "the Mind Stone is free");
        check(Wishes.STONE_HOLD.equals(Wishes.refusal(plain, ModItems.POWER_STONE.get())),
                "a bare stone is refused to one who cannot hold it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WISH)
    public static void gungnirHasOneBearer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        AsgardTestSupport.ensureAsgard(server); // (the bearer is recorded in Asgard's state)
        AsgardState state = AsgardState.get(server);
        UUID was = state.gungnirOwner();
        f.onClose(() -> state.setGungnirOwner(was));
        state.setGungnirOwner(UUID.randomUUID());
        ServerPlayer player = traveller(f, 4.5, 4.5, Power.NONE);
        check(Wishes.GUNGNIR.equals(Wishes.refusal(player, ModItems.GUNGNIR.get())), "it has its one bearer");
        EternityRealm.wish(player, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(ModItems.GUNGNIR.get()));
        check(count(player, ModItems.GUNGNIR.get()) == 0 && Wishes.mayWish(player), "refused");
        state.setGungnirOwner(null);
        EternityRealm.wish(player, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(ModItems.GUNGNIR.get()));
        check(count(player, ModItems.GUNGNIR.get()) == 1, "his now");
        ItemStack spear = player.getInventory().items.stream().filter(s -> s.is(ModItems.GUNGNIR.get())).findFirst()
                .orElseThrow();
        check(GungnirItem.isOwnGungnir(player, spear), "bound to him");
        check(!GungnirStand.onePerWorld() || player.getUUID().equals(GungnirStand.owner(server)),
                "and recorded as the world's bearer");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WISH)
    public static void mjolnirOnlyForTheWorthy(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer plain = traveller(f, 4.5, 4.5, Power.NONE);
        check(!Worthiness.isWorthy(plain), "an ordinary player is not worthy");
        EternityRealm.wish(plain, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(ModItems.MJOLNIR.get()));
        check(count(plain, ModItems.MJOLNIR.get()) == 0 && Wishes.mayWish(plain), "refused to the unworthy");
        check(Wishes.locks(plain).stream().anyMatch(l -> l.reasonKey().equals(Wishes.MJOLNIR)), "shown locked");
        ServerPlayer love = traveller(f, 6.5, 4.5, Power.LOVE);
        EternityRealm.wish(love, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(ModItems.MJOLNIR.get()));
        ItemStack hammer = love.getInventory().items.stream().filter(s -> s.is(ModItems.MJOLNIR.get())).findFirst()
                .orElse(ItemStack.EMPTY);
        check(!hammer.isEmpty() && Worthiness.ownerOf(hammer) != null && Worthiness.ownerOf(hammer).is(love),
                "claimed by the worthy");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WISH)
    public static void oneWishThenResetGivesItBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = traveller(f, 4.5, 4.5, Power.NONE);
        EternityRealm.wish(player, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(Items.DIAMOND));
        EternityRealm.returnHome(player);
        check(EternityRealm.enter(player, player.blockPosition()), "a second visit");
        EternityRealm.wish(player, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(Items.EMERALD));
        check(count(player, Items.EMERALD) == 0, "a second wish refused");
        EternityCommand.reset(player.createCommandSourceStack(), player);
        check(Wishes.mayWish(player) && ModAttachments.ETERNITY.get(player).visits() == 2,
                "reset gives it back (the visits kept)");
        EternityRealm.wish(player, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(Items.EMERALD));
        check(count(player, Items.EMERALD) == 64, "and he wishes again");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WISH)
    public static void homeToTheExactSpotAndFacing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        EternityTestSupport.ensureRealm(helper.getLevel().getServer());
        ServerPlayer player = f.player(7.25, FLOOR, 9.75);
        player.setYRot(37.0F);
        player.setXRot(12.0F);
        Vec3 from = player.position();
        check(EternityRealm.enter(player, player.blockPosition()) && EternityRealm.in(player), "there");
        EternityRealm.wish(player, EternityWishPayload.LEAVE, BuiltInRegistries.ITEM.getKey(Items.AIR));
        check(player.level() == helper.getLevel(), "home again");
        assertNear(player.position().distanceTo(from), 0.0, 1.0E-6, "on the very spot");
        assertNear(player.getYRot(), 37.0, 1.0E-3, "facing the same way");
        assertNear(player.getXRot(), 12.0, 1.0E-3, "looking at the same height");
        check(ModAttachments.ETERNITY.get(player).returnTo().isEmpty() && Wishes.mayWish(player),
                "the way home forgotten; leaving costs no wish");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WISH)
    public static void aPlayerWhoLogsInThereIsSentHome(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = traveller(f, 5.5, 5.5, Power.NONE);
        EternityEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        EternityEvents.onPlayerTick(new PlayerTickEvent.Post(player));
        check(!EternityRealm.in(player) && player.level() == helper.getLevel(), "sent home at his first tick");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = WISH)
    public static void loveIsAWish(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = traveller(f, 4.5, 4.5, Power.WOLVERINE);
        answer(new EternityWishPayload(EternityWishPayload.LOVE, BuiltInRegistries.ITEM.getKey(Items.AIR)), player);
        check(Powers.power(player) == Power.LOVE, "she is Love");
        assertNear(player.getMaxHealth(), 32.0, 1.0E-4, "32 health (16 hearts)");
        check(count(player, ModItems.STORMBREAKER.get()) == 1, "with a Stormbreaker");
        check(ModAttachments.ETERNITY.get(player).lastWish() == EternityProgress.LOVE, "her wish was Love");
        // One who already carries the axe gets no second.
        ServerPlayer thor = traveller(f, 6.5, 4.5, Power.THOR);
        thor.getInventory().add(new ItemStack(ModItems.STORMBREAKER.get()));
        EternityRealm.wish(thor, EternityWishPayload.LOVE, BuiltInRegistries.ITEM.getKey(Items.AIR));
        check(Powers.power(thor) == Power.LOVE && count(thor, ModItems.STORMBREAKER.get()) == 1, "no second axe");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = CFG)
    public static void theWishesFollowTheConfig(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EternityConfig.WISH_FULL_STACK, false);
        f.config(EternityConfig.LOVE_WISH, false);
        ServerPlayer player = traveller(f, 4.5, 4.5, Power.NONE);
        EternityRealm.wish(player, EternityWishPayload.LOVE, BuiltInRegistries.ITEM.getKey(Items.AIR));
        check(Powers.power(player) == Power.NONE && Wishes.mayWish(player), "no Love when the config says so");
        EternityRealm.wish(player, EternityWishPayload.ITEM, BuiltInRegistries.ITEM.getKey(Items.DIAMOND));
        check(count(player, Items.DIAMOND) == 1, "one item, not a stack");
        helper.succeed();
    }

    /** What the server does with a message the client sends (the payload's own handler). */
    private static void answer(EternityWishPayload payload, ServerPlayer player) {
        try {
            Method handle = EternityWishPayload.class.getDeclaredMethod("handle", EternityWishPayload.class,
                    PacketContext.class);
            handle.setAccessible(true);
            PacketContext context = (PacketContext) Proxy.newProxyInstance(PacketContext.class.getClassLoader(),
                    new Class[]{PacketContext.class}, (proxy, method, args) -> method.getName().equals("player")
                            ? player : null);
            handle.invoke(null, payload, context);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private EternityTests() {
    }
}
