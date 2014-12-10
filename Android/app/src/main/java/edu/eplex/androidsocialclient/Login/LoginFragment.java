package edu.eplex.androidsocialclient.Login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.Arrays;
import java.util.logging.Handler;

import edu.eplex.androidsocialclient.API.LoginAPI;
import edu.eplex.androidsocialclient.API.Manager.APIManager;
import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import edu.eplex.androidsocialclient.API.Objects.UsernameCheck;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.FragmentFlowManager;

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

    private Button registerEmailButton;
    private Button loginButton;

    private MaterialEditText usernameEditText;
    private View usernameLinearLayout;
    private MaterialEditText passwordEditText;
    private View passwordLinearLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        //hide that shit yo!
//        getActivity().setTheme(R.style.HiddenActionTheme);

//        getActivity().getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
//        getActivity().getActionBar().hide();

        super.onCreate(savedInstanceState);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

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
        fbLogin.getBackground().setAlpha(getResources().getInteger(R.integer.percent_alpha));

        //tie fb login button results to this fragment and would handle changing the login/logout button
        fbLogin.setFragment(this);

        //ask for public profile and email access
        fbLogin.setReadPermissions(Arrays.asList("public_profile", "email"));

        //if we want to register for callbacks, but not change the login/logout button
//        fbLogin.setSessionStatusCallback(callback);

        registerEmailButton = (Button)rootView.findViewById(R.id.register_email_button);
        loginButton = (Button) rootView.findViewById(R.id.login_button);

        usernameEditText = (MaterialEditText)rootView.findViewById(R.id.username_edit_text);
        usernameLinearLayout = rootView.findViewById(R.id.username_linear_layout);

        passwordEditText = (MaterialEditText)rootView.findViewById(R.id.password_edit_text);
        passwordLinearLayout = rootView.findViewById(R.id.password_linear_layout);


        registerEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //transfer over to registered please
                FragmentFlowManager.getInstance().homeSignupFacebookOrEmail(getActivity());

//                //create a registration fragment -- handles signing up with e-mail
//                //or alternatively, signing up once logged in with facebook
//                RegisterFragment rf = new RegisterFragment();
//
//                //add register fragment to the stack
//                getActivity().getSupportFragmentManager()
//                        .beginTransaction()
//                        .replace(android.R.id.content, rf)
//                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
//                        .addToBackStack(null)
//                        .commit();
            }
        });


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginViews();
            }
        });


        return rootView;
    }

    void showLoginViews()
    {
        //fade out email/fb, fade in username/password combos
        Animation fadeOutEmail = alphaAnimationFromTo(registerEmailButton, 0f, 2000, false, false);
        Animation fadeOutFBLogin = alphaAnimationFromTo(fbLogin, 0f, 2000, true, false);

        //no clicky clicky
        registerEmailButton.setEnabled(false);
        //when finished with this animation, focus on the username entry
        fadeOutEmail.setAnimationListener(focusUsernameOnCompleteListener(registerEmailButton, true, false));
        registerEmailButton.startAnimation(fadeOutEmail);

        //don't click during animation!
        fbLogin.setEnabled(false);
        fbLogin.startAnimation(fadeOutFBLogin);

        usernameLinearLayout.setVisibility(View.VISIBLE);
        passwordLinearLayout.setVisibility(View.VISIBLE);

        Animation fadeInUsername = alphaAnimationFromTo(usernameLinearLayout, 0.011f, 1.0f, 3000, false, false);
        Animation fadeInPassword = alphaAnimationFromTo(passwordLinearLayout, 0.001f, 1.0f, 3000, false, false);

        usernameLinearLayout.requestLayout();
        usernameLinearLayout.startAnimation(fadeInUsername);

        passwordLinearLayout.requestLayout();
        passwordLinearLayout.startAnimation(fadeInPassword);
    }

    void hideLoginViews()
    {

    }

    //create an alpha animation from the current value to the desired target
    private Animation alphaAnimationFromTo(final View view, float fromValue, float toValue, int msDuration, final boolean hideOnComplete, final boolean showOnStart)
    {
        AlphaAnimation alphaAnimation = new AlphaAnimation(fromValue, toValue);
        alphaAnimation.setDuration(msDuration);

        if(hideOnComplete || showOnStart) {
            alphaAnimation.setAnimationListener(defaultHideShowListener(view, hideOnComplete, showOnStart));
        }

        return alphaAnimation;
    }
    //create an alpha animation from the current value to the desired target
    private Animation alphaAnimationFromTo(final View view, float toValue, int msDuration, final boolean hideOnComplete, final boolean showOnStart)
    {
        AlphaAnimation alphaAnimation = new AlphaAnimation(view.getAlpha(), toValue);
        alphaAnimation.setDuration(msDuration);

        if(hideOnComplete || showOnStart) {
            alphaAnimation.setAnimationListener(defaultHideShowListener(view, hideOnComplete, showOnStart));
        }

        return alphaAnimation;
    }

    private Animation.AnimationListener focusUsernameOnCompleteListener(final View view, final boolean hideOnComplete, final boolean showOnStart)
    {
        return new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (showOnStart)
                    view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                if (hideOnComplete)
                    view.setVisibility(View.INVISIBLE);

                //ask for focus when done
//                usernameEditText.requestFocus();
                //open up our keyboard when the animation finishes
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(usernameEditText, InputMethodManager.SHOW_IMPLICIT);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
    }

    private Animation.AnimationListener defaultHideShowListener(final View view, final boolean hideOnComplete, final boolean showOnStart)
    {
        return new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (showOnStart)
                    view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                if (hideOnComplete)
                    view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
    }


    //handle register click by adding the RegisterFragment to our stack
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


                        if(apiTokenReturn.user != null && apiTokenReturn.user.isInitialized)
                        {
                            //we're signed in! should we check we have access?
                            APIToken confirmUserAccess = apiService.syncVerifyAPIAccess(apiTokenReturn.api_token);


                            Log.d(TAG, "User verified? " + confirmUserAccess.user.user_id);


                        }
                        else if(apiTokenReturn.user != null && !apiTokenReturn.user.isInitialized)
                        {
                            OAuth2Signup signupInfo = new OAuth2Signup();
                            signupInfo.api_token = apiTokenReturn.api_token;
                            signupInfo.user_id = apiTokenReturn.user.user_id;
                            signupInfo.username = "bobsyouruncle";
                            signupInfo.password = "bob22";
                            signupInfo.email = apiTokenReturn.user.email;

                            //in theory we are signed up, let's also verify
                            APIToken signupConfirmation = apiService.syncFacebookSignup(signupInfo);

                            //we're signed in! should we check we have access?
                            APIToken confirmUserAccess = apiService.syncVerifyAPIAccess(signupConfirmation.api_token);

                            Log.d(TAG, "User verified after signup? " + confirmUserAccess.user.isInitialized);

                        }




//                        UsernameCheck check = apiService.syncUsernameCheck("bobsyouruncle");

//                        Log.d(TAG, "Username available? " + check.isAvailable);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

//            thread.start();



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
