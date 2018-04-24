package com.microsoft.identity.common.test.automation.utility;

import com.google.gson.Gson;
import com.microsoft.identity.internal.test.labapi.model.TestConfiguration;

import jdk.nashorn.internal.parser.Token;

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

    private void createSilentTokenRequest(){
        // mAuthenticationContext.acquireTokenSilentAsync(mResource, mClientId, mUserId, getAdalCallback());
        TokenRequest tr = new TokenRequest();
        tr.setAuthority(getAuthority(this.getTestConfiguration().getAuthority().get(0)));
        tr.setResourceId(this.getTestConfiguration().getResourceIds().get(0));
        tr.setClientId(this.getTestConfiguration().getAppId());
        tr.setUniqueUserId(this.getTestConfiguration().getUsers().getObjectId());
        this.mSilentTokenRequest = tr;
    }

    private String getAuthority(String authorityHost){
        return authorityHost + "common";
    }

    private String getTenantedAuthority(String authorityHost, String tenantId){

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

        return scenario;
    }
}
