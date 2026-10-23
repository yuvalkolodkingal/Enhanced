package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.MindReadingOverlay;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server → client, to the Phoenix alone: what her Mind Reading found in the mind of whoever she looks at, for the panel
 * beside her crosshair. Every stack is a stripped copy (item, count, damage, enchantments, name: no container contents,
 * no book pages) and the whole payload stays under 64 KiB, because a clientbound payload over 1 MiB disconnects the
 * reader.
 *
 * @param entityId  the creature read
 * @param name      a player's name or a creature's custom name, else empty (the panel then names the species)
 * @param species   the id of its entity type ({@code minecraft:zombie}), which the client names in its own language
 * @param state     {@link #OK}, {@link #BLOCKED}, {@link #ILLUSION}, {@link #TOO_VAST} or {@link #PLAYERS_OFF}; for any
 *                  but {@link #OK} nothing below the state is filled in
 * @param mood      {@link #CALM}, {@link #ALERT}, {@link #HOSTILE} or {@link #AFRAID}
 * @param hostileTo the name of the creature it means harm (mood {@link #HOSTILE}), or empty
 * @param health    its health, and its largest health
 * @param food      a player's food level, and their experience level (else 0)
 * @param worn      the seven slots it wears, in the order main hand, off hand, head, chest, legs, feet, body (an empty
 *                  slot is an empty stack)
 * @param carried   a player's 36 inventory slots (hotbar first, empty slots kept) unless the server keeps packs
 *                  private, a creature's inventory (at most 8 stacks), an Enderman's block, else empty
 * @param extra     {@code "shaped:<form>"} for a Loki in another shape (the form is what the Loki's power data saves:
 *                  an entity type id, or {@code minecraft:player;<uuid>;<name>}), {@code "disguised"} for a Vision in
 *                  his human disguise, else empty
 */
public record MindReadPayload(int entityId, String name, String species, int state, int mood, String hostileTo, float health,
                              float maxHealth, int food, int level, List<ItemStack> worn, List<ItemStack> carried, String extra)
        implements CustomPacketPayload {
    public static final int OK = 0;
    public static final int BLOCKED = 1;
    public static final int ILLUSION = 2;
    public static final int TOO_VAST = 3;
    public static final int PLAYERS_OFF = 4;

    public static final int CALM = 0;
    public static final int ALERT = 1;
    public static final int HOSTILE = 2;
    public static final int AFRAID = 3;

    public static final Type<MindReadPayload> TYPE = new Type<>(Enhanced.id("mind_read"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACKS =
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(36));

    public static final StreamCodec<RegistryFriendlyByteBuf, MindReadPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.entityId);
                ByteBufCodecs.stringUtf8(256).encode(buf, p.name);
                ByteBufCodecs.stringUtf8(256).encode(buf, p.species);
                ByteBufCodecs.VAR_INT.encode(buf, p.state);
                ByteBufCodecs.VAR_INT.encode(buf, p.mood);
                ByteBufCodecs.stringUtf8(256).encode(buf, p.hostileTo);
                ByteBufCodecs.FLOAT.encode(buf, p.health);
                ByteBufCodecs.FLOAT.encode(buf, p.maxHealth);
                ByteBufCodecs.VAR_INT.encode(buf, p.food);
                ByteBufCodecs.VAR_INT.encode(buf, p.level);
                STACKS.encode(buf, p.worn);
                STACKS.encode(buf, p.carried);
                ByteBufCodecs.stringUtf8(256).encode(buf, p.extra);
            },
            buf -> new MindReadPayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.stringUtf8(256).decode(buf),
                    ByteBufCodecs.stringUtf8(256).decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.stringUtf8(256).decode(buf), ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), STACKS.decode(buf), STACKS.decode(buf),
                    ByteBufCodecs.stringUtf8(256).decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The panel beside her crosshair shows it. */
    static void handle(MindReadPayload payload, PacketContext context) {
        MindReadingOverlay.accept(payload);
    }
}
