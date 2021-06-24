# Build Config

In this doc we will cover what is a `BuildConfig` as well as the differences between Android and
Non-Android modules.

## Android Modules

Android modules have a concept of a `BuildConfig.java` class that can store build specific constants
such as build type, version etc. Furthermore, we can inject additional constants into this class as
we want from the `build.gradle` script.

For example, we can inject a constant as follows:

```java
buildConfigField("String", "MY_CONSTANT", "my-value")
```

We can read more about BuildConfig here: https://developer.android.com/studio/build/gradle-tips#simplify-app-development

## Non-Android Modules

Unlike Android module, java modules don't support a `BuildConfig` class out of the box, however, we
have a few gradle plugins that can help us achieve similar behavior.

For instance, we have `gradle-build-config` plugin from gmazzo that can help us achieve this. We
can read more about this plugin here: https://github.com/gmazzo/gradle-buildconfig-plugin

The plugin allows us to generate a BuildConfig file for different source sets such as source code or
test. The plugin is straightforward to use, please refer to official plugin docs to see complete
documentation.

We are currently using this plugin to inject `labSecret` for our Lab Api in Java only modules. For
more information on configuring Lab Api, please read [Lab Setup Doc](../Automation/labsetup.md).

We can see example usage in the tests for LabApiUtilities module.
See [build.gradle for LabApiUtilities](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/blob/dev/LabApiUtilities/build.gradle).
