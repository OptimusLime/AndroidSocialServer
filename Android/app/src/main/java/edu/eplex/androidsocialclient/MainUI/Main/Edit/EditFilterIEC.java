package edu.eplex.androidsocialclient.MainUI.Main.Edit;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    //our evolution object
    @Inject
    AsyncInteractiveEvolution evolution;

    boolean fetchingMoreFilters;

    //
    EditIECCompositeAdapter filterImageAdapter;
    EndlessGridScrollListener mScrollListener;
    JsonNode iecParams;

    public void setIecParams(JsonNode iecParams)
    {
        this.iecParams = iecParams;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //initilaize!
        asyncInitializeIECandUI(getActivity());
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.app_fragment_workshop, container, false);

        //we now have access to list view thanks to butterknife!
        ButterKnife.inject(this, rootView);

        //Register for events from the edit flow manager please -- we should keep in touch
        //you know, as friends or whatever
        //with event benefits
        //just thinking out loud
        EditFlowManager.getInstance().registerUIEvents(this);

        return rootView;
    }

    public void injectGraph(ObjectGraph graph) {
        //need to inject these individuals
        //that will create appropriate new evolution and async artifact objects
        graph.inject(this);

        //inject inside our evolution object as well
        graph.inject(evolution);
    }

    Task<Void> asyncInitializeIECandUI(FragmentActivity activity) {

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
    void addMoreOffspring(int count, FilterComposite original)
    {
        if(fetchingMoreFilters)
            return;

        //we get some cards, async style!

        fetchingMoreFilters = true;

        //create a bunch of children object, wouldn't you please?
        List<Artifact> offspring = evolution.createOffspring(count);

        List<FilterComposite> evolutionComposites = new ArrayList<>();
        if(original != null)
            evolutionComposites.add(original);

        FilterComposite currentFilter = FilterManager.getInstance().getLastEditedFilter();
        for(int i=0; i < offspring.size(); i++)
        {
            //create a composite using the original information from the filter
            FilterComposite nComposite = new FilterComposite(currentFilter.getImageURL(), (FilterArtifact)offspring.get(i), currentFilter.getReadableName());
            evolutionComposites.add(nComposite);
        }

        //then add to our dude all at once
        filterImageAdapter.addAll(evolutionComposites);
    }



    void initializeUI(FragmentActivity activity)
    {
        //no images to start with, we will get those asynchronously
        filterImageAdapter = new EditIECCompositeAdapter(activity, new ArrayList<FilterComposite>(), new EditIECCompositeAdapter.OnFilterSelected() {
            @Override
            public void longSelectFilteredImage(FilterComposite selected, int ix) {
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
                addMoreOffspring(5,selected);
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

                //all done, thanks
                fetchingMoreFilters = false;
            }
        });

        //set the damn adapter -- we'll figure out fancy thigns later
        horizontalScroll.setAdapter(filterImageAdapter);


        //here we're going to set our scroll listener for creating more objects and appending them!
        mScrollListener = new EndlessGridScrollListener(horizontalScroll);

//        //lets set our callback item now -- this is called whenever the user scrolls to the bottom
        mScrollListener.setRequestItemsCallback(new EndlessGridScrollListener.RequestItemsCallback() {
            @Override
            public void requestItems(int pageNumber) {
//                System.out.println("On Refresh invoked..");

                //add more offspring, hoo-ray!!!

                //every time it's the same process -- generate artifacts, convert to phenotype, display!
                //rinse and repeat
                addMoreOffspring(4, null);
            }
        });
//
//        //make sure to add our infinite scroller here
        horizontalScroll.setOnScrollListener(mScrollListener);
    }

    void setSelectedFilterAsMainImage(FilterComposite filterComposite)
    {
        //now we have to install in the main image!

        //grab our artifact -- for now, we grab our inside NEAT Artifact like usual
        FilterArtifact filterArtifact = filterComposite.getFilterArtifact();

        //original image plzzzzz
        Bitmap mainImage = filterComposite.getCurrentBitmap();

        //we need a new image
        int widthHeight = EditFlowManager.getInstance().getBitmapWidthHeight(getActivity(), filterImage.getLayoutParams().width);

        //need to lazy load in the main image -- then async run the filter plz
        EditFlowManager.getInstance().lazyLoadFilterIntoImageView(getActivity(), filterComposite, widthHeight, widthHeight, false, filterImage);

    }










}
