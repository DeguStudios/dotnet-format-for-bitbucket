package com.degustudios.dotnetformat;

import org.springframework.stereotype.Service;
import com.degustudios.bitbucket.mergechecks.NativeCommandRunner;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Service
public class DotnetFormatRunner {
    private final NativeCommandRunner commandRunner;

    public DotnetFormatRunner(NativeCommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    public DotnetFormatCommandResult runDotnetFormat(Path workingDirectory, List<String> param) {
        String shell;
        String executeSwitch;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            shell = "cmd.exe";
            executeSwitch = "/c";
        } else {
            shell = "sh";
            executeSwitch = "-c";
        }
        String[] allParams = Stream.concat(
                Arrays.stream(new String[]{shell, executeSwitch, "dotnet format"}),
                param.stream()).toArray(String[]::new);
        return commandRunner.runCommand(workingDirectory.toFile(), allParams);

    }

}