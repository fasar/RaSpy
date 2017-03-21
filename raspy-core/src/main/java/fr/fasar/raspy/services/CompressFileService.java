package fr.fasar.raspy.services;

import org.apache.commons.exec.*;

import java.io.File;
import java.io.IOException;

/**
 * Created by fabien on 16/03/2017.
 */
public class CompressFileService {

    private String appPath;

    public CompressFileService(String apppath) {
        this.appPath = apppath;
    }


    public void compress(File file) throws IOException, InterruptedException {
        CommandLine cmdLine = new CommandLine(appPath);
        cmdLine.addArgument("-o");
        cmdLine.addArgument(file.getAbsolutePath().replace(".wav", ".ogg"));
        cmdLine.addArgument(file.getAbsolutePath());

        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);
        Executor executor = new DefaultExecutor();
        executor.setExitValue(1);
        executor.setWatchdog(watchdog);
        executor.execute(cmdLine, resultHandler);

        // some time later the result handler callback was invoked so we
        // can safely request the exit value
        resultHandler.waitFor(100_000);
    }

}
