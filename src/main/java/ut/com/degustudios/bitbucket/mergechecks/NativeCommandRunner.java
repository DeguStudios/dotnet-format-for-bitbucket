package ut.com.degustudios.bitbucket.mergechecks;

import com.degustudios.dotnetformat.DotnetFormatCommandResult;

import java.io.File;

public interface NativeCommandRunner {
    DotnetFormatCommandResult runCommand(File directory, String... command);
}
