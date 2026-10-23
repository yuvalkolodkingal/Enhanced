package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.HeroServers;
import com.kolodkingal.enhanced.ability.Metal;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.beam.AvengersBeams;
import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.captain.CaptainState;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.config.EternityConfig;
import com.kolodkingal.enhanced.config.FalconConfig;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.config.SerumConfig;
import com.kolodkingal.enhanced.config.TowerConfig;
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
import com.kolodkingal.enhanced.eternity.EternityProgress;
import com.kolodkingal.enhanced.eternity.EternityRealm;
import com.kolodkingal.enhanced.falcon.FalconState;
import com.kolodkingal.enhanced.falcon.FalconWings;
import com.kolodkingal.enhanced.hulk.GrabSmash;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.hulk.SuperLeap;
import com.kolodkingal.enhanced.item.SwitchedRecipe;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.MissileGuidance;
import com.kolodkingal.enhanced.ironman.SuitItemState;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.kolodkingal.enhanced.love.LoveAbilities;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.network.ChangeLookPayload;
import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.network.EternityScreenPayload;
import com.kolodkingal.enhanced.network.EternityWishPayload;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.network.SerumChoicePayload;
import com.kolodkingal.enhanced.network.SerumScreenPayload;
import com.kolodkingal.enhanced.network.TowerFxPayload;
import com.kolodkingal.enhanced.odin.GungnirStand;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variant;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.redhulk.RedHulkState;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.world.ShrinePlan;
import com.kolodkingal.enhanced.world.StoneSitePlans;
import com.kolodkingal.enhanced.world.TowerPlan;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The skeleton of 3.8, the serum, Captain America, Red Hulk, Iron Man and his suits, Falcon, the tower's armoury,
 * Eternity and Love (S0): the five heroes appended after Deadpool, the bits (the momentary states in two words) and
 * payloads (protocol 12), the 221 hero rows, the data and the configs' defaults, the shrine's and the tower's fixed
 * points, and the seams, neutral until their lanes. Batch {@code avengers_skel}; a test that changes the config or
 * server-wide state has the batch {@code avengers_skel_cfg}.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class AvengersSkeletonTests {
    static final String BATCH = "avengers_skel";
    static final String CFG = "avengers_skel_cfg";
    static final List<Power> HEROES = List.of(Power.CAPTAIN_AMERICA, Power.RED_HULK, Power.IRON_MAN, Power.FALCON,
            Power.LOVE);

    // ---- the heroes, the bits, the protocol ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theFiveHeroesComeAfterDeadpool(GameTestHelper helper) {
        Power[] all = Power.values();
        int d = Power.DEADPOOL.ordinal();
        check(d == 24 && all.length == d + 6, "…DEADPOOL and five more (the stream sends the ordinal)");
        for (int i = 0; i < HEROES.size(); i++) {
            check(all[d + 1 + i] == HEROES.get(i), HEROES.get(i) + " in its place");
        }
        Map<Power, int[]> colours = Map.of(Power.CAPTAIN_AMERICA, new int[]{0x2A4BA0, 0xE8EEF5, 0x3D6FD6},
                Power.RED_HULK, new int[]{0xC4231B, 0xFF7A3A, 0xE0352A},
                Power.IRON_MAN, new int[]{0xB3121B, 0x9FE8FF, 0xD62A2A},
                Power.FALCON, new int[]{0x8A8F98, 0xE0313A, 0xD8323C},
                Power.LOVE, new int[]{0xB57BFF, 0xFFE59A, 0xC08CFF});
        for (Power hero : HEROES) {
            int[] c = colours.get(hero);
            check(hero.color == c[0] && hero.glow == c[1] && hero.titleColor() == c[2], hero + "'s colours");
            check(HeroServers.of(hero) != null && HeroServers.of(hero).power() == hero, hero + " has a server plug-in");
            check(HeroClients.of(hero) != null, hero + " has a client plug-in");
            List<Variant> looks = Variants.of(hero);
            check(looks.get(looks.size() - 1).id().equals("own"), hero + "'s last look is the player's own");
            String acting = GestureRoutes.acting(hero, 1);
            check(acting.startsWith("none: ") && !acting.equals(GestureRoutes.PENDING_ACTING),
                    hero + "'s keys are explained, not the 3.0 pending marker");
        }
        for (Power walker : List.of(Power.CAPTAIN_AMERICA, Power.RED_HULK)) {
            check(!walker.canFly() && walker.flightAbility() == -1 && walker.flightSpeed() == 0.05F,
                    walker + " does not fly: all five keys are rows");
        }
        for (Power flier : List.of(Power.IRON_MAN, Power.FALCON, Power.LOVE)) {
            check(flier.canFly() && flier.flightAbility() == 2 && GestureRoutes.acting(flier, 2).equals("flight"),
                    flier + " flies on G (gated)");
        }
        TestKit.assertNear(Power.FALCON.flightSpeed(), 0.10, 1.0E-6, "Falcon's 0.10");
        TestKit.assertNear(Power.LOVE.flightSpeed(), 0.10, 1.0E-6, "Love's 0.10");
        check(Variants.of(Power.CAPTAIN_AMERICA).stream().map(Variant::id).toList()
                .equals(List.of("classic", "stealth", "own")), "Cap's three looks");
        check(Variants.of(Power.RED_HULK).stream().map(Variant::id).toList().equals(List.of("red_hulk", "own")),
                "Red Hulk's two");
        check(Variants.of(Power.IRON_MAN).stream().map(Variant::id).toList()
                .equals(List.of("tony", "shades", "workshop", "own")), "Tony's three looks under the suit (3.8.1)");
        check(Variants.of(Power.FALCON).stream().map(Variant::id).toList()
                .equals(List.of("endgame", "winter_soldier", "own")), "Falcon's three");
        check(Variants.of(Power.LOVE).stream().map(Variant::id).toList().equals(List.of("love", "own")), "Love's two");
        check(Traits.of(Power.CAPTAIN_AMERICA).equals(Set.of(Trait.WORTHY))
                && Traits.of(Power.LOVE).equals(Set.of(Trait.WORTHY)), "Cap and Love are worthy");
        for (Power p : List.of(Power.RED_HULK, Power.IRON_MAN, Power.FALCON)) {
            check(Traits.of(p).isEmpty(), p + " has no role");
        }
        check(HeroItems.of(Power.CAPTAIN_AMERICA, "classic").equals(List.of(ModItems.VIBRANIUM_SHIELD.get()))
                && HeroItems.of(Power.RED_HULK, "red_hulk").isEmpty()
                && HeroItems.of(Power.IRON_MAN, "tony").equals(List.of(ModItems.ARC_REACTOR.get(),
                ModItems.IRON_MAN_MARK_3.get()))
                && HeroItems.of(Power.FALCON, "endgame").equals(List.of(ModItems.FALCON_EXO_WINGS.get()))
                && HeroItems.of(Power.LOVE, "love").equals(List.of(ModItems.STORMBREAKER.get())),
                "the items Choose a hero gives");
        check(Power.CAPTAIN_AMERICA.awakeningTitle().getString().length() <= 14, "Cap wakes as The Captain");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBitsFitAndTheProtocolIsTwelve(GameTestHelper helper) {
        check(ModNetwork.PROTOCOL_VERSION.equals("13"), "protocol 13");
        check(Live.values().length == 75 && Live.values().length <= 127, "Live: 58 + 2 + 6 + 6 + 3 = 75: "
                + Live.values().length);
        check(Toggle.values().length == 24 && Toggle.values().length <= 32, "Toggle: 22 + 2 = 24: "
                + Toggle.values().length);
        check(Live.CAP_CHARGING.ordinal() == 58 && Live.RED_HULK_TRANSFORMING.ordinal() == 60
                && Live.IRON_SUITING.ordinal() == 66 && Live.FALCON_GLIDING.ordinal() == 74,
                "the states of 3.8 are appended");
        check(Toggle.ROSS_FORM.ordinal() == 22 && Toggle.REDWING_OUT.ordinal() == 23, "and the toggles");
        check(Live.DEADPOOL_AIMING.word() == 0 && Live.RED_HULK_HEATED.word() == 0
                && Live.RED_HULK_ABSORBING.word() == 1 && Live.FALCON_GLIDING.word() == 1, "64 to a word");
        check(Live.RED_HULK_ABSORBING.mask() == 1L && Live.RED_HULK_HEATED.mask() == 1L << 63, "the masks");
        for (Live live : Live.values()) {
            check(GestureRoutes.reason(live) == null || !GestureRoutes.reason(live)
                    .startsWith(GestureRoutes.PENDING_REASON), live + " is not the release gate's pending marker");
        }
        check(GestureRoutes.toggle(Power.RED_HULK, Toggle.ROSS_FORM, true) != null
                && GestureRoutes.toggle(Power.FALCON, Toggle.REDWING_OUT, true) != null
                && GestureRoutes.toggle(Power.HULK, Toggle.ROSS_FORM, true) == null,
                        "the toggles routed for their own");
        helper.succeed();
    }

    /** The second word: a state past the 64th sets, clears and survives the sync, and leaves the first word alone. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void aStatePastTheSixtyFourthRoundTrips(GameTestHelper helper) {
        PowerData data = new PowerData();
        check(Live.IRON_SUITING.ordinal() >= 64 && Live.IRON_SUITING.word() == 1, "IRON_SUITING is in the high word");
        data.set(Live.IRON_SUITING, true);
        check(data.is(Live.IRON_SUITING) && data.liveBits() == 0L && data.liveBitsHigh() != 0L,
                "set in the high word, the low word untouched");
        check(!data.is(Live.SPEEDING), "the low word's first state is not set by it");
        data.set(Live.SPEEDING, true);
        data.set(Live.RED_HULK_HEATED, true);
        data.set(Live.FALCON_GLIDING, true);
        PowerData back = roundTrip(helper, data);
        for (Live live : Live.values()) {
            check(back.is(live) == data.is(live), live + " survives the sync");
        }
        check(back.is(Live.IRON_SUITING) && back.is(Live.SPEEDING) && back.is(Live.RED_HULK_HEATED)
                && back.is(Live.FALCON_GLIDING), "both words travel");
        data.set(Live.IRON_SUITING, false);
        check(!data.is(Live.IRON_SUITING) && data.is(Live.FALCON_GLIDING) && data.is(Live.RED_HULK_HEATED),
                "cleared alone");
        PowerData again = roundTrip(helper, data);
        check(!again.is(Live.IRON_SUITING) && again.is(Live.FALCON_GLIDING), "and the clear travels too");
        for (Live live : Live.values()) {
            data.set(live, true);
        }
        PowerData full = roundTrip(helper, data);
        for (Live live : Live.values()) {
            check(full.is(live), live + " survives with every state on");
        }
        helper.succeed();
    }

    private static PowerData roundTrip(GameTestHelper helper, PowerData data) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        PowerData.STREAM_CODEC.encode(buf, data);
        return PowerData.STREAM_CODEC.decode(buf);
    }

    // ---- the payloads ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void thePayloadsOfThreeEightRoundTripAtTheirLargest(GameTestHelper helper) {
        List<Vec3> ten = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ten.add(new Vec3(-29_999_999.5 + i, 319.5, 29_999_999.5));
        }
        int big = 1 << 30;
        roundTrip(CaptainFxPayload.STREAM_CODEC, new CaptainFxPayload(CaptainFxPayload.FLIP, big, ten, big),
                "Cap's moment", 256);
        roundTrip(RedHulkFxPayload.STREAM_CODEC, new RedHulkFxPayload(RedHulkFxPayload.TRANSFORM_OUT, big, ten, big),
                "Red Hulk's", 256);
        roundTrip(IronFxPayload.STREAM_CODEC, new IronFxPayload(IronFxPayload.OFFLINE, big, ten, big), "Iron Man's",
                256);
        roundTrip(FalconFxPayload.STREAM_CODEC, new FalconFxPayload(FalconFxPayload.MARK, big, ten, big), "Falcon's",
                256);
        roundTrip(TowerFxPayload.STREAM_CODEC, new TowerFxPayload(TowerFxPayload.GANTRY,
                new BlockPos(29_999_999, 319, -29_999_999), big), "the tower's", 256);
        roundTrip(EternityFxPayload.STREAM_CODEC, new EternityFxPayload(EternityFxPayload.EYES_OFF, big,
                new BlockPos(-29_999_999, -64, 29_999_999), big), "Eternity's", 256);
        check(CaptainFxPayload.FLIP == 13 && RedHulkFxPayload.TRANSFORM_OUT == 14 && IronFxPayload.OFFLINE == 32
                && FalconFxPayload.MARK == 13 && TowerFxPayload.GANTRY == 4 && EternityFxPayload.EYES_OFF == 8,
                "the kinds' numbers");
        roundTrip(SerumScreenPayload.STREAM_CODEC, new SerumScreenPayload(InteractionHand.OFF_HAND, true),
                "the serum's screen", 4096);
        roundTrip(SerumChoicePayload.STREAM_CODEC, new SerumChoicePayload(Power.FALCON), "the serum's choice", 4096);
        List<EternityScreenPayload.Lock> locks = new ArrayList<>();
        for (int i = 0; i < EternityScreenPayload.MAX_LOCKS; i++) {
            locks.add(new EternityScreenPayload.Lock(Enhanced.id("lock_" + i),
                    "screen.enhanced.wish.lock.gungnir"));
        }
        roundTrip(EternityScreenPayload.STREAM_CODEC, new EternityScreenPayload(true, true, locks),
                "the wish screen with 64 locks", 4096);
        roundTrip(EternityWishPayload.STREAM_CODEC, new EternityWishPayload(EternityWishPayload.ITEM,
                ResourceLocation.withDefaultNamespace("diamond")), "a wish", 4096);
        // the states
        roundTrip(CaptainState.STREAM_CODEC, new CaptainState(77, 2, 12345L, 3), "Cap's state", 4096);
        roundTrip(RedHulkState.STREAM_CODEC, new RedHulkState(99.5F, 400L, 900L, RedHulkState.FALL_SAFE),
                "Red Hulk's", 4096);
        IronManState iron = IronManState.EMPTY.withSuit(SuitKind.MARK_85).withEnergy(42.5F)
                .withPhase(IronManState.UP, SuitKind.MARK_85, 1000L, 24).with(IronManState.NANO_OUT, true)
                .withDronesUntil(2000L);
        roundTrip(IronManState.STREAM_CODEC, iron, "Iron Man's", 4096);
        check(iron.worn() == SuitKind.MARK_85 && iron.phaseKind() == SuitKind.MARK_85 && iron.lastSuit()
                == SuitKind.MARK_85.asByte(), "the suit and the phase");
        roundTrip(FalconState.STREAM_CODEC, new FalconState(Optional.of(new UUID(3, 4)), 1200L, 600L, 1), "Falcon's",
                4096);
        roundTrip(EternityProgress.STREAM_CODEC, new EternityProgress(1, EternityProgress.LOVE, Optional.of(
                new EternityProgress.ReturnPoint(Level.END, new Vec3(1000.5, 60, -2000.5), 90.0F, 10.0F)), 2),
                "the journey", 4096);
        roundTrip(SuitItemState.STREAM_CODEC, SuitItemState.FOLDED, "the suitcase", 4096);
        // the saved state of Iron Man keeps no phase
        IronManState loaded = IronManState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,
                IronManState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, iron).getOrThrow()).getOrThrow();
        check(loaded.phase() == IronManState.NONE && loaded.worn() == SuitKind.MARK_85 && loaded.energy() == 42.5F,
                "saved without the phase");
        helper.succeed();
    }

    private static <T> void roundTrip(StreamCodec<ByteBuf, T> codec, T value, String what, int limit) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        int size = buf.readableBytes();
        check(size <= limit, what + " stays within " + limit + " bytes: " + size);
        check(codec.decode(buf).equals(value), what + " round-trips");
    }

    // ---- the Hero Creator ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRowsAre221(GameTestHelper helper) throws IOException {
        check(HeroAbility.values().length == 221, "176 + 27 + 18 rows: " + HeroAbility.values().length);
        check(HeroAbility.values()[HeroAbility.CAPTAIN_SHIELD_THROW.ordinal() - 1] == HeroAbility.DEADPOOL_BODY,
                "Captain America after Deadpool");
        check(HeroAbility.values()[HeroAbility.CAPTAIN_RICOCHET.ordinal() - 1] == HeroAbility.DEADPOOL_SMOKE,
                "3.8's variants after 3.7's");
        check(HeroAbility.values()[HeroAbility.values().length - 1] == HeroAbility.LOVE_BIFROST_LOOK,
                "and they come last");
        Map<Power, int[]> counts = Map.of(Power.CAPTAIN_AMERICA, new int[]{6, 5}, Power.RED_HULK, new int[]{6, 4},
                Power.IRON_MAN, new int[]{5, 3}, Power.FALCON, new int[]{5, 4}, Power.LOVE, new int[]{5, 2});
        for (Power hero : HEROES) {
            List<HeroAbility> rows = HeroAbility.of(hero).stream().filter(a -> !a.isVariant()).toList();
            List<HeroAbility> variants = HeroAbility.of(hero).stream().filter(HeroAbility::isVariant).toList();
            check(rows.size() == counts.get(hero)[0] && variants.size() == counts.get(hero)[1],
                    hero + " has " + rows.size() + " rows and " + variants.size() + " variants");
            check(HeroAbility.at(hero, 0) != null && HeroAbility.at(hero, 0).passive(), hero + " has a body");
            for (int key = 1; key <= 5; key++) {
                check((HeroAbility.at(hero, key) != null) != (key == hero.flightAbility()),
                        hero + " key " + key + " is a row or the flight key");
            }
        }
        check(HeroAbility.CAPTAIN_BODY.traits().equals(Set.of(Trait.WORTHY))
                && HeroAbility.LOVE_BODY.traits().equals(Set.of(Trait.WORTHY)), "the worthy bodies");
        check(HeroAbility.CAPTAIN_COMBO.heroId().equals("captain") && HeroAbility.CAPTAIN_COMBO.id()
                .equals("captain.combo") && HeroAbility.RED_HULK_BURST.id().equals("red_hulk.burst")
                && HeroAbility.IRON_MAN_SUIT.id().equals("iron_man.suit"), "the ids");
        check(HeroAbility.RED_HULK_TRANSFORM.alt() == null && !HeroAbility.RED_HULK_TRANSFORM.scales()
                && HeroAbility.RED_HULK_TRANSFORM.resources(HeroAbility.BOTH).contains(Resource.FORM),
                "the transform is fixed, his form");
        check(HeroAbility.RED_HULK_ABSORB.resources(null).contains(Resource.SHIELD)
                && HeroAbility.IRON_MAN_SUIT.resources(null).contains(Resource.SUIT_ENERGY)
                && HeroAbility.LOVE_BIFROST.world, "the resources and the world power");
        Set<String> ids = new HashSet<>();
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        for (HeroAbility a : HeroAbility.values()) {
            check(ids.add(a.id()), a.id() + " is unique");
            if (a.isVariant()) {
                check(a.parent() != null && a.parent().hasAlt() && a.parent().variant() == a,
                        a.id() + "'s parent has a Sneak variant, and it is this row");
            }
            if (HEROES.contains(a.hero)) {
                for (JsonObject lang : List.of(en, he)) {
                    check(lang.has(a.nameKey()) && lang.has(a.descKey()) && (!a.hasAlt() || a.isVariant()
                            || lang.has(a.altKey())), a.id() + " has its names");
                }
            }
        }
        check(Catalog.all().size() == PowerModule.values().length + HeroAbility.values().length
                + StoneRow.values().length, "the catalogue is the three lists");
        Map<ResourceKey<DamageType>, HeroAbility> dealt = Map.of(ModDamageTypes.CAP_SHIELD,
                HeroAbility.CAPTAIN_SHIELD_THROW, ModDamageTypes.RED_HULK_SMASH, HeroAbility.RED_HULK_SMASH,
                ModDamageTypes.REPULSOR, HeroAbility.IRON_MAN_REPULSOR, ModDamageTypes.FALCON_DART,
                HeroAbility.FALCON_BLASTERS, ModDamageTypes.LOVE_EYE_LASER, HeroAbility.LOVE_EYE_LASER,
                ModDamageTypes.CAP_COMBO, HeroAbility.CAPTAIN_COMBO); // (3.8.1: Cap's fists, scaled like his shield)
        for (Map.Entry<ResourceKey<DamageType>, HeroAbility> e : dealt.entrySet()) {
            CustomHero hero = CustomKitSupport.def().pick(e.getValue().id(), 5).key(e.getValue().home,
                    e.getValue().id()).build();
            check(HeroDamage.abilityOf(e.getKey(), CustomKitSupport.data(hero).kit()) == e.getValue(),
                    e.getKey().location() + " is dealt by " + e.getValue().id());
        }
        helper.succeed();
    }

    // ---- the data ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theDataOfThreeEightLoads(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        check(server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).containsKey(Enhanced.id("eternity")),
                "the realm's dimension type");
        ServerLevel realm = EternityTestSupport.ensureRealm(server);
        check(realm != null && realm.dimension() == EternityRealm.LEVEL, "the realm is a level made of its data");
        check(realm.dimensionType().fixedTime().orElse(-1L) == 6000L && realm.dimensionType().ambientLight() == 1.0F
                && !realm.dimensionType().bedWorks() && realm.getMinBuildHeight() == 0, "its type's rules");
        for (BlockPos at : List.of(new BlockPos(0, 63, 0), new BlockPos(37, 63, -150))) {
            check(realm.getBlockState(at).is(com.kolodkingal.enhanced.registry.ModBlocks.ETERNITY_MIRROR.get())
                    && realm.getBlockState(at.above()).isAir() && realm.getBlockState(at.below()).isAir(),
                    "the still mirror at y 63, nothing over or under it: " + at);
        }
        check(server.registryAccess().registryOrThrow(Registries.BIOME).containsKey(Enhanced.id("eternity")),
                "the realm's biome");
        check(server.registryAccess().registryOrThrow(Registries.STRUCTURE).containsKey(Enhanced.id("eternity_shrine"))
                && server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .containsKey(Enhanced.id("eternity_shrines")), "the shrine and its set");
        check(server.getStructureManager().get(ShrinePlan.TEMPLATE).isPresent(), "the shrine's template");
        Optional<HolderSet.Named<Item>> keys = BuiltInRegistries.ITEM.getTag(ModTags.BIFROST_KEYS);
        check(keys.isPresent() && keys.get().size() == 4, "four keys of the Bifrost");
        for (Item key : List.of(ModItems.MJOLNIR.get(), ModItems.STORMBREAKER.get(), ModItems.HOFUND.get(),
                ModItems.GUNGNIR.get())) {
            check(new ItemStack(key).is(ModTags.BIFROST_KEYS), key + " opens the altar");
        }
        Optional<HolderSet.Named<Item>> forbidden = BuiltInRegistries.ITEM.getTag(ModTags.WISH_FORBIDDEN);
        check(forbidden.isPresent() && forbidden.get().size() == 18, "eighteen forbidden wishes, all real items");
        check(new ItemStack(Items.COMMAND_BLOCK).is(ModTags.WISH_FORBIDDEN)
                && !new ItemStack(Items.DIAMOND).is(ModTags.WISH_FORBIDDEN), "a command block, not a diamond");
        check(BuiltInRegistries.ITEM.getTag(ModTags.IRON_SUITS).map(HolderSet::size).orElse(0) == 6
                && BuiltInRegistries.ITEM.getTag(ModTags.NANO_SUITS).map(HolderSet::size).orElse(0) == 2
                && new ItemStack(ModItems.NANO_ARC_REACTOR.get()).is(ModTags.ARC_REACTORS), "the suits and reactors");
        Registry<DamageType> types = server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        for (String name : List.of("cap_shield", "cap_slam", "cap_combo", "red_hulk_smash", "red_hulk_burst",
                "repulsor", "unibeam", "iron_missile", "iron_flame", "iron_blade", "refocuser_lightning",
                "drone_laser", "hulkbuster_punch", "falcon_dart", "falcon_dive", "redwing_laser", "love_eye_laser",
                "love_lightning")) {
            check(types.containsKey(Enhanced.id(name)), "the damage " + name);
        }
        check(types.getHolderOrThrow(ModDamageTypes.RED_HULK_BURST).is(DamageTypeTags.IS_FIRE)
                && types.getHolderOrThrow(ModDamageTypes.CAP_SHIELD).is(DamageTypeTags.IS_PROJECTILE)
                && types.getHolderOrThrow(ModDamageTypes.IRON_MISSILE).is(DamageTypeTags.IS_EXPLOSION)
                && types.getHolderOrThrow(ModDamageTypes.LOVE_LIGHTNING).is(DamageTypeTags.IS_LIGHTNING)
                && types.getHolderOrThrow(ModDamageTypes.HULKBUSTER_PUNCH).is(ModTags.MELEE_BLOWS)
                && types.getHolderOrThrow(ModDamageTypes.CAP_COMBO).is(ModTags.MELEE_BLOWS),
                "the damages' tags");
        for (ResourceKey<DamageType> energy : List.of(ModDamageTypes.REPULSOR, ModDamageTypes.LOVE_EYE_LASER,
                ModDamageTypes.CHAOS_BEAM, ModDamageTypes.LIGHTNING_BEAM, ModDamageTypes.OPTIC_BLAST)) {
            check(types.getHolderOrThrow(energy).is(ModTags.ENERGY_DAMAGE), energy.location() + " is energy");
        }
        check(!types.getHolderOrThrow(ModDamageTypes.CAP_SLAM).is(ModTags.ENERGY_DAMAGE), "a slam is not");
        for (ResourceKey<DamageType> ours : List.of(ModDamageTypes.LOVE_LIGHTNING, ModDamageTypes.LOVE_EYE_LASER,
                ModDamageTypes.REFOCUSER_LIGHTNING, ModDamageTypes.RED_HULK_BURST, ModDamageTypes.CAP_SHIELD)) {
            Holder<DamageType> type = types.getHolderOrThrow(ours);
            check(!type.is(ModTags.DEFEATS_HEALING_STONES) && !type.is(ModTags.DEFEATS_HEALING_ODIN)
                    && !type.is(ModTags.DEFEATS_HEALING_CHAOS) && !type.is(ModTags.DEFEATS_HEALING_PHOENIX),
                    ours.location() + " defeats no healer (open question 9)");
        }
        for (String id : List.of("special_soldier_serum", "arc_reactor", "nano_arc_reactor")) {
            check(server.getRecipeManager().byKey(Enhanced.id(id)).isPresent(), "the recipe " + id + " loads");
        }
        check(!SwitchedRecipe.Switch.SERUM.on() && !SwitchedRecipe.Switch.ARC_REACTOR.on(), "and is off by default");
        for (String adv : List.of("super_soldier", "shield_up", "hotter_when_angry", "suit_up", "mark_85", "wings_out",
                "gates_of_eternity", "careful_what_you_wish_for", "thunder_runs_in_the_family")) {
            check(server.getAdvancements().get(Enhanced.id("avengers/" + adv)) != null, "the advancement " + adv);
        }
        for (String entity : List.of("thrown_shield", "iron_missile", "nano_drone", "hulkbuster_frame", "falcon_rocket",
                "falcon_dart", "redwing", "eternity")) {
            check(BuiltInRegistries.ENTITY_TYPE.containsKey(Enhanced.id(entity)), "the entity " + entity);
        }
        check(com.kolodkingal.enhanced.registry.ModEntities.ETERNITY.get().is(ModTags.GRAB_IMMUNE)
                && com.kolodkingal.enhanced.registry.ModEntities.NANO_DRONE.get().is(ModTags.GRAB_IMMUNE)
                && com.kolodkingal.enhanced.registry.ModEntities.HULKBUSTER_FRAME.get().is(ModTags.GRAB_IMMUNE),
                "Eternity, the drones and the frame cannot be grabbed");
        for (String block : List.of("suit_display", "tech_case", "eternity_altar", "eternity_mirror")) {
            check(BuiltInRegistries.BLOCK.containsKey(Enhanced.id(block))
                    && BuiltInRegistries.ITEM.containsKey(Enhanced.id(block)), "the block " + block + " and its item");
        }
        for (String item : List.of("special_soldier_serum", "vibranium_shield", "falcon_exo_wings", "arc_reactor",
                "nano_arc_reactor", "iron_man_mark_1", "iron_man_mark_3", "iron_man_mark_5", "iron_man_mark_50",
                "iron_man_mark_85", "iron_man_hulkbuster")) {
            check(BuiltInRegistries.ITEM.containsKey(Enhanced.id(item)), "the item " + item);
        }
        check(new ItemStack(ModItems.IRON_MAN_MARK_5.get()).get(com.kolodkingal.enhanced.registry.ModDataComponents
                .SUIT.get()) == SuitItemState.FOLDED, "the Mark 5 comes as its suitcase");
        check(new ItemStack(ModItems.IRON_MAN_MARK_85.get()).getMaxDamage() == 2035
                && new ItemStack(ModItems.FALCON_EXO_WINGS.get()).getMaxDamage() == 900
                && !new ItemStack(ModItems.VIBRANIUM_SHIELD.get()).isDamageableItem(),
                        "durabilities; the shield never wears");
        for (String sound : List.of("serum_open", "shield_throw", "red_hulk_roar", "repulsor_blast", "nano_flow",
                "falcon_dart", "suit_display_take", "eternity_ambient", "love_eye_laser")) {
            check(BuiltInRegistries.SOUND_EVENT.containsKey(Enhanced.id(sound)), "the sound " + sound);
        }
        // (3.8 L2, the sound lane, made the 109 files: SoundAssetTests.AVENGERS_EVENTS holds them, no placeholder left.)
        check(SoundAssetTests.SKELETON_38.isEmpty() && SoundAssetTests.AVENGERS_EVENTS.size() == 109,
                "the 109 sound events are made");
        for (String beam : List.of("unibeam", "chest_laser", "hulkbuster_unibeam", "drone_laser", "love_eyes",
                "flamethrower")) {
            check(BeamStyles.byId(beam) != null, "the beam style " + beam);
        }
        check(AvengersBeams.LOVE_EYES == BeamStyles.byId("love_eyes"), "registered once");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theConfigsHaveThePlansDefaults(GameTestHelper helper) {
        check(SerumConfig.ENABLED.getDefault() && SerumConfig.PENDING_TICKS.getDefault() == 600
                && !SerumConfig.CRAFTABLE.getDefault() && SerumConfig.CASE_PER_PLAYER.getDefault() == 1, "the serum's");
        TestKit.assertNear(CaptainConfig.SPEED_BONUS.getDefault(), 0.40, 1.0E-9, "Speed II");
        TestKit.assertNear(CaptainConfig.RESISTANCE.getDefault(), 0.20, 1.0E-9, "Resistance I");
        check(CaptainConfig.RICOCHETS.getDefault() == 3 && CaptainConfig.THROW_RANGE.getDefault() == 24
                && CaptainConfig.CAP_BLOCKS_EVERYTHING.getDefault() && !CaptainConfig.AXE_DISABLES.getDefault(),
                "Cap's and the shield's");
        check(RedHulkConfig.BURST_SETS_FIRE.getDefault() && !RedHulkConfig.SMASH_BREAKS_BLOCKS.getDefault()
                && RedHulkConfig.JUMP_CHARGE_TICKS.getDefault() == 30, "Red Hulk's");
        TestKit.assertNear(RedHulkConfig.SCALE_BONUS.getDefault(), 0.5, 1.0E-9, "1.5 times");
        check(IronManConfig.REACTOR_HOLD_TICKS.getDefault() == 40
                && IronManConfig.SUIT_UP_TICKS_MARK3.getDefault() == 50
                && IronManConfig.SUIT_PHASE_COOLDOWN.getDefault() == 40
                        && !IronManConfig.BOOMS_BREAK_BLOCKS.getDefault()
                && IronManConfig.MARK85_DURABILITY.getDefault() == 2035
                && IronManConfig.MARK85_MISSILE_HOMING_TICKS.getDefault() == 200
                && IronManConfig.HULKBUSTER_EXPLOSION_IMMUNE.getDefault(), "Iron Man's");
        TestKit.assertNear(IronManConfig.SUIT_UP_DAMAGE_FACTOR.getDefault(), 0.5, 1.0E-9, "half damage in a suit-up");
        TestKit.assertNear(IronManConfig.HULKBUSTER_SCALE_BONUS.getDefault(), 2.5, 1.0E-9, "3.5 times");
        check(SuitKind.MARK_3.upTicks() == 50 && SuitKind.MARK_3.downTicks() == 25 && SuitKind.HULKBUSTER.upTicks()
                == 80 && SuitKind.HULKBUSTER.downTicks() == 40 && SuitKind.MARK_85.downTicks() == 12,
                "the phases' lengths");
        check(FalconConfig.REDWING_TICKS.getDefault() == 1200 && FalconConfig.WINGS_DURABILITY.getDefault() == 900,
                "Falcon's");
        check(TowerConfig.DISPLAYS_PER_PLAYER.getDefault() == 1 && TowerConfig.REFIT.getDefault(), "the tower's");
        check(EternityConfig.WISHES_PER_PLAYER.getDefault() == 1 && EternityConfig.WISH_FULL_STACK.getDefault()
                && EternityConfig.LOVE_FLIGHT_NEEDS_AXE.getDefault()
                && EternityConfig.LOVE_EYE_LASER_DAMAGE.getDefault() == 6.0, "Eternity's and Love's");
        TestKit.assertNear(EternityConfig.LOVE_HEALTH_BONUS.getDefault(), 12.0, 1.0E-9, "32 health");
        helper.succeed();
    }

    // ---- the fixed points ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theShrinesFixedPointsHoldTheirBlocks(GameTestHelper helper) throws IOException {
        Map<BlockPos, String> blocks = template(helper, ShrinePlan.TEMPLATE);
        check(size(helper, ShrinePlan.TEMPLATE).equals(List.of(47, 34, 47)), "the shrine is 47 x 34 x 47");
        check(ShrinePlan.SIZE.getX() == 47 && ShrinePlan.SIZE.getY() == 34 && ShrinePlan.SIZE.getZ() == 47,
                "and so says the plan");
        check("enhanced:eternity_altar".equals(blocks.get(ShrinePlan.ALTAR)), "the altar in the middle");
        check("minecraft:lectern".equals(blocks.get(ShrinePlan.LECTERN)), "the lectern by the stair");
        check(blocks.containsKey(ShrinePlan.ETERNITY_STATUE), "Eternity's statue stands behind the altar");
        check(ShrinePlan.STATUES.size() == 8 && ShrinePlan.STATUES.stream().map(ShrinePlan.Statue::id)
                .distinct().count() == 8, "eight statues");
        for (ShrinePlan.Statue statue : ShrinePlan.STATUES) {
            check(blocks.containsKey(statue.plinth()) && !"minecraft:air".equals(blocks.get(statue.plinth())),
                    statue.id() + "'s plinth");
            check(Math.abs(statue.plinth().distSqr(ShrinePlan.CENTER) - 18 * 18) < 40,
                    statue.id() + " on the ring round the altar");
        }
        BlockPos order = ShrinePlan.STATUES.stream().filter(s -> s.id().equals("master_order")).findFirst()
                .orElseThrow().plinth();
        BlockPos chaos = ShrinePlan.STATUES.stream().filter(s -> s.id().equals("lord_chaos")).findFirst()
                .orElseThrow().plinth();
        check(order.getX() + chaos.getX() == 2 * ShrinePlan.CENTER.getX()
                && order.getZ() + chaos.getZ() == 2 * ShrinePlan.CENTER.getZ(), "Master Order faces Lord Chaos");
        check(blocks.get(ShrinePlan.CENTER.below(1)) != null, "the plaza under the altar");
        helper.succeed();
    }

    /** The armoury's points lie in their rooms, and map into the tower's three templates as the cradle does. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theTowerPointsLieInTheirRooms(GameTestHelper helper) {
        check(TowerPlan.SUIT_DISPLAYS.size() == SuitKind.values().length, "a display for every suit");
        for (Map.Entry<SuitKind, BlockPos> e : TowerPlan.SUIT_DISPLAYS.entrySet()) {
            check(TowerPlan.WORKSHOP.isInside(e.getValue()) && TowerPlan.WORKSHOP.isInside(e.getValue().above()),
                    e.getKey() + "'s display (both halves) is in the workshop");
            check(TowerPlan.inTemplate(e.getValue().above()).part() == 1, e.getKey() + " is in the mid template");
        }
        check(TowerPlan.WORKSHOP.isInside(TowerPlan.ARC_REACTOR_CASE)
                && TowerPlan.WORKSHOP.isInside(TowerPlan.NANO_REACTOR_CASE), "the reactors on the desk");
        check(TowerPlan.LAB.isInside(TowerPlan.SERUM_CASE), "the serum in the lab");
        check(TowerPlan.PENTHOUSE.isInside(TowerPlan.SHIELD_CASE), "Cap's shield in his closet");
        check(TowerPlan.OBSERVATION.isInside(TowerPlan.WINGS_CASE)
                && TowerPlan.OBSERVATION.isInside(TowerPlan.CARTOGRAPHY_TABLE), "the wings on the deck");
        TowerPlan.InTemplate mark3 = TowerPlan.inTemplate(TowerPlan.SUIT_DISPLAYS.get(SuitKind.MARK_3));
        check(mark3.part() == 1 && mark3.template().equals(StoneSitePlans.Tower.MID)
                && mark3.local().equals(new BlockPos(4, 30, 4)), "the Mark 3 in the mid template: " + mark3);
        TowerPlan.InTemplate wings = TowerPlan.inTemplate(TowerPlan.WINGS_CASE);
        check(wings.part() == 2 && wings.local().equals(new BlockPos(14, 26, 16)), "the wings in the top: " + wings);
        BlockPos cradle = StoneSitePlans.Tower.CRADLE_IN_TOP.offset(0, StoneSitePlans.Tower.PART_Y[2], 0);
        check(TowerPlan.inTemplate(cradle).local().equals(StoneSitePlans.Tower.CRADLE_IN_TOP)
                && TowerPlan.inTower(cradle).equals(StoneSitePlans.Tower.CRADLE), "the same frames as the cradle's");
        Vec3iCheck.inside(mark3.local(), StoneSitePlans.Tower.MID_SIZE);
        Vec3iCheck.inside(wings.local(), StoneSitePlans.Tower.TOP_SIZE);
        Vec3iCheck.inside(TowerPlan.inTemplate(TowerPlan.SERUM_CASE).local(), StoneSitePlans.Tower.MID_SIZE);
        Vec3iCheck.inside(TowerPlan.inTemplate(TowerPlan.SHIELD_CASE).local(), StoneSitePlans.Tower.TOP_SIZE);
        helper.succeed();
    }

    private static final class Vec3iCheck {
        static void inside(BlockPos p, net.minecraft.core.Vec3i size) {
            check(p.getX() >= 0 && p.getY() >= 0 && p.getZ() >= 0 && p.getX() < size.getX() && p.getY() < size.getY()
                    && p.getZ() < size.getZ(), p + " lies inside its template " + size);
        }
    }

    // ---- the seams, neutral until the lanes ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theSeamsAreNeutralUntilTheLanes(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // Only Iron Man (or a Creative player) may wear a suit. (3.8, lane L3: the suits work now: a Mark 3 with a
        // reactor in his pocket is powered, flies at its 0.10 and is iron for Magneto; IronCoreTests has the rest.)
        ServerPlayer tony = f.player(3.5, FLOOR, 3.5, Power.IRON_MAN);
        tony.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.IRON_MAN_MARK_3.get()));
        tony.getInventory().add(new ItemStack(ModItems.ARC_REACTOR.get()));
        check(IronSuits.worn(tony) == SuitKind.MARK_3 && IronSuits.hasReactor(tony) && IronSuits.mayWear(tony),
                "he wears a Mark 3, a reactor in his pocket");
        check(IronSuits.powered(tony) && IronSuits.isMetal(tony) && Math.abs(IronSuits.flightSpeed(tony) - 0.10F)
                < 1.0E-6F, "and it works (lane L3)");
        PowerTicker.applyPassives(tony, Powers.get(tony));
        check(tony.mayFly(), "so he flies");
        check(Metal.isMagnetic(tony), "and is Magneto's: a powered classic suit is iron");
        ServerPlayer plain = f.player(6.5, FLOOR, 3.5);
        check(!IronSuits.mayWear(plain) && !Metal.isMagnetic(plain), "a powerless player may not wear a suit");
        plain.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        check(Metal.isMagnetic(plain), "but iron armour is metal, as before");
        // Falcon flies with his wings on (lane L5 filled FalconWings.canFly).
        ServerPlayer sam = f.player(9.5, FLOOR, 3.5, Power.FALCON);
        HeroItems.give(sam, Power.FALCON, "endgame");
        check(FalconWings.carries(sam) && FalconWings.worn(sam), "Choose a hero puts the wings on his back");
        check(FalconWings.canFly(sam), "with them on his back he flies (lane L5)");
        ServerPlayer love = f.player(12.5, FLOOR, 3.5, Power.LOVE);
        love.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.STORMBREAKER.get()));
        // (3.8, lane L8: Love flies while she holds the axe: LoveTests.)
        check(LoveAbilities.holdsStormbreaker(love) && LoveAbilities.canFly(love), "Love holds the axe and may fly");
        check(Powers.get(love).has(HeroAbility.LOVE_AXE) && !Powers.get(love).has(HeroAbility.THOR_HAMMER),
                "the thrown axe flies home to her by her own row");
        check(com.kolodkingal.enhanced.bifrost.BifrostKeys.opensScreen(love, 5, false)
                && !com.kolodkingal.enhanced.bifrost.BifrostKeys.opensScreen(love, 5, true),
                "her N opens the Bifrost with the axe; Sneak + N goes to the server");
        ServerPlayer cap = f.player(15.5, FLOOR, 3.5);
        ItemStack serum = new ItemStack(ModItems.SPECIAL_SOLDIER_SERUM.get());
        cap.setItemInHand(InteractionHand.MAIN_HAND, serum);
        answer(SerumChoicePayload.class, new SerumChoicePayload(Power.CAPTAIN_AMERICA), cap);
        check(Powers.power(cap) == Power.NONE && cap.getMainHandItem().is(ModItems.SPECIAL_SOLDIER_SERUM.get()),
                "a choice with no serum screen open changes nobody and keeps the serum (lane L1: SerumTests)");
        // (3.8, lane L8: the realm opens through the altar's charge: EternityTests.)
        check(!EternityRealm.in(cap), "nobody is taken to the realm by a choice");
        // H: Wolverine's looks still cycle; Iron Man's H is the suit key (lane L3), never his looks.
        ServerPlayer logan = f.player(3.5, FLOOR, 8.5, Power.WOLVERINE);
        String before = Powers.variant(logan).id();
        answer(ChangeLookPayload.class, new ChangeLookPayload(1), logan);
        check(!Powers.variant(logan).id().equals(before), "Wolverine's next look: " + Powers.variant(logan).id());
        String tonyLook = Powers.variant(tony).id();
        answer(ChangeLookPayload.class, new ChangeLookPayload(1), tony);
        check(Powers.variant(tony).id().equals(tonyLook), "Iron Man's H does not cycle his looks");
        // Thor's lightning beam, through its new door, in his colours.
        ServerPlayer thor = f.player(6.5, FLOOR, 8.5, Power.THOR);
        ThorAbilities.onKey(thor, Powers.get(thor), 3, true, false);
        check(Powers.get(thor).is(Live.CALLING_LIGHTNING), "Thor's beam starts");
        int[] colours = ThorAbilities.lightningBeamColours(thor);
        check(colours[0] == Power.THOR.color && colours[1] == Power.THOR.glow, "in Thor's colours");
        ThorAbilities.onKey(thor, Powers.get(thor), 3, false, false);
        check(!Powers.get(thor).is(Live.CALLING_LIGHTNING), "and stops");
        // The pure stand-ins.
        Zombie dummy = helper.spawn(EntityType.ZOMBIE, new Vec3(10.5, FLOOR, 10.5));
        dummy.setDeltaMovement(0.1, 0.2, 0.3);
        check(MissileGuidance.steer(dummy).equals(new Vec3(0.1, 0.2, 0.3)), "missiles fly straight");
        dummy.discard();
        // (The skeleton's linear timelines are lane L7's choreographies now, SuitArtTests: here only their ends.)
        for (SuitParts part : SuitParts.VALUES) {
            check(SuitTimelines.reveal(SuitKind.MARK_85, part, 0.0F, true) == 0.0F
                    && SuitTimelines.reveal(SuitKind.MARK_85, part, 1.0F, true) == 1.0F, part + " up");
            check(SuitTimelines.reveal(SuitKind.MARK_85, part, 0.0F, false) == 1.0F
                    && SuitTimelines.reveal(SuitKind.MARK_85, part, 1.0F, false) == 0.0F, part + " down");
        }
        TestKit.assertNear(SuitTimelines.progress(110L, 100L, 20, 0.5F), 0.525, 1.0E-6, "the progress");
        check(SuitTimelines.progress(200L, 100L, 20, 0.0F) == 1.0F, "and it ends at 1");
        helper.succeed();
    }

    /** The Hulk's moves through their new doors: the old signatures keep the Hulk's numbers and cooldowns. */
    @GameTest(template = ARENA, batch = CFG)
    public static void theHulksMovesKeepTheirNumbers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerPlayer hulk = f.player(3.5, FLOOR, 3.5, Power.HULK);
        PowerData data = Powers.get(hulk);
        long now = hulk.level().getGameTime();
        check(SuperLeap.bound(hulk, data), "the Hulk bounds");
        SuperLeap leap = SuperLeap.of(hulk);
        check(leap != null && HulkAbilities.moving(hulk), "his move is the Bound");
        double reach = Math.hypot(leap.target().x - hulk.getX(), leap.target().z - hulk.getZ());
        TestKit.assertNear(reach, com.kolodkingal.enhanced.config.HulkConfig.BOUND_DISTANCE.get(), 0.01,
                "the Hulk's bound distance");
        check(!data.isReady(HeroAbility.HULK_BOUND, now), "and its cooldown");
        ServerPlayer other = f.player(9.5, FLOOR, 3.5, Power.HULK);
        PowerData otherData = Powers.get(other);
        check(SuperLeap.bound(other, otherData, 20.0, 0.0F), "a bound with the numbers given");
        SuperLeap given = SuperLeap.of(other);
        TestKit.assertNear(Math.hypot(given.target().x - other.getX(), given.target().z - other.getZ()), 20.0, 0.01,
                "20 blocks");
        check(otherData.isReady(HeroAbility.HULK_BOUND, now), "no cooldown asked or started: the caller's");
        ServerPlayer grabber = f.player(15.5, FLOOR, 3.5, Power.HULK);
        Zombie first = helper.spawn(EntityType.ZOMBIE, new Vec3(15.5, FLOOR, 5.5));
        check(GrabSmash.start(grabber, Powers.get(grabber)) == first, "the Hulk grabs the zombie in front");
        check(!Powers.get(grabber).isReady(HeroAbility.HULK_GRAB_SMASH, now), "with his cooldown");
        ServerPlayer red = f.player(3.5, FLOOR, 12.5, Power.HULK);
        Zombie second = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, FLOOR, 14.5));
        check(GrabSmash.start(red, Powers.get(red), new double[]{5, 5, 7}, 8.0) == second,
                "the grab with the numbers given");
        check(Powers.get(red).isReady(HeroAbility.HULK_GRAB_SMASH, now), "and no cooldown of the Hulk's");
        first.discard();
        second.discard();
        helper.succeed();
    }

    /** Gungnir's give-and-record, out of the stand for Eternity's wish: bound to him, the world's bearer recorded. */
    @GameTest(template = ARENA, batch = CFG)
    public static void gungnirsGiveRecordsTheBearer(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        AsgardTestSupport.ensureAsgard(server);
        AsgardState state = AsgardState.get(server);
        UUID was = state.gungnirOwner();
        f.onClose(() -> state.setGungnirOwner(was));
        state.setGungnirOwner(null);
        ServerPlayer odin = f.player(3.5, FLOOR, 3.5);
        check(GungnirStand.onePerWorld(), "one Gungnir per world by default");
        check(GungnirStand.giveTo(odin), "he becomes the world's bearer");
        check(odin.getUUID().equals(GungnirStand.owner(server)), "recorded");
        check(odin.getInventory().contains(s -> s.is(ModItems.GUNGNIR.get())
                && com.kolodkingal.enhanced.item.GungnirItem.isOwnGungnir(odin, s)), "his own Gungnir");
        ServerPlayer other = f.player(6.5, FLOOR, 3.5);
        check(!GungnirStand.giveTo(other) && odin.getUUID().equals(GungnirStand.owner(server)),
                "a second give records nobody new (the caller checks the owner first)");
        helper.succeed();
    }

    /** What the server does with a message the client sends (the payload's own handler, as in RoadEndingTests). */
    private static <T> void answer(Class<T> type, T payload, ServerPlayer player) {
        try {
            Method handle = type.getDeclaredMethod("handle", type, PacketContext.class);
            handle.setAccessible(true);
            PacketContext context = (PacketContext) Proxy.newProxyInstance(PacketContext.class.getClassLoader(),
                    new Class[]{PacketContext.class}, (proxy, method, args) -> method.getName().equals("player")
                            ? player : null);
            handle.invoke(null, payload, context);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- helpers ----

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

    private static JsonObject lang(String name) throws IOException {
        IModFileInfo mod = ModList.get().getModFileById(Enhanced.MODID);
        return JsonParser.parseString(Files.readString(mod.getFile().findResource("assets", Enhanced.MODID, "lang",
                name + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private AvengersSkeletonTests() {
    }
}
