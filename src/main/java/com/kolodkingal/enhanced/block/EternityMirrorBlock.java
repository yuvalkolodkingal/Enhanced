package com.kolodkingal.enhanced.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.TransparentBlock;

/**
 * The still mirror of Eternity's realm (3.8, PLAN 10.4): a solid, full, translucent block, pale silver-blue water you
 * walk on; the realm's one layer at y 63. Not obtainable in Survival (the creative tab only). Registered by the 3.8
 * skeleton (translucent, unbreakable); lane L8 owns its look (the ripple, the footsteps' rings).
 */
public class EternityMirrorBlock extends TransparentBlock {
    public static final MapCodec<EternityMirrorBlock> CODEC = simpleCodec(EternityMirrorBlock::new);

    public EternityMirrorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends TransparentBlock> codec() {
        return CODEC;
    }
}
