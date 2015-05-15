package edu.eplex.androidsocialclient.MainUI.Main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import edu.eplex.androidsocialclient.MainUI.Filters.FilterComposite;
import edu.eplex.androidsocialclient.MainUI.Filters.FilterManager;
import edu.eplex.androidsocialclient.MainUI.Main.Edit.EditFlowManager;
import edu.eplex.androidsocialclient.MainUI.Main.Publish.PublishFlowManager;
import edu.eplex.androidsocialclient.R;

/**
 * Created by paul on 5/13/15.
 */
public class MainPublishScreen extends ActionBarActivity {

    boolean editorStarted = false;
    @Override
    protected void onStart() {
        super.onStart();

        //start up our editor -- simple launch routine
        if(!editorStarted) {
            startEditor();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //anytime we're created -- this means we don't have the editor started
//        editorStarted = false;

        //make sure edit flow manager knows wassup
        PublishFlowManager.getInstance().setMainPublishActivity(this);

        //start it up!
//        startEditor();


        //load the very basic outline -- most editing layout comes from the objects themselves -- maybe a save/forward bar at top always?
//        setContentView(R.layout.app_main_layout);
    }



    void startEditor()
    {
        editorStarted = true;

        //we were probably launched by intent
        Intent intent = getIntent();

        //get our flow manager for this fragmentactivity -- knows all about our intent :)
        EditFlowManager efm = EditFlowManager.getInstance();

        //grab our existing item
        FilterComposite currentFilter = efm.getFilterFromEditIntent(intent);

        //this is the object we're working with -- set it as our main object in the first edit fragment we open
        PublishFlowManager.getInstance().launchPublishUI(this, currentFilter);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        FilterManager.getInstance().asyncSaveFiltersToFile(this);
    }


}
