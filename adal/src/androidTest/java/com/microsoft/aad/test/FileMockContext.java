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

package com.microsoft.aad.test;

import static org.mockito.Mockito.mock;

import java.io.File;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.os.Looper;
import android.test.mock.MockContext;
import android.test.mock.MockPackageManager;

class FileMockContext extends MockContext {

    private Context mContext;

    static final String PREFIX = "test.mock.";

    boolean resolveIntent = true;

    String dirName;

    int fileWriteMode;

    String requestedPermissionName;

    int responsePermissionFlag;

    public FileMockContext(Context context) {
        mContext = context;
        // default
        requestedPermissionName = "android.permission.INTERNET";
        responsePermissionFlag = PackageManager.PERMISSION_GRANTED;
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
        dirName = name;
        fileWriteMode = mode;
        return null;
    }

    @Override
    public Object getSystemService(String name) {
        if (name.equalsIgnoreCase("account")) {
            return mock(AccountManager.class);
        } else if(name.equalsIgnoreCase("connectivity")) {
            return mock(ConnectivityManager.class);
        }
        return new Object();
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return mContext.getSharedPreferences(name, mode);
    }

    @Override
    public PackageManager getPackageManager() {
        return new TestPackageManager();
    }

    class TestPackageManager extends MockPackageManager {
        @Override
        public ResolveInfo resolveActivity(Intent intent, int flags) {
            if (resolveIntent)
                return new ResolveInfo();

            return null;
        }

        @Override
        public int checkPermission(String permName, String pkgName) {
            if (permName.equals(requestedPermissionName)) {
                return responsePermissionFlag;
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
