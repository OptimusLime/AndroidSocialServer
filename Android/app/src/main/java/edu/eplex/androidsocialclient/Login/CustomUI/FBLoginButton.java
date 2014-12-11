package edu.eplex.androidsocialclient.Login.CustomUI;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;

import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.internal.SessionAuthorizationType;
import com.facebook.internal.SessionTracker;

import java.util.Arrays;
import java.util.List;

/**
 * Created by paul on 12/11/14.
 */
public class FBLoginButton extends Button {

    //change this for more permissions
    private List<String> fbReadPermissions = Arrays.asList("public_profile", "email");
    private SessionLoginBehavior fbLoginBehavior = SessionLoginBehavior.SSO_WITH_FALLBACK;

    /**
     * Create the LoginButton by inflating from XML
     *
     * @see android.view.View#View(android.content.Context, android.util.AttributeSet)
     */
    public FBLoginButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        //this will set the style for the button, and the background -- can be done on the xml as well, I believe
        //this was taken from the FB SDK so it may change over time
        if (attrs.getStyleAttribute() == 0) {
            // apparently there's no method of setting a default style in xml,
            // so in case the users do not explicitly specify a style, we need
            // to use sensible defaults.
            this.setGravity(Gravity.CENTER);
            this.setTextColor(getResources().getColor(com.facebook.android.R.color.com_facebook_loginview_text_color));
            this.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimension(com.facebook.android.R.dimen.com_facebook_loginview_text_size));
            this.setTypeface(Typeface.DEFAULT_BOLD);
            if (isInEditMode()) {
                // cannot use a drawable in edit mode, so setting the background color instead
                // of a background resource.
                this.setBackgroundColor(getResources().getColor(com.facebook.android.R.color.com_facebook_blue));
                // hardcoding in edit mode as getResources().getString() doesn't seem to work in IntelliJ
            } else {
                this.setBackgroundResource(com.facebook.android.R.drawable.com_facebook_button_blue);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {


        super.onAttachedToWindow();
    }

    public Session fetchFBAccessToken(Fragment parentFragment, Session.StatusCallback callback)
    {
        return fetchFBAccessToken(parentFragment, callback, fbReadPermissions);
    }

    //we only ever open the session for reading
    public Session fetchFBAccessToken(Fragment parentFragment, Session.StatusCallback callback, List<String> permissions)
    {
        //we check if we're already logging in for a session, if so, we kill the session
        final Session openSession = Session.getActiveSession();

        //we always clear this info because we don't want outdated tokens during signup/login
        //the user won't mind calling facebook again, it's expected
        if(openSession != null)
            openSession.closeAndClearTokenInformation();

        //allowloginui == true because we want to open the session no matter what -- whether we have something cached or not
        return Session.openActiveSession(parentFragment.getActivity(), parentFragment, true, permissions, callback);
    }



}