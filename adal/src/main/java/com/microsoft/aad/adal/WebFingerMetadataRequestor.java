package com.microsoft.aad.adal;

import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

class WebFingerMetadataRequestor
        extends AbstractMetadataRequestor<WebFingerMetadata, WebFingerMetadataRequestParameters> {

    /**
     * Used for logging.
     */
    private static final String TAG = "WebFingerMetadataRequestor";

    @Override
    WebFingerMetadata requestMetadata(WebFingerMetadataRequestParameters webFingerMetadataRequestParameters)
            throws AuthenticationException {
        final URL domain = webFingerMetadataRequestParameters.getDomain();
        final DrsMetadata drsMetadata = webFingerMetadataRequestParameters.getDrsMetadata();
        Logger.v(TAG, "Validating authority for auth endpoint: " + domain.toString());
        try {
            // create the URL
            URL webFingerUrl = buildWebFingerUrl(domain, drsMetadata);

            // make the request
            final HttpWebResponse webResponse =
                    getWebrequestHandler()
                            .sendGet(
                                    webFingerUrl,
                                    new HashMap<String, String>()
                            );

            // get the status code
            final int statusCode = webResponse.getStatusCode();

            if (HttpURLConnection.HTTP_OK != statusCode) { // check 200 OK
                throw new AuthenticationException(
                        ADALError.DRS_FAILED_SERVER_ERROR,
                        "Unexpected error code: [" + statusCode + "]"
                );
            }

            // parse the response
            return parseMetadata(webResponse);

        } catch (IOException e) {
            throw new AuthenticationException(ADALError.IO_EXCEPTION, "Unexpected error", e);
        }
    }

    /**
     * Deserializes {@link HttpWebResponse} bodies into {@link WebFingerMetadata}.
     *
     * @param webResponse the HttpWebResponse to deserialize
     * @return the parsed response
     */
    WebFingerMetadata parseMetadata(HttpWebResponse webResponse) throws AuthenticationException {
        Logger.v(TAG, "Parsing WebFinger response");
        try {
            return parser().fromJson(webResponse.getBody(), WebFingerMetadata.class);
        } catch (JsonSyntaxException e) {
            throw new AuthenticationException(ADALError.JSON_PARSE_ERROR);
        }
    }

    /**
     * Create the URL used to retrieve the WebFinger metadata.
     *
     * @param resource    the resource to verify
     * @param drsMetadata the {@link DrsMetadata} to consult
     * @return the URL of the WebFinger document
     * @throws MalformedURLException if the URL could not be constructed
     */
    private static URL buildWebFingerUrl(URL resource, DrsMetadata drsMetadata)
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
