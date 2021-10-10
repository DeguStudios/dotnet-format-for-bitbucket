package com.degustudios.bitbucket.mergechecks;

import java.io.IOException;

public class DotnetFormatCommandResult {
    private int exitCode;
    private String message;

    public DotnetFormatCommandResult(int exitCode, String message) {
        this.exitCode = exitCode;
        this.message = message;
    }

    public static DotnetFormatCommandResult failed(Exception e) {
        return new DotnetFormatCommandResult(-1, "Failed to execute dotnet-format command: " + e.getMessage());
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getMessage() {
        return message;
    }
}
