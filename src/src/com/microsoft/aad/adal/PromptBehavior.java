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
    REFRESH_SESSION, 
    
    /**
     * If Azure Authenticator or Company Portal is installed, this flag will have 
     * the broker app force the prompt behavior, otherwise it will be same as Always. 
     * If using embeded flow, please keep using Always, if FORCE_PROMPT is set for 
     * embeded flow, the sdk will re-intepret it to Always. 
     */
    FORCE_PROMPT
}
