
package com.microsoft.adal.test;

public enum TestTenant {
    MANAGED("https://login.windows.net/omercantest.onmicrosoft.com",
            "https://omercantest.onmicrosoft.com/AllHandsTry",
            "https://omercantest.onmicrosoft.com/spacemonkey",
            "650a6609-5463-4bc4-b7c6-19df7990a8bc", "http://taskapp",
            "faruk@omercantest.onmicrosoft.com", "Jink1234"),
    /**
     * federated ADFS30
     */
    ADFS30FEDERATED("https://login.windows-ppe.net/AdalE2ETenant1.ccsctp.net",
            "http://adalscenariohealthwebapi.azurewebsites.net/", null,
            "f556da69-f8b3-4058-a3f8-01d9b60d7df8", "https://login.live.com/",
            "adaluser@ade2eadfs30.com", "P@ssw0rd"),
    /**
     * federated ADFS20
     */
    ADFS20FEDERATED("https://login.windows-ppe.net/AdalE2ETenant2.ccsctp.net",
            "http://arwin8/TokenConsumer", null, "9e6d1c62-3eca-4419-acdd-1226a0e7e662",
            "https://login.live.com/", "adaluser@ade2eadfs20.com", "P@ssw0rd"),
    /**
     * ADFS as STS
     */
    ADFS30("https://fs.ade2eadfs30.com/adfs", "urn:msft:ad:test:oauth:teamdashboard", null,
            "DE25CE3A-B772-4E6A-B431-96DCB5E7E559", "https://login.live.com/",
            "ade2eadfs30.com\\adaluser", "P@ssw0rd");

    private TestTenant(String authority, String resource, String resource2, String clientId,
            String redirect, String username, String password) {
        mAuthority = authority;
        mResource = resource;
        mResource2 = resource2;
        mClientId = clientId;
        mRedirect = redirect;
        mUserName = username;
        mPassword = password;
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

    public String getUserName() {
        return mUserName;
    }

    public String getPassword() {
        return mPassword;
    }

    private String mAuthority;

    private String mResource;

    private String mResource2;

    private String mClientId;

    private String mRedirect;

    private String mUserName;

    private String mPassword;
}
