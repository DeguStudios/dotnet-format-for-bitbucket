package com.degustudios.dotnetformat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.degustudios.bitbucket.mergechecks.NativeCommandRunner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

@Component
public class NativeCommandRunnerImpl implements NativeCommandRunner {

    private final Logger logger = LoggerFactory.getLogger(NativeCommandRunnerImpl.class);

    @Override
    public DotnetFormatCommandResult runCommand(File workingDirectory, String... commands) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        Process process;
        try {
            process = processBuilder
                    .command(commands)
                    .directory(workingDirectory)
                    .start();
        } catch (IOException e) {
            logger.error("IO exception during running command  with params:  {} Directory: {}",
                    String.join(", ", commands), workingDirectory, e);
            return DotnetFormatCommandResult.failed(e);
        }
        StringBuilder messageBuffer = new StringBuilder();
        StreamGobbler inputStreamGobbler = new StreamGobbler(process.getInputStream(), messageBuffer::append);
        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), messageBuffer::append);
        Executors.newSingleThreadExecutor().submit(inputStreamGobbler);
        Executors.newSingleThreadExecutor().submit(errorStreamGobbler);

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            logger.error("Process was aborted for: parameters: {} in {}",
                    String.join(", ", commands), workingDirectory, e);
            Thread.currentThread().interrupt();
            return DotnetFormatCommandResult.failed(e);
        }
        return DotnetFormatCommandResult.executedCorrectly(
                exitCode,
                messageBuffer.toString());
    }
}
