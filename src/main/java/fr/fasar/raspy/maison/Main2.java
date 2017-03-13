package fr.fasar.raspy.maison;

import javax.sound.sampled.*;
import java.util.Arrays;

/**
 * Created by fabien on 07/03/2017.
 */
public class Main2 {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Les types  de fichiers audio sont : ");
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.out.println(" - " + type);
        }

        System.out.println("Les type de mixer sont : ");
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println(" - " + info.getName() + " - " + info.getVendor() + "," + info.getDescription());
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] sourceLineInfo = mixer.getSourceLineInfo();
            for (Line.Info info2 : Arrays.asList(sourceLineInfo)) {
                System.out.println("   -> " + info2.toString()  );
            }
            Line.Info[] targetLines = mixer.getTargetLineInfo();
            for (Line.Info info1 : Arrays.asList(targetLines)) {
                System.out.println("   >- " + info1.toString()  );
            }
        }

        AudioFormat format = getFormat();
        TargetDataLine line = getTargetDataLine(format);
        System.out.println("I get " + line.getLineInfo() + " object");


        Port mic = null;
        if (AudioSystem.isLineSupported(Port.Info.MICROPHONE)) {
            try {
                mic = (Port) AudioSystem.getLine(Port.Info.MICROPHONE);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("I get " + mic.getLineInfo() + " object");

        Mixer sourceLine = getSourceLine(mic);
        Line.Info[] targetLines = sourceLine.getTargetLineInfo();
        System.out.println("Je peux faire un read sur");
        for (Line.Info line1 : Arrays.asList(targetLines)) {
            System.out.println(" - " + line1 + " - " + line1.getClass());
        }
        System.out.println("Je peux faire un write sur");

        for (Line.Info line1 : Arrays.asList(sourceLine.getSourceLineInfo())) {
            System.out.println(" - " + line1 + " - " + line1.getClass() );

        }

        Thread.sleep(100);


        // Getting mic
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format is an AudioFormat object
        System.out.println("Data line info of " + info);
        try {
            TargetDataLine mymic = (TargetDataLine) AudioSystem.getLine(info);
            mymic.open(format);

        } catch (LineUnavailableException ex) {
            // Handle the error ...
        }
    }

    private static Mixer getSourceLine(Port elem) {
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] sourceLineInfo = mixer.getSourceLineInfo(elem.getLineInfo());
            for (Line.Info info2 : Arrays.asList(sourceLineInfo)) {
                if (elem.getLineInfo() == info2) {
                    return mixer;
                }
            }
        }

        return null;

    }

    private static TargetDataLine getTargetDataLine(AudioFormat format) {
        TargetDataLine line = null;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format is an AudioFormat object
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Can't open TargetDataLine of " + info);
            System.exit(3);
        }
        // Obtain and open the line.
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
        return line;
    }


    private static AudioFormat getFormat() {
        float sampleRate = 44000;
        int sampleSizeInBits = 8;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate,
                sampleSizeInBits, channels, signed, bigEndian);
    }
}
