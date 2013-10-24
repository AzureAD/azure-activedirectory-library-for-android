/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

import java.io.Serializable;
import java.util.Date;

/**
 * Extended result to store more info Queries will be performed over this item
 * not the key
 * 
 * @author omercan
 */
public class TokenCacheItem implements Serializable {

    public TokenCacheItem() {

    }

    UserInfo getUserInfo() {
        throw new UnsupportedOperationException("come back later");
    }

    void setUserInfo(UserInfo info) {
        throw new UnsupportedOperationException("come back later");
    }

    public String getResource() {
        throw new UnsupportedOperationException("come back later");
    }

    public void setResource(String resource) {
        throw new UnsupportedOperationException("come back later");
    }

    public String getAuthority() {
        throw new UnsupportedOperationException("come back later");
    }

    public void setAuthority(String authority) {
        throw new UnsupportedOperationException("come back later");
    }

    public String getClientId() {
        throw new UnsupportedOperationException("come back later");
    }

    public void setClientId(String clientId) {
        throw new UnsupportedOperationException("come back later");
    }

    public String getAccessToken() {
        throw new UnsupportedOperationException("come back later");
    }

    public void setAccessToken(String accessToken) {
        throw new UnsupportedOperationException("come back later");
    }

    public String getRefreshToken() {
        throw new UnsupportedOperationException("come back later");
    }

    public void setRefreshToken(String refreshToken) {
        throw new UnsupportedOperationException("come back later");
    }

    public Date getExpiresOn() {
        throw new UnsupportedOperationException("come back later");
    }

    public void setExpiresOn(Date expiresOn) {
        throw new UnsupportedOperationException("come back later");
    }
}
