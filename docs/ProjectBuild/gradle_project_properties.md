# Gradle Project Properties as Command Line Arguments

Gradle supports supplying/setting project properties via command line arguments to a gradle task. 

## What are Gradle project properties?

Gradle project properties are just a way to configure and customize builds. Often times a build may need to built slightly differently from usual and/or it needs to be built with some specific flags or values supplied - project properties can help us configure this. You can read more about project properties here: [Gradle Project Properties](https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#property%28java.lang.String%29)

## How to supply Gradle Project properties as command line args?

Gradle project properties can be supplied as command line arguments by passing custom p params as follows:

`./gradlew <task> -P<property-name>=<value>`

## How to supply command line arguments when building from Android Studio?

Command Line arguments can also be supplied from within Android Studio as follows:

1. Go to File / Settings
2. Under Build, Execution, Deployment - > select Compiler
3. Command Line arguments can be supplied in the Command-line Options box.

![Android Studio Command Line Parameters](command_line_args.png "Android Studio Command Line Parameters")

## Which properties / arguments are supported across which of our android-complete modules?

We currently support the following gradle properties via command line across the following modules:

| Property               | Declaring Module                              | Description                                                  |
| ---------------------- | --------------------------------------------- | ------------------------------------------------------------ |
| labSecret              | testutils                                     | Enables access to [Lab Api](../Automation/labsetup.md) to get test accounts |
| brokerSource           | uiautomationutilities                         | Determines whether to install broker app (Authenticator, CP) from PlayStore vs a Local Apk file sideloaded |
| preferPreInstalledApks | uiautomationutilities                         | Determines if automation code should just use whatever app is already installed on the device (currently can be used for Auth app, CP, BrokerHost, AzureSample) |
| slice                  | common                                        | Send all ESTS requests to specific slice                     |
| dc                     | common                                        | Send all ESTS requests to specific DC                        |
| writeTestResultsToCsv  | testutils                                     | Determines whether to write results of tests to a CSV file (required for Kusto ingest by server validation pipeline) |
| robolectricSdkVersion  | msal, AADAuthenticator                        | Sets the target sdk version to the version that we want to use for Robolectric tests. |
| distAdalVersion        | adalTestApp, brokerautomationapp              | The version of ADAL that should be used while building the dist variant of an adal test app. |
| distCommonVersion      | adalTestApp, brokerautomationapp              | The version of Common that should be used while building the dist variant of an adal test app. |
| distMsalVersion        | AzureSample, MSAL Test App, msalautomationapp | The version of MSAL that should be used while building the dist variant of an msal test app. |
| commonVersion          | brokerHost                                    | The version of common that should be used while building the dist variant of the brokerHost app. |
| adAccountsVersion      | brokerHost                                    | The version of ad-accounts library that should be used while building the dist variant of the brokerHost app. |
| adalVersion            | brokerHost                                    | The version of adal that should be used while building the dist variant of the brokerHost app. |
| msalVersion            | brokerHost                                    | The version of msal that should be used while building the dist variant of the brokerHost app. |

**NOTE:** Please note that above table only indicates the original module that declared the property, however, any other modules that depend on this module are also eligible to accept these properties and pass them along to actual module that's meant to use it. For example, the property `slice` is present on `common`, however, this property of common can also be supplied while building any consumer of common. In that case it will propagate the value all the way to common.

**Example**: `./gradlew msal:testLocalDebugUnitTest -Pslice=<testslice>`

The above example command will assemble and run unit tests in the msal module and while doing so it will set the `slice` project property based on the value supplied so that it is passed over to common project and thus all token requests in our tests are going to be targeting the slice specified.