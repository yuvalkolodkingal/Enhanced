package com.kolodkingal.enhanced.client.music;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Decodes an MP3 into 16-bit PCM for the sound engine, using JLayer. */
public class Mp3AudioStream implements AudioStream {
    private final Bitstream bitstream;
    private final Decoder decoder = new Decoder();
    private final InputStream input;
    private AudioFormat format;
    /** Decoded samples not handed out yet. */
    private short[] pending = new short[0];
    private int pendingPos;
    private boolean ended;

    public Mp3AudioStream(InputStream input) throws IOException {
        this.input = input;
        this.bitstream = new Bitstream(input);
        // Decode the first frame now so the format (sample rate, channels) is known.
        if (!decodeFrame()) {
            throw new IOException("Not a playable MP3 file");
        }
    }

    private boolean decodeFrame() throws IOException {
        try {
            Header header = bitstream.readFrame();
            if (header == null) {
                ended = true;
                return false;
            }
            SampleBuffer out = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            bitstream.closeFrame();
            if (format == null) {
                format = new AudioFormat(decoder.getOutputFrequency(), 16, decoder.getOutputChannels(), true, false);
            }
            short[] samples = new short[out.getBufferLength()];
            System.arraycopy(out.getBuffer(), 0, samples, 0, samples.length);
            pending = samples;
            pendingPos = 0;
            return true;
        } catch (BitstreamException | DecoderException e) {
            throw new IOException("Broken MP3 frame", e);
        }
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public ByteBuffer read(int size) throws IOException {
        int samples = size / 2;
        ByteBuffer buffer = BufferUtils.createByteBuffer(samples * 2).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() >= 2) {
            if (pendingPos >= pending.length) {
                if (ended || !decodeFrame()) {
                    break;
                }
                continue;
            }
            buffer.putShort(pending[pendingPos++]);
        }
        buffer.flip();
        return buffer;
    }

    @Override
    public void close() throws IOException {
        try {
            bitstream.close();
        } catch (BitstreamException e) {
            throw new IOException(e);
        } finally {
            input.close();
        }
    }
}
