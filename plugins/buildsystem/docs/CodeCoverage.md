# Code Coverage Plugin 


## Intro
- [Code coverage](https://en.wikipedia.org/wiki/Code_coverage) is a software metric used to measure how many lines of our code are executed during automated tests.

- [JaCoCo](https://www.eclemma.org/jacoco/trunk/index.html) is a free Java code coverage tool and the [JaCoCo plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html) provides code coverage metrics for Java code via integration with JaCoCo.

## Why
In order to generate [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/index.html) unit test coverage reports for Android projects you need to create `JacocoReport` tasks and configure them by providing paths to source code, execution data and compiled classes. It's not straightforward since Android projects can have different flavors and build types thus requiring additional paths to be set. This plugin configures these `JacocoReport` tasks automatically.

## Usage
```groovy
plugins {
    ...
    id 'com.gradle.plugin-publish' version '0.14.0' // or whatever version is most recent
}

codeCoverageReport{
    html.enabled = true // by default it's true
    xml.enabled = true // by default it's true
    csv.enabled = true // by default it's true

    unitTests.enabled = true // whether code coverage tasks for unit tests will be generated
    androidTests.enabled = true // whether code coverage tasks for instrumentation tests will be generated

    excludeFlavors = [''] // the product flavors to exclude when generating the code coverage tasks

    excludeClasses = [''] // additional classes to exclude - most are already catered for

    destination = '/some/other/directory' // if you want to configure a custom path to save the code coverage reports, by default your report gets saved in `[project]/build/jacoco/{flavor}{build type}{project}{test type}CoverageReport`

    includeNoLocationClasses = true // To include Robolectric tests in the Jacoco report this needs to be true
}

android {
    buildTypes {
        debug {
            testCoverageEnabled true // this instructs the plugin to generate code coverage reports for this build type
            ...
        }
        release {
            testCoverageEnabled false // this instructs the plugin to NOT generate code coverage reports for this build type
            ...
        }
    }
    ...
    productFlavors {
        local {}
        dist {}
    }
}
```

The above configuration creates a `JacocoReport` task for each variant in the form of `{flavor}{build type}{project}{test type}CoverageReport`
```
distDebugAppAndroidTestCoverageReport
distDebugAppUnitTestCoverageReport
localDebugAppAndroidTestCoverageReport
localDebugAppUnitTestCoverageReport
```

By default these are the excluded classes under [Constants](https://github.com/AzureAD/android-complete/blob/paul/code-coverage-plugin/plugins/buildsystem/src/main/java/com/microsoft/identity/buildsystem/codecov/Constants.kt#L25)
