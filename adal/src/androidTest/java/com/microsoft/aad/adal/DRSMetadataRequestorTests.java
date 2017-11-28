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

import android.support.test.runner.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.net.HttpUrlConnectionFactory;
import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class DRSMetadataRequestorTests extends AndroidTestHelper {

    private static final String RESPONSE = "{\n"
            +
            "  \"DeviceRegistrationService\": {\n"
            +
            "    \"RegistrationEndpoint\": \"https://fs.lindft6.com/EnrollmentServer/DeviceEnrollmentWebService.svc\",\n"
            +
            "    \"RegistrationResourceId\": \"urn:ms-drs:UUID\",\n"
            +
            "    \"ServiceVersion\": \"1.0\"\n"
            +
            "  },\n"
            +
            "  \"AuthenticationService\": {\n"
            +
            "    \"OAuth2\": {\n"
            +
            "      \"AuthCodeEndpoint\": \"https://fs.lindft6.com/adfs/oauth2/authorize\",\n"
            +
            "      \"TokenEndpoint\": \"https://fs.lindft6.com/adfs/oauth2/token\"\n"
            +
            "    }\n"
            +
            "  },\n"
            +
            "  \"IdentityProviderService\": {\n"
            +
            "    \"PassiveAuthEndpoint\": \"https://fs.lindft6.com/adfs/ls\"\n"
            +
            "  }\n"
            +
            "}";

    private static final String TEST_ADFS = "https://fs.lindft6.com/adfs/ls";
    private static final String DOMAIN = "lindft6.com";

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        super.tearDown();
    }

    @Test
    public void testRequestMetadata() throws IOException, AuthenticationException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        Util.prepareMockedUrlConnection(mockedConnection);

        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(RESPONSE));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        DRSMetadataRequestor requestor = new DRSMetadataRequestor();

        DRSMetadata metadata = requestor.requestMetadata(DOMAIN);

        assertEquals(
                TEST_ADFS,
                metadata.getIdentityProviderService().getPassiveAuthEndpoint()
        );
    }

    @Test
    public void testRequestMetadataThrows() throws IOException, AuthenticationException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        Util.prepareMockedUrlConnection(mockedConnection);

        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(RESPONSE));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        DRSMetadataRequestor requestor = new DRSMetadataRequestor();

        try {
            DRSMetadata metadata = requestor.requestMetadata(DOMAIN);
            fail();
        } catch (AuthenticationException e) {
            // should throw
            return;
        }
    }

    @Test
    public void testParseMetadata() throws AuthenticationException {
        HttpWebResponse mockWebResponse = Mockito.mock(HttpWebResponse.class);
        Mockito.when(mockWebResponse.getBody()).thenReturn(RESPONSE);

        DRSMetadata metadata = new DRSMetadataRequestor().parseMetadata(mockWebResponse);

        assertEquals(
                TEST_ADFS,
                metadata.getIdentityProviderService().getPassiveAuthEndpoint()
        );
    }

    @Test
    public void testBuildRequestUrlByTypeOnPrem() {
        final String expected = "https://enterpriseregistration.lindft6.com/enrollmentserver/contract?api-version=1.0";
        DRSMetadataRequestor requestor = new DRSMetadataRequestor();
        assertEquals(
                expected,
                requestor.buildRequestUrlByType(
                        DRSMetadataRequestor.Type.ON_PREM,
                        DOMAIN
                )
        );
    }

    @Test
    public void testBuildRequestUrlByTypeCloud() {
        final String expected = "https://enterpriseregistration.windows.net/lindft6.com/enrollmentserver/contract?api-version=1.0";
        DRSMetadataRequestor requestor = new DRSMetadataRequestor();
        assertEquals(
                expected,
                requestor.buildRequestUrlByType(
                        DRSMetadataRequestor.Type.CLOUD,
                        DOMAIN
                )
        );
    }
}
