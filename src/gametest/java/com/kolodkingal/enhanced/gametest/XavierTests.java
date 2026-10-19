package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.ability.HeroLifecycle;
import com.kolodkingal.enhanced.ability.MindLink;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.Telepathy;
import com.kolodkingal.enhanced.ability.Thoughts;
import com.kolodkingal.enhanced.ability.XavierAbilities;
import com.kolodkingal.enhanced.config.XavierConfig;
import com.kolodkingal.enhanced.item.XavierWheelchairItem;
import com.kolodkingal.enhanced.network.CerebroPayload;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.network.ThoughtsPayload;
import com.kolodkingal.enhanced.network.XavierChoicePayload;
import com.kolodkingal.enhanced.network.XavierInputPayload;
import com.kolodkingal.enhanced.network.XavierPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.power.Variants;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.PhoenixTests.actionBarKeys;
import static com.kolodkingal.enhanced.gametest.PhoenixTests.payloads;
import static com.kolodkingal.enhanced.gametest.PhoenixTests.press;
import static com.kolodkingal.enhanced.gametest.PhoenixTests.rested;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * Professor X (3.3, lane XAVIER): the telepathy rule book, the wheelchair and his two versions, the Mental Blast, Hear
 * Thoughts (with Billy's The Noise), Take Over a Body and every way it ends, Freeze Minds, Cerebro, the network.
 *
 * <p>Batches: {@code xavier} (PvP off, nothing server-wide), {@code xavier_pvp} (every test there switches PvP on), and a
 * {@code xavier_solo_*} batch for each test that changes a config value or ends every link.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class XavierTests {
    static final String QUICK = "xavier";
    static final String PVP = "xavier_pvp";

    static ServerPlayer xavier(TestKit.Fixture f, double x, double z) {
        return f.player(x, FLOOR, z, Power.PROFESSOR_X);
    }

    /** A husk with its own will (it walks when steered and does not burn in the sun). */
    static Husk husk(GameTestHelper helper, double x, double z) {
        Husk husk = helper.spawn(EntityType.HUSK, new Vec3(x, FLOOR, z));
        husk.setPersistenceRequired();
        return husk;
    }

    /** Whether a creature's will to move is off (the goal flag {@code MindLink} disables while he steers it). */
    @SuppressWarnings("unchecked")
    static boolean willOff(net.minecraft.world.entity.Mob mob) {
        try {
            java.lang.reflect.Field field = net.minecraft.world.entity.ai.goal.GoalSelector.class.getDeclaredField("disabledFlags");
            field.setAccessible(true);
            return ((java.util.Set<Goal.Flag>) field.get(mob.goalSelector)).contains(Goal.Flag.MOVE);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Ends his link when the test ends, pass or fail (the links are the server's, not the test's). */
    static void releaseOnClose(TestKit.Fixture f, ServerPlayer xavier) {
        f.onClose(() -> MindLink.end(xavier.server, xavier.getUUID(), MindLink.End.SERVER));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The rule book

    @GameTest(template = ARENA, batch = QUICK)
    public static void theTelepathyRanksAreEilonsList(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        check(Telepathy.rank(Power.PROFESSOR_X) == Telepathy.Rank.TELEPATH, "Professor X is a telepath");
        for (Power p : new Power[]{Power.SCARLET_WITCH, Power.PHOENIX, Power.BILLY}) {
            check(Telepathy.rank(p) == Telepathy.Rank.STRONGER, p + " is a stronger telepath");
        }
        for (Power p : Power.values()) {
            if (p != Power.PROFESSOR_X && p != Power.SCARLET_WITCH && p != Power.PHOENIX && p != Power.BILLY) {
                check(Telepathy.rank(p) == Telepathy.Rank.NONE, p + " has no telepathy (Vision neither)");
            }
        }
        ServerPlayer charles = xavier(f, 3.5, 3.5);
        ServerPlayer other = xavier(f, 3.5, 6.5);
        ServerPlayer wanda = f.player(3.5, FLOOR, 9.5, Power.SCARLET_WITCH);
        ServerPlayer billy = f.player(3.5, FLOOR, 12.5, Power.BILLY);
        ServerPlayer vision = f.player(3.5, FLOOR, 15.5, Power.VISION);
        ServerPlayer plain = f.player(6.5, FLOOR, 3.5);
        Husk husk = husk(helper, 9.5, 9.5);
        check(Telepathy.reach(charles, plain) == Telepathy.Verdict.OPEN, "a player without powers is open to him");
        check(Telepathy.reach(charles, vision) == Telepathy.Verdict.OPEN, "and so is Vision");
        check(Telepathy.reach(charles, husk) == Telepathy.Verdict.OPEN, "and a creature");
        check(Telepathy.reach(charles, other) == Telepathy.Verdict.TELEPATH, "another Professor X keeps him out");
        check(Telepathy.reach(charles, wanda) == Telepathy.Verdict.THROWS_OUT, "the Scarlet Witch throws him out");
        check(Telepathy.reach(charles, billy) == Telepathy.Verdict.THROWS_OUT, "Billy throws him out");
        check(Telepathy.reach(billy, charles) == Telepathy.Verdict.OPEN, "Billy, stronger, hears him");
        check(Telepathy.reach(billy, wanda) == Telepathy.Verdict.TELEPATH, "but not his mother");
        check(Telepathy.isStronger(wanda, charles) && !Telepathy.isStronger(charles, wanda), "who is stronger");
        plain.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        check(Telepathy.reach(charles, plain) == Telepathy.Verdict.SHIELDED && Telepathy.isShielded(plain), "Magneto's helmet shuts a mind");
        var warden = helper.spawnWithNoFreeWill(EntityType.WARDEN, new Vec3(14.5, FLOOR, 14.5));
        check(Telepathy.reach(charles, warden) == Telepathy.Verdict.SHIELDED, "and a boss's mind is too vast");
        warden.discard();
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The wheelchair and the two versions

    @GameTest(template = ARENA, batch = QUICK)
    public static void theWheelchairAwakensHimAndHeChoosesHisVersion(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerLevel level = helper.getLevel();
        ServerPlayer player = f.player(5.5, FLOOR, 5.5);
        XavierWheelchairItem chair = ModItems.XAVIER_WHEELCHAIR.get();
        ItemStack stack = new ItemStack(chair);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        check(chair.power() == Power.PROFESSOR_X && chair.holdTicks() == 60, "a three-second hold makes Professor X");
        chair.use(level, player, InteractionHand.MAIN_HAND);
        TestKit.sentPackets(player);
        chair.finishUsingItem(stack, level, player);
        check(Powers.power(player) == Power.PROFESSOR_X, "he is Professor X");
        check(stack.isEmpty(), "and the chair is his (used up)");
        check(Variants.current(Powers.get(player)).id().equals(XavierAbilities.YOUNG), "young until he chooses");
        List<XavierPayload> sent = payloads(TestKit.sentPackets(player), XavierPayload.class);
        check(sent.stream().anyMatch(p -> p.kind() == XavierPayload.CHOOSE), "the screen of his versions opens");

        XavierAbilities.choose(player, XavierAbilities.OLD);
        check(Variants.current(Powers.get(player)).id().equals(XavierAbilities.OLD), "he chose the old professor");
        check(XavierAbilities.seated(player), "who sits in his wheelchair");
        XavierAbilities.choose(player, "thor");
        check(Variants.current(Powers.get(player)).id().equals(XavierAbilities.OLD), "a version he has not got changes nothing");
        XavierAbilities.choose(player, XavierAbilities.YOUNG);
        check(!XavierAbilities.seated(player), "the young one walks");

        ServerPlayer hero = f.player(9.5, FLOOR, 5.5, Power.THOR);
        XavierAbilities.choose(hero, XavierAbilities.OLD);
        check(Powers.power(hero) == Power.THOR && !Variants.current(Powers.get(hero)).id().equals(XavierAbilities.OLD),
                "nobody else can choose it");
        ItemStack other = new ItemStack(chair);
        hero.setItemInHand(InteractionHand.MAIN_HAND, other);
        chair.use(level, hero, InteractionHand.MAIN_HAND);
        check(!hero.isUsingItem() && other.getCount() == 1, "a hero is turned away with nothing used up");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void theWheelchairIsCrafted(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(Enhanced.id("xavier_wheelchair"));
        check(holder.isPresent() && holder.get().value() instanceof ShapedRecipe, "a shaped recipe");
        ShapedRecipe recipe = (ShapedRecipe) holder.get().value();
        check(recipe.getResultItem(level.registryAccess()).is(ModItems.XAVIER_WHEELCHAIR.get()), "that makes the wheelchair");
        ItemStack e = ItemStack.EMPTY;
        ItemStack l = new ItemStack(Items.LEATHER);
        ItemStack i = new ItemStack(Items.IRON_INGOT);
        ItemStack eye = new ItemStack(Items.ENDER_EYE);
        check(recipe.matches(CraftingInput.of(3, 3, List.of(l, e, e, l, l, eye, i, e, i)), level), "leather, an Eye of Ender and iron");
        check(!recipe.matches(CraftingInput.of(3, 3, List.of(l, e, e, l, l, l, i, e, i)), level), "the Eye of Ender is needed");
        check(level.getServer().getAdvancements().get(Enhanced.id("recipes/misc/xavier_wheelchair")) != null, "its recipe advancement exists");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: the Mental Blast

    @GameTest(template = ARENA, batch = QUICK)
    public static void theMentalBlastHurtsAnOpenMindOnly(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = xavier(f, 4.5, 10.5);
        PowerData data = Powers.get(charles);
        Zombie husk = PhoenixTests.zombie(helper, 9.5, 10.5);
        float before = husk.getHealth();
        lookAt(charles, husk);
        press(charles, 1);
        check(husk.getHealth() <= before - 5.9F, "6 damage: " + (before - husk.getHealth()));
        check(husk.hasEffect(MobEffects.CONFUSION) && husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "dizzy and slowed");
        check(!data.isReady(1, helper.getLevel().getGameTime()) && data.is(Live.XAVIER_FOCUS), "R rests; the fingers at his temple");
        check(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageTypes.MIND_BLAST)
                .is(DamageTypeTags.BYPASSES_ARMOR), "the blast ignores armour");

        rested(charles, 1);
        husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        float shielded = husk.getHealth();
        TestKit.sentPackets(charles);
        press(charles, 1);
        check(husk.getHealth() == shielded && actionBarKeys(charles).contains("message.enhanced.xavier.shielded"), "Magneto's helmet stops it");
        check(data.isReady(1, helper.getLevel().getGameTime()), "and it costs nothing");

        ServerPlayer plain = f.player(9.5, FLOOR, 14.5);
        lookAt(charles, plain);
        press(charles, 1);
        check(plain.getHealth() == plain.getMaxHealth() && actionBarKeys(charles).contains("message.enhanced.xavier.no_pvp"),
                "a player only with PvP on");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PVP)
    public static void theMentalBlastHurtsAPlayerWithPvp(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer charles = xavier(f, 4.5, 10.5);
        ServerPlayer plain = f.player(9.5, FLOOR, 10.5);
        ServerPlayer billy = f.player(9.5, FLOOR, 14.5, Power.BILLY);
        lookAt(charles, plain);
        press(charles, 1);
        check(plain.getHealth() < plain.getMaxHealth(), "PvP on: it hurts a player");
        rested(charles, 1);
        lookAt(charles, billy);
        TestKit.sentPackets(charles);
        press(charles, 1);
        check(billy.getHealth() == billy.getMaxHealth() && actionBarKeys(charles).contains("message.enhanced.xavier.telepath"),
                "but never a telepath's mind");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // G: Hear Thoughts

    @GameTest(template = ARENA, batch = QUICK)
    public static void hearThoughtsSendsTheThoughtsAround(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = xavier(f, 4.5, 4.5);
        PowerData data = Powers.get(charles);
        press(charles, 2);
        check(data.toggle(Toggle.HEAR_THOUGHTS) && Thoughts.listens(charles, data), "G: he hears thoughts");
        Zombie husk = PhoenixTests.zombie(helper, 8.5, 4.5);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(8.5, FLOOR, 8.5));
        ServerPlayer wanda = f.player(4.5, FLOOR, 8.5, Power.SCARLET_WITCH);
        ServerPlayer hurt = f.player(12.5, FLOOR, 4.5);
        hurt.setHealth(4.0F);
        husk.setTarget(hurt);
        List<ThoughtsPayload.Thought> heard = Thoughts.around(charles, Power.PROFESSOR_X);
        check(key(heard, husk.getId()).equals("thought.enhanced.hunting.0") || key(heard, husk.getId()).equals("thought.enhanced.hunting.1"),
                "the husk thinks of its prey: " + key(heard, husk.getId()));
        check(key(heard, pig.getId()).startsWith("thought.enhanced.pig."), "the pig thinks a pig's thought");
        check(key(heard, hurt.getId()).startsWith("thought.enhanced.player_hurt."), "a hurt player thinks of their wounds");
        check(key(heard, wanda.getId()).equals("thought.enhanced.closed.0") && mood(heard, wanda.getId()) == ThoughtsPayload.CLOSED,
                "the Scarlet Witch's mind is closed to him");
        boolean sent = false;
        for (int i = 0; i < Thoughts.PERIOD; i++) {
            charles.tickCount++;
            PowerTicker.tick(charles);
            sent |= !payloads(TestKit.sentPackets(charles), ThoughtsPayload.class).isEmpty();
        }
        check(sent, "every two seconds the thoughts are sent to him");

        // Billy's The Noise is the same system: his thoughts are heard too, and Professor X's mind is open to him.
        ServerPlayer billy = f.player(12.5, FLOOR, 12.5, Power.BILLY);
        Powers.get(billy).setToggle(Toggle.THE_NOISE, true);
        check(Thoughts.listens(billy, Powers.get(billy)), "Billy with The Noise on hears them");
        List<ThoughtsPayload.Thought> billys = Thoughts.around(billy, Power.BILLY);
        check(mood(billys, charles.getId()) != ThoughtsPayload.CLOSED, "Billy, stronger, hears Professor X");
        press(charles, 2);
        check(!data.toggle(Toggle.HEAR_THOUGHTS) && !Thoughts.listens(charles, data), "G again: quiet");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void everyThoughtHasItsWordsInBothLanguages(GameTestHelper helper) throws IOException {
        var mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        for (String lang : List.of("en_us", "he_il")) {
            JsonObject json;
            try (Reader r = Files.newBufferedReader(mod.findResource("assets", "enhanced", "lang", lang + ".json"), StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(r).getAsJsonObject();
            }
            for (Map.Entry<String, Integer> kind : Thoughts.KINDS.entrySet()) {
                for (int i = 0; i < kind.getValue(); i++) {
                    check(json.has("thought.enhanced." + kind.getKey() + "." + i), lang + " has thought " + kind.getKey() + "." + i);
                }
                check(!json.has("thought.enhanced." + kind.getKey() + "." + kind.getValue()), lang + ": no unused line for " + kind.getKey());
            }
        }
        helper.succeed();
    }

    private static String key(List<ThoughtsPayload.Thought> thoughts, int id) {
        for (ThoughtsPayload.Thought t : thoughts) {
            if (t.entityId() == id && t.text().getContents() instanceof TranslatableContents tc) {
                return tc.getKey();
            }
        }
        return "";
    }

    private static int mood(List<ThoughtsPayload.Thought> thoughts, int id) {
        return thoughts.stream().filter(t -> t.entityId() == id).findFirst().map(ThoughtsPayload.Thought::mood).orElse(-1);
    }

    // ------------------------------------------------------------------------------------------------------------
    // V: Take Over a Body (creatures)

    @GameTest(template = ARENA, batch = "xavier_link", timeoutTicks = 80)
    public static void heTakesACreatureAndSteersIt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = xavier(f, 3.5, 10.5);
        releaseOnClose(f, charles);
        PowerData data = Powers.get(charles);
        Husk husk = husk(helper, 6.5, 10.5);
        lookAt(charles, husk);
        TestKit.sentPackets(charles);
        press(charles, 3);
        check(MindLink.isHost(husk) && MindLink.isLinked(charles) && data.is(Live.MIND_LINKED), "V: he is in the husk");
        List<XavierPayload> sent = payloads(TestKit.sentPackets(charles), XavierPayload.class);
        check(sent.stream().anyMatch(p -> p.kind() == XavierPayload.LINK_START && p.entityId() == husk.getId() && p.ticks() == 600),
                "his client is told whose eyes to look through, and for 30 seconds");
        check(willOff(husk), "the husk's own will is off");
        double startX = husk.getX();
        // He walks it east (+x): yaw -90 looks along +x.
        f.everyTick(60, tick -> {
            MindLink.input(charles, new XavierInputPayload(1.0F, 0.0F, false, false, TestKit.EAST, 0.0F, false));
            PowerTicker.tick(charles);
            if (tick < 40) {
                return false;
            }
            check(husk.getX() > startX + 2.0, "the husk walked where he told it: " + (husk.getX() - startX));
            rested(charles, 3);
            press(charles, 3);
            check(!MindLink.isHost(husk) && !MindLink.isLinked(charles) && !data.is(Live.MIND_LINKED), "V again: he is back");
            check(husk.hasEffect(ModEffects.CONFUSED), "the husk wakes confused");
            check(!willOff(husk), "with its will");
            check(!data.isReady(3, helper.getLevel().getGameTime()), "and V rests");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "xavier_link")
    public static void aSteeredCreatureStrikesWhatHeLooksAt(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = xavier(f, 3.5, 4.5);
        releaseOnClose(f, charles);
        Husk husk = husk(helper, 6.5, 4.5);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(8.0, FLOOR, 4.5));
        lookAt(charles, husk);
        press(charles, 3);
        check(MindLink.isHost(husk), "in the husk");
        float before = villager.getHealth();
        MindLink.input(charles, new XavierInputPayload(0.0F, 0.0F, false, false, TestKit.EAST, 10.0F, true));
        PowerTicker.tick(charles);
        check(villager.getHealth() < before, "a click: the husk strikes the villager in front of it");
        float after = villager.getHealth();
        MindLink.input(charles, new XavierInputPayload(0.0F, 0.0F, false, false, TestKit.EAST, 10.0F, true));
        PowerTicker.tick(charles);
        check(villager.getHealth() == after, "and not again before half a second");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "xavier_link")
    public static void aLinkEndsWhenTheBodyDiesOrHeIsHurtOrGone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        // The body dies.
        ServerPlayer a = xavier(f, 2.5, 2.5);
        releaseOnClose(f, a);
        Husk ha = husk(helper, 5.5, 2.5);
        lookAt(a, ha);
        press(a, 3);
        check(MindLink.isHost(ha), "a is in a husk");
        ha.kill();
        check(!MindLink.isLinked(a) && !Powers.get(a).is(Live.MIND_LINKED), "its death brings him back");

        // His own body is hurt.
        ServerPlayer b = xavier(f, 2.5, 7.5);
        releaseOnClose(f, b);
        Husk hb = husk(helper, 5.5, 7.5);
        lookAt(b, hb);
        press(b, 3);
        check(MindLink.isHost(hb), "b is in a husk");
        b.hurt(b.damageSources().generic(), 1.0F);
        check(!MindLink.isLinked(b) && !MindLink.isHost(hb), "a blow to his own body brings him back");

        // He dies, logs out or changes dimension.
        ServerPlayer c = xavier(f, 2.5, 12.5);
        releaseOnClose(f, c);
        Husk hc = husk(helper, 5.5, 12.5);
        lookAt(c, hc);
        press(c, 3);
        HeroLifecycle.onChangeDimension(c);
        check(!MindLink.isLinked(c) && !MindLink.isHost(hc), "a change of dimension ends it");

        ServerPlayer d = xavier(f, 2.5, 17.5);
        Husk hd = husk(helper, 5.5, 17.5);
        lookAt(d, hd);
        press(d, 3);
        check(MindLink.isHost(hd), "d is in a husk");
        helper.getLevel().getServer().getPlayerList().remove(d);
        check(!MindLink.isHost(hd) && !willOff(hd), "his logout gives the husk back its will");

        // He loses the power.
        ServerPlayer e = xavier(f, 12.5, 2.5);
        releaseOnClose(f, e);
        Husk he = husk(helper, 15.5, 2.5);
        lookAt(e, he);
        press(e, 3);
        Powers.setPower(e, Power.NONE);
        check(!MindLink.isLinked(e) && !MindLink.isHost(he) && !Powers.get(e).is(Live.MIND_LINKED), "losing the power ends it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "xavier_link", timeoutTicks = 60)
    public static void aLinkEndsWhenTheBodyGoesTooFar(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = xavier(f, 3.5, 10.5);
        releaseOnClose(f, charles);
        Husk husk = husk(helper, 6.5, 10.5);
        lookAt(charles, husk);
        press(charles, 3);
        check(MindLink.isHost(husk), "in the husk");
        Vec3 far = charles.position().add(XavierConfig.LINK_MAX_DISTANCE.get() + 12.0, 0.0, 0.0);
        charles.teleportTo(far.x, far.y, far.z);
        PowerTicker.tick(charles);
        check(!MindLink.isLinked(charles) && !MindLink.isHost(husk), "too far: the link snaps");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "xavier_solo_link_time", timeoutTicks = 80)
    public static void aLinkEndsWithItsTime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(XavierConfig.LINK_CREATURE_TICKS, 20);
        ServerPlayer charles = xavier(f, 3.5, 10.5);
        releaseOnClose(f, charles);
        Husk husk = husk(helper, 6.5, 10.5);
        lookAt(charles, husk);
        press(charles, 3);
        check(MindLink.isHost(husk), "in the husk");
        f.everyTick(40, tick -> {
            PowerTicker.tick(charles);
            if (tick < 25) {
                return false;
            }
            check(!MindLink.isLinked(charles) && !MindLink.isHost(husk), "the link ends with its time");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = "xavier_solo_server_stop")
    public static void aStoppingServerEndsEveryLink(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = xavier(f, 3.5, 10.5);
        Husk husk = husk(helper, 6.5, 10.5);
        lookAt(charles, husk);
        press(charles, 3);
        check(MindLink.isHost(husk), "in the husk");
        MindLink.endAll(helper.getLevel().getServer());
        check(MindLink.count() == 0 && !MindLink.isHost(husk) && !willOff(husk),
                "a stopping server gives every body back its will");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void someMindsCannotBeTaken(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = xavier(f, 3.5, 3.5);
        releaseOnClose(f, charles);
        ServerPlayer other = xavier(f, 7.5, 3.5);
        lookAt(charles, other);
        TestKit.sentPackets(charles);
        press(charles, 3);
        check(!MindLink.isLinked(charles) && actionBarKeys(charles).contains("message.enhanced.xavier.telepath"), "another telepath keeps him out");
        check(charles.getHealth() == charles.getMaxHealth(), "and it costs him nothing");

        ServerPlayer plain = f.player(3.5, FLOOR, 8.5);
        lookAt(charles, plain);
        press(charles, 3);
        check(!MindLink.isLinked(charles) && actionBarKeys(charles).contains("message.enhanced.xavier.no_pvp"), "a player only with PvP on");

        Zombie husk = PhoenixTests.zombie(helper, 9.5, 13.5);
        husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        lookAt(charles, husk);
        press(charles, 3);
        check(!MindLink.isLinked(charles) && actionBarKeys(charles).contains("message.enhanced.xavier.shielded"), "nor under Magneto's helmet");
        check(Powers.get(charles).isReady(3, helper.getLevel().getGameTime()), "a refusal costs nothing");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // V: Take Over a Body (players), and the stronger telepaths

    @GameTest(template = ARENA, batch = PVP)
    public static void heSteersAPlayerWhoNeverStaysStuck(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer charles = xavier(f, 3.5, 10.5);
        releaseOnClose(f, charles);
        ServerPlayer victim = f.player(6.5, FLOOR, 10.5, Power.LOKI);
        lookAt(charles, victim);
        press(charles, 3);
        check(MindLink.isHost(victim) && Powers.get(victim).is(Live.MIND_CONTROLLED), "PvP on: he steers the player");
        check(MindLink.ticksLeft(charles, helper.getLevel().getGameTime()) == 160, "for 8 seconds");
        // Their own keys do nothing.
        AbilityHandler.onKey(victim, 5, true);
        AbilityHandler.onKey(victim, 5, false);
        check(!Powers.get(victim).toggle(Toggle.FROST_GIANT), "the body's own keys do nothing (no Frost Giant for Loki)");
        // His keys move them: a velocity the server gives, never a position from his client.
        MindLink.input(charles, new XavierInputPayload(1.0F, 0.0F, false, false, TestKit.EAST, 0.0F, false));
        PowerTicker.tick(charles);
        check(victim.getDeltaMovement().x > 0.15 && Math.abs(victim.getDeltaMovement().z) < 1.0E-6, "they are pushed east: " + victim.getDeltaMovement());
        MindLink.input(charles, new XavierInputPayload(Float.NaN, 99.0F, false, false, Float.POSITIVE_INFINITY, 0.0F, false));
        PowerTicker.tick(charles);
        check(Double.isFinite(victim.getDeltaMovement().x) && victim.getDeltaMovement().length() < 0.5, "nonsense is clamped: " + victim.getDeltaMovement());
        // PvP off: let go.
        f.pvp(false);
        PowerTicker.tick(charles);
        check(!MindLink.isHost(victim) && !Powers.get(victim).is(Live.MIND_CONTROLLED), "PvP off: they are free");
        f.pvp(true);
        // Nobody is left stuck: a stray mark is cleared by the watchdog.
        Powers.get(victim).set(Live.MIND_CONTROLLED, true);
        PowerTicker.tick(victim);
        check(!Powers.get(victim).is(Live.MIND_CONTROLLED), "a mark with no link behind it is cleared");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PVP)
    public static void aSteeredPlayerLogsOutOrDiesAndIsFree(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        ServerPlayer a = xavier(f, 3.5, 4.5);
        releaseOnClose(f, a);
        ServerPlayer leaves = f.player(6.5, FLOOR, 4.5);
        lookAt(a, leaves);
        press(a, 3);
        check(MindLink.isHost(leaves), "a steers a player");
        helper.getLevel().getServer().getPlayerList().remove(leaves);
        check(!MindLink.isLinked(a) && !Powers.get(a).is(Live.MIND_LINKED), "their logout ends it");

        ServerPlayer b = xavier(f, 3.5, 12.5);
        releaseOnClose(f, b);
        ServerPlayer dies = f.player(6.5, FLOOR, 12.5);
        lookAt(b, dies);
        press(b, 3);
        check(MindLink.isHost(dies), "b steers a player");
        dies.hurt(dies.damageSources().fellOutOfWorld(), 1000.0F);
        check(!MindLink.isLinked(b) && !Powers.get(dies).is(Live.MIND_CONTROLLED), "their death ends it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = PVP)
    public static void aStrongerTelepathThrowsHimOut(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.pvp(true);
        Power[] strongers = {Power.SCARLET_WITCH, Power.PHOENIX, Power.BILLY};
        for (int i = 0; i < strongers.length; i++) {
            Power stronger = strongers[i];
            int row = 3 + 6 * i;
            ServerPlayer charles = xavier(f, 3.5, row + 0.5);
            releaseOnClose(f, charles);
            ServerPlayer them = f.player(7.5, FLOOR, row + 0.5, stronger);
            lookAt(charles, them);
            TestKit.sentPackets(charles);
            TestKit.sentPackets(them);
            press(charles, 3);
            check(!MindLink.isLinked(charles) && !MindLink.isHost(them), stronger + " cannot be taken");
            check(charles.getHealth() < charles.getMaxHealth(), stronger + " throws him out: he is hurt");
            check(charles.hasEffect(MobEffects.CONFUSION) && charles.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "and stunned");
            check(!Powers.get(charles).isReady(3, helper.getLevel().getGameTime()), "and V rests");
            check(payloads(TestKit.sentPackets(charles), XavierPayload.class).stream().anyMatch(p -> p.kind() == XavierPayload.THROWN_OUT),
                    "his screen flashes");
            check(actionBarKeys(them).contains("message.enhanced.xavier.threw_out"), stronger + " is told");
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // B: Freeze Minds, N: Cerebro

    @GameTest(template = ARENA, batch = QUICK)
    public static void freezeMindsHoldsOpenMindsAround(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = xavier(f, 10.5, 10.5);
        Zombie husk = PhoenixTests.zombie(helper, 13.5, 10.5);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(7.5, FLOOR, 10.5));
        Zombie helmet = helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(10.5, FLOOR, 14.5));
        helmet.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        ServerPlayer plain = f.player(10.5, FLOOR, 6.5);
        press(charles, 4);
        check(husk.hasEffect(ModEffects.SPELLBOUND) && villager.hasEffect(ModEffects.SPELLBOUND), "everyone around stands still");
        check(husk.getEffect(ModEffects.SPELLBOUND).getDuration() == 100, "for five seconds");
        check(!helmet.hasEffect(ModEffects.SPELLBOUND), "not under Magneto's helmet");
        check(!plain.hasEffect(ModEffects.SPELLBOUND), "nor a player with PvP off");
        check(!Powers.get(charles).isReady(4, helper.getLevel().getGameTime()) && Powers.get(charles).is(Live.XAVIER_FOCUS), "B rests");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = QUICK)
    public static void cerebroFindsTheMindsOfPlayers(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer charles = xavier(f, 3.5, 3.5);
        ServerPlayer thor = f.player(15.5, FLOOR, 15.5, Power.THOR);
        ServerPlayer hidden = f.player(10.5, FLOOR, 15.5);
        hidden.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.MAGNETO_HELMET.get()));
        TestKit.sentPackets(charles);
        TestKit.sentPackets(thor);
        press(charles, 5);
        PowerData data = Powers.get(charles);
        check(data.is(Live.CEREBRO) && !data.isReady(5, helper.getLevel().getGameTime()), "N: the helmet is on, N rests");
        List<CerebroPayload> sent = payloads(TestKit.sentPackets(charles), CerebroPayload.class);
        check(!sent.isEmpty(), "his client is told what Cerebro found");
        List<CerebroPayload.Mind> minds = sent.get(sent.size() - 1).minds();
        check(minds.stream().anyMatch(m -> m.name().equals(thor.getGameProfile().getName()) && m.power() == Power.THOR.ordinal()),
                "Thor is found, as Thor");
        check(minds.stream().noneMatch(m -> m.name().equals(hidden.getGameProfile().getName())), "Magneto's helmet hides a mind");
        check(minds.stream().noneMatch(m -> m.name().equals(charles.getGameProfile().getName())), "and he does not find himself");
        check(actionBarKeys(thor).contains("message.enhanced.xavier.cerebro.found_you"), "Thor feels it");
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The network

    @GameTest(template = ARENA, batch = QUICK)
    public static void theMessagesOfProfessorXRoundTrip(GameTestHelper helper) {
        check(Integer.parseInt(ModNetwork.PROTOCOL_VERSION) >= 10,
                "3.6 bumps the protocol to 10 (3.3 had 7, 3.4 8, 3.5 9; 3.7 11, pinned by MutantSkeletonTests)");
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        XavierInputPayload input = new XavierInputPayload(0.5F, -1.0F, true, false, 12.5F, -30.0F, true);
        XavierInputPayload.STREAM_CODEC.encode(buf, input);
        check(XavierInputPayload.STREAM_CODEC.decode(buf).equals(input), "the input");
        XavierPayload link = new XavierPayload(XavierPayload.LINK_START, 42, 160);
        XavierPayload.STREAM_CODEC.encode(buf, link);
        check(XavierPayload.STREAM_CODEC.decode(buf).equals(link), "the link");
        XavierChoicePayload choice = new XavierChoicePayload("old");
        XavierChoicePayload.STREAM_CODEC.encode(buf, choice);
        check(XavierChoicePayload.STREAM_CODEC.decode(buf).equals(choice), "the choice");
        ThoughtsPayload thoughts = new ThoughtsPayload(List.of(new ThoughtsPayload.Thought(7, Component.translatable("thought.enhanced.pig.0"), 0)));
        ThoughtsPayload.STREAM_CODEC.encode(buf, thoughts);
        check(ThoughtsPayload.STREAM_CODEC.decode(buf).equals(thoughts), "the thoughts");
        CerebroPayload cerebro = new CerebroPayload(List.of(new CerebroPayload.Mind("Eilon", Power.THOR.ordinal(), new Vec3(1, 2, 3))), 100);
        CerebroPayload.STREAM_CODEC.encode(buf, cerebro);
        check(CerebroPayload.STREAM_CODEC.decode(buf).equals(cerebro), "Cerebro's minds");
        helper.succeed();
    }

    private XavierTests() {
    }
}
