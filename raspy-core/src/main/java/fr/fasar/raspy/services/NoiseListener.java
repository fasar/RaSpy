package fr.fasar.raspy.services;

import java.io.IOException;
import java.time.Instant;

/**
 * Created by Sartor on 13.03.2017.
 */
public interface NoiseListener {

    void startNoise(Instant instant) throws ServiceException, IOException;

    void stopNoise(Instant instant) throws ServiceException;


}
