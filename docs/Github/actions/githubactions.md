# Github Actions

## Introduction

Android Identity uses Travis, Github Actions and Azure Devops to perform builds.  This document describes how we use github actions and provides examples and references for how github actions can be used to perform common tasks.  

## References

- [Contexts & Expression Syntax](https://docs.github.com/en/free-pro-team@latest/actions/reference/context-and-expression-syntax-for-github-actions)
- [Authentication in GitHub Actions](https://docs.github.com/en/free-pro-team@latest/actions/reference/authentication-in-a-workflow)
- [GitHub App Token Permissions](https://docs.github.com/en/free-pro-team@latest/actions/reference/authentication-in-a-workflow#permissions-for-the-github_token)
- [GitHub Encrypted Secrets](https://docs.github.com/en/free-pro-team@latest/actions/reference/encrypted-secrets)
- [Creating a Personal Access Token](https://docs.github.com/en/free-pro-team@latest/github/authenticating-to-github/creating-a-personal-access-token)
- [Azure DevOps Personal Access Token](https://docs.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=azure-devops&tabs=preview-page)

### Actions

The following are used in github actions in use with Android Identity projects:

- https://github.com/actions/checkout
- https://github.com/actions/setup-java
- https://github.com/eskatos/gradle-command-action
- https://github.com/dangoslen/changelog-enforcer@v1.4.0


# Workflows

GitHub actions are workflows or if you prefer a set of jobs each with an associated list of tasks.  These jobs can be configured to run on specific platforms but typically run on Linux.

## Context

Each GitHub workflow has a context via which you can access / set environment variables for use throughout your build.  


For example:

```yaml

env:
  FIRST_NAME: Mona
  middle_name: The
  Last_Name: Octocat

# environment variables are access by prefixing with a single dollar sign
run:
    $FIRST_NAME + 'was something'

# the context can be accessed using the following syntax
${{ context }}
```
Context objects: [https://docs.github.com/en/free-pro-team@latest/actions/reference/context-and-expression-syntax-for-github-actions#contexts](https://docs.github.com/en/free-pro-team@latest/actions/reference/context-and-expression-syntax-for-github-actions#contexts)

```yaml

# Here's an example of getting the github pat
${{ secrets.ACTION_PAT }}

```

## Jobs

Jobs are a grouping of tasks.  Jobs by default run in parallel, but can be coordinated to run sequentially (or in an order of your preference) by establishing job depdencies via the "needs:" yaml entry.  Multiple dependencies can be specified with json list ("[a, b]") syntax.


For example:

```yaml
jobs:
  incrementLatestPatch:

  build:
    # Make this workflow sequential
    needs: incrementLatestPatch
```

In this case build only fires after increment has completed successfully.

## Steps

Steps consists of one or more actions and or commands that you can run.

## Actions

Acions are re-useable pre-built components that can perform specific functions within your worklow.  In our case we use actions that do things like:

- Checkout the code
- Build the code using Gradle
- Etc...

# Authentication

In order for github to perform operations within Github and/or with Travis and/or Azure DevOps you need to configure access tokens to be able to access these resources.

## Secrets

We currently have the following secrets configured within the common repo:

[https://github.com/AzureAD/microsoft-authentication-library-common-for-android/settings/secrets/actions](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/settings/secrets/actions)

### ACTION_PAT

This is a GitHub Personal Access Token (PAT) based on the [shoatman@microsoft.com](mailto:shoatman@microsoft.com) account.  The PAT is configured with the following GitHub OAuth2 permissions: 
- `public_repo`
- `read:user`
- `repo:status`
- `user:email`

> NOTE: We need to enable SSO by clicking the enable SSO button.  I noticed that on first use of the token within a Github action it was necessary to do this again using an error link in the output from the first run.  After that the token worked as expected.  So I'm assuming that the button didn't really do what I was hoping it would.

### IDDP_PIPELINE

This is an Azure DevOps Personal Access Token based on the [shoatman@microsoft.com](mailto:shoatman@microsoft.com) account.  The PAT is configured with the following GitHub OAuth2 permissions: 

- [Build Scopes](https://docs.microsoft.com/en-us/azure/devops/integrate/get-started/authentication/oauth?view=azure-devops#scopes):
- `vso.build`
- `vso.build_execute`



