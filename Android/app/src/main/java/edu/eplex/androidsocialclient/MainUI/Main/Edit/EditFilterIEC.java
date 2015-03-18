package edu.eplex.androidsocialclient.MainUI.Main.Edit;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Filter;

import javax.inject.Inject;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import dagger.ObjectGraph;
import edu.eplex.AsyncEvolution.asynchronous.implementation.AsyncLocalIEC;
import edu.eplex.AsyncEvolution.asynchronous.interfaces.AsyncInteractiveEvolution;
import edu.eplex.AsyncEvolution.cardUI.EndlessGridScrollListener;
import edu.eplex.androidsocialclient.GPU.GPUNetworkFilter;
import edu.eplex.androidsocialclient.MainUI.Adapters.BitmapAdapter;
import edu.eplex.androidsocialclient.MainUI.Adapters.EditIECCompositeAdapter;
import edu.eplex.androidsocialclient.MainUI.Cache.BitmapCacheManager;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterArtifact;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.R;
import eplex.win.winBackbone.Artifact;
import it.neokree.materialtabs.MaterialTab;
import it.sephiroth.android.library.widget.HListView;

/**
 * Created by paul on 3/16/15.
 */
public class EditFilterIEC extends Fragment {

    //need to handle evolution and our evolution views

    @InjectView(R.id.edit_iec_filter_main_image)
    public ImageView filterImage;

    @InjectView(R.id.edit_iec_filter_hlist)
    public HListView horizontalScroll;

    @InjectView(R.id.app_edit_iec_action_button_back_text)
    public TextView backButtonText;

    @InjectView(R.id.app_edit_iec_action_button_complete_text)
    public TextView saveButtonText;


    //our evolution object
    @Inject
    AsyncInteractiveEvolution evolution;

//    boolean fetchingMoreFilters;
    boolean initialized = false;
    boolean touchImage = false;

    //
    EditIECCompositeAdapter filterImageAdapter;
    EndlessGridScrollListener mScrollListener;
    JsonNode iecParams;
    FilterComposite selectedFilter;

    ArrayList<FilterComposite> allFilters = new ArrayList<>();

    int mainImageDesiredWidthHeight;

    public void setIecParams(JsonNode iecParams)
    {
        this.iecParams = iecParams;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.app_edit_fragment_iec, container, false);

        //we now have access to list view thanks to butterknife!
        ButterKnife.inject(this, rootView);

        //Register for events from the edit flow manager please -- we should keep in touch
        //you know, as friends or whatever
        //with event benefits
        //just thinking out loud
        EditFlowManager.getInstance().registerUIEvents(this);

        //initilaize!
        asyncInitializeIECandUI(getActivity());

        return rootView;
    }

    void clearEvolutionMemory(FilterComposite chosenFilter)
    {
        String chosen = chosenFilter == null ? "" : chosenFilter.getUniqueID();
        for(int i=0; i < allFilters.size(); i++)
        {
            FilterComposite filter = allFilters.get(i);

            String fUID = filter.getUniqueID();

            //should we call the cache manager? Or just clear it out anyways
            if(!fUID.equals(chosen))
            {
                filter.clearFilterBitmapMemory();
            }

//            if(!filter.getImageURL().equals(chosenFilter.getImageURL()))
//                filter.clearFilterBitmapMemory();
        }

        filterImageAdapter.clear();
    }
    @OnClick(R.id.app_edit_iec_action_button_complete)
    void completeClick()
    {
        FilterComposite originalFilter = FilterManager.getInstance().getLastEditedFilter();
        clearEvolutionMemory(selectedFilter);

        EditFlowManager.getInstance().finishIECFilter(getActivity(), originalFilter, selectedFilter);
    }

    @OnClick(R.id.app_edit_iec_action_button_back)
    void backClick()
    {
        //clear everything including the selected filter
        clearEvolutionMemory(null);

        //we need to go back
        EditFlowManager.getInstance().cancelIECFilter(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //if we die, evolution bitmaps die with us
        clearEvolutionMemory(null);
    }

    public void injectGraph(ObjectGraph graph) {
        //need to inject these individuals
        //that will create appropriate new evolution and async artifact objects
        graph.inject(this);

        //inject inside our evolution object as well
        graph.inject(evolution);
    }

    Task<Void> asyncInitializeIECandUI(FragmentActivity activity) {

//        if(initialized)
//            return null;

        //don't do this more than once -- it's starting evolution
        initialized = true;

        //first we initialize all our internal organs, so to speak
        initializeUI(activity);

        //then we send off our evolution process to initialize itself!
        return evolution.asyncInitialize(iecParams)
                .continueWithTask(new Continuation<Void, Task<Void>>() {
                    @Override
                    public Task<Void> then(Task<Void> task) throws Exception {
//
//                        List<Artifact> allSeeds = evolution.seeds();
//                        for (Artifact seed : allSeeds)
//                            allArtifactMap.put(seed.wid(), seed);

                        //start by fetching the minimal required for displaying -- 6/8 should do!
                        addMoreOffspring(6, null);
                        return null;
                    }
                }, Task.UI_THREAD_EXECUTOR);
    }


    //we've been summoned to fetch new card objects
    //this is the crucial function for async IEC
    //this means creating artifacts, eating their children, then turning them into cards for the UI
    //wait... what was that second thing?
    //Creating new artifacts?
    //no... that wasn't it...
    //OH! Turning the artifacts into Card UI objects
    //uhhhh.....
    void addMoreOffspring(int count, FilterComposite original) {
//        if(fetchingMoreFilters)
//            return;

        //we get some cards, async style!
//        fetchingMoreFilters = true;

        //create a bunch of children object, wouldn't you please?
        List<Artifact> offspring = evolution.createOffspring(count);

        List<FilterComposite> evolutionComposites = new ArrayList<>();
        if (original != null)
            evolutionComposites.add(original);

        FilterComposite currentFilter = FilterManager.getInstance().getLastEditedFilter();
        for (int i = 0; i < offspring.size(); i++) {
            //create a composite using the original information from the filter
            FilterComposite nComposite = new FilterComposite(currentFilter.getImageURL(), (FilterArtifact) offspring.get(i), currentFilter.getReadableName());
            evolutionComposites.add(nComposite);
            allFilters.add(nComposite);
        }

        //then add to our dude all at once
        filterImageAdapter.addAll(evolutionComposites);

        //jump back to zero plz -- we're doign next round of evo
        if(original != null)
            horizontalScroll.smoothScrollToPosition(0);

        //if we have no selected filter -- set it to be the first indiviual :)
        if (selectedFilter == null)
            setSelectedFilterAsMainImage(evolutionComposites.get(0));
        else {
            setSelectedFilterAsMainImage(selectedFilter);

        }
    }

    void initializeUI(FragmentActivity activity)
    {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        mainImageDesiredWidthHeight = Math.min(width,height);

        filterImage.getLayoutParams().width = mainImageDesiredWidthHeight;
        filterImage.getLayoutParams().height = mainImageDesiredWidthHeight;
        filterImage.setImageResource(R.drawable.ic_action_emo_tongue_white);
        filterImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        Typeface typeFace= Typeface.createFromAsset(getActivity().getAssets(), "fonts/android.ttf");
        backButtonText.setTypeface(typeFace);
        saveButtonText.setTypeface(typeFace);

        //no images to start with, we will get those asynchronously
        if(filterImageAdapter == null)
            filterImageAdapter = new EditIECCompositeAdapter(activity, new ArrayList<FilterComposite>(), new EditIECCompositeAdapter.OnFilterSelected() {
                @Override
                public void longSelectFilteredImage(final FilterComposite selected, int ix) {
                    //TIME FOR EVOLUTION!
                    setSelectedFilterAsMainImage(selected);

                    //get the filter artifact -- we need to do some evolution
                    FilterArtifact filterArtifact = selected.getFilterArtifact();

                    evolution.clearParents();
                    evolution.selectParents(Arrays.asList(filterArtifact.wid()));

                    //clear it out, then fill it up!
                    filterImageAdapter.clear();

                    //go get more please!
                    //we keep around our original selection -- kind of like elitism
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addMoreOffspring(5, selected);
                        }
                    });
                }

                @Override
                public void selectFilteredImage(FilterComposite filter, int ix) {

                    //set as our image
                    setSelectedFilterAsMainImage(filter);
                }

                @Override
                public void finishedLoadingAndFiltering() {

                    //all done with this batch, be prepared to fetch more!
                    mScrollListener.notifyMorePages();

                    //request more if we're at the end of the scroll -- this would
                    //stop the issue where the user attempts to scroll more while loading only to be denied
                    //then have to go back and forth to load again
                    mScrollListener.forceRequestMore();

                    //all done, thanks
//                    fetchingMoreFilters = false;
                }
            });

        //set the damn adapter -- we'll figure out fancy thigns later
        horizontalScroll.setAdapter(filterImageAdapter);


        //here we're going to set our scroll listener for creating more objects and appending them!
        if(mScrollListener == null)
        {

            mScrollListener = new EndlessGridScrollListener(horizontalScroll);

            //lets set our callback item now -- this is called whenever the user scrolls to the bottom
            mScrollListener.setRequestItemsCallback(new EndlessGridScrollListener.RequestItemsCallback() {
                @Override
                public void requestItems(int pageNumber) {
                    //add more offspring, hoo-ray!!!
                    //every time it's the same process -- generate artifacts, convert to phenotype, display!
                    //rinse and repeat
                    addMoreOffspring(4, null);
                }
            });
        }

        //make sure to add our infinite scroller here
        horizontalScroll.setOnScrollListener(mScrollListener);


        filterImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    //we want to display the chosen filter's original image

                    touchImage = true;
                    if(selectedFilter != null)
                        filterImage.setImageBitmap(selectedFilter.getThumbnailBitmap());

                    //need to return true if we handle the up action later
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                {
                    if(touchImage)
                    {
                        touchImage = false;
                        if(selectedFilter != null)
                            filterImage.setImageBitmap(selectedFilter.getFilteredBitmap());
                    }

                }

                return false;
            }
        });
    }


    void setSelectedFilterAsMainImage(FilterComposite filterComposite) {
        //now we have to install in the main image!
        if(filterComposite == selectedFilter)
            return;

        selectedFilter = filterComposite;

        //grab our artifact -- for now, we grab our inside NEAT Artifact like usual
//        FilterArtifact filterArtifact = filterComposite.getFilterArtifact();

        //original image plzzzzz
//        Bitmap mainImage = filterComposite.getCurrentBitmap();

        //we need a new image
        int widthHeight = EditFlowManager.getInstance().getBitmapSquareSize(getActivity(), mainImageDesiredWidthHeight);

        try {
            //need to lazy load in the main image -- then async run the filter plz
            EditFlowManager.getInstance().lazyLoadFilterIntoImageView(getActivity(), filterComposite, widthHeight, widthHeight, false, filterImage);
        }
        catch (Exception e)
        {
            Toast.makeText(getActivity(), "Error loading main image file.", Toast.LENGTH_SHORT);
        }
    }










}
