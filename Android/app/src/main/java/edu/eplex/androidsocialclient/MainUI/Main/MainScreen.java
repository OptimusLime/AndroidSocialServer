package edu.eplex.androidsocialclient.MainUI.Main;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.quinny898.library.persistentsearch.SearchBox;

import java.util.ArrayList;
import java.util.Arrays;

import dagger.ObjectGraph;
import edu.eplex.androidsocialclient.MainUI.API.Modules.AdjustableHostAPIModule;
import edu.eplex.androidsocialclient.MainUI.API.WinAPIManager;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Discovery.DiscoveryFlowManager;
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
    public ViewPager pager;
    public TabFlowManager.ViewPagerAdapter adapter;

    @Override
    protected void onStart() {
        super.onStart();

//        hideKeyboard();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fresco.initialize(this);

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
                hideKeyboard();
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

        hideKeyboard();
    }

    public void switchToTab(TabFlowManager.TabID tab, boolean smooth)
    {
        switchToTab(tab.ordinal(), smooth);
    }

    //simple switch call -- smooth if you want it
    public void switchToTab(int pos, boolean smooth)
    {
        pager.setCurrentItem(pos, smooth);
        //always hide keyboard on switching
        hideKeyboard();
    }

    void hideKeyboard()
    {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        try {
//            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

//            inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        catch (NullPointerException e)
        {
            inputMethodManager.hideSoftInputFromWindow(this.pager.getWindowToken(), 0);

        }
    }

    @Override
    public void onTabSelected(MaterialTab tab) {
        pager.setCurrentItem(tab.getPosition());
        hideKeyboard();
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

        Log.d("MAINEDITSCREEN", "Activity RESULT from " + requestCode);


        ArrayList<String> matches;
        switch (requestCode)
        {
            case SearchBox.VOICE_RECOGNITION_CODE:
                if(resultCode == FragmentActivity.RESULT_OK) {
                    matches = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    TabFlowManager.getInstance().audioSearchText(matches);
                }
                break;
            case DiscoveryFlowManager.DISCOVERY_SCREEN_REQUEST_CODE:
                //need to handle returning from discovery -- k thx

                if(resultCode == FragmentActivity.RESULT_OK) {

                   FilterManager.getInstance().makeTemporaryFilterPermanent(this, data.getStringExtra("filter"));

                    try {
                        TabFlowManager.getInstance().switchToTab(TabFlowManager.TabID.Camera, TabFlowManager.TabID.Workshop);
                    } catch (Exception e) {
                    }
                }
                else
                {
                    try {
                        TabFlowManager.getInstance().switchToTab(TabFlowManager.TabID.Workshop, TabFlowManager.TabID.Camera);
                    } catch (Exception e) {
                    }
                    if(data == null)
                    {
                        if(fm.getLastEditedFilter() != null)
                            fm.deleteTemporaryFilter(fm.getLastEditedFilter().getUniqueID());
                    }
                    else
                    {
                        String df = data.getStringExtra("filter");
                        if(df != null)
                            fm.deleteTemporaryFilter(df);
                    }
                }

                break;

            case EditFlowManager.EDIT_SCREEN_REQUEST_CODE:
                fm.asyncSaveFiltersToFile(this);

                break;
            case PublishFlowManager.PUBLISH_SCREEN_REQUEST_CODE:

                if(resultCode == FragmentActivity.RESULT_OK) {

                    //we actually published something!
                    try {
                        Log.d("MAINSCREEN", "Closed activity after successful publish, now publishing.");

                        fm.publishedCompositeFilter(this, fm.getFilter(data.getStringExtra("filter")), true);
                        TabFlowManager.getInstance().switchToTab(TabFlowManager.TabID.Workshop, TabFlowManager.TabID.Feed);
                    }
                    catch (Exception e)
                    {
                        //failed. doh.
                        Log.d("MAINSCREEN", "Failed to publish on local device: " + e.getMessage());
                    }
                }
                else
                {
                    Log.d("MAINSCREEN", "Result code not okay: " + resultCode + " ok: " + FragmentActivity.RESULT_OK);
                }

                break;
        }


    }


}
