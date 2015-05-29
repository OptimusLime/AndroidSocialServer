package edu.eplex.androidsocialclient.MainUI.Main.Discovery;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.WindowManager;

import com.squareup.otto.Bus;

import java.util.HashMap;
import java.util.HashSet;

import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Discovery.Tabs.DiscoveryFragment;
import edu.eplex.androidsocialclient.MainUI.Main.Discovery.Tabs.ScratchFragment;
import edu.eplex.androidsocialclient.MainUI.Main.MainDiscoveryScreen;
import edu.eplex.androidsocialclient.R;

/**
 * Created by paul on 3/16/15.
 */
public class DiscoveryFlowManager {

    //all done, we call this code to know it was us -- i made this up randomly 4021 -- seemed awesome.
    public final static int DISCOVERY_SCREEN_REQUEST_CODE = 43211;

    static final String EXTRA_FILTER_WID = "filterWID";
    static final String EXTRA_INNER_FILTER_WID = "innerWID";

    //what tab are we??? How do the tabs behave for switching?
    public enum DiscoveryID
    {
        Popular,
        Favorites,
        Search,
        Recent,
        Scratch
    }

    HashSet<Object> registered = new HashSet<>();
    MainDiscoveryScreen mainDiscoveryActivity;
    Bus uiEventBus;

    private static DiscoveryFlowManager instance = null;
    protected DiscoveryFlowManager() {
        // Exists only to defeat instantiation.

        uiEventBus = new Bus();

    }

    public static DiscoveryFlowManager getInstance() {
        if(instance == null) {
            instance = new DiscoveryFlowManager();
        }
        return instance;
    }

    public void setMainDiscoveryActivity(MainDiscoveryScreen mainActivity)
    {
        mainDiscoveryActivity = mainActivity;
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
    public void finishDiscoveryActivity(FragmentActivity activity, FilterComposite finalFilter)
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
            Log.d("DISCFLOWMANAGER", "Activity closed");
//        activity.finishActivity(EDIT_SCREEN_REQUEST_CODE);
        }
        catch (Exception e)
        {
            Log.d("DISCFLOWMANAGER", "Failed to end discovery activity : " + e.getMessage());
        }
    }

    public void cancelDiscovery(FragmentActivity activity)
    {
        //I want to go back
        Bundle conData = new Bundle();
        conData.putString("discovery", "cancelled");
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
    public FilterComposite getFilterFromDiscoveryIntent(Intent intent)
    {
        FilterManager fm = FilterManager.getInstance();
        if(intent == null)
            return fm.getLastEditedFilter();
        else
        {
            //grab info from intent
            String filterID = intent.getStringExtra(EXTRA_FILTER_WID);
            FilterComposite fc =  fm.getFilter(filterID);
            if(fc == null)
                fc = fm.getTemporaryFilter(filterID);
            return fc;
        }
    }

    public void launchDiscoveryUI(FragmentActivity mContext, FilterComposite filter)
    {
        //need to open up IEC frag
        DiscoveryFragment discoveryFragment = new DiscoveryFragment();

        //add register fragment to the stack
        mContext.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, discoveryFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    public Intent createDiscoveryIntent(Context mContext, FilterComposite toPublish)
    {
        return createDiscoveryIntent(mContext, toPublish, null);
    }
    public Intent createDiscoveryIntent(Context mContext, FilterComposite toPublish, String innerFilterWID)
    {
        Intent i = new Intent();
        i.setClass(mContext, MainDiscoveryScreen.class);
        i.putExtra(EXTRA_FILTER_WID, toPublish.getUniqueID());
        if(innerFilterWID != null && !innerFilterWID.equals(""))
            i.putExtra(EXTRA_INNER_FILTER_WID, innerFilterWID);
        return i;
    }
    public DiscoveryID DefaultStartingTab()
    {
        return DiscoveryID.Popular;
    }

    /*
      * It doesn't matter the color of the icons, but they must have solid colors
      */
    public Drawable getTabIcons(int position) {

        DiscoveryID tab = DiscoveryID.values()[position];
        switch(tab) {
            case Popular:
                return mainDiscoveryActivity.getResources().getDrawable(R.drawable.ic_action_heart_black);
            case Favorites:
                return mainDiscoveryActivity.getResources().getDrawable(R.drawable.ic_action_star_10_black);
            case Search:
                return mainDiscoveryActivity.getResources().getDrawable(R.drawable.ic_action_search_black);
            case Recent:
                return mainDiscoveryActivity.getResources().getDrawable(R.drawable.ic_action_clock_black);
            case Scratch:
                return mainDiscoveryActivity.getResources().getDrawable(R.drawable.ic_action_tablet_black);
        }
        return null;
    }

    //handle the pager for our tabs -- we're the flow manager damnit- IT'S WHAT WE DO
    public static class ViewPagerAdapter extends FragmentStatePagerAdapter {

        HashMap<DiscoveryID, Fragment> savedFragments = new HashMap<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public HashMap<DiscoveryID, Fragment> GetFragmentMap()
        {
            return this.savedFragments;
        }

        public Fragment getItem(int num) {
            DiscoveryID tab = DiscoveryID.values()[num];

            if(savedFragments.containsKey(tab))
                return savedFragments.get(tab);

            Fragment f;
            switch(tab) {
                case Popular:
                case Favorites:
                case Search:
                case Recent:
                    DiscoveryFragment d = new DiscoveryFragment();
                    d.setDiscoverTabID(tab);
                    f = d;
                    break;
                case Scratch:
                default:
                    f = new ScratchFragment();
                    break;
            }

            savedFragments.put(tab, f);

            return f;
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {

            switch (position)
            {
                case 0:
                    return "" + position;
                case 1:
                    return "" + position;
                case 2:
                    return "" + position;
                case 3:
                    return "" + position;
                case 4:
                    return "" + position;
                default:
                    return "" + position;
            }
        }

    }
}
