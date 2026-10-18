package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.custom.AbilityColors;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.entity.HexVisionEntity;
import com.kolodkingal.enhanced.entity.IronShrapnelEntity;
import com.kolodkingal.enhanced.entity.LokiDaggerEntity;
import com.kolodkingal.enhanced.entity.MjolnirFragmentEntity;
import com.kolodkingal.enhanced.entity.ThrownWeaponEntity;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.protect.DarkMagic;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.Predicate;

/** Wanda Maximoff, the Scarlet Witch. */
public final class WandaAbilities {
    public static final double SHIELD_RADIUS = 1.9;

    private static final ResourceLocation CROWN_HEALTH = Enhanced.id("wanda_crown_health");
    private static final ResourceLocation CROWN_ARMOR = Enhanced.id("wanda_crown_armor");
    private static final ResourceLocation CROWN_TOUGHNESS = Enhanced.id("wanda_crown_toughness");

    /** What the Chaos Beam can hit: creatures, Ender Dragon parts and End Crystals. */
    public static final Predicate<Entity> BEAM_TARGET = e -> e.isPickable() && !e.isSpectator()
            && (e instanceof LivingEntity || e instanceof PartEntity<?> || e instanceof EndCrystal);

    /** What Wanda's Chaos Beam hits: {@link #BEAM_TARGET}, but never the Vision her own Hex raised (he is on her side). */
    public static Predicate<Entity> beamTargets(Player wanda) {
        return e -> BEAM_TARGET.test(e) && !(e instanceof HexVisionEntity vision && wanda.getUUID().equals(vision.getOwnerUUID()));
    }

    // ---- input ----

    /**
     * The key of old, which reads Sneak from the player (the Scarlet Witch herself: {@code AbilityHandler}).
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of hers (3.4: also one of a custom hero's bindings, {@code HeroKeys.press}, with the key the ability has for
     * her). {@code alt}: its Sneak variant (only the Hex has one: Sneak + N dissolves it).
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        switch (ability) {
            case 1 -> {
                boolean start = down && !player.isSpectator();
                if (start && !data.beamActive()) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CHAOS_BEAM_START.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                data.setBeamActive(start);
            }
            case 2 -> {
                if (down) {
                    PowerTicker.toggleFlight(player);
                }
            }
            case 3 -> {
                if (down) {
                    Telekinesis.press(player, Telekinesis.Mode.WANDA);
                }
            }
            case 4 -> {
                if (down) {
                    raiseShield(player, data);
                } else if (data.chaosShieldActive()) {
                    dropShield(player, data, false);
                }
            }
            case 5 -> {
                if (down) {
                    HexManager.onHexKey(player, alt);
                }
            }
            default -> {
            }
        }
    }

    /** Her upkeep (3.4: a custom hero's too, of the abilities of hers he has: the beam flag and the shield are shared fields). */
    static void tick(ServerPlayer player, PowerData data) {
        if (data.beamActive() && data.has(HeroAbility.WANDA_CHAOS_BEAM)) {
            beamTick(player);
        }
        if (data.chaosShieldActive() && data.has(HeroAbility.WANDA_CHAOS_SHIELD)) {
            shieldTick(player, data);
        }
    }

    /**
     * 3.4: the abilities of hers a custom hero had end (he lost them, was redefined, died or left): the beam goes out and the
     * shield comes down. (Her Hex goes with the Hex ability: {@code Powers.setPower}, {@link HexManager#tickLevel}.)
     */
    static void endAll(ServerPlayer player, PowerData data) {
        if (data.power() != Power.CUSTOM) {
            return; // (the Scarlet Witch losing her power: Powers.setPower, as before)
        }
        if (data.has(HeroAbility.WANDA_CHAOS_BEAM)) {
            data.setBeamActive(false);
        }
        if (data.has(HeroAbility.WANDA_CHAOS_SHIELD)) {
            data.setChaosShieldActive(false);
        }
        if (data.has(HeroAbility.WANDA_BUILD_MAGIC)) {
            // 3.5: a custom hero's Build Magic of hers ends with it (as Billy's does in BillyAbilities.endAll).
            BuildMagic.onPowerChanged(player, Power.SCARLET_WITCH, Power.CUSTOM);
        }
    }

    /**
     * The colour of her magic for this player: the Scarlet Witch's red, or (3.4) a custom hero's own colour ({@link
     * Powers#color}); {@link #glow} likewise.
     */
    public static int color(Player player) {
        return Powers.power(player) == Power.SCARLET_WITCH ? Power.SCARLET_WITCH.color : Powers.color(player);
    }

    /** The bright colour of her magic for this player (see {@link #color}). */
    public static int glow(Player player) {
        return Powers.power(player) == Power.SCARLET_WITCH ? Power.SCARLET_WITCH.glow : Powers.glow(player);
    }

    // ---- Wanda's Crown ----

    public static boolean wearsCrown(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WANDA_CROWN.get());
    }

    /** While the Scarlet Witch wears her crown: 40 max health and the protection of a full Diamond set. */
    static void applyCrown(ServerPlayer player, PowerData data) {
        boolean transformed = data.power() == Power.SCARLET_WITCH && wearsCrown(player);
        PowerTicker.setModifier(player, Attributes.MAX_HEALTH, CROWN_HEALTH, 20.0, transformed);
        PowerTicker.setModifier(player, Attributes.ARMOR, CROWN_ARMOR, 20.0, transformed);
        PowerTicker.setModifier(player, Attributes.ARMOR_TOUGHNESS, CROWN_TOUGHNESS, 8.0, transformed);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ---- Ability 1: Chaos Energy Beam ----

    private static void beamTick(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        HitResult hit = Raycasts.any(player, EnhancedConfig.CHAOS_BEAM_RANGE.get(), beamTargets(player), 1.0F);
        // 3.3: a wall in the way (another's Chaos Shield, Billy's barrier, a ward is no matter to her) ends the beam there; chaos
        // magic wears a Hex Barrier down (3.3.1: by its damage, no longer at once).
        ShieldStop shield = shieldAcross(level, player, player.getEyePosition(), hit.getLocation(), 1.0F);
        if (shield != null) {
            if (player.tickCount % 10 == 0) {
                pay(shield, player, EnhancedConfig.CHAOS_BEAM_DAMAGE.get().floatValue());
            }
            Vec3 at = shield.point();
            if (player.tickCount % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(shield.color(), 1.4F), at.x, at.y, at.z, 4, 0.12, 0.12, 0.12, 0.12);
            }
            return;
        }
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            target.hurt(ModDamageTypes.source(level, ModDamageTypes.CHAOS_BEAM, player, player),
                    EnhancedConfig.CHAOS_BEAM_DAMAGE.get().floatValue());
            LivingEntity living = target instanceof LivingEntity l ? l
                    : target instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity l ? l : null;
            int nausea = EnhancedConfig.CHAOS_BEAM_NAUSEA_TICKS.get();
            if (living != null && living.isAlive() && nausea > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nausea, 0), player);
            }
        }
        Vec3 end = hit.getLocation();
        if (player.tickCount % 2 == 0) {
            level.sendParticles(new EnergyParticleOptions(glow(player), 1.4F), end.x, end.y, end.z, 4, 0.12, 0.12, 0.12, 0.12);
        }
        if (hit.getType() == HitResult.Type.BLOCK && player.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    // ---- Ability 4: Chaos Shield ----

    /** What a projectile costs the shield when nothing better is known (a snowball, a potion, a shulker bullet). */
    private static final float DEFAULT_PROJECTILE_POWER = 2.0F;
    /** How many red shards fly out when the shield shatters. */
    private static final int SHATTER_SHARDS = 56;

    /**
     * 3.2: whether the Chaos Shield is unlimited ({@code scarlet_witch.shieldUnlimited}): no hunger, no strength to wear
     * down, no time limit, no recharge. (Before the config loads, on a client that has not joined yet, it is the default.)
     */
    public static boolean shieldUnlimited() {
        return !EnhancedConfig.SPEC.isLoaded() || EnhancedConfig.WANDA_SHIELD_UNLIMITED.get();
    }

    /** Whether the Chaos Shield can be held: it costs hunger, so a hungry Scarlet Witch is refused (the client's animation asks too). */
    public static boolean canAffordShield(Player player) {
        return shieldUnlimited() || player.getAbilities().invulnerable || player.getFoodData().getFoodLevel() > 0;
    }

    /** The shield is held and complete: only then does it stop anything (it takes time to form, and wears out). */
    public static boolean hasShieldUp(Player player) {
        PowerData data = Powers.get(player);
        return data.has(HeroAbility.WANDA_CHAOS_SHIELD) && data.chaosShieldUp(player.level().getGameTime());
    }

    /**
     * The strength a fresh Chaos Shield has, in points (this is also the most a client can show); infinite while it is
     * {@linkplain #shieldUnlimited unlimited}.
     */
    public static float maxShieldStrength() {
        if (shieldUnlimited()) {
            return Float.POSITIVE_INFINITY;
        }
        return configuredShieldStrength();
    }

    /** The strength a fresh shield is given (it is never worn down while the shield is unlimited, but the rules can be switched back). */
    private static float configuredShieldStrength() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.CHAOS_SHIELD_STRENGTH.get().floatValue() : 30.0F;
    }

    /** How long a Chaos Shield takes to form after B is pressed, in ticks (what a client expects of its own press). */
    public static int windUpTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get() : 15;
    }

    /** How long a complete Chaos Shield can be held, in ticks ({@link Integer#MAX_VALUE} while it is unlimited). */
    public static int maxShieldTicks() {
        if (shieldUnlimited()) {
            return Integer.MAX_VALUE;
        }
        return EnhancedConfig.SPEC.isLoaded() ? EnhancedConfig.CHAOS_SHIELD_MAX_TICKS.get() : 160;
    }

    /** B pressed: the shield starts to form, if she is hungry enough and it has recharged. */
    private static void raiseShield(ServerPlayer player, PowerData data) {
        if (player.isSpectator() || data.chaosShieldActive()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.WANDA_CHAOS_SHIELD, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.recharging",
                    String.format(Locale.ROOT, "%.1f", (data.cooldownUntil(data.slot(HeroAbility.WANDA_CHAOS_SHIELD)) - now) / 20.0)), true);
            return;
        }
        if (!canAffordShield(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.hungry"), true);
            return;
        }
        data.raiseChaosShield(now, EnhancedConfig.CHAOS_SHIELD_WIND_UP_TICKS.get(), configuredShieldStrength());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    /**
     * The shield comes down (B released, hunger, or the time limit) and has to recharge: the short cooldown, or the
     * long one if it was {@code worn} out (held to its limit; a shattered shield is handled by {@link #shatter}).
     */
    private static void dropShield(ServerPlayer player, PowerData data, boolean worn) {
        data.setChaosShieldActive(false);
        if (shieldUnlimited()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.7F);
            return; // nothing was spent, so nothing has to recharge
        }
        data.startCooldown(HeroAbility.WANDA_CHAOS_SHIELD, player.level().getGameTime(),
                (worn ? EnhancedConfig.CHAOS_SHIELD_BROKEN_COOLDOWN : EnhancedConfig.CHAOS_SHIELD_COOLDOWN).get());
        if (worn) {
            ServerLevel level = player.serverLevel();
            level.sendParticles(new EnergyParticleOptions(glow(player), 1.2F), player.getX(), player.getY() + player.getBbHeight() * 0.5,
                    player.getZ(), 24, SHIELD_RADIUS * 0.5, 0.7, SHIELD_RADIUS * 0.5, 0.05);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.7F);
    }

    /** The shield took all it could: a burst of red shards and a glassy sound, and a long wait before the next one. */
    private static void shatter(ServerPlayer player, PowerData data) {
        data.setChaosShieldActive(false);
        ServerLevel level = player.serverLevel();
        data.startCooldown(HeroAbility.WANDA_CHAOS_SHIELD, level.getGameTime(), EnhancedConfig.CHAOS_SHIELD_BROKEN_COOLDOWN.get());
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        RandomSource random = player.getRandom();
        // Chips of bright red. (A red stained glass block's own particles are see-through and a dull grey-violet in the
        // air, and every block's are darkened; the red dye's chips stay a clear red and fly the way they are sent.)
        ItemParticleOption shard = new ItemParticleOption(ParticleTypes.ITEM,
                new ItemStack(AbilityColors.dye(player, HeroAbility.WANDA_CHAOS_SHIELD, Items.RED_DYE)));
        for (int i = 0; i < SHATTER_SHARDS; i++) {
            // Out from a random spot on the sphere, along the way out.
            Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3 at = center.add(out.scale(SHIELD_RADIUS));
            level.sendParticles(shard, at.x, at.y, at.z, 0, out.x, out.y, out.z, 0.3);
            if (i % 2 == 0) {
                level.sendParticles(new EnergyParticleOptions(i % 4 == 0
                        ? AbilityColors.glow(player, HeroAbility.WANDA_CHAOS_SHIELD, glow(player))
                                : AbilityColors.color(player, HeroAbility.WANDA_CHAOS_SHIELD, color(player)), 1.5F),
                        at.x, at.y, at.z, 0, out.x, out.y, out.z, 0.3);
            }
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 1.2F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.3F);
        player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.shattered").withStyle(ChatFormatting.RED), true);
    }

    private static void shieldTick(ServerPlayer player, PowerData data) {
        if (player.isSpectator()) {
            data.setChaosShieldActive(false);
            return;
        }
        boolean unlimited = shieldUnlimited();
        if (!unlimited && !canAffordShield(player)) {
            dropShield(player, data, false);
            player.displayClientMessage(Component.translatable("message.enhanced.chaos_shield.hungry"), true);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!unlimited) {
            if (now >= data.shieldFormedAt() + EnhancedConfig.CHAOS_SHIELD_MAX_TICKS.get()) {
                dropShield(player, data, true);
                return;
            }
            player.causeFoodExhaustion(EnhancedConfig.CHAOS_SHIELD_EXHAUSTION.get().floatValue());
        }

        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        if (!data.chaosShieldUp(now)) {
            // Still forming: motes gather onto the growing sphere. It stops nothing yet.
            float grown = data.chaosShieldProgress(now);
            RandomSource random = player.getRandom();
            for (int i = 0; i < 2; i++) {
                Vec3 out = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
                Vec3 at = center.add(out.scale(SHIELD_RADIUS * Math.max(0.3, grown)));
                level.sendParticles(new EnergyParticleOptions(glow(player), 1.0F), at.x, at.y, at.z, 0, -out.x, -out.y, -out.z, 0.1);
            }
            return;
        }
        if (now == data.shieldFormedAt()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.5F);
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(SHIELD_RADIUS + 4),
                p -> p.getOwner() != player && !isHeadingHome(p))) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 toPlayer = center.subtract(projectile.position());
            double reach = SHIELD_RADIUS + 0.5 + velocity.length();
            if (velocity.dot(toPlayer) > 0 && toPlayer.lengthSqr() < reach * reach) {
                block(player, projectile);
                if (!data.chaosShieldActive()) {
                    return; // shattered: the rest get through
                }
            }
        }
    }

    /**
     * What a projectile would have done to her, in the strength it takes out of the shield: what it says itself if it
     * is a {@link ShieldCost} (the projectiles of the villains and of the heroes of 3.0), an arrow's damage at its
     * speed, a trident's 8, a fireball's 6, a thrown hammer's, a hammer fragment's, a dagger's or a shard's own damage,
     * and 2 for the rest.
     */
    public static float projectilePower(Projectile projectile) {
        if (projectile instanceof ShieldCost cost) {
            return cost.shieldCost();
        }
        if (projectile instanceof ThrownTrident) {
            return 8.0F;
        }
        if (projectile instanceof AbstractArrow arrow) {
            return (float) Math.ceil(arrow.getDeltaMovement().length() * arrow.getBaseDamage());
        }
        if (projectile instanceof AbstractHurtingProjectile) {
            return 6.0F;
        }
        if (projectile instanceof ThrownWeaponEntity weapon) {
            return (ThorAbilities.isStormbreaker(weapon.weapon()) ? EnhancedConfig.STORMBREAKER_THROW_DAMAGE
                    : EnhancedConfig.MJOLNIR_THROW_DAMAGE).get().floatValue();
        }
        if (projectile instanceof MjolnirFragmentEntity) {
            return EnhancedConfig.MJOLNIR_SHATTER_DAMAGE.get().floatValue();
        }
        if (projectile instanceof LokiDaggerEntity) {
            return EnhancedConfig.LOKI_DAGGER_DAMAGE.get().floatValue();
        }
        if (projectile instanceof IronShrapnelEntity) {
            return EnhancedConfig.SHRAPNEL_DAMAGE.get().floatValue();
        }
        return DEFAULT_PROJECTILE_POWER;
    }

    /**
     * A projectile reaches the shield: it is neutralised ({@link #neutralize}), and the shield pays what it would have
     * cost her.
     */
    public static void block(ServerPlayer player, Projectile projectile) {
        float power = projectilePower(projectile);
        neutralize(player.serverLevel(), projectile, AbilityColors.color(player, HeroAbility.WANDA_CHAOS_SHIELD,
                color(player)));
        wear(player, power, projectile);
    }

    /**
     * What a shield of any hero does to a projectile that reaches it, without the cost: it turns to sparks in the
     * Scarlet Witch's red. (Mjolnir, Stormbreaker, the fragments of a shattered Mjolnir and every {@link ShieldCost}
     * that is {@linkplain ShieldCost#oneOfAKind one of a kind} are the exception: see {@link #repel}.)
     */
    public static void neutralize(ServerLevel level, Projectile projectile) {
        neutralize(level, projectile, Power.SCARLET_WITCH.color);
    }

    /** {@link #neutralize(ServerLevel, Projectile)} in the colour of another hero's shield (Apocalypse's gold). */
    public static void neutralize(ServerLevel level, Projectile projectile, int color) {
        if (projectile instanceof ThrownWeaponEntity weapon) {
            repel(level, weapon, color, () -> weapon.setReturning(true));
        } else if (projectile instanceof MjolnirFragmentEntity fragment) {
            repel(level, fragment, color, fragment::setReturning);
        } else if (projectile instanceof ShieldCost cost && cost.oneOfAKind()) {
            repel(level, projectile, color, cost::turnHome);
        } else {
            disintegrate(level, projectile, color);
        }
    }

    /**
     * A projectile already on its way home to its owner (a weapon of Thor's, Thanos's sword): the shield lets it by.
     */
    public static boolean isHeadingHome(Projectile projectile) {
        if (projectile instanceof ShieldCost cost && cost.oneOfAKind()) {
            return cost.returning();
        }
        return projectile instanceof ThrownWeaponEntity weapon ? weapon.isReturning()
                : projectile instanceof MjolnirFragmentEntity fragment && fragment.isReturning();
    }

    /**
     * Mjolnir and Stormbreaker are one of a kind (a thrown one holds the only copy of the weapon), and the fragments
     * of a shattered Mjolnir are the hammer itself: the shield turns them round and they fly home to Thor, instead of
     * being destroyed for good (a fragment shredded here would leave the hammer one piece short).
     */
    private static void repel(ServerLevel level, Projectile projectile, int color, Runnable turnHome) {
        level.sendParticles(new EnergyParticleOptions(color, 1.3F), projectile.getX(), projectile.getY(), projectile.getZ(),
                10, 0.15, 0.15, 0.15, 0.08);
        level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), ModSounds.MJOLNIR_IMPACT.get(), SoundSource.PLAYERS, 0.9F, 0.8F);
        turnHome.run();
    }

    // ---- the shield against blows ----

    /**
     * How hard a blow that reaches the shield throws its attacker back (a sword's knockback is 0.4). A creature on the
     * ground is lifted a little as well and flies about 3.5 blocks, which takes it out of the 1.9-block sphere from
     * anywhere in it.
     */
    private static final double MELEE_KNOCKBACK = 0.6;

    /**
     * Whether {@code source} is a creature's or a player's own blow, struck with a hand, claw, paw, fang or weapon:
     * what a zombie, a spider, a Ravager or a Warden does when it swipes at someone, a sword, an axe or a fist, a
     * bee's sting and Quicksilver's blitz. Whatever is thrown, shot or cast (arrows, fireballs, beams, magic and
     * potions, a Guardian's laser, a Warden's sonic boom), whatever explodes, and the world's own harm (falling,
     * starving, fire, the void, {@code /kill}) are not melee.
     *
     * <p>Which damage types are blows is the data-driven tag {@code enhanced:melee_blows} (the vanilla creature attack,
     * sting and player attack, the speed blitz, and the blows of the heroes and villains of 3.0).
     */
    public static boolean isMeleeHit(DamageSource source) {
        Entity attacker = source.getDirectEntity();
        return attacker instanceof LivingEntity && attacker == source.getEntity() && source.is(ModTags.MELEE_BLOWS);
    }

    /**
     * A creature's or player's blow lands on a complete shield: the sphere is as solid to a swung sword or a swiping
     * claw as it is to an arrow. The blow never touches Wanda (the caller cancels it) and costs the shield what it
     * would have done to her (the blow that breaks the shield is stopped too), and red sparks fly where it struck as
     * the attacker is thrown back out of the dome with a ring of sound.
     *
     * @param amount what the blow would have done to Wanda (before her armour)
     */
    public static void blockMelee(ServerPlayer player, LivingEntity attacker, float amount) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 away = attacker.position().add(0, attacker.getBbHeight() * 0.5, 0).subtract(center);
        double distance = away.length();
        // The blow struck the sphere (or the attacker, if it is inside it): sparks and a ring of sound there.
        Vec3 contact = distance < 1.0E-3 ? center : center.add(away.scale(Math.min(distance, SHIELD_RADIUS) / distance));
        level.sendParticles(new EnergyParticleOptions(glow(player), 1.4F), contact.x, contact.y, contact.z, 8, 0.15, 0.15, 0.15, 0.1);
        level.sendParticles(new EnergyParticleOptions(color(player), 1.2F), contact.x, contact.y, contact.z, 10, 0.2, 0.2, 0.2, 0.15);
        level.playSound(null, contact.x, contact.y, contact.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.9F);
        level.playSound(null, contact.x, contact.y, contact.z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8F, 1.5F);

        // Thrown straight out from her (a creature that can't be knocked back, like the Warden, stays; so does whoever
        // Mjolnir pins). One that stands right on top of her goes the way she faces.
        double flat = Math.hypot(away.x, away.z);
        Vec3 out = flat < 1.0E-3 ? Vec3.directionFromRotation(0, player.getYRot()) : new Vec3(away.x / flat, 0, away.z / flat);
        // knockback() throws away from the point it is given: from the attacker towards her.
        attacker.knockback(MELEE_KNOCKBACK, -out.x, -out.z);
        attacker.hurtMarked = true;

        wear(player, amount, attacker);
    }

    // ---- the shield against beams ----

    /**
     * A wall that a beam has run into (3.3: any of them, see {@link Shelters#across}): whose it is (the Scarlet Witch's Chaos
     * Shield, Billy's Hex Barrier; null for the wall of a Hex or a ward), where on it the beam ends, which wall it is and its
     * colour, for the sparks.
     */
    public record ShieldStop(@Nullable Player owner, Vec3 point, Shelters.Wall wall, int color) {
        /** A complete Chaos Shield of {@code owner}. */
        public ShieldStop(Player owner, Vec3 point) {
            this(owner, point, Shelters.Wall.CHAOS_SHIELD, Power.SCARLET_WITCH.glow);
        }
    }

    /**
     * The first wall that a beam from {@code from} to {@code to} runs into, or null: a complete Chaos Shield (the sphere is
     * solid to a beam as it is to an arrow; a beam that starts inside it is not stopped here, and what hits Wanda herself is
     * stopped by the damage hook), and since 3.3 Billy's raised Hex Barrier from the front, the wall of a Hex the firer may not
     * cross and a ward if the firer is a monster or a boss or the beam is dark magic ({@link Shelters#across}). Shared by the
     * server (what a beam hits) and the client (where it is drawn to). A caller pays the wall with {@link #pay}.
     *
     * @param firer the one who fires the beam (never its own shield)
     */
    @Nullable
    public static ShieldStop shieldAcross(Level level, @Nullable Entity firer, Vec3 from, Vec3 to, float partialTick) {
        return shieldAcross(level, firer, from, to, partialTick, DarkMagic.casts(firer));
    }

    /** {@link #shieldAcross(Level, Entity, Vec3, Vec3, float)} for a beam that is (or is not) dark magic, whoever fires it. */
    @Nullable
    public static ShieldStop shieldAcross(Level level, @Nullable Entity firer, Vec3 from, Vec3 to, float partialTick, boolean dark) {
        Shelters.Stop stop = Shelters.across(level, firer, from, to, partialTick, dark);
        return stop == null ? null : new ShieldStop(stop.owner(), stop.point(), stop.wall(), stop.color());
    }

    /** The first complete Chaos Shield (only) that a beam from {@code from} to {@code to} runs into from outside it, or null. */
    @Nullable
    public static ShieldStop chaosShieldAcross(Level level, @Nullable Entity firer, Vec3 from, Vec3 to, float partialTick) {
        Vec3 path = to.subtract(from);
        double length = path.length();
        if (length < 1.0E-6) {
            return null;
        }
        Vec3 direction = path.scale(1.0 / length);
        ShieldStop nearest = null;
        double nearestAt = length;
        AABB reach = new AABB(from, to).inflate(SHIELD_RADIUS + 1.0);
        for (Player wanda : level.getEntitiesOfClass(Player.class, reach, p -> p != firer && !p.isSpectator() && hasShieldUp(p))) {
            Vec3 fromCenter = from.subtract(wanda.getPosition(partialTick).add(0, wanda.getBbHeight() * 0.5, 0));
            double outside = fromCenter.lengthSqr() - SHIELD_RADIUS * SHIELD_RADIUS;
            if (outside <= 0) {
                continue;
            }
            // Where the line first meets the sphere: the smaller root of |fromCenter + t * direction| = radius.
            double along = fromCenter.dot(direction);
            double discriminant = along * along - outside;
            if (discriminant < 0) {
                continue;
            }
            double at = -along - Math.sqrt(discriminant);
            if (at >= 0 && at <= nearestAt) {
                nearestAt = at;
                nearest = new ShieldStop(wanda, from.add(direction.scale(at)));
            }
        }
        return nearest;
    }

    /**
     * A beam (or a blast) of {@code firer} spends itself on the wall {@code stop}: a Chaos Shield or a Hex Barrier pays {@code
     * amount} (the barrier shatters at once if the firer is the Scarlet Witch); a Hex's or a ward's wall pays nothing. As
     * before, with PvP off a hit that would not have hurt the holder costs nothing. Server.
     */
    public static void pay(ShieldStop stop, @Nullable Entity firer, float amount) {
        if (stop.owner() instanceof ServerPlayer holder && (!(firer instanceof Player shooter) || shooter.canHarmPlayer(holder))) {
            wear(holder, amount, firer);
        }
    }

    /**
     * Wears the shield down by what it just stopped; when nothing is left it shatters.
     *
     * @return how much of {@code amount} the shield took (all of it, unless it had less strength left)
     */
    public static float wear(ServerPlayer player, float amount) {
        return wear(player, amount, null);
    }

    /**
     * {@link #wear(ServerPlayer, float)} for damage that has a known source: with {@code scarlet_witch.shieldUnlimitedVsBosses} off, what a
     * boss fight deals ({@link #fromBoss}) wears even an unlimited shield down by the old rules.
     *
     * @param cause who or what dealt it (a projectile counts as its owner), or null if it is not known
     */
    public static float wear(ServerPlayer player, float amount, @Nullable Entity cause) {
        PowerData data = Powers.get(player);
        if (BillyBarrier.owns(data)) {
            return BillyBarrier.wear(player, data, amount, cause); // 3.3: the wall it stopped at was his Hex Barrier
        }
        if (data.has(HeroAbility.WANDA_CHAOS_SHIELD) && shieldUnlimited() && (EnhancedConfig.WANDA_SHIELD_UNLIMITED_VS_BOSSES.get() || !fromBoss(cause))) {
            return amount; // 3.2: an unlimited shield stops it all and is never worn down
        }
        float taken = Math.min(amount, data.shieldStrength());
        data.setShieldStrength(data.shieldStrength() - taken);
        if (data.shieldStrength() <= 1.0E-4F) {
            data.setShieldStrength(0F);
            shatter(player, data);
        }
        return taken;
    }

    /** Whether this came from a boss fight: a boss, something a fight made (a minion, a sand lance...), or a projectile of one. */
    public static boolean fromBoss(@Nullable Entity cause) {
        for (int i = 0; cause != null && i < 4; i++) {
            if (cause.getType().is(ModTags.BOSSES) || BossFights.isFightEntity(cause) || BossFights.fightOf(cause) != null) {
                return true;
            }
            cause = cause instanceof Projectile projectile ? projectile.getOwner() : null;
        }
        return false;
    }

    /** A projectile touching the Chaos Shield turns to red sparks. */
    public static void disintegrate(ServerLevel level, Projectile projectile) {
        disintegrate(level, projectile, Power.SCARLET_WITCH.color);
    }

    /** A projectile touching a shield turns to sparks of its colour and is gone. */
    public static void disintegrate(ServerLevel level, Projectile projectile, int color) {
        level.sendParticles(new EnergyParticleOptions(color, 1.3F), projectile.getX(), projectile.getY(), projectile.getZ(),
                10, 0.15, 0.15, 0.15, 0.08);
        level.sendParticles(ParticleTypes.SMOKE, projectile.getX(), projectile.getY(), projectile.getZ(), 3, 0.1, 0.1, 0.1, 0.01);
        level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 1.8F);
        projectile.discard();
    }

    private WandaAbilities() {
    }
}
