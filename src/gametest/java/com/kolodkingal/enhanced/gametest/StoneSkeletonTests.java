package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardGate;
import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.config.HydraConfig;
import com.kolodkingal.enhanced.config.SoulStoneConfig;
import com.kolodkingal.enhanced.config.StoneSitesConfig;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.Catalog;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.Resource;
import com.kolodkingal.enhanced.custom.StoneRow;
import com.kolodkingal.enhanced.custom.Trait;
import com.kolodkingal.enhanced.custom.Traits;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.item.ScepterRituals.Ritual;
import com.kolodkingal.enhanced.network.HulkFxPayload;
import com.kolodkingal.enhanced.network.HydraFxPayload;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.network.RealmScreenPayload;
import com.kolodkingal.enhanced.network.SlingRingActionPayload;
import com.kolodkingal.enhanced.network.StoneActionPayload;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.network.StrangeFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.stones.SpaceStone;
import com.kolodkingal.enhanced.stones.StoneProgress;
import com.kolodkingal.enhanced.stones.StoneRules;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.world.HydraOutpostPlan;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The skeleton of 3.6, the Infinity Stones, Doctor Strange, the Hulk and the Hydra soldiers (S0): the two heroes
 * appended after Hela, the bits and payloads (protocol 10), the 154 hero rows and 6 stone rows, the holding rule, the
 * two new rituals, the Gauntlet's default, the recipes, the gate's Space Stone exception, the fixed points of the Hydra
 * outpost and the configs' defaults. Batch {@code stones_skel}; a test that changes the config has a batch of its own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class StoneSkeletonTests {
    static final String BATCH = "stones_skel";
    static final String CFG = "stones_skel_cfg";

    // ---- the heroes, the bits, the protocol ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void strangeAndTheHulkComeAfterHela(GameTestHelper helper) {
        Power[] all = Power.values();
        check(all[Power.HELA.ordinal() + 1] == Power.DOCTOR_STRANGE && all[Power.HELA.ordinal() + 2] == Power.HULK
                && all.length >= Power.HELA.ordinal() + 3,
                        "…HELA, DOCTOR_STRANGE, HULK (the stream sends the ordinal; 3.7 appends after them)");
        check(Power.HELA.ordinal() == 20, "nobody before them moved");
        check(Power.DOCTOR_STRANGE.color == 0xF29A2E && Power.DOCTOR_STRANGE.glow == 0xFFE0A8
                && Power.DOCTOR_STRANGE.titleColor() == 0xC2302E, "Strange's colours");
        check(Power.HULK.color == 0x5BA33A && Power.HULK.glow == 0xB6F27A && Power.HULK.titleColor() == 0x6CC04A,
                "the Hulk's");
        check(Power.DOCTOR_STRANGE.canFly() && Power.DOCTOR_STRANGE.flightAbility() == 2, "the cloak flies on G");
        TestKit.assertNear(Power.DOCTOR_STRANGE.flightSpeed(), 0.06, 1.0E-6, "at 0.06");
        check(!Power.HULK.canFly() && Power.HULK.flightAbility() == -1 && Power.HULK.flightSpeed() == 0.05F,
                "the Hulk leaps");
        check(Traits.of(Power.DOCTOR_STRANGE).equals(Set.of(Trait.SORCERER)), "Strange is a sorcerer, not a Bearer");
        check(Traits.of(Power.HULK).isEmpty(), "the Hulk has no traits");
        for (Power bearer : List.of(Power.THOR, Power.LOKI, Power.SCARLET_WITCH, Power.PHOENIX, Power.HEIMDALL,
                Power.ODIN,
                Power.HELA)) {
            check(Traits.of(bearer).contains(Trait.STONE_BEARER), bearer + " is a Bearer");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBitsFitAndTheProtocolIsTen(GameTestHelper helper) {
        // (3.7: at least 3.6's numbers; MutantSkeletonTests pins 3.7's.)
        check(Integer.parseInt(ModNetwork.PROTOCOL_VERSION) >= 10, "protocol 10 or later");
        check(Live.values().length >= 53 && Live.values().length <= 63, "Live: 42 + 6 + 5 = 53 or more: "
                + Live.values().length);
        check(Toggle.values().length >= 19 && Toggle.values().length <= 32, "Toggle: 15 + 3 + 1 = 19 or more: "
                + Toggle.values().length);
        check(Live.STRANGE_SHIELD.ordinal() == 42 && Live.HULK_ENRAGED.ordinal() == 52,
                "the states of 3.6 are appended");
        check(Toggle.STRANGE_ASTRAL.ordinal() == 15 && Toggle.HUMAN_FORM.ordinal() == 18, "and the toggles");
        check(StoneProgress.FLAG_COUNT <= 31, "the stone flags fit");
        for (StoneKind kind : StoneKind.values()) {
            check(StoneProgress.bit(kind) == 1 << kind.ordinal(), kind + " has its own bit");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void thePayloadsOfThreeSixRoundTrip(GameTestHelper helper) {
        List<Vec3> points = List.of(Vec3.ZERO, new Vec3(1, 2, 3));
        roundTrip(StoneActionPayload.STREAM_CODEC, new StoneActionPayload(StoneActionPayload.REALM, 3),
                "a stone action");
        roundTrip(RealmScreenPayload.STREAM_CODEC,
                new RealmScreenPayload(List.of(new RealmScreenPayload.RealmOption("asgard",
                false, "message.enhanced.asgard.closed"), new RealmScreenPayload.RealmOption("the_nether", true, ""))),
                "the realm picker");
        roundTrip(StoneFxPayload.STREAM_CODEC, new StoneFxPayload(StoneFxPayload.SNAP, 7, points, 2),
                "a stone's effect");
        roundTrip(StrangeFxPayload.STREAM_CODEC, new StrangeFxPayload(StrangeFxPayload.MIRROR_ON, 7, points, 14),
                "a spell");
        roundTrip(HulkFxPayload.STREAM_CODEC, new HulkFxPayload(HulkFxPayload.GAMMA_BEAM, 7, points, 1), "a smash");
        roundTrip(HydraFxPayload.STREAM_CODEC, new HydraFxPayload(HydraFxPayload.TURNED, 7, points, 0), "a shot");
        roundTrip(SlingRingActionPayload.STREAM_CODEC, new SlingRingActionPayload(SlingRingActionPayload.SAVE, 2,
                "home"), "the ring");
        roundTrip(GauntletContents.STREAM_CODEC, new GauntletContents(0b101010, false, 3), "the Gauntlet's contents");
        roundTrip(StoneProgress.STREAM_CODEC, new StoneProgress(0b11, StoneProgress.SNAPPED, 1234L),
                "the stone progress");
        helper.succeed();
    }

    private static <T> void roundTrip(StreamCodec<ByteBuf, T> codec, T value, String what) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        check(codec.decode(buf).equals(value), what + " round-trips");
    }

    // ---- the Hero Creator ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRowsAre154AndTheStoneRowsSix(GameTestHelper helper) {
        check(HeroAbility.values().length >= 154, "133 + 12 + 9 rows or more: " + HeroAbility.values().length);
        check(HeroAbility.values()[HeroAbility.STRANGE_WHIP.ordinal() - 1] == HeroAbility.HELA_BODY,
                "Strange after Hela");
        check(HeroAbility.values()[HeroAbility.HULK_SMASH.ordinal() - 1] == HeroAbility.STRANGE_BODY,
                "the Hulk after Strange");
        for (Power hero : List.of(Power.DOCTOR_STRANGE, Power.HULK)) {
            List<HeroAbility> rows = HeroAbility.of(hero).stream().filter(a -> !a.isVariant()).toList();
            check(rows.size() == 6, hero + " has six rows");
            check(HeroAbility.at(hero, 0) != null, hero + " has a body");
        }
        check(HeroAbility.STRANGE_EYE.id().equals("strange.eye") && HeroAbility.at(Power.DOCTOR_STRANGE,
                6) == HeroAbility.STRANGE_EYE,
                "the Eye is Strange's Y row");
        check(HeroAbility.at(Power.DOCTOR_STRANGE, 2) == null, "and G is his flight");
        check(HeroAbility.THANOS_SOUL.resources(null).contains(Resource.SOUL_CHAIN),
                "thanos.soul takes the soul chain");
        Set<String> ids = new HashSet<>();
        for (HeroAbility a : HeroAbility.values()) {
            check(ids.add(a.id()), a.id() + " is unique");
            if (a.isVariant()) {
                check(a.parent() != null && a.parent().hasAlt(), a.id() + "'s parent has a Sneak variant");
            }
        }
        List<HeroAbility> variants = List.of(HeroAbility.STRANGE_WHIP_GRAPPLE, HeroAbility.STRANGE_SERAPHIM_DOME,
                HeroAbility.STRANGE_ASTRAL, HeroAbility.STRANGE_MIRROR, HeroAbility.STRANGE_EYE_REWIND,
                        HeroAbility.HULK_GRAB_SMASH,
                HeroAbility.HULK_ROAR, HeroAbility.HULK_BOUND, HeroAbility.HULK_GAMMA_CHARGE);
        for (HeroAbility v : variants) {
            check(v.isVariant(), v + " is a variant row");
        }
        check(!HeroAbility.HULK_TRANSFORM.hasAlt() || HeroAbility.HULK_TRANSFORM.variant() == null,
                "no variant row for Switch Hulk");
        check(StoneRow.values().length == 6, "six stone rows");
        for (StoneRow row : StoneRow.values()) {
            check(row.id().equals("stone." + row.stone().name().toLowerCase(java.util.Locale.ROOT)), row + "'s id");
            check(row.hero() == null && StoneRow.of(row.stone()) == row, row + " is a stone's, not a hero's");
            check(Catalog.byId(row.id()) == row && Catalog.all().contains(row), row + " is in the catalogue");
            check(row.alt() != null, row + " has a Sneak use");
        }
        check(Catalog.all().size() == com.kolodkingal.enhanced.custom.PowerModule.values().length
                + HeroAbility.values().length + StoneRow.values().length, "the catalogue is the three lists");
        check(StoneRow.SOUL.resources(null).contains(Resource.SOUL_CHAIN), "the Soul Stone's row takes the soul chain");
        helper.succeed();
    }

    // ---- the holding rule ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theHoldingRule(GameTestHelper helper) {
        Predicate<Trait> none = t -> false;
        Predicate<Trait> sorcerer = t -> t == Trait.SORCERER;
        Predicate<Trait> bearer = t -> t == Trait.STONE_BEARER;
        for (StoneKind kind : StoneKind.values()) {
            check(StoneRules.mayHold(Power.THOR, traits(Power.THOR), false, kind), "Thor holds the " + kind);
            check(StoneRules.mayHold(Power.NONE, none, true, kind), "Creative holds the " + kind);
            check(!StoneRules.mayHold(Power.NONE, none, false, kind), "a powerless player does not hold the " + kind);
            check(!StoneRules.mayHold(Power.HULK, none, false, kind), "nor does the Hulk hold the bare " + kind);
            check(StoneRules.mayHold(Power.CUSTOM, bearer, false, kind), "a custom Bearer holds the " + kind);
            check(StoneRules.mayHold(Power.DOCTOR_STRANGE, sorcerer, false, kind) == (kind == StoneKind.TIME),
                    "Strange holds only the Time Stone, not the " + kind);
            check(StoneRules.mayHold(Power.VISION, none, false, kind) == (kind == StoneKind.MIND),
                    "Vision holds only the Mind Stone, not the " + kind);
            check(!StoneRules.mayHold(Power.THANOS, none, false, kind), "the hero Thanos holds no bare stone");
        }
        check(!StoneRules.mayUse(Power.THOR, traits(Power.THOR), false, StoneKind.TIME),
                "Thor holds the Time Stone but cannot use it");
        check(StoneRules.mayUse(Power.LOKI, traits(Power.LOKI), false, StoneKind.TIME),
                "Loki, a sorcerer and a Bearer, uses it");
        check(StoneRules.mayUse(Power.DOCTOR_STRANGE, sorcerer, false, StoneKind.TIME), "and Strange");
        check(!StoneRules.mayUse(Power.NONE, none, true, StoneKind.TIME), "Creative alone does not make a sorcerer");
        check(StoneRules.mayUse(Power.THOR, traits(Power.THOR), false, StoneKind.POWER), "Thor uses the Power Stone");
        check(StoneRules.isBearer(Power.CUSTOM, bearer) && !StoneRules.isBearer(Power.CUSTOM, sorcerer),
                "a custom hero by trait");
        helper.succeed();
    }

    private static Predicate<Trait> traits(Power power) {
        return Traits.of(power)::contains;
    }

    @GameTest(template = ARENA, batch = CFG)
    public static void theCustomBearersOptionTurnsTheTraitOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(StonesConfig.CUSTOM_BEARERS, false);
        check(!StoneRules.isBearer(Power.CUSTOM, t -> t == Trait.STONE_BEARER), "no custom Bearer with the option off");
        check(StoneRules.isBearer(Power.THOR, t -> false), "the built-in Bearers stay");
        f.config(StonesConfig.SORCERERS_HOLD_TIME, false);
        check(!StoneRules.mayHold(Power.DOCTOR_STRANGE, t -> t == Trait.SORCERER, false, StoneKind.TIME),
                "nor may a sorcerer hold the Time Stone with that off");
        helper.succeed();
    }

    // ---- the rituals, the Gauntlet, the recipes ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theScepterAndTheTesseractAreTheStonesVessels(GameTestHelper helper) {
        ItemStack empty = ItemStack.EMPTY;
        ItemStack tesseract = new ItemStack(ModItems.TESSERACT.get());
        ItemStack scepter = new ItemStack(ModItems.LOKIS_SCEPTER.get());
        check(ScepterRituals.scepter(Power.NONE, empty, true, false) == Ritual.MIND_BEAM,
                "Sneak + use, other hand empty: the Mind Beam");
        check(ScepterRituals.scepter(Power.LOKI, empty, true, true) == Ritual.MIND_BEAM, "on a block too");
        check(ScepterRituals.scepter(Power.NONE, empty, false, false) == Ritual.EXPLODES, "a plain use still explodes");
        check(ScepterRituals.scepter(Power.SCARLET_WITCH, empty, false, false) == Ritual.RECOGNISES,
                "Wanda is still recognised");
        check(ScepterRituals.scepter(Power.NONE, tesseract, true, false) == Ritual.THANOS,
                "the Scepter with the Tesseract: Thanos");
        check(ScepterRituals.scepter(Power.NONE, new ItemStack(ModItems.ULTRON_PROGRAM.get()), true,
                true) == Ritual.ULTRON_FIGHT,
                "the Ultron program on a block, sneaking: the fight");
        check(ScepterRituals.scepter(Power.NONE, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()), true, false)
                == Ritual.WANDA_GENE, "the gene, sneaking, as before");
        check(ScepterRituals.tesseract(Power.NONE, empty, true) == Ritual.REALMS,
                "Sneak + Tesseract: the realm picker");
        check(ScepterRituals.tesseract(Power.NONE, empty) == Ritual.ESCAPE && ScepterRituals.tesseract(Power.NONE,
                empty, false)
                == Ritual.ESCAPE, "a plain use as before");
        check(ScepterRituals.tesseract(Power.THOR, empty, false) == Ritual.JUMP, "a hero jumps as before");
        check(ScepterRituals.tesseract(Power.NONE, scepter, true) == Ritual.THANOS,
                "with the Scepter: Thanos, sneaking or not");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theGauntletIsTheTitansAndTheRecipesLoad(GameTestHelper helper) {
        ItemStack gauntlet = new ItemStack(ModItems.INFINITY_GAUNTLET.get());
        check(GauntletContents.TITAN.equals(gauntlet.get(ModDataComponents.GAUNTLET.get())),
                "a Gauntlet with no component is Thanos's (old saves): "
                        + gauntlet.get(ModDataComponents.GAUNTLET.get()));
        check(GauntletContents.TITAN.titan() && Integer.bitCount(GauntletContents.TITAN.stones()) == 5,
                "five stones, not Soul");
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        ItemStack crafted = new ItemStack(ModItems.INFINITY_GAUNTLET.get());
        crafted.set(ModDataComponents.GAUNTLET.get(), GauntletContents.EMPTY);
        player.setItemInHand(InteractionHand.MAIN_HAND, crafted);
        check(crafted.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.PASS
                && !player.isUsingItem() && Powers.power(player) == Power.NONE,
                        "a crafted, empty Gauntlet awakens nobody");
        for (StoneKind kind : StoneKind.values()) {
            String id = kind.name().toLowerCase(java.util.Locale.ROOT) + "_stone";
            ItemStack stone = new ItemStack(BuiltInRegistries.ITEM.get(Enhanced.id(id)));
            check(!stone.isEmpty() && stone.getMaxStackSize() == 1, kind + " is an item that does not stack");
        }
        MinecraftServer server = helper.getLevel().getServer();
        for (String id : List.of("mind_stone", "space_stone", "lokis_scepter_from_stone", "tesseract_from_stone",
                "eye_of_agamotto",
                "time_stone_from_eye", "infinity_gauntlet", "sling_ring")) {
            check(server.getRecipeManager().byKey(Enhanced.id(id)).isPresent(), "the recipe " + id + " loads");
        }
        for (String structure : List.of("sanctum", "avengers_tower", "end_tunnel", "vormir_cliff")) {
            check(server.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(Enhanced.id(structure)),
                    "the structure " + structure);
        }
        for (String template : List.of("sanctum/house", "avengers_tower/base", "avengers_tower/mid",
                "avengers_tower/top",
                "end_tunnel/vault", "vormir/cliff")) {
            check(server.getStructureManager().get(Enhanced.id(template)).isPresent(), "the template " + template);
        }
        for (String type : List.of("eldritch_whip", "crimson_bands", "power_stone_beam", "power_stone_explosion",
                "stone_reject",
                "snap_dust", "hulk_smash", "hulk_grab", "hulk_clap", "hulk_leap", "hulk_boulder", "hulk_charge",
                        "hydra_bolt",
                "hydra_baton")) {
            check(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).containsKey(Enhanced.id(type)),
                    "the damage " + type);
        }
        for (String sound : List.of("stone_reject", "strange_whip", "ambient.sanctum", "hulk_smash", "hydra_alarm",
                "time_rewind")) {
            check(BuiltInRegistries.SOUND_EVENT.containsKey(Enhanced.id(sound)), "the sound " + sound);
        }
        helper.succeed();
    }

    // ---- the gate of Asgard ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSpaceStoneIsTheGatesOneException(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        check(!AsgardGate.allows(player, Level.OVERWORLD, AsgardDimension.KEY), "nobody walks into Asgard");
        check(!SpaceStone.isMoving(player), "the stone is not moving him");
        check(SpaceStone.moving(player, () -> AsgardGate.allows(player, Level.OVERWORLD, AsgardDimension.KEY)),
                "the stone takes him in");
        check(SpaceStone.moving(player, () -> AsgardGate.allows(player, AsgardDimension.KEY, Level.OVERWORLD)),
                "and out");
        check(!SpaceStone.isMoving(player), "and lets go after");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "stones_skel_gate")
    public static void theSpaceStoneStopsAtTheGateWhenTheOptionIsOff(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(StonesConfig.SPACE_REACHES_ASGARD, false);
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        check(!SpaceStone.moving(player, () -> AsgardGate.allows(player, Level.OVERWORLD, AsgardDimension.KEY)),
                "with reachesAsgard off the stone does not take him in");
        check(!SpaceStone.moving(player, () -> AsgardGate.allows(player, AsgardDimension.KEY, Level.OVERWORLD)),
                "nor out");
        helper.succeed();
    }

    // ---- the Hydra outpost ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theOutpostsFixedPointsAreInsideItsTemplateInEveryTurn(GameTestHelper helper) {
        Optional<StructureTemplate> found = helper.getLevel().getStructureManager().get(HydraOutpostPlan.TEMPLATE);
        check(found.isPresent(), "the outpost's template");
        StructureTemplate template = found.get();
        check(template.getSize().equals(HydraOutpostPlan.SIZE), "its size is the plan's: " + template.getSize());
        List<BlockPos> points = new ArrayList<>(List.of(HydraOutpostPlan.CENTRE, HydraOutpostPlan.GATE,
                HydraOutpostPlan.DOOR,
                HydraOutpostPlan.TOWER_POST, HydraOutpostPlan.TOWER_FLOOR, HydraOutpostPlan.COMMAND_POST,
                        HydraOutpostPlan.RADIO_POST,
                HydraOutpostPlan.ALARM_CONSOLE, HydraOutpostPlan.CELLAR_POST, HydraOutpostPlan.CELLAR_SPAWN));
        points.addAll(HydraOutpostPlan.GATE_POSTS);
        points.addAll(HydraOutpostPlan.ALARM_LAMPS);
        points.addAll(HydraOutpostPlan.YARD_ROUTE);
        points.addAll(HydraOutpostPlan.COVER);
        BlockPos center = new BlockPos(1000, 70, -2000);
        BlockPos origin = HydraOutpostPlan.templatePosition(center);
        for (Rotation rotation : Rotation.values()) {
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation)
                    .setRotationPivot(HydraOutpostPlan.PIVOT);
            BoundingBox box = template.getBoundingBox(settings, origin);
            for (BlockPos p : points) {
                check(box.isInside(HydraOutpostPlan.toWorld(p, rotation, origin)), p + " turned " + rotation
                        + " is inside the outpost");
            }
            check(HydraOutpostPlan.toWorld(HydraOutpostPlan.PIVOT, rotation, origin)
                    .equals(origin.offset(HydraOutpostPlan.PIVOT)),
                    "the pivot stays put " + rotation);
        }
        check(HydraOutpostPlan.toWorld(BlockPos.ZERO, Rotation.NONE, origin).equals(origin),
                "unturned, the corner is the origin");
        helper.succeed();
    }

    /**
     * Every fixed point holds its block in {@code hydra_outpost/base.nbt} (the iron door, the observer console, the
     * four lamps, the tower's planks, the cover; air or nothing at the posts), and the template turned four ways puts
     * those blocks where {@link HydraOutpostPlan#toWorld} says.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theOutpostsFixedPointsHoldTheirBlocksInEveryTurn(GameTestHelper helper) throws IOException {
        MinecraftServer server = helper.getLevel().getServer();
        ResourceLocation file = Enhanced.id("structure/hydra_outpost/base.nbt");
        CompoundTag root;
        try (InputStream in = server.getResourceManager().getResourceOrThrow(file).open()) {
            root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        }
        ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
        Map<BlockPos, String> blocks = new HashMap<>();
        for (Tag t : root.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) t;
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            blocks.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)),
                    palette.getCompound(b.getInt("state")).getString("Name"));
        }
        Map<BlockPos, Block> solid = new HashMap<>();
        solid.put(HydraOutpostPlan.DOOR, Blocks.IRON_DOOR);
        solid.put(HydraOutpostPlan.ALARM_CONSOLE, Blocks.OBSERVER);
        solid.put(HydraOutpostPlan.TOWER_FLOOR, Blocks.DARK_OAK_PLANKS);
        for (BlockPos lamp : HydraOutpostPlan.ALARM_LAMPS) {
            solid.put(lamp, Blocks.REDSTONE_LAMP);
        }
        for (BlockPos cover : HydraOutpostPlan.COVER) {
            check(blocks.containsKey(cover) && !blocks.get(cover).equals("minecraft:air"), "cover at " + cover
                    + " is a block");
        }
        for (Map.Entry<BlockPos, Block> e : solid.entrySet()) {
            String id = BuiltInRegistries.BLOCK.getKey(e.getValue()).toString();
            check(id.equals(blocks.get(e.getKey())), e.getKey() + " holds " + id + ", not " + blocks.get(e.getKey()));
        }
        List<BlockPos> posts = new ArrayList<>(List.of(HydraOutpostPlan.CENTRE, HydraOutpostPlan.GATE,
                HydraOutpostPlan.TOWER_POST, HydraOutpostPlan.COMMAND_POST, HydraOutpostPlan.RADIO_POST,
                HydraOutpostPlan.CELLAR_POST, HydraOutpostPlan.CELLAR_SPAWN));
        posts.addAll(HydraOutpostPlan.GATE_POSTS);
        posts.addAll(HydraOutpostPlan.YARD_ROUTE);
        for (BlockPos post : posts) {
            String id = blocks.getOrDefault(post, "minecraft:air");
            check(id.equals("minecraft:air"), "a soldier fits at the post " + post + ": " + id);
        }
        StructureTemplate template = server.getStructureManager().get(HydraOutpostPlan.TEMPLATE).orElseThrow();
        BlockPos origin = HydraOutpostPlan.templatePosition(new BlockPos(1000, 70, -2000));
        for (Rotation rotation : Rotation.values()) {
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation)
                    .setRotationPivot(HydraOutpostPlan.PIVOT);
            for (Map.Entry<BlockPos, Block> e : solid.entrySet()) {
                BlockPos at = HydraOutpostPlan.toWorld(e.getKey(), rotation, origin);
                check(template.filterBlocks(origin, settings, e.getValue()).stream()
                        .anyMatch(info -> info.pos().equals(at)),
                        e.getValue() + " of " + e.getKey() + " turned " + rotation + " is at " + at);
            }
        }
        helper.succeed();
    }

    // ---- the configs ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theConfigsHaveThePlansDefaults(GameTestHelper helper) {
        check(StonesConfig.UNIQUENESS.getDefault() == StonesConfig.Uniqueness.PLAYER, "one of each stone per player");
        check(StonesConfig.GRACE_SECONDS.getDefault() == 10 && StonesConfig.CUSTOM_BEARERS.getDefault()
                && StonesConfig.VISION_HOLDS_MIND.getDefault() && StonesConfig.SORCERERS_HOLD_TIME.getDefault()
                && StonesConfig.SPACE_REACHES_ASGARD.getDefault(), "the stones' rules");
        check(SoulStoneConfig.ENABLED.getDefault(), "the Soul Stone's sacrifice is on");
        TestKit.assertNear(StrangeConfig.FLIGHT_SPEED.getDefault(), 0.06, 1.0E-9, "Strange's flight");
        check(StoneSitesConfig.SANCTUMS.getDefault() && StoneSitesConfig.AVENGERS_TOWERS.getDefault()
                && StoneSitesConfig.END_TUNNELS.getDefault() && StoneSitesConfig.VORMIR_CLIFFS.getDefault(),
                        "the four sites generate");
        check(HulkConfig.BLOCK_BREAKING.getDefault() == HulkConfig.BlockBreaking.SOFT, "the Hulk breaks soft blocks");
        check(HydraConfig.RESPAWN.getDefault() == HydraConfig.Respawn.DAYS && HydraConfig.ENABLED.getDefault(),
                "the garrison");
        check(StonesConfig.UNIQUENESS.get() == StonesConfig.Uniqueness.PLAYER, "and the loaded config is the default");
        helper.succeed();
    }

    private StoneSkeletonTests() {
    }
}
