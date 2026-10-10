package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.entity.MjolnirPinEntity;
import com.kolodkingal.enhanced.item.MjolnirOwner;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Mjolnir pins someone down (The Avengers: Thor sets his hammer on Loki's chest, and Loki can't get up). Sneak + right
 * click a creature or player with Mjolnir in hand, as its owner (or any worthy player, with {@code mjolnirOwnerOnly}
 * off): the hammer is set down on them. While it lies there they can't move, jump, fly, swim up, ride, teleport, or
 * glide on an elytra; they can still look around. Only the owner can lift it again (right click the pinned one, or call
 * it back with R), nobody else, the pinned one included. It lets go by itself after a minute (config), or when its owner
 * logs out, dies or changes dimension, or when the pinned one dies or leaves, and then it falls to the ground, its owner
 * kept. The bosses ({@code enhanced:pin_immune}) can't be pinned, nor can players PvP rules protect.
 * <p>
 * The hammer lies on them as a {@link MjolnirPinEntity}. This class holds the pinned one in place: creatures through the
 * tick events (before they tick they stop; after, they are put back), players each tick, because they move themselves,
 * and the pinned player's client stops their own movement as well ({@link Live#PINNED}).
 */
public final class MjolnirPin {
    /** How far (blocks) a pinned player's own movement may take them before the server puts them back. */
    private static final double PLAYER_DRIFT = 0.06;
    /** A teleport that leaves the player's looking direction alone. */
    private static final Set<RelativeMovement> KEEP_LOOKING = EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT);

    /** Who is pinned (by their UUID) → the hammer lying on them. Server only. */
    private static final Map<UUID, MjolnirPinEntity> BY_TARGET = new HashMap<>();

    // ---- right click ----

    /**
     * Right click on {@code clicked}: lifting Mjolnir off someone pinned (only its owner can; for anyone else it will not
     * budge), or, with Sneak and Mjolnir in {@code hand}, setting it down on them. Returns what the click did, or null if
     * it was none of this business. Run on both sides, so the client doesn't also use the hammer (the Bifrost).
     */
    @Nullable
    public static InteractionResult interact(Player player, InteractionHand hand, Entity clicked) {
        Entity entity = clicked instanceof PartEntity<?> part ? part.getParent() : clicked;
        if (!(entity instanceof LivingEntity target) || player.isSpectator()) {
            return null;
        }
        boolean client = player.level().isClientSide;
        MjolnirPinEntity pin = pinOf(target);
        if (pin != null) {
            if (player instanceof ServerPlayer lifter) {
                lift(lifter, target, pin);
            }
            return InteractionResult.sidedSuccess(client);
        }
        if (player.isShiftKeyDown() && ThorAbilities.isMjolnir(player.getItemInHand(hand)) && !(target instanceof ArmorStand)) {
            if (player instanceof ServerPlayer pinner) {
                pin(pinner, hand, target);
            }
            return InteractionResult.sidedSuccess(client);
        }
        return null;
    }

    /** Why Mjolnir can't pin this one, or null if it can. */
    @Nullable
    private static String refusal(ServerPlayer pinner, LivingEntity target) {
        if (target == pinner) {
            return "message.enhanced.mjolnir.cannot_pin";
        }
        if (target.getType().is(ModTags.PIN_IMMUNE)) {
            return "message.enhanced.mjolnir.pin_immune";
        }
        if (target instanceof Player victim) {
            if (victim.isCreative() || victim.isSpectator()) {
                return "message.enhanced.mjolnir.cannot_pin";
            }
            if (!pinner.canHarmPlayer(victim)) {
                return "message.enhanced.mjolnir.pin_no_pvp";
            }
        }
        return pinOf(target) != null ? "message.enhanced.mjolnir.already_pinned" : null;
    }

    /** Sets the Mjolnir in {@code hand} down on {@code target}. */
    public static void pin(ServerPlayer pinner, InteractionHand hand, LivingEntity target) {
        ItemStack held = pinner.getItemInHand(hand);
        if (!Worthiness.canCall(pinner, held)) {
            MjolnirOwner owner = Worthiness.ownerOf(held);
            pinner.displayClientMessage((owner != null ? Component.translatable("message.enhanced.mjolnir.not_yours", owner.name())
                    : Component.translatable("message.enhanced.mjolnir.unworthy")).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        String refusal = refusal(pinner, target);
        if (refusal != null) {
            pinner.displayClientMessage(Component.translatable(refusal, target.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ServerLevel level = pinner.serverLevel();
        // An unclaimed Mjolnir becomes the pinner's now: only its owner can lift it again.
        Worthiness.claim(pinner, held);
        ItemStack hammer = held.copyWithCount(1);
        pinner.setItemInHand(hand, ItemStack.EMPTY);
        MjolnirPinEntity pin = new MjolnirPinEntity(level, pinner.getUUID(), target, hammer, EnhancedConfig.MJOLNIR_PIN_DURATION.get());
        level.addFreshEntity(pin);
        BY_TARGET.put(target.getUUID(), pin);
        target.stopRiding();
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        if (target instanceof ServerPlayer victim) {
            Powers.get(victim).set(Live.PINNED, true);
            MjolnirOwner owner = Worthiness.ownerOf(hammer);
            victim.displayClientMessage(Component.translatable("message.enhanced.mjolnir.you_are_pinned",
                    owner != null ? owner.name() : pinner.getGameProfile().getName()).withStyle(ChatFormatting.GRAY), true);
        }
        pinner.swing(hand, true);
        pinner.displayClientMessage(Component.translatable("message.enhanced.mjolnir.pinned", target.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
        Vec3 at = pin.position();
        level.playSound(null, at.x, at.y, at.z, ModSounds.MJOLNIR_IMPACT.get(), SoundSource.PLAYERS, 1.2F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.5F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 30, 0.4, 0.3, 0.4, 0.4);
        level.sendParticles(new EnergyParticleOptions(Power.THOR.glow, 1.4F), at.x, at.y, at.z, 12, 0.3, 0.2, 0.3, 0.1);
    }

    /** The hammer lying on {@code target}, or null if it is not pinned. Works on both sides. */
    @Nullable
    public static MjolnirPinEntity pinOf(Entity target) {
        if (target.level().isClientSide) {
            return target.level().getEntitiesOfClass(MjolnirPinEntity.class, target.getBoundingBox().inflate(1.0),
                    p -> p.targetEntityId() == target.getId()).stream().findFirst().orElse(null);
        }
        if (BY_TARGET.isEmpty()) {
            return null;
        }
        MjolnirPinEntity pin = BY_TARGET.get(target.getUUID());
        if (pin != null && (pin.isRemoved() || pin.level() != target.level())) {
            BY_TARGET.remove(target.getUUID());
            return null;
        }
        return pin;
    }

    /** Whether Mjolnir lies on this entity (server side). */
    public static boolean isPinned(Entity entity) {
        return !entity.level().isClientSide && pinOf(entity) != null;
    }

    /** The owner lifts the hammer off {@code target}, into their hand; anyone else finds it will not budge. */
    private static void lift(ServerPlayer lifter, LivingEntity target, MjolnirPinEntity pin) {
        ServerLevel level = lifter.serverLevel();
        ItemStack hammer = pin.hammer();
        if (!Worthiness.canCall(lifter, hammer) || lifter.getUUID().equals(pin.targetUuid())) {
            MjolnirOwner owner = Worthiness.ownerOf(hammer);
            lifter.displayClientMessage((owner != null ? Component.translatable("message.enhanced.mjolnir.not_yours", owner.name())
                    : Component.translatable("message.enhanced.mjolnir.unworthy")).withStyle(ChatFormatting.GRAY), true);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.3F, 0.5F);
            return;
        }
        ItemStack lifted = takeHammer(level, pin);
        Worthiness.claim(lifter, lifted);
        if (lifter.getMainHandItem().isEmpty()) {
            lifter.setItemInHand(InteractionHand.MAIN_HAND, lifted);
        } else if (!lifter.getInventory().add(lifted)) {
            lifter.drop(lifted, false);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    // ---- letting go ----

    /** The pinned one is free, and the hammer falls to the ground beside them. */
    public static void release(ServerLevel level, MjolnirPinEntity pin) {
        LivingEntity target = targetOf(level, pin);
        Vec3 at = target != null ? target.position() : pin.holdAt();
        ThorAbilities.dropAt(level, at, end(pin, target));
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 0.8F);
    }

    /** The hammer is taken off (Thor called it, or its owner lifted it): the pinned one is free and the hammer is returned. */
    public static ItemStack takeHammer(ServerLevel level, MjolnirPinEntity pin) {
        return end(pin, targetOf(level, pin));
    }

    private static ItemStack end(MjolnirPinEntity pin, @Nullable LivingEntity target) {
        ItemStack hammer = pin.hammer();
        BY_TARGET.remove(pin.targetUuid(), pin);
        pin.spend();
        pin.discard();
        if (target instanceof ServerPlayer player) {
            Powers.get(player).set(Live.PINNED, false);
        }
        return hammer;
    }

    /** The one this hammer pins, if they are still here: alive, in this world, not a spectator. */
    @Nullable
    public static LivingEntity targetOf(ServerLevel level, MjolnirPinEntity pin) {
        return level.getEntity(pin.targetUuid()) instanceof LivingEntity living && living.isAlive() && !living.isSpectator() ? living : null;
    }

    /** The owner is gone (logged out, or died): everything they pinned is free. */
    public static void onOwnerGone(ServerPlayer owner) {
        for (MjolnirPinEntity pin : new ArrayList<>(BY_TARGET.values())) {
            if (pin.ownerId().equals(owner.getUUID()) && pin.level() instanceof ServerLevel level) {
                release(level, pin);
            }
        }
    }

    // ---- holding them down ----

    /** The pin's own tick: it lets go when its time is up or its owner is gone, and keeps the pinned one off any steed. */
    public static void tick(ServerLevel level, MjolnirPinEntity pin, LivingEntity target) {
        BY_TARGET.put(target.getUUID(), pin);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(pin.ownerId());
        boolean gone = owner == null || !owner.isAlive() || owner.level() != level;
        boolean protectedPlayer = target instanceof Player player && (player.isCreative() || player.isSpectator());
        if (gone || protectedPlayer || level.getGameTime() >= pin.endsAt()) {
            release(level, pin);
            return;
        }
        if (target.isPassenger()) {
            target.stopRiding();
        }
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        if (level.getGameTime() % 20 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pin.getX(), pin.getY(), pin.getZ(), 4, 0.4, 0.2, 0.4, 0.15);
        }
    }

    /** Before a pinned creature ticks it stands still (a fall excepted). */
    public static void beforeTick(Entity entity) {
        MjolnirPinEntity pin = pinnedCreature(entity);
        if (pin != null) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(0, Math.min(motion.y, 0), 0);
        }
    }

    /** After it ticked, a pinned creature is put back where it was pinned (it may have fallen, but it hasn't climbed). */
    public static void afterTick(Entity entity) {
        MjolnirPinEntity pin = pinnedCreature(entity);
        if (pin == null) {
            return;
        }
        Vec3 hold = lowered(pin, entity);
        Vec3 motion = entity.getDeltaMovement();
        if (Math.abs(entity.getX() - hold.x) > 1.0E-3 || Math.abs(entity.getZ() - hold.z) > 1.0E-3 || entity.getY() > hold.y + 1.0E-3) {
            Vec3 back = new Vec3(hold.x, Math.min(entity.getY(), hold.y), hold.z);
            if (entity.level().noCollision(entity, entity.getBoundingBox().move(back.subtract(entity.position())))) {
                entity.setPos(back);
            } else {
                // Something solid is in the way (a block placed under it, say): it is held here instead.
                pin.setHoldAt(entity.position());
            }
        }
        entity.setDeltaMovement(0, Math.min(motion.y, 0), 0);
    }

    /** A pinned creature that is not a player (players move themselves: {@link #tickPlayer}). */
    @Nullable
    private static MjolnirPinEntity pinnedCreature(Entity entity) {
        if (BY_TARGET.isEmpty() || entity.level().isClientSide || !(entity instanceof LivingEntity) || entity instanceof Player) {
            return null;
        }
        return pinOf(entity);
    }

    /** Where the pinned one is held, lowered if they have fallen. */
    private static Vec3 lowered(MjolnirPinEntity pin, Entity entity) {
        Vec3 hold = pin.holdAt();
        if (entity.getY() < hold.y - 1.0E-4) {
            hold = new Vec3(hold.x, entity.getY(), hold.z);
            pin.setHoldAt(hold);
        }
        return hold;
    }

    /**
     * Every player, every tick: a pinned player is marked for their own client (which stops them moving), can't fly, glide
     * or ride, and is put back if they got anywhere all the same.
     */
    public static void tickPlayer(ServerPlayer player) {
        MjolnirPinEntity pin = BY_TARGET.isEmpty() ? null : pinOf(player);
        PowerData data = Powers.get(player);
        data.set(Live.PINNED, pin != null);
        if (pin == null) {
            return;
        }
        Abilities abilities = player.getAbilities();
        if (abilities.flying) {
            abilities.flying = false;
            player.onUpdateAbilities();
        }
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        if (player.isPassenger()) {
            player.stopRiding();
        }
        Vec3 hold = lowered(pin, player);
        if (Math.abs(player.getX() - hold.x) > PLAYER_DRIFT || Math.abs(player.getZ() - hold.z) > PLAYER_DRIFT || player.getY() > hold.y + PLAYER_DRIFT) {
            Vec3 back = new Vec3(hold.x, Math.min(player.getY(), hold.y), hold.z);
            if (player.serverLevel().noCollision(player, player.getBoundingBox().move(back.subtract(player.position())))) {
                player.connection.teleport(back.x, back.y, back.z, player.getYRot(), player.getXRot(), KEEP_LOOKING);
                player.setDeltaMovement(0, Math.min(player.getDeltaMovement().y, 0), 0);
                player.hurtMarked = true;
            } else {
                pin.setHoldAt(player.position());
            }
        }
    }

    public static void clear() {
        BY_TARGET.clear();
    }

    private MjolnirPin() {
    }
}
