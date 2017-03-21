package fr.fasar.raspy.services.impl;

import fr.fasar.raspy.services.NoiseListener;
import fr.fasar.raspy.services.SoundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

/**
 * Created by Sartor on 13.03.2017.
 */
public class NoiseDetector implements SoundHandler {
    private static Logger LOG = LoggerFactory.getLogger(NoiseDetector.class);


    private final NoiseListener noiseListener;
    private double defaultnoiseLevel;

    public NoiseDetector(NoiseListener noiseListener, double defaultnoiseLevel) {
        this.noiseListener = noiseListener;
        this.defaultnoiseLevel = defaultnoiseLevel;
    }

    public double getDefaultnoiseLevel() {
        return defaultnoiseLevel;
    }

    public void setDefaultnoiseLevel(double defaultnoiseLevel) {
        this.defaultnoiseLevel = defaultnoiseLevel;
    }

    @Override
    public void addBuffer(byte[] buffer, int offset, int size) throws IOException {
        final double rms = volumeRMS(buffer);
        try {
            if (rms > defaultnoiseLevel) {
                LOG.debug("Noide detection of {}", rms);
                noiseListener.startNoise(Instant.now());
            } else {
                LOG.debug("Silence detection of {}", rms);
                noiseListener.stopNoise(Instant.now());
            }
        } catch (Exception e) {
            LOG.error("Can't warn the noise listener for the state {}" , rms > 1.0?"NOISY":"SILENCE");
        }
    }


    /** Computes the RMS volume of a group of signal sizes ranging from -1 to 1. */
    private double volumeRMS(byte[] raw) {
        double sum = 0d;
        if (raw.length==0) {
            return sum;
        } else {
            for (int ii=0; ii<raw.length; ii++) {
                sum += raw[ii];
            }
        }
        double average = sum/raw.length;

        double sumMeanSquare = 0d;
        for (int ii=0; ii<raw.length; ii++) {
            sumMeanSquare += Math.pow(raw[ii]-average,2d);
        }
        double averageMeanSquare = sumMeanSquare/raw.length;
        double rootMeanSquare = Math.sqrt(averageMeanSquare);

        return rootMeanSquare;
    }

}
