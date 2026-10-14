package com.kolodkingal.enhanced.gametest;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
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
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

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

    /** Every entity the mod adds has a name, and every damage type its death messages, in English and in Hebrew. */
    @GameTest(template = ARENA)
    public static void everyEntityHasANameAndEveryDamageTypeItsDeathMessages(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        List<String> missing = new ArrayList<>();
        for (String name : List.of("en_us", "he_il")) {
            JsonObject lang = lang(mod, name);
            for (var entity : ModEntities.ENTITIES.getEntries()) {
                String key = "entity." + Enhanced.MODID + "." + entity.getId().getPath();
                if (!lang.has(key)) {
                    missing.add(name + ": " + key);
                }
            }
            var damageTypes = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            for (ResourceLocation id : damageTypes.keySet()) {
                if (!id.getNamespace().equals(Enhanced.MODID)) {
                    continue;
                }
                for (String suffix : List.of("", ".player")) {
                    String key = "death.attack." + Enhanced.MODID + "." + id.getPath() + suffix;
                    if (!lang.has(key)) {
                        missing.add(name + ": " + key);
                    }
                }
            }
        }
        check(missing.isEmpty(), "missing translations: " + missing);
        check(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(ModDamageTypes.LIGHTNING_BEAM.location()),
                "the lightning beam's damage type is there");
        helper.succeed();
    }

    /**
     * Every item, block and effect the mod adds, the subtitle of every sound (and of the sounds of 3.1 whose files are still
     * to come), each client option and each section of the client options has a name in English and in Hebrew, and a
     * Hebrew line takes the same placeholders as its English one (a translation that loses its "%s" would show the wrong
     * message).
     */
    @GameTest(template = ARENA)
    public static void everyItemBlockEffectSubtitleAndOptionHasANameInBothLanguages(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        JsonObject sounds = JsonParser.parseString(Files.readString(mod.findResource("assets", Enhanced.MODID, "sounds.json"),
                StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> keys = new ArrayList<>();
        ModItems.ITEMS.getEntries().forEach(item -> keys.add(item.get().getDescriptionId()));
        ModBlocks.BLOCKS.getEntries().forEach(block -> keys.add(block.get().getDescriptionId()));
        ModEffects.EFFECTS.getEntries().forEach(effect -> keys.add(effect.get().getDescriptionId()));
        for (Map.Entry<String, JsonElement> event : sounds.entrySet()) {
            JsonObject definition = event.getValue().getAsJsonObject();
            if (definition.has("subtitle")) {
                keys.add(definition.get("subtitle").getAsString());
            }
        }
        // 3.1's sounds: Build Magic's eight and Apocalypse's critical hit join sounds.json only with their files, but their
        // subtitles are named already.
        for (String sound : List.of("magnet_pull", "apocalypse_crit", "build_on_wanda", "build_on_billy", "build_cast_wanda",
                "build_cast_billy", "build_hum", "build_fizzle", "build_undo", "build_rank_up")) {
            keys.add("subtitles." + Enhanced.MODID + "." + sound);
        }
        for (var option : List.<ModConfigSpec.ConfigValue<?>>of(EnhancedClientConfig.ANIMATIONS, EnhancedClientConfig.REDUCED_MOTION,
                EnhancedClientConfig.FLIGHT_EFFECTS, EnhancedClientConfig.BUILD_HINTS, EnhancedClientConfig.BUILD_TIPS,
                EnhancedClientConfig.BUILD_GHOST_DETAIL, EnhancedClientConfig.BUILD_SEE_THROUGH, EnhancedClientConfig.BUILD_SHOW_OTHERS)) {
            String key = option.getSpec().getTranslationKey();
            check(key != null, "a client option has a translation key");
            keys.add(key);
            keys.add(key + ".tooltip");
        }
        // Each section of the client options is a row of the config screen. NeoForge names it by its own translation key,
        // else by "enhanced.configuration.<section>", and shows the bare key when the lang files lack it; the row also has
        // a tooltip and the text of its button.
        List<String> sections = new ArrayList<>();
        for (Map.Entry<String, Object> entry : EnhancedClientConfig.SPEC.getSpec().valueMap().entrySet()) {
            if (entry.getValue() instanceof UnmodifiableConfig) {
                sections.add(entry.getKey());
                String key = Optional.ofNullable(EnhancedClientConfig.SPEC.getLevelTranslationKey(List.of(entry.getKey())))
                        .orElse(Enhanced.MODID + ".configuration." + entry.getKey());
                keys.add(key);
                keys.add(key + ".tooltip");
                keys.add(key + ".button");
            }
        }
        check(sections.containsAll(List.of("animation", "appearance", "thanos", "build_magic")),
                "the sections of the client options are read from its spec (" + sections + ")");
        check(keys.size() > 40, "there are names to check (" + keys.size() + ")");
        JsonObject en = lang(mod, "en_us");
        JsonObject he = lang(mod, "he_il");
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            for (JsonObject lang : List.of(en, he)) {
                if (!lang.has(key)) {
                    missing.add((lang == en ? "en_us: " : "he_il: ") + key);
                }
            }
        }
        check(missing.isEmpty(), "missing translations: " + missing);
        Pattern placeholder = Pattern.compile("%(\\d+\\$)?[sd]");
        List<String> differing = new ArrayList<>();
        for (String key : en.keySet()) {
            if (!he.has(key)) {
                continue; // (the keys are compared in everyLookHasItsSkinAndEveryNameItsTranslation)
            }
            List<String> inEnglish = placeholder.matcher(en.get(key).getAsString()).results().map(MatchResult::group).sorted().toList();
            List<String> inHebrew = placeholder.matcher(he.get(key).getAsString()).results().map(MatchResult::group).sorted().toList();
            if (!inEnglish.equals(inHebrew)) {
                differing.add(key + " " + inEnglish + " / " + inHebrew);
            }
        }
        check(differing.isEmpty(), "the Hebrew lines take other placeholders than the English ones: " + differing);
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

    /**
     * Thor's two Love and Thunder looks (the bare head with its long hair, and the winged helmet) follow the skin
     * rules: a 64x64 skin whose body parts are fully opaque, whose overlay layer has no half-transparent pixels and
     * whose unused corners are empty; the cape has the same alpha mask as Thor's own; the hat layer keeps the four
     * pixels clear where the glowing eyes of the God of Thunder shine through, and the helmet look is the same suit
     * with a different head.
     */
    @GameTest(template = ARENA)
    public static void thorsLoveAndThunderLooksFollowTheSkinRules(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        List<String> ids = Variants.of(Power.THOR).stream().map(Variant::id).toList();
        check(ids.indexOf("love_and_thunder_helmet") == ids.indexOf("love_and_thunder") + 1,
                "the helmet look follows the Love and Thunder look in the cycle: " + ids);
        BufferedImage thorCape = readImage(mod, Variants.get(Power.THOR, "thor").cape());
        BufferedImage eyes = readImage(mod, Enhanced.id("textures/entity/thor_eyes.png"));
        Map<String, BufferedImage> skins = new java.util.LinkedHashMap<>();
        for (String id : List.of("love_and_thunder", "love_and_thunder_helmet")) {
            Variant look = Variants.get(Power.THOR, id);
            check(look.id().equals(id) && !look.slim(), id + " is a look of Thor with the classic arms");
            BufferedImage skin = readImage(mod, look.skin());
            BufferedImage cape = readImage(mod, look.cape());
            check(skin.getWidth() == 64 && skin.getHeight() == 64, id + " skin is 64x64");
            boolean[][] used = new boolean[64][64];
            for (SkinPart part : SKIN_PARTS) {
                for (int[] face : part.faces()) {
                    for (int y = face[1]; y < face[1] + face[3]; y++) {
                        for (int x = face[0]; x < face[0] + face[2]; x++) {
                            used[y][x] = true;
                            int alpha = skin.getRGB(x, y) >>> 24;
                            check(!part.base() || alpha == 255, id + ": the base layer is opaque at " + x + "," + y);
                            check(part.base() || alpha == 0 || alpha == 255, id + ": no half-transparent overlay pixel at " + x + "," + y);
                        }
                    }
                }
            }
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    check(used[y][x] || skin.getRGB(x, y) >>> 24 == 0, id + ": nothing is drawn outside the used rectangles at " + x + "," + y);
                }
            }
            check(cape.getWidth() == 64 && cape.getHeight() == 32, id + " cape is 64x32");
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 64; x++) {
                    check(cape.getRGB(x, y) >>> 24 == thorCape.getRGB(x, y) >>> 24, id + ": the cape has Thor's alpha mask at " + x + "," + y);
                }
            }
            // The glowing eyes are drawn over the face (8..15, 8..15); the hat layer (32..39 further right) leaves them free.
            int glow = 0;
            for (int y = 8; y < 16; y++) {
                for (int x = 8; x < 16; x++) {
                    if (eyes.getRGB(x, y) >>> 24 != 0) {
                        glow++;
                        check(skin.getRGB(x + 32, y) >>> 24 == 0, id + ": the hat layer is clear over the glowing eye at " + x + "," + y);
                    }
                }
            }
            check(glow == 4, "the God of Thunder's eyes are the four pixels this look expects, not " + glow);
            skins.put(id, skin);
        }
        BufferedImage bare = skins.get("love_and_thunder");
        BufferedImage helmet = skins.get("love_and_thunder_helmet");
        int differing = 0;
        for (SkinPart part : SKIN_PARTS) {
            for (int[] face : part.faces()) {
                for (int y = face[1]; y < face[1] + face[3]; y++) {
                    for (int x = face[0]; x < face[0] + face[2]; x++) {
                        if (bare.getRGB(x, y) != helmet.getRGB(x, y)) {
                            differing++;
                            check(part.name().equals("hat") || part.name().equals("head") || part.name().equals("jacket"),
                                    "the helmet look wears the same suit: " + part.name() + " differs at " + x + "," + y);
                        }
                    }
                }
            }
        }
        check(differing > 100, "the helmet changes the head (" + differing + " pixels differ)");
        helper.succeed();
    }

    /** One box of the player model in the skin: its name, whether it is a base part and its six face rectangles (x, y, w, h). */
    private record SkinPart(String name, boolean base, int u, int v, int w, int h, int d) {
        int[][] faces() {
            return new int[][]{{u + d, v, w, d}, {u + d + w, v, w, d}, {u, v + d, d, h}, {u + d, v + d, w, h},
                    {u + d + w, v + d, d, h}, {u + d + w + d, v + d, w, h}};
        }
    }

    /** The classic (4-pixel arms) skin layout: head, body, arms and legs, and the second layer over each. */
    private static final List<SkinPart> SKIN_PARTS = List.of(
            new SkinPart("head", true, 0, 0, 8, 8, 8), new SkinPart("hat", false, 32, 0, 8, 8, 8),
            new SkinPart("body", true, 16, 16, 8, 12, 4), new SkinPart("jacket", false, 16, 32, 8, 12, 4),
            new SkinPart("rarm", true, 40, 16, 4, 12, 4), new SkinPart("rsleeve", false, 40, 32, 4, 12, 4),
            new SkinPart("larm", true, 32, 48, 4, 12, 4), new SkinPart("lsleeve", false, 48, 48, 4, 12, 4),
            new SkinPart("rleg", true, 0, 16, 4, 12, 4), new SkinPart("rpants", false, 0, 32, 4, 12, 4),
            new SkinPart("lleg", true, 16, 48, 4, 12, 4), new SkinPart("lpants", false, 0, 48, 4, 12, 4));

    private static BufferedImage readImage(IModFile mod, ResourceLocation texture) throws IOException {
        Path file = mod.findResource(("assets/" + texture.getNamespace() + "/" + texture.getPath()).split("/"));
        try (InputStream in = Files.newInputStream(file)) {
            BufferedImage image = ImageIO.read(in);
            check(image != null, texture + " is a readable image");
            return image;
        }
    }

    /**
     * Every awakening line is seen whole: a subtitle is drawn at twice the size of text and never wrapped, so a short line
     * goes under the title and a long one in the action bar; a Hebrew line shown under the title fits there too.
     */
    @GameTest(template = ARENA)
    public static void everyAwakeningLineIsSeenWhole(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        JsonObject en = lang(mod, "en_us");
        JsonObject he = lang(mod, "he_il");
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Power power : Power.values()) {
            if (!power.hasPowers()) {
                continue;
            }
            String key = "power.enhanced." + power.getSerializedName() + ".awaken";
            boolean under = Powers.fitsSubtitle(key);
            check(under == (en.get(key).getAsString().length() <= Powers.SUBTITLE_CHARS), key + ": the server measures the English line");
            check(!under || he.get(key).getAsString().length() <= Powers.SUBTITLE_CHARS,
                    "the Hebrew of " + key + " fits under the title too: " + he.get(key).getAsString());
            ServerPlayer player = f.player(10.5, FLOOR, 10.5);
            TestKit.sentPackets(player);
            Powers.awaken(player, power);
            Component subtitle = null;
            boolean inActionBar = false;
            List<String> sent = new ArrayList<>();
            for (Object packet : TestKit.sentPackets(player)) {
                sent.add(packet.getClass().getSimpleName());
                if (packet instanceof ClientboundSetSubtitleTextPacket text) {
                    subtitle = text.text();
                } else if (packet instanceof ClientboundSystemChatPacket chat && chat.overlay()) {
                    inActionBar |= says(chat.content(), key);
                }
            }
            check(subtitle != null, power + ": a subtitle is sent (empty or not), so no old one stays under the title: " + sent);
            if (under) {
                check(says(subtitle, key) && !inActionBar, power + ": the line goes under the title");
            } else {
                check(subtitle.getString().isEmpty() && inActionBar, power + ": the long line goes in the action bar, not under the title ("
                        + subtitle.getString() + "): " + sent);
            }
        }
        helper.succeed();
    }

    /**
     * The longest a Build Magic line may be, numbers and names included (each %s counted as {@value #BUILD_SAMPLE_CHARS}
     * characters): the action bar never wraps and the window's default (854 x 480, GUI scale 2) is 427 pixels wide, where
     * a character is about 5 pixels in either language, so 56 characters (at most about 290 pixels) are read whole and
     * stand clear of the hint strip's corner (measured on the font's own glyphs, joint check findings 1 and 7).
     */
    private static final int BUILD_LINE_CHARS = 56;
    private static final int BUILD_SAMPLE_CHARS = 6;

    /** Every line Build Magic says (message.enhanced.build.*) is short enough to be read whole at the default window. */
    @GameTest(template = ARENA)
    public static void everyBuildMagicLineIsShortEnoughToReadWhole(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        List<String> tooLong = new ArrayList<>();
        int seen = 0;
        for (String name : List.of("en_us", "he_il")) {
            for (var entry : lang(mod, name).entrySet()) {
                if (!entry.getKey().startsWith("message.enhanced.build.")) {
                    continue;
                }
                seen++;
                String line = entry.getValue().getAsString().replaceAll("%(\\d+\\$)?s", "x".repeat(BUILD_SAMPLE_CHARS));
                if (line.length() > BUILD_LINE_CHARS) {
                    tooLong.add(name + " " + entry.getKey() + " (" + line.length() + "): " + line);
                }
            }
        }
        check(seen >= 2 * 50, "the Build Magic lines were found in both languages: " + seen);
        check(tooLong.isEmpty(), "Build Magic lines over " + BUILD_LINE_CHARS + " characters: " + tooLong);
        helper.succeed();
    }

    private static boolean says(Component text, String key) {
        return text.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key);
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
