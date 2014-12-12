package edu.eplex.androidsocialclient.Login;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.InputType;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.otto.Subscribe;

import java.security.spec.ECField;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import edu.eplex.androidsocialclient.API.LoginAPI;
import edu.eplex.androidsocialclient.API.Manager.APIManager;
import edu.eplex.androidsocialclient.API.Manager.UserSessionManager;
import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.LoginRequest;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import edu.eplex.androidsocialclient.API.Objects.UsernameCheck;
import edu.eplex.androidsocialclient.Login.CustomUI.FBLoginButton;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.FragmentFlowManager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by paul on 12/3/14.
 */
public class LoginFragment extends Fragment {

    //Tag for logging
    private static final String TAG = "LoginFragment";

    private static final int FADE_OUT_BUTTON_ANIMATION_MS_DURATION = 900;
    private static final int FADE_IN_BUTTON_ANIMATION_MS_DURATION = 1300;

    private static final int FADE_OUT_EDIT_ANIMATION_MS_DURATION = 900;
    private static final int FADE_IN_EDIT_ANIMATION_MS_DURATION = 1300;

    //Helper for life cycle maintenance in FB
//    private UiLifecycleHelper uiHelper;
    private FBLoginButton fbLogin;
    private LoginAPI apiService;

    private Button registerEmailButton;
    private Button loginButton;
    private Button cancelLoginButton;

    private MaterialEditText usernameEditText;
    private View usernameLinearLayout;
    private MaterialEditText passwordEditText;
    private View passwordLinearLayout;

    private LinearLayout loginLinearLayout;
    private RelativeLayout fbButtonHolder;

    private boolean readyToLogin = false;
    private boolean facebookRequested = false;
    private float originalWeightSum = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        //hide that shit yo!
//        getActivity().setTheme(R.style.HiddenActionTheme);

//        getActivity().getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
//        getActivity().getActionBar().hide();

        super.onCreate(savedInstanceState);

        try {
            if (apiService == null)
                apiService = APIManager.getInstance().createLoginAPI(getActivity());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        UserSessionManager.getInstance().register(this, this);


//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        //whenever opening the login screen, kill the session if one exists please
//        Session s = Session.getActiveSession();
//        if(s != null && s.isOpened())
//            s.closeAndClearTokenInformation();
//
//        //clear out the cache as well please -- no need for FB info to be stored locally
//        s = Session.openActiveSessionFromCache(getActivity());
//
//        //clear it out please
//        if(s != null && s.isOpened())
//            s.closeAndClearTokenInformation();

//        uiHelper = new UiLifecycleHelper(getActivity(), callback);
//        uiHelper.onCreate(savedInstanceState);
    }

    //callback that handles change in Login/Logout status
//    private Session.StatusCallback callback = new Session.StatusCallback() {
//        @Override
//        public void call(Session session, SessionState state, Exception exception) {
//            onSessionStateChange(session, state, exception);
//        }
//    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_login, container, false);

        fbButtonHolder = (RelativeLayout)rootView.findViewById(R.id.fbButtonHolder);
        fbLogin = (FBLoginButton) rootView.findViewById(R.id.authButton);
        fbLogin.getBackground().setAlpha(getResources().getInteger(R.integer.percent_alpha));

        //pass this fragment info onwards for calling activity with result
        final Fragment self = this;
        fbLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                facebookRequested = true;
                //one click until resolved please
                fbLogin.setEnabled(false);

                //try to log us in please!
                UserSessionManager.getInstance().getUserFBInformation(self);
            }
        });

        //tie fb login button results to this fragment and would handle changing the login/logout button
//        fbLogin.setFragment(this);

        //ask for public profile and email access
//        fbLogin.setReadPermissions(Arrays.asList("public_profile", "email"));

        //if we want to register for callbacks, but not change the login/logout button
//        fbLogin.setSessionStatusCallback(callback);

        registerEmailButton = (Button)rootView.findViewById(R.id.register_email_button);
        loginButton = (Button) rootView.findViewById(R.id.login_button);
        cancelLoginButton = (Button) rootView.findViewById(R.id.cancel_button);

        loginLinearLayout = (LinearLayout)rootView.findViewById(R.id.login_linear_layout);

        usernameEditText = (MaterialEditText)rootView.findViewById(R.id.username_edit_text);
        usernameLinearLayout = rootView.findViewById(R.id.username_linear_layout);

        passwordEditText = (MaterialEditText)rootView.findViewById(R.id.password_edit_text);
        passwordLinearLayout = rootView.findViewById(R.id.password_linear_layout);

        //disable auto correct please
        usernameEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

//        passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEditText.setTypeface(usernameEditText.getTypeface());

        registerEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //logout the user -- clear anything in there before heading to register with email
                UserSessionManager.getInstance().logoutUser(self);

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

                if(!readyToLogin)
                    showLoginViews();
                else
                    attemptUserLogin();
            }
        });

        cancelLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if canceled when we are ready to login, then we need to hide
                if(readyToLogin)
                    hideLoginViews();
            }
        });


        return rootView;
    }

    void showLoginViews()
    {
        //fade out email/fb, fade in username/password combos
        Animation fadeOutEmail = alphaAnimationFromTo(registerEmailButton, 1.0f, 0.01f, FADE_OUT_BUTTON_ANIMATION_MS_DURATION, false, false);
        Animation fadeOutFBLogin = alphaAnimationFromTo(fbButtonHolder, 1.0f, 0.01f, FADE_OUT_BUTTON_ANIMATION_MS_DURATION, true, false);

        //no clicky clicky
        registerEmailButton.setEnabled(false);
        //when finished with this animation, focus on the username entry
        fadeOutEmail.setAnimationListener(finishLoginShowOnCompleteListener(registerEmailButton, 0.01f, true, false));
        registerEmailButton.startAnimation(fadeOutEmail);

        //don't click during animation!
        fbLogin.setEnabled(false);
        fbButtonHolder.startAnimation(fadeOutFBLogin);

        //do not enable login button till you can actually login again
        loginButton.setEnabled(false);
        cancelLoginButton.setEnabled(false);

        usernameLinearLayout.setVisibility(View.VISIBLE);
        passwordLinearLayout.setVisibility(View.VISIBLE);
        usernameLinearLayout.requestLayout();
        passwordLinearLayout.requestLayout();

        Animation fadeInUsername = alphaAnimationFromTo(usernameLinearLayout, 0.01f, 1.0f, FADE_IN_EDIT_ANIMATION_MS_DURATION, false, false);
        Animation fadeInPassword = alphaAnimationFromTo(passwordLinearLayout, 0.01f, 1.0f, FADE_IN_EDIT_ANIMATION_MS_DURATION, false, false);

        usernameLinearLayout.startAnimation(fadeInUsername);
        passwordLinearLayout.startAnimation(fadeInPassword);


        float ws = loginLinearLayout.getWeightSum();
        if(originalWeightSum == 0)
            originalWeightSum = ws;

        ObjectAnimator anim = ObjectAnimator.ofFloat(loginLinearLayout, "weightSum", ws, 2.1f);
        anim.setDuration(FADE_OUT_BUTTON_ANIMATION_MS_DURATION);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                loginLinearLayout.requestLayout();
            }
        });
        anim.start();
    }

    void hideKeyboard(View view)
    {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    void showKeyboard(View view)
    {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    void completeLoginShow()
    {
        //ask for focus when done
//                usernameEditText.requestFocus();
        //open up our keyboard when the animation finishes
        showKeyboard(usernameEditText);
        loginButton.setEnabled(true);
        cancelLoginButton.setEnabled(true);
        readyToLogin = true;
    }

    void hideLoginViews()
    {
        //hide the keyboard before starting animation
        hideKeyboard(usernameEditText);

        //make sure these are visible
        registerEmailButton.setVisibility(View.VISIBLE);
        fbButtonHolder.setVisibility(View.VISIBLE);

        //fade out email/fb, fade in username/password combos
        Animation fadeInEmail = alphaAnimationFromTo(registerEmailButton, 0.01f, 1.0f, FADE_IN_BUTTON_ANIMATION_MS_DURATION, false, false);
        Animation fadeInFBLogin = alphaAnimationFromTo(fbButtonHolder, 0.01f, 1.0f, FADE_IN_BUTTON_ANIMATION_MS_DURATION, false, false);

        //no clicky clicky during
        registerEmailButton.setEnabled(false);
        //when finished with this animation, focus on the username entry
        fadeInEmail.setAnimationListener(finishLoginHideOnCompleteListener(registerEmailButton, 1.0f, false, false));
        registerEmailButton.requestLayout();
        registerEmailButton.startAnimation(fadeInEmail);

        //don't click during animation!
        fbLogin.setEnabled(false);
        fbButtonHolder.requestLayout();
        fbButtonHolder.startAnimation(fadeInFBLogin);

        loginButton.setEnabled(false);
        cancelLoginButton.setEnabled(false);

        //fade out email/fb, fade in username/password combos
        Animation fadeOutUsername = alphaAnimationFromTo(usernameLinearLayout, 1.0f, 0.01f, FADE_OUT_EDIT_ANIMATION_MS_DURATION, true, false);
        Animation fadeOutPassword = alphaAnimationFromTo(passwordLinearLayout, 1.0f, 0.01f, FADE_OUT_EDIT_ANIMATION_MS_DURATION, true, false);

        //fade out the edit text boxes :)
        usernameLinearLayout.startAnimation(fadeOutUsername);
        passwordLinearLayout.startAnimation(fadeOutPassword);

        float ws = loginLinearLayout.getWeightSum();

        //return to the original weight sum, thanks
        ObjectAnimator anim = ObjectAnimator.ofFloat(loginLinearLayout, "weightSum", ws, originalWeightSum);
        anim.setDuration(FADE_IN_BUTTON_ANIMATION_MS_DURATION);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                loginLinearLayout.requestLayout();
            }
        });
        anim.start();
    }

    void finishHidingLoginView()
    {
        readyToLogin = false;
        loginButton.setEnabled(true);
        cancelLoginButton.setEnabled(true);
        registerEmailButton.setEnabled(true);
        fbLogin.setEnabled(true);
    }

    void attemptUserLogin()
    {
        //we attempt to take our username/password combo and send it to the server
        //locally deny when not matching properly
        if(passwordEditText.getText().toString().length() < 6)
            invalidPasswordLength();
        else
        {
            //Attempt login with username password -- need to be told of failure!
            UserSessionManager.getInstance().loginWithUsernamePassword(
                    this,
                    new LoginRequest(usernameEditText.getText().toString(), passwordEditText.getText().toString()));
//            try {
//                //attempt login please
//                apiService.asyncLoginRequest(new LoginRequest(usernameEditText.getText().toString(), passwordEditText.getText().toString()), new Callback<APIToken>() {
//                    @Override
//                    public void success(APIToken apiToken, Response response) {
//
//                        if (apiToken.user != null && apiToken.api_token != null) {
//                            //hide keyboard from the user, no more logging in for them!
//                            hideKeyboard(usernameEditText);
//                            //success! save the user, then log us in for real!
//                            //temporary flow to next activity here
//                            FragmentFlowManager.getInstance().tempLaunchUserSettings(getActivity());
//                        } else {
//                            //display the error to the user
//                            displayLoginError("Login Failed", "Unknown Server Error");
//                        }
//                    }
//
//                    @Override
//                    public void failure(RetrofitError error) {
//
////                        switch (error.getResponse().getStatus()) {
////                            case 500:
//                                //server error -- but for now, ALL ERRORS -- TODO: fix that please
//
//                                //display the error to the user
//                                displayLoginError("Login Failed", "Invalid username/password");
//
////                                break;
////                            default:
////                                //display the error to the user
////                                displayLoginError("Login Failed", "Unknown Server Error");
////
////                                break;
////                        }
//
//                    }
//                });
//            }
//            catch (Exception e)
//            {
//                //why did this fail!
//                e.printStackTrace();
//            }
        }

    }
    void displayLoginError(String errorTitle, String errorMessage)
    {
        new MaterialDialog.Builder(getActivity())
                .title(errorTitle)
                .content(Html.fromHtml(errorMessage))
                .titleAlignment(Alignment.CENTER)
                .contentAlignment(Alignment.CENTER)
                .positiveText("Okay")
                .callback(new MaterialDialog.SimpleCallback() {
                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        materialDialog.hide();
                    }
                })
                .show();
    }
    void invalidPasswordLength()
    {
        new MaterialDialog.Builder(getActivity())
                .title("Invalid Password Format")
                .content(Html.fromHtml("Incorrect password length, must be at least 6 characters."))
                .titleAlignment(Alignment.CENTER)
                .contentAlignment(Alignment.CENTER)
                .positiveText("Okay")
                .callback(new MaterialDialog.Callback() {
                    @Override
                    public void onNegative(MaterialDialog materialDialog) {
                        //easy, just hide ourselves!
                        materialDialog.hide();
                    }

                    @Override
                    public void onPositive(MaterialDialog materialDialog) {
                        materialDialog.hide();
                    }
                })
                .show();
    }


    //create an alpha animation from the current value to the desired target
    private Animation alphaAnimationFromTo(final View view, float fromValue, float toValue, int msDuration, final boolean hideOnComplete, final boolean showOnStart)
    {
        AlphaAnimation alphaAnimation = new AlphaAnimation(fromValue, toValue);
        alphaAnimation.setDuration(msDuration);

        //set the current from alpha value
//        view.setAlpha(fromValue);

        if(hideOnComplete || showOnStart) {
            alphaAnimation.setAnimationListener(defaultHideShowListener(view, toValue, hideOnComplete, showOnStart));
        }

        return alphaAnimation;
    }
    //create an alpha animation from the current value to the desired target
    private Animation alphaAnimationFromTo(final View view, float toValue, int msDuration, final boolean hideOnComplete, final boolean showOnStart)
    {
        AlphaAnimation alphaAnimation = new AlphaAnimation(view.getAlpha(), toValue);
        alphaAnimation.setDuration(msDuration);

        if(hideOnComplete || showOnStart) {
            alphaAnimation.setAnimationListener(defaultHideShowListener(view, toValue, hideOnComplete, showOnStart));
        }

        return alphaAnimation;
    }

    private Animation.AnimationListener finishLoginShowOnCompleteListener(final View view, final float toValue, final boolean hideOnComplete, final boolean showOnStart)
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

//                view.setAlpha(toValue);

                completeLoginShow();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
    }

    private Animation.AnimationListener finishLoginHideOnCompleteListener(final View view, final float toValue, final boolean hideOnComplete, final boolean showOnStart)
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

//                view.setAlpha(toValue);

                finishHidingLoginView();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
    }

    private Animation.AnimationListener defaultHideShowListener(final View view, final float toValue, final boolean hideOnComplete, final boolean showOnStart)
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

//                view.setAlpha(toValue);
            }


            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
    }


    @Subscribe
    public void readyForEvent(UserSessionManager.ReadyForEvent event) {

        switch (event.registrationType)
        {
            case UserLoginWithFacebook:

                //we've accessed our facebook info, now we're ready to make a request to our
                //servers before registering a user -- we are simply being informed of the readiness
                //nothing to be done here!


                break;
            case UserSignupWithFacebook:

                //user successfully contacted our server, now we're ready to shift to the registration page
                FragmentFlowManager.getInstance().homeSignupFacebookOrEmail(getActivity());

                break;
        }
    }

    @Subscribe
    public void userLoggedIn(UserSessionManager.UserLoggedInEvent loggedInUser)
    {

        //hide keyboard from the user -- if it exists, no more logging in for them!
        hideKeyboard(usernameEditText);

        //therefore, we need to switch to our new account status!
        FragmentFlowManager.getInstance().tempLaunchUserSettings(getActivity(), loggedInUser.apiToken.user.username);
    }

    @Subscribe
    public void loginFailure(UserSessionManager.LoginFailure failureEvent) {

        switch (failureEvent.loginFailureReason)
        {
            case FacebookException:
                displayLoginError("Facebook Login Error", "Unknown error while logging into Facebook");
                break;
            case ServerNon200Status:

                switch (failureEvent.htmlStatus)
                {
                    //TODO: this should be the correct html status number from the server
                    case 500:
                        displayLoginError("Login Failed!", "Invalid username/password");
                        break;

                    default:
                        displayLoginError("Error from Server", "Error contacting server: " + failureEvent.htmlStatus);
                        break;
                }
                break;
            case ServerNonResponsive:
                displayLoginError("Error from Server", "Server not responding. Try again in a moment.");
                break;
        }

        //we can now try again
        fbLogin.setEnabled(true);

    }

//
//    //handle register click by adding the RegisterFragment to our stack
//    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
//
//        if(exception != null)
//            Log.d(TAG, exception.toString());
//
//        //session state changed, re-enable
//        if(facebookRequested)
//            fbLogin.setEnabled(true);
//
//        if(facebookRequested && session.isOpened())
//        {
//            //one request per click thank you
//            facebookRequested = false;
//
//            //grab the access token, then make an async request to our servers to login
//            final String fbAccessToken = session.getAccessToken();
//
//            try {
//
//                if (apiService == null)
//                    apiService = APIManager.getInstance().createLoginAPI(getActivity());
//
//
//                //now we have our api service, let's try to send out this access token
//                AccessToken at = new AccessToken();
//                at.access_token = fbAccessToken;
//
//                //syncrhonously attempt to access server with facebook info!
//                apiService.asyncFacebookLoginRequest(at, new Callback<APIToken>() {
//                    @Override
//                    public void success(APIToken apiToken, Response response) {
//                        //now we have two options
//                        //either our user exists and is initialized -- that is, we just logged in
//                        //OR we haven't been initialized yet, so we need to send this info to our servers
//                        //either way, we'll need to store this info locally so that we can transfer it between fragments
//
//                        //we don't have a user
//                        if(apiToken.user == null)
//                        {
//                            displayLoginError("Unknown Server Error", "Failed to Login with Facebook");
//                            return;
//                        }
//
//                        //we know the user exists at this point, are they initialized?
//                        if(apiToken.user.isInitialized)
//                        {
//                            //we are an existing user, thus we are logged in -- take us home please
//                            FragmentFlowManager.getInstance().tempLaunchUserSettings(getActivity());
//                        }
//                        else
//                        {
//                            //not initialized -- thus we must take the user to the registration page
//                            //AFTER storing the user related callbacks
//                            FragmentFlowManager.getInstance().homeSignupFacebookOrEmail(getActivity());
//                        }
//                    }
//
//                    @Override
//                    public void failure(RetrofitError error) {
//
//                        displayLoginError("Unknown Server Error", "Failed to Login with Facebook");
//                        return;
//
//                    }
//                });
//
//
//            }
//            catch (Exception e)
//            {
//                e.printStackTrace();
//            }
//
//
//            Thread thread = new Thread()
//            {
//                @Override
//                public void run() {
//
//                    //now we want to exchange our fb access token for one from our server
//                    try {
//                        if(apiService == null)
//                            apiService = APIManager.getInstance().createLoginAPI(getActivity());
//
//                        //now we have our api service, let's try to send out this access token
//                        AccessToken at = new AccessToken();
//                        at.access_token = fbAccessToken;
//
//                        //syncrhonously attempt to access server with facebook info!
//                        APIToken apiTokenReturn = apiService.syncFacebookLoginRequest(at);
//
//                        Log.d(TAG, "User exists? " + apiTokenReturn.user.isInitialized);
//
//
//                        if(apiTokenReturn.user != null && apiTokenReturn.user.isInitialized)
//                        {
//                            //we're signed in! should we check we have access?
//                            APIToken confirmUserAccess = apiService.syncVerifyAPIAccess(apiTokenReturn.api_token);
//
//
//                            Log.d(TAG, "User verified? " + confirmUserAccess.user.user_id);
//
//
//                        }
//                        else if(apiTokenReturn.user != null && !apiTokenReturn.user.isInitialized)
//                        {
//                            OAuth2Signup signupInfo = new OAuth2Signup();
//                            signupInfo.api_token = apiTokenReturn.api_token;
//                            signupInfo.user_id = apiTokenReturn.user.user_id;
//                            signupInfo.username = "bobsyouruncle";
//                            signupInfo.password = "bob22";
//                            signupInfo.email = apiTokenReturn.user.email;
//
//                            //in theory we are signed up, let's also verify
//                            APIToken signupConfirmation = apiService.syncFacebookSignup(signupInfo);
//
//                            //we're signed in! should we check we have access?
//                            APIToken confirmUserAccess = apiService.syncVerifyAPIAccess(signupConfirmation.api_token);
//
//                            Log.d(TAG, "User verified after signup? " + confirmUserAccess.user.isInitialized);
//
//                        }
//
//
//
//
////                        UsernameCheck check = apiService.syncUsernameCheck("bobsyouruncle");
//
////                        Log.d(TAG, "Username available? " + check.isAvailable);
//
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            };
//
////            thread.start();
//
//
//
//        } else if (session.isClosed()) {
//            Log.i(TAG, "Session is closed...");
//        }
//    }

    //Handle different App status
    @Override
    public void onResume() {
        super.onResume();

        UserSessionManager.getInstance().register(this, this);


        // For scenarios where the main activity is launched and user
        // session is not null, the session state change notification
        // may not be triggered. Trigger it if it's open/closed.
//        Session session = Session.getActiveSession();
//        if (session != null &&
//                (session.isOpened() || session.isClosed()) ) {
//            onSessionStateChange(session, session.getState(), null);
//        }

//        uiHelper.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //call our session manager for handling
        UserSessionManager.getInstance().onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onPause() {
        super.onPause();
//        uiHelper.onPause();
        //drop out of event updates please!
        UserSessionManager.getInstance().unregister(this, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save the current frag state in the user session
        UserSessionManager.getInstance().onSaveInstanceState(outState);
//        uiHelper.onSaveInstanceState(outState);
    }
}
