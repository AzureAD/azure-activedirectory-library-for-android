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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.aad.adal.TelemetryUtils.CliTelemInfo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class HttpEventTest {

    @Test
    public void testProcessEvent() throws MalformedURLException {
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setHttpPath(new URL("https://login.microsoftonline.com/contoso/oauth2/token"));
        event.setOauthErrorCode("interaction_required");
        event.setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);

        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);

        assertFalse(dispatchMap.isEmpty());
        assertTrue(dispatchMap.containsKey(EventStrings.OAUTH_ERROR_CODE));
        assertTrue(dispatchMap.containsKey(EventStrings.HTTP_PATH));

        final String httpPath = dispatchMap.get(EventStrings.HTTP_PATH);
        assertTrue(httpPath.equals("https://login.microsoftonline.com/oauth2/token/"));
    }

    @Test
    public void testADFSAuthorityPath() throws MalformedURLException {
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setHttpPath(new URL("https://contoso.com/adfs/ls"));

        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);

        // The path should not be there, so the map should have only the count element
        assertNull(dispatchMap.get(EventStrings.HTTP_PATH));
        assertTrue(dispatchMap.get(EventStrings.HTTP_EVENT_COUNT).equals("1"));
    }

    @Test
    public void testAADAuthorityPath() throws MalformedURLException {
        final HttpEvent event = new HttpEvent((EventStrings.HTTP_EVENT));
        event.setHttpPath(new URL("https://login.microsoftonline.com/myTenant.com/oauth2/"));

        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);

        // The path should be here
        assertNotNull(dispatchMap.get(EventStrings.HTTP_PATH));
        assertTrue(dispatchMap.get(EventStrings.HTTP_PATH).equals("https://login.microsoftonline.com/oauth2/"));
        assertTrue(dispatchMap.get(EventStrings.HTTP_EVENT_COUNT).equals("1"));
    }

    @Test
    public void testOverlappingHttpEvent() {
        final HttpEvent event = new HttpEvent((EventStrings.HTTP_EVENT));
        event.setOauthErrorCode("some error");
        event.setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST);

        final HttpEvent event2 = new HttpEvent(EventStrings.HTTP_EVENT);
        event2.setResponseCode(HttpURLConnection.HTTP_OK);

        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        event2.processEvent(dispatchMap);

        assertTrue(dispatchMap.get(EventStrings.OAUTH_ERROR_CODE).isEmpty());
        assertTrue(dispatchMap.get(EventStrings.HTTP_RESPONSE_CODE).equals(String.valueOf(HttpURLConnection.HTTP_OK)));
    }

    @Test
    public void testSpeRingInfoStrangeFormatting() {
        final String speHeaderStr = "1 , ,, ,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testEmptySpeRingInfo() {
        final String speHeaderStr = ",,,,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testVersionOnlySpeRingInfo() {
        final String speHeaderStr = "1,,,,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingWithVersionAndAgeOnly() {
        final String speHeaderStr = "1,,,1234.1234,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals("1234.1234", dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingInfoWithBlankFieldsInnerRing() {
        final String speHeaderStr = "1,0,0,,I";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals("I", dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingInfoWithSubErrorCodeAndAgeOnly() {
        final String speHeaderStr = "1,,1,1234.1234,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals("1", dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals("1234.1234", dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingWithUnsupportedVersion() {
        final String speHeaderStr = "2,1,2,3.3,I";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingWithErrorAndSubError() {
        final String speHeaderStr = "1,2,3,,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals("2", dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals("3", dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingWithLeadingWhitespaceAgeInner() {
        final String speHeaderStr = "1,,, 1234.1234,I";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals("1234.1234", dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals("I", dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingWithVersionAndRingOnly() {
        final String speHeaderStr = "1,,,,I";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals("I", dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpedRingEmpty() {
        final String speHeaderStr = "";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingWrongLength1() {
        final String speHeaderStr = ",";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingWrongLength2() {
        final String speHeaderStr = ",,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingWrongLength3() {
        final String speHeaderStr = ",,,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingTooLong() {
        final String speHeaderStr = ",,,,,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingTooShort() {
        final String speHeaderStr = "1,00";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingTooShort2() {
        final String speHeaderStr = "1,0,0";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingTooShort3() {
        final String speHeaderStr = "1,1,1,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testSpeRingTooLong2() {
        final String speHeaderStr = "1,1,1,,,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testVersionWithMajorMinor() {
        final String speHeaderStr = "1.2,1,2,12.34,";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testVersionWithMajorMinorPatch() {
        final String speHeaderStr = "1.2.3,1,2,12.34,I";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }

    @Test
    public void testMultiDigitVersionWithMajorMinorPatch() {
        final String speHeaderStr = "11.22.33,1,2,12.34,I";
        final CliTelemInfo cliTelemInfo = TelemetryUtils.parseXMsCliTelemHeader(speHeaderStr);
        final HttpEvent event = new HttpEvent(EventStrings.HTTP_EVENT);
        event.setXMsCliTelemData(cliTelemInfo);
        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_ERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.SERVER_SUBERROR_CODE));
        assertEquals(null, dispatchMap.get(EventStrings.TOKEN_AGE));
        assertEquals(null, dispatchMap.get(EventStrings.SPE_INFO));
    }
}
