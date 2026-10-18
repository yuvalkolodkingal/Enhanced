package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.PhoenixBurst;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseFight;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGifts;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.BossConfig;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.gametest.UltronTestSupport.Log;
import com.kolodkingal.enhanced.item.AwakeningItem;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.item.ScepterRituals.Ritual;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosFight;
import com.kolodkingal.enhanced.thanos.ThanosLane;
import com.kolodkingal.enhanced.ultron.LiftHook;
import com.kolodkingal.enhanced.ultron.LiftHooks;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import com.kolodkingal.enhanced.ultron.SokoviaLift;
import com.kolodkingal.enhanced.ultron.SokoviaRecovery;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronFight;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.config.UltronConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.CrossHeroBossTests.press;
import static com.kolodkingal.enhanced.gametest.CrossHeroBossTests.sneakPress;
import static com.kolodkingal.enhanced.gametest.CrossHeroBossTests.tank;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The 3.0 heroes and bosses meet for the first time (integration.md 5.10), the rows of the cross-hero matrix that need no
 * duel: what Loki's Scepter and the Tesseract do with each lane's items, that the bosses' powers come only from beating them
 * (and Thanos's stones stop at five), the shared rule of the area attacks against a village, one boss fight at a time, the
 * rewards that wait for whoever was away, a restart in the middle of a fight, and the heroes the Sokovia lift carries. The
 * duels of the heroes with the bosses are in {@link CrossHeroBossTests}.
 *
 * <p>Every test that starts a fight or changes a server-wide value has a batch of its own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CrossHeroTests {

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    /** Puts {@code main} and {@code off} in the hands and right-clicks with the main hand, as the game does. */
    private static InteractionResultHolder<ItemStack> use(ServerPlayer player, Item main, Item off) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(main));
        player.setItemInHand(InteractionHand.OFF_HAND, off == null ? ItemStack.EMPTY : new ItemStack(off));
        return player.getMainHandItem().getItem().use(player.serverLevel(), player, InteractionHand.MAIN_HAND);
    }

    /** How many of the item the player carries (both hands and the inventory). */
    private static int count(ServerPlayer player, Item item) {
        return player.getInventory().countItem(item);
    }

    /** A Sneak + use of the item in the main hand on the block under the player's feet (the Scepter's and the Capstone's rituals). */
    private static InteractionResult useOnTheGround(ServerPlayer player, BlockPos block) {
        ItemStack stack = player.getMainHandItem();
        player.setShiftKeyDown(true);
        TestKit.look(player, 0.0F, 90.0F);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(block).add(0.0, 0.5, 0.0), Direction.UP, block, false);
        InteractionResult result = stack.getItem().useOn(new UseOnContext(player.serverLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        player.setShiftKeyDown(false);
        return result;
    }

    /** The block under the player's feet. */
    private static BlockPos under(ServerPlayer player) {
        return player.blockPosition().below();
    }

    /**
     * A fixture for a test that runs the three bosses one after the other in the test arena: the Ultron island small and the
     * lift the Tremor (as the Ultron tests have it), the Apocalypse arena of radius 9, the Thanos arena of 16 with the check
     * of his site off and a short countdown, Normal difficulty, the announcements off.
     */
    private static TestKit.Fixture threeBosses(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        Difficulty difficulty = server.getWorldData().getDifficulty();
        server.setDifficulty(Difficulty.NORMAL, true);
        f.onClose(() -> server.setDifficulty(difficulty, true));
        LiftHook lift = LiftHooks.install(null);
        f.onClose(() -> LiftHooks.install(lift));
        boolean site = ThanosLane.checkSite;
        ThanosLane.checkSite = false;
        f.onClose(() -> ThanosLane.checkSite = site);
        f.config(BossConfig.ANNOUNCE_FIGHTS, false);
        f.config(UltronBossConfig.ISLAND_RADIUS, 9);
        f.config(UltronBossConfig.LIFT_HEIGHT, 16);
        f.config(UltronBossConfig.CONE_DEPTH, 6);
        f.config(UltronBossConfig.SKIN_DEPTH, 4);
        f.config(UltronBossConfig.LIFT_TICKS, 40);
        f.config(ApocalypseBossConfig.ARENA_RADIUS, 9);
        f.config(ThanosBossConfig.ARENA_RADIUS, 16);
        f.config(ThanosBossConfig.SUMMON_COUNTDOWN_TICKS, 20);
        f.config(ThanosBossConfig.SUMMON_CHARGE_TICKS, 20);
        return f;
    }

    /** Ends every fight at once, as a second operator's stop does (the first lets them wind down). */
    private static void stopEveryFight(MinecraftServer server) {
        BossFights.stopAll(server, StopReason.COMMAND);
        BossFights.stopAll(server, StopReason.COMMAND);
        check(!BossFights.anyActive(server), "every fight is over and let go");
    }

    // ============================================================================================================
    // 1. ScepterRituals: the table, row by row, with the real lanes behind it

    /**
     * Loki's Scepter, used with each thing the Mind Stone answers to, by the heroes of every lane (integration.md 3.2): the gene
     * makes the Scarlet Witch, the Synthezoid Core Vision and Ultron's Core Ultron (each used up), a hero is refused and keeps
     * the item, the Ultron Program in the air only says to aim at the ground, and the stone knows Wanda, Vision and Ultron.
     * Nobody dies, nothing else changes and no fight starts.
     */
    @GameTest(template = ARENA, batch = "cross_scepter")
    public static void theScepterAnswersEachItemAndEachHeroAsTheTableSays(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        Item scepter = ModItems.LOKIS_SCEPTER.get();
        record Row(String what, Power power, Item other, Power becomes, int otherLeft, String said) {
        }
        List<Row> rows = List.of(
                new Row("a powerless player with the Scarlet Witch Gene", Power.NONE, ModItems.SCARLET_WITCH_GENE.get(), Power.SCARLET_WITCH, 0, null),
                new Row("a powerless player with a Synthezoid Core", Power.NONE, ModItems.SYNTHEZOID_CORE.get(), Power.VISION, 0, null),
                new Row("a powerless player with Ultron's Core", Power.NONE, ModItems.ULTRON_CORE.get(), Power.ULTRON, 0, null),
                new Row("a Phoenix with Ultron's Core", Power.PHOENIX, ModItems.ULTRON_CORE.get(), Power.PHOENIX, 1, "message.enhanced.mind_stone.already_powered"),
                new Row("a Spider-Man with a Synthezoid Core", Power.SPIDER_MAN, ModItems.SYNTHEZOID_CORE.get(), Power.SPIDER_MAN, 1,
                        "message.enhanced.mind_stone.already_powered"),
                new Row("a Thanos with the Scarlet Witch Gene", Power.THANOS, ModItems.SCARLET_WITCH_GENE.get(), Power.THANOS, 1,
                        "message.enhanced.mind_stone.already_powered"),
                new Row("an Apocalypse with Ultron's Core", Power.APOCALYPSE, ModItems.ULTRON_CORE.get(), Power.APOCALYPSE, 1,
                        "message.enhanced.mind_stone.already_powered"),
                new Row("a powerless player with the Ultron Program, in the air", Power.NONE, ModItems.ULTRON_PROGRAM.get(), Power.NONE, 1,
                        "message.enhanced.ultron_program.aim"),
                new Row("a Phoenix with the Ultron Program, in the air", Power.PHOENIX, ModItems.ULTRON_PROGRAM.get(), Power.PHOENIX, 1,
                        "message.enhanced.ultron_program.aim"),
                new Row("an Ultron with nothing", Power.ULTRON, null, Power.ULTRON, 0, "message.enhanced.mind_stone.recognizes"),
                new Row("an Ultron with the Gene", Power.ULTRON, ModItems.SCARLET_WITCH_GENE.get(), Power.ULTRON, 1, "message.enhanced.mind_stone.recognizes"),
                new Row("a Scarlet Witch with Ultron's Core", Power.SCARLET_WITCH, ModItems.ULTRON_CORE.get(), Power.SCARLET_WITCH, 1,
                        "message.enhanced.mind_stone.already_powered"),
                new Row("a Vision with the Gene", Power.VISION, ModItems.SCARLET_WITCH_GENE.get(), Power.VISION, 1, "message.enhanced.mind_stone.recognizes"));
        int i = 0;
        for (Row row : rows) {
            double x = 2.5 + (i % 5) * 4.0;
            double z = 3.5 + (i / 5) * 6.0;
            i++;
            ServerPlayer player = f.player(x, FLOOR, z, row.power());
            Vec3 at = player.position();
            float health = player.getHealth();
            BossTestSupport.lastActionBarKey(player);
            use(player, scepter, row.other());
            String said = BossTestSupport.lastActionBarKey(player);
            check(player.isAlive() && player.getHealth() >= health, row.what() + ": the Mind Stone hurt nobody (" + health + " -> " + player.getHealth() + ")");
            check(Powers.power(player) == row.becomes(), row.what() + ": becomes " + row.becomes() + ", not " + Powers.power(player));
            if (row.other() != null) {
                check(count(player, row.other()) == row.otherLeft(), row.what() + ": " + row.otherLeft() + " of the item left, not " + count(player, row.other()));
            }
            check(count(player, scepter) == 1, row.what() + ": the Scepter is never used up");
            check(player.position().equals(at), row.what() + ": nobody is moved");
            if (row.said() != null) {
                check(row.said().equals(said), row.what() + ": is told " + row.said() + ", not " + said);
            }
        }
        check(!BossFights.anyActive(server), "no fight was started by any of it");
        helper.succeed();
    }

    /**
     * The 3.0 items that are no ritual of the Mind Stone (the Ember, the Radioactive Spider, the Gauntlet, the Essence, the
     * Capstone): the Scepter does with them what it does with anything else (integration.md 3.2, "anything else: explode and
     * die"). A Creative player is only told, keeps the item and gains no power; a survival player with the Gauntlet dies of it
     * and does not become Thanos.
     */
    @GameTest(template = ARENA, batch = "cross_scepter_explodes")
    public static void theScepterTearsApartWhoeverHoldsANewItemThatIsNoRitual(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        Item scepter = ModItems.LOKIS_SCEPTER.get();
        List<Item> items = List.of(ModItems.PHOENIX_EMBER.get(), ModItems.RADIOACTIVE_SPIDER.get(), ModItems.INFINITY_GAUNTLET.get(),
                ModItems.ESSENCE.get(), ModItems.CAPSTONE.get());
        for (Item item : items) {
            check(ScepterRituals.scepter(Power.NONE, new ItemStack(item), false, false) == Ritual.EXPLODES,
                    BuiltInRegistries.ITEM.getKey(item) + " is no ritual of the Scepter");
        }
        int i = 0;
        for (Item item : items) {
            ServerPlayer creative = f.player(3.5 + i * 3.5, FLOOR, 3.5, GameType.CREATIVE);
            i++;
            BossTestSupport.lastActionBarKey(creative);
            use(creative, scepter, item);
            check("message.enhanced.mind_stone.creative".equals(BossTestSupport.lastActionBarKey(creative)),
                    "a Creative player with " + BuiltInRegistries.ITEM.getKey(item) + " is only told the stone would tear them apart");
            check(Powers.power(creative) == Power.NONE, "and gains no power from " + BuiltInRegistries.ITEM.getKey(item));
            check(count(creative, item) == 1, "and keeps " + BuiltInRegistries.ITEM.getKey(item));
        }
        ServerPlayer survivor = f.player(10.5, FLOOR, 16.5);
        use(survivor, scepter, ModItems.INFINITY_GAUNTLET.get());
        check(!survivor.isAlive() && survivor.getLastDamageSource() != null && survivor.getLastDamageSource().is(ModDamageTypes.MIND_STONE),
                "a survival player with the Gauntlet and the Scepter is torn apart by the Mind Stone: " + survivor.getLastDamageSource());
        check(Powers.power(survivor) == Power.NONE, "and did not become Thanos");
        helper.succeed();
    }

    /**
     * The Tesseract and the Scepter together call the Mad Titan, whoever holds them and whichever is used (the real Thanos lane
     * answers: the hold begins, nothing explodes, nobody is taken anywhere, nothing is used up). The Tesseract's other rows
     * stand: with the gene a powerless player becomes the Scarlet Witch, a hero jumps; and with a reward item in the other
     * hand it is Loki's escape for a powerless player, never the Mad Titan.
     */
    @GameTest(template = ARENA, batch = "cross_tesseract", timeoutTicks = 60)
    public static void theTesseractAndTheScepterCallTheRealMadTitanForAnyHero(GameTestHelper helper) {
        TestKit.Fixture f = ThanosSummonTests.fixture(helper);
        Item scepter = ModItems.LOKIS_SCEPTER.get();
        Item tesseract = ModItems.TESSERACT.get();
        record Call(Power power, Item main, Item off) {
        }
        List<Call> calls = List.of(new Call(Power.NONE, tesseract, scepter), new Call(Power.NONE, scepter, tesseract),
                new Call(Power.PHOENIX, scepter, tesseract), new Call(Power.SPIDER_MAN, tesseract, scepter),
                new Call(Power.ULTRON, scepter, tesseract), new Call(Power.APOCALYPSE, tesseract, scepter),
                new Call(Power.SCARLET_WITCH, scepter, tesseract), new Call(Power.THANOS, tesseract, scepter));
        int i = 0;
        for (Call call : calls) {
            ServerPlayer player = f.player(2.5 + (i % 4) * 5.0, FLOOR, 3.5 + (i / 4) * 5.0, call.power());
            i++;
            Vec3 at = player.position();
            float health = player.getHealth();
            String who = call.power() + " with the " + BuiltInRegistries.ITEM.getKey(call.main()).getPath() + " used";
            InteractionResultHolder<ItemStack> result = use(player, call.main(), call.off());
            check(result.getResult() == InteractionResult.CONSUME && player.isUsingItem(), who + ": the summoning's hold begins (" + result.getResult() + ")");
            check(player.isAlive() && player.getHealth() >= health, who + ": the Scepter did not explode (" + health + " -> " + player.getHealth() + ")");
            check(player.position().equals(at), who + ": the Tesseract did not take them anywhere");
            check(Powers.power(player) == call.power(), who + ": the power is unchanged");
            check(count(player, scepter) == 1 && count(player, tesseract) == 1, who + ": nothing is used up");
            player.releaseUsingItem();
            check(!player.isUsingItem(), who + ": and let go");
        }
        check(BossFights.active(f.level()) == null, "no fight was called: every hold was let go");

        ServerPlayer gene = f.player(4.5, FLOOR, 16.5);
        use(gene, tesseract, ModItems.SCARLET_WITCH_GENE.get());
        check(Powers.power(gene) == Power.SCARLET_WITCH && count(gene, ModItems.SCARLET_WITCH_GENE.get()) == 0,
                "the Tesseract with the gene makes the Scarlet Witch");
        ServerPlayer hero = f.player(10.5, FLOOR, 16.5, Power.SPIDER_MAN);
        Vec3 from = hero.position();
        TestKit.look(hero, 0.0F, 45.0F);
        use(hero, tesseract, null);
        check(Powers.power(hero) == Power.SPIDER_MAN && hero.position().distanceTo(from) > 0.5, "a hero's Tesseract jumps (moved " + hero.position().distanceTo(from) + ")");
        for (Item reward : List.of(ModItems.INFINITY_GAUNTLET.get(), ModItems.ULTRON_CORE.get(), ModItems.ESSENCE.get(), ModItems.ULTRON_PROGRAM.get(),
                ModItems.CAPSTONE.get(), ModItems.PHOENIX_EMBER.get())) {
            check(ScepterRituals.tesseract(Power.NONE, new ItemStack(reward)) == Ritual.ESCAPE,
                    "the Tesseract with " + BuiltInRegistries.ITEM.getKey(reward) + " is Loki's escape, nothing of the villains");
        }
        // And the item does what the table says: the Gauntlet in the other hand calls no Titan, it is Loki's escape.
        ServerPlayer escaper = f.player(16.5, FLOOR, 16.5);
        use(escaper, tesseract, ModItems.INFINITY_GAUNTLET.get());
        check(!escaper.isUsingItem(), "the Tesseract with the Gauntlet begins no summoning");
        check(Powers.power(escaper) == Power.LOKI, "it is Loki's escape: a Loki now, not " + Powers.power(escaper));
        check(count(escaper, ModItems.INFINITY_GAUNTLET.get()) == 1 && count(escaper, tesseract) == 1, "and the Gauntlet and the Tesseract are kept");
        check(BossFights.active(f.level()) == null, "and no fight was called");
        helper.succeed();
    }

    /**
     * The Scepter with the Ultron Program, Sneak + use on the ground, wakes the real Ultron, for a hero too (integration.md
     * 3.2), and uses the Program up; a second ritual while he fights is refused and uses nothing.
     */
    @GameTest(template = ARENA, batch = "cross_scepter_ultron", timeoutTicks = 40)
    public static void theScepterAndTheProgramWakeTheRealUltronAndOnlyOnce(GameTestHelper helper) {
        TestKit.Fixture f = UltronTestSupport.fixture(helper);
        ServerPlayer phoenix = tank(f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z, Power.PHOENIX));
        phoenix.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        phoenix.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.ULTRON_PROGRAM.get()));
        check(useOnTheGround(phoenix, under(phoenix)).consumesAction(), "Sneak + use on the ground is the ritual");
        check(BossFights.active(f.level()) instanceof UltronFight, "a Phoenix woke Ultron");
        check(count(phoenix, ModItems.ULTRON_PROGRAM.get()) == 0, "and the Program is used up");
        check(Powers.power(phoenix) == Power.PHOENIX && phoenix.isAlive(), "she is still the Phoenix, unhurt");

        ServerPlayer spider = tank(f.player(4.5, FLOOR, 4.5, Power.SPIDER_MAN));
        spider.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        spider.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.ULTRON_PROGRAM.get()));
        BossTestSupport.lastActionBarKey(spider);
        useOnTheGround(spider, under(spider));
        check("message.enhanced.boss.busy".equals(BossTestSupport.lastActionBarKey(spider)), "a second ritual is refused: a fight runs");
        check(count(spider, ModItems.ULTRON_PROGRAM.get()) == 1, "and its Program is kept");
        check(BossFights.all(f.level().getServer()).size() == 1, "one fight, not two");
        helper.succeed();
    }

    // ============================================================================================================
    // 15. Each boss's power is won only by beating it, and Thanos has five stones, never six

    /**
     * The three rewards (the Infinity Gauntlet, Ultron's Core, the Essence of En Sabah Nur) are made nowhere but by their
     * bosses: no recipe makes one, no loot table or loot modifier holds one, the bosses cannot be summoned, and of every item
     * of the game only the Gauntlet awakens Thanos and only the Essence Apocalypse (Ultron's Core is a Scepter ritual).
     */
    @GameTest(template = ARENA, batch = "cross_rewards_data")
    public static void theThreeBossPowersAreMadeByNothingButTheirBosses(GameTestHelper helper) throws IOException {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        List<Item> rewards = List.of(ModItems.INFINITY_GAUNTLET.get(), ModItems.ULTRON_CORE.get(), ModItems.ESSENCE.get());
        for (RecipeHolder<?> recipe : level.getRecipeManager().getRecipes()) {
            ItemStack result = recipe.value().getResultItem(level.registryAccess());
            for (Item reward : rewards) {
                check(!result.is(reward), "the recipe " + recipe.id() + " does not make " + BuiltInRegistries.ITEM.getKey(reward));
            }
        }
        List<String> ids = rewards.stream().map(item -> BuiltInRegistries.ITEM.getKey(item).toString()).toList();
        int scanned = 0;
        for (String folder : List.of("loot_table", "loot_modifiers")) {
            var found = server.getResourceManager().listResources(folder, id -> id.getPath().endsWith(".json"));
            for (var entry : found.entrySet()) {
                try (InputStream in = entry.getValue().open()) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    for (String id : ids) {
                        check(!text.contains(id), entry.getKey() + " does not hold " + id);
                    }
                    scanned++;
                }
            }
        }
        check(scanned > 100, "every loot table and loot modifier was read: " + scanned);
        for (EntityType<?> boss : List.of(ModEntities.THANOS.get(), ModEntities.ULTRON.get(), ModEntities.APOCALYPSE.get())) {
            check(!boss.canSummon(), EntityType.getKey(boss) + " cannot be summoned: only its fight makes it");
        }
        Map<Power, List<ResourceLocation>> awakens = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof AwakeningItem awakening) {
                awakens.computeIfAbsent(awakening.power(), p -> new ArrayList<>()).add(BuiltInRegistries.ITEM.getKey(item));
            }
        }
        check(awakens.getOrDefault(Power.THANOS, List.of()).equals(List.of(BuiltInRegistries.ITEM.getKey(ModItems.INFINITY_GAUNTLET.get()))),
                "only the Infinity Gauntlet awakens Thanos: " + awakens);
        check(awakens.getOrDefault(Power.APOCALYPSE, List.of()).equals(List.of(BuiltInRegistries.ITEM.getKey(ModItems.ESSENCE.get()))),
                "only the Essence awakens Apocalypse: " + awakens);
        check(!awakens.containsKey(Power.ULTRON), "no held item awakens Ultron (his Core is a ritual of the Scepter): " + awakens);
        // And no row of the Scepter's table makes a villain of anything but Ultron's own Core.
        for (Power power : Power.values()) {
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                for (boolean sneak : new boolean[]{false, true}) {
                    Ritual row = ScepterRituals.scepter(power, stack, sneak, sneak);
                    check(row != Ritual.ULTRON_CORE || item == ModItems.ULTRON_CORE.get(), "only Ultron's Core makes Ultron, not " + BuiltInRegistries.ITEM.getKey(item));
                }
            }
        }
        helper.succeed();
    }

    /**
     * A fight that is not won gives nobody the boss's power, though they fought it: each boss is fought by two who hurt it and
     * then lost (a wipe for Thanos, an abandonment for Ultron, a withdrawal for Apocalypse); then the same fight won gives each
     * of them the reward. The control shows the fighters were the kind the reward goes to.
     */
    @GameTest(template = ARENA, batch = "cross_rewards_lost", timeoutTicks = 200)
    public static void aBossFightThatIsNotWonGivesNoPowerAndAWonOneDoes(GameTestHelper helper) {
        TestKit.Fixture f = threeBosses(helper);
        f.config(BossConfig.REWARD_MIN_SECONDS, 0);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer a = tank(f.player(7.5, FLOOR, 7.5, Power.SCARLET_WITCH));
        ServerPlayer b = tank(f.player(13.5, FLOOR, 13.5, Power.PHOENIX));
        record Run(BossKind kind, Item reward, BossEnd lost) {
        }
        List<Run> runs = List.of(new Run(BossKind.THANOS, ModItems.INFINITY_GAUNTLET.get(), BossEnd.WIPE),
                new Run(BossKind.ULTRON, ModItems.ULTRON_CORE.get(), BossEnd.ABANDON),
                new Run(BossKind.APOCALYPSE, ModItems.ESSENCE.get(), BossEnd.WITHDRAW));
        int[] step = {0};
        BossFight[] fight = {null};
        f.everyTick(180, tick -> {
            int run = step[0] / 2;
            boolean win = step[0] % 2 == 1;
            if (run >= runs.size()) {
                return true;
            }
            Run r = runs.get(run);
            String what = r.kind() + (win ? " won" : " lost by " + r.lost());
            if (fight[0] == null) {
                UltronTestSupport.put(f, a, 7.5, FLOOR, 7.5);
                UltronTestSupport.put(f, b, 13.5, FLOOR, 13.5);
                check(BossFights.start(r.kind(), a, true), what + ": the fight starts");
                fight[0] = BossFights.active(level);
                if (fight[0] instanceof ApocalypseFight apocalypse) {
                    apocalypse.forceState(ApocalypseFight.Stage.FIGHT, 1);
                    ApocalypseTestSupport.calm(apocalypse);
                } else if (fight[0] instanceof UltronFight ultron) {
                    ultron.forceState(UltronFight.Phase.GROUND);
                    UltronTestSupport.calm(ultron);
                }
                return false;
            }
            if (fight[0] instanceof ThanosFight thanos && thanos.boss() == null) {
                return false; // (the portal's countdown)
            }
            LivingEntity boss = boss(fight[0]);
            if (boss instanceof ThanosEntity titan) {
                titan.forceArrived();
                titan.setPassive(true);
            }
            check(fight[0].isParticipant(a.getUUID()) && fight[0].isParticipant(b.getUUID()), what + ": both fight");
            // Both hurt him (Apocalypse's shield is worn by their mind power, which his reward counts too).
            for (ServerPlayer fighter : List.of(a, b)) {
                boss.invulnerableTime = 0;
                if (boss instanceof ApocalypseEntity) {
                    boss.hurt(ModDamageTypes.source(level, fighter == a ? ModDamageTypes.CHAOS_BEAM : ModDamageTypes.PHOENIX_BURST, fighter, fighter), 5.0F);
                } else {
                    // (Thanos rewards only who took three percent of him: a blow of 1000 is cut to the most one blow may take.)
                    boss.hurt(fighter.damageSources().playerAttack(fighter), 1000.0F);
                }
            }
            fight[0].end(win ? BossEnd.VICTORY : r.lost());
            BossFights.stopAll(server, StopReason.SERVER_STOPPING);
            check(!BossFights.anyActive(server), what + ": the fight is over");
            for (ServerPlayer fighter : List.of(a, b)) {
                int held = count(fighter, r.reward());
                int waiting = (int) BossGifts.pending(server, fighter.getUUID()).stream().filter(s -> s.is(r.reward())).count();
                int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(fighter.blockPosition()).inflate(30.0), e -> e.getItem().is(r.reward())).size();
                if (win) {
                    check(held == 1, what + ": " + fighter.getGameProfile().getName() + " (" + Powers.power(fighter) + ") is given one "
                            + BuiltInRegistries.ITEM.getKey(r.reward()) + ", not " + held);
                } else {
                    check(held == 0 && waiting == 0 && dropped == 0, what + ": nobody is given " + BuiltInRegistries.ITEM.getKey(r.reward())
                            + " (held " + held + ", waiting " + waiting + ", dropped " + dropped + ")");
                }
                fighter.getInventory().clearContent();
            }
            fight[0] = null;
            step[0]++;
            return false;
        });
    }

    /** The boss of a fight (the lanes keep it each in its own way). */
    private static LivingEntity boss(BossFight fight) {
        LivingEntity boss = switch (fight) {
            case ThanosFight thanos -> thanos.boss();
            case UltronFight ultron -> ultron.boss();
            case ApocalypseFight apocalypse -> apocalypse.boss();
            default -> null;
        };
        check(boss != null, "the boss of the " + fight.kind() + " fight is there");
        return boss;
    }

    /**
     * Thanos gathers his stones up to five, by the real clock of the fight, and never the sixth: the clock runs on past the
     * fifth for more than the time of another stone and nothing comes, and a Vision among the fighters (the Mind Stone is in
     * his forehead) does not change it, not even when Thanos kills him (thanos.md 3.4).
     */
    @GameTest(template = ARENA, batch = "cross_thanos_stones", timeoutTicks = 600)
    public static void thanosGathersFiveStonesAndNeverTheSixth(GameTestHelper helper) {
        TestKit.Fixture f = ThanosMoveSupport.fixture(helper);
        int interval = 200;
        f.config(ThanosBossConfig.STONE_INTERVAL_TICKS, interval);
        ServerPlayer vision = ThanosMoveSupport.fighter(f, -6, 0, Power.VISION);
        // (A second fighter keeps the clock running once the Vision is dead: it stands still with nobody in the arena.)
        ThanosMoveSupport.fighter(f, 6, 0, Power.THOR);
        ThanosEntity boss = ThanosBossTests.boss(f, vision);
        boss.forceCombatTicks(4L * interval - 20);
        check(boss.stones() == 4, "four stones after almost four intervals: " + boss.stones());
        long[] fifthAt = {-1};
        f.everyTick(580, tick -> {
            check(boss.stones() <= 5 && !boss.hasStone(StoneKind.MIND), "never a sixth stone, never the Mind Stone (tick " + tick + ")");
            if (fifthAt[0] < 0) {
                if (boss.stones() == 5) {
                    fifthAt[0] = boss.combatTicks();
                    check(boss.hasTimeStone(), "the fifth is the Time Stone");
                    check(boss.nextStoneIn() < 0, "and no other is coming");
                }
                return false;
            }
            if (boss.combatTicks() < fifthAt[0] + interval + 40) {
                if (vision.isAlive() && boss.combatTicks() > fifthAt[0] + interval / 2) {
                    vision.hurt(boss.damageSources().mobAttack(boss), 1.0E6F);
                    check(!vision.isAlive(), "Thanos killed the Vision");
                }
                return false;
            }
            check(boss.stones() == 5 && boss.nextStoneIn() < 0, "the clock ran on past another interval and he still has five: " + boss.stones());
            return true;
        });
    }

    // ============================================================================================================
    // 10. AreaTargets: every 3.0 area attack against a village

    /**
     * A village round a spot: who must be spared (a villager, an Iron Golem, a cow, the caster's own wolf, a neighbour's wolf
     * and the neighbour himself, PvP being off as on the GameTest server), and a Zombie that must not be.
     */
    private record Village(Villager villager, IronGolem golem, Cow cow, Wolf othersPet, Wolf ownPet, ServerPlayer neighbour, Zombie zombie,
                           Map<LivingEntity, Float> health) {
        static Village round(TestKit.Fixture f, ServerPlayer caster, double x, double z) {
            GameTestHelper helper = f.helper();
            ServerPlayer neighbour = f.player(x + 3.0, FLOOR, z - 3.0);
            Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(x + 3.0, FLOOR, z));
            IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(x, FLOOR, z + 3.0));
            Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(x - 3.0, FLOOR, z + 1.0));
            Wolf othersPet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(x, FLOOR, z - 3.0));
            othersPet.tame(neighbour);
            Wolf ownPet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(x + 2.0, FLOOR, z + 2.0));
            ownPet.tame(caster);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(x - 2.0, FLOOR, z - 2.0));
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
            zombie.setHealth(200.0F);
            Map<LivingEntity, Float> health = new LinkedHashMap<>();
            for (LivingEntity e : List.of(villager, golem, cow, othersPet, ownPet, neighbour)) {
                health.put(e, e.getHealth());
            }
            return new Village(villager, golem, cow, othersPet, ownPet, neighbour, zombie, health);
        }

        /** Nobody of the village was hurt or webbed by {@code what}. */
        void spared(String what) {
            for (Map.Entry<LivingEntity, Float> e : health.entrySet()) {
                LivingEntity creature = e.getKey();
                String name = EntityType.getKey(creature.getType()).getPath() + (creature == ownPet ? " (the caster's own pet)"
                        : creature == othersPet ? " (another player's pet)" : creature == neighbour ? " (the neighbour, PvP off)" : "");
                check(creature.isAlive() && creature.getHealth() == e.getValue(), what + " spared the " + name + ": " + e.getValue() + " -> " + creature.getHealth());
                check(!creature.hasEffect(ModEffects.WEBBED) && !creature.hasEffect(ModEffects.ENTOMBED), what + " held nothing of the " + name);
            }
        }

        boolean zombieHurt() {
            return zombie.getHealth() < 200.0F || !zombie.isAlive();
        }
    }

    /** The Phoenix Burst spares the village (phoenix.md 4.5, {@code burstHurtsPassives} false) and burns the Zombie. */
    @GameTest(template = ARENA, batch = "cross_area_burst", timeoutTicks = 80)
    public static void thePhoenixBurstSparesAVillage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(PhoenixConfig.BURST_CHARGE_TICKS, 10);
        f.config(PhoenixConfig.BURST_MIN_RADIUS, 4);
        f.config(PhoenixConfig.BURST_MAX_RADIUS, 9);
        f.config(PhoenixConfig.BURST_FIRE_SECONDS, 0);
        f.config(PhoenixConfig.BURST_KNOCKBACK, 0.0);
        ServerPlayer jean = f.player(10.5, FLOOR, 10.5, Power.PHOENIX);
        Village village = Village.round(f, jean, 10.5, 10.5);
        CrossHeroBossTests.hold(jean, 5);
        boolean[] fired = {false};
        f.everyTick(60, tick -> {
            PowerTicker.tick(jean);
            fired[0] |= PhoenixBurst.active() > 0;
            if (!fired[0] || PhoenixBurst.active() > 0) {
                return false;
            }
            check(village.zombieHurt(), "the Burst burned the Zombie");
            village.spared("the Phoenix Burst");
            return true;
        });
    }

    /** The Web Burst webs the Zombie and no villager, golem, animal or pet (spider-man.md 4.3.1 and the area rule). */
    @GameTest(template = ARENA, batch = "cross_area_web_burst", timeoutTicks = 40)
    public static void theWebBurstSparesAVillage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer spider = f.player(10.5, FLOOR, 10.5, Power.SPIDER_MAN);
        Village village = Village.round(f, spider, 10.5, 10.5);
        sneakPress(spider, 1);
        check(village.zombie().hasEffect(ModEffects.WEBBED), "the Web Burst webbed the Zombie");
        village.spared("the Web Burst");
        helper.succeed();
    }

    /** The Thanos hero's Titan Slam and the splash of his Power Blast spare the village and hurt the Zombie. */
    @GameTest(template = ARENA, batch = "cross_area_thanos", timeoutTicks = 80)
    public static void theTitansSlamAndBlastSpareAVillage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer thanos = tank(f.player(10.5, FLOOR, 10.5, Power.THANOS));
        PowerTicker.tick(thanos);
        Village village = Village.round(f, thanos, 10.5, 10.5);
        sneakPress(thanos, 1);
        float[] afterSlam = {-1.0F};
        f.everyTick(60, tick -> {
            PowerTicker.tick(thanos);
            if (tick == 20) {
                check(village.zombieHurt(), "the Titan Slam hurt the Zombie");
                village.spared("the Titan Slam");
                afterSlam[0] = village.zombie().getHealth();
                Powers.get(thanos).startCooldown(1, helper.getLevel().getGameTime(), 0);
                lookAt(thanos, village.zombie());
                press(thanos, 1);
            }
            if (tick == 30) {
                check(village.zombie().getHealth() < afterSlam[0], "the Power Blast hit the Zombie");
                village.spared("the Power Blast and its splash");
                return true;
            }
            return false;
        });
    }

    /** The Ultron hero's Gravity Slam and the splash of his Overcharge spare the village and hurt the Zombie. */
    @GameTest(template = ARENA, batch = "cross_area_ultron", timeoutTicks = 100)
    public static void ultronsSlamAndOverchargeSpareAVillage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer ultron = tank(f.player(10.5, FLOOR, 10.5, Power.ULTRON));
        PowerTicker.tick(ultron);
        Village village = Village.round(f, ultron, 10.5, 10.5);
        CrossHeroBossTests.put(helper, ultron, 10.5, FLOOR, 10.5);
        sneakPress(ultron, 3);
        check(village.zombieHurt(), "the Gravity Slam hurt the Zombie");
        village.spared("the Gravity Slam");
        float afterSlam = village.zombie().getHealth();
        Powers.get(ultron).setIronCharge(UltronConfig.BOLT_POOL.get());
        lookAt(ultron, village.zombie());
        sneakPress(ultron, 1);
        check(Powers.get(ultron).is(com.kolodkingal.enhanced.power.Live.ULTRON_CHARGING), "the Overcharge builds");
        f.everyTick(80, tick -> {
            lookAt(ultron, village.zombie());
            PowerTicker.tick(ultron);
            if (village.zombie().getHealth() < afterSlam) {
                village.spared("the Overcharge and its splash");
                return true;
            }
            return false;
        });
    }

    /** The Apocalypse hero's Sand Wave and Sandstorm spare the village and hurt the Zombie. */
    @GameTest(template = ARENA, batch = "cross_area_apocalypse", timeoutTicks = 100)
    public static void apocalypsesSandSparesAVillage(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer apocalypse = tank(f.player(4.5, FLOOR, 10.5, Power.APOCALYPSE));
        PowerTicker.tick(apocalypse);
        Village village = Village.round(f, apocalypse, 10.5, 10.5);
        TestKit.look(apocalypse, TestKit.EAST, 0.0F);
        press(apocalypse, 1);
        float[] afterWave = {-1.0F};
        f.everyTick(80, tick -> {
            PowerTicker.tick(apocalypse);
            if (tick == 25) {
                check(village.zombieHurt(), "the Sand Wave hurt the Zombie");
                village.spared("the Sand Wave");
                afterWave[0] = village.zombie().getHealth();
                CrossHeroBossTests.put(helper, apocalypse, 10.5, FLOOR, 10.5);
                sneakPress(apocalypse, 5);
            }
            if (tick == 60) {
                check(village.zombie().getHealth() < afterWave[0], "the Sandstorm hurt the Zombie");
                village.spared("the Sandstorm");
                return true;
            }
            return false;
        });
    }

    // ============================================================================================================
    // 11. Two boss fights cannot run at once

    /**
     * While one boss fights, neither of the other two can be started, by an operator or by its ritual, and the ritual item is
     * kept (integration.md 5.4: one fight at a time, forced starts too): every ordered pair of the three.
     */
    @GameTest(template = ARENA, batch = "cross_one_fight", timeoutTicks = 60)
    public static void neverTwoBossFightsAtOnceWhicheverStartsFirst(GameTestHelper helper) {
        TestKit.Fixture f = threeBosses(helper);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer starter = tank(f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z));
        ServerPlayer other = tank(f.player(4.5, FLOOR, 4.5));
        // (The three villains of 3.0 and their rituals: Hela's lane of 3.5 tests her own start rules.)
        List<BossKind> villains = List.of(BossKind.ULTRON, BossKind.THANOS, BossKind.APOCALYPSE);
        for (BossKind running : villains) {
            check(BossFights.start(running, starter, true), "the " + running + " fight starts");
            for (BossKind second : villains) {
                if (second == running) {
                    continue;
                }
                String pair = second + " while " + running + " fights";
                BossTestSupport.lastActionBarKey(other);
                check(!BossFights.start(second, other, true), "an operator cannot start " + pair);
                check("message.enhanced.boss.busy".equals(BossTestSupport.lastActionBarKey(other)), "and is told a fight runs (" + pair + ")");
                // Its ritual, with its item: refused because a fight runs (not for some other reason), the item kept.
                BossTestSupport.lastActionBarKey(other);
                switch (second) {
                    case THANOS -> {
                        InteractionResultHolder<ItemStack> result = use(other, ModItems.TESSERACT.get(), ModItems.LOKIS_SCEPTER.get());
                        check(result.getResult() != InteractionResult.CONSUME && !other.isUsingItem(), "the summoning does not begin: " + pair);
                        check(count(other, ModItems.TESSERACT.get()) == 1 && count(other, ModItems.LOKIS_SCEPTER.get()) == 1, "nothing used up: " + pair);
                    }
                    case ULTRON -> {
                        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
                        other.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.ULTRON_PROGRAM.get()));
                        useOnTheGround(other, under(other));
                        check(count(other, ModItems.ULTRON_PROGRAM.get()) == 1, "the Program is kept: " + pair);
                    }
                    case APOCALYPSE -> {
                        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAPSTONE.get()));
                        other.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                        useOnTheGround(other, under(other));
                        check(count(other, ModItems.CAPSTONE.get()) == 1, "the Capstone is kept: " + pair);
                    }
                }
                String told = BossTestSupport.lastActionBarKey(other);
                check("message.enhanced.boss.busy".equals(told), "the ritual is refused because a fight runs (" + pair + "), not: " + told);
                check(other.isAlive(), "the refused ritual hurt nobody: " + pair);
                check(BossFights.all(server).size() == 1 && BossFights.all(server).get(0).kind() == running, "still one fight, the " + running + " one: " + pair);
                other.getInventory().clearContent();
            }
            stopEveryFight(server);
        }
        helper.succeed();
    }

    // ============================================================================================================
    // 12. The bosses' rewards wait for whoever was away, and arrive at login, at respawn and after a dimension change

    /**
     * Each boss beaten with one fighter logged out and one dead: the reward of each lane (the Gauntlet, the Core, the Essence)
     * waits for them as a gift, and arrives when the one logs in again and when the other respawns.
     */
    @GameTest(template = ARENA, batch = "cross_gifts", timeoutTicks = 200)
    public static void aBeatenBossesRewardWaitsForTheAbsentAndTheDead(GameTestHelper helper) {
        TestKit.Fixture f = threeBosses(helper);
        f.config(BossConfig.REWARD_MIN_SECONDS, 0);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer starter = tank(f.player(UltronTestSupport.MIDDLE.x, FLOOR, UltronTestSupport.MIDDLE.z));
        record Run(BossKind kind, Item reward) {
        }
        List<Run> runs = List.of(new Run(BossKind.ULTRON, ModItems.ULTRON_CORE.get()), new Run(BossKind.THANOS, ModItems.INFINITY_GAUNTLET.get()),
                new Run(BossKind.APOCALYPSE, ModItems.ESSENCE.get()));
        int[] run = {0};
        int[] stage = {0};
        int[] stageAt = {0};
        BossFight[] fight = {null};
        ServerPlayer[] fighters = new ServerPlayer[2];
        f.everyTick(190, tick -> {
            if (run[0] >= runs.size()) {
                return true;
            }
            Run r = runs.get(run[0]);
            switch (stage[0]) {
                case 0 -> {
                    fighters[0] = tank(f.player(7.5, FLOOR, 7.5, Power.SCARLET_WITCH));
                    fighters[1] = tank(f.player(13.5, FLOOR, 13.5, Power.PHOENIX));
                    check(BossFights.start(r.kind(), starter, true), r.kind() + ": the fight starts");
                    fight[0] = BossFights.active(level);
                    if (fight[0] instanceof ApocalypseFight apocalypse) {
                        apocalypse.forceState(ApocalypseFight.Stage.FIGHT, 1);
                        ApocalypseTestSupport.calm(apocalypse);
                    } else if (fight[0] instanceof UltronFight ultron) {
                        ultron.forceState(UltronFight.Phase.GROUND);
                        UltronTestSupport.calm(ultron);
                    }
                    stage[0]++;
                    stageAt[0] = tick;
                }
                case 1 -> {
                    if (fight[0] instanceof ThanosFight thanos && thanos.boss() == null) {
                        // (The portal's countdown: the blows begin when he is there.)
                        stageAt[0] = tick;
                        return false;
                    }
                    LivingEntity boss = boss(fight[0]);
                    if (boss instanceof ThanosEntity titan) {
                        titan.forceArrived();
                        titan.setPassive(true);
                    }
                    // One fighter hurts him a tick (a boss does not take two blows in the same second as one).
                    int who = tick - stageAt[0] - 2;
                    if (who < 0 || who > 1) {
                        if (who > 1) {
                            stage[0]++;
                        }
                        return false;
                    }
                    ServerPlayer fighter = fighters[who];
                    check(fight[0].isParticipant(fighter.getUUID()), r.kind() + ": " + fighter.getGameProfile().getName() + " fights");
                    boss.invulnerableTime = 0;
                    if (boss instanceof ApocalypseEntity) {
                        boss.hurt(ModDamageTypes.source(level, who == 0 ? ModDamageTypes.CHAOS_BEAM : ModDamageTypes.PHOENIX_BURST, fighter, fighter), 5.0F);
                    } else {
                        // (Thanos rewards only who took three percent of him: a blow of 1000 is cut to the most one blow may take.)
                        boss.hurt(fighter.damageSources().playerAttack(fighter), 1000.0F);
                    }
                }
                case 2 -> {
                    // (A second apart: Thanos counts what one second may take from him.)
                    if (tick - stageAt[0] < 25) {
                        return false;
                    }
                    ServerPlayer absent = fighters[0];
                    ServerPlayer dead = fighters[1];
                    UUID absentId = absent.getUUID();
                    server.getPlayerList().remove(absent);
                    dead.setHealth(0.0F);
                    dead.die(level.damageSources().generic());
                    check(dead.isDeadOrDying(), "the second fighter is dead");
                    fight[0].end(BossEnd.VICTORY);
                    check(BossGifts.pending(server, absentId).stream().anyMatch(s -> s.is(r.reward())), r.kind() + ": the reward waits for the one who left");
                    check(BossGifts.pending(server, dead.getUUID()).stream().anyMatch(s -> s.is(r.reward())), r.kind() + ": and for the dead one");
                    BossFights.stopAll(server, StopReason.SERVER_STOPPING);
                    ServerPlayer back = f.player(7.5, FLOOR, 7.5, GameType.SURVIVAL, absentId);
                    check(count(back, r.reward()) == 1 && BossGifts.pending(server, absentId).isEmpty(), r.kind() + ": it arrives at the login");
                    ServerPlayer reborn = server.getPlayerList().respawn(dead, false, Entity.RemovalReason.KILLED);
                    f.onClose(() -> server.getPlayerList().remove(reborn));
                    check(count(reborn, r.reward()) == 1 && BossGifts.pending(server, dead.getUUID()).isEmpty(), r.kind() + ": and at the respawn");
                    server.getPlayerList().remove(back);
                    server.getPlayerList().remove(reborn);
                    run[0]++;
                    stage[0] = 0;
                }
                default -> {
                }
            }
            return false;
        });
    }

    // ============================================================================================================
    // 14. A server stop ends the fight and gives the ritual item back (12: the gift arrives after a dimension change)

    /**
     * The server stops in the middle of each fight, started by its own ritual: the fight ends at once, and what the ritual used
     * up comes back (the Ultron Program, the Capstone) as a gift, which arrives after a change of dimension and at the next
     * login; the Mad Titan's call uses nothing up, so nothing is owed and the Tesseract and the Scepter are still in the hands.
     */
    @GameTest(template = ARENA, batch = "cross_server_stop", timeoutTicks = 200)
    public static void aServerStopEndsEachFightAndGivesTheRitualItemBack(GameTestHelper helper) {
        TestKit.Fixture f = threeBosses(helper);
        ApocalypseRitualTests.readyGround(f);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        ServerPlayer wanda = tank(f.player(10.5, FLOOR, 16.5, Power.SCARLET_WITCH));
        int[] stage = {0};
        int[] stageAt = {0};
        ServerPlayer[] who = new ServerPlayer[1];
        f.everyTick(190, tick -> {
            switch (stage[0]) {
                case 0 -> {
                    // Ultron's ritual: the Scepter and the Program, Sneak + use on the ground.
                    ServerPlayer starter = tank(f.player(10.5, FLOOR, 10.5));
                    starter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
                    starter.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.ULTRON_PROGRAM.get()));
                    useOnTheGround(starter, under(starter));
                    check(BossFights.active(level) instanceof UltronFight, "the ritual woke Ultron");
                    UUID fightId = BossFights.active(level).id();
                    check(count(starter, ModItems.ULTRON_PROGRAM.get()) == 0, "and used the Program up");
                    check(Files.exists(SokoviaJournal.file(server, fightId)), "a note on disk says the Program is owed if the server crashes");
                    BossFights.stopAll(server, StopReason.SERVER_STOPPING);
                    check(!BossFights.anyActive(server), "the server stopping ends the Ultron fight at once");
                    check(level.getEntitiesOfClass(UltronEntity.class, new AABB(starter.blockPosition()).inflate(40.0)).isEmpty(), "and leaves no Ultron behind");
                    check(BossGifts.pending(server, starter.getUUID()).stream().filter(s -> s.is(ModItems.ULTRON_PROGRAM.get())).count() == 1,
                            "the Program waits for its owner as a gift");
                    // The next start reads the notes of fights a crash cut short: this one was settled, so it gives nothing again.
                    check(!Files.exists(SokoviaJournal.file(server, fightId)), "the note is gone: the Program is given back once");
                    SokoviaRecovery.recoverAll(server);
                    check(BossGifts.pending(server, starter.getUUID()).stream().filter(s -> s.is(ModItems.ULTRON_PROGRAM.get())).count() == 1,
                            "and the next start does not give it twice");
                    ServerLevel nether = server.getLevel(Level.NETHER);
                    check(nether != null, "the Nether is there");
                    starter.teleportTo(nether, 0.5, 128.0, 0.5, 0.0F, 0.0F);
                    check(starter.level() == nether, "the starter went to the Nether");
                    check(count(starter, ModItems.ULTRON_PROGRAM.get()) == 1 && BossGifts.pending(server, starter.getUUID()).isEmpty(),
                            "and the Program arrived with the change of dimension");
                    server.getPlayerList().remove(starter);
                    stage[0]++;
                    stageAt[0] = tick;
                }
                case 1 -> {
                    // Apocalypse's ritual: the Capstone planted on sand by day, a Scarlet Witch near.
                    if (tick - stageAt[0] < 3) {
                        return false;
                    }
                    ServerPlayer starter = ApocalypseRitualTests.starter(f, 10.5, 5.5);
                    ApocalypseRitualTests.plant(starter, helper.absolutePos(ApocalypseRitualTests.CLICK));
                    check(BossFights.active(level) instanceof ApocalypseFight, "the Capstone called Apocalypse");
                    check(count(starter, ModItems.CAPSTONE.get()) == 0, "and was used up");
                    BossFights.stopAll(server, StopReason.SERVER_STOPPING);
                    check(!BossFights.anyActive(server), "the server stopping ends the Apocalypse fight at once");
                    UUID id = starter.getUUID();
                    check(BossGifts.pending(server, id).stream().anyMatch(s -> s.is(ModItems.CAPSTONE.get())), "the Capstone waits as a gift");
                    server.getPlayerList().remove(starter);
                    ServerPlayer back = f.player(10.5, FLOOR, 5.5, GameType.SURVIVAL, id);
                    check(count(back, ModItems.CAPSTONE.get()) == 1 && BossGifts.pending(server, id).isEmpty(), "and arrives at the next login");
                    server.getPlayerList().remove(back);
                    stage[0]++;
                    stageAt[0] = tick;
                }
                case 2 -> {
                    // The Mad Titan's call: the Tesseract and the Scepter held up.
                    ServerPlayer starter = tank(f.player(10.5, FLOOR, 10.5));
                    use(starter, ModItems.TESSERACT.get(), ModItems.LOKIS_SCEPTER.get());
                    check(starter.isUsingItem(), "the summoning's hold begins");
                    who[0] = starter;
                    stage[0]++;
                }
                case 3 -> {
                    ServerPlayer starter = who[0];
                    starter.doTick();
                    if (!(BossFights.active(level) instanceof ThanosFight)) {
                        return false;
                    }
                    BossFights.stopAll(server, StopReason.SERVER_STOPPING);
                    check(!BossFights.anyActive(server), "the server stopping ends the Thanos fight at once");
                    check(BossGifts.pending(server, starter.getUUID()).isEmpty(), "his call used nothing up, so nothing is owed");
                    check(count(starter, ModItems.TESSERACT.get()) == 1 && count(starter, ModItems.LOKIS_SCEPTER.get()) == 1,
                            "the Tesseract and the Scepter are still his");
                    check(wanda.isAlive(), "and the Scarlet Witch who stood by is well");
                    return true;
                }
                default -> {
                }
            }
            return false;
        });
    }

    // ============================================================================================================
    // 13. A player carried by Ultron's Sokovia lift is not kicked for flying

    private static final Field FLOATING;

    static {
        try {
            FLOATING = ServerGamePacketListenerImpl.class.getDeclaredField("clientIsFloating");
            FLOATING.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Heroes of every 3.0 lane on the island while it rises for 200 ticks (a survival player without flight is kicked after
     * 80 in the air): a Spider-Man, a Phoenix, a Thanos (taller than a player), an Apocalypse and a powerless player are all
     * carried, ticked by their own heroes' code as a connected client's would be, and none is kicked.
     */
    @GameTest(template = SokoviaTestSupport.PAD, batch = "cross_lift_kick", timeoutTicks = 700)
    public static void heroesCarriedByTheLiftAreNotKickedForFlying(GameTestHelper helper) {
        TestKit.Fixture f = SokoviaTestSupport.pad(helper);
        f.config(UltronBossConfig.LIFT_TICKS, 200);
        double x = SokoviaTestSupport.MIDDLE_X;
        double z = SokoviaTestSupport.MIDDLE_Z;
        double y = SokoviaTestSupport.GROUND;
        List<ServerPlayer> riders = List.of(
                tank(f.player(x, y, z)),
                tank(f.player(x + 3.0, y, z, Power.SPIDER_MAN)),
                tank(f.player(x - 3.0, y, z, Power.PHOENIX)),
                tank(f.player(x, y, z + 3.0, Power.THANOS)),
                tank(f.player(x, y, z - 3.0, Power.APOCALYPSE)));
        for (ServerPlayer rider : riders) {
            PowerTicker.tick(rider);
            rider.setHealth(rider.getMaxHealth());
            check(!rider.getAbilities().mayfly, Powers.power(rider) + ": a survival player who may not fly");
        }
        UltronFight fight = UltronTestSupport.onTheGround(f, riders.get(0));
        UltronTestSupport.calm(fight);
        Map<ServerPlayer, Integer> carried = new LinkedHashMap<>();
        Map<ServerPlayer, Integer> floating = new LinkedHashMap<>();
        Map<ServerPlayer, Integer> highest = new LinkedHashMap<>();
        Map<ServerPlayer, Double> startY = new LinkedHashMap<>();
        Map<ServerPlayer, Double> risen = new LinkedHashMap<>();
        SokoviaTestSupport.Script script = new SokoviaTestSupport.Script();
        script.then("the lock", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            fight.forceState(UltronFight.Phase.LIFT_LOCK);
            for (ServerPlayer rider : riders) {
                TestKit.sentPackets(rider);
                startY.put(rider, rider.getY());
            }
        });
        script.step("200 ticks in the air", () -> {
            SokoviaLift.Lift lift = SokoviaLift.of(fight);
            double ground = helper.absoluteVec(new Vec3(0, y + 1.0, 0)).y;
            for (ServerPlayer rider : riders) {
                boolean up = lift != null && lift.carried().contains(rider.getUUID());
                boolean inTheAir = up && rider.getY() > ground;
                try {
                    FLOATING.setBoolean(rider.connection, inTheAir);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
                if (inTheAir) {
                    floating.merge(rider, 1, Integer::sum);
                }
                risen.merge(rider, rider.getY() - startY.get(rider), Math::max);
                // What the server does to every connected player each tick (the player's own tick, the heroes' code
                // with it, and the kick check), and what the client does with the velocity it is given.
                rider.connection.tick();
                SokoviaTestSupport.client(rider);
                if (up) {
                    carried.merge(rider, 1, Integer::sum);
                    highest.merge(rider, rider.connection.aboveGroundTickCount, Math::max);
                    // (The lift holds them without gravity, so the server allows them any time in the air; their own hero's
                    // tick must not give it back.)
                    check(rider.isNoGravity(), Powers.power(rider) + " was given back gravity while the lift carried them");
                }
                for (Object packet : TestKit.sentPackets(rider)) {
                    check(!(packet instanceof ClientboundDisconnectPacket), Powers.power(rider) + " was kicked after " + carried.getOrDefault(rider, 0) + " ticks");
                }
            }
            return lift != null && lift.step() == SokoviaLift.Step.PLACED;
        });
        script.then("never near the limit", () -> {
            for (ServerPlayer rider : riders) {
                String who = Powers.power(rider).toString();
                check(carried.getOrDefault(rider, 0) >= 200, who + " was carried for " + carried.getOrDefault(rider, 0) + " ticks");
                // (Off the ground, as the server sees a client in the air, for longer than the 80 ticks that get a player kicked.)
                check(floating.getOrDefault(rider, 0) > 100, who + " hung in the air for only " + floating.getOrDefault(rider, 0) + " ticks");
                check(risen.getOrDefault(rider, 0.0) > SokoviaTestSupport.LIFT - 1.0, who + " rose with the island only " + risen.get(rider) + " blocks");
                check(highest.getOrDefault(rider, 0) <= 2, who + "'s floating counter rose to " + highest.get(rider));
                check(rider.isAlive() && rider.connection.isAcceptingMessages(), who + " is still on the server");
            }
        });
        script.run(f, 650);
    }

    /**
     * 3.3: Wanda's Hex stands over the ground where Ultron fights: the island carries no one through its wall (only those she
     * lets in come in), so at the warning the lift is refused and the fight goes on as the Tremor (ultron.md: the lift's
     * warning asks whether the sky is still free).
     */
    @GameTest(template = SokoviaTestSupport.PAD, batch = "cross_hex_lift", timeoutTicks = 200)
    public static void theSokoviaLiftDoesNotRiseThroughAHexWall(GameTestHelper helper) {
        TestKit.Fixture f = SokoviaTestSupport.pad(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        f.config(EnhancedConfig.HEX_DEATH_CHANCE, 1.0);
        double x = SokoviaTestSupport.MIDDLE_X;
        double z = SokoviaTestSupport.MIDDLE_Z;
        double y = SokoviaTestSupport.GROUND;
        ServerPlayer wanda = tank(f.player(x, y, z, Power.SCARLET_WITCH));
        ServerPlayer spider = tank(f.player(x + 3.0, y, z, Power.SPIDER_MAN));
        UltronFight fight = UltronTestSupport.onTheGround(f, wanda);
        UltronTestSupport.calm(fight);
        HexManager.create(wanda);
        f.onClose(() -> HexManager.dissolveAllOwnedBy(f.level().getServer(), wanda.getUUID()));
        SokoviaTestSupport.Script script = new SokoviaTestSupport.Script();
        script.step("the fighters join", () -> fight.participants().size() == 2);
        script.then("the warning", () -> {
            fight.forceState(UltronFight.Phase.LIFT_WARN);
            check(!fight.isLifting(), "a Hex wall cuts the island's way: no lift, the Tremor instead");
            check(!fight.hexExempt() && spider.isAlive(), "and nobody is carried through the wall");
        });
        script.run(f, 180);
    }

    private static Vec3 rel(GameTestHelper helper, Entity entity) {
        return helper.relativeVec(entity.position());
    }

    private CrossHeroTests() {
    }
}
