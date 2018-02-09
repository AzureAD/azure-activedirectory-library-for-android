package com.microsoft.aad.adal;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v4.app.JobIntentService;
import android.widget.Toast;

public class AuthJobIntentService extends JobIntentService {
    /**
     * Unique job ID for this service.
     */
    private static final String TAG = "AuthJobIntentService:Heidi";
    static final int JOB_ID = 889288;
    static final String BROKER_AUTH_REQUEST = ".BROKER_AUTH";

    /**
     * Convenience method for enqueuing work in to this service.
     */
    static void enqueueWork(Context context, Intent work) {
        Logger.i(TAG, "Enqueuing work. ", "Details: " + work);
        enqueueWork(context, AuthJobIntentService.class, JOB_ID, work);
    }

    protected void onHandleWork(Intent intent) {
        // We have received work to do.  The system or framework is already
        // holding a wake lock for us at this point, so we can just go.
        Logger.i(TAG, "Executing work. ", "Details: " + intent);
        String label = intent.getStringExtra("label");
        if (label == null) {
            label = intent.toString();
        }
        toast("Executing: " + label);
        for (int i = 0; i < 5; i++) {
            Logger.i(TAG, "Running service " + (i + 1)
                    + "/5 @ " + SystemClock.elapsedRealtime(), "");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        Logger.i(TAG, "Completed service @ " + SystemClock.elapsedRealtime(), "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        toast("All work complete");
    }

    final Handler mHandler = new Handler();

    // Helper for showing tests
    void toast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(AuthJobIntentService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
