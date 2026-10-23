package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.thanos.StoneKind;
import com.kolodkingal.enhanced.thanos.ThanosEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Time Stone (3.6), only for a sorcerer. Use, <b>Slow Time</b> (a switch): a green zone of slowed time round him,
 * {@code [time_stone] slowRadius}, moving with him; creatures in it tick every {@code slowFactor}-th tick, projectiles
 * hang, other players get heavy Slowness, and he is not slowed ({@link Live#TIME_SLOWING}, which {@code SlowMotion}
 * reads through {@link StoneZones}). It burns hunger and lasts at most {@code slowMaxTicks}; then its cooldown. Sneak +
 * use, the <b>Rewind</b>: Thanos's ({@link TimeRewind}, shared): back to where he was a few seconds ago, the health he
 * had then if it was higher, the harmful effects of the window undone. The history is kept for every player who
 * carries a Time Stone he may use (or the Eye). And the 2016 apple: on a crop, a sapling or a baby animal the stone
 * grows it one stage; with Sneak on a crop, un-grows it. The Eye of Agamotto (lane L3) and Doctor Strange's Y come
 * here through {@link StoneUse}. Server only.
 */
public final class TimeStonePowers {
    /** Hunger of a tick of Slow Time, and of one Rewind. */
    static final float SLOW_EXHAUSTION = 0.02F;
    static final float REWIND_EXHAUSTION = 2.0F;
    /** The most health a Rewind gives back. */
    static final float REWIND_MAX_HEAL = 10.0F;

    /** A Slow Time that runs: the use that started it (its cooldown) and since when. */
    private record Slow(StoneUse.Cast cast, long since) {
    }

    private static final Map<UUID, Slow> SLOWS = new HashMap<>();

    // ---- Slow Time ----

    /** Whether the player's Time Stone slows time round him now. */
    public static boolean slowing(Player player) {
        return SLOWS.containsKey(player.getUUID());
    }

    static void startSlow(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        PowerData data = cast.data();
        SLOWS.put(player.getUUID(), new Slow(cast, cast.now()));
        data.setToggle(Toggle.TIME_SLOW, true);
        data.set(Live.TIME_SLOWING, true);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TIME_SLOW_ON.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        StoneKind.TIME.voice().use(level, player.position(), 0.6F, 0.9F);
        ThanosEffects.mandala(level, player.position().add(0.0, 0.05, 0.0), 2.4F, 30, StoneKind.TIME.color());
        fx(player, StoneFxPayload.SLOW_ON);
        player.displayClientMessage(Component.translatable("message.enhanced.stones.time.slow_on"), true);
    }

    /**
     * Slow Time ends: by his key ({@code byHand}), its limit, hunger or the stone leaving him; its cooldown starts.
     */
    public static void stopSlow(ServerPlayer player, PowerData data, boolean byHand) {
        Slow slow = SLOWS.remove(player.getUUID());
        data.setToggle(Toggle.TIME_SLOW, false);
        data.set(Live.TIME_SLOWING, false);
        if (slow == null) {
            return;
        }
        slow.cast().startCooldown(StonesConfig.TIME_SLOW_COOLDOWN.get());
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TIME_SLOW_OFF.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        fx(player, StoneFxPayload.SLOW_OFF);
        if (byHand) {
            player.displayClientMessage(Component.translatable("message.enhanced.stones.time.slow_off"), true);
        }
    }

    static void tick(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        // The history of the Rewind, for whoever carries a Time Stone he may use (Thanos keeps his own).
        if (now % TimeRewind.SNAPSHOT_EVERY == 0 && !data.has(HeroAbility.THANOS_TIME)
                && StoneUse.carried(player, StoneKind.TIME)) {
            TimeRewind.remember(player, now, StonesConfig.TIME_REWIND_SECONDS.get() * 20L);
        }
        Slow slow = SLOWS.get(player.getUUID());
        if (slow == null) {
            if (data.toggle(Toggle.TIME_SLOW) || data.is(Live.TIME_SLOWING)) {
                // (a switch saved on from a session that is gone: nothing slows time any more)
                data.setToggle(Toggle.TIME_SLOW, false);
                data.set(Live.TIME_SLOWING, false);
            }
            return;
        }
        boolean lost = now % 10 == 0 && !StoneUse.carried(player, StoneKind.TIME);
        if (now - slow.since() >= StonesConfig.TIME_SLOW_MAX_TICKS.get() || player.isSpectator()
                || StoneUse.starving(player) || StoneUse.restrained(player) || lost) {
            stopSlow(player, data, false);
            return;
        }
        player.causeFoodExhaustion(SLOW_EXHAUSTION);
        edge(level, player, now);
    }

    /** The zone's edge: a few green motes drifting on the sphere round him. */
    private static void edge(ServerLevel level, ServerPlayer player, long now) {
        double radius = StoneZones.radius();
        EnergyParticleOptions mote = new EnergyParticleOptions(StoneKind.TIME.color(), 0.9F);
        for (int i = 0; i < 4; i++) {
            double a = (now * 0.11 + i * 1.7) % Mth.TWO_PI;
            double b = Math.acos(1.0 - 2.0 * ((now * 0.037 + i * 0.29) % 1.0));
            double x = player.getX() + radius * Math.sin(b) * Math.cos(a);
            double y = player.getY() + 1.0 + radius * Math.cos(b) * 0.6;
            double z = player.getZ() + radius * Math.sin(b) * Math.sin(a);
            level.sendParticles(mote, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // ---- the Rewind ----

    /** Back a few seconds: Thanos's Rewind, the stone's numbers. */
    static void rewind(StoneUse.Cast cast) {
        ServerPlayer player = cast.player();
        if (MjolnirPin.isPinned(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.mjolnir.pinned_cannot"), true);
            return;
        }
        if (TimeRewind.historyTicks(player) < TimeRewind.MIN_HISTORY) {
            player.displayClientMessage(Component.translatable("message.enhanced.thanos.nothing_to_rewind"), true);
            return;
        }
        Vec3 from = player.position();
        TimeRewind.rewind(player, cast.data(), new TimeRewind.Cost(HeroAbility.THANOS_TIME, 0, REWIND_EXHAUSTION,
                REWIND_MAX_HEAL, StoneKind.TIME.color()));
        cast.startCooldown(StonesConfig.TIME_REWIND_COOLDOWN.get());
        Net.sendToPlayersTrackingEntityAndSelf(player, new StoneFxPayload(StoneFxPayload.REWIND,
                player.getId(), List.of(from, player.position()), 0));
    }

    // ---- the 2016 apple ----

    /**
     * The Time Stone on a block: grows a crop or a sapling one stage, or with {@code ungrow} (Sneak) takes a crop back
     * one. False if the block is none of these (the use then goes on as the stone's own).
     */
    public static boolean cropMagic(ServerPlayer player, StoneUse.Source source, BlockPos pos, boolean ungrow) {
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        if (!isCropTarget(state, ungrow) || !StonesConfig.TIME_CROP_MAGIC.get()) {
            return false;
        }
        IntegerProperty age = ageOf(state);
        if (refused(player, source)) {
            return true;
        }
        if (!StoneCooldowns.ready(player, StoneKind.TIME, level.getGameTime())) {
            return true;
        }
        if (!BlockRights.vanillaRights(player, level, pos)) {
            return true;
        }
        boolean changed;
        if (age != null) {
            int now = state.getValue(age);
            int next = ungrow ? now - 1 : now + 1;
            changed = age.getPossibleValues().contains(next);
            if (changed) {
                level.setBlock(pos, state.setValue(age, next), 3);
            }
        } else {
            BonemealableBlock sapling = (BonemealableBlock) state.getBlock();
            changed = sapling.isValidBonemealTarget(level, pos, state);
            if (changed) {
                sapling.performBonemeal(level, level.random, pos, state);
            }
        }
        if (changed) {
            StoneCooldowns.start(player, StoneKind.TIME, StonesConfig.TIME_CROP_COOLDOWN.get());
            Vec3 at = Vec3.atBottomCenterOf(pos);
            ThanosEffects.mandala(level, at.add(0.0, 0.05, 0.0), 0.8F, 16, StoneKind.TIME.color());
            level.playSound(null, at.x, at.y, at.z, ModSounds.TIME_CROP.get(), SoundSource.PLAYERS, 1.0F,
                    ungrow ? 0.8F : 1.2F);
        }
        return true;
    }

    /** The Time Stone on a baby animal: it grows up at once. False if it is no baby (the use goes on). */
    public static boolean ageUp(ServerPlayer player, StoneUse.Source source, AgeableMob mob) {
        if (!mob.isBaby() || !StonesConfig.TIME_CROP_MAGIC.get()) {
            return false;
        }
        if (refused(player, source)) {
            return true;
        }
        ServerLevel level = player.serverLevel();
        if (!StoneCooldowns.ready(player, StoneKind.TIME, level.getGameTime())) {
            return true;
        }
        mob.setAge(0);
        StoneCooldowns.start(player, StoneKind.TIME, StonesConfig.TIME_CROP_COOLDOWN.get());
        ThanosEffects.mandala(level, mob.position().add(0.0, 0.05, 0.0), 1.0F, 16, StoneKind.TIME.color());
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), ModSounds.TIME_CROP.get(), SoundSource.PLAYERS, 1.0F,
                1.2F);
        return true;
    }

    /** Whether the stone may not do it (and he is told why). */
    private static boolean refused(ServerPlayer player, StoneUse.Source source) {
        Component refusal = StoneUse.refusal(player, StoneKind.TIME, source);
        if (refusal != null) {
            player.displayClientMessage(refusal, true);
            return true;
        }
        return StoneUse.restrained(player) || player.isSpectator();
    }

    /**
     * Whether the Time Stone works on this block: a crop with growth stages (and, growing, a sapling). Both sides ask
     * it (the client so that the use on the block is not also the stone's own use).
     */
    public static boolean isCropTarget(BlockState state, boolean ungrow) {
        return crop(state) && ageOf(state) != null || !ungrow && state.is(BlockTags.SAPLINGS)
                && state.getBlock() instanceof BonemealableBlock;
    }

    /** Whether the block is a crop that grows in stages (wheat, carrots, berries, nether wart, cocoa...). */
    private static boolean crop(BlockState state) {
        return state.is(BlockTags.CROPS) || state.is(Blocks.NETHER_WART) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.COCOA);
    }

    /** The block's growth stage ({@code age}), if it has one. */
    @Nullable
    private static IntegerProperty ageOf(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && "age".equals(property.getName())) {
                return integer;
            }
        }
        return null;
    }

    private static void fx(ServerPlayer player, int kind) {
        Net.sendToPlayersTrackingEntityAndSelf(player, new StoneFxPayload(kind, player.getId(),
                List.of(player.position()), (int) Math.round(StoneZones.radius())));
    }

    static void forget(ServerPlayer player, PowerData data) {
        if (SLOWS.remove(player.getUUID()) != null || data.toggle(Toggle.TIME_SLOW)) {
            data.setToggle(Toggle.TIME_SLOW, false);
            data.set(Live.TIME_SLOWING, false);
        }
    }

    static void clear() {
        SLOWS.clear();
    }

    private TimeStonePowers() {
    }
}
