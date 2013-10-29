
package com.microsoft.adal;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

public final class HashMapExtensions
{
    /**
     * decode url string into a key value pairs with default query delimiter
     * 
     * @param query
     * @return key value pairs
     */
    static final HashMap<String, String> URLFormDecode(String query)
    {
        HashMap<String, String> result = URLFormDecodeData(query, "&");
        return result;
    }

    /**
     * decode url string into a key value pairs with given query delimiter
     * 
     * @param parameters
     * @param delimiter
     * @return key value pairs
     */
    static final HashMap<String, String> URLFormDecodeData(String parameters, String delimiter)
    {
        HashMap<String, String> result = new HashMap<String, String>();

        if (!StringExtensions.IsNullOrBlank(parameters))
        {
            StringTokenizer parameterTokenizer = new StringTokenizer(parameters, delimiter);

            while (parameterTokenizer.hasMoreTokens())
            {
                String pair = parameterTokenizer.nextToken();
                String[] elements = pair.split("=");

                if (elements != null && elements.length == 2)
                {
                    String key = StringExtensions.URLFormDecode(elements[0].trim());
                    String value = StringExtensions.URLFormDecode(elements[1].trim());

                    if (key != null && key.length() > 0 && value != null && value.length() > 0)
                    {
                        result.put(key, value);
                    }
                }
            }
        }

        return result;
    }

    /**
     * URL form encode a HashMap<String, String> into a string
     */
    static final String URLFormEncode(HashMap<String, String> parameters)
    {
        Iterator<Entry<String, String>> iterator = parameters.entrySet().iterator();
        String result = null;

        while (iterator.hasNext())
        {
            Entry<String, String> entry = iterator.next();

            if (result == null)
            {
                result = String.format("%s=%s", StringExtensions.URLFormEncode(entry.getKey()),
                        StringExtensions.URLFormEncode(entry.getValue()));
            }
            else
            {
                result = String.format("%s&%s=%s", result,
                        StringExtensions.URLFormEncode(entry.getKey()),
                        StringExtensions.URLFormEncode(entry.getValue()));
            }

        }

        return result;
    }
}
