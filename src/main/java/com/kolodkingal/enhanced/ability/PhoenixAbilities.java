package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.PhoenixConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.network.PhoenixBurstPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Jean Grey, the Phoenix: a flier and a mind. R reads the mind of whoever she looks at (Sneak: Mind Sense, a ping that
 * finds every mind round her through the walls), G flies, V is Wanda's telekinesis in violet, B takes over one creature
 * for a while (Sneak: the Psionic Field holds everyone round her still) and N, held, charges the Phoenix Burst: she
 * rises in flames and on release a wave of cosmic fire spreads in every direction. Telepathy is violet and the Force is
 * gold and orange, so a glance tells which she is using. She is not hurt by fire. (DESIGN section 13.)
 *
 * <p>This class holds the abilities and what they remember; {@link PhoenixServer} is the plug-in that routes the keys
 * and the lifecycle here, {@link MindReading} reads minds and {@link PhoenixBurst} spreads the waves.
 */
public final class PhoenixAbilities {
    // ---- the fire: the Force of the Phoenix, from the white-hot core out to the deep red ----
    public static final int FIRE_CORE = 0xFFF4C2;
    public static final int FIRE_GOLD = 0xFFD65A;
    public static final int FIRE_MAIN = 0xFF8A1F;
    public static final int FIRE_EMBER = 0xE2531A;
    public static final int FIRE_DEEP = 0x8C2410;
    // ---- the mind: telepathy, Mind Control, the Psionic Field, telekinesis ----
    public static final int PSY_CORE = 0xF4EEFF;
    public static final int PSY_MAIN = 0xB79CFF;
    public static final int PSY_BODY = 0x7C5CE0;
    public static final int PSY_FRINGE = 0x3F2B96;

    /** How long the casting pose of Mind Control lasts. */
    public static final int MIND_CAST_TICKS = 16;
    /** How long the arms-out pose of the Psionic Field lasts. */
    public static final int PSIONIC_POSE_TICKS = 14;
    /** The flames keep burning round her this long after the Burst. */
    public static final int BLAZE_AFTER_TICKS = 20;
    /** A host she lets go wakes confused for this long (2 s). */
    public static final int FREED_CONFUSED_TICKS = 40;
    /** The speed (blocks per tick) she rises at while she charges, and how high above the ground she hovers. */
    private static final double RISE_SPEED = 0.12;
    private static final double HOVER_HEIGHT = 1.5;
    /** The charge cuts her walking speed, and her flying speed while it hovers her, to 40 percent. */
    private static final double CHARGE_SLOW = -0.6;
    /** The flying speed {@link PowerTicker} gives everyone who is not a survival flier (the Creative default). */
    private static final float CREATIVE_FLYING_SPEED = 0.05F;
    private static final ResourceLocation CHARGE_SLOW_ID = Enhanced.id("phoenix_charge_slow");
    /** How far up a charging Phoenix looks for the ground below her. */
    private static final int GROUND_SEARCH = 6;
    /** The hosts' upkeep (and the steering of a host) happen this often. */
    private static final int STEER_PERIOD = 10;
    /** How far from Jean the creature that last hurt her is still her enemy, and how far a hostile is still near. */
    private static final double HURTER_RANGE = 24.0;
    private static final double NEARBY_HOSTILE_RANGE = 16.0;
    /** A host she has nothing to fight stays within this many blocks of her. */
    private static final double GUARD_DISTANCE = 4.0;
    /** She "last hit" a creature this recently (5 s) if it is the one her host goes for second. */
    private static final int LAST_HIT_TICKS = 100;

    /** What one Phoenix is doing that lasts (game times, and the charge). */
    private static final class State {
        long sensingUntil;
        long castingUntil;
        long psionicUntil;
        long blazeUntil;
        /** The game time N went down, or -1 when she is not charging. */
        long chargeStart = -1L;
        /** The charge made her fly (she was on the ground), so it puts her down again. */
        boolean chargeStartedFlight;
        /** The creature Mind Control took last (the stream of motes runs to it while she casts). */
        @Nullable
        UUID castTarget;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    /** Host → the Phoenix that took it. */
    private static final Map<UUID, UUID> HOST_OWNER = new HashMap<>();
    /** Phoenix → her hosts, the oldest first. */
    private static final Map<UUID, List<UUID>> HOSTS_OF = new HashMap<>();
    /** True while the Phoenix's own steering sets a host's target (a host takes no other). */
    private static boolean steering;

    static {
        // Her Burst and her Psionic Field spare the creatures she has taken over.
        AreaTargets.addAllyRule(PhoenixAbilities::isHostOf);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Queries (also the rules the tests and the events ask)

    /** Whether this has a mind to read, sense and take: a living creature that is not an armour stand, an illusion or a Creative or Spectator player. */
    public static boolean hasMind(Entity e) {
        return e instanceof LivingEntity living && living.isAlive() && !(e instanceof ArmorStand) && !(e instanceof LokiIllusionEntity)
                && !(e instanceof Player player && (player.isCreative() || player.isSpectator()));
    }

    /** Whether Magneto's helmet shields this creature's mind from every telepathy (not from telekinesis or fire). */
    public static boolean isShielded(Entity e) {
        return e instanceof LivingEntity living && MagnetoAbilities.wearsHelmet(living);
    }

    /** Whether the player is a Phoenix who may not be burned (the server option is on). */
    public static boolean isFireImmune(Entity e) {
        return PhoenixPower.isPhoenix(e) && PhoenixConfig.FIRE_IMMUNE.get();
    }

    /** Whether the player is charging a Burst (N is down and the charge has begun). */
    public static boolean isCharging(Player player) {
        PowerData data = Powers.get(player);
        return data.power() == Power.PHOENIX && data.is(Live.PHOENIX_CHARGING);
    }

    /** Whether this creature is possessed by a Phoenix right now. */
    public static boolean isHost(Entity e) {
        return e instanceof Mob mob && mob.hasEffect(ModEffects.POSSESSED);
    }

    /** Whether {@code target} is a creature {@code owner} has taken over. */
    public static boolean isHostOf(LivingEntity owner, LivingEntity target) {
        return owner.getUUID().equals(HOST_OWNER.get(target.getUUID())) && isHost(target);
    }

    /** The creatures this Phoenix has taken over and that are still hers, the oldest first. */
    public static List<Mob> hostsOf(ServerPlayer phoenix) {
        List<Mob> hosts = new ArrayList<>();
        for (UUID id : new ArrayList<>(HOSTS_OF.getOrDefault(phoenix.getUUID(), List.of()))) {
            if (find(phoenix.server, id) instanceof Mob mob && isHostOf(phoenix, mob)) {
                hosts.add(mob);
            }
        }
        return hosts;
    }

    /** Whether a creature belongs on her side: hers, her team's, or a pet that is hers. */
    public static boolean isHers(LivingEntity phoenix, Entity e) {
        return e == phoenix || phoenix.isAlliedTo(e) || (e instanceof OwnableEntity pet && phoenix.getUUID().equals(pet.getOwnerUUID()))
                || (e instanceof LivingEntity living && isHostOf(phoenix, living));
    }

    /** Whether the Phoenix may hold or take this player (PvP on, and she may harm them). */
    private static boolean mayReach(ServerPlayer phoenix, Player player) {
        return phoenix.server.isPvpAllowed() && phoenix.canHarmPlayer(player);
    }

    @Nullable
    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    private static State state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new State());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input

    public static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        if (ability == 5) {
            // N is held: both edges count (a release always goes through, so a charge never sticks).
            if (down) {
                startCharge(player, data);
            } else {
                releaseCharge(player, data);
            }
            return;
        }
        if (!down || player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (player.isShiftKeyDown()) {
                    mindSense(player, data);
                } else {
                    toggleMindReading(player, data);
                }
            }
            case 2 -> PowerTicker.toggleFlight(player);
            case 3 -> Telekinesis.press(player, Telekinesis.Mode.JEAN);
            case 4 -> {
                if (player.isShiftKeyDown()) {
                    psionicField(player, data);
                } else {
                    mindControl(player, data);
                }
            }
            default -> {
            }
        }
    }

    /** The Phoenix's own tick (alive): the fire, the timers, Mind Reading, the charge, the upkeep of her hosts. */
    public static void tick(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        State state = state(player);
        if (player.getRemainingFireTicks() > 0 && PhoenixConfig.FIRE_IMMUNE.get()) {
            player.clearFire();
        }
        // The poses and effects that last a moment.
        data.set(Live.MIND_SENSING, now < state.sensingUntil);
        data.set(Live.CASTING, now < state.castingUntil);
        data.set(Live.PHOENIX_PSIONIC, now < state.psionicUntil);
        if (state.chargeStart >= 0) {
            chargeTick(player, data, state, now);
        } else {
            data.set(Live.PHOENIX_CHARGING, false);
            data.set(Live.PHOENIX_BLAZE, now < state.blazeUntil);
        }
        if (now < state.castingUntil && now % 5 == 0 && state.castTarget != null
                && find(player.server, state.castTarget) instanceof LivingEntity host && host.isAlive()) {
            stream(level, player, host);
        }

        if (data.toggle(Toggle.MIND_READING)) {
            // Reading is not free: a whiff of hunger every tick (none in Creative).
            player.causeFoodExhaustion(PhoenixConfig.MIND_READ_EXHAUSTION.get().floatValue());
            MindReading.tick(player, data);
        } else {
            MindReading.stop(player);
        }
        if (now % STEER_PERIOD == 0) {
            upkeep(player);
        }
    }

    /**
     * Every player, whenever the passives are applied: a charging Phoenix is slow (60 percent off her walking speed and
     * off her flying speed, which is what she moves at while the charge hovers her); everyone else has none of it.
     *
     * <p>The walking speed is an attribute, but a flier does not move at it: her speed in the air is the flying speed of
     * her abilities, which {@link PowerTicker#applyPassives} sets before this runs and which only the client's own
     * movement obeys. So the charge sets it lower again, every tick, for as long as it lasts (the passives put it back
     * the tick after).
     */
    public static void applyBody(ServerPlayer player, PowerData data) {
        boolean charging = data.power() == Power.PHOENIX && data.is(Live.PHOENIX_CHARGING);
        PowerTicker.setModifier(player, Attributes.MOVEMENT_SPEED, CHARGE_SLOW_ID, CHARGE_SLOW, charging,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (charging) {
            Abilities abilities = player.getAbilities();
            float base = player.isCreative() || player.isSpectator() ? CREATIVE_FLYING_SPEED : Power.PHOENIX.flightSpeed();
            float slowed = base * (1.0F + (float) CHARGE_SLOW);
            if (Math.abs(abilities.getFlyingSpeed() - slowed) > 1.0E-4F) {
                abilities.setFlyingSpeed(slowed);
                player.onUpdateAbilities();
            }
        }
    }

    /** The power is being taken away: everything she left in the world ends. */
    public static void endAll(ServerPlayer player, PowerData data) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.chargeStart >= 0) {
            endCharge(player, data, state);
        }
        STATES.remove(player.getUUID());
        MindReading.stop(player);
        freeAll(player.server, player.getUUID(), false);
        data.setToggle(Toggle.MIND_READING, false);
        data.set(Live.PHOENIX_CHARGING, false);
        data.set(Live.PHOENIX_BLAZE, false);
        data.set(Live.MIND_SENSING, false);
        data.set(Live.PHOENIX_PSIONIC, false);
        data.set(Live.CASTING, false);
    }

    /** The key-down states of the Phoenix end (her body is dead, or her power gone): the charge and its flames. */
    public static void stopHold(PowerData data) {
        data.set(Live.PHOENIX_CHARGING, false);
        data.set(Live.PHOENIX_BLAZE, false);
    }

    /** The Phoenix left the server, died or changed dimension: her hosts are freed and a charge ends unfired. */
    public static void onGone(ServerPlayer player, boolean forgetAll) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.chargeStart >= 0) {
            endCharge(player, Powers.get(player), state);
        }
        freeAll(player.server, player.getUUID(), false);
        if (forgetAll) {
            STATES.remove(player.getUUID());
            MindReading.stop(player);
        }
    }

    /** Forget everything (the server stopped). */
    public static void clear() {
        STATES.clear();
        HOST_OWNER.clear();
        HOSTS_OF.clear();
        steering = false;
        MindReading.clear();
        PhoenixBurst.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 1: Mind Reading (R) and Mind Sense (Sneak + R)

    private static void toggleMindReading(ServerPlayer player, PowerData data) {
        boolean on = !data.toggle(Toggle.MIND_READING);
        data.setToggle(Toggle.MIND_READING, on);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.mind_reading.on" : "message.enhanced.mind_reading.off"), true);
        player.playNotifySound(ModSounds.MIND_READ.get(), SoundSource.PLAYERS, 0.6F, on ? 1.0F : 0.8F);
        if (!on) {
            MindReading.stop(player);
        }
    }

    /**
     * A ping like a small Cerebro: for {@link PhoenixConfig#MIND_SENSE_TICKS} ticks every mind round her glows through
     * the walls on her screen (the client does that; the server says how long and tells her what it found).
     */
    private static void mindSense(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(1, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind_sense.recovering", seconds(data.cooldownUntil(1) - now)), true);
            return;
        }
        data.startCooldown(1, now, PhoenixConfig.MIND_SENSE_COOLDOWN.get());
        player.causeFoodExhaustion(PhoenixConfig.MIND_SENSE_EXHAUSTION.get().floatValue());
        state(player).sensingUntil = now + PhoenixConfig.MIND_SENSE_TICKS.get();
        data.set(Live.MIND_SENSING, true);

        MindReading.Sensed sensed = MindReading.sense(player, PhoenixConfig.MIND_SENSE_RANGE.get());
        Component found = Component.translatable("message.enhanced.mind_sense.result", sensed.minds().size(), sensed.players(), sensed.hostile());
        if (sensed.silences() > 0) {
            found = found.copy().append(" ").append(Component.translatable("message.enhanced.mind_sense.blank", sensed.silences()));
        }
        player.displayClientMessage(found, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MIND_SENSE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Seconds as the messages write them: one decimal. */
    private static String seconds(long ticks) {
        return String.format(Locale.ROOT, "%.1f", Math.max(0L, ticks) / 20.0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 4: Mind Control (B)

    /**
     * Takes over the creature she looks at. {@link PhoenixPower#touched} first: even a mind that will refuse her (a
     * boss's) hears of the attempt. A player cannot be driven, only held for a moment, and only with PvP on.
     */
    private static void mindControl(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(4, now)) {
            return;
        }
        EntityHitResult hit = Raycasts.entity(player, PhoenixConfig.MIND_CONTROL_RANGE.get(), 0.3,
                e -> e instanceof LivingEntity living && living != player && living.isAlive() && !living.isSpectator()
                        && !(living instanceof ArmorStand), 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.mind_control.nothing"), true);
            return;
        }
        LivingEntity target = (LivingEntity) hit.getEntity();
        if (Shelters.stops(player, player.getEyePosition(), target, 0.0F)) {
            // 3.3: Billy's Hex Barrier faces her (a Hex's wall was already in the way of her look)
            player.displayClientMessage(Component.translatable("message.enhanced.shelter.power_stopped"), true);
            return;
        }
        PhoenixPower.touched(player, target, PhoenixPower.Kind.MIND_CONTROL, 0.0F);

        String refusal = controlRefusal(player, target);
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal), true);
            return;
        }
        if (target instanceof ServerPlayer other) {
            // Their controls are theirs: all she can do is hold them still while their mind goes blank.
            if (!other.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, PhoenixConfig.MIND_CONTROL_PLAYER_TICKS.get(), 0, false, false, true), player)) {
                player.displayClientMessage(Component.translatable("message.enhanced.mind_control.cannot"), true);
                return;
            }
        } else if (target instanceof Mob mob) {
            if (!possess(player, mob)) {
                player.displayClientMessage(Component.translatable("message.enhanced.mind_control.cannot"), true);
                return;
            }
        } else {
            player.displayClientMessage(Component.translatable("message.enhanced.mind_control.cannot"), true);
            return;
        }
        data.startCooldown(4, now, PhoenixConfig.MIND_CONTROL_COOLDOWN.get());
        player.causeFoodExhaustion(PhoenixConfig.MIND_CONTROL_EXHAUSTION.get().floatValue());
        State state = state(player);
        state.castingUntil = now + MIND_CAST_TICKS;
        state.castTarget = target.getUUID();
        data.set(Live.CASTING, true);
        player.displayClientMessage(Component.translatable("message.enhanced.mind_control.host", target.getName()), true);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.MIND_CONTROL.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        level.sendParticles(new EnergyParticleOptions(PSY_MAIN, 1.0F), target.getX(), target.getEyeY() + 0.3, target.getZ(), 12, 0.3, 0.2, 0.3, 0.03);
        stream(level, player, target);
    }

    /** The lang key of why this mind is closed to her, or null if it is open. */
    @Nullable
    private static String controlRefusal(ServerPlayer player, LivingEntity target) {
        if (target instanceof Player other && (other.isCreative() || other.isSpectator())) {
            return "message.enhanced.mind_control.cannot";
        }
        if (target instanceof LokiIllusionEntity || target.getType().is(ModTags.TELEPATHY_IMMUNE) || isShielded(target)) {
            return "message.enhanced.mind_control.cannot";
        }
        if (MjolnirPin.isPinned(target)) {
            return "message.enhanced.mind_control.pinned";
        }
        if (target instanceof Player other) {
            if (!mayReach(player, other)) {
                return "message.enhanced.mind_control.no_pvp";
            }
            return PhoenixConfig.MIND_CONTROL_PLAYER_TICKS.get() > 0 ? null : "message.enhanced.mind_control.cannot";
        }
        if (target instanceof OwnableEntity pet && pet.getOwnerUUID() != null && !player.getUUID().equals(pet.getOwnerUUID())
                && !player.server.isPvpAllowed()) {
            // Nobody turns a stranger's wolf on its owner with PvP off.
            return "message.enhanced.mind_control.cannot";
        }
        return null;
    }

    /**
     * Takes the creature over for {@link PhoenixConfig#MIND_CONTROL_TICKS} ticks. Past {@link PhoenixConfig#MIND_CONTROL_MAX}
     * hosts she lets go of the oldest, who wakes confused. A freed mind is hers: she clears the spells that would stop
     * it taking her orders.
     *
     * @return false if the effect would not take
     */
    static boolean possess(ServerPlayer phoenix, Mob mob) {
        mob.removeEffect(ModEffects.SPELLBOUND);
        mob.removeEffect(ModEffects.CONFUSED);
        List<UUID> mine = HOSTS_OF.computeIfAbsent(phoenix.getUUID(), id -> new ArrayList<>());
        mine.remove(mob.getUUID());
        int max = PhoenixConfig.MIND_CONTROL_MAX.get();
        while (mine.size() >= max) {
            UUID oldest = mine.remove(0);
            HOST_OWNER.remove(oldest);
            if (find(phoenix.server, oldest) instanceof Mob old) {
                release(old, true);
            }
        }
        // Registered first: the effect's own tick looks for its owner.
        mine.add(mob.getUUID());
        HOST_OWNER.put(mob.getUUID(), phoenix.getUUID());
        if (!mob.addEffect(new MobEffectInstance(ModEffects.POSSESSED, PhoenixConfig.MIND_CONTROL_TICKS.get(), 0, false, true, true), phoenix)) {
            mine.remove(mob.getUUID());
            HOST_OWNER.remove(mob.getUUID());
            return false;
        }
        mob.getNavigation().stop();
        steer(mob);
        tell(mob, true);
        return true;
    }

    /**
     * Tells every client that tracks the creature that it is (or no longer is) possessed. The effects of a creature
     * that is not a player reach no client by themselves, and her HUD names the host she has taken (it looks for the
     * effect).
     */
    private static void tell(Mob mob, boolean possessed) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        MobEffectInstance effect = mob.getEffect(ModEffects.POSSESSED);
        Packet<?> packet = possessed && effect != null ? new ClientboundUpdateMobEffectPacket(mob.getId(), effect, false)
                : new ClientboundRemoveMobEffectPacket(mob.getId(), ModEffects.POSSESSED);
        level.getChunkSource().broadcast(mob, packet);
    }

    /** Lets a host go: its effect ends, and (when she took another in its place) it wakes confused for a moment. */
    private static void release(Mob mob, boolean confuse) {
        mob.removeEffect(ModEffects.POSSESSED);
        if (confuse && mob.isAlive()) {
            mob.addEffect(new MobEffectInstance(ModEffects.CONFUSED, FREED_CONFUSED_TICKS, 0, false, false, true));
        }
    }

    /** Lets go of every creature this Phoenix has taken (she died, left, changed dimension or lost the power). */
    static void freeAll(MinecraftServer server, UUID phoenix, boolean confuse) {
        List<UUID> mine = HOSTS_OF.remove(phoenix);
        if (mine == null) {
            return;
        }
        for (UUID id : mine) {
            HOST_OWNER.remove(id);
            if (find(server, id) instanceof Mob mob) {
                release(mob, confuse);
            }
        }
    }

    /** A host's effect ended (it ran out, was cured, or the creature died): it is nobody's any more. */
    public static void hostEnded(LivingEntity host) {
        UUID owner = HOST_OWNER.remove(host.getUUID());
        if (owner != null) {
            List<UUID> mine = HOSTS_OF.get(owner);
            if (mine != null) {
                mine.remove(host.getUUID());
                if (mine.isEmpty()) {
                    HOSTS_OF.remove(owner);
                }
            }
        }
        if (host instanceof Mob mob && (owner != null || mob.hasEffect(ModEffects.POSSESSED))) {
            mob.setTarget(null); // what she sent it after is not its quarrel
            tell(mob, false);
        }
    }

    /** Once in a while: the hosts that are gone (unloaded, dead, cured) leave her list. */
    private static void upkeep(ServerPlayer player) {
        List<UUID> mine = HOSTS_OF.get(player.getUUID());
        if (mine == null) {
            return;
        }
        mine.removeIf(id -> {
            boolean gone = !(find(player.server, id) instanceof Mob mob) || !mob.isAlive() || !isHost(mob);
            if (gone) {
                HOST_OWNER.remove(id);
            }
            return gone;
        });
        if (mine.isEmpty()) {
            HOSTS_OF.remove(player.getUUID());
        }
    }

    /** Whether the Phoenix's steering is setting a target right now (a possessed creature takes no other). */
    public static boolean isSteering() {
        return steering;
    }

    /**
     * Steers a possessed creature: it goes for the creature that last hurt her (within 24 blocks), the creature she
     * last hit (within 5 s) or the nearest hostile near her; with none it walks to within 4 blocks of her and stands
     * guard. A passive animal or a villager is a harmless puppet that follows her.
     *
     * @return false if the creature is nobody's (its owner is gone): the effect should end
     */
    public static boolean steer(Mob host) {
        UUID ownerId = HOST_OWNER.get(host.getUUID());
        MinecraftServer server = host.getServer();
        ServerPlayer owner = ownerId == null || server == null ? null : server.getPlayerList().getPlayer(ownerId);
        if (owner == null || !owner.isAlive() || owner.level() != host.level() || !PhoenixPower.isPhoenix(owner)) {
            return false;
        }
        LivingEntity goal = goal(owner, host);
        steering = true;
        try {
            host.setTarget(goal);
            if (host.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
                // A creature with a brain (a piglin, a hoglin) keeps its quarrel there: hers replaces it, none forgets it.
                if (goal != null) {
                    host.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, goal);
                } else {
                    host.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                }
            }
        } finally {
            steering = false;
        }
        if (goal == null) {
            follow(host, owner);
        }
        return true;
    }

    /**
     * A creature with a brain (a piglin, a hoglin, a breeze) picks its quarrels itself and puts them straight into its
     * memory, which {@code PhoenixEvents.onChangeTarget} never sees: so every tick a host forgets a quarrel with someone
     * it may not be sent after (her, her pets, her team, the bosses), and cannot turn on her in the half second
     * between two steerings.
     */
    public static void guardBrain(Mob host) {
        if (!host.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)) {
            return;
        }
        UUID ownerId = HOST_OWNER.get(host.getUUID());
        MinecraftServer server = host.getServer();
        ServerPlayer owner = ownerId == null || server == null ? null : server.getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        host.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(target -> {
            if (!mayFight(owner, host, target)) {
                host.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        });
    }

    /** What the creature should go for, in her order of preference, or null. */
    @Nullable
    private static LivingEntity goal(ServerPlayer owner, Mob host) {
        LivingEntity hurter = owner.getLastHurtByMob();
        if (hurter != null && hurter.distanceToSqr(owner) <= HURTER_RANGE * HURTER_RANGE && mayFight(owner, host, hurter)) {
            return hurter;
        }
        LivingEntity hit = owner.getLastHurtMob();
        if (hit != null && owner.tickCount - owner.getLastHurtMobTimestamp() < LAST_HIT_TICKS && mayFight(owner, host, hit)) {
            return hit;
        }
        AABB near = owner.getBoundingBox().inflate(NEARBY_HOSTILE_RANGE);
        return owner.level().getEntitiesOfClass(LivingEntity.class, near, e -> e instanceof Enemy && e.distanceToSqr(owner) <= NEARBY_HOSTILE_RANGE * NEARBY_HOSTILE_RANGE
                && mayFight(owner, host, e)).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(host))).orElse(null);
    }

    /** Whether a host of hers may be sent after this creature: not hers, not a boss, not a mind she cannot harm. */
    private static boolean mayFight(ServerPlayer owner, Mob host, LivingEntity target) {
        if (target == host || target == owner || !target.isAlive() || target.isSpectator() || target instanceof ArmorStand) {
            return false;
        }
        if (isHers(owner, target) || isHost(target) || target.getType().is(ModTags.BOSSES)) {
            return false;
        }
        return !(target instanceof Player player) || (!player.isCreative() && mayReach(owner, player));
    }

    /** With nothing to fight a host stays near her: it walks to within {@link #GUARD_DISTANCE} blocks and stops. */
    private static void follow(Mob host, ServerPlayer owner) {
        if (host.distanceToSqr(owner) <= GUARD_DISTANCE * GUARD_DISTANCE) {
            host.getNavigation().stop();
            return;
        }
        if (host.getBrain().checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED)) {
            // Villagers, piglins...: their brains walk where the walk target says.
            host.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(owner, 1.1F, (int) GUARD_DISTANCE - 1));
        } else if (!host.getNavigation().moveTo(owner, 1.1)) {
            host.getMoveControl().setWantedPosition(owner.getX(), owner.getY(), owner.getZ(), 1.1);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 4, Sneak: the Psionic Field

    /**
     * Everything living within {@link PhoenixConfig#PSIONIC_RADIUS} of her (through the walls, it is a field of the
     * mind) is held still. Not held: herself, her pets, her team, her host, the creatures too big for a spell, whoever
     * wears Magneto's helmet; players only with PvP on. {@link PhoenixPower#touched} reaches every mind in the field,
     * held or not. If nobody is held it costs nothing and the recharge does not start.
     */
    private static void psionicField(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(4, now)) {
            return;
        }
        double radius = PhoenixConfig.PSIONIC_RADIUS.get();
        int ticks = PhoenixConfig.PSIONIC_TICKS.get();
        int held = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && hasMind(e) && e.distanceToSqr(player) <= radius * radius && !Shelters.shelters(player, e))) {
            PhoenixPower.touched(player, e, PhoenixPower.Kind.PSIONIC_FIELD, 0.0F);
            if (!fieldHolds(player, e)) {
                continue;
            }
            if (!e.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, ticks, 0, false, false, true), player)) {
                continue;
            }
            held++;
            if (e instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            level.sendParticles(new EnergyParticleOptions(PSY_MAIN, 1.0F), e.getX(), e.getEyeY() + 0.3, e.getZ(), 6, 0.25, 0.15, 0.25, 0.02);
        }
        if (held == 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.psionic_field.nothing"), true);
            return;
        }
        data.startCooldown(4, now, PhoenixConfig.PSIONIC_COOLDOWN.get());
        player.causeFoodExhaustion(PhoenixConfig.PSIONIC_EXHAUSTION.get().floatValue());
        State state = state(player);
        state.psionicUntil = now + PSIONIC_POSE_TICKS;
        data.set(Live.PHOENIX_PSIONIC, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PSIONIC_FIELD.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        sparksForOthers(level, player, new EnergyParticleOptions(PSY_CORE, 1.4F), player.getX(), player.getY() + player.getBbHeight() * 0.5,
                player.getZ(), 30, 0.5, 0.6, 0.5, 0.2);
    }

    /** Whether the Psionic Field holds this mind. */
    private static boolean fieldHolds(ServerPlayer phoenix, LivingEntity e) {
        if (isHers(phoenix, e) || isShielded(e) || e.getType().is(ModTags.SPELLBOUND_IMMUNE) || e.getType().is(ModTags.TELEPATHY_IMMUNE)) {
            return false;
        }
        return !(e instanceof Player player) || mayReach(phoenix, player);
    }

    /** A thin stream of violet motes from her forehead to the creature she holds. */
    private static void stream(ServerLevel level, ServerPlayer from, LivingEntity to) {
        Vec3 start = from.getEyePosition().add(from.getLookAngle().scale(0.3));
        Vec3 end = to.getEyePosition();
        Vec3 delta = end.subtract(start);
        int steps = (int) Math.min(40, delta.length() * 2.5);
        for (int i = 1; i <= steps; i++) {
            Vec3 p = start.add(delta.scale(i / (double) steps));
            int colour = level.random.nextInt(3) == 0 ? PSY_CORE : PSY_MAIN;
            level.sendParticles(new EnergyParticleOptions(colour, 0.7F), p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.0);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ability 5: the Phoenix Burst (hold N to charge, release to unleash)

    /** N went down: the Burst begins to gather, if she is rested and not too hungry. */
    private static void startCharge(ServerPlayer player, PowerData data) {
        State state = state(player);
        if (player.isSpectator() || state.chargeStart >= 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(5, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.phoenix.not_ready", seconds(data.cooldownUntil(5) - now)), true);
            return;
        }
        if (!player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() < PhoenixConfig.BURST_FOOD_COST.get()) {
            player.displayClientMessage(Component.translatable("message.enhanced.phoenix.too_hungry"), true);
            return;
        }
        state.chargeStart = now;
        data.set(Live.PHOENIX_CHARGING, true);
        data.set(Live.PHOENIX_BLAZE, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PHOENIX_CHARGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** N came up: the Burst goes off, or (if she let go too soon) slips away. */
    private static void releaseCharge(ServerPlayer player, PowerData data) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.chargeStart < 0) {
            return;
        }
        long ticks = player.level().getGameTime() - state.chargeStart;
        endCharge(player, data, state);
        if (ticks < PhoenixConfig.BURST_MIN_CHARGE_TICKS.get()) {
            data.startCooldown(5, player.level().getGameTime(), PhoenixConfig.BURST_CANCEL_COOLDOWN.get());
            player.displayClientMessage(Component.translatable("message.enhanced.phoenix.cancelled"), true);
            stopSound(player.serverLevel(), player, ModSounds.PHOENIX_CHARGE.getId());
            data.set(Live.PHOENIX_BLAZE, false);
            state.blazeUntil = 0L;
            return;
        }
        float f = Mth.clamp(ticks / (float) PhoenixConfig.BURST_CHARGE_TICKS.get(), 0.0F, 1.0F);
        burst(player, data, state, f);
    }

    /** Ends a charge without a Burst: the bits go, she is put down if the charge lifted her. */
    private static void endCharge(ServerPlayer player, PowerData data, State state) {
        state.chargeStart = -1L;
        data.set(Live.PHOENIX_CHARGING, false);
        if (state.chargeStartedFlight) {
            state.chargeStartedFlight = false;
            Abilities abilities = player.getAbilities();
            if (abilities.flying && !player.isCreative()) {
                abilities.flying = false;
                player.onUpdateAbilities();
            }
        }
        if (!player.isAlive()) {
            data.set(Live.PHOENIX_BLAZE, false);
        }
    }

    /** One tick of the charge: she rises, the fire spirals round her, and at full charge it goes off by itself. */
    private static void chargeTick(ServerPlayer player, PowerData data, State state, long now) {
        if (!data.is(Live.PHOENIX_CHARGING)) {
            // The key-down state was cleared from outside (the player died and came back, a screen opened).
            endCharge(player, data, state);
            return;
        }
        if (player.isSpectator()) {
            endCharge(player, data, state);
            data.set(Live.PHOENIX_BLAZE, false);
            return;
        }
        data.set(Live.PHOENIX_BLAZE, true);
        long ticks = now - state.chargeStart;
        rise(player, state);
        flames(player.serverLevel(), player, Mth.clamp(ticks / (float) PhoenixConfig.BURST_CHARGE_TICKS.get(), 0.0F, 1.0F), now);
        if (ticks >= PhoenixConfig.BURST_CHARGE_TICKS.get()) {
            // Full charge: it fires by itself.
            endCharge(player, data, state);
            burst(player, data, state, 1.0F);
        }
    }

    /**
     * She rises if she is on the ground: up at {@link #RISE_SPEED} until she is {@link #HOVER_HEIGHT} above the block
     * below her, then she hovers. A pinned Phoenix stays where she is. Falling hurts nothing while she charges.
     */
    private static void rise(ServerPlayer player, State state) {
        player.resetFallDistance();
        if (MjolnirPin.isPinned(player) || !player.mayFly()) {
            return;
        }
        Abilities abilities = player.getAbilities();
        if (!abilities.flying) {
            // The charge hovers her, wherever she is (the end of it puts her down again).
            abilities.flying = true;
            state.chargeStartedFlight = true;
            player.onUpdateAbilities();
        }
        if (heightAboveGround(player) < HOVER_HEIGHT) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, RISE_SPEED, motion.z);
            player.hurtMarked = true;
        }
    }

    /** How far above the first block below her feet she is (a large number if there is none in reach). */
    private static double heightAboveGround(ServerPlayer player) {
        Vec3 from = player.position();
        BlockHitResult hit = player.level().clip(new ClipContext(from.add(0, 0.1, 0), from.add(0, -GROUND_SEARCH, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? GROUND_SEARCH + 1.0 : from.y - hit.getLocation().y;
    }

    /** The flames that gather: gold and orange motes spiralling up her body, the faster the more charged she is. */
    private static void flames(ServerLevel level, ServerPlayer player, float f, long now) {
        double turn = now * 0.45;
        double height = player.getBbHeight();
        double radius = 0.6 + 0.5 * f;
        for (int i = 0; i < 6; i++) {
            double angle = turn + i * Mth.TWO_PI / 6.0;
            double y = player.getY() + ((now + i * 3) % 20) / 20.0 * height;
            int colour = i % 3 == 0 ? FIRE_GOLD : (i % 3 == 1 ? FIRE_MAIN : FIRE_CORE);
            sparksForOthers(level, player, new EnergyParticleOptions(colour, 1.0F + f * 0.5F), player.getX() + Math.cos(angle) * radius, y,
                    player.getZ() + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
        }
        sparksForOthers(level, player, ParticleTypes.FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0.3, 0.1, 0.3, 0.02);
    }

    /**
     * Sends particles to everyone near but {@code except}. The server's sparks round the Phoenix herself are for the
     * others to see: she sees her own flames from the client ({@code PhoenixFx} draws them for every player), and a
     * spark that spawns a block from her camera is a blob over half her screen.
     */
    public static void sparksForOthers(ServerLevel level, Entity except, ParticleOptions options, double x, double y, double z, int count,
                                        double dx, double dy, double dz, double speed) {
        for (ServerPlayer viewer : level.players()) {
            if (viewer != except) {
                level.sendParticles(viewer, options, false, x, y, z, count, dx, dy, dz, speed);
            }
        }
    }

    /** The release: pays, rests, cleanses her, drains her and sends the wave. */
    private static void burst(ServerPlayer player, PowerData data, State state, float f) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        data.startCooldown(5, now, PhoenixConfig.BURST_COOLDOWN.get());
        state.blazeUntil = now + BLAZE_AFTER_TICKS;
        data.set(Live.PHOENIX_BLAZE, true);
        player.causeFoodExhaustion(4.0F * PhoenixConfig.BURST_FOOD_COST.get() * Math.max(0.3F, f));
        // The fire burns off what holds her.
        player.removeEffect(ModEffects.SPELLBOUND);
        player.removeEffect(ModEffects.CONFUSED);
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.DARKNESS);
        int drain = PhoenixConfig.BURST_DRAIN_TICKS.get();
        if (!player.isCreative() && drain > 0) {
            // Without the swirls of a potion: in first person they are dark squares over her face for five seconds.
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, drain, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, drain, 0, false, false, true));
        }
        Vec3 centre = player.position().add(0, player.getBbHeight() * 0.5, 0);
        float radius = PhoenixBurst.radius(f);
        PhoenixBurst.start(player, centre, f);
        PacketDistributor.sendToPlayersNear(level, null, centre.x, centre.y, centre.z, Math.max(64.0, radius + 32.0),
                new PhoenixBurstPayload(player.getId(), centre, player.getYRot(), f, radius, now));
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.PHOENIX_BURST.get(), SoundSource.PLAYERS, Math.max(1.0F, radius / 8.0F), 1.0F);
        sparksForOthers(level, player, new EnergyParticleOptions(FIRE_CORE, 2.0F), centre.x, centre.y, centre.z, 40, 0.4, 0.4, 0.4, 0.3);
    }

    /** Stops a sound for every player near (the Burst slipped away; its gathering roar should not go on). */
    private static void stopSound(ServerLevel level, ServerPlayer at, ResourceLocation sound) {
        ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(sound, SoundSource.PLAYERS);
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(at) <= 64.0 * 64.0) {
                other.connection.send(packet);
            }
        }
    }

    private PhoenixAbilities() {
    }
}
