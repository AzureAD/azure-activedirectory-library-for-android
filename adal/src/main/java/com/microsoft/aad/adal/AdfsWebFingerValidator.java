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

    /**
     * Used for logging.
     */
    private static final String TAG = "AdfsWebFingerValidator";

    /**
     * Constant identifying trust between two realms.
     */
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
            // create the URL
            URL webFingerUrl = buildWebFingerUrl(authorizationEndpoint, drsMetadata);

            // make the request
            final HttpWebResponse webResponse =
                    getWebrequestHandler()
                            .sendGet(
                                    webFingerUrl,
                                    new HashMap<String, String>()
                            );

            // get the status code
            final int statusCode = webResponse.getStatusCode();

            if (SC_OK != statusCode) { // check 200 OK
                throw new AuthenticationException(
                        ADALError.DRS_FAILED_SERVER_ERROR,
                        "Unexpected error code: [" + statusCode + "]"
                );
            }

            // parse the response
            WebFingerMetadata metadata = parseMetadata(webResponse);

            // verify the trust
            if (!realmIsTrusted(authorizationEndpoint, metadata)) {
                throw new AuthenticationException(ADALError.WEBFINGER_NOT_TRUSTED);
            }

        } catch (IOException e) {
            throw new AuthenticationException(ADALError.IO_EXCEPTION, "Unexpected error", e);
        } catch (JsonSyntaxException e) {
            throw new AuthenticationException(ADALError.JSON_PARSE_ERROR);
        }
    }

    /**
     * Verify that trust is established between IDP and the SP.
     *
     * @param authorizationEndpoint the authorization endpoint used
     * @param metadata              the {@link WebFingerMetadata} to consult
     * @return True, if trust exists: otherwise false.
     */
    private boolean realmIsTrusted(URL authorizationEndpoint, WebFingerMetadata metadata) {
        String href, rel, host;
        for (Link link : metadata.getLinks()) {
            href = link.getHref();
            rel = link.getRel();
            host = authorizationEndpoint.getProtocol() + "://" + authorizationEndpoint.getHost();
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

    /**
     * Create the URL used to retrieve the WebFinger metadata.
     *
     * @param resource    the resource to verify
     * @param drsMetadata the {@link DrsMetadata} to consult
     * @return the URL of the WebFinger document
     * @throws MalformedURLException if the URL could not be constructed
     */
    private URL buildWebFingerUrl(URL resource, DrsMetadata drsMetadata)
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
                        resource.toString()
                );
        Logger.v(TAG, "Validator will use WebFinger URL: " + url);
        return new URL(url);
    }

}
