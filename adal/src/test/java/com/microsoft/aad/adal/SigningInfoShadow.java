package com.microsoft.aad.adal;

import android.content.pm.Signature;
import android.content.pm.SigningInfo;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(SigningInfo.class)
public class SigningInfoShadow {
    private static Signature[] signatures;

    @Implementation
    public boolean hasMultipleSigners() {
        return signatures != null && signatures.length > 1;
    }

    @Implementation
    public boolean hasPastSigningCertificates() {
        return false;
    }

    @Implementation
    public Signature[] getSigningCertificateHistory() {
        return signatures;
    }

    @Implementation
    public Signature[] getApkContentsSigners() {
        return signatures;
    }

    public static void setSignatures(Signature[] signatures) {
        SigningInfoShadow.signatures = signatures;
    }
}
