
package com.microsoft.aad.adal.hello;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class FragmentHolderActivity extends  FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_holder);
    }
}
