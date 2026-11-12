package com.kolodkingal.enhanced.nano;

import com.kolodkingal.enhanced.mind.MindTalk;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.ultron.UltronBoltEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/**
 * Ultron in nanotech (3.12, the boss of the Horton manor's storage): he flies, hits hard, fires red lasers, calls
 * people the nanotech has taken ({@link NanoInfectedEntity}) to fight for him, and every so often turns into a cloud
 * of nano particles that no blow, arrow or other physical thing can touch (only energy, magic and fire do).
 *
 * <p>At half his health, if Vision is fighting him, he stops and asks: "What am I to you?". "Nothing" and the fight
 * goes on; "You are not my father. You are my something." and he flies away, and the people he took are themselves
 * again (villagers).
 */
public class NanoUltronEntity extends Monster {
    private static final EntityDataAccessor<Boolean> NANO = SynchedEntityData.defineId(NanoUltronEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int NANO_EVERY = 260;
    private static final int NANO_FOR = 90;
    private static final int LASER_EVERY = 50;
    private static final int CALL_EVERY = 220;
    private static final int MAX_INFECTED = 6;

    private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.enhanced.nano_ultron"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean asked;
    private int quietUntil;
    private int leaving = -1;
    private BlockPos home = BlockPos.ZERO;

    public NanoUltronEntity(EntityType<? extends NanoUltronEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.FLYING_SPEED, 0.75)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NANO, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 24.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Whether he is a cloud of nano particles now. */
    public boolean nano() {
        return entityData.get(NANO);
    }

    public void setHome(BlockPos home) {
        this.home = home;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (nano()) {
                for (int i = 0; i < 6; i++) {
                    level().addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.1F), 1.2F),
                            getRandomX(0.8), getRandomY(), getRandomZ(0.8), 0, 0.02, 0);
                }
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (leaving >= 0) {
            fly(level);
            return;
        }
        // the nano form: on for a while, then off
        int phase = tickCount % NANO_EVERY;
        boolean wantNano = phase >= NANO_EVERY - NANO_FOR;
        if (wantNano != nano()) {
            entityData.set(NANO, wantNano);
            level.playSound(null, getX(), getY(), getZ(), wantNano ? SoundEvents.SCULK_CLICKING : SoundEvents.IRON_GOLEM_REPAIR,
                    SoundSource.HOSTILE, 1.2F, 0.7F);
        }
        LivingEntity target = getTarget();
        if (tickCount < quietUntil) {
            setDeltaMovement(getDeltaMovement().scale(0.5));
            return;
        }
        if (target != null && target.isAlive()) {
            Vec3 to = target.position().add(0, 2.5, 0);
            if (distanceToSqr(target) > 9.0) {
                getMoveControl().setWantedPosition(to.x, to.y, to.z, 1.0);
            } else if (tickCount % 20 == 0) {
                doHurtTarget(target);
            }
            getLookControl().setLookAt(target);
            if (tickCount % LASER_EVERY == 0 && hasLineOfSight(target)) {
                laser(level, target);
            }
            if (tickCount % CALL_EVERY == 0) {
                call(level, target);
            }
        }
        if (!asked && getHealth() <= getMaxHealth() / 2.0F) {
            ask(level);
        }
    }

    private void laser(ServerLevel level, LivingEntity target) {
        Vec3 from = getEyePosition();
        Vec3 dir = target.getEyePosition().subtract(from).normalize();
        for (int side : new int[] {-1, 1}) {
            Vec3 hand = from.add(dir.cross(new Vec3(0, 1, 0)).normalize().scale(0.35 * side));
            UltronBoltEntity bolt = UltronBoltEntity.create(level, this, hand, dir.scale(1.6), 9.0F, UltronBoltEntity.Palette.HUMAN);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 2.0F);
    }

    /** Up to {@link #MAX_INFECTED} taken people near him. */
    private void call(ServerLevel level, LivingEntity target) {
        List<NanoInfectedEntity> near = level.getEntitiesOfClass(NanoInfectedEntity.class, getBoundingBox().inflate(32));
        int room = MAX_INFECTED - near.size();
        for (int i = 0; i < Math.min(2, room); i++) {
            NanoInfectedEntity infected = ModEntities.NANO_INFECTED.get().create(level);
            if (infected == null) {
                continue;
            }
            infected.setKind(NanoInfectedEntity.randomKind(level.random));
            Vec3 at = target.position().add((random.nextDouble() - 0.5) * 8.0, 0.0, (random.nextDouble() - 0.5) * 8.0);
            BlockPos ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at));
            infected.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            infected.setTarget(target);
            level.addFreshEntity(infected);
            level.sendParticles(ParticleTypes.SMOKE, infected.getX(), infected.getY() + 1, infected.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        }
    }

    /** Half way down: if Vision fights him, he asks his question. */
    private void ask(ServerLevel level) {
        asked = true;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32))) {
            if (Powers.is(player, Power.VISION)) {
                quietUntil = tickCount + 20 * 30;
                entityData.set(NANO, false);
                MindTalk.start(player, "nano_question", this, true);
                return;
            }
        }
    }

    /** The answer was "You are my something": he goes, and the people he took are themselves again. */
    public void makePeace(ServerPlayer by) {
        if (leaving >= 0) {
            return;
        }
        leaving = 0;
        ServerLevel level = (ServerLevel) level();
        for (NanoInfectedEntity infected : level.getEntitiesOfClass(NanoInfectedEntity.class, getBoundingBox().inflate(48))) {
            infected.free();
        }
        by.displayClientMessage(Component.translatable("message.enhanced.nano_ultron.peace"), false);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    /** The answer was "Nothing": the fight goes on. */
    public void goOn() {
        quietUntil = 0;
    }

    private void fly(ServerLevel level) {
        leaving++;
        setDeltaMovement(0.0, 0.6, 0.0);
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
        level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.1F), 1.5F), getX(), getY() + 1, getZ(), 12, 0.4, 0.8, 0.4, 0.02);
        if (leaving > 80) {
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (leaving >= 0) {
            return false;
        }
        if (nano() && physical(source)) {
            if (level() instanceof ServerLevel level) {
                level.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.1F), 1.0F), getX(), getY() + 1, getZ(), 14, 0.4, 0.7, 0.4, 0.05);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    /** A blow, an arrow, a falling thing: what nano particles let through. */
    static boolean physical(DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.WITCH_RESISTANT_TO)
                || source.is(DamageTypeTags.IS_LIGHTNING)) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        return direct == null || direct == source.getEntity() || direct instanceof Projectile;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Asked", asked);
        tag.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        asked = tag.getBoolean("Asked");
        home = BlockPos.of(tag.getLong("Home"));
    }

    /** The boss of a storage, if one is fighting near it. */
    static NanoUltronEntity near(ServerLevel level, BlockPos pos) {
        List<NanoUltronEntity> all = level.getEntitiesOfClass(NanoUltronEntity.class, new AABB(pos).inflate(64));
        return all.isEmpty() ? null : all.get(0);
    }

    static boolean isNano(Mob mob) {
        return mob instanceof NanoUltronEntity n && n.nano();
    }
}
