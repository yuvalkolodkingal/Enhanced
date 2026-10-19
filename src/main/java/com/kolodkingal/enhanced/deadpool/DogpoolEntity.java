package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dogpool (3.7): Deadpool's dog in a red-and-black hood and goggles (Deadpool &amp; Wolverine), on the vanilla wolf's
 * body and goals. N whistles him out of a puff of red smoke beside Deadpool ({@link #whistle}): 20 health, a bite of 4,
 * fast; he follows him, bites what Deadpool hits and what attacks him (the wolf's own goals, his owner's), never a player
 * without PvP and never what the heroes' rule spares ({@link AreaTargets#mayHurt}). He stays {@code dogpool.seconds}
 * or until N again, one at a time; at 0 health he "needs a nap" and poofs (he never dies on screen); he poofs too when
 * Deadpool leaves, changes dimension or loses the power. Nobody can sit, feed, breed or lead him.
 */
public class DogpoolEntity extends Wolf {
    /** The game time he goes home at. */
    private long until = Long.MAX_VALUE;

    public DogpoolEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35);
    }

    /** He is Deadpool's from now until {@code until}, with the server's health and bite. */
    void setup(ServerPlayer owner, long until) {
        setTame(true, false);
        setOwnerUUID(owner.getUUID());
        this.until = until;
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(DeadpoolAbilities.cfg(DeadpoolConfig.DOGPOOL_HEALTH, 20.0));
        }
        var bite = getAttribute(Attributes.ATTACK_DAMAGE);
        if (bite != null) {
            bite.setBaseValue(DeadpoolAbilities.cfg(DeadpoolConfig.DOGPOOL_BITE_DAMAGE, 4.0));
        }
        setHealth(getMaxHealth());
    }

    /** The game time he goes home at. */
    public long until() {
        return until;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        LivingEntity owner = getOwner();
        if (!(owner instanceof ServerPlayer player) || !player.isAlive() || player.level() != server) {
            poof(false);
            return;
        }
        Optional<UUID> his = DeadpoolAbilities.data(player).dogpool();
        if (server.getGameTime() >= until || his.isEmpty() || !his.get().equals(getUUID())
                || !Powers.get(player).has(HeroAbility.DEADPOOL_DOGPOOL)) {
            poof(true);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel) {
            poof(true); // (he never dies on screen: he needs a nap)
            return;
        }
        super.die(source);
    }

    /** A puff of red smoke and he is gone home; Deadpool's state forgets him, his owner is told. */
    public void poof(boolean tell) {
        if (!(level() instanceof ServerLevel server) || isRemoved()) {
            return;
        }
        Vec3 at = position();
        server.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.4, at.z, 14, 0.3, 0.3, 0.3, 0.02);
        server.sendParticles(new DustParticleOptions(new Vector3f(0.78F, 0.06F, 0.18F), 1.3F), at.x, at.y + 0.4,
                at.z, 12, 0.35, 0.3, 0.35, 0.0);
        server.playSound(null, at.x, at.y, at.z, ModSounds.DOGPOOL_POOF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        DeadpoolAbilities.send(server, this, DeadpoolFxPayload.DOGPOOL_OUT, List.of(at), 0);
        if (getOwner() instanceof ServerPlayer owner) {
            DeadpoolState state = DeadpoolAbilities.data(owner);
            if (state.dogpool().isPresent() && state.dogpool().get().equals(getUUID())) {
                DeadpoolAbilities.setData(owner, state.withDogpool(Optional.empty(), 0L));
            }
            if (tell) {
                owner.displayClientMessage(Component.translatable("message.enhanced.deadpool.dogpool_nap")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        discard();
    }

    // ---- what he is not: a pet anyone may sit, feed, breed or lead ----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Nullable
    @Override
    public Wolf getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ---- whom he bites ----

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        return super.wantsToAttack(target, owner) && AreaTargets.mayHurt(owner, target, false);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        LivingEntity owner = getOwner();
        if (owner != null && (target instanceof Player || target == owner)) {
            return target != owner && AreaTargets.mayHurt(owner, target, false) && super.canAttack(target);
        }
        return super.canAttack(target);
    }

    /** Whether Dogpool may bite {@code target}: his owner's rule, and never his owner. */
    public boolean mayBite(Entity target) {
        LivingEntity owner = getOwner();
        if (!(target instanceof LivingEntity living) || target == owner) {
            return false;
        }
        return owner == null || AreaTargets.mayHurt(owner, living, false) || !(target instanceof Player);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.DOGPOOL_WHINE.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("dogpool_until", until);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        until = tag.contains("dogpool_until") ? tag.getLong("dogpool_until") : 0L;
    }

    // ---- Deadpool's side ----

    /**
     * N: a whistle and Dogpool comes out of a puff of red smoke beside him (N's slot rested; one at a time); N while he
     * is out sends him home.
     */
    static void whistle(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.DEADPOOL_DOGPOOL)) {
            return;
        }
        if (find(player) != null) {
            sendHome(player);
            return;
        }
        if (!DeadpoolAbilities.cfg(DeadpoolConfig.DOGPOOL_ENABLED, true)) {
            player.displayClientMessage(Component.translatable("message.enhanced.deadpool.dogpool_off")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.DEADPOOL_DOGPOOL, now)) {
            return;
        }
        DogpoolEntity dog = summon(player, now);
        if (dog == null) {
            return;
        }
        data.startCooldown(HeroAbility.DEADPOOL_DOGPOOL, now, DeadpoolAbilities.cfg(DeadpoolConfig.DOGPOOL_COOLDOWN,
                1200));
    }

    /** Dogpool beside him, his for {@code dogpool.seconds}; null if there is no room at all. */
    @Nullable
    static DogpoolEntity summon(ServerPlayer player, long now) {
        ServerLevel level = player.serverLevel();
        DogpoolEntity dog = ModEntities.DOGPOOL.get().create(level);
        if (dog == null) {
            return null;
        }
        Vec3 side = player.getLookAngle().yRot((float) Math.toRadians(70.0)).multiply(1.0, 0.0, 1.0);
        BlockPos wanted = BlockPos.containing(player.position().add(side.lengthSqr() < 1.0E-4 ? Vec3.ZERO
                : side.normalize().scale(1.5)));
        BlockPos spot = TeleportBelt.nearestSafe(level, player, wanted, 2);
        Vec3 at = spot != null ? Vec3.atBottomCenterOf(spot) : player.position();
        dog.moveTo(at.x, at.y, at.z, player.getYRot(), 0.0F);
        long until = now + 20L * DeadpoolAbilities.cfg(DeadpoolConfig.DOGPOOL_SECONDS, 120);
        dog.setup(player, until);
        level.addFreshEntity(dog);
        DeadpoolAbilities.setData(player, DeadpoolAbilities.data(player).withDogpool(Optional.of(dog.getUUID()),
                until));
        level.sendParticles(new DustParticleOptions(new Vector3f(0.78F, 0.06F, 0.18F), 1.5F), at.x, at.y + 0.4,
                at.z, 20, 0.4, 0.4, 0.4, 0.0);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.4, at.z, 10, 0.3, 0.3, 0.3, 0.02);
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.DOGPOOL_IN, List.of(at), dog.getId());
        DeadpoolAbilities.sound(level, player.position(), ModSounds.DEADPOOL_WHISTLE.get(), 1.0F, 1.0F);
        level.playSound(null, at.x, at.y, at.z, ModSounds.DOGPOOL_BARK.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        DeadpoolLines.say(player, DeadpoolLines.Group.DOGPOOL, true);
        return dog;
    }

    /** His Dogpool, if one is out and loaded in his level. */
    @Nullable
    public static DogpoolEntity find(ServerPlayer player) {
        Optional<UUID> id = DeadpoolAbilities.data(player).dogpool();
        if (id.isEmpty()) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(id.get());
        return entity instanceof DogpoolEntity dog && dog.isAlive() ? dog : null;
    }

    /** Dogpool goes home now (N again, the power lost, Deadpool gone). */
    static void sendHome(ServerPlayer player) {
        DogpoolEntity dog = find(player);
        if (dog != null) {
            dog.poof(false);
        }
        DeadpoolState state = DeadpoolAbilities.data(player);
        if (state.dogpool().isPresent()) {
            DeadpoolAbilities.setData(player, state.withDogpool(Optional.empty(), 0L));
        }
    }

    /** Every tick of his: Dogpool's time ran out where his body is not loaded. */
    static void ownerTick(ServerPlayer player, PowerData data, long now) {
        DeadpoolState state = DeadpoolAbilities.data(player);
        if (state.dogpool().isPresent() && now >= state.dogpoolUntil() && find(player) == null) {
            DeadpoolAbilities.setData(player, state.withDogpool(Optional.empty(), 0L));
        }
    }
}
