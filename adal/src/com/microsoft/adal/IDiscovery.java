/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

public interface IDiscovery {
    
/**
 * query authorizationEndpoint from well known instances
 * @param authorizationEndpoint
 * @return
 *  true if instance is discovered or in local list
 *  false if instance is not in the list and not discovered
 */
    boolean IsValidAuthority(String authorizationEndpoint);
}
