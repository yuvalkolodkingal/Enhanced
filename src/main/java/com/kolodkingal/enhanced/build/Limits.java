package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A builder's numbers for one cast (D-B 8.2, 9.1, 14.2), read live from the config: what {@link SpellRecipe#clamped}
 * forces a recipe into, and what the checks of a cast ask (D-B 10.2 C7 to C13): the reach, the longest side, the
 * shapes, the copies of a Repeat, the brush, the magic-select cap, the undo steps, the queue and the pause, the
 * ceiling, TNT, lava and fire, whether copies keep what containers hold. The same on both sides: the server's checks, a
 * client's rubber band and badge.
 *
 * <ul>
 *     <li><b>Billy</b> has his rank's numbers ({@link BillyRanks}): side and reach 24 at rank 1, shapes of radius
 *     (side - 1) / 2 and height up to his side, his Repeat count, a brush of side / 8, a double click as big as his
 *     meter, one spell at a time with a pause after it, never TNT, lava or fire, copies of containers that come out
 *     empty.</li>
 *     <li><b>The Scarlet Witch</b> has no gameplay limit (R39), only the technical ones of {@code [build_magic_wanda]}:
 *     her reach, her biggest shape, brush and selection; her side is the world itself, her shapes as tall as the world,
 *     a Repeat of any count when {@code repeatMax} is 0 (the world's width bounds it).</li>
 *     <li>Billy in Creative with {@code build_magic_billy.limitsInCreative} off has hers (he is still Billy: his
 *     switch, his messages, his crown).</li>
 * </ul>
 *
 * <p>The world part ({@link #inWorld}) is the caster's dimension at the cast: its build height and its world border.
 * Before the server config has loaded on a client, the defaults.
 */
public final class Limits implements RecipeLimits {
    /**
     * How far beyond his reach the server still takes an anchor, a pick or a selected cell (D-B 10.2 C7): the client
     * chose it a moment earlier, and the player may have moved since.
     */
    public static final int REACH_SLACK = 2;

    private final Power hero;
    private final int rank;
    private final boolean limited;
    private final BoundingBox world;

    private Limits(Power hero, int rank, boolean limited, BoundingBox world) {
        this.hero = hero;
        this.rank = Mth.clamp(rank, BillyRanks.MIN_RANK, BillyRanks.MAX_RANK);
        this.limited = limited;
        this.world = world;
    }

    /**
     * The numbers of {@code player} now: the Scarlet Witch's, or Billy's by the rank that applies to him (his
     * {@link BuildData}), in the world he stands in; null for anyone else (Build Magic is theirs alone). 3.4: a custom hero with
     * Billy's Build Magic has Billy's rules (the meter, his rank, no Instant).
     */
    public static @Nullable Limits of(Player player) {
        Power power = Powers.power(player);
        BoundingBox world = world(player.level());
        if (power == Power.SCARLET_WITCH || power == Power.CUSTOM
                && Powers.get(player).has(HeroAbility.WANDA_BUILD_MAGIC)) {
            // (3.5: a custom hero with the Scarlet Witch's Build Magic has her numbers and her Instant)
            return wanda(world);
        }
        if (power == Power.BILLY || power == Power.CUSTOM && Powers.get(player).has(HeroAbility.BILLY_BUILD_MAGIC)) {
            int rank = BillyRanks.rank(player.getData(ModAttachments.BUILD_DATA));
            return player.isCreative() && !limitsInCreative() ? new Limits(Power.BILLY, rank, false, world) : billy(rank, world);
        }
        return null;
    }

    /** The Scarlet Witch's numbers in a world of these cells. */
    public static Limits wanda(BoundingBox world) {
        return new Limits(Power.SCARLET_WITCH, BillyRanks.MIN_RANK, false, world);
    }

    /** Billy's numbers at {@code rank} (capped by {@code maxRank}), with his limits, in a world of these cells. */
    public static Limits billy(int rank, BoundingBox world) {
        return new Limits(Power.BILLY, Math.min(rank, BillyRanks.maxRank()), true, world);
    }

    /**
     * The cells of a level a spell may change: inside its build height and inside its world border, as the planner
     * reads them ({@link SpellMath#world}).
     */
    public static BoundingBox world(Level level) {
        return SpellMath.world(level);
    }

    /** The hero these numbers belong to: {@link Power#SCARLET_WITCH} or {@link Power#BILLY}. */
    public Power hero() {
        return hero;
    }

    /** Whether they are Billy's. */
    public boolean billy() {
        return hero == Power.BILLY;
    }

    /**
     * Whether Billy's limits apply: his meter pays every spell, his side, reach and counts are his rank's. False for
     * the Scarlet Witch, and for Billy in Creative with {@code build_magic_billy.limitsInCreative} off.
     */
    public boolean limited() {
        return limited;
    }

    /** The rank whose numbers apply (1 for the Scarlet Witch). */
    public int rank() {
        return rank;
    }

    /** The cells of the caster's world: its build height and its border. */
    public BoundingBox world() {
        return world;
    }

    /** Whether the hero's own switch is on ({@code build_magic_wanda.enabled}, {@code build_magic_billy.enabled}). */
    public boolean enabled() {
        if (!loaded()) {
            return true;
        }
        return billy() ? BuildConfig.BILLY_ENABLED.get() : BuildConfig.WANDA_ENABLED.get();
    }

    // ------------------------------------------------------------------------------------------------------------
    // What a recipe is clamped to

    /** Billy's longest side; the Scarlet Witch's is the world's widest. */
    @Override
    public int side() {
        if (limited) {
            return BillyRanks.side(rank);
        }
        return Math.max(world.getXSpan(), Math.max(world.getYSpan(), world.getZSpan()));
    }

    @Override
    public int reach() {
        if (limited) {
            return BillyRanks.reach(rank);
        }
        return loaded() ? BuildConfig.WANDA_REACH.get() : 256;
    }

    /** Billy's (side - 1) / 2 (11 at rank 1), so a shape fits his side; the Scarlet Witch's {@code shapeMaxRadius}. */
    @Override
    public int shapeRadius() {
        if (limited) {
            return Math.max(1, (side() - 1) / 2);
        }
        return loaded() ? BuildConfig.WANDA_SHAPE_MAX_RADIUS.get() : 512;
    }

    /** Billy's side; the Scarlet Witch's the world's height. */
    @Override
    public int shapeHeight() {
        return limited ? side() : world.getYSpan();
    }

    /** Billy's count by rank; the Scarlet Witch's {@code repeatMax}, or no count at all when it is 0 (R39). */
    @Override
    public int repeatCopies() {
        if (limited) {
            return BillyRanks.repeatMax(rank);
        }
        int max = loaded() ? BuildConfig.WANDA_REPEAT_MAX.get() : 0;
        return max <= 0 ? Integer.MAX_VALUE : max;
    }

    /** Billy's side / 8 (3 at rank 1); the Scarlet Witch's {@code brushMax}, a technical bound (R39). */
    @Override
    public int brush() {
        if (limited) {
            return BillyRanks.brush(rank);
        }
        return loaded() ? BuildConfig.WANDA_BRUSH_MAX.get() : 16;
    }

    /** The nearest cell inside the world: its y in the build height, its x and z inside the border. */
    @Override
    public BlockPos inWorld(BlockPos pos) {
        if (world.isInside(pos)) {
            return pos;
        }
        return new BlockPos(Mth.clamp(pos.getX(), world.minX(), world.maxX()), Mth.clamp(pos.getY(), world.minY(), world.maxY()),
                Mth.clamp(pos.getZ(), world.minZ(), world.maxZ()));
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the checks of a cast ask (D-B 10.2)

    /**
     * The side a box, a selected thing, a pocket or a Pull surface may not pass: Billy's, or 0 when the hero has none
     * (the planner's Pull span and {@link MagicSelect}'s side take 0 as no limit).
     */
    public int sideLimit() {
        return limited ? side() : 0;
    }

    /** Whether a box fits the longest side (C7, {@code TOO_WIDE}); for the Scarlet Witch, always. */
    public boolean fits(BoundingBox box) {
        if (!limited) {
            return true;
        }
        int side = side();
        return box.getXSpan() <= side && box.getYSpan() <= side && box.getZSpan() <= side;
    }

    /**
     * Whether the cell is within reach of the eyes (C7, {@code TOO_FAR}): its centre at most {@link #reach} blocks
     * away, and {@link #REACH_SLACK} more on the server ({@code slack}), whose player may have moved since he clicked.
     */
    public boolean inReach(Vec3 eyes, BlockPos pos, boolean slack) {
        double radius = reach() + (slack ? REACH_SLACK : 0);
        return eyes.distanceToSqr(Vec3.atCenterOf(pos)) <= radius * radius;
    }

    /** The most cells a double click selects: Billy's meter, the Scarlet Witch's {@code magicSelectMax}. */
    public int magicSelect() {
        if (limited) {
            return BillyRanks.magicSelect(rank);
        }
        return loaded() ? BuildConfig.WANDA_MAGIC_SELECT_MAX.get() : 100000;
    }

    /**
     * Magic select from {@code seeds} with these numbers: the cap, Billy's side, the reach from {@code eyes} (with the
     * server's {@code slack}, {@link #inReach}).
     */
    public MagicSelect select(BlockGetter level, Collection<BlockPos> seeds, Vec3 eyes, boolean slack) {
        return new MagicSelect(level, seeds, magicSelect(), sideLimit(), new MagicSelect.Reach(eyes, reach(), slack ? REACH_SLACK : 0));
    }

    /**
     * How many spells can be undone: Billy's by rank (10 at rank 1); the Scarlet Witch's {@code undoSteps}, 0 meaning
     * no count, only what the undo memory and disk hold (R39).
     */
    public int undoSteps() {
        if (limited) {
            return BillyRanks.undoSteps(rank);
        }
        return loaded() ? BuildConfig.WANDA_UNDO_STEPS.get() : 0;
    }

    /**
     * The most blocks one of her spells, undos or redos may write in the tick it is cast when her Instant switch is on
     * ({@code build_magic_wanda.instantMaxBlocks}, 3.2); 0 for Billy, who has no Instant level, and when the server turned
     * it off.
     */
    public int instantMax() {
        if (hero != Power.SCARLET_WITCH) {
            return 0;
        }
        return loaded() ? BuildConfig.WANDA_INSTANT_MAX_BLOCKS.get() : 250000;
    }

    /** The most blocks one spell may cover ({@code build_magic_wanda.maxBlocks}, C11, {@code CEILING}); 0 for none. */
    public int maxBlocks() {
        if (limited) {
            return 0;
        }
        return loaded() ? BuildConfig.WANDA_MAX_BLOCKS.get() : 0;
    }

    /** Whether the hero's spells may make TNT, lava or fire at all (C8: never Billy's, {@code VOLATILE}). */
    public boolean volatileAllowed() {
        return !limited;
    }

    /** The most TNT, lava and fire one spell may make (C13, {@code VOLATILE_CAP}); 0 for Billy. */
    public int volatilePerSpell() {
        if (limited) {
            return 0;
        }
        return loaded() ? BuildConfig.WANDA_VOLATILE_PER_SPELL.get() : 4096;
    }

    /**
     * How many spells may wait behind the one at work (C11, {@code QUEUE_FULL}): none for Billy, one spell at a time.
     */
    public int maxQueued() {
        if (limited) {
            return 0;
        }
        return loaded() ? BuildConfig.MAX_QUEUED.get() : 4;
    }

    /** Ticks after one of his spells before the next (C10, {@code WAIT}): Billy's {@code castPause}, else none. */
    public int castPause() {
        if (!limited) {
            return 0;
        }
        return loaded() ? BuildConfig.BILLY_CAST_PAUSE.get() : 10;
    }

    /** Whether a copy keeps what containers hold (D-B 11.6): {@code copiesContents} of the hero's section. */
    public boolean copiesContents() {
        if (!loaded()) {
            return !limited;
        }
        return limited ? BuildConfig.BILLY_COPIES_CONTENTS.get() : BuildConfig.WANDA_COPIES_CONTENTS.get();
    }

    /** Shares of the server's work a spell gets inside its caster's own Hex ({@code build_magic_wanda.hexBoost}). */
    public double hexBoost() {
        if (limited) {
            return 1.0;
        }
        return loaded() ? BuildConfig.WANDA_HEX_BOOST.get() : 2.0;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the hero is told

    /** "Too far for your magic (at most %s blocks)" ({@code TOO_FAR}). */
    public static Component tooFar(int reach) {
        return Component.translatable("message.enhanced.build.too_far", reach);
    }

    /** "Too big for you yet: at most %s blocks on a side" ({@code TOO_WIDE}). */
    public static Component tooWide(int side) {
        return Component.translatable("message.enhanced.build.too_wide", side);
    }

    private static boolean limitsInCreative() {
        return !loaded() || BuildConfig.BILLY_LIMITS_IN_CREATIVE.get();
    }

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }
}
