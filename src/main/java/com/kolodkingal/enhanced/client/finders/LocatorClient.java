package com.kolodkingal.enhanced.client.finders;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.client.school.LabFx;
import com.kolodkingal.enhanced.client.stones.StoneFx;
import com.kolodkingal.enhanced.finders.LocatorState;
import com.kolodkingal.enhanced.network.LabFxPayload;
import com.kolodkingal.enhanced.network.StoneFxPayload;
import com.kolodkingal.enhanced.registry.ModDataComponents;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Stone Locator on the client (3.7, lane L5): the needle (the item model's {@code enhanced:angle}, 32 angles, the
 * vanilla compass's way and wobble, toward the stack's {@code locator.target}; no target or another realm: it spins;
 * searching: it sweeps slowly; all six: north), the socket's glow in the tuned stone's colour (white with all six), and
 * the spark trail ({@code StoneFxPayload.LOCATOR_TRAIL}: sparks in the stone's colour fly along the points over 20
 * ticks and fade). It also hears the Cerebro console turn on and off ({@code LabFxPayload.CONSOLE_ON|OFF}) for {@link
 * CerebroSphere}.
 */
@EventBusSubscriber(modid = Enhanced.MODID, value = Dist.CLIENT)
public final class LocatorClient {
    public static final ResourceLocation ANGLE = Enhanced.id("angle");
    /** A full slow sweep of the needle while it searches (ticks). */
    static final int SWEEP_TICKS = 160;
    /** How long a spark trail flies. */
    static final int TRAIL_TICKS = 20;

    private static final CompassItemPropertyFunction COMPASS = new CompassItemPropertyFunction(LocatorClient::target);

    /** A spark trail in flight. */
    private record Trail(List<Vec3> points, int rgb, int[] age) {
    }

    private static final List<Trail> TRAILS = new ArrayList<>();

    /** From the client setup (ClientSetup's finders region). */
    public static void setup() {
        ItemProperties.register(ModItems.STONE_LOCATOR.get(), ANGLE, LocatorClient::angle);
        StoneFx.register(StoneFxPayload.LOCATOR_TRAIL, LocatorClient::trail);
        LabFx.register(LabFxPayload.CONSOLE_ON, p -> CerebroSphere.console(p.pos(), true));
        LabFx.register(LabFxPayload.CONSOLE_OFF, p -> CerebroSphere.console(p.pos(), false));
    }

    /** What a Locator knows (nothing without the component). */
    static LocatorState state(ItemStack stack) {
        LocatorState state = stack.get(ModDataComponents.LOCATOR.get());
        return state == null ? LocatorState.EMPTY : state;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The needle

    /** The needle's angle (0 to 1, clockwise from ahead). */
    static float angle(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity holder, int seed) {
        LocatorState state = state(stack);
        if (state.status() == LocatorState.SEARCHING && state.target().isEmpty()) {
            long time = level == null ? 0 : level.getGameTime();
            return (time % SWEEP_TICKS) / (float) SWEEP_TICKS;
        }
        return COMPASS.unclampedCall(stack, level, holder, seed);
    }

    /** Where the vanilla compass's function points: the target; all six: far to the north; else nowhere (it spins). */
    @Nullable
    private static GlobalPos target(ClientLevel level, ItemStack stack, Entity entity) {
        LocatorState state = state(stack);
        if (state.status() == LocatorState.HAS_ALL) {
            BlockPos at = entity == null ? BlockPos.ZERO : entity.blockPosition();
            return GlobalPos.of(level.dimension(), at.offset(0, 0, -100000));
        }
        return state.target().orElse(null);
    }

    /** The socket's colour: the tuned stone's; white with all six or untuned. */
    static int socketColor(ItemStack stack) {
        LocatorState state = state(stack);
        if (state.status() == LocatorState.HAS_ALL || state.tuned().isEmpty()) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | state.tuned().get().color();
    }

    // ------------------------------------------------------------------------------------------------------------
    // The spark trail

    private static void trail(StoneFxPayload payload) {
        if (payload.points().isEmpty()) {
            return;
        }
        StoneKind[] kinds = StoneKind.values();
        int rgb = kinds[Math.floorMod(payload.arg(), kinds.length)].color();
        synchronized (TRAILS) {
            TRAILS.add(new Trail(List.copyOf(payload.points()), rgb, new int[1]));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        CerebroSphere.tick(mc);
        if (level == null || mc.isPaused()) {
            return;
        }
        synchronized (TRAILS) {
            Iterator<Trail> it = TRAILS.iterator();
            while (it.hasNext()) {
                Trail trail = it.next();
                int age = trail.age()[0]++;
                if (age >= TRAIL_TICKS + 6) {
                    it.remove();
                    continue;
                }
                sparks(level, trail, age);
            }
        }
    }

    /** One tick of a trail: the head flies along the points, a fading tail behind it. */
    private static void sparks(ClientLevel level, Trail trail, int age) {
        List<Vec3> points = trail.points();
        float head = Mth.clamp((age + 1) / (float) TRAIL_TICKS, 0.0F, 1.0F) * (points.size() - 1);
        int rgb = trail.rgb();
        Vector3f color = new Vector3f((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
        float fade = age < TRAIL_TICKS ? 1.0F : 1.0F - (age - TRAIL_TICKS) / 6.0F;
        for (int i = 0; i <= (int) head; i++) {
            if (head - i > 4.0F) {
                continue;
            }
            Vec3 at = points.get(i);
            float size = (1.4F - 0.25F * (head - i)) * fade;
            if (size > 0.2F) {
                level.addParticle(new DustParticleOptions(color, size), at.x, at.y, at.z, 0.0, 0.01, 0.0);
            }
        }
        if (age < TRAIL_TICKS) {
            Vec3 at = points.get(Math.min(points.size() - 1, (int) head));
            level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        synchronized (TRAILS) {
            TRAILS.clear();
        }
        CerebroSphere.clear();
    }

    /** The socket's tint (a mod-bus event, routed by its type). */
    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 1 ? socketColor(stack) : -1, ModItems.STONE_LOCATOR.get());
    }

    private LocatorClient() {
    }
}
