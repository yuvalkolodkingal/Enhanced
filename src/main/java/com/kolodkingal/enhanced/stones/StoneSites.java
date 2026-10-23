package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.block.StonePedestalBlock;
import com.kolodkingal.enhanced.platform.event.ServerStartedEvent;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.registry.ModBlocks;
import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where the site stones are put at run time (3.6): the Reality Stone's pedestal in Asgard's vault, put when the vault
 * opens ({@code odin/Vault.open}, after Hela's first defeat) and, for worlds whose vault opened in 3.5, when the server
 * starts with Odin awake. It is never part of the city's chunk generation, so {@code CityPlan.VERSION} does not change.
 * Hogun, asked about the vault once it is open, warns of the Aether sleeping in it.
 */
@Subscriber
public final class StoneSites {
    /** The pedestal faces Gungnir's stand across the vault (east). */
    static final Direction ASGARD_FACING = Direction.EAST;

    private static boolean dialogue;

    /**
     * Asgard's vault is open (Hela's first defeat, {@code odin/Vault.open}, and at server start for worlds whose vault
     * opened in 3.5): the stone pedestal with the Reality Stone is put at {@code CityPlan.REALITY_PEDESTAL} if it is
     * not there.
     */
    public static void placeAsgardPedestal(ServerLevel asgard) {
        BlockPos pos = CityPlan.REALITY_PEDESTAL;
        BlockState here = asgard.getBlockState(pos);
        if (here.is(ModBlocks.STONE_PEDESTAL.get())) {
            return;
        }
        BlockState pedestal = ModBlocks.STONE_PEDESTAL.get().defaultBlockState()
                .setValue(StonePedestalBlock.STONE, StonePedestalBlock.Stone.of(StoneKind.REALITY))
                .setValue(StonePedestalBlock.FACING, ASGARD_FACING);
        asgard.setBlock(pos, pedestal, Block.UPDATE_ALL);
    }

    /** Worlds whose vault opened before 3.6 get the pedestal when the server starts; and Hogun's line is added. */
    @Subscribe
    public static void onServerStarted(ServerStartedEvent event) {
        registerDialogue();
        ServerLevel asgard = AsgardDimension.level(event.getServer());
        if (asgard != null && AsgardState.get(event.getServer()).odinAwake()) {
            placeAsgardPedestal(asgard);
        }
    }

    /** Hogun's warning about the Aether (once): an answer to his talk about the vault, while Odin is awake. */
    public static synchronized void registerDialogue() {
        if (dialogue) {
            return;
        }
        dialogue = true;
        AsgardDialogue.node(Role.HOGUN, "aether").option(AsgardDialogue.Option.close("thanks")).register();
        AsgardDialogue.addOption(Role.HOGUN, "vault", AsgardDialogue.Option.to("about_aether",
                ctx -> ctx.state().odinAwake(), "aether"));
    }

    private StoneSites() {
    }
}
