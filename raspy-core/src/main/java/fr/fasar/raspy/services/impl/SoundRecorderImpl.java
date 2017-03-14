package fr.fasar.raspy.services.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Sartor on 13.03.2017.
 */
public class SoundRecorderImpl implements SoundRecorder, NoiseListener {
    private static Logger LOG = LoggerFactory.getLogger(SoundRecorder.class);

    private ListeningScheduledExecutorService scheduledExecutorService;
    private final File basePath;
    private final AudioFormat audioFormat;

    private Duration windowStop = Duration.of(15, ChronoUnit.SECONDS);
    private Duration windowDetection = Duration.of(5, ChronoUnit.SECONDS);
    private final List<byte[]> soundBuffer = new ArrayList<>();

    private AtomicReference<State> state = new AtomicReference<>(State.DISCARD);

    private WaveWriter waveWriter;
    private final Object lockWriter = new Object();
    private final Object lockTask = new Object();
    private ListenableScheduledFuture<?> task;


    @Inject
    public SoundRecorderImpl(
            ListeningScheduledExecutorService scheduledExecutorService,
            SoundBuffer soundBuffer,
            File basePath,
            Duration windowDetection,
            Duration windowStop,
            AudioFormat audioFormat
    ) {
        if (!basePath.isDirectory()) {
            throw new IllegalArgumentException("basePath must be a folder");
        }
        this.scheduledExecutorService = scheduledExecutorService;
        this.basePath = basePath;
        this.windowDetection = windowDetection;
        this.windowStop = windowStop;
        this.audioFormat = audioFormat;
    }

    @Override
    public void startNoise(Instant instant) throws IOException {
        switch (state.get()) {
            case DISCARD:
                LOG.debug("(GOTO {}) Detecting noise for the first time at {}", State.DETECT_NOISE, instant);
                changeState(State.DETECT_NOISE, "start");
                final long currentTimeMillis = System.currentTimeMillis();

                // Create the output file
                // Write data in the buffer.
                synchronized (lockTask) {
                    task = scheduledExecutorService.schedule(() -> {
                        if (state.get() == State.DETECT_NOISE) {
                            LOG.debug("(GOTO {}) Detecting noise more than {} seconds. Write buffer in the file", State.WRITING, windowDetection.get(ChronoUnit.SECONDS));
                            // Create the output file
                            File out = new File(basePath, "" + currentTimeMillis + ".wav");
                            try {
                                createTheOutputFile(out, instant, currentTimeMillis);
                            } catch (IOException e) {
                                LOG.error("Can't create the output file {}", out.getAbsolutePath());
                                throw new RuntimeException(e);
                            }
                            // Write data in the buffer.
                            final byte[][] objects;
                            synchronized (soundBuffer) {
                                if (!soundBuffer.isEmpty()) {
                                    synchronized (lockWriter) {
                                        try {
                                            for (byte[] bytes : soundBuffer) {
                                                waveWriter.write(bytes, 0, bytes.length);
                                            }
                                        } catch (IOException e) {
                                            LOG.error("Can't write buffer on wave", e);
                                        }
                                    }
                                    soundBuffer.clear();
                                }
                            }

                        }
                    }, windowDetection.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);

                    Futures.addCallback(task, new FutureCallback<Object>() {
                        @Override
                        public void onSuccess(Object result) {
                            LOG.debug("Task to confirm noise is success");
                            changeState(State.WRITING, "T+winDet");
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            LOG.debug("Task to confirm noise fails", t);
                            changeState(State.DISCARD, "T+winDetFail");
                            finishOutput();
                        }
                    });
                }
                break;
            case DETECT_SILENCE:

                synchronized (lockTask) {
                    if (task != null) {
                        task.cancel(true);

                        Futures.addCallback(task, new FutureCallback<Object>() {
                            @Override
                            public void onSuccess(Object result) {
                                LOG.debug("The task is already success - The new state must be Discard. New state : {}", state.get());
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                changeState(State.WRITING, "start(timeout)");
                            }
                        });
                    }

                }
                break;
            case DETECT_NOISE:
            case WRITING:
            default:
        }


    }

    private void createTheOutputFile(File out, Instant instant, long currentTimeMillis) throws IOException {
        int sampleRate = Math.round(audioFormat.getSampleRate());
        int channels = Math.round(audioFormat.getChannels());
        int sampleBits = Math.round(audioFormat.getSampleSizeInBits());

        synchronized (lockWriter) {
            if (waveWriter == null) {
                LOG.debug("Noise start at : {}", instant);
                waveWriter = new WaveWriter(out, sampleRate, channels, sampleBits);
                waveWriter.createWaveFile();
                waveWriter = null;
            }
        }
    }

    private void finishOutput() {
        synchronized (lockWriter) {
            if (waveWriter != null) {
                try {
                    LOG.debug("Finish waveWriter");
                    waveWriter.closeWaveFile();
                    waveWriter = null;
                } catch (IOException e) {
                    LOG.error("Can't close the file");
                }
            }
        }
    }

    @Override
    public void stopNoise(Instant instant) throws ServiceException {
        switch (state.get()) {
            case DETECT_NOISE:
                LOG.debug("(GOTO {}) Detecting silence during the detection window of {} seconds.", State.DISCARD, windowDetection.get(ChronoUnit.SECONDS));
                synchronized (lockTask) {
                    if (task != null) {
                        if (!task.isCancelled() && !task.isDone()) {
                            task.cancel(true);
                        }
                        task = null;
                    }
                }
                changeState(State.DISCARD, "stop");
                break;
            case WRITING:
                changeState(State.DETECT_SILENCE, "stop");

                synchronized (lockTask) {
                    task = scheduledExecutorService.schedule(() -> {
                        LOG.debug("End of the task of waiting for silence detection");
                        if (state.get() != State.DETECT_SILENCE) {
                            throw new RuntimeException();
                        }
                    }, windowStop.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);

                    Futures.addCallback(task, new FutureCallback<Object>() {
                        @Override
                        public void onSuccess(Object result) {
                            LOG.debug("Task to wait silence is success");
                            changeState(State.DISCARD, "t+winStop");
                            finishOutput();
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            LOG.debug("Task to wait silence fails");
                        }
                    });

                }
                break;
            case DETECT_SILENCE:
            case DISCARD:
                break;
        }

        WaveWriter currentWaveWriter = null;
        synchronized (lockTask) {
            if (waveWriter != null) {
                currentWaveWriter = this.waveWriter;
                this.waveWriter = null;
            }
        }

        if (currentWaveWriter != null) {
            try {
                currentWaveWriter.closeWaveFile();
            } catch (IOException e) {
                throw new ServiceException("Can't write the wave file", e);
            }
        }
    }

    private void changeState(State newState, String action) {
        LOG.debug("({} -{}-> {}) ", state.get(), action, newState);
        state.set(newState);
    }

    @Override
    public void addBuffer(byte[] buffer, int offset, int size) throws IOException {
        switch (state.get()) {
            case DETECT_NOISE:


            case WRITING:
            case DISCARD:
            case DETECT_SILENCE:
        }

        if (waveWriter != null) {
            synchronized (waveWriter) {
                waveWriter.write(buffer, offset, size);
            }
        }


    }

    private static enum State {
        DISCARD, DETECT_NOISE, WRITING, DETECT_SILENCE
    }
}
