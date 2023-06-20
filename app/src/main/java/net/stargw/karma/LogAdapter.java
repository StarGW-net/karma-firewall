package net.stargw.karma;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by swatts on 17/11/15.
 */
public class LogAdapter extends ArrayAdapter<String> implements Filterable {

    private ArrayList<String> appsSorted; // for filtering
    private ArrayList<String> appsOriginal; // for filtering

    public LogAdapter(Context context, ArrayList<String> apps) {
        super(context, 0, apps);

    }

    // Take a copy of the list when it changes for filtering later
    public void updateFull()
    {
        // setNotifyOnChange(false);
        this.appsOriginal = new ArrayList<String>();
        for(int i = 0, l = getCount(); i < l; i++) {
            String app = getItem(i);
            this.appsOriginal.add(app);
        }
        // super.notifyDataSetChanged();
    }

    public String toText()
    {
        StringBuilder sb = new StringBuilder();

        for(int i = 0, l = getCount(); i < l; i++) {
            sb.append(getItem(i) + "\n");
        }

        return sb.toString();
    }

    public void remove(int pos)
    {
        remove(pos);
        notifyDataSetChanged();
    }
    /*
    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }
    */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        String apps = getItem(position);


        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_logs_row, parent, false);
        }

        TextView text1 = (TextView) convertView.findViewById(R.id.activity_logs_row_text);
        TextView date1 = (TextView) convertView.findViewById(R.id.activity_logs_row_date);

        // Try to figure out what sort of log it it
        int logType = 0;

        if (apps.contains(" @SW@ "))
        {
            logType = 1;
        } else {
            if (Character.isDigit(apps.charAt(0))) {
                logType = 2;
            }
        }


        String d = "";
        String t = "";
        switch (logType) {
            case 1:
                int pos = apps.indexOf("@SW@ ");
                d = apps.substring(0, pos);
                t = apps.substring(pos+5);

                date1.setText(d);

                text1.setMovementMethod(LinkMovementMethod.getInstance());

                text1.setText(Html.fromHtml(t));

                // text1.setText(t);
                break;

            case 2: // This is an ADB log
                d = apps.substring(0, 14);
                t = apps.substring(19);

                date1.setText(d);
                text1.setText(t);
                break;

            default:
                d = apps;
                date1.setText("");
                date1.setVisibility(View.GONE);
                text1.setText(d);
                break;
        }

                // Return the completed view to render on screen
        return convertView;

    }

    @Override
    public Filter getFilter() {

        Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                appsSorted = (ArrayList<String>)results.values;
                notifyDataSetChanged();
                clear();
                for(int i = 0, l = appsSorted.size(); i < l; i++)
                    add(appsSorted.get(i));
                notifyDataSetInvalidated();

                notifyDataSetChanged();

            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults results = new FilterResults();

                constraint = constraint.toString().toLowerCase();
                FilterResults result = new FilterResults();
                if(constraint != null && constraint.toString().length() > 0)
                {
                    ArrayList<String> filteredItems = new ArrayList<String>();

                    for(int i = 0, l = appsOriginal.size(); i < l; i++)
                    {
                        String app = appsOriginal.get(i);
                        // Logs.myLog("Filter match: " + constraint + " v " + app , 3);
                        if( (app.toLowerCase().contains(constraint) ) ) {
                        // ) || (app.packageName.toString().toLowerCase().contains(constraint)) ) {
                            filteredItems.add(app);
                            // Logs.myLog("Add match: " + app, 3);
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
