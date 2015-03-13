package edu.eplex.androidsocialclient.Login;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.CharacterPickerDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.validation.RegexpValidator;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import edu.eplex.androidsocialclient.API.LoginAPI;
import edu.eplex.androidsocialclient.API.Manager.APIManager;
import edu.eplex.androidsocialclient.API.Manager.UserSessionManager;
import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import edu.eplex.androidsocialclient.API.Objects.UsernameCheck;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.FragmentFlowManager;
import edu.eplex.androidsocialclient.Utilities.ProgressWheel;
import edu.eplex.androidsocialclient.Utilities.UserEmailFetcher;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by paul on 12/3/14.
 */
public class RegisterFragment extends Fragment implements Callback<UsernameCheck>{

    //Tag for logging
    private static final String TAG = "RegisterFragment";
    //arbitrary number -- need user to sign in to access token
    private static final int REQUEST_CODE_TOKEN_AUTH =  1;

    private LoginAPI apiService;

    private MaterialEditText emailEditText;
    private MaterialEditText usernameEditText;
    private MaterialEditText passwordEditText;
    private ImageButton completeMenuButton;
    private View rootView;
    private ProgressWheel progressWheel;

    private boolean isEmailValid;
    private boolean lostEmailFocus;
    private boolean isUsernameValid;
    private boolean isPasswordValid;
    private boolean lostPasswordFocus;

    private AlphaAnimation progressAnimation;
    private boolean ignoreNextAnimationEnd;

    private boolean userSentFromFacebook;

    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    ScheduledFuture scheduledAPIRequest;
    private int USERNAME_CHECK_TIMEOUT = 2000;

    private boolean attachedToActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attachedToActivity = true;
//
//        //Listen for changes in the back stack
//        getActivity().getSupportFragmentManager().addOnBackStackChangedListener(this);

        try {

//            getActivity().getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
//            getActivity().getActionBar().show();

            if(apiService == null)
                apiService = APIManager.getInstance().createLoginAPI(getActivity());


        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.FLAG_FULLSCREEN);

//        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
//        getActivity().getWindow().clearFlags(
//                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
//

//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);
//

    }

   @Subscribe
   public void currentUserInformation(UserSessionManager.CurrentUserInformation userInfo)
    {
        //we were sent with Facebook info -- best make use of it for registering
        if(userInfo.lastKnownFacebookAccessToken != null && userInfo.lastKnownFacebookAccessToken.access_token != null)
            userSentFromFacebook = true;
        else
            userSentFromFacebook = false;

        //we have our user info, let's populate!
        if(userInfo.currentAPIToken != null && userInfo.currentAPIToken.user != null)
        {
            quickSetText(userInfo.currentAPIToken.user.email);
        }
        else
            quickSetText(null);
    }

    @Subscribe
    public void userLoggedIn(UserSessionManager.UserLoggedInEvent loggedInUser)
    {
        //only happens when we successfully get logged in
        //therefore, we need to switch to our new account status!
        FragmentFlowManager.getInstance().launchLoginLanding(getActivity(), loggedInUser.apiToken.user.username);

//        FragmentFlowManager.getInstance().tempLaunchUserSettings(getActivity(), loggedInUser.apiToken.user.username);
    }

    @Subscribe
    public void loginFailure(UserSessionManager.LoginFailure failureEvent) {

        switch (failureEvent.loginFailureReason)
        {
            case GoogleAuthorizationRecovered:
//                showRegistrationFailure("Google Authorized", "Please try again, Google authorization is now working.");
                asyncRegisterEmail();
                break;
            case GoogleNetworkError:
                showRegistrationFailure("Registration Failed", "Network error authorizing Google account. Try again.");
                break;
            case ServerNon200Status:

                switch (failureEvent.htmlStatus)
                {
                    //TODO: this should be the correct html status number from the server
                    case 500:
                        showRegistrationFailure("Signup Failed", "Server error. Try again in a moment.");
                        break;

                    default:
                        showRegistrationFailure("Error from Server", "Error contacting server: " + failureEvent.htmlStatus);
                        break;
                }
                break;
            case ServerNonResponsive:
                showRegistrationFailure("Error from Server", "Server not responding. Try again in a moment.");
                break;
            default:
                showRegistrationFailure("Error from Server", "Unkmown error signing up.");
                break;
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.registration_page, container, false);

//        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);


        progressWheel = (ProgressWheel)rootView.findViewById(R.id.username_progress_wheel);
//        progressWheel.setVisibility(View.INVISIBLE);
        instantHideProgressWheel();

        //set our text editing functions here
        emailEditText = (MaterialEditText)rootView.findViewById(R.id.email_edit_text);
        usernameEditText = (MaterialEditText)rootView.findViewById(R.id.username_edit_text);
        passwordEditText = (MaterialEditText)rootView.findViewById(R.id.password_edit_text);

        //correct the weird desire for textPassword input type to change the text typeface
        //-hack
        passwordEditText.setTypeface(usernameEditText.getTypeface());
        emailEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        usernameEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        emailEditText.addValidator(new RegexpValidator("Invalid Email", "^([a-z0-9_\\.-]+)@([\\da-z\\.-]+)\\.([a-z\\.]{2,6})$"));
        usernameEditText.addValidator(new RegexpValidator("Invalid Username", "^[a-z0-9_-]{1,16}$"));
        usernameEditText.setMaxCharacters(16);
        passwordEditText.addValidator(new RegexpValidator("Invalid Password", "^.{6,32}$"));


        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(lostEmailFocus) {
                    //check email valid locally
                    if(!isEmailValid)
                    {
                        isEmailValid = emailEditText.getValidators().get(0).isValid(emailEditText.getText().toString(),true);
                    }
                    else
                        isEmailValid = emailEditText.validate();

                    applyGreenRedIcon(
                            (ImageView) rootView.findViewById(R.id.register_email_icon),
                            isEmailValid,
                            R.drawable.ic_action_mail_green,
                            R.drawable.ic_action_mail_red);
                }
                else
                {
                    isEmailValid = emailEditText.getValidators().get(0).isValid(emailEditText.getText().toString(),true);

                    if(isEmailValid)
                        lostEmailFocus = true;

                    //we let it be known, even if you never lost focus, when you run into an acceptable password!
                    applyGreenRedIcon(
                            (ImageView) rootView.findViewById(R.id.register_email_icon),
                            isEmailValid,
                            R.drawable.ic_action_mail_green,
                            R.drawable.ic_action_mail);
                }
            }
        });

        emailEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                //once true, always true
                lostEmailFocus = !hasFocus || lostEmailFocus;

                if(!hasFocus)
                {
                    //check email valid locally
                    isEmailValid = emailEditText.validate();

                    applyGreenRedIcon(
                            (ImageView)rootView.findViewById(R.id.register_email_icon),
                            isEmailValid,
                            R.drawable.ic_action_mail_green,
                            R.drawable.ic_action_mail_red);
                }
            }
        });

        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                //any change to the text, hide the progress wheel -- we don't care about it anymore
//                instantHideProgressWheel();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {

                //validate it locally though
                boolean locallyValid = usernameEditText.validate();

                //we don't know if it's valid yet -- we will be told that after the return info
                isUsernameValid = false;

                if (locallyValid) {

                    //tell the user we're asking, even if it's premature -- it will feel better
                    //it will go away when the server responds
                    showProgressWheel();


                    scheduleAPIRequest(s.toString());
                    //we need to run a check for this username every so often

                    //we are going to apply the generic look until the username is confirmed by the server
                    applyGreenRedIcon(
                            (ImageView) rootView.findViewById(R.id.register_username_icon),
                            true,
                            R.drawable.ic_action_user,
                            R.drawable.ic_action_user_red);
                } else {
                    //if you're not valid, we aren't checking.
                    instantHideProgressWheel();
                    //we need to run a check for this username every so often
                    applyGreenRedIcon(
                            (ImageView) rootView.findViewById(R.id.register_username_icon),
                            false,
                            R.drawable.ic_action_user_green,
                            R.drawable.ic_action_user_red);
                }
            }
        });

        usernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if(!hasFocus)
                {
                    //check if valid in the correct way -- api callback
                    boolean meetsLocalRequirements = usernameEditText.validate();
                    isUsernameValid = false;

                    //if we satisfy our local requirements, we'll schedule a more formal check
                    if(meetsLocalRequirements)
                        scheduleAPIRequest(usernameEditText.getEditableText().toString());
                }
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                //we start checking with every letter after you already failed once -- or passed
                if(lostPasswordFocus) {
                    //check if the password is good yet!
                    if(!isPasswordValid)
                    {
                        isPasswordValid = passwordEditText.getValidators().get(0).isValid(passwordEditText.getText().toString(),true);
                    }
                    else
                        isPasswordValid = passwordEditText.validate();


                    //we need to run a check for this username every so often
                    applyGreenRedIcon(
                            (ImageView) rootView.findViewById(R.id.register_password_icon),
                            isPasswordValid,
                            R.drawable.ic_action_lock_closed_green,
                            R.drawable.ic_action_lock_closed_red);
                }
                else
                {
                    isPasswordValid = passwordEditText.getValidators().get(0).isValid(passwordEditText.getText().toString(),true);

                    if(isPasswordValid)
                        lostPasswordFocus = true;
                    //we let it be known, even if you never lost focus, when you run into an acceptable password!
                    applyGreenRedIcon(
                            (ImageView) rootView.findViewById(R.id.register_password_icon),
                            isPasswordValid,
                            R.drawable.ic_action_lock_closed_green,
                            R.drawable.ic_action_lock_closed);
                }
            }
        });

        passwordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                //always remember after we lost focus
                lostPasswordFocus = !hasFocus || lostPasswordFocus;

                if(!hasFocus) {

                    //validate our password when we go out of focus
                    isPasswordValid = passwordEditText.validate();

                    applyGreenRedIcon(
                            (ImageView) rootView.findViewById(R.id.register_password_icon),
                            isPasswordValid,
                            R.drawable.ic_action_lock_closed_green,
                            R.drawable.ic_action_lock_closed_red);
                }
            }
        });

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.registrationToolbar);
        ActionBarActivity abActivity = ((ActionBarActivity)getActivity());
        abActivity.setSupportActionBar(toolbar);
        abActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        abActivity.getSupportActionBar().setTitle("CREATE ACCOUNT");

        //confirm we want options callback
        setHasOptionsMenu(true);

        //we register for callbacks, this will actually
        UserSessionManager.getInstance().register(this, this);

        return rootView;
    }
    void quickSetText(String email){

        if(email == null)
        {
            String potentialEmail = UserEmailFetcher.getEmail(getActivity());

            //check again -- no null ness- regardless of fetching
            email = (potentialEmail != null) ? potentialEmail : "";
        }

        emailEditText.setText(email);
//        passwordEditText.setText("dododo");
//        usernameEditText.setText("checkface");
    }

    void applyGreenRedIcon(ImageView view, boolean validated, int drawGreen, int drawRed)
    {
        if(validated)
            view.setImageResource(drawGreen);
        else
            view.setImageResource(drawRed);

        //check to see if after we've applied this validation element, we know if we're overall validated
        checkReadyToFinish();
    }

    void scheduleAPIRequest(final String text)
    {
        //any text changes force us to abandon our previous request
        //and wait the full timeout before sending API calls
        if(scheduledAPIRequest != null)
            scheduledAPIRequest.cancel(true);

        //start timer to wait a second, then make a call to our API to check
        scheduledAPIRequest = executorService.schedule(new Runnable() {
            @Override
            public void run() {

                //begin the process of asking about the username
                //show our progress wheel, k thx
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        makeAsyncAPIRequest(text);
                    }
                });

            }
        }, USERNAME_CHECK_TIMEOUT, TimeUnit.MILLISECONDS);

    }

    //not thread safe, only call on the main thread please!
    void makeAsyncAPIRequest(String username)
    {
        //make the async call, handle when it gets back
        apiService.asyncUsernameCheck(username,this);

        //show our progress wheel, k thx
//        showProgressWheel();
    }

    //retrofit calls on the main thread, no worries about thread safe
    @Override
    public void success(UsernameCheck usernameCheck, Response response) {

        if(!attachedToActivity)
            return;

        //make sure we're setting the same things
        if(usernameEditText.getText().toString().equals(usernameCheck.username))
        {
            isUsernameValid = usernameCheck.isAvailable;

            //then we set it up -- was it available??
            applyGreenRedIcon(
                    (ImageView) rootView.findViewById(R.id.register_username_icon),
                    usernameCheck.isAvailable,
                    R.drawable.ic_action_user_green,
                    R.drawable.ic_action_user_red);

            if(isUsernameValid) {
                //animate to full progress bar -- before removing
                progressWheel.stopSpinning();
                progressWheel.setProgress(1.0f);
                progressWheel.setSpinSpeed(2.0f);
                progressWheel.setBarColor(getResources().getColor(R.color.indeterminate_progress_success));
            }
            else
            {
                progressWheel.stopSpinning();
                progressWheel.setInstantProgress(1.0f);
                progressWheel.setProgress(0f);
                progressWheel.setSpinSpeed(1.0f);
                progressWheel.setBarColor(getResources().getColor(R.color.indeterminate_progress_failure));
            }

            fadeProgressWheelOut();
        }
    }
    @Override
    public void failure(RetrofitError error) {

        //if we get a response and we aren't attached to an activity
        if(!attachedToActivity)
            return;

        //TODO: decide what to do here

        //some sort of server error, just remove the thing
        fadeProgressWheelOut();

        //then we set it up -- was it available??
        applyGreenRedIcon(
                (ImageView) rootView.findViewById(R.id.register_username_icon),
                false,
                R.drawable.ic_action_user_green,
                R.drawable.ic_action_user_red);
    }

    void showProgressWheel()
    {
        //if we're animating right now, make sure to ignore the next animation end
        //we don't want to show the wheel only to have it hidden in a moment!
        if(progressAnimation != null) {
            ignoreNextAnimationEnd = true;

            //cut off the animation so it doesn't mess with the alpha
            progressAnimation.cancel();
        }

        try {
            progressWheel.spin();
            progressWheel.setBarColor(getResources().getColor(R.color.indeterminate_progress_wheel));
            progressWheel.setAlpha(1.0f);
            progressWheel.setSpinSpeed(230.0f / 360.0f);
            progressWheel.setVisibility(View.VISIBLE);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    void instantHideProgressWheel()
    {
        //shut it down
        progressWheel.setAlpha(0.0f);
        progressWheel.stopSpinning();
        progressWheel.setVisibility(View.INVISIBLE);
    }

    void fadeProgressWheelOut()
    {
        progressAnimation = new AlphaAnimation(1.0f, 0.0f);
        progressAnimation.setDuration(2250);
        progressAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //all done!

                //shut it down
                if(!ignoreNextAnimationEnd)
                    instantHideProgressWheel();

                ignoreNextAnimationEnd = false;

                progressAnimation = null;

//                progressWheel.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        progressWheel.startAnimation(progressAnimation);
    }

    //Handle different App status
    @Override
    public void onResume()
    {
        UserSessionManager.getInstance().register(this, this);
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        UserSessionManager.getInstance().onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_CODE_TOKEN_AUTH:

                //now we have our answer
                if(resultCode == Activity.RESULT_OK)
                {
                    //go ahead and request AGAIN -- same procedure
                    asyncRegisterEmail();
                }
                else
                {
                    showRegistrationFailure("Security Failure", "Google account authentication is required for registration");
                }

                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {

        attachedToActivity = false;
        super.onPause();

        //drop out of event updates please!
        UserSessionManager.getInstance().unregister(this, this);

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.menu_register, menu);

        //grab our menu item button from the newly inflated menu
        MenuItem cr = menu.findItem(R.id.complete_register);

        completeMenuButton = (ImageButton)cr.getActionView().findViewById(R.id.register_complete_button);

        // Calculate ActionBar height
        if(completeMenuButton != null) {

            int actionBarHeight = ((ActionBarActivity)getActivity()).getSupportActionBar().getHeight();

            completeMenuButton.setLayoutParams(new RelativeLayout.LayoutParams(actionBarHeight, actionBarHeight));

            completeMenuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //hide the keyboard, no use for that here!
                    closeKeyboardInput();
                    new MaterialDialog.Builder(getActivity())
                            .title("Verify Correct E-mail")
                            .content(Html.fromHtml("You have entered <br/><b>" + emailEditText.getText().toString() + "</b><br/><br/> Is this your correct e-mail address?"))
                            .titleAlignment(Alignment.CENTER)
                            .contentAlignment(Alignment.CENTER)
                            .positiveText("Yes")
                            .neutralText("No")
                            .callback(new MaterialDialog.Callback() {
                                @Override
                                public void onNegative(MaterialDialog materialDialog) {
                                    //easy, just hide ourselves!
                                    materialDialog.hide();
                                }

                                @Override
                                public void onPositive(MaterialDialog materialDialog) {
                                    materialDialog.hide();
                                    asyncRegisterEmail();
                                }
                            })
                            .show();
                }
            });

        }

        //quickly set up the entries to test faster manually
//        quickSetText();

        //set up our menu initially -- if we have everything we need, we are good to go
        //but initially, that won't be the case
        checkReadyToFinish();



    }

    //make a call to register this email info when ready -- also fetches google token
    //called in an async task -- probably good place for bolts
    //TODO: better than async task, BOLTS
    void asyncRegisterEmail()
    {
        //locally good to go
        if(isEmailValid && isUsernameValid && isPasswordValid) {

            //did we come in with facebook info?
            if(userSentFromFacebook)
            {
                OAuth2Signup signup = new OAuth2Signup();
                signup.email = emailEditText.getText().toString();
                signup.username = usernameEditText.getText().toString();
                signup.password = passwordEditText.getText().toString();

                //we'll do our registering with facebook thanks!
                UserSessionManager.getInstance().signupWithFacebook(signup);
            }
            else
            {
                OAuth2Signup signup = new OAuth2Signup();
                signup.email = emailEditText.getText().toString();
                signup.username = usernameEditText.getText().toString();
                signup.password = passwordEditText.getText().toString();

                UserSessionManager.getInstance().signupWithEmail(this, signup);
            }

            //grab user email for google specifically to create the token
//            AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
//                @Override
//                protected String doInBackground(Void... params) {
//
//                    String email = UserEmailFetcher.getEmail(getActivity());
//                    String token = null;
//
//                    try {
//
//                        token = GoogleAuthUtil.getToken(getActivity(),
//                                email,
//                                "audience:server:client_id:" + getResources().getString(R.string.google_client_id));
////                                                    "audience:server:client_id:694834200067-jv9qgnu0eb5pr95cc4gva5i4f2qjcok6.apps.googleusercontent.com");
//
//                        OAuth2Signup signup = new OAuth2Signup();
//                        signup.api_token = token;
//                        signup.email = emailEditText.getText().toString();
//                        signup.username = usernameEditText.getText().toString();
//                        signup.password = passwordEditText.getText().toString();
//
//                        APIToken apiToken = apiService.syncEmailSignup(signup);
//                        if(apiToken.user != null && apiToken.api_token != null)
//                        {
//                            Log.d("TokenRetrieve", apiToken.api_token);
//
//                            //launch into the actual app now (temporarily user settings)
//                            FragmentFlowManager.getInstance().tempLaunchUserSettings(getActivity());
//                        }
//
//
//                    } catch (IOException transientEx) {
//                        // Network or server error, try later
//                        Log.e(TAG, transientEx.toString());
//                    } catch (UserRecoverableAuthException e) {
//                        // Recover (with e.getIntent())
//                        Log.e(TAG, e.toString());
//                        Intent recover = e.getIntent();
//                        startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);
//
//                    } catch (GoogleAuthException authEx) {
//                        // The call is not ever expected to succeed
//                        // assuming you have already verified that
//                        // Google Play services is installed.
//                        Log.e(TAG, authEx.toString());
//                    }
//                    catch (Exception e)
//                    {
//                        e.printStackTrace();
//                    }
//
//                    return token;
//                }
//
//                @Override
//                protected void onPostExecute(String token) {
//                    Log.i(TAG, "Access token retrieved:" + token);
//                }
//
//            };
//
//            //start it up!
//            task.execute();
        }
    }


    boolean checkReadyToFinish()
    {
        boolean readyToFinish = isEmailValid && isPasswordValid && isUsernameValid;

        if(completeMenuButton!= null && completeMenuButton.isEnabled() != readyToFinish)
            completeMenuButton.setEnabled(readyToFinish);

        return readyToFinish;
    }
    void popRegistrationFragment()
    {
        getFragmentManager()
                .beginTransaction()
                .remove(this)
                .commit();
    }
    void closeKeyboardInput()
    {
        //close the damn keyboard
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(emailEditText.getWindowToken(), 0);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id)
        {
            case android.R.id.home:

                closeKeyboardInput();
                getActivity().onBackPressed();
                return true;
            case R.id.complete_register:

                //if we're enabled, then we're good to go!
                if(completeMenuButton.isEnabled())
                {
                    //locally good to go
                    if(isEmailValid && isUsernameValid && isPasswordValid) {
                        //grab user email for google specifically to create the token

                        Thread sendStuff = new Thread()
                        {
                            @Override
                            public void run() {
                                String email = UserEmailFetcher.getEmail(getActivity());
                                try {
                                    String tokenToVerify = GoogleAuthUtil.getToken
                                            (getActivity(),
                                                    email,
                                                    "audience:server:client_id:694834200067-jv9qgnu0eb5pr95cc4gva5i4f2qjcok6.apps.googleusercontent.com");

                                    OAuth2Signup signup = new OAuth2Signup();
                                    signup.api_token = tokenToVerify;
                                    signup.email = emailEditText.getText().toString();
                                    signup.username = usernameEditText.getText().toString();
                                    signup.password = passwordEditText.getText().toString();

                                    APIToken apiToken = apiService.syncEmailSignup(signup);

                                    Log.d("TokenRetrieve", apiToken.api_token);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        };

                        //start it up!
                        sendStuff.run();

                    }
                }


                break;
            default:

                break;
        }

        Log.d("Button press", "Frag action: " + item.toString() + " item id: " + id);



        return super.onOptionsItemSelected(item);
    }


    void showRegistrationFailure(String errorTitle, String errorMessage)
    {
        new MaterialDialog.Builder(getActivity())
                .title(errorTitle)
                .content(Html.fromHtml(errorMessage))
                .titleAlignment(Alignment.CENTER)
                .contentAlignment(Alignment.CENTER)
                .positiveText("Okay")
                .show();
    }

}
