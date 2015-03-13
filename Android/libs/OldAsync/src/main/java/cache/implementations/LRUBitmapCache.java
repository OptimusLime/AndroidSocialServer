package cache.implementations;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.fasterxml.jackson.databind.annotation.JsonNaming;

import interfaces.PhenotypeCache;

/**
 * Created by paul on 8/20/14.
 */
public class LRUBitmapCache<T> implements PhenotypeCache<T> {

    LruCache<String, T> lruCache;

    public LRUBitmapCache(int maxCacheSize)
    {
        //hold 200 bitmaps?
        lruCache = new LruCache<String, T>(maxCacheSize);
    }

    @Override
    public void cachePhenotype(String wid, T phenotype) {

        lruCache.put(wid, phenotype);
    }

    @Override
    public T retrievePhenotype(String wid) {
        return lruCache.get(wid);
    }
}
