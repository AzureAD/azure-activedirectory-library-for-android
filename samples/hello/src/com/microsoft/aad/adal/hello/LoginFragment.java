
package com.microsoft.aad.adal.hello;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.aad.adal.AuthenticationContext;
import com.microsoft.aad.adal.AuthenticationResult;
import com.microsoft.aad.adal.IWindowComponent;
import com.microsoft.aad.adal.PromptBehavior;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private static TextView textView;

    private AuthenticationContext mAuthContext;

    private AuthenticationResult mResult;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // Provide key info for Encryption
            if (Build.VERSION.SDK_INT < 18) {
                Utils.setupKeyForSample();
            }

            // init authentication Context
            mAuthContext = new AuthenticationContext(this.getActivity().getApplicationContext(),
                    Constants.AUTHORITY_URL, false);
        } catch (Exception e) {
            Toast.makeText(this.getActivity().getApplicationContext(), "Encryption failed",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_login, container, false);

        textView = (TextView)view.findViewById(R.id.textView1);

        final Button button = (Button)view.findViewById(R.id.buttonLogin);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonClicked(v);
            }
        });

        if (mResult != null && mResult.getUserInfo() != null
                && mResult.getUserInfo().getDisplayableId() != null) {
            textView.setText(mResult.getUserInfo().getDisplayableId());
        }

        return view;
    }

    public void buttonClicked(View view) {
        if (mResult != null) {
            // logout
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            CookieSyncManager.getInstance().sync();
            mAuthContext.getCache().removeAll();
        } else {
            // login
            mAuthContext.acquireToken(wrapFragment(LoginFragment.this), Constants.RESOURCE_ID,
                    Constants.CLIENT_ID, Constants.REDIRECT_URL, "", PromptBehavior.Auto, "",
                    getCallback());
        }
    }

    private IWindowComponent wrapFragment(final Fragment fragment) {
        return new IWindowComponent() {
            Fragment refFragment = fragment;

            @Override
            public void startActivityForResult(Intent intent, int requestCode) {
                refFragment.startActivityForResult(intent, requestCode);
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mAuthContext.onActivityResult(requestCode, resultCode, data);
    }

    private void showInfo(String msg) {
        Log.d(TAG, msg);
    }

    private AuthenticationCallback<AuthenticationResult> getCallback() {
        return new AuthenticationCallback<AuthenticationResult>() {

            @Override
            public void onError(Exception exc) {
                showInfo("getToken Error:" + exc.getMessage());
            }

            @Override
            public void onSuccess(AuthenticationResult result) {
                mResult = result;
                Log.v(TAG, "Token info:" + result.getAccessToken());
                Log.v(TAG, "IDToken info:" + result.getIdToken());
                showInfo("Token is returned");

                if (mResult.getUserInfo() != null) {
                    Log.v(TAG, "User info userid:" + result.getUserInfo().getUserId()
                            + " displayableId:" + result.getUserInfo().getDisplayableId());
                    textView.setText(result.getUserInfo().getDisplayableId());
                }
            }
        };
    }
}
