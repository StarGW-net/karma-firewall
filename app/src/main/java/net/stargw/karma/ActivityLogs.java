package net.stargw.karma;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by swatts on 14/06/16.
 */
public class ActivityLogs extends Activity {

    LogAdapter adapter;
    ListView lv;

    private BroadcastListener mReceiver;

    private boolean viewBlocked = true;
    boolean debugLog = false;
    Context myContext;


    private class BroadcastListener extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            // Logs.myLog("App Received intent", 2);
            if (Global.SCREEN_REFRESH_INTENT.equals(intent.getAction())) {
                Logs.myLog("ActivityLogs Received intent to update screen", 2);
                if (adapter != null) {
                    refreshLogs();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_logs);

        myContext = this;

        Global.getSettings();
        Logs.getLoggingLevel();

        Logs.myLog("ActivityLogs Activity started" , 2);

        lv = (ListView) findViewById(R.id.activity_logs_listview);
        refreshLogs();

        ImageView btn = (ImageView) findViewById(R.id.activity_logs_menu_icon);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showOptionsMenu(v);
            }
        });



        EditText myFilter = (EditText) findViewById(R.id.activity_logs_filter_text);
        myFilter.setVisibility(View.GONE);

        ImageView mySearch = (ImageView) findViewById(R.id.activity_logs_search_icon);

        mySearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //v.getId() will give you the image id

                EditText myFilter = (EditText) findViewById(R.id.activity_logs_filter_text);

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
                            adapter.getFilter().filter(s.toString());
                        }
                    });
                } else {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(myFilter.getWindowToken(), 0);

                    myFilter.setVisibility(View.GONE);
                    myFilter.setText("");
                }

            }
        });

        /*
        TextView title = (TextView) findViewById(R.id.dialogAppsTitle2);

        //
        // Show debug logs on long press
        //

        title.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Do something here
                if (!debugLog)
                {
                    debugLog = true;
                } else {
                    debugLog = false;
                }
                refreshLogs();
                return false;
            }
        });
        */

    }



    @Override
    public void onBackPressed()
    {
        EditText myFilter = (EditText) findViewById(R.id.activity_logs_filter_text);

        // override back if we are showing the search filter. Dismiss this first
        if (myFilter.getVisibility() == View.VISIBLE) {
            myFilter.setVisibility(View.GONE);
            myFilter.setText("");
        } else {
            super.onBackPressed();
        }
    }

    void refreshLogs()
    {
        // adapter.clear();

        if (Global.settingsLoggingLevel < 5)
        {
            ArrayList<String> logBuffer = Logs.getLogBufferList();

            /*
            for (int i = logBuffer.size() -1; i >= 0; i--)
            {
                String logLine = logBuffer.get(i);
                if (Build.VERSION.SDK_INT > 28)
                {
                    if (!viewBlocked && !(logLine.contains(getString(R.string.allowed)))) {
                        logBuffer.remove(i);
                    }
                    if (viewBlocked && !(logLine.contains(getString(R.string.blocked)))) {
                        logBuffer.remove(i);
                    }
                }
            }
            */

            adapter = new LogAdapter(this, logBuffer);

            // adapter = new LogAdapter(myContext, Logs.getLogBufferList());
            adapter.updateFull();
            // adapter.getFilter().filter("deny: ");
        } else {
            // Show adb logcat...
            try {
                Process process = Runtime.getRuntime().exec("logcat -v time -d");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                // StringBuilder log=new StringBuilder();
                String line = "";
                ArrayList<String> theLog = new ArrayList<String>();
                while ((line = bufferedReader.readLine()) != null) {
                    theLog.add(line);
                }
                adapter = new LogAdapter(myContext, theLog);
                adapter.updateFull();

            } catch (IOException e) {
                Logs.myLog("Error getting ADB log" + e.toString(),2);
                Global.settingsLoggingLevel = 2;
                refreshLogs();
            }
        }


        // adapter.notifyDataSetChanged();
        lv.setAdapter(adapter);
        lv.setSelection(lv.getAdapter().getCount() - 1);


    }

    public void showOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_logs);
        // getMenuInflater().inflate(R.menu.menu_main, menu);

        final Menu m = popup.getMenu();
        MenuItem item;
        // m.removeItem(m.findItem(R.id.add_queue));

        // Just don't show on any version
        if (Build.VERSION.SDK_INT > 28)
        {
            item = m.findItem(R.id.action_view);
            item.setVisible(false);
        } else {
            item = m.findItem(R.id.action_view);
            item.setVisible(false);
        }

        item = m.findItem(R.id.action_view);
        if (viewBlocked)
        {
            item.setTitle(R.string.activity_logs_menu_view_allowed);
        } else {
            item.setTitle(R.string.activity_logs_menu_view_blocked);
        }

        switch (Global.settingsLoggingLevel) {
            case 0:
                item = m.findItem(R.id.action_none);
                item.setChecked(true);
                break;
            case 1:
                item = m.findItem(R.id.action_normal);
                item.setChecked(true);
                break;
            case 2:
                item = m.findItem(R.id.action_detailed);
                item.setChecked(true);
                break;
            case 3:
                item = m.findItem(R.id.action_debug);
                item.setChecked(true);
                break;
            case 4:
                item = m.findItem(R.id.action_insane);
                item.setChecked(true);
                break;
            case 5:
                item = m.findItem(R.id.action_adb);
                item.setChecked(true);
                break;
            default:
                break;
        }


        // item.setChecked(Global.settingsDetailedLogs);

         // popup.getMenu().findItem(R.id.menu_name).setTitle(text);
        invalidateOptionsMenu();


        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {


            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_refresh2:
                        refreshLogs();
                        return true;
                    case R.id.action_share:
                        // Logs.emailLog();
                        if (Global.settingsLoggingLevel == 5)
                        {
                            Logs.shareLogADB();
                        } else {
                            Logs.shareLog();
                        }
                        return true;
                    case R.id.action_clear:
                        Logs.clearLog();
                        refreshLogs();
                        return true;
                    case R.id.action_none:
                        // change to int
                        // set
                        Global.settingsLoggingLevel = 0;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    case R.id.action_normal:
                        // change to int
                        // set
                        Global.settingsLoggingLevel = 1;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    case R.id.action_detailed:
                        // change to int
                        // set
                        Global.settingsLoggingLevel = 2;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    case R.id.action_debug:
                        // change to int
                        // set
                        Global.settingsLoggingLevel = 3;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    case R.id.action_insane:
                        // change to int
                        // set
                        Global.settingsLoggingLevel = 4;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    case R.id.action_adb:
                        // change to int
                        // set
                        Global.settingsLoggingLevel = 5;
                        Global.saveSetings();
                        logWarn();
                        refreshLogs();
                        return true;
                    case R.id.action_view:
                        if (viewBlocked)
                        {
                            viewBlocked = false;
                        } else {
                            viewBlocked = true;
                        }
                        refreshLogs();
                        return true;
                    case R.id.action_email:
                        // copy to clipboard??
                        return true;
                    default:
                        return false;
                }
            }

        });
        popup.show();
    }


    private void logWarn()
    {

        // String s = myContext.getString(R.string.activity_logs_menu_log_level);
        Global.infoMessage(myContext,
                myContext.getString(R.string.activity_logs_menu_log_level),
                        myContext.getString(R.string.log_level_message1));
    }

    /*
    //
    // change the log level
    //
    protected void logLevelPickerDropdown()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(myContext);

        final String options[] = myContext.getResources().getStringArray(R.array.logLevels);

        builder.setTitle(myContext.getString(R.string.log_level_set));

        int selected = Logs.getLoggingLevel();

        builder.setSingleChoiceItems(options, selected, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                dialog.cancel();
                if (( item ) != Logs.getLoggingLevel())
                {
                    Logs.setLoggingLevel(item);
                    warn(item, options[item]);
                }
            }});

        builder.show();
    }
    */

    /*
    private void warn(int level, String levelText)
    {
        String message = myContext.getString(R.string.log_level_message1);
        if (level > 1)
        {
            message = message + "\n\n" + String.format(myContext.getString(R.string.log_level_message2), levelText);
        }

        Global.infoMessage(myContext,myContext.getString(R.string.dialog_warning),message);
    }
    */

    @Override
    protected void onResume() {
        super.onResume();

        // Runs TWICE on startup. WHY? WHY? WHY?
        Logs.myLog("ActivityLogs Activity Resumed", 2);

        mReceiver = new BroadcastListener();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Global.SCREEN_REFRESH_INTENT);
        registerReceiver(mReceiver, mIntentFilter);

        viewBlocked = true;
        debugLog = false;

        refreshLogs();

    }
}
