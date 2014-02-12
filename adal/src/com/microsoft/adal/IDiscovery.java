/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.net.URL;
import java.util.UUID;

public interface IDiscovery {

    /**
     * query authorizationEndpoint from well known instances to validate the instance.
     * It does not validate tenant info. Common name can be used instead of tenant name for authority url to get token.
     * 
     * @param authorizationEndpoint
     * @param callback result will be post here
     */
    boolean isValidAuthority(URL authorizationEndpoint);

    void setCorrelationId(UUID requestCorrelationId);
}
