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

package com.microsoft.aad.adal.integration_tests;

import java.io.Serializable;
import java.util.Properties;

public class TenantInfo implements Serializable {

    /**
     * Serialize your settings
     */
    private static final long serialVersionUID = 3560085163832716550L;

    public TenantInfo(TenantType tenantType, String authority, String resource, String resource2,
            String clientId, String redirect, String federated, String username, String password,
            String username2, String password2) {
        mType = tenantType;
        mAuthority = authority;
        mResource = resource;
        mResource2 = resource2;
        mClientId = clientId;
        mRedirect = redirect;
        mFederated = federated;
        mUserName = username;
        mPassword = password;
        mUserName2 = username2;
        mPassword2 = password2;
    }

    public static TenantInfo parseTenant(TenantType tenant, Properties p) {
        String authority = p.getProperty(tenant.name() + "-authority");
        String resource = p.getProperty(tenant.name() + "-resourceid");
        String resource2 = p.getProperty(tenant.name() + "-resourceid2");
        String clientid = p.getProperty(tenant.name() + "-clientid");
        String redirect = p.getProperty(tenant.name() + "-redirect");
        String username = p.getProperty(tenant.name() + "-username");
        String password = p.getProperty(tenant.name() + "-password");
        String username2 = p.getProperty(tenant.name() + "-username2");
        String password2 = p.getProperty(tenant.name() + "-password2");
        String federated = p.getProperty(tenant.name() + "-federated");
        return new TenantInfo(tenant, authority, resource, resource2, clientid, redirect,
                federated, username, password, username2, password2);
    }

    public TenantType getTag() {
        return mType;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public void setAuthority(String authority) {
        mAuthority = authority;
    }

    public String getResource() {
        return mResource;
    }

    public String getResource2() {
        return mResource2;
    }

    public String getClientId() {
        return mClientId;
    }

    public String getRedirect() {
        return mRedirect;
    }

    public String getFederated() {
        return mFederated;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getPassword() {
        return mPassword;
    }

    public String getUserName2() {
        return mUserName2;
    }

    public void setUserName2(String mUserName2) {
        this.mUserName2 = mUserName2;
    }

    public String getPassword2() {
        return mPassword2;
    }

    public void setPassword2(String mPassword2) {
        this.mPassword2 = mPassword2;
    }

    enum TenantType {
        AAD, ADFS30, ADFS20FEDERATED, ADFS30FEDERATED
    }

    private TenantType mType;

    private String mAuthority;

    private String mResource;

    private String mResource2;

    private String mClientId;

    private String mRedirect;

    private String mFederated;

    private String mUserName;

    private String mPassword;
    
    private String mUserName2;

    private String mPassword2;
}
