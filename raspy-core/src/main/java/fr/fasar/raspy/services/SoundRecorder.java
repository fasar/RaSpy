package fr.fasar.raspy.services;

import java.io.IOException;

/**
 * Created by Sartor on 13.03.2017.
 */
public interface SoundRecorder {

    public void addBuffer(byte[] buffer, int offset, int size) throws IOException;

}
