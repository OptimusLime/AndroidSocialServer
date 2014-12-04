package edu.eplex.androidsocialclient.API.Manager;

import edu.eplex.androidsocialclient.API.LoginAPI;
import retrofit.RestAdapter;

/**
 * Created by paul on 12/3/14.
 */
public class APIManager {

    private RestAdapter restAdapter;

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

    public LoginAPI createLoginAPI() throws Exception {
        if(restAdapter == null)
            throw new Exception("Instantiating login API before creating rest adapter");

        return restAdapter.create(LoginAPI.class);
    }

}
