package fr.fasar.raspy.services.impl;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import fr.fasar.raspy.services.SoundBuffer;
import fr.fasar.raspy.services.SoundRecorder;

import javax.inject.Inject;
import javax.sound.sampled.TargetDataLine;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Created by Sartor on 13.03.2017.
 */
public class SoundRecorderImpl implements SoundRecorder {

    private ListeningScheduledExecutorService scheduledExecutorService;
    private final SoundBuffer soundBuffer;
    private final TargetDataLine input;
    private final File basePath;

    private Instant start = null;

    private Duration minWindow = Duration.of(5, ChronoUnit.SECONDS);

    @Inject
    public SoundRecorderImpl(
            ListeningScheduledExecutorService scheduledExecutorService,
            SoundBuffer soundBuffer,
            TargetDataLine input,
            File basePath) {
        if(! basePath.exists()) {
            basePath.mkdirs();
        }
        if(! basePath.isDirectory()) {
            throw new IllegalArgumentException("basePath must be a folder");
        }
        this.scheduledExecutorService = scheduledExecutorService;
        this.soundBuffer = soundBuffer;
        this.input = input;
        this.basePath = basePath;
    }

    @Override
    public void start(Instant instant) {

    }

    @Override
    public void stop(Instant instant) {

    }
}
