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

        try {
            config = api.getTestConfiguration(query.appName, query.appId, query.federationProvider, query.mfa, query.mam, query.mdm, query.ca, query.mamca, query.mdmca, query.license, query.federated, query.isFederated, query.userType, query.role, query.external, query.upn);
        } catch (com.microsoft.identity.internal.test.labapi.ApiException ex) {
            throw new RuntimeException("Error retrieving test configuration", ex);
        }

        return config;

    }

}
