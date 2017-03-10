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

import android.test.suitebuilder.annotation.SmallTest;

import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WebFingerMetadataRequestorTests extends AndroidTestHelper {

    private static final String RESPONSE = "{\n"
            +
            "  \"subject\": \"https://fs.lindft6.com\",\n"
            +
            "  \"links\": [\n"
            +
            "    {\n"
            +
            "      \"rel\": \"http://schemas.microsoft.com/rel/trusted-realm\",\n"
            +
            "      \"href\": \"https://fs.lindft6.com\"\n"
            +
            "    }\n"
            +
            "  ]\n"
            +
            "}";

    private static final String DOMAIN = "https://fs.lindft6.com";

    private static final DRSMetadata DRS_METADATA = new DRSMetadata();

    static {
        IdentityProviderService identityProviderService = new IdentityProviderService();
        identityProviderService.setPassiveAuthEndpoint(DOMAIN + "/adfs/ls");
        DRS_METADATA.setIdentityProviderService(identityProviderService);
    }

    @SmallTest
    public void testRequestMetadata() throws IOException, AuthenticationException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        Util.prepareMockedUrlConnection(mockedConnection);

        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(RESPONSE));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        WebFingerMetadataRequestor requestor = new WebFingerMetadataRequestor();

        WebFingerMetadataRequestParameters parameters = new WebFingerMetadataRequestParameters(
                new URL(DOMAIN),
                DRS_METADATA
        );

        WebFingerMetadata metadata = requestor.requestMetadata(parameters);

        assertEquals("https://fs.lindft6.com", metadata.getSubject());
        assertNotNull(metadata.getLinks());
        assertEquals(1, metadata.getLinks().size());
        assertEquals(
                "http://schemas.microsoft.com/rel/trusted-realm",
                metadata.getLinks().get(0).getRel()
        );
        assertEquals(
                "https://fs.lindft6.com",
                metadata.getLinks().get(0).getHref()
        );
    }

    @SmallTest
    public void testRequestMetadataThrows() throws IOException, AuthenticationException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        Util.prepareMockedUrlConnection(mockedConnection);

        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(RESPONSE));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        WebFingerMetadataRequestor requestor = new WebFingerMetadataRequestor();

        WebFingerMetadataRequestParameters parameters = new WebFingerMetadataRequestParameters(
                new URL(DOMAIN),
                DRS_METADATA
        );

        try {
            WebFingerMetadata metadata = requestor.requestMetadata(parameters);
        } catch (AuthenticationException e) {
            // should throw
            return;
        }
    }

    @SmallTest
    public void testParseMetadata() throws AuthenticationException {
        HttpWebResponse mockWebResponse = Mockito.mock(HttpWebResponse.class);
        Mockito.when(mockWebResponse.getBody()).thenReturn(RESPONSE);

        WebFingerMetadata metadata = new WebFingerMetadataRequestor().parseMetadata(mockWebResponse);

        assertEquals("https://fs.lindft6.com", metadata.getSubject());
        assertNotNull(metadata.getLinks());
        assertEquals(1, metadata.getLinks().size());
        assertEquals(
                "http://schemas.microsoft.com/rel/trusted-realm",
                metadata.getLinks().get(0).getRel()
        );
        assertEquals(
                "https://fs.lindft6.com",
                metadata.getLinks().get(0).getHref()
        );
    }

    @SmallTest
    public void testBuildWebFingerUrl() throws MalformedURLException {
        final URL expected = new URL("https://fs.lindft6.com/.well-known/webfinger?resource=https://fs.lindft6.com");
        final URL wfURL = WebFingerMetadataRequestor.buildWebFingerUrl(new URL(DOMAIN), DRS_METADATA);
        assertEquals(expected, wfURL);
    }
}
