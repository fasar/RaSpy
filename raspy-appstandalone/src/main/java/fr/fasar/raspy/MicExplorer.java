package fr.fasar.raspy;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by fabien on 19/03/2017.
 */
public class MicExplorer {

    public static void main(String[] args) throws InterruptedException, LineUnavailableException {
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

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info: mixerInfos){
            Mixer m = AudioSystem.getMixer(info);
            Line.Info[] lineInfos = m.getSourceLineInfo();
            for (Line.Info lineInfo:lineInfos){
                System.out.println (info.getName()+"---"+lineInfo);
                Line line = m.getLine(lineInfo);
                System.out.println("\t-----"+line);
            }
            lineInfos = m.getTargetLineInfo();
            for (Line.Info lineInfo:lineInfos){
                System.out.println (m+"---"+lineInfo);
                Line line = m.getLine(lineInfo);
                System.out.println("\t-----"+line);

            }

        }

        System.out.println("Supported data type");
        List<AudioFormat> supportedFormats = getSupportedFormats(TargetDataLine.class);
        if(supportedFormats != null) {
            for (AudioFormat supportedFormat : supportedFormats) {
                System.out.println(" -- " + supportedFormat);
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
        } else {
            System.out.println("" + Port.Info.MICROPHONE + " is not supported.");
        }
        if(mic!=null) {
            System.out.println("I get " + mic.getLineInfo() + " object");
        }

        Mixer sourceLine = null;
        if(mic !=null) {
            sourceLine = getSourceLine(mic);
        }
        if(sourceLine != null) {
            Line.Info[] targetLines = sourceLine.getTargetLineInfo();
            System.out.println("Je peux faire un read sur");
            for (Line.Info line1 : Arrays.asList(targetLines)) {
                System.out.println(" - " + line1 + " - " + line1.getClass());
            }
            System.out.println("Je peux faire un write sur");

            for (Line.Info line1 : Arrays.asList(sourceLine.getSourceLineInfo())) {
                System.out.println(" - " + line1 + " - " + line1.getClass());

            }
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
            if(mixer == null) {
                return null;
            }

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

    public static List<AudioFormat> getSupportedFormats(Class<?> dataLineClass) {
    /*
     * These define our criteria when searching for formats supported
     * by Mixers on the system.
     */
        float sampleRates[] = { (float) 8000.0, (float) 16000.0, (float) 44100.0 };
        int channels[] = { 1, 2 };
        int bytesPerSample[] = { 2 };

        AudioFormat format;
        DataLine.Info lineInfo;

        List<AudioFormat> formats = new ArrayList<AudioFormat>();

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            for (int a = 0; a < sampleRates.length; a++) {
                for (int b = 0; b < channels.length; b++) {
                    for (int c = 0; c < bytesPerSample.length; c++) {
                        format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                sampleRates[a], 8 * bytesPerSample[c], channels[b], bytesPerSample[c],
                                sampleRates[a], false);
                        lineInfo = new DataLine.Info(dataLineClass, format);
                        if (AudioSystem.isLineSupported(lineInfo)) {
                        /*
                         * TODO: To perform an exhaustive search on supported lines, we should open
                         * TODO: each Mixer and get the supported lines. Do this if this approach
                         * TODO: doesn't give decent results. For the moment, we just work with whatever
                         * TODO: the unopened mixers tell us.
                         */
                            if (AudioSystem.getMixer(mixerInfo).isLineSupported(lineInfo)) {
                                formats.add(format);
                            }
                        }
                    }
                }
            }
        }
        return formats;
    }


}
