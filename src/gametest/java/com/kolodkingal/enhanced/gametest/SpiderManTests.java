package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.SpiderClimb;
import com.kolodkingal.enhanced.ability.SpiderMath;
import com.kolodkingal.enhanced.ability.SpiderServer;
import com.kolodkingal.enhanced.ability.SpiderSense;
import com.kolodkingal.enhanced.ability.SpiderWebs;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.mixin.ServerGamePacketListenerImplAccessor;
import com.kolodkingal.enhanced.network.SpiderClingPayload;
import com.kolodkingal.enhanced.network.SpiderFxPayload;
import com.kolodkingal.enhanced.network.SpiderSensePayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforgespi.locating.IModFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.EAST;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.look;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Spider-Man (DESIGN section 14), the server side: how a player becomes him (the Radioactive Spider), his body and his
 * suits, the Web Shot, the Web Burst, the Web Zip and the Web Swing, walls and ceilings, the Iron Spider's legs and Instant
 * Kill, the Black Eyes, the spider-sense and how it all ends. His movement is the client's, so what needs it is tested
 * through the pure functions ({@link SpiderClimb}, {@link SpiderMath}) and through the flags the server raises, with the
 * player put where a client would have moved him. Tests that place blocks round him and tick him have a batch of their
 * own, as does the one that needs a boss fight.
 *
 * <p>Nothing ticks a player without a client, so a test ticks the hero itself ({@code PowerTicker.tick}); what depends on
 * the game's clock (a cooldown, a hysteresis, a timeout) is driven by {@code everyTick}, one hero tick per game tick.
 */
public final class SpiderManTests {
    private static final String QUICK = "spider";
    private static final String CLIMB = "spider_climb";
    private static final String BOSS = "spider_boss";

    // ------------------------------------------------------------------------------------------------------------
    // Helpers

    private static ServerPlayer spider(TestKit.Fixture f, double x, double z) {
        return f.player(x, FLOOR, z, Power.SPIDER_MAN);
    }

    private static PowerData data(Player player) {
        return Powers.get(player);
    }

    private static void press(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, true);
        AbilityHandler.onKey(player, ability, false);
    }

    private static void pressSneaking(ServerPlayer player, int ability) {
        player.setShiftKeyDown(true);
        press(player, ability);
        player.setShiftKeyDown(false);
    }

    private static void hold(ServerPlayer player, int ability, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        AbilityHandler.onKey(player, ability, true);
        player.setShiftKeyDown(false);
    }

    private static void release(ServerPlayer player, int ability) {
        AbilityHandler.onKey(player, ability, false);
    }

    private static void wear(ServerPlayer player, String look) {
        Powers.setVariant(player, Variants.get(Power.SPIDER_MAN, look));
        PowerTicker.tick(player);
    }

    private static long now(GameTestHelper helper) {
        return helper.getLevel().getGameTime();
    }

    private static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    private static Vec3 abs(GameTestHelper helper, double x, double y, double z) {
        return helper.absoluteVec(new Vec3(x, y, z));
    }

    /** A creature that does not burn in the sun and has no will of its own: a Husk, which is a Zombie. */
    private static Zombie zombie(GameTestHelper helper, double x, double z) {
        Zombie husk = TestKit.spawnWithNoFreeWill(helper, EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.getAttribute(Attributes.ARMOR).setBaseValue(0.0);
        return husk;
    }

    private static void toughen(LivingEntity entity) {
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        entity.setHealth(1000.0F);
    }

    private static double exhaustion(Player player) {
        return player.getFoodData().getExhaustionLevel();
    }

    private static void resetHunger(Player player) {
        player.getFoodData().setExhaustion(0.0F);
    }

    private static boolean isWeb(GameTestHelper helper, BlockPos relative) {
        return helper.getBlockState(relative).is(ModBlocks.SPIDER_WEB.get());
    }

    private static int countWebs(GameTestHelper helper) {
        int count = 0;
        for (int x = 0; x <= 20; x++) {
            for (int y = 2; y <= 9; y++) {
                for (int z = 0; z <= 20; z++) {
                    if (isWeb(helper, new BlockPos(x, y, z))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** What the server sent a player since the last time: read once, asked many ways. */
    private record Seen(List<Object> packets) {
        /** The key of the last action-bar message, or null. */
        String message() {
            String key = null;
            for (Object packet : packets) {
                if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay() && chat.content().getContents() instanceof TranslatableContents translatable) {
                    key = translatable.getKey();
                }
            }
            return key;
        }

        List<SpiderFxPayload> fx() {
            List<SpiderFxPayload> found = new ArrayList<>();
            for (Object sent : packets) {
                collect(sent, found, null);
            }
            return found;
        }

        List<SpiderSensePayload> senses() {
            List<SpiderSensePayload> found = new ArrayList<>();
            for (Object sent : packets) {
                collect(sent, null, found);
            }
            return found;
        }

        boolean heard(String name) {
            ResourceLocation id = Enhanced.id(name);
            return TestKit.soundsIn(packets).stream().anyMatch(s -> s.id().equals(id));
        }

        /** The last end of a zip or a swing among the effects, or null. */
        SpiderFxPayload lastEnd() {
            SpiderFxPayload end = null;
            for (SpiderFxPayload payload : fx()) {
                if (payload.kind() == SpiderFxPayload.Kind.ZIP_END || payload.kind() == SpiderFxPayload.Kind.SWING_END) {
                    end = payload;
                }
            }
            return end;
        }
    }

    private static void collect(Object packet, List<SpiderFxPayload> effects, List<SpiderSensePayload> senses) {
        if (packet instanceof ClientboundCustomPayloadPacket custom) {
            if (effects != null && custom.payload() instanceof SpiderFxPayload effect) {
                effects.add(effect);
            }
            if (senses != null && custom.payload() instanceof SpiderSensePayload sense) {
                senses.add(sense);
            }
        } else if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> inner : bundle.subPackets()) {
                collect(inner, effects, senses);
            }
        }
    }

    private static Seen seen(ServerPlayer player) {
        return new Seen(TestKit.sentPackets(player));
    }

    private static void stone(GameTestHelper helper, int x, int y, int z) {
        helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
    }

    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    /** What a web block does to an entity that stands in it: the multiplier it was made to move at, or null if untouched. */
    private static Vec3 stuck(Entity entity) {
        try {
            Field field = Entity.class.getDeclaredField("stuckSpeedMultiplier");
            field.setAccessible(true);
            Vec3 multiplier = (Vec3) field.get(entity);
            return multiplier == null || multiplier.equals(Vec3.ZERO) ? null : multiplier;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void clearStuck(Entity entity) {
        entity.makeStuckInBlock(Blocks.AIR.defaultBlockState(), Vec3.ZERO);
    }

    /** Puts the player at a place of the arena, the way a client's movement does (no packet, no previous-tick position). */
    private static void put(GameTestHelper helper, ServerPlayer player, double x, double y, double z) {
        Vec3 at = abs(helper, x, y, z);
        player.absMoveTo(at.x, at.y, at.z);
    }

    /**
     * Runs {@code body} with {@code block} listed in the block tag {@code tag}, as a data pack could list it (the tag is
     * bound once when the data loads; the holder's set is swapped for the length of the call, and put back whatever
     * happens). Nothing else runs in between, so no other test sees it.
     */
    @SuppressWarnings("unchecked")
    private static void listedIn(Block block, TagKey<Block> tag, Runnable body) {
        Holder.Reference<Block> holder = block.builtInRegistryHolder();
        try {
            Field tags = Holder.Reference.class.getDeclaredField("tags");
            tags.setAccessible(true);
            Set<TagKey<Block>> before = (Set<TagKey<Block>>) tags.get(holder);
            Set<TagKey<Block>> with = new HashSet<>(before);
            with.add(tag);
            tags.set(holder, with);
            try {
                body.run();
            } finally {
                tags.set(holder, before);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot list " + block + " in " + tag, e);
        }
    }

    private static void noModifiersOfHis(ServerPlayer player) {
        for (var attribute : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                for (var modifier : instance.getModifiers()) {
                    check(!modifier.id().getPath().startsWith("spider_"), "a modifier of his is left: " + modifier.id());
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Getting the power

    @EnhancedTest(template = ARENA, batch = QUICK, timeoutTicks = 400)
    public static void theRadioactiveBiteAwakensSpiderMan(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RADIOACTIVE_SPIDER.get(), 2));
        float health = player.getHealth();
        TestKit.sentPackets(player);

        InteractionResultHolder<ItemStack> used = player.getMainHandItem().getItem().use(level, player, InteractionHand.MAIN_HAND);
        check(used.getResult().consumesAction(), "the bite is a use");
        check(player.getMainHandItem().getCount() == 1, "the spider is used up");
        check(player.hasEffect(ModEffects.RADIOACTIVE_BITE) && player.hasEffect(MobEffects.CONFUSION) && player.hasEffect(MobEffects.WEAKNESS)
                && player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "he has the bite, Nausea, Weakness and Slowness");
        check(!player.hasEffect(MobEffects.POISON), "the poison comes at the end");
        assertNear(player.getHealth(), health - 1.0F, 0.01, "one point of damage");
        check(player.getEffect(ModEffects.RADIOACTIVE_BITE).getDuration() == SpiderConfig.BITE_DELAY_TICKS.get(), "seven seconds by default");
        check(SpiderConfig.BITE_DELAY_TICKS.get() == 140, "(140 ticks)");
        check(seen(player).heard("spider_bite"), "the bite is heard");
        check(!Powers.power(player).hasPowers(), "he is not Spider-Man yet");

        // The sickness runs out the way it really does: the player ticks until it ends. The poison starts for the last 60.
        boolean poisoned = false;
        for (int i = 0; i < 200 && player.hasEffect(ModEffects.RADIOACTIVE_BITE); i++) {
            player.doTick();
            poisoned |= player.hasEffect(MobEffects.POISON);
            check(i > 70 || !player.hasEffect(MobEffects.POISON), "no poison in the first 70 ticks, at tick " + i);
        }
        check(poisoned, "the last three seconds are poisoned");
        check(Powers.is(player, Power.SPIDER_MAN), "when the bite ends he is Spider-Man");
        check(Powers.variant(player).id().equals("homemade"), "in the Homemade Suit");
        PowerData data = data(player);
        check(data.toggle(Toggle.WALL_CRAWL) && data.toggle(Toggle.SPIDER_SENSE) && !data.toggle(Toggle.SPIDER_LEGS), "with his toggles at their defaults");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void aBiteChangesNothingForAHero(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        for (Power hero : List.of(Power.MAGNETO, Power.THOR, Power.SPIDER_MAN)) {
            ServerPlayer player = f.player(10.5, FLOOR, 10.5, hero);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RADIOACTIVE_SPIDER.get(), 3));
            TestKit.sentPackets(player);
            InteractionResultHolder<ItemStack> used = player.getMainHandItem().getItem().use(level, player, InteractionHand.MAIN_HAND);
            check(used.getResult().consumesAction() && !used.getResult().shouldSwing(),
                    hero + " is turned away, and the refusal spends the click so the other hand's item is not used instead");
            check(player.getMainHandItem().getCount() == 3, hero + " keeps the spider");
            check(!player.hasEffect(ModEffects.RADIOACTIVE_BITE), hero + " is not bitten");
            check("message.enhanced.bite.not_now".equals(seen(player).message()), hero + " is told it does not mix");
        }
        // Creative: bitten, and the spider is kept.
        ServerPlayer creative = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RADIOACTIVE_SPIDER.get(), 2));
        creative.getMainHandItem().getItem().use(level, creative, InteractionHand.MAIN_HAND);
        check(creative.hasEffect(ModEffects.RADIOACTIVE_BITE), "a Creative player is bitten");
        check(creative.getMainHandItem().getCount() == 2, "and keeps the spider");
        // Already sick from a bite: nothing more.
        ServerPlayer sick = f.player(10.5, FLOOR, 10.5);
        sick.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RADIOACTIVE_SPIDER.get(), 2));
        sick.getMainHandItem().getItem().use(level, sick, InteractionHand.MAIN_HAND);
        check(sick.getMainHandItem().getCount() == 1, "the first bite uses one");
        TestKit.sentPackets(sick);
        int left = sick.getEffect(ModEffects.RADIOACTIVE_BITE).getDuration();
        sick.getMainHandItem().getItem().use(level, sick, InteractionHand.MAIN_HAND);
        check(sick.getMainHandItem().getCount() == 1, "a second spider is not used up while he is sick");
        check("message.enhanced.bite.sick".equals(seen(sick).message()), "he is told he is still sick");
        check(sick.getEffect(ModEffects.RADIOACTIVE_BITE).getDuration() == left, "and the sickness is not restarted");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_milk_and_another_power_cancel_the_bite", timeoutTicks = 200)
    public static void milkAndAnotherPowerCancelTheBite(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.BITE_DELAY_TICKS, 20);
        ServerLevel level = helper.getLevel();
        // Milk takes the effect off: nothing awakens.
        ServerPlayer milk = f.player(8.5, FLOOR, 10.5);
        milk.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RADIOACTIVE_SPIDER.get()));
        milk.getMainHandItem().getItem().use(level, milk, InteractionHand.MAIN_HAND);
        TestKit.sentPackets(milk);
        milk.removeEffect(ModEffects.RADIOACTIVE_BITE);
        check("message.enhanced.bite.fades".equals(seen(milk).message()), "the bite fades");
        for (int i = 0; i < 40; i++) {
            milk.doTick();
        }
        check(!Powers.power(milk).hasPowers(), "milk cures it");
        // Another power in the meantime: the bite changes nothing.
        ServerPlayer other = f.player(12.5, FLOOR, 10.5);
        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RADIOACTIVE_SPIDER.get()));
        other.getMainHandItem().getItem().use(level, other, InteractionHand.MAIN_HAND);
        Powers.setPower(other, Power.MAGNETO);
        TestKit.sentPackets(other);
        for (int i = 0; i < 40; i++) {
            other.doTick();
        }
        check(Powers.is(other, Power.MAGNETO), "he is still Magneto");
        check(!other.hasEffect(ModEffects.RADIOACTIVE_BITE), "the bite is over");
        // Dying: nothing happens either.
        ServerPlayer dead = f.player(10.5, FLOOR, 12.5);
        dead.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RADIOACTIVE_SPIDER.get()));
        dead.getMainHandItem().getItem().use(level, dead, InteractionHand.MAIN_HAND);
        dead.kill();
        for (int i = 0; i < 40; i++) {
            dead.doTick();
        }
        check(!Powers.power(dead).hasPowers(), "the dead do not wake");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK, timeoutTicks = 200)
    public static void theRecipeAndTheLootAreThere(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("radioactive_spider"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:radioactive_spider exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack f = new ItemStack(Items.FERMENTED_SPIDER_EYE);
        ItemStack g = new ItemStack(Items.GLOW_INK_SAC);
        ItemStack b = new ItemStack(Items.SLIME_BLOCK);
        check(recipe.matches(CraftingInput.of(3, 3, List.of(f, g, f, g, b, g, f, g, f)), level), "F G F / G B G / F G F");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(f, g, f, g, new ItemStack(Items.SLIME_BALL), g, f, g, f)), level), "a slime ball is not a block");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.RADIOACTIVE_SPIDER.get()), "it makes the Radioactive Spider");
        check(level.getServer().getAdvancements().get(Enhanced.id("recipes/misc/radioactive_spider")) != null, "the recipe has its advancement");

        // The chests, by the chance each one gives (the bounds are many standard deviations wide).
        check(chestSpiders(helper, BuiltInLootTables.ABANDONED_MINESHAFT, 3000, 60, 180), "mineshaft chests, 4 percent");
        check(chestSpiders(helper, BuiltInLootTables.SIMPLE_DUNGEON, 3000, 90, 215), "dungeon chests, 5 percent");
        check(chestSpiders(helper, BuiltInLootTables.TRIAL_CHAMBERS_REWARD, 3000, 40, 145), "trial chamber reward chests, 3 percent");
        check(chestSpiders(helper, BuiltInLootTables.ANCIENT_CITY, 3000, 20, 105), "ancient city chests, 2 percent");

        // A cave spider killed by a player: 0.4 percent; killed by a fall: never.
        ServerPlayer killer = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        var cave = TestKit.spawnWithNoFreeWill(helper, EntityType.CAVE_SPIDER, new Vec3(10.5, FLOOR, 10.5));
        LootTable table = level.getServer().reloadableRegistries().getLootTable(EntityType.CAVE_SPIDER.getDefaultLootTable());
        LootParams byPlayer = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, cave)
                .withParameter(LootContextParams.ORIGIN, cave.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .create(LootContextParamSets.ENTITY);
        LootParams byFall = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, cave)
                .withParameter(LootContextParams.ORIGIN, cave.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().fall())
                .create(LootContextParamSets.ENTITY);
        RandomSource random = RandomSource.create(2002L);
        int withPlayer = 0;
        int without = 0;
        for (int i = 0; i < 20000; i++) {
            for (ItemStack stack : table.getRandomItems(byPlayer, random)) {
                if (stack.is(ModItems.RADIOACTIVE_SPIDER.get())) {
                    withPlayer += stack.getCount();
                }
            }
            for (ItemStack stack : table.getRandomItems(byFall, random)) {
                if (stack.is(ModItems.RADIOACTIVE_SPIDER.get())) {
                    without += stack.getCount();
                }
            }
        }
        check(withPlayer >= 35 && withPlayer <= 130, "a cave spider killed by a player drops it about 0.4 percent of the time, got " + withPlayer + "/20000");
        check(without == 0, "only when killed by a player, got " + without);
        helper.succeed();
    }

    private static boolean chestSpiders(GameTestHelper helper, ResourceKey<LootTable> id, int rolls, int low, int high) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(id);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, abs(helper, 10.5, 2.0, 10.5))
                .create(LootContextParamSets.CHEST);
        RandomSource random = RandomSource.create(1962L);
        int spiders = 0;
        for (int i = 0; i < rolls; i++) {
            for (ItemStack stack : table.getRandomItems(params, random)) {
                if (stack.is(ModItems.RADIOACTIVE_SPIDER.get())) {
                    spiders += stack.getCount();
                }
            }
        }
        Enhanced.LOGGER.info("{} x{}: {} Radioactive Spiders", id.location(), rolls, spiders);
        return spiders >= low && spiders <= high;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Body and suits

    private static double armor(Player player) {
        return player.getAttributeValue(Attributes.ARMOR);
    }

    @EnhancedTest(template = ARENA, batch = "spider_each_suit_has_its_protection")
    public static void eachSuitHasItsProtection(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        PowerTicker.tick(player);
        record Suit(String look, double armor, double toughness, double knockback) {
        }
        for (Suit suit : List.of(new Suit("homemade", 0, 0, 0), new Suit("iron_spider", 14, 6, 0.2), new Suit("brand_new_day", 8, 2, 0),
                new Suit("own", 0, 0, 0))) {
            wear(player, suit.look());
            assertNear(armor(player), suit.armor(), 1.0E-6, suit.look() + " armor");
            assertNear(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), suit.toughness(), 1.0E-6, suit.look() + " toughness");
            assertNear(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), suit.knockback(), 1.0E-6, suit.look() + " knockback resistance");
        }
        // The look follows at once, and the numbers are the config's, read live.
        f.config(SpiderConfig.IRON_SPIDER_ARMOR, 20.0);
        wear(player, "iron_spider");
        assertNear(armor(player), 20.0, 1.0E-6, "the config is read live");
        f.config(SpiderConfig.HOMEMADE_ARMOR, 3.0);
        wear(player, "own");
        assertNear(armor(player), 3.0, 1.0E-6, "his own skin has the homemade suit's armor");
        // Losing the power takes every modifier of his off.
        wear(player, "brand_new_day");
        Powers.setPower(player, Power.NONE);
        noModifiersOfHis(player);
        assertNear(armor(player), 0.0, 1.0E-6, "no armor left");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_body_is_quicker_stronger_and_lands_softly")
    public static void theBodyIsQuickerStrongerAndLandsSoftly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        ServerPlayer other = f.player(8.5, FLOOR, 8.5, Power.THOR);
        PowerTicker.tick(player);
        PowerTicker.tick(other);
        assertNear(player.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.1 * 1.15, 1.0E-6, "his speed is 15 percent up");
        assertNear(player.getAttributeValue(Attributes.JUMP_STRENGTH), 0.42 * 1.30, 1.0E-6, "his jump is 30 percent up");
        assertNear(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 3.0, 1.0E-6, "his fists hit for 3");
        assertNear(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 0.5, 1.0E-6, "half the fall damage");
        assertNear(player.getAttributeValue(Attributes.MAX_HEALTH), 20.0, 1.0E-6, "his health is the normal 20");
        check(other.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Enhanced.id("spider_speed")) == null, "nobody else has his speed");
        assertNear(other.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 1.0, 1.0E-6, "or his falls");
        f.config(SpiderConfig.SPEED_BONUS, 0.5);
        PowerTicker.tick(player);
        assertNear(player.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.15, 1.0E-6, "the speed follows the config");
        f.config(SpiderConfig.SPEED_BONUS, 0.15);

        // The legs: no fall damage at all, and a longer reach.
        wear(player, "iron_spider");
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        press(player, 4);
        PowerTicker.tick(player);
        check(data(player).toggle(Toggle.SPIDER_LEGS), "the legs are out");
        assertNear(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 0.0, 1.0E-6, "with the legs out he takes no fall damage");
        assertNear(player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), reach + 2.0, 1.0E-6, "and reaches 2 blocks further");
        press(player, 4);
        PowerTicker.tick(player);
        assertNear(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 0.5, 1.0E-6, "the legs in, half again");
        assertNear(player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), reach, 1.0E-6, "and the reach back");
        // The Black Eyes: another quarter off.
        wear(player, "homemade");
        press(player, 5);
        PowerTicker.tick(player);
        check(data(player).is(Live.BLACK_EYES), "the surge is on");
        assertNear(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), 0.25, 1.0E-6, "with the Black Eyes he takes a quarter");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void webbingNeverHoldsASpiderMan(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = spider(f, 10.5, 10.5);
        check(!player.addEffect(new MobEffectInstance(ModEffects.WEBBED, 100)) && !player.hasEffect(ModEffects.WEBBED), "he is never Webbed");
        Zombie zombie = zombie(helper, 12.5, 10.5);
        BlockPos web = abs(helper, 12, 2, 10);
        level.setBlock(web, ModBlocks.SPIDER_WEB.get().defaultBlockState(), 3);
        BlockState state = level.getBlockState(web);
        state.entityInside(level, web, zombie);
        Vec3 held = stuck(zombie);
        check(held != null && held.x < 1.0, "a zombie in the web is stuck: " + held);
        clearStuck(player);
        player.setPos(Vec3.atBottomCenterOf(web));
        state.entityInside(level, web, player);
        check(stuck(player) == null, "he walks through it");
        ServerPlayer other = f.player(9.5, FLOOR, 9.5, Power.MAGNETO);
        other.setPos(Vec3.atBottomCenterOf(web));
        state.entityInside(level, web, other);
        check(stuck(other) != null, "a hero of another power is stuck");
        ServerPlayer flier = f.player(9.5, FLOOR, 12.5, GameType.CREATIVE);
        flier.getAbilities().flying = true;
        flier.setPos(Vec3.atBottomCenterOf(web));
        state.entityInside(level, web, flier);
        check(stuck(flier) == null, "a Creative player who flies passes too");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Web Shot, organic webs, web blocks

    @EnhancedTest(template = ARENA, batch = QUICK, timeoutTicks = 100)
    public static void aWebShotWebsACreature(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 6.5);
        ServerPlayer watcher = f.player(14.5, FLOOR, 6.5);
        Zombie zombie = zombie(helper, 10.5, 12.5);
        lookAt(player, zombie);
        PowerTicker.tick(player);
        seen(player);
        seen(watcher);
        resetHunger(player);
        long start = now(helper);

        press(player, 1);
        Seen heard = seen(player);
        MobEffectInstance webbed = zombie.getEffect(ModEffects.WEBBED);
        check(webbed != null && webbed.getDuration() == SpiderConfig.WEBBED_TICKS.get(), "the zombie is Webbed for " + SpiderConfig.WEBBED_TICKS.get() + " ticks");
        check(isWeb(helper, new BlockPos(10, 2, 12)) && isWeb(helper, new BlockPos(10, 3, 12)), "with web blocks at its feet and at its chest");
        assertNear(exhaustion(player), SpiderConfig.WEB_SHOT_EXHAUSTION.get(), 0.01, "the shot costs its hunger");
        check(data(player).is(Live.CASTING), "his hand is thrust out");
        check(data(player).isReady(1, start), "the shot has no cooldown on the HUD");
        check(heard.heard("web_shot"), "the thwip is heard");
        List<SpiderFxPayload> told = seen(watcher).fx();
        check(told.size() == 1 && told.get(0).kind() == SpiderFxPayload.Kind.SHOT && told.get(0).player() == player.getId() && told.get(0).arg() == zombie.getId(),
                "a watcher is told of the shot at the zombie: " + told);
        double spent = exhaustion(player);

        f.everyTick(12, tick -> {
            PowerTicker.tick(player);
            if (tick == 3) {
                press(player, 1);
                assertNear(exhaustion(player), spent, 1.0E-6, "a second shot inside the hidden timer does nothing");
            }
            if (tick == 5) {
                check(data(player).is(Live.CASTING), "the hand is still out at tick 5");
            }
            if (tick == 6) {
                check(!data(player).is(Live.CASTING), "and back after 6 ticks");
            }
            if (tick == 11) {
                press(player, 1);
                assertNear(exhaustion(player), 2 * spent, 0.01, "then the next shot works");
            }
            return tick == 12;
        });
    }

    @EnhancedTest(template = ARENA, batch = "spider_web_shot_at_nothing_costs_nothing")
    public static void aWebShotAtNothingCostsNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.WEB_SHOT_RANGE, 4);
        ServerPlayer player = spider(f, 10.5, 10.5);
        look(player, 0.0F, 0.0F);
        PowerTicker.tick(player);
        resetHunger(player);
        seen(player);
        press(player, 1);
        assertNear(exhaustion(player), 0.0, 1.0E-9, "no hunger");
        check(!data(player).is(Live.CASTING), "no pose");
        check(data(player).cooldownUntil(1) == 0, "no cooldown");
        check("message.enhanced.web.no_target".equals(seen(player).message()), "he is told there is nothing to web");
        check(countWebs(helper) == 0, "no web anywhere");
        // ... and no timer was started either: a shot at something works at once.
        Zombie zombie = zombie(helper, 10.5, 12.5);
        f.config(SpiderConfig.WEB_SHOT_RANGE, 24);
        lookAt(player, zombie);
        press(player, 1);
        check(zombie.hasEffect(ModEffects.WEBBED), "the next shot works at once");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void aWebShotOnABlockLaysAPatch(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 6.5);
        // A wall of stone to shoot at; in front of it, in the plane of the patch, things that are not free air.
        fill(helper, 8, 2, 12, 12, 6, 12, Blocks.STONE);
        helper.setBlock(new BlockPos(11, 4, 11), Blocks.COBWEB);
        helper.setBlock(new BlockPos(9, 4, 11), Blocks.WATER);
        helper.setBlock(new BlockPos(10, 5, 11), Blocks.GLASS);
        helper.setBlock(new BlockPos(9, 5, 11), Blocks.STONE);
        lookAt(player, abs(helper, 10.5, 4.5, 12.0));
        PowerTicker.tick(player);
        press(player, 1);
        // The patch is the nine cells of the plane z = 11 round (10, 4); five of them were free air.
        int placed = 0;
        for (int x = 9; x <= 11; x++) {
            for (int y = 3; y <= 5; y++) {
                boolean obstacle = (x == 11 && y == 4) || (x == 9 && y == 4) || (x == 10 && y == 5) || (x == 9 && y == 5);
                check(isWeb(helper, new BlockPos(x, y, 11)) == !obstacle, "cell " + x + "," + y + (obstacle ? " is not a web" : " is a web"));
                if (!obstacle) {
                    placed++;
                }
            }
        }
        check(placed == 5, "five of the nine cells were free air: " + placed);
        check(helper.getBlockState(new BlockPos(11, 4, 11)).is(Blocks.COBWEB) && helper.getBlockState(new BlockPos(9, 4, 11)).is(Blocks.WATER)
                && helper.getBlockState(new BlockPos(10, 5, 11)).is(Blocks.GLASS) && helper.getBlockState(new BlockPos(9, 5, 11)).is(Blocks.STONE),
                "the cobweb, the water, the glass and the stone are as they were");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_web_blocks_switch_and_cap_and_vanish", timeoutTicks = 200)
    public static void theWebBlocksSwitchAndCapAndVanish(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        f.config(SpiderConfig.WEB_BLOCK_TICKS, 20);
        f.config(SpiderConfig.WEB_SHOT_COOLDOWN, 0);
        ServerPlayer player = spider(f, 10.5, 6.5);
        fill(helper, 8, 2, 12, 12, 6, 12, Blocks.STONE);
        lookAt(player, abs(helper, 10.5, 4.5, 12.0));
        PowerTicker.tick(player);
        // The switch: with webBlocks off a web only entangles.
        f.config(SpiderConfig.WEB_BLOCKS, false);
        press(player, 1);
        check(countWebs(helper) == 0, "with webBlocks off no web block goes up");
        f.config(SpiderConfig.WEB_BLOCKS, true);
        // The cap: the oldest goes first.
        f.config(SpiderConfig.WEB_BLOCK_CAP, 8);
        f.config(SpiderConfig.WEB_PATCH_RADIUS, 0);
        for (int i = 0; i < 12; i++) {
            lookAt(player, abs(helper, 8.5 + (i % 5), 3.5 + (i / 5), 12.0));
            press(player, 1);
        }
        check(SpiderServer.websStanding(player) == 8, "at most 8 webs stand: " + SpiderServer.websStanding(player));
        check(countWebs(helper) == 8, "and they are all there is: " + countWebs(helper));
        check(!isWeb(helper, new BlockPos(8, 3, 11)), "the first of them is gone");
        check(level.getBlockTicks().hasScheduledTick(abs(helper, 11, 4, 11), ModBlocks.SPIDER_WEB.get()) || isWeb(helper, new BlockPos(11, 4, 11)),
                "the web that stands has its tick scheduled (saved with the chunk)");
        check(SpiderWebs.lifetime(false) == 20 && SpiderWebs.lifetime(true) == 30, "a web lasts its time, and 1.5 times as long when it is organic");
        // They vanish by themselves, and a cell built over meanwhile is left alone.
        BlockPos built = new BlockPos(11, 4, 11);
        BlockPos some = null;
        for (int x = 8; x <= 12 && some == null; x++) {
            for (int y = 3; y <= 5 && some == null; y++) {
                if (isWeb(helper, new BlockPos(x, y, 11))) {
                    some = new BlockPos(x, y, 11);
                }
            }
        }
        check(some != null, "a web to build over");
        BlockPos over = some;
        level.setBlock(helper.absolutePos(over), Blocks.DIRT.defaultBlockState(), 3);
        helper.runAfterDelay(26, () -> {
            check(countWebs(helper) == 0, "after 20 ticks every web is gone: " + countWebs(helper));
            check(helper.getBlockState(over).is(Blocks.DIRT), "and the block built over a web is untouched");
            helper.succeed();
        });
    }

    @EnhancedTest(template = ARENA, batch = "spider_organic_webs_last_longer_and_cost_more")
    public static void organicWebsLastLongerAndCostMore(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.WEB_SHOT_COOLDOWN, 0);
        ServerPlayer player = spider(f, 10.5, 6.5);
        Zombie plain = zombie(helper, 10.5, 12.5);
        lookAt(player, plain);
        PowerTicker.tick(player);
        resetHunger(player);
        press(player, 1);
        int plainTicks = plain.getEffect(ModEffects.WEBBED).getDuration();
        double plainCost = exhaustion(player);
        check(plainTicks == 120, "the homemade suit's web lasts 120 ticks");
        // The Brand New Day look: organic.
        wear(player, "brand_new_day");
        check(SpiderServer.organic(player, data(player)), "the Brand New Day suit's webs are organic");
        Zombie organic = zombie(helper, 14.5, 12.5);
        put(helper, player, 14.5, FLOOR, 6.5);
        lookAt(player, organic);
        resetHunger(player);
        press(player, 1);
        check(organic.getEffect(ModEffects.WEBBED).getDuration() == 180, "organic webs last 1.5 times as long");
        assertNear(exhaustion(player), 2 * plainCost, 0.01, "and cost twice the hunger");
        // The Black Eyes make the webs of any look organic.
        wear(player, "homemade");
        check(!SpiderServer.organic(player, data(player)), "the homemade suit is not organic");
        press(player, 5);
        PowerTicker.tick(player);
        check(SpiderServer.organic(player, data(player)), "but the Black Eyes are");
        Zombie surge = zombie(helper, 6.5, 12.5);
        put(helper, player, 6.5, FLOOR, 6.5);
        lookAt(player, surge);
        resetHunger(player);
        press(player, 1);
        check(surge.getEffect(ModEffects.WEBBED).getDuration() == 180, "the Black Eyes' web is organic too");
        // An empty stomach has nothing left to make webs with, and only organic webs need it.
        player.getFoodData().setFoodLevel(0);
        Zombie hungry = zombie(helper, 2.5, 14.5);
        put(helper, player, 2.5, FLOOR, 8.5);
        lookAt(player, hungry);
        seen(player);
        press(player, 1);
        check(!hungry.hasEffect(ModEffects.WEBBED), "no web on an empty stomach in the Black Eyes");
        check("message.enhanced.web.hungry".equals(seen(player).message()), "he says why");
        data(player).set(Live.BLACK_EYES, false);
        PowerTicker.tick(player);
        press(player, 1);
        check(hungry.hasEffect(ModEffects.WEBBED), "the mechanical shooters of the homemade suit need no food");
        helper.succeed();
    }

    /** A Wither that is told when a web reaches it, as a boss with a hook would be. */
    private static final class HookedWither extends WitherBoss implements BossHooks {
        int shots;

        HookedWither(Level level) {
            super(EntityType.WITHER, level);
            setNoAi(true);
            setInvulnerable(true);
        }

        @Override
        public void onWebShot(ServerPlayer spider) {
            shots++;
        }
    }

    @EnhancedTest(template = ARENA, batch = "spider_bosses_and_spider_man_cannot_be_webbed")
    public static void bossesAndSpiderManCannotBeWebbed(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        f.pvp(true);
        f.config(SpiderConfig.WEB_SHOT_COOLDOWN, 0);
        ServerPlayer player = spider(f, 10.5, 6.5);
        PowerTicker.tick(player);
        HookedWither wither = new HookedWither(level);
        wither.moveTo(abs(helper, 10.5, FLOOR, 12.5));
        level.addFreshEntity(wither);
        lookAt(player, wither);
        press(player, 1);
        check(!wither.hasEffect(ModEffects.WEBBED), "the Wither is never webbed");
        check(wither.shots == 1, "but it is told of the web shot: " + wither.shots);
        check(wither.getType().is(ModTags.WEB_IMMUNE) && EntityType.ENDER_DRAGON.is(ModTags.WEB_IMMUNE) && EntityType.WARDEN.is(ModTags.WEB_IMMUNE),
                "the dragon, the Wither and the Warden are web immune");
        check(!SpiderWebs.canWeb(player, wither), "the rule says so");
        wither.discard();
        ServerPlayer twin = f.player(14.5, FLOOR, 10.5, Power.SPIDER_MAN);
        ServerPlayer creative = f.player(6.5, FLOOR, 10.5, GameType.CREATIVE);
        ServerPlayer survival = f.player(10.5, FLOOR, 14.5);
        for (ServerPlayer target : List.of(twin, creative)) {
            lookAt(player, target);
            press(player, 1);
            check(!target.hasEffect(ModEffects.WEBBED), target.getGameProfile().getName() + " is not webbed");
        }
        lookAt(player, survival);
        press(player, 1);
        check(survival.hasEffect(ModEffects.WEBBED), "a player is webbed when PvP is on");
        survival.removeEffect(ModEffects.WEBBED);
        f.pvp(false);
        lookAt(player, survival);
        press(player, 1);
        check(!survival.hasEffect(ModEffects.WEBBED), "and not when it is off");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_instant_kill_spares_the_minions_and_bosses_of_a_fight")
    public static void instantKillSparesTheMinionsAndBossesOfAFight(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(10.5, FLOOR, 7.5, Power.SPIDER_MAN);
        wear(player, "iron_spider");
        press(player, 4);
        BossTestSupport.TestFight fight = BossFrameworkTests.start(f, BossKind.ULTRON, player,
                BossFrameworkTests.fight(f, BossKind.ULTRON, BossArena.Containment.NONE, true));
        // A marked Husk of the running fight, a Sentry of it, a boss, and a free Zombie that goes for him.
        Husk husk = EntityType.HUSK.create(level);
        husk.moveTo(abs(helper, 10.5, FLOOR, 9.5));
        husk.setNoAi(true);
        fight.spawn(husk);
        var sentry = ModEntities.ULTRON_SENTRY.get().create(level);
        sentry.moveTo(abs(helper, 11.5, FLOOR, 9.5));
        sentry.setNoAi(true);
        fight.spawn(sentry);
        HookedWither wither = new HookedWither(level);
        wither.moveTo(abs(helper, 12.5, FLOOR, 9.5));
        fight.spawn(wither);
        Zombie zombie = zombie(helper, 9.5, 9.5);
        zombie.setTarget(player);
        check(BossFights.isMinion(husk) && BossFights.isMinion(sentry) && !BossFights.isMinion(zombie), "the Husk and the Sentry are minions");

        pressSneaking(player, 4);
        pressSneaking(player, 4);
        check(data(player).is(Live.INSTANT_KILL), "Instant Kill is on");
        float huskHealth = husk.getHealth();
        float sentryHealth = sentry.getHealth();
        float legs = SpiderConfig.LEGS_DAMAGE.get().floatValue();
        PowerTicker.tick(player);
        check(zombie.isDeadOrDying() || zombie.getHealth() <= 0, "a Zombie is killed by a leg");
        check(husk.isAlive() && husk.getHealth() >= huskHealth - legs - 0.01, "a marked Husk takes only the legs' damage: " + husk.getHealth());
        check(sentry.isAlive() && sentry.getHealth() >= sentryHealth - legs - 0.01, "a Sentry takes only the legs' damage: " + sentry.getHealth());
        check(wither.isAlive(), "and a boss is not killed");
        helper.succeed();
    }

    /**
     * Instant Kill takes a hit off a boss's minion at the rhythm of the game's own invulnerability, as the legs' guard
     * does, not at every strike: a leg that resets the minion's invulnerability for every strike (as it does for the
     * creatures it kills) would hit it twice as often, and four legs would clear a boss's army in seconds.
     */
    @EnhancedTest(template = ARENA, batch = "spider_instant_kill_does_not_hasten_the_blows_on_a_minion", timeoutTicks = 100)
    public static void instantKillDoesNotHastenTheBlowsOnAMinionOfAFight(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = spider(f, 10.5, 7.5);
        wear(player, "iron_spider");
        press(player, 4);
        BossTestSupport.TestFight fight = BossFrameworkTests.start(f, BossKind.ULTRON, player,
                BossFrameworkTests.fight(f, BossKind.ULTRON, BossArena.Containment.NONE, true));
        Husk husk = EntityType.HUSK.create(level);
        husk.moveTo(abs(helper, 10.5, FLOOR, 10.5));
        husk.setNoAi(true);
        toughen(husk);
        fight.spawn(husk);
        check(BossFights.isMinion(husk), "the Husk is a minion of the fight");
        pressSneaking(player, 4);
        pressSneaking(player, 4);
        check(data(player).is(Live.INSTANT_KILL), "Instant Kill is on");
        float before = husk.getHealth();
        float legs = SpiderConfig.LEGS_DAMAGE.get().floatValue();
        int strikes = 36 / SpiderConfig.INSTANT_KILL_INTERVAL.get() + 1;
        f.everyTick(36, tick -> {
            PowerTicker.tick(player);
            if (tick < 36) {
                return false;
            }
            float lost = before - husk.getHealth();
            check(lost >= 2 * legs * 0.7F, "the minion did take the legs' blows: " + lost);
            // Vanilla lets a hit through every ten ticks (four of them in 36 ticks); a leg that resets the minion's
            // invulnerability at every strike would land every one of them.
            check(lost <= 5 * legs + 0.01F, "but no more often than the game's own rhythm lets them (" + strikes + " strikes, " + lost + " lost)");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = "spider_no_web_block_goes_up_in_a_protected_arena")
    public static void noWebBlockGoesUpInAProtectedArena(GameTestHelper helper) {
        TestKit.Fixture f = BossFrameworkTests.fixture(helper);
        ServerLevel level = helper.getLevel();
        f.config(SpiderConfig.WEB_SHOT_COOLDOWN, 0);
        ServerPlayer player = f.player(10.5, FLOOR, 7.5, Power.SPIDER_MAN);
        BossFrameworkTests.start(f, BossKind.ULTRON, player, BossFrameworkTests.fight(f, BossKind.ULTRON, BossArena.Containment.NONE, true));
        // The arena protects its cells: no web block goes up inside it, though the web still entangles.
        check(BossFights.protects(level, abs(helper, 10, 2, 12)), "the arena protects its cells");
        Zombie inside = zombie(helper, 10.5, 12.5);
        lookAt(player, inside);
        press(player, 1);
        check(inside.hasEffect(ModEffects.WEBBED), "the web still entangles a creature in the arena");
        check(countWebs(helper) == 0, "but no web block goes up inside a protected arena");
        // ... and a web shot at a block of it lays no patch either.
        look(player, 0.0F, 30.0F);
        press(player, 1);
        check(countWebs(helper) == 0, "nor a patch on a block of it");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void aTornWebBreaksFree(GameTestHelper helper) {
        Zombie zombie = zombie(helper, 10.5, 10.5);
        toughen(zombie);
        zombie.addEffect(new MobEffectInstance(ModEffects.WEBBED, 100));
        DamageSource source = helper.getLevel().damageSources().generic();
        zombie.invulnerableTime = 0;
        zombie.hurt(source, 2.0F);
        check(zombie.getEffect(ModEffects.WEBBED).getDuration() == 100, "a 2 point hit does not tear the web");
        zombie.invulnerableTime = 0;
        zombie.hurt(source, 8.0F);
        check(zombie.getEffect(ModEffects.WEBBED).getDuration() == 80, "an 8 point hit takes 20 ticks off it: " + zombie.getEffect(ModEffects.WEBBED).getDuration());
        zombie.removeEffect(ModEffects.WEBBED);
        zombie.addEffect(new MobEffectInstance(ModEffects.WEBBED, 15));
        zombie.invulnerableTime = 0;
        zombie.hurt(source, 8.0F);
        check(!zombie.hasEffect(ModEffects.WEBBED), "a web with less than 20 ticks left is torn off by a hard blow");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void aWebBurstWebsEveryoneAround(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        List<Zombie> zombies = List.of(zombie(helper, 6.5, 10.5), zombie(helper, 14.5, 12.5), zombie(helper, 9.5, 5.5));
        Villager villager = TestKit.spawnWithNoFreeWill(helper, EntityType.VILLAGER, new Vec3(13.5, FLOOR, 7.5));
        PowerTicker.tick(player);
        resetHunger(player);
        seen(player);
        long start = now(helper);
        pressSneaking(player, 1);
        Seen after = seen(player);
        for (Zombie zombie : zombies) {
            MobEffectInstance web = zombie.getEffect(ModEffects.WEBBED);
            check(web != null && web.getDuration() == SpiderConfig.WEB_BURST_WEBBED_TICKS.get(), "a zombie is webbed for the burst's 80 ticks, once: " + web);
        }
        check(!villager.hasEffect(ModEffects.WEBBED), "the villager is spared (an area attack spares who it should)");
        int webs = countWebs(helper);
        check(webs > 0 && webs <= 24, "up to one web block per strand: " + webs);
        assertNear(exhaustion(player), SpiderConfig.WEB_BURST_EXHAUSTION.get(), 0.01, "the burst costs 2.0");
        check(data(player).cooldownUntil(1) - start == SpiderConfig.WEB_BURST_COOLDOWN.get(), "the cooldown is the burst's 240 ticks");
        check(after.heard("web_burst"), "the whump is heard");
        List<SpiderFxPayload> told = after.fx();
        check(told.size() == 1 && told.get(0).kind() == SpiderFxPayload.Kind.BURST && told.get(0).points().size() == 24, "24 strands are shown: " + told);
        // The Web Shot is still there during the burst's cooldown, the burst itself is not.
        Zombie fresh = zombie(helper, 10.5, 15.5);
        lookAt(player, fresh);
        press(player, 1);
        check(fresh.hasEffect(ModEffects.WEBBED), "a shot is possible during the burst's cooldown");
        resetHunger(player);
        pressSneaking(player, 1);
        assertNear(exhaustion(player), 0.0, 1.0E-6, "a second burst does nothing");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void aWebBurstNeedsSomeoneNearby(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 15.5);
        Zombie far = zombie(helper, 10.5, 2.5);
        Zombie behind = zombie(helper, 12.5, 15.5);
        fill(helper, 11, 2, 14, 11, 5, 16, Blocks.STONE);
        PowerTicker.tick(player);
        resetHunger(player);
        seen(player);
        pressSneaking(player, 1);
        assertNear(exhaustion(player), 0.0, 1.0E-9, "no hunger");
        check(data(player).cooldownUntil(1) == 0, "no cooldown");
        check("message.enhanced.burst.no_target".equals(seen(player).message()), "the message");
        check(!far.hasEffect(ModEffects.WEBBED) && !behind.hasEffect(ModEffects.WEBBED), "nobody is webbed: one is out of range, one behind a wall");
        check(countWebs(helper) == 3 * 0, "and no web is laid");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_chaos_shield_stops_his_webs")
    public static void aChaosShieldStopsHisWebs(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        f.config(SpiderConfig.WEB_SHOT_COOLDOWN, 0);
        ServerPlayer player = spider(f, 10.5, 6.0);
        ServerPlayer wanda = f.player(10.5, FLOOR, 8.5, Power.SCARLET_WITCH);
        PowerData shield = data(wanda);
        shield.raiseChaosShield(now(helper), 0, 30.0F);
        Zombie behind = zombie(helper, 10.5, 12.5);
        lookAt(player, behind);
        PowerTicker.tick(player);
        seen(player);
        press(player, 1);
        check(!behind.hasEffect(ModEffects.WEBBED), "the zombie behind the shield is not webbed");
        assertNear(shield.shieldStrength(), 28.0, 1.0E-4, "the shield paid 2");
        check("message.enhanced.web.shield".equals(seen(player).message()), "he is told the web splats");
        check(countWebs(helper) == 0, "no web block was laid beyond it");
        // A burst: every strand that meets the sphere pays half a point, at most 12 in all. (The shield is 2.5 blocks
        // away, so about a sixth of the 24 strands meet it: the cost is a multiple of half a point, never none.)
        Zombie near = zombie(helper, 7.5, 6.0);
        pressSneaking(player, 1);
        float left = shield.shieldStrength();
        check(near.hasEffect(ModEffects.WEBBED), "the one beside him is webbed");
        check(left < 28.0F - 0.4F && left >= 28.0F - 12.0F - 1.0E-4F, "the burst's strands wore the shield by at most 12, at least 0.5: " + left);
        check(Math.abs((28.0F - left) * 2.0F - Math.round((28.0F - left) * 2.0F)) < 1.0E-3F, "in halves of a point: " + left);
        // With PvP off the web costs the shield nothing.
        f.pvp(false);
        shield.setShieldStrength(30.0F);
        press(player, 1);
        assertNear(shield.shieldStrength(), 30.0, 1.0E-4, "with PvP off the shield pays nothing");
        // A shield that is still forming stops nothing.
        f.pvp(true);
        shield.raiseChaosShield(now(helper), 40, 30.0F);
        press(player, 1);
        check(wanda.hasEffect(ModEffects.WEBBED), "a shield that is still forming stops nothing: the web reaches Wanda herself");
        assertNear(shield.shieldStrength(), 30.0, 1.0E-4, "and costs it nothing");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Zip and swing

    @EnhancedTest(template = ARENA, batch = "spider_zip_needs_a_block_in_range")
    public static void aZipNeedsABlockInRange(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.ZIP_RANGE, 8);
        ServerPlayer player = spider(f, 10.5, 10.5);
        ServerPlayer watcher = f.player(14.5, FLOOR, 10.5);
        look(player, 0.0F, 0.0F);
        PowerTicker.tick(player);
        resetHunger(player);
        seen(player);
        press(player, 2);
        check(!data(player).is(Live.ZIPPING) && data(player).cooldownUntil(2) == 0, "nothing in range: no zip, no cooldown");
        Seen nothing = seen(player);
        check("message.enhanced.zip.no_anchor".equals(nothing.message()), "the message");
        check(nothing.lastEnd() != null && nothing.lastEnd().arg() == 2, "and his client, which may have begun the zip, is told it was refused");
        check(seen(watcher).fx().isEmpty(), "nobody else is told anything");
        assertNear(exhaustion(player), 0.0, 1.0E-9, "no hunger");

        // A pillar 6 blocks off.
        fill(helper, 10, 2, 17, 10, 5, 17, Blocks.STONE);
        lookAt(player, abs(helper, 10.5, 3.5, 17.0));
        seen(watcher);
        seen(player);
        long start = now(helper);
        press(player, 2);
        check(data(player).is(Live.ZIPPING), "he is zipping");
        check(SpiderServer.zipAnchor(player) != null && SpiderServer.zipAnchor(player).distanceTo(abs(helper, 10.5, 3.5, 17.0)) < 0.6,
                "the anchor is where he looked");
        check(SpiderServer.zipStand(player) != null, "and he has a place to end up");
        check(data(player).cooldownUntil(2) - start == SpiderConfig.ZIP_COOLDOWN.get(), "the cooldown is 6 ticks");
        assertNear(exhaustion(player), SpiderConfig.ZIP_EXHAUSTION.get(), 0.01, "a zip costs 0.3");
        Seen mine = seen(player);
        for (Seen s : List.of(mine, seen(watcher))) {
            List<SpiderFxPayload> told = s.fx();
            check(told.size() == 1 && told.get(0).kind() == SpiderFxPayload.Kind.ZIP_START && told.get(0).points().size() == 1,
                    "the zip start goes to him and to a watcher, with its stand point: " + told);
            check(told.get(0).normal().z < -0.5, "on the face that looks at him");
        }
        check(mine.heard("web_zip"), "the zip is heard");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void aZipThroughTheHexIsRefused(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = HexTests.raiseHex(f);
        Hex hex = HexManager.ownedBy(helper.getLevel(), wanda.getUUID()).orElseThrow();
        ServerPlayer player = f.player(1.5, FLOOR, 1.5, Power.SPIDER_MAN);
        // A pillar inside the Hex (a sphere of radius 8 about the middle): its stand point is inside, he is outside.
        fill(helper, 6, 2, 6, 6, 5, 6, Blocks.STONE);
        lookAt(player, abs(helper, 6.0, 3.5, 6.5));
        PowerTicker.tick(player);
        seen(player);
        check(!hex.contains(player.position().add(0, 0.9, 0)), "he stands outside the Hex");
        press(player, 2);
        Seen refused = seen(player);
        check(!data(player).is(Live.ZIPPING), "the zip across the wall is refused");
        check("message.enhanced.zip.hex".equals(refused.message()), "with the Hex's message");
        check(data(player).cooldownUntil(2) == 0, "and no cooldown");
        // His client began the zip on its own prediction: it is told at once that the web never came.
        SpiderFxPayload end = refused.lastEnd();
        check(end != null && end.kind() == SpiderFxPayload.Kind.ZIP_END && end.arg() == 2, "he is told so (a zip end with the reason 'refused'): " + end);
        // A zip that does not cross it is fine: he steps inside, next to the pillar.
        put(helper, player, 8.5, FLOOR, 8.5);
        lookAt(player, abs(helper, 6.5, 3.5, 7.0));
        press(player, 2);
        check(data(player).is(Live.ZIPPING), "a zip inside the Hex is allowed");
        helper.succeed();
    }

    /** One zip, started at a pillar 9 blocks off, for the tests of how it ends. */
    private static ServerPlayer zipping(TestKit.Fixture f, GameTestHelper helper) {
        ServerPlayer player = spider(f, 10.5, 6.5);
        fill(helper, 10, 2, 16, 10, 6, 16, Blocks.STONE);
        lookAt(player, abs(helper, 10.5, 3.5, 16.0));
        PowerTicker.tick(player);
        press(player, 2);
        check(data(player).is(Live.ZIPPING), "he is zipping");
        seen(player);
        return player;
    }

    /** What a client's zip does: the hero moves toward the stand point in steps of 2.5 blocks (never a jump of over 3). */
    private static void zipALittle(GameTestHelper helper, ServerPlayer player, int steps) {
        Vec3 stand = SpiderServer.zipStand(player);
        for (int i = 0; i < steps; i++) {
            Vec3 to = stand.subtract(player.position());
            double length = to.length();
            Vec3 next = length <= 2.5 ? stand : player.position().add(to.scale(2.5 / length));
            player.absMoveTo(next.x, next.y, next.z);
            PowerTicker.tick(player);
            if (!data(player).is(Live.ZIPPING)) {
                return;
            }
        }
    }

    /** The position of a hero on a slow client or a hitching connection comes in steps of several ticks at once. */
    @EnhancedTest(template = ARENA, batch = "spider_zip_hitch", timeoutTicks = 100)
    public static void aHitchDoesNotEndAZip(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.ZIP_COOLDOWN, 0);
        // His position is held for four ticks, then the movement of four ticks arrives at once.
        ServerPlayer held = zipping(f, helper);
        Vec3 stand = SpiderServer.zipStand(held);
        for (int i = 0; i < 4; i++) {
            PowerTicker.tick(held);
        }
        Vec3 toward = stand.subtract(held.position()).normalize().scale(5.5);
        held.absMoveTo(held.getX() + toward.x, held.getY() + toward.y, held.getZ() + toward.z);
        PowerTicker.tick(held);
        check(data(held).is(Live.ZIPPING), "a step of 5.5 blocks after four held ticks is a hitch, not a jump");
        // A step is no more than twice the step before it, and more than that is a jump.
        ServerPlayer slow = zipping(f, helper);
        Vec3 first = SpiderServer.zipStand(slow).subtract(slow.position()).normalize().scale(2.5);
        slow.absMoveTo(slow.getX() + first.x, slow.getY() + first.y, slow.getZ() + first.z);
        PowerTicker.tick(slow);
        slow.absMoveTo(slow.getX() + first.x * 2.0, slow.getY() + first.y * 2.0, slow.getZ() + first.z * 2.0);
        PowerTicker.tick(slow);
        check(data(slow).is(Live.ZIPPING), "a step of twice the step before it is a client that was slow for a tick");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_zip_ends_when_it_should", timeoutTicks = 200)
    public static void aZipEndsWhenItShould(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.ZIP_COOLDOWN, 0);
        // Arrival: he is at the stand point.
        ServerPlayer arrived = zipping(f, helper);
        zipALittle(helper, arrived, 6);
        check(!data(arrived).is(Live.ZIPPING), "on arrival the zip ends");
        SpiderFxPayload end = seen(arrived).lastEnd();
        check(end != null && end.kind() == SpiderFxPayload.Kind.ZIP_END && end.arg() == 0, "with the reason 'arrived': " + end);
        check(SpiderServer.surfaceOf(arrived).touches(), "and he is on the face he arrived at: he can go on to the next wall");
        // The anchor block is broken.
        ServerPlayer broken = zipping(f, helper);
        helper.setBlock(new BlockPos(10, 3, 16), Blocks.AIR);
        PowerTicker.tick(broken);
        check(!data(broken).is(Live.ZIPPING), "a zip whose anchor is gone ends");
        end = seen(broken).lastEnd();
        check(end != null && end.arg() == 6, "with 'anchor lost': " + end);
        // Billy's spell holds him.
        ServerPlayer spellbound = zipping(f, helper);
        spellbound.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100));
        PowerTicker.tick(spellbound);
        check(!data(spellbound).is(Live.ZIPPING), "Billy's spell ends it");
        end = seen(spellbound).lastEnd();
        check(end != null && end.arg() == 3, "blocked: " + end);
        // Telekinesis holds him.
        ServerPlayer wanda = f.player(2.5, FLOOR, 2.5, Power.SCARLET_WITCH);
        ServerPlayer held = zipping(f, helper);
        Telekinesis.hold(wanda, held, Telekinesis.Mode.WANDA);
        PowerTicker.tick(held);
        check(!data(held).is(Live.ZIPPING), "telekinesis ends it");
        Telekinesis.release(wanda, false);
        // Mjolnir pins him.
        ServerPlayer thor = f.player(18.5, FLOOR, 2.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MJOLNIR.get()));
        PowerTicker.tick(thor);
        ServerPlayer pinned = zipping(f, helper);
        f.pvp(true); // (Mjolnir pins a player only with PvP on; the others in this test must not be kicked at as players.)
        thor.setShiftKeyDown(true);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(thor, InteractionHand.MAIN_HAND, pinned));
        thor.setShiftKeyDown(false);
        f.pvp(false);
        check(com.kolodkingal.enhanced.ability.MjolnirPin.isPinned(pinned), "Mjolnir lies on him");
        PowerTicker.tick(pinned);
        check(!data(pinned).is(Live.ZIPPING), "Mjolnir's pin ends it");
        // A teleport of more than 3 blocks.
        ServerPlayer teleported = zipping(f, helper);
        Vec3 at = teleported.position();
        teleported.absMoveTo(at.x + 6.0, at.y, at.z);
        PowerTicker.tick(teleported);
        check(!data(teleported).is(Live.ZIPPING), "a jump of more than 3 blocks ends it");
        end = seen(teleported).lastEnd();
        check(end != null && end.arg() == 1, "(cancelled): " + end);
        // He moved off, away from the stand point (Space cancels a zip: he leaps off).
        ServerPlayer leapt = zipping(f, helper);
        Vec3 stand = SpiderServer.zipStand(leapt);
        leapt.absMoveTo(leapt.getX(), leapt.getY(), leapt.getZ() + 2.5);
        PowerTicker.tick(leapt);
        check(data(leapt).is(Live.ZIPPING), "going toward it, he is zipping");
        leapt.absMoveTo(leapt.getX(), leapt.getY(), leapt.getZ() - 2.5);
        PowerTicker.tick(leapt);
        leapt.absMoveTo(leapt.getX(), leapt.getY(), leapt.getZ() - 2.5);
        PowerTicker.tick(leapt);
        check(!data(leapt).is(Live.ZIPPING), "going away from it, he has let go");
        check(stand != null, "(he had somewhere to go)");
        // Time runs out: no movement at all, so the clock is what ends it.
        ServerPlayer slow = zipping(f, helper);
        long begun = now(helper);
        int max = SpiderConfig.ZIP_MAX_TICKS.get();
        f.everyTick(max + 2, tick -> {
            PowerTicker.tick(slow);
            if (tick < max - 1) {
                check(data(slow).is(Live.ZIPPING), "still zipping at tick " + tick);
                return false;
            }
            if (!data(slow).is(Live.ZIPPING)) {
                SpiderFxPayload ended = seen(slow).lastEnd();
                check(ended != null && ended.arg() == 4, "timed out: " + ended + " after " + (now(helper) - begun) + " ticks");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = QUICK, timeoutTicks = 100)
    public static void zipsChain(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = zipping(f, helper);
        fill(helper, 16, 2, 10, 16, 6, 10, Blocks.STONE);
        Vec3 first = SpiderServer.zipAnchor(player);
        f.everyTick(10, tick -> {
            PowerTicker.tick(player);
            if (tick == 3) {
                press(player, 2);
                check(SpiderServer.zipAnchor(player).distanceTo(first) < 0.01, "a press 3 ticks later is refused by the 6-tick cooldown");
            }
            if (tick == 7) {
                lookAt(player, abs(helper, 16.0, 3.5, 10.5));
                press(player, 2);
                Vec3 second = SpiderServer.zipAnchor(player);
                check(second != null && second.distanceTo(first) > 3.0, "G pressed again while zipping re-anchors at once");
                check(data(player).is(Live.ZIPPING), "and he is still zipping");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void zipMathRampsAndBrakes(GameTestHelper helper) {
        double top = 1.3;
        double factor = 1.15;
        double cap = top * Math.sqrt(factor);
        double last = 0.0;
        for (int tick = 0; tick < 20; tick++) {
            double v = SpiderMath.zipSpeed(tick, 40.0, top, factor);
            check(v <= cap + 1.0E-9, "never over the top speed: " + v);
            if (tick > 0 && v < cap - 1.0E-9) {
                assertNear(v - last, 0.1, 1.0E-9, "it rises 0.1 a tick at tick " + tick);
            }
            last = v;
        }
        assertNear(SpiderMath.zipSpeed(0, 40.0, top, factor), 0.3, 1.0E-9, "it starts at 0.3");
        assertNear(SpiderMath.zipSpeed(3, 40.0, top, factor), 0.6, 1.0E-9, "0.6 at tick 3");
        assertNear(SpiderMath.zipSpeed(7, 40.0, top, factor), 1.0, 1.0E-9, "1.0 at tick 7");
        assertNear(SpiderMath.zipSpeed(10, 40.0, top, factor), 1.3, 1.0E-9, "1.3 at tick 10, still under the top speed of " + cap);
        assertNear(SpiderMath.zipSpeed(30, 40.0, top, factor), cap, 1.0E-9, "and the top speed after that");
        check(SpiderMath.zipSpeed(30, 1.0, top, factor) < 0.5, "under 0.5 at 1 block");
        check(SpiderMath.zipSpeed(30, 3.0, top, factor) < SpiderMath.zipSpeed(30, 10.0, top, factor), "slower nearer the end");
        check(SpiderMath.zipSpeed(30, 10.0, top, 0.0) == 0.0, "no speed factor, no speed (Billy's spell)");

        // The stand point: the first free offset.
        Vec3 anchor = new Vec3(10.0, 4.0, 10.5);
        Vec3 wall = SpiderMath.standAt(spot -> true, anchor, Direction.WEST, 1.8);
        check(wall != null && Math.abs(wall.x - 9.65) < 1.0E-9 && Math.abs(wall.y - (4.0 - 1.2)) < 1.0E-9,
                "on a west face: 0.35 out, the anchor at chest height: " + wall);
        Vec3 ceiling = SpiderMath.standAt(spot -> true, new Vec3(10.5, 6.0, 10.5), Direction.DOWN, 1.8);
        check(ceiling != null && Math.abs(ceiling.y - (6.0 - 1.81)) < 1.0E-9 && ceiling.x == 10.5, "under a ceiling: hanging with his head against it: " + ceiling);
        Vec3 floor = SpiderMath.standAt(spot -> true, new Vec3(10.5, 2.0, 10.5), Direction.UP, 1.8);
        check(floor != null && Math.abs(floor.y - 2.001) < 1.0E-9, "on top of a block: standing on it: " + floor);
        Vec3 next = SpiderMath.standAt(spot -> spot.y > 2.95, anchor, Direction.WEST, 1.8);
        check(next != null && next.y > 2.95, "a wall with a block in the way chooses the next offset: " + next);
        check(SpiderMath.standAt(spot -> false, anchor, Direction.WEST, 1.8) == null, "none that fits: none");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_swing_lasts_while_g_is_held", timeoutTicks = 100)
    public static void aSwingLastsWhileGIsHeld(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 4.5);
        fill(helper, 9, 6, 8, 11, 8, 8, Blocks.STONE);
        put(helper, player, 10.5, 5.0, 4.5);
        lookAt(player, abs(helper, 10.5, 6.5, 8.0));
        PowerTicker.tick(player);
        seen(player);
        hold(player, 2, true);
        check(data(player).is(Live.SWINGING), "Sneak + G starts a swing");
        double rope = SpiderServer.swingRope(player);
        double distance = player.getEyePosition().distanceTo(SpiderServer.swingAnchor(player));
        check(rope >= 4.0 && rope <= SpiderConfig.SWING_RANGE.get() && Math.abs(rope - Math.max(4.0, distance)) < 0.01, "the rope is the distance, at least 4: " + rope);
        List<SpiderFxPayload> told = seen(player).fx();
        check(told.size() == 1 && told.get(0).kind() == SpiderFxPayload.Kind.SWING_START && Math.abs(told.get(0).value() - rope) < 1.0E-3, "he is told, with the rope: " + told);
        release(player, 2);
        check(!data(player).is(Live.SWINGING), "letting go of G ends it");
        SpiderFxPayload end = seen(player).lastEnd();
        check(end != null && end.kind() == SpiderFxPayload.Kind.SWING_END && end.arg() == 5, "with 'released': " + end);
        check(SpiderServer.swingAnchor(player) == null, "and the web is gone");

        // The anchor broken.
        hold(player, 2, true);
        check(data(player).is(Live.SWINGING), "again");
        helper.setBlock(new BlockPos(10, 6, 8), Blocks.AIR);
        PowerTicker.tick(player);
        check(!data(player).is(Live.SWINGING), "a swing whose anchor is gone ends");
        check(seen(player).lastEnd().arg() == 6, "with 'anchor lost'");
        // The clock, and the hunger meanwhile.
        helper.setBlock(new BlockPos(10, 6, 8), Blocks.STONE);
        f.config(SpiderConfig.SWING_MAX_TICKS, 20);
        lookAt(player, abs(helper, 10.5, 6.5, 8.0));
        hold(player, 2, true);
        resetHunger(player);
        f.everyTick(24, tick -> {
            PowerTicker.tick(player);
            if (tick == 10) {
                assertNear(exhaustion(player), 0.1, 0.02, "a swing costs 0.01 a tick: " + exhaustion(player));
            }
            if (tick >= 21) {
                check(!data(player).is(Live.SWINGING), "swingMaxTicks ends it: tick " + tick);
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = QUICK, timeoutTicks = 100)
    public static void aSwingEndsOnTheFloorAndWhenHeLeapsOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 4.5);
        fill(helper, 9, 6, 8, 11, 8, 8, Blocks.STONE);
        put(helper, player, 10.5, 5.0, 4.5);
        lookAt(player, abs(helper, 10.5, 6.5, 8.0));
        PowerTicker.tick(player);
        hold(player, 2, true);
        check(data(player).is(Live.SWINGING), "swinging");
        double rope = SpiderServer.swingRope(player);
        Vec3 anchor = SpiderServer.swingAnchor(player);
        // He leaps off: far from the anchor, beyond what the rope allows.
        player.absMoveTo(anchor.x, anchor.y - rope * 1.25 - 1.0, anchor.z - 0.5);
        PowerTicker.tick(player);
        check(!data(player).is(Live.SWINGING), "a swing he has left far behind is let go");
        check(seen(player).lastEnd().arg() == 1, "(cancelled)");
        // Standing on the floor for 10 ticks (after the first 10) lets go of it too.
        put(helper, player, 10.5, FLOOR, 4.5);
        lookAt(player, abs(helper, 10.5, 6.5, 8.0));
        hold(player, 2, true);
        check(data(player).is(Live.SWINGING), "a new swing");
        f.everyTick(30, tick -> {
            player.setOnGround(true);
            PowerTicker.tick(player);
            if (tick < 19) {
                // (The first 10 ticks on the floor do not count; then 10 more let it go.)
                check(data(player).is(Live.SWINGING), "still swinging at tick " + tick);
                return false;
            }
            if (!data(player).is(Live.SWINGING)) {
                check(seen(player).lastEnd().arg() == 1, "on the floor it is let go");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void swingMathIsAPendulum(GameTestHelper helper) {
        double g = 0.06;
        double length = 8.0;
        Vec3 anchor = new Vec3(0, 20, 0);
        // A small swing with no drag: its period is 2 pi sqrt(L / g), to within 5 percent.
        double angle = 0.15;
        Vec3 pos = anchor.add(Math.sin(angle) * length, -Math.cos(angle) * length, 0);
        Vec3 vel = Vec3.ZERO;
        int first = -1;
        int second = -1;
        double previousX = pos.x - anchor.x;
        for (int tick = 1; tick <= 400; tick++) {
            SpiderMath.Swing step = SpiderMath.swingStep(pos, vel, anchor, length, Vec3.ZERO, g, 1.0, 10.0);
            pos = step.position();
            vel = step.velocity();
            check(pos.distanceTo(anchor) <= length + 1.0E-9, "never further than the rope");
            double x = pos.x - anchor.x;
            if (previousX > 0 && x <= 0) {
                if (first < 0) {
                    first = tick;
                } else if (second < 0) {
                    second = tick;
                }
            }
            previousX = x;
        }
        check(second > 0, "the pendulum came back");
        double period = second - first;
        double expected = 2.0 * Math.PI * Math.sqrt(length / g);
        check(Math.abs(period - expected) / expected < 0.05, "the period is " + period + " ticks, expected about " + expected);

        // With drag the energy only falls (a little numerical room); the speed never passes the cap.
        pos = anchor.add(Math.sin(1.0) * length, -Math.cos(1.0) * length, 0);
        vel = Vec3.ZERO;
        double highest = Double.NEGATIVE_INFINITY;
        double energy = 0.5 * vel.lengthSqr() + g * pos.y;
        for (int tick = 0; tick < 300; tick++) {
            SpiderMath.Swing step = SpiderMath.swingStep(pos, vel, anchor, length, Vec3.ZERO, g, 0.992, 1.8);
            pos = step.position();
            vel = step.velocity();
            check(vel.length() <= 1.8 + 1.0E-9, "the speed never passes the cap");
            double e = 0.5 * vel.lengthSqr() + g * pos.y;
            highest = Math.max(highest, e);
        }
        check(highest <= energy + 0.05, "with drag the energy never rises: " + highest + " from " + energy);
        // A faster swing is capped.
        SpiderMath.Swing fast = SpiderMath.swingStep(anchor.add(0, -length, 0), new Vec3(5.0, 0, 0), anchor, length, Vec3.ZERO, g, 0.992, 1.8);
        check(fast.velocity().length() <= 1.8 + 1.0E-9, "a fast start is capped at the top speed");
        Vec3 kept = new Vec3(1.0, 0.0, 0.0);
        check(SpiderMath.releaseVelocity(kept, false, 1.8).equals(kept), "a release keeps the velocity");
        check(SpiderMath.releaseVelocity(new Vec3(3, 0, 0), false, 1.8).length() <= 1.8 + 1.0E-9, "capped at the top speed");
        Vec3 leap = SpiderMath.releaseVelocity(kept, true, 1.8);
        check(leap.x > 1.0 && leap.y > 0.1, "a leap adds a push");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void theKickCounterIsResetWhileHanging(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = zipping(f, helper);
        ((ServerGamePacketListenerImplAccessor) player.connection).enhanced$setAboveGroundTickCount(50);
        player.fallDistance = 12.0F;
        PowerTicker.tick(player);
        check(((ServerGamePacketListenerImplAccessor) player.connection).enhanced$getAboveGroundTickCount() == 0
                && player.fallDistance == 0.0F, "a zip resets the kick counter, and the fall");
        ServerPlayer swinger = spider(f, 8.5, 4.5);
        fill(helper, 7, 6, 8, 9, 8, 8, Blocks.STONE);
        put(helper, swinger, 8.5, 5.0, 4.5);
        lookAt(swinger, abs(helper, 8.5, 6.5, 8.0));
        PowerTicker.tick(swinger);
        hold(swinger, 2, true);
        ((ServerGamePacketListenerImplAccessor) swinger.connection).enhanced$setAboveGroundTickCount(50);
        swinger.fallDistance = 12.0F;
        PowerTicker.tick(swinger);
        check(((ServerGamePacketListenerImplAccessor) swinger.connection).enhanced$getAboveGroundTickCount() == 0
                && swinger.fallDistance == 0.0F, "a swing does too");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Walls and ceilings

    /** A wall of stone to the east of x = 12 (the cells x = 12, y 2 to 6, z 8 to 12) and a ceiling over x 4 to 8, z 4 to 8. */
    private static void builtSurfaces(GameTestHelper helper) {
        fill(helper, 12, 2, 8, 12, 6, 12, Blocks.STONE);
        fill(helper, 4, 4, 4, 8, 4, 8, Blocks.STONE);
    }

    /** Where a hero stands flush against the wall of {@link #builtSurfaces}, at a height. */
    private static Vec3 besideTheWall(GameTestHelper helper, double y) {
        return abs(helper, 11.69, y, 10.5);
    }

    private static void putBesideTheWall(GameTestHelper helper, ServerPlayer player, double y) {
        Vec3 at = besideTheWall(helper, y);
        player.absMoveTo(at.x, at.y, at.z);
    }

    @EnhancedTest(template = ARENA, batch = CLIMB)
    public static void surfacesAreDetected(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = spider(f, 10.5, 10.5);
        builtSurfaces(helper);
        putBesideTheWall(helper, player, 3.0);
        SpiderClimb.Surface wall = SpiderClimb.detect(level, player, Vec3.ZERO, null);
        check(wall.kind() == SpiderClimb.Kind.WALL && wall.normal() == Direction.WEST, "a wall to his east faces west: " + wall);
        // Head against the underside of a block.
        put(helper, player, 6.5, 2.19, 6.5);
        SpiderClimb.Surface ceiling = SpiderClimb.detect(level, player, Vec3.ZERO, null);
        check(ceiling.kind() == SpiderClimb.Kind.CEILING && ceiling.normal() == Direction.DOWN, "a ceiling: " + ceiling);
        // Open air is none.
        put(helper, player, 10.5, FLOOR, 5.5);
        check(!SpiderClimb.detect(level, player, Vec3.ZERO, null).touches(), "open air is no surface");
        // A cobweb, a ladder and a vine have no collision: never a surface.
        for (Block soft : List.of(Blocks.COBWEB, Blocks.VINE, Blocks.LADDER)) {
            level.setBlock(abs(helper, 13, 2, 5), soft.defaultBlockState(), 3);
            put(helper, player, 12.69, FLOOR, 5.5);
            check(!SpiderClimb.detect(level, player, Vec3.ZERO, null).touches(), soft + " is not a surface");
        }
        level.setBlock(abs(helper, 13, 2, 5), Blocks.STONE.defaultBlockState(), 3);
        check(SpiderClimb.detect(level, player, Vec3.ZERO, null).touches(), "but stone there is");
        // A corner brush: the block's edge barely reaches his side (0.05) is none, a hand's breadth more (0.15) is a wall.
        stone(helper, 17, 2, 5);
        put(helper, player, 16.69, FLOOR, 6.25);
        check(!SpiderClimb.detect(level, player, Vec3.ZERO, null).touches(), "a corner brush of 0.05 is not a surface");
        put(helper, player, 16.69, FLOOR, 6.15);
        check(SpiderClimb.detect(level, player, Vec3.ZERO, null).touches(), "0.15 is");
        // The tag cannot_cling is empty by default (a server may list blocks he may not stick to): with one listed, it is no surface.
        check(!Blocks.STONE.defaultBlockState().is(ModTags.CANNOT_CLING), "no block is in cannot_cling by default");
        level.setBlock(abs(helper, 13, 2, 5), Blocks.RAW_GOLD_BLOCK.defaultBlockState(), 3);
        put(helper, player, 12.69, FLOOR, 5.5);
        check(SpiderClimb.detect(level, player, Vec3.ZERO, null).touches(), "a block of raw gold is a surface");
        listedIn(Blocks.RAW_GOLD_BLOCK, ModTags.CANNOT_CLING, () ->
                check(!SpiderClimb.detect(level, player, Vec3.ZERO, null).touches(), "but not once it is in cannot_cling"));
        check(SpiderClimb.detect(level, player, Vec3.ZERO, null).touches(), "and again when the tag is back to empty");
        // Two walls (a corner): the one he presses into wins, else the one he had.
        fill(helper, 11, 2, 13, 14, 4, 13, Blocks.STONE);
        put(helper, player, 11.69, 3.0, 12.69);
        check(SpiderClimb.detect(level, player, new Vec3(1, 0, 0), null).normal() == Direction.WEST, "pressing east he takes the wall to the east");
        check(SpiderClimb.detect(level, player, new Vec3(0, 0, 1), null).normal() == Direction.NORTH, "pressing south he takes the wall to the south");
        SpiderClimb.Surface previous = new SpiderClimb.Surface(SpiderClimb.Kind.WALL, Direction.NORTH);
        check(SpiderClimb.detect(level, player, Vec3.ZERO, previous).normal() == Direction.NORTH, "not pressing, he keeps the one he had");
        check(SpiderClimb.enters(wall, new Vec3(1, 0, 0), false, false) && !SpiderClimb.enters(wall, new Vec3(1, 0, 0), true, false)
                && SpiderClimb.enters(wall, new Vec3(1, 0, 0), true, true) && !SpiderClimb.enters(wall, Vec3.ZERO, false, true),
                "in the air pressing is enough; on the ground he must jump at it; not pressing is nothing");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void theKeysMoveHimAlongEveryWall(GameTestHelper helper) {
        for (Direction normal : Direction.Plane.HORIZONTAL) {
            SpiderClimb.Surface wall = new SpiderClimb.Surface(SpiderClimb.Kind.WALL, normal);
            Vec3 into = new Vec3(-normal.getStepX(), 0, -normal.getStepZ());
            float yaw = (float) (Math.atan2(-into.x, into.z) * 180.0 / Math.PI);
            check(Math.abs(SpiderClimb.wish(1, 0, yaw).dot(into) - 1.0) < 1.0E-6, "W from yaw " + yaw + " points into the wall " + normal);
            // He looks at the wall, level: W climbs, D goes to the camera's right.
            Vec3 f = SpiderClimb.forwardAxis(wall, into, yaw);
            check(f.y > 0.99 && Math.abs(f.x) < 1.0E-6 && Math.abs(f.z) < 1.0E-6, "looking at the " + normal + " wall W climbs: " + f);
            Vec3 s = SpiderClimb.strafeAxis(wall, into, yaw);
            Vec3 cameraRight = new Vec3(-into.z, 0, into.x);
            check(s.dot(cameraRight) > 0.99, "D goes to the camera's right on the " + normal + " wall: " + s);
            check(SpiderClimb.strafeAxis(wall, into, yaw).scale(-1).dot(cameraRight.scale(-1)) > 0.99, "and A to its left");
            // Looking up at the wall: W climbs. Looking down 45 degrees: W climbs down.
            Vec3 up = new Vec3(into.x * 0.5, 0.866, into.z * 0.5);
            check(SpiderClimb.forwardAxis(wall, up, yaw).y > 0.99, "looking up W climbs");
            Vec3 down = new Vec3(into.x * 0.707, -0.707, into.z * 0.707);
            check(SpiderClimb.forwardAxis(wall, down, yaw).y < -0.99, "looking down 45 degrees W climbs down: " + SpiderClimb.forwardAxis(wall, down, yaw));
            // Looking along the wall: W goes that way.
            Vec3 along = new Vec3(-normal.getStepZ(), 0, normal.getStepX());
            for (double sign : new double[]{1.0, -1.0}) {
                Vec3 lookAlong = along.scale(sign);
                float alongYaw = (float) (Math.atan2(-lookAlong.x, lookAlong.z) * 180.0 / Math.PI);
                Vec3 g = SpiderClimb.forwardAxis(wall, lookAlong, alongYaw);
                check(g.dot(lookAlong) > 0.85, "looking along the " + normal + " wall W goes that way: " + g);
            }
            // At about 27 degrees down, W gives zero, and shrinks smoothly either side of it.
            check(SpiderClimb.forwardAxis(wall, tilted(into, 27.0), yaw).length() < 0.05, "at 27 degrees down W holds him still");
            double before = SpiderClimb.forwardAxis(wall, tilted(into, 25.0), yaw).length();
            double after = SpiderClimb.forwardAxis(wall, tilted(into, 29.0), yaw).length();
            check(before < 0.4 && after < 0.4, "the vector shrinks smoothly through the dead zone: " + before + " / " + after);
            check(SpiderClimb.forwardAxis(wall, tilted(into, 25.0), yaw).y > 0 && SpiderClimb.forwardAxis(wall, tilted(into, 29.0), yaw).y < 0,
                    "and changes its sign through zero, not by a jump");
            check(SpiderClimb.strafeAxis(wall, tilted(into, 27.0), yaw).length() > 0.99, "D is not stalled by the dead zone");
            // The velocity: the speed along the vector, plus the stick into the wall.
            Vec3 v = SpiderClimb.velocity(wall, into, yaw, 1.0F, 0.0F, 0.22);
            check(Math.abs(v.y - 0.22) < 1.0E-6 && Math.abs(v.dot(into) - SpiderClimb.STICK) < 1.0E-6,
                    "W at a speed of 0.22 climbs at 0.22, with the stick into the wall: " + v);
        }
        // A ceiling: W follows his yaw, D is to his right.
        SpiderClimb.Surface ceiling = new SpiderClimb.Surface(SpiderClimb.Kind.CEILING, Direction.DOWN);
        Vec3 east = new Vec3(1, 0, 0);
        Vec3 forward = SpiderClimb.forwardAxis(ceiling, east, EAST);
        check(forward.dot(east) > 0.99 && Math.abs(forward.y) < 1.0E-6, "on a ceiling W walks the way he faces: " + forward);
        check(SpiderClimb.strafeAxis(ceiling, east, EAST).dot(new Vec3(0, 0, 1)) > 0.99, "and D goes to his right (facing east, to the south)");
        helper.succeed();
    }

    /** The horizontal direction {@code into}, tilted down by {@code degrees}. */
    private static Vec3 tilted(Vec3 into, double degrees) {
        double radians = Math.toRadians(degrees);
        return new Vec3(into.x * Math.cos(radians), -Math.sin(radians), into.z * Math.cos(radians));
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void theSpeedFactorScalesEveryMovement(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        PowerTicker.tick(player);
        assertNear(SpiderMath.speedFactor(player), 1.15, 1.0E-6, "a plain Spider-Man is 1.15");
        player.setSprinting(true);
        assertNear(SpiderMath.speedFactor(player), 1.15, 1.0E-6, "and the sprint does not change it");
        player.setSprinting(false);
        press(player, 5);
        PowerTicker.tick(player);
        assertNear(SpiderMath.speedFactor(player), 1.84, 1.0E-6, "with the Black Eyes 1.84");
        press(player, 5);
        PowerTicker.tick(player);
        player.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100));
        assertNear(SpiderMath.speedFactor(player), 0.0, 1.0E-6, "under Spellbound it is zero");
        player.removeEffect(ModEffects.SPELLBOUND);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        check(SpiderMath.speedFactor(player) < 1.15 * 0.7, "Slowness lowers it: " + SpiderMath.speedFactor(player));
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        ServerPlayer plain = f.player(8.5, FLOOR, 8.5);
        plain.addEffect(new MobEffectInstance(ModEffects.WEBBED, 100));
        assertNear(SpiderMath.speedFactor(plain), 0.15, 1.0E-6, "a webbed creature is 0.15");
        // The movement scales by it: the crawl by the factor, the zip and the swing by its square root.
        assertNear(SpiderMath.crawlSpeed(0.22, 1.15, false), 0.22 * 1.15, 1.0E-9, "the crawl");
        assertNear(SpiderMath.crawlSpeed(0.22, 1.15, true), 0.22 * 1.15 * 1.25, 1.0E-9, "faster with the legs");
        assertNear(SpiderMath.zipSpeed(50, 100.0, 1.3, 1.84), 1.3 * Math.sqrt(1.84), 1.0E-9, "the zip by the root");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = CLIMB, timeoutTicks = 100)
    public static void clingingRaisesTheFlagAndResetsFalls(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        builtSurfaces(helper);
        player.setOnGround(false);
        player.zza = 1.0F;
        look(player, EAST, 0.0F);
        player.fallDistance = 5.0F;
        putBesideTheWall(helper, player, 3.5);
        PowerTicker.tick(player);
        check(data(player).is(Live.CLINGING_WALL), "in the air beside a wall, pressing into it: clinging at once");
        check(player.fallDistance == 0.0F, "and the fall counts for nothing");
        // Not while Wall Crawl is off.
        press(player, 3);
        PowerTicker.tick(player);
        check(!data(player).toggle(Toggle.WALL_CRAWL) && !data(player).is(Live.CLINGING_WALL), "with Wall Crawl off he does not cling");
        press(player, 3);
        // The rest follow the game's clock (the flag has a grace of four ticks), one hero tick per game tick: a body that
        // falls past the wall, one that jumps beside it, and one that walks along it.
        ServerPlayer faller = spider(f, 9.5, 9.5);
        faller.setOnGround(false);
        ServerPlayer jumper = spider(f, 8.5, 8.5);
        ServerPlayer walker = spider(f, 7.5, 7.5);
        walker.setOnGround(true);
        double[] fall = {8.0, 7.92, 7.76, 7.52, 7.2, 6.8, 6.3, 5.7, 5.0, 4.2, 3.3};
        double[] jump = {FLOOR, FLOOR, FLOOR, FLOOR + 0.42, FLOOR + 0.76, FLOOR + 1.02, FLOOR + 1.20, FLOOR + 1.30, FLOOR + 1.33, FLOOR + 1.34};
        f.everyTick(fall.length, tick -> {
            // The falling body is in the open for three ticks (its history is of a fall), then drops past the wall.
            if (tick <= 3) {
                put(helper, faller, 9.5, fall[tick - 1], 9.5);
            } else {
                putBesideTheWall(helper, faller, fall[tick - 1]);
            }
            PowerTicker.tick(faller);
            check(!data(faller).is(Live.CLINGING_WALL), "tick " + tick + ": a fall past the wall is not a cling");
            // The jumper stands beside the wall for three ticks, then jumps with the slowing rise of a jump.
            jumper.setOnGround(tick <= 3);
            if (tick <= jump.length) {
                putBesideTheWall(helper, jumper, jump[tick - 1]);
                PowerTicker.tick(jumper);
                check(!data(jumper).is(Live.CLINGING_WALL), "tick " + tick + ": nor is a jump");
            }
            // The walker keeps to the ground.
            putBesideTheWall(helper, walker, FLOOR);
            PowerTicker.tick(walker);
            check(!data(walker).is(Live.CLINGING_WALL), "tick " + tick + ": walking on the ground beside it he does not cling");
            return tick == fall.length;
        });
    }

    /**
     * What his own client says he clings to is the truth for the flag the others see: a crawl down a wall looks like a fall
     * from his positions alone, and a steady climb like the arc of a jump, so a server that only guessed would drop the
     * stance of a hero who climbs or descends. The claim is believed only while a block is there, for a few ticks at a
     * time, and only for a Spider-Man.
     */
    @EnhancedTest(template = ARENA, batch = CLIMB, timeoutTicks = 100)
    public static void whatHisClientSaysHeClingsToIsBelievedOnlyWhereABlockIs(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        builtSurfaces(helper);
        int west = Direction.WEST.get3DDataValue();
        // He crawls steadily down the wall (a fall, to the server's own reckoning), told to be clinging to it.
        ServerPlayer told = spider(f, 10.5, 10.5);
        told.setOnGround(false);
        told.fallDistance = 5.0F;
        SpiderServer.onClingReport(told, SpiderClingPayload.WALL, west);
        // The same crawl with nothing said: the old guess, which takes it for a fall.
        ServerPlayer silent = spider(f, 9.5, 9.5);
        silent.setOnGround(false);
        // Claims where there is nothing to cling to, and a hero who is not Spider-Man.
        ServerPlayer liar = spider(f, 2.5, 8.5);
        liar.setOnGround(false);
        put(helper, liar, 2.5, 3.0, 8.5); // (in the open air, away from the wall and from the roof of the other surface)
        SpiderServer.onClingReport(liar, SpiderClingPayload.WALL, west);
        ServerPlayer roof = spider(f, 2.5, 5.5);
        roof.setOnGround(false);
        put(helper, roof, 2.5, 3.0, 5.5);
        SpiderServer.onClingReport(roof, SpiderClingPayload.CEILING, Direction.DOWN.get3DDataValue());
        ServerPlayer magneto = f.player(6.5, FLOOR, 6.5, Power.MAGNETO);
        SpiderServer.onClingReport(magneto, SpiderClingPayload.WALL, west);
        check(!SpiderServer.hasState(magneto), "the claim of a hero who is not Spider-Man is ignored");
        f.everyTick(26, tick -> {
            double y = Math.max(2.1, 6.0 - 0.22 * (tick - 1));
            if (tick <= 18) {
                putBesideTheWall(helper, told, y);
                if (tick == 8) {
                    SpiderServer.onClingReport(told, SpiderClingPayload.WALL, west); // (his client says it again every few ticks)
                }
                PowerTicker.tick(told);
                check(data(told).is(Live.CLINGING_WALL), "tick " + tick + ": told to be on the wall, he is, however he moves");
                check(told.fallDistance == 0.0F, "tick " + tick + ": and the fall counts for nothing");
                putBesideTheWall(helper, silent, y);
                PowerTicker.tick(silent);
                if (tick >= 8) {
                    check(!data(silent).is(Live.CLINGING_WALL), "tick " + tick + ": a crawl down the wall that nobody speaks for is guessed to be a fall");
                }
            } else {
                // His last word (at tick 8) counts for twelve ticks, and the flag outlives the last of them by four.
                putBesideTheWall(helper, told, 3.0);
                PowerTicker.tick(told);
                check(data(told).is(Live.CLINGING_WALL) == (tick <= 8 + SpiderServer.REPORT_TICKS + 4),
                        "tick " + tick + ": a word that is not renewed runs out (twelve ticks, then the four of grace)");
            }
            PowerTicker.tick(liar);
            check(!data(liar).is(Live.CLINGING_WALL), "tick " + tick + ": a wall that is not there is not clung to, whatever is said");
            PowerTicker.tick(roof);
            check(!data(roof).is(Live.CLINGING_CEILING), "tick " + tick + ": nor is a ceiling");
            return tick == 26;
        });
    }

    @EnhancedTest(template = ARENA, batch = CLIMB, timeoutTicks = 100)
    public static void aClientThatSaysItLetGoDropsTheFlagAtOnce(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        builtSurfaces(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        player.setOnGround(false);
        putBesideTheWall(helper, player, 4.0);
        SpiderServer.onClingReport(player, SpiderClingPayload.WALL, Direction.WEST.get3DDataValue());
        PowerTicker.tick(player);
        check(data(player).is(Live.CLINGING_WALL), "told to cling beside the wall: he does");
        SpiderServer.onClingReport(player, SpiderClingPayload.NONE, 0);
        PowerTicker.tick(player);
        check(!data(player).is(Live.CLINGING_WALL), "told that he let go: the flag is down at once, though he is still beside the wall");
        // And a zip that has just arrived (the server holds it a few ticks) does not outlive his word.
        SpiderServer.onClingReport(player, SpiderClingPayload.WALL, 99); // a normal that is nothing: no surface
        PowerTicker.tick(player);
        check(!data(player).is(Live.CLINGING_WALL), "a surface that makes no sense is no surface");
        // A ceiling is a ceiling, whatever direction is said with it.
        put(helper, player, 6.5, 2.19, 6.5);
        SpiderServer.onClingReport(player, SpiderClingPayload.CEILING, Direction.EAST.get3DDataValue());
        PowerTicker.tick(player);
        check(data(player).is(Live.CLINGING_CEILING) && !data(player).is(Live.CLINGING_WALL), "told to hang from a ceiling, with a block above his head: he does");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void theClingReportRoundTripsAndNearIsAPureCheck(GameTestHelper helper) {
        for (int kind : new int[]{SpiderClingPayload.NONE, SpiderClingPayload.WALL, SpiderClingPayload.CEILING}) {
            for (Direction direction : Direction.values()) {
                SpiderClingPayload sent = new SpiderClingPayload(kind, direction.get3DDataValue());
                ByteBuf buffer = Unpooled.buffer();
                SpiderClingPayload.STREAM_CODEC.encode(buffer, sent);
                check(SpiderClingPayload.STREAM_CODEC.decode(buffer).equals(sent), "the cling report " + sent + " is the same on the other side");
            }
        }
        builtSurfaces(helper);
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        ServerLevel level = helper.getLevel();
        SpiderClimb.Surface wall = new SpiderClimb.Surface(SpiderClimb.Kind.WALL, Direction.WEST);
        putBesideTheWall(helper, player, 3.0);
        check(SpiderClimb.near(level, player, wall, 0.45), "flush with the wall: near");
        put(helper, player, 11.3, 3.0, 10.5);
        check(SpiderClimb.near(level, player, wall, 0.45) && !SpiderClimb.near(level, player, wall, 0.12), "0.4 off the wall: near for the wider reach, not the narrow");
        put(helper, player, 10.5, 3.0, 10.5);
        check(!SpiderClimb.near(level, player, wall, 0.45), "a block's width away: not near");
        check(!SpiderClimb.near(level, player, new SpiderClimb.Surface(SpiderClimb.Kind.WALL, Direction.DOWN), 0.45), "a wall that faces down is no wall");
        check(!SpiderClimb.near(level, player, SpiderClimb.Surface.NONE, 0.45), "nothing is never near");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = CLIMB, timeoutTicks = 100)
    public static void theClingFlagOutlivesLostContactByFourTicks(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        builtSurfaces(helper);
        player.setOnGround(false);
        f.everyTick(12, tick -> {
            if (tick <= 3) {
                putBesideTheWall(helper, player, 3.0);
            } else {
                put(helper, player, 8.5, 3.0, 10.5);
            }
            PowerTicker.tick(player);
            if (tick <= 7) {
                check(data(player).is(Live.CLINGING_WALL), "tick " + tick + ": the flag is up (contact lost at tick 4, 4 ticks of grace)");
            } else {
                check(!data(player).is(Live.CLINGING_WALL), "tick " + tick + ": the flag is gone");
            }
            return tick == 10;
        });
    }

    @EnhancedTest(template = ARENA, batch = CLIMB)
    public static void aCeilingIsClungToInTheAirAndNotWhileFalling(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        builtSurfaces(helper);
        player.setOnGround(false);
        for (int i = 0; i < 3; i++) {
            put(helper, player, 6.5, 2.19, 6.5);
            PowerTicker.tick(player);
        }
        check(data(player).is(Live.CLINGING_CEILING) && !data(player).is(Live.CLINGING_WALL), "head against a ceiling in the air: hanging from it");
        // Falling past it, fast: no.
        ServerPlayer faller = spider(f, 9.5, 9.5);
        faller.setOnGround(false);
        for (double y : new double[]{4.4, 4.4, 4.4, 4.4, 4.0, 3.5, 2.9, 2.19}) {
            put(helper, faller, 6.5, y - 2.0 + 2.0 - 0.0, 6.5);
            PowerTicker.tick(faller);
        }
        check(!data(faller).is(Live.CLINGING_CEILING), "a body that falls is not clinging");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void leapAndMantleRules(GameTestHelper helper) {
        for (Direction normal : Direction.Plane.HORIZONTAL) {
            SpiderClimb.Surface wall = new SpiderClimb.Surface(SpiderClimb.Kind.WALL, normal);
            Vec3 n = wall.normalVector();
            Vec3 along = new Vec3(-normal.getStepZ(), 0.3, normal.getStepX()).normalize();
            for (Vec3 look : new Vec3[]{n.scale(-1), n, along, new Vec3(0, 1, 0), new Vec3(0, -1, 0)}) {
                Vec3 v = SpiderClimb.leap(wall, look, 0.9, 1.15).subtract(0, SpiderClimb.LEAP_LIFT, 0);
                check(Math.abs(v.length() - 0.9 * Math.sqrt(1.15)) < 1.0E-6, "the leap has the speed 0.9 times the root of the factor: " + v.length());
                check(v.normalize().dot(n) >= 0.2 - 1.0E-6, "and points at least 0.2 away from the " + normal + " wall: " + v.normalize().dot(n));
            }
        }
        check(SpiderClimb.RESTICK_DELAY == 6, "he cannot re-stick for 6 ticks");
        check(SpiderClimb.mantles(true, true, 0.3) && SpiderClimb.mantles(true, true, 0.65) && SpiderClimb.mantles(true, true, -0.4),
                "a block's top within 0.65 of the feet: he mantles");
        check(!SpiderClimb.mantles(true, true, 0.8), "a top 0.8 over his feet is too high");
        check(!SpiderClimb.mantles(false, true, 0.3), "not when he was not climbing up");
        check(!SpiderClimb.mantles(true, false, 0.3), "not while he still has contact");
        check(!SpiderClimb.mantles(true, true, Double.NaN), "and not with nothing there");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = CLIMB)
    public static void crawlingCostsHungerOnlyWhileMoving(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        builtSurfaces(helper);
        player.setOnGround(false);
        putBesideTheWall(helper, player, 3.0);
        PowerTicker.tick(player);
        PowerTicker.tick(player);
        check(data(player).is(Live.CLINGING_WALL), "he hangs on the wall");
        resetHunger(player);
        for (int i = 0; i < 5; i++) {
            PowerTicker.tick(player);
        }
        assertNear(exhaustion(player), 0.0, 1.0E-9, "he hangs still: nothing");
        for (int i = 1; i <= 5; i++) {
            putBesideTheWall(helper, player, 3.0 + i * 0.1);
            PowerTicker.tick(player);
        }
        assertNear(exhaustion(player), 5 * SpiderConfig.CRAWL_EXHAUSTION.get(), 1.0E-4, "he moves: 0.005 a tick");
        player.getFoodData().setFoodLevel(0);
        PowerTicker.tick(player);
        check(!data(player).is(Live.CLINGING_WALL), "at 0 food he lets go");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = CLIMB, timeoutTicks = 100)
    public static void blockedHeroesDoNothing(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        fill(helper, 10, 2, 16, 10, 6, 16, Blocks.STONE);
        ServerPlayer spectator = spider(f, 10.5, 10.5);
        spectator.setGameMode(GameType.SPECTATOR);
        lookAt(spectator, abs(helper, 10.5, 3.5, 16.0));
        press(spectator, 2);
        check(!data(spectator).is(Live.ZIPPING), "a spectator's keys do nothing");
        // Each blocker, in turn: what he was doing ends, and his keys do nothing.
        record Blocker(String name, Consumer<ServerPlayer> on) {
        }
        List<Blocker> blockers = List.of(
                new Blocker("Billy's spell", p -> p.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 200))),
                new Blocker("the cocoon of sand", p -> p.addEffect(new MobEffectInstance(ModEffects.ENTOMBED, 200))),
                new Blocker("a flight in Creative", p -> {
                    p.setGameMode(GameType.CREATIVE);
                    p.getAbilities().flying = true;
                }),
                new Blocker("Sokovia's lift", p -> data(p).set(Live.LIFTED, true)));
        for (Blocker blocker : blockers) {
            ServerPlayer player = spider(f, 10.5, 10.5);
            lookAt(player, abs(helper, 10.5, 3.5, 16.0));
            PowerTicker.tick(player);
            press(player, 2);
            check(data(player).is(Live.ZIPPING), "free, he zips");
            blocker.on().accept(player);
            PowerTicker.tick(player);
            check(!data(player).is(Live.ZIPPING), blocker.name() + " ends the zip");
            data(player).startCooldown(2, now(helper) - 100, 0);
            press(player, 2);
            check(!data(player).is(Live.ZIPPING), blocker.name() + ": his keys do nothing");
        }
        // Held by telekinesis, riding.
        ServerPlayer wanda = f.player(8.5, FLOOR, 8.5, Power.SCARLET_WITCH);
        ServerPlayer held = spider(f, 10.5, 10.5);
        Telekinesis.hold(wanda, held, Telekinesis.Mode.WANDA);
        check(Telekinesis.isHeld(held) && SpiderServer.blocked(held, data(held)), "telekinesis holds him: blocked");
        Telekinesis.release(wanda, false);
        check(!SpiderServer.blocked(held, data(held)), "and he is free again");
        var boat = EntityType.BOAT.create(level);
        boat.moveTo(held.position());
        level.addFreshEntity(boat);
        held.startRiding(boat, true);
        check(SpiderServer.blocked(held, data(held)), "riding: blocked");
        held.stopRiding();
        // Under the spell on a wall, the cling holds him in place.
        ServerPlayer frozen = spider(f, 10.5, 10.5);
        builtSurfaces(helper);
        frozen.setOnGround(false);
        putBesideTheWall(helper, frozen, 3.0);
        PowerTicker.tick(frozen);
        check(data(frozen).is(Live.CLINGING_WALL), "he clings");
        frozen.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 200));
        for (int i = 0; i < 6; i++) {
            PowerTicker.tick(frozen);
        }
        check(data(frozen).is(Live.CLINGING_WALL), "Billy's spell freezes him in place on the wall");
        frozen.removeEffect(ModEffects.SPELLBOUND);
        data(frozen).set(Live.LIFTED, true);
        PowerTicker.tick(frozen);
        check(!data(frozen).is(Live.CLINGING_WALL), "but any other hold lets him fall");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Iron Spider: legs and Instant Kill

    /** Batch of its own: Magneto's strip of a player asks PvP, which the test switches on (3.1, D-M 5). */
    @EnhancedTest(template = ARENA, batch = "spider_legs_magneto_pvp")
    public static void legsAreIronSpiderOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        PowerTicker.tick(player);
        seen(player);
        press(player, 4);
        check(!data(player).toggle(Toggle.SPIDER_LEGS) && "message.enhanced.legs.iron_only".equals(seen(player).message()),
                "in the homemade suit B only says that the Iron Spider has legs");
        wear(player, "brand_new_day");
        press(player, 4);
        check(!data(player).toggle(Toggle.SPIDER_LEGS), "nor in the Brand New Day suit");
        wear(player, "iron_spider");
        seen(player);
        press(player, 4);
        check(data(player).toggle(Toggle.SPIDER_LEGS), "in the Iron Spider B puts the legs out");
        check(seen(player).heard("spider_legs"), "with the sound");
        wear(player, "homemade");
        check(!data(player).toggle(Toggle.SPIDER_LEGS), "changing look folds the legs");
        wear(player, "iron_spider");
        press(player, 4);
        check(data(player).toggle(Toggle.SPIDER_LEGS), "out again");
        // With PvP off Magneto's strip leaves another player alone, and says why.
        ServerPlayer magneto = f.player(8.5, FLOOR, 10.5, Power.MAGNETO);
        lookAt(magneto, player);
        seen(magneto);
        press(magneto, 3);
        check(data(player).toggle(Toggle.SPIDER_LEGS) && "message.enhanced.disarm.no_pvp".equals(seen(magneto).message()),
                "with PvP off the strip leaves the legs out: PvP is off");
        // With PvP on it scatters the nanites and folds them for a while.
        f.pvp(true);
        seen(player);
        press(magneto, 3);
        check(!data(player).toggle(Toggle.SPIDER_LEGS), "Magneto's strip folds them");
        seen(player);
        press(player, 4);
        check(!data(player).toggle(Toggle.SPIDER_LEGS) && "message.enhanced.legs.stripped".equals(seen(player).message()),
                "and they will not come out while the nanites are scattered");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_legs_guard_against_threats_only", timeoutTicks = 100)
    public static void legsGuardAgainstThreatsOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = spider(f, 10.5, 10.5);
        wear(player, "iron_spider");
        press(player, 4);
        Zombie hostile = zombie(helper, 12.5, 10.5);
        hostile.setTarget(player);
        toughen(hostile);
        Villager villager = TestKit.spawnWithNoFreeWill(helper, EntityType.VILLAGER, new Vec3(8.5, FLOOR, 10.5));
        IronGolem golem = TestKit.spawnWithNoFreeWill(helper, EntityType.IRON_GOLEM, new Vec3(10.5, FLOOR, 12.5));
        Wolf wolf = TestKit.spawnWithNoFreeWill(helper, EntityType.WOLF, new Vec3(10.5, FLOOR, 8.5));
        wolf.tame(player);
        Cow cow = TestKit.spawnWithNoFreeWill(helper, EntityType.COW, new Vec3(9.5, FLOOR, 9.5));
        f.pvp(true);
        ServerPlayer rival = f.player(11.5, FLOOR, 11.5);
        LokiIllusionEntity illusion = ModEntities.LOKI_ILLUSION.get().create(level);
        illusion.moveTo(abs(helper, 9.5, FLOOR, 11.5));
        level.addFreshEntity(illusion);
        EnderMan neutral = TestKit.spawnWithNoFreeWill(helper, EntityType.ENDERMAN, new Vec3(12.5, FLOOR, 8.5));
        check(SpiderServer.isThreat(player, hostile), "a zombie that goes for him is a threat");
        for (LivingEntity spared : List.of(villager, golem, wolf, cow, rival, illusion)) {
            check(!SpiderServer.isThreat(player, spared), spared.getType() + " is no threat");
        }
        check(!SpiderServer.isThreat(player, neutral), "an Enderman that ignores him is no threat");
        neutral.setTarget(player);
        check(SpiderServer.isThreat(player, neutral), "one that goes for him is");
        neutral.setTarget(null);
        f.config(SpiderConfig.INSTANT_KILL_PLAYERS, true);
        check(SpiderServer.isThreat(player, rival), "a player is a threat once the server allows it, with PvP on");
        f.pvp(false);
        check(!SpiderServer.isThreat(player, rival), "never with PvP off");
        f.config(SpiderConfig.INSTANT_KILL_PLAYERS, false);

        // The guard: a jab of 5 every 20 ticks at the nearest threat, and nobody else is touched.
        float before = hostile.getHealth();
        float others = villager.getHealth() + golem.getHealth() + cow.getHealth() + rival.getHealth();
        PowerTicker.tick(player);
        float legs = SpiderConfig.LEGS_DAMAGE.get().floatValue();
        check(hostile.getHealth() <= before - legs + 0.01, "the zombie is jabbed for the legs' damage: " + before + " -> " + hostile.getHealth());
        check(villager.getHealth() + golem.getHealth() + cow.getHealth() + rival.getHealth() == others && wolf.isAlive(), "and nobody else");
        float afterOne = hostile.getHealth();
        f.everyTick(25, tick -> {
            hostile.invulnerableTime = 0;
            PowerTicker.tick(player);
            if (tick == 10) {
                check(hostile.getHealth() == afterOne, "no second jab inside 20 ticks");
            }
            if (tick == 21) {
                check(hostile.getHealth() <= afterOne - legs + 0.01, "the next jab after the interval: " + hostile.getHealth());
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "spider_instant_kill_needs_a_confirmation", timeoutTicks = 200)
    public static void instantKillNeedsAConfirmation(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        PowerTicker.tick(player);
        seen(player);
        pressSneaking(player, 4);
        check(!data(player).is(Live.INSTANT_KILL) && "message.enhanced.legs.iron_only".equals(seen(player).message()), "not in the homemade suit");
        wear(player, "iron_spider");
        seen(player);
        pressSneaking(player, 4);
        check(!data(player).is(Live.INSTANT_KILL) && "message.enhanced.instant_kill.legs_first".equals(seen(player).message()), "legs out first");
        press(player, 4);
        seen(player);
        pressSneaking(player, 4);
        check(!data(player).is(Live.INSTANT_KILL), "the first press only arms it");
        check("message.enhanced.instant_kill.confirm".equals(seen(player).message()), "and says to press again");
        long armed = now(helper);
        pressSneaking(player, 4);
        Seen on = seen(player);
        check(data(player).is(Live.INSTANT_KILL), "the second press inside the window turns it on");
        check(data(player).cooldownUntil(4) - armed == SpiderConfig.INSTANT_KILL_MAX_TICKS.get(), "the end shows in cooldownUntil(4)");
        check(on.heard("instant_kill_on"), "with its alert");
        pressSneaking(player, 4);
        check(!data(player).is(Live.INSTANT_KILL), "Sneak + B again ends it");
        check(data(player).cooldownUntil(4) - now(helper) == SpiderConfig.INSTANT_KILL_COOLDOWN.get(), "the cooldown is 30 seconds");
        seen(player);
        pressSneaking(player, 4);
        check(!data(player).is(Live.INSTANT_KILL) && "message.enhanced.instant_kill.recharging".equals(seen(player).message()), "while it recharges it is refused");
        // The window: a second press after it must be confirmed again.
        data(player).startCooldown(4, now(helper) - 1000, 0);
        f.config(SpiderConfig.INSTANT_KILL_CONFIRM_TICKS, 20);
        pressSneaking(player, 4);
        check(!data(player).is(Live.INSTANT_KILL), "armed");
        f.everyTick(30, tick -> {
            if (tick == 25) {
                pressSneaking(player, 4);
                check(!data(player).is(Live.INSTANT_KILL), "after the window it must be confirmed again");
                pressSneaking(player, 4);
                check(data(player).is(Live.INSTANT_KILL), "and then it is on");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "spider_instant_kill_kills_threats_and_spares_the_rest", timeoutTicks = 200)
    public static void instantKillKillsThreatsAndSparesTheRest(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = spider(f, 10.5, 10.5);
        wear(player, "iron_spider");
        press(player, 4);
        pressSneaking(player, 4);
        pressSneaking(player, 4);
        check(data(player).is(Live.INSTANT_KILL), "Instant Kill is on");
        List<Zombie> zombies = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Zombie zombie = zombie(helper, 8.5 + i * 0.9, 12.5);
            zombie.setTarget(player);
            zombies.add(zombie);
        }
        Villager villager = TestKit.spawnWithNoFreeWill(helper, EntityType.VILLAGER, new Vec3(10.5, FLOOR, 8.5));
        HookedWither wither = new HookedWither(level);
        wither.moveTo(abs(helper, 14.5, FLOOR, 10.5));
        level.addFreshEntity(wither);
        float witherHealth = wither.getHealth();
        f.pvp(true);
        ServerPlayer rival = f.player(6.5, FLOOR, 10.5);
        resetHunger(player);
        PowerTicker.tick(player);
        long dead = zombies.stream().filter(z -> !z.isAlive() || z.getHealth() <= 0).count();
        check(dead == 4, "four zombies die at the first strike, one leg each: " + dead);
        check(villager.isAlive() && villager.getHealth() == villager.getMaxHealth(), "a villager lives");
        check(rival.getHealth() == rival.getMaxHealth(), "a player takes nothing unless instantKillPlayers");
        check(wither.getHealth() >= witherHealth - SpiderConfig.LEGS_DAMAGE.get() - 0.01 && wither.isAlive(), "a boss takes only the legs' damage");
        assertNear(exhaustion(player), SpiderConfig.INSTANT_KILL_EXHAUSTION.get() + SpiderConfig.LEGS_EXHAUSTION.get(), 0.01, "and the hunger goes");
        f.everyTick(12, tick -> {
            PowerTicker.tick(player);
            if (tick == SpiderConfig.INSTANT_KILL_INTERVAL.get() + 1) {
                long now = zombies.stream().filter(z -> !z.isAlive() || z.getHealth() <= 0).count();
                check(now == 5, "the fifth one the next time: " + now);
            }
            return tick == 12;
        });
    }

    @EnhancedTest(template = ARENA, batch = "spider_instant_kill_ends_when_it_should", timeoutTicks = 100)
    public static void instantKillEndsWhenItShould(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.INSTANT_KILL_MAX_TICKS, 20);
        ServerPlayer player = spider(f, 10.5, 10.5);
        wear(player, "iron_spider");
        press(player, 4);
        pressSneaking(player, 4);
        pressSneaking(player, 4);
        check(data(player).is(Live.INSTANT_KILL), "on");
        player.getFoodData().setFoodLevel(0);
        PowerTicker.tick(player);
        check(!data(player).is(Live.INSTANT_KILL), "at 0 food it ends");
        check(data(player).cooldownUntil(4) > now(helper), "and the cooldown starts");
        player.getFoodData().setFoodLevel(20);
        data(player).startCooldown(4, now(helper) - 1000, 0);
        pressSneaking(player, 4);
        pressSneaking(player, 4);
        check(data(player).is(Live.INSTANT_KILL), "on again");
        player.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100));
        PowerTicker.tick(player);
        check(!data(player).is(Live.INSTANT_KILL), "when he is blocked it ends");
        player.removeEffect(ModEffects.SPELLBOUND);
        data(player).startCooldown(4, now(helper) - 1000, 0);
        pressSneaking(player, 4);
        pressSneaking(player, 4);
        check(data(player).is(Live.INSTANT_KILL), "on once more");
        f.everyTick(25, tick -> {
            PowerTicker.tick(player);
            if (tick < 20) {
                check(data(player).is(Live.INSTANT_KILL), "still on at tick " + tick);
                return false;
            }
            check(!data(player).is(Live.INSTANT_KILL), "after instantKillMaxTicks it ends: tick " + tick);
            check(data(player).cooldownUntil(4) >= now(helper), "then the cooldown");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = "spider_chaos_shield_stops_a_legs_strike")
    public static void aChaosShieldStopsALegsStrike(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer player = spider(f, 10.5, 6.5);
        wear(player, "iron_spider");
        press(player, 4);
        ServerPlayer wanda = f.player(10.5, FLOOR, 8.5, Power.SCARLET_WITCH);
        data(wanda).raiseChaosShield(now(helper), 0, 30.0F);
        Zombie zombie = zombie(helper, 10.5, 9.9);
        zombie.setTarget(player);
        toughen(zombie);
        // The zombie is inside the sphere of the shield (radius 1.9 round Wanda), he is outside: the leg is stopped.
        PowerTicker.tick(player);
        check(zombie.getHealth() == 1000.0F, "a leg does not get through the shield");
        assertNear(data(wanda).shieldStrength(), 25.0, 1.0E-4, "and the shield pays the strike, capped at 5");
        helper.succeed();
    }

    /** Batch of its own: Magneto's strip of a player asks PvP, which the test switches on (3.1, D-M 5). */
    @EnhancedTest(template = ARENA, batch = "spider_magneto_pvp", timeoutTicks = 300)
    public static void magnetoCanLiftAndStripTheIronSpiderOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        PowerTicker.tick(player);
        for (String look : List.of("homemade", "brand_new_day", "own")) {
            wear(player, look);
            check(!Telekinesis.isMagnetic(player), "the " + look + " look is cloth: Magneto cannot lift it");
        }
        wear(player, "iron_spider");
        check(Telekinesis.isMagnetic(player), "the Iron Spider is metal");
        check(SpiderServer.isIronSpider(player) && !SpiderServer.isStripped(player), "an Iron Spider, not yet stripped");
        press(player, 4);
        pressSneaking(player, 4);
        pressSneaking(player, 4);
        PowerTicker.tick(player);
        check(data(player).is(Live.INSTANT_KILL) && armor(player) == 14.0, "with the legs out, Instant Kill on and his armor");
        ServerPlayer magneto = f.player(8.5, FLOOR, 10.5, Power.MAGNETO);
        lookAt(magneto, player);
        seen(magneto);
        press(magneto, 3);
        check("message.enhanced.disarm.no_pvp".equals(seen(magneto).message()) && !SpiderServer.isStripped(player)
                && Powers.get(magneto).isReady(3, now(helper)), "with PvP off the strip takes nothing from a player, and says why");
        f.pvp(true);
        press(magneto, 3);
        check(!"message.enhanced.disarm.no_metal".equals(seen(magneto).message()), "Magneto found metal to strip");
        PowerTicker.tick(player);
        check(SpiderServer.isStripped(player), "the nanites are scattered");
        check(armor(player) == 0.0 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0.0 && player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.0,
                "the suit's armor, toughness and knockback resistance are off");
        check(!data(player).toggle(Toggle.SPIDER_LEGS) && !data(player).is(Live.INSTANT_KILL), "the legs fold and Instant Kill ends");
        f.everyTick(205, tick -> {
            if (tick == 199) {
                PowerTicker.tick(player);
                check(SpiderServer.isStripped(player) && armor(player) == 0.0, "after 199 ticks it is not over yet");
            }
            if (tick == 201) {
                PowerTicker.tick(player);
                check(!SpiderServer.isStripped(player) && armor(player) == 14.0, "after 10 seconds the suit is back: " + armor(player));
                return true;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Black Eyes and spider-sense

    @EnhancedTest(template = ARENA, batch = "spider_black_eyes_boost_and_drain", timeoutTicks = 300)
    public static void theBlackEyesBoostAndDrain(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.BLACK_EYES_MAX_TICKS, 40);
        ServerPlayer player = spider(f, 10.5, 10.5);
        PowerTicker.tick(player);
        player.getFoodData().setFoodLevel(5);
        seen(player);
        press(player, 5);
        check(!data(player).is(Live.BLACK_EYES) && "message.enhanced.black_eyes.hungry".equals(seen(player).message()), "food below 6 refuses");
        player.getFoodData().setFoodLevel(20);
        long start = now(helper);
        press(player, 5);
        Seen on = seen(player);
        PowerTicker.tick(player);
        check(data(player).is(Live.BLACK_EYES), "N turns the Black Eyes on");
        check(data(player).cooldownUntil(5) - start == 40, "the end shows in cooldownUntil(5)");
        assertNear(player.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.1 * 1.15 * 1.6, 1.0E-6, "speed 1.15 x 1.6");
        assertNear(player.getAttributeValue(Attributes.ATTACK_DAMAGE), 1.0 + 2.0 + 4.0, 1.0E-6, "strength +4 on top of his +2");
        assertNear(player.getAttributeValue(Attributes.ATTACK_SPEED), 4.0 * 1.5, 1.0E-6, "attack speed +50 percent");
        assertNear(player.getAttributeValue(Attributes.JUMP_STRENGTH), 0.42 * (1 + 0.30 + 0.25), 1.0E-6, "jump +25 percent");
        assertNear(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.3, 1.0E-6, "knockback resistance 0.3");
        check(SpiderServer.organic(player, data(player)), "and organic webs in the homemade look");
        check(on.heard("black_eyes_on"), "the heartbeat is heard");
        resetHunger(player);
        PowerTicker.tick(player);
        assertNear(exhaustion(player), SpiderConfig.BLACK_EYES_EXHAUSTION.get(), 1.0E-4, "hunger 0.05 a tick");
        // N again calms it: no crash, half the cooldown.
        long calmAt = now(helper);
        press(player, 5);
        check(!data(player).is(Live.BLACK_EYES), "N again ends it");
        check(!player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && !player.hasEffect(MobEffects.WEAKNESS), "with no crash");
        check(data(player).cooldownUntil(5) - calmAt == SpiderConfig.BLACK_EYES_COOLDOWN.get() / 2, "and half the cooldown");
        PowerTicker.tick(player);
        check(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Enhanced.id("spider_eyes_speed")) == null, "the modifiers are gone");
        seen(player);
        press(player, 5);
        check(!data(player).is(Live.BLACK_EYES) && "message.enhanced.black_eyes.recharging".equals(seen(player).message()), "recharging refuses");
        // The clock runs out: a crash with the full cooldown.
        data(player).startCooldown(5, now(helper) - 1000, 0);
        press(player, 5);
        f.everyTick(45, tick -> {
            PowerTicker.tick(player);
            if (tick < 40) {
                return false;
            }
            check(!data(player).is(Live.BLACK_EYES), "after blackEyesMaxTicks it ends");
            check(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && player.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == 1
                    && player.hasEffect(MobEffects.WEAKNESS), "in a crash: Slowness II and Weakness");
            check(player.getEffect(MobEffects.WEAKNESS).getDuration() > 150, "for 10 seconds");
            check(data(player).cooldownUntil(5) - now(helper) > SpiderConfig.BLACK_EYES_COOLDOWN.get() - 10, "and the full cooldown");
            PowerTicker.tick(player); // (the body follows the state one tick later)
            check(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Enhanced.id("spider_eyes_speed")) == null, "the modifiers vanish");
            return true;
        });
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void theBlackEyesEndAtZeroFoodAndCostNothingInCreative(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        PowerTicker.tick(player);
        press(player, 5);
        check(data(player).is(Live.BLACK_EYES), "on");
        player.getFoodData().setFoodLevel(0);
        PowerTicker.tick(player);
        check(!data(player).is(Live.BLACK_EYES) && player.hasEffect(MobEffects.WEAKNESS), "at 0 food it ends in a crash");
        ServerPlayer creative = f.player(8.5, FLOOR, 8.5, GameType.CREATIVE);
        Powers.setPower(creative, Power.SPIDER_MAN);
        creative.getFoodData().setFoodLevel(3);
        PowerTicker.tick(creative);
        press(creative, 5);
        check(data(creative).is(Live.BLACK_EYES), "in Creative the surge starts with an empty stomach");
        resetHunger(creative);
        PowerTicker.tick(creative);
        assertNear(exhaustion(creative), 0.0, 1.0E-9, "and costs nothing");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_black_eyes_can_switch_on_by_themselves", timeoutTicks = 200)
    public static void theBlackEyesCanSwitchOnByThemselves(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        PowerTicker.tick(player);
        player.setHealth(5.0F);
        PowerTicker.tick(player);
        check(!data(player).is(Live.BLACK_EYES), "with blackEyesPanicHealth at 0 they never do");
        f.config(SpiderConfig.BLACK_EYES_PANIC_HEALTH, 6.0);
        player.setHealth(10.0F);
        PowerTicker.tick(player);
        check(!data(player).is(Live.BLACK_EYES), "while he is healthy they stay off");
        player.setHealth(5.0F);
        PowerTicker.tick(player);
        check(data(player).is(Live.BLACK_EYES), "hurt down to 6 hit points they switch on by themselves");
        press(player, 5);
        check(data(player).is(Live.BLACK_EYES), "and cannot be calmed at once");
        f.everyTick(105, tick -> {
            PowerTicker.tick(player);
            if (tick == 101) {
                press(player, 5);
                check(!data(player).is(Live.BLACK_EYES), "after 5 seconds they can");
                return true;
            }
            return false;
        });
    }

    @EnhancedTest(template = ARENA, batch = "spider_spider_sense_warns_of_an_arrow_once_and_to_him_alone", timeoutTicks = 100)
    public static void spiderSenseWarnsOfAnArrowOnceAndToHimAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        f.config(SpiderConfig.SPIDER_SENSE_INTERVAL, 1);
        ServerPlayer player = spider(f, 10.5, 10.5);
        ServerPlayer watcher = f.player(12.5, FLOOR, 12.5);
        PowerTicker.tick(player);
        seen(player);
        seen(watcher);
        Vec3 from = abs(helper, 10.5, 3.0, 5.0);
        Arrow arrow = new Arrow(level, from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
        arrow.setDeltaMovement(0.0, 0.0, 0.05);
        arrow.setNoGravity(true);
        level.addFreshEntity(arrow);
        f.everyTick(60, tick -> {
            // The arrow crawls toward him (0.05 a tick: it would take 100 ticks, so inside 25 only when close enough).
            arrow.setPos(from.x, from.y, from.z + (tick <= 1 ? 0.0 : 0.1 * tick));
            arrow.setDeltaMovement(0.0, 0.0, 0.5);
            PowerTicker.tick(player);
            if (tick == 1) {
                check(data(player).is(Live.SENSING), "an arrow flying at him: he senses it");
                List<SpiderSensePayload> told = seen(player).senses();
                check(told.size() == 1 && told.get(0).ids().contains(arrow.getId()), "and he alone is told which: " + told);
                check(seen(watcher).senses().isEmpty(), "nobody else is told");
            } else if (tick < 40) {
                check(seen(player).senses().isEmpty(), "and not twice within 40 ticks (tick " + tick + ")");
            }
            if (tick == 13) {
                check(!data(player).is(Live.SENSING), "the pose lasts 12 ticks");
            }
            return tick == 39;
        });
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void spiderSenseIgnoresWhatCannotHurtHim(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        // His own arrow, an arrow flying past, an arrow stuck in the ground, an arrow flying away: nothing; and, as the
        // control that the scan sees anything at all, an arrow of someone else's flying straight at him. (A hero scans on
        // his first tick and then every few: each case has a hero of its own, who has not ticked before the arrow flies.)
        for (int kind = 0; kind < 5; kind++) {
            ServerPlayer calm = spider(f, 4.5, 4.5 + kind * 4.0);
            seen(calm);
            Vec3 from = abs(helper, 1.5, 3.0, 4.5 + kind * 4.0);
            Arrow other = new Arrow(level, from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
            other.setNoGravity(true);
            switch (kind) {
                case 0 -> {
                    other.setOwner(calm);
                    other.setDeltaMovement(0.6, 0, 0);
                }
                case 1 -> other.setDeltaMovement(0.6, 0, 3.0);
                case 2 -> other.setDeltaMovement(Vec3.ZERO);
                case 3 -> other.setDeltaMovement(-0.6, 0, 0);
                default -> other.setDeltaMovement(0.6, 0, 0);
            }
            level.addFreshEntity(other);
            PowerTicker.tick(calm);
            if (kind < 4) {
                check(!data(calm).is(Live.SENSING) && seen(calm).senses().isEmpty(), "case " + kind + " (own, passing, stuck, away) is no threat");
            } else {
                check(data(calm).is(Live.SENSING), "the control: an arrow of someone else's flying at him is sensed");
            }
            other.discard();
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = "spider_spider_sense_warns_of_fuses_and_strikes")
    public static void spiderSenseWarnsOfFusesAndStrikes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        // A swelling creeper.
        ServerPlayer one = spider(f, 4.5, 4.5);
        Creeper creeper = TestKit.spawnWithNoFreeWill(helper, EntityType.CREEPER, new Vec3(7.5, FLOOR, 4.5));
        creeper.setSwellDir(1);
        PowerTicker.tick(one);
        check(data(one).is(Live.SENSING), "a swelling creeper is sensed");
        // Primed TNT with a short fuse.
        ServerPlayer two = spider(f, 4.5, 9.5);
        var tnt = EntityType.TNT.create(level);
        tnt.setFuse(30);
        tnt.moveTo(abs(helper, 8.5, FLOOR, 9.5));
        level.addFreshEntity(tnt);
        PowerTicker.tick(two);
        check(data(two).is(Live.SENSING), "primed TNT is sensed");
        tnt.discard();
        // A zombie that goes for him and is about to strike.
        ServerPlayer three = spider(f, 14.5, 8.5);
        Zombie zombie = zombie(helper, 16.5, 8.5);
        zombie.setTarget(three);
        PowerTicker.tick(three);
        check(!data(three).is(Live.SENSING), "a zombie that is not attacking yet is not");
        // (He scans every few ticks, and no game time passes in here: a second hero stands where the first did.)
        ServerPlayer again = spider(f, 14.5, 8.5);
        zombie.setTarget(again);
        zombie.setAggressive(true);
        PowerTicker.tick(again);
        check(data(again).is(Live.SENSING), "one that is, is");
        // A player about to hit him, with PvP on.
        f.pvp(true);
        ServerPlayer four = spider(f, 14.5, 12.5);
        ServerPlayer rival = f.player(14.5, FLOOR, 14.5);
        lookAt(rival, four);
        PowerTicker.tick(four);
        check(!data(four).is(Live.SENSING), "a player who does not swing is no threat");
        rival.swinging = true;
        ServerPlayer fourAgain = spider(f, 14.5, 12.5); // (a fresh scan: no game time has passed for the first)
        PowerTicker.tick(fourAgain);
        check(data(fourAgain).is(Live.SENSING), "a player swinging at him is");
        // The toggle off sends nothing.
        ServerPlayer five = spider(f, 10.5, 17.5);
        press(five, 3);
        pressSneaking(five, 3);
        check(!data(five).toggle(Toggle.SPIDER_SENSE), "Sneak + V switches the sense off");
        Creeper another = TestKit.spawnWithNoFreeWill(helper, EntityType.CREEPER, new Vec3(12.5, FLOOR, 17.5));
        another.setSwellDir(1);
        seen(five);
        PowerTicker.tick(five);
        check(seen(five).senses().isEmpty() && !data(five).is(Live.SENSING), "with the sense off nothing is sent");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void theBlackEyesDoubleTheRangeOfTheSense(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 2.5);
        Creeper far = TestKit.spawnWithNoFreeWill(helper, EntityType.CREEPER, new Vec3(10.5, FLOOR, 2.5 + 11.0));
        far.setSwellDir(1);
        check(SpiderSense.threats(player, 12.0, 1.0).stream().noneMatch(t -> t.entity() == far), "a swelling creeper 11 blocks away is outside the range of 7");
        check(SpiderSense.threats(player, 24.0, 2.0).stream().anyMatch(t -> t.entity() == far), "but inside the doubled range of the Black Eyes");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Lifecycle

    @EnhancedTest(template = ARENA, batch = "spider_losing_the_power_cleans_everything", timeoutTicks = 100)
    public static void losingThePowerCleansEverything(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(SpiderConfig.WEB_SHOT_COOLDOWN, 0);
        ServerPlayer player = spider(f, 10.5, 6.5);
        wear(player, "iron_spider");
        press(player, 4);
        press(player, 5);
        PowerTicker.tick(player);
        Zombie zombie = zombie(helper, 12.5, 6.5);
        lookAt(player, zombie);
        press(player, 1);
        fill(helper, 10, 2, 16, 10, 6, 16, Blocks.STONE);
        lookAt(player, abs(helper, 10.5, 3.5, 16.0));
        press(player, 2);
        check(SpiderServer.hasState(player) && data(player).is(Live.ZIPPING) && data(player).is(Live.BLACK_EYES), "he is in the middle of it all");
        check(SpiderServer.websTracked(player) > 0, "with webs tracked");
        seen(player);
        Powers.setPower(player, Power.NONE);
        for (Live live : Live.values()) {
            check(!data(player).is(live), "the state " + live + " is cleared");
        }
        check(!SpiderServer.hasState(player), "the server forgot him: state and web list");
        check(seen(player).fx().stream().anyMatch(p -> p.kind() == SpiderFxPayload.Kind.ZIP_END), "the zip was ended for the clients");
        noModifiersOfHis(player);
        check(!data(player).toggle(Toggle.WALL_CRAWL) && !data(player).toggle(Toggle.SPIDER_LEGS), "and his toggles are those of no power");
        Powers.setPower(player, Power.SPIDER_MAN);
        check(data(player).toggle(Toggle.WALL_CRAWL) && data(player).toggle(Toggle.SPIDER_SENSE), "a new Spider-Man starts with the defaults");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void deathLogoutAndDimensionEndEverything(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (String how : List.of("death", "logout", "dimension")) {
            ServerPlayer player = spider(f, 10.5, 6.5);
            wear(player, "iron_spider");
            press(player, 5);
            PowerTicker.tick(player);
            fill(helper, 10, 2, 16, 10, 6, 16, Blocks.STONE);
            lookAt(player, abs(helper, 10.5, 3.5, 16.0));
            press(player, 2);
            check(data(player).is(Live.ZIPPING) && data(player).is(Live.BLACK_EYES), how + ": he is zipping with the Black Eyes");
            switch (how) {
                case "death" -> player.kill();
                case "logout" -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
                default -> NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(player, Level.OVERWORLD, Level.NETHER));
            }
            check(!data(player).is(Live.ZIPPING) && !data(player).is(Live.BLACK_EYES), how + " ends the zip and the surge");
            check(!SpiderServer.hasState(player), how + ": the state is gone");
            check(Powers.is(player, Power.SPIDER_MAN) && Powers.variant(player).id().equals("iron_spider"), how + ": the power and the look remain");
            check(data(player).toggle(Toggle.WALL_CRAWL) && data(player).toggle(Toggle.SPIDER_SENSE), how + ": and the toggles");
            if (player.isAlive()) {
                PowerTicker.tick(player);
                check(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Enhanced.id("spider_eyes_speed")) == null, how + ": the surge's modifiers are gone");
            }
        }
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void spiderTogglesDefaultAndPersist(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 10.5);
        PowerData data = data(player);
        check(data.toggle(Toggle.WALL_CRAWL) && data.toggle(Toggle.SPIDER_SENSE) && !data.toggle(Toggle.SPIDER_LEGS), "the defaults");
        press(player, 3);
        pressSneaking(player, 3);
        check(!data.toggle(Toggle.WALL_CRAWL) && !data.toggle(Toggle.SPIDER_SENSE), "V and Sneak + V flip them");
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var tag = PowerData.CODEC.encodeStart(ops, data).getOrThrow();
        PowerData loaded = PowerData.CODEC.parse(ops, tag).getOrThrow();
        check(loaded.power() == Power.SPIDER_MAN && !loaded.toggle(Toggle.WALL_CRAWL) && !loaded.toggle(Toggle.SPIDER_SENSE) && !loaded.toggle(Toggle.SPIDER_LEGS),
                "they are saved and loaded");
        ServerPlayer tommy = f.player(8.5, FLOOR, 8.5, Power.TOMMY);
        check(data(tommy).toggle(Toggle.SUPER_SPEED) && !data(tommy).toggle(Toggle.WALL_CRAWL), "Tommy's default is unchanged");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void theWebGeometryAndTheThreatMathHoldUp(GameTestHelper helper) {
        // The strands of the burst: unit vectors spread evenly over a sphere.
        for (int count : new int[]{6, 24, 96}) {
            Vec3[] strands = SpiderMath.fibonacciSphere(count, 12345L);
            check(strands.length == count, count + " strands");
            Vec3 mean = Vec3.ZERO;
            for (Vec3 d : strands) {
                check(Math.abs(d.length() - 1.0) < 1.0E-9, "each is a unit vector");
                mean = mean.add(d);
            }
            check(mean.length() / count < 0.05, "balanced round the middle: " + mean.length() / count);
        }
        check(!SpiderMath.fibonacciSphere(24, 1L)[3].equals(SpiderMath.fibonacciSphere(24, 40000L)[3]), "the seed turns the sphere");
        // The closest-approach test of the spider-sense.
        double[] hit = SpiderSense.closestApproach(new Vec3(0, 0, 0), new Vec3(0, 0, 1), new Vec3(0.5, 0, 10), 25);
        check(Math.abs(hit[0] - 10.0) < 1.0E-9 && Math.abs(hit[1] - 0.5) < 1.0E-9, "an arrow that passes 0.5 away in 10 ticks: " + hit[0] + " " + hit[1]);
        check(SpiderSense.closestApproach(new Vec3(0, 0, 0), new Vec3(0, 0, -1), new Vec3(0, 0, 10), 25)[1] == Double.POSITIVE_INFINITY, "one that flies away is no threat");
        check(SpiderSense.closestApproach(new Vec3(0, 0, 0), new Vec3(0, 0, 0.1), new Vec3(0, 0, 100), 25)[0] == 25.0, "and the look ahead is 25 ticks");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void aLokiDuplicateDissolvesAtAWebAndIsNotFeared(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = spider(f, 10.5, 6.5);
        ServerPlayer loki = f.player(2.5, FLOOR, 2.5, Power.LOKI);
        LokiIllusionEntity illusion = LokiIllusionEntity.conjure(loki, abs(helper, 10.5, FLOOR, 12.5), null, LokiIllusionEntity.Mode.DECOY, 400);
        illusion.setNoAi(true);
        lookAt(player, illusion);
        PowerTicker.tick(player);
        check(!data(player).is(Live.SENSING), "a duplicate is no danger to the spider-sense");
        check(!SpiderServer.isThreat(player, illusion), "and the legs never go for it");
        seen(player);
        press(player, 1);
        check(illusion.isRemoved(), "a web dissolves it, like any touch");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Config and assets

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void spiderManConfigIsSane(GameTestHelper helper) throws IllegalAccessException {
        int options = 0;
        for (Field field : SpiderConfig.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !ConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                continue;
            }
            options++;
            ConfigSpec.ConfigValue<?> value = (ConfigSpec.ConfigValue<?>) field.get(null);
            check(value != null, field.getName() + " is defined");
            ConfigSpec.ValueSpec spec = value.getSpec();
            check(spec.getComment() != null && !spec.getComment().isBlank(), field.getName() + " has a comment");
            check(spec.test(spec.getDefault()), field.getName() + " has a default inside its range");
            if (spec.getDefault() instanceof Number number) {
                check(number.doubleValue() >= 0.0, field.getName() + " is not negative");
                String name = field.getName();
                boolean mayBeZero = name.endsWith("_BONUS") || name.endsWith("_REDUCTION") || name.endsWith("_RESISTANCE")
                        || name.endsWith("_ARMOR") || name.endsWith("_TOUGHNESS") || name.endsWith("_EXHAUSTION")
                        || name.endsWith("_DAMAGE") || name.endsWith("_PATCH_RADIUS") || name.endsWith("_PANIC_HEALTH")
                        || name.endsWith("_CAP") || name.endsWith("_TICKS") || name.endsWith("_COOLDOWN");
                if (!mayBeZero) {
                    check(number.doubleValue() > 0.0, name + " is positive: " + number);
                }
            }
        }
        // (67 of 3.0, and the five of Web Assist in 3.3.)
        check(options == 72, "every option of the design is there: " + options);
        // The ones that must make something happen.
        check(SpiderConfig.WEB_SHOT_COOLDOWN.getDefault() > 0 && SpiderConfig.ZIP_COOLDOWN.getDefault() > 0
                && SpiderConfig.WEB_BURST_COOLDOWN.getDefault() > 0 && SpiderConfig.BLACK_EYES_COOLDOWN.getDefault() > 0
                && SpiderConfig.INSTANT_KILL_COOLDOWN.getDefault() > 0, "the cooldowns are real by default");
        check(SpiderConfig.WEB_SHOT_RANGE.getDefault() > 0 && SpiderConfig.ZIP_RANGE.getDefault() > 0
                && SpiderConfig.SWING_RANGE.getDefault() > 0 && SpiderConfig.SPIDER_SENSE_RANGE.getDefault() > 0, "the ranges are real");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = QUICK)
    public static void spiderManServerAssetsAndDataAreThere(GameTestHelper helper) {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        String[][] files = {
                {"assets", Enhanced.MODID, "textures", "mob_effect", "webbed.png"},
                {"assets", Enhanced.MODID, "textures", "mob_effect", "radioactive_bite.png"},
                {"assets", Enhanced.MODID, "textures", "block", "spider_web.png"},
                {"assets", Enhanced.MODID, "models", "block", "spider_web.json"},
                {"assets", Enhanced.MODID, "blockstates", "spider_web.json"},
                {"data", Enhanced.MODID, "recipe", "radioactive_spider.json"},
                {"data", Enhanced.MODID, "advancement", "recipes", "misc", "radioactive_spider.json"},
                {"data", Enhanced.MODID, "loot_table", "inject", "radioactive_spider.json"},
        };
        for (String[] file : files) {
            check(Files.isRegularFile(mod.findResource(file)), String.join("/", file) + " exists");
        }
        for (String source : new String[]{"abandoned_mineshaft", "simple_dungeon", "trial_chambers_reward", "ancient_city", "cave_spider"}) {
            check(Files.isRegularFile(mod.findResource("data", Enhanced.MODID, "loot_modifiers", "radioactive_spider_from_" + source + ".json")),
                    "the loot from " + source + " exists");
        }
        // The two effect icons are 18 x 18, as the effect overlay draws them.
        for (String icon : new String[]{"webbed.png", "radioactive_bite.png"}) {
            try (InputStream in = Files.newInputStream(mod.findResource("assets", Enhanced.MODID, "textures", "mob_effect", icon))) {
                BufferedImage image = ImageIO.read(in);
                check(image != null && image.getWidth() == 18 && image.getHeight() == 18, icon + " is a PNG of 18 x 18");
            } catch (IOException e) {
                throw new AssertionError(icon + " cannot be read", e);
            }
        }
        // The entities the web leaves alone and the minions he must not kill outright are tagged.
        check(EntityType.ENDER_DRAGON.is(ModTags.WEB_IMMUNE) && EntityType.WITHER.is(ModTags.WEB_IMMUNE) && EntityType.WARDEN.is(ModTags.WEB_IMMUNE),
                "the dragon, the Wither and the Warden cannot be webbed");
        check(EntityType.ENDER_DRAGON.is(ModTags.INSTANT_KILL_IMMUNE) && EntityType.WITHER.is(ModTags.INSTANT_KILL_IMMUNE),
                "nor killed in one blow");
        helper.succeed();
    }
}
