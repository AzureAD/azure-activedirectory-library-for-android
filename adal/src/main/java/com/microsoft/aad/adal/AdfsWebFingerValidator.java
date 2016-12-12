package com.microsoft.aad.adal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import static com.microsoft.aad.adal.HttpConstants.StatusCode.*;

class AdfsWebFingerValidator extends AbstractRequestor {

    private static final String TAG = "AdfsWebFingerValidator";

    void validateAuthority(final URL authorizationEndpoint, final DrsMetadata drsMetadata)
            throws AuthenticationException {
        try {
            URL webFingerUrl = forgeWebFingerUrl(authorizationEndpoint, drsMetadata);

            final HttpWebResponse webResponse =
                    mWebrequestHandler.sendGet(webFingerUrl, new HashMap<String, String>());
            if (SC_OK != webResponse.getStatusCode()) {
                throw new AuthenticationException();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            // TODO throw AuthenticationException?
            throw new AuthenticationException();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deserializes {@link HttpWebResponse} bodies into {@link WebFingerMetadata}
     *
     * @param webResponse the HttpWebResponse to deserialize
     * @return the parsed response
     */
    WebFingerMetadata parseMetadata(HttpWebResponse webResponse) {
        return parser().fromJson(webResponse.getBody(), WebFingerMetadata.class);
    }

    private URL forgeWebFingerUrl(URL authorizationEndpoint, DrsMetadata drsMetadata)
            throws MalformedURLException {
        final URL passiveAuthEndpoint = new URL(
                drsMetadata
                        .mIdentityProviderService
                        .mPassiveAuthEndpoint
        );

        final String paeDomain = passiveAuthEndpoint.getHost();
        String url =
                "https://"
                        + paeDomain
                        + String.format(
                        "/.well-known/webfinger?resource=%s",
                        authorizationEndpoint.toString()
                );
        return new URL(url);
    }

}
