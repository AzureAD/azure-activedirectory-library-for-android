package com.microsoft.identity.common.test.automation.ui;

import net.serenitybdd.screenplay.targets.Target;

import org.openqa.selenium.By;

public class Main {

    public static Target ACQUIRE_TOKEN_BUTTON = Target.the("Acquire token button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/acquireToken"));
    public static Target ACQUIRE_TOKEN_SILENT = Target.the("Acquire token silent tutton").located(By.id("com.microsoft.aad.automation.testapp.adal:id/acquireTokenSilent"));
    public static Target EXPIRE_ACCESS_TOKEN = Target.the("Expire access token button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/expireAccessToken"));
    public static Target EXPIRE_AT_INVALIDATE_RT = Target.the("Expire access token and invalidate refresh token button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/invalidateRefreshToken"));
    public static Target INVALIDATE_FRT = Target.the("Invalidate family fresh token button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/invalidateFamilyRefreshToken"));
    public static Target READ_CACHE = Target.the("Read cache button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/readCache"));
    public static Target CLEAR_CACHE = Target.the("Clear cache button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/clearCache"));
    public static Target CONSENT_TO_CERT = Target.the("Consent to automation certificate button").located(By.id("com.microsoft.aad.automation.testapp.adal:id/consentToCertificate"));

}
