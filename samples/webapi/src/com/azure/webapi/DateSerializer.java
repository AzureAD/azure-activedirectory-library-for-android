/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Apache 2.0 License
 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
     http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.azure.webapi;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Date Serializer/Deserializer to make Mobile Services and Java dates
 * compatible
 */
@SuppressLint("SimpleDateFormat")
public class DateSerializer implements JsonSerializer<Date>, JsonDeserializer<Date> {

	final static String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	final static String TIME_FORMAT_DETAIL = "yyyy-MM-dd'T'HH:mm:ss'.'SSZ";

	/**
	 * Deserializes a JsonElement containing an ISO-8601 formatted date
	 */
	@Override
	public Date deserialize(JsonElement element, Type type,
			JsonDeserializationContext ctx) throws JsonParseException {
		String strVal = element.getAsString();

		try {
			return deserialize(strVal);
		} catch (ParseException e) {
			throw new JsonParseException(e);
		}

	}

	/**
	 * Serializes a Date to a JsonElement containing a ISO-8601 formatted date
	 */
	@Override
	public JsonElement serialize(Date date, Type type,
			JsonSerializationContext ctx) {
		JsonElement element = new JsonPrimitive(serialize(date));
		return element;
	}

	/**
	 * TODO add tests for this Deserializes an ISO-8601 formatted date WebAPI
	 * also sends in this format
	 */
	public static Date deserialize(String strVal) throws ParseException {
		// Change Z to +00:00 to adapt the string to a format
		// that can be parsed in Java
		String s = strVal.replace("Z", "+0000");
		// it may not have Z or +00:00 at the end for WEBAPI
		if (s.length() < 23) {
			s = s.concat("+0000");
		}

		// it includes timezone with minutes
		int lasti = s.lastIndexOf(":");
		if (lasti > 20) {
			s = s.substring(0, lasti) + s.substring(lasti+1);

		}

		// SimpleDateFormat is not compliant to ISO-8601 so last time zone needs
		// to converted
		// Parse the well-formatted date string
		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			Date date = dateFormat.parse(s);
			return date;
		} catch (ParseException ex) {
			Log.d("DateSerializer", ex.getMessage());
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT_DETAIL);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date date = dateFormat.parse(s);
		return date;
	}

	/**
	 * Serializes a Date object to an ISO-8601 formatted date string
	 */
	public static String serialize(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				TIME_FORMAT, Locale.getDefault());
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		String formatted = dateFormat.format(date);

		return formatted;
	}

}
