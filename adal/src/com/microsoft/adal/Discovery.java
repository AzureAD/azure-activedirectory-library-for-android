/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Future;

import android.net.Uri;
import android.os.Handler;

import com.microsoft.adal.AuthenticationParameters.AuthenticationParamCallback;

/**
 *
 */
final class Discovery implements IDiscovery {

    private boolean mAllowSSLErrors = false;

    private final static String API_VERSION_KEY = "api-version";

    private final static String API_VERSION_VALUE = "1.0";

    private final static String AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";

    private final static String ISSUER_ENDPOINT_KEY = "issuer";

    private final static String INSTANCE_DISCOVERY_SUFFIX = "/common/discovery/instance";

    private final static String AUTHORIZATION_COMMON_ENDPOINT = "/common/oauth2/authorize";

    private final static String INSTANCE_TO_CHECK = "login.windows.net";

    /**
     * UI handler to create async task and execute asycn task at UI thread
     */
    private Handler mHandler;

    /**
     * sync set of valid hosts to skip query to server
     */
    private final static Set<String> mValidHosts = Collections
            .synchronizedSet(new HashSet<String>());

    /**
     * instances to verify given auth endpoint
     */
    private final static Set<String> mCloudInstances = new HashSet(Arrays.asList(new String[] {
            "login.windows.net", "login.chinacloudapi.cn", "login.cloudgovapi.us"
    }));

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
                // validhosts will help to skip validation if validated before
                callback.onSuccess(true);
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
     * initialize initial valid host list
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

    private void queryEndpointPerInstanceNext(final String authorizationEndpointUrl,
            final AuthenticationCallback<Boolean> callback, final Iterator<String> iterator) {

        if (iterator.hasNext()) {
            String instanceHost = iterator.next();

            try {
                // construct query string for this instance
                String queryString = buildQueryString(instanceHost,
                        getAuthorizationCommonEndpoint(authorizationEndpointUrl));

                // setup callback to query again if not found
                final AuthenticationCallback<Boolean> evalStepCallback = new AuthenticationCallback<Boolean>() {

                    @Override
                    public void onSuccess(Boolean result) {
                        if (result) {
                            // it is validated
                            callback.onSuccess(result);
                        } else {
                            queryEndpointPerInstanceNext(authorizationEndpointUrl, callback,
                                    iterator);
                        }

                    }

                    @Override
                    public void onError(Exception exc) {
                        callback.onError(exc);
                    }
                };

                // post to async call
                sendRequest(evalStepCallback, queryString);

            } catch (MalformedURLException e) {
                callback.onError(e);
            }
        } else {
            // it checked all of them
            callback.onSuccess(false);
        }
    }

    private void sendRequest(final AuthenticationCallback<Boolean> callback, String queryUrl)
            throws MalformedURLException {
        HttpWebRequest webRequest = new HttpWebRequest(new URL(queryUrl));
        webRequest.getRequestHeaders().put("Accept", "application/json");

        webRequest.sendAsyncGet(new HttpWebRequestCallback() {
            @Override
            public void onComplete(HttpWebResponse webResponse, Exception exception) {

                if (webResponse != null) {
                    try {
                        callback.onCompleted(null, parseResponse(webResponse));
                    } catch (IllegalArgumentException exc) {
                        callback.onError(exc);
                    }
                } else
                    callback.onError(exception);
            }
        });
    }

    /**
     * service side does not validate tenant, so it is sending common keyword as
     * tenant
     * 
     * @param authorizationEndpointUrl
     * @return
     * @throws MalformedURLException
     */
    private String getAuthorizationCommonEndpoint(final String authorizationEndpointUrl)
            throws MalformedURLException {
        URL url = new URL(authorizationEndpointUrl);
        return String.format("https://%s%s", url.getHost(), AUTHORIZATION_COMMON_ENDPOINT);
    }

    private String buildQueryString(final String instance, final String authorizationEndpointUrl)
            throws MalformedURLException {

        Uri.Builder builder = new Uri.Builder();
        builder.authority(new URL(instance).getAuthority());
        builder.appendQueryParameter(API_VERSION_KEY, API_VERSION_VALUE);
        builder.appendQueryParameter(AUTHORIZATION_ENDPOINT_KEY, authorizationEndpointUrl);
        return builder.build().toString();
    }
}
