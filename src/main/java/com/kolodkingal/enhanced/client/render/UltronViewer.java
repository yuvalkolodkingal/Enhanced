package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.ModelViewer;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.ultron.UltronEntity;
import com.kolodkingal.enhanced.ultron.UltronSentryEntity;
import com.kolodkingal.enhanced.ultron.UltronState;
import com.kolodkingal.enhanced.ultron.VibraniumKeyEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What {@code /enhanced_model} can show of the Ultron lane: the boss in every state of his ({@code idle}, {@code volley},
 * {@code beams}, {@code dive}, {@code pull}, {@code call}, {@code punch}, {@code stagger}, {@code lift}, {@code transfer},
 * {@code death}, {@code awakening}) and in each of his bodies ({@code hybrid}, {@code ultron3}), a Sentry in each act,
 * and the Key rising and powering down. {@code /enhanced_model ultron volley 14} shows the Volley frozen at its fourteenth
 * tick (the last of its tell); without the tick the state runs on from its first.
 */
public final class UltronViewer {
    private static final List<String> BOSS_STATES = List.of("idle", "volley", "beams", "dive", "pull", "call", "punch", "stagger", "lift",
            "transfer", "death", "awakening", "hybrid", "ultron3", "ultron4");
    private static final List<String> SENTRY_STATES = List.of("idle", "idle_red", "idle_white", "shot", "punch", "stunned", "assembling");
    private static final List<String> KEY_STATES = List.of("buried", "rising", "up", "powering_down", "dark");
    /** The tick each shown entity was last posed at: a tick that does not change is a frozen one. */
    private static final Map<Integer, Integer> LAST = new HashMap<>();

    /** Offers the models of the lane to the viewer (called once, when the client is set up). */
    public static void register() {
        ModelViewer.register("ultron", new ModelViewer.Subject() {
            @Override
            public EntityType<?> type() {
                return ModEntities.ULTRON.get();
            }

            @Override
            public List<String> states() {
                return BOSS_STATES;
            }

            @Override
            public void pose(Entity entity, String state, int tick) {
                UltronEntity boss = (UltronEntity) entity;
                UltronState shown = switch (state) {
                    case "hybrid", "ultron3", "ultron4" -> UltronState.IDLE;
                    default -> UltronState.valueOf(state.equals("lift") ? "LIFT_CHANNEL" : state.toUpperCase(Locale.ROOT));
                };
                int body = switch (state) {
                    case "hybrid" -> 1;
                    case "ultron3" -> 2;
                    case "ultron4" -> 3;
                    default -> 0;
                };
                boss.showState(shown, body, tick, frozen(entity, tick));
            }
        });
        ModelViewer.register("ultron_sentry", new ModelViewer.Subject() {
            @Override
            public EntityType<?> type() {
                return ModEntities.ULTRON_SENTRY.get();
            }

            @Override
            public List<String> states() {
                return SENTRY_STATES;
            }

            @Override
            public void pose(Entity entity, String state, int tick) {
                UltronSentryEntity sentry = (UltronSentryEntity) entity;
                String act = state.startsWith("idle") ? "idle" : state;
                UltronSentryEntity.Eyes eyes = switch (state) {
                    case "idle_red" -> UltronSentryEntity.Eyes.RED;
                    case "idle_white", "shot", "punch" -> UltronSentryEntity.Eyes.WHITE;
                    default -> UltronSentryEntity.Eyes.BLUE;
                };
                sentry.showAct(UltronSentryEntity.Act.valueOf(act.toUpperCase(Locale.ROOT)), eyes, tick);
            }
        });
        ModelViewer.register("vibranium_key", new ModelViewer.Subject() {
            @Override
            public EntityType<?> type() {
                return ModEntities.VIBRANIUM_KEY.get();
            }

            @Override
            public List<String> states() {
                return KEY_STATES;
            }

            @Override
            public void pose(Entity entity, String state, int tick) {
                VibraniumKeyEntity key = (VibraniumKeyEntity) entity;
                int up = VibraniumKeyEntity.EMERGE_TICKS;
                switch (state) {
                    case "buried" -> key.showStage(-1000000, -1);
                    case "rising" -> key.showStage(tick, -1);
                    case "up" -> key.showStage(up + 40 + tick, -1);
                    case "powering_down" -> key.showStage(up + 200 + tick, tick);
                    default -> key.showStage(up + 400, 200);
                }
            }
        });
    }

    private static boolean frozen(Entity entity, int tick) {
        Integer last = LAST.put(entity.getId(), tick);
        return last != null && last == tick;
    }

    private UltronViewer() {
    }
}
