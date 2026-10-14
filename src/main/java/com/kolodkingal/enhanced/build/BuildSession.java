package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.build.SpellMath.Volume;
import com.kolodkingal.enhanced.config.BuildConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A builder's live Build Magic state on the server (D-B 11.1): whether she is in build mode, the block she last picked
 * (with the middle button, or on the palette page), the token buckets that bound what her client may send (C6:
 * {@code maxRequestsPerSecond} for every build payload, 4 preflights and 20 dabs a second), the Paint stroke she is
 * painting ({@link Stroke}), her pocket on its way to her client, how many spells she has cast since she logged in (the
 * first five say how many blocks), the lines she is to be told at her next login ("While you were away, your spell
 * finished"), when "Mom is near" was last said to Billy, and what Billy's spells that ended while he was away owe his
 * meter and his practice (settled at his next login, {@link #owe}).
 *
 * <p>One per player, by UUID; it is not saved. It outlives a logout (so a spell that finishes while she is away can
 * leave her its line), and {@link #clear} forgets them all (the server's stop, the end of a test).
 */
public final class BuildSession {
    /** The most preflights a builder may ask for each second (D-B 10.1); more are dropped, not queued. */
    public static final int PREFLIGHTS_PER_SECOND = 4;
    /** How often (ticks) a builder's dropped requests are logged: once a minute (C6). */
    private static final int DROPS_LOGGED_EVERY = 1200;
    /** The spells after a login whose done line says how many blocks they changed; after them, just the pop. */
    public static final int SAID_SPELLS = 5;
    /** The most dabs of a stroke a builder may send each second (D-B 13.4); more are dropped, not queued. */
    public static final int DABS_PER_SECOND = 20;

    private static final Map<UUID, BuildSession> ALL = new HashMap<>();

    private final UUID player;
    private boolean mode;
    private @Nullable BlockState picked;
    private @Nullable Block pickedBlock;
    private int pickedSlot = -1;
    private @Nullable Item pickedWith;
    private double requests = Double.NaN;
    private long requestsAt;
    private double preflights = PREFLIGHTS_PER_SECOND;
    private long preflightsAt;
    private double dabs = DABS_PER_SECOND;
    private long dabsAt;
    private @Nullable Stroke stroke;
    private int lastStroke = Integer.MIN_VALUE;
    private int refusedStroke = Integer.MIN_VALUE;
    private Pocket.@Nullable Stream pocketStream;
    private int casts;
    private long droppedLoggedAt = Long.MIN_VALUE / 2;
    private long momSaidAt = Long.MIN_VALUE / 2;
    private final List<Component> atLogin = new ArrayList<>();
    private long owedMeter;
    private long owedPractice;

    private BuildSession(UUID player) {
        this.player = player;
    }

    /** The session of the player {@code player} (made the first time it is asked for). */
    public static BuildSession of(UUID player) {
        return ALL.computeIfAbsent(player, BuildSession::new);
    }

    /** The session of {@code player}, if there is one. */
    public static @Nullable BuildSession existing(UUID player) {
        return ALL.get(player);
    }

    /** Every session there is. */
    public static Collection<BuildSession> all() {
        return List.copyOf(ALL.values());
    }

    /** Forgets every session. */
    public static void clear() {
        ALL.clear();
    }

    public UUID player() {
        return player;
    }

    /** Whether the builder is in build mode. */
    public boolean mode() {
        return mode;
    }

    public void setMode(boolean mode) {
        this.mode = mode;
    }

    /** The block she last picked with the middle button (the server's own read of it), or null for the hand. */
    public @Nullable BlockState picked() {
        return picked;
    }

    /** The block she last chose on the palette page (placed as she would place it), or null. */
    public @Nullable Block pickedBlock() {
        return pickedBlock;
    }

    /**
     * She picked {@code state} (the middle button) or {@code block} (the palette) while holding {@code player}'s hotbar
     * slot and item: changing either goes back to the hand (D-B 5.2). Both null: back to the hand now.
     */
    public void setPicked(@Nullable BlockState state, @Nullable Block block, Player player) {
        this.picked = state;
        this.pickedBlock = state == null ? block : null;
        this.pickedSlot = player.getInventory().selected;
        this.pickedWith = player.getMainHandItem().getItem();
    }

    /** Back to the hand. */
    public void dropPicked() {
        this.picked = null;
        this.pickedBlock = null;
    }

    /**
     * Whether she still holds what she held when she picked (the same hotbar slot, the same item in it); if not, the
     * pick is forgotten (D-B 5.2: changing the hotbar slot or the held item goes back to the hand).
     */
    public boolean keepsPick(Player player) {
        if (picked == null && pickedBlock == null) {
            return false;
        }
        if (player.getInventory().selected != pickedSlot || player.getMainHandItem().getItem() != pickedWith) {
            dropPicked();
            return false;
        }
        return true;
    }

    /**
     * Whether a build payload that arrives at server tick {@code tick} is taken (C6): a bucket of
     * {@code build_magic.maxRequestsPerSecond} tokens that refills at that many a second.
     */
    public boolean takeRequest(long tick) {
        int perSecond = EnhancedConfig.SPEC.isLoaded() ? BuildConfig.MAX_REQUESTS_PER_SECOND.get() : 40;
        if (Double.isNaN(requests)) {
            requests = perSecond;
            requestsAt = tick;
        }
        requests = Math.min(perSecond, requests + Math.max(0L, tick - requestsAt) * perSecond / 20.0);
        requestsAt = tick;
        if (requests < 1.0) {
            return false;
        }
        requests -= 1.0;
        return true;
    }

    /** Whether a dropped request at server tick {@code tick} is to be logged: once a minute at most (C6). */
    public boolean logDropped(long tick) {
        if (tick - droppedLoggedAt < DROPS_LOGGED_EVERY) {
            return false;
        }
        droppedLoggedAt = tick;
        return true;
    }

    /** Whether a preflight that arrives at server tick {@code tick} is answered: at most 4 a second (D-B 10.1). */
    public boolean takePreflight(long tick) {
        preflights = Math.min(PREFLIGHTS_PER_SECOND, preflights + Math.max(0L, tick - preflightsAt) * PREFLIGHTS_PER_SECOND / 20.0);
        preflightsAt = tick;
        if (preflights < 1.0) {
            return false;
        }
        preflights -= 1.0;
        return true;
    }

    /** Whether a dab that arrives at server tick {@code tick} is taken: at most 20 a second (D-B 13.4). */
    public boolean takeDab(long tick) {
        dabs = Math.min(DABS_PER_SECOND, dabs + Math.max(0L, tick - dabsAt) * DABS_PER_SECOND / 20.0);
        dabsAt = tick;
        if (dabs < 1.0) {
            return false;
        }
        dabs -= 1.0;
        return true;
    }

    /** The stroke she is painting, or null. */
    public @Nullable Stroke stroke() {
        return stroke;
    }

    /** She begins painting {@code stroke}. */
    void open(Stroke stroke) {
        this.stroke = stroke;
        this.lastStroke = stroke.number;
    }

    /** The stroke is sealed: it is no longer hers to paint on. */
    void sealed(Stroke done) {
        if (stroke == done) {
            stroke = null;
        }
    }

    /** Whether {@code number} is a stroke she has painted already (a late dab of it is dropped). */
    boolean painted(int number) {
        return number == lastStroke && stroke == null;
    }

    /**
     * A dab of the stroke {@code number} was refused (too far, no block, not yet): true the first time, when she is
     * told why; its next dabs are refused without a word (a held button sends 20 a second).
     */
    boolean refusedDab(int number) {
        if (refusedStroke == number) {
            return false;
        }
        refusedStroke = number;
        return true;
    }

    /** Her pocket on its way to her client, or null. */
    public Pocket.@Nullable Stream pocketStream() {
        return pocketStream;
    }

    public void setPocketStream(Pocket.@Nullable Stream stream) {
        this.pocketStream = stream;
    }

    /** Counts a spell done; true for the first {@link #SAID_SPELLS} since the login (their line says how many blocks). */
    public boolean saysDone() {
        return ++casts <= SAID_SPELLS;
    }

    /** A new login: the first spells say their size again. */
    public void loggedIn() {
        casts = 0;
    }

    /** When "Mom is near" was last said (game time). */
    public long momSaidAt() {
        return momSaidAt;
    }

    public void setMomSaidAt(long gameTime) {
        this.momSaidAt = gameTime;
    }

    /** A line to tell the builder when she next logs in. */
    public void tellAtLogin(Component line) {
        atLogin.add(line);
    }

    /** The lines waiting for her login, oldest first; they are forgotten as they are given. */
    public List<Component> takeLoginLines() {
        List<Component> lines = List.copyOf(atLogin);
        atLogin.clear();
        return lines;
    }

    /**
     * Billy's spell ended (or was paid for) while he was away: at his next login his meter pays {@code meter} more
     * blocks of magic (less than 0: gets them back) and his practice grows by {@code practice} (less than 0: is taken
     * back).
     */
    public void owe(long meter, long practice) {
        owedMeter += meter;
        owedPractice += practice;
    }

    /**
     * What his meter owes since he left (less than 0: what it gets back) and the practice he learned (less than 0:
     * lost), or null when nothing is owed; forgotten as it is taken.
     */
    public long @Nullable [] takeOwed() {
        if (owedMeter == 0 && owedPractice == 0) {
            return null;
        }
        long[] owed = {owedMeter, owedPractice};
        owedMeter = 0;
        owedPractice = 0;
        return owed;
    }

    /**
     * A Paint stroke being painted (D-B 11.8): the right button's press sends its first dab, every tick the brush moves
     * another, and its release {@code STROKE_END}. Each dab is a small job of its own (the same rules, the same
     * budget), but they all write into this one step, so the whole stroke from press to release is one undo step; it is
     * sealed at {@code STROKE_END}, 40 ticks after its last dab, when another spell is cast, or at Ctrl + Z, once its
     * last dab is done. Its dabs share one rule book (a section a claim refused stays refused for the stroke), and a
     * stroke over a selected thing floods it once, at its first dab.
     */
    public static final class Stroke {
        /** The ticks after a stroke's last dab when it is sealed without its release (D-B 14.7). */
        public static final int CLOSES_AFTER = 40;
        /** The most dabs a stroke keeps waiting at once (a second of them); more are dropped. */
        public static final int MAX_WAITING = DABS_PER_SECOND;

        final UUID caster;
        final int number;
        final ServerLevel level;
        final Limits limits;
        final UndoStep step;
        final BuildRules.Cells rules;
        final boolean mom;
        final int momId;
        @Nullable Volume mask;
        /** What its sealing says instead of the done line (a dab ran out of material), or null. */
        @Nullable Component said;
        int fxId;
        int token;
        long lastDab;
        int pending;
        boolean closing;
        boolean sealed;
        int cost;
        long changed;

        Stroke(UUID caster, int number, ServerLevel level, Limits limits, UndoStep step, BuildRules.Cells rules, boolean mom, int momId) {
            this.caster = caster;
            this.number = number;
            this.level = level;
            this.limits = limits;
            this.step = step;
            this.rules = rules;
            this.mom = mom;
            this.momId = momId;
        }

        /** Its number, as the client counts its strokes. */
        public int number() {
            return number;
        }

        /** The step its dabs write. */
        public UndoStep step() {
            return step;
        }

        /** How many of its dabs wait or work. */
        public int pending() {
            return pending;
        }

        /** Whether it takes no more dabs and is sealed once the last one is done. */
        public boolean closing() {
            return closing;
        }
    }
}
