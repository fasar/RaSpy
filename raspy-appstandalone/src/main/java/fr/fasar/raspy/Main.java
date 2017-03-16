package fr.fasar.raspy;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import fr.fasar.raspy.services.NoiseDetector;
import fr.fasar.raspy.services.ServiceException;
import fr.fasar.raspy.services.SoundBuffer;
import fr.fasar.raspy.services.impl.SoundRecorderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;

/**
 * Created by Sartor on 13.03.2017.
 */
public class Main {
    private static Logger LOG = LoggerFactory.getLogger(Main.class);



    public static void main(String[] args) throws LineUnavailableException, IOException, ServiceException {
        // Create objects dependencies
        AudioFormat format = getAudioFormat();

        ListeningScheduledExecutorService scheduledExecutorService = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(4));
        SoundBuffer soundBuffer = new SoundBuffer(5, Math.round(format.getFrameRate()), Math.round(format.getFrameSize()));
        File outPath = new File("./outfile");
        outPath.mkdirs();
        int oneSecondSamplesNb = Math.round((format.getSampleRate() * format.getChannels() * format.getFrameSize()));



        // Create the service to output wave.
        SoundRecorderImpl soundRecorder = new SoundRecorderImpl(
                scheduledExecutorService, soundBuffer, outPath,
                Duration.of(2, ChronoUnit.SECONDS),
                Duration.of(2, ChronoUnit.SECONDS),
                format);

        // Create the Noise Detector
        NoiseDetector noiseDetector = new NoiseDetector(soundRecorder);


        //Open the mic.
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        // checks if system supports the data line
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Line not supported");
            System.exit(0);
        }
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();   // start capturing

        System.out.println("Start capturing...");

        AudioInputStream ais = new AudioInputStream(line);
        byte[] buffer = new byte[oneSecondSamplesNb];

        boolean run = true;
        while(run) {
            int read = ais.read(buffer, 0, oneSecondSamplesNb);
            try {
                noiseDetector.addBuffer(buffer, 0, read);
                soundRecorder.addBuffer(buffer, 0, read);
            } catch (Exception e) {
                LOG.error("Can't handle the sample", e);
            }
        }

    }

    public static AudioFormat getAudioFormat() {
        float sampleRate = 44_000;
        int sampleSizeInBits = 8;
        int channels = 1;
        boolean signed = false;
        boolean bigEndian = false;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian);
        return format;
    }

}
