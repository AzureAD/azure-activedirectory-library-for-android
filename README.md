
# Introduction

This repository contains a build gradle and git alias commands for building ADAL, MSAL, Authentication Broker, Common and test apps.  This project is intended for use by developers building and verifying integration primarily between ADAL, MSAL and the Android Authentication Broker.

## Pre-requisites

The android related auth projects pull artifacts from public and private package repositories.  The private artifacts are published using Azure DevOps.  You will need to generate
and store the credentials for the Identity and Aria azure devops instances.

- [Aria](https://msasg.visualstudio.com/Shared%20Data/_packaging?_a=package&feed=ARIA-SDK&package=com.microsoft.applications%3Aariaandroidjavasdk-release&version=3.0.22.0&protocolType=maven)
- [Identity](https://identitydivision.visualstudio.com/DevEx/_packaging?_a=feed&feed=AndroidADAL)

In each case you'll need to:

1. Click the "Connect to feed" button.  
2. Then select gradle.  
3. Then click the generate credentials button

Then add the following to your gradle properties (in your user folder on windows in the .gradle folder.  You may need to create this file: gradle.properties.  Learn more about gradle configuration properties [here](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties)) file replacing with the token values from the generate credentials UI:

```gradle.properties
vstsUsername=VSTS 
vstsGradleAccessToken=<InsertIdentityAccessTokenHere>
vstsMavenAccessToken=<InsertIdentityAccessTokenHere>
vstsAriaGradleAccessToken=<InsertAriaAccessTokenHere>
```
>NOTE: The sample configuration produced by Azure DevOps change when the service was renamed from Visual Studio Online to Azure DevOps... the vstsUsername VSTS is still accepted.  

## Install

1. Clone the repo
2. Run the following commands from within the repo to register the custom aliases and initiate the clone and setup for the Android projects/repositories

```bash
# Include the .gitconfig file included with project to your local gitconfig
git config --local include.path ../.gitconfig
# Run this newly minted command to clone each repo as a subfolder
git droidSetup
```

## Usage - Custom git commands

Running droidSetup will clone ADAL, MSAL, Broker (AD Accounts) and Common into sub-folders.  Each of these folders is a separate git repo.
In order to help ease the management of changes to those repos the following custom git commands are provided for your convenience.  Please feel free to propose
additional commands and/or changes to this initial set.

A typical flow would include:

```bash
# Create a new feature branch in each repo
git droidNewFeature githubid-newfeature

# Make the changes for your feature/change

# Check status
git droidStatus

# If changes to common were made
# Push changes to common then run droidUpdateCommon
git droidUpdateCommon

# Push changes made to other repos

# On Github create PRs to integrate the feature branches

```
> NOTE: Open to adding support for droidPush and for opening PRs from the command line.

### droidUpdateCommon

This build places a shared common repo at the root of the global project.  In order to ensure that your checkin builds for ADAL, MSAL and broker are updated with the correct sub-module pointer the following command is provided to update the sub-modules to the matching revision.

```bat
git droidUpdateCommon
```

>NOTE: Your changes to common need to be committed and pushed to github in order for the sub-module update to succeed.

### droidStatus

Outputs the git status for each of the repos under the project

```bat
git droidStatus
```

### droidNewFeature

Creates a new feature with the specified name in each of the repositories

```bat
git droidNewFeature <nameofnewfeaturebranch>
```

### droidCheckout

Attempts to check out the specified branch in each repo

```bat
git droidCheckout <nameofbranchtocheckout>
```

### droidPull

Pulls changes from origin to local for each repository

```bat
git droidPull
```

### droidStash

Runs stash on each of the repositories...

```bat
git droidStash
git droidStash apply
git droidStash clear
```

# Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.microsoft.com.

When you submit a pull request, a CLA-bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., label, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
