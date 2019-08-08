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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class AuthenticationRequestTests {
    static final int REQUEST_ID = 1234;

    @Test
    public void testAuthenticationRequestParams() {
        AuthenticationRequest request = new AuthenticationRequest();
        assertNull("authority is null", request.getAuthority());


        // call with params
        request = new AuthenticationRequest("authority1", "resource2", "client3", false);
        assertEquals("authority is same", "authority1", request.getAuthority());
        assertEquals("resource is same", "resource2", request.getResource());
        assertEquals("client is same", "client3", request.getClientId());

        request = new AuthenticationRequest("authority31", "resource32", "client33", "redirect34", "loginhint35", false);
        assertEquals("authority is same", "authority31", request.getAuthority());
        assertEquals("resource is same", "resource32", request.getResource());
        assertEquals("client is same", "client33", request.getClientId());
        assertEquals("redirect is same", "redirect34", request.getRedirectUri());
        assertEquals("loginhint is same", "loginhint35", request.getLoginHint());

        UUID correlationId = UUID.randomUUID();
        request = new AuthenticationRequest("authority41", "resource42", "client43", "redirect44", "loginhint45", correlationId, false);
        assertEquals("authority is same", "authority41", request.getAuthority());
        assertEquals("resource is same", "resource42", request.getResource());
        assertEquals("client is same", "client43", request.getClientId());
        assertEquals("redirect is same", "redirect44", request.getRedirectUri());
        assertEquals("loginhint is same", "loginhint45", request.getLoginHint());
        assertEquals("correlationId is same", correlationId, request.getCorrelationId());


        request = new AuthenticationRequest("authority51", "resource52", "client53", "redirect54",
                "loginhint55", PromptBehavior.Always, "extraQueryParam56", correlationId, false, "testClaims");
        assertEquals("authority is same", "authority51", request.getAuthority());
        assertEquals("resource is same", "resource52", request.getResource());
        assertEquals("client is same", "client53", request.getClientId());
        assertEquals("redirect is same", "redirect54", request.getRedirectUri());
        assertEquals("loginhint is same", "loginhint55", request.getLoginHint());
        assertEquals("ExtraQueryParams is same", "extraQueryParam56", request.getExtraQueryParamsAuthentication());
        assertEquals("PromptBehavior is same", PromptBehavior.Always, request.getPrompt());
        assertEquals("correlationId is same", correlationId, request.getCorrelationId());
        assertEquals("claimsChallenge is same", "testClaims", request.getClaimsChallenge());
    }

    @Test
    public void testRequestId() {
        final AuthenticationRequest request = new AuthenticationRequest("authority1", "resource2", "client3", false);
        request.setRequestId(REQUEST_ID);

        assertEquals("Same RequestId", REQUEST_ID, request.getRequestId());
    }

    @Test
    public void testGetUpnSuffix() {
        AuthenticationRequest authenticationRequest =
                new AuthenticationRequest(
                        "NA", // authority
                        "NA", // resource
                        "NA", // client
                        "NA", // redirect
                        "user@tenant.internet", // loginhint,
                        false
                );
        assertEquals("tenant.internet", authenticationRequest.getUpnSuffix());
    }

    @Test
    public void testGetUpnSuffixNull() {
        AuthenticationRequest authenticationRequest =
                new AuthenticationRequest(
                        "NA", // authority
                        "NA", // resource
                        "NA", // client
                        "NA", // redirect
                        "user", // loginhint,
                        false
                );
        assertEquals(null, authenticationRequest.getUpnSuffix());
    }
}
