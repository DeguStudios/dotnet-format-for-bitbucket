package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

@Component("isFormattedWithDotnetFormatMergeCheck")
public class IsFormattedWithDotnetFormatMergeCheck implements RepositoryMergeCheck {
    private final CodeService codeService;
    private final DotnetFormatRunner dotnetFormatRunner = new DotnetFormatRunner();

    @Autowired
    public IsFormattedWithDotnetFormatMergeCheck(@ComponentImport ContentService contentService) {
        this.codeService = new CodeService(contentService);
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        try {
            Path codebaseDirectoryPath = Files.createTempDirectory("bb");
            
            DotnetFormatCommandResult result = runDotnetFormat(request, codebaseDirectoryPath);

            if (result.getExitCode() != 0) {
                return RepositoryHookResult.rejected("Dotnet format has found issues.", result.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return RepositoryHookResult.accepted();
    }

    private DotnetFormatCommandResult runDotnetFormat(PullRequestMergeHookRequest request, Path codebaseDirectoryPath) throws IOException, InterruptedException {
        codeService.downloadRepositoryCode(
                codebaseDirectoryPath,
                request.getRepository(),
                request.getFromRef().getLatestCommit());
        return dotnetFormatRunner.runDotnetFormat(codebaseDirectoryPath);
    }
}