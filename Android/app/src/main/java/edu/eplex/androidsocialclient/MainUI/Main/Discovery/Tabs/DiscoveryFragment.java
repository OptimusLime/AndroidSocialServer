package edu.eplex.androidsocialclient.MainUI.Main.Discovery.Tabs;

import android.content.Context;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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

    private GridViewAdapter mAdapter;

    long lastTime = -1;

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

        mAdapter = new GridViewAdapter(getActivity(), R.layout.app_fragment_feed_grid_item, new ArrayList<FilterArtifact>());

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
                updateData();

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
    void updateData()
    {
        //
        int count = 10;

        String lt = null;
        if(lastTime != 0 && lastTime != -1)
            lt = "" + lastTime;
//        lastTime = -1;
//lastTime
        WinAPIManager.getInstance().asyncGetLatestArtifacts(lt, count)
                .continueWith(new Continuation<ArrayList<FilterArtifact>, Void>() {
                    @Override
                    public Void then(Task<ArrayList<FilterArtifact>> task) throws Exception {

                        //complete the refresh
                        refreshFrame.refreshComplete();

                        //get our feed items
                        ArrayList<FilterArtifact> items = task.getResult();
                        if (task.getResult() != null && items.size() > 0) {
                            for (int i = 0; i < items.size(); i++) {

                                FilterArtifact f = items.get(i);

                                if(f.date != 0)
                                    lastTime = Math.max(f.date, lastTime);
                                else if(f.meta != null)
                                {
                                    lastTime = Math.max(f.meta.timeofcreation, lastTime); ;
                                }
                            }

                            //add to the adapter plz
                            mAdapter.addAll(items);

                            //then let the adapter know the data is ready
//                            mAdapter.notifyDataSetChanged();
                        }

                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);


    }

    public class GridViewAdapter extends ArrayAdapter<FilterArtifact> {
        private FragmentActivity context;

        HashSet<String> existing = new HashSet<>();
        int layoutResourceId;

        public GridViewAdapter(FragmentActivity context, int layoutResourceId, ArrayList<FilterArtifact> data) {
            super(context, layoutResourceId, data);
            this.context = context;
            this.layoutResourceId = layoutResourceId;
        }

        @Override
        public void clear() {
            super.clear();
            existing.clear();
        }

        @Override
        public void remove(FilterArtifact object) {
            super.remove(object);
            existing.remove(object.wid());
        }


        @Override
        public void addAll(Collection<? extends FilterArtifact> collection) {

            Object[] cArray = collection.toArray();
            for(int i=0; i < cArray.length; i++)
            {
                FilterArtifact fi = (FilterArtifact)cArray[i];
                if(existing.contains(fi.wid()))
                    collection.remove(cArray[i]);
                else
                    existing.add(fi.wid());
            }

            super.addAll(collection);
        }

        @Override
        public void add(FilterArtifact object) {
            if(!existing.contains(object.wid()))
            {
                existing.add(object.wid());
                super.add(object);
            }
        }

        @Override
        public void addAll(FilterArtifact... items) {

            Collection<FilterArtifact> nItems = new ArrayList<>();

            for(int i=0; i < items.length; i++) {
                FilterArtifact fi = (FilterArtifact) items[i];
                if (!existing.contains(fi.wid())){
                    existing.add(fi.wid());
                    nItems.add(fi);
                }
            }

            if(nItems.size() > 0) {
                FilterArtifact[] fArray = new FilterArtifact[nItems.size()];
                nItems.toArray(fArray);
                super.addAll(fArray);
            }
        }

        @Override
        public void insert(FilterArtifact object, int index) {
            if(!existing.contains(object.wid()))
                super.insert(object, index);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if (row == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

            } else {
            }

            FilterArtifact fi = this.getItem(position);

            String baseS3Server = context.getResources().getString(R.string.s3_bucket_url_endpoint);

            //we get the image directly from S3
//            Uri uri = Uri.parse(baseS3Server + "/" + fi.username + "/" + fi.s3Key + "/" + WinAPIManager.FILTER_FULL);
            SimpleDraweeView draweeView = (SimpleDraweeView) row.findViewById(R.id.app_feed_grid_item_image_view);

            Point screenSize = ScreenUtilities.ScreenSize(context);
            int size = Math.min(screenSize.x, screenSize.y);

            draweeView.getLayoutParams().width = size;
            draweeView.getLayoutParams().height = size;

//            draweeView.setImageURI(uri);

            return row;
        }


    }


}
