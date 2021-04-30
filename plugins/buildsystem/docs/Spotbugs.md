# Build Plugin - SpotBugs

The [SpotBugs plugin](https://github.com/spotbugs/spotbugs-gradle-plugin) is applied by default with the Auth Client's [BuildPlugin](Overview.md). This document describes the default SpotBugs configs applied by the BuildPlugin as well as how to overrride these configuration for individual project.

## References

- [Spotbugs docs](https://spotbugs.readthedocs.io/en/stable/introduction.html)
- [SpotBugs Gradle Task](https://spotbugs-gradle-plugin.netlify.app/com/github/spotbugs/snom/spotbugstask)
- [Extension to configure Spotbugs gradle plugin](https://spotbugs-gradle-plugin.netlify.app/com/github/spotbugs/snom/spotbugsextension)
- [Spotbugs github](https://github.com/spotbugs/spotbugs-gradle-plugin#readme)
- [Spotbugs bug descriptions](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html)
- [SpotBugs Build Task for Azure Devops](https://www.1eswiki.com/wiki/SpotBugs_Build_Task)
- [Secure development tools extension for Azure Devops](https://www.1eswiki.com/wiki/Secure_Development_Tools_Extension_For_Azure_DevOps)

## What is SpotBugs

SpotBugs is a static analysis tool to find bugs in Java programs. It looks for instances of “bug patterns” — code instances that are likely to be errors and reports them as violations. More details can be found at [official docs](https://spotbugs.readthedocs.io/en/stable/introduction.html).

## SpotBugs Configs applied by Auth Client BuildPlugin

Below are the specific values that BuildPlugin sets in addition to the [default values](https://spotbugs-gradle-plugin.netlify.app/com/github/spotbugs/snom/spotbugstask) already set by the SpotBugs Plugin

- baseline file path

```java
final File baselineFile = project.file("../config/spotbugs/baseline.xml");
if(baselineFile.exists()) {
    spotBugsExtension.getBaselineFile().set(baselineFile);
}
```

## Overriding Spotbugs config in individual projects

In order to override the default configs add below task in your project's build.gradle file and then set the values for any property that you want to override

```groovy
tasks.withType(com.github.spotbugs.snom.SpotBugsTask) {
    baselineFile = file('<PATH TO OVERRIDDEN BASELINE FILE>')
    reports {
        xml.enabled = false
        html.enabled = true
    }
}
```

### Min BuildPlugin version

SpotBugs is enabled in the BuildPlugin version 0.1.0
