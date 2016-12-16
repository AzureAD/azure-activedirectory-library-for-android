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
     * @param authority the endpoint used
     * @param metadata  the {@link WebFingerMetadata} to consult
     * @return True, if trust exists: otherwise false.
     */
    static boolean realmIsTrusted(URL authority, WebFingerMetadata metadata) {
        Logger.v(TAG, "Verifying trust: " + authority.toString() + metadata.toString());
        for (Link link : metadata.getLinks()) {
            String href = link.getHref();
            String rel = link.getRel();
            // TODO treat this java.net.URI so that ports and stuff are preserved
            String host = authority.getProtocol() + "://" + authority.getHost();
            if (href.equalsIgnoreCase(host) && rel.equalsIgnoreCase(TRUSTED_REALM_REL)) {
                return true;
            }
        }
        return false;
    }

}
