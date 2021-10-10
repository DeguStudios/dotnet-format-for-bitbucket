package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.content.ArchiveRequest;
import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CodeService {
    private final ContentService contentService;

    @Autowired
    public CodeService(@ComponentImport ContentService contentService) {
        this.contentService = contentService;
    }

    void downloadRepositoryCode(Path extractedArchiveDirectoryPath, Repository repository, String commitId) throws IOException {
        Path archiveFilePath = Files.createTempFile("archive", ".zip");
        downloadRepository(repository, commitId, archiveFilePath);
        extractArchive(archiveFilePath, extractedArchiveDirectoryPath);
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

    private void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }
}