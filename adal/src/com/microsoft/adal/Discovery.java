/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.json.JSONException;

import android.net.Uri;

/**
 * Instance and Tenant discovery. It takes authorization endpoint and sends
 * query to known hard coded instances to get tenant discovery endpoint. If
 * instance is valid, it will return tenant discovery endpoint info. Instance
 * discovery endpoint does not verify tenant info, so Discovery implementation
 * sends common as a tenant name. Discovery checks only authorization endpoint.
 * It does not do tenant verification. Initialize and call from UI thread.
 */
final class Discovery implements IDiscovery {

    private final static String TAG = "Discovery";

    private final static String API_VERSION_KEY = "api-version";

    private final static String API_VERSION_VALUE = "1.0";

    private final static String AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";

    private final static String INSTANCE_DISCOVERY_SUFFIX = "common/discovery/instance";

    private final static String AUTHORIZATION_COMMON_ENDPOINT = "/common/oauth2/authorize";

    private final static String TENANT_DISCOVERY_ENDPOINT = "tenant_discovery_endpoint";

    /**
     * sync set of valid hosts to skip query to server if host was verified
     * before
     */
    private final static Set<String> mValidHosts = Collections
            .synchronizedSet(new HashSet<String>());

    /**
     * Discovery query will go to the prod only for now.
     */
    private final static String TRUSTED_QUERY_INSTANCE = "login.windows.net";

    public Discovery() {
        initValidList();
    }

    @Override
    public void isValidAuthority(URL authorizationEndpoint, AuthenticationCallback<Boolean> callback) {
        // For comparison purposes, convert to lowercase Locale.US
        // getProtocol returns scheme and it is available if it is absolute url
        // Authority is in the form of https://Instance/tenant/somepath
        if (authorizationEndpoint != null
                && !StringExtensions.IsNullOrBlank(authorizationEndpoint.getHost())
                && authorizationEndpoint.getProtocol().equals("https")
                && StringExtensions.IsNullOrBlank(authorizationEndpoint.getQuery())
                && StringExtensions.IsNullOrBlank(authorizationEndpoint.getRef())
                && !StringExtensions.IsNullOrBlank(authorizationEndpoint.getPath())) {

            if (isADFSAuthority(authorizationEndpoint)) {
                callback.onError(new AuthenticationException(ADALError.DISCOVERY_NOT_SUPPORTED));
            } else if (mValidHosts.contains(authorizationEndpoint.getHost().toLowerCase(Locale.US))) {
                // host can be the instance or inside the validated list.
                // Valid hosts will help to skip validation if validated before
                // call Callback and skip the look up
                callback.onSuccess(true);
            } else {
                // Only query from Prod instance for now, not all of the
                // instances in the list
                queryInstance(authorizationEndpoint, callback);
            }
        } else {
            // Not valid authority
            callback.onSuccess(false);
        }
    }

    private boolean isADFSAuthority(URL authorizationEndpoint) {
        // similar to ADAL.NET
        String path = authorizationEndpoint.getPath();
        return !StringExtensions.IsNullOrBlank(path)
                && path.toLowerCase(Locale.ENGLISH).equals("/adfs");
    }

    /**
     * add this host as valid to skip another query to server
     * 
     * @param validhost
     */
    private void addValidHostToList(URL validhost) {
        String validHost = validhost.getHost();
        if (!StringExtensions.IsNullOrBlank(validHost)) {
            // for comparisons it uses Locale.US, so it needs to be same
            // here
            mValidHosts.add(validHost.toLowerCase(Locale.US));
        }
    }

    /**
     * initialize initial valid host list with known instances
     */
    private void initValidList() {
        // mValidHosts is a sync set
        if (mValidHosts.size() == 0) {
            mValidHosts.add("login.windows.net");
            mValidHosts.add("login.chinacloudapi.cn");
            mValidHosts.add("login.cloudgovapi.us");
        }
    }

    private void queryInstance(final URL authorizationEndpointUrl,
            final AuthenticationCallback<Boolean> callback) {

        // It will query prod instance to verify the authority
        try {
            // construct query string for this instance
            URL queryUrl = buildQueryString(TRUSTED_QUERY_INSTANCE,
                    getAuthorizationCommonEndpoint(authorizationEndpointUrl));

            // setup callback to query again if not found
            final AuthenticationCallback<Boolean> evalStepCallback = new AuthenticationCallback<Boolean>() {

                @Override
                public void onSuccess(Boolean result) {
                    callback.onSuccess(result);

                    if (result) {
                        // it is validated
                        addValidHostToList(authorizationEndpointUrl);
                    }
                }

                @Override
                public void onError(Exception exc) {
                    Logger.e(TAG, "Error in instance discovery", "",
                            ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED, exc);
                    callback.onError(exc);
                }
            };

            // post async call to current instance
            sendRequest(evalStepCallback, queryUrl);

        } catch (MalformedURLException e) {
            Logger.e(TAG, "Invalid authority", "", ADALError.DEVELOPER_AUTHORITY_IS_NOT_VALID_URL,
                    e);
            callback.onError(e);
        }
    }

    private void sendRequest(final AuthenticationCallback<Boolean> callback, final URL queryUrl)
            throws MalformedURLException {

        Logger.v(TAG, "Sending discovery request to:" + queryUrl);
        WebRequestHandler request = new WebRequestHandler();
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(WebRequestHandler.HEADER_ACCEPT, WebRequestHandler.HEADER_ACCEPT_JSON);
        request.sendAsyncGet(queryUrl, headers, new HttpWebRequestCallback() {
            @Override
            public void onComplete(HttpWebResponse webResponse, Exception exception) {

                if (webResponse != null) {
                    try {
                        callback.onSuccess(parseResponse(webResponse));
                    } catch (IllegalArgumentException exc) {
                        Logger.e(TAG, exc.getMessage(), "",
                                ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED, exc);
                        callback.onError(exc);
                    } catch (JSONException e) {
                        Logger.e(TAG, "Json parsing error", "",
                                ADALError.DEVELOPER_AUTHORITY_CAN_NOT_BE_VALIDED, e);
                        callback.onError(e);
                    }
                } else
                    callback.onError(exception);
            }
        });
    }

    /**
     * get Json output from web response body. If it is well formed response, it
     * will have tenant discovery endpoint.
     * 
     * @param webResponse
     * @return true if tenant discovery endpoint is reported. false otherwise.
     * @throws JSONException
     */
    private Boolean parseResponse(HttpWebResponse webResponse) throws JSONException {

        HashMap<String, String> response = HashMapExtensions.getJsonResponse(webResponse);

        return (response != null && response.containsKey(TENANT_DISCOVERY_ENDPOINT));
    }

    /**
     * service side does not validate tenant, so it is sending common keyword as
     * tenant.
     * 
     * @param authorizationEndpointUrl
     * @return https://hostname/common
     * @throws MalformedURLException
     */
    private String getAuthorizationCommonEndpoint(final URL authorizationEndpointUrl)
            throws MalformedURLException {
        return String.format("https://%s%s", authorizationEndpointUrl.getHost(),
                AUTHORIZATION_COMMON_ENDPOINT);
    }

    /**
     * It will build url similar to
     * https://login.windows.net/common/discovery/instance
     * ?api-version=1.0&authorization_endpoint
     * =https%3A%2F%2Flogin.windows.net%2F
     * aaltest.onmicrosoft.com%2Foauth2%2Fauthorize
     * 
     * @param instance
     * @param authorizationEndpointUrl
     * @return
     * @throws MalformedURLException
     */
    private URL buildQueryString(final String instance, final String authorizationEndpointUrl)
            throws MalformedURLException {

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https").authority(instance);
        // replacing tenant to common since instance validation does not check
        // tenant name
        builder.appendEncodedPath(INSTANCE_DISCOVERY_SUFFIX);
        builder.appendQueryParameter(API_VERSION_KEY, API_VERSION_VALUE);
        builder.appendQueryParameter(AUTHORIZATION_ENDPOINT_KEY, authorizationEndpointUrl);
        return new URL(builder.build().toString());
    }
}
