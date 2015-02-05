package cardUI.cards;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardThumbnail;
import win.eplex.backbone.R;

/**
 * Created by paul on 10/19/14.
 */
public class HomeGridCard extends Card {

    Activity activity;
    HomeGridThumb thumbnail;
    public String wid;
    Bitmap internalBitmap;
    HomeGridCardButtonHandler buttonHandler;


    public HomeGridCard(Activity activity) {
        super(activity, R.layout.home_card_buttons);
        this.activity = activity;
    }

    public HomeGridCard(Activity activity, int innerLayout) {
        super(activity, innerLayout);
        this.activity = activity;
    }

    public void setButtonHandler(HomeGridCardButtonHandler handler)
    {
        buttonHandler = handler;
    }

    public Bitmap getThumbnailBitmap()
    {
        return internalBitmap;
    }
    public void constructImage(String wid, Bitmap bitThumb) {
        thumbnail = new HomeGridThumb(getContext(), wid, bitThumb);
        internalBitmap = bitThumb;
        this.wid = wid;

//        if (resourceIdThumbnail > -1)
//            thumbnail.setDrawableResource(resourceIdThumbnail);
//        else
//            thumbnail.setDrawableResource(R.drawable.ic_ic_launcher_web);

        addCardThumbnail(thumbnail);
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {

        Typeface typeFace= Typeface.createFromAsset(getContext().getAssets(), "fonts/android.ttf");

        Button myTextView=(Button)view.findViewById(R.id.card_button_evolve);
        myTextView.setTypeface(typeFace);
        myTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //we should process the like!
                if(buttonHandler != null)
                {
                    //we send out to our button handler
                    buttonHandler.handleEvolve(wid);
                }
                Toast.makeText(getContext(), "Evolve please!", Toast.LENGTH_SHORT).show();

            }
        });

        myTextView=(Button)view.findViewById(R.id.card_button_history);
        myTextView.setTypeface(typeFace);
        myTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(buttonHandler != null) {
                    //we send out to our button handler
                    buttonHandler.handleHistory(wid);
                }
            }
        });

    }
    public interface HomeGridCardButtonHandler
    {
        void handleEvolve(String wid);
        void handleHistory(String wid);
    }

    class HomeGridThumb extends CardThumbnail {

        public HomeGridThumb(final Context context, final String wid, final Bitmap b) {
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

            viewImage.getLayoutParams().width = (int)viewImage.getResources().getDimension(R.dimen.home_card_grid_render_width);
            viewImage.getLayoutParams().height = (int)viewImage.getResources().getDimension(R.dimen.home_card_grid_render_height);
        }
    }
}
