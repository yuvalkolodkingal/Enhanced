package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.PowerHud;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The client side of one hero, as a plug-in: {@link HeroClients} finds it by {@link Power} and the shared client code
 * (the HUD, the eyes, the animator, the poses, the first-person hands, the flight effects, {@code ClientEvents}) calls
 * it, so a hero built after Thor never edits those switch statements. (The heroes before Thor keep their arms.) It
 * lives in this package because what the animation hooks work on ({@link PoseContext}, {@link HeroAnimator#start},
 * {@link FirstPersonArms.Offset}, the helpers of {@link FlightEffects}) is not visible outside it.
 *
 * <p>Every method does nothing by default; a hero overrides the ones it needs. All of them are called on the render
 * thread, for the player the animation runs for (everyone who has the power and is in view), except where it says it is
 * only for the local player.
 */
public interface HeroClient {
    /** The hero this plug-in is the client side of. */
    Power power();

    // ---- the HUD ----

    /**
     * The status shown after the ability's line on the HUD of the local player. Called before the generic cooldown, so
     * a hero whose cooldown means something else (the seconds a mode has left) can say so; the default is the generic
     * grey seconds of a cooldown that runs, and nothing.
     */
    @Nullable
    default Component hudStatus(LocalPlayer player, PowerData data, int ability, long now) {
        return PowerHud.cooldown(data, ability, now);
    }

    // ---- the body ----

    /**
     * How this hero's energy shield is drawn, or null if it raises none. The shield is the one in {@code PowerData}
     * ({@code chaosShieldActive}, {@code shieldFormedAt}, {@code shieldStrength}); the world renderer draws the dome of
     * every player whose power says a style, and the hero's HUD line is {@link PowerHud#shield}.
     */
    @Nullable
    default ShieldStyle shieldStyle() {
        return null;
    }

    /**
     * The mask of the glowing eyes for this player right now, or null: a texture the size of the skin (see
     * GlowingEyesLayer).
     */
    @Nullable
    default RenderType eyes(AbstractClientPlayer player, PowerData data) {
        return null;
    }

    // ---- the animation ----

    /** Switches the hero's stances on for this tick ({@code on[Stance.X.ordinal()] = ...}); every stance starts off. */
    default void stanceTargets(HeroAnimState s, AbstractClientPlayer p, PowerData data, boolean[] on, long now) {
    }

    /** The changes of this tick against the last (only after the first tick of a seeded hero): start gestures here. */
    default void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, long now) {
    }

    /** The gesture a take-off starts, or null for the default one. (Only for a hero with a flight profile.) */
    @Nullable
    default GestureId takeoffGesture() {
        return null;
    }

    /** The degrees of lean the hero's stances add to the body (summed with the others). */
    default float lean(HeroAnimState st) {
        return 0.0F;
    }

    /** The dip (blocks, positive sinks) the hero's stances add to the body. */
    default float drop(HeroAnimState st) {
        return 0.0F;
    }

    /** The ambient stance rows: applied first, under the aimed and holding ones. */
    default void ambient(PoseContext c) {
    }

    /** The aimed and holding stance rows: applied after the ambient ones, so they win where they overlap. */
    default void aimed(PoseContext c) {
    }

    /**
     * The gesture the local player's own key press should start at once, or null (only where the client can tell from
     * its own copy of the data that the ability will work). See {@code GestureTable.predicted}.
     */
    @Nullable
    default GestureId predictedGesture(HeroAnimState s, PowerData data, int ability, long now, boolean sneaking) {
        return null;
    }

    /** Defines the keys of the hero's gestures ({@link Gestures#define}), once, when the gesture tables are built. */
    default void registerGestures() {
    }

    // ---- first person and flight ----

    /**
     * The movement the hero's stances and gestures add to the local player's two hands (main and off) in first person.
     */
    default void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset main, FirstPersonArms.Offset off) {
    }

    /**
     * The particles of the hero in the air, laid once per tick ({@code density} thins them with the client's options).
     */
    default void flightParticles(ClientLevel level, AbstractClientPlayer player, HeroAnimState st, FlightEffects.Fx fx, float density,
                                 boolean local) {
    }

    /** The ribbons of the hero in the air, drawn every frame through the recent history of the points of their body. */
    default void flightRibbons(Matrix4f pose, VertexConsumer vc, Vec3 camera, AbstractClientPlayer player, HeroAnimState st,
                               FlightEffects.Fx fx, float time) {
    }

    // ---- the client's life ----

    /** Once per client tick, with a world. */
    default void tick(Minecraft mc) {
    }

    /** The client left the world or changed dimension: forget every effect, sound and cache of the hero. */
    default void clear() {
    }
}
