package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossData;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.hex.HexData;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.road.RoadDoors;
import com.kolodkingal.enhanced.ultron.SokoviaRelocations;
import net.minecraft.nbt.CompoundTag;
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
     * A sorcerer's Book of the Vishanti (3.2): the places they saved with it (some of them bases, which carry a ward) and its
     * cooldown, the same record as the Bifrost's with a list of its own. Saved, kept on death, synced to that player only.
     */
    public static final Supplier<AttachmentType<BifrostData>> VISHANTI = ATTACHMENTS.register("vishanti",
            () -> AttachmentType.builder(() -> BifrostData.EMPTY)
                    .serialize(BifrostData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, BifrostData.STREAM_CODEC)
                    .build());

    /**
     * A builder's own Build Magic (3.1): Billy's magic meter, rank and practice, the pocket's id and size (saved), and what
     * the HUD shows of the builder's history and spells at work (not saved). Kept on death, synced to that player only (as the
     * Bifrost). A player saved before 3.1 has none and reads as {@link BuildData#EMPTY}: Billy at rank 1 with a full meter.
     * (As with the Bifrost, NeoForge's first sync of a player to someone who starts seeing him sends all his synced data
     * without asking whom it is for; every change after that reaches him alone.)
     */
    public static final Supplier<AttachmentType<BuildData>> BUILD_DATA = ATTACHMENTS.register("build_data",
            () -> AttachmentType.builder(() -> BuildData.EMPTY)
                    .serialize(BuildData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, BuildData.STREAM_CODEC)
                    .build());

    /**
     * Whether a player is in build mode, her spell and her box (3.1): what makes the Scarlet Witch's eyes glow, keeps
     * Billy's crown on, and draws a builder's box for the others. Not saved (build mode is off after a login); synced to
     * everyone who can see the player, as the power.
     */
    public static final Supplier<AttachmentType<BuildView>> BUILD_VIEW = ATTACHMENTS.register("build_view",
            () -> AttachmentType.builder(() -> BuildView.EMPTY)
                    .sync(BuildView.STREAM_CODEC)
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

    /**
     * What the Witches' Road put aside from a player's backpack while a trial gives him its own blocks (the Copy the Structure trial): saved
     * WITH the player, so that the stash and the inventory always reach the disk together (a stash in the world's save could outlive an
     * inventory that already has the things back, and give them twice). Kept on a clone, not synced.
     */
    public static final Supplier<AttachmentType<CompoundTag>> ROAD_STASH = ATTACHMENTS.register("road_stash",
            () -> AttachmentType.builder(() -> new CompoundTag())
                    .serialize(CompoundTag.CODEC, tag -> !tag.isEmpty())
                    .copyOnDeath()
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

    /**
     * The players who were in the sky on Sokovia when it went away while they were offline, to be set down on the
     * ground when they log in. Saved with the level, not synced; read and written on the Overworld only.
     */
    public static final Supplier<AttachmentType<SokoviaRelocations>> SOKOVIA_RELOCATIONS = ATTACHMENTS.register("sokovia_relocations",
            () -> AttachmentType.builder(() -> SokoviaRelocations.EMPTY)
                    .serialize(SokoviaRelocations.CODEC)
                    .build());

    private ModAttachments() {
    }
}
