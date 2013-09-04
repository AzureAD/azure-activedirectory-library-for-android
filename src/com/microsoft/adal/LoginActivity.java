/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */
package com.microsoft.adal;

import java.io.UnsupportedEncodingException;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

/**
 * TODO: overwrite webview functions
 * TODO: when url is reached call the complete to "setresult" for the calling activity
 * Activity will launch webview that is defined static. It will set the result back to calling activity.
 * @author omercan
 *
 */
public class LoginActivity extends Activity {

    private final String TAG = "com.microsoft.adal.LoginActivity";
    private Button btnCancel;
    private WebView wv;    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Get the message from the intent
        Intent intent = getIntent();
        final AuthenticationRequest request = (AuthenticationRequest) intent.getSerializableExtra(AuthenticationContext.BROWSER_REQUEST_MESSAGE);
        
     // cancel action will send the request back to onActivityResult method
        btnCancel = (Button)findViewById(R.id.btnCancel);
        
        btnCancel.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                setResult(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, resultIntent);    
            }
        });
        
        
        // Create the Web View to show the login page
        wv = (WebView)findViewById(R.id.webView1);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.requestFocus(View.FOCUS_DOWN);

        // Set focus to the view for touch event
        wv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN
                        || action == MotionEvent.ACTION_UP) {
                    if (!view.hasFocus()) {
                        view.requestFocus();
                    }
                }
                return false;
            }
        });

        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setLoadWithOverviewMode(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setUseWideViewPort(true);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.getSettings().setPluginState(WebSettings.PluginState.ON);
        wv.getSettings().setPluginsEnabled(true);     
        wv.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, final String url) {
                Log.d(TAG, "Override:"+url);
                
                view.loadUrl(url);
                return true;
            }
        });
        
        String startUrl = "about:blank";    
        wv.loadUrl(startUrl);
        
        try {
            startUrl = request.getCodeRequestUrl();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
            
            Intent resultIntent = new Intent();
            resultIntent.putExtra(AuthenticationContext.BROWSER_RESPONSE_ERROR_REQUEST, request);
            setResult(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);  
        }
        
        wv.loadUrl(startUrl);
    }

   
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

}
