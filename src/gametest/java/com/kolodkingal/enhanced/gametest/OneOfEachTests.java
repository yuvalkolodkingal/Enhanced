package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.AsgardPopulation;
import com.kolodkingal.enhanced.asgard.npc.AsgardTalk;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Asgardians;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.config.HeimdallConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.heimdall.HeimdallPost;
import com.kolodkingal.enhanced.heimdall.HeimdallTalk;
import com.kolodkingal.enhanced.network.AsgardTalkPayload;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * 3.5.1, Eilon's requests of 19/10: one of each character (the Odin of the city is away while a player is Odin and comes
 * back after, and the lines that spoke of the Allfather asleep name the player instead); Heimdall the exception (the
 * city's Heimdall hands his post to a player who is Heimdall, steps aside, and takes it back when it ends); and Hela's
 * headdress redrawn (its textures are not the old "pineapple" ones). Batches {@code asgard_one_each} and
 * {@code asgard_one_each_lines} (a player who is Odin is seen server-wide: each of these tests has its batch) and
 * {@code heimdall_post} (one keeper at a time; config).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class OneOfEachTests {
    static final String BATCH = "asgard_one_each";
    static final String LINES = "asgard_one_each_lines";
    static final String POST = "heimdall_post";

    /** The SHA-256 of the 3.5.0 headdress textures (the "pineapple"): the new ones must differ. */
    static final Map<String, String> OLD_TEXTURES = Map.of(
            "textures/entity/suit/hela_headdress.png", "d646a8072910ec96153c9b5bff92b473e1c05ed13d696d47cbf59be5947eb4bc",
            "textures/item/hela_headdress.png", "89edfbf0d89e0d4810304645647561e5718729aa1696c3de89a868425243f0f9");

    private OneOfEachTests() {
    }

    // ---- one of each character: Odin ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 120)
    public static void theOdinOfTheCityIsAwayWhileAPlayerIsOdin(GameTestHelper helper) {
        TestKit.Fixture fx = TestKit.fixture(helper);
        Map<String, UUID> table = AsgardianTests.records(fx);
        BlockPos floor = helper.absolutePos(new BlockPos(5, 2, 5));
        List<CityPlan.Home> homes = List.of(new CityPlan.Home(Role.ODIN, 0, floor, 4),
                new CityPlan.Home(Role.FRIGGA, 0, floor.east(6), 4));
        check(Role.ODIN.yieldsToHero() && !Role.HEIMDALL.yieldsToHero() && Role.HEIMDALL.hero() == Power.HEIMDALL
                && Role.FRIGGA.hero() == null, "Odin yields to his hero; Heimdall stays; Frigga has no hero");
        fx.everyTick(100, tick -> {
            if (!AsgardianTests.areaLoaded(fx.level(), floor) || !AsgardianTests.areaLoaded(fx.level(), floor.east(6))) {
                return false;
            }
            ServerLevel level = fx.level();
            AsgardPopulation.useResidents(level, AsgardianTests.residents(fx));
            List<AsgardianEntity> made = AsgardPopulation.keep(level, homes, AsgardianTests.residents(fx));
            check(made.size() == 2, "nobody is Odin: both are made: " + made.size());
            AsgardianEntity odin = made.stream().filter(n -> n.role() == Role.ODIN).findFirst().orElseThrow();
            AsgardianEntity frigga = made.stream().filter(n -> n.role() == Role.FRIGGA).findFirst().orElseThrow();
            ServerPlayer player = fx.player(2.5, FLOOR, 2.5, Power.ODIN);
            check(Asgardians.yields(level.getServer(), Role.ODIN), "a player is Odin: the Odin of the city yields");
            check(AsgardPopulation.keep(level, homes, AsgardianTests.residents(fx)).isEmpty(), "the keeper makes nobody");
            check(odin.isRemoved() && !table.containsKey("ODIN"), "the Odin of the city has left (in gold), unrecorded");
            check(!frigga.isRemoved(), "Frigga stays");
            AsgardianEntity copy = ModEntities.ASGARDIAN.get().create(level);
            copy.setRole(Role.ODIN, 0);
            copy.setHome("ODIN", floor, 4);
            copy.moveTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
            check(!level.addFreshEntity(copy), "one of his slot coming back while a player is Odin is refused");
            check(AsgardPopulation.keep(level, homes, AsgardianTests.residents(fx)).isEmpty(), "still nobody");
            Powers.setPower(player, Power.NONE);
            check(!Asgardians.yields(level.getServer(), Role.ODIN), "nobody is Odin any more");
            List<AsgardianEntity> back = AsgardPopulation.keep(level, homes, AsgardianTests.residents(fx));
            check(back.size() == 1 && back.get(0).role() == Role.ODIN && back.get(0).getUUID().equals(table.get("ODIN")),
                    "the Odin of the city returns, recorded");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = LINES)
    public static void theLinesOfTheAllfatherAsleepNameThePlayerWhoIsOdin(GameTestHelper helper) {
        TestKit.Fixture fx = AsgardTestSupport.fixture(helper);
        ServerPlayer talker = fx.player(5.5, FLOOR, 5.5);
        AsgardianEntity hogun = AsgardianTests.npc(fx, Role.HOGUN, 7.5, 5.5);
        AsgardianEntity frigga = AsgardianTests.npc(fx, Role.FRIGGA, 5.5, 7.5);
        AsgardDialogue.Ctx ctx = AsgardTalk.context(talker, hogun, Role.HOGUN);
        check(AsgardDialogue.said(ctx, "npc.enhanced.hogun.vault", List.of()).line().equals("npc.enhanced.hogun.vault"),
                "nobody is Odin: the vault stays sealed while the Allfather sleeps");
        check(AsgardDialogue.swaps().size() >= 8, "every line of the Allfather asleep has its woken line: " + AsgardDialogue.swaps().size());
        ServerPlayer odin = fx.player(3.5, FLOOR, 3.5, Power.ODIN);
        String name = odin.getGameProfile().getName();
        for (AsgardDialogue.Swap swap : AsgardDialogue.swaps()) {
            AsgardDialogue.Said said = AsgardDialogue.said(ctx, swap.line(), List.of());
            check(said.line().equals(swap.instead()) && said.args().equals(List.of(name)), swap.line() + " names " + name + ": " + said);
        }
        check(AsgardDialogue.said(ctx, "npc.enhanced.einherjar.news.3", List.of()).line().equals("npc.enhanced.einherjar.news_odin_player"),
                "the Einherjar's news: the Allfather has woken");
        // Through a talk: Frigga, asked where Odin is, says the player sits the throne.
        TestKit.sentPackets(talker);
        check(AsgardTalk.begin(talker, frigga), "Frigga talks");
        check(AsgardTalk.tryChoose(talker, frigga.getId(), "about_odin"), "where is Odin?");
        AsgardTalkPayload.Open open = lastOpen(talker);
        check(open != null && open.node().equals("npc.enhanced.frigga.odin_player") && open.args().equals(List.of(name)),
                "the Allfather has woken: " + name + " sits the throne: " + open);
        AsgardTalk.end(talker);
        Powers.setPower(odin, Power.NONE);
        check(AsgardDialogue.said(ctx, "npc.enhanced.hogun.vault", List.of()).line().equals("npc.enhanced.hogun.vault"),
                "nobody is Odin again: the old line");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void helasHeaddressIsNoLongerThePineapple(GameTestHelper helper) throws IOException, NoSuchAlgorithmException {
        IModFile mod = ModList.get().getModFileById(Enhanced.MODID).getFile();
        for (Map.Entry<String, String> old : OLD_TEXTURES.entrySet()) {
            String[] parts = ("assets/" + Enhanced.MODID + "/" + old.getKey()).split("/");
            Path path = mod.findResource(parts);
            check(Files.exists(path), old.getKey() + " exists");
            byte[] bytes = Files.readAllBytes(path);
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            check(!sha.equals(old.getValue()), old.getKey() + " is redrawn (not the 3.5.0 texture)");
            BufferedImage image;
            try (InputStream in = Files.newInputStream(path)) {
                image = ImageIO.read(in);
            }
            int size = old.getKey().contains("/item/") ? 16 : 32;
            check(image != null && image.getWidth() == size && image.getHeight() == size, old.getKey() + " is " + size + "x" + size);
            int dark = 0;
            int opaque = 0;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int argb = image.getRGB(x, y);
                    if ((argb >>> 24) > 0) {
                        opaque++;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        if (r + g + b < 3 * 90) {
                            dark++;
                        }
                    }
                }
            }
            check(opaque > 0 && dark * 2 > opaque, old.getKey() + " is mostly black-green: " + dark + "/" + opaque);
        }
        helper.succeed();
    }

    /** The last talk screen the player was sent, or null. */
    static AsgardTalkPayload.Open lastOpen(ServerPlayer player) {
        AsgardTalkPayload.Open last = null;
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof AsgardTalkPayload.Open open) {
                last = open;
            }
        }
        return last;
    }

    // ---- Heimdall the exception: his post ----

    /** A player in Asgard, at the Observatory (the Bifrost's landing). */
    static ServerPlayer inAsgard(TestKit.Fixture f, GameTestHelper helper, Power power) {
        AsgardTestSupport.withArrival(helper.getLevel().getServer());
        ServerPlayer player = f.player(10.5, FLOOR, 10.5, power);
        check(Bifrost.open(player, Bifrost.Destination.OBSERVATORY, Bifrost.Source.RESCUE), "the Bifrost takes him to the Observatory");
        check(AsgardDimension.in(player), "he is in Asgard");
        return player;
    }

    /** The Heimdall of the city at his post by the socket, two blocks from {@code near}. */
    static AsgardianEntity cityHeimdall(TestKit.Fixture f, ServerPlayer near) {
        ServerLevel asgard = near.serverLevel();
        AsgardianEntity npc = ModEntities.ASGARDIAN.get().create(asgard);
        npc.setRole(Role.HEIMDALL, 0);
        npc.setHome("", CityPlan.OBSERVATORY_SOCKET.south(3), 4);
        npc.moveTo(near.getX() + 2.0, near.getY(), near.getZ(), 0.0F, 0.0F);
        check(asgard.addFreshEntity(npc), "Heimdall stands in the Observatory");
        f.onClose(npc::discard);
        return npc;
    }

    @GameTest(template = ARENA, batch = POST, timeoutTicks = 200)
    public static void theCitysHeimdallHandsHisPostToAPlayerWhoIsHeimdall(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerPlayer player = inAsgard(f, helper, Power.HEIMDALL);
        f.onClose(() -> HeimdallPost.end(player, false));
        AsgardianEntity npc = cityHeimdall(f, player);
        check(!Asgardians.yields(player.server, Role.HEIMDALL), "Heimdall stays while a player is Heimdall");
        // Their first meeting: he comes near, and the post is his.
        TestKit.sentPackets(player);
        HeimdallPost.forgetMeeting(player);
        HeimdallPost.tick(player, Powers.get(player));
        check(HeimdallPost.keeps(player), "on their first meeting, Heimdall hands him his post");
        check(saidInChat(player, "npc.enhanced.heimdall.handover"), "\"We are both guardians of Asgard\"");
        check(npc.restAt() != null && !npc.restAt().equals(npc.home()) && npc.post().equals(npc.restAt()),
                "Heimdall steps aside to rest nearby: " + npc.restAt());
        check(HeimdallPost.bar() != null && HeimdallPost.bar().getPlayers().contains(player), "a bar counts the time down");
        long left = HeimdallPost.ticksLeft(player);
        check(left > 0 && left <= HeimdallPost.seconds() * 20L, "for heimdall.postSeconds: " + left);
        // His rights: he sends a traveller from the Observatory with no cooldown, and the socket answers to him.
        ServerPlayer traveller = inAsgard(f, helper, Power.NONE);
        PowerData data = Powers.get(player);
        data.startCooldown(HeroAbility.HEIMDALL_STRIKE, player.level().getGameTime(), 2000);
        check(HeimdallPost.sendsFreely(player), "in the Observatory, the keeper sends freely");
        check(Bifrost.send(player, traveller.getId(), "home"), "he sends the traveller home though his Strike rests");
        check(HeimdallPost.touchSocket(player), "the socket answers to its keeper");
        // Others hear who keeps the Bifrost; he may give it back.
        check("greeting".equals(picked(traveller, npc)), "a newcomer is greeted first: " + picked(traveller, npc));
        traveller.setData(ModAttachments.ASGARD, traveller.getData(ModAttachments.ASGARD).with(AsgardProgress.MET_HEIMDALL));
        check("keeper_away".equals(picked(traveller, npc)), "then hears who keeps the Bifrost: " + picked(traveller, npc));
        check(AsgardDialogue.offered(AsgardTalk.context(traveller, npc, Role.HEIMDALL), AsgardDialogue.find(Role.HEIMDALL, "keeper_away"))
                .stream().anyMatch(o -> o.key().equals(HeimdallTalk.HOME)), "and may still be sent home");
        check("post_kept".equals(picked(player, npc)), "the keeper is asked whether Heimdall may take it back");
        HeimdallPost.end(player, true);
        check(!HeimdallPost.keeps(player) && npc.restAt() == null && npc.post().equals(npc.home()), "Heimdall walks back to his post");
        check(saidInChat(player, "npc.enhanced.heimdall.post_return"), "and thanks him");
        check(HeimdallPost.bar() == null, "the bar is gone");
        // Talking again hands it again, for the configured time; it ends by itself.
        f.config(HeimdallConfig.POST_SECONDS, 1);
        check("handover".equals(picked(player, npc)), "talking again: the handover");
        check(AsgardTalk.begin(player, npc) && "handover".equals(AsgardTalk.currentNode(player)), "the talk opens on it");
        check(HeimdallPost.keeps(player) && HeimdallPost.ticksLeft(player) <= 20, "the post is his again, for a second");
        check(AsgardTalk.offered(player).equals(List.of(HeimdallTalk.POST_THANKS)), "\"I will guard it well\"");
        AsgardTalk.end(player);
        f.everyTick(80, tick -> {
            HeimdallPost.tick(player, Powers.get(player));
            if (HeimdallPost.keeps(player)) {
                return false;
            }
            check(npc.restAt() == null, "the time is up: Heimdall takes his post again");
            return true;
        });
    }

    @GameTest(template = ARENA, batch = POST, timeoutTicks = 120)
    public static void thePostEndsWhenTheKeeperIsNoLongerHeimdall(GameTestHelper helper) {
        TestKit.Fixture f = AsgardTestSupport.fixture(helper);
        ServerPlayer player = inAsgard(f, helper, Power.HEIMDALL);
        f.onClose(() -> HeimdallPost.end(player, false));
        AsgardianEntity npc = cityHeimdall(f, player);
        ServerPlayer other = inAsgard(f, helper, Power.HEIMDALL);
        f.onClose(() -> HeimdallPost.end(other, false));
        f.everyTick(100, tick -> {
            if (HeimdallPost.keeper() != null && !HeimdallPost.keeps(player)) {
                return false; // (the other test of the batch holds the post: one keeper at a time)
            }
            if (!HeimdallPost.keeps(player)) {
                check(HeimdallPost.handOver(player, npc), "the post is his");
            }
            check(!HeimdallPost.handOver(other, npc) && !HeimdallPost.mayHandOver(other), "one keeper at a time");
            Powers.setPower(player, Power.NONE);
            HeimdallPost.tick(player, Powers.get(player));
            check(!HeimdallPost.keeps(player) && npc.restAt() == null, "no longer Heimdall: the post ends, Heimdall returns");
            check(HeimdallPost.mayHandOver(other), "and another Heimdall may take it");
            return true;
        });
    }

    static String picked(ServerPlayer player, AsgardianEntity npc) {
        AsgardDialogue.Node node = AsgardDialogue.pick(AsgardTalk.context(player, npc, npc.role()));
        return node == null ? null : node.key();
    }

    /** Whether a chat line naming this translation key (in any part of it) was sent to the player since the last look. */
    static boolean saidInChat(ServerPlayer player, String key) {
        for (Object packet : TestKit.sentPackets(player)) {
            if (packet instanceof ClientboundSystemChatPacket chat && !chat.overlay() && mentions(chat.content(), key)) {
                return true;
            }
        }
        return false;
    }

    static boolean mentions(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents text) {
            if (text.getKey().equals(key)) {
                return true;
            }
            for (Object arg : text.getArgs()) {
                if (arg instanceof Component inner && mentions(inner, key)) {
                    return true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (mentions(sibling, key)) {
                return true;
            }
        }
        return false;
    }
}
