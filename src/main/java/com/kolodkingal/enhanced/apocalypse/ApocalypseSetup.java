package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.PhoenixPower;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.boss.BossKind;
import com.kolodkingal.enhanced.entity.SandCocoonEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * The mod-bus side of Apocalypse: when the mod loads, his lane is registered with the boss framework (so that the
 * Capstone's ritual and {@code /enhanced boss start apocalypse} can wake him), the Phoenix's mind is told what it does to
 * his shield, and the cocoons a hero casts are exempted from the orphan sweep (a boss's are marked by their fight).
 */
@EventBusSubscriber(modid = Enhanced.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ApocalypseSetup {

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BossFights.register(BossKind.APOCALYPSE, new ApocalypseLane());
            BossFights.exemptFromSweep(entity -> entity instanceof SandCocoonEntity cocoon && cocoon.isHeroes());
            registerPhoenixListener();
        });
    }

    /**
     * What the Phoenix's mind does to the shield besides her Burst and her slams (those are damage and reach it through
     * {@code hurt}): a Mind Control (even a refused one) strains it by {@code phoenixMindControlStrain}, a Psionic Field
     * whose sphere holds him by {@code phoenixPsionicStrain} (each once per cast), and either one, touching him in the tell
     * of the Mind Crush, interrupts it (the astral duel of the film). A hero Apocalypse's own shield is strained the same way
     * ({@link ApocalypseGuard#strain}).
     */
    static void registerPhoenixListener() {
        PhoenixPower.addListener(ApocalypseSetup::phoenixTouched);
    }

    static void phoenixTouched(ServerPlayer phoenix, LivingEntity target, PhoenixPower.Kind kind, float amount) {
        if (kind != PhoenixPower.Kind.MIND_CONTROL && kind != PhoenixPower.Kind.PSIONIC_FIELD) {
            return; // her Burst and her slams are damage: they reach the shield through hurt
        }
        if (target instanceof ServerPlayer hero) {
            ApocalypseGuard.strain(hero, phoenix, kind); // a hero Apocalypse behind his own shield
            return;
        }
        if (!(target instanceof ApocalypseEntity boss)) {
            return;
        }
        if (MindPower.isBreaker(phoenix)) {
            boss.strain(phoenix, kind);
        }
        boss.brain().interrupt();
    }

    private ApocalypseSetup() {
    }
}
