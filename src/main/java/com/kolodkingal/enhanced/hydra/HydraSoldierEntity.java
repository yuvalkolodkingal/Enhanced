package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MindLink;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.HydraConfig;
import com.kolodkingal.enhanced.network.HydraFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A Hydra soldier (3.6, the plan's 15.2-15.4) of the Hydra outposts, in black tactical gear: a {@link Rank#RIFLEMAN}
 * (energy rifle: bursts of three blue bolts, a reload every six; the rifle's butt up close), an {@link Rank#ENFORCER}
 * (stun baton and a riot shield that stops arrows and bolts from the front) or a {@link Rank#SNIPER} (a red laser
 * warns for a moment, then one heavy bolt). A monster: Peaceful removes him and the Snap counts him.
 *
 * <p>His life ({@link HydraGoals}): at his post (looking round in slow sweeps) or on his patrol's loop; alert when he
 * hears something; fighting what he sees ({@link HydraSenses}) or what attacks him; back to his post when it is over,
 * healing. He never goes farther than {@code [hydra] leashRadius} from his outpost. After 3 s of fighting he runs to a
 * radio console to raise the alarm ({@link HydraAlarm}) if the officer lives. He belongs to an outpost's post (the
 * garrison, {@link HydraGarrison}) or is a reinforcement, or nobody's (a spawn egg's).
 *
 * <p>Minds: Loki's Scepter or the Mind Stone turn him ({@link #turnTo}: blue eyes, he fights Hydra for his leader);
 * Professor X can steer him ({@link #steeredAttack} fires his weapon); a frozen or turned soldier raises no alarm.
 */
public class HydraSoldierEntity extends Monster implements SteeredShooter {
    /** What he carries. */
    public enum Arms {
        RIFLE, BATON, LONG_RIFLE, PISTOL
    }

    /** What he is doing, for the client's poses (synced). */
    public enum Act {
        IDLE, PATROL, AIM, RELOAD, RADIO, SHIELD, RUN, ALERT;

        static Act byId(int id) {
            Act[] all = values();
            return id >= 0 && id < all.length ? all[id] : IDLE;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_RANK = SynchedEntityData.defineId(HydraSoldierEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(
            HydraSoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ACT = SynchedEntityData.defineId(HydraSoldierEntity.class,
            EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_TURNED = SynchedEntityData.defineId(
            HydraSoldierEntity.class, EntityDataSerializers.BOOLEAN);
    /** The entity the sniper's red laser points at while he aims, or -1. */
    private static final EntityDataAccessor<Integer> DATA_LASER = SynchedEntityData.defineId(HydraSoldierEntity.class,
            EntityDataSerializers.INT);

    /** Ticks between two bolts of a burst. */
    public static final int BURST_GAP = 5;
    /** Bolts between two reloads, and how long a reload takes. */
    public static final int CELL_SHOTS = 6;
    public static final int RELOAD_TICKS = 30;
    /** The rifle's butt: reach, damage, knockback and rest. */
    public static final double BUTT_REACH = 2.5;
    public static final float BUTT_DAMAGE = 4.0F;
    public static final double BUTT_KNOCKBACK = 0.6;
    public static final int BUTT_COOLDOWN = 30;
    /** The baton's rest, and its Slowness II (1 s). */
    public static final int BATON_COOLDOWN = 20;
    public static final int BATON_SLOW_TICKS = 20;
    /** The rifle's range. */
    public static final double RIFLE_RANGE = 20.0;
    /** The sniper's rest between two shots. */
    public static final int SNIPER_COOLDOWN = 80;
    /** The officer's pistol: range and rest. */
    public static final double PISTOL_RANGE = 16.0;
    public static final int PISTOL_COOLDOWN = 25;
    /** How long a soldier fights before he raises the alarm (3 s). */
    public static final int ALARM_AFTER = 60;
    /** How far the bark carries, and how far a fight is heard by the others. */
    public static final double BARK_RANGE = 16.0;
    /** The range a soldier keeps from his target with a rifle or pistol. */
    public static final double KEEP_MIN = 8.0;
    public static final double KEEP_MAX = 16.0;
    /** Below this part of his health a ranged soldier takes cover. */
    public static final float COVER_HEALTH = 0.3F;

    // ---- whose he is ----
    private long outpostKey;
    private boolean hasOutpost;
    private int post = -1;
    private boolean reinforcement;
    private boolean leaving;
    private long leavingSince;

    // ---- his mind ----
    @Nullable
    private UUID turnedById;
    private long turnedUntil;
    @Nullable
    private Vec3 alertPos;
    private long alertUntil;
    private long combatSince = -1L;
    private boolean alarmTried;

    // ---- his weapon ----
    private int shots;
    private int burstLeft;
    private long nextShotAt;
    private long nextBurstAt;
    private long reloadUntil;
    private long aimSince = -1L;
    private long meleeReadyAt;
    @Nullable
    private BlockPos coverSpot;
    /** Bolts left of a burst Professor X fired through him, and when the next goes. */
    private int steeredBurst;
    private long steeredNext;

    public HydraSoldierEntity(EntityType<? extends HydraSoldierEntity> type, Level level) {
        super(type, level);
        this.xpReward = Rank.RIFLEMAN.xp();
        setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new HydraNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RANK, Rank.RIFLEMAN.ordinal());
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_ACT, (byte) Act.IDLE.ordinal());
        builder.define(DATA_TURNED, false);
        builder.define(DATA_LASER, -1);
    }

    @Override
    protected void registerGoals() {
        HydraGoals.addTo(this);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Who he is

    public Rank rank() {
        return Rank.byOrdinal(entityData.get(DATA_RANK));
    }

    /** His skin among his rank's. */
    public int variant() {
        return entityData.get(DATA_VARIANT);
    }

    public boolean isOfficer() {
        return false;
    }

    /** What he carries: the officer a pistol, the ranks their own. */
    public Arms arms() {
        return switch (rank()) {
            case ENFORCER -> Arms.BATON;
            case SNIPER -> Arms.LONG_RIFLE;
            default -> Arms.RIFLE;
        };
    }

    /** Makes him a soldier of {@code rank} with skin {@code variant}: his health, armour and experience. */
    public void setRank(Rank rank, int variant) {
        entityData.set(DATA_RANK, rank.ordinal());
        entityData.set(DATA_VARIANT, Math.floorMod(variant, rank.skins()));
        setBase(Attributes.MAX_HEALTH, rank.health());
        setBase(Attributes.ARMOR, rank.armor());
        setBase(Attributes.ATTACK_DAMAGE, rank == Rank.ENFORCER ? batonDamage() : BUTT_DAMAGE);
        setBase(Attributes.KNOCKBACK_RESISTANCE, rank == Rank.ENFORCER ? 0.4 : 0.0);
        setHealth(getMaxHealth());
        this.xpReward = rank.xp();
    }

    protected void setVariant(int variant) {
        entityData.set(DATA_VARIANT, variant);
    }

    protected void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                           double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    protected Component getTypeName() {
        if (isOfficer()) {
            return super.getTypeName();
        }
        return Component.translatable("entity.enhanced.hydra_soldier." + rank().id());
    }

    /** What the client poses. */
    public Act act() {
        return Act.byId(entityData.get(DATA_ACT));
    }

    void setAct(Act act) {
        if (entityData.get(DATA_ACT) != act.ordinal()) {
            entityData.set(DATA_ACT, (byte) act.ordinal());
        }
    }

    /** The entity id the sniper's laser points at, or -1. */
    public int laserTarget() {
        return entityData.get(DATA_LASER);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Whose he is

    /** Makes him the soldier of post {@code post} of {@code outpost} (or a reinforcement of it, {@code post} -1). */
    public void belongTo(HydraOutposts.Outpost outpost, int post, boolean reinforcement) {
        this.outpostKey = outpost.key();
        this.hasOutpost = true;
        this.post = post;
        this.reinforcement = reinforcement;
        setPersistenceRequired();
    }

    /** His outpost, or null (a spawn egg's soldier, or an outpost that is gone). */
    @Nullable
    public HydraOutposts.Outpost outpost() {
        if (!hasOutpost || !(level() instanceof ServerLevel level)) {
            return null;
        }
        HydraOutposts store = HydraOutposts.peek(level);
        return store == null ? null : store.byKey(outpostKey);
    }

    /** His post's index ({@link Post}), or -1. */
    public int post() {
        return post;
    }

    public boolean isReinforcement() {
        return reinforcement;
    }

    /** Whether he is a reinforcement on his way out (he vanishes when nobody sees him). */
    public boolean isLeaving() {
        return leaving;
    }

    void leave(long now) {
        if (!leaving) {
            leaving = true;
            leavingSince = now;
            setTarget(null);
        }
    }

    long leavingSince() {
        return leavingSince;
    }

    /**
     * Where he stands guard now (world), or null: a patrol has no fixed point; the officer's is the command room by day
     * and the radio room by night.
     */
    @Nullable
    public BlockPos postPos() {
        HydraOutposts.Outpost outpost = outpost();
        Post p = Post.byIndex(post);
        if (outpost == null || p == null || p.route() != null) {
            return null;
        }
        if (p.isOfficer()) {
            return outpost.where(level().isDay() ? com.kolodkingal.enhanced.world.HydraOutpostPlan.COMMAND_POST
                    : com.kolodkingal.enhanced.world.HydraOutpostPlan.RADIO_POST);
        }
        return outpost.where(p.point());
    }

    /** His patrol's loop (world points, the perimeter on the ground), or an empty list. */
    public List<BlockPos> route() {
        HydraOutposts.Outpost outpost = outpost();
        Post p = Post.byIndex(post);
        if (outpost == null || p == null || p.route() == null) {
            return List.of();
        }
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos corner : p.route().corners()) {
            BlockPos at = outpost.where(corner);
            if (p.route().onSurface() && outpost.real()) {
                at = new BlockPos(at.getX(), level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(),
                        at.getZ()), at.getZ());
            }
            out.add(at);
        }
        return out;
    }

    /** The patrol's starting corner. */
    public int routeStart() {
        Post p = Post.byIndex(post);
        return p == null ? 0 : p.start();
    }

    /** The middle of his outpost (the leash), or null. */
    @Nullable
    public BlockPos home() {
        HydraOutposts.Outpost outpost = outpost();
        return outpost == null ? null : outpost.centre();
    }

    // ------------------------------------------------------------------------------------------------------------
    // His mind: turned, frozen, steered

    /** Whether he fights for someone else now (the Scepter, the Mind Stone). */
    public boolean isTurned() {
        if (level().isClientSide) {
            return entityData.get(DATA_TURNED);
        }
        return turnedById != null && turnedUntil > level().getGameTime();
    }

    /** Whether he is turned to {@code leader}'s side. */
    public boolean turnedBy(@Nullable Entity leader) {
        return leader != null && isTurned() && leader.getUUID().equals(turnedById);
    }

    /** The one he fights for, if turned and in this level. */
    @Nullable
    public LivingEntity leader() {
        if (!isTurned() || !(level() instanceof ServerLevel level) || turnedById == null) {
            return null;
        }
        return level.getEntity(turnedById) instanceof LivingEntity living ? living : null;
    }

    /** How many ticks he stays turned. */
    public long turnedTicksLeft() {
        return isTurned() ? turnedUntil - level().getGameTime() : 0L;
    }

    /**
     * Turns him to {@code leader}'s side for {@code ticks}: blue eyes, he drops his quarrel, follows the leader and
     * fights Hydra; he never raises the alarm.
     */
    public boolean turnTo(LivingEntity leader, int ticks) {
        if (!(level() instanceof ServerLevel level) || ticks <= 0) {
            return false;
        }
        turnedById = leader.getUUID();
        turnedUntil = level.getGameTime() + ticks;
        entityData.set(DATA_TURNED, true);
        setTarget(null);
        alertPos = null;
        combatSince = -1L;
        cancelAim();
        HydraOutposts.Outpost outpost = outpost();
        if (outpost != null && getUUID().equals(outpost.runner)) {
            outpost.runner = null;
        }
        level.playSound(null, getX(), getY(), getZ(), ModSounds.HYDRA_RADIO.get(), SoundSource.HOSTILE, 0.8F, 1.4F);
        Net.sendToPlayersTrackingEntityAndSelf(this, new HydraFxPayload(HydraFxPayload.TURNED, getId(),
                List.of(getEyePosition()), ticks));
        if (leader instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.enhanced.hydra.turned"), true);
        }
        return true;
    }

    private void endTurn() {
        turnedById = null;
        turnedUntil = 0L;
        entityData.set(DATA_TURNED, false);
        setTarget(null);
        Net.sendToPlayersTrackingEntityAndSelf(this, new HydraFxPayload(HydraFxPayload.TURNED, getId(),
                List.of(getEyePosition()), 0));
    }

    /** Whether Freeze Minds (or any spell that binds him) holds him: a frozen soldier raises no alarm. */
    public boolean isFrozen() {
        return hasEffect(ModEffects.SPELLBOUND);
    }

    /** Whether he may raise the alarm now: not frozen, turned, steered or possessed. */
    public boolean mayRaiseAlarm() {
        return isAlive() && !isFrozen() && !HydraLoyalty.isTraitor(this);
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity instanceof LivingEntity living && (HydraLoyalty.sameSide(this, living) || turnedBy(living))) {
            return true;
        }
        if (entity instanceof HydraSoldierEntity other && isTurned() && other.isTurned()
                && turnedById != null && turnedById.equals(other.turnedById)) {
            return true;
        }
        return super.isAlliedTo(entity);
    }

    /**
     * He heard or glimpsed something at {@code pos}: if he is not fighting he goes to look (for 5 s). With {@code
     * bark}, he shouts and the Hydra round him are alert too.
     */
    public void alertTo(Vec3 pos, boolean bark) {
        if (!(level() instanceof ServerLevel level) || getTarget() != null || isTurned() || isFrozen()) {
            return;
        }
        alertPos = pos;
        alertUntil = level.getGameTime() + 100L;
        if (bark) {
            bark(pos);
        }
    }

    /** A wordless shout: every Hydra within {@link #BARK_RANGE} on his side is alert toward {@code pos}. */
    void bark(Vec3 pos) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, getX(), getEyeY(), getZ(), ModSounds.HYDRA_BARK.get(), SoundSource.HOSTILE, 1.2F,
                0.9F + random.nextFloat() * 0.2F);
        for (HydraSoldierEntity other : level.getEntitiesOfClass(HydraSoldierEntity.class,
                getBoundingBox().inflate(BARK_RANGE), o -> o != this && HydraLoyalty.sameSide(this, o))) {
            other.alertTo(pos, false);
        }
    }

    @Nullable
    Vec3 alertPos() {
        if (alertPos != null && level().getGameTime() > alertUntil) {
            alertPos = null;
        }
        return alertPos;
    }

    void clearAlert() {
        alertPos = null;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The fight

    /** A new target: he shouts, the others are alert, and the officer raises the alarm (lane rule: at once). */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity old = getTarget();
        super.setTarget(target);
        if (level().isClientSide) {
            return;
        }
        LivingEntity now = getTarget();
        if (now == null) {
            combatSince = -1L;
            alarmTried = false;
            cancelAim();
            coverSpot = null;
        } else if (now != old) {
            if (combatSince < 0L) {
                combatSince = level().getGameTime();
            }
            alertPos = null;
            if (!isTurned()) {
                bark(now.position());
            }
        }
    }

    /** How long he has been fighting (ticks), or -1. */
    public long fightingFor() {
        return combatSince < 0L ? -1L : level().getGameTime() - combatSince;
    }

    boolean alarmTried() {
        return alarmTried;
    }

    void markAlarmTried() {
        alarmTried = true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        steeredTick();
        if (turnedById != null && turnedUntil <= now) {
            endTurn();
        }
        LivingEntity target = getTarget();
        if (target != null && !keeps(target)) {
            setTarget(null);
            target = null;
        }
        BlockPos home = home();
        if (home != null && !leaving) {
            double leash = EnhancedConfig.SPEC.isLoaded() ? HydraConfig.LEASH_RADIUS.get() : 48.0;
            if (horizontalDistanceSqr(home) > leash * leash && !isTurned()) {
                setTarget(null);
                target = null;
            }
        }
        if (target == null && (tickCount + getId()) % 40 == 0 && getHealth() < getMaxHealth()) {
            heal(1.0F); // half a heart a second while nobody is fought
        }
        if (target instanceof Player && !isTurned()) {
            HydraOutposts.Outpost outpost = outpost();
            if (outpost != null) {
                outpost.lastIntruder = now;
            }
        }
        if (target != null && mayRaiseAlarm() && !reinforcement) {
            HydraAlarm.fighting(level, this, target, now);
        }
        if (target == null && act() != Act.RUN) {
            setAct(isPatrolling() ? Act.PATROL : alertPos() != null ? Act.ALERT : Act.IDLE);
        }
    }

    private double horizontalDistanceSqr(BlockPos pos) {
        double dx = getX() - (pos.getX() + 0.5);
        double dz = getZ() - (pos.getZ() + 0.5);
        return dx * dx + dz * dz;
    }

    /** Whether he walks a patrol's loop. */
    public boolean isPatrolling() {
        Post p = Post.byIndex(post);
        return p != null && p.route() != null && outpost() != null;
    }

    /** Whether he keeps fighting {@code target}: alive, fightable, not his side, not hidden for good. */
    private boolean keeps(LivingEntity target) {
        if (!target.isAlive() || target.level() != level()) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        if (isTurned()) {
            return !(target instanceof Player) && !turnedBy(target);
        }
        if (target instanceof LivingEntity living && HydraLoyalty.sameSide(this, living)) {
            return false;
        }
        return distanceToSqr(target) < 48.0 * 48.0;
    }

    /** The spread of his aim (degrees) on this difficulty: 12 Easy, 7 Normal, 4 Hard. */
    public static double spread(Difficulty difficulty) {
        return switch (difficulty) {
            case HARD -> 4.0;
            case NORMAL -> 7.0;
            default -> 12.0;
        };
    }

    /** The point his weapon fires from: before his chest, a little to his right. */
    public Vec3 muzzle() {
        Vec3 look = getViewVector(1.0F);
        Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
        return getEyePosition().add(0.0, -0.25, 0.0).add(right.scale(0.25)).add(look.scale(0.6));
    }

    /**
     * Fires one bolt along {@code direction} (spread by {@code spreadDegrees}): its sound and muzzle flash, his
     * cell's count.
     */
    public HydraBoltEntity fire(Vec3 direction, float damage, double range, int kind, double spreadDegrees) {
        Vec3 dir = direction.normalize();
        if (spreadDegrees > 0.0) {
            double s = Math.toRadians(spreadDegrees) * 0.5;
            dir = dir.add(random.triangle(0.0, s), random.triangle(0.0, s), random.triangle(0.0, s)).normalize();
        }
        Vec3 from = muzzle();
        double speed = EnhancedConfig.SPEC.isLoaded() ? HydraConfig.BOLT_SPEED.get() : 2.2;
        HydraBoltEntity bolt = HydraBoltEntity.create(level(), this, from, dir,
                kind == HydraBoltEntity.SNIPER ? speed * 1.5 : speed, damage, range, kind);
        level().addFreshEntity(bolt);
        // (The rifle's sound holds a whole burst of three: it is played once, when a burst begins.)
        SoundEvent sound = switch (kind) {
            case HydraBoltEntity.SNIPER -> ModSounds.HYDRA_SNIPER_SHOT.get();
            case HydraBoltEntity.PISTOL -> ModSounds.HYDRA_PISTOL.get();
            default -> null;
        };
        if (sound != null) {
            level().playSound(null, from.x, from.y, from.z, sound, SoundSource.HOSTILE, 1.0F,
                    0.95F + random.nextFloat() * 0.1F);
        }
        if (level() instanceof ServerLevel) {
            Net.sendToPlayersTrackingEntityAndSelf(this, new HydraFxPayload(HydraFxPayload.MUZZLE,
                    getId(), List.of(from), kind));
        }
        shots++;
        return bolt;
    }

    /** Fires at {@code target}'s chest. */
    HydraBoltEntity fireAt(LivingEntity target, float damage, double range, int kind) {
        Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0).subtract(muzzle());
        return fire(aim, damage, range, kind, spread(level().getDifficulty()));
    }

    /** Whether he swaps his cell now (a pause the player can use). */
    public boolean reloading() {
        return reloadUntil > level().getGameTime();
    }

    /** The rifleman's tick against {@code target}: bursts, reloads, the butt up close. */
    void rifleTick(LivingEntity target, double distance, boolean sees) {
        long now = level().getGameTime();
        if (reloading()) {
            setAct(Act.RELOAD);
            return;
        }
        if (distance <= BUTT_REACH && now >= meleeReadyAt) {
            meleeReadyAt = now + BUTT_COOLDOWN;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (target.hurt(damageSources().mobAttack(this), BUTT_DAMAGE)) {
                Vec3 push = target.position().subtract(position()).normalize();
                target.knockback(BUTT_KNOCKBACK, -push.x, -push.z);
                target.hurtMarked = true;
            }
            return;
        }
        if (burstLeft > 0) {
            setAct(Act.AIM);
            if (now >= nextShotAt) {
                fireAt(target, rifleDamage(), RIFLE_RANGE, HydraBoltEntity.RIFLE);
                burstLeft--;
                nextShotAt = now + BURST_GAP;
                if (shots % CELL_SHOTS == 0) {
                    burstLeft = 0;
                    startReload(now);
                }
            }
            return;
        }
        if (sees && distance <= RIFLE_RANGE && now >= nextBurstAt) {
            startBurst(now);
            setAct(Act.AIM);
            rifleTick(target, distance, true);
            return;
        }
        setAct(sees ? Act.AIM : Act.RUN);
    }

    /** A burst begins: its sound (all its shots in one), the bolts follow {@link #BURST_GAP} ticks apart. */
    private void startBurst(long now) {
        burstLeft = burstShots();
        nextShotAt = now;
        nextBurstAt = now + burstCooldown();
        level().playSound(null, getX(), getEyeY(), getZ(), ModSounds.HYDRA_RIFLE_BURST.get(), SoundSource.HOSTILE,
                1.0F, 0.95F + random.nextFloat() * 0.1F);
    }

    private void startReload(long now) {
        reloadUntil = now + RELOAD_TICKS;
        setAct(Act.RELOAD);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.HYDRA_RELOAD.get(), SoundSource.HOSTILE, 0.8F, 1.0F);
    }

    /** The officer's tick: one pistol bolt every {@link #PISTOL_COOLDOWN} ticks. */
    void pistolTick(LivingEntity target, double distance, boolean sees) {
        long now = level().getGameTime();
        setAct(sees ? Act.AIM : Act.RUN);
        if (sees && distance <= PISTOL_RANGE && now >= nextBurstAt) {
            nextBurstAt = now + PISTOL_COOLDOWN;
            fireAt(target, pistolDamage(), PISTOL_RANGE, HydraBoltEntity.PISTOL);
        }
    }

    /**
     * The sniper's tick: he aims {@code [hydra] sniperAimTicks} with his red laser on the target (the warning), then
     * one heavy bolt; losing sight of the target breaks his aim.
     */
    void sniperTick(LivingEntity target, double distance, boolean sees) {
        long now = level().getGameTime();
        double range = EnhancedConfig.SPEC.isLoaded() ? HydraConfig.SNIPER_RANGE.get() : 40.0;
        if (!sees || distance > range || now < nextBurstAt) {
            cancelAim();
            setAct(Act.IDLE);
            return;
        }
        if (aimSince < 0L) {
            aimSince = now;
            entityData.set(DATA_LASER, target.getId());
            level().playSound(null, getX(), getEyeY(), getZ(), ModSounds.HYDRA_SNIPER_AIM.get(), SoundSource.HOSTILE,
                    1.0F, 1.0F);
            if (level() instanceof ServerLevel) {
                Net.sendToPlayersTrackingEntityAndSelf(this, new HydraFxPayload(
                        HydraFxPayload.SNIPER_AIM, getId(), List.of(target.getEyePosition()), aimTicks()));
            }
        }
        setAct(Act.AIM);
        if (entityData.get(DATA_LASER) != target.getId()) {
            entityData.set(DATA_LASER, target.getId());
        }
        if (now - aimSince >= aimTicks()) {
            Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0).subtract(muzzle());
            fire(aim, sniperDamage(), range, HydraBoltEntity.SNIPER, 0.0);
            nextBurstAt = now + SNIPER_COOLDOWN;
            cancelAim();
        }
    }

    /** Whether the sniper is aiming now, and since when. */
    public long aimingSince() {
        return aimSince;
    }

    void cancelAim() {
        aimSince = -1L;
        if (entityData.get(DATA_LASER) != -1) {
            entityData.set(DATA_LASER, -1);
        }
    }

    /** The enforcer's tick: his baton when in reach (Slowness II for 1 s). */
    void batonTick(LivingEntity target, double distance) {
        long now = level().getGameTime();
        setAct(holdsRanged(target) ? Act.SHIELD : Act.IDLE);
        double reach = 1.6 + getBbWidth() * 0.5 + target.getBbWidth() * 0.5;
        if (distance <= reach && now >= meleeReadyAt && hasLineOfSight(target)) {
            meleeReadyAt = now + BATON_COOLDOWN;
            baton(target);
        }
    }

    /** One blow of the stun baton. */
    void baton(LivingEntity target) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        DamageSource source = ModDamageTypes.source(level(), ModDamageTypes.HYDRA_BATON, this, this);
        if (target.hurt(source, batonDamage())) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BATON_SLOW_TICKS, 1), this);
        }
        level().playSound(null, getX(), getY(), getZ(), ModSounds.HYDRA_BATON.get(), SoundSource.HOSTILE, 1.0F,
                1.0F);
    }

    /** Whether {@code target} holds something that shoots (the enforcer lifts his shield). */
    public static boolean holdsRanged(LivingEntity target) {
        for (ItemStack stack : List.of(target.getMainHandItem(), target.getOffhandItem())) {
            if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem
                    || stack.getItem() instanceof TridentItem || stack.getItem() instanceof HydraBlasterItem) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    BlockPos coverSpot() {
        return coverSpot;
    }

    void setCoverSpot(@Nullable BlockPos spot) {
        coverSpot = spot;
    }

    /** Whether he is hurt enough to take cover. */
    boolean wantsCover() {
        return getHealth() < getMaxHealth() * COVER_HEALTH;
    }

    // ---- the numbers (the server's options, or their defaults before the config is loaded) ----

    static float rifleDamage() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.RIFLE_DAMAGE.get().floatValue() : 4.0F;
    }

    static int burstShots() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.BURST_SHOTS.get() : 3;
    }

    static int burstCooldown() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.BURST_COOLDOWN.get() : 50;
    }

    static float sniperDamage() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.SNIPER_DAMAGE.get().floatValue() : 8.0F;
    }

    static int aimTicks() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.SNIPER_AIM_TICKS.get() : 30;
    }

    static float batonDamage() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.BATON_DAMAGE.get().floatValue() : 6.0F;
    }

    static float pistolDamage() {
        return EnhancedConfig.SPEC.isLoaded() ? HydraConfig.PISTOL_DAMAGE.get().floatValue() : 3.0F;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Hurt: the riot shield; his death

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && blocksWithShield(source)) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.HYDRA_SHIELD_BLOCK.get(), SoundSource.HOSTILE,
                    1.0F, 0.9F + random.nextFloat() * 0.2F);
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity attacker && isAlive()) {
            HydraSenses.heard((ServerLevel) level(), position());
        }
        return hurt;
    }

    /**
     * Whether the enforcer's riot shield stops this: an arrow, a bolt or any other projectile that comes at his front
     * (within 60 degrees of where his body faces), while he is not frozen.
     */
    public boolean blocksWithShield(DamageSource source) {
        if (rank() != Rank.ENFORCER || isOfficer() || isFrozen()
                || !(source.getDirectEntity() instanceof Projectile p)) {
            return false;
        }
        Vec3 from = p.position().subtract(position());
        if (p.getDeltaMovement().lengthSqr() > 1.0E-4) {
            from = p.getDeltaMovement().scale(-1.0);
        }
        return HydraSenses.inCone(yBodyRot, Vec3.ZERO, new Vec3(from.x, 0.0, from.z));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            HydraGarrison.died(level, this);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason == RemovalReason.DISCARDED && level() instanceof ServerLevel level) {
            HydraGarrison.discarded(level, this);
        }
        super.remove(reason);
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return HydraDrops.table(this);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        HydraDrops.dropRare(level, this, source, recentlyHit);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HYDRA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HYDRA_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !hasOutpost && super.removeWhenFarAway(distance);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Professor X steers him

    /** Xavier, inside him, clicked: he fires his own weapon where he looks (the baton: what is in front of him). */
    @Override
    public void steeredAttack(ServerPlayer xavier) {
        Vec3 look = Vec3.directionFromRotation(getXRot(), getYHeadRot());
        switch (arms()) {
            case RIFLE -> {
                if (steeredBurst <= 0) {
                    long now = level().getGameTime();
                    startBurst(now);
                    burstLeft = 0;
                    steeredBurst = burstShots();
                    steeredNext = now;
                    steeredTick();
                }
            }
            case LONG_RIFLE -> fire(look, sniperDamage(),
                    EnhancedConfig.SPEC.isLoaded() ? HydraConfig.SNIPER_RANGE.get() : 40.0, HydraBoltEntity.SNIPER,
                    0.0);
            case PISTOL -> fire(look, pistolDamage(), PISTOL_RANGE, HydraBoltEntity.PISTOL, 0.0);
            case BATON -> {
                Vec3 eye = getEyePosition();
                EntityHitResult hit = Raycasts.entityBetween(level(), this, eye, eye.add(look.scale(BUTT_REACH)), 0.3,
                        e -> e instanceof LivingEntity living && living != xavier && living.isAlive()
                                && !(e instanceof Player p && (!xavier.server.isPvpAllowed()
                                || !xavier.canHarmPlayer(p))));
                if (hit != null && hit.getEntity() instanceof LivingEntity victim) {
                    baton(victim);
                } else {
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    /** The steered burst: a bolt every {@link #BURST_GAP} ticks along where Xavier looks through him. */
    private void steeredTick() {
        long now = level().getGameTime();
        if (steeredBurst <= 0 || now < steeredNext) {
            return;
        }
        if (!steered()) {
            steeredBurst = 0;
            return;
        }
        fire(Vec3.directionFromRotation(getXRot(), getYHeadRot()), rifleDamage(), RIFLE_RANGE, HydraBoltEntity.RIFLE,
                0.0);
        steeredBurst--;
        steeredNext = now + BURST_GAP;
    }

    /** Whether Professor X steers him now. */
    public boolean steered() {
        return MindLink.isHost(this);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Spawning and saving

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, type, data);
        if (isOfficer()) {
            setRank(Rank.RIFLEMAN, random.nextInt(HydraOfficerEntity.SKINS));
        } else if (type != MobSpawnType.EVENT && type != MobSpawnType.STRUCTURE) {
            float r = random.nextFloat();
            Rank rank = r < 0.6F ? Rank.RIFLEMAN : r < 0.85F ? Rank.ENFORCER : Rank.SNIPER;
            setRank(rank, random.nextInt(rank.skins()));
        }
        return out;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Rank", rank().id());
        tag.putInt("Variant", variant());
        if (hasOutpost) {
            tag.putLong("Outpost", outpostKey);
            tag.putInt("Post", post);
            tag.putBoolean("Reinforcement", reinforcement);
            tag.putBoolean("Leaving", leaving);
        }
        if (turnedById != null) {
            tag.putUUID("TurnedBy", turnedById);
            tag.putLong("TurnedUntil", turnedUntil);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Rank")) {
            Rank rank = Rank.byName(tag.getString("Rank"));
            float health = getHealth();
            setRank(rank, tag.getInt("Variant"));
            if (tag.contains("Health")) {
                setHealth(health);
            }
        }
        if (tag.contains("Outpost")) {
            outpostKey = tag.getLong("Outpost");
            hasOutpost = true;
            post = tag.getInt("Post");
            reinforcement = tag.getBoolean("Reinforcement");
            leaving = tag.getBoolean("Leaving");
        }
        if (tag.hasUUID("TurnedBy")) {
            turnedById = tag.getUUID("TurnedBy");
            turnedUntil = tag.getLong("TurnedUntil");
            entityData.set(DATA_TURNED, true);
        }
    }

    /** The yaw (degrees) from him to {@code pos}. */
    float yawTo(Vec3 pos) {
        return (float) (Mth.atan2(pos.z - getZ(), pos.x - getX()) * Mth.RAD_TO_DEG) - 90.0F;
    }
}
