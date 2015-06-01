package edu.eplex.androidsocialclient.MainUI.Main.Discovery.Tabs;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;

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
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.ScreenUtilities;
import eplex.win.FastNEATJava.utils.cuid;

/**
 * Created by paul on 6/1/15.
 */
public class ArtifactViewAdapter extends ArrayAdapter<FilterArtifact> {

    public class GridViewInjectionHolder {

        @InjectView(R.id.app_feed_artifact_grid_username_text_view)
        public TextView gridUsernameTextView;

        @InjectView(R.id.app_feed_artifact_grid_item_image_view)
        public SimpleDraweeView gridArtifactImageView;

        @InjectView(R.id.app_feed_artifact_grid_button_original)
        public ImageButton gridArtifactOriginalButton;

        @InjectView(R.id.app_feed_artifact_grid_button_branch)
        public ImageButton gridArtifactBranchButton;


        @OnClick(R.id.app_feed_artifact_grid_button_original)
        public void onOriginalClick()
        {
            //need to handle what happens with an original click

            Task.call(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (!isOriginal) {
                        FilterArtifact fi = filterComposite.getFilterArtifact();

                        //depends on what version of server software -- might not need this in future
                        if (fi.meta.s3Key == null && fi.s3Key == null)
                            return null;

                        String s3Key = fi.meta.s3Key;
                        if (s3Key == null)
                            s3Key = fi.s3Key;

                        String baseS3Server = context.getResources().getString(R.string.s3_bucket_url_endpoint);
                        Uri uri = Uri.parse(baseS3Server + "/" + fi.meta.user + "/" + s3Key + "/" + WinAPIManager.FILTER_FULL);
                        Toast.makeText(context, "or: " + uri, Toast.LENGTH_SHORT).show();
                        gridArtifactImageView.setImageBitmap(null);
                        gridArtifactImageView.setImageURI(uri);
                        isOriginal = true;
                    } else {
                        //replace with previous image
                        gridArtifactImageView.setImageBitmap(filterComposite.getFilteredBitmap());
                        isOriginal = false;
                    }

                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);

        }

        @OnClick(R.id.app_feed_artifact_grid_button_branch)
        public void onBranchClick()
        {
            //need to handle what happens with a branch click
            Toast.makeText(context, "Want to branch to load filtered image.", Toast.LENGTH_SHORT).show();

            FilterArtifact fa = filterComposite.getFilterArtifact();

            //must make this filter artifact a descendant of the original!
            //thus save the current wid
//            String wid = fa.wid();
            //swap the wid for the filter for a brand new ID
//            fa.setWID(cuid.getInstance().generate());

            //set the original wid as one of the parents!
//            fa.setParents(Lists.newArrayList(wid));

            //need to close the intent with this object basically

            //we send in the ORIGINAL object -- this will be our SEED when evolution starts up
            baseFilter.setFilterArtifact(fa);

            //filter achieved!
            DiscoveryFlowManager.getInstance().finishDiscoveryActivity(context, artfactBranchType, baseFilter);
        }

        private FilterComposite baseFilter;
        private FragmentActivity context;
        private FilterComposite filterComposite;
        private boolean isOriginal;
        private String artfactBranchType;

        public GridViewInjectionHolder(FragmentActivity context, FilterComposite fc, FilterComposite baseFilter, String artfactBranchType)
        {
            setContextAndComposite(context,fc, baseFilter, artfactBranchType);
        }
        public void setContextAndComposite(FragmentActivity context, FilterComposite fc, FilterComposite baseFilter, String artfactBranchType)
        {
            this.context = context;
            this.filterComposite = fc;
            this.baseFilter = baseFilter;
            this.artfactBranchType = artfactBranchType;
            isOriginal = false;
//            clearFilterBitmap();
        }

        public void clearFilterBitmap()
        {
            //when overwriting -- remove previous memory issues
            if(this.filterComposite != null && this.filterComposite.getFilteredBitmap() != null)
            {
                //make sure the image view is no longer attached to the filtered bitmap in question
                if(gridArtifactImageView != null)
                    gridArtifactImageView.setImageBitmap(null);

                this.filterComposite.getFilteredBitmap().recycle();
                this.filterComposite.setFilteredBitmap(null);
            }
        }

        public void updateView()
        {
            gridUsernameTextView.setText(filterComposite.getFilterArtifact().meta.user);
        }


    }

    private FragmentActivity context;

    FilterComposite baseFilter;
    HashSet<String> existing = new HashSet<>();
    HashMap<String, FilterComposite> filters = new HashMap<>();
    HashMap<String, GridViewInjectionHolder> viewHolders = new HashMap<>();
    HashMap<View, GridViewInjectionHolder> viewToInjection = new HashMap<>();
    int layoutResourceId;
    String artifactBranchType;

    public ArtifactViewAdapter(FragmentActivity context, String artifactType, int layoutResourceId, FilterComposite baseFilter, ArrayList<FilterArtifact> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.artifactBranchType= artifactType;
        this.layoutResourceId = layoutResourceId;
        this.baseFilter = baseFilter;
    }
    public void clearFilterMemory()
    {

        for (Object me : filters.entrySet()) {

            Map.Entry<String, FilterComposite> mapEntry = ((Map.Entry<String, FilterComposite>)me);

            GridViewInjectionHolder gvi = viewHolders.get(mapEntry.getKey());
            if(gvi != null)
                gvi.clearFilterBitmap();

//                FilterComposite fc = mapEntry.getValue();
//                if (fc.getFilteredBitmap() != null) {
//                    fc.getFilteredBitmap().recycle();
//                    fc.setFilteredBitmap(null);
//                }

        }


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

        if(collection.size() > 0) {
            super.addAll(collection);
        }
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
        GridViewInjectionHolder tgvi = null;//= viewToInjection.get(row);
        if (row == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

        } else {
            //otherwise we're repurposing this thing
            tgvi = viewToInjection.get(row);
        }

        FilterArtifact fi = this.getItem(position);
        final GridViewInjectionHolder gvi;
        final FilterComposite fc;
        if(filters.containsKey(fi.wid()))
        {
            fc = filters.get(fi.wid());
            gvi = viewHolders.get(fi.wid());
            gvi.setContextAndComposite(context, fc, baseFilter, artifactBranchType);
//                gvi.clearFilterBitmap();
        }
        else
        {
            fc= new FilterComposite(baseFilter.getImageURL(), fi, cuid.getInstance().generate(), FilterManager.getInstance().nextReadableName());
            gvi = new GridViewInjectionHolder(context, fc, baseFilter, artifactBranchType);
            filters.put(fi.wid(), fc);
            viewHolders.put(fi.wid(), gvi);
        }

        //if they're different- time to clear!

//            if(gvi != tgvi && tgvi != null)
//                tgvi.clearFilterBitmap();

        viewToInjection.put(row, gvi);
        //load up the injected views - k thx
        ButterKnife.inject(gvi, row);
        gvi.updateView();


        //we get the image directly from S3
//            Uri uri = Uri.parse(baseS3Server + "/" + fi.username + "/" + fi.s3Key + "/" + WinAPIManager.FILTER_FULL);
        final SimpleDraweeView imageView = (SimpleDraweeView) row.findViewById(R.id.app_feed_artifact_grid_item_image_view);

        Point screenSize = ScreenUtilities.ScreenSize(context);
        int size = Math.min(screenSize.x, screenSize.y);

        imageView.getLayoutParams().width = size;
        imageView.getLayoutParams().height = size;

        int filteredFullSize = context.getResources().getInteger(R.integer.medium_filtered_image_size);

        Bitmap bp = fc.getFilteredBitmap();

        imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_emo_tongue_black));
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        if(bp == null)
        {
//                imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_emo_tongue_black));

            try {

                //need to lazy load in the main image -- then async run the filter plz
                EditFlowManager.getInstance().lazyLoadFilterIntoImageView(context, fc, filteredFullSize, filteredFullSize, false, imageView, new Continuation<FilterComposite, Void>() {
                    @Override
                    public Void then(Task<FilterComposite> task) throws Exception {

                        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                        return null;
                    }
                });
            }
            catch (Exception e)
            {
                Toast.makeText(context, "Unable to load filtered image.", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageBitmap(bp);
        }
        final boolean[] touchImage = {false};

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    //we want to display the chosen filter's original image
                    touchImage[0] = true;
                    if (fc != null)
                        imageView.setImageBitmap(fc.getCurrentBitmap());

                    //need to return true if we handle the up action later
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (touchImage[0]) {
                        touchImage[0] = false;
                        if (fc != null)
                            imageView.setImageBitmap(fc.getFilteredBitmap());
                    }

                }

                return false;
            }
        });


        return row;
    }


}