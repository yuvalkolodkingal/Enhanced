package com.kolodkingal.enhanced.hex;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Vision of the Hex. "When Wanda raises the Hex, Vision appears and attacks whoever she tells him to": a Hex brings
 * one {@link HexVisionEntity} to fight at her side while it stands. This keeps track of who has one, rebuilds him
 * after a while when he falls, and decides whom he may harm.
 *
 * <p>Wanda orders him with the Vision key (Z, "Vision, that one!") and the Hex Control screen; see {@link #point}.
 */
public final class HexVisions {
    /** How far (blocks) "Vision, that one!" reaches. */
    public static final double POINT_RANGE = 48.0;

    /** Wanda's UUID → the Vision her Hex raised, while he is in the world. */
    private static final Map<UUID, HexVisionEntity> BY_OWNER = new HashMap<>();
    /** Hex id → game time at which Wanda has rebuilt the Vision that fell. In memory only. */
    private static final Map<UUID, Long> REBUILD_AT = new HashMap<>();

    /** The Vision this Wanda's Hex raised, if he is standing. */
    @Nullable
    public static HexVisionEntity of(UUID owner) {
        HexVisionEntity vision = BY_OWNER.get(owner);
        return vision != null && !vision.isRemoved() ? vision : null;
    }

    /**
     * The Vision joined the world (called by the entity). There is one per Wanda: should a second one ever turn up, it
     * is not entered here, and goes by itself (see {@link HexVisionEntity#tick}).
     */
    public static void joined(HexVisionEntity vision) {
        UUID owner = vision.getOwnerUUID();
        if (owner != null) {
            BY_OWNER.merge(owner, vision, (old, fresh) -> old.isRemoved() ? fresh : old);
        }
    }

    /** The Vision left the world (called by the entity). */
    public static void left(HexVisionEntity vision) {
        UUID owner = vision.getOwnerUUID();
        if (owner != null) {
            BY_OWNER.remove(owner, vision);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Coming and going

    /** Wanda raised a Hex: Vision appears beside her. */
    public static void raise(ServerPlayer wanda, Hex hex) {
        REBUILD_AT.remove(hex.id());
        if (EnhancedConfig.HEX_VISION_ENABLED.get() && of(wanda.getUUID()) == null) {
            conjure(wanda, hex);
        }
    }

    /** The Hex came down: so does the Vision it raised, and nothing is left to rebuild. */
    public static void dissolved(ServerLevel level, Hex hex) {
        REBUILD_AT.remove(hex.id());
        HexVisionEntity vision = of(hex.owner());
        if (vision != null && hex.id().equals(vision.hexId())) {
            vision.vanish(true);
        }
    }

    /** Called once per tick for every level: brings Vision back where a Hex stands without him. */
    public static void tickLevel(ServerLevel level) {
        HexData data = HexManager.data(level);
        if (data.hexes().isEmpty() || !EnhancedConfig.HEX_VISION_ENABLED.get()) {
            return;
        }
        for (Hex hex : data.hexes()) {
            if (of(hex.owner()) != null) {
                continue;
            }
            ServerPlayer wanda = level.getServer().getPlayerList().getPlayer(hex.owner());
            // Wanda has to be here to build him; if she is away (or dead) the Hex waits.
            if (wanda == null || wanda.serverLevel() != level || !wanda.isAlive()) {
                continue;
            }
            Long at = REBUILD_AT.get(hex.id());
            if (at != null && level.getGameTime() < at) {
                continue;
            }
            boolean rebuilt = at != null;
            REBUILD_AT.remove(hex.id());
            conjure(wanda, hex);
            if (rebuilt) {
                wanda.displayClientMessage(Component.translatable("message.enhanced.hex_vision.rebuilt"), true);
            }
        }
    }

    /** The Vision fell: Wanda starts to rebuild him (unless the Hex is gone). */
    public static void fell(HexVisionEntity vision) {
        UUID hexId = vision.hexId();
        UUID owner = vision.getOwnerUUID();
        if (hexId == null || owner == null || !(vision.level() instanceof ServerLevel level) || HexManager.byId(level, hexId).isEmpty()) {
            return;
        }
        int ticks = EnhancedConfig.HEX_VISION_REBUILD_TICKS.get();
        REBUILD_AT.put(hexId, level.getGameTime() + ticks);
        ServerPlayer wanda = level.getServer().getPlayerList().getPlayer(owner);
        if (wanda != null) {
            wanda.displayClientMessage(Component.translatable("message.enhanced.hex_vision.fell", Mth.ceil(ticks / 20.0)), true);
        }
    }

    /** Wanda is gone from the world (logged out, or changed power): so is the Vision of her Hex. */
    public static void ownerGone(UUID owner) {
        HexVisionEntity vision = of(owner);
        if (vision != null) {
            vision.vanish(false);
        }
    }

    private static HexVisionEntity conjure(ServerPlayer wanda, Hex hex) {
        ServerLevel level = wanda.serverLevel();
        HexVisionEntity vision = new HexVisionEntity(ModEntities.HEX_VISION.get(), level);
        vision.bind(wanda.getUUID(), hex.id());
        Vec3 at = spawnSpot(wanda, hex, vision);
        vision.moveTo(at.x, at.y, at.z, wanda.getYRot(), 0.0F);
        vision.setYHeadRot(wanda.getYRot());
        vision.setYBodyRot(wanda.getYRot());
        level.addFreshEntity(vision);
        vision.materialize();
        return vision;
    }

    /** Beside her (right, then left, behind, above), inside the Hex and clear of blocks; or where she stands. */
    private static Vec3 spawnSpot(ServerPlayer wanda, Hex hex, HexVisionEntity vision) {
        float yaw = wanda.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3[] offsets = {right.scale(1.8).add(0, 0.9, 0), right.scale(-1.8).add(0, 0.9, 0), forward.scale(-2.0).add(0, 0.9, 0), new Vec3(0, 2.2, 0),
                right.scale(1.2).add(0, 1.0, 0)};
        for (Vec3 offset : offsets) {
            Vec3 spot = wanda.position().add(offset);
            if (hex.contains(spot.add(0, 0.9, 0)) && Teleports.fits(vision, spot)) {
                return spot;
            }
        }
        return wanda.position();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Orders

    /**
     * "Vision, that one!": he attacks the creature Wanda looks at. Looking at nothing calls him back to her side. He
     * won't harm Wanda's own: players she allowed into the Hex, their pets, or players at all while PvP is off.
     */
    public static void point(ServerPlayer wanda, Hex hex) {
        HexVisionEntity vision = of(wanda.getUUID());
        if (vision == null || vision.level() != wanda.level()) {
            wanda.displayClientMessage(Component.translatable("message.enhanced.hex_vision.absent"), true);
            return;
        }
        EntityHitResult hit = Raycasts.entity(wanda, POINT_RANGE, 0.3,
                e -> e instanceof LivingEntity living && living != vision && living.isAlive() && !living.isSpectator(), 1.0F);
        if (hit == null) {
            vision.standDown();
            wanda.displayClientMessage(Component.translatable("message.enhanced.hex_vision.recall"), true);
            return;
        }
        LivingEntity target = (LivingEntity) hit.getEntity();
        if (!mayHarm(wanda.server, hex, target)) {
            wanda.displayClientMessage(Component.translatable("message.enhanced.hex_vision.refuses", target.getDisplayName()), true);
            return;
        }
        vision.command(target);
        wanda.displayClientMessage(Component.translatable("message.enhanced.hex_vision.attack", target.getDisplayName()), true);
        // A mark where he is sent: a small ring of his gold above the target.
        ServerLevel level = wanda.serverLevel();
        level.sendParticles(new EnergyParticleOptions(Power.VISION.color, 1.4F), target.getX(), target.getY() + target.getBbHeight() + 0.4,
                target.getZ(), 10, 0.25, 0.1, 0.25, 0.02);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.VISION_PHASE.get(), SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Whom he may harm

    /**
     * Whether the Vision of this Hex may harm {@code target}: not Wanda, not another Vision, not a player she allowed
     * into the Hex or a Creative or Spectator one, not a pet of hers or of an allowed player, and no player at all
     * while the server has PvP off.
     */
    public static boolean mayHarm(MinecraftServer server, Hex hex, LivingEntity target) {
        if (!target.isAlive() || target instanceof HexVisionEntity || isTrusted(hex, target)) {
            return false;
        }
        if (target instanceof Player player) {
            return server.isPvpAllowed() && !player.isCreative() && !player.isSpectator();
        }
        return true;
    }

    /** Wanda, the players she allowed, and the pets of either: the Vision of the Hex is on their side. */
    public static boolean isTrusted(Hex hex, Entity entity) {
        if (entity instanceof Player player) {
            return hex.isAllowed(player.getUUID());
        }
        return entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null && hex.isAllowed(pet.getOwnerUUID());
    }

    /**
     * Something was hurt. If it was Wanda, her Vision goes for whoever did it; if she did the hurting, he joins in; if
     * it was he who was struck, he strikes back.
     */
    public static void onDamage(LivingEntity victim, DamageSource source) {
        if (BY_OWNER.isEmpty()) {
            return;
        }
        Entity attacker = source.getEntity();
        if (victim instanceof Player player) {
            HexVisionEntity vision = of(player.getUUID());
            if (vision != null && attacker instanceof LivingEntity enemy) {
                vision.defend(enemy);
            }
        }
        if (attacker instanceof Player player) {
            HexVisionEntity vision = of(player.getUUID());
            if (vision != null) {
                vision.join(victim);
            }
        }
        if (victim instanceof HexVisionEntity vision && attacker instanceof LivingEntity enemy) {
            vision.defend(enemy);
        }
    }

    public static void clear() {
        BY_OWNER.clear();
        REBUILD_AT.clear();
    }

    private HexVisions() {
    }
}
