package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.effect.ConfusedEffect;
import com.kolodkingal.enhanced.effect.EmpoweredEffect;
import com.kolodkingal.enhanced.effect.EntombedEffect;
import com.kolodkingal.enhanced.effect.PossessedEffect;
import com.kolodkingal.enhanced.effect.RadioactiveBiteEffect;
import com.kolodkingal.enhanced.effect.SpellboundEffect;
import com.kolodkingal.enhanced.effect.StoneEffect;
import com.kolodkingal.enhanced.effect.WebbedEffect;
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

    // The effects of 3.0, in one region per hero (the // ===== lines).

    // ===== phoenix =====
    /** A creature whose mind the Phoenix has taken over. */
    public static final DeferredHolder<MobEffect, PossessedEffect> POSSESSED = EFFECTS.register("possessed", PossessedEffect::new);

    // ===== spider =====
    public static final DeferredHolder<MobEffect, WebbedEffect> WEBBED = EFFECTS.register("webbed", WebbedEffect::new);
    public static final DeferredHolder<MobEffect, RadioactiveBiteEffect> RADIOACTIVE_BITE = EFFECTS.register("radioactive_bite",
            RadioactiveBiteEffect::new);

    // ===== thanos =====
    public static final DeferredHolder<MobEffect, StoneEffect> PETRIFIED = EFFECTS.register("petrified", () -> new StoneEffect(0x7A7A82));
    public static final DeferredHolder<MobEffect, StoneEffect> SOULWORLD = EFFECTS.register("soulworld", () -> new StoneEffect(0xFF8A1E));
    public static final DeferredHolder<MobEffect, StoneEffect> TIME_LOCKED = EFFECTS.register("time_locked", () -> new StoneEffect(0x5BDB6A));

    // ===== ultron =====

    // ===== apocalypse =====
    public static final DeferredHolder<MobEffect, EntombedEffect> ENTOMBED = EFFECTS.register("entombed", EntombedEffect::new);
    public static final DeferredHolder<MobEffect, EmpoweredEffect> EMPOWERED = EFFECTS.register("empowered", EmpoweredEffect::new);

    private ModEffects() {
    }
}
