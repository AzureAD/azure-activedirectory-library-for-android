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
import com.github.spotbugs.snom.SpotBugsExtension;
import com.github.spotbugs.snom.SpotBugsPlugin;

import org.gradle.api.Project;
import java.io.File;

/**
 * Class to apply and configure the SpotBugs gradle Plugin to a project
 * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin">Spotbugs gradle plugin </a>
 */
public final class SpotBugs {
    private static final String BASELINE_FILE_PATH_RELATIVE_TO_PROJECT_ROOT = "../config/spotbugs/baseline.xml";
    private static final String EXCLUDE_FILE_PATH_RELATIVE_TO_PROJECT_ROOT = "../config/spotbugs/exclude.xml";

    /**
     * Applies theo  SpotBugs Plugin to given project
     * @see <a href="https://github.com/spotbugs/spotbugs-gradle-plugin">Spotbugs gradle plugin </a>
     * @param project project to apply the Spotbugs plugin
     */
    static void applySpotBugsPlugin(Project project) {
        if(!project.getPlugins().hasPlugin(SpotBugsPlugin.class)) {
            project.getPlugins().apply(SpotBugsPlugin.class);
        }

        final SpotBugsExtension spotBugsExtension = project.getExtensions().findByType(SpotBugsExtension.class);
        final File baselineFile = project.file(BASELINE_FILE_PATH_RELATIVE_TO_PROJECT_ROOT);
        if(baselineFile.exists()) {
            spotBugsExtension.getBaselineFile().set(baselineFile);
        }

        final File excludeFile = project.file(EXCLUDE_FILE_PATH_RELATIVE_TO_PROJECT_ROOT);
        if (excludeFile.exists()) {
            spotBugsExtension.getExcludeFilter().set(excludeFile);
        }
    }
}
