package com.kolodkingal.enhanced.client;

import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Who glows through walls for the local player, and in which colour: the creatures Billy hears with The Noise, the
 * minds the Phoenix senses, the dangers Spider-Man's sense warns him of, the souls Thanos sees. Each registers a source
 * once; the two mixins ({@code MinecraftMixin}, which says who glows, and {@code LevelRendererMixin}, which says in
 * what colour) ask this class, so they are never edited again.
 *
 * <p>The first source that reveals an entity decides its colour. Called every frame for every entity in view: a source
 * must be cheap.
 */
public final class EntityGlow {
    private record Source(Predicate<Entity> reveals, ToIntFunction<Entity> color) {
    }

    private static final List<Source> SOURCES = new ArrayList<>();

    /**
     * Adds a source.
     *
     * @param reveals whether the local player sees this entity glow right now
     * @param color   the colour it glows in ({@code 0xRRGGBB}), asked only for an entity {@code reveals} said yes to
     */
    public static void register(Predicate<Entity> reveals, ToIntFunction<Entity> color) {
        SOURCES.add(new Source(reveals, color));
    }

    /** Whether any source makes the entity glow. */
    public static boolean reveals(Entity entity) {
        for (Source source : SOURCES) {
            if (source.reveals().test(entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The colour the entity glows in: the first source that reveals it decides; {@code fallback} (its team's) if none
     * does.
     */
    public static int color(Entity entity, IntSupplier fallback) {
        for (Source source : SOURCES) {
            if (source.reveals().test(entity)) {
                return source.color().applyAsInt(entity);
            }
        }
        return fallback.getAsInt();
    }

    private EntityGlow() {
    }
}
