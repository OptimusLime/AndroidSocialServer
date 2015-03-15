package edu.eplex.androidsocialclient.MainUI.Main;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import edu.eplex.androidsocialclient.MainUI.TakePictureFragment;
import edu.eplex.androidsocialclient.MainUI.UserSettingsFragment;
import edu.eplex.androidsocialclient.R;
import it.neokree.materialtabs.MaterialTab;
import it.neokree.materialtabs.MaterialTabHost;
import it.neokree.materialtabs.MaterialTabListener;

/**
 * Created by paul on 3/15/15.
 */
public class MainScreen extends ActionBarActivity implements MaterialTabListener {
    MaterialTabHost tabHost;
    ViewPager pager;
    ViewPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_main_layout);

//        Toolbar toolbar = (android.support.v7.widget.Toolbar) this.findViewById(R.id.app_main_toolbar);
//        this.setSupportActionBar(toolbar);

        tabHost = (MaterialTabHost) this.findViewById(R.id.materialTabHost);


        pager = (ViewPager) this.findViewById(R.id.app_main_pager);

        // init view pager
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // when user do a swipe the selected tab change
                tabHost.setSelectedNavigationItem(position);

            }
        });

        // insert all tabs from pagerAdapter data
        for (int i = 0; i < adapter.getCount(); i++) {
            tabHost.addTab(
                    tabHost.newTab()
                            .setIcon(getIcon(i))//adapter.getPageTitle(i))
                            .setTabListener(this)
            );

        }

    }


    @Override
    public void onTabSelected(MaterialTab tab) {
        pager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(MaterialTab tab) {

    }

    @Override
    public void onTabUnselected(MaterialTab tab) {

    }
    /*
       * It doesn't matter the color of the icons, but they must have solid colors
       */
    private Drawable getIcon(int position) {
        switch(position) {
            case 0:
                return getResources().getDrawable(R.drawable.camsilhouette);
            case 1:
                return getResources().getDrawable(R.drawable.galleryicon);
            case 2:
                return getResources().getDrawable(R.drawable.videosilhouette);
            case 3:
                return getResources().getDrawable(R.drawable.ic_action_mail);
            case 4:
                return getResources().getDrawable(R.drawable.ic_action_lock_closed);
        }
        return null;
    }

    private class ViewPagerAdapter extends FragmentStatePagerAdapter {

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);

        }

        public Fragment getItem(int num) {
            switch (num)
            {
                case 0:
                    return new UserSettingsFragment();
                case 1:
                    return new TakePictureFragment();
                case 2:
                    return new UserSettingsFragment();
                case 3:
                    return new UserSettingsFragment();
                case 4:
                    return new UserSettingsFragment();
                default:
                    return new UserSettingsFragment();
            }
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
