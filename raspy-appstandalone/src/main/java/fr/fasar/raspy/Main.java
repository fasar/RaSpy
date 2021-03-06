package fr.fasar.raspy;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import fr.fasar.raspy.services.CompressFileService;
import fr.fasar.raspy.services.impl.NoiseDetector;
import fr.fasar.raspy.services.ServiceException;
import fr.fasar.raspy.services.SoundBuffer;
import fr.fasar.raspy.services.impl.SoundCompress;
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
        double defaultnoiseLevel = 87.0;
        if(args.length > 0) {
            defaultnoiseLevel = Double.parseDouble(args[0]);
        }

        LOG.debug("Starting the application with silence detection of {}", defaultnoiseLevel);
        // Create objects dependencies
        AudioFormat format = getAudioFormat();

        LOG.debug("Initialize all objects");
        ListeningScheduledExecutorService scheduledExecutorService = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(4));
        SoundBuffer soundBuffer = new SoundBuffer(5, Math.round(format.getFrameRate()), Math.round(format.getFrameSize()));
        File outPath = new File("./outfile");
        outPath.mkdirs();
        int oneSecondSamplesNb = Math.round((format.getSampleRate() * format.getChannels() * format.getFrameSize()));


        // Create the service to output wave.
        SoundRecorderImpl soundRecorder = new SoundRecorderImpl(
                scheduledExecutorService, soundBuffer, outPath,
                Duration.of(2, ChronoUnit.SECONDS),
                Duration.of(10, ChronoUnit.SECONDS),
                format, new SoundCompress(
                        scheduledExecutorService,
                        new CompressFileService("oggenc")
                ));

        // Create the Noise Detector
        NoiseDetector noiseDetector = new NoiseDetector(soundRecorder, defaultnoiseLevel);

        //Open the mic.
        LOG.debug("Openning the targetDataLine with format {}", format);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        // checks if system supports the data line
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Line not supported");
            System.exit(0);
        }
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();   // start capturing

        LOG.info("Start capturing...");
        AudioInputStream ais = new AudioInputStream(line);
        byte[] buffer = new byte[oneSecondSamplesNb];

        boolean run = true;
        while (run) {
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
        float sampleRate = 44_100;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits,
                channels, signed, bigEndian);
        return format;

//        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
//        return format;
    }

}
