package com.microsoft.aad.automation.testapp.utility;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.microsoft.aad.automation.testapp.MainActivityTest2;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class AzureTokenVolley {
    private static String V2Endpoint;
    private static String ENCODED_SCOPE;
    private static String RESPONSE_STRING;
    private static String CLIENT_ID;
    private static String CLIENT_SECRET;
    private static String token;

    private static void setEncodedScope(String string)
    {
        String query = "";
        try{
            query = URLEncoder.encode(string, "utf-8");
        } catch (Exception e){

        }
        ENCODED_SCOPE = query;
    }

    private static void setEndpoint(String tenantID){
        V2Endpoint = "https://login.microsoftonline.com/" + tenantID + "/oauth2/v2.0/token";
    }

    private static void setClientCreds(String clientSecret, String clientID){
        CLIENT_SECRET = clientSecret;
        CLIENT_ID = clientID;
    }


    public String getaccessTokenVolley(String clientSecret, String clientID, String tenantID, String scope) {
        setEncodedScope(scope);
        setEndpoint(tenantID);
        setClientCreds(clientSecret,clientID);

        // Instantiate the RequestQueue with the cache and network.
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivityTest2.getContext());

        // Start the queue
        requestQueue.start();

        try{
            StringRequest stringRequest = new StringRequest(Request.Method.POST, V2Endpoint,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the response string.
                            RESPONSE_STRING = response;
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // log error here.
                }
            }){
                @Override
                protected Map<String,String> getParams(){
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("clientID", CLIENT_ID);
                    params.put("clientSecret", CLIENT_SECRET);
                    params.put("scope", ENCODED_SCOPE);
                    params.put("grant_type", "client_credentials");

                    return params;
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("Content-Type","application/x-www-form-urlencoded");
                    return params;
                }
            };

            requestQueue.add(stringRequest);

        } catch (Exception e){

        }

        try{
            JSONObject jObject = new JSONObject(RESPONSE_STRING);
            token = jObject.getString("access_token");
        } catch (JSONException e){}
        return token;
    }
}
