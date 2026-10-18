package com.kolodkingal.enhanced.protect;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.beam.BeamStyle;
import com.kolodkingal.enhanced.darkhold.DarkholdBook;
import com.kolodkingal.enhanced.item.DarkholdItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Dark magic (3.3): what the ward of the Book of the Vishanti keeps out besides monsters and bosses. Today it is the
 * Darkhold's: its purple beam ({@code enhanced:darkhold_beam}), its hex mark and the poison it answers magic with. It is
 * data-driven, so another dark power joins by tag: its damage types in {@code #enhanced:dark_magic} (damage type tag), its
 * projectiles or creatures in {@code #enhanced:dark_magic} (entity type tag), its beam style in {@link #isDark(BeamStyle)}
 * and whoever casts it in {@link #casts}.
 */
public final class DarkMagic {
    /** Damage types that are dark magic. */
    public static final TagKey<DamageType> DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, Enhanced.id("dark_magic"));
    /** Projectiles and conjured things that are dark magic. */
    public static final TagKey<EntityType<?>> ENTITIES = TagKey.create(Registries.ENTITY_TYPE, Enhanced.id("dark_magic"));

    /** Whether this harm is dark magic. */
    public static boolean is(DamageSource source) {
        return source.is(DAMAGE);
    }

    /** Whether this thing (a projectile, a conjured creature) is dark magic. */
    public static boolean is(@Nullable Entity entity) {
        return entity != null && entity.getType().is(ENTITIES);
    }

    /** Whether a beam drawn in this style is dark magic (the Darkhold's purple beam). */
    public static boolean isDark(@Nullable BeamStyle style) {
        return style != null && style.id().equals(DarkholdItem.STYLE.id());
    }

    /**
     * Whether this one works dark magic right now, so that a line of their power (a beam) is dark: a holder of the Darkhold who
     * fires its purple beam.
     */
    public static boolean casts(@Nullable Entity entity) {
        // (3.3: every power of Agatha's is the Darkhold's magic.)
        return entity instanceof Player player
                && (DarkholdBook.firing(player) || com.kolodkingal.enhanced.power.Powers.power(player) == com.kolodkingal.enhanced.power.Power.AGATHA);
    }

    private DarkMagic() {
    }
}
