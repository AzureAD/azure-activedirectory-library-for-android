package com.microsoft.aad.adal;

import android.content.pm.Signature;

public class MockedSigningInfo {

    private final Signature[] signatures;

    public MockedSigningInfo(Signature[] signatures) {
        this.signatures = signatures;
    }

    public boolean hasMultipleSigners() {
        return false;
    }

    public Signature[] getSigningCertificateHistory() {
        return signatures;
    }
}
