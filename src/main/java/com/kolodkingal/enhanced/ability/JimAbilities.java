package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.JimConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Jim Hammond (3.17), the android first Human Torch, as the designer specified him (and nothing more):
 * <ol>
 *   <li>R, the Fire Beam ({@link ChargedBeam}: tap fires, hold charges; it burns what it hits and the ground where it
 *   strikes; charged enough it turns the ground into magma);</li>
 *   <li>total immunity to fire ({@link #FIRE_IMMUNITY});</li>
 *   <li>a body almost as durable as Vision's, healing five times as fast ({@link #BODY});</li>
 *   <li>V, fireballs ({@link JimFireballs});</li>
 *   <li>B, the Nova Flame: he gathers his energy (hold B), the blast goes off when it is full, then a long cooldown;</li>
 *   <li>his whole body burns while he flies, and whoever strikes him then is set alight ({@link JimEvents}).</li>
 * </ol>
 * G is flight (a flame form the client draws), N the human disguise (the look, android and human).
 */
public final class JimAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("jim_body_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("jim_body_armor");
    /** The Nova's blast stays on the clients' screens this long. */
    public static final int NOVA_BLAST_TICKS = 16;

    /** When each player began gathering a Nova Flame, and when each blast went off. */
    private static final Map<UUID, Long> NOVA_START = new HashMap<>();
    private static final Map<UUID, Long> NOVA_FIRED = new HashMap<>();
    private static final Map<UUID, Long> LAST_HEAL = new HashMap<>();

    private JimAbilities() {
    }

    /** Jim's Fire Beam: the shared charged beam with his numbers, his fire and his magma. */
    public static final ChargedBeam.Profile BEAM = new ChargedBeam.Profile(JimConfig.BEAM_CHARGE_TICKS::get,
            JimConfig.BEAM_TAP_TICKS::get, JimConfig.BEAM_COOLDOWN::get, JimConfig.BEAM_MIN_DAMAGE::get,
            JimConfig.BEAM_MAX_DAMAGE::get, JimConfig.BEAM_MIN_RANGE::get, JimConfig.BEAM_MAX_RANGE::get,
            JimConfig.BEAM_MAX_WIDTH::get, ModDamageTypes.SOLAR_BEAM,
            c -> Math.round(JimConfig.BEAM_BURN_SECONDS.get() * (1.0F + (float) c)), JimAbilities::beamImpact);

    // ---- keys ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        switch (ability) {
            case 1 -> ChargedBeam.onKey(player, data, down, BEAM, HeroAbility.JIM_BEAM);
            case 2 -> {
                if (down) {
                    PowerTicker.toggleFlight(player);
                }
            }
            case 3 -> {
                if (down) {
                    JimFireballs.launch(player, data);
                }
            }
            case 4 -> {
                if (down) {
                    startNova(player, data);
                } else {
                    cancelNova(player, data);
                }
            }
            case 5 -> {
                if (down) {
                    disguise(player, data);
                }
            }
            default -> {
            }
        }
    }

    /** N: the human disguise: android to human and back (the look is drawn by the client from {@link PowerData#disguise}). */
    private static void disguise(ServerPlayer player, PowerData data) {
        data.setDisguise(!data.disguise());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        player.serverLevel().sendParticles(new EnergyParticleOptions(0xFF8A2A, 1.4F), player.getX(), player.getY() + 1, player.getZ(),
                24, 0.3, 0.8, 0.3, 0.02);
        player.displayClientMessage(Component.translatable(data.disguise() ? "message.enhanced.disguise.on"
                : "message.enhanced.disguise.off"), true);
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        if (data.has(HeroAbility.JIM_BEAM)) {
            ChargedBeam.tick(player, data, BEAM, HeroAbility.JIM_BEAM);
        }
        if (data.has(HeroAbility.JIM_FIREBALL)) {
            JimFireballs.tick(player, data);
        }
        if (data.has(HeroAbility.JIM_NOVA)) {
            novaTick(player, data);
        }
        if (data.has(HeroAbility.JIM_FIRE_IMMUNITY)) {
            fireImmunity(player);
        }
        if (data.has(HeroAbility.JIM_BODY)) {
            regenerate(player);
        }
        boolean flames = flaming(player, data);
        if (data.is(Live.JIM_FLAMING) != flames) {
            data.set(Live.JIM_FLAMING, flames);
        }
    }

    /** Whether his whole body is burning now: he flies (in a look that burns: not the human disguise). */
    public static boolean flaming(ServerPlayer player, PowerData data) {
        return data.has(HeroAbility.JIM_FLAME_BODY) && JimConfig.FLAMES_WHILE_FLYING.get() && !player.isSpectator()
                && player.getAbilities().flying && !data.disguise() && !"human".equals(data.variant());
    }

    // ---- the body ----

    /** Total immunity to fire: burning, lava, magma, fire-type attacks. */
    private static void fireImmunity(ServerPlayer player) {
        player.clearFire();
        MobEffectInstance effect = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (effect == null || effect.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0, true, false, false));
        }
    }

    /** Healing five times as fast as a normal player (who heals one health in four seconds). */
    private static void regenerate(ServerPlayer player) {
        if (player.isDeadOrDying() || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        long now = player.level().getGameTime();
        int every = Math.max(1, Math.round(80.0F / JimConfig.REGEN_MULTIPLIER.get().floatValue()));
        Long last = LAST_HEAL.get(player.getUUID());
        if (last == null || now - last >= every) {
            LAST_HEAL.put(player.getUUID(), now);
            player.heal(1.0F);
        }
    }

    static void applyBody(ServerPlayer player, PowerData data) {
        boolean body = data.has(HeroAbility.JIM_BODY);
        float scale = data.bodyScale(HeroAbility.JIM_BODY);
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, JimConfig.HEALTH_BONUS.get() * scale, body);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, JimConfig.ARMOR_BONUS.get() * scale, body);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ---- the Fire Beam's mark on the world ----

    private static void beamImpact(ServerPlayer player, ServerLevel level, HitResult hit, float charge) {
        if (JimConfig.BEAM_FIRE.get()) {
            BeamFire.strike(player, level, hit, 0, 2);
        }
        if (JimConfig.MAGMA.get() && charge >= JimConfig.MAGMA_MIN_CHARGE.get() && hit instanceof BlockHitResult block
                && hit.getType() == HitResult.Type.BLOCK) {
            magma(player, level, block.getBlockPos());
        }
    }

    /** A charged beam turns the ground it strikes into magma: the struck block and the ones round it on its layer. */
    static int magma(ServerPlayer player, ServerLevel level, BlockPos centre) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return 0;
        }
        int r = JimConfig.MAGMA_RADIUS.get();
        int made = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + r / 2) {
                    continue;
                }
                BlockPos pos = centre.offset(dx, 0, dz);
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);
                if (state.isAir() || state.is(Blocks.MAGMA_BLOCK) || hardness < 0.0F || hardness > 8.0F || !state.getFluidState().isEmpty()
                        || level.getBlockEntity(pos) != null || !state.isCollisionShapeFullBlock(level, pos)) {
                    continue;
                }
                if (BlockRights.mayBlast(player, level, pos)) {
                    level.setBlockAndUpdate(pos, Blocks.MAGMA_BLOCK.defaultBlockState());
                    level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.0);
                    made++;
                }
            }
        }
        return made;
    }

    // ---- B: the Nova Flame ----

    public static boolean gatheringNova(ServerPlayer player) {
        return NOVA_START.containsKey(player.getUUID());
    }

    private static void startNova(ServerPlayer player, PowerData data) {
        if (gatheringNova(player) || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        long until = data.cooldownUntil(data.slot(HeroAbility.JIM_NOVA));
        if (until > now && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.enhanced.jim.nova_cooldown", (until - now + 19) / 20), true);
            return;
        }
        data.setBeamActive(false);
        NOVA_START.put(player.getUUID(), now);
        data.set(Live.JIM_NOVA_CHARGING, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    /** He let go before it was full: the energy is lost, and he must wait a little. */
    private static void cancelNova(ServerPlayer player, PowerData data) {
        if (NOVA_START.remove(player.getUUID()) != null) {
            data.set(Live.JIM_NOVA_CHARGING, false);
            data.startCooldown(HeroAbility.JIM_NOVA, player.level().getGameTime(), JimConfig.NOVA_CANCEL_COOLDOWN.get());
            player.displayClientMessage(Component.translatable("message.enhanced.jim.nova_lost"), true);
        }
    }

    private static void novaTick(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long start = NOVA_START.get(player.getUUID());
        if (start != null) {
            long held = now - start;
            int full = JimConfig.NOVA_CHARGE_TICKS.get();
            if (!data.is(Live.JIM_NOVA_CHARGING) || !player.isAlive() || player.isSpectator()) {
                NOVA_START.remove(player.getUUID());
                data.set(Live.JIM_NOVA_CHARGING, false);
            } else if (held >= full) {
                NOVA_START.remove(player.getUUID());
                data.set(Live.JIM_NOVA_CHARGING, false);
                novaBlast(player, data);
            } else {
                float t = held / (float) full;
                if (held % 5 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                            1.0F, 0.6F + 1.4F * t);
                }
                // he braces: slowed, the heat gathering round him
                player.setDeltaMovement(player.getDeltaMovement().multiply(0.5, 0.5, 0.5));
                player.hurtMarked = true;
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 2 + (int) (6 * t), 0.5, 0.8, 0.5, 0.02);
            }
        } else if (data.is(Live.JIM_NOVA_CHARGING)) {
            data.set(Live.JIM_NOVA_CHARGING, false);
        }
        Long fired = NOVA_FIRED.get(player.getUUID());
        if (fired != null && now - fired >= NOVA_BLAST_TICKS) {
            NOVA_FIRED.remove(player.getUUID());
            data.set(Live.JIM_NOVA_BLAST, false);
        } else if (fired == null && data.is(Live.JIM_NOVA_BLAST)) {
            data.set(Live.JIM_NOVA_BLAST, false);
        }
    }

    /** The blast: a great fiery shell round him that hurts, burns and throws back what it reaches, and fires the ground. */
    static void novaBlast(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Vec3 centre = player.position().add(0, player.getBbHeight() * 0.5, 0);
        double radius = JimConfig.NOVA_RADIUS.get();
        float damage = JimConfig.NOVA_DAMAGE.get().floatValue();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(radius),
                e -> e != player && e.isAlive() && e.getBoundingBox().distanceToSqr(centre) <= radius * radius)) {
            if (!AreaTargets.mayHurt(player, e, true)) {
                continue;
            }
            double d = Math.sqrt(e.getBoundingBox().distanceToSqr(centre));
            float scaled = (float) (damage * (1.0 - 0.5 * d / radius));
            e.hurt(ModDamageTypes.source(level, ModDamageTypes.SOLAR_BEAM, player, player), scaled);
            BeamFire.ignite(e, JimConfig.NOVA_BURN_SECONDS.get());
            Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1.0E-4) {
                away = away.normalize().scale(JimConfig.NOVA_PUSH.get() * (1.0 - 0.5 * d / radius));
                e.push(away.x, 0.35, away.z);
                e.hurtMarked = true;
            }
        }
        if (JimConfig.NOVA_FIRE.get()) {
            int placed = 0;
            for (int i = 0; i < 80 && placed < 30; i++) {
                double angle = level.random.nextDouble() * Mth.TWO_PI;
                double dist = 2.0 + Math.sqrt(level.random.nextDouble()) * (radius - 2.0);
                BlockPos column = BlockPos.containing(centre.x + Math.cos(angle) * dist, centre.y, centre.z + Math.sin(angle) * dist);
                for (int dy = 3; dy >= -5; dy--) {
                    BlockPos floor = column.offset(0, dy, 0);
                    if (!level.getBlockState(floor).isAir() && level.getBlockState(floor).getFluidState().isEmpty()) {
                        if (BeamFire.place(player, level, floor.above())) {
                            placed++;
                        }
                        break;
                    }
                }
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FLAME, centre.x, centre.y, centre.z, 160, radius * 0.35, 0.6, radius * 0.35, 0.18);
        level.sendParticles(ParticleTypes.LAVA, centre.x, centre.y, centre.z, 40, radius * 0.3, 0.5, radius * 0.3, 0.0);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0F, 0.5F);
        data.set(Live.JIM_NOVA_BLAST, true);
        NOVA_FIRED.put(player.getUUID(), now);
        data.startCooldown(HeroAbility.JIM_NOVA, now, JimConfig.NOVA_COOLDOWN.get());
    }

    // ---- ending ----

    static void endAll(ServerPlayer player, PowerData data) {
        NOVA_START.remove(player.getUUID());
        NOVA_FIRED.remove(player.getUUID());
        LAST_HEAL.remove(player.getUUID());
        JimFireballs.forget(player.getUUID());
        data.set(Live.JIM_NOVA_CHARGING, false);
        data.set(Live.JIM_NOVA_BLAST, false);
        data.set(Live.JIM_FLAMING, false);
        data.set(Live.BEAM_CHARGING, false);
        data.set(Live.BEAM_FIRING, false);
        // the fire resistance he kept up (quiet, a few seconds) goes with the power
        MobEffectInstance kept = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (kept != null && kept.isAmbient() && !kept.isVisible() && kept.getDuration() <= 100) {
            player.removeEffect(MobEffects.FIRE_RESISTANCE);
        }
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, 0, false);
        PowerTicker.setModifier(player, Attributes.ARMOR, BODY_ARMOR, 0, false);
    }

    static void forget(UUID player) {
        NOVA_START.remove(player);
        NOVA_FIRED.remove(player);
        LAST_HEAL.remove(player);
        JimFireballs.forget(player);
    }
}
