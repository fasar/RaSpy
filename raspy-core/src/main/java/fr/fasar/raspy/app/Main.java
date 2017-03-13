package fr.fasar.raspy.app;

import javax.sound.sampled.*;

/**
 * Created by Sartor on 09.03.2017.
 */
public class Main {

    public static void main(String[] args) throws LineUnavailableException {
        AudioFormat format = format();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format is an AudioFormat object
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Can't handle format " + info);
        }

        // Obtain and open the line.
        TargetDataLine line = null;
        line = AudioSystem.getTargetDataLine(format);
        line.open(format);


        byte[] buffer = new byte[44000];
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + 10_000) {
            final int read = line.read(buffer, 0, buffer.length);
            System.out.println("Buffer of : " + buffer[0] + " " + buffer[1] + " ...(" + read + " elem read)");
        }


    }

    private static AudioFormat format() {
//        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
//        float sampleRate = 8000;
//        int sampleSizeInBits = 16;
//        int channels = 1;
//        int frameSize = 2;
//        float frameRate = 8000;
//        boolean bigEndian = false;
        //return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        return new AudioFormat(8000.0f, 16, 1, true, true);
    }

}
