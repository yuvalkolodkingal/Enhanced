package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.gametest.UltronTestSupport.Log;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.ultron.UltronLane;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.UltronSounds;
import com.kolodkingal.enhanced.ultron.VibraniumKeyEntity;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.kolodkingal.enhanced.gametest.BossTestSupport.lastActionBarKey;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.MIDDLE;
import static com.kolodkingal.enhanced.gametest.UltronTestSupport.fixture;

/**
 * The ritual that wakes Ultron and the items it needs: the recipe of the Program, that no chest holds either item, the
 * Scepter's Sneak + use on the ground that starts a fight (and every refusal that starts none and uses nothing up), the
 * Mind Stone's answer to the Program and the Core, that the boss cannot be summoned, and the operator's commands.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class UltronRitualTests {
    /** Quick tests that keep no server-wide state share one batch. */
    private static final String QUICK = "ultron";

    // ------------------------------------------------------------------------------------------------------------
    // The items

    @GameTest(template = ARENA, batch = QUICK)
    public static void programRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("ultron_program"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:ultron_program exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack iron = new ItemStack(Items.IRON_BLOCK);
        ItemStack echo = new ItemStack(Items.ECHO_SHARD);
        ItemStack redstone = new ItemStack(Items.REDSTONE_BLOCK);
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        ItemStack none = ItemStack.EMPTY;
        check(recipe.matches(CraftingInput.of(3, 3, List.of(iron, echo, iron, redstone, star, redstone, iron, echo, iron)), level),
                "I E I / R S R / I E I");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(echo, iron, echo, redstone, star, redstone, iron, echo, iron)), level),
                "an Echo Shard where an Iron Block belongs does not");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(iron, echo, iron, star, redstone, star, iron, echo, iron)), level),
                "nor does the Star and the Redstone swapped");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(iron, echo, iron, redstone, star, redstone, iron, none, iron)), level),
                "nor does a missing Echo Shard");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.ULTRON_PROGRAM.get()) && recipe.getResultItem(level.registryAccess()).getCount() == 1,
                "it makes one Program");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void noLootForTheProgramOrTheCore(GameTestHelper helper) throws IOException {
        MinecraftServer server = helper.getLevel().getServer();
        int scanned = 0;
        for (String folder : List.of("loot_table", "loot_modifiers")) {
            Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> found =
                    server.getResourceManager().listResources(folder, id -> id.getNamespace().equals(Enhanced.MODID) && id.getPath().endsWith(".json"));
            for (var entry : found.entrySet()) {
                try (InputStream in = entry.getValue().open()) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    check(!text.contains("ultron_program") && !text.contains("ultron_core"), entry.getKey() + " does not hold the Program or the Core");
                    scanned++;
                }
            }
        }
        // The global list of loot modifiers is the other way in.
        var global = server.getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("neoforge", "loot_modifiers/global_loot_modifiers.json"));
        if (global.isPresent()) {
            try (InputStream in = global.get().open()) {
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                check(!text.contains("ultron_program") && !text.contains("ultron_core"), "the global loot modifiers do not name them");
            }
        }
        check(scanned > 0, "the mod's loot tables and modifiers were found and read");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The ritual

    @GameTest(template = ARENA, batch = "ultron_ritual", timeoutTicks = 400)
    public static void ritualStartsAFight(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        ServerPlayer watcher = f.player(3.5, FLOOR, 3.5);
        ItemStack scepter = new ItemStack(ModItems.LOKIS_SCEPTER.get());
        ItemStack program = new ItemStack(ModItems.ULTRON_PROGRAM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, scepter);
        player.setItemInHand(InteractionHand.OFF_HAND, program);
        player.setShiftKeyDown(true);
        TestKit.look(player, 0.0F, 90.0F); // down at the floor: the middle of the arena is the block under their feet
        BlockPos under = BlockPos.containing(helper.absoluteVec(MIDDLE)).below();
        Log log = new Log(watcher);

        InteractionResult result = scepter.getItem().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, scepter,
                new BlockHitResult(Vec3.atCenterOf(under), Direction.UP, under, false)));
        check(result.consumesAction(), "the Scepter's use on the ground is the ritual");
        check(program.isEmpty(), "the Program is used up");
        check(player.getCooldowns().isOnCooldown(scepter.getItem()), "and the Scepter rests");
        UltronFight fight = UltronTestSupport.fightOf(f);
        Vec3 centre = fight.arena().center();
        check(centre.distanceTo(helper.absoluteVec(new Vec3(MIDDLE.x, FLOOR, MIDDLE.z))) < 0.01,
                "the fight is centred on the top face of the clicked block, not on " + centre);
        check(fight.isParticipant(player.getUUID()), "the player who clicked is the first participant");

        f.everyTick(135, tick -> {
            log.drain();
            if (tick == 5) {
                check(log.heard(UltronSounds.AWAKEN.event()), "the awakening sounds at once");
                check(fight.boss() == null && fight.sentries().isEmpty(), "nothing stands there yet: the scrap is still assembling");
            }
            if (tick == 62) {
                UltronEntity boss = fight.boss();
                check(boss != null, "Ultron Prime stands among the scrap");
                check(boss.isShielded() && boss.isInvulnerableTo(level.damageSources().generic()), "and nothing can hurt him");
                check(fight.sentries().size() == 6, "with six Sentries, not " + fight.sentries().size());
                for (UltronSentryEntity sentry : fight.sentries()) {
                    check(sentry.isAssembling() && sentry.isInvulnerableTo(level.damageSources().generic()), "which are still assembling, invulnerable");
                }
                check(fight.bars().health().isVisible() && fight.bars().second().isVisible(), "both boss bars are up");
                check(fight.bars().health().getPlayers().contains(player) && fight.bars().health().getPlayers().contains(watcher),
                        "for the fighters and for whoever is near");
                check(log.said("speech.enhanced.ultron.awaken1"), "he says 'I'm here to help'");
            }
            if (tick == 105) {
                check(log.said("speech.enhanced.ultron.awaken2"), "and 'Peace in our time'");
                check(fight.phase() == UltronFight.Phase.AWAKENING && boss(fight).isShielded(), "he is still invulnerable");
            }
            if (tick == 128) {
                check(fight.phase() == UltronFight.Phase.GROUND, "after 120 ticks the fight is on, not " + fight.phase());
                check(!boss(fight).isShielded(), "and he can be hurt");
                check(fight.sentries().stream().noneMatch(UltronSentryEntity::isAssembling), "the Sentries are ready");
                return true;
            }
            return false;
        });
    }

    private static UltronEntity boss(UltronFight fight) {
        return UltronTestSupport.boss(fight);
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void ritualWithoutSneakOrWithoutTheProgramDoesNothingNew(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        ItemStack scepter = new ItemStack(ModItems.LOKIS_SCEPTER.get());
        ItemStack program = new ItemStack(ModItems.ULTRON_PROGRAM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, scepter);
        player.setItemInHand(InteractionHand.OFF_HAND, program);
        TestKit.look(player, 0.0F, 90.0F);
        BlockPos under = BlockPos.containing(helper.absoluteVec(MIDDLE)).below();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(under), Direction.UP, under, false);

        check(scepter.getItem().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, scepter, hit)) == InteractionResult.PASS,
                "without Sneak the use on a block passes, as it always did");
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        check(scepter.getItem().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, scepter, hit)) == InteractionResult.PASS,
                "and so does Sneak without the Program in the other hand");
        check(!BossFights.anyActive(level.getServer()) && program.getCount() == 1, "no fight, nothing used up");

        // The old use() runs unchanged: with nothing in the other hand the stone kills an unprotected player; a Creative one is told.
        ScepterRituals.useScepter(player, InteractionHand.MAIN_HAND, false);
        check(!player.isAlive(), "the stone still kills a player it does not know");
        ServerPlayer creative = f.player(4.5, FLOOR, 4.5, GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        ScepterRituals.useScepter(creative, InteractionHand.MAIN_HAND, false);
        check(creative.isAlive(), "a Creative player is only told");
        helper.succeed();
    }

    /** Each refusal answers with its own message, starts no fight and uses nothing up. */
    @GameTest(template = ARENA, batch = "ultron_refusals")
    public static void ritualRefusals(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        ItemStack scepter = new ItemStack(ModItems.LOKIS_SCEPTER.get());
        ItemStack program = new ItemStack(ModItems.ULTRON_PROGRAM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, scepter);
        player.setItemInHand(InteractionHand.OFF_HAND, program);
        player.setShiftKeyDown(true);
        TestKit.look(player, 0.0F, 90.0F);
        UltronLane lane = (UltronLane) BossFights.lane(BossKind.ULTRON);
        check(lane != null, "the Ultron lane is registered");
        check(refusalKey(lane.refusal(player, false)) == null, "the flat arena floor is accepted");

        // Switched off on the server.
        f.config(UltronBossConfig.ENABLED, false);
        expect(lane, player, "message.enhanced.ultron.refuse.disabled", "he is switched off");
        check(refusalKey(lane.refusal(player, true)).equals("message.enhanced.ultron.refuse.disabled"), "even an operator cannot wake him");
        f.config(UltronBossConfig.ENABLED, true);

        // No Program in the hand; nothing aimed at.
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        expect(lane, player, "message.enhanced.ultron.refuse.program", "there is no Program in the hand");
        check(refusalKey(lane.refusal(player, true)) == null, "an operator needs none");
        player.setItemInHand(InteractionHand.OFF_HAND, program);
        TestKit.look(player, 0.0F, 0.0F);
        expect(lane, player, "message.enhanced.ultron_program.aim", "no block is aimed at");
        TestKit.look(player, 0.0F, 90.0F);

        // A dimension with a ceiling (the validator's sky check is faked).
        Predicate<ServerLevel> real = UltronLane.skyCheck(l -> false);
        f.onClose(() -> UltronLane.skyCheck(real));
        expect(lane, player, "message.enhanced.ultron.refuse.dimension", "there is no open sky");
        check(refusalKey(lane.refusal(player, true)).equals("message.enhanced.ultron.refuse.dimension"), "not even by force");
        UltronLane.skyCheck(real);

        // The world border.
        WorldBorder border = level.getWorldBorder();
        double size = border.getSize();
        double cx = border.getCenterX();
        double cz = border.getCenterZ();
        Vec3 here = helper.absoluteVec(MIDDLE);
        f.onClose(() -> {
            border.setCenter(cx, cz);
            border.setSize(size);
        });
        border.setCenter(here.x, here.z);
        border.setSize(24.0);
        expect(lane, player, "message.enhanced.ultron.refuse.border", "the arena does not fit in the world border");
        border.setCenter(cx, cz);
        border.setSize(size);

        // A claim: a simulated break of a block of the island is cancelled.
        Consumer<BlockEvent.BreakEvent> claim = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.BreakEvent.class, claim);
        try {
            expect(lane, player, "message.enhanced.ultron.refuse.protected", "the ground is protected");
        } finally {
            NeoForge.EVENT_BUS.unregister(claim);
        }

        // The ground is too uneven: a pillar higher than maxRelief in the disc.
        BlockPos pillar = helper.absolutePos(new BlockPos(14, 2, 10));
        for (int dy = 0; dy < UltronBossConfig.MAX_RELIEF.get() + 2; dy++) {
            level.setBlock(pillar.above(dy), Blocks.STONE.defaultBlockState(), 3);
        }
        f.onClose(() -> {
            for (int dy = 0; dy < 12; dy++) {
                level.setBlock(pillar.above(dy), Blocks.AIR.defaultBlockState(), 3);
            }
        });
        expect(lane, player, "message.enhanced.ultron.refuse.relief", "the ground is not level");
        for (int dy = 0; dy < 12; dy++) {
            level.setBlock(pillar.above(dy), Blocks.AIR.defaultBlockState(), 3);
        }

        // A lake: more than 40 percent of the disc is water.
        for (int x = 1; x <= 20; x++) {
            for (int z = 1; z <= 12; z++) {
                level.setBlock(helper.absolutePos(new BlockPos(x, 2, z)), Blocks.WATER.defaultBlockState(), 3);
            }
        }
        f.onClose(() -> {
            for (int x = 1; x <= 20; x++) {
                for (int z = 1; z <= 12; z++) {
                    level.setBlock(helper.absolutePos(new BlockPos(x, 2, z)), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        });
        expect(lane, player, "message.enhanced.ultron.refuse.fluid", "water is in the way");
        for (int x = 1; x <= 20; x++) {
            for (int z = 1; z <= 12; z++) {
                level.setBlock(helper.absolutePos(new BlockPos(x, 2, z)), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        check(refusalKey(lane.refusal(player, false)) == null, "the floor is accepted again");

        // Peaceful: the shared rule, before the lane is asked.
        Difficulty difficulty = server.getWorldData().getDifficulty();
        server.setDifficulty(Difficulty.PEACEFUL, true);
        boolean peaceful = BossFights.start(BossKind.ULTRON, player, false);
        server.setDifficulty(difficulty, true);
        check(!peaceful && "message.enhanced.boss.peaceful".equals(lastActionBarKey(player)), "Ultron cannot wake in Peaceful");

        // Everything above used up nothing and started nothing.
        check(program.getCount() == 1 && !BossFights.anyActive(server), "nothing was used up and no fight began");

        // A second fight is refused (the first one is the operator's).
        ServerPlayer other = f.player(2.5, FLOOR, 2.5);
        check(BossFights.start(BossKind.ULTRON, other, true), "an operator wakes him");
        check(!BossFights.start(BossKind.ULTRON, player, false) && "message.enhanced.boss.busy".equals(lastActionBarKey(player)),
                "and a second ritual is refused: he is already awake");
        check(program.getCount() == 1, "the Program is still there");
        helper.succeed();
    }

    private static void expect(UltronLane lane, ServerPlayer player, String key, String why) {
        String actual = refusalKey(lane.refusal(player, false));
        check(key.equals(actual), why + ": expected " + key + " but the lane said " + actual);
    }

    private static String refusalKey(Component refusal) {
        return refusal != null && refusal.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : null;
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void relief(GameTestHelper helper) {
        // The Tremor's rule of the ground: a rise of maxRelief is fine, one more is not.
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        ItemStack program = new ItemStack(ModItems.ULTRON_PROGRAM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, program);
        TestKit.look(player, 0.0F, 90.0F);
        UltronLane lane = (UltronLane) BossFights.lane(BossKind.ULTRON);
        BlockPos pillar = helper.absolutePos(new BlockPos(14, 2, 10));
        int allowed = UltronBossConfig.MAX_RELIEF.get();
        f.onClose(() -> {
            for (int dy = 0; dy < 14; dy++) {
                level.setBlock(pillar.above(dy), Blocks.AIR.defaultBlockState(), 3);
            }
        });
        for (int dy = 0; dy < allowed; dy++) {
            level.setBlock(pillar.above(dy), Blocks.STONE.defaultBlockState(), 3);
        }
        check(refusalKey(lane.refusal(player, false)) == null, "a rise of " + allowed + " blocks is accepted");
        level.setBlock(pillar.above(allowed), Blocks.STONE.defaultBlockState(), 3);
        check("message.enhanced.ultron.refuse.relief".equals(refusalKey(lane.refusal(player, false))), "a rise of " + (allowed + 1) + " is not");
        helper.succeed();
    }

    /**
     * A tree is something the island carries, not a hill: oaks standing in the island's disc (a trunk of five logs and
     * a canopy eight blocks over the ground, taller than {@code maxRelief}) do not make the ground uneven, while a
     * pillar of stone as high as they are does.
     */
    @GameTest(template = ARENA, batch = QUICK)
    public static void aGroveIsLevelGround(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ULTRON_PROGRAM.get()));
        TestKit.look(player, 0.0F, 90.0F);
        UltronLane lane = (UltronLane) BossFights.lane(BossKind.ULTRON);
        List<BlockPos> placed = new ArrayList<>();
        f.onClose(() -> placed.forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)));
        for (int[] at : new int[][]{{14, 10}, {6, 12}, {10, 5}}) {
            BlockPos foot = helper.absolutePos(new BlockPos(at[0], 2, at[1]));
            for (int dy = 0; dy < 5; dy++) {
                placed.add(foot.above(dy));
                level.setBlock(foot.above(dy), Blocks.OAK_LOG.defaultBlockState(), 3);
            }
            for (int dy = 3; dy < 7; dy++) {
                int reach = dy < 5 ? 2 : 1;
                for (int dx = -reach; dx <= reach; dx++) {
                    for (int dz = -reach; dz <= reach; dz++) {
                        BlockPos leaf = foot.offset(dx, dy, dz);
                        if (level.getBlockState(leaf).isAir()) {
                            placed.add(leaf);
                            level.setBlock(leaf, Blocks.OAK_LEAVES.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
        check(refusalKey(lane.refusal(player, false)) == null, "three oaks in the disc are level ground");
        BlockPos pillar = helper.absolutePos(new BlockPos(14, 2, 14));
        int height = UltronBossConfig.MAX_RELIEF.get() + 2;
        for (int dy = 0; dy < height; dy++) {
            placed.add(pillar.above(dy));
            level.setBlock(pillar.above(dy), Blocks.STONE.defaultBlockState(), 3);
        }
        check("message.enhanced.ultron.refuse.relief".equals(refusalKey(lane.refusal(player, false))), "a stone pillar as high as the trees is not");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Mind Stone

    @GameTest(template = ARENA, batch = QUICK)
    public static void programAndCoreProtectFromTheStone(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        ServerPlayer watcher = f.player(15.5, FLOOR, 15.5);
        Log log = new Log(watcher);

        // The Program in the other hand turns the stone away: no explosion, a hint.
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        ItemStack program = new ItemStack(ModItems.ULTRON_PROGRAM.get());
        player.setItemInHand(InteractionHand.OFF_HAND, program);
        float health = player.getHealth();
        ScepterRituals.useScepter(player, InteractionHand.MAIN_HAND, false);
        check(player.isAlive() && player.getHealth() == health && program.getCount() == 1, "the Program protects from the stone and is kept");
        check("message.enhanced.ultron_program.aim".equals(lastActionBarKey(player)), "and the stone says to aim at the ground");

        // The Core and no power: Ultron awakens, with lightning and a message to the server.
        ItemStack core = new ItemStack(ModItems.ULTRON_CORE.get());
        player.setItemInHand(InteractionHand.OFF_HAND, core);
        log.drain();
        ScepterRituals.useScepter(player, InteractionHand.MAIN_HAND, false);
        check(Powers.is(player, Power.ULTRON), "a player without a power becomes Ultron");
        check(core.isEmpty(), "the Core is used up");
        List<LightningBolt> bolts = level.getEntitiesOfClass(LightningBolt.class, new AABB(player.blockPosition()).inflate(6.0));
        check(!bolts.isEmpty(), "a bolt of lightning marks the moment");
        check(log.drain().said("power.enhanced.ultron.broadcast"), "and the whole server is told");

        // As Ultron: the stone knows him, and the Core is not used up (he would not need it).
        ItemStack another = new ItemStack(ModItems.ULTRON_CORE.get());
        player.setItemInHand(InteractionHand.OFF_HAND, another);
        ScepterRituals.useScepter(player, InteractionHand.MAIN_HAND, false);
        check(player.isAlive() && another.getCount() == 1 && Powers.is(player, Power.ULTRON), "Ultron is recognised by the stone: no explosion, nothing used");

        // Another hero is told he already holds a power.
        ServerPlayer thor = f.player(9.5, FLOOR, 5.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        ItemStack kept = new ItemStack(ModItems.ULTRON_CORE.get());
        thor.setItemInHand(InteractionHand.OFF_HAND, kept);
        ScepterRituals.useScepter(thor, InteractionHand.MAIN_HAND, false);
        check(Powers.is(thor, Power.THOR) && kept.getCount() == 1 && thor.isAlive(), "a hero with a power keeps the Core");

        // Creative: the Core is not used up.
        ServerPlayer creative = f.player(12.5, FLOOR, 5.5, GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        ItemStack creativeCore = new ItemStack(ModItems.ULTRON_CORE.get());
        creative.setItemInHand(InteractionHand.OFF_HAND, creativeCore);
        ScepterRituals.useScepter(creative, InteractionHand.MAIN_HAND, false);
        check(creativeCore.getCount() == 1, "in Creative the Core is not used up");

        // Used alone it does nothing but say what it needs.
        ServerPlayer alone = f.player(7.5, FLOOR, 14.5);
        alone.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ULTRON_CORE.get()));
        ModItems.ULTRON_CORE.get().use(level, alone, InteractionHand.MAIN_HAND);
        check("message.enhanced.ultron_core.needs_stone".equals(lastActionBarKey(alone)) && Powers.power(alone) == Power.NONE,
                "the Core alone says it needs the Mind Stone");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The boss cannot be summoned

    @GameTest(template = ARENA, batch = "ultron_orphans")
    public static void theBossCannotBeSummoned(GameTestHelper helper) {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        for (EntityType<?> type : List.of(ModEntities.ULTRON.get(), ModEntities.ULTRON_SENTRY.get(), ModEntities.VIBRANIUM_KEY.get(), ModEntities.ULTRON_BOLT.get())) {
            check(!type.canSummon(), type + " cannot be summoned");
        }
        Vec3 at = helper.absoluteVec(new Vec3(5.5, FLOOR + 1.0, 5.5));
        // No fight: none of them joins a level.
        UltronEntity stray = new UltronEntity(ModEntities.ULTRON.get(), level);
        stray.setPos(at);
        check(!level.addFreshEntity(stray) && level.getEntity(stray.getUUID()) == null, "a stray body is discarded the moment it joins");
        UltronSentryEntity sentry = new UltronSentryEntity(ModEntities.ULTRON_SENTRY.get(), level);
        sentry.setPos(at);
        check(!level.addFreshEntity(sentry), "so is a stray Sentry");
        VibraniumKeyEntity key = new VibraniumKeyEntity(ModEntities.VIBRANIUM_KEY.get(), level);
        key.setPos(at);
        check(!level.addFreshEntity(key), "and a stray key");
        UltronBoltEntity bolt = UltronBoltEntity.create(level, null, at, new Vec3(0.1, 0, 0), 4.0F, UltronBoltEntity.Palette.PRIME);
        check(!level.addFreshEntity(bolt), "and a bolt that nobody fired");

        // The hero's own are not strays: a drone with an owner, a bolt a player shot.
        ServerPlayer hero = f.player(10.5, FLOOR, 10.5, Power.ULTRON);
        UltronSentryEntity drone = new UltronSentryEntity(ModEntities.ULTRON_SENTRY.get(), level);
        drone.setOwner(hero.getUUID());
        drone.setPos(at);
        check(level.addFreshEntity(drone) && drone.isAlive(), "a drone with an owner is left alone");
        UltronBoltEntity shot = UltronBoltEntity.create(level, hero, at.add(1, 0, 0), new Vec3(0.1, 0, 0), 4.0F, UltronBoltEntity.Palette.PRIME);
        check(level.addFreshEntity(shot), "and so is a bolt a player shot");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The operator

    @GameTest(template = ARENA, batch = "ultron_commands")
    public static void operatorCommands(GameTestHelper helper) throws CommandSyntaxException {
        TestKit.Fixture f = fixture(helper);
        ServerLevel level = f.level();
        ServerPlayer player = f.player(MIDDLE.x, FLOOR, MIDDLE.z);
        check(UltronTestSupport.run(player, "enhanced boss start ultron force") == 1, "/enhanced boss start ultron force wakes him");
        UltronFight fight = UltronTestSupport.fightOf(f);
        check(fight.arena().center().distanceTo(player.position()) < 0.01, "at the operator's feet");
        check(player.getInventory().isEmpty(), "with no item used");
        check(UltronTestSupport.run(player, "enhanced boss stop") == 1, "stop ends it");
        check(fight.result() == BossEnd.COMMAND, "like an operator's stop");
        check(player.getInventory().isEmpty(), "and refunds nothing (nothing was taken)");

        BossFights.clear();
        ServerPlayer hero = f.player(4.5, FLOOR, 4.5);
        String name = hero.getGameProfile().getName();
        for (String look : List.of("prime", "sentry", "human", "own")) {
            UltronTestSupport.run(player, "enhanced power set " + name + " ultron " + look);
            check(Powers.is(hero, Power.ULTRON) && Powers.variant(hero).equals(Variants.get(Power.ULTRON, look)), "power set ultron " + look);
        }
        helper.succeed();
    }
}
