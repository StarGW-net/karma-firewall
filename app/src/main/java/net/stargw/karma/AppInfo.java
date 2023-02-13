package net.stargw.karma;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;

/**
 * Created by swatts on 17/11/15.
 */
public class AppInfo implements Cloneable {



    public AppInfo() {
        // TODO Auto-generated constructor stub
    }

    public String name;
    // public String packageName;
    public Drawable icon;

    public ArrayList<String> packageNames = null;
    public ArrayList<String> appNames = null;

    public int UID2; // No UID 1
    // public boolean launcher;


    public long bytesIn = 0;
    public boolean bytesLocal = false;

    public long traffic = 0;

    /*
    public long bytesFWIn = 0;
    public long bytesFWOut = 0;
    */

    public long date = 0;

    public boolean fw =false;
    public boolean system;
    public boolean internet = false;
    public boolean enabled = true;

    public boolean expandView = false;
    // Where do I put the method to build this?

    public boolean flush = false;

    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            return null;
        }
    }

}
