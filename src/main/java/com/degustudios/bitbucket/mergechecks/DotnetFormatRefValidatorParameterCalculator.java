package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.content.Change;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.scm.ChangesCommandParameters;
import com.atlassian.bitbucket.scm.ScmCommandFactory;
import com.atlassian.bitbucket.scm.ScmService;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DotnetFormatRefValidatorParameterCalculator {
    public static final String DOTNET_FORMAT_PARAMS = "dotnetFormatParams";
    public static final String SHOULD_USE_INCLUDE_PARAMETER = "shouldUseIncludeParameter";

    private final ScmService scmService;

    public DotnetFormatRefValidatorParameterCalculator(@ComponentImport ScmService scmService){
        this.scmService = scmService;
    }

    public List<String> calculateParameters(Settings settings, PullRequest pullRequest) {
        //TODO: handle empty change list, false for include parameter, tests
        String dotnetFormatParams = settings.getString(DOTNET_FORMAT_PARAMS);
        Collection<String> includePaths = getChanges(pullRequest);
        String includeParameter = "--include";

        List<String> allParameters = Stream.concat(
                Stream.concat(
                        Arrays.stream(dotnetFormatParams.split(" ")),
                        Collections.singletonList(includeParameter).stream()),
                includePaths.stream())
                .collect(Collectors.toList());
        return allParameters;
    }

    private Collection<String> getChanges(PullRequest request) {
        ScmCommandFactory commandFactory = scmService.getCommandFactory(request.getFromRef().getRepository());
        int pageSize = 10;
        Set<String> changes = new HashSet<>();
        Page<Change> query = null;

        do {
            PageRequest pageRequest = query == null
                    ? new PageRequestImpl(0, 10)
                    : query.getNextPageRequest().buildRestrictedPageRequest(pageSize);
            query = commandFactory.changes(
                    new ChangesCommandParameters.Builder()
                            .sinceId(request.getToRef().getId())
                            .untilId(request.getFromRef().getId())
                            .build(),
                    pageRequest).call();

            changes.addAll(query.stream().map(change -> change.getPath().toString()).collect(Collectors.toSet()));
        }
        while (!query.getIsLastPage());

        return changes;
    }
}
