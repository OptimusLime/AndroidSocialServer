package cppn.implementations;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.IntBuffer;
import java.util.concurrent.Callable;

import asynchronous.interfaces.AsyncPhenotypeToUI;
import bolts.Task;
import cardUI.cards.GridCard;
import it.gmariotti.cardslib.library.internal.Card;
import win.eplex.backbone.R;

/**
 * Created by paul on 8/14/14.
 */
public class AsyncCPPNOutputToCard implements AsyncPhenotypeToUI<double[][], GridCard> {

    //need to know the activity for creating the UI
    @Override
    public Task<GridCard> asyncPhenotypeToUI(final Activity a, final String wid, final double[][] phenotype, final JsonNode params) {

        return Task.callInBackground(new Callable<GridCard>() {
            @Override
            public GridCard call() throws Exception {
                return convertArtifactToPhenotype(a, wid, phenotype, params);
            }
        });
    }

    private GridCard convertArtifactToPhenotype(Activity activity, final String wid, double[][] results, final JsonNode params)
    {
        //now that we've got the juice, let's make ourselves useful, eh?
        //we should make a card with this info
        if(activity != null)
        {

            GridCard card = new GridCard(activity);

            int i = (int)Math.floor(Math.random()*Integer.MAX_VALUE);

//            card.headerTitle = "App example " + i;
//            card.secondaryTitle = "Some text here " + i;
//            card.rating = (float) (Math.random() * (5.0));

            //Only for test, change some icons
//            if ((i % 6 == 0)) {
//                card.resourceIdThumbnail = R.drawable.ic_ic_dh_bat;
//            } else if ((i % 6 == 1)) {
//                card.resourceIdThumbnail = R.drawable.ic_ic_dh_net;
//            } else if ((i % 6 == 2)) {
//                card.resourceIdThumbnail = R.drawable.ic_tris;
//            } else if ((i % 6 == 3)) {
//                card.resourceIdThumbnail = R.drawable.ic_info;
//            } else if ((i % 6 == 4)) {
//                card.resourceIdThumbnail = R.drawable.ic_smile;
//            }


            if(results.length > 0) {

                int[] colors = new int[results.length];

                int width = params.get("width").asInt();
                int height = params.get("height").asInt();

                int ix = 0;
                for(int c=0; c < results.length; c++)
                {
                    double[] hsv = results[c];
                    int[] rgb = PicHSBtoRGB(hsv[0], clampZeroOne(hsv[1]), Math.abs(hsv[2]));

                    //set the color from our network outputs
                    colors[ix++] = Color.argb(255, rgb[0], rgb[1], rgb[2]);
                }
                // You are using RGBA that's why Config is ARGB.8888
                Bitmap picture = Bitmap.createBitmap(colors, 0, width, width, height, Bitmap.Config.ARGB_8888);

                card.constructImage(wid, picture);
            }
            return card;
        }
        //oops, we don't have an activity--just return null
        return null;
    }

    private double clampZeroOne(double val)
    {
        return Math.max(0.0, Math.min(val,1.0));
    }


    private IntBuffer makeBuffer(int[] src, int n) {
        IntBuffer dst = IntBuffer.allocate(n);
        for (int i = 0; i < n; i++) {
            dst.put(src[i]);
        }
        dst.rewind();
        return dst;
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

}
