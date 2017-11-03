//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.adal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Hold the authority validation metadata.
 */

final class AuthorityValidationMetadataCache {
    private static final String TAG = AuthorityValidationMetadataCache.class.getSimpleName();

    static final String TENANT_DISCOVERY_ENDPOINT = "tenant_discovery_endpoint";

    static final String META_DATA = "metadata";

    private static final String PREFERRED_NETWORK = "preferred_network";

    private static final String PREFERRED_CACHE = "preferred_cache";

    private static final String ALIASES = "aliases";

    private static ConcurrentMap<String, InstanceDiscoveryMetadata> sAadAuthorityHostMetadata = new ConcurrentHashMap<>();

    private AuthorityValidationMetadataCache() {
        // Utility class, no public constructor
    }

    static boolean containsAuthorityHost(final URL authorityUrl) {
        return sAadAuthorityHostMetadata.containsKey(authorityUrl.getHost().toLowerCase(Locale.US));
    }

    static boolean isAuthorityValidated(final URL authorityUrl) {
        return containsAuthorityHost(authorityUrl) && getCachedInstanceDiscoveryMetadata(authorityUrl).isValidated();
    }

    static InstanceDiscoveryMetadata getCachedInstanceDiscoveryMetadata(final URL authorityUrl) {
        return sAadAuthorityHostMetadata.get(authorityUrl.getHost().toLowerCase(Locale.US));
    }

    static void processInstanceDiscoveryMetadata(final URL authorityUrl, final Map<String, String> discoveryResponse) throws JSONException {
        final String methodName = ":processInstanceDiscoveryMetadata";
        final boolean isTenantDiscoveryEndpointReturned = discoveryResponse.containsKey(TENANT_DISCOVERY_ENDPOINT);
        final String metadata = discoveryResponse.get(META_DATA);
        final String authorityHost = authorityUrl.getHost().toLowerCase(Locale.US);

        if (!isTenantDiscoveryEndpointReturned) {
            sAadAuthorityHostMetadata.put(authorityHost, new InstanceDiscoveryMetadata(false));
            return;
        }

        // No metadata is returned, fill in the metadata with passed
        if (StringExtensions.isNullOrBlank(metadata)) {
            Logger.v(TAG + methodName, "No metadata returned from instance discovery.");
            sAadAuthorityHostMetadata.put(authorityHost, new InstanceDiscoveryMetadata(authorityHost, authorityHost));
            return;
        }

        processInstanceDiscoveryResponse(metadata);
    }

    static void updateInstanceDiscoveryMap(final String host, final InstanceDiscoveryMetadata metadata) {
        sAadAuthorityHostMetadata.put(host.toLowerCase(Locale.US), metadata);
    }

    static Map<String, InstanceDiscoveryMetadata> getAuthorityValidationMetadataCache() {
        return Collections.unmodifiableMap(sAadAuthorityHostMetadata);
    }

    static void clearAuthorityValidationCache() {
        sAadAuthorityHostMetadata.clear();
    }

    private static void processInstanceDiscoveryResponse(final String metadata) throws JSONException {
        final JSONArray jsonArray = new JSONArray(metadata);
        for (int i = 0; i < jsonArray.length(); i++) {
            final InstanceDiscoveryMetadata instanceDiscoveryMetadata = processSingleJsonArray(new JSONObject(jsonArray.get(i).toString()));
            final List<String> aliases = instanceDiscoveryMetadata.getAliases();
            for (final String alias : aliases) {
                sAadAuthorityHostMetadata.put(alias.toLowerCase(Locale.US), instanceDiscoveryMetadata);
            }
        }
    }

    private static InstanceDiscoveryMetadata processSingleJsonArray(final JSONObject jsonObject) throws JSONException {
        final String preferredNetwork = jsonObject.getString(PREFERRED_NETWORK);
        final String preferredCache = jsonObject.getString(PREFERRED_CACHE);
        final JSONArray aliasArray = jsonObject.getJSONArray(ALIASES);

        final List<String> aliases = new ArrayList<>();
        for (int i = 0; i < aliasArray.length(); i++) {
            aliases.add(aliasArray.getString(i));
        }

        return new InstanceDiscoveryMetadata(preferredNetwork, preferredCache, aliases);
    }
}
