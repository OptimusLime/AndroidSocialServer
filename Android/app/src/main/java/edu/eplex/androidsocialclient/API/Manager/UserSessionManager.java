package edu.eplex.androidsocialclient.API.Manager;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import java.util.Arrays;
import java.util.List;

import edu.eplex.androidsocialclient.API.LoginAPI;
import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import edu.eplex.androidsocialclient.API.Objects.User;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by paul on 12/11/14.
 * Handles all the complexities of signing in/storing user information, facebook or otherwise
 */
public class UserSessionManager {

    public enum RegistrationType
    {
        UserLoginWithFacebook,
        UserSignupWithFacebook,
        UserSignupWithEmail
    }
    public class ReadyForEvent
    {
        public RegistrationType registrationType;
        public Object eventObject;

        public ReadyForEvent(RegistrationType rt, Object eventObject)
        {
            this.registrationType = rt;
            this.eventObject = eventObject;
        }
    }
    public enum LoginFailureReason
    {
        ServerNon200Status,
        ServerNonResponsive
    }
    public class LoginFailure {
        public LoginFailureReason loginFailureReason;
        public int htmlStatus;
        public LoginFailure(LoginFailureReason lfr, int htmlStatus)
        {
            this.loginFailureReason = lfr;
            this.htmlStatus = htmlStatus;
        }
    }
    public class CurrentUserInformation
    {
        public APIToken currentAPIToken;
        public AccessToken lastKnownFacebookAccessToken;
        public CurrentUserInformation(APIToken userInfo, AccessToken fbInfo)
        {
            this.currentAPIToken = userInfo;
            this.lastKnownFacebookAccessToken =  fbInfo;
        }
    }
    public class UserLoggedInEvent
    {
        public APIToken apiToken;
        public UserLoggedInEvent(APIToken currentToken)
        {
            this.apiToken = currentToken;
        }
    }

    //tag it up
    private static String TAG = "UserSessionManager";

    //change this for more permissions
    private List<String> fbReadPermissions = Arrays.asList("public_profile", "email");

    //handle all user related calls across the app
    Bus userEventBus = new Bus();
    LoginAPI apiService;

    //contains all the user information required!
    APIToken currentAPIToken;

    //just a string value
    AccessToken lastActiveFacebookAccessToken;

    //handle session state callbacks by Facebook
    Activity currentUILifeCycleActivity;
    UiLifecycleHelper uiHelper;

    boolean userIsLoggedIn;

    private static UserSessionManager instance = null;

    protected UserSessionManager() {
        // Exists only to defeat instantiation.
        //register ourself for the event bus -- to produce things
        userEventBus.register(this);
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

    //FACEBOOK LOGIN HANDLING

    //we have our helper for passing on activity results
    //must be called by all fragments interested in user information!
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(uiHelper != null)
            uiHelper.onActivityResult(requestCode, resultCode, data);
    }
    public void onSaveInstanceState(Bundle outState) {
        if(uiHelper != null)
            uiHelper.onSaveInstanceState(outState);
    }

    //we only ever open the session for reading
    public void getUserFBInformation(Fragment parentFragment)
    {
        getUserFBInformation(parentFragment, fbReadPermissions);
    }
    public void getUserFBInformation(Fragment parentFragment, List<String> permissions)
    {
        if(currentUILifeCycleActivity != parentFragment.getActivity())
        {
            //store this activity so that we don't keep duplicating the uiHelper object
            currentUILifeCycleActivity = parentFragment.getActivity();

            //grab any session changes in callback
            uiHelper = new UiLifecycleHelper(parentFragment.getActivity(), callback);
        }

        //we check if we're already logging in for a session, if so, we kill the session
        Session openSession = Session.getActiveSession();

        //we always clear this info because we don't want outdated tokens during signup/login
        //the user won't mind calling facebook again, it's expected
        if(openSession != null)
            openSession.closeAndClearTokenInformation();

        //clear out the cache as well please -- no need for FB info to be stored locally
        openSession = Session.openActiveSessionFromCache(parentFragment.getActivity());

        //clear it out please
        if(openSession != null && openSession.isOpened())
            openSession.closeAndClearTokenInformation();

        //allowloginui == true because we want to open the session no matter what -- whether we have something cached or not
        Session.openActiveSession(parentFragment.getActivity(), parentFragment, true, permissions, callback);
    }

    //callback that handles change in Login/Logout status
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    //handle register click by adding the RegisterFragment to our stack
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {

        if(exception != null)
            Log.d(TAG, exception.toString());

        //we only care about the open sessions, that's it
        if(session.isOpened())
        {
            //get our latest access token
            final String fbAccessToken = session.getAccessToken();

            //store the last known access token
            lastActiveFacebookAccessToken = new AccessToken(fbAccessToken);

            //with this information, we're ready to attempt login now -- let the world know
            userEventBus.post(new ReadyForEvent(RegistrationType.UserLoginWithFacebook, lastActiveFacebookAccessToken));
        }
    }

    //USER LOGIN HANDLING
    public boolean loginUserWithFacebook(Fragment fragment)
    {
        if(lastActiveFacebookAccessToken == null || lastActiveFacebookAccessToken.access_token == null)
        {
            //can't make a user login request without an existing access token
            return false;
        }
        try {
            //will send facebook access token info and attempt to requisition an api token
            if (apiService == null)
                apiService = APIManager.getInstance().createLoginAPI(fragment.getActivity());

            apiService.asyncFacebookLoginRequest(lastActiveFacebookAccessToken, userLoginWithFacebookCallback);
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    //handle
    Callback<APIToken> userLoginWithFacebookCallback = new Callback<APIToken>() {
        @Override
        public void success(APIToken apiToken, Response response) {

            //if our user is not initialized, we're ready to register, but we are not yet loggin in
            currentAPIToken = apiToken;

            if(!apiToken.user.isInitialized)
            {
                //we're ready for the user to signup using their facebook info now
                userEventBus.post(new ReadyForEvent(RegistrationType.UserSignupWithFacebook, apiToken));
            }
            else
            {
                //the user is initialized!
                if(apiToken.api_token != null)
                {
                    //we're all logged in!
                    userOfficiallyLoggedIn();
                }

            }
        }

        @Override
        public void failure(RetrofitError error) {

            LoginFailure failure;
            if(error.getResponse() != null)
            {
                failure = new LoginFailure(LoginFailureReason.ServerNon200Status, error.getResponse().getStatus());
            }
            else
                failure = new LoginFailure(LoginFailureReason.ServerNonResponsive, 0);

            //send out this login failure event
            userEventBus.post(failure);
        }
    };


    //this gets called each time someone subscribes to the usereventbus
    //this way, you get an immediate callback of the user info -- to sycnrhonously decide
    //how to proceed inside the fragment/activity
    @Produce public CurrentUserInformation currentUserInformation()
    {
        //TODO: Pull user info from cached information if it's null
        return new CurrentUserInformation(currentAPIToken, lastActiveFacebookAccessToken);
    }

    private void userOfficiallyLoggedIn()
    {
        //let's go ahead and let anyone interested know that the user is offficially logged in
        userIsLoggedIn = true;

        //sound out the current api token in an events
        userEventBus.post(new UserLoggedInEvent(currentAPIToken));
    }


}
