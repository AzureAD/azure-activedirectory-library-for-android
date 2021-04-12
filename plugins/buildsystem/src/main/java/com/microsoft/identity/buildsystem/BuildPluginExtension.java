package com.microsoft.identity.buildsystem;

import org.gradle.api.provider.Property;

abstract public class BuildPluginExtension {

    abstract public Property<String> getMessage();
    abstract public Property<Boolean> getDesugar();

}
