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

package com.microsoft.aad.automation.testapp;

import android.app.Application;
import android.os.Build;
import android.webkit.WebView;

import com.microsoft.aad.adal.AuthenticationSettings;
import com.microsoft.aad.adal.Logger;
import com.microsoft.aad.adal.ADALError;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 */
public class AndroidAutomationApp extends Application {
    
    private StringBuffer mADALLogs;

    @Override
    public void onCreate() {
        super.onCreate();
        mADALLogs = new StringBuffer();
        Logger.getInstance().setExternalLogger(new Logger.ILogger() {

            @Override
            public void Log(String tag, String message, String additionalMessage, Logger.LogLevel level, ADALError errorCode) {
                mADALLogs.append("tag:" + tag + ", message:" + message + ", additionalMessage:" 
                    + additionalMessage + ", level:" + level + ", errorCode:" + errorCode + "\n");
            }
        });
        setEncryptionKey();
    }

    private void setEncryptionKey() {
        // Provide secret key for token encryption.
        try {
            // For API version lower than 18, you have to provide the secret key. The secret key
            // needs to be 256 bits. You can use the following way to generate the secret key. And
            // use AuthenticationSettings.Instance.setSecretKey(secretKeyBytes) to supply us the key.
            // For API version 18 and above, we use android keystore to generate keypair, and persist
            // the keypair in AndroidKeyStore. Current investigation shows 1)Keystore may be locked with
            // a lock screen, if calling app has a lot of background activity, keystore cannot be
            // accessed when locked, we'll be unable to decrypt the cache items 2) AndroidKeystore could
            // be reset when gesture to unlock the device is changed.
            // We do recommend the calling app the supply us the key with the above two limitations.
            if (AuthenticationSettings.INSTANCE.getSecretKeyData() == null) {
                // use same key for tests
                SecretKeyFactory keyFactory = SecretKeyFactory
                        .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
                SecretKey tempkey = keyFactory.generateSecret(new PBEKeySpec("test".toCharArray(),
                        "abcdedfdfd".getBytes("UTF-8"), 100, 256));
                SecretKey secretKey = new SecretKeySpec(tempkey.getEncoded(), "AES");
                AuthenticationSettings.INSTANCE.setSecretKey(secretKey.getEncoded());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | UnsupportedEncodingException ex) {
            throw new IllegalStateException("Fail to generate secret key:" + ex.getMessage(), ex);
        }
    }
    
    public String getADALLogs() {
        return mADALLogs.toString();
    }
    
    public void setADALLogs(String log) {
        mADALLogs.append(log);
    }
}
