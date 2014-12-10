package edu.eplex.androidsocialclient.Utilities;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import edu.eplex.androidsocialclient.Login.LoginFragment;
import edu.eplex.androidsocialclient.Login.RegisterFragment;
import edu.eplex.androidsocialclient.MainUI.UserSettingsFragment;

/**
 * Created by paul on 12/9/14.
 */
public class FragmentFlowManager {

    private static FragmentFlowManager instance = null;
    protected FragmentFlowManager() {
        // Exists only to defeat instantiation.
    }

    public static FragmentFlowManager getInstance() {
        if(instance == null) {
            instance = new FragmentFlowManager();
        }
        return instance;
    }

    public void logoutUser(FragmentActivity activity)
    {
        //TODO: figure out login/main activity issues
        //we need to drop back to whatever activity is holding this
        //it's not really a fragment issue, it's an activity level issue

        //importantly, we cannot go back to anything else in our history
        //we are at the login, there is no back -- just out of the app
        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        //for now, we just replace the current fragment with a login screen
        //add login fragment to the stack
        //do not add to back state
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new LoginFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

    }

    public void homeSignupFacebookOrEmail(FragmentActivity activity)
    {
        //transfer
        //create a registration fragment -- handles signing up with e-mail
        //or alternatively, signing up once logged in with facebook
        RegisterFragment rf = new RegisterFragment();

        //add register fragment to the stack
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, rf)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit();
    }

    public void tempLaunchUserSettings(FragmentActivity activity)
    {
        //importantly, we cannot go back to anything else in our history
        //we are at the user settings and logged in, there is no back -- just out of the app
        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

//        FragmentManager fm = activity.getSupportFragmentManager();
//        for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
//            fm.popBackStack();
//        }

        //add register fragment to the stack
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new UserSettingsFragment())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }



}
