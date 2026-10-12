package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.apocalypse.ApocalypseEntity;
import com.kolodkingal.enhanced.apocalypse.ApocalypseShield;
import com.kolodkingal.enhanced.apocalypse.ApocalypseState;
import com.kolodkingal.enhanced.client.ModelViewer;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What {@code /enhanced_model} can show of the Apocalypse lane: En Sabah Nur in every state of his controller and at every
 * tick of it ({@code /enhanced_model apocalypse tell_wave 12} is his Wave twelve ticks in), armoured or, with the prefix
 * {@code true_}, in the true form; with his shield in every state it has ({@code shield_up}, {@code shield_cracked},
 * {@code shield_forming}, {@code shield_broken}); and the lance, the boulder and the cocoon. For the people who draw: the
 * models are photographed here, state by state.
 */
public final class ApocalypseViewer {
    /** The extra states that are about the shield: the name, the state of the shield and how strong it is. */
    private static final List<String> SHIELDS = List.of("shield_up", "shield_cracked", "shield_forming", "shield_reforming", "shield_broken");
    private static final List<String> STATES = new ArrayList<>();

    static {
        for (ApocalypseState state : ApocalypseState.values()) {
            STATES.add(state.name().toLowerCase(Locale.ROOT));
        }
        STATES.add("dying_fire");
        for (ApocalypseState state : ApocalypseState.values()) {
            STATES.add("true_" + state.name().toLowerCase(Locale.ROOT));
        }
        STATES.addAll(SHIELDS);
    }

    /** Offers the lane's models to the viewer, once, when the client starts. */
    public static void register() {
        ModelViewer.register("apocalypse", new Boss());
        ModelViewer.register("sand_cocoon", new Simple(ModEntities.SAND_COCOON.get(), List.of("cocoon")));
        ModelViewer.register("sand_lance", new Simple(ModEntities.SAND_LANCE.get(), List.of("flying")));
        ModelViewer.register("sand_boulder", new Simple(ModEntities.SAND_BOULDER.get(), List.of("flying")));
    }

    /** Whether a name of the viewer is about the shield, and the numbers it stands for. */
    static ApocalypseShield.State shieldOf(String name) {
        return switch (name) {
            case "shield_up", "shield_cracked" -> ApocalypseShield.State.UP;
            case "shield_forming" -> ApocalypseShield.State.FORMING;
            case "shield_reforming" -> ApocalypseShield.State.REFORMING;
            case "shield_broken" -> ApocalypseShield.State.BROKEN;
            default -> ApocalypseShield.State.GONE;
        };
    }

    /** The boss. */
    private static final class Boss implements ModelViewer.Subject {
        private int lastTick = -1;

        @Override
        public EntityType<?> type() {
            return ModEntities.APOCALYPSE.get();
        }

        @Override
        public List<String> states() {
            return STATES;
        }

        @Override
        public void pose(Entity entity, String state, int tick) {
            if (!(entity instanceof ApocalypseEntity boss)) {
                return;
            }
            boolean running = lastTick >= 0 && tick != lastTick;
            lastTick = tick;
            boolean frail = state.startsWith("true_");
            String name = frail ? state.substring(5) : state;
            boolean fire = name.equals("dying_fire");
            ApocalypseState shown = ApocalypseState.IDLE;
            ApocalypseShield.State shield = ApocalypseShield.State.GONE;
            float fraction = 1.0F;
            if (SHIELDS.contains(name)) {
                shield = shieldOf(name);
                fraction = name.equals("shield_cracked") ? 0.25F : 1.0F;
                shown = shield == ApocalypseShield.State.BROKEN ? ApocalypseState.STAGGERED : ApocalypseState.IDLE;
            } else {
                for (ApocalypseState candidate : ApocalypseState.values()) {
                    if (candidate.name().toLowerCase(Locale.ROOT).equals(fire ? "dying" : name)) {
                        shown = candidate;
                    }
                }
            }
            // He faces the viewer, so that is where his aim goes (the chest of whoever is looking).
            Vec3 aim = entity.position().add(entity.getLookAngle().scale(5.0)).add(0.0, 1.0, 0.0);
            boss.showState(shown, tick, running, frail, shield, fraction, frail ? 3 : 1, fire, aim);
        }
    }

    /** A model with nothing to pose. */
    private static final class Simple implements ModelViewer.Subject {
        private final EntityType<?> type;
        private final List<String> states;

        Simple(EntityType<?> type, List<String> states) {
            this.type = type;
            this.states = states;
        }

        @Override
        public EntityType<?> type() {
            return type;
        }

        @Override
        public List<String> states() {
            return states;
        }

        @Override
        public void pose(Entity entity, String state, int tick) {
        }
    }

    private ApocalypseViewer() {
    }
}
