package fr.fasar.raspy.services;

import java.time.Instant;

/**
 * Created by Sartor on 13.03.2017.
 */
public interface SoundRecorder {

    public void start(Instant instant);

    public void stop(Instant instant);

}
