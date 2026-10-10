package com.kolodkingal.enhanced.hex;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Wanda's Hex: creating and controlling it, and enforcing the price of crossing its wall.
 *
 * <p>Each tick every living thing near a Hex is checked against which side of the wall it was on last tick.
 * Crossing without Wanda's permission means: bounce off (anyone with powers, and bosses), or a 95% chance of
 * instant death (the survivors simply walk on).
 */
public final class HexManager {

    /** What happens to something crossing the wall. */
    public enum Verdict {
        /** Wanda allowed it (or it is Wanda, or a Creative/Spectator player). */
        PASS,
        /** It has powers (or is a boss): it bounces off. */
        BLOCK,
        /** It must survive the wall: 95% death (by default, see the config). */
        GAMBLE
    }

    /** Hex id → entity UUID → was inside last tick. In memory only. */
    private static final Map<UUID, Map<UUID, Boolean>> SIDES = new HashMap<>();
    /** Where an entity was before a random teleport (Chorus Fruit, Enderman); judged once it has landed. */
    private static final Map<UUID, Vec3> TELEPORTED_FROM = new HashMap<>();
    /** Most players Wanda can allow into one Hex. */
    public static final int MAX_ALLOWED = 256;

    // ------------------------------------------------------------------------------------------------------------
    // Data access

    public static HexData data(Level level) {
        return level.getData(ModAttachments.HEXES);
    }

    private static void save(ServerLevel level, HexData data) {
        level.setData(ModAttachments.HEXES, data);
    }

    public static Optional<Hex> ownedBy(Level level, UUID owner) {
        return data(level).byOwner(owner);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Wanda's controls

    /** Ability 5: raise the Hex, or (sneaking) dissolve it. With a Hex already up, the client opens the control screen. */
    public static void onHexKey(ServerPlayer wanda) {
        if (wanda.isShiftKeyDown()) {
            if (dissolveAllOwnedBy(wanda.server, wanda.getUUID())) {
                wanda.displayClientMessage(Component.translatable("message.enhanced.hex.dissolved"), true);
            } else {
                wanda.displayClientMessage(Component.translatable("message.enhanced.hex.none"), true);
            }
            return;
        }
        ServerLevel level = wanda.serverLevel();
        if (ownedBy(level, wanda.getUUID()).isPresent()) {
            return;
        }
        for (ServerLevel other : wanda.server.getAllLevels()) {
            if (other != level && ownedBy(other, wanda.getUUID()).isPresent()) {
                wanda.displayClientMessage(Component.translatable("message.enhanced.hex.elsewhere",
                        Component.literal(other.dimension().location().toString())), true);
                return;
            }
        }
        create(wanda);
    }

    public static Hex create(ServerPlayer wanda) {
        ServerLevel level = wanda.serverLevel();
        int radius = Mth.clamp(EnhancedConfig.HEX_DEFAULT_RADIUS.get(), EnhancedConfig.HEX_MIN_RADIUS.get(), EnhancedConfig.HEX_MAX_RADIUS.get());
        Hex hex = new Hex(UUID.randomUUID(), wanda.getUUID(), wanda.getGameProfile().getName(), wanda.position(), radius,
                Set.of(), false, level.getGameTime());
        save(level, data(level).with(hex));
        // Volume above 1 widens the range: everyone inside the new Hex hears it rise.
        level.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), ModSounds.HEX_RISE.get(), SoundSource.PLAYERS, Math.max(1.0F, radius / 16.0F), 1.0F);
        level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.color, 2.0F), wanda.getX(), wanda.getY() + 1, wanda.getZ(),
                150, 1.5, 1.5, 1.5, 0.4);
        wanda.displayClientMessage(Component.translatable("message.enhanced.hex.created"), true);
        return hex;
    }

    /** Applies a change from Wanda's Hex Control screen. Only the owner can change her Hex. */
    public static void handleAction(ServerPlayer wanda, HexAction action, Optional<UUID> target, int value) {
        if (!Powers.is(wanda, Power.SCARLET_WITCH)) {
            return;
        }
        ServerLevel level = wanda.serverLevel();
        Optional<Hex> found = ownedBy(level, wanda.getUUID());
        if (found.isEmpty()) {
            return;
        }
        Hex hex = found.get();
        switch (action) {
            case SET_PLAYER -> target.ifPresent(uuid -> {
                boolean allow = value != 0;
                // Only real, online players can be let in, and the list can't grow without bound.
                if (uuid.equals(wanda.getUUID()) || (allow && (wanda.server.getPlayerList().getPlayer(uuid) == null
                        || hex.allowed().size() >= MAX_ALLOWED))) {
                    return;
                }
                save(level, data(level).update(hex.id(), h -> h.withAllowed(uuid, allow)));
            });
            case SET_CREATURES -> save(level, data(level).update(hex.id(), h -> h.withAllowCreatures(value != 0)));
            case SET_RADIUS -> {
                int radius = Mth.clamp(value, EnhancedConfig.HEX_MIN_RADIUS.get(), EnhancedConfig.HEX_MAX_RADIUS.get());
                save(level, data(level).update(hex.id(), h -> h.withRadius(radius)));
                // Resizing moves the wall; nobody should "cross" just because the wall moved over them.
                SIDES.remove(hex.id());
            }
            case DISSOLVE -> dissolve(level, hex);
        }
    }

    public static void dissolve(ServerLevel level, Hex hex) {
        save(level, data(level).without(hex.id()));
        SIDES.remove(hex.id());
        Vec3 c = hex.center();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 3.0F, 0.5F);
    }

    /** @return true if any Hex was dissolved */
    public static boolean dissolveAllOwnedBy(MinecraftServer server, UUID owner) {
        boolean any = false;
        for (ServerLevel level : server.getAllLevels()) {
            Optional<Hex> hex;
            while ((hex = ownedBy(level, owner)).isPresent()) {
                dissolve(level, hex.get());
                any = true;
            }
        }
        return any;
    }

    public static int clear(ServerLevel level) {
        int n = data(level).hexes().size();
        for (Hex hex : data(level).hexes()) {
            SIDES.remove(hex.id());
        }
        save(level, HexData.EMPTY);
        return n;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The wall

    /** Called once per tick for every level. */
    public static void tickLevel(ServerLevel level) {
        HexData data = data(level);
        if (data.hexes().isEmpty()) {
            return;
        }
        for (Hex hex : data.hexes()) {
            Map<UUID, Boolean> sides = SIDES.computeIfAbsent(hex.id(), k -> new HashMap<>());
            Set<UUID> seen = new HashSet<>();
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, hex.bounds(6.0), LivingEntity::isAlive)) {
                UUID id = entity.getUUID();
                seen.add(id);
                boolean inside = hex.contains(bodyCenter(entity));
                Boolean before = sides.get(id);
                if (before == null) {
                    // First time we see it near this Hex: if it just teleported here, judge where it came from.
                    Vec3 from = TELEPORTED_FROM.get(id);
                    if (from != null) {
                        before = hex.contains(from);
                    }
                }
                if (before == null || before == inside) {
                    sides.put(id, inside);
                    continue;
                }
                // It crossed the wall this tick.
                Verdict verdict = judge(hex, entity);
                switch (verdict) {
                    case PASS -> sides.put(id, inside);
                    case BLOCK -> bounce(level, hex, entity, before);
                    case GAMBLE -> {
                        if (gamble(level, hex, entity)) {
                            sides.put(id, inside);
                        } else {
                            sides.remove(id);
                        }
                    }
                }
            }
            sides.keySet().retainAll(seen);
        }
        TELEPORTED_FROM.clear();
    }

    /**
     * A random teleport (Chorus Fruit, Enderman) is about to happen. Its final landing spot isn't known yet, so
     * remember where it started; the next wall check compares the two sides.
     */
    public static void rememberTeleportStart(LivingEntity entity) {
        TELEPORTED_FROM.put(entity.getUUID(), bodyCenter(entity));
        for (Map<UUID, Boolean> sides : SIDES.values()) {
            sides.remove(entity.getUUID());
        }
    }

    /** Forget which side of every wall a player was on (respawn, login, dimension change). */
    public static void forget(UUID entity) {
        for (Map<UUID, Boolean> sides : SIDES.values()) {
            sides.remove(entity);
        }
        TELEPORTED_FROM.remove(entity);
    }

    /**
     * A teleport whose exact target is known in advance (an Ender Pearl, a Vibrate, the Bifrost) that would cross a Hex
     * wall. {@code level} is where it lands: arriving from another dimension counts as coming from outside every Hex
     * there.
     *
     * @return false if the teleport must be cancelled
     */
    public static boolean onTeleport(ServerLevel level, Entity entity, Vec3 target) {
        if (!(entity instanceof LivingEntity living)) {
            return true;
        }
        // Whoever Mjolnir pins down teleports nowhere (this is where the mod's own teleports and Ender Pearls are judged).
        if (MjolnirPin.isPinned(living)) {
            if (living instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.enhanced.mjolnir.pinned_cannot"), true);
            }
            return false;
        }
        Vec3 from = living.level() == level ? bodyCenter(living) : null;
        Vec3 to = target.add(0, living.getBbHeight() * 0.5, 0);
        for (Hex hex : data(level).hexes()) {
            boolean insideBefore = from != null && hex.contains(from);
            boolean insideAfter = hex.contains(to);
            if (insideBefore == insideAfter) {
                continue;
            }
            Map<UUID, Boolean> sides = SIDES.computeIfAbsent(hex.id(), k -> new HashMap<>());
            switch (judge(hex, living)) {
                case PASS -> sides.put(living.getUUID(), insideAfter);
                case BLOCK -> {
                    if (living instanceof Player player) {
                        player.displayClientMessage(Component.translatable("message.enhanced.hex.repelled"), true);
                    }
                    return false;
                }
                case GAMBLE -> {
                    if (!gamble(level, hex, living)) {
                        return false;
                    }
                    sides.put(living.getUUID(), insideAfter);
                }
            }
        }
        return true;
    }

    public static Verdict judge(Hex hex, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (player.isCreative() || player.isSpectator() || hex.isAllowed(player.getUUID())) {
                return Verdict.PASS;
            }
            return Powers.power(player).hasPowers() ? Verdict.BLOCK : Verdict.GAMBLE;
        }
        if (hex.allowCreatures()) {
            return Verdict.PASS;
        }
        if (entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null && hex.isAllowed(pet.getOwnerUUID())) {
            return Verdict.PASS;
        }
        if (entity.getType().is(ModTags.HEX_RESISTANT)) {
            return Verdict.BLOCK;
        }
        return Verdict.GAMBLE;
    }

    /** @return true if the entity survived */
    private static boolean gamble(ServerLevel level, Hex hex, LivingEntity entity) {
        if (entity.getRandom().nextDouble() >= EnhancedConfig.HEX_DEATH_CHANCE.get()) {
            return true;
        }
        Vec3 at = bodyCenter(entity);
        level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.color, 1.8F), at.x, at.y, at.z, 40,
                entity.getBbWidth() * 0.6, entity.getBbHeight() * 0.5, entity.getBbWidth() * 0.6, 0.05);
        level.playSound(null, at.x, at.y, at.z, ModSounds.HEX_CROSS.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        entity.hurt(ModDamageTypes.source(level, ModDamageTypes.HEX_WALL), 1.0E6F);
        return false;
    }

    /** Pushes something back to the side it came from, as if it walked into glass. */
    private static void bounce(ServerLevel level, Hex hex, LivingEntity entity, boolean wasInside) {
        Vec3 back = new Vec3(entity.xo, entity.yo, entity.zo);
        Vec3 radial = bodyCenter(entity).subtract(hex.center());
        Vec3 push = radial.lengthSqr() < 1.0E-6 ? Vec3.ZERO : radial.normalize().scale(wasInside ? -0.6 : 0.6);
        // Get off the boat/minecart first, or the vehicle would carry the rider straight back across.
        entity.stopRiding();
        // A player's client moves them before the tick starts, so their last-tick position is usually already past
        // the wall. Then put them back just on their own side of it instead.
        double halfHeight = entity.getBbHeight() * 0.5;
        if (hex.contains(back.add(0, halfHeight, 0)) != wasInside && radial.lengthSqr() > 1.0E-6) {
            back = hex.center().add(radial.normalize().scale(wasInside ? hex.radius() - 1.0 : hex.radius() + 1.0)).subtract(0, halfHeight, 0);
        }
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(back.x, back.y, back.z, player.getYRot(), player.getXRot());
            player.displayClientMessage(Component.translatable("message.enhanced.hex.repelled"), true);
        } else {
            entity.teleportTo(back.x, back.y, back.z);
        }
        entity.setDeltaMovement(push.x, Math.max(0.2, push.y), push.z);
        entity.hurtMarked = true;
        level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.color, 1.5F), entity.getX(), entity.getY() + 1, entity.getZ(),
                20, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SHULKER_BULLET_HURT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    private static Vec3 bodyCenter(LivingEntity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
    }

    /** Wanda died: her Hex collapses. */
    public static void onOwnerDeath(ServerPlayer wanda) {
        dissolveAllOwnedBy(wanda.server, wanda.getUUID());
    }

    public static void clearMemory() {
        SIDES.clear();
        TELEPORTED_FROM.clear();
    }

    private HexManager() {
    }
}
