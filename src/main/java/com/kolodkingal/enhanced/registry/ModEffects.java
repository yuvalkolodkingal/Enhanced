package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.effect.ConfusedEffect;
import com.kolodkingal.enhanced.effect.SpellboundEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Enhanced.MODID);

    /** Billy's Freeze: held in place, unable to move, jump or hurt anyone. */
    public static final DeferredHolder<MobEffect, SpellboundEffect> SPELLBOUND = EFFECTS.register("spellbound", SpellboundEffect::new);

    /** What Loki's Scepter does to a creature it hits: it forgets its target, can't pick another and stumbles about. */
    public static final DeferredHolder<MobEffect, ConfusedEffect> CONFUSED = EFFECTS.register("confused", ConfusedEffect::new);

    private ModEffects() {
    }
}
