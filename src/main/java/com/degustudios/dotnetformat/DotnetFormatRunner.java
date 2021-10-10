package com.degustudios.dotnetformat;

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
    public DotnetFormatRunner() {
    }

    public DotnetFormatCommandResult runDotnetFormat(Path workingDirectory) {
        String shell = "";
        String executeSwitch = "";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            shell = "cmd.exe";
            executeSwitch = "/c";
        } else {
            shell = "sh";
            executeSwitch = "-c";
        }

        ProcessBuilder builder = new ProcessBuilder();
        Process process = null;
        try {
            process = builder
                    .command(shell, executeSwitch, "dotnet format", "--check")
                    .directory(workingDirectory.toFile())
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
            return DotnetFormatCommandResult.failed(e);
        }

        StringBuffer messageBuffer = new StringBuffer();
        StreamGobbler inputStreamGobbler = new StreamGobbler(process.getInputStream(), s -> messageBuffer.append(s));
        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), s -> messageBuffer.append(s));
        Executors.newSingleThreadExecutor().submit(inputStreamGobbler);
        Executors.newSingleThreadExecutor().submit(errorStreamGobbler);

        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return DotnetFormatCommandResult.failed(e);
        }

        return new DotnetFormatCommandResult(exitCode, messageBuffer.toString());
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

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