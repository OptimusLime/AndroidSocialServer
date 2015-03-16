package edu.eplex.androidsocialclient.MainUI.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Collection;
import java.util.List;
import java.util.logging.Filter;

import bolts.Continuation;
import bolts.Task;
import edu.eplex.AsyncEvolution.cardUI.cards.GridCard;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.R;

/**
 * Created by paul on 3/12/15.
 */
public class EditIECCompositeAdapter extends ArrayAdapter<FilterComposite> {
    protected Context mContext;
    protected OnFilterSelected filterSelection;
    public interface OnFilterSelected
    {
        void selectFilteredImage(FilterComposite filter, int ix);
        void longSelectFilteredImage(FilterComposite filter, int ix);
        void finishedLoadingAndFiltering();
    }

    public EditIECCompositeAdapter(Context context, List<FilterComposite> bitmaps, OnFilterSelected selectFunction)
    {
        super(context, 0, bitmaps);
        this.mContext = context;
        this.filterSelection = selectFunction;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //does it exist or not???
        View view = convertView;

        LayoutInflater mInflater = (LayoutInflater)this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //grab our iamge for this position
        FilterComposite filter = this.getItem(position);

        //whaaaaaaaat
        Bitmap mImage = filter.getFilteredThumbnailBitmap();

        int layout = R.layout.app_edit_fragment_iec_individual_thumb;
        boolean recycle = false;
        if (convertView == null) {
            view = mInflater.inflate(layout, parent, false);
        }

        //set it up!
//        setupInnerViewElements(parent, view);

        ImageView bview = (ImageView) view.findViewById(R.id.app_edit_iec_individual_image_thumb);

        if (mImage != null && !mImage.isRecycled()) {
            bview.setImageBitmap(mImage);
            syncDecrementLoadCount();
        } else {

            int widthHeight = (int)mContext.getResources().getDimension(R.dimen.app_edit_iec_thumbnail_size);

            //otherwise, you need to load the thumbnail then filter it
            EditFlowManager.getInstance().lazyLoadFilterIntoImageView(mContext, filter, widthHeight, widthHeight, true, bview, new Continuation<Void, Void>() {
                @Override
                public Void then(Task<Void> task) throws Exception {

                    //when we're done loading, drop it down plz
                    syncDecrementLoadCount();

                    return null;
                }
            });
        }

        //set up listener for each object
        setClickListener(bview, filter, position);

        return view;
    }

    void syncDecrementLoadCount()
    {
        synchronized (inProgress) {
            loadCount--;
            if(loadCount == 0)
                filterSelection.finishedLoadingAndFiltering();
        }
    }

    void setClickListener(View bview, final FilterComposite filter, final int ix)
    {
        bview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set as the main image asynchronously!
                filterSelection.selectFilteredImage(filter, ix);
            }
        });

        bview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                filterSelection.longSelectFilteredImage(filter, ix);
                return true;
            }
        });
    }

    final Object inProgress = new Object();
    int loadCount = 0;

    @Override
    public void add(FilterComposite object) {

        Log.d("EDITIECCOMPADAPTER", "Disabled single add to adapter. Only chunks please.");
        //super.add(object);
    }

    @Override
    public void addAll(Collection<? extends FilterComposite> collection) {

        synchronized (inProgress) {
            loadCount += collection.size();
        }
        super.addAll(collection);
    }
}
