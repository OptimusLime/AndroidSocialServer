package edu.eplex.androidsocialclient.API.Manager;

import android.content.Context;

import edu.eplex.androidsocialclient.API.LoginAPI;
import edu.eplex.androidsocialclient.R;
import retrofit.RestAdapter;

/**
 * Created by paul on 12/3/14.
 */
public class APIManager {

    private RestAdapter restAdapter;
    private LoginAPI loginServiceAPI;

    private static APIManager instance = null;
    protected APIManager() {
        // Exists only to defeat instantiation.
    }
    public static APIManager getInstance() {
        if(instance == null) {
            instance = new APIManager();
        }
        return instance;
    }

    private void createAPIAdapter(Context context)
    {
        if(restAdapter != null)
            return;

        restAdapter = new RestAdapter.Builder()
                .setEndpoint(context.getResources().getString(R.string.app_server_endpoint))
                .build();
    }

    //only create one across the app, it doesn't need to be duplicated -- the calls all'
    //come from the same place
    public LoginAPI createLoginAPI(Context context) throws Exception {
        if(restAdapter == null)
            createAPIAdapter(context);

        if(loginServiceAPI == null)
            loginServiceAPI = restAdapter.create(LoginAPI.class);

        return loginServiceAPI;
    }

}
