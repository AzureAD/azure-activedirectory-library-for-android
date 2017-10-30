package com.microsoft.aad.adal;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public final class TelemetryPrivacyComplianceTests {

    @Test
    public void testApiEventPrivacyCompliance() {
        final APIEvent apiEvent = new APIEvent(EventStrings.API_EVENT);
        populateWithPii(apiEvent);
        verifyEmpty(apiEvent);
    }

    @Test
    public void testBrokerEventPrivacyCompliance() {
        final BrokerEvent brokerEvent = new BrokerEvent(EventStrings.BROKER_EVENT);
        populateWithPii(brokerEvent);
        verifyEmpty(brokerEvent);
    }

    @Test
    public void testCacheEventPrivacyCompliance() {
        final CacheEvent cacheEvent = new CacheEvent(EventStrings.CACHE_EVENT_COUNT);
        populateWithPii(cacheEvent);
        verifyEmpty(cacheEvent);
    }

    @Test
    public void testDefaultEventPrivacyCompliance() {
        final DefaultEvent defaultEvent = new DefaultEvent();
        populateWithPii(defaultEvent);
        verifyEmpty(defaultEvent);
    }

    @Test
    public void testHttpEventPrivacyCompliance() {
        final HttpEvent httpEvent = new HttpEvent(EventStrings.HTTP_EVENT);
        populateWithPii(httpEvent);
        verifyEmpty(httpEvent);
    }

    @Test
    public void testUiEventPrivacyCompliance() {
        final UIEvent uiEvent = new UIEvent(EventStrings.UI_EVENT);
        populateWithPii(uiEvent);
        verifyEmpty(uiEvent);
    }

    @Test
    public void testApiEventPrivacyComplianceWithPiiPresent() {
        Telemetry.setAllowPii(true);
        final APIEvent apiEvent = new APIEvent(EventStrings.API_EVENT);
        apiEvent.setLoginHint("sample_value");
        apiEvent.setIdToken(AuthenticationContextTest.TEST_IDTOKEN);
        verifyHashed(apiEvent);
        Telemetry.setAllowPii(false);
    }

    private void populateWithPii(final DefaultEvent event) {
        for (final String piiProperty : TelemetryUtils.GDPR_FILTERED_FIELDS) {
            event.setProperty(piiProperty, "sample_value");
        }
    }

    private void verifyEmpty(final DefaultEvent event) {
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        for (final String key : dispatchMap.keySet()) {
            if (TelemetryUtils.GDPR_FILTERED_FIELDS.contains(key)) {
                throw new AssertionError("Telemetry dispatch contained PII key: " + key);
            }
        }
    }

    private void verifyHashed(final DefaultEvent event) {
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        for (final Map.Entry<String, String> entry : dispatchMap.entrySet()) {
            final String key = entry.getKey();
            boolean shouldThrow = false;
            try {
                if (key.equals(EventStrings.LOGIN_HINT)) {
                    shouldThrow = !entry.getValue().equals(StringExtensions.createHash("sample_value"));
                }

                if (key.equals(EventStrings.USER_ID)) {
                    shouldThrow = !entry.getValue().equals(StringExtensions.createHash("admin@aaltests.onmicrosoft.com"));
                }

                if (key.equals(EventStrings.TENANT_ID)) {
                    shouldThrow = !entry.getValue().equals(StringExtensions.createHash("30baa666-8df8-48e7-97e6-77cfd0995963"));
                }

                if (shouldThrow) {
                    throw new AssertionError("Event contained PII key/pair: "
                            + key
                            + "/"
                            + entry.getValue()
                    );
                }
            } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
                throw new AssertionError("Could not validate PII compliance/hashing");
            }
        }
    }
}
