package com.kolodkingal.enhanced.spellbook;

import com.kolodkingal.enhanced.custom.CustomHero;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * What a player keeps of the spellbooks (saved, kept on death, never synced: the clients read everything they draw from
 * the caster's synced definition, and her level from {@link WitchLevelPayload}). {@code stash} is the hero of his own he
 * had made with the Hero Creator before a book made him its caster ({@link CustomHero#EMPTY} if none): the book's
 * definition takes its place while he is the caster, and he gets it back when he gives the book's magic up.
 * {@code hatColor} is the colour he chose for the Ethereal witch's hat (it is kept while the hat is off).
 *
 * <p>3.18.1, the witches' levels ({@link WitchLevels}): {@code xp} is the experience of each source of magic, by key (a
 * book's id, or {@link WitchLevels#GIFT_KEY} for the magic the Demiurge gave): each has its own progress, and a witch who
 * comes back to a book comes back to her level. A source with no entry was never begun (a caster from before the levels
 * has none: she starts at level 5). {@code gift} is the full definition the Demiurge gave ({@link CustomHero#EMPTY} if
 * none): its five levels open its rows in their key order. {@code capStart}/{@code capUsed} are the hour of the hourly
 * cap (game time) and what it has given; {@code practiceDay}/{@code practiceUsed} the day of the book practice and what it
 * has given; {@code studyDay} the last day of study at a book's home lectern (+1: 0 is never).
 */
public record SpellbookData(CustomHero stash, int hatColor, Map<String, Integer> xp, CustomHero gift, long capStart, int capUsed,
                            long practiceDay, int practiceUsed, long studyDay) {
    /** The hat of a witch who has never chosen: the dark violet of Agatha's own. */
    public static final int DEFAULT_HAT = 0x3A2F48;
    public static final SpellbookData EMPTY = new SpellbookData(CustomHero.EMPTY, DEFAULT_HAT, Map.of(), CustomHero.EMPTY, 0L, 0,
            0L, 0, 0L);

    public static final Codec<SpellbookData> CODEC = RecordCodecBuilder.create(i -> i.group(
            CustomHero.CODEC.optionalFieldOf("stash", CustomHero.EMPTY).forGetter(SpellbookData::stash),
            Codec.INT.optionalFieldOf("hat_color", DEFAULT_HAT).forGetter(SpellbookData::hatColor),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("xp", Map.of()).forGetter(SpellbookData::xp),
            CustomHero.CODEC.optionalFieldOf("gift", CustomHero.EMPTY).forGetter(SpellbookData::gift),
            Codec.LONG.optionalFieldOf("cap_start", 0L).forGetter(SpellbookData::capStart),
            Codec.INT.optionalFieldOf("cap_used", 0).forGetter(SpellbookData::capUsed),
            Codec.LONG.optionalFieldOf("practice_day", 0L).forGetter(SpellbookData::practiceDay),
            Codec.INT.optionalFieldOf("practice_used", 0).forGetter(SpellbookData::practiceUsed),
            Codec.LONG.optionalFieldOf("study_day", 0L).forGetter(SpellbookData::studyDay)
    ).apply(i, SpellbookData::new));

    public SpellbookData {
        stash = stash == null ? CustomHero.EMPTY : stash;
        xp = xp == null ? Map.of() : Map.copyOf(xp);
        gift = gift == null ? CustomHero.EMPTY : gift;
    }

    /** A record as it was before the levels (the tests' old saves). */
    public SpellbookData(CustomHero stash, int hatColor) {
        this(stash, hatColor, Map.of(), CustomHero.EMPTY, 0L, 0, 0L, 0, 0L);
    }

    public boolean isEmpty() {
        return stash.isEmpty() && hatColor == DEFAULT_HAT && xp.isEmpty() && gift.isEmpty() && capUsed == 0 && practiceUsed == 0
                && studyDay == 0L;
    }

    public SpellbookData withStash(CustomHero stash) {
        return new SpellbookData(stash, hatColor, xp, gift, capStart, capUsed, practiceDay, practiceUsed, studyDay);
    }

    public SpellbookData withHatColor(int color) {
        return new SpellbookData(stash, color & 0xFFFFFF, xp, gift, capStart, capUsed, practiceDay, practiceUsed, studyDay);
    }

    /** Whether this source of magic ({@code key}) was ever begun. */
    public boolean begun(String key) {
        return xp.containsKey(key);
    }

    /** The experience of a source of magic (0 if never begun). */
    public int xp(String key) {
        return xp.getOrDefault(key, 0);
    }

    public SpellbookData withXp(String key, int points) {
        Map<String, Integer> m = new HashMap<>(xp);
        m.put(key, Math.max(0, points));
        return new SpellbookData(stash, hatColor, m, gift, capStart, capUsed, practiceDay, practiceUsed, studyDay);
    }

    public SpellbookData withGift(CustomHero gift) {
        return new SpellbookData(stash, hatColor, xp, gift, capStart, capUsed, practiceDay, practiceUsed, studyDay);
    }

    public SpellbookData withCap(long start, int used) {
        return new SpellbookData(stash, hatColor, xp, gift, start, used, practiceDay, practiceUsed, studyDay);
    }

    public SpellbookData withPractice(long day, int used) {
        return new SpellbookData(stash, hatColor, xp, gift, capStart, capUsed, day, used, studyDay);
    }

    public SpellbookData withStudyDay(long day) {
        return new SpellbookData(stash, hatColor, xp, gift, capStart, capUsed, practiceDay, practiceUsed, day);
    }
}
