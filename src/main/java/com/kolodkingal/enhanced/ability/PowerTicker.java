package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

/** Runs every hero's per-tick logic and keeps passive bonuses (flight, crown stats) in sync with the player's state. */
public final class PowerTicker {
    private static final ResourceLocation FLIGHT_ID = Enhanced.id("power_flight");

    /** Called at the end of every server player tick. */
    public static void tick(ServerPlayer player) {
        PowerData data = Powers.get(player);
        MagnetoAbilities.helmetTick(player, data);
        applyPassives(player, data);
        switch (data.power()) {
            case MAGNETO -> MagnetoAbilities.tick(player, data);
            case SCARLET_WITCH -> WandaAbilities.tick(player, data);
            case PHOTON -> PhotonAbilities.tick(player, data);
            case NONE -> {
            }
        }
        Telekinesis.tickHolder(player, data);
        data.setFlying(player.getAbilities().flying);
        Powers.syncIfDirty(player);
    }

    /** Flight permission and speed, plus the Scarlet Witch's crown bonuses. */
    public static void applyPassives(ServerPlayer player, PowerData data) {
        Power power = data.power();
        setModifier(player, NeoForgeMod.CREATIVE_FLIGHT, FLIGHT_ID, 1.0, power.canFly());

        Abilities abilities = player.getAbilities();
        boolean survivalLike = !player.isCreative() && !player.isSpectator();
        float speed = survivalLike && power.canFly() ? power.flightSpeed() : 0.05F;
        boolean changed = false;
        if (Math.abs(abilities.getFlyingSpeed() - speed) > 1.0E-4F) {
            abilities.setFlyingSpeed(speed);
            changed = true;
        }
        if (abilities.flying && !player.mayFly()) {
            abilities.flying = false;
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
        WandaAbilities.applyCrown(player, data);
    }

    /** The hero's flight key: take off or land, the same as double-tapping Space. */
    static void toggleFlight(ServerPlayer player) {
        if (!player.mayFly()) {
            return;
        }
        Abilities abilities = player.getAbilities();
        abilities.flying = !abilities.flying;
        if (abilities.flying && player.onGround()) {
            player.setDeltaMovement(new Vec3(0, 0.45, 0));
            player.hurtMarked = true;
        }
        player.onUpdateAbilities();
        player.displayClientMessage(Component.translatable(abilities.flying
                ? "message.enhanced.flight.on" : "message.enhanced.flight.off"), true);
    }

    /**
     * Adds or removes a permanent attribute modifier. Permanent (saved) modifiers are used so that max health and
     * flight are already correct when the player's health and abilities are loaded on login.
     */
    static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    private PowerTicker() {
    }
}
