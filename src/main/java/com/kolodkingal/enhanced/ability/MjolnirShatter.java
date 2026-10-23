package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mjolnir shatters (Thor: Love and Thunder, where Jane's Mjolnir bursts into dozens of glowing pieces that each strike
 * an enemy and then fly back to reassemble). Sneak + R with Mjolnir in hand: it bursts into fragments, one for each
 * enemy Thor can see within 16 blocks (hostile monsters, and anyone who is going after him or has just hurt him; the
 * nearest 12), or a ring of them if there is nobody; each strikes its enemy ({@link MjolnirFragmentEntity}) and flies
 * home, and when the last one is back the hammer is whole again in his hand.
 * <p>
 * While it is in pieces the hammer is kept in Thor's own saved data (not in the world), so it can neither be lost nor
 * copied. If Thor dies, logs out, changes dimension or loses his power meanwhile, it comes together where he was and
 * falls to the ground, its owner kept.
 */
public final class MjolnirShatter {
    /** How far (blocks) from Thor an enemy is sent a fragment. */
    public static final double RANGE = 16.0;
    /** The most fragments there are: one for each of the nearest twelve enemies. */
    public static final int MAX_FRAGMENTS = 12;
    /** Fragments in the ring that bursts out when there is nobody to strike. */
    public static final int RING_FRAGMENTS = 12;
    private static final double RING_RADIUS = 6.0;
    /** However it goes, the hammer is whole again after this many ticks. */
    private static final int MAX_TICKS = 200;
    /** Someone who hurt Thor this recently (ticks) counts as attacking him. */
    private static final int ATTACKER_MEMORY = 100;

    private static final Map<UUID, Shattered> ACTIVE = new HashMap<>();
    /** Thor → the game time from which he may shatter Mjolnir again. */
    private static final Map<UUID, Long> READY = new HashMap<>();

    /** A hammer in pieces: the hand it came from, the fragments in the air, and where Thor was last. */
    private static final class Shattered {
        final ItemStack hammer;
        final InteractionHand hand;
        final List<UUID> fragments;
        final long startedAt;
        ResourceKey<Level> dimension;
        Vec3 lastPos;

        Shattered(ItemStack hammer, InteractionHand hand, List<UUID> fragments, long startedAt, ResourceKey<Level> dimension, Vec3 lastPos) {
            this.hammer = hammer;
            this.hand = hand;
            this.fragments = fragments;
            this.startedAt = startedAt;
            this.dimension = dimension;
            this.lastPos = lastPos;
        }
    }

    // ---- shattering ----

    /** Sneak + R with Mjolnir in {@code hand}: it shatters, if it has gathered itself since the last time. */
    public static void shatter(ServerPlayer thor, PowerData data, InteractionHand hand) {
        UUID id = thor.getUUID();
        ServerLevel level = thor.serverLevel();
        long now = level.getGameTime();
        if (ACTIVE.containsKey(id)) {
            return;
        }
        long ready = READY.getOrDefault(id, 0L);
        if (now < ready) {
            thor.displayClientMessage(Component.translatable("message.enhanced.mjolnir.shatter_not_ready", (ready - now + 19) / 20), true);
            return;
        }
        ItemStack hammer = thor.getItemInHand(hand).copyWithCount(1);
        Worthiness.claim(thor, hammer);
        List<LivingEntity> enemies = enemies(thor);
        Vec3 from = muzzle(thor);
        RandomSource random = thor.getRandom();
        int count = enemies.isEmpty() ? RING_FRAGMENTS : enemies.size();
        List<UUID> fragments = new ArrayList<>();
        int first = -1;
        for (int i = 0; i < count; i++) {
            double angle = (i + random.nextDouble() * 0.6) * Mth.TWO_PI / count;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 burst = new Vec3(cos * 0.9, 0.3 + random.nextDouble() * 0.5, sin * 0.9);
            Vec3 ring = from.add(cos * RING_RADIUS, 0.5 + random.nextDouble() * 1.5, sin * RING_RADIUS);
            MjolnirFragmentEntity fragment = new MjolnirFragmentEntity(level, thor, from, burst, enemies.isEmpty() ? null : enemies.get(i), ring);
            level.addFreshEntity(fragment);
            fragments.add(fragment.getUUID());
            if (first == -1) {
                first = fragment.getId();
            }
        }
        thor.setItemInHand(hand, ItemStack.EMPTY);
        ModAttachments.SHATTERED_MJOLNIR.set(thor, hammer);
        ACTIVE.put(id, new Shattered(hammer, hand, fragments, now, level.dimension(), thor.position()));
        READY.put(id, now + EnhancedConfig.MJOLNIR_SHATTER_COOLDOWN.get());
        // Counted as a weapon in flight: Thor keeps flying while it is in the air.
        data.setHammerEntityId(first);
        thor.swing(hand, true);
        level.playSound(null, thor.getX(), thor.getY(), thor.getZ(), ModSounds.MJOLNIR_THROW.get(), SoundSource.PLAYERS, 1.4F, 1.5F);
        level.playSound(null, thor.getX(), thor.getY(), thor.getZ(), ModSounds.MJOLNIR_IMPACT.get(), SoundSource.PLAYERS, 0.9F, 0.7F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 40, 0.3, 0.3, 0.3, 0.5);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.color(thor, HeroAbility.THOR_SHATTER, Power.THOR.glow), 1.6F), from.x, from.y, from.z, 24,
                0.25, 0.25, 0.25, 0.3);
    }

    /**
     * 3.5 (Hela's lane): a thrown Mjolnir caught in Hela's hand and crushed at {@code at} (the moment of Ragnarok). The
     * hammer, already out of Thor's hand (the thrown weapon is gone), bursts into fragments there as his own Sneak + R
     * makes it burst: they strike his enemies and fly home, and the last one back makes it whole in his hand. Thor's own
     * cooldown of the shatter is neither asked nor started. Returns false (and nothing happens) if his hammer is already in
     * pieces: the caller then gives the hammer back as it would.
     */
    public static boolean shatterCaught(ServerPlayer thor, ItemStack hammer, Vec3 at) {
        UUID id = thor.getUUID();
        if (ACTIVE.containsKey(id)) {
            return false;
        }
        ServerLevel level = thor.serverLevel();
        long now = level.getGameTime();
        ItemStack whole = hammer.copyWithCount(1);
        Worthiness.claim(thor, whole);
        List<LivingEntity> enemies = enemies(thor);
        RandomSource random = thor.getRandom();
        int count = enemies.isEmpty() ? RING_FRAGMENTS : enemies.size();
        List<UUID> fragments = new ArrayList<>();
        int first = -1;
        for (int i = 0; i < count; i++) {
            double angle = (i + random.nextDouble() * 0.6) * Mth.TWO_PI / count;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 burst = new Vec3(cos * 0.9, 0.3 + random.nextDouble() * 0.5, sin * 0.9);
            Vec3 ring = at.add(cos * RING_RADIUS, 0.5 + random.nextDouble() * 1.5, sin * RING_RADIUS);
            MjolnirFragmentEntity fragment = new MjolnirFragmentEntity(level, thor, at, burst, enemies.isEmpty() ? null : enemies.get(i), ring);
            level.addFreshEntity(fragment);
            fragments.add(fragment.getUUID());
            if (first == -1) {
                first = fragment.getId();
            }
        }
        ModAttachments.SHATTERED_MJOLNIR.set(thor, whole);
        ACTIVE.put(id, new Shattered(whole, InteractionHand.MAIN_HAND, fragments, now, level.dimension(), thor.position()));
        Powers.get(thor).setHammerEntityId(first);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 40, 0.3, 0.3, 0.3, 0.5);
        level.sendParticles(new EnergyParticleOptions(Power.THOR.glow, 1.6F), at.x, at.y, at.z, 24, 0.25, 0.25, 0.25, 0.3);
        return true;
    }

    /**
     * Where the fragments burst out and come together: out in front of him, where the hammer is held. (A burst of
     * sparks at his eyes would fill the whole screen of his first-person view.)
     */
    private static Vec3 muzzle(ServerPlayer thor) {
        return thor.getEyePosition().add(thor.getLookAngle().scale(0.9)).subtract(0, 0.5, 0);
    }

    /**
     * The enemies Thor can see within {@link #RANGE} blocks, the nearest {@link #MAX_FRAGMENTS} of them, nearest first.
     */
    public static List<LivingEntity> enemies(ServerPlayer thor) {
        List<LivingEntity> found = new ArrayList<>(thor.level().getEntitiesOfClass(LivingEntity.class, thor.getBoundingBox().inflate(RANGE),
                e -> isEnemy(thor, e)));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(thor)));
        return found.size() > MAX_FRAGMENTS ? new ArrayList<>(found.subList(0, MAX_FRAGMENTS)) : found;
    }

    /**
     * A hostile monster, or a creature or player going after Thor right now or who hurt him lately, that he can see.
     * Not players who are in Creative or Spectator mode, or whom PvP rules out, and not his own pets.
     */
    private static boolean isEnemy(ServerPlayer thor, LivingEntity e) {
        if (e == thor || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.distanceToSqr(thor) > RANGE * RANGE) {
            return false;
        }
        if (e instanceof Player player && (player.isCreative() || !thor.canHarmPlayer(player))) {
            return false;
        }
        if (e instanceof TamableAnimal pet && pet.isOwnedBy(thor)) {
            return false;
        }
        boolean hostile = e instanceof Enemy;
        boolean goingAfterHim = e instanceof Mob mob && mob.getTarget() == thor;
        boolean hurtHim = thor.getLastHurtByMob() == e && thor.tickCount - thor.getLastHurtByMobTimestamp() <= ATTACKER_MEMORY;
        return (hostile || goingAfterHim || hurtHim) && thor.hasLineOfSight(e);
    }

    /** R while the hammer is in pieces: every fragment turns home at once. Returns false if it is not shattered. */
    public static boolean callBack(ServerPlayer thor) {
        Shattered shattered = ACTIVE.get(thor.getUUID());
        if (shattered == null) {
            return false;
        }
        for (UUID id : shattered.fragments) {
            if (thor.serverLevel().getEntity(id) instanceof MjolnirFragmentEntity fragment) {
                fragment.setReturning();
            }
        }
        return true;
    }

    public static boolean isShattered(Player player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    // ---- putting it together again ----

    /** Every player, every tick: the hammer is whole again once the last fragment is home. */
    public static void tick(ServerPlayer player) {
        Shattered shattered = ACTIVE.get(player.getUUID());
        if (shattered == null) {
            // A hammer still in pieces in the player's data, but with no fragments to bring it together (the world was
            // closed or crashed meanwhile): it simply comes back.
            ItemStack stored = ModAttachments.SHATTERED_MJOLNIR.getExisting(player).orElse(ItemStack.EMPTY);
            if (!stored.isEmpty()) {
                ModAttachments.SHATTERED_MJOLNIR.remove(player);
                giveBack(player, stored.copy(), InteractionHand.MAIN_HAND);
            }
            return;
        }
        if (player.level().dimension() != shattered.dimension) {
            scatter(player.server, player.getUUID(), shattered, player);
            return;
        }
        shattered.lastPos = player.position();
        ServerLevel level = player.serverLevel();
        int alive = -1;
        for (UUID id : shattered.fragments) {
            if (level.getEntity(id) instanceof MjolnirFragmentEntity fragment && fragment.isAlive()) {
                alive = fragment.getId();
                break;
            }
        }
        PowerData data = Powers.get(player);
        if (alive == -1 || level.getGameTime() - shattered.startedAt > MAX_TICKS) {
            reassemble(player, data, shattered);
        } else {
            data.setHammerEntityId(alive);
        }
    }

    /** The last fragment is home: Mjolnir, whole, in the hand it came from (or in his inventory). */
    private static void reassemble(ServerPlayer thor, PowerData data, Shattered shattered) {
        ServerLevel level = thor.serverLevel();
        discardFragments(thor.server, shattered);
        ACTIVE.remove(thor.getUUID());
        ModAttachments.SHATTERED_MJOLNIR.remove(thor);
        if (data.hammerEntityId() != -1) {
            data.setHammerEntityId(-1);
        }
        giveBack(thor, shattered.hammer.copy(), shattered.hand);
        level.playSound(null, thor.getX(), thor.getY(), thor.getZ(), ModSounds.MJOLNIR_RETURN.get(), SoundSource.PLAYERS, 1.2F, 1.2F);
        Vec3 hand = muzzle(thor);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 24, 0.25, 0.25, 0.25, 0.3);
        level.sendParticles(new EnergyParticleOptions(AbilityColors.color(thor, HeroAbility.THOR_SHATTER, Power.THOR.glow), 1.4F), hand.x, hand.y, hand.z, 10,
                0.2, 0.2, 0.2, 0.15);
    }

    /** Mjolnir back in the hand it came from, otherwise in the inventory, otherwise at his feet. */
    private static void giveBack(ServerPlayer thor, ItemStack hammer, InteractionHand preferred) {
        Worthiness.claim(thor, hammer);
        if (thor.getItemInHand(preferred).isEmpty()) {
            thor.setItemInHand(preferred, hammer);
        } else if (!thor.getInventory().add(hammer)) {
            thor.drop(hammer, false);
        }
    }

    // ---- when Thor is gone ----

    /** Thor dies or logs out: the hammer comes together where he was and falls. */
    public static void onGone(ServerPlayer thor) {
        Shattered shattered = ACTIVE.get(thor.getUUID());
        if (shattered != null) {
            scatter(thor.server, thor.getUUID(), shattered, thor);
            return;
        }
        ItemStack stored = ModAttachments.SHATTERED_MJOLNIR.getExisting(thor).orElse(ItemStack.EMPTY);
        if (!stored.isEmpty()) {
            ModAttachments.SHATTERED_MJOLNIR.remove(thor);
            ThorAbilities.dropAt(thor.serverLevel(), thor.position(), stored.copy());
        }
    }

    /** The hammer comes together where Thor last was, in the world he was in, and falls to the ground. */
    private static void scatter(MinecraftServer server, UUID id, Shattered shattered, @Nullable ServerPlayer thor) {
        discardFragments(server, shattered);
        ACTIVE.remove(id);
        if (thor != null) {
            ModAttachments.SHATTERED_MJOLNIR.remove(thor);
            Powers.get(thor).setHammerEntityId(-1);
        }
        ServerLevel level = server.getLevel(shattered.dimension);
        if (level != null) {
            ThorAbilities.dropAt(level, shattered.lastPos, shattered.hammer.copy());
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, shattered.lastPos.x, shattered.lastPos.y + 1.0, shattered.lastPos.z, 20, 0.3, 0.3, 0.3, 0.3);
        }
    }

    private static void discardFragments(MinecraftServer server, Shattered shattered) {
        ServerLevel level = server.getLevel(shattered.dimension);
        if (level == null) {
            return;
        }
        for (UUID id : shattered.fragments) {
            Entity fragment = level.getEntity(id);
            if (fragment != null) {
                fragment.discard();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
        READY.clear();
    }

    private MjolnirShatter() {
    }
}
