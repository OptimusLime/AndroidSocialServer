package edu.eplex.androidsocialclient.MainUI.Main.Publish;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.otto.Bus;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import dagger.ObjectGraph;
import edu.eplex.AsyncEvolution.main.NEATInitializer;
import edu.eplex.androidsocialclient.GPU.GPUNetworkFilter;
import edu.eplex.androidsocialclient.MainUI.Cache.BitmapCacheManager;
import edu.eplex.androidsocialclient.MainUI.Filters.Evolution.FilterEvolutionInjectModule;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFilterIEC;
import edu.eplex.androidsocialclient.MainUI.Main.MainEditScreen;
import edu.eplex.androidsocialclient.MainUI.Main.MainPublishScreen;
import edu.eplex.androidsocialclient.R;
import eplex.win.FastNEATJava.utils.NeatParameters;

/**
 * Created by paul on 3/16/15.
 */
public class PublishFlowManager {

    //all done, we call this code to know it was us -- i made this up randomly 4021 -- seemed awesome.
    public final static int PUBLISH_SCREEN_REQUEST_CODE = 4021;

    static final String EXTRA_FILTER_WID = "filterWID";
    static final String EXTRA_INNER_FILTER_WID = "innerWID";

    //what tab are we??? How do the tabs behave for switching?
    public enum PublishID
    {
      Publish
    }

    HashSet<Object> registered = new HashSet<>();
    MainPublishScreen mainPublishActivity;
    Bus uiEventBus;

    private static PublishFlowManager instance = null;
    protected PublishFlowManager() {
        // Exists only to defeat instantiation.

        uiEventBus = new Bus();

    }

    public static PublishFlowManager getInstance() {
        if(instance == null) {
            instance = new PublishFlowManager();
        }
        return instance;
    }

    public void setMainPublishActivity(MainPublishScreen mainActivity)
    {
        mainPublishActivity = mainActivity;
    }


    //get access to the bus
    public Bus getUiEventBus()
    {
        return uiEventBus;
    }

    //register an object for all UI events
    public void registerUIEvents(Object object)
    {
        if(!registered.contains(object)) {
            registered.add(object);
            uiEventBus.register(object);
        }
    }

    //unregister from any future UI events
    public void unregisterUIEvents(Object object)
    {
        if(registered.contains(object)) {
            uiEventBus.unregister(object);
            registered.remove(object);
        }
    }
    public void finishPublishArtifactActivity(FragmentActivity activity, FilterComposite finalFilter)
    {
        //we've finished with our filter, we need to replace our old filter
        //lets stick it in the cloud!
        //I want to go back
        try {
            Bundle conData = new Bundle();
            conData.putString("filter", finalFilter.getUniqueID());
            Intent intent = new Intent();
            intent.putExtras(conData);
            hideKeyboard(activity);
            activity.setResult(FragmentActivity.RESULT_OK, intent);
            activity.finish();
            Log.d("PUBFLOWMANAGER", "Activity closed");
//        activity.finishActivity(EDIT_SCREEN_REQUEST_CODE);
        }
        catch (Exception e)
        {
            Log.d("PUBFLOWMANAGER", "Failed to end publish activity : " + e.getMessage());
        }
    }

    public void cancelPublishFilter(FragmentActivity activity)
    {
        //I want to go back
        Bundle conData = new Bundle();
        conData.putString("publish", "cancelled");
        Intent intent = new Intent();
        intent.putExtras(conData);
        hideKeyboard(activity);
        activity.setResult(FragmentActivity.RESULT_CANCELED, intent);
        activity.finish();
    }
    void hideKeyboard(FragmentActivity activity)
    {
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }
    //last saved object
    public FilterComposite getFilterFromPublishIntent(Intent intent)
    {
        FilterManager fm = FilterManager.getInstance();
        if(intent == null)
            return fm.getLastEditedFilter();
        else
        {
            //grab info from intent
            String filterID = intent.getStringExtra(EXTRA_FILTER_WID);
            return fm.getFilter(filterID);
        }
    }

    public void launchPublishUI(FragmentActivity mContext, FilterComposite filter)
    {
        //need to open up IEC frag
        PublishFragment publishFragment = new PublishFragment();

        //add register fragment to the stack
        mContext.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, publishFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    public Intent createPublishIntent(Context mContext, FilterComposite toPublish)
    {
        return createPublishIntent(mContext, toPublish, null);
    }
    public Intent createPublishIntent(Context mContext, FilterComposite toPublish, String innerFilterWID)
    {
        Intent i = new Intent();
        i.setClass(mContext, MainPublishScreen.class);
        i.putExtra(EXTRA_FILTER_WID, toPublish.getUniqueID());
        if(innerFilterWID != null && !innerFilterWID.equals(""))
            i.putExtra(EXTRA_INNER_FILTER_WID, innerFilterWID);
        return i;
    }
}
