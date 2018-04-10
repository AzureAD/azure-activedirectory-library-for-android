package com.microsoft.aad.automation.testapp.utility;

import android.security.KeyChain;
import android.security.KeyChainException;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.microsoft.identity.common.internal.providers.keys.CertificateCredential;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftClientAssertion;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.internal.test.keyvault.Configuration;
import com.microsoft.identity.internal.test.keyvault.api.SecretsApi;
import com.microsoft.identity.internal.test.keyvault.model.SecretBundle;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


public class Secrets {

    private static String mAccessToken = null;
    public static String API_VERSION = "2016-10-01";
    private final static String CLIENT_ID = "4bc6e96f-bd23-408f-8ecb-a7a7145463f9";
    private final static String SCOPE = "https://vault.azure.net/.default";
    private final static String GRANT_TYPE = "client_credentials";
    private final static String CERTIFICATE_ALIAS = "AutomationRunner";
    private final static String KEYSTORE_TYPE = "AndroidKeyStore";
    private final static String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private final static String MSSTS_CLIENT_ASSERTION_AUDIENCE = "https://login.microsoftonline.com/microsoft.com/oauth2/v2.0/token";


    public static String getAccessToken() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, KeyChainException, InterruptedException {
        if (mAccessToken != null) {
            return mAccessToken;
        } else {
            requestAccessTokenForAutomation();
            return mAccessToken;
        }
    }

    /**
     * Yep.  Hardcoding this method to retrieve access token for reading key vault
     */
    private static void requestAccessTokenForAutomation() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, KeyChainException, InterruptedException {

        PrivateKey pk = KeyChain.getPrivateKey(InstrumentationRegistry.getTargetContext().getApplicationContext(), "te-5676ab88-cb56-4b1a-a707-db44711a147a");

        X509Certificate[] certificates = KeyChain.getCertificateChain(InstrumentationRegistry.getTargetContext().getApplicationContext(), "te-5676ab88-cb56-4b1a-a707-db44711a147a");

        CertificateCredential credential = new CertificateCredential.CertificateCredentialBuilder(CLIENT_ID)
                .privateKey(pk)
                .certificate(certificates[0])
                .build();

        String audience = MSSTS_CLIENT_ASSERTION_AUDIENCE;

        MicrosoftClientAssertion assertion = new MicrosoftClientAssertion(audience, credential);

        com.microsoft.identity.common.internal.providers.oauth2.TokenRequest tr = new com.microsoft.identity.common.internal.providers.oauth2.TokenRequest();

        tr.setClientAssertionType(assertion.getClientAssertionType());
        tr.setClientAssertion(assertion.getClientAssertion());
        tr.setClientId(CLIENT_ID);
        tr.setScope(SCOPE);
        tr.setGrantType(GRANT_TYPE);

        OAuth2Strategy strategy = new MicrosoftStsOAuth2Strategy(new MicrosoftStsOAuth2Configuration());

        TokenResult tokenResult = strategy.requestToken(tr);

        if (tokenResult.getSuccess()) {
            Secrets.mAccessToken = tokenResult.getTokenResponse().getAccessToken();
        } else {
            throw new RuntimeException(tokenResult.getErrorResponse().getErrorDescription());
        }

    }


    public static Credential GetCredential(String upn, String secretName) throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, IOException, KeyChainException, InterruptedException {

        Configuration.getDefaultApiClient().setBasePath("https://msidlabs.vault.azure.net");
        Configuration.getDefaultApiClient().setAccessToken(Secrets.getAccessToken());

        SecretsApi secretsApi = new SecretsApi();
        Credential credential = new Credential();
        credential.userName = upn;

        try {
            SecretBundle secretBundle = secretsApi.getSecret(secretName, "", Secrets.API_VERSION);
            credential.password = secretBundle.getValue();
        } catch (com.microsoft.identity.internal.test.keyvault.ApiException ex) {
            Log.e("KEYVAULT", "Error accessing secret named: " + secretName, ex);
            throw new RuntimeException("exception accessing secret", ex);
        }

        return credential;
    }
}
