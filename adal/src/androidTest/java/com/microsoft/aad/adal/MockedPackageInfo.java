package com.microsoft.aad.adal;

import android.content.pm.PackageInfo;
import android.content.pm.Signature;

public class MockedPackageInfo extends PackageInfo {

    public MockedSigningInfo signingInfo;

    public MockedPackageInfo(Signature [] signatures) {
        this.signingInfo = new MockedSigningInfo(signatures);
    }
}
