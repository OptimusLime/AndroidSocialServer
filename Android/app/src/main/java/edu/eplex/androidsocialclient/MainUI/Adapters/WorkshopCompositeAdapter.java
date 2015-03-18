package edu.eplex.androidsocialclient.MainUI.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Task;
import edu.eplex.androidsocialclient.MainUI.Cache.BitmapCacheManager;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.FileUtilities;
import eplex.win.FastNEATJava.utils.cuid;

/**
 * Created by paul on 3/15/15.
 */
public class WorkshopCompositeAdapter extends ArrayAdapter<FilterComposite> {

    //no duplicates please
    Map<String,Integer> existingFilters = new HashMap<>();

    Map<String,Bitmap> cachedImages = new HashMap<>();


    protected Context mContext;
    protected OnCompositeFilterSelected filterSelection;

    public interface OnCompositeFilterSelected
    {
        void publishCompositeFilter(FilterComposite filter, int position);
        void editClickCompositeFilter(FilterComposite filter, int position);
        void selectCompositeFilter(FilterComposite filter, int position);
        void longSelectCompositeFilter(FilterComposite filter, int position);
    }

    public WorkshopCompositeAdapter(Context context, OnCompositeFilterSelected selectFunction)
    {
        super(context, 0);
        this.mContext = context;
        this.filterSelection = selectFunction;
    }
    public WorkshopCompositeAdapter(Context context, List<FilterComposite> bitmaps, OnCompositeFilterSelected selectFunction)
    {
        super(context, 0, bitmaps);
        this.mContext = context;
        this.filterSelection = selectFunction;
    }

    public void safeAdd(FilterComposite filter)
    {
        String uuid = filter.getUniqueID();

        if(!existingFilters.containsKey(uuid))
        {
            existingFilters.put(uuid, this.getCount());
            this.add(filter);
        }
    }

    public void safeRemove(int ix)
    {
        //we want to remove a filter
        FilterComposite filter = this.getItem(ix);

        this.safeRemove(filter);
    }
    public void safeRemove(FilterComposite filter)
    {
        //we remove from our filter map, and then remove from ourselves
        existingFilters.remove(filter.getUniqueID());

        //remove the filter plz
        this.remove(filter);
    }

//    public class CropSquareTransformation implements Transformation {
//        @Override public Bitmap transform(Bitmap source) {
//            int size = Math.min(source.getWidth(), source.getHeight());
//            int x = (source.getWidth() - size) / 2;
//            int y = (source.getHeight() - size) / 2;
//            Bitmap result = Bitmap.createBitmap(source, x, y, size, size);
//            if (result != source) {
//                source.recycle();
//            }
//            return result;
//        }
//
//        @Override public String key() { return "square()"; }
//    }

//    void lazyLoadBitmap(final String url, )
//    {
//        Task.callInBackground(new Callable<Object>() {
//            @Override
//            public Object call() throws Exception {
//
//                return null;
//            }
//        });
//    }

    void lazyLoadBitmapFromCache(String uriPath, int width, final ImageView imageView) throws FileNotFoundException {
        imageView.setImageResource(R.drawable.ic_action_emo_tongue_black);

        BitmapCacheManager.getInstance().lazyLoadBitmap(mContext, uriPath, width, true, new BitmapCacheManager.LazyLoadedCallback() {
            @Override
            public void imageLoaded(String url, Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
            }

            @Override
            public void imageLoadFailed(String reason, Exception e) {
                //shoot, image view all busted
                imageView.setImageResource(R.drawable.ic_action_emo_cry_red_dark);

            }
        });
    }



    void setClickListener(View rootView, final FilterComposite filter, final int ix)
    {

        View editView = rootView.findViewById(R.id.workshop_preview_action_icon_edit);
        View publishView = rootView.findViewById(R.id.workshop_preview_action_icon_publish);
        View imageView = rootView.findViewById(R.id.workshop_preview_image_view);

        editView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterSelection.editClickCompositeFilter(filter, ix);
            }
        });

        publishView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterSelection.publishCompositeFilter(filter, ix);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set as the main image asynchronously!
                filterSelection.selectCompositeFilter(filter, ix);
            }
        });
//
        rootView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                filterSelection.longSelectCompositeFilter(filter, ix);
                return true;
            }
        });
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //does it exist or not???
        View view = convertView;

        LayoutInflater mInflater = (LayoutInflater)this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //we need to inflate if we don't exist
        if (convertView == null) {
            view = mInflater.inflate(R.layout.app_fragment_workshop_preview, parent, false);
        }

        //then we need to inject into an inner view for Picasso
        FilterComposite filter = getItem(position);

        //handle clicks for this view plz
        setClickListener(view, filter, position);

        //ya hear? dis our image yo
        String url = filter.getFilteredImageURL();

        //grab the file object -- load picasso from that
//        File f = new File(url);

        ImageView imagePreview = (ImageView)view.findViewById(R.id.workshop_preview_image_view);
//        ImageView beginIconView = (ImageView)view.findViewById(R.id.workshop_preview_action_icon_edit);
        FrameLayout relativeLayout = (FrameLayout)view.findViewById(R.id.workshop_preview_image_view_holder);

        TextView textView = (TextView)view.findViewById(R.id.workshop_preview_filter_text_name);
        textView.setText(filter.getReadableName());

        int thumbnailSize = (int)mContext.getResources().getDimension(R.dimen.workshop_preview_thumbnail_size);

//        beginIconView.getLayoutParams().width = 70;
//        beginIconView.getLayoutParams().height = 70;
//        beginIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
//        beginIconView.setImageResource(R.drawable.ic_action_process_start_black);

        relativeLayout.getLayoutParams().width = thumbnailSize;
        relativeLayout.getLayoutParams().height = thumbnailSize;
//        imagePreview.getLayoutParams().width = 150;
//        imagePreview.getLayoutParams().height = 150;
        imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagePreview.setImageResource(R.drawable.ic_action_emo_tongue_black);

        //this should fail randomly MWAHAHAH
//        if(Math.random() < .5)
//            url += ".png";

        Bitmap filterThumb = filter.getFilteredThumbnailBitmap();

        if(filterThumb != null && !filterThumb.isRecycled())
            imagePreview.setImageBitmap(filterThumb);
        else {
            try {
                EditFlowManager.getInstance().lazyLoadFilterIntoImageView(mContext, filter, thumbnailSize, thumbnailSize, true, imagePreview);
            } catch (FileNotFoundException e) {
                Toast.makeText(mContext, "Base file not found", Toast.LENGTH_SHORT).show();
            }
        }
        //if we have it on hand, set it up plz
//        Bitmap filterThumb = filter.getFilteredThumbnailBitmap();

        //make sure it's locked please -- is it recylced or not?

//        synchronized (filterThumb) {
//            if (filterThumb != null && !filterThumb.isRecycled())
//                imagePreview.setImageBitmap(filterThumb);
//            else
//                lazyLoadBitmapFromCache(url, thumbnailSize, imagePreview);
//        }

//        Bitmap bm;
//
//
//        if(!cachedImages.containsKey(url))
//        {
//            //decode the image -- specifying a MAX size of 50 -- then we're gonna stick it in our image view after squaring it
//            bm = new CropSquareTransformation().transform(FileUtilities.decodeSampledBitmapFromResource(url, 100));
//            cachedImages.put(url, bm);
//        }
//        else
//            bm = cachedImages.get(url);
//
//        //set it as the cached image -- if we need to.
//        imagePreview.setImageBitmap(bm);

        //set our image -- it'll handle caching and what have you -- pretty simple process
//        Picasso.with(mContext)
//                .load(url)
//                .transform(new CropSquareTransformation())
//                .resize(100, 100)
////                .transform(new CropSquareTransformation())
////                .resize(50, 50)
////                .centerCrop()
//                .error(R.drawable.camsilhouette)
//                .into(imagePreview);

        return view;
    }

}
