package com.kolodkingal.enhanced.hex;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Wanda's Hex: creating and controlling it, and enforcing the price of crossing its wall.
 *
 * <p>Each tick every living thing near a Hex (and every player of the level, wherever) is checked against which side of the
 * wall it was on last tick. 3.3: only those Wanda lets in may enter (her list of players, their pets, creatures if she lets
 * creatures in; never a boss or a boss's minion): anyone else crossing in bounces off (anyone with powers, and bosses), or
 * faces a 95% chance of instant death and, surviving it, is thrown back out all the same. Going out, a creature without
 * powers that survives the wall walks on, as before. What a power does across the wall from outside is stopped by {@link
 * com.kolodkingal.enhanced.protect.Shelters}. Vision is the exception to permission: inside a standing Hex he can't
 * leave it (Wanda is in control, and in WandaVision he comes apart at the edge of Westview), whether or not she let him
 * in, until Wanda is badly hurt, gone from the world or dead (her death dissolves the Hex). White Vision is held the
 * same way but is simply thrown back: he doesn't come apart. The same goes for the Vision her Hex raised.
 */
public final class HexManager {

    /** What happens to something crossing the wall. */
    public enum Verdict {
        /** Wanda allowed it (or it is Wanda, or a Creative/Spectator player). */
        PASS,
        /** It has powers (or is a boss): it bounces off. */
        BLOCK,
        /** It must survive the wall: 95% death (by default, see the config). */
        GAMBLE,
        /**
         * It is inside and the Hex does not let it go (Vision): it comes apart a little and is thrown back inside (White
         * Vision is only thrown back).
         */
        HELD
    }

    /** How long (ticks) without pushing against the wall before Vision's next push counts as the first again. */
    private static final int STRAIN_RESET_TICKS = 60;
    /** The wall hurts Vision at most this often (ticks) however hard he leans on it: twice a second. */
    private static final int HOLD_DAMAGE_INTERVAL = 10;

    /** How hard Vision has been pushing against the wall. */
    private static final class Strain {
        /** Game time of his last throw back, and of the last one that hurt him; how many hurt in a row. */
        long lastPush = Long.MIN_VALUE / 2;
        long lastDamage = Long.MIN_VALUE / 2;
        int pushes;
    }

    /** Entity UUID → how hard it has been pushing. In memory only. */
    private static final Map<UUID, Strain> STRAIN = new HashMap<>();
    /** What a Vision flakes into at the wall: orange and yellow. */
    private static final int[] FLAKES = {0xFFB02E, 0xFF8A1E, 0xF5D77A};

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
        onHexKey(wanda, wanda.isShiftKeyDown());
    }

    /**
     * The Hex key with its Sneak variant said explicitly (3.4: a custom hero's binding of the Hex, {@code alt} = its Sneak
     * cell): raise the Hex, or ({@code dissolve}) dissolve it.
     */
    public static void onHexKey(ServerPlayer wanda, boolean dissolve) {
        if (dissolve) {
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
        // (3.5: a custom hero's Hex rises in the colour his pick chose.)
        Hex hex = new Hex(UUID.randomUUID(), wanda.getUUID(), wanda.getGameProfile().getName(), wanda.position(), radius,
                Set.of(), false, level.getGameTime(), false, false, 0L, AbilityColors.chosen(wanda, HeroAbility.WANDA_HEX));
        save(level, data(level).with(hex));
        // Volume above 1 widens the range: everyone inside the new Hex hears it rise.
        level.playSound(null, wanda.getX(), wanda.getY(), wanda.getZ(), ModSounds.HEX_RISE.get(), SoundSource.PLAYERS, Math.max(1.0F, radius / 16.0F), 1.0F);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.color(wanda, HeroAbility.WANDA_HEX, WandaAbilities.color(wanda)), 2.0F),
                wanda.getX(), wanda.getY() + 1, wanda.getZ(), 150, 1.5, 1.5, 1.5, 0.4);
        wanda.displayClientMessage(Component.translatable("message.enhanced.hex.created"), true);
        // "When Wanda raises the Hex, Vision appears": the one who will fight at her side while it stands.
        HexVisions.raise(wanda, hex);
        return hex;
    }

    public static Optional<Hex> byId(Level level, UUID id) {
        return data(level).byId(id);
    }

    /** Applies a change from Wanda's Hex Control screen. Only the owner can change her Hex. */
    public static void handleAction(ServerPlayer wanda, HexAction action, Optional<UUID> target, int value) {
        if (!Powers.get(wanda).has(HeroAbility.WANDA_HEX)) { // (3.4: or a custom hero with her Hex)
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
            case SET_VISION_STAYS -> {
                boolean stays = value != 0;
                save(level, data(level).update(hex.id(), h -> h.withVisionStays(stays)));
                wanda.displayClientMessage(Component.translatable(stays
                        ? "message.enhanced.hex_vision.stays" : "message.enhanced.hex_vision.follows"), true);
            }
            case VISION_TARGET -> HexVisions.point(wanda, hex);
            case SET_RUNES -> HexRunes.set(wanda, hex, value != 0);
        }
    }

    public static void dissolve(ServerLevel level, Hex hex) {
        save(level, data(level).without(hex.id()));
        SIDES.remove(hex.id());
        HexVisions.dissolved(level, hex);
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

    /**
     * 3.4: the Hex of a custom hero who no longer has the Hex ability (he was made into another hero without it) dissolves,
     * as the Scarlet Witch's does when she loses her power ({@code Powers.setPower}). An owner who is away keeps it, as she
     * does.
     *
     * @return whether any was dissolved
     */
    private static boolean dissolveUnowned(ServerLevel level, HexData data) {
        boolean any = false;
        for (Hex hex : data.hexes()) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(hex.owner());
            if (owner != null) {
                PowerData power = Powers.get(owner);
                if (power.power() == Power.CUSTOM && !power.has(HeroAbility.WANDA_HEX)) {
                    dissolve(level, hex);
                    any = true;
                }
            }
        }
        return any;
    }

    public static int clear(ServerLevel level) {
        int n = data(level).hexes().size();
        for (Hex hex : data(level).hexes()) {
            SIDES.remove(hex.id());
            HexVisions.dissolved(level, hex);
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
        if (dissolveUnowned(level, data)) {
            data = data(level);
        }
        for (Hex hex : data.hexes()) {
            Map<UUID, Boolean> sides = SIDES.computeIfAbsent(hex.id(), k -> new HashMap<>());
            Set<UUID> seen = new HashSet<>();
            List<LivingEntity> near = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, hex.bounds(6.0), LivingEntity::isAlive));
            // 3.3: every player of the level is watched wherever they are, so that however fast they fly (Quicksilver, Thor,
            // a lag spike, the Phoenix) the side they were on is known, and a jump clear past the edge of the box is still a
            // crossing (a Vision inside is brought back the same way).
            for (ServerPlayer player : level.players()) {
                if (player.isAlive() && !near.contains(player)) {
                    near.add(player);
                }
            }
            for (LivingEntity entity : near) {
                UUID id = entity.getUUID();
                seen.add(id);
                boolean inside = hex.contains(bodyCenter(entity));
                Boolean before = sides.get(id);
                if (before == null) {
                    // First time we see it near this Hex: if it just teleported here, judge where it came from; a creature
                    // that came so fast that last tick it was outside the box round the Hex is judged where it was then.
                    Vec3 from = TELEPORTED_FROM.get(id);
                    if (from != null) {
                        before = hex.contains(from);
                    } else if (!(entity instanceof Player) && entity.tickCount > 1) {
                        before = hex.contains(new Vec3(entity.xo, entity.yo + entity.getBbHeight() * 0.5, entity.zo));
                    }
                }
                if (before == null || before == inside) {
                    sides.put(id, inside);
                    continue;
                }
                // It crossed the wall this tick.
                Verdict verdict = judge(hex, entity, before);
                switch (verdict) {
                    case PASS -> sides.put(id, inside);
                    case BLOCK -> bounce(level, hex, entity, before);
                    case HELD -> hold(level, hex, entity, true);
                    case GAMBLE -> {
                        if (!gamble(level, hex, entity)) {
                            sides.remove(id);
                        } else if (inside) {
                            bounce(level, hex, entity, false); // 3.3: surviving the wall is not being let in
                        } else {
                            sides.put(id, false);
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
        STRAIN.remove(entity);
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
        // The arena of a boss fight keeps its fighters in.
        if (!BossFights.allowsTeleport(level, entity, target)) {
            BossFights.tellHeld(entity);
            return false;
        }
        // A player in a trial of the Witches' Road leaves it only the Road's way.
        if (!RoadTrials.allowsTeleport(entity, level, target)) {
            return false;
        }
        // Whoever Mjolnir pins down teleports nowhere (this is where the mod's own teleports and Ender Pearls are
        // judged).
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
            switch (judge(hex, living, insideBefore)) {
                case PASS -> sides.put(living.getUUID(), insideAfter);
                case BLOCK -> {
                    if (living instanceof Player player) {
                        player.displayClientMessage(Component.translatable("message.enhanced.hex.repelled"), true);
                    }
                    return false;
                }
                case HELD -> {
                    // No teleport takes Vision out of a Hex that holds him: he flakes where he stands, and stays.
                    if (!whiteVision(living)) {
                        comeApart(level, hex, living);
                    }
                    if (living instanceof Player player) {
                        player.displayClientMessage(Component.translatable("message.enhanced.hex.holds_vision"), true);
                    }
                    return false;
                }
                case GAMBLE -> {
                    if (!gamble(level, hex, living) || insideAfter) {
                        // 3.3: only those Wanda lets in come in; a survivor of the wall is not one of them.
                        if (living.isAlive() && living instanceof Player player) {
                            player.displayClientMessage(Component.translatable("message.enhanced.hex.repelled"), true);
                        }
                        return false;
                    }
                    sides.put(living.getUUID(), insideAfter);
                }
            }
        }
        return true;
    }

    /**
     * 3.3: something arrived in this level from elsewhere (a portal, a respawn at a bed or an anchor): where it stands inside a
     * Hex, the wall judges it as coming from outside. Let in, it is inside; else it is set down outside the wall (a hero, a boss,
     * a creature: the wall's price is not asked for a respawn or a portal; it is only kept out).
     */
    public static void arrive(ServerLevel level, LivingEntity entity) {
        Vec3 at = bodyCenter(entity);
        for (Hex hex : data(level).hexes()) {
            if (!hex.contains(at)) {
                continue;
            }
            Map<UUID, Boolean> sides = SIDES.computeIfAbsent(hex.id(), k -> new HashMap<>());
            if (judge(hex, entity, false) == Verdict.PASS) {
                sides.put(entity.getUUID(), true);
                continue;
            }
            setOutside(level, hex, entity);
            sides.put(entity.getUUID(), false);
            if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.enhanced.hex.repelled"), true);
            }
            return;
        }
    }

    /** Sets something down outside the wall, straight out from the middle of the Hex: on the ground there, or beside the wall. */
    private static void setOutside(ServerLevel level, Hex hex, LivingEntity entity) {
        Vec3 radial = bodyCenter(entity).subtract(hex.center());
        Vec3 flat = new Vec3(radial.x, 0, radial.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : flat.normalize();
        // Out past the wall's widest ring, so the spot is outside at any height.
        Vec3 column = hex.center().add(flat.scale(hex.radius() + 1.5));
        Vec3 spot = com.kolodkingal.enhanced.ability.Teleports.groundAt(entity, level, (int) Math.floor(column.x), (int) Math.floor(column.z));
        if (spot == null || hex.contains(spot.add(0, entity.getBbHeight() * 0.5, 0))) {
            spot = column.add(0, Math.max(0.0, radial.y), 0);
        }
        entity.stopRiding();
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        } else {
            entity.teleportTo(spot.x, spot.y, spot.z);
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.resetFallDistance();
        level.sendParticles(new EnergyParticleOptions(wallColor(hex), 1.5F), spot.x, spot.y + 1, spot.z, 20, 0.4, 0.6, 0.4, 0.05);
    }

    /** 3.5: the colour of a Hex's wall: its own raspberry red, or the colour a custom hero's pick chose. */
    public static int wallColor(Hex hex) {
        return hex.color() < 0 ? Power.SCARLET_WITCH.color : hex.color();
    }

    /** What the wall makes of something crossing it into the Hex. */
    public static Verdict judge(Hex hex, LivingEntity entity) {
        return judge(hex, entity, false);
    }

    /**
     * What the wall makes of something crossing it.
     *
     * @param leaving true if it is inside and going out: Vision, and the Vision of the Hex, are then held, whatever
     *                Wanda allowed (Creative and Spectator players and Wanda herself pass), unless Wanda can no longer
     *                hold him (see {@link #holdsVision})
     */
    public static Verdict judge(Hex hex, LivingEntity entity, boolean leaving) {
        if (entity instanceof Player player) {
            if (player.isCreative() || player.isSpectator() || player.getUUID().equals(hex.owner())) {
                return Verdict.PASS;
            }
            if (leaving && Powers.is(player, Power.VISION)) {
                // While Wanda holds the Hex he can't go; once she can't, he walks out, allowed in or not.
                return holdsVision(hex, player) ? Verdict.HELD : Verdict.PASS;
            }
            if (hex.isAllowed(player.getUUID())) {
                return Verdict.PASS;
            }
            return Powers.power(player).hasPowers() ? Verdict.BLOCK : Verdict.GAMBLE;
        }
        if (entity instanceof HexVisionEntity vision && hex.id().equals(vision.hexId())) {
            return leaving ? Verdict.HELD : Verdict.PASS;
        }
        if (entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null && hex.isAllowed(pet.getOwnerUUID())) {
            return Verdict.PASS;
        }
        if (entity.getType().is(ModTags.HEX_RESISTANT)) {
            return Verdict.BLOCK;
        }
        if (entity.getType().is(ModTags.BOSSES) || entity.getType().is(ModTags.BOSS_MINIONS) || BossFights.fightOf(entity) != null) {
            // 3.3: letting creatures in never lets in a boss or what a boss fight brings (Wanda cannot name them).
            return Verdict.GAMBLE;
        }
        if (hex.allowCreatures()) {
            return Verdict.PASS;
        }
        return Verdict.GAMBLE;
    }

    /**
     * Whether Wanda still holds Vision in her Hex: she must be in the world, alive, and above {@code hexHoldReleaseHealth}
     * of her health. Hurt her badly enough (or kill her: her death dissolves the Hex) and he can leave; and with her gone
     * from the world there is no one to hold him.
     */
    public static boolean holdsVision(Hex hex, Entity vision) {
        if (vision.getServer() == null) {
            return true;
        }
        ServerPlayer wanda = vision.getServer().getPlayerList().getPlayer(hex.owner());
        if (wanda == null || !wanda.isAlive()) {
            return false;
        }
        return wanda.getHealth() > wanda.getMaxHealth() * EnhancedConfig.HEX_HOLD_RELEASE_HEALTH.get();
    }

    /**
     * A telekinetic grip carried Vision (or the Vision of the Hex) to the wall: it lets go of him, and its holder is told the
     * Hex holds him in (D-M 3.7). The Vision of the Hex calls this too when his own tick finds him outside, since that tick
     * can come before the wall's check.
     *
     * @return whether a grip held him
     */
    public static boolean releaseCarried(LivingEntity entity) {
        ServerPlayer holder = Telekinesis.holderOf(entity);
        if (holder == null || !Telekinesis.releaseTarget(entity)) {
            return false;
        }
        holder.displayClientMessage(Component.translatable("message.enhanced.telekinesis.hex_holds",
                Component.translatable("power.enhanced.vision")), true);
        return true;
    }

    /** White Vision is held in a Hex like Vision, but the Hex doesn't take him apart: he is only thrown back. */
    private static boolean whiteVision(LivingEntity entity) {
        return entity instanceof Player player && Powers.is(player, Power.VISION) && "white_vision".equals(Powers.get(player).variant());
    }

    /** @return true if the entity survived */
    private static boolean gamble(ServerLevel level, Hex hex, LivingEntity entity) {
        if (entity.getRandom().nextDouble() >= EnhancedConfig.HEX_DEATH_CHANCE.get()) {
            return true;
        }
        Vec3 at = bodyCenter(entity);
        level.sendParticles(new EnergyParticleOptions(wallColor(hex), 1.8F), at.x, at.y, at.z, 40,
                entity.getBbWidth() * 0.6, entity.getBbHeight() * 0.5, entity.getBbWidth() * 0.6, 0.05);
        level.playSound(null, at.x, at.y, at.z, ModSounds.HEX_CROSS.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        entity.hurt(ModDamageTypes.source(level, ModDamageTypes.HEX_WALL), 1.0E6F);
        return false;
    }

    /** Pushes something back to the side it came from, as if it walked into glass. */
    private static void bounce(ServerLevel level, Hex hex, LivingEntity entity, boolean wasInside) {
        throwBack(hex, entity, wasInside);
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.enhanced.hex.repelled"), true);
        }
        level.sendParticles(new EnergyParticleOptions(wallColor(hex), 1.5F), entity.getX(), entity.getY() + 1, entity.getZ(),
                20, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SHULKER_BULLET_HURT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /**
     * The Hex holds Vision (or the Vision it raised): he is thrown back inside and comes apart a little where he met
     * the wall. Vision himself is told why, and takes a little damage that grows while he keeps pushing (see
     * {@link #holdDamage}); it never kills him. White Vision is only thrown back, like a hero walking into glass. A Vision
     * that a telekinetic grip carried to the wall is not to blame: the grip lets go of him ({@link
     * Telekinesis#releaseTarget}), its holder is told the Hex holds him in, and he is only thrown back inside, neither
     * coming apart nor bitten by the wall (D-M 3.7).
     */
    private static void hold(ServerLevel level, Hex hex, LivingEntity entity, boolean wasInside) {
        if (releaseCarried(entity)) {
            throwBack(hex, entity, wasInside);
            return;
        }
        if (whiteVision(entity)) {
            bounce(level, hex, entity, wasInside);
            ((ServerPlayer) entity).displayClientMessage(Component.translatable("message.enhanced.hex.holds_vision"), true);
            return;
        }
        throwBack(hex, entity, wasInside);
        comeApart(level, hex, entity);
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.enhanced.hex.holds_vision"), true);
            float damage = Math.min(holdDamage(level, player), player.getHealth() - 1.0F);
            if (damage > 0) {
                player.hurt(ModDamageTypes.source(level, ModDamageTypes.HEX_WALL), damage);
            }
        }
    }

    /**
     * What this throw back at the wall costs Vision: {@code hexHoldDamage} the first time, and that much more each time
     * he pushes again within three seconds, up to {@code hexHoldDamageMax}. However hard he leans on the wall it hurts
     * him at most twice a second.
     */
    private static float holdDamage(ServerLevel level, LivingEntity entity) {
        long now = level.getGameTime();
        Strain strain = STRAIN.computeIfAbsent(entity.getUUID(), k -> new Strain());
        boolean rested = now - strain.lastPush > STRAIN_RESET_TICKS;
        strain.lastPush = now;
        if (!rested && now - strain.lastDamage < HOLD_DAMAGE_INTERVAL) {
            return 0.0F;
        }
        strain.pushes = rested ? 1 : strain.pushes + 1;
        strain.lastDamage = now;
        return (float) Math.min(EnhancedConfig.HEX_HOLD_DAMAGE_MAX.get(), EnhancedConfig.HEX_HOLD_DAMAGE.get() * strain.pushes);
    }

    /**
     * Pieces of him flake off in orange and yellow and drift away towards the wall, with a crackle: Vision, or his
     * double, at the edge of Westview.
     */
    private static void comeApart(ServerLevel level, Hex hex, LivingEntity entity) {
        Vec3 at = bodyCenter(entity);
        RandomSource random = entity.getRandom();
        Vec3 radial = at.subtract(hex.center());
        Vec3 out = radial.lengthSqr() < 1.0E-6 ? Vec3.ZERO : radial.normalize();
        for (int i = 0; i < 56; i++) {
            double x = entity.getX() + (random.nextDouble() - 0.5) * entity.getBbWidth() * 1.4;
            double y = entity.getY() + random.nextDouble() * entity.getBbHeight();
            double z = entity.getZ() + (random.nextDouble() - 0.5) * entity.getBbWidth() * 1.4;
            double drift = 0.25 + random.nextDouble() * 0.5;
            // count 0: the "spread" is the particle's own velocity, so each flake drifts off on its own way
            level.sendParticles(new EnergyParticleOptions(FLAKES[i % FLAKES.length], 1.4F + random.nextFloat() * 1.0F), x, y, z, 0,
                    out.x * drift + random.nextGaussian() * 0.12, 0.15 + random.nextDouble() * 0.45, out.z * drift + random.nextGaussian() * 0.12, 0.1);
        }
        level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 10, entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.4,
                entity.getBbWidth() * 0.4, 0.03);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.PLAYERS, 1.2F, 1.3F);
        level.playSound(null, at.x, at.y, at.z, ModSounds.HEX_CROSS.get(), SoundSource.PLAYERS, 0.6F, 1.7F);
    }

    /** Sets something back on the side of the wall it came from. */
    private static void throwBack(Hex hex, LivingEntity entity, boolean wasInside) {
        Vec3 back = new Vec3(entity.xo, entity.yo, entity.zo);
        Vec3 radial = bodyCenter(entity).subtract(hex.center());
        Vec3 push = radial.lengthSqr() < 1.0E-6 ? Vec3.ZERO : radial.normalize().scale(wasInside ? -0.6 : 0.6);
        // Get off the boat/minecart first, or the vehicle would carry the rider straight back across.
        entity.stopRiding();
        // A player's client moves them before the tick starts, so their last-tick position is usually already past
        // the wall. Then put them back just on their own side of it instead.
        double halfHeight = entity.getBbHeight() * 0.5;
        if (hex.contains(back.add(0, halfHeight, 0)) != wasInside && radial.lengthSqr() > 1.0E-6) {
            back = besideWall(hex, radial, wasInside).subtract(0, halfHeight, 0);
        }
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(back.x, back.y, back.z, player.getYRot(), player.getXRot());
        } else {
            entity.teleportTo(back.x, back.y, back.z);
        }
        entity.setDeltaMovement(push.x, Math.max(0.2, push.y), push.z);
        entity.hurtMarked = true;
    }

    /**
     * The middle of the body one block off the wall on the side it came from, straight out from the middle of the Hex
     * and at the height the body is at. (A line from the middle of the Hex through the body would bring it a little
     * lower each time it is thrown back, so a hero leaning on the wall would sink into the floor.) Only right at the
     * top of the dome, where no such spot is left at that height, does it follow that line.
     *
     * @param radial from the middle of the Hex to the middle of the body
     */
    private static Vec3 besideWall(Hex hex, Vec3 radial, boolean wasInside) {
        double distance = wasInside ? hex.radius() - 1.0 : hex.radius() + 1.0;
        double across = distance * distance - radial.y * radial.y;
        double flat = Math.sqrt(radial.x * radial.x + radial.z * radial.z);
        if (across > 0.0 && flat > 1.0E-6) {
            double along = Math.sqrt(across) / flat;
            return hex.center().add(radial.x * along, radial.y, radial.z * along);
        }
        return hex.center().add(radial.normalize().scale(distance));
    }

    private static Vec3 bodyCenter(LivingEntity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
    }

    /** Wanda died: her Hex collapses. */
    public static void onOwnerDeath(ServerPlayer wanda) {
        dissolveAllOwnedBy(wanda.server, wanda.getUUID());
    }

    /**
     * Whether a Hex that stands round this entity keeps it from leaving the dimension (a portal): Vision inside a Hex
     * is held, wherever he tries to go.
     */
    public static boolean holdsInside(ServerLevel level, LivingEntity entity) {
        Vec3 at = bodyCenter(entity);
        for (Hex hex : data(level).hexes()) {
            if (hex.contains(at) && judge(hex, entity, true) == Verdict.HELD) {
                return true;
            }
        }
        return false;
    }

    public static void clearMemory() {
        SIDES.clear();
        TELEPORTED_FROM.clear();
        STRAIN.clear();
        HexVisions.clear();
    }

    private HexManager() {
    }
}
