package edu.eplex.androidsocialclient.Login;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.validation.RegexpValidator;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import edu.eplex.androidsocialclient.API.LoginAPI;
import edu.eplex.androidsocialclient.API.Manager.APIManager;
import edu.eplex.androidsocialclient.API.Objects.APIToken;
import edu.eplex.androidsocialclient.API.Objects.AccessToken;
import edu.eplex.androidsocialclient.API.Objects.OAuth2Signup;
import edu.eplex.androidsocialclient.API.Objects.UsernameCheck;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.ProgressWheel;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by paul on 12/3/14.
 */
public class RegisterFragment extends Fragment implements Callback<UsernameCheck>{

    //Tag for logging
    private static final String TAG = "RegisterFragment";

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

    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    ScheduledFuture scheduledAPIRequest;
    private int USERNAME_CHECK_TIMEOUT = 2000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//
//        //Listen for changes in the back stack
//        getActivity().getSupportFragmentManager().addOnBackStackChangedListener(this);

        try {

//            getActivity().getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
//            getActivity().getActionBar().show();

            if(apiService == null)
                apiService = APIManager.getInstance().createLoginAPI();


        }
        catch (NullPointerException e)
        {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.registration_page, container, false);

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


        return rootView;
    }

    void applyGreenRedIcon(ImageView view, boolean validated, int drawGreen, int drawRed)
    {
        if(validated)
            view.setBackgroundDrawable(getResources().getDrawable(drawGreen));
        else
            view.setBackgroundDrawable(getResources().getDrawable(drawRed));

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
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
        }


        //set up our menu initially -- if we have everything we need, we are good to go
        //but initially, that won't be the case
        checkReadyToFinish();

    }

    boolean checkReadyToFinish()
    {
        boolean readyToFinish = isEmailValid && isPasswordValid && isUsernameValid;

        if(completeMenuButton.isEnabled() != readyToFinish)
            completeMenuButton.setEnabled(readyToFinish);

        return  readyToFinish;
    }
    void popRegistrationFragment()
    {
        getFragmentManager()
                .beginTransaction()
                .remove(this)
                .commit();
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
                //close the damn keyboard
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(emailEditText.getWindowToken(), 0);

                getActivity().onBackPressed();
                return true;
            default:

                break;
        }

        Log.d("Button press", "Frag action: " + item.toString() + " item id: " + id);



        return super.onOptionsItemSelected(item);
    }
}
