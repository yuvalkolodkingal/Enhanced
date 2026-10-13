package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.BossConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One running boss fight: everything the three villains' fights share and the rules every one of them follows, so that
 * a boss only has to say what is <em>its</em> own. A boss lane extends this class ({@code UltronFight}, {@code
 * ThanosFight}, {@code ApocalypseFight}), registers a {@link BossLane} that makes it, and the framework does the rest:
 *
 * <ul>
 *     <li><b>Participants and the ledger.</b> Every player who comes within the arena's join radius joins (Creative
 *     players too; Spectators never), stays a participant when they die or log out, and has a ledger of named counters
 *     ({@link Participant#counter}: the damage they did, a boss's own, like Apocalypse's shield), the ticks they were
 *     present and whether they struck the last blow.</li>
 *     <li><b>The end.</b> A fight ends by itself when it is <em>won</em>, <em>wiped</em> (every fighter dead or gone
 *     for {@code wipeSeconds}), <em>abandoned</em> (nobody near for {@code abandonSeconds}) or <em>timed out</em>
 *     ({@code maxFightMinutes}), when the difficulty goes Peaceful, and when the lane calls {@link #end} (a withdrawal,
 *     an abort). Operators and the server end it from outside ({@link BossFights#stopAll}). Every ending is one of
 *     {@link BossEnd}, which says whether the fighters are rewarded and whether the ritual item is refunded.</li>
 *     <li><b>Winding down.</b> After the end the fight goes on ticking until the lane says its ending is over ({@link
 *     #windDown}: the boss leaves, the island lowers); only then are the arena's protection and tickets, the bars and
 *     the fight's entities let go, and the arena goes on holding its fighters. A lane that forgets is stopped after
 *     {@link #MAX_WIND_DOWN_TICKS}.</li>
 *     <li><b>The rest.</b> Two boss bars ({@link BossBars}), the arena ({@link BossArena}), the region tickets, the
 *     orphan sweep (an entity of a fight that is not running is discarded when it joins a level), rewards ({@link
 *     BossRewards}) and gifts ({@link BossGifts}).</li>
 * </ul>
 *
 * <p>Everything here runs on the server thread. A lane adds its own fight entities with {@link #spawn} (or {@link
 * #adopt} before it adds them itself: an entity of a boss type that is not marked as this fight's is swept as an orphan
 * the moment it joins the level).
 */
public abstract class BossFight {
    /** The ledger counter every fight keeps: the damage a participant did to the boss and its minions. */
    public static final String DAMAGE = "damage";
    /** A fight that is over but never finishes its ending is finished by force after this many ticks (five minutes). */
    public static final int MAX_WIND_DOWN_TICKS = 6000;
    /** The bars and the speech go to whoever is near, and the bars are refreshed this often, in ticks. */
    private static final int BAR_INTERVAL = 20;
    /** The name of the mark that ties an entity to its fight, in its persistent data. */
    private static final String MARK = Enhanced.MODID + ":boss_fight";

    private final UUID id = UUID.randomUUID();
    private final ServerLevel level;
    private final BossArena arena;
    private final BossBars bars;
    private final Map<UUID, Participant> participants = new LinkedHashMap<>();
    private final List<ItemEntity> keptDrops = new ArrayList<>();
    private List<ChunkPos> ticketed = List.of();

    private ItemStack refund = ItemStack.EMPTY;
    @Nullable
    private UUID starter;
    private long age;
    private int wipeTicks;
    private int abandonTicks;
    private int windDownTicks;
    private boolean begun;
    private boolean finished;
    private boolean hexExempt;
    @Nullable
    private BossEnd end;
    private List<ServerPlayer> fighters = List.of();

    protected BossFight(ServerLevel level, BossArena arena, BossBars bars) {
        this.level = level;
        this.arena = arena;
        this.bars = bars;
    }

    // ---------------------------------------------------------------------------------------------------------
    // What a lane says

    /** Which villain this fight is against. */
    public abstract BossKind kind();

    /** Called once the fight is registered, with the player who started it (the first participant). */
    protected void onBegin(ServerPlayer starter) {
    }

    /**
     * Called every tick while the fight is running (not after it ended): the lane's states, its spawning, its speech,
     * and the boss bars ({@link #bars}). The framework has already joined the players, kept the ledger and checked for
     * a wipe, an abandonment and the time limit.
     */
    protected abstract void onTick();

    /**
     * What a participant who qualifies is given when the boss falls (see {@link BossRewards#eligible}): one Core, one
     * Gauntlet, one Essence, and anything else the lane adds. The player may be offline: the stacks are then kept as
     * gifts. Called only for {@link BossEnd#VICTORY}.
     */
    protected abstract List<ItemStack> rewards(Participant participant);

    /**
     * The least damage (the sum of {@link #rewardCounters}) a participant must have done to earn the reward by being
     * present long enough: the lane's own threshold (Thanos: 3 percent of his health), or anything above zero.
     */
    protected float rewardMinDamage() {
        return Float.MIN_VALUE;
    }

    /** The ledger counters whose sum is the "damage" of the reward rule: by default {@link #DAMAGE}. */
    protected Set<String> rewardCounters() {
        return Set.of(DAMAGE);
    }

    /**
     * Whether the framework credits the damage dealt to the fight's entities to the player who dealt it (through
     * {@code LivingDamageEvent.Post}). A lane that credits every point itself overrides this to false.
     */
    protected boolean creditsDamageEvents() {
        return true;
    }

    /**
     * How long the boss rests after the fight ended this way before it can be called again, in ticks (default none).
     */
    protected long restTicks(BossEnd why) {
        return 0L;
    }

    /**
     * The fight is over ({@code why}) and the rewards and the refund are already out: start the ending (the boss
     * leaves, its minions fall, the island lowers). The fight goes on ticking, protected, until {@link #windDown}
     * answers true.
     */
    protected void onEnd(BossEnd why) {
    }

    /**
     * Called every tick after the fight ended, with the ticks since.
     *
     * @return true when the ending is over and the world is as it was (the default: at once)
     */
    protected boolean windDown(int ticks) {
        return true;
    }

    /**
     * The fight is removed: the last thing the lane does. {@code forced} means the world must be put back <em>now</em>
     * (the server is stopping, or an operator cleans up), without any animation. Called exactly once.
     */
    protected void onFinished(boolean forced) {
    }

    /** Wanda pressed her telekinesis key while holding nothing; true if this fight handled it (Ultron's heart). */
    protected boolean onTelekinesisPress(ServerPlayer wanda) {
        return false;
    }

    /** Wanda throws {@code held}; true if this fight handled it (it is then not thrown). */
    protected boolean onTelekinesisLaunch(ServerPlayer wanda, Entity held) {
        return false;
    }

    /**
     * Whether the clients hide the ground at {@code pos} while this fight runs (the ground under Ultron's island in the
     * sky): nothing lands there, nothing is borrowed from there, and neither Build Magic nor the Magnetic Pull changes it
     * (see {@link BossFights#hidesGround}). None by default.
     */
    protected boolean hidesGround(BlockPos pos) {
        return false;
    }

    /**
     * Whether the fight holds {@code entity} where it is, so that no telekinesis may take it (whatever the Sokovia lift
     * carries; see {@link BossFights#isLocked}). None by default.
     */
    protected boolean locks(Entity entity) {
        return false;
    }

    /** A player joined the fight (their first time within the join radius). */
    protected void onJoin(ServerPlayer player) {
    }

    /** One line about the fight for {@code /enhanced boss list}: its phase, in the lane's words. */
    protected String status() {
        return end == null ? "running" : "ending";
    }

    // ---------------------------------------------------------------------------------------------------------
    // What a lane can ask

    public final UUID id() {
        return id;
    }

    public final ServerLevel level() {
        return level;
    }

    public final MinecraftServer server() {
        return level.getServer();
    }

    public final BossArena arena() {
        return arena;
    }

    public final BossBars bars() {
        return bars;
    }

    /** Ticks since the fight began. */
    public final long age() {
        return age;
    }

    /** Whether the fight has ended (it may still be winding down). */
    public final boolean isOver() {
        return end != null;
    }

    /** How the fight ended, or null while it runs. */
    @Nullable
    public final BossEnd result() {
        return end;
    }

    /** Whether the fight is gone for good: its entities, tickets and bars are let go. */
    public final boolean isFinished() {
        return finished;
    }

    /** The ticks since the fight ended (0 while it runs). */
    public final int windDownTicks() {
        return windDownTicks;
    }

    @Nullable
    public final UUID starter() {
        return starter;
    }

    /**
     * Whether the Hex lets the fight's members through its wall (see {@link BossFights#isHexExempt}): Ultron's lift
     * carries everybody through the dome. Off by default.
     */
    public final boolean hexExempt() {
        return hexExempt;
    }

    public final void setHexExempt(boolean hexExempt) {
        this.hexExempt = hexExempt;
    }

    /**
     * The ritual item the fight consumed, given back to whoever started it if the fight ends through no fault of
     * theirs.
     */
    public final void refundOnFailure(ItemStack consumed) {
        this.refund = consumed.copyWithCount(1);
    }

    // ---- participants ----

    public final Collection<Participant> participants() {
        return Collections.unmodifiableCollection(participants.values());
    }

    @Nullable
    public final Participant participant(UUID player) {
        return participants.get(player);
    }

    /** Whether the player took part in the fight (they may be dead, offline or far away now). */
    public final boolean isParticipant(UUID player) {
        return participants.containsKey(player);
    }

    /**
     * The participants who are online, alive, in the fight's dimension and not Spectators (Creative ones included),
     * wherever they are in it. Computed once per tick.
     */
    public final List<ServerPlayer> fighters() {
        return fighters;
    }

    /** The fighters who are in the arena: within the join radius of its centre. */
    public final List<ServerPlayer> present() {
        List<ServerPlayer> present = new ArrayList<>();
        for (ServerPlayer player : fighters) {
            if (arena.inJoinRange(player.position())) {
                present.add(player);
            }
        }
        return present;
    }

    /**
     * Adds {@code amount} to the participant's counter (damage dealt, shield worn down, ...). Nothing for a stranger.
     */
    public final void credit(ServerPlayer player, String counter, float amount) {
        if (amount > 0.0F && end == null) {
            Participant participant = participants.get(player.getUUID());
            if (participant != null) {
                participant.counters.merge(counter, amount, Float::sum);
            }
        }
    }

    /** Records that the player struck the last blow: they are rewarded whatever their time and damage. */
    public final void lastBlow(ServerPlayer player) {
        Participant participant = participants.get(player.getUUID());
        if (participant != null && end == null) {
            participants.values().forEach(p -> p.lastBlow = false);
            participant.lastBlow = true;
        }
    }

    // ---- entities ----

    /**
     * Marks the entity as this fight's, so that it is not swept as an orphan and counts as a member: do it before the
     * entity joins the level. The fight discards every entity it marked when it finishes.
     */
    public final <T extends Entity> T adopt(T entity) {
        entity.getPersistentData().putUUID(MARK, id);
        return entity;
    }

    /** Marks the entity and adds it to the fight's level. */
    public final <T extends Entity> T spawn(T entity) {
        adopt(entity);
        level.addFreshEntity(entity);
        return entity;
    }

    /** Whether the entity belongs to this fight. */
    public final boolean owns(Entity entity) {
        return id.equals(markOf(entity));
    }

    /** The id of the fight an entity is marked for, or null if it is marked for none. */
    @Nullable
    static UUID markOf(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.hasUUID(MARK) ? data.getUUID(MARK) : null;
    }

    /**
     * The loaded entities of this fight (the boss, its minions, its projectiles): a scan of the level, not for every
     * tick.
     */
    public final List<Entity> entities() {
        List<Entity> found = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity.isAlive() && owns(entity)) {
                found.add(entity);
            }
        }
        return found;
    }

    /** Whether the entity is a member of this fight: a participant, or something the fight made. */
    public final boolean isMember(Entity entity) {
        return entity instanceof ServerPlayer player ? participants.containsKey(player.getUUID()) : owns(entity);
    }

    // ---- speech and messages ----

    /**
     * The players who see the bars: they hear the boss, too. So does a fighter of the level who is on the death screen:
     * a dead player leaves the level's list a second after they fall, and with it the bars, but the boss's line over a
     * wipe and how the fight ended are for them.
     */
    public final Set<ServerPlayer> audience() {
        Set<ServerPlayer> audience = new LinkedHashSet<>(bars.viewers());
        for (Participant participant : participants.values()) {
            ServerPlayer player = server().getPlayerList().getPlayer(participant.id);
            if (player != null && !player.isAlive() && player.level() == level) {
                audience.add(player);
            }
        }
        return audience;
    }

    /**
     * The boss speaks: a chat line in its colour to everyone who sees the bars, with its voice.
     *
     * @param voice the sound that goes with the line (null for none)
     */
    public final void speak(Component name, int color, Component line, @Nullable SoundEvent voice) {
        MutableComponent text = Component.literal("<").append(name).append("> ").append(line)
                .withStyle(Style.EMPTY.withColor(color));
        for (ServerPlayer player : audience()) {
            player.sendSystemMessage(text);
            if (voice != null) {
                player.playNotifySound(voice, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
    }

    /** Tells everyone who sees the bars something (an action-bar line or a chat line). */
    public final void tell(Component message, boolean actionBar) {
        for (ServerPlayer player : audience()) {
            if (actionBar) {
                player.displayClientMessage(message, true);
            } else {
                player.sendSystemMessage(message);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // The life of a fight (called by BossFights)

    /** Registers the first participant, the tickets and the bars; the lane's {@link #onBegin} follows. */
    final void begin(ServerPlayer starter) {
        if (begun) {
            return;
        }
        begun = true;
        this.starter = starter.getUUID();
        ticketed = ticketChunks();
        for (ChunkPos chunk : ticketed) {
            level.getChunkSource().addRegionTicket(kind().ticket(), chunk, arena.ticketDistance(), id, true);
        }
        join(starter);
        refreshFighters();
        bars.refresh(level, arena.center(), fighters);
        onBegin(starter);
    }

    /** One tick of the fight. */
    final void tick() {
        if (finished || !begun) {
            return;
        }
        if (end != null) {
            windDownTicks++;
            // The arena holds its fighters until the ending is over (Ultron's island is lowered with them on it).
            refreshFighters();
            arena.tick(this, age + windDownTicks);
            if (windDown(windDownTicks) || windDownTicks >= MAX_WIND_DOWN_TICKS) {
                finish(false);
            }
            return;
        }
        age++;
        scan();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            end(BossEnd.PEACEFUL);
            return;
        }
        if (age >= BossConfig.MAX_FIGHT_MINUTES.get() * 1200L) {
            end(BossEnd.TIME_OUT);
            return;
        }
        if (checkWipe() || checkAbandon()) {
            return;
        }
        arena.tick(this, age);
        if (age % BAR_INTERVAL == 0) {
            bars.refresh(level, arena.center(), fighters);
        }
        onTick();
    }

    /**
     * Ends the fight: the qualifying fighters are rewarded if it is a victory, the ritual item is given back if the
     * ending was not the players' doing, everyone is told, and the lane's ending begins. Does nothing if it already
     * ended.
     */
    public final void end(BossEnd why) {
        if (end != null || finished) {
            return;
        }
        end = why;
        Component boss = kind().displayName();
        if (why.rewards()) {
            List<Participant> rewarded = BossRewards.reward(this);
            if (BossConfig.ANNOUNCE_FIGHTS.get()) {
                // (Whoever fought, if nobody earned a reward: a boss is not defeated in silence.)
                server().getPlayerList().broadcastSystemMessage(Component.translatable("message.enhanced.boss.defeated",
                        names(rewarded.isEmpty() ? List.copyOf(participants.values()) : rewarded), boss).withStyle(ChatFormatting.GOLD), false);
            }
        } else {
            // Told to the participants too, wherever they are: after a wipe they are all on the death screen, and the
            // dead no longer see the bars.
            Component message = Component.translatable(why.messageKey(), boss).withStyle(ChatFormatting.GRAY);
            Set<ServerPlayer> told = new LinkedHashSet<>(audience());
            for (Participant participant : participants.values()) {
                ServerPlayer player = server().getPlayerList().getPlayer(participant.id());
                if (player != null) {
                    told.add(player);
                }
            }
            told.forEach(player -> player.sendSystemMessage(message));
        }
        if (why.refunds() && !refund.isEmpty() && starter != null) {
            BossRewards.hand(server(), starter, refund, why != BossEnd.SERVER_STOPPING);
            refund = ItemStack.EMPTY;
        }
        long rest = restTicks(why);
        if (rest > 0) {
            BossFights.rest(server(), kind(), level.getGameTime() + rest);
        }
        try {
            onEnd(why);
        } catch (RuntimeException e) {
            Enhanced.LOGGER.error("The {} fight failed to end as {}", kind().id(), why, e);
        }
    }

    /**
     * Lets the fight go: the lane puts everything back, the fight's entities are discarded, the tickets and the bars
     * go, and the fight leaves the registry. Does nothing if it already finished.
     */
    final void finish(boolean forced) {
        if (finished) {
            return;
        }
        finished = true;
        try {
            onFinished(forced);
        } catch (RuntimeException e) {
            Enhanced.LOGGER.error("The {} fight failed to finish", kind().id(), e);
        }
        for (Entity entity : entities()) {
            entity.discard();
        }
        for (ItemEntity item : keptDrops) {
            if (!item.isRemoved()) {
                item.setExtendedLifetime();
            }
        }
        keptDrops.clear();
        for (ChunkPos chunk : ticketed) {
            level.getChunkSource().removeRegionTicket(kind().ticket(), chunk, arena.ticketDistance(), id, true);
        }
        ticketed = List.of();
        bars.close();
        BossFights.finished(this);
    }

    /** The drops of a participant who died in the arena stay until the fight is over. */
    final void keepDrop(ItemEntity item) {
        item.setUnlimitedLifetime();
        keptDrops.add(item);
    }

    /**
     * Whether {@code item} is the drop of a participant who died in the arena, which the fight keeps for them until it is
     * over (see {@link BossFights#isKeptDrop}).
     */
    public final boolean keepsDrop(ItemEntity item) {
        return keptDrops.contains(item);
    }

    // ---- joining, presence, wipe, abandon ----

    private void refreshFighters() {
        List<ServerPlayer> online = new ArrayList<>();
        for (Participant participant : participants.values()) {
            ServerPlayer player = server().getPlayerList().getPlayer(participant.id);
            if (player != null) {
                participant.seen(player);
                if (player.level() == level && player.isAlive() && !player.isSpectator()) {
                    online.add(player);
                }
            }
        }
        fighters = online;
    }

    /** Joins whoever came within reach, and counts the time of whoever is in the arena. */
    private void scan() {
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && !participants.containsKey(player.getUUID())
                    && arena.inJoinRange(player.position())) {
                join(player);
            }
        }
        refreshFighters();
        for (ServerPlayer player : present()) {
            participants.get(player.getUUID()).present++;
        }
    }

    private void join(ServerPlayer player) {
        if (participants.containsKey(player.getUUID())) {
            return;
        }
        Participant participant = new Participant(player.getUUID(), player.getGameProfile().getName());
        participant.seen(player);
        participants.put(player.getUUID(), participant);
        onJoin(player);
    }

    /**
     * Every fighter who can die is dead or gone (logged out, or in another dimension) for {@code wipeSeconds}: a wipe.
     * (Creative players cannot die.) A fighter who is alive but has walked away is not fallen: if nobody comes back, the
     * fight is abandoned ({@link #checkAbandon}).
     */
    private boolean checkWipe() {
        boolean mortals = false;
        for (Participant participant : participants.values()) {
            mortals |= !participant.creative;
        }
        boolean standing = false;
        for (ServerPlayer player : fighters) {
            standing |= !player.isCreative();
        }
        if (!mortals || standing) {
            wipeTicks = 0;
            return false;
        }
        if (++wipeTicks >= BossConfig.WIPE_SECONDS.get() * 20) {
            end(BossEnd.WIPE);
            return true;
        }
        return false;
    }

    /** Nobody within reach of the centre, alive and online, for {@code abandonSeconds}: abandoned. */
    private boolean checkAbandon() {
        double reach = arena.abandonRadius() * arena.abandonRadius();
        for (ServerPlayer player : fighters) {
            Vec3 pos = player.position();
            double dx = pos.x - arena.center().x;
            double dz = pos.z - arena.center().z;
            if (dx * dx + dz * dz <= reach) {
                abandonTicks = 0;
                return false;
            }
        }
        if (++abandonTicks >= BossConfig.ABANDON_SECONDS.get() * 20) {
            end(BossEnd.ABANDON);
            return true;
        }
        return false;
    }

    /** Every chunk within the arena's ticket radius of its centre. */
    private List<ChunkPos> ticketChunks() {
        List<ChunkPos> chunks = new ArrayList<>();
        int reach = arena.ticketRadius();
        BlockPos center = BlockPos.containing(arena.center());
        int minX = (center.getX() - reach) >> 4;
        int maxX = (center.getX() + reach) >> 4;
        int minZ = (center.getZ() - reach) >> 4;
        int maxZ = (center.getZ() + reach) >> 4;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double nearestX = Math.max(Math.max(x * 16 - center.getX(), center.getX() - (x * 16 + 15)), 0);
                double nearestZ = Math.max(Math.max(z * 16 - center.getZ(), center.getZ() - (z * 16 + 15)), 0);
                if (nearestX * nearestX + nearestZ * nearestZ <= (double) reach * reach) {
                    chunks.add(new ChunkPos(x, z));
                }
            }
        }
        return chunks;
    }

    private static Component names(List<Participant> participants) {
        MutableComponent names = Component.empty();
        for (int i = 0; i < participants.size(); i++) {
            if (i > 0) {
                names.append(i == participants.size() - 1 ? " & " : ", ");
            }
            names.append(participants.get(i).name);
        }
        return names;
    }

    /** One line about the fight for {@code /enhanced boss list}. */
    public final String describe() {
        Vec3 center = arena.center();
        return kind().id() + " at " + (int) center.x + " " + (int) center.y + " " + (int) center.z + " in "
                + level.dimension().location() + ": " + participants.size() + " fighters, " + age / 20 + " s, " + status();
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * One person in a fight: who they are, how long they were there and what they did. Kept for the whole fight, even
     * when the player dies or logs out.
     */
    public static final class Participant {
        private final UUID id;
        private final String name;
        private final Map<String, Float> counters = new LinkedHashMap<>();
        private int present;
        private boolean lastBlow;
        private boolean creative;

        private Participant(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        private void seen(ServerPlayer player) {
            this.creative = player.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
        }

        public UUID id() {
            return id;
        }

        public String name() {
            return name;
        }

        /** The ledger counter of that name (0 if nothing was ever credited). */
        public float counter(String counter) {
            return counters.getOrDefault(counter, 0.0F);
        }

        /** The damage they did to the boss and its minions. */
        public float damage() {
            return counter(DAMAGE);
        }

        /** The ticks they were in the arena, alive. */
        public int ticksPresent() {
            return present;
        }

        public int secondsPresent() {
            return present / 20;
        }

        /** Whether they struck the last blow. */
        public boolean struckLastBlow() {
            return lastBlow;
        }

        /**
         * Whether they were in Creative mode when last seen: they never wipe a fight, and are rewarded only by config.
         */
        public boolean wasCreative() {
            return creative;
        }
    }
}
