package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.apocalypse.Horsemen;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.gametest.ApocalypseTestSupport.Log;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The Capstone and the ritual (spec 3.2 and 8, tests 1 to 6): the recipe, what wakes him and what refuses to, and what
 * the boss is when nothing owns it. The ritual here is the real one (a Capstone, Sneak and right-click on sand under the
 * open sky in daylight, a Scarlet Witch near); only the arena is made small.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class ApocalypseRitualTests {
    /** The block of the floor the Capstone is planted on (relative coordinates). */
    static final BlockPos CLICK = new BlockPos(10, 1, 10);

    // ------------------------------------------------------------------------------------------------------------
    // The recipe and the items

    @GameTest(template = ARENA, batch = "apocalypse_pure")
    public static void capstoneRecipeIsAPyramid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("capstone"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:capstone is a shaped recipe");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack result = recipe.getResultItem(level.registryAccess());
        check(result.is(ModItems.CAPSTONE.get()) && result.getCount() == 1, "it makes one Capstone");
        check(recipe.getWidth() == 3 && recipe.getHeight() == 3, "in a 3 by 3 pattern");

        ItemStack e = ItemStack.EMPTY;
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        ItemStack gold = new ItemStack(Items.GOLD_BLOCK);
        ItemStack lapis = new ItemStack(Items.LAPIS_BLOCK);
        check(recipe.matches(CraftingInput.of(3, 3, List.of(e, star, e, gold, lapis, gold, gold, gold, gold)), level), "a Nether Star over a Lapis Block in a pyramid of five Gold Blocks");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(e, star, e, gold, gold, gold, gold, gold, gold)), level), "without the Lapis it is nothing");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(e, lapis, e, gold, star, gold, gold, gold, gold)), level), "the star is at the top");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(e, star, e, gold, lapis, gold, gold, e, gold)), level), "five Gold Blocks, not four");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(e, new ItemStack(Items.GOLD_INGOT), e, gold, lapis, gold, gold, gold, gold)), level), "a Gold Ingot is not the star");
        int ingredients = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty()) {
                ingredients++;
            }
        }
        check(ingredients == 7, "seven ingredients: one star, one lapis, five gold: " + ingredients);

        // The advancement that shows the recipe exists.
        var advancement = helper.getLevel().getServer().getAdvancements().get(Enhanced.id("recipes/misc/capstone"));
        check(advancement != null, "its recipe advancement exists");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_pure")
    public static void capstoneAndEssenceAreInTheCreativeTab(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
        var tab = com.kolodkingal.enhanced.registry.ModCreativeTab.ENHANCED.get();
        var items = tab.getDisplayItems();
        check(items.stream().anyMatch(s -> s.is(ModItems.CAPSTONE.get())), "the Capstone is in the creative tab");
        check(items.stream().anyMatch(s -> s.is(ModItems.ESSENCE.get())), "and so is the Essence");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "apocalypse_pure")
    public static void essenceAndCapstoneAreNotInChests(GameTestHelper helper) throws Exception {
        MinecraftServer server = helper.getLevel().getServer();
        var manager = server.getResourceManager();
        int files = 0;
        for (String folder : List.of("loot_modifiers", "loot_table")) {
            for (var entry : manager.listResources(folder, id -> id.getNamespace().equals(Enhanced.MODID) && id.getPath().endsWith(".json")).entrySet()) {
                try (var reader = entry.getValue().openAsReader()) {
                    String text = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                    check(!text.contains("enhanced:capstone") && !text.contains("enhanced:essence"), entry.getKey() + " does not hand out the Capstone or the Essence");
                    files++;
                }
            }
        }
        check(files > 5, "the loot files were read (" + files + ")");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Setting the stage

    /** The floor open to the sky, sand where the Capstone goes, and broad daylight. */
    static void readyGround(TestKit.Fixture f) {
        var helper = f.helper();
        for (int x = 1; x <= 19; x++) {
            for (int z = 1; z <= 19; z++) {
                for (int y = 2; y <= 22; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!helper.getBlockState(pos).isAir()) {
                        helper.setBlock(pos, Blocks.AIR);
                    }
                }
            }
        }
        helper.setBlock(CLICK, Blocks.SANDSTONE);
        day(f, true);
    }

    /** Sets the time of day for the length of the test. */
    static void day(TestKit.Fixture f, boolean day) {
        ServerLevel level = f.level();
        long before = level.getDayTime();
        level.setDayTime(day ? 6000L : 18000L);
        f.onClose(() -> level.setDayTime(before));
    }

    /** A Capstone planted, sneaking, on the block, by the player: whatever the item answers. */
    static void plant(ServerPlayer player, BlockPos click) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        player.setShiftKeyDown(true);
        Vec3 at = Vec3.atCenterOf(click).add(0.0, 0.5, 0.0);
        BlockHitResult hit = new BlockHitResult(at, Direction.UP, click, false);
        stack.getItem().useOn(new UseOnContext(player.serverLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
    }

    /** A survival player with a Capstone in the hand. */
    static ServerPlayer starter(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = f.player(x, FLOOR, z);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAPSTONE.get()));
        ApocalypseTestSupport.tank(player);
        return player;
    }

    private static BlockPos absolute(TestKit.Fixture f, BlockPos relative) {
        return f.helper().absolutePos(relative);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The ritual

    @GameTest(template = ARENA, batch = "apocalypse_ritual", timeoutTicks = 320)
    public static void ritualStartsTheFightOnSandInDaylight(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        readyGround(f);
        ServerPlayer wanda = f.player(10.5, FLOOR, 15.5, Power.SCARLET_WITCH);
        ApocalypseTestSupport.tank(wanda);
        ServerPlayer starter = starter(f, 10.5, 5.5);
        ItemStack capstone = starter.getMainHandItem();
        Log log = new Log(starter);
        ServerLevel level = f.level();
        // The sun is up and the sky is open (a failure here is the test world, not the mod).
        helper.runAfterDelay(3, () -> {
            plant(starter, absolute(f, CLICK));
            check(BossFights.active(level) instanceof ApocalypseFight, "the fight begins; the action bar says: " + log.drain().actionBarKeys()
                    + " (day " + level.isDay() + ", sky " + level.canSeeSky(absolute(f, CLICK).above()) + ")");
            ApocalypseFight fight = (ApocalypseFight) BossFights.active(level);
            check(capstone.isEmpty(), "the Capstone is used up");
            check(starter.getCooldowns().isOnCooldown(ModItems.CAPSTONE.get()), "and a new one would have to wait");
            check(fight.isParticipant(starter.getUUID()), "the starter is the first fighter");
            check(fight.stage() == ApocalypseFight.Stage.AWAKENING && fight.boss() == null, "he is not there yet");
            check(log.drain().said("message.enhanced.apocalypse.trembles"), "the ground trembles for those near");
            check(fight.bars().health().getPlayers().contains(starter), "the bars are shown to the starter");
            Vec3 centre = fight.arena().center();
            check(Math.abs(centre.x - helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5)).x) < 1.0E-6 && Math.abs(centre.y - helper.absoluteVec(new Vec3(0, FLOOR, 0)).y) < 1.0E-6,
                    "the arena is centred on the top of the block that was clicked: " + centre);
            helper.runAfterDelay(ApocalypseFight.EMERGE_AT + 6, () -> {
                ApocalypseEntity boss = fight.boss();
                check(fight.isParticipant(wanda.getUUID()), "the Scarlet Witch near joined the fight");
                check(boss != null && boss.isAlive(), "he steps out of the portal");
                check(fight.stage() == ApocalypseFight.Stage.AWAKENING && boss.state() == ApocalypseState.AWAKENING, "still waking");
                check(fight.bossInvulnerable() && !boss.hurt(level.damageSources().generic(), 20.0F), "and nothing can hurt him");
                check(boss.shield().state() == ApocalypseShield.State.FORMING, "his shield knits");
                check(fight.bars().health().isVisible() && fight.bars().second().isVisible(), "two bars: his health and his shield");
                helper.runAfterDelay(ApocalypseFight.AWAKENING_TICKS - ApocalypseFight.EMERGE_AT + 4, () -> {
                    check(fight.stage() == ApocalypseFight.Stage.FIGHT && fight.phase() == 1, "after 160 ticks the fight is on, in phase 1: " + fight.stage());
                    check(boss.shield().isUp(), "behind his shield");
                    check(!fight.bossInvulnerable() && !boss.hurt(level.damageSources().generic(), 20.0F), "which is the only thing that keeps him whole: " + boss.shield().state());
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_ritual_breaker", timeoutTicks = 100)
    public static void ritualRefusesWithoutABreaker(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        readyGround(f);
        f.config(ApocalypseBossConfig.REQUIRE_BREAKER, true);
        ServerLevel level = f.level();
        ServerPlayer lone = starter(f, 10.5, 5.5);
        Log log = new Log(lone);
        helper.runAfterDelay(3, () -> {
            plant(lone, absolute(f, CLICK));
            check(!BossFights.anyActive(level.getServer()), "a lone Survival player cannot start it");
            check(log.drain().toldOnActionBar("message.enhanced.apocalypse.refuse.breaker"), "the action bar says why");
            check(lone.getMainHandItem().getCount() == 1 && !lone.getCooldowns().isOnCooldown(ModItems.CAPSTONE.get()), "the Capstone is kept");

            // A Creative or a Spectator Wanda does not count.
            ServerPlayer creative = f.player(10.5, FLOOR, 15.5, GameType.CREATIVE);
            com.kolodkingal.enhanced.power.Powers.setPower(creative, Power.SCARLET_WITCH);
            ServerPlayer spectator = f.player(12.5, FLOOR, 15.5, GameType.SPECTATOR);
            com.kolodkingal.enhanced.power.Powers.setPower(spectator, Power.SCARLET_WITCH);
            plant(lone, absolute(f, CLICK));
            check(!BossFights.anyActive(level.getServer()), "a Creative Wanda and a Spectator Wanda do not count");

            // A Wanda too far away does not either.
            f.config(ApocalypseBossConfig.BREAKER_RANGE, 8);
            ServerPlayer far = f.player(10.5, FLOOR, 20.5, Power.SCARLET_WITCH);
            plant(lone, absolute(f, CLICK));
            check(!BossFights.anyActive(level.getServer()), "a Wanda ten blocks away is out of range 8");
            far.moveTo(helper.absoluteVec(new Vec3(10.5, FLOOR, 16.5)));
            plant(lone, absolute(f, CLICK));
            check(BossFights.active(level) instanceof ApocalypseFight, "one six blocks away starts it");
            BossFights.clear();

            // A Phoenix does.
            far.moveTo(helper.absoluteVec(new Vec3(10.5, FLOOR, 20.5)));
            com.kolodkingal.enhanced.power.Powers.setPower(far, Power.PHOENIX);
            far.moveTo(helper.absoluteVec(new Vec3(10.5, FLOOR, 16.5)));
            lone.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAPSTONE.get()));
            plant(lone, absolute(f, CLICK));
            check(BossFights.active(level) instanceof ApocalypseFight, "a Phoenix starts it too");
            BossFights.clear();

            // The server can say it needs none.
            far.moveTo(helper.absoluteVec(new Vec3(10.5, FLOOR, 20.5)));
            com.kolodkingal.enhanced.power.Powers.setPower(far, Power.NONE);
            f.config(ApocalypseBossConfig.REQUIRE_BREAKER, false);
            lone.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAPSTONE.get()));
            plant(lone, absolute(f, CLICK));
            check(BossFights.active(level) instanceof ApocalypseFight, "with requireBreaker off a lone player starts it");
            BossFights.clear();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "apocalypse_ritual_checks", timeoutTicks = 200)
    public static void ritualChecksEachConditionAndUsesNothingUpWhenItRefuses(GameTestHelper helper) {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        readyGround(f);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer wanda = f.player(10.5, FLOOR, 15.5, Power.SCARLET_WITCH);
        ApocalypseTestSupport.tank(wanda);
        ServerPlayer starter = starter(f, 10.5, 5.5);
        Log log = new Log(starter);
        BlockPos click = absolute(f, CLICK);
        // A roof over the block, for the sun check below (the light needs a few ticks to know).
        helper.setBlock(CLICK.above(2), Blocks.STONE);

        helper.runAfterDelay(6, () -> {
            // Nothing in the hand, and the wrong thing in it.
            starter.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            starter.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            refused(f, starter, log, click, "message.enhanced.apocalypse.capstone_use", "no Capstone in the hand", true, false);
            starter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAPSTONE.get()));

            // Switched off.
            f.config(ApocalypseBossConfig.ENABLED, false);
            refused(f, starter, log, click, "message.enhanced.apocalypse.refuse.disabled", "switched off", true);
            f.config(ApocalypseBossConfig.ENABLED, true);

            // The wrong dimension.
            List<String> dimensions = new ArrayList<>(ApocalypseBossConfig.RITUAL_DIMENSIONS.get());
            f.config(ApocalypseBossConfig.RITUAL_DIMENSIONS, List.of("minecraft:the_nether"));
            refused(f, starter, log, click, "message.enhanced.apocalypse.refuse.dimension", "the wrong dimension", true);
            f.config(ApocalypseBossConfig.RITUAL_DIMENSIONS, dimensions);

            // A roof over the block: no sun.
            refused(f, starter, log, click, "message.enhanced.apocalypse.refuse.sun", "a roof over the block", true);
            helper.setBlock(CLICK.above(2), Blocks.AIR);
            helper.runAfterDelay(6, () -> {
                // Not sand.
                helper.setBlock(CLICK, Blocks.STONE);
                refused(f, starter, log, click, "message.enhanced.apocalypse.refuse.sand", "a block of stone", true);
                helper.setBlock(CLICK, Blocks.SANDSTONE);

                // Water in the circle: no room.
                helper.setBlock(CLICK.offset(3, 1, 0), Blocks.WATER);
                refused(f, starter, log, click, "message.enhanced.apocalypse.refuse.room", "water in the circle", true);
                helper.setBlock(CLICK.offset(3, 1, 0), Blocks.AIR);

                // A protected block (a claim or spawn protection says no to a break).
                Consumer<BlockEvent.BreakEvent> claim = event -> event.setCanceled(true);
                NeoForge.EVENT_BUS.addListener(claim);
                f.onClose(() -> NeoForge.EVENT_BUS.unregister(claim));
                refused(f, starter, log, click, "message.enhanced.apocalypse.refuse.protected", "a claimed block", true);
                NeoForge.EVENT_BUS.unregister(claim);

                // The border cuts through the arena.
                WorldBorder border = level.getWorldBorder();
                double size = border.getSize();
                double centreX = border.getCenterX();
                double centreZ = border.getCenterZ();
                f.onClose(() -> {
                    border.setCenter(centreX, centreZ);
                    border.setSize(size);
                });
                border.setCenter(click.getX() + 0.5, click.getZ() + 0.5);
                border.setSize(10.0);
                refused(f, starter, log, click, "message.enhanced.apocalypse.refuse.border", "the world border", true);
                border.setCenter(centreX, centreZ);
                border.setSize(size);

                // Night.
                day(f, false);
                helper.runAfterDelay(2, () -> {
                    refused(f, starter, log, click, "message.enhanced.apocalypse.refuse.sun", "night", true);
                    day(f, true);
                    helper.runAfterDelay(2, () -> {
                        // Peaceful.
                        Difficulty difficulty = server.getWorldData().getDifficulty();
                        f.onClose(() -> server.setDifficulty(difficulty, true));
                        server.setDifficulty(Difficulty.PEACEFUL, true);
                        refused(f, starter, log, click, "message.enhanced.boss.peaceful", "Peaceful", true);
                        server.setDifficulty(difficulty, true);

                        // A fight already runs.
                        ServerPlayer other = starter(f, 14.5, 5.5);
                        Log otherLog = new Log(other);
                        plant(other, click);
                        check(BossFights.active(level) instanceof ApocalypseFight, "the first ritual works");
                        refused(f, starter, log, click, "message.enhanced.boss.busy", "a fight already running", false);
                        BossFights.stopAll(server, StopReason.COMMAND);
                        helper.succeed();
                    });
                });
            });
        });
    }

    /** Plants the Capstone and checks the ritual was refused with this message, with nothing used up and no fight started. */
    private static void refused(TestKit.Fixture f, ServerPlayer starter, Log log, BlockPos click, String key, String what, boolean noFight) {
        refused(f, starter, log, click, key, what, noFight, true);
    }

    /** {@code viaItem} false asks the boss facade directly, as a command without {@code force} would (nothing is planted). */
    private static void refused(TestKit.Fixture f, ServerPlayer starter, Log log, BlockPos click, String key, String what, boolean noFight, boolean viaItem) {
        MinecraftServer server = f.level().getServer();
        int before = starter.getMainHandItem().getCount();
        boolean ran = BossFights.anyActive(server);
        log.drain();
        log.clear();
        if (viaItem) {
            plant(starter, click);
        } else {
            BossFights.start(BossKind.APOCALYPSE, starter, false);
        }
        log.drain();
        check(log.toldOnActionBar(key), what + ": the action bar says " + key + " but " + log.actionBarKeys());
        check(starter.getMainHandItem().is(ModItems.CAPSTONE.get()) || before == 0, what + ": the Capstone is still in the hand");
        check(starter.getMainHandItem().getCount() == before, what + ": nothing was used up");
        check(!starter.getCooldowns().isOnCooldown(ModItems.CAPSTONE.get()), what + ": no cooldown");
        check(BossFights.anyActive(server) == ran, what + ": no fight was started");
        if (noFight) {
            check(!BossFights.anyActive(server), what + ": no fight runs");
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Nobody summons him

    @GameTest(template = ARENA, batch = "apocalypse_orphans", timeoutTicks = 100)
    public static void apocalypseCannotBeSummonedAndOrphansAreSwept(GameTestHelper helper) throws CommandSyntaxException {
        TestKit.Fixture f = ApocalypseTestSupport.fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer op = f.player(5.5, FLOOR, 5.5);
        CommandSourceStack source = op.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        check(!ModEntities.APOCALYPSE.get().canSummon(), "his type cannot be summoned");
        boolean failed = false;
        try {
            level.getServer().getCommands().getDispatcher().execute("summon enhanced:apocalypse 1 -50 1", source);
        } catch (CommandSyntaxException e) {
            failed = true;
        }
        check(failed, "/summon enhanced:apocalypse fails");
        check(level.getEntities(ModEntities.APOCALYPSE.get(), e -> true).isEmpty(), "and made nothing");

        // One that no fight owns is nothing: it never joins the level.
        ApocalypseEntity orphan = ModEntities.APOCALYPSE.get().create(level);
        orphan.moveTo(helper.absoluteVec(new Vec3(12.5, FLOOR, 12.5)));
        check(!level.addFreshEntity(orphan), "a boss that no fight owns is turned away at the door");

        // A marked Husk that no fight owns never joins the level.
        Husk husk = EntityType.HUSK.create(level);
        husk.moveTo(helper.absoluteVec(new Vec3(8.5, FLOOR, 12.5)));
        husk.getPersistentData().putString(Horsemen.MARK, "war");
        check(!level.addFreshEntity(husk), "a Horseman with no fight is turned away at the door");
        Husk plain = EntityType.HUSK.create(level);
        plain.moveTo(helper.absoluteVec(new Vec3(8.5, FLOOR, 8.5)));
        plain.setNoAi(true);
        check(level.addFreshEntity(plain), "an ordinary Husk is not");
        plain.discard();

        helper.runAfterDelay(4, () -> {
            check(level.getEntity(orphan.getUUID()) == null && level.getEntities(ModEntities.APOCALYPSE.get(), e -> true).isEmpty(), "the orphan is nowhere in the world");
            helper.succeed();
        });
    }

    private ApocalypseRitualTests() {
    }
}
