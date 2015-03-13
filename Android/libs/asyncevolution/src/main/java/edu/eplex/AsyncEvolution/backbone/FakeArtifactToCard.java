package edu.eplex.AsyncEvolution.backbone;


import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;

import com.octo.android.robospice.Jackson2SpringAndroidSpiceService;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.PendingRequestListener;
import com.octo.android.robospice.request.listener.RequestListener;
import com.octo.android.robospice.request.listener.SpiceServiceListener;

import edu.eplex.AsyncEvolution.cardUI.cards.GridCard;
import eplex.win.winBackbone.Artifact;
import it.gmariotti.cardslib.library.internal.Card;
import edu.eplex.AsyncEvolution.R;

/**
 * Created by paul on 8/13/14.
 */
public class FakeArtifactToCard implements ArtifactToCard {

    private SpiceManager spiceManager = new SpiceManager(Jackson2SpringAndroidSpiceService.class);
    private ArtifactCardCallback cardCallback;
    private Artifact currentArtifact;

    private Activity activity;

    @Override
    public void spicyStart(Activity act) {

        this.activity = act;

        spiceManager.start(act);
        if (currentArtifact != null) {
            PendingRequestListener prl;
            spiceManager.addListenerIfPending(double[][].class, currentArtifact.wid(), new SpicyPhenotypeRequestListener());
        }
    }

    @Override
    public void spicyStop() {
        if (spiceManager.isStarted()) {
            spiceManager.shouldStop();
        }
    }

    @Override
    public Card convertArtifactToUI(Artifact offspring) {

        return convertArtifactToPhenotype(new FakeArtifactToPhenotypeMapping(offspring, null).syncConvertNetworkToOutputs());
    }

    @Override
    public void asyncConvertArtifactToUI(Artifact offspring, ArtifactCardCallback finished) {

        //we need to take our spicy request, make it, and get our stuff back
        this.cardCallback  = finished;

        //now we make a request for our phenotype mapping to be sent
        FakeArtifactToPhenotypeMapping request = new FakeArtifactToPhenotypeMapping(offspring, null);
        spiceManager.execute(request, offspring.wid(), DurationInMillis.ALWAYS_RETURNED, new SpicyPhenotypeRequestListener());
    }

    private Card convertArtifactToPhenotype(double[][] results)
    {
        //now that we've got the juice, let's make ourselves useful, eh?
        //we should make a card with this info
        if(this.activity != null)
        {

            GridCard card = new GridCard(this.activity);

            int i = (int)Math.floor(Math.random()*Integer.MAX_VALUE);

//            card.headerTitle = "App example " + i;
//            card.secondaryTitle = "Some text here " + i;
//            card.rating = (float) (Math.random() * (5.0));

            //Only for test, change some icons
            if ((i % 6 == 0)) {
                card.resourceIdThumbnail = R.drawable.ic_ic_dh_bat;
            } else if ((i % 6 == 1)) {
                card.resourceIdThumbnail = R.drawable.ic_ic_dh_net;
            } else if ((i % 6 == 2)) {
                card.resourceIdThumbnail = R.drawable.ic_tris;
            } else if ((i % 6 == 3)) {
                card.resourceIdThumbnail = R.drawable.ic_info;
            } else if ((i % 6 == 4)) {
                card.resourceIdThumbnail = R.drawable.ic_smile;
            }

            if(results.length > 0) {

                int[] colors = new int[results.length * 4];

                int width = 50;
                int height = 50;

                int ix = 0;
                for(int c=0; c < results.length; c++)
                {
                    double[] hsv = results[c];
                    int[] rgb = PicHSBtoRGB(hsv[0], hsv[1], hsv[2]);
                    for(int p=0; p < 3; p++)
                    {
                        colors[ix++] = rgb[p];
                    }
                    //set the alpha!
                    colors[ix++] = 255;
                }

                Bitmap picture = Bitmap.createBitmap(colors, 0, width, width, height, Bitmap.Config.ARGB_8888);

                card.constructImage("fakeWID", picture);
//                card.init("fakeWID", picture);
            }
            return card;
        }
        //oops, we don't have an activity--just return null
        return null;
    }
    int[] PicHSBtoRGB(double h, double s, double v)
    {

        h = (h*6.0)%6.0;


        double r = 0.0, g = 0.0, b = 0.0;

        if(h < 0.0) h += 6.0;
        int hi = (int)Math.floor(h);
        double f = h - hi;

        double vs = v * s;
        double vsf = vs * f;

        double p = v - vs;
        double q = v - vsf;
        double t = v - vs + vsf;

        switch(hi) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: r = v; g = p; b = q; break;
        }

        return new int[]{(int)Math.floor(r*255),(int)Math.floor(g*255),(int)Math.floor(b*255)};
    }


    //this class handles returned calls from other threads
    //those other threads convert our artifact into a phenotype in the background
    //then return the final object for insertion into the UI system
    private final class SpicyPhenotypeRequestListener implements RequestListener<double[][]>, PendingRequestListener<double[][]> {


        @Override
        public void onRequestNotFound() {
            if(cardCallback != null)
                cardCallback.cardCreated(null);
        }

        @Override
        public void onRequestFailure(SpiceException spiceException) {

            if(cardCallback != null)
                cardCallback.cardCreated(null);
        }

        @Override
        public void onRequestSuccess(double[][] result) {

            if(cardCallback != null)
                cardCallback.cardCreated(convertArtifactToPhenotype(result));
        }
    }


}
