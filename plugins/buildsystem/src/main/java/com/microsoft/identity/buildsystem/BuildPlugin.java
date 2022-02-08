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
package com.microsoft.identity.buildsystem;

import com.android.build.gradle.LibraryExtension;
import com.microsoft.identity.buildsystem.codecov.CodeCoverage;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BuildPlugin implements Plugin<Project> {

    private final static String ANDROID_LIBRARY_PLUGIN_ID = "com.android.library";
    private final static String JAVA_LIBRARY_PLUGIN_ID = "java-library";

    private final static String JAVA_SOURCE_COMPATIBILITY_PROPERTY = "sourceCompatibility";
    private final static String JAVA_TARGET_COMPATIBILITY_PROPERTY = "targetCompatibility";

    @Override
    public void apply(final Project project) {

        final BuildPluginExtension config = project.getExtensions()
                .create("buildSystem", BuildPluginExtension.class);

        project.afterEvaluate(project1 -> {
            if(config.getDesugar().get()) {
                project1.getLogger().warn("DESUGARING ENABLED");
                applyDesugaringToAndroidProject(project1);
                applyJava8ToJavaProject(project1);
            }else{
                project1.getLogger().warn("DESUGARING DISABLED");
            }
        });

        SpotBugs.applySpotBugsPlugin(project);

        CodeCoverage.applyCodeCoveragePlugin(project);
    }

    private void applyDesugaringToAndroidProject(final Project project){

        project.getPluginManager().withPlugin(ANDROID_LIBRARY_PLUGIN_ID, appliedPlugin -> {
            LibraryExtension libraryExtension = project.getExtensions().findByType(LibraryExtension.class);
            libraryExtension.getCompileOptions().setSourceCompatibility(JavaVersion.VERSION_1_8);
            libraryExtension.getCompileOptions().setTargetCompatibility(JavaVersion.VERSION_1_8);
            libraryExtension.getCompileOptions().setCoreLibraryDesugaringEnabled(true);
        });

    }

    private void applyJava8ToJavaProject(final Project project) {
        project.getPluginManager().withPlugin(JAVA_LIBRARY_PLUGIN_ID, appliedPlugin -> {
            project.setProperty(JAVA_SOURCE_COMPATIBILITY_PROPERTY, JavaVersion.VERSION_1_8);
            project.setProperty(JAVA_TARGET_COMPATIBILITY_PROPERTY, JavaVersion.VERSION_1_8);
        });
    }
}
