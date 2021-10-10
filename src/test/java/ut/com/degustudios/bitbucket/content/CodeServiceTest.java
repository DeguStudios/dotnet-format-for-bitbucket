package ut.com.degustudios.bitbucket.content;

import com.atlassian.bitbucket.content.ArchiveRequest;
import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.io.TypeAwareOutputSupplier;
import com.atlassian.bitbucket.repository.Repository;
import com.degustudios.bitbucket.content.CodeService;
import com.degustudios.bitbucket.mergechecks.IsFormattedWithDotnetFormatMergeCheck;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CodeServiceTest {
    private final String commitId = "ref/master/1";

    @Mock
    private ContentService contentService;
    @Mock
    private Repository repository;

    private Path temporaryDirectory;

    @Before
    public void initialize() throws IOException {
        temporaryDirectory = Files.createTempDirectory("tests");
    }

    @After
    public void teardown() throws IOException {
        FileUtils.deleteDirectory(new File(temporaryDirectory.toString()));
    }

    @Test
    public void cleansUpTemporaryArchive() throws IOException {
        List<Path> itemsBefore = Files.list(temporaryDirectory.getParent()).collect(Collectors.toList());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                TypeAwareOutputSupplier supplier = (TypeAwareOutputSupplier) invocationOnMock.getArguments()[1];
                OutputStream outputStream = supplier.getStream("archive/zip");
                try (InputStream inputStream = CodeServiceTest.class.getResourceAsStream("archives/codebase.zip")) {
                    CodeServiceTest.this.copy(inputStream, outputStream);
                }
                return null;
            }
        })
                .when(contentService)
                .streamArchive(notNull(ArchiveRequest.class), notNull(TypeAwareOutputSupplier.class));

        CodeService codeService = new CodeService(contentService);

        codeService.tryDownloadRepositoryCode(
                temporaryDirectory,
                repository,
                commitId);


        List<Path> itemsAfter = Files.list(temporaryDirectory.getParent()).collect(Collectors.toList());
        assertThat(itemsAfter.size(), is(itemsBefore.size()));
    }

    void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }
}
