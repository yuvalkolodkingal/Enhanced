package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.config.UltronConfig;
import com.kolodkingal.enhanced.network.UltronFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.lerp;
import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * What the client does to act the hero Ultron besides the poses of {@link UltronHeroPoses} and the keys of
 * {@link UltronGestures}: the gesture his own key press starts before the server has answered (with the guards only a
 * client can see), the gestures that the payloads of his effects start for everyone who sees them (the Pull, the Slam,
 * Command), the movement of his hands in first person and the flash at a palm when a gesture begins. Client only:
 * {@link UltronClient} hands its arguments on to it.
 */
final class UltronActing {
    /** A predicted gesture swallows the payload that arrives for it within this many ticks of the key press. */
    private static final float PREDICTION_WINDOW = 12.0F;
    /** The same gesture is not started again within this many ticks. */
    private static final float DEDUPE = 4.0F;

    private UltronActing() {
    }

    // ---- the key press ----

    /**
     * The gesture the local Ultron's key press starts at once (see {@link UltronGestures#predicted}), with the guards
     * only a client can see: plasma in the pool, enough of it for the Overcharge, a creature in the sight of the Pull, a
     * drone to jump into and room for more.
     */
    @Nullable
    static GestureId predicted(PowerData data, int ability, long now, boolean sneaking) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null || me.isSpectator()) {
            return null;
        }
        boolean creative = me.getAbilities().instabuild;
        boolean plasma = creative || data.ironCharge() > 0;
        boolean overcharge = creative || data.ironCharge() >= UltronConfig.OVERCHARGE_COST.get();
        boolean target = ability == 3 && !sneaking && pullTargetInSight(me);
        boolean drone = ability == 4 && !sneaking && !UltronView.isCut(me) && dronesNear(me, UltronConfig.UPLOAD_RANGE.get()) > 0;
        boolean room = ability == 5 && !sneaking && dronesNear(me, 96.0) < UltronConfig.DRONE_MAX.get();
        return UltronGestures.predicted(data, ability, now, sneaking, new UltronGestures.Guards(plasma, overcharge, target, drone, room));
    }

    /** Whether the Pull would find something: the same look, reach and filter as the server's (a wider net for small things). */
    private static boolean pullTargetInSight(LocalPlayer me) {
        double range = UltronConfig.PULL_RANGE.get();
        EntityHitResult hit = Raycasts.entity(me, range, 0.3, e -> pullable(me, e), 1.0F);
        if (hit == null) {
            hit = Raycasts.entity(me, range, 1.0, e -> (e instanceof Projectile || e instanceof ItemEntity) && pullable(me, e), 1.0F);
        }
        return hit != null;
    }

    private static boolean pullable(LocalPlayer me, net.minecraft.world.entity.Entity e) {
        if (e == me || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) {
            return false;
        }
        if (e instanceof UltronSentryEntity sentry && me.getUUID().equals(sentry.getOwnerUUID())) {
            return false;
        }
        return e instanceof LivingEntity || e instanceof Projectile || e instanceof ItemEntity;
    }

    /** How many of the local player's drones are within {@code range} of him. */
    static int dronesNear(LocalPlayer me, double range) {
        return me.level().getEntitiesOfClass(UltronSentryEntity.class, me.getBoundingBox().inflate(range),
                s -> s.isAlive() && me.getUUID().equals(s.getOwnerUUID()) && s.distanceToSqr(me) <= range * range).size();
    }

    // ---- the payloads ----

    /**
     * A payload of the hero's effects arrived: for the Pull, the Slam and Command it also starts the caster's gesture,
     * for everyone who sees it. (A key press that already started it, a moment ago, is swallowed.)
     */
    static void onFx(UltronFxPayload payload) {
        GestureId id = switch (payload.kind()) {
            case UltronFxPayload.GRAVITY_PULL -> GestureId.ULTRON_PULL;
            case UltronFxPayload.GRAVITY_SLAM -> GestureId.ULTRON_SLAM;
            case UltronFxPayload.COMMAND_MARK -> GestureId.ULTRON_COMMAND;
            default -> null;
        };
        ClientLevel level = Minecraft.getInstance().level;
        if (id == null || level == null || !(level.getEntity(payload.entityId()) instanceof AbstractClientPlayer player)) {
            return;
        }
        HeroAnimState s = HeroAnimator.peek(player);
        if (s == null) {
            return;
        }
        float age = player.tickCount;
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.id != id) {
                continue;
            }
            float since = age - slot.start;
            if (slot.predicted && since <= PREDICTION_WINDOW) {
                slot.predicted = false;
                return;
            }
            if (since < DEDUPE) {
                return;
            }
        }
        HeroAnimator.start(s, id, 0.0F, age);
        started(player, id);
    }

    // ---- first person ----

    /**
     * His hands at the edges of the screen: both palms along the look for the plasma (the hands kick in turn), the main
     * palm raised for the Overcharge with the other hand under it, and the movements of each gesture (the push of the
     * Pull, both hands brought down by the Slam, drawn in and thrown wide by the Upload, held out for the Hack).
     */
    static void firstPerson(HeroAnimState s, LocalPlayer player, FirstPersonArms.Offset m, FirstPersonArms.Offset o) {
        float age = (float) s.time;
        boolean calm = EnhancedClientConfig.reducedMotion();
        float w = s.stance(Stance.ULTRON_FIRE);
        if (w > 0.001F) {
            float kickM = Math.max(0.0F, (float) Math.cos(Math.PI * 2.0 * age / 8.0));
            float kickO = Math.max(0.0F, (float) Math.cos(Math.PI * 2.0 * (age - 4.0) / 8.0));
            kickM *= kickM;
            kickO *= kickO;
            m.add(w, -0.20F, 0.16F, 0.30F - 0.06F * kickM * kickM, -22, 6, 0);
            o.add(w, -0.20F, 0.16F, 0.30F - 0.06F * kickO * kickO, -22, 6, 0);
        }
        w = s.stance(Stance.ULTRON_CHARGE);
        if (w > 0.001F) {
            m.add(w, -0.14F, 0.22F, 0.38F, -34, 6, 0);
            o.add(w, -0.04F, 0.10F, 0.22F, -26, -14, 0);
            FirstPersonArms.tremble(m, o, w, 0.010F, 2.1F, age, calm);
        }
        for (HeroAnimState.Slot slot : s.slots) {
            if (!slot.active || slot.weight <= 0.001F) {
                continue;
            }
            float gw = slot.weight;
            float t = slot.t;
            switch (slot.id) {
                case ULTRON_PULL -> {
                    // The palm out, then drawn back to the shoulder.
                    float out = ss(0.0F, 3.0F, t) * (1.0F - ss(5.0F, 10.0F, t));
                    m.add(gw, lerp(out, 0.0F, -0.16F), lerp(out, 0.30F, 0.14F), lerp(out, 0.0F, 0.44F), lerp(out, -20, -34), 4, 0);
                }
                case ULTRON_SLAM -> {
                    // Both hands up, then down with the stamp.
                    float up = ss(0.0F, 6.0F, t) * (1.0F - ss(7.0F, 11.0F, t));
                    float down = ss(7.0F, 11.0F, t) * (1.0F - ss(13.0F, 22.0F, t));
                    FirstPersonArms.both(gw, 0.0F, 0.30F * up - 0.22F * down, 0.0F, -40 * up + 20 * down, 0, 0);
                }
                case ULTRON_UPLOAD -> {
                    // Drawn in across the chest, then thrown wide.
                    float in = ss(0.0F, 5.0F, t) * (1.0F - ss(6.0F, 10.0F, t));
                    float wide = ss(6.0F, 10.0F, t) * (1.0F - ss(12.0F, 18.0F, t));
                    FirstPersonArms.both(gw, -0.20F * in + 0.18F * wide, 0.20F * in, 0.10F * in, -30 * in, 0, 0);
                }
                case ULTRON_HACK -> {
                    m.add(gw, -0.14F, 0.18F, 0.34F * ss(0.0F, 4.0F, t), -28, 6, 0);
                    FirstPersonArms.tremble(m, null, gw * ss(4.0F, 6.0F, t), 0.010F, 2.1F, age, calm);
                }
                case ULTRON_ARMY -> {
                    float up = ss(0.0F, 7.0F, t) * (1.0F - ss(8.0F, 14.0F, t));
                    FirstPersonArms.both(gw, 0.0F, 0.32F * up, 0.0F, -42 * up, 0, 0);
                }
                case ULTRON_COMMAND -> m.add(gw, -0.10F, 0.14F, 0.40F * ss(0.0F, 3.0F, t) * (1.0F - ss(9.0F, 14.0F, t)), -26, 4, 0);
                case ULTRON_BLAST -> {
                    // Thrown up by the blast, and settling.
                    float kick = (1.0F - ss(0.0F, 2.0F, t)) + ss(0.0F, 2.0F, t) * (1.0F - ss(2.0F, 8.0F, t));
                    m.add(gw, -0.14F, 0.22F + 0.06F * kick, 0.38F - 0.12F * kick, -34, 6, 0);
                }
                default -> {
                }
            }
        }
    }

    // ---- the glints ----

    /** A gesture of his just began: a flash at the palm the gesture uses. */
    static void started(AbstractClientPlayer player, GestureId id) {
        switch (id) {
            case ULTRON_FIRE_START -> flash(player, true, 3, 0xFFA23A);
            case ULTRON_CHARGE_START -> flash(player, true, 5, 0xFFD27A);
            case ULTRON_PULL, ULTRON_HACK -> flash(player, true, 4, 0x9FD8FF);
            case ULTRON_SLAM, ULTRON_ARMY, ULTRON_UPLOAD -> {
                flash(player, true, 4, 0x9FD8FF);
                flash(player, false, 4, 0x9FD8FF);
            }
            default -> {
            }
        }
    }

    /** A few motes at a palm (small in the local player's own first-person view). */
    private static void flash(AbstractClientPlayer player, boolean main, int count, int colour) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        HumanoidArm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
        Vec3 hand = HandTracker.hand(player, arm, 0.0F);
        if (hand == null) {
            return;
        }
        boolean firstPerson = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        float size = firstPerson ? 0.4F : 0.8F;
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            level.addParticle(new EnergyParticleOptions(colour, size), hand.x + (random.nextDouble() - 0.5) * 0.2, hand.y + (random.nextDouble() - 0.5) * 0.2,
                    hand.z + (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.04, 0.01 + random.nextDouble() * 0.03,
                    (random.nextDouble() - 0.5) * 0.04);
        }
    }
}
