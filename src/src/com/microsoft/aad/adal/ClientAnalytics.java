package com.microsoft.aad.adal;

import android.util.Pair;

import java.util.List;

/**
 * Interface that provides ability to client application
 * send telemetry events to server
 */
public class ClientAnalytics {

    private IEventListener mEventListener;

    /**
     * Empty constructor, use ClientAnalytics.getInstance()
     */
    private ClientAnalytics() {
    }

    /**
     * @return event logger
     */
    public static ClientAnalytics getInstance() {
        return InstanceHolder.sInstance;
    }

    /**
     * set custom event listener.
     * @param eventListener
     */
    public void setEventListener(IEventListener eventListener) {
        mEventListener = eventListener;
    }

    /**
     * send event to listener
     * @param eventName
     * @param properties
     */
    static void logEvent(final String eventName, List<Pair<String, String>> properties) {
        InstanceHolder.sInstance.log(eventName, properties);
    }

    /**
     * if there is event listener set send event to listener
     * @param eventName
     * @param properties
     */
    private void log(final String eventName, List<Pair<String, String>> properties) {
        if (mEventListener != null) {
            mEventListener.logEvent(eventName, properties);
        }
    }

    /**
     * Event listener interface
     */
    public interface IEventListener {
        void logEvent(final String eventName, List<Pair<String, String>> properties);
    }

    /**
     * The instance holder
     */
    private static final class InstanceHolder {
        private static ClientAnalytics sInstance = new ClientAnalytics();
    }
}
