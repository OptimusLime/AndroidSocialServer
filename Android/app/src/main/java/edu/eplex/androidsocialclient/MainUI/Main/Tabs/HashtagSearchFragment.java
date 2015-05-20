package edu.eplex.androidsocialclient.MainUI.Main.Tabs;

import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.quinny898.library.persistentsearch.SearchBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.FeedItem;
import edu.eplex.androidsocialclient.MainUI.API.WinAPIManager;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.ScreenUtilities;
import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;


/**
 * Created by paul on 5/17/15.
 */
public class HashtagSearchFragment extends Fragment {

    @InjectView(R.id.app_search_header_grid_view)
    public GridView refreshGridView;

    @InjectView(R.id.app_search_header_grid_view_frame)
    public PtrClassicFrameLayout refreshFrame;

    @InjectView(R.id.app_search_hash_searchbox)
    public SearchBox searchBox;


    private GridViewAdapter mAdapter;

    long lastTime = -1;

    String lastSearch = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.app_fragment_hash_search, container, false);

        //we now have access to list view thanks to butterknife!
        ButterKnife.inject(this, rootView);

        //then register for events -- we'll get one as soon as this happens
        TabFlowManager.getInstance().registerUIEvents(this);

        searchBox.setLogoText(getString(R.string.default_filter_search));
        searchBox.enableVoiceRecognition(getActivity());
        setSearchListener();

        refreshGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0) {
                }
            }
        });

        mAdapter = new GridViewAdapter(getActivity(), R.layout.app_fragment_feed_grid_item, new ArrayList<FeedItem>());

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
                updateSearchData();

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

        //if this isn't our first rodeo, do another search fetch
        lastSearch = WinAPIManager.getInstance().lastSearchString();
        if(lastSearch != null) {
            lastTime = -1;
            searchBox.populateEditText(new ArrayList<String>(Arrays.asList(lastSearch)));;
            updateSearchData();
        }

        return rootView;
    }

    public void setSearchValue(ArrayList<String> searchValues)
    {
        if(searchBox != null && searchValues != null) {

            if (searchValues.size() > 0) {
                //set according to search from voice
                searchBox.populateEditText(new ArrayList<String>(Arrays.asList(searchValues.get(0))));
            }
        }
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

                Toast.makeText(getActivity(), "Searching for: " + searchTerm, Toast.LENGTH_SHORT).show();

                searchTerm = searchTerm.replace(" ", "");
                searchTerm = "#" + searchTerm;
                lastSearch = searchTerm;

                //last time is research with every search click
                lastTime = -1;

                //clear the adapter please
                mAdapter.clear();
                mAdapter.notifyDataSetChanged();

                updateSearchData();
            }

            @Override
            public void onSearchCleared() {

            }

        });

    }

    void updateSearchData()
    {
        if(lastSearch == null)
            return;

        int count = 10;

        WinAPIManager.getInstance().asyncGetLatestByHashtagAfter(lastSearch, count, lastTime)
                .continueWith(new Continuation<ArrayList<FeedItem>, Void>() {
                    @Override
                    public Void then(Task<ArrayList<FeedItem>> task) throws Exception {

                        //complete the refresh
                        refreshFrame.refreshComplete();

                        //get our feed items
                        ArrayList<FeedItem> items = task.getResult();
                        if (task.getResult() != null && items.size() > 0) {
                            for (int i = 0; i < items.size(); i++) {
                                FeedItem f = items.get(i);

                                if (i == 0)
                                    lastTime = f.date;
                                else
                                    lastTime = Math.max(f.date, lastTime);

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

    public class GridViewAdapter extends ArrayAdapter<FeedItem> {
        private FragmentActivity context;

        HashSet<String> existing = new HashSet<>();
        int layoutResourceId;

        public GridViewAdapter(FragmentActivity context, int layoutResourceId, ArrayList<FeedItem> data) {
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
        public void remove(FeedItem object) {
            super.remove(object);
            existing.remove(object.wid);
        }


        @Override
        public void addAll(Collection<? extends FeedItem> collection) {

            Object[] cArray = collection.toArray();
            for(int i=0; i < cArray.length; i++)
            {
                FeedItem fi = (FeedItem)cArray[i];
                if(existing.contains(fi.wid))
                    collection.remove(fi);
                else
                    existing.add(fi.wid);
            }

            super.addAll(collection);
        }

        @Override
        public void add(FeedItem object) {
            if(!existing.contains(object.wid))
            {
                existing.add(object.wid);
                super.add(object);
            }
        }

        @Override
        public void addAll(FeedItem... items) {

            Collection<FeedItem> nItems = new ArrayList<>();

            for(int i=0; i < items.length; i++) {
                FeedItem fi = (FeedItem) items[i];
                if (!existing.contains(fi.wid)){
                    existing.add(fi.wid);
                    nItems.add(fi);
                }
            }

            if(nItems.size() > 0) {
                FeedItem[] fArray = new FeedItem[nItems.size()];
                nItems.toArray(fArray);
                super.addAll(fArray);
            }
        }

        @Override
        public void insert(FeedItem object, int index) {
            if(!existing.contains(object.wid))
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

            FeedItem fi = this.getItem(position);

            String baseS3Server = context.getResources().getString(R.string.s3_bucket_url_endpoint);

            //we get the image directly from S3
            Uri uri = Uri.parse(baseS3Server + "/" + fi.username + "/" + fi.s3Key + "/" + WinAPIManager.FILTER_FULL);
            SimpleDraweeView draweeView = (SimpleDraweeView) row.findViewById(R.id.app_feed_grid_item_image_view);

            Point screenSize = ScreenUtilities.ScreenSize(context);
            int size = Math.min(screenSize.x, screenSize.y);

            draweeView.getLayoutParams().width = size;
            draweeView.getLayoutParams().height = size;


            draweeView.setImageURI(uri);

            return row;
        }


    }

}
