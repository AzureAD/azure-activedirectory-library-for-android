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

/**
 * Prompt Behaviors that sets the behavior for authentication activity launch.
 */
public enum PromptBehavior {
    /**
     * Acquire token will prompt the user for credentials only when necessary.
     */
    Auto,

    /**
     * The user will be prompted for credentials even if it is available in the
     * cache or in the form of refresh token. New acquired access token and
     * refresh token will be used to replace previous value. If Settings
     * switched to Auto, new request will use this latest token from cache.
     */
    Always,

    /**
     * Re-authorizes (through displaying webview) the resource usage, making
     * sure that the resulting access token contains the updated claims. If user
     * logon cookies are available, the user will not be asked for credentials
     * again and the logon dialog will dismiss automatically. This is equivalent
     * to passing prompt=refresh_session as an extra query parameter during
     * the authorization.
     */
    REFRESH_SESSION
}
