package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.config.FalconConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Falcon's Exo-Wings, the API the shared seams call (3.8, PLAN 8.2-8.3): {@code PowerTicker.applyPassives} asks {@link
 * #canFly} and {@link #flightSpeed}; the serum asks {@link #carries} (the Falcon card is locked without the wings); the
 * item asks {@link #canElytraFly}. Every wing move asks {@link #ready}.
 *
 * <p>The wings fly for Falcon while they are on his chest and not broken (down to their last point of durability, as
 * an elytra). G is the creative-style flight of the mod's heroes; Sneak + G the Glide: vanilla's fall-flying, the wings
 * being his elytra ({@code Live.FALCON_GLIDING} while he glides; a jump in the air starts it too, as an elytra's);
 * firework rockets push him as they push an elytra's wearer, and his nozzles give a small boost of their own every
 * {@code glideBoostTicks} while he looks up. Behind the Wing Shield he hovers still (his flight's speed is 0). For
 * anyone else the wings are a chest plate with folded wings.
 */
public final class FalconWings {
    /** How long (ticks) after Sneak + G on the ground he may still start gliding at the top of his hop. */
    static final int GLIDE_HOP_TICKS = 30;
    /** The hop of a glide started on the ground. */
    static final double GLIDE_HOP = 0.9;
    /** He must look this far up (degrees above the horizon) for the nozzles' glide boost. */
    static final float BOOST_PITCH = -10.0F;

    /** Whether he carries the Exo-Wings anywhere: the inventory, the chest slot, the off hand (the serum's rule). */
    public static boolean carries(Player player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.FALCON_EXO_WINGS.get()));
    }

    /** Whether the Exo-Wings are on his chest slot (every wing move needs them there). */
    public static boolean worn(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.FALCON_EXO_WINGS.get());
    }

    /** Whether a stack is a pair of Exo-Wings that still works (not down to its last point, as an elytra). */
    public static boolean usable(ItemStack stack) {
        return stack.is(ModItems.FALCON_EXO_WINGS.get()) && stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    /** Whether working wings are on his chest. */
    public static boolean wornUsable(Player player) {
        return usable(player.getItemBySlot(EquipmentSlot.CHEST));
    }

    /**
     * Whether the player has wings to move with: Falcon with working wings on his chest; a custom hero with any of
     * Falcon's rows always (the rows bring the wings, drawn without an item: PLAN 11.1).
     */
    public static boolean hasWings(Player player, PowerData data) {
        if (data.power() == Power.FALCON) {
            return wornUsable(player);
        }
        return data.power() == Power.CUSTOM && FalconAbilities.anyRow(data);
    }

    /**
     * Whether a wing move may go: {@link #hasWings}, or else (with {@code say}) "Put on your Exo-Wings!" (or that they
     * are broken) over his hotbar.
     */
    static boolean ready(ServerPlayer player, PowerData data, boolean say) {
        if (hasWings(player, data)) {
            return true;
        }
        if (say) {
            FalconAbilities.say(player, worn(player) ? "message.enhanced.falcon.wings_broken"
                    : "message.enhanced.falcon.needs_wings");
        }
        return false;
    }

    /** Whether Falcon may fly now (PLAN 8.3): wearing the wings, not broken. (Shielding, he hovers: speed 0.) */
    public static boolean canFly(ServerPlayer player) {
        return wornUsable(player);
    }

    /** Falcon's flight speed: {@code [falcon] flightSpeed}, 0 behind the Wing Shield (he hovers still). */
    public static float flightSpeed(ServerPlayer player) {
        if (Powers.get(player).is(Live.FALCON_WING_SHIELD)) {
            return 0.0F;
        }
        return (float) FalconAbilities.cfg(FalconConfig.FLIGHT_SPEED, 0.10);
    }

    /**
     * Whether the wings are an elytra for this wearer: Falcon himself, or a custom hero with Falcon's body (PLAN 8.2);
     * for anyone else a chest plate with folded wings. Asked on both sides (the client starts the glide on a jump).
     */
    public static boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player) || !usable(stack)) {
            return false;
        }
        PowerData data = Powers.get(player);
        return data.power() == Power.FALCON || data.power() == Power.CUSTOM && data.has(HeroAbility.FALCON_BODY);
    }

    /** Whether the wings keep him from fall damage and from a wall he glides into (Falcon or his body's card). */
    public static boolean saves(Player player, PowerData data) {
        return data.has(HeroAbility.FALCON_BODY) && hasWings(player, data);
    }

    // ---- the glide ----

    /** Sneak + G: the Glide on (from the ground he hops first) or off (he hovers on if he may fly). */
    static void toggleGlide(ServerPlayer player, PowerData data) {
        FalconAbilities.State state = FalconAbilities.state(player.getUUID());
        if (player.isFallFlying() || state.glideAsked >= 0) {
            endGlide(player, true);
            return;
        }
        Abilities abilities = player.getAbilities();
        if (abilities.flying) {
            abilities.flying = false;
            player.onUpdateAbilities();
        }
        if (player.onGround() || player.isInWater()) {
            Vec3 m = player.getDeltaMovement();
            player.setDeltaMovement(m.x, GLIDE_HOP, m.z);
            player.hurtMarked = true;
            state.glideAsked = player.level().getGameTime();
            FalconAbilities.sound(player.serverLevel(), player.position(), ModSounds.FALCON_THRUST.get(), 0.7F, 1.2F);
            return;
        }
        startGlide(player);
    }

    /** Spreads the wings into a glide (vanilla's fall-flying, if the wings let him). */
    static boolean startGlide(ServerPlayer player) {
        FalconAbilities.State state = FalconAbilities.state(player.getUUID());
        state.glideAsked = -1L;
        if (!canElytraFly(player.getItemBySlot(EquipmentSlot.CHEST), player)) {
            return false;
        }
        player.startFallFlying();
        FalconAbilities.sound(player.serverLevel(), player.position(), ModSounds.FALCON_WINGS_OPEN.get(), 0.9F, 1.0F);
        return true;
    }

    /** The glide ends; with {@code hover}, he goes on flying where he is (if he may fly and is in the air). */
    static void endGlide(ServerPlayer player, boolean hover) {
        FalconAbilities.State state = FalconAbilities.peek(player.getUUID());
        if (state != null) {
            state.glideAsked = -1L;
        }
        if (!player.isFallFlying()) {
            return;
        }
        player.stopFallFlying();
        FalconAbilities.sound(player.serverLevel(), player.position(), ModSounds.FALCON_WINGS_CLOSE.get(), 0.7F, 1.0F);
        if (hover && player.mayFly() && !player.onGround()) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
    }

    /** Every tick: the glide's state, a glide asked for from the ground, the nozzles' boost. */
    static void tick(ServerPlayer player, PowerData data, FalconAbilities.State state, long now) {
        boolean gliding = player.isFallFlying() && hasWings(player, data);
        data.set(Live.FALCON_GLIDING, gliding);
        if (state.glideAsked >= 0) {
            if (now - state.glideAsked > GLIDE_HOP_TICKS || !hasWings(player, data)) {
                state.glideAsked = -1L;
            } else if (now - state.glideAsked >= 2 && !player.onGround() && player.getDeltaMovement().y < 0.15) {
                startGlide(player);
            }
        }
        if (gliding && data.power() == Power.FALCON) {
            int every = FalconAbilities.cfg(FalconConfig.GLIDE_BOOST_TICKS, 40);
            double boost = FalconAbilities.cfg(FalconConfig.GLIDE_BOOST, 0.6);
            if (boost > 0.0 && now - state.lastBoost >= every && player.getXRot() < BOOST_PITCH) {
                state.lastBoost = now;
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(player.getDeltaMovement().add(look.scale(boost)));
                player.hurtMarked = true;
                FalconAbilities.sound(player.serverLevel(), player.position(), ModSounds.FALCON_THRUST.get(), 0.8F,
                        1.1F);
                FalconAbilities.send(player.serverLevel(), player, FalconFxPayload.CLIMB, List.of(player.position()),
                        1);
            }
        }
    }

    private FalconWings() {
    }
}
