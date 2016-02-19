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

package com.microsoft.aad.adal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

/**
 * Gets information about calling activity.
 */
class PackageHelper {
    private static final String TAG = "CallerInfo";

    private Context mContext;

    AccountManager mAcctManager;

    /**
     * Creates helper to check caller info.
     * 
     * @param ctx
     */
    public PackageHelper(Context ctx) {
        mContext = ctx;
        mAcctManager = AccountManager.get(mContext);
    }

    /**
     * Gets metadata information from AndroidManifest file.
     * 
     * @param packageName
     * @param component
     * @param metaDataName
     * @return MetaData
     */
    public Object getValueFromMetaData(final String packageName, final ComponentName component,
            final String metaDataName) {
        try {
            Logger.v(TAG, "Calling package:" + packageName);
            if (component != null) {
                Logger.v(TAG, "component:" + component.flattenToString());
                ActivityInfo ai = mContext.getPackageManager().getActivityInfo(component,
                        PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
                if (ai != null) {
                    Bundle metaData = ai.metaData;
                    if (metaData == null) {
                        Logger.v(TAG, "metaData is null. Unable to get meta data for "
                                + metaDataName);
                    } else {
                        Object value = (Object)metaData.get(metaDataName);
                        return value;
                    }
                }
            } else {
                Logger.v(TAG, "calling component is null.");
            }
        } catch (NameNotFoundException e) {
            Logger.e(TAG, "ActivityInfo is not found", "",
                    ADALError.BROKER_ACTIVITY_INFO_NOT_FOUND, e);
        }
        return null;
    }

    /**
     * Reads first signature in the list for given package name.
     * 
     * @param packagename
     * @return signature for package
     */
    public String getCurrentSignatureForPackage(final String packagename) {
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(packagename,
                    PackageManager.GET_SIGNATURES);
            if (info != null && info.signatures != null && info.signatures.length > 0) {
                Signature signature = info.signatures[0];
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                // Server side needs to register all other tags. ADAL will
                // send one of them.
            }
        } catch (NameNotFoundException e) {
            Logger.e(TAG, "Calling App's package does not exist in PackageManager", "",
                    ADALError.APP_PACKAGE_NAME_NOT_FOUND);
        } catch (NoSuchAlgorithmException e) {
            Logger.e(TAG, "Digest SHA algorithm does not exists", "",
                    ADALError.DEVICE_NO_SUCH_ALGORITHM);
        }
        return null;
    }

    /**
     * Gets package UID.
     * 
     * @param packagename
     * @return UID
     */
    public int getUIDForPackage(final String packagename) {
        int callingUID = 0;
        try {
            ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(packagename, 0);
            if (info != null) {
                callingUID = info.uid;
            }
        } catch (NameNotFoundException e) {
            Logger.e(TAG, "Package " + packagename + " is not found", "",
                    ADALError.PACKAGE_NAME_NOT_FOUND, e);
        }
        return callingUID;
    }

    /**
     * Gets redirect uri for broker.
     * @param packageName   application package name
     * @param signatureDigest   application signature 
     * @return broker redirect url
     */
    public static String getBrokerRedirectUrl(final String packageName, final String signatureDigest) {
        if (!StringExtensions.IsNullOrBlank(packageName)
                && !StringExtensions.IsNullOrBlank(signatureDigest)) {
            try {
                return String.format("%s://%s/%s", AuthenticationConstants.Broker.REDIRECT_PREFIX,
                        URLEncoder.encode(packageName, AuthenticationConstants.ENCODING_UTF8),
                        URLEncoder.encode(signatureDigest, AuthenticationConstants.ENCODING_UTF8));
            } catch (UnsupportedEncodingException e) {
                // This encoding issue will happen at the beginning of API call,
                // if it is not supported on this device. ADAL uses one encoding
                // type.
                Log.e(TAG, "Encoding", e);
            }
        }
        return "";
    }
}
