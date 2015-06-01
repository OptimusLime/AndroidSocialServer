package edu.eplex.androidsocialclient.MainUI;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nhaarman.listviewanimations.swinginadapters.AnimationAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.AlphaInAnimationAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import bolts.Continuation;
import bolts.Task;
import dagger.ObjectGraph;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncArtifactToUI;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncFetchBitmaps;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncInteractiveEvolution;
import edu.eplex.AsyncEvolution.backbone.NEATArtifact;
import edu.eplex.AsyncEvolution.cache.implementations.EvolutionBitmapManager;
import edu.eplex.AsyncEvolution.cardUI.EndlessGridScrollListener;
import edu.eplex.AsyncEvolution.cardUI.ExternalCardGridArrayAdapter;
import edu.eplex.AsyncEvolution.cardUI.StickyCardGridArrayAdapter;
import edu.eplex.AsyncEvolution.cardUI.StickyCardGridView;
import edu.eplex.AsyncEvolution.cardUI.cards.GridCard;
import edu.eplex.AsyncEvolution.interfaces.PhenotypeCache;
import edu.eplex.AsyncEvolution.main.NEATInitializer;
import edu.eplex.AsyncEvolution.views.HorizontalListView;
import edu.eplex.androidsocialclient.GPU.GPUNetworkFilter;
import edu.eplex.androidsocialclient.MainUI.Adapters.BitmapAdapter;
import edu.eplex.androidsocialclient.R;
import eplex.win.FastCPPNJava.network.CPPN;
import eplex.win.FastNEATJava.decode.DecodeToFloatFastConcurrentNetwork;
import eplex.win.FastNEATJava.genome.NeatGenome;
import eplex.win.FastNEATJava.utils.NeatParameters;
import eplex.win.winBackbone.Artifact;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.sephiroth.android.library.widget.AbsHListView;
import it.sephiroth.android.library.widget.HListView;

/**
 * Created by paul on 3/5/15.
 */
public class IECFilters extends Fragment implements GridCard.GridCardButtonHandler {
    //Tag for logging
    private static final String TAG = "IECFilters";

    JsonNode iecParams;

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

    LayoutParams params;
    LinearLayout next, prev;
    int viewWidth;
    GestureDetector gestureDetector = null;
    HListView horizontalListView;
    BitmapAdapter filterImageAdapter;
    Map<Integer, String> filterArrayIndex = new HashMap<Integer, String>();

    LayoutParams mainImageSize;
    ImageView mainImageView;

    boolean fetchingMoreFilters = false;
    ArrayList<LinearLayout> layouts;
    int parentLeft, parentRight;
    int mWidth;
    int currPosition, prevPosition;

    //This is the menu options handling
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        getActivity().getMenuInflater().inflate(R.menu.menu_account_settings, menu);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setContentView(R.layout.fragment_iec_filters);

//        prev = (LinearLayout)  getActivity().findViewById(R.id.prev);
//        next = (LinearLayout) getActivity().findViewById(R.id.next);
        horizontalListView = (HListView) getActivity().findViewById(R.id.hsvFilters);
        gestureDetector = new GestureDetector(new MyGestureDetector());


//        next.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new Handler().postDelayed(new Runnable() {
//                    public void run() {
//                        horizontalListView.scrollTo(
//                                (int) horizontalListView.getScrollX()
//                                        + viewWidth,
//                                (int) horizontalListView.getScrollY());
//                    }
//                }, 100L);
//            }
//        });
//        prev.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new Handler().postDelayed(new Runnable() {
//                    public void run() {
//                        horizontalListView.scrollTo(
//                                (int) horizontalListView.getScrollX()
//                                        - viewWidth,
//                                (int) horizontalListView.getScrollY());
//                    }
//                }, 100L);
//            }
//        });
//        horizontalScrollView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (gestureDetector.onTouchEvent(event)) {
//                    return true;
//                }
//                return false;
//            }
//        });

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        String evoCacheName = getActivity().getResources().getString(R.string.evolution_cache_name);

//        LinearLayout ll = (LinearLayout) getActivity().findViewById(R.id.filterselection);



        int minWidth = Math.min(width, height);

        LayoutParams outerLayout = new LayoutParams(minWidth, minWidth);
        LayoutParams innerLayout = new LayoutParams(minWidth - 20, minWidth - 20);

        LinearLayout mainImage = (LinearLayout)getActivity().findViewById(R.id.filter_main_image);
        LinearLayout mainImageHolder = (LinearLayout)getActivity().findViewById(R.id.filter_main_image_holder);
        mainImageView = (ImageView)getActivity().findViewById(R.id.filter_main_image_view);

        mainImage.setLayoutParams(outerLayout);
        mainImageHolder.setLayoutParams(outerLayout);

        Bitmap inputImage = EvolutionBitmapManager.getInstance().getBitmap(evoCacheName);

        mainImageSize = innerLayout;
        mainImageView.setLayoutParams(mainImageSize);
        mainImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mainImageView.setImageBitmap(inputImage);


//        mainImageView = (ImageView)getFilterPhoto(evoCacheName, outerLayout, innerLayout);
        //add in the main item
//        mainImage.addView(mainImageView);

//        //add 5 of the same images
//        for(int i=0; i < 5; i++)
//            ll.addView(getFilterPhoto(evoCacheName, null, null));

        //here we're going to set our scroll listener for creating more objects and appending them!
        mScrollListener = new EndlessGridScrollListener(horizontalListView);

//        //lets set our callback item now -- this is called whenever the user scrolls to the bottom
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
//
//        //make sure to add our infinite scroller here
        horizontalListView.setOnScrollListener(mScrollListener);

        //initilaize!
        asyncInitializeIECandUI(getActivity());

    }

    View getFilterPhoto(String evoCacheName, @Nullable LayoutParams outerLayout, @Nullable LayoutParams innerLayout, int imageID){
        Bitmap inputImage = EvolutionBitmapManager.getInstance().getBitmap(evoCacheName);

        LinearLayout layout = new LinearLayout(getActivity());

        if(outerLayout == null)
            outerLayout = new LayoutParams(250, 250);


        layout.setLayoutParams(outerLayout);
        layout.setGravity(Gravity.CENTER);

        ImageView imageView = new ImageView(getActivity());
        if(innerLayout == null)
            innerLayout = new LayoutParams(220, 220);

        imageView.setLayoutParams(innerLayout);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageBitmap(inputImage);
        if(imageID != -1)
            imageView.setId(imageID);


        layout.addView(imageView);
        return layout;
    }

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            if (e1.getX() < e2.getX()) {
                currPosition = getVisibleViews("left");
            } else {
                currPosition = getVisibleViews("right");
            }

            horizontalListView.scrollTo(layouts.get(currPosition)
                    .getLeft(), 0);
            return true;
        }
    }
    public int getVisibleViews(String direction) {
        Rect hitRect = new Rect();
        int position = 0;
        int rightCounter = 0;
        for (int i = 0; i < layouts.size(); i++) {
            if (layouts.get(i).getLocalVisibleRect(hitRect)) {
                if (direction.equals("left")) {
                    position = i;
                    break;
                } else if (direction.equals("right")) {
                    rightCounter++;
                    position = i;
                    if (rightCounter == 2)
                        break;
                }
            }
        }
        return position;
    }



//    @Override
//    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        //root view -- camera fragment
//        View rootView = inflater.inflate(R.layout.fragment_camera_home, container, false);
//
//        //register our toolbar with the activity -- so we get callbacks and stuff
//        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.cameraHomeToolbar);
//        ActionBarActivity abActivity = ((ActionBarActivity)getActivity());
//        abActivity.setSupportActionBar(toolbar);
////        abActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        abActivity.getSupportActionBar().setTitle(getResources().getString(R.string.ch_toolbar_title));
//
//        //confirm we want options callback
//        setHasOptionsMenu(true);
//
//        UserSessionManager.getInstance().register(this, this);
//
//        //return the inflated root view
//        return rootView;
//    }

    public void injectGraph(Activity act, ObjectGraph graph) {
        //need to inject these individuals
        //that will create appropriate new evolution and async artifact objects
        graph.inject(this);

        //inject inside our evolution object as well
        graph.inject(evolution);

        //inject the inner workings of our async artifact-to-UI object
        graph.inject(asyncArtifactToUIMapper);
    }

    public static ObjectNode DefaultParams(FragmentActivity activity)
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jNode = mapper.createObjectNode();

        ObjectNode uiParams = mapper.createObjectNode();

        uiParams.set("width", mapper.convertValue(100, JsonNode.class));
        uiParams.set("height", mapper.convertValue(100 , JsonNode.class));

        jNode.set("ui", uiParams);

        ObjectNode uiParParams = mapper.createObjectNode();

        uiParams.set("width", mapper.convertValue(150, JsonNode.class));
        uiParams.set("height", mapper.convertValue(150, JsonNode.class));

        jNode.set("parents", uiParParams);

        return jNode;
    }

    public void InitializeParameters(JsonNode params)
    {
        //save params -- even if null
        iecParams = params;
    }

    Task<Void> asyncInitializeIECandUI(FragmentActivity activity) {

        //don't have any params? MAKE SOME
        if(iecParams == null)
            iecParams = DefaultParams(activity);

        //first we initialize all our internal organs, so to speak
        initializeUI(activity);

        //then we send off our evolution process to initialize itself!
        return evolution.asyncInitialize(iecParams,null)
                .continueWithTask(new Continuation<Void, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<Void> task) throws Exception {

                        List<Artifact> allSeeds = evolution.seeds();
                        for (Artifact seed : allSeeds)
                            allArtifactMap.put(seed.wid(), seed);

                        //start by fetching the minimal required for displaying -- 6/8 should do!
                        return asyncGetMoreCards(6);
                    }
                });
    }


    Task<Void> CreateCardTask(final Artifact selected, Bitmap inputImage)
    {
        //create filter object
        GPUNetworkFilter gpuNetworkFilter = new GPUNetworkFilter();

        return gpuNetworkFilter.AsyncFilterBitmapGPU(getActivity(), inputImage, selected, iecParams.get("ui"))
                .continueWith(new Continuation<Bitmap, Void>() {
                    @Override
                    public Void then(Task<Bitmap> task) throws Exception {

                        if (task.isCancelled()) {
                            throw new RuntimeException("Converting object to UI was cancelled!");
                        } else if (task.isFaulted()) {
                            Toast.makeText(getActivity(), "Error creating card from Artifact", Toast.LENGTH_SHORT).show();
                            Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
                            throw task.getError();
                        }
                        //great success!
                        else {

                            final Bitmap filtered = task.getResult();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    Integer ix = filterImageAdapter.getCount();
                                    filterArrayIndex.put(ix, selected.wid());
                                    //add the card please!
                                    filterImageAdapter.add(filtered);
                                }
                            });

                        }
                        return null;
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
        if(fetchingMoreFilters)
            return Task.call(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    return null;
                }
            });
        //we get some cards, async style!

        fetchingMoreFilters = true;

        //create a bunch of children object, wouldn't you please?
        List<Artifact> offspring = evolution.createOffspring(count);

        //we're going to do a bunch of async conversions between artifacts and UI objects
        //that's the process of converting a genome into a phenotype into a UI object
        ArrayList<Task<Void>> tasks = new ArrayList<Task<Void>>();

        final GridCard.GridCardButtonHandler self = this;

        //original image plzzzzz -- use the small one!
        String imageName = getActivity().getResources().getString(R.string.evolution_small_cache_name);
        Bitmap inputImage = EvolutionBitmapManager.getInstance().getBitmap(imageName);

        //now that we have our offspring, we go about our real businazzzz
        //lets make some cards ... biatch!
        for(Artifact a : offspring)
        {
            //artifact? ... check!
            allArtifactMap.put(a.wid(), a);


            tasks.add(CreateCardTask(a, inputImage));


//            tasks.add(asyncArtifactToUIMapper.asyncConvertArtifactToUI(getActivity(), a, iecParams.get("ui"))
//                    .continueWith(new Continuation<GridCard, Void>() {
//                        @Override
//                        public Void then(Task<GridCard> task) throws Exception {
//
//                            if(task.isCancelled())
//                            {
//                                throw new RuntimeException("Converting object to UI was cancelled!");
//                            }
//                            else if(task.isFaulted())
//                            {
//                                Toast.makeText(getActivity(), "Error creating card from Artifact", Toast.LENGTH_SHORT).show();
//                                Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
//                                throw task.getError();
//                            }
//                            //great success!
//                            else
//                            {
//                                final GridCard c = task.getResult();
//
//                                if (c != null) {
//
//                                    gridCardCache.cachePhenotype(c.wid, c);
//                                    c.setButtonHandler(self);
//
//                                    getActivity().runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//
//                                            Integer ix = filterImageAdapter.getCount();
//                                            filterArrayIndex.put(ix, c.wid);
//                                            //add the card please!
//                                            filterImageAdapter.add(c.getThumbnailBitmap());
//                                        }
//                                    });
//
//
//                                }
//                            }
//
//                            return null;
//                        }
//                    }));
        }

        //now that we've created all those promises, when they're all done,
        //we renotify that we're accepting more scrolls
        return Task.whenAll(tasks)
                .continueWith(new Continuation<Void, Void>() {
                    @Override
                    public Void then(Task<Void> task) throws Exception {
                        //all done with this batch, be prepared to fetch more!
                        mScrollListener.notifyMorePages();

                        //all done, thanks
                        fetchingMoreFilters = false;
                        return null;
                    }
                });
    }

    void initializeUI(FragmentActivity activity)
    {
        //no images to start with, we will get those asynchronously
        filterImageAdapter = new BitmapAdapter(activity, new ArrayList<Bitmap>(), new BitmapAdapter.OnFilterSelected() {
            @Override
            public void longSelectImageAtIndex(int ix) {
                //TIME FOR EVOLUTION!
                Artifact selected = allArtifactMap.get(filterArrayIndex.get(ix));

                Bitmap existing = filterImageAdapter.getItem(ix);


                setSelectedIndexAsMainImage(ix);

                evolution.clearParents();
                evolution.selectParents(Arrays.asList(selected.wid()));

                //clear it out, then fill it up!
                filterImageAdapter.clear();
                filterArrayIndex.clear();

                //we keep around our original selection -- kind of like elitism
                filterImageAdapter.add(existing);
                filterArrayIndex.put(0, selected.wid());

                //go get more please!
                asyncGetMoreCards(5);
            }

            @Override
            public void selectImageAtIndex(int ix) {

                setSelectedIndexAsMainImage(ix);

            }
        });

//        filterImageAdapter = new ExternalCardGridArrayAdapter(getActivity(), null);

        //set the damn adapter -- we'll figure out fancy thigns later
        horizontalListView.setAdapter(filterImageAdapter);
//        setAlphaAdapter(horizontalListView);

//        mCardArrayAdapter.setBitmapCacheAndFetch(this);

    }


    void setSelectedIndexAsMainImage(int ix)
    {
        //now we have to install in the main image!
        Artifact selected = allArtifactMap.get(filterArrayIndex.get(ix));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode uiParams = mapper.createObjectNode();

        uiParams.set("width", mapper.convertValue(170, JsonNode.class));//(int)(Math.round(mainImageSize.width/3.0)), JsonNode.class));
        uiParams.set("height", mapper.convertValue(170, JsonNode.class));//(int)(Math.round(mainImageSize.height/3.0)), JsonNode.class));

        String imageName = getActivity().getResources().getString(R.string.evolution_cache_name);
        uiParams.set("image", mapper.convertValue(imageName, JsonNode.class));

        //original image plzzzzz
        Bitmap inputImage = EvolutionBitmapManager.getInstance().getBitmap(imageName);

        //create filter object
        GPUNetworkFilter gpuNetworkFilter = new GPUNetworkFilter();

        gpuNetworkFilter.AsyncFilterBitmapGPU(getActivity(), inputImage, selected, uiParams)
                .continueWith(new Continuation<Bitmap, Void>() {
                    @Override
                    public Void then(Task<Bitmap> task) throws Exception {

                        if (task.isCancelled()) {
                            throw new RuntimeException("Converting object to UI was cancelled!");
                        } else if (task.isFaulted()) {
                            Toast.makeText(getActivity(), "Error creating card from Artifact", Toast.LENGTH_SHORT).show();
                            Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
                            throw task.getError();
                        }
                        //great success!
                        else {

                            final Bitmap filtered = task.getResult();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    //grab the image from the card -- hack for now
                                    mainImageView.setImageBitmap(filtered);
                                }
                            });
                        }
                        return null;
                    }
                });
        //now convert selected into a BIG image plz
//        asyncArtifactToUIMapper.asyncConvertArtifactToUI(getActivity(), selected, uiParams)
//                .continueWith(new Continuation<GridCard, Void>() {
//                    @Override
//                    public Void then(Task<GridCard> task) throws Exception {
//
//                        if (task.isCancelled()) {
//                            throw new RuntimeException("Converting object to UI was cancelled!");
//                        } else if (task.isFaulted()) {
//                            Toast.makeText(getActivity(), "Error creating card from Artifact", Toast.LENGTH_SHORT).show();
//                            Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
//                            throw task.getError();
//                        }
//                        //great success!
//                        else {
//                            final GridCard c = task.getResult();
//
//                            getActivity().runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//
//                                    //grab the image from the card -- hack for now
//                                    mainImageView.setImageBitmap(c.getThumbnailBitmap());
//                                }
//                            });
//
//
//                        }
//
//                        return null;
//                    }
//                });
    }

    /**
     * Alpha animation
     */
//    private void setAlphaAdapter(AbsHListView gridView) {
//        AnimationAdapter animCardArrayAdapter = new AlphaInAnimationAdapter(filterImageAdapter);
//        animCardArrayAdapter.setAbsListView(gridView);
//
//        filterImageAdapter.setExternalAdapter(animCardArrayAdapter);
//        gridView.setAdapter(filterImageAdapter);
//
//        //set the external adapter for our sticky mofo
////        gridView.setExternalAdapter(animCardArrayAdapter, mCardArrayAdapter);
//    }

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
