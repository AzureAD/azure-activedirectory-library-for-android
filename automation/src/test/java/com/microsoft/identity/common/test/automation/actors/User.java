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

package com.microsoft.identity.common.test.automation.actors;

import com.google.gson.Gson;
import com.microsoft.identity.common.test.automation.model.TokenCacheItemReadResult;
import com.microsoft.identity.common.test.automation.utility.Credential;
import com.microsoft.identity.common.test.automation.utility.TokenRequest;

import net.serenitybdd.screenplay.Actor;

public class User extends Actor {

    private TokenRequest tokenRequest;
    private TokenRequest silentTokenRequest;
    private String federationProvider;
    private Credential credential;
    private TokenCacheItemReadResult cacheResult;
    private boolean workplaceJoined = false;

    public void setTokenRequest(TokenRequest tokenRequest) {
        this.tokenRequest = tokenRequest;
    }

    public TokenRequest getTokenRequest() {
        return this.tokenRequest;
    }

    public void setSilentTokenRequest(TokenRequest silentTokenRequest) {
        this.silentTokenRequest = silentTokenRequest;
    }

    public TokenRequest getSilentTokenRequest() {
        return this.silentTokenRequest;
    }


    public String getTokenRequestAsJson() {
        Gson gson = new Gson();
        String requestJson = gson.toJson(this.tokenRequest);
        return requestJson;
    }

    public String getSilentTokenRequestAsJson() {
        Gson gson = new Gson();
        String requestJson = gson.toJson(this.silentTokenRequest);
        return requestJson;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public Credential getCredential() {
        return this.credential;
    }

    public String getFederationProvider() {
        return this.federationProvider;
    }

    public void setFederationProvider(String federationProvider) {
        this.federationProvider = federationProvider;
    }

    public boolean getWorkplaceJoined() {
        return workplaceJoined;
    }

    public void setWorkplaceJoined(boolean workplaceJoined) {
        this.workplaceJoined = workplaceJoined;
    }


    public User(String name) {
        super(name);
    }

    public static User named(String name) {
        return new User(name);
    }



    public TokenCacheItemReadResult getCacheResult() {
        return cacheResult;
    }

    public void setCacheResult(TokenCacheItemReadResult cacheResult) {
        this.cacheResult = cacheResult;
    }

    public String getCacheResultAsJson(){
        String cacheJson = new Gson().toJson(this.cacheResult);
        return cacheJson;
    }
}
