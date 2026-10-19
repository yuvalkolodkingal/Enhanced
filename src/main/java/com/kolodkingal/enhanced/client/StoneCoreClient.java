package com.kolodkingal.enhanced.client;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.screen.StoneWheelScreen;
import com.kolodkingal.enhanced.client.stones.StoneFx;
import com.kolodkingal.enhanced.item.InfinityGauntletItem;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.stones.GauntletContents;
import com.kolodkingal.enhanced.stones.Snap;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The client of the stones' core (3.6, lane L1): the Gauntlet's socket model ({@code enhanced:stones}, the bitmask of
 * its stones as a model predicate, so 64 models light the right sockets; Thanos's Gauntlet is the five), the moments of
 * {@code StoneFxPayload} that are lane L1's (a rejection, a slip, a stone taken, the Snap), the Stone Wheel key, and
 * the Snap's wait in the Gauntlet's tooltip.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class StoneCoreClient {
    /** The model predicate of the Gauntlet's lit sockets: the stones' bitmask over 64. */
    public static final net.minecraft.resources.ResourceLocation STONES = Enhanced.id("stones");

    /** From the client setup (in its work queue). */
    public static void setup() {
        ItemProperties.register(ModItems.INFINITY_GAUNTLET.get(), STONES,
                (stack, level, entity, seed) -> InfinityGauntletItem.contents(stack).stones() / 64.0F);
        StoneFx.register(StoneFxPayload.REJECT, StoneCoreClient::reject);
        StoneFx.register(StoneFxPayload.SLIP, StoneCoreClient::slip);
        StoneFx.register(StoneFxPayload.PEDESTAL_TAKE, StoneCoreClient::taken);
        StoneFx.register(StoneFxPayload.SNAP, StoneCoreClient::snap);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        StoneWheelScreen.tick(Minecraft.getInstance());
    }

    /** A Gauntlet with six stones says when it may snap again. */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null || !InfinityGauntletItem.crafted(event.getItemStack())) {
            return;
        }
        GauntletContents contents = InfinityGauntletItem.contents(event.getItemStack());
        long wait = Snap.waitLeft(event.getEntity());
        if (contents.full() && wait > 0) {
            event.getToolTip().add(Component.translatable("hud.enhanced.stones.snap_wait", StoneHud.clock(wait))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static StoneKind kind(StoneFxPayload payload) {
        StoneKind[] kinds = StoneKind.values();
        return kinds[Mth.clamp(payload.arg(), 0, kinds.length - 1)];
    }

    /** A stone rejects its carrier: a ring of its colour round his hand; his own screen burns red at the edges. */
    private static void reject(StoneFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.points().isEmpty()) {
            return;
        }
        StoneKind kind = kind(payload);
        Vec3 at = payload.points().get(0);
        for (int i = 0; i < 20; i++) {
            float a = Mth.TWO_PI * i / 20;
            level.addParticle(new EnergyParticleOptions(kind.color(), 0.9F), at.x + Mth.cos(a) * 0.5, at.y,
                    at.z + Mth.sin(a) * 0.5, Mth.cos(a) * 0.08, 0.02, Mth.sin(a) * 0.08);
        }
        if (Minecraft.getInstance().player != null && payload.entityId() == Minecraft.getInstance().player.getId()) {
            StoneHud.burn();
        }
    }

    /** A stone slips away: a few sparks where it lands. */
    private static void slip(StoneFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.points().isEmpty()) {
            return;
        }
        StoneKind kind = kind(payload);
        Vec3 at = payload.points().get(0);
        for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.2, at.z, (level.random.nextDouble() - 0.5)
                    * 0.2, 0.1, (level.random.nextDouble() - 0.5) * 0.2);
        }
        level.addParticle(new EnergyParticleOptions(kind.color(), 0.7F), at.x, at.y + 0.2, at.z, 0.0, 0.05, 0.0);
    }

    /** A stone taken from a pedestal (or out of the Orb): a column of its colour rises and fades. */
    private static void taken(StoneFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.points().isEmpty()) {
            return;
        }
        StoneKind kind = kind(payload);
        Vec3 at = payload.points().get(0);
        for (int i = 0; i < 24; i++) {
            float a = Mth.TWO_PI * i / 12;
            level.addParticle(new EnergyParticleOptions(kind.color(), 0.8F), at.x + Mth.cos(a) * 0.3,
                    at.y - 0.3 + i * 0.05, at.z + Mth.sin(a) * 0.3, 0.0, 0.06, 0.0);
        }
    }

    /** The Snap: a white flash for everyone near (fainter far away), and a ring of the six colours round the hand. */
    private static void snap(StoneFxPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || payload.points().isEmpty()) {
            return;
        }
        Vec3 at = payload.points().get(0);
        double distance = mc.player.position().distanceTo(at);
        StoneHud.flash((float) Mth.clamp(1.0 - distance / 96.0, 0.2, 1.0));
        StoneKind[] kinds = StoneKind.values();
        for (int i = 0; i < 36; i++) {
            float a = Mth.TWO_PI * i / 36;
            StoneKind kind = kinds[i % kinds.length];
            level.addParticle(new EnergyParticleOptions(kind.color(), 1.1F), at.x, at.y, at.z, Mth.cos(a) * 0.35,
                    0.02, Mth.sin(a) * 0.35);
        }
    }

    private StoneCoreClient() {
    }
}
