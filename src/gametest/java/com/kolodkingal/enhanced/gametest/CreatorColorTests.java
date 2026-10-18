package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.BillyAbilities;
import com.kolodkingal.enhanced.ability.SpeedStrikes;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.CustomBeams;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroJson;
import com.kolodkingal.enhanced.custom.CustomHeroRules;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.LokiDaggerEntity;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.kolodkingal.enhanced.gametest.CustomKitSupport.def;
import static com.kolodkingal.enhanced.gametest.CustomKitSupport.tap;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Every ability can change colour (3.5, the creator lane): each pick's colour reaches the effects of its row
 * ({@link AbilityColors}), the server sends it where it sends an effect (a beam's style, a thrown dagger, the Hex), the
 * built-in heroes keep their colours, and the colours survive the JSON file, the save and the wire.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class CreatorColorTests {
    private static final String BATCH = "creator_color";
    private static final int GREEN = 0x43A047;
    private static final int BLUE = 0x1E88E5;
    private static final int GOLD = 0xFDD835;
    private static final int RED = 0xE53935;

    private static CustomHero.Pick pick(String id, int color) {
        return new CustomHero.Pick(id, 5, HeroAbility.BOTH, color);
    }

    private static CustomHero with(CustomHero.Pick... picks) {
        return CustomHero.EMPTY.withName("Colour Tester").withPowers(List.of(picks));
    }

    /**
     * Every row of the catalogue takes its pick's colour; "hero colour" (-1) takes none (the effect keeps its own, or
     * the hero's); a variant row not picked takes its parent's pick's, and a parent not picked its variant row's.
     */
    @GameTest(template = ARENA, batch = BATCH)
    public static void everyRowTakesTheColourOfItsPick(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (HeroAbility a : HeroAbility.values()) {
            int c = (a.ordinal() * 0x2F3A1B) & 0xFFFFFF;
            if (AbilityColors.chosen(with(pick(a.id(), c)), a) != c) {
                wrong.add(a.id());
            }
            if (AbilityColors.chosen(with(pick(a.id(), -1)), a) != -1) {
                wrong.add(a.id() + " (hero colour)");
            }
            if (a.parent() != null && AbilityColors.chosen(with(pick(a.parent().id(), c)), a) != c) {
                wrong.add(a.id() + " (from its parent)");
            }
            if (a.variant() != null && AbilityColors.chosen(with(pick(a.variant().id(), c)), a) != c) {
                wrong.add(a.id() + " (from its variant row)");
            }
        }
        check(wrong.isEmpty(), "rows that miss their colour: " + wrong);
        // A palette is recoloured as a whole: white stays white, the hue is the chosen one.
        check(AbilityColors.recolor(0xFFFFFF, GREEN) == 0xFFFFFF, "a white core stays white");
        float[] hsl = AbilityColors.hsl(AbilityColors.recolor(0xC72D4B, BLUE));
        assertHue(hsl[0], AbilityColors.hsl(BLUE)[0], "the Chaos red becomes blue");
        helper.succeed();
    }

    private static void assertHue(float got, float want, String what) {
        float d = Math.abs(got - want);
        check(Math.min(d, 1.0F - d) < 0.02F, what + ": " + got + " vs " + want);
    }

    /** A built-in hero keeps every colour of his: nothing chosen, his beam's own style. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theBuiltInHeroesKeepTheirColours(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer billy = f.player(10.5, TestKit.FLOOR, 10.5);
        Powers.setPower(billy, Power.BILLY);
        for (HeroAbility a : HeroAbility.values()) {
            check(AbilityColors.chosen(billy, a) == -1 && AbilityColors.shade(billy, a, 0x123456) == 0x123456,
                    a.id() + " unchanged for Billy");
        }
        check(BillyAbilities.beamStyle(billy) == BeamStyles.BILLY, "Billy's beam is Billy's blue");
        check(Telekinesis.Mode.MAGNETO.colorOf(billy) == Telekinesis.Mode.MAGNETO.color, "a mode's own colour");
        helper.succeed();
    }

    /**
     * What the server makes of a custom hero's coloured picks: the Hex Beam's style (the beam's payload names it), a
     * thrown dagger's colour (synced with the entity), the Hex's colour (synced with the level's Hexes), a dash's
     * colours, a grip's.
     */
    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 40)
    public static void theServerSendsEveryEffectInItsColour(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        f.config(EnhancedConfig.HEX_DEFAULT_RADIUS, 8);
        CustomHero hero = def().build().withPowers(List.of(pick("billy.hex_beam", GREEN), pick("loki.daggers", GOLD),
                pick("wanda.hex", BLUE), pick("quicksilver.dash", RED), pick("magneto.metal", BLUE)))
                .withKeys(List.of("loki.daggers", "wanda.hex", "billy.hex_beam", "quicksilver.dash", "magneto.metal"));
        ServerPlayer player = CustomKitSupport.hero(f, 10.5, 10.5, hero);
        check(BillyAbilities.beamStyle(player) == CustomBeams.style(GREEN, AbilityColors.glowOf(GREEN))
                && BillyAbilities.beamStyle(player).id()
                        .equals(String.format(Locale.ROOT, "custom_%06x_%06x", GREEN, AbilityColors.glowOf(GREEN))),
                "the Hex Beam's style names its green: " + BillyAbilities.beamStyle(player).id());
        check(BillyAbilities.beamSpec(player).style() == BillyAbilities.beamStyle(player),
                "the beam's numbers carry it");

        tap(player, 1, false);
        List<LokiDaggerEntity> daggers = helper.getLevel().getEntitiesOfClass(LokiDaggerEntity.class,
                player.getBoundingBox().inflate(6.0));
        check(!daggers.isEmpty(), "R threw a dagger");
        for (LokiDaggerEntity dagger : daggers) {
            check(dagger.magicColor() == GOLD, "the dagger flies gold: " + Integer.toHexString(dagger.magicColor()));
        }

        tap(player, 2, false);
        Optional<Hex> hex = HexManager.ownedBy(helper.getLevel(), player.getUUID());
        check(hex.isPresent() && hex.get().color() == BLUE && HexManager.wallColor(hex.get()) == BLUE,
                "G raises a blue Hex");
        ByteBuf buf = Unpooled.buffer();
        Hex.STREAM_CODEC.encode(buf, hex.get());
        check(Hex.STREAM_CODEC.decode(buf).color() == BLUE, "the Hex's colour goes over the wire");

        check(SpeedStrikes.Style.of(player, Power.QUICKSILVER, HeroAbility.QUICKSILVER_DASH).color() == RED,
                "the dash is red");
        check(Telekinesis.Mode.MAGNETO.colorOf(player) == BLUE, "Magneto's grip is blue");
        check(AbilityColors.chosen(player, HeroAbility.THANOS_POWER) == -1, "what he has not picked chooses nothing");
        helper.succeed();
    }

    /** Every pick's colour, a hero row's and a variant row's too, survives the JSON file, the save and the wire. */
    @GameTest(template = ARENA, batch = BATCH)
    public static void theColoursOfEveryPickSurviveJsonSaveAndWire(GameTestHelper helper) {
        CustomHero raw = def().build().withPowers(List.of(pick("billy.hex_barrier", GREEN),
                new CustomHero.Pick("wanda.chaos_beam", 6, HeroAbility.BOTH, BLUE), pick("spider.web", GOLD),
                pick("thor.lightning", RED), new CustomHero.Pick("energy_blast", 4, "bolts", 0xFF4040),
                new CustomHero.Pick("flight", 5, "jet", -1)))
                .withKeys(List.of("billy.hex_barrier", "wanda.chaos_beam", "spider.web", "thor.lightning",
                        "energy_blast"));
        CustomHero hero = CustomHeroRules.sanitize(raw, CustomHeroRules.Limits.DEFAULT).hero();
        check(hero.pick(HeroAbility.BILLY_HEX_BARRIER).color() == GREEN
                && hero.pick(HeroAbility.THOR_LIGHTNING).color() == RED,
                "the rules keep the colours");
        String json = CustomHeroJson.write(hero);
        JsonArray powers = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("powers");
        int coloured = 0;
        for (JsonElement e : powers) {
            JsonObject p = e.getAsJsonObject();
            if (p.has("color")) {
                coloured++;
            }
            if (p.get("id").getAsString().equals("billy.hex_barrier")) {
                check(p.get("color").getAsString().equalsIgnoreCase("#43A047"),
                        "the barrier's colour in the file: " + p);
            }
            if (p.get("id").getAsString().equals("flight")) {
                check(!p.has("color"), "the hero's colour writes nothing");
            }
        }
        check(coloured == 5, "five colours in the file: " + coloured);
        CustomHeroJson.Read read = CustomHeroJson.read(json);
        check(read.ok() && hero.equals(read.hero()), "the file reads back the same");
        Tag saved = CustomHero.CODEC.encodeStart(NbtOps.INSTANCE, hero).getOrThrow();
        check(hero.equals(CustomHero.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow()), "the save");
        ByteBuf buf = Unpooled.buffer();
        CustomHero.STREAM_CODEC.encode(buf, hero);
        check(hero.equals(CustomHero.STREAM_CODEC.decode(buf)), "the wire");
        helper.succeed();
    }
}
