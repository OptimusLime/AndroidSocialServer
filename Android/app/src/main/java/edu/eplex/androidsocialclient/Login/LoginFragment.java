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

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.security.spec.ECField;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.regex.Pattern;

import edu.eplex.androidsocialclient.API.LoginAPI;
import edu.eplex.androidsocialclient.API.Manager.APIManager;
import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.LoginRequest;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import edu.eplex.androidsocialclient.API.Objects.UsernameCheck;
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
    private UiLifecycleHelper uiHelper;
    private LoginButton fbLogin;
    private LoginAPI apiService;

    private Button registerEmailButton;
    private Button loginButton;
    private Button cancelLoginButton;

    private MaterialEditText usernameEditText;
    private View usernameLinearLayout;
    private MaterialEditText passwordEditText;
    private View passwordLinearLayout;

    private LinearLayout loginLinearLayout;

    private boolean readyToLogin = false;
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

//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

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
        Animation fadeOutFBLogin = alphaAnimationFromTo(fbLogin, 1.0f, 0.01f, FADE_OUT_BUTTON_ANIMATION_MS_DURATION, true, false);

        //no clicky clicky
        registerEmailButton.setEnabled(false);
        //when finished with this animation, focus on the username entry
        fadeOutEmail.setAnimationListener(finishLoginShowOnCompleteListener(registerEmailButton, 0.01f, true, false));
        registerEmailButton.startAnimation(fadeOutEmail);

        //don't click during animation!
        fbLogin.setEnabled(false);
        fbLogin.startAnimation(fadeOutFBLogin);

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
        fbLogin.setVisibility(View.VISIBLE);

        //fade out email/fb, fade in username/password combos
        Animation fadeInEmail = alphaAnimationFromTo(registerEmailButton, 0.01f, 1.0f, FADE_IN_BUTTON_ANIMATION_MS_DURATION, false, false);
        Animation fadeInFBLogin = alphaAnimationFromTo(fbLogin, 0.01f, 1.0f, FADE_IN_BUTTON_ANIMATION_MS_DURATION, false, false);

        //no clicky clicky during
        registerEmailButton.setEnabled(false);
        //when finished with this animation, focus on the username entry
        fadeInEmail.setAnimationListener(finishLoginHideOnCompleteListener(registerEmailButton, 1.0f, false, false));
        registerEmailButton.requestLayout();
        registerEmailButton.startAnimation(fadeInEmail);

        //don't click during animation!
        fbLogin.setEnabled(false);
        fbLogin.requestLayout();
        fbLogin.startAnimation(fadeInFBLogin);

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
            try {
                //attempt login please
                apiService.asyncLoginRequest(new LoginRequest(usernameEditText.getText().toString(), passwordEditText.getText().toString()), new Callback<APIToken>() {
                    @Override
                    public void success(APIToken apiToken, Response response) {

                        if (apiToken.user != null && apiToken.api_token != null) {
                            //hide keyboard from the user, no more logging in for them!
                            hideKeyboard(usernameEditText);
                            //success! save the user, then log us in for real!
                            //temporary flow to next activity here
                            FragmentFlowManager.getInstance().tempLaunchUserSettings(getActivity());
                        } else {
                            //display the error to the user
                            displayLoginError("Login Failed", "Unknown Server Error");
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {

//                        switch (error.getResponse().getStatus()) {
//                            case 500:
                                //server error -- but for now, ALL ERRORS -- TODO: fix that please

                                //display the error to the user
                                displayLoginError("Login Failed", "Invalid username/password");

//                                break;
//                            default:
//                                //display the error to the user
//                                displayLoginError("Login Failed", "Unknown Server Error");
//
//                                break;
//                        }

                    }
                });
            }
            catch (Exception e)
            {
                //why did this fail!
                e.printStackTrace();
            }
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
                            apiService = APIManager.getInstance().createLoginAPI(getActivity());

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
