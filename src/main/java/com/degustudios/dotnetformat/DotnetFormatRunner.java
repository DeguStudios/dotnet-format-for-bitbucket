package com.degustudios.dotnetformat;

import org.springframework.stereotype.Service;
import ut.com.degustudios.bitbucket.mergechecks.NativeCommandRunner;

import java.nio.file.Path;

@Service
public class DotnetFormatRunner {
    private final NativeCommandRunner commandRunner;

    public DotnetFormatRunner(NativeCommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    public DotnetFormatCommandResult runDotnetFormat(Path workingDirectory, String param) {
        String shell;
        String executeSwitch;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            shell = "cmd.exe";
            executeSwitch = "/c";
        } else {
            shell = "sh";
            executeSwitch = "-c";
        }
       return commandRunner.runCommand(workingDirectory.toFile(), shell, executeSwitch, "dotnet format", param);

    }

}