package com.kolodkingal.enhanced.particle;

import com.kolodkingal.enhanced.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * @param color RGB colour of the mote
 * @param scale size multiplier (1 = a small spark)
 */
public record EnergyParticleOptions(int color, float scale) implements ParticleOptions {
    public static final MapCodec<EnergyParticleOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("color").forGetter(EnergyParticleOptions::color),
            Codec.FLOAT.optionalFieldOf("scale", 1F).forGetter(EnergyParticleOptions::scale)
    ).apply(i, EnergyParticleOptions::new));

    public static final StreamCodec<ByteBuf, EnergyParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, EnergyParticleOptions::color,
            ByteBufCodecs.FLOAT, EnergyParticleOptions::scale,
            EnergyParticleOptions::new);

    @Override
    public ParticleType<?> getType() {
        return ModParticles.ENERGY.get();
    }
}
