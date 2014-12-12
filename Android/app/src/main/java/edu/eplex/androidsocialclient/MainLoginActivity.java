package edu.eplex.androidsocialclient;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.view.Window;

import com.facebook.widget.LoginButton;
import com.squareup.otto.Subscribe;

import java.util.Arrays;

import edu.eplex.androidsocialclient.API.Manager.APIManager;
import edu.eplex.androidsocialclient.API.Manager.UserSessionManager;
import edu.eplex.androidsocialclient.Login.LoginFragment;
import edu.eplex.androidsocialclient.MainUI.UserSettingsFragment;


public class MainLoginActivity extends ActionBarActivity {

    private static final String TAG = "MainLoginActivity";

    private android.support.v4.app.Fragment uiFragment;
    private boolean userLoggedIn;

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //register for user info BEFORE we choose which screen to load
        UserSessionManager.getInstance().register(this, this);

        //TODO: Open true main activity -- not this fragment BS
        if (savedInstanceState == null) {

            android.support.v4.app.Fragment uiFragment;

            //we're already logged in -- route us more appropriately
            if(userLoggedIn)
                uiFragment = new UserSettingsFragment();
            else
                //not logged in -  Add the fragment on initial activity setup
                uiFragment = new LoginFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, uiFragment)
                    .commit();
        } else {
            // Or set the fragment from restored state info
            uiFragment = getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        UserSessionManager.getInstance().unregister(this, this);
    }

    @Subscribe
    public void currentUserInformation(UserSessionManager.CurrentUserInformation userInformation)
    {
        if(userInformation.currentAPIToken != null)
            userLoggedIn = true;
        else
            userLoggedIn = false;
    }

    @Subscribe
    public void userLoggedIn(UserSessionManager.UserLoggedInEvent userLoggedInEvent)
    {
        //we are officially in!
        userLoggedIn = true;
    }

    @Subscribe
    public void userLoggedOut(UserSessionManager.UserLoggedOutEvent userInformation)
    {
        //officially out!
        userLoggedIn = false;
    }
}
