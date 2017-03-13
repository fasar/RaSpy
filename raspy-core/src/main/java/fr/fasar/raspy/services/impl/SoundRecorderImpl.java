package fr.fasar.raspy.services.impl;

import com.google.common.util.concurrent.*;
import fr.fasar.raspy.services.NoiseListener;
import fr.fasar.raspy.services.ServiceException;
import fr.fasar.raspy.services.SoundBuffer;
import fr.fasar.raspy.services.SoundRecorder;
import fr.fasar.raspy.sound.WaveWriter;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final SoundBuffer soundBuffer;

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
        this.soundBuffer = new SoundBuffer(
                (int) Math.max(windowDetection.getSeconds(), windowStop.getSeconds()),
                Math.round(audioFormat.getFrameRate()),
                Math.round(audioFormat.getFrameSize()));


    }

    @Override
    public void startNoise(Instant instant) throws IOException {
        switch (state.get()) {
            case DISCARD:
                LOG.debug("(GOTO {}) Detecting noise for the first time at {}", State.DETECT_NOISE, instant);
                state.set(State.DETECT_NOISE);
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
                            final Byte[] objects;
                            synchronized (soundBuffer) {
                                objects = (Byte[]) soundBuffer.toArray();
                                soundBuffer.clear();
                            }
                            synchronized (lockWriter) {
                                try {
                                    waveWriter.write(ArrayUtils.toPrimitive(objects), 0, objects.length);
                                } catch (IOException e) {
                                    LOG.error("Can't write the buffer on the file {}", out.getAbsolutePath());
                                }
                            }
                        }
                    }, windowDetection.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);

                    Futures.addCallback(task, new FutureCallback<Object>() {
                        @Override
                        public void onSuccess(Object result) {
                            LOG.debug("Task to confirm noise is success");
                            state.set(State.WRITING);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            LOG.debug("Task to confirm noise fails");
                            state.set(State.DISCARD);
                            finishOutput();
                        }
                    });
                }
                break;
            case DETECT_SILENCE:

                synchronized (lockTask) {
                    if(task != null) {
                        task.cancel(true);

                        Futures.addCallback(task, new FutureCallback<Object>() {
                            @Override
                            public void onSuccess(Object result) {
                                LOG.debug("The task is already success - The new state must be Discard. New state : {}", state.get());
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                logChangeState(State.WRITING);
                                state.set(State.WRITING);
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
                    waveWriter.closeWaveFile();
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
                logChangeState(State.DISCARD);
                LOG.debug("(GOTO {}) Detecting silence during the detection window of {} seconds.", State.DISCARD, windowDetection.get(ChronoUnit.SECONDS));
                synchronized (lockTask) {
                    if(task != null) {
                        task.cancel(true);
                        task = null;
                    }
                }
                state.set(State.DISCARD);
                break;
            case WRITING:
                logChangeState(State.DETECT_SILENCE);
                state.set(State.DETECT_SILENCE);

                synchronized (lockTask) {
                    task = scheduledExecutorService.schedule(() -> {
                        LOG.debug("End of the task of waiting for silence detection");
                        if(state.get() == State.DETECT_SILENCE) {

                        } else {
                            throw new RuntimeException();
                        }
                    }, windowStop.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);

                    Futures.addCallback(task, new FutureCallback<Object>() {
                        @Override
                        public void onSuccess(Object result) {
                            LOG.debug("Task to wait silence is success");
                            logChangeState(State.DISCARD);
                            state.set(State.DISCARD);
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
                logChangeState(state.get());
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

    private void logChangeState(State newState) {
        LOG.debug("({} -> {})", state.get(), newState);
    }

    @Override
    public void addBuffer(byte[] buffer, int offset, int size) throws IOException {
        switch (state.get()) {
            case DISCARD:
            case DETECT_NOISE:
            case WRITING:
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
