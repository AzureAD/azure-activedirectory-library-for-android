//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.test.automation.ui;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class Main {

    public static Target ACQUIRE_TOKEN_BUTTON = Target.the("Acquire token button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/acquireToken"));
    public static Target ACQUIRE_TOKEN_SILENT = Target.the("Acquire token silent button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/acquireTokenSilent"));
    public static Target EXPIRE_ACCESS_TOKEN = Target.the("Expire access token button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/expireAccessToken"));
    public static Target EXPIRE_AT_INVALIDATE_RT = Target.the("Expire access token and invalidate refresh token button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/invalidateRefreshToken"));
    public static Target INVALIDATE_FRT = Target.the("Invalidate family fresh token button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/invalidateFamilyRefreshToken"));
    public static Target READ_CACHE = Target.the("Read cache button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/readCache"));
    public static Target CLEAR_CACHE = Target.the("Clear cache button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/clearCache"));
    public static Target CONSENT_TO_CERT = Target.the("Consent to automation certificate button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/consentToCertificate"));

}
