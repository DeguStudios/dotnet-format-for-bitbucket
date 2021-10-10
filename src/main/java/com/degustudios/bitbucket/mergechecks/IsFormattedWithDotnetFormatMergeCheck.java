package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.content.ArchiveRequest;
import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.*;
import java.util.zip.ZipFile;

import javax.annotation.Nonnull;

@Component("isFormattedWithDotnetFormatMergeCheck")
public class IsFormattedWithDotnetFormatMergeCheck implements RepositoryMergeCheck {
    private final I18nService i18nService;
    private final ContentService contentService;

    @Autowired
    public IsFormattedWithDotnetFormatMergeCheck(@ComponentImport I18nService i18nService,
                                                 @ComponentImport ContentService contentService) {
        this.i18nService = i18nService;
        this.contentService = contentService;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        try {
            Path tempFilePath = Files.createTempFile("bb", ".zip");
            FileOutputStream fileOutputStream = new FileOutputStream(tempFilePath.toFile());
            contentService.streamArchive(
                    new ArchiveRequest.Builder(request.getRepository(), request.getFromRef().getLatestCommit()).build(),
                    fileType -> fileOutputStream);
            fileOutputStream.close();

            Path tempDir = Files.createTempDirectory("bb");
            ZipFile zipFile = new ZipFile(tempFilePath.toFile());
            zipFile.stream().forEach(zipEntry -> {
                File newFile = new File(tempDir.toString(), zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                }
                else {
                    try {
                        InputStream zipFileInputStream = zipFile.getInputStream(zipEntry);
                        OutputStream newFileOutputStream = new FileOutputStream(newFile);
                        copy(zipFileInputStream, newFileOutputStream);
                        zipFileInputStream.close();
                        newFileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });


            String shell = "";
            String executeSwitch = "";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                shell = "cmd.exe";
                executeSwitch = "/c";
            } else {
                shell = "sh";
                executeSwitch = "-c";
            }
            ProcessBuilder builder = new ProcessBuilder();
            Process process = builder
                    .command(shell, executeSwitch, "dotnet format", "--check")
                    .directory(tempDir.toFile())
                    .start();
            StringBuffer stringBuffer = new StringBuffer();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), s -> stringBuffer.append(s));
            StreamGobbler streamGobbler2 = new StreamGobbler(process.getErrorStream(), s -> stringBuffer.append(s));
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            Executors.newSingleThreadExecutor().submit(streamGobbler2);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                return RepositoryHookResult.rejected("Dotnet format has found issues.", stringBuffer.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return RepositoryHookResult.accepted();
    }

    private void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .forEach(consumer);
        }
    }
}