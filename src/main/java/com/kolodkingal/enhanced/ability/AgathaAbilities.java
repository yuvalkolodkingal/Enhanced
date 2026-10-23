package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.beam.BeamSpec;
import com.kolodkingal.enhanced.beam.ContinuousBeam;
import com.kolodkingal.enhanced.config.AgathaConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.item.DarkholdItem;
import com.kolodkingal.enhanced.network.AgathaFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.vishanti.Ward;
import com.kolodkingal.enhanced.vishanti.WardManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Agatha Harkness (3.3): a player with no power who read the Darkhold ({@link DarkholdBook#awakens}). The book opened its
 * dark magic to her, and she keeps it with or without the book in hand: R is the Darkhold's Purple Beam (held; Sneak + R
 * the book's hex mark on the creature she looks at, or its mind glimpse), G flight, V the Power Drain (she draws the power
 * out of a hero for a while, as she does to Wanda in WandaVision, or the life out of a creature), B the Witch's Shield
 * (held, {@link AgathaShield}) and N the Rune Trap (a circle of glowing runes that binds and burns what stands in it).
 * Magic used on her poisons whoever used it, as the Darkhold does for its holder ({@code AgathaEvents}); she has 40
 * health. She is a sorcerer (the Book of the Vishanti, the Witches' Road, the Hex Runes). Her magic is dark magic: its
 * damage types are the tag {@code enhanced:dark_magic}, and the Power Drain and the Rune Trap do not reach into a ward of
 * the Vishanti ({@link #wardStops}).
 *
 * <p>The plug-in is {@link AgathaServer}; what the clients draw comes with {@link AgathaFxPayload} and the synced data
 * (the beam's {@link Live#BEAMING}, the hand of the hex's {@link Live#CASTING}, the shield's fields, the cooldowns).
 */
public final class AgathaAbilities {
    // ---- her magic: the violet of WandaVision, from the white-hot core out to the deep purple ----
    public static final int CORE = 0xF6ECFF;
    public static final int LILAC = 0xE2C2FF;
    public static final int VIOLET = 0x9A3FE0;
    public static final int DEEP = 0x4B1A86;

    private static final ResourceLocation BODY_HEALTH = Enhanced.id("agatha_health");

    /** The cooldown slots of the Power Drain and of the Rune Trap (the shield's is {@link AgathaShield#SLOT}). */
    public static final int DRAIN_SLOT = 3;
    public static final int RUNE_SLOT = 5;
    /** How long the hand of Sneak + R stays raised ({@link Live#CASTING}). */
    public static final int CAST_TICKS = 10;
    /** How long the violet stream of the Power Drain flows into her hands. */
    public static final int DRAIN_STREAM_TICKS = 16;
    /** A creature in the runes is bound for this long, renewed every {@link #RUNE_PULSE} ticks while it stays. */
    private static final int RUNE_BIND_TICKS = 15;
    private static final int RUNE_PULSE = 10;
    /** How far above the circle the runes reach (blocks), and how far below it a creature still stands in it. */
    private static final double RUNE_HEIGHT = 2.5;
    private static final double RUNE_BELOW = 1.0;
    /** How far down from a wall she looks at the Rune Trap finds the ground. */
    private static final int GROUND_SEARCH = 6;

    /** A circle of runes on the ground, laid by one Agatha (one at a time: a new one puts out the old). */
    public record RuneTrap(ResourceKey<Level> dimension, Vec3 center, double radius, long start, long end) {
        /** Whether this creature stands in the circle. */
        public boolean holds(Entity e) {
            if (e.level().dimension() != dimension || e.getY() < center.y - RUNE_BELOW || e.getY() > center.y + RUNE_HEIGHT) {
                return false;
            }
            double dx = e.getX() - center.x;
            double dz = e.getZ() - center.z;
            return dx * dx + dz * dz <= radius * radius;
        }
    }

    /** Who fires her own Purple Beam now (R is held; not the Darkhold's, which is the book's to stop). */
    private static final Set<UUID> FIRING = new HashSet<>();
    /** Agatha -> the game time the hand of Sneak + R comes down. */
    private static final Map<UUID, Long> CASTING_UNTIL = new HashMap<>();
    /** Agatha -> her Rune Trap. */
    private static final Map<UUID, RuneTrap> TRAPS = new HashMap<>();
    /** Who is reading the Darkhold to become her, and since when. */
    private static final Map<UUID, Long> READING = new HashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // Queries

    /** Whether this is a player who is Agatha. */
    public static boolean isAgatha(@Nullable Entity e) {
        return e instanceof Player player && Powers.is(player, Power.AGATHA);
    }

    /**
     * Whether this player has this ability of hers: Agatha, or (3.4) a custom hero who picked it (her body: the poison that
     * answers magic used on her; her shield).
     */
    public static boolean has(@Nullable Entity e, HeroAbility ability) {
        return e instanceof Player player && Powers.get(player).has(ability);
    }

    /** Agatha's Rune Trap, or null. */
    @Nullable
    public static RuneTrap trapOf(Player agatha) {
        return TRAPS.get(agatha.getUUID());
    }

    /** Whether her own Purple Beam (R) burns. */
    public static boolean firing(Player player) {
        return FIRING.contains(player.getUUID());
    }

    /**
     * Whether a ward of the Vishanti keeps her magic from this point: it stands in a ward that she is not inside herself.
     * (Dark magic is kept out at a ward's edge; the damage of it is the tag {@code enhanced:dark_magic}.)
     */
    public static boolean wardStops(ServerPlayer agatha, Vec3 at) {
        ServerLevel level = agatha.serverLevel();
        Ward ward = WardManager.wardAt(level, at);
        return ward != null && !ward.equals(WardManager.wardAt(level, agatha.position()));
    }

    private static boolean starving(Player player) {
        return !player.getAbilities().invulnerable && player.getFoodData().getFoodLevel() <= 0;
    }

    private static void pay(ServerPlayer player, double exhaustion) {
        if (!player.isCreative()) {
            player.causeFoodExhaustion((float) exhaustion);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input and ticks

    /**
     * A key of hers (3.4: also one of a custom hero's bindings, with the key the ability has for her). {@code alt}: its Sneak
     * variant (Sneak + R is the hex mark).
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down) {
            if (ability == 1) {
                beamKey(player, data, false);
            } else if (ability == AgathaShield.SLOT && data.chaosShieldActive() && data.has(HeroAbility.AGATHA_SHIELD)) {
                AgathaShield.drop(player, data, false);
            }
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    hex(player, data);
                } else {
                    beamKey(player, data, true);
                }
            }
            case 2 -> PowerTicker.toggleFlight(player);
            case 3 -> drain(player, data);
            case 4 -> AgathaShield.raise(player, data);
            case 5 -> runes(player, data);
            default -> {
            }
        }
    }

    static void tick(ServerPlayer player, PowerData data) {
        Long castUntil = CASTING_UNTIL.get(player.getUUID());
        if (castUntil != null && player.level().getGameTime() >= castUntil) {
            CASTING_UNTIL.remove(player.getUUID());
            data.set(Live.CASTING, false);
        }
        beamTick(player, data);
        if (data.chaosShieldActive() && data.has(HeroAbility.AGATHA_SHIELD)) { // (3.4: the shield fields may be another hero's)
            AgathaShield.tick(player, data);
        }
        trapTick(player);
    }

    /** Every player: a drained hero lets go of whatever was held (the keys come up by themselves only for the shield and beam). */
    static void everyPlayerTick(ServerPlayer player, PowerData data) {
        if (player.hasEffect(ModEffects.DRAINED)) {
            PowerTicker.stopHoldAbilities(data);
            if (data.heldEntityId() != -1) {
                Telekinesis.release(player, false);
            }
        }
    }

    /**
     * Her body: 40 health (with the default bonus) while she is Agatha, and nothing of it on anyone else; 3.4: a custom hero
     * with her body gets the bonus at his strength ({@link PowerData#bodyScale}).
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean agatha = data.has(HeroAbility.AGATHA_BODY);
        double bonus = agatha ? AgathaConfig.BODY_HEALTH_BONUS.get() * data.bodyScale(HeroAbility.AGATHA_BODY) : 0.0;
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, bonus, agatha);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** She is losing the power: the beam goes out, the shield falls, the runes fade. */
    static void endAll(ServerPlayer player, PowerData data) {
        if (FIRING.remove(player.getUUID())) {
            ContinuousBeam.stop(player);
        }
        if (data.chaosShieldActive() && data.has(HeroAbility.AGATHA_SHIELD)) {
            data.setChaosShieldActive(false);
            AgathaShield.forget(player.getUUID());
        }
        if (CASTING_UNTIL.remove(player.getUUID()) != null) {
            data.set(Live.CASTING, false);
        }
        endTrap(player, true);
    }

    /** She left (logout), died or changed dimension: what she left burning goes out. */
    static void onGone(ServerPlayer player, boolean silently) {
        if (FIRING.remove(player.getUUID())) {
            if (silently) {
                ContinuousBeam.forget(player);
            } else {
                ContinuousBeam.stop(player);
            }
        }
        CASTING_UNTIL.remove(player.getUUID());
        READING.remove(player.getUUID());
        AgathaShield.forget(player.getUUID());
        endTrap(player, !silently);
    }

    public static void clear() {
        FIRING.clear();
        CASTING_UNTIL.clear();
        TRAPS.clear();
        READING.clear();
        AgathaShield.clear();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Becoming Agatha: reading the Darkhold

    /**
     * One tick of a player with no power reading the Darkhold ({@code held} ticks so far): its whispers rise, violet motes
     * spiral down into the reader, and at {@code agatha.awakenTicks} she wakes as Agatha. The book is kept.
     */
    public static void reading(ServerPlayer player, int held) {
        ServerLevel level = player.serverLevel();
        if (held == 0 || !READING.containsKey(player.getUUID())) {
            READING.put(player.getUUID(), level.getGameTime());
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.AGATHA_AWAKEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        int total = AgathaConfig.AWAKEN_TICKS.get();
        float progress = Mth.clamp(held / (float) total, 0.0F, 1.0F);
        double turn = held * 0.35;
        for (int i = 0; i < 2; i++) {
            double angle = turn + i * Math.PI;
            double radius = 1.1 - 0.6 * progress;
            double y = player.getY() + player.getBbHeight() * (1.3 - ((held * 0.04 + i * 0.5) % 1.0));
            // (For the others: a mote a block from the reader's own camera would be a blot over her screen.)
            PhoenixAbilities.sparksForOthers(level, player, new EnergyParticleOptions(i == 0 ? VIOLET : LILAC, 0.9F),
                    player.getX() + Math.cos(angle) * radius, y, player.getZ() + Math.sin(angle) * radius, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (held < total) {
            return;
        }
        READING.remove(player.getUUID());
        Powers.awaken(player, Power.AGATHA);
        level.sendParticles(new EnergyParticleOptions(VIOLET, 1.6F), player.getX(), player.getY() + 1.0, player.getZ(), 70, 0.5, 0.8, 0.5, 0.15);
        level.sendParticles(new EnergyParticleOptions(CORE, 1.0F), player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.3, 0.6, 0.3, 0.1);
        player.stopUsingItem();
    }

    /** The reading was cut short (use let go, the book put away): the whispers fall silent and nothing happens. */
    public static void stopReading(ServerPlayer player) {
        if (READING.remove(player.getUUID()) == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, 0.8F);
        ClientboundStopSoundPacket stop = new ClientboundStopSoundPacket(ModSounds.AGATHA_AWAKEN.getId(), SoundSource.PLAYERS);
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(player) <= 64.0 * 64.0) {
                other.connection.send(stop);
            }
        }
    }

    /** Whether this player is reading the Darkhold to become Agatha now. */
    public static boolean isReading(Player player) {
        return READING.containsKey(player.getUUID());
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: the Purple Beam; Sneak + R: the hex mark or the mind glimpse

    /** The numbers of her Purple Beam for one tick: the Darkhold's beam, with her own numbers. */
    public static BeamSpec beamSpec() {
        int interval = AgathaConfig.BEAM_INTERVAL.get();
        float perHit = AgathaConfig.BEAM_DAMAGE_PER_SECOND.get().floatValue() * interval / 20.0F;
        float exhaustion = AgathaConfig.BEAM_EXHAUSTION_PER_SECOND.get().floatValue() / 20.0F;
        return new BeamSpec(DarkholdItem.STYLE, AgathaConfig.BEAM_RANGE.get(), perHit, interval, exhaustion, ModDamageTypes.DARKHOLD_BEAM, null);
    }

    /** R down: the beam begins (if she is fed and no other beam of hers burns); R up: it ends. */
    private static void beamKey(ServerPlayer player, PowerData data, boolean down) {
        if (DarkholdBook.firing(player)) {
            return; // (the beam that burns is the book's, held in her hand: R neither starts nor stops it)
        }
        if (!down) {
            if (FIRING.remove(player.getUUID())) {
                ContinuousBeam.stop(player);
            }
            return;
        }
        if (ContinuousBeam.isFiring(player)) {
            return;
        }
        if (starving(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.agatha.hungry"), true);
            return;
        }
        FIRING.add(player.getUUID());
        ContinuousBeam.start(player, DarkholdItem.STYLE, AgathaConfig.BEAM_RANGE.get());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** One tick of her beam: it ends when something let go of it, or she can no longer feed it; otherwise it burns. */
    private static void beamTick(ServerPlayer player, PowerData data) {
        if (!FIRING.contains(player.getUUID())) {
            return;
        }
        if (!data.is(Live.BEAMING) || !ContinuousBeam.isFiring(player)) {
            // (Something that lets go of every held ability took the flag away, or the beam was stopped elsewhere.)
            FIRING.remove(player.getUUID());
            if (ContinuousBeam.isFiring(player)) {
                ContinuousBeam.stop(player);
            }
            return;
        }
        if (player.isSpectator() || starving(player)) {
            if (starving(player)) {
                player.displayClientMessage(Component.translatable("message.enhanced.agatha.hungry"), true);
            }
            FIRING.remove(player.getUUID());
            ContinuousBeam.stop(player);
            return;
        }
        ContinuousBeam.tick(player, beamSpec());
    }

    /** Sneak + R: the Darkhold's hex mark on the creature she looks at, or its mind glimpse; her hand rises with it. */
    private static void hex(ServerPlayer player, PowerData data) {
        if (DarkholdBook.hexOrGlimpse(player)) {
            CASTING_UNTIL.put(player.getUUID(), player.level().getGameTime() + CAST_TICKS);
            data.set(Live.CASTING, true);
            AbilityColors.mark(player,
                    HeroAbility.AGATHA_HEX_MARK); // (3.5: the mark's own colour, when one was chosen)
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // V: the Power Drain

    /** The living creature under her crosshair within {@code range}, or null (blocks stop the look). */
    @Nullable
    static LivingEntity sight(ServerPlayer player, double range) {
        HitResult hit = Raycasts.any(player, range, ContinuousBeam.targets(player), 1.0F);
        if (hit instanceof EntityHitResult entityHit) {
            Entity seen = entityHit.getEntity() instanceof PartEntity<?> part ? part.getParent() : entityHit.getEntity();
            if (seen instanceof LivingEntity living && !(seen instanceof ArmorStand)) {
                return living;
            }
        }
        return null;
    }

    /**
     * Why the Power Drain cannot take this target (a translation key), or null if it can: a player only where PvP lets her
     * hurt them, only if they have a power and (3.5, {@code agatha.drainOnlySorcerers}) only a witch's or sorcerer's
     * ({@link #drainable}); a creature only if an attack of hers may hurt it; nothing inside a ward.
     */
    @Nullable
    public static String drainRefusal(ServerPlayer agatha, LivingEntity target) {
        if (wardStops(agatha, target.position())) {
            return "message.enhanced.agatha.warded";
        }
        if (target instanceof Player other) {
            if (!AreaTargets.mayHurt(agatha, other, false)) {
                return "message.enhanced.agatha.drain_refused";
            }
            if (!Powers.power(other).hasPowers()) {
                return "message.enhanced.agatha.no_power";
            }
            return !AgathaConfig.DRAIN_ONLY_SORCERERS.get() || drainable(Powers.get(other)) ? null
                    : "message.enhanced.agatha.drain_not_sorcerer";
        }
        return AreaTargets.mayHurt(agatha, target, true) ? null : "message.enhanced.agatha.drain_refused";
    }

    /** The heroes whose power is a witch's or sorcerer's, which the Power Drain can take (3.5). */
    private static final Set<Power> DRAINABLE = EnumSet.of(Power.LOKI, Power.SCARLET_WITCH, Power.BILLY, Power.AGATHA);

    /**
     * Whether the Power Drain can take this hero's power (3.5): Loki, the Scarlet Witch, Billy, Agatha (as Wanda drained
     * her), or a custom hero with an ability of theirs. Not a hero who only holds the Darkhold: its magic is the book's,
     * and the drain takes the hero's own power.
     */
    public static boolean drainable(PowerData data) {
        if (data.power() == Power.CUSTOM) {
            HeroKit kit = data.kit();
            if (kit == null) {
                return false;
            }
            for (HeroAbility row : HeroAbility.values()) {
                if (DRAINABLE.contains(row.hero()) && kit.has(row)) {
                    return true;
                }
            }
            return false;
        }
        return DRAINABLE.contains(data.power());
    }

    /**
     * V: she draws the power out of the hero she looks at, who cannot use it for {@code drainTicks} (no ability key, no
     * flight: the effect Drained), or the life out of a creature ({@code drainDamage} of dark magic); either way she heals
     * {@code drainHeal}. A refusal costs nothing.
     */
    private static void drain(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.AGATHA_DRAIN, now)) {
            return;
        }
        LivingEntity target = sight(player, AgathaConfig.DRAIN_RANGE.get());
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.agatha.drain_nothing"), true);
            return;
        }
        String refusal = drainRefusal(player, target);
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal, target.getDisplayName()), true);
            return;
        }
        drainNow(player, data, target);
    }

    /** The drain itself, on a target already judged. */
    static void drainNow(ServerPlayer player, PowerData data, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        boolean power = target instanceof ServerPlayer;
        if (target instanceof ServerPlayer victim) {
            victim.addEffect(new MobEffectInstance(ModEffects.DRAINED, AgathaConfig.DRAIN_TICKS.get(), 0, false, true, true), player);
            victim.displayClientMessage(Component.translatable("message.enhanced.agatha.drained_you", player.getDisplayName())
                    .withStyle(s -> s.withColor(VIOLET)), true);
            player.displayClientMessage(Component.translatable("message.enhanced.agatha.drained", victim.getDisplayName(),
                    AgathaConfig.DRAIN_TICKS.get() / 20), true);
        } else {
            target.hurt(ModDamageTypes.source(level, ModDamageTypes.AGATHA_DRAIN, player, player), AgathaConfig.DRAIN_DAMAGE.get().floatValue());
        }
        player.heal(AgathaConfig.DRAIN_HEAL.get().floatValue());
        data.startCooldown(HeroAbility.AGATHA_DRAIN, now, AgathaConfig.DRAIN_COOLDOWN.get());
        pay(player, AgathaConfig.DRAIN_EXHAUSTION.get());
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.color(player, HeroAbility.AGATHA_DRAIN, VIOLET),
                1.2F), at.x, at.y, at.z, 14,
                0.3, 0.4, 0.3, 0.04);
        level.playSound(null, at.x, at.y, at.z, ModSounds.AGATHA_DRAIN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        send(level, new AgathaFxPayload(AgathaFxPayload.DRAIN, player.getId(), at, target.getId(), DRAIN_STREAM_TICKS, power ? 1 : 0));
    }

    // ------------------------------------------------------------------------------------------------------------
    // N: the Rune Trap

    /**
     * Where the circle goes: on the ground where she looks, within {@code runeRange} (looking at a wall, on the ground at
     * its foot), or round her own feet when she looks at nothing.
     */
    public static Vec3 runeCenter(ServerPlayer player) {
        BlockHitResult hit = Raycasts.block(player, AgathaConfig.RUNE_RANGE.get(), 1.0F);
        if (hit.getType() == HitResult.Type.MISS) {
            return player.position();
        }
        Vec3 at = hit.getLocation();
        if (hit.getDirection() == Direction.UP) {
            return new Vec3(at.x, hit.getBlockPos().getY() + 1.0, at.z);
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        for (int i = 0; i < GROUND_SEARCH; i++) {
            BlockPos below = pos.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return new Vec3(at.x, pos.getY(), at.z);
            }
            pos = below;
        }
        return at;
    }

    /** N: a circle of runes is laid on the ground (a new one puts out her last). */
    private static void runes(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.AGATHA_RUNES, now)) {
            return;
        }
        Vec3 center = runeCenter(player);
        if (wardStops(player, center)) {
            player.displayClientMessage(Component.translatable("message.enhanced.agatha.warded"), true);
            return;
        }
        lay(player, center);
        data.startCooldown(HeroAbility.AGATHA_RUNES, now, AgathaConfig.RUNE_COOLDOWN.get());
        pay(player, AgathaConfig.RUNE_EXHAUSTION.get());
    }

    /** The circle is laid at {@code center}, now: it binds from its first tick. */
    public static RuneTrap lay(ServerPlayer player, Vec3 center) {
        endTrap(player, true);
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        int ticks = AgathaConfig.RUNE_TICKS.get();
        double radius = AgathaConfig.RUNE_RADIUS.get();
        RuneTrap trap = new RuneTrap(level.dimension(), center, radius, now, now + ticks);
        TRAPS.put(player.getUUID(), trap);
        level.playSound(null, center.x, center.y, center.z, ModSounds.AGATHA_RUNES.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        send(level, new AgathaFxPayload(AgathaFxPayload.RUNES, player.getId(), center, 0, ticks, Math.round((float) radius * 10.0F)));
        pulse(player, trap, true, false); // (it binds from the moment it is laid)
        return trap;
    }

    /** Her runes put out (she laid new ones, lost the power, left); {@code tell}: the clients see them fade now. */
    private static void endTrap(ServerPlayer player, boolean tell) {
        RuneTrap trap = TRAPS.remove(player.getUUID());
        if (trap != null && tell && player.level().dimension() == trap.dimension()) {
            send(player.serverLevel(), new AgathaFxPayload(AgathaFxPayload.RUNES_END, player.getId(), trap.center(), 0, 0, 0));
        }
    }

    /** One tick of her circle: it binds what stands in it every half second and burns it every second, until it runs out. */
    private static void trapTick(ServerPlayer player) {
        RuneTrap trap = TRAPS.get(player.getUUID());
        if (trap == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (level.dimension() != trap.dimension() || now >= trap.end()) {
            TRAPS.remove(player.getUUID());
            return;
        }
        long age = now - trap.start();
        boolean bind = age % RUNE_PULSE == 0;
        boolean burn = age % 20 == 0 && age > 0;
        if (bind || burn) {
            pulse(player, trap, bind, burn);
        }
    }

    /** What stands in the circle is bound ({@code bind}) and burned ({@code burn}), if her magic may touch it. */
    private static void pulse(ServerPlayer player, RuneTrap trap, boolean bind, boolean burn) {
        ServerLevel level = player.serverLevel();
        float damage = AgathaConfig.RUNE_DAMAGE_PER_SECOND.get().floatValue();
        double r = trap.radius();
        AABB box = new AABB(trap.center().x - r, trap.center().y - RUNE_BELOW, trap.center().z - r,
                trap.center().x + r, trap.center().y + RUNE_HEIGHT, trap.center().z + r);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && trap.holds(e))) {
            if (!AreaTargets.mayHurt(player, e, false) || wardStops(player, e.position())) {
                continue;
            }
            if (bind && !DarkholdBook.bossLike(e) && !e.getType().is(ModTags.SPELLBOUND_IMMUNE)) {
                e.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, RUNE_BIND_TICKS, 0, false, false, true), player);
            }
            if (burn && damage > 0.0F) {
                e.hurt(ModDamageTypes.source(level, ModDamageTypes.AGATHA_RUNES, player, player), damage);
            }
        }
    }

    /** The creatures her runes hold now (for the tests and the HUD). */
    public static List<LivingEntity> bound(ServerPlayer player) {
        RuneTrap trap = TRAPS.get(player.getUUID());
        List<LivingEntity> held = new ArrayList<>();
        if (trap != null) {
            for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    new AABB(trap.center(), trap.center()).inflate(trap.radius(), RUNE_HEIGHT, trap.radius()), trap::holds)) {
                held.add(e);
            }
        }
        return held;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Effects

    static void send(ServerLevel level, AgathaFxPayload payload) {
        Net.sendToPlayersNear(level, null, payload.a().x, payload.a().y, payload.a().z, 96.0, payload);
    }

    private AgathaAbilities() {
    }
}
