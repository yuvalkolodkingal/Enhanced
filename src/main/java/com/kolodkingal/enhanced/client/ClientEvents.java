package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.TommyAbilities;
import com.kolodkingal.enhanced.client.render.Afterimages;
import com.kolodkingal.enhanced.client.screen.HexScreen;
import com.kolodkingal.enhanced.network.AbilityKeyPayload;
import com.kolodkingal.enhanced.network.ChangeLookPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
        handleKeys(mc, player);
        spawnFlightParticles(mc.level);
        spawnSpeedTrails(mc.level);
        spawnFrostParticles(mc);
        Afterimages.tick(mc.level);
        Shapeshifts.tick(mc.level);
        QuicksilverMusic.tick(mc);
        PowerLoopSounds.tick(mc);
        SlowMotionEffect.tick(mc);
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
        Shapeshifts.clear();
        SlowMotionEffect.stop(Minecraft.getInstance());
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
     * Quicksilver leaves a silver-blue streak behind him; Tommy a teal-green mist, low along the ground, and a ring of
     * it while he spins a whirlwind.
     */
    private static void spawnSpeedTrails(ClientLevel level) {
        RandomSource random = level.random;
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (player.isInvisible()) {
                continue;
            }
            if (data.power() == Power.TOMMY && data.is(Live.WHIRLWIND)) {
                whirlwindParticles(level, player, random);
            }
            boolean tommy = data.power() == Power.TOMMY && data.is(Live.SPEEDING);
            if (!tommy && (data.power() != Power.QUICKSILVER || !data.slowMo())) {
                continue;
            }
            // Only while he actually moves, and laid down along the path he just ran (never in his own face).
            double dx = player.getX() - player.xOld;
            double dy = player.getY() - player.yOld;
            double dz = player.getZ() - player.zOld;
            if (dx * dx + dz * dz < 0.04) {
                continue;
            }
            if (tommy) {
                for (int i = 0; i < 4; i++) {
                    double back = 0.8 + random.nextDouble() * 1.6;
                    int color = i == 0 ? 0xE6FFF5 : i == 1 ? Power.TOMMY.glow : Power.TOMMY.color;
                    level.addParticle(new EnergyParticleOptions(color, 1.0F + random.nextFloat() * 0.4F),
                            player.getX() - dx * back + (random.nextDouble() - 0.5) * 0.6, player.getY() - dy * back + random.nextDouble() * random.nextDouble() * 1.6,
                            player.getZ() - dz * back + (random.nextDouble() - 0.5) * 0.6, 0, 0.005, 0);
                }
                continue;
            }
            for (int i = 0; i < 3; i++) {
                double back = 1.0 + random.nextDouble();
                level.addParticle(new EnergyParticleOptions(i == 0 ? Power.QUICKSILVER.glow : Power.QUICKSILVER.color, 1.1F),
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
            int color = random.nextInt(3) == 0 ? 0xE6FFF5 : random.nextBoolean() ? Power.TOMMY.glow : Power.TOMMY.color;
            level.addParticle(new EnergyParticleOptions(color, 1.1F), x, player.getY() + height, z, vx, 0.02, vz);
        }
    }

    /**
     * Quicksilver's footwork, done on the client because players move themselves: in Quicksilver Time he runs
     * straight up walls he runs into, and across water as long as he keeps running.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        PowerData data = Powers.get(player);
        if (data.power() != Power.QUICKSILVER || !data.slowMo() || !data.wallRun() || player.getAbilities().flying) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        if (player.horizontalCollision && player.zza > 0) {
            player.setDeltaMovement(motion.x, Math.max(motion.y, 0.42), motion.z);
            player.resetFallDistance();
            // Running into the wall would otherwise cancel the sprint (and with it Quicksilver Time) later this tick.
            player.minorHorizontalCollision = true;
            return;
        }
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
        for (int i = 1; i <= PowerData.ABILITIES; i++) {
            KeyMapping key = KeyBindings.ability(i);
            boolean clicked = false;
            while (key.consumeClick()) {
                clicked = true;
            }
            boolean usable = power.hasPowers() && mc.screen == null && !player.isSpectator();
            boolean down = usable && key.isDown();
            if (usable && clicked && !SENT_DOWN[i]) {
                if (i == 5 && power == Power.SCARLET_WITCH && !player.isShiftKeyDown() && hasOwnHex(player)) {
                    mc.setScreen(new HexScreen());
                    continue;
                }
                PacketDistributor.sendToServer(new AbilityKeyPayload(i, true));
                SENT_DOWN[i] = true;
            }
            if (!down && SENT_DOWN[i]) {
                PacketDistributor.sendToServer(new AbilityKeyPayload(i, false));
                SENT_DOWN[i] = false;
            }
        }
    }

    private static boolean hasOwnHex(LocalPlayer player) {
        return player.level().getData(ModAttachments.HEXES).byOwner(player.getUUID()).isPresent();
    }

    /** Wanda's hands trail red energy in flight, Photon leaves a trail of light, Magneto a faint violet field. */
    private static void spawnFlightParticles(ClientLevel level) {
        RandomSource random = level.random;
        for (AbstractClientPlayer player : level.players()) {
            PowerData data = Powers.get(player);
            if (!data.flying() || player.isInvisible()) {
                continue;
            }
            switch (data.power()) {
                case SCARLET_WITCH -> {
                    for (HumanoidArm arm : HumanoidArm.values()) {
                        Vec3 hand = handInWorld(player, arm);
                        level.addParticle(new EnergyParticleOptions(random.nextBoolean() ? Power.SCARLET_WITCH.color : Power.SCARLET_WITCH.glow, 1.0F),
                                hand.x + (random.nextDouble() - 0.5) * 0.15, hand.y + (random.nextDouble() - 0.5) * 0.15,
                                hand.z + (random.nextDouble() - 0.5) * 0.15, 0, 0.01, 0);
                    }
                }
                case PHOTON -> level.addParticle(new EnergyParticleOptions(Power.PHOTON.color, 1.3F),
                        player.getX() + (random.nextDouble() - 0.5) * 0.4, player.getY() + 0.2 + random.nextDouble() * 1.2,
                        player.getZ() + (random.nextDouble() - 0.5) * 0.4, 0, 0, 0);
                case MAGNETO -> {
                    if (player.tickCount % 2 == 0) {
                        level.addParticle(new EnergyParticleOptions(Power.MAGNETO.glow, 0.9F),
                                player.getX() + (random.nextDouble() - 0.5) * 0.8, player.getY() - 0.05,
                                player.getZ() + (random.nextDouble() - 0.5) * 0.8, 0, -0.03, 0);
                    }
                }
                case QUICKSILVER, VISION, BILLY, TOMMY, LOKI, THOR, NONE -> {
                }
            }
        }
    }

    /** Rough position of a hand of a player standing or flying upright. */
    private static Vec3 handInWorld(Player player, HumanoidArm arm) {
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        double side = arm == HumanoidArm.RIGHT ? -0.38 : 0.38;
        double x = -Mth.sin(yaw) * 0.05 + Mth.cos(yaw) * side;
        double z = Mth.cos(yaw) * 0.05 + Mth.sin(yaw) * side;
        return player.position().add(x, player.isCrouching() ? 0.6 : 0.75, z);
    }

    /** Players currently drawn with a hover bob (their pose stack was pushed in Pre and must be popped in Post). */
    private static final java.util.Set<Integer> BOBBING = new java.util.HashSet<>();

    /** Flying heroes float with a gentle up-and-down bob. */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PowerData data = Powers.get(player);
        if (data.power().hasPowers() && data.flying() && !player.onGround()) {
            float t = player.tickCount + event.getPartialTick();
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(0.0, Mth.sin(t * 0.08F) * 0.06, 0.0);
            BOBBING.add(player.getId());
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (BOBBING.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
        Afterimages.render(event);
    }

    private ClientEvents() {
    }
}
