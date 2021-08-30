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

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.*
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

class CodeCoveragePlugin {

    companion object {
        private val logger: Logger = Logging.getLogger(CodeCoveragePlugin::class.java)
        private lateinit var reportExtension: JacocoCoverageReportExtension

        @JvmStatic fun applyCodeCoveragePlugin(project: Project) {
            reportExtension = project.extensions.create("jacocoCoverageReport", JacocoCoverageReportExtension::class.java, JacocoCoverageReportExtension.excludes)
            project.plugins.apply(JacocoPlugin::class.java)

            findAndroidPlugin(project.plugins)

            val jacocoTestReportTask = findOrCreateJacocoTestReportTask(project.tasks)

            project.android().variants().all { variant ->
                val reportTask = createReportTask(project, variant)
                jacocoTestReportTask.dependsOn(reportTask)

                logTaskAdded(reportTask)
            }
        }

        private fun createReportTask(project: Project, variant: BaseVariant): JacocoReport {
            val sourceDirs = sourceDirs(variant)
            val classesDir = classesDir(variant)
            val testTask = testTask(project.tasks, variant)
            val executionData = executionDataFile(testTask)

            return project.tasks.create("jacoco${testTask.name.capitalize()}Report", JacocoReport::class.java) { reportTask ->
                reportTask.dependsOn(testTask)
                reportTask.group = "Reporting"
                reportTask.description = "Generates Jacoco coverage reports for the ${variant.name} variant."
                reportTask.executionData.setFrom(project.files(executionData))
                reportTask.sourceDirectories.setFrom(project.files(sourceDirs))
                val javaTree = project.fileTree(classesDir, excludes = getFileFilterPatterns())
                if (hasKotlin(project.plugins)) {
                    val kotlinClassesDir = "${project.buildDir}/tmp/kotlin-classes/${variant.name}"
                    val kotlinTree = project.fileTree(kotlinClassesDir, excludes = getFileFilterPatterns())
                    reportTask.classDirectories.setFrom(javaTree + kotlinTree)
                } else {
                    reportTask.classDirectories.setFrom(javaTree)
                }

                reportTask.reports { task ->
                    val destination = reportExtension.destination

                    task.html.isEnabled = reportExtension.generateHtml
                    task.xml.isEnabled = reportExtension.generateXml
                    task.csv.isEnabled = reportExtension.generateCsv

                    if (reportExtension.generateHtml) {
                        task.html.destination = File(if (destination.isNullOrBlank()) "${project.buildDir}/jacoco/jacocoHtml" else "${destination.trim()}/jacocoHtml")
                    }

                    if (reportExtension.generateXml) {
                        task.xml.destination = File(if (destination.isNullOrBlank()) "${project.buildDir}/jacoco/jacoco.xml" else "${destination.trim()}/jacoco.xml")
                    }

                    if (reportExtension.generateCsv) {
                        task.csv.destination = File(if (destination.isNullOrBlank()) "${project.buildDir}/jacoco/jacoco.csv" else "${destination.trim()}/jacoco.csv")
                    }
                }
            }
        }

        fun findAndroidPlugin(plugins: PluginContainer) {
            plugins.findPlugin("android") ?: plugins.findPlugin("android-library")
            ?: throw GradleException("You must apply the Android plugin or the Android library plugin before using the jacoco-android plugin")
        }

        private fun findOrCreateJacocoTestReportTask(tasks: TaskContainer): Task {
            var task: Task? = tasks.findByName("jacocoTestReport")
            if (task == null) {
                task = tasks.create("jacocoTestReport") { tsk ->
                    tsk.group = "Reporting"
                }
            }
            return task!!
        }

        private fun sourceDirs(variant: BaseVariant) {
            variant.sourceSets.flatMap { it.javaDirectories.map { dir -> dir.path } }
        }

        private fun classesDir(variant: BaseVariant): File {
            return if (variant.javaCompileProvider != null && variant.javaCompileProvider.isPresent) {
                variant.javaCompileProvider.get().destinationDir
            } else {
                variant.javaCompile.destinationDir
            }
        }

        private fun testTask(tasks: TaskContainer, variant: BaseVariant): Task = tasks.getByName("test${variant.name.capitalize()}UnitTest")

        private fun executionDataFile(testTask: Task): File? = testTask.extensions.findByType(JacocoTaskExtension::class.java)?.destinationFile

        private fun hasKotlin(plugins: PluginContainer) = plugins.hasPlugin("kotlin-android")

        private fun logTaskAdded(reportTask: JacocoReport) {
            logger.info("Added $reportTask")
            logger.info("  executionData: $reportTask.executionData.asPath")
            logger.info("  sourceDirectories: $reportTask.sourceDirectories.asPath")
            logger.info("  csv.destination: $reportTask.reports.csv.destination")
            logger.info("  xml.destination: $reportTask.reports.xml.destination")
            logger.info("  html.destination: $reportTask.reports.html.destination")
        }

        fun Project.android(): BaseExtension {
            val android = project.extensions.findByType(BaseExtension::class.java)
            if (android != null) {
                return android
            } else {
                throw GradleException("Project $name is not an Android project")
            }
        }

        private fun BaseExtension.variants(): DomainObjectSet<out BaseVariant> {
            return when (this) {
                is AppExtension -> {
                    applicationVariants
                }

                is LibraryExtension -> {
                    libraryVariants
                }

                else -> throw GradleException("Unsupported BaseExtension type!")
            }
        }

        private fun getFileFilterPatterns(): List<String> = JacocoCoverageReportExtension.DEFAULT_EXCLUDES + reportExtension.excludes

        private fun Project.fileTree(dir: Any, excludes: List<String> = listOf(), includes: List<String> = listOf()): ConfigurableFileTree =
                fileTree(mapOf("dir" to dir, "excludes" to excludes, "includes" to includes))

    }
}