package com.kolodkingal.enhanced.heimdall;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.npc.Asgardians;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.config.HeimdallConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.item.HofundItem;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Heimdall, the Guardian of the Bifrost (3.5): a player who held his own Hofund after winning the Guardian's Watch
 * ({@link GuardiansWatch}). He walks (the Bifrost carries him) and fights with Hofund:
 * <ul>
 * <li><b>R</b> the Hofund Slash: a wide golden arc before him, a combo of three blows (the third knocks back); Sneak + R the
 * Hofund Lunge, a dash and a thrust. Hofund must be in his hand (a custom hero's: any sword); with none of his own left
 * anywhere, R calls a new one into his hand ({@link HofundBond}).</li>
 * <li><b>G</b> the All-Seeing Sight (a switch, {@link HeimdallSight}); Sneak + G held, Far Sight (the client's zoom).</li>
 * <li><b>V</b> the Bifrost with no item (the client opens the screen); Sneak + V to where he looks.</li>
 * <li><b>B</b> held, the Guardian's Ward: Hofund planted, a golden dome round him that stops projectiles and keeps monsters
 * out; he stands fast and takes less harm. Sneak + B the Gatekeeper's Call: Einherjar come to his side.</li>
 * <li><b>N</b> the Bifrost Strike: a column of rainbow light where he looks; Sneak + N, the Bifrost for a friend (the player
 * he looks at, whom he sends from the Bifrost screen's Send mode, {@link Bifrost#send}).</li>
 * </ul>
 * Every ability is a {@link HeroAbility} row (3.4): a custom hero who picks one has it on his own key, at his strength
 * (the cooldowns by {@link PowerData#startCooldown(HeroAbility, long, int)}, the damage by {@code HeroDamage}, the body by
 * {@link PowerData#bodyScale}). The plug-in is {@link HeimdallServer}; his events (the Ward's and the body's damage, the
 * bonus of Hofund) are {@link HeimdallEvents}.
 */
public final class HeimdallAbilities {
    // ---- his colours: the gold of Asgard and the Bifrost's ----
    public static final int GOLD = 0xE8B54A;
    public static final int LIGHT = 0xFFE7A3;

    /** The arc of a blow: its reach (blocks) and its width (degrees, centred on the look). */
    public static final double SLASH_RADIUS = 3.5;
    public static final double SLASH_ANGLE = 100.0;
    /** Ticks between two blows, and how long after a blow the next one still continues the combo. */
    public static final int SLASH_COOLDOWN = 12;
    public static final int COMBO_RESET = 30;
    /** How long {@link Live#HEIMDALL_SLASHING} stays up for each blow (every client starts the blow's gesture on it). */
    public static final int SLASH_PULSE = 6;
    /** The third blow's knockback. */
    private static final double THIRD_KNOCKBACK = 1.1;

    /** The lunge: its length (blocks) and how wide its thrust reaches either side. */
    public static final double LUNGE_DISTANCE = 6.0;
    private static final double LUNGE_WIDTH = 0.6;
    /** Hofund comes back to an empty hand at most this often (ticks). */
    public static final int RETURN_COOLDOWN = 200;

    /** The Ward's dome (blocks), how much less harm he takes under it, and how hard it pushes a monster out. */
    public static final double WARD_RADIUS = 5.0;
    public static final float WARD_REDUCTION = 0.4F;
    private static final double WARD_PUSH = 0.45;

    /** How far the Bifrost Strike reaches, and its radius. */
    public static final double STRIKE_RANGE = 48.0;
    public static final double STRIKE_RADIUS = 3.0;
    private static final double STRIKE_LIFT = 0.8;

    /** Einherjar of the Gatekeeper's Call: three, four in Asgard. */
    public static final int CALL_COUNT = 3;
    public static final int CALL_COUNT_ASGARD = 4;

    private static final ResourceLocation BODY_HEALTH = Enhanced.id("heimdall_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("heimdall_armor");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("heimdall_knockback");
    private static final ResourceLocation WARD_STILL = Enhanced.id("heimdall_ward_still");
    private static final ResourceLocation WARD_STEADY = Enhanced.id("heimdall_ward_steady");
    private static final ResourceLocation WARD_JUMP = Enhanced.id("heimdall_ward_jump");
    /** His body: health, armour, knockback resistance (the fall is {@link HeimdallEvents}'). */
    public static final double BODY_HEALTH_BONUS = 8.0;
    public static final double BODY_ARMOR_BONUS = 4.0;
    public static final double BODY_KNOCKBACK_BONUS = 0.4;
    /** Falls shorter than this (blocks) do him no harm. */
    public static final float SAFE_FALL = 8.0F;

    /** A combo in progress: the blow last struck (1-3) and when. */
    record Combo(int step, long at) {
    }

    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    /** Heimdall -> the game time {@link Live#HEIMDALL_SLASHING} comes down. */
    private static final Map<UUID, Long> SLASHING_UNTIL = new HashMap<>();
    /** Heimdall -> when the Ward was planted. */
    private static final Map<UUID, Long> WARD_SINCE = new HashMap<>();
    /** Heimdall -> when Hofund may next return to his hand. */
    private static final Map<UUID, Long> RETURN_READY = new HashMap<>();

    /**
     * Who answers the Gatekeeper's Call: the Asgardians' lane ({@link Asgardians#summonEinherjar}). A test may put in
     * another, and put this one back when it ends.
     */
    public static Summoner summoner = Asgardians::summonEinherjar;

    /** Who answers the Gatekeeper's Call. */
    @FunctionalInterface
    public interface Summoner {
        List<Entity> summon(ServerPlayer owner, int count, int ticks);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Queries

    /** Whether this player has this ability of Heimdall's: Heimdall, or a custom hero who picked it. */
    public static boolean has(@Nullable Entity e, HeroAbility ability) {
        return e instanceof Player player && Powers.get(player).has(ability);
    }

    /** Whether the Guardian's Ward stands round this player now. */
    public static boolean warded(@Nullable Entity e) {
        return e instanceof Player player && Powers.get(player).is(Live.HEIMDALL_WARD) && has(player, HeroAbility.HEIMDALL_WARD);
    }

    /** The blow his combo is on (the last one struck, 1-3), or 0 when the combo has lapsed. */
    public static int comboStep(Player player, long now) {
        Combo combo = COMBOS.get(player.getUUID());
        return combo == null || now - combo.at() > COMBO_RESET ? 0 : combo.step();
    }

    /** The blow that follows {@code last} (struck at {@code lastAt}) at {@code now}: the next of three, or the first again. */
    public static int nextStep(int last, long lastAt, long now) {
        return last <= 0 || last >= 3 || now - lastAt > COMBO_RESET ? 1 : last + 1;
    }

    /** The ticks the Ward has stood, or -1. */
    public static long wardAge(Player player, long now) {
        Long since = WARD_SINCE.get(player.getUUID());
        return since == null ? -1 : now - since;
    }

    /**
     * Whether the blade in his hand serves his Slash: Hofund for Heimdall himself, any sword for a custom hero (who has no
     * Hofund of his own; Hofund still serves him best).
     */
    public static boolean bladeServes(Player player, PowerData data) {
        ItemStack held = player.getMainHandItem();
        if (HofundItem.isHofund(held)) {
            return true;
        }
        return data.power() != Power.HEIMDALL && (held.getItem() instanceof SwordItem || held.is(ItemTags.SWORDS));
    }

    /** Whether a point lies in the arc of a blow from {@code eye} along {@code look} (flat). Pure. */
    public static boolean inArc(Vec3 from, Vec3 look, Vec3 point, double radius, double angle) {
        double dx = point.x - from.x;
        double dz = point.z - from.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > radius) {
            return false;
        }
        if (dist < 0.6) {
            return true;
        }
        double lx = look.x;
        double lz = look.z;
        double ll = Math.sqrt(lx * lx + lz * lz);
        if (ll < 1.0E-6) {
            return true;
        }
        double cos = (dx * lx + dz * lz) / (dist * ll);
        return cos >= Math.cos(Math.toRadians(angle * 0.5));
    }

    private static double cfg(Function<Void, Double> read, double fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? read.apply(null) : fallback;
    }

    private static int cfgInt(Function<Void, Integer> read, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? read.apply(null) : fallback;
    }

    /** {@code heimdall.hofundBonus}: how much more Hofund deals in his hands. */
    public static double hofundBonus() {
        return cfg(v -> HeimdallConfig.HOFUND_BONUS.get(), 0.3);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Input

    /** A key of his (3.4: also one of a custom hero's bindings, with the key the ability has for him). {@code alt}: Sneak's variant. */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down) {
            if (ability == 2 && data.is(Live.HEIMDALL_FARSIGHT)) {
                farSight(player, data, false);
            } else if (ability == 4 && data.is(Live.HEIMDALL_WARD)) {
                dropWard(player, data);
            }
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        switch (ability) {
            case 1 -> {
                if (alt) {
                    lunge(player, data);
                } else {
                    slash(player, data);
                }
            }
            case 2 -> {
                if (alt) {
                    farSight(player, data, true);
                } else {
                    toggleSight(player, data);
                }
            }
            case 3 -> {
                // V alone opens the Bifrost screen on the client; Sneak + V goes to where he looks.
                if (alt) {
                    Bifrost.toLook(player);
                }
            }
            case 4 -> {
                if (alt) {
                    call(player, data);
                } else {
                    plantWard(player, data);
                }
            }
            case 5 -> {
                if (alt) {
                    chooseFriend(player, data);
                } else {
                    strike(player, data);
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // R: Hofund

    /** The Hofund Slash: the next blow of the combo, a golden arc before him. */
    static void slash(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HEIMDALL_HOFUND, now)) {
            return;
        }
        if (!bladeServes(player, data)) {
            noBlade(player, data, now);
            return;
        }
        Combo last = COMBOS.get(player.getUUID());
        int step = last == null ? 1 : nextStep(last.step(), last.at(), now);
        COMBOS.put(player.getUUID(), new Combo(step, now));
        double base = switch (step) {
            case 1 -> cfg(v -> HeimdallConfig.SLASH_DAMAGE1.get(), 9.0);
            case 2 -> cfg(v -> HeimdallConfig.SLASH_DAMAGE2.get(), 11.0);
            default -> cfg(v -> HeimdallConfig.SLASH_DAMAGE3.get(), 14.0);
        };
        float damage = (float) (base * (HofundItem.isHofund(player.getMainHandItem()) ? 1.0 + hofundBonus() : 1.0));
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position();
        Vec3 look = player.getLookAngle();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SLASH_RADIUS, 1.5, SLASH_RADIUS),
                e -> e != player && e.isAlive() && inArc(from, look, e.position(), SLASH_RADIUS + e.getBbWidth() * 0.5, SLASH_ANGLE)
                        && AreaTargets.mayHurt(player, e, false))) {
            if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.HOFUND, player, player), damage) && step == 3) {
                Vec3 push = target.position().subtract(from).multiply(1, 0, 1);
                push = push.lengthSqr() < 1.0E-6 ? look.multiply(1, 0, 1) : push;
                target.knockback(THIRD_KNOCKBACK, -push.x, -push.z);
                target.hurtMarked = true;
            }
        }
        data.startCooldown(HeroAbility.HEIMDALL_HOFUND, now, step == 3 ? SLASH_COOLDOWN + 4 : SLASH_COOLDOWN);
        data.set(Live.HEIMDALL_SLASHING, true);
        SLASHING_UNTIL.put(player.getUUID(), now + SLASH_PULSE);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEIMDALL_SLASH.get(), SoundSource.PLAYERS, 1.0F,
                0.9F + 0.1F * step);
        HeimdallFx.near(level, from, HeimdallFx.SLASH, player, List.of(from.add(0, player.getBbHeight() * 0.55, 0), look), step);
    }

    /**
     * No blade to strike with: Heimdall with no Hofund of his own anywhere calls a new one into his hand (it returns); with
     * one in his pack, he is told to take it up.
     */
    private static void noBlade(ServerPlayer player, PowerData data, long now) {
        if (data.power() == Power.HEIMDALL && !HofundBond.hasOwn(player)) {
            if (now >= RETURN_READY.getOrDefault(player.getUUID(), 0L)) {
                returnHofund(player, now);
            }
            return;
        }
        player.displayClientMessage(Component.translatable(data.power() == Power.HEIMDALL ? "message.enhanced.heimdall.no_hofund"
                : "message.enhanced.heimdall.no_sword"), true);
    }

    /** Hofund returns to his hand: a new one bound to him (the old copies will crumble), in a golden flash. */
    static void returnHofund(ServerPlayer player, long now) {
        RETURN_READY.put(player.getUUID(), now + RETURN_COOLDOWN);
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && !player.getInventory().add(held.copy())) {
            player.drop(held.copy(), false);
        }
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        HofundBond.give(player);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HOFUND_RETURN.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        HeimdallFx.near(level, player.position(), HeimdallFx.RETURN, player, List.of(player.position()), 0);
        player.displayClientMessage(Component.translatable("message.enhanced.heimdall.hofund_returns"), true);
    }

    /**
     * Where the lunge ends from {@code start} along {@code dir} (flat, unit length): {@link #LUNGE_DISTANCE} blocks, or short
     * of the first wall (the player's box must fit all the way).
     */
    public static Vec3 lungeEnd(ServerLevel level, Player player, Vec3 start, Vec3 dir) {
        AABB box = player.getBoundingBox().move(start.subtract(player.position()));
        Vec3 at = start;
        for (double d = 0.25; d <= LUNGE_DISTANCE + 1.0E-6; d += 0.25) {
            Vec3 next = start.add(dir.scale(d));
            if (!level.noCollision(player, box.move(next.subtract(start)))) {
                break;
            }
            at = next;
        }
        return at;
    }

    /** The Hofund Lunge: a dash along the look, the first creature on the way thrust through. */
    static void lunge(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HEIMDALL_HOFUND, now)) {
            return;
        }
        if (!bladeServes(player, data)) {
            noBlade(player, data, now);
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, player.getYRot()) : dir.normalize();
        Vec3 start = player.position();
        Vec3 end = lungeEnd(level, player, start, dir);
        // The first creature on the way takes the thrust, and the dash stops at it.
        Vec3 mid = start.add(0, player.getBbHeight() * 0.5, 0);
        EntityHitResult hit = Raycasts.entityBetween(level, player, mid, end.add(0, player.getBbHeight() * 0.5, 0), LUNGE_WIDTH,
                e -> e instanceof LivingEntity living && living.isAlive() && AreaTargets.mayHurt(player, living, false));
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            double reach = Math.max(0.0, start.distanceTo(new Vec3(target.getX(), start.y, target.getZ())) - 1.2);
            end = start.add(dir.scale(Math.min(reach, start.distanceTo(end))));
            float damage = (float) (cfg(v -> HeimdallConfig.LUNGE_DAMAGE.get(), 12.0)
                    * (HofundItem.isHofund(player.getMainHandItem()) ? 1.0 + hofundBonus() : 1.0));
            target.hurt(ModDamageTypes.source(level, ModDamageTypes.HOFUND, player, player), damage);
            target.knockback(0.6, -dir.x, -dir.z);
        }
        // The client moves the player: an impulse that glides him (ground friction) to the end of the dash.
        double distance = start.distanceTo(end);
        player.setDeltaMovement(dir.scale(distance / 2.2).add(0, 0.12, 0));
        player.hurtMarked = true;
        player.resetFallDistance();
        data.startCooldown(HeroAbility.HEIMDALL_HOFUND, now, cfgInt(v -> HeimdallConfig.LUNGE_COOLDOWN.get(), 80));
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEIMDALL_LUNGE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        HeimdallFx.near(level, start, HeimdallFx.LUNGE, player, List.of(start, end), 0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // G: the Sight

    static void toggleSight(ServerPlayer player, PowerData data) {
        boolean on = !data.toggle(Toggle.HEIMDALL_SIGHT);
        data.setToggle(Toggle.HEIMDALL_SIGHT, on);
        sound(player, on ? ModSounds.HEIMDALL_SIGHT_ON.get() : ModSounds.HEIMDALL_SIGHT_OFF.get(), 1.0F);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.heimdall.sight_on" : "message.enhanced.heimdall.sight_off"), true);
        if (on) {
            HeimdallSight.send(player);
        } else {
            HeimdallSight.stop(player);
        }
    }

    static void farSight(ServerPlayer player, PowerData data, boolean on) {
        if (data.is(Live.HEIMDALL_FARSIGHT) == on) {
            return;
        }
        data.set(Live.HEIMDALL_FARSIGHT, on);
        if (on) {
            sound(player, ModSounds.HEIMDALL_FARSIGHT.get(), 0.8F);
            HeimdallSight.send(player);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // B: the Ward and the Call

    static void plantWard(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (data.is(Live.HEIMDALL_WARD) || !data.isReady(HeroAbility.HEIMDALL_WARD, now)) {
            return;
        }
        data.set(Live.HEIMDALL_WARD, true);
        WARD_SINCE.put(player.getUUID(), now);
        wardBody(player, true);
        player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
        player.hurtMarked = true;
        sound(player, ModSounds.HEIMDALL_WARD_UP.get(), 1.0F);
    }

    static void dropWard(ServerPlayer player, PowerData data) {
        boolean was = data.is(Live.HEIMDALL_WARD);
        data.set(Live.HEIMDALL_WARD, false);
        WARD_SINCE.remove(player.getUUID());
        wardBody(player, false);
        if (was) {
            data.startCooldown(HeroAbility.HEIMDALL_WARD, player.level().getGameTime(), cfgInt(v -> HeimdallConfig.WARD_COOLDOWN.get(), 160));
            sound(player, ModSounds.HEIMDALL_WARD_DOWN.get(), 1.0F);
        }
    }

    /** Under the Ward he stands fast: he cannot walk or jump, and nothing knocks him back. */
    private static void wardBody(ServerPlayer player, boolean on) {
        setModifier(player, Attributes.MOVEMENT_SPEED, WARD_STILL, -1.0, on, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.JUMP_STRENGTH, WARD_JUMP, -1.0, on, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, WARD_STEADY, 1.0, on, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Every tick the Ward stands: its time, the projectiles it stops, the monsters it keeps out. */
    private static void wardTick(ServerPlayer player, PowerData data, long now) {
        long age = wardAge(player, now);
        if (age < 0) {
            WARD_SINCE.put(player.getUUID(), now);
            age = 0;
        }
        if (age >= cfgInt(v -> HeimdallConfig.WARD_SECONDS.get(), 10) * 20L) {
            dropWard(player, data);
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(WARD_RADIUS + 4.0),
                p -> p.getOwner() != player && !ApocalypseGuard.headingHome(p))) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 toPlayer = center.subtract(projectile.position());
            double reach = WARD_RADIUS + 0.5 + velocity.length();
            if (velocity.dot(toPlayer) > 0 && toPlayer.lengthSqr() < reach * reach) {
                stopProjectile(level, projectile);
            }
        }
        pushOut(player, level, center);
    }

    /** A projectile stopped at the Ward's edge: turned to golden sparks (a thrown weapon turns home). */
    public static void stopProjectile(ServerLevel level, Projectile projectile) {
        WandaAbilities.neutralize(level, projectile, GOLD);
    }

    /** Hostile creatures inside the dome are pushed out of it. */
    static void pushOut(ServerPlayer player, ServerLevel level, Vec3 center) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(WARD_RADIUS),
                e -> e instanceof Enemy && e.isAlive() && e.position().distanceToSqr(player.position()) < WARD_RADIUS * WARD_RADIUS)) {
            Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.setDeltaMovement(away.scale(WARD_PUSH).add(0, 0.15, 0));
            e.hurtMarked = true;
            if (e.distanceToSqr(player) < 2.0 * 2.0) {
                Vec3 out = player.position().add(away.scale(2.2));
                e.teleportTo(out.x, e.getY(), out.z);
            }
        }
    }

    /** How many Einherjar answer the Call here: four in Asgard, three elsewhere. */
    public static int callCount(Player player) {
        return AsgardDimension.in(player) ? CALL_COUNT_ASGARD : CALL_COUNT;
    }

    /** The Gatekeeper's Call: Einherjar come to his side in a flash of gold. */
    static List<Entity> call(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HEIMDALL_WARD, now)) {
            return List.of();
        }
        data.startCooldown(HeroAbility.HEIMDALL_WARD, now, cfgInt(v -> HeimdallConfig.CALL_COOLDOWN.get(), 900));
        int count = callCount(player);
        List<Entity> came = summoner.summon(player, count, cfgInt(v -> HeimdallConfig.CALL_SECONDS.get(), 40) * 20);
        List<Vec3> where = new ArrayList<>();
        for (Entity e : came) {
            where.add(e.position());
        }
        if (where.isEmpty()) {
            for (int i = 0; i < count; i++) {
                double a = Math.PI * 2 * i / count + Math.toRadians(player.getYRot());
                where.add(player.position().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5));
            }
        }
        sound(player, ModSounds.HEIMDALL_CALL.get(), 1.2F);
        HeimdallFx.near(player.serverLevel(), player.position(), HeimdallFx.CALL, player, where, count);
        return came;
    }

    // ------------------------------------------------------------------------------------------------------------
    // N: the Bifrost Strike and the Bifrost for a friend

    /** Where the Bifrost Strike falls: the block he looks at (up to {@link #STRIKE_RANGE}), or null. */
    @Nullable
    public static Vec3 strikePoint(Player player) {
        BlockHitResult hit = Raycasts.block(player, STRIKE_RANGE, 1.0F);
        return hit.getType() == HitResult.Type.MISS ? null : Vec3.atBottomCenterOf(hit.getBlockPos().above());
    }

    static void strike(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HEIMDALL_STRIKE, now)) {
            return;
        }
        Vec3 at = strikePoint(player);
        if (at == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.bifrost.no_ground"), true);
            return;
        }
        strikeAt(player, at);
        data.startCooldown(HeroAbility.HEIMDALL_STRIKE, now, cfgInt(v -> HeimdallConfig.STRIKE_COOLDOWN.get(), 140));
    }

    /** The column of rainbow light comes down at {@code at}: harm in its radius, everything thrown up. Sets nothing alight. */
    public static void strikeAt(ServerPlayer player, Vec3 at) {
        ServerLevel level = player.serverLevel();
        float damage = (float) cfg(v -> HeimdallConfig.STRIKE_DAMAGE.get(), 14.0);
        AABB box = new AABB(at, at).inflate(STRIKE_RADIUS, 3.0, STRIKE_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e.position().distanceToSqr(at) <= (STRIKE_RADIUS + 0.5) * (STRIKE_RADIUS + 0.5)
                        && AreaTargets.mayHurt(player, e, false))) {
            if (target.hurt(ModDamageTypes.source(level, ModDamageTypes.BIFROST_STRIKE, player, player), damage)) {
                target.setDeltaMovement(target.getDeltaMovement().add(0, STRIKE_LIFT, 0));
                target.hurtMarked = true;
            }
        }
        level.playSound(null, at.x, at.y, at.z, ModSounds.BIFROST_STRIKE.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        HeimdallFx.near(level, at, HeimdallFx.STRIKE, player, List.of(at), (int) Math.round(STRIKE_RADIUS * 10));
        Bifrost.mark(level, at);
    }

    /** The player Heimdall looks at, to be sent: within {@code heimdall.sendRange}, or null. */
    @Nullable
    public static ServerPlayer friendInView(ServerPlayer player) {
        double range = cfgInt(v -> HeimdallConfig.SEND_RANGE.get(), 24);
        EntityHitResult hit = Raycasts.entity(player, range, 0.3, e -> e instanceof ServerPlayer p && p != player && !p.isSpectator(), 1.0F);
        return hit != null && hit.getEntity() instanceof ServerPlayer friend ? friend : null;
    }

    /** Sneak + N: the friend he looks at is chosen, and his Bifrost screen opens in Send mode for him. */
    static void chooseFriend(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.HEIMDALL_STRIKE, now)) {
            return;
        }
        ServerPlayer friend = friendInView(player);
        if (friend == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.heimdall.no_friend"), true);
            return;
        }
        HeimdallFx.to(player, HeimdallFx.OPEN_SEND, friend.getId(), List.of(friend.position()), 0);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Ticks, the body, the end

    /** His tick, or (3.4) a custom hero's with any of his abilities: each part only for the ability it belongs to. */
    static void tick(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (data.is(Live.HEIMDALL_SLASHING) && now >= SLASHING_UNTIL.getOrDefault(player.getUUID(), 0L)) {
            data.set(Live.HEIMDALL_SLASHING, false);
        }
        if (data.has(HeroAbility.HEIMDALL_WARD) && data.is(Live.HEIMDALL_WARD)) {
            wardTick(player, data, now);
        } else if (WARD_SINCE.containsKey(player.getUUID()) || data.is(Live.HEIMDALL_WARD)) {
            dropWard(player, data);
        } else if (player.tickCount % 20 == 0) {
            wardBody(player, false);
        }
        boolean seeing = data.has(HeroAbility.HEIMDALL_SIGHT) && (data.toggle(Toggle.HEIMDALL_SIGHT) || data.is(Live.HEIMDALL_FARSIGHT));
        if (!data.has(HeroAbility.HEIMDALL_SIGHT) && data.is(Live.HEIMDALL_FARSIGHT)) {
            data.set(Live.HEIMDALL_FARSIGHT, false);
        }
        if (seeing) {
            nightVision(player);
            if (player.tickCount % HeimdallSight.EVERY == 0) {
                HeimdallSight.send(player);
            }
        }
    }

    /** Night Vision while the Sight is open (ambient, so it is known for his and is taken away with it). */
    static void nightVision(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect == null || effect.isAmbient() && effect.getDuration() < 260) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
        }
    }

    static void clearNightVision(ServerPlayer player) {
        MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
        if (effect != null && effect.isAmbient()) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    /**
     * His body (every player, whenever the passives are applied): health, armour and footing while he has it, at his
     * strength for a custom hero; none of it otherwise.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean body = data.has(HeroAbility.HEIMDALL_BODY);
        double scale = body ? data.bodyScale(HeroAbility.HEIMDALL_BODY) : 0.0;
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, BODY_HEALTH_BONUS * scale, body, AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.ARMOR, BODY_ARMOR, BODY_ARMOR_BONUS * scale, body, AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK, BODY_KNOCKBACK_BONUS * scale, body,
                AttributeModifier.Operation.ADD_VALUE);
        if (!body && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** He loses the power (or a custom hero with his abilities redefines, dies or leaves): everything of his ends. */
    static void endAll(ServerPlayer player, PowerData data) {
        if (data.is(Live.HEIMDALL_WARD) || WARD_SINCE.containsKey(player.getUUID())) {
            dropWard(player, data);
        }
        wardBody(player, false);
        data.set(Live.HEIMDALL_FARSIGHT, false);
        data.set(Live.HEIMDALL_SLASHING, false);
        if (data.toggle(Toggle.HEIMDALL_SIGHT)) {
            data.setToggle(Toggle.HEIMDALL_SIGHT, false);
        }
        if (!GuardiansWatch.watching(player)) {
            clearNightVision(player);
            HeimdallSight.stop(player);
        }
        forget(player.getUUID());
    }

    /** Gone from the world for a moment (death, another dimension, logout): the Ward and the held keys end. */
    static void onGone(ServerPlayer player) {
        PowerData data = Powers.get(player);
        if (data.is(Live.HEIMDALL_WARD) || WARD_SINCE.containsKey(player.getUUID())) {
            dropWard(player, data);
        }
        data.set(Live.HEIMDALL_FARSIGHT, false);
        data.set(Live.HEIMDALL_SLASHING, false);
        COMBOS.remove(player.getUUID());
        SLASHING_UNTIL.remove(player.getUUID());
    }

    /** The held keys are let go of (death, the power lost, a screen opened). */
    static void stopHold(PowerData data) {
        data.set(Live.HEIMDALL_FARSIGHT, false);
    }

    static void forget(UUID id) {
        COMBOS.remove(id);
        SLASHING_UNTIL.remove(id);
        WARD_SINCE.remove(id);
        RETURN_READY.remove(id);
    }

    static void clear() {
        COMBOS.clear();
        SLASHING_UNTIL.clear();
        WARD_SINCE.clear();
        RETURN_READY.clear();
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float volume) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, 1.0F);
    }

    /** Puts an attribute modifier on (with this amount) or takes it off. */
    static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present,
                            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount || existing.operation() != operation) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    private HeimdallAbilities() {
    }
}
