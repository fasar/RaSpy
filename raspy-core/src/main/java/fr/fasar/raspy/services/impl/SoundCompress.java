package fr.fasar.raspy.services.impl;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import fr.fasar.raspy.services.CompressFile;
import fr.fasar.raspy.services.SoundCreation;
import fr.fasar.raspy.services.SoundRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by fabien on 16/03/2017.
 */
public class SoundCompress implements SoundCreation {
    private static Logger LOG = LoggerFactory.getLogger(SoundCompress.class);
    private ListeningScheduledExecutorService scheduledExecutorService;

    public SoundCompress(ListeningScheduledExecutorService scheduledExecutorService) {

        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Override
    public void newSound(File newFile) {
        scheduledExecutorService.execute(() -> {
            try {
                CompressFile.compress(newFile);
                newFile.delete();
            } catch (Exception e) {
                LOG.error("Can't compress the file {}", e);
            }
        });
    }
}
