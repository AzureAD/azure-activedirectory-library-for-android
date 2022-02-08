//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.buildsystem.codecov

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

/**
 * This class creates code coverage tasks in the given project
 */
object CodeCoverage {

    private lateinit var reportExtension: CodeCoverageReportExtension

    /**
     * Gets the codeCoverageReport configurations and uses them to create the code coverage tasks
     */
    @JvmStatic
    fun applyCodeCoveragePlugin(project: Project) {
        // get the configurations under codeCoverageReport
        reportExtension = project.extensions.create("codeCoverageReport", CodeCoverageReportExtension::class.java)

        // after build file has been evaluated ... add tasks
        project.afterEvaluate { evaluatedProject ->
            // check if code coverage is enabled, if not just return
            if (!reportExtension.coverage.enabled) {
                return@afterEvaluate
            }

            evaluatedProject.configure()

            if (isAndroidProject(evaluatedProject)) {
                addJacocoToAndroid(evaluatedProject)
            } else if (isJavaProject(evaluatedProject) || isKotlinMultiplatform(evaluatedProject)) {
                addJacocoToJava(evaluatedProject)
            }
        }
    }

    /**
     * Apply some plugin configuration from [CodeCoverageReportExtension] to the project.
     */
    private fun Project.configure() {
        if (isAndroidProject(this)) {
            // need to create below file so that jacoco is properly initialized during instrumented tests
            // see https://github.com/jacoco/jacoco/issues/968 for more details
            val androidTestResourcesDirectory = project.file("src/androidTest/resources")
            val jacocoAgentProperties = File(androidTestResourcesDirectory, "jacoco-agent.properties")
            if (!jacocoAgentProperties.exists()) {
                androidTestResourcesDirectory.mkdirs()
                jacocoAgentProperties.writeText("output=none")
            }
        }

        project.plugins.apply(JacocoPlugin::class.java)

        project.extensions.findByType(JacocoPluginExtension::class.java)?.apply {
            toolVersion = reportExtension.jacocoVersion
        }

        tasks.withType(Test::class.java) { testTask ->
            testTask.extensions.findByType(JacocoTaskExtension::class.java)?.apply {
                // To include Robolectric tests in the Jacoco report, flag -> "includeNolocationClasses" should be set to true
                isIncludeNoLocationClasses = reportExtension.includeNoLocationClasses
                if (isIncludeNoLocationClasses) {
                    // This needs to be excluded for JDK 11
                    // SEE: https://stackoverflow.com/questions/68065743/cannot-run-gradle-test-tasks-because-of-java-lang-noclassdeffounderror-jdk-inte
                    excludes = listOf("jdk.internal.*")
                }
            }
        }
    }

    /**
     * add jacoco tasks to an android project
     */
    private fun addJacocoToAndroid(project: Project) {
        project.android().jacoco.version = reportExtension.jacocoVersion

        if (reportExtension.unitTests.enabled) {
            createTasks(project, TestTypes.UnitTest)
        }

        if (reportExtension.androidTests.enabled) {
            createTasks(project, TestTypes.AndroidTest)
        }
    }

    /**
     * Creates the code coverage tasks for the different build variants
     */
    private fun createTasks(project: Project, testType: String) {
        val excludeFlavors = (reportExtension.excludeFlavors ?: emptyList()).map { it.toLowerCase() }
        project.android().variants().all { variant ->
            if (shouldCreateTaskForVariant(excludeFlavors, variant, testType)) {
                createReportTask(project, variant, testType)
            }
        }
    }

    /**
     * Creates the code coverage task for the given build variant and adds it to a group (Reporting)
     */
    private fun createReportTask(project: Project, variant: BaseVariant, testType: String): JacocoReport {
        // get the sources
        val sourceDirs = variant.getSourceFolders(SourceKind.JAVA).map { file -> file.dir }
        // get the classes
        val classesDir = variant.javaCompileProvider.get().destinationDir
        // get the test task for this variant
        val testTask = getAndroidTestTask(project.tasks, variant, testType)
        // get JacocoTaskExtension execution destination
        val executionData = getAndroidExecutionDataFile(testTask, variant, testType)

        val taskName = "${variant.name}${project.name.capitalize()}${testType}CoverageReport"
        return project.tasks.create(taskName, JacocoReport::class.java) { reportTask ->
            // set the task attributes
            reportTask.dependsOn(testTask)
            reportTask.group = "Reporting"
            reportTask.description = "Generates Jacoco coverage reports for the ${variant.name} variant."
            reportTask.executionData.setFrom(project.filesTree(project.buildDir, includes = setOf(executionData)))
            reportTask.sourceDirectories.setFrom(project.files(sourceDirs))

            // get the java project tree and exclude the defined excluded classes
            val javaTree = project.filesTree(classesDir, excludes = reportExtension.getFileFilterPatterns)

            // if kotlin is available, get the kotlin project tree and exclude the defined excluded classes
            if (hasKotlin(project.plugins)) {
                val kotlinClassesDir = "${project.buildDir}/tmp/kotlin-classes/${variant.name}"
                val kotlinTree = project.filesTree(kotlinClassesDir, excludes = reportExtension.getFileFilterPatterns)
                reportTask.classDirectories.setFrom(javaTree + kotlinTree)
            } else {
                reportTask.classDirectories.setFrom(javaTree)
            }

            configureReport(project, reportTask, taskName)
        }
    }

    private fun addJacocoToJava(project: Project) {
        val testTask = project.tasks.getByName("test")
        val jacocoTestReportTask = project.tasks.getByName("jacocoTestReport") as JacocoReport

        val taskName = "${project.name.decapitalize()}UnitTestCoverageReport"
        project.tasks.create(taskName, JacocoReport::class.java) { reportTask ->
            // set the task attributes
            reportTask.dependsOn(testTask)
            reportTask.group = "Reporting"
            reportTask.description = "Generates Jacoco coverage reports"
            reportTask.executionData.setFrom(jacocoTestReportTask.executionData)
            reportTask.sourceDirectories.setFrom(jacocoTestReportTask.sourceDirectories)
            reportTask.additionalSourceDirs.setFrom(jacocoTestReportTask.additionalSourceDirs)
            reportTask.classDirectories.setFrom(jacocoTestReportTask.classDirectories)

            // set destination
            configureReport(project, reportTask, taskName)
        }
    }

    private fun configureReport(project: Project, reportTask: JacocoReport, taskName: String) {
        reportTask.reports { task ->
            // set the outputs enabled according to configs
            task.html.isEnabled = reportExtension.html.enabled
            task.xml.isEnabled = reportExtension.xml.enabled
            task.csv.isEnabled = reportExtension.csv.enabled

            // default reports path
            val defaultCommonPath = "${project.buildDir}/reports/jacoco/$taskName"
            val configuredDestination = reportExtension.destination

            // configure destination for html code coverage output
            if (reportExtension.html.enabled) {
                val path = File(if (configuredDestination.isNullOrBlank()) "$defaultCommonPath/html" else "${configuredDestination.trim()}/html")
                task.html.destination = path
            }

            // configure destination for xml code coverage output
            if (reportExtension.xml.enabled) {
                val path = File(if (configuredDestination.isNullOrBlank()) "$defaultCommonPath/${taskName}.xml" else "${configuredDestination.trim()}/${taskName}.xml")
                task.xml.destination = path
            }

            // configure destination for csv code coverage output
            if (reportExtension.csv.enabled) {
                val path = File(if (configuredDestination.isNullOrBlank()) "$defaultCommonPath/${taskName}.csv" else "${configuredDestination.trim()}/${taskName}.csv")
                task.csv.destination = path
            }
        }
    }
}
