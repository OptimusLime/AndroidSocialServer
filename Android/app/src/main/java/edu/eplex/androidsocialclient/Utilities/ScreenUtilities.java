package edu.eplex.androidsocialclient.Utilities;

import android.content.Context;
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

    public static int dipsToPixels(Context ctx, float dips) {
        final float scale = ctx.getResources().getDisplayMetrics().density;
        int px = (int) (dips * scale + 0.5f);
        return px;
    }

}
