package edu.eplex.androidsocialclient.MainUI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.Alignment;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;

import java.util.Arrays;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import edu.eplex.androidsocialclient.API.Manager.UserSessionManager;
import edu.eplex.androidsocialclient.R;
import edu.eplex.androidsocialclient.Utilities.FragmentFlowManager;

/**
 * Created by paul on 12/9/14.
 */
public class UserSettingsFragment extends Fragment {

    //Tag for logging
    private static final String TAG = "UserSettingsFragment";

    ListView listView;

    public static final int HDR_POS1 = 0;
    public static final int HDR_POS2 = 5;

    public final TitleActionPair[] ACTIONLIST = {
            new TitleActionPair("ACCOUNT"),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff),
            new TitleActionPair("Log Out", ButtonActionEnum.LogOff)

    };

    private static final Integer LIST_HEADER = 0;
    private static final Integer LIST_ITEM = 1;

    private enum ButtonActionEnum
    {
        LogOff,
        Nothing
    }
    private class TitleActionPair{

        public TitleActionPair(String title)
        {
            this.title = title;
            this.action = ButtonActionEnum.Nothing;
        }
        public TitleActionPair(String title, ButtonActionEnum action)
        {
            this.title = title;
            this.action = action;
        }
        public String title;
        public ButtonActionEnum action;
    }

    private boolean userFromCache = false;
    private boolean welcomeUser = false;
    private String currentUser;
    public void shouldWelcomeUsers(boolean welcomeUser)
    {
        this.welcomeUser = welcomeUser;
    }
    void welcomeUserCrouton(String username)
    {
        if(!welcomeUser && !userFromCache)
            return;

        if(userFromCache)
            Crouton.makeText(getActivity(),
                    getActivity().getResources().getString(R.string.user_back_message) + " " + username,
                    Style.INFO,
                    R.id.crouton_handle).show();
        else
            Crouton.makeText(getActivity(),
                    getActivity().getResources().getString(R.string.user_login_message) + " " + username,
                    Style.INFO,
                    R.id.crouton_handle).show();

//        (RelativeLayout)getActivity().findViewById(R.id.crouton_toolbar_layout)).show();

        //no more of this thank you!
        welcomeUser = false;
        userFromCache = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_account_settings, container, false);

        listView = (ListView)rootView.findViewById(R.id.account_settings_listview);
        listView.setAdapter(new SettingsListAdapter(getActivity()));

        //make sure we're clickable!
        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //we got an item, switch on the position
                switch (ACTIONLIST[position].action)
                {
                    case LogOff:
                        //logging off please! Warn the user, then confirm logoff
                        warnUserBeforeLogout();

                        break;
                }

            }
        });

        //register our toolbar with the activity -- so we get callbacks and stuff
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.accountSettingsToolbar);
        ActionBarActivity abActivity = ((ActionBarActivity)getActivity());
        abActivity.setSupportActionBar(toolbar);
//        abActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        abActivity.getSupportActionBar().setTitle(getResources().getString(R.string.as_toolbar_title));

        //confirm we want options callback
        setHasOptionsMenu(true);

        UserSessionManager.getInstance().register(this, this);


        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

        //when we're getting paused -- not shown, just unregister -- no zombie calls please
        UserSessionManager.getInstance().unregister(this, this);
    }

    @Override
    public void onResume() {
        UserSessionManager.getInstance().register(this, this);
        super.onResume();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Crouton.clearCroutonsForActivity(getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);


    }

    private class SettingsListAdapter extends BaseAdapter {
        public SettingsListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return ACTIONLIST.length;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            String headerText = getHeader(position);
            if(headerText != null) {

                View item = convertView;
                if(convertView == null || convertView.getTag() == LIST_ITEM) {

                    item = LayoutInflater.from(mContext).inflate(
                            R.layout.account_settings_header, parent, false);
                    item.setTag(LIST_HEADER);
                }

                TextView headerTextView = (TextView)item.findViewById(R.id.as_list_hdr);
                headerTextView.setText(headerText);
                return item;
            }

            View item = convertView;
            if(convertView == null || convertView.getTag() == LIST_HEADER) {
                item = LayoutInflater.from(mContext).inflate(
                        R.layout.account_settings_listview_items, parent, false);
                item.setTag(LIST_ITEM);
            }

            TextView header = (TextView)item.findViewById(R.id.as_item_header);
            header.setText(ACTIONLIST[position % ACTIONLIST.length].title);

            return item;
        }

        private String getHeader(int position) {

            if(position == HDR_POS1  || position == HDR_POS2) {
                return ACTIONLIST[position].title;
            }

            return null;
        }

        private final Context mContext;
    }

    //here we warn the user before loggin off
    void warnUserBeforeLogout()
    {
        new MaterialDialog.Builder(getActivity())
            .title("Confirm Log Out")
            .content(Html.fromHtml("<br/>Are you sure you want to log out?<br/>"))
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
                    logoutUser();
                }
            })
            .show();
    }

    void logoutUser()
    {
        //TODO: more officially log user out
        UserSessionManager.getInstance().logoutUser(this);
    }

    @Subscribe
    public void currentUserInformation(UserSessionManager.CurrentUserInformation currentUserInfo)
    {
        currentUser = currentUserInfo.currentAPIToken.user.username;
        userFromCache = currentUserInfo.userLoadedFromCache;
    }

    @Subscribe
    public void userLoggedOut(UserSessionManager.UserLoggedOutEvent event)
    {
        //its official, the user has been logged out of the system

        //we need to drop back to login page
        FragmentFlowManager.getInstance().returnToLoginScreen(getActivity());
    }

    //This is the menu options handling
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.menu_account_settings, menu);

        //got our current user -- welcome if we should
        welcomeUserCrouton(currentUser);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case android.R.id.home:

                //go back please!
                getActivity().onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
