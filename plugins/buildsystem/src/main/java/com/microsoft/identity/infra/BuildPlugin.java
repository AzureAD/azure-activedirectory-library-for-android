package com.microsoft.identity.infra;

import com.android.build.gradle.LibraryExtension;
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
                .create("aca", BuildPluginExtension.class);

        project.afterEvaluate(project1 -> {
            if(config.getDesugar().get()) {
                project1.getLogger().warn("DESUGARING ENABLED");
                applyDesugaringToAndroidProject(project1);
                applyJava8ToJavaProject(project1);
            }else{
                project1.getLogger().warn("DESUGARING DISABLED");
            }
        });

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
