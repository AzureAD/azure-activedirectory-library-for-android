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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.tasks.TaskContainer

/**
 * Check if we are allowed to create code coverage tasks for this variant
 */
fun shouldCreateTaskForVariant(excludeFlavours: List<String>, variant: BaseVariant, testType: String): Boolean {
    if (!variant.buildType.isTestCoverageEnabled || excludeFlavours.contains(variant.flavorName.toLowerCase())) {
        return false
    }

    // AFAIK, the android tests tasks are only generated for debug build types
    if (testType == TestTypes.AndroidTest && variant.buildType.name != "debug") {
        return false
    }

    return true
}

/**
 * Get the test task for this variant
 */
fun getAndroidTestTask(tasks: TaskContainer, variant: BaseVariant, testType: String): Task {
    val name = if (testType == TestTypes.UnitTest) "test${variant.name.capitalize()}UnitTest" else "connected${variant.name.capitalize()}AndroidTest"
    return tasks.getByName(name)
}

/**
 * get JacocoTaskExtension execution destination
 */
fun getAndroidExecutionDataFile(testTask: Task, variant: BaseVariant, testType: String): String {
    // Output of those additional tasks are stored in .exec file for unit tests and .ec file for android tests
    val unitTestFile = "jacoco/${testTask.name}.exec"
    val androidTestFile = "outputs/code_coverage/${variant.name}AndroidTest/connected/*.ec"
    return if (testType == TestTypes.UnitTest) unitTestFile else androidTestFile
}

fun getJavaExecutionDataFile(project: Project): String {
    return if (isKotlinMultiplatform(project)) "${project.buildDir}/jacoco/jvmTest.exec" else "${project.buildDir}/jacoco/test.exec"
}

/**
 * check kotlin is available
 */
fun hasKotlin(plugins: PluginContainer) = plugins.hasPlugin("kotlin-android")

fun isAndroidProject(project: Project): Boolean {
    val isAndroidLibrary = project.plugins.hasPlugin("com.android.library")
    val isAndroidApp = project.plugins.hasPlugin("com.android.application")
    val isAndroidTest = project.plugins.hasPlugin("com.android.test")
    val isAndroidFeature = project.plugins.hasPlugin("com.android.feature")
    val isAndroidDynamicFeature = project.plugins.hasPlugin("com.android.dynamic-feature")
    val isAndroidInstantApp = project.plugins.hasPlugin("com.android.instantapp")
    return isAndroidLibrary || isAndroidApp || isAndroidTest || isAndroidFeature || isAndroidDynamicFeature || isAndroidInstantApp
}

fun isJavaProject(project: Project): Boolean {
    val isJava = project.plugins.hasPlugin("java")
    val isJavaLibrary = project.plugins.hasPlugin("java-library")
    val isJavaGradlePlugin = project.plugins.hasPlugin("java-gradle-plugin")
    return isJava || isJavaLibrary || isJavaGradlePlugin
}

fun isKotlinMultiplatform(project: Project): Boolean {
    return project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
}

/**
 * method to get the android extension - as a Project class extension method!
 */
fun Project.android(): BaseExtension {
    val android = project.extensions.findByType(BaseExtension::class.java)
    if (android != null) {
        return android
    } else {
        throw GradleException("Project $name is not an Android project")
    }
}

/**
 * method to get variants
 */
fun BaseExtension.variants(): DomainObjectSet<out BaseVariant> {
    return when (this) {
        is AppExtension -> {
            applicationVariants
        }

        is LibraryExtension -> {
            libraryVariants
        }

        else -> throw GradleException("Unsupported Android BaseExtension type!")
    }
}

/**
 * utility method to get the file tree
 */
fun Project.filesTree(dir: Any, excludes: Set<String> = emptySet(), includes: Set<String> = emptySet()): ConfigurableFileTree =
        fileTree(mapOf("dir" to dir, "excludes" to excludes, "includes" to includes))

val DEFAULT_EXCLUDES = setOf(
        // Core Android generated class filters
        "**/R.class",
        "**/R2.class", // ButterKnife Gradle Plugin.
        "**/R$*.class",
        "**/R2$*.class", // ButterKnife Gradle Plugin.
        "**/*$$*",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "android/**/*.*",
        "**/*\$Lambda\$*.*", // Jacoco can not handle several "$" in class name.
        "**/*\$inlined$*.*", // Kotlin specific, Jacoco can not handle several "$" in class name.
        "**/*Dagger*.*", // Dagger auto-generated code.
        "**/*MembersInjector*.*", // Dagger auto-generated code.
        "**/*_Provide*Factory*.*", // Dagger auto-generated code.
        "**/*_Factory*.*", // Dagger auto-generated code.
        "**/*\$StateSaver.*", // android-state auto-generated code.
        "**/*AutoValue_*.*" // AutoValue auto-generated code.
)

object TestTypes {
    const val UnitTest = "UnitTest"
    const val AndroidTest = "AndroidTest"
}
