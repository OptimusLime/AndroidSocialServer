package edu.eplex.androidsocialclient.MainUI.Main;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.util.Arrays;

import dagger.ObjectGraph;
import edu.eplex.androidsocialclient.MainUI.API.Modules.AdjustableHostAPIModule;
import edu.eplex.androidsocialclient.MainUI.API.WinAPIManager;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Publish.PublishFlowManager;
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

        //we need to inject our objects!
        ObjectGraph graph = ObjectGraph.create(Arrays.asList(new AdjustableHostAPIModule(this, R.string.local_app_server_endpoint, R.string.s3_bucket_url_endpoint)).toArray());

        //send it in!
        WinAPIManager.getInstance().injectAPIManager(graph);

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
        FilterManager fm = FilterManager.getInstance();
        switch (requestCode)
        {
            case EditFlowManager.EDIT_SCREEN_REQUEST_CODE:
                fm.asyncSaveFiltersToFile(this);

                break;
            case PublishFlowManager.PUBLISH_SCREEN_REQUEST_CODE:

                if(resultCode == FragmentActivity.RESULT_OK) {

                    //we actually published something!
                    try {
                        fm.publishedCompositeFilter(this, fm.getFilter(data.getStringExtra("filter")), true);
                    }
                    catch (Exception e)
                    {
                        //failed. doh.
                        Log.d("MAINEDITSCREEN", "Failed to publish on local device: " + e.getMessage());
                    }
                }

                break;
        }
    }


}
