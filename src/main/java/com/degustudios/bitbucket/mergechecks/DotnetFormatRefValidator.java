package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.repository.RepositoryRef;
import com.degustudios.dotnetformat.DotnetFormatCommandResult;

import java.util.List;

public interface DotnetFormatRefValidator {
    DotnetFormatCommandResult validate(RepositoryRef ref, List<String> params);
}
