package com.kolodkingal.enhanced.hex;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
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
 * instant death, with the 5% survivors who are players becoming Photon.
 */
public final class HexManager {

    /** What happens to something crossing the wall. */
    public enum Verdict {
        /** Wanda allowed it (or it is Wanda, or a Creative/Spectator player). */
        PASS,
        /** It has powers (or is a boss): it bounces off. */
        BLOCK,
        /** It must survive the wall: 95% death, 5% Monica Rambeau effect. */
        GAMBLE
    }

    /** Hex id → entity UUID → was inside last tick. In memory only. */
    private static final Map<UUID, Map<UUID, Boolean>> SIDES = new HashMap<>();

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
        level.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 0.5F);
        level.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.6F, 0.6F);
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
                if (!uuid.equals(wanda.getUUID())) {
                    save(level, data(level).update(hex.id(), h -> h.withAllowed(uuid, value != 0)));
                }
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
    }

    /**
     * A teleport (Ender Pearl, Chorus Fruit, Enderman) that would cross a Hex wall.
     *
     * @return false if the teleport must be cancelled
     */
    public static boolean onTeleport(ServerLevel level, Entity entity, Vec3 target) {
        if (!(entity instanceof LivingEntity living)) {
            return true;
        }
        Vec3 from = bodyCenter(living);
        Vec3 to = target.add(0, living.getBbHeight() * 0.5, 0);
        for (Hex hex : data(level).hexes()) {
            boolean insideBefore = hex.contains(from);
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
        boolean dies = entity.getRandom().nextDouble() < EnhancedConfig.HEX_DEATH_CHANCE.get();
        Vec3 at = bodyCenter(entity);
        if (dies) {
            level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.color, 1.8F), at.x, at.y, at.z, 40,
                    entity.getBbWidth() * 0.6, entity.getBbHeight() * 0.5, entity.getBbWidth() * 0.6, 0.05);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 1.0F, 0.6F);
            entity.hurt(ModDamageTypes.source(level, ModDamageTypes.HEX_WALL), 1.0E6F);
            return false;
        }
        level.sendParticles(new EnergyParticleOptions(Power.PHOTON.glow, 2.0F), at.x, at.y, at.z, 80, 0.8, 1.0, 0.8, 0.2);
        if (entity instanceof ServerPlayer player && !Powers.power(player).hasPowers()) {
            Powers.awaken(player, Power.PHOTON);
            player.sendSystemMessage(Component.translatable("message.enhanced.hex.monica_effect").withStyle(ChatFormatting.AQUA));
        }
        return true;
    }

    /** Pushes something back to the side it came from, as if it walked into glass. */
    private static void bounce(ServerLevel level, Hex hex, LivingEntity entity, boolean wasInside) {
        Vec3 back = new Vec3(entity.xo, entity.yo, entity.zo);
        Vec3 radial = bodyCenter(entity).subtract(hex.center());
        Vec3 push = radial.lengthSqr() < 1.0E-6 ? Vec3.ZERO : radial.normalize().scale(wasInside ? -0.6 : 0.6);
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
    }

    private HexManager() {
    }
}
