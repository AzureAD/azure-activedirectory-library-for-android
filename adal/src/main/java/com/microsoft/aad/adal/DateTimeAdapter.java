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

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class DateTimeAdapter implements JsonDeserializer<Date>, JsonSerializer<Date> {

    private static final String TAG = "DateTimeAdapter";

    private final DateFormat enUsFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
            DateFormat.DEFAULT, Locale.US);

    private final DateFormat localFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT,
            DateFormat.DEFAULT);

    private final DateFormat iso8601Format = buildIso8601Format();
    private final DateFormat enUs24HourFormat = buildEnUs24HourDateFormat();
    private final DateFormat local24HourFormat = buildLocal24HourDateFormat();

    private static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }
    
    /**
     * Add new en-us date format for parsing date string if it doesn't contain AM/PM.
     */
    private static DateFormat buildEnUs24HourDateFormat() {
        return new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US);
    }
    
    /**
     * Add new local date format when parsing date string if it doesn't contain AM/PM.
     */
    private static DateFormat buildLocal24HourDateFormat() {
        return new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
    }

    public DateTimeAdapter() {
    }

    @Override
    public synchronized Date deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
        String jsonString = json.getAsString();

        // Datetime string is serialized with iso8601 format by default, should
        // always try to deserialize with iso8601. But to support the backward
        // compatibility, we also need to deserialize with old format if failing
        // with iso8601. 
        try {
            return iso8601Format.parse(jsonString);
        } catch (final ParseException ignored) {
        }
        
        // fallback logic for old date format parsing. 
        try {
            return localFormat.parse(jsonString);
        } catch (final ParseException ignored) {
        }
        
        try {
            return local24HourFormat.parse(jsonString);
        } catch (final ParseException ignored) {
        }

        try {
            return enUsFormat.parse(jsonString);
        } catch (final ParseException ignored) {
        }

        try {
            return enUs24HourFormat.parse(jsonString);
        } catch (final ParseException e) {
            Logger.e(TAG, "Could not parse date: " + e.getMessage(), "",
                    ADALError.DATE_PARSING_FAILURE, e);
        }

        throw new JsonParseException("Could not parse date: " + jsonString);
    }

    @Override
    public synchronized JsonElement serialize(Date src, Type typeOfSrc,
            JsonSerializationContext context) {
        return new JsonPrimitive(iso8601Format.format(src));
    }
}
