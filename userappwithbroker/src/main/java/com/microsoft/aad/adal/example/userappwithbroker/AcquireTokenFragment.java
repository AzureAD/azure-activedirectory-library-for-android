//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.

package com.microsoft.aad.adal.example.userappwithbroker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.microsoft.aad.adal.PromptBehavior;

import java.util.ArrayList;

/**
 * AcquireToken Fragment, contains the flow for acquireToken interactively, acquireTokenSilent, getUsers, removeUser.
 */
public class AcquireTokenFragment extends Fragment {
    private Spinner mAuthority;
    private EditText mLoginhint;
    private EditText mExtraQp;
    private Spinner mResource;
    private Spinner mPromptBehavior;
    private Spinner mClientId;
    private Spinner mRedirectUri;

    private Button mAcquireToken;
    private Button mAcquireTokenSilent;

    private OnFragmentInteractionListener mOnFragmentInteractionListener;

    public AcquireTokenFragment() {
        // left empty
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_acquire, container, false);

        mAuthority = (Spinner) view.findViewById(R.id.authorityType);
        mLoginhint = (EditText) view.findViewById(R.id.loginHint);
        mClientId = (Spinner) view.findViewById(R.id.client_id);
        mRedirectUri = (Spinner) view.findViewById(R.id.redirect_uri);
        mResource = (Spinner) view.findViewById(R.id.data_profile);

        mPromptBehavior = (Spinner) view.findViewById(R.id.prompt_behavior);

        mExtraQp = (EditText) view.findViewById(R.id.extraQP);
        mAcquireToken = (Button) view.findViewById(R.id.btn_acquiretoken);
        mAcquireTokenSilent = (Button) view.findViewById(R.id.btn_acquiretokensilent);

        bindSpinnerChoice(mAuthority, Constants.AuthorityType.class);
        bindSpinnerChoice(mPromptBehavior, PromptBehavior.class);
        bindSpinnerChoice(mResource, Constants.DataProfile.class);
        bindSpinnerChoice(mClientId, Constants.ClientId.class);
        bindSpinnerChoice(mRedirectUri, Constants.RedirectUri.class);

        mAcquireToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnFragmentInteractionListener.onAcquireTokenClicked(getCurrentRequestOptions());
            }
        });

        mAcquireTokenSilent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnFragmentInteractionListener.onAcquireTokenSilentClicked(getCurrentRequestOptions());
            }
        });

        return view;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mOnFragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new IllegalStateException("OnFragmentInteractionListener is not implemented");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnFragmentInteractionListener = null;
    }

    void bindSpinnerChoice(final Spinner spinner, final Class<? extends Enum> spinnerChoiceClass) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item,
                new ArrayList<String>() {{
                        for (Enum choice : spinnerChoiceClass.getEnumConstants())
                            add(choice.name());
                }}
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    RequestOptions getCurrentRequestOptions() {
        final Constants.AuthorityType authorityType = Constants.AuthorityType.valueOf(mAuthority.getSelectedItem().toString()) ;
        final String loginHint = mLoginhint.getText().toString();
        final String extraQp = mExtraQp.getText().toString();
        final Constants.DataProfile resource = Constants.DataProfile.valueOf(mResource.getSelectedItem().toString());
        final PromptBehavior behavior = PromptBehavior.valueOf(mPromptBehavior.getSelectedItem().toString());
        final Constants.RedirectUri redirectUri = Constants.RedirectUri.valueOf(mRedirectUri.getSelectedItem().toString());
        final Constants.ClientId clientId = Constants.ClientId.valueOf(mClientId.getSelectedItem().toString());
        return RequestOptions.create(authorityType, loginHint, extraQp, resource, behavior, clientId, redirectUri);
    }

    static class RequestOptions {
        final Constants.AuthorityType mAuthorityType;
        final String mLoginHint;
        final String mExtraQp;
        final Constants.DataProfile mResource;
        final PromptBehavior mBehavior;
        final Constants.RedirectUri mRedirectUri;
        final Constants.ClientId mClientId;

        RequestOptions(final Constants.AuthorityType authorityType, final String loginHint,
                       final String extraQp, final Constants.DataProfile dataProfile,
                       final PromptBehavior behavior, final Constants.ClientId clientId,
                       final Constants.RedirectUri redirectUri) {
            mAuthorityType = authorityType;
            mLoginHint = loginHint;
            mExtraQp = extraQp;
            mResource = dataProfile;
            mBehavior = behavior;
            mClientId = clientId;
            mRedirectUri = redirectUri;
        }

        static RequestOptions create(final Constants.AuthorityType authority, final String loginHint, final String extraQp, final Constants.DataProfile dataProfile,
                                     final PromptBehavior behavior, final Constants.ClientId clientId, final Constants.RedirectUri redirectUri) {
            return new RequestOptions(authority, loginHint, extraQp, dataProfile, behavior, clientId, redirectUri);
        }

        Constants.AuthorityType getAuthorityType() {
            return mAuthorityType;
        }

        String getLoginHint() {
            return mLoginHint;
        }

        String getExtraQp() {
            return mExtraQp;
        }

        Constants.DataProfile getDataProfile() {
            return mResource;
        }

        PromptBehavior getBehavior() {
            return mBehavior;
        }

        Constants.ClientId getClientId() {
            return mClientId;
        }

        Constants.RedirectUri getRedirectUri() {
            return mRedirectUri;
        }
    }

    public interface OnFragmentInteractionListener {

        void onAcquireTokenClicked(final RequestOptions requestOptions);

        void onAcquireTokenSilentClicked(final RequestOptions requestOptions);
    }
}
