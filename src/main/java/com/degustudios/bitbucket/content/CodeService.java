package com.degustudios.bitbucket.content;

import com.atlassian.bitbucket.content.ArchiveRequest;
import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class CodeService {
    private final ContentService contentService;

    @Autowired
    public CodeService(@ComponentImport ContentService contentService) {
        this.contentService = contentService;
    }

    public boolean tryDownloadRepositoryCode(Path extractedArchiveDirectoryPath, Repository repository, String commitId) {
        Path archiveFilePath = null;
        try {
            archiveFilePath = Files.createTempFile("archive", ".zip");
            downloadRepositoryCode(
                    extractedArchiveDirectoryPath,
                    archiveFilePath,
                    repository,
                    commitId);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            cleanUp(archiveFilePath);
        }
    }

    private void cleanUp(Path archiveFilePath) {
        if (archiveFilePath != null && Files.exists(archiveFilePath)) {
            try {
                Files.delete(archiveFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void downloadRepositoryCode(Path extractedArchiveDirectoryPath, Path archiveFilePath, Repository repository, String commitId) throws IOException {
        downloadRepository(repository, commitId, archiveFilePath);
        extractArchive(archiveFilePath, extractedArchiveDirectoryPath);
    }

    private void extractArchive(Path archiveFilePath, Path extractedArchiveDirectoryPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(archiveFilePath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                extractZipFile(extractedArchiveDirectoryPath, zipFile, entries.nextElement());
            }
        }
    }

    private void extractZipFile(Path extractedArchiveDirectoryPath, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        File newFile = new File(extractedArchiveDirectoryPath.toString(), zipEntry.getName());
        if (zipEntry.isDirectory()) {
            newFile.mkdirs();
        } else {
            try (InputStream zipFileInputStream = zipFile.getInputStream(zipEntry)) {
                try (OutputStream newFileOutputStream = new FileOutputStream(newFile)) {
                    copy(zipFileInputStream, newFileOutputStream);
                }
            }
        }
    }

    private void downloadRepository(Repository repository, String commitId, Path filePath) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile())) {
            contentService.streamArchive(
                    new ArchiveRequest.Builder(repository, commitId).build(),
                    fileType -> fileOutputStream);
        }
    }

    private void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }
}