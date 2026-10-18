package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.client.render.Afterimages;
import com.kolodkingal.enhanced.client.render.BarrierRenderer;
import com.kolodkingal.enhanced.client.render.BeamRenderer;
import com.kolodkingal.enhanced.client.render.FlightEffects;
import com.kolodkingal.enhanced.client.render.GestureEffects;
import com.kolodkingal.enhanced.client.render.HandTracker;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.client.render.HeroClients;
import com.kolodkingal.enhanced.client.render.MagnetStreams;
import com.kolodkingal.enhanced.client.render.ThorEffects;
import com.kolodkingal.enhanced.client.render.BillyCrownLayer;
import com.kolodkingal.enhanced.client.render.CustomActing;
import com.kolodkingal.enhanced.client.render.WorldEffectsRenderer;
import com.kolodkingal.enhanced.client.screen.BifrostScreen;
import com.kolodkingal.enhanced.client.screen.HexScreen;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.custom.HeroKit;
import com.kolodkingal.enhanced.hex.Hex;
import com.kolodkingal.enhanced.hex.HexAction;
import com.kolodkingal.enhanced.network.AbilityKeyPayload;
import com.kolodkingal.enhanced.network.ChangeLookPayload;
import com.kolodkingal.enhanced.network.HexActionPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.Music;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/** Client game events: ability keys, hand poses and flight particles. */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    /** Whether we told the server that ability key i is down. */
    private static final boolean[] SENT_DOWN = new boolean[PowerData.ABILITIES + 1];

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            java.util.Arrays.fill(SENT_DOWN, false);
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
        QuicksilverMusic.tick(mc);
        PowerLoopSounds.tick(mc);
        SlowMotionEffect.tick(mc);
        TheNoise.tick(mc);
        HeroClients.tick(mc);
    }

    /** The game's own music waits while Quicksilver's song plays, and for its usual pause after the song ends. */
    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        Music chosen = event.getMusic();
        if (chosen != null && QuicksilverMusic.holdsGameMusic(chosen)) {
            event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        java.util.Arrays.fill(SENT_DOWN, false);
        QuicksilverMusic.stop();
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
    @SubscribeEvent
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
            if (!tommy && !QuicksilverAbilities.isRunning(player)) {
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
                    int color = i == 0 ? 0xE6FFF5 : i == 1 ? CustomActing.tint(player, Power.TOMMY.glow, true)
                            : CustomActing.tint(player, Power.TOMMY.color, false);
                    level.addParticle(new EnergyParticleOptions(color, 1.0F + random.nextFloat() * 0.4F),
                            player.getX() - dx * back + (random.nextDouble() - 0.5) * 0.6, player.getY() - dy * back + random.nextDouble() * random.nextDouble() * 1.6,
                            player.getZ() - dz * back + (random.nextDouble() - 0.5) * 0.6, 0, 0.005, 0);
                }
                continue;
            }
            for (int i = 0; i < 3; i++) {
                double back = 1.0 + random.nextDouble();
                int color = i == 0 ? CustomActing.tint(player, Power.QUICKSILVER.glow, true)
                        : CustomActing.tint(player, Power.QUICKSILVER.color, false);
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
            int color = random.nextInt(3) == 0 ? 0xE6FFF5 : random.nextBoolean() ? CustomActing.tint(player, Power.TOMMY.glow, true)
                    : CustomActing.tint(player, Power.TOMMY.color, false);
            level.addParticle(new EnergyParticleOptions(color, 1.1F), x, player.getY() + height, z, vx, 0.02, vz);
        }
    }

    /**
     * Quicksilver's footwork on water, done on the client because players move themselves: while he runs at super speed
     * with Wall &amp; Water Run on, he runs across water as long as he keeps running. (Walls and ceilings are
     * {@link SpeedsterMotion}'s, since 3.3.)
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        PowerData data = Powers.get(player);
        if (!QuicksilverAbilities.isRunning(player) || !data.wallRun() || player.getAbilities().flying || SpeedsterMotion.runner().active()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.05, player.getZ());
        if (!player.isInWater() && motion.y <= 0 && player.level().getFluidState(below).is(FluidTags.WATER)
                && player.level().getBlockState(below.above()).isAir()) {
            player.setDeltaMovement(motion.x, 0.0, motion.z);
            player.setOnGround(true);
            player.resetFallDistance();
        }
    }

    /** Running at super speed would otherwise widen the field of view absurdly. */
    @SubscribeEvent
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
            PacketDistributor.sendToServer(new ChangeLookPayload(player.isShiftKeyDown() ? -1 : 1));
        }
        boolean visionClicked = false;
        while (KeyBindings.VISION_COMMAND.consumeClick()) {
            visionClicked = true;
        }
        PowerData own = Powers.get(player);
        if (visionClicked && own.has(HeroAbility.WANDA_HEX) && mc.screen == null && !player.isSpectator()) {
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
            if (usable && clicked && !SENT_DOWN[i]) {
                // (3.4: on the key that holds the ability: N for the hero, any key a custom hero put it on.)
                if (opens(own, HeroAbility.WANDA_HEX, i) && !player.isShiftKeyDown() && hasOwnHex(player)) {
                    mc.setScreen(new HexScreen());
                    continue;
                }
                if (opens(own, HeroAbility.THOR_BIFROST, i) && !player.isShiftKeyDown()
                        && (power == Power.CUSTOM || ThorAbilities.holdsWeapon(player))) {
                    mc.setScreen(new BifrostScreen());
                    continue;
                }
                PacketDistributor.sendToServer(new AbilityKeyPayload(i, true));
                SENT_DOWN[i] = true;
                HeroAnimator.predict(player, i);
            }
            if (!down && SENT_DOWN[i]) {
                PacketDistributor.sendToServer(new AbilityKeyPayload(i, false));
                SENT_DOWN[i] = false;
            }
        }
    }

    /**
     * Whether key {@code key} without Sneak is this ability's main action (3.4): its hero's own key, or the key a custom
     * hero put it on (its plain cell).
     */
    private static boolean opens(PowerData data, HeroAbility ability, int key) {
        if (data.power() != Power.CUSTOM) {
            return data.power() == ability.hero && key == ability.home;
        }
        HeroKit kit = data.kit();
        HeroKit.Binding binding = kit == null ? null : kit.plain(key);
        return binding != null && binding.ability() == ability && !binding.isAlt();
    }

    private static boolean hasOwnHex(LocalPlayer player) {
        return player.level().getData(ModAttachments.HEXES).byOwner(player.getUUID()).isPresent();
    }

    /**
     * The Vision key, for a Scarlet Witch with a Hex: "Vision, that one!" at the creature she looks at (nothing in view
     * calls him back to her side); with Shift, he holds his post, or follows her again.
     */
    private static void sendVisionOrder(LocalPlayer player) {
        Optional<Hex> hex = player.level().getData(ModAttachments.HEXES).byOwner(player.getUUID());
        if (hex.isEmpty()) {
            return;
        }
        if (player.isShiftKeyDown()) {
            PacketDistributor.sendToServer(new HexActionPayload(HexAction.SET_VISION_STAYS, Optional.empty(), hex.get().visionStays() ? 0 : 1));
        } else {
            PacketDistributor.sendToServer(new HexActionPayload(HexAction.VISION_TARGET, Optional.empty(), 0));
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Afterimages.render(event);
    }

    private ClientEvents() {
    }
}
