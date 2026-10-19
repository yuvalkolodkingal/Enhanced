package com.kolodkingal.enhanced.client.wolverine;

import com.kolodkingal.enhanced.client.EntityGlow;
import com.kolodkingal.enhanced.network.ScentPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Powers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What Wolverine smells, on his own client (3.7, {@link ScentPayload}): the creatures his Sniff found glow through
 * walls in their scent's colour ({@link EntityGlow}) until the time is up, and the scent trails drift as faint motes
 * along where they walked; Track's one creature glows gold and its trail leads from him to it, and his HUD points the
 * way ({@link #trackedAt}).
 */
public final class ScentView {
    /** The colours of the scents: monsters, animals, villagers, players (unless their hero has one), poison, Track. */
    public static final int MONSTER = 0xE8483C;
    public static final int ANIMAL = 0x5CD65C;
    public static final int VILLAGER = 0xF2D24A;
    public static final int PLAYER = 0xF0F0F0;
    public static final int POISONED = 0xA650E6;
    public static final int TRACKED = 0xFFB020;
    /** The trails' motes. */
    static final int TRAIL = 0xE8D6A8;

    private record Glow(byte kind, long until) {
    }

    private record Trail(List<Vec3> points, long until, boolean tracked) {
    }

    private static final Int2ObjectMap<Glow> GLOWS = new Int2ObjectOpenHashMap<>();
    private static final List<Trail> TRAILS = new ArrayList<>();
    /** Track's creature (its entity id, -1: none), where it was last told to be, until when. */
    private static int trackedId = -1;
    @Nullable
    private static Vec3 trackedAt;
    private static long trackedUntil;

    /** Registers the handler and the glow (the client setup calls it once). */
    public static void register() {
        Scents.handler(ScentView::accept);
        EntityGlow.register(ScentView::reveals, ScentView::color);
    }

    static void accept(ScentPayload payload) {
        Minecraft.getInstance().execute(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            long until = level.getGameTime() + payload.ticks();
            boolean track = false;
            for (ScentPayload.Scent scent : payload.scents()) {
                GLOWS.put(scent.entityId(), new Glow(scent.kind(), until));
                if (scent.kind() == ScentPayload.TRACKED) {
                    track = true;
                    trackedId = scent.entityId();
                    trackedUntil = until;
                }
            }
            TRAILS.removeIf(t -> t.tracked);
            for (ScentPayload.Trail trail : payload.trails()) {
                TRAILS.add(new Trail(List.copyOf(trail.points()), until, track));
                if (track && !trail.points().isEmpty()) {
                    trackedAt = trail.points().get(trail.points().size() - 1);
                }
            }
        });
    }

    /** Whether the local player smells this entity now. */
    static boolean reveals(Entity entity) {
        Glow glow = GLOWS.get(entity.getId());
        ClientLevel level = Minecraft.getInstance().level;
        return glow != null && level != null && level.getGameTime() <= glow.until;
    }

    static int color(Entity entity) {
        Glow glow = GLOWS.get(entity.getId());
        if (glow == null) {
            return PLAYER;
        }
        return switch (glow.kind) {
            case ScentPayload.MONSTER -> MONSTER;
            case ScentPayload.ANIMAL -> ANIMAL;
            case ScentPayload.VILLAGER -> VILLAGER;
            case ScentPayload.POISONED -> POISONED;
            case ScentPayload.TRACKED -> TRACKED;
            default -> entity instanceof Player p && Powers.power(p).hasPowers() ? Powers.color(p) : PLAYER;
        };
    }

    /** Every client tick: the motes of the trails drift up; what is over is forgotten. */
    public static void tick(ClientLevel level) {
        long now = level.getGameTime();
        GLOWS.int2ObjectEntrySet().removeIf(e -> e.getValue().until < now);
        TRAILS.removeIf(t -> t.until < now);
        if (trackedUntil < now) {
            trackedId = -1;
            trackedAt = null;
        }
        Entity tracked = trackedId >= 0 ? level.getEntity(trackedId) : null;
        if (tracked != null) {
            trackedAt = tracked.position();
        }
        if (now % 4 != 0) {
            return;
        }
        RandomSource r = level.random;
        for (Trail trail : TRAILS) {
            for (Vec3 p : trail.points) {
                if (!trail.tracked && r.nextInt(3) != 0) {
                    continue;
                }
                level.addParticle(new EnergyParticleOptions(trail.tracked ? TRACKED : TRAIL, trail.tracked ? 0.45F
                                : 0.3F), p.x + r.nextGaussian() * 0.15, p.y + 0.3 + r.nextDouble() * 0.4,
                        p.z + r.nextGaussian() * 0.15, 0, 0.01, 0);
            }
        }
    }

    /** Where Track's creature is, as his client knows it (null: no Track). */
    @Nullable
    public static Vec3 trackedAt() {
        return trackedId >= 0 ? trackedAt : null;
    }

    /** The distance to Track's creature, in whole blocks (-1: no Track). */
    public static int trackedDistance(LocalPlayer player) {
        Vec3 at = trackedAt();
        return at == null ? -1 : (int) Math.round(at.distanceTo(player.position()));
    }

    public static void clear() {
        GLOWS.clear();
        TRAILS.clear();
        trackedId = -1;
        trackedAt = null;
    }

    private ScentView() {
    }
}
