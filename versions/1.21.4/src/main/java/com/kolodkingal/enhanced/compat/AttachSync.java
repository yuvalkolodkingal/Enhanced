package com.kolodkingal.enhanced.compat;

import com.kolodkingal.enhanced.Enhanced;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 1.21.4 only: NeoForge 21.4 has no attachment sync, so the mod sends its synced attachments itself, as NeoForge 21.1
 * did: the whole value after {@code sync} (to everyone who sees the entity and to it, or only to it), when someone
 * starts seeing the entity, and to a player when he joins, respawns or changes dimension.
 */
public final class AttachSync {
    private record Synced<T>(Supplier<AttachmentType<T>> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                             boolean selfOnly) {
    }

    /** In registration order, which is the same on both sides. */
    private static final List<Synced<?>> SYNCED = new ArrayList<>();

    /** One attachment's value for one entity: the index in {@link #SYNCED} and the value's bytes. */
    public record Payload(int entityId, int index, byte[] bytes) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Enhanced.id("attachment"));
        public static final StreamCodec<FriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Payload::entityId,
                ByteBufCodecs.VAR_INT, Payload::index,
                ByteBufCodecs.BYTE_ARRAY, Payload::bytes,
                Payload::new);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    private AttachSync() {
    }

    /** Declares a synced attachment (what the builder's {@code sync} was). */
    public static synchronized <T> void add(Supplier<AttachmentType<T>> type,
                                            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, boolean selfOnly) {
        SYNCED.add(new Synced<>(type, codec, selfOnly));
    }

    /** The payload's registration (mod bus). */
    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.STREAM_CODEC, AttachSync::receive);
    }

    private static int indexOf(AttachmentType<?> type) {
        for (int i = 0; i < SYNCED.size(); i++) {
            if (SYNCED.get(i).type().get() == type) {
                return i;
            }
        }
        return -1;
    }

    /** The id a payload carries for the level itself (no entity has it). */
    private static final int LEVEL = Integer.MIN_VALUE;

    @SuppressWarnings("unchecked")
    private static <T> Payload payload(net.neoforged.neoforge.attachment.IAttachmentHolder holder, net.minecraft.core.RegistryAccess registries,
                                       int id, int index) {
        Synced<T> synced = (Synced<T>) SYNCED.get(index);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            synced.codec().encode(buf, holder.getData(synced.type()));
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new Payload(id, index, bytes);
        } finally {
            buf.release();
        }
    }

    private static Payload payload(Entity holder, int index) {
        return payload(holder, holder.registryAccess(), holder.getId(), index);
    }

    private static Payload payload(net.minecraft.server.level.ServerLevel level, int index) {
        return payload(level, level.registryAccess(), LEVEL, index);
    }

    /** Whether {@code type} is a synced attachment (setting it sends it, as NeoForge 21.1 did). */
    public static boolean synced(AttachmentType<?> type) {
        return indexOf(type) >= 0;
    }

    /**
     * Sends a level's synced attachments (a dimension's Hexes) to a player in it: NeoForge 21.1 synced a level's as
     * it did an entity's, and the mod's own sync knew only entities, so from 1.21.4 a Hex never reached the players
     * (no walls, no red sky, no runes: only its Vision).
     */
    private static void sendLevel(net.minecraft.server.level.ServerLevel level, ServerPlayer to) {
        for (int i = 0; i < SYNCED.size(); i++) {
            if (level.hasData(SYNCED.get(i).type().get())) {
                PacketDistributor.sendToPlayer(to, payload(level, i));
            }
        }
    }

    /** Sends {@code type}'s value of {@code holder}, if it is a synced attachment of an entity on the server. */
    public static void send(Object holder, AttachmentType<?> type) {
        if (holder instanceof net.minecraft.server.level.ServerLevel level) {
            int index = indexOf(type);
            if (index >= 0) {
                PacketDistributor.sendToPlayersInDimension(level, payload(level, index));
            }
            return;
        }
        if (!(holder instanceof Entity entity) || entity.level().isClientSide()) {
            return;
        }
        int index = indexOf(type);
        if (index < 0) {
            return;
        }
        if (SYNCED.get(index).selfOnly()) {
            if (entity instanceof ServerPlayer p) {
                PacketDistributor.sendToPlayer(p, payload(entity, index));
            }
        } else {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload(entity, index));
        }
    }

    private static void sendAll(Entity holder, ServerPlayer to) {
        for (int i = 0; i < SYNCED.size(); i++) {
            Synced<?> synced = SYNCED.get(i);
            if ((!synced.selfOnly() || holder == to) && holder.hasData(synced.type().get())) {
                PacketDistributor.sendToPlayer(to, payload(holder, i));
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer to) {
            sendAll(event.getTarget(), to);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sendAll(p, p);
            sendLevel(p.serverLevel(), p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sendAll(p, p);
            sendLevel(p.serverLevel(), p);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sendAll(p, p);
            sendLevel(p.serverLevel(), p);
        }
    }

    private static void receive(Payload payload, IPayloadContext context) {
        context.enqueueWork(() -> Client.apply(payload));
    }

    /** Client classes only here. */
    private static final class Client {
        @SuppressWarnings("unchecked")
        static <T> void apply(Payload payload) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || payload.index() < 0 || payload.index() >= SYNCED.size()) {
                return;
            }
            Synced<T> synced = (Synced<T>) SYNCED.get(payload.index());
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.bytes()),
                    mc.level.registryAccess());
            if (payload.entityId() == LEVEL) {
                mc.level.setData(synced.type(), synced.codec().decode(buf));
                return;
            }
            Entity entity = mc.level.getEntity(payload.entityId());
            if (entity == null) {
                return;
            }
            entity.setData(synced.type(), synced.codec().decode(buf));
        }
    }
}
