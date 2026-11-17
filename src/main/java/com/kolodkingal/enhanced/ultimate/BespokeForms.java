package com.kolodkingal.enhanced.ultimate;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.custom.Shots;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.magic.Magic;
import com.kolodkingal.enhanced.magic.MagicFx;
import com.kolodkingal.enhanced.magic.Spell;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Three ultimate forms with rules of their own (3.17, Ultimate Mode only; with the mode off none of this runs and no
 * modifier of it is on anyone):
 * <ul>
 *   <li><b>Cosmic Spider-Man</b> (Captain Universe): three times the Hulk's strength at full rage, flight at great speed,
 *   Netherite's protection, twice a man's healing, energy blasts from his hands (the signature) or his eyes (with Sneak),
 *   matter and portals as spells (two thirds of Billy's strength), a sharper spider-sense. The Uni-Power is a meter of
 *   {@link #COSMIC_ENERGY}: it only runs down while the form lasts, and when it is empty he is Spider-Man again; it climbs
 *   back out of the form and must be full to awaken it. It is life itself: dark magic hurts him half as much again.</li>
 *   <li><b>The Godkiller</b> (Iron Man): the armour as tall as the game lets a body be ({@link #GODKILLER_SCALE} times a
 *   man), struck anywhere on that body, reaching what stands under it; more than Netherite's protection (best against
 *   blows and energy, a little less against magic); his suit's energy lasts as 230 would; great strength; the giant
 *   repulsor cannons of the chest and hands (the signature), the stamp under him (with Sneak).</li>
 *   <li><b>Ultimate Tommy</b>: while his speed or his slow time is on, every other player in his world is slowed three
 *   times as much as his slow time slows, their flight too; Netherite's protection; the great tornado that stops what is
 *   shot or cast into it (the signature); time quickened on himself or a friend, healing for hunger (with Sneak); spells
 *   hurt him less and he is immune to time's powers; near the Scarlet Witch or Billy he feeds their magic; his running
 *   costs no extra hunger.</li>
 * </ul>
 */
public final class BespokeForms {
    /** The Uni-Power's meter. */
    public static final int COSMIC_ENERGY = 170;
    /** Ticks one point of it lasts in the form (a little over four minutes in all), and takes to come back out of it. */
    private static final int COSMIC_DRAIN_TICKS = 30;
    private static final int COSMIC_REFILL_TICKS = 14;
    /** How many times a man's size the Godkiller is: the most the game's scale attribute allows. */
    public static final double GODKILLER_SCALE = 16.0;
    /** The Godkiller's suit energy lasts as this much would, against the suit's own meter. */
    public static final double GODKILLER_ENERGY = 230.0;
    /** The spells the Uni-Power gives: matter made and changed, and portals. */
    private static final Set<Spell> COSMIC_SPELLS = EnumSet.of(Spell.TRANSMUTE, Spell.CONJURE_WEAPON, Spell.CONJURE_FOOD,
            Spell.CONJURE_BRIDGE, Spell.CONJURE_WALL, Spell.CONJURE_ARMOR, Spell.LIGHT_PLATFORM, Spell.SLING_PORTAL,
            Spell.COMBAT_PORTALS, Spell.DIMENSION_GATE);

    private static final Map<UUID, Float> COSMIC = new HashMap<>();
    private static final Map<UUID, Float> SUIT_ENERGY = new HashMap<>();

    /** A tornado of Ultimate Tommy's: where, how wide, whose, until when. */
    private record Tornado(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> level, Vec3 center, double radius,
                           UUID owner, long until) {
    }

    private static final List<Tornado> TORNADOES = new ArrayList<>();

    private BespokeForms() {
    }

    private static boolean form(Player player, Power power) {
        return Ultimate.on() && Powers.power(player) == power && Powers.get(player).is(Live.ULTIMATE);
    }

    /** Whether he is Cosmic Spider-Man now (both sides). */
    public static boolean cosmic(Player player) {
        return form(player, Power.SPIDER_MAN);
    }

    /** Whether he is in the Godkiller armour now (both sides). */
    public static boolean godkiller(Player player) {
        return form(player, Power.IRON_MAN);
    }

    /** Whether he is Ultimate Tommy now (both sides). */
    public static boolean tommy(Player player) {
        return form(player, Power.TOMMY);
    }

    // ---- what the rest of the mod asks ----

    /** Whether the form itself carries him through the air (Cosmic Spider-Man). */
    public static boolean flies(Player player) {
        return cosmic(player);
    }

    /** The Uni-Power's spells. */
    public static boolean cosmicSpell(Spell spell) {
        return COSMIC_SPELLS.contains(spell);
    }

    /** The Uni-Power he has now (server; full for someone never yet in the form). */
    public static float cosmicEnergy(Player player) {
        return COSMIC.getOrDefault(player.getUUID(), (float) COSMIC_ENERGY);
    }

    /** Whether Spider-Man may awaken the form: the Uni-Power is full again. */
    static boolean cosmicReady(ServerPlayer player) {
        return player.getAbilities().instabuild || cosmicEnergy(player) >= COSMIC_ENERGY;
    }

    /** How many times as fast this player's flight is now: a third while an Ultimate Tommy slows his world. */
    public static float flightFactor(Player player) {
        return slowedByTommy(player) ? 1.0F / 3.0F : 1.0F;
    }

    /** How many times the hunger of his running Tommy pays: none of it in his ultimate form. */
    public static float runHunger(Player player) {
        return tommy(player) ? 0.0F : 1.0F;
    }

    /** Whether no power of time touches him (Ultimate Tommy): a Time Stone's slow, another speedster's slow time. */
    public static boolean timeProof(Player player) {
        return tommy(player);
    }

    /** Whether an Ultimate Tommy in this player's world has his speed on: everyone else there is slowed. */
    public static boolean slowedByTommy(Player player) {
        if (!Ultimate.on() || player.isSpectator() || player.isCreative()) {
            return false;
        }
        for (Player other : player.level().players()) {
            if (other != player && tommy(other) && !timeProof(player)) {
                PowerData data = Powers.get(other);
                if (data.superSpeed() || data.slowMo()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- every tick of every player (server) ----

    private static final ResourceLocation ARMOR = Enhanced.id("ultimate_armor");
    private static final ResourceLocation TOUGHNESS = Enhanced.id("ultimate_toughness");
    private static final ResourceLocation STRENGTH = Enhanced.id("ultimate_strength");
    private static final ResourceLocation KNOCKBACK = Enhanced.id("ultimate_knockback");
    private static final ResourceLocation SCALE = Enhanced.id("godkiller_scale");
    private static final ResourceLocation STEP = Enhanced.id("godkiller_step");
    private static final ResourceLocation REACH = Enhanced.id("godkiller_reach");
    private static final ResourceLocation BLOCK_REACH = Enhanced.id("godkiller_block_reach");

    public static void tick(ServerPlayer player) {
        boolean cosmic = cosmic(player);
        boolean godkiller = godkiller(player);
        boolean tommy = tommy(player);
        // their bodies: Netherite's protection (the Godkiller's a little more), their strength
        set(player, Attributes.ARMOR, ARMOR, godkiller ? 24.0 : 20.0, cosmic || godkiller || tommy, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS, godkiller ? 14.0 : 12.0, cosmic || godkiller || tommy,
                AttributeModifier.Operation.ADD_VALUE);
        // (the Hulk at full rage strikes for 7 more than a man: three times that; the Godkiller's fist more still)
        set(player, Attributes.ATTACK_DAMAGE, STRENGTH, godkiller ? 34.0 : 21.0, cosmic || godkiller, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, 1.0, godkiller, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.SCALE, SCALE, GODKILLER_SCALE - 1.0, godkiller, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(player, Attributes.STEP_HEIGHT, STEP, 9.0, godkiller, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.ENTITY_INTERACTION_RANGE, REACH, 66.0, godkiller, AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH, 66.0, godkiller, AttributeModifier.Operation.ADD_VALUE);
        if (!Ultimate.on()) {
            return;
        }
        long now = player.level().getGameTime();
        if (Powers.power(player) == Power.SPIDER_MAN) {
            cosmicTick(player, cosmic, now);
        }
        if (godkiller) {
            // the suit's energy lasts as 230 would: part of what each tick took is given back
            float energy = SuitEnergy.get(player);
            Float before = SUIT_ENERGY.get(player.getUUID());
            if (before != null && energy < before) {
                energy += (before - energy) * (float) (1.0 - SuitEnergy.max() / GODKILLER_ENERGY);
                SuitEnergy.set(player, energy);
            }
            SUIT_ENERGY.put(player.getUUID(), energy);
        } else {
            SUIT_ENERGY.remove(player.getUUID());
        }
        if (tommy) {
            tommyTick(player, now);
        }
    }

    private static void cosmicTick(ServerPlayer player, boolean cosmic, long now) {
        float energy = cosmicEnergy(player);
        if (cosmic) {
            if (!player.getAbilities().instabuild && now % COSMIC_DRAIN_TICKS == 0) {
                energy = Math.max(0.0F, energy - 1.0F);
            }
            // twice a man's healing; the spider-sense, sharper: what means him harm shows through walls, to him
            if (player.isAlive() && now % 40 == 0 && player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F);
            }
            if (now % 40 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, false));
                for (LivingEntity e : UltimateForms.targets(player, 40.0)) {
                    if (e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof Player) {
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, false), player);
                    }
                }
            }
            if (energy <= 0.0F) {
                COSMIC.put(player.getUUID(), 0.0F);
                player.displayClientMessage(Component.translatable("message.enhanced.cosmic.spent").withStyle(ChatFormatting.GRAY), true);
                UltimateForms.end(player, false);
                return;
            }
        } else if (energy < COSMIC_ENERGY && now % COSMIC_REFILL_TICKS == 0) {
            energy = Math.min(COSMIC_ENERGY, energy + 1.0F);
        }
        COSMIC.put(player.getUUID(), energy);
        // (the meter rides in the hero's synced charge, which Spider-Man has no other use for: his HUD reads it)
        PowerData data = Powers.get(player);
        if (data.ironCharge() != Math.round(energy)) {
            data.setIronCharge(Math.round(energy));
        }
    }

    private static void tommyTick(ServerPlayer player, long now) {
        ServerLevel level = player.serverLevel();
        PowerData data = Powers.get(player);
        if ((data.superSpeed() || data.slowMo()) && now % 5 == 0) {
            // his speed is the whole world's slowness: three times what his slow time does to those beside him
            for (ServerPlayer other : level.players()) {
                if (other != player && !other.isSpectator() && !other.isCreative() && !timeProof(other)) {
                    other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 5, true, false, true), player);
                    other.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 12, 2, true, false, false), player);
                }
            }
        }
        if (now % 20 == 0) {
            // the cosmic battery: the Scarlet Witch and Billy near him are fed (their magic fuller, their powers harder)
            for (ServerPlayer other : level.players()) {
                Power power = Powers.power(other);
                if (other != player && (power == Power.SCARLET_WITCH || power == Power.BILLY) && other.distanceToSqr(player) < 16 * 16) {
                    other.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, true, false, true), player);
                    other.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true), player);
                    if (Magic.isCaster(other)) {
                        Magic.set(other, Magic.data(other).withMana(Math.min(Magic.maxMana(other), Magic.data(other).mana() + 12.0F)));
                    }
                    MagicFx.line(level, player.position().add(0, 1, 0), other.position().add(0, 1, 0), 0xA8F0D4, 0.5F);
                }
            }
        }
    }

    /** Every tick of the server: the tornadoes turn, throw out what comes into them, and end. */
    public static void serverTick(net.minecraft.server.MinecraftServer server) {
        if (TORNADOES.isEmpty()) {
            return;
        }
        Iterator<Tornado> it = TORNADOES.iterator();
        while (it.hasNext()) {
            Tornado t = it.next();
            ServerLevel level = server.getLevel(t.level());
            if (level == null || level.getGameTime() >= t.until()) {
                it.remove();
                continue;
            }
            long now = level.getGameTime();
            ServerPlayer owner = server.getPlayerList().getPlayer(t.owner());
            for (Entity e : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(t.center(), t.center()).inflate(t.radius(), 24, t.radius()))) {
                Vec3 to = e.position().subtract(t.center());
                double flat = Math.sqrt(to.x * to.x + to.z * to.z);
                if (flat > t.radius() || e == owner) {
                    continue;
                }
                if (e instanceof Projectile projectile && (owner == null || projectile.getOwner() != owner)) {
                    projectile.discard(); // (nothing shot gets through the wind)
                } else if (e instanceof LivingEntity living && owner != null && Magic.enemy(owner, living)) {
                    // round and up and out
                    Vec3 round = new Vec3(-to.z, 0, to.x).normalize().scale(0.55);
                    Vec3 out = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0, to.z / flat).scale(0.18);
                    living.setDeltaMovement(round.add(out).add(0, 0.32, 0));
                    living.hurtMarked = true;
                    if (now % 10 == 0) {
                        living.hurt(ModDamageTypes.source(level, ModDamageTypes.ULTIMATE, owner, owner), 6.0F);
                    }
                }
            }
            if (now % 12 == 0) {
                MagicFx.send(level, MagicFx.VORTEX, t.center(), List.of(), 0x2E9C7E, 0xA8F0D4, (float) t.radius(), 16);
                level.playSound(null, t.center().x, t.center().y, t.center().z, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.9F, 0.6F);
            }
        }
    }

    // ---- blows ----

    /** How many times as much of a blow reaches this victim, for the forms' weaknesses and strengths. */
    public static float taken(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof Player player) || !Ultimate.on()) {
            return 1.0F;
        }
        boolean magic = source.is(ModTags.MAGIC_DAMAGE);
        if (cosmic(player)) {
            return source.is(ModTags.DARK_MAGIC) ? 1.5F : 1.0F;
        }
        if (godkiller(player)) {
            return magic ? 1.1F : 0.7F;
        }
        if (tommy(player)) {
            return magic ? 0.6F : 1.0F;
        }
        return 1.0F;
    }

    /** How many times as hard this attacker's own powers strike, for his form. */
    public static float dealt(Player attacker) {
        return godkiller(attacker) ? 2.5F : cosmic(attacker) ? 1.5F : 1.0F;
    }

    /** Whether a tornado of Ultimate Tommy's stops this blow: something shot or cast from outside it at someone inside. */
    public static boolean blockedByTornado(LivingEntity victim, DamageSource source) {
        if (TORNADOES.isEmpty() || source.getEntity() == null) {
            return false;
        }
        boolean ranged = source.getDirectEntity() != source.getEntity() || source.is(ModTags.MAGIC_DAMAGE) || source.is(ModTags.ENERGY_DAMAGE);
        if (!ranged) {
            return false;
        }
        for (Tornado t : TORNADOES) {
            if (t.level() == victim.level().dimension() && !source.getEntity().getUUID().equals(t.owner())
                    && flat(victim.position(), t.center()) <= t.radius() && flat(source.getEntity().position(), t.center()) > t.radius()) {
                return true;
            }
        }
        return false;
    }

    private static double flat(Vec3 a, Vec3 b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
    }

    // ---- the signature moves ----

    /** The form's own signature move, if it has one (else false: the form's usual one). */
    static boolean signature(ServerPlayer player, boolean sneak) {
        ServerLevel level = player.serverLevel();
        if (cosmic(player)) {
            float cost = sneak ? 4.0F : 6.0F;
            COSMIC.put(player.getUUID(), Math.max(0.0F, cosmicEnergy(player) - (player.getAbilities().instabuild ? 0.0F : cost)));
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            double reach = sneak ? 64.0 : 44.0;
            Vec3 end = eye.add(look.scale(reach));
            int color = sneak ? 0xFFFFFF : 0x7FD8FF;
            if (sneak) {
                MagicFx.send(level, MagicFx.BEAMS, eye.add(look.scale(0.4)), List.of(end), color, 0x9FE0FF, 1.0F, 16);
            } else {
                Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize().scale(0.45);
                MagicFx.send(level, MagicFx.BEAMS, eye.add(side).add(0, -0.5, 0), List.of(end), color, 0xFFFFFF, 1.6F, 18);
                MagicFx.send(level, MagicFx.BEAMS, eye.subtract(side).add(0, -0.5, 0), List.of(end), color, 0xFFFFFF, 1.6F, 18);
            }
            strikeAlong(player, eye, look, reach, sneak ? 1.2 : 2.2, sneak ? 14.0F : 24.0F, 0.6);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, sneak ? 2.0F : 1.4F);
            return true;
        }
        if (godkiller(player)) {
            if (!SuitEnergy.spend(player, sneak ? 6.0 : 12.0)) {
                return true;
            }
            if (sneak) {
                // the stamp: everything under him, all the way down
                Vec3 at = player.position();
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0, 0, 6.0).expandTowards(0, -70, 0),
                        e -> e != player && Magic.enemy(player, e))) {
                    e.hurt(ModDamageTypes.source(level, ModDamageTypes.ULTIMATE, player, player), 44.0F);
                    Shots.push(e, at, 1.6, 0.6);
                }
                MagicFx.send(level, MagicFx.RING, at, List.of(), 0xFFC83A, 0xFFF3C2, 22.0F, 24);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.5F);
                return true;
            }
            // the cannons of the chest and both hands
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            double h = player.getBbHeight();
            Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize().scale(player.getBbWidth() * 0.9);
            Vec3 chest = player.position().add(0, h * 0.7, 0).add(look.scale(player.getBbWidth() * 0.5));
            Vec3 end = eye.add(look.scale(140.0));
            MagicFx.send(level, MagicFx.BEAMS, chest, List.of(end), 0xFFD86A, 0xFFFFFF, 9.0F, 24);
            MagicFx.send(level, MagicFx.BEAMS, chest.add(side).add(0, -h * 0.2, 0), List.of(end), 0xFFD86A, 0xFFFFFF, 6.0F, 24);
            MagicFx.send(level, MagicFx.BEAMS, chest.subtract(side).add(0, -h * 0.2, 0), List.of(end), 0xFFD86A, 0xFFFFFF, 6.0F, 24);
            MagicFx.send(level, MagicFx.STARBURST, end, List.of(), 0xFFD86A, 0xFFFFFF, 14.0F, 24);
            strikeAlong(player, chest, end.subtract(chest).normalize(), 140.0, 5.0, 60.0F, 1.5);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 0.6F);
            return true;
        }
        if (tommy(player)) {
            if (sneak) {
                // time quickened on a friend he looks at, or on himself: a great healing, paid in hunger
                LivingEntity target = player;
                Vec3 eye = player.getEyePosition();
                for (Player other : level.players()) {
                    if (other != player && other.distanceToSqr(player) < 8 * 8 && Magic.friend(player, other)
                            && other.position().add(0, 1, 0).subtract(eye).normalize().dot(player.getLookAngle()) > 0.9) {
                        target = other;
                    }
                }
                if (!player.getAbilities().instabuild && player.getFoodData().getFoodLevel() < 4) {
                    player.displayClientMessage(Component.translatable("message.enhanced.tommy.too_hungry").withStyle(ChatFormatting.GRAY), true);
                    return true;
                }
                if (!player.getAbilities().instabuild) {
                    player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 4);
                }
                target.heal(14.0F);
                target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2, true, true, true), player);
                MagicFx.sendOn(level, MagicFx.AWAKEN, target, List.of(), 0x2E9C7E, 0xA8F0D4, 1.0F, 24);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.8F);
                return true;
            }
            TORNADOES.removeIf(t -> t.owner().equals(player.getUUID()));
            TORNADOES.add(new Tornado(level.dimension(), player.position(), 9.0, player.getUUID(), level.getGameTime() + 200));
            MagicFx.send(level, MagicFx.VORTEX, player.position(), List.of(), 0x2E9C7E, 0xA8F0D4, 9.0F, 16);
            MagicFx.send(level, MagicFx.RING, player.position(), List.of(), 0x2E9C7E, 0xA8F0D4, 9.0F, 20);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.4F, 0.5F);
            return true;
        }
        return false;
    }

    /** Strikes whatever stands within {@code width} of the line from {@code from} along {@code dir}. */
    private static void strikeAlong(ServerPlayer player, Vec3 from, Vec3 dir, double reach, double width, float damage, double push) {
        ServerLevel level = player.serverLevel();
        Vec3 end = from.add(dir.scale(reach));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(from, end).inflate(width + 1.0),
                e -> e != player && Magic.enemy(player, e))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(from);
            double along = to.dot(dir);
            if (along < 0 || along > reach || to.subtract(dir.scale(along)).length() > width + e.getBbWidth() * 0.5) {
                continue;
            }
            e.invulnerableTime = 0;
            e.hurt(ModDamageTypes.source(level, ModDamageTypes.ULTIMATE, player, player), damage);
            Shots.push(e, from, push, 0.3);
        }
    }

    // ---- lifecycle ----

    public static void forget(UUID id) {
        SUIT_ENERGY.remove(id);
        TORNADOES.removeIf(t -> t.owner().equals(id));
    }

    public static void clear() {
        COSMIC.clear();
        SUIT_ENERGY.clear();
        TORNADOES.clear();
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present,
                            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }
}
