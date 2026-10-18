package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.vishanti.VishantiBook;
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
        if (!player.isAlive()) {
            // A dead body must not keep beaming or firing while its player sits on the death screen.
            stopHoldAbilities(data);
            Powers.syncIfDirty(player);
            return;
        }
        MagnetoAbilities.helmetTick(player, data);
        QuicksilverAbilities.gogglesTick(player, data);
        ThorAbilities.everyPlayerTick(player, data);
        VishantiBook.everyPlayerTick(player);
        DarkholdBook.everyPlayerTick(player);
        HeroServers.everyPlayerTick(player, data);
        applyPassives(player, data);
        heroTick(data.power(), player, data);
        Telekinesis.tickHolder(player, data);
        data.setFlying(player.getAbilities().flying);
        Powers.syncIfDirty(player);
    }

    /** Flight permission and speed, plus the Scarlet Witch's crown bonuses. */
    public static void applyPassives(ServerPlayer player, PowerData data) {
        Power power = data.power();
        // Vision's dense body is too heavy to fly, and Thor needs his weapon to fly.
        // (3.3: a power Agatha has drained does not fly either.)
        // (3.4: so is a custom hero's with Vision's density.)
        boolean canFly = power.canFly() && !(data.has(HeroAbility.VISION_DENSITY) && data.dense())
                && !(power == Power.THOR && !ThorAbilities.canFly(player, data))
                && !(power == Power.CUSTOM && !CustomHeroes.canFly(player))
                && !HexRunes.silences(player) && !player.hasEffect(ModEffects.DRAINED); // 3.3: the Hex Runes hold a sorcerer's powers, flight too
        setModifier(player, NeoForgeMod.CREATIVE_FLIGHT, FLIGHT_ID, 1.0, canFly);

        Abilities abilities = player.getAbilities();
        boolean survivalLike = !player.isCreative() && !player.isSpectator();
        float speed = survivalLike && canFly ? (power == Power.CUSTOM ? CustomHeroes.flightSpeed(player) : power.flightSpeed()) : 0.05F;
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
        VisionAbilities.applyBody(player, data);
        if (SlowMotion.slowTimeOf(data) == Power.NONE) {
            // (Tommy's slow time, 3.3, is set by his own tick, like Quicksilver Time; 3.4: so is a custom hero's with one.)
            data.setSlowMo(false);
        }
        if (!data.acts(Power.QUICKSILVER)) {
            QuicksilverAbilities.applySpeed(player, data);
        }
        TommyAbilities.applySpeed(player, data);
        LokiAbilities.applyForm(player, data);
        ThorAbilities.applyBody(player, data);
        HeroServers.applyBody(player, data);
    }

    /**
     * The tick of hero {@code hero}'s code for this player: his own power's, or (3.4) one of a custom hero's sources
     * (each hero's tick runs only the parts of the abilities the player has).
     */
    public static void heroTick(Power hero, ServerPlayer player, PowerData data) {
        switch (hero) {
            case MAGNETO -> MagnetoAbilities.tick(player, data);
            case SCARLET_WITCH -> WandaAbilities.tick(player, data);
            case QUICKSILVER -> QuicksilverAbilities.tick(player, data);
            case VISION -> VisionAbilities.tick(player, data);
            case BILLY -> BillyAbilities.tick(player, data);
            case TOMMY -> TommyAbilities.tick(player, data);
            case LOKI -> LokiAbilities.tick(player, data);
            case THOR -> ThorAbilities.tick(player, data);
            case PHOENIX, SPIDER_MAN, THANOS, ULTRON, APOCALYPSE, AGATHA, CYCLOPS, PROFESSOR_X, CUSTOM -> HeroServers.tick(hero, player, data);
            case NONE -> {
            }
        }
    }

    /** The player is about to lose this power: everything it left in the world ends. */
    public static void endPower(ServerPlayer player, PowerData data) {
        heroEnd(data.power(), player, data);
    }

    /** Everything hero {@code hero}'s code left in the world for this player ends (his power, or 3.4 a custom hero's source). */
    public static void heroEnd(Power hero, ServerPlayer player, PowerData data) {
        switch (hero) {
            case MAGNETO -> MagnetoAbilities.endAll(player, data);
            case SCARLET_WITCH -> WandaAbilities.endAll(player, data);
            case LOKI -> LokiAbilities.endAll(player, data);
            case THOR -> ThorAbilities.endAll(player, data);
            case BILLY -> BillyAbilities.endAll(player, data);
            case TOMMY -> TommyAbilities.endAll(player, data);
            case QUICKSILVER -> QuicksilverAbilities.endAll(player, data);
            case PHOENIX, SPIDER_MAN, THANOS, ULTRON, APOCALYPSE, AGATHA, CYCLOPS, PROFESSOR_X, CUSTOM -> HeroServers.endAll(hero, player, data);
            default -> {
            }
        }
    }

    /**
     * Releases every hold-to-use ability (beam, shrapnel, shield) and stops Quicksilver Time slowing the world and
     * any run at super speed. (Which mode is chosen is kept: it comes back with the player.)
     */
    public static void stopHoldAbilities(PowerData data) {
        data.setBeamActive(false);
        data.setChaosShieldActive(false);
        data.setShrapnelActive(false);
        data.setSlowMo(false);
        data.set(Live.SPEEDING, false);
        data.set(Live.CALLING_LIGHTNING, false);
        data.set(Live.BEAMING, false);
        HeroServers.stopHold(data);
    }

    /** The hero's flight key: take off or land, the same as double-tapping Space. */
    public static void toggleFlight(ServerPlayer player) {
        if (!player.mayFly() || MjolnirPin.isPinned(player)) {
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
        setModifier(player, attribute, id, amount, present, AttributeModifier.Operation.ADD_VALUE);
    }

    static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present,
                            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount || existing.operation() != operation) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    private PowerTicker() {
    }
}
