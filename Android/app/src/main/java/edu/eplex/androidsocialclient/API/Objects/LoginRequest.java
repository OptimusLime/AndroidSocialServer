package edu.eplex.androidsocialclient.API.Objects;

/**
 * Created by paul on 12/10/14.
 */
public class LoginRequest {
    public LoginRequest(String user, String pw)
    {
        username = user;
        password = pw;
    }
    String username;
    String password;
    //potentially we will require a google token as well
    //to prevent someone slamming our server with username/password inquiries
//    String clientID;
}
