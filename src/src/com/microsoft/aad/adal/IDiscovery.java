// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.aad.adal;

import java.net.URL;
import java.util.UUID;

/**
 * Discovery interface that validates authority.
 */
public interface IDiscovery {

    /**
     * Query authorizationEndpoint from well known instances to validate the
     * instance. It does not validate tenant info. Common name can be used
     * instead of tenant name for authority url to get token.
     * 
     * @param authorizationEndpoint URL for authorization endpoint
     * @return true if authority is valid
     */
    boolean isValidAuthority(URL authorizationEndpoint);

    /**
     * Sets correlationId.
     * @param requestCorrelationId {@link UUID}
     */
    void setCorrelationId(UUID requestCorrelationId);
}
