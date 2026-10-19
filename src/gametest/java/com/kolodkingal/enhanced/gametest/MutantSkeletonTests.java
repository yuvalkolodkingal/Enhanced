package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.HeroServers;
import com.kolodkingal.enhanced.asgard.npc.AsgardTrades;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.config.CottageConfig;
import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.config.HealingConfig;
import com.kolodkingal.enhanced.config.LocatorConfig;
import com.kolodkingal.enhanced.config.SchoolLabConfig;
import com.kolodkingal.enhanced.config.WolverineConfig;
import com.kolodkingal.enhanced.config.XavierConfig;
import com.kolodkingal.enhanced.cottage.CottageProgress;
import com.kolodkingal.enhanced.custom.Catalog;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroDamage;
import com.kolodkingal.enhanced.custom.HeroItems;
import com.kolodkingal.enhanced.custom.PowerModule;
import com.kolodkingal.enhanced.custom.Resource;
import com.kolodkingal.enhanced.custom.StoneRow;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.deadpool.DeadpoolState;
import com.kolodkingal.enhanced.deadpool.PodTreatment;
import com.kolodkingal.enhanced.finders.CerebroRoom;
import com.kolodkingal.enhanced.finders.LocatorState;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.healing.HealingFactor.Defeat;
import com.kolodkingal.enhanced.healing.HealingState;
import com.kolodkingal.enhanced.network.CerebroPayload;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.network.HealingFxPayload;
import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.network.ScentPayload;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.network.WolverineFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.wolverine.WolverineState;
import com.kolodkingal.enhanced.world.CottagePlan;
import com.kolodkingal.enhanced.world.SchoolLabPlan;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The skeleton of 3.7, Wolverine, Deadpool, the X-Men lower lab, the Stone Locator and Agatha's Cottage (S0): the two
 * heroes appended after the Hulk, the bits and payloads (protocol 11), the 176 hero rows, the healing factor's rule
 * (the truth table of {@link HealingFactor#defeat} over every damage type), the cottage's roles appended to the
 * Asgardians', the fixed points of the two placeholder templates, the data and the configs' defaults. Batch {@code
 * mutant_skel}; a test that changes the config has the batch {@code mutant_skel_cfg}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class MutantSkeletonTests {
    static final String BATCH = "mutant_skel";
    static final String CFG = "mutant_skel_cfg";

    /** The healers' killers of the plan's 2.3, by group, as the 3.6 damage types they name. */
    static final Map<Defeat, Set<String>> KILLERS = Map.of(
            Defeat.STONES, Set.of("stone_power", "stone_blast", "stone_soul", "mind_stone", "mind_stone_beam",
                    "power_stone_beam", "power_stone_explosion", "snap_dust"),
            Defeat.CHAOS, Set.of("chaos_beam", "hex_wall", "hex_bolt", "hex_beam"),
            Defeat.ODIN, Set.of("odinforce", "gungnir"),
            Defeat.PHOENIX, Set.of("phoenix_burst"),
            Defeat.DARKHOLD, Set.of("darkhold_beam", "agatha_drain", "agatha_runes"));

    // ---- the heroes, the bits, the protocol ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void wolverineAndDeadpoolComeAfterTheHulk(GameTestHelper helper) {
        Power[] all = Power.values();
        check(all[Power.HULK.ordinal() + 1] == Power.WOLVERINE && all[Power.HULK.ordinal() + 2] == Power.DEADPOOL
                && all.length == Power.HULK.ordinal() + 3, "…HULK, WOLVERINE, DEADPOOL (the stream sends the ordinal)");
        check(Power.HULK.ordinal() == 22, "nobody before them moved");
        check(Power.WOLVERINE.color == 0xF2C230 && Power.WOLVERINE.glow == 0xD9DEE3
                && Power.WOLVERINE.titleColor() == 0xF2C230, "Wolverine's colours");
        check(Power.DEADPOOL.color == 0xC8102E && Power.DEADPOOL.glow == 0xFF8A8A
                && Power.DEADPOOL.titleColor() == 0xD6262E, "Deadpool's");
        for (Power hero : List.of(Power.WOLVERINE, Power.DEADPOOL)) {
            check(!hero.canFly() && hero.flightAbility() == -1 && hero.flightSpeed() == 0.05F, hero + " does not fly");
            check(HeroServers.of(hero) != null && HeroServers.of(hero).power() == hero, hero + " has a server plug-in");
            check(HeroClients.of(hero) != null, hero + " has a client plug-in");
            List<Variant> looks = Variants.of(hero);
            check(looks.get(looks.size() - 1).id().equals("own"), hero + "'s last look is the player's own");
        }
        check(Variants.of(Power.WOLVERINE).stream().map(Variant::id).toList()
                .equals(List.of("classic", "x_men_2000", "own")), "Wolverine's three looks");
        check(Variants.of(Power.DEADPOOL).stream().map(Variant::id).toList()
                .equals(List.of("classic", "trainee", "kidpool", "own")), "Deadpool's four looks");
        check(Traits.of(Power.WOLVERINE).equals(Set.of(Trait.HEALING_FACTOR, Trait.METAL_BODY)),
                "Wolverine heals and is metal");
        check(Traits.of(Power.DEADPOOL).equals(Set.of(Trait.HEALING_FACTOR)), "Deadpool heals");
        for (Power other : Power.values()) {
            if (other != Power.WOLVERINE && other != Power.DEADPOOL) {
                check(!Traits.of(other).contains(Trait.HEALING_FACTOR), other + " has no healing factor");
            }
        }
        check(HeroItems.of(Power.DEADPOOL, "classic").equals(List.of(ModItems.CHIMICHANGA.get()))
                && HeroItems.of(Power.WOLVERINE, "classic").isEmpty(), "Deadpool's chimichangas, nothing for Logan");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBitsFitAndTheProtocolIsEleven(GameTestHelper helper) {
        check(ModNetwork.PROTOCOL_VERSION.equals("11"), "protocol 11");
        check(Live.values().length == 58 && Live.values().length <= 63, "Live: 53 + 1 + 2 + 2 = 58: "
                + Live.values().length);
        check(Toggle.values().length == 22 && Toggle.values().length <= 32, "Toggle: 19 + 3 = 22: "
                + Toggle.values().length);
        check(Live.HEALING_DOWN.ordinal() == 53 && Live.WOLVERINE_BERSERK.ordinal() == 54
                && Live.DEADPOOL_AIMING.ordinal() == 57, "the states of 3.7 are appended");
        check(Toggle.CLAWS_OUT.ordinal() == 19 && Toggle.SCENT_TRACK.ordinal() == 21, "and the toggles");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void thePayloadsOfThreeSevenRoundTrip(GameTestHelper helper) {
        List<Vec3> points = List.of(Vec3.ZERO, new Vec3(1, 2, 3));
        roundTrip(HealingFxPayload.STREAM_CODEC, new HealingFxPayload(HealingFxPayload.CUT, 7, 3), "a healing moment");
        roundTrip(WolverineFxPayload.STREAM_CODEC, new WolverineFxPayload(WolverineFxPayload.POUNCE_HIT, 7, points, 2),
                "a claw");
        roundTrip(DeadpoolFxPayload.STREAM_CODEC, new DeadpoolFxPayload(DeadpoolFxPayload.TAUNT, 7, points, 11),
                "a joke");
        roundTrip(LabFxPayload.STREAM_CODEC, new LabFxPayload(LabFxPayload.LIFT_DOWN, new BlockPos(1, -40, 3)),
                "the lift");
        roundTrip(ScentPayload.STREAM_CODEC, new ScentPayload(List.of(new ScentPayload.Scent(4, ScentPayload.MONSTER),
                new ScentPayload.Scent(9, ScentPayload.TRACKED)), List.of(new ScentPayload.Trail(points)), 200),
                "a scent");
        roundTrip(StoneFxPayload.STREAM_CODEC, new StoneFxPayload(StoneFxPayload.LOCATOR_TRAIL, 7, points,
                StoneKind.TIME.ordinal()), "the Locator's spark trail");
        CerebroPayload old = new CerebroPayload(List.of(new CerebroPayload.Mind("Eilon", 3, new Vec3(1, 2, 3))), 100);
        check(!old.amplified() && old.sites().isEmpty() && old.minds().get(0).dimension().isEmpty(),
                "Cerebro of 3.3 is not amplified, in his own dimension");
        roundTrip(CerebroPayload.STREAM_CODEC, old, "Cerebro as before");
        roundTrip(CerebroPayload.STREAM_CODEC, new CerebroPayload(List.of(new CerebroPayload.Mind("Peter", 24,
                new Vec3(5, 70, -5), "minecraft:the_nether")), 600, true, List.of(new CerebroPayload.Site(
                StoneKind.SOUL.ordinal(), new BlockPos(100, 40, -300)))), "the amplified Cerebro");
        roundTrip(HealingState.STREAM_CODEC, new HealingState(1234L, 99L, HealingState.TIP_SHOWN), "the healing state");
        roundTrip(WolverineState.STREAM_CODEC, new WolverineState(500L, 3), "Wolverine's state");
        roundTrip(DeadpoolState.STREAM_CODEC, new DeadpoolState(7, 40L, Optional.of(new UUID(1, 2)), 2400L),
                "Deadpool's state");
        roundTrip(CottageProgress.STREAM_CODEC, new CottageProgress(1, 3L, -1L, 2L), "the cottage progress");
        roundTrip(LocatorState.STREAM_CODEC, new LocatorState(Optional.of(StoneKind.TIME), Optional.empty(),
                Optional.of(GlobalPos.of(Level.NETHER, new BlockPos(1, 2, 3))), LocatorState.FOUND, 810),
                "the Locator's state");
        check(size(HealingFxPayload.STREAM_CODEC, new HealingFxPayload(HealingFxPayload.DOWN, Integer.MAX_VALUE,
                Integer.MAX_VALUE)) <= 256 && size(LabFxPayload.STREAM_CODEC, new LabFxPayload(LabFxPayload.POD_DONE,
                new BlockPos(29_999_999, 319, -29_999_999))) <= 256, "the small effects stay under 256 bytes");
        List<Vec3> ten = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ten.add(new Vec3(-29_999_999.5 + i, 319.5, 29_999_999.5));
        }
        check(size(WolverineFxPayload.STREAM_CODEC, new WolverineFxPayload(WolverineFxPayload.CLIMB, 1 << 30, ten,
                1 << 30)) <= 256, "an effect of ten points stays under 256 bytes");
        helper.succeed();
    }

    private static <T> void roundTrip(StreamCodec<ByteBuf, T> codec, T value, String what) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        check(codec.decode(buf).equals(value), what + " round-trips");
    }

    private static <T> int size(StreamCodec<ByteBuf, T> codec, T value) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        return buf.readableBytes();
    }

    // ---- the Hero Creator ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRowsAre176(GameTestHelper helper) throws IOException {
        check(HeroAbility.values().length == 176, "154 + 12 + 10 rows: " + HeroAbility.values().length);
        check(HeroAbility.values()[HeroAbility.WOLVERINE_SLASH.ordinal() - 1] == HeroAbility.HULK_BODY,
                "Wolverine after the Hulk");
        check(HeroAbility.values()[HeroAbility.DEADPOOL_KATANAS.ordinal() - 1] == HeroAbility.WOLVERINE_BODY,
                "Deadpool after Wolverine");
        check(HeroAbility.values()[HeroAbility.values().length - 1] == HeroAbility.DEADPOOL_SMOKE,
                "their variants come last");
        for (Power hero : List.of(Power.WOLVERINE, Power.DEADPOOL)) {
            List<HeroAbility> rows = HeroAbility.of(hero).stream().filter(a -> !a.isVariant()).toList();
            List<HeroAbility> variants = HeroAbility.of(hero).stream().filter(HeroAbility::isVariant).toList();
            check(rows.size() == 6 && variants.size() == 5, hero + " has six rows and five variants");
            for (int key = 0; key <= 5; key++) {
                check(HeroAbility.at(hero, key) != null, hero + " has a row on key " + key);
            }
            check(HeroAbility.at(hero, 0).traits().contains(Trait.HEALING_FACTOR)
                    && HeroAbility.at(hero, 0).costPerPoint == 3, hero + "'s body heals and costs 3");
        }
        check(HeroAbility.WOLVERINE_BODY.traits().equals(Set.of(Trait.HEALING_FACTOR, Trait.METAL_BODY)),
                "the adamantium body is metal");
        check(HeroAbility.WOLVERINE_CLAWS.heroId().equals("wolverine") && HeroAbility.DEADPOOL_TAUNT.id()
                .equals("deadpool.taunt"), "the ids");
        check(HeroAbility.WOLVERINE_CLAW_CLIMB.requires().equals(List.of("wolverine.claws")),
                "the climb needs the claws");
        check(HeroAbility.DEADPOOL_KATANAS.resources(HeroAbility.BOTH).contains(Resource.SHIELD)
                && HeroAbility.DEADPOOL_PARRY.resources(null).contains(Resource.SHIELD), "the Parry is his shield");
        Set<String> ids = new HashSet<>();
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        for (HeroAbility a : HeroAbility.values()) {
            check(ids.add(a.id()), a.id() + " is unique");
            if (a.isVariant()) {
                check(a.parent() != null && a.parent().hasAlt() && a.parent().variant() == a,
                        a.id() + "'s parent has a Sneak variant, and it is this row");
            }
            if (a.hero == Power.WOLVERINE || a.hero == Power.DEADPOOL) {
                for (JsonObject lang : List.of(en, he)) {
                    check(lang.has(a.nameKey()) && lang.has(a.descKey()) && (!a.hasAlt() || a.isVariant()
                            || lang.has(a.altKey())), a.id() + " has its names");
                }
            }
        }
        check(Catalog.all().size() == PowerModule.values().length + HeroAbility.values().length
                + StoneRow.values().length, "the catalogue is the three lists");
        Map<ResourceKey<DamageType>, HeroAbility> dealt = Map.of(ModDamageTypes.WOLVERINE_CLAWS,
                HeroAbility.WOLVERINE_SLASH, ModDamageTypes.WOLVERINE_POUNCE, HeroAbility.WOLVERINE_POUNCE,
                ModDamageTypes.DEADPOOL_KATANA, HeroAbility.DEADPOOL_KATANAS, ModDamageTypes.DEADPOOL_BULLET,
                HeroAbility.DEADPOOL_PISTOLS);
        for (Map.Entry<ResourceKey<DamageType>, HeroAbility> e : dealt.entrySet()) {
            CustomHero hero = CustomKitSupport.def().pick(e.getValue().id(), 5).key(e.getValue().home,
                    e.getValue().id()).build();
            check(HeroDamage.abilityOf(e.getKey(), CustomKitSupport.data(hero).kit()) == e.getValue(),
                    e.getKey().location() + " is dealt by " + e.getValue().id());
        }
        helper.succeed();
    }

    // ---- the healing factor ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theKillerTagsLoadAndNameRealTypes(GameTestHelper helper) {
        Registry<DamageType> types = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Map<Defeat, TagKey<DamageType>> tags = Map.of(Defeat.STONES, ModTags.DEFEATS_HEALING_STONES, Defeat.CHAOS,
                ModTags.DEFEATS_HEALING_CHAOS, Defeat.ODIN, ModTags.DEFEATS_HEALING_ODIN, Defeat.PHOENIX,
                ModTags.DEFEATS_HEALING_PHOENIX, Defeat.DARKHOLD, ModTags.DEFEATS_HEALING_DARKHOLD);
        for (Map.Entry<Defeat, TagKey<DamageType>> e : tags.entrySet()) {
            Set<String> members = new HashSet<>();
            for (Holder<DamageType> holder : types.getTagOrEmpty(e.getValue())) {
                members.add(holder.unwrapKey().orElseThrow().location().getPath());
            }
            check(members.equals(KILLERS.get(e.getKey())), e.getValue().location() + " holds " + members);
            for (String name : members) {
                check(types.containsKey(Enhanced.id(name)), name + " is a real damage type");
            }
        }
        check(MobEffects.POISON.is(ModTags.STOPS_HEALING) && !MobEffects.WITHER.is(ModTags.STOPS_HEALING),
                "Poison stops the healing, Wither does not");
        for (String name : List.of("wolverine_claws", "deadpool_katana")) {
            check(types.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, Enhanced.id(name)))
                    .is(ResourceKey.create(Registries.DAMAGE_TYPE, Enhanced.id(name))), name + " exists");
        }
        check(types.getHolderOrThrow(ModDamageTypes.DEADPOOL_BULLET).is(DamageTypeTags.IS_PROJECTILE),
                "the bullet is a projectile");
        helper.succeed();
    }

    /**
     * The rule of 2.3 for every damage type of the game and the mod, healer active or poisoned, under the defaults and
     * with each group switched: the void and {@code /kill} always kill, poison lets anything through, each group's tag
     * kills while its option is on, and nothing else does.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theLethalForHealerTruthTable(GameTestHelper helper) {
        Registry<DamageType> types = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        List<HealingFactor.Rules> rules = new ArrayList<>(List.of(HealingFactor.Rules.DEFAULTS,
                new HealingFactor.Rules(false, false, false, false, false, false),
                new HealingFactor.Rules(true, true, true, true, true, true)));
        for (int off = 0; off < 6; off++) {
            boolean[] on = {true, true, true, true, false, true};
            on[off] = !on[off];
            rules.add(new HealingFactor.Rules(on[0], on[1], on[2], on[3], on[4], on[5]));
        }
        int cases = 0;
        for (Holder.Reference<DamageType> type : types.holders().toList()) {
            String name = type.key().location().getNamespace().equals(Enhanced.MODID)
                    ? type.key().location().getPath() : "";
            boolean bypasses = type.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && name.isEmpty();
            check(HealingFactor.isVoidOrKill(type) == bypasses, type.key().location() + ": the void or /kill?");
            Predicate<TagKey<DamageType>> is = type::is;
            for (HealingFactor.Rules r : rules) {
                for (boolean active : new boolean[]{true, false}) {
                    for (boolean fall : new boolean[]{false, true}) {
                        Defeat want = bypasses ? Defeat.ALWAYS : !active ? Defeat.POISON
                                : r.stones() && KILLERS.get(Defeat.STONES).contains(name) ? Defeat.STONES
                                : r.chaos() && KILLERS.get(Defeat.CHAOS).contains(name) ? Defeat.CHAOS
                                : r.odin() && KILLERS.get(Defeat.ODIN).contains(name) ? Defeat.ODIN
                                : r.phoenix() && KILLERS.get(Defeat.PHOENIX).contains(name) ? Defeat.PHOENIX
                                : r.darkhold() && KILLERS.get(Defeat.DARKHOLD).contains(name) ? Defeat.DARKHOLD
                                : r.vormir() && fall ? Defeat.VORMIR : null;
                        Defeat got = HealingFactor.defeat(is, bypasses, active, r, fall);
                        check(got == want, type.key().location() + " " + r + " active " + active + " fall " + fall
                                + ": " + got + ", not " + want);
                        cases++;
                    }
                }
            }
        }
        check(cases > 1000, "the table is the whole registry: " + cases);
        check(HealingFactor.defeat(t -> false, false, true, HealingFactor.Rules.DEFAULTS, false) == null,
                "a zombie's blow, a fall, Thor's lightning: no");
        check(HealingFactor.Defeat.STONES.messageKey().equals("message.enhanced.healing.defeated.stones"),
                "the death line's key");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theHealersAreTheHealers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = f.player(3.5, FLOOR, 3.5, Power.WOLVERINE);
        ServerPlayer wade = f.player(6.5, FLOOR, 3.5, Power.DEADPOOL);
        ServerPlayer hulk = f.player(9.5, FLOOR, 3.5, Power.HULK);
        ServerPlayer custom = CustomKitSupport.hero(f, 12.5, 3.5, CustomKitSupport.def().pick("wolverine.body", 5)
                .build());
        check(HealingFactor.has(logan) && HealingFactor.has(wade) && HealingFactor.has(custom)
                && !HealingFactor.has(hulk), "the two healers and a custom hero with the body, not the Hulk");
        check(HealingFactor.active(logan) && !HealingFactor.isDown(logan), "on, and up");
        check(!HealingFactor.lethalForHealer(helper.getLevel().damageSources().generic(), logan),
                "a plain blow does not kill him");
        check(HealingFactor.lethalForHealer(helper.getLevel().damageSources().fellOutOfWorld(), logan),
                "the void does");
        check(HealingFactor.lethalForHealer(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM),
                logan), "the Scarlet Witch's chaos does");
        check(!HealingFactor.lethalForHealer(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM),
                logan), "Agatha's Darkhold does not, by default");
        check(HealingFactor.lethalForHealer(helper.getLevel().damageSources().generic(), hulk),
                "anything kills one who is no healer");
        check(!HealingFactor.isSacrificeFall(logan, helper.getLevel().damageSources().fall()),
                "a fall far from Vormir is no sacrifice");
        wade.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        check(HealingFactor.suppressed(wade) && !HealingFactor.active(wade) && HealingFactor.has(wade),
                "poison switches his healing off");
        check(HealingFactor.defeat(helper.getLevel().damageSources().generic(), wade) == Defeat.POISON,
                "and then a plain blow kills him");
        wade.removeAllEffects();
        check(HealingFactor.active(wade), "milk (or the end of it) turns it back on");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CFG)
    public static void theHealingOptionsSwitchItsRules(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer logan = f.player(3.5, FLOOR, 3.5, Power.WOLVERINE);
        f.config(HealingConfig.DARKHOLD_DEFEATS, true);
        check(HealingFactor.defeat(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.DARKHOLD_BEAM), logan)
                == Defeat.DARKHOLD, "darkholdDefeats on: the Darkhold kills him");
        f.config(HealingConfig.CHAOS_DEFEATS, false);
        check(!HealingFactor.lethalForHealer(ModDamageTypes.source(helper.getLevel(), ModDamageTypes.CHAOS_BEAM),
                logan), "chaosDefeats off: chaos spares him");
        f.config(HealingConfig.POISON_STOPS_HEALING, false);
        logan.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        check(HealingFactor.active(logan), "poisonStopsHealing off: poison does not stop it");
        ServerPlayer custom = CustomKitSupport.hero(f, 8.5, 3.5, CustomKitSupport.def().pick("deadpool.body", 5)
                .build());
        f.config(HealingConfig.CUSTOM_HEROES, false);
        check(!HealingFactor.has(custom) && HealingFactor.has(logan), "customHeroes off: only the built-in heroes");
        f.config(HealingConfig.ENABLED, false);
        check(!HealingFactor.has(logan), "enabled off: nobody");
        helper.succeed();
    }

    // ---- the cottage's family, the seams ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theCottageRolesAreAppended(GameTestHelper helper) {
        Role[] all = Role.values();
        List<Role> asgard = List.of(Role.HEIMDALL, Role.ODIN, Role.FRIGGA, Role.SIF, Role.VALKYRIE, Role.VOLSTAGG,
                Role.FANDRAL, Role.HOGUN, Role.EINHERJAR, Role.CITIZEN);
        for (int i = 0; i < asgard.size(); i++) {
            check(all[i] == asgard.get(i) && !all[i].cottage(), asgard.get(i) + " kept its ordinal " + i);
        }
        check(all.length == 13 && all[10] == Role.AGATHA && all[11] == Role.NICKY && all[12] == Role.RIO,
                "AGATHA, NICKY, RIO appended");
        check(Role.AGATHA.cottage() && Role.NICKY.cottage() && Role.RIO.cottage(), "the cottage's three");
        check(Role.AGATHA.hero() == Power.AGATHA && Role.AGATHA.yieldsToHero(), "one Agatha: she yields to a player");
        check(Role.NICKY.hero() == null && Role.RIO.hero() == null, "Nicky and Rio stay");
        check(Role.NICKY.scale() == 0.75F && !Role.NICKY.trader() && Role.AGATHA.trader() && Role.RIO.trader()
                && !Role.AGATHA.warrior(), "Nicky is small; Agatha and Rio trade; nobody fights");
        for (Role role : List.of(Role.AGATHA, Role.RIO)) {
            MerchantOffers offers = AsgardTrades.offers(role);
            check(!offers.isEmpty(), role + " trades from the start");
        }
        AsgardianEntity agatha = ModEntities.COTTAGE_RESIDENT.get().create(helper.getLevel());
        check(agatha != null, "the cottage_resident type makes the Asgardians' class");
        agatha.setRole(Role.AGATHA, 0);
        check(agatha.getType() == ModEntities.COTTAGE_RESIDENT.get() && agatha.role() == Role.AGATHA
                && agatha.getAttributeValue(Attributes.MAX_HEALTH) == 30.0, "she is Agatha, with her health");
        check(agatha.getType().is(ModTags.GRAB_IMMUNE) && !agatha.getType().is(ModTags.ASGARDIANS),
                "grab-immune, not one of Asgard's people");
        agatha.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSeamsAreNeutralUntilTheLanes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer xavier = f.player(3.5, FLOOR, 3.5, Power.PROFESSOR_X);
        check(!CerebroRoom.amplify(xavier), "no console amplifies Cerebro yet");
        check(CerebroRoom.useConsole(xavier, new BlockPos(1, 2, 3)) == InteractionResult.PASS, "the console waits");
        ServerPlayer plain = f.player(6.5, FLOOR, 3.5);
        PodTreatment.use(plain, new BlockPos(1, 2, 3));
        check(Powers.power(plain) == Power.NONE, "the pod makes nobody Deadpool yet");
        ItemStack chimichanga = new ItemStack(ModItems.CHIMICHANGA.get());
        FoodProperties food = chimichanga.getFoodProperties(plain);
        check(food != null && food.nutrition() == 8, "a chimichanga feeds 8");
        check(LocatorState.EMPTY.equals(new ItemStack(ModItems.STONE_LOCATOR.get())
                .get(ModDataComponents.LOCATOR.get())),
                "a Locator knows nothing yet");
        check(ModItems.ADAMANTIUM_INJECTION.get().getDefaultMaxStackSize() == 1, "one injection at a time");
        helper.succeed();
    }

    // ---- the placeholder templates' fixed points ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theLabsFixedPointsHoldTheirBlocks(GameTestHelper helper) throws IOException {
        Map<BlockPos, String> blocks = template(helper, SchoolLabPlan.TEMPLATE);
        check(size(helper, SchoolLabPlan.TEMPLATE).equals(List.of(45, 28, 42)), "the lab is 45 x 28 x 42");
        check(SchoolLabPlan.LAB_SIZE.getX() == 45 && SchoolLabPlan.LAB_SIZE.getY() == 28
                && SchoolLabPlan.LAB_SIZE.getZ() == 42, "and so says the plan");
        Map<BlockPos, String> want = new HashMap<>();
        for (BlockPos pad : SchoolLabPlan.LAB_PADS) {
            want.put(pad, "enhanced:elevator_pad");
        }
        want.put(SchoolLabPlan.CURE_STATION, "enhanced:cure_station");
        want.put(SchoolLabPlan.ADAMANTIUM_RACK, "enhanced:adamantium_rack");
        want.put(SchoolLabPlan.WEAPON_X_POD, "enhanced:weapon_x_pod");
        want.put(SchoolLabPlan.WEAPON_X_POD.above(), "enhanced:weapon_x_pod");
        want.put(SchoolLabPlan.CEREBRO_DOOR, "enhanced:x_door_round");
        want.put(SchoolLabPlan.CEREBRO_CONSOLE, "enhanced:cerebro_console");
        want.put(new BlockPos(SchoolLabPlan.WALKWAY.minX(), SchoolLabPlan.WALKWAY.minY(), SchoolLabPlan.WALKWAY.minZ()),
                "minecraft:iron_block");
        want.put(new BlockPos(SchoolLabPlan.TANK_BOX.minX(), SchoolLabPlan.TANK_BOX.minY(),
                SchoolLabPlan.TANK_BOX.minZ()),
                "minecraft:glass");
        for (Map.Entry<BlockPos, String> e : want.entrySet()) {
            check(e.getValue().equals(blocks.get(e.getKey())), e.getKey() + " holds " + e.getValue() + ", not "
                    + blocks.get(e.getKey()));
        }
        check("minecraft:air".equals(blocks.get(SchoolLabPlan.CEREBRO_CENTER)), "the sphere's middle is open");
        check(SchoolLabPlan.inSchool(BlockPos.ZERO).equals(new BlockPos(1, -31, 1))
                && SchoolLabPlan.LAB_ORIGIN.getY() + SchoolLabPlan.LAB_SIZE.getY() <= -3,
                "under the school, three layers below its cellar");
        check(SchoolLabPlan.STUDY_SHELVES.size() == 4 && SchoolLabPlan.STUDY_PADS.size() == 2,
                "four shelves and two pads in the study");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theCottagesFixedPointsHoldTheirBlocks(GameTestHelper helper) throws IOException {
        Map<BlockPos, String> blocks = template(helper, CottagePlan.TEMPLATE);
        check(size(helper, CottagePlan.TEMPLATE).equals(List.of(27, 18, 25)), "the cottage is 27 x 18 x 25");
        check("enhanced:witches_road_door".equals(blocks.get(CottagePlan.ROAD_DOOR)), "the Road door in the cellar");
        check(blocks.getOrDefault(CottagePlan.GATE, "").endsWith("_fence_gate"), "the gate");
        for (BlockPos seat : CottagePlan.TABLE_SEATS) {
            check("minecraft:oak_stairs".equals(blocks.get(seat)), "a chair at " + seat);
        }
        check(CottagePlan.TABLE_SEATS.size() == 4 && CottagePlan.BEDS.size() == 3, "four chairs, three beds");
        for (BlockPos bed : CottagePlan.BEDS) {
            check(blocks.getOrDefault(bed, "").endsWith("_bed"), "a bed at " + bed);
        }
        for (BlockPos home : List.of(CottagePlan.AGATHA_HOME, CottagePlan.NICKY_HOME, CottagePlan.RIO_HOME,
                CottagePlan.HUTCH)) {
            check("minecraft:air".equals(blocks.get(home)) && blocks.containsKey(home.below())
                    && !"minecraft:air".equals(blocks.get(home.below())), "one stands at " + home);
        }
        helper.succeed();
    }

    private static Map<BlockPos, String> template(GameTestHelper helper, ResourceLocation template) throws IOException {
        CompoundTag root = nbt(helper, template);
        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
        Map<BlockPos, String> blocks = new HashMap<>();
        for (Tag t : root.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            blocks.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    palette.getCompound(b.getInt("state")).getString("Name"));
        }
        return blocks;
    }

    private static List<Integer> size(GameTestHelper helper, ResourceLocation template) throws IOException {
        ListTag size = nbt(helper, template).getList("size", Tag.TAG_INT);
        return List.of(size.getInt(0), size.getInt(1), size.getInt(2));
    }

    private static CompoundTag nbt(GameTestHelper helper, ResourceLocation template) throws IOException {
        MinecraftServer server = helper.getLevel().getServer();
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(template.getNamespace(),
                "structure/" + template.getPath() + ".nbt");
        try (InputStream in = server.getResourceManager().getResourceOrThrow(file).open()) {
            return NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
    }

    // ---- the data ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theDataOfThreeSevenLoads(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (String id : List.of("stone_locator", "chimichanga")) {
            check(server.getRecipeManager().byKey(Enhanced.id(id)).isPresent(), "the recipe " + id + " loads");
        }
        check(server.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(Enhanced.id("agatha_cottage")),
                "the structure agatha_cottage");
        check(server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .containsKey(Enhanced.id("agatha_cottages")), "and its set");
        for (String template : List.of("mutant_school/lower_lab", "agatha_cottage/house")) {
            check(server.getStructureManager().get(Enhanced.id(template)).isPresent(), "the template " + template);
        }
        for (String type : List.of("wolverine_claws", "wolverine_pounce", "deadpool_katana", "deadpool_bullet")) {
            check(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(Enhanced.id(type)),
                    "the damage " + type);
        }
        for (String sound : List.of("healing_down", "wolverine_snikt", "deadpool_taunt", "ambient.school_lab",
                "locator_pulse", "ambient.cottage")) {
            check(BuiltInRegistries.SOUND_EVENT.containsKey(Enhanced.id(sound)), "the sound " + sound);
        }
        for (String entity : List.of("deadpool_bullet", "dogpool", "cottage_resident")) {
            check(BuiltInRegistries.ENTITY_TYPE.containsKey(Enhanced.id(entity)), "the entity " + entity);
        }
        for (String block : List.of("secret_bookshelf", "elevator_pad", "x_door", "x_door_round", "adamantium_rack",
                "weapon_x_pod", "cure_station", "cerebro_console")) {
            check(BuiltInRegistries.BLOCK.containsKey(Enhanced.id(block))
                    && BuiltInRegistries.ITEM.containsKey(Enhanced.id(block)), "the block " + block + " and its item");
        }
        check(server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Enhanced.id("chests/school_lab/medical"))) != LootTable.EMPTY, "the lab's chests");
        check(new ItemStack(ModItems.STONE_LOCATOR.get()).is(ModItems.STONE_LOCATOR.get())
                && new ItemStack(ModItems.TIME_STONE.get()).is(ModTags.LOCATOR_CORES)
                && new ItemStack(ModItems.LOKIS_SCEPTER.get()).is(ModTags.LOCATOR_CORES), "the Locator's cores");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theConfigsHaveThePlansDefaults(GameTestHelper helper) {
        check(HealingConfig.ENABLED.getDefault() && HealingConfig.CUSTOM_HEROES.getDefault()
                && HealingConfig.DOWN_TICKS.getDefault() == 100 && HealingConfig.STONES_DEFEAT.getDefault()
                && HealingConfig.CHAOS_DEFEATS.getDefault() && HealingConfig.ODIN_DEFEATS.getDefault()
                && HealingConfig.PHOENIX_DEFEATS.getDefault() && !HealingConfig.DARKHOLD_DEFEATS.getDefault()
                && HealingConfig.VORMIR_DEFEATS.getDefault() && HealingConfig.POISON_STOPS_HEALING.getDefault(),
                "the healing factor's rules");
        TestKit.assertNear(HealingConfig.REGEN_PER_SECOND.getDefault(), 1.0, 1.0E-9, "1 health/s");
        TestKit.assertNear(HealingConfig.REGEN_OUT_OF_COMBAT.getDefault(), 3.0, 1.0E-9, "3 out of harm");
        TestKit.assertNear(HealingConfig.RISE_HEALTH.getDefault(), 8.0, 1.0E-9, "rises with 8");
        check(HealingFactor.config().equals(HealingFactor.Rules.DEFAULTS), "the config's rules are the defaults");
        check(WolverineConfig.HEAVY.getDefault() && WolverineConfig.POUNCE_RANGE.getDefault() == 16
                && WolverineConfig.BERSERK_TICKS.getDefault() == 200
                        && WolverineConfig.INJECTION_HOLD_TICKS.getDefault()
                == 60 && WolverineConfig.INJECTION_HEROES_REFUSED.getDefault()
                && !WolverineConfig.INJECTION_CRAFTABLE.getDefault(), "Wolverine's");
        check(DeadpoolConfig.MAGAZINE.getDefault() == 16 && DeadpoolConfig.POD_AWAKENS.getDefault()
                && DeadpoolConfig.POD_TICKS.getDefault() == 100 && DeadpoolConfig.DOGPOOL_SECONDS.getDefault() == 120,
                "Deadpool's");
        TestKit.assertNear(DeadpoolConfig.GLITCH_CHANCE.getDefault(), 0.10, 1.0E-9, "the belt glitches one in ten");
        check(SchoolLabConfig.ENABLED.getDefault() && SchoolLabConfig.AUTO_BUILD.getDefault()
                && SchoolLabConfig.BUILD_RADIUS.getDefault() == 96 && SchoolLabConfig.OPEN_TO_ALL.getDefault(),
                "the lab's");
        check(LocatorConfig.CRAFTABLE.getDefault() && LocatorConfig.SEARCH_RADIUS.getDefault() == 4096
                && LocatorConfig.START_CHECKS_PER_TICK.getDefault() == 2, "the Locator's");
        check(CottageConfig.ENABLED.getDefault() && CottageConfig.RESIDENTS.getDefault()
                && CottageConfig.AGATHA_YIELDS.getDefault(), "the cottage's");
        check(XavierConfig.CEREBRO_AMPLIFIED_TICKS.getDefault() == 600
                && XavierConfig.CEREBRO_AMPLIFIED_ALL_DIMENSIONS.getDefault()
                && XavierConfig.CEREBRO_TICKS.getDefault() == 200, "Cerebro: amplified 30 s, plain as before");
        helper.succeed();
    }

    private static JsonObject lang(String name) throws IOException {
        IModFileInfo mod = ModList.get().getModFileById(Enhanced.MODID);
        return JsonParser.parseString(Files.readString(mod.getFile().findResource("assets", Enhanced.MODID, "lang",
                name + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private MutantSkeletonTests() {
    }
}
