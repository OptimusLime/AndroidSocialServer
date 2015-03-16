package edu.eplex.androidsocialclient.MainUI.Cache;

import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;

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

    //need to lazy load images
    public void lazyLoadBitmap(String url, int squareSize, LazyLoadedCallback callback) {
        lazyLoadBitmap(url, squareSize, squareSize, callback);
    }

    //finish it up
    void completeCallbacks(final String url, final String cachedURL, final Bitmap bitmap)
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
                        callbacks.get(i).imageLoaded(url, bitmap);
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
                }

                //all done

                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);
    }

    void finishLazyLoad(final String url, final int width, final int height, final String cached)
    {
        //okay, we know what to do -- load the bitmap from the factory on the background thread
        //okay, we know what to do -- load the bitmap from the factory on the background thread
        Task.callInBackground(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception{

                if (width == height) {
                    return FileUtilities.decodeSampledBitmapFromResource(url, width);
                } else
                    return FileUtilities.decodeSampledBitmapFromResource(url, width, height);
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
                    completeCallbacks(url, cached, bm);
                }

                return null;
            }
        });

    }

    public void lazyLoadBitmap(final String url, int width,  int height, final LazyLoadedCallback callback)
    {
        //what is the cache name?
        final String cached = urlCacheName(url, width, height);

        synchronized (allCached)
        {
            if(allCached.contains(cached))
            {
                //exists -- just send it back later on the UI thread
                Task.call(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {

                        callback.imageLoaded(url, cachedBitmaps.get(cached));
                        return null;
                    }
                });

                return;
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

        //start the lazy loading -- let me know when you're done
        finishLazyLoad(url, width, height, cached);
    }

    String urlCacheName(String url, int width, int height)
    {
        if(width == height)
            return url + "_square_"+width;
        else
            return url + "_w_" + width + "_h_" + height;
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
