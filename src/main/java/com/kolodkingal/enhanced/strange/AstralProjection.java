package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.StrangeFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Astral Projection (Sneak + B, a switch), 3.6, as in the film of 2016: his body stays where he stood ({@link
 * AstralBodyEntity}, kneeling); he becomes the spirit. The spirit is unseen by creatures (they lose him), passes through
 * blocks and floats as Vision does when he phases ({@code VisionAbilities.isPhased} asks {@link #isAstral}: the same
 * physics, the same immunity, drawn see-through with a pale-blue glow by the client's {@code AstralLayer}), and cannot
 * hurt, use items, open containers or pick anything up ({@link StrangeEvents}). It goes at most {@code astralRange} from
 * the body (pulled back at the edge) for at most {@code astralSeconds}.
 *
 * <p>He returns (the key again, the time, or the body is harmed: "the body calls him back") to where the body kneels,
 * and takes half the harm the body took. A logout, a death, another dimension or the power lost return him first.
 * Refused inside a boss's arena.
 */
public final class AstralProjection {
    /** What reaches him of a harm to his body when it calls him back. */
    public static final float BODY_SHARE = 0.5F;
    /** The spirit's hunger each tick. */
    static final float EXHAUSTION = 0.01F;

    /** A spirit out of its body: the body, where it kneels, and since when. */
    record Spirit(UUID body, ResourceKey<Level> dimension, Vec3 at, float yaw, long since) {
    }

    private static final Map<UUID, Spirit> SPIRITS = new HashMap<>();

    public static int ticks() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.ASTRAL_SECONDS.get(), 30) * 20;
    }

    public static int range() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.ASTRAL_RANGE.get(), 64);
    }

    public static int cooldown() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.ASTRAL_COOLDOWN.get(), 300);
    }

    // ---- queries (both sides: the switch is synced) ----

    /** Whether this player's spirit is out of his body. */
    public static boolean isAstral(Player player) {
        PowerData data = Powers.get(player);
        return data.toggle(Toggle.STRANGE_ASTRAL) && data.has(HeroAbility.STRANGE_BANDS);
    }

    /** Whether this body is one a spirit is out of (server). */
    static boolean isBody(AstralBodyEntity body) {
        UUID owner = body.owner();
        Spirit spirit = owner == null ? null : SPIRITS.get(owner);
        return spirit != null && spirit.body().equals(body.getUUID());
    }

    /** The body of a spirit (server), or null. */
    @Nullable
    public static AstralBodyEntity bodyOf(ServerPlayer player) {
        Spirit spirit = SPIRITS.get(player.getUUID());
        if (spirit == null) {
            return null;
        }
        ServerLevel level = player.server.getLevel(spirit.dimension());
        Entity body = level == null ? null : level.getEntity(spirit.body());
        return body instanceof AstralBodyEntity astral ? astral : null;
    }

    /** The ticks his spirit has been out (server), or -1. */
    public static long age(ServerPlayer player, long now) {
        Spirit spirit = SPIRITS.get(player.getUUID());
        return spirit == null ? -1 : now - spirit.since();
    }

    // ---- the switch ----

    static void toggle(ServerPlayer player, PowerData data) {
        if (data.toggle(Toggle.STRANGE_ASTRAL) || SPIRITS.containsKey(player.getUUID())) {
            end(player, data, 0.0F, null);
        } else {
            stepOut(player, data);
        }
    }

    /** His spirit steps out: the body kneels where he stood, and he rises out of it. */
    static void stepOut(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.STRANGE_BANDS, now)) {
            return;
        }
        if (BossFights.inAnyArena(level, player.blockPosition())) {
            StrangeAbilities.say(player, "message.enhanced.strange.astral_arena");
            return;
        }
        if (StrangeAbilities.refuseHungry(player)) {
            return;
        }
        player.stopRiding();
        AstralBodyEntity body = ModEntities.ASTRAL_BODY.get().create(level);
        if (body == null) {
            return;
        }
        body.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        body.setYHeadRot(player.getYRot());
        body.setYBodyRot(player.getYRot());
        body.setOwner(player.getUUID());
        SPIRITS.put(player.getUUID(), new Spirit(body.getUUID(), level.dimension(), player.position(), player.getYRot(),
                now));
        level.addFreshEntity(body);
        data.setToggle(Toggle.STRANGE_ASTRAL, true);
        data.set(Live.STRANGE_ASTRAL, true);
        loseHim(player);
        if (player.mayFly()) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
        player.setDeltaMovement(player.getDeltaMovement().add(0, 0.25, 0));
        player.hurtMarked = true;
        StrangeAbilities.sound(player, ModSounds.STRANGE_ASTRAL_OUT.get(), 1.0F, 1.0F);
        StrangeSignals.near(level, player.position(), StrangeFxPayload.ASTRAL_OUT, player, List.of(body.position()), 0);
    }

    /** Creatures that hunted him lose him: the spirit is unseen. */
    static void loseHim(ServerPlayer player) {
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0),
                m -> m.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    /**
     * He returns to his body (the key, the time, the body harmed, a logout, a death, another dimension, the power lost):
     * set back where it kneels, the body gone, the switch off, the recharge started. {@code harm} is what the body took
     * (half of it reaches him, by {@code source}).
     */
    static void end(ServerPlayer player, PowerData data, float harm, @Nullable DamageSource source) {
        Spirit spirit = SPIRITS.remove(player.getUUID());
        boolean was = data.toggle(Toggle.STRANGE_ASTRAL) || data.is(Live.STRANGE_ASTRAL);
        data.setToggle(Toggle.STRANGE_ASTRAL, false);
        data.set(Live.STRANGE_ASTRAL, false);
        if (spirit == null) {
            return;
        }
        MinecraftServer server = player.server;
        ServerLevel there = server.getLevel(spirit.dimension());
        Entity body = there == null ? null : there.getEntity(spirit.body());
        Vec3 back = body != null ? body.position() : spirit.at();
        float yaw = body != null ? body.getYRot() : spirit.yaw();
        if (body != null) {
            body.discard();
        }
        if (there != null && player.isAlive()) {
            player.teleportTo(there, back.x, back.y, back.z, yaw, player.getXRot());
            player.resetFallDistance();
            // Back in the body that knelt on the ground: standing, not floating.
            if (player.getAbilities().flying && !player.isCreative() && !player.isSpectator()) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
        long now = player.level().getGameTime();
        if (was) {
            data.startCooldown(HeroAbility.STRANGE_BANDS, now, cooldown());
            StrangeAbilities.sound(player, ModSounds.STRANGE_ASTRAL_IN.get(), 1.0F, 1.0F);
            StrangeSignals.near(player.serverLevel(), back, StrangeFxPayload.ASTRAL_IN, player, List.of(back), 0);
        }
        if (harm > 0.0F && source != null && player.isAlive()) {
            player.invulnerableTime = 0;
            player.hurt(source, harm * BODY_SHARE);
        }
    }

    /** The body was harmed: it calls him back, with half the harm. */
    static void bodyHurt(AstralBodyEntity body, DamageSource source, float amount) {
        UUID owner = body.owner();
        ServerPlayer player = owner == null || body.getServer() == null ? null
                : body.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            body.discard();
            return;
        }
        end(player, Powers.get(player), amount, source);
    }

    // ---- the tick ----

    static void tick(ServerPlayer player, PowerData data, long now) {
        Spirit spirit = SPIRITS.get(player.getUUID());
        if (spirit == null) {
            if (data.toggle(Toggle.STRANGE_ASTRAL) || data.is(Live.STRANGE_ASTRAL)) {
                // (A switch left on with no body: a restart while he was out. He is where he was saved: back.)
                data.setToggle(Toggle.STRANGE_ASTRAL, false);
                data.set(Live.STRANGE_ASTRAL, false);
            }
            return;
        }
        if (!data.has(HeroAbility.STRANGE_BANDS) || !data.toggle(Toggle.STRANGE_ASTRAL)
                || player.level().dimension() != spirit.dimension()) {
            end(player, data, 0.0F, null);
            return;
        }
        AstralBodyEntity body = bodyOf(player);
        if (body == null || !body.isAlive()) {
            end(player, data, 0.0F, null);
            return;
        }
        if (now - spirit.since() >= ticks()) {
            StrangeAbilities.say(player, "message.enhanced.strange.astral_time");
            end(player, data, 0.0F, null);
            return;
        }
        double range = range();
        Vec3 from = body.position();
        Vec3 off = player.position().subtract(from);
        if (off.lengthSqr() > range * range) {
            // The cord holds: back to its edge.
            Vec3 edge = from.add(off.normalize().scale(range - 1.0));
            player.teleportTo(player.serverLevel(), edge.x, edge.y, edge.z, player.getYRot(), player.getXRot());
            StrangeAbilities.say(player, "message.enhanced.strange.astral_edge");
        }
        // The spirit floats: it never stands on (and so never sinks through) a floor it passes through. A client that
        // lands ends its flight; it is given back.
        if (player.mayFly() && !player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
        if (player.tickCount % 10 == 0) {
            loseHim(player);
        }
        StrangeAbilities.spend(player, data, EXHAUSTION);
    }

    static void clear() {
        SPIRITS.clear();
    }

    private AstralProjection() {
    }
}
