package com.microsoft.aad.adal;

import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import static com.microsoft.aad.adal.HttpConstants.StatusCode.SC_OK;

/**
 * Validates trusts between authorities and ADFS instances using DRS metadata and WebFinger.
 */
class AdfsWebFingerValidator extends AbstractRequestor {

    private static final String TAG = "AdfsWebFingerValidator";

    private static final String TRUSTED_REALM_REL = "http://schemas.microsoft.com/rel/trusted-realm";

    /**
     * Validate the authority.
     *
     * @param authorizationEndpoint the authorization endpoint against which the DRS should be validated
     * @param drsMetadata           the metadata to use for validation
     * @throws AuthenticationException if the authority is not trusted
     */
    void validateAuthority(final URL authorizationEndpoint, final DrsMetadata drsMetadata)
            throws AuthenticationException {
        Logger.v(TAG, "Validating authority for auth endpoint: " + authorizationEndpoint.toString());
        try {
            URL webFingerUrl = forgeWebFingerUrl(authorizationEndpoint, drsMetadata);

            final HttpWebResponse webResponse =
                    getWebrequestHandler()
                            .sendGet(
                                    webFingerUrl,
                                    new HashMap<String, String>()
                            );

            if (SC_OK != webResponse.getStatusCode()) {
                // TODO add msg
                throw new AuthenticationException();
            }

            WebFingerMetadata metadata = parseMetadata(webResponse);

            if (!realmIsTrusted(authorizationEndpoint, metadata)) {
                // TODO add msg
                throw new AuthenticationException();
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            // TODO throw AuthenticationException?
            throw new AuthenticationException();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            throw new AuthenticationException(ADALError.JSON_PARSE_ERROR);
        }
    }

    private boolean realmIsTrusted(URL authorizationEndpoint, WebFingerMetadata metadata) {
        String href, rel;
        for (Link link : metadata.getLinks()) {
            href = link.getHref();
            rel = link.getRel();
            String host =
                    authorizationEndpoint.getProtocol() + "://" + authorizationEndpoint.getHost();
            if (href.equalsIgnoreCase(host) && rel.equalsIgnoreCase(TRUSTED_REALM_REL)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deserializes {@link HttpWebResponse} bodies into {@link WebFingerMetadata}.
     *
     * @param webResponse the HttpWebResponse to deserialize
     * @return the parsed response
     */
    WebFingerMetadata parseMetadata(HttpWebResponse webResponse) {
        Logger.v(TAG, "Parsing WebFinger response");
        return parser().fromJson(webResponse.getBody(), WebFingerMetadata.class);
    }

    private URL forgeWebFingerUrl(URL authorizationEndpoint, DrsMetadata drsMetadata)
            throws MalformedURLException {
        final URL passiveAuthEndpoint = new URL(
                drsMetadata
                        .getIdentityProviderService()
                        .getPassiveAuthEndpoint()
        );

        final String paeDomain = passiveAuthEndpoint.getHost();
        String url =
                "https://"
                        + paeDomain
                        + String.format(
                        "/.well-known/webfinger?resource=%s",
                        authorizationEndpoint.toString()
                );
        Logger.v(TAG, "Validator will use WebFinger URL: " + url);
        return new URL(url);
    }

}
