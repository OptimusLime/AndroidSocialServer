package edu.eplex.androidsocialclient.MainUI.Cache;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.squareup.picasso.Transformation;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import edu.eplex.androidsocialclient.MainUI.Adapters.WorkshopCompositeAdapter;
import edu.eplex.androidsocialclient.Utilities.FileUtilities;

/**
 * Created by paul on 3/15/15.
 */
public class BitmapCacheManager {

    final HashMap<String, ArrayList<LazyLoadedCallback>> callbacksWaiting = new HashMap<>();
    final HashMap<String, Bitmap> cachedBitmaps = new HashMap<>();
    final HashSet<String> allCached = new HashSet<>();

    public interface LazyLoadedCallback
    {
        void imageLoaded(String url, Bitmap bitmap);
        void imageLoadFailed(String reason, Exception e);
    }

    private static BitmapCacheManager instance = null;
    protected BitmapCacheManager() {
        // Exists only to defeat instantiation.
    }

    public static BitmapCacheManager getInstance() {
        if(instance == null) {
            instance = new BitmapCacheManager();
        }
        return instance;
    }

    //we have an image we want replaced
//    public void replaceCachedBitmap(String url, int width, int height, Bitmap replace)
//    {
//        replaceCachedBitmap(url, width, height, replace, false);
//    }
    public void replaceCachedBitmap(String url, int width, int height,boolean isFiltered, Bitmap replace)
    {
        //cccccccache name plz?
        String cacheName = urlCacheName(url, width, height, isFiltered);

        //do we need to replace anything?
        Bitmap bm = null;

        //need to do this safely please
        synchronized (allCached)
        {
            if(allCached.contains(cacheName))
            {
                bm = cachedBitmaps.get(cacheName);
                //replace in the cache
                cachedBitmaps.put(cacheName, replace);
            }
        }

        //clear out our bitmap -- it's removed
        if(bm != null && !bm.isRecycled())
            bm.recycle();
    }

    //need to lazy load images
    public void lazyLoadBitmap(Context mContext, String uriPath, int squareSize, boolean filtered, LazyLoadedCallback callback) throws FileNotFoundException {
        lazyLoadBitmap(mContext, uriPath, squareSize, squareSize, filtered, callback);
    }

    //finish it up
    void completeCallbacks(final String cachedURL, final Bitmap bitmap)
    {
        synchronized (cachedBitmaps)
        {
            cachedBitmaps.put(cachedURL, bitmap);
            allCached.add(cachedURL);
        }

        //send bitmaps to callbacks on the UI thread
        Task.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                ArrayList<LazyLoadedCallback> callbacks;

                synchronized (callbacksWaiting) {

                    callbacks = callbacksWaiting.get(cachedURL);

                    for (int i = 0; i < callbacks.size(); i++) {
                        callbacks.get(i).imageLoaded(cachedURL, bitmap);
                    }

                    //need to remove the key -- sync it up
                    callbacksWaiting.remove(cachedURL);
                }

                //all done -- removed the callbacks waiting after satisfying the callbacks
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    void errorCallbacks(final String cachedURL, final String reason, final Exception e)
    {
        Task.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                synchronized (callbacksWaiting) {
                    ArrayList<LazyLoadedCallback> callbacks = callbacksWaiting.get(cachedURL);

                    for (int i = 0; i < callbacks.size(); i++) {
                        callbacks.get(i).imageLoadFailed(reason, e);
                    }

                    //need to remove the key -- sync it up
                    callbacksWaiting.remove(cachedURL);
                    //make sure we don't keep a reference to any mutant objects that failed!
                    allCached.remove(cachedURL);
                    cachedBitmaps.remove(cachedURL);
                }

                //all done

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    void finishLazyLoad(final ContentResolver cr, final Uri uri, final int width, final int height, final String cached)
    {
        //okay, we know what to do -- load the bitmap from the factory on the background thread
        //okay, we know what to do -- load the bitmap from the factory on the background thread
        Task.callInBackground(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception{

                if (width == height) {
                    return FileUtilities.decodeSampledBitmapFromResource(cr, uri, width);
                } else
                    return FileUtilities.decodeSampledBitmapFromResource(cr, uri, width, height);
                }
        })
        .continueWith(new Continuation<Bitmap, Object>() {
            @Override
            public Object then(Task<Bitmap> task) throws Exception {

                if (task.isFaulted() || task.isCancelled()) {
                    //error loading bitmap! ABORT
                    errorCallbacks(cached, (task.isFaulted() ? "Failed to load. Check error." : "Cancelled"), task.getError());
                } else if (task.getResult() == null) { //null result and we weren't cancelled -- just end it
                    errorCallbacks(cached, "Failed to load.", task.getError());
                } else {
                    Bitmap bm = task.getResult();

                    //auto crop if we need to
                    if (width == height)
                        bm = new CropSquareTransformation().transform(bm);

                    //now we need to complete on the UI thread -- because that's how we do here
                    completeCallbacks(cached, bm);
                }

                return null;
            }
        });

    }

    public void lazyLoadBitmap(final Context mContext, final String uriPath, int width,  int height, boolean filtered, final LazyLoadedCallback callback) throws FileNotFoundException {
        //what is the cache name?
        final String cached = urlCacheName(uriPath, width, height, filtered);

        synchronized (allCached)
        {
            if(allCached.contains(cached))
            {
                Bitmap bm = cachedBitmaps.get(cached);

                if(!bm.isRecycled()) {
                    //exists -- just send it back later on the UI thread
                    Task.call(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {

                            callback.imageLoaded(cached, cachedBitmaps.get(cached));
                            return null;
                        }
                    });

                    return;
                }
            }
        }

        //if we were in the cache, we would have started the return by now
        //Therefore, we don't have it in the cache

        //are we currently loading it?
        synchronized (callbacksWaiting)
        {
            //if the object is here -- then we're already loading anyways
            if(callbacksWaiting.containsKey(cached))
            {
                //we will let you know when complete
                callbacksWaiting.get(cached).add(callback);
                return;
            }

            //otherwise, we aren't in the process of loading it -- and it's definitely not cached --
            //lets add it

            //we need to lazy load -- nobody is messing with this right now -- because only one thread can claim it at a time
            //it's synched above
            ArrayList<LazyLoadedCallback> lazyLoadedCallbacks = new ArrayList<>();
            callbacksWaiting.put(cached, lazyLoadedCallbacks);
            lazyLoadedCallbacks.add(callback);
        }

        //we've made it this far, we can only be the thread that added -- therefore no more blocking necessary
        //load up the resolver here
        ContentResolver cr = mContext.getContentResolver();
//        InputStream in = cr.openInputStream();

        //start the lazy loading -- let me know when you're done
        finishLazyLoad(cr, Uri.parse(uriPath), width, height, cached);
    }

    String urlCacheName(String url, int width, int height)
    {
        return urlCacheName(url, width,height, false);
    }
    String urlCacheName(String url, int width, int height, boolean filtered)
    {
        if(width == height)
            return url + "_square_" + width + (filtered ? "_filtered" : "");
        else
            return url + "_w_" + width + "_h_" + height + (filtered ? "_filtered" : "");
    }


    public class CropSquareTransformation implements Transformation {
        @Override public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;
            Bitmap result = Bitmap.createBitmap(source, x, y, size, size);
            if (result != source) {
                source.recycle();
            }
            return result;
        }

        @Override public String key() { return "square()"; }
    }


}
