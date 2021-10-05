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

/**
 * This class controls some of the configurations used by the code coverage plugin.
 */
open class CodeCoverageReportExtension {

    var html = ReportConfig(true) // whether code coverage html output is enabled
    var xml = ReportConfig(true) // whether code coverage xml output is enabled
    var csv = ReportConfig(true) // whether code coverage csv output is enabled

    var unitTests = ReportConfig(true) // whether code coverage targets unit tests
    var androidTests = ReportConfig(false) // whether code coverage targets android tests

    var destination: String? = null // the destination of the reports - by default it's buildDir/reports/jacoco
    var excludeFlavors: Set<String>? = null // add some product flavours to exclude
    var excludeClasses: Set<String>? = null // add some classes to exclude

    var includeNoLocationClasses: Boolean = true // To include Robolectric tests in the Jacoco report, flag -> "includeNolocationClasses" is set to true

    var jacocoVersion: String = "0.8.7" // jacoco version

    /**
     * get files to exclude
     */
    val getFileFilterPatterns: Set<String>
        get() {
            return DEFAULT_EXCLUDES + (excludeClasses ?: emptySet())
        }

}

open class ReportConfig(var enabled: Boolean)
