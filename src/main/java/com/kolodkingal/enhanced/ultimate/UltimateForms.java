package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.SolarJewelConfig;
import com.kolodkingal.enhanced.config.UltimateConfig;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.magic.MagicFx;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The ultimate forms at work (3.17, Ultimate Mode): O awakens the player's {@link UltimateForm} (the awakening's column
 * of light and pose, then the form's aura, its strengths and a quarter more damage for {@code ultimate.durationSeconds}),
 * and O again, while it lasts, fires its signature move. Everything ends when the form does, the power changes, Ultimate
 * Mode is turned off, the player dies or leaves.
 */
public final class UltimateForms {
    /** One player's form: its awakening, how long it lasts, when it may come again, its signature's recharge. */
    private static final class State {
        final Power power;
        long awakenStart = -1;
        long activeUntil;
        long cooldownUntil;
        long signatureReady;
        /** The Demiurge's storm, still striking (game time it ends). */
        long stormUntil;
        /** A vortex about to throw out what it pulled in (game time, or 0). */
        long vortexAt;
        Vec3 vortexCenter = Vec3.ZERO;

        State(Power power) {
            this.power = power;
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private UltimateForms() {
    }

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    private static int awakenTicks() {
        return loaded() ? UltimateConfig.AWAKEN_TICKS.get() : 30;
    }

    /** Whether this player's ultimate form lasts now (the server's record). */
    public static boolean active(Player player) {
        State st = STATES.get(player.getUUID());
        return st != null && st.awakenStart < 0 && player.level().getGameTime() < st.activeUntil;
    }

    /** Seconds until the form can be awakened again (0: ready), for the HUD and the messages. */
    public static int cooldownSeconds(Player player) {
        State st = STATES.get(player.getUUID());
        if (st == null) {
            return 0;
        }
        long left = st.cooldownUntil - player.level().getGameTime();
        return left > 0 ? (int) ((left + 19) / 20) : 0;
    }

    // ---- the key ----

    /** O went down: awaken the form, or (while it lasts) fire its signature move. */
    public static void key(ServerPlayer player) {
        key(player, false);
    }

    /**
     * O went down ({@code alt}: with Sneak): awaken the form, or (while it lasts) fire its signature move. Sneak + O is the
     * Demiurge's Magic types ({@link Demiurge#cycle}), and ends the Nexus Being's form (it has no time limit).
     */
    public static void key(ServerPlayer player, boolean alt) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        if (alt && Ultimate.on() && com.kolodkingal.enhanced.magic.Magic.isDemiurge(player)) {
            Demiurge.cycle(player);
            return;
        }
        if (alt && active(player) && untimed(player)) {
            end(player, false);
            return;
        }
        if (!Ultimate.on()) {
            player.displayClientMessage(Component.translatable("message.enhanced.ultimate.off").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        UltimateForm form = UltimateForm.of(player);
        if (form == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.ultimate.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        long now = player.level().getGameTime();
        State st = STATES.get(player.getUUID());
        if (st != null && st.awakenStart >= 0) {
            return; // (it is awakening)
        }
        if (st != null && now < st.activeUntil) {
            if (now < st.signatureReady && !untimed(player)) {
                player.displayClientMessage(Component.translatable("message.enhanced.ultimate.signature_wait",
                        (st.signatureReady - now + 19) / 20), true);
                return;
            }
            st.signatureReady = now + 20L * (loaded() ? UltimateConfig.SIGNATURE_COOLDOWN_SECONDS.get() : 10);
            signature(player, form, st);
            return;
        }
        if (st != null && now < st.cooldownUntil && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.enhanced.ultimate.cooldown", cooldownSeconds(player)), true);
            return;
        }
        awaken(player, form);
    }

    /** The awakening begins: the hero rises in a column of light. */
    public static void awaken(ServerPlayer player, UltimateForm form) {
        ServerLevel level = player.serverLevel();
        State st = new State(Powers.power(player));
        State old = STATES.get(player.getUUID());
        if (old != null) {
            st.cooldownUntil = old.cooldownUntil;
        }
        st.awakenStart = level.getGameTime();
        STATES.put(player.getUUID(), st);
        PowerData data = Powers.get(player);
        data.set(Live.ULTIMATE_AWAKENING, true);
        Powers.syncIfDirty(player);
        int c = form.color(player);
        MagicFx.sendOn(level, MagicFx.AWAKEN, player, List.of(), c, form.glow(player), 1.0F, awakenTicks() + 10);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.5F);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, awakenTicks() + 5, 4, true, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, awakenTicks() + 5, 0, true, false, false));
    }

    /** The form is the hero's: its aura, its strengths, the title. */
    private static void become(ServerPlayer player, UltimateForm form, State st) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        st.awakenStart = -1;
        st.activeUntil = untimed(player) ? Long.MAX_VALUE : now + 20L * (loaded() ? UltimateConfig.DURATION_SECONDS.get() : 45);
        st.signatureReady = now + 20;
        PowerData data = Powers.get(player);
        data.set(Live.ULTIMATE_AWAKENING, false);
        data.set(Live.ULTIMATE, true);
        if (form == UltimateForm.VISION) {
            // the jewel wakes full of light
            data.setIronCharge(loaded() ? SolarJewelConfig.MAX_CHARGE.get() : 1000);
        }
        Powers.syncIfDirty(player);
        strengths(player, form);
        int c = form.color(player);
        MagicFx.send(level, MagicFx.RING, player.position(), List.of(), c, form.glow(player), 9.0F, 24);
        MagicFx.send(level, MagicFx.STARBURST, player.position().add(0, 1, 0), List.of(), c, form.glow(player), 4.0F, 16);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 0.6F);
        player.displayClientMessage(Component.translatable("message.enhanced.ultimate.awakened", form.displayName(),
                form.signatureName()).withStyle(s -> s.withColor(c).withBold(true)), true);
    }

    /** The form's strengths, kept up while it lasts (short, so they fade within seconds of its end). */
    private static void strengths(ServerPlayer player, UltimateForm form) {
        for (UltimateForm.Effect e : form.effects) {
            player.addEffect(new MobEffectInstance(e.effect(), 100, e.amplifier(), true, false, true));
        }
    }

    /** Ends the form (its time is up, the power changed, the mode went off). */
    public static void end(ServerPlayer player, boolean cooldown) {
        State st = STATES.get(player.getUUID());
        if (st == null) {
            return;
        }
        long now = player.level().getGameTime();
        boolean was = st.awakenStart >= 0 || now < st.activeUntil;
        st.awakenStart = -1;
        st.activeUntil = 0;
        st.stormUntil = 0;
        st.vortexAt = 0;
        if (cooldown && was && !untimed(player)) {
            st.cooldownUntil = now + 20L * (loaded() ? UltimateConfig.COOLDOWN_SECONDS.get() : 180);
        }
        NexusBeing.release(player);
        PowerData data = Powers.get(player);
        if (data.is(Live.ULTIMATE) || data.is(Live.ULTIMATE_AWAKENING)) {
            data.set(Live.ULTIMATE, false);
            data.set(Live.ULTIMATE_AWAKENING, false);
            Powers.syncIfDirty(player);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.enhanced.ultimate.ended").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Every tick of every player. */
    public static void tick(ServerPlayer player) {
        State st = STATES.get(player.getUUID());
        PowerData data = Powers.get(player);
        if (st == null) {
            if (data.is(Live.ULTIMATE) || data.is(Live.ULTIMATE_AWAKENING)) {
                data.set(Live.ULTIMATE, false); // (the flag outlived the record: a restart)
                data.set(Live.ULTIMATE_AWAKENING, false);
            }
            return;
        }
        long now = player.level().getGameTime();
        boolean live = st.awakenStart >= 0 || now < st.activeUntil;
        if (!live) {
            if (now >= st.cooldownUntil) {
                STATES.remove(player.getUUID());
            }
            if (data.is(Live.ULTIMATE)) {
                end(player, false);
            }
            return;
        }
        UltimateForm form = UltimateForm.of(player);
        if (!Ultimate.on() || form == null || Powers.power(player) != st.power || !player.isAlive()) {
            end(player, false);
            return;
        }
        if (st.awakenStart >= 0) {
            long age = now - st.awakenStart;
            // risen a little off the ground, turning slowly
            if (age < awakenTicks() / 2) {
                player.setDeltaMovement(player.getDeltaMovement().multiply(0.6, 0.0, 0.6).add(0, 0.06, 0));
                player.hurtMarked = true;
            }
            if (age >= awakenTicks()) {
                become(player, form, st);
            }
            return;
        }
        if (now >= st.activeUntil) {
            end(player, true);
            return;
        }
        if (now % 40 == 0) {
            strengths(player, form);
        }
        if (form == UltimateForm.SCARLET_WITCH) {
            NexusBeing.tick(player);
        }
        if (st.stormUntil > now && (st.stormUntil - now) % 10 == 0) {
            strike(player, form, 6, 0.8F);
        }
        if (st.vortexAt > 0 && now >= st.vortexAt) {
            st.vortexAt = 0;
            throwOut(player, form, st.vortexCenter);
        } else if (st.vortexAt > 0) {
            pullIn(player, st.vortexCenter);
        }
    }

    // ---- the signature moves ----

    private static double radius() {
        return loaded() ? UltimateConfig.SIGNATURE_RADIUS.get() : 10;
    }

    private static float damage(ServerPlayer player) {
        return (float) ((loaded() ? UltimateConfig.SIGNATURE_DAMAGE.get() : 14.0) * Magic.corruptionBonus(player));
    }

    private static DamageSource source(ServerPlayer player) {
        return ModDamageTypes.source(player.level(), ModDamageTypes.ULTIMATE, player, player);
    }

    /** What a signature move may strike round the hero, nearest first. */
    static List<LivingEntity> targets(ServerPlayer player, double r) {
        boolean players = !loaded() || UltimateConfig.SIGNATURE_HURTS_PLAYERS.get();
        List<LivingEntity> list = player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r),
                e -> e != player && e.distanceToSqr(player) <= r * r && Magic.enemy(player, e) && (players || !(e instanceof Player)));
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return list;
    }

    private static Vec3 middle(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0);
    }

    private static List<Vec3> feet(List<LivingEntity> list, int max) {
        List<Vec3> out = new ArrayList<>();
        for (LivingEntity e : list.subList(0, Math.min(max, list.size()))) {
            out.add(e.position());
        }
        return out;
    }

    private static void signature(ServerPlayer player, UltimateForm form, State st) {
        ServerLevel level = player.serverLevel();
        double r = radius();
        float d = damage(player);
        int c = form.color(player);
        int g = form.glow(player);
        Vec3 at = player.position();
        List<LivingEntity> near = targets(player, r);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        switch (form.signature) {
            case NOVA -> {
                MagicFx.send(level, MagicFx.RING, at, List.of(), c, g, (float) r, 20);
                for (LivingEntity e : near) {
                    e.hurt(source(player), d);
                    Shots.push(e, at, 1.6, 0.5);
                }
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.7F);
            }
            case STRIKES, BLADES -> {
                strike(player, form, 8, form.signature == UltimateForm.Signature.BLADES ? 1.0F : 1.1F);
            }
            case STORM -> {
                strike(player, form, 6, 0.8F);
                st.stormUntil = level.getGameTime() + 60;
                MagicFx.send(level, MagicFx.RING, at, List.of(), c, g, (float) r, 30);
            }
            case VORTEX -> {
                st.vortexCenter = at;
                st.vortexAt = level.getGameTime() + 20;
                MagicFx.send(level, MagicFx.VORTEX, at, List.of(), c, g, (float) r, 24);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
            case DRAIN -> {
                List<Vec3> from = new ArrayList<>();
                float taken = 0;
                for (LivingEntity e : near.subList(0, Math.min(10, near.size()))) {
                    if (e.hurt(source(player), d * 0.7F)) {
                        taken += d * 0.7F;
                    }
                    from.add(middle(e));
                }
                player.heal(Math.min(taken * 0.5F, 20.0F));
                MagicFx.send(level, MagicFx.DRAIN, player.position().add(0, 1.2, 0), from, c, g, 1.0F, 30);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.8F);
            }
            case BIND -> {
                List<Vec3> bound = new ArrayList<>();
                for (LivingEntity e : near.subList(0, Math.min(12, near.size()))) {
                    if (!e.getType().is(ModTags.SPELLBOUND_IMMUNE)) {
                        e.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, 100, 0, false, false, true), player);
                    }
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4, false, true, true), player);
                    e.hurt(source(player), d * 0.4F);
                    if (e instanceof Mob mob) {
                        mob.setTarget(null);
                    }
                    bound.add(e.position());
                }
                MagicFx.send(level, MagicFx.BIND, at, bound, c, g, 1.9F, 100);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            case BARRAGE -> {
                Vec3 from = player.position().add(0, 1, 0);
                for (LivingEntity e : near.subList(0, Math.min(10, near.size()))) {
                    e.invulnerableTime = 0;
                    e.hurt(source(player), d * 0.8F);
                    MagicFx.line(level, from, middle(e), g, 0.9F);
                    from = middle(e);
                }
                MagicFx.send(level, MagicFx.RING, at, List.of(), c, g, (float) r * 0.6F, 12);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            case SNAP -> {
                MagicFx.send(level, MagicFx.STARBURST, at.add(0, 1.4, 0), List.of(), c, g, (float) r, 24);
                boolean half = false;
                for (LivingEntity e : near) {
                    boolean boss = e.getType().is(ModTags.BOSSES) || e.getType().is(ModTags.BOSS_MINIONS);
                    if (e instanceof Enemy && !(e instanceof Player) && !boss && (half = !half)) {
                        level.sendParticles(ParticleTypes.ASH, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 40,
                                e.getBbWidth() * 0.5, e.getBbHeight() * 0.4, e.getBbWidth() * 0.5, 0.02);
                        e.hurt(source(player), Math.max(d * 4, e.getMaxHealth() * 2));
                    } else {
                        e.hurt(source(player), d);
                    }
                }
                level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0F, 0.5F);
            }
            case BEAM_RING -> {
                Vec3 eye = player.getEyePosition();
                List<Vec3> ends = new ArrayList<>();
                for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2 * i / 16;
                    ends.add(eye.add(Math.cos(a) * r, -0.6, Math.sin(a) * r));
                }
                MagicFx.send(level, MagicFx.BEAMS, eye, ends, c, g, 1.0F, 16);
                for (LivingEntity e : near) {
                    if (player.hasLineOfSight(e)) {
                        e.hurt(source(player), d);
                        e.igniteForSeconds(form == UltimateForm.VISION ? 4 : 0);
                    }
                }
                level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
            }
            case FIRE -> {
                MagicFx.send(level, MagicFx.RING, at, List.of(), c, g, (float) r, 20);
                for (LivingEntity e : near) {
                    e.igniteForSeconds(6);
                    e.hurt(source(player), d * 0.8F);
                    Shots.push(e, at, 0.8, 0.3);
                }
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1, at.z, 80, r * 0.3, 0.6, r * 0.3, 0.12);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.6F);
            }
            case REVEAL -> {
                MagicFx.send(level, MagicFx.RING, at, List.of(), c, g, (float) r * 1.5F, 26);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48), e -> e != player)) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false), player);
                }
                for (LivingEntity e : near) {
                    e.hurt(source(player), d * 0.6F);
                    Shots.push(e, at, 1.0, 0.3);
                }
                level.playSound(null, at.x, at.y, at.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.2F, 0.8F);
            }
            case BLAST -> {
                for (LivingEntity e : near.subList(0, Math.min(10, near.size()))) {
                    level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 0.5, e.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
                    level.sendParticles(ParticleTypes.FIREWORK, e.getX(), e.getY() + 0.8, e.getZ(), 20, 0.3, 0.3, 0.3, 0.2);
                    e.hurt(source(player), d);
                    Shots.push(e, player.position(), 1.0, 0.5);
                }
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.2F, 1.0F);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7F, 1.3F);
            }
        }
    }

    /** Bolts (or blades) from the sky on up to {@code max} things near. */
    private static void strike(ServerPlayer player, UltimateForm form, int max, float factor) {
        ServerLevel level = player.serverLevel();
        List<LivingEntity> near = targets(player, radius());
        List<Vec3> points = feet(near, max);
        float d = damage(player) * factor;
        for (LivingEntity e : near.subList(0, Math.min(max, near.size()))) {
            e.invulnerableTime = 0;
            e.hurt(source(player), d);
        }
        if (!points.isEmpty()) {
            MagicFx.send(level, MagicFx.BOLTS, player.position(), points, form.color(player), form.glow(player), 1.0F, 12);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), form.signature == UltimateForm.Signature.BLADES
                ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private static void pullIn(ServerPlayer player, Vec3 center) {
        for (LivingEntity e : targets(player, radius())) {
            Vec3 to = center.subtract(e.position());
            if (to.lengthSqr() > 1.0) {
                e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(to.normalize().scale(0.35)).add(0, 0.04, 0));
                e.hurtMarked = true;
            }
        }
    }

    private static void throwOut(ServerPlayer player, UltimateForm form, Vec3 center) {
        ServerLevel level = player.serverLevel();
        float d = damage(player);
        for (LivingEntity e : targets(player, radius())) {
            e.hurt(source(player), d);
            Shots.push(e, center, 2.0, 0.7);
        }
        MagicFx.send(level, MagicFx.RING, center, List.of(), form.color(player), form.glow(player), (float) radius(), 18);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 0.9F);
    }

    // ---- damage ----

    /**
     * How many times as much damage this attacker does now: its form (a quarter more by default); the Demiurge's
     * abilities always hit {@link Demiurge#DAMAGE_FACTOR} times as hard; the Nexus Being's twice, and a quarter more for
     * each hero her runes strip.
     */
    public static float damageFactor(LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayer player)) {
            return 1.0F;
        }
        float factor = 1.0F;
        if (active(player)) {
            factor += (float) (loaded() ? UltimateConfig.DAMAGE_BONUS.get() : 0.25);
        }
        if (com.kolodkingal.enhanced.magic.Magic.isDemiurge(player)) {
            factor *= Demiurge.DAMAGE_FACTOR;
        }
        if (NexusBeing.is(player) && active(player)) {
            factor *= 2.0F + 0.25F * Math.min(8, NexusBeing.stripped(player));
        }
        return factor;
    }

    /** Whether this player's form has no time limit and no cooldown (the Nexus Being's: until she ends it). */
    static boolean untimed(Player player) {
        return Powers.power(player) == Power.SCARLET_WITCH;
    }

    // ---- lifecycle ----

    public static void forget(ServerPlayer player) {
        end(player, false);
        STATES.remove(player.getUUID());
    }

    /** Ultimate Mode was turned off: every form ends. */
    public static void endEveryone(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            end(player, false);
        }
    }

    public static void clear() {
        STATES.clear();
        NexusBeing.clear();
    }
}
