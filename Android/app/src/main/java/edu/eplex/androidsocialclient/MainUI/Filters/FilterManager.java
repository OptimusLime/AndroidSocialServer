package edu.eplex.androidsocialclient.MainUI.Filters;

import android.content.Context;
import android.content.Intent;

import com.squareup.otto.Produce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;

import dagger.Provides;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFilterIEC;
import edu.eplex.androidsocialclient.MainUI.Main.Tabs.TabFlowManager;
import eplex.win.FastNEATJava.utils.cuid;

/**
 * Created by paul on 3/15/15.
 * Singleton that handles all of the filters -- basically master of the in progress filter creations and what to do with them
 */
public class FilterManager {

    public class ExistingCompositeFilterEvent
    {
        public List<FilterComposite> currentFilters;
        public ExistingCompositeFilterEvent(ArrayList<FilterComposite> existingFilters)
        {
            this.currentFilters = existingFilters;
        }
    }

    public enum FilterEventAction
    {
        Add,
        Remove,
        BothAddRemove
    }

    public class ChangeCompositeFilterEvent
    {
        public FilterEventAction action;
        public List<FilterComposite> addedFilters;
        public List<FilterComposite> removedFilters;

        //derrrrr
        public ChangeCompositeFilterEvent(FilterComposite filter, FilterEventAction action) throws Exception {
            this.action = action;
            switch (action)
            {
                case Add:
                    this.addedFilters = Arrays.asList(filter);
                    break;
                case Remove:
                    this.removedFilters = Arrays.asList(filter);
                    break;
                case BothAddRemove:
                    throw new Exception("Can't create add/remove filter event with a single filter!");
            }
        }
    }




    static final String BASE_DEFAULT_NAME = "Filter ";
    static final int BASE_DEFAULT_NUMBER_ADD = 1;

    //Very simple naming convention -- just append to the size of the filters -- is it your 5th filter? Then the default name is Filter 5.
    //DERRRRRR
    String nextReadableName()
    {
        return BASE_DEFAULT_NAME + (existingFilters.size() + BASE_DEFAULT_NUMBER_ADD);

    }

    FilterComposite lastEditedFilter;
    ArrayList<FilterComposite> existingFilters = new ArrayList<FilterComposite>();
    Map<String, FilterComposite> filterMap = new HashMap<>();

    private static FilterManager instance = null;

    protected FilterManager() {
        // Exists only to defeat instantiation.

        //we must subscribe to the flow manager

        //we need to register to send events -- we provide some things that are useful
        TabFlowManager.getInstance().registerUIEvents(this);
    }

    public static FilterManager getInstance() {
        if (instance == null) {
            instance = new FilterManager();
        }
        return instance;
    }


    //get it yo.
    public FilterComposite getFilter(String filterID)
    {
        return this.filterMap.get(filterID);
    }
    public FilterComposite getLastEditedFilter()
    {
        return lastEditedFilter;
    }



    //Add a new composite filter -- passing in the image to work with
    public FilterComposite createNewComposite(String imageURL) throws Exception {
        //lets make a new filter -- that's what they want!
        FilterComposite filter = new FilterComposite(imageURL, cuid.getInstance().generate(), nextReadableName());

        //add filter to existing -- it's in there now!
        this.existingFilters.add(filter);
        this.filterMap.put(filter.getUniqueID(), filter);

        //added a filter -- alter our followers -- they're so needy
        ChangeCompositeFilterEvent changeEvent = new ChangeCompositeFilterEvent(filter, FilterEventAction.Add);

        //fuckyourface
        TabFlowManager.getInstance().getUiEventBus().post(changeEvent);

        //all done! Send it back if you must.
        return filter;
    }

    public void deleteCompositeFilter(String filterWID) throws Exception {

        FilterComposite filter = this.filterMap.get(filterWID);
        deleteCompositeFilter(filter);
    }
    public void deleteCompositeFilter(FilterComposite filter) throws Exception {

        //remove the filter please
        this.existingFilters.remove(filter);
        this.filterMap.remove(filter.getUniqueID());

        //now we must update our filter event
        //removed a filter -- alter our followers -- they're so needy
        ChangeCompositeFilterEvent changeEvent = new ChangeCompositeFilterEvent(filter, FilterEventAction.Remove);

        //goodbyeyoudevil
        TabFlowManager.getInstance().getUiEventBus().post(changeEvent);
    }


    @Produce
    public ExistingCompositeFilterEvent currentFilterList()
    {
        return new ExistingCompositeFilterEvent(this.existingFilters);
    }





}
