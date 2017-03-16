package fr.fasar.raspy.services;

import org.apache.commons.exec.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by fabien on 16/03/2017.
 */
public class CompressFile {

    static String ENC_CMD = "C:\\Utils\\oggenc\\oggenc2.exe";

    public static void compress(File file) throws IOException, InterruptedException {
        CommandLine cmdLine = new CommandLine(ENC_CMD);
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
        resultHandler.waitFor();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        compress(new File("C:\\work\\git\\RaSpy\\outfile\\1489701090163.wav"));
    }
}
