package com.kolodkingal.enhanced.client.healing;

import com.kolodkingal.enhanced.client.EnhancedClientConfig;
import com.kolodkingal.enhanced.client.render.GestureId;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.healing.Downed;
import com.kolodkingal.enhanced.healing.HealingFactor;
import com.kolodkingal.enhanced.network.HealingFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderHandEvent;
import com.kolodkingal.enhanced.platform.event.client.ViewportEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The healing factor on the client (3.7): the drawers of {@link HealingFxPayload} (each kind's light: soft green-white
 * motes, never anything of a wound), the DOWN pose held while a healer is DOWN and the RISE after it (for everyone who
 * sees him, whoever his hero: {@link HealingGestures}), the glints that rise from a healer whose wounds close, and, for
 * the local player while he is DOWN, the world tipped a little on its side and his hands out of sight (the HUD's part is
 * {@link HealingHud}).
 */
@Subscriber(side = Side.CLIENT)
public final class HealingClient {
    /** The healing factor's light, and its brighter core. */
    static final int GLINT = Downed.GLINT;
    static final int WHITE = 0xF4FFF6;
    /** Poison's sour green, and the healing's bright return. */
    static final int SOUR = 0x7FA33A;
    static final int BRIGHT = 0x9CFFB0;
    /** Ticks after the end of DOWN when the RISE plays (he is on his feet by then: the DOWN gesture's keys). */
    static final int RISE_DELAY = 14;
    /** How far (degrees) the local player's view tips while he lies DOWN. */
    static final float VIEW_ROLL = 16.0F;

    /** The healers whose healing the server has told off (poisoned): no glints from them. */
    private static final Set<Integer> OFF = new HashSet<>();
    /** The players seen DOWN last tick, and when each one's RISE is due. */
    private static final Set<UUID> WAS_DOWN = new HashSet<>();
    private static final Map<UUID, Long> RISE_AT = new HashMap<>();
    private static float viewRoll;
    private static float viewRollO;

    private HealingClient() {
    }

    /** Registers the drawer of every kind (the client setup's healing region). */
    public static void setup() {
        HealingFx.register(HealingFxPayload.DOWN, HealingClient::down);
        HealingFx.register(HealingFxPayload.RISE, HealingClient::rise);
        HealingFx.register(HealingFxPayload.BURST, HealingClient::burst);
        HealingFx.register(HealingFxPayload.RESCUE, HealingClient::rescue);
        HealingFx.register(HealingFxPayload.OFF, p -> poison(p, true));
        HealingFx.register(HealingFxPayload.ON, p -> poison(p, false));
        HealingFx.register(HealingFxPayload.CUT, HealingClient::cut);
    }

    // ---- the drawers ----

    private static Entity entity(HealingFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : level.getEntity(payload.entityId());
    }

    /** He goes DOWN: the pose begins (the tick holds it), a ring of light runs out on the ground. */
    static void down(HealingFxPayload payload) {
        if (!(entity(payload) instanceof Player player)) {
            return;
        }
        HeroAnimator.play(player, GestureId.HEALING_DOWN, 0.0F);
        ring(player, 0.15, 0.55, 14, GLINT, 0.09);
        motes(player, 10, WHITE, 0.9F, 0.03);
    }

    /** He rises: light from the ground up through him (the RISE gesture follows when he is up). */
    static void rise(HealingFxPayload payload) {
        if (!(entity(payload) instanceof Player player)) {
            return;
        }
        ring(player, 0.1, 0.6, 16, GLINT, 0.05);
        column(player, 22, 0.08);
    }

    /** The Healing Burst: the gesture, a spiral of light up round him, a flash of white. */
    static void burst(HealingFxPayload payload) {
        if (!(entity(payload) instanceof Player player)) {
            return;
        }
        HeroAnimator.play(player, GestureId.HEALING_BURST, 0.0F);
        ClientLevel level = (ClientLevel) player.level();
        for (int i = 0; i < 36; i++) {
            float a = i * 0.55F;
            double r = 0.75 - i * 0.012;
            double y = player.getY() + 0.1 + i * 0.055;
            level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? WHITE : GLINT, 1.1F),
                    player.getX() + r * Mth.cos(a), y, player.getZ() + r * Mth.sin(a),
                    -0.04 * Mth.sin(a), 0.035, 0.04 * Mth.cos(a));
        }
        ring(player, 1.0, 0.4, 18, WHITE, 0.12);
    }

    /** He was dragged out of a hazard: a column of light where he stands now. */
    static void rescue(HealingFxPayload payload) {
        if (entity(payload) instanceof Player player) {
            column(player, 26, 0.1);
        }
    }

    /** Poison switched his healing off (a sour puff falling from him) or it came back (a bright sparkle). */
    static void poison(HealingFxPayload payload, boolean off) {
        if (off) {
            OFF.add(payload.entityId());
        } else {
            OFF.remove(payload.entityId());
        }
        if (entity(payload) instanceof Player player) {
            motes(player, 14, off ? SOUR : BRIGHT, 1.0F, off ? -0.03 : 0.05);
        }
    }

    /** A blow was cut short of killing him: a quick ring of light round his chest, for the one who struck. */
    static void cut(HealingFxPayload payload) {
        if (entity(payload) instanceof Player player) {
            ring(player, player.getBbHeight() * 0.55, 0.65, 18, GLINT, 0.08);
        }
    }

    // ---- the tick ----

    @Subscribe
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        viewRollO = viewRoll;
        if (level == null) {
            clear();
            return;
        }
        long now = level.getGameTime();
        for (AbstractClientPlayer player : level.players()) {
            UUID id = player.getUUID();
            boolean down = Powers.get(player).is(Live.HEALING_DOWN) && player.isAlive();
            if (down) {
                WAS_DOWN.add(id);
                RISE_AT.remove(id);
                if (!playing(player, GestureId.HEALING_DOWN)) {
                    HeroAnimator.play(player, GestureId.HEALING_DOWN, 0.0F);
                }
                HeroAnimator.hold(player, GestureId.HEALING_DOWN, 3);
                if ((now + player.getId()) % 3 == 0 && !player.isInvisible()) {
                    motes(player, 2, (now / 3) % 2 == 0 ? GLINT : WHITE, 0.8F, 0.045);
                }
            } else if (WAS_DOWN.remove(id)) {
                RISE_AT.put(id, now + RISE_DELAY);
            }
            Long at = RISE_AT.get(id);
            if (at != null && now >= at) {
                RISE_AT.remove(id);
                HeroAnimator.play(player, GestureId.HEALING_RISE, 0.0F);
            }
            if (!down && glints(player) && (now + player.getId()) % 8 == 0) {
                motes(player, 1, GLINT, 0.7F, 0.04);
            }
        }
        WAS_DOWN.removeIf(id -> level.getPlayerByUUID(id) == null);
        RISE_AT.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        LocalPlayer self = mc.player;
        boolean tipped = self != null && HealingFactor.isDown(self) && mc.options.getCameraType().isFirstPerson()
                && !EnhancedClientConfig.reducedMotion();
        viewRoll += ((tipped ? VIEW_ROLL : 0.0F) - viewRoll) * 0.18F;
        if (Math.abs(viewRoll) < 0.01F) {
            viewRoll = 0.0F;
        }
    }

    /** Whether a healer's wounds are closing now (some glints rise from him): hurt, up, his healing not off. */
    static boolean glints(Player player) {
        return HealingFactor.has(player) && player.isAlive() && player.getHealth() < player.getMaxHealth()
                && !OFF.contains(player.getId()) && !player.isInvisible()
                && !(player == Minecraft.getInstance().player && HealingFactor.suppressed(player));
    }

    /** Whether the gesture plays on him now (and is not fading out). */
    static boolean playing(Player player, GestureId id) {
        HeroAnimState state = HeroAnimator.peek(player);
        if (state == null) {
            return true; // (nobody draws him: nothing to start)
        }
        for (HeroAnimState.Slot slot : state.slots) {
            if (slot.active && slot.id == id && Float.isNaN(slot.releaseStart)) {
                return true;
            }
        }
        return false;
    }

    // ---- the view of the one who is DOWN ----

    @Subscribe
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float roll = Mth.lerp((float) event.getPartialTick(), viewRollO, viewRoll);
        if (roll != 0.0F) {
            event.setRoll(event.getRoll() + roll);
            event.setPitch(event.getPitch() + roll * 0.4F);
        }
    }

    @Subscribe
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self != null && HealingFactor.isDown(self)) {
            event.setCanceled(true);
        }
    }

    public static void clear() {
        OFF.clear();
        WAS_DOWN.clear();
        RISE_AT.clear();
        viewRoll = 0.0F;
        viewRollO = 0.0F;
    }

    // ---- the light ----

    /** {@code count} motes about his body, drifting up at {@code rise} a tick (down when negative). */
    static void motes(Player player, int count, int color, float scale, double rise) {
        ClientLevel level = (ClientLevel) player.level();
        RandomSource random = player.getRandom();
        double w = player.getBbWidth() * 0.7;
        double h = HealingFactor.isDown(player) ? 0.6 : player.getBbHeight();
        for (int i = 0; i < count; i++) {
            level.addParticle(new EnergyParticleOptions(color, scale),
                    player.getX() + (random.nextDouble() - 0.5) * 2.0 * w,
                    player.getY() + 0.1 + random.nextDouble() * h,
                    player.getZ() + (random.nextDouble() - 0.5) * 2.0 * w,
                    (random.nextDouble() - 0.5) * 0.01, rise * (0.7 + 0.6 * random.nextDouble()),
                    (random.nextDouble() - 0.5) * 0.01);
        }
    }

    /** A ring of {@code count} motes round him at height {@code y} and radius {@code r}, running out at {@code speed}. */
    static void ring(Player player, double y, double r, int count, int color, double speed) {
        ClientLevel level = (ClientLevel) player.level();
        for (int i = 0; i < count; i++) {
            float a = Mth.TWO_PI * i / count;
            double cx = Mth.cos(a);
            double cz = Mth.sin(a);
            level.addParticle(new EnergyParticleOptions(color, 1.0F), player.getX() + r * cx, player.getY() + y,
                    player.getZ() + r * cz, speed * cx, 0.0, speed * cz);
        }
    }

    /** A column of light rising through him from the ground. */
    static void column(Player player, int count, double rise) {
        ClientLevel level = (ClientLevel) player.level();
        RandomSource random = player.getRandom();
        for (int i = 0; i < count; i++) {
            float a = random.nextFloat() * Mth.TWO_PI;
            double r = 0.2 + random.nextDouble() * 0.4;
            level.addParticle(new EnergyParticleOptions(i % 4 == 0 ? WHITE : GLINT, 1.0F),
                    player.getX() + r * Mth.cos(a), player.getY() + random.nextDouble() * 0.4,
                    player.getZ() + r * Mth.sin(a), 0.0, rise * (0.6 + random.nextDouble()), 0.0);
        }
    }
}
