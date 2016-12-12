package com.microsoft.aad.adal;

import com.google.gson.Gson;

import java.util.UUID;

abstract class AbstractRequestor implements Correlatable {

    protected final IWebRequestHandler mWebrequestHandler;

    protected UUID mCorrelationId;

    private Gson mGson;

    AbstractRequestor() {
        mWebrequestHandler = new WebRequestHandler();
    }

    @Override
    public final void setCorrelationId(final UUID requestCorrelationId) {
        mCorrelationId = requestCorrelationId;
    }

    @Override
    public final UUID getCorrelationId() {
        return mCorrelationId;
    }

    /**
     * Thread-safe lazy-initialized Singleton parser for JSON
     *
     * @return the gson instance
     */
    protected synchronized Gson parser() {
        if (null == mGson) {
            mGson = new Gson();
        }

        return mGson;
    }
}
