package com.kolodkingal.enhanced.block;

import net.minecraft.world.level.block.WebBlock;

/**
 * The temporary web Spider-Man leaves where a web shot lands: in the air only, and it goes away by itself. A stub: the
 * placing, the cap and the scheduled removal are built by the Spider lane (DESIGN section 14); until then it is only
 * registered, with a cobweb's behaviour.
 */
public class SpiderWebBlock extends WebBlock {
    public SpiderWebBlock(Properties properties) {
        super(properties);
    }
}
