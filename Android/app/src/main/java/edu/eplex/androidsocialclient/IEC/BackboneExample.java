package edu.eplex.androidsocialclient.IEC;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import javax.inject.Inject;

import edu.eplex.AsyncEvolution.cardUI.GridSquareFragment;
import edu.eplex.AsyncEvolution.cardUI.StickyGridFragment;
import eplex.win.winBackbone.BasicEvolution;
import eplex.win.winBackbone.winBackbone;
import edu.eplex.AsyncEvolution.main.fragments.HomeFragment;
import edu.eplex.AsyncEvolution.R;


public class BackboneExample extends FragmentActivity {

    @Inject
    BasicEvolution evolution;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backbone_example);
        if (savedInstanceState == null) {
           getSupportFragmentManager()
                   .beginTransaction()
                   .add(R.id.container, new HomeFragment())
                   .commit();
//            getFragmentManager().beginTransaction()
//                    .add(R.id.container, new HomeFragment())
//                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.backbone_example, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
