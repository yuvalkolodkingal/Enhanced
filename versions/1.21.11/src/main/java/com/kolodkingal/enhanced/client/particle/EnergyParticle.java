package com.kolodkingal.enhanced.client.particle;
//#norenames

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

/**
 * A glowing mote that fades and shrinks. Always full-bright, so it glows in the dark. (1.21.11's: a particle is a
 * single quad with a layer from 1.21.9; the 1.21.1 file is the same mote.)
 */
public class EnergyParticle extends SingleQuadParticle {
    private final float baseSize;

    protected EnergyParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                             EnergyParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        int color = options.color();
        float shade = 0.85F + random.nextFloat() * 0.15F;
        this.rCol = ((color >> 16) & 0xFF) / 255F * shade;
        this.gCol = ((color >> 8) & 0xFF) / 255F * shade;
        this.bCol = (color & 0xFF) / 255F * shade;
        this.baseSize = 0.11F * options.scale() * (0.75F + random.nextFloat() * 0.5F);
        this.quadSize = baseSize;
        this.lifetime = 10 + random.nextInt(12);
        this.friction = 0.9F;
        this.gravity = 0F;
        this.hasPhysics = false;
        this.setSprite(sprites.get(random));
    }

    @Override
    public void tick() {
        super.tick();
        float life = (float) age / lifetime;
        this.alpha = 1F - life * life;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = (age + partialTick) / lifetime;
        return baseSize * (1F - life * 0.6F);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<EnergyParticleOptions> {
        @Override
        public Particle createParticle(EnergyParticleOptions options, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd, RandomSource random) {
            return new EnergyParticle(level, x, y, z, xd, yd, zd, options, sprites);
        }
    }
}
