# Firebase Test Lab & gcloud SDK

The Android Authencation Client SDK runs our E2E UI Automation on real devices hosted by the Firebase Test Lab. 

## References

- [Firebase Test Lab]([Firebase Test Lab](https://firebase.google.com/docs/test-lab))

- [gcloud SDK](https://cloud.google.com/sdk)

- [gcloud Firebase Test Android](https://cloud.google.com/sdk/gcloud/reference/firebase/test/android/run)

- [gcloud Service Account Authentication](https://cloud.google.com/sdk/gcloud/reference/auth/activate-service-account)

- [gsutil](https://cloud.google.com/storage/docs/gsutil)

- [Azure DevOps Pipelines](https://docs.microsoft.com/en-us/azure/devops/pipelines/get-started/what-is-azure-pipelines?view=azure-devops#:~:text=Azure%20Pipelines%20automatically%20builds%20and,ship%20it%20to%20any%20target.)

- [Secure files for Azure Pipelines and TFS - Azure Pipelines | Microsoft Docs](https://docs.microsoft.com/en-us/azure/devops/pipelines/library/secure-files?view=azure-devops)

## Pipelines

The tests run on Firebase is executed by our Pipelines hosted on Azure DevOps. All of our UI Automation Pipelines are located here: [UI Automation Pipelines](https://dev.azure.com/IdentityDivision/IDDP/_build?definitionScope=%5CCI%5CAndroid%5CUI%20Automation)

### Here's how the pipelines work:

- Assembles the app APK that needs to be tested (for instance our MSAL Automation app)

- Assembles the test APK that contains the tests that need to be run

- Installs gcloud SDK on Azure Pipeline host machine

- Authenticate against the gloud SDK using a service account (more on this in a later section)

- Downloads any additonal files / APKs on the host machine that need to be used across the tests and need to be supplied as part of Test Run. For example, some of our tests involve the `BrokerHost` app so the pipeline will first download its APK (from another pipeline) to host machine that is currently executing the UI Automation Pipeline

- Execute gcloud command to start test run on Firebase. This command will supply all the required flags and argumented as appropriate for the pipeline or the tests that we intend to run in that pipeline. For a complete list of possibilities, please read the official docs: [gcloud firebase test android run - Cloud SDK Documentation](https://cloud.google.com/sdk/gcloud/reference/firebase/test/android/run)

- Download the test result XML from gcloud Storage onto the Azure Pipeline Host Machine. All the test results are stored in raw format in a google cloud storage bucket. We use the [`gsutil`](https://cloud.google.com/storage/docs/gsutil) command to download the file from there and use it to create the test result in Azure DevOps Pipeline.

### Authenticate with gcloud using Service Account

We use a service account to authenticate with gcloud. We can read more on this here: [gcloud auth activate-service-account - Cloud SDK Documentation](https://cloud.google.com/sdk/gcloud/reference/auth/activate-service-account)

The service account can be setup by logging in into your Google Cloud Account and creating a service account. After that, you will receive a json file that is essentially the key for this account and allows to authenticate as a service as opposed to a human being i.e. we don't need username / password when using a service account. Our service account is stored in Azure DevOps using ([Secure files for Azure Pipelines and TFS - Azure Pipelines | Microsoft Docs](https://docs.microsoft.com/en-us/azure/devops/pipelines/library/secure-files?view=azure-devops)).

## Viewing Test Results in Firebase

We are going to need login credentials for our Google / Firebase credentials to view the data in there. Those credentials can be obtained via a `KeyVault`.

### (Skip this if you've already done this) How to get access to the KeyVault where Client Secret is stored:

1. Go to myaccess/

2. Click on request access

3. Request permissions from one
   of the following groups:

4. If a member of
   the Identity org, request read-write permissions from tm-msidlabs-int

5. If outside the Identity org,
   request read access to TM-MSIDLABS-DevKV

6. You can also reach out to msidlabint@microsoft.com to ask them for
   a rushed approval

7. After access has been
   approved, wait for 2-24 hours for changes to be effective.

### How to get Firebase username and password:

1. Go to Azure Portal: [Microsoft Azure](https://portal.azure.com/) and login with MS
   credentials
2. Switch to the Microsoft
   directory (if not already there)
3. Search for the KeyVault named
   "AdalTestInfo" (be sure to select "all" for the subcription,
   location etc filters)
4. Click into the AdalTestInfo keyvault (if you don't see
   this KeyVault, make sure you follow the above steps to get access to the
   KeyVault)
5. Under settings, click on
   secrets
6. Grab the AndroidFirebaseUsername - this is username of the
   Google account
7. Grab the AndroidFirebasePassword - this is password of the
   Google account
8. Now you can use these
   credentials to login into our Google account which would allow us to our
   Firebase as well as Google cloud portal.

### Firebase Google Account Login Issue

1. When logging in from a new
   device/location, Google may ask to verify identity.

2. There may be two options:

3. Verify with phone

4. Verify with recovery email

5. Please select verify with recovery email. The recovery email is already put in Google account and it is following the email: [droididautomation@microsoft.com](mailto:droididautomation@microsoft.com)

6. If not done already, please join the [droididautomation@microsoft.com](mailto:droididautomation@microsoft.com) Distribution List so you can
   receive the confirmation code that arrives on that email alias.
