package com.microsoft.aad.automation.testapp.utility;

import android.util.Log;


import com.microsoft.identity.internal.test.keyvault.Configuration;
import com.microsoft.identity.internal.test.keyvault.api.SecretsApi;
import com.microsoft.identity.internal.test.keyvault.model.SecretBundle;


public class Secrets {

    public static String accessToken;
    public static String API_VERSION = "2016-10-01";

    public static Credential GetCredential(String upn, String secretName){

        accessToken = AzureToken.getaccessTokenJAVA(
                (System.getProperty("APP_SECRET_ENV")),
                (System.getProperty("APP_ID_ENV")),
                "microsoft.com",
                "https://management.azure.com/.default"
        );

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
