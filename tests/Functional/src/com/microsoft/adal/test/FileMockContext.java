package com.microsoft.adal.test;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
        // TODO Auto-generated method stub
        return super.getSystemService(name);
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