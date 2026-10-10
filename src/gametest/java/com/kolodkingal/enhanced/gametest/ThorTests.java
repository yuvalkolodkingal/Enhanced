package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.block.MjolnirBlockEntity;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/** Thor, Mjolnir, Stormbreaker and the Bifrost. */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ThorTests {

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    /** Proves the player worthy: grants Hero of the Village. */
    private static void makeWorthy(ServerPlayer player) {
        AdvancementHolder hero = player.server.getAdvancements().get(Worthiness.HERO_OF_THE_VILLAGE);
        check(hero != null, "the Hero of the Village advancement exists");
        for (String criterion : hero.value().criteria().keySet()) {
            player.getAdvancements().award(hero, criterion);
        }
    }

    private static ServerPlayer thor(TestKit.Fixture f, double x, double z) {
        ServerPlayer thor = f.player(x, FLOOR, z, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        return thor;
    }

    private static boolean holdsMjolnir(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.MJOLNIR.get());
    }

    @GameTest(template = ARENA)
    public static void mjolnirRecipeIsForgedFromUruAndAStar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("mjolnir"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:mjolnir exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack n = new ItemStack(Items.NETHERITE_INGOT);
        ItemStack s = new ItemStack(Items.NETHER_STAR);
        ItemStack h = new ItemStack(Items.HEAVY_CORE);
        ItemStack r = new ItemStack(Items.BREEZE_ROD);
        ItemStack e = ItemStack.EMPTY;
        check(recipe.matches(CraftingInput.of(3, 3, List.of(n, s, n, n, h, n, e, r, e)), level), "N S N / N H N / _ R _");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.MJOLNIR.get()), "it makes Mjolnir");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theUnworthyCannotHoldMjolnir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.getInventory().add(new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(player);
        check(player.getInventory().countItem(ModItems.MJOLNIR.get()) == 0, "it slips out of the unworthy's inventory");
        check(Powers.is(player, Power.NONE), "and doesn't make them Thor");
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2),
                e -> e.getItem().is(ModItems.MJOLNIR.get()));
        check(dropped.size() == 1, "it falls at their feet");
        // ... where it comes to rest as a block that only the worthy can lift.
        BlockPos at = new BlockPos(10, 2, 10);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(ModBlocks.MJOLNIR.get(), at);
            ServerLevel level = helper.getLevel();
            BlockPos abs = helper.absolutePos(at);
            check(level.getBlockEntity(abs) instanceof MjolnirBlockEntity, "the block remembers its hammer");
            level.getBlockState(abs).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false));
            helper.assertBlockPresent(ModBlocks.MJOLNIR.get(), at);
            check(!helper.getLevel().getBlockState(abs).canBeReplaced(), "it doesn't budge for the unworthy");
        });
    }

    @GameTest(template = ARENA)
    public static void theWorthyLiftItAndBecomeThor(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        makeWorthy(player);
        check(Worthiness.isWorthy(player), "a Hero of the Village is worthy");
        BlockPos rest = new BlockPos(11, 2, 10);
        BlockPos abs = helper.absolutePos(rest);
        check(com.kolodkingal.enhanced.block.MjolnirBlock.setDown(helper.getLevel(), abs, new ItemStack(ModItems.MJOLNIR.get())), "set Mjolnir down");
        helper.getLevel().getBlockState(abs).useWithoutItem(helper.getLevel(), player, new BlockHitResult(Vec3.atCenterOf(abs),
                net.minecraft.core.Direction.UP, abs, false));
        helper.assertBlockPresent(Blocks.AIR, rest);
        check(holdsMjolnir(player), "lifted into their hand");
        PowerTicker.tick(player);
        check(Powers.is(player, Power.THOR), "the worthy become Thor");
        assertNear(player.getAttributeValue(Attributes.MAX_HEALTH), 30.0, 1.0E-6, "an Asgardian body: 30 health");
        check(player.mayFly(), "with Mjolnir in hand he can fly");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        PowerTicker.tick(player);
        check(!player.mayFly(), "without it he can't");

        // Vision is worthy too (Age of Ultron), but stays Vision.
        ServerPlayer vision = f.player(6.5, FLOOR, 6.5, Power.VISION);
        vision.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(vision);
        check(holdsMjolnir(vision) && Powers.is(vision, Power.VISION), "Vision can hold Mjolnir and stays Vision");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void thrownMjolnirHitsAndComesBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 3.5, 10.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        lookAt(thor, zombie);
        float before = zombie.getHealth();
        press(thor, 1);
        PowerData data = Powers.get(thor);
        check(!holdsMjolnir(thor) && data.hammerEntityId() != -1, "Mjolnir is in flight");
        check(helper.getLevel().getEntity(data.hammerEntityId()) instanceof ThrownWeaponEntity, "as a thrown hammer");
        boolean[] hit = {false};
        f.everyTick(150, tick -> {
            PowerTicker.tick(thor);
            hit[0] |= zombie.getHealth() < before || !zombie.isAlive();
            if (holdsMjolnir(thor)) {
                check(hit[0], "it hit the zombie on the way");
                check(data.hammerEntityId() == -1, "nothing left in flight");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void thorCallsMjolnirBackFromWhereItRests(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        BlockPos rest = new BlockPos(16, 2, 10);
        com.kolodkingal.enhanced.block.MjolnirBlock.setDown(helper.getLevel(), helper.absolutePos(rest), new ItemStack(ModItems.MJOLNIR.get()));
        press(thor, 1);
        helper.assertBlockPresent(Blocks.AIR, rest);
        f.everyTick(150, tick -> {
            PowerTicker.tick(thor);
            return holdsMjolnir(thor);
        });
    }

    @GameTest(template = ARENA)
    public static void lightningStrikesWhatThorLooksAt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 3.5, 10.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(14.5, FLOOR, 10.5));
        Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new Vec3(14.5, FLOOR, 12.0));
        lookAt(thor, zombie);
        float before = zombie.getHealth();
        float thorBefore = thor.getHealth();
        press(thor, 3);
        check(zombie.getHealth() < before, "the zombie is struck");
        check(creeper.isPowered(), "a creeper next to the strike is charged, as by any lightning");
        check(Powers.get(thor).is(Live.CALLING_LIGHTNING), "Thor raises Mjolnir to the sky");
        check(thor.getHealth() == thorBefore, "Thor isn't hurt");
        check(!thor.hurt(thor.damageSources().lightningBolt(), 10.0F), "lightning can't hurt Thor");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theGodOfThunderArcsLightningWithEveryBlow(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 8.5, 10.5);
        Zombie first = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.0, FLOOR, 10.5));
        Zombie second = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(13.0, FLOOR, 10.5));
        press(thor, 4);
        PowerData data = Powers.get(thor);
        check(data.toggle(Toggle.GOD_OF_THUNDER), "the God of Thunder is unleashed");
        float secondBefore = second.getHealth();
        lookAt(thor, first);
        thor.attack(first);
        check(second.getHealth() < secondBefore, "the lightning leaps to the next zombie");
        press(thor, 4);
        check(!data.toggle(Toggle.GOD_OF_THUNDER), "and calms down again");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void theBifrostTakesThorWhereHeLooks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 3.5, 10.5);
        BlockPos spot = new BlockPos(16, 1, 10);
        lookAt(thor, helper.absoluteVec(Vec3.atCenterOf(spot).add(0, 0.49, 0)));
        thor.setShiftKeyDown(true);
        press(thor, 5);
        thor.setShiftKeyDown(false);
        PowerData data = Powers.get(thor);
        check(data.is(Live.BIFROST), "the Bifrost's light gathers around him");
        f.everyTick(100, tick -> {
            PowerTicker.tick(thor);
            if (!data.is(Live.BIFROST)) {
                Vec3 rel = helper.relativeVec(thor.position());
                check(Math.abs(rel.x - 16.5) < 0.6 && Math.abs(rel.z - 10.5) < 0.6, "he lands where he looked: " + rel);
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA)
    public static void stormbreakerIsForgedFromUruAStarAndGrootsArm(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("stormbreaker"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:stormbreaker exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack n = new ItemStack(Items.NETHERITE_INGOT);
        ItemStack s = new ItemStack(Items.NETHER_STAR);
        ItemStack log = new ItemStack(Items.OAK_LOG);
        ItemStack e = ItemStack.EMPTY;
        check(recipe.matches(CraftingInput.of(3, 3, List.of(n, n, e, n, s, e, e, log, e)), level), "N N / N S / _ L");
        check(recipe.matches(CraftingInput.of(3, 3, List.of(e, n, n, e, s, n, e, new ItemStack(Items.MANGROVE_LOG), e)), level),
                "mirrored, with any log");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.STORMBREAKER.get()), "it makes Stormbreaker");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void anyoneCanWieldStormbreaker(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(4.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        player.doTick();
        check(player.getMainHandItem().is(ModItems.STORMBREAKER.get()), "nobody needs to be worthy to hold it");
        check(Powers.is(player, Power.NONE), "and holding it doesn't make anyone Thor");
        assertNear(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 12.0, 1.0E-6, "it hits for 12");
        check(!player.getMainHandItem().isDamageableItem(), "it never wears out");
        // Sneak + use: the Bifrost, to where they look.
        lookAt(player, helper.absoluteVec(new Vec3(14.5, 1.99, 10.5)));
        player.setShiftKeyDown(true);
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        check(Powers.get(player).is(Live.BIFROST), "the Bifrost answers anyone who holds Stormbreaker");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void thrownStormbreakerHitsAndComesBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        PowerTicker.tick(thor);
        check(thor.mayFly(), "Thor flies with Stormbreaker too");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        lookAt(thor, zombie);
        float before = zombie.getHealth();
        press(thor, 1);
        PowerData data = Powers.get(thor);
        check(thor.getMainHandItem().isEmpty() && helper.getLevel().getEntity(data.hammerEntityId()) instanceof ThrownWeaponEntity,
                "Stormbreaker is in flight");
        boolean[] hit = {false};
        f.everyTick(150, tick -> {
            PowerTicker.tick(thor);
            hit[0] |= zombie.getHealth() < before - 8.0F || !zombie.isAlive();
            if (thor.getMainHandItem().is(ModItems.STORMBREAKER.get())) {
                check(hit[0], "it cleaved the zombie on the way");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void thorCallsHisWeaponOutOfACreaturesHands(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(15.5, FLOOR, 10.5));
        zombie.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        press(thor, 1);
        check(zombie.getMainHandItem().isEmpty(), "it tears out of the zombie's hand");
        f.everyTick(150, tick -> {
            PowerTicker.tick(thor);
            return thor.getMainHandItem().is(ModItems.STORMBREAKER.get());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void theBifrostRemembersPlacesInEveryWorld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(4.5, FLOOR, 4.5);
        ServerLevel level = helper.getLevel();
        ThorAbilities.savePlace(player, "Camp");
        check(player.getData(ModAttachments.BIFROST).places().isEmpty(), "nothing is saved without Mjolnir or Stormbreaker in hand");
        ThorAbilities.bifrostHome(player);
        check(!Powers.get(player).is(Live.BIFROST), "and the Bifrost doesn't answer");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        ThorAbilities.savePlace(player, "\u00a7cCamp\n");
        BifrostData data = player.getData(ModAttachments.BIFROST);
        check(data.places().size() == 1 && data.places().get(0).name().equals("Camp"), "saved as Camp (no formatting): " + data);
        check(data.places().get(0).dimension() == level.dimension(), "in this world");
        Vec3 moved = helper.absoluteVec(new Vec3(14.5, FLOOR, 14.5));
        player.moveTo(moved.x, moved.y, moved.z);
        ThorAbilities.savePlace(player, "camp");
        data = player.getData(ModAttachments.BIFROST);
        check(data.places().size() == 1 && data.places().get(0).pos().distanceTo(moved) < 1.0E-6, "saving the name again moves it");
        check(data.places().get(0).name().equals("camp"), "under the name as typed this time");

        // A place saved earlier on the roof of the Nether.
        player.setData(ModAttachments.BIFROST, data.with(new BifrostData.Place("Nether roof", Level.NETHER, new Vec3(0.5, 128.0, 0.5), 90.0F)));
        ThorAbilities.bifrostToPlace(player, 1, "Somewhere else");
        check(!Powers.get(player).is(Live.BIFROST), "a place that isn't there any more (the list changed) is ignored");
        ThorAbilities.bifrostToPlace(player, 1, "Nether roof");
        check(Powers.get(player).is(Live.BIFROST), "the Bifrost gathers around them");
        f.everyTick(150, tick -> {
            PowerTicker.tick(player);
            if (player.level().dimension() != Level.NETHER) {
                return false;
            }
            assertNear(player.getX(), 0.5, 1.0E-6, "x in the Nether");
            assertNear(player.getZ(), 0.5, 1.0E-6, "z in the Nether");
            check(player.getY() >= 128.0 - 1.0E-6, "standing on the roof of the Nether");
            ThorAbilities.bifrostHome(player);
            check(!Powers.get(player).is(Live.BIFROST), "not again until the Bifrost has gathered its strength");
            check(player.getData(ModAttachments.BIFROST).cooldown(player.level().getGameTime()) > 0, "its cooldown is running");
            ThorAbilities.forgetPlace(player, 0, "camp");
            List<BifrostData.Place> left = player.getData(ModAttachments.BIFROST).places();
            check(left.size() == 1 && left.get(0).name().equals("Nether roof"), "the camp is forgotten");
            return true;
        });
    }

    @GameTest(template = ARENA)
    public static void bifrostNamesAreCleanedAndTheListIsBounded(GameTestHelper helper) {
        check(BifrostData.cleanName("  \u00a7lHome\u00a7r  ", "x").equals("Home"), "formatting and spaces are stripped");
        check(BifrostData.cleanName("\n\t", "1, 2, 3").equals("1, 2, 3"), "an empty name falls back to the coordinates");
        check(BifrostData.cleanName("a".repeat(100), "x").length() == BifrostData.MAX_NAME_LENGTH, "long names are cut");
        BifrostData data = BifrostData.EMPTY;
        for (int i = 0; i < BifrostData.MAX_PLACES; i++) {
            data = data.with(new BifrostData.Place("P" + i, Level.OVERWORLD, Vec3.ZERO, 0.0F));
        }
        check(data.isFull() && data.with(new BifrostData.Place("One more", Level.OVERWORLD, Vec3.ZERO, 0.0F)) == data, "at most 24 places");
        check(data.with(new BifrostData.Place("p3", Level.NETHER, Vec3.ZERO, 0.0F)).places().get(3).dimension() == Level.NETHER,
                "but an existing name can still be moved");
        check(data.without(99) == data && data.without(0).places().size() == BifrostData.MAX_PLACES - 1, "forgetting checks the index");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void speedstersCantSnatchMjolnir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 12.5, 10.5);
        ServerPlayer tommy = f.player(9.5, FLOOR, 10.5, Power.TOMMY);
        press(tommy, 2);
        check(holdsMjolnir(thor), "Tommy's Disarm Blitz leaves Mjolnir alone");
        ServerPlayer peter = f.player(9.5, FLOOR, 8.5, Power.QUICKSILVER);
        lookAt(peter, thor);
        press(peter, 3);
        check(holdsMjolnir(thor), "and so does Quicksilver's Pickpocket");
        check(TommyAbilities.BLITZ_RADIUS > 3, "sanity");
        helper.succeed();
    }

    private ThorTests() {
    }
}
