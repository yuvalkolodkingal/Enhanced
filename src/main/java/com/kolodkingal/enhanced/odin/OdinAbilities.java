package com.kolodkingal.enhanced.odin;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.ApocalypseGuard;
import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.MjolnirPin;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.asgard.AsgardProgress;
import com.kolodkingal.enhanced.asgard.npc.Asgardians;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.boss.BossFights;
import com.kolodkingal.enhanced.config.OdinConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.item.GungnirItem;
import com.kolodkingal.enhanced.network.AsgardFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.power.Toggle;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Odin, the Allfather (3.5): the king of Asgard, who wields Gungnir and the Odinforce. A powerless player who beat Hela,
 * took Gungnir from its stand in the vault and sat on the Throne of Asgard holding it becomes him ({@link Coronation}).
 * The plug-in is {@link OdinServer}; the Odinforce Shield's damage hook is {@link OdinEvents}.
 *
 * <p>R is the Gungnir Blast (a golden bolt from the spear's tip), Sneak + R the Gungnir Throw (the spear flies and comes
 * home: {@link GungnirEntity}; R again calls it back); G held raises the Odinforce Shield, Sneak + G is the Odinforce Heal;
 * V banishes the creature he looks at, Sneak + V is the Odinforce Wave; B sends Huginn and Muninn ({@link RavenEntity}) to
 * circle where he looks, Sneak + B the Ravens' Report round him; N is the Allfather's Command (the warriors of Asgard, or
 * spectral Einherjar elsewhere, fight his target), Sneak + N the Odinsleep. He walks; the Bifrost carries him (his own
 * Gungnir is its key).
 *
 * <p>3.4's rules from birth: every cooldown is by its row ({@code data.startCooldown(HeroAbility.X, ...)}), the Sneak
 * variant is {@code alt}, the tick's parts are guarded by {@code data.has(row)} (a custom hero with any of his rows ticks
 * here), his body is by {@link HeroAbility#ODIN_BODY} and its {@code bodyScale}, the colours of an ability's effects are
 * {@link Powers#color}/{@link Powers#glow} of the player. A custom hero fires the blast from his hand and throws only a
 * Gungnir he holds (else the blast); the hero himself needs Gungnir in hand for both.
 */
public final class OdinAbilities {
    private static final ResourceLocation BODY_HEALTH = Enhanced.id("odin_health");
    private static final ResourceLocation BODY_ARMOR = Enhanced.id("odin_armor");
    private static final ResourceLocation BODY_KNOCKBACK = Enhanced.id("odin_knockback");
    private static final ResourceLocation SLEEP_SPEED = Enhanced.id("odinsleep_speed");
    private static final ResourceLocation SLEEP_JUMP = Enhanced.id("odinsleep_jump");

    /** The Allfather's gold (his blasts, his shield, his light) and its pale core. */
    public static final int GOLD = 0xFFC23A;
    public static final int WHITE_GOLD = 0xFFF0C2;

    /** The body of the Allfather. */
    public static final double BODY_HEALTH_BONUS = 12.0;
    public static final double BODY_ARMOR_BONUS = 6.0;
    public static final double BODY_KNOCKBACK_RESISTANCE = 0.5;

    /** How far the Gungnir Blast reaches, and the radius of its splash. */
    public static final double BLAST_RANGE = 48.0;
    public static final double BLAST_SPLASH = 1.5;
    /** The cooldown (ticks) after a new Gungnir comes to his hand (his own was lost). */
    public static final int RECALL_COOLDOWN = 200;

    /** The Odinforce Shield: its radius, how long it takes to form, how much it can take. */
    public static final double SHIELD_RADIUS = 3.5;
    public static final int SHIELD_WIND_UP = 6;
    public static final float SHIELD_STRENGTH = 60.0F;
    /** After the shield broke or was held to its limit, it rests longer (times the cooldown). */
    public static final double SHIELD_BROKEN_FACTOR = 1.5;

    /** The Odinforce Heal reaches this far. */
    public static final double HEAL_RADIUS = 8.0;
    /** Banish reaches this far; what it cannot banish takes this much instead. */
    public static final double BANISH_RANGE = 32.0;
    public static final float BANISH_RESISTED_DAMAGE = 20.0F;
    /** The Odinforce Wave's radius and its push. */
    public static final double WAVE_RADIUS = 8.0;
    public static final double WAVE_PUSH = 1.6;

    /** Huginn and Muninn fly to where he looks, up to this far, and circle at this radius. */
    public static final double RAVENS_RANGE = 48.0;
    public static final double RAVENS_RADIUS = 16.0;
    /** The Ravens' Report: they circle him at this radius for this long, and its cooldown. */
    public static final double REPORT_RADIUS = 16.0;
    public static final int REPORT_SECONDS = 10;
    public static final int REPORT_COOLDOWN = 900;

    /** The Allfather's Command: the target he looks at (this far), how long the warriors follow it. */
    public static final double COMMAND_RANGE = 48.0;
    public static final int COMMAND_TICKS = 1200;
    public static final int SPECTRAL_COUNT = 4;
    public static final int SPECTRAL_TICKS = 600;

    /** The Odinsleep heals this share of his health every second. */
    public static final float SLEEP_HEAL_SHARE = 0.05F;

    /** How far the effects are sent. */
    private static final double FX_RANGE = 96.0;

    // The kinds of AsgardFxPayload of Odin's lane (300-399).
    public static final int FX_BLAST = AsgardFxPayload.ODIN;
    public static final int FX_HEAL = AsgardFxPayload.ODIN + 1;
    public static final int FX_BANISH = AsgardFxPayload.ODIN + 2;
    public static final int FX_WAVE = AsgardFxPayload.ODIN + 3;
    public static final int FX_COMMAND = AsgardFxPayload.ODIN + 4;
    public static final int FX_CORONATION = AsgardFxPayload.ODIN + 5;
    public static final int FX_CROWNED = AsgardFxPayload.ODIN + 6;
    public static final int FX_GUNGNIR_GIVEN = AsgardFxPayload.ODIN + 7;
    public static final int FX_VAULT_OPEN = AsgardFxPayload.ODIN + 8;
    public static final int FX_SHIELD_BREAK = AsgardFxPayload.ODIN + 9;

    /** What each Odin carries from tick to tick; forgotten when he leaves or the server stops. */
    private static final Map<UUID, State> STATES = new HashMap<>();

    static final class State {
        /** The spear he threw, while it flies. */
        @Nullable
        GungnirEntity spear;
        /** His two ravens, while they fly. */
        final List<RavenEntity> ravens = new ArrayList<>();
        /** When the Odinsleep began (game time). */
        long sleepStart;
        /** The last tick the shield's stop rang (a beam hits every few ticks). */
        long lastTink = Long.MIN_VALUE / 2;
    }

    static State state(UUID id) {
        return STATES.computeIfAbsent(id, k -> new State());
    }

    static {
        // His ravens are his: his own area attacks spare them.
        AreaTargets.addAllyRule((owner, target) -> target instanceof RavenEntity raven && owner instanceof Player p && raven.ownedBy(p));
    }

    // ---- the numbers ----

    private static boolean loaded() {
        return EnhancedConfig.SPEC.isLoaded();
    }

    public static float blastDamage() {
        return loaded() ? OdinConfig.BLAST_DAMAGE.get().floatValue() : 12.0F;
    }

    public static float throwDamage() {
        return loaded() ? OdinConfig.THROW_DAMAGE.get().floatValue() : 16.0F;
    }

    /** How long (ticks) a complete Odinforce Shield can be held. */
    public static int shieldMaxTicks() {
        return 20 * (loaded() ? OdinConfig.SHIELD_SECONDS.get() : 12);
    }

    public static float shieldStrength() {
        return SHIELD_STRENGTH;
    }

    public static int ravensSeconds() {
        return loaded() ? OdinConfig.RAVENS_SECONDS.get() : 20;
    }

    public static int odinsleepMaxTicks() {
        return 20 * (loaded() ? OdinConfig.ODINSLEEP_MAX_SECONDS.get() : 30);
    }

    // ---- input ----

    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down) {
        onKey(player, data, ability, down, player.isShiftKeyDown());
    }

    /**
     * A key of his, with its Sneak variant said explicitly ({@code alt}): Odin's own Sneak, or the binding of a custom hero
     * with his abilities (3.4).
     */
    static void onKey(ServerPlayer player, PowerData data, int ability, boolean down, boolean alt) {
        if (!down) {
            if (ability == 2 && data.chaosShieldActive() && data.has(HeroAbility.ODIN_ODINFORCE)) {
                dropShield(player, data, false);
            }
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        if (data.toggle(Toggle.ODINSLEEP) && data.has(HeroAbility.ODIN_COMMAND)) {
            // Asleep he does nothing but wake (N again).
            if (ability == 5) {
                endSleep(player, data, "message.enhanced.odin.sleep_woken");
            } else {
                player.displayClientMessage(Component.translatable("message.enhanced.odin.asleep"), true);
            }
            return;
        }
        switch (ability) {
            case 1 -> {
                if (!recallSpear(player)) {
                    if (alt) {
                        throwSpear(player, data);
                    } else {
                        blast(player, data);
                    }
                }
            }
            case 2 -> {
                if (alt) {
                    heal(player, data);
                } else {
                    raiseShield(player, data);
                }
            }
            case 3 -> {
                if (alt) {
                    wave(player, data);
                } else {
                    banish(player, data);
                }
            }
            case 4 -> ravens(player, data, alt);
            case 5 -> {
                if (alt) {
                    startSleep(player, data);
                } else {
                    command(player, data);
                }
            }
            default -> {
            }
        }
    }

    // ---- every tick ----

    static void tick(ServerPlayer player, PowerData data) {
        // (3.4: each part for the ability it belongs to: a custom hero ticks here with any of Odin's abilities.)
        if (data.chaosShieldActive() && data.has(HeroAbility.ODIN_ODINFORCE)) {
            shieldTick(player, data);
        }
        State state = STATES.get(player.getUUID());
        if (data.has(HeroAbility.ODIN_GUNGNIR)) {
            boolean flying = state != null && state.spear != null && state.spear.isAlive();
            if (!flying && state != null) {
                state.spear = null;
            }
            data.set(Live.ODIN_THROWN, flying);
        }
        if (data.has(HeroAbility.ODIN_RAVENS)) {
            boolean ravens = false;
            if (state != null) {
                // (Their time is kept here too: a raven out in a chunk that does not tick still leaves when it should.)
                long now = player.level().getGameTime();
                for (RavenEntity raven : state.ravens) {
                    if (raven.isAlive() && now >= raven.leavesAt()) {
                        raven.leave();
                    }
                }
                state.ravens.removeIf(r -> !r.isAlive());
                ravens = !state.ravens.isEmpty();
            }
            data.set(Live.ODIN_RAVENS, ravens);
        }
        if (data.toggle(Toggle.ODINSLEEP) && data.has(HeroAbility.ODIN_COMMAND)) {
            sleepTick(player, data);
        }
        if (data.has(HeroAbility.ODIN_BODY) && player.tickCount % 20 == 0 && player.getHealth() < player.getMaxHealth() * 0.5F
                && !player.hasEffect(MobEffects.REGENERATION)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        }
    }

    // ---- the body ----

    /**
     * The body of the Allfather (+12 health, +6 armour, +0.5 knockback resistance) while the player has it, and nothing of it
     * on anyone else. Permanent modifiers, so they are right when a player logs in.
     */
    static void applyBody(ServerPlayer player, PowerData data) {
        boolean hero = data.has(HeroAbility.ODIN_BODY);
        double scale = data.bodyScale(HeroAbility.ODIN_BODY);
        setModifier(player, Attributes.MAX_HEALTH, BODY_HEALTH, BODY_HEALTH_BONUS * scale, hero);
        setModifier(player, Attributes.ARMOR, BODY_ARMOR, BODY_ARMOR_BONUS * scale, hero);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, BODY_KNOCKBACK, Math.min(1.0, BODY_KNOCKBACK_RESISTANCE * scale), hero);
        if (!data.toggle(Toggle.ODINSLEEP) || !data.has(HeroAbility.ODIN_COMMAND)) {
            sleepModifiers(player, false);
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** Adds, changes or removes a permanent attribute modifier (as {@code PowerTicker}'s own helper does). */
    static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present) {
        setModifier(player, attribute, id, amount, present, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean present,
                                    AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (present) {
            if (existing == null || existing.amount() != amount || existing.operation() != operation) {
                instance.removeModifier(id);
                instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
            }
        } else if (existing != null) {
            instance.removeModifier(id);
        }
    }

    // ---- Ability 1: the Gungnir Blast and the Throw ----

    /** Whether he holds a Gungnir (anyone's) in either hand. */
    public static boolean holdsGungnir(Player player) {
        return GungnirItem.isGungnir(player.getMainHandItem()) || GungnirItem.isGungnir(player.getOffhandItem());
    }

    /** Whether he has a Gungnir of his own anywhere about him (either hand, his inventory, in flight). */
    public static boolean hasOwnGungnir(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.spear != null && state.spear.isAlive()) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (GungnirItem.isOwnGungnir(player, stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (GungnirItem.isOwnGungnir(player, stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a blast or a throw of this player needs Gungnir in his hand: the Allfather's do (his power is the spear's); a
     * custom hero's blast leaves his hand (3.4, R7).
     */
    public static boolean needsSpear(PowerData data) {
        return data.power() == Power.ODIN;
    }

    /**
     * R: a golden bolt from the spear's tip (or a custom hero's hand) along his look, up to {@link #BLAST_RANGE}: the first
     * creature it meets takes {@code odin.blastDamage}, the others within {@link #BLAST_SPLASH} of where it struck a little
     * less; a shield on its way stops it (and pays for it).
     *
     * @return the creatures it hurt (the first first)
     */
    public static List<LivingEntity> blast(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.ODIN_GUNGNIR, now)) {
            return List.of();
        }
        if (needsSpear(data) && !holdsGungnir(player)) {
            missingSpear(player, data);
            return List.of();
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 from = eye.add(look.scale(0.8)).add(0, -0.1, 0);
        Vec3 end = eye.add(look.scale(BLAST_RANGE));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        EntityHitResult hit = Raycasts.entityBetween(level, player, eye, end, 0.3,
                e -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator() && AreaTargets.mayHurt(player, living, true));
        LivingEntity first = hit == null ? null : (LivingEntity) hit.getEntity();
        Vec3 at = first == null ? end : hit.getLocation();
        float damage = blastDamage();
        WandaAbilities.ShieldStop shield = WandaAbilities.shieldAcross(level, player, eye, at, 1.0F);
        List<LivingEntity> hurt = new ArrayList<>();
        if (shield != null) {
            at = shield.point();
            WandaAbilities.pay(shield, player, damage);
        } else {
            if (first != null && first.hurt(ModDamageTypes.source(level, ModDamageTypes.GUNGNIR, player, player), damage)) {
                hurt.add(first);
            }
            Vec3 center = at;
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(BLAST_SPLASH + 1.0),
                    e -> e != player && e != first && e.isAlive())) {
                if (near.getBoundingBox().inflate(BLAST_SPLASH).contains(center) && AreaTargets.mayHurt(player, near, false)
                        && near.hurt(ModDamageTypes.source(level, ModDamageTypes.GUNGNIR, player, player), damage * 0.5F)) {
                    hurt.add(near);
                }
            }
        }
        data.startCooldown(HeroAbility.ODIN_GUNGNIR, now, OdinConfig.BLAST_COOLDOWN.get());
        send(level, player, FX_BLAST, List.of(from, at), Powers.glow(player));
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.GUNGNIR_BLAST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(new EnergyParticleOptions(Powers.color(player), 1.2F), at.x, at.y, at.z, 14, 0.3, 0.3, 0.3, 0.12);
        return hurt;
    }

    /**
     * Sneak + R: Gungnir leaves his hand ({@link GungnirEntity}) and comes home. A custom hero with no Gungnir in his hand
     * blasts instead (R7).
     *
     * @return the spear in flight, or null
     */
    @Nullable
    public static GungnirEntity throwSpear(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.ODIN_GUNGNIR, now)) {
            return null;
        }
        ItemStack held = player.getMainHandItem();
        if (!GungnirItem.isGungnir(held)) {
            if (needsSpear(data)) {
                missingSpear(player, data);
            } else {
                blast(player, data);
            }
            return null;
        }
        if (MjolnirPin.isPinned(player)) {
            return null;
        }
        GungnirEntity spear = new GungnirEntity(level, player, held.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        level.addFreshEntity(spear);
        state(player.getUUID()).spear = spear;
        data.set(Live.ODIN_THROWN, true);
        data.startCooldown(HeroAbility.ODIN_GUNGNIR, now, OdinConfig.THROW_COOLDOWN.get());
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.GUNGNIR_THROW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return spear;
    }

    /** R while his spear flies: it turns home at once. Returns whether there was one. */
    public static boolean recallSpear(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || state.spear == null || !state.spear.isAlive()) {
            return false;
        }
        state.spear.recall();
        return true;
    }

    /** His spear in flight, or null. */
    @Nullable
    public static GungnirEntity spear(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null || state.spear == null || !state.spear.isAlive() ? null : state.spear;
    }

    /** The spear was caught, dropped or gone: he has none in flight. */
    static void spearGone(ServerPlayer player, GungnirEntity spear) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.spear == spear) {
            state.spear = null;
        }
        PowerData data = Powers.get(player);
        if (data.is(Live.ODIN_THROWN)) {
            data.set(Live.ODIN_THROWN, false);
            Powers.syncIfDirty(player);
        }
    }

    /**
     * The Allfather has no Gungnir in his hand: if he has none of his own anywhere, a new one comes to him (his bond grows,
     * every older copy crumbles when it next ticks); otherwise he is told to take it in hand.
     */
    private static void missingSpear(ServerPlayer player, PowerData data) {
        if (hasOwnGungnir(player)) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.no_gungnir_in_hand"), true);
            return;
        }
        summonSpear(player, data);
    }

    /** A new Gungnir of his own comes to his hand (the old one was lost). */
    public static ItemStack summonSpear(ServerPlayer player, PowerData data) {
        ItemStack spear = GungnirItem.bound(player, nextBond(player));
        give(player, spear);
        ServerLevel level = player.serverLevel();
        data.startCooldown(HeroAbility.ODIN_GUNGNIR, level.getGameTime(), RECALL_COOLDOWN);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GUNGNIR_CATCH.get(), SoundSource.PLAYERS, 1.2F, 0.9F);
        send(level, player, FX_GUNGNIR_GIVEN, List.of(player.position().add(0, 1.0, 0)), 0);
        player.displayClientMessage(Component.translatable("message.enhanced.odin.gungnir_returns").withStyle(ChatFormatting.GOLD), true);
        return spear;
    }

    /** Raises the player's Gungnir bond by one (every older copy of his crumbles) and returns the new bond. */
    public static int nextBond(ServerPlayer player) {
        AsgardProgress progress = ModAttachments.ASGARD.get(player);
        int bond = progress.gungnirBond() + 1;
        ModAttachments.ASGARD.set(player, progress.withGungnirBond(bond));
        return bond;
    }

    /** Into his main hand if it is empty, else his inventory, else at his feet. */
    static void give(ServerPlayer player, ItemStack stack) {
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ---- Ability 2: the Odinforce Shield (held) and the Heal ----

    /** Whether the player's Odinforce Shield is complete: only then does it stop anything. */
    public static boolean shieldUp(Player player) {
        PowerData data = Powers.get(player);
        return data.has(HeroAbility.ODIN_ODINFORCE) && data.chaosShieldUp(player.level().getGameTime());
    }

    /** G pressed: the golden dome begins to form round him, if it has rested. */
    static void raiseShield(ServerPlayer player, PowerData data) {
        if (data.chaosShieldActive()) {
            return;
        }
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.ODIN_ODINFORCE, now)) {
            player.displayClientMessage(Component.translatable("message.enhanced.shield.recharging",
                    String.format(Locale.ROOT, "%.1f", (data.cooldownUntil(data.slot(HeroAbility.ODIN_ODINFORCE)) - now) / 20.0)), true);
            return;
        }
        data.raiseChaosShield(now, SHIELD_WIND_UP, SHIELD_STRENGTH);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ODINFORCE_SHIELD_UP.get(), SoundSource.PLAYERS,
                1.0F, 1.0F);
    }

    /** The shield comes down (G released; {@code worn}: held to its limit) and rests. */
    static void dropShield(ServerPlayer player, PowerData data, boolean worn) {
        data.setChaosShieldActive(false);
        int rest = OdinConfig.SHIELD_COOLDOWN.get();
        data.startCooldown(HeroAbility.ODIN_ODINFORCE, player.level().getGameTime(), worn ? (int) Math.round(rest * SHIELD_BROKEN_FACTOR) : rest);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ODINFORCE_SHIELD_DOWN.get(), SoundSource.PLAYERS,
                0.9F, 1.0F);
    }

    /** It took all it could: golden shards, and the longer rest. */
    static void shatterShield(ServerPlayer player, PowerData data) {
        data.setChaosShieldActive(false);
        data.setShieldStrength(0.0F);
        ServerLevel level = player.serverLevel();
        data.startCooldown(HeroAbility.ODIN_ODINFORCE, level.getGameTime(),
                (int) Math.round(OdinConfig.SHIELD_COOLDOWN.get() * SHIELD_BROKEN_FACTOR));
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        send(level, player, FX_SHIELD_BREAK, List.of(center), (int) Math.round(SHIELD_RADIUS * 10.0));
        level.playSound(null, center.x, center.y, center.z, ModSounds.ODINFORCE_SHIELD_DOWN.get(), SoundSource.PLAYERS, 1.2F, 0.7F);
        player.displayClientMessage(Component.translatable("message.enhanced.shield.shattered").withStyle(ChatFormatting.GOLD), true);
    }

    /** Every tick the shield is held: the time limit, the motes while it forms, the projectiles it turns away. */
    static void shieldTick(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now >= data.shieldFormedAt() + shieldMaxTicks()) {
            dropShield(player, data, true);
            return;
        }
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        if (!data.chaosShieldUp(now)) {
            Vec3 out = new Vec3(player.getRandom().nextGaussian(), player.getRandom().nextGaussian(), player.getRandom().nextGaussian()).normalize();
            Vec3 at = center.add(out.scale(SHIELD_RADIUS * Math.max(0.3, data.chaosShieldProgress(now))));
            level.sendParticles(new EnergyParticleOptions(Powers.glow(player), 0.9F), at.x, at.y, at.z, 0, -out.x, -out.y, -out.z, 0.1);
            return;
        }
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(SHIELD_RADIUS + 4.0),
                p -> p.getOwner() != player && !ApocalypseGuard.headingHome(p))) {
            Vec3 velocity = projectile.getDeltaMovement();
            Vec3 toPlayer = center.subtract(projectile.position());
            double reach = SHIELD_RADIUS + 0.5 + velocity.length();
            if (velocity.dot(toPlayer) > 0 && toPlayer.lengthSqr() < reach * reach) {
                float cost = WandaAbilities.projectilePower(projectile);
                WandaAbilities.neutralize(level, projectile, Powers.color(player));
                absorb(player, data, cost);
                tink(player, state(player.getUUID()), now);
                if (!data.chaosShieldActive()) {
                    return; // shattered: the rest get through
                }
            }
        }
    }

    /**
     * The damage hook, for a player whose Odinforce Shield is complete: the blow is taken off its strength, or, if it gives
     * way, the rest goes through. What only the world deals (a fall, drowning, the void) it never stops.
     *
     * @return whether the shield took the blow
     */
    static boolean guard(ServerPlayer player, LivingIncomingDamageEvent event) {
        if (!shieldUp(player) || !ApocalypseGuard.stops(event.getSource())) {
            return false;
        }
        PowerData data = Powers.get(player);
        float left = absorb(player, data, event.getAmount());
        if (left > 1.0E-4F) {
            event.setAmount(left);
        } else {
            event.setCanceled(true);
        }
        tink(player, state(player.getUUID()), player.level().getGameTime());
        return true;
    }

    /** Takes {@code cost} off the shield's strength; if it has less, it shatters. Returns what it could not take. */
    static float absorb(ServerPlayer player, PowerData data, float cost) {
        float strength = data.shieldStrength();
        if (cost < strength - 1.0E-4F) {
            data.setShieldStrength(strength - cost);
            return 0.0F;
        }
        shatterShield(player, data);
        return Math.max(0.0F, cost - strength);
    }

    private static void tink(ServerPlayer player, State state, long now) {
        if (now - state.lastTink < 4 && now >= state.lastTink) {
            return;
        }
        state.lastTink = now;
        player.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), ModSounds.ODINFORCE_SHIELD_UP.get(), SoundSource.PLAYERS,
                0.5F, 1.6F);
    }

    /**
     * Sneak + G: the Odinforce heals him and every ally within {@link #HEAL_RADIUS} ({@code odin.heal} health each) and takes
     * every harmful effect away. Allies: players not of a rival team, tamed creatures, the Asgardians; never a monster.
     *
     * @return everyone it healed (him first)
     */
    public static List<LivingEntity> heal(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.ODIN_ODINFORCE, now)) {
            return List.of();
        }
        float amount = OdinConfig.HEAL.get().floatValue();
        List<LivingEntity> healed = new ArrayList<>();
        healed.add(player);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(HEAL_RADIUS),
                e -> e != player && e.isAlive() && e.distanceTo(player) <= HEAL_RADIUS && isAlly(player, e))) {
            healed.add(e);
        }
        List<Vec3> points = new ArrayList<>();
        for (LivingEntity e : healed) {
            e.heal(amount);
            for (MobEffectInstance effect : List.copyOf(e.getActiveEffects())) {
                if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    e.removeEffect(effect.getEffect());
                }
            }
            if (points.size() < AsgardFxPayload.MAX_POINTS) {
                points.add(e.position().add(0, e.getBbHeight() * 0.5, 0));
            }
        }
        data.startCooldown(HeroAbility.ODIN_ODINFORCE, now, OdinConfig.HEAL_COOLDOWN.get());
        send(level, player, FX_HEAL, points, Powers.glow(player));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ODINFORCE_HEAL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.enhanced.odin.healed", healed.size() - 1), true);
        return healed;
    }

    /** Whether the Odinforce counts {@code e} as Odin's ally (healed, never banished). */
    public static boolean isAlly(ServerPlayer odin, LivingEntity e) {
        if (e instanceof Player other) {
            if (other.isSpectator()) {
                return false;
            }
            PlayerTeam mine = odin.getTeam();
            PlayerTeam theirs = other.getTeam();
            return mine == null || theirs == null || mine == theirs;
        }
        if (e instanceof Enemy || e.getType().is(ModTags.BOSSES) || e.getType().is(ModTags.BOSS_MINIONS)) {
            return false;
        }
        return e instanceof OwnableEntity pet && pet.getOwnerUUID() != null || e.getType().is(ModTags.ASGARDIANS) || odin.isAlliedTo(e);
    }

    // ---- Ability 3: Banish and the Odinforce Wave ----

    /** What a banishing did. */
    public enum Banished {
        /** Nothing was there, or it was spared (an ally, a creature that means no harm, a player with PvP off). */
        NONE,
        /** A hostile creature was banished: gone in golden light. */
        GONE,
        /** It was too strong to banish (a boss, a creature of a fight, a named one) and took the Odinforce's blow instead. */
        RESISTED,
        /** A player was sent home by the Bifrost. */
        SENT_HOME
    }

    /**
     * V: the creature he looks at (up to {@link #BANISH_RANGE}) is banished. A hostile creature is gone in golden light,
     * leaving nothing; a boss, a creature of a boss fight or a named creature resists and takes {@link #BANISH_RESISTED_DAMAGE}
     * instead; a player (with PvP on and {@code odin.banishPlayers}) is sent home to his respawn point by the Bifrost. Allies,
     * creatures that mean no harm and players with PvP off are spared (no cooldown).
     */
    public static Banished banish(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.ODIN_BANISH, now)) {
            return Banished.NONE;
        }
        EntityHitResult hit = Raycasts.entity(player, BANISH_RANGE, 0.4,
                e -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator() && !(e instanceof RavenEntity), 1.0F);
        if (hit == null) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.banish_nothing"), true);
            return Banished.NONE;
        }
        LivingEntity target = (LivingEntity) hit.getEntity();
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Banished result;
        if (target instanceof ServerPlayer other) {
            if (!OdinConfig.BANISH_PLAYERS.get() || !AreaTargets.mayHurt(player, other, false) || other.isCreative()) {
                player.displayClientMessage(Component.translatable("message.enhanced.odin.banish_spared", other.getDisplayName()), true);
                return Banished.NONE;
            }
            if (!Bifrost.open(other, Bifrost.Destination.HOME, Bifrost.Source.HEIMDALL_SEND)) {
                return Banished.NONE;
            }
            other.displayClientMessage(Component.translatable("message.enhanced.odin.banished_you", player.getDisplayName())
                    .withStyle(ChatFormatting.GOLD), false);
            result = Banished.SENT_HOME;
        } else if (!AreaTargets.mayHurt(player, target, false) || !(target instanceof Enemy) && !resists(target)) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.banish_spared", target.getDisplayName()), true);
            return Banished.NONE;
        } else if (resists(target)) {
            target.hurt(ModDamageTypes.source(level, ModDamageTypes.ODINFORCE, player, player), BANISH_RESISTED_DAMAGE);
            player.displayClientMessage(Component.translatable("message.enhanced.odin.banish_resisted", target.getDisplayName()), true);
            result = Banished.RESISTED;
        } else {
            target.discard();
            result = Banished.GONE;
        }
        data.startCooldown(HeroAbility.ODIN_BANISH, now, OdinConfig.BANISH_COOLDOWN.get());
        send(level, player, FX_BANISH, List.of(at, player.getEyePosition()), result == Banished.GONE ? 1 : 0);
        level.playSound(null, at.x, at.y, at.z, ModSounds.BANISH.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        return result;
    }

    /** Whether a creature is too strong to be banished: a boss, a creature of a boss fight, a named creature. */
    public static boolean resists(LivingEntity e) {
        return e.getType().is(ModTags.BOSSES) || e.getType().is(ModTags.BOSS_MINIONS) || BossFights.isFightEntity(e)
                || BossFights.fightOf(e) != null || e.hasCustomName();
    }

    /**
     * Sneak + V: a golden shock wave from where he stands, {@link #WAVE_RADIUS} round: {@code odin.waveDamage} to every
     * creature his area attacks may hurt, thrown hard away from him.
     *
     * @return how many it struck
     */
    public static int wave(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.ODIN_BANISH, now)) {
            return 0;
        }
        float damage = OdinConfig.WAVE_DAMAGE.get().floatValue();
        int count = 0;
        Vec3 center = player.position();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(WAVE_RADIUS, 3.0, WAVE_RADIUS),
                e -> e != player && e.isAlive())) {
            if (victim.position().distanceTo(center) > WAVE_RADIUS + victim.getBbWidth() * 0.5 || !AreaTargets.mayHurt(player, victim, false)) {
                continue;
            }
            if (victim.hurt(ModDamageTypes.source(level, ModDamageTypes.ODINFORCE, player, player), damage)) {
                count++;
            }
            Vec3 away = victim.position().subtract(center);
            Vec3 d = away.horizontalDistanceSqr() < 1.0E-6 ? player.getLookAngle() : away.normalize();
            double k = WAVE_PUSH * Math.max(0.0, 1.0 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            victim.setDeltaMovement(victim.getDeltaMovement().add(d.x * k, 0.45, d.z * k));
            victim.hurtMarked = true;
        }
        data.startCooldown(HeroAbility.ODIN_BANISH, now, OdinConfig.WAVE_COOLDOWN.get());
        send(level, player, FX_WAVE, List.of(center), (int) Math.round(WAVE_RADIUS * 10.0));
        level.playSound(null, center.x, center.y, center.z, ModSounds.ODINFORCE_WAVE.get(), SoundSource.PLAYERS, 1.3F, 1.0F);
        return count;
    }

    // ---- Ability 4: Huginn and Muninn ----

    /**
     * B: Huginn and Muninn fly from his shoulders to circle where he looks ({@link #RAVENS_RANGE}) for {@code odin.ravensSeconds};
     * Sneak + B: the Ravens' Report, they circle him for {@link #REPORT_SECONDS}. Earlier ravens of his leave first.
     *
     * @return the two ravens, or none
     */
    public static List<RavenEntity> ravens(ServerPlayer player, PowerData data, boolean report) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.ODIN_RAVENS, now)) {
            return List.of();
        }
        State state = state(player.getUUID());
        for (RavenEntity old : state.ravens) {
            old.leave();
        }
        state.ravens.clear();
        Vec3 center;
        double radius;
        int seconds;
        if (report) {
            center = player.position().add(0, 6.0, 0);
            radius = REPORT_RADIUS;
            seconds = REPORT_SECONDS;
        } else {
            BlockHitResult look = Raycasts.block(player, RAVENS_RANGE, 1.0F);
            Vec3 spot = look.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(RAVENS_RANGE)) : look.getLocation();
            center = spot.add(0, 8.0, 0);
            radius = RAVENS_RADIUS;
            seconds = ravensSeconds();
        }
        long leavesAt = now + seconds * 20L;
        List<RavenEntity> sent = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            RavenEntity raven = RavenEntity.send(level, player, center, radius, i * Math.PI, i == 0 ? 1 : -1, leavesAt, report);
            level.addFreshEntity(raven);
            sent.add(raven);
        }
        state.ravens.addAll(sent);
        data.set(Live.ODIN_RAVENS, true);
        data.startCooldown(HeroAbility.ODIN_RAVENS, now, report ? REPORT_COOLDOWN : OdinConfig.RAVENS_COOLDOWN.get());
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.RAVEN_CAW.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.RAVEN_FLAP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return sent;
    }

    /** His ravens in flight (none when he has sent none). */
    public static List<RavenEntity> ravensOf(Player player) {
        State state = STATES.get(player.getUUID());
        return state == null ? List.of() : List.copyOf(state.ravens);
    }

    // ---- Ability 5: the Allfather's Command and the Odinsleep ----

    /**
     * N: the Allfather commands. In Asgard every Einherjar and warrior within reach attacks the creature he looks at (or
     * follows him for a minute if he looks at none); elsewhere {@link #SPECTRAL_COUNT} spectral Einherjar answer for {@link
     * #SPECTRAL_TICKS}. The warriors are the Asgardians' ({@link Asgardians}).
     *
     * @return how many answered
     */
    public static int command(ServerPlayer player, PowerData data) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (!data.isReady(HeroAbility.ODIN_COMMAND, now)) {
            return 0;
        }
        EntityHitResult hit = Raycasts.entity(player, COMMAND_RANGE, 0.4, e -> e instanceof LivingEntity living && living.isAlive()
                && !e.isSpectator() && AreaTargets.mayHurt(player, living, false), 1.0F);
        LivingEntity target = hit == null ? null : (LivingEntity) hit.getEntity();
        int answered;
        if (AsgardDimension.in(player)) {
            answered = Asgardians.command(player, target, target == null ? COMMAND_TICKS : COMMAND_TICKS / 2);
        } else {
            answered = Asgardians.summonEinherjar(player, SPECTRAL_COUNT, SPECTRAL_TICKS).size();
            if (target != null && answered > 0) {
                Asgardians.command(player, target, SPECTRAL_TICKS);
            }
        }
        data.startCooldown(HeroAbility.ODIN_COMMAND, now, OdinConfig.COMMAND_COOLDOWN.get());
        send(level, player, FX_COMMAND, List.of(player.position()), answered);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ODIN_COMMAND.get(), SoundSource.PLAYERS, 1.4F, 1.0F);
        player.displayClientMessage(Component.translatable(answered > 0 ? "message.enhanced.odin.command" : "message.enhanced.odin.command_none",
                answered), true);
        return answered;
    }

    /**
     * Sneak + N: he kneels in a golden cocoon, the Odinsleep: he cannot move or act, takes Resistance IV and heals
     * {@link #SLEEP_HEAL_SHARE} of his health each second, until he is whole, presses N again, or {@code
     * odin.odinsleepMaxSeconds} have passed. Its cooldown starts when it ends. Refused while he is whole.
     *
     * @return whether he fell asleep
     */
    public static boolean startSleep(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.isReady(HeroAbility.ODIN_COMMAND, now)) {
            return false;
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            player.displayClientMessage(Component.translatable("message.enhanced.odin.not_weary"), true);
            return false;
        }
        if (data.chaosShieldActive() && data.has(HeroAbility.ODIN_ODINFORCE)) {
            dropShield(player, data, false);
        }
        data.setToggle(Toggle.ODINSLEEP, true);
        state(player.getUUID()).sleepStart = now;
        sleepModifiers(player, true);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, true, false, true));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ODINSLEEP_START.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.enhanced.odin.sleep_start").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** One tick of the Odinsleep: he is held still, guarded and healed; it ends when he is whole or its time is up. */
    static void sleepTick(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        State state = state(player.getUUID());
        if (state.sleepStart == 0L || state.sleepStart > now) {
            state.sleepStart = now; // (a sleep that outlived a restart begins its count again)
        }
        sleepModifiers(player, true);
        player.setDeltaMovement(0.0, Math.min(0.0, player.getDeltaMovement().y), 0.0);
        if (!player.hasEffect(MobEffects.DAMAGE_RESISTANCE) || player.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration() < 20) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, true, false, true));
        }
        long slept = now - state.sleepStart;
        if (slept > 0 && slept % 20 == 0) {
            player.heal(player.getMaxHealth() * SLEEP_HEAL_SHARE);
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            endSleep(player, data, "message.enhanced.odin.sleep_whole");
        } else if (slept >= odinsleepMaxTicks()) {
            endSleep(player, data, "message.enhanced.odin.sleep_over");
        }
    }

    /** He wakes from the Odinsleep; its cooldown starts now. */
    static void endSleep(ServerPlayer player, PowerData data, @Nullable String message) {
        if (!data.toggle(Toggle.ODINSLEEP)) {
            return;
        }
        data.setToggle(Toggle.ODINSLEEP, false);
        sleepModifiers(player, false);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        data.startCooldown(HeroAbility.ODIN_COMMAND, player.level().getGameTime(), OdinConfig.ODINSLEEP_COOLDOWN.get());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ODINSLEEP_END.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (message != null) {
            player.displayClientMessage(Component.translatable(message), true);
        }
        STATES.computeIfPresent(player.getUUID(), (k, s) -> {
            s.sleepStart = 0L;
            return s;
        });
    }

    /** While he sleeps his legs do not answer: no walking, no jumping. */
    private static void sleepModifiers(ServerPlayer player, boolean on) {
        setModifier(player, Attributes.MOVEMENT_SPEED, SLEEP_SPEED, -1.0, on, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.JUMP_STRENGTH, SLEEP_JUMP, -1.0, on, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    // ---- what everyone near sees ----

    static void send(ServerLevel level, Entity who, int kind, List<Vec3> points, int arg) {
        Net.sendToPlayersNear(level, null, who.getX(), who.getY(), who.getZ(), FX_RANGE,
                new AsgardFxPayload(kind, who.getId(), List.copyOf(points), arg));
    }

    // ---- the end of the power, of the life, of the session ----

    /** The power is being taken away: the shield falls, the spear comes home, the ravens leave, he wakes. */
    static void endAll(ServerPlayer player, PowerData data) {
        if (data.chaosShieldActive() && data.has(HeroAbility.ODIN_ODINFORCE)) {
            data.setChaosShieldActive(false);
        }
        State state = STATES.get(player.getUUID());
        if (state != null) {
            if (state.spear != null && state.spear.isAlive()) {
                state.spear.giveBack(player);
            }
            for (RavenEntity raven : state.ravens) {
                raven.leave();
            }
            state.ravens.clear();
        }
        if (data.toggle(Toggle.ODINSLEEP)) {
            data.setToggle(Toggle.ODINSLEEP, false);
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
        sleepModifiers(player, false);
        data.set(Live.ODIN_THROWN, false);
        data.set(Live.ODIN_RAVENS, false);
        STATES.remove(player.getUUID());
    }

    /**
     * He died or left: the ravens leave; a spear in flight falls where it is (a death) or comes back into his pack (he left,
     * so it is saved with him); the Odinsleep ends without its cooldown.
     */
    static void onGone(ServerPlayer player, boolean leaving) {
        State state = STATES.get(player.getUUID());
        PowerData data = Powers.get(player);
        if (state != null) {
            if (state.spear != null && state.spear.isAlive()) {
                if (leaving) {
                    state.spear.giveBack(player);
                } else {
                    state.spear.drop();
                }
            }
            for (RavenEntity raven : state.ravens) {
                raven.leave();
            }
            state.ravens.clear();
            state.sleepStart = 0L;
        }
        if (data.toggle(Toggle.ODINSLEEP)) {
            data.setToggle(Toggle.ODINSLEEP, false);
        }
        sleepModifiers(player, false);
        data.set(Live.ODIN_THROWN, false);
        data.set(Live.ODIN_RAVENS, false);
        if (leaving) {
            STATES.remove(player.getUUID());
        }
    }

    /** He changed realm: the spear he threw cannot follow him through the Bifrost; it falls where it is. */
    static void onChangeDimension(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null && state.spear != null && state.spear.isAlive() && state.spear.level() != player.level()) {
            state.spear.drop();
        }
    }

    /** The server has stopped: nothing of the hero's is kept. */
    static void clear() {
        STATES.clear();
    }

    private OdinAbilities() {
    }
}
