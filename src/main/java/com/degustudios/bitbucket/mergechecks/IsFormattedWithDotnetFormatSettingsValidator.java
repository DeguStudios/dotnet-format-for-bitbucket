package com.degustudios.bitbucket.mergechecks;

import com.atlassian.bitbucket.scope.Scope;
import com.atlassian.bitbucket.setting.Settings;
import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.bitbucket.setting.SettingsValidator;
import javax.annotation.Nonnull;

public class IsFormattedWithDotnetFormatSettingsValidator implements SettingsValidator {
    @Override
    public void validate(@Nonnull Settings settings, @Nonnull SettingsValidationErrors errors, @Nonnull Scope scope) {

    }
}