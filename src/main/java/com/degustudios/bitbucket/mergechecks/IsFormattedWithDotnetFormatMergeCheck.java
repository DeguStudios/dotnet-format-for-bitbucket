package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Component("isFormattedWithDotnetFormatMergeCheck")
public class IsFormattedWithDotnetFormatMergeCheck implements RepositoryMergeCheck {
    private final CodeService codeService;
    private final DotnetFormatRunner dotnetFormatRunner;
    private final String rejectedSummaryMessage = "Dotnet format has found issues.";

    @Autowired
    public IsFormattedWithDotnetFormatMergeCheck(CodeService codeService, DotnetFormatRunner dotnetFormatRunner) {
        this.codeService = codeService;
        this.dotnetFormatRunner = dotnetFormatRunner;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        Path codebaseDirectoryPath;
        try {
            codebaseDirectoryPath = Files.createTempDirectory("bb");
        } catch (IOException e) {
            e.printStackTrace();
            return RepositoryHookResult.rejected(rejectedSummaryMessage, e.getMessage());
        }

        DotnetFormatCommandResult result = runDotnetFormat(request, codebaseDirectoryPath);

        if (result.getExitCode() == 0) {
            return RepositoryHookResult.accepted();
        } else {
            return RepositoryHookResult.rejected(rejectedSummaryMessage, result.getMessage());
        }
    }

    private DotnetFormatCommandResult runDotnetFormat(PullRequestMergeHookRequest request, Path codebaseDirectoryPath)  {
        if (codeService.tryDownloadRepositoryCode(
                codebaseDirectoryPath,
                request.getRepository(),
                request.getFromRef().getLatestCommit())) {
            return dotnetFormatRunner.runDotnetFormat(codebaseDirectoryPath);
        } else {
            return new DotnetFormatCommandResult(-1, "Downloading code failed. Check log file for more information.");
        }
    }
}