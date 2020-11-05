# Azure DevOps Upstream Sources

## Introduction

This document describes how upstream sources are used by the Android Identity team to simplify access management across Azure DevOps Artifacts Feeds.  

## References

- What are feeds? : [https://docs.microsoft.com/en-us/azure/devops/artifacts/concepts/feeds?view=azure-devops](https://docs.microsoft.com/en-us/azure/devops/artifacts/concepts/feeds?view=azure-devops)
- Upstream Sources: [https://docs.microsoft.com/en-us/azure/devops/artifacts/concepts/upstream-sources?view=azure-devops](https://docs.microsoft.com/en-us/azure/devops/artifacts/concepts/upstream-sources?view=azure-devops)
- Configure upstream sources: [https://docs.microsoft.com/en-us/azure/devops/artifacts/how-to/set-up-upstream-sources?view=azure-devops](https://docs.microsoft.com/en-us/azure/devops/artifacts/how-to/set-up-upstream-sources?view=azure-devops)
- OSS Packages w/ Upstream Sources: [https://docs.microsoft.com/en-us/azure/devops/artifacts/tutorials/protect-oss-packages-with-upstream-sources?view=azure-devops&tabs=npm](https://docs.microsoft.com/en-us/azure/devops/artifacts/tutorials/protect-oss-packages-with-upstream-sources?view=azure-devops&tabs=npm)
- Feed Permissions: [https://docs.microsoft.com/en-us/azure/devops/artifacts/feeds/feed-permissions?view=azure-devops](https://docs.microsoft.com/en-us/azure/devops/artifacts/feeds/feed-permissions?view=azure-devops)


# Android Identity Feeds

## Android Identity Core Team Feed

Android Identity publishes our packages to an internal feed called AndroidADAL in the IDDP (It may migrate in the future) project in Azure DevOps.

Feed: 
[https://docs.microsoft.com/en-us/azure/devops/artifacts/concepts/feeds?view=azure-devops](https://docs.microsoft.com/en-us/azure/devops/artifacts/concepts/feeds?view=azure-devops)

The following packages are published to this feed:

- ADAL Android
- MSAL Android
- Android Common
- AD Accounts (Broker & Workplace Join - This may be renamed in the future when this is expanded to support consumer identity)
- KeyVault (Generated java client code for keyvault)
- LabAPI (Generated java client code to invoking MSID Labs Test Data APIs)
- TestUtilities (Utilities supporting tests)
- UIAutomationUtilities (Utilities supporting UI Automation)

## Other Feeds

- OneAuthAndroid: [https://office.visualstudio.com/OneAuth/_packaging?_a=feed&feed=OneAuthAndroid](https://office.visualstudio.com/OneAuth/_packaging?_a=feed&feed=OneAuthAndroid)
  - The feed that OneAuth reads dependencies from
- AndroidOneDrive (TSL): [https://onedrive.visualstudio.com/SkyDrive/_packaging?_a=feed&feed=AndroidOneDrive%40Local](https://onedrive.visualstudio.com/SkyDrive/_packaging?_a=feed&
feed=AndroidOneDrive%40Local)
  - The feed to which the Android Token Sharing Library is published
- ARIA-SDK: [https://msasg.visualstudio.com/Aria/_packaging?_a=feed&feed=ARIA-SDK%40Local](https://msasg.visualstudio.com/Aria/_packaging?_a=feed&feed=ARIA-SDK%40Local)
- PowerLift: [https://office.visualstudio.com/OneAuth/_packaging?_a=feed&feed=PowerLift](https://office.visualstudio.com/OneAuth/_packaging?_a=feed&feed=PowerLift)
- Duo SDK: [https://dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging?_a=feed&feed=Duo-SDK-Feed%40Local](https://dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging?_a=feed&feed=Duo-SDK-Feed%40Local)
- Intune: [https://msazure.visualstudio.com/One/_packaging?_a=feed&feed=android-maven%40Local](https://msazure.visualstudio.com/One/_packaging?_a=feed&feed=android-maven%40Local)
- AuthApp: [https://msazure.visualstudio.com/One/_packaging?_a=feed&feed=AuthApp](https://msazure.visualstudio.com/One/_packaging?_a=feed&feed=AuthApp)

# Feed permissions

All Android Identity feeds are published to Azure DevOps Organizations that are currently associated with the same Azure Active Directory tenant.  Specifically the microsoft.com tenant.  This means that any feed can reference any other feed via a "feed locator" with no additional permission (all Microsoft employees typically have read access).  The feed locator is effectively a connection string that includes:

- The organization that publishes the feed
- The project (optional - our feeds were created before this feature was introduced by Azure DevOps)
- The feed name
- The feed view

## Feed locator values

- AndroidADAL: azure-feed://IdentityDivision/AndroidADAL@Local
- AndroidOneDrive: azure-feed://IdentityDivision/AndroidOneDrive@Local
- OneAuthAndroid: azure-feed://office/OneAuthAndroid@Local
- ARIS-SDK: azure-feed://msag/ARIA-SDK@Local
- PowerLift: azure-feed://office/PowerLift@Local
- DuoSDK: azure-feed://MicrosoftDeviceSDK/DuoSDK-Public/Duo-SDK-Feed@Local
- AuthApp: azure-feed://msazure/One/AuthApp@Local

## Feeds & Their Current Upstreams

### AndroidADAL Upstream Sources

- AndroidOneDrive: azure-feed://IdentityDivision/AndroidOneDrive@Local
- ARIS-SDK: azure-feed://msag/ARIA-SDK@Local
- PowerLift: azure-feed://office/PowerLift@Local
- DuoSDK: azure-feed://MicrosoftDeviceSDK/DuoSDK-Public/Duo-SDK-Feed@Local
- AuthApp: azure-feed://msazure/One/AuthApp@Local

### OneAuthAndroid Upstream Sources

- AndroidOneDrive: azure-feed://IdentityDivision/AndroidOneDrive@Local
- AndroidADAL: azure-feed://IdentityDivision/AndroidADAL@Local

# Gradle Root Project - Feed references

Each gradle root project references 1 azrue devops feed which in turn references other feeds as an upstream source.  This enables each gradle project to only require a single personal access token to be generated per project.  

## Android Identity Core:

Android-Complete (This project): Refers to the AndroidADAL feed as do:

- MSAL, ADAL, Common, AD Accounts, etc...

## OneAuth

OneAuth has multiple gradle root projects which refer to the OneAuthAndroid feed

# Upstream Sources Quirks

- Only packages that are tagged as release or pre-release are visible via upstream sources
- SNAPSHOT releases are not visibile via upstream sources
- Synchronization between a feed and an upstream feed occurs approximately every 3-6 hours, but is not under our control

# Upstream sources for open source projects public maven

You can use an Azure DevOps feeds to cache packages from publich maven repositories.  This provides caching for public maven feeds that may not be reliable. 

> Note: We have not currently configured this.

# Troubleshooting

## Package not found

This is the most likely error that you are to encounter.  When you encouter it's likely to be one of 2 things.

### Sychronization

It's possible when publishing a new package or a new version of a package that it may take some time to be available in your feed.  Recall that feed synchronization is not under our control and happens every 3-6 hours.  You can verify whether a package is available in your feed by going to the feed and searching for the package.  If not found you can open the feed settings, click on the "Upstream sources" tab and check when the source of the feed was last synchronized.

### Package not tagged

As mentioned previously if a package is not tagged as release or pre-release it will not be available via upstream sources.  Verify that the package in it's original feed is marked as release or pre-release.
