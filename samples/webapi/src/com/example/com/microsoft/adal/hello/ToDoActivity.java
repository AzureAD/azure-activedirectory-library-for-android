
package com.example.com.microsoft.adal.hello;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.azure.webapi.DateSerializer;
import com.azure.webapi.LongSerializer;
import com.azure.webapi.MobileServiceClient;
import com.azure.webapi.MobileServiceQueryOperations;
import com.azure.webapi.MobileServiceTable;
import com.azure.webapi.MobileServiceUser;
import com.azure.webapi.NextServiceFilterCallback;
import com.azure.webapi.ServiceFilter;
import com.azure.webapi.ServiceFilterRequest;
import com.azure.webapi.ServiceFilterResponse;
import com.azure.webapi.ServiceFilterResponseCallback;
import com.azure.webapi.TableOperationCallback;
import com.azure.webapi.TableQueryCallback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.microsoft.adal.AuthenticationCallback;
import com.microsoft.adal.AuthenticationContext;
import com.microsoft.adal.AuthenticationResult;

/*
 * TODO:
 * 1- q	
 */
public class ToDoActivity extends Activity {

    private final static String TAG = "ToDoActivity";

    public static final int MENU_LOGOUT = Menu.FIRST;

    public static final int MENU_CLEAR_TOKEN = Menu.FIRST + 1;

    public static final int MENU_GET_TOKEN = Menu.FIRST + 2;

    public static final int MENU_GET_NEWSFEED = Menu.FIRST + 3;

    public static final int MENU_GET_SETTINGS = Menu.FIRST + 4;

    public static final int MENU_GET_LAYOUT_DEMO = Menu.FIRST + 5;

    private AuthenticationContext mAuthContext;

    private boolean refreshInProgress = false;

    /**
     * Mobile Service Client reference
     */
    private MobileServiceClient mClient;

    /**
     * Mobile Service Table used to access data
     */
    private MobileServiceTable<WorkItem> mToDoTable = null;

    /**
     * Adapter to sync the items list with the view
     */
    private WorkItemAdapter mAdapter = null;

    /**
     * EditText containing the "New ToDo" text
     */
    private EditText mTextNewToDo;

    /**
     * Progress spinner to use for table operations
     */
    private ProgressBar mProgressBar;

    /**
     * Show this dialog when activity first launches to check if user has login
     * or not.
     */
    private ProgressDialog mLoginProgressDialog;

    private String mToken;

    private static int pickerYear;

    private static int pickerMonth;

    private static int pickerDayOfMonth;

    /**
     * Initializes the activity
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_do);

        Toast.makeText(getApplicationContext(), TAG + "LifeCycle: OnCreate", Toast.LENGTH_SHORT)
                .show();

        mProgressBar = (ProgressBar)findViewById(R.id.loadingProgressBar);

        // Initialize the progress bar
        mProgressBar.setVisibility(ProgressBar.GONE);

        mLoginProgressDialog = new ProgressDialog(this);
        mLoginProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLoginProgressDialog.setMessage("Login in progress...");
        mLoginProgressDialog.show();
        refreshInProgress = false;

        // Ask for token and provide callback
        mAuthContext = new AuthenticationContext(ToDoActivity.this, Constants.AUTHORITY_URL, false);

        mAuthContext.acquireToken(ToDoActivity.this, Constants.RESOURCE_ID, Constants.CLIENT_ID,
                Constants.REDIRECT_URL, Constants.USER_HINT,
                new AuthenticationCallback<AuthenticationResult>() {

                    @Override
                    public void onError(Exception exc) {
                        if (mLoginProgressDialog.isShowing()) {
                            mLoginProgressDialog.dismiss();
                        }
                        Toast.makeText(getApplicationContext(),
                                TAG + "getToken Error:" + exc.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                        navigateToLogOut();
                    }

                    @Override
                    public void onSuccess(AuthenticationResult result) {
                        if (mLoginProgressDialog.isShowing()) {
                            mLoginProgressDialog.dismiss();
                        }

                        if (result != null && !result.getAccessToken().isEmpty()) {
                            setLocalToken(result.getAccessToken());
                            sendRequest();
                        } else {
                            navigateToLogOut();
                        }
                    }
                });

        Toast.makeText(getApplicationContext(), TAG + "done", Toast.LENGTH_SHORT).show();
    }

    private void sendRequest() {

        if (refreshInProgress || mToken == null || mToken.isEmpty())
            return;

        refreshInProgress = true;

    }

    private URL getEndpointUrl() {
        URL endpoint = null;
        try {
            endpoint = new URL(Constants.SERVICE_URL + "/api/" + Constants.TABLE_WORKITEM);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return endpoint;
    }

    /**
     * Register gson serializer for long and date type
     * 
     * @return
     */
    public static GsonBuilder createServiceGsonBuilder() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        // Register custom date serializer/deserializer
        gsonBuilder.registerTypeAdapter(Date.class, new DateSerializer());
        LongSerializer longSerializer = new LongSerializer();
        gsonBuilder.registerTypeAdapter(Long.class, longSerializer);
        gsonBuilder.registerTypeAdapter(long.class, longSerializer);
        gsonBuilder.serializeNulls();
        return gsonBuilder;
    }

    private void initAppTables() {
        try {
            // Create the Mobile Service Client instance, using the provided
            // Mobile Service URL
            // TODO this is targeting single webapi controller
            // each table should target different webapi controller

            mClient = new MobileServiceClient(Constants.SERVICE_URL, ToDoActivity.this)
                    .withFilter(new ProgressFilter());

            // When app is initializing, It needs token to continue. If not
            // possible to get token without UI flow, it should direct to login
            // screen so that
            // user can see some instructions.

            if (getLocalToken() != null) {
                MobileServiceUser user = new MobileServiceUser();
                user.setAuthenticationToken(getLocalToken());
                mClient.setCurrentUser(user);
            } else {

                navigateToLogOut();
                return;
            }

            // Get the Mobile Service Table instance to use
            mToDoTable = mClient.getTable(WorkItem.class);
            mToDoTable.TABLES_URL = "/api/";
            mTextNewToDo = (EditText)findViewById(R.id.textNewToDo);

            // Create an adapter to bind the items with the view
            mAdapter = new WorkItemAdapter(ToDoActivity.this, R.layout.row_list_to_do);
            ListView listViewToDo = (ListView)findViewById(R.id.listViewToDo);
            listViewToDo.setAdapter(mAdapter);

        } catch (MalformedURLException e) {
            createAndShowDialog(new Exception(
                    "There was an error creating the Mobile Service. Verify the URL"), "Error");
        }
    }

    private void navigateToLogOut() {
        // Show logout page
        // Go to logout page
        Intent intent = new Intent(ToDoActivity.this, LogOutActivity.class);
        startActivity(intent);

        // Close this activity
        finish();
    }

    private void getToken(final AuthenticationCallback callback) {

        // one of the acquireToken overloads
        mAuthContext.acquireToken(ToDoActivity.this, Constants.CLIENT_ID, Constants.RESOURCE_ID,
                Constants.REDIRECT_URL, Constants.USER_HINT, callback);
    }

    private String getLocalToken() {
        return mToken;
    }

    private void setLocalToken(String newToken) {
        mToken = newToken;
    }

    /**
     * Mark an item as completed
     * 
     * @param item The item to mark
     */
    public void checkItem(WorkItem item) {
        if (mClient == null) {
            return;
        }

        // Set the item as completed and update it in the table
        item.setComplete(true);
        final WorkItem itemRemove = item;
        mToDoTable.update(item, new TableOperationCallback<WorkItem>() {

            public void onCompleted(WorkItem entity, Exception exception,
                    ServiceFilterResponse response) {
                if (exception == null) {
                    // We dont need to wait for entity in update operation
                    mAdapter.remove(itemRemove);
                } else {
                    createAndShowDialog(exception, "Error");
                }
            }

        });
    }

    public void updateDate(WorkItem item) {
        if (mClient == null) {
            return;
        }

        // Set the item as completed and update it in the table
        mToDoTable.update(item, new TableOperationCallback<WorkItem>() {
            public void onCompleted(WorkItem entity, Exception exception,
                    ServiceFilterResponse response) {
                if (exception != null) {
                    createAndShowDialog(exception, "Error");
                }
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(Menu.NONE, MENU_LOGOUT, Menu.NONE, "LogOut");
        menu.add(Menu.NONE, MENU_CLEAR_TOKEN, Menu.NONE, "Clear Token");
        menu.add(Menu.NONE, MENU_GET_TOKEN, Menu.NONE, "Get Token");
        menu.add(Menu.NONE, MENU_GET_NEWSFEED, Menu.NONE, "Other Activity");
        menu.add(Menu.NONE, MENU_GET_SETTINGS, Menu.NONE, "Test Settings");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_LOGOUT: {
                resetTokens();

                // Clear browser cookies
                CookieSyncManager.createInstance(ToDoActivity.this);
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.removeAllCookie();

                // Go to logout page
                Intent intent = new Intent(ToDoActivity.this, LogOutActivity.class);
                startActivity(intent);
                // dont finish since when it comes back this page can be used
                // again
                // if not discarded
                return true;
            }
            case MENU_CLEAR_TOKEN: {
                resetTokens();
                return true;
            }
            case MENU_GET_TOKEN:
                getToken(new AuthenticationCallback<AuthenticationResult>() {

                    @Override
                    public void onError(Exception exc) {
                        Toast.makeText(getApplicationContext(), exc.getMessage(), Toast.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onSuccess(AuthenticationResult result) {
                        Toast.makeText(getApplicationContext(), "OnCompleted", Toast.LENGTH_LONG)
                                .show();
                        setLocalToken(result.getAccessToken());
                    }
                });
                return true;
            case MENU_GET_NEWSFEED: {
                Intent intent = new Intent(ToDoActivity.this, FeedActivity.class);
                startActivity(intent);
                return true;
            }
            case MENU_GET_SETTINGS: {
                Intent intent = new Intent(ToDoActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume(); // Always call the superclass method first

        // User can click logout, it will come back here
        // It should refresh list again
        refreshInProgress = false;
        sendRequest();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), TAG + "LifeCycle: OnDestroy", Toast.LENGTH_SHORT)
                .show();

    }

    private void resetTokens() {
        // Clear auth context tokens and local field as well
        mAuthContext.getCache().removeAll();
        mToken = null;

        // clear token from current user obj
        mClient.getCurrentUser().setAuthenticationToken(null);
    }

    /*
     * Target SDK above 11 for fragments
     */
    public void showDatePicker(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }

    /*
     * Datepicker to set duedate
     */
    public static class DatePickerFragment extends DialogFragment implements
            DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();

            // If user does not change the date, add button will use this date
            pickerYear = c.get(Calendar.YEAR);
            pickerMonth = c.get(Calendar.MONTH);
            pickerDayOfMonth = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, pickerYear, pickerMonth,
                    pickerDayOfMonth);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {

            // Update current selected date to be used in add post
            // there is only one item posting
            pickerYear = year;
            pickerMonth = month;
            pickerDayOfMonth = day;
        }
    }

    /**
     * Add a new item
     * 
     * @param view The view that originated the call
     */
    public void addItem(View view) {
        if (mClient == null) {
            initAppTables();
        }

        if (getLocalToken() != null) {
            MobileServiceUser user = new MobileServiceUser();
            user.setAuthenticationToken(getLocalToken());
            mClient.setCurrentUser(user);
        }

        // Create a new item
        WorkItem item = new WorkItem();

        // Get title from edit text field
        item.setTitle(mTextNewToDo.getText().toString());
        // Checkbox set to false
        item.setComplete(false);

        Calendar calendar = Calendar.getInstance();
        calendar.set(pickerYear, pickerMonth, pickerDayOfMonth);

        item.setDueDate(calendar.getTime());

        // Insert the new item
        mToDoTable.insert(item, new TableOperationCallback<WorkItem>() {

            public void onCompleted(WorkItem entity, Exception exception,
                    ServiceFilterResponse response) {

                if (exception == null) {
                    if (!entity.isComplete()) {
                        mAdapter.add(entity);

                    }
                } else {
                    createAndShowDialog(exception, "Error");
                }

            }
        });

        mTextNewToDo.setText("");
    }

    public void refreshItem(View view) {
        if (mClient == null) {
            return;
        }

        // Load the items from the Mobile Service
        refreshItemsFromTable();
    }

    /**
     * Refresh the list with the items in the Mobile Service Table
     */
    private void refreshItemsFromTable() {

        // Get the items that weren't marked as completed and add them in the
        // adapter
        if (getLocalToken() != null) {
            MobileServiceUser user = new MobileServiceUser();
            user.setAuthenticationToken(getLocalToken());
            mClient.setCurrentUser(user);
        }

        if (mToDoTable != null) {
            mToDoTable.where().field("Complete").eq(MobileServiceQueryOperations.val(false))
                    .execute(new TableQueryCallback<WorkItem>() {

                        public void onCompleted(List<WorkItem> result, int count,
                                Exception exception, ServiceFilterResponse response) {
                            if (exception == null) {
                                mAdapter.clear();

                                for (WorkItem item : result) {
                                    mAdapter.add(item);
                                }

                            } else {
                                createAndShowDialog(exception, "Error");
                            }
                        }
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mAuthContext.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Creates a dialog and shows it
     * 
     * @param exception The exception to show in the dialog
     * @param title The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        createAndShowDialog(exception.toString(), title);
    }

    /**
     * Creates a dialog and shows it
     * 
     * @param message The dialog message
     * @param title The dialog title
     */
    private void createAndShowDialog(String message, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    private class ProgressFilter implements ServiceFilter {

        @Override
        public void handleRequest(ServiceFilterRequest request,
                NextServiceFilterCallback nextServiceFilterCallback,
                final ServiceFilterResponseCallback responseCallback) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mProgressBar != null)
                        mProgressBar.setVisibility(ProgressBar.VISIBLE);
                }
            });

            nextServiceFilterCallback.onNext(request, new ServiceFilterResponseCallback() {

                @Override
                public void onResponse(ServiceFilterResponse response, Exception exception) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (mProgressBar != null)
                                mProgressBar.setVisibility(ProgressBar.GONE);
                        }
                    });

                    if (responseCallback != null)
                        responseCallback.onResponse(response, exception);
                }
            });
        }
    }

    @Override
    @Deprecated
    public Object onRetainNonConfigurationInstance() {

        return mAuthContext;
    }

}
