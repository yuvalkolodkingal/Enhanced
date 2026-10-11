package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossBars;
import com.kolodkingal.enhanced.boss.BossData;
import com.kolodkingal.enhanced.boss.BossEnd;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.client.BossBarOverride;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.ShieldStyle;
import com.kolodkingal.enhanced.client.render.ShieldStyles;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModTicketTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.assertNear;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The pieces of the boss framework that need no fight: the hero-generic shield (Wanda's numbers, another hero's), the
 * boss bars and the client's bar override, the saved data and the tables of endings and kinds.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BossClientTests {
    private static final String CLIENT = "boss_client_pure";

    private static String key(Component component) {
        return component.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
    }

    @GameTest(template = ARENA, batch = CLIENT)
    public static void theGenericShieldDrawsWandasChaosShieldWithWandasNumbers(GameTestHelper helper) {
        check(ShieldStyles.of(Power.SCARLET_WITCH) == ShieldStyles.CHAOS_SHIELD, "the Scarlet Witch has the Chaos Shield");
        for (Power power : Power.values()) {
            check(power == Power.SCARLET_WITCH || ShieldStyles.of(power) == null, power + " has none until its lane says one");
        }
        check(HeroClients.of(Power.APOCALYPSE).shieldStyle() == null, "a hero's client says no style by default");

        ShieldStyle chaos = ShieldStyles.CHAOS_SHIELD;
        assertNear(chaos.radius(), 1.9, 1.0E-6, "a radius of 1.9");
        check(chaos.veil() == 0xC72D4B && chaos.lines() == Power.SCARLET_WITCH.color, "in crimson with a pink rim");
        check(chaos.texture().getPath().equals("textures/entity/hex_wall.png"), "on the hex lattice");

        PowerData data = new PowerData();
        data.raiseChaosShield(1000, 15, 30.0F);
        ShieldStyle.Look start = chaos.look(data, 1000.0F);
        ShieldStyle.Look half = chaos.look(data, 1007.5F);
        ShieldStyle.Look done = chaos.look(data, 1015.0F);
        assertNear(start.radius(), 1.9 * 0.2, 1.0E-4, "it starts at a fifth of its size, at her hands");
        check(start.radius() < half.radius() && half.radius() < done.radius(), "and grows");
        assertNear(done.radius(), 1.9, 1.0E-4, "to its full size");
        check(start.flicker() < done.flicker(), "fainter while it forms");
        data.setShieldStrength(3.0F);
        check(chaos.look(data, 1015.0F).flicker() <= done.flicker(), "and gutters when it is nearly spent");

        // Another hero's shield has numbers of its own.
        ShieldStyle gold = new ShieldStyle(0xF2B84B, 0xFFF0B8, chaos.texture(), 2.4F, () -> 60.0F, () -> 120);
        PowerData apocalypse = new PowerData();
        apocalypse.raiseChaosShield(1000, 10, 60.0F);
        assertNear(gold.look(apocalypse, 1010.0F).radius(), 2.4, 1.0E-4, "a radius of 2.4");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CLIENT)
    public static void theHudLineOfAShieldTakesTheHerosOwnNumbers(GameTestHelper helper) {
        PowerData data = new PowerData();
        check(ShieldStyles.hudLine(data, 1000L, 30.0F, 160) == null, "no shield, no line");
        data.raiseChaosShield(1000, 15, 30.0F);
        check(key(ShieldStyles.hudLine(data, 1005L, 30.0F, 160)).equals("hud.enhanced.shield_forming"), "forming");
        Component full = ShieldStyles.hudLine(data, 1015L, 30.0F, 160);
        check(key(full).equals("hud.enhanced.shield_strength") && full.getStyle().getColor().getValue() == ChatFormatting.GREEN.getColor(),
                "complete: its strength, in green");
        TranslatableContents strength = (TranslatableContents) full.getContents();
        check(strength.getArgs()[0].equals(30) && strength.getArgs()[1].equals(30), "30 of 30");
        data.setShieldStrength(10.0F);
        check(ShieldStyles.hudLine(data, 1015L, 30.0F, 160).getStyle().getColor().getValue() == ChatFormatting.YELLOW.getColor(), "yellow below half");
        data.setShieldStrength(5.0F);
        check(ShieldStyles.hudLine(data, 1015L, 30.0F, 160).getStyle().getColor().getValue() == ChatFormatting.RED.getColor(), "red below a quarter");
        // The same strength is half of a shield of 60: Apocalypse's maximum, not Wanda's.
        data.setShieldStrength(25.0F);
        TranslatableContents other = (TranslatableContents) ShieldStyles.hudLine(data, 1015L, 60.0F, 120).getContents();
        check(other.getArgs()[0].equals(25) && other.getArgs()[1].equals(60), "25 of 60 for a hero whose shield has 60");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CLIENT)
    public static void aBossBarIsFoundByTheKeyOfItsName(GameTestHelper helper) {
        check(BossBarOverride.nameKey(Component.translatable("boss.enhanced.thanos")).equals("boss.enhanced.thanos"), "a translation is found by its key");
        check(BossBarOverride.nameKey(Component.translatable("boss.enhanced.thanos", "x").append(" more")).equals("boss.enhanced.thanos"),
                "whatever follows it");
        check(BossBarOverride.nameKey(Component.literal("Thanos")).isEmpty(), "a plain name has none");
        // (A drawer takes the client's GuiGraphics, which a test server cannot load: only the lookup is tried here.)
        check(BossBarOverride.drawerFor(Component.translatable("boss.enhanced.ultron")) == null, "nobody has registered a drawer: the game draws the bar");
        check(BossBarOverride.drawerFor(Component.literal("Thanos")) == null, "and a plain name is never a drawer's");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "boss_bars")
    public static void theBarsAreShownToFightersAndToWhoeverIsNearAndTakenFromTheRest(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer fighter = f.player(2.5, FLOOR, 2.5);
        ServerPlayer near = f.player(10.5, FLOOR, 10.5);
        ServerPlayer far = f.player(18.5, FLOOR, 18.5);
        BossBars bars = new BossBars(Component.translatable("boss.enhanced.ultron"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
        check(!bars.health().shouldDarkenScreen() && !bars.health().shouldCreateWorldFog() && !bars.health().shouldPlayBossMusic(),
                "the bar does not darken, fog or play music");
        check(!bars.second().isVisible() && bars.second().getColor() == BossEvent.BossBarColor.WHITE, "the second bar starts hidden");

        Vec3 centre = helper.absoluteVec(new Vec3(10.5, FLOOR, 10.5));
        bars.refresh(helper.getLevel(), centre.add(1000, 0, 0), List.of(fighter));
        check(bars.viewers().equals(java.util.Set.of(fighter)), "far from everyone, only the fighter sees it");
        bars.refresh(helper.getLevel(), centre, List.of(fighter));
        check(bars.viewers().contains(near) && bars.viewers().contains(fighter) && bars.viewers().contains(far),
                "near the centre, everybody does (the test floor is under 64 blocks wide)");
        bars.refresh(helper.getLevel(), centre.add(1000, 0, 0), List.of());
        check(bars.viewers().isEmpty(), "and when nobody is near and nobody fights, nobody");
        bars.refresh(helper.getLevel(), centre, List.of());
        bars.showSecond(true);
        check(bars.second().isVisible() && bars.second().getPlayers().containsAll(List.of(fighter, near, far)), "the second bar shows when asked");
        bars.close();
        check(bars.health().getPlayers().isEmpty() && bars.second().getPlayers().isEmpty(), "and closing takes both away");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = CLIENT)
    public static void theSavedDataAreNeverChangedInPlaceAndTheTablesAgree(GameTestHelper helper) {
        UUID who = UUID.randomUUID();
        BossData empty = BossData.EMPTY;
        BossData gifted = empty.withGift(who, new ItemStack(Items.NETHER_STAR, 1)).withGift(who, new ItemStack(Items.ECHO_SHARD, 2));
        check(empty.gifts().isEmpty() && gifted.giftsOf(who).size() == 2 && gifted.giftsOf(UUID.randomUUID()).isEmpty(), "gifts are added to a copy");
        BossData resting = gifted.withRest(BossKind.THANOS, 12000L);
        check(resting.restUntil(BossKind.THANOS) == 12000L && resting.restUntil(BossKind.ULTRON) == 0L && gifted.restUntil(BossKind.THANOS) == 0L,
                "so is a rest");
        check(resting.withoutGifts(who).giftsOf(who).isEmpty() && resting.withoutGifts(who).restUntil(BossKind.THANOS) == 12000L
                && resting.withoutGifts(UUID.randomUUID()) == resting, "gifts are taken without touching the rest");

        for (BossKind kind : BossKind.values()) {
            check(BossKind.byId(kind.id()) == kind && key(kind.displayName()).equals("entity.enhanced." + kind.id()), kind + " is named by its entity");
        }
        check(BossKind.ULTRON.ticket() == ModTicketTypes.ENHANCED_ULTRON && BossKind.THANOS.ticket() == ModTicketTypes.ENHANCED_THANOS
                && BossKind.APOCALYPSE.ticket() == ModTicketTypes.ENHANCED_APOCALYPSE, "and keeps its arena loaded with a ticket of its own");
        for (BossEnd end : BossEnd.values()) {
            check(end.messageKey().equals("message.enhanced.boss.end." + end.name().toLowerCase(java.util.Locale.ROOT)), end + " has a message");
        }
        helper.succeed();
    }

    private BossClientTests() {
    }
}
