package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/** Data-driven content: loot modifiers and recipes. */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class DataTests {

    @GameTest(template = ARENA)
    public static void endCityLootHasScepterAndGene(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.END_CITY_TREASURE);
        check(table != LootTable.EMPTY, "end city treasure loot table exists");
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(10.5, 2, 10.5)))
                .create(LootContextParamSets.CHEST);
        RandomSource random = RandomSource.create(20241117L);
        int rolls = 3000;
        int scepters = 0;
        int genes = 0;
        for (int i = 0; i < rolls; i++) {
            for (ItemStack stack : table.getRandomItems(params, random)) {
                if (stack.is(ModItems.LOKIS_SCEPTER.get())) {
                    scepters += stack.getCount();
                }
                if (stack.is(ModItems.SCARLET_WITCH_GENE.get())) {
                    genes += stack.getCount();
                }
            }
        }
        Enhanced.LOGGER.info("End city treasure x{}: {} Loki's Scepters, {} Scarlet Witch Genes", rolls, scepters, genes);
        // Expected 10% (300) and 5% (150); the bounds are more than 5 standard deviations wide.
        check(scepters >= 210 && scepters <= 390, "Loki's Scepter should be in ~10% of End City treasure chests, got " + scepters + "/" + rolls);
        check(genes >= 85 && genes <= 215, "the Scarlet Witch Gene should be in ~5% of End City treasure chests, got " + genes + "/" + rolls);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void witchDropsGeneOnlyWhenKilledByAPlayer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, 2, 10.5);
        Witch witch = helper.spawnWithNoFreeWill(EntityType.WITCH, new Vec3(10.5, 2, 10.5));
        LootTable table = level.getServer().reloadableRegistries().getLootTable(EntityType.WITCH.getDefaultLootTable());
        LootParams byPlayer = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, witch)
                .withParameter(LootContextParams.ORIGIN, witch.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
                .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                .create(LootContextParamSets.ENTITY);
        LootParams byFall = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, witch)
                .withParameter(LootContextParams.ORIGIN, witch.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().fall())
                .create(LootContextParamSets.ENTITY);
        RandomSource random = RandomSource.create(1969L);
        int rolls = 6000;
        int withPlayer = countGenes(table, byPlayer, random, rolls);
        int withoutPlayer = countGenes(table, byFall, random, rolls);
        Enhanced.LOGGER.info("Witch x{}: {} genes when killed by a player, {} otherwise", rolls, withPlayer, withoutPlayer);
        // Expected 1% (60); the bounds are more than 5 standard deviations wide.
        check(withPlayer >= 20 && withPlayer <= 110, "a witch killed by a player drops the gene ~1% of the time, got " + withPlayer + "/" + rolls);
        check(withoutPlayer == 0, "only when killed by a player, got " + withoutPlayer);
        helper.succeed();
    }

    private static int countGenes(LootTable table, LootParams params, RandomSource random, int rolls) {
        int genes = 0;
        for (int i = 0; i < rolls; i++) {
            for (ItemStack stack : table.getRandomItems(params, random)) {
                if (stack.is(ModItems.SCARLET_WITCH_GENE.get())) {
                    genes += stack.getCount();
                }
            }
        }
        return genes;
    }

    @GameTest(template = ARENA)
    public static void magnetoHelmetRecipeNeedsFiveNetheriteBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("magneto_helmet"));
        check(holder.isPresent(), "recipe enhanced:magneto_helmet exists");
        check(holder.get().value() instanceof ShapedRecipe, "it is a shaped recipe");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.MAGNETO_HELMET.get()), "it makes Magneto's Helmet");

        int netherite = 0;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            check(ingredient.test(new ItemStack(Items.NETHERITE_BLOCK)) && !ingredient.test(new ItemStack(Items.NETHERITE_INGOT)),
                    "every ingredient is a Netherite Block");
            netherite++;
        }
        check(netherite == 5, "exactly 5 Netherite Blocks, got " + netherite);

        ItemStack n = new ItemStack(Items.NETHERITE_BLOCK);
        ItemStack e = ItemStack.EMPTY;
        check(recipe.matches(CraftingInput.of(3, 2, List.of(n, n, n, n, e, n)), level), "helmet shape of 5 Netherite Blocks crafts it");
        check(!recipe.matches(CraftingInput.of(3, 2, List.of(n, n, n, n, e, e)), level), "4 blocks are not enough");
        List<ItemStack> ingots = new ArrayList<>(List.of(n, n, n, n, e, n));
        ingots.set(0, new ItemStack(Items.NETHERITE_INGOT));
        check(!recipe.matches(CraftingInput.of(3, 2, ingots), level), "ingots don't count as blocks");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wandaCrownRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("wanda_crown"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:wanda_crown exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack g = new ItemStack(Items.GOLD_INGOT);
        ItemStack r = new ItemStack(Items.REDSTONE_BLOCK);
        check(recipe.matches(CraftingInput.of(3, 2, List.of(g, r, g, g, ItemStack.EMPTY, g)), level), "gold + redstone block crown shape");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.WANDA_CROWN.get()), "it makes Wanda's Crown");
        helper.succeed();
    }

    private DataTests() {
    }
}
