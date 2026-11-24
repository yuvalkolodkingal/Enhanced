package com.kolodkingal.enhanced.hydra;

import com.kolodkingal.enhanced.multiverse.HydraCity;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The two who rule the city of What If... Hydra Won? (3.19.1), drawn as Hydra's officers are, in their own skins:
 * <ul>
 *     <li><b>Red Skull</b> (Johann Schmidt), Hydra's founder in the films: Erskine's first serum made him strong and
 *     burned his face to a red skull. He fires his Luger and, every few seconds, a blast of the Tesseract's blue power;
 *     at half health he calls his men ("Cut off one head, two more shall take its place").</li>
 *     <li><b>Captain Hydra</b> (Steve Rogers as Kobik, the living Cosmic Cube, remade him: a Hydra sleeper since
 *     childhood, the Supreme Leader of Secret Empire): a super-soldier in black with Hydra's red, who fights hand to hand,
 *     leaps at whoever he fights, stops shots from the front with his shield, and rallies the soldiers near him.</li>
 * </ul>
 * Each has a boss bar. When one falls he drops what made him (the serum, the shard of Kobik) and {@link HydraCity}
 * writes it down; when both have fallen the city is free.
 */
public class HydraBossEntity extends HydraOfficerEntity {
    public static final int RED_SKULL = 100;
    public static final int CAPTAIN_HYDRA = 101;
    /** How often Red Skull's Tesseract blast and Captain Hydra's leap come (ticks). */
    static final int BLAST_EVERY = 90;
    static final int LEAP_EVERY = 70;
    static final float BLAST_DAMAGE = 10.0F;

    private final ServerBossEvent bossBar = new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private boolean calledMen;
    private int nextLine;

    public HydraBossEntity(EntityType<? extends HydraBossEntity> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        setPersistenceRequired();
    }

    /** Makes a boss of the kind ({@link #RED_SKULL}, {@link #CAPTAIN_HYDRA}), at full health. Not added to the level. */
    public static HydraBossEntity make(ServerLevel level, int kind) {
        HydraBossEntity boss = new HydraBossEntity(ModEntities.HYDRA_BOSS.get(), level);
        boss.become(kind);
        return boss;
    }

    void become(int kind) {
        setVariant(kind);
        boolean skull = kind == RED_SKULL;
        setBase(Attributes.MAX_HEALTH, skull ? 300.0 : 360.0);
        setBase(Attributes.ARMOR, skull ? 12.0 : 16.0);
        setBase(Attributes.MOVEMENT_SPEED, skull ? 0.28 : 0.33);
        setBase(Attributes.ATTACK_DAMAGE, skull ? 7.0 : 10.0);
        setBase(Attributes.KNOCKBACK_RESISTANCE, skull ? 0.6 : 0.85);
        setBase(Attributes.FOLLOW_RANGE, 48.0);
        setHealth(getMaxHealth());
        bossBar.setName(getTypeName().copy().withStyle(ChatFormatting.RED));
        this.xpReward = 150;
    }

    public int kind() {
        return variant() == CAPTAIN_HYDRA ? CAPTAIN_HYDRA : RED_SKULL;
    }

    public boolean isRedSkull() {
        return kind() == RED_SKULL;
    }

    /** A boss keeps his own skin: the ranks' skins never touch him. */
    @Override
    public void setRank(Rank rank, int variant) {
        this.xpReward = 150;
    }

    @Override
    public Arms arms() {
        return isRedSkull() ? Arms.PISTOL : Arms.BATON;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable(isRedSkull() ? "entity.enhanced.red_skull" : "entity.enhanced.captain_hydra");
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Captain Hydra's shield stops what is shot at him from the front. */
    @Override
    public boolean blocksWithShield(DamageSource source) {
        if (isRedSkull() || isFrozen() || !(source.getDirectEntity() instanceof Projectile p)) {
            return false;
        }
        Vec3 from = p.getDeltaMovement().lengthSqr() > 1.0E-4 ? p.getDeltaMovement().scale(-1.0) : p.position().subtract(position());
        return HydraSenses.inCone(yBodyRot, Vec3.ZERO, new Vec3(from.x, 0.0, from.z));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || !isAlive()) {
            return;
        }
        bossBar.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (tickCount >= nextLine && target instanceof Player) {
            nextLine = tickCount + 240 + random.nextInt(200);
            say(level, (isRedSkull() ? "hydra.enhanced.red_skull." : "hydra.enhanced.captain_hydra.") + random.nextInt(4));
        }
        if (isRedSkull()) {
            if (tickCount % BLAST_EVERY == 0 && distanceTo(target) <= 28.0 && hasLineOfSight(target)) {
                Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0).subtract(muzzle());
                fire(aim.normalize(), BLAST_DAMAGE, 28.0, HydraBoltEntity.SNIPER, 1.0);
                level.sendParticles(new EnergyParticleOptions(0x3FA9FF, 1.4F), getX(), getY() + 1.4, getZ(), 24, 0.4, 0.5, 0.4, 0.08);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.0F, 1.6F);
            }
            if (!calledMen && getHealth() < getMaxHealth() * 0.5F) {
                calledMen = true;
                say(level, "hydra.enhanced.red_skull.heads");
                for (int i = 0; i < 3; i++) {
                    HydraSoldierEntity soldier = ModEntities.HYDRA_SOLDIER.get().create(level);
                    if (soldier != null) {
                        double angle = i * 2.1;
                        soldier.moveTo(getX() + Math.cos(angle) * 3.0, getY(), getZ() + Math.sin(angle) * 3.0, getYRot(), 0.0F);
                        soldier.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                        soldier.addTag(HydraCity.PATROL);
                        soldier.setTarget(target);
                        level.addFreshEntity(soldier);
                    }
                }
            }
        } else {
            if (tickCount % LEAP_EVERY == 0 && onGround() && distanceTo(target) > 4.0 && distanceTo(target) < 16.0) {
                Vec3 to = target.position().subtract(position());
                Vec3 flat = new Vec3(to.x, 0.0, to.z).normalize().scale(Math.min(1.6, to.length() * 0.12));
                setDeltaMovement(flat.x, 0.55, flat.z);
                hurtMarked = true;
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.7F);
            }
            if (tickCount % 120 == 0) {
                for (HydraSoldierEntity soldier : level.getEntitiesOfClass(HydraSoldierEntity.class, getBoundingBox().inflate(16.0),
                        s -> s != this)) {
                    soldier.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 140, 0, true, true), this);
                }
            }
        }
    }

    /** A line of his to everyone near (chat, in his name). */
    private void say(ServerLevel level, String key) {
        Component line = Component.translatable("hydra.enhanced.says", getTypeName(), Component.translatable(key))
                .withStyle(ChatFormatting.RED);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < 48.0 * 48.0) {
                player.sendSystemMessage(line);
            }
        }
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
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            HydraCity.bossDown(level, this);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        bossBar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Boss", kind());
        tag.putBoolean("CalledMen", calledMen);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float health = getHealth();
        become(tag.getInt("Boss") == CAPTAIN_HYDRA ? CAPTAIN_HYDRA : RED_SKULL);
        if (tag.contains("Health")) {
            setHealth(health);
        }
        calledMen = tag.getBoolean("CalledMen");
    }

    @Nullable
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                                                  net.minecraft.world.DifficultyInstance difficulty, MobSpawnType type,
                                                                  @Nullable net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, difficulty, type, data);
        become(variant() == CAPTAIN_HYDRA ? CAPTAIN_HYDRA : RED_SKULL);
        return out;
    }
}
