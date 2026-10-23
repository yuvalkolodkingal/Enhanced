package com.kolodkingal.enhanced.strange;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.StrangeConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.StrangeFxPayload;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Crimson Bands of Cyttorak (B), 3.6: red bands fly to the creature he looks at and bind it. A bound creature is
 * {@code SPELLBOUND} (Billy's spell: it cannot move, strike or use powers) for {@code bandsTicks}, a boss only for {@code
 * bandsBossTicks}; the few too great for the spell (the Wither, the Mad Titan...) are held still by the bands themselves
 * for that second ({@link #HELD}). The binding costs it {@link #DAMAGE}.
 */
public final class CrimsonBands {
    /** The harm of the bands closing. */
    public static final float DAMAGE = 2.0F;

    /** A creature the bands themselves hold (one the spell cannot): where it was bound and until when. */
    private record Hold(Vec3 at, long until) {
    }

    /** Entity UUID -> the bands holding it. */
    private static final Map<UUID, Hold> HELD = new HashMap<>();

    public static int range() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.BANDS_RANGE.get(), 20);
    }

    public static int ticks() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.BANDS_TICKS.get(), 80);
    }

    public static int bossTicks() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.BANDS_BOSS_TICKS.get(), 20);
    }

    public static int cooldown() {
        return StrangeAbilities.cfgInt(() -> StrangeConfig.BANDS_COOLDOWN.get(), 200);
    }

    /** Whether the bands bind {@code e} only for a boss's second. */
    public static boolean isBoss(Entity e) {
        return e.getType().is(ModTags.BOSSES);
    }

    /** The creature the bands fly to: the one he looks at, within {@link #range}, that he may hurt; or null. */
    @Nullable
    public static LivingEntity target(ServerPlayer player) {
        EntityHitResult hit = Raycasts.entity(player, range(), 0.4, e -> e instanceof LivingEntity living
                && living.isAlive() && !living.isSpectator() && AreaTargets.mayHurt(player, living, false), 1.0F);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /** B: the bands fly to the creature he looks at. Nobody there: told, and nothing spent. */
    static void cast(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.STRANGE_BANDS, now) || StrangeAbilities.refuseHungry(player)) {
            return;
        }
        LivingEntity target = target(player);
        if (target == null) {
            StrangeAbilities.say(player, "message.enhanced.strange.no_target");
            return;
        }
        bind(player, target);
        data.startCooldown(HeroAbility.STRANGE_BANDS, now, cooldown());
        StrangeAbilities.spend(player, data, 1.0F);
        StrangeAbilities.sound(player, ModSounds.STRANGE_BANDS.get(), 1.0F, 1.0F);
        StrangeSignals.near(level, target.position(), StrangeFxPayload.BANDS, player,
                List.of(target.position(), new Vec3(boss(target) ? bossTicks() : ticks(), 0, 0)), target.getId());
    }

    private static boolean boss(LivingEntity target) {
        return isBoss(target);
    }

    /** The bands close on {@code target}: spellbound (a boss for a second), held still if the spell cannot hold it. */
    public static void bind(ServerPlayer caster, LivingEntity target) {
        ServerLevel level = caster.serverLevel();
        int ticks = isBoss(target) ? bossTicks() : ticks();
        target.hurt(ModDamageTypes.source(level, ModDamageTypes.CRIMSON_BANDS, caster, caster), DAMAGE);
        if (!target.isAlive()) {
            return;
        }
        boolean took = target.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, ticks, 0, false, false, true),
                caster);
        if (!took || target.getType().is(ModTags.SPELLBOUND_IMMUNE)) {
            HELD.put(target.getUUID(), new Hold(target.position(), level.getGameTime() + ticks));
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    /** The ticks the bands themselves still hold {@code e} (one the spell cannot), or 0. */
    public static long heldTicks(Entity e) {
        Hold hold = HELD.get(e.getUUID());
        return hold == null ? 0 : Math.max(0, hold.until() - e.level().getGameTime());
    }

    /** Every server tick: what the bands themselves hold stays where it was bound. */
    public static void serverTick() {
        if (HELD.isEmpty()) {
            return;
        }
        var server = Platform.get().currentServer();
        if (server == null) {
            HELD.clear();
            return;
        }
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Hold>> it = HELD.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Hold> e = it.next();
            if (now >= e.getValue().until()) {
                it.remove();
                continue;
            }
            for (ServerLevel level : server.getAllLevels()) {
                Entity held = level.getEntity(e.getKey());
                if (held != null) {
                    if (!held.isAlive()) {
                        it.remove();
                    } else {
                        Vec3 at = e.getValue().at();
                        held.setDeltaMovement(Vec3.ZERO);
                        if (held.position().distanceToSqr(at) > 0.04) {
                            held.teleportTo(at.x, at.y, at.z);
                        }
                        held.hurtMarked = true;
                    }
                    break;
                }
            }
        }
    }

    static void clear() {
        HELD.clear();
    }

    private CrimsonBands() {
    }
}
