
package com.microsoft.adal.integration_tests;

import java.io.Serializable;
import java.util.Properties;

public class TenantInfo implements Serializable {

    /**
     * Serialize your settings
     */
    private static final long serialVersionUID = 3560085163832716550L;

    private TenantInfo(TenantType tenantType, String authority, String resource, String resource2,
            String clientId, String redirect, String federated, String username, String password) {
        mType = tenantType;
        mAuthority = authority;
        mResource = resource;
        mResource2 = resource2;
        mClientId = clientId;
        mRedirect = redirect;
        mFederated = federated;
        mUserName = username;
        mPassword = password;
    }

    public static TenantInfo parseTenant(TenantType tenant, Properties p) {
        String authority = p.getProperty(tenant.name() + "-authority");
        String resource = p.getProperty(tenant.name() + "-resourceid");
        String resource2 = p.getProperty(tenant.name() + "-resourceid2");
        String clientid = p.getProperty(tenant.name() + "-clientid");
        String redirect = p.getProperty(tenant.name() + "-redirect");
        String username = p.getProperty(tenant.name() + "-username");
        String password = p.getProperty(tenant.name() + "-password");
        String federated = p.getProperty(tenant.name() + "-federated");
        return new TenantInfo(tenant, authority, resource, resource2, clientid, redirect, federated, username,
                password);
    }

    public TenantType getTag() {
        return mType;
    }

    public String getAuthority() {
        return mAuthority;
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
}
