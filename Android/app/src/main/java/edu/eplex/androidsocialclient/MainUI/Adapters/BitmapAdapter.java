package edu.eplex.androidsocialclient.MainUI.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.method.CharacterPickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.List;

import edu.eplex.AsyncEvolution.cardUI.cards.GridCard;
import edu.eplex.androidsocialclient.R;

/**
 * Created by paul on 3/12/15.
 */
public class BitmapAdapter extends ArrayAdapter<Bitmap> {
    protected Context mContext;
    protected OnFilterSelected filterSelection;
    public interface OnFilterSelected
    {
        void selectImageAtIndex(int ix);
        void longSelectImageAtIndex(int ix);
    }


    public BitmapAdapter(Context context, List<Bitmap> bitmaps, OnFilterSelected selectFunction)
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
        Bitmap mImage = this.getItem(position);

        int layout = R.layout.iec_filter_scroll_image;
        boolean recycle = false;
        if (convertView == null) {
            view = mInflater.inflate(layout, parent, false);
        }

        //set it up!
//        setupInnerViewElements(parent, view);

        ImageView bview = (ImageView) view.findViewById(R.id.bitmap_image_view);
        if(mImage != null) {
            bview.setImageBitmap(mImage);
        }

        //set up listener for each object
        setClickListener(bview, position);

        return view;
    }

    void setClickListener(View bview, final int ix)
    {
        bview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set as the main image asynchronously!
                filterSelection.selectImageAtIndex(ix);
            }
        });

        bview.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                filterSelection.longSelectImageAtIndex(ix);
                return true;
            }
        });
    }

    boolean isLiked;
    boolean isFavorited;
    GridCard.GridCardButtonHandler buttonHandler;

    void setLikeButton( Button like )
    {
        if(isLiked)
            like.setText("\uE815");
        else
            like.setText("\uE814");
    }

    void setFavoriteButton(Button fav)
    {
        if(isFavorited)
            fav.setText("\uE812");
        else
            fav.setText("\uE813");
    }

    public void setupInnerViewElements(ViewGroup parent, View view) {

        Typeface typeFace= Typeface.createFromAsset(getContext().getAssets(), "fonts/android.ttf");

        Button myTextView=(Button)view.findViewById(R.id.card_button_like);
        myTextView.setTypeface(typeFace);
        setLikeButton(myTextView);
        myTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                onLikeButtonPressed();
            }
        });

        myTextView=(Button)view.findViewById(R.id.card_button_publish);
        myTextView.setTypeface(typeFace);
        myTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //we should process the like!
                if(buttonHandler != null)
                {
                    //we send out to our button handler
//                    buttonHandler.handlePublish(wid);
                }

//                Button inspect = (Button)view.findViewById(R.id.card_button_publish);

            }
        });




    }
}
