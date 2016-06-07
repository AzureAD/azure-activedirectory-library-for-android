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

import junit.framework.Assert;

import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Discovery class is not public, so it needs reflection to make a call to
 * non-public class in different package Valid call:
 * https://login.windows.net/common
 * /discovery/instance?api-version=1.0&authorization_endpoint
 * =https%3A%2F%2Flogin
 * .windows.net%2Faaltest.onmicrosoft.com%2Foauth2%2Fauthorize
 */
public class DiscoveryTests extends AndroidTestHelper {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        HttpUrlConnectionFactory.mockedConnection = null;
        super.tearDown();
    }

    // sts.login.windows-int.net
    public void testaddValidHostToList() throws IOException {
        // Use HttpUrlConnection to mock when authority is the given one, discovery returns true.
        // clear mocked connection, check if the authority is valid.
        final Discovery discovery = new Discovery();

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);

        final String response = "{\"tenant_discovery_endpoint\":\"valid endpoint\"}";
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);

        final URL testingURL = new URL("https://login.somewhere.com/path");
        discovery.isValidAuthority(testingURL);

        HttpUrlConnectionFactory.mockedConnection = null;
        assertTrue(discovery.isValidAuthority(testingURL));
    }

    /**
     * instance that is in the list with different path
     *
     * @throws MalformedURLException
     */
    public void testIsValidAuthorityPositiveInList() throws MalformedURLException {
        final Discovery discovery = new Discovery();

        final URL endpointFull = new URL("https://login.windows.net/common/oauth2/authorize");
        Assert.assertTrue(discovery.isValidAuthority(endpointFull));

        final URL endpointInstanceRight = new URL("https://login.windows.net/something/something");
        Assert.assertTrue(discovery.isValidAuthority(endpointInstanceRight));

        final URL endpointInstanceOnly = new URL("https://login.windows.net");
        Assert.assertFalse(discovery.isValidAuthority(endpointInstanceOnly));
    }

    public void testIsValidAuthorityNegative() throws IOException {
        final Discovery discovery = new Discovery();
        final URL endpointFull = new URL("https://login.invalidlogin.net/common/oauth2/authorize");

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);

        final String response = "{\"error_codes\":\"errors\"}";
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(400);

        assertFalse(discovery.isValidAuthority(endpointFull));
    }

    public void testServerInvalidJsonResponse() throws IOException {
        final Discovery discovery = new Discovery();

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);

        final String response = "{invalidJson}";
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);

        TestLogResponse logTrack = new TestLogResponse();
        logTrack.listenForLogMessage("Json parsing error", null);

        final URL endpointFull = new URL("https://login.invalidlogin.net/common/oauth2/authorize");
        assertFalse(discovery.isValidAuthority(endpointFull));
        assertTrue("Exception msg is logged", logTrack.message.contains("Json parsing error"));
    }

    public void testIsValidAuthorityNegative_InvalidUrl() throws MalformedURLException {
        final Discovery discovery = new Discovery();

        final URL endpointFull = new URL("http://login.windows.net/common");
        assertFalse(discovery.isValidAuthority(endpointFull));

        final URL endpointWithQueryParams = new URL(
                "https://login.windows.net/common?resource=2343&client_id=234");
        assertFalse(discovery.isValidAuthority(endpointWithQueryParams));

        final URL endpointWithFragment = new URL("https://login.windows.net/common#token=23434");
        assertFalse(discovery.isValidAuthority(endpointWithFragment));

        final URL adfsEndpoint = new URL("https://fs.ade2eadfs30.com/adfs");
        assertFalse(discovery.isValidAuthority(adfsEndpoint));

        final URL endpointWithInvalidPath = new URL("https://login.windows.net/common/test/test");
        assertTrue(discovery.isValidAuthority(endpointWithInvalidPath));
    }

    /**
     * call instance that is not in the hard coded list.
     */
    @SuppressWarnings("unchecked")
    public void testIsValidAuthorityPositiveRequeryInList() throws IOException {
        final Discovery discovery = new Discovery();
        final URL endpointFull = new URL("https://login.windows-ppe.net/common");

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection;
        Util.prepareMockedUrlConnection(mockedConnection);

        final String response = "{\"tenant_discovery_endpoint\":\"valid endpoint\"}";
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);

        assertTrue(discovery.isValidAuthority(endpointFull));

        HttpUrlConnectionFactory.mockedConnection = null;
        // case sensitivity check
        final URL endpointCaseDifferent = new URL("https://logiN.Windows-PPE.Net/Common");
        assertTrue(discovery.isValidAuthority(endpointCaseDifferent));

        final HttpURLConnection mockedConnection2 = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.mockedConnection = mockedConnection2;
        Util.prepareMockedUrlConnection(mockedConnection);

        final String addHostResponse = "{\"tenant_discovery_endpoint\":\"valid endpoint\"}";
        Mockito.when(mockedConnection.getInputStream()).thenReturn(Util.createInputStream(addHostResponse));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(200);

        final URL endpointTest = new URL("https://login.test-direct-add.net/common");
        discovery.isValidAuthority(endpointTest);

        HttpUrlConnectionFactory.mockedConnection = null;
        assertTrue(discovery.isValidAuthority(endpointTest));
    }
}
