package com.kolodkingal.enhanced.redhulk;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.BlockRights;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.RedHulkConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.hulk.HulkAbilities;
import com.kolodkingal.enhanced.network.RedHulkFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Anger Burst (3.8, PLAN 4.3): at 100 anger by itself, or with B from {@code earlyBurstMin} (then scaled by
 * anger/100). A ring of fire and heat runs out from his body over {@link #TICKS} ticks to {@code burstRadius}: {@code
 * burstDamage} ({@code red_hulk_burst}, a fire damage) to every creature he may hurt, falling off to half at the edge,
 * each set burning {@code burstFireSeconds} and pushed out {@code burstPush}. The ring's edge lights the ground (fire
 * on the air over a solid or flammable block, one spot in three) <b>only</b> when the game rule {@code mobGriefing}
 * and {@code [red_hulk] burstSetsFire} are both on, and never where a hand could not build ({@code
 * BlockRights.mayPlace}: a boss's arena, Asgard's city, a Hex, spawn protection, claims) nor in the arena of any
 * running fight. His anger is spent at once.
 */
public final class RedHulkBurst {
    /** The ticks the ring takes to reach its edge. */
    public static final int TICKS = 10;
    /** "Hotter when angry", the advancement of his first burst. */
    static final ResourceLocation ADVANCEMENT = Enhanced.id("avengers/hotter_when_angry");

    private final Vec3 center;
    private final long start;
    private final double radius;
    private final float damage;
    private final IntSet hit = new IntOpenHashSet();
    /** The cells set alight (for the tests). */
    private final List<BlockPos> lit = new ArrayList<>();

    private RedHulkBurst(Vec3 center, long start, double radius, float damage) {
        this.center = center;
        this.start = start;
        this.radius = radius;
        this.damage = damage;
    }

    /** B: the burst now, if he is angry enough ({@code earlyBurstMin}); below it, "Not angry enough… yet.". */
    public static boolean early(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.RED_HULK_BURST, now)) {
            return false;
        }
        float anger = Anger.of(player);
        if (anger < RedHulkAbilities.cfg(RedHulkConfig.EARLY_BURST_MIN, 50.0)) {
            player.displayClientMessage(Component.translatable("message.enhanced.red_hulk.not_angry")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        data.startCooldown(HeroAbility.RED_HULK_BURST, now, RedHulkAbilities.cfg(RedHulkConfig.EARLY_BURST_COOLDOWN,
                200));
        start(player, data, anger / Anger.MAX);
        return true;
    }

    /** The burst begins, {@code factor} (0-1) of its full strength; his anger is spent. */
    static RedHulkBurst start(ServerPlayer player, PowerData data, double factor) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        double radius = Math.max(1.0, RedHulkAbilities.cfg(RedHulkConfig.BURST_RADIUS, 6.0) * factor);
        float damage = (float) (RedHulkAbilities.cfg(RedHulkConfig.BURST_DAMAGE, 10.0) * factor);
        RedHulkBurst burst = new RedHulkBurst(player.position(), now, radius, damage);
        RedHulkAbilities.state(player.getUUID()).burst = burst;
        Anger.set(player, Anger.get(player).withAnger(0.0F, now));
        RedHulkAbilities.sound(level, burst.center, ModSounds.RED_HULK_BURST.get(), 2.2F, 1.0F);
        RedHulkAbilities.send(level, player, RedHulkFxPayload.BURST, List.of(burst.center),
                (int) Math.ceil(radius));
        award(player);
        return burst;
    }

    /** One tick of the ring; whether it is over. */
    boolean tick(ServerPlayer player, PowerData data, long now) {
        int t = (int) (now - start);
        if (t < 0) {
            return true;
        }
        double from = radius * t / TICKS;
        double to = radius * (t + 1) / TICKS;
        ring(player, from, to);
        if (t + 1 >= TICKS && setsFire(player.serverLevel())) {
            fire(player, to);
        }
        return t + 1 >= TICKS;
    }

    private void ring(ServerPlayer player, double from, double to) {
        ServerLevel level = player.serverLevel();
        double push = RedHulkAbilities.cfg(RedHulkConfig.BURST_PUSH, 1.2);
        int fireTicks = RedHulkAbilities.cfg(RedHulkConfig.BURST_FIRE_SECONDS, 5) * 20;
        AABB box = new AABB(center, center).inflate(to + 1.0, 3.0, to + 1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player
                && !hit.contains(e.getId()) && RedHulkAbilities.mayHurt(player, e))) {
            double distance = Math.hypot(e.getX() - center.x, e.getZ() - center.z) - e.getBbWidth() * 0.5;
            if (distance > to || Math.abs(e.getY() - center.y) > 3.0) {
                continue;
            }
            hit.add(e.getId());
            float amount = (float) (damage * (1.0 - 0.5 * Math.min(1.0, Math.max(0.0, distance) / radius)));
            if (RedHulkAbilities.hurt(player, e, ModDamageTypes.RED_HULK_BURST, amount, player)) {
                e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), fireTicks));
                HulkAbilities.push(e, center, push, 0.35, player);
            }
        }
    }

    /** Whether the burst may light blocks here: {@code mobGriefing} and {@code burstSetsFire}. */
    static boolean setsFire(ServerLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && RedHulkAbilities.cfg(RedHulkConfig.BURST_SETS_FIRE, true);
    }

    /** The ring's spots at its edge {@code r}: one in three catches fire. */
    private void fire(ServerPlayer player, double r) {
        if (r < 1.5) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int n = (int) Math.ceil(2.0 * Math.PI * r);
        for (int i = 0; i < n; i++) {
            if (i % 3 != 0) {
                continue;
            }
            double a = i * 2.0 * Math.PI / n;
            BlockPos pos = spot(level, BlockPos.containing(center.x + Math.cos(a) * r, center.y + 0.5,
                    center.z + Math.sin(a) * r));
            if (pos != null && light(player, level, pos)) {
                lit.add(pos);
            }
        }
    }

    /** The air cell over the ground near {@code at} (within two up and three down), or null. */
    private static BlockPos spot(ServerLevel level, BlockPos at) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = at.offset(0, dy, 0);
            if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    /** Sets fire at the cell, if fire can be there and a hand of his could build there. */
    static boolean light(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!BaseFireBlock.canBePlacedAt(level, pos, Direction.UP) || BossFights.inAnyArena(level, pos)
                || !BlockRights.mayPlace(player, level, pos)) {
            return false;
        }
        BlockState fire = BaseFireBlock.getState(level, pos);
        return level.setBlock(pos, fire, 11);
    }

    /** The cells this burst has set alight so far. */
    public List<BlockPos> lit() {
        return List.copyOf(lit);
    }

    /** Grants "Hotter when angry" at his first burst. */
    static void award(ServerPlayer player) {
        AdvancementHolder advancement = player.server.getAdvancements().get(ADVANCEMENT);
        if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            for (String criterion : advancement.value().criteria().keySet()) {
                player.getAdvancements().award(advancement, criterion);
            }
        }
    }
}
