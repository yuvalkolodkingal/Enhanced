package com.kolodkingal.enhanced.client.music;

import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** Plays an uncompressed WAV file (converted to 16-bit signed PCM if needed). */
public class PcmAudioStream implements AudioStream {
    private final AudioInputStream audio;

    private PcmAudioStream(AudioInputStream audio) {
        this.audio = audio;
    }

    public static PcmAudioStream fromWav(InputStream input) throws IOException {
        try {
            AudioInputStream source = AudioSystem.getAudioInputStream(input);
            AudioFormat in = source.getFormat();
            AudioFormat pcm16 = new AudioFormat(in.getSampleRate(), 16, in.getChannels(), true, false);
            return new PcmAudioStream(AudioSystem.getAudioInputStream(pcm16, source));
        } catch (UnsupportedAudioFileException | IllegalArgumentException e) {
            throw new IOException("Unsupported WAV file", e);
        }
    }

    @Override
    public AudioFormat getFormat() {
        return audio.getFormat();
    }

    @Override
    public ByteBuffer read(int size) throws IOException {
        byte[] bytes = new byte[size - size % 4];
        int total = 0;
        while (total < bytes.length) {
            int n = audio.read(bytes, total, bytes.length - total);
            if (n <= 0) {
                break;
            }
            total += n;
        }
        ByteBuffer buffer = BufferUtils.createByteBuffer(total);
        buffer.put(bytes, 0, total).flip();
        return buffer;
    }

    @Override
    public void close() throws IOException {
        audio.close();
    }
}
