package edu.eplex.androidsocialclient.API.Objects;

import java.util.Date;

/**
 * Created by paul on 12/3/14.
 */
public class APIToken {
    //did we find a user? If not, we need to sign them up
    public boolean user_exists;

    //API token
    public String api_token;

    //sends back the user object associated with the API token
    public User user;

    //When is this api token okay until? Server will check too, but if it expires locally, logout
    public Date token_expiration;
}
