package com.kolodkingal.enhanced.custom;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Whether some bytes are a Minecraft skin the server may pass on to every client: a PNG of exactly 64x64 pixels, no
 * bigger than the limit, whose every chunk is whole (its CRC right), with an image header the game can draw (8 bits a
 * channel, grey, RGB, palette or with alpha, not interlaced) and image data that inflates to exactly the size the header
 * says. It is pure Java (no image library, none of the client's): the server judges, and the client checks again when
 * it turns the bytes into a texture.
 */
public final class SkinCheck {
    /** The side of a skin, in pixels. */
    public static final int SIZE = 64;

    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    /** The verdict on a skin, with the lang key of the reason ({@code message.enhanced.hero_creator.skin.<name>}). */
    public enum Verdict {
        OK, EMPTY, TOO_BIG, NOT_PNG, WRONG_SIZE, BROKEN;

        public String messageKey() {
            return "message.enhanced.hero_creator.skin." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static Verdict check(byte[] png, int maxBytes) {
        if (png == null || png.length == 0) {
            return Verdict.EMPTY;
        }
        if (png.length > maxBytes) {
            return Verdict.TOO_BIG;
        }
        if (png.length < SIGNATURE.length + 25) {
            return Verdict.NOT_PNG;
        }
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (png[i] != SIGNATURE[i]) {
                return Verdict.NOT_PNG;
            }
        }
        ByteBuffer buf = ByteBuffer.wrap(png);
        buf.position(SIGNATURE.length);
        int width = -1;
        int height = -1;
        int channels = 0;
        int depth = 0;
        boolean sawEnd = false;
        boolean first = true;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        while (buf.remaining() >= 12) {
            int length = buf.getInt();
            if (length < 0 || length > buf.remaining() - 8) {
                return Verdict.BROKEN;
            }
            int typeAt = buf.position();
            byte[] type = new byte[4];
            buf.get(type);
            byte[] body = new byte[length];
            buf.get(body);
            int crc = buf.getInt();
            CRC32 sum = new CRC32();
            sum.update(png, typeAt, 4 + length);
            if ((int) sum.getValue() != crc) {
                return Verdict.BROKEN;
            }
            String name = new String(type, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (first) {
                if (!name.equals("IHDR") || length != 13) {
                    return Verdict.BROKEN;
                }
                ByteBuffer h = ByteBuffer.wrap(body);
                width = h.getInt();
                height = h.getInt();
                depth = h.get() & 0xFF;
                int colorType = h.get() & 0xFF;
                int compression = h.get() & 0xFF;
                int filter = h.get() & 0xFF;
                int interlace = h.get() & 0xFF;
                if (width != SIZE || height != SIZE) {
                    return Verdict.WRONG_SIZE;
                }
                channels = switch (colorType) {
                    case 0, 3 -> 1;
                    case 2 -> 3;
                    case 4 -> 2;
                    case 6 -> 4;
                    default -> 0;
                };
                if (channels == 0 || depth != 8 || compression != 0 || filter != 0 || interlace != 0) {
                    return Verdict.BROKEN;
                }
                first = false;
            } else if (name.equals("IDAT")) {
                data.write(body, 0, body.length);
            } else if (name.equals("IEND")) {
                sawEnd = true;
                break;
            }
        }
        if (first || !sawEnd || data.size() == 0) {
            return Verdict.BROKEN;
        }
        // Every row is a filter byte and its pixels: nothing more, nothing less.
        int expected = height * (1 + width * channels * depth / 8);
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data.toByteArray());
            byte[] out = new byte[expected + 1];
            int total = 0;
            while (!inflater.finished() && total <= expected) {
                int n = inflater.inflate(out, total, out.length - total);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                total += n;
            }
            if (!inflater.finished() || total != expected) {
                return Verdict.BROKEN;
            }
            for (int row = 0; row < height; row++) {
                if ((out[row * (expected / height)] & 0xFF) > 4) {
                    return Verdict.BROKEN;
                }
            }
        } catch (DataFormatException e) {
            return Verdict.BROKEN;
        } finally {
            inflater.end();
        }
        return Verdict.OK;
    }

    private SkinCheck() {
    }
}
