package net.stargw.karma;

/**
 * Created by swatts on 16/06/16.
 */

//
// This provides a link between the adpator and the activity class that uses it.
// When an icon is clicked we need to let the activity know which item was selected
// It is passsed back by using this interface
//
public interface ActivityMainListener
{
    public void changeSelectedItem(int pos);

    // public void taskAppListBuildFinished();
}
