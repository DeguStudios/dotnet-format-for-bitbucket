package com.degustudios.dotnetformat;

public class DotnetFormatCommandResult {
    private int exitCode;
    private String message;
    private Exception exception;
    private boolean hasExecutedCorrectly;

    private DotnetFormatCommandResult(int exitCode, String message, Exception exception, boolean hasExecutedCorrectly) {
        this.exitCode = exitCode;
        this.message = message;
        this.exception = exception;
        this.hasExecutedCorrectly = hasExecutedCorrectly;
    }

    private DotnetFormatCommandResult(int exitCode, String message, boolean hasExecutedCorrectly) {
        this(exitCode, message, null, hasExecutedCorrectly);
    }

    public static DotnetFormatCommandResult executedCorrectly(int exitCode, String message) {
        return new DotnetFormatCommandResult(exitCode, message, null, true);
    }

    public static DotnetFormatCommandResult failed(Exception e) {
        return new DotnetFormatCommandResult(
                -1,
                "Failed to execute dotnet-format command: " + e.getMessage(),
                e,
                false);
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasExecutedCorrectly() { return hasExecutedCorrectly; }

    public Exception getException() { return exception; }
}
