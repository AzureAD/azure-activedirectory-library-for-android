/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Future;

import org.json.JSONException;

import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.microsoft.adal.AuthenticationParameters.AuthenticationParamCallback;

/**
 * Instance and Tenant discovery. It takes authorization endpoint and sends
 * query to known hard coded instances to get tenant discovery endpoint. If
 * instance is valid, it will send tenant discovery endpoint to query tenant
 * info. Instance discovery endpoint does not verify tenant info, so Discoveryc
 * implementation sends common as a tenant name. Discovery checks only
 * authorization endpoint. It does not do tenant verification.
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
     * UI handler to create async task and execute async task at UI thread
     */
    private Handler mHandler;

    /**
     * sync set of valid hosts to skip query to server
     */
    private final static Set<String> mValidHosts = Collections
            .synchronizedSet(new HashSet<String>());

    /**
     * instances to verify given auth endpoint. windows.net to run query first
     * and then others.
     */
    private static Set<String> mCloudInstances = new LinkedHashSet(Arrays.asList(new String[] {
            "login.windows.net", "login.chinacloudapi.cn", "login.cloudgovapi.us"
    }));

    public Discovery() {
        initValidList();
        mHandler = null;
    }

    /**
     * @param uiHandler Handler is needted if you are calling from non-UI
     *            thread. It will be used to post runnable for async tasks
     */
    public Discovery(Handler uiHandler) {
        initValidList();
        mHandler = uiHandler;
    }

    @Override
    public void isValidAuthority(URL authorizationEndpoint, AuthenticationCallback<Boolean> callback) {

        if (authorizationEndpoint != null
                && !StringExtensions.IsNullOrBlank(authorizationEndpoint.getHost())) {
            if (mCloudInstances.contains(authorizationEndpoint.getHost())
                    || mValidHosts.contains(authorizationEndpoint.getHost())) {
                // host can be the instance or inside the validated list.
                // Validhosts will help to skip validation if validated before
                // call Callback and skip the look up
                callback.onSuccess(true);
                return;
            }
        }

        queryEndpointPerInstance(authorizationEndpoint, callback);
    }

    /**
     * add this host as valid to skip another query to server
     * 
     * @param validhost
     */
    private void addValidHostToList(URL validhost) {
        String validHost = validhost.getHost();
        if (!StringExtensions.IsNullOrBlank(validHost)) {
            synchronized (mValidHosts) {
                mValidHosts.add(validHost);
            }
        }
    }

    /**
     * initialize initial valid host list with known instances
     */
    private void initValidList() {
        synchronized (mValidHosts) {
            if (mValidHosts.size() == 0) {
                mValidHosts.add("login.windows.net");
                mValidHosts.add("login.chinacloudapi.cn");
                mValidHosts.add("login.cloudgovapi.us");
            }
        }
    }

    /**
     * discovery call will return true, if this authorization endpoint exists at
     * the instances it sends async query for each instance if it returns false
     * 
     * @param authorizationEndpointUrl
     * @return
     */
    private void queryEndpointPerInstance(final URL authorizationEndpointUrl,
            final AuthenticationCallback<Boolean> callback) {

        // Try instances to see if host is valid
        final Iterator<String> instanceIterator = mCloudInstances.iterator();
        queryEndpointPerInstanceNext(authorizationEndpointUrl, callback, instanceIterator);
    }

    private void queryEndpointPerInstanceNext(final URL authorizationEndpointUrl,
            final AuthenticationCallback<Boolean> callback, final Iterator<String> iterator) {

        if (iterator.hasNext()) {
            // if there are more instance to query, it will send another async
            // call to check endpoint
            String instanceHost = iterator.next();

            try {
                // construct query string for this instance
                URL queryUrl = buildQueryString(instanceHost,
                        getAuthorizationCommonEndpoint(authorizationEndpointUrl));

                // setup callback to query again if not found
                final AuthenticationCallback<Boolean> evalStepCallback = new AuthenticationCallback<Boolean>() {

                    @Override
                    public void onSuccess(Boolean result) {
                        if (result) {
                            // it is validated
                            addValidHostToList(authorizationEndpointUrl);
                            callback.onSuccess(result);
                        } else {
                            queryEndpointPerInstanceNext(authorizationEndpointUrl, callback,
                                    iterator);
                        }
                    }

                    @Override
                    public void onError(Exception exc) {
                        Log.e(TAG, exc.getMessage());
                        callback.onError(exc);
                    }
                };

                // post async call to current instance
                sendRequest(evalStepCallback, queryUrl);

            } catch (MalformedURLException e) {
                Log.e(TAG, e.getMessage());
                callback.onError(e);
            }
        } else {
            // it checked all of the instances
            Log.w(TAG, "all of the instances returned invalid for this endpoint:"
                    + authorizationEndpointUrl.toString());
            callback.onSuccess(false);
        }
    }

    private void sendRequest(final AuthenticationCallback<Boolean> callback, final URL queryUrl)
            throws MalformedURLException {

        Runnable request = new Runnable() {

            @Override
            public void run() {
                Log.d(TAG, "Sending discovery request to:" + queryUrl);
                WebRequestHandler request = new WebRequestHandler();
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                request.sendAsyncGet(queryUrl, headers, new HttpWebRequestCallback() {
                    @Override
                    public void onComplete(HttpWebResponse webResponse, Exception exception) {

                        if (webResponse != null) {
                            try {
                                callback.onSuccess(parseResponse(webResponse));
                            } catch (IllegalArgumentException exc) {
                                Log.e(TAG, exc.getMessage());
                                callback.onError(exc);
                            } catch (JSONException e) {
                                Log.e(TAG, "Json parsing error:" + e.getMessage());
                                callback.onError(e);
                            }
                        } else
                            callback.onError(exception);
                    }

                });
            }
        };

        if (mHandler != null) {
            Log.d(TAG, "Sending discovery request using handler:" + queryUrl);
            mHandler.post(request);
        } else {
            Log.d(TAG, "Sending discovery request without using handler:" + queryUrl);
            request.run();
        }
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
     * it will build url similar to
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
        builder.appendEncodedPath(INSTANCE_DISCOVERY_SUFFIX);
        builder.appendQueryParameter(API_VERSION_KEY, API_VERSION_VALUE);
        builder.appendQueryParameter(AUTHORIZATION_ENDPOINT_KEY, authorizationEndpointUrl);
        return new URL(builder.build().toString());
    }
}
