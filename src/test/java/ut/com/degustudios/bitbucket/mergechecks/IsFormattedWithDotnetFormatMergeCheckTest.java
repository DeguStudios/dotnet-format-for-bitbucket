package ut.com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.content.ContentService;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.*;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.permission.PermissionService;
import com.degustudios.bitbucket.mergechecks.IsFormattedWithDotnetFormatMergeCheck;
import com.atlassian.bitbucket.setting.Settings;


@RunWith (MockitoJUnitRunner.class)
public class IsFormattedWithDotnetFormatMergeCheckTest
{
    @Mock
    private ContentService contentService;
    @Mock
    private PreRepositoryHookContext context;
    @Mock
    private PullRequestMergeHookRequest request;

    private IsFormattedWithDotnetFormatMergeCheck checker;

    @Test
    public void canCreateInstance()
    {
        new IsFormattedWithDotnetFormatMergeCheck(contentService);
    }
}