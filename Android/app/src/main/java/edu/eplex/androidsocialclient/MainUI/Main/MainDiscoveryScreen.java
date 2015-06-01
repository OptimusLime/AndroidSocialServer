package edu.eplex.androidsocialclient.MainUI.Main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import butterknife.ButterKnife;
import butterknife.InjectView;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Main.Discovery.DiscoveryFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Tabs.TabFlowManager;
import edu.eplex.androidsocialclient.R;
import it.neokree.materialtabs.MaterialTab;
import it.neokree.materialtabs.MaterialTabHost;
import it.neokree.materialtabs.MaterialTabListener;

/**
 * Created by paul on 5/13/15.
 */
public class MainDiscoveryScreen extends ActionBarActivity implements MaterialTabListener {

    @InjectView(R.id.app_discovery_materialTabHost)
    MaterialTabHost tabHost;

    @InjectView(R.id.app_discovery_view_pager)
    public ViewPager pager;

    @InjectView(R.id.app_discovery_toolbar)
    public Toolbar discoverToolbar;

    public DiscoveryFlowManager.ViewPagerAdapter adapter;


    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up the publish UI -- then we need to instantiate the tabs
        setContentView(R.layout.app_discovery_main_ui);

        //anytime we're created -- this means we don't have the editor started
//        editorStarted = false;

//        discoverToolbar

        //make sure edit flow manager knows wassup
        DiscoveryFlowManager dfm = DiscoveryFlowManager.getInstance();

        dfm.setMainDiscoveryActivity(this);

        //we now have access to list view thanks to butterknife!
        ButterKnife.inject(this);

        this.setSupportActionBar(discoverToolbar);
//        abActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setTitle(getResources().getString(R.string.discovery_title));

        //confirm we want options callback
//        setHasOptionsMenu(true);

        //we were probably launched by intent
        Intent intent = getIntent();

        //get our flow manager for this fragmentactivity -- knows all about our intent :)
        EditFlowManager efm = EditFlowManager.getInstance();

        //grab our existing item
        FilterComposite currentFilter = efm.getFilterFromEditIntent(intent);

        // init view pager
        adapter = new DiscoveryFlowManager.ViewPagerAdapter(getSupportFragmentManager(), this);
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
                            .setIcon(dfm.getTabIcons(i))//adapter.getPageTitle(i))
                            .setTabListener(this)
            );
        }

        //no need to be graceful, just switch immediately
        switchToTab(dfm.DefaultStartingTab(), false);
    }

    public void switchToTab(DiscoveryFlowManager.DiscoveryID tab, boolean smooth)
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }


}
