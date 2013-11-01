/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.URL;
import java.util.concurrent.Future;

interface IDiscovery {

    /**
     * query authorizationEndpoint from well known instances
     * 
     * @param authorizationEndpoint
     * @param callback result will be post here
     */
    void isValidAuthority(URL authorizationEndpoint, AuthenticationCallback<Boolean> callback);
}
