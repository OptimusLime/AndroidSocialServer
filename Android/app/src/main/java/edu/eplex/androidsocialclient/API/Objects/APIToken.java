package edu.eplex.androidsocialclient.API.Objects;

import java.util.Date;

/**
 * Created by paul on 12/3/14.
 */
public class APIToken {
    //API token
    public String api_token;

    //When is this api token okay until? Server will check too, but if it expires locally, logout
    public Date token_expiration;

    //sends back the user object associated with the API token
    public User user;
}
