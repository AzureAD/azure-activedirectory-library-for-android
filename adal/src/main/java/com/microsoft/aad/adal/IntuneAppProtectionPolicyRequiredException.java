// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

public class IntuneAppProtectionPolicyRequiredException extends AuthenticationException {

    private final String mAccountUpn;
    private final String mAccountUserId;
    private final String mTenantId;
    private final String mAuthorityUrl;

    /**
     * @param msg           Details related to the error such as query string, request
     *                      info
     * @param accountUpn    The UPN of the account, needed for Intune MAM enrollment.
     * @param accountUserId The unique ID of the account, needed for Intune MAM enrollment.
     * @param tenantId      The tenant ID, needed of Intune MAM enrollment.
     * @param authorityUrl  The authority URL, used by Intune MAM enrollment to support
     *                      sovereign clouds.  If null, default public cloud will be used.
     */
    public IntuneAppProtectionPolicyRequiredException(final String msg, String accountUpn, String accountUserId, String tenantId, String authorityUrl) {
        super(ADALError.AUTH_FAILED_INTUNE_POLICY_REQUIRED, msg);
        this.mAccountUpn = accountUpn;
        this.mAccountUserId = accountUserId;
        this.mTenantId = tenantId;
        this.mAuthorityUrl = authorityUrl;
    }

    public String getAccountUpn() {
        return mAccountUpn;
    }

    public String getAccountUserId() {
        return mAccountUserId;
    }

    public String getTenantId() {
        return mTenantId;
    }

    public String getAuthorityURL() {
        return mAuthorityUrl;
    }
}
