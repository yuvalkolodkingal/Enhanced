package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.screen.HexScreen;
import com.kolodkingal.enhanced.network.AbilityKeyPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
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
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
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
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        java.util.Arrays.fill(SENT_DOWN, false);
    }

    private static void handleKeys(Minecraft mc, LocalPlayer player) {
        Power power = Powers.power(player);
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
                case NONE -> {
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

    /** Arms up and forward while casting: beaming, firing shrapnel or holding something with telekinesis. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PowerData data = Powers.get(player);
        boolean casting = (data.power() == Power.SCARLET_WITCH && data.beamActive())
                || (data.power() == Power.MAGNETO && data.shrapnelActive())
                || data.heldEntityId() != -1;
        if (!casting) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
        if (player.getMainArm() == HumanoidArm.RIGHT) {
            model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        } else {
            model.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
        }
    }

    private ClientEvents() {
    }
}
