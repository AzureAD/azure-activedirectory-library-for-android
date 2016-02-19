// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

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

    private static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return iso8601Format;
    }

    public DateTimeAdapter() {
    }

    @Override
    public synchronized Date deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
        String jsonString = json.getAsString();

        try {
            return localFormat.parse(jsonString);
        } catch (ParseException ignored) {
        }

        try {
            return enUsFormat.parse(jsonString);
        } catch (ParseException ignored) {
        }

        try {
            return iso8601Format.parse(jsonString);
        } catch (ParseException e) {
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
