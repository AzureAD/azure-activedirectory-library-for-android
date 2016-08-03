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

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

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

                if (elements.length == 2) {
                    String key = null;
                    String value = null;
                    try {
                        key = StringExtensions.urlFormDecode(elements[0].trim());
                        value = StringExtensions.urlFormDecode(elements[1].trim());
                    } catch (UnsupportedEncodingException e) {
                        Log.d(TAG, e.getMessage());
                    }

                    if (!StringExtensions.isNullOrBlank(key)
                            && !StringExtensions.isNullOrBlank(value)) {
                        result.put(key, value);
                    }
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

}
