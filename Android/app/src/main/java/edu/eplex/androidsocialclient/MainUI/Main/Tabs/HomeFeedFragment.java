package edu.eplex.androidsocialclient.MainUI.Main.Tabs;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import edu.eplex.androidsocialclient.MainUI.API.Publish.Objects.FeedItem;
import edu.eplex.androidsocialclient.MainUI.API.WinAPIManager;
import edu.eplex.androidsocialclient.R;

import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;


/**
 * Created by paul on 5/17/15.
 */
public class HomeFeedFragment extends Fragment {

    @InjectView(R.id.app_feed_header_grid_view)
    public GridView refreshGridView;

    @InjectView(R.id.app_feed_header_grid_view_frame)
    public PtrClassicFrameLayout refreshFrame;

    private ListAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.app_fragment_feed, container, false);

        //we now have access to list view thanks to butterknife!
        ButterKnife.inject(this, rootView);

        //then register for events -- we'll get one as soon as this happens
        TabFlowManager.getInstance().registerUIEvents(this);

        refreshGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0) {
                }
            }
        });

        mAdapter = new GridViewAdapter(getActivity(), R.layout.app_fragment_feed_grid_item, null);
        refreshGridView.setAdapter(mAdapter);


        refreshFrame.setLastUpdateTimeRelateObject(this);
        refreshFrame.setPtrHandler(new PtrHandler() {
            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
//                updateData();

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
                // mPtrFrame.autoRefresh();
            }
        }, 100);


        return rootView;
    }

    public class GridViewAdapter extends ArrayAdapter<FeedItem> {
        private FragmentActivity context;

        int layoutResourceId;

        public GridViewAdapter(FragmentActivity context, int layoutResourceId, ArrayList<FeedItem> data) {
            super(context, layoutResourceId, data);
            this.context = context;
            this.layoutResourceId = layoutResourceId;
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
            draweeView.setImageURI(uri);

            return row;
        }


    }

}
