package com.kolodkingal.enhanced.beam;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * The numbers of one tick of a beam, worked out by whoever fires it from its own config (and, for Billy, its rank): the
 * style it is drawn in, how far it reaches, what each hit does and how often it lands, what a tick of it costs in
 * hunger, and the damage type of its hits.
 *
 * @param style      how it looks (and which hand)
 * @param range      blocks
 * @param damage     what one hit does to the creature the crosshair is on
 * @param interval   ticks between two hits (the beam's damage type should bypass the creature's damage cooldown, so every
 *                   hit lands)
 * @param exhaustion food exhaustion a tick of the beam costs the one who fires it (4.0 is one food point)
 * @param damageType the damage type of the hits
 * @param onHit      what else a hit does (called after the damage, with the one who fires and what was hit), or null
 */
public record BeamSpec(BeamStyle style, double range, float damage, int interval, float exhaustion, ResourceKey<DamageType> damageType,
                       @Nullable BiConsumer<ServerPlayer, Entity> onHit) {
}
