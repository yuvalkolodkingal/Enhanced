package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/** Data-driven content: loot modifiers and recipes, and what old saves still hold. */
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
    public static void endCityLootHasTesseract(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.END_CITY_TREASURE);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(10.5, 2, 10.5)))
                .create(LootContextParamSets.CHEST);
        RandomSource random = RandomSource.create(20121104L);
        int rolls = 3000;
        int tesseracts = 0;
        for (int i = 0; i < rolls; i++) {
            for (ItemStack stack : table.getRandomItems(params, random)) {
                if (stack.is(ModItems.TESSERACT.get())) {
                    tesseracts += stack.getCount();
                }
            }
        }
        Enhanced.LOGGER.info("End city treasure x{}: {} Tesseracts", rolls, tesseracts);
        // Expected 8% (240); the bounds are more than 5 standard deviations wide.
        check(tesseracts >= 160 && tesseracts <= 320, "the Tesseract should be in ~8% of End City treasure chests, got " + tesseracts + "/" + rolls);
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

    @GameTest(template = ARENA)
    public static void quicksilverGogglesRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("quicksilver_goggles"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:quicksilver_goggles exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack sugar = new ItemStack(Items.SUGAR);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack pane = new ItemStack(Items.GLASS_PANE);
        ItemStack diamonds = new ItemStack(Items.DIAMOND_BLOCK);
        check(recipe.matches(CraftingInput.of(3, 2, List.of(sugar, iron, sugar, pane, diamonds, pane)), level), "S I S / G D G");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.QUICKSILVER_GOGGLES.get()), "it makes Quicksilver's Goggles");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void synthezoidCoreRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("synthezoid_core"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "recipe enhanced:synthezoid_core exists");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        ItemStack r = new ItemStack(Items.REDSTONE_BLOCK);
        ItemStack n = new ItemStack(Items.NETHERITE_INGOT);
        ItemStack star = new ItemStack(Items.NETHER_STAR);
        check(recipe.matches(CraftingInput.of(3, 3, List.of(r, n, r, n, star, n, r, n, r)), level), "R N R / N S N / R N R");
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.SYNTHEZOID_CORE.get()), "it makes a Synthezoid Core");
        helper.succeed();
    }

    /**
     * Every sound the mod defines, Quicksilver's song included, is a registered sound event backed by a real Ogg file
     * in the mod (not missing, renamed or an LFS pointer).
     */
    @GameTest(template = ARENA)
    public static void everySoundHasItsFile(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        Path soundsJson = mod.findResource("assets", Enhanced.MODID, "sounds.json");
        check(Files.isRegularFile(soundsJson), "assets/enhanced/sounds.json exists");
        JsonObject sounds = JsonParser.parseString(Files.readString(soundsJson)).getAsJsonObject();
        check(sounds.has("quicksilver_theme"), "Quicksilver's song is defined");
        for (Map.Entry<String, JsonElement> event : sounds.entrySet()) {
            check(BuiltInRegistries.SOUND_EVENT.containsKey(Enhanced.id(event.getKey())), "sound event enhanced:" + event.getKey() + " is registered");
            for (JsonElement sound : event.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                String name = sound.isJsonObject() ? sound.getAsJsonObject().get("name").getAsString() : sound.getAsString();
                ResourceLocation id = ResourceLocation.parse(name);
                Path file = mod.findResource(("assets/" + id.getNamespace() + "/sounds/" + id.getPath() + ".ogg").split("/"));
                check(Files.isRegularFile(file), name + " has its .ogg file");
                byte[] head = new byte[4];
                try (InputStream in = Files.newInputStream(file)) {
                    check(in.readNBytes(head, 0, 4) == 4 && "OggS".equals(new String(head, StandardCharsets.US_ASCII)), name + " is an Ogg file");
                }
            }
        }
        for (var registered : ModSounds.SOUNDS.getEntries()) {
            check(sounds.has(registered.getId().getPath()), registered.getId() + " has an entry in sounds.json");
        }
        Path song = mod.findResource("assets", Enhanced.MODID, "sounds", "quicksilver", "sweet_dreams.ogg");
        check(Files.size(song) > 1_000_000, "Sweet Dreams is the whole song (" + Files.size(song) + " bytes)");
        helper.succeed();
    }

    /**
     * Every look of every hero has its skin (64x64) and cape (64x32) in the mod, and every hero, ability and look has
     * a name in both English and Hebrew.
     */
    @GameTest(template = ARENA)
    public static void everyLookHasItsSkinAndEveryNameItsTranslation(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        JsonObject en = lang(mod, "en_us");
        JsonObject he = lang(mod, "he_il");
        check(en.keySet().equals(he.keySet()), "en_us and he_il have the same keys; differing: "
                + symmetricDifference(en.keySet(), he.keySet()));
        List<String> missing = new ArrayList<>();
        for (Power power : Power.values()) {
            if (!power.hasPowers()) {
                continue;
            }
            String id = power.getSerializedName();
            for (String key : List.of("power.enhanced." + id, "power.enhanced." + id + ".awaken", "power.enhanced." + id + ".broadcast")) {
                if (!en.has(key)) {
                    missing.add(key);
                }
            }
            for (int ability = 1; ability <= PowerData.ABILITIES; ability++) {
                if (!en.has(power.abilityKey(ability))) {
                    missing.add(power.abilityKey(ability));
                }
            }
            for (Variant look : Variants.of(power)) {
                if (!en.has(look.translationKey())) {
                    missing.add(look.translationKey());
                }
                if (look.skin() != null) {
                    checkImage(mod, look.skin(), 64, 64);
                }
                if (look.cape() != null) {
                    checkImage(mod, look.cape(), 64, 32);
                }
            }
        }
        check(missing.isEmpty(), "missing translations: " + missing);
        helper.succeed();
    }

    /**
     * A world saved while a player was Photon (a hero this version no longer has) still loads: the unknown power reads
     * as NONE instead of failing (NeoForge throws away an attachment whose codec fails), and whatever else was saved
     * with it comes along.
     */
    @GameTest(template = ARENA)
    public static void savedPhotonLoadsAsNoPower(GameTestHelper helper) {
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag saved = new CompoundTag();
        saved.putString("power", "photon");
        saved.putBoolean("spectrum_vision", true);
        saved.putFloat("energy", 40.0F);
        saved.putBoolean("quicksilver_time", false);
        saved.putBoolean("wall_run", false);
        saved.putInt("iron_charge", 7);
        saved.putString("variant", "tracksuit");

        DataResult<PowerData> result = PowerData.CODEC.parse(ops, saved);
        check(result.error().isEmpty(), "a saved Photon decodes without an error: " + result.error());
        PowerData data = result.getOrThrow();
        check(data.power() == Power.NONE, "a saved Photon is a player without powers, not " + data.power());
        check(!data.quicksilverTime() && !data.wallRun() && data.ironCharge() == 7 && data.variant().equals("tracksuit"),
                "and the other fields it can read are kept");

        saved.putString("power", "no_such_hero");
        check(PowerData.CODEC.parse(ops, saved).getOrThrow().power() == Power.NONE, "so is any other name this version doesn't know");

        for (Power power : Power.values()) {
            saved.putString("power", power.getSerializedName());
            check(PowerData.CODEC.parse(ops, saved).getOrThrow().power() == power, power + " still reads as itself");
        }
        helper.succeed();
    }

    /**
     * The same through the real load path (what a login does): the player file of a Photon, with the fields Photon
     * kept in the power attachment, loads as a player without powers, and the attachment is not thrown away.
     */
    @GameTest(template = ARENA)
    public static void savedPhotonPlayerLoadsWithoutPowers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(2.5, FLOOR, 2.5, Power.QUICKSILVER);
        Powers.get(player).setWallRun(false);

        CompoundTag file = player.saveWithoutId(new CompoundTag());
        CompoundTag attachment = file.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY)
                .getCompound(Enhanced.id("power").toString());
        check(attachment.getString("power").equals("quicksilver") && !attachment.getBoolean("wall_run"),
                "the player file holds the power attachment: " + attachment);
        attachment.putString("power", "photon");
        attachment.putBoolean("spectrum_vision", true);
        attachment.putFloat("energy", 40.0F);

        player.load(file);
        PowerData data = Powers.get(player);
        check(data.power() == Power.NONE, "the saved Photon loads as a player without powers, not " + data.power());
        check(!data.wallRun(), "and the attachment was read, not dropped (Quicksilver's wall run is still off)");
        helper.succeed();
    }

    private static JsonObject lang(IModFile mod, String name) throws IOException {
        Path file = mod.findResource("assets", Enhanced.MODID, "lang", name + ".json");
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static java.util.Set<String> symmetricDifference(java.util.Set<String> a, java.util.Set<String> b) {
        java.util.Set<String> out = new java.util.TreeSet<>(a);
        out.addAll(b);
        java.util.Set<String> both = new java.util.HashSet<>(a);
        both.retainAll(b);
        out.removeAll(both);
        return out;
    }

    /** The texture exists and is a PNG of the given size. */
    private static void checkImage(IModFile mod, ResourceLocation texture, int width, int height) throws IOException {
        Path file = mod.findResource(("assets/" + texture.getNamespace() + "/" + texture.getPath()).split("/"));
        check(Files.isRegularFile(file), texture + " exists");
        // The PNG header: the signature, then the IHDR chunk with the width and height (no image library on a server).
        byte[] head = new byte[24];
        try (InputStream in = Files.newInputStream(file)) {
            check(in.readNBytes(head, 0, 24) == 24 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G', texture + " is a PNG");
        }
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(head);
        int w = buffer.getInt(16);
        int h = buffer.getInt(20);
        check(w == width && h == height, texture + " is " + width + "x" + height + ", not " + w + "x" + h);
    }

    private DataTests() {
    }
}
