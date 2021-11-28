package ut.com.degustudios.dotnetformat;

import com.degustudios.dotnetformat.DotnetFormatRunner;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ut.com.degustudios.bitbucket.mergechecks.NativeCommandRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class DotnetFormatRunnerTest  {
    private static final String WINDOWS_10 = "WINDOWS 10";
    private static final String LINUX = "LINUX";

    private DotnetFormatRunner dotnetFormatRunner;
    private Path tempDirectory;
    private NativeCommandRunner nativeCommandRunner;

    @Before
    public void initialize() throws IOException {
        nativeCommandRunner = Mockito.mock(NativeCommandRunner.class);
        dotnetFormatRunner = new DotnetFormatRunner(nativeCommandRunner);
        tempDirectory = Files.createTempDirectory("tempDir1");
    }

    @Test
    public void testRunDotnetFormatInWindows() {
        System.setProperty("os.name", WINDOWS_10);

        dotnetFormatRunner.runDotnetFormat(tempDirectory, Collections.singletonList(" --abc2"));

        Mockito.verify(nativeCommandRunner).runCommand(tempDirectory.toFile(),
                "cmd.exe", "/c", "dotnet format", " --abc2");
    }

    @Test
    public void testRunDotnetFormatInLinux() {
        System.setProperty("os.name", LINUX);

        dotnetFormatRunner.runDotnetFormat(tempDirectory, Collections.singletonList(" --abc"));

        Mockito.verify(nativeCommandRunner).runCommand(tempDirectory.toFile(),
                "sh",  "-c", "dotnet format", " --abc");
    }
}