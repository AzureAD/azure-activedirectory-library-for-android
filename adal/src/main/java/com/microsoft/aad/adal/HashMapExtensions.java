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

import android.text.TextUtils;

import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

final class HashMapExtensions {

    private static final String TAG = "HashMapExtensions";

    private HashMapExtensions() {
        // Intentionally left blank
    }
    /**
     * decode url string into a key value pairs with default query delimiter.
     * 
     * @param parameters URL query parameter
     * @return key value pairs
     */
    static HashMap<String, String> urlFormDecode(String parameters) {
        return urlFormDecodeData(parameters, "&");
    }

    /**
     * decode url string into a key value pairs with given query delimiter given
     * string as a=1&b=2 will return key value of [[a,1],[b,2]].
     * 
     * @param parameters URL parameter to be decoded
     * @param delimiter query delimiter
     * @return Map key value pairs
     */
    static HashMap<String, String> urlFormDecodeData(String parameters, String delimiter) {
        final HashMap<String, String> result = new HashMap<>();

        if (!StringExtensions.isNullOrBlank(parameters)) {
            StringTokenizer parameterTokenizer = new StringTokenizer(parameters, delimiter);

            while (parameterTokenizer.hasMoreTokens()) {
                String pair = parameterTokenizer.nextToken();
                String[] elements = pair.split("=");
                String value = null;
                String key = null;

                if (elements.length == 2) {
                    try {
                        key = StringExtensions.urlFormDecode(elements[0].trim());
                        value = StringExtensions.urlFormDecode(elements[1].trim());
                    } catch (UnsupportedEncodingException e) {
                        Logger.i(TAG, ADALError.ENCODING_IS_NOT_SUPPORTED.getDescription(), e.getMessage(), null);
                        continue;
                    }
                } else if (elements.length == 1) {
                    try {
                        key = StringExtensions.urlFormDecode(elements[0].trim());
                        value = "";
                    } catch (UnsupportedEncodingException e) {
                        Logger.i(TAG, ADALError.ENCODING_IS_NOT_SUPPORTED.getDescription(), e.getMessage(), null);
                        continue;
                    }
                }

                if (!StringExtensions.isNullOrBlank(key)) {
                    result.put(key, value);
                }
            }
        }

        return result;
    }
    
    
    /**
     * get key value pairs from response.
     * @param webResponse HttpWebResponse to convert to a map
     * @return Map
     * @throws JSONException
     */
    static Map<String, String> getJsonResponse(HttpWebResponse webResponse) throws JSONException {
        final Map<String, String> response = new HashMap<>();
        if (webResponse != null && !TextUtils.isEmpty(webResponse.getBody())) {
            JSONObject jsonObject = new JSONObject(webResponse.getBody());
            Iterator<?> i = jsonObject.keys();
            while (i.hasNext()) {
                String key = (String) i.next();
                response.put(key, jsonObject.getString(key));
            }
        }
        return response;
    }

    /**
     * Parse json String into HashMap<String, String>.
     * @param jsonString
     * @return HashMap<String, String>
     * @throws JSONException
     */
    static HashMap<String, String> jsonStringAsMap(String jsonString) throws JSONException {
        final HashMap<String, String> responseItems = new HashMap<>();
        if (!StringExtensions.isNullOrBlank(jsonString)) {
            JSONObject jsonObject = new JSONObject(jsonString);
            Iterator<?> i = jsonObject.keys();
            while (i.hasNext()) {
                final String key = (String) i.next();
                responseItems.put(key, jsonObject.getString(key));
            }
        }

        return responseItems;
    }

    /**
     * Parse json String into HashMap<String, List<String>>.
     * @param jsonString
     * @return HashMap<String, List<String>>
     * @throws JSONException
     */
    static HashMap<String, List<String>> jsonStringAsMapList(String jsonString) throws JSONException {
        final HashMap<String, List<String>> responseItems = new HashMap<>();
        if (!StringExtensions.isNullOrBlank(jsonString)) {
            JSONObject jsonObject = new JSONObject(jsonString);
            Iterator<?> i = jsonObject.keys();
            while (i.hasNext()) {
                final String key = (String) i.next();
                final List<String> list = new ArrayList<>();
                final JSONArray json = new JSONArray(jsonObject.getString(key));
                for (int index = 0; index < json.length(); index++) {
                    list.add(json.get(index).toString());
                }
                responseItems.put(key, list);
            }
        }

        return  responseItems;
    }
}
