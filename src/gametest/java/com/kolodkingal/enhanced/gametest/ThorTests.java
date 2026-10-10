package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.MjolnirShatter;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.ability.Worthiness;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.block.MjolnirBlockEntity;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.entity.MjolnirPinEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import io.netty.buffer.Unpooled;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    private static boolean ownedBy(ItemStack hammer, ServerPlayer player) {
        MjolnirOwner owner = Worthiness.ownerOf(hammer);
        return owner != null && owner.is(player);
    }

    /** A Mjolnir that belongs to {@code owner}. */
    private static ItemStack ownedMjolnir(MjolnirOwner owner) {
        ItemStack hammer = new ItemStack(ModItems.MJOLNIR.get());
        hammer.set(ModDataComponents.OWNER.get(), owner);
        return hammer;
    }

    /** The first Mjolnir in the player's inventory, or an empty stack. */
    private static ItemStack carriedMjolnir(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.MJOLNIR.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Right click on the Mjolnir block at the absolute position. */
    private static void useBlock(GameTestHelper helper, ServerPlayer player, BlockPos abs) {
        ServerLevel level = helper.getLevel();
        level.getBlockState(abs).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    /** Lays the Mjolnir on the ground at the player's feet and lets them walk into it. */
    private static void walkInto(GameTestHelper helper, ServerPlayer player, ItemStack hammer) {
        ItemEntity lying = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), hammer);
        lying.setNoPickUpDelay();
        helper.getLevel().addFreshEntity(lying);
        lying.playerTouch(player);
    }

    private static List<ItemEntity> mjolnirsOnTheGround(Entity near) {
        return near.level().getEntitiesOfClass(ItemEntity.class, near.getBoundingBox().inflate(2), e -> e.getItem().is(ModItems.MJOLNIR.get()));
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
            if (holdsMjolnir(thor)) {
                // Nobody owned it: it is his from the moment he catches it.
                check(ownedBy(thor.getMainHandItem(), thor), "the Mjolnir nobody owned is Thor's now");
                return true;
            }
            return false;
        });
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

    // ------------------------------------------------------------------------------------------------------------
    // The lightning beam (V, held)

    /** V (or another key) goes down; a fresh Thor's beam is free to hit at once, whatever tick it is. */
    private static void hold(ServerPlayer player, int ability) {
        ThorAbilities.forgetBeamPace(player);
        AbilityHandler.onKey(player, ability, true);
    }

    private static void letGo(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, false);
    }

    /** A zombie (a Husk, which does not burn in the sun) that takes damage as written: no armour to soften it. */
    private static Zombie bareZombie(GameTestHelper helper, double x, double z) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x, FLOOR, z));
        zombie.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        return zombie;
    }

    /** One hit of the beam on {@code target}: V held for a single tick, then let go. Returns the damage it did. */
    private static float oneHit(ServerPlayer thor, Zombie target) {
        target.setHealth(target.getMaxHealth());
        hold(thor, 3);
        PowerTicker.tick(thor);
        letGo(thor, 3);
        return target.getMaxHealth() - target.getHealth();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void theBeamHitsTheFirstCreatureInLineButNotOneBehindAWall(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // Bare-handed, with a zombie in line and another right behind it.
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        Zombie first = bareZombie(helper, 8.5, 10.5);
        Zombie behind = bareZombie(helper, 12.5, 10.5);
        lookAt(thor, first);
        // Another Thor on the other side of the arena, with a wall between him and his zombie.
        ServerPlayer walled = f.player(3.5, FLOOR, 4.5, Power.THOR);
        Zombie hidden = bareZombie(helper, 12.5, 4.5);
        for (int y = 2; y <= 5; y++) {
            helper.setBlock(new BlockPos(8, y, 4), Blocks.STONE);
        }
        lookAt(walled, hidden);
        hold(thor, 3);
        hold(walled, 3);
        PowerData data = Powers.get(thor);
        check(data.is(Live.CALLING_LIGHTNING), "the beam is on while V is held");
        float thorBefore = thor.getHealth();
        f.everyTick(12, tick -> {
            PowerTicker.tick(thor);
            PowerTicker.tick(walled);
            if (tick < 12) {
                return false;
            }
            // Hits at ticks 1, 6 and 11: three hits of 2.
            assertNear(first.getMaxHealth() - first.getHealth(), 6.0, 1.0E-4, "the first zombie took three hits of 2");
            check(behind.getHealth() == behind.getMaxHealth(), "the zombie behind it is not touched");
            check(hidden.getHealth() == hidden.getMaxHealth(), "nor is the one behind the wall");
            check(thor.getHealth() == thorBefore, "Thor isn't hurt");
            check(!thor.hurt(thor.damageSources().lightningBolt(), 10.0F), "lightning can't hurt Thor");
            check(!thor.hurt(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.LIGHTNING_BEAM, walled, walled), 10.0F),
                    "nor another Thor's beam");
            letGo(thor, 3);
            check(!data.is(Live.CALLING_LIGHTNING), "the beam is off when V is let go");
            return true;
        });
    }

    @GameTest(template = ARENA)
    public static void theWeaponMakesTheBeamReachFartherAndHitHarder(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(EnhancedConfig.THOR_BEAM_RANGE.getDefault() == 24 && EnhancedConfig.THOR_BEAM_WEAPON_RANGE.getDefault() == 48,
                "24 blocks bare-handed, 48 with the weapon");
        check(EnhancedConfig.THOR_BEAM_DAMAGE.getDefault() == 2.0 && EnhancedConfig.THOR_BEAM_INTERVAL.getDefault() == 5,
                "2 damage every 5 ticks");
        f.config(EnhancedConfig.THOR_BEAM_RANGE, 6);
        f.config(EnhancedConfig.THOR_BEAM_WEAPON_RANGE, 14);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        Zombie near = bareZombie(helper, 7.5, 10.5);
        lookAt(thor, near);
        assertNear(oneHit(thor, near), 2.0, 1.0E-4, "bare-handed: 2 damage a hit");
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        assertNear(oneHit(thor, near), 4.0, 1.0E-4, "with Stormbreaker in hand: twice that");
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        assertNear(oneHit(thor, near), 4.0, 1.0E-4, "and with Mjolnir");

        // The God of Thunder on top: 1.5 times.
        press(thor, 4);
        check(Powers.get(thor).toggle(Toggle.GOD_OF_THUNDER), "the God of Thunder is unleashed");
        assertNear(oneHit(thor, near), 6.0, 1.0E-4, "the God of Thunder with the weapon: 6");
        thor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        assertNear(oneHit(thor, near), 3.0, 1.0E-4, "and bare-handed: 3");
        press(thor, 4);

        // Reach: a zombie 10 blocks off is out of reach bare-handed, but not with the weapon.
        Zombie far = bareZombie(helper, 13.5, 10.5);
        check(oneHit(thor, near) > 0.0F, "sanity: the near zombie is the first in line");
        near.discard();
        lookAt(thor, far);
        assertNear(ThorAbilities.beamRange(false), 6.0, 1.0E-9, "bare-handed range (config)");
        assertNear(ThorAbilities.beamRange(true), 14.0, 1.0E-9, "weapon range (config)");
        check(oneHit(thor, far) == 0.0F, "10 blocks away is out of reach bare-handed");
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        check(oneHit(thor, far) > 0.0F, "but not with Mjolnir in hand");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theWeaponsBeamChargesCreepersTheFirstTimeItHitsThem(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new Vec3(9.5, FLOOR, 10.5));
        lookAt(thor, creeper);
        hold(thor, 3);
        PowerTicker.tick(thor);
        letGo(thor, 3);
        check(creeper.getHealth() < creeper.getMaxHealth(), "the beam hurts the creeper");
        check(!creeper.isPowered(), "but bare-handed it doesn't charge it");
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        hold(thor, 3);
        PowerTicker.tick(thor);
        check(creeper.isPowered(), "through Mjolnir it does, as any lightning would");
        check(creeper.getRemainingFireTicks() <= 0, "without setting it alight");
        letGo(thor, 3);
        // And a pig turns into a zombified piglin (unless the world is peaceful).
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(9.5, FLOOR, 13.5));
        creeper.discard();
        lookAt(thor, pig);
        hold(thor, 3);
        PowerTicker.tick(thor);
        letGo(thor, 3);
        if (helper.getLevel().getDifficulty() != Difficulty.PEACEFUL) {
            check(pig.isRemoved() && !helper.getLevel().getEntitiesOfClass(ZombifiedPiglin.class, thor.getBoundingBox().inflate(16)).isEmpty(),
                    "the pig is a zombified piglin now");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theGodOfThunderBeamArcsToTheNextTwoCreatures(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 3.5, 10.5);
        Zombie first = bareZombie(helper, 8.5, 10.5);
        Zombie second = bareZombie(helper, 8.5, 13.5);
        Zombie third = bareZombie(helper, 8.5, 16.5);
        lookAt(thor, first);
        oneHit(thor, first);
        check(second.getHealth() == second.getMaxHealth() && third.getHealth() == third.getMaxHealth(), "without the God of Thunder nothing arcs");
        press(thor, 4);
        float firstBefore = first.getHealth();
        hold(thor, 3);
        PowerTicker.tick(thor);
        letGo(thor, 3);
        check(first.getHealth() < firstBefore, "the beam hurts the first zombie");
        check(second.getHealth() < second.getMaxHealth(), "the lightning arcs to the next");
        check(third.getHealth() < third.getMaxHealth(), "and on to the one after");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theBeamBurnsHungerAndStopsWhenThorStarvesOrIsASpectator(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 3.5, 10.5);
        PowerData data = Powers.get(thor);
        float exhaustion = thor.getFoodData().getExhaustionLevel();
        hold(thor, 3);
        PowerTicker.tick(thor);
        check(thor.getFoodData().getExhaustionLevel() > exhaustion, "firing the beam burns hunger");
        thor.getFoodData().setFoodLevel(0);
        PowerTicker.tick(thor);
        check(!data.is(Live.CALLING_LIGHTNING), "with an empty hunger bar the beam dies");
        hold(thor, 3);
        check(!data.is(Live.CALLING_LIGHTNING), "and can't be started");
        thor.getFoodData().setFoodLevel(20);
        hold(thor, 3);
        check(data.is(Live.CALLING_LIGHTNING), "fed again, it can");
        thor.setGameMode(GameType.SPECTATOR);
        PowerTicker.tick(thor);
        check(!data.is(Live.CALLING_LIGHTNING), "a spectator fires nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theBeamSparesTheSteedThorRidesAndLettingGoDoesNotResetItsPace(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        Pig steed = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(3.5, FLOOR, 10.5));
        check(thor.startRiding(steed, true), "Thor sits on the pig");
        Zombie zombie = bareZombie(helper, 9.5, 10.5);
        lookAt(thor, zombie);
        float steedBefore = steed.getHealth();
        assertNear(oneHit(thor, zombie), 2.0, 1.0E-4, "the beam goes past his steed, to the zombie");
        check(steed.getHealth() == steedBefore, "and the steed he rides isn't hurt");
        // Pressing and letting go again and again can't hit faster than the beam's own pace (one hit in this very tick).
        zombie.setHealth(zombie.getMaxHealth());
        for (int i = 0; i < 6; i++) {
            AbilityHandler.onKey(thor, 3, true);
            PowerTicker.tick(thor);
            letGo(thor, 3);
        }
        check(zombie.getMaxHealth() - zombie.getHealth() <= 2.0F + 1.0E-4F, "the pace holds: " + (zombie.getMaxHealth() - zombie.getHealth()));
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Mjolnir shatters

    /** The fragments in the air in this test's arena (the tests of a batch run side by side in the same world). */
    private static List<MjolnirFragmentEntity> fragments(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(MjolnirFragmentEntity.class, helper.getBounds());
    }

    private static void sneakPress(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        press(player, ability);
        player.setShiftKeyDown(false);
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void mjolnirShattersStrikesEveryZombieAndReassemblesInThorsHand(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 10.5, 10.5);
        PowerTicker.tick(thor);
        check(ownedBy(thor.getMainHandItem(), thor), "sanity: Mjolnir is Thor's");
        List<Zombie> zombies = List.of(bareZombie(helper, 5.5, 10.5), bareZombie(helper, 15.5, 8.5), bareZombie(helper, 10.5, 16.5));
        // A friendly creature isn't an enemy.
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, FLOOR, 12.5));
        sneakPress(thor, 1);
        PowerData data = Powers.get(thor);
        check(!holdsMjolnir(thor), "Mjolnir leaves his hand");
        check(fragments(helper).size() == 3, "one fragment for each of the three zombies, and none for the pig: " + fragments(helper).size());
        PowerTicker.tick(thor);
        check(data.hammerEntityId() != -1 && thor.mayFly(), "it counts as a weapon in flight: he keeps his wings");
        ItemStack stored = thor.getExistingData(ModAttachments.SHATTERED_MJOLNIR).orElse(ItemStack.EMPTY);
        check(ownedBy(stored, thor), "the hammer is kept in his data, owner and all");
        f.everyTick(120, tick -> {
            PowerTicker.tick(thor);
            if (!holdsMjolnir(thor)) {
                return false;
            }
            for (Zombie zombie : zombies) {
                check(zombie.getMaxHealth() - zombie.getHealth() >= 5.0F, "every zombie was struck by its fragment");
            }
            check(pig.getHealth() == pig.getMaxHealth(), "the pig was left alone");
            check(ownedBy(thor.getMainHandItem(), thor), "Mjolnir is back in his hand, and still his");
            check(fragments(helper).isEmpty() && data.hammerEntityId() == -1, "no fragment is left");
            check(thor.getExistingData(ModAttachments.SHATTERED_MJOLNIR).isEmpty(), "nothing is kept in his data any more");
            // The cooldown: not again for a while.
            sneakPress(thor, 1);
            check(holdsMjolnir(thor) && fragments(helper).isEmpty(), "it needs to gather itself before it can shatter again");
            return true;
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void withNobodyAroundTheFragmentsBurstOutInARingAndComeBack(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 10.5, 10.5);
        PowerTicker.tick(thor);
        sneakPress(thor, 1);
        check(!holdsMjolnir(thor) && fragments(helper).size() == MjolnirShatter.RING_FRAGMENTS, "a ring of fragments bursts out");
        f.everyTick(150, tick -> {
            PowerTicker.tick(thor);
            if (!holdsMjolnir(thor)) {
                return false;
            }
            check(ownedBy(thor.getMainHandItem(), thor), "Mjolnir is whole again, in his hand");
            return true;
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aFullHandSendsTheReassembledMjolnirToTheInventory(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 10.5, 10.5);
        PowerTicker.tick(thor);
        sneakPress(thor, 1);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        f.everyTick(150, tick -> {
            PowerTicker.tick(thor);
            ItemStack carried = carriedMjolnir(thor);
            if (carried.isEmpty()) {
                return false;
            }
            check(thor.getMainHandItem().is(Items.DIAMOND_SWORD), "his hand was busy, and keeps what it holds");
            check(ownedBy(carried, thor), "Mjolnir comes to his inventory instead, still his");
            return true;
        });
    }

    @GameTest(template = ARENA)
    public static void stormbreakerDoesNotShatter(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = f.player(10.5, FLOOR, 10.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        bareZombie(helper, 5.5, 10.5);
        sneakPress(thor, 1);
        check(fragments(helper).isEmpty(), "Stormbreaker doesn't burst into fragments");
        check(helper.getLevel().getEntity(Powers.get(thor).hammerEntityId()) instanceof ThrownWeaponEntity, "Sneak + R throws it, as R does");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void aShatteredMjolnirFallsWhereThorDiesOrLogsOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer thor = thor(f, 6.5, 6.5);
        PowerTicker.tick(thor);
        sneakPress(thor, 1);
        check(fragments(helper).size() == MjolnirShatter.RING_FRAGMENTS, "shattered");
        // He logs out (the same call the logout event makes).
        MjolnirShatter.onGone(thor);
        check(fragments(helper).isEmpty(), "the fragments are gone");
        List<ItemEntity> fallen = mjolnirsOnTheGround(thor);
        check(fallen.size() == 1 && ownedBy(fallen.get(0).getItem(), thor), "Mjolnir lies where he stood, still his");
        check(thor.getExistingData(ModAttachments.SHATTERED_MJOLNIR).isEmpty(), "nothing is kept in his data");
        fallen.get(0).discard();

        // And he dies.
        ServerPlayer other = thor(f, 15.5, 15.5);
        PowerTicker.tick(other);
        sneakPress(other, 1);
        check(fragments(helper).size() == MjolnirShatter.RING_FRAGMENTS, "the other Thor's shattered");
        other.hurt(level.damageSources().genericKill(), 1000.0F);
        check(!other.isAlive(), "he is dead");
        check(fragments(helper).isEmpty(), "the fragments are gone");
        check(mjolnirsOnTheGround(other).stream().anyMatch(e -> ownedBy(e.getItem(), other)), "Mjolnir lies where he fell, still his");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aShatteredMjolnirKeptInHisDataComesBackIfTheWorldWasClosedMeanwhile(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 10.5, 10.5);
        PowerTicker.tick(thor);
        sneakPress(thor, 1);
        // The world is closed (nothing remembers the fragments), and opened again.
        fragments(helper).forEach(Entity::discard);
        MjolnirShatter.clear();
        PowerTicker.tick(thor);
        check(holdsMjolnir(thor) && ownedBy(thor.getMainHandItem(), thor), "it is in his hand again, and his");
        check(thor.getExistingData(ModAttachments.SHATTERED_MJOLNIR).isEmpty(), "and no longer kept in pieces");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Mjolnir pins someone down

    /** Right click on {@code target} with the player's Sneak state as given; returns whether the click was taken. */
    private static boolean rightClick(ServerPlayer player, Entity target, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        PlayerInteractEvent.EntityInteract click = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, target);
        NeoForge.EVENT_BUS.post(click);
        player.setShiftKeyDown(false);
        return click.isCanceled();
    }

    /** The hammers lying on this entity. */
    private static List<MjolnirPinEntity> pinsNear(Entity target) {
        return target.level().getEntitiesOfClass(MjolnirPinEntity.class, target.getBoundingBox().inflate(1.5));
    }

    /** The Y and X/Z drift of an entity from where it was. */
    private static double drift(Entity entity, Vec3 from) {
        return entity.position().distanceTo(from);
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void mjolnirPinsAZombieInPlaceAndOnlyItsOwnerLiftsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 6.5, 10.5);
        PowerTicker.tick(thor);
        ServerPlayer hero = f.player(8.5, FLOOR, 12.5);
        makeWorthy(hero);
        Zombie zombie = bareZombie(helper, 8.5, 10.5);
        check(!rightClick(thor, zombie, false) && pinsNear(zombie).isEmpty(), "a plain right click doesn't pin");
        check(holdsMjolnir(thor), "and Mjolnir stays in his hand");
        check(rightClick(thor, zombie, true), "Sneak + right click is taken");
        check(!holdsMjolnir(thor), "Mjolnir is set down on the zombie");
        List<MjolnirPinEntity> pins = pinsNear(zombie);
        check(pins.size() == 1 && ownedBy(pins.get(0).hammer(), thor), "it lies on it, still Thor's");
        Vec3 pinned = zombie.position();
        // It tries to bolt, and jump.
        zombie.setDeltaMovement(0.8, 0.6, 0.4);
        zombie.hurtMarked = true;
        f.everyTick(60, t -> {
            PowerTicker.tick(thor);
            if (t == 1 || t == 2) {
                zombie.setDeltaMovement(0.8, 0.6, 0.4);
            }
            if (t <= 30) {
                check(drift(zombie, pinned) < 0.01, "the pinned zombie hasn't moved at tick " + t + ": " + drift(zombie, pinned));
            }
            if (t == 20) {
                // Another worthy player can't lift it: it will not budge.
                check(rightClick(hero, zombie, false), "the click is taken, but ...");
                check(pinsNear(zombie).size() == 1 && hero.getMainHandItem().isEmpty() && carriedMjolnir(hero).isEmpty(),
                        "another worthy player can't lift Mjolnir off it");
            }
            if (t == 30) {
                check(rightClick(thor, zombie, false), "its owner lifts it");
                check(pinsNear(zombie).isEmpty(), "the pin is gone");
                check(holdsMjolnir(thor) && ownedBy(thor.getMainHandItem(), thor), "Mjolnir is back in his hand, and his");
                zombie.setDeltaMovement(0.8, 0.0, 0.0);
            }
            if (t == 40) {
                check(zombie.getX() > pinned.x + 0.5, "the zombie is free to move again");
                return true;
            }
            return false;
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void mjolnirLetsGoByItselfAfterItsTime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(EnhancedConfig.MJOLNIR_PIN_DURATION.getDefault() == 1200, "a minute by default");
        f.config(EnhancedConfig.MJOLNIR_PIN_DURATION, 40);
        ServerPlayer thor = thor(f, 6.5, 10.5);
        PowerTicker.tick(thor);
        Zombie zombie = bareZombie(helper, 8.5, 10.5);
        check(rightClick(thor, zombie, true) && pinsNear(zombie).size() == 1, "pinned");
        Vec3 pinned = zombie.position();
        f.everyTick(60, t -> {
            PowerTicker.tick(thor);
            zombie.setDeltaMovement(0.5, 0.0, 0.0);
            if (!pinsNear(zombie).isEmpty()) {
                check(t < 45 && drift(zombie, pinned) < 0.01, "held until its time is up");
                return false;
            }
            check(t >= 39, "it held for its time: " + t);
            check(mjolnirsOnTheGround(zombie).stream().anyMatch(e -> ownedBy(e.getItem(), thor)), "Mjolnir falls beside it, still Thor's");
            return true;
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void mjolnirLetsGoWhenItsOwnerDiesOrTheZombieDies(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 6.5, 6.5);
        PowerTicker.tick(thor);
        Zombie one = bareZombie(helper, 8.5, 6.5);
        check(rightClick(thor, one, true) && pinsNear(one).size() == 1, "pinned");
        // The pinned zombie dies: the hammer falls, its owner kept.
        one.kill();
        ServerPlayer second = thor(f, 6.5, 14.5);
        PowerTicker.tick(second);
        Zombie two = bareZombie(helper, 8.5, 14.5);
        check(rightClick(second, two, true) && pinsNear(two).size() == 1, "the second is pinned");
        // Its owner dies: the zombie is free.
        second.hurt(helper.getLevel().damageSources().genericKill(), 1000.0F);
        check(!second.isAlive(), "the second Thor is dead");
        check(pinsNear(two).isEmpty() && !MjolnirPin.isPinned(two), "the zombie is free");
        check(mjolnirsOnTheGround(two).stream().anyMatch(e -> ownedBy(e.getItem(), second)), "Mjolnir lies beside it, still his");
        f.everyTick(10, t -> {
            if (!pinsNear(one).isEmpty()) {
                return false;
            }
            check(mjolnirsOnTheGround(one).stream().anyMatch(e -> ownedBy(e.getItem(), thor)), "the dead zombie's Mjolnir fell, still Thor's");
            return true;
        });
    }

    @GameTest(template = ARENA)
    public static void mjolnirLetsGoWhenItsOwnerLogsOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 6.5, 10.5);
        PowerTicker.tick(thor);
        Zombie zombie = bareZombie(helper, 8.5, 10.5);
        check(rightClick(thor, zombie, true) && MjolnirPin.isPinned(zombie), "pinned");
        // The same call the logout event makes.
        MjolnirPin.onOwnerGone(thor);
        check(pinsNear(zombie).isEmpty() && !MjolnirPin.isPinned(zombie), "the zombie is free");
        check(mjolnirsOnTheGround(zombie).stream().anyMatch(e -> ownedBy(e.getItem(), thor)), "Mjolnir lies beside it, still Thor's");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void thorCallsMjolnirOffSomeoneItPins(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 3.5, 10.5);
        PowerTicker.tick(thor);
        Zombie zombie = bareZombie(helper, 12.5, 10.5);
        check(rightClick(thor, zombie, true) && !holdsMjolnir(thor), "pinned, and Mjolnir is out of his hand");
        press(thor, 1);
        check(pinsNear(zombie).isEmpty() && !MjolnirPin.isPinned(zombie), "R calls Mjolnir off the zombie, which is free");
        f.everyTick(150, t -> {
            PowerTicker.tick(thor);
            if (!holdsMjolnir(thor)) {
                return false;
            }
            check(ownedBy(thor.getMainHandItem(), thor), "Mjolnir flies home to his hand");
            return true;
        });
    }

    @GameTest(template = ARENA)
    public static void theBossesCantBePinned(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(EntityType.ENDER_DRAGON.is(ModTags.PIN_IMMUNE) && EntityType.WITHER.is(ModTags.PIN_IMMUNE)
                && EntityType.WARDEN.is(ModTags.PIN_IMMUNE), "the Ender Dragon, the Wither and the Warden are immune");
        check(!EntityType.ZOMBIE.is(ModTags.PIN_IMMUNE), "a zombie is not");
        ServerPlayer thor = thor(f, 6.5, 10.5);
        PowerTicker.tick(thor);
        Warden warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(8.5, FLOOR, 10.5));
        check(rightClick(thor, warden, true), "the click is taken (Mjolnir doesn't open the Bifrost), but ...");
        check(pinsNear(warden).isEmpty() && !MjolnirPin.isPinned(warden), "the Warden isn't pinned");
        check(holdsMjolnir(thor), "and Mjolnir stays in his hand");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void aPlayerCantBePinnedWithPvpOffButIsHeldWithItOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 6.5, 10.5);
        PowerTicker.tick(thor);
        ServerPlayer victim = f.player(8.5, FLOOR, 10.5, Power.MAGNETO);
        f.pvp(false);
        check(rightClick(thor, victim, true), "the click is taken");
        check(pinsNear(victim).isEmpty() && holdsMjolnir(thor) && !MjolnirPin.isPinned(victim), "with PvP off a player can't be pinned");
        f.pvp(true);
        check(rightClick(thor, victim, true) && pinsNear(victim).size() == 1, "with PvP on they can");
        PowerTicker.tick(victim);
        check(Powers.get(victim).is(Live.PINNED), "their client is told to hold still");
        Vec3 pinned = victim.position();
        // They try to walk off, to fly and to teleport.
        victim.setPos(pinned.x + 3.0, pinned.y, pinned.z);
        PowerTicker.tick(victim);
        check(drift(victim, pinned) < 0.01, "they are put back where they were pinned: " + drift(victim, pinned));
        victim.setPos(pinned.x, pinned.y + 2.0, pinned.z);
        PowerTicker.tick(victim);
        check(drift(victim, pinned) < 0.01, "and brought down if they jump or fly");
        victim.getAbilities().flying = true;
        PowerTicker.tick(victim);
        check(!victim.getAbilities().flying, "they can't fly");
        check(!HexManager.onTeleport(helper.getLevel(), victim, pinned.add(5, 0, 0)), "nor teleport (Ender Pearls, the Bifrost, Vanish...)");
        AbilityHandler.onKey(victim, 5, true);
        check(victim.getAbilities().flying == false, "(Magneto's flight key does nothing)");
        // They can still look around: being put back doesn't turn them.
        victim.setYRot(victim.getYRot() + 90.0F);
        float looking = victim.getYRot();
        victim.setPos(pinned.x + 2.0, pinned.y, pinned.z);
        PowerTicker.tick(victim);
        check(victim.getYRot() == looking && drift(victim, pinned) < 0.01, "they can look around, and are still held");
        // A pinned creature isn't knocked about either.
        victim.knockback(2.0, 1.0, 0.0);
        check(victim.getDeltaMovement().x == 0.0, "nor knocked back");
        check(rightClick(thor, victim, false), "its owner lifts Mjolnir off them");
        PowerTicker.tick(victim);
        check(!Powers.get(victim).is(Live.PINNED) && pinsNear(victim).isEmpty(), "and they are free");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void killingTheHammerThatPinsSomeoneStillDropsMjolnir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 6.5, 10.5);
        PowerTicker.tick(thor);
        Zombie zombie = bareZombie(helper, 8.5, 10.5);
        check(rightClick(thor, zombie, true) && pinsNear(zombie).size() == 1, "pinned");
        // Something kills the entity that carries the hammer (a /kill of everything, say).
        pinsNear(zombie).forEach(Entity::kill);
        check(pinsNear(zombie).isEmpty() && !MjolnirPin.isPinned(zombie), "the pin is gone, and the zombie is free");
        check(mjolnirsOnTheGround(zombie).stream().anyMatch(e -> ownedBy(e.getItem(), thor)), "Mjolnir isn't lost: it lies beside it, still Thor's");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void aPinnedCreatureCantRideTeleportOrTravel(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thor = thor(f, 6.5, 10.5);
        PowerTicker.tick(thor);
        Zombie zombie = bareZombie(helper, 8.5, 10.5);
        Pig steed = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.5, FLOOR, 12.5));
        check(rightClick(thor, zombie, true) && pinsNear(zombie).size() == 1, "pinned");
        check(!zombie.startRiding(steed), "it can't climb onto a steed");
        Vec3 pinned = zombie.position();
        zombie.setPos(zombie.getX() + 1.0, zombie.getY(), zombie.getZ());
        f.everyTick(4, t -> {
            if (t < 4) {
                return false;
            }
            check(MjolnirPin.isPinned(zombie) && drift(zombie, pinned) < 0.01, "still pinned, and put back where it was");
            check(!HexManager.onTeleport(helper.getLevel(), zombie, zombie.position().add(4, 0, 0)), "it can't be teleported");
            return true;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Mjolnir belongs to one player

    @GameTest(template = ARENA)
    public static void theFirstWorthyHandToLiftMjolnirClaimsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        check(EnhancedConfig.MJOLNIR_OWNER_ONLY.getDefault() && EnhancedConfig.MJOLNIR_OWNER_ONLY.get(), "ownership is on by default");
        // Fresh from the forge (or the creative tab) it is nobody's. The unworthy can't keep it, so it stays that way.
        ServerPlayer villager = f.player(4.5, FLOOR, 4.5);
        ItemStack fresh = new ItemStack(ModItems.MJOLNIR.get());
        check(Worthiness.ownerOf(fresh) == null, "a fresh Mjolnir has no owner");
        villager.getInventory().add(fresh);
        PowerTicker.tick(villager);
        List<ItemEntity> fallen = mjolnirsOnTheGround(villager);
        check(fallen.size() == 1 && Worthiness.ownerOf(fallen.get(0).getItem()) == null, "it slips from the unworthy still unclaimed");
        ItemStack unworthyForge = new ItemStack(ModItems.MJOLNIR.get());
        unworthyForge.onCraftedBy(level, villager, 1);
        check(Worthiness.ownerOf(unworthyForge) == null, "the unworthy don't claim what they forge");

        // The worthy who lift it claim it, and become Thor.
        ServerPlayer hero = f.player(10.5, FLOOR, 4.5);
        makeWorthy(hero);
        hero.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(hero);
        MjolnirOwner owner = Worthiness.ownerOf(hero.getMainHandItem());
        check(owner != null && owner.is(hero) && owner.name().equals(hero.getGameProfile().getName()), "the Hero of the Village is its owner: " + owner);
        check(Powers.is(hero, Power.THOR), "and becomes Thor");

        // Vision is worthy too: whoever holds an unclaimed one first claims it, and Vision stays Vision.
        ServerPlayer vision = f.player(16.5, FLOOR, 4.5, Power.VISION);
        vision.getInventory().add(new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(vision);
        check(vision.getInventory().countItem(ModItems.MJOLNIR.get()) == 1, "Vision keeps it");
        check(ownedBy(carriedMjolnir(vision), vision), "and owns it");
        check(Powers.is(vision, Power.VISION), "Vision stays Vision");

        // So do the worthy who forge it, the moment they take it from the table ...
        ServerPlayer smith = f.player(4.5, FLOOR, 12.5);
        makeWorthy(smith);
        ItemStack forged = new ItemStack(ModItems.MJOLNIR.get());
        forged.onCraftedBy(level, smith, 1);
        check(ownedBy(forged, smith), "a worthy smith owns the Mjolnir they forge");
        // ... and Creative players, who are worthy of everything.
        ServerPlayer creative = f.player(10.5, FLOOR, 12.5, GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(creative);
        check(ownedBy(creative.getMainHandItem(), creative), "a Creative player claims an unclaimed one as usual");

        // Lifted from the ground as a block: claimed on the way up.
        ServerPlayer lifter = f.player(16.5, FLOOR, 12.5);
        makeWorthy(lifter);
        BlockPos rest = new BlockPos(15, 2, 12);
        BlockPos abs = helper.absolutePos(rest);
        check(MjolnirBlock.setDown(level, abs, new ItemStack(ModItems.MJOLNIR.get())), "set Mjolnir down");
        useBlock(helper, lifter, abs);
        helper.assertBlockPresent(Blocks.AIR, rest);
        check(ownedBy(lifter.getMainHandItem(), lifter), "lifting it from the ground claims it");

        // Claiming is permanent: the next worthy hand to hold it finds it taken.
        ServerPlayer latecomer = f.player(10.5, FLOOR, 8.5, Power.VISION);
        latecomer.getInventory().add(lifter.getMainHandItem().copy());
        PowerTicker.tick(latecomer);
        check(latecomer.getInventory().countItem(ModItems.MJOLNIR.get()) == 0, "nobody else can take it over");
        check(ownedBy(lifter.getMainHandItem(), lifter), "it is still the lifter's");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void onlyTheOwnerCanLiftTheirMjolnir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = thor(f, 4.5, 4.5);
        PowerTicker.tick(owner);
        ItemStack hammer = owner.getMainHandItem().copy();
        MjolnirOwner mark = Worthiness.ownerOf(hammer);
        check(mark != null && mark.is(owner), "Thor owns the Mjolnir he first lifted");
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ServerPlayer hero = f.player(10.5, FLOOR, 10.5);
        makeWorthy(hero);
        ServerPlayer vision = f.player(14.5, FLOOR, 14.5, Power.VISION);
        ServerPlayer otherThor = f.player(4.5, FLOOR, 14.5, Power.THOR);
        List<ServerPlayer> others = List.of(hero, vision, otherThor);
        for (ServerPlayer other : others) {
            check(Worthiness.isWorthy(other), "they are all worthy ...");
            check(mark.equals(Worthiness.ownerKeepingFrom(other, hammer)), "... but it is not theirs");
        }
        check(Worthiness.canLift(owner, hammer), "while its owner can lift it");

        // It slips out of their inventory ...
        for (ServerPlayer other : others) {
            other.getInventory().add(hammer.copy());
            PowerTicker.tick(other);
            check(other.getInventory().countItem(ModItems.MJOLNIR.get()) == 0, "it slips out of " + other.getGameProfile().getName() + "'s inventory");
            List<ItemEntity> fallen = mjolnirsOnTheGround(other);
            check(fallen.size() == 1 && mark.equals(Worthiness.ownerOf(fallen.get(0).getItem())), "it falls at their feet, still its owner's");
            fallen.get(0).discard();
        }
        // ... and off the cursor.
        hero.containerMenu.setCarried(hammer.copy());
        PowerTicker.tick(hero);
        check(hero.containerMenu.getCarried().isEmpty() && mjolnirsOnTheGround(hero).size() == 1, "it slips off the cursor too");
        check(Powers.is(hero, Power.NONE) && Powers.is(vision, Power.VISION), "and nobody's power changes");

        // They can't pick it up.
        for (ServerPlayer other : others) {
            walkInto(helper, other, hammer.copy());
            check(other.getInventory().countItem(ModItems.MJOLNIR.get()) == 0, other.getGameProfile().getName() + " can't pick it up");
        }
        walkInto(helper, owner, hammer.copy());
        check(owner.getInventory().countItem(ModItems.MJOLNIR.get()) == 1, "but its owner can");
        owner.getInventory().clearContent();

        // They can't lift it as a block either, and the block remembers whose it is.
        BlockPos rest = new BlockPos(8, 2, 14);
        BlockPos abs = helper.absolutePos(rest);
        check(MjolnirBlock.setDown(level, abs, hammer.copy()), "set Mjolnir down");
        check(level.getBlockEntity(abs) instanceof MjolnirBlockEntity be && mark.equals(Worthiness.ownerOf(be.hammer())),
                "the block remembers its owner");
        for (ServerPlayer other : others) {
            useBlock(helper, other, abs);
            helper.assertBlockPresent(ModBlocks.MJOLNIR.get(), rest);
            check(other.getInventory().countItem(ModItems.MJOLNIR.get()) == 0 && other.getMainHandItem().isEmpty(),
                    "it doesn't budge for " + other.getGameProfile().getName());
        }
        useBlock(helper, owner, abs);
        helper.assertBlockPresent(Blocks.AIR, rest);
        check(holdsMjolnir(owner) && ownedBy(owner.getMainHandItem(), owner), "its owner lifts it, and it is still theirs");
        PowerTicker.tick(owner);
        check(holdsMjolnir(owner) && Powers.is(owner, Power.THOR), "and keeps it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void creativePlayersCanMoveAnyMjolnirButNeverTakeItOver(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        MjolnirOwner odin = new MjolnirOwner(UUID.randomUUID(), "Odin");
        ServerPlayer op = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        check(Worthiness.canLift(op, ownedMjolnir(odin)), "a Creative player may lift anyone's Mjolnir");

        op.setItemInHand(InteractionHand.MAIN_HAND, ownedMjolnir(odin));
        PowerTicker.tick(op);
        check(holdsMjolnir(op), "they can hold it");
        check(odin.equals(Worthiness.ownerOf(op.getMainHandItem())), "and it is still Odin's");
        check(Powers.is(op, Power.NONE), "which doesn't make them Thor");

        walkInto(helper, op, ownedMjolnir(odin));
        check(op.getInventory().countItem(ModItems.MJOLNIR.get()) == 2, "they can pick it up");

        BlockPos rest = new BlockPos(15, 2, 15);
        BlockPos abs = helper.absolutePos(rest);
        check(MjolnirBlock.setDown(level, abs, ownedMjolnir(odin)), "set Mjolnir down");
        useBlock(helper, op, abs);
        helper.assertBlockPresent(Blocks.AIR, rest);
        PowerTicker.tick(op);
        check(op.getInventory().countItem(ModItems.MJOLNIR.get()) == 3, "and lift it from the ground");
        for (int slot = 0; slot < op.getInventory().getContainerSize(); slot++) {
            ItemStack stack = op.getInventory().getItem(slot);
            check(!stack.is(ModItems.MJOLNIR.get()) || odin.equals(Worthiness.ownerOf(stack)), "none of them changed hands");
        }

        // An unclaimed one, though, is claimed in Creative as usual.
        ServerPlayer builder = f.player(4.5, FLOOR, 4.5, GameType.CREATIVE);
        builder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(builder);
        check(ownedBy(builder.getMainHandItem(), builder), "an unclaimed one becomes the Creative player's");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void thorCallsBackOnlyMjolnirsThatAreHis(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, Power.THOR);
        MjolnirOwner odin = new MjolnirOwner(UUID.randomUUID(), "Odin");
        // Odin's Mjolnir rests close by, a zombie nearer still holds another of Odin's, a third lies on the ground right
        // next to Thor, and a fourth is in his own pocket.
        BlockPos odinsBlock = new BlockPos(6, 2, 10);
        check(MjolnirBlock.setDown(level, helper.absolutePos(odinsBlock), ownedMjolnir(odin)), "set Odin's Mjolnir down");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, FLOOR, 8.5));
        zombie.setItemInHand(InteractionHand.MAIN_HAND, ownedMjolnir(odin));
        Vec3 beside = helper.absoluteVec(new Vec3(4.5, FLOOR, 10.5));
        ItemEntity lying = new ItemEntity(level, beside.x, beside.y, beside.z, ownedMjolnir(odin));
        level.addFreshEntity(lying);
        thor.getInventory().setItem(5, ownedMjolnir(odin));
        press(thor, 1);
        check(Powers.get(thor).hammerEntityId() == -1 && !holdsMjolnir(thor), "Thor can't call a Mjolnir that is Odin's");
        helper.assertBlockPresent(ModBlocks.MJOLNIR.get(), odinsBlock);
        check(zombie.getMainHandItem().is(ModItems.MJOLNIR.get()), "and the zombie keeps its grip on it");
        check(lying.isAlive() && lying.getItem().is(ModItems.MJOLNIR.get()), "and the one on the ground stays where it lies");
        check(thor.getInventory().getItem(5).is(ModItems.MJOLNIR.get()), "and the one in his pocket doesn't come to his hand");
        lying.discard();
        thor.getInventory().clearContent();

        // His own, farther away, comes at once.
        BlockPos hisBlock = new BlockPos(16, 2, 10);
        check(MjolnirBlock.setDown(level, helper.absolutePos(hisBlock), ownedMjolnir(MjolnirOwner.of(thor))), "set Thor's own Mjolnir down");
        press(thor, 1);
        helper.assertBlockPresent(Blocks.AIR, hisBlock);
        helper.assertBlockPresent(ModBlocks.MJOLNIR.get(), odinsBlock);
        f.everyTick(150, tick -> {
            PowerTicker.tick(thor);
            if (!holdsMjolnir(thor)) {
                return false;
            }
            check(ownedBy(thor.getMainHandItem(), thor), "it is his own Mjolnir that comes");
            check(zombie.getMainHandItem().is(ModItems.MJOLNIR.get()), "the zombie still holds Odin's");
            helper.assertBlockPresent(ModBlocks.MJOLNIR.get(), odinsBlock);
            return true;
        });
    }

    @GameTest(template = ARENA)
    public static void aCreativeThorCanMoveAnyMjolnirButStillCallsOnlyHis(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer thor = f.player(3.5, FLOOR, 10.5, GameType.CREATIVE);
        Powers.setPower(thor, Power.THOR);
        MjolnirOwner odin = new MjolnirOwner(UUID.randomUUID(), "Odin");
        check(Worthiness.canLift(thor, ownedMjolnir(odin)), "an operator can lift Odin's Mjolnir by hand ...");
        check(!Worthiness.canCall(thor, ownedMjolnir(odin)), "... but his call is never answered by it");
        check(Worthiness.canCall(thor, new ItemStack(ModItems.MJOLNIR.get())), "an unclaimed one answers it");
        check(Worthiness.canCall(thor, ownedMjolnir(MjolnirOwner.of(thor))), "and so does his own");
        BlockPos odinsBlock = new BlockPos(6, 2, 10);
        check(MjolnirBlock.setDown(level, helper.absolutePos(odinsBlock), ownedMjolnir(odin)), "set Odin's Mjolnir down");
        press(thor, 1);
        check(Powers.get(thor).hammerEntityId() == -1 && !holdsMjolnir(thor), "Thor can't call a Mjolnir that is Odin's, even in Creative");
        helper.assertBlockPresent(ModBlocks.MJOLNIR.get(), odinsBlock);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theOwnersNameOnTheHammerFollowsThemWhenTheyRename(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer owner = thor(f, 4.5, 4.5);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ownedMjolnir(new MjolnirOwner(owner.getUUID(), "OldName")));
        PowerTicker.tick(owner);
        MjolnirOwner now = Worthiness.ownerOf(owner.getMainHandItem());
        check(now != null && now.is(owner) && now.name().equals(owner.getGameProfile().getName()), "the hammer shows their new name: " + now);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void withOwnershipOffAnyWorthyPlayerCanLiftAnyMjolnir(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.MJOLNIR_OWNER_ONLY, false);
        ServerLevel level = helper.getLevel();
        MjolnirOwner odin = new MjolnirOwner(UUID.randomUUID(), "Odin");
        ItemStack hammer = ownedMjolnir(odin);
        ServerPlayer hero = f.player(10.5, FLOOR, 10.5);
        makeWorthy(hero);
        check(Worthiness.canLift(hero, hammer), "the old rule: any worthy player can lift any Mjolnir");
        hero.getInventory().add(hammer.copy());
        PowerTicker.tick(hero);
        check(hero.getInventory().countItem(ModItems.MJOLNIR.get()) == 1, "it stays in the Hero of the Village's inventory");
        check(Powers.is(hero, Power.THOR), "who becomes Thor, as before there were owners");
        check(odin.equals(Worthiness.ownerOf(carriedMjolnir(hero))), "and it is still marked as Odin's");

        ServerPlayer vision = f.player(14.5, FLOOR, 14.5, Power.VISION);
        walkInto(helper, vision, hammer.copy());
        check(vision.getInventory().countItem(ModItems.MJOLNIR.get()) == 1, "Vision picks it up");
        BlockPos rest = new BlockPos(8, 2, 14);
        BlockPos abs = helper.absolutePos(rest);
        check(MjolnirBlock.setDown(level, abs, hammer.copy()), "set Mjolnir down");
        ServerPlayer smith = f.player(4.5, FLOOR, 14.5);
        makeWorthy(smith);
        useBlock(helper, smith, abs);
        helper.assertBlockPresent(Blocks.AIR, rest);
        check(holdsMjolnir(smith), "and the smith lifts it from the ground");

        // Nobody claims anything either, but the unworthy still can't hold it.
        ServerPlayer claimer = f.player(4.5, FLOOR, 4.5, Power.VISION);
        claimer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(claimer);
        check(holdsMjolnir(claimer) && Worthiness.ownerOf(claimer.getMainHandItem()) == null, "an unclaimed one stays unclaimed");
        ServerPlayer villager = f.player(16.5, FLOOR, 4.5);
        villager.getInventory().add(hammer.copy());
        PowerTicker.tick(villager);
        check(villager.getInventory().countItem(ModItems.MJOLNIR.get()) == 0, "the unworthy still can't keep it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theOwnerIsSavedSentAndShownOnTheTooltip(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer hero = f.player(10.5, FLOOR, 10.5);
        ItemStack hammer = ownedMjolnir(MjolnirOwner.of(hero));
        // Saved with the item ...
        CompoundTag saved = (CompoundTag) hammer.save(level.registryAccess());
        check(ownedBy(ItemStack.parseOptional(level.registryAccess(), saved), hero), "the owner is saved with the item");
        // ... sent to clients with it ...
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        ItemStack.STREAM_CODEC.encode(buf, hammer);
        check(ownedBy(ItemStack.STREAM_CODEC.decode(buf), hero), "and sent to clients with it");
        // ... and kept by the block it is set down as, through a save and a load.
        BlockPos abs = helper.absolutePos(new BlockPos(8, 2, 8));
        check(MjolnirBlock.setDown(level, abs, hammer), "set Mjolnir down");
        MjolnirBlockEntity resting = (MjolnirBlockEntity) level.getBlockEntity(abs);
        MjolnirBlockEntity loaded = new MjolnirBlockEntity(abs, level.getBlockState(abs));
        loaded.loadWithComponents(resting.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
        check(ownedBy(loaded.hammer(), hero), "the block keeps the owner through a save and a load");

        // The tooltip says whose it is, or that it is up for the taking.
        check(tooltipHas(hammer, "item.enhanced.mjolnir.bound", hero.getGameProfile().getName()), "the tooltip says: Bound to <name>");
        check(tooltipHas(new ItemStack(ModItems.MJOLNIR.get()), "item.enhanced.mjolnir.unclaimed", null), "an unclaimed one says so");
        helper.succeed();
    }

    /** Whether the stack's tooltip has a line with this translation key (and, if given, this first argument). */
    private static boolean tooltipHas(ItemStack stack, String key, String argument) {
        for (Component line : stack.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL)) {
            if (line.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(key)
                    && (argument == null || (translatable.getArgs().length == 1 && argument.equals(translatable.getArgs()[0])))) {
                return true;
            }
        }
        return false;
    }

    private ThorTests() {
    }
}
