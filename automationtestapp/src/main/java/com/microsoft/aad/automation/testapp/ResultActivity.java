//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.automation.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

/**
 * Activity that is used to display the result sent back, could be success case or error case.
 */
public class ResultActivity extends AppCompatActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mTextView = (TextView) findViewById(R.id.resultInfo);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        String resultText;
        try {
            resultText = convertIntentDataToJsonString();
        } catch (final JSONException e) {
            resultText = "{\"error \" : \"Unable to convert to JSON\"}";
        }
        mTextView.setText(resultText);

        final Button doneButton = (Button) findViewById(R.id.resultDone);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResultActivity.this.finish();
            }
        });
    }

    private String convertIntentDataToJsonString() throws JSONException {
        final Intent intent = getIntent();

        final JSONObject jsonObject = new JSONObject();

        if (!TextUtils.isEmpty(intent.getStringExtra(Constants.ACCESS_TOKEN))) {
            jsonObject.put(Constants.ACCESS_TOKEN, intent.getStringExtra(Constants.ACCESS_TOKEN));
            jsonObject.put(Constants.ACCESS_TOKEN_TYPE, intent.getStringExtra(Constants.ACCESS_TOKEN_TYPE));
            jsonObject.put(Constants.REFRESH_TOKEN, intent.getStringExtra(Constants.REFRESH_TOKEN));
            jsonObject.put(Constants.EXPIRES_ON, new Date(intent.getLongExtra(Constants.EXPIRES_ON, 0)));
            jsonObject.put(Constants.TENANT_ID, intent.getStringExtra(Constants.TENANT_ID));
            jsonObject.put(Constants.UNIQUE_ID, intent.getStringExtra(Constants.UNIQUE_ID));
            jsonObject.put(Constants.DISPLAYABLE_ID, intent.getStringExtra(Constants.DISPLAYABLE_ID));
            jsonObject.put(Constants.FAMILY_NAME, intent.getStringExtra(Constants.FAMILY_NAME));
            jsonObject.put(Constants.GIVEN_NAME, intent.getStringExtra(Constants.GIVEN_NAME));
            jsonObject.put(Constants.IDENTITY_PROVIDER, intent.getStringExtra(Constants.IDENTITY_PROVIDER));
            jsonObject.put(Constants.ID_TOKEN, intent.getStringExtra(Constants.ID_TOKEN));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(Constants.ERROR))) {
            jsonObject.put(Constants.ERROR, intent.getStringExtra(Constants.ERROR));
            jsonObject.put(Constants.ERROR_DESCRIPTION, intent.getStringExtra(Constants.ERROR_DESCRIPTION));
            jsonObject.put(Constants.ERROR_CAUSE, intent.getSerializableExtra(Constants.ERROR_CAUSE));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(Constants.EXPIRED_ACCESS_TOKEN_COUNT))) {
            jsonObject.put(Constants.EXPIRED_ACCESS_TOKEN_COUNT, intent.getStringExtra(Constants.EXPIRED_ACCESS_TOKEN_COUNT));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(Constants.INVALIDATED_REFRESH_TOKEN_COUNT))) {
            jsonObject.put(Constants.INVALIDATED_REFRESH_TOKEN_COUNT, intent.getStringExtra(Constants.INVALIDATED_REFRESH_TOKEN_COUNT));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(Constants.INVALIDATED_FAMILY_REFRESH_TOKEN_COUNT))) {
            jsonObject.put(Constants.INVALIDATED_FAMILY_REFRESH_TOKEN_COUNT, intent.getStringExtra(Constants.INVALIDATED_FAMILY_REFRESH_TOKEN_COUNT));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(Constants.CLEARED_TOKEN_COUNT))) {
            jsonObject.put(Constants.CLEARED_TOKEN_COUNT, intent.getStringExtra(Constants.CLEARED_TOKEN_COUNT));
        } else if (intent.getStringArrayListExtra(Constants.READ_CACHE) != null) {
            final ArrayList<String> items = intent.getStringArrayListExtra(Constants.READ_CACHE);
            jsonObject.put(Constants.ITEM_COUNT, items.size());

            final ArrayList<String> itemsWithCount = new ArrayList<>();
            itemsWithCount.addAll(items);
            final JSONArray arrayItems = new JSONArray(itemsWithCount);
            jsonObject.put("items", arrayItems);
        } else if (intent.getStringArrayListExtra(Constants.READ_LOGS) != null) {
            final ArrayList<String> items = intent.getStringArrayListExtra(Constants.READ_LOGS);
            jsonObject.put(Constants.ITEM_COUNT, items.size());
            final ArrayList<String> itemsWithCount = new ArrayList<>();
            itemsWithCount.addAll(items);
            final JSONArray arrayItems = new JSONArray(itemsWithCount);
            jsonObject.put("items", arrayItems);
        }

        return jsonObject.toString();
    }
}
