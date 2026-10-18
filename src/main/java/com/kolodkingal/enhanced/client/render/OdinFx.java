package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.client.asgard.AsgardFx;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.AsgardFxPayload;
import com.kolodkingal.enhanced.odin.OdinAbilities;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Odin's light (3.5), on the client: what the server's {@link AsgardFxPayload} kinds of his lane show to everyone near (the
 * bolt of the Gungnir Blast, the Heal's motes, the column of a banishing, the Odinforce Wave's ring, the Command's call, the
 * pillar of the coronation, a Gungnir coming to a hand, the shield shattering), and the golden cocoon of the Odinsleep, laid
 * every tick round every sleeping Odin in view. All particles: nothing here is drawn per frame.
 */
public final class OdinFx {
    /** Effects that last a few ticks (the wave's ring, the coronation's pillar). */
    private static final List<Timed> TIMED = new ArrayList<>();

    private record Timed(int kind, Vec3 at, int entityId, long start, int ticks, float size) {
    }

    /** Registers the drawers of his kinds (once, from the client's setup). */
    public static void register() {
        AsgardFx.register(OdinAbilities.FX_BLAST, OdinFx::blast);
        AsgardFx.register(OdinAbilities.FX_HEAL, OdinFx::heal);
        AsgardFx.register(OdinAbilities.FX_BANISH, OdinFx::banish);
        AsgardFx.register(OdinAbilities.FX_WAVE, p -> timed(p, 10, p.arg() / 10.0F));
        AsgardFx.register(OdinAbilities.FX_COMMAND, OdinFx::command);
        AsgardFx.register(OdinAbilities.FX_CORONATION, p -> timed(p, Math.max(1, p.arg()), 1.0F));
        AsgardFx.register(OdinAbilities.FX_CROWNED, OdinFx::crowned);
        AsgardFx.register(OdinAbilities.FX_GUNGNIR_GIVEN, OdinFx::given);
        AsgardFx.register(OdinAbilities.FX_SHIELD_BREAK, OdinFx::shieldBreak);
    }

    private static ClientLevel level() {
        return Minecraft.getInstance().level;
    }

    private static int glow(AsgardFxPayload p) {
        ClientLevel level = level();
        Entity who = level == null ? null : level.getEntity(p.entityId());
        return who instanceof Player player ? Powers.glow(player) : OdinAbilities.WHITE_GOLD;
    }

    private static int color(AsgardFxPayload p) {
        ClientLevel level = level();
        Entity who = level == null ? null : level.getEntity(p.entityId());
        return who instanceof Player player ? Powers.color(player) : OdinAbilities.GOLD;
    }

    /** The bolt: a line of golden light from the spear's tip to where it struck, and a burst there. */
    private static void blast(AsgardFxPayload p) {
        ClientLevel level = level();
        if (level == null || p.points().size() < 2) {
            return;
        }
        Vec3 from = p.points().get(0);
        Vec3 to = p.points().get(1);
        int color = color(p);
        int core = glow(p);
        double length = from.distanceTo(to);
        int steps = Math.max(2, (int) (length / 0.35));
        RandomSource r = level.random;
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.lerp(to, i / (double) steps);
            level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? core : color, 0.7F), at.x, at.y, at.z,
                    (r.nextDouble() - 0.5) * 0.02, (r.nextDouble() - 0.5) * 0.02, (r.nextDouble() - 0.5) * 0.02);
        }
        for (int i = 0; i < 14; i++) {
            level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? core : color, 1.0F), to.x, to.y, to.z,
                    (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3, (r.nextDouble() - 0.5) * 0.3);
        }
    }

    /** The heal: golden motes rise round everyone it touched. */
    private static void heal(AsgardFxPayload p) {
        ClientLevel level = level();
        if (level == null) {
            return;
        }
        RandomSource r = level.random;
        for (Vec3 at : p.points()) {
            for (int i = 0; i < 10; i++) {
                double a = r.nextDouble() * Math.PI * 2.0;
                level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? glow(p) : color(p), 0.6F), at.x + Math.cos(a) * 0.5, at.y - 0.6,
                        at.z + Math.sin(a) * 0.5, 0, 0.06 + r.nextDouble() * 0.05, 0);
            }
            level.addParticle(ParticleTypes.HEART, at.x, at.y + 0.9, at.z, 0, 0.05, 0);
        }
    }

    /** Banish: a column of golden light where the creature stood (and, if it was gone, a flash). */
    private static void banish(AsgardFxPayload p) {
        ClientLevel level = level();
        if (level == null || p.points().isEmpty()) {
            return;
        }
        Vec3 at = p.points().get(0);
        RandomSource r = level.random;
        for (int i = 0; i < 40; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double rad = 0.2 + r.nextDouble() * 0.6;
            level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? glow(p) : color(p), 1.0F), at.x + Math.cos(a) * rad, at.y - 1.0 + r.nextDouble(),
                    at.z + Math.sin(a) * rad, 0, 0.25 + r.nextDouble() * 0.25, 0);
        }
        if (p.arg() == 1) {
            level.addParticle(ParticleTypes.FLASH, at.x, at.y, at.z, 0, 0, 0);
            level.addParticle(ParticleTypes.END_ROD, at.x, at.y, at.z, 0, 0.2, 0);
        }
    }

    /** The command: a golden ring at his feet and sparks thrown up, as if a horn sounded. */
    private static void command(AsgardFxPayload p) {
        ClientLevel level = level();
        if (level == null || p.points().isEmpty()) {
            return;
        }
        Vec3 at = p.points().get(0);
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI * 2.0 / 32;
            level.addParticle(new EnergyParticleOptions(color(p), 0.8F), at.x + Math.cos(a) * 1.4, at.y + 0.1, at.z + Math.sin(a) * 1.4,
                    Math.cos(a) * 0.12, 0.08, Math.sin(a) * 0.12);
        }
    }

    /** He is crowned: a burst of gold round him, and he lifts the spear (the gesture of the coronation). */
    private static void crowned(AsgardFxPayload p) {
        ClientLevel level = level();
        if (level == null || !(level.getEntity(p.entityId()) instanceof AbstractClientPlayer player)) {
            return;
        }
        HeroAnimator.play(player, GestureId.ODIN_CROWN, 0.0F);
        RandomSource r = level.random;
        for (int i = 0; i < 60; i++) {
            Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.6 + 0.6, r.nextGaussian()).normalize();
            level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? OdinAbilities.GOLD : OdinAbilities.WHITE_GOLD, 1.2F),
                    player.getX(), player.getY() + 1.0, player.getZ(), d.x * 0.3, d.y * 0.3, d.z * 0.3);
        }
    }

    /** A Gungnir comes to a hand: a trail of gold from where it came (the stand) to him, or a flash at his hand. */
    private static void given(AsgardFxPayload p) {
        ClientLevel level = level();
        if (level == null || p.points().isEmpty()) {
            return;
        }
        Vec3 from = p.points().get(0);
        Vec3 to = p.points().size() > 1 ? p.points().get(1) : from;
        int steps = Math.max(1, (int) (from.distanceTo(to) / 0.3));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.lerp(to, i / (double) steps);
            level.addParticle(new EnergyParticleOptions(OdinAbilities.GOLD, 0.7F), at.x, at.y, at.z, 0, 0.02, 0);
        }
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            level.addParticle(new EnergyParticleOptions(OdinAbilities.WHITE_GOLD, 0.8F), to.x, to.y, to.z, Math.cos(a) * 0.15, 0.05,
                    Math.sin(a) * 0.15);
        }
    }

    /** The shield shatters: golden shards fly out from the sphere. */
    private static void shieldBreak(AsgardFxPayload p) {
        ClientLevel level = level();
        if (level == null || p.points().isEmpty()) {
            return;
        }
        Vec3 c = p.points().get(0);
        double radius = p.arg() / 10.0;
        RandomSource r = level.random;
        for (int i = 0; i < 56; i++) {
            Vec3 out = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize();
            Vec3 at = c.add(out.scale(radius));
            level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? glow(p) : color(p), 1.3F), at.x, at.y, at.z, out.x * 0.3, out.y * 0.3,
                    out.z * 0.3);
        }
    }

    private static void timed(AsgardFxPayload p, int ticks, float size) {
        ClientLevel level = level();
        if (level == null || p.points().isEmpty()) {
            return;
        }
        TIMED.add(new Timed(p.kind(), p.points().get(0), p.entityId(), level.getGameTime(), ticks, size));
    }

    // ---- every client tick ----

    static void tick(ClientLevel level) {
        long now = level.getGameTime();
        RandomSource r = level.random;
        for (Iterator<Timed> it = TIMED.iterator(); it.hasNext(); ) {
            Timed t = it.next();
            long age = now - t.start();
            if (age > t.ticks() || age < 0) {
                it.remove();
                continue;
            }
            if (t.kind() == OdinAbilities.FX_WAVE) {
                float radius = OdinHeroMath.waveRadius(age, t.size());
                int n = Math.max(16, (int) (radius * 7));
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2.0 / n + r.nextDouble() * 0.1;
                    level.addParticle(new EnergyParticleOptions(OdinAbilities.GOLD, 0.9F), t.at().x + Math.cos(a) * radius, t.at().y + 0.15,
                            t.at().z + Math.sin(a) * radius, 0, 0.02, 0);
                }
            } else if (t.kind() == OdinAbilities.FX_CORONATION) {
                // The pillar of light over the throne: motes rising round it, thicker as the moment comes.
                float u = (float) age / t.ticks();
                int n = 4 + (int) (10 * u);
                for (int i = 0; i < n; i++) {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double rad = 0.4 + r.nextDouble() * (1.0 - 0.5 * u);
                    level.addParticle(new EnergyParticleOptions(i % 3 == 0 ? OdinAbilities.WHITE_GOLD : OdinAbilities.GOLD, 1.1F),
                            t.at().x + Math.cos(a) * rad, t.at().y + r.nextDouble() * 3.0, t.at().z + Math.sin(a) * rad, 0, 0.12, 0);
                }
                if (age % 10 == 0) {
                    level.addParticle(ParticleTypes.END_ROD, t.at().x, t.at().y + 2.6, t.at().z, 0, 0.05, 0);
                }
            }
        }
        // The cocoon of every Odin who sleeps the Odinsleep: an egg of golden motes that closes over him and breathes.
        for (Player p : level.players()) {
            PowerData data = Powers.get(p);
            if (!data.toggle(Toggle.ODINSLEEP) || !data.has(HeroAbility.ODIN_COMMAND) || p.isInvisible()) {
                continue;
            }
            float t = p.tickCount % 2000;
            float radius = OdinHeroMath.cocoonRadius(t);
            for (int i = 0; i < 6; i++) {
                double a = r.nextDouble() * Math.PI * 2.0;
                double h = r.nextDouble() * 2.0 - 1.0;
                double ring = Math.sqrt(Math.max(0.0, 1.0 - h * h)) * radius;
                level.addParticle(new EnergyParticleOptions(i % 2 == 0 ? Powers.glow(p) : Powers.color(p), 0.55F),
                        p.getX() + Math.cos(a) * ring, p.getY() + 0.75 + h * radius * 1.1, p.getZ() + Math.sin(a) * ring,
                        -Math.sin(a) * 0.01, 0.005, Math.cos(a) * 0.01);
            }
        }
    }

    static void clear() {
        TIMED.clear();
    }

    private OdinFx() {
    }
}
