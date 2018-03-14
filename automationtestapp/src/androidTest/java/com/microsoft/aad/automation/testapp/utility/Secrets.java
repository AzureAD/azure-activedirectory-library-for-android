package com.microsoft.aad.automation.testapp.utility;

import android.util.Log;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.identity.internal.test.keyvault.Configuration;
import com.microsoft.identity.internal.test.keyvault.api.SecretsApi;
import com.microsoft.identity.internal.test.keyvault.model.SecretBundle;



public class Secrets {

    public static String accessToken;
    public static String API_VERSION = "2016-10-01";

    private String appSecret = "";
    private String appID = "dd3a0594-8c09-43b9-b687-1de2b191c4f8";
    private static final String Authority = "https://login.windows.net/common/oauth2/authorize";
    private static final String ResourceURI = "https://msidlabs.vault.azure.net/";

    private void setAT() {
        KeyvaultToken KeyvaultAT = new KeyvaultToken();
        KeyvaultAT.ClientSecretKeyVaultCredential(appID,appSecret);
        String AT = KeyvaultAT.doAuthenticate(Authority,ResourceURI,"");
        if(AT != null)
        {
            accessToken = AT;
        }
        Log.d("Token",AT);
    }


    public static Credential GetCredential(String upn, String secretName){

        Configuration.getDefaultApiClient().setBasePath("https://msidlabs.vault.azure.net");

        Configuration.getDefaultApiClient().setAccessToken(Secrets.accessToken);

        SecretsApi secretsApi = new SecretsApi();
        Credential credential = new Credential();
        credential.userName = upn;

        try {
            SecretBundle secretBundle = secretsApi.getSecret(secretName, "", Secrets.API_VERSION);
            credential.password = secretBundle.getValue();
        }
        catch (com.microsoft.identity.internal.test.keyvault.ApiException ex){
            Log.e("KEYVAULT", "Error accessing secret named: " + secretName, ex);
            throw new RuntimeException("exception accessing secret", ex);
        }

        return credential;
    }
}
