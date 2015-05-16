package edu.eplex.androidsocialclient.MainUI.Main.Tabs;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Filter;

import com.melnykov.fab.FloatingActionButton;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import edu.eplex.androidsocialclient.MainUI.API.WinAPIManager;
import edu.eplex.androidsocialclient.MainUI.Adapters.WorkshopCompositeAdapter;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Publish.PublishFlowManager;
import edu.eplex.androidsocialclient.R;

/**
 * Created by paul on 3/15/15.
 */
public class WorkshopFragment extends Fragment implements WorkshopCompositeAdapter.OnCompositeFilterSelected {

    public static final TabFlowManager.TabID TAB = TabFlowManager.TabID.Workshop;
    public static final int SWIPE_DELETE_ID = 0;

    WorkshopCompositeAdapter compositeAdapter;

    @InjectView(R.id.workshop_fab)
    public FloatingActionButton fab;

    @InjectView(R.id.workshop_list_view)
    public SwipeMenuListView listView;


    //take a picture
    @OnClick(R.id.workshop_fab) void createFilterClick() {

        try {
            //take us to the photo creation
            switchTabs(TabFlowManager.TabID.Camera);
        }
        catch (Exception e)
        {
            //oops, can't create
            Toast.makeText(getActivity(), "Oops. Cannot create new filter", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_workshop_long_press, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    void editFilter(FilterComposite filter, int position)
    {
        //close menu before leaving
        toggleMenu(position, true);

        //we start our new edit activity!
        getActivity().startActivityForResult(EditFlowManager.getInstance().createEditIntent(getActivity(), filter), EditFlowManager.EDIT_SCREEN_REQUEST_CODE);
    }
    @Override
    public void publishCompositeFilter(final FilterComposite filter, int position) {
        //pub up the comp
        toggleMenu(position, true);

        //we want to publish -- for now, be aware of it
        Toast.makeText(getActivity(), "Looking to publish: " + filter.getUniqueID(), Toast.LENGTH_SHORT).show();

        //this is hte one we want to edit for publishing
        FilterManager.getInstance().setLastEditedFilter(filter);

        //we start our new edit activity!
        getActivity().startActivityForResult(PublishFlowManager.getInstance().createPublishIntent(getActivity(), filter),
                PublishFlowManager.PUBLISH_SCREEN_REQUEST_CODE);

//        WinAPIManager.getInstance().asyncPublishArtifact(filter)
//            .onSuccess(new Continuation<Void, Void>() {
//                @Override
//                public Void then(Task<Void> task) throws Exception {
//
//                    Toast.makeText(getActivity(), "Publish successful: " + filter.getUniqueID(), Toast.LENGTH_SHORT).show();
//
//                    return null;
//                }
//            });


    }

    @Override
    public void editClickCompositeFilter(FilterComposite filter, int position) {
        //edit up the stuff comp
        editFilter(filter,position);
    }

    @Override
    public void selectCompositeFilter(FilterComposite filter, int position) {
        //we select a filter yo! What do?
        editFilter(filter,position);
    }

    @Override
    public void longSelectCompositeFilter(FilterComposite filter, int position) {
        //long press - this should trigger contextual things -- want to delete? rename? etc...
//        getActivity().openContextMenu(listView);
        toggleMenu(position, false);
    }

    //we can open/close at this menu location -- and we can force try close if we want
    void toggleMenu(int position, boolean forceClose)
    {
        if(listView.isMenuOpen(position) || forceClose)
            listView.smoothCloseMenu();
        else
            listView.smoothOpenMenu(position);
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

        //we want long presses from our list view plz
        getActivity().registerForContextMenu(listView);
//        listView.setClickable(true);

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getActivity());
                //delete id
                deleteItem.setId(SWIPE_DELETE_ID);
                // set item background
                deleteItem.setBackground(new ColorDrawable(getActivity().getResources().getColor(R.color.primary_app_blue)));
                // set item width
                deleteItem.setWidth(getActivity().getResources().getDimensionPixelSize(R.dimen.workshop_preview_swipe_icon_size));
                // set a icon
                deleteItem.setIcon(R.drawable.ic_action_trash_white);

                // add to menu
                menu.addMenuItem(deleteItem);

            }
        };

// set creator
        listView.setMenuCreator(creator);

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {

                switch(menu.getMenuItem(index).getId())
                {
                    case SWIPE_DELETE_ID:
                        //we are asking to delete, let's confirm then do it
                        warnUserBeforeDelete(compositeAdapter.getItem(position));
                        break;
                }
                return false;
            }
        });


        return rootView;
    }


    //here we warn the user before loggin off
    void warnUserBeforeDelete(final FilterComposite filter)
    {
        new MaterialDialog.Builder(getActivity())
                .title("Confirm Delete Filter" )
                .content(Html.fromHtml("<br/>Are you sure you want to delete " + filter.getReadableName() + "?<br/>"))
                .titleAlignment(Alignment.CENTER)
                .contentAlignment(Alignment.CENTER)
                .positiveText("Yes")
                .neutralText("No")
                .callback(new MaterialDialog.Callback() {
                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        //easy, just hide ourselves!
                        materialDialog.hide();
                    }

                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        materialDialog.hide();

                        //lets delete the filter plz
                        try {
                            FilterManager.getInstance().deleteCompositeFilter(getActivity(), filter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
                .show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        TabFlowManager.getInstance().registerUIEvents(this);
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
        ArrayList<FilterComposite> removeSafely = new ArrayList<>();
        ArrayList<FilterComposite> addSafely = new ArrayList<>(existingList.currentFilters);
        for(int i=0; i < compositeAdapter.getCount(); i++)
        {
            FilterComposite fc = compositeAdapter.getItem(i);

            if(!existingList.currentFilters.contains(fc))
            {
                removeSafely.add(fc);
            }
            else //otherwise we already have it in composite adapter -- so remove from adding
                addSafely.remove(fc);
        }
        if(removeSafely.size() > 0)
            safeRemoveListFromAdapter(removeSafely);

        if(addSafely.size() > 0)
            //pretty simple -- safe add everything
            safeAddListToAdapter(addSafely);
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
            case Publish:
                //remove from the list of pictures -- but technically we would add to published images
                safeRemoveListFromAdapter(changeEvent.publishedFilters);
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

    @Override
    public void onResume() {
        super.onResume();

        if(compositeAdapter != null)
            compositeAdapter.notifyDataSetChanged();
    }
}
