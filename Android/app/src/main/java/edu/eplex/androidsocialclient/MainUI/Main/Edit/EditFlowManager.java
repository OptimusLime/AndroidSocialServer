package edu.eplex.androidsocialclient.MainUI.Main.Edit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.otto.Bus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import dagger.ObjectGraph;
import edu.eplex.AsyncEvolution.main.NEATInitializer;
import edu.eplex.androidsocialclient.GPU.GPUNetworkFilter;
import edu.eplex.androidsocialclient.MainUI.Cache.BitmapCacheManager;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.Evolution.FilterEvolutionInjectModule;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.MainEditScreen;
import edu.eplex.androidsocialclient.R;
import eplex.win.FastNEATJava.utils.NeatParameters;

/**
 * Created by paul on 3/16/15.
 */
public class EditFlowManager {

    //what tab are we??? How do the tabs behave for switching?
    public enum EditID
    {
        Feed,
        Search,
        Camera,
        Workshop,
        User
    }

    HashSet<Object> registered = new HashSet<>();
    MainEditScreen mainEditActivity;
    Bus uiEventBus;

    private static EditFlowManager instance = null;
    protected EditFlowManager() {
        // Exists only to defeat instantiation.

        uiEventBus = new Bus();

    }

    public static EditFlowManager getInstance() {
        if(instance == null) {
            instance = new EditFlowManager();
        }
        return instance;
    }

    public void setMainEditActivity(MainEditScreen mainActivity)
    {
        mainEditActivity = mainActivity;
    }

    //get access to the bus
    public Bus getUiEventBus()
    {
        return uiEventBus;
    }

    //register an object for all UI events
    public void registerUIEvents(Object object)
    {
        if(!registered.contains(object)) {
            registered.add(object);
            uiEventBus.register(object);
        }
    }

    //unregister from any future UI events
    public void unregisterUIEvents(Object object)
    {
        if(registered.contains(object)) {
            uiEventBus.unregister(object);
            registered.remove(object);
        }
    }

    static final String EXTRA_FILTER_WID = "filterWID";
    static final String EXTRA_INNER_FILTER_WID = "innerWID";

    public Intent createEditIntent(Context mContext, FilterComposite toEdit)
    {
        return createEditIntent(mContext, toEdit, null);
    }
    public Intent createEditIntent(Context mContext, FilterComposite toEdit, String innerFilterWID)
    {
        Intent i = new Intent();
        i.setClass(mContext, MainEditScreen.class);
        i.putExtra(EXTRA_FILTER_WID, toEdit.getUniqueID());
        if(innerFilterWID != null && !innerFilterWID.equals(""))
            i.putExtra(EXTRA_INNER_FILTER_WID, innerFilterWID);
        return i;
    }

    //last saved object
    public FilterComposite getFilterFromEditIntent(Intent intent)
    {
        FilterManager fm = FilterManager.getInstance();
        if(intent == null)
            return fm.getLastEditedFilter();
        else
        {
            //grab info from intent
            String filterID = intent.getStringExtra(EXTRA_FILTER_WID);
            return fm.getFilter(filterID);
        }
    }

    public ObjectNode DefaultParams(FragmentActivity activity)
    {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jNode = mapper.createObjectNode();

        ObjectNode uiParams = mapper.createObjectNode();

        int thumbnailSize = (int)activity.getResources().getDimension(R.dimen.app_edit_iec_thumbnail_size);
        int cppnCalculateSize = getBitmapSquareSize(activity, thumbnailSize);

        uiParams.set("width", mapper.convertValue(cppnCalculateSize, JsonNode.class));
        uiParams.set("height", mapper.convertValue(cppnCalculateSize , JsonNode.class));

        jNode.set("ui", uiParams);

        ObjectNode uiParParams = mapper.createObjectNode();

        uiParams.set("width", mapper.convertValue(thumbnailSize, JsonNode.class));
        uiParams.set("height", mapper.convertValue(thumbnailSize, JsonNode.class));

        jNode.set("parents", uiParParams);

        return jNode;
    }

    //most of this stuff should be handled in the module honestly. That's what it's for, to centralize how evolution is done
    //why doesn't it have the correct stuff?
    public void temporaryLaunchIECWithFilter(FragmentActivity mContext, FilterComposite filter)
    {
        //need to open up IEC frag
        EditFilterIEC editFilterIEC = new EditFilterIEC();

        //then async start evolution!
        //set the evo cache location in our params
        JsonNode params = DefaultParams(mContext);

        //set the iec params now that we have them
        editFilterIEC.setIecParams(params);

        //create our parameters
        NeatParameters np = NEATInitializer.DefaultNEATParameters();

        //higher mutations after creating children plz
        //-- for now
        np.postSexualMutations = 15;
        np.postAsexualMutations = 15;

        //initialize (only happens once no worries)
        NEATInitializer.InitializeActivationFunctions();

        //we need to inject our objects!
        ObjectGraph graph = ObjectGraph.create(Arrays.asList(new FilterEvolutionInjectModule(mContext, np, null)).toArray());
        editFilterIEC.injectGraph(graph);


        //add register fragment to the stack
        mContext.getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, editFilterIEC)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }


    //both bitmap sizes and evolutionary defaults -- params, should be handled elsewhere
    int getBitmapSquareSize(Context mContext, int imageContainerSize)
    {
        //how much to scale the CPPNs
        float widthHeightScale = Float.parseFloat(mContext.getResources().getString(R.string.mainImageCPPNScaleDown));

        //waaaaaaaaaa
        int widthHeight = (int)(imageContainerSize*widthHeightScale);
        return widthHeight;
    }

    public Task<FilterComposite> asyncRunFilterOnImage(final Context mContext,
                                                       final FilterComposite filter,
                                                       final int imageContainerSize,
                                                       final boolean isThumbnail,
                                                       final Bitmap inputImage)
    {
        //you said huh?
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode uiParams = mapper.createObjectNode();

        int widthHeight = getBitmapSquareSize(mContext, imageContainerSize);

        uiParams.set("width", mapper.convertValue(widthHeight, JsonNode.class));
        uiParams.set("height", mapper.convertValue(widthHeight, JsonNode.class));

        //just select default for now
        FilterArtifact filterArtifact = filter.getFilterArtifact();

        //create filter object
        GPUNetworkFilter gpuNetworkFilter = new GPUNetworkFilter();

        return gpuNetworkFilter.AsyncFilterBitmapGPU(mContext, inputImage, filterArtifact, uiParams)
                .continueWith(new Continuation<Bitmap, FilterComposite>() {
                    @Override
                    public FilterComposite then(Task<Bitmap> task) throws Exception {

                        if (task.isCancelled()) {
                            throw new RuntimeException("Converting object to UI was cancelled!");
                        } else if (task.isFaulted()) {
                            Toast.makeText(mContext, "Error creating card from Artifact", Toast.LENGTH_SHORT).show();
                            Log.d("IEC: ArtifactToUIError", "Error creating UI from Artifact: " + task.getError().getMessage());
                            throw task.getError();
                        }
                        //great success!
                        else {

                            final Bitmap filtered = task.getResult();
                            if(!isThumbnail)
                                filter.setFilteredBitmap(filtered);
                            else
                                filter.setFilteredThumbnailBitmap(filtered);
                        }
                        return filter;
                    }
                });
    }

    static final Continuation<FilterComposite,Void> emptyContinuation = new Continuation<FilterComposite, Void>() {
        @Override
        public Void then(Task<FilterComposite> task) throws Exception {
            return null;
        }
    };

    public void lazyLoadFilterIntoImageView(final Context mContext, final FilterComposite filterComposite,
                                            final int width, int height, final boolean isThumbnail, final ImageView view)
    {
        lazyLoadFilterIntoImageView(mContext, filterComposite, width, height, isThumbnail, view, emptyContinuation);
    }
    public void lazyLoadFilterIntoImageView(final Context mContext, final FilterComposite filterComposite,
                                            final int width, int height, final boolean isThumbnail, final ImageView view,
                                            final Continuation<FilterComposite, Void> afterSetImageContinuation)
    {
        BitmapCacheManager.getInstance().lazyLoadBitmap(filterComposite.getImageURL(), width, height, false,
                new BitmapCacheManager.LazyLoadedCallback() {
                    @Override
                    public void imageLoaded(String url, Bitmap bitmap) {

                        //set current bitmap
                        if(!isThumbnail)
                            filterComposite.setCurrentBitmap(bitmap);
                        else
                            filterComposite.setThumbnailBitmap(bitmap);

                        asyncRunFilterOnImage(mContext, filterComposite, width, isThumbnail, bitmap)
                                .continueWith(new Continuation<FilterComposite, FilterComposite>() {
                                    @Override
                                    public FilterComposite then(Task<FilterComposite> task) throws Exception {

                                        //grab the image from the card -- hack for now
                                        view.setScaleType(ImageView.ScaleType.FIT_CENTER);

                                        if (!isThumbnail)
                                            view.setImageBitmap(filterComposite.getFilteredBitmap());
                                        else
                                            view.setImageBitmap(filterComposite.getFilteredThumbnailBitmap());

                                        return task.getResult();
                                    }
                                }, Task.UI_THREAD_EXECUTOR).continueWith(afterSetImageContinuation, Task.UI_THREAD_EXECUTOR);
                    }

                    @Override
                    public void imageLoadFailed(String reason, Exception e) {

                        //otherwise, fail -- don't know how to handle
                        Toast.makeText(mContext, "EditFilterIEC: Filter Load Failed - " + reason, Toast.LENGTH_SHORT).show();

                        Task.call(new Callable<FilterComposite>() {
                            @Override
                            public FilterComposite call() throws Exception {
                                return null;
                            }
                        }).continueWith(afterSetImageContinuation, Task.UI_THREAD_EXECUTOR);
                    }
                });
    }

}
