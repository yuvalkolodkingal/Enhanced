package com.kolodkingal.enhanced.block;

import com.kolodkingal.enhanced.config.SpiderConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The temporary web Spider-Man leaves where a web lands (DESIGN section 14): a cobweb that holds whoever walks into it,
 * except a Spider-Man (and a Creative player who flies), and that disappears by itself. It is only ever put into free
 * air (see {@code SpiderWebs}), is not obtainable (no item, no drop) and lives {@link SpiderConfig#WEB_BLOCK_TICKS}
 * ticks, or longer if it was made with organic webs: the one who places it schedules the tick before the block goes in,
 * and the block asks for the default only if nobody did. Scheduled ticks are saved with the chunk, so a world that closes
 * and opens again still cleans up and nothing ever stays.
 */
public class SpiderWebBlock extends WebBlock {
    public SpiderWebBlock(Properties properties) {
        super(properties);
    }

    /** Spider-Man is never held by a web, nor is a Creative player who flies. */
    public static boolean spares(Entity entity) {
        return entity instanceof Player player && (Powers.is(player, Power.SPIDER_MAN) || player.getAbilities().flying);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!spares(entity)) {
            super.entityInside(state, level, pos, entity);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            // (A tick scheduled for this cell and block before it went in is kept: a second one for the same place is ignored.)
            level.scheduleTick(pos, this, SpiderConfig.WEB_BLOCK_TICKS.get());
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBWEB)), pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, 3, 0.25, 0.25, 0.25, 0.02);
        level.removeBlock(pos, false);
    }
}
