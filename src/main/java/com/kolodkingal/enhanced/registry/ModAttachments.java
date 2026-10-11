package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossData;
import com.kolodkingal.enhanced.hex.HexData;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.road.RoadDoors;
import net.minecraft.world.item.ItemStack;
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

    /** A player's Bifrost (saved places, cooldown). Saved, kept on death, synced to that player only. */
    public static final Supplier<AttachmentType<BifrostData>> BIFROST = ATTACHMENTS.register("bifrost",
            () -> AttachmentType.builder(() -> BifrostData.EMPTY)
                    .serialize(BifrostData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, BifrostData.STREAM_CODEC)
                    .build());

    /**
     * The Mjolnir a player has shattered into fragments (Love and Thunder) while its pieces are in the air: saved with
     * the player, so the hammer is neither lost nor copied if the world closes before it is whole again. Not kept on
     * death (it falls where he dies instead).
     */
    public static final Supplier<AttachmentType<ItemStack>> SHATTERED_MJOLNIR = ATTACHMENTS.register("shattered_mjolnir",
            () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.OPTIONAL_CODEC)
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

    /**
     * What the boss fights keep between fights (rewards waiting to be handed over, when each boss may be called again).
     * Saved with the level, not synced; read and written on the Overworld only, so it is the same from every dimension.
     */
    public static final Supplier<AttachmentType<BossData>> BOSS_DATA = ATTACHMENTS.register("boss_data",
            () -> AttachmentType.builder(() -> BossData.EMPTY)
                    .serialize(BossData.CODEC)
                    .build());

    private ModAttachments() {
    }
}
