package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.beam.BeamStyles;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.CosmicSpiderConfig;
import com.kolodkingal.enhanced.config.HulkConfig;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.protect.DarkMagic;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.strange.SlingPortals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cosmic Spider-Man (3.18): Spider-Man filled with the Enigma Force, as in the 1990 comics. A transformation of
 * {@link Power#SPIDER_MAN} (or of a custom hero with the row {@link HeroAbility#SPIDER_COSMIC}), not a hero of its own.
 *
 * <p><b>The meter.</b> A cosmic meter of {@code energyMax} (170), Iron Man's suit energy in another colour: it is kept in
 * {@link PowerData#ironCharge} in hundredths (saved and synced; Spider-Man has no other use for it). While he is plain
 * Spider-Man it fills by {@code refillPerSecond}; Sneak + N makes him cosmic only when it is full; while cosmic it never
 * recharges, drains by {@code drainPerSecond} (and {@code flightDrainPerSecond} more in flight; the blasts by their own
 * cost) and, at 0, he is plain Spider-Man again (flight ends with Slow Falling) and it starts to fill again. Creative players
 * spend nothing, as Iron Man's suit.
 *
 * <p><b>Cosmic.</b> {@link Live#SPIDER_COSMIC}. His fists hit {@code strengthFactor} times as hard as the Hulk's at full rage
 * ({@link #fistDamage}); armour, toughness and knockback resistance of a full netherite set (in place of his suit's); he
 * flies ({@code flightSpeed}); he heals {@code regenAmount} every {@code regenIntervalTicks} on top of a player's own
 * healing (twice as fast); his spider-sense is {@code senseFactor} wider. His keys: R held fires the Cosmic Blast from his
 * hand, Sneak + R the Cosmic Eyes ({@link ContinuousBeam}); B opens his portal screen (the client's: the places he saved and
 * the worlds), Sneak + B a portal to the next world ({@link #worldPortal}); Y is Matter Restructuring, Billy's Build Magic
 * at {@code matterFactor} of Billy's sizes ({@code build.Limits}). <b>The weakness:</b> pure life force, dark magic hurts him
 * {@code darkMagicFactor} times as much ({@link #onIncomingDamage}).
 */
public final class CosmicSpider {
    /** The meter's units: {@link PowerData#ironCharge} counts hundredths of cosmic energy. */
    public static final int UNITS = 100;
    /** The aura's white-blue and the cosmic white. */
    public static final int AURA = 0x9FD8FF;
    public static final int WHITE = 0xF4FAFF;
    /** The tint of his portals (the Sling Ring's orange recoloured). */
    public static final int PORTAL_TINT = 0x8FD0FF;

    /** The Cosmic Blast from his hand: a white-hot core in a white-blue body. */
    public static final BeamStyle BLAST = BeamStyles.register(new BeamStyle("cosmic_blast", "beam.enhanced.cosmic_blast",
            0xFFFFFF, 0xE8F6FF, 0xA8DCFF, 0x5FA8F0, 0x2A5FC0, false));
    /** The Cosmic Eyes: two white beams from his eyes (drawn as Love's eye beams, in these colours). */
    public static final BeamStyle EYES = BeamStyles.register(new BeamStyle("cosmic_eyes", "beam.enhanced.cosmic_eyes",
            0xFFFFFF, 0xF0FAFF, 0xC8ECFF, 0x7FC0F8, 0x3A70C8, false));

    static final ResourceLocation STRENGTH = Enhanced.id("spider_cosmic_strength");

    /** The beam he fires: 1 the hand, 2 the eyes. */
    private static final Map<UUID, Integer> BEAM = new HashMap<>();
    /** His exact energy (the synced int is rounded to hundredths). */
    private static final Map<UUID, Double> EXACT = new HashMap<>();
    /** When his cosmic healing next heals. */
    private static final Map<UUID, Long> REGEN_AT = new HashMap<>();
    /** When he may open another portal. */
    private static final Map<UUID, Long> PORTAL_READY = new HashMap<>();
    /** Whether "cosmic power ready" has been said since the meter was last below full. */
    private static final Map<UUID, Boolean> READY_SAID = new HashMap<>();

    /** The worlds his portals go to, in the order Sneak + B steps through them. */
    public static final List<ResourceKey<Level>> WORLDS = List.of(Level.OVERWORLD, Level.NETHER, Level.END);

    /** Loads the class (its beam styles register themselves). */
    public static void init() {
    }

    // ---- who ----

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    public static boolean enabled() {
        return !loaded() || CosmicSpiderConfig.ENABLED.get();
    }

    /**
     * Whether this player may be Cosmic Spider-Man: the feature is on and he has Ultimate (the switch {@code [ultimate]
     * enabled}, off by default, or an operator's grant: {@code /enhanced ultimate}).
     */
    public static boolean allowed(net.minecraft.world.entity.player.Player player) {
        return enabled() && com.kolodkingal.enhanced.ultimate.Ultimate.allowed(player);
    }

    /** Whether he has the cosmic meter: Spider-Man, or a custom hero with the cosmic row. */
    public static boolean hasMeter(PowerData data) {
        return data.power() == Power.SPIDER_MAN || data.power() == Power.CUSTOM && data.has(HeroAbility.SPIDER_COSMIC);
    }

    /** Whether he is cosmic right now (both sides). */
    public static boolean isCosmic(PowerData data) {
        return data.is(Live.SPIDER_COSMIC);
    }

    public static boolean isCosmic(@Nullable Entity entity) {
        return entity instanceof Player player && isCosmic(Powers.get(player));
    }

    /** Spider-Man himself, cosmic: his keys are the cosmic ones (a custom hero keeps his own). */
    public static boolean cosmicKeys(PowerData data) {
        return data.power() == Power.SPIDER_MAN && isCosmic(data);
    }

    // ---- the meter ----

    public static double max() {
        return loaded() ? CosmicSpiderConfig.ENERGY_MAX.get() : 170.0;
    }

    /** His cosmic energy (both sides: the synced int). */
    public static double energy(PowerData data) {
        return data.ironCharge() / (double) UNITS;
    }

    /** The meter as a share (0-1). */
    public static float fraction(PowerData data) {
        double max = max();
        return max <= 0.0 ? 0.0F : (float) Mth.clamp(energy(data) / max, 0.0, 1.0);
    }

    /** Whether the meter is full (he may become cosmic). */
    public static boolean full(PowerData data) {
        return energy(data) + 0.5 / UNITS >= max();
    }

    /** The meter gathered since it was last sent (see the tick), and for how many ticks. */
    private static final java.util.Map<java.util.UUID, Double> PENDING = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Integer> PENDING_TICKS = new java.util.HashMap<>();

    private static double exact(Player player, PowerData data) {
        double synced = energy(data);
        Double known = EXACT.get(player.getUUID());
        // (anyone else who set the int, a command or a test, is believed)
        return known == null || Math.abs(known - synced) > 1.0 / UNITS ? synced : known;
    }

    /** Sets his energy (clamped to the meter). */
    public static void setEnergy(Player player, PowerData data, double value) {
        double clamped = Mth.clamp(value, 0.0, max());
        EXACT.put(player.getUUID(), clamped);
        data.setIronCharge((int) Math.round(clamped * UNITS));
    }

    // ---- the keys ----

    /**
     * A key of his (from {@code SpiderServer.onKey}). Sneak + N becomes cosmic or lets it go; while Spider-Man is cosmic, R,
     * Sneak + R and Sneak + B are the cosmic ones (B alone opens the portal screen on his client). Returns whether the key was
     * the cosmic power's.
     */
    static boolean onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt, boolean blocked) {
        if (ability == 5 && alt && hasMeter(data)) {
            if (down && !blocked) {
                toggle(player, data);
            }
            return true;
        }
        if (!cosmicKeys(data)) {
            return false;
        }
        switch (ability) {
            case 1 -> {
                if (!down) {
                    stopBeam(player);
                } else if (!blocked) {
                    startBeam(player, data, alt);
                }
                return true;
            }
            case 4 -> {
                if (down && !blocked && alt) {
                    worldPortal(player, data, null);
                }
                return true;
            }
            case 5 -> {
                if (down) {
                    say(player, "message.enhanced.cosmic_spider.no_black_eyes");
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /** Whether his cosmic look is Captain Universe's (else the starfield suit). */
    public static boolean universe(PowerData data) {
        return data.toggle(Toggle.COSMIC_UNIVERSE);
    }

    /** H while cosmic: the other cosmic look (the starfield suit or Captain Universe's); the choice is kept for next time. */
    public static void switchLook(ServerPlayer player, PowerData data) {
        boolean now = !universe(data);
        data.setToggle(Toggle.COSMIC_UNIVERSE, now);
        burst(player.serverLevel(), player, 20);
        player.displayClientMessage(Component.translatable("message.enhanced.cosmic_spider.look",
                Component.translatable(now ? "hud.enhanced.cosmic_spider.look.universe" : "hud.enhanced.cosmic_spider.look.starfield"))
                .withStyle(s -> s.withColor(AURA)), true);
    }

    /** Sneak + N: cosmic when the meter is full, or plain Spider-Man again. */
    public static void toggle(ServerPlayer player, PowerData data) {
        if (isCosmic(data)) {
            revert(player, data, false);
            return;
        }
        if (!enabled()) {
            say(player, "message.enhanced.cosmic_spider.disabled");
            return;
        }
        if (!allowed(player)) {
            say(player, "message.enhanced.cosmic_spider.no_ultimate");
            return;
        }
        if (!full(data)) {
            player.displayClientMessage(Component.translatable("message.enhanced.cosmic_spider.not_ready",
                    (int) Math.floor(energy(data)), (int) Math.round(max())).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        enter(player, data);
    }

    /** He becomes Cosmic Spider-Man (the meter is not checked: the key does). */
    public static void enter(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        data.set(Live.SPIDER_COSMIC, true);
        SpiderServer.cosmicBegins(player, data);
        if (data.toggle(Toggle.SPIDER_LEGS)) {
            data.setToggle(Toggle.SPIDER_LEGS, false);
        }
        REGEN_AT.put(player.getUUID(), level.getGameTime() + regenInterval());
        READY_SAID.remove(player.getUUID());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.8F);
        burst(level, player, 60);
        player.displayClientMessage(Component.translatable("message.enhanced.cosmic_spider.on")
                .withStyle(s -> s.withColor(AURA).withBold(true)), true);
    }

    /**
     * He is plain Spider-Man again: when the meter ran out ({@code ranOut}) or he let it go. His beam ends, his flight ends
     * softly (Slow Falling), and the meter starts to fill again.
     */
    public static void revert(ServerPlayer player, PowerData data, boolean ranOut) {
        if (!isCosmic(data)) {
            return;
        }
        data.set(Live.SPIDER_COSMIC, false);
        stopBeam(player);
        ServerLevel level = player.serverLevel();
        if (player.getAbilities().flying || !player.onGround()) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.3F);
        burst(level, player, 24);
        player.displayClientMessage(Component.translatable(ranOut ? "message.enhanced.cosmic_spider.spent"
                : "message.enhanced.cosmic_spider.off").withStyle(ChatFormatting.GRAY), true);
    }

    /** Ends the cosmic state without a word (the power is gone, he logged out or died). */
    static void endQuietly(ServerPlayer player, PowerData data) {
        data.set(Live.SPIDER_COSMIC, false);
        if (BEAM.remove(player.getUUID()) != null) {
            ContinuousBeam.stop(player);
        }
    }

    // ---- every tick ----

    /** Every tick of a Spider-Man (or a custom hero with the row): the meter, and the cosmic body's upkeep. */
    static void tick(ServerPlayer player, PowerData data, long now) {
        if (!hasMeter(data)) {
            if (isCosmic(data)) {
                endQuietly(player, data);
            }
            return;
        }
        double energy = exact(player, data);
        if (!isCosmic(data)) {
            if (!allowed(player)) {
                return; // (without Ultimate the meter rests: nothing to fill, nothing to send)
            }
            double max = max();
            if (energy < max) {
                // (gathered every tick but sent once a second, or when it is full: each change of the meter sends his whole
                // power data to the players who see him, and a send every tick undid the client's own swings and Web
                // Assist before they began)
                double pending = PENDING.merge(player.getUUID(), refill() / 20.0, Double::sum);
                int ticks = PENDING_TICKS.merge(player.getUUID(), 1, Integer::sum);
                if (energy + pending >= max || ticks >= 20) {
                    PENDING.remove(player.getUUID());
                    PENDING_TICKS.remove(player.getUUID());
                    setEnergy(player, data, energy + pending);
                }
                if (full(data) && !READY_SAID.getOrDefault(player.getUUID(), false)) {
                    READY_SAID.put(player.getUUID(), true);
                    player.displayClientMessage(Component.translatable("message.enhanced.cosmic_spider.ready")
                            .withStyle(s -> s.withColor(AURA)), true);
                    player.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.6F);
                }
            } else if (energy > max) {
                setEnergy(player, data, max);
            }
            return;
        }
        if (!allowed(player)) {
            revert(player, data, false);
            return;
        }
        double perSecond = cfg(CosmicSpiderConfig.DRAIN_PER_SECOND, 0.75);
        if (player.getAbilities().flying) {
            perSecond += cfg(CosmicSpiderConfig.FLIGHT_DRAIN_PER_SECOND, 0.25);
        }
        perSecond += beamTick(player, data);
        if (!player.getAbilities().instabuild) {
            energy = Math.max(0.0, energy - perSecond / 20.0);
            setEnergy(player, data, energy);
        }
        // His healing: on top of a player's own (twice as fast).
        long at = REGEN_AT.getOrDefault(player.getUUID(), now + regenInterval());
        if (now >= at) {
            REGEN_AT.put(player.getUUID(), now + regenInterval());
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal((float) cfg(CosmicSpiderConfig.REGEN_AMOUNT, 1.0));
            }
        } else if (at - now > regenInterval()) {
            REGEN_AT.put(player.getUUID(), now + regenInterval());
        }
        if (energy <= 0.0 && !player.getAbilities().instabuild) {
            revert(player, data, true);
        }
    }

    private static int regenInterval() {
        return loaded() ? CosmicSpiderConfig.REGEN_INTERVAL_TICKS.get() : 80;
    }

    private static double refill() {
        return cfg(CosmicSpiderConfig.REFILL_PER_SECOND, 1.0);
    }

    static double cfg(com.kolodkingal.enhanced.config.core.ConfigSpec.DoubleValue value, double fallback) {
        return loaded() ? value.get() : fallback;
    }

    // ---- the body ----

    /** What his bare fist hits for while cosmic: {@code strengthFactor} times the Hulk's at 100% rage. */
    public static double fistDamage() {
        double hulk = (1.0 + cfg(HulkConfig.ATTACK_BONUS, 7.0)) * (1.0 + cfg(HulkConfig.RAGE_DAMAGE_BONUS, 0.3));
        return hulk * cfg(CosmicSpiderConfig.STRENGTH_FACTOR, 3.0);
    }

    /**
     * The cosmic body's modifiers (from {@code SpiderServer.applyBody}): the melee strength that brings his bare fist to
     * {@link #fistDamage}. The armour is put in place of his suit's there.
     */
    static void applyBody(ServerPlayer player, PowerData data, boolean body, double bodyScale) {
        boolean cosmic = isCosmic(data) && hasMeter(data);
        double own = 1.0 + (body ? SpiderConfig.STRENGTH_BONUS.get() * bodyScale : 0.0);
        PowerTicker.setModifier(player, Attributes.ATTACK_DAMAGE, STRENGTH, Math.max(0.0, fistDamage() - own), cosmic);
    }

    /** The flight speed while cosmic. */
    public static float flightSpeed() {
        return (float) cfg(CosmicSpiderConfig.FLIGHT_SPEED, 0.16);
    }

    /** Whether he may fly: cosmic (for {@code PowerTicker}). */
    public static boolean canFly(Player player) {
        PowerData data = Powers.get(player);
        return isCosmic(data) && hasMeter(data);
    }

    /**
     * (3.18.1) Agatha's Power Drain on Cosmic Spider-Man: it takes {@code agathaDrain} (25) of his cosmic meter, which he must
     * gather again; at 0 he is plain Spider-Man again (the tick sees to it). Returns how much it took.
     */
    public static int drainedBy(ServerPlayer victim, ServerPlayer agatha) {
        PowerData data = Powers.get(victim);
        double before = exact(victim, data);
        double take = cfg(CosmicSpiderConfig.AGATHA_DRAIN, 25.0);
        setEnergy(victim, data, before - take);
        int taken = (int) Math.round(before - energy(data));
        victim.displayClientMessage(Component.translatable("message.enhanced.cosmic_spider.drained", agatha.getDisplayName(), taken)
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        if (energy(data) <= 0.0) {
            revert(victim, data, true);
        }
        Powers.syncIfDirty(victim);
        return taken;
    }

    /** Pure life force: dark magic hurts him {@code darkMagicFactor} times as much while cosmic. */
    public static float onIncomingDamage(Player player, DamageSource source, float amount) {
        if (!isCosmic(Powers.get(player)) || !isDark(source)) {
            return amount;
        }
        return (float) (amount * cfg(CosmicSpiderConfig.DARK_MAGIC_FACTOR, 1.5));
    }

    /** Whether this harm is dark magic: by its type, the thing that dealt it, or one who works dark magic. */
    public static boolean isDark(DamageSource source) {
        return DarkMagic.is(source) || DarkMagic.is(source.getDirectEntity()) || DarkMagic.casts(source.getEntity());
    }

    /** How much wider his spider-sense is now. */
    public static double senseFactor(PowerData data) {
        return isCosmic(data) ? cfg(CosmicSpiderConfig.SENSE_FACTOR, 2.5) : 1.0;
    }

    // ---- the blasts ----

    static void startBeam(ServerPlayer player, PowerData data, boolean eyes) {
        if (exact(player, data) <= 0.0 && !player.getAbilities().instabuild) {
            return;
        }
        if (BEAM.containsKey(player.getUUID())) {
            stopBeam(player);
        }
        BEAM.put(player.getUUID(), eyes ? 2 : 1);
        ContinuousBeam.start(player, eyes ? EYES : BLAST, eyes ? cfgInt(CosmicSpiderConfig.EYE_BEAM_RANGE, 24)
                : cfgInt(CosmicSpiderConfig.BLAST_RANGE, 32));
        if (eyes) {
            data.set(Live.BEAMING, false); // (the eyes: no arm out along the look)
        }
        player.serverLevel().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.6F, eyes ? 1.9F : 1.6F);
    }

    static void stopBeam(ServerPlayer player) {
        if (BEAM.remove(player.getUUID()) != null) {
            ContinuousBeam.stop(player);
        }
    }

    /** Whether he fires a cosmic beam (1 the hand, 2 the eyes, 0 none). */
    public static int beam(Player player) {
        return BEAM.getOrDefault(player.getUUID(), 0);
    }

    /** A tick of his beam; returns what it costs a second. */
    private static double beamTick(ServerPlayer player, PowerData data) {
        Integer mode = BEAM.get(player.getUUID());
        if (mode == null) {
            return 0.0;
        }
        if (!ContinuousBeam.isFiring(player) || SpiderServer.cosmicBlocked(player, data)) {
            stopBeam(player);
            return 0.0;
        }
        boolean eyes = mode == 2;
        BeamSpec spec = eyes
                ? new BeamSpec(EYES, cfgInt(CosmicSpiderConfig.EYE_BEAM_RANGE, 24), (float) cfg(CosmicSpiderConfig.EYE_BEAM_DAMAGE, 8.0),
                cfgInt(CosmicSpiderConfig.EYE_BEAM_INTERVAL, 5), 0.0F, ModDamageTypes.COSMIC_BLAST, null)
                : new BeamSpec(BLAST, cfgInt(CosmicSpiderConfig.BLAST_RANGE, 32), (float) cfg(CosmicSpiderConfig.BLAST_DAMAGE, 6.0),
                cfgInt(CosmicSpiderConfig.BLAST_INTERVAL, 5), 0.0F, ModDamageTypes.COSMIC_BLAST, null);
        ContinuousBeam.tick(player, spec);
        return eyes ? cfg(CosmicSpiderConfig.EYE_BEAM_ENERGY_PER_SECOND, 4.0) : cfg(CosmicSpiderConfig.BLAST_ENERGY_PER_SECOND, 3.0);
    }

    static int cfgInt(com.kolodkingal.enhanced.config.core.ConfigSpec.IntValue value, int fallback) {
        return loaded() ? value.get() : fallback;
    }

    // ---- the portals ----

    /** Ticks before he may open another portal (0: now). */
    public static long portalWait(Player player) {
        long ready = PORTAL_READY.getOrDefault(player.getUUID(), 0L);
        return Math.max(0L, ready - player.level().getGameTime());
    }

    /**
     * Whether he may open a portal costing {@code cost} now: cosmic, not on the Witches' Road, the cooldown passed and the
     * energy there. Says why not.
     */
    public static boolean mayOpen(ServerPlayer player, PowerData data, double cost) {
        if (!isCosmic(data) || data.power() != Power.SPIDER_MAN) {
            return false;
        }
        if (SpiderServer.cosmicBlocked(player, data)) {
            return false;
        }
        if (RoadDimension.in(player)) {
            say(player, "message.enhanced.sling_ring.shut");
            return false;
        }
        long wait = portalWait(player);
        if (wait > 0) {
            player.displayClientMessage(Component.translatable("message.enhanced.cosmic_spider.portal_wait", (wait + 19) / 20)
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!player.getAbilities().instabuild && exact(player, data) + 1.0E-6 < cost) {
            say(player, "message.enhanced.cosmic_spider.portal_energy");
            return false;
        }
        return true;
    }

    /** The cost of a portal is paid and the cooldown starts. */
    public static void paid(ServerPlayer player, PowerData data, double cost) {
        if (!player.getAbilities().instabuild) {
            setEnergy(player, data, exact(player, data) - cost);
        }
        PORTAL_READY.put(player.getUUID(), player.level().getGameTime() + cfgInt(CosmicSpiderConfig.PORTAL_COOLDOWN, 100));
    }

    public static double portalCost() {
        return cfg(CosmicSpiderConfig.PORTAL_ENERGY, 15.0);
    }

    public static double worldPortalCost() {
        return cfg(CosmicSpiderConfig.WORLD_PORTAL_ENERGY, 25.0);
    }

    /** The world after his own in {@link #WORLDS} (the Overworld from any other). */
    public static ResourceKey<Level> nextWorld(ResourceKey<Level> here) {
        int i = WORLDS.indexOf(here);
        return WORLDS.get(i < 0 ? 0 : (i + 1) % WORLDS.size());
    }

    /**
     * A pair of portals before him to another world ({@code to}, or the next one): its twin stands where he would be there (a
     * Nether eighth of the way, the End's island, the Overworld's spawn from the End), on the first safe ground. Returns
     * whether it opened.
     */
    public static boolean worldPortal(ServerPlayer player, PowerData data, @Nullable ResourceKey<Level> to) {
        double cost = worldPortalCost();
        if (!mayOpen(player, data, cost)) {
            return false;
        }
        ServerLevel here = player.serverLevel();
        ResourceKey<Level> target = to == null ? nextWorld(here.dimension()) : to;
        if (target == here.dimension()) {
            say(player, "message.enhanced.cosmic_spider.same_world");
            return false;
        }
        if (PlaceBook.refusesAsgard(player, PlaceBook.Kind.SLING_RING, target)) {
            return false;
        }
        ServerLevel there = player.server.getLevel(target);
        if (there == null) {
            say(player, "message.enhanced.sling_ring.no_world");
            return false;
        }
        Vec3 near = SlingPortals.nearFeet(player);
        if (!SlingPortals.roomFor(here, near)) {
            say(player, "message.enhanced.strange.portal_no_room");
            return false;
        }
        Vec3 far = landing(there, arrival(player, here, there));
        open(player, here, near, there, far);
        paid(player, data, cost);
        ResourceKey<Level> dimension = target;
        player.displayClientMessage(Component.translatable("message.enhanced.cosmic_spider.world_portal",
                Component.translatable("dimension." + dimension.location().getNamespace() + "." + dimension.location().getPath()))
                .withStyle(s -> s.withColor(AURA)), true);
        return true;
    }

    /** Opens a pair of his white-blue portals (the Sling Ring's, recoloured). */
    public static void open(ServerPlayer player, ServerLevel here, Vec3 near, ServerLevel there, Vec3 far) {
        SlingPortals.open(player, here, near, player.getYRot(), there, far, player.getYRot(), PORTAL_TINT);
    }

    /** Where he would arrive in {@code there}: the same place scaled by the worlds' coordinate scale, the End's island, or the spawn. */
    static Vec3 arrival(ServerPlayer player, ServerLevel here, ServerLevel there) {
        if (there.dimension() == Level.END) {
            return new Vec3(100.5, 50, 0.5);
        }
        if (here.dimension() == Level.END) {
            BlockPos spawn = there.getSharedSpawnPos();
            return new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }
        double scale = here.dimensionType().coordinateScale() / there.dimensionType().coordinateScale();
        return new Vec3(player.getX() * scale, player.getY(), player.getZ() * scale);
    }

    /**
     * The first safe ground near {@code at} in {@code level} (two free cells over a solid block that is not lava or fire,
     * under the Nether's roof), looked for in a widening square; failing that, a small obsidian floor is laid there, as
     * vanilla lays one in the End.
     */
    public static Vec3 landing(ServerLevel level, Vec3 at) {
        int x0 = Mth.floor(at.x);
        int z0 = Mth.floor(at.z);
        for (int r = 0; r <= 24; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    BlockPos found = standAt(level, x0 + dx, z0 + dz, Mth.floor(at.y));
                    if (found != null) {
                        return new Vec3(found.getX() + 0.5, found.getY(), found.getZ() + 0.5);
                    }
                }
            }
        }
        int y = level.dimension() == Level.END ? 49 : Mth.clamp(Mth.floor(at.y), level.getMinBuildHeight() + 8,
                level.dimension() == Level.NETHER ? 100 : level.getMaxBuildHeight() - 8);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlockAndUpdate(new BlockPos(x0 + dx, y - 1, z0 + dz), Blocks.OBSIDIAN.defaultBlockState());
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlockAndUpdate(new BlockPos(x0 + dx, y + dy, z0 + dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        return new Vec3(x0 + 0.5, y, z0 + 0.5);
    }

    /** The feet of a safe stand in this column, nearest {@code nearY}, or null. */
    @Nullable
    static BlockPos standAt(ServerLevel level, int x, int z, int nearY) {
        level.getChunkAt(new BlockPos(x, 0, z));
        boolean nether = level.dimension() == Level.NETHER;
        int top = nether ? 120 : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int bottom = level.getMinBuildHeight() + 1;
        if (!nether) {
            // (the surface: the first ground from the sky)
            BlockPos feet = new BlockPos(x, top, z);
            return safe(level, feet) ? feet : null;
        }
        BlockPos best = null;
        for (int y = top; y > bottom + 1; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            if (safe(level, feet) && (best == null || Math.abs(y - nearY) < Math.abs(best.getY() - nearY))) {
                best = feet;
            }
        }
        return best;
    }

    private static boolean safe(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        var floor = level.getBlockState(below);
        return !floor.getCollisionShape(level, below).isEmpty() && floor.getFluidState().isEmpty()
                && !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.BEDROCK) && !floor.is(Blocks.FIRE)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() && level.getBlockState(feet).getFluidState().isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.getBlockState(feet.above()).getFluidState().isEmpty();
    }

    // ---- effects and words ----

    private static void burst(ServerLevel level, ServerPlayer player, int count) {
        level.sendParticles(new EnergyParticleOptions(AURA, 1.4F), player.getX(), player.getY() + 1.0, player.getZ(), count,
                0.6, 0.9, 0.6, 0.15);
        level.sendParticles(new EnergyParticleOptions(WHITE, 1.0F), player.getX(), player.getY() + 1.0, player.getZ(), count / 2,
                0.4, 0.8, 0.4, 0.25);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), count / 3, 0.5, 0.8, 0.5, 0.08);
    }

    static void say(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY), true);
    }

    /** Forgets a player (logout). */
    static void forget(UUID id) {
        BEAM.remove(id);
        EXACT.remove(id);
        PENDING.remove(id);
        PENDING_TICKS.remove(id);
        REGEN_AT.remove(id);
        PORTAL_READY.remove(id);
        READY_SAID.remove(id);
    }

    static void clear() {
        BEAM.clear();
        EXACT.clear();
        PENDING.clear();
        PENDING_TICKS.clear();
        REGEN_AT.clear();
        PORTAL_READY.clear();
        READY_SAID.clear();
    }

    private CosmicSpider() {
    }
}
