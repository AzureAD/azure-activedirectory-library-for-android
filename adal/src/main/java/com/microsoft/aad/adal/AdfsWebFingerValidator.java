package com.microsoft.aad.adal;

import java.net.URL;

/**
 * Validates trusts between authorities and ADFS instances using DRS metadata and WebFinger.
 */
final class AdfsWebFingerValidator {

    private AdfsWebFingerValidator() {
        // utility class
    }

    /**
     * Used for logging.
     */
    private static final String TAG = "AdfsWebFingerValidator";

    /**
     * Constant identifying trust between two realms.
     */
    private static final String TRUSTED_REALM_REL = "http://schemas.microsoft.com/rel/trusted-realm";

    /**
     * Verify that trust is established between IDP and the SP.
     *
     * @param authorizationEndpoint the authorization endpoint used
     * @param metadata              the {@link WebFingerMetadata} to consult
     * @return True, if trust exists: otherwise false.
     */
    static boolean realmIsTrusted(URL authorizationEndpoint, WebFingerMetadata metadata) {
        Logger.v(TAG, "Verifying trust: " + authorizationEndpoint.toString() + metadata.toString());
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

}
