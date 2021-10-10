package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.content.ArchiveRequest;
import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.hook.repository.RepositoryHookResult;
import com.atlassian.bitbucket.hook.repository.RepositoryMergeCheck;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component("isFormattedWithDotnetFormatMergeCheck")
public class IsFormattedWithDotnetFormatMergeCheck implements RepositoryMergeCheck {
    private final ContentService contentService;

    @Autowired
    public IsFormattedWithDotnetFormatMergeCheck(@ComponentImport ContentService contentService) {
        this.contentService = contentService;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(@Nonnull PreRepositoryHookContext context,
                                          @Nonnull PullRequestMergeHookRequest request) {
        try {
            Path archiveFilePath = Files.createTempFile("bb", ".zip");
            Path extractedArchiveDirectoryPath = Files.createTempDirectory("bb");

            downloadRepository(request.getRepository(), request.getFromRef().getLatestCommit(), archiveFilePath);
            extractArchive(archiveFilePath, extractedArchiveDirectoryPath);
            DotnetFormatCommandResult result = runDotnetFormat(extractedArchiveDirectoryPath);

            if (result.getExitCode() != 0) {
                return RepositoryHookResult.rejected("Dotnet format has found issues.", result.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return RepositoryHookResult.accepted();
    }

    private void extractArchive(Path archiveFilePath, Path extractedArchiveDirectoryPath) throws IOException {
        ZipFile zipFile = new ZipFile(archiveFilePath.toFile());
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            extractZipFile(extractedArchiveDirectoryPath, zipFile, entries.nextElement());
        }
    }

    private void extractZipFile(Path extractedArchiveDirectoryPath, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        File newFile = new File(extractedArchiveDirectoryPath.toString(), zipEntry.getName());
        if (zipEntry.isDirectory()) {
            newFile.mkdirs();
        } else {
            InputStream zipFileInputStream = zipFile.getInputStream(zipEntry);
            OutputStream newFileOutputStream = new FileOutputStream(newFile);
            copy(zipFileInputStream, newFileOutputStream);
            zipFileInputStream.close();
            newFileOutputStream.close();
        }
    }

    private void downloadRepository(Repository repository, String commitId, Path filePath) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile());
        contentService.streamArchive(
                new ArchiveRequest.Builder(repository, commitId).build(),
                fileType -> fileOutputStream);
        fileOutputStream.close();
    }

    private DotnetFormatCommandResult runDotnetFormat(Path workingDirectory) throws IOException, InterruptedException {
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
                .directory(workingDirectory.toFile())
                .start();

        StringBuffer messageBuffer = new StringBuffer();
        StreamGobbler inputStreamGobbler = new StreamGobbler(process.getInputStream(), s -> messageBuffer.append(s));
        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), s -> messageBuffer.append(s));
        Executors.newSingleThreadExecutor().submit(inputStreamGobbler);
        Executors.newSingleThreadExecutor().submit(errorStreamGobbler);
        int exitCode = process.waitFor();

        return new DotnetFormatCommandResult(exitCode, messageBuffer.toString());
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