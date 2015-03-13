package edu.eplex.AsyncEvolution.cardUI.cards;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.text.Html;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import it.gmariotti.cardslib.library.internal.base.BaseCard;
import edu.eplex.AsyncEvolution.R;


public class GridCard extends Card {

    protected TextView mTitle;
    protected TextView mSecondaryTitle;
    protected RatingBar mRatingBar;
    public int resourceIdThumbnail = -1;
    protected int count;

    boolean isLiked;
    boolean isFavorited;

    GestureDetector gestureDetector;

    Activity activity;
    GplayGridThumb thumbnail;
    public String wid;
    GridCardButtonHandler buttonHandler;
    Bitmap internalBitmap;

    public GridCard(Activity activity) {
        super(activity, R.layout.card_inner_buttons);
        this.activity = activity;
    }

    public GridCard(Activity activity, int innerLayout) {
        super(activity, innerLayout);
        this.activity = activity;
    }

    public Bitmap getThumbnailBitmap()
    {
        return internalBitmap;
    }
    public void constructImage(String wid, Bitmap bitThumb)
    {
        thumbnail = new GplayGridThumb(getContext(), wid, bitThumb);
        internalBitmap = bitThumb;
        this.wid = wid;

//        if (resourceIdThumbnail > -1)
//            thumbnail.setDrawableResource(resourceIdThumbnail);
//        else
//            thumbnail.setDrawableResource(R.drawable.ic_ic_launcher_web);

        addCardThumbnail(thumbnail);

        activity.runOnUiThread(new Runnable() {
            public void run() {
                gestureDetector = new GestureDetector(getContext(), new GestureListener(new DoubleTapHandler() {
                    @Override
                    public void handleDoubleTap() {
                        onLikeButtonPressed();
                    }
                }));

                //Set a clickListener on Thumbnail area -- watch for double tap!
                thumbnail.setGestureDetector(gestureDetector);
            }
        });





    }
    public void setButtonHandler(GridCardButtonHandler buttonHandler) {

        this.buttonHandler = buttonHandler;

        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                //Do something
            }
        });
    }

    private interface DoubleTapHandler
    {
        void handleDoubleTap();
    }
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {


        DoubleTapHandler doubleTapHandler;
        public GestureListener(DoubleTapHandler doubleTapHandler)
        {
            this.doubleTapHandler = doubleTapHandler;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {

            if(this.doubleTapHandler != null)
                this.doubleTapHandler.handleDoubleTap();

            return true;
        }
    }

    void onLikeButtonPressed()
    {
        //we should process the like!
        if(buttonHandler != null)
        {
            //we send out to our button handler
            isLiked = buttonHandler.handleLike(wid, isLiked);
        }

       setLikeButton();
    }
    void setLikeButton()
    {
        Button like = (Button)this.mCardView.getInternalMainCardLayout().findViewById(R.id.card_button_like);
        if(isLiked)
            like.setText("\uE815");
        else
            like.setText("\uE814");
    }

    void setFavoriteButton()
    {
        Button fav = (Button)this.mCardView.getInternalMainCardLayout().findViewById(R.id.card_button_favorite);
        if(isFavorited)
            fav.setText("\uE812");
        else
            fav.setText("\uE813");
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {

        Typeface typeFace= Typeface.createFromAsset(getContext().getAssets(), "fonts/android.ttf");

        Button myTextView=(Button)view.findViewById(R.id.card_button_like);
        myTextView.setTypeface(typeFace);
        setLikeButton();
        myTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onLikeButtonPressed();
            }
        });

        myTextView=(Button)view.findViewById(R.id.card_button_favorite);
        myTextView.setTypeface(typeFace);
        setFavoriteButton();
        myTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //we should process the like!
                if(buttonHandler != null)
                {
                    //we send out to our button handler
                    isFavorited = buttonHandler.handleFavorite(wid, isFavorited);
                }

                setFavoriteButton();
            }
        });

        myTextView=(Button)view.findViewById(R.id.card_button_inspect);
        myTextView.setTypeface(typeFace);
        myTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //we should process the like!
                if(buttonHandler != null)
                {
                    //we send out to our button handler
                    buttonHandler.handleInspect(wid);
                }

//                Button inspect = (Button)view.findViewById(R.id.card_button_inspect);

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
                    buttonHandler.handlePublish(wid);
                }

//                Button inspect = (Button)view.findViewById(R.id.card_button_publish);

            }
        });




    }

    public interface GridCardButtonHandler
    {
        boolean handleLike(String wid, boolean like);
        void handlePublish(String wid);
        boolean handleFavorite(String wid, boolean favorite);
        void handleInspect(String wid);
    }

    class GplayGridThumb extends CardThumbnail {

        GestureDetector gestureDetector;

        public GplayGridThumb(final Context context, final String wid, final Bitmap b) {
            super(context);
            this.setCustomSource(new CustomSource() {
                @Override
                public String getTag() {
                    return wid;
                }

                @Override
                public Bitmap getBitmap() {
                    return b;
                }
            });
        }


        @Override
        public void setupInnerViewElements(ViewGroup parent, View viewImage) {

            viewImage.getLayoutParams().width = (int)viewImage.getResources().getDimension(R.dimen.card_grid_render_width);
            viewImage.getLayoutParams().height = (int)viewImage.getResources().getDimension(R.dimen.card_grid_render_height);

           viewImage.setOnTouchListener(new View.OnTouchListener() {
               @Override
               public boolean onTouch(View view, MotionEvent motionEvent) {
                   return gestureDetector.onTouchEvent(motionEvent);
               }
           });
            //viewImage.getLayoutParams().width = 196;
            //viewImage.getLayoutParams().height = 196;

        }

        public void setGestureDetector(GestureDetector gestureDetector) {

            this.gestureDetector = gestureDetector;
        }
    }

}
