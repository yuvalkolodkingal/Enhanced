package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Enhanced.MODID);

    /** A glowing, coloured mote of energy (chaos magic, magnetism, light). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<EnergyParticleOptions>> ENERGY = PARTICLES.register("energy",
            () -> new ParticleType<EnergyParticleOptions>(false) {
                @Override
                public MapCodec<EnergyParticleOptions> codec() {
                    return EnergyParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, EnergyParticleOptions> streamCodec() {
                    return EnergyParticleOptions.STREAM_CODEC;
                }
            });

    private ModParticles() {
    }
}
