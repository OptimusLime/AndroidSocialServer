package edu.eplex.androidsocialclient.API.Objects;


import java.util.ArrayList;

/**
 * Created by paul on 12/3/14.
 */
public class User {
    public String username;

    public String user_id;

    //whats the associated email
    public String email;

    //has this user been initialized? i.e. do they have email/username/password
    public boolean isInitialized;

    //what are the associated social account if any
    public ArrayList<SocialAccount> social_accounts;
}
