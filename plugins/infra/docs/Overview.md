# Auth Client - Build - Custom Plugin

This document describes the custom plugin created by the AuthClient team for use with their builds as well as provides references to the documentation necessary to build, maintain, test and publish it.  The plugin encapsulates common logic uses by all of our builds in one single place.  

## References:

- [Consuming a custom plugin](https://docs.gradle.org/current/userguide/plugins.html#sec:custom_plugin_repositories)
- [Plugin to help create custom plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins.html)
- [The plugin to help publish a custom plugin](https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html)
- [Latest Gradle Domain Specific Language (DSL)](https://docs.gradle.org/current/dsl/index.html)
- [Project DSL - Which is the interface to build.gradle](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html)
- [Gradle Build Lifecycle](https://docs.gradle.org/current/userguide/build_lifecycle.html)
- [Android Gradle DSL (3.4 Latest when authored)](https://google.github.io/android-gradle-dsl/3.4/)
- [IntelliJ IDEA Download](https://www.jetbrains.com/idea/download/#section=windows)
- [Gradle Plugin Development Plugin](https://docs.gradle.org/current/userguide/java_gradle_plugin.html)
- [Plugin Publishing Plugin & Instructions to publish to Plugin Portal](https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html)

## Implementation Notes:

### Basic Project Setup

You need an IDE to develop/maintain the plugin.  The author used IntelliJ IDEA for this purpose.  Any IDE for building java gradle projects work.  

### Plugins to help build and publish plugins and build.gradle for our plugin

The com.microsoft.identity.infra plugin uses 3 plugins to help build and publish the plugin such that it can be located by consuming projects correctly.  If you want to know the details of what they do please refer to their documentation above.

- java-gradle-plugin: Which adds the java plugin and knows about how to generate plugin specific build artifacts
- com.gradle.plugin-publish: Which knows how to publsh plugin specific build artifacts and knows about the plugin portal
- maven-publish: required by com.gradle.plugin-publish

We simply add them to our build.gradle and configure them as follows:

```groovy
plugins {
    id 'java-gradle-plugin'
    id 'maven-publish'
    id 'com.gradle.plugin-publish' version '0.14.0'
}

group 'com.microsoft.identity'
version '1.0'

pluginBundle {
    website = 'https://github.com/azuread'
    vcsUrl = 'https://github.com/azuread/android-complete'
    tags = ['build']
}

gradlePlugin {
    plugins {
        buildPlugin {
            id = 'com.microsoft.identity.infra'
            implementationClass = 'com.microsoft.identity.infra.BuildPlugin'
            displayName = "AuthClient Android Build Plugin"
            description = "Gradle plugin to encapsulate custom build tasks and configuration for AuthClient android projects."
        }
    }
}
```

The key piece of configuation is within the "buildPlugin" properties.  Specifically:

- id (the plugin id used when applying the plugin in other projects)
- implementationClass (is the entry point for the plugin)


### Basic plugin implementation

Gradle gives plugins the opportunity to insert Gradle Tasks and/or add code to be executed in response to particular project and/or build lifecycle events.

Developers can connect into gradle by implementing the Plugin<Project> apply method and by applying the plugin to the project via the corresponding build.gradle file.

```java
@Override
public void apply(final Project project) {

}
```

In this method you receive a reference to the project object which is an in memory represenation of the build.gradle file.  Plugins are the first things to be added to a build so it's very likely that other plugins have not yet been added and that the rest of the project object state has not been fully loaded/evaluated when your plugin code is being executed.

### Android Plugin Extensions

Since we're using gradle to build android projects (applications and libraries) it's necessary to be familiar with the android gradle plugin extension types.  For example:

'com.android.library' is the plugin id for the class 'com.android.build.gradle.LibraryExtension'
'com.android.application' is the plugin id for the class 'com.android.build.gradle.ApplicationExtension'

> NOTE: Plugins can apply other plugins.  So we could consider applying the com.android.library plugin to our projects if we wanted to.  

Again since the android plugin may not have been applied when our plugin code runs.  We use the "withPlugin" method of the PluginManager to ensure that it's available in the project before interacting with it's extensions.  In the example our code to get a reference to the LibraryExtension.class isn't executed until the android library plugin is applied to the project.

```java
project.getPluginManager().withPlugin(ANDROID_LIBRARY_PLUGIN_ID, appliedPlugin -> {
    LibraryExtension libraryExtension = project.getExtensions().findByType(LibraryExtension.class);
    
});
```

### Project lifecycle events

You can wire up your code to execute in response to specific project lifecycle events.  In the example below you see an example using afterEvaluate.  You can refer to the Project DSL documentation above for other available lifecycle events.

```java
@Override
public void apply(final Project project) {

    project.afterEvaluate(project1 -> {
        //Add code to be executed after the project is evaluated
    });

}
```

### Resolving Plugin References that are not in Plugin Portal

In order to resolve plugin references that are not published to the portal.  You need to tell gradle where to look for them.  

Similar to normal dependencies you can confirm one or more repositories for plugins.  In the case of plugins this is one in the pluginManagement section of your settings.gradle file.

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        google()
        jcenter()
        gradlePluginPortal()
    }
}
```

> NOTE: Since our plugin references the android plugin we need to include google() and jcenter() here.
