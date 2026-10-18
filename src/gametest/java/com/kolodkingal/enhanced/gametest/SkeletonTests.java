package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.HeroServer;
import com.kolodkingal.enhanced.ability.HeroServers;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.boss.BossArena;
import com.kolodkingal.enhanced.boss.BossData;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossGrip;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.boss.StopReason;
import com.kolodkingal.enhanced.build.ShapeKind;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.GestureRoutes;
import com.kolodkingal.enhanced.client.render.HeroClient;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.item.AwakeningItem;
import com.kolodkingal.enhanced.item.ScepterRituals;
import com.kolodkingal.enhanced.item.ScepterRituals.Ritual;
import com.kolodkingal.enhanced.network.ApocalypseFxPayload;
import com.kolodkingal.enhanced.network.BuildActionPayload;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.network.BuildFxPayload;
import com.kolodkingal.enhanced.network.BuildPocketPayload;
import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.network.MagnetPullPayload;
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
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTags;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    /**
     * The batch of the tests that need nothing but themselves. They all finish in their first tick, and a finished test
     * lets go of the chunks it had forced. The grid puts tests side by side, and a thrown weapon that leaves its own
     * arena (the Stormbreaker test throws 16 blocks east) stops ticking in a chunk nobody holds, and never comes back.
     * So these tests run in a batch of their own and are never the neighbours of the others.
     */
    private static final String SKELETON = "skeleton";

    /** The heroes of 3.0, in Eilon's order: the last five of {@link Power}. */
    private static final List<Power> NEW_HEROES = List.of(Power.PHOENIX, Power.SPIDER_MAN, Power.THANOS, Power.ULTRON, Power.APOCALYPSE);

    /**
     * Whether the hero's lane is built: its plug-in does something with the keys, so the tests that pin a stub ("does
     * nothing yet", "G is not wired yet") leave it to the lane's own tests.
     */
    private static boolean wired(Power hero) {
        try {
            return HeroServers.of(hero).getClass().getMethod("onKey", ServerPlayer.class, PowerData.class, int.class, boolean.class)
                    .getDeclaringClass() != HeroServer.class;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The five powers

    @GameTest(template = ARENA, batch = SKELETON)
    public static void theFiveNewHeroesComeLastInEilonsOrder(GameTestHelper helper) {
        Power[] all = Power.values();
        // (The heroes of 3.3 are appended after them: Apocalypse is no longer the last.)
        for (int i = 0; i < NEW_HEROES.size(); i++) {
            check(all[Power.THOR.ordinal() + 1 + i] == NEW_HEROES.get(i), "hero " + i + " of 3.0 is " + NEW_HEROES.get(i));
        }
        check(all[Power.PHOENIX.ordinal() - 1] == Power.THOR, "the heroes of 3.0 follow Thor (the stream sends the ordinal)");
        check(Power.THOR.ordinal() == 8, "and nobody before them moved: Thor is still the ninth power");
        helper.succeed();
    }

    /** A name for everything, a look to choose, and flight rules that agree with each other. */
    @GameTest(template = ARENA, batch = SKELETON)
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
    @GameTest(template = ARENA, batch = SKELETON)
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

    /** A hero whose lane is not built yet: its plug-in is the stub the skeleton wrote, the power and nothing else. */
    private static boolean isStub(Power hero) {
        HeroServer server = HeroServers.of(hero);
        return server != null && server.getClass().getDeclaredMethods().length <= 1;
    }

    /** The power exists and does nothing: no key does anything, nothing ticks into a state, nobody is hurt. */
    @GameTest(template = ARENA, batch = SKELETON)
    public static void aNewHeroDoesNothingYet(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        for (Power hero : NEW_HEROES) {
            if (wired(hero)) {
                continue;
            }
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

    /**
     * What a lane's plug-in can count on (the lane guide, section 3): its key for its own player only, its tick for its
     * own player while alive, the everyPlayer and body calls for everybody, the hold release when the player is dead,
     * endAll when the power is taken away (while the data still say whose it was), and one call per session event for
     * every player, from the real events.
     */
    @GameTest(template = ARENA, batch = SKELETON)
    public static void aHeroPlugInIsCalledForTheRightPlayersAndOncePerSessionEvent(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        List<String> calls = new ArrayList<>();
        HeroServer recorder = new HeroServer() {
            @Override
            public Power power() {
                return Power.PHOENIX;
            }

            @Override
            public void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
                calls.add("key" + ability + (down ? "v" : "^") + ":" + name(player));
            }

            @Override
            public void tick(ServerPlayer player, PowerData data) {
                calls.add("tick:" + name(player));
            }

            @Override
            public void everyPlayerTick(ServerPlayer player, PowerData data) {
                calls.add("every:" + name(player));
            }

            @Override
            public void applyBody(ServerPlayer player, PowerData data) {
                calls.add("body:" + name(player) + ":" + data.power());
            }

            @Override
            public void endAll(ServerPlayer player, PowerData data) {
                calls.add("end:" + name(player) + ":" + data.power());
            }

            @Override
            public void stopHold(PowerData data) {
                calls.add("hold");
            }

            @Override
            public void onLogin(ServerPlayer player) {
                calls.add("login:" + name(player));
            }

            @Override
            public void onLogout(ServerPlayer player) {
                calls.add("logout:" + name(player));
            }

            @Override
            public void onDeath(ServerPlayer player) {
                calls.add("death:" + name(player));
            }

            @Override
            public void onRespawn(ServerPlayer player) {
                calls.add("respawn:" + name(player));
            }

            @Override
            public void onChangeDimension(ServerPlayer player) {
                calls.add("dimension:" + name(player));
            }

            @Override
            public void onServerStopped() {
                calls.add("stopped");
            }

            private String name(ServerPlayer player) {
                return player.getGameProfile().getName();
            }
        };
        HeroServer previous = HeroServers.register(recorder);
        f.onClose(() -> HeroServers.register(previous));
        ServerPlayer phoenix = f.player(5.5, FLOOR, 5.5, Power.PHOENIX);
        ServerPlayer wanda = f.player(9.5, FLOOR, 5.5, Power.SCARLET_WITCH);
        ServerPlayer third = f.player(13.5, FLOOR, 5.5, Power.PHOENIX);
        String hero = phoenix.getGameProfile().getName();
        String other = wanda.getGameProfile().getName();
        calls.clear();

        // Keys go to the hero's own plug-in, for the hero's own player only.
        AbilityHandler.onKey(phoenix, 3, true);
        AbilityHandler.onKey(phoenix, 3, false);
        AbilityHandler.onKey(wanda, 5, false);
        check(calls.equals(List.of("key3v:" + hero, "key3^:" + hero)), "keys: " + calls);

        // A tick: everybody's everyPlayerTick and applyBody, the hero's own tick.
        calls.clear();
        PowerTicker.tick(phoenix);
        check(calls.equals(List.of("every:" + hero, "body:" + hero + ":PHOENIX", "tick:" + hero)), "the hero's tick: " + calls);
        calls.clear();
        PowerTicker.tick(wanda);
        check(calls.equals(List.of("every:" + other, "body:" + other + ":SCARLET_WITCH")), "another hero's tick: " + calls);

        // Taking the power away: endAll while the data still say whose it was, then the body for the new power.
        calls.clear();
        Powers.setPower(third, Power.NONE);
        check(calls.equals(List.of("end:" + third.getGameProfile().getName() + ":PHOENIX", "body:" + third.getGameProfile().getName() + ":NONE")),
                "losing the power: " + calls);

        // Dying: the real event calls onDeath once; a dead body releases its holds and does not tick.
        calls.clear();
        phoenix.kill();
        check(calls.equals(List.of("death:" + hero)), "death: " + calls);
        calls.clear();
        PowerTicker.tick(phoenix);
        check(calls.equals(List.of("hold")), "a dead hero's tick only lets go of what it held: " + calls);

        // The session events, once each, for the player they are about.
        for (var event : List.of(new PlayerEvent.PlayerLoggedInEvent(wanda), new PlayerEvent.PlayerLoggedOutEvent(wanda),
                new PlayerEvent.PlayerRespawnEvent(wanda, false), new PlayerEvent.PlayerChangedDimensionEvent(wanda, Level.OVERWORLD, Level.NETHER))) {
            calls.clear();
            NeoForge.EVENT_BUS.post(event);
            String expected = (event instanceof PlayerEvent.PlayerLoggedInEvent ? "login:" : event instanceof PlayerEvent.PlayerLoggedOutEvent ? "logout:"
                    : event instanceof PlayerEvent.PlayerRespawnEvent ? "respawn:" : "dimension:") + other;
            check(calls.equals(List.of(expected)), event.getClass().getSimpleName() + ": " + calls);
        }
        calls.clear();
        HeroLifecycle.onServerStopped();
        check(calls.equals(List.of("stopped")), "the server stopping: " + calls);
        helper.succeed();
    }

    /** A client plug-in that answers every gesture route with a gesture of Thor's, so a test can tell who was asked. */
    private static final class RoutingClient implements HeroClient {
        @Override
        public Power power() {
            return Power.PHOENIX;
        }

        @Override
        public GestureId cooldownGesture(int ability, long left) {
            return ability == 2 ? GestureId.THOR_CATCH : null;
        }

        @Override
        public GestureId flagGesture(GestureRoutes.Flag flag, boolean on) {
            return flag == GestureRoutes.Flag.BEAM && on ? GestureId.THOR_ARRIVAL : null;
        }

        @Override
        public GestureId liveGesture(Live live) {
            return live == Live.CASTING ? GestureId.THOR_SHATTER : null;
        }

        @Override
        public GestureId grabGesture() {
            return GestureId.THOR_REFORM;
        }

        @Override
        public GestureId flingGesture() {
            return GestureId.THOR_BEAM_KICK;
        }

        @Override
        public boolean startedElsewhere(GestureId id) {
            return id == GestureId.THOR_BEAM_KICK_BARE;
        }
    }

    /**
     * The routes from what the data show to the gesture to act (GestureRoutes) ask a hero of 3.0 what it means, so a lane
     * routes its own abilities without editing the arms of the others, and the heroes before it answer as ever.
     */
    @GameTest(template = ARENA, batch = SKELETON)
    public static void theGestureRoutesAskTheClientOfAHeroOf3AndLeaveTheOlderHeroesAlone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        HeroClient previous = HeroClients.register(new RoutingClient());
        f.onClose(() -> HeroClients.register(previous));
        check(GestureRoutes.cooldown(Power.PHOENIX, 2, 5) == GestureId.THOR_CATCH && GestureRoutes.cooldown(Power.PHOENIX, 1, 5) == null,
                "the cooldown of an ability of the new hero");
        check(GestureRoutes.flag(Power.PHOENIX, GestureRoutes.Flag.BEAM, true, false, false) == GestureId.THOR_ARRIVAL
                && GestureRoutes.flag(Power.PHOENIX, GestureRoutes.Flag.BEAM, false, false, false) == null, "its flags");
        check(GestureRoutes.live(Power.PHOENIX, Live.CASTING, false) == GestureId.THOR_SHATTER
                && GestureRoutes.live(Power.PHOENIX, Live.PINNED, false) == null, "its states");
        check(GestureRoutes.grab(Power.PHOENIX) == GestureId.THOR_REFORM && GestureRoutes.fling(Power.PHOENIX) == GestureId.THOR_BEAM_KICK,
                "its grab and its fling");
        check(GestureRoutes.startedElsewhere(GestureId.THOR_BEAM_KICK_BARE) && !GestureRoutes.startedElsewhere(GestureId.THOR_CATCH),
                "and the gestures a payload starts");
        // The heroes before it are as they were (and are not asked): the arms of their own routes.
        check(GestureRoutes.cooldown(Power.LOKI, 1, 5) == GestureId.LOKI_DAGGER && GestureRoutes.cooldown(Power.MAGNETO, 3, 5) == GestureId.MAG_DISARM,
                "the cooldowns of the older heroes");
        check(GestureRoutes.flag(Power.SCARLET_WITCH, GestureRoutes.Flag.BEAM, true, false, false) == GestureId.WANDA_BEAM_KICK
                && GestureRoutes.live(Power.LOKI, Live.CASTING, false) == GestureId.LOKI_CAST
                && GestureRoutes.grab(Power.BILLY) == GestureId.TK_GRAB_BILLY && GestureRoutes.fling(Power.SCARLET_WITCH) == GestureId.TK_FLING_WANDA,
                "their flags, states, grabs and flings");
        check(GestureRoutes.startedElsewhere(GestureId.TAKEOFF_THOR) && GestureRoutes.startedElsewhere(GestureId.THOR_PIN), "and what they start elsewhere");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = SKELETON)
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
            if (hero.canFly() && !wired(hero)) {
                AbilityHandler.onKey(player, hero.flightAbility(), true);
                check(!player.getAbilities().flying, hero + ": G is not wired yet, so nothing took off");
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = SKELETON)
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
    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
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
    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
    public static void theHeroesAreAllRegisteredAsPlugIns(GameTestHelper helper) {
        for (Power hero : NEW_HEROES) {
            check(HeroServers.of(hero) != null && HeroServers.of(hero).power() == hero, hero + " has a server plug-in");
            check(HeroClients.of(hero) != null, hero + " has a client plug-in");
        }
        // (The heroes of 3.3 are plug-ins too: every power after Thor is one, and none before.)
        int after = Power.values().length - Power.PHOENIX.ordinal();
        check(HeroServers.all().size() == after, "and nobody else is a plug-in");
        for (Power power : Power.values()) {
            check((HeroServers.of(power) != null) == (power.ordinal() > Power.THOR.ordinal()), power + " is a plug-in only after Thor");
        }
        for (Power old : List.of(Power.NONE, Power.MAGNETO, Power.SCARLET_WITCH, Power.QUICKSILVER, Power.VISION, Power.BILLY,
                Power.TOMMY, Power.LOKI, Power.THOR)) {
            check(HeroServers.of(old) == null, old + " keeps its switch arms");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
    public static void mindPowerIsTheBeamTheSlamAndTheBurst(GameTestHelper helper) {
        Registry<DamageType> registry = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        Set<String> inTag = new LinkedHashSet<>();
        registry.holders().filter(h -> h.is(ModTags.MIND_POWER)).forEach(h -> inTag.add(h.key().location().toString()));
        check(inTag.equals(Set.of("enhanced:chaos_beam", "enhanced:telekinetic_slam", "enhanced:phoenix_burst")), "mind power: " + inTag);
        helper.succeed();
    }

    /**
     * The Phoenix's side of Apocalypse's shield (phoenix.md 4.8), written by the skeleton so that the Apocalypse lane
     * builds against the real thing: mind power is the tag with a Phoenix as the causing entity, and the listeners are
     * told of every touch.
     */
    @GameTest(template = ARENA, batch = SKELETON)
    public static void phoenixPowerIsTheMindPowerTagWithAPhoenixCauseAndTellsItsListeners(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer phoenix = f.player(5.5, FLOOR, 5.5, Power.PHOENIX);
        ServerPlayer wanda = f.player(9.5, FLOOR, 5.5, Power.SCARLET_WITCH);
        ServerPlayer nobody = f.player(13.5, FLOOR, 5.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, FLOOR, 9.5));

        check(PhoenixPower.isPhoenix(phoenix) && !PhoenixPower.isPhoenix(wanda) && !PhoenixPower.isPhoenix(zombie), "who is a Phoenix");
        check(PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, phoenix, phoenix)),
                "the Burst of a Phoenix is her mind power");
        check(PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.TELEKINETIC_SLAM, phoenix, zombie)),
                "so is the slam of what she threw");
        check(!PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, wanda, wanda)),
                "Wanda's beam is mind power, but not the Phoenix's");
        check(!PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.PHOENIX_BURST, nobody, nobody)),
                "a Burst nobody who is a Phoenix caused is not (a stray source)");
        check(!PhoenixPower.isPhoenixMindPower(ModDamageTypes.source(level, ModDamageTypes.SHRAPNEL, phoenix, phoenix))
                && !PhoenixPower.isPhoenixMindPower(level.damageSources().generic()), "nor is anything outside the tag");

        List<String> told = new ArrayList<>();
        PhoenixPower.Listener listener = (who, target, kind, amount) -> told.add(kind + ":" + amount + ":" + (who == phoenix) + ":" + (target == zombie));
        PhoenixPower.addListener(listener);
        f.onClose(() -> PhoenixPower.removeListener(listener));
        PhoenixPower.touched(phoenix, zombie, PhoenixPower.Kind.MIND_CONTROL, 0.0F);
        PhoenixPower.touched(phoenix, zombie, PhoenixPower.Kind.BURST, 12.5F);
        check(told.equals(List.of("MIND_CONTROL:0.0:true:true", "BURST:12.5:true:true")), "the listener was told of both touches: " + told);
        PhoenixPower.removeListener(listener);
        PhoenixPower.touched(phoenix, zombie, PhoenixPower.Kind.SLAM, 3.0F);
        check(told.size() == 2, "and not after it was removed");
        helper.succeed();
    }

    /**
     * Every tag file of the mod parses, and every entry names a real thing or a tag that exists. An entry written
     * {@code {"id": ..., "required": false}} is skipped: it names what another mod may add (other mods' metal ores), and
     * the game loads the tag without it when that mod is not installed. The twelve tag files of 3.1 are among those read.
     */
    @GameTest(template = ARENA, batch = SKELETON)
    public static void everyTagFileNamesRealThings(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        ServerLevel level = helper.getLevel();
        List<String> problems = new ArrayList<>();
        Set<String> read = new HashSet<>();
        int files = 0;
        int optional = 0;
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
                    read.add(namespace + ":" + kind + "/" + name);
                    JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                    for (JsonElement entry : json.getAsJsonArray("values")) {
                        if (entry.isJsonObject() && entry.getAsJsonObject().has("required")
                                && !entry.getAsJsonObject().get("required").getAsBoolean()) {
                            optional++;
                            continue;
                        }
                        String id = entry.isJsonObject() ? entry.getAsJsonObject().get("id").getAsString() : entry.getAsString();
                        boolean isTag = id.startsWith("#");
                        ResourceLocation location = ResourceLocation.parse(isTag ? id.substring(1) : id);
                        boolean found = switch (kind) {
                            case "entity_type" -> exists(BuiltInRegistries.ENTITY_TYPE, location, isTag);
                            case "block" -> exists(BuiltInRegistries.BLOCK, location, isTag);
                            case "item" -> exists(BuiltInRegistries.ITEM, location, isTag);
                            case "damage_type" -> exists(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE), location, isTag);
                            case "block_entity_type" -> exists(BuiltInRegistries.BLOCK_ENTITY_TYPE, location, isTag);
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
        List<String> of31 = List.of("entity_type/pull_immune", "entity_type/held_as_bosses", "entity_type/conjured_bodies",
                "block/magnetic_blocks", "block/magnetic_ores", "block/build_untouchable", "block/build_unconjurable", "block/build_volatile",
                "block/build_ground", "block/build_plants", "block_entity_type/build_copies_data", "block_entity_type/build_holds_items");
        List<String> missing = of31.stream().filter(name -> !read.contains("enhanced:" + name)).toList();
        check(missing.isEmpty(), "the tag files of 3.1 are there to check: " + missing + " missing");
        check(optional > 0, "the entries for other mods' metals are skipped (" + optional + ")");
        check(problems.isEmpty(), "bad tag entries: " + problems);
        helper.succeed();
    }

    private static <T> boolean exists(Registry<T> registry, ResourceLocation location, boolean isTag) {
        return isTag ? registry.getTag(TagKey.create(registry.key(), location)).isPresent() : registry.containsKey(location);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Loki's Scepter and the Tesseract: one table for both

    /** The table of integration.md 3.2, row by row. */
    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
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
    @GameTest(template = ARENA, batch = SKELETON)
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

    /**
     * Once the Thanos lane has registered its summoning, the call to the Mad Titan is the lane's alone: a summoning that
     * refuses (it has told the player why) must not leave the Scepter to explode in the player's hand, nor the Tesseract
     * to take them a hundred blocks away as Loki. One that begins holds the item for five seconds.
     */
    @GameTest(template = ARENA, batch = SKELETON)
    public static void aRefusedSummoningNeitherExplodesTheScepterNorSendsTheTesseractAway(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = helper.getLevel();
        int[] begun = {0};
        boolean[] accept = {false};
        ScepterRituals.registerThanosHold(new ScepterRituals.Hold() {
            @Override
            public boolean begin(ServerPlayer player, InteractionHand hand) {
                begun[0]++;
                return accept[0];
            }

            @Override
            public void tick(ServerPlayer player, ItemStack stack, int remaining) {
            }

            @Override
            public void release(ServerPlayer player, ItemStack stack) {
            }

            @Override
            public void finish(ServerPlayer player, ItemStack stack) {
            }
        });

        // The Scepter in the main hand, the Tesseract in the other: refused.
        ServerPlayer holder = f.player(5.5, FLOOR, 5.5);
        ItemStack scepter = new ItemStack(ModItems.LOKIS_SCEPTER.get());
        holder.setItemInHand(InteractionHand.MAIN_HAND, scepter);
        holder.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TESSERACT.get()));
        float health = holder.getHealth();
        InteractionResultHolder<ItemStack> used = scepter.getItem().use(level, holder, InteractionHand.MAIN_HAND);
        check(begun[0] == 1, "the summoning was asked");
        check(holder.isAlive() && holder.getHealth() == health, "a refusal leaves the Scepter's holder whole, not torn apart");
        check(!holder.isUsingItem() && used.getResult().consumesAction(), "no hold begins, and the Scepter is only on its cooldown");

        // The Tesseract in the main hand, the Scepter in the other: refused, nobody is taken away or made Loki.
        ServerPlayer traveller = f.player(9.5, FLOOR, 5.5);
        ItemStack tesseract = new ItemStack(ModItems.TESSERACT.get());
        traveller.setItemInHand(InteractionHand.MAIN_HAND, tesseract);
        traveller.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        Vec3 where = traveller.position();
        InteractionResultHolder<ItemStack> tried = tesseract.getItem().use(level, traveller, InteractionHand.MAIN_HAND);
        check(begun[0] == 2, "the summoning was asked again");
        check(traveller.position().equals(where) && Powers.power(traveller) == Power.NONE, "a refusal does not send the Tesseract's holder away as Loki");
        check(!traveller.isUsingItem() && !tried.getResult().consumesAction(), "and no hold begins");
        check(!traveller.getCooldowns().isOnCooldown(tesseract.getItem()), "nor does the Tesseract cool down");

        // The same hands with a summoning that begins: the item is held (five seconds), nothing else happens.
        accept[0] = true;
        ServerPlayer caller = f.player(13.5, FLOOR, 5.5);
        ItemStack held = new ItemStack(ModItems.TESSERACT.get());
        caller.setItemInHand(InteractionHand.MAIN_HAND, held);
        caller.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        held.getItem().use(level, caller, InteractionHand.MAIN_HAND);
        check(caller.isUsingItem() && caller.getUseItem() == held && Powers.power(caller) == Power.NONE, "a summoning that begins holds the Tesseract");
        caller.stopUsingItem();
        ItemStack stone = new ItemStack(ModItems.LOKIS_SCEPTER.get());
        caller.setItemInHand(InteractionHand.MAIN_HAND, stone);
        caller.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TESSERACT.get()));
        stone.getItem().use(level, caller, InteractionHand.MAIN_HAND);
        check(caller.isUsingItem() && caller.isAlive(), "and the Scepter");

        // Without a summoning the Tesseract's holder is not asked anything and keeps the old behaviour.
        ScepterRituals.registerThanosHold(null);
        check(!ScepterRituals.callsThanos(traveller, InteractionHand.MAIN_HAND), "with no summoning built, the call is nobody's");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The awakening items

    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
    public static void anAwakeningItemTurnsAHeroAwayBeforeTheHoldAndCreativeKeepsIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer hero = f.player(5.5, FLOOR, 5.5, Power.THOR);
        ItemStack ember = new ItemStack(ModItems.PHOENIX_EMBER.get());
        hero.setItemInHand(InteractionHand.MAIN_HAND, ember);
        InteractionResultHolder<ItemStack> used = ember.getItem().use(level, hero, InteractionHand.MAIN_HAND);
        check(used.getResult().consumesAction() && !hero.isUsingItem(), "no hold begins for a hero (the click is spent on the refusal, not passed to the other hand)");
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

    @GameTest(template = ARENA, batch = SKELETON)
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
    @GameTest(template = ARENA, batch = SKELETON)
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

    @GameTest(template = ARENA, batch = SKELETON)
    public static void theBossFacadeAnswersAsIfThereWereNoBossYet(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 8.5));
        check(BossFights.active(level) == null && !BossFights.anyActive(level.getServer()), "no fight runs");
        check(!BossFights.isMember(player) && !BossFights.isMinion(zombie) && !BossFights.isHexExempt(player), "nobody is in one");
        check(BossFights.allowsTeleport(level, player, new Vec3(1, 2, 3)), "a teleport is allowed anywhere");
        check(!BossFights.protects(level, helper.absolutePos(new BlockPos(5, 2, 5))), "no block is protected");
        check(!BossFights.telekinesisPress(player) && !BossFights.telekinesisLaunch(player, Telekinesis.Mode.WANDA, zombie), "Wanda's telekinesis is hers alone");
        // 3.1 (D-T 14.1): telekinesis is not held back by any fight.
        BlockPos cell = helper.absolutePos(new BlockPos(5, 2, 5));
        Vec3 far = zombie.position().add(100.0, -40.0, 100.0);
        check(!BossFights.isLocked(player) && !BossFights.isLocked(zombie), "nothing is locked where it is");
        check(!BossFights.hidesGround(level, cell), "no ground is hidden");
        check(!BossFights.isCopyCell(level, cell) && BossFights.lendRefusal(player, cell) == null && BossFights.lend(player, cell,
                level.getBlockState(cell)) == null, "no cell is an island's copy, and no arena lends (or refuses) a block");
        check(BossFights.carryLimit(zombie, far) == far && BossFights.edgeStop(zombie, far) == null && !BossFights.bringBack(zombie),
                "a zombie is carried and thrown anywhere");
        check(BossGrip.regripLeft(zombie) == 0 && !BossGrip.stills(zombie) && BossGrip.refusal(player, zombie, Telekinesis.Mode.WANDA) == null,
                "and no boss rule holds it back");
        for (BossKind kind : BossKind.values()) {
            check(BossKind.byId(kind.id()) == kind, kind + " is found by its id");
            if (BossFights.lane(kind) != null) {
                continue; // a boss whose lane is built answers to the tests of its own lane
            }
            check(!BossFights.start(kind, player, true), "no " + kind + " fight can start yet, not even by force");
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
        check(BossFights.lane(BossKind.ULTRON) != null || run(player, "enhanced boss start ultron force") == 0,
                "even by force, until a lane registers its fight");
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
        // (An entity of a boss that no fight owns never joins a level, so a fight of the test owns these.)
        BossTestSupport.TestFight fight = BossFrameworkTests.start(f, BossKind.ULTRON, player,
                BossFrameworkTests.fight(f, BossKind.ULTRON, BossArena.Containment.NONE, false));
        fight.spawn(thanos);
        fight.spawn(sentry);
        fight.spawn(far);
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

    @GameTest(template = ARENA, batch = SKELETON)
    public static void theProtocolIsSeven(GameTestHelper helper) {
        check(ModNetwork.PROTOCOL_VERSION.equals("7"), "3.3 bumps the protocol from 6 to 7, once: " + ModNetwork.PROTOCOL_VERSION);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = SKELETON)
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

    /**
     * The six messages of 3.1 give back what they were given, and each stays within its bound (R57): a cast with 8 seeds
     * 140 bytes, a verdict with 64 marks and every number at its largest 620, a spell's effect 64, every part of a pocket
     * 31 KiB (a 24 x 24 x 24 pocket of 20 kinds of block in at most two parts; a pocket of 3,000 different states, and
     * one whose palette is every state there is, read back whole), a pull of 64 streams 2 KiB.
     */
    @GameTest(template = ARENA, batch = SKELETON)
    public static void everyPayloadOf31RoundTrips(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        BlockPos far = new BlockPos(-29_999_999, -64, 29_999_999);
        BlockPos farther = new BlockPos(29_999_999, 319, -29_999_999);

        List<BlockPos> seeds = new ArrayList<>();
        for (int i = 0; i < SpellRecipe.MAX_SEEDS; i++) {
            seeds.add(far.offset(i, i, -i));
        }
        SpellRecipe recipe = new SpellRecipe(Spell.SHAPE, SpellRecipe.Op.DAB, ShapeKind.HEART.ordinal(), SpellRecipe.MASK, far, farther,
                seeds, farther, Direction.SOUTH.get3DDataValue(), Integer.MIN_VALUE, Integer.MIN_VALUE, 3, SpellRecipe.UPSIDE_DOWN,
                SpellRecipe.HOLLOW | SpellRecipe.MIX | SpellRecipe.PLANTED, SpellRecipe.PICKED, Long.MIN_VALUE, Integer.MIN_VALUE);
        BuildCastPayload cast = new BuildCastPayload(Integer.MIN_VALUE, BuildCastPayload.PREFLIGHT | BuildCastPayload.HELD_SHORT
                | BuildCastPayload.HELD_LONG, recipe);
        check(roundTrip(BuildCastPayload.STREAM_CODEC, cast, access).equals(cast), "the cast");
        check(encodedSize(BuildCastPayload.STREAM_CODEC, cast, access) <= 140,
                "a cast with 8 seeds is " + encodedSize(BuildCastPayload.STREAM_CODEC, cast, access) + " bytes");
        for (Spell spell : Spell.values()) {
            SpellRecipe plain = new SpellRecipe(spell, SpellRecipe.Op.CAST, 0, SpellRecipe.BOX, BlockPos.ZERO, new BlockPos(4, 3, 4), List.of(),
                    BlockPos.ZERO, SpellRecipe.NO_FACE, 2, 1, 0, SpellRecipe.NO_FLIP, 0, SpellRecipe.HAND, 42L, 0);
            check(roundTrip(SpellRecipe.STREAM_CODEC, plain, access).equals(plain), "a recipe of " + spell);
        }

        BuildActionPayload action = new BuildActionPayload(BuildActionPayload.VIEW, far, farther, Integer.MIN_VALUE);
        check(roundTrip(BuildActionPayload.STREAM_CODEC, action, access).equals(action), "an action");
        check(encodedSize(BuildActionPayload.STREAM_CODEC, action, access) <= 24,
                "an action is " + encodedSize(BuildActionPayload.STREAM_CODEC, action, access) + " bytes");

        long[] marks = new long[BuildVerdictPayload.MAX_MARKS];
        byte[] kinds = new byte[BuildVerdictPayload.MAX_MARKS];
        for (int i = 0; i < marks.length; i++) {
            marks[i] = far.offset(i, i, i).asLong();
            kinds[i] = (byte) (i % (BuildVerdictPayload.MARK_BEYOND + 1));
        }
        int all = BuildVerdictPayload.CUT_OFF | BuildVerdictPayload.PROTECTED | BuildVerdictPayload.KEEPS | BuildVerdictPayload.BODIES
                | BuildVerdictPayload.UNLOADED;
        BuildVerdictPayload verdict = new BuildVerdictPayload(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, all, marks, kinds);
        BuildVerdictPayload verdictBack = roundTrip(BuildVerdictPayload.STREAM_CODEC, verdict, access);
        check(verdictBack.equals(verdict) && verdictBack.mark(63).equals(far.offset(63, 63, 63)), "the verdict");
        check(encodedSize(BuildVerdictPayload.STREAM_CODEC, verdict, access) <= 620,
                "the biggest verdict is " + encodedSize(BuildVerdictPayload.STREAM_CODEC, verdict, access) + " bytes");
        BuildVerdictPayload refusal = new BuildVerdictPayload(3, BuildVerdictPayload.TOO_FAR, 0, 0, 24, 0);
        check(roundTrip(BuildVerdictPayload.STREAM_CODEC, refusal, access).equals(refusal) && BuildVerdictPayload.isRefusal(refusal.code())
                && !BuildVerdictPayload.isRefusal(BuildVerdictPayload.NOTHING_CHANGES), "a refusal without marks");

        BuildFxPayload fx = new BuildFxPayload(BuildFxPayload.RANK_UP, Integer.MAX_VALUE, Integer.MAX_VALUE, BuildFxPayload.BILLY, far, farther,
                far, Integer.MAX_VALUE, Integer.MAX_VALUE, BuildFxPayload.NO_MOM, BuildFxPayload.COLUMNS);
        check(roundTrip(BuildFxPayload.STREAM_CODEC, fx, access).equals(fx), "a spell's effect");
        check(encodedSize(BuildFxPayload.STREAM_CODEC, fx, access) <= 64,
                "a spell's effect is " + encodedSize(BuildFxPayload.STREAM_CODEC, fx, access) + " bytes");

        // A 24 x 24 x 24 copy of 20 kinds of block (and holes), every cell another block than the one before it.
        List<BlockState> twenty = BuiltInRegistries.BLOCK.stream().filter(block -> !block.defaultBlockState().isAir()).limit(20)
                .map(Block::defaultBlockState).toList();
        int[] cells = new int[24 * 24 * 24];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = i % 21;
        }
        List<BuildPocketPayload> house = pocketRoundTrip(access, 7, 24, twenty, cells);
        check(house.size() <= 2, "a 24^3 pocket of 20 kinds of block takes " + house.size() + " parts");
        // 3,000 different states, a different one in every cell: the runs go over several parts.
        List<BlockState> states = new ArrayList<>();
        for (int id = Block.BLOCK_STATE_REGISTRY.size() - 3000; id < Block.BLOCK_STATE_REGISTRY.size(); id++) {
            states.add(Block.stateById(id));
        }
        for (int i = 0; i < cells.length; i++) {
            cells[i] = 1 + i % states.size();
        }
        check(pocketRoundTrip(access, 8, 24, states, cells).size() >= 2, "3,000 states in every cell take more than one part");
        // Every state there is: the palette itself is split over parts.
        List<BlockState> every = new ArrayList<>();
        Block.BLOCK_STATE_REGISTRY.forEach(every::add);
        int[] few = new int[4 * 4 * 4];
        for (int i = 0; i < few.length; i++) {
            few[i] = 1 + i * (every.size() / few.length);
        }
        check(pocketRoundTrip(access, 9, 4, every, few).size() >= 3, "a palette of every state takes several parts");

        List<MagnetPullPayload.Stream> streams = new ArrayList<>();
        List<Item> metal = List.of(Items.IRON_INGOT, Items.RAW_COPPER, Items.RAIL);
        for (int i = 0; i < MagnetPullPayload.MAX_STREAMS; i++) {
            streams.add(new MagnetPullPayload.Stream(new Vec3(-29_999_999.5 + i, 319.5, 29_999_999.5 - i), metal.get(i % 3), 1 + i % 3));
        }
        for (boolean first : new boolean[]{true, false}) {
            MagnetPullPayload pull = new MagnetPullPayload(Integer.MAX_VALUE, first, 12.5F, streams);
            check(roundTrip(MagnetPullPayload.STREAM_CODEC, pull, access).equals(pull), "a pull, first " + first);
            check(encodedSize(MagnetPullPayload.STREAM_CODEC, pull, access) <= 2048,
                    "a pull of 64 streams is " + encodedSize(MagnetPullPayload.STREAM_CODEC, pull, access) + " bytes");
        }
        MagnetPullPayload start = new MagnetPullPayload(5, true, 8.0F, List.of());
        check(roundTrip(MagnetPullPayload.STREAM_CODEC, start, access).equals(start), "a pull's start, nothing taken yet");
        helper.succeed();
    }

    /**
     * A pocket of size {@code side} on each axis, with these cells (0: no block, i: {@code palette.get(i - 1)}), sent in
     * parts: every part comes back equal and within 31 KiB, and the parts put together, in any order, give back the
     * palette and every cell. Returns the parts.
     */
    private static List<BuildPocketPayload> pocketRoundTrip(RegistryAccess access, int id, int side, List<BlockState> palette, int[] cells) {
        ByteBuf out = Unpooled.buffer();
        for (int i = 0; i < cells.length; ) {
            int run = 1;
            while (i + run < cells.length && cells[i + run] == cells[i]) {
                run++;
            }
            BuildPocketPayload.writeRun(out, cells[i], run);
            i += run;
        }
        byte[] runs = ByteBufUtil.getBytes(out);
        List<BuildPocketPayload> parts = BuildPocketPayload.split(id, side, side, side, palette, runs);
        for (BuildPocketPayload part : parts) {
            check(roundTrip(BuildPocketPayload.STREAM_CODEC, part, access).equals(part), "pocket " + id + ", " + part);
            int bytes = encodedSize(BuildPocketPayload.STREAM_CODEC, part, access);
            check(bytes <= 31 * 1024, "pocket " + id + ": part " + part.part() + " is " + bytes + " bytes");
        }
        List<BuildPocketPayload> reversed = new ArrayList<>(parts);
        Collections.reverse(reversed);
        BuildPocketPayload.Whole whole = BuildPocketPayload.join(reversed);
        check(whole != null && whole.pocket() == id && whole.sx() == side && whole.sz() == side && whole.palette().equals(palette)
                && Arrays.equals(whole.runs(), runs), "pocket " + id + " is put together again, its palette whole");
        int[] back = new int[cells.length];
        check(BuildPocketPayload.readRuns(whole.runs(), cells.length, palette.size(),
                (first, length, index) -> Arrays.fill(back, (int) first, (int) first + length, index)), "pocket " + id + ": the runs read");
        check(Arrays.equals(back, cells), "pocket " + id + ": every cell comes back");
        if (parts.size() > 1) {
            check(BuildPocketPayload.join(parts.subList(1, parts.size())) == null, "pocket " + id + ": with a part missing, no pocket yet");
        }
        return parts;
    }

    private static <T> int encodedSize(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T value, RegistryAccess access) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        codec.encode(buf, value);
        return buf.readableBytes();
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

    /**
     * The release gate for the sentinel lines of the lang files ({@code "_lane.phoenix": ""} and so on): they were the
     * scaffolding that kept the lanes' blocks apart while 3.0 was built in parallel, and are gone now that the lanes are
     * merged.
     */
    @GameTest(template = ARENA, batch = SKELETON)
    public static void noLangSentinelsRemain(GameTestHelper helper) throws IOException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        check(sentinels(lang(mod, "en_us")).isEmpty() && sentinels(lang(mod, "he_il")).isEmpty(),
                "the lanes' sentinel lines are still in the lang files");
        helper.succeed();
    }

    private static List<String> sentinels(JsonObject lang) {
        return lang.keySet().stream().filter(key -> key.startsWith("_")).toList();
    }

    /**
     * The release gate for the animation tables: every ability, toggle and state of the five heroes is acted by a
     * gesture of its own or explained, not pending. Passes while the version is older than 3.0.0.
     */
    @GameTest(template = ARENA, batch = SKELETON)
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
    @GameTest(template = ARENA, batch = SKELETON)
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
