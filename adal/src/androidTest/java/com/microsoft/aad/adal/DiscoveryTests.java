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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.common.adal.internal.net.HttpUrlConnectionFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Discovery class is not public, so it needs reflection to make a call to
 * non-public class in different package Valid call:
 * https://login.windows.net/common
 * /discovery/instance?api-version=1.0&authorization_endpoint
 * =https%3A%2F%2Flogin
 * .windows.net%2Faaltest.onmicrosoft.com%2Foauth2%2Fauthorize
 */
@RunWith(AndroidJUnit4.class)
public class DiscoveryTests extends AndroidTestHelper {

    @Before
    public void setUp() throws Exception {
        AuthorityValidationMetadataCache.clearAuthorityValidationCache();
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        super.tearDown();
    }

    // sts.login.windows-int.net
    @Test
    public void testaddValidHostToList() throws IOException {
        // Use HttpUrlConnection to mock when authority is the given one, discovery returns true.
        // clear mocked connection, check if the authority is valid.
        final FileMockContext context =
                new FileMockContext(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                .getContext());
        final Discovery discovery = new Discovery(context);

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        final String response = "{\"tenant_discovery_endpoint\":\"valid endpoint\"}";
        Mockito.when(mockedConnection.getInputStream())
                .thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        final URL testingURL = new URL("https://login.somewhere.com/path");
        try {
            discovery.validateAuthority(testingURL);
        } catch (final AuthenticationException e) {
            fail("non-expected exception. ");
        } finally {
            HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        }
    }

    /**
     * instance that is in the list with different path
     *
     * @throws MalformedURLException
     */
    @Test
    public void testIsValidAuthorityPositiveInList() throws MalformedURLException {
        final FileMockContext context =
                new FileMockContext(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                .getContext());
        final Discovery discovery = new Discovery(context);

        final URL endpointFull = new URL("https://login.windows.net/common/oauth2/authorize");
        try {
            discovery.validateAuthority(endpointFull);
        } catch (final AuthenticationException e) {
            fail("unexpected exception. ");
        }

        final URL endpointInstanceRight = new URL("https://login.windows.net/something/something");
        try {
            discovery.validateAuthority(endpointInstanceRight);
        } catch (final AuthenticationException e) {
            fail("unexpected exception.");
        }

        final URL endpointInstanceOnly = new URL("https://login.windows.net");
        try {
            discovery.validateAuthority(endpointInstanceOnly);
        } catch (final AuthenticationException e) {
            assertNotNull(e);
            assertTrue(e.getCode() == ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE);
        }
    }

    @Test
    public void testIsValidAuthorityNegative() throws IOException {
        final FileMockContext context =
                new FileMockContext(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                .getContext());
        final Discovery discovery = new Discovery(context);
        final URL endpointFull = new URL("https://login.invalidlogin.net/common/oauth2/authorize");

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        final String response = "{\"error_codes\":\"errors\"}";
        Mockito.when(mockedConnection.getInputStream())
                .thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode())
                .thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);

        try {
            discovery.validateAuthority(endpointFull);
            fail();
        } catch (AuthenticationException e) {
            assertNotNull(e);
            assertTrue(e.getCode().equals(ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE));
        }
    }

    @Test
    public void testServerInvalidJsonResponse() throws IOException {
        final FileMockContext context =
                new FileMockContext(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                .getContext());
        final Discovery discovery = new Discovery(context);

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        final String response = "{invalidJson}";
        Mockito.when(mockedConnection.getInputStream())
                .thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        final URL endpointFull = new URL("https://login.invalidlogin.net/common/oauth2/authorize");
        try {
            discovery.validateAuthority(endpointFull);
            fail();
        } catch (final AuthenticationException e) {
            assertNotNull(e);
            assertTrue(e.getCode() == ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE);
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof JSONException);
        }
    }

    @Test
    public void testIsValidAuthorityNegativeInvalidUrl() throws MalformedURLException {
        final FileMockContext context =
                new FileMockContext(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                .getContext());
        final Discovery discovery = new Discovery(context);

        final URL endpointFull = new URL("http://login.windows.net/common");
        try {
            discovery.validateAuthority(endpointFull);
            fail();
        } catch (final AuthenticationException e) {
            assertNotNull(e);
            assertTrue(e.getCode() == ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE);
        }

        final URL endpointWithQueryParams =
                new URL("https://login.windows.net/common?resource=2343&client_id=234");
        try {
            discovery.validateAuthority(endpointWithQueryParams);
            fail();
        } catch (final AuthenticationException e) {
            assertNotNull(e);
            assertTrue(e.getCode() == ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE);
        }

        final URL endpointWithFragment = new URL("https://login.windows.net/common#token=23434");
        try {
            discovery.validateAuthority(endpointWithFragment);
            fail();
        } catch (final AuthenticationException e) {
            assertNotNull(e);
            assertTrue(e.getCode() == ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE);
        }

        final URL adfsEndpoint = new URL("https://fs.ade2eadfs30.com/adfs");
        try {
            discovery.validateAuthority(adfsEndpoint);
        } catch (final AuthenticationException e) {
            assertNotNull(e);
            assertTrue(e.getCode() == ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_INSTANCE);
        }

        final URL endpointWithInvalidPath = new URL("https://login.windows.net/common/test/test");
        try {
            discovery.validateAuthority(endpointWithInvalidPath);
        } catch (final AuthenticationException e) {
            fail();
        }
    }

    /**
     * call instance that is not in the hard coded list.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIsValidAuthorityPositiveRequeryInList() throws IOException {
        final FileMockContext context =
                new FileMockContext(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                .getContext());
        final Discovery discovery = new Discovery(context);
        final URL endpointFull = new URL("https://login.windows-ppe.net/common");

        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        final String response = "{\"tenant_discovery_endpoint\":\"valid endpoint\"}";
        Mockito.when(mockedConnection.getInputStream())
                .thenReturn(Util.createInputStream(response));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        try {
            discovery.validateAuthority(endpointFull);
        } catch (final AuthenticationException e) {
            fail();
        } finally {
            HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        }

        HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
        // case sensitivity check
        final URL endpointCaseDifferent = new URL("https://logiN.Windows-PPE.Net/Common");
        try {
            discovery.validateAuthority(endpointCaseDifferent);
        } catch (final AuthenticationException e) {
            fail();
        }

        final HttpURLConnection mockedConnection2 = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection2);
        Util.prepareMockedUrlConnection(mockedConnection);

        final String addHostResponse = "{\"tenant_discovery_endpoint\":\"valid endpoint\"}";
        Mockito.when(mockedConnection.getInputStream())
                .thenReturn(Util.createInputStream(addHostResponse));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        final URL endpointTest = new URL("https://login.test-direct-add.net/common");
        try {
            discovery.validateAuthority(endpointTest);

            HttpUrlConnectionFactory.setMockedHttpUrlConnection(null);
            discovery.validateAuthority(endpointTest);
        } catch (final AuthenticationException e) {
            fail();
        }
    }

    // Test when there are two requests from different threads trying to do authority validation for
    // the same authority, only
    // one hit network.
    @Test
    public void testMultiValidateAuthorityRequestsInDifferentThreads()
            throws IOException, InterruptedException, ExecutionException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        Mockito.when(mockedConnection.getInputStream())
                .thenReturn(Util.createInputStream(getDiscoveryResponse()));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        Callable<Void> task =
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        final FileMockContext context =
                                new FileMockContext(
                                        androidx.test.platform.app.InstrumentationRegistry
                                                .getInstrumentation()
                                                .getContext());
                        final Discovery discovery = new Discovery(context);
                        discovery.validateAuthority(new URL("https://login.windows.net/common"));

                        return null;
                    }
                };

        final List<Callable<Void>> tasks = Collections.nCopies(2, task);
        final List<Future<Void>> results = executorService.invokeAll(tasks);
        for (final Future<Void> result : results) {
            result.get();
        }

        Mockito.verify(mockedConnection, Mockito.times(1)).getInputStream();
    }

    /**
     * Verified scenario:
     * When an authority is valid and metadata is returned:
     * a. Subsequent requests for any aliases provided in the metadata do not result in further validation network requests
     * being made for the process lifetime
     */
    @Test
    public void testAuthorityInAliasedList() throws IOException {
        final HttpURLConnection mockedConnection = Mockito.mock(HttpURLConnection.class);
        HttpUrlConnectionFactory.setMockedHttpUrlConnection(mockedConnection);
        Util.prepareMockedUrlConnection(mockedConnection);

        Mockito.when(mockedConnection.getInputStream())
                .thenReturn(Util.createInputStream(getDiscoveryResponse()));
        Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        final String authorityUrl1 = "https://login.windows.net/sometenant.onmicrosoft.com";
        try {
            final FileMockContext context =
                    new FileMockContext(
                            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                    .getContext());
            final Discovery discovery = new Discovery(context);
            discovery.validateAuthority(new URL(authorityUrl1));
        } catch (AuthenticationException e) {
            fail();
        }

        // do authority validation for aliased authority
        final String aliasedAuthorityUrl =
                "https://login.microsoftonline.com/sometenant.onmicrosoft.com";
        try {
            final FileMockContext context =
                    new FileMockContext(
                            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                    .getContext());
            final Discovery discovery = new Discovery(context);
            discovery.validateAuthority(new URL(aliasedAuthorityUrl));
        } catch (AuthenticationException e) {
            fail();
        }

        final String aliasedAuthorityUrl2 = "https://sts.microsoft.com/sometenant.onmicrosoft.com";
        try {
            final FileMockContext context =
                    new FileMockContext(
                            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                    .getContext());
            final Discovery discovery = new Discovery(context);
            discovery.validateAuthority(new URL(aliasedAuthorityUrl2));
        } catch (AuthenticationException e) {
            fail();
        }

        Mockito.verify(mockedConnection, Mockito.times(1)).getInputStream();
    }

    @Test
    public void testValidateAuthorityFailedWithoutNetwork() throws IOException {
        final FileMockContext context =
                new FileMockContext(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                                .getContext());
        final Discovery discovery = new Discovery(context);
        context.setConnectionAvailable(false);
        final URL endpointFull = new URL("https://login.invalidlogin.net/common/oauth2/authorize");

        try {
            discovery.validateAuthority(endpointFull);
            fail();
        } catch (AuthenticationException e) {
            assertNotNull(e);
            assertTrue(e.getCode().equals(ADALError.DEVICE_CONNECTION_IS_NOT_AVAILABLE));
        }
    }

    static String getDiscoveryResponse() {
        final Map<String, String> discoveryResponse =
                AuthorityValidationMetadataCacheTest.getDiscoveryResponse();
        final JSONObject discoveryResponseJsonObject = new JSONObject(discoveryResponse);

        return discoveryResponseJsonObject.toString();
    }
}
