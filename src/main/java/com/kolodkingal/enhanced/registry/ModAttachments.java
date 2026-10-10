package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.hex.HexData;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.road.RoadDoors;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Enhanced.MODID);

    /** A player's hero power. Saved, kept on death, synced to everyone who can see the player. */
    public static final Supplier<AttachmentType<PowerData>> POWER = ATTACHMENTS.register("power",
            () -> AttachmentType.builder(PowerData::new)
                    .serialize(PowerData.CODEC)
                    .copyOnDeath()
                    .sync(PowerData.STREAM_CODEC)
                    .build());

    /** All Hexes in a dimension. Saved with the level and synced to every player in it. */
    public static final Supplier<AttachmentType<HexData>> HEXES = ATTACHMENTS.register("hexes",
            () -> AttachmentType.builder(() -> HexData.EMPTY)
                    .serialize(HexData.CODEC)
                    .sync(HexData.STREAM_CODEC)
                    .build());

    /** Every Witches' Road door in a dimension, in the order they were placed. Saved with the level; only the server needs it. */
    public static final Supplier<AttachmentType<RoadDoors>> ROAD_DOORS = ATTACHMENTS.register("road_doors",
            () -> AttachmentType.builder(() -> RoadDoors.EMPTY)
                    .serialize(RoadDoors.CODEC)
                    .build());

    private ModAttachments() {
    }
}
