package com.degustudios.dotnetformat;

import junit.framework.TestCase;
import org.mockito.Mockito;
import ut.com.degustudios.bitbucket.mergechecks.NativeCommandRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class DotnetFormatRunnerTest extends TestCase {

    private static final String WINDOWS_10 = "WINDOWS 10";
    private static final String LINUX = "LINUX";

    public void testRunDotnetFormatInWindows() throws IOException {
        //precondition
        NativeCommandRunner nativeCommandRunner = Mockito.mock(NativeCommandRunner.class);
        final DotnetFormatRunner dotnetFormatRunner = new DotnetFormatRunner(nativeCommandRunner);
        Path tmp = Files.createTempDirectory("tempDir1");
        //Given
        System.setProperty("os.name", WINDOWS_10);

        // action
        dotnetFormatRunner.runDotnetFormat(tmp, Collections.singletonList(" --abc2"));

        // result
        Mockito.verify(nativeCommandRunner).runCommand(tmp.toFile(),
                "cmd.exe", "/c", "dotnet format", " --abc2");
    }

    public void testRunDotnetFormatInLinux() throws IOException {
        //precondition
        NativeCommandRunner nativeCommandRunner = Mockito.mock(NativeCommandRunner.class);
        final DotnetFormatRunner dotnetFormatRunner = new DotnetFormatRunner(nativeCommandRunner);
        Path tmp = Files.createTempDirectory("tempDir1");
        //Given
        System.setProperty("os.name", LINUX);

        // action
        dotnetFormatRunner.runDotnetFormat(tmp, Collections.singletonList(" --abc"));

        // result
        Mockito.verify(nativeCommandRunner).runCommand(tmp.toFile(),
                "sh",  "-c", "dotnet format", " --abc");
    }
}