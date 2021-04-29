package com.microsoft.identity.buildsystem;

import com.android.build.gradle.LibraryExtension;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BuildPlugin implements Plugin<Project> {

    private final static String ANDROID_LIBRARY_PLUGIN_ID = "com.android.library";

    @Override
    public void apply(final Project project) {

        final BuildPluginExtension config = project.getExtensions()
                .create("buildSystem", BuildPluginExtension.class);

        project.afterEvaluate(project1 -> {
            if(config.getDesugar().get()) {
                project1.getLogger().warn("DESUGARING ENABLED");
                applyDesugaring(project1);
            }else{
                project1.getLogger().warn("DESUGARING DISABLED");
            }
        });

        SpotBugs.apply(project);
    }

    private void applyDesugaring(final Project project){

        project.getPluginManager().withPlugin(ANDROID_LIBRARY_PLUGIN_ID, appliedPlugin -> {
            LibraryExtension libraryExtension = project.getExtensions().findByType(LibraryExtension.class);
            libraryExtension.getCompileOptions().setSourceCompatibility(JavaVersion.VERSION_1_8);
            libraryExtension.getCompileOptions().setTargetCompatibility(JavaVersion.VERSION_1_8);
            libraryExtension.getCompileOptions().setCoreLibraryDesugaringEnabled(true);
        });

    }
}
