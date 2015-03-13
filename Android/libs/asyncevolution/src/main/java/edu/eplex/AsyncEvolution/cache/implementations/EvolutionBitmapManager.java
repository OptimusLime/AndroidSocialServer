package edu.eplex.AsyncEvolution.cache.implementations;

import android.graphics.Bitmap;

/**
 * Created by paul on 2/20/15.
 */
//singleton to hold all the bitmaps across the code
public class EvolutionBitmapManager {

    private static EvolutionBitmapManager instance = null;
    protected EvolutionBitmapManager() {
        // Exists only to defeat instantiation.
    }

    public static EvolutionBitmapManager getInstance() {
        if(instance == null) {
            instance = new EvolutionBitmapManager();
        }
        return instance;
    }

    int cacheSize = 100;
    LRUBitmapCache<Bitmap> bitmapCache = new LRUBitmapCache<>(cacheSize);
    LRUBitmapCache<int[]> pixelCache = new LRUBitmapCache<>(cacheSize);


    public int[] getBitmapPixels(String wid)
    {
        return pixelCache.retrievePhenotype(wid);
    }

    public Bitmap getBitmap(String wid)
    {
        return bitmapCache.retrievePhenotype(wid);
    }

    public void setBitmap(String wid, Bitmap bit)
    {
        bitmapCache.cachePhenotype(wid, bit);

        int truePixelWidth =bit.getWidth();
        int truePixelHeight= bit.getHeight();
        int[] pixels = new int[truePixelWidth*truePixelHeight];

        bit.getPixels(pixels, 0, truePixelWidth, 0, 0,
                truePixelWidth, truePixelHeight);


        //store only one copy of the bitmap pixels -- everybody can access it! It's read only derrrr
        pixelCache.cachePhenotype(wid, pixels);
    }

}
