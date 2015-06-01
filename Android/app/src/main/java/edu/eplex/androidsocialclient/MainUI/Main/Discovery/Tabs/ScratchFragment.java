package edu.eplex.androidsocialclient.MainUI.Main.Discovery.Tabs;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import dagger.ObjectGraph;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncSeedLoader;
import edu.eplex.AsyncEvolution.cardUI.EndlessGridScrollListener;
import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.FeedItem;
import edu.eplex.androidsocialclient.MainUI.API.WinAPIManager;
import edu.eplex.androidsocialclient.MainUI.Filters.Evolution.FilterSeedLoader;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Discovery.DiscoveryFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Publish.PublishFlowManager;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.ScreenUtilities;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;

/**
 * Created by paul on 3/16/15.
 */
public class ScratchFragment extends Fragment {

    @InjectView(R.id.app_feed_header_grid_view)
    public GridView refreshGridView;

    @InjectView(R.id.app_feed_header_grid_view_frame)
    public PtrClassicFrameLayout refreshFrame;

    private ArtifactViewAdapter mAdapter;
    private FilterComposite interestedFilter;
    long lastTime = -1;

    EndlessGridScrollListener mScrollListener;

    @Inject
    AsyncSeedLoader seedLoader;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.app_fragment_feed, container, false);

        //we now have access to list view thanks to butterknife!
        ButterKnife.inject(this, rootView);

        //then register for events -- we'll get one as soon as this happens
        DiscoveryFlowManager.getInstance().registerUIEvents(this);

        refreshGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0) {
                }
            }
        });

        //get the filter we wish to investigate
        interestedFilter = FilterManager.getInstance().getLastEditedFilter();
        if(interestedFilter == null)
        {
            interestedFilter = DiscoveryFlowManager.getInstance().getFilterFromDiscoveryIntent(getActivity().getIntent());
        }

        mAdapter = new ArtifactViewAdapter(getActivity(), FilterManager.SEED_TYPE, R.layout.app_fragment_artifact_grid_item, interestedFilter, new ArrayList<FilterArtifact>());

        try {
            refreshGridView.setAdapter(mAdapter);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //here we're going to set our scroll listener for creating more objects and appending them!
        mScrollListener = new EndlessGridScrollListener(refreshGridView);

//        //lets set our callback item now -- this is called whenever the user scrolls to the bottom
        mScrollListener.setRequestItemsCallback(new EndlessGridScrollListener.RequestItemsCallback() {
            @Override
            public void requestItems(int pageNumber) {
//                System.out.println("On Refresh invoked..");

                //add more cards, hoo-ray!!!

                //every time it's the same process -- generate artifacts, convert to phenotype, display!
                //rinse and repeat
                //this gets appended to bottom -- do not insert at 0th position
                updateData(false);
                mScrollListener.notifyMorePages();
            }
        });
//        //make sure to add our infinite scroller here
        refreshGridView.setOnScrollListener(mScrollListener);


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
    void updateData(boolean insert)
    {
        //
        int count = 3;

        lastTime = -1;

        ArrayList<FilterArtifact> generatedArtifacts = ((FilterSeedLoader)seedLoader).CreateRandomSeeds(count);

        //add to the adapter plz
        if(insert)
        {
            for(int i=0; i < generatedArtifacts.size(); i++)
                mAdapter.insert(generatedArtifacts.get(i), 0);
        }
        else
            mAdapter.addAll(generatedArtifacts);

        mAdapter.notifyDataSetChanged();

        //complete the refresh
        refreshFrame.refreshComplete();
    }


}
