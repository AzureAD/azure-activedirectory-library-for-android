package com.microsoft.adal.integration_tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Properties;

import com.microsoft.adal.ITokenCacheStore;

public class TenantInfo implements Serializable{
     

    /**
     * Serialize your settings
     */
    private static final long serialVersionUID = 3560085163832716550L;

    private static final String FILE_NAME = "integration_tests.properties";
    
    public TenantInfo(String authority, String resource, String resource2, String clientId,
            String redirect, String username, String password) {
        mAuthority = authority;
        mResource = resource;
        mResource2 = resource2;
        mClientId = clientId;
        mRedirect = redirect;
        mUserName = username;
        mPassword = password;
    }
    
    public static void saveConfiguredTenants(){
        Properties p = new Properties();
        p.put("AAD-authority", "https://login.windows.net/omercantest.onmicrosoft.com");
        p.put("AAD-resourceid", "https://omercantest.onmicrosoft.com/AllHandsTry");
        p.put("AAD-resourceid2", "https://omercantest.onmicrosoft.com/spacemonkey");
        p.put("AAD-clientid", "650a6609-5463-4bc4-b7c6-19df7990a8bc");
        p.put("AAD-redirect", "http://taskapp");
        p.put("AAD-username", "faruk@omercantest.onmicrosoft.com");
        p.put("AAD-password", "Jink1235");
        
         
        try {
            p.store(new FileOutputStream(new File("integration_tests.properties"), true), "save config");
            
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void loadConfiguredTenants(){
        
        Properties p = new Properties();
        try {
           
            p.load(new FileInputStream(new File("integration_tests.properties")));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
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
