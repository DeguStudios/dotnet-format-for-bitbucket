# dotnet-format for Bitbucket

Merge check plugin for bitbucket - ensures that pull requests have been formatted with dotnet-format.

## Getting started
### Installing
In order to enjoy the plugin you first need to install it on your Bitbucket server

1. Download the Atlassian SDK (from [here](https://developer.atlassian.com/server/framework/atlassian-sdk/set-up-the-atlassian-plugin-sdk-and-build-a-project/))
2. Compile the plugin .jar file with `atlas-package`
3. Go to Bitbucket server's "Manage apps" page
4. Click on the "Upload app" link and you will get a prompt for a file upload
5. Upload the plugin .jar file you've just compiled