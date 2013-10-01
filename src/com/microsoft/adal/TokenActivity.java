
package com.microsoft.adal;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;

/**
 * This is for broker flow.
 * this activity uses authentication context locally to query cache, get new tokens etc.
 * If loginActivity starts, it will return here. Final return to callback will set results back to original caller.
 * @author omercan
 *
 */
public class TokenActivity extends Activity {

    public static String TAG = "TokenActivity";
    
    private AuthenticationContext mAuthContext;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);
        
     // Get the message from the intent
        Intent intent = getIntent();
        final AuthenticationRequest request = (AuthenticationRequest) intent
                .getSerializableExtra(AuthenticationConstants.BROWSER_REQUEST_MESSAGE);
        Log.d(TAG, "OnCreate redirect"+request.getRedirectUri());
        
        
        //create new context and use local to do remaining actions
        // it will query local storage, refresh inside broker's local, and so on.
        mAuthContext = new AuthenticationContext(this, request.getAuthority());
        AuthenticationOptions options = new AuthenticationOptions();
        //TODO set options for broker call
        
        mAuthContext.acquireTokenLocal(TokenActivity.this, request, options, new AuthenticationCallback() {
            
            @Override
            public void onError(Exception exc) {

                Intent resultIntent = new Intent();
                resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_ERROR_CODE, exc.getMessage()); //TODO
                resultIntent
                        .putExtra(AuthenticationConstants.BROWSER_RESPONSE_ERROR_MESSAGE, exc.getMessage());
                resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_REQUEST_INFO, request);
                ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR, resultIntent);
            }
            
            @Override
            public void onCompleted(AuthenticationResult result) {
                // TODO Auto-generated method stub
                Intent resultIntent = new Intent();
                resultIntent.putExtra(AuthenticationConstants.BROKER_RESPONSE, result);
                resultIntent.putExtra(AuthenticationConstants.BROWSER_RESPONSE_REQUEST_INFO, request);
                ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE, resultIntent);                
            }
            
            @Override
            public void onCancelled() {
                // TODO Auto-generated method stub
                Intent resultIntent = new Intent();
                ReturnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL, resultIntent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.token, menu);
        return true;
    }
    
    private void ReturnToCaller(int resultCode, Intent data)
    {
        Log.d(TAG, "ReturnToCaller=" + resultCode);
        setResult(resultCode, data);
        this.finish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "broker code returned" + requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        mAuthContext.onActivityResult(requestCode, resultCode, data);
    }
}
