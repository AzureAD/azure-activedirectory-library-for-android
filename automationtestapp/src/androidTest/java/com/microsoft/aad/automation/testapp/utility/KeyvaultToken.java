package com.microsoft.aad.automation.testapp.utility;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

//import org.apache.http.Header;
//import org.apache.http.message.BasicHeader;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
/**
 * Created by v-dmkapi on 3/14/2018.
 */

public class KeyvaultToken
{
    private String applicationId = "dd3a0594-8c09-43b9-b687-1de2b191c4f8";
    private String applicationSecret;

    public void ClientSecretKeyVaultCredential(String applicationId, String applicationSecret)
    {
        this.setApplicationId(applicationId);
        this.setApplicationSecret(applicationSecret);
    }

    public String getApplicationId() {
        return applicationId;
    }

    private void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationSecret() {
        return applicationSecret;
    }

    private void setApplicationSecret(String applicationSecret) {
        this.applicationSecret = applicationSecret;
    }


    public String doAuthenticate(String authorization, String resource, String scope) {
        AuthenticationResult res = null;

        try {
            res = GetAccessToken(authorization, resource, this.applicationId, this.applicationSecret);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return res.getAccessToken();
    }

    private AuthenticationResult GetAccessToken(String authorization, String resource, String clientID, String clientKey)
            throws InterruptedException, ExecutionException {
        AuthenticationContext ctx = null;
        ExecutorService service = Executors.newFixedThreadPool(1);
        try {
            ctx = new AuthenticationContext(authorization, false, service);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Future<AuthenticationResult> resp = ctx.acquireToken(resource, new ClientCredential(
                clientID, clientKey), null);
        AuthenticationResult res = resp.get();
        return res;
    }
}