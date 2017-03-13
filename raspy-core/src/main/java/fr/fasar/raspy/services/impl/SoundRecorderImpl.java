package fr.fasar.raspy.services.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import fr.fasar.raspy.services.NoiseListener;
import fr.fasar.raspy.services.ServiceException;
import fr.fasar.raspy.services.SoundBuffer;
import fr.fasar.raspy.services.SoundRecorder;
import fr.fasar.raspy.sound.WaveWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Created by Sartor on 13.03.2017.
 */
public class SoundRecorderImpl implements SoundRecorder, NoiseListener {
    private static Logger LOG = LoggerFactory.getLogger(SoundRecorder.class);

    private ListeningScheduledExecutorService scheduledExecutorService;
    private final SoundBuffer soundBuffer;
    private final File basePath;
    private Duration minWindow = Duration.of(5, ChronoUnit.SECONDS);
    private final AudioFormat audioFormat;

    private ListenableFuture<Duration> recordTask = null;
    private final Object lockObject = new Object();

    private WaveWriter waveWriter;


    @Inject
    public SoundRecorderImpl(
            ListeningScheduledExecutorService scheduledExecutorService,
            SoundBuffer soundBuffer,
            File basePath,
            Duration minWindow,
            AudioFormat audioFormat
    ) {
        if(! basePath.isDirectory()) {
            throw new IllegalArgumentException("basePath must be a folder");
        }
        this.scheduledExecutorService = scheduledExecutorService;
        this.soundBuffer = soundBuffer;
        this.basePath = basePath;
        this.minWindow = minWindow;
        this.audioFormat = audioFormat;
    }

    @Override
    public void startNoise(Instant instant) throws IOException {
        File out = new File(basePath, "" + System.currentTimeMillis() + ".wav");

        int sampleRate = Math.round(audioFormat.getSampleRate());
        int channels = Math.round(audioFormat.getChannels());
        int sampleBits = Math.round(audioFormat.getSampleSizeInBits());

        synchronized (lockObject) {
            if (waveWriter == null) {
                LOG.debug("Noise start at : {}", instant);
                waveWriter = new WaveWriter(out, sampleRate, channels, sampleBits);
                waveWriter.createWaveFile();
            }

        }
    }

    @Override
    public void stopNoise(Instant instant) throws ServiceException {
        WaveWriter currentWaveWriter = null;
        synchronized (lockObject) {
            if (waveWriter != null) {
                currentWaveWriter = this.waveWriter;
                this.waveWriter = null;
            }
        }

        if(currentWaveWriter  != null ){
            try {
                currentWaveWriter.closeWaveFile();
            } catch (IOException e) {
                throw new ServiceException("Can't write the wave file", e);
            }
        }
    }

    @Override
    public void addBuffer(byte[] buffer, int offset, int size) throws IOException {

        if(waveWriter != null) {
            synchronized (waveWriter) {
                waveWriter.write(buffer, offset, size);
            }
        }
    }
}
