package net.stargw.karma;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import static net.stargw.karma.Global.APPLIST_DOING;
import static net.stargw.karma.Global.getContext;

public class Widget2Configure extends Activity implements ActivityMainListener {

    private AppInfoAdapterWidget appListWidgetAdapter;
    private ArrayList<AppInfo> appListWidget;
    private ListView appListWidgetListView;
    private Context myContext;

    private int appWidgetId;

    private Dialog appLoadWidget = null;

    private BroadcastListener mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myContext = this;

        Global.getSettings();
        Logs.getLoggingLevel(); // gets level + housekeeping

        Logs.myLog("Widget2Configure - onCreate()", 2);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }


        setContentView(R.layout.widget2_configure);

        ImageView btn = (ImageView) findViewById(R.id.activity_main_menu_icon);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOptionsMenu(v);
            }
        });

        // When app starts show user apps
        Global.settingsEnableExpert = false;

        EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);
        myFilter.setVisibility(View.GONE);

        ImageView mySearch = (ImageView) findViewById(R.id.activity_main_filter_icon);

        mySearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //v.getId() will give you the image id

                EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);

                if (myFilter.getVisibility() == View.GONE) {

                    myFilter.setVisibility(View.VISIBLE);
                    myFilter.setFocusableInTouchMode(true);
                    myFilter.requestFocus();

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    // imm.showSoftInput(myFilter, InputMethodManager.SHOW_IMPLICIT);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

                    myFilter.addTextChangedListener(new TextWatcher() {

                        public void afterTextChanged(Editable s) {
                        }

                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                            // Logs.myLog("Filter on text: " + s , 3);
                            appListWidgetAdapter.getFilter().filter(s.toString()); // CRASH REPORTED HERE
                        }
                    });
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(myFilter.getWindowToken(), 0);

                    myFilter.setVisibility(View.GONE);
                    myFilter.setText("");
                    // adapter.getFilter().filter(null);
                }

            }
        });


    }

    @Override
    protected void onPause() {
        Logs.myLog("Widget2Configure - onPause()", 2);

        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onPause();
    }

    public void updateAppListWidgetListView()
    {
        Logs.myLog("Widget2Configure - updateAppListWidgetListView()", 2);

        // rebuild the app list
        Iterator<Integer> it = Global.appListFW.keySet().iterator();
        appListWidget = new ArrayList<AppInfo>();

        while (it.hasNext())
        {
            int key = it.next();
            AppInfo thisApp = Global.appListFW.get(key);
            if (thisApp.system == Global.settingsEnableExpert) {
                appListWidget.add(thisApp);
                // Log.w("FWWidget2", "Add GUI app: " + thisApp.name);
            }
        }


        mySort0(appListWidget);

        appListWidgetAdapter = new AppInfoAdapterWidget(myContext, appListWidget);
        appListWidgetAdapter.updateFull();

        appListWidgetListView = (ListView) findViewById(R.id.listViewApps);
        appListWidgetListView.setAdapter(appListWidgetAdapter);

    }


    // Sort all alphabetically
    void mySort0( ArrayList<AppInfo> appInfo2)
    {
        Collections.sort(appInfo2, new Comparator<AppInfo>() {
            public int compare(AppInfo appInfoA, AppInfo appInfoB) {
                return appInfoA.name.compareToIgnoreCase(appInfoB.name);
            }
        });
    }

    public void changeSelectedItem(int pos)
    {
        AppInfo thisApp = appListWidgetAdapter.getItem(pos);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(myContext);

        RemoteViews views = new RemoteViews(myContext.getPackageName(),
                R.layout.widget2_layout);
        appWidgetManager.updateAppWidget(appWidgetId, views);

/*

    The requestCode used when creating a pendingIntent is not
    intended to pass on to the receiver, it is intended as a way
    for the app creating the pendingIntent to be able to manage
    multiple pendingIntents.

    Suppose an alarm app needed to create several pendingIntents,
    and later needs to cancel or modify one of them. The requestCode
    is used to identify which one to cancel/modify.

    To pass data on, use the putExtra as described above. Note you
    might very well want to use RowId for both the requestCode and
    the Extra data.

 */

        Intent i = new Intent(myContext, Widget2Provider.class);
        i.setAction(Global.TOGGLEAPP);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        i.putExtra("APP", thisApp.UID2);
        i.putExtra("VIEW", R.layout.widget2_layout);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(myContext, 0, i, 0);

        // Log.w("FWWidget2 Config","App Widget ID = " + appWidgetId + " - UID = " + thisApp.UID2);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());

        p.edit().putInt("W-" + appWidgetId, thisApp.UID2).apply();

        views.setImageViewBitmap(R.id.widgit2_icon, Global.drawableToBitmap(thisApp.icon));

        if (thisApp.fw >= 30) {
            views.setViewVisibility(R.id.widgit2_allow, View.INVISIBLE);
            views.setViewVisibility(R.id.widgit2_deny, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widgit2_deny, View.INVISIBLE);
            views.setViewVisibility(R.id.widgit2_allow, View.VISIBLE);
        }

        if (Global.getFirewallState() == true) {
            views.setViewVisibility(R.id.widgit2_wall, View.VISIBLE);
            views.setViewVisibility(R.id.widgit2_disabled, View.INVISIBLE);
        } else {
            views.setViewVisibility(R.id.widgit2_disabled, View.VISIBLE);
            views.setViewVisibility(R.id.widgit2_wall, View.INVISIBLE);
        }


        // set the onclick again cos - onsuper and onupdate do not always work :-(
        views.setOnClickPendingIntent(R.id.widgit2_icon, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);

        appListWidgetAdapter.clearExpanded();

        finish();

    }


    void displayAppDialog()
    {
        // Display a loading dialog box until the app list is prepared by the service.
        appLoadWidget = new Dialog(myContext);

        appLoadWidget.setContentView(R.layout.dialog_progress);
        appLoadWidget.setTitle(R.string.LoadingApps);

        TextView text = (TextView) appLoadWidget.findViewById(R.id.infoMessage);
        text.setText(R.string.BuildingApps);

        text = (TextView) appLoadWidget.findViewById(R.id.infoMessage2);
        text.setText(R.string.InfoLoadingApps);

        appLoadWidget.show();
    }

    private class BroadcastListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            // Logs.myLog("Widget2Configure - onReceive()", 2);

            if (Global.REBUILD_APPS_IN_PROGRESS.equals(intent.getAction())) {
                if (appLoadWidget != null) {
                    ProgressBar progBar = (ProgressBar) appLoadWidget.findViewById(R.id.progBar);
                    int x1 = (int) (Global.packageCurrent /(float)Global.packageMax*100);
                    progBar.setProgress(x1);
                }
            }

            if (Global.REBUILD_APPS_DONE.equals(intent.getAction()))
            {
                Logs.myLog("Widget2Configure - Process Global.REBUILD_APPS_DONE", 2);
                if (Global.appListFW != null)
                {
                    // close the dialog box if we had one open
                    if (appLoadWidget != null) {
                        Logs.myLog("Close dialog", 3);
                        appLoadWidget.cancel();
                        appLoadWidget.dismiss();
                        appLoadWidget = null;
                    }

                    AppWidgetManager man = AppWidgetManager.getInstance(myContext);
                    int[] ids = man.getAppWidgetIds(
                            new ComponentName(myContext, Widget2Provider.class));
                    for (int i=0; i<ids.length; i++) {
                        int appWidgetId = ids[i];
                        Intent updateIntent = new Intent();
                        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        // updateIntent.putExtra(MyWidgetProvider.WIDGET_DATA_KEY, data);
                        myContext.sendBroadcast(updateIntent);
                    }

                    updateAppListWidgetListView();

                }
            }

        }
    };


    @Override
    protected void onResume() {
        super.onResume();

        Logs.myLog("Widget2Configure - onResume()", 2);

        // register receiver
        mReceiver = new Widget2Configure.BroadcastListener();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Global.FIREWALL_STATE_CHANGE);

        mIntentFilter.addAction(Global.REBUILD_APPS_DONE);
        mIntentFilter.addAction(Global.REBUILD_APPS_IN_PROGRESS);
        mIntentFilter.addAction(Global.TOGGLEAPP_REFRESH);

        registerReceiver(mReceiver, mIntentFilter);

        // If we have no list try and build one
        // Don't want widget refreshing really

        if (Global.appListState == APPLIST_DOING)
        {
            displayAppDialog();
        } else {
            if ( (Global.appListFW == null) || (Global.appListFW.isEmpty()) )
            {
                Logs.myLog("Widget2Configure - appListFW = null, so need to build", 2);
                displayAppDialog();
                Global.getAppListBackground();
            } else {
                // we have a list so display it
                updateAppListWidgetListView();
            }
        }


    }


    public void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_widget);
        // getMenuInflater().inflate(R.menu.menu_main, menu);

        EditText myFilter = (EditText) findViewById(R.id.activity_main_filter_text);
        if (myFilter.getVisibility() == View.VISIBLE) {
            myFilter.setVisibility(View.GONE);
            myFilter.setText("");
        }


        Menu m = popup.getMenu();
        MenuItem item = m.findItem(R.id.action_apps_system);


        if (Global.settingsEnableExpert == true) {
            item.setTitle(R.string.activity_main_menu_apps_user);
        } else {
            item.setTitle(R.string.activity_main_menu_apps_system);
        }


        // popup.getMenu().findItem(R.id.menu_name).setTitle(text);
        invalidateOptionsMenu();


        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {


            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_logs:
                        Intent intent = new Intent(myContext, ActivityLogs.class);
                        startActivity(intent);
                        return true;
                    case R.id.action_apps_system:
                        if (Global.settingsEnableExpert == true) {
                            Global.settingsEnableExpert = false;
                        } else {
                            Global.settingsEnableExpert = true;
                        }
                        Global.saveSetings();
                        updateAppListWidgetListView();
                        return true;

                    case R.id.action_refresh:
                        Global.getAppListBackground();
                        displayAppDialog();
                        return true;

                    default:
                        return false;
                }
            }

        });

        popup.show();

    }

}
