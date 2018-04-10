package com.microsoft.aad.automation.testapp.utility;

import com.google.gson.Gson;
import com.microsoft.identity.internal.test.labapi.model.TestConfiguration;

public class Scenario {

    private TestConfiguration mTestConfiguration;
    private Credential mCredential;
    private TokenRequest mTokenRequest;

    public TestConfiguration getTestConfiguration() {
        return mTestConfiguration;
    }

    public void setTestConfiguration(TestConfiguration testConfiguration) {
        this.mTestConfiguration = testConfiguration;
        this.createTokenRequest();
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

    public String getTokenRequestAsJson() {
        Gson gson = new Gson();
        String requestJson = gson.toJson(this.mTokenRequest);
        return requestJson;
    }

    private void createTokenRequest() {
        TokenRequest tr = new TokenRequest();
        tr.setAuthority(this.getTestConfiguration().getAuthority().get(0) + "common");
        tr.setRedirectUri(this.getTestConfiguration().getRedirectUri().get(0));
        tr.setResourceId(this.getTestConfiguration().getResourceIds().get(0));
        tr.setClientId(this.getTestConfiguration().getAppId());
        this.mTokenRequest = tr;
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

        return scenario;
    }
}
