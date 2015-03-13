package edu.eplex.AsyncEvolution.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.Hashtable;

import edu.eplex.AsyncEvolution.main.fragments.HomeFragment;
import edu.eplex.AsyncEvolution.main.fragments.IECFragment;


/**
 * Created by ln0.
 */
public class AppFragmentPager extends FragmentPagerAdapter {

    //we have only 2 fragments being displayed
    //the CameraPreviewFragment and ImageViewerFragment
    static final int NUM_ITEMS = 2;

    //we're going to keep a weak reference to our created fragments
    //http://stackoverflow.com/questions/14035090/how-to-get-existing-fragments-when-using-fragmentpageradapter/23843743#23843743
    protected Hashtable<Integer, WeakReference<Fragment>> fragmentReferences = new Hashtable<Integer, WeakReference<Fragment>>();

    public AppFragmentPager(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
//                fragment = new HomeFragment();
                break;
            case 1:
//                fragment = new IECFragment();
                break;
            default:
                throw new RuntimeException("Only two views supported!");
        }

        //save a reference to the fragment so it doesn't detach from activity
        //http://stackoverflow.com/questions/14035090/how-to-get-existing-fragments-when-using-fragmentpageradapter/23843743#23843743
//        fragmentReferences.put(position, new WeakReference<Fragment>(fragment));

        return fragment;
    }


    @Override
    public void finishUpdate(ViewGroup container) {
        super.finishUpdate(container);

        Log.i("AppFragmentPager", "Finished switching pages");

    }

}