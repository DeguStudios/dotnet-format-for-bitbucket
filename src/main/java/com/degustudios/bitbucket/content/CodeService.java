package com.degustudios.bitbucket.content;

import com.atlassian.bitbucket.content.ArchiveRequest;
import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class CodeService {
    private static final Logger logger = LoggerFactory.getLogger(CodeService.class);
    private final ContentService contentService;
    private final SecurityService securityService;

    @Autowired
    public CodeService(@ComponentImport ContentService contentService, @ComponentImport SecurityService securityService) {
        this.contentService = contentService;
        this.securityService = securityService;
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
            logger.error("Failed to download repository code for: {}", commitId, e);
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
                logger.error("Failed to clean up temporary file {}", archiveFilePath, e);
            }
        }
    }

    private void downloadRepositoryCode(Path extractedArchiveDirectoryPath, Path archiveFilePath, Repository repository, String commitId) throws IOException {
        securityService.withPermission(Permission.REPO_READ, "download repository").call(() -> {
            downloadRepository(repository, commitId, archiveFilePath);
            return null;
        });
        extractArchive(archiveFilePath, extractedArchiveDirectoryPath);
    }

    private void extractArchive(Path archiveFilePath, Path extractedArchiveDirectoryPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(archiveFilePath.toFile())) {
            ArrayList<? extends ZipEntry> entries = Collections.list(zipFile.entries());

            for (ZipEntry x : entries) {
                if (!canBeExtracted(extractedArchiveDirectoryPath, x)) {
                    throw new IOException("ZipEntry is outside of the target directory");
                }
            }

            for (ZipEntry zipEntry : entries) {
                extractZipFile(extractedArchiveDirectoryPath, zipFile, zipEntry);
            }
        }
    }

    private boolean canBeExtracted(Path extractedArchiveDirectoryPath, ZipEntry zipEntry) throws IOException {
        File newFile = new File(extractedArchiveDirectoryPath.toString(), zipEntry.getName());
        return newFile.getCanonicalPath().startsWith(extractedArchiveDirectoryPath.toString());
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