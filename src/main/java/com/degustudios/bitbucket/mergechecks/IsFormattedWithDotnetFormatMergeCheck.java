package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;

@Component("isFormattedWithDotnetFormatMergeCheck")
public class IsFormattedWithDotnetFormatMergeCheck implements RepositoryMergeCheck {
    private static final String REJECTED_SUMMARY_MESSAGE_WHEN_RUN = "Dotnet format has found issues.";
    private static final String REJECTED_SUMMARY_MESSAGE_WHEN_COULD_NOT_RUN = "Dotnet format could not be run.";
    private final DotnetFormatRefValidator dotnetFormatRefValidator;
    private final PullRequestCommenter pullRequestCommenter;
    private final DotnetFormatRefValidatorParameterCalculator parameterCalculator;

    @Autowired
    public IsFormattedWithDotnetFormatMergeCheck(
            @Qualifier("IdempotentlyCachedDotnetFormatRefValidatorWrapper") DotnetFormatRefValidator validator,
            PullRequestCommenter pullRequestCommenter,
            DotnetFormatRefValidatorParameterCalculator parameterCalculator) {
        this.dotnetFormatRefValidator = validator;
        this.pullRequestCommenter = pullRequestCommenter;
        this.parameterCalculator = parameterCalculator;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        List<String> allParameters = parameterCalculator.calculateParameters(context.getSettings(), request.getPullRequest());
        DotnetFormatCommandResult result = dotnetFormatRefValidator.validate(request.getFromRef(), allParameters);

        if (result.getExitCode() == 0) {
            return RepositoryHookResult.accepted();
        } else if (!result.hasExecutedCorrectly()) {
            return RepositoryHookResult.rejected(REJECTED_SUMMARY_MESSAGE_WHEN_COULD_NOT_RUN, result.getMessage());
        } else {
            pullRequestCommenter.addComment(
                    request.getPullRequest(),
                    "dotnet-format results:" + System.lineSeparator() + result.getMessage());
            return RepositoryHookResult.rejected(
                    REJECTED_SUMMARY_MESSAGE_WHEN_RUN,
                    "Dotnet format exit code: " + result.getExitCode());
        }
    }
}