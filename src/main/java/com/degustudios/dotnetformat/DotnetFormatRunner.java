package com.degustudios.dotnetformat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class DotnetFormatRunner {

    private static Logger LOGGER = LoggerFactory.getLogger(DotnetFormatRunner.class);

    public DotnetFormatRunner() {
    }

    public DotnetFormatCommandResult runDotnetFormat(Path workingDirectory) {
        String shell;
        String executeSwitch;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            shell = "cmd.exe";
            executeSwitch = "/c";
        } else {
            shell = "sh";
            executeSwitch = "-c";
        }

        ProcessBuilder builder = new ProcessBuilder();
        Process process;
        try {
            process = builder
                    .command(shell, executeSwitch, "dotnet format", "--check")
                    .directory(workingDirectory.toFile())
                    .start();
        } catch (IOException e) {
            LOGGER.error("IO exception in main loop with params:  dotnet format, --check, Directory: ", workingDirectory.toString(),  e);
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
            e.printStackTrace();
            return DotnetFormatCommandResult.failed(e);
        }

        return DotnetFormatCommandResult.executedCorrectly(
                exitCode,
                messageBuffer.toString());
    }

    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .forEach(consumer);
        }
    }
}