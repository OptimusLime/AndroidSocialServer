package edu.eplex.androidsocialclient.MainUI.Main.Tabs;


import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.squareup.otto.Bus;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import edu.eplex.androidsocialclient.MainUI.Main.MainScreen;
import edu.eplex.androidsocialclient.MainUI.TakePictureFragment;
import edu.eplex.androidsocialclient.MainUI.UserSettingsFragment;
import edu.eplex.androidsocialclient.R;

/**
 * Created by paul on 3/15/15.
 * Singleton that handles all of the tabs -- basically master of the UI fragments
 */
public class TabFlowManager {

    public static final String HASHTAG_PATTERN = "(#[\\p{L}0-9-_]+)";

    //what tab are we??? How do the tabs behave for switching?
    public enum TabID
    {
        Feed,
        Search,
        Camera,
        Workshop,
        User
    }

    HashSet<Object> registered = new HashSet<>();
    MainScreen mainTagActivity;
    Bus uiEventBus;

    private static TabFlowManager instance = null;
    protected TabFlowManager() {
        // Exists only to defeat instantiation.

        uiEventBus = new Bus();

    }

    public static TabFlowManager getInstance() {
        if(instance == null) {
            instance = new TabFlowManager();
        }
        return instance;
    }

    public void setTabActivity(MainScreen mainActivity)
    {
        mainTagActivity = mainActivity;
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

    //we want to move to another tab
    public void switchToTab(TabID fromTab, TabID toTab) throws Exception {

        if(mainTagActivity == null)
            throw new Exception("Need to set main tag activity. Did you forget?");


        //now we done it, lets do the switch
        //-- for now we simply smooth scroll to victory
        mainTagActivity.switchToTab(toTab.ordinal(), true);
    }

    public TabID DefaultStartingTab()
    {
        return TabID.Camera;
    }

    /*
      * It doesn't matter the color of the icons, but they must have solid colors
      */
    public Drawable getTabIcons(int position) {

        TabID tab = TabID.values()[position];
        switch(tab) {
            case Camera:
                return mainTagActivity.getResources().getDrawable(R.drawable.camsilhouette);
            case Workshop:
                return mainTagActivity.getResources().getDrawable(R.drawable.ic_action_lab_black);
            case Search:
                return mainTagActivity.getResources().getDrawable(R.drawable.ic_action_search_black);
            case Feed:
                return mainTagActivity.getResources().getDrawable(R.drawable.ic_action_tiles_large_black);
            case User:
                return mainTagActivity.getResources().getDrawable(R.drawable.ic_action_user_black);
        }
        return null;
    }

    //handle the pager for our tabs -- we're the flow manager damnit- IT'S WHAT WE DO
    public static class ViewPagerAdapter extends FragmentStatePagerAdapter {


        HashMap<TabID, Fragment> savedFragments = new HashMap<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public HashMap<TabID, Fragment> GetFragmentMap()
        {
            return this.savedFragments;
        }

        public Fragment getItem(int num) {
            TabID tab = TabID.values()[num];

            if(savedFragments.containsKey(tab))
                return savedFragments.get(tab);

            Fragment f;
            switch(tab) {
                case Camera:
                    f = new SelectPictureFragment();
                    break;
                case Workshop:
                    f = new WorkshopFragment();
                    break;
                case Search:
                    f = new HashtagSearchFragment();
                    break;
                case Feed:
                    f = new HomeFeedFragment();
                    break;
                case User:
                    f =  new UserSettingsFragment();
                default:
                    f =  new UserSettingsFragment();
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

    public void audioSearchText(ArrayList<String> searchWords)
    {
        HashtagSearchFragment hsf = null;

        HashMap<TabID, Fragment> fragMap = mainTagActivity.adapter.GetFragmentMap();

        //grab our search fragment please
        hsf = (HashtagSearchFragment)fragMap.get(TabID.Search);

        if(hsf != null)
        {
            hsf.setSearchValue(searchWords);
        }
    }

}
