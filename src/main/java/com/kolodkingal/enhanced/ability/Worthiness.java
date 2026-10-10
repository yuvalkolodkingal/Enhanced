package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * "Whosoever holds this hammer, if he be worthy, shall possess the power of Thor." Who may lift Mjolnir: Thor
 * himself; Vision (who lifted it in Age of Ultron); anyone who has proven themselves a hero by saving a village from a
 * raid (the Hero of the Village advancement); and players in Creative or Spectator mode. The server config can open it
 * to everyone.
 */
public final class Worthiness {
    public static final ResourceLocation HERO_OF_THE_VILLAGE = ResourceLocation.withDefaultNamespace("adventure/hero_of_the_village");

    public static boolean isWorthy(Player player) {
        Power power = Powers.power(player);
        if (power == Power.THOR || power == Power.VISION || player.isCreative() || player.isSpectator()) {
            return true;
        }
        if (EnhancedConfig.MJOLNIR_ANYONE_WORTHY.get()) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            AdvancementHolder hero = serverPlayer.server.getAdvancements().get(HERO_OF_THE_VILLAGE);
            return hero != null && serverPlayer.getAdvancements().getOrStartProgress(hero).isDone();
        }
        // Clients don't know other players' advancements: the server decides, the client only predicts.
        return false;
    }

    private Worthiness() {
    }
}
