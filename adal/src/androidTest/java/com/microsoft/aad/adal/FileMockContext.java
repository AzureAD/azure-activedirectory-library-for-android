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

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;
import android.test.mock.MockPackageManager;


import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

class FileMockContext extends MockContext {

    private Context mContext;

    static final String PREFIX = "test.mock.";

    private boolean mResolveIntent = true;

    private String mDirName;

    private int mFileWriteMode;

    private Map<String, Integer> mPermissionMap = new HashMap<String, Integer>();
    
    private boolean mIsConnectionAvaliable = true;
    
    private AccountManager mMockedAccountManager = null;

    private PackageManager mMockedPackageManager = null;

    public FileMockContext(Context context) {
        mContext = context;
        // default
        mPermissionMap.put("android.permission.INTERNET", PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public Looper getMainLooper() {
        return mContext.getMainLooper();
    }

    @Override
    public String getPackageName() {
        return PREFIX;
    }

    @Override
    public Context getApplicationContext() {
        return mContext;
    }

    @Override
    public File getDir(String name, int mode) {
        mDirName = name;
        mFileWriteMode = mode;
        return null;
    }

    @Override
    public Object getSystemService(String name) {
        if (name.equalsIgnoreCase("account")) {
            if (mMockedAccountManager == null) {
                return mock(AccountManager.class);
            }
            
            return mMockedAccountManager;
        } else if (name.equalsIgnoreCase("connectivity")) {
            final ConnectivityManager mockedConnectivityManager = mock(ConnectivityManager.class);
            final NetworkInfo mockedNetworkInfo = mock(NetworkInfo.class);
            Mockito.when(mockedNetworkInfo.isConnectedOrConnecting()).thenReturn(mIsConnectionAvaliable);
            Mockito.when(mockedConnectivityManager.getActiveNetworkInfo()).thenReturn(mockedNetworkInfo);
            return mockedConnectivityManager;
        }
        return new Object();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return mContext.getSharedPreferences(name, mode);
    }

    @Override
    public PackageManager getPackageManager() {
        if (mMockedPackageManager == null) {
            return new TestPackageManager();
        }

        return mMockedPackageManager;
    }

    @Override
    public ContentResolver getContentResolver() {

        MockContentProvider mockContentProvider = new MockContentProvider(mContext) {
            @Override
            public Bundle call(String method, String request, Bundle args) {
                return new Bundle();
            }
        };

        MockContentResolver mockContentResolver = new MockContentResolver();
        mockContentResolver.addProvider(Settings.AUTHORITY, mockContentProvider);

        return mockContentResolver;
    }

    public void setMockedAccountManager(final AccountManager mockedAccountManager) {
        if (mockedAccountManager == null) {
            throw new IllegalArgumentException("mockedAccountManager");
        }

        mMockedAccountManager = mockedAccountManager;
    }

    public void addPermission(String permissionName) {
        mPermissionMap.put(permissionName, PackageManager.PERMISSION_GRANTED);
    }
    
    public void removePermission(String permissionName) {
        mPermissionMap.remove(permissionName);
    }

    public int checkPermission(String permName, String pkgName) {
        if (mPermissionMap.containsKey(permName)) {
            return PackageManager.PERMISSION_GRANTED;
        }

        return PackageManager.PERMISSION_DENIED;
    }

    public void setConnectionAvaliable(boolean connectionAvaliable) {
        mIsConnectionAvaliable = connectionAvaliable;
    }

    public void setResolveIntent(boolean resolveIntent) {
        mResolveIntent = resolveIntent;
    }

    public String getDirName() {
        return mDirName;
    }

    public int getFileWriteMode() {
        return mFileWriteMode;
    }

    AccountManager getAccountManager() {
        return mMockedAccountManager;
    }

    public void setMockedPackageManager(final PackageManager mockedPackageManager) {
        if (mockedPackageManager == null) {
            throw new IllegalArgumentException("mockedPackageManager");
        }

        mMockedPackageManager = mockedPackageManager;
    }

    class TestPackageManager extends MockPackageManager {
        @Override
        public ResolveInfo resolveActivity(Intent intent, int flags) {
            if (mResolveIntent) {
                return new ResolveInfo();
            }

            return null;
        }

        @Override
        public int checkPermission(String permName, String pkgName) {
            if (mPermissionMap.containsKey(permName)) {
                return PackageManager.PERMISSION_GRANTED;
            }
            
            return PackageManager.PERMISSION_DENIED;
        }

        @Override
        public PackageInfo getPackageInfo(String packageName, int flags)
                throws NameNotFoundException {
            // TODO Auto-generated method stub
            PackageInfo info = new PackageInfo();
            info.packageName = packageName;
            info.versionName = "test";
            return info;
        }
    }
}
