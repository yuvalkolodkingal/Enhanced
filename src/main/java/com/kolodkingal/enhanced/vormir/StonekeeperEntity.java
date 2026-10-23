package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.stones.StoneProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Stonekeeper of the Vormir Cliff (3.6, the plan's 6.3): the hooded keeper of the Soul Stone, hovering a little
 * over his place on the summit ({@link #HOVER}). He cannot be hurt (only {@code /kill} and the void reach him), pushed,
 * moved, leashed or affected by anything; he never leaves his place (put back every tick), drops nothing, never
 * despawns, and turns only his head and body to look at the nearest player within 12 blocks. Right-clicked, he speaks
 * his three lines in turn ({@link Stonekeeper#lines}), one every {@link Stonekeeper#LINE_GAP} ticks, and the player has
 * met him ({@link StoneProgress#MET_STONEKEEPER}). The template places him; {@link VormirKeeper} puts a new one at his
 * place when he is missing.
 */
public class StonekeeperEntity extends PathfinderMob {
    /** How high (blocks) he floats over his place. */
    public static final double HOVER = 0.3;

    /** His place (the block he floats over); set from where he first stands. */
    @Nullable
    private BlockPos home;
    /** What he is saying to whom: the lines left and when the next one is due. */
    private final Map<UUID, Speech> speeches = new HashMap<>();

    /** A speech to one player: its lines, when it began, how many have been said. */
    private static final class Speech {
        private final List<String> lines;
        private final long start;
        private int said;

        private Speech(List<String> lines, long start) {
            this.lines = lines;
            this.start = start;
        }
    }

    public StonekeeperEntity(EntityType<? extends StonekeeperEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** A new keeper floating at {@code home}, facing south (toward the chasm and the pool). */
    public static StonekeeperEntity spawn(ServerLevel level, BlockPos home) {
        StonekeeperEntity keeper = new StonekeeperEntity(ModEntities.STONEKEEPER.get(), level);
        keeper.home = home.immutable();
        keeper.moveTo(home.getX() + 0.5, home.getY() + HOVER, home.getZ() + 0.5, 0.0F, 0.0F);
        keeper.setYHeadRot(0.0F);
        keeper.setYBodyRot(0.0F);
        keeper.finalizeSpawn(level, level.getCurrentDifficultyAt(home), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(keeper);
        return keeper;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12.0F, 1.0F));
    }

    /** His place, or null before he has stood anywhere. */
    @Nullable
    public BlockPos home() {
        return home;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (home == null) {
            home = blockPosition(); // (placed on his floor or already floating over it: the same block)
        }
        Vec3 place = new Vec3(home.getX() + 0.5, home.getY() + HOVER, home.getZ() + 0.5);
        if (position().distanceToSqr(place) > 1.0E-4) {
            setPos(place);
        }
        setDeltaMovement(Vec3.ZERO);
        clearFire();
        speak();
    }

    // ---- his lines ----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer server && !speeches.containsKey(server.getUUID())) {
            speeches.put(server.getUUID(), new Speech(Stonekeeper.lines(server), level().getGameTime()));
            ModAttachments.STONES.set(server, ModAttachments.STONES.get(server)
                    .with(StoneProgress.MET_STONEKEEPER));
            getLookControl().setLookAt(player);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Each line of each speech when it is due; a listener who has left ends his. */
    private void speak() {
        if (speeches.isEmpty() || !(level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Map.Entry<UUID, Speech>> it = speeches.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Speech> entry = it.next();
            Speech speech = entry.getValue();
            ServerPlayer listener = level.getServer().getPlayerList().getPlayer(entry.getKey());
            long elapsed = now - speech.start;
            if (listener == null || listener.level() != level || elapsed < 0) {
                it.remove();
                continue;
            }
            while (speech.said < speech.lines.size() && elapsed >= (long) Stonekeeper.LINE_GAP * speech.said) {
                Stonekeeper.say(listener, speech.lines.get(speech.said++));
            }
            if (elapsed >= (long) Stonekeeper.LINE_GAP * speech.lines.size()) {
                it.remove();
            }
        }
    }

    /** Whether he is speaking to the player now. */
    public boolean speaksTo(Player player) {
        return speeches.containsKey(player.getUUID());
    }

    // ---- nothing reaches him ----

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(net.minecraft.world.level.Explosion explosion) {
        return true;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
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
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    // ---- saved ----

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(pos -> home = pos);
    }
}
