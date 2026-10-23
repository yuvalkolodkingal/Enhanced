package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.ability.WaterRun;
import com.kolodkingal.enhanced.bifrost.BifrostKeys;
import com.kolodkingal.enhanced.client.render.Afterimages;
import com.kolodkingal.enhanced.client.render.BarrierRenderer;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.BillyCrownLayer;
import com.kolodkingal.enhanced.client.render.CustomActing;
import com.kolodkingal.enhanced.client.render.FlightEffects;
import com.kolodkingal.enhanced.client.render.GestureEffects;
import com.kolodkingal.enhanced.client.render.HandTracker;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.MagnetStreams;
import com.kolodkingal.enhanced.client.render.ThorEffects;
import com.kolodkingal.enhanced.client.render.WorldEffectsRenderer;
import com.kolodkingal.enhanced.client.screen.BifrostScreen;
import com.kolodkingal.enhanced.client.screen.HexScreen;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.network.AbilityKeyPayload;
import com.kolodkingal.enhanced.network.ChangeLookPayload;
import com.kolodkingal.enhanced.network.HexActionPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.client.Keys;
import com.kolodkingal.enhanced.platform.event.LevelEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientPlayerNetworkEvent;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.platform.event.client.ComputeFovModifierEvent;
import com.kolodkingal.enhanced.platform.event.client.InputEvent;
import com.kolodkingal.enhanced.platform.event.client.RenderPlayerEvent;
import com.kolodkingal.enhanced.platform.event.client.SelectMusicEvent;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.HexRunes;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.Music;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Optional;

/** Client game events: ability keys, hand poses and flight particles. */
@Subscriber(side = Side.CLIENT)
public final class ClientEvents {
    /** Whether we told the server that ability key i is down. */
    private static final boolean[] SENT_DOWN = new boolean[PowerData.ABILITIES + 1];
    /**
     * 3.5: the Sneak key's state at the moment ability key i went down (0 not pressed since the last tick, 1 without
     * Sneak, 2 with it). The keys are handled once a tick, and by then a quick Sneak may already be let go of (or the
     * server not yet told of it), so a press carries the Sneak it was made with.
     */
    private static final byte[] PRESS_SNEAK = new byte[PowerData.ABILITIES + 1];

    /** 3.5: an ability key went down: remember whether Sneak was held at that moment. */
    @Subscribe
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            pressed(com.mojang.blaze3d.platform.InputConstants.getKey(event.getKey(), event.getScanCode()));
        }
    }

    /** 3.5: the same for an ability bound to a mouse button. */
    @Subscribe
    public static void onMouseInput(InputEvent.MouseButton.Post event) {
        if (event.getAction() == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            pressed(com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        }
    }

    private static void pressed(com.mojang.blaze3d.platform.InputConstants.Key input) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 1; i <= PowerData.ABILITIES; i++) {
            if (PRESS_SNEAK[i] == 0 && Keys.matches(KeyBindings.ability(i), input)) {
                PRESS_SNEAK[i] = (byte) (mc.options.keyShift.isDown() ? 2 : 1);
            }
        }
    }

    @Subscribe
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            java.util.Arrays.fill(SENT_DOWN, false);
            java.util.Arrays.fill(PRESS_SNEAK, (byte) 0);
            return;
        }
        HeroAnimator.tick(mc.level);
        FlightEffects.tick(mc.level);
        GestureEffects.tick(mc.level);
        MagnetStreams.tick(mc.level);
        handleKeys(mc, player);
        QuickHands.tick(mc, player);
        spawnSpeedTrails(mc.level);
        spawnFrostParticles(mc);
        Afterimages.tick(mc.level);
        Shapeshifts.tick(mc.level);
        SpeedsterMusic.tick(mc);
        PowerLoopSounds.tick(mc);
        SlowMotionEffect.tick(mc);
        TheNoise.tick(mc);
        HeroClients.tick(mc);
    }

    /** The game's own music waits while a speedster's Walkman song plays, and for its usual pause after the song ends. */
    @Subscribe
    public static void onSelectMusic(SelectMusicEvent event) {
        Music chosen = event.getMusic();
        if (chosen != null && SpeedsterMusic.holdsGameMusic(chosen)) {
            event.setMusic(null);
        }
    }

    @Subscribe
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        java.util.Arrays.fill(SENT_DOWN, false);
        SpeedsterMusic.stop();
        PowerLoopSounds.stopAll();
        Afterimages.clear();
        HeroAnimator.clear();
        HandTracker.clear();
        FlightEffects.clear();
        GestureEffects.clear();
        ThorEffects.clear();
        Shapeshifts.clear();
        SlowMotionEffect.stop(Minecraft.getInstance());
        TheNoise.stop();
        BillyCrownLayer.clear();
        WorldEffectsRenderer.clearHexBolts();
        WorldEffectsRenderer.clearHexes();
        WorldEffectsRenderer.clearShields();
        BeamRenderer.clear();
        BarrierRenderer.clear();
        MagnetStreams.clear();
        HeroClients.clear();
    }

    /**
     * A new world (a dimension change, say) starts the heroes' lean from scratch, and nothing of the old one (a Magnetic
     * Pull's streams) is drawn in it.
     */
    @Subscribe
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            HeroAnimator.clear();
            HandTracker.clear();
            FlightEffects.clear();
            GestureEffects.clear();
            MagnetStreams.clear();
            HeroClients.clear();
        }
    }

    /** Frost drifts off a Frost Giant: snowflakes and a faint icy shimmer (not in his own face in first person). */
    private static void spawnFrostParticles(Minecraft mc) {
        ClientLevel level = mc.level;
        RandomSource random = level.random;
        for (AbstractClientPlayer player : level.players()) {
            if (!LokiAbilities.isFrostGiant(player) || player.isInvisible() || player.tickCount % 3 != 0
                    || (player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson())) {
                continue;
            }
            double x = player.getX() + (random.nextDouble() - 0.5) * 0.8;
            double y = player.getY() + 0.2 + random.nextDouble() * 1.5;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 0.8;
            level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, (random.nextDouble() - 0.5) * 0.04, -0.02, (random.nextDouble() - 0.5) * 0.04);
            if (random.nextInt(3) == 0) {
                level.addParticle(new EnergyParticleOptions(LokiAbilities.FROST_GLOW, 0.7F), x, y, z, 0, -0.01, 0);
            }
        }
    }

    /**
     * Quicksilver leaves a grey blur behind him; Tommy a teal-green mist, low along the ground, and a ring of
     * it while he spins a whirlwind.
     */
    private static void spawnSpeedTrails(ClientLevel level) {
        RandomSource random = level.random;
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (player.isInvisible()) {
                continue;
            }
            if (data.has(HeroAbility.TOMMY_WHIRLWIND) && data.is(Live.WHIRLWIND)) {
                whirlwindParticles(level, player, random);
            }
            boolean tommy = data.has(HeroAbility.TOMMY_SPEED) && data.is(Live.SPEEDING);
            if (!tommy && !(data.has(HeroAbility.QUICKSILVER_SPEED) && data.is(Live.SPEEDING))) {
                continue;
            }
            // Only while he actually moves, and laid down along the path he just ran (never in his own face).
            double dx = player.getX() - player.xOld;
            double dy = player.getY() - player.yOld;
            double dz = player.getZ() - player.zOld;
            if (SpeedSurfaceFx.trail(level, player, data, random, dx, dy, dz)) {
                continue; // (3.3: on a wall or a ceiling the blur follows the body turned onto it)
            }
            if (dx * dx + dz * dz < 0.04) {
                continue;
            }
            if (tommy) {
                for (int i = 0; i < 4; i++) {
                    double back = 0.8 + random.nextDouble() * 1.6;
                    int color = AbilityColors.shade(player, HeroAbility.TOMMY_SPEED, i == 0 ? 0xE6FFF5 : i == 1
                            ? CustomActing.tint(player, Power.TOMMY.glow, true)
                            : CustomActing.tint(player, Power.TOMMY.color, false));
                    level.addParticle(new EnergyParticleOptions(color, 1.0F + random.nextFloat() * 0.4F),
                            player.getX() - dx * back + (random.nextDouble() - 0.5) * 0.6, player.getY() - dy * back + random.nextDouble() * random.nextDouble() * 1.6,
                            player.getZ() - dz * back + (random.nextDouble() - 0.5) * 0.6, 0, 0.005, 0);
                }
                continue;
            }
            for (int i = 0; i < 3; i++) {
                double back = 1.0 + random.nextDouble();
                int color = AbilityColors.color(player, HeroAbility.QUICKSILVER_SPEED, i == 0
                        ? CustomActing.tint(player, Power.QUICKSILVER.glow, true)
                        : CustomActing.tint(player, Power.QUICKSILVER.color, false));
                level.addParticle(new EnergyParticleOptions(color, 1.1F),
                        player.getX() - dx * back + (random.nextDouble() - 0.5) * 0.5, player.getY() - dy * back + random.nextDouble() * 1.8,
                        player.getZ() - dz * back + (random.nextDouble() - 0.5) * 0.5, 0, 0, 0);
            }
        }
    }

    /** A spinning wall of teal-green mist around Tommy's whirlwind. */
    private static void whirlwindParticles(ClientLevel level, AbstractClientPlayer player, RandomSource random) {
        float t = player.tickCount;
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double height = random.nextDouble() * random.nextDouble() * 4.0;
            double radius = 2.0 + height * 0.35 + random.nextDouble() * 0.6;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            // Moving along the ring, so the mist swirls.
            double vx = -Math.sin(angle) * 0.35;
            double vz = Math.cos(angle) * 0.35;
            int color = AbilityColors.shade(player, HeroAbility.TOMMY_WHIRLWIND, random.nextInt(3) == 0 ? 0xE6FFF5 : random.nextBoolean()
                    ? CustomActing.tint(player, Power.TOMMY.glow, true) : CustomActing.tint(player, Power.TOMMY.color,
                            false));
            level.addParticle(new EnergyParticleOptions(color, 1.1F), x, player.getY() + height, z, vx, 0.02, vz);
        }
    }

    /**
     * The speedsters' footwork on water, done on the client because players move themselves ({@link WaterRun}: 3.3
     * Quicksilver's Wall &amp; Water Run; 3.6.1 Tommy's Wall Run too). Walls and ceilings are {@link SpeedsterMotion}'s.
     */
    @Subscribe
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof LocalPlayer player && !SpeedsterMotion.runner().active()) {
            WaterRun.hold(player);
        }
    }

    /** Running at super speed would otherwise widen the field of view absurdly. */
    @Subscribe
    public static void onComputeFov(ComputeFovModifierEvent event) {
        if (QuicksilverAbilities.isRunning(event.getPlayer()) || TommyAbilities.isRunning(event.getPlayer())) {
            event.setNewFovModifier(Math.min(event.getNewFovModifier(), 1.2F));
        }
    }

    private static void handleKeys(Minecraft mc, LocalPlayer player) {
        Power power = Powers.power(player);
        boolean lookClicked = false;
        while (KeyBindings.CHANGE_LOOK.consumeClick()) {
            lookClicked = true;
        }
        if (lookClicked && power.hasPowers() && mc.screen == null && !player.isSpectator()) {
            Net.sendToServer(new ChangeLookPayload(player.isShiftKeyDown() ? -1 : 1));
        }
        boolean visionClicked = false;
        while (KeyBindings.VISION_COMMAND.consumeClick()) {
            visionClicked = true;
        }
        if (visionClicked && HexScreen.ordersVision(player) && mc.screen == null && !player.isSpectator()) {
            sendVisionOrder(player);
        }
        for (int i = 1; i <= PowerData.ABILITIES; i++) {
            KeyMapping key = KeyBindings.ability(i);
            boolean clicked = false;
            while (key.consumeClick()) {
                clicked = true;
            }
            // (3.3: the Hex Runes hold a sorcerer's powers: no gesture is predicted for a press the server refuses)
            boolean usable = power.hasPowers() && mc.screen == null && !player.isSpectator() && !HexRunes.silences(player);
            boolean down = usable && key.isDown();
            // (3.5: the Sneak the key was pressed with, not the one the tick read; see PRESS_SNEAK)
            boolean sneak = PRESS_SNEAK[i] != 0 ? PRESS_SNEAK[i] == 2 : mc.options.keyShift.isDown();
            PRESS_SNEAK[i] = 0;
            if (usable && clicked && !SENT_DOWN[i]) {
                if (HexScreen.opens(player, i, sneak)) { // (3.4: a custom hero's Hex key too)
                    mc.setScreen(new HexScreen());
                    continue;
                }
                if (BifrostKeys.opensScreen(player, i, sneak)) { mc.setScreen(new BifrostScreen()); continue; }
                Net.sendToServer(new AbilityKeyPayload(i, true, sneak));
                SENT_DOWN[i] = true;
                HeroAnimator.predict(player, i);
            }
            if (!down && SENT_DOWN[i]) {
                Net.sendToServer(new AbilityKeyPayload(i, false, mc.options.keyShift.isDown()));
                SENT_DOWN[i] = false;
            }
        }
    }

    /**
     * The Vision key, for a Scarlet Witch with a Hex: "Vision, that one!" at the creature she looks at (nothing in view
     * calls him back to her side); with Shift, he holds his post, or follows her again.
     */
    private static void sendVisionOrder(LocalPlayer player) {
        Optional<Hex> hex = ModAttachments.HEXES.get(player.level()).byOwner(player.getUUID());
        if (hex.isEmpty()) {
            return;
        }
        if (player.isShiftKeyDown()) {
            Net.sendToServer(new HexActionPayload(HexAction.SET_VISION_STAYS, Optional.empty(), hex.get().visionStays() ? 0 : 1));
        } else {
            Net.sendToServer(new HexActionPayload(HexAction.VISION_TARGET, Optional.empty(), 0));
        }
    }

    @Subscribe
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Afterimages.render(event);
    }

    private ClientEvents() {
    }
}
