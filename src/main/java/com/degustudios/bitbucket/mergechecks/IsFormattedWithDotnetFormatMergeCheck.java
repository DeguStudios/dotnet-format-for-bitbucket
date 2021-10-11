package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component("isFormattedWithDotnetFormatMergeCheck")
public class IsFormattedWithDotnetFormatMergeCheck implements RepositoryMergeCheck {
    private static final String rejectedSummaryMessageWhenRun = "Dotnet format has found issues.";
    private static final String rejectedSummaryMessageWhenCouldNotRun = "Dotnet format could not be run.";
    private final DotnetFormatRefValidator dotnetFormatRefValidator;

    @Autowired
    public IsFormattedWithDotnetFormatMergeCheck(DotnetFormatRefValidator validator) {
        this.dotnetFormatRefValidator = validator;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        DotnetFormatCommandResult result = dotnetFormatRefValidator.validate(request.getFromRef());
        if (result.getExitCode() == 0) {
            return RepositoryHookResult.accepted();
        } else {
            return RepositoryHookResult.rejected(
                    result.hasExecutedCorrectly()
                        ? rejectedSummaryMessageWhenRun
                        : rejectedSummaryMessageWhenCouldNotRun,
                    result.getMessage());
        }
    }


}