package com.kolodkingal.enhanced.network;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.render.UltronEffects;
import com.kolodkingal.enhanced.platform.net.PacketContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Server → client: one effect of Ultron (the boss or the hero) that everyone nearby should see: a tell decal on the
 * ground, a beam, a ring, the scrap that assembles his body.
 *
 * <p>The kinds of the boss fight are the constants below (their meaning is in each one's comment: which fields it uses
 * and what the client draws); the kinds from {@link #HERO} up are the hero's. The server sends them through {@code
 * UltronFx}; the client draws them in {@link #handle}.
 *
 * @param kind     what to draw (one of the constants)
 * @param entityId the caster, or -1
 * @param at       where it is, or where it starts
 * @param to       where it ends (a line), or zero
 * @param radius   its size
 * @param ticks    how long it lasts, or the time to impact
 */
public record UltronFxPayload(int kind, int entityId, Vec3 at, Vec3 to, float radius, int ticks) implements CustomPacketPayload {
    /** The ritual: scrap iron spirals up out of the ground at {@code at} and assembles a standing silhouette ({@code ticks} = 50). */
    public static final int ASSEMBLE = 0;
    /** A white flash at {@code at} ({@code radius} blocks across): the body appears, the plates fall open. */
    public static final int FLASH = 1;
    /** The Volley's glint: a thin amber line from the caster's palms to {@code to} for the last {@code ticks} (6) of the tell. */
    public static final int GLINT = 2;
    /** The Finger Beams' aim: ten dotted red-orange lines from the caster's fingertips converge on {@code at} (it moves; sent every 5 ticks). */
    public static final int BEAMS_AIM = 3;
    /** The Finger Beams fire: ten ribbons fan +-10 degrees round the line from the caster to {@code at} (locked) for {@code ticks} (20). */
    public static final int BEAMS_FIRE = 4;
    /** The Dive's ring: red, radius {@code radius} (4), shrinking over {@code ticks} (12), flat on the ground at {@code at}. It does not move. */
    public static final int DIVE_RING = 5;
    /** A piece of falling Sokovia is about to land: a red ring of {@code radius} (3) at {@code at} for {@code ticks} (30). */
    public static final int DEBRIS_RING = 6;
    /** The Gravity Pull's ring of pale-blue motes contracts on the ground at {@code at} from {@code radius} (16) to 3 over {@code ticks} (24). */
    public static final int PULL_RING = 7;
    /** A Sentry will emerge here: cracks and a pillar of pale-blue light at {@code at} for {@code ticks} (30). */
    public static final int SPAWN_PILLAR = 8;
    /** A shockwave ring leaves {@code at} out to {@code radius}, with a ring of dust (the Dive, the Pull's punch, a piece of Sokovia). */
    public static final int SHOCKWAVE = 9;
    /** White and red square motes run along the line from {@code at} to {@code to} for {@code ticks} (a transfer, an upload). */
    public static final int DATA_LINE = 10;
    /** The wreck of a destroyed body: a pile of scrap and smoke at {@code at} for {@code ticks} (200); nothing to pick up. */
    public static final int WRECK = 11;
    /** The ground shakes (the Tremor, and the rise): {@code ticks} of camera shake within {@code radius} of {@code at}. */
    public static final int TREMOR = 12;
    /** The lift's warning: a ring of light on the ground at the island's rim, centre {@code at}, radius {@code radius}, for {@code ticks}. */
    public static final int LIFT_RING = 13;
    /** The last body falls: {@code entityId} does the long death over {@code ticks} (60): plates fall open, a slow blue-white flash. */
    public static final int DEATH = 14;
    /** The boss's eyes flash or the Core pulses: a short glow on {@code entityId} for {@code ticks}. */
    public static final int PULSE = 15;
    /**
     * The hero's effects start here (the Pull and Slam rings, the Upload's motes, the drones' assembling, the Hack).
     */
    public static final int HERO = 32;
    /**
     * Gravity Slam: a ring of pale-blue anti-gravity motes runs out from {@code at} (his feet) to {@code radius} (7)
     * over {@code ticks} (10), with a ring of dust (the block under him) and a puff at the middle. Flat on the ground;
     * it does not move. {@code entityId} is the hero (his arms come down with the stamp).
     */
    public static final int GRAVITY_SLAM = HERO;
    /**
     * Gravity Pull: pale-blue motes run inward along the line from {@code to} (the target) to {@code at} (his palm) for
     * {@code ticks} (8), and a short ring contracts round the target; {@code entityId} is the hero (the palm rises).
     */
    public static final int GRAVITY_PULL = HERO + 1;
    /**
     * Overcharge: the heavy blast leaves the raised palm at {@code at} along {@code to} (a unit vector): a white-orange
     * flash {@code radius} (2) across and a short cone of sparks. {@code entityId} is the hero.
     */
    public static final int OVERCHARGE_FLASH = HERO + 2;
    /**
     * A burst of the network palette (white and red square motes, scanline streaks) {@code radius} blocks across at
     * {@code at}: both ends of an Upload, Second Life and a Sentry that is taken apart into data. {@code ticks} is 10.
     */
    public static final int NETWORK_FLASH = HERO + 3;
    /**
     * A drone assembles: sparks gather into a Sentry-sized silhouette at {@code at} over {@code ticks} (20), blue
     * turning red; {@code entityId} is the Sentry that is forming (or -1).
     */
    public static final int DRONE_ASSEMBLE = HERO + 4;
    /**
     * The Hack reaches out: data motes run from the hero's open hand ({@code entityId}) to the Sentry at {@code to} for
     * {@code ticks} (20) while the Sentry's eyes flicker blue, white and red; {@code at} is the hand.
     */
    public static final int HACK_BEAM = HERO + 5;
    /**
     * The hero tells his drones what to do: a pale ring of motes at {@code at} (the creature they are sent to, or the
     * hero's own feet when they gather) {@code radius} (1.5) across for {@code ticks} (10); {@code entityId} is the hero
     * (his arm points).
     */
    public static final int COMMAND_MARK = HERO + 6;

    public static final Type<UltronFxPayload> TYPE = new Type<>(Enhanced.id("ultron_fx"));

    public static final StreamCodec<ByteBuf, UltronFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UltronFxPayload::kind,
            ByteBufCodecs.VAR_INT, UltronFxPayload::entityId,
            PayloadCodecs.VEC3, UltronFxPayload::at,
            PayloadCodecs.VEC3, UltronFxPayload::to,
            ByteBufCodecs.FLOAT, UltronFxPayload::radius,
            ByteBufCodecs.VAR_INT, UltronFxPayload::ticks,
            UltronFxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** The effect is drawn here (the client class is loaded only when this runs, on a client). */
    static void handle(UltronFxPayload payload, PacketContext context) {
        UltronEffects.receive(payload);
    }
}
