package com.kolodkingal.enhanced.asgard.npc;

import com.kolodkingal.enhanced.asgard.AsgardState;
import com.kolodkingal.enhanced.asgard.CityPlan;
import com.kolodkingal.enhanced.protect.Shelters;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * An Asgardian (3.5): the one entity {@code enhanced:asgardian} of every {@link Role} (the named gods and warriors, the
 * Einherjar, the citizens), told apart by the synced role and skin variant.
 *
 * <ul>
 *   <li><b>Never killed by players</b>: a blow of a player (or of his arrow, pet or summons) does nothing; the Asgardian
 *   steps back and tells him to mind his manners (once in 10 s per player). Other harm lands, but at the last of his
 *   health he goes {@link NpcState#DOWN DOWN} (kneels, untouchable) and rises after 20 s at half health: the city never
 *   shrinks. Only the void and {@code /kill} truly kill him (the keeper makes him anew); summoned Einherjar vanish in
 *   gold when they fall or their time runs out.</li>
 *   <li><b>Idle life</b> (goals of {@link AsgardianGoals}): strolls round his home, patrols, guard posts, sparring and
 *   feasting, sitting, talking in pairs; Heimdall keeps his post and greets arrivals; Odin sleeps until Hela first falls.</li>
 *   <li><b>Talk and trade</b>: a right-click opens the talk screen ({@link AsgardTalk}); the traders' offers
 *   ({@link AsgardTrades}) open the vanilla merchant screen.</li>
 *   <li><b>War</b>: the warriors fight Hela's army ({@link DefendAsgardGoal}); the citizens flee from it.</li>
 * </ul>
 */
public class AsgardianEntity extends PathfinderMob implements Merchant {
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(AsgardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(AsgardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(AsgardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PARTNER = SynchedEntityData.defineId(AsgardianEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(AsgardianEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    /** How long a knocked-down Asgardian kneels before he rises (ticks). */
    public static final int DOWN_TICKS = 400;
    /** How long a player waits between two "mind your manners" lines of the same Asgardian (ticks). */
    public static final int MANNERS_COOLDOWN = 200;
    /** The longest a talk keeps an Asgardian still (ticks). */
    public static final int TALK_TICKS = 1200;

    private String slot = "";
    @Nullable
    private BlockPos home;
    private int homeRadius = 16;
    private long downUntil;
    private long stateUntil;
    private long summonUntil;
    @Nullable
    private UUID talkingWith;
    private final Map<UUID, Long> manners = new HashMap<>();
    @Nullable
    private MerchantOffers offers;
    private long restockDay = -1;
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private LivingEntity commanded;
    private long commandUntil;
    @Nullable
    private UUID followId;
    private long followUntil;
    /** Client: the tick of this entity's clock at which its state last changed (the poses ease from it). */
    private int clientStateStart;
    /** Client: the state before the current one (the poses ease out of it). */
    private NpcState clientPreviousState = NpcState.IDLE;
    private int clientPreviousStateOrdinal;
    private boolean goalsMade;

    public AsgardianEntity(EntityType<? extends AsgardianEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanPassDoors(true);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.ARMOR, 4.0).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, Role.CITIZEN.ordinal());
        builder.define(VARIANT, 0);
        builder.define(STATE, NpcState.IDLE.ordinal());
        builder.define(PARTNER, -1);
        builder.define(OWNER, Optional.empty());
    }

    // ---- who he is ----

    public Role role() {
        Role[] all = Role.values();
        int i = entityData.get(ROLE);
        return i >= 0 && i < all.length ? all[i] : Role.CITIZEN;
    }

    /** His skin among his role's ({@link Role#skins()}). */
    public int variant() {
        return entityData.get(VARIANT);
    }

    /**
     * Makes him an Asgardian of {@code role} with skin {@code variant}: his health, armour, blow and size, his weapon and
     * his name. Call before he joins the level (the keeper, the summons) or at any time on the server.
     */
    public void setRole(Role role, int variant) {
        entityData.set(ROLE, role.ordinal());
        entityData.set(VARIANT, Math.floorMod(variant, role.skins()));
        setAttribute(Attributes.MAX_HEALTH, role.health());
        setAttribute(Attributes.ARMOR, role.armor());
        setAttribute(Attributes.ATTACK_DAMAGE, role.damage());
        setAttribute(Attributes.SCALE, role.scale());
        setHealth(getMaxHealth());
        arm();
        if (role.named()) {
            setCustomName(Component.translatable(role.nameKey()));
            setCustomNameVisible(false);
        }
        if (!level().isClientSide) {
            makeGoals();
        }
    }

    private void setAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** The weapons and shields of his role (props: they never drop). */
    void arm() {
        ItemStack main = ItemStack.EMPTY;
        ItemStack off = ItemStack.EMPTY;
        switch (role()) {
            case HEIMDALL -> main = new ItemStack(ModItems.HOFUND.get());
            case ODIN -> main = state() == NpcState.SLEEPING ? ItemStack.EMPTY : new ItemStack(ModItems.GUNGNIR.get());
            case SIF -> {
                main = new ItemStack(Items.IRON_SWORD);
                off = shield(DyeColor.BLACK);
            }
            case VALKYRIE -> main = new ItemStack(Items.IRON_SWORD);
            case VOLSTAGG -> main = new ItemStack(state() == NpcState.FEASTING ? Items.COOKED_CHICKEN : Items.IRON_AXE);
            case FANDRAL -> main = new ItemStack(Items.IRON_SWORD);
            case HOGUN -> main = new ItemStack(Items.MACE);
            case EINHERJAR -> {
                main = new ItemStack(Items.TRIDENT);
                off = shield(DyeColor.RED);
            }
            default -> {
            }
        }
        setItemSlot(EquipmentSlot.MAINHAND, main);
        setItemSlot(EquipmentSlot.OFFHAND, off);
    }

    private static ItemStack shield(DyeColor color) {
        ItemStack shield = new ItemStack(Items.SHIELD);
        shield.set(DataComponents.BASE_COLOR, color);
        return shield;
    }

    /** The keeper's slot he fills ({@code FRIGGA}, {@code EINHERJAR#3}), or "" (summoned, or made by a command). */
    public String slot() {
        return slot;
    }

    /** Gives him his slot and his home (where he strolls, within {@code radius}). */
    public void setHome(String slot, BlockPos home, int radius) {
        this.slot = slot;
        this.home = home.immutable();
        this.homeRadius = radius;
        restrictTo(home, radius);
    }

    /** Where he lives (his post), or null (summoned, or a stray made by a command). */
    @Nullable
    public BlockPos home() {
        return home;
    }

    public int homeRadius() {
        return homeRadius;
    }

    /** Odin lives in his chamber while he sleeps and walks the throne room once he wakes. */
    BlockPos activeHome() {
        if (role() == Role.ODIN && home != null && level() instanceof ServerLevel server && AsgardState.get(server.getServer()).odinAwake()) {
            return CityPlan.THRONE_ROOM_CENTER;
        }
        return home;
    }

    // ---- what he is doing ----

    public NpcState state() {
        return NpcState.byOrdinal(entityData.get(STATE));
    }

    /** Sets his state (synced: the clients pose him by it), for {@code ticks} (0: until changed). */
    public void setState(NpcState state, int ticks) {
        NpcState before = state();
        entityData.set(STATE, state.ordinal());
        stateUntil = ticks > 0 ? level().getGameTime() + ticks : 0L;
        if (before != state) {
            if (state != NpcState.TALKING && state != NpcState.SPARRING) {
                setPartner(null);
            }
            if (role() == Role.VOLSTAGG || role() == Role.ODIN) {
                arm();
            }
        }
    }

    /** The entity he talks or spars with (synced: he faces it), or null. */
    @Nullable
    public Entity partner() {
        int id = entityData.get(PARTNER);
        return id < 0 ? null : level().getEntity(id);
    }

    public void setPartner(@Nullable Entity partner) {
        entityData.set(PARTNER, partner == null ? -1 : partner.getId());
    }

    /** Whether nothing holds him: idle and free to walk, talk in pairs, spar or sit. */
    public boolean free() {
        return state() == NpcState.IDLE && getTarget() == null && !summoned() && leader() == null;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key) && level().isClientSide) {
            clientPreviousState = NpcState.byOrdinal(clientPreviousStateOrdinal);
            clientPreviousStateOrdinal = entityData.get(STATE);
            clientStateStart = tickCount;
        }
    }

    /** Client: ticks (with the partial tick) since his state last changed. */
    public float stateAge(float partialTick) {
        return tickCount - clientStateStart + partialTick;
    }

    /** Client: the state he was in before the current one. */
    public NpcState previousState() {
        return clientPreviousState;
    }

    // ---- the summoned Einherjar ----

    /** The player who summoned him (Heimdall's call, Odin's command), or null for the city's own. */
    @Nullable
    public UUID owner() {
        return entityData.get(OWNER).orElse(null);
    }

    public boolean summoned() {
        return entityData.get(OWNER).isPresent();
    }

    /** Makes him a summoned warrior of {@code owner} for {@code ticks}. */
    public void summonFor(ServerPlayer owner, int ticks) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        summonUntil = level().getGameTime() + ticks;
        slot = "";
        home = null;
        clearRestriction();
    }

    @Nullable
    public ServerPlayer ownerPlayer() {
        UUID id = owner();
        return id != null && level() instanceof ServerLevel server && server.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer p
                ? p : null;
    }

    /** The target his master commands (Odin's command), while the command lasts. */
    @Nullable
    LivingEntity commanded() {
        if (commanded != null && (!commanded.isAlive() || level().getGameTime() > commandUntil)) {
            commanded = null;
        }
        return commanded;
    }

    /** Odin's command without a foe: follow {@code player} for {@code ticks}. */
    public void followFor(ServerPlayer player, int ticks) {
        followId = player.getUUID();
        followUntil = level().getGameTime() + ticks;
    }

    /** Whom he follows: his master (summoned), or the one who commanded him to, while it lasts; else null. */
    @Nullable
    public ServerPlayer leader() {
        ServerPlayer owner = ownerPlayer();
        if (owner != null) {
            return owner;
        }
        if (followId != null && level().getGameTime() < followUntil && level() instanceof ServerLevel server
                && server.getPlayerByUUID(followId) instanceof ServerPlayer p) {
            return p;
        }
        followId = null;
        return null;
    }

    /** Odin's command: attack {@code target} for {@code ticks}. */
    public void command(LivingEntity target, int ticks) {
        commanded = target;
        commandUntil = level().getGameTime() + ticks;
        setTarget(target);
    }

    // ---- goals ----

    @Override
    protected void registerGoals() {
        // The role is not known yet when the constructor runs: the goals come with it (setRole, readAdditionalSaveData).
    }

    private void makeGoals() {
        if (goalsMade) {
            return;
        }
        goalsMade = true;
        Role role = role();
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new AsgardianGoals.HoldStill(this));
        goalSelector.addGoal(1, new AsgardianGoals.Fight(this));
        if (!role.warrior()) {
            goalSelector.addGoal(1, new AsgardianGoals.Flee(this));
        }
        goalSelector.addGoal(2, new AsgardianGoals.FollowOwner(this));
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        goalSelector.addGoal(3, new AsgardianGoals.ReturnHome(this));
        goalSelector.addGoal(3, new AsgardianGoals.SparPlayer(this));
        switch (role) {
            case HEIMDALL -> goalSelector.addGoal(4, new AsgardianGoals.KeepPost(this, true));
            case ODIN -> goalSelector.addGoal(4, new AsgardianGoals.Sleep(this));
            case SIF, FANDRAL, HOGUN -> goalSelector.addGoal(4, new AsgardianGoals.Spar(this));
            case VOLSTAGG -> goalSelector.addGoal(4, new AsgardianGoals.Feast(this));
            case EINHERJAR -> {
                goalSelector.addGoal(4, new AsgardianGoals.KeepPost(this, false));
                goalSelector.addGoal(5, new AsgardianGoals.Patrol(this));
            }
            default -> goalSelector.addGoal(5, new AsgardianGoals.Sit(this));
        }
        goalSelector.addGoal(6, new AsgardianGoals.TalkInPairs(this));
        goalSelector.addGoal(7, new AsgardianGoals.Stroll(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new AsgardianGoals.Defend(this));
        targetSelector.addGoal(2, new DefendAsgardGoal(this));
    }

    // ---- the tick ----

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            serverTick(server);
        }
    }

    private void serverTick(ServerLevel server) {
        long now = server.getGameTime();
        NpcState state = state();
        if (state == NpcState.DOWN) {
            getNavigation().stop();
            if (now >= downUntil) {
                rise();
            } else if (tickCount % 10 == 0) {
                server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 1, 0.3, 0.05, 0.3, 0.0);
            }
        } else if (stateUntil > 0 && now >= stateUntil) {
            if (state == NpcState.TALKING || talkingWith == null) {
                endTalk();
                setState(NpcState.IDLE, 0);
            } else {
                // A bow (or a sit) before a talk: back to the talk.
                Player player = server.getPlayerByUUID(talkingWith);
                setState(NpcState.TALKING, TALK_TICKS);
                setPartner(player);
            }
        }
        if (state == NpcState.TALKING && talkingWith != null) {
            Player player = server.getPlayerByUUID(talkingWith);
            if (player == null || player.distanceTo(this) > AsgardTalk.talkDistance() + 3) {
                endTalk();
            } else {
                getLookControl().setLookAt(player, 30.0F, 30.0F);
            }
        }
        if (state == NpcState.SLEEPING && tickCount % 8 == 0) {
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.6, getZ(), 1, 0.6, 0.3, 0.6, 0.002);
        }
        if (summoned()) {
            ServerPlayer owner = ownerPlayer();
            if (now >= summonUntil || owner == null || owner.level() != level() || !owner.isAlive()) {
                vanish();
                return;
            }
        }
        if (tradingPlayer != null && (!tradingPlayer.isAlive() || tradingPlayer.distanceTo(this) > 12)) {
            setTradingPlayer(null);
        }
    }

    /** Summoned Einherjar leave in a flash of gold. */
    public void vanish() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1.0F, 0.85F, 0.35F), 1.4F),
                    getX(), getY() + 1.0, getZ(), 24, 0.35, 0.8, 0.35, 0.0);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.EINHERJAR_SUMMON.get(), getSoundSource(), 0.6F, 1.4F);
        }
        discard();
    }

    // ---- never killed by a player ----

    /**
     * Whether a blow comes from a player: his own hand, his arrow or thrown thing, his tamed pet or anything else he owns
     * ({@link OwnableEntity}: a hero's summons).
     */
    public static boolean fromPlayer(DamageSource source) {
        for (Entity e : new Entity[]{source.getEntity(), source.getDirectEntity()}) {
            Entity maker = Shelters.maker(e);
            if (maker instanceof Player) {
                return true;
            }
            if (maker instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return false;
        }
        boolean lethal = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (!lethal && state() == NpcState.DOWN) {
            return false;
        }
        if (!lethal && fromPlayer(source)) {
            Entity maker = Shelters.maker(source.getEntity() != null ? source.getEntity() : source.getDirectEntity());
            if (maker instanceof ServerPlayer player) {
                if (AsgardianGoals.SparPlayer.hit(this, player)) {
                    return false;
                }
                mindYourManners(player);
            }
            return false;
        }
        if (!lethal && summoned() && source.getEntity() instanceof AsgardianEntity) {
            return false;
        }
        return super.hurt(source, amount);
    }

    /** A player struck him: he steps back and tells him to mind his manners (once in a while). */
    void mindYourManners(ServerPlayer player) {
        Vec3 away = position().subtract(player.position()).multiply(1, 0, 1);
        if (away.lengthSqr() > 1.0E-4) {
            away = away.normalize().scale(0.45);
            setDeltaMovement(getDeltaMovement().add(away.x, 0.15, away.z));
            hurtMarked = true;
        }
        getLookControl().setLookAt(player, 60.0F, 60.0F);
        long now = level().getGameTime();
        Long last = manners.get(player.getUUID());
        if (last == null || now - last >= MANNERS_COOLDOWN) {
            manners.put(player.getUUID(), now);
            AsgardTalk.speak(this, player, AsgardDialogue.mannersLine(role(), getRandom()), java.util.List.of());
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !summoned() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            goDown();
            return;
        }
        if (summoned() && !level().isClientSide) {
            vanish();
            return;
        }
        super.die(source);
    }

    /** Knocked down: he kneels, untouchable, and rises after {@link #DOWN_TICKS}. */
    public void goDown() {
        setHealth(1.0F);
        dead = false;
        deathTime = 0;
        setTarget(null);
        getNavigation().stop();
        endTalk();
        setTradingPlayer(null);
        downUntil = level().getGameTime() + DOWN_TICKS;
        setState(NpcState.DOWN, 0);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.NPC_DOWN.get(), getSoundSource(), 1.0F, 1.0F);
    }

    /** He rises, at half his health. */
    public void rise() {
        setHealth(getMaxHealth() * 0.5F);
        downUntil = 0;
        setState(NpcState.IDLE, 0);
    }

    /** Whether he is down (for the tests and the other lanes). */
    public boolean isDown() {
        return state() == NpcState.DOWN;
    }

    @Override
    public boolean isPushable() {
        return state() != NpcState.DOWN && state() != NpcState.SLEEPING && super.isPushable();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof Player player && !mayHurtPlayer(player)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            swing(InteractionHand.MAIN_HAND);
        }
        return hit;
    }

    /** A city Asgardian never hurts a player; a summoned one only one who attacked his master, with PvP on. */
    boolean mayHurtPlayer(Player player) {
        ServerPlayer owner = ownerPlayer();
        return owner != null && player != owner && level() instanceof ServerLevel server && server.getServer().isPvpAllowed()
                && owner.getLastHurtByMob() == player;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof AsgardianEntity) {
            return false;
        }
        if (target instanceof Player player && !mayHurtPlayer(player)) {
            return false;
        }
        return super.canAttack(target);
    }

    // ---- staying in Asgard ----

    /** The city's people never leave their realm (the summoned ones stay with their master's level and vanish). */
    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected void pickUpItem(ItemEntity item) {
    }

    // ---- talking ----

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return state().still() || state() == NpcState.FIGHTING ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer server && AsgardTalk.begin(server, this)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** A player talks to him: he stops, turns to the player and keeps still while the talk lasts. */
    public void startTalk(ServerPlayer player) {
        talkingWith = player.getUUID();
        getNavigation().stop();
        if (state() == NpcState.TALKING || state() == NpcState.IDLE || state() == NpcState.SITTING || state() == NpcState.FEASTING
                || state() == NpcState.SPARRING) {
            setState(NpcState.TALKING, TALK_TICKS);
            setPartner(player);
        }
    }

    /** The talk is over. */
    public void endTalk() {
        if (talkingWith != null) {
            talkingWith = null;
            if (state() == NpcState.TALKING) {
                setState(NpcState.IDLE, 0);
            }
        }
    }

    /** The player he talks to, or null. */
    @Nullable
    public UUID talkingWith() {
        return talkingWith;
    }

    /** The sound of his voice (a murmur, pitched by role). */
    public SoundEvent voice() {
        return switch (role().voice()) {
            case LOW -> ModSounds.NPC_TALK_LOW.get();
            case MID -> ModSounds.NPC_TALK_MID.get();
            case HIGH -> ModSounds.NPC_TALK_HIGH.get();
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.NPC_DOWN.get();
    }

    @Override
    public Component getTypeName() {
        return Component.translatable(role().nameKey());
    }

    // ---- trading (the vanilla merchant screen) ----

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) {
            offers = AsgardTrades.offers(role());
        }
        if (level() instanceof ServerLevel server) {
            long day = server.getDayTime() / 24000L;
            if (restockDay < 0) {
                restockDay = day;
            } else if (day != restockDay) {
                restockDay = day;
                restock();
            }
        }
        return offers;
    }

    /** Every offer back to full (the traders restock once a day). */
    public void restock() {
        if (offers != null) {
            for (MerchantOffer offer : offers) {
                offer.resetUses();
            }
        }
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        ambientSoundTime = -getAmbientSoundInterval();
        playSound(voice(), 1.0F, 1.0F);
        if (tradingPlayer instanceof ServerPlayer player) {
            AsgardTrades.traded(this, player, offer);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return voice();
    }

    @Override
    public boolean isClientSide() {
        return level().isClientSide;
    }

    /** Opens his trades for {@code player} (Sif, Valkyrie, Volstagg). */
    public void openTrades(ServerPlayer player) {
        if (!role().trader() || getOffers().isEmpty()) {
            return;
        }
        setTradingPlayer(player);
        openTradingScreen(player, Component.translatable(role().nameKey()), 1);
    }

    // ---- spawning and saving ----

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        if (!goalsMade) {
            setRole(role(), level.getRandom().nextInt(role().skins()));
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Role", role().name());
        tag.putInt("Variant", variant());
        tag.putString("State", state().name());
        tag.putString("Slot", slot);
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
            tag.putInt("HomeRadius", homeRadius);
        }
        tag.putLong("DownUntil", downUntil);
        if (offers != null) {
            tag.putLong("RestockDay", restockDay);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        Role role = tag.contains("Role") ? Role.byName(tag.getString("Role")) : role();
        float health = getHealth();
        setRole(role, tag.getInt("Variant"));
        if (tag.contains("Health")) {
            setHealth(health);
        }
        slot = tag.getString("Slot");
        NbtUtils.readBlockPos(tag, "Home").ifPresent(pos -> setHome(slot, pos, tag.contains("HomeRadius") ? tag.getInt("HomeRadius") : 16));
        NpcState state = NpcState.byName(tag.getString("State"));
        // A talk, a spar or a bow does not outlive a save; a fall does (he rises when it is over), and so does sleep.
        if (state == NpcState.DOWN || state == NpcState.SLEEPING) {
            setState(state, 0);
        }
        downUntil = tag.getLong("DownUntil");
        restockDay = tag.contains("RestockDay") ? tag.getLong("RestockDay") : -1;
        arm();
    }
}
