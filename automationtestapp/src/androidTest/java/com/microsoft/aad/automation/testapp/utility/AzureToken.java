package com.microsoft.aad.automation.testapp.utility;

import android.app.Activity;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.InputStreamReader;
import java.net.URLEncoder;

public class AzureToken extends Activity {
    private static String V2Endpoint;
    private static String ENCODED_SCOPE;


    private static String postMethod(String url, String body, int timeout, String method) {
        HttpURLConnection connection = null;
        try {

            URL u = new URL(url);
            connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod(method);

            //set the sending type and receiving type to json
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Accept", "application/x-www-form-urlencoded");

            connection.setAllowUserInteraction(false);
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);

            if (body != null) {
                //set the content length of the body
                connection.setRequestProperty("Content-length", body.getBytes().length + "");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);

                //send the json as body of the request
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(body.getBytes("UTF-8"));
                outputStream.close();
            }

            //Connect to the server
            connection.connect();

            int status = connection.getResponseCode();
            switch (status) {
                case 200:
                case 201:
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    bufferedReader.close();
                    //return received string
                    return sb.toString();
            }

        } catch (MalformedURLException ex) {
            // log error in http connection here.
        } catch (IOException ex) {
            // log error in http connection here.
        } catch (Exception ex) {
            // log error in http connection here.
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ex) {
                    // log error in http connection here.
                }
            }
        }
        return null;
    }

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

    public static String getaccessTokenJAVA(String clientSecret, String clientID, String tenantID, String scope) {
        setEncodedScope(scope);
        setEndpoint(tenantID);

        String postData = "grant_type=client_credentials" +
                "&client_id=" + clientID +
                "&client_secret=" + clientSecret +
                "&scope=" + ENCODED_SCOPE;

        String rawResponse = postMethod(V2Endpoint,postData,15000,"POST");
        String token = "";

        try{
            JSONObject jObject = new JSONObject(rawResponse);
            token = jObject.getString("access_token");
        } catch (Exception JSONException){}

        return token;
    }
}
