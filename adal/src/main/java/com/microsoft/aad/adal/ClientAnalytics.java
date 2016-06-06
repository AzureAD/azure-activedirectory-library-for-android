// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import java.util.Map;

/**
 * Interface that provides ability to client application
 * send telemetry events to server
 *
 * In order to send ADAL instrumentation events to any instrumentation service
 * client application needs to implement IEventListener interface
 * and set it for ClientAnalytics
 * (@use ClientAnalytics.getInstance().setEventListener)
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
     * send event to listener
     * @param event
     */
    static void logEvent(final Event event) {
        logEvent(event.mName, event.mProperties);
    }

    /**
     * send event to listener
     * @param eventName
     * @param properties
     */
    static void logEvent(final String eventName, Map<String, String> properties) {
        InstanceHolder.sInstance.log(eventName, properties);
    }

    /**
     * Set custom event listener.
     * @param eventListener client implementation of instrumentation event logger
     */
    public void setEventListener(IEventListener eventListener) {
        mEventListener = eventListener;
    }

    /**
     * if there is event listener set send event to listener
     * @param eventName
     * @param properties
     */
    private void log(final String eventName, Map<String, String> properties) {
        if (mEventListener != null) {
            mEventListener.logEvent(eventName, properties);
        }
    }

    static class Event {
        private final String mName;
        private final Map<String, String> mProperties;

        public Event(String name, Map<String, String> properties) {
            mName = name;
            mProperties = properties;
        }
    }
    /**
     * Event listener interface
     */
    public interface IEventListener {

        /**
         * Sends event info to server
         * @param eventName specified name that defines this event on server in DB
         * @param properties set of metrics that classifies this event
         */
        void logEvent(final String eventName, final Map<String, String> properties);
    }

    /**
     * The instance holder
     */
    private static final class InstanceHolder {
        private static ClientAnalytics sInstance = new ClientAnalytics();
    }
}
