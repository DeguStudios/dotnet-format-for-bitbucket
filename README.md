[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=DeguStudios_dotnet-format-for-bitbucket&metric=coverage)](https://sonarcloud.io/summary/new_code?id=DeguStudios_dotnet-format-for-bitbucket)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=DeguStudios_dotnet-format-for-bitbucket&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=DeguStudios_dotnet-format-for-bitbucket)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DeguStudios_dotnet-format-for-bitbucket&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DeguStudios_dotnet-format-for-bitbucket)

# dotnet-format for Bitbucket

Merge check plugin for bitbucket - ensures that pull requests have been formatted with dotnet-format.

## Getting started
### Installing
dotnet-format for Bitbucket uses [dotnet-format](https://github.com/dotnet/format) - make sure it's accessible for your Bitbucket server first!

In order to enjoy the plugin you first need to install it on your Bitbucket server

1. Download the Atlassian SDK (from [here](https://developer.atlassian.com/server/framework/atlassian-sdk/set-up-the-atlassian-plugin-sdk-and-build-a-project/))
2. Compile the plugin .jar file with `atlas-package`
3. Go to Bitbucket server's "Manage apps" page
4. Click on the "Upload app" link and you will get a prompt for a file upload
5. Upload the plugin .jar file you've just compiled