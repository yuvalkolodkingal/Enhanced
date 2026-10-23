package com.kolodkingal.enhanced.platform.neoforge.test;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.attach.AttachKey;
import com.kolodkingal.enhanced.platform.neoforge.NeoAttach;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.platform.test.EnhancedTests;
import com.kolodkingal.enhanced.platform.test.TestHooks;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.SyncAttachmentsPayload;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The GameTests' hooks on NeoForge, and the registration of the r1 adapter ({@link EnhancedTests}). */
@EventBusSubscriber(modid = Enhanced.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NeoTestHooks implements TestHooks {
    private static final Type ENHANCED_TEST = Type.getType(EnhancedTest.class);

    /** Registers the mod's tests: the one generator of {@link EnhancedTests}. */
    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        event.register(EnhancedTests.class);
    }

    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public List<Class<?>> testClasses() {
        // The scan's order of the classes (as @GameTestHolder's registration had them before the port).
        Set<String> names = new LinkedHashSet<>();
        ModList.get().getAllScanData().stream()
                .map(ModFileScanData::getAnnotations)
                .flatMap(Set::stream)
                .filter(a -> ENHANCED_TEST.equals(a.annotationType()))
                .forEach(a -> names.add(a.clazz().getClassName()));
        List<Class<?>> classes = new ArrayList<>();
        for (String name : names) {
            try {
                classes.add(Class.forName(name, true, NeoTestHooks.class.getClassLoader()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return classes;
    }

    @Override
    public ServerPlayer mockPlayerWithConnection(ServerLevel level, GameProfile profile) {
        MinecraftServer server = level.getServer();
        CommonListenerCookie cookie = new CommonListenerCookie(profile, 0, ClientInformation.createDefault(), false,
                ConnectionType.NEOFORGE);
        ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // The fake client "supports" every channel, so synced attachments (powers, Hexes) can be sent to it.
        connection.channel().attr(ChannelAttributes.ADHOC_CHANNELS).set(new AcceptAll());
        ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    @Override
    public boolean isAttachmentSync(Object packet, AttachKey<?> key, int entityId) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Object sub : bundle.subPackets()) {
                if (isAttachmentSync(sub, key, entityId)) {
                    return true;
                }
            }
            return false;
        }
        return packet instanceof ClientboundCustomPayloadPacket custom
                && custom.payload() instanceof SyncAttachmentsPayload sync
                && sync.types().contains(NeoAttach.typeOf(key))
                && sync.target() instanceof SyncAttachmentsPayload.EntityTarget(int id) && id == entityId;
    }

    /** A channel set that contains every channel, so the fake client accepts every payload. */
    private static final class AcceptAll extends AbstractSet<ResourceLocation> {
        @Override
        public boolean contains(Object o) {
            return true;
        }

        @Override
        public boolean add(ResourceLocation resourceLocation) {
            return false;
        }

        @Override
        public Iterator<ResourceLocation> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    }
}
