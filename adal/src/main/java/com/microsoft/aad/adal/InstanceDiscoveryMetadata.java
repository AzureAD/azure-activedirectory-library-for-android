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

import java.util.ArrayList;
import java.util.List;

/**
 * Hold the instance discovery metadata returned from discovery endpoint, which includes the preferred network location,
 * preferred cache location and all the associated aliased host related to the passed in authority.
 */
final class InstanceDiscoveryMetadata {
    private final String mPreferredNetwork;
    private final String mPreferredCache;
    private final List<String> mAliases = new ArrayList<String>();
    private final boolean mIsValidated;

    /**
     * Constructor for {@link InstanceDiscoveryMetadata}, indicates that metadata not returned back from server or instance
     * discovery fails.
     * @param isValidated
     */
    InstanceDiscoveryMetadata(boolean isValidated) {
        mIsValidated = isValidated;

        mPreferredNetwork = null;
        mPreferredCache = null;
    }

    /**
     * Constructor for {@link InstanceDiscoveryMetadata}, indicates that instance discovery succeeds and metadata is returned back.
     * @param preferredNetwork
     * @param preferredCache
     * @param aliases
     */
    InstanceDiscoveryMetadata(final String preferredNetwork, final String preferredCache, final List<String> aliases) {
        mPreferredNetwork = preferredNetwork;
        mPreferredCache = preferredCache;
        mAliases.addAll(aliases);
        mIsValidated = true;
    }

    /**
     * Constructor for {@link InstanceDiscoveryMetadata}, indicates instance discovery succeeds. Preferred network and preferred cache
     * are returned back but aliased location is not.
     * @param preferredNetwork
     * @param preferredCache
     */
    InstanceDiscoveryMetadata(final String preferredNetwork, final String preferredCache) {
        mPreferredNetwork = preferredNetwork;
        mPreferredCache = preferredCache;
        mIsValidated = true;
    }

    public String getPreferredNetwork() {
        return mPreferredNetwork;
    }

    public String getPreferredCache() {
        return mPreferredCache;
    }

    public List<String> getAliases() {
        return mAliases;
    }

    public boolean isValidated() {
        return mIsValidated;
    }
}