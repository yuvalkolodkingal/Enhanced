package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.boss.BossData;
import com.kolodkingal.enhanced.build.BuildData;
import com.kolodkingal.enhanced.build.BuildView;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomState;
import com.kolodkingal.enhanced.hex.HexData;
import com.kolodkingal.enhanced.hulk.HulkState;
import com.kolodkingal.enhanced.power.BifrostData;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.road.RoadDoors;
import com.kolodkingal.enhanced.stones.StoneProgress;
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

    /**
     * 3.3: the hero a player made with the Hero Creator (its name, powers, keys, colours and skin), what {@code Power.CUSTOM}
     * is for him. Saved (as the JSON of its export), kept on death and kept when he becomes another hero (so the creator
     * can give it back), synced to everyone who can see him: their clients draw his skin and his colours.
     */
    public static final Supplier<AttachmentType<CustomHero>> CUSTOM_HERO = ATTACHMENTS.register("custom_hero",
            () -> AttachmentType.builder(() -> CustomHero.EMPTY)
                    .serialize(CustomHero.CODEC, hero -> !hero.isEmpty())
                    .copyOnDeath()
                    .sync(CustomHero.STREAM_CODEC)
                    .build());

    /**
     * 3.3: what a custom hero is doing right now (the powers switched on, stored energy, the Force Field). Not saved; synced
     * to everyone who can see him.
     */
    public static final Supplier<AttachmentType<CustomState>> CUSTOM_STATE = ATTACHMENTS.register("custom_state",
            () -> AttachmentType.builder(CustomState::new)
                    .sync(CustomState.STREAM_CODEC)
                    .build());

    /**
     * 3.5: a player's progress in Asgard (where he came from, the Watch won, Hela beaten, Gungnir taken, crowned, the
     * Asgardians he has talked to, his weapons' bonds). Saved, kept on death, synced to that player only.
     */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.asgard.AsgardProgress>> ASGARD = ATTACHMENTS.register("asgard",
            () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.asgard.AsgardProgress.EMPTY)
                    .serialize(com.kolodkingal.enhanced.asgard.AsgardProgress.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, com.kolodkingal.enhanced.asgard.AsgardProgress.STREAM_CODEC)
                    .build());

    /**
     * 3.6: a player's progress with the Infinity Stones (the site stones he took, the Stonekeeper, the sacrifice, the
     * Snap's cooldown). Saved, kept on death, synced to that player only.
     */
    public static final Supplier<AttachmentType<StoneProgress>> STONES = ATTACHMENTS.register("stones",
            () -> AttachmentType.builder(() -> StoneProgress.EMPTY)
                    .serialize(StoneProgress.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, StoneProgress.STREAM_CODEC)
                    .build());

    /**
     * 3.6: a sorcerer's Sling Ring: its places and cooldown (as the Book of the Vishanti's). Saved, kept on death,
     * synced to him.
     */
    public static final Supplier<AttachmentType<BifrostData>> SLING_RING = ATTACHMENTS.register("sling_ring",
            () -> AttachmentType.builder(() -> BifrostData.EMPTY)
                    .serialize(BifrostData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, BifrostData.STREAM_CODEC)
                    .build());

    /**
     * 3.6: the Hulk's rage, Banner's anger, the last Hulk look, the burned arm. Saved, kept on death, synced to him.
     */
    public static final Supplier<AttachmentType<HulkState>> HULK = ATTACHMENTS.register("hulk",
            () -> AttachmentType.builder(() -> HulkState.EMPTY)
                    .serialize(HulkState.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, HulkState.STREAM_CODEC)
                    .build());

    // ===== healing (3.7) =====
    /**
     * 3.7: a healer's state ({@link com.kolodkingal.enhanced.healing.HealingState}: when he rises, when he was last
     * hurt, the one-time tip). Saved, not kept on death, synced to everyone who sees him (lane L1).
     */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.healing.HealingState>> HEALING =
            ATTACHMENTS.register("healing",
                    () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.healing.HealingState.EMPTY)
                            .serialize(com.kolodkingal.enhanced.healing.HealingState.CODEC)
                            .sync(com.kolodkingal.enhanced.healing.HealingState.STREAM_CODEC)
                            .build());
    // ===== wolverine (3.7) =====
    /** 3.7: Wolverine's Rage (lane L2). Saved, not kept on death, synced to him. */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.wolverine.WolverineState>> WOLVERINE =
            ATTACHMENTS.register("wolverine",
                    () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.wolverine.WolverineState.EMPTY)
                            .serialize(com.kolodkingal.enhanced.wolverine.WolverineState.CODEC)
                            .sync((holder, to) -> holder == to,
                                    com.kolodkingal.enhanced.wolverine.WolverineState.STREAM_CODEC)
                            .build());
    // ===== deadpool (3.7) =====
    /** 3.7: Deadpool's magazine, reload and Dogpool (lane L3). Saved, not kept on death, synced to him. */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.deadpool.DeadpoolState>> DEADPOOL =
            ATTACHMENTS.register("deadpool",
                    () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.deadpool.DeadpoolState.EMPTY)
                            .serialize(com.kolodkingal.enhanced.deadpool.DeadpoolState.CODEC)
                            .sync((holder, to) -> holder == to,
                                    com.kolodkingal.enhanced.deadpool.DeadpoolState.STREAM_CODEC)
                            .build());
    // ===== cottage (3.7) =====
    /** 3.7: a player's progress at Agatha's Cottages (lane L6). Saved, kept on death, synced to him. */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.cottage.CottageProgress>> COTTAGE =
            ATTACHMENTS.register("cottage",
                    () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.cottage.CottageProgress.EMPTY)
                            .serialize(com.kolodkingal.enhanced.cottage.CottageProgress.CODEC)
                            .copyOnDeath()
                            .sync((holder, to) -> holder == to,
                                    com.kolodkingal.enhanced.cottage.CottageProgress.STREAM_CODEC)
                            .build());

    // ===== captain (3.8) =====
    /**
     * 3.8: Captain America's thrown shield and combo (lane L1). Saved, not kept on death, synced to everyone who sees
     * him (lane L1: the others draw his Vibranium Shield on his back by its flag).
     */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.captain.CaptainState>> CAPTAIN =
            ATTACHMENTS.register("captain",
                    () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.captain.CaptainState.EMPTY)
                            .serialize(com.kolodkingal.enhanced.captain.CaptainState.CODEC)
                            .sync(com.kolodkingal.enhanced.captain.CaptainState.STREAM_CODEC)
                            .build());
    // ===== red_hulk (3.8) =====
    /** 3.8: Red Hulk's anger (lane L2). Saved, not kept on death, synced to everyone who sees him (his heat glow). */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.redhulk.RedHulkState>> RED_HULK =
            ATTACHMENTS.register("red_hulk",
                    () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.redhulk.RedHulkState.EMPTY)
                            .serialize(com.kolodkingal.enhanced.redhulk.RedHulkState.CODEC)
                            .sync(com.kolodkingal.enhanced.redhulk.RedHulkState.STREAM_CODEC)
                            .build());
    // ===== iron_man (3.8) =====
    /**
     * 3.8: Iron Man's suit, energy and suit-up (lanes L3 and L4). Saved without the phase, kept on death (his chosen
     * suits), synced to everyone who sees him (the suit-up and a deployed nanotech suit are drawn from it).
     */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.ironman.IronManState>> IRON_MAN =
            ATTACHMENTS.register("iron_man",
                    () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.ironman.IronManState.EMPTY)
                            .serialize(com.kolodkingal.enhanced.ironman.IronManState.CODEC)
                            .copyOnDeath()
                            .sync(com.kolodkingal.enhanced.ironman.IronManState.STREAM_CODEC)
                            .build());
    // ===== falcon (3.8) =====
    /** 3.8: Falcon's Redwing (lane L5). Saved, not kept on death, synced to him. */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.falcon.FalconState>> FALCON =
            ATTACHMENTS.register("falcon",
                    () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.falcon.FalconState.EMPTY)
                            .serialize(com.kolodkingal.enhanced.falcon.FalconState.CODEC)
                            .sync((holder, to) -> holder == to,
                                    com.kolodkingal.enhanced.falcon.FalconState.STREAM_CODEC)
                            .build());
    // ===== eternity (3.8) =====
    /** 3.8: a player's journey to Eternity and his wish (lane L8). Saved, kept on death, synced to him. */
    public static final Supplier<AttachmentType<com.kolodkingal.enhanced.eternity.EternityProgress>> ETERNITY =
            ATTACHMENTS.register("eternity",
                    () -> AttachmentType.builder(() -> com.kolodkingal.enhanced.eternity.EternityProgress.EMPTY)
                            .serialize(com.kolodkingal.enhanced.eternity.EternityProgress.CODEC)
                            .copyOnDeath()
                            .sync((holder, to) -> holder == to,
                                    com.kolodkingal.enhanced.eternity.EternityProgress.STREAM_CODEC)
                            .build());

    private ModAttachments() {
    }
}
