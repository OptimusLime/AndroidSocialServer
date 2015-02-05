package asynchronous.main;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;
import android.widget.GridView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhaarman.listviewanimations.swinginadapters.AnimationAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.AlphaInAnimationAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import asynchronous.interfaces.AsyncArtifactToUI;
import asynchronous.interfaces.AsyncFetchBitmaps;
import asynchronous.interfaces.AsyncInteractiveEvolution;
import bolts.Continuation;
import bolts.Task;
import cardUI.EndlessGridScrollListener;
import cardUI.StickyCardGridArrayAdapter;
import cardUI.StickyCardGridView;
import cardUI.cards.GridCard;
import dagger.ObjectGraph;
import eplex.win.winBackbone.Artifact;
import interfaces.PhenotypeCache;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.view.CardGridView;
import win.eplex.backbone.ArtifactCardCallback;
import win.eplex.backbone.R;

/**
 * Created by paul on 8/14/14.
 */
public class AsyncInfiniteIEC implements GridCard.GridCardButtonHandler, AsyncFetchBitmaps {

    JsonNode iecParams;

    StickyCardGridArrayAdapter mCardArrayAdapter;
    EndlessGridScrollListener mScrollListener;

    @Inject
    AsyncInteractiveEvolution evolution;

    @Inject
    AsyncArtifactToUI<Artifact, double[][], GridCard> asyncArtifactToUIMapper;

    @Inject
    PhenotypeCache<Bitmap> parentBitmapCache;

    @Inject
    PhenotypeCache<GridCard> gridCardCache;

    Map<String, Artifact> allArtifactMap = new HashMap<String, Artifact>();

    Activity activity;

    Activity getActivity() {
        return activity;
    }

    public AsyncInfiniteIEC()
    {

    }
    public void injectGraph(Activity act, ObjectGraph graph)
    {
        this.activity = act;

        //need to inject these individuals
        //that will create appropriate new evolution and async artifact objects
        graph.inject(this);

        //inject inside our evolution object as well
        graph.inject(evolution);

        //inject the inner workings of our async artifact-to-UI object
        graph.inject(asyncArtifactToUIMapper);
    }

    public Task<Void> asyncInitializeIECandUI(JsonNode params) {

        iecParams = params;
        if(iecParams == null) {

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jNode = mapper.createObjectNode();

            ObjectNode uiParams = mapper.createObjectNode();

            uiParams.set("width", mapper.convertValue(100, JsonNode.class));
            uiParams.set("height", mapper.convertValue(100 , JsonNode.class));

            jNode.set("ui", uiParams);

            ObjectNode uiParParams = mapper.createObjectNode();

            uiParams.set("width", mapper.convertValue(25, JsonNode.class));
            uiParams.set("height", mapper.convertValue(25 , JsonNode.class));

            jNode.set("parents", uiParParams);

            iecParams = jNode;
        }

        //first we initialize all our internal organs, so to speak
        initializeUI();

        //then we send off our evolution process to initialize itself!
        return evolution.asyncInitialize(params)
                .continueWithTask(new Continuation<Void, Task<Void>>() {
                      @Override
                      public Task<Void> then(Task<Void> task) throws Exception {

                          List<Artifact> allSeeds  = evolution.seeds();
                          for(Artifact seed : allSeeds)
                              allArtifactMap.put(seed.wid(), seed);

                          //start by fetching the minimal required for displaying -- 6/8 should do!
                          return asyncGetMoreCards(6);
                      }
                  });
    }

    //we've been summoned to fetch new card objects
    //this is the crucial function for async IEC
    //this means creating artifacts, eating their children, then turning them into cards for the UI
    //wait... what was that second thing?
    //Creating new artifacts?
    //no... that wasn't it...
    //OH! Turning the artifacts into Card UI objects
    //uhhhh.....
    Task<Void> asyncGetMoreCards(int count)
    {
        //we get some cards, async style!

        //create a bunch of children object, wouldn't you please?
        List<Artifact> offspring = evolution.createOffspring(count);

        //we're going to do a bunch of async conversions between artifacts and UI objects
        //that's the process of converting a genome into a phenotype into a UI object
        ArrayList<Task<Void>> tasks = new ArrayList<Task<Void>>();

        final GridCard.GridCardButtonHandler self = this;

        //now that we have our offspring, we go about our real businazzzz
        //lets make some cards ... biatch!
        for(Artifact a : offspring)
        {
            //artifact? ... check!
            allArtifactMap.put(a.wid(), a);

            //asynch convert artifact to UI Card Object? ... Check!
            tasks.add(asyncArtifactToUIMapper.asyncConvertArtifactToUI(getActivity(), a, iecParams.get("ui"))
                    .continueWith(new Continuation<GridCard, Void>() {
                        @Override
                        public Void then(Task<GridCard> task) throws Exception {

                            if(task.isCancelled())
                            {
                                throw new RuntimeException("Converting object to UI was cancelled!");
                            }
                            else if(task.isFaulted())
                            {
                                Toast.makeText(getActivity(), "Error creating card from Artifact", Toast.LENGTH_SHORT).show();
                                Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
                                throw task.getError();
                            }
                            //great success!
                            else
                            {
                                final GridCard c = task.getResult();

                                if (c != null) {

                                    gridCardCache.cachePhenotype(c.wid, c);
                                    c.setButtonHandler(self);

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            //add the card please!
                                            mCardArrayAdapter.add(c);
                                        }
                                    });


                                }
                            }

                            return null;
                        }
                    }));
        }

        //now that we've created all those promises, when they're all done,
        //we renotify that we're accepting more scrolls
        return Task.whenAll(tasks)
                .continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        //all done with this batch, be prepared to fetch more!
                        mScrollListener.notifyMorePages();
                        return null;
                    }
                });
    }

    void initializeUI()
    {
        mCardArrayAdapter = new StickyCardGridArrayAdapter(getActivity(), new ArrayList<Card>());

        mCardArrayAdapter.setBitmapCacheAndFetch(this);

        StickyCardGridView listView = (StickyCardGridView) getActivity().findViewById(R.id.carddemo_grid_base1);
        if (listView != null) {
//            listView.setAdapter(mCardArrayAdapter);
            setAlphaAdapter(listView);
//            listView.setAdapter(mCardArrayAdapter);

            //here we're going to set our scroll listener for creating more objects and appending them!
            mScrollListener = new EndlessGridScrollListener(listView);

            //lets set our callback item now -- this is called whenever the user scrolls to the bottom
            mScrollListener.setRequestItemsCallback(new EndlessGridScrollListener.RequestItemsCallback() {
                @Override
                public void requestItems(int pageNumber) {
                    System.out.println("On Refresh invoked..");

                    //add more cards, hoo-ray!!!

                    //every time it's the same process -- generate artifacts, convert to phenotype, display!
                    //rinse and repeat
                    asyncGetMoreCards(4);
                }
            });

            //make sure to add our infinite scroller here
            listView.setOnScrollListener(mScrollListener);
        }
    }

    /**
     * Alpha animation
     */
    private void setAlphaAdapter(StickyCardGridView gridView) {
        AnimationAdapter animCardArrayAdapter = new AlphaInAnimationAdapter(mCardArrayAdapter);
        animCardArrayAdapter.setAbsListView(gridView);

        mCardArrayAdapter.setExternalAdapter(animCardArrayAdapter);
        gridView.setAdapter(mCardArrayAdapter);

        //set the external adapter for our sticky mofo
//        gridView.setExternalAdapter(animCardArrayAdapter, mCardArrayAdapter);
    }

    //get all our parents please!
    @Override
    public List<String> syncRetrieveParents(String wid) {
        return allArtifactMap.get(wid).parents();
    }

    @Override
    public Bitmap syncRetrieveBitmap(String wid) {
        return parentBitmapCache.retrievePhenotype(wid);
    }

    @Override
    public Task<Map<String, Bitmap>> asyncFetchParentBitmaps(List<String> parents) {

        List<Task<Void>> promises = new ArrayList<Task<Void>>();

        final Map<String, Bitmap> parentsToImages = new HashMap<String, Bitmap>();
        for(String parent : parents)
        {
            Artifact a = allArtifactMap.get(parent);

            promises.add(asyncArtifactToUIMapper.asyncConvertArtifactToUI(getActivity(), a, iecParams.get("parents"))
                    .continueWith(new Continuation<GridCard, Void>() {
                        @Override
                        public Void then(Task<GridCard> task) throws Exception {

                            if(task.isCancelled())
                            {
                                throw new RuntimeException("Converting object to UI was cancelled!");
                            }
                            else if(task.isFaulted())
                            {
//                                Toast.makeText(getActivity(), "Error creating card from Artifact", Toast.LENGTH_SHORT).show();
                                Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
                                throw task.getError();
                            }
                            //great success!
                            else
                            {
                                GridCard gc = task.getResult();

                                //convert to circular view
                                Bitmap circular = getCircularBitmap(gc.getThumbnailBitmap());

                                //grab the bitmap from our fetching process!
                                parentsToImages.put(gc.wid, circular);

                                //put in our cache for future retrieval!
                                parentBitmapCache.cachePhenotype(gc.wid, circular);
                            }
                            return null;
                        }
                    }));
        }

        return Task.whenAll(promises)
                .continueWith(new Continuation<Void, Map<String, Bitmap>>() {
                    @Override
                    public Map<String, Bitmap> then(Task<Void> task) throws Exception {
                        //all done, return the parent ot bitmap mapping!
                        return parentsToImages;
                    }
                });

    }




    @Override
    public boolean handleLike(String wid, boolean like) {

        //here we handle likes -- that means we've selected a parent!
        boolean weLikeObject = !like;

        if(weLikeObject)
        {
            //select the new parent, please!
            evolution.selectParents(Arrays.asList(wid));


            //cache the parent object bitmap!
            GridCard gc =  gridCardCache.retrievePhenotype(wid);

            //cache the parent's bitmap for future retrieval!
            parentBitmapCache.cachePhenotype(gc.wid, getCircularBitmap(gc.getThumbnailBitmap()));
        }
        else
        {
            //please remove this parent, we don't like them anymore :(
            evolution.unselectParents(Arrays.asList(wid));
        }

        //toggle like after we're done!
        return weLikeObject;
    }

    /**
     * This class takes a rectangular bitmap and gives a circular bitmap back.
     * Can be used for profile pictures or other kind of images.
     * from the internet: http://blog.fabgate.co/growth-hacking-for-android-let-the-user-invite-her-friends-part-1/
     *
     *
     * @return circular bitmap
     */
    public Bitmap getCircularBitmap(Bitmap bitmap) {
        Bitmap output = null;
        if (bitmap != null) {

            if (bitmap.getWidth() > bitmap.getHeight()) {
                output = Bitmap.createBitmap(bitmap.getHeight(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            } else {
                output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(), Bitmap.Config.ARGB_8888);
            }

            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

            float r = 0;

            if (bitmap.getWidth() > bitmap.getHeight()) {
                r = bitmap.getHeight() / 2;
            } else {
                r = bitmap.getWidth() / 2;
            }

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawCircle(r, r, r, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
        }
        return output;
    }

    @Override
    public void handlePublish(String wid) {
        Toast.makeText(getActivity(), "We want to publish something!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean handleFavorite(String wid, boolean favorite) {

        boolean weFavoriteObject = !favorite;

        //we want to favorite this for the user!
        Toast.makeText(getActivity(), weFavoriteObject ? "We like it!" : "We stopped liking it!", Toast.LENGTH_SHORT).show();

        return weFavoriteObject;
    }

    @Override
    public void handleInspect(String wid) {
        Toast.makeText(getActivity(), "We want to inspect something!", Toast.LENGTH_SHORT).show();
    }

}
