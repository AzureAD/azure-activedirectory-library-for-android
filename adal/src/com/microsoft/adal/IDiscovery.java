/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.URL;
import java.util.concurrent.Future;

public interface IDiscovery {

    /**
     * query authorizationEndpoint from well known instances to validate the instance.
     * It does not validate tenant info. Common name can be used instead of tenant name for authority url to get token.
     * 
     * @param authorizationEndpoint
     * @param callback result will be post here
     */
    void isValidAuthority(URL authorizationEndpoint, AuthenticationCallback<Boolean> callback);
}
