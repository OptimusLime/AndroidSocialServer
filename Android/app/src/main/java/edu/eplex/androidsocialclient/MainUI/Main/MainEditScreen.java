package edu.eplex.androidsocialclient.MainUI.Main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;

import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.R;

/**
 * Created by paul on 3/16/15.
 */
public class MainEditScreen extends ActionBarActivity {


    @Override
    protected void onStart() {
        super.onStart();

        //start up our editor -- simple launch routine
        startEditor();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //make sure edit flow manager knows wassup
        EditFlowManager.getInstance().setMainEditActivity(this);

        //load the very basic outline -- most editing layout comes from the objects themselves -- maybe a save/forward bar at top always?
        setContentView(R.layout.app_main_edit_layout);
    }



    void startEditor()
    {
        //we were probably launched by intent
        Intent intent = getIntent();

        //get our flow manager for this fragmentactivity -- knows all about our intent :)
        EditFlowManager efm = EditFlowManager.getInstance();

        //grab our existing item
        FilterComposite currentFilter = efm.getFilterFromEditIntent(intent);

        //set it to be our last edited -- since we were sent here to DESTROY
        //sorry dont know where that came from. This filter was sent for LOVE.
        FilterManager.getInstance().setLastEditedFilter(currentFilter);

        //this is the object we're working with -- set it as our main object in the first edit fragment we open
        efm.temporaryLaunchIECWithFilter(this, currentFilter);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FilterManager.getInstance().asyncSaveFiltersToFile(this);
    }
}

