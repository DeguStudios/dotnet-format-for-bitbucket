package com.degustudios.bitbucket.mergechecks;

public class DotnetFormatCommandResult {
    private int exitCode;
    private String message;

    public DotnetFormatCommandResult(int exitCode, String message) {
        this.exitCode = exitCode;
        this.message = message;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getMessage() {
        return message;
    }
}
