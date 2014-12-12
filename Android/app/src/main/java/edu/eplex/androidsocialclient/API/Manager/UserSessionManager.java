package edu.eplex.androidsocialclient.API.Manager;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.eplex.androidsocialclient.API.LoginAPI;
import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.LoginRequest;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import edu.eplex.androidsocialclient.API.Objects.User;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.FragmentFlowManager;
import edu.eplex.androidsocialclient.Utilities.UserEmailFetcher;
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
        ServerUserNotInitialized,
        ServerInvalidAPIToken,
        ServerNon200Status,
        ServerNonResponsive,

        FacebookException,

        GoogleAuthorizationRecovered,
        GoogleNetworkError,
        UnauthorizedGoogleUser,
        UnknownGoogleTokenError
    }
    public class LoginFailure {
        public LoginFailureReason loginFailureReason;
        public int htmlStatus;
        public String reason;

        public LoginFailure(LoginFailureReason lfr, int htmlStatus)
        {
            this.loginFailureReason = lfr;
            this.htmlStatus = htmlStatus;
        }

        public LoginFailure(LoginFailureReason lfr, int htmlStatus, String reason)
        {
            this.loginFailureReason = lfr;
            this.htmlStatus = htmlStatus;
            this.reason = reason;
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

    public class UserLoggedOutEvent
    {
    }

    //tag it up
    private static String TAG = "UserSessionManager";

    //for handling call to google token auth function
    private static final int REQUEST_CODE_TOKEN_AUTH =  1;


    //change this for more permissions
    private List<String> fbReadPermissions = Arrays.asList("public_profile", "email");



    //handle all user related calls across the app
    Bus userEventBus = new Bus();
    private List<Object> registeredObjects = new ArrayList<Object>();

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

    public void logoutUser(Fragment parentFragment)
    {
        //let's go ahead and let anyone interested know that the user is GONE
        userIsLoggedIn = false;

        //clear out facebook
        lastActiveFacebookAccessToken = null;
        clearFacebookSessions(parentFragment);

        //delete our local tokens
        currentAPIToken = null;

        //sound out the current api token in an events
        userEventBus.post(new UserLoggedOutEvent());
    }

    //register/unregister on the bus
    public void register(Object eventObject)
    {
        //no duplicate registering
        if(registeredObjects.contains(eventObject))
            return;

        //register to our list and to the bus
        registeredObjects.add(eventObject);
        userEventBus.register(eventObject);
    }
    public void unregister(Object eventObject)
    {
        if(!registeredObjects.contains(eventObject))
            return;

        //unregister and remove from our list of existing registered objects
        registeredObjects.remove(eventObject);
        userEventBus.unregister(eventObject);
    }

    //FACEBOOK LOGIN HANDLING

    //we have our helper for passing on activity results
    //must be called by all fragments interested in user information!
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(uiHelper != null)
            uiHelper.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_CODE_TOKEN_AUTH:

                //now we have our answer
                if(resultCode == Activity.RESULT_OK)
                {
                    //go ahead and request AGAIN -- same procedure -- let an error be known though for repeat attempts
                    userEventBus.post(new LoginFailure(LoginFailureReason.GoogleAuthorizationRecovered, 0));
                }

                break;
        }
    }
    public void onSaveInstanceState(Bundle outState) {
        if(uiHelper != null)
            uiHelper.onSaveInstanceState(outState);
    }


    void loadAPIService(Fragment fragment) {
        try {
            //will send facebook access token info and attempt to requisition an api token
            if (apiService == null)
                apiService = APIManager.getInstance().createLoginAPI(fragment.getActivity());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    //we only ever open the session for reading
    public void getUserFBInformation(Fragment parentFragment)
    {
        getUserFBInformation(parentFragment, fbReadPermissions);
    }

    void clearFacebookSessions(Fragment parentFragment)
    {
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

    }

    public void getUserFBInformation(Fragment parentFragment, List<String> permissions)
    {
        //make sure to load up our api service for later access
        loadAPIService(parentFragment);

        if(currentUILifeCycleActivity != parentFragment.getActivity())
        {
            //store this activity so that we don't keep duplicating the uiHelper object
            currentUILifeCycleActivity = parentFragment.getActivity();

            //grab any session changes in callback
            uiHelper = new UiLifecycleHelper(parentFragment.getActivity(), callback);
        }

        //clear out any previous sessions that existed
        clearFacebookSessions(parentFragment);

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

        if(exception != null) {
            Log.d(TAG, exception.toString());

            //facebook exception -- let it be known -- we've failed a login step
            userEventBus.post(new LoginFailure(LoginFailureReason.FacebookException, 0, exception.getMessage()));

            return;
        }
        //we only care about the open sessions, that's it
        if(session.isOpened())
        {
            //get our latest access token
            final String fbAccessToken = session.getAccessToken();

            //store the last known access token
            lastActiveFacebookAccessToken = new AccessToken(fbAccessToken);

            //with this information, we're ready to attempt login now -- let the world know
            userEventBus.post(new ReadyForEvent(RegistrationType.UserLoginWithFacebook, lastActiveFacebookAccessToken));

            //we let them know about the login readiness, but we will continue towards user login now
            loginUserWithFacebook();
        }
    }

    //USER LOGIN HANDLING



    boolean loginUserWithFacebook()
    {
        if(lastActiveFacebookAccessToken == null || lastActiveFacebookAccessToken.access_token == null)
        {
            //can't make a user login request without an existing access token
            return false;
        }

        //make an async request to the servers with this newly discovered facebook info
        apiService.asyncFacebookLoginRequest(lastActiveFacebookAccessToken, userLoginWithFacebookCallback);

        return true;
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

    //USER LOGIN USERNAME/PASSWORD
    public void loginWithUsernamePassword(Fragment parentFragment, LoginRequest loginRequest) {

        //make sure to load up our api service for later access
        loadAPIService(parentFragment);

        //attempt login please
        apiService.asyncLoginRequest(loginRequest, userLoginUsernamePasswordCallback);
    }

    public boolean signupWithFacebook(OAuth2Signup signupRequest)
    {
        //must have all of these in order to continue -- instant fail
        if(apiService == null
                || lastActiveFacebookAccessToken == null
                || lastActiveFacebookAccessToken.access_token == null
                || currentAPIToken.user == null
                || currentAPIToken.user.user_id == null
                )
            return false;

        //ready to signup iwth our facebooks
        signupRequest.api_token = lastActiveFacebookAccessToken.access_token;
        //then we associate our request with the previously acquired user_id -- during facebook signup attempt
        signupRequest.user_id = currentAPIToken.user.user_id;

        //let our request out into the wild
        apiService.asyncFacebookSignup(signupRequest, userSignupWithFacebookCallback);

        return true;
    }

    ///USER SIGN UP HANDLING
    public void signupWithEmail(Fragment parentFragment, final OAuth2Signup signupRequest)
    {
        //need to fetch api service using fragment -- signup with email has never used it before
        loadAPIService(parentFragment);

        //we must erase any known facebook affiliation
        lastActiveFacebookAccessToken = null;

        //then importantly we must fetch a google token for authentication

        asyncRetrieveGoogleToken(parentFragment, new GoogleTokenCallback() {
            @Override
            public void onGoogleTokenRetrieved(String token) {
                //google token retrieved, time for some good old fashioned async email signup
                signupRequest.api_token = token;

                //send it out!
                apiService.asyncEmailSignup(signupRequest, userSignupWithEmailCallback);

            }

            @Override
            public void onGoogleTokenError(Exception e) {
                if (e instanceof IOException) {
                    // Network or server error, try later
                    Log.e(TAG, e.toString());
                    userEventBus.post(new LoginFailure(LoginFailureReason.GoogleNetworkError, 0));

                } else if (e instanceof GoogleAuthException) {
                    // The call is not ever expected to succeed
                    // assuming you have already verified that
                    // Google Play services is installed.
                    Log.e(TAG, e.toString());
                    userEventBus.post(new LoginFailure(LoginFailureReason.UnauthorizedGoogleUser, 0));

                } else {
                    e.printStackTrace();
                    Log.e(TAG, e.toString());
                    userEventBus.post(new LoginFailure(LoginFailureReason.UnknownGoogleTokenError, 0));

                }

            }
        });

    }


    //GOOGLE TOKEN SECTION --
    private interface GoogleTokenCallback
    {
        public void onGoogleTokenRetrieved(String token);
        public void onGoogleTokenError(Exception e);
    }

    //We must retrieve a Google token asyncrhonously
    void asyncRetrieveGoogleToken(final Fragment parentFragment, final GoogleTokenCallback callback)
    {
        //grab user email for google specifically to create the token
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {

                String email = UserEmailFetcher.getEmail(parentFragment.getActivity());
                String token = null;

                try {

                    token = GoogleAuthUtil.getToken(parentFragment.getActivity(),
                            email,
                            "audience:server:client_id:" + parentFragment.getResources().getString(R.string.google_client_id));

                } catch (UserRecoverableAuthException e) {
                    // Recover (with e.getIntent())
                    Log.e(TAG, e.toString());
                    Intent recover = e.getIntent();
                    parentFragment.startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);

                }
                catch (Exception e)
                {
                    //handle all other unrecoverable exceptions here
                    callback.onGoogleTokenError(e);
                }

                callback.onGoogleTokenRetrieved(token);

                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                Log.i(TAG, "Access token retrieved:" + token);
            }

        };

        //start it up!
        task.execute();
    }

    //USER LOGIN/FBSIGNUP/EMAILSIGNUP RETROFIT CALLBACKS ----

    //this is where all the successful cases go! We check the api token info to see
    //if we're actually logged in successfully -- no weird errors
    void onUserLoginSuccess(APIToken apiToken, Response response)
    {
        //if our user is not initialized, we're ready to register, but we are not yet loggin in
        currentAPIToken = apiToken;

        if(apiToken.user.isInitialized)
        {
            //the user is initialized!
            if(apiToken.api_token != null)
            {
                //we're all logged in!
                userOfficiallyLoggedIn();
            }
            else
                userEventBus.post(new LoginFailure(LoginFailureReason.ServerInvalidAPIToken, 0));
        }
        else
            userEventBus.post(new LoginFailure(LoginFailureReason.ServerUserNotInitialized, 0));
    }

    //handle user login response
    Callback<APIToken> userLoginUsernamePasswordCallback = new Callback<APIToken>() {
        @Override
        public void success(APIToken apiToken, Response response) {
            onUserLoginSuccess(apiToken, response);
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

    //handle success/errors from signup with email
    Callback<APIToken> userSignupWithEmailCallback = new Callback<APIToken>() {
        @Override
        public void success(APIToken apiToken, Response response) {
           onUserLoginSuccess(apiToken, response);
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

    //handle user signup response with facebook related errors
    Callback<APIToken> userSignupWithFacebookCallback = new Callback<APIToken>() {
        @Override
        public void success(APIToken apiToken, Response response) {
            onUserLoginSuccess(apiToken, response);
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
}
