package com.degustudios.bitbucket.repository.validators;

import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.bitbucket.content.CodeService;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidator;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.dotnetformat.DotnetFormatRunner;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DotnetFormatRefValidatorImpl implements DotnetFormatRefValidator {
    private final CodeService codeService;
    private final DotnetFormatRunner dotnetFormatRunner;

    public DotnetFormatRefValidatorImpl(CodeService codeService, DotnetFormatRunner dotnetFormatRunner) {
        this.codeService = codeService;
        this.dotnetFormatRunner = dotnetFormatRunner;
    }

    @Override
    public DotnetFormatCommandResult validate(RepositoryRef ref) {
        Path codebaseDirectoryPath = null;
        try {
            codebaseDirectoryPath = Files.createTempDirectory("bb");
            return runDotnetFormat(ref, codebaseDirectoryPath);
        } catch (IOException e) {
            e.printStackTrace();
            return DotnetFormatCommandResult.executedCorrectly(-1, e.getMessage());
        } finally {
            cleanUp(codebaseDirectoryPath);
        }
    }

    private void cleanUp(Path codebaseDirectoryPath) {
        if (codebaseDirectoryPath != null && Files.exists(codebaseDirectoryPath)) {
            try {
                FileUtils.deleteDirectory(new File(codebaseDirectoryPath.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private DotnetFormatCommandResult runDotnetFormat(RepositoryRef ref, Path codebaseDirectoryPath) {
        if (codeService.tryDownloadRepositoryCode(
                codebaseDirectoryPath,
                ref.getRepository(),
                ref.getLatestCommit())) {
            return dotnetFormatRunner.runDotnetFormat(codebaseDirectoryPath);
        } else {
            return DotnetFormatCommandResult.executedCorrectly(
                    -1,
                    "Downloading code failed. Check log file for more information.");
        }
    }
}