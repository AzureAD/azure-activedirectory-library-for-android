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

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

final class HashMapExtensions {

    private static final String TAG = "HashMapExtensions";

    /**
     * decode url string into a key value pairs with default query delimiter.
     * 
     * @param query
     * @return key value pairs
     */
    static final HashMap<String, String> URLFormDecode(String query) {
        HashMap<String, String> result = URLFormDecodeData(query, "&");
        return result;
    }

    /**
     * decode url string into a key value pairs with given query delimiter given
     * string as a=1&b=2 will return key value of [[a,1],[b,2]].
     * 
     * @param parameters
     * @param delimiter
     * @return key value pairs
     */
    static final HashMap<String, String> URLFormDecodeData(String parameters, String delimiter) {
        HashMap<String, String> result = new HashMap<String, String>();

        if (!StringExtensions.IsNullOrBlank(parameters)) {
            StringTokenizer parameterTokenizer = new StringTokenizer(parameters, delimiter);

            while (parameterTokenizer.hasMoreTokens()) {
                String pair = parameterTokenizer.nextToken();
                String[] elements = pair.split("=");

                if (elements != null && elements.length == 2) {
                    String key = null;
                    String value = null;
                    try {
                        key = StringExtensions.URLFormDecode(elements[0].trim());
                        value = StringExtensions.URLFormDecode(elements[1].trim());
                    } catch (UnsupportedEncodingException e) {
                        Log.d(TAG, e.getMessage());
                    }

                    if (!StringExtensions.IsNullOrBlank(key)
                            && !StringExtensions.IsNullOrBlank(value)) {
                        result.put(key, value);
                    }
                }
            }
        }

        return result;
    }
    
    
    /**
     * get key value pairs from response.
     * @param webResponse
     * @return
     * @throws JSONException
     */
    static final HashMap<String, String> getJsonResponse(HttpWebResponse webResponse) throws JSONException{
        HashMap<String, String> response = new HashMap<String, String>();
        if(webResponse != null && webResponse.getBody() != null && webResponse.getBody().length != 0){
            JSONObject jsonObject = new JSONObject(
                    new String(webResponse.getBody()));

            Iterator<?> i = jsonObject.keys();

            while (i.hasNext()) {
                String key = (String) i.next();
                response.put(key,
                        jsonObject.getString(key));
            }
        }
        return response;
    }

}
