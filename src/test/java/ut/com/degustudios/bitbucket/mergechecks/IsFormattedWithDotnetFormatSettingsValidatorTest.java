package ut.com.degustudios.bitbucket.mergechecks;

import com.degustudios.bitbucket.mergechecks.DotnetFormatRefValidatorParameterCalculator;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import com.atlassian.bitbucket.setting.SettingsValidationErrors;
import com.atlassian.bitbucket.scope.Scope;
import com.degustudios.bitbucket.mergechecks.IsFormattedWithDotnetFormatSettingsValidator;
import com.atlassian.bitbucket.setting.Settings;


@RunWith (MockitoJUnitRunner.class)
public class IsFormattedWithDotnetFormatSettingsValidatorTest
{
    private static final String AdditionalParamsSettingKey = "dotnetFormatParams";

    @Mock
    private Settings settings;
    @Mock
    private SettingsValidationErrors errors;
    @Mock
    private Scope scope;

    private IsFormattedWithDotnetFormatSettingsValidator validator;

    @Before 
    public void initialize() {
        validator = new IsFormattedWithDotnetFormatSettingsValidator();
    }

    @Test
    public void additionalParamsValueIsNotRequired()
    {
        setAdditionalParamsSettingTo("--check");

        run();

        verifyNoErrors();
    }

    @Test
    public void additionalParamsValueIsNotValidated()
    {
        setAdditionalParamsSettingTo(null);

        run();

        verifyNoErrors();
    }

    @Test
    public void useIncludeParamValueIsNotRequired()
    {
        setUseIncludeParamSettingTo(true);

        run();

        verifyNoErrors();
    }

    @Test
    public void useIncludeParamValueIsNotValidated()
    {
        setUseIncludeParamSettingTo(null);

        run();

        verifyNoErrors();
    }

    private void setAdditionalParamsSettingTo(String value) {
        when(settings.getString(DotnetFormatRefValidatorParameterCalculator.DOTNET_FORMAT_PARAMS)).thenReturn(value);
    }

    private void setUseIncludeParamSettingTo(Boolean value) {
        when(settings.getBoolean(DotnetFormatRefValidatorParameterCalculator.SHOULD_USE_INCLUDE_PARAMETER)).thenReturn(value);
    }

    private void run() {
        validator.validate(settings, errors, scope);
    }

    private void verifyNoErrors() {
        verify(errors, never()).addFieldError("", "");
    }
}