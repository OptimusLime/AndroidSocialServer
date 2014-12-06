package edu.eplex.androidsocialclient.Login;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;

import java.util.Arrays;
import java.util.logging.Handler;

import edu.eplex.androidsocialclient.API.LoginAPI;
import edu.eplex.androidsocialclient.API.Manager.APIManager;
import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import edu.eplex.androidsocialclient.API.Objects.UsernameCheck;
import edu.eplex.androidsocialclient.R;

/**
 * Created by paul on 12/3/14.
 */
public class LoginFragment extends Fragment {

    //Tag for logging
    private static final String TAG = "LoginFragment";

    //Helper for life cycle maintenance in FB
    private UiLifecycleHelper uiHelper;
    private LoginButton fbLogin;
    private LoginAPI apiService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
    }

    //callback that handles change in Login/Logout status
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_login, container, false);

        fbLogin = (LoginButton) rootView.findViewById(R.id.authButton);

        //tie fb login button results to this fragment and would handle changing the login/logout button
        fbLogin.setFragment(this);

        //ask for public profile and email access
        fbLogin.setReadPermissions(Arrays.asList("public_profile", "email"));

        //if we want to register for callbacks, but not change the login/logout button
//        fbLogin.setSessionStatusCallback(callback);

        return rootView;
    }


    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (state.isOpened()) {
            Log.i(TAG, "Logged in...");

            final String fbAccessToken = session.getAccessToken();

            Thread thread = new Thread()
            {
                @Override
                public void run() {

                    //now we want to exchange our fb access token for one from our server
                    try {
                        if(apiService == null)
                            apiService = APIManager.getInstance().createLoginAPI();

                        //now we have our api service, let's try to send out this access token
                        AccessToken at = new AccessToken();
                        at.access_token = fbAccessToken;

                        //syncrhonously attempt to access server with facebook info!
                        APIToken apiTokenReturn = apiService.syncFacebookLoginRequest(at);

                        Log.d(TAG, "User exists? " + apiTokenReturn.user.isInitialized);

                        OAuth2Signup signupInfo = new OAuth2Signup();
                        signupInfo.api_token = apiTokenReturn.api_token;
                        signupInfo.user_id = apiTokenReturn.user.user_id;
                        signupInfo.username = "bobsyouruncle";
                        signupInfo.password = "bob22";
                        signupInfo.email = apiTokenReturn.user.email;

                        APIToken signupConfirmation = apiService.syncFacebookSignup(signupInfo);


                        UsernameCheck check = apiService.syncUsernameCheck("bobsyouruncle");

                        Log.d(TAG, "Username available? " + check.isAvailable);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.start();



        } else if (state.isClosed()) {
            Log.i(TAG, "Logged out...");
        }
    }

    //Handle different App status
    @Override
    public void onResume() {
        super.onResume();

        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
        Session session = Session.getActiveSession();
        if (session != null &&
                (session.isOpened() || session.isClosed()) ) {
            onSessionStateChange(session, session.getState(), null);
        }

        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }
}
