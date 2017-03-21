package fr.fasar.raspy.services.impl;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import fr.fasar.raspy.services.CompressFileService;
import fr.fasar.raspy.services.SoundCreation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by fabien on 16/03/2017.
 */
public class SoundCompress implements SoundCreation {
    private static Logger LOG = LoggerFactory.getLogger(SoundCompress.class);
    private ListeningScheduledExecutorService scheduledExecutorService;
    private CompressFileService compressFileService;

    public SoundCompress(
            ListeningScheduledExecutorService scheduledExecutorService,
            CompressFileService compressFileService
    ) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.compressFileService = compressFileService;
    }

    @Override
    public void newSound(File newFile) {
        scheduledExecutorService.execute(() -> {
            try {
                compressFileService.compress(newFile);
                newFile.delete();
            } catch (Exception e) {
                LOG.error("Can't compress the file {}", e);
            }
        });
    }
}
