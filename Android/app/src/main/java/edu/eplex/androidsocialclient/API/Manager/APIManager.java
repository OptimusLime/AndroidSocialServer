package edu.eplex.androidsocialclient.API.Manager;

import edu.eplex.androidsocialclient.API.LoginAPI;
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

    public void createAPIAdapter(String endpoint)
    {
        if(restAdapter != null)
            return;

        restAdapter = new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .build();
    }

    //only create one across the app, it doesn't need to be duplicated -- the calls all'
    //come from the same place
    public LoginAPI createLoginAPI() throws Exception {
        if(restAdapter == null)
            throw new Exception("Instantiating login API before creating rest adapter");

        if(loginServiceAPI == null)
            loginServiceAPI = restAdapter.create(LoginAPI.class);

        return loginServiceAPI;
    }

}
