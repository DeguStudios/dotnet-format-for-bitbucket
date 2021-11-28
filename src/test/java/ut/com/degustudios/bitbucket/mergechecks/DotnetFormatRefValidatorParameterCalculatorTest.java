package ut.com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.content.Change;
import com.atlassian.bitbucket.content.Path;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.*;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageImpl;
import com.atlassian.bitbucket.util.PageRequest;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidatorParameterCalculator;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DotnetFormatRefValidatorParameterCalculatorTest {
    @Mock
    private ScmService scmService;
    @Mock
    private PullRequest pullRequest;
    @Mock
    private Settings settings;
    @Mock
    private Repository repository;
    @Mock
    private PullRequestRef pullRequestFromRef;
    @Mock
    private PullRequestRef pullRequestToRef;
    @Mock
    private ScmCommandFactory commandFactory;

    private Matcher<ChangesCommandParameters> correctSearchRequest;

    private DotnetFormatRefValidatorParameterCalculator calculator;

    @Before
    public void initialize() {
        when(pullRequestFromRef.getId()).thenReturn("11111");
        when(pullRequestToRef.getId()).thenReturn("22222");
        when(pullRequest.getFromRef()).thenReturn(pullRequestFromRef);
        when(pullRequest.getToRef()).thenReturn(pullRequestToRef);
        when(pullRequestFromRef.getRepository()).thenReturn(repository);
        when(pullRequestFromRef.getRepository()).thenReturn(repository);
        when(scmService.getCommandFactory(repository)).thenReturn(commandFactory);

        correctSearchRequest = new ArgumentMatcher<ChangesCommandParameters>() {
            @Override
            public boolean matches(Object o) {
                ChangesCommandParameters searchRequest = (ChangesCommandParameters) o;
                return searchRequest.getSinceId().equals(pullRequestToRef.getId())
                        && searchRequest.getUntilId().equals(pullRequestFromRef.getId());
            }
        };

        calculator = new DotnetFormatRefValidatorParameterCalculator(scmService);
    }

    @Test
    public void whenDotNetFormatParametersAreNullCalculatorReturnsEmptyList() {
        setupIncludeParameterTo(false);

        List<String> result = calculator.calculateParameters(settings, pullRequest);

        assertThat(result.size(), is(0));
    }

    @Test
    public void whenUseIncludeParameterIsFalseCalculatorReturnsOnlyDotnetFormatParameters() {
        String dotNetFormatSingleParameter = "params";
        setupDotNetFormatParameterTo(dotNetFormatSingleParameter);
        setupIncludeParameterTo(false);

        List<String> result = calculator.calculateParameters(settings, pullRequest);

        assertThat(result.size(), is(1));
        assertThat(result, hasItem(dotNetFormatSingleParameter));
    }

    @Test
    public void whenUseIncludeParameterIsTrueCalculatorAddsIncludeParametersToReturnList() {
        String dotNetFormatSingleParameter = "params";
        String[] changedPaths = new String[]{ "/code.cs", "/img/code.png"};
        setupDotNetFormatParameterTo(dotNetFormatSingleParameter);
        setupIncludeParameterTo(true);
        setupSearchToReturn(convertPathsToChanges(changedPaths));

        List<String> result = calculator.calculateParameters(settings, pullRequest);

        assertThat(result.size(), is(4));
        assertThat(result.get(0), is(dotNetFormatSingleParameter));
        assertThat(result.get(1), is("--include"));
        assertThat(result.stream().skip(2).collect(Collectors.toList()), hasItems(changedPaths));
    }

    @Test
    public void calculatorCanHandleManyPagesOfChanges() {
        String dotNetFormatSingleParameter = "params";
        int changesCount = 1000;
        String[] changedPaths = IntStream.range(0, changesCount).mapToObj(i -> "/src/" + i + ".cs").toArray(String[]::new);
        setupDotNetFormatParameterTo(dotNetFormatSingleParameter);
        setupIncludeParameterTo(true);
        setupSearchToReturn(convertPathsToChanges(changedPaths));

        List<String> result = calculator.calculateParameters(settings, pullRequest);

        assertThat(result.size(), is(changesCount+2));
        assertThat(result.get(0), is(dotNetFormatSingleParameter));
        assertThat(result.get(1), is("--include"));
        assertThat(result.stream().skip(2).collect(Collectors.toList()), hasItems(changedPaths));
    }

    @Test
    public void whenUseIncludeParameterIsTrueButThereAreNoChangesCalculatorReturnsOnlyDotNetParametersList() {
        String dotNetFormatSingleParameter = "params";
        setupDotNetFormatParameterTo(dotNetFormatSingleParameter);
        setupIncludeParameterTo(true);
        setupSearchToReturn(convertPathsToChanges(new String[0]));

        List<String> result = calculator.calculateParameters(settings, pullRequest);

        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(dotNetFormatSingleParameter));
    }

    @Test
    public void dotNetFormatParametersAreCorrectlySplitByWhiteSpaces() {
        String dotNetFormatParameters = "A B C D E";
        setupDotNetFormatParameterTo(dotNetFormatParameters);
        setupIncludeParameterTo(false);

        List<String> result = calculator.calculateParameters(settings, pullRequest);

        assertThat(result.toArray(), is(dotNetFormatParameters.split(" ")));
    }

    @Test
    public void dotNetFormatParametersAreNotSplitByWhiteSpacesWhenWithinDoubleQuotes() {
        String dotNetFormatSingleParameter = "\"A B C D E\"";
        setupDotNetFormatParameterTo(dotNetFormatSingleParameter);
        setupIncludeParameterTo(false);

        List<String> result = calculator.calculateParameters(settings, pullRequest);

        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(dotNetFormatSingleParameter));
    }

    private Collection<Change> convertPathsToChanges(String[] changedPaths) {
        return Arrays.stream(changedPaths).map(s -> {
            Change change = mock(Change.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn(s);
            when(change.getPath()).thenReturn(path);
            return change;
        }).collect(Collectors.toList());
    }

    private void setupSearchToReturn(Collection<Change> elements) {
        when(commandFactory
                .changes(argThat(correctSearchRequest), notNull(PageRequest.class)))
                .thenAnswer(invocationOnMock -> {
                            PageRequest request = (PageRequest) invocationOnMock.getArguments()[1];
                            return new SimpleCommand<Page<Change>>() {
                                @Override
                                public Page<Change> call() {
                                    return new PageImpl<>(
                                            request,
                                            elements
                                                .stream()
                                                .skip(request.getStart())
                                                .limit(request.getLimit())
                                                .collect(Collectors.toList()),
                                    elements.size() <= request.getStart() + request.getLimit());
                                }
                            };
                        }
                );
    }

    private void setupIncludeParameterTo(boolean b) {
        when(settings.getBoolean(DotnetFormatRefValidatorParameterCalculator.SHOULD_USE_INCLUDE_PARAMETER))
                .thenReturn(b);
        when(settings.getBoolean(DotnetFormatRefValidatorParameterCalculator.SHOULD_USE_INCLUDE_PARAMETER, false))
                .thenReturn(b);
    }

    private void setupDotNetFormatParameterTo(String dotNetFormatSingleParameter) {
        when(settings.getString(DotnetFormatRefValidatorParameterCalculator.DOTNET_FORMAT_PARAMS))
                .thenReturn(dotNetFormatSingleParameter);
    }
}
