package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.HeroServers;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.boss.BossData;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.item.AwakeningItem;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.item.ScepterRituals.Ritual;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.network.MindReadPayload;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.network.PhoenixBurstPayload;
import com.kolodkingal.enhanced.network.SokoviaLiftPayload;
import com.kolodkingal.enhanced.network.SpiderFxPayload;
import com.kolodkingal.enhanced.network.SpiderSensePayload;
import com.kolodkingal.enhanced.network.ThanosFxPayload;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The skeleton of 3.0: the five new heroes (the Phoenix, Spider-Man, Thanos, Ultron, Apocalypse) exist, do nothing yet
 * and break nothing; the shared pieces the lanes build on (the plug-in registries, the tags, the melee rule, the
 * Scepter and Tesseract table, the awakening items, the area rule, the boss facade, the payloads) work as designed.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class SkeletonTests {
    /** The heroes of 3.0, in Eilon's order: the last five of {@link Power}. */
    private static final List<Power> NEW_HEROES = List.of(Power.PHOENIX, Power.SPIDER_MAN, Power.THANOS, Power.ULTRON, Power.APOCALYPSE);

    // ------------------------------------------------------------------------------------------------------------
    // The five powers

    @GameTest(template = ARENA)
    public static void theFiveNewHeroesComeLastInEilonsOrder(GameTestHelper helper) {
        Power[] all = Power.values();
        for (int i = 0; i < NEW_HEROES.size(); i++) {
            check(all[all.length - NEW_HEROES.size() + i] == NEW_HEROES.get(i), "hero " + i + " of 3.0 is " + NEW_HEROES.get(i));
        }
        check(all[all.length - NEW_HEROES.size() - 1] == Power.THOR, "the heroes of 3.0 follow Thor (the stream sends the ordinal)");
        check(Power.THOR.ordinal() == 8, "and nobody before them moved: Thor is still the ninth power");
        helper.succeed();
    }

    /** A name for everything, a look to choose, and flight rules that agree with each other. */
    @GameTest(template = ARENA)
    public static void everyNewHeroHasNamesLooksAndConsistentFlightRules(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        JsonObject en = lang(mod, "en_us");
        JsonObject he = lang(mod, "he_il");
        for (Power power : NEW_HEROES) {
            check(power.hasPowers(), power + " is a power");
            check(Power.byName(power.getSerializedName()) == power, power + " is found by its name");
            for (JsonObject lang : List.of(en, he)) {
                check(lang.has("power.enhanced." + power.getSerializedName()), power + " has a name");
                for (int ability = 1; ability <= PowerData.ABILITIES; ability++) {
                    check(lang.has(power.abilityKey(ability)), power + " has a name for ability " + ability);
                }
            }
            List<Variant> looks = Variants.of(power);
            check(looks.size() >= 3 && Variants.hasChoice(power), power + " has looks to choose from, not " + looks.size());
            check(looks.get(looks.size() - 1).id().equals("own"), power + "'s last look is the player's own skin");
            if (power.canFly()) {
                check(power.flightAbility() == 2, power + " flies on G");
                check(power.flightSpeed() > 0.05F, power + " flies faster than Creative flight");
            } else {
                check(power.flightAbility() == -1, power + " has no flight key");
            }
        }
        assertNear(Power.PHOENIX.flightSpeed(), 0.09, 1.0E-6, "the Phoenix flies like Wanda");
        assertNear(Power.ULTRON.flightSpeed(), 0.10, 1.0E-6, "Ultron's flight speed");
        assertNear(Power.APOCALYPSE.flightSpeed(), 0.08, 1.0E-6, "Apocalypse's flight speed");
        check(Power.ULTRON.titleColor() == 0xD8262B && Power.APOCALYPSE.titleColor() == 0x4A9BC4 && Power.MAGNETO.titleColor() == 0xC8323C
                && Power.PHOENIX.titleColor() == Power.PHOENIX.color, "the colours of the names");
        helper.succeed();
    }

    /** {@code /enhanced power set <player> <hero>} makes each of them, with and without a look. */
    @GameTest(template = ARENA)
    public static void thePowerCommandMakesEveryNewHero(GameTestHelper helper) throws CommandSyntaxException {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        for (Power hero : NEW_HEROES) {
            run(player, "enhanced power set " + player.getGameProfile().getName() + " " + hero.getSerializedName());
            check(Powers.power(player) == hero, "the command makes " + hero + ", not " + Powers.power(player));
            check(Powers.variant(player).equals(Variants.of(hero).get(0)), hero + " starts in its first look");
            String look = Variants.of(hero).get(1).id();
            run(player, "enhanced power set " + player.getGameProfile().getName() + " " + hero.getSerializedName() + " " + look);
            check(Powers.variant(player).id().equals(look), hero + " takes the look " + look);
        }
        run(player, "enhanced power set " + player.getGameProfile().getName() + " none");
        check(Powers.power(player) == Power.NONE, "and it takes the power away");
        helper.succeed();
    }

    /** The power exists and does nothing: no key does anything, nothing ticks into a state, nobody is hurt. */
    @GameTest(template = ARENA)
    public static void aNewHeroDoesNothingYet(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Power hero : NEW_HEROES) {
            ServerPlayer player = f.player(10.5, FLOOR, 10.5, hero);
            PowerData data = Powers.get(player);
            float health = player.getHealth();
            double maxHealth = player.getMaxHealth();
            for (int ability = 1; ability <= PowerData.ABILITIES; ability++) {
                AbilityHandler.onKey(player, ability, true);
                AbilityHandler.onKey(player, ability, false);
                PowerTicker.tick(player);
                check(data.isReady(ability, helper.getLevel().getGameTime()), hero + ": ability " + ability + " started a cooldown");
            }
            for (Live live : Live.values()) {
                check(!data.is(live), hero + " has the state " + live);
            }
            check(!data.beamActive() && !data.chaosShieldActive() && !data.shrapnelActive() && data.heldEntityId() == -1,
                    hero + " holds nothing");
            check(player.getHealth() == health && player.getMaxHealth() == maxHealth, hero + " is as healthy as before");
            check(player.isAlive(), hero + " is alive");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theFliersFlyAndTheOthersDoNot(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Power hero : NEW_HEROES) {
            ServerPlayer player = f.player(10.5, FLOOR, 10.5, hero);
            PowerTicker.tick(player);
            check(player.mayFly() == hero.canFly(), hero + (hero.canFly() ? " can fly" : " cannot fly"));
            if (hero.canFly()) {
                assertNear(player.getAbilities().getFlyingSpeed(), hero.flightSpeed(), 1.0E-4, hero + " flies at its speed");
            }
            // G toggles the flight of the fliers, once their lane wires it: until then nothing takes off.
            if (hero.canFly()) {
                AbilityHandler.onKey(player, hero.flightAbility(), true);
                check(!player.getAbilities().flying, hero + ": G is not wired yet, so nothing took off");
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void spiderManStartsWithHisWallAndHisSenseOn(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer spider = f.player(5.5, FLOOR, 5.5, Power.SPIDER_MAN);
        PowerData data = Powers.get(spider);
        check(data.toggle(Toggle.WALL_CRAWL) && data.toggle(Toggle.SPIDER_SENSE) && !data.toggle(Toggle.SPIDER_LEGS),
                "his wall crawl and his spider-sense are on from the start, his legs are not out");
        ServerPlayer tommy = f.player(8.5, FLOOR, 5.5, Power.TOMMY);
        check(Powers.get(tommy).toggle(Toggle.SUPER_SPEED) && !Powers.get(tommy).toggle(Toggle.WALL_CRAWL), "Tommy's default is unchanged");
        ServerPlayer phoenix = f.player(5.5, FLOOR, 8.5, Power.PHOENIX);
        check(!Powers.get(phoenix).toggle(Toggle.MIND_READING), "the Phoenix does not read minds until she says so");
        helper.succeed();
    }

    /**
     * The momentary states are a long since 3.0: 26 bits are used, there is room for 64, and the sync keeps them all.
     */
    @GameTest(template = ARENA)
    public static void theLiveStatesFitInALongAndSurviveTheSync(GameTestHelper helper) {
        check(Live.values().length >= 26 && Live.values().length <= 63, "26 states or more, and room to grow: " + Live.values().length);
        PowerData data = new PowerData();
        for (Live live : Live.values()) {
            data.set(live, true);
        }
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        PowerData.STREAM_CODEC.encode(buf, data);
        PowerData decoded = PowerData.STREAM_CODEC.decode(buf);
        for (Live live : Live.values()) {
            check(decoded.is(live), live + " is lost on the way to the client");
        }
        data.set(Live.SANDSTORM, false);
        data.set(Live.PHOENIX_CHARGING, false);
        RegistryFriendlyByteBuf again = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        PowerData.STREAM_CODEC.encode(again, data);
        PowerData second = PowerData.STREAM_CODEC.decode(again);
        check(!second.is(Live.SANDSTORM) && !second.is(Live.PHOENIX_CHARGING) && second.is(Live.PINNED) && second.is(Live.LIFTED),
                "and a bit turned off arrives off");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void shiftingCooldownsMovesOnlyWhatIsRunning(GameTestHelper helper) {
        PowerData data = new PowerData();
        long now = 1000;
        data.startCooldown(1, now, 100);
        data.startCooldown(2, now, 10);
        data.clearDirty();
        data.shiftCooldowns(now, 0);
        check(!data.isDirty(), "moving nothing does not make the data dirty");
        data.shiftCooldowns(now, 30);
        check(data.cooldownUntil(1) == now + 70, "a long cooldown ends 30 ticks sooner");
        check(data.cooldownUntil(2) == now, "a short one ends now, not before");
        check(data.cooldownUntil(3) == 0 && data.isDirty(), "one that was not running is left alone, and the data is dirty");
        data.clearDirty();
        data.shiftCooldowns(now + 200, 30);
        check(!data.isDirty(), "nothing is running later on, so nothing moves and nothing is sent");
        helper.succeed();
    }

    /** A world saved with a new hero loads with it; one saved without any loads without. */
    @GameTest(template = ARENA)
    public static void aSavedNewHeroLoadsBackAndAnOldSaveLoadsWithout(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5, Power.SPIDER_MAN);
        Powers.setVariant(player, Variants.get(Power.SPIDER_MAN, "iron_spider"));
        Powers.get(player).set(Live.BLACK_EYES, true);
        CompoundTag file = player.saveWithoutId(new CompoundTag());
        CompoundTag attachment = file.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY).getCompound(Enhanced.id("power").toString());
        check(attachment.getString("power").equals("spider_man"), "saved by name: " + attachment);
        check(!attachment.contains("live"), "the momentary states are not saved");

        player.load(file);
        PowerData data = Powers.get(player);
        check(data.power() == Power.SPIDER_MAN && data.variant().equals("iron_spider"), "he loads back as the Iron Spider");
        check(data.toggle(Toggle.WALL_CRAWL), "with his toggles");
        check(!data.is(Live.BLACK_EYES), "and without the states that were never saved");

        attachment.remove("power");
        player.load(file);
        check(Powers.power(player) == Power.NONE, "a save with no power in it loads without one");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The registries

    @GameTest(template = ARENA)
    public static void theHeroesAreAllRegisteredAsPlugIns(GameTestHelper helper) {
        for (Power hero : NEW_HEROES) {
            check(HeroServers.of(hero) != null && HeroServers.of(hero).power() == hero, hero + " has a server plug-in");
            check(HeroClients.of(hero) != null, hero + " has a client plug-in");
        }
        check(HeroServers.all().size() == NEW_HEROES.size(), "and nobody else is a plug-in");
        for (Power old : List.of(Power.NONE, Power.MAGNETO, Power.SCARLET_WITCH, Power.QUICKSILVER, Power.VISION, Power.BILLY,
                Power.TOMMY, Power.LOKI, Power.THOR)) {
            check(HeroServers.of(old) == null, old + " keeps its switch arms");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theEntitiesAreRegisteredAndCannotBeSummoned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<EntityType<?>> types = List.of(ModEntities.THANOS.get(), ModEntities.THANOS_SWORD.get(), ModEntities.THANOS_METEOR.get(),
                ModEntities.THANOS_ORB.get(), ModEntities.THANOS_PHANTOM.get(), ModEntities.ULTRON.get(), ModEntities.ULTRON_SENTRY.get(),
                ModEntities.VIBRANIUM_KEY.get(), ModEntities.ULTRON_BOLT.get(), ModEntities.APOCALYPSE.get(), ModEntities.SAND_LANCE.get(),
                ModEntities.SAND_BOULDER.get(), ModEntities.SAND_COCOON.get());
        check(types.size() == 13, "thirteen entities");
        for (EntityType<?> type : types) {
            check(!type.canSummon(), type + " cannot be summoned");
            check(type.canSerialize() == (type == ModEntities.ULTRON_SENTRY.get()), type + " is saved only if it is a Sentry");
            var entity = type.create(level);
            check(entity != null, type + " can be made");
            check(BossFights.isFightEntity(entity), type + " is a fight entity");
            if (entity instanceof LivingEntity) {
                check(DefaultAttributes.hasSupplier((EntityType<? extends LivingEntity>) type), type + " has attributes");
            }
            entity.discard();
        }
        check(!BossFights.isFightEntity(helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, FLOOR, 3.5))), "a zombie is not");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theEffectsAreRegisteredAndHold(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        var effects = List.of(ModEffects.POSSESSED, ModEffects.WEBBED, ModEffects.RADIOACTIVE_BITE, ModEffects.PETRIFIED,
                ModEffects.SOULWORLD, ModEffects.TIME_LOCKED, ModEffects.ENTOMBED, ModEffects.EMPOWERED);
        for (var effect : effects) {
            check(player.addEffect(new MobEffectInstance(effect, 40)), effect.getId() + " can be given");
            check(player.hasEffect(effect), effect.getId() + " is held");
        }
        check(effects.size() == new HashSet<>(effects).size(), "eight different effects");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theBossDataStartsEmptyAndTheCodecRoundTrips(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BossData fresh = level.getServer().overworld().getData(ModAttachments.BOSS_DATA);
        check(fresh.gifts().isEmpty() && fresh.rest().isEmpty(), "no gift and no rest at the start");
        RegistryOps<Tag> ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        UUID who = UUID.randomUUID();
        ItemStack gauntlet = new ItemStack(ModItems.INFINITY_GAUNTLET.get());
        BossData data = new BossData(Map.of(who, List.of(gauntlet)), Map.of("thanos", 12000L));
        Tag saved = BossData.CODEC.encodeStart(ops, data).getOrThrow();
        BossData loaded = BossData.CODEC.parse(ops, saved).getOrThrow();
        check(loaded.rest().equals(Map.of("thanos", 12000L)), "the rest comes back");
        check(loaded.gifts().get(who).size() == 1 && ItemStack.matches(loaded.gifts().get(who).get(0), gauntlet), "so does the gift");
        check(BossData.CODEC.parse(ops, new CompoundTag()).getOrThrow().gifts().isEmpty(), "an empty save reads as empty");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Chaos Shield's melee rule reads a tag, and the tag holds exactly what the rule held before

    @GameTest(template = ARENA)
    public static void theMeleeBlowsTagHoldsExactlyTodaysListPlusTheNewBlows(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Set<String> before = Set.of("minecraft:mob_attack", "minecraft:mob_attack_no_aggro", "minecraft:player_attack", "minecraft:sting",
                "enhanced:speed_blitz");
        Set<String> added = Set.of("enhanced:thanos_blow", "enhanced:thanos_sword", "enhanced:apocalypse_blow", "enhanced:spider_legs",
                "enhanced:web_kick");
        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, FLOOR, 5.5));
        Set<String> inTag = new LinkedHashSet<>();
        for (Holder.Reference<DamageType> holder : registry.holders().toList()) {
            String id = holder.key().location().toString();
            DamageSource blow = new DamageSource(holder, attacker, attacker);
            boolean melee = WandaAbilities.isMeleeHit(blow);
            if (holder.is(ModTags.MELEE_BLOWS)) {
                inTag.add(id);
            }
            if (!added.contains(id)) {
                // Every damage type that existed in 2.3 is a melee blow exactly when the hard-coded list said so.
                check(melee == before.contains(id), id + ": " + (melee ? "is" : "is not") + " a melee blow, 2.3 said the opposite");
            } else {
                check(melee, id + " is a melee blow of 3.0");
            }
            // A blow that is thrown or cast (nobody's own hand) is never melee, whatever its type.
            check(!WandaAbilities.isMeleeHit(new DamageSource(holder, helper.spawn(EntityType.ARROW, new Vec3(3.5, FLOOR + 3, 3.5)), attacker)),
                    id + ": a projectile of it is not a blow");
        }
        Set<String> expected = new LinkedHashSet<>(before);
        expected.addAll(added);
        check(inTag.equals(expected), "the tag holds " + inTag + ", not " + expected);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void mindPowerIsTheBeamTheSlamAndTheBurst(GameTestHelper helper) {
        Registry<DamageType> registry = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Set<String> inTag = new LinkedHashSet<>();
        registry.holders().filter(h -> h.is(ModTags.MIND_POWER)).forEach(h -> inTag.add(h.key().location().toString()));
        check(inTag.equals(Set.of("enhanced:chaos_beam", "enhanced:telekinetic_slam", "enhanced:phoenix_burst")), "mind power: " + inTag);
        helper.succeed();
    }

    /** Every tag file of the mod parses, and every entry names a real thing or a tag that exists. */
    @GameTest(template = ARENA)
    public static void everyTagFileNamesRealThings(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        int files = 0;
        for (String namespace : List.of("enhanced", "minecraft")) {
            Path root = mod.findResource("data", namespace, "tags");
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".json")).toList()) {
                    files++;
                    Path relative = root.relativize(file);
                    String kind = relative.getName(0).toString();
                    String name = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
                    name = name.substring(0, name.length() - ".json".length());
                    JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (JsonElement entry : json.getAsJsonArray("values")) {
                        String id = entry.isJsonObject() ? entry.getAsJsonObject().get("id").getAsString() : entry.getAsString();
                        boolean isTag = id.startsWith("#");
                        ResourceLocation location = ResourceLocation.parse(isTag ? id.substring(1) : id);
                        boolean found = switch (kind) {
                            case "entity_type" -> exists(BuiltInRegistries.ENTITY_TYPE, location, isTag);
                            case "block" -> exists(BuiltInRegistries.BLOCK, location, isTag);
                            case "item" -> exists(BuiltInRegistries.ITEM, location, isTag);
                            case "damage_type" -> exists(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE), location, isTag);
                            default -> true;
                        };
                        if (!found) {
                            problems.add(namespace + ":" + kind + "/" + name + " names " + id + ", which does not exist");
                        }
                    }
                }
            }
        }
        check(files > 40, "there are tag files to check (" + files + ")");
        check(problems.isEmpty(), "bad tag entries: " + problems);
        helper.succeed();
    }

    private static <T> boolean exists(Registry<T> registry, ResourceLocation location, boolean isTag) {
        return isTag ? registry.getTag(TagKey.create(registry.key(), location)).isPresent() : registry.containsKey(location);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Loki's Scepter and the Tesseract: one table for both

    /** The table of integration.md 3.2, row by row. */
    @GameTest(template = ARENA)
    public static void theScepterAndTesseractTableIsReadRowByRow(GameTestHelper helper) {
        ItemStack gene = new ItemStack(ModItems.SCARLET_WITCH_GENE.get());
        ItemStack core = new ItemStack(ModItems.SYNTHEZOID_CORE.get());
        ItemStack ultronCore = new ItemStack(ModItems.ULTRON_CORE.get());
        ItemStack program = new ItemStack(ModItems.ULTRON_PROGRAM.get());
        ItemStack tesseract = new ItemStack(ModItems.TESSERACT.get());
        ItemStack scepter = new ItemStack(ModItems.LOKIS_SCEPTER.get());
        ItemStack stick = new ItemStack(Items.STICK);
        ItemStack none = ItemStack.EMPTY;

        // The Scepter in one hand, ...
        row(Ritual.WANDA_GENE, ScepterRituals.scepter(Power.NONE, gene, false, false), "Scepter + Gene");
        row(Ritual.VISION_CORE, ScepterRituals.scepter(Power.NONE, core, false, false), "Scepter + Synthezoid Core");
        row(Ritual.ULTRON_CORE, ScepterRituals.scepter(Power.NONE, ultronCore, false, false), "Scepter + Ultron's Core");
        row(Ritual.ULTRON_FIGHT, ScepterRituals.scepter(Power.NONE, program, true, true), "Scepter + Program, Sneak, on a block");
        row(Ritual.ULTRON_HINT, ScepterRituals.scepter(Power.NONE, program, false, true), "Scepter + Program, not sneaking");
        row(Ritual.ULTRON_HINT, ScepterRituals.scepter(Power.NONE, program, true, false), "Scepter + Program, Sneak, in the air");
        row(Ritual.THANOS, ScepterRituals.scepter(Power.NONE, tesseract, false, false), "Scepter + Tesseract");
        row(Ritual.EXPLODES, ScepterRituals.scepter(Power.NONE, stick, false, false), "Scepter + anything else");
        row(Ritual.EXPLODES, ScepterRituals.scepter(Power.NONE, none, false, false), "Scepter + nothing");
        row(Ritual.WANDA_GENE, ScepterRituals.scepter(Power.LOKI, gene, false, false), "a hero with a gene (the executor refuses, not the table)");
        // ... the stone knows its own (unchanged), but not when a villain's ritual is asked for.
        for (Power own : List.of(Power.SCARLET_WITCH, Power.VISION)) {
            row(Ritual.RECOGNISES, ScepterRituals.scepter(own, none, false, false), own + " + nothing");
            row(Ritual.RECOGNISES, ScepterRituals.scepter(own, gene, false, false), own + " + Gene (unchanged)");
            row(Ritual.RECOGNISES, ScepterRituals.scepter(own, stick, false, false), own + " + anything else");
            row(Ritual.ULTRON_FIGHT, ScepterRituals.scepter(own, program, true, true), own + " + Program: a hero can wake Ultron");
            row(Ritual.THANOS, ScepterRituals.scepter(own, tesseract, false, false), own + " + Tesseract: a hero can call the Mad Titan");
        }
        // The Tesseract in one hand, ...
        for (Power power : List.of(Power.NONE, Power.LOKI, Power.THOR)) {
            row(Ritual.THANOS, ScepterRituals.tesseract(power, scepter), "Tesseract + Scepter for " + power);
        }
        row(Ritual.JUMP, ScepterRituals.tesseract(Power.THOR, none), "a hero's jump");
        row(Ritual.JUMP, ScepterRituals.tesseract(Power.THOR, gene), "a hero with a gene still jumps");
        row(Ritual.WANDA_GENE, ScepterRituals.tesseract(Power.NONE, gene), "Tesseract + Gene");
        row(Ritual.ESCAPE, ScepterRituals.tesseract(Power.NONE, none), "Loki's escape");
        row(Ritual.ESCAPE, ScepterRituals.tesseract(Power.NONE, stick), "Loki's escape with anything else");
        row(Ritual.ESCAPE, ScepterRituals.tesseract(Power.NONE, core), "(a Synthezoid Core does nothing for the Tesseract)");
        helper.succeed();
    }

    private static void row(Ritual expected, Ritual actual, String what) {
        check(expected == actual, what + ": expected " + expected + " but was " + actual);
    }

    @GameTest(template = ARENA)
    public static void theScepterWakesUltronFromHisCoreForAPowerlessPlayerOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        ItemStack core = new ItemStack(ModItems.ULTRON_CORE.get());
        player.setItemInHand(InteractionHand.OFF_HAND, core);
        check(!ScepterRituals.useScepter(player, InteractionHand.MAIN_HAND, false), "no hold begins");
        check(Powers.power(player) == Power.ULTRON, "a powerless player becomes Ultron, not " + Powers.power(player));
        check(core.isEmpty(), "and the core is used up");

        ServerPlayer thor = f.player(9.5, FLOOR, 5.5, Power.THOR);
        thor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        ItemStack kept = new ItemStack(ModItems.ULTRON_CORE.get());
        thor.setItemInHand(InteractionHand.OFF_HAND, kept);
        ScepterRituals.useScepter(thor, InteractionHand.MAIN_HAND, false);
        check(Powers.power(thor) == Power.THOR && kept.getCount() == 1 && thor.isAlive(), "a hero is told so, and the core is not used up");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theScepterWithTheProgramNeverHurtsAndUsesNothingUp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        ItemStack program = new ItemStack(ModItems.ULTRON_PROGRAM.get());
        player.setItemInHand(InteractionHand.OFF_HAND, program);
        float health = player.getHealth();
        ScepterRituals.useScepter(player, InteractionHand.MAIN_HAND, false);
        check(player.isAlive() && player.getHealth() == health, "not sneaking: a hint, no explosion");
        player.setShiftKeyDown(true);
        check(ScepterRituals.useScepterOnBlock(player, InteractionHand.MAIN_HAND), "Sneak + use on a block is the Ultron row");
        check(player.isAlive() && player.getHealth() == health && program.getCount() == 1 && Powers.power(player) == Power.NONE,
                "the boss framework is not built yet: nothing was woken, used up or hurt");
        player.setShiftKeyDown(false);
        check(!ScepterRituals.useScepterOnBlock(player, InteractionHand.MAIN_HAND), "an ordinary use on a block is not");
        helper.succeed();
    }

    /** Until the summoning is built the Mad Titan's rows are declined and the Scepter does what it always did. */
    @GameTest(template = ARENA)
    public static void theScepterAndTheTesseractDoWhatTheyDidWhileTheSummoningIsNotBuilt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(5.5, FLOOR, 5.5, Power.SCARLET_WITCH);
        wanda.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        wanda.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TESSERACT.get()));
        check(!ScepterRituals.useScepter(wanda, InteractionHand.MAIN_HAND, false) && wanda.isAlive(),
                "the stone knows Wanda with a Tesseract in her other hand, as before");
        check(!ScepterRituals.useTesseract(wanda, InteractionHand.OFF_HAND), "the Tesseract starts no hold");

        ServerPlayer creative = f.player(9.5, FLOOR, 5.5, GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        creative.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TESSERACT.get()));
        ScepterRituals.useScepter(creative, InteractionHand.MAIN_HAND, false);
        check(creative.isAlive() && Powers.power(creative) == Power.NONE, "and a Creative player is told the stone has nothing for them");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The awakening items

    @GameTest(template = ARENA)
    public static void anAwakeningItemIsHeldThenUsedUpAndAwakens(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        record Item(AwakeningItem item, Power power) {
        }
        for (Item entry : List.of(new Item((AwakeningItem) ModItems.PHOENIX_EMBER.get(), Power.PHOENIX),
                new Item((AwakeningItem) ModItems.INFINITY_GAUNTLET.get(), Power.THANOS),
                new Item((AwakeningItem) ModItems.ESSENCE.get(), Power.APOCALYPSE))) {
            ServerPlayer player = f.player(5.5, FLOOR, 5.5);
            ItemStack stack = new ItemStack(entry.item());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            check(entry.item().power() == entry.power() && entry.item().getUseDuration(stack, player) == 60, entry.item() + " is held for three seconds");
            InteractionResultHolder<ItemStack> used = entry.item().use(level, player, InteractionHand.MAIN_HAND);
            check(used.getResult().consumesAction() && player.isUsingItem(), "the hold begins");
            // Letting go early changes nothing.
            player.stopUsingItem();
            check(Powers.power(player) == Power.NONE && stack.getCount() == 1, "an early release changes nothing");
            entry.item().use(level, player, InteractionHand.MAIN_HAND);
            entry.item().finishUsingItem(stack, level, player);
            check(Powers.power(player) == entry.power(), "the hold's end awakens " + entry.power());
            check(stack.isEmpty(), "and the item is used up");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void anAwakeningItemTurnsAHeroAwayBeforeTheHoldAndCreativeKeepsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer hero = f.player(5.5, FLOOR, 5.5, Power.THOR);
        ItemStack ember = new ItemStack(ModItems.PHOENIX_EMBER.get());
        hero.setItemInHand(InteractionHand.MAIN_HAND, ember);
        InteractionResultHolder<ItemStack> used = ember.getItem().use(level, hero, InteractionHand.MAIN_HAND);
        check(!used.getResult().consumesAction() && !hero.isUsingItem(), "no hold begins for a hero");
        check(ember.getCount() == 1 && Powers.power(hero) == Power.THOR, "and nothing is used up");

        ServerPlayer creative = f.player(9.5, FLOOR, 5.5, GameType.CREATIVE);
        ItemStack essence = new ItemStack(ModItems.ESSENCE.get());
        creative.setItemInHand(InteractionHand.MAIN_HAND, essence);
        essence.getItem().use(level, creative, InteractionHand.MAIN_HAND);
        essence.getItem().finishUsingItem(essence, level, creative);
        check(Powers.power(creative) == Power.APOCALYPSE && essence.getCount() == 1, "in Creative the item is kept");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Area attacks, the hero is held, the boss facade

    @GameTest(template = ARENA)
    public static void anAreaAttackSparesWhatItShouldAndHurtsTheRest(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer owner = f.player(10.5, FLOOR, 10.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(12.5, FLOOR, 10.5));
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(12.5, FLOOR, 12.5));
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(14.5, FLOOR, 12.5));
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(14.5, FLOOR, 10.5));
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(8.5, FLOOR, 12.5));
        Wolf pet = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(8.5, FLOOR, 8.5));
        pet.tame(owner);
        Wolf stray = helper.spawnWithNoFreeWill(EntityType.WOLF, new Vec3(14.5, FLOOR, 8.5));

        check(AreaTargets.mayHurt(owner, zombie, false), "a hostile creature is hurt");
        check(!AreaTargets.mayHurt(owner, owner, true), "the caster never is");
        check(!AreaTargets.mayHurt(owner, villager, false) && !AreaTargets.mayHurt(owner, golem, false) && !AreaTargets.mayHurt(owner, cow, false),
                "villagers, golems and animals are spared by default");
        check(AreaTargets.mayHurt(owner, villager, true) && AreaTargets.mayHurt(owner, cow, true), "unless the hero's option says otherwise");
        check(!AreaTargets.mayHurt(owner, stand, true), "an armour stand never is");
        check(!AreaTargets.mayHurt(owner, pet, true), "the caster's own pet never is");
        check(!AreaTargets.mayHurt(owner, stray, false), "a wolf that minds its business is spared");
        stray.setTarget(owner);
        check(AreaTargets.mayHurt(owner, stray, false), "a creature going after the caster is not");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "area_targets_players")
    public static void anAreaAttackHurtsPlayersOnlyWithPvpAndNeverCreativeOnes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer owner = f.player(10.5, FLOOR, 10.5);
        ServerPlayer other = f.player(12.5, FLOOR, 10.5);
        ServerPlayer creative = f.player(14.5, FLOOR, 10.5, GameType.CREATIVE);
        ServerPlayer spectator = f.player(8.5, FLOOR, 10.5, GameType.SPECTATOR);
        f.pvp(false);
        check(!AreaTargets.mayHurt(owner, other, true), "with PvP off a player is spared whatever the option says");
        f.pvp(true);
        check(AreaTargets.mayHurt(owner, other, false), "with PvP on another player is hurt");
        check(!AreaTargets.mayHurt(owner, creative, true) && !AreaTargets.mayHurt(owner, spectator, true),
                "a Creative or Spectator player never is");
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, FLOOR, 5.5));
        check(AreaTargets.mayHurt(zombie, other, false), "a monster may hurt a player whatever PvP says");
        helper.succeed();
    }

    /** The cocoon covers the hands: a key press does nothing while entombed, a release still goes through. */
    @GameTest(template = ARENA)
    public static void anEntombedHeroCannotPressAKey(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(5.5, FLOOR, 5.5, Power.SCARLET_WITCH);
        wanda.addEffect(new MobEffectInstance(ModEffects.ENTOMBED, 200));
        AbilityHandler.onKey(wanda, 2, true);
        check(!wanda.getAbilities().flying, "G does not take off while entombed");
        AbilityHandler.onKey(wanda, 2, false);
        wanda.removeEffect(ModEffects.ENTOMBED);
        AbilityHandler.onKey(wanda, 2, true);
        check(wanda.getAbilities().flying, "and does once she is free");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void theBossFacadeAnswersAsIfThereWereNoBossYet(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 8.5));
        check(BossFights.active(level) == null && !BossFights.anyActive(level.getServer()), "no fight runs");
        check(!BossFights.isMember(player) && !BossFights.isMinion(zombie) && !BossFights.isHexExempt(player), "nobody is in one");
        check(BossFights.allowsTeleport(level, player, new Vec3(1, 2, 3)), "a teleport is allowed anywhere");
        check(!BossFights.protects(level, helper.absolutePos(new BlockPos(5, 2, 5))), "no block is protected");
        check(!BossFights.telekinesisPress(player) && !BossFights.telekinesisLaunch(player, zombie), "Wanda's telekinesis is hers alone");
        for (BossKind kind : BossKind.values()) {
            check(!BossFights.start(kind, player, true), "no " + kind + " fight can start yet, not even by force");
            check(BossKind.byId(kind.id()) == kind, kind + " is found by its id");
        }
        BossFights.tickLevel(level);
        BossFights.stopAll(level.getServer(), StopReason.COMMAND);
        BossFights.clear();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "boss_framework")
    public static void theBossCommandsStartStopListAndCleanUp(GameTestHelper helper) throws CommandSyntaxException {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        check(run(player, "enhanced boss start thanos") == 0, "a fight that cannot start is a failure");
        check(run(player, "enhanced boss start ultron force") == 0, "even by force, until the framework is built");
        boolean refused = false;
        try {
            run(player, "enhanced boss start nobody");
        } catch (CommandSyntaxException e) {
            refused = true;
        }
        check(refused, "an unknown boss is refused");
        check(run(player, "enhanced boss stop") == 1, "stop works");
        check(run(player, "enhanced boss list") == 0, "no fight is listed");

        var thanos = ModEntities.THANOS.get().create(level);
        var sentry = ModEntities.ULTRON_SENTRY.get().create(level);
        var far = ModEntities.APOCALYPSE.get().create(level);
        Vec3 here = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        thanos.moveTo(here.x, here.y, here.z);
        sentry.moveTo(here.x + 3, here.y, here.z);
        far.moveTo(here.x + 40, here.y, here.z);
        level.addFreshEntity(thanos);
        level.addFreshEntity(sentry);
        level.addFreshEntity(far);
        Zombie bystander = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 12.5));
        check(run(player, String.format(Locale.ROOT, "enhanced boss cleanup %f %f %f 8", here.x, here.y, here.z)) == 2,
                "two boss entities were near");
        check(thanos.isRemoved() && sentry.isRemoved(), "they are gone");
        check(!far.isRemoved() && !bystander.isRemoved(), "the one too far away and an ordinary zombie are not");
        far.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The network

    @GameTest(template = ARENA)
    public static void theProtocolIsFour(GameTestHelper helper) {
        check(ModNetwork.PROTOCOL_VERSION.equals("4"), "3.0 bumps the protocol from 3 to 4, once: " + ModNetwork.PROTOCOL_VERSION);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void everyPayloadOf3RoundTrips(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Vec3 a = new Vec3(1.5, 64.0, -2.25);
        Vec3 b = new Vec3(10, 70, 12);

        ItemStack named = new ItemStack(Items.DIAMOND_SWORD, 1);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Frostmourne"));
        List<ItemStack> worn = new ArrayList<>();
        List<ItemStack> carried = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            worn.add(named.copy());
        }
        for (int i = 0; i < 36; i++) {
            carried.add(i % 3 == 0 ? ItemStack.EMPTY : new ItemStack(Items.IRON_INGOT, i + 1));
        }
        MindReadPayload mind = new MindReadPayload(7, "Magneto", "player", MindReadPayload.OK, MindReadPayload.HOSTILE, "Charles", 17.5F, 20.0F,
                14, 31, worn, carried, "really Loki");
        MindReadPayload mindBack = roundTrip(MindReadPayload.STREAM_CODEC, mind, access);
        check(mindBack.entityId() == 7 && mindBack.name().equals("Magneto") && mindBack.species().equals("player")
                && mindBack.state() == MindReadPayload.OK && mindBack.mood() == MindReadPayload.HOSTILE && mindBack.hostileTo().equals("Charles")
                && mindBack.health() == 17.5F && mindBack.maxHealth() == 20.0F && mindBack.food() == 14 && mindBack.level() == 31
                && mindBack.extra().equals("really Loki"), "the mind read: the fields");
        check(ItemStack.listMatches(mindBack.worn(), worn) && ItemStack.listMatches(mindBack.carried(), carried), "the mind read: the stacks");

        PhoenixBurstPayload burst = new PhoenixBurstPayload(5, a, 135.0F, 0.75F, 24.0F, 123456789L);
        check(roundTrip(PhoenixBurstPayload.STREAM_CODEC, burst, access).equals(burst), "the burst");

        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            points.add(new Vec3(i, i * 0.5, -i));
        }
        for (SpiderFxPayload.Kind kind : SpiderFxPayload.Kind.values()) {
            SpiderFxPayload fx = new SpiderFxPayload(kind, 3, a, b, new Vec3(0, 1, 0), 4, 2.5F, true, -99L, points);
            check(roundTrip(SpiderFxPayload.STREAM_CODEC, fx, access).equals(fx), "the spider fx " + kind);
        }
        SpiderSensePayload sense = new SpiderSensePayload(List.of(1, 20, 300), true);
        check(roundTrip(SpiderSensePayload.STREAM_CODEC, sense, access).equals(sense), "the spider-sense");

        ThanosFxPayload thanos = new ThanosFxPayload(4, -1, 9, 40, 6.5F, points.subList(0, 16));
        check(roundTrip(ThanosFxPayload.STREAM_CODEC, thanos, access).equals(thanos), "Thanos's fx");
        UltronFxPayload ultron = new UltronFxPayload(2, 11, a, b, 7.0F, 30);
        check(roundTrip(UltronFxPayload.STREAM_CODEC, ultron, access).equals(ultron), "Ultron's fx");
        SokoviaLiftPayload lift = new SokoviaLiftPayload(SokoviaLiftPayload.RISE, 12, new BlockPos(-100, 64, 200), 20, 40, 14,
                0xFEEDFACEL, 160);
        check(roundTrip(SokoviaLiftPayload.STREAM_CODEC, lift, access).equals(lift), "the lift");
        ApocalypseFxPayload apocalypse = new ApocalypseFxPayload((byte) 5, 3, a, b, 120, -4);
        check(roundTrip(ApocalypseFxPayload.STREAM_CODEC, apocalypse, access).equals(apocalypse), "Apocalypse's fx");

        // The mind read stays far below the limit of a clientbound payload (1 MiB) even with full stacks.
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        MindReadPayload.STREAM_CODEC.encode(buf, mind);
        check(buf.readableBytes() < 64 * 1024, "the mind read is " + buf.readableBytes() + " bytes");
        helper.succeed();
    }

    private static <T> T roundTrip(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T value, RegistryAccess access) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        codec.encode(buf, value);
        T decoded = codec.decode(buf);
        check(buf.readableBytes() == 0, "the codec read everything it wrote (" + buf.readableBytes() + " bytes left)");
        return decoded;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The lang files and the release gates

    /** The lanes' regions of the lang files: the same sentinel lines, in the same order, in both languages. */
    @GameTest(template = ARENA)
    public static void theLangSentinelsAreTheSameInBothLanguagesAndInHeroOrder(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        List<String> english = sentinels(lang(mod, "en_us"));
        List<String> hebrew = sentinels(lang(mod, "he_il"));
        check(english.equals(hebrew), "the sentinels differ: " + english + " / " + hebrew);
        check(english.equals(List.of("_lane.shared", "_lane.phoenix", "_lane.spider", "_lane.thanos", "_lane.ultron", "_lane.apocalypse", "_end")),
                "the lanes are in Eilon's order: " + english);
        helper.succeed();
    }

    private static List<String> sentinels(JsonObject lang) {
        return lang.keySet().stream().filter(key -> key.startsWith("_")).toList();
    }

    /**
     * The release gate for the sentinel lines of the lang files: they are scaffolding of the 3.0 work and must be gone
     * from the release (a version of 3.0.0 or later). Passes while the version is older.
     */
    @GameTest(template = ARENA)
    public static void noLangSentinelsRemainAtTheRelease(GameTestHelper helper) throws IOException {
        if (isReleaseOf3()) {
            IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
            check(sentinels(lang(mod, "en_us")).isEmpty() && sentinels(lang(mod, "he_il")).isEmpty(),
                    "the lanes' sentinel lines are still in the lang files");
        }
        helper.succeed();
    }

    /**
     * The release gate for the animation tables: every ability, toggle and state of the five heroes is acted by a
     * gesture of its own or explained, not pending. Passes while the version is older than 3.0.0.
     */
    @GameTest(template = ARENA)
    public static void noAnimationIsPendingAtTheRelease(GameTestHelper helper) {
        List<String> pending = pendingAnimation();
        if (isReleaseOf3()) {
            check(pending.isEmpty(), "still pending: " + pending);
        }
        helper.succeed();
    }

    /** The routes of the five heroes that still stand in for the lanes' work. */
    private static List<String> pendingAnimation() {
        List<String> pending = new ArrayList<>();
        for (Power hero : NEW_HEROES) {
            for (int ability = 1; ability <= PowerData.ABILITIES; ability++) {
                if (GestureRoutes.acting(hero, ability).equals(GestureRoutes.PENDING_ACTING)) {
                    pending.add(hero + " ability " + ability);
                }
            }
        }
        for (Toggle toggle : Toggle.values()) {
            if (toggle.ordinal() >= Toggle.MIND_READING.ordinal() && GestureRoutes.toggle(Power.PHOENIX, toggle, true) == GestureRoutes.PENDING_GESTURE) {
                pending.add("toggle " + toggle);
            }
        }
        for (Live live : Live.values()) {
            String reason = GestureRoutes.reason(live);
            if (reason != null && reason.startsWith(GestureRoutes.PENDING_REASON)) {
                pending.add("state " + live);
            }
        }
        return pending;
    }

    /**
     * Every ability of the five heroes is acted by a gesture or has a reason why not (the same rule as
     * {@code AnimationMathTests.everyAbilityIsActedOrExplained}, which checks the heroes before them); until a lane
     * builds the hero, the reason is "pending lane".
     */
    @GameTest(template = ARENA)
    public static void everyAbilityOfTheNewHeroesIsActedOrExplained(GameTestHelper helper) {
        for (Power hero : NEW_HEROES) {
            for (int ability = 1; ability <= PowerData.ABILITIES; ability++) {
                String how = GestureRoutes.acting(hero, ability);
                String where = hero + " ability " + ability + " (" + how + ")";
                if (how.startsWith("none: ")) {
                    check(how.length() > 12, where + " needs a reason");
                } else if (how.equals("cooldown")) {
                    check(GestureRoutes.cooldown(hero, ability, 1) != null || GestureRoutes.cooldown(hero, ability, 99) != null, where + " has no gesture");
                } else if (how.startsWith("flag:")) {
                    check(GestureRoutes.flag(hero, GestureRoutes.Flag.valueOf(how.substring(5)), true, false, false) != null, where + " has no gesture");
                } else if (how.startsWith("toggle:")) {
                    check(GestureRoutes.toggle(hero, Toggle.valueOf(how.substring(7)), true) != null, where + " has no gesture");
                } else if (how.startsWith("live:")) {
                    check(GestureRoutes.live(hero, Live.valueOf(how.substring(5)), true) != null, where + " has no gesture");
                } else if (how.equals("held")) {
                    check(GestureRoutes.grab(hero) != null && GestureRoutes.fling(hero) != null, where + " has no grab and fling");
                } else {
                    check(how.equals("hammer") || how.equals("hex") || how.equals("flight"), where + " is not a known way of acting");
                }
            }
        }
        // The momentary states of the new heroes have a gesture or a reason, whatever the lanes have done so far.
        for (Live live : Live.values()) {
            if (live.ordinal() < Live.PHOENIX_CHARGING.ordinal()) {
                continue; // (the states of the heroes before them are checked by AnimationMathTests)
            }
            boolean acted = false;
            for (Power hero : NEW_HEROES) {
                acted |= GestureRoutes.live(hero, live, true) != null;
            }
            check(acted || GestureRoutes.reason(live) != null, "the state " + live + " has neither a gesture nor a reason");
        }
        helper.succeed();
    }

    /** Whether the mod is a release of 3.0 or later (not a 2.x version, which is what the work in progress carries). */
    private static boolean isReleaseOf3() {
        ArtifactVersion version = ModList.get().getModContainerById(Enhanced.MODID).orElseThrow().getModInfo().getVersion();
        return version.getMajorVersion() >= 3;
    }

    private static JsonObject lang(IModFile mod, String name) throws IOException {
        Path file = mod.findResource("assets", Enhanced.MODID, "lang", name + ".json");
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** Runs a command as the player (with operator rights) and returns what it returned. */
    private static int run(ServerPlayer player, String command) throws CommandSyntaxException {
        MinecraftServer server = player.getServer();
        CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
        return server.getCommands().getDispatcher().execute(command, source);
    }

    private SkeletonTests() {
    }
}
