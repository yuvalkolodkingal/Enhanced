package com.kolodkingal.enhanced.music;

import com.kolodkingal.enhanced.road.EndingHall;
import com.kolodkingal.enhanced.road.RoadMusic;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The music a server or a player can replace (3.5), by name: Quicksilver's Walkman song ({@code quicksilver}), Speed's
 * ({@code speed}), the Witches' Road ({@code witches_road}, or one version of it: {@code witches_road_<stage>}, such as
 * {@code witches_road_parkour}), and {@code music}, which stands in for all of them. A file is called after the music it
 * replaces: {@code quicksilver.ogg}, {@code witches_road_ice_boat.ogg}, {@code music.ogg}.
 * <p>
 * Every name has a chain of fallbacks, the most particular first: {@code witches_road_parkour} → {@code witches_road} →
 * {@code music}; {@code speed} → {@code music}. The first one there is a file for is played. Everything here is pure (file
 * names in, a file name out), so the server, the client and the tests share it.
 */
public final class MusicTracks {
    public static final String QUICKSILVER = "quicksilver";
    public static final String SPEED = "speed";
    public static final String ROAD = "witches_road";
    /** The file that stands in for every track that has none of its own. */
    public static final String FALLBACK = "music";
    /** The Road's version for the end of the Road (its stage is {@link EndingHall#ID}, "road_end"). */
    public static final String ROAD_ENDING = "ending";

    /**
     * The name of a player's own copy of "Sweet Dreams (Are Made of This)" in his folder ({@code sweet_dreams.mp3},
     * .ogg or .wav). Since 3.8.4 the recording is no longer in the mod, and the client option quicksilverSong =
     * sweet_dreams plays this file for Quicksilver first (see {@link #resolvePersonal(String, Collection, boolean)}).
     */
    public static final String SWEET_DREAMS = "sweet_dreams";

    /** What a track's name may be: lower-case letters, digits and underscores. */
    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,64}");
    /** The kinds of file a player's own folder may hold (the server's takes Ogg Vorbis only). */
    public static final List<String> PERSONAL_TYPES = List.of("ogg", "mp3", "wav");

    /** The track of a stage of the Witches' Road ({@link RoadMusic}), or null for none (off the Road). */
    @Nullable
    public static String road(String stage) {
        if (stage == null || stage.isEmpty()) {
            return null;
        }
        return ROAD + "_" + (EndingHall.ID.equals(stage) ? ROAD_ENDING : stage);
    }

    /** Whether a name is one this mod plays: one of the tracks, a version of the Road, or the fallback. */
    public static boolean isTrackName(String name) {
        return name.equals(QUICKSILVER) || name.equals(SPEED) || name.equals(ROAD) || name.equals(FALLBACK)
                || name.startsWith(ROAD + "_") && name.length() > ROAD.length() + 1 && NAME.matcher(name).matches();
    }

    /** The names to look for, in order, for a track: itself, the whole Road for a version of it, then {@code music}. */
    public static List<String> chain(String track) {
        List<String> chain = new ArrayList<>();
        chain.add(track);
        if (track.startsWith(ROAD + "_")) {
            chain.add(ROAD);
        }
        if (!track.equals(FALLBACK)) {
            chain.add(FALLBACK);
        }
        return chain;
    }

    /** The name in {@code available} that plays for {@code track}, if any. */
    public static Optional<String> resolve(String track, Collection<String> available) {
        for (String name : chain(track)) {
            if (available.contains(name)) {
                return Optional.of(name);
            }
        }
        return Optional.empty();
    }

    /** A file's name without its extension, in lower case ("Quicksilver.OGG" is "quicksilver"). */
    public static String baseName(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        return dot <= 0 ? lower : lower.substring(0, dot);
    }

    /** A file's extension in lower case, or "" for none. */
    public static String extension(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        return dot <= 0 ? "" : lower.substring(dot + 1);
    }

    /**
     * The file in a player's own folder (config/enhanced/music) that plays for {@code track}, from the names of the files
     * there:
     * <ol>
     *     <li>a file named after the track or its fallbacks, apart from {@code music} ({@code witches_road_parkour.*}, then
     *     {@code witches_road.*});</li>
     *     <li>for Quicksilver only, as before 3.5: any other song there, the first in alphabetical order, that is not named
     *     after another track ({@code music.mp3} or {@code my song.ogg} are his; {@code speed.ogg} is not).</li>
     * </ol>
     * Files of other kinds (README.txt) are left alone.
     */
    public static Optional<String> resolvePersonal(String track, Collection<String> fileNames) {
        return resolvePersonal(track, fileNames, false);
    }

    /**
     * As {@link #resolvePersonal(String, Collection)}; with {@code sweetDreams} (the player chose quicksilverSong =
     * sweet_dreams, 3.8.4), Quicksilver's song is his own copy {@code sweet_dreams.*} first, then the usual one
     * ({@code quicksilver.*} or any other song). Empty when he has none: then Quicksilver's own song plays.
     */
    public static Optional<String> resolvePersonal(String track, Collection<String> fileNames, boolean sweetDreams) {
        List<String> songs = fileNames.stream()
                .filter(f -> PERSONAL_TYPES.contains(extension(f)))
                .sorted(Comparator.comparing((String f) -> f.toLowerCase(Locale.ROOT)))
                .toList();
        if (sweetDreams && track.equals(QUICKSILVER)) {
            for (String file : songs) {
                if (baseName(file).equals(SWEET_DREAMS)) {
                    return Optional.of(file);
                }
            }
        }
        for (String name : chain(track)) {
            if (name.equals(FALLBACK)) {
                continue;
            }
            for (String file : songs) {
                if (baseName(file).equals(name)) {
                    return Optional.of(file);
                }
            }
        }
        if (track.equals(QUICKSILVER)) {
            for (String file : songs) {
                String base = baseName(file);
                if (base.equals(FALLBACK) || !isTrackName(base)) {
                    return Optional.of(file);
                }
            }
        }
        return Optional.empty();
    }

    private MusicTracks() {
    }
}
