package com.degustudios.dotnetformat;

public class DotnetFormatCommandResult {
    private final int exitCode;
    private final String message;
    private final Exception exception;
    private final boolean hasExecutedCorrectly;

    private DotnetFormatCommandResult(int exitCode, String message, Exception exception, boolean hasExecutedCorrectly) {
        this.exitCode = exitCode;
        this.message = message;
        this.exception = exception;
        this.hasExecutedCorrectly = hasExecutedCorrectly;
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

    public static DotnetFormatCommandResult failed(String message) {
        return new DotnetFormatCommandResult(-1, message, null, false);
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
