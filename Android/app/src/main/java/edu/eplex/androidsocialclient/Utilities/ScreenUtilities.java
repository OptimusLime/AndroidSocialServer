package edu.eplex.androidsocialclient.Utilities;

import android.graphics.Point;
import android.support.v4.app.FragmentActivity;
import android.view.Display;

/**
 * Created by paul on 5/18/15.
 */
public class ScreenUtilities {

    public static Point ScreenSize(FragmentActivity activity)
    {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

}
