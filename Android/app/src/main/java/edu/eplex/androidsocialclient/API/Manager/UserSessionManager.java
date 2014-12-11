package edu.eplex.androidsocialclient.API.Manager;

import android.animation.ObjectAnimator;

import com.squareup.otto.Bus;

import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.User;

/**
 * Created by paul on 12/11/14.
 * Handles all the complexities of signing in/storing user information, facebook or otherwise
 */
public class UserSessionManager {

    //handle all user related calls across the app
    Bus userEventBus = new Bus();

    //contains all the user information required!
    APIToken currentAPIToken;

    //just a string value
    APIToken currentSignupToken;

    private static UserSessionManager instance = null;
    protected UserSessionManager() {
        // Exists only to defeat instantiation.
    }
    public static UserSessionManager getInstance() {
        if(instance == null) {
            instance = new UserSessionManager();
        }
        return instance;
    }

    //register/unregister on the bus
    public void register(Object eventObject)
    {
        userEventBus.register(eventObject);
    }
    public void unregister(Object eventObject)
    {
        userEventBus.unregister(eventObject);
    }




}
