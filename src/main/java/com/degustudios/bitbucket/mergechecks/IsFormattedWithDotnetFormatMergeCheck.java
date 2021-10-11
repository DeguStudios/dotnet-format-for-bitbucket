package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import com.degustudios.bitbucket.content.CodeService;
import com.degustudios.bitbucket.repository.validators.DotnetFormatRefValidator;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.dotnetformat.DotnetFormatRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component("isFormattedWithDotnetFormatMergeCheck")
public class IsFormattedWithDotnetFormatMergeCheck implements RepositoryMergeCheck {
    private final String rejectedSummaryMessage = "Dotnet format has found issues.";
    private final DotnetFormatRefValidator dotnetFormatRefValidator;

    @Autowired
    public IsFormattedWithDotnetFormatMergeCheck(CodeService codeService, DotnetFormatRunner dotnetFormatRunner) {
        this.dotnetFormatRefValidator = new DotnetFormatRefValidator(codeService, dotnetFormatRunner);
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        DotnetFormatCommandResult result = dotnetFormatRefValidator.validate(request.getFromRef());
        if (result.getExitCode() == 0) {
            return RepositoryHookResult.accepted();
        } else {
            return RepositoryHookResult.rejected(rejectedSummaryMessage, result.getMessage());
        }
    }


}