package com.microsoft.aad.adal;

import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Builds properties for ClientAnalytics.
 */
class InstrumentationEventBuilder {
    final private List<Pair<String, String>> mProperties = new LinkedList<>();

    InstrumentationEventBuilder(AuthenticationRequest request, Exception exc) {
        addPropertiesForRequest(request);
        final Throwable cause = exc.getCause() != null ? exc.getCause() : exc;
        final Class causeClass = cause.getClass();
        final ADALError errorCode = exc instanceof AuthenticationException ? ((AuthenticationException) exc).getCode() : null;
        addProperty(new Pair<>(InstrumentationIDs.ERROR_CLASS, causeClass.getSimpleName()));
        addProperty(new Pair<>(InstrumentationIDs.ERROR_MESSAGE, cause.getMessage()));
        addProperty(new Pair<>(InstrumentationIDs.ERROR_CODE, errorCode.getDescription()));
    }

    InstrumentationEventBuilder(AuthenticationRequest request, AuthenticationResult result) {
        addPropertiesForRequest(request);
        addProperty(new Pair<>(InstrumentationIDs.ERROR_MESSAGE, result.getErrorDescription()));
        addProperty(new Pair<>(InstrumentationIDs.ERROR_CODE, result.getErrorCode()));
    }

    InstrumentationEventBuilder add(Pair<String, String> property) {
        addProperty(property);
        return this;
    }

    private void addProperty(Pair<String, String> property) {
        if (property != null && property.second != null) {
            mProperties.add(property);
        }
    }

    List<Pair<String, String>> build() {
        return mProperties;
    }

    private void addPropertiesForRequest(AuthenticationRequest request) {
        addProperty(new Pair<>(InstrumentationIDs.USER_ID, request.getUserId()));
        addProperty(new Pair<>(InstrumentationIDs.RESOURCE_ID, request.getResource()));
        addProperty(new Pair<>(InstrumentationIDs.AUTHORITY_ID, request.getAuthority()));
        addProperty(new Pair<>(InstrumentationIDs.CORRELATION_ID, request.getCorrelationId().toString()));
    }
}
