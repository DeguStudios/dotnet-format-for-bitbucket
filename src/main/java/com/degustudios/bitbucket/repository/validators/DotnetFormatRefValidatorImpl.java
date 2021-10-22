package com.degustudios.bitbucket.repository.validators;

import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.bitbucket.content.CodeService;
import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidator;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;
import com.degustudios.dotnetformat.DotnetFormatRunner;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service("DotnetFormatRefValidatorImpl")
public class DotnetFormatRefValidatorImpl implements DotnetFormatRefValidator {
    private static final Logger logger = LoggerFactory.getLogger(DotnetFormatRefValidatorImpl.class);
    private final CodeService codeService;
    private final DotnetFormatRunner dotnetFormatRunner;

    @Autowired
    public DotnetFormatRefValidatorImpl(CodeService codeService, DotnetFormatRunner dotnetFormatRunner) {
        this.codeService = codeService;
        this.dotnetFormatRunner = dotnetFormatRunner;
    }

    @Override
    public DotnetFormatCommandResult validate(RepositoryRef ref) {
        Path codebaseDirectoryPath = null;
        try {
            codebaseDirectoryPath = Files.createTempDirectory("bb");
            return runDotnetFormat(ref, codebaseDirectoryPath);
        } catch (IOException e) {
            logger.error("Exception for repositoryRef: {}", ref, e);
            return DotnetFormatCommandResult.executedCorrectly(-1, e.getMessage());
        } finally {
            cleanUp(codebaseDirectoryPath);
        }
    }

    private void cleanUp(Path codebaseDirectoryPath) {
        if (codebaseDirectoryPath != null && Files.exists(codebaseDirectoryPath)) {
            try {
                FileUtils.deleteDirectory(new File(codebaseDirectoryPath.toString()));
            } catch (IOException e) {
                logger.error("Exception for codebaseDirectoryPath: {}", codebaseDirectoryPath, e);
            }
        }
    }

    private DotnetFormatCommandResult runDotnetFormat(RepositoryRef ref, Path codebaseDirectoryPath) {
        if (codeService.tryDownloadRepositoryCode(
                codebaseDirectoryPath,
                ref.getRepository(),
                ref.getLatestCommit())) {
            return dotnetFormatRunner.runDotnetFormat(codebaseDirectoryPath);
        } else {
            return DotnetFormatCommandResult.failed("Downloading code failed. Check log file for more information.");
        }
    }
}