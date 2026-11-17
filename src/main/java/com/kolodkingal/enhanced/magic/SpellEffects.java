package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MindLink;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.WitchcraftConfig;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.hela.HelaArmy;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What each spell of 3.17 does ({@link Spell}): every spell is one method here, called by {@link Casting} once the
 * incantation is over. Each returns whether it could be cast (if not, nothing is spent and it does not cool down) and
 * shows itself: its shape for every client ({@link MagicFx}), its particles and its sound. {@code power} is the caster's
 * strength ({@link Magic#strength}).
 */
public final class SpellEffects {
    private SpellEffects() {
    }

    // ---- helpers ----

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    private static float dmg(ServerPlayer p, float base, float power) {
        float factor = loaded() ? WitchcraftConfig.SPELL_DAMAGE.get().floatValue() : 1.0F;
        return base * power * factor * Magic.corruptionBonus(p);
    }

    private static DamageSource src(ServerPlayer p, Spell spell) {
        return ModDamageTypes.source(p.level(), spell.forbidden() ? ModDamageTypes.DARK_SPELL : ModDamageTypes.SPELL, p, p);
    }

    private static boolean enemy(ServerPlayer p, LivingEntity e) {
        if (e instanceof Player && loaded() && !WitchcraftConfig.SPELLS_HURT_PLAYERS.get()) {
            return false;
        }
        return Magic.enemy(p, e);
    }

    /** The creature the caster looks at, in reach (any living thing but himself). */
    @Nullable
    private static LivingEntity looked(ServerPlayer p, double range) {
        EntityHitResult hit = Raycasts.entity(p, range, 0.3, e -> e instanceof LivingEntity && e != p && e.isAlive() && !e.isSpectator(), 1.0F);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }

    /** Where the caster looks, in reach: the creature, the block, or the end of the reach. */
    private static Vec3 aim(ServerPlayer p, double range) {
        HitResult hit = Raycasts.any(p, range, e -> e instanceof LivingEntity && e != p, 1.0F);
        return hit.getLocation();
    }

    private static List<LivingEntity> enemies(ServerPlayer p, Vec3 at, double r) {
        List<LivingEntity> list = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(r),
                e -> e != p && e.position().distanceToSqr(at) <= r * r && enemy(p, e));
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
        return list;
    }

    private static List<LivingEntity> friends(ServerPlayer p, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e.distanceToSqr(p) <= r * r && Magic.friend(p, e));
    }

    private static Vec3 mid(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0);
    }

    private static void say(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
    }

    private static void sound(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** (A sound that is a holder on some versions.) */
    private static void sound(ServerLevel level, Vec3 at, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static long now(ServerPlayer p) {
        return p.level().getGameTime();
    }

    private static int tempTicks() {
        return 20 * (loaded() ? WitchcraftConfig.TEMP_BLOCK_SECONDS.get() : 30);
    }

    private static boolean nothing(ServerPlayer p) {
        say(p, "message.enhanced.spell.no_target");
        return false;
    }

    /** A bolt of a spell from the caster's hand to whatever he looks at; true if it struck a creature. */
    private static LivingEntity bolt(ServerPlayer p, Spell spell, double range, float damage, int color) {
        ServerLevel level = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.3, 0).add(p.getLookAngle().scale(0.6));
        LivingEntity target = looked(p, range);
        Vec3 end = target != null ? mid(target) : aim(p, range);
        MagicFx.send(level, MagicFx.BEAMS, from, List.of(end), color, 0xFFFFFF, 1.0F, 8);
        MagicFx.burst(level, end, color, 12, 0.25, 1.1F);
        if (target != null && enemy(p, target)) {
            target.hurt(src(p, spell), damage);
            return target;
        }
        return null;
    }

    // ---- the spells ----

    /** Casts {@code spell}; false if it could not be cast now (nothing spent). */
    public static boolean cast(ServerPlayer p, Spell spell, float power, boolean alt) {
        ServerLevel level = p.serverLevel();
        int c = Magic.color(p, spell);
        return switch (spell) {
            // ===== Green =====
            case VINE_SNARE -> {
                Vec3 at = aim(p, 20);
                List<LivingEntity> caught = enemies(p, at, 3.5 + power);
                MagicFx.send(level, MagicFx.CIRCLE, at, List.of(), c, 0xD8FFC8, (float) (3.5 + power), 60);
                List<Vec3> pts = new ArrayList<>();
                for (LivingEntity e : caught) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) (80 * power), 5, false, true, true), p);
                    e.addEffect(new MobEffectInstance(MobEffects.JUMP, (int) (80 * power), 128, false, false, false), p);
                    e.hurt(src(p, spell), dmg(p, 2.0F, power));
                    pts.add(e.position());
                    level.sendParticles(ParticleTypes.COMPOSTER, e.getX(), e.getY() + 0.3, e.getZ(), 16, 0.4, 0.3, 0.4, 0.01);
                }
                MagicFx.send(level, MagicFx.BIND, at, pts, 0x3F9F48, 0x8FE07A, 0.9F, (int) (80 * power));
                sound(level, at, SoundEvents.AZALEA_LEAVES_PLACE, 1.2F, 0.6F);
                yield true;
            }
            case HERBAL_MEND -> {
                boolean night = !level.isDay() && level.canSeeSky(p.blockPosition());
                float heal = 6.0F * power * (night ? 1.5F : 1.0F);
                for (LivingEntity e : friends(p, 6)) {
                    e.heal(heal);
                    MagicFx.spiral(level, e.position(), 0.6, e.getBbHeight(), night ? 0xCFE8FF : c, 0.9F);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1, e.getZ(), 6, 0.4, 0.5, 0.4, 0.0);
                }
                MagicFx.send(level, MagicFx.CIRCLE, p.position(), List.of(), c, 0xFFFFFF, 6.0F, 30);
                sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
                yield true;
            }
            case MOONLIGHT_BLESSING -> {
                boolean night = !level.isDay();
                int ticks = (int) (400 * power * (night ? 1.5F : 1.0F));
                for (LivingEntity e : friends(p, 10)) {
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, night ? 1 : 0, false, true, true), p);
                    e.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks, 0, false, false, true), p);
                    MagicFx.spiral(level, e.position(), 0.7, 2.2, 0xCFE8FF, 0.8F);
                }
                MagicFx.send(level, MagicFx.RING, p.position(), List.of(), 0xCFE8FF, 0xFFFFFF, 10.0F, 30);
                sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.8F);
                yield true;
            }
            case THORN_WALL -> wall(p, c, true);
            case ROAD_PORTAL -> CovenRoad.openPortal(p, c);
            // ===== Potions =====
            case QUICK_BREW -> {
                give(p, PotionContents.createItemStack(Items.POTION, Potions.STRONG_STRENGTH));
                give(p, PotionContents.createItemStack(Items.POTION, Potions.STRONG_SWIFTNESS));
                give(p, PotionContents.createItemStack(Items.POTION, power >= 1.3F ? Potions.STRONG_TURTLE_MASTER : Potions.STRONG_REGENERATION));
                MagicFx.spiral(level, p.position(), 0.5, 1.8, c, 1.0F);
                sound(level, p.position(), SoundEvents.BREWING_STAND_BREW, 1.0F, 1.4F);
                say(p, "message.enhanced.spell.brewed");
                yield true;
            }
            case HEALING_DRAUGHT -> {
                for (LivingEntity e : friends(p, 5)) {
                    e.heal(8.0F * power);
                    e.clearFire();
                }
                level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 1, p.getZ(), 60, 2.0, 0.6, 2.0, 0.2);
                MagicFx.send(level, MagicFx.RING, p.position(), List.of(), c, 0xFFFFFF, 5.0F, 18);
                sound(level, p.position(), SoundEvents.SPLASH_POTION_BREAK, 1.0F, 1.0F);
                yield true;
            }
            case ACID_FLASK -> {
                Vec3 at = aim(p, 16);
                MagicFx.send(level, MagicFx.BEAMS, p.getEyePosition(), List.of(at), c, 0xE8FF9A, 1.0F, 6);
                for (LivingEntity e : enemies(p, at, 3.0)) {
                    e.hurt(src(p, spell), dmg(p, 6.0F, power));
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, true, true), p);
                }
                level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.2, at.z, 30, 1.2, 0.3, 1.2, 0.1);
                MagicFx.send(level, MagicFx.RING, at, List.of(), c, 0xE8FF9A, 3.0F, 14);
                sound(level, at, SoundEvents.SPLASH_POTION_BREAK, 1.0F, 0.7F);
                yield true;
            }
            case ANTIDOTE -> {
                for (LivingEntity e : friends(p, 6)) {
                    cure(e, false);
                    if (e instanceof ServerPlayer sp) {
                        Darkness.cleanse(sp, 10);
                    }
                    MagicFx.spiral(level, e.position(), 0.5, 2.0, c, 0.8F);
                }
                sound(level, p.position(), SoundEvents.GENERIC_DRINK, 1.0F, 1.2F);
                yield true;
            }
            case TRUTH_SERUM -> {
                LivingEntity t = looked(p, 16);
                if (t == null) {
                    yield nothing(p);
                }
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, false, false), p);
                t.removeEffect(MobEffects.INVISIBILITY);
                t.setInvisible(false);
                Component what = t instanceof Player other
                        ? Component.translatable("message.enhanced.spell.truth.player", other.getDisplayName(),
                        Powers.power(other).displayName(), (int) other.getHealth(), other.blockPosition().getX(),
                        other.blockPosition().getY(), other.blockPosition().getZ())
                        : Component.translatable("message.enhanced.spell.truth.creature", t.getDisplayName(), (int) t.getHealth(),
                        (int) t.getMaxHealth());
                p.displayClientMessage(what.copy().withStyle(ChatFormatting.YELLOW), false);
                if (t instanceof ServerPlayer other) {
                    other.displayClientMessage(Component.translatable("message.enhanced.spell.truth.you").withStyle(ChatFormatting.YELLOW), true);
                }
                MagicFx.spiral(level, t.position(), 0.6, t.getBbHeight(), c, 0.9F);
                sound(level, t.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.2F);
                yield true;
            }
            // ===== Protection =====
            case SHIELD_ALLY -> {
                LivingEntity t = looked(p, 16);
                if (t == null || !Magic.friend(p, t)) {
                    t = p;
                }
                t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int) (600 * power), 1, false, true, true), p);
                t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, (int) (600 * power), 0, false, true, true), p);
                MagicFx.send(level, MagicFx.BIND, t.position(), List.of(t.position()), c, 0xFFE2A8, t.getBbHeight(), 40);
                sound(level, t.position(), SoundEvents.BLAZE_SHOOT, 0.6F, 1.6F);
                yield true;
            }
            case PROTECTION_CIRCLE -> {
                int ticks = (int) (400 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.PROTECTION_CIRCLE, p, p.position(), 5.0, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.CIRCLE, p.position(), List.of(), c, 0xFFE2A8, 5.0F, ticks));
                sound(level, p.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
                yield true;
            }
            case REVERSAL -> {
                MagicZones.buff(p, "reversal", (int) (160 * power));
                MagicFx.sendOn(level, MagicFx.DISKS, p, List.of(), c, 0xFFFFFF, 0.9F, (int) (160 * power));
                sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.8F);
                say(p, "message.enhanced.spell.reversal");
                yield true;
            }
            case FLAME_WARD -> {
                int ticks = (int) (200 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.FLAME_WARD, p, p.position(), 4.0, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.CIRCLE, p.position(), List.of(), c, 0xFFD27A, 4.0F, ticks));
                level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 0.2, p.getZ(), 60, 3.0, 0.1, 3.0, 0.02);
                sound(level, p.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 0.9F);
                yield true;
            }
            // ===== Divination =====
            case WIND_STEP -> {
                Vec3 look = p.getLookAngle();
                p.setDeltaMovement(look.scale(1.6 * Math.min(1.5F, power)).add(0, 0.3, 0));
                p.hurtMarked = true;
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false, true));
                CustomHeroes.guardFall(p, 80);
                level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 20, 0.4, 0.3, 0.4, 0.05);
                sound(level, p.position(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8F, 1.4F);
                yield true;
            }
            case TRUE_SIGHT -> {
                int n = 0;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(32), e -> e != p && e.isAlive())) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) (300 * power), 0, false, false), p);
                    n++;
                }
                MagicFx.send(level, MagicFx.RING, p.position(), List.of(), c, 0xFFFFFF, 32.0F, 30);
                say(p, "message.enhanced.spell.true_sight", n);
                sound(level, p.position(), SoundEvents.ENDER_EYE_DEATH, 1.0F, 1.2F);
                yield true;
            }
            case FORESIGHT -> {
                MagicZones.buff(p, "foresight", (int) (600 * power));
                MagicFx.sendOn(level, MagicFx.AWAKEN, p, List.of(), c, 0xFFFFFF, 1.0F, 20);
                say(p, "message.enhanced.spell.foresight");
                sound(level, p.position(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.0F, 1.2F);
                yield true;
            }
            case SEEING_SIGIL -> {
                int ticks = (int) (2400 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.SEEING_SIGIL, p, p.position(), 12.0, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.SIGIL, p.position(), List.of(), c, 0xF0E8FF, 1.6F, ticks));
                sound(level, p.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 0.8F);
                yield true;
            }
            case PREMONITION -> {
                MagicZones.buff(p, "premonition", (int) (400 * power));
                for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24), m -> m.getTarget() == p)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false), p);
                }
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) (400 * power), 0, false, false, true));
                say(p, "message.enhanced.spell.premonition");
                sound(level, p.position(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.0F, 1.4F);
                yield true;
            }
            // ===== Spirit =====
            case CHAOS_BOLT -> {
                LivingEntity t = bolt(p, spell, 24, dmg(p, 8.0F, power), c);
                if (t != null) {
                    List<net.minecraft.core.Holder<MobEffect>> chaos = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS,
                            MobEffects.LEVITATION, MobEffects.BLINDNESS, MobEffects.GLOWING);
                    t.addEffect(new MobEffectInstance(chaos.get(p.getRandom().nextInt(chaos.size())), 80, 0, false, true, true), p);
                }
                sound(level, p.position(), SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.4F);
                yield true;
            }
            case SPIRIT_WALK -> {
                int ticks = (int) (120 * power);
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 4, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1, false, false, true));
                for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24), m -> m.getTarget() == p)) {
                    mob.setTarget(null);
                }
                MagicFx.burst(level, p.position().add(0, 1, 0), c, 30, 0.5, 1.2F);
                sound(level, p.position(), SoundEvents.SOUL_ESCAPE.value(), 1.2F, 0.8F);
                yield true;
            }
            // ===== Sorcerer =====
            case ELDRITCH_BOLT -> {
                bolt(p, spell, 24, dmg(p, 7.0F, power), c);
                sound(level, p.position(), SoundEvents.BLAZE_SHOOT, 0.6F, 1.8F);
                yield true;
            }
            case MANDALA_SHIELD -> {
                int ticks = (int) (140 * power);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 2, false, false, true));
                MagicZones.buff(p, "reversal", ticks);
                MagicFx.send(level, MagicFx.PORTAL, p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(0.8)).add(0, -0.4, 0),
                        List.of(p.getLookAngle()), c, 0xFFE0A8, 1.1F, ticks);
                sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.6F);
                yield true;
            }
            case CRIMSON_BANDS -> {
                LivingEntity t = looked(p, 20);
                if (t == null || !enemy(p, t)) {
                    yield nothing(p);
                }
                bind(p, t, (int) (120 * power), c);
                sound(level, t.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.6F);
                yield true;
            }
            case SLING_PORTAL -> {
                Vec3 to = aim(p, 64);
                Vec3 dir = to.subtract(p.position()).multiply(1, 0, 1);
                Vec3 at = p.position().add(dir.normalize().scale(2.0));
                Portals.open(p, at, dir, level.dimension(), to.subtract(dir.normalize().scale(1.5)), (int) (200 * power), c);
                yield true;
            }
            // ===== every witch =====
            case MINDSCAPE -> Realms.toggleMindscape(p);
            case BROOM -> {
                int ticks = (int) (1200 * power);
                MagicZones.buff(p, "broom", ticks);
                Broom.mount(p);
                MagicFx.burst(level, p.position().add(0, 0.6, 0), c, 20, 0.5, 1.0F);
                sound(level, p.position(), SoundEvents.ELYTRA_FLYING, 0.5F, 1.8F);
                yield true;
            }
            case COVEN_CIRCLE -> {
                int n = 0;
                for (Player other : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(10), o -> Magic.isCaster(o))) {
                    other.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0, false, true, true), p);
                    other.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0, false, true, true), p);
                    if (other instanceof ServerPlayer sp) {
                        Magic.set(sp, Magic.data(sp).withMana(Math.min(Magic.maxMana(sp), Magic.data(sp).mana() + 50)));
                    }
                    n++;
                }
                MagicFx.send(level, MagicFx.CIRCLE, p.position(), List.of(), c, 0xF0FFF6, 10.0F, 60);
                say(p, "message.enhanced.spell.coven", n);
                sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.4F);
                yield true;
            }
            // ===== the Darkhold =====
            case DREAMWALK -> Realms.dreamwalk(p, (int) (400 * power));
            case POWER_SIPHON -> {
                LivingEntity t = looked(p, 16);
                if (t == null || !enemy(p, t)) {
                    yield nothing(p);
                }
                float d = dmg(p, 8.0F, power);
                if (t.hurt(src(p, spell), d)) {
                    p.heal(d);
                }
                if (t instanceof ServerPlayer victim) {
                    victim.addEffect(new MobEffectInstance(ModEffects.DRAINED, 160, 0, false, true, true), p);
                } else {
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true, true), p);
                }
                MagicFx.send(level, MagicFx.DRAIN, p.position().add(0, 1.2, 0), List.of(mid(t)), c, 0xB45CFF, 1.0F, 30);
                sound(level, t.position(), SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.7F);
                yield true;
            }
            case SOUL_BIND -> {
                LivingEntity t = looked(p, 16);
                if (t == null || !enemy(p, t)) {
                    yield nothing(p);
                }
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.SOUL_BIND, p, p.position(), 12.0, (int) (400 * power), power);
                z.target = t.getUUID();
                MagicFx.send(level, MagicFx.BIND, t.position(), List.of(t.position()), c, 0xB45CFF, t.getBbHeight(), 60);
                say(p, "message.enhanced.spell.soul_bound", t.getDisplayName());
                sound(level, t.position(), SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.8F, 0.6F);
                yield true;
            }
            case NECROMANCY -> {
                HelaArmy.raise(p, power >= 1.4F ? 6 : 4, (int) (800 * power));
                MagicFx.send(level, MagicFx.CIRCLE, p.position(), List.of(), c, 0x9CF2B5, 4.0F, 60);
                yield true;
            }
            // ===== the Vishanti =====
            case VISHANTI_LIGHT -> {
                for (LivingEntity e : friends(p, 8)) {
                    cure(e, true);
                    if (e instanceof ServerPlayer sp) {
                        Darkness.cleanse(sp, 40);
                    }
                }
                for (LivingEntity e : enemies(p, p.position(), 8)) {
                    if (e.isInvertedHealAndHarm() || e.getType().is(ModTags.BOSS_MINIONS)) {
                        e.hurt(src(p, spell), dmg(p, 10.0F, power));
                        e.igniteForSeconds(3);
                    }
                }
                MagicFx.send(level, MagicFx.STARBURST, p.position().add(0, 1.2, 0), List.of(), c, 0xFFFFFF, 8.0F, 24);
                MagicFx.send(level, MagicFx.RING, p.position(), List.of(), c, 0xFFFFFF, 8.0F, 24);
                sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.8F);
                yield true;
            }
            case VISHANTI_SHIELD -> {
                int ticks = (int) (300 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.VISHANTI_SHIELD, p, p.position(), 6.0, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.DOME, p.position(), List.of(), c, 0xFFFFFF, 6.0F, ticks));
                sound(level, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.4F);
                yield true;
            }
            case BANISH -> {
                int banished = 0;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(12), e -> e != p && e.isAlive())) {
                    if (Spirits.summoned(e) && !Spirits.ownedBy(e, p)) {
                        level.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
                        e.discard();
                        banished++;
                    } else if (e.isInvertedHealAndHarm() && enemy(p, e)) {
                        e.hurt(src(p, spell), dmg(p, 12.0F, power));
                        level.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.5, 0.3, 0.05);
                        banished++;
                    }
                }
                MagicFx.send(level, MagicFx.RING, p.position(), List.of(), c, 0xFFFFFF, 12.0F, 20);
                say(p, "message.enhanced.spell.banished", banished);
                sound(level, p.position(), SoundEvents.ZOMBIE_VILLAGER_CURE, 0.8F, 1.6F);
                yield true;
            }
            // ===== Cagliostro =====
            case CHRONO_HOLD -> {
                LivingEntity t = looked(p, 16);
                if (t == null || !enemy(p, t)) {
                    yield nothing(p);
                }
                bind(p, t, (int) (100 * power), c);
                t.setDeltaMovement(Vec3.ZERO);
                MagicFx.send(level, MagicFx.CIRCLE, t.position(), List.of(), c, 0xC8FFD0, 1.2F, (int) (100 * power));
                sound(level, t.position(), SoundEvents.BELL_RESONATE, 1.0F, 1.6F);
                yield true;
            }
            case TIME_LOOP -> TimeLoop.cast(p);
            case DARK_DIMENSION -> {
                Vec3 at = aim(p, 24);
                int ticks = (int) (120 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.DARK_DIMENSION, p, at, 6.0, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.VORTEX, at, List.of(), c, 0xE0B0FF, 6.0F, ticks));
                sound(level, at, SoundEvents.END_PORTAL_SPAWN, 0.8F, 0.5F);
                yield true;
            }
            // ===== Billy =====
            case UNBINDING -> {
                int freed = 0;
                for (LivingEntity e : friends(p, 8)) {
                    if (unbind(e)) {
                        freed++;
                    }
                    MagicFx.spiral(level, e.position(), 0.6, e.getBbHeight() + 0.5, c, 1.0F);
                }
                MagicFx.send(level, MagicFx.STARBURST, p.position().add(0, 1.2, 0), List.of(), c, 0xFFFFFF, 6.0F, 18);
                say(p, "message.enhanced.spell.unbound", freed);
                sound(level, p.position(), SoundEvents.ZOMBIE_VILLAGER_CURE, 0.8F, 1.8F);
                yield true;
            }
            case SPATIAL_FOLD -> fold(p, c);
            case CONJURE_WEAPON -> {
                ItemStack sword = new ItemStack(power >= 1.4F ? Items.NETHERITE_SWORD : Items.DIAMOND_SWORD);
                Conjured.mark(sword, now(p) + (long) (1200 * power));
                give(p, sword);
                MagicFx.spiral(level, p.position(), 0.5, 1.6, c, 1.0F);
                sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
                yield true;
            }
            case CONJURE_FOOD -> {
                give(p, new ItemStack(Items.GOLDEN_CARROT, 3));
                give(p, new ItemStack(Items.BREAD, 4));
                MagicFx.burst(level, p.position().add(0, 1, 0), c, 16, 0.4, 1.0F);
                sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
                yield true;
            }
            case CONJURE_ARMOR -> {
                long until = now(p) + (long) (1200 * power);
                int n = 0;
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                net.minecraft.world.item.Item[] items = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
                for (int i = 0; i < 4; i++) {
                    if (p.getItemBySlot(slots[i]).isEmpty()) {
                        ItemStack piece = new ItemStack(items[i]);
                        Conjured.mark(piece, until);
                        p.setItemSlot(slots[i], piece);
                        n++;
                    }
                }
                if (n == 0) {
                    say(p, "message.enhanced.spell.armor_full");
                    yield false;
                }
                MagicFx.sendOn(level, MagicFx.AWAKEN, p, List.of(), c, 0xFFFFFF, 1.0F, 16);
                sound(level, p.position(), SoundEvents.ARMOR_EQUIP_DIAMOND.value(), 1.0F, 1.2F);
                yield true;
            }
            case LIGHT_PLATFORM -> {
                BlockPos under = BlockPos.containing(p.getX(), p.getY() - 0.5, p.getZ());
                int placed = 0;
                long until = now(p) + tempTicks();
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (TempBlocks.place(level, under.offset(dx, 0, dz), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), until)) {
                            placed++;
                        }
                    }
                }
                if (placed == 0) {
                    yield nothing(p);
                }
                p.setDeltaMovement(p.getDeltaMovement().multiply(1, 0, 1));
                p.hurtMarked = true;
                p.resetFallDistance();
                MagicFx.send(level, MagicFx.CIRCLE, Vec3.atBottomCenterOf(under.above()), List.of(), c, 0xFFFFFF, 2.5F, 30);
                sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_PLACE, 1.0F, 1.2F);
                yield true;
            }
            case SILENCING_SIGIL -> {
                int ticks = (int) (600 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.SILENCING_SIGIL, p, p.position(), 8.0, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.SIGIL, p.position(), List.of(), c, 0x9CC8FF, 2.2F, ticks));
                for (ServerPlayer other : level.players()) {
                    if (other != p && z.contains(other) && !Magic.friend(p, other)) {
                        Casting.stop(other);
                    }
                }
                sound(level, p.position(), SoundEvents.SCULK_CLICKING_STOP, 1.0F, 0.6F);
                yield true;
            }
            case SPATIAL_CRUSH -> {
                LivingEntity t = looked(p, 24);
                if (t == null || !enemy(p, t)) {
                    yield nothing(p);
                }
                t.hurt(src(p, spell), dmg(p, 6.0F + Magic.billyRank(p), power));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true, true), p);
                t.setDeltaMovement(t.getDeltaMovement().multiply(0.2, 0.2, 0.2));
                t.hurtMarked = true;
                MagicFx.send(level, MagicFx.VORTEX, t.position(), List.of(), c, 0xFFFFFF, 1.6F, 14);
                MagicFx.send(level, MagicFx.SHATTER, mid(t), List.of(), c, 0xFFFFFF, 1.5F, 12);
                sound(level, t.position(), SoundEvents.ANVIL_LAND, 0.6F, 1.6F);
                yield true;
            }
            case CONJURE_BRIDGE -> {
                Vec3 dir = p.getLookAngle().multiply(1, 0, 1);
                if (dir.lengthSqr() < 1.0E-4) {
                    yield nothing(p);
                }
                dir = dir.normalize();
                long until = now(p) + tempTicks();
                int placed = 0;
                int y = (int) Math.floor(p.getY() - 0.5);
                Vec3 side = new Vec3(-dir.z, 0, dir.x);
                for (int i = 1; i <= 16 + 2 * Magic.billyRank(p); i++) {
                    for (int w = -1; w <= 1; w++) {
                        Vec3 at = p.position().add(dir.scale(i)).add(side.scale(w));
                        if (TempBlocks.place(level, new BlockPos((int) Math.floor(at.x), y, (int) Math.floor(at.z)),
                                Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), until)) {
                            placed++;
                        }
                    }
                }
                if (placed == 0) {
                    yield nothing(p);
                }
                MagicFx.send(level, MagicFx.TRAIL, p.position(), List.of(p.position(), p.position().add(dir.scale(16))), c, 0xFFFFFF, 1.0F, 30);
                sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_PLACE, 1.0F, 1.0F);
                yield true;
            }
            case CONJURE_WALL -> wall(p, c, false);
            case CLAIRVOYANCE -> {
                com.kolodkingal.enhanced.platform.net.Net.sendToPlayer(p, new MagicScreenPayload(MagicScreenPayload.CLAIRVOYANCE,
                        Realms.onlineNames(p), List.of(), 0));
                yield true;
            }
            case ENCHANT_ITEM -> {
                ItemStack held = p.getMainHandItem();
                if (held.isEmpty()) {
                    say(p, "message.enhanced.spell.empty_hand");
                    yield false;
                }
                Conjured.enchant(held, now(p) + (long) (1200 * power));
                MagicFx.spiral(level, p.position(), 0.5, 1.6, c, 1.0F);
                sound(level, p.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
                say(p, "message.enhanced.spell.enchanted", held.getHoverName());
                yield true;
            }
            case FAMILIAR -> Familiars.summon(p, (int) (2400 * power));
            case PROTECTION_RUNE -> {
                int ticks = (int) (400 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.PROTECTION_RUNE, p, p.position(), 6.0, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.DOME, p.position(), List.of(), c, 0xCFEFFF, 6.0F, ticks));
                sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.2F);
                yield true;
            }
            case SIPHON_STANCE -> {
                MagicZones.buff(p, "siphon", (int) (200 * power));
                MagicFx.sendOn(level, MagicFx.DISKS, p, List.of(), c, 0xFFFFFF, 1.2F, (int) (200 * power));
                say(p, "message.enhanced.spell.siphon");
                sound(level, p.position(), SoundEvents.CONDUIT_ACTIVATE, 1.0F, 1.4F);
                yield true;
            }
            case POCKET_DIMENSION -> Realms.togglePocket(p, alt || p.isShiftKeyDown());
            case GIANT_HAND -> {
                Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
                double cos = Math.cos(Math.toRadians(50));
                int hit = 0;
                for (LivingEntity e : enemies(p, p.position(), 9)) {
                    Vec3 to = e.position().subtract(p.position()).multiply(1, 0, 1);
                    if (to.lengthSqr() < 1.0E-4 || to.normalize().dot(look) >= cos) {
                        e.hurt(src(p, spell), dmg(p, 5.0F, power));
                        e.setDeltaMovement(look.scale(2.2 * power).add(0, 0.9, 0));
                        e.hurtMarked = true;
                        hit++;
                    }
                }
                MagicFx.send(level, MagicFx.HAND, p.position(), List.of(look), c, 0xFFFFFF, 8.0F, 14);
                sound(level, p.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.6F);
                yield true;
            }
            case RAISE_SPIRIT -> Spirits.raise(p, (int) (1200 * power), c);
            case INFINITE_HALLWAY -> {
                LivingEntity t = looked(p, 24);
                if (t == null || !enemy(p, t)) {
                    yield nothing(p);
                }
                int max = loaded() ? WitchcraftConfig.HALLWAY_MAX_SECONDS.get() : 29;
                int seconds = Math.min(max, Magic.isDemiurge(p) ? max : 10 + 2 * Magic.billyRank(p));
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.HALLWAY, p, p.position(), 1.0, seconds * 20, power);
                z.target = t.getUUID();
                z.startDistance = Math.max(4.0, t.distanceTo(p));
                MagicFx.send(level, new Vec3(p.getX(), p.getY(), p.getZ()), new MagicFxPayload(MagicFx.HALLWAY, 0, t.getId(), p.position(),
                        List.of(), c, 0xCFEFFF, 1.0F, seconds * 20));
                if (t instanceof ServerPlayer victim) {
                    victim.displayClientMessage(Component.translatable("message.enhanced.spell.hallway.you").withStyle(ChatFormatting.AQUA), true);
                }
                say(p, "message.enhanced.spell.hallway", t.getDisplayName(), seconds);
                sound(level, t.position(), SoundEvents.PORTAL_AMBIENT, 0.6F, 1.6F);
                yield true;
            }
            case SPINNING_DISKS -> {
                int ticks = (int) (200 * power);
                MagicZones.add(MagicZones.Kind.DISKS, p, p.position(), 1.8, ticks, power);
                MagicFx.sendOn(level, MagicFx.DISKS, p, List.of(), c, 0xFFFFFF, 1.8F, ticks);
                sound(level, p.position(), SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.0F, 1.4F);
                yield true;
            }
            case CURSE_ITEM -> {
                LivingEntity t = looked(p, 16);
                if (t == null || !enemy(p, t) || t.getMainHandItem().isEmpty()) {
                    yield nothing(p);
                }
                MagicZones.mark(t, "cursed_hand", (int) (400 * power));
                if (!(t instanceof Player)) {
                    t.igniteForSeconds(20.0F * power); // (a creature's cursed weapon burns it the whole while)
                }
                MagicFx.spiral(level, t.position(), 0.4, 1.4, c, 0.9F);
                say(p, "message.enhanced.spell.cursed", t.getMainHandItem().getHoverName());
                sound(level, t.position(), SoundEvents.EVOKER_PREPARE_WOLOLO, 1.0F, 0.8F);
                yield true;
            }
            case COMBAT_PORTALS -> {
                Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 at = p.position().add(look.scale(2.5));
                int ticks = (int) (200 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.COMBAT_PORTAL, p, at.add(0, 1.2, 0), 2.2, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.PORTAL, at, List.of(look), c, 0xFFFFFF, 1.6F, ticks));
                sound(level, at, SoundEvents.END_PORTAL_SPAWN, 0.5F, 1.8F);
                yield true;
            }
            case DREAM_BIND -> {
                LivingEntity t = looked(p, 16);
                if (t == null || !enemy(p, t)) {
                    yield nothing(p);
                }
                yield Realms.dreamBind(p, t, (int) (240 * power), alt || p.isShiftKeyDown(), c);
            }
            case ZERO_GRAVITY -> {
                int ticks = (int) (100 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.ZERO_GRAVITY, p, p.position(), 8.0, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.GRAVITY, p.position(), List.of(), c, 1, 8.0F, ticks));
                sound(level, p.position(), SoundEvents.SHULKER_SHOOT, 1.0F, 0.6F);
                yield true;
            }
            case GRAVITY_SLAM -> {
                for (LivingEntity e : enemies(p, p.position(), 8)) {
                    e.setDeltaMovement(e.getDeltaMovement().multiply(0.2, 0, 0.2).add(0, -2.4, 0));
                    e.hurtMarked = true;
                    e.hurt(src(p, spell), dmg(p, 7.0F, power));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, true, true), p);
                }
                MagicFx.send(level, MagicFx.GRAVITY, p.position(), List.of(), c, 0, 8.0F, 20);
                MagicFx.send(level, MagicFx.RING, p.position(), List.of(), c, 0xFFFFFF, 8.0F, 16);
                sound(level, p.position(), SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.2F, 0.7F);
                yield true;
            }
            case PROBABILITY_HEX -> {
                int ticks = (int) (300 * power);
                MagicZones.Zone z = MagicZones.add(MagicZones.Kind.PROBABILITY_HEX, p, aim(p, 16), 6.0, ticks, power);
                MagicZones.show(z, MagicFx.sendLasting(level, MagicFx.SIGIL, z.center, List.of(), c, 0xD8C8FF, 6.0F, ticks));
                sound(level, z.center, SoundEvents.WITCH_CELEBRATE, 1.0F, 1.0F);
                yield true;
            }
            case REALITY_SHATTER -> {
                Vec3 at = aim(p, 24);
                for (LivingEntity e : enemies(p, at, 4.0)) {
                    e.hurt(src(p, spell), dmg(p, 10.0F + Magic.billyRank(p), power));
                    Shots.push(e, at, 0.8, 0.4);
                }
                MagicFx.send(level, MagicFx.SHATTER, at.add(0, 0.5, 0), List.of(), c, 0x7FD0FF, 4.0F, 20);
                sound(level, at, SoundEvents.GLASS_BREAK, 1.5F, 0.5F);
                sound(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8F, 1.6F);
                yield true;
            }
            case TRANSMUTE -> {
                LivingEntity t = looked(p, 16);
                if (t == null || !enemy(p, t)) {
                    yield nothing(p);
                }
                transmute(p, t, c);
                yield true;
            }
            case DIMENSION_GATE -> {
                ResourceDim to = ResourceDim.gateFor(level);
                ServerLevel there = level.getServer().getLevel(to.key());
                if (there == null) {
                    yield nothing(p);
                }
                Vec3 dest = Portals.gateDestination(level, there, p.position());
                Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
                Portals.open(p, p.position().add(look.scale(2.0)), look, to.key(), dest, (int) (300 * power), c);
                yield true;
            }
            case UNMAKING -> {
                LivingEntity t = looked(p, 16);
                if (!(t instanceof ServerPlayer victim) || !enemy(p, t) || !Powers.power(victim).hasPowers()) {
                    say(p, "message.enhanced.spell.unmaking.no");
                    yield false;
                }
                int ticks = (int) (20 * (8 + Magic.billyRank(p)) * (Magic.isDemiurge(p) ? 1.5 : 1.0));
                CustomHeroes.suppress(victim, ticks);
                victim.addEffect(new MobEffectInstance(ModEffects.DRAINED, ticks, 0, false, true, true), p);
                MagicFx.send(level, MagicFx.SHATTER, mid(victim), List.of(), c, 0xCFEFFF, 2.0F, 20);
                MagicFx.send(level, MagicFx.BIND, victim.position(), List.of(victim.position()), c, 0xFFFFFF, victim.getBbHeight(), ticks);
                victim.displayClientMessage(Component.translatable("message.enhanced.spell.unmade").withStyle(ChatFormatting.AQUA), false);
                say(p, "message.enhanced.spell.unmaking", victim.getDisplayName(), ticks / 20);
                sound(level, victim.position(), SoundEvents.BEACON_DEACTIVATE, 1.2F, 0.5F);
                yield true;
            }
            case GRANT_MAGIC -> {
                Grants.openScreen(p);
                yield true;
            }
        };
    }

    // ---- the shared parts ----

    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
    }

    /** Bands of light hold a creature where it stands. */
    private static void bind(ServerPlayer p, LivingEntity t, int ticks, int color) {
        if (!t.getType().is(ModTags.SPELLBOUND_IMMUNE)) {
            t.addEffect(new MobEffectInstance(ModEffects.SPELLBOUND, ticks, 0, false, false, true), p);
        }
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true, true), p);
        if (t instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        MagicFx.send(p.serverLevel(), MagicFx.BIND, t.position(), List.of(t.position()), color, 0xFFFFFF, t.getBbHeight(), ticks);
    }

    /** Takes away the bad effects (curses: the mind's too, when {@code all}). */
    static void cure(LivingEntity e, boolean all) {
        List<net.minecraft.core.Holder<MobEffect>> bad = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) {
            if (!i.getEffect().value().isBeneficial() && (all || i.getEffect().value() != ModEffects.DRAINED.get())) {
                bad.add(i.getEffect());
            }
        }
        for (var h : bad) {
            e.removeEffect(h);
        }
        e.clearFire();
    }

    /** The Unbinding ("Dimitte eam..."): mind control, stuns, curses and a Hex's runes let go. True if anything held him. */
    static boolean unbind(LivingEntity e) {
        boolean held = false;
        for (var effect : List.of(ModEffects.SPELLBOUND.holder(), ModEffects.CONFUSED.holder(), ModEffects.DRAINED.holder())) {
            if (e.hasEffect(effect)) {
                e.removeEffect(effect);
                held = true;
            }
        }
        for (var effect : List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.LEVITATION, MobEffects.BLINDNESS, MobEffects.DARKNESS,
                MobEffects.WEAKNESS, MobEffects.UNLUCK, MobEffects.CONFUSION)) {
            if (e.hasEffect(effect)) {
                e.removeEffect(effect);
                held = true;
            }
        }
        if (MindLink.isHost(e) && e.getServer() != null) {
            ServerPlayer xavier = MindLink.controller(e);
            if (xavier != null) {
                MindLink.end(e.getServer(), xavier.getUUID(), MindLink.End.LET_GO);
                held = true;
            }
        }
        for (String curse : List.of("cursed_hand", "transmuted", "dream_bound")) {
            if (MagicZones.has(e, curse)) {
                MagicZones.unbuff(e, curse);
                held = true;
            }
        }
        MagicZones.zones().removeIf(z -> (z.kind == MagicZones.Kind.SOUL_BIND || z.kind == MagicZones.Kind.HALLWAY)
                && e.getUUID().equals(z.target));
        if (e instanceof ServerPlayer sp) {
            Magic.set(sp, Magic.data(sp).with(MagicData.UNBOUND));
            MagicEvents.unboundUntil(sp, sp.level().getGameTime() + 1200);
            if (Darkness.cleanse(sp, 20)) {
                held = true;
            }
            if (Realms.dreaming(sp)) {
                Realms.wake(sp);
                held = true;
            }
        }
        return held;
    }

    /** A wall in front of the caster: thorns (leaves and berry bushes) or solid light. */
    private static boolean wall(ServerPlayer p, int color, boolean thorns) {
        ServerLevel level = p.serverLevel();
        Direction facing = p.getDirection();
        BlockPos base = p.blockPosition().relative(facing, 3);
        Direction side = facing.getClockWise();
        long until = level.getGameTime() + tempTicks();
        int placed = 0;
        for (int w = -2; w <= 2; w++) {
            for (int h = 0; h < (thorns ? 3 : 4); h++) {
                BlockPos at = base.relative(side, w).above(h);
                BlockState state = thorns
                        ? (h == 0 ? Blocks.SWEET_BERRY_BUSH.defaultBlockState() : Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true))
                        : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
                if (TempBlocks.place(level, at, state, until)) {
                    placed++;
                }
            }
        }
        if (placed == 0) {
            return nothing(p);
        }
        MagicFx.burst(level, Vec3.atCenterOf(base.above()), color, 30, 1.2, 1.0F);
        sound(level, Vec3.atCenterOf(base), thorns ? SoundEvents.SWEET_BERRY_BUSH_PLACE : SoundEvents.AMETHYST_BLOCK_PLACE, 1.0F, 0.8F);
        return true;
    }

    /** Spatial folding: what he looks at (a creature, a player, an item) comes to his hand. */
    private static boolean fold(ServerPlayer p, int color) {
        ServerLevel level = p.serverLevel();
        EntityHitResult hit = Raycasts.entity(p, 32, 0.6, e -> (e instanceof LivingEntity || e instanceof ItemEntity) && e != p
                && e.isAlive() && !e.getType().is(ModTags.BOSSES), 1.0F);
        if (hit == null) {
            return nothing(p);
        }
        Entity e = hit.getEntity();
        Vec3 to = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
        if (e instanceof Player other && !Magic.friend(p, other) && !Magic.enemy(p, other)) {
            return nothing(p);
        }
        MagicFx.send(level, MagicFx.VORTEX, e.position(), List.of(), color, 0xFFFFFF, 1.2F, 10);
        Vec3 from = e.position();
        e.teleportTo(to.x, p.getY(), to.z);
        e.setDeltaMovement(Vec3.ZERO);
        e.hurtMarked = true;
        MagicFx.line(level, from.add(0, 0.5, 0), to.add(0, 0.5, 0), color, 0.8F);
        sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.6F);
        return true;
    }

    /** Transmutation: a creature's weapons and armour turn to butterflies; a player's gear to glass and lead. */
    private static void transmute(ServerPlayer p, LivingEntity t, int color) {
        ServerLevel level = p.serverLevel();
        if (t instanceof Player) {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, true, true), p);
            t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 1, false, true, true), p);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, true, true), p);
            MagicZones.mark(t, "transmuted", 200);
            if (t instanceof ServerPlayer victim) {
                victim.displayClientMessage(Component.translatable("message.enhanced.spell.transmuted").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() != EquipmentSlot.Type.ANIMAL_ARMOR && !t.getItemBySlot(slot).isEmpty()) {
                    t.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true, true), p);
        }
        // the butterflies
        for (int i = 0; i < 24; i++) {
            int hue = i % 3 == 0 ? 0xFFC8F0 : i % 3 == 1 ? 0xC8E8FF : 0xFFF0A8;
            level.sendParticles(new com.kolodkingal.enhanced.particle.EnergyParticleOptions(hue, 0.9F), t.getX(), t.getY() + 1, t.getZ(),
                    1, 0.6, 0.6, 0.6, 0.15);
        }
        MagicFx.send(level, MagicFx.STARBURST, mid(t), List.of(), color, 0xFFFFFF, 2.0F, 16);
        sound(level, t.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 1.4F);
    }

    /** Where Billy's gate leads from a world. */
    record ResourceDim(net.minecraft.resources.ResourceKey<Level> key) {
        static ResourceDim gateFor(ServerLevel from) {
            if (from.dimension() == Level.OVERWORLD) {
                return new ResourceDim(Level.NETHER);
            }
            return new ResourceDim(Level.OVERWORLD);
        }
    }
}
