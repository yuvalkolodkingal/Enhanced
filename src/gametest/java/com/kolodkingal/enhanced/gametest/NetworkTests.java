package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.network.AsgardTalkPayload;
import com.kolodkingal.enhanced.network.BifrostActionPayload;
import com.kolodkingal.enhanced.network.BuildCastPayload;
import com.kolodkingal.enhanced.network.HelloPayload;
import com.kolodkingal.enhanced.network.HeroCreatorPayload;
import com.kolodkingal.enhanced.network.ModNetwork;
import com.kolodkingal.enhanced.network.MusicRequestPayload;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The mod's messages as the port needs them (port P0, lane L8): the handshake a loader without NeoForge's negotiation
 * uses ({@link HelloPayload}), the caps every client → server message puts on what it reads (a server cannot trust a
 * client, and only NeoForge limits what a message may allocate), and the optional music channel.
 */
public final class NetworkTests {
    private static final String BATCH = "network";

    private NetworkTests() {
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theHelloCarriesBothVersionsAndRefusesAsNeoForgeDoes(GameTestHelper helper) {
        HelloPayload local = HelloPayload.local();
        check(local.protocol().equals(ModNetwork.PROTOCOL_VERSION) && local.music().equals(ModNetwork.MUSIC_VERSION),
                "this side says its two versions: " + local);
        check(HelloPayload.TYPE.id().toString().equals("enhanced:hello"), "the hello is enhanced:hello");
        ByteBuf buf = Unpooled.buffer();
        HelloPayload.STREAM_CODEC.encode(buf, local);
        check(HelloPayload.STREAM_CODEC.decode(buf).equals(local) && buf.readableBytes() == 0, "the hello reads back");

        HelloPayload.Verdict same = HelloPayload.check(local, local);
        check(same.accepted() && same.music(), "the same versions: in, with the music");
        HelloPayload.Verdict noMusic = HelloPayload.check(local, new HelloPayload(ModNetwork.PROTOCOL_VERSION, ""));
        check(noMusic.accepted() && !noMusic.music(), "a client without the music channel plays, without the music");
        HelloPayload.Verdict otherMusic = HelloPayload.check(local, new HelloPayload(ModNetwork.PROTOCOL_VERSION, "0"));
        check(otherMusic.accepted() && !otherMusic.music(), "another music version: plays, without the music");

        HelloPayload.Verdict older = HelloPayload.check(local, new HelloPayload("12", ModNetwork.MUSIC_VERSION));
        check(!older.accepted() && !older.music(), "another protocol version is refused");
        String reason = older.refusal().getString();
        check(reason.equals("Channel of mod \"Enhanced\" failed to connect: The client wants the payload to be version: 12, "
                + "but the server wants it to be version: " + ModNetwork.PROTOCOL_VERSION + "!"), "NeoForge's words: " + reason);
        HelloPayload.Verdict none = HelloPayload.check(local, null);
        check(!none.accepted() && none.refusal().getString().endsWith(
                "This channel is missing on the client side, but required on the server!"), "a client without the mod: "
                + none.refusal().getString());
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void clientMessagesRefuseWhatIsTooLong(GameTestHelper helper) {
        String tooLong = "x".repeat(33);
        refused("a hello's version over 32", HelloPayload.STREAM_CODEC, buf -> {
            ByteBufCodecs.STRING_UTF8.encode(buf, tooLong);
            ByteBufCodecs.STRING_UTF8.encode(buf, "1");
        });
        refused("a music request's hash over 64", MusicRequestPayload.STREAM_CODEC,
                buf -> ByteBufCodecs.STRING_UTF8.encode(buf, "a".repeat(65)));
        refused("an Asgardian answer over " + AsgardTalkPayload.MAX_KEY, AsgardTalkPayload.Choose.STREAM_CODEC, buf -> {
            ByteBufCodecs.VAR_INT.encode(buf, 1);
            ByteBufCodecs.STRING_UTF8.encode(buf, "o".repeat(AsgardTalkPayload.MAX_KEY + 1));
        });
        refused("a Bifrost place's name over 128", BifrostActionPayload.STREAM_CODEC, buf -> {
            ByteBufCodecs.VAR_INT.encode(buf, 0);
            ByteBufCodecs.VAR_INT.encode(buf, 0);
            ByteBufCodecs.STRING_UTF8.encode(buf, "n".repeat(129));
        });
        refused("a Hero Creator power over 32", HeroCreatorPayload.STREAM_CODEC, buf -> {
            ByteBufCodecs.VAR_INT.encode(buf, HeroCreatorPayload.CHOOSE);
            ByteBufCodecs.STRING_UTF8.encode(buf, "p".repeat(33));
        });
        refused("a hero with 41 powers", HeroCreatorPayload.STREAM_CODEC, buf -> {
            ByteBufCodecs.VAR_INT.encode(buf, HeroCreatorPayload.APPLY);
            ByteBufCodecs.STRING_UTF8.encode(buf, "");
            ByteBufCodecs.STRING_UTF8.encode(buf, "");
            ByteBufCodecs.VAR_INT.encode(buf, 1);           // the hero: its format,
            ByteBufCodecs.STRING_UTF8.encode(buf, "Hero");  // name,
            buf.writeInt(0xFFFFFF);                         // colour,
            buf.writeInt(0);                                // glow,
            ByteBufCodecs.STRING_UTF8.encode(buf, "");      // look,
            buf.writeBoolean(false);                        // slim arms,
            ByteBufCodecs.VAR_INT.encode(buf, 0);           // no skin,
            ByteBufCodecs.VAR_INT.encode(buf, 41);          // and too many powers
        });
        refused("a Build Magic cast with 9 seeds", BuildCastPayload.STREAM_CODEC, buf -> {
            ByteBufCodecs.VAR_INT.encode(buf, 1);           // the token,
            buf.writeByte(0);                               // the flags,
            buf.writeByte(0);                               // the spell,
            buf.writeByte(0);                               // its op, style and region,
            buf.writeByte(0);
            buf.writeByte(0);
            // its corners,
            BlockPos.STREAM_CODEC.encode(buf, BlockPos.ZERO);
            BlockPos.STREAM_CODEC.encode(buf, BlockPos.ZERO);
            ByteBufCodecs.VAR_INT.encode(buf, 9);           // and too many seeds
            for (int i = 0; i < 9; i++) {
                BlockPos.STREAM_CODEC.encode(buf, BlockPos.ZERO);
            }
        });
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void theMusicGoesOnlyToAClientWithItsChannel(GameTestHelper helper) {
        // The tests' fake client has every channel of the mod (TestHooks.mockPlayerWithConnection).
        ServerPlayer player = TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
        check(ModNetwork.hasMusic(player), "a client with the music channel hears the world's music");
        helper.succeed();
    }

    /** Reading what {@code write} wrote fails (a decoder exception, or the codec's own refusal). */
    private static void refused(String what, StreamCodec<ByteBuf, ?> codec, Consumer<ByteBuf> write) {
        ByteBuf buf = Unpooled.buffer();
        write.accept(buf);
        Object read;
        try {
            read = codec.decode(buf);
        } catch (RuntimeException expected) {
            return;
        }
        check(false, what + " is refused when read, not read as " + read);
    }
}
