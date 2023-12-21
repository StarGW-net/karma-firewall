package net.stargw.karma;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;

public class AppInfoAdapter extends ArrayAdapter<AppInfo> implements Filterable {

    ActivityMainListener listener;

    private ArrayList<AppInfo> appsSorted; // for filtering
    private ArrayList<AppInfo> appsOriginal; // for filtering

    Context mContext;

    public AppInfoAdapter(Context context, ArrayList<AppInfo> apps) {
        super(context, 0, apps);

        mContext = context;
        updateFull();

    }


    // Take a copy of the list when it changes for filtering later
    public void updateFull()
    {
        setNotifyOnChange(false);
        this.appsOriginal = new ArrayList<AppInfo>();

        for(int i = 0, l = getCount(); i < l; i++) {
            AppInfo app = getItem(i);
            this.appsOriginal.add(app);
        }

        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        AppInfo apps = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_main_row2, parent, false);
        }

        // Lookup view for data population
        TextView text1 = (TextView) convertView.findViewById(R.id.activity_main_row_app_name);
        TextView text2 = (TextView) convertView.findViewById(R.id.activity_main_row_app_traffic);

        // We don't use text2 line any more
        text2.setText("");
        text2.setTypeface(null, Typeface.ITALIC);
        text2.setTextColor(Color.WHITE);

        ImageView icon = (ImageView) convertView.findViewById(R.id.activity_main_row_app_icon);
        ImageView tog = (ImageView) convertView.findViewById(R.id.activity_main_row_fwstate_icon);

        // Populate the data into the template view using the data object
        text1.setText(apps.name);

        if (apps.enabled == false)
        {
            text1.setPaintFlags(text1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            text1.setPaintFlags( text1.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
        }

        if (apps.icon != null)
        {
            icon.setImageDrawable(apps.icon);
        }

        tog.setVisibility(View.VISIBLE);

        if (apps.fw >= 30)
        {
            tog.setImageDrawable(getContext().getResources().getDrawable(R.drawable.fw_app_off));
        } else {
            tog.setImageDrawable(getContext().getResources().getDrawable(R.drawable.fw_app_on));
        }

        final int pos = position;
        tog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action(pos);
            }
        });

        text1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action(pos);
            }
        });

        final View vp = convertView;
        final LinearLayout expand = (LinearLayout) convertView.findViewById(R.id.activity_main_row_app_expand);

        if (apps.expandView == true)
        {
            expand.setVisibility(View.VISIBLE);
            expandApp(vp, pos);
        } else {
            expand.setVisibility(View.GONE);
        }

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppInfo app = getItem(pos);
                if (app.expandView == true)
                {
                    expand.setVisibility(View.GONE);
                    app.expandView = false;
                } else {
                    expand.setVisibility(View.VISIBLE);
                    app.expandView = true;
                }
                expandApp(vp, pos);
            }
        });


        // Return the completed view to render on screen
        return convertView;

    }

    public void action(int pos)
    {
        listener.changeSelectedItem(pos);
    }

    private void expandApp(View v, int position) {

        AppInfo app = getItem(position);

        LinearLayout expand = (LinearLayout) v.findViewById(R.id.activity_main_row_app_expand_text);
        expand.removeAllViews();

        if (app.appInfoExtra.size() == 1)
        {
            TextView text2 = new TextView(Global.getContext());
            text2.setTextColor(Color.WHITE);
            text2.setTypeface(null, Typeface.ITALIC);
            text2.setEllipsize(TextUtils.TruncateAt.END);
            // text1.setTextSize();
            text2.setSingleLine(true);
            text2.setMaxLines(1);

            text2.setText("(" + app.appInfoExtra.get(app.appInfoExtra.keySet().toArray()[0]).packageFQDN + ")");

            if (app.appInfoExtra.get(app.appInfoExtra.keySet().toArray()[0]).packageEnabled == false)
            {
                text2.setPaintFlags(text2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                text2.setPaintFlags(text2.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
            }
            text2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Logs.myLog("Clicked for detail: " + app.appInfoExtra.get(app.appInfoExtra.keySet().toArray()[0]).packageFQDN,1);
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + app.appInfoExtra.get(app.appInfoExtra.keySet().toArray()[0]).packageFQDN));
                    getContext().startActivity(intent);
                }
            });
            text2.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Karma", app.appInfoExtra.keySet().toArray()[0].toString());
                    clipboard.setPrimaryClip(clip);
                    return true;
                }
            });
            expand.addView(text2);
            TextView text4 = (TextView) v.findViewById(R.id.activity_main_row_app_uid);
            text4.setText("UID: " + app.UID2);
        } else {
            Iterator<String> it = app.appInfoExtra.keySet().iterator();

            while (it.hasNext())
            {
                String key = it.next();

                TextView text1 = new TextView(Global.getContext());
                text1.setTextColor(Color.WHITE);
                text1.setEllipsize(TextUtils.TruncateAt.END);
                // text1.setTextSize();
                text1.setSingleLine(true);
                text1.setMaxLines(1);

                text1.setText(app.appInfoExtra.get(key).packageName);
                expand.addView(text1);

                TextView text2 = new TextView(Global.getContext());
                text2.setTextColor(Color.WHITE);
                text2.setTypeface(null, Typeface.ITALIC);
                text2.setEllipsize(TextUtils.TruncateAt.END);
                // text1.setTextSize();
                text2.setSingleLine(true);
                text2.setMaxLines(1);

                text2.setText("(" + app.appInfoExtra.get(key).packageFQDN + ")");

                if (app.appInfoExtra.get(key).packageEnabled == false)
                {
                    text1.setPaintFlags(text1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    text2.setPaintFlags(text2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    text1.setPaintFlags(text1.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                    text2.setPaintFlags(text2.getPaintFlags() & (~ Paint.STRIKE_THRU_TEXT_FLAG));
                }
                final String f = key;
                text2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + app.appInfoExtra.get(f).packageFQDN));
                        getContext().startActivity(intent);
                    }
                });
                text2.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Karma", app.appInfoExtra.get(f).packageFQDN);
                        clipboard.setPrimaryClip(clip);
                        return true;
                    }
                });
                expand.addView(text2);
            }
            TextView text4 = (TextView) v.findViewById(R.id.activity_main_row_app_uid);
            text4.setText("");
        }



    }

    public void clearExpanded()
    {
        if (appsOriginal != null) {
            for (int i = 0, l = appsOriginal.size(); i < l; i++)
                appsOriginal.get(i).expandView = false;
        }
    }

    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                appsSorted = (ArrayList<AppInfo>)results.values;
                notifyDataSetChanged();
                clear();
                for(int i = 0, l = appsSorted.size(); i < l; i++)
                    add(appsSorted.get(i));
                notifyDataSetInvalidated();

                notifyDataSetChanged();

            }



            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                constraint = constraint.toString().toLowerCase();
                FilterResults result = new FilterResults();

                if(constraint != null && constraint.toString().length() > 0)
                {
                    ArrayList<AppInfo> filteredItems = new ArrayList<AppInfo>();

                    for(int i = 0, l = appsOriginal.size(); i < l; i++)
                    {
                        AppInfo app = appsOriginal.get(i);
                        // Logs.myLog("Filter match: " + constraint + " v " + app.packageName , 3);

                        app.expandView = false;

                        if( (app.name.toLowerCase().contains(constraint) ) ) {
                            filteredItems.add(app);
                        }
                        // Loop over package names...
                        Iterator<String> it = app.appInfoExtra.keySet().iterator();

                        while (it.hasNext())
                        {
                            String key = it.next();
                            if( (app.appInfoExtra.get(key).packageFQDN.toLowerCase().contains(constraint) ) ) {
                                // Logs.myLog("Filter match: " + constraint + " v " + app.appInfoExtra.get(key).packageFQDN.toLowerCase() , 3);

                                boolean newApp = true;
                                for(int i4 = 0; i4 < filteredItems.size() ; i4++)
                                {
                                    if (filteredItems.get(i4).UID2 == app.UID2)
                                    {
                                        newApp = false;
                                        break;
                                    }
                                }
                                if (newApp == true) {
                                    app.expandView = true;
                                    Logs.myLog("Filter Expand: " + app.name , 3);
                                    filteredItems.add(app);
                                }
                            }
                        }
                    }
                    result.count = filteredItems.size();
                    result.values = filteredItems;
                }
                else
                {
                    synchronized(this)
                    {
                        result.values = appsOriginal;
                        result.count = appsOriginal.size();
                    }
                }
                return result;
            }

        };

        return filter;
    }
}
