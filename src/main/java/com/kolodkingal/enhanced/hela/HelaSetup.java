package com.kolodkingal.enhanced.hela;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.npc.AsgardDialogue;
import com.kolodkingal.enhanced.asgard.npc.Role;
import com.kolodkingal.enhanced.boss.BossFight;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.List;

/**
 * The mod-bus side of Hela (3.5): when the mod loads, her lane is registered with the boss framework (so that the seal and
 * {@code /enhanced boss start hela} call her), the rule that her dead are on her side is given to every area attack
 * ({@link AreaTargets#addAllyRule}), and the warnings the warriors of Asgard give about her are added to their dialogue
 * (lane L5's talk screen shows them).
 */
@EventBusSubscriber(modid = Enhanced.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class HelaSetup {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BossFights.register(BossKind.HELA, new HelaLane());
            AreaTargets.addAllyRule(HelaArmy::sameSide);
            registerDialogue();
        });
    }

    /** Whether a fight against Hela runs in {@code level} (the Einherjar shout their warning while it does). */
    public static boolean fightIn(ServerLevel level) {
        BossFight fight = BossFights.active(level);
        return fight instanceof HelaFight && !fight.isOver();
    }

    /**
     * Her nodes in the Asgardians' dialogue: while she fights, every guard and warrior says "Halt!" (the highest priority:
     * nothing else matters then); to one who has beaten her, the warriors say so.
     */
    static void registerDialogue() {
        for (Role role : List.of(Role.EINHERJAR, Role.SIF, Role.VALKYRIE, Role.VOLSTAGG, Role.FANDRAL, Role.HOGUN)) {
            AsgardDialogue.register(role, new AsgardDialogue.Node("hela_halt",
                    ctx -> ctx.speaker().level() instanceof ServerLevel level && fightIn(level), 1000, List.of()));
            AsgardDialogue.register(role, new AsgardDialogue.Node("hela_fallen",
                    ctx -> ctx.progress().has(AsgardProgress.HELA_VICTOR), 30, List.of()));
        }
    }

    private HelaSetup() {
    }
}
