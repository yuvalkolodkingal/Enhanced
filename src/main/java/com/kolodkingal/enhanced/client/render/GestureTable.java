package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

/**
 * Finds the changes in a hero's synced data that stand for an ability being used (a cooldown starting, a flag flipping, a
 * toggle switched, something grabbed with telekinesis, the weapon thrown or caught, the Hex growing) and starts the matching
 * gesture ({@link GestureRoutes} says which). Called once per client tick by {@link HeroAnimator}; the first tick after a
 * hero is seeded only remembers, so nobody replays a burst of old gestures on walking into view.
 *
 * <p>The local player's gesture may already be running, started by the key press ({@link #predicted}); the edge that arrives
 * a moment later is swallowed so it does not play twice. Two edges that stand for the same gesture (Loki's shapeshift raises
 * the casting state and changes his form) start it once.
 */
public final class GestureTable {
    private static final Toggle[] TOGGLES = Toggle.values();
    private static final Live[] LIVES = Live.values();
    private static final GestureRoutes.Flag[] FLAGS = GestureRoutes.Flag.values();
    /** A predicted gesture swallows its edge if it arrives within this many ticks of the key press. */
    private static final float PREDICTION_WINDOW = 12.0F;
    /** The same gesture is not started again within this many ticks. */
    private static final float DEDUPE = 4.0F;
    /** Ticks to wait for the weapon to appear in the hand after the data says it came home or was drawn. */
    private static final int PENDING_TICKS = 5;

    /** What was seen of a hero's data last tick. */
    public static final class Memory {
        final long[] cooldown = new long[com.kolodkingal.enhanced.power.PowerData.ABILITIES + 1];
        final boolean[] toggles = new boolean[TOGGLES.length];
        final boolean[] lives = new boolean[LIVES.length];
        final boolean[] flags = new boolean[FLAGS.length];
        String form = "";
        boolean swinging;
        int held = -1;
        Entity heldEntity;
        /** What telekinesis just let go of, to be told apart (thrown or set down) a tick later. */
        Entity released;
        int hammer = -1;
        /** When the last Chaos Shield seen raised was (or will be) complete: let go of sooner, it never formed. */
        long shieldFormedAt;
        /** The weapon "in flight" is Mjolnir's fragments (it was shattered), not the whole weapon. */
        boolean fragments;
        /** A catch or a draw seen in the data whose weapon has not yet appeared in the hand, and the ticks left to wait for it. */
        GestureRoutes.Hammer pendingHammer;
        int pendingWait;
        boolean returning;
        int weaponKind;
        boolean hasHex;
        int hexRadius;
        int hexAllowed;
        boolean hexCreatures;

        void clear() {
            java.util.Arrays.fill(cooldown, 0L);
            java.util.Arrays.fill(toggles, false);
            java.util.Arrays.fill(lives, false);
            java.util.Arrays.fill(flags, false);
            form = "";
            shieldFormedAt = 0L;
            held = -1;
            heldEntity = null;
            released = null;
            pendingHammer = null;
            hammer = -1;
            fragments = false;
            returning = false;
            weaponKind = 0;
            hasHex = false;
        }
    }

    // ====================================================================================================================
    // Edges
    // ====================================================================================================================

    /** The changes of this tick against the last one, started as gestures. */
    static void edges(HeroAnimState s, AbstractClientPlayer p, PowerData data, ClientLevel level, long now) {
        Memory m = s.table;
        Power power = s.power;
        float age = p.tickCount;
        boolean white = "white_vision".equals(data.variant());

        // 5: an ability's cooldown starts.
        boolean thrown1 = false;
        for (int i = 1; i <= PowerData.ABILITIES; i++) {
            long until = data.cooldownUntil(i);
            if (until > m.cooldown[i] && until > now) {
                if (i == 1 && power == Power.THOR) {
                    thrown1 = true;
                }
                if (i == 4 && power == Power.BILLY && s.stance(Stance.BARRIER_BILLY) > 0.05F) {
                    continue; // (the recharge of the Hex Barrier coming down is not a Freeze)
                }
                fire(s, p, GestureRoutes.cooldown(power, i, until - now), age);
            }
        }

        // 7, a tick late: what was let go of is flying (thrown) or still (set down).
        if (m.released != null) {
            Entity let = m.released;
            m.released = null;
            if (!let.isRemoved()) {
                fire(s, p, let.getDeltaMovement().lengthSqr() > 0.25 ? GestureRoutes.fling(power) : GestureId.TK_RELEASE, age);
            }
        }

        // 6 and 7: telekinesis takes hold of something, or lets go.
        int held = data.heldEntityId();
        if (m.held == -1 && held != -1) {
            fire(s, p, GestureRoutes.grab(power), age);
        } else if (m.held != -1 && held == -1) {
            Entity gone = m.heldEntity;
            if (gone != null && !gone.isRemoved()) {
                if (p.isShiftKeyDown()) {
                    fire(s, p, GestureId.TK_RELEASE, age);
                } else {
                    // Thrown or set down? Told by its speed, which arrives a tick after the data: decided next tick.
                    m.released = gone;
                }
            }
        }

        // 8 and Thor's throw, summon and draw: his weapon.
        if (power == Power.THOR) {
            hammer(s, p, data, m, thrown1, age);
        }

        // 9: a toggle is switched.
        for (Toggle toggle : TOGGLES) {
            boolean on = data.toggle(toggle);
            if (on != m.toggles[toggle.ordinal()]) {
                fire(s, p, GestureRoutes.toggle(power, toggle, on), age);
            }
        }

        // 10: a flag flips.
        boolean phased = data.visionPhased();
        for (GestureRoutes.Flag flag : FLAGS) {
            boolean on = flag(data, flag);
            if (on != m.flags[flag.ordinal()]) {
                GestureId id = GestureRoutes.flag(power, flag, on, white, phased);
                if (id == GestureId.WANDA_SHIELD_DROP && now < m.shieldFormedAt) {
                    // Let go of before it formed: there is no shield to drop, the arms only go back down.
                    HeroAnimator.release(s, GestureId.WANDA_SHIELD_RAISE, age);
                    HeroAnimator.release(s, GestureId.WANDA_SHIELD_SNAP, age);
                } else if (id == GestureId.WANDA_SHIELD_RAISE) {
                    // Acted over the shield's own wind-up (the data says how long it is), whatever the config.
                    int windUp = (int) (data.shieldFormedAt() - data.shieldStart());
                    fire(s, p, GestureRoutes.shieldRaise(windUp), windUp, age);
                } else {
                    fire(s, p, id, age);
                }
            }
        }

        // 11: a momentary state begins.
        for (Live live : LIVES) {
            if (data.is(live) && !m.lives[live.ordinal()]) {
                fire(s, p, GestureRoutes.live(power, live, s.weaponKind > 0), age);
            }
        }

        // 12: Loki takes a shape or his own again.
        if (power == Power.LOKI && !data.form().equals(m.form)) {
            fire(s, p, GestureId.LOKI_CAST, age);
        }

        // 13: the Scarlet Witch's Hex.
        if (power == Power.SCARLET_WITCH) {
            hex(s, p, level, m, now, age);
        }

        // 15: a swing with Loki's Scepter touches (the jab itself is drawn from the swing, see HeroPoses).
        if (p.swinging && !m.swinging && p.getItemInHand(p.swingingArm == null ? InteractionHand.MAIN_HAND : p.swingingArm).is(ModItems.LOKIS_SCEPTER.get())) {
            GestureEffects.scepterGlint(p);
        }
    }

    /** The weapon was thrown, called, drawn or caught. */
    private static void hammer(HeroAnimState s, AbstractClientPlayer p, PowerData data, Memory m, boolean cooldownRose, float age) {
        int hammer = data.hammerEntityId();
        boolean returning = s.thrownWeapon instanceof ThrownWeaponEntity flying && flying.isReturning();
        boolean armed = s.weaponKind > 0;
        if (m.hammer == -1 && hammer != -1) {
            m.pendingHammer = null;
            // A shattered Mjolnir is "in flight" as its fragments (the server spawns them before it says so, so the client
            // already knows the entity).
            boolean shattered = p.level().getEntity(hammer) instanceof MjolnirFragmentEntity;
            fire(s, p, GestureRoutes.hammer(shattered ? GestureRoutes.Hammer.SHATTERED
                    : m.weaponKind > 0 ? GestureRoutes.Hammer.THROWN : GestureRoutes.Hammer.SUMMONED), age);
        } else if (m.hammer != -1 && hammer != -1 && returning && !m.returning) {
            fire(s, p, GestureRoutes.hammer(GestureRoutes.Hammer.RECALLED_IN_FLIGHT), age);
        } else if (m.hammer != -1 && hammer == -1) {
            // It came home (whole, or its fragments came together). The inventory update that puts it in his hand can come
            // a tick after the data: wait a few ticks for it.
            m.pendingHammer = m.fragments ? GestureRoutes.Hammer.REFORMED : GestureRoutes.Hammer.CAUGHT;
            m.pendingWait = PENDING_TICKS;
        } else if (m.hammer == -1 && hammer == -1 && cooldownRose && !armed && m.weaponKind == 0) {
            m.pendingHammer = GestureRoutes.Hammer.DRAWN;
            m.pendingWait = PENDING_TICKS;
        } else if (m.hammer == -1 && hammer == -1 && cooldownRose && armed && m.weaponKind == 0) {
            fire(s, p, GestureRoutes.hammer(GestureRoutes.Hammer.DRAWN), age);
        }
        if (m.pendingHammer != null && hammer == -1 && armed) {
            if (m.pendingHammer == GestureRoutes.Hammer.CAUGHT) {
                // The arm that catches it points back where it came from.
                s.catchFrom = s.weaponTrail[2] != null ? s.weaponTrail[2] : s.weaponTrail[0];
            }
            fire(s, p, GestureRoutes.hammer(m.pendingHammer), age);
            m.pendingHammer = null;
        } else if (m.pendingHammer != null && --m.pendingWait <= 0) {
            m.pendingHammer = null;
        }
    }

    /** The Hex appears, vanishes, grows or shrinks, or has its allowed list or creature switch changed. */
    private static void hex(HeroAnimState s, AbstractClientPlayer p, ClientLevel level, Memory m, long now, float age) {
        Optional<Hex> hex = level.getData(ModAttachments.HEXES).byOwner(p.getUUID());
        if (hex.isPresent() && !m.hasHex) {
            if (hex.get().createdAt() >= now - 40) {
                fire(s, p, GestureRoutes.hex(GestureRoutes.HexChange.RISES), age);
            }
        } else if (hex.isEmpty() && m.hasHex) {
            fire(s, p, GestureRoutes.hex(GestureRoutes.HexChange.DISSOLVES), age);
        } else if (hex.isPresent()) {
            Hex h = hex.get();
            if (h.radius() > m.hexRadius) {
                fire(s, p, GestureRoutes.hex(GestureRoutes.HexChange.GROWS), age);
            } else if (h.radius() < m.hexRadius) {
                fire(s, p, GestureRoutes.hex(GestureRoutes.HexChange.SHRINKS), age);
            } else if (h.allowed().size() != m.hexAllowed || h.allowCreatures() != m.hexCreatures) {
                fire(s, p, GestureRoutes.hex(GestureRoutes.HexChange.TWEAKED), age);
            }
        }
    }

    /** Remembers this tick's data for the next. */
    static void remember(HeroAnimState s, AbstractClientPlayer p, PowerData data, ClientLevel level) {
        Memory m = s.table;
        for (int i = 1; i <= PowerData.ABILITIES; i++) {
            m.cooldown[i] = data.cooldownUntil(i);
        }
        for (Toggle toggle : TOGGLES) {
            m.toggles[toggle.ordinal()] = data.toggle(toggle);
        }
        for (Live live : LIVES) {
            m.lives[live.ordinal()] = data.is(live);
        }
        for (GestureRoutes.Flag flag : FLAGS) {
            m.flags[flag.ordinal()] = flag(data, flag);
        }
        if (data.chaosShieldActive()) {
            m.shieldFormedAt = data.shieldFormedAt();
        }
        m.form = data.form();
        m.swinging = p.swinging;
        m.held = data.heldEntityId();
        m.heldEntity = s.heldEntity;
        m.hammer = data.hammerEntityId();
        // (A fragment that just came home is gone before the data names the next one: it still counts.)
        Entity inFlight = m.hammer == -1 ? null : level.getEntity(m.hammer);
        m.fragments = m.hammer != -1 && (inFlight instanceof MjolnirFragmentEntity || m.fragments && inFlight == null);
        m.returning = s.thrownWeapon instanceof ThrownWeaponEntity flying && flying.isReturning();
        m.weaponKind = s.weaponKind;
        if (s.power == Power.SCARLET_WITCH) {
            Optional<Hex> hex = level.getData(ModAttachments.HEXES).byOwner(p.getUUID());
            m.hasHex = hex.isPresent();
            m.hexRadius = hex.map(Hex::radius).orElse(0);
            m.hexAllowed = hex.map(h -> h.allowed().size()).orElse(0);
            m.hexCreatures = hex.map(Hex::allowCreatures).orElse(false);
        }
    }

    private static boolean flag(PowerData data, GestureRoutes.Flag flag) {
        return switch (flag) {
            case MAGNETIC_SHIELD -> data.magneticShield();
            case SHRAPNEL -> data.shrapnelActive();
            case BEAM -> data.beamActive();
            case CHAOS_SHIELD -> data.chaosShieldActive();
            case VISION_PHASED -> data.visionPhased();
            case DENSE -> data.dense();
            case DISGUISE -> data.disguise();
            case QUICKSILVER_TIME -> data.quicksilverTime();
            case SUPER_SPEED -> data.superSpeed();
            case WALKMAN -> data.walkman();
        };
    }

    // ====================================================================================================================
    // Starting a gesture
    // ====================================================================================================================

    /** Starts a gesture because the data changed, unless the key press already started it or it was just started. */
    private static void fire(HeroAnimState s, AbstractClientPlayer p, GestureId id, float age) {
        fire(s, p, id, 0.0F, age);
    }

    /** As above, for a gesture that takes a number ({@link HeroAnimator#start}). */
    private static void fire(HeroAnimState s, AbstractClientPlayer p, GestureId id, float param, float age) {
        if (id == null) {
            return;
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || !sameGesture(slot.id, id)) {
                continue;
            }
            float since = age - slot.start;
            if (slot.predicted && since <= PREDICTION_WINDOW) {
                // The key press started it: this is the edge it was waiting for.
                slot.predicted = false;
                return;
            }
            if (since < DEDUPE) {
                return;
            }
        }
        HeroAnimator.start(s, id, param, age);
        GestureEffects.started(p, s, id, param);
    }

    /** Whether two gestures stand for the same thing (the shield's raise and snap: the wind-up decides which plays). */
    private static boolean sameGesture(GestureId a, GestureId b) {
        return a == b || (a == GestureId.WANDA_SHIELD_RAISE || a == GestureId.WANDA_SHIELD_SNAP)
                && (b == GestureId.WANDA_SHIELD_RAISE || b == GestureId.WANDA_SHIELD_SNAP);
    }

    // ====================================================================================================================
    // Prediction
    // ====================================================================================================================

    /**
     * The gesture the local player's own key press should start at once, without waiting for the server's answer, or null:
     * only where the client can tell from its own copy of the data that the ability will work.
     */
    static GestureId predicted(HeroAnimState s, PowerData data, int ability, long now, boolean hasOwnHex, boolean sneaking) {
        switch (s.power) {
            case MAGNETO -> {
                // (An empty hunger bar: the server turns the pull down with its line, and no cooldown starts.)
                LocalPlayer me = Minecraft.getInstance().player;
                boolean fed = me != null && (me.getAbilities().invulnerable || me.getFoodData().getFoodLevel() > 0);
                return GestureRoutes.magnetoPredicted(data, ability, now, sneaking, fed);
            }
            case SCARLET_WITCH -> {
                if (ability == 4 && !data.chaosShieldActive() && data.isReady(4, now)) {
                    return GestureId.WANDA_SHIELD_RAISE;
                }
                if (ability == 5 && !hasOwnHex && !sneaking) {
                    return GestureId.WANDA_HEX_RISE;
                }
            }
            case BILLY -> {
                if (ability == 1 && data.isReady(1, now)) {
                    return GestureId.BILLY_BOLT;
                }
            }
            case LOKI -> {
                if (ability == 1 && data.isReady(1, now)) {
                    return sneaking ? GestureId.LOKI_FAN : GestureId.LOKI_DAGGER;
                }
            }
            case THOR -> {
                // Sneak + R with Mjolnir shatters it, if it has gathered itself (only the server knows): not predicted.
                if (ability == 1 && data.isReady(1, now) && s.weaponKind > 0 && !(sneaking && s.weaponKind == 1)) {
                    return GestureId.THOR_THROW;
                }
            }
            default -> {
                HeroClient hero = HeroClients.of(s.power);
                if (hero != null) {
                    return hero.predictedGesture(s, data, ability, now, sneaking);
                }
            }
        }
        return null;
    }

    private GestureTable() {
    }
}
