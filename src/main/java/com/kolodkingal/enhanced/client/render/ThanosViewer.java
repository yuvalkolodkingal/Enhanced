package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.ModelViewer;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.thanos.ThanosDefence;
import com.kolodkingal.enhanced.thanos.ThanosEntity;
import com.kolodkingal.enhanced.thanos.ThanosMove;
import com.kolodkingal.enhanced.thanos.ThanosState;
import com.kolodkingal.enhanced.thanos.ThanosSwordEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What {@code /enhanced_model} can show of the Thanos lane: the Mad Titan in every state and at every tick of every
 * move ({@code /enhanced_model thanos sword_sweep 8} is his Sweep eight ticks in), with his stones, plates and fields;
 * his illusion; the sword, the meteor and the sphere. For the people who draw: the models are photographed here, state
 * by state.
 */
public final class ThanosViewer {
    /** The states of the Titan that are not moves, and how each puts him. */
    private static final List<String> STATES = new ArrayList<>();

    static {
        STATES.addAll(List.of("idle", "idle_2", "idle_3", "idle_4", "idle_5", "broken", "barrier", "veil", "surge", "surge_5", "stagger", "falling",
                "arriving", "dying"));
        for (ThanosMove move : ThanosMove.values()) {
            STATES.add(move.name().toLowerCase(Locale.ROOT));
        }
    }

    /** Offers the lane's models to the viewer, once, when the client starts. */
    public static void register() {
        ModelViewer.register("thanos", new Boss());
        ModelViewer.register("thanos_phantom", new Simple(ModEntities.THANOS_PHANTOM.get(), List.of("stand")));
        ModelViewer.register("thanos_sword", new Sword());
        ModelViewer.register("thanos_meteor", new Simple(ModEntities.THANOS_METEOR.get(), List.of("falling")));
        ModelViewer.register("thanos_orb", new Simple(ModEntities.THANOS_ORB.get(), List.of("flying")));
    }

    /** The Mad Titan. */
    private static final class Boss implements ModelViewer.Subject {
        @Override
        public EntityType<?> type() {
            return ModEntities.THANOS.get();
        }

        @Override
        public List<String> states() {
            return STATES;
        }

        @Override
        public void pose(Entity entity, String state, int tick) {
            if (!(entity instanceof ThanosEntity boss)) {
                return;
            }
            ThanosState shown = ThanosState.IDLE;
            ThanosMove move = null;
            int stones = 1;
            int plates = ThanosDefence.ALL_PLATES;
            int flags = 0;
            switch (state) {
                case "idle", "idle_2", "idle_3", "idle_4", "idle_5" -> stones = state.length() > 4 ? state.charAt(5) - '0' : 1;
                case "broken" -> {
                    stones = 5;
                    plates = 0;
                }
                case "barrier" -> {
                    stones = 2;
                    flags = ThanosEntity.FLAG_BARRIER;
                }
                case "veil" -> {
                    stones = 3;
                    flags = ThanosEntity.FLAG_VEIL;
                }
                case "surge" -> {
                    shown = ThanosState.SURGE;
                    stones = 2;
                }
                case "surge_5" -> {
                    shown = ThanosState.SURGE;
                    stones = 5;
                }
                case "stagger" -> {
                    shown = ThanosState.STAGGERED;
                    flags = ThanosEntity.FLAG_GAUNTLET_LOCKED;
                }
                case "falling", "arriving" -> shown = ThanosState.ARRIVING;
                case "dying" -> {
                    shown = ThanosState.DYING;
                    stones = 5;
                }
                default -> {
                    move = ThanosMove.valueOf(state.toUpperCase(Locale.ROOT));
                    shown = tick < move.tell() ? ThanosState.TELL : ThanosState.ACTIVE;
                    stones = move.stone() == null ? 5 : move.stone().ordinal() + 1;
                }
            }
            // He faces the viewer, so that is where his aim goes (a little below his hand).
            Vec3 aim = boss.forward().add(0.0, -0.1, 0.0).normalize();
            boss.showState(shown, move, tick, stones, plates, flags, aim);
        }
    }

    /** The sword, flying out or coming home. */
    private static final class Sword extends Simple {
        Sword() {
            super(ModEntities.THANOS_SWORD.get(), List.of("flying", "returning"));
        }

        @Override
        public void pose(Entity entity, String state, int tick) {
            if (entity instanceof ThanosSwordEntity sword) {
                sword.setReturning("returning".equals(state));
            }
        }
    }

    /** A model with nothing to pose. */
    private static class Simple implements ModelViewer.Subject {
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

    private ThanosViewer() {
    }
}
