package com.kolodkingal.enhanced.client.tower;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.block.TechCaseBlockEntity;
import com.kolodkingal.enhanced.network.TowerFxPayload;
import com.kolodkingal.enhanced.registry.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The client's side of the Avengers Tower's armoury (3.8, lane L6): the drawers of {@link TowerFxPayload} (a suit
 * taken: sparks round the display; refused: a puff; a case's lid opening and closing, which its renderer slides) and
 * the suit display's stand-in renderer ({@link SuitDisplayStandIn}). The stand-in is registered before the client
 * setup's own renderers ({@link EventPriority#HIGHEST}), so lane L7's display renderer, registered there, takes its
 * place when the lanes are merged; until then the displays show the suits of {@code SuitModels} (the 3.8 skeleton's
 * boxes, or lane L7's models).
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class TowerClient {

    /** The drawers (from the client setup's {@code tower (3.8)} region). */
    public static void setup() {
        TowerFx.register(TowerFxPayload.DISPLAY_TAKE, TowerClient::taken);
        TowerFx.register(TowerFxPayload.DISPLAY_REFUSE, TowerClient::refused);
        TowerFx.register(TowerFxPayload.CASE_OPEN, TowerClient::opened);
        TowerFx.register(TowerFxPayload.CASE_CLOSE, TowerClient::closed);
    }

    /** The display's stand-in, first of all the renderers: lane L7's display renderer replaces it. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SUIT_DISPLAY.get(), SuitDisplayStandIn::new);
    }

    private static void taken(TowerFxPayload payload) {
        burst(payload.pos(), ParticleTypes.ELECTRIC_SPARK, 14, 2.0, 0.15);
        burst(payload.pos(), ParticleTypes.END_ROD, 4, 2.0, 0.03);
    }

    private static void refused(TowerFxPayload payload) {
        burst(payload.pos(), ParticleTypes.SMOKE, 6, 1.0, 0.02);
    }

    private static void opened(TowerFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(payload.pos()) instanceof TechCaseBlockEntity box) {
            box.openedAt = level.getGameTime();
            box.closedAt = Long.MIN_VALUE;
        }
        burst(payload.pos(), ParticleTypes.END_ROD, 6, 1.0, 0.04);
    }

    private static void closed(TowerFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(payload.pos()) instanceof TechCaseBlockEntity box) {
            box.closedAt = level.getGameTime();
        }
    }

    /** {@code count} particles in the block (and {@code height} blocks over it), drifting by up to {@code speed}. */
    private static void burst(BlockPos p, ParticleOptions particle, int count, double height, double speed) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RandomSource random = level.random;
        for (int i = 0; i < count; i++) {
            level.addParticle(particle, p.getX() + 0.15 + random.nextDouble() * 0.7,
                    p.getY() + 0.2 + random.nextDouble() * height, p.getZ() + 0.15 + random.nextDouble() * 0.7,
                    (random.nextDouble() - 0.5) * speed, random.nextDouble() * speed, (random.nextDouble() - 0.5)
                            * speed);
        }
    }

    private TowerClient() {
    }
}
