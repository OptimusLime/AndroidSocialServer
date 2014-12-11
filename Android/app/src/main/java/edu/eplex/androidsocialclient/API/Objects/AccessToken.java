package edu.eplex.androidsocialclient.API.Objects;

/**
 * Created by paul on 12/3/14.
 */
//This is the object that's sent to the WIN servers that then gets converted into a API token
public class AccessToken {
    //we send the access token from the network
    public AccessToken(String access_token)
    {this.access_token = access_token;}

    public String access_token;
}
