package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dead a hero Hela raises (3.5): who serves whom, for how long, and the rule that keeps them on her side. Raise the
 * Dead (B) calls berserkers, Sneak + B Fenris; they climb out of the ground round her, fight what she fights, never hurt
 * her or her side ({@link #sameSide}: the ally rule of {@code AreaTargets} too), and crumble when their time is over, or
 * when she logs out, dies or loses her power ({@link #dismiss}).
 *
 * <p>Allies are entities with an owner: the boss framework's orphan sweep never takes them, and a boss fight's arena
 * counts them as hers only by their owner (the {@code Live.HELA_SUMMONING} bit says she has some).
 */
public final class HelaArmy {
    /** A creature of hers made with no fight and no owner (a test's, an operator's summon): it does not crumble by itself. */
    public static final String LOOSE_TAG = Enhanced.MODID + ":hela_loose";

    /** Hero -> the UUIDs of her dead who walk. */
    private static final Map<UUID, List<UUID>> ALLIES = new HashMap<>();

    /**
     * Whether {@code a} and {@code b} fight on the same side of Hela's: a hero and her dead, two of the dead of one hero,
     * or one of the dead and the hero's team.
     */
    public static boolean sameSide(LivingEntity a, LivingEntity b) {
        return sides(a, b) || sides(b, a);
    }

    private static boolean sides(Entity a, Entity b) {
        if (!(a instanceof ArmyMob mob) || mob.ownerId() == null) {
            return false;
        }
        UUID owner = mob.ownerId();
        if (b.getUUID().equals(owner) || b instanceof ArmyMob other && owner.equals(other.ownerId())) {
            return true;
        }
        Player hero = mob.owner();
        return hero != null && hero.isAlliedTo(b);
    }

    /**
     * Raises {@code count} berserkers round {@code hero}, each for {@code ticks}: they climb out of the ground between 2 and
     * 4 blocks away. Returns them.
     */
    public static List<BerserkerEntity> raise(ServerPlayer hero, int count, int ticks) {
        ServerLevel level = hero.serverLevel();
        List<BerserkerEntity> raised = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = hero.getYRot() * Math.PI / 180.0 + Math.PI * 2.0 * i / count + Math.PI / count;
            double r = 2.2 + (i % 2) * 1.4;
            Vec3 at = HelaRules.onGround(level, hero.position().add(-Math.sin(angle) * r, 0.5, Math.cos(angle) * r));
            BerserkerEntity berserker = new BerserkerEntity(ModEntities.BERSERKER.get(), level);
            berserker.moveTo(at.x, at.y, at.z, hero.getYRot(), 0.0F);
            berserker.serve(hero, ticks);
            berserker.beginRise();
            level.addFreshEntity(berserker);
            HelaFx.send(level, HelaFx.RISE, berserker.getId(), at, 0);
            raised.add(berserker);
            remember(hero, berserker);
        }
        level.playSound(null, hero.getX(), hero.getY(), hero.getZ(), com.kolodkingal.enhanced.registry.ModSounds.BERSERKER_RISE.get(),
                SoundSource.PLAYERS, 1.3F, 1.0F);
        return raised;
    }

    /** Calls Fenris to {@code hero} for {@code ticks}: he rises out of the ground behind her. */
    public static FenrisEntity fenris(ServerPlayer hero, int ticks) {
        ServerLevel level = hero.serverLevel();
        Vec3 back = hero.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(-3.0);
        Vec3 at = HelaRules.onGround(level, hero.position().add(back).add(0.0, 0.5, 0.0));
        FenrisEntity wolf = new FenrisEntity(ModEntities.FENRIS.get(), level);
        wolf.moveTo(at.x, at.y, at.z, hero.getYRot(), 0.0F);
        wolf.serve(hero, ticks);
        wolf.beginRise();
        level.addFreshEntity(wolf);
        HelaFx.send(level, HelaFx.FENRIS, wolf.getId(), at, 0);
        level.playSound(null, at.x, at.y, at.z, com.kolodkingal.enhanced.registry.ModSounds.FENRIS_HOWL.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        remember(hero, wolf);
        return wolf;
    }

    private static void remember(ServerPlayer hero, ArmyMob mob) {
        ALLIES.computeIfAbsent(hero.getUUID(), k -> new ArrayList<>()).add(mob.getUUID());
    }

    /** Her dead who still walk in her level (forgetting the ones that are gone). */
    public static List<ArmyMob> alive(ServerPlayer hero) {
        List<UUID> ids = ALLIES.get(hero.getUUID());
        List<ArmyMob> alive = new ArrayList<>();
        if (ids == null) {
            return alive;
        }
        ServerLevel level = hero.serverLevel();
        ids.removeIf(id -> !(level.getEntity(id) instanceof ArmyMob mob) || !mob.isAlive());
        for (UUID id : ids) {
            if (level.getEntity(id) instanceof ArmyMob mob) {
                alive.add(mob);
            }
        }
        if (ids.isEmpty()) {
            ALLIES.remove(hero.getUUID());
        }
        return alive;
    }

    /** Whether she has any of her dead walking. */
    public static boolean any(ServerPlayer hero) {
        return !alive(hero).isEmpty();
    }

    /** The game time at which the last of her dead crumbles, or 0 with none. */
    public static long lastExpiry(ServerPlayer hero) {
        long last = 0L;
        for (ArmyMob mob : alive(hero)) {
            last = Math.max(last, mob.expiresAt());
        }
        return last;
    }

    /** Every one of her dead crumbles now (she logs out, dies, changes realm, loses her power). */
    public static void dismiss(ServerPlayer hero) {
        List<UUID> ids = ALLIES.remove(hero.getUUID());
        if (ids == null) {
            return;
        }
        for (ServerLevel level : hero.server.getAllLevels()) {
            for (UUID id : ids) {
                if (level.getEntity(id) instanceof ArmyMob mob) {
                    mob.crumble();
                }
            }
        }
    }

    /** The owner of one of the dead, if it serves a hero who is here. */
    @Nullable
    public static Player ownerOf(Entity entity) {
        return entity instanceof ArmyMob mob ? mob.owner() : null;
    }

    public static void clear() {
        ALLIES.clear();
    }

    private HelaArmy() {
    }
}
