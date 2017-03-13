package fr.fasar.raspy.maison;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by fabien on 12/03/2017.
 */
public class RmsDetector {

    public static void main(String[] args) throws LineUnavailableException {
        final AudioFormat format = getFormat();
        DataLine.Info info = new DataLine.Info(
                TargetDataLine.class, format);
        final TargetDataLine line = (TargetDataLine)AudioSystem.getLine(info);
        line.open(format);
        line.start();

        int bufferSize = (int) format.getSampleRate()
                * format.getFrameSize();
        byte buffer[] = new byte[bufferSize];
        System.out.println("Buffer size of : " + bufferSize + " octets");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean running = true;
        try {
            for (int i = 0; i < 10000; i++) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                    double rms = volumeRMS(buffer);
                    System.out.println("RMS of slice is: " + rms+ "First bytes : " + buffer[0] + " " + buffer[1]);
                }
            }
            out.close();
        } catch (IOException e) {
            System.err.println("I/O problems: " + e);
            System.exit(-1);
        }
        System.out.println("End of capture Audio");
    }




    private static AudioFormat getFormat() {
        float sampleRate = 16000;
        int sampleSizeInBits = 8;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate,
                sampleSizeInBits, channels, signed, bigEndian);
    }



    /** Computes the RMS volume of a group of signal sizes ranging from -1 to 1. */
    public static double volumeRMS(byte[] raw) {
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
