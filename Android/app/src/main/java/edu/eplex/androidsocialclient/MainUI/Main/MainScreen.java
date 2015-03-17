package edu.eplex.androidsocialclient.MainUI.Main;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Tabs.SelectPictureFragment;
import edu.eplex.androidsocialclient.MainUI.Main.Tabs.TabFlowManager;
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
    TabFlowManager.ViewPagerAdapter adapter;

    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //start loading things in -- async please
        FilterManager.getInstance().asyncLoadFiltersFromFile(this);

        //make sure to set our tab activity as us on create!
        TabFlowManager.getInstance().setTabActivity(this);

        setContentView(R.layout.app_main_layout);

//        Toolbar toolbar = (android.support.v7.widget.Toolbar) this.findViewById(R.id.app_main_toolbar);
//        this.setSupportActionBar(toolbar);

        TabFlowManager tfm = TabFlowManager.getInstance();

        tabHost = (MaterialTabHost) this.findViewById(R.id.materialTabHost);


        pager = (ViewPager) this.findViewById(R.id.app_main_pager);

        // init view pager
        adapter = new TabFlowManager.ViewPagerAdapter(getSupportFragmentManager());
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
                            .setIcon(tfm.getTabIcons(i))//adapter.getPageTitle(i))
                            .setTabListener(this)
            );
        }

        //no need to be graceful, just switch immediately
        switchToTab(tfm.DefaultStartingTab(), false);
    }

    public void switchToTab(TabFlowManager.TabID tab, boolean smooth)
    {
        switchToTab(tab.ordinal(), smooth);
    }

    //simple switch call -- smooth if you want it
    public void switchToTab(int pos, boolean smooth)
    {
        pager.setCurrentItem(pos, smooth);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FilterManager.getInstance().asyncSaveFiltersToFile(this);
    }


}
