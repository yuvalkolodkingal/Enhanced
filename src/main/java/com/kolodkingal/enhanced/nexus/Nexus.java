package com.kolodkingal.enhanced.nexus;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.UltimateWanda;
import com.kolodkingal.enhanced.network.EternityFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * (3.19.2) The Nexus of All Realities, the place between the universes ({@code enhanced:nexus}): a deep, dark void of
 * black, dark purple and cosmic blue, with far nebulae and twinkling stars and no sun or moon ({@code NexusSkyEffects}),
 * where a floating island of black stone is lit by floating orbs of energy and glowing crystals ({@link NexusBuilder}).
 *
 * <p>Ultimate Wanda's Banishment ({@link #banish}) sends whom she whispers to here in a flash of red light. A banished player
 * cannot leave by any power (the Sling Ring, the Bifrost, a cloud, a command of the game's own teleports...): every way
 * out is refused ({@link #onTravel}), and whoever is found outside anyway (dead and born again at their spawn) is
 * brought straight back ({@link #tick}). Falling off the island brings one back onto it. The only way out is a red portal
 * of Ultimate Wanda's ({@code WandaPortals}): she opens one into the Nexus, another out of it, and whoever she leads
 * through is free again ({@link #leaveThrough}).
 */
public final class Nexus {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("nexus"));
    /** The island's top (y), and its middle (x = z = 0). */
    public static final int GROUND = 80;
    /** Wanda's red (the flash, the sparks of the banishment and of her portals). */
    public static final int RED = 0xE8102C;
    /** How far her whisper reaches (blocks). */
    public static final double BANISH_RANGE = 48.0;

    /** Banished players crossing her portal out right now (the refusal lets them by). */
    private static final Set<UUID> RELEASING = new HashSet<>();

    private Nexus() {
    }

    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(KEY);
    }

    public static boolean in(Entity entity) {
        return entity.level().dimension() == KEY;
    }

    /** Whether this player is banished to the Nexus (kept with the world). */
    public static boolean banished(Player player) {
        return player.getServer() != null && State.get(player.getServer()).banished.contains(player.getUUID());
    }

    /** Where one lands in the Nexus: on the island, a few blocks from its crystal. */
    public static Vec3 arrival(RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double r = 4.0 + random.nextDouble() * 6.0;
        return new Vec3(Math.cos(angle) * r + 0.5, GROUND + 1, Math.sin(angle) * r + 0.5);
    }

    /** The Nexus, its island built (the first time anyone comes). Null if the world has no Nexus. */
    @Nullable
    public static ServerLevel ready(MinecraftServer server) {
        ServerLevel level = level(server);
        if (level == null) {
            return null;
        }
        State state = State.get(server);
        if (state.built < NexusBuilder.VERSION) {
            NexusBuilder.build(level);
            state.built = NexusBuilder.VERSION;
            state.setDirty();
        }
        return level;
    }

    // ---- the Banishment ----

    /**
     * Ultimate Wanda whispers to {@code target}: a flash of red light, and it is in the Nexus. A player stays banished
     * until she leads him out. Returns whether it went.
     */
    public static boolean banish(ServerPlayer wanda, LivingEntity target) {
        if (target == wanda || !target.isAlive() || in(target) || target instanceof Player p && (p.isSpectator() || p.isCreative())) {
            return false;
        }
        ServerLevel nexus = ready(wanda.server);
        if (nexus == null) {
            wanda.displayClientMessage(Component.translatable("message.enhanced.nexus.no_world"), true);
            return false;
        }
        ServerLevel from = (ServerLevel) target.level();
        Vec3 left = target.position();
        flashAt(from, left.add(0, target.getBbHeight() * 0.5, 0));
        from.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.6F);
        Vec3 spot = arrival(target.getRandom());
        if (target instanceof ServerPlayer player) {
            State state = State.get(wanda.server);
            state.banished.add(player.getUUID());
            state.setDirty();
            Net.sendToPlayer(player, new EternityFxPayload(EternityFxPayload.FLASH, player.getId(), player.blockPosition(), RED));
            send(player, nexus, spot);
            player.displayClientMessage(Component.translatable("message.enhanced.nexus.banished", wanda.getDisplayName())
                    .withStyle(ChatFormatting.RED), false);
        } else {
            target.stopRiding();
            Entity arrived = target.changeDimension(new DimensionTransition(nexus, spot, Vec3.ZERO, target.getYRot(),
                    target.getXRot(), DimensionTransition.DO_NOTHING));
            if (arrived == null) {
                return false;
            }
        }
        flashAt(nexus, spot.add(0, 1, 0));
        wanda.displayClientMessage(Component.translatable("message.enhanced.nexus.sent", target.getDisplayName())
                .withStyle(s -> s.withColor(RED)), true);
        return true;
    }

    /** Takes a player into the Nexus at {@code spot}. */
    static void send(ServerPlayer player, ServerLevel nexus, Vec3 spot) {
        player.stopRiding();
        player.teleportTo(nexus, spot.x, spot.y, spot.z, player.getYRot(), 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false, true));
        nexus.playSound(null, spot.x, spot.y, spot.z, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.3F, 0.6F);
    }

    /** The red flash where someone is taken or lands. */
    static void flashAt(ServerLevel level, Vec3 at) {
        level.sendParticles(new EnergyParticleOptions(RED, 2.0F), at.x, at.y, at.z, 80, 0.6, 0.9, 0.6, 0.25);
        level.sendParticles(new EnergyParticleOptions(0xFFD0D6, 1.4F), at.x, at.y, at.z, 30, 0.3, 0.5, 0.3, 0.15);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.2F, 0.5F);
    }

    // ---- no way out ----

    /** Whether a banished player in the Nexus may not go to {@code dimension} now. */
    public static boolean holds(Entity entity, ResourceKey<Level> dimension) {
        return entity instanceof ServerPlayer player && dimension != KEY && in(player) && !RELEASING.contains(player.getUUID())
                && banished(player);
    }

    /** A banished player is about to leave the Nexus some way other than Wanda's portal: refused. */
    public static void refused(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.enhanced.nexus.held").withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    /**
     * Something crosses a pair of portals whose owner is {@code owner}, out of the Nexus: whether it may. Only a banished
     * player is asked; he may if the portal is an Ultimate Wanda's, and then he is free.
     */
    public static boolean leaveThrough(Entity entity, @Nullable UUID owner, ServerLevel to) {
        if (!(entity instanceof ServerPlayer player) || !in(player) || to.dimension() == KEY || !banished(player)) {
            return true;
        }
        Player wanda = null;
        if (owner != null) {
            for (ServerLevel level : player.server.getAllLevels()) {
                wanda = level.getPlayerByUUID(owner);
                if (wanda != null) {
                    break;
                }
            }
        }
        if (wanda == null || !UltimateWanda.is(wanda)) {
            refused(player);
            return false;
        }
        free(player);
        RELEASING.add(player.getUUID());
        return true;
    }

    /** After a crossing (whether it went or not). */
    public static void crossed(Entity entity) {
        RELEASING.remove(entity.getUUID());
    }

    /** The player is banished no more. */
    public static void free(ServerPlayer player) {
        State state = State.get(player.server);
        if (state.banished.remove(player.getUUID())) {
            state.setDirty();
            player.displayClientMessage(Component.translatable("message.enhanced.nexus.freed").withStyle(s -> s.withColor(RED)), false);
        }
    }

    /** Every tick of every player: the void of the Nexus gives back, and a banished player outside it is taken back. */
    public static void tick(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        if (in(player)) {
            if (player.getY() < player.level().getMinBuildHeight() + 8) {
                send(player, (ServerLevel) player.level(), arrival(player.getRandom()));
            }
            return;
        }
        if (player.tickCount % 5 == 0 && banished(player)) {
            ServerLevel nexus = ready(player.server);
            if (nexus != null) {
                Net.sendToPlayer(player, new EternityFxPayload(EternityFxPayload.FLASH, player.getId(), player.blockPosition(), RED));
                send(player, nexus, arrival(player.getRandom()));
                refused(player);
            }
        }
    }

    // ---- what is kept ----

    /** Who is banished, and which island the Nexus has (kept with the Overworld). */
    public static final class State extends SavedData {
        static final String NAME = "enhanced_nexus";
        final Set<UUID> banished = new LinkedHashSet<>();
        int built;

        public static SavedData.Factory<State> factory() {
            return new SavedData.Factory<>(State::new, State::load, null);
        }

        static State get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State state = new State();
            state.built = tag.getInt("built");
            for (String id : tag.getString("banished").split(",")) {
                if (!id.isBlank()) {
                    try {
                        state.banished.add(UUID.fromString(id.trim()));
                    } catch (IllegalArgumentException ignored) {
                        // (a damaged entry is dropped)
                    }
                }
            }
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt("built", built);
            StringBuilder ids = new StringBuilder();
            for (UUID id : banished) {
                if (ids.length() > 0) {
                    ids.append(',');
                }
                ids.append(id);
            }
            tag.putString("banished", ids.toString());
            return tag;
        }
    }

    /** Tests: is {@code player} banished, set or cleared directly. */
    public static void setBanished(ServerPlayer player, boolean banished) {
        State state = State.get(player.server);
        if (banished ? state.banished.add(player.getUUID()) : state.banished.remove(player.getUUID())) {
            state.setDirty();
        }
    }
}
