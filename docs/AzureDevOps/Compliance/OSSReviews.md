# Azure DevOps Compliance

## Introduction

This document describes how Open Source Security Reviews are carried out by the Android Identity team and provides references towards the elements that compose it across Azure DevOps.

## References

- What is Component Governance? : [https://aka.ms/cgdocs](https://aka.ms/cgdocs)
- Governed repositories: [https://identitydivision.visualstudio.com/Engineering/_componentGovernance](https://identitydivision.visualstudio.com/Engineering/_componentGovernance)
  > **_NOTE:_**  It is important to remark that the azuread/android-complete contains the current state of our components. The other repositories are out of date and are only kept open to keep the history of previous component reviews.
- Component detection task: [https://docs.opensource.microsoft.com/tools/cg/features/buildtask/](https://docs.opensource.microsoft.com/tools/cg/features/buildtask/)
- Manage tracked branches/pipelines: [https://docs.opensource.microsoft.com/tools/cg/how-to/manage-tracking/](https://docs.opensource.microsoft.com/tools/cg/how-to/manage-tracking/)
- Locking dependency versions with Gradle: [https://docs.gradle.org/current/userguide/dependency_locking.html](https://docs.gradle.org/current/userguide/dependency_locking.html)
- Compliance Assessments: [https://identitydivision.visualstudio.com/Engineering/_compliance/product/all](https://identitydivision.visualstudio.com/Engineering/_compliance/product/all)
    - [AuthN SDK - Broker/MSAL Android](https://identitydivision.visualstudio.com/Engineering/_compliance/product/7c52141e-0d6d-c50a-576b-c4d81584bf01/assessments)
    - [AuthN SDK - ADAL Android](https://identitydivision.visualstudio.com/Engineering/_compliance/product/a514ff6b-8c3e-0d7e-38b9-4462ec60d7fa/assessments)
- Resolve Alerts:
    - Security & malware alerts [https://docs.opensource.microsoft.com/tools/cg/how-to/resolve-security-alerts/](https://docs.opensource.microsoft.com/tools/cg/how-to/resolve-security-alerts/)
    - Legal alerts [https://docs.opensource.microsoft.com/tools/cg/features/legal-alerts/](https://docs.opensource.microsoft.com/tools/cg/features/legal-alerts/)
    

## Open Source Security Review

OSS reviews are part of the compliance assessment and SDL Compliance, to be compliant, we must do the following:
- Have Component Governance being executed in our builds
- Ensure notifications are enabled to receive alerts about security vulnerabilities in our open source component/s
- Resolve all identified security vulnerabilities

## Component Governance Task

The component detection task runs daily as part of the continuous delivery pipeline [auth-client-android-dev](https://identitydivision.visualstudio.com/Engineering/_build?definitionId=1515).

> This pipeline is in charge of listing our dependencies, builds our libraries, and publishes our dev libraries.

In this pipeline, we can find a _Generate Lockfile_ task for each stage, this task locks versions of dependencies and transitive dependencies in a file named gradle.lockfile, then in a subsequent step, the task _ComponentGovernanceComponentDetection_ collects the components from the gradle.lockfile and sends the results up to the Component Governance service for registration. Which can be observed in the governed repositories.

## Governance Repositories

Android Identity governed repositories are found in the [Engineering project](https://identitydivision.visualstudio.com/Engineering/) in Azure DevOps, and the repositories are the following:

- azuread/ad-accounts-for-android
- azuread/android-complete
- azuread/azure-activedirectory-library-for-android
- azuread/microsoft-authentication-library-common-for-android
- azuread/microsoft-authentication-library-for-android

The [azuread/android-complete repository](https://identitydivision.visualstudio.com/Engineering/_componentGovernance/181889?_a=alerts&typeId=9846578&alerts-view-option=active) is the only repository we maintain that keeps track of the approval status of OSS components and has an up-to-date list of the consumed components for all our projects (msal, adal, common, common4j, broker, broker4j, and Linux broker).
