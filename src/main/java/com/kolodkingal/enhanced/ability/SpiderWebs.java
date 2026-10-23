package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossHooks;
import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.entity.LokiIllusionEntity;
import com.kolodkingal.enhanced.platform.Platform;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Spider-Man's webs (DESIGN section 14): who a web may touch, the Webbed effect, and the temporary web blocks (only into
 * free air, protection-checked, capped, and removing themselves). The shot, the burst and the zip use it; the aiming, the
 * hunger and the effects on the hero's side are {@link SpiderServer}'s.
 */
public final class SpiderWebs {
    /** A web block a Spider-Man made: where, so that the oldest can go when he has too many. */
    public record WebRef(ResourceKey<Level> dimension, BlockPos pos) {
    }

    // ---- who ----

    /** What a web, a strand or a Web Shot can hit: a creature (or a part of one, so the Ender Dragon's body counts), alive. */
    public static boolean webTarget(Entity entity) {
        Entity body = body(entity);
        return body instanceof LivingEntity && body.isAlive() && !body.isSpectator() && !(body instanceof ArmorStand);
    }

    /** The creature a part of a creature belongs to (the Ender Dragon's parts), or the entity itself. */
    public static Entity body(Entity entity) {
        return Platform.get().parts().root(entity);
    }

    /**
     * Whether {@code target} can be webbed by {@code spider}: not himself, not another Spider-Man, not a boss (the
     * {@code enhanced:web_immune} tag), not a Creative or Spectator player, and a player only if PvP is on and he may
     * harm them.
     */
    public static boolean canWeb(ServerPlayer spider, Entity target) {
        Entity body = body(target);
        if (body == spider || !(body instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()
                || living.getType().is(ModTags.WEB_IMMUNE)) {
            return false;
        }
        if (living instanceof Player player) {
            return !player.isCreative() && !player.isSpectator() && !SpiderServer.isSpiderMan(player) && spider.canHarmPlayer(player);
        }
        return true;
    }

    /**
     * A web reaches {@code target}: it is wrapped in webs (the Webbed effect) for {@code ticks}, and a mob stops walking.
     * A boss (a {@code web_immune} creature) is not webbed, but if it implements {@link BossHooks} it is told
     * ({@code onWebShot}: Thanos locks his Gauntlet).
     *
     * @return whether the creature is webbed now
     */
    public static boolean web(ServerPlayer spider, Entity target, int ticks) {
        Entity body = body(target);
        if (body instanceof LokiIllusionEntity illusion) {
            // One touch is enough for a duplicate of Loki's: it dissolves into light at the web.
            illusion.dissolve(illusion.position().add(0, illusion.getBbHeight() * 0.5, 0));
            return false;
        }
        if (body.getType().is(ModTags.WEB_IMMUNE)) {
            if (body instanceof BossHooks boss && body.isAlive()) {
                boss.onWebShot(spider);
            }
            return false;
        }
        if (!canWeb(spider, body)) {
            return false;
        }
        LivingEntity living = (LivingEntity) body;
        living.addEffect(new MobEffectInstance(ModEffects.WEBBED, ticks, 0, false, true, true), spider);
        if (living instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        return living.hasEffect(ModEffects.WEBBED);
    }

    // ---- web blocks ----

    /** How long a web block made now lasts. */
    public static int lifetime(boolean organic) {
        int ticks = SpiderConfig.WEB_BLOCK_TICKS.get();
        return organic ? (int) Math.round(ticks * SpiderConfig.ORGANIC_WEB_FACTOR.get()) : ticks;
    }

    /**
     * Whether a web block may go into {@code pos}: free air only (never over a block, a fluid, a plant, a torch or a
     * cobweb), inside the world and its border, where he may build (spawn protection and claim mods, Adventure mode) and
     * outside a protected boss arena.
     */
    public static boolean canPlace(ServerLevel level, ServerPlayer spider, BlockPos pos) {
        return SpiderConfig.WEB_BLOCKS.get() && level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.isEmptyBlock(pos)
                && level.getWorldBorder().isWithinBounds(pos) && spider.getAbilities().mayBuild && level.mayInteract(spider, pos)
                && !BossFights.protects(level, pos);
    }

    /**
     * Puts a temporary web block into {@code pos} if it may go there, and keeps count: past the cap the oldest of his
     * webs that is still standing is removed.
     *
     * @return whether a web was placed
     */
    public static boolean place(ServerLevel level, ServerPlayer spider, ArrayDeque<WebRef> webs, BlockPos pos, boolean organic) {
        if (!canPlace(level, spider, pos)) {
            return false;
        }
        BlockState web = ModBlocks.SPIDER_WEB.get().defaultBlockState();
        // The tick is scheduled before the block goes in, so that organic webs keep their longer life: the block asks for
        // the default only when nothing is scheduled.
        level.scheduleTick(pos, web.getBlock(), lifetime(organic));
        if (!level.setBlock(pos, web, 3)) {
            return false;
        }
        webs.addLast(new WebRef(level.dimension(), pos.immutable()));
        trim(level, webs);
        return true;
    }

    /** Forgets the webs that are gone and, while there are more than the cap, removes the oldest that is still standing. */
    private static void trim(ServerLevel level, ArrayDeque<WebRef> webs) {
        for (Iterator<WebRef> it = webs.iterator(); it.hasNext(); ) {
            if (!standing(level, it.next())) {
                it.remove();
            }
        }
        int cap = SpiderConfig.WEB_BLOCK_CAP.get();
        while (webs.size() > cap) {
            WebRef oldest = webs.pollFirst();
            ServerLevel there = level.getServer().getLevel(oldest.dimension());
            if (there != null && standing(there, oldest)) {
                there.removeBlock(oldest.pos(), false);
            }
        }
    }

    private static boolean standing(ServerLevel level, WebRef ref) {
        ServerLevel there = level.dimension().equals(ref.dimension()) ? level : level.getServer().getLevel(ref.dimension());
        return there != null && there.hasChunkAt(ref.pos()) && there.getBlockState(ref.pos()).is(ModBlocks.SPIDER_WEB.get());
    }

    /** How many of these webs are still standing. */
    public static int standing(ServerLevel level, Iterable<WebRef> webs) {
        int count = 0;
        for (WebRef ref : webs) {
            if (standing(level, ref)) {
                count++;
            }
        }
        return count;
    }

    /**
     * The patch a Web Shot leaves on a face: every free air cell within {@code radius} of the cell in front of the face, in
     * the plane of the face (3 x 3 for a radius of 1).
     *
     * @return how many webs were placed
     */
    public static int patch(ServerLevel level, ServerPlayer spider, ArrayDeque<WebRef> webs, BlockPos hit, Direction face, int radius,
                            boolean organic) {
        BlockPos front = hit.relative(face);
        int placed = 0;
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                BlockPos pos = switch (face.getAxis()) {
                    case X -> front.offset(0, a, b);
                    case Y -> front.offset(a, 0, b);
                    case Z -> front.offset(a, b, 0);
                };
                if (place(level, spider, webs, pos, organic)) {
                    placed++;
                }
            }
        }
        return placed;
    }

    /** The cocoon round a webbed creature: a web at its feet and one at the height of its chest. */
    public static int cocoon(ServerLevel level, ServerPlayer spider, ArrayDeque<WebRef> webs, LivingEntity target, boolean organic) {
        BlockPos feet = BlockPos.containing(target.getX(), target.getY() + 0.1, target.getZ());
        BlockPos chest = BlockPos.containing(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
        int placed = place(level, spider, webs, feet, organic) ? 1 : 0;
        if (!chest.equals(feet) && place(level, spider, webs, chest, organic)) {
            placed++;
        }
        return placed;
    }

    private SpiderWebs() {
    }
}
