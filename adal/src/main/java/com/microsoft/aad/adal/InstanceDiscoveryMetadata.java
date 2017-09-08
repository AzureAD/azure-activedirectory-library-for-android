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

final class InstanceDiscoveryMetadata {
    private final String mPreferredNetwork;
    private final String mPreferredCache;
    private final List<String> mAliases = new ArrayList();
    private final boolean mIsValidated;

    InstanceDiscoveryMetadata(boolean isValidated) {
        mIsValidated = isValidated;

        mPreferredNetwork = null;
        mPreferredCache = null;
    }

    InstanceDiscoveryMetadata(final String preferredNetwork, final String preferredCache, final List<String> aliases) {
        mPreferredNetwork = preferredNetwork;
        mPreferredCache = preferredCache;
        mAliases.addAll(aliases);
        mIsValidated = true;
    }

    InstanceDiscoveryMetadata(final String preferredNetwork, final String preferredCache) {
        mPreferredNetwork = preferredNetwork;
        mPreferredCache = preferredCache;
        mIsValidated = true;
    }

    String getPreferredNetwork() {
        return mPreferredNetwork;
    }

    String getPreferredCache() {
        return mPreferredCache;
    }

    List<String> getAliases() {
        return mAliases;
    }

    boolean isValidated() {
        return mIsValidated;
    }
}
