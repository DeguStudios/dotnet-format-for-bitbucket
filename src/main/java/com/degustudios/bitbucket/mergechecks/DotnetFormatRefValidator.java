package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;

public interface DotnetFormatRefValidator {
    DotnetFormatCommandResult validate(RepositoryRef ref);
}
