package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.item.InfinityStone;
import com.kolodkingal.enhanced.item.LokisScepterItem;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.platform.reg.RegEntry;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;
import static com.kolodkingal.enhanced.gametest.TestKit.lookAt;

/**
 * The voices of the Infinity Stones: the files behind them, and where the Space and Mind Stones speak (the Tesseract,
 * Loki's Scepter, Vision's beam), always under the sound they come with. (The hum while someone holds the Tesseract or
 * the Scepter is client-side, and is checked in a running game.)
 */
public final class InfinityStoneTests {

    /** A sound the server sent: which, where and how loud. */
    private record Heard(ResourceLocation id, Vec3 at, float volume) {
    }

    private static List<Heard> heard(ServerPlayer listener) {
        List<Heard> heard = new ArrayList<>();
        for (Object sent : TestKit.sentPackets(listener)) {
            if (sent instanceof ClientboundSoundPacket packet) {
                heard.add(new Heard(packet.getSound().value().getLocation(), new Vec3(packet.getX(), packet.getY(), packet.getZ()), packet.getVolume()));
            }
        }
        return heard;
    }

    private static List<Heard> only(List<Heard> all, RegEntry<SoundEvent, SoundEvent> sound) {
        return all.stream().filter(h -> h.id().equals(sound.getId())).toList();
    }

    private static List<Heard> only(List<Heard> all, SoundEvent sound) {
        return all.stream().filter(h -> h.id().equals(sound.getLocation())).toList();
    }

    private static AABB arena(GameTestHelper helper) {
        return new AABB(helper.absoluteVec(new Vec3(0, 0, 0)), helper.absoluteVec(new Vec3(21, 12, 21)));
    }

    /** The loudness of the {@code stone} voice heard at {@code at}, which must be quieter than {@code with}. */
    private static void checkUnder(List<Heard> stone, List<Heard> with, String what) {
        check(!stone.isEmpty() && !with.isEmpty(), what + ": both sounds are there");
        for (Heard voice : stone) {
            Heard other = with.stream().filter(h -> h.at().equals(voice.at())).findFirst().orElse(null);
            check(other != null, what + ": the stone speaks where the other sound is played, at " + voice.at());
            check(voice.volume() < other.volume(), what + ": the stone (" + voice.volume() + ") is quieter than the sound it comes with (" + other.volume() + ")");
        }
    }

    // ---- the files ----

    private record OggInfo(int channels, int rate, long samples) {
        double seconds() {
            return samples / (double) rate;
        }
    }

    /** What an Ogg Vorbis file says about itself: its identification header and the last page's granule position. */
    private static OggInfo oggInfo(byte[] ogg) {
        int id = -1;
        for (int i = 0; i + 16 < Math.min(ogg.length, 100); i++) {
            if (ogg[i] == 1 && "vorbis".equals(new String(ogg, i + 1, 6, StandardCharsets.US_ASCII))) {
                id = i;
                break;
            }
        }
        check(id >= 0, "an Ogg Vorbis identification header");
        int channels = ogg[id + 11] & 0xFF;
        int rate = ByteBuffer.wrap(ogg, id + 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        int last = -1;
        for (int i = ogg.length - 14; i >= 0; i--) {
            if (ogg[i] == 'O' && ogg[i + 1] == 'g' && ogg[i + 2] == 'g' && ogg[i + 3] == 'S') {
                last = i;
                break;
            }
        }
        check(last >= 0, "an Ogg page at the end");
        return new OggInfo(channels, rate, ByteBuffer.wrap(ogg, last + 6, 8).order(ByteOrder.LITTLE_ENDIAN).getLong());
    }

    private static JsonObject json(String... path) throws IOException {
        return JsonParser.parseString(Files.readString(modFile(path))).getAsJsonObject();
    }

    /**
     * Each of the six stones has a use sound with several takes and a hum, registered and in sounds.json, with
     * subtitles in both languages; every file is a mono 44.1 kHz Ogg Vorbis like the mod's other sounds, the uses are
     * short and the hums are 2-second loops.
     */
    @EnhancedTest(template = ARENA)
    public static void everyStoneHasItsVoice(GameTestHelper helper) throws IOException {
        JsonObject sounds = json("assets", Enhanced.MODID, "sounds.json");
        JsonObject en = json("assets", Enhanced.MODID, "lang", "en_us.json");
        JsonObject he = json("assets", Enhanced.MODID, "lang", "he_il.json");
        check(InfinityStone.values().length == 6, "six Infinity Stones");
        for (InfinityStone stone : InfinityStone.values()) {
            for (boolean hum : new boolean[] {false, true}) {
                String name = stone.id() + "_stone" + (hum ? "_hum" : "");
                SoundEvent event = hum ? stone.humSound() : stone.useSound();
                check(BuiltInRegistries.SOUND_EVENT.getKey(event).equals(Enhanced.id(name)), name + " is registered under its name");
                check(sounds.has(name), name + " is in sounds.json");
                JsonObject entry = sounds.getAsJsonObject(name);
                String subtitle = entry.get("subtitle").getAsString();
                check(en.has(subtitle) && he.has(subtitle), name + " has its subtitle in English and Hebrew");
                JsonArray takes = entry.getAsJsonArray("sounds");
                int wanted = hum ? 1 : (stone == InfinityStone.SPACE || stone == InfinityStone.MIND ? 3 : 2);
                check(takes.size() >= wanted, name + " has at least " + wanted + " takes, not " + takes.size());
                for (JsonElement take : takes) {
                    ResourceLocation file = ResourceLocation.parse(take.getAsJsonObject().get("name").getAsString());
                    Path path = modFile(("assets/" + file.getNamespace() + "/sounds/" + file.getPath() + ".ogg").split("/"));
                    check(Files.isRegularFile(path), file + " has its .ogg file");
                    OggInfo info = oggInfo(Files.readAllBytes(path));
                    check(info.channels() == 1 && info.rate() == 44100, file + " is mono 44.1 kHz, not " + info.channels() + " channels at " + info.rate());
                    if (hum) {
                        check(file.getPath().contains("_loop_"), file + " is named as a loop");
                        check(Math.abs(info.seconds() - 2.0) < 0.005, file + " loops over 2 s, not " + info.seconds());
                    } else {
                        check(info.seconds() >= 0.8 && info.seconds() <= 3.0, file + " is a short sound, not " + info.seconds() + " s");
                    }
                }
            }
        }
        helper.succeed();
    }

    // ---- the Space Stone: the Tesseract ----

    /** Loki's escape: the Space Stone swells with the portal at both ends, quieter than it. */
    @EnhancedTest(template = ARENA)
    public static void tesseractEscapeHasTheSpaceStoneUnderThePortal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        heard(player);

        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        List<Heard> all = heard(player);
        List<Heard> space = only(all, ModSounds.SPACE_STONE);
        check(space.size() == 2, "the Space Stone sounds where they vanish and where they appear, not " + space.size() + " times");
        check(space.stream().anyMatch(h -> arena(helper).contains(h.at())) && space.stream().anyMatch(h -> !arena(helper).contains(h.at())),
                "one end in the arena, the other far away");
        checkUnder(space, only(all, ModSounds.TESSERACT), "Loki's escape");
        helper.succeed();
    }

    /** A hero's jump: the same, at both ends. */
    @EnhancedTest(template = ARENA)
    public static void tesseractJumpHasTheSpaceStoneAtBothEnds(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(3.5, FLOOR, 10.5, Power.MAGNETO);
        magneto.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        BlockPos block = new BlockPos(12, 2, 10);
        helper.setBlock(block, Blocks.STONE);
        lookAt(magneto, helper.absoluteVec(Vec3.atCenterOf(block)));
        heard(magneto);

        magneto.getMainHandItem().use(helper.getLevel(), magneto, InteractionHand.MAIN_HAND);

        List<Heard> all = heard(magneto);
        check(only(all, ModSounds.SPACE_STONE).size() == 2, "the jump plays the Space Stone at both ends");
        checkUnder(only(all, ModSounds.SPACE_STONE), only(all, ModSounds.TESSERACT), "the jump");
        helper.succeed();
    }

    /** Tesseract + gene: no portal, but the Space Stone answers the gene, under the awakening's own sound. */
    @EnhancedTest(template = ARENA)
    public static void tesseractWithGeneHasTheSpaceStoneButNoPortal(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TESSERACT.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()));
        heard(player);

        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        check(Powers.is(player, Power.SCARLET_WITCH), "she awakens");
        List<Heard> all = heard(player);
        check(only(all, ModSounds.SPACE_STONE).size() == 1, "the Space Stone speaks once");
        check(only(all, ModSounds.TESSERACT).isEmpty(), "and the cube opens no portal");
        checkUnder(only(all, ModSounds.SPACE_STONE), only(all, ModSounds.AWAKEN), "the awakening");
        helper.succeed();
    }

    // ---- the Mind Stone: Loki's Scepter ----

    /** The scepter, gene in the other hand: the Mind Stone sings as the Scarlet Witch awakens, under the awakening. */
    @EnhancedTest(template = ARENA)
    public static void scepterAwakeningOfTheScarletWitchHasTheMindStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()));
        heard(player);

        LokisScepterItem.touchMindStone(player, InteractionHand.MAIN_HAND);

        check(Powers.is(player, Power.SCARLET_WITCH), "she awakens");
        List<Heard> all = heard(player);
        check(only(all, ModSounds.MIND_STONE).size() == 1, "the Mind Stone sings once");
        checkUnder(only(all, ModSounds.MIND_STONE), only(all, ModSounds.AWAKEN), "the awakening");
        helper.succeed();
    }

    /** The scepter and a synthezoid core: the Mind Stone sings as it moves into the body, and Vision awakens. */
    @EnhancedTest(template = ARENA)
    public static void scepterAwakeningOfVisionHasTheMindStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SYNTHEZOID_CORE.get()));
        heard(player);

        LokisScepterItem.touchMindStone(player, InteractionHand.MAIN_HAND);

        check(Powers.is(player, Power.VISION), "Vision awakens");
        List<Heard> all = heard(player);
        check(only(all, ModSounds.MIND_STONE).size() == 1, "the Mind Stone sings once");
        checkUnder(only(all, ModSounds.MIND_STONE), only(all, ModSounds.AWAKEN), "the awakening");
        helper.succeed();
    }

    /** It sings for Vision and the Scarlet Witch too, when they touch it: it knows them. */
    @EnhancedTest(template = ARENA)
    public static void scepterRecognisingAHeroHasTheMindStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer wanda = f.player(10.5, FLOOR, 10.5, Power.SCARLET_WITCH);
        wanda.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        heard(wanda);

        LokisScepterItem.touchMindStone(wanda, InteractionHand.MAIN_HAND);

        List<Heard> all = heard(wanda);
        check(only(all, ModSounds.MIND_STONE).size() == 1, "the Mind Stone answers the Scarlet Witch");
        checkUnder(only(all, ModSounds.MIND_STONE), only(all, SoundEvents.AMETHYST_BLOCK_RESONATE), "the recognition");
        helper.succeed();
    }

    /**
     * Touching the stone with neither gene nor body tears an ordinary player apart, and it sings first. Own batch: it
     * explodes.
     */
    @EnhancedTest(template = ARENA, batch = "mind_stone_death")
    public static void scepterTearingSomeoneApartHasTheMindStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer player = f.player(10.5, FLOOR, 10.5, GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        heard(player);

        LokisScepterItem.touchMindStone(player, InteractionHand.MAIN_HAND);

        check(only(heard(player), ModSounds.MIND_STONE).size() == 1, "the Mind Stone sounds for the touch that would kill an ordinary body");
        helper.succeed();
    }

    /** A hero who already has powers gets only a message when the gene meets the stone: nothing happens, no singing. */
    @EnhancedTest(template = ARENA)
    public static void scepterThatDoesNothingIsSilent(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer magneto = f.player(10.5, FLOOR, 10.5, Power.MAGNETO);
        magneto.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        magneto.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()));
        heard(magneto);

        LokisScepterItem.touchMindStone(magneto, InteractionHand.MAIN_HAND);

        check(only(heard(magneto), ModSounds.MIND_STONE).isEmpty(), "the Mind Stone stays quiet when it does nothing");
        helper.succeed();
    }

    /** A hit that confuses someone: the Mind Stone's voice under the chime. */
    @EnhancedTest(template = ARENA)
    public static void scepterHitHasTheMindStoneUnderItsChime(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer attacker = f.player(10.5, FLOOR, 10.5);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        Zombie zombie = TestKit.spawn(helper, EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 10.5));
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // no sunburn
        zombie.setNoAi(true);
        heard(attacker);

        attacker.attack(zombie);

        check(zombie.hasEffect(ModEffects.CONFUSED), "the zombie is confused");
        List<Heard> all = heard(attacker);
        check(only(all, ModSounds.MIND_STONE).size() == 1, "the Mind Stone sings once");
        checkUnder(only(all, ModSounds.MIND_STONE), only(all, SoundEvents.AMETHYST_BLOCK_CHIME), "the hit");
        helper.succeed();
    }

    /** A blow that kills leaves nothing behind: no confusion, no chime, no Mind Stone. */
    @EnhancedTest(template = ARENA)
    public static void scepterKillingBlowHasNoMindStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer attacker = f.player(10.5, FLOOR, 10.5);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOKIS_SCEPTER.get()));
        Zombie zombie = TestKit.spawn(helper, EntityType.ZOMBIE, new Vec3(11.5, FLOOR, 10.5));
        zombie.setNoAi(true);
        zombie.setHealth(0.1F);
        heard(attacker);

        attacker.attack(zombie);

        check(!zombie.isAlive(), "the blow kills the zombie");
        check(only(heard(attacker), ModSounds.MIND_STONE).isEmpty(), "and the Mind Stone doesn't sing over it");
        helper.succeed();
    }

    // ---- the Mind Stone: Vision ----

    /** The beam starting: the stone in his forehead sings once; holding R doesn't repeat it, letting go makes none. */
    @EnhancedTest(template = ARENA)
    public static void visionBeamStartHasTheMindStone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer vision = f.player(10.5, FLOOR, 10.5, Power.VISION);
        heard(vision);

        AbilityHandler.onKey(vision, 1, true);
        List<Heard> start = heard(vision);
        check(only(start, ModSounds.MIND_STONE).size() == 1, "the Mind Stone sings as the beam starts");
        checkUnder(only(start, ModSounds.MIND_STONE), only(start, SoundEvents.BEACON_POWER_SELECT), "the ignition");

        AbilityHandler.onKey(vision, 1, true);
        check(only(heard(vision), ModSounds.MIND_STONE).isEmpty(), "holding the key doesn't sing it again");
        AbilityHandler.onKey(vision, 1, false);
        check(only(heard(vision), ModSounds.MIND_STONE).isEmpty(), "and neither does letting go");
        helper.succeed();
    }

    /** A file of the mod by its path parts (a path that does not exist if the mod has no such file). */
    private static Path modFile(String... parts) {
        return Platform.get().modResource(String.join("/", parts)).orElse(Path.of("missing", parts));
    }
}
