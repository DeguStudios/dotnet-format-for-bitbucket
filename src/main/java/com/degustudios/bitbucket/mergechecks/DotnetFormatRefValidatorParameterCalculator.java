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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        Stream<String> dotNetFormatParametersStream = streamDotNetFormatParameters(settings);
        if (!settings.getBoolean(SHOULD_USE_INCLUDE_PARAMETER)) {
            return dotNetFormatParametersStream.collect(Collectors.toList());
        }

        Collection<String> includePaths = getChanges(pullRequest);
        if (includePaths.isEmpty()){
            return dotNetFormatParametersStream.collect(Collectors.toList());
        }

        String includeParameter = "--include";

        return Stream.concat(
                Stream.concat(
                        dotNetFormatParametersStream,
                        Collections.singletonList(includeParameter).stream()),
                includePaths.stream())
                .collect(Collectors.toList());
    }

    private Stream<String> streamDotNetFormatParameters(Settings settings) {
        String dotnetFormatParametersInlined = settings.getString(DOTNET_FORMAT_PARAMS);
        if (dotnetFormatParametersInlined == null) {
            dotnetFormatParametersInlined = "";
        }

        List<String> allMatches = new LinkedList<>();
        Matcher matcher = Pattern.compile("(\".*?\"|\\S+)").matcher(dotnetFormatParametersInlined);
        while (matcher.find()) {
            allMatches.add(matcher.group());
        }

        return allMatches.stream().filter(s -> !s.isEmpty());
    }

    private Collection<String> getChanges(PullRequest request) {
        ScmCommandFactory commandFactory = scmService.getCommandFactory(request.getFromRef().getRepository());
        int pageSize = 100;
        Set<String> changes = new HashSet<>();
        Page<Change> query = null;

        do {
            PageRequest pageRequest = query == null
                    ? new PageRequestImpl(0, pageSize)
                    : query.getNextPageRequest().buildRestrictedPageRequest(pageSize);
            query = commandFactory.changes(
                    new ChangesCommandParameters.Builder()
                            .sinceId(request.getToRef().getId())
                            .untilId(request.getFromRef().getId())
                            .build(),
                    pageRequest).call();

            changes.addAll(query
                    .stream()
                    .map(change -> change.getPath().toString())
                    .collect(Collectors.toSet()));
        }
        while (!query.getIsLastPage());

        return changes;
    }
}
