package edu.eplex.androidsocialclient.MainUI.Main.Tabs;

import android.content.ContentValues;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.logging.Filter;

import com.melnykov.fab.FloatingActionButton;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import edu.eplex.androidsocialclient.MainUI.Adapters.WorkshopCompositeAdapter;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.R;

/**
 * Created by paul on 3/15/15.
 */
public class WorkshopFragment extends Fragment implements WorkshopCompositeAdapter.OnCompositeFilterSelected {

    public static final TabFlowManager.TabID TAB = TabFlowManager.TabID.Workshop;

    WorkshopCompositeAdapter compositeAdapter;

    @InjectView(R.id.workshop_fab)
    public FloatingActionButton fab;

    @InjectView(R.id.workshop_list_view)
    public ListView listView;

    //take a picture
    @OnClick(R.id.workshop_fab) void createFilterClick() {

        try {
            //take us to the photo creation
            switchTabs(TabFlowManager.TabID.Camera);
        }
        catch (Exception e)
        {
            //oops, can't create
            Toast.makeText(getActivity(), "Oops. Cannot create new filter", Toast.LENGTH_SHORT);
        }
    }


    @Override
    public void selectCompositeFilter(FilterComposite filter, int position) {
        //we select a filter yo! What do?

        //we start our new edit activity!
        getActivity().startActivity(EditFlowManager.getInstance().createEditIntent(getActivity(), filter));
    }

    @Override
    public void longSelectCompositeFilter(FilterComposite filter, int position) {
        //long press - this should trigger contextual things -- want to delete? rename? etc...

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.app_fragment_workshop, container, false);

        //we now have access to list view thanks to butterknife!
        ButterKnife.inject(this, rootView);

        //we should create our composite adapter BEFORE we register for UI events -- then we can add things later
        compositeAdapter = new WorkshopCompositeAdapter(getActivity(), this);

        //add our composite adapter for display purposes! Thanks.
        listView.setAdapter(compositeAdapter);

        //float below this list view plz, thx
        fab.attachToListView(listView);

        //then register for events -- we'll get one as soon as this happens
        TabFlowManager.getInstance().registerUIEvents(this);

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        TabFlowManager.getInstance().unregisterUIEvents(this);
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    void safeAddListToAdapter(List<FilterComposite> filters)
    {
        //we have the list -- added it to our listview adapter please!
        for(int i=0; i < filters.size(); i++)
        {
            compositeAdapter.safeAdd(filters.get(i));
        }
    }

    void safeRemoveListFromAdapter(List<FilterComposite> filters)
    {
        //we have the list -- added it to our listview adapter please!
        for(int i=0; i < filters.size(); i++)
        {
            compositeAdapter.safeRemove(filters.get(i));
        }
    }
    @Subscribe
    public void currentCompositeList(FilterManager.ExistingCompositeFilterEvent existingList)
    {
        //pretty simple -- safe add everything
        safeAddListToAdapter(existingList.currentFilters);
    }
    @Subscribe
    public void changeCompositeList(FilterManager.ChangeCompositeFilterEvent changeEvent)
    {
        //we have some modifications, remove some things from our object
        switch (changeEvent.action)
        {
            case Add:
                safeAddListToAdapter(changeEvent.addedFilters);
                break;
            case Remove:
                safeRemoveListFromAdapter(changeEvent.removedFilters);
                break;
            case BothAddRemove:
                safeAddListToAdapter(changeEvent.addedFilters);
                safeRemoveListFromAdapter(changeEvent.removedFilters);
                break;
        }
    }

    public void switchTabs(TabFlowManager.TabID toTab) throws Exception
    {
        //need to do something with the uri for the image -- but nothing for now
        TabFlowManager.getInstance().switchToTab(TAB, toTab);
    }


}
