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

package com.microsoft.identity.common.test.automation.utility;

import com.microsoft.identity.internal.test.labapi.Configuration;
import com.microsoft.identity.internal.test.labapi.api.DefaultApi;
import com.microsoft.identity.internal.test.labapi.model.TestConfiguration;

/**
 * Created by shoatman on 3/12/2018.
 */

public class TestConfigurationHelper {

    public static TestConfiguration GetTestConfiguration(TestConfigurationQuery query) {

        Configuration.getDefaultApiClient().setBasePath("http://api.msidlab.com/api");

        DefaultApi api = new DefaultApi();
        TestConfiguration config;

        //Since we cannot pass simply pass "Cloud" to the API, a white space will do.
        if(query.federationProvider.contains("loud")){
            query.federationProvider = "";
        }

        try {
            config = api.getTestConfiguration(query.appName, query.appId, query.federationProvider, query.mfa, query.mam, query.mdm, query.ca, query.mamca, query.mdmca, query.license, query.federated, query.isFederated, query.userType, query.role, query.external, query.upn);
        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving test configuration", ex);
        }

        return config;

    }

}
