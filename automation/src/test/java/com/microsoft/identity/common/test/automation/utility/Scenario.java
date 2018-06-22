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

import com.google.gson.Gson;
import com.microsoft.identity.internal.test.labapi.model.TestConfiguration;

public class Scenario {

    private TestConfiguration mTestConfiguration;
    private Credential mCredential;
    private TokenRequest mTokenRequest;
    private TokenRequest mSilentTokenRequest;

    public TestConfiguration getTestConfiguration() {
        return mTestConfiguration;
    }

    public void setTestConfiguration(TestConfiguration testConfiguration) {
        this.mTestConfiguration = testConfiguration;
        this.createTokenRequest();
        this.createSilentTokenRequest();
    }

    public Credential getCredential() {
        return mCredential;
    }

    public void setCredential(Credential credential) {
        this.mCredential = credential;
    }

    public TokenRequest getTokenRequest() {
        return mTokenRequest;
    }

    public TokenRequest getSilentTokenRequest() {
        return mSilentTokenRequest;
    }

    public String getTokenRequestAsJson() {
        Gson gson = new Gson();
        String requestJson = gson.toJson(this.mTokenRequest);
        return requestJson;
    }

    private void createTokenRequest() {
        TokenRequest tr = new TokenRequest();
        tr.setAuthority(getAuthority(this.getTestConfiguration().getAuthority().get(0)));
        tr.setRedirectUri(this.getTestConfiguration().getRedirectUri().get(0));
        tr.setResourceId(this.getTestConfiguration().getResourceIds().get(0));
        tr.setClientId(this.getTestConfiguration().getAppId());
        this.mTokenRequest = tr;
    }

    private void createSilentTokenRequest() {
        // mAuthenticationContext.acquireTokenSilentAsync(mResource, mClientId, mUserId, getAdalCallback());
        TokenRequest tr = new TokenRequest();
        tr.setAuthority(getAuthority(this.getTestConfiguration().getAuthority().get(0)));
        tr.setResourceId(this.getTestConfiguration().getResourceIds().get(0));
        tr.setClientId(this.getTestConfiguration().getAppId());
        tr.setUniqueUserId(this.getTestConfiguration().getUsers().getObjectId());
        this.mSilentTokenRequest = tr;
    }

    private String getAuthority(String authorityHost) {
        return authorityHost + "common";
    }

    private String getTenantedAuthority(String authorityHost, String tenantId) {

        return authorityHost + tenantId;
    }

    public static Scenario GetScenario(TestConfigurationQuery query) {
        TestConfiguration tc = TestConfigurationHelper.GetTestConfiguration(query);
        String keyVaultLocation = tc.getUsers().getCredentialVaultKeyName();
        String secretName = keyVaultLocation.substring(keyVaultLocation.lastIndexOf('/') + 1);

        Credential credential = null;
        try {
            credential = Secrets.GetCredential(tc.getUsers().getUpn(), secretName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Scenario scenario = new Scenario();
        scenario.setTestConfiguration(tc);

        scenario.setCredential(credential);
        if(credential.userName.contains("#")){
            credential.userName = tc.getUsers().getHomeUPN();
        }
        return scenario;
    }
}
