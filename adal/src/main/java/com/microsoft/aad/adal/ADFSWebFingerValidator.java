package com.microsoft.aad.adal;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates trusts between authorities and ADFS instances using DRS metadata and WebFinger.
 */
final class ADFSWebFingerValidator {

    private ADFSWebFingerValidator() {
        // utility class
    }

    /**
     * Used for logging.
     */
    private static final String TAG = "AdfsWebFingerValidator";

    /**
     * Constant identifying trust between two realms.
     */
    private static final URI TRUSTED_REALM_REL;

    static {
        try {
            TRUSTED_REALM_REL = new URI("http://schemas.microsoft.com/rel/trusted-realm");
        } catch (URISyntaxException e) {
            // will not throw
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify that trust is established between IDP and the SP.
     *
     * @param authority the endpoint used
     * @param metadata  the {@link WebFingerMetadata} to consult
     * @return True, if trust exists: otherwise false.
     */
    static boolean realmIsTrusted(final URI authority, final WebFingerMetadata metadata) {
        if (authority == null) {
            throw new IllegalArgumentException("Authority cannot be null");
        }

        if (metadata == null) {
            throw new IllegalArgumentException("WebFingerMetadata cannot be null");
        }

        Logger.v(TAG, "Verifying trust: " + authority.toString() + metadata.toString());
        if (metadata.getLinks() != null) {
            for (Link link : metadata.getLinks()) {
                try {
                    URI href = new URI(link.getHref());
                    URI rel = new URI(link.getRel());
                    if (href.getScheme().equalsIgnoreCase(authority.getScheme())
                            && href.getAuthority().equalsIgnoreCase(authority.getAuthority())
                            && rel.equals(TRUSTED_REALM_REL)) {
                        return true;
                    }
                } catch (URISyntaxException e) {
                    // noop
                    continue;
                }
            }
        }
        return false;
    }

}
