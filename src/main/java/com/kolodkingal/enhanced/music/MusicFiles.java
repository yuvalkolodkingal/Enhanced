package com.kolodkingal.enhanced.music;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * The pure parts of sending a server's music to its players (3.5): checking that a file is Ogg Vorbis, its SHA-256, and
 * cutting it into chunks and putting them back together. Shared by the server, the client and the tests.
 */
public final class MusicFiles {
    /** The most bytes of a song one message carries (under 30 KB, well inside a play packet). */
    public static final int CHUNK_BYTES = 30_000;
    /** The largest song a client accepts from any server, whatever that server's own limit (64 MiB). */
    public static final int CLIENT_MAX_BYTES = 64 * 1024 * 1024;
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    /**
     * Whether the bytes start like an Ogg Vorbis file: an Ogg page ("OggS", version 0) that begins a stream, whose first
     * packet is a Vorbis identification header (type 1, "vorbis", version 0, 1 or more channels, a sample rate). Only this
     * kind is sent to the players, since every client can play it with the game's own decoder.
     */
    public static boolean isOggVorbis(byte[] data) {
        if (data.length < 28 || data[0] != 'O' || data[1] != 'g' || data[2] != 'g' || data[3] != 'S' || data[4] != 0
                || (data[5] & 0x02) == 0) {
            return false;
        }
        int segments = data[26] & 0xFF;
        int packet = 27 + segments;
        if (segments == 0 || data.length < packet + 16) {
            return false;
        }
        if (data[packet] != 1 || !new String(data, packet + 1, 6, StandardCharsets.US_ASCII).equals("vorbis")) {
            return false;
        }
        int version = (data[packet + 7] & 0xFF) | (data[packet + 8] & 0xFF) << 8 | (data[packet + 9] & 0xFF) << 16 | (data[packet + 10] & 0xFF) << 24;
        int channels = data[packet + 11] & 0xFF;
        int rate = (data[packet + 12] & 0xFF) | (data[packet + 13] & 0xFF) << 8 | (data[packet + 14] & 0xFF) << 16 | (data[packet + 15] & 0xFF) << 24;
        return version == 0 && channels > 0 && rate > 0;
    }

    /** The SHA-256 of the bytes, as 64 lower-case hex digits. */
    public static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Whether a string is a hash as {@link #sha256} writes it (it also names a cached file, so nothing else may pass). */
    public static boolean isHash(String s) {
        return s != null && HASH.matcher(s).matches();
    }

    /** How many chunks a song of {@code size} bytes is sent in (at least one). */
    public static int chunkCount(int size) {
        return Math.max(1, (size + CHUNK_BYTES - 1) / CHUNK_BYTES);
    }

    /** Chunk {@code index} of a song. */
    public static byte[] chunk(byte[] data, int index) {
        int from = index * CHUNK_BYTES;
        if (index < 0 || from > data.length || index >= chunkCount(data.length)) {
            throw new IndexOutOfBoundsException(index);
        }
        return Arrays.copyOfRange(data, from, Math.min(data.length, from + CHUNK_BYTES));
    }

    /**
     * A song coming in chunk by chunk, in any order. It checks every chunk against what was announced (the size, the count,
     * each chunk's length) and, once whole, the hash: a song that does not match is thrown away.
     */
    public static final class Assembly {
        private final String hash;
        private final int size;
        private final byte[] data;
        private final boolean[] got;
        private int received;

        public Assembly(String hash, int size) {
            if (!isHash(hash) || size <= 0 || size > CLIENT_MAX_BYTES) {
                throw new IllegalArgumentException("Bad song " + hash + " of " + size + " bytes");
            }
            this.hash = hash;
            this.size = size;
            this.data = new byte[size];
            this.got = new boolean[chunkCount(size)];
        }

        /** Takes one chunk; false if it does not fit the song (wrong index, count or length), which is then ignored. */
        public boolean accept(int index, int total, byte[] bytes) {
            if (total != got.length || index < 0 || index >= got.length) {
                return false;
            }
            int from = index * CHUNK_BYTES;
            int expected = Math.min(CHUNK_BYTES, size - from);
            if (bytes.length != expected) {
                return false;
            }
            if (!got[index]) {
                got[index] = true;
                received++;
            }
            System.arraycopy(bytes, 0, data, from, bytes.length);
            return true;
        }

        public boolean complete() {
            return received == got.length;
        }

        /** The song, once whole and only if its hash is the one announced; null otherwise. */
        public byte[] result() {
            return complete() && sha256(data).equals(hash) ? data : null;
        }

        public String hash() {
            return hash;
        }

        public int size() {
            return size;
        }

        public int received() {
            return received;
        }
    }

    private MusicFiles() {
    }
}
