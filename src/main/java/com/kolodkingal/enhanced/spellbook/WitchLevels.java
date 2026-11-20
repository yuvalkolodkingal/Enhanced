package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.WitchLevelsConfig;
import com.kolodkingal.enhanced.cottage.CottageKeeper;
import com.kolodkingal.enhanced.custom.CustomHero;
import com.kolodkingal.enhanced.custom.CustomHeroes;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModSounds;
import com.kolodkingal.enhanced.world.SiteAmbience;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 3.18.1, the witches' levels (the server side): a witch starts weak and grows by her own experience, five levels for each
 * source of magic, each level opening one more spell on a fixed key and raising her strengths ({@link SpellLevels}); level 5
 * is all of it, and never more than the real hero (every hero's ability at most at its strength 5). A witch is either a
 * spellbook's caster ({@link Kind#BOOK}) or a player the Demiurge gave magic to ({@link Kind#GIFT}, {@link #startGift}):
 * both have the same experience, levels, HUD line, Book of Spells screen and cantrips.
 *
 * <p>Experience (each source its own, kept on death, {@link SpellbookData#xp}): 2 for a spell that hits a hostile
 * creature or a player (each spell at most once in 2 seconds), 5 for a kill by a spell (15 for a creature of 40 health or
 * more), 400 to every witch who hurt a boss that dies (Thanos, Ultron, Apocalypse, Hela, the Ender Dragon, the Wither, the
 * Warden), the book's practice (the Ethereal witch's hex mark: 5; the Eldritch portal journey of over 100 blocks: 3; the
 * Green growth: 1 for each 5 plants; up to 30 a day), study at the book's home lectern once a day (60), all times
 * {@code xpScale} and a quarter more ({@code covenBonus}) while another witch is within 16 blocks. Hits and kills give at
 * most {@code hourlyCap} an hour. A caster from before the levels starts at level 5: nothing changes for her.
 */
public final class WitchLevels {
    /** The key of the Demiurge's gift in {@link SpellbookData#xp}. */
    public static final String GIFT_KEY = "gift";
    /** The experience a spell that hits gives, at most once in {@link #CAST_GAP} ticks for each spell. */
    public static final int CAST_XP = 2;
    public static final int CAST_GAP = 40;
    public static final int KILL_XP = 5;
    public static final int STRONG_KILL_XP = 15;
    /** A creature this healthy (at most) is a strong one. */
    public static final float STRONG_HEALTH = 40.0F;
    public static final int BOSS_XP = 400;
    public static final int STUDY_XP = 60;
    public static final int PRACTICE_PER_DAY = 30;
    public static final int HEX_MARK_XP = 5;
    public static final int PORTAL_XP = 3;
    /** A portal journey this long (blocks), or to another world, is practice. */
    public static final double PORTAL_DISTANCE = 100.0;
    public static final double COVEN_RADIUS = 16.0;
    /** The hour of the hourly cap (ticks). */
    public static final long HOUR = 72000L;
    public static final long DAY = 24000L;

    /** Where a witch's magic comes from. */
    public enum Kind {
        BOOK, GIFT
    }

    /** A witch's source of magic: a book, or the Demiurge's gift. */
    public record Source(Kind kind, @Nullable Spellbook book) {
        /** The key of its experience in {@link SpellbookData#xp}. */
        public String key() {
            return book != null ? book.id : GIFT_KEY;
        }

        /** The number the client knows it by: 1-3 a book ({@code ordinal + 1}), 4 the gift. */
        public int wire() {
            return book != null ? book.ordinal() + 1 : 4;
        }
    }

    /** The spells each witch last earned experience for, and when. */
    private static final Map<UUID, Map<String, Long>> CASTS = new HashMap<>();
    /** The witches who hurt each boss alive. */
    private static final Map<UUID, Set<UUID>> FOUGHT = new HashMap<>();
    /** The five definitions of each gift seen (without the cantrips [0], with them [1]). */
    private static final Map<CustomHero, CustomHero[][]> GIFTS = new LinkedHashMap<>();

    private WitchLevels() {
    }

    // ---- the settings ----

    public static boolean enabled() {
        return !EnhancedConfig.SPEC.isLoaded() || WitchLevelsConfig.ENABLED.get();
    }

    public static int maxLevel() {
        return EnhancedConfig.SPEC.isLoaded() ? WitchLevelsConfig.MAX_LEVEL.get() : SpellLevels.MAX;
    }

    private static double xpScale() {
        return EnhancedConfig.SPEC.isLoaded() ? WitchLevelsConfig.XP_SCALE.get() : 1.0;
    }

    private static int hourlyCap() {
        return EnhancedConfig.SPEC.isLoaded() ? WitchLevelsConfig.HOURLY_CAP.get() : 600;
    }

    private static double covenBonus() {
        return EnhancedConfig.SPEC.isLoaded() ? WitchLevelsConfig.COVEN_BONUS.get() : 0.25;
    }

    /** Whether a witch's empty keys get the cantrips ({@code defaults.enabled}). */
    public static boolean cantripsOn() {
        return !EnhancedConfig.SPEC.isLoaded() || WitchLevelsConfig.DEFAULTS_ENABLED.get();
    }

    /** Whether a hero of the Hero Creator gets the cantrips on his empty keys ({@code defaults.customHeroes}). */
    public static boolean cantripsForCustomHeroes() {
        return cantripsOn() && EnhancedConfig.SPEC.isLoaded() && WitchLevelsConfig.DEFAULTS_CUSTOM_HEROES.get();
    }

    // ---- who is a witch, and at what level ----

    /** The witch's source of magic, or null if this player is no witch. */
    @Nullable
    public static Source source(ServerPlayer player) {
        if (Powers.power(player) != Power.CUSTOM) {
            return null;
        }
        CustomHero hero = ModAttachments.CUSTOM_HERO.get(player);
        Spellbook book = Spellbook.of(hero);
        if (book != null) {
            return new Source(Kind.BOOK, book);
        }
        CustomHero gift = ModAttachments.SPELLBOOK.get(player).gift();
        return !gift.isEmpty() && giftLevelOf(gift, hero) > 0 ? new Source(Kind.GIFT, null) : null;
    }

    /** The experience of a source (a source never begun by a witch who has it is one from before the levels: level 5's). */
    public static int xp(SpellbookData data, String key) {
        return data.begun(key) ? data.xp(key) : SpellLevels.threshold(SpellLevels.MAX);
    }

    /** The level of a source: 5 when the levels are off, else what its experience reaches, at most {@code maxLevel}. */
    public static int level(SpellbookData data, String key) {
        return enabled() ? SpellLevels.levelFor(xp(data, key), maxLevel()) : SpellLevels.MAX;
    }

    /** The witch's level now (0 if she is no witch). */
    public static int level(ServerPlayer player) {
        Source s = source(player);
        return s == null ? 0 : level(ModAttachments.SPELLBOOK.get(player), s.key());
    }

    /** The witch's experience now (0 if she is no witch). */
    public static int xp(ServerPlayer player) {
        Source s = source(player);
        return s == null ? 0 : xp(ModAttachments.SPELLBOOK.get(player), s.key());
    }

    // ---- the gift ----

    private static synchronized CustomHero[][] gift(CustomHero gift) {
        CustomHero[][] defs = GIFTS.get(gift);
        if (defs == null) {
            defs = new CustomHero[2][SpellLevels.MAX + 1];
            for (int level = 1; level <= SpellLevels.MAX; level++) {
                CustomHero plain = SpellLevels.giftLevel(gift, level);
                defs[0][level] = plain;
                defs[1][level] = SpellLevels.fill(plain, level);
            }
            if (GIFTS.size() >= 64) {
                GIFTS.clear();
            }
            GIFTS.put(gift, defs);
        }
        return defs;
    }

    /** The definition of a gift at a level: its rows that level opens, at its strengths, and the cantrips if the server gives them. */
    public static CustomHero giftDefinition(CustomHero gift, int level) {
        return gift(gift)[cantripsOn() ? 1 : 0][Math.max(1, Math.min(SpellLevels.MAX, level))];
    }

    /** The level of a gift this definition is (with or without the cantrips), or 0 if it is none of them. */
    public static int giftLevelOf(CustomHero gift, CustomHero hero) {
        CustomHero[][] defs = gift(gift);
        for (int level = SpellLevels.MAX; level >= 1; level--) {
            for (int f = 0; f < 2; f++) {
                CustomHero d = defs[f][level];
                if (hero.name().equals(d.name()) && hero.powers().equals(d.powers()) && hero.keys().equals(d.keys())
                        && hero.sneakKeys().equals(d.sneakKeys())) {
                    return level;
                }
            }
        }
        return 0;
    }

    /**
     * The entry point of the Demiurge's gift of magic: {@code target} becomes a witch of {@code gift}, the custom hero the
     * Demiurge made of the magic rows he chose (the Hero Creator's rows of the Scarlet Witch, Billy, Agatha, Doctor Strange,
     * Loki...), and grows into it by the witches' levels like a spellbook's caster: at level 1 she has the rows of the
     * gift's first key (and its powers that need no key), each level opens the rows of the next key in their key order, and
     * level 5 is the full gift; every power is at the level's share of its strength (40% at level 1, all of it at level 5)
     * and no ability of a real hero is ever above its strength 5. Her empty keys get the cantrips, and she has the same
     * experience, HUD line and Book of Spells screen (using any spellbook opens it). A hero of her own making (the Hero
     * Creator's) is kept aside and given back if the gift is ever taken; the gift's experience starts again at 0.
     *
     * <p>Call it on the server with the gift already judged ({@code CustomHeroRules.sanitize}); it replaces whatever power
     * the target had (the caller decides who may receive it). With the levels off ({@code spellbook_levels.enabled}) she
     * has the whole gift at once.
     *
     * @param target the player who receives the magic
     * @param gift   the full definition given (its name, colours, look, picks and keys); level 5 is this
     */
    public static void startGift(ServerPlayer target, CustomHero gift) {
        SpellbookData data = ModAttachments.SPELLBOOK.get(target);
        CustomHero own = ModAttachments.CUSTOM_HERO.get(target);
        boolean witchDefinition = Spellbook.of(own) != null || !data.gift().isEmpty() && giftLevelOf(data.gift(), own) > 0;
        if (!witchDefinition) {
            data = data.withStash(own);
        }
        data = data.withGift(gift).withXp(GIFT_KEY, 0);
        ModAttachments.SPELLBOOK.set(target, data);
        if (Powers.power(target) != Power.NONE && Powers.power(target) != Power.CUSTOM) {
            Powers.setPower(target, Power.NONE);
        }
        CustomHeroes.become(target, giftDefinition(gift, level(data, GIFT_KEY)), true);
        sync(target);
    }

    // ---- the definition of the level ----

    /** The definition a witch should have at a level of her source (the Ethereal witch's hat as she wears it now). */
    public static CustomHero definition(ServerPlayer player, Source source, int level) {
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        if (source.book() != null) {
            boolean hat = source.book() != Spellbook.ETHEREAL || Spellbook.of(player) != Spellbook.ETHEREAL || Spellbooks.wearsHat(player);
            return source.book().definition(level, data.hatColor(), hat);
        }
        return giftDefinition(data.gift(), level);
    }

    /**
     * The witch's definition made right for her level now (a level gained or lost, a setting changed, a caster from before
     * the levels): she becomes it if it differs (her powers end and begin again), and her client is told her level.
     */
    public static void refresh(ServerPlayer player) {
        Source s = source(player);
        if (s == null) {
            sync(player);
            return;
        }
        CustomHero want = definition(player, s, level(ModAttachments.SPELLBOOK.get(player), s.key()));
        if (!want.equals(ModAttachments.CUSTOM_HERO.get(player))) {
            CustomHeroes.become(player, want, false);
        }
        sync(player);
    }

    /** A witch logs in: a caster from before the levels is written down at level 5, her definition made right, her client told. */
    public static void onLogin(ServerPlayer player) {
        Source s = source(player);
        if (s != null) {
            SpellbookData data = ModAttachments.SPELLBOOK.get(player);
            if (!data.begun(s.key())) {
                ModAttachments.SPELLBOOK.set(player, data.withXp(s.key(), SpellLevels.threshold(SpellLevels.MAX)));
            }
        }
        refresh(player);
    }

    /** A source is begun (a book used for the first time): at 0, level 1. */
    static SpellbookData begin(SpellbookData data, String key) {
        return data.begun(key) ? data : data.withXp(key, 0);
    }

    // ---- the operators' commands ----

    /** Sets the experience of a source to what a level needs, and makes the witch's definition right. */
    public static void setLevel(ServerPlayer player, String key, int level) {
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        ModAttachments.SPELLBOOK.set(player, data.withXp(key, SpellLevels.threshold(level)));
        refresh(player);
    }

    /** Sets (or adds to) the experience of the witch's source now; false if she is no witch. */
    public static boolean setXp(ServerPlayer player, int points, boolean add) {
        Source s = source(player);
        if (s == null) {
            return false;
        }
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        int before = level(data, s.key());
        int now = Math.min(SpellLevels.threshold(SpellLevels.MAX), Math.max(0, add ? xp(data, s.key()) + points : points));
        ModAttachments.SPELLBOOK.set(player, data.withXp(s.key(), now));
        int after = level(ModAttachments.SPELLBOOK.get(player), s.key());
        refresh(player);
        if (after > before) {
            announce(player, s, after);
        }
        return true;
    }

    // ---- experience ----

    /**
     * Gives the witch experience for her source now: times {@code xpScale}, a quarter more near another witch, within the
     * hourly cap if {@code capped}, never past level 5's. A level reached opens its spell (and says so).
     *
     * @return the points given
     */
    public static int gain(ServerPlayer player, int points, boolean capped) {
        Source s = source(player);
        if (s == null || !enabled() || points <= 0) {
            return 0;
        }
        double amount = points * xpScale();
        if (coven(player)) {
            amount *= 1.0 + covenBonus();
        }
        int pts = (int) Math.round(amount);
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        if (capped) {
            long now = player.server.overworld().getGameTime();
            if (now < data.capStart() || now - data.capStart() >= HOUR) {
                data = data.withCap(now, 0);
            }
            pts = Math.min(pts, Math.max(0, hourlyCap() - data.capUsed()));
            data = data.withCap(data.capStart(), data.capUsed() + pts);
        }
        int max = SpellLevels.threshold(SpellLevels.MAX);
        int before = level(data, s.key());
        int had = xp(data, s.key());
        int now = Math.min(max, had + Math.max(0, pts));
        data = data.withXp(s.key(), now);
        ModAttachments.SPELLBOOK.set(player, data);
        int after = level(data, s.key());
        if (after > before) {
            refresh(player);
            announce(player, s, after);
        } else if (now != had) {
            sync(player);
        }
        return now - had;
    }

    /** Whether another witch is within {@link #COVEN_RADIUS}. */
    private static boolean coven(ServerPlayer player) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other != player && other.isAlive() && other.distanceTo(player) <= COVEN_RADIUS && source(other) != null) {
                return true;
            }
        }
        return false;
    }

    private static long day(ServerPlayer player) {
        return player.server.overworld().getDayTime() / DAY;
    }

    /** The book's practice: up to {@link #PRACTICE_PER_DAY} points a day. @return the points given */
    public static int practice(ServerPlayer player, int points) {
        if (points <= 0 || source(player) == null) {
            return 0;
        }
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        long day = day(player) + 1;
        if (data.practiceDay() != day) {
            data = data.withPractice(day, 0);
        }
        int pts = Math.min(points, Math.max(0, PRACTICE_PER_DAY - data.practiceUsed()));
        if (pts <= 0) {
            return 0;
        }
        ModAttachments.SPELLBOOK.set(player, data.withPractice(day, data.practiceUsed() + pts));
        return gain(player, pts, false);
    }

    /** The structure that is a book's home (its lectern's), or null. */
    @Nullable
    public static ResourceKey<Structure> home(Spellbook book) {
        return book == Spellbook.ELDRITCH ? SiteAmbience.SANCTUM : CottageKeeper.COTTAGE;
    }

    /** Whether {@code pos} is inside the structure of a book's home (Agatha's cottage, the Sanctum Sanctorum). */
    public static boolean atHome(ServerLevel level, BlockPos pos, Spellbook book) {
        ResourceKey<Structure> key = home(book);
        Structure structure = key == null ? null : level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(key);
        return structure != null && level.structureManager().getStructureWithPieceAt(pos, structure).isValid();
    }

    /**
     * The daily study at the book's home lectern: once a day, {@link #STUDY_XP}. Says how it went.
     *
     * @return whether she studied
     */
    public static boolean study(ServerPlayer player) {
        Source s = source(player);
        if (s == null || s.book() == null) {
            return false;
        }
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        long day = day(player) + 1;
        if (data.studyDay() == day) {
            player.displayClientMessage(Component.translatable("message.enhanced.witch_levels.studied")
                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return false;
        }
        ModAttachments.SPELLBOOK.set(player, data.withStudyDay(day));
        int given = gain(player, STUDY_XP, false);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS,
                1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("message.enhanced.witch_levels.study", given)
                .withStyle(st -> st.withColor(s.book().glow)), true);
        return true;
    }

    /** Whether a creature is a boss whose fall gives every witch who fought it {@link #BOSS_XP}. */
    public static boolean isBoss(LivingEntity e) {
        return e instanceof com.kolodkingal.enhanced.thanos.ThanosEntity || e instanceof com.kolodkingal.enhanced.ultron.UltronEntity
                || e instanceof com.kolodkingal.enhanced.apocalypse.ApocalypseEntity || e instanceof com.kolodkingal.enhanced.hela.HelaEntity
                || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || e instanceof net.minecraft.world.entity.monster.warden.Warden;
    }

    /** Whether the damage is a punch (no spell). */
    private static boolean melee(DamageSource source, ServerPlayer player) {
        return source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == player;
    }

    /** A creature was hurt ({@code CommonEvents}): a witch's spell that hits earns her experience; bosses remember who fought them. */
    public static void onDamage(LivingEntity target, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer player) || target == player || !target.isAlive() && !isBoss(target)) {
            return;
        }
        if (isBoss(target)) {
            FOUGHT.computeIfAbsent(target.getUUID(), u -> new HashSet<>()).add(player.getUUID());
        }
        if (melee(source, player) || !(target instanceof Enemy || target instanceof Player) || source(player) == null) {
            return;
        }
        String spell = source.getMsgId();
        long now = player.serverLevel().getGameTime();
        Map<String, Long> casts = CASTS.computeIfAbsent(player.getUUID(), u -> new HashMap<>());
        Long last = casts.get(spell);
        if (last != null && now >= last && now - last < CAST_GAP) {
            return;
        }
        casts.put(spell, now);
        gain(player, CAST_XP, true);
    }

    /** A creature died ({@code CommonEvents}): a witch's kill by a spell, or a boss's fall, earns experience. */
    public static void onDeath(LivingEntity dead, DamageSource source) {
        if (dead.level().isClientSide()) {
            return;
        }
        Set<UUID> fought = FOUGHT.remove(dead.getUUID());
        if (isBoss(dead)) {
            if (fought != null && dead.getServer() != null) {
                for (UUID id : fought) {
                    ServerPlayer p = dead.getServer().getPlayerList().getPlayer(id);
                    if (p != null) {
                        gain(p, BOSS_XP, false);
                    }
                }
            }
            return;
        }
        if (source.getEntity() instanceof ServerPlayer player && player != dead && !melee(source, player)
                && (dead instanceof Enemy || dead instanceof Player)) {
            gain(player, dead.getMaxHealth() >= STRONG_HEALTH ? STRONG_KILL_XP : KILL_XP, true);
        }
    }

    /** Forgets a player who left. */
    public static void forget(ServerPlayer player) {
        CASTS.remove(player.getUUID());
    }

    /** The server stopped. */
    public static void clear() {
        CASTS.clear();
        FOUGHT.clear();
    }

    // ---- what is seen ----

    /** The name of a level of a source ("Rune Mistress"), in the player's language. */
    public static MutableComponent levelTitle(Source source, int level) {
        return Component.translatable(levelTitleKey(source.wire(), level));
    }

    /** The lang key of a level's name for the client's source number ({@link Source#wire}). */
    public static String levelTitleKey(int wire, int level) {
        String id = wire >= 1 && wire <= 3 ? Spellbook.values()[wire - 1].id : GIFT_KEY;
        return "spellbook.enhanced." + id + ".level." + Math.max(1, Math.min(SpellLevels.MAX, level));
    }

    /**
     * What each level of a source opens, as cells ("{@code column|sneak|cell}", column 0 for a power on no key), from the
     * definitions of its five levels without the cantrips: the client's screen and the level-up's subtitle name them.
     */
    public static List<List<String>> opens(ServerPlayer player, Source source) {
        List<CustomHero> defs = new ArrayList<>();
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        for (int level = 1; level <= SpellLevels.MAX; level++) {
            defs.add(source.book() != null ? source.book().base(level, false) : gift(data.gift())[0][level]);
        }
        List<List<String>> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CustomHero def : defs) {
            List<String> opened = new ArrayList<>();
            for (int c = 1; c <= CustomHero.KEYS; c++) {
                for (boolean sneak : new boolean[]{false, true}) {
                    String cell = SpellLevels.displayCell(def, def.binding(c, sneak));
                    if (!cell.isEmpty() && seen.add(c + "|" + sneak + "|" + cell)) {
                        opened.add(c + "|" + (sneak ? 1 : 0) + "|" + cell);
                    }
                }
            }
            for (CustomHero.Pick pick : def.powers()) {
                if (SpellLevels.onNoKey(def, pick.id()) && seen.add("0|" + pick.id())) {
                    opened.add("0|0|" + pick.id());
                }
            }
            out.add(opened);
        }
        return out;
    }

    /** The key each column first holds a spell of the source's own at (0: never). */
    public static int[] keyLevels(ServerPlayer player, Source source) {
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        return SpellLevels.keyLevels(level -> source.book() != null ? source.book().base(level, false) : gift(data.gift())[0][level]);
    }

    /** Tells the player's client her level, experience and what her levels open (no witch: source 0). */
    public static void sync(ServerPlayer player) {
        Net.sendToPlayer(player, payload(player));
    }

    /** The message {@link #sync} sends. */
    public static WitchLevelPayload payload(ServerPlayer player) {
        Source s = source(player);
        if (s == null) {
            return WitchLevelPayload.NONE;
        }
        SpellbookData data = ModAttachments.SPELLBOOK.get(player);
        List<String> opens = new ArrayList<>();
        for (List<String> level : opens(player, s)) {
            opens.add(String.join(";", level));
        }
        return new WitchLevelPayload(s.wire(), level(data, s.key()), xp(data, s.key()), enabled() ? maxLevel() : SpellLevels.MAX,
                keyLevels(player, s), opens);
    }

    /**
     * A level reached: its name as a title in the source's colour, the spell it opens as the subtitle, the awakening's
     * sound and a burst; at level 5, a word to the whole server.
     */
    public static void announce(ServerPlayer player, Source source, int level) {
        int color = source.book() != null ? source.book().color : ModAttachments.CUSTOM_HERO.get(player).color();
        int glow = source.book() != null ? source.book().glow : ModAttachments.CUSTOM_HERO.get(player).glow();
        Component title = Component.translatable("message.enhanced.witch_levels.level_title", level, levelTitle(source, level))
                .withStyle(st -> st.withColor(color).withBold(true));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        List<List<String>> opens = opens(player, source);
        List<String> opened = level >= 1 && level <= opens.size() ? opens.get(level - 1) : List.of();
        Component sub = null;
        for (String entry : opened) {
            String[] p = entry.split("\\|", 3);
            int column = Integer.parseInt(p[0]);
            if (column > 0) {
                Component key = "1".equals(p[1])
                        ? Component.translatable("hud.enhanced.custom.sneak", Component.keybind("key.enhanced.ability_" + column))
                        : Component.keybind("key.enhanced.ability_" + column);
                sub = Component.translatable("message.enhanced.witch_levels.new_spell", SpellLevels.label(p[2]), key);
                break;
            }
        }
        if (sub == null) {
            sub = Component.translatable("message.enhanced.witch_levels.stronger");
        }
        Component subtitle = sub.copy().withStyle(st -> st.withColor(glow));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        ServerLevel level0 = player.serverLevel();
        level0.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CUSTOM_AWAKEN.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        level0.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.9F);
        Powers.burst(level0, player.getX(), player.getY() + 1.0, player.getZ(), color, 90, 1.4);
        Powers.burst(level0, player.getX(), player.getY() + 1.0, player.getZ(), glow, 50, 0.7);
        if (level >= SpellLevels.MAX) {
            player.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.enhanced.witch_levels.mastered",
                    player.getDisplayName(), levelTitle(source, level)).withStyle(st -> st.withColor(color)), false);
        }
    }
}
