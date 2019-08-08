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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link AuthorityValidationMetadataCache}.
 */
@RunWith(AndroidJUnit4.class)
public class AuthorityValidationMetadataCacheTest extends AndroidTestHelper {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
    }

    @Test
    public void testProcessInstanceDiscoveryMetadata() throws MalformedURLException, JSONException {
        final URL authorityURL = new URL("https://login.windows.net/common");
        AuthorityValidationMetadataCache.processInstanceDiscoveryMetadata(authorityURL, getDiscoveryResponse());

        Map<String, InstanceDiscoveryMetadata> authorityValidationMap = AuthorityValidationMetadataCache.getAuthorityValidationMetadataCache();

        final int expectedMapSize = 5;
        Assert.assertTrue(authorityValidationMap.size() == expectedMapSize);
        Assert.assertTrue(authorityValidationMap.containsKey("login.windows.net"));
        Assert.assertTrue(authorityValidationMap.containsKey("login.microsoftonline.com"));
        Assert.assertTrue(authorityValidationMap.containsKey("sts.microsoft.com"));
        Assert.assertTrue(authorityValidationMap.containsKey("login.microsoft.com"));
        Assert.assertTrue(authorityValidationMap.containsKey("login.microsoftonline.de"));
    }

    @Test
    public void testMetadataNotReturned() throws MalformedURLException, JSONException {
        final URL authorityUrl = new URL("https://login.windows.net/common");
        AuthorityValidationMetadataCache.processInstanceDiscoveryMetadata(authorityUrl, getDiscoveryResponseWithNoMetadata());

        Map<String, InstanceDiscoveryMetadata> authorityValidationMap = AuthorityValidationMetadataCache.getAuthorityValidationMetadataCache();
        Assert.assertTrue(authorityValidationMap.size() == 1);
        Assert.assertTrue(authorityValidationMap.containsKey("login.windows.net"));
    }

    private Map<String, String> getDiscoveryResponseWithNoMetadata() {
        final Map<String, String> discoveryResponse = new HashMap<>();
        discoveryResponse.put(AuthorityValidationMetadataCache.TENANT_DISCOVERY_ENDPOINT, "valid_endpoint");

        return discoveryResponse;
    }

    static Map<String, String> getDiscoveryResponse() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put(AuthorityValidationMetadataCache.TENANT_DISCOVERY_ENDPOINT, "valid_endpoint");
        metadata.put(AuthorityValidationMetadataCache.META_DATA, getMetadata());

        return metadata;
    }

    static String getMetadata() {
        final String metadata = "["
                + "{"
                + "\"preferred_network\": \"login.microsoftonline.com\","
                + "\"preferred_cache\": \"login.windows.net\","
                + "\"aliases\": ["
                + "\"login.microsoftonline.com\","
                + "\"login.windows.net\","
                + "\"login.microsoft.com\","
                + "\"sts.microsoft.com\""
                + "]"
                + "},"
                + "{"
                + "\"preferred_network\": \"login.microsoftonline.de\","
                + "\"preferred_cache\": \"login.microsoftonline.de\","
                + "\"aliases\": ["
                + "\"login.microsoftonline.de\""
                + "]"
                + "}"
                + "]";

        return metadata;
    }
}
