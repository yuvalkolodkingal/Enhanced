package com.kolodkingal.enhanced.client.ironman.suits;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.block.SuitDisplayBlockEntity;
import com.kolodkingal.enhanced.client.render.HeroAnimState;
import com.kolodkingal.enhanced.client.render.HeroAnimator;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.ironman.IronManState;
import com.kolodkingal.enhanced.ironman.IronSuits;
import com.kolodkingal.enhanced.ironman.SuitKind;
import com.kolodkingal.enhanced.ironman.SuitParts;
import com.kolodkingal.enhanced.ironman.SuitTimelines;
import com.kolodkingal.enhanced.platform.event.Side;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.client.ClientTickEvent;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * The suits' small motions and the suit-up's effects on every client (3.8, PLAN 7.2), once a client tick: the moving
 * extras eased toward the synced state (blades, shields, the Mark 50's cannon, the Mark 85's back flaps, the Mark 3's
 * pods), each Mark's per-piece effects as its pieces land (the Mark 1's clank, sparks and steam, the Mark 3's bolts and
 * the gantry's whirr, the Mark 5's clacks and its faceplate's slam, the nanites' glitter at the wave's edge), and
 * which tower display's gantry serves a Mark 3 suit-up. The phase's opening sound is the Iron Man lane's (its FX
 * payload); these are the pieces'.
 */
@Subscriber(side = Side.CLIENT)
public final class SuitFx {
    public static final int BLADES = 0;
    public static final int SHIELD = 1;
    public static final int CANNON = 2;
    public static final int FLAPS = 3;
    public static final int PODS = 4;
    private static final int COUNT = 5;
    /** Per tick, how far each extra moves toward its target. */
    private static final float[] STEP = {0.25F, 0.2F, 0.2F, 0.16F, 0.34F};
    /** How long the Mark 3's pods stay open after the mini-missiles. */
    private static final int PODS_TICKS = 30;

    /** Per player: each extra at the last tick and at this one. */
    private static final Int2ObjectMap<float[]> EXTRAS = new Int2ObjectOpenHashMap<>();
    private static final Int2LongMap PODS_UNTIL = new Int2LongOpenHashMap();
    /** Per player in a Mark 3 suit-up near a display: the display, and the phase it serves. */
    private static final Int2ObjectMap<Link> GANTRY = new Int2ObjectOpenHashMap<>();

    /** A Mark 3 suit-up served by a display's gantry. */
    public record Link(BlockPos display, long phaseStart) {
    }

    /** The Mark 3's shoulder pods open (the Iron Man lane calls it with the mini-missiles' FX). */
    public static void openPods(int entityId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            PODS_UNTIL.put(entityId, level.getGameTime() + PODS_TICKS);
        }
    }

    /** How far one of a player's extras is out this frame (0-1, smoothed). */
    public static float eased(Player player, int which, float partialTick) {
        float[] f = EXTRAS.get(player.getId());
        if (f == null) {
            return 0.0F;
        }
        return SuitTimelines.smooth(Mth.lerp(partialTick, f[which], f[COUNT + which]));
    }

    /** The display whose gantry serves this player's Mark 3 suit-up, or null. */
    @Nullable
    public static BlockPos gantryOf(Player player) {
        Link link = GANTRY.get(player.getId());
        return link == null ? null : link.display();
    }

    /** The player a display's gantry serves now, or null. */
    @Nullable
    public static Player servedBy(ClientLevel level, BlockPos display) {
        for (Int2ObjectMap.Entry<Link> e : GANTRY.int2ObjectEntrySet()) {
            if (e.getValue().display().equals(display) && level.getEntity(e.getIntKey()) instanceof Player p) {
                return p;
            }
        }
        return null;
    }

    @Subscribe
    public static void onTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            EXTRAS.clear();
            PODS_UNTIL.clear();
            GANTRY.clear();
            return;
        }
        long now = level.getGameTime();
        Int2ObjectMap<float[]> seen = new Int2ObjectOpenHashMap<>();
        for (Player p : level.players()) {
            SuitKind kind = SuitRender.shownSuit(p);
            float[] f = EXTRAS.get(p.getId());
            if (kind == null && f == null) {
                GANTRY.remove(p.getId());
                continue;
            }
            if (f == null) {
                f = new float[COUNT * 2];
            }
            float[] target = targets(p, kind, now);
            boolean any = false;
            for (int i = 0; i < COUNT; i++) {
                f[i] = f[COUNT + i];
                float cur = f[COUNT + i];
                f[COUNT + i] = target[i] > cur ? Math.min(target[i], cur + STEP[i])
                        : Math.max(target[i], cur - STEP[i]);
                any |= f[i] > 0.0F || f[COUNT + i] > 0.0F;
            }
            if (kind != null || any) {
                seen.put(p.getId(), f);
            }
            pieces(level, p, now);
        }
        EXTRAS.clear();
        EXTRAS.putAll(seen);
        PODS_UNTIL.int2LongEntrySet().removeIf(e -> e.getLongValue() < now);
    }

    private static float[] targets(Player p, @Nullable SuitKind kind, long now) {
        float[] t = new float[COUNT];
        if (kind == null) {
            return t;
        }
        PowerData data = Powers.get(p);
        boolean blades = data.is(Live.IRON_BLADES) && kind.nanotech();
        t[BLADES] = blades ? 1.0F : 0.0F;
        t[SHIELD] = data.is(Live.IRON_SHIELD) && kind.nanotech() ? 1.0F : 0.0F;
        t[CANNON] = data.is(Live.IRON_CHARGING) && kind == SuitKind.MARK_50 ? 1.0F : 0.0F;
        boolean flying = p instanceof LocalPlayer lp ? lp.getAbilities().flying : data.flying();
        t[FLAPS] = kind == SuitKind.MARK_85 && (blades || flying && !p.onGround()) ? 1.0F : 0.0F;
        t[PODS] = kind == SuitKind.MARK_3 && (PODS_UNTIL.getOrDefault(p.getId(), 0L) >= now
                || playing(p, "IRON_MISSILES")) ? 1.0F : 0.0F;
        return t;
    }

    /** Whether a gesture of this name plays on the player now (the Iron Man lane's, by name). */
    private static boolean playing(Player p, String gesture) {
        HeroAnimState state = HeroAnimator.peek(p);
        if (state == null) {
            return false;
        }
        for (HeroAnimState.Slot slot : state.slots) {
            if (slot.active && slot.id != null && slot.id.name().equals(gesture)) {
                return true;
            }
        }
        return false;
    }

    // ---- the pieces' effects ----

    private static void pieces(ClientLevel level, Player p, long now) {
        IronManState state = IronSuits.state(p);
        SuitKind kind = state.phaseKind();
        if (!state.inPhase() || kind == null) {
            GANTRY.remove(p.getId());
            return;
        }
        boolean up = state.phase() == IronManState.UP;
        if (kind == SuitKind.MARK_3 && up) {
            Link link = GANTRY.get(p.getId());
            if (link == null || link.phaseStart() != state.phaseStart()) {
                BlockPos display = findDisplay(level, p);
                if (display != null) {
                    GANTRY.put(p.getId(), new Link(display, state.phaseStart()));
                } else {
                    GANTRY.remove(p.getId());
                }
            }
        } else {
            GANTRY.remove(p.getId());
        }
        float before = SuitTimelines.progress(now - 1, state.phaseStart(), state.phaseTicks(), 0.0F);
        float after = SuitTimelines.progress(now, state.phaseStart(), state.phaseTicks(), 0.0F);
        if (after <= 0.0F || before >= 1.0F) {
            return;
        }
        RandomSource random = level.getRandom();
        for (SuitParts part : SuitParts.VALUES) {
            float r0 = SuitTimelines.reveal(kind, part, before, up);
            float r1 = SuitTimelines.reveal(kind, part, after, up);
            boolean landed = up ? r0 < 1.0F && r1 >= 1.0F : r0 > 0.0F && r1 <= 0.0F;
            Vec3 at = where(p, part);
            if (kind.nanotech()) {
                if (r1 > 0.0F && r1 < 1.0F || r0 > 0.0F && r0 < 1.0F) {
                    glitter(level, at, random, kind);
                }
                continue;
            }
            if (!landed) {
                continue;
            }
            switch (kind) {
                case MARK_1 -> {
                    play(level, at, ModSounds.MARK1_CLANK.get(), 0.7F, up ? 0.9F + random.nextFloat() * 0.2F : 0.7F);
                    for (int i = 0; i < 6; i++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
                                (random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.2,
                                (random.nextDouble() - 0.5) * 0.3);
                    }
                    level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, 0.0, 0.1, 0.0);
                    if (part == SuitParts.BACK || part == SuitParts.FACEPLATE) {
                        steam(level, where(p, SuitParts.BACK), p, random);
                    }
                }
                case MARK_3 -> {
                    if (part == SuitParts.FACEPLATE) {
                        play(level, at, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.6F, 1.4F);
                    } else if (GANTRY.containsKey(p.getId())) {
                        play(level, at, ModSounds.MARK3_GANTRY.get(), 0.35F, 1.0F + random.nextFloat() * 0.3F);
                    } else {
                        play(level, at, SoundEvents.ARMOR_EQUIP_IRON.value(), 0.5F, 1.2F);
                    }
                }
                case MARK_5 -> {
                    if (part == SuitParts.FACEPLATE) {
                        play(level, at, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.8F, 1.0F);
                    } else {
                        play(level, at, SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.45F, 1.3F);
                    }
                }
                default -> {
                }
            }
        }
    }

    /** About where a part of the suit is in the world (for the sparks and the sounds). */
    static Vec3 where(Player p, SuitParts part) {
        float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
        double lx;
        double ly;
        double lz = 0.0;
        switch (part) {
            case HELMET -> ly = 1.62;
            case FACEPLATE -> {
                ly = 1.6;
                lz = 0.28;
            }
            case CHEST -> {
                ly = 1.2;
                lz = 0.2;
            }
            case BACK -> {
                ly = 1.2;
                lz = -0.22;
            }
            default -> ly = 0.0;
        }
        String n = part.name();
        lx = n.endsWith("ARM") || n.endsWith("FOREARM") || n.endsWith("GAUNTLET") ? 0.37 : n.endsWith("THIGH")
                || n.endsWith("SHIN") || n.endsWith("BOOT") ? 0.12 : 0.0;
        if (part.right()) {
            lx = -lx;
        }
        if (n.endsWith("UPPER_ARM")) {
            ly = 1.3;
        } else if (n.endsWith("FOREARM")) {
            ly = 1.0;
        } else if (n.endsWith("GAUNTLET")) {
            ly = 0.78;
        } else if (n.endsWith("THIGH")) {
            ly = 0.6;
        } else if (n.endsWith("SHIN")) {
            ly = 0.35;
        } else if (n.endsWith("BOOT")) {
            ly = 0.08;
        }
        double scale = p.getScale();
        double cos = Mth.cos(yaw);
        double sin = Mth.sin(yaw);
        // left = (cos, sin), forward = (-sin, cos) in the x-z plane
        double x = p.getX() + (lx * cos - lz * sin) * scale;
        double z = p.getZ() + (lx * sin + lz * cos) * scale;
        return new Vec3(x, p.getY() + ly * scale, z);
    }

    private static void glitter(ClientLevel level, Vec3 at, RandomSource random, SuitKind kind) {
        Vector3f gold = new Vector3f(0.95F, 0.75F, 0.3F);
        Vector3f red = kind == SuitKind.MARK_85 ? new Vector3f(0.6F, 0.05F, 0.08F) : new Vector3f(0.7F, 0.1F, 0.1F);
        for (int i = 0; i < 2; i++) {
            double dx = (random.nextDouble() - 0.5) * 0.3;
            double dy = (random.nextDouble() - 0.5) * 0.3;
            double dz = (random.nextDouble() - 0.5) * 0.3;
            level.addParticle(new DustParticleOptions(random.nextBoolean() ? gold : red, 0.45F), at.x + dx,
                    at.y + dy, at.z + dz, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0.0, 0.02, 0.0);
        }
    }

    private static void steam(ClientLevel level, Vec3 at, Player p, RandomSource random) {
        float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
        double bx = Mth.sin(yaw) * 0.05;
        double bz = -Mth.cos(yaw) * 0.05;
        for (int i = 0; i < 5; i++) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, at.x, at.y + 0.1, at.z, bx + (random.nextDouble() - 0.5)
                    * 0.02, 0.05 + random.nextDouble() * 0.03, bz);
        }
    }

    private static void play(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    /** The nearest suit display within the gantry's reach ({@code [iron_man] gantryRange}). */
    @Nullable
    private static BlockPos findDisplay(ClientLevel level, Player p) {
        double range = EnhancedConfig.SPEC.isLoaded() ? IronManConfig.GANTRY_RANGE.get()
                : IronManConfig.GANTRY_RANGE.getDefault();
        int reach = Mth.ceil(range);
        BlockPos centre = p.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-reach, -2, -reach), centre.offset(reach, 2, reach))) {
            if (level.getBlockEntity(pos) instanceof SuitDisplayBlockEntity) {
                double d = pos.distToCenterSqr(p.position());
                if (d < bestDist && d <= (range + 0.5) * (range + 0.5)) {
                    bestDist = d;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    private SuitFx() {
    }
}
