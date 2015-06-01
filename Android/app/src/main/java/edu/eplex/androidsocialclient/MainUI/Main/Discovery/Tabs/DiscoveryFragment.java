package edu.eplex.androidsocialclient.MainUI.Main.Discovery.Tabs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.google.common.collect.Lists;
import com.quinny898.library.persistentsearch.SearchBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Filter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import edu.eplex.androidsocialclient.MainUI.API.WinAPIManager;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Discovery.DiscoveryFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Publish.PublishFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Tabs.TabFlowManager;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.ScreenUtilities;
import eplex.win.FastNEATJava.utils.cuid;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import it.neokree.materialtabs.MaterialTabHost;

/**
 * Created by paul on 3/16/15.
 */
public class DiscoveryFragment extends Fragment {

    DiscoveryFlowManager.DiscoveryID discoverTabID;

    public void setDiscoverTabID(DiscoveryFlowManager.DiscoveryID id)
    {
        this.discoverTabID = id;
    }

    @InjectView(R.id.app_feed_header_grid_view)
    public GridView refreshGridView;

    @InjectView(R.id.app_feed_header_grid_view_frame)
    public PtrClassicFrameLayout refreshFrame;

    public SearchBox searchBox;

    private ArtifactViewAdapter mAdapter;
    private FilterComposite interestedFilter;
    private String lastSearch;

    int skipCount = 0;
    long lastTime = -1;
    long earliestTime = Long.MAX_VALUE;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAdapter.clearFilterMemory();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView;


        if(discoverTabID == DiscoveryFlowManager.DiscoveryID.Search) {

            rootView = inflater.inflate(R.layout.app_fragment_feed_search, container, false);
            SearchBox sb = (SearchBox) rootView.findViewById(R.id.app_discovery_search_hash_searchbox);
            this.searchBox = sb;
            searchBox.setLogoText(getString(R.string.default_filter_search));
            searchBox.enableVoiceRecognition(getActivity());
            setSearchListener();
        }
        else
        {
            rootView = inflater.inflate(R.layout.app_fragment_feed, container, false);

        }
        //we now have access to list view thanks to butterknife!
        ButterKnife.inject(this, rootView);

        lastTime = -1;

        //then register for events -- we'll get one as soon as this happens
        DiscoveryFlowManager.getInstance().registerUIEvents(this);

        //get the filter we wish to investigate
        interestedFilter = FilterManager.getInstance().getLastEditedFilter();
        if(interestedFilter == null)
        {
            interestedFilter = DiscoveryFlowManager.getInstance().getFilterFromDiscoveryIntent(getActivity().getIntent());
        }
        refreshGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0) {
                }
            }
        });

        mAdapter = new ArtifactViewAdapter(getActivity(), FilterManager.DISCOVER_TYPE, R.layout.app_fragment_artifact_grid_item, interestedFilter, new ArrayList<FilterArtifact>());

        try {
            refreshGridView.setAdapter(mAdapter);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        refreshFrame.setLastUpdateTimeRelateObject(this);
        refreshFrame.setPtrHandler(new PtrHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                updateData(true);

                //need to get data
                Log.d("HOMEFEEDFRAGMENT", "Get more data");

            }

            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
            }
        });

        // the following are default settings
        refreshFrame.setResistance(1.7f);
        refreshFrame.setRatioOfHeaderHeightToRefresh(1.2f);
        refreshFrame.setDurationToClose(200);
        refreshFrame.setDurationToCloseHeader(1000);
        // default is false
        refreshFrame.setPullToRefresh(false);
        // default is true
        refreshFrame.setKeepHeaderWhenRefresh(true);
        refreshFrame.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshFrame.autoRefresh();
            }
        }, 100);

        return rootView;
    }


    void setSearchListener()
    {
        searchBox.setSearchListener(new SearchBox.SearchListener(){

            @Override
            public void onSearchOpened() {
                //Use this to tint the screen
            }

            @Override
            public void onSearchClosed() {
                //Use this to un-tint the screen
            }

            @Override
            public void onSearchTermChanged() {
                //React to the search term changing
                //Called after it has updated results
            }

            @Override
            public void onSearch(String searchTerm) {

//                Toast.makeText(getActivity(), "Searching for: " + searchTerm, Toast.LENGTH_SHORT).show();

                searchTerm = searchTerm.replace(" ", "");
                searchTerm = "#" + searchTerm;
                lastSearch = searchTerm;

                //last time is research with every search click
                lastTime = -1;


                //dump the memory before starting again
//                mAdapter.clearFilterMemory();

                //clear the adapter please
                mAdapter.clear();
                mAdapter.notifyDataSetChanged();

                updateData(false);
            }

            @Override
            public void onSearchCleared() {

            }

        });

    }


    Task<ArrayList<FilterArtifact>> getDiscoveryData()
    {
        int count = 10;

        String lt = null;
        if(lastTime != 0 && lastTime != -1)
            lt = "" + lastTime;

        switch (discoverTabID)
        {
            case Recent:
                return WinAPIManager.getInstance().asyncGetLatestArtifacts(lt, count);

            case Favorites:

                //get favorites -- the latest -- no skips please
                return WinAPIManager.getInstance().asyncGetFavoriteArtifacts("paul", 0, count);

            case Popular:
                //when updating data -- never skip any, just pull down latest -- skip count happens
                //when scrolling infinitely
                return WinAPIManager.getInstance().asyncGetPopularArtifacts(0, count);

            case Search:
                if(lastSearch == null)
                    return null;

                return WinAPIManager.getInstance().asyncGetHashtagArtifacts(lastSearch, lt, count);

            default:
                return null;
        }
    }



    void updateData(final boolean insert)
    {
        //
        int count = 10;

        String lt = null;
        if(lastTime != 0 && lastTime != -1)
            lt = "" + lastTime;
//        lastTime = -1;
//lastTime
        Task<ArrayList<FilterArtifact>> asyncGetArtifacts = getDiscoveryData();

        //if null -- skip it for now
        if(asyncGetArtifacts == null) {

            refreshFrame.refreshComplete();
            return;
        }

        asyncGetArtifacts.continueWith(new Continuation<ArrayList<FilterArtifact>, Void>() {
            @Override
            public Void then(Task<ArrayList<FilterArtifact>> task) throws Exception {

                //complete the refresh
                refreshFrame.refreshComplete();

                //get our feed items
                ArrayList<FilterArtifact> items = task.getResult();
                if (items != null && items.size() > 0) {


                    //we need to sort search/recetn and favorites by date -- popular stays in the order it was transmitted
                    switch (discoverTabID) {
                        case Search:
                        case Recent:
                        case Favorites:
                            Collections.sort(items, new Comparator<FilterArtifact>() {
                                @Override
                                public int compare(FilterArtifact lhs, FilterArtifact rhs) {

                                    long d1, d2;
                                    if (lhs.date != 0)
                                        d1 = lhs.date;
                                    else
                                        d1 = lhs.meta.timeofcreation;

                                    if (rhs.date != 0)
                                        d2 = rhs.date;
                                    else
                                        d2 = rhs.meta.timeofcreation;

                                    return (int) (d2 - d1);
                                }
                            });
                            break;
                    }

                    for (int i = 0; i < items.size(); i++) {

                        FilterArtifact f = items.get(i);

                        if (f.date != 0) {
                            lastTime = Math.max(f.date, lastTime);
                            earliestTime = Math.min(f.date, earliestTime);
                        } else if (f.meta != null) {
                            lastTime = Math.max(f.meta.timeofcreation, lastTime);
                            earliestTime = Math.min(f.meta.timeofcreation, earliestTime);
                        }
                    }

                    if(insert)
                    {
                        for(int i=0; i < items.size(); i++)
                        {
                            mAdapter.insert(items.get(i),i);
                        }
                    }
                    else {
                        //add to the adapter plz
                        mAdapter.addAll(items);
                    }

                    mAdapter.notifyDataSetChanged();

                    //how many to skip of all the objects we have? well a good few -- however many we have!
                    skipCount = mAdapter.getCount();

                    //then let the adapter know the data is ready
//                            mAdapter.notifyDataSetChanged();
                }

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);


    }






}
