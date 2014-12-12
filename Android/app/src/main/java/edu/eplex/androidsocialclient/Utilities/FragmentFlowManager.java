package edu.eplex.androidsocialclient.Utilities;

import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import edu.eplex.androidsocialclient.API.Manager.UserSessionManager;
import edu.eplex.androidsocialclient.Login.LoginFragment;
import edu.eplex.androidsocialclient.Login.RegisterFragment;
import edu.eplex.androidsocialclient.MainUI.UserSettingsFragment;
import edu.eplex.androidsocialclient.R;

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

    public void returnToLoginScreen(FragmentActivity activity)
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

    public void tempLaunchUserSettings(FragmentActivity activity, String username)
    {
        //importantly, we cannot go back to anything else in our history
        //we are at the user settings and logged in, there is no back -- just out of the app
        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

//        FragmentManager fm = activity.getSupportFragmentManager();
//        for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
//            fm.popBackStack();
//        }

        UserSettingsFragment userEntryPoint = new UserSettingsFragment();
        userEntryPoint.shouldWelcomeUsers(true);

        //add register fragment to the stack
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, userEntryPoint)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        //only happens when we successfully get logged in

    }



}
