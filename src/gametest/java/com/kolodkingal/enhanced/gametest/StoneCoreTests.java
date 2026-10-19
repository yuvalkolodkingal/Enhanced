package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.StonePedestalBlock;
import com.kolodkingal.enhanced.block.StonePedestalBlockEntity;
import com.kolodkingal.enhanced.boss.BossArena.Containment;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.config.StonesConfig.Uniqueness;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.item.TheOrbItem;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.stones.GauntletRecipe;
import com.kolodkingal.enhanced.stones.SiteStoneLootFunction;
import com.kolodkingal.enhanced.stones.Snap;
import com.kolodkingal.enhanced.stones.StoneLedger;
import com.kolodkingal.enhanced.stones.StoneProgress;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.stones.StoneTouch;
import com.kolodkingal.enhanced.stones.StoneWheel;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Infinity Stones' core (3.6, lane L1): the holding rule's events (the slip, the grace, the rejection, the safe
 * containers), the recipes, the Orb, the stone pedestal and the ledger in its three modes, lost stones, the site-stone
 * loot function, the Infinity Gauntlet (stones in and out, Thanos's Gauntlet unchanged, who may wield it, the Stone
 * Wheel) and the Snap. Batches {@code stones_core}, {@code stones_ledger}, {@code stones_gauntlet}; the tests that
 * change the config or reach far (the world and unlimited modes, the Snap, the arena) have batches of their own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class StoneCoreTests {
    static final String CORE = "stones_core";
    static final String CORE_CFG = "stones_core_cfg";
    static final String LEDGER = "stones_ledger";
    static final String LEDGER_WORLD = "stones_ledger_world";
    static final String LEDGER_UNLIMITED = "stones_ledger_unlimited";
    static final String GAUNTLET = "stones_gauntlet";
    static final String SNAP = "stones_snap";
    static final String SNAP_ARENA = "stones_snap_arena";
    static final String ASGARD = "stones_asgard";

    // ---- helpers ----

    static ItemStack stone(StoneKind kind) {
        return new ItemStack(ModItems.stone(kind));
    }

    /** A fixture that also forgets every player's grace and removes the stones left on the floor when the test ends. */
    static TestKit.Fixture fixture(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.onClose(() -> {
            for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, arenaBox(helper))) {
                item.discard();
            }
        });
        return f;
    }

    static AABB arenaBox(GameTestHelper helper) {
        Vec3 a = helper.absoluteVec(new Vec3(0, 0, 0));
        return new AABB(a.x - 2, a.y - 2, a.z - 2, a.x + 24, a.y + 14, a.z + 24);
    }

    static List<ItemEntity> stonesOnTheFloor(GameTestHelper helper, StoneKind kind) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, arenaBox(helper),
                item -> item.getItem().is(ModItems.stone(kind)));
    }

    static boolean carries(ServerPlayer player, StoneKind kind) {
        return !find(player, kind).isEmpty();
    }

    /** The stone of this kind in the player's inventory (whatever is written on it), or empty. */
    static ItemStack find(ServerPlayer player, StoneKind kind) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.stone(kind))) {
                return player.getInventory().getItem(i);
            }
        }
        return ItemStack.EMPTY;
    }

    static void tick(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            StoneTouch.tick(player);
        }
    }

    static ItemStack gauntletWith(StoneKind... kinds) {
        ItemStack gauntlet = InfinityGauntletItem.empty();
        GauntletContents contents = GauntletContents.EMPTY;
        for (StoneKind kind : kinds) {
            contents = contents.with(kind);
        }
        if (kinds.length > 0) {
            contents = contents.withSelected(kinds[0]);
        }
        gauntlet.set(ModDataComponents.GAUNTLET.get(), contents);
        return gauntlet;
    }

    static ItemStack fullGauntlet() {
        return gauntletWith(StoneKind.values());
    }

    // ---- the slip, the grace, the rejection ----

    @GameTest(template = ARENA, batch = CORE)
    public static void aStoneSlipsFromAPowerlessHandButNotFromThors(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer nobody = f.player(4.5, FLOOR, 4.5);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, FLOOR, 4.5));
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stone(StoneKind.POWER));
        item.setNoPickUpDelay();
        level.addFreshEntity(item);
        item.playerTouch(nobody);
        check(item.isAlive() && !carries(nobody, StoneKind.POWER), "the Power Stone slips from a powerless hand");
        check("message.enhanced.stones.slips".equals(lastActionBarKey(nobody)), "and the action bar says so: "
                + lastActionBarKey(nobody));
        check(item.getDeltaMovement().y > 0.0, "it hops away");
        ServerPlayer thor = f.player(14.5, FLOOR, 14.5, Power.THOR);
        Vec3 there = helper.absoluteVec(new Vec3(14.5, FLOOR, 14.5));
        ItemEntity other = new ItemEntity(level, there.x, there.y, there.z, stone(StoneKind.POWER));
        other.setNoPickUpDelay();
        level.addFreshEntity(other);
        other.playerTouch(thor);
        check(!other.isAlive() && carries(thor, StoneKind.POWER), "Thor picks it up");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CORE)
    public static void aPowerlessPlayerIsZappedAfterTenSecondsAndTheStoneThrownOut(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer nobody = f.player(10.5, FLOOR, 10.5);
        nobody.getInventory().setItem(9, stone(StoneKind.POWER));
        int limit = StoneTouch.graceUnits();
        check(limit == 200, "ten seconds: " + limit);
        tick(nobody, limit - 1);
        check(carries(nobody, StoneKind.POWER) && nobody.getHealth() == nobody.getMaxHealth(),
                "nothing before the 200th tick");
        check(StoneTouch.grace(nobody, StoneKind.POWER) == limit - 1, "the grace counts every tick");
        tick(nobody, 1);
        check(!carries(nobody, StoneKind.POWER), "at the 200th tick the stone jumps out");
        TestKit.assertNear(nobody.getHealth(), nobody.getMaxHealth() - 2.0F, 0.01, "a zap of 2");
        check(stonesOnTheFloor(helper, StoneKind.POWER).size() == 1, "the stone is on the floor in front of him");
        ItemEntity thrown = stonesOnTheFloor(helper, StoneKind.POWER).get(0);
        check(thrown.distanceTo(nobody) < 2.5, "about 1.5 blocks away: " + thrown.distanceTo(nobody));
        check("message.enhanced.stones.too_strong".equals(lastActionBarKey(nobody)), "too strong for you");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CORE)
    public static void inAHandTheGraceRunsThreeTimesFasterAndNeverKills(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer nobody = f.player(10.5, FLOOR, 10.5);
        nobody.setItemInHand(InteractionHand.MAIN_HAND, stone(StoneKind.SOUL));
        int ticks = (StoneTouch.graceUnits() + 2) / 3;
        check(ticks == 67, "200 units at 3 a tick: 67 ticks");
        tick(nobody, ticks - 1);
        check(carries(nobody, StoneKind.SOUL), "still in his hand at tick 66");
        tick(nobody, 1);
        check(!carries(nobody, StoneKind.SOUL), "thrown out at tick 67");
        // Low on health: never below one heart... of health (1.0).
        ServerPlayer weak = f.player(4.5, FLOOR, 4.5);
        weak.setHealth(1.5F);
        weak.setItemInHand(InteractionHand.OFF_HAND, stone(StoneKind.REALITY));
        tick(weak, ticks);
        check(!carries(weak, StoneKind.REALITY) && weak.isAlive() && weak.getHealth() >= 1.0F,
                "the zap leaves him at 1 health at least: " + weak.getHealth());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CORE)
    public static void aGauntletAShulkerBoxAndTheOrbAreSafeABundleIsNot(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer nobody = f.player(10.5, FLOOR, 10.5);
        nobody.getInventory().setItem(10, gauntletWith(StoneKind.TIME, StoneKind.POWER));
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stone(StoneKind.SOUL))));
        nobody.getInventory().setItem(11, shulker);
        nobody.getInventory().setItem(12, new ItemStack(ModItems.THE_ORB.get()));
        tick(nobody, 300);
        check(StoneTouch.unsafe(nobody).isEmpty() && nobody.getHealth() == nobody.getMaxHealth(),
                "the Gauntlet, the shulker box and the Orb keep their stones safe");
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(stone(StoneKind.MIND))));
        nobody.getInventory().setItem(13, bundle);
        check(StoneTouch.unsafe(nobody).size() == 1, "a bundle in the inventory is not safe");
        tick(nobody, StoneTouch.graceUnits());
        BundleContents left = nobody.getInventory().getItem(13).get(DataComponents.BUNDLE_CONTENTS);
        check(left != null && left.isEmpty(), "the stone jumped out of the bundle");
        check(stonesOnTheFloor(helper, StoneKind.MIND).size() == 1, "onto the floor");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CORE)
    public static void aBearerWhoLosesHisPowerGetsTheGrace(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer thor = f.player(10.5, FLOOR, 10.5, Power.THOR);
        thor.getInventory().setItem(9, stone(StoneKind.SPACE));
        tick(thor, 250);
        check(carries(thor, StoneKind.SPACE) && StoneTouch.grace(thor, StoneKind.SPACE) == 0, "Thor holds it at ease");
        Powers.setPower(thor, Power.NONE);
        tick(thor, 20);
        check(StoneTouch.grace(thor, StoneKind.SPACE) == 20, "without his power the grace begins");
        ServerPlayer billy = f.player(4.5, FLOOR, 4.5, Power.BILLY);
        billy.getInventory().setItem(9, stone(StoneKind.TIME));
        billy.getInventory().setItem(10, stone(StoneKind.POWER));
        tick(billy, 10);
        check(StoneTouch.grace(billy, StoneKind.TIME) == 0 && StoneTouch.grace(billy, StoneKind.POWER) == 10,
                "a sorcerer holds the Time Stone, not the Power Stone");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CORE)
    public static void thorHoldsTheTimeStoneButItStaysQuiet(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer thor = f.player(10.5, FLOOR, 10.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, stone(StoneKind.TIME));
        InteractionResult result = thor.getMainHandItem().use(helper.getLevel(), thor, InteractionHand.MAIN_HAND)
                .getResult();
        check(result == InteractionResult.CONSUME && !thor.isUsingItem(), "no power answers: " + result);
        check("message.enhanced.stones.quiet".equals(lastActionBarKey(thor)), "he is told he is no sorcerer");
        check(StoneRules.mayHold(thor, StoneKind.TIME) && !StoneRules.mayUse(thor, StoneKind.TIME), "the rule");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CORE)
    public static void aStoneOnTheGroundIsProofAndNeverDespawns(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        Vec3 at = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stone(StoneKind.REALITY));
        level.addFreshEntity(item);
        item.hurt(level.damageSources().explosion(null, null), 100.0F);
        item.hurt(level.damageSources().cactus(), 100.0F);
        item.hurt(level.damageSources().lava(), 100.0F);
        check(item.isAlive(), "explosions, cactus and lava leave it");
        item.tick();
        check(item.getAge() == -32768, "it never despawns");
        helper.succeed();
    }

    // ---- the recipes and the Orb ----

    static CraftingRecipe recipe(MinecraftServer server, String id) {
        Optional<RecipeHolder<?>> holder = server.getRecipeManager().byKey(Enhanced.id(id));
        check(holder.isPresent() && holder.get().value() instanceof CraftingRecipe, "the recipe " + id);
        return (CraftingRecipe) holder.get().value();
    }

    static CraftingInput grid(ItemStack... nine) {
        return CraftingInput.of(3, 3, List.of(nine));
    }

    @GameTest(template = ARENA, batch = CORE)
    public static void theStonesComeOutOfTheirVesselsAndGoBack(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ItemStack e = ItemStack.EMPTY;
        CraftingRecipe mind = recipe(server, "mind_stone");
        CraftingInput scepter = grid(e, e, e, e, new ItemStack(ModItems.LOKIS_SCEPTER.get()), e, e, e, e);
        check(mind.matches(scepter, level), "Loki's Scepter alone");
        check(mind.assemble(scepter, level.registryAccess()).is(ModItems.MIND_STONE.get()), "gives the Mind Stone");
        NonNullList<ItemStack> left = mind.getRemainingItems(scepter);
        check(left.stream().filter(stack -> !stack.isEmpty()).count() == 1 && left.get(scepter.size() == 1 ? 0 : 4)
                .is(ModItems.EMPTY_SCEPTER.get()), "and the empty Scepter stays in its slot: " + left);
        check(!mind.matches(grid(e, new ItemStack(ModItems.LOKIS_SCEPTER.get()), e, e, new ItemStack(Items.STICK), e,
                e, e, e), level), "not with anything else");
        CraftingRecipe space = recipe(server, "space_stone");
        CraftingInput tesseract = grid(new ItemStack(ModItems.TESSERACT.get()), e, e, e, e, e, e, e, e);
        check(space.matches(tesseract, level) && space.assemble(tesseract, level.registryAccess())
                .is(ModItems.SPACE_STONE.get()) && space.getRemainingItems(tesseract).get(0)
                .is(ModItems.EMPTY_TESSERACT.get()), "the Tesseract: the Space Stone, the empty Tesseract left");
        CraftingRecipe back = recipe(server, "lokis_scepter_from_stone");
        CraftingInput both = grid(new ItemStack(ModItems.EMPTY_SCEPTER.get()), stone(StoneKind.MIND), e, e, e, e, e,
                e, e);
        check(back.matches(both, level) && back.assemble(both, level.registryAccess())
                .is(ModItems.LOKIS_SCEPTER.get()), "the way back: Loki's Scepter");
        CraftingRecipe cube = recipe(server, "tesseract_from_stone");
        CraftingInput cubeBoth = grid(stone(StoneKind.SPACE), new ItemStack(ModItems.EMPTY_TESSERACT.get()), e, e, e,
                e, e, e, e);
        check(cube.matches(cubeBoth, level) && cube.assemble(cubeBoth, level.registryAccess())
                .is(ModItems.TESSERACT.get()), "and the Tesseract");
        ItemStack g = new ItemStack(Items.GOLD_BLOCK);
        CraftingRecipe eye = recipe(server, "eye_of_agamotto");
        CraftingInput eyeGrid = grid(g, g, g, g, stone(StoneKind.TIME), g, g, g, g);
        check(eye.matches(eyeGrid, level) && eye.assemble(eyeGrid, level.registryAccess())
                .is(ModItems.EYE_OF_AGAMOTTO.get()), "eight gold blocks round the Time Stone: the Eye");
        CraftingRecipe out = recipe(server, "time_stone_from_eye");
        CraftingInput eyeAlone = grid(new ItemStack(ModItems.EYE_OF_AGAMOTTO.get()), e, e, e, e, e, e, e, e);
        check(out.matches(eyeAlone, level) && out.assemble(eyeAlone, level.registryAccess())
                .is(ModItems.TIME_STONE.get()), "the Eye alone: the Time Stone");
        helper.succeed();
    }

    static CraftingInput gauntletGrid() {
        ItemStack g = new ItemStack(Items.GOLD_BLOCK);
        return grid(g, new ItemStack(Items.NETHERITE_INGOT), g, g, new ItemStack(Items.NETHER_STAR), g, g, g, g);
    }

    @GameTest(template = ARENA, batch = CORE)
    public static void theEmptyGauntletIsCrafted(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CraftingRecipe recipe = recipe(level.getServer(), "infinity_gauntlet");
        check(recipe instanceof GauntletRecipe, "the Gauntlet's own recipe type");
        check(recipe.matches(gauntletGrid(), level), "gold, a netherite ingot and a nether star");
        ItemStack made = recipe.assemble(gauntletGrid(), level.registryAccess());
        check(made.is(ModItems.INFINITY_GAUNTLET.get()) && GauntletContents.EMPTY.equals(
                made.get(ModDataComponents.GAUNTLET.get())), "an empty Gauntlet, not Thanos's: "
                + made.get(ModDataComponents.GAUNTLET.get()));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CORE_CFG)
    public static void theGauntletCanBeMadeUncraftable(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(StonesConfig.GAUNTLET_CRAFTABLE, false);
        ServerLevel level = helper.getLevel();
        check(!recipe(level.getServer(), "infinity_gauntlet").matches(gauntletGrid(), level),
                "[gauntlet] craftable false: no Gauntlet");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CORE)
    public static void theOrbOpensForLokiNotForAPowerlessPlayer(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer nobody = f.player(4.5, FLOOR, 4.5);
        nobody.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.THE_ORB.get()));
        nobody.getMainHandItem().use(helper.getLevel(), nobody, InteractionHand.MAIN_HAND);
        check(nobody.getMainHandItem().is(ModItems.THE_ORB.get()), "the Orb stays shut for him");
        check("message.enhanced.stones.orb_closed".equals(lastActionBarKey(nobody)), "and says so");
        tick(nobody, 300);
        check(nobody.getHealth() == nobody.getMaxHealth(), "the Orb never burns him");
        ServerPlayer loki = f.player(14.5, FLOOR, 14.5, Power.LOKI);
        ItemStack orb = new ItemStack(ModItems.THE_ORB.get());
        StoneLedger.stamp(orb, nobody.getUUID());
        loki.setItemInHand(InteractionHand.MAIN_HAND, orb);
        var held = loki.getMainHandItem().use(helper.getLevel(), loki, InteractionHand.MAIN_HAND);
        loki.setItemInHand(InteractionHand.MAIN_HAND, held.getObject());
        check(loki.getMainHandItem().is(ModItems.POWER_STONE.get()), "Loki opens it: the Power Stone");
        check(nobody.getUUID().equals(StoneLedger.taker(loki.getMainHandItem())), "with the Orb's taker on it");
        check(TheOrbItem.open(loki, new ItemStack(ModItems.THE_ORB.get())).is(ModItems.POWER_STONE.get()), "always");
        helper.succeed();
    }

    // ---- the pedestal and the ledger ----

    static BlockPos pedestal(GameTestHelper helper, int x, int z, StoneKind kind) {
        BlockPos rel = new BlockPos(x, 2, z);
        helper.setBlock(rel, ModBlocks.STONE_PEDESTAL.get().defaultBlockState()
                .setValue(StonePedestalBlock.STONE, StonePedestalBlock.Stone.of(kind)));
        return helper.absolutePos(rel);
    }

    static boolean take(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockState state = helper.getLevel().getBlockState(pos);
        return StonePedestalBlock.take(player, helper.getLevel(), pos, state, StonePedestalBlock.kind(state));
    }

    @GameTest(template = ARENA, batch = LEDGER)
    public static void eachPlayerTakesEachSiteStoneOnce(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        BlockPos pos = pedestal(helper, 10, 10, StoneKind.TIME);
        ServerPlayer thor = f.player(9.5, FLOOR, 9.5, Power.THOR);
        ServerPlayer loki = f.player(11.5, FLOOR, 11.5, Power.LOKI);
        check(take(helper, thor, pos) && carries(thor, StoneKind.TIME), "Thor takes the Time Stone");
        check(thor.getData(ModAttachments.STONES).hasTaken(StoneKind.TIME), "the ledger marks him");
        check(thor.getUUID().equals(StoneLedger.taker(thor.getMainHandItem())), "the stone carries his name");
        StonePedestalBlockEntity be = (StonePedestalBlockEntity) helper.getLevel().getBlockEntity(pos);
        check(be != null && !be.shows(thor.getUUID(), StoneKind.TIME) && be.shows(loki.getUUID(), StoneKind.TIME),
                "the pedestal is empty for Thor, full for Loki");
        check(take(helper, loki, pos) && carries(loki, StoneKind.TIME), "Loki takes his too");
        thor.getInventory().clearContent();
        check(!take(helper, thor, pos) && !carries(thor, StoneKind.TIME), "Thor may not take it twice");
        check("message.enhanced.stones.already_taken".equals(lastActionBarKey(thor)), "he is told: "
                + lastActionBarKey(thor));
        ServerPlayer nobody = f.player(5.5, FLOOR, 5.5);
        check(!take(helper, nobody, pos), "a powerless player may not take it");
        check("message.enhanced.stones.pedestal_refuse_time".equals(lastActionBarKey(nobody)), "only a god... or a "
                + "sorcerer");
        BlockPos reality = pedestal(helper, 4, 14, StoneKind.REALITY);
        ServerPlayer billy = f.player(4.5, FLOOR, 13.5, Power.BILLY);
        check(!take(helper, billy, reality)
                && "message.enhanced.stones.pedestal_refuse".equals(lastActionBarKey(billy)),
                "a sorcerer may not take the Reality Stone");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = LEDGER)
    public static void aStoneLostInTheVoidOrKilledFreesItsKind(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer thor = f.player(10.5, FLOOR, 10.5, Power.THOR);
        BlockPos time = pedestal(helper, 8, 8, StoneKind.TIME);
        BlockPos soul = pedestal(helper, 12, 12, StoneKind.SOUL);
        check(take(helper, thor, time) && take(helper, thor, soul), "Thor takes the Time and Soul Stones");
        ItemStack lost = find(thor, StoneKind.TIME).copy();
        thor.getInventory().clearContent();
        Vec3 at = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        ItemEntity falling = new ItemEntity(level, at.x, level.getMinBuildHeight() - 70, at.z, lost);
        level.addFreshEntity(falling);
        falling.tick();
        check(falling.isRemoved(), "the stone fell out of the world");
        check(!thor.getData(ModAttachments.STONES).hasTaken(StoneKind.TIME), "Thor may take the Time Stone again");
        ItemStack soulStone = stone(StoneKind.SOUL);
        StoneLedger.stamp(soulStone, thor.getUUID());
        ItemEntity killed = new ItemEntity(level, at.x, at.y, at.z, soulStone);
        level.addFreshEntity(killed);
        check(thor.getData(ModAttachments.STONES).hasTaken(StoneKind.SOUL), "the Soul Stone still counts");
        killed.kill();
        check(!thor.getData(ModAttachments.STONES).hasTaken(StoneKind.SOUL), "killed: it is free again");
        check(take(helper, thor, time), "and he takes the Time Stone once more");
        ItemStack gauntlet = gauntletWith(StoneKind.TIME);
        StoneLedger.putInGauntletOf(gauntlet, StoneKind.TIME, thor.getUUID());
        StoneLedger.destroyed(gauntlet);
        check(!thor.getData(ModAttachments.STONES).hasTaken(StoneKind.TIME), "a destroyed Gauntlet frees its stones");
        helper.succeed();
    }

    /**
     * 3.6 integration (L1 x L3 x L5): an Eye of Agamotto crafted round a site's Time Stone carries its taker (and gives
     * it back to the stone taken out of it); a destroyed Eye (a sacrificed Doctor Strange's) frees the Time Stone, an
     * Eye that never held a site's stone frees nothing.
     */
    @GameTest(template = ARENA, batch = LEDGER)
    public static void theEyeOfAgamottoCarriesItsTimeStonesTaker(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        BlockPos pos = pedestal(helper, 10, 10, StoneKind.TIME);
        ServerPlayer loki = f.player(9.5, FLOOR, 9.5, Power.LOKI);
        check(take(helper, loki, pos) && carries(loki, StoneKind.TIME), "Loki takes the Time Stone");
        ItemStack stone = find(loki, StoneKind.TIME).copy();
        loki.getInventory().clearContent();
        loki.inventoryMenu.getCraftSlots().setItem(0, stone);
        loki.containerMenu = loki.inventoryMenu;
        ItemStack eye = new ItemStack(ModItems.EYE_OF_AGAMOTTO.get());
        eye.getItem().onCraftedBy(eye, level, loki);
        check(loki.getUUID().equals(StoneLedger.taker(eye)), "the crafted Eye carries Loki's name");
        loki.inventoryMenu.getCraftSlots().setItem(0, eye.copy());
        ItemStack back = stone(StoneKind.TIME);
        back.getItem().onCraftedBy(back, level, loki);
        check(loki.getUUID().equals(StoneLedger.taker(back)), "so does the stone taken back out of it");
        loki.inventoryMenu.getCraftSlots().clearContent();
        StoneLedger.destroyed(new ItemStack(ModItems.EYE_OF_AGAMOTTO.get()));
        check(loki.getData(ModAttachments.STONES).hasTaken(StoneKind.TIME), "an Eye that held no site stone: nothing");
        StoneLedger.destroyed(eye);
        check(!loki.getData(ModAttachments.STONES).hasTaken(StoneKind.TIME), "the destroyed Eye frees the Time Stone");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = LEDGER)
    public static void theVaultChestGivesTheOrbOncePerPlayer(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer thor = f.player(4.5, FLOOR, 4.5, Power.THOR);
        ServerPlayer nobody = f.player(14.5, FLOOR, 14.5);
        SiteStoneLootFunction function = new SiteStoneLootFunction(List.of(), StoneKind.POWER);
        check(function.apply(new ItemStack(ModItems.THE_ORB.get()), opener(helper, thor)).is(ModItems.THE_ORB.get()),
                "the first chest gives Thor the Orb");
        check(thor.getData(ModAttachments.STONES).hasTaken(StoneKind.POWER), "the ledger counts it");
        ItemStack second = function.apply(new ItemStack(ModItems.THE_ORB.get()), opener(helper, thor));
        check(second.is(Items.PAPER), "a second chest gives him a note");
        check(second.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals("item.enhanced.orb_gone"), "\"The Orb is gone.\"");
        ItemStack theirs = function.apply(new ItemStack(ModItems.THE_ORB.get()), opener(helper, nobody));
        check(theirs.is(ModItems.THE_ORB.get()) && nobody.getUUID().equals(StoneLedger.taker(theirs)),
                "another player gets his own (anyone may carry the Orb)");
        LootParams hopper = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,
                helper.absoluteVec(new Vec3(10.5, 2, 10.5))).create(LootContextParamSets.CHEST);
        check(function.apply(new ItemStack(ModItems.THE_ORB.get()), new LootContext.Builder(hopper)
                .create(Optional.empty())).is(Items.PAPER), "a chest nobody opened (a hopper): no Orb");
        helper.succeed();
    }

    static LootContext opener(GameTestHelper helper, ServerPlayer player) {
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(10.5, 2, 10.5)))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);
        return new LootContext.Builder(params).create(Optional.empty());
    }

    @GameTest(template = ARENA, batch = LEDGER_WORLD)
    public static void inWorldModeOneOfEachAndALostStoneRefills(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(StonesConfig.UNIQUENESS, Uniqueness.WORLD);
        MinecraftServer server = helper.getLevel().getServer();
        StoneLedger.reset(server);
        f.onClose(() -> StoneLedger.reset(server));
        BlockPos a = pedestal(helper, 6, 6, StoneKind.REALITY);
        BlockPos b = pedestal(helper, 14, 14, StoneKind.REALITY);
        ServerPlayer thor = f.player(6.5, FLOOR, 7.5, Power.THOR);
        ServerPlayer loki = f.player(14.5, FLOOR, 13.5, Power.LOKI);
        check(take(helper, thor, a), "Thor takes the Reality Stone");
        check(thor.getUUID().equals(StoneLedger.holder(server, StoneKind.REALITY)), "the world's ledger names him");
        check(!take(helper, loki, b) && !take(helper, loki, a), "every pedestal is empty for Loki");
        check("message.enhanced.stones.one_per_world".equals(lastActionBarKey(loki)), "someone has it");
        StoneLedger.destroyed(find(thor, StoneKind.REALITY));
        check(StoneLedger.holder(server, StoneKind.REALITY) == null, "lost: back at its sites");
        StonePedestalBlockEntity.serverTick(helper.getLevel(), a, helper.getLevel().getBlockState(a),
                (StonePedestalBlockEntity) helper.getLevel().getBlockEntity(a));
        check(take(helper, loki, b), "Loki takes it now");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = LEDGER_UNLIMITED)
    public static void inUnlimitedModeEverySiteGivesEveryTime(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        f.config(StonesConfig.UNIQUENESS, Uniqueness.UNLIMITED);
        BlockPos pos = pedestal(helper, 10, 10, StoneKind.POWER);
        ServerPlayer thor = f.player(9.5, FLOOR, 9.5, Power.THOR);
        check(take(helper, thor, pos) && take(helper, thor, pos), "twice from the same pedestal");
        check(thor.getInventory().countItem(ModItems.POWER_STONE.get()) == 2, "two Power Stones");
        helper.succeed();
    }

    // ---- the Gauntlet ----

    @GameTest(template = ARENA, batch = GAUNTLET)
    public static void stonesGoInForAnyoneAndOutOnlyForAHolder(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer nobody = f.player(4.5, FLOOR, 4.5);
        ItemStack gauntlet = InfinityGauntletItem.empty();
        ItemStack power = stone(StoneKind.POWER);
        StoneLedger.stamp(power, nobody.getUUID());
        // Through the inventory screen's click: the stone on the cursor, the Gauntlet in a slot.
        nobody.getInventory().setItem(0, gauntlet);
        net.minecraft.world.inventory.Slot slot = new net.minecraft.world.inventory.Slot(nobody.getInventory(), 0, 0,
                0);
        net.minecraft.world.SimpleContainer cursor = new net.minecraft.world.SimpleContainer(power);
        check(gauntlet.overrideOtherStackedOnMe(power, slot, net.minecraft.world.inventory.ClickAction.SECONDARY,
                nobody, net.minecraft.world.entity.SlotAccess.forContainer(cursor, 0)), "the click is the Gauntlet's");
        check(cursor.getItem(0).isEmpty() && InfinityGauntletItem.contents(gauntlet).has(StoneKind.POWER),
                "a powerless player sets the Power Stone in");
        check(InfinityGauntletItem.contents(gauntlet).selectedStone() == StoneKind.POWER, "and it is selected");
        check(nobody.getUUID().equals(StoneLedger.gauntletTakerOf(gauntlet, StoneKind.POWER)),
                "the Gauntlet keeps its taker's name");
        check(InfinityGauntletItem.insert(gauntlet, stone(StoneKind.SOUL), nobody) != null
                && InfinityGauntletItem.contents(gauntlet).count() == 2, "and the Soul Stone");
        check(InfinityGauntletItem.insert(gauntlet, stone(StoneKind.SOUL), nobody) == null,
                "a full socket takes no second stone");
        check(InfinityGauntletItem.takeOut(gauntlet, StoneKind.POWER, nobody).isEmpty()
                && "message.enhanced.gauntlet.stays".equals(lastActionBarKey(nobody)),
                "the stones will not leave it for him");
        ServerPlayer thor = f.player(14.5, FLOOR, 14.5, Power.THOR);
        ItemStack out = InfinityGauntletItem.takeOut(gauntlet, StoneKind.POWER, thor);
        check(out.is(ModItems.POWER_STONE.get()) && nobody.getUUID().equals(StoneLedger.taker(out)),
                "Thor takes the Power Stone out, its taker's name with it");
        check(!InfinityGauntletItem.contents(gauntlet).has(StoneKind.POWER)
                && InfinityGauntletItem.contents(gauntlet).selectedStone() == StoneKind.SOUL,
                "the Soul Stone is selected now");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = GAUNTLET)
    public static void thanossGauntletTakesNoStoneAndStillAwakensThanos(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer nobody = f.player(10.5, FLOOR, 10.5);
        ItemStack titan = new ItemStack(ModItems.INFINITY_GAUNTLET.get());
        check(InfinityGauntletItem.insert(titan, stone(StoneKind.MIND), nobody) == null
                && GauntletContents.TITAN.equals(InfinityGauntletItem.contents(titan)),
                "the Titan's own stones: no Mind Stone goes in");
        check("message.enhanced.gauntlet.titans_own".equals(lastActionBarKey(nobody)), "he is told");
        check(InfinityGauntletItem.takeOut(titan, StoneKind.POWER, f.player(5.5, FLOOR, 5.5, Power.THOR)).isEmpty(),
                "none comes out");
        nobody.setItemInHand(InteractionHand.MAIN_HAND, titan);
        titan.use(helper.getLevel(), nobody, InteractionHand.MAIN_HAND);
        check(nobody.isUsingItem() && titan.getUseDuration(nobody) == ((InfinityGauntletItem) titan.getItem())
                .holdTicks(), "the three-second hold of 3.4");
        titan.finishUsingItem(helper.getLevel(), nobody);
        check(Powers.power(nobody) == Power.THANOS, "he is Thanos");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = GAUNTLET)
    public static void onlyABearerOrThanosWieldsAFilledGauntlet(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer nobody = f.player(4.5, FLOOR, 4.5);
        ItemStack gauntlet = gauntletWith(StoneKind.POWER);
        nobody.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
        InteractionResult result = gauntlet.use(helper.getLevel(), nobody, InteractionHand.MAIN_HAND).getResult();
        check(result == InteractionResult.CONSUME && !nobody.isUsingItem(), "refused: " + result);
        check("message.enhanced.gauntlet.not_yours".equals(lastActionBarKey(nobody)), "and told so");
        tick(nobody, 300);
        check(nobody.getHealth() == nobody.getMaxHealth(), "but not burned: the Gauntlet protects its wearer");
        for (Power power : List.of(Power.THOR, Power.SCARLET_WITCH, Power.PHOENIX, Power.THANOS)) {
            ServerPlayer wielder = f.player(14.5, FLOOR, 14.5, power);
            check(StoneRules.mayWield(wielder), power + " may wield it");
        }
        for (Power power : List.of(Power.DOCTOR_STRANGE, Power.SPIDER_MAN, Power.BILLY)) {
            check(!StoneRules.mayWield(f.player(10.5, FLOOR, 10.5, power)), power + " may not");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = GAUNTLET)
    public static void theStoneWheelCyclesThroughTheStonesInIt(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerPlayer thor = f.player(10.5, FLOOR, 10.5, Power.THOR);
        ItemStack gauntlet = gauntletWith(StoneKind.SPACE, StoneKind.SOUL, StoneKind.MIND);
        thor.setItemInHand(InteractionHand.OFF_HAND, gauntlet);
        List<StoneKind> seen = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            StoneWheel.select(thor, -1);
            seen.add(InfinityGauntletItem.contents(thor.getOffhandItem()).selectedStone());
        }
        check(seen.equals(List.of(StoneKind.SOUL, StoneKind.MIND, StoneKind.SPACE, StoneKind.SOUL)),
                "a tap: the next stone in socket order, round again: " + seen);
        StoneWheel.select(thor, StoneKind.POWER.ordinal());
        check(InfinityGauntletItem.contents(thor.getOffhandItem()).selectedStone() == StoneKind.SOUL,
                "a stone not in it cannot be picked");
        StoneWheel.select(thor, StoneKind.MIND.ordinal());
        check(InfinityGauntletItem.contents(thor.getOffhandItem()).selectedStone() == StoneKind.MIND,
                "the wheel picks the Mind Stone");
        check("hud.enhanced.stones.selected".equals(lastActionBarKey(thor)), "the action bar names it");
        helper.succeed();
    }

    // ---- the Snap ----

    @GameTest(template = ARENA, batch = SNAP)
    public static void theSnapDustsHalfOfTenZombiesAndNothingElse(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer thor = f.player(10.5, FLOOR, 10.5, Power.THOR);
        ServerPlayer friend = f.player(12.5, FLOOR, 12.5);
        List<Zombie> zombies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5 + i * 1.5, FLOOR, 3.5));
            zombie.setNoAi(true);
            zombies.add(zombie);
        }
        Zombie named = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, FLOOR, 17.5));
        named.setNoAi(true);
        named.setCustomName(Component.literal("Bob"));
        Villager villager = helper.spawn(EntityType.VILLAGER, new Vec3(17.5, FLOOR, 17.5));
        villager.setNoAi(true);
        Wolf wolf = helper.spawn(EntityType.WOLF, new Vec3(15.5, FLOOR, 8.5));
        wolf.setNoAi(true);
        wolf.tame(thor);
        f.onClose(() -> {
            for (Entity e : level.getEntitiesOfClass(Entity.class, arenaBox(helper),
                    e -> !(e instanceof ServerPlayer))) {
                e.discard();
            }
        });
        ItemStack gauntlet = fullGauntlet();
        thor.setItemInHand(InteractionHand.MAIN_HAND, gauntlet);
        thor.setShiftKeyDown(true);
        gauntlet.use(level, thor, InteractionHand.MAIN_HAND);
        check(Snap.charging(thor) && thor.isUsingItem(), "Sneak + use with all six: the Snap's charge");
        check(!Snap.chargeTick(thor, Snap.holdTicks() - 1), "not before three seconds");
        check(Snap.chargeTick(thor, Snap.holdTicks()), "at three seconds: the Snap");
        long dusted = zombies.stream().filter(Entity::isRemoved).count();
        check(dusted == 5, "half of ten zombies are dust: " + dusted);
        check(named.isAlive() && villager.isAlive() && wolf.isAlive() && friend.isAlive() && thor.isAlive(),
                "never a named creature, a villager, a pet or a player");
        TestKit.assertNear(thor.getHealth(), 2.0, 0.01, "the cost: Thor at 2 health");
        check(thor.hasEffect(MobEffects.WEAKNESS) && thor.getEffect(MobEffects.WEAKNESS).getAmplifier() == 1,
                "and Weakness II");
        StoneProgress progress = thor.getData(ModAttachments.STONES);
        check(progress.has(StoneProgress.SNAPPED), "the flag of the first Snap");
        check(progress.snapReadyAt() == level.getGameTime() + 24000, "a day before the next");
        check(!Snap.mayCharge(thor, InfinityGauntletItem.contents(gauntlet)), "so it may not charge again");
        check(Snap.chosen(zombies, 0.5, 7L).size() == 5 && Snap.chosen(zombies.subList(0, 3), 0.5, 0L).size() == 1,
                "every second one, rounded down");
        thor.setShiftKeyDown(false);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = SNAP_ARENA)
    public static void theSnapIsRefusedInABossArena(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerPlayer thor = f.player(10.5, FLOOR, 10.5, Power.THOR);
        BossFrameworkTests.start(f, thor, Containment.FIELD, false);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 12.5));
        Zombie other = helper.spawn(EntityType.ZOMBIE, new Vec3(13.5, FLOOR, 12.5));
        f.onClose(() -> {
            zombie.discard();
            other.discard();
        });
        float health = thor.getHealth();
        check(Snap.perform(thor) == -1 && zombie.isAlive() && other.isAlive(), "not here");
        check("message.enhanced.gauntlet.not_here".equals(lastActionBarKey(thor)), "he is told");
        check(thor.getHealth() == health && Snap.ready(thor), "and it costs nothing");
        helper.succeed();
    }

    // ---- Asgard's pedestal ----

    @GameTest(template = ARENA, batch = CORE)
    public static void thePedestalItemIsEmptyAndTheStoneIsShownToWhoMayTakeIt(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        BlockPos pos = pedestal(helper, 10, 10, StoneKind.MIND);
        BlockState state = helper.getLevel().getBlockState(pos);
        check(StonePedestalBlock.kind(state) == StoneKind.MIND, "a pedestal with the Mind Stone");
        check(new ItemStack(ModItems.STONE_PEDESTAL.get()).is(ModItems.STONE_PEDESTAL.get())
                && ModBlocks.STONE_PEDESTAL.get().defaultBlockState().getValue(StonePedestalBlock.STONE)
                == StonePedestalBlock.Stone.NONE, "the pedestal's item places an empty one");
        check(state.getDestroySpeed(helper.getLevel(), pos) < 0.0F, "unbreakable");
        ServerPlayer vision = f.player(9.5, FLOOR, 9.5, Power.VISION);
        check(take(helper, vision, pos) && carries(vision, StoneKind.MIND), "Vision takes his own stone");
        check(take(helper, vision, pos), "the Mind Stone is not counted by the ledger");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = ASGARD, timeoutTicks = 400)
    public static void asgardsVaultGetsTheRealityStonesPedestalOnce(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerLevel asgard = AsgardTestSupport.ensureAsgard(helper.getLevel().getServer());
        BlockPos pos = com.kolodkingal.enhanced.asgard.CityPlan.REALITY_PEDESTAL;
        asgard.getChunk(pos);
        BlockState before = asgard.getBlockState(pos);
        f.onClose(() -> asgard.setBlock(pos, before, net.minecraft.world.level.block.Block.UPDATE_ALL));
        com.kolodkingal.enhanced.stones.StoneSites.placeAsgardPedestal(asgard);
        BlockState placed = asgard.getBlockState(pos);
        check(placed.is(ModBlocks.STONE_PEDESTAL.get()) && StonePedestalBlock.kind(placed) == StoneKind.REALITY,
                "the Reality Stone's pedestal in the vault: " + placed);
        check(placed.getValue(StonePedestalBlock.FACING) == net.minecraft.core.Direction.EAST, "facing Gungnir");
        asgard.setBlock(pos, placed.setValue(StonePedestalBlock.STONE, StonePedestalBlock.Stone.NONE),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        com.kolodkingal.enhanced.stones.StoneSites.placeAsgardPedestal(asgard);
        check(StonePedestalBlock.kind(asgard.getBlockState(pos)) == null, "a pedestal already there is left as it is");
        com.kolodkingal.enhanced.stones.StoneSites.registerDialogue();
        check(com.kolodkingal.enhanced.asgard.npc.AsgardDialogue.find(com.kolodkingal.enhanced.asgard.npc.Role.HOGUN,
                "aether") != null, "Hogun warns of the Aether");
        helper.succeed();
    }
}
